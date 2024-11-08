package biz.xsoftware.impl.nio.cm.basic;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import biz.xsoftware.api.nio.channels.Channel;
import biz.xsoftware.api.nio.handlers.DataListener;
import biz.xsoftware.api.nio.handlers.WriteCloseCallback;
import biz.xsoftware.api.nio.libs.BufferFactory;
import biz.xsoftware.api.nio.libs.ChannelSession;
import biz.xsoftware.api.nio.libs.FactoryCreator;
import biz.xsoftware.impl.nio.util.UtilWaitForCompletion;

/**
 * @author Dean Hiller
 */
public abstract class BasChannelImpl extends RegisterableChannelImpl implements Channel {

    private static final Logger apiLog = Logger.getLogger(Channel.class.getName());

    private static final Logger log = Logger.getLogger(BasChannelImpl.class.getName());

    private static final FactoryCreator CREATOR = FactoryCreator.createFactory(null);

    private ChannelSession session;

    private LinkedBlockingQueue<DelayedWritesCloses> waitingWriters = new LinkedBlockingQueue<DelayedWritesCloses>(100);

    private ByteBuffer b;

    private boolean isConnecting = false;

    private boolean isClosed = false;

    private boolean registered;

    public BasChannelImpl(IdObject id, BufferFactory factory, SelectorManager2 selMgr) {
        super(id, selMgr);
        session = CREATOR.createSession(this);
        b = factory.createBuffer(id, 1000);
    }

    public abstract SelectableChannel getRealChannel();

    public abstract boolean isBlocking();

    public abstract int readImpl(ByteBuffer b) throws IOException;

    protected abstract int writeImpl(ByteBuffer b) throws IOException;

    /**
     * This is the method where writes are added to the queue to be written later when the selector
     * fires and tells me we have room to write again.
     * 
     * @param id
     * @return true if the whole ByteBuffer was written, false if only part of it or none of it was written.
     * @throws IOException
     * @throws InterruptedException
     */
    private synchronized boolean tryWriteOrClose(DelayedWritesCloses action, int id) throws IOException, InterruptedException {
        if (!registered) {
            if (action.runDelayedAction(false)) return true;
        }
        boolean accepted = waitingWriters.offer(action, 30, TimeUnit.SECONDS);
        if (!accepted) {
            log.warning(this + "Dropping data, write buffer is backing up.  Remote end will not receive data");
            return false;
        }
        if (!registered) {
            registered = true;
            if (log.isLoggable(Level.FINER)) log.finer(this + "registering channel for write msg id=" + id + " size=" + waitingWriters.size());
            getSelectorManager().registerSelectableChannel(this, SelectionKey.OP_WRITE, null, false);
        }
        return false;
    }

    /**
     * This method is reading from the queue and writing out to the socket buffers that
     * did not get written out when client called write.
     *
     */
    synchronized void writeAll() {
        Queue<DelayedWritesCloses> writers = waitingWriters;
        if (writers.isEmpty()) return;
        while (!writers.isEmpty()) {
            DelayedWritesCloses writer = writers.peek();
            boolean finished = writer.runDelayedAction(true);
            if (!finished) {
                if (log.isLoggable(Level.FINER)) log.finer(this + "Did not write all of id=" + writer);
                break;
            }
            writers.remove();
        }
        if (writers.isEmpty()) {
            if (log.isLoggable(Level.FINER)) log.fine(this + "unregister writes");
            registered = false;
            Helper.unregisterSelectableChannel(this, SelectionKey.OP_WRITE);
        }
    }

    public void bind(SocketAddress addr) throws IOException {
        if (!(addr instanceof InetSocketAddress)) throw new IllegalArgumentException(this + "Can only bind to InetSocketAddress addressses");
        if (apiLog.isLoggable(Level.FINE)) apiLog.fine(this + "Basic.bind called addr=" + addr);
        bindImpl(addr);
    }

    private void bindImpl(SocketAddress addr) throws IOException {
        try {
            bindImpl2(addr);
        } catch (Error e) {
            if (e.getCause() instanceof SocketException) {
                BindException exc = new BindException(e.getMessage());
                exc.initCause(e.getCause());
                throw exc;
            }
            throw e;
        }
    }

    /**
     * 
     * @param addr
     * @throws IOException
     */
    protected abstract void bindImpl2(SocketAddress addr) throws IOException;

