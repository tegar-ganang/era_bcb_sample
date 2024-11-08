package org.jsresources.apps.am.net;

import org.jsresources.apps.am.Debug;
import org.jsresources.apps.am.util.BufferQueue;
import java.io.*;
import java.net.URL;

/**
 * special OutputStream that writes asynchronously to a socket
 * it uses a QueueBuffer that grows with demand
 * Some methods have special semantics here:
 * close(): will not accept any new data, but continue feeding
 *          the socket until no bytes are anymore in the queue.
 * flush(): will clear the queue and close the socket immediately - causing a quick&dirty stop.
 * write():  is non-blocking
 * use stopAndWait(boolean) to wait until it finished writing data
 */
public class BufferedSocketOutputStream extends OutputStream implements Runnable {

    private Thread thread;

    private BufferQueueReader queue;

    private OutputStream m_stream;

    private boolean closed;

    private boolean running;

    private long pos;

    private ClientConnection conn;

    private byte[] buffer;

    public BufferedSocketOutputStream() {
        super();
        thread = null;
        queue = new BufferQueueReader();
        running = false;
        closed = true;
    }

    public void open(URL url) throws Exception {
        if (Debug.TRACE) {
            Debug.println("BufferedSocketOutputStream.open(" + url + ")");
        }
        conn = ClientConnection.getClientConnection(url);
        buffer = new byte[2048];
        closed = false;
        queue.clear();
    }

    public void start() throws Exception {
        if (conn == null) {
            throw new Exception("Started BufferedSocketOutputStream, but not opened it.");
        }
        conn.sendCommand("put");
        m_stream = conn.getOutputStream();
        pos = 0;
        thread = new Thread(this);
        thread.start();
    }

    public void stop(boolean immediately, boolean wait) {
        close();
        if (isRunning()) {
            if (Debug.TRACE) {
                Debug.println("BufferedSocketOutputStream.stopAndWait(): begin");
            }
            if (immediately) {
                flush();
            }
            if (wait && thread != null && Thread.currentThread() != thread && isRunning()) {
                if (Debug.DEBUG) {
                    Debug.println("BufferedSocketOutputStream.stopAndWait(): waiting for thread to die");
                }
                try {
                    boolean buffersChanged = false;
                    do {
                        long waitTime = 2000;
                        long availableBytes = queue.availableBytes();
                        if (queue.availableBuffers() > 0) {
                            waitTime = 10000;
                        }
                        thread.join(waitTime);
                        buffersChanged = availableBytes != queue.availableBytes();
                    } while (isRunning() && queue.availableBuffers() > 0 && buffersChanged);
                } catch (InterruptedException e) {
                }
                if (isRunning()) {
                    if (Debug.ERROR) {
                        Debug.println("BufferedSocketOutputStream.stopAndWait(): join failed. Close socket.");
                    }
                    flush();
                }
            }
            if (Debug.TRACE) {
                Debug.println("BufferedSocketOutputStream.stopAndWait(): end");
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void close() {
        closed = true;
    }

    public synchronized void flush() {
        if (isRunning()) {
            if (Debug.TRACE) {
                Debug.println("BufferedSocketOutputStream.flush()");
            }
            close();
            queue.clear();
            closeStream();
            notifyAll();
        }
    }

    private synchronized void closeStream() {
        if (m_stream != null) {
            try {
                if (Debug.DEBUG) {
                    Debug.println("BufferedSocketOutputStream.closeStream(): closing socket stream...");
                }
                m_stream.close();
            } catch (IOException ioe) {
            }
        }
        if (conn != null) {
            conn.close();
        }
        closed = true;
    }

    public void run() {
        if (Debug.TRACE) {
            Debug.println("BufferedSocketOutputStream.run(): begin");
        }
        running = true;
        try {
            while (!closed || queue.availableBuffers() > 0) {
                if (queue.availableBuffers() < 1) {
                    synchronized (this) {
                        try {
                            wait(100);
                        } catch (InterruptedException e) {
                        }
                    }
                    continue;
                }
                int readBytes = queue.removeRead(buffer, 0, buffer.length);
                try {
                    m_stream.write(buffer, 0, readBytes);
                    pos += readBytes;
                } catch (IOException ioe) {
                    if (Debug.DEBUG) {
                        Debug.println("BufferedSocketOutputStream.run(): stream is closed -> break");
                    }
                    break;
                }
            }
        } catch (Throwable t) {
            if (Debug.ERROR) {
                Debug.println("BufferedSocketOutputStream.run():");
                Debug.println(t);
            }
        }
        closeStream();
        if (Debug.DEBUG) {
            Debug.println("BufferedSocketOutputStream.run(): smooth thread death. Wrote " + pos + " bytes.");
        }
        running = false;
    }

    public void write(int b) throws IOException {
        byte[] hack = new byte[1];
        hack[0] = (byte) b;
        write(hack);
    }

    public synchronized void write(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("BufferedSocketOutputStream is closed");
        }
        byte[] buffer = new byte[len];
        System.arraycopy(b, off, buffer, 0, len);
        queue.addBuffer(buffer);
        notify();
    }

    public long getPos() {
        return pos;
    }

    public long getBufferSize() {
        return queue.availableBytes();
    }

    public class BufferQueueReader extends BufferQueue {

        private int currBuffReadPos = 0;

        public BufferQueueReader() {
            super(false);
        }

        public synchronized byte[] removeBuffer() {
            byte[] result = super.removeBuffer();
            if (result != null) {
                addAvailableBytes(currBuffReadPos);
                currBuffReadPos = 0;
            }
            return result;
        }

        public synchronized int removeRead(byte[] buffer, int pos, int len) {
            int res = 0;
            while (res < len && availableBuffers() > 0) {
                byte[] lastBuffer = getLastBuffer();
                int toRead = lastBuffer.length - currBuffReadPos;
                if (toRead + res > len) {
                    toRead = len - res;
                }
                System.arraycopy(lastBuffer, currBuffReadPos, buffer, pos, toRead);
                pos += toRead;
                addAvailableBytes(-toRead);
                currBuffReadPos += toRead;
                res += toRead;
                if (currBuffReadPos >= lastBuffer.length) {
                    removeBuffer();
                }
            }
            return res;
        }

        public synchronized void clear() {
            super.clear();
            currBuffReadPos = 0;
        }
    }
}
