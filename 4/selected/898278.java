package org.jsresources.apps.jam.audio;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.jsresources.apps.jam.Debug;
import org.jsresources.apps.jam.util.PlayableObject;

/**	Plays patterns.
 *
 *	@author Matthias Pfisterer
 */
public class PreListen implements LineListener {

    private static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0F, 16, 2, 4, 44100.0F, false);

    private File m_file;

    private Clip m_clip;

    private boolean m_bPlaying;

    private boolean m_bLooping;

    private PropertyChangeSupport m_propertyChangeSupport;

    /**	Plays soundfiles.
	 *	The soundfiles with the given names are played.
	 */
    public PreListen() {
        if (Debug.getTraceAudio()) {
            Debug.out("PreListen.<init>(): begin");
        }
        m_propertyChangeSupport = new PropertyChangeSupport(this);
        m_bPlaying = false;
        m_bLooping = false;
        if (Debug.getTraceAudio()) {
            Debug.out("PreListen.<init>(): end");
        }
    }

    /** Sets the file to play.
	    @param file the file to play.

	    @return true, if the file can be played, false otherwise.
	*/
    public boolean setFile(File file) {
        boolean bSuccess = true;
        m_file = file;
        if (Debug.getTraceAudio()) {
            Debug.out("PreListen.start(): begin");
        }
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(m_file);
        } catch (UnsupportedAudioFileException e) {
            if (Debug.getTraceAllExceptions()) {
                Debug.out(e);
            }
            bSuccess = false;
        } catch (IOException e) {
            if (Debug.getTraceAllExceptions()) {
                Debug.out(e);
            }
            bSuccess = false;
        }
        if (bSuccess) {
            AudioFormat format = audioInputStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            boolean bIsSupportedDirectly = AudioSystem.isLineSupported(info);
            if (!bIsSupportedDirectly) {
                AudioFormat sourceFormat = format;
                AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels() * (16 / 8), sourceFormat.getSampleRate(), false);
                if (Debug.getTraceAudio()) {
                    Debug.out("PreListen.start(): source format: " + sourceFormat);
                    Debug.out("PreListen.start(): target format: " + targetFormat);
                }
                audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
                format = audioInputStream.getFormat();
                if (Debug.getTraceAudio()) {
                    Debug.out("PreListen.start(): converted AIS: " + audioInputStream);
                }
                if (Debug.getTraceAudio()) {
                    Debug.out("PreListen.start(): converted format: " + format);
                }
                info = new DataLine.Info(Clip.class, format);
            }
            try {
                m_clip = (Clip) AudioSystem.getLine(info);
                m_clip.addLineListener(this);
                m_clip.open(audioInputStream);
            } catch (LineUnavailableException e) {
                if (Debug.getTraceAllExceptions()) {
                    Debug.out(e);
                }
                bSuccess = false;
            } catch (IllegalArgumentException e) {
                if (Debug.getTraceAllExceptions()) {
                    Debug.out(e);
                }
                bSuccess = false;
            } catch (IOException e) {
                if (Debug.getTraceAllExceptions()) {
                    Debug.out(e);
                }
                bSuccess = false;
            }
        }
        return bSuccess;
    }

    public void setPlaying(boolean bPlaying) {
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
        if (m_clip != null) {
            int nLoopCount = isLooping() ? Clip.LOOP_CONTINUOUSLY : 0;
            m_clip.loop(nLoopCount);
        }
    }

    public boolean isLooping() {
        return m_bLooping;
    }

    private void start() {
        if (Debug.getTraceAudio()) {
            Debug.out("PreListen.start(): begin");
        }
        int nLoopCount = isLooping() ? Clip.LOOP_CONTINUOUSLY : 0;
        m_clip.loop(nLoopCount);
    }

    public void stop() {
        m_clip.stop();
    }

    public void update(LineEvent event) {
        if (Debug.getTraceAudio()) {
            Debug.out("PreListen.update(): called with " + event);
        }
        LineEvent.Type type = event.getType();
        if (type == LineEvent.Type.START) {
            setPlayingImpl(true);
        } else if (type == LineEvent.Type.STOP) {
            setPlayingImpl(false);
            m_clip.close();
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        m_propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        m_propertyChangeSupport.removePropertyChangeListener(listener);
    }

    protected void firePropertyChange(String sPropertyName, boolean oldValue, boolean newValue) {
        if (Debug.getTraceAudio()) {
            Debug.out("PlayEngine.firePropertyChange(): notifies property '" + sPropertyName + "'.");
        }
        m_propertyChangeSupport.firePropertyChange(sPropertyName, oldValue, newValue);
    }
}
