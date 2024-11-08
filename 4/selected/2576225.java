package de.psychomatic.applicationtools.io;

import java.io.IOException;
import java.io.OutputStream;

@Deprecated
public class ThreadedOutputStream extends OutputStream {

    static final int BLOCKSIZE = 256;

    private final RingBuffer _buffer;

    private final OutputStream _out;

    IOException _exception;

    boolean _close;

    public ThreadedOutputStream(final OutputStream out, final int bufferSize) {
        _out = out;
        _buffer = new RingBuffer(bufferSize);
        _close = false;
        final Thread t = new Thread(new OutputRunner());
        t.start();
    }

    @Override
    public void write(final int b) throws IOException {
        write(new byte[] { (byte) b }, 0, 1);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        if (len < BLOCKSIZE) {
            write0(b, off, len);
        } else {
            int offset = off;
            int written = 0;
            for (int i = 0, s = len / BLOCKSIZE; i < s; i++) {
                write0(b, offset, BLOCKSIZE);
                offset += BLOCKSIZE;
                written += BLOCKSIZE;
            }
            if (written < len) {
                write0(b, offset, len - written);
            }
        }
    }

    private void write0(final byte[] b, final int off, final int len) throws IOException {
        if (_exception != null) {
            throw _exception;
        }
        synchronized (_buffer) {
            while (len > _buffer.putAvailable()) {
                try {
                    _buffer.wait(1000);
                } catch (final Exception e) {
                    System.out.println("Put.Signal.wait:" + e);
                }
            }
            _buffer.put(b, off, len);
            _buffer.notify();
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (_buffer) {
            while (_buffer.getAvailable() > 0) {
                try {
                    _buffer.wait(1000);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        flush();
        _close = true;
        synchronized (_buffer) {
            _buffer.notifyAll();
        }
    }

    class OutputRunner implements Runnable {

        private final byte[] _block = new byte[BLOCKSIZE];

        @Override
        public void run() {
            while (!_close) {
                int readData = 0;
                synchronized (_buffer) {
                    while (_buffer.getAvailable() <= 0) {
                        try {
                            _buffer.wait(1000);
                        } catch (final Exception e) {
                        }
                    }
                    if (!_close) {
                        readData = _buffer.get(_block, 0, BLOCKSIZE);
                        _buffer.notify();
                    }
                }
                if (readData > 0 && !_close) {
                    try {
                        _out.write(_block, 0, readData);
                    } catch (final IOException e) {
                        _exception = e;
                        return;
                    }
                }
            }
        }
    }
}
