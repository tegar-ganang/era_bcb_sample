package org.gamio.channel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.gamio.buffer.Buffer;
import org.gamio.conf.GateProps;
import org.gamio.logging.Log;
import org.gamio.logging.Logger;
import org.gamio.res.Resource;
import org.gamio.system.Context;
import org.gamio.util.GmQueue;
import org.gamio.util.Helper;
import org.gamio.work.Worker;

/**
 * @author Agemo Cui <agemocui@gamio.org>
 * @version $Rev: 23 $ $Date: 2008-10-05 21:00:52 -0400 (Sun, 05 Oct 2008) $
 */
public abstract class Channel implements Resource {

    private static final Log log = Logger.getLogger(Channel.class);

    private Integer id = null;

    private String name = null;

    private GateProps gateProps = null;

    private String path = null;

    private String remoteAddr = null;

    private int remotePort = 0;

    private String localAddr = null;

    private int localPort = 0;

    private SelectionKey selectionKey = null;

    private SocketChannel socketChannel = null;

    private final OnAccept onAccept = new OnAccept();

    private final OnRead onRead = new OnRead();

    private final OnWrite onWrite = new OnWrite(getWriteQueueCacheSize());

    private WriteState writeState = null;

    private ChannelState channelState = ChannelClosed.getInstance();

    private final Lock readLock = new ReentrantLock();

    private final Lock writeLock = new ReentrantLock();

    private final Lock lock = new ReentrantLock();

    static enum RCStatus {

        E_OK, E_ALREADYCLOSED, E_SENDERROR
    }

    private final class OnAccept implements Runnable {

        public void run() {
            open();
            log.debug(this);
            getPeersInfo();
            Context.getInstance().getChannelManager().accept(Channel.this);
        }

        @Override
        public String toString() {
            return Helper.buildString("Server[name<", gateProps.getName(), ">, id<", gateProps.getId(), ">] got a connection - ", Channel.this, ": ", socketChannel);
        }
    }

    private final class OnRead implements Runnable {

        private int msgLen = 0;

        private Buffer buffer = null;

        private ArrayList<Buffer> dataList = new ArrayList<Buffer>();

        public void run() {
            Msglet msglet = getMsglet();
            if (msglet == null) return;
            int n = 0;
            readLock.lock();
            try {
                n = channelState.doRead(this, msglet);
            } catch (ChannelClosedException e) {
                return;
            } finally {
                readLock.unlock();
            }
            if (n == 0) {
                lock.lock();
                try {
                    channelState.interestRead(Channel.this);
                } catch (ChannelClosedException e) {
                    return;
                } finally {
                    lock.unlock();
                }
            } else if (n > 0) {
                boolean ok = false;
                ChannelMsgListener channelMsgListener = getChannelMsgListener();
                lock.lock();
                try {
                    if (ok = channelState.doSplitMsg(this, msglet)) {
                        log();
                        channelMsgListener.onMessage(Channel.this, dataList);
                    }
                } catch (ChannelClosedException e) {
                    return;
                } catch (Exception e) {
                    ok = false;
                    log.error(e, "Error - ", Channel.this);
                } finally {
                    dataList.clear();
                    lock.unlock();
                }
                if (ok) {
                    try {
                        channelMsgListener.onMessageComplete(Channel.this);
                    } catch (Exception e) {
                        Channel.this.close();
                        log.error(e, "Error - ", Channel.this);
                    }
                } else Channel.this.close();
            } else Channel.this.close();
        }

        public void close() {
            if (buffer != null) {
                buffer.close();
                buffer = null;
                msgLen = 0;
            }
        }

        private void log() {
            if (!log.isDebugEnabled()) return;
            for (Buffer data : dataList) {
                StringBuilder strBldr = Helper.getStringBuilder();
                strBldr.append(Helper.getLineSeparator()).append(Channel.this).append(" got message:").append(Helper.getLineSeparator()).append("length: ").append(data.size()).append(Helper.getLineSeparator());
                data.dump(strBldr);
                log.debug(strBldr.toString());
            }
        }

        public int read(Msglet msglet) {
            ByteBuffer byteBuffer = ((Worker) Thread.currentThread()).getByteBuffer();
            for (; ; ) {
                int n = 0;
                try {
                    n = socketChannel.read(byteBuffer);
                } catch (Exception e) {
                    log.error(e, Channel.this, " failed to read");
                    return -1;
                }
                if (n < 0) {
                    log.debug(Channel.this, ": The peer closed the connection");
                    return -1;
                } else if (n == 0) return 0;
                byteBuffer.flip();
                if (buffer == null) buffer = Context.getInstance().getBufferFactory().create();
                buffer.write(byteBuffer);
                if (msgLen <= 0) {
                    try {
                        msgLen = msglet.parseMsgLength(buffer);
                    } catch (Exception e) {
                        log.error(e, "Failed to parse the message length - ", Channel.this);
                        return -1;
                    }
                }
                if (msgLen > 0 && buffer.size() >= msgLen) return msgLen; else byteBuffer.clear();
            }
        }

