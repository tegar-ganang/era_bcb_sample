package orbgate;

import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.Any;

class CDRInputStream extends org.omg.CORBA.portable.InputStream {

    /**
     * The ORB we belong to
     */
    private org.omg.CORBA.ORB orb_;

    /**
     * Call context to release when we're done
     */
    private ORBCall call_context_;

    /**
     * Number of bytes read so far (alignment counter)
     */
    private int nrd;

    /**
     * Remaining bytes
     */
    private int remain;

    /**
     * Byte order
     */
    private int byte_order;

    /**
     * Block we are reading from
     */
    private MsgBlk current_block;

    /**
     * Offset in the current_block
     */
    private int current_pos;

    public CDRInputStream(org.omg.CORBA.ORB orb) {
        orb_ = orb;
        current_block = null;
        remain = 0;
        current_pos = 0;
        nrd = 0;
        byte_order = CDR.BIG_ENDIAN;
    }

    public final void open(MsgBlk blk, int offt, int size) {
        current_block = blk;
        current_pos = offt;
        remain = size;
        nrd = 0;
    }

    public final ORBCall get_call_context() {
        return call_context_;
    }

    public final void set_call_context(ORBCall ctx) {
        call_context_ = ctx;
    }

    public final int get_byte_order() {
        return byte_order;
    }

    public final void set_byte_order(int b) {
        byte_order = b;
    }

    public final void set_stream_pos(int n) {
        nrd = n;
    }

    public final boolean read_boolean() {
        return read_octet() == 0 ? false : true;
    }

    public final char read_char() {
        return (char) read_octet();
    }

    public final char read_wchar() {
        return (char) read_short();
    }

    public final byte read_octet() {
        MsgBlk blk = reserve(1);
        byte value = blk.data[current_pos++];
        return value;
    }

    public final short read_short() {
        align2();
        MsgBlk blk = reserve(2);
        short value = (byte_order == CDR.BIG_ENDIAN) ? CDR.read2b(blk.data, current_pos) : CDR.read2l(blk.data, current_pos);
        current_pos += 2;
        return value;
    }

    public final short read_ushort() {
        return read_short();
    }

    public final int read_long() {
        align4();
        MsgBlk blk = reserve(4);
        int value = (byte_order == CDR.BIG_ENDIAN) ? CDR.read4b(blk.data, current_pos) : CDR.read4l(blk.data, current_pos);
        current_pos += 4;
        return value;
    }

    public final int read_ulong() {
        return read_long();
    }

    public final long read_longlong() {
        align8();
        MsgBlk blk = reserve(8);
        long value = (byte_order == CDR.BIG_ENDIAN) ? CDR.read8b(blk.data, current_pos) : CDR.read8l(blk.data, current_pos);
        current_pos += 8;
        return value;
    }

    public final long read_ulonglong() {
        return read_longlong();
    }

    public final float read_float() {
        return Float.intBitsToFloat(read_long());
    }

    public final double read_double() {
        return Double.longBitsToDouble(read_longlong());
    }

    public final String read_string() {
        int length = read_long();
        if (length <= 0) throw_marshal_error();
        MsgBlk blk = reserve(length);
        char[] sb = new char[length - 1];
        byte[] buf = blk.data;
        int idx = current_pos;
        for (int i = 0; i < length - 1; ++i) sb[i] = (char) buf[idx++];
        current_pos += length;
        return new String(sb);
    }

    public final String read_wstring() {
        int rawlen = read_long();
        if (rawlen < 0) throw_marshal_error();
        int length = rawlen / 2;
        MsgBlk blk = reserve(rawlen);
        char[] sb = new char[length];
        byte[] buf = blk.data;
        int idx = current_pos;
        if (byte_order == CDR.BIG_ENDIAN) {
            for (int i = 0; i < length; ++i, idx += 2) sb[i] = (char) CDR.read2b(buf, idx);
        } else {
            for (int i = 0; i < length; ++i, idx += 2) sb[i] = (char) CDR.read2l(buf, idx);
        }
        current_pos += rawlen;
        return new String(sb);
    }

