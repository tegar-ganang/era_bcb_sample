package org.jsresources.apps.jam.audio;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.sound.sampled.*;
import org.jsresources.apps.jmvp.manager.RM;
import org.jsresources.apps.jmvp.manager.swing.ActionManager;
import org.jsresources.apps.jmvp.manager.swing.AMAction;
import org.jsresources.apps.jam.Main;
import org.jsresources.apps.jam.Debug;
import org.jsresources.apps.jam.style.PlayEngine;
import org.jsresources.apps.jam.style.Style;
import org.jsresources.apps.jam.style.StyleSelection;
import org.tritonus.share.sampled.FloatSampleBuffer;

public class AudioCaptureDialog extends JDialog implements Runnable {

    private JLabel m_timeDisplay;

    private Thread m_thread;

    private boolean m_destroyThread = false;

    private boolean m_recording = false;

    private AudioCapture m_capture;

    private PlayEngine m_playEngine;

    private StyleSelection m_styleSelection;

    private int m_recordDelay;

    private int m_recordSampleCount;

    private AudioInputStream m_ais;

    private FloatSampleBuffer m_lastRecordedBuffer = new FloatSampleBuffer();

    private double m_tempo;

    private int m_beatsPerMeasure;

    private String strRecordedSuccessful = RM.getResourceString("AudioCaptureDialog.success");

    public AudioCaptureDialog(Frame frame, AudioCapture capture, PlayEngine playEngine, StyleSelection styleSelection) throws Exception {
        super(frame, RM.getResourceString("AudioCaptureDialog.title"));
        if (Debug.getTraceRecordGUI()) {
            Debug.out("AudioCaptureDialog.<init>(): begin");
        }
        m_capture = capture;
        m_playEngine = playEngine;
        m_styleSelection = styleSelection;
        if (styleSelection.getStyle() == null) {
            throw new Exception("No selection!");
        }
        m_tempo = (double) styleSelection.getStyle().getBeatsPerMinute();
        m_beatsPerMeasure = styleSelection.getStyle().getPhaseBeatsPerMeasure(styleSelection.getCurrentPhase());
        m_ais = AudioUtils.getConvertedStream(m_capture.getAudioInputStream(), AudioQualityModel.getInstance().getPlaybackFormat());
        if (Debug.getTraceRecordGUI()) {
            Debug.out("AudioCaptureDialog.<init>(): creating Actions...");
        }
        createActions(getActionManager());
        WindowListener windowListener = new WindowAdapter() {

            public void windowClosing(WindowEvent we) {
                close();
            }
        };
        this.addWindowListener(windowListener);
        if (Debug.getTraceRecordGUI()) {
            Debug.out("AudioCaptureDialog.<init>(): assembling GUI components....");
        }
        this.getContentPane().setLayout(new BorderLayout());
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        statusPanel.add(new JLabel(RM.getResourceString("AudioCaptureDialog.Status")));
        m_timeDisplay = new JLabel();
        statusPanel.add(m_timeDisplay);
        statusPanel.add(Box.createHorizontalGlue());
        this.getContentPane().add(statusPanel, BorderLayout.NORTH);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        AbstractButton button;
        button = getActionManager().createButton("audioCaptureStartButton");
        buttonPanel.add(button);
        button = getActionManager().createButton("audioCaptureTakeButton");
        buttonPanel.add(button);
        button = getActionManager().createButton("audioCaptureCancelButton");
        buttonPanel.add(button);
        m_thread = new Thread(this);
        m_thread.start();
        m_playEngine.setRecordedData(m_lastRecordedBuffer);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        if (Debug.getTraceRecordGUI()) {
            Debug.out("AudioCaptureDialog.<init>(): packing...");
        }
        pack();
        if (Debug.getTraceRecordGUI()) {
            Debug.out("AudioCaptureDialog.<init>(): end");
        }
    }

    private void start() throws Exception {
        m_recordDelay = m_playEngine.getRemainingSamplesUntilStartOfLoop();
        m_recordSampleCount = m_styleSelection.getStyle().getAudioDataSampleCount(m_styleSelection.getCurrentPhase());
        m_capture.start();
        m_recording = true;
        synchronized (m_thread) {
            m_thread.notify();
        }
        getActionManager().setEnabled("AudioCaptureStartAction", false);
        getActionManager().setEnabled("AudioCaptureTakeAction", false);
    }

    private void take() {
        m_playEngine.setRecordedData(null);
        m_styleSelection.getStyle().setCellAudioData(m_styleSelection.getCurrentTrack(), m_styleSelection.getCurrentPhase(), m_lastRecordedBuffer);
        close();
    }

