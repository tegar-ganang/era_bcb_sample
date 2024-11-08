package jaxlib.arc.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import jaxlib.jaxlib_private.util.ResourceTools;
import jaxlib.util.CheckArg;

/**
 * Java port of {@code ZLib inflate}.
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: Inflater.java 2858 2010-12-30 06:33:16Z joerg_wassmer $
 */
final class Inflater extends Engine {

    private static final int PRESET_DICT = 0x20;

    private static final int Z_DEFLATED = 8;

    private static final int MODE_METHOD = 0;

    private static final int MODE_FLAG = 1;

    private static final int MODE_DICT4 = 2;

    private static final int MODE_DICT3 = 3;

    private static final int MODE_DICT2 = 4;

    private static final int MODE_DICT1 = 5;

    private static final int MODE_DICT0 = 6;

    private static final int MODE_BLOCKS = 7;

    private static final int MODE_CHECK4 = 8;

    private static final int MODE_CHECK3 = 9;

    private static final int MODE_CHECK2 = 10;

    private static final int MODE_CHECK1 = 11;

    private static final int MODE_DONE = 12;

    private static final int MODE_BAD = 13;

    static final int[] INFLATE_MASK = { 0x00000000, 0x00000001, 0x00000003, 0x00000007, 0x0000000f, 0x0000001f, 0x0000003f, 0x0000007f, 0x000000ff, 0x000001ff, 0x000003ff, 0x000007ff, 0x00000fff, 0x00001fff, 0x00003fff, 0x00007fff, 0x0000ffff };

    private static final byte[] MARK = { 0, 0, (byte) 0xff, (byte) 0xff };

    int mode;

    int method;

    int was;

    int need;

    int marker;

    final boolean nowrap;

    final int wbits;

    final InfBlocks blocks;

    final byte[] next_in;

    int next_in_index;

    int avail_in;

    long total_in;

    int available;

    byte[] next_out_array;

    ByteBuffer next_out_buffer;

    OutputStream next_out_stream;

    int next_out_index;

    long avail_out;

    long total_out;

    int adler;

    Inflater(final int bufferSize, final boolean nowrap, final int windowSize) {
        super();
        CheckArg.between(windowSize, 8, 15, "windowSize");
        this.next_in = new byte[bufferSize];
        this.nowrap = nowrap;
        this.wbits = windowSize;
        this.blocks = new InfBlocks(this, nowrap ? null : this, 1 << windowSize);
        reset();
    }

    final boolean inflate(final boolean eof) throws IOException {
        final byte[] in = this.next_in;
        if (in == null) throw new InflaterException("Inflater input buffer is not set.");
        final int f = eof ? Z_BUF_ERROR : Z_OK;
        int r = Z_BUF_ERROR;
        try {
            LOOP: while (true) {
                switch(this.mode) {
                    case MODE_METHOD:
                        if (inflateMETHOD(in, f)) return true;
                        r = f;
                        continue LOOP;
                    case MODE_FLAG:
                        if (inflateFLAG(in, r, f)) return true;
                        r = f;
                        continue LOOP;
                    case MODE_DICT4:
                        inflateDICT4(in, r, f);
                        return true;
                    case MODE_DICT3:
                        inflateDICT3(in, r, f);
                        return true;
                    case MODE_DICT2:
                        inflateDICT2(in, r, f);
                        return true;
                    case MODE_DICT1:
                        inflateDICT1(in, r);
                        return true;
                    case MODE_DICT0:
                        throw inflateDICT0();
                    case MODE_BLOCKS:
                        return inflateBLOCKS(in, r, f);
                    case MODE_CHECK4:
                        return inflateCHECK4(in);
                    case MODE_CHECK3:
                        return inflateCHECK3(in);
                    case MODE_CHECK2:
                        return inflateCHECK2(in);
                    case MODE_CHECK1:
                        return inflateCHECK1(in);
                    case MODE_DONE:
                        return false;
                    case MODE_BAD:
                        throw new InflaterException("Data error");
                    default:
                        throw new InflaterException("Stream error");
                }
            }
        } catch (final InflaterException ex) {
            this.mode = MODE_BAD;
            throw ex;
        }
    }

    private boolean inflateBLOCKS(final byte[] in, int r, final int f) throws IOException {
        r = this.blocks.proc(this, r);
        if (r == Z_DATA_ERROR) {
            this.marker = 0;
            this.mode = MODE_BAD;
            throw new InflaterException("Malformed input.");
        }
        if (r == Z_OK) r = f;
        if (r != Z_STREAM_END) return true;
        this.blocks.reset(this);
        if (this.nowrap) {
            this.mode = MODE_DONE;
            return false;
        }
        this.mode = MODE_CHECK4;
        return inflateCHECK4(in);
    }

    private boolean inflateCHECK1(final byte[] in) throws InflaterException {
        if (this.avail_in == 0) return true;
        this.avail_in--;
        this.total_in++;
        this.need += (in[this.next_in_index++] & 0xff);
        if (this.was != this.need) {
            this.mode = MODE_BAD;
            this.marker = 5;
            throw new InflaterException("Incorrect data check");
        }
        this.mode = MODE_DONE;
        return false;
    }

    private boolean inflateCHECK2(final byte[] in) throws InflaterException {
        if (this.avail_in == 0) return true;
        this.avail_in--;
        this.total_in++;
        this.need += ((in[this.next_in_index++] & 0xff) << 8) & 0xff00;
        this.mode = MODE_CHECK1;
        return inflateCHECK1(in);
    }

    private boolean inflateCHECK3(final byte[] in) throws InflaterException {
        if (this.avail_in == 0) return true;
        this.avail_in--;
        this.total_in++;
        this.need += ((in[this.next_in_index++] & 0xff) << 16) & 0xff0000;
        this.mode = MODE_CHECK2;
        return inflateCHECK2(in);
    }

    private boolean inflateCHECK4(final byte[] in) throws InflaterException {
        if (this.avail_in == 0) return true;
        this.avail_in--;
        this.total_in++;
        this.need = ((in[this.next_in_index++] & 0xff) << 24) & 0xff000000;
        this.mode = MODE_CHECK3;
        return inflateCHECK3(in);
    }

