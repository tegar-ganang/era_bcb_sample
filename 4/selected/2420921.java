package org.armedbear.lisp.util;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

public class RandomAccessCharacterFile {

    private class RandomAccessInputStream extends PushbackInputStream {

        public RandomAccessInputStream() {
            super(null);
        }

        private byte[] read_buf = new byte[1];

        @Override
        public final int read() throws IOException {
            int len = read(read_buf);
            if (len == 1) {
                return 0xff & (int) read_buf[0];
            } else {
                return -1;
            }
        }

        @Override
        public final int read(byte[] b, int off, int len) throws IOException {
            return RandomAccessCharacterFile.this.read(b, off, len);
        }

        @Override
        public final void unread(int b) throws IOException {
            RandomAccessCharacterFile.this.unreadByte((byte) b);
        }

        @Override
        public final void unread(byte[] b, int off, int len) throws IOException {
            for (int i = 0; i < len; i++) this.unread(b[off + i]);
        }

        @Override
        public final void unread(byte[] b) throws IOException {
            this.unread(b, 0, b.length);
        }

        @Override
        public final int available() throws IOException {
            return (int) (RandomAccessCharacterFile.this.length() - RandomAccessCharacterFile.this.position());
        }

        @Override
        public final synchronized void mark(int readlimit) {
        }

        @Override
        public final boolean markSupported() {
            return false;
        }

        @Override
        public final synchronized void reset() throws IOException {
            throw new IOException("Operation not supported");
        }

        @Override
        public final long skip(long n) throws IOException {
            RandomAccessCharacterFile.this.position(RandomAccessCharacterFile.this.position() + n);
            return n;
        }

        @Override
        public final int read(byte[] b) throws IOException {
            return this.read(b, 0, b.length);
        }

        @Override
        public final void close() throws IOException {
            RandomAccessCharacterFile.this.close();
        }
    }

    private class RandomAccessOutputStream extends OutputStream {

        RandomAccessOutputStream() {
        }

        private byte[] buf = new byte[1];

        public final void write(int b) throws IOException {
            buf[0] = (byte) b;
            RandomAccessCharacterFile.this.write(buf, 0, 1);
        }

        @Override
        public final void write(byte[] b) throws IOException {
            RandomAccessCharacterFile.this.write(b, 0, b.length);
        }

        @Override
        public final void write(byte[] b, int off, int len) throws IOException {
            RandomAccessCharacterFile.this.write(b, off, len);
        }

        @Override
        public final void flush() throws IOException {
            RandomAccessCharacterFile.this.flush();
        }

        @Override
        public final void close() throws IOException {
            RandomAccessCharacterFile.this.close();
        }
    }

    static Reader staticReader = new StringReader("");

    private class RandomAccessReader extends PushbackReader {

        RandomAccessReader() {
            super(staticReader);
        }

        @Override
        public final void close() throws IOException {
            RandomAccessCharacterFile.this.close();
        }

        private char[] read_buf = new char[1];

        @Override
        public final int read() throws IOException {
            int n = this.read(read_buf);
            if (n == 1) return read_buf[0]; else return -1;
        }

        @Override
        public final void unread(int c) throws IOException {
            RandomAccessCharacterFile.this.unreadChar((char) c);
        }

        @Override
        public final void unread(char[] cbuf, int off, int len) throws IOException {
            for (int i = 0; i < len; i++) this.unread(cbuf[off + i]);
        }

        @Override
        public final void unread(char[] cbuf) throws IOException {
            this.unread(cbuf, 0, cbuf.length);
        }

        @Override
        public final int read(CharBuffer target) throws IOException {
            throw new IOException("Not implemented");
        }

        @Override
        public final int read(char[] cbuf) throws IOException {
            return RandomAccessCharacterFile.this.read(cbuf, 0, cbuf.length);
        }

        @Override
        public final int read(char[] cb, int off, int len) throws IOException {
            return RandomAccessCharacterFile.this.read(cb, off, len);
        }

        @Override
        public final boolean ready() throws IOException {
            return true;
        }
    }

    private class RandomAccessWriter extends Writer {

        RandomAccessWriter() {
        }

        public final void close() throws IOException {
            RandomAccessCharacterFile.this.close();
        }

        public final void flush() throws IOException {
            RandomAccessCharacterFile.this.flush();
        }

        @Override
        public final void write(char[] cb, int off, int len) throws IOException {
            RandomAccessCharacterFile.this.write(cb, off, len);
        }
    }

    static final int BUFSIZ = 4 * 1024;

    private RandomAccessWriter writer;

    private RandomAccessReader reader;

    private RandomAccessInputStream inputStream;

    private RandomAccessOutputStream outputStream;

    private FileChannel fcn;

    private Charset cset;

    private CharsetEncoder cenc;

    private CharsetDecoder cdec;

    /**
     * bbuf is treated as a cache of the file content.
     * If it points to somewhere in the middle of the file, it holds the copy of the file content,
     * even when you are writing a large chunk of data.  If you write in the middle of a file,
     * bbuf first gets filled with contents of the data, and only after that any new data is
     * written on bbuf.
     * The exception is when you are appending data at the end of the file.
     */
    private ByteBuffer bbuf;

    private boolean bbufIsDirty;

    private boolean bbufIsReadable;

    private long bbufpos;

    public RandomAccessCharacterFile(RandomAccessFile raf, String encoding) throws IOException {
        fcn = raf.getChannel();
        cset = (encoding == null) ? Charset.defaultCharset() : Charset.forName(encoding);
        cdec = cset.newDecoder();
        cdec.onMalformedInput(CodingErrorAction.REPLACE);
        cdec.onUnmappableCharacter(CodingErrorAction.REPLACE);
        cenc = cset.newEncoder();
        bbuf = ByteBuffer.allocate(BUFSIZ);
        bbuf.flip();
        bbufIsDirty = false;
        bbufIsReadable = false;
        bbufpos = fcn.position();
        reader = new RandomAccessReader();
        writer = new RandomAccessWriter();
        inputStream = new RandomAccessInputStream();
        outputStream = new RandomAccessOutputStream();
    }

