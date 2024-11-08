package lia.util.net.copy.transport;

import gui.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import lia.util.net.common.Config;
import lia.util.net.common.DirectByteBufferPool;
import lia.util.net.common.HeaderBufferPool;
import lia.util.net.common.Utils;
import lia.util.net.copy.FileBlock;
import lia.util.net.copy.FileBlockProducer;
import lia.util.net.copy.transport.internal.FDTSelectionKey;

/**
 * The one and (not the) only writer over a channel
 * 
 * @author ramiro
 */
public class SocketWriterTask extends SocketTask {

    private static final Log logger = Log.getLoggerInstance();

    volatile FDTSelectionKey fdtSelectionKey;

    private static final int BUFF_LEN_SIZE = Config.NETWORK_BUFF_LEN_SIZE;

    private TCPSessionWriter master;

    private FileBlockProducer fileBlockProducer;

    private final boolean isNetTest;

    SocketWriterTask(BlockingQueue<FDTSelectionKey> readyChannelsQueue, FileBlockProducer fileBlockProducer, TCPSessionWriter master) {
        super(readyChannelsQueue);
        this.fileBlockProducer = fileBlockProducer;
        this.master = master;
        this.isNetTest = master.isNetTest();
    }

    private long writeToChannel(SocketChannel sc, ByteBuffer[] buffsToWrite) throws IOException {
        final ByteBuffer[] bToWrite = buffsToWrite;
        for (int i = 0; i < bToWrite.length; i++) {
            if (isNetTest) {
                for (final ByteBuffer b : bToWrite) {
                    b.position(0);
                    b.limit(b.capacity());
                }
            }
        }
        return sc.write(bToWrite);
    }

    private long writeData() throws IOException, InterruptedException {
        final FDTWriterKeyAttachement attach = (FDTWriterKeyAttachement) fdtSelectionKey.attachment();
        final boolean connectCookieSent = attach.connectCookieSent.get();
        long count = -1;
        final SocketChannel sc = fdtSelectionKey.channel();
        int mss = fdtSelectionKey.getMSS();
        int bufferSize = BUFF_LEN_SIZE;
        mss = -1;
        if (mss > 0) {
            bufferSize = mss;
        }
        final boolean logFinest = logger.isLoggable(Level.FINEST);
        if (logFinest) {
            logger.log(Level.FINEST, "Using MSS: " + bufferSize + " for socket channel: " + sc);
        }
        final DirectByteBufferPool dbPool = DirectByteBufferPool.getInstance();
        final HeaderBufferPool hbPool = HeaderBufferPool.getInstance();
        for (; ; ) {
            count = -1;
            if (!attach.hasBuffers()) {
                if (isNetTest) {
                    ByteBuffer bb = null;
                    ByteBuffer hh = null;
                    try {
                        bb = dbPool.take();
                        hh = hbPool.take();
                    } finally {
                        if (isClosed() || Thread.currentThread().isInterrupted()) {
                            if (bb != null) {
                                dbPool.put(bb);
                            }
                            if (hh != null) {
                                hbPool.put(hh);
                            }
                            return count;
                        }
                    }
                    if (hh == null || bb == null) {
                        if (bb != null) {
                            dbPool.put(bb);
                        }
                        if (hh != null) {
                            hbPool.put(hh);
                        }
                        attach.updateLastOperation();
                        readyChannelsQueue.offer(fdtSelectionKey);
                        if (logFinest) {
                            logger.log(Level.FINEST, " [ SocketWriterTask ] Empty FD queue. Added SK: " + fdtSelectionKey + " NEW Sel Queue: " + readyChannelsQueue);
                        }
                        return 0;
                    }
                    bb.position(0);
                    bb.limit(bb.capacity());
                    hh.position(0);
                    hh.limit(hh.capacity());
                    attach.setBuffers(hh, bb);
                } else {
                    FileBlock fb = fileBlockProducer.poll(5, TimeUnit.SECONDS);
                    if (fb == null) {
                        attach.updateLastOperation();
                        readyChannelsQueue.offer(fdtSelectionKey);
                        if (logFinest) {
                            logger.log(Level.FINEST, " [ SocketWriterTask ] Empty FD queue. Added SK: " + fdtSelectionKey + " NEW Sel Queue: " + readyChannelsQueue);
                        }
                        return 0;
                    }
                    boolean bRet = false;
                    try {
                        bRet = FDTWriterKeyAttachement.fromFileBlock(fb, attach);
                    } finally {
                        if (!bRet) {
                            attach.recycleBuffers();
                            if (fb != null && fb.buff != null) {
                                dbPool.put(fb.buff);
                                fb = null;
                            }
                        }
                    }
                }
            }
            count = -1;
            final int cPos = attach.payload().position();
            long canWrite = (attach.payloadSize - cPos);
            if (canWrite > bufferSize) {
                canWrite = bufferSize;
            }
            if (master.getRateLimit() > 0) {
                final long shouldWrite = master.awaitSend(canWrite);
                if (shouldWrite < canWrite) {
                    canWrite = shouldWrite;
                }
                attach.payload().limit(cPos + (int) canWrite);
            }
            if (!connectCookieSent) {
                ByteBuffer connectCookie = null;
                try {
                    connectCookie = dbPool.take();
                    if (!isNetTest) {
                        connectCookie.limit(1 + 16);
                    } else {
                        connectCookie.limit(connectCookie.capacity());
                    }
                    connectCookie.put((byte) 1).putLong(master.fdtSession.sessionID().getMostSignificantBits()).putLong(master.fdtSession.sessionID().getLeastSignificantBits());
                    if (isNetTest) {
                        connectCookie.position(connectCookie.capacity());
                    }
                    connectCookie.flip();
                    long tmpW = 0;
                    while (connectCookie.hasRemaining()) {
                        count = sc.write(connectCookie);
                        tmpW += count;
                        if (isNetTest && tmpW >= 18) {
                            break;
                        }
                    }
                    attach.connectCookieSent.set(true);
                } finally {
                    if (connectCookie != null) {
                        dbPool.put(connectCookie);
                        if (!attach.connectCookieSent.get()) {
                            close("Unable to send connect cookie", new IOException("Unable to send connect cookie"));
                        }
                    }
                }
            }
            while ((count = writeToChannel(sc, attach.asArray())) > 0) {
                if (isNetTest) {
                    fdtSelectionKey.opCount = 0;
                    addAndGetTotalBytes(count);
                    master.addAndGetTotalBytes(count);
                    continue;
                }
                attach.payload().limit(attach.payloadSize);
                fdtSelectionKey.opCount = 0;
                addAndGetTotalBytes(count);
                master.addAndGetTotalBytes(count);
                if (logFinest) {
                    logger.log(Level.FINEST, " [ SocketWriterTask ] Socket: " + sc.socket() + " written: " + count);
                }
                attach.updateLastOperation();
                if (attach.isPayloadWritten()) {
                    final long ladd = attach.payload().limit();
                    addAndGetUtilBytes(ladd);
                    master.addAndGetUtilBytes(ladd);
                    if (isNetTest) {
                        final ByteBuffer h = attach.header();
                        h.position(0);
                        h.limit(h.capacity());
                        final ByteBuffer p = attach.payload();
                        p.position(0);
                        p.limit(p.capacity());
                    } else {
                        attach.recycleBuffers();
                        FileBlock fb = fileBlockProducer.poll(5, TimeUnit.SECONDS);
                        if (fb == null) {
                            attach.updateLastOperation();
                            readyChannelsQueue.offer(fdtSelectionKey);
                            return 0;
                        }
                        boolean bRet = false;
                        try {
                            bRet = FDTWriterKeyAttachement.fromFileBlock(fb, attach);
                        } finally {
                            if (!bRet) {
                                attach.recycleBuffers();
                                if (fb != null && fb.buff != null) {
                                    dbPool.put(fb.buff);
                                    fb = null;
                                }
                            }
                        }
                    }
                }
                count = -1;
                canWrite = (attach.payloadSize - attach.payload().position());
                if (canWrite > bufferSize) {
                    canWrite = bufferSize;
                }
                if (master.getRateLimit() > 0) {
                    final long shouldWrite = master.awaitSend(canWrite);
                    if (shouldWrite < canWrite) {
                        canWrite = shouldWrite;
                    }
                    attach.payload().limit(attach.payload().position() + (int) canWrite);
                }
            }
            if (count == 0) {
                if (isNetTest && isBlocking) {
                    final ByteBuffer buff = attach.payload();
                    buff.position(0);
                    buff.limit(buff.capacity());
                    continue;
                }
                attach.payload().limit(attach.payloadSize);
                fdtSelectionKey.renewInterest();
                return count;
            }
            attach.payload().limit(attach.payloadSize);
            return count;
        }
    }