    private InflaterException inflateDICT0() throws InflaterException {
        this.marker = 0;
        this.mode = MODE_BAD;
        throw new InflaterException("Dictionary missing in input.");
    }

    private void inflateDICT1(final byte[] in, final int r) throws InflaterException {
        if (this.avail_in == 0) {
            if (r == Z_BUF_ERROR) throw new InflaterException("Truncated input.");
        } else {
            this.avail_in--;
            this.total_in++;
            this.need += (in[this.next_in_index++] & 0xff);
            this.adler = this.need;
            this.mode = MODE_DICT0;
        }
    }

    private void inflateDICT2(final byte[] in, final int r, final int f) throws InflaterException {
        if (this.avail_in == 0) {
            if (r == Z_BUF_ERROR) throw new InflaterException("Truncated input.");
        } else {
            this.avail_in--;
            this.total_in++;
            this.need += ((in[this.next_in_index++] & 0xff) << 8) & 0xff00;
            this.mode = MODE_DICT1;
            inflateDICT1(in, f);
        }
    }

    private void inflateDICT3(final byte[] in, final int r, final int f) throws InflaterException {
        if (this.avail_in == 0) {
            if (r == Z_BUF_ERROR) throw new InflaterException("Truncated input.");
        } else {
            this.avail_in--;
            this.total_in++;
            this.need += ((in[this.next_in_index++] & 0xff) << 16) & 0xff0000;
            this.mode = MODE_DICT2;
            inflateDICT2(in, f, f);
        }
    }

    private void inflateDICT4(final byte[] in, final int r, final int f) throws InflaterException {
        if (this.avail_in == 0) {
            if (r == Z_BUF_ERROR) throw new InflaterException("Truncated input.");
        } else {
            this.avail_in--;
            this.total_in++;
            this.need = ((in[this.next_in_index++] & 0xff) << 24) & 0xff000000;
            this.mode = MODE_DICT3;
            inflateDICT3(in, f, f);
        }
    }

    private boolean inflateFLAG(final byte[] in, final int r, final int f) throws InflaterException {
        if (this.avail_in == 0) {
            if (r == Z_BUF_ERROR) throw new InflaterException("Truncated input.");
            return true;
        }
        this.avail_in--;
        this.total_in++;
        final int b = in[this.next_in_index++] & 0xff;
        if ((((this.method << 8) + b) % 31) != 0) {
            this.mode = MODE_BAD;
            this.marker = 5;
            throw new InflaterException("Incorrect header check.");
        }
        if ((b & PRESET_DICT) == 0) {
            this.mode = MODE_BLOCKS;
            return false;
        }
        this.mode = MODE_DICT4;
        inflateDICT4(in, f, f);
        return true;
    }

    private boolean inflateMETHOD(final byte[] in, final int r) throws InflaterException {
        if (this.avail_in == 0) throw new InflaterException("Inflater input buffer is empty.");
        this.avail_in--;
        this.total_in++;
        if (((this.method = in[this.next_in_index++]) & 0xf) != Z_DEFLATED) {
            this.mode = MODE_BAD;
            this.marker = 5;
            throw new InflaterException("Unknown compression method");
        }
        if ((this.method >> 4) + 8 > this.wbits) {
            this.mode = MODE_BAD;
            this.marker = 5;
            throw new InflaterException("Invalid window size");
        }
        this.mode = MODE_FLAG;
        return inflateFLAG(in, r, r);
    }

    final void reset() {
        this.next_out_array = null;
        this.next_out_buffer = null;
        this.next_out_stream = null;
        this.total_in = 0;
        this.total_out = 0;
        this.mode = this.nowrap ? MODE_BLOCKS : MODE_METHOD;
        this.blocks.reset(this);
        this.was = 0;
    }

    final int inflateSetDictionary(byte[] dictionary, int dictLength) throws InflaterException {
        int index = 0;
        int length = dictLength;
        if (this.mode != MODE_DICT0) throw new InflaterException("Illegal state");
        if (Engine.adler32(1, dictionary, 0, dictLength) != this.adler) throw new InflaterException("Checksum mismatch");
        this.adler = 1;
        if (length >= (1 << this.wbits)) {
            length = (1 << this.wbits) - 1;
            index = dictLength - length;
        }
        this.blocks.set_dictionary(dictionary, index, length);
        this.mode = MODE_BLOCKS;
        return Z_OK;
    }

    final int inflateSync() {
        if (this.mode != MODE_BAD) {
            this.mode = MODE_BAD;
            this.marker = 0;
        }
        int n = this.avail_in;
        if (n == 0) return Z_BUF_ERROR;
        int p = this.next_in_index;
        int m = this.marker;
        final byte[] in = this.next_in;
        while ((n != 0) && (m < 4)) {
            if (in[p] == MARK[m]) m++; else if (in[p] != 0) m = 0; else m = 4 - m;
            p++;
            n--;
        }
        this.total_in += p - this.next_in_index;
        this.next_in_index = p;
        this.avail_in = n;
        this.marker = m;
        if (m != 4) return Z_DATA_ERROR;
        final long r = this.total_in;
        final long w = this.total_out;
        reset();
        this.total_in = r;
        this.total_out = w;
        this.mode = MODE_BLOCKS;
        return Z_OK;
    }

    /**
   * Returns true if inflate is currently at the end of a block generated
   * by Z_SYNC_FLUSH or Z_FULL_FLUSH. This function is used by one PPP
   * implementation to provide an additional safety check. PPP uses Z_SYNC_FLUSH
   * but removes the length bytes of the resulting empty stored block. When
   * decompressing, PPP checks that at the end of input packet, inflate is
   * waiting for these length bytes.
   */
    final int inflateSyncPoint() throws InflaterException {
        if (this.blocks == null) throw new InflaterException("Disposed");
        return this.blocks.sync_point();
    }

    private static final class InfBlocks extends Object {

        private static final int MANY = 1440;

