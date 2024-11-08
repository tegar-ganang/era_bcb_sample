package jaxlib.io.stream;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.Formatter;
import java.util.Locale;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.jaxlib_private.io.CheckIOArg;
import jaxlib.lang.Ints;
import jaxlib.lang.Longs;
import jaxlib.system.SystemProperties;
import jaxlib.text.SimpleIntegerFormat;

/**
 * Provides an extented version of {@link java.io.Writer}.
 * <tt>XWriter</tt> implements <tt>print</tt> methods like known from {@link java.io.PrintWriter} 
 * <p>
 * By default <tt>XWriter</tt> methods are <b>not</b> threadsafe. 
 * If you need a threadsafe <tt>XWriter</tt> you may use {@link jaxlib.io.stream.concurrent.SynchronizedXWriter}.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: XWriter.java,v 1.5 2004/09/24 01:10:48 joerg_wassmer Exp $
 */
public abstract class XWriter extends Writer {

    private Formatter formatter;

    protected XWriter() {
        super();
    }

    /**
   * Closes this stream, flushing it first.  Once a stream has been closed,
   * further write() or flush() invocations will cause an IOException to be
   * thrown. Closing a previously-closed stream, however, has no effect.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @since JDK 1.1
   */
    @Override
    public abstract void close() throws IOException;

    /**
   * Closes this stream without closing the underlying stream (if there is one). 
   * A call to this method has the same effect as a call to the {@link #close() close()} method, but the underlying stream keeps opened.
   *
   * @see #close()
   *
   * @since JaXLib 1.0
   */
    public abstract void closeInstance() throws IOException;

    /**
   * Returns <tt>true</tt> iff this stream is not closed.
   *
   * @see #close()
   *
   * @since JaXLib 1.0
   */
    public abstract boolean isOpen();

    /**
   * Writes the specified single character.  
   * The character to be written is contained in the 16 low-order bits of the given integer value; the 16 high-order bits are ignored.
   * This method blocks until the specified char has been written or an I/O error occurs.
   *
   * @param c the character to be written.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @since JDK 1.1
   */
    @Override
    public abstract void write(int c) throws IOException;

    /**
   * Appends the specified character to this writer.
   * <p>
   * This method has been implemented for compatibility with the new <tt>Appendable</tt> interface. 
   * It simply redirects to {@link #write(int) write(c)}.
   * </p>
   * 
   * <p> An invocation of this method of the form <tt>out.append(c)</tt>
   * behaves in exactly the same way as the invocation
   *
   * <pre>
   *     out.write(c) </pre>
   *
   * @param  c
   *         The 16-bit character to append
   *
   * @return  This writer
   *
   * @since JDK 1.5
   */
    @Override
    public XWriter append(char c) throws IOException {
        write(c);
        return this;
    }

    /**
   * Appends the specified character sequence to this writer.
   * <p>
   * This method has been implemented for compatibility with the new <tt>Appendable</tt> interface. 
   * It simply redirects to {@link #print(CharSequence) print(s)}.
   * </p>
   * 
   * <p> An invocation of this method of the form <tt>out.append(csq)</tt>
   * behaves in exactly the same way as the invocation
   *
   * <pre>
   *     out.print(csq) </pre>
   *
   * <p> Depending on the specification of <tt>toString</tt> for the
   * character sequence <tt>csq</tt>, the entire sequence may not be
   * appended. For instance, invoking the <tt>toString</tt> method of a
   * character buffer will return a subsequence whose content depends upon
   * the buffer's position and limit.
   *
   * @param  s
   *         The character sequence to append.  If <tt>csq</tt> is
   *         <tt>null</tt>, then the four characters <tt>"null"</tt> are
   *         appended to this text-output stream.
   *
   * @return  This writer
   *
   * @since JDK 1.5
   */
    @Override
    public XWriter append(CharSequence s) throws IOException {
        return print(s);
    }

