package org.jsresources.apps.ripper;

import java.io.IOException;
import javax.sound.sampled.*;

/** 
 */
public class AsynchronousAudioInputStream extends AudioInputStream implements Runnable {

    private TCircularBuffer buffer;

    private Thread thread;

    private AudioInputStream ais;

    private long currPos;

    public AsynchronousAudioInputStream(AudioInputStream ais, int bufferSize) {
        super(ais, ais.getFormat(), AudioSystem.NOT_SPECIFIED);
        this.ais = ais;
        buffer = new TCircularBuffer(bufferSize, true, true, null);
        currPos = 0;
    }

    public int read() throws IOException {
        if (getFormat().getFrameSize() != 1) {
            throw new IOException("frame size must be 1 to read a single byte");
        }
        byte[] temp = new byte[1];
        int result = read(temp);
        if (result == -1) {
            return -1;
        }
        if (result == 0) {
            return -1;
        }
        return temp[0];
    }

    public int read(byte[] abData, int nOffset, int nLength) throws IOException {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
        nLength = nLength - (nLength % getFrameSize());
        int result = buffer.read(abData, nOffset, nLength);
        if (result > 0) {
            currPos += result;
        }
        return result;
    }

    public void run() {
        byte[] bytes = new byte[2352];
        while (buffer.isOpen()) {
            try {
                int read = ais.read(bytes);
                if (read == -1) {
                    close();
                } else {
                    buffer.write(bytes, 0, read);
                }
            } catch (IOException ioe) {
                try {
                    close();
                } catch (IOException ioe2) {
                }
            }
        }
        synchronized (this) {
            thread = null;
            notifyAll();
        }
    }

    public long skip(long nSkip) throws IOException {
        return 0;
    }

    public int available() throws IOException {
        return buffer.availableRead();
    }

    public void close() throws IOException {
        buffer.flush();
        buffer.close();
        synchronized (this) {
            if (thread != null) {
                try {
                    wait(15000);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    public void mark(int readlimit) {
    }

    public void reset() throws IOException {
    }

    public boolean markSupported() {
        return false;
    }

    private int getFrameSize() {
        return getFormat().getFrameSize();
    }

    public int getBufferPercent() {
        return buffer.availableRead() * 100 / buffer.getSize();
    }

    public long getPositionInMs() {
        return (long) (currPos / getFormat().getFrameRate() * 1000 / getFormat().getFrameSize());
    }
}
