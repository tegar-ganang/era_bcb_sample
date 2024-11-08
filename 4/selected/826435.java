package tristero.util;

import java.io.*;

public class Conduit extends Thread {

    protected static int bufflen = 131072;

    InputStream sin;

    OutputStream sout;

    int thresh;

    Lock lck;

    PumpListener listener;

    public static void pump(InputStream in, OutputStream out) throws IOException {
        pump(in, out, 0, null, null);
    }

    public static void pump(InputStream in, OutputStream out, int threshold, Lock lock, PumpListener listener) throws IOException {
        int totalRead = 0;
        boolean fired = (threshold == 0);
        byte[] buffer = new byte[bufflen];
        int bytes_read = 0;
        int times = 0;
        while (bytes_read != -1) {
            times = times + 1;
            bytes_read = in.read(buffer);
            if (bytes_read == -1) break;
            totalRead = totalRead + bytes_read;
            out.write(buffer, 0, bytes_read);
            if (!fired && totalRead > threshold) {
                if (lock != null) lock.unlock();
                fired = true;
            }
            if (fired && bytes_read == bufflen) {
                bufflen = bufflen * 2;
                buffer = new byte[bufflen];
                System.err.println("Increased buffer: " + bufflen);
            }
        }
        if (lock != null) lock.unlock();
        if (listener != null) listener.fireDone();
    }

    public Conduit(InputStream in, OutputStream out) {
        this(in, out, 0, null);
    }

    public Conduit(InputStream in, OutputStream out, int i, Lock l) {
        sin = in;
        sout = out;
        thresh = i;
        lck = l;
    }

    public Conduit(InputStream in, OutputStream out, int i, Lock l, PumpListener dl) {
        sin = in;
        sout = out;
        thresh = i;
        lck = l;
        listener = dl;
    }

    public void run() {
        try {
            Conduit.pump(sin, sout, thresh, lck, listener);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                sout.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        synchronized (this) {
            notifyAll();
        }
    }
}
