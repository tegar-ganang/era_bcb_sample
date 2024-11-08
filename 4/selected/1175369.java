package org.javaseis.io;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.logging.Logger;
import org.javaseis.util.SeisException;

/**
 * A VirtualMappedIO file is intended to be similar to the public API of VirtualIO
 * however this file does not use secondary storage.  There are files like the 
 * TraceMap and the files that define the definition of a sortmap that are stored
 * in primary and tend to be read a lot.  These files were originally written
 * using VirtualIO in order to support the parallel writing.  However the small
 * frame sizes, the fact that the file resides in primary and the need to read them
 * for each ensemble can cause very high loads on the NFS server.
 * 
 * This file still requires random IO and is generally to large to hold in memory
 * so we use a Memory Mapped file.  This allows Java NIO to do buffering on the reads 
 * which testing has shows can improve performance dramatically.
 * 
 * @author Steve Angelovich
 *
 */
public class VirtualMappedIO implements IVirtualIO {

    private static Logger LOG = Logger.getLogger("org.javaseis.io.virtualmappedio");

    private FileChannel _channel;

    private long _channelSize;

    private MappedByteBuffer _mappedByteBuffer;

    private int _currentSegment = 0;

    private int _segmentSize;

    private long _fpos;

    private boolean _trackTime = false;

    private long _ioTime;

    private String _path;

    private MapMode _mode;

    public VirtualMappedIO(String path) throws SeisException {
        this(path, Integer.MAX_VALUE);
    }

    public VirtualMappedIO(String path, int segmentSize) throws SeisException {
        assert (segmentSize > 1);
        _path = path;
        _segmentSize = segmentSize;
        try {
            openExisting(path);
        } catch (IOException e) {
            throw new SeisException("Unable to open VirtualMappedIO File '" + path + "'\n", e);
        }
    }

    public VirtualMappedIO(String path, int segmentSize, long flen) throws SeisException {
        assert (segmentSize > 1);
        _path = path;
        _segmentSize = segmentSize;
        try {
            openNew(path, flen);
        } catch (IOException e) {
            throw new SeisException("Unable to open VirtualMappedIO File '" + path + "'\n", e);
        }
    }

    private void openExisting(String path) throws IOException {
        _channel = new RandomAccessFile(path, "r").getChannel();
        _channelSize = _channel.size();
        _mode = FileChannel.MapMode.READ_ONLY;
        long len = Math.min((long) _segmentSize, _channelSize);
        _mappedByteBuffer = _channel.map(_mode, 0, (int) len);
    }

    private void openNew(String path, long flen) throws IOException {
        _channel = new RandomAccessFile(path, "rw").getChannel();
        _channelSize = flen;
        _mode = FileChannel.MapMode.READ_WRITE;
        long len = Math.min((long) _segmentSize, _channelSize);
        _mappedByteBuffer = _channel.map(_mode, 0, (int) len);
    }

    /**
   * Index to the correct position in the file.
   */
    public long setPosition(long fpos) throws SeisException {
        if (fpos < 0) throw new IllegalArgumentException("Invalid position was specified, fpos = " + fpos);
        long t0 = 0;
        if (_trackTime) t0 = System.nanoTime();
        _fpos = fpos;
        int index = (int) (fpos / _segmentSize);
        if (_currentSegment != index) {
            try {
                long offset = ((long) index * (long) _segmentSize);
                long len = Math.min(_channelSize - offset, (long) _segmentSize);
                _mappedByteBuffer = _channel.map(_mode, offset, len);
            } catch (IOException e) {
                throw new SeisException("Failed to create mapped byte buffer, index = " + index + "\n", e);
            }
            _currentSegment = index;
        }
        int ipos = getOffset(fpos);
        if (ipos < 0 || ipos > _segmentSize) throw new IllegalArgumentException("Integer position in map buffer is invalid" + "\nfpos = " + fpos + "\nindex = " + index);
        _mappedByteBuffer.position(ipos);
        if (_trackTime) _ioTime += System.nanoTime() - t0;
        return _fpos;
    }

