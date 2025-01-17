package org.apache.bookkeeper.proto;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.bookkeeper.client.BKException;
import org.apache.bookkeeper.proto.BookkeeperInternalCallbacks.GenericCallback;
import org.apache.bookkeeper.proto.BookkeeperInternalCallbacks.WriteCallback;
import org.apache.bookkeeper.proto.BookkeeperInternalCallbacks.ReadEntryCallback;
import org.apache.bookkeeper.util.OrderedSafeExecutor;
import org.apache.bookkeeper.util.SafeRunnable;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.CorruptedFrameException;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;

/**
 * This class manages all details of connection to a particular bookie. It also
 * has reconnect logic if a connection to a bookie fails.
 * 
 */
@ChannelPipelineCoverage("one")
public class PerChannelBookieClient extends SimpleChannelHandler implements ChannelPipelineFactory {

    static final Logger LOG = Logger.getLogger(PerChannelBookieClient.class);

    static final long maxMemory = Runtime.getRuntime().maxMemory() / 5;

    public static int MAX_FRAME_LENGTH = 2 * 1024 * 1024;

    InetSocketAddress addr;

    boolean connected = false;

    AtomicLong totalBytesOutstanding;

    ClientSocketChannelFactory channelFactory;

    OrderedSafeExecutor executor;

    ConcurrentHashMap<CompletionKey, AddCompletion> addCompletions = new ConcurrentHashMap<CompletionKey, AddCompletion>();

    ConcurrentHashMap<CompletionKey, ReadCompletion> readCompletions = new ConcurrentHashMap<CompletionKey, ReadCompletion>();

    /**
     * The following member variables do not need to be concurrent, or volatile
     * because they are always updated under a lock
     */
    Queue<GenericCallback<Void>> pendingOps = new ArrayDeque<GenericCallback<Void>>();

    boolean connectionAttemptInProgress;

    Channel channel = null;

    public PerChannelBookieClient(OrderedSafeExecutor executor, ClientSocketChannelFactory channelFactory, InetSocketAddress addr, AtomicLong totalBytesOutstanding) {
        this.addr = addr;
        this.executor = executor;
        this.totalBytesOutstanding = totalBytesOutstanding;
        this.channelFactory = channelFactory;
        connect(channelFactory);
    }

