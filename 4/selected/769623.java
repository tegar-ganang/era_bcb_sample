package org.asyncj;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asyncj.handlers.Handler;
import org.asyncj.handlers.IdleHandler;

public class Async implements Runnable {

    public static final String VERSION = "1.5_01";

    protected static Selector selector;

    protected Log log = LogFactory.getLog(Async.class);

    public static long SELECT_TIMEOUT = 500;

    private IdleHandler idleHandler;

    /**
     * returns singleton Async router, after initialization, and opening the
     * select.
     * 
     * @return an async router
     * @throws IOException
     */
    public static synchronized Async startup() throws IOException {
        Async self = new Async();
        if (selector == null) Async.selector = Selector.open();
        return self;
    }

    /**
     * Adds registers the handler's channel with the select.
     * 
     * @param handler
     * @return
     */
    public SelectionKey register(Handler handler) {
        SelectionKey key = null;
        try {
            key = handler.getChannel().register(selector, handler.getCriteria(), handler);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
        return key;
    }

    public void run() {
        long timeout = getTimeOut();
        try {
            while (true) {
                Set channels = selector.keys();
                if (channels.isEmpty()) break;
                for (Iterator i = channels.iterator(); i.hasNext(); ) {
                    SelectionKey key = (SelectionKey) i.next();
                    if (key.isValid()) {
                        Handler handler = (Handler) key.attachment();
                        key.interestOps(handler.getCriteria());
                    } else {
                        key.attach(null);
                    }
                }
                if (selector.select(timeout) == 0) continue;
                Set ready = selector.selectedKeys();
                Iterator iterator = ready.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = (SelectionKey) iterator.next();
                    iterator.remove();
                    Handler handler = (Handler) key.attachment();
                    if (key.isValid()) {
                        try {
                            handler.process(this, key);
                        } catch (IOException e) {
                            e.printStackTrace();
                            handler.close();
                        }
                    } else if (idleHandler != null) idleHandler.process(this, key);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Override this to change the default time out [SELECT_TIMEOUT]
     * 
     * @return select timout.
     */
    protected long getTimeOut() {
        return SELECT_TIMEOUT;
    }

    /**
     * 
     * @return the selector opened by Async.
     */
    public Selector getSelector() {
        return selector;
    }
}