        private static final int TYPE = 0;

        private static final int LENS = 1;

        private static final int STORED = 2;

        private static final int TABLE = 3;

        private static final int BTREE = 4;

        private static final int DTREE = 5;

        private static final int CODES = 6;

        private static final int DRY = 7;

        private static final int DONE = 8;

        private static final int BAD = 9;

        /**
     * Table for deflate from PKZIP's appnote.txt.
     * Order of the bit length code lengths
     */
        static final int[] BORDER = { 16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15 };

        int mode;

        int left;

        int table;

        int index;

        int[] blens;

        final int[] bb = new int[1];

        final int[] tb = new int[1];

        final InfCodes codes = new InfCodes();

        int last;

        int bitk;

        int bitb;

        final int[] hufts;

        final byte[] window;

        final int end;

        int read;

        int write;

        final Object checkfn;

        int check;

        final InfTree inftree = new InfTree();

        InfBlocks(final Inflater z, final Object checkfn, final int w) {
            super();
            this.checkfn = checkfn;
            this.end = w;
            this.hufts = new int[MANY * 3];
            this.mode = TYPE;
            this.window = new byte[w];
            reset(z);
            z.was = 0;
        }

        /**
     * Copy as much as possible from the sliding window to the output area
     */
        final int inflate_flush(final Inflater z, int r) throws IOException {
            int p = z.next_out_index;
            int q = this.read;
            int n = (((q <= this.write) ? this.write : this.end) - q);
            if (n <= z.avail_out) z.available = 0; else {
                z.available = (int) (n - z.avail_out);
                n = (int) z.avail_out;
            }
            if ((n != 0) && (r == Z_BUF_ERROR)) r = Z_OK;
            z.avail_out -= n;
            z.total_out += n;
            if (this.checkfn != null) z.adler = this.check = Engine.adler32(this.check, this.window, q, n);
            inflate_flush0(z, q, p, n);
            p += n;
            q += n;
            if (q == end) {
                q = 0;
                if (write == end) write = 0;
                n = write - q;
                if (n <= z.avail_out) z.available = 0; else {
                    z.available = (int) (n - z.avail_out);
                    n = (int) z.avail_out;
                }
                if ((n != 0) && (r == Z_BUF_ERROR)) r = Z_OK;
                z.avail_out -= n;
                z.total_out += n;
                if (this.checkfn != null) z.adler = this.check = Engine.adler32(this.check, this.window, q, n);
                inflate_flush0(z, q, p, n);
                p += n;
                q += n;
            }
            z.next_out_index = p;
            this.read = q;
            return r;
        }

        final void inflate_flush0(final Inflater z, final int srcOffs, final int dstOffs, final int len) throws IOException {
            if (z.next_out_array != null) System.arraycopy(this.window, srcOffs, z.next_out_array, dstOffs, len); else if (z.next_out_buffer != null) z.next_out_buffer.put(this.window, srcOffs, len); else if (z.next_out_stream != null) z.next_out_stream.write(this.window, srcOffs, len);
        }