    void connect(ChannelFactory channelFactory) {
        if (LOG.isDebugEnabled()) LOG.debug("Connecting to bookie: " + addr);
        ClientBootstrap bootstrap = new ClientBootstrap(channelFactory);
        bootstrap.setPipelineFactory(this);
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);
        connectionAttemptInProgress = true;
        ChannelFuture future = bootstrap.connect(addr);
        future.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                int rc;
                Queue<GenericCallback<Void>> oldPendingOps;
                synchronized (PerChannelBookieClient.this) {
                    if (future.isSuccess()) {
                        LOG.info("Successfully connected to bookie: " + addr);
                        rc = BKException.Code.OK;
                        channel = future.getChannel();
                        connected = true;
                    } else {
                        LOG.error("Could not connect to bookie: " + addr);
                        rc = BKException.Code.BookieHandleNotAvailableException;
                        channel = null;
                        connected = false;
                    }
                    connectionAttemptInProgress = false;
                    PerChannelBookieClient.this.channel = channel;
                    oldPendingOps = pendingOps;
                    pendingOps = new ArrayDeque<GenericCallback<Void>>();
                }
                for (GenericCallback<Void> pendingOp : oldPendingOps) {
                    pendingOp.operationComplete(rc, null);
                }
            }
        });
    }

    void connectIfNeededAndDoOp(GenericCallback<Void> op) {
        boolean doOpNow;
        if (channel != null && connected) {
            doOpNow = true;
        } else {
            synchronized (this) {
                if (channel != null && connected) {
                    doOpNow = true;
                } else {
                    doOpNow = false;
                    pendingOps.add(op);
                    if (!connectionAttemptInProgress) {
                        connect(channelFactory);
                    }
                }
            }
        }
        if (doOpNow) {
            op.operationComplete(BKException.Code.OK, null);
        }
    }

    /**
     * This method should be called only after connection has been checked for
     * {@link #connectIfNeededAndDoOp(GenericCallback)}
     * 
     * @param ledgerId
     * @param masterKey
     * @param entryId
     * @param lastConfirmed
     * @param macCode
     * @param data
     * @param cb
     * @param ctx
     */
    void addEntry(final long ledgerId, byte[] masterKey, final long entryId, ChannelBuffer toSend, WriteCallback cb, Object ctx) {
        final int entrySize = toSend.readableBytes();
        final CompletionKey completionKey = new CompletionKey(ledgerId, entryId);
        addCompletions.put(completionKey, new AddCompletion(cb, entrySize, ctx));
        int totalHeaderSize = 4 + 4 + masterKey.length;
        ChannelBuffer header = channel.getConfig().getBufferFactory().getBuffer(totalHeaderSize);
        header.writeInt(totalHeaderSize - 4 + entrySize);
        header.writeInt(BookieProtocol.ADDENTRY);
        header.writeBytes(masterKey);
        ChannelBuffer wrappedBuffer = ChannelBuffers.wrappedBuffer(header, toSend);
        ChannelFuture future = channel.write(wrappedBuffer);
        future.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Successfully wrote request for adding entry: " + entryId + " ledger-id: " + ledgerId + " bookie: " + channel.getRemoteAddress() + " entry length: " + entrySize);
                    }
                } else {
                    errorOutAddKey(completionKey);
                }
            }
        });
    }

    public void readEntry(final long ledgerId, final long entryId, ReadEntryCallback cb, Object ctx) {
        final CompletionKey key = new CompletionKey(ledgerId, entryId);
        readCompletions.put(key, new ReadCompletion(cb, ctx));
        int totalHeaderSize = 4 + 4 + 8 + 8;
        ChannelBuffer tmpEntry = channel.getConfig().getBufferFactory().getBuffer(totalHeaderSize);
        tmpEntry.writeInt(totalHeaderSize - 4);
        tmpEntry.writeInt(BookieProtocol.READENTRY);
        tmpEntry.writeLong(ledgerId);
        tmpEntry.writeLong(entryId);
        ChannelFuture future = channel.write(tmpEntry);
        future.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Successfully wrote request for reading entry: " + entryId + " ledger-id: " + ledgerId + " bookie: " + channel.getRemoteAddress());
                    }
                } else {
                    errorOutReadKey(key);
                }
            }
        });
    }

    public void close() {
        if (channel != null) {
            channel.close();
        }
    }

    void errorOutReadKey(final CompletionKey key) {
        executor.submitOrdered(key.ledgerId, new SafeRunnable() {

            @Override
            public void safeRun() {
                ReadCompletion readCompletion = readCompletions.remove(key);
                if (readCompletion != null) {
                    LOG.error("Could not write  request for reading entry: " + key.entryId + " ledger-id: " + key.ledgerId + " bookie: " + channel.getRemoteAddress());
                    readCompletion.cb.readEntryComplete(BKException.Code.BookieHandleNotAvailableException, key.ledgerId, key.entryId, null, readCompletion.ctx);
                }
            }
        });
    }

    void errorOutAddKey(final CompletionKey key) {
        executor.submitOrdered(key.ledgerId, new SafeRunnable() {

            @Override
            public void safeRun() {
                AddCompletion addCompletion = addCompletions.remove(key);
                if (addCompletion != null) {
                    String bAddress = "null";
                    if (channel != null) bAddress = channel.getRemoteAddress().toString();
                    LOG.error("Could not write request for adding entry: " + key.entryId + " ledger-id: " + key.ledgerId + " bookie: " + bAddress);
                    addCompletion.cb.writeComplete(BKException.Code.BookieHandleNotAvailableException, key.ledgerId, key.entryId, addr, addCompletion.ctx);
                    LOG.error("Invoked callback method: " + key.entryId);
                }
            }
        });
    }

    /**
     * Errors out pending entries. We call this method from one thread to avoid
     * concurrent executions to QuorumOpMonitor (implements callbacks). It seems
     * simpler to call it from BookieHandle instead of calling directly from
     * here.
     */
    void errorOutOutstandingEntries() {
        for (CompletionKey key : addCompletions.keySet()) {
            errorOutAddKey(key);
        }
        for (CompletionKey key : readCompletions.keySet()) {
            errorOutReadKey(key);
        }
    }

    /**
     * In the netty pipeline, we need to split packets based on length, so we
     * use the {@link LengthFieldBasedFrameDecoder}. Other than that all actions
     * are carried out in this class, e.g., making sense of received messages,
     * prepending the length to outgoing packets etc.
     */
    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("lengthbasedframedecoder", new LengthFieldBasedFrameDecoder(MAX_FRAME_LENGTH, 0, 4, 0, 4));
        pipeline.addLast("mainhandler", this);
        return pipeline;
    }

    /**
     * If our channel has disconnected, we just error out the pending entries
     */
    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        LOG.info("Disconnected from bookie: " + addr);
        errorOutOutstandingEntries();
        channel.close();
        connected = false;
    }

    /**
     * Called by netty when an exception happens in one of the netty threads
     * (mostly due to what we do in the netty threads)
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        Throwable t = e.getCause();
        if (t instanceof CorruptedFrameException || t instanceof TooLongFrameException) {
            LOG.error("Corrupted fram recieved from bookie: " + e.getChannel().getRemoteAddress());
            return;
        }
        if (t instanceof IOException) {
            return;
        }
        LOG.fatal("Unexpected exception caught by bookie client channel handler", t);
    }

    /**
     * Called by netty when a message is received on a channel
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (!(e.getMessage() instanceof ChannelBuffer)) {
            ctx.sendUpstream(e);
            return;
        }
        final ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
        final int type, rc;
        final long ledgerId, entryId;
        try {
            type = buffer.readInt();
            rc = buffer.readInt();
            ledgerId = buffer.readLong();
            entryId = buffer.readLong();
        } catch (IndexOutOfBoundsException ex) {
            LOG.error("Unparseable response from bookie: " + addr, ex);
            return;
        }
        executor.submitOrdered(ledgerId, new SafeRunnable() {

            @Override
            public void safeRun() {
                switch(type) {
                    case BookieProtocol.ADDENTRY:
                        handleAddResponse(ledgerId, entryId, rc);
                        break;
                    case BookieProtocol.READENTRY:
                        handleReadResponse(ledgerId, entryId, rc, buffer);
                        break;
                    default:
                        LOG.error("Unexpected response, type: " + type + " recieved from bookie: " + addr + " , ignoring");
                }
            }
        });
    }

    void handleAddResponse(long ledgerId, long entryId, int rc) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Got response for add request from bookie: " + addr + " for ledger: " + ledgerId + " entry: " + entryId + " rc: " + rc);
        }
        if (rc != BookieProtocol.EOK) {
            LOG.error("Add for ledger: " + ledgerId + ", entry: " + entryId + " failed on bookie: " + addr + " with code: " + rc);
            rc = BKException.Code.WriteException;
        } else {
            rc = BKException.Code.OK;
        }
        AddCompletion ac;
        ac = addCompletions.remove(new CompletionKey(ledgerId, entryId));
        if (ac == null) {
            LOG.error("Unexpected add response received from bookie: " + addr + " for ledger: " + ledgerId + ", entry: " + entryId + " , ignoring");
            return;
        }
        ac.cb.writeComplete(rc, ledgerId, entryId, addr, ac.ctx);
    }

    void handleReadResponse(long ledgerId, long entryId, int rc, ChannelBuffer buffer) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Got response for read request from bookie: " + addr + " for ledger: " + ledgerId + " entry: " + entryId + " rc: " + rc + "entry length: " + buffer.readableBytes());
        }
        if (rc == BookieProtocol.EOK) {
            rc = BKException.Code.OK;
        } else if (rc == BookieProtocol.ENOENTRY || rc == BookieProtocol.ENOLEDGER) {
            rc = BKException.Code.NoSuchEntryException;
        } else {
            LOG.error("Read for ledger: " + ledgerId + ", entry: " + entryId + " failed on bookie: " + addr + " with code: " + rc);
            rc = BKException.Code.ReadException;
        }
        CompletionKey key = new CompletionKey(ledgerId, entryId);
        ReadCompletion readCompletion = readCompletions.remove(key);
        if (readCompletion == null) {
            readCompletion = readCompletions.remove(new CompletionKey(ledgerId, -1));
        }
        if (readCompletion == null) {
            LOG.error("Unexpected read response recieved from bookie: " + addr + " for ledger: " + ledgerId + ", entry: " + entryId + " , ignoring");
            return;
        }
        readCompletion.cb.readEntryComplete(rc, ledgerId, entryId, buffer.slice(), readCompletion.ctx);
    }

    /**
     * Boiler-plate wrapper classes follow
     * 
     */
    private static class ReadCompletion {

        final ReadEntryCallback cb;

        final Object ctx;

        public ReadCompletion(ReadEntryCallback cb, Object ctx) {
            this.cb = cb;
            this.ctx = ctx;
        }
    }

    private static class AddCompletion {

        final WriteCallback cb;

        final Object ctx;

        public AddCompletion(WriteCallback cb, long size, Object ctx) {
            this.cb = cb;
            this.ctx = ctx;
        }
    }

    private static class CompletionKey {

        long ledgerId;

        long entryId;

        CompletionKey(long ledgerId, long entryId) {
            this.ledgerId = ledgerId;
            this.entryId = entryId;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CompletionKey) || obj == null) {
                return false;
            }
            CompletionKey that = (CompletionKey) obj;
            return this.ledgerId == that.ledgerId && this.entryId == that.entryId;
        }

        @Override
        public int hashCode() {
            return ((int) ledgerId << 16) ^ ((int) entryId);
        }
    }
}
