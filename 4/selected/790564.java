package org.javaseis.io;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;

public class JSRandomAccessFile {

    private RandomAccessFile _data_stream = null;

    private FileChannel _data_channel = null;

    private int _block_count = 0;

    private boolean _initialized = false;

    private ByteBuffer _buf = null;

    private int _num_buffers = 0;

    private int _limit = 0;

    private int[] _limits = null;

    private int[] _positions = null;

    private int[] _alt_limits = null;

    private static final int DEFAULT_BLOCK_COUNT = 1024;

    private static final int BLOCK_SIZE = 512;

    public JSRandomAccessFile(String name, String mode) throws FileNotFoundException, IOException {
        this(name, mode, getBlockCount());
    }

    public JSRandomAccessFile(String name, String mode, int block_count) throws FileNotFoundException, IOException {
        _block_count = block_count;
        _data_stream = new RandomAccessFile(name, mode);
        JSUtilFile.setODirect(_data_stream.getFD(), name, 1);
        _data_channel = _data_stream.getChannel();
    }

    private static int getBlockCount() {
        int retval = DEFAULT_BLOCK_COUNT;
        try {
            String js_block_count = System.getenv("JS_BLOCK_COUNT");
            if (js_block_count != null) {
                retval = Integer.parseInt(js_block_count);
            }
        } catch (Exception e) {
        }
        return retval;
    }

    public int read(ByteBuffer buf) throws IOException {
        int retval = 0;
        int limit = buf.limit();
        initialize(buf);
        for (int k2 = 0; k2 < _num_buffers; k2++) {
            if (_limits[k2] < limit) {
                buf.limit(_limits[k2]);
            } else {
                buf.limit(limit);
                k2 = _num_buffers;
            }
            retval += _data_channel.read(buf);
        }
        buf.limit(limit);
        return retval;
    }

    public int limitedRead(ByteBuffer buf, int len) throws IOException {
        int retval = 0;
        int limit = buf.limit();
        if (limit(buf, len) == len) {
            for (int k2 = 0; k2 < _num_buffers; k2++) {
                if (_alt_limits[k2] < limit) {
                    buf.limit(_alt_limits[k2]);
                } else {
                    buf.limit(limit);
                }
                retval += _data_channel.read(buf);
            }
        }
        buf.limit(limit);
        return retval;
    }

    public int write(ByteBuffer buf) throws IOException {
        int retval = 0;
        int limit = buf.limit();
        initialize(buf);
        for (int k2 = 0; k2 < _num_buffers; k2++) {
            if (_limits[k2] < limit) {
                buf.limit(_limits[k2]);
            } else {
                buf.limit(limit);
            }
            retval += _data_channel.write(buf);
        }
        buf.limit(limit);
        return retval;
    }

    public void position(long newposition) throws IOException {
        if (_data_channel != null) {
            _data_channel.position(newposition);
        }
    }

    public void flush() throws IOException {
        try {
            _data_channel.force(true);
        } catch (ClosedChannelException e) {
            throw new IOException("JSRandomAccessFile.flush: channel is closed");
        }
    }

    public void close() throws IOException {
        flush();
        _data_channel.close();
        _data_channel = null;
        _data_stream.close();
        _data_stream = null;
    }

    public FileDescriptor getFD() throws IOException {
        FileDescriptor retval = _data_stream.getFD();
        return retval;
    }

    private void initialize(ByteBuffer buf) throws IOException {
        if (!initialized(buf)) {
            _initialized = false;
            if (_data_stream != null && buf != null && _block_count > 0) {
                int limit = buf.limit();
                int position = buf.position();
                int length = buf.remaining();
                if (length > 0) {
                    int blength = _block_count * BLOCK_SIZE;
                    double d_num_buffers = (double) length / (double) blength;
                    int num_buffers = (int) Math.ceil(d_num_buffers);
                    _num_buffers = num_buffers;
                    _limits = new int[num_buffers];
                    _positions = new int[num_buffers];
                    int last_length = length - (num_buffers - 1) * blength;
                    int offset = position;
                    for (int k2 = 0; k2 < num_buffers - 1; k2++) {
                        _positions[k2] = offset;
                        offset += blength;
                        _limits[k2] = offset;
                    }
                    _positions[num_buffers - 1] = offset;
                    _limits[num_buffers - 1] = offset + last_length;
                    if (_limits[num_buffers - 1] != limit) {
                        throw new IOException("JSRandomAccessFile.initialize: unexpected limit");
                    }
                    _initialized = true;
                    _alt_limits = null;
                    _buf = buf;
                }
            } else {
                throw new IOException("JSRandomAccessFile.initialize: data stream or buffer bad");
            }
        }
    }

    private boolean initialized(ByteBuffer buf) {
        boolean retval = _data_stream != null && _initialized && _buf == buf && buf != null && _limits != null && _num_buffers > 0;
        if (retval) {
            retval = _limits[_num_buffers - 1] >= _buf.limit();
        }
        return retval;
    }

    private int limit(ByteBuffer buf, int newlimit) throws IOException {
        int retval = 0;
        initialize(buf);
        if (_num_buffers > 0 && newlimit < buf.limit()) {
            if (_limit != newlimit || _alt_limits == null) {
                int limit;
                int next_len;
                boolean finished = false;
                _alt_limits = new int[_limits.length];
                for (int k2 = 0; k2 < _limits.length; k2++) {
                    if (!finished) {
                        limit = newlimit - retval;
                        next_len = _limits[k2] - _positions[k2];
                        if (limit < next_len) {
                            finished = true;
                            if (limit > 0) {
                                _alt_limits[k2] = limit;
                            }
                            retval += limit;
                            _limit = retval;
                        } else {
                            _alt_limits[k2] = next_len;
                            retval += next_len;
                        }
                    } else {
                        _alt_limits[k2] = _positions[k2];
                    }
                }
            } else {
                retval = newlimit;
            }
        } else {
            retval = newlimit;
        }
        return retval;
    }
}
