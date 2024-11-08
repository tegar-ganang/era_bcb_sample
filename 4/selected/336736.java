package net.community.chest.net;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import net.community.chest.io.FileUtil;
import net.community.chest.lang.StringUtil;

/**
 * Copyright 2007 as per GPLv2
 * @author Lyor G.
 * @since Jul 4, 2007 9:21:27 AM
 */
public class TextSocket extends AbstractTextNetConnection {

    /**
	 * Actual socket channel through which the connection is managed
	 */
    private SocketChannel _channel;

    @Override
    public SocketChannel getChannel() {
        return _channel;
    }

    public void setChannel(SocketChannel channel) {
        _channel = channel;
    }

    public TextSocket() {
        super();
    }

    public TextSocket(SocketChannel channel) throws IOException {
        attach(channel);
    }

    @Override
    public boolean isOpen() {
        final SocketChannel channel = getChannel();
        return (channel != null) && channel.isConnected();
    }

    @Override
    public SocketChannel detachChannel() throws IOException {
        final SocketChannel retChannel = getChannel();
        setChannel(null);
        return retChannel;
    }

    /**
	 * Remote host name to which currently connected
	 * @see #getRemoteHostName()
	 */
    private String _remoteHost;

    /**
	 * Remote address to which currently connected
	 * @see #getRemoteAddress()
	 */
    private String _remoteAddr;

    /**
	 * Remote port to which currently connected
	 * @see #getRemotePort()
	 */
    private int _remotePort;

    private static final String resolveRemoteString(String orgVal, String defVal) {
        if ((null == orgVal) || (orgVal.length() <= 0)) return defVal; else return orgVal;
    }

    /**
	 * Updates the internally cached remote connection details if not already
	 * done so. We do this since some calls may take a long time if DNS is not
	 * set up properly - so don't do it automatically until actually requested
	 * (which may be never...)
	 */
    private void updateRemoteDetails() {
        if ((null == _remoteHost) || (null == _remoteAddr) || (_remotePort <= 0)) {
            if (isOpen()) {
                final SocketChannel channel = getChannel();
                final Socket sock = (null == channel) ? null : channel.socket();
                final InetSocketAddress sockAddress = (null == sock) ? null : (InetSocketAddress) sock.getRemoteSocketAddress();
                if (null == _remoteAddr) _remoteAddr = resolveRemoteString(NetUtil.getRemoteHostAddress(sockAddress), "?.?.?.?");
                if (null == _remoteHost) _remoteHost = resolveRemoteString(sockAddress.getHostName(), "???");
                if (_remotePort <= 0) {
                    if ((_remotePort = sockAddress.getPort()) <= 0) _remotePort = Integer.MAX_VALUE;
                }
            }
        }
    }

    @Override
    public String getRemoteHostName() {
        if (null == _remoteHost) updateRemoteDetails();
        return _remoteHost;
    }

    @Override
    public String getRemoteAddress() {
        if (null == _remoteAddr) updateRemoteDetails();
        return _remoteAddr;
    }

    @Override
    public int getRemotePort() {
        if (_remotePort <= 0) updateRemoteDetails();
        return _remotePort;
    }

    /**
	 * local port to which the connection is bound (or <=0 if error/undefined)
	 * @see #getLocalPort()
	 */
    private int _localPort;

    @Override
    public int getLocalPort() {
        return _localPort;
    }

    /**
	 * Current read timeout value (msec.)
	 * @see #setReadTimeout(int timeMillis)
	 * @see #getReadTimeout()
	 */
    private int _readTimeout;

    @Override
    public int getReadTimeout() {
        return _readTimeout;
    }

    @Override
    public void setReadTimeout(final int timeMillis) throws IOException {
        final SocketChannel channel = getChannel();
        if (channel != null) NetUtil.setReadTimeout(channel, timeMillis); else if (timeMillis < 0) throw new SocketException(getClass().getName() + "#setReadTimeout(" + timeMillis + ") bad value");
        _readTimeout = timeMillis;
    }

    @Override
    public void attach(final SocketChannel channel) throws IOException {
        if (null == channel) throw new SocketException("No " + SocketChannel.class.getName() + " instance to attach");
        if (!channel.isConnected()) throw new EOFException(SocketChannel.class.getName() + " instance to attach is not connected");
        final Socket sock = channel.socket();
        NetUtil.setReadTimeout(sock, getReadTimeout());
        sock.setSoLinger(true, 0);
        if (!channel.isBlocking()) channel.configureBlocking(true);
        _channel = channel;
        _localPort = sock.getLocalPort();
    }