    private void onRecordingFinished() {
        m_capture.stop();
        m_timeDisplay.setText(strRecordedSuccessful);
        getActionManager().setEnabled("AudioCaptureStartAction", true);
        getActionManager().setEnabled("AudioCaptureTakeAction", true);
    }

    private void close() {
        m_capture.close();
        setVisible(false);
        m_playEngine.setRecordedData(null);
        m_destroyThread = true;
        synchronized (m_thread) {
            m_thread.notify();
        }
    }

    public ActionManager getActionManager() {
        return Main.getApplicationPresenter().getActionManager();
    }

    public void createActions(ActionManager actionManager) {
        actionManager.addAction(new AudioCaptureStartAction());
        actionManager.addAction(new AudioCaptureTakeAction());
        actionManager.addAction(new AudioCaptureCancelAction());
        getActionManager().setEnabled("AudioCaptureStartAction", true);
        getActionManager().setEnabled("AudioCaptureTakeAction", false);
        getActionManager().setEnabled("AudioCaptureCancelAction", true);
    }

    private class AudioCaptureStartAction extends AMAction {

        public void actionPerformed(ActionEvent ae) {
            if (Debug.getTraceActions()) {
                Debug.out("AudioCaptureDialog.AudioCaptureStartAction.actionPerformed(): called");
            }
            try {
                AudioCaptureDialog.this.start();
            } catch (Exception e) {
                if (Debug.getTraceAllExceptions()) {
                    Debug.out(e);
                }
                String sMessage = e.getMessage();
                JOptionPane.showMessageDialog((Frame) getOwner(), sMessage);
            }
        }
    }

    private class AudioCaptureTakeAction extends AMAction {

        public void actionPerformed(ActionEvent ae) {
            if (Debug.getTraceActions()) {
                Debug.out("AudioCaptureDialog.AudioCaptureTakeAction.actionPerformed(): called");
            }
            AudioCaptureDialog.this.take();
            AudioCaptureDialog.this.close();
        }
    }

    private class AudioCaptureCancelAction extends AMAction {

        public void actionPerformed(ActionEvent ae) {
            if (Debug.getTraceActions()) {
                Debug.out("AudioCaptureDialog.AudioQualitCancelAction.actionPerformed(): called");
            }
            AudioCaptureDialog.this.close();
        }
    }

    private static final int BUFFER_SIZE = 4096;

    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (!m_destroyThread) {
            if (!m_recording) {
                synchronized (m_thread) {
                    try {
                        m_thread.wait();
                    } catch (InterruptedException ie) {
                    }
                }
            }
            if (m_destroyThread) break;
            if (m_recording) {
                int total = 0;
                int frameSize = m_ais.getFormat().getFrameSize();
                int floatOffset = 0;
                m_lastRecordedBuffer.reset(m_ais.getFormat().getChannels(), m_recordSampleCount, m_ais.getFormat().getSampleRate());
                try {
                    while (m_recording && !m_destroyThread) {
                        int thisRead = m_ais.read(buffer) / frameSize;
                        if (m_destroyThread) break;
                        if (thisRead > 0) {
                            if (total + thisRead >= m_recordDelay) {
                                int srcOffset = 0;
                                int sampleCount = thisRead;
                                if (total < m_recordDelay) {
                                    srcOffset = m_recordDelay - total;
                                    sampleCount -= srcOffset;
                                }
                                if (floatOffset + sampleCount > m_lastRecordedBuffer.getSampleCount()) {
                                    sampleCount = m_lastRecordedBuffer.getSampleCount() - floatOffset;
                                }
                                m_lastRecordedBuffer.setSamplesFromBytes(buffer, srcOffset, m_ais.getFormat(), floatOffset, sampleCount);
                                floatOffset += sampleCount;
                                displayTime((int) AudioUtils.samples2millis(floatOffset, m_ais.getFormat().getSampleRate()));
                            } else {
                                displayTime((int) AudioUtils.samples2millis(total + thisRead - m_recordDelay, m_ais.getFormat().getSampleRate()));
                            }
                            total += thisRead;
                            if (floatOffset >= m_lastRecordedBuffer.getSampleCount()) {
                                m_recording = false;
                                onRecordingFinished();
                            }
                        }
                    }
                } catch (Exception e) {
                    if (Debug.getTraceAllExceptions()) {
                        Debug.out(e);
                    }
                    m_timeDisplay.setText(e.getMessage());
                    m_recording = false;
                    onRecordingFinished();
                }
            }
        }
    }

    private void displayTime(int millis) {
        if (m_recording) {
            m_timeDisplay.setText(AudioUtils.formatBarsBeats(millis, m_tempo, m_beatsPerMeasure));
        }
    }
}
