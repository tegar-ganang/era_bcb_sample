package com.xixibase.cache;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class TCPSocket implements XixiSocket {

    private CacheClientManager manager;

    private String host;

    private Socket socket;

    private java.nio.channels.SocketChannel socketChannel;

    private ByteBuffer readBuffer;

    private ByteBuffer writeBuffer;

    private long lastActiveTime;

    public TCPSocket(CacheClientManager manager, String host, int writeBufferSize, int timeout, int connectTimeout, boolean noDelay) throws IOException, UnknownHostException {
        this.manager = manager;
        this.host = host;
        String[] ip = host.split(":");
        if (ip.length >= 2) {
            socket = createSocket(ip[0].trim(), Integer.parseInt(ip[1].trim()), connectTimeout);
        } else {
            socket = createSocket(ip[0].trim(), manager.getDefaultPort(), connectTimeout);
        }
        readBuffer = ByteBuffer.allocateDirect(8 * 1024);
        readBuffer.flip();
        writeBuffer = ByteBuffer.allocateDirect(writeBufferSize);
        if (timeout >= 0) {
            socket.setSoTimeout(timeout);
        }
        socket.setTcpNoDelay(noDelay);
        socketChannel = socket.getChannel();
        if (socketChannel == null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
            throw new IOException("Can not getChannel for host:" + host);
        }
    }

    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }

    public final SocketChannel getChannel() {
        return socketChannel;
    }

    public final String getHost() {
        return this.host;
    }

    public final boolean trueClose() {
        readBuffer.clear();
        writeBuffer.clear();
        boolean ret = true;
        try {
            socketChannel.close();
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException e2) {
            }
            ret = false;
        }
        socketChannel = null;
        try {
            socket.close();
        } catch (IOException e) {
            ret = false;
        }
        socket = null;
        return ret;
    }

    public final void close() {
        readBuffer.clear();
        readBuffer.flip();
        writeBuffer.clear();
        if (!manager.addSocket(this)) {
            trueClose();
        }
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        int remain = writeBuffer.remaining();
        if (len <= remain) {
            writeBuffer.put(b, off, len);
        } else {
            while (len > 0) {
                if (remain == 0) {
                    writeToChannel();
                    remain = writeBuffer.remaining();
                    if (remain == 0) {
                        throw new IOException("TCPSocket.write failed on write, len=" + len);
                    }
                }
                int w = len < remain ? len : remain;
                writeBuffer.put(b, off, w);
                off += w;
                len -= w;
                remain = writeBuffer.remaining();
            }
        }
    }

    private final void readFromChannel() throws IOException {
        readBuffer.clear();
        for (int i = 0; i < 100; i++) {
            if (socketChannel.read(readBuffer) != 0) {
                break;
            }
        }
        readBuffer.flip();
    }

    private final void writeToChannel() throws IOException {
        writeBuffer.flip();
        while (writeBuffer.hasRemaining()) {
            socketChannel.write(writeBuffer);
        }
        writeBuffer.clear();
    }

    public void flush() throws IOException {
        writeToChannel();
    }

    public byte readByte() throws IOException {
        byte[] b = new byte[1];
        read(b, 0, 1);
        return b[0];
    }

    public short readShort() throws IOException {
        byte[] b = new byte[2];
        read(b, 0, 2);
        return ObjectTransCoder.decodeShort(b);
    }

    public int readInt() throws IOException {
        byte[] b = new byte[4];
        read(b, 0, 4);
        return ObjectTransCoder.decodeInt(b);
    }

    public long readLong() throws IOException {
        byte[] b = new byte[8];
        read(b, 0, 8);
        return ObjectTransCoder.decodeLong(b);
    }

    public byte[] read(int len) throws IOException {
        byte[] b = new byte[len];
        read(b, 0, len);
        return b;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int remain = len;
        while (remain > 0) {
            int r1 = readBuffer.remaining();
            if (r1 == 0) {
                readFromChannel();
                r1 = readBuffer.remaining();
                if (r1 == 0) {
                    throw new IOException("TCPSocket.read failed on read, len=" + len);
                }
            }
            r1 = r1 < remain ? r1 : remain;
            readBuffer.get(b, off, r1);
            off += r1;
            remain -= r1;
        }
        return len;
    }

    protected static final Socket createSocket(String host, int port, int timeout) throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.socket().connect(new InetSocketAddress(host, port), timeout);
        return sc.socket();
    }

    public ByteChannel getByteChannel() {
        return socketChannel;
    }

    public SelectableChannel getSelectableChannel() {
        return socketChannel;
    }

    private AsyncHandle handle;

    public boolean isBlocking() {
        return socketChannel.isBlocking();
    }

    public void configureBlocking(boolean block) throws IOException {
        socketChannel.configureBlocking(block);
        if (block) {
            handle = null;
        }
    }

    public int read(ByteBuffer dst) throws IOException {
        return socketChannel.read(dst);
    }

    public int write(ByteBuffer src) throws IOException {
        return socketChannel.write(src);
    }

    public boolean handleRead() throws IOException {
        return handle.onRead();
    }

    public boolean handleWrite() throws IOException {
        return handle.onWrite();
    }

    public void register(Selector sel, int ops, AsyncHandle handle) throws IOException {
        this.handle = handle;
        socketChannel.register(sel, ops, this);
    }
}