    @Override
    public void connect(final String host, final int port) throws IOException {
        if (isOpen()) throw new ConnectException("connect(" + host + ":" + port + ") already connected to " + getRemoteAddress() + " on port=" + getRemotePort());
        SocketChannel connChannel = NetUtil.openSocketChannel(host, port);
        try {
            attach(connChannel);
            connChannel = null;
        } finally {
            FileUtil.closeAll(connChannel);
        }
        _remoteHost = host;
        _remotePort = port;
    }

    /**
	 * Cached {@link InputStream} from underlying {@link Socket}. Initialized
	 * by first call to {@link #getInputStream()} unless {@link #setInputStream(InputStream)}
	 * called prior to this
	 */
    private InputStream _in;

    protected void setInputStream(InputStream in) {
        _in = in;
    }

    protected InputStream getInputStream() throws IOException {
        if (null == _in) {
            final SocketChannel channel = getChannel();
            if (null == channel) throw new SocketException("No current channel to retrieve " + InputStream.class.getName() + " instance from");
            final Socket s = channel.socket();
            if (null == s) throw new SocketException("No current socket to retrieve " + InputStream.class.getName() + " instance from");
            if (null == (_in = s.getInputStream())) throw new StreamCorruptedException("No input stream from socket");
        }
        return _in;
    }

    @Override
    public int readBytes(byte[] buf, int offset, int len) throws IOException {
        if (!isOpen()) throw new EOFException("No valid connection to read bytes from text socket");
        if (0 == len) return len;
        if ((null == buf) || (offset < 0) || (len < 0) || ((offset + len) > buf.length)) throw new IOException("Bad/Illegal bytes buffer and/or start offset/max length");
        final InputStream in = getInputStream();
        final int nRead = in.read(buf, offset, len);
        if (nRead <= 0) throw new StreamCorruptedException("Invalid number of characters (" + nRead + ") read in buf[" + offset + "-" + (offset + len) + "]");
        return nRead;
    }

    /**
	 * Default internal buffer size used for reading (ASCII) characters
	 */
    public static final int DEFAULT_CACHEBUF_SIZE = 1480;

    /**
	 * @return internal buffer size used for reading/writing (ASCII) characters.
	 * Must be greater than {@link Byte#MAX_VALUE}
	 */
    public int getCacheBufSize() {
        return DEFAULT_CACHEBUF_SIZE;
    }

    /**
	 * Cached buffer for reading bytes and converting them to characters.
	 * Initialized by first call to {@link #getCacheArray()}
	 */
    private byte[] _cacheArray;

    /**
	 * @return byte buffer to be used for reading characters and turning them
	 * into (ASCII) characters - may NOT be null and its <I>length</I> must be
	 * greater than {@link Byte#MAX_VALUE}
	 * @throws IOException if cannot allocate the buffer
	 */
    protected byte[] getCacheArray() throws IOException {
        if (null == _cacheArray) {
            final int readBufSize = getCacheBufSize();
            if (readBufSize <= Byte.MAX_VALUE) throw new SocketException("Internal buffer size (" + readBufSize + ") too small");
            _cacheArray = new byte[readBufSize];
        }
        return _cacheArray;
    }

    @Override
    public int read(final char[] buf, final int offset, final int len) throws IOException {
        if (0 == len) {
            if (!isOpen()) throw new EOFException("read(chars)[len=0] no current connection");
            return len;
        }
        final byte[] rb = getCacheArray();
        if ((null == rb) || (rb.length <= Byte.MAX_VALUE)) throw new SocketException("read(chars)[" + offset + " - " + (offset + len) + ") bad internal read buffer");
        final int readLen = Math.min(len, rb.length), nRead = readBytes(rb, 0, readLen);
        return StringUtil.toASCIIChars(rb, 0, nRead, buf, offset);
    }

    /**
	 * Cached {@link LineInfo} object - initialized by first call to {@link #getLineInfo()} 
	 */
    private LineInfo _li;

    /**
	 * @return an internal {@link LineInfo} object - allocates if none
	 * available (may NOT be null)
	 */
    protected LineInfo getLineInfo() {
        if (null == _li) _li = new LineInfo(); else _li.reset();
        return _li;
    }

