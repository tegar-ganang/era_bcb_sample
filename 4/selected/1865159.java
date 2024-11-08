package org.ibex.io;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.util.Hashtable;
import org.ibex.util.*;

/** plays the role of InputStream, OutputStream, Reader and Writer, with logging and unchecked exceptions */
public class Stream {

    protected final In in;

    protected final Out out;

    private StringBuffer log = loggingEnabled ? new StringBuffer(16 * 1024) : null;

    private String newLine = "\r\n";

    private Stream in_next = null;

    public Stream appendStream(Stream in_next) {
        if (this.in_next != null) return this.in_next.appendStream(in_next);
        this.in_next = in_next;
        return this;
    }

    public static boolean loggingEnabled = "true".equals(System.getProperty("ibex.io.stream.logEnabled", "false"));

    public void transcribe(Stream out) {
        try {
            byte[] buf = new byte[1024];
            while (true) {
                int numread = in.read(buf, 0, buf.length);
                if (numread == -1) return;
                out.out.write(buf, 0, numread);
            }
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    public void transcribe(StringBuffer out) {
        char[] buf = new char[1024];
        while (true) {
            int numread = in.read(buf, 0, buf.length);
            if (numread == -1) return;
            out.append(buf, 0, numread);
        }
    }

    public static int countLines(Stream s) {
        int ret = 0;
        while (s.readln() != null) ret++;
        return ret;
    }

    public Stream(byte[] b, int off, int len) {
        this.in = new Stream.In(new ByteArrayInputStream(b, off, len));
        this.out = null;
    }

    public Stream(InputStream in) {
        this.in = new Stream.In(in);
        this.out = null;
    }

    public Stream(OutputStream out) {
        this.in = null;
        this.out = new Stream.Out(out);
    }

    public Stream(InputStream in, OutputStream out) {
        this.in = new Stream.In(in);
        this.out = new Stream.Out(out);
    }

    public Stream(String s) {
        this(new ByteArrayInputStream(s.getBytes()));
    }

    public Stream(File f) {
        try {
            this.in = new Stream.In(new FileInputStream(f));
        } catch (IOException e) {
            throw new StreamException(e);
        }
        this.out = null;
    }

    public Stream(Socket s) {
        try {
            this.in = new Stream.In(s.getInputStream());
        } catch (IOException e) {
            throw new StreamException(e);
        }
        try {
            this.out = new Stream.Out(s.getOutputStream());
        } catch (IOException e) {
            throw new StreamException(e);
        }
    }

    private static int ioe(Exception e) {
        throw new StreamException(e);
    }

    public static class StreamException extends RuntimeException {

        public StreamException(Exception e) {
            super(e);
        }

        public StreamException(String s) {
            super(s);
        }
    }

    public static class EOF extends StreamException {

        public EOF() {
            super("End of stream");
        }
    }

    public static class Closed extends StreamException {

        public Closed(String s) {
            super(s);
        }
    }

    private static Hashtable blocker = new Hashtable();

    public static void kill(Thread thread) {
        Stream block = (Stream) blocker.get(thread);
        if (block == null) {
            DefaultLog.warn(Stream.class, "thread " + thread + " is not blocked on a stream");
        } else {
            DefaultLog.warn(Stream.class, "asynchronously closing " + block);
            block.close();
        }
    }

    public char peekc() {
        Stream old = (Stream) blocker.get(Thread.currentThread());
        try {
            blocker.put(Thread.currentThread(), this);
            flush();
            return in.getc(true);
        } finally {
            if (old == null) blocker.remove(Thread.currentThread()); else blocker.put(Thread.currentThread(), old);
        }
    }

    public char getc() {
        Stream old = (Stream) blocker.get(Thread.currentThread());
        try {
            blocker.put(Thread.currentThread(), this);
            flush();
            char ret = in.getc(false);
            log(ret);
            return ret;
        } finally {
            if (old == null) blocker.remove(Thread.currentThread()); else blocker.put(Thread.currentThread(), old);
        }
    }

    public String readln() {
        Stream old = (Stream) blocker.get(Thread.currentThread());
        try {
            blocker.put(Thread.currentThread(), this);
            flush();
            String s = in.readln();
            log(s);
            log('\n');
            return s;
        } finally {
            if (old == null) blocker.remove(Thread.currentThread()); else blocker.put(Thread.currentThread(), old);
        }
    }

    public void print(String s) {
        Stream old = (Stream) blocker.get(Thread.currentThread());
        try {
            blocker.put(Thread.currentThread(), this);
            logWrite(s);
            out.write(s);
            flush();
        } finally {
            if (old == null) blocker.remove(Thread.currentThread()); else blocker.put(Thread.currentThread(), old);
        }
    }

    public void println(String s) {
        Stream old = (Stream) blocker.get(Thread.currentThread());
        try {
            blocker.put(Thread.currentThread(), this);
            logWrite(s);
            out.write(s);
            out.write(newLine);
            flush();
        } finally {
            if (old == null) blocker.remove(Thread.currentThread()); else blocker.put(Thread.currentThread(), old);
        }
    }

    public void flush() {
        Stream old = (Stream) blocker.get(Thread.currentThread());
        try {
            blocker.put(Thread.currentThread(), this);
            if (out != null) try {
                out.w.flush();
            } catch (IOException e) {
                ioe(e);
            }
        } finally {
            if (old == null) blocker.remove(Thread.currentThread()); else blocker.put(Thread.currentThread(), old);
        }
    }

    public int read(byte[] b, int off, int len) {
        Stream old = (Stream) blocker.get(Thread.currentThread());
        try {
            blocker.put(Thread.currentThread(), this);
            flush();
            int ret = in.readBytes(b, off, len);
            if (log != null) log("\n[read " + ret + " bytes of binary data ]\n");
            nnl = false;
            return ret;
        } finally {
            if (old == null) blocker.remove(Thread.currentThread()); else blocker.put(Thread.currentThread(), old);
        }
    }

    public int read(char[] c, int off, int len) {
        Stream old = (Stream) blocker.get(Thread.currentThread());
        try {
            blocker.put(Thread.currentThread(), this);
            flush();
            int ret = in.read(c, off, len);
            if (log != null && ret != -1) log(new String(c, off, ret));
            return ret;
        } finally {
            if (old == null) blocker.remove(Thread.currentThread()); else blocker.put(Thread.currentThread(), old);
        }
    }

    public void unread(String s) {
        in.unread(s);
    }

    public void close() {
        try {
            if (in != null) in.close();
        } finally {
            if (out != null) out.close();
        }
    }

    public void setNewline(String s) {
        newLine = s;
    }

    /** dumps the connection log into a file */
    public String dumpLog() {
        if (log == null) return "";
        String ret = log.toString();
        log = new StringBuffer(16 * 1024);
        return ret;
    }

    private void log(String s) {
        if (log == null) return;
        if (!nnl) DefaultLog.logger.note("\n[read ] ");
        DefaultLog.logger.note(s + "\n");
        nnl = false;
        if (log != null) log.append(s);
    }

    private void logWrite(String s) {
        if (log == null) return;
        if (nnl) DefaultLog.logger.note("\n");
        DefaultLog.logger.note("[write] " + s + "\n");
        nnl = false;
        if (log != null) log.append(s);
    }

    private void log(char c) {
        if (log == null) return;
        if (c == '\r') return;
        if (!nnl) DefaultLog.logger.note("[read ] ");
        DefaultLog.logger.note(c + "");
        nnl = c != '\n';
        if (log != null) log.append(c);
    }

    private boolean nnl = false;

    private static class Out extends BufferedOutputStream {

        private Writer w = new BufferedWriter(new OutputStreamWriter(this));

        public Out(OutputStream out) {
            super(out);
        }

        public void close() {
            try {
                super.close();
            } catch (Exception e) {
                DefaultLog.error(this, e);
            }
        }

        public void write(String s) {
            try {
                w.write(s);
            } catch (IOException e) {
                ioe(e);
            }
        }
    }

    private class In extends InputStream {

        public final Reader reader = new InputStreamReader(this);

        private InputStream orig;

        public In(InputStream in) {
            orig = in;
        }

        char[] cbuf = new char[8192];

        int cstart = 0;

        int cend = 0;

        byte[] buf = new byte[8192];

        int start = 0;

        int end = 0;

        boolean flushing = false;

        public int available() {
            return flushing ? 0 : (end - start);
        }

        public void close() {
            try {
                if (orig != null) orig.close();
            } catch (Exception e) {
                DefaultLog.error(this, e);
            }
        }

        public char getc(boolean peek) {
            try {
                if (cstart == cend) {
                    cstart = 0;
                    cend = reader.read(cbuf, 0, cbuf.length);
                    if (cend == -1) {
                        cend = cstart;
                        if (in_next == null) throw new EOF();
                        orig = in_next.in.orig;
                        in_next = in_next.in_next;
                        return getc(peek);
                    }
                }
                return peek ? cbuf[cstart] : cbuf[cstart++];
            } catch (IOException e) {
                return (char) ioe(e);
            }
        }

        public String readln() {
            try {
                while (true) {
                    for (int i = cstart; i < cend; i++) if (cbuf[i] == '\n') {
                        int begin = cstart;
                        int len = i - cstart;
                        cstart = i + 1;
                        if (cbuf[begin] == '\r') {
                            begin++;
                            len--;
                        }
                        while (len > 0 && cbuf[begin + len - 1] == '\r') {
                            len--;
                        }
                        return new String(cbuf, begin, len);
                    }
                    ensurec(256);
                    int numread = reader.read(cbuf, cend, cbuf.length - cend);
                    if (numread == -1) {
                        if (cstart == cend) return null;
                        String ret = new String(cbuf, cstart, cend - cstart);
                        cstart = cend = 0;
                        return ret;
                    }
                    cend += numread;
                }
            } catch (IOException e) {
                ioe(e);
                return null;
            }
        }

        public int read(char[] c, int pos, int len) {
            try {
                if (cstart == cend) {
                    cstart = 0;
                    cend = reader.read(cbuf, 0, cbuf.length);
                    if (cend == -1) {
                        cend = cstart;
                        return -1;
                    }
                }
                if (len > cend - cstart) len = cend - cstart;
                System.arraycopy(cbuf, cstart, c, pos, len);
                cstart += len;
                return len;
            } catch (IOException e) {
                ioe(e);
                return -1;
            }
        }

        public int readBytes(byte[] b, int pos, int len) {
            flushchars();
            return read(b, pos, len);
        }

        public int read() {
            byte[] b = new byte[1];
            if (read(b, 0, 1) == -1) return -1;
            return (int) b[0];
        }

        public int read(byte[] b, int pos, int len) {
            try {
                if (start == end) {
                    start = 0;
                    end = orig.read(buf, 0, buf.length);
                    if (end == -1) {
                        end = start;
                        return -1;
                    }
                }
                if (len > end - start) len = end - start;
                System.arraycopy(buf, start, b, pos, len);
                start += len;
                return len;
            } catch (IOException e) {
                ioe(e);
                return -1;
            }
        }

        private void growc(int s) {
            char[] cbuf2 = new char[cbuf.length + s * 2];
            System.arraycopy(cbuf, 0, cbuf2, 0, cbuf.length);
            cbuf = cbuf2;
        }

        private void shiftc() {
            char[] cbuf2 = new char[cbuf.length];
            System.arraycopy(cbuf, cstart, cbuf2, 0, cend - cstart);
            cend -= cstart;
            cstart = 0;
            cbuf = cbuf2;
        }

        private void ensurec(int space) {
            if (cend - cstart + space > cbuf.length) growc(space);
            if (cend + space > cbuf.length) shiftc();
        }

        private void growb(int s) {
            byte[] buf2 = new byte[buf.length + s * 2];
            System.arraycopy(buf, 0, buf2, 0, buf.length);
            buf = buf2;
        }

        private void ensureb2(int space) {
            if (end - start + space > buf.length) growb(space);
            if (start < space) unshiftb();
        }

        private void unshiftb() {
            System.arraycopy(buf, start, buf, buf.length - (end - start), end - start);
            start = buf.length - (end - start);
            end = buf.length;
        }

        public void unread(String s) {
            ensurec(s.length());
            s.getChars(0, s.length(), cbuf, cend);
            cend += s.length();
        }

        private void flushchars() {
            try {
                flushing = true;
                for (; reader.ready(); reader.read(cbuf, cend++, 1)) ensurec(1);
                unreader.write(cbuf, cstart, cend);
                cstart = cend = 0;
                unreader.flush();
            } catch (IOException e) {
                ioe(e);
            } finally {
                flushing = false;
            }
        }

        Writer unreader = new OutputStreamWriter(new InOutputStream());

        private class InOutputStream extends OutputStream {

            public void close() {
            }

            public void write(int i) throws IOException {
                byte[] b = new byte[1];
                b[0] = (byte) i;
                write(b, 0, 1);
            }

            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }

            public void write(byte[] b, int p, int l) {
                ensureb2(l);
                System.arraycopy(b, p, buf, start - l, l);
                start -= l;
            }
        }
    }

    public static interface Transformer {

        public Stream transform(Stream in);
    }
}
