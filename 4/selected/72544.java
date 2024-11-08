package org.jsresources.apps.radio;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import javax.swing.JOptionPane;
import javax.sound.sampled.*;
import org.jsresources.utils.audio.*;
import org.tritonus.share.sampled.AudioFormats;
import static org.jsresources.apps.radio.Constants.*;

public class RadioModel {

    private MasterModel m_masterModel;

    private PropertyChangeSupport m_propertyChangeSupport;

    private Network m_network;

    private DataInputStream m_receiveStream;

    private AsynchronousRecorder recorder;

    private AudioBase[] audio = new AudioBase[2];

    private CircularBuffer circBuf;

    private boolean m_audioActive;

    private String m_url;

    private List<StatusListener> displayListeners = new ArrayList<StatusListener>();

    private StatusListenerThread statusThread;

    public RadioModel(MasterModel masterModel) {
        m_masterModel = masterModel;
        m_propertyChangeSupport = new PropertyChangeSupport(this);
        audio[DIR_MIC] = new AudioCapture(getCircBufFormat(), getAudioSettings().getSelMixer(DIR_MIC), getAudioSettings().getBufferSizeMillis(DIR_MIC));
        audio[DIR_SPK] = new AudioPlayback(getCircBufFormat(), getFileFormat(), getAudioSettings().getSelMixer(DIR_SPK), getAudioSettings().getBufferSizeMillis(DIR_SPK));
        circBuf = new CircularBuffer();
        m_audioActive = false;
    }

    private MasterModel getMasterModel() {
        return m_masterModel;
    }

    private ConnectionSettings getConnectionSettings() {
        return getMasterModel().getConnectionSettings();
    }

    private AudioSettings getAudioSettings() {
        return getMasterModel().getAudioSettings();
    }

    private Network getNetwork() {
        return m_network;
    }

    public AudioBase getAudio(int d) {
        return audio[d];
    }

    public CircularBuffer getCircularBuffer() {
        return circBuf;
    }

    public boolean isSourceCapture() {
        return ((m_url != null) && (m_url.length() == 0));
    }

    public void startCapture() {
        start("");
    }

    public void start(String sourceURL) {
        m_url = sourceURL;
        try {
            if (isSourceCapture()) {
                circBuf.init(getCircBufFormat(), getAudioSettings().getCircBufMillis());
                Debug.out("Starting circular buffer with this format:");
                Debug.out("   " + circBuf.getFormat());
                startAudio(DIR_MIC);
                initAudioOutputStream();
            } else {
                throw new Exception("Not implemented");
            }
            startAudio(DIR_SPK);
            ((AudioPlayback) getAudio(DIR_SPK)).setAudioInputStream(circBuf.getSpeakerAIS());
            setAudioActive(true);
        } catch (Exception e) {
            Debug.out(e);
            JOptionPane.showMessageDialog(null, new Object[] { "Error: ", e.getMessage() }, "Error", JOptionPane.ERROR_MESSAGE);
            stop();
        }
        notifyStarted();
    }

    public void stop() {
        Debug.out("closing audio...");
        closeAudio();
        Debug.out("...closed");
        stopRecording();
        if (isConnected()) {
            Debug.out("diconnecting network...");
            getNetwork().disconnect();
            Debug.out("disconnected...");
            notifyConnection();
        }
        circBuf.init();
        for (int i = 0; i < displayListeners.size(); i++) {
            displayListeners.get(i).displayStatus(0, 0);
        }
        notifyStarted();
    }

    public synchronized void startRecording(String filename) throws Exception {
        circBuf.setRecorderPosToSpeakerPos();
        if (recorder != null) {
            stopRecording();
        }
        AudioFileFormat.Type afft = getAudioSettings().getPreferredAudioFileType();
        AudioFormat prefFormat = getAudioSettings().getPreferredAudioFormat();
        AudioInputStream ais = circBuf.getRecorderAIS();
        if (!AudioFormats.matches(ais.getFormat(), prefFormat)) {
            ais = AudioSystem.getAudioInputStream(prefFormat, ais);
        }
        recorder = new AsynchronousRecorder(filename, ais, afft);
        recorder.start();
        notifyRecording();
    }

    public synchronized void stopRecording() {
        if (recorder != null) {
            recorder.stop();
            recorder = null;
        }
        notifyRecording();
    }

    public synchronized boolean isRecording() {
        return (recorder != null) && (recorder.isActive());
    }

    public boolean isConnected() {
        return getNetwork() != null && getNetwork().isConnected();
    }

    public boolean isStarted() {
        return getAudio(DIR_SPK).isStarted();
    }

    public void initAudioOutputStream() {
        if (getAudio(DIR_MIC).isStarted()) {
            Debug.out("Connect line in with circular buffer.");
            ((AudioCapture) getAudio(DIR_MIC)).setOutputStream(circBuf);
        }
    }

    private void closeAudio() {
        setAudioActive(false);
        closeAudio(DIR_SPK);
        closeAudio(DIR_MIC);
    }

    public boolean isAudioActive() {
        return m_audioActive;
    }

    private void setAudioActive(boolean active) {
        m_audioActive = active;
        notifyAudio();
    }

    private void closeAudio(int d) {
        if (getAudio(d) != null) {
            getAudio(d).close();
        }
    }