    public void registerForReads(DataListener listener) throws IOException, InterruptedException {
        if (listener == null) throw new IllegalArgumentException(this + "listener cannot be null"); else if (!isConnecting && !isConnected()) {
            throw new IllegalStateException(this + "Must call one of the connect methods first");
        }
        if (apiLog.isLoggable(Level.FINE)) apiLog.fine(this + "Basic.registerForReads called");
        getSelectorManager().registerChannelForRead(this, listener);
    }

    public void unregisterForReads() throws IOException, InterruptedException {
        if (apiLog.isLoggable(Level.FINE)) apiLog.fine(this + "Basic.unregisterForReads called");
        getSelectorManager().unregisterChannelForRead(this);
    }

    ByteBuffer getIncomingDataBuf() {
        return b;
    }

    public int write(ByteBuffer b) throws IOException {
        if (!getSelectorManager().isRunning()) throw new IllegalStateException(this + "ChannelManager must be running and is stopped"); else if (isClosed) {
            AsynchronousCloseException exc = new AsynchronousCloseException();
            IOException ioe = new IOException(this + "Client cannot write after the client closed the socket");
            exc.initCause(ioe);
            throw exc;
        }
        Object t = getSelectorManager().getThread();
        if (Thread.currentThread().equals(t)) {
            throw new RuntimeException(this + "You should not perform a " + "blocking write on the channelmanager thread unless you like deadlock.  " + "Use the cm threading layer, or put the code calling this write on another thread");
        }
        try {
            int remain = b.remaining();
            UtilWaitForCompletion waitWrite = new UtilWaitForCompletion(this, t);
            write(b, waitWrite, -1);
            waitWrite.waitForComplete();
            if (b.hasRemaining()) throw new RuntimeException(this + "Did not write all of the ByteBuffer out");
            return remain;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(ByteBuffer b, WriteCloseCallback h, int id) throws IOException, InterruptedException {
        if (!getSelectorManager().isRunning()) throw new IllegalStateException(this + "ChannelManager must be running and is stopped"); else if (isClosed) {
            AsynchronousCloseException exc = new AsynchronousCloseException();
            IOException ioe = new IOException(this + "Client cannot write after the client closed the socket");
            exc.initCause(ioe);
            throw exc;
        }
        if (apiLog.isLoggable(Level.FINER)) apiLog.finer(this + "Basic.write called-id=" + id);
        ByteBuffer newOne = ByteBuffer.allocate(b.remaining());
        newOne.put(b);
        newOne.flip();
        WriteRunnable holder = new WriteRunnable(this, newOne, h, id);
        boolean wroteNow = tryWriteOrClose(holder, id);
        if (log.isLoggable(Level.FINER)) {
            if (!wroteNow) log.finer(this + "did not write immediately, queued up for delivery"); else log.finest(this + "delivered");
        }
    }

    protected void setConnecting(boolean b) {
        isConnecting = b;
    }

    protected boolean isConnecting() {
        return isConnecting;
    }

    protected void setClosed(boolean b) {
        isClosed = b;
    }

    public void close() {
        Object t = getSelectorManager().getThread();
        if (t != null && Thread.currentThread().equals(t)) {
            throw new RuntimeException(this + "You should not perform a blocking close " + "on the channelmanager thread for performance reasons.  Use the cm threading layer, " + "or put the code calling this write on another thread");
        }
        try {
            UtilWaitForCompletion waitWrite = new UtilWaitForCompletion(this, null);
            close(waitWrite, -1);
            waitWrite.waitForComplete();
        } catch (Exception e) {
            log.log(Level.WARNING, this + "Exception closing channel", e);
        }
    }

    public void close(WriteCloseCallback h, int id) {
        try {
            if (apiLog.isLoggable(Level.FINE)) apiLog.fine(this + "Basic.close called");
            if (!getRealChannel().isOpen()) h.finished(this, id);
            setClosed(true);
            CloseRunnable runnable = new CloseRunnable(this, h, id);
            tryWriteOrClose(runnable, id);
        } catch (Exception e) {
            log.log(Level.WARNING, this + "Exception closing channel", e);
        }
    }

    protected abstract void closeImpl() throws IOException;

    public ChannelSession getSession() {
        return session;
    }
}
