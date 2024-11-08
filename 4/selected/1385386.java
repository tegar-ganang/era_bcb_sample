package com.talis.platform.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import org.apache.log4j.Logger;

public class DelayedInputStream extends InputStream {

    private static final transient Logger log = Logger.getLogger(DelayedInputStream.class);

    private DelayedStreamable myWriter;

    private InputStream myInputStream;

    public DelayedInputStream(DelayedStreamable writer) {
        myWriter = writer;
        myInputStream = null;
    }

    @Override
    public int read() throws IOException {
        if (myInputStream == null) {
            myInputStream = getInputStream();
        }
        return myInputStream.read();
    }

    @Override
    public int available() throws IOException {
        if (myInputStream == null) {
            myInputStream = getInputStream();
        }
        return myInputStream.available();
    }

    private InputStream getInputStream() throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream pos = new PipedOutputStream(in);
        new WriterThread(myWriter, pos).start();
        return in;
    }

    private class WriterThread extends Thread {

        DelayedStreamable myWriter;

        OutputStream myStream;

        public WriterThread(DelayedStreamable writer, OutputStream out) {
            myWriter = writer;
            myStream = out;
        }

        public void run() {
            myWriter.write(myStream);
            try {
                myStream.close();
            } catch (IOException e) {
                log.error("IOException writing to delayed input stream.", e);
                e.printStackTrace();
            }
        }
    }
}