    @Override
    public String readLine() throws IOException {
        if (!isOpen()) throw new EOFException("readLine() no current connection");
        final byte[] rb = getCacheArray();
        if ((null == rb) || (rb.length <= Byte.MAX_VALUE)) throw new SocketException("readLine() bad internal read buffer");
        final LineInfo li = getLineInfo();
        final int rLen = readBinaryLine(rb, li);
        if (rLen <= 0) throw new StreamCorruptedException("readLine() - bad binary line read len: " + rLen);
        if (!li.isLFDetected()) throw new StreamCorruptedException("readLine() - incomplete line after " + rLen + " bytes");
        final int ll = li.getLength();
        if (ll <= 0) return "";
        return new String(rb, 0, ll, "US-ASCII");
    }

    @Override
    public int available() throws IOException {
        if (!isOpen()) throw new EOFException("No current valid socket channel to get available data from");
        return 0;
    }

    @Override
    public long skip(long skipSize) throws IOException {
        if (skipSize <= 0L) return skipSize;
        final byte[] rb = getCacheArray();
        if ((null == rb) || (rb.length <= Byte.MAX_VALUE)) throw new SocketException("skip(" + skipSize + ") bad internal read buffer");
        for (long dataToSkip = skipSize; dataToSkip > 0L; ) {
            final int readLen = (int) Math.min(dataToSkip, rb.length), nRead = readBytes(rb, 0, readLen);
            if (nRead <= 0) throw new EOFException("Invalid number of characters skipped from text socket: " + nRead);
            dataToSkip -= nRead;
        }
        return skipSize;
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        if (null == dst) throw new IOException("No " + ByteBuffer.class.getName() + " instance to read into");
        if (!isOpen()) throw new EOFException("No channel to read " + ByteBuffer.class.getName() + " data");
        return getChannel().read(dst);
    }

    @Override
    public int read(CharBuffer cb) throws IOException {
        return Integer.MIN_VALUE;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (!isOpen()) throw new EOFException("No current channel to write bytes to");
        if (null == src) return 0;
        return getChannel().write(src);
    }

    @Override
    public int writeBytes(byte[] buf, int startPos, int maxLen, boolean flushIt) throws IOException {
        if (maxLen > 0) {
            if ((null == buf) || (startPos < 0) || (maxLen < 0) || ((startPos + maxLen) > buf.length)) throw new StreamCorruptedException("Bad/Illegal write bytes buffer parameters");
            final int nWritten = write(ByteBuffer.wrap(buf, startPos, maxLen));
            if (nWritten != maxLen) throw new StreamCorruptedException("Write bytes mismatch (" + nWritten + " <> " + maxLen + ")");
        } else {
            if (!isOpen()) throw new EOFException("No current channel to write (ZERO) data into");
        }
        if (flushIt) flush();
        return maxLen;
    }

    @Override
    public void flush() throws IOException {
        if (!isOpen()) throw new EOFException("No current valid socket channel to flush from");
    }

    @Override
    public int write(char[] buf, int startOffset, int maxLen, boolean flushIt) throws IOException {
        if (!isOpen()) throw new EOFException("No current channel to write to");
        if (maxLen < 0) throw new IOException("Bad/Illegal write max. len=" + maxLen);
        if (maxLen > 0) {
            final byte[] wa = getCacheArray();
            if ((null == wa) || (wa.length <= Byte.MAX_VALUE)) throw new SocketException("write(chars)[" + startOffset + " - " + (startOffset + maxLen) + ") bad internal read buffer");
            for (int writeLen = 0; writeLen < maxLen; ) {
                final int remLen = Math.min((maxLen - writeLen), wa.length), toWrite = StringUtil.toASCIIBytes(buf, writeLen, remLen, wa, 0);
                final int nWritten = writeBytes(wa, 0, toWrite, false);
                if (nWritten != toWrite) throw new IOException("Partial write mismatch (" + nWritten + " <> " + toWrite + ")");
                writeLen += nWritten;
            }
        }
        if (flushIt) flush();
        return maxLen;
    }

    @Override
    public void close() throws IOException {
        IOException exc = null;
        try {
            super.close();
        } catch (IOException ioe) {
            exc = ioe;
        } finally {
            if (_channel != null) _channel = null;
        }
        if (_in != null) {
            try {
                _in.close();
            } catch (IOException ioe) {
                if (null == exc) exc = ioe;
            } finally {
                _in = null;
            }
        }
        if (_remoteAddr != null) _remoteAddr = null;
        if (_remoteHost != null) _remoteHost = null;
        if (_remotePort > 0) _remotePort = 0;
        if (_localPort > 0) _localPort = 0;
        if (_cacheArray != null) _cacheArray = null;
        if (exc != null) throw exc;
    }
}
