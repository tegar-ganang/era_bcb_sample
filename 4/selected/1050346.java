package org.mortbay.io.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import org.mortbay.io.Buffer;
import org.mortbay.io.EndPoint;
import org.mortbay.io.Portable;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ChannelEndPoint implements EndPoint {

    protected ByteChannel _channel;

    protected ByteBuffer[] _gather2 = new ByteBuffer[2];

    protected Socket _socket;

    protected InetSocketAddress _local;

    protected InetSocketAddress _remote;

    /**
     * 
     */
    public ChannelEndPoint(ByteChannel channel) {
        super();
        this._channel = channel;
        if (channel instanceof SocketChannel) _socket = ((SocketChannel) channel).socket();
    }

    public boolean isBlocking() {
        if (_channel instanceof SelectableChannel) return ((SelectableChannel) _channel).isBlocking();
        return true;
    }

    public boolean blockReadable(long millisecs) throws IOException {
        return true;
    }

    public boolean blockWritable(long millisecs) throws IOException {
        return true;
    }

    public boolean isOpen() {
        return _channel.isOpen();
    }

    public void close() throws IOException {
        if (_channel.isOpen()) {
            try {
                if (_channel instanceof SocketChannel) {
                    Socket socket = ((SocketChannel) _channel).socket();
                    try {
                        socket.shutdownOutput();
                    } finally {
                        socket.close();
                    }
                }
            } finally {
                _channel.close();
            }
        }
    }

    public int fill(Buffer buffer) throws IOException {
        Buffer buf = buffer.buffer();
        int len = 0;
        if (buf instanceof NIOBuffer) {
            NIOBuffer nbuf = (NIOBuffer) buf;
            ByteBuffer bbuf = nbuf.getByteBuffer();
            synchronized (nbuf) {
                try {
                    bbuf.position(buffer.putIndex());
                    len = _channel.read(bbuf);
                } finally {
                    buffer.setPutIndex(bbuf.position());
                    bbuf.position(0);
                }
            }
        } else {
            throw new IOException("Not Implemented");
        }
        return len;
    }

    public int flush(Buffer buffer) throws IOException {
        Buffer buf = buffer.buffer();
        int len = 0;
        if (buf instanceof NIOBuffer) {
            NIOBuffer nbuf = (NIOBuffer) buf;
            ByteBuffer bbuf = nbuf.getByteBuffer();
            synchronized (bbuf) {
                try {
                    bbuf.position(buffer.getIndex());
                    bbuf.limit(buffer.putIndex());
                    len = _channel.write(bbuf);
                } finally {
                    if (len > 0) buffer.skip(len);
                    bbuf.position(0);
                    bbuf.limit(bbuf.capacity());
                }
            }
        } else if (buffer.array() != null) {
            ByteBuffer b = ByteBuffer.wrap(buffer.array(), buffer.getIndex(), buffer.length());
            len = _channel.write(b);
            if (len > 0) buffer.skip(len);
        } else {
            throw new IOException("Not Implemented");
        }
        return len;
    }

    public int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException {
        int length = 0;
        Buffer buf0 = header == null ? null : header.buffer();
        Buffer buf1 = buffer == null ? null : buffer.buffer();
        if (_channel instanceof GatheringByteChannel && header != null && header.length() != 0 && header instanceof NIOBuffer && buffer != null && buffer.length() != 0 && buffer instanceof NIOBuffer) {
            NIOBuffer nbuf0 = (NIOBuffer) buf0;
            ByteBuffer bbuf0 = nbuf0.getByteBuffer();
            NIOBuffer nbuf1 = (NIOBuffer) buf1;
            ByteBuffer bbuf1 = nbuf1.getByteBuffer();
            synchronized (this) {
                synchronized (bbuf0) {
                    synchronized (bbuf1) {
                        try {
                            bbuf0.position(header.getIndex());
                            bbuf0.limit(header.putIndex());
                            bbuf1.position(buffer.getIndex());
                            bbuf1.limit(buffer.putIndex());
                            _gather2[0] = bbuf0;
                            _gather2[1] = bbuf1;
                            length = (int) ((GatheringByteChannel) _channel).write(_gather2);
                            int hl = header.length();
                            if (length > hl) {
                                header.clear();
                                buffer.skip(length - hl);
                            } else if (length > 0) {
                                header.skip(length);
                            }
                        } finally {
                            if (!header.isImmutable()) header.setGetIndex(bbuf0.position());
                            if (!buffer.isImmutable()) buffer.setGetIndex(bbuf1.position());
                            bbuf0.position(0);
                            bbuf1.position(0);
                            bbuf0.limit(bbuf0.capacity());
                            bbuf1.limit(bbuf1.capacity());
                        }
                    }
                }
            }
        } else {
            if (header != null && header.length() > 0) length = flush(header);
            if ((header == null || header.length() == 0) && buffer != null && buffer.length() > 0) length += flush(buffer);
            if ((header == null || header.length() == 0) && (buffer == null || buffer.length() == 0) && trailer != null && trailer.length() > 0) length += flush(trailer);
        }
        return length;
    }

    /**
     * @return Returns the channel.
     */
    public ByteChannel getChannel() {
        return _channel;
    }

    public String getLocalAddr() {
        if (_socket == null) return null;
        if (_local == null) _local = (InetSocketAddress) _socket.getLocalSocketAddress();
        if (_local == null || _local.getAddress() == null || _local.getAddress().isAnyLocalAddress()) return Portable.ALL_INTERFACES;
        return _local.getAddress().getHostAddress();
    }

    public String getLocalHost() {
        if (_socket == null) return null;
        if (_local == null) _local = (InetSocketAddress) _socket.getLocalSocketAddress();
        if (_local == null || _local.getAddress() == null || _local.getAddress().isAnyLocalAddress()) return Portable.ALL_INTERFACES;
        return _local.getAddress().getCanonicalHostName();
    }

    public int getLocalPort() {
        if (_socket == null) return 0;
        if (_local == null) _local = (InetSocketAddress) _socket.getLocalSocketAddress();
        if (_local == null) return -1;
        return _local.getPort();
    }

    public String getRemoteAddr() {
        if (_socket == null) return null;
        if (_remote == null) _remote = (InetSocketAddress) _socket.getRemoteSocketAddress();
        if (_remote == null) return null;
        return _remote.getAddress().getHostAddress();
    }

    public String getRemoteHost() {
        if (_socket == null) return null;
        if (_remote == null) _remote = (InetSocketAddress) _socket.getRemoteSocketAddress();
        return _remote.getAddress().getCanonicalHostName();
    }

    public int getRemotePort() {
        if (_socket == null) return 0;
        if (_remote == null) _remote = (InetSocketAddress) _socket.getRemoteSocketAddress();
        return _remote == null ? -1 : _remote.getPort();
    }

    public Object getTransport() {
        return _channel;
    }

    public void flush() throws IOException {
    }

    public boolean isBufferingInput() {
        return false;
    }

    public boolean isBufferingOutput() {
        return false;
    }

    public boolean isBufferred() {
        return false;
    }
}