        public boolean splitMsg(Msglet msglet) {
            for (; ; ) {
                Buffer data = normalize();
                dataList.add(data);
                try {
                    if (buffer == null || (msgLen = msglet.parseMsgLength(buffer)) <= 0 || msgLen > buffer.size()) return true;
                } catch (Exception e) {
                    log.error(e, "Failed to parse the message length - ", Channel.this);
                    return false;
                }
            }
        }

        private Buffer normalize() {
            Buffer data = buffer;
            if (buffer.size() > msgLen) {
                Buffer tempBuf = Context.getInstance().getBufferFactory().create();
                buffer.skip(msgLen);
                tempBuf.write(buffer);
                buffer.rewind();
                buffer.trimToSize(msgLen);
                buffer = tempBuf;
            } else buffer = null;
            msgLen = 0;
            return data;
        }
    }

    private Msglet getMsglet() {
        GateProps temp = gateProps;
        if (temp == null) return null;
        Msglet msglet = temp.getMsglet();
        if (msglet == null) {
            log.error("Msglet[", temp.getMsgletRef(), "] Not Found");
            close();
        }
        return msglet;
    }

    private final class OnWrite implements Runnable {

        private Buffer data = null;

        private GmQueue<Buffer> queue = null;

        public OnWrite() {
            this(50);
        }

        public OnWrite(int cacheSize) {
            queue = new GmQueue<Buffer>(cacheSize);
        }

        public void inQueue(Buffer data) {
            queue.push(data);
        }

        public boolean isDone() {
            return queue.size() == 0;
        }

        public boolean isSent() {
            return data == null && queue.size() == 0;
        }

        public void close() {
            if (data != null) data.close();
            while ((data = queue.pop()) != null) data.close();
            data = null;
        }

        public void run() {
            do {
                int n = 0;
                writeLock.lock();
                try {
                    n = channelState.doWrite(this);
                } catch (ChannelClosedException e) {
                    break;
                } finally {
                    writeLock.unlock();
                }
                if (n < 0) {
                    Channel.this.close();
                    break;
                } else if (n == 0) {
                    lock.lock();
                    try {
                        channelState.interestWrite(Channel.this);
                    } catch (ChannelClosedException e) {
                    } finally {
                        lock.unlock();
                    }
                    break;
                }
            } while (!doWriteZombie());
        }

        private boolean doWriteZombie() {
            lock.lock();
            try {
                if (!onWrite.isDone()) return false;
                changeWriteState(WriteComplete.getInstance());
            } finally {
                lock.unlock();
            }
            return true;
        }

        public int write() {
            ByteBuffer byteBuffer = ((Worker) Thread.currentThread()).getByteBuffer();
            if (data == null) data = queue.pop();
            while (data != null) {
                while (data.remaining() > 0) {
                    data.read(byteBuffer);
                    byteBuffer.flip();
                    while (byteBuffer.hasRemaining()) {
                        try {
                            if (socketChannel.write(byteBuffer) == 0) return 0;
                        } catch (Exception e) {
                            log.error(e, Channel.this, " failed to write");
                            return -1;
                        }
                    }
                    byteBuffer.clear();
                }
                log();
                data.close();
                data = queue.pop();
            }
            return 1;
        }

        private void log() {
            if (!log.isDebugEnabled()) return;
            StringBuilder strBldr = Helper.getStringBuilder();
            strBldr.append(Helper.getLineSeparator()).append(Channel.this).append(" sent message:").append(Helper.getLineSeparator()).append("length: ").append(data.size()).append(Helper.getLineSeparator());
            data.dump(strBldr);
            log.debug(strBldr.toString());
        }
    }

    private interface WriteState {

        public void write(Channel channel, Buffer data);
    }

    private static final class OnWriting implements WriteState {

        private static OnWriting onWriting = new OnWriting();

        public static WriteState getInstance() {
            return onWriting;
        }

        public void write(Channel channel, Buffer data) {
            channel.onWrite.inQueue(data);
        }
    }

    private static final class WriteComplete implements WriteState {

        private static WriteComplete writeComplete = new WriteComplete();

        public static WriteState getInstance() {
            return writeComplete;
        }

        public void write(Channel channel, Buffer data) {
            channel.onWrite.inQueue(data);
            channel.changeWriteState(OnWriting.getInstance());
            Context.getInstance().getChannelManager().onWriteRequired(channel);
        }
    }

    interface ChannelState {

        public void open(Channel channel);

        public RCStatus close(Channel channel);

