package org.mbm.io.thread;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class LogReader {

    private final File file;

    private RandomAccessFile accessFile;

    long lastModified, lastPrinted;

    public static enum STATE {

        NOT_INITIALIZED, INITIALIZING, READY, CLOSING, CLOSED, PRINTING
    }

    ;

    private STATE state = STATE.NOT_INITIALIZED;

    public LogReader(File file) {
        this.file = file;
    }

    public void init() throws IOException {
        state = STATE.INITIALIZING;
        accessFile = new RandomAccessFile(file, "r");
        lastModified = file.lastModified();
        state = STATE.READY;
    }

    public void destroy() throws IOException {
        state = STATE.CLOSING;
        accessFile.close();
        state = STATE.CLOSED;
    }

    public STATE state() {
        return state;
    }

    public void print(OutputStream stream) throws IOException {
        state = STATE.PRINTING;
        lastPrinted = System.currentTimeMillis();
        long length = accessFile.length();
        while (accessFile.getFilePointer() < length) {
            stream.write(accessFile.read());
        }
        stream.flush();
        state = STATE.READY;
    }

    public boolean changed() {
        System.out.println("Checkiing");
        lastModified = file.lastModified();
        return lastModified > lastPrinted;
    }

    private static boolean closed = false;

    public static void markClose() {
        closed = true;
    }

    public static void execute(final String filePath, final OutputStream output) throws IOException {
        final LogReader data = new LogReader(new File(filePath));
        data.init();
        Thread thrd = new Thread() {

            @Override
            public void run() {
                try {
                    final int pooling = 1000 * 3;
                    long lastChecked = System.currentTimeMillis();
                    while (!closed) {
                        long now = System.currentTimeMillis();
                        if (lastChecked + pooling > now) continue;
                        lastChecked = now;
                        if (data.changed()) {
                            data.print(output);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        data.destroy();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Thread close = new Thread() {

            @Override
            public void run() {
                try {
                    System.out.println("ReadFileThread.main(...).new Thread() {...}.run()");
                    data.destroy();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(close);
        thrd.start();
    }
}