    private void recycleBuffers() {
        try {
            if (fdtSelectionKey != null) {
                FDTWriterKeyAttachement attach = (FDTWriterKeyAttachement) fdtSelectionKey.attachment();
                if (attach != null) {
                    attach.recycleBuffers();
                }
                fdtSelectionKey = null;
            }
        } catch (Throwable t1) {
            logger.log(Level.WARNING, " Got exception trying to recover the buffers and returning them to pool", t1);
        }
    }

    @Override
    public void internalClose() {
        try {
            if (fdtSelectionKey != null) {
                fdtSelectionKey.cancel();
                FDTWriterKeyAttachement attach = (FDTWriterKeyAttachement) fdtSelectionKey.attachment();
                if (attach != null) {
                    attach.recycleBuffers();
                }
                SocketChannel sc = fdtSelectionKey.channel();
                try {
                    sc.close();
                } catch (Throwable t) {
                }
            }
            if (fdtSelectionKey != null) {
                master.workerDown(fdtSelectionKey, downCause());
            } else {
                master.workerDown(null, downCause());
            }
            recycleBuffers();
        } catch (Throwable t1) {
            logger.logError("\n\n\n\n\\n ========================= \n\n\n");
            logger.logError(t1);
            logger.logError("\n\n\n\n\\n ========================= \n\n\n");
        }
    }

    public void run() {
        try {
            for (; ; ) {
                fdtSelectionKey = null;
                FDTSelectionKey iFDTSelectionKey = null;
                while ((iFDTSelectionKey = readyChannelsQueue.poll(2, TimeUnit.SECONDS)) == null) {
                    if (isClosed()) {
                        break;
                    }
                }
                fdtSelectionKey = iFDTSelectionKey;
                if (isClosed()) break;
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, " writeDate for SK: " + Utils.toStringSelectionKey(fdtSelectionKey) + " SQSize : " + readyChannelsQueue.size() + " SelQueue: " + readyChannelsQueue);
                }
                if (writeData() < 0) {
                    return;
                }
            }
        } catch (Throwable t) {
            master.workerDown(fdtSelectionKey, t);
            close("SocketWriterTask got exception ", t);
        } finally {
            try {
                if (fdtSelectionKey != null) {
                    FDTKeyAttachement attach = fdtSelectionKey.attachment();
                    if (attach != null) {
                        attach.recycleBuffers();
                    }
                }
            } catch (Throwable t1) {
                logger.log(Level.WARNING, " Got exception trying to return buffers to the pool", t1);
            }
            master.workerDown(fdtSelectionKey, null);
            close(null, null);
        }
    }
}
