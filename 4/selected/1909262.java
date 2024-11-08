package org.bing.zion.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.bing.zion.helper.DirectBufferPool;
import org.bing.zion.helper.KeyValuePair;
import org.bing.zion.helper.SelectorBuilder;

public abstract class AbstractChannelService extends AbstractEventService implements Runnable {

    protected static final DirectBufferPool bufferPool = DirectBufferPool.instance();

    protected static final int DEFAULT_SELECT_TIMEOUT = 1000;

    protected final BlockingQueue<Session> coming;

    protected final BlockingQueue<KeyValuePair<Session, Object>> outing;

    protected final BlockingQueue<Session> sessions;

    protected Selector selector;

    public AbstractChannelService(int maxBackNum) {
        selector = SelectorBuilder.newSelector();
        coming = new ArrayBlockingQueue<Session>(maxBackNum);
        outing = new LinkedBlockingQueue<KeyValuePair<Session, Object>>();
        sessions = new LinkedBlockingQueue<Session>();
    }

    @Override
    public void fireSessionClosed(Session session) {
        session.setClosed(true);
        sessions.remove(session);
        super.fireSessionClosed(session);
    }

    /** handle read */
    public void read(Session session) {
        coming.offer(session);
        fireSessionCreated(session);
    }

    protected void regRead() {
        for (; ; ) {
            Session session = coming.poll();
            if (session == null) {
                break;
            } else {
                regRead(session);
            }
        }
    }

    /** allow override by child-class? */
    private void regRead(Session session) {
        SocketChannel channel = session.getChannel();
        try {
            channel.register(selector, SelectionKey.OP_READ, session);
            sessions.offer(session);
            fireSessionOpened(session);
        } catch (IOException e) {
            logger.error("Fail to register OP_READ!", e);
            close(channel);
            fireSessionClosed(session);
        }
    }

    private void doRead(SelectionKey key) throws IOException {
        Session session = (Session) key.attachment();
        SocketChannel channel = session.getChannel();
        ByteBuffer buf = bufferPool.obtain();
        int k = channel.read(buf);
        if (k >= 0) {
            fireMessageReceived(session, buf);
        } else {
            close(channel);
            fireSessionClosed(session);
        }
        bufferPool.release(buf);
    }

    /** handle write */
    public void write(Session session, Object msg) {
        boolean flag3 = outing.offer(new KeyValuePair<Session, Object>(session, msg));
        if (!flag3) {
            logger.info("ChannelService outing queue is fully, not offer " + session);
        }
    }

    protected void doWrite(Session session, Object msg) {
        this.fireMessageSent(session, msg);
    }

    private void doWrite() {
        for (int i = 0; i < 50; i++) {
            KeyValuePair<Session, Object> pair = outing.poll();
            if (pair == null) {
                break;
            } else {
                Session session = pair.getKey();
                Object msg = pair.getValue();
                try {
                    doWrite(session, msg);
                } catch (Exception e) {
                    logger.error("Failed in write object!", e);
                    close(session.getChannel());
                    fireSessionClosed(session);
                }
            }
        }
    }

    /** handle flush */
    public void flush(Session session, ByteBuffer buf) throws IOException {
        ByteBuffer bak = buf.duplicate();
        int num = session.getChannel().write(buf);
        if (num != buf.limit()) {
            while (buf.hasRemaining()) {
                session.getChannel().write(buf);
            }
        }
        fireMessageFlush(session, bak);
    }

    /** handle channel destroy */
    protected void close(SocketChannel channel) {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
                logger.info("Close channel " + channel.socket());
            } else {
                logger.info("Channel is null or closed!");
            }
        } catch (IOException e) {
            logger.error("Fail to close channel " + channel.socket(), e);
        }
    }

    private void process(SelectionKey key) throws IOException {
        if (key.isValid() && key.isReadable()) {
            doRead(key);
        }
    }

    private void process(Set<SelectionKey> keys) {
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
            SelectionKey key = it.next();
            it.remove();
            try {
                process(key);
            } catch (Exception e) {
                logger.error("Failed in process key!", e);
                Session session = (Session) key.attachment();
                if (session != null) {
                    close(session.getChannel());
                    fireSessionClosed(session);
                }
            }
        }
    }

    protected void selRead(int time) throws IOException {
        if (time == 0) {
            if (selector.selectNow() > 0) {
                process(selector.selectedKeys());
            }
        } else if (selector.select(time) > 0) {
            process(selector.selectedKeys());
        }
    }

    /** main loop control */
    public void run() {
        for (; ; ) {
            try {
                if (coming.size() > 0) {
                    regRead();
                }
                doWrite();
                selRead(DEFAULT_SELECT_TIMEOUT);
                if (!selector.isOpen()) {
                    break;
                }
            } catch (Exception e) {
                logger.error("Exception in main loop!", e);
            }
        }
        logger.error("End channel service. ");
    }
}
