package jaxlib.io.stream;

import java.io.DataInput;
import java.io.InputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.jaxlib_private.io.CheckIOArg;

/**
 * An output stream which writes to nirvana.
 * <p>
 * By convention, code which is called to write to a <tt>NullOutputStream</tt> may skip all write 
 * operations. Thus the output stream may reveive only partial or no data at all.
 * </p><p>
 * This stream does not write letters to the band.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: NullOutputStream.java,v 1.3 2004/08/30 14:37:00 joerg_wassmer Exp $
 */
public class NullOutputStream extends XOutputStream implements WritableByteChannel {

    /**
   * A public instance of <tt>NullOutputStream</tt>.
   * Use this only if you are sure your I/O operations do not aquire a monitor on the stream to
   * synchronize threads.
   *
   * @since JaXLib 1.0
   */
    public static final NullOutputStream SHARED_INSTANCE = new NullOutputStream();

    public NullOutputStream() {
        super();
    }

    /**
   * @throws IOException if the {@link #isOpen() isOpen()} method returns <tt>false</tt>.
   */
    private void ensureOpen() throws ClosedChannelException {
        if (!isOpen()) throw new ClosedChannelException();
    }

    /**
   * Does nothing except setting the specified buffer's position to its limit.
   *
   * @throws IOException          if the <tt>isOpen()</tt> method returns <tt>false</tt>.
   * @throws NullPointerException if <tt>source == null</tt>.
   */
    private int writeImpl(Buffer source) throws IOException {
        ensureOpen();
        int pos = source.position();
        int lim = source.limit();
        source.position(lim);
        return lim - pos;
    }

    /**
   * Redirects to {@link #closeInstance() closeInstance()}.
   */
    public void close() throws IOException {
        closeInstance();
    }

    /**
   * This implementation does nothing.
   */
    public void closeInstance() throws IOException {
    }

    /**
   * This implementation returns <tt>true</tt> always.
   * Subclasses may overwrite this method to return <tt>false</tt>, causing write methods throwing an IOException.
   */
    public boolean isOpen() {
        return true;
    }

    public void transferFullyFrom(DataInput in, long count) throws IOException {
        ensureOpen();
        CheckIOArg.count(count);
        boolean single = false;
        while (count > 0) {
            if (single) {
                in.readByte();
                single = false;
            } else {
                int step = in.skipBytes((int) Math.min(count, Integer.MAX_VALUE));
                count -= step;
                single = step == 0;
            }
        }
    }

    public long transferFrom(InputStream in, long maxCount) throws IOException {
        ensureOpen();
        CheckArg.maxCount(maxCount);
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
    public void write(int b) throws IOException {
        ensureOpen();
    }

    /**
   * Does nothing except checking arguments and {@link #isOpen() isOpen()}.
   *
   * @throws IOException                if the <tt>isOpen()</tt> method returns <tt>false</tt>.
   * @throws IndexOutOfBoundsException  if <tt>offs</tt> or <tt>len</tt> are invalid.
   * @throws NullPointerException       if <tt>buf == null</tt>.
   */
    public void write(byte[] buf, int off, int len) throws IOException {
        CheckIOArg.range(buf, off, len);
        ensureOpen();
    }

    /**
   * Does nothing except setting the specified buffer's position to its limit and checking {@link #isOpen() isOpen()}.
   *
   * @throws IOException          if the <tt>isOpen()</tt> method returns <tt>false</tt>.
   * @throws NullPointerException if <tt>source == null</tt>.
   */
    public int write(ByteBuffer source) throws IOException {
        return writeImpl(source);
    }

    /**
   * Does nothing except setting the specified buffer's position to its limit and checking {@link #isOpen() isOpen()}.
   *
   * @throws IOException          if the <tt>isOpen()</tt> method returns <tt>false</tt>.
   * @throws NullPointerException if <tt>source == null</tt>.
   */
    public int write(CharBuffer source) throws IOException {
        return writeImpl(source);
    }

    /**
   * Does nothing except setting the specified buffer's position to its limit and checking {@link #isOpen() isOpen()}.
   *
   * @throws IOException          if the <tt>isOpen()</tt> method returns <tt>false</tt>.
   * @throws NullPointerException if <tt>source == null</tt>.
   */
    public int write(DoubleBuffer source) throws IOException {
        return writeImpl(source);
    }

    /**
   * Does nothing except setting the specified buffer's position to its limit and checking {@link #isOpen() isOpen()}.
   *
   * @throws IOException          if the <tt>isOpen()</tt> method returns <tt>false</tt>.
   * @throws NullPointerException if <tt>source == null</tt>.
   */
    public int write(FloatBuffer source) throws IOException {
        return writeImpl(source);
    }

    /**
   * Does nothing except setting the specified buffer's position to its limit and checking {@link #isOpen() isOpen()}.
   *
   * @throws IOException          if the <tt>isOpen()</tt> method returns <tt>false</tt>.
   * @throws NullPointerException if <tt>source == null</tt>.
   */
    public int write(IntBuffer source) throws IOException {
        return writeImpl(source);
    }

    /**
   * Does nothing except setting the specified buffer's position to its limit and checking {@link #isOpen() isOpen()}.
   *
   * @throws IOException          if the <tt>isOpen()</tt> method returns <tt>false</tt>.
   * @throws NullPointerException if <tt>source == null</tt>.
   */
    public int write(LongBuffer source) throws IOException {
        return writeImpl(source);
    }

    /**
   * Does nothing except setting the specified buffer's position to its limit and checking {@link #isOpen() isOpen()}.
   *
   * @throws IOException          if the <tt>isOpen()</tt> method returns <tt>false</tt>.
   * @throws NullPointerException if <tt>source == null</tt>.
   */
    public int write(ShortBuffer source) throws IOException {
        return writeImpl(source);
    }

    /**
   * Does nothing except checking {@link #isOpen() isOpen()}.
   *
   * @throws IOException          if the <tt>isOpen()</tt> method returns <tt>false</tt>.
   * @throws NullPointerException if <tt>s == null</tt>.
   */
    public void writeUTF(String s) throws IOException {
        if (s == null) throw new NullPointerException();
        ensureOpen();
    }
}