    /**
  * Appends a subsequence of the specified character sequence to this
  * <tt>Appendable</tt>.
  * <p>
  * This method has been implemented for compatibility with the new <tt>Appendable</tt> interface. 
  * It simply redirects to {@link #print(CharSequence,int,int) print(s, fromIndex, toIndex - fromIndex)}.
  * </p>
  *
  * <p> An invocation of this method of the form <tt>out.append(csq, start,
  * end)</tt> when <tt>csq</tt> is not <tt>null</tt>, behaves in
  * exactly the same way as the invocation
  *
  * <pre>
  *     out.append(csq.subSequence(start, end)) </pre>
  *
  * @param  s
  *         The character sequence from which a subsequence will be
  *         appended.  If <tt>csq</tt> is <tt>null</tt>, then characters
  *         will be appended as if <tt>csq</tt> contained the four
  *         characters <tt>"null"</tt>.
  *
  * @param  fromIndex
  *         The index of the first character in the subsequence
  *
  * @param  toIndex
  *         The index of the character following the last character in the
  *         subsequence
  *
  * @return  A reference to this <tt>Appendable</tt>
  *
  * @throws  IndexOutOfBoundsException
  *          If <tt>start</tt> or <tt>end</tt> are negative, <tt>start</tt>
  *          is greater than <tt>end</tt>, or <tt>end</tt> is greater than
  *          <tt>csq.length()</tt>
  *
  * @throws  IOException
  *          If an I/O error occurs
  */
    @Override
    public XWriter append(CharSequence s, int fromIndex, int toIndex) throws IOException {
        CheckArg.range(s.length(), fromIndex, toIndex);
        return print(s, fromIndex, toIndex - fromIndex);
    }

    /**
   * Flushes this stream.  If the stream has saved any characters from the
   * various write() methods in a buffer, write them immediately to their
   * intended destination.  Then, if that destination is another character or
   * byte stream, flush it.  Thus one flush() invocation will flush all the
   * buffers in a chain of Writers and OutputStreams.
   * <p>
   * The default <tt>XWriter</tt> implementation of this method does nothing.
   * </p>
   *
   * @throws IOException if an I/O error occurs
   *
   * @since JDK 1.1
   */
    public void flush() throws IOException {
    }

    /**
   * Writes specified array of characters.
   * This method blocks until all specified data has been written or an I/O error occurs.
   * <p>
   * The default implementation of <tt>XWriter</tt> calls {@link #write(char[],int,int) write(source, 0, source.length)}.
   * </p>
   *
   * @param source the array of characters to be written.
   * 
   * @throws IOException if an I/O error occurs.
   *
   * @since JDK 1.1
   */
    @Override
    public void write(char[] source) throws IOException {
        write(source, 0, source.length);
    }

    /**
   * Writes specified subsequence of specified array of characters.
   * This method blocks until all specified data has been written or an I/O error occurs.
   * <p>
   * The default implementation of <tt>XWriter</tt> either redirects to {@link #write(CharBuffer) write(CharBuffer)} or uses {@link #write(int) write(char)}.
   * Subclasses are encouraged to overwrite this method with a more efficient implementation.
   * </p>
   *
   * @param source array of characters.
   * @param off    offset from which to start writing characters.
   * @param len    number of characters to write.
   *
   * @throws IndexOutOfBoundsException  if <tt>(off &lt; 0) || (len &lt; 0) || (off + len &gt; source.length)</tt>.
   * @throws IOException                if an I/O error occurs
   *
   * @since JDK 1.1
   */
    @Override
    public void write(char[] source, int off, int len) throws IOException {
        if (len < 16) {
            CheckIOArg.range(source, off, len);
            while (--len >= 0) write(source[off++]);
        } else write(CharBuffer.wrap(source, off, len));
    }