    public final void read_boolean_array(boolean[] value, int offset, int length) {
        MsgBlk blk = reserve(length);
        byte[] buf = blk.data;
        int idx = current_pos;
        for (int i = 0; i < length; ++i) {
            value[offset++] = buf[idx++] == 0 ? false : true;
        }
        current_pos = idx;
    }

    public final void read_char_array(char[] value, int offset, int length) {
        MsgBlk blk = reserve(length);
        byte[] buf = blk.data;
        int idx = current_pos;
        for (int i = 0; i < length; ++i) value[offset++] = (char) buf[idx++];
        current_pos = idx;
    }

    public final void read_wchar_array(char[] value, int offset, int length) {
        if (length == 0) return;
        align2();
        int rawlen = length * 2;
        MsgBlk blk = reserve(rawlen);
        byte[] buf = blk.data;
        int idx = current_pos;
        if (byte_order == CDR.BIG_ENDIAN) {
            for (int i = 0; i < length; ++i, idx += 2) {
                value[offset++] = (char) CDR.read2b(buf, idx);
            }
        } else {
            for (int i = 0; i < length; ++i, idx += 2) {
                value[offset++] = (char) CDR.read2l(buf, idx);
            }
        }
        current_pos = idx;
    }

    public final void read_octet_array(byte[] value, int offset, int length) {
        MsgBlk blk = reserve(length);
        System.arraycopy(blk.data, current_pos, value, offset, length);
        current_pos += length;
    }

    public final void read_short_array(short[] value, int offset, int length) {
        if (length == 0) return;
        align2();
        int rawlen = length * 2;
        MsgBlk blk = reserve(rawlen);
        byte[] buf = blk.data;
        int idx = current_pos;
        if (byte_order == CDR.BIG_ENDIAN) {
            for (int i = 0; i < length; ++i, idx += 2) {
                value[offset++] = CDR.read2b(buf, idx);
            }
        } else {
            for (int i = 0; i < length; ++i, idx += 2) {
                value[offset++] = CDR.read2l(buf, idx);
            }
        }
        current_pos = idx;
    }

    public final void read_ushort_array(short[] value, int offset, int length) {
        read_short_array(value, offset, length);
    }

    public final void read_long_array(int[] value, int offset, int length) {
        if (length == 0) return;
        align4();
        int rawlen = length * 4;
        MsgBlk blk = reserve(rawlen);
        byte[] buf = blk.data;
        int idx = current_pos;
        if (byte_order == CDR.BIG_ENDIAN) {
            for (int i = 0; i < length; ++i, idx += 4) {
                value[offset++] = CDR.read4b(buf, idx);
            }
        } else {
            for (int i = 0; i < length; ++i, idx += 4) {
                value[offset++] = CDR.read4l(buf, idx);
            }
        }
        current_pos = idx;
    }

    public final void read_ulong_array(int[] value, int offset, int length) {
        read_long_array(value, offset, length);
    }

    public final void read_longlong_array(long[] value, int offset, int length) {
        if (length == 0) return;
        align8();
        int rawlen = length * 8;
        MsgBlk blk = reserve(rawlen);
        byte[] buf = blk.data;
        int idx = current_pos;
        if (byte_order == CDR.BIG_ENDIAN) {
            for (int i = 0; i < length; ++i, idx += 8) {
                value[offset++] = CDR.read8b(buf, idx);
            }
        } else {
            for (int i = 0; i < length; ++i, idx += 8) {
                value[offset++] = CDR.read8l(buf, idx);
            }
        }
        current_pos = idx;
    }

    public final void read_ulonglong_array(long[] value, int offset, int length) {
        read_longlong_array(value, offset, length);
    }

    public final void read_float_array(float[] value, int offset, int length) {
        if (length == 0) return;
        align4();
        int rawlen = length * 4;
        MsgBlk blk = reserve(rawlen);
        byte[] buf = blk.data;
        int idx = current_pos;
        if (byte_order == CDR.BIG_ENDIAN) {
            for (int i = 0; i < length; ++i, idx += 4) {
                value[offset++] = Float.intBitsToFloat(CDR.read4b(buf, idx));
            }
        } else {
            for (int i = 0; i < length; ++i, idx += 4) {
                value[offset++] = Float.intBitsToFloat(CDR.read4l(buf, idx));
            }
        }
        current_pos = idx;
    }

