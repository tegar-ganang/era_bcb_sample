package xbird.storage.io;

import java.io.Externalizable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.DefaultFileRegion;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import xbird.server.services.RemotePagingService;
import xbird.server.services.RemotePagingService.RequestMessage;
import xbird.storage.io.VarSegments.IDescriptor;
import xbird.util.io.IOUtils;
import xbird.util.net.NetUtils;
import xbird.util.net.PoolableSocketChannelFactory;
import xbird.util.nio.NIOUtils;
import xbird.util.pool.ConcurrentKeyedStackObjectPool;
import xbird.util.pool.ObjectPool;
import xbird.util.pool.StackObjectPool;
import xbird.util.primitive.Primitives;
import xbird.util.string.StringUtils;

/**
 * 
 * <DIV lang="en"></DIV>
 * <DIV lang="ja"></DIV>
 * 
 * @author Makoto YUI (yuin405+xbird@gmail.com)
 */
public final class RemoteVarSegments implements Segments, Externalizable {

    private static final long serialVersionUID = -2804462847438664966L;

    private static final Log LOG = LogFactory.getLog(RemoteVarSegments.class);

    private static final int DEFAULT_BUF_CAPACITY = 2 * 1024 * 1024;

    private InetSocketAddress _sockAddr;

    private String _filePath;

    private ObjectPool<ByteBuffer> _rbufPool;

    private static final ConcurrentKeyedStackObjectPool<SocketAddress, ByteChannel> _sockPool;

    static {
        boolean datagram = "NIODATAGRAM".equals(RemotePagingService.CONN_TYPE);
        PoolableSocketChannelFactory factory = new PoolableSocketChannelFactory(datagram, true);
        factory.configure(RemotePagingService.SWEEP_INTERVAL, RemotePagingService.TIME_TO_LIVE, RemotePagingService.SO_RCVBUF_SIZE);
        _sockPool = new ConcurrentKeyedStackObjectPool<SocketAddress, ByteChannel>("RemoteVarSegments", factory);
    }

    public RemoteVarSegments() {
        this._rbufPool = createPool();
    }

    public RemoteVarSegments(int port, String filePath, boolean alloc) {
        this._sockAddr = new InetSocketAddress(NetUtils.getLocalHost(), port);
        this._filePath = filePath;
        this._rbufPool = alloc ? createPool() : null;
    }

    private static ObjectPool<ByteBuffer> createPool() {
        return new StackObjectPool<ByteBuffer>(1) {

            protected ByteBuffer createObject() {
                return ByteBuffer.allocate(DEFAULT_BUF_CAPACITY);
            }
        };
    }

    public byte[] read(long idx) throws IOException {
        final ByteChannel channel;
        try {
            channel = openConnection(_sockAddr);
        } catch (IOException ioe) {
            throw new IllegalStateException("failed opening a socket", ioe);
        }
        final byte[] dst;
        final ByteBuffer rbuf = _rbufPool.borrowObject();
        try {
            sendRequest(channel, idx);
            dst = recvResponse(channel, rbuf);
        } catch (IOException ioe) {
            IOUtils.closeQuietly(channel);
            throw new IllegalStateException(ioe);
        } catch (Throwable e) {
            IOUtils.closeQuietly(channel);
            throw new IllegalStateException(e);
        } finally {
            _rbufPool.returnObject(rbuf);
            _sockPool.returnObject(_sockAddr, channel);
        }
        return dst;
    }

    public byte[][] readv(final long[] idxs) throws IOException {
        final ByteChannel channel;
        try {
            channel = openConnection(_sockAddr);
        } catch (IOException ioe) {
            throw new IllegalStateException("failed opening a socket", ioe);
        }
        final int size = idxs.length;
        final byte[][] dst = new byte[size][];
        final ByteBuffer rbuf = _rbufPool.borrowObject();
        try {
            sendRequest(channel, idxs);
            for (int i = 0; i < size; i++) {
                dst[i] = recvResponse(channel, rbuf);
            }
        } catch (IOException ioe) {
            IOUtils.closeQuietly(channel);
            throw new IllegalStateException(ioe);
        } catch (Throwable e) {
            IOUtils.closeQuietly(channel);
            throw new IllegalStateException(e);
        } finally {
            _rbufPool.returnObject(rbuf);
            _sockPool.returnObject(_sockAddr, channel);
        }
        return dst;
    }

    private static ByteChannel openConnection(final SocketAddress sockAddr) throws IOException {
        return _sockPool.borrowObject(sockAddr);
    }

    private void sendRequest(final ByteChannel channel, final long... idxs) throws IOException {
        byte[] bFilePath = StringUtils.getBytes(_filePath);
        final int size = idxs.length;
        int buflen = bFilePath.length + 16 + (Primitives.LONG_BYTES * size);
        if (buflen > RemotePagingService.MAX_COMMAND_BUFLEN) {
            throw new IllegalStateException("command size exceeds limit in MAX_COMMAND_BUFLEN(" + RemotePagingService.MAX_COMMAND_BUFLEN + "): " + buflen + " bytes");
        }
        ByteBuffer oprBuf = ByteBuffer.allocate(buflen);
        oprBuf.putInt(buflen - 4);
        oprBuf.putInt(RemotePagingService.COMMAND_TRACK_READ);
        oprBuf.putInt(bFilePath.length);
        oprBuf.put(bFilePath);
        oprBuf.putInt(size);
        for (int i = 0; i < size; i++) {
            oprBuf.putLong(idxs[i]);
        }
        oprBuf.flip();
        NIOUtils.writeFully(channel, oprBuf);
    }