    private int getOffset(long fpos) {
        int ioffset = (int) (fpos - (((long) _currentSegment) * (long) _segmentSize));
        return ioffset;
    }

    /**
   * Fills the buffer.  This assumes the buffer has been properly prepared
   * and the limit has been set indicating how much information to read.
   * @throws SeisException 
   */
    public int read(ByteBuffer buf) throws SeisException {
        long t0 = 0;
        if (_trackTime) t0 = System.nanoTime();
        int limit = buf.limit();
        int ipos = _mappedByteBuffer.position();
        int icap = _mappedByteBuffer.capacity();
        int left_remaining = icap - ipos;
        int right_remaining = limit - left_remaining;
        if (left_remaining < limit) {
            buf.limit(left_remaining);
            for (int i = 0; i < left_remaining; i++) buf.put(_mappedByteBuffer.get());
            setPosition(_fpos + (long) left_remaining);
            buf.limit(limit);
            for (int i = 0; i < limit - left_remaining - 1; i++) buf.put(_mappedByteBuffer.get());
        } else {
            for (int i = 0; i < limit; i++) buf.put(_mappedByteBuffer.get());
        }
        if (_trackTime) _ioTime += System.nanoTime() - t0;
        return buf.position();
    }

    @Override
    public int write(ByteBuffer buf) throws SeisException {
        long t0 = 0;
        if (_trackTime) t0 = System.nanoTime();
        int limit = buf.limit();
        int ipos = _mappedByteBuffer.position();
        int icap = _mappedByteBuffer.capacity();
        int left_remaining = icap - ipos;
        int right_remaining = limit - left_remaining;
        if (left_remaining < limit) {
            buf.limit(left_remaining);
            _mappedByteBuffer.put(buf);
            setPosition(_fpos + (long) left_remaining);
            buf.limit(limit);
            _mappedByteBuffer.put(buf);
        } else {
            _mappedByteBuffer.put(buf);
        }
        if (_trackTime) _ioTime += System.nanoTime() - t0;
        return buf.position();
    }

    public void close() throws SeisException {
        try {
            if (_channel != null) _channel.close();
        } catch (IOException e) {
            throw new SeisException("Error closing VirtualMappedIO file\n", e);
        }
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public void flush() throws SeisException {
    }

    @Override
    public ExtentPolicy getExtentPolicy() {
        return null;
    }

    @Override
    public FileDescriptor getFD() {
        return null;
    }

    @Override
    public String getPath() {
        return _path;
    }

    @Override
    public int readBuffer(ByteBuffer buffer) throws SeisException {
        return 0;
    }

    @Override
    public int readBuffer(ByteBuffer buffer, int len) throws SeisException {
        return 0;
    }

    @Override
    public int writeBuffer(ByteBuffer buffer) throws SeisException {
        return 0;
    }

    @Override
    public float getVirtualRate() {
        return 0;
    }

    @Override
    public float getVirtualTime() {
        return 0;
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public long getIoBytes() {
        return 0;
    }

    @Override
    public float getIoRate() {
        return 0;
    }

    @Override
    public float getLoadTime() {
        return 0;
    }

    @Override
    public float getLockTime() {
        return 0;
    }

    @Override
    public ExtentListEntry[] getExtents() {
        return null;
    }

    /**
   * Set flag that determines if I/O rate is tracked
   * @param flag true if I/O time is to be tracked
   */
    public void trackTime(boolean flag) {
        _trackTime = flag;
    }

    /**
   * Return current I/O time in seconds if I/O rate tracking is enabled
   * @return elapsed time spent in I/O routines in seconds, 0 if rate tracking is disabled
   */
    public float getIoTime() {
        return 1.e-9f * (float) _ioTime;
    }
}
