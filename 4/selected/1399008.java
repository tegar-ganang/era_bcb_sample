package net.spy.memcached.protocol;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import net.spy.SpyObject;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationState;

/**
 * Represents a node with the memcached cluster, along with buffering and
 * operation queues.
 */
public abstract class TCPMemcachedNodeImpl extends SpyObject implements MemcachedNode {

    private final SocketAddress socketAddress;

    private final ByteBuffer rbuf;

    private final ByteBuffer wbuf;

    protected final BlockingQueue<Operation> writeQ;

    private final BlockingQueue<Operation> readQ;

    private final BlockingQueue<Operation> inputQueue;

    private volatile int reconnectAttempt = 1;

    private SocketChannel channel;

    private int toWrite = 0;

    protected GetOperation getOp = null;

    private SelectionKey sk = null;

    public TCPMemcachedNodeImpl(SocketAddress sa, SocketChannel c, int bufSize, BlockingQueue<Operation> rq, BlockingQueue<Operation> wq, BlockingQueue<Operation> iq) {
        super();
        assert sa != null : "No SocketAddress";
        assert c != null : "No SocketChannel";
        assert bufSize > 0 : "Invalid buffer size: " + bufSize;
        assert rq != null : "No operation read queue";
        assert wq != null : "No operation write queue";
        assert iq != null : "No input queue";
        socketAddress = sa;
        setChannel(c);
        rbuf = ByteBuffer.allocate(bufSize);
        wbuf = ByteBuffer.allocate(bufSize);
        getWbuf().clear();
        readQ = rq;
        writeQ = wq;
        inputQueue = iq;
    }

    public final void copyInputQueue() {
        Collection<Operation> tmp = new ArrayList<Operation>();
        inputQueue.drainTo(tmp, writeQ.remainingCapacity());
        writeQ.addAll(tmp);
    }

    public final void setupResend() {
        Operation op = getCurrentWriteOp();
        if (op != null) {
            op.getBuffer().reset();
        }
        while (hasReadOp()) {
            op = removeCurrentReadOp();
            getLogger().warn("Discarding partially completed op: %s", op);
            op.cancel();
        }
        getWbuf().clear();
        getRbuf().clear();
        toWrite = 0;
    }

    private boolean preparePending() {
        copyInputQueue();
        Operation nextOp = getCurrentWriteOp();
        while (nextOp != null && nextOp.isCancelled()) {
            getLogger().info("Removing cancelled operation: %s", nextOp);
            removeCurrentWriteOp();
            nextOp = getCurrentWriteOp();
        }
        return nextOp != null;
    }

    public final void fillWriteBuffer(boolean optimizeGets) {
        if (toWrite == 0 && readQ.remainingCapacity() > 0) {
            getWbuf().clear();
            Operation o = getCurrentWriteOp();
            while (o != null && toWrite < getWbuf().capacity()) {
                assert o.getState() == OperationState.WRITING;
                ByteBuffer obuf = o.getBuffer();
                int bytesToCopy = Math.min(getWbuf().remaining(), obuf.remaining());
                byte b[] = new byte[bytesToCopy];
                obuf.get(b);
                getWbuf().put(b);
                getLogger().debug("After copying stuff from %s: %s", o, getWbuf());
                if (!o.getBuffer().hasRemaining()) {
                    o.writeComplete();
                    transitionWriteItem();
                    preparePending();
                    if (optimizeGets) {
                        optimize();
                    }
                    o = getCurrentWriteOp();
                }
                toWrite += bytesToCopy;
            }
            getWbuf().flip();
            assert toWrite <= getWbuf().capacity() : "toWrite exceeded capacity: " + this;
            assert toWrite == getWbuf().remaining() : "Expected " + toWrite + " remaining, got " + getWbuf().remaining();
        } else {
            getLogger().debug("Buffer is full, skipping");
        }
    }

    public final void transitionWriteItem() {
        Operation op = removeCurrentWriteOp();
        assert op != null : "There is no write item to transition";
        getLogger().debug("Transitioning %s to read", op);
        readQ.add(op);
    }

    protected abstract void optimize();

    public final Operation getCurrentReadOp() {
        return readQ.peek();
    }

    public final Operation removeCurrentReadOp() {
        return readQ.remove();
    }

    public final Operation getCurrentWriteOp() {
        return getOp == null ? writeQ.peek() : getOp;
    }

    public final Operation removeCurrentWriteOp() {
        Operation rv = getOp;
        if (rv == null) {
            rv = writeQ.remove();
        } else {
            getOp = null;
        }
        return rv;
    }

    public final boolean hasReadOp() {
        return !readQ.isEmpty();
    }

    public final boolean hasWriteOp() {
        return !(getOp == null && writeQ.isEmpty());
    }

    public final void addOp(Operation op) {
        boolean added = inputQueue.add(op);
        assert added;
    }

    public final int getSelectionOps() {
        int rv = 0;
        if (getChannel().isConnected()) {
            if (hasReadOp()) {
                rv |= SelectionKey.OP_READ;
            }
            if (toWrite > 0 || hasWriteOp()) {
                rv |= SelectionKey.OP_WRITE;
            }
        } else {
            rv = SelectionKey.OP_CONNECT;
        }
        return rv;
    }

    public final ByteBuffer getRbuf() {
        return rbuf;
    }

    public final ByteBuffer getWbuf() {
        return wbuf;
    }

    public final SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public final boolean isActive() {
        return reconnectAttempt == 0 && getChannel() != null && getChannel().isConnected();
    }

    public final void reconnecting() {
        reconnectAttempt++;
    }

    public final void connected() {
        reconnectAttempt = 0;
    }

    public final int getReconnectCount() {
        return reconnectAttempt;
    }

    @Override
    public final String toString() {
        int sops = 0;
        if (getSk() != null && getSk().isValid()) {
            sops = getSk().interestOps();
        }
        int rsize = readQ.size() + (getOp == null ? 0 : 1);
        int wsize = writeQ.size();
        int isize = inputQueue.size();
        return "{QA sa=" + getSocketAddress() + ", #Rops=" + rsize + ", #Wops=" + wsize + ", #iq=" + isize + ", topRop=" + getCurrentReadOp() + ", topWop=" + getCurrentWriteOp() + ", toWrite=" + toWrite + ", interested=" + sops + "}";
    }

    public final void registerChannel(SocketChannel ch, SelectionKey skey) {
        setChannel(ch);
        setSk(skey);
    }

    public final void setChannel(SocketChannel to) {
        assert channel == null || !channel.isOpen() : "Attempting to overwrite channel";
        channel = to;
    }

    public final SocketChannel getChannel() {
        return channel;
    }

    public final void setSk(SelectionKey to) {
        sk = to;
    }

    public final SelectionKey getSk() {
        return sk;
    }

    public final int getBytesRemainingToWrite() {
        return toWrite;
    }

    public final int writeSome() throws IOException {
        int wrote = channel.write(wbuf);
        assert wrote >= 0 : "Wrote negative bytes?";
        toWrite -= wrote;
        assert toWrite >= 0 : "toWrite went negative after writing " + wrote + " bytes for " + this;
        getLogger().debug("Wrote %d bytes", wrote);
        return wrote;
    }

    public final void fixupOps() {
        if (sk != null && sk.isValid()) {
            int iops = getSelectionOps();
            getLogger().debug("Setting interested opts to %d", iops);
            sk.interestOps(iops);
        } else {
            getLogger().debug("Selection key is not valid.");
        }
    }
}