        @SuppressWarnings("fallthrough")
        final int proc(final Inflater z, int r) throws IOException {
            int p = z.next_in_index;
            int n = z.avail_in;
            int b = this.bitb;
            int k = this.bitk;
            int q = this.write;
            int m = ((q < this.read) ? (this.read - q - 1) : (end - q));
            String error = null;
            final byte[] in = z.next_in;
            LOOP: while (true) {
                switch(this.mode) {
                    case TYPE:
                        {
                            while (k < 3) {
                                if (n == 0) break LOOP;
                                r = Z_OK;
                                n--;
                                b |= (in[p++] & 0xff) << k;
                                k += 8;
                            }
                            int t = b & 7;
                            last = t & 1;
                            switch(t >>> 1) {
                                case 0:
                                    b >>>= 3;
                                    k -= 3;
                                    t = k & 7;
                                    b >>>= t;
                                    k -= t;
                                    this.mode = LENS;
                                    continue LOOP;
                                case 1:
                                    codes.init(InfTree.fixed_bl, InfTree.fixed_bd, InfTree.fixed_tl, 0, InfTree.fixed_td, 0);
                                    b >>>= 3;
                                    k -= 3;
                                    this.mode = CODES;
                                    continue LOOP;
                                case 2:
                                    b >>>= 3;
                                    k -= 3;
                                    this.mode = TABLE;
                                    continue LOOP;
                                case 3:
                                    error = "Invalid block type";
                                    b >>>= 3;
                                    k -= 3;
                                    r = Z_DATA_ERROR;
                                    this.mode = BAD;
                                    break LOOP;
                            }
                        }
                        continue LOOP;
                    case LENS:
                        {
                            while (k < 32) {
                                if (n == 0) break LOOP;
                                r = Z_OK;
                                n--;
                                b |= (in[p++] & 0xff) << k;
                                k += 8;
                            }
                            if ((((~b) >>> 16) & 0xffff) != (b & 0xffff)) {
                                error = "Invalid stored block lengths";
                                r = Z_DATA_ERROR;
                                this.mode = BAD;
                                break LOOP;
                            }
                            this.left = (b & 0xffff);
                            b = 0;
                            k = 0;
                            this.mode = (this.left != 0) ? STORED : ((this.last != 0) ? DRY : TYPE);
                        }
                        continue LOOP;
                    case STORED:
                        {
                            if (n == 0) break LOOP;
                            if (m == 0) {
                                if ((q == this.end) && (this.read != 0)) {
                                    q = 0;
                                    m = (q < this.read ? (this.read - q - 1) : (this.end - q));
                                }
                                if (m == 0) {
                                    this.write = q;
                                    r = inflate_flush(z, r);
                                    q = this.write;
                                    m = ((q < this.read) ? (this.read - q - 1) : (this.end - q));
                                    if ((q == this.end) && (this.read != 0)) {
                                        q = 0;
                                        m = ((q < this.read) ? (this.read - q - 1) : (this.end - q));
                                    }
                                    if (m == 0) break LOOP;
                                }
                            }
                            r = Z_OK;
                            int t = this.left;
                            if (t > n) t = n;
                            if (t > m) t = m;
                            System.arraycopy(in, p, this.window, q, t);
                            p += t;
                            n -= t;
                            q += t;
                            m -= t;
                            if ((this.left -= t) == 0) this.mode = (this.last != 0) ? DRY : TYPE;
                        }
                        continue LOOP;
                    case TABLE:
                        {
                            while (k < 14) {
                                if (n == 0) break LOOP;
                                r = Z_OK;
                                n--;
                                b |= (in[p++] & 0xff) << k;
                                k += 8;
                            }
                            int t = b & 0x3fff;
                            this.table = t;
                            if ((t & 0x1f) > 29 || ((t >> 5) & 0x1f) > 29) {
                                error = "Too many length or distance symbols";
                                r = Z_DATA_ERROR;
                                this.mode = BAD;
                                break LOOP;
                            }
                            t = 258 + (t & 0x1f) + ((t >> 5) & 0x1f);
                            int[] blens = this.blens;
                            if ((blens != null) && (blens.length >= t)) Arrays.fill(blens, 0, t, 0); else {
                                blens = null;
                                this.blens = null;
                                this.blens = new int[t];
                            }
                            b >>>= 14;
                            k -= 14;
                            this.index = 0;
                            this.mode = BTREE;
                        }
                    case BTREE:
                        {
                            final int[] blens = this.blens;
                            int index = this.index;
                            while (index < 4 + (this.table >>> 10)) {
                                while (k < 3) {
                                    if (n == 0) {
                                        this.index = index;
                                        break LOOP;
                                    }
                                    r = Z_OK;
                                    n--;
                                    b |= (in[p++] & 0xff) << k;
                                    k += 8;
                                }
                                blens[BORDER[index++]] = b & 7;
                                b >>>= 3;
                                k -= 3;
                            }
                            while (index < 19) blens[BORDER[index++]] = 0;
                            this.bb[0] = 7;
                            final int t = inftree.inflate_trees_bits(blens, this.bb, this.tb, this.hufts);
                            if (t != Z_OK) {
                                r = t;
                                if (r != Z_DATA_ERROR) r = Z_DATA_ERROR; else {
                                    this.blens = null;
                                    this.mode = BAD;
                                }
                                this.index = index;
                                break LOOP;
                            }
                            this.index = 0;
                            this.mode = DTREE;
                        }
                    case DTREE:
                        {
                            final int[] blens = this.blens;
                            int index = this.index;
                            while (true) {
                                int t = this.table;
                                if (index >= 258 + (t & 0x1f) + ((t >> 5) & 0x1f)) break;
                                t = this.bb[0];
                                while (k < t) {
                                    if (n == 0) {
                                        this.index = index;
                                        break LOOP;
                                    }
                                    r = Z_OK;
                                    n--;
                                    b |= (in[p++] & 0xff) << k;
                                    k += 8;
                                }
                                final int tb = this.tb[0];
                                t = this.hufts[(tb + (b & INFLATE_MASK[t])) * 3 + 1];
                                int c = this.hufts[(tb + (b & INFLATE_MASK[t])) * 3 + 2];
                                if (c < 16) {
                                    b >>>= t;
                                    k -= t;
                                    blens[index++] = c;
                                } else {
                                    int i = (c == 18) ? 7 : (c - 14);
                                    int j = (c == 18) ? 11 : 3;
                                    while (k < (t + i)) {
                                        if (n == 0) {
                                            this.index = index;
                                            break LOOP;
                                        }
                                        r = Z_OK;
                                        n--;
                                        b |= (in[p++] & 0xff) << k;
                                        k += 8;
                                    }
                                    b >>>= t;
                                    k -= t;
                                    j += b & INFLATE_MASK[i];
                                    b >>>= i;
                                    k -= i;
                                    i = index;
                                    t = this.table;
                                    if ((i + j > 258 + (t & 0x1f) + ((t >> 5) & 0x1f)) || (c == 16 && i < 1)) {
                                        error = "Invalid bit length repeat";
                                        r = Z_DATA_ERROR;
                                        this.mode = BAD;
                                        this.index = index;
                                        break LOOP;
                                    }
                                    c = (c == 16) ? blens[i - 1] : 0;
                                    do blens[i++] = c; while (--j != 0);
                                    index = i;
                                }
                            }
                            this.index = index;
                            this.tb[0] = -1;
                            final int[] bl = new int[] { 9 };
                            final int[] bd = new int[] { 6 };
                            final int[] tl = new int[1];
                            final int[] td = new int[1];
                            int t = this.table;
                            t = this.inftree.inflate_trees_dynamic(257 + (t & 0x1f), 1 + ((t >> 5) & 0x1f), blens, bl, bd, tl, td, this.hufts);
                            if (t != Z_OK) {
                                if (t == Z_DATA_ERROR) {
                                    this.blens = null;
                                    this.mode = BAD;
                                }
                                r = t;
                                break LOOP;
                            }
                            codes.init(bl[0], bd[0], this.hufts, tl[0], this.hufts, td[0]);
                            this.mode = CODES;
                        }
                    case CODES:
                        this.bitb = b;
                        this.bitk = k;
                        this.write = q;
                        z.avail_in = n;
                        z.total_in += p - z.next_in_index;
                        z.next_in_index = p;
                        if ((r = codes.proc(this, z, r)) != Z_STREAM_END) return inflate_flush(z, r);
                        r = Z_OK;
                        p = z.next_in_index;
                        n = z.avail_in;
                        b = this.bitb;
                        k = this.bitk;
                        q = this.write;
                        m = ((q < this.read) ? (this.read - q - 1) : (this.end - q));
                        if (this.last == 0) {
                            this.mode = TYPE;
                            continue LOOP;
                        }
                        mode = DRY;
                    case DRY:
                        this.write = q;
                        r = inflate_flush(z, r);
                        q = this.write;
                        m = ((q < this.read) ? (this.read - q - 1) : (this.end - q));
                        if (this.read != this.write) break LOOP;
                        this.mode = DONE;
                    case DONE:
                        r = Z_STREAM_END;
                        break LOOP;
                    case BAD:
                        r = Z_DATA_ERROR;
                        break LOOP;
                    default:
                        r = Z_STREAM_ERROR;
                        break LOOP;
                }
            }
            this.bitb = b;
            this.bitk = k;
            this.write = q;
            z.avail_in = n;
            z.total_in += p - z.next_in_index;
            z.next_in_index = p;
            r = inflate_flush(z, r);
            if (r <= Z_ERRNO) {
                if (r == Z_DATA_ERROR) throw new InflaterException((error == null) ? "data error" : error);
                if (r != Z_BUF_ERROR) throw new InflaterException("stream error (code " + r + ")");
            }
            return r;
        }

