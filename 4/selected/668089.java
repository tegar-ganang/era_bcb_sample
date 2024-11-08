package org.piax.trans.ts.udpx;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import org.grlea.log.SimpleLogger;

/**
 * @author     Mikio Yoshida
 * @version    1.1.0
 */
public class SelectDispatcher extends Thread {

    private static final SimpleLogger log = new SimpleLogger(SelectDispatcher.class);

    public final int MAX_FIN_WAIT = 50;

    private final Selector selector;

    public SelectDispatcher() throws IOException {
        selector = Selector.open();
    }

    public SelectionKey register(SelectorHandler handler, int ops) throws ClosedChannelException {
        SelectableChannel channel = handler.getChannel();
        return channel.register(selector, ops, handler);
    }

    public void unregister(SelectionKey key) {
        key.cancel();
    }

    public void fin() {
        try {
            selector.close();
            this.interrupt();
        } catch (IOException e) {
            log.warnException(e);
        }
        try {
            this.join(MAX_FIN_WAIT);
        } catch (InterruptedException ignore) {
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                selector.select();
                for (SelectionKey key : selector.selectedKeys()) {
                    SelectorHandler handler = (SelectorHandler) key.attachment();
                    if (key.isReadable()) handler.doReadOperation();
                }
            } catch (IOException e) {
                log.warnException(e);
            } catch (ClosedSelectorException e) {
                break;
            }
            if (isInterrupted()) break;
        }
    }
}
