package jaxlib.io.stream.adapter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Formatter;
import java.util.Locale;
import jaxlib.io.stream.XWriter;
import jaxlib.jaxlib_private.io.CheckIOArg;
import jaxlib.util.CharSequences;

/**
 * Handles a standard {@link java.io.Writer} as a {@link jaxlib.io.stream.XWriter}.
 * <p>
 * All methods declared by the <tt>Writer</tt> class are directly delegated.
 * If the underlying <tt>Writer</tt> is instance of <tt>XWriter</tt>, then <tt>AdapterWriter</tt> 
 * also directly delegates <tt>XWriter</tt> specific calls. Print methods are delegated to 
 * <tt>PrintWriter</tt> if the underlying writer is instance of.
 * </p><p>
 * <i>Note:</i> A call to {@link #closeInstance()} closes the <tt>AdapterWriter</tt> but not the
 * stream it delegates to.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: AdapterWriter.java,v 1.3 2004/09/14 19:59:39 joerg_wassmer Exp $
 */
public class AdapterWriter extends XWriter {

    public static XWriter asXWriter(Writer delegate) {
        if (delegate instanceof XWriter) return (XWriter) delegate; else return new AdapterWriter(delegate);
    }

    private Formatter formatter;

    private Writer out;

    private PrintWriter pout;

    private XWriter xout;

    public AdapterWriter(Writer out) {
        super();
    }

    private void setOutImpl(Writer out) {
        this.formatter = null;
        this.out = out;
        if (out instanceof XWriter) this.xout = (XWriter) out;
        if (out instanceof PrintWriter) this.pout = (PrintWriter) out;
    }

    private Writer ensureOpen() throws ClosedChannelException {
        Writer out = this.out;
        if (out == null) throw new ClosedChannelException();
        return out;
    }

    /**
   * Returns the character output stream this <tt>AdapterWriter</tt> delegates to.
   *
   * @since JaXLib 1.0
   */
    protected Writer getOut() {
        return this.out;
    }

    /**
   * Sets the character output stream this <tt>AdapterWriter</tt> delegates to.
   *
   * @since JaXLib 1.0
   */
    protected void setOut(Writer out) throws IOException {
        setOutImpl(out);
    }

    @Override
    public void close() throws IOException {
        Writer out = this.out;
        if (out != null) {
            closeInstance();
            out.close();
        }
    }

    @Override
    public void closeInstance() throws IOException {
        try {
            flush();
        } finally {
            setOut(null);
        }
    }

    @Override
    public void flush() throws IOException {
        Writer out = this.out;
        if (out != null) out.flush();
    }

    @Override
    public boolean isOpen() {
        return (this.out != null) && ((this.xout == null) || this.xout.isOpen());
    }

    @Override
    public AdapterWriter append(char c) throws IOException {
        ensureOpen().append(c);
        return this;
    }

    @Override
    public AdapterWriter append(CharSequence s) throws IOException {
        ensureOpen().append(s);
        return this;
    }

    @Override
    public AdapterWriter append(CharSequence s, int fromIndex, int toIndex) throws IOException {
        ensureOpen().append(s, fromIndex, toIndex);
        return this;
    }

    @Override
    public long transferFrom(Readable in, long maxCount) throws IOException {
        if (this.xout != null) return this.xout.transferFrom(in, maxCount); else return super.transferFrom(in, maxCount);
    }

    @Override
    public void write(int b) throws IOException {
        ensureOpen().write(b);
    }

    @Override
    public void write(char[] source, int off, int len) throws IOException {
        ensureOpen().write(source, off, len);
    }

    @Override
    public void write(String source, int off, int len) throws IOException {
        ensureOpen().write(source, off, len);
    }

    @Override
    public AdapterWriter print(CharSequence v, int off, int len) throws IOException {
        Writer out = ensureOpen();
        if (out instanceof XWriter) ((XWriter) out).print(v, off, len); else {
            int vlen = v.length();
            CheckIOArg.range(vlen, off, len);
            if ((off == 0) && (vlen == len)) out.append(v); else out.append(v.subSequence(off, off + len));
        }
        return this;
    }

    @Override
    public AdapterWriter print(boolean v) throws IOException {
        if (this.pout != null) this.pout.print(v); else if (this.xout != null) this.xout.print(v); else super.print(v);
        return this;
    }

    @Override
    public AdapterWriter print(char v) throws IOException {
        if (this.pout != null) this.pout.print(v); else if (this.xout != null) this.xout.print(v); else super.print(v);
        return this;
    }

    @Override
    public AdapterWriter print(double v) throws IOException {
        if (this.pout != null) this.pout.print(v); else if (this.xout != null) this.xout.print(v); else super.print(v);
        return this;
    }

    @Override
    public AdapterWriter print(float v) throws IOException {
        if (this.pout != null) this.pout.print(v); else if (this.xout != null) this.xout.print(v); else super.print(v);
        return this;
    }

    @Override
    public AdapterWriter print(int v, int radix) throws IOException {
        if ((radix == 10) && (this.pout != null)) this.pout.print(v); else if (this.xout != null) this.xout.print(v, radix); else super.print(v);
        return this;
    }

    @Override
    public AdapterWriter print(long v, int radix) throws IOException {
        if ((radix == 10) && (this.pout != null)) this.pout.print(v); else if (this.xout != null) this.xout.print(v); else super.print(v);
        return this;
    }

    @Override
    public AdapterWriter println() throws IOException {
        if (this.pout != null) this.pout.println(); else if (this.xout != null) this.xout.println(); else super.println();
        return this;
    }

    @Override
    public AdapterWriter printf(Locale l, String format, Object... args) throws IOException {
        if (this.pout != null) this.pout.printf(l, format, args); else if (this.xout != null) this.xout.printf(l, format, args); else {
            if ((this.formatter == null) || (this.formatter.locale() != l)) this.formatter = new Formatter(ensureOpen(), l);
            this.formatter.format(l, format, args);
            IOException ex = this.formatter.ioException();
            if (ex != null) throw ex;
        }
        return this;
    }
}