    /**
   * Writes all remaining characters of specified buffer to this stream.
   * This method blocks until all specified data has been written or an I/O error occurs.
   * When this method returns the specified buffer has no more remaining elements.
   * <p>
   * The default implementation of <tt>XWriter</tt> uses {@link #write(int) write(char)}.
   * Subclasses are encouraged to overwrite this method with a more efficient implementation.
   * </p>
   *
   * @return The initial value of <tt>source.remaining()</tt> <b>always</b>.
   *         The return value was implemented for compatibility with other interfaces - implementations are never allowed to return another value.  
   *
   * @param source the buffer whose content to write.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if <tt>source == null</tt>.
   *
   * @see #write(int)
   * @see CharBuffer#get()
   *
   * @since JaXLib 1.0
   */
    public int write(CharBuffer source) throws IOException {
        int count = source.remaining();
        if (source.hasArray()) {
            char[] a = source.array();
            int pos = source.position();
            int lim = pos + count;
            int ao = source.arrayOffset();
            int i = ao + pos;
            try {
                while (i < lim) {
                    write(a[i]);
                    i++;
                }
            } finally {
                source.position(i - ao);
            }
        } else {
            for (int rem = count; --rem >= 0; ) write(source.get());
        }
        return count;
    }

    /**
   * Writes the characters of specified string to this stream.
   * This method blocks until all specified data has been written or an I/O error occurs.
   * <p>
   * The default implementation of <tt>XWriter</tt> simply redirects to {@link #write(String,int,int) write(source, 0, source.length())}.
   * </p>
   *
   * @param source the characters to write.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if <tt>source == null</tt>.
   *
   * @since JDK 1.1
   */
    @Override
    public void write(String source) throws IOException {
        write(source, 0, source.length());
    }

    /**
   * Writes the characters of specified substring to this stream.
   * This method blocks until all specified data has been written or an I/O error occurs.
   * <p>
   * The default implementation of <tt>XWriter</tt> simply redirects to {@link #print(CharSequence,int,int) print(source, off, len)}.
   * </p>
   *
   * @param source the characters to write.
   * @param off    offset from which to start writing characters.
   * @param len    number of characters to write.
   *
   * @throws IndexOutOfBoundsException  if <tt>(off < 0) || (len < 0) || (off + len > source.length())</tt>.
   * @throws IOException                if an I/O error occurs.
   * @throws NullPointerException       if <tt>source == null</tt>.
   *
   * @since JDK 1.1
   */
    @Override
    public void write(String source, int off, int len) throws IOException {
        print(source, off, len);
    }

    /**
   * Same as {@link #write(char[]) write(v)} - implemented for convenience.
   * This method blocks until all specified data has been written or an I/O error occurs.
   *
   * @since JaXLib 1.0
   */
    public XWriter print(char[] v) throws IOException {
        print(v, 0, v.length);
        return this;
    }

    /**
   * Same as {@link #write(char[],int,int) write(v, off, len)} - implemented for convenience.
   * This method blocks until all specified data has been written or an I/O error occurs.
   *
   * @since JaXLib 1.0
   */
    public XWriter print(char[] v, int off, int len) throws IOException {
        write(v, off, len);
        return this;
    }

    /**
   * Writes the string representation of specified object to this stream.
   * This method blocks until all specified data has been written or an I/O error occurs.
   *
   * @param v the object whose string representation to write.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see String#valueOf(Object)
   * @see java.io.PrintWriter#print(Object)
   *
   * @since JaXLib 1.0
   */
    public XWriter print(Object v) throws IOException {
        if (v instanceof CharSequence) print((CharSequence) v); else write(String.valueOf(v));
        return this;
    }

    /**
   * Writes the specified sequence of characters to this stream.
   * This method blocks until all specified data has been written or an I/O error occurs.
   * <p>
   * This method has been implemented for convenience. 
   * It simply redirects to {@link #append(CharSequence) append(v)}.
   * </p>
   *
   * @param s the string to write; <tt>null</tt> causes "null" to be written. 
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see java.io.PrintWriter#print(String)
   *
   * @since JaXLib 1.0
   */
    public XWriter print(CharSequence s) throws IOException {
        if (s == null) write("null"); else print(s, 0, s.length());
        return this;
    }