    public final void read_double_array(double[] value, int offset, int length) {
        if (length == 0) return;
        align8();
        int rawlen = length * 8;
        MsgBlk blk = reserve(rawlen);
        byte[] buf = blk.data;
        int idx = current_pos;
        if (byte_order == CDR.BIG_ENDIAN) {
            for (int i = 0; i < length; ++i, idx += 8) {
                value[offset++] = Double.longBitsToDouble(CDR.read8b(buf, idx));
            }
        } else {
            for (int i = 0; i < length; ++i, idx += 8) {
                value[offset++] = Double.longBitsToDouble(CDR.read8l(buf, idx));
            }
        }
        current_pos = idx;
    }

    public final org.omg.CORBA.Object read_Object() {
        ObjectRef objref = ObjectRef.read(this);
        if (objref.is_null()) return null;
        ObjectImpl obj = new ObjectImpl(new DelegateImpl(orb_, objref));
        return obj;
    }

    public final org.omg.CORBA.TypeCode read_TypeCode() {
        return TypeCodeImpl.read(this);
    }

    public final org.omg.CORBA.Any read_any() {
        TypeCode type = read_TypeCode();
        Any val = orb_.create_any();
        val.read_value(this, type);
        return val;
    }

    public final org.omg.CORBA.Principal read_Principal() {
        throw new org.omg.CORBA.NO_IMPLEMENT();
    }

    public final int read() throws java.io.IOException {
        throw new org.omg.CORBA.NO_IMPLEMENT();
    }

    public final java.math.BigDecimal read_fixed() {
        throw new org.omg.CORBA.NO_IMPLEMENT();
    }

    public final org.omg.CORBA.Context read_Context() {
        throw new org.omg.CORBA.NO_IMPLEMENT();
    }

    public final org.omg.CORBA.Object read_Object(java.lang.Class clz) {
        try {
            org.omg.CORBA.portable.ObjectImpl obj = (org.omg.CORBA.portable.ObjectImpl) clz.newInstance();
            obj._set_delegate(new DelegateImpl(orb_, ObjectRef.read(this)));
            return obj;
        } catch (InstantiationException ex) {
            throw new org.omg.CORBA.INTERNAL(ex.toString(), 0, org.omg.CORBA.CompletionStatus.COMPLETED_YES);
        } catch (IllegalAccessException ex) {
            throw new org.omg.CORBA.INTERNAL(ex.toString(), 0, org.omg.CORBA.CompletionStatus.COMPLETED_YES);
        }
    }

