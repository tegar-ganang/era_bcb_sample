package alto.lang;

import alto.sys.IO;

/**
 * <p> The operation of this class is normal or special.  Normal
 * operation is as the filter input stream.  Special operation limits
 * the number of bytes read from this stream by a number given to the
 * {@link #set(int)} or {@link #reset(int)} methods.  </p>
 * 
 * <h3>Operation</h3>
 * 
 * <p> With no call into {@link #set(int)} or {@link #reset(int)} this
 * class operates like a normal filter input stream.  </p>
 * 
 * <p> With a call into {@link #set(int)} or {@link #reset(int)} this
 * class operates to limit the number of bytes read from this filter
 * input stream.  </p>
 * 
 * @author jdp
 * @since 1.5
 */
public abstract class ConsumptionInputStream extends java.io.FilterInputStream implements alto.sys.IO.Filter.Source {

    protected final Trace trace = new Trace();

    private boolean traceread, tracereadLast;

    private int tracereadStack;

    private long consume = -1L, count;

    public ConsumptionInputStream(java.io.InputStream in) {
        super(in);
    }

    public IO.Edge getIOEdge() {
        return null;
    }

    public final java.io.InputStream getFilterSource() {
        return this.in;
    }

    /**
     * Current
     */
    public boolean isTraceread() {
        return (this.traceread);
    }

    /**
     * Toggle
     */
    public void traceread() {
        this.traceread = (!this.traceread);
    }

    /**
     * Enable
     */
    public void traceread(boolean enable) {
        this.traceread = enable;
    }

    /**
     * Push
     */
    public void enterTraceread() {
        if (0 == this.tracereadStack) this.tracereadLast = this.traceread;
        this.traceread = true;
        this.tracereadStack += 1;
    }

    /**
     * Pop
     */
    public void exitTraceread() {
        this.tracereadStack -= 1;
        if (0 == this.tracereadStack) this.traceread = this.tracereadLast; else if (0 > this.tracereadStack) this.tracereadStack = 0;
    }

    /**
     * Set or reset the special operation of this class.  This call
     * resets the count of consumable bytes to zero, and defines the
     * number of consumable bytes to the argument.
     * 
     * @param length Use negative one to turn off special operation,
     * zero for an empty filter, or a positive value to limit
     * consumable bytes to this many.
     */
    public final void set(long length) throws java.io.IOException {
        synchronized (this) {
            if (0L > length) this.consume = -1L; else this.consume = length;
            this.count = 0L;
        }
    }

    public final void consume() throws java.io.IOException {
        int skip = this.consumable();
        if (0 < skip) {
            if (this.traceread) {
                byte[] skipb = new byte[skip];
                this.read(skipb, 0, skip);
            } else this.skip(skip);
        }
    }

    /**
     * @return In consumption operation, this method returns the
     * number of consumable bytes as from {@link #consumable()}.
     * Otherwise this method returns the number of available bytes
     * from the underlying input stream.
     */
    public final int available() throws java.io.IOException {
        long consume = this.consume;
        if (-1L < consume) {
            long avail = (consume - this.count);
            return (int) (avail & 0x7fffffff);
        } else return this.in.available();
    }

    public final boolean isConsuming() {
        return (-1L != this.consume);
    }

    public final boolean isNotConsuming() {
        return (-1L == this.consume);
    }

    /**
     * @return Negative one for normal filter operation, or zero or
     * more for consumption operation.  The return value has an upper
     * bound of integer max value.
     */
    public final int consumable() throws java.io.IOException {
        long consume = this.consume;
        if (-1L < consume) {
            long avail = (consume - this.count);
            return (int) (avail & 0x7fffffff);
        } else return -1;
    }

    public final int read() throws java.io.IOException {
        if (this.isNotConsuming()) {
            if (this.traceread) {
                int ch = this.in.read();
                this.trace.write(ch);
                return ch;
            } else return this.in.read();
        } else if (0 < this.consumable()) {
            this.count += 1;
            if (this.traceread) {
                int ch = this.in.read();
                this.trace.write(ch);
                return ch;
            } else return this.in.read();
        } else return -1;
    }

    public final int read(byte buf[], int ofs, int len) throws java.io.IOException {
        if (this.isConsuming()) {
            int avail = this.consumable();
            if (0 < avail) {
                int many = java.lang.Math.min(avail, len);
                int read = this.in.read(buf, ofs, many);
                if (0 < read) {
                    if (this.traceread) {
                        this.trace.write(buf, ofs, read);
                    }
                    this.count += read;
                }
                return read;
            } else return -1;
        } else return this.in.read(buf, ofs, len);
    }

    public final long skip(long n) throws java.io.IOException {
        if (this.isConsuming()) {
            long skip = this.consumable();
            if (n > skip) {
                if (0 < skip) {
                    this.count += skip;
                    return this.in.skip(skip);
                } else return 0L;
            }
        }
        return this.in.skip(n);
    }

    /**
     * @return Read available bytes.
     */
    public final byte[] readAvailable() throws java.io.IOException {
        int count = this.available();
        if (0 < count) {
            byte[] buf = new byte[count];
            this.read(buf, 0, count);
            return buf;
        } else return null;
    }

    /**
     * @return Read consumable bytes.
     */
    public final byte[] readConsumable() throws java.io.IOException {
        int count = this.consumable();
        if (0 < count) return this.readMany(count); else return null;
    }

    /**
     * This method fills a new byte array with a number of bytes.
     * 
     * @param many Length of return buffer to read, as from HTTP
     * Content Length.
     * @return New array read from input, filled with 'many' bytes.
     * Otherwise an array with as many bytes as could be consumed.  An
     * empty array is never returned, but rather null.
     */
    public final byte[] readMany(int many) throws java.io.IOException {
        if (0 < many) {
            byte[] buf = new byte[many];
            int len = many, ofs = 0, read;
            while (0 < (read = this.read(buf, ofs, len))) {
                ofs += read;
                len -= read;
                if (1 > len) return buf;
            }
            if (0 < ofs) {
                byte[] copier = new byte[ofs];
                java.lang.System.arraycopy(buf, 0, copier, 0, ofs);
                return copier;
            }
        }
        return null;
    }

    public final int copyTo(alto.io.Output out) throws java.io.IOException {
        byte[] iob = this.readConsumable();
        if (null != iob) {
            int length = iob.length;
            out.write(iob, 0, length);
            return length;
        } else return 0;
    }
}