    private void startAudio(int d) throws Exception {
        String dir;
        if (isAudioActive()) {
            throw new Exception("Cannot start audio if already active!");
        }
        if (d == DIR_MIC) {
            dir = "line in";
        } else {
            dir = "speaker";
        }
        Debug.out("Start audio: " + dir);
        if (d == DIR_MIC) {
            getAudio(d).setFormat(getCircBufFormat(), getSourceFormat());
        } else {
            getAudio(d).setFormat(getCircBufFormat(), getCircBufFormat());
        }
        Debug.out("Opening " + dir + " with this format:");
        Debug.out("   " + getAudio(d).getLineFormat());
        getAudio(d).open();
        getAudio(d).start();
    }

    public AudioFormat getCircBufFormat() {
        AudioFormat res = getSourceFormat();
        if (AudioUtils.isPCM(res)) return res;
        return new AudioFormat(res.getSampleRate(), 16, res.getChannels(), true, false);
    }

    public AudioFormat getFileFormat() {
        AudioFormat cbFormat = getCircBufFormat();
        AudioFormat prefFormat = getAudioSettings().getPreferredAudioFormat();
        if (AudioFormats.matches(cbFormat, prefFormat)) {
            return cbFormat;
        }
        if (AudioUtils.isPCM(prefFormat)) {
            return cbFormat;
        }
        if ((Math.abs(prefFormat.getSampleRate() - cbFormat.getSampleRate()) > 0.1) || (prefFormat.getChannels() != cbFormat.getChannels())) {
            return new AudioFormat(prefFormat.getEncoding(), cbFormat.getSampleRate(), prefFormat.getSampleSizeInBits(), cbFormat.getChannels(), AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false);
        }
        return prefFormat;
    }

    public AudioFormat getSourceFormat() {
        if (m_network == null || isSourceCapture()) {
            AudioFormat prefFormat = getAudioSettings().getPreferredAudioFormat();
            if (AudioUtils.isPCM(prefFormat)) {
                return prefFormat;
            } else {
                return new AudioFormat(prefFormat.getSampleRate(), 16, prefFormat.getChannels(), true, false);
            }
        } else {
            return null;
        }
    }

    public int getPlayPositionMillis() {
        return getAudio(DIR_SPK).getPositionMillis();
    }

    public int getSpeakerLagMillis() {
        return circBuf.getSpeakerLagMillis();
    }

    public int getRecorderLagMillis() {
        return circBuf.getRecorderLagMillis();
    }

    public void wind(int dir, int millis) {
        AudioInputStream ais = getAudio(dir).getAudioInputStream();
        if (ais != null) {
            try {
                ais.skip(AudioUtils.millis2bytes(millis, ais.getFormat()));
            } catch (IOException ioe) {
            }
        }
    }

    public DataInputStream getReceiveStream() {
        return m_receiveStream;
    }

    private void streamError(String strError) {
        JOptionPane.showMessageDialog(null, new Object[] { strError, "Connection will be terminated" }, "Error", JOptionPane.ERROR_MESSAGE);
        getNetwork().disconnect();
        closeAudio();
        notifyConnection();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        m_propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        m_propertyChangeSupport.removePropertyChangeListener(listener);
    }

    private void notifyConnection() {
        m_propertyChangeSupport.firePropertyChange(CONNECTION_PROPERTY, !isConnected(), isConnected());
    }

    private void notifyStarted() {
        m_propertyChangeSupport.firePropertyChange(STARTED_PROPERTY, !isStarted(), isStarted());
        if (isStarted()) {
            startStatusListenerThread();
        } else {
            stopStatusListenerThread();
        }
    }

    private void notifyAudio() {
        m_propertyChangeSupport.firePropertyChange(AUDIO_PROPERTY, !isAudioActive(), isAudioActive());
    }

    private void notifyRecording() {
        m_propertyChangeSupport.firePropertyChange(RECORDING_PROPERTY, !isRecording(), isRecording());
    }

    public void addStatusListener(StatusListener l) {
        displayListeners.add(l);
    }

    public void removeStatusListener(StatusListener l) {
        displayListeners.remove(l);
    }

    private int getCurrLevel(int dir, int oldLevel) {
        AudioBase ab = getAudio(dir);
        int newlevel = 0;
        if (ab != null) {
            newlevel = ab.getLevel();
        }
        if (newlevel < 0) newlevel = 0;
        if (oldLevel - newlevel > 2) {
            return oldLevel - 2;
        }
        return newlevel;
    }

    private synchronized void startStatusListenerThread() {
        stopStatusListenerThread();
        statusThread = new StatusListenerThread();
        statusThread.start();
    }

    private synchronized void stopStatusListenerThread() {
        if (statusThread != null) {
            statusThread.terminate();
            statusThread = null;
        }
    }

    private class StatusListenerThread extends Thread {

        private volatile boolean terminated;

        public void run() {
            if (DEBUG) out("Meter Thread: start");
            try {
                int inlevel = 0;
                int outlevel = 0;
                while (!terminated) {
                    inlevel = getCurrLevel(0, inlevel);
                    outlevel = getCurrLevel(1, outlevel);
                    Thread.sleep(30);
                    for (int i = 0; i < displayListeners.size(); i++) {
                        displayListeners.get(i).displayStatus(inlevel, outlevel);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int i = 0; i < displayListeners.size(); i++) {
                displayListeners.get(i).displayStatus(0, 0);
            }
            if (DEBUG) out("Meter Thread: stop");
        }

        public void terminate() {
            terminated = true;
        }
    }

    public interface StatusListener {

        public void displayStatus(int inLevel, int outLevel);
    }
}
