package org.shava.core.impl;

import java.util.HashSet;
import java.util.Set;
import org.shava.core.Buffer;
import org.shava.core.exceptions.NoBufferReaderException;
import org.shava.core.exceptions.NoBufferWriterException;

/**
 * This class decorate a buffer with a reference counter.
 * A thread can register himself.
 * 
 * @author Julián Gutierrez Oschmann.
 *
 * @param <T>
 */
public class RefCountBuffer<T> implements Buffer<T> {

    /**
     * Decorated buffer.
     */
    private Buffer<T> _buffer;

    /**
     * Writer threads.
     */
    private Set<Thread> _writersThreads = new HashSet<Thread>();

    /**
     * Readers threads.
     */
    private Set<Thread> _readersThreads = new HashSet<Thread>();

    /**
     * Writers references.
     */
    private int _writers = 0;

    /**
     * Readers references.
     */
    private int _readers = 0;

    public RefCountBuffer(final Buffer<T> b) {
        _buffer = b;
    }

    public T get() {
        if (isEmpty() && _writers == 0) {
            throw new NoBufferWriterException("There's no other references to this buffer");
        } else {
            while (true) {
                try {
                    return _buffer.get();
                } catch (Exception e) {
                }
                if (isEmpty() && _writers == 0) {
                    throw new NoBufferWriterException("There's no other references to this buffer");
                }
            }
        }
    }

    public void put(final T x) {
        if (_readers == 0) {
            throw new NoBufferReaderException("There's no other references to this buffer");
        } else {
            boolean retry = true;
            while (retry) {
                try {
                    if (_readers == 0) {
                        throw new NoBufferReaderException("There's no other references to this buffer");
                    }
                    _buffer.put(x);
                    retry = false;
                } catch (Exception e) {
                }
            }
        }
    }

    public void acquireWriter(final Thread writerThread) {
        _writers++;
        _writersThreads.add(writerThread);
    }

    public void releaseWriter() {
        if (_writers == 0) {
            throw new IllegalStateException("Can't release this buffer, try acquire first");
        }
        _writers--;
        if (_writers == 0) {
            for (Thread t : _readersThreads) {
                t.interrupt();
            }
        }
    }

    public void acquireReader(final Thread readerThread) {
        _readers++;
        _readersThreads.add(readerThread);
    }

    public void releaseReader() {
        if (_readers == 0) {
            throw new IllegalStateException("Can't release this buffer, try acquire first");
        }
        _readers--;
    }

    public boolean isEmpty() {
        return _buffer.isEmpty();
    }
}
