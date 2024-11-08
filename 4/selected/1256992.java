package glisten;

import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

/**
 *
 * @author paul
 */
public class AudioSource_old {

    private URL url;

    private String title;

    private Clip clip;

    private float dB;

    AudioInputStream decoded_input_stream;

    private java.util.List listeners;

    private boolean opening = false;

    private boolean opened = false;

    private boolean playing = false;

    private boolean closing = false;

    private boolean stopping = false;

    private boolean failed = false;

    private FloatControl gainControl;

    /**
     * This is the only constructor it takes in the source URL for the
     * thread.
     * @param source_url
     */
    public AudioSource_old(URL source_url, String source_title) {
        listeners = new java.util.ArrayList();
        title = source_title;
        url = source_url;
    }

    public AudioSource_old(File source_file, String source_title) {
        try {
            listeners = new java.util.ArrayList();
            URL source_url = source_file.toURI().toURL();
            title = source_title;
            url = source_url;
        } catch (Exception e) {
            url = null;
        }
    }

    public void checkURL() {
        if (url == null) {
            _fireAudioStateEvent(AudioStateEvent.AudioState.INVALID_URL);
            failed = true;
        }
    }

    public void checkAudioFormat() {
        if (!(url.getFile().endsWith(".mp3") || url.getFile().endsWith(".wav") || url.getFile().endsWith(".aiff") || url.getFile().endsWith(".au"))) {
            _fireAudioStateEvent(AudioStateEvent.AudioState.UNSUPPORTED_FORMAT);
            failed = true;
            url = null;
        }
    }

    public void open() {
        if ((!opening) && (!opened) && url != null) {
            opening = true;
            _fireAudioStateEvent(AudioStateEvent.AudioState.OPENING);
            Thread openThread = new Thread() {

                public void run() {
                    decoded_input_stream = null;
                    AudioInputStream input_stream = null;
                    try {
                        System.out.println(url.toString());
                        input_stream = AudioSystem.getAudioInputStream(url);
                    } catch (Exception e) {
                        input_stream = null;
                        e.printStackTrace();
                    }
                    if (input_stream != null) {
                        AudioFormat baseFormat = input_stream.getFormat();
                        AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                        decoded_input_stream = AudioSystem.getAudioInputStream(decodedFormat, input_stream);
                        DataLine.Info info = new DataLine.Info(Clip.class, decodedFormat);
                        try {
                            clip = (Clip) AudioSystem.getLine(info);
                            clip.addLineListener(new LineListener() {

                                public void update(LineEvent evt) {
                                    LineEvent.Type eventType = evt.getType();
                                    if (eventType == LineEvent.Type.OPEN) {
                                        opened = true;
                                    }
                                    if (eventType == LineEvent.Type.CLOSE) {
                                        opened = false;
                                    }
                                    if (eventType == LineEvent.Type.START) {
                                        playing = true;
                                    }
                                    if (eventType == LineEvent.Type.STOP) {
                                        playing = false;
                                        if (clip.getMicrosecondLength() == clip.getMicrosecondPosition()) {
                                            _fireAudioStateEvent(AudioStateEvent.AudioState.STOPPED);
                                            clip.setMicrosecondPosition(0);
                                        }
                                    }
                                }
                            });
                            clip.open(decoded_input_stream);
                            try {
                                input_stream.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Runtime.getRuntime().gc();
                            gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                            gainControl.setValue(dB);
                            _fireAudioStateEvent(AudioStateEvent.AudioState.READY);
                            opened = true;
                        } catch (Exception e) {
                            _fireAudioStateEvent(AudioStateEvent.AudioState.OPEN_FAILED);
                            failed = true;
                        }
                    }
                    opening = false;
                }
            };
            openThread.start();
        }
    }

    public void close() {
        if (!closing) {
            closing = true;
            _fireAudioStateEvent(AudioStateEvent.AudioState.CLOSING);
            java.lang.Thread closeThread = new java.lang.Thread() {

                @Override
                public void run() {
                    if (opening) {
                        while (!opened) {
                            Thread.yield();
                        }
                    }
                    if (!opened) {
                    } else if (opened && (!playing)) {
                        clip.close();
                        try {
                            decoded_input_stream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (playing) {
                        clip.stop();
                        clip.flush();
                        clip.close();
                        try {
                            decoded_input_stream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    closing = false;
                    _fireAudioStateEvent(AudioStateEvent.AudioState.CLOSED);
                    Runtime.getRuntime().gc();
                }
            };
            closeThread.start();
        }
    }

    public void stop() {
        if (playing) {
            stopping = true;
            _fireAudioStateEvent(AudioStateEvent.AudioState.STOPPING);
            java.lang.Thread stopThread = new java.lang.Thread() {

                @Override
                public void run() {
                    clip.stop();
                    playing = false;
                    stopping = false;
                    _fireAudioStateEvent(AudioStateEvent.AudioState.STOPPED);
                }
            };
            stopThread.start();
        }
    }

    public void play() {
        if (opened && (!playing) && clip.getMicrosecondLength() != clip.getMicrosecondPosition()) {
            playing = true;
            _fireAudioStateEvent(AudioStateEvent.AudioState.PLAYING);
            clip.start();
        }
    }

    public String getTitle() {
        return title;
    }

    public long getDuration() {
        long duration = 0;
        if (opened) {
            duration = clip.getMicrosecondLength();
        }
        return duration;
    }

    public long getPosition() {
        long position = 0;
        if (opened) {
            position = clip.getMicrosecondPosition();
        }
        return position;
    }

    public long getPositionPercentage(float percent) {
        long position = 0;
        if (opened) {
            position = (long) ((float) clip.getMicrosecondLength() * percent);
        }
        return position;
    }

    public URL getURL() {
        return url;
    }

    public void setPosition(long microseconds) {
        if (opened) {
            clip.setMicrosecondPosition(microseconds);
        }
    }

    public void setPositionPercentage(float percent) {
        if (opened) {
            long microseconds = (long) ((float) clip.getMicrosecondLength() * percent);
            clip.setMicrosecondPosition(microseconds);
        }
    }

    public void setVolume(double gain) {
        this.dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
        if (opened) {
            gainControl.setValue(dB);
        }
    }

    public boolean isOpenFailed() {
        return failed;
    }

    public boolean isOpening() {
        return opening;
    }

    public boolean isClosing() {
        return closing;
    }

    public boolean isStopping() {
        return stopping;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isOpen() {
        return opened;
    }

    public synchronized void attachAudioStateListener(AudioStateListener asl) {
        listeners.add(asl);
    }

    public synchronized void removeAudioStateListeners() {
        listeners.clear();
    }

    private synchronized void _fireAudioStateEvent(AudioStateEvent.AudioState audioState) {
        AudioStateEvent ase = new AudioStateEvent(this, audioState);
        java.util.Iterator listeners_iter = listeners.iterator();
        while (listeners_iter.hasNext()) {
            ((AudioStateListener) listeners_iter.next()).AudioStateReceived(ase);
        }
    }
}