        final void reset(final Inflater z) {
            z.was = this.check;
            this.mode = TYPE;
            this.bitk = 0;
            this.bitb = 0;
            this.read = 0;
            this.write = 0;
            if (this.checkfn != null) z.adler = check = 1;
        }

        final void set_dictionary(final byte[] d, final int start, final int n) {
            System.arraycopy(d, start, this.window, 0, n);
            this.read = n;
            this.write = n;
        }

        /**
     * Returns true if inflate is currently at the end of a block generated
     * by Z_SYNC_FLUSH or Z_FULL_FLUSH.
     */
        final int sync_point() {
            return (this.mode == LENS) ? 1 : 0;
        }
    }

    private static final class InfCodes extends Object {

        static final int START = 0;

        static final int LEN = 1;

        static final int LENEXT = 2;

        static final int DIST = 3;

        static final int DISTEXT = 4;

        static final int COPY = 5;

        static final int LIT = 6;

        static final int WASH = 7;

        static final int END = 8;

        static final int BADCODE = 9;

        int mode;

        int len;

        int[] tree;

        int tree_index = 0;

        int need;

        int lit;

        int get;

        int dist;

        byte lbits;

        byte dbits;

        int[] ltree;

        int ltree_index;

        int[] dtree;

        int dtree_index;

        InfCodes() {
            super();
        }

        /**
     * Called with number of bytes left to write in window at least 258
     * (the maximum string length) and number of input bytes available
     * at least ten.  The ten bytes are six bytes for the longest length/
     * distance pair plus four bytes for overloading the bit buffer.
     */
        static int inflate_fast(final int bl, final int bd, final int[] tl, final int tl_index, final int[] td, final int td_index, final InfBlocks s, final Inflater z) throws InflaterException {
            int p = z.next_in_index;
            int n = z.avail_in;
            int b = s.bitb;
            int k = s.bitk;
            int q = s.write;
            int m = (q < s.read) ? (s.read - q - 1) : (s.end - q);
            final int ml = INFLATE_MASK[bl];
            final int md = INFLATE_MASK[bd];
            final byte[] in = z.next_in;
            final byte[] window = s.window;
            do {
                while (k < 20) {
                    n--;
                    b |= (in[p++] & 0xff) << k;
                    k += 8;
                }
                int t = b & ml;
                int[] tp = tl;
                int tp_index = tl_index;
                int tp_index_t_3 = (tp_index + t) * 3;
                int e = tp[tp_index_t_3];
                if ((e = tp[tp_index_t_3]) == 0) {
                    b >>= tp[tp_index_t_3 + 1];
                    k -= tp[tp_index_t_3 + 1];
                    window[q++] = (byte) tp[tp_index_t_3 + 2];
                    m--;
                    continue;
                }
                while (true) {
                    b >>= tp[tp_index_t_3 + 1];
                    k -= tp[tp_index_t_3 + 1];
                    if ((e & 16) != 0) {
                        e &= 15;
                        int c = tp[tp_index_t_3 + 2] + (b & INFLATE_MASK[e]);
                        b >>= e;
                        k -= e;
                        while (k < 15) {
                            n--;
                            b |= (in[p++] & 0xff) << k;
                            k += 8;
                        }
                        t = b & md;
                        tp = td;
                        tp_index = td_index;
                        tp_index_t_3 = (tp_index + t) * 3;
                        e = tp[tp_index_t_3];
                        while (true) {
                            b >>= tp[tp_index_t_3 + 1];
                            k -= tp[tp_index_t_3 + 1];
                            if ((e & 16) != 0) {
                                e &= 15;
                                while (k < e) {
                                    n--;
                                    b |= (in[p++] & 0xff) << k;
                                    k += 8;
                                }
                                int d = tp[tp_index_t_3 + 2] + (b & INFLATE_MASK[e]);
                                b >>= e;
                                k -= e;
                                m -= c;
                                int r;
                                if (q >= d) {
                                    r = q - d;
                                    window[q] = window[r];
                                    window[q + 1] = window[r + 1];
                                    q += 2;
                                    r += 2;
                                    c -= 2;
                                } else {
                                    r = q - d;
                                    do r += s.end; while (r < 0);
                                    e = s.end - r;
                                    if (c > e) {
                                        c -= e;
                                        do window[q++] = window[r++]; while (--e != 0);
                                        r = 0;
                                    }
                                }
                                if ((q > r) && (c > q - r)) {
                                    do window[q++] = window[r++]; while (--c != 0);
                                } else {
                                    System.arraycopy(window, r, window, q, c);
                                    q += c;
                                    r += c;
                                    c = 0;
                                }
                                break;
                            } else if ((e & 64) == 0) {
                                t += tp[tp_index_t_3 + 2] + (b & INFLATE_MASK[e]);
                                tp_index_t_3 = (tp_index + t) * 3;
                                e = tp[tp_index_t_3];
                            } else {
                                c = z.avail_in - n;
                                c = (k >> 3 < c) ? (k >> 3) : c;
                                n += c;
                                p -= c;
                                k -= c << 3;
                                s.bitb = b;
                                s.bitk = k;
                                z.avail_in = n;
                                z.total_in += p - z.next_in_index;
                                z.next_in_index = p;
                                s.write = q;
                                throw new InflaterException("Invalid distance code");
                            }
                        }
                        break;
                    }
                    if ((e & 64) == 0) {
                        t += tp[tp_index_t_3 + 2] + (b & INFLATE_MASK[e]);
                        tp_index_t_3 = (tp_index + t) * 3;
                        if ((e = tp[tp_index_t_3]) == 0) {
                            b >>= tp[tp_index_t_3 + 1];
                            k -= tp[tp_index_t_3 + 1];
                            window[q++] = (byte) tp[tp_index_t_3 + 2];
                            m--;
                            break;
                        }
                    } else if ((e & 32) != 0) {
                        int c = z.avail_in - n;
                        c = (k >> 3 < c) ? (k >> 3) : c;
                        p -= c;
                        s.bitb = b;
                        s.bitk = k - (c << 3);
                        z.avail_in = n + c;
                        z.total_in += p - z.next_in_index;
                        z.next_in_index = p;
                        s.write = q;
                        return Z_STREAM_END;
                    } else {
                        int c = z.avail_in - n;
                        c = (k >> 3 < c) ? (k >> 3) : c;
                        n += c;
                        p -= c;
                        k -= (c << 3);
                        s.bitb = b;
                        s.bitk = k;
                        z.avail_in = n;
                        z.total_in += p - z.next_in_index;
                        z.next_in_index = p;
                        s.write = q;
                        throw new InflaterException("Invalid literal/length code");
                    }
                }
            } while ((m >= 258) && (n >= 10));
            int c = z.avail_in - n;
            c = (k >> 3 < c) ? (k >> 3) : c;
            n += c;
            p -= c;
            k -= c << 3;
            s.bitb = b;
            s.bitk = k;
            z.avail_in = n;
            z.total_in += p - z.next_in_index;
            z.next_in_index = p;
            s.write = q;
            return Z_OK;
        }