        public void send(Channel channel, Buffer data) throws Exception;

        public void onFinishConnect(Channel channel) throws Exception;

        public Runnable onRead(Channel channel) throws ChannelClosedException;

        public Runnable onWrite(Channel channel) throws ChannelClosedException;

        public void write(Channel channel, Buffer data) throws ChannelClosedException;

        public int doRead(OnRead onRead, Msglet msglet) throws ChannelClosedException;

        public int doWrite(OnWrite onWrite) throws ChannelClosedException;

        public void interestRead(Channel channel) throws ChannelClosedException;

        public void interestWrite(Channel channel) throws ChannelClosedException;

        public boolean doSplitMsg(OnRead onRead, Msglet msglet) throws ChannelClosedException;
    }

    private static final class ChannelOpened implements ChannelState {

        private static ChannelOpened channelOpened = new ChannelOpened();

        public static ChannelState getInstance() {
            return channelOpened;
        }

        public void open(Channel channel) {
        }

        public RCStatus close(Channel channel) {
            try {
                channel.socketChannel.close();
            } catch (IOException e) {
                log.error(e, channel, " failed to close");
            }
            log.debug(channel, " was closed");
            RCStatus rcs = channel.onWrite.isSent() ? RCStatus.E_OK : RCStatus.E_SENDERROR;
            channel.id = null;
            channel.name = null;
            channel.gateProps = null;
            channel.path = null;
            channel.socketChannel = null;
            channel.selectionKey = null;
            channel.writeState = null;
            channel.remoteAddr = null;
            channel.remotePort = 0;
            channel.localAddr = null;
            channel.localPort = 0;
            channel.onRead.close();
            channel.onWrite.close();
            channel.changeChannelState(ChannelClosed.getInstance());
            return rcs;
        }

        public void send(Channel channel, Buffer data) throws Exception {
            channel.reactivate();
            channel.writeState.write(channel, data);
        }

        public void onFinishConnect(Channel channel) throws Exception {
            if (channel.socketChannel.finishConnect()) {
                log.debug("Client[name<", channel.gateProps.getName(), ">, id<", channel.gateProps.getId(), ">] initiated a connection, ", channel, ": ", channel.socketChannel);
                channel.getPeersInfo();
            } else throw new Exception("Failed to connect");
        }

        public Runnable onRead(Channel channel) throws ChannelClosedException {
            return channel.onRead;
        }

        public Runnable onWrite(Channel channel) throws ChannelClosedException {
            return channel.onWrite;
        }

        public void write(Channel channel, Buffer data) throws ChannelClosedException {
            channel.writeState.write(channel, data);
        }

        public int doRead(OnRead onRead, Msglet msglet) throws ChannelClosedException {
            return onRead.read(msglet);
        }

        public int doWrite(OnWrite onWrite) throws ChannelClosedException {
            return onWrite.write();
        }

        public void interestRead(Channel channel) throws ChannelClosedException {
            Context.getInstance().getChannelManager().onReadRequired(channel);
        }

        public void interestWrite(Channel channel) throws ChannelClosedException {
            Context.getInstance().getChannelManager().onWriteRequired(channel);
        }

        public boolean doSplitMsg(OnRead onRead, Msglet msglet) throws ChannelClosedException {
            return onRead.splitMsg(msglet);
        }
    }

    private static final class ChannelClosed implements ChannelState {

        private static ChannelClosed channelClosed = new ChannelClosed();

        public static ChannelState getInstance() {
            return channelClosed;
        }

        public void open(Channel channel) {
            channel.id = channel.generateId();
            channel.changeWriteState(WriteComplete.getInstance());
            channel.changeChannelState(ChannelOpened.getInstance());
        }

        public RCStatus close(Channel channel) {
            return RCStatus.E_ALREADYCLOSED;
        }

        public void send(Channel channel, Buffer data) throws Exception {
            open(channel);
            channel.socketChannel = SocketChannel.open();
            Context.getInstance().getChannelManager().onConnectRequired(channel);
            channel.socketChannel.connect(new InetSocketAddress(channel.gateProps.getIP(), channel.gateProps.getPort()));
            channel.writeState.write(channel, data);
        }

        public void onFinishConnect(Channel channel) throws Exception {
        }

        public Runnable onRead(Channel channel) throws ChannelClosedException {
            throw new ChannelClosedException();
        }

        public Runnable onWrite(Channel channel) throws ChannelClosedException {
            throw new ChannelClosedException();
        }

        public void write(Channel channel, Buffer data) throws ChannelClosedException {
            data.close();
            throw new ChannelClosedException();
        }

        public int doRead(OnRead onRead, Msglet msglet) throws ChannelClosedException {
            throw new ChannelClosedException();
        }

