package org.chesstools.bots.printplayer;

import java.io.InputStream;
import java.io.OutputStream;

public class StreamPipe implements Runnable {

    private InputStream in;

    private PipeClosedEventListener listener;

    private OutputStream out;

    private Thread piperThread;

    public StreamPipe(InputStream inputstream, OutputStream outputstream) {
        this(inputstream, outputstream, null);
    }

    public StreamPipe(InputStream inputstream, OutputStream outputstream, PipeClosedEventListener pipeclosedeventlistener) {
        in = inputstream;
        out = outputstream;
        listener = pipeclosedeventlistener;
        piperThread = new Thread(this);
        piperThread.start();
    }

    public void close() {
        piperThread.interrupt();
    }

    private void notifyListener() {
        if (listener != null) listener.pipeClosedNotification(this);
    }

    public void run() {
        try {
            byte buf[] = new byte[1000];
            while (!piperThread.isInterrupted()) {
                int readCount = in.read(buf);
                if (readCount == -1) {
                    notifyListener();
                    break;
                }
                if (readCount != 0) out.write(buf, 0, readCount);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
