package biz.xsoftware.impl.nio.cm.basic;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import biz.xsoftware.api.nio.handlers.ConnectionListener;
import biz.xsoftware.api.nio.handlers.DataListener;
import biz.xsoftware.api.nio.handlers.NullWriteCallback;
import biz.xsoftware.api.nio.testutil.nioapi.Select;

final class Helper {

    private static final Logger apiLog = Logger.getLogger(DataListener.class.getName());

    private static final Logger log = Logger.getLogger(Helper.class.getName());

    private static boolean logBufferNextRead = false;

    private Helper() {
    }

    public static String opType(int ops) {
        String retVal = "";
        if ((ops & SelectionKey.OP_ACCEPT) > 0) retVal += "A";
        if ((ops & SelectionKey.OP_CONNECT) > 0) retVal += "C";
        if ((ops & SelectionKey.OP_READ) > 0) retVal += "R";
        if ((ops & SelectionKey.OP_WRITE) > 0) retVal += "W";
        return retVal;
    }

    public static void processKeys(Object id, Set<SelectionKey> keySet) {
        Iterator<SelectionKey> iter = keySet.iterator();
        while (iter.hasNext()) {
            SelectionKey key = null;
            try {
                key = iter.next();
                if (log.isLoggable(Level.FINE)) log.fine(key.attachment() + " ops=" + Helper.opType(key.readyOps()) + " acc=" + key.isAcceptable() + " read=" + key.isReadable() + " write" + key.isWritable());
                processKey(id, key);
            } catch (IOException e) {
                log.log(Level.WARNING, id + "" + key.attachment() + "Processing of key failed, closing channel", e);
                try {
                    if (key != null) key.channel().close();
                } catch (Throwable ee) {
                    log.log(Level.WARNING, id + "" + key.attachment() + "Close of channel failed", ee);
                }
            } catch (CancelledKeyException e) {
                log.log(Level.FINE, id + "" + key.attachment() + "Processing of key failed, but continuing channel manager loop", e);
            } catch (Throwable e) {
                log.log(Level.WARNING, id + "" + key.attachment() + "Processing of key failed, but continuing channel manager loop", e);
                try {
                    key.cancel();
                } catch (Throwable ee) {
                }
            }
        }
        keySet.clear();
    }

    private static void processKey(Object id, SelectionKey key) throws IOException, InterruptedException {
        if (log.isLoggable(Level.FINEST)) log.finest(id + "" + key.attachment() + "proccessing");
        if (!key.channel().isOpen() || !key.isValid()) return;
        if (key.isAcceptable()) {
            Helper.acceptSocket(id, key);
        } else {
            if (key.isConnectable()) Helper.connect(id, key); else {
                if (key.isWritable()) {
                    Helper.write(id, key);
                }
                if (key.isReadable()) {
                    Helper.read(id, key);
                }
            }
        }
    }

    private static void acceptSocket(Object id, SelectionKey key) throws IOException {
        if (log.isLoggable(Level.FINER)) log.finer(id + "" + key.attachment() + "Incoming Connection=" + key);
        WrapperAndListener struct = (WrapperAndListener) key.attachment();
        ConnectionListener cb = struct.getAcceptCallback();
        BasTCPServerChannel channel = (BasTCPServerChannel) struct.getChannel();
        channel.accept("session " + channel.getSession(), cb);
    }

    private static void connect(Object id, SelectionKey key) throws IOException {
        if (log.isLoggable(Level.FINEST)) log.finest(id + "" + key.attachment() + "finishing connect process");
        WrapperAndListener struct = (WrapperAndListener) key.attachment();
        ConnectionListener callback = struct.getConnectCallback();
        BasTCPChannel channel = (BasTCPChannel) struct.getChannel();
        int interests = key.interestOps();
        key.interestOps(interests & (~SelectionKey.OP_CONNECT));
        try {
            channel.finishConnect();
            callback.connected(channel);
        } catch (Exception e) {
            log.log(Level.WARNING, id + "" + key.attachment() + "Could not open connection", e);
            callback.connectFailed(channel, e);
        }
    }