        public int doWrite(OnWrite onWrite) throws ChannelClosedException {
            throw new ChannelClosedException();
        }

        public void interestRead(Channel channel) throws ChannelClosedException {
            throw new ChannelClosedException();
        }

        public void interestWrite(Channel channel) throws ChannelClosedException {
            throw new ChannelClosedException();
        }

        public boolean doSplitMsg(OnRead onRead, Msglet msglet) throws ChannelClosedException {
            throw new ChannelClosedException();
        }
    }

    public final Integer getId() {
        return id;
    }

    public final String getGateId() {
        GateProps temp = gateProps;
        return temp != null ? temp.getId() : null;
    }

    public void setGateProps(GateProps gateProps) {
        this.gateProps = gateProps;
    }

    public final String getPath() {
        return path;
    }

    public final void setPath(String path) {
        this.path = path;
    }

    public final void configureBlocking(boolean block) throws IOException {
        socketChannel.configureBlocking(block);
    }

    public final void register(Selector selector, int ops) throws ClosedChannelException {
        selectionKey = socketChannel.register(selector, ops, this);
    }

    public final int interestOps() throws ChannelClosedException {
        try {
            return selectionKey.interestOps();
        } catch (NullPointerException e) {
            throw new ChannelClosedException();
        }
    }

    public final void interestOps(int ops) throws ChannelClosedException {
        try {
            selectionKey.interestOps(ops);
        } catch (NullPointerException e) {
            throw new ChannelClosedException();
        }
    }

    public int getTimeout() {
        return gateProps.getTimeout();
    }

    public final Runnable onAccept(SocketChannel socketChannel, GateProps gateProps) {
        this.socketChannel = socketChannel;
        this.gateProps = gateProps;
        return onAccept;
    }

    public final Runnable onRead() throws ChannelClosedException {
        lock.lock();
        try {
            return channelState.onRead(this);
        } finally {
            lock.unlock();
        }
    }

    public final Runnable onWrite() throws ChannelClosedException {
        lock.lock();
        try {
            return channelState.onWrite(this);
        } finally {
            lock.unlock();
        }
    }

    public final void write(Buffer data) throws ChannelClosedException {
        lock.lock();
        try {
            channelState.write(this, data);
        } finally {
            lock.unlock();
        }
    }

    public void send(Buffer data) throws Exception {
        lock.lock();
        try {
            channelState.send(this, data);
        } finally {
            lock.unlock();
        }
    }

    public final void onFinishConnect() throws Exception {
        lock.lock();
        try {
            channelState.onFinishConnect(this);
        } finally {
            lock.unlock();
        }
    }

    public final void onTimeout() {
        String temp = toString();
        if (id != null) {
            log.warn(temp, " is timeout");
            close();
        }
    }

    public void release() {
    }

    public String getLocalAddr() {
        return localAddr;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public int getRemotePort() {
        return remotePort;
    }

    @Override
    public String toString() {
        if (name == null) {
            StringBuilder strBldr = Helper.getRawStringBuilder();
            int start = strBldr.length();
            strBldr.append(getTypeName()).append("Channel[id<").append(getGateId());
            Helper.dumpInt32(strBldr, id);
            strBldr.append(">, sid<").append(getSessionId()).append(">]");
            name = strBldr.substring(start);
            strBldr.delete(start, strBldr.length());
        }
        return name;
    }

    protected final void open() {
        lock.lock();
        try {
            channelState.open(this);
        } finally {
            lock.unlock();
        }
    }

    private final void getPeersInfo() {
        Socket socket = socketChannel.socket();
        remoteAddr = socket.getInetAddress().getHostAddress();
        remotePort = socket.getPort();
        localAddr = socket.getLocalAddress().getHostAddress();
        localPort = socket.getLocalPort();
    }

    RCStatus rawClose() {
        lock.lock();
        readLock.lock();
        writeLock.lock();
        try {
            return channelState.close(this);
        } finally {
            writeLock.unlock();
            readLock.unlock();
            lock.unlock();
        }
    }

    final ChannelState getChannelState() {
        return channelState;
    }

    final void clearName() {
        name = null;
    }

    final Lock getLock() {
        return lock;
    }

    private final void changeWriteState(WriteState writeState) {
        this.writeState = writeState;
    }

    private final void changeChannelState(ChannelState channelState) {
        this.channelState = channelState;
    }

    public abstract void activate();

    public abstract void reactivate();

    public abstract String getSessionId();

    public abstract void setSessionId(String sessionId);

    public abstract Integer getOriginalChannelId();

    public abstract void setOriginalChannelId(Integer originalChannelId);

    public abstract void close();

    protected abstract int getWriteQueueCacheSize();

    protected abstract String getTypeName();

    protected abstract int generateId();

    protected abstract ChannelMsgListener getChannelMsgListener();
}
