package com.taobao.top.analysis.transport.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.taobao.top.analysis.transport.BasePacket;
import com.taobao.top.analysis.transport.Connection;
import com.taobao.top.analysis.transport.Listener;
import com.taobao.top.analysis.transport.NioBufferReadWorker;
import com.taobao.top.analysis.transport.TransportConstants;
import com.taobao.top.analysis.util.TransportUtil;

/**
 * 简单的NIO客户端
 * 
 * @author fangweng
 * 
 */
public class ClientConnection implements Connection {

    private static final Log log = LogFactory.getLog(ClientConnection.class);

    private InetSocketAddress address = null;

    private Selector selector = null;

    private SocketChannel channel = null;

    private SelectionKey key = null;

    private Queue<BasePacket> writeQueue = new ConcurrentLinkedQueue<BasePacket>();

    private Map<Long, BasePacket> waitReplyList = new ConcurrentHashMap<Long, BasePacket>(16);

    private int connectState = TransportConstants.CONN_STATUS_NOT_CONNECT;

    private static AtomicInteger waitThreadCount = new AtomicInteger();

    private LinkedBlockingQueue<ByteBuffer> readBufferQueue;

    private int blockSize = 1024;

    private NioBufferReadWorker innerWorker;

    private AtomicBoolean isConnect = new AtomicBoolean(false);

    private long lastConnectTime = 0;

    ConcurrentMap<Long, Connection> connMap;

    public ClientConnection(Selector selector, ConcurrentMap<Long, Connection> connMap, InetSocketAddress address) {
        this.selector = selector;
        this.address = address;
        this.readBufferQueue = new LinkedBlockingQueue<ByteBuffer>(TransportConstants.READQUEUE_MAX_COUNT);
        this.connMap = connMap;
        innerWorker = new NioBufferReadWorker(readBufferQueue, waitReplyList);
        innerWorker.setName("TCPClient ReadBufferWorker");
        innerWorker.setDaemon(true);
        innerWorker.start();
    }

