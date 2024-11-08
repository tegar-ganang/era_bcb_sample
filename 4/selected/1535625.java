package org.simpleframework.util.packet;

import static java.nio.channels.SelectionKey.OP_WRITE;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import org.simpleframework.util.select.Operation;
import org.simpleframework.util.select.Reactor;

class SocketFlusher implements Flusher {

    private final Signaller signaller;

    private final Window window;

    private final Reactor reactor;

    private final AtomicBoolean running;

    public SocketFlusher(Reactor reactor, Window window) throws Exception {
        this.running = new AtomicBoolean(false);
        this.signaller = new Signaller(window);
        this.reactor = reactor;
        this.window = window;
    }

    public synchronized void flush() throws Exception {
        flush(false);
    }

    public synchronized void flush(boolean block) throws Exception {
        if (!running.getAndSet(true)) {
            reactor.process(signaller, OP_WRITE);
        }
        if (block) {
            wait();
        }
    }

    private synchronized void execute() throws Exception {
        boolean ready = window.flush();
        if (!ready) {
            reactor.process(signaller, OP_WRITE);
        } else {
            running.getAndSet(false);
            notifyAll();
        }
    }

    private synchronized void close() throws Exception {
        window.close();
    }

    private class Signaller implements Operation {

        private final Window window;

        public Signaller(Window window) {
            this.window = window;
        }

        public SocketChannel getChannel() {
            return window.getSocket();
        }

        public void run() {
            try {
                execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
