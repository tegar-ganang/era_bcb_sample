package unclej.utasks.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author scottv
 */
class StreamCopier extends Thread {

    public StreamCopier(InputStream input, OutputStream output, boolean close) {
        this.input = input;
        this.output = output;
        this.close = close;
        setName("StreamCopier " + input);
    }

    public void run() {
        if (this.close) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException x) {
                interrupt();
            }
        }
        try {
            copy();
        } catch (IOException x) {
            x.printStackTrace();
        }
    }

    public void shutdown(int delay) {
        synchronized (this) {
            dieNow = true;
            notify();
        }
        try {
            join(delay);
        } catch (InterruptedException x) {
            interrupt();
        }
    }

    private void copy() throws IOException {
        int numAvailable = 0;
        int readSize = 0;
        int zeroCnt = 0;
        byte[] buf = new byte[READSIZE];
        while (!dieNow || zeroCnt <= 5) {
            if (numAvailable <= 0) {
                numAvailable = input.available();
            }
            if (numAvailable == 0) {
                ++zeroCnt;
                waitSome();
            } else {
                readSize = (numAvailable > READSIZE) ? READSIZE : numAvailable;
                numAvailable -= readSize;
                readSize = input.read(buf, 0, readSize);
                write(buf, readSize);
                output.flush();
                blackBox.put(buf, 0, readSize);
            }
        }
        closeStreams();
    }

    private void write(byte[] buf, int readSize) throws IOException {
        for (int i = 0; i < readSize; ++i) {
            int b = buf[i] & 0xff;
            if (b == '\\') {
                output.write(b);
                output.write(b);
            } else if ((b < 32 && b != 9 && b != 10 && b != 13) || b >= 127) {
                output.write('\\');
                output.write('x');
                output.write(HEX[b / 16]);
                output.write(HEX[b % 16]);
            } else {
                output.write(b);
            }
        }
    }

    private void waitSome() {
        try {
            synchronized (this) {
                wait(WAITTIME);
            }
        } catch (InterruptedException x) {
            interrupt();
        }
    }

    private void closeStreams() throws IOException {
        if (close) {
            input.close();
            output.close();
        } else {
            output.flush();
        }
    }

    /**
   * Returns a String containing only the most recent copied characters.
   * @return the most recent copied characters as a String
   */
    public String getBlackBox() {
        return blackBox.toString();
    }

    private static final int READSIZE = 1024;

    private static final int WAITTIME = 100;

    private boolean dieNow = false;

    private InputStream input;

    private OutputStream output;

    private boolean close;

    private final BlackBox blackBox = new BlackBox(1024);

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
}