    private static void read(Object id, SelectionKey key) throws IOException {
        if (log.isLoggable(Level.FINEST)) log.finest(id + "" + key.attachment() + "reading data");
        WrapperAndListener struct = (WrapperAndListener) key.attachment();
        DataListener in = struct.getDataHandler();
        BasChannelImpl channel = (BasChannelImpl) struct.getChannel();
        ByteBuffer b = channel.getIncomingDataBuf();
        try {
            int bytes = -1;
            try {
                if (logBufferNextRead) log.info(channel + "buffer=" + b);
                bytes = channel.readImpl(b);
                if (logBufferNextRead) {
                    logBufferNextRead = false;
                    log.info(channel + "buffer2=" + b);
                }
            } catch (IOException e) {
                log.fine("message='" + e.getMessage() + "'");
                String msg = e.getMessage();
                if (msg == null) throw e;
                if (!msg.startsWith("An existing connection was forcibly closed")) throw e;
                bytes = -1;
            }
            processBytes(id, key, b, bytes);
        } catch (PortUnreachableException e) {
            log.log(Level.FINEST, id + "Client sent data to a host or port that is not listening " + "to udp, or udp can't get through to that machine", e);
            in.failure(channel, null, e);
        } catch (NotYetConnectedException e) {
            log.log(Level.WARNING, id + "Can't read until UDPChannel is connected", e);
            in.failure(channel, null, e);
        } catch (IOException e) {
            log.log(Level.FINE, id + "Exception", e);
            channel.close(NullWriteCallback.singleton(), -1);
            in.farEndClosed(channel);
        }
    }

    /**
     * @param id
     * @param b
     * @param bytes
     * @throws IOException
     */
    private static void processBytes(Object id, SelectionKey key, ByteBuffer b, int bytes) throws IOException {
        WrapperAndListener struct = (WrapperAndListener) key.attachment();
        DataListener in = struct.getDataHandler();
        BasChannelImpl channel = (BasChannelImpl) struct.getChannel();
        b.flip();
        if (bytes < 0) {
            if (apiLog.isLoggable(Level.FINE)) apiLog.fine(channel + "far end closed, cancel key, close socket");
            channel.close(NullWriteCallback.singleton(), -1);
            in.farEndClosed(channel);
        } else if (bytes > 0) {
            if (apiLog.isLoggable(Level.FINER)) apiLog.finer(channel + "READ bytes=" + bytes);
            in.incomingData(channel, b);
            if (b.hasRemaining()) {
                log.warning(id + "Discarding unread data(" + b.remaining() + ") from class=" + in.getClass());
            }
        } else {
            if (log.isLoggable(Level.WARNING)) log.warning(channel + "READ 0 bytes(this is strange)...buffer=" + b);
            logBufferNextRead = true;
            assert false : "Should not occur";
        }
        b.clear();
    }

    private static void write(Object id, SelectionKey key) throws IOException, InterruptedException {
        if (log.isLoggable(Level.FINEST)) log.finest(key.attachment() + "writing data");
        WrapperAndListener struct = (WrapperAndListener) key.attachment();
        BasChannelImpl channel = (BasChannelImpl) struct.getChannel();
        if (log.isLoggable(Level.FINER)) log.finer(channel + "notifying channel of write");
        channel.writeAll();
    }

    static void unregisterSelectableChannel(RegisterableChannelImpl channel, int ops) {
        SelectorManager2 mgr = channel.getSelectorManager();
        if (!Thread.currentThread().equals(mgr.getThread())) throw new RuntimeException(channel + "Bug, changing selector keys can only be done " + "on registration thread because there is not synchronization");
        Select select = channel.getSelectorManager().getSelector();
        SelectionKey key = channel.keyFor(select);
        if (key == null || !key.isValid()) return;
        int previous = key.interestOps();
        int opsNow = previous & ~ops;
        key.interestOps(opsNow);
        if (key.attachment() != null) {
            WrapperAndListener struct = (WrapperAndListener) key.attachment();
            struct.removeListener(ops);
        }
    }
}
