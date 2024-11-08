package org.jsresources.apps.jam.style;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import javax.sound.sampled.*;
import org.jsresources.apps.jam.Debug;
import org.jsresources.apps.jam.audio.AudioQualityModel;
import org.jsresources.apps.jam.audio.AudioUtils;
import org.jsresources.apps.jam.audio.Metronome;
import org.jsresources.apps.jam.util.PlayableObject;
import org.tritonus.share.sampled.FloatSampleBuffer;

public class PlayEngine implements Runnable, PlayableObject {

    private static final int AUDIO_DEVICE_DELAY_MILLIS = 0;

    private Thread m_thread;

    private boolean m_bTerminate = false;

    private PropertyChangeSupport m_propertyChangeSupport;

    private boolean m_bPlaying = false;

    private boolean m_bLooping;

    private int m_bufferSize;

    private int m_totalDelayInSamples;

    private SourceDataLine m_line;

    private AudioFormat m_format;

    private Object m_lineLock = new Object();

    private int m_position = 0;

    private int m_playCounter = 0;

    private boolean m_lineStarted = false;

    private Style m_style;

    private Metronome m_metronome;

    private int m_selectedPhase;

    private FloatSampleBuffer m_recordedData;

    private long m_positionCurrentTime;

    public PlayEngine() {
        m_thread = new Thread(this);
        m_thread.setPriority(Thread.MAX_PRIORITY * 4 / 5);
        m_propertyChangeSupport = new PropertyChangeSupport(this);
        m_bPlaying = false;
        m_bLooping = false;
        m_format = AudioQualityModel.getInstance().getPlaybackFormat();
        m_metronome = null;
        m_selectedPhase = -1;
        m_thread.start();
    }