        final void init(final int bl, final int bd, final int[] tl, final int tl_index, final int[] td, final int td_index) {
            this.mode = START;
            this.lbits = (byte) bl;
            this.dbits = (byte) bd;
            this.ltree = tl;
            this.ltree_index = tl_index;
            this.dtree = td;
            this.dtree_index = td_index;
            this.tree = null;
        }

        @SuppressWarnings("fallthrough")
        final int proc(final InfBlocks s, final Inflater z, int r) throws IOException {
            int p = z.next_in_index;
            int n = z.avail_in;
            int b = s.bitb;
            int k = s.bitk;
            int q = s.write;
            int m = (q < s.read) ? (s.read - q - 1) : (s.end - q);
            final byte[] in = z.next_in;
            LOOP: while (true) {
                switch(this.mode) {
                    case START:
                        if ((m >= 258) && (n >= 10)) {
                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.total_in += p - z.next_in_index;
                            z.next_in_index = p;
                            s.write = q;
                            r = inflate_fast(this.lbits, this.dbits, this.ltree, this.ltree_index, this.dtree, this.dtree_index, s, z);
                            p = z.next_in_index;
                            n = z.avail_in;
                            b = s.bitb;
                            k = s.bitk;
                            q = s.write;
                            m = (q < s.read) ? (s.read - q - 1) : (s.end - q);
                            if (r != Z_OK) {
                                mode = (r == Z_STREAM_END) ? WASH : BADCODE;
                                continue LOOP;
                            }
                        }
                        this.need = lbits;
                        this.tree = ltree;
                        this.tree_index = ltree_index;
                        this.mode = LEN;
                    case LEN:
                        {
                            final int j = this.need;
                            while (k < j) {
                                if (n == 0) break LOOP;
                                r = Z_OK;
                                n--;
                                b |= (in[p++] & 0xff) << k;
                                k += 8;
                            }
                            final int[] tree = this.tree;
                            final int tindex = (this.tree_index + (b & INFLATE_MASK[j])) * 3;
                            b >>>= tree[tindex + 1];
                            k -= tree[tindex + 1];
                            final int e = tree[tindex];
                            if (e == 0) {
                                this.lit = tree[tindex + 2];
                                this.mode = LIT;
                                continue LOOP;
                            } else if ((e & 16) != 0) {
                                this.get = e & 15;
                                this.len = tree[tindex + 2];
                                this.mode = LENEXT;
                                continue LOOP;
                            } else if ((e & 64) == 0) {
                                this.need = e;
                                this.tree_index = (tindex / 3) + tree[tindex + 2];
                                continue LOOP;
                            } else if ((e & 32) != 0) {
                                this.mode = WASH;
                                continue LOOP;
                            } else {
                                this.mode = BADCODE;
                                throw new InflaterException("Invalid literal/length code");
                            }
                        }
                    case LENEXT:
                        {
                            final int j = this.get;
                            while (k < j) {
                                if (n == 0) break LOOP;
                                r = Z_OK;
                                n--;
                                b |= (in[p++] & 0xff) << k;
                                k += 8;
                            }
                            this.len += (b & INFLATE_MASK[j]);
                            b >>= j;
                            k -= j;
                            this.need = dbits;
                            this.tree = dtree;
                            this.tree_index = dtree_index;
                            this.mode = DIST;
                        }
                    case DIST:
                        {
                            final int j = this.need;
                            while (k < j) {
                                if (n == 0) break LOOP;
                                r = Z_OK;
                                n--;
                                b |= (in[p++] & 0xff) << k;
                                k += 8;
                            }
                            final int[] tree = this.tree;
                            final int tindex = (this.tree_index + (b & INFLATE_MASK[j])) * 3;
                            b >>= tree[tindex + 1];
                            k -= tree[tindex + 1];
                            final int e = tree[tindex];
                            if ((e & 16) != 0) {
                                this.get = e & 15;
                                this.dist = tree[tindex + 2];
                                this.mode = DISTEXT;
                                continue LOOP;
                            } else if ((e & 64) == 0) {
                                this.need = e;
                                this.tree_index = (tindex / 3) + tree[tindex + 2];
                                continue LOOP;
                            } else {
                                this.mode = BADCODE;
                                throw new InflaterException("Invalid distance code");
                            }
                        }
                    case DISTEXT:
                        {
                            final int j = this.get;
                            while (k < j) {
                                if (n == 0) break LOOP;
                                r = Z_OK;
                                n--;
                                b |= (in[p++] & 0xff) << k;
                                k += 8;
                            }
                            this.dist += (b & INFLATE_MASK[j]);
                            b >>= j;
                            k -= j;
                            this.mode = COPY;
                        }
                    case COPY:
                        {
                            int f = q - this.dist;
                            while (f < 0) f += s.end;
                            while (this.len != 0) {
                                if (m == 0) {
                                    if ((q == s.end) && (s.read != 0)) {
                                        q = 0;
                                        m = (s.read > 0) ? (s.read - 1) : s.end;
                                    }
                                    if (m == 0) {
                                        s.write = q;
                                        r = s.inflate_flush(z, r);
                                        q = s.write;
                                        m = (q < s.read) ? (s.read - q - 1) : (s.end - q);
                                        if ((q == s.end) && (s.read != 0)) {
                                            q = 0;
                                            m = (s.read > 0) ? (s.read - 1) : s.end;
                                        }
                                        if (m == 0) break LOOP;
                                    }
                                }
                                s.window[q++] = s.window[f++];
                                m--;
                                if (f == s.end) f = 0;
                                this.len--;
                            }
                            this.mode = START;
                        }
                        continue LOOP;
                    case LIT:
                        if (m == 0) {
                            if ((q == s.end) && (s.read != 0)) {
                                q = 0;
                                m = (s.read > 0) ? (s.read - 1) : s.end;
                            }
                            if (m == 0) {
                                s.write = q;
                                r = s.inflate_flush(z, r);
                                q = s.write;
                                m = (q < s.read) ? (s.read - q - 1) : (s.end - q);
                                if ((q == s.end) && (s.read != 0)) {
                                    q = 0;
                                    m = (s.read > 0) ? (s.read - 1) : s.end;
                                }
                                if (m == 0) break LOOP;
                            }
                        }
                        r = Z_OK;
                        s.window[q++] = (byte) this.lit;
                        m--;
                        this.mode = START;
                        continue LOOP;
                    case WASH:
                        if (k > 7) {
                            k -= 8;
                            n++;
                            p--;
                        }
                        s.write = q;
                        r = s.inflate_flush(z, r);
                        q = s.write;
                        m = (q < s.read) ? (s.read - q - 1) : (s.end - q);
                        if (s.read != s.write) break LOOP;
                        this.mode = END;
                    case END:
                        r = Z_STREAM_END;
                        break LOOP;
                    default:
                        s.bitb = b;
                        s.bitk = k;
                        z.avail_in = n;
                        z.total_in += p - z.next_in_index;
                        z.next_in_index = p;
                        s.write = q;
                        if (this.mode == BADCODE) {
                            s.inflate_flush(z, Z_DATA_ERROR);
                            throw new InflaterException("Data error");
                        } else {
                            s.inflate_flush(z, Z_STREAM_ERROR);
                            throw new InflaterException("Malformed input");
                        }
                }
            }
            s.bitb = b;
            s.bitk = k;
            z.avail_in = n;
            z.total_in += p - z.next_in_index;
            z.next_in_index = p;
            s.write = q;
            return s.inflate_flush(z, r);
        }
    }