    public static final class TrackReadRequestMessage extends RequestMessage {

        final String filePath;

        final long[] idxs;

        private TrackReadRequestMessage(IoBuffer in) {
            super(RemotePagingService.COMMAND_TRACK_READ);
            int filePathLen = in.getInt();
            byte[] bFilePath = new byte[filePathLen];
            in.get(bFilePath);
            this.filePath = StringUtils.toString(bFilePath, 0, filePathLen);
            final int size = in.getInt();
            long[] idxs = new long[size];
            for (int i = 0; i < size; i++) {
                idxs[i] = in.getLong();
            }
            this.idxs = idxs;
        }

        public static TrackReadRequestMessage decode(IoBuffer in) {
            return new TrackReadRequestMessage(in);
        }
    }

    public static void handleResponse(final TrackReadRequestMessage request, final ProtocolEncoderOutput out, final ConcurrentMap<String, FileChannel> fdCacheMap, final ConcurrentMap<String, IDescriptor> directoryCache) throws IOException {
        final String filePath = request.filePath;
        final long[] idxs = request.idxs;
        final int size = idxs.length;
        final File dataFile = new File(filePath);
        final long[] offsets = new long[size];
        IDescriptor directory = directoryCache.get(filePath);
        try {
            if (directory == null) {
                directory = VarSegments.initDescriptor(dataFile);
                directoryCache.put(filePath, directory);
            }
            for (int i = 0; i < size; i++) {
                offsets[i] = directory.getRecordAddr(idxs[i]);
            }
        } catch (IOException e) {
            LOG.error(e);
            throw e;
        }
        FileChannel fileChannel = fdCacheMap.get(filePath);
        if (fileChannel == null) {
            if (!dataFile.exists()) {
                throw new IllegalStateException("file not exists: " + filePath);
            }
            final RandomAccessFile raf;
            try {
                raf = new RandomAccessFile(dataFile, "r");
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(e);
            }
            fileChannel = raf.getChannel();
            fdCacheMap.put(filePath, fileChannel);
        }
        for (int i = 0; i < size; i++) {
            final long offset = offsets[i];
            final ByteBuffer tmpBuf = ByteBuffer.allocate(4);
            try {
                fileChannel.read(tmpBuf, offset);
            } catch (IOException e) {
                LOG.error(e);
                throw e;
            }
            tmpBuf.flip();
            final int length = tmpBuf.getInt();
            tmpBuf.rewind();
            IoBuffer ioBuf = IoBuffer.wrap(tmpBuf);
            out.write(ioBuf);
            long position = offset + 4;
            long count = length;
            FileRegion fileRegion = new DefaultFileRegion(fileChannel, position, count);
            out.write(fileRegion);
        }
    }

    private byte[] recvResponse(final ByteChannel channel, final ByteBuffer rcvBuf) throws IOException {
        ByteBuffer tmpBuf = ByteBuffer.allocate(4);
        NIOUtils.readFully(channel, tmpBuf, 4);
        tmpBuf.flip();
        int datalen = tmpBuf.getInt();
        final ByteBuffer buf = truncateBuffer(rcvBuf, datalen);
        NIOUtils.readFully(channel, buf, datalen);
        buf.flip();
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            buf.order(ByteOrder.BIG_ENDIAN);
        }
        final byte[] b = new byte[datalen];
        buf.get(b);
        if (buf != rcvBuf) {
            _rbufPool.returnObject(buf);
        }
        return b;
    }

    public void close() throws IOException {
        this._rbufPool = null;
    }

    public File getFile() {
        throw new UnsupportedOperationException();
    }

    public long write(long idx, byte[] b) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void flush(boolean close) throws IOException {
        throw new UnsupportedOperationException();
    }

    private ByteBuffer truncateBuffer(final ByteBuffer buf, final int size) {
        if (size > buf.capacity()) {
            _rbufPool.returnObject(buf);
            return ByteBuffer.allocate(size);
        } else {
            buf.clear();
            buf.limit(size);
            return buf;
        }
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        String host = IOUtils.readString(in);
        int port = in.readInt();
        this._sockAddr = new InetSocketAddress(host, port);
        this._filePath = IOUtils.readString(in);
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        IOUtils.writeString(_sockAddr.getHostName(), out);
        out.writeInt(_sockAddr.getPort());
        IOUtils.writeString(_filePath, out);
        close();
    }

    public static RemoteVarSegments read(ObjectInput in) throws IOException, ClassNotFoundException {
        RemoteVarSegments seg = new RemoteVarSegments();
        seg.readExternal(in);
        return seg;
    }
}
