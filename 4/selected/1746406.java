package glisten;

import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

/**
 *
 * @author paul
 */
public class AudioSource {

    private URL url;

    private String title;

    private SourceDataLine source;

    private float dB;

    private long current_seek_position;

    private long duration;

    private long skipped_microseconds;

    private long safe_pos_microseconds;

    AudioInputStream decoded_input_stream;

    AudioInputStream input_stream;

    AudioFormat decodedFormat;

    private java.util.List listeners;

    private boolean seeking = false;

    private boolean opening = false;

    private boolean opened = false;

    private boolean playing = false;

    private boolean closing = false;

    private boolean stopping = false;

    private boolean failed = false;

    private boolean resume = false;

    private boolean EOF = false;

    private FloatControl gainControl;

    /**
     * Url based constructor
     * @param source_url
     * @param source_title
     */
    public AudioSource(URL source_url, String source_title) {
        listeners = new java.util.ArrayList();
        title = source_title;
        url = source_url;
        Thread getDurationThread = new Thread() {

            public void run() {
                try {
                    AudioFileFormat format = AudioSystem.getAudioFileFormat(url);
                    float seconds = format.getFrameLength() / format.getFormat().getFrameRate();
                    duration = (long) (seconds * 1000000);
                } catch (Exception e) {
                    duration = 999999999;
                }
            }
        };
        getDurationThread.start();
    }

