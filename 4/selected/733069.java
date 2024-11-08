package org.jsresources.apps.jam.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jsresources.apps.jmvp.manager.swing.ActionManager;
import org.jsresources.apps.jmvp.manager.swing.AMAction;
import org.jsresources.apps.jam.Main;
import org.jsresources.apps.jam.Debug;
import org.jsresources.apps.jam.audio.PreListen;

/**
 */
public class AudioFileChooserAccessory extends JPanel implements PropertyChangeListener {

    private JLabel m_encodingLabel;

    private JLabel m_sampleRateLabel;

    private JLabel m_bitsPerSampleLabel;

    private JLabel m_channelsLabel;

    private JLabel m_lengthLabel;

    private PreListen m_preListen;

    private File m_selectedFile;

    private boolean m_bFilePlayable;

    private Action m_startPrelistenAction;

    private Action m_stopPrelistenAction;

    public AudioFileChooserAccessory(JFileChooser fileChooser) {
        super();
        fileChooser.addPropertyChangeListener(this);
        m_preListen = new PreListen();
        m_preListen.addPropertyChangeListener(this);
        m_preListen.setLooping(true);
        m_selectedFile = null;
        createActions(getActionManager());
        setLayout(new BorderLayout());
        Box fileInfoPanel = new Box(BoxLayout.Y_AXIS);
        m_encodingLabel = new JLabel("-");
        fileInfoPanel.add(m_encodingLabel);
        m_sampleRateLabel = new JLabel("-");
        fileInfoPanel.add(m_sampleRateLabel);
        m_bitsPerSampleLabel = new JLabel("-");
        fileInfoPanel.add(m_bitsPerSampleLabel);
        m_channelsLabel = new JLabel("-");
        fileInfoPanel.add(m_channelsLabel);
        m_lengthLabel = new JLabel();
        fileInfoPanel.add(m_lengthLabel);
        updateFileInfo();
        add(fileInfoPanel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        AbstractButton button;
        button = getActionManager().createButton("startPrelistenButton");
        buttonPanel.add(button);
        button = getActionManager().createButton("stopPrelistenButton");
        buttonPanel.add(button);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void prepareFile() {
        m_bFilePlayable = m_preListen.setFile(m_selectedFile);
    }

    private void updateFile(File file) {
        boolean bWasPlaying = false;
        if (m_preListen.isPlaying()) {
            bWasPlaying = true;
            m_preListen.setPlaying(false);
        }
        m_selectedFile = file;
        updateFileInfo();
        if (bWasPlaying) {
            prepareFile();
            if (m_bFilePlayable) {
                m_preListen.setPlaying(true);
            }
        }
    }

    private void updateFileInfo() {
        String strNoInfoText = "---";
        if (m_selectedFile == null) {
            m_encodingLabel.setText(strNoInfoText);
            m_sampleRateLabel.setText(strNoInfoText);
            m_bitsPerSampleLabel.setText(strNoInfoText);
            m_channelsLabel.setText(strNoInfoText);
            m_lengthLabel.setText(strNoInfoText);
            return;
        }
        AudioInputStream ais = null;
        try {
            ais = AudioSystem.getAudioInputStream(m_selectedFile);
        } catch (UnsupportedAudioFileException e) {
            if (Debug.getTraceAllExceptions()) {
                Debug.out(e);
            }
        } catch (IOException e) {
            if (Debug.getTraceAllExceptions()) {
                Debug.out(e);
            }
        }
        if (ais != null) {
            AudioFormat format = ais.getFormat();
            m_encodingLabel.setText(format.getEncoding().toString());
            m_sampleRateLabel.setText("" + ((int) format.getSampleRate()) + " Hz");
            m_bitsPerSampleLabel.setText("" + format.getSampleSizeInBits() + " bit");
            String strChannelsText;
            switch(format.getChannels()) {
                case 1:
                    strChannelsText = "mono";
                    break;
                case 2:
                    strChannelsText = "stereo";
                    break;
                default:
                    strChannelsText = "" + format.getChannels() + "channels";
                    break;
            }
            m_channelsLabel.setText(strChannelsText);
            m_lengthLabel.setText((ais.getFrameLength() == AudioSystem.NOT_SPECIFIED) ? "unknown length" : "" + (ais.getFrameLength() / format.getSampleRate()) + " sec.");
        } else {
            m_encodingLabel.setText(strNoInfoText);
            m_sampleRateLabel.setText(strNoInfoText);
            m_bitsPerSampleLabel.setText(strNoInfoText);
            m_channelsLabel.setText(strNoInfoText);
            m_lengthLabel.setText(strNoInfoText);
        }
    }

    public void propertyChange(PropertyChangeEvent event) {
        String strProperty = event.getPropertyName();
        if (strProperty.equals(PlayableObject.PLAYABLE_STATUS_PROPERTY)) {
            updateActions();
        } else if (strProperty.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
            File file = (File) event.getNewValue();
            updateFile(file);
        }
    }

    private void updateActions() {
        m_startPrelistenAction.setEnabled(!m_preListen.isPlaying() && m_selectedFile != null);
        m_stopPrelistenAction.setEnabled(m_preListen.isPlaying());
    }

    public ActionManager getActionManager() {
        return Main.getApplicationPresenter().getActionManager();
    }

    public void createActions(ActionManager actionManager) {
        m_startPrelistenAction = new StartPrelistenAction();
        actionManager.addAction(m_startPrelistenAction);
        m_stopPrelistenAction = new StopPrelistenAction();
        actionManager.addAction(m_stopPrelistenAction);
    }

    private class StartPrelistenAction extends AMAction {

        public void actionPerformed(ActionEvent ae) {
            if (Debug.getTraceActions()) {
                Debug.out("AudioFileChooserAccessory.StartPrelistenAction.actionPerformed(): begin");
            }
            prepareFile();
            m_preListen.setPlaying(true);
            if (Debug.getTraceActions()) {
                Debug.out("AudioFileChooserAccessory.StartPrelistenAction.actionPerformed(): end");
            }
        }
    }

    private class StopPrelistenAction extends AMAction {

        public void actionPerformed(ActionEvent ae) {
            if (Debug.getTraceActions()) {
                Debug.out("AudioFileChooserAccessory.StopPrelistenAction.actionPerformed(): begin");
            }
            m_preListen.setPlaying(false);
            if (Debug.getTraceActions()) {
                Debug.out("AudioFileChooserAccessory.StopPrelistenAction.actionPerformed(): end");
            }
        }
    }
}