    public Writer getWriter() {
        return writer;
    }

    public PushbackReader getReader() {
        return reader;
    }

    public PushbackInputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public final void close() throws IOException {
        internalFlush(true);
        fcn.close();
    }

    public final void flush() throws IOException {
        internalFlush(false);
    }

    private final boolean ensureReadBbuf(boolean force) throws IOException {
        boolean bufReady = true;
        if ((bbuf.remaining() == 0) || force || !bbufIsReadable) {
            if (bbufIsDirty) {
                bbuf.flip();
                fcn.position(bbufpos);
                fcn.write(bbuf);
                bbufpos += bbuf.position();
                bbuf.clear();
            } else {
                int bbufEnd = bbufIsReadable ? bbuf.limit() : bbuf.position();
                fcn.position(bbufpos + bbufEnd);
                bbufpos += bbuf.position();
                bbuf.compact();
            }
            bufReady = (fcn.read(bbuf) != -1);
            bbuf.flip();
            bbufIsReadable = true;
        }
        return bufReady;
    }

    final int read(char[] cb, int off, int len) throws IOException {
        CharBuffer cbuf = CharBuffer.wrap(cb, off, len);
        boolean decodeWasUnderflow = false;
        boolean atEof = false;
        while ((cbuf.remaining() > 0) && !atEof) {
            atEof = !ensureReadBbuf(decodeWasUnderflow);
            CoderResult r = cdec.decode(bbuf, cbuf, atEof);
            decodeWasUnderflow = (CoderResult.UNDERFLOW == r);
        }
        if (cbuf.remaining() == len) {
            return -1;
        } else {
            return len - cbuf.remaining();
        }
    }

    final void write(char[] cb, int off, int len) throws IOException {
        CharBuffer cbuf = CharBuffer.wrap(cb, off, len);
        encodeAndWrite(cbuf, false, false);
    }

    final void internalFlush(boolean endOfFile) throws IOException {
        if (endOfFile) {
            CharBuffer cbuf = CharBuffer.allocate(0);
            encodeAndWrite(cbuf, true, endOfFile);
        } else {
            flushBbuf(false);
        }
    }

    private final void encodeAndWrite(CharBuffer cbuf, boolean flush, boolean endOfFile) throws IOException {
        while (cbuf.remaining() > 0) {
            CoderResult r = cenc.encode(cbuf, bbuf, endOfFile);
            bbufIsDirty = true;
            if (CoderResult.OVERFLOW == r || bbuf.remaining() == 0) {
                flushBbuf(false);
                bbuf.clear();
            }
        }
        if (bbuf.position() > 0 && bbufIsDirty && flush) {
            flushBbuf(false);
        }
    }

    public final void position(long newPosition) throws IOException {
        flushBbuf(true);
        long bbufend = bbufpos + (bbufIsReadable ? bbuf.limit() : bbuf.position());
        if (newPosition >= bbufpos && newPosition < bbufend) {
            bbuf.position((int) (newPosition - bbufpos));
        } else {
            fcn.position(newPosition);
            bbuf.clear();
            bbuf.flip();
            bbufpos = newPosition;
        }
    }

    public final long position() throws IOException {
        return bbufpos + bbuf.position();
    }

    public final long length() throws IOException {
        flushBbuf(false);
        return fcn.size();
    }

    private final void flushBbuf(boolean commitOnly) throws IOException {
        if (!bbufIsDirty) return;
        fcn.position(bbufpos);
        if (commitOnly || bbufIsReadable) {
            ByteBuffer dup = bbuf.duplicate();
            dup.flip();
            fcn.write(dup);
            return;
        }
        bbuf.flip();
        fcn.write(bbuf);
        bbufpos += bbuf.position();
        bbuf.clear();
        bbuf.flip();
        bbufIsDirty = false;
        bbufIsReadable = false;
    }

    public final int read(byte[] b, int off, int len) throws IOException {
        int pos = off;
        boolean atEof = false;
        while (pos - off < len && !atEof) {
            atEof = !ensureReadBbuf(false);
            int want = len - pos;
            if (want > bbuf.remaining()) {
                want = bbuf.remaining();
            }
            bbuf.get(b, pos, want);
            pos += want;
        }
        return pos - off;
    }

    private CharBuffer singleCharBuf;

    private ByteBuffer shortByteBuf;

    public final void unreadChar(char c) throws IOException {
        if (singleCharBuf == null) {
            singleCharBuf = CharBuffer.allocate(1);
            shortByteBuf = ByteBuffer.allocate((int) cenc.maxBytesPerChar());
        }
        singleCharBuf.clear();
        singleCharBuf.append(c);
        singleCharBuf.flip();
        shortByteBuf.clear();
        cenc.encode(singleCharBuf, shortByteBuf, false);
        int n = shortByteBuf.position();
        long pos = position() - n;
        position(pos);
    }

    public final void unreadByte(byte b) throws IOException {
        long pos = position() - 1;
        position(pos);
    }

    final void write(byte[] b, int off, int len) throws IOException {
        int pos = off;
        while (pos < off + len) {
            int want = len;
            if (want > bbuf.remaining()) {
                want = bbuf.remaining();
            }
            bbuf.put(b, pos, want);
            pos += want;
            bbufIsDirty = true;
            if (bbuf.remaining() == 0) {
                flushBbuf(false);
                bbuf.clear();
            }
        }
    }
}
