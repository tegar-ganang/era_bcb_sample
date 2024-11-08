package ru.greeneyes.socksThrough;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.InterruptedIOException;

/**
 * @author ivanalx
 * @date 20.05.2009 14:58:02
 */
public class ReaderThread implements Runnable {

    private InputStream read;

    private OutputStream write;

    private Runnable afterExecute;

    private int bufferSize;

    private String readId;

    private String writeId;

    private volatile boolean isStoped = false;

    public ReaderThread(InputStream read, String readId, OutputStream write, String writeId, Runnable afterExecute, int bufferSize) {
        this.read = read;
        this.write = write;
        this.afterExecute = afterExecute;
        this.bufferSize = bufferSize;
        this.readId = readId;
        this.writeId = writeId;
    }

    public void run() {
        int readAmount = 0;
        byte[] buffer = new byte[bufferSize];
        try {
            while (readAmount != -1 && !isStoped) {
                try {
                    while ((readAmount = read.read(buffer)) != -1) {
                        write.write(buffer, 0, readAmount);
                        if (readAmount > 0) {
                            System.out.println("DEBUG: from " + readId + " readed " + readAmount + " bytes and send to " + writeId + ";");
                            readAmount = 0;
                        }
                        if (isStoped) {
                            break;
                        }
                    }
                } catch (InterruptedIOException e) {
                }
            }
        } catch (Exception e) {
            System.err.println("An error for " + readId + ":" + writeId + " :" + e);
        } finally {
            System.out.println("All done for " + readId + ":" + writeId + " reader. Internal state is: " + isStoped + ":" + readAmount + " Execute afterend scenario.");
            afterExecute.run();
        }
    }

    public void stop() {
        isStoped = true;
    }
}
