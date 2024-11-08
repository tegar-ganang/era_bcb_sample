package org.tn5250j.framework.tn5250;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import org.tn5250j.encoding.CodePage;
import org.tn5250j.tools.logging.TN5250jLogFactory;
import org.tn5250j.tools.logging.TN5250jLogger;

public class DataStreamProducer implements Runnable {

    private BufferedInputStream bin;

    private ByteArrayOutputStream baosin;

    private Thread me;

    private byte[] saveStream;

    private final BlockingQueue<Object> dsq;

    private tnvt vt;

    private byte[] abyte2;

    private FileOutputStream fw;

    private BufferedOutputStream dw;

    private boolean dumpBytes = false;

    private CodePage codePage;

    private TN5250jLogger log = TN5250jLogFactory.getLogger(this.getClass());

    public DataStreamProducer(tnvt vt, BufferedInputStream in, BlockingQueue<Object> queue, byte[] init) {
        bin = in;
        this.vt = vt;
        baosin = new ByteArrayOutputStream();
        dsq = queue;
        abyte2 = init;
    }

    public void setInputStream(ByteArrayOutputStream is) {
        baosin = is;
    }

    public final void run() {
        boolean done = false;
        me = Thread.currentThread();
        loadStream(abyte2, 0);
        while (!done) {
            try {
                byte[] abyte0 = readIncoming();
                if (abyte0 != null) {
                    if (abyte0.length > 0) {
                        loadStream(abyte0, 0);
                    } else {
                        done = true;
                        vt.disconnect();
                    }
                }
            } catch (SocketException se) {
                log.warn("   DataStreamProducer thread interrupted and stopping " + se.getMessage());
                done = true;
            } catch (IOException ioe) {
                log.warn(ioe.getMessage());
                if (me.isInterrupted()) done = true;
            } catch (Exception ex) {
                log.warn(ex.getMessage());
                if (me.isInterrupted()) done = true;
            }
        }
    }

    private final void loadStream(byte abyte0[], int i) {
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
            log.debug("partial stream found");
        }
        if (j > size) {
            saveStream = new byte[abyte0.length];
            System.arraycopy(abyte0, 0, saveStream, 0, abyte0.length);
            log.debug("partial stream saved");
        } else {
            byte abyte1[];
            try {
                abyte1 = new byte[j + 2];
                System.arraycopy(abyte0, i, abyte1, 0, j + 2);
                dsq.put(abyte1);
                if (abyte0.length > abyte1.length + i) loadStream(abyte0, i + j + 2);
            } catch (Exception ex) {
                log.warn("load stream error " + ex.getMessage());
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
            if (i == -1) {
                done = true;
                vt.disconnect();
                continue;
            }
            if (j == 255 && i == 255) {
                j = -1;
                continue;
            }
            baosin.write(i);
            if (j == 255 && i == 239) done = true;
            if (i == 253 && j == 255) {
                done = true;
                negotiate = true;
            }
            j = i;
        }
        byte[] rBytes = baosin.toByteArray();
        if (dumpBytes) {
            dump(rBytes);
        }
        if (negotiate) {
            baosin.write(bin.read());
            vt.negotiate(rBytes);
            return null;
        }
        return rBytes;
    }

    protected final void toggleDebug(CodePage cp) {
        if (codePage == null) codePage = cp;
        dumpBytes = !dumpBytes;
        if (dumpBytes) {
            try {
                if (fw == null) {
                    fw = new FileOutputStream("log.txt");
                    dw = new BufferedOutputStream(fw);
                }
            } catch (FileNotFoundException fnfe) {
                log.warn(fnfe.getMessage());
            }
        } else {
            try {
                if (dw != null) dw.close();
                if (fw != null) fw.close();
                dw = null;
                fw = null;
                codePage = null;
            } catch (IOException ioe) {
                log.warn(ioe.getMessage());
            }
        }
        log.info("Data Stream output is now " + dumpBytes);
    }

    public void dump(byte[] abyte0) {
        try {
            log.info("\n Buffer Dump of data from AS400: ");
            dw.write("\r\n Buffer Dump of data from AS400: ".getBytes());
            StringBuffer h = new StringBuffer();
            for (int x = 0; x < abyte0.length; x++) {
                if (x % 16 == 0) {
                    System.out.println("  " + h.toString());
                    dw.write(("  " + h.toString() + "\r\n").getBytes());
                    h.setLength(0);
                    h.append("+0000");
                    h.setLength(5 - Integer.toHexString(x).length());
                    h.append(Integer.toHexString(x).toUpperCase());
                    System.out.print(h.toString());
                    dw.write(h.toString().getBytes());
                    h.setLength(0);
                }
                char ac = codePage.ebcdic2uni(abyte0[x]);
                if (ac < ' ') h.append('.'); else h.append(ac);
                if (x % 4 == 0) {
                    System.out.print(" ");
                    dw.write((" ").getBytes());
                }
                if (Integer.toHexString(abyte0[x] & 0xff).length() == 1) {
                    System.out.print("0" + Integer.toHexString(abyte0[x] & 0xff).toUpperCase());
                    dw.write(("0" + Integer.toHexString(abyte0[x] & 0xff).toUpperCase()).getBytes());
                } else {
                    System.out.print(Integer.toHexString(abyte0[x] & 0xff).toUpperCase());
                    dw.write((Integer.toHexString(abyte0[x] & 0xff).toUpperCase()).getBytes());
                }
            }
            System.out.println();
            dw.write("\r\n".getBytes());
            dw.flush();
        } catch (EOFException _ex) {
        } catch (Exception _ex) {
            log.warn("Cannot dump from host\n\r");
        }
    }
}