    /**
   * Writes the specified subsequence of characters to this stream.
   * This method blocks until all specified data has been written or an I/O error occurs.
   *
   * @param s   the string to write; 
   * @param off offset from which to start writing characters.
   * @param len number of characters to write.
   *
   * @throws IndexOutOfBoundsException  if <tt>(off < 0) || (len < 0) || (off + len > s.length())</tt>.
   * @throws IOException                if an I/O error occurs.
   * @throws NullPointerException       if <tt>v == null</tt>.
   *
   * @since JaXLib 1.0
   */
    public XWriter print(CharSequence s, int off, int len) throws IOException {
        CheckIOArg.range(s.length(), off, len);
        if (len == 0) return this; else if (len == 1) write(s.charAt(0)); else if (s instanceof CharBuffer) {
            CharBuffer b = (CharBuffer) s;
            if (b.hasArray()) write(b.array(), b.arrayOffset() + b.position() + off, len); else {
                b = b.duplicate();
                b.position(b.position() + off);
                b.limit(b.position() + len);
                write(b);
            }
        } else write(CharBuffer.wrap(s, off, off + len));
        return this;
    }

    /**
   * Writes the string representation of specified value to this stream.
   *
   * @param v the value whose string representation to write.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see String#valueOf(boolean)
   * @see java.io.PrintWriter#print(boolean)
   *
   * @since JaXLib 1.0
   */
    public XWriter print(boolean v) throws IOException {
        write(Boolean.toString(v));
        return this;
    }

    /**
   * Same as {@link #write(int) write(v)} - implemented for convenience.
   *
   * @since JaXLib 1.0
   */
    public XWriter print(char v) throws IOException {
        write(v);
        return this;
    }

    /**
   * Writes the string representation of specified value to this stream.
   *
   * @param v the value whose string representation to write.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see String#valueOf(double)
   * @see java.io.PrintWriter#print(double)
   *
   * @since JaXLib 1.0
   */
    public XWriter print(double v) throws IOException {
        write(Double.toString(v));
        return this;
    }

    /**
   * Writes the string representation of specified value to this stream.
   *
   * @param v the value whose string representation to write.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see String#valueOf(float)
   * @see java.io.PrintWriter#print(float)
   *
   * @since JaXLib 1.0
   */
    public XWriter print(float v) throws IOException {
        write(Float.toString(v));
        return this;
    }

    /**
   * Writes the string representation of specified value to this stream, using radix <tt>10</tt>.
   *
   * @param v the value whose string representation to write.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see String#valueOf(int)
   * @see java.io.PrintWriter#print(int)
   *
   * @since JaXLib 1.0
   */
    public XWriter print(int v) throws IOException {
        print(v, 10);
        return this;
    }

    /**
   * Writes the string representation of specified value to this stream, using radix <tt>10</tt>.
   *
   * @param v the value whose string representation to write.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see String#valueOf(long)
   * @see java.io.PrintWriter#print(long)
   *
   * @since JaXLib 1.0
   */
    public XWriter print(long v) throws IOException {
        print(v, 10);
        return this;
    }

    /**
   * Writes the string representation of specified value to this stream.
   *
   * @param v the value whose string representation to write.
   * @param radix the radix to use to format the string.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see Integer#toString(int,int)
   *
   * @since JaXLib 1.0
   */
    public XWriter print(int v, int radix) throws IOException {
        SimpleIntegerFormat.getInstance(radix).writeTo(v, this);
        return this;
    }