    public boolean needReConnect() {
        if (channel == null || (channel != null && !channel.isConnected())) return true;
        if ((lastConnectTime == 0) || (lastConnectTime > (System.currentTimeMillis() - 5000))) {
            return false;
        }
        return (channel == null);
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void open() {
        if (channel == null) {
            boolean bConn = false;
            lastConnectTime = System.currentTimeMillis();
            connectState = TransportConstants.CONN_STATUS_CONNECTING;
            try {
                channel = SocketChannel.open();
                channel.configureBlocking(false);
                channel.connect(address);
                bConn = true;
                key = channel.register(selector, SelectionKey.OP_CONNECT, this);
                if (log.isWarnEnabled()) log.warn(new StringBuilder().append("新连接, size: ").append(selector.keys().size()).append(", addr:").append(address.toString()));
            } catch (IOException e) {
                if (bConn) {
                    try {
                        connectState = TransportConstants.CONN_STATUS_NOT_CONNECT;
                        channel.close();
                    } catch (IOException e1) {
                    }
                }
                channel = null;
                log.error(e, e);
            }
        }
    }

    /**
	 * 关闭连接
	 */
    public void close() {
        try {
            if (channel != null) {
                connMap.remove(TransportUtil.hostToLong(address.getAddress().getHostAddress(), address.getPort()));
                key.cancel();
                channel.close();
                channel = null;
                isConnect = new AtomicBoolean(false);
                writeQueue.clear();
                readBufferQueue.clear();
                waitReplyList.clear();
                innerWorker.stopWorker();
                innerWorker = null;
                if (log.isWarnEnabled()) log.warn("关闭连接, addr:" + address.toString());
            }
            connectState = TransportConstants.CONN_STATUS_NOT_CONNECT;
        } catch (IOException e) {
            channel = null;
            log.error(e, e);
        }
    }

    public void onError(Throwable e) {
        close();
        log.error(e, e);
    }

    public void onConnection() {
        try {
            if (connectState != TransportConstants.CONN_STATUS_CONNECTED) if (!isConnect.compareAndSet(false, true)) return;
            SocketChannel ch = (SocketChannel) key.channel();
            if (ch.finishConnect()) {
                ch.socket().setReceiveBufferSize(TransportConstants.INOUT_BUFFER_SIZE);
                ch.socket().setSendBufferSize(TransportConstants.INOUT_BUFFER_SIZE);
                ch.socket().setSoTimeout(TransportConstants.DEFAULT_TIMEOUT);
                connectState = TransportConstants.CONN_STATUS_CONNECTED;
                key.interestOps(SelectionKey.OP_READ);
                this.selector.wakeup();
            } else {
                close();
            }
        } catch (IOException e) {
            close();
            log.error(address.toString(), e);
        }
    }

    public void onRead() {
        try {
            int ret = 0;
            do {
                ByteBuffer readByteBuffer = ByteBuffer.allocate(blockSize);
                ret = channel.read(readByteBuffer);
                if (ret > 0) {
                    readByteBuffer.flip();
                    readBufferQueue.add(readByteBuffer);
                }
            } while (ret > 0);
        } catch (IOException e) {
            close();
            log.error("Close connection! OnRead error...");
            log.error(e, e);
        }
    }

    public void onWrite() {
        try {
            BasePacket packet = null;
            if (writeQueue.size() > TransportConstants.BATCH_WRITE_PAGESIZE) {
                log.error("flush write");
                ByteBuffer[] buffers = new ByteBuffer[TransportConstants.DEFAULT_WRITE_PAGE];
                int index = 0;
                while ((packet = writeQueue.poll()) != null) {
                    packet.getByteBuffer().putInt(TransportConstants.PACKET_HEADER_PORTPOS, channel.socket().getLocalPort());
                    ByteBuffer b = packet.getByteBuffer();
                    if (b == null) {
                        packet.notify();
                        return;
                    }
                    b.flip();
                    checkMalloc(b.remaining());
                    packet.setStartTime(System.currentTimeMillis());
                    waitReplyList.put(packet.getSequence(), packet);
                    buffers[index] = b;
                    index += 1;
                    if (index == TransportConstants.DEFAULT_WRITE_PAGE) {
                        channel.write(buffers);
                        index = 0;
                    }
                }
                if (index > 0) {
                    channel.write(buffers, 0, index);
                }
            } else {
                while ((packet = writeQueue.poll()) != null) {
                    ByteBuffer b = packet.getByteBuffer();
                    if (b == null) {
                        packet.notify();
                        return;
                    }
                    b.putInt(TransportConstants.PACKET_HEADER_PORTPOS, channel.socket().getLocalPort());
                    b.flip();
                    checkMalloc(b.remaining());
                    packet.setStartTime(System.currentTimeMillis());
                    waitReplyList.put(packet.getSequence(), packet);
                    while (b.remaining() > 0) channel.write(b);
                }
            }
        } catch (IOException e) {
            close();
            log.error("Close connection! OnWrite error...");
            log.error(e, e);
        }
    }

    /**
	 * 同步发送一个BasePacket
	 */
    public BasePacket sendPacket(BasePacket packet, int timeout) {
        if (writeQueue.size() > TransportConstants.WRITEQUEUE_MAX_COUNT) {
            if (log.isWarnEnabled()) log.warn(new StringBuilder("writeQueue has ").append(writeQueue.size()).append(" waiting, return null").toString());
            return null;
        }
        writeQueue.offer(packet);
        try {
            if ((key != null) && ((SocketChannel) key.channel()).isConnected()) {
                this.key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                this.selector.wakeup();
            } else {
                if (key == null) {
                    log.info("key is null");
                } else {
                    if (log.isInfoEnabled()) log.info("key: " + key + " channel state: " + ((SocketChannel) key.channel()).isConnected());
                }
            }
        } catch (CancelledKeyException e) {
            close();
            log.error(e, e);
        }
        try {
            if ((channel == null) && (connectState != TransportConstants.CONN_STATUS_CONNECTED)) {
                if (log.isErrorEnabled()) log.error("channel=" + channel + ", connectState=" + connectState);
                return null;
            }
            waitThreadCount.incrementAndGet();
            synchronized (packet) {
                if (packet.getReturnPacket() == null) {
                    packet.wait(timeout);
                }
            }
            waitThreadCount.decrementAndGet();
            BasePacket retPacket = packet.getReturnPacket();
            if (retPacket == null) {
                log.error("retPacket is null");
            } else if (retPacket.getException() != null) {
                log.error(retPacket.getException(), retPacket.getException());
            }
            if (retPacket != null) retPacket.decode();
            return retPacket;
        } catch (Exception e) {
            log.error(e, e);
        }
        return null;
    }

    /**
	 * 异步发送一个BasePacket
	 */
    public boolean postPacket(BasePacket packet, Listener listener) {
        if (writeQueue.size() > TransportConstants.WRITEQUEUE_MAX_COUNT) {
            return false;
        }
        packet.setListener(listener);
        writeQueue.offer(packet);
        try {
            if ((key != null) && ((SocketChannel) key.channel()).isConnected()) {
                this.key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                this.selector.wakeup();
            }
            return true;
        } catch (CancelledKeyException e) {
            close();
            log.error(e, e);
        }
        return false;
    }

    public String toString() {
        if (address != null) return address.toString() + ", waiting reply size: " + waitReplyList.size();
        return "***NULL TCPConnection";
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    /**
	 * 检查分配的内存大小是否超过限定值
	 */
    public void checkMalloc(int size) {
        if (size > TransportConstants.MALLOC_MAX) {
            throw new IllegalArgumentException("alloc to large byte[], size: " + size);
        }
    }
}
