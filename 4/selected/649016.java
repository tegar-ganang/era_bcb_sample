package org.jtv.backend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.log4j.Logger;

public class InputToOutputStreams extends Thread {

    private static final Logger LOGGER = Logger.getLogger(InputToOutputStreams.class);

    public interface InputStreamFactory {

        InputStream create() throws IOException;
    }

    private InputStreamFactory inFactory;

    private int transportSize;

    private OutputStream[] out;

    private int numOutputStreams;

    private boolean active;

    private String identifier;

    private long tempMarkMb;

    public InputToOutputStreams(InputStreamFactory inFactory, int transportSize, String identifier) {
        super("ios");
        this.inFactory = inFactory;
        this.transportSize = transportSize;
        out = new OutputStream[8];
        this.identifier = identifier;
        initTempMarkMb();
    }

    private void initTempMarkMb() {
        tempMarkMb = 1 * 1024 * 1024;
    }

    public synchronized boolean isOpen() {
        return active;
    }

    private void logInfo(String message) {
        LOGGER.info("(" + identifier + ") " + message);
    }

    private void logDebug(String message) {
        LOGGER.debug("(" + identifier + ") " + message);
    }

    public synchronized void run() {
        long tempWritten = 0;
        active = true;
        byte[] readTransport = new byte[transportSize];
        try {
            InputStream in = null;
            while (active) {
                if (numOutputStreams == 0) {
                    if (in != null) {
                        in.close();
                        in = null;
                        logInfo("closed in");
                    }
                    while (active && numOutputStreams == 0) {
                        logDebug("waiting forever");
                        doWait(0);
                    }
                    if (active) {
                        in = inFactory.create();
                        logInfo("opened in");
                    }
                }
                if (active) {
                    int read = in.read(readTransport);
                    writeToBuffers(readTransport, read);
                    if (LOGGER.isDebugEnabled()) {
                        tempWritten += read;
                        if (tempWritten > tempMarkMb) {
                            logDebug("written " + tempWritten + " bytes to " + numOutputStreams + " streams");
                            tempWritten = 0;
                            if (tempMarkMb < 256 * 1024 * 1024) {
                                tempMarkMb *= 2;
                            }
                        }
                    }
                    doWait(5);
                }
            }
            logInfo("became inactive");
            for (int i = 0; i < numOutputStreams; i++) {
                out[i].close();
            }
            if (in != null) {
                in.close();
                in = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logInfo("exited");
    }

    private void doWait(int i) {
        try {
            wait(i);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    private void writeToBuffers(byte[] buffer, int writePos) {
        int i = 0;
        while (i < numOutputStreams) {
            try {
                out[i].write(buffer, 0, writePos);
                i++;
            } catch (IOException ioe) {
                logInfo("dropping output stream " + i + " (" + ioe.getMessage() + ")");
                removeOutputStream(i);
            }
        }
    }

    private void removeOutputStream(int i) {
        numOutputStreams--;
        logInfo("removing " + out[i] + ", leaving " + numOutputStreams);
        System.arraycopy(out, i + 1, out, i, numOutputStreams - i);
    }

    public synchronized int removeOutputStream(OutputStream outputStream) {
        for (int i = 0; i < numOutputStreams; i++) {
            if (out[i].equals(outputStream)) {
                removeOutputStream(i);
                break;
            }
        }
        notifyAll();
        return numOutputStreams;
    }

    public synchronized void addOutputStream(OutputStream outputStream) {
        out[numOutputStreams] = outputStream;
        numOutputStreams++;
        logInfo("adding output " + numOutputStreams + " (" + outputStream + ")");
        initTempMarkMb();
        notifyAll();
    }

    public synchronized void close() {
        active = false;
        notifyAll();
    }
}