    /**
   * Writes the string representation of specified value to this stream.
   *
   * @param v the value whose string representation to write.
   * @param radix the radix to use to format the string.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see Long#toString(long,int)
   *
   * @since JaXLib 1.0
   */
    public XWriter print(long v, int radix) throws IOException {
        SimpleIntegerFormat.getInstance(radix).writeTo(v, this);
        return this;
    }

    /**
   * Writes a line separator to this stream.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see java.io.PrintWriter#println()
   *
   * @since JaXLib 1.0
   */
    public XWriter println() throws IOException {
        write(SystemProperties.getLineSeparator());
        return this;
    }

    /**
   * Writes specified array of characters and terminates the line. 
   * This method behaves as though it invokes {@link #print(char[]) print(v)} and then {@link #println()}.
   *
   * @param v the array of characters to be written.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see java.io.PrintWriter#println(char[])
   *
   * @since JaXLib 1.0
   */
    public XWriter println(char[] v) throws IOException {
        println(v, 0, v.length);
        return this;
    }

    /**
   * Writes specified subsequence of specified array of characters and terminates the line. 
   * This method behaves as though it invokes {@link #print(char[]) print(v, off, len)} and then {@link #println()}.
   *
   * @param v      array of characters.
   * @param off    offset from which to start writing characters.
   * @param len    number of characters to write.
   *
   * @throws IndexOutOfBoundsException  if <tt>(off &lt; 0) || (len &lt; 0) || (off + len &gt; source.length)</tt>.
   * @throws IOException                if an I/O error occurs
   *
   * @since JaXLib 1.0
   */
    public XWriter println(char[] v, int off, int len) throws IOException {
        print(v, off, len);
        println();
        return this;
    }

    /**
   * Writes the string representation of specified object and terminates the line.
   * This method behaves as though it invokes {@link #print(Object) print(v)} and then {@link #println()}.
   *
   * @param v the object whose string representation to write.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see String#valueOf(Object)
   * @see java.io.PrintWriter#println(Object)
   *
   * @since JaXLib 1.0
   */
    public XWriter println(Object v) throws IOException {
        print(v);
        println();
        return this;
    }

    /**
   * Writes the specified sequence of characters to this stream and terminates the line.
   * This method behaves as though it invokes {@link #print(CharSequence) print(v)} and then {@link #println()}.
   *
   * @param v the string to write; <tt>null</tt> causes "null" to be written. 
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see java.io.PrintWriter#println(String)
   *
   * @since JaXLib 1.0
   */
    public XWriter println(CharSequence v) throws IOException {
        if (v == null) v = "null";
        print(v);
        println();
        return this;
    }

    /**
   * Writes the string representation of specified value and terminates the line.
   * This method behaves as though it invokes {@link #print(boolean) print(v)} and then {@link #println()}.
   *
   * @param v the value whose string representation to write.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see String#valueOf(boolean)
   * @see java.io.PrintWriter#println(boolean)
   *
   * @since JaXLib 1.0
   */
    public XWriter println(boolean v) throws IOException {
        print(v);
        println();
        return this;
    }

    /**
   * Writes the specified character and terminates the line.
   * This method behaves as though it invokes {@link #write(int) write(v)} and then {@link #println()}.
   *
   * @param v the character to write.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see java.io.PrintWriter#println(char)
   *
   * @since JaXLib 1.0
   */
    public XWriter println(char v) throws IOException {
        print(v);
        println();
        return this;
    }

    /**
   * Writes the string representation of specified value and terminates the line.
   * This method behaves as though it invokes {@link #print(double) print(v)} and then {@link #println()}.
   *
   * @param v the value whose string representation to write.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see String#valueOf(double)
   * @see java.io.PrintWriter#println(double)
   *
   * @since JaXLib 1.0
   */
    public XWriter println(double v) throws IOException {
        print(v);
        println();
        return this;
    }

