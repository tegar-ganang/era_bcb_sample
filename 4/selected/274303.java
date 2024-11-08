package org.chessworks.common.javatools.io;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.Collection;

/**
 * The RecordedReader class represents a reader which writes a copies of its
 * input as it reads. Among other uses, this can be helpful for recording a copy
 * of the input to a log.
 * 
 * @author Doug Bateman
 */
public class RecordedReader extends Reader {

    protected final Reader reader;

    protected final Writer writer;

    /**
	 * Creates a RecordedReader which reads from the underlying reader and sends
	 * a copy of the input to the writer.
	 * 
	 * @param source
	 *            the underlying Reader to use to obtain input.
	 * @param recorder
	 *            the Writer receiving a copy of all data as it is read.
	 */
    public RecordedReader(Reader source, Writer recorder) {
        if (source == null) throw new NullPointerException("Source reader must not be null.");
        this.reader = source;
        if (recorder == null) {
            this.writer = NullWriter.INSTANCE;
        } else {
            this.writer = recorder;
        }
    }

    /**
	 * Creates a RecordedReader which reads from the underlying reader and sends
	 * a copy of the input to each of the provided writers. The array of writers
	 * is cloned to ensure the contents remain unchanged.
	 * 
	 * 
	 * @param source
	 *            the underlying Reader to use to obtain input.
	 * @param recorders
	 *            the Writer's receiving a copy of all data as it is read.
	 */
    public RecordedReader(Reader source, Writer... recorders) {
        if (source == null) throw new NullPointerException("Source reader must not be null.");
        this.reader = source;
        if (recorders == null) {
            this.writer = NullWriter.INSTANCE;
        } else if (recorders.length == 1) {
            this.writer = recorders[0];
        } else {
            this.writer = new MulticastWriter(recorders);
        }
    }

    /**
	 * Creates a RecordedReader which reads from the underlying reader and sends
	 * a copy of the input to each of the provided writers. The collection is
	 * copied to ensure the contents remain unchanged.
	 * 
	 * @param source
	 *            the underlying Reader to use to obtain input.
	 * @param recorders
	 *            the Writer's receiving a copy of all data as it is read.
	 */
    public RecordedReader(Reader source, Collection<Writer> recorders) {
        if (source == null) throw new NullPointerException("Source reader must not be null.");
        this.reader = source;
        if (recorders == null) {
            this.writer = NullWriter.INSTANCE;
        } else if (recorders.size() == 1) {
            this.writer = recorders.iterator().next();
        } else {
            this.writer = new MulticastWriter(recorders);
        }
    }

    /**
	 * Does a shallow copy, creating a new RecordedReader pointing to the same
	 * underlying source Reader and target Writers.
	 * 
	 * @see java.lang.Object#clone()
	 */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
	 * Returns a hashCode build from the reader and writers.
	 * 
	 * @see java.lang.Object#hashCode()
	 */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((reader == null) ? 0 : reader.hashCode());
        result = prime * result + ((writer == null) ? 0 : writer.hashCode());
        return result;
    }

    /**
	 * Two RecordedReaders are equal if their reader and list of writers are
	 * equal.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RecordedReader other = (RecordedReader) obj;
        if (reader == null) {
            if (other.reader != null) {
                return false;
            }
        } else if (!reader.equals(other.reader)) {
            return false;
        }
        if (writer == null) {
            if (other.writer != null) {
                return false;
            }
        } else if (!writer.equals(other.writer)) {
            return false;
        }
        return true;
    }

    /**
	 * Returns a String representation of this RecordedReader.
	 * 
	 * @see java.lang.Object#toString()
	 */
    @Override
    public String toString() {
        return "RecordedReader [reader=" + reader + ", writer=" + writer + "]";
    }

    /**
	 * @see java.io.Reader#close()
	 */
    public void close() throws IOException {
        reader.close();
    }

    /**
	 * @see java.io.Reader#mark(int)
	 */
    public void mark(int readAheadLimit) throws IOException {
        reader.mark(readAheadLimit);
    }

    /**
	 * @see java.io.Reader#markSupported()
	 */
    public boolean markSupported() {
        return reader.markSupported();
    }

    /**
	 * @see java.io.Reader#read()
	 */
    public int read() throws IOException {
        int ch = reader.read();
        if (ch >= 0) {
            writer.write(ch);
        }
        return ch;
    }

    /**
	 * @see java.io.Reader#read(char[], int, int)
	 */
    public int read(char[] cbuf, int off, int len) throws IOException {
        int count = reader.read(cbuf, off, len);
        if (count >= 0) {
            writer.write(cbuf, off, count);
        }
        return count;
    }

    /**
	 * @see java.io.Reader#read(char[])
	 */
    public int read(char[] cbuf) throws IOException {
        int count = reader.read(cbuf);
        if (count >= 0) {
            writer.write(cbuf, 0, count);
        }
        return count;
    }

    /**
	 * @see java.io.Reader#read(java.nio.CharBuffer)
	 */
    public int read(CharBuffer target) throws IOException {
        int len = target.remaining();
        char[] cbuf = new char[len];
        int count = read(cbuf, 0, len);
        if (count > 0) {
            target.put(cbuf, 0, count);
            writer.write(cbuf, 0, count);
        }
        return count;
    }

    /**
	 * @see java.io.Reader#ready()
	 */
    public boolean ready() throws IOException {
        return reader.ready();
    }

    /**
	 * @see java.io.Reader#reset()
	 */
    public void reset() throws IOException {
        reader.reset();
    }

    /**
	 * @see java.io.Reader#skip(long)
	 */
    public long skip(long n) throws IOException {
        return reader.skip(n);
    }
}
