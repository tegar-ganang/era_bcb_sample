package edu.xtec.jclic.media;

import java.io.ByteArrayOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.Line.Info;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * This class extends {@link AudioBuffer} using the javax.sound.sampled package.
 * @author Francesc Busquets (fbusquets@xtec.net)
 * @version 1.0
 */
public class JavaSoundAudioBuffer extends AudioBuffer {

    /**
     * Default sample rate used in recordings
     */
    public static final float RATE = 11025.0F;

    /**
     * Default resolution used in recordings
     */
    public static final int BITS = 16;

    /**
     * Default number of channels used in recordings
     */
    public static final int CHANNELS = 1;

    /**
     * Default buffer size
     */
    public static final int LINE_BUFFER = 10000;

    /**
     * Default size for small buffer used in read loops
     */
    public static final int STEP_BUFFER = 5000;

    /**
     * Line used for recording audio
     */
    private static TargetDataLine m_targetLine;

    /**
     * Line used for playing recorded audio
     */
    private static SourceDataLine m_sourceLine;

    /**
     * Thread used to record audio. Static because only one instance can stay making
     * use of the sound hardware.
     */
    private static RecordThread recordThread;

    /**
     * Thread used to play audio. Static because only one instance can stay making
     * use of the audio hardware.
     */
    private static PlayThread playThread;

    /**
     * Timer used to stop the record thread when the maximum time is achieved
     */
    private static Timer recordTimer;

    /**
     * <CODE>true</CODE> only when all {@link Line} members are build
     */
    private static boolean initialized;

    /**
     * Array that holds the recorded audio data
     */
    private byte[] m_buffer;

    /**
     * Creates new JavaSoundAudioBuffer
     * @param seconds Maximum amount of seconds allowed for recording
     * @throws Exception If something goes wrong...
     */
    public JavaSoundAudioBuffer(int seconds) throws Exception {
        super(seconds);
        initialize();
    }

    public static void initialize() throws Exception {
        if (!initialized) buildLines();
    }

    /**
     * Looks for available formats and lines, and builds <CODE>m_sourceLine</CODE> and <CODE>m_targetLine</CODE> members
     * @throws Exception If it was unable to build the lines
     */
    protected static void buildLines() throws Exception {
        Vector vOptimal = new Vector();
        Vector vAll = new Vector();
        Info[] tl = AudioSystem.getTargetLineInfo(new Info(TargetDataLine.class));
        for (int k = 0; k < tl.length; k++) {
            if (tl[k] instanceof javax.sound.sampled.DataLine.Info) {
                AudioFormat[] formats = ((javax.sound.sampled.DataLine.Info) tl[k]).getFormats();
                for (int j = 0; j < formats.length; j++) {
                    AudioFormat f = formats[j];
                    if (f.getSampleRate() == AudioSystem.NOT_SPECIFIED || f.getSampleRate() >= 8000.0F) vAll.add(f);
                    if (f.getChannels() == CHANNELS && f.getSampleSizeInBits() == BITS && (f.getSampleRate() == AudioSystem.NOT_SPECIFIED || f.getSampleRate() == RATE)) vOptimal.add(f);
                }
            }
        }
        if (vAll.isEmpty()) {
            throw new Exception("Unable to find any available TargetDataLine for recording");
        }
        javax.sound.sampled.DataLine.Info dli;
        AudioFormat[] af;
        if (!vOptimal.isEmpty()) {
            af = (AudioFormat[]) vOptimal.toArray(new AudioFormat[vOptimal.size()]);
            dli = new javax.sound.sampled.DataLine.Info(TargetDataLine.class, af, LINE_BUFFER, LINE_BUFFER + 1000);
            try {
                m_targetLine = (TargetDataLine) AudioSystem.getLine(dli);
                m_targetLine.open();
            } catch (Exception ex) {
                m_targetLine = null;
            }
        }
        if (m_targetLine == null) {
            af = (AudioFormat[]) vAll.toArray(new AudioFormat[vAll.size()]);
            dli = new javax.sound.sampled.DataLine.Info(TargetDataLine.class, af, LINE_BUFFER, LINE_BUFFER + 1000);
            m_targetLine = (TargetDataLine) AudioSystem.getLine(dli);
            m_targetLine.open();
        }
        dli = new javax.sound.sampled.DataLine.Info(SourceDataLine.class, m_targetLine.getFormat());
        m_sourceLine = (SourceDataLine) AudioSystem.getLine(dli);
        m_sourceLine.open();
        initialized = true;
    }