    /**
   * Writes the string representation of specified value and terminates the line.
   * This method behaves as though it invokes {@link #print(float) print(v)} and then {@link #println()}.
   *
   * @param v the value whose string representation to write.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see String#valueOf(float)
   * @see java.io.PrintWriter#println(float)
   *
   * @since JaXLib 1.0
   */
    public XWriter println(float v) throws IOException {
        print(v);
        println();
        return this;
    }

    /**
   * Writes the string representation of specified value using radix 10, and terminates the line.
   * This method behaves as though it invokes {@link #print(int) print(v)} and then {@link #println()}.
   *
   * @param v the value whose string representation to write.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see String#valueOf(int)
   * @see java.io.PrintWriter#println(int)
   *
   * @since JaXLib 1.0
   */
    public XWriter println(int v) throws IOException {
        print(v);
        println();
        return this;
    }

    /**
   * Writes the string representation of specified value using radix 10, and terminates the line.
   * This method behaves as though it invokes {@link #print(long) print(long)} and then {@link #println()}.
   *
   * @param v the value whose string representation to write.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see String#valueOf(long)
   * @see java.io.PrintWriter#println(long)
   *
   * @since JaXLib 1.0
   */
    public XWriter println(long v) throws IOException {
        print(v);
        println();
        return this;
    }

    /**
   * Writes the string representation of specified value and terminates the line.
   * This method behaves as though it invokes {@link #print(int) print(v)} and then {@link #println()}.
   *
   * @param v the value whose string representation to write.
   * @param radix the radix to use to format the string.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see Integer#toString(int,int)
   * @see #println()
   *
   * @since JaXLib 1.0
   */
    public XWriter println(int v, int radix) throws IOException {
        print(v, radix);
        println();
        return this;
    }

    /**
   * Writes the string representation of specified value and terminates the line.
   * This method behaves as though it invokes {@link #print(long) print(long)} and then {@link #println()}.
   *
   * @param v the value whose string representation to write.
   * @param radix the radix to use to format the string.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see Long#toString(long,int)
   * @see #println()
   *
   * @since JaXLib 1.0
   */
    public XWriter println(long v, int radix) throws IOException {
        print(v, radix);
        println();
        return this;
    }

    /**
   * Writes a formatted string to this writer using the specified format
   * string and arguments.
   *
   * <p> The locale always used is the one returned by {@link
   * java.util.Locale#getDefault() Locale.getDefault()}, regardless of any
   * previous invocations of other formatting methods on this object.
   *
   * @return  This writer
   *
   * @param  format
   *         A format string as described in {@link java.util.Formatter Format string syntax}.
   *
   * @param  args
   *         Arguments referenced by the format specifiers in the format
   *         string.  If there are more arguments than format specifiers, the
   *         extra arguments are ignored.  The number of arguments is
   *         variable and may be zero.  The maximum number of arguments is
   *         limited by the maximum dimension of a Java array as defined by
   *         the <a href="http://java.sun.com/docs/books/vmspec/">Java
   *         Virtual Machine Specification</a>.  The behaviour on a
   *         <tt>null</tt> argument depends on the {@link java.util.Formatter conversion}.
   *
   * @throws  IOException
   *          If an I/O error occurs.
   *
   * @throws  IllegalFormatException
   *          If a format string contains an illegal syntax, a format
   *          specifier that is incompatible with the given arguments,
   *          insufficient arguments given the format string, or other
   *          illegal conditions.  For specification of all possible
   *          formatting errors, see the <a
   *          href="../util/Formatter.html#detail">Details</a> section of the
   *          Formatter class specification.
   *
   * @throws  NullPointerException
   *          If the <tt>format</tt> is <tt>null</tt>
   *
   * @see java.io.PrintWriter#printf(String,Object[])
   *
   * @since JaXLib 1.0
   */
    public XWriter printf(String format, Object... args) throws IOException {
        return printf(Locale.getDefault(), format, args);
    }