    /**
     * File based constructor
     * @param source_file
     * @param source_title
     */
    public AudioSource(File source_file, String source_title) {
        try {
            listeners = new java.util.ArrayList();
            URL source_url = source_file.toURI().toURL();
            title = source_title;
            url = source_url;
            AudioFileFormat format = AudioSystem.getAudioFileFormat(source_file);
            float seconds = format.getFrameLength() / format.getFormat().getFrameRate();
            duration = (long) (seconds * 1000000);
        } catch (Exception e) {
            e.printStackTrace();
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
                    input_stream = null;
                    try {
                        input_stream = AudioSystem.getAudioInputStream(url);
                        input_stream.mark(256000000);
                    } catch (Exception e) {
                        input_stream = null;
                        e.printStackTrace();
                    }
                    if (input_stream != null) {
                        AudioFormat baseFormat = input_stream.getFormat();
                        decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                        decoded_input_stream = AudioSystem.getAudioInputStream(decodedFormat, input_stream);
                        DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                        try {
                            source = (SourceDataLine) AudioSystem.getLine(info);
                            source.addLineListener(new LineListener() {

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
                                        _fireAudioStateEvent(AudioStateEvent.AudioState.STOPPED);
                                        if (duration <= source.getMicrosecondPosition() + 30000 + skipped_microseconds) {
                                            setPosition(0);
                                        }
                                    }
                                }
                            });
                            source.open();
                            gainControl = (FloatControl) source.getControl(FloatControl.Type.MASTER_GAIN);
                            gainControl.setValue(dB);
                            opened = true;
                            _fireAudioStateEvent(AudioStateEvent.AudioState.READY);
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
                        source.close();
                        try {
                            decoded_input_stream.close();
                            input_stream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (playing) {
                        source.stop();
                        source.flush();
                        source.close();
                        try {
                            decoded_input_stream.close();
                            input_stream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    closing = false;
                    _fireAudioStateEvent(AudioStateEvent.AudioState.CLOSED);
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
                    while (playing) {
                        try {
                            Thread.yield();
                        } catch (Exception e) {
                        }
                    }
                    setPosition(0);
                }
            };
            stopThread.start();
        }
    }

    public void pause() {
        if (playing) {
            stopping = true;
            _fireAudioStateEvent(AudioStateEvent.AudioState.STOPPING);
        }
    }

    public void play() {
        if (opened && (!playing) && (!seeking)) {
            stopping = false;
            playing = true;
            _fireAudioStateEvent(AudioStateEvent.AudioState.PLAYING);
            java.lang.Thread playThread = new java.lang.Thread() {

                @Override
                public void run() {
                    byte[] data = new byte[4096];
                    source.start();
                    int nBytesRead = 0, nBytesWritten = 0;
                    while (nBytesRead != -1 && !stopping) {
                        try {
                            nBytesRead = decoded_input_stream.read(data, 0, data.length);
                            if (nBytesRead != -1) {
                                nBytesWritten = source.write(data, 0, nBytesRead);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            nBytesRead = -1;
                        }
                    }
                    if (!stopping) {
                        source.drain();
                        if (duration == 999999999) {
                            duration = source.getMicrosecondPosition() + skipped_microseconds;
                        }
                    }
                    source.stop();
                    source.flush();
                    playing = false;
                    stopping = false;
                    _fireAudioStateEvent(AudioStateEvent.AudioState.STOPPED);
                }
            };
            playThread.start();
        }
    }

    public String getTitle() {
        return title;
    }

    public long getDuration() {
        return duration;
    }

    public long getPosition() {
        long position = 0;
        if (opened && !seeking) {
            position = source.getMicrosecondPosition() + skipped_microseconds;
        }
        if (seeking) {
            position = current_seek_position;
        }
        return position;
    }

    public long getPositionPercentage(float percent) {
        long position = 0;
        if (opened) {
            position = (long) ((float) duration * percent);
        }
        return position;
    }

    public URL getURL() {
        return url;
    }

    public void setPosition(long microseconds) {
        if (!seeking) {
            seeking = true;
            current_seek_position = 0;
            _fireAudioStateEvent(AudioStateEvent.AudioState.SEEKING);
            try {
                skipped_microseconds = microseconds;
                source.close();
                decoded_input_stream = null;
                input_stream = null;
                try {
                    input_stream = AudioSystem.getAudioInputStream(url);
                    input_stream.mark(256000000);
                } catch (Exception e) {
                    input_stream = null;
                    e.printStackTrace();
                }
                if (input_stream != null) {
                    AudioFormat baseFormat = input_stream.getFormat();
                    decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                    decoded_input_stream = AudioSystem.getAudioInputStream(decodedFormat, input_stream);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                    try {
                        source = (SourceDataLine) AudioSystem.getLine(info);
                        source.addLineListener(new LineListener() {

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
                                    _fireAudioStateEvent(AudioStateEvent.AudioState.STOPPED);
                                    if (duration <= source.getMicrosecondPosition() + 30000 + skipped_microseconds) {
                                        setPosition(0);
                                    }
                                }
                            }
                        });
                        source.open();
                        gainControl = (FloatControl) source.getControl(FloatControl.Type.MASTER_GAIN);
                        gainControl.setValue(dB);
                    } catch (Exception e) {
                    }
                }
                input_stream.mark(256000000);
                source.open();
                EOF = false;
                int frame_size = decoded_input_stream.getFormat().getFrameSize();
                double frames_per_microsecond = (decoded_input_stream.getFormat().getFrameRate()) / 1000000.0;
                long frames_to_skip = (long) (frames_per_microsecond * microseconds);
                long bytes_to_skip = frames_to_skip * frame_size;
                byte[] data = new byte[8192];
                long nBytesRead = 0, nBytesWritten = 0;
                long bytes_skipped = 0;
                while (bytes_to_skip > 0 && nBytesRead != -1) {
                    try {
                        if (bytes_to_skip >= data.length) {
                            nBytesRead = decoded_input_stream.read(data, 0, data.length);
                            bytes_to_skip -= nBytesRead;
                            bytes_skipped += nBytesRead;
                            current_seek_position = (long) ((double) (bytes_skipped / frame_size) / frames_per_microsecond);
                            if (nBytesRead == -1) {
                                if (duration == 999999999) {
                                    long frames_skipped = bytes_skipped / frame_size;
                                    duration = (long) (frames_skipped / frames_per_microsecond);
                                    EOF = true;
                                    setPosition(0);
                                }
                            }
                        } else {
                            bytes_to_skip = 0;
                        }
                    } catch (Exception e) {
                        if (duration == 999999999) {
                            long frames_skipped = bytes_skipped / frame_size;
                            duration = (long) (frames_skipped / frames_per_microsecond);
                        }
                        e.printStackTrace();
                        nBytesRead = -1;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            seeking = false;
            if (EOF == true) {
                setPosition(0);
            }
            _fireAudioStateEvent(AudioStateEvent.AudioState.READY);
        }
    }

    public void setPositionPercentageSafe(float percent) {
        if (opened && !seeking) {
            safe_pos_microseconds = (long) ((float) duration * percent);
            if (playing) {
                resume = true;
            } else {
                resume = false;
            }
            stopping = true;
            _fireAudioStateEvent(AudioStateEvent.AudioState.STOPPING);
            java.lang.Thread setThread = new java.lang.Thread() {

                @Override
                public void run() {
                    while (playing) {
                        try {
                            Thread.sleep(0);
                        } catch (Exception e) {
                        }
                    }
                    setPosition(safe_pos_microseconds);
                    if (resume) {
                        play();
                    }
                }
            };
            setThread.start();
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
