package ch.comtools.jsch.jzlib;

final class InfCodes {

    private static final int[] inflate_mask = { 0x00000000, 0x00000001, 0x00000003, 0x00000007, 0x0000000f, 0x0000001f, 0x0000003f, 0x0000007f, 0x000000ff, 0x000001ff, 0x000003ff, 0x000007ff, 0x00000fff, 0x00001fff, 0x00003fff, 0x00007fff, 0x0000ffff };

    private static final int Z_OK = 0;

    private static final int Z_STREAM_END = 1;

    private static final int Z_NEED_DICT = 2;

    private static final int Z_ERRNO = -1;

    private static final int Z_STREAM_ERROR = -2;

    private static final int Z_DATA_ERROR = -3;

    private static final int Z_MEM_ERROR = -4;

    private static final int Z_BUF_ERROR = -5;

    private static final int Z_VERSION_ERROR = -6;

    private static final int START = 0;

    private static final int LEN = 1;

    private static final int LENEXT = 2;

    private static final int DIST = 3;

    private static final int DISTEXT = 4;

    private static final int COPY = 5;

    private static final int LIT = 6;

    private static final int WASH = 7;

    private static final int END = 8;

    private static final int BADCODE = 9;

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
    }

    void init(int bl, int bd, int[] tl, int tl_index, int[] td, int td_index, ZStream z) {
        mode = START;
        lbits = (byte) bl;
        dbits = (byte) bd;
        ltree = tl;
        ltree_index = tl_index;
        dtree = td;
        dtree_index = td_index;
        tree = null;
    }

    int proc(InfBlocks s, ZStream z, int r) {
        int j;
        int[] t;
        int tindex;
        int e;
        int b = 0;
        int k = 0;
        int p = 0;
        int n;
        int q;
        int m;
        int f;
        p = z.next_in_index;
        n = z.avail_in;
        b = s.bitb;
        k = s.bitk;
        q = s.write;
        m = q < s.read ? s.read - q - 1 : s.end - q;
        while (true) {
            switch(mode) {
                case START:
                    if (m >= 258 && n >= 10) {
                        s.bitb = b;
                        s.bitk = k;
                        z.avail_in = n;
                        z.total_in += p - z.next_in_index;
                        z.next_in_index = p;
                        s.write = q;
                        r = inflate_fast(lbits, dbits, ltree, ltree_index, dtree, dtree_index, s, z);
                        p = z.next_in_index;
                        n = z.avail_in;
                        b = s.bitb;
                        k = s.bitk;
                        q = s.write;
                        m = q < s.read ? s.read - q - 1 : s.end - q;
                        if (r != Z_OK) {
                            mode = r == Z_STREAM_END ? WASH : BADCODE;
                            break;
                        }
                    }
                    need = lbits;
                    tree = ltree;
                    tree_index = ltree_index;
                    mode = LEN;
                case LEN:
                    j = need;
                    while (k < (j)) {
                        if (n != 0) r = Z_OK; else {
                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.total_in += p - z.next_in_index;
                            z.next_in_index = p;
                            s.write = q;
                            return s.inflate_flush(z, r);
                        }
                        n--;
                        b |= (z.next_in[p++] & 0xff) << k;
                        k += 8;
                    }
                    tindex = (tree_index + (b & inflate_mask[j])) * 3;
                    b >>>= (tree[tindex + 1]);
                    k -= (tree[tindex + 1]);
                    e = tree[tindex];
                    if (e == 0) {
                        lit = tree[tindex + 2];
                        mode = LIT;
                        break;
                    }
                    if ((e & 16) != 0) {
                        get = e & 15;
                        len = tree[tindex + 2];
                        mode = LENEXT;
                        break;
                    }
                    if ((e & 64) == 0) {
                        need = e;
                        tree_index = tindex / 3 + tree[tindex + 2];
                        break;
                    }
                    if ((e & 32) != 0) {
                        mode = WASH;
                        break;
                    }
                    mode = BADCODE;
                    z.msg = "invalid literal/length code";
                    r = Z_DATA_ERROR;
                    s.bitb = b;
                    s.bitk = k;
                    z.avail_in = n;
                    z.total_in += p - z.next_in_index;
                    z.next_in_index = p;
                    s.write = q;
                    return s.inflate_flush(z, r);
                case LENEXT:
                    j = get;
                    while (k < (j)) {
                        if (n != 0) r = Z_OK; else {
                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.total_in += p - z.next_in_index;
                            z.next_in_index = p;
                            s.write = q;
                            return s.inflate_flush(z, r);
                        }
                        n--;
                        b |= (z.next_in[p++] & 0xff) << k;
                        k += 8;
                    }
                    len += (b & inflate_mask[j]);
                    b >>= j;
                    k -= j;
                    need = dbits;
                    tree = dtree;
                    tree_index = dtree_index;
                    mode = DIST;
                case DIST:
                    j = need;
                    while (k < (j)) {
                        if (n != 0) r = Z_OK; else {
                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.total_in += p - z.next_in_index;
                            z.next_in_index = p;
                            s.write = q;
                            return s.inflate_flush(z, r);
                        }
                        n--;
                        b |= (z.next_in[p++] & 0xff) << k;
                        k += 8;
                    }
                    tindex = (tree_index + (b & inflate_mask[j])) * 3;
                    b >>= tree[tindex + 1];
                    k -= tree[tindex + 1];
                    e = (tree[tindex]);
                    if ((e & 16) != 0) {
                        get = e & 15;
                        dist = tree[tindex + 2];
                        mode = DISTEXT;
                        break;
                    }
                    if ((e & 64) == 0) {
                        need = e;
                        tree_index = tindex / 3 + tree[tindex + 2];
                        break;
                    }
                    mode = BADCODE;
                    z.msg = "invalid distance code";
                    r = Z_DATA_ERROR;
                    s.bitb = b;
                    s.bitk = k;
                    z.avail_in = n;
                    z.total_in += p - z.next_in_index;
                    z.next_in_index = p;
                    s.write = q;
                    return s.inflate_flush(z, r);
                case DISTEXT:
                    j = get;
                    while (k < (j)) {
                        if (n != 0) r = Z_OK; else {
                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.total_in += p - z.next_in_index;
                            z.next_in_index = p;
                            s.write = q;
                            return s.inflate_flush(z, r);
                        }
                        n--;
                        b |= (z.next_in[p++] & 0xff) << k;
                        k += 8;
                    }
                    dist += (b & inflate_mask[j]);
                    b >>= j;
                    k -= j;
                    mode = COPY;
                case COPY:
                    f = q - dist;
                    while (f < 0) {
                        f += s.end;
                    }
                    while (len != 0) {
                        if (m == 0) {
                            if (q == s.end && s.read != 0) {
                                q = 0;
                                m = q < s.read ? s.read - q - 1 : s.end - q;
                            }
                            if (m == 0) {
                                s.write = q;
                                r = s.inflate_flush(z, r);
                                q = s.write;
                                m = q < s.read ? s.read - q - 1 : s.end - q;
                                if (q == s.end && s.read != 0) {
                                    q = 0;
                                    m = q < s.read ? s.read - q - 1 : s.end - q;
                                }
                                if (m == 0) {
                                    s.bitb = b;
                                    s.bitk = k;
                                    z.avail_in = n;
                                    z.total_in += p - z.next_in_index;
                                    z.next_in_index = p;
                                    s.write = q;
                                    return s.inflate_flush(z, r);
                                }
                            }
                        }
                        s.window[q++] = s.window[f++];
                        m--;
                        if (f == s.end) f = 0;
                        len--;
                    }
                    mode = START;
                    break;
                case LIT:
                    if (m == 0) {
                        if (q == s.end && s.read != 0) {
                            q = 0;
                            m = q < s.read ? s.read - q - 1 : s.end - q;
                        }
                        if (m == 0) {
                            s.write = q;
                            r = s.inflate_flush(z, r);
                            q = s.write;
                            m = q < s.read ? s.read - q - 1 : s.end - q;
                            if (q == s.end && s.read != 0) {
                                q = 0;
                                m = q < s.read ? s.read - q - 1 : s.end - q;
                            }
                            if (m == 0) {
                                s.bitb = b;
                                s.bitk = k;
                                z.avail_in = n;
                                z.total_in += p - z.next_in_index;
                                z.next_in_index = p;
                                s.write = q;
                                return s.inflate_flush(z, r);
                            }
                        }
                    }
                    r = Z_OK;
                    s.window[q++] = (byte) lit;
                    m--;
                    mode = START;
                    break;
                case WASH:
                    if (k > 7) {
                        k -= 8;
                        n++;
                        p--;
                    }
                    s.write = q;
                    r = s.inflate_flush(z, r);
                    q = s.write;
                    m = q < s.read ? s.read - q - 1 : s.end - q;
                    if (s.read != s.write) {
                        s.bitb = b;
                        s.bitk = k;
                        z.avail_in = n;
                        z.total_in += p - z.next_in_index;
                        z.next_in_index = p;
                        s.write = q;
                        return s.inflate_flush(z, r);
                    }
                    mode = END;
                case END:
                    r = Z_STREAM_END;
                    s.bitb = b;
                    s.bitk = k;
                    z.avail_in = n;
                    z.total_in += p - z.next_in_index;
                    z.next_in_index = p;
                    s.write = q;
                    return s.inflate_flush(z, r);
                case BADCODE:
                    r = Z_DATA_ERROR;
                    s.bitb = b;
                    s.bitk = k;
                    z.avail_in = n;
                    z.total_in += p - z.next_in_index;
                    z.next_in_index = p;
                    s.write = q;
                    return s.inflate_flush(z, r);
                default:
                    r = Z_STREAM_ERROR;
                    s.bitb = b;
                    s.bitk = k;
                    z.avail_in = n;
                    z.total_in += p - z.next_in_index;
                    z.next_in_index = p;
                    s.write = q;
                    return s.inflate_flush(z, r);
            }
        }
    }

    void free(ZStream z) {
    }

    int inflate_fast(int bl, int bd, int[] tl, int tl_index, int[] td, int td_index, InfBlocks s, ZStream z) {
        int t;
        int[] tp;
        int tp_index;
        int e;
        int b;
        int k;
        int p;
        int n;
        int q;
        int m;
        int ml;
        int md;
        int c;
        int d;
        int r;
        int tp_index_t_3;
        p = z.next_in_index;
        n = z.avail_in;
        b = s.bitb;
        k = s.bitk;
        q = s.write;
        m = q < s.read ? s.read - q - 1 : s.end - q;
        ml = inflate_mask[bl];
        md = inflate_mask[bd];
        do {
            while (k < (20)) {
                n--;
                b |= (z.next_in[p++] & 0xff) << k;
                k += 8;
            }
            t = b & ml;
            tp = tl;
            tp_index = tl_index;
            tp_index_t_3 = (tp_index + t) * 3;
            if ((e = tp[tp_index_t_3]) == 0) {
                b >>= (tp[tp_index_t_3 + 1]);
                k -= (tp[tp_index_t_3 + 1]);
                s.window[q++] = (byte) tp[tp_index_t_3 + 2];
                m--;
                continue;
            }
            do {
                b >>= (tp[tp_index_t_3 + 1]);
                k -= (tp[tp_index_t_3 + 1]);
                if ((e & 16) != 0) {
                    e &= 15;
                    c = tp[tp_index_t_3 + 2] + ((int) b & inflate_mask[e]);
                    b >>= e;
                    k -= e;
                    while (k < (15)) {
                        n--;
                        b |= (z.next_in[p++] & 0xff) << k;
                        k += 8;
                    }
                    t = b & md;
                    tp = td;
                    tp_index = td_index;
                    tp_index_t_3 = (tp_index + t) * 3;
                    e = tp[tp_index_t_3];
                    do {
                        b >>= (tp[tp_index_t_3 + 1]);
                        k -= (tp[tp_index_t_3 + 1]);
                        if ((e & 16) != 0) {
                            e &= 15;
                            while (k < (e)) {
                                n--;
                                b |= (z.next_in[p++] & 0xff) << k;
                                k += 8;
                            }
                            d = tp[tp_index_t_3 + 2] + (b & inflate_mask[e]);
                            b >>= (e);
                            k -= (e);
                            m -= c;
                            if (q >= d) {
                                r = q - d;
                                if (q - r > 0 && 2 > (q - r)) {
                                    s.window[q++] = s.window[r++];
                                    s.window[q++] = s.window[r++];
                                    c -= 2;
                                } else {
                                    System.arraycopy(s.window, r, s.window, q, 2);
                                    q += 2;
                                    r += 2;
                                    c -= 2;
                                }
                            } else {
                                r = q - d;
                                do {
                                    r += s.end;
                                } while (r < 0);
                                e = s.end - r;
                                if (c > e) {
                                    c -= e;
                                    if (q - r > 0 && e > (q - r)) {
                                        do {
                                            s.window[q++] = s.window[r++];
                                        } while (--e != 0);
                                    } else {
                                        System.arraycopy(s.window, r, s.window, q, e);
                                        q += e;
                                        r += e;
                                        e = 0;
                                    }
                                    r = 0;
                                }
                            }
                            if (q - r > 0 && c > (q - r)) {
                                do {
                                    s.window[q++] = s.window[r++];
                                } while (--c != 0);
                            } else {
                                System.arraycopy(s.window, r, s.window, q, c);
                                q += c;
                                r += c;
                                c = 0;
                            }
                            break;
                        } else if ((e & 64) == 0) {
                            t += tp[tp_index_t_3 + 2];
                            t += (b & inflate_mask[e]);
                            tp_index_t_3 = (tp_index + t) * 3;
                            e = tp[tp_index_t_3];
                        } else {
                            z.msg = "invalid distance code";
                            c = z.avail_in - n;
                            c = (k >> 3) < c ? k >> 3 : c;
                            n += c;
                            p -= c;
                            k -= c << 3;
                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.total_in += p - z.next_in_index;
                            z.next_in_index = p;
                            s.write = q;
                            return Z_DATA_ERROR;
                        }
                    } while (true);
                    break;
                }
                if ((e & 64) == 0) {
                    t += tp[tp_index_t_3 + 2];
                    t += (b & inflate_mask[e]);
                    tp_index_t_3 = (tp_index + t) * 3;
                    if ((e = tp[tp_index_t_3]) == 0) {
                        b >>= (tp[tp_index_t_3 + 1]);
                        k -= (tp[tp_index_t_3 + 1]);
                        s.window[q++] = (byte) tp[tp_index_t_3 + 2];
                        m--;
                        break;
                    }
                } else if ((e & 32) != 0) {
                    c = z.avail_in - n;
                    c = (k >> 3) < c ? k >> 3 : c;
                    n += c;
                    p -= c;
                    k -= c << 3;
                    s.bitb = b;
                    s.bitk = k;
                    z.avail_in = n;
                    z.total_in += p - z.next_in_index;
                    z.next_in_index = p;
                    s.write = q;
                    return Z_STREAM_END;
                } else {
                    z.msg = "invalid literal/length code";
                    c = z.avail_in - n;
                    c = (k >> 3) < c ? k >> 3 : c;
                    n += c;
                    p -= c;
                    k -= c << 3;
                    s.bitb = b;
                    s.bitk = k;
                    z.avail_in = n;
                    z.total_in += p - z.next_in_index;
                    z.next_in_index = p;
                    s.write = q;
                    return Z_DATA_ERROR;
                }
            } while (true);
        } while (m >= 258 && n >= 10);
        c = z.avail_in - n;
        c = (k >> 3) < c ? k >> 3 : c;
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
}