    private static final class InfTree extends Object {

        static final int MANY = 1440;

        static final int fixed_bl = 9;

        static final int fixed_bd = 5;

        static final int[] fixed_tl = new int[1536];

        static {
            String res = InfTree.class.getSimpleName() + ".fixedTL.csv";
            try {
                ResourceTools.readCsvFixedLengthIntArray(InfTree.class.getResourceAsStream(res), fixed_tl);
            } catch (final Exception ex) {
                throw new Error("Unable to load resource: " + res, ex);
            }
        }

        static final int[] fixed_td = new int[96];

        static {
            final String res = InfTree.class.getSimpleName() + ".fixedTD.csv";
            try {
                ResourceTools.readCsvFixedLengthIntArray(InfTree.class.getResourceAsStream(res), fixed_td);
            } catch (final Exception ex) {
                throw new Error("Unable to load resource: " + res, ex);
            }
        }

        /**
     * Tables for deflate from PKZIP's appnote.txt.
     * Copy lengths for literal codes 257..285
     */
        static final int[] cplens = { 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0 };

        /**
     * Extra bits for literal codes 257..285
     */
        static final int[] cplext = { 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0, 112, 112 };

        /**
     * Copy offsets for distance codes 0..29
     */
        static final int[] cpdist = { 1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577 };

        /**
     * Extra bits for distance codes
     */
        static final int[] cpdext = { 0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13 };

        static final int BMAX = 15;

        int[] hn;

        int[] v;

        int[] c;

        int[] r;

        int[] u;

        int[] x;