    /**
   * Writes a formatted string to this writer using the specified format
   * string and arguments.
   *
   * @return  This writer
   *
   * @param  l
   *         The {@linkplain java.util.Locale locale} to apply durring
   *         formatting.  If <tt>l</tt> is <tt>null</tt> then no localization
   *         is applied.
   *
   * @param  format
   *         A format string as described in {@link java.util.Formatter Format string syntax}.
   *
   * @param  args
   *         Arguments referenced by the format specifiers in the format
   *         string.  If there are more arguments than format specifiers, the
   *         extra arguments are ignored.  The number of arguments is
   *         variable and may be zero.  The maximum number of arguments is
   *         limited by the maximum dimension of a Java array as defined by
   *         the <a href="http://java.sun.com/docs/books/vmspec/">Java
   *         Virtual Machine Specification</a>.  The behaviour on a
   *         <tt>null</tt> argument depends on the {@link java.util.Formatter conversion}.
   *
   * @throws  IOException
   *          If an I/O error occurs.
   *
   * @throws  IllegalFormatException
   *          If a format string contains an illegal syntax, a format
   *          specifier that is incompatible with the given arguments,
   *          insufficient arguments given the format string, or other
   *          illegal conditions.  For specification of all possible
   *          formatting errors, see the <a
   *          href="../util/Formatter.html#detail">Details</a> section of the
   *          formatter class specification.
   *
   * @throws  NullPointerException
   *          If the <tt>format</tt> is <tt>null</tt>
   *
   * @see java.io.PrintWriter#printf(Locale,String,Object[])
   *
   * @since JaXLib 1.0
   */
    public XWriter printf(Locale l, String format, Object... args) throws IOException {
        if ((this.formatter == null) || (this.formatter.locale() != l)) this.formatter = new Formatter(this, l);
        this.formatter.format(l, format, args);
        IOException ex = this.formatter.ioException();
        if (ex != null) throw ex;
        return this;
    }

    /**
   * Transfers characters from specified input stream to this output stream.
   * This method blocks until the specified number of characters have been transferred, or the end of the 
   * input stream has been reached.
   *
   * @return the number of characters transferred.
   *
   * @param in       the source stream.
   * @param maxCount the maximum number of characters to be transferred; <tt>-1</tt> for no limit.
   *
   * @throws IllegalArgumentException if <tt>maxCount < -1</tt>.
   * @throws IOException              if some I/O error occurs.
   * @throws NullPointerException     if <tt>dest == null</tt>
   *
   * @since JaXLib 1.0
   */
    public long transferFrom(Readable in, long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return 0;
        long count = 0;
        if (in instanceof Reader) {
            Reader r = (Reader) in;
            while ((count < maxCount) || (maxCount < 0)) {
                int b = r.read();
                if (b < 0) {
                    break;
                } else {
                    write(b);
                    count++;
                }
            }
        } else {
            int bufSize = (maxCount < 0) ? 256 : (int) Math.min(maxCount, 256);
            CharBuffer buf = CharBuffer.allocate(256);
            while (((count < maxCount) || (maxCount < 0)) && (in.read(buf) >= 0)) {
                buf.flip();
                count += buf.position();
                write(buf.array(), 0, buf.position());
                buf.clear();
            }
        }
        return count;
    }

    final void writeSecurely(char[] source, int off, int len) throws IOException {
        CheckIOArg.range(source.length, off, len);
        if (len == 0) {
            if (!isOpen()) throw new IOException("stream closed");
        } else writeSecurelyImpl(source, off, len);
    }

    void writeSecurelyImpl(char[] source, int off, int len) throws IOException {
        if (len < 64) {
            for (int hi = off + len; off < hi; off++) write(source[off]);
        } else {
            int capacity = Math.min(1024, len);
            char[] a = new char[capacity];
            while (len > 0) {
                int step = Math.min(capacity, len);
                System.arraycopy(source, off, a, 0, step);
                write(a, 0, step);
                len -= step;
                off += step;
            }
        }
    }
}
