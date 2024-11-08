package org.tn5250j;

import java.io.*;
import java.net.*;

public class DataStreamProducer implements Runnable {

    private BufferedInputStream bin;

    private ByteArrayOutputStream baosin;

    private Thread me;

    private byte[] saveStream;

    private DataStreamQueue dsq;

    private tnvt vt;

    private byte[] abyte2;

    public DataStreamProducer(tnvt vt, BufferedInputStream in, DataStreamQueue queue, byte[] init) {
        bin = in;
        this.vt = vt;
        baosin = new ByteArrayOutputStream();
        dsq = queue;
        abyte2 = init;
    }

    public void setInputStream(ByteArrayOutputStream is) {
        baosin = is;
    }

    public void setQueue(DataStreamQueue queue) {
        dsq = queue;
    }

    public final void run() {
        boolean done = false;
        me = Thread.currentThread();
        try {
            loadStream(abyte2, 0);
        } catch (IOException ioef) {
            System.out.println(" run() " + ioef.getMessage());
        }
        while (!done) {
            try {
                byte[] abyte0 = readIncoming();
                loadStream(abyte0, 0);
            } catch (SocketException se) {
                System.out.println(se.getMessage());
                done = true;
                vt.disconnect();
            } catch (IOException ioe) {
                if (me.isInterrupted()) done = true;
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                if (me.isInterrupted()) done = true;
            }
        }
    }

    private final void loadStream(byte abyte0[], int i) throws IOException {
        int j = 0;
        int size = 0;
        if (saveStream == null) {
            j = (abyte0[i] & 0xff) << 8 | abyte0[i + 1] & 0xff;
            size = abyte0.length;
        } else {
            size = saveStream.length + abyte0.length;
            byte[] inter = new byte[size];
            System.arraycopy(saveStream, 0, inter, 0, saveStream.length);
            System.arraycopy(abyte0, 0, inter, saveStream.length, abyte0.length);
            abyte0 = new byte[size];
            System.arraycopy(inter, 0, abyte0, 0, size);
            saveStream = null;
            inter = null;
            j = (abyte0[i] & 0xff) << 8 | abyte0[i + 1] & 0xff;
        }
        if (j > size) {
            saveStream = new byte[abyte0.length];
            System.arraycopy(abyte0, 0, saveStream, 0, abyte0.length);
        } else {
            byte abyte1[];
            try {
                abyte1 = new byte[j + 2];
                System.arraycopy(abyte0, i, abyte1, 0, j + 2);
                dsq.put(new Stream5250(abyte1));
                if (abyte0.length > abyte1.length + i) loadStream(abyte0, i + j + 2);
            } catch (Exception ex) {
                System.out.println("load stream error " + ex.getMessage());
            }
        }
    }

    public final byte[] readIncoming() throws IOException {
        boolean done = false;
        boolean negotiate = false;
        baosin.reset();
        int j = -1;
        int i = 0;
        while (!done) {
            i = bin.read();
            if (j == 255 && i == 255) {
                j = -1;
                continue;
            } else {
                baosin.write(i);
                if (j == 255 && i == 239) done = true;
                if (i == 253 && j == 255) {
                    done = true;
                    negotiate = true;
                }
                j = i;
            }
        }
        if (negotiate) {
            baosin.write(bin.read());
            vt.negotiate(baosin.toByteArray());
        }
        return baosin.toByteArray();
    }
}
