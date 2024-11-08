package uk.co.simphoney.audio;

import uk.co.simphoney.audio.dft.AudioFlags;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;

public class BufferedAudioProcess {

    public static final int OVERFLOW = -1;

    float buff[];

    int cacheSize;

    long inPtr = 0;

    long outPtr = 0;

    Thread outBlockingThread = null;

    public final AudioProcess in = new In();

    public final AudioProcess out = new Out();

    private int required;

    private boolean itWasMe = false;

    private boolean disconnected = false;

    int overflowCount = 0;

    private boolean blocking = false;

    public BufferedAudioProcess(int cacheSize) {
        this.cacheSize = cacheSize;
        buff = new float[cacheSize];
    }

    class In implements AudioProcess {

        int cnt;

        public void close() throws Exception {
        }

        public void open() throws Exception {
        }

        public int processAudio(AudioBuffer buffer) {
            int n = buffer.getSampleCount();
            if (inPtr + n - outPtr > cacheSize) {
                System.out.println(" OVERFLOW " + overflowCount++);
                return OVERFLOW;
            }
            int inCy0 = (int) (inPtr % cacheSize);
            int inCy1 = (int) ((inPtr + n) % cacheSize);
            if (inCy1 > inCy0) {
                System.arraycopy(buffer.getChannel(0), 0, buff, inCy0, n);
            } else {
                System.arraycopy(buffer.getChannel(0), 0, buff, inCy0, n - inCy1);
                System.arraycopy(buffer.getChannel(0), n - inCy1, buff, 0, inCy1);
            }
            inPtr += n;
            if (blocking) {
                if (inPtr - outPtr > required) {
                    itWasMe = true;
                    outBlockingThread.interrupt();
                }
            }
            return AUDIO_OK;
        }
    }

    class Out implements AudioProcess {

        public synchronized int processAudio(AudioBuffer buffer) {
            required = buffer.getSampleCount();
            if (inPtr - outPtr < required) {
                outBlockingThread = Thread.currentThread();
                try {
                    while (Thread.currentThread().interrupted()) ;
                    blocking = true;
                    wait();
                } catch (InterruptedException e) {
                    blocking = false;
                    if (itWasMe) {
                        itWasMe = false;
                    } else {
                        e.printStackTrace();
                        e.getCause().printStackTrace();
                        inPtr = 0;
                        outPtr = 0;
                        itWasMe = false;
                        return AudioFlags.INTERRUPTED;
                    }
                }
                assert (inPtr - outPtr >= required);
            }
            int outCy0 = (int) (outPtr % cacheSize);
            int outCy1 = (int) ((outPtr + required) % cacheSize);
            if (outCy1 > outCy0) {
                System.arraycopy(buff, outCy0, buffer.getChannel(0), 0, required);
            } else {
                System.arraycopy(buff, outCy0, buffer.getChannel(0), 0, required - outCy1);
                System.arraycopy(buff, 0, buffer.getChannel(0), required - outCy1, outCy1);
            }
            outPtr += required;
            return AUDIO_OK;
        }

        public void close() throws Exception {
        }

        public void open() throws Exception {
        }
    }

    class OverflowException extends Exception {

        public String toString() {
            return " BufferedAuioProcess:  Overflow ";
        }
    }

    public int getLag() {
        return (int) (inPtr - outPtr);
    }
}