        /**
     * Given a list of code lengths and a maximum table size, make a set of
     * tables to decode that set of codes.  Return Z_OK on success, Z_BUF_ERROR
     * if the given code set is incomplete (the tables are still built in this
     * case), Z_DATA_ERROR if the input is invalid (an over-subscribed set of
     * lengths), or Z_MEM_ERROR if not enough memory.
     *
     * @param b
     *  code lengths in bits (all assumed <= BMAX)
     * @param n
     *  number of codes (assumed <= 288)
     * @param s
     *  number of simple-valued codes (0..s-1)
     * @param d
     *  list of base values for non-simple codes
     * @param e
     *  list of extra bits for non-simple codes
     * @param t
     *  result: starting table
     * @param m
     *  maximum lookup bits, returns actual
     * @param hp
     *  space for trees
     * @param hn
     *  hufts used in space
     * @param v
     *  working area: values in order of bit length
     */
        final int huft_build(final int[] b, final int bindex, int n, final int s, final int[] d, final int[] e, final int[] t, final int[] m, final int[] hp, final int[] hn, final int[] v) throws InflaterException {
            final int[] c = this.c;
            int p = 0;
            int i = n;
            do {
                c[b[bindex + p]]++;
                p++;
                i--;
            } while (i != 0);
            if (c[0] == n) {
                t[0] = -1;
                m[0] = 0;
                return Z_OK;
            }
            int l = m[0];
            int j;
            for (j = 1; j <= BMAX; j++) {
                if (c[j] != 0) break;
            }
            int k = j;
            if (l < j) l = j;
            for (i = BMAX; i != 0; i--) {
                if (c[i] != 0) break;
            }
            int g = i;
            if (l > i) l = i;
            m[0] = l;
            int y;
            for (y = 1 << j; j < i; j++, y <<= 1) {
                if ((y -= c[j]) < 0) throw new InflaterException("Data error");
            }
            if ((y -= c[i]) < 0) throw new InflaterException("Data error");
            c[i] += y;
            final int[] x = this.x;
            x[1] = j = 0;
            p = 1;
            int xp = 2;
            while (--i != 0) {
                x[xp] = (j += c[p]);
                xp++;
                p++;
            }
            i = 0;
            p = 0;
            do {
                if ((j = b[bindex + p]) != 0) v[x[j]++] = i;
                p++;
            } while (++i < n);
            final int[] r = this.r;
            final int[] u = this.u;
            n = x[g];
            x[0] = i = 0;
            p = 0;
            int h = -1;
            int w = -l;
            u[0] = 0;
            int q = 0;
            int z = 0;
            for (; k <= g; k++) {
                int a = c[k];
                while (a-- != 0) {
                    while (k > w + l) {
                        h++;
                        w += l;
                        z = g - w;
                        z = (z > l) ? l : z;
                        j = k - w;
                        int f = 1 << j;
                        if (f > a + 1) {
                            f -= a + 1;
                            xp = k;
                            if (j < z) {
                                while (++j < z) {
                                    if ((f <<= 1) <= c[++xp]) break;
                                    f -= c[xp];
                                }
                            }
                        }
                        z = 1 << j;
                        if (hn[0] + z > MANY) {
                            throw new InflaterException("Data error");
                        }
                        u[h] = q = hn[0];
                        hn[0] += z;
                        if (h == 0) t[0] = q; else {
                            x[h] = i;
                            r[0] = (byte) j;
                            r[1] = (byte) l;
                            j = i >>> (w - l);
                            r[2] = (q - u[h - 1] - j);
                            final int di = (u[h - 1] + j) * 3;
                            hp[di] = r[0];
                            hp[di + 1] = r[1];
                            hp[di + 2] = r[2];
                        }
                    }
                    r[1] = (byte) (k - w);
                    if (p >= n) {
                        r[0] = 128 + 64;
                    } else if (v[p] < s) {
                        r[0] = (byte) (v[p] < 256 ? 0 : 32 + 64);
                        r[2] = v[p++];
                    } else {
                        r[0] = (byte) (e[v[p] - s] + 16 + 64);
                        r[2] = d[v[p++] - s];
                    }
                    final int f = 1 << (k - w);
                    for (j = i >>> w; j < z; j += f) {
                        final int di = (q + j) * 3;
                        hp[di] = r[0];
                        hp[di + 1] = r[1];
                        hp[di + 2] = r[2];
                    }
                    for (j = 1 << (k - 1); (i & j) != 0; j >>>= 1) i ^= j;
                    i ^= j;
                    int mask = (1 << w) - 1;
                    while ((i & mask) != x[h]) {
                        h--;
                        w -= l;
                        mask = (1 << w) - 1;
                    }
                }
            }
            return (y != 0) && (g != 1) ? Z_BUF_ERROR : Z_OK;
        }

        final int inflate_trees_bits(final int[] c, final int[] bb, final int[] tb, final int[] hp) throws InflaterException {
            initWorkArea(19);
            this.hn[0] = 0;
            final int result = huft_build(c, 0, 19, 19, null, null, tb, bb, hp, hn, v);
            if (result == Z_DATA_ERROR) throw new InflaterException("Oversubscribed dynamic bit lengths tree");
            if (result == Z_BUF_ERROR || bb[0] == 0) throw new InflaterException("Incomplete dynamic bit lengths tree");
            return result;
        }

        final int inflate_trees_dynamic(final int nl, final int nd, final int[] c, final int[] bl, final int[] bd, final int[] tl, final int[] td, final int[] hp) throws InflaterException {
            initWorkArea(288);
            this.hn[0] = 0;
            int result = huft_build(c, 0, nl, 257, cplens, cplext, tl, bl, hp, hn, v);
            if ((result != Z_OK) || (bl[0] == 0)) {
                if (result == Z_DATA_ERROR) throw new InflaterException("Oversubscribed literal/length tree");
                if (result != Z_MEM_ERROR) throw new InflaterException("Incomplete literal/length tree");
                return result;
            }
            initWorkArea(288);
            result = huft_build(c, nl, nd, 0, cpdist, cpdext, td, bd, hp, hn, v);
            if ((result != Z_OK) || ((nl > 257) && (bd[0] == 0))) {
                if (result == Z_DATA_ERROR) throw new InflaterException("Oversubscribed distance tree");
                if (result == Z_BUF_ERROR) throw new InflaterException("Incomplete distance tree");
                if (result != Z_MEM_ERROR) throw new InflaterException("Empty distance tree with lengths");
                return result;
            }
            return Z_OK;
        }

        private void initWorkArea(final int vsize) {
            if (this.hn == null) {
                this.hn = new int[1];
                this.v = new int[vsize];
                this.c = new int[BMAX + 1];
                this.r = new int[3];
                this.u = new int[BMAX];
                this.x = new int[BMAX + 1];
            } else {
                if (this.v.length < vsize) this.v = new int[vsize]; else Arrays.fill(this.v, 0, vsize, 0);
                final int[] r = this.r;
                r[0] = 0;
                r[1] = 0;
                r[2] = 0;
                final int[] c = this.c;
                for (int i = BMAX + 1; --i >= 0; ) c[i] = 0;
                System.arraycopy(c, 0, this.u, 0, BMAX);
                System.arraycopy(c, 0, this.x, 0, BMAX + 1);
            }
        }
    }
}