    public static void copy_value(org.omg.CORBA.portable.InputStream is, org.omg.CORBA.portable.OutputStream os, TypeCode type) {
        int tk = type.kind().value();
        try {
            switch(tk) {
                case TCKind._tk_null:
                case TCKind._tk_void:
                    break;
                case TCKind._tk_octet:
                    os.write_octet(is.read_octet());
                    break;
                case TCKind._tk_boolean:
                    os.write_boolean(is.read_boolean());
                    break;
                case TCKind._tk_short:
                case TCKind._tk_ushort:
                    os.write_short(is.read_short());
                    break;
                case TCKind._tk_long:
                case TCKind._tk_ulong:
                case TCKind._tk_enum:
                    os.write_long(is.read_long());
                    break;
                case TCKind._tk_longlong:
                case TCKind._tk_ulonglong:
                    os.write_longlong(is.read_longlong());
                    break;
                case TCKind._tk_fixed:
                    os.write_fixed(is.read_fixed());
                    break;
                case TCKind._tk_char:
                    os.write_char(is.read_char());
                    break;
                case TCKind._tk_wchar:
                    os.write_wchar(is.read_wchar());
                    break;
                case TCKind._tk_string:
                    os.write_string(is.read_string());
                    break;
                case TCKind._tk_wstring:
                    os.write_wstring(is.read_wstring());
                    break;
                case TCKind._tk_array:
                    {
                        TypeCode content_type = type.content_type();
                        int len = type.length();
                        for (int i = 0; i < len; ++i) {
                            copy_value(is, os, content_type);
                        }
                    }
                    break;
                case TCKind._tk_sequence:
                    {
                        TypeCode content_type = type.content_type();
                        int len = is.read_long();
                        os.write_long(len);
                        for (int i = 0; i < len; ++i) {
                            copy_value(is, os, content_type);
                        }
                    }
                    break;
                case TCKind._tk_except:
                    os.write_string(is.read_string());
                case TCKind._tk_struct:
                    {
                        int cnt = type.member_count();
                        for (int i = 0; i < cnt; ++i) {
                            TypeCode mbr_type = type.member_type(i);
                            copy_value(is, os, type.member_type(i));
                        }
                    }
                    break;
                case TCKind._tk_alias:
                    copy_value(is, os, type.content_type());
                    break;
                case TCKind._tk_union:
                    {
                        TypeCode dt = type.discriminator_type();
                        Any disc = org.omg.CORBA.ORB.init().create_any();
                        disc.read_value(is, dt);
                        int cnt = type.member_count();
                        int idx = type.default_index();
                        for (int i = 0; i < cnt; ++i) {
                            Any label = type.member_label(i);
                            if (i != idx && disc.equal(type.member_label(i))) {
                                idx = i;
                                break;
                            }
                        }
                        if (idx < 0) throw new org.omg.CORBA.INTERNAL("Unknown union member");
                        copy_value(is, os, type.member_type(idx));
                    }
                    break;
                case TCKind._tk_any:
                    os.write_any(is.read_any());
                    break;
                case TCKind._tk_TypeCode:
                    os.write_TypeCode(is.read_TypeCode());
                    break;
                case TCKind._tk_Principal:
                    os.write_Principal(is.read_Principal());
                    break;
                case TCKind._tk_objref:
                    os.write_Object(is.read_Object());
                    break;
                default:
                    throw new org.omg.CORBA.INTERNAL("Unsupported typecode for copy_value");
            }
        } catch (org.omg.CORBA.TypeCodePackage.BadKind ex) {
            throw new org.omg.CORBA.INTERNAL(ex.toString());
        } catch (org.omg.CORBA.TypeCodePackage.Bounds ex) {
            throw new org.omg.CORBA.INTERNAL(ex.toString());
        }
    }

    public final org.omg.CORBA.ORB orb() {
        return orb_;
    }

    public final void skip(int n) {
        reserve(n);
        current_pos += n;
    }

    public final CDRInputStream read_encapsulation() {
        int length = read_long();
        CDRInputStream is = new CDRInputStream(orb_);
        is.open(current_block, current_pos, remain);
        boolean little_endian = is.read_boolean();
        is.set_byte_order(little_endian ? CDR.LITTLE_ENDIAN : CDR.BIG_ENDIAN);
        skip(length);
        return is;
    }

    private final void align2() {
        if ((nrd & 1) == 0) return;
        reserve(1);
        ++current_pos;
    }

    private final void align4() {
        int na = nrd & 3;
        if (na == 0) return;
        na = 4 - na;
        reserve(na);
        current_pos += na;
    }

    private final void align8() {
        int na = nrd & 7;
        if (na == 0) return;
        na = 8 - na;
        reserve(na);
        current_pos += na;
    }

    private final MsgBlk reserve(int n) {
        int rem = remain;
        if (n > rem) throw_marshal_error();
        MsgBlk cb = current_block;
        if (cb != null && cb.used - current_pos >= n) {
            nrd += n;
            remain = rem - n;
            return cb;
        }
        if (cb.used != 0) throw_marshal_error();
        current_pos = 0;
        while (true) {
            cb = cb.next;
            if (cb == null) throw_marshal_error();
            if (cb.used >= n) break;
            if (cb.used > 0) throw_marshal_error();
        }
        current_block = cb;
        nrd += n;
        remain = rem - n;
        return cb;
    }

    private final void throw_marshal_error() {
        throw new org.omg.CORBA.MARSHAL("CDRInputStream", 0, org.omg.CORBA.CompletionStatus.COMPLETED_YES);
    }
}
