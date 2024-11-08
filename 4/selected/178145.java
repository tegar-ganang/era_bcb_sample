package org.apache.mina.transport.socket.nio;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.polling.AbstractPollingIoProcessor;
import org.apache.mina.core.session.SessionState;

/**
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class NioProcessor extends AbstractPollingIoProcessor<NioSession> {

    /** The selector associated with this processor */
    private Selector selector;

    /**
     * 
     * Creates a new instance of NioProcessor.
     * 
     * @param executor
     */
    public NioProcessor(Executor executor) {
        super(executor);
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeIoException("Failed to open a selector.", e);
        }
    }

    @Override
    protected void dispose0() throws Exception {
        selector.close();
    }

    @Override
    protected int select(long timeout) throws Exception {
        return selector.select(timeout);
    }

    @Override
    protected int select() throws Exception {
        return selector.select();
    }

    @Override
    protected boolean isSelectorEmpty() {
        return selector.keys().isEmpty();
    }

    @Override
    protected void wakeup() {
        wakeupCalled.getAndSet(true);
        selector.wakeup();
    }

    @Override
    protected Iterator<NioSession> allSessions() {
        return new IoSessionIterator(selector.keys());
    }

    @SuppressWarnings("synthetic-access")
    @Override
    protected Iterator<NioSession> selectedSessions() {
        return new IoSessionIterator(selector.selectedKeys());
    }

    @Override
    protected void init(NioSession session) throws Exception {
        SelectableChannel ch = (SelectableChannel) session.getChannel();
        ch.configureBlocking(false);
        session.setSelectionKey(ch.register(selector, SelectionKey.OP_READ, session));
    }

    @Override
    protected void destroy(NioSession session) throws Exception {
        ByteChannel ch = session.getChannel();
        SelectionKey key = session.getSelectionKey();
        if (key != null) {
            key.cancel();
        }
        ch.close();
    }

    /**
     * In the case we are using the java select() method, this method is used to
     * trash the buggy selector and create a new one, registering all the
     * sockets on it.
     */
    protected void registerNewSelector() throws IOException {
        synchronized (selector) {
            Set<SelectionKey> keys = selector.keys();
            Selector newSelector = Selector.open();
            for (SelectionKey key : keys) {
                SelectableChannel ch = key.channel();
                NioSession session = (NioSession) key.attachment();
                SelectionKey newKey = ch.register(newSelector, key.interestOps(), session);
                session.setSelectionKey(newKey);
            }
            selector.close();
            selector = newSelector;
        }
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isBrokenConnection() throws IOException {
        boolean brokenSession = false;
        synchronized (selector) {
            Set<SelectionKey> keys = selector.keys();
            for (SelectionKey key : keys) {
                SelectableChannel channel = key.channel();
                if ((((channel instanceof DatagramChannel) && ((DatagramChannel) channel).isConnected())) || ((channel instanceof SocketChannel) && ((SocketChannel) channel).isConnected())) {
                    key.cancel();
                    brokenSession = true;
                }
            }
        }
        return brokenSession;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SessionState getState(NioSession session) {
        SelectionKey key = session.getSelectionKey();
        if (key == null) {
            return SessionState.OPENING;
        }
        if (key.isValid()) {
            return SessionState.OPENED;
        } else {
            return SessionState.CLOSING;
        }
    }

    @Override
    protected boolean isReadable(NioSession session) {
        SelectionKey key = session.getSelectionKey();
        return key.isValid() && key.isReadable();
    }

    @Override
    protected boolean isWritable(NioSession session) {
        SelectionKey key = session.getSelectionKey();
        return key.isValid() && key.isWritable();
    }

    @Override
    protected boolean isInterestedInRead(NioSession session) {
        SelectionKey key = session.getSelectionKey();
        return key.isValid() && (key.interestOps() & SelectionKey.OP_READ) != 0;
    }

    @Override
    protected boolean isInterestedInWrite(NioSession session) {
        SelectionKey key = session.getSelectionKey();
        return key.isValid() && (key.interestOps() & SelectionKey.OP_WRITE) != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInterestedInRead(NioSession session, boolean isInterested) throws Exception {
        SelectionKey key = session.getSelectionKey();
        int oldInterestOps = key.interestOps();
        int newInterestOps = oldInterestOps;
        if (isInterested) {
            newInterestOps |= SelectionKey.OP_READ;
        } else {
            newInterestOps &= ~SelectionKey.OP_READ;
        }
        if (oldInterestOps != newInterestOps) {
            key.interestOps(newInterestOps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInterestedInWrite(NioSession session, boolean isInterested) throws Exception {
        SelectionKey key = session.getSelectionKey();
        if (key == null) {
            return;
        }
        int newInterestOps = key.interestOps();
        if (isInterested) {
            newInterestOps |= SelectionKey.OP_WRITE;
        } else {
            newInterestOps &= ~SelectionKey.OP_WRITE;
        }
        key.interestOps(newInterestOps);
    }

    @Override
    protected int read(NioSession session, IoBuffer buf) throws Exception {
        ByteChannel channel = session.getChannel();
        return session.getChannel().read(buf.buf());
    }

    @Override
    protected int write(NioSession session, IoBuffer buf, int length) throws Exception {
        if (buf.remaining() <= length) {
            return session.getChannel().write(buf.buf());
        }
        int oldLimit = buf.limit();
        buf.limit(buf.position() + length);
        try {
            return session.getChannel().write(buf.buf());
        } finally {
            buf.limit(oldLimit);
        }
    }

    @Override
    protected int transferFile(NioSession session, FileRegion region, int length) throws Exception {
        try {
            return (int) region.getFileChannel().transferTo(region.getPosition(), length, session.getChannel());
        } catch (IOException e) {
            String message = e.getMessage();
            if (message != null && message.contains("temporarily unavailable")) {
                return 0;
            }
            throw e;
        }
    }

    /**
     * An encapsulating iterator around the {@link Selector#selectedKeys()} or
     * the {@link Selector#keys()} iterator;
     */
    protected static class IoSessionIterator<NioSession> implements Iterator<NioSession> {

        private final Iterator<SelectionKey> iterator;

        /**
         * Create this iterator as a wrapper on top of the selectionKey Set.
         * 
         * @param keys
         *            The set of selected sessions
         */
        private IoSessionIterator(Set<SelectionKey> keys) {
            iterator = keys.iterator();
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return iterator.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        public NioSession next() {
            SelectionKey key = iterator.next();
            NioSession nioSession = (NioSession) key.attachment();
            return nioSession;
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            iterator.remove();
        }
    }
}