    private void createSourceDataLine() throws Exception {
        synchronized (m_lineLock) {
            SourceDataLine line = m_line;
            m_line = null;
            if (line != null) {
                line.close();
                line = null;
            }
            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, m_format);
                line = (SourceDataLine) AudioSystem.getLine(info);
                if (Debug.getTracePlay()) Debug.out("PlayEngine.createSourceDataLine(): Got SourceDataLine " + line.getClass());
                m_bufferSize = (int) (m_format.getSampleRate() / 2);
                m_bufferSize -= m_bufferSize % m_format.getFrameSize();
                line.open(m_format, m_bufferSize);
                if (Debug.getTracePlay()) Debug.out("PlayEngine.createSourceDataLine(): opened SourceDataLine ");
                m_bufferSize = line.getBufferSize();
                m_totalDelayInSamples = m_bufferSize / m_format.getFrameSize() + (int) AudioUtils.millis2samples(AUDIO_DEVICE_DELAY_MILLIS, m_format.getSampleRate());
                m_line = line;
                m_lineStarted = false;
            } catch (LineUnavailableException ex) {
                throw new Exception("Unable to open the line: " + ex.getMessage());
            }
        }
    }

    public void setPlaying(boolean bPlaying) throws Exception {
        if (bPlaying != isPlaying()) {
            if (bPlaying) {
                start();
            } else {
                stop();
            }
        }
    }

    protected void setPlayingImpl(boolean bPlaying) {
        boolean bOldValue = isPlaying();
        m_bPlaying = bPlaying;
        firePropertyChange(PlayableObject.PLAYABLE_STATUS_PROPERTY, bOldValue, isPlaying());
    }

    public boolean isPlaying() {
        return m_bPlaying;
    }

    public void setLooping(boolean bLooping) {
        m_bLooping = bLooping;
    }

    public boolean isLooping() {
        return m_bLooping;
    }

    public void start() throws Exception {
        if (m_metronome == null) {
            m_metronome = new Metronome();
        }
        createSourceDataLine();
        setPlayingImpl(true);
        m_playCounter++;
        synchronized (m_thread) {
            m_thread.notify();
        }
    }

    public void stop() {
        if (Debug.getTracePlay()) Debug.out("PlayEngine.stop(): stop called");
        setPlayingImpl(false);
        m_playCounter++;
        if (m_line != null) {
            synchronized (m_lineLock) {
                m_line.stop();
                m_line.close();
                if (Debug.getTracePlay()) Debug.out("PlayEngine.run(): closed line");
                m_line = null;
            }
        }
        m_position = 0;
        synchronized (m_thread) {
            m_thread.notify();
        }
    }

    public void exit() {
        m_bTerminate = true;
        stop();
    }

    private void reconfigureMetronome() {
        if (m_metronome == null) return;
        if (m_style != null && m_selectedPhase < m_style.getPhaseCount() && m_selectedPhase >= 0) {
            m_metronome.setProperties(m_style.getBeatsPerMinute(), m_style.getPhaseBeatsPerMeasure(m_selectedPhase), m_style.getPhaseMeasures(m_selectedPhase));
        } else {
            m_metronome.setProperties(0, 0, 0);
        }
    }

    public void setStyleAndSelection(Style style, StyleSelection sel) {
        if (Debug.getTracePlay()) Debug.out("PlayEngine.setStyleAndSelection() called");
        int newPhase;
        if (sel != null) {
            newPhase = sel.getCurrentPhase();
        } else {
            newPhase = -1;
        }
        if ((m_style != style) || (newPhase != m_selectedPhase)) {
            m_selectedPhase = newPhase;
            m_style = style;
            m_playCounter++;
            if (Debug.getTracePlay()) {
                if (m_style != null && m_selectedPhase >= 0) {
                    Debug.out("PlayEngine.setStyleAndSelection: style.sampleCount = " + m_style.getAudioDataSampleCount(m_selectedPhase));
                }
            }
        }
        if (Debug.getTracePlay()) Debug.out("PlayEngine.setStyleAndSelection() new Phase=" + m_selectedPhase + " style=" + m_style);
    }

    public int getPlayPositionInSamples() {
        if (m_style == null || m_selectedPhase < 0) return -1;
        int sampleCount = m_style.getAudioDataSampleCount(m_selectedPhase);
        int samplesOffset = 0;
        if (m_bPlaying) {
            long millisOffset = AudioUtils.getCurrentTime() - m_positionCurrentTime;
            samplesOffset = (int) AudioUtils.millis2samples(millisOffset, m_format.getSampleRate());
        }
        int playPosition = ((int) (m_position - m_totalDelayInSamples - samplesOffset));
        while (playPosition < 0) {
            playPosition += sampleCount;
        }
        return playPosition % sampleCount;
    }

    public int getRemainingSamplesUntilStartOfLoop() {
        if (m_style == null || m_selectedPhase < 0) return -1;
        int sampleCount = m_style.getAudioDataSampleCount(m_selectedPhase);
        int playPosition = getPlayPositionInSamples();
        return sampleCount - playPosition;
    }

    public int getPlayPositionInMillis() {
        int pp = getPlayPositionInSamples();
        return (int) AudioUtils.samples2millis(pp, m_format.getSampleRate());
    }

    public void run() {
        FloatSampleBuffer mixBuffer = new FloatSampleBuffer();
        if (Debug.getTracePlay()) Debug.out("PlayEngine.run(): begin");
        while (!m_bTerminate) {
            while (!m_bTerminate && !m_bPlaying) {
                if (Debug.getTracePlay()) Debug.out("PlayEngine.run(): waiting for started state");
                synchronized (m_thread) {
                    try {
                        m_thread.wait();
                    } catch (InterruptedException ie) {
                    }
                }
                if (Debug.getTracePlay()) Debug.out("PlayEngine.run(): woke up from waiting");
            }
            if (m_bTerminate) break;
            int playCounter = m_playCounter;
            reconfigureMetronome();
            int frameSize = m_format.getFrameSize();
            int bufferSampleCount = m_bufferSize / frameSize;
            AudioFormat format = m_format;
            mixBuffer.reset(format.getChannels(), bufferSampleCount, format.getSampleRate());
            byte[] byteData = new byte[m_bufferSize];
            if (Debug.getTracePlay()) Debug.out("PlayEngine.run(): start of play loop");
            while (!m_bTerminate && m_bPlaying && playCounter == m_playCounter) {
                mixBuffer.makeSilence();
                mixAll(mixBuffer);
                mixBuffer.convertToByteArray(byteData, 0, format);
                synchronized (m_lineLock) {
                    if (m_line != null) {
                        int toWrite = byteData.length;
                        int offset = 0;
                        while (!m_bTerminate && m_bPlaying && playCounter == m_playCounter && toWrite > 0) {
                            int written = m_line.write(byteData, offset, toWrite);
                            if (Debug.getTracePlay()) {
                                if (written != toWrite) {
                                    Debug.out("PlayEngine.run(): line wrote " + written + " bytes instead of requested " + toWrite);
                                }
                            }
                            toWrite -= written;
                            offset += written;
                            if (!m_lineStarted) {
                                if (Debug.getTracePlay()) Debug.out("PlayEngine.run(): started line");
                                m_line.start();
                                m_lineStarted = true;
                            }
                        }
                        m_position += (offset / frameSize);
                        m_positionCurrentTime = AudioUtils.getCurrentTime();
                    }
                }
            }
            if (Debug.getTracePlay()) Debug.out("PlayEngine.run(): end of play loop");
        }
        if (Debug.getTracePlay()) {
            Debug.out("PlayEngine.run(): terminated by the m_bTerminate flag.");
        }
    }

    private void mixAll(FloatSampleBuffer fsb) {
        Style style = m_style;
        int phase = m_selectedPhase;
        if (style == null) return;
        int sampleCount = style.getAudioDataSampleCount(phase);
        if (sampleCount == 0) return;
        m_position = m_position % sampleCount;
        int position = m_position;
        int remaining = fsb.getSampleCount();
        int destOffset = 0;
        while (remaining > 0) {
            if (position >= sampleCount) {
                position = 0;
            }
            int thisCount = remaining;
            if (position + thisCount > sampleCount) {
                thisCount = sampleCount - position;
                if (thisCount <= 0) {
                    if (Debug.getTracePlay()) Debug.out("PlayEngine.mixAll(): weird: position > sampleCount!");
                    break;
                }
            }
            if (m_metronome != null) {
                mixOne(m_metronome.getTockBuffer(), position, fsb, destOffset, thisCount);
            }
            if (m_recordedData != null) {
                mixOne(m_recordedData, position, fsb, destOffset, thisCount);
            }
            if (m_selectedPhase >= 0) {
                int tracks = m_style.getTrackCount();
                for (int track = 0; track < tracks; track++) {
                    if (!m_style.getTrackActuallyMuted(track)) {
                        mixOne(m_style.getCellAudioData(track, m_selectedPhase), position, fsb, destOffset, thisCount);
                    }
                }
            }
            position += thisCount;
            remaining -= thisCount;
            destOffset += thisCount;
        }
    }

    private void mixOne(FloatSampleBuffer src, int srcOffset, FloatSampleBuffer dest, int destOffset, int sampleCount) {
        if (src == null) return;
        if (srcOffset + sampleCount > src.getSampleCount()) {
            sampleCount = src.getSampleCount() - srcOffset;
        }
        if (sampleCount <= 0) return;
        Object[] srcChannels = src.getAllChannels();
        Object[] destChannels = dest.getAllChannels();
        if (srcChannels.length == 1 && destChannels.length > 1) {
            float[] srcData = (float[]) (srcChannels[0]);
            for (int destChannel = 0; destChannel < destChannels.length; destChannel++) {
                float[] destData = (float[]) (destChannels[destChannel]);
                for (int i = 0; i < sampleCount; i++) {
                    destData[destOffset + i] += srcData[srcOffset + i];
                }
            }
        } else {
            int maxChannels = srcChannels.length;
            if (maxChannels > destChannels.length) {
                maxChannels = destChannels.length;
            }
            for (int channel = 0; channel < maxChannels; channel++) {
                float[] srcData = (float[]) (srcChannels[channel]);
                float[] destData = (float[]) (destChannels[channel]);
                for (int i = 0; i < sampleCount; i++) {
                    destData[destOffset + i] += srcData[srcOffset + i];
                }
            }
        }
    }

    public void setRecordedData(FloatSampleBuffer fsb) {
        m_recordedData = fsb;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        m_propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        m_propertyChangeSupport.removePropertyChangeListener(listener);
    }

    protected void firePropertyChange(String sPropertyName, boolean oldValue, boolean newValue) {
        if (Debug.getTracePlay()) {
            Debug.out("PlayEngine.firePropertyChange(): notifies property '" + sPropertyName + "'.");
        }
        m_propertyChangeSupport.firePropertyChange(sPropertyName, oldValue, newValue);
    }
}