    /**
     * Used for recording data. The thread stops itself when <CODE>running</CODE>
     * is set to <CODE>false</CODE>.
     */
    class RecordThread extends Thread {

        /**
         * When <CODE>false</CODE>, the thread must be stopped as soon as possible.
         */
        public boolean running;

        /**
         * Creates new RecordThread
         */
        public RecordThread() {
            running = false;
        }

        /**
         * Thread main method
         */
        public void run() {
            AudioBuffer.activeAudioBuffer = JavaSoundAudioBuffer.this;
            running = true;
            m_buffer = null;
            ByteArrayOutputStream out = new ByteArrayOutputStream(LINE_BUFFER);
            try {
                int avail, numBytesRead;
                byte[] data = new byte[STEP_BUFFER];
                m_targetLine.start();
                do {
                    avail = m_targetLine.available();
                    if (avail > 0) {
                        numBytesRead = m_targetLine.read(data, 0, Math.min(avail, data.length));
                        out.write(data, 0, numBytesRead);
                    }
                    Thread.currentThread().yield();
                } while (running);
                if (recordTimer != null) {
                    recordTimer.cancel();
                    recordTimer = null;
                }
            } catch (Exception ex) {
                System.err.println("JavaSound recording error:\n" + ex);
            }
            m_targetLine.stop();
            m_targetLine.flush();
            m_buffer = out.toByteArray();
            AudioBuffer.hideRecordingCursor();
            AudioBuffer.activeAudioBuffer = null;
            recordThread = null;
            running = false;
        }
    }

    /**
     * Used for playing data. The thread stops itself when <CODE>running</CODE>
     * is set to <CODE>false</CODE>, or when all audio data was played.
     */
    class PlayThread extends Thread {

        /**
         * When <CODE>false</CODE>, the thread must be stopped as soon as possible.
         */
        public boolean running;

        /**
         * In some cases, the data stored in <CODE>m_buffer</CODE> was corrupted after
         * calling to <CODE>m_sourceLine.write</CODE>. A workaround to this JavaSond
         * bug is to make a duplicate of the data and discard it after used.
         */
        byte[] buf;

        /**
         * Creates new PlayThread
         */
        public PlayThread() {
            running = false;
            buf = new byte[m_buffer.length];
            System.arraycopy(m_buffer, 0, buf, 0, m_buffer.length);
        }

        /**
         * Thread main method
         */
        public void run() {
            running = true;
            try {
                m_sourceLine.start();
                int l = m_sourceLine.getBufferSize() / 2;
                int p = 0;
                int remainingData = buf.length;
                while (running && remainingData > 0) {
                    int k = m_sourceLine.write(buf, p, Math.min(l, remainingData));
                    p += k;
                    remainingData -= k;
                    Thread.currentThread().yield();
                }
            } catch (Exception ex) {
                System.err.println("JavaSound playing error:\n" + ex);
            }
            m_sourceLine.drain();
            m_sourceLine.stop();
            playThread = null;
            running = false;
        }
    }

    /**
     * Starts recording
     * @throws Exception If something goes wrong.
     */
    public void record() throws Exception {
        if (!initialized) {
            AudioBuffer.hideRecordingCursor();
            AudioBuffer.activeAudioBuffer = null;
            return;
        }
        stop();
        recordThread = new RecordThread();
        recordThread.start();
        recordTimer = new Timer();
        recordTimer.schedule(new TimerTask() {

            public void run() {
                recordTimer = null;
                if (recordThread != null) {
                    recordThread.running = false;
                }
            }
        }, m_seconds * 1000);
    }

    /**
     * Plays recorded audio data, if any.
     * @throws Exception If something goes wrong
     */
    public void play() throws Exception {
        if (!initialized) return;
        stop();
        if (m_buffer != null && m_buffer.length > 0) {
            playThread = new PlayThread();
            playThread.start();
        }
    }

    /**
     * Checks if the AudioBuffer is currently playing or recording sound.
     * @return <CODE>true</CODE> if playing or recording, <CODE>false</CODE> otherwise
     */
    private boolean isRunning() {
        return (playThread != null || recordThread != null);
    }

    /**
     * If running, gently stops play and record threads
     */
    public void stop() {
        if (recordThread != null) recordThread.running = false;
        if (playThread != null) playThread.running = false;
        while (recordThread != null || playThread != null) {
            Thread.currentThread().yield();
        }
    }

    /**
     * Stops playing or recording and clears all recorded data
     */
    protected void clear() {
        stop();
        m_buffer = null;
    }
}
