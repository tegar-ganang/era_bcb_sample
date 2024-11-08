package jaxlib.io.stream;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.jaxlib_private.io.CheckIOArg;

/**
 * A writer which writes to nirvana.
 * <p>
 * By convention, code which is called to write to a <tt>NullOutputStream</tt> may skip all write 
 * operations. Thus the output stream may reveive only partial or no data at all.
 * </p><p>
 * This stream does not write letters to the band.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: NullWriter.java,v 1.3 2004/08/30 14:37:00 joerg_wassmer Exp $
 */
public class NullWriter extends XWriter {

    /**
   * A public instance of <tt>NullWriter</tt>.
   * Use this only if you are sure your I/O operations do not aquire a monitor on the stream to
   * synchronize threads.
   *
   * @since JaXLib 1.0
   */
    public static final NullWriter SHARED_INSTANCE = new NullWriter();

    public NullWriter() {
        super();
    }

    /**
   * @throws IOException if the {@link #isOpen() isOpen()} method returns <tt>false</tt>.
   */
    private void ensureOpen() throws ClosedChannelException {
        if (!isOpen()) throw new ClosedChannelException();
    }

    @Override
    public NullWriter append(char c) throws IOException {
        ensureOpen();
        return this;
    }

    @Override
    public NullWriter append(CharSequence s) throws IOException {
        ensureOpen();
        return this;
    }

    /**
   * Redirects to {@link #closeInstance() closeInstance()}.
   */
    @Override
    public void close() throws IOException {
        closeInstance();
    }

    /**
   * This implementation does nothing.
   */
    @Override
    public void closeInstance() throws IOException {
    }

    /**
   * This implementation returns <tt>true</tt> always.
   * Subclasses may overwrite this method to return <tt>false</tt>, causing write methods throwing an IOException.
   */
    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public long transferFrom(Readable in, long maxCount) throws IOException {
        ensureOpen();
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return 0;
        if (in instanceof Reader) return transferFromImpl((Reader) in, maxCount); else return super.transferFrom(in, maxCount);
    }

    private long transferFromImpl(Reader in, long maxCount) throws IOException {
        long count = 0;
        if (maxCount < 0) {
            for (long step; (step = in.skip(Long.MAX_VALUE)) > 0; ) count += step;
        } else {
            for (long step; (count < maxCount) && ((step = in.skip(maxCount - count)) > 0); ) count += step;
        }
        return count;
    }

    /**
   * Does nothing except checking {@link #isOpen() isOpen()}.
   *
   * @throws IOException if the <tt>isOpen()</tt> method returns <tt>false</tt>.
   */
    @Override
    public void write(int b) throws IOException {
        ensureOpen();
    }

    /**
   * Does nothing except checking {@link #isOpen() isOpen()}.
   *
   * @throws IOException                if the <tt>isOpen()</tt> method returns <tt>false</tt>.
   * @throws IndexOutOfBoundsException  if <tt>offs</tt> or <tt>len</tt> are invalid.
   * @throws NullPointerException       if <tt>buf == null</tt>.
   */
    @Override
    public void write(char[] buf, int off, int len) throws IOException {
        CheckIOArg.range(buf, off, len);
        ensureOpen();
    }

    /**
   * Does nothing except setting the specified buffer's position to its limit and checking {@link #isOpen() isOpen()}.
   *
   * @throws IOException          if the <tt>isOpen()</tt> method returns <tt>false</tt>.
   * @throws NullPointerException if <tt>source == null</tt>.
   */
    @Override
    public int write(CharBuffer source) throws IOException {
        ensureOpen();
        int pos = source.position();
        int lim = source.limit();
        source.position(lim);
        return lim - pos;
    }

    /**
   * Does nothing except checking {@link #isOpen() isOpen()}.
   *
   * @throws IOException                if the <tt>isOpen()</tt> method returns <tt>false</tt>.
   * @throws IndexOutOfBoundsException  if <tt>offs</tt> or <tt>len</tt> are invalid.
   * @throws NullPointerException       if <tt>v == null</tt>.
   */
    @Override
    public NullWriter print(CharSequence v, int off, int len) throws IOException {
        CheckIOArg.range(v.length(), off, len);
        ensureOpen();
        return this;
    }

    @Override
    public NullWriter print(double v) throws IOException {
        ensureOpen();
        return this;
    }

    @Override
    public NullWriter print(float v) throws IOException {
        ensureOpen();
        return this;
    }

    @Override
    public NullWriter print(int v, int radix) throws IOException {
        ensureOpen();
        return this;
    }

    @Override
    public NullWriter print(long v, int radix) throws IOException {
        ensureOpen();
        return this;
    }

    @Override
    public NullWriter printf(Locale l, String format, Object... args) throws IOException {
        ensureOpen();
        return this;
    }
}
