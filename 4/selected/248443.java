package org.crap4j.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class StreamCopier extends Thread {

    private static int READSIZE = 1024;

    private static int WAITTIME = 100;

    private boolean dieNow = false;

    private InputStream is;

    private OutputStream os;

    private boolean close;

    public StreamCopier(InputStream i, OutputStream o, boolean c) {
        assert i != null;
        assert o != null;
        is = i;
        os = o;
        close = c;
        waitOnClose();
        setName("StreamCopier " + i);
        setDaemon(true);
        start();
    }

    private void waitOnClose() {
        if (close) {
            try {
                Thread.sleep(100);
            } catch (Exception ignore) {
            }
        }
    }

    public void run() {
        try {
            copy(is, os, close);
        } catch (IOException ex) {
            if (!dieNow) {
                ex.printStackTrace();
            }
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
        }
    }

    private void copy(InputStream input, OutputStream output, boolean close) throws IOException {
        int numAvailable = 0;
        int readSize = 0;
        int zeroCnt = 0;
        byte[] buf = new byte[READSIZE];
        while (true) {
            if (numAvailable <= 0) {
                numAvailable = input.available();
            }
            if (numAvailable == 0) {
                if (dieNow == true && zeroCnt++ > 5) {
                    break;
                }
                try {
                    synchronized (this) {
                        wait(WAITTIME);
                    }
                } catch (Exception ignore) {
                }
            } else {
                readSize = (numAvailable > READSIZE) ? READSIZE : numAvailable;
                numAvailable -= readSize;
                int read = input.read(buf, 0, readSize);
                output.write(buf, 0, read);
                output.flush();
            }
        }
        if (close) {
            input.close();
            output.close();
        } else {
            output.flush();
        }
    }
}
