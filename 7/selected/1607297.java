package org.openorb.orb.iiop;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import javax.rmi.CORBA.ValueHandler;
import org.omg.CORBA.OctetSeqHolder;
import org.omg.CORBA.IntHolder;
import org.omg.CORBA.Any;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.UnionMember;
import org.omg.CORBA.ValueMember;
import org.omg.SendingContext.RunTime;
import org.openorb.orb.io.AbstractInputStream;
import org.openorb.orb.io.BufferSource;
import org.openorb.orb.io.StorageBuffer;
import org.openorb.orb.rmi.ValueHandlerImpl;
import org.openorb.orb.util.Trace;
import org.openorb.util.ExceptionTool;
import org.openorb.util.HexPrintStream;
import org.openorb.util.NumberCache;
import org.openorb.util.RepoIDHelper;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.logger.LogEnabled;

/**
 * This class implements CDR for IIOP
 *
 * @author Chris Wood
 * @version $Revision: 1.22 $ $Date: 2004/08/20 10:08:12 $
 */
public class CDRInputStream extends AbstractInputStream implements LogEnabled {

    private static final int NO_TYPE_INFORMATION = 0;

    private static final int CODEBASE = 1;

    private static final int MULTIPLE_TYPE_INFORMATION = 6;

    private static final int MASK_TYPE_INFORMATION = 6;

    private static final int CHUNK = 8;

    /**
     * ORB Reference
     */
    private org.omg.CORBA.ORB m_orb;

    /**
     * CDR protocol version
     */
    private org.omg.GIOP.Version m_version;

    /**
     * Cache for unmarshalled bits of value. Hashed on index, only one
     * table is needed as indexes are unique.
     */
    private Map m_value_cache;

    /**
     * Bundle of state for mark / reset
     */
    private MarkState m_mark = null;

    /**
     * Encoding for char data.
     */
    private String m_char_enc = "ISO-8859-1";

    /**
     * Encoding for wchar data.
     */
    private String m_wchar_enc = "UnicodeBig";

    /**
     * Alignment for wchar data.
     */
    private int m_wchar_align = 2;

    /**
     * reverse wchar data.
     */
    private boolean m_wchar_reverse = false;

    /**
     * Current index
     */
    private int m_index = 0;

    /**
     * Flag indicating swap byte order.
     */
    private boolean m_swap = false;

    /**
     * Remaining bytes in encapsulation
     */
    private int m_encaps_remain = -1;

    /**
     * Close encapsulation is pending
     */
    private int m_pending_encaps_close = 0;

    /**
     * remaining size in enclosing encapsulations
     */
    private LinkedList m_encaps_stack = new LinkedList();

    /**
     * Value nesting level.
     */
    private int m_value_level = 0;

    /**
     * Continuation chunk for value pending.
     */
    private boolean m_pending_value_reopen = false;

    /**
     * True if within read for chunked value
     */
    private boolean m_in_chunked_value = false;

    /**
     * passed between read_value and read_value(java.io.Streamable).
     * for placement in indirection table.
     */
    private int m_value_indirect = -1;

    /**
     * count level of continuation chunk. If this is 0 then failure to find
     * value factory while unmarshaling results in a cancel, otherwise the
     * exception is thrown without cancelling.
     */
    private int m_continue_level = 0;

    /**
     * Reference to the ValueHandler.
     */
    private static ValueHandler s_handler;

    /**
     * The codebase of the current value.
     */
    private String m_code_base;

    /**
     * The logger instance.
     */
    private Logger m_logger;

    public void enableLogging(Logger logger) {
        m_logger = logger;
    }

    protected Logger getLogger() {
        if (null == m_logger) {
            m_logger = ((org.openorb.orb.core.ORBSingleton) m_orb).getLogger();
        }
        return m_logger;
    }

    private class MarkState {

        MarkState() {
            this.m_index = CDRInputStream.this.m_index;
            this.m_swap = CDRInputStream.this.m_swap;
            this.m_encaps_remain = CDRInputStream.this.m_encaps_remain;
            this.m_pending_encaps_close = CDRInputStream.this.m_pending_encaps_close;
            this.m_encaps_stack = new LinkedList(CDRInputStream.this.m_encaps_stack);
            this.m_value_level = CDRInputStream.this.m_value_level;
            this.m_pending_value_reopen = CDRInputStream.this.m_pending_value_reopen;
            this.m_in_chunked_value = CDRInputStream.this.m_in_chunked_value;
            this.m_value_indirect = CDRInputStream.this.m_value_indirect;
        }

        void reset() {
            CDRInputStream.this.m_index = this.m_index;
            CDRInputStream.this.m_swap = this.m_swap;
            CDRInputStream.this.m_encaps_remain = this.m_encaps_remain;
            CDRInputStream.this.m_pending_encaps_close = this.m_pending_encaps_close;
            CDRInputStream.this.m_encaps_stack = this.m_encaps_stack;
            CDRInputStream.this.m_value_level = this.m_value_level;
            CDRInputStream.this.m_pending_value_reopen = this.m_pending_value_reopen;
            CDRInputStream.this.m_in_chunked_value = this.m_in_chunked_value;
            CDRInputStream.this.m_value_indirect = this.m_value_indirect;
        }

        private int m_index;

        private boolean m_swap;

        private int m_encaps_remain = -1;

        private int m_pending_encaps_close;

        private LinkedList m_encaps_stack;

        private int m_value_level;

        private boolean m_pending_value_reopen;

        private boolean m_in_chunked_value;

        private int m_value_indirect;
    }

    /**
     * tempory vars. here to avoid creating new every time
     */
    private OctetSeqHolder m_tmp_buf = new OctetSeqHolder();

    private IntHolder m_tmp_off = new IntHolder();

    private IntHolder m_tmp_len = new IntHolder();

    /**
     * Constructor used by codec. Extending classes must implement a constructor
     * with this exact signature.
     */
    public CDRInputStream(org.omg.CORBA.ORB orb, boolean bigEndian, org.omg.GIOP.Version version, StorageBuffer buf) {
        super(buf);
        m_orb = orb;
        bigEndian(bigEndian);
        m_version = version;
        if (m_version.minor == 0) {
            m_char_enc = "ISO-8859-1";
            m_wchar_enc = null;
        }
        s_handler = ValueHandlerImpl.createValueHandler(getLogger().getChildLogger("vh"));
    }

    /**
     * Constructor used by lower layer. Extending classes must implement a constructor
     * with this exact signature.
     */
    public CDRInputStream(org.omg.CORBA.ORB orb, boolean bigEndian, org.omg.GIOP.Version version, BufferSource source) {
        super(source);
        m_orb = orb;
        bigEndian(bigEndian);
        m_version = version;
        if (m_version.minor == 0) {
            m_char_enc = "ISO-8859-1";
            m_wchar_enc = null;
        }
        s_handler = ValueHandlerImpl.createValueHandler(getLogger().getChildLogger("vh"));
    }

    /**
     * Get the orb associated with the stream.
     */
    public org.omg.CORBA.ORB orb() {
        return m_orb;
    }

    /**
     * Set the translation codesets.
     */
    public void setCodesets(int tcsc, int tcsw) {
        if (tcsc != 0) {
            m_char_enc = CodeSetDatabase.getNameFromId(tcsc);
            if (CodeSetDatabase.getAlignmentFromId(tcsc) > 1) {
                throw new org.omg.CORBA.CODESET_INCOMPATIBLE(0, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
            }
        }
        if (tcsw == 0) {
            m_wchar_enc = null;
        } else {
            m_wchar_enc = CodeSetDatabase.getNameFromId(tcsw);
            if (m_wchar_enc.equals("UnicodeBigUnmarked")) {
                m_wchar_enc = "UnicodeBig";
            }
            m_wchar_align = CodeSetDatabase.getAlignmentFromId(tcsw);
            if (m_version.minor == 1 && m_wchar_align != 2) {
                throw new org.omg.CORBA.CODESET_INCOMPATIBLE(0, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
            }
            if (m_swap && m_wchar_align > 1) {
                String canonical = CodeSetDatabase.getCanonicalNameFromId(tcsw);
                if (canonical.startsWith("Unicode")) {
                    if (m_version.minor == 1) {
                        m_wchar_enc = "UnicodeLittle";
                    }
                } else {
                    m_wchar_reverse = true;
                }
            }
        }
        if (getLogger().isDebugEnabled() && Trace.isHigh()) {
            getLogger().debug("New codesets for input stream set to [" + m_char_enc + "] and [" + m_wchar_enc + "].");
        }
    }

    /**
     * Get the current index in the buffer.
     */
    public int index() {
        return m_index;
    }

    /**
     * Reset the index to 0. This affects the alignment. Using this inside an
     * encapsulation may cause unexpected behaviour.
     */
    public void reset_index() {
        m_index = 0;
    }

    /**
     * Get the CDR protocol version
     */
    public org.omg.GIOP.Version version() {
        return m_version;
    }

    /**
     * Get the endian mode
     */
    public boolean bigEndian() {
        return !m_swap;
    }

    /**
     * Set the endian mode
     */
    public void bigEndian(boolean bigEndian) {
        m_swap = !bigEndian;
    }

    /**
     * Adjust alignment.
     */
    public void alignment(int size) {
        if (m_pending_value_reopen) {
            m_pending_value_reopen = false;
            int len = peek_long();
            if (!((len >= 0x7fffff00) && (len <= 0x7fffffff))) {
                begin_value_chunk(read_long());
            }
        }
        if (size > 1) {
            int tmp = m_index % size;
            if (tmp != 0) {
                force_skip(size - tmp);
            }
        }
    }

    /**
     * Encapsulation begin.
     */
    public void begin_encapsulation() {
        int len = read_ulong();
        m_encaps_stack.addLast(m_swap ? Boolean.TRUE : Boolean.FALSE);
        m_encaps_stack.addLast(NumberCache.getInteger(m_encaps_remain));
        m_encaps_stack.addLast(NumberCache.getInteger(m_index));
        m_encaps_stack.addLast(Boolean.FALSE);
        m_encaps_remain = len;
        m_in_chunked_value = false;
        m_swap = read_boolean();
    }

    /**
     * skip to end of current encapsulation
     */
    public void end_encapsulation() {
        if (m_pending_encaps_close == 0) {
            force_skip(m_encaps_remain);
        }
        --m_pending_encaps_close;
    }

    /**
     * Skip over bytes.
     */
    public long skip(long count) {
        long total = super.skip(count);
        postread((int) total);
        return total;
    }

    public void mark(int readlimit) {
        super.mark(readlimit);
        m_mark = new MarkState();
    }

    public void reset() throws IOException {
        super.reset();
        m_mark.reset();
        m_mark = null;
    }

    /**
     * Return an IDL boolean
     */
    public boolean read_boolean() {
        return (read_octet() == 1);
    }

    /**
     * Return an IDL char
     */
    public char read_char() {
        alignment(1);
        m_tmp_len.value = 1;
        next(m_tmp_buf, m_tmp_off, m_tmp_len);
        postread(1);
        try {
            String s = new String(m_tmp_buf.value, m_tmp_off.value, 1, m_char_enc);
            if (s.length() == 1) {
                return s.charAt(0);
            }
            cancel(new org.omg.CORBA.MARSHAL("Unable to decode char value", IIOPMinorCodes.MARSHAL_CHAR, CompletionStatus.COMPLETED_MAYBE));
        } catch (final UnsupportedEncodingException ex) {
            getLogger().error("Unsupported encoding should be impossible.", ex);
        }
        return '\0';
    }

    /**
     * Read a wchar from the input stream.
     *
     * @return '\0' When no wchar encoder is set or the letter represented by the bytes
     * is longer than 1.
     */
    public char read_wchar() {
        if (m_wchar_enc == null) {
            if (m_version.minor == 0) {
                cancel(new org.omg.CORBA.BAD_OPERATION("Wchar not supported in IIOP 1.0", IIOPMinorCodes.BAD_OPERATION_IIOP_VERSION, CompletionStatus.COMPLETED_MAYBE));
            } else {
                cancel(new org.omg.CORBA.MARSHAL("Missing wchar encoder.", IIOPMinorCodes.MARSHAL_WCHAR, CompletionStatus.COMPLETED_MAYBE));
            }
            return '\0';
        }
        byte[] buf = null;
        int off = 0;
        int len = 0;
        switch(m_version.minor) {
            case 0:
                cancel(new org.omg.CORBA.BAD_OPERATION("Wchar not supported in IIOP 1.0", IIOPMinorCodes.BAD_OPERATION_IIOP_VERSION, CompletionStatus.COMPLETED_MAYBE));
                return '\0';
            case 1:
                alignment(2);
                len = 2;
                m_tmp_len.value = 2;
                next(m_tmp_buf, m_tmp_off, m_tmp_len);
                if (m_tmp_len.value != 0) {
                    buf = new byte[2];
                    if (m_wchar_reverse) {
                        buf[1] = m_tmp_buf.value[m_tmp_off.value];
                        next(m_tmp_buf, m_tmp_off, m_tmp_len);
                        buf[0] = m_tmp_buf.value[m_tmp_off.value];
                    } else {
                        buf[0] = m_tmp_buf.value[m_tmp_off.value];
                        next(m_tmp_buf, m_tmp_off, m_tmp_len);
                        buf[1] = m_tmp_buf.value[m_tmp_off.value];
                    }
                    off = 0;
                } else if (m_wchar_reverse) {
                    buf = new byte[2];
                    buf[0] = m_tmp_buf.value[1];
                    buf[1] = m_tmp_buf.value[0];
                    off = 0;
                } else {
                    buf = m_tmp_buf.value;
                    off = m_tmp_off.value;
                }
                postread(2);
                break;
            case 2:
                alignment(1);
                m_tmp_len.value = 1;
                next(m_tmp_buf, m_tmp_off, m_tmp_len);
                len = m_tmp_buf.value[m_tmp_off.value];
                m_tmp_len.value = m_tmp_buf.value[m_tmp_off.value];
                next(m_tmp_buf, m_tmp_off, m_tmp_len);
                postread(len - m_tmp_len.value + 1);
                if (m_tmp_len.value != 0) {
                    buf = new byte[len];
                    off = 0;
                    System.arraycopy(m_tmp_buf.value, m_tmp_off.value, buf, 0, len - m_tmp_len.value);
                    read_octet_array(buf, len - m_tmp_len.value, m_tmp_len.value);
                    if (m_wchar_reverse) {
                        byte tmp;
                        for (int i = 0; i < m_wchar_align / 2; ++i) {
                            tmp = buf[i];
                            buf[i] = buf[m_wchar_align - i];
                            buf[m_wchar_align - i] = tmp;
                        }
                    }
                } else if (m_wchar_reverse) {
                    buf = new byte[len];
                    off = 0;
                    for (int i = 0; i < m_wchar_align; ++i) {
                        buf[i] = m_tmp_buf.value[m_tmp_off.value + m_wchar_align - i];
                    }
                } else {
                    buf = m_tmp_buf.value;
                    off = m_tmp_off.value;
                }
                break;
        }
        try {
            String s = new String(buf, off, len, m_wchar_enc);
            if (s.length() == 1) {
                return s.charAt(0);
            }
            cancel(new org.omg.CORBA.MARSHAL("Unable to decode wchar value", IIOPMinorCodes.MARSHAL_WCHAR, CompletionStatus.COMPLETED_MAYBE));
        } catch (final UnsupportedEncodingException ex) {
            getLogger().error("Unsupported encoding should be impossible.", ex);
        }
        return '\0';
    }

    /**
     * Return an IDL octet
     */
    public byte read_octet() {
        alignment(1);
        m_tmp_len.value = 1;
        next(m_tmp_buf, m_tmp_off, m_tmp_len);
        byte b = m_tmp_buf.value[m_tmp_off.value];
        postread(1);
        return b;
    }

    /**
     * Return an IDL short
     */
    public short read_short() {
        alignment(2);
        int val;
        m_tmp_len.value = 2;
        if (next(m_tmp_buf, m_tmp_off, m_tmp_len) == 2) {
            val = ((m_tmp_buf.value[m_tmp_off.value] & 0xFF) << (m_swap ? 0 : 8)) | ((m_tmp_buf.value[m_tmp_off.value + 1] & 0xFF) << (m_swap ? 8 : 0));
        } else {
            val = ((m_tmp_buf.value[m_tmp_off.value] & 0xFF) << (m_swap ? 0 : 8));
            next(m_tmp_buf, m_tmp_off, m_tmp_len);
            val = val | ((m_tmp_buf.value[m_tmp_off.value] & 0xFF) << (m_swap ? 8 : 0));
        }
        postread(2);
        return (short) val;
    }

    /**
     * Return an IDL unsigned short
     */
    public short read_ushort() {
        return read_short();
    }

    /**
     * Return an IDL long
     */
    public int read_long() {
        alignment(4);
        int val;
        int got;
        m_tmp_len.value = 4;
        if ((got = next(m_tmp_buf, m_tmp_off, m_tmp_len)) == 4) {
            val = ((m_tmp_buf.value[m_tmp_off.value] & 0xFF) << (m_swap ? 0 : 24)) | ((m_tmp_buf.value[m_tmp_off.value + 1] & 0xFF) << (m_swap ? 8 : 16)) | ((m_tmp_buf.value[m_tmp_off.value + 2] & 0xFF) << (m_swap ? 16 : 8)) | ((m_tmp_buf.value[m_tmp_off.value + 3] & 0xFF) << (m_swap ? 24 : 0));
        } else {
            val = 0;
            int shf = m_swap ? 0 : 24;
            while (true) {
                for (int i = 0; i < got; ++i) {
                    val = val | ((m_tmp_buf.value[m_tmp_off.value + i] & 0xFF) << shf);
                    shf += m_swap ? 8 : -8;
                }
                if (m_swap ? (shf < 24) : (shf > 0)) {
                    got = next(m_tmp_buf, m_tmp_off, m_tmp_len);
                    continue;
                }
                break;
            }
        }
        postread(4);
        return val;
    }

    private int peek_long() {
        mark(4);
        int ret = read_long();
        try {
            reset();
        } catch (final java.io.IOException ex) {
            getLogger().error("IOException during reset().", ex);
        }
        return ret;
    }

    /**
     * Return an IDL unsigned long
     */
    public int read_ulong() {
        return read_long();
    }

    /**
     * Return an IDL long long
     */
    public long read_longlong() {
        alignment(8);
        long val;
        int got;
        m_tmp_len.value = 8;
        if ((got = next(m_tmp_buf, m_tmp_off, m_tmp_len)) == 8) {
            val = ((m_tmp_buf.value[m_tmp_off.value] & 0xFFL) << (m_swap ? 0L : 56L)) | ((m_tmp_buf.value[m_tmp_off.value + 1] & 0xFFL) << (m_swap ? 8L : 48L)) | ((m_tmp_buf.value[m_tmp_off.value + 2] & 0xFFL) << (m_swap ? 16L : 40L)) | ((m_tmp_buf.value[m_tmp_off.value + 3] & 0xFFL) << (m_swap ? 24L : 32L)) | ((m_tmp_buf.value[m_tmp_off.value + 4] & 0xFFL) << (m_swap ? 32L : 24L)) | ((m_tmp_buf.value[m_tmp_off.value + 5] & 0xFFL) << (m_swap ? 40L : 16L)) | ((m_tmp_buf.value[m_tmp_off.value + 6] & 0xFFL) << (m_swap ? 48L : 8L)) | ((m_tmp_buf.value[m_tmp_off.value + 7] & 0xFFL) << (m_swap ? 56L : 0L));
        } else {
            val = 0;
            long shf = m_swap ? 0 : 56;
            while (true) {
                for (int i = 0; i < got; ++i) {
                    val = val | ((long) (m_tmp_buf.value[m_tmp_off.value + i] & 0xFF) << shf);
                    shf += m_swap ? 8 : -8;
                }
                if (m_swap ? (shf < 56) : (shf > 0)) {
                    got = next(m_tmp_buf, m_tmp_off, m_tmp_len);
                    continue;
                }
                break;
            }
        }
        postread(8);
        return val;
    }

    /**
     * Return an IDL unsigned long long
     */
    public long read_ulonglong() {
        return read_longlong();
    }

    /**
     * Return an IDL float
     */
    public float read_float() {
        return Float.intBitsToFloat(read_long());
    }

    /**
     * Return an IDL double
     */
    public double read_double() {
        return Double.longBitsToDouble(read_longlong());
    }

    /**
     * Return an IDL string
     */
    public String read_string() {
        int len = read_ulong();
        byte[] buf;
        int off;
        m_tmp_len.value = len;
        next(m_tmp_buf, m_tmp_off, m_tmp_len);
        postread(len - m_tmp_len.value);
        if (m_tmp_len.value == 0) {
            buf = m_tmp_buf.value;
            off = m_tmp_off.value;
        } else {
            buf = new byte[len];
            off = 0;
            System.arraycopy(m_tmp_buf.value, m_tmp_off.value, buf, 0, len - m_tmp_len.value);
            read_octet_array(buf, len - m_tmp_len.value, m_tmp_len.value);
        }
        try {
            return new String(buf, off, len - 1, m_char_enc);
        } catch (final UnsupportedEncodingException ex) {
            getLogger().error("Unsupported encoding should be impossible.", ex);
        }
        return null;
    }

    /**
     * Return an IDL wstring
     */
    public String read_wstring() {
        if (m_version.minor == 0) {
            cancel(new org.omg.CORBA.BAD_OPERATION("Wchar not supported in IIOP 1.0", IIOPMinorCodes.BAD_OPERATION_IIOP_VERSION, CompletionStatus.COMPLETED_MAYBE));
            return null;
        }
        if (m_wchar_enc == null) {
            cancel(new org.omg.CORBA.MARSHAL("Missing wchar encoder.", IIOPMinorCodes.MARSHAL_WCHAR, CompletionStatus.COMPLETED_MAYBE));
            return null;
        }
        byte[] buf;
        int off;
        int len = read_ulong();
        if (m_version.minor == 1 && m_wchar_align > 1) {
            len *= m_wchar_align;
        }
        m_tmp_len.value = len;
        next(m_tmp_buf, m_tmp_off, m_tmp_len);
        postread(len - m_tmp_len.value);
        if (m_tmp_len.value != 0) {
            buf = new byte[len];
            off = 0;
            System.arraycopy(m_tmp_buf.value, m_tmp_off.value, buf, 0, len - m_tmp_len.value);
            read_octet_array(buf, len - m_tmp_len.value, m_tmp_len.value);
            if (m_wchar_reverse) {
                byte tmp;
                for (int i = 0; i < len; i += m_wchar_align) {
                    for (int j = 0; j < m_wchar_align / 2; ++j) {
                        tmp = buf[off + i + j];
                        buf[off + i + j] = buf[off + i + m_wchar_align - j - 1];
                        buf[off + i + m_wchar_align - j - 1] = tmp;
                    }
                }
            }
        } else if (m_wchar_reverse) {
            buf = new byte[len];
            off = 0;
            for (int i = 0; i < len; i += m_wchar_align) {
                for (int j = 0; j < m_wchar_align; ++j) {
                    buf[i + j] = m_tmp_buf.value[m_tmp_off.value + i + m_wchar_align - j - 1];
                }
            }
        } else {
            buf = m_tmp_buf.value;
            off = m_tmp_off.value;
        }
        if (m_version.minor == 1) {
            len -= (m_wchar_align == 0) ? 1 : m_wchar_align;
        }
        if ((buf[off] == (byte) 0xFE && buf[off + 1] == (byte) 0xFF)) {
            off += 2;
            len -= 2;
        } else if ((buf[off] == (byte) 0xFF && buf[off + 1] == (byte) 0xFE)) {
            off += 2;
            len -= 2;
            for (int i = 0; i < len; i += m_wchar_align) {
                for (int j = 0; j < m_wchar_align / 2; ++j) {
                    byte tmp = buf[off + i + j];
                    buf[off + i + j] = buf[off + i + m_wchar_align - j - 1];
                    buf[off + i + m_wchar_align - j - 1] = tmp;
                }
            }
        }
        if (len == 0) {
            return "";
        }
        try {
            return new String(buf, off, len, m_wchar_enc);
        } catch (final UnsupportedEncodingException ex) {
            getLogger().error("Unsupported encoding should be impossible.", ex);
        }
        return null;
    }

    /**
     * Read an IDL boolean array
     */
    public void read_boolean_array(boolean[] val, int off, int len) {
        if (0 == len) {
            return;
        }
        alignment(1);
        int rd;
        m_tmp_len.value = len;
        while (m_tmp_len.value > 0) {
            rd = next(m_tmp_buf, m_tmp_off, m_tmp_len);
            for (int i = 0; i < rd; ++i) {
                val[off + i] = (m_tmp_buf.value[m_tmp_off.value + i] == 1);
            }
            off += rd;
        }
        postread(len);
    }

    /**
     * Read an IDL char array from the stream
     */
    public void read_char_array(char[] val, int voff, int len) {
        if (0 == len) {
            return;
        }
        alignment(1);
        byte[] buf;
        int off;
        m_tmp_len.value = len;
        next(m_tmp_buf, m_tmp_off, m_tmp_len);
        postread(len - m_tmp_len.value);
        if (m_tmp_len.value == 0) {
            buf = m_tmp_buf.value;
            off = m_tmp_off.value;
        } else {
            buf = new byte[len];
            off = 0;
            System.arraycopy(m_tmp_buf.value, m_tmp_off.value, buf, 0, len - m_tmp_len.value);
            read_octet_array(buf, len - m_tmp_len.value, m_tmp_len.value);
        }
        try {
            String s = new String(buf, off, len, m_char_enc);
            if (s.length() != len) {
                cancel(new org.omg.CORBA.MARSHAL("Unable to decode char value", IIOPMinorCodes.MARSHAL_CHAR, CompletionStatus.COMPLETED_MAYBE));
            }
            s.getChars(0, len, val, voff);
        } catch (final UnsupportedEncodingException ex) {
            getLogger().error("Unsupported encoding should be impossible.", ex);
        }
    }

    /**
     * Read a wchar array
     */
    public void read_wchar_array(char[] val, int voff, int vlen) {
        if (0 == vlen) {
            return;
        }
        if (m_wchar_enc == null) {
            if (m_version.minor == 0) {
                cancel(new org.omg.CORBA.BAD_OPERATION("Wchar not supported in IIOP 1.0", IIOPMinorCodes.BAD_OPERATION_IIOP_VERSION, CompletionStatus.COMPLETED_MAYBE));
            } else {
                cancel(new org.omg.CORBA.MARSHAL("Missing wchar encoder.", IIOPMinorCodes.MARSHAL_WCHAR, CompletionStatus.COMPLETED_MAYBE));
            }
            return;
        }
        byte[] buf;
        int off;
        int len;
        switch(m_version.minor) {
            case 0:
                cancel(new org.omg.CORBA.MARSHAL("Wchar not supported in IIOP 1.0", IIOPMinorCodes.BAD_OPERATION_IIOP_VERSION, CompletionStatus.COMPLETED_MAYBE));
                break;
            case 1:
                alignment(2);
                m_tmp_len.value = 2 * vlen;
                len = m_tmp_len.value;
                next(m_tmp_buf, m_tmp_off, m_tmp_len);
                postread(len - m_tmp_len.value);
                if (m_tmp_len.value != 0) {
                    buf = new byte[len];
                    off = 0;
                    System.arraycopy(m_tmp_buf.value, m_tmp_off.value, buf, 0, len - m_tmp_len.value);
                    read_octet_array(buf, len - m_tmp_len.value, m_tmp_len.value);
                    if (m_wchar_reverse) {
                        byte tmp;
                        for (int i = 0; i < len; i += 2) {
                            tmp = buf[i];
                            buf[i] = buf[i + 1];
                            buf[i + 1] = tmp;
                        }
                    }
                } else if (m_wchar_reverse) {
                    buf = new byte[len];
                    off = 0;
                    for (int i = 0; i < len; i += 2) {
                        buf[0] = m_tmp_buf.value[m_tmp_off.value + i + 1];
                        buf[1] = m_tmp_buf.value[m_tmp_off.value + i];
                    }
                } else {
                    buf = m_tmp_buf.value;
                    off = m_tmp_off.value;
                }
                try {
                    String s = new String(buf, off, len, m_wchar_enc);
                    if (s.length() != vlen) {
                        cancel(new org.omg.CORBA.MARSHAL("Unable to decode char value", IIOPMinorCodes.MARSHAL_CHAR, CompletionStatus.COMPLETED_MAYBE));
                    }
                    s.getChars(0, vlen, val, voff);
                } catch (final UnsupportedEncodingException ex) {
                    getLogger().error("Unsupported encoding should be impossible.", ex);
                }
                return;
            case 2:
                alignment(1);
                int post = 0;
                for (int c = 0; c < vlen; ++c) {
                    m_tmp_len.value = 1;
                    next(m_tmp_buf, m_tmp_off, m_tmp_len);
                    len = m_tmp_buf.value[m_tmp_off.value];
                    m_tmp_len.value = m_tmp_buf.value[m_tmp_off.value];
                    next(m_tmp_buf, m_tmp_off, m_tmp_len);
                    if (m_tmp_len.value != 0) {
                        postread(post + len + 1 - m_tmp_len.value);
                        post = 0;
                        buf = new byte[len];
                        off = 0;
                        System.arraycopy(m_tmp_buf.value, m_tmp_off.value, buf, 0, len - m_tmp_len.value);
                        read_octet_array(buf, len - m_tmp_len.value, m_tmp_len.value);
                        if (m_wchar_reverse) {
                            byte tmp;
                            for (int i = 0; i < m_wchar_align / 2; ++i) {
                                tmp = buf[off + i];
                                buf[off + i] = buf[off + m_wchar_align - i];
                                buf[off + m_wchar_align - i] = tmp;
                            }
                        }
                    } else if (m_wchar_reverse) {
                        buf = new byte[len];
                        off = 0;
                        for (int i = 0; i < m_wchar_align; ++i) {
                            buf[i] = m_tmp_buf.value[m_tmp_off.value + m_wchar_align - 1];
                        }
                    } else {
                        post += len + 1;
                        buf = m_tmp_buf.value;
                        off = m_tmp_off.value;
                    }
                    try {
                        String s = new String(buf, off, len, m_wchar_enc);
                        if (s.length() != 1) {
                            cancel(new org.omg.CORBA.MARSHAL("Unable to decode wchar value", IIOPMinorCodes.MARSHAL_WCHAR, CompletionStatus.COMPLETED_MAYBE));
                        }
                        val[voff + c] = s.charAt(0);
                    } catch (final UnsupportedEncodingException ex) {
                        getLogger().error("Unsupported encoding should be impossible.", ex);
                    }
                }
                if (post > 0) {
                    postread(post);
                }
                break;
        }
    }

    /**
     * Read an octet array
     */
    public void read_octet_array(byte[] val, int off, int len) {
        if (0 == len) {
            return;
        }
        alignment(1);
        int rd;
        m_tmp_len.value = len;
        while (m_tmp_len.value > 0) {
            rd = next(m_tmp_buf, m_tmp_off, m_tmp_len);
            System.arraycopy(m_tmp_buf.value, m_tmp_off.value, val, off, rd);
            off += rd;
        }
        postread(len);
    }

    /**
     * Read a short array
     */
    public void read_short_array(short[] val, int off, int len) {
        if (0 == len) {
            return;
        }
        alignment(2);
        m_tmp_len.value = len * 2;
        int rd;
        while (m_tmp_len.value > 0) {
            rd = next(m_tmp_buf, m_tmp_off, m_tmp_len);
            for (int i = 0; i < rd - 1; i += 2, ++off) {
                val[off] = (short) (((m_tmp_buf.value[m_tmp_off.value + i] & 0xFF) << (m_swap ? 0 : 8)) | ((m_tmp_buf.value[m_tmp_off.value + i + 1] & 0xFF) << (m_swap ? 8 : 0)));
            }
            if (rd % 2 != 0) {
                int hlf = ((m_tmp_buf.value[m_tmp_off.value + rd] & 0xFF) << (m_swap ? 0 : 8));
                int tmp = m_tmp_len.value;
                m_tmp_len.value = 1;
                next(m_tmp_buf, m_tmp_off, m_tmp_len);
                val[off] = (short) (hlf | ((m_tmp_buf.value[m_tmp_off.value] & 0xFF) << (m_swap ? 8 : 0)));
                ++off;
                m_tmp_len.value = tmp - 1;
            }
        }
        postread(len * 2);
    }

    /**
     * Read an unsigned short array
     */
    public void read_ushort_array(short[] val, int off, int len) {
        read_short_array(val, off, len);
    }

    /**
     * Read a long array
     */
    public void read_long_array(int[] val, int off, int len) {
        if (0 == len) {
            return;
        }
        alignment(4);
        m_tmp_len.value = len * 4;
        int rd;
        while (m_tmp_len.value > 0) {
            rd = next(m_tmp_buf, m_tmp_off, m_tmp_len);
            for (int i = 0; i < rd - 3; i += 4, ++off) {
                val[off] = (((m_tmp_buf.value[m_tmp_off.value + i] & 0xFF) << (m_swap ? 0 : 24)) | ((m_tmp_buf.value[m_tmp_off.value + i + 1] & 0xFF) << (m_swap ? 8 : 16)) | ((m_tmp_buf.value[m_tmp_off.value + i + 2] & 0xFF) << (m_swap ? 16 : 8)) | ((m_tmp_buf.value[m_tmp_off.value + i + 3] & 0xFF) << (m_swap ? 24 : 0)));
            }
            if (rd % 4 != 0) {
                val[off] = 0;
                int rcvd = rd % 4;
                rd -= rd % 4;
                int s = 0;
                for (; s < rcvd; ++s) {
                    val[off] = val[off] | ((m_tmp_buf.value[m_tmp_off.value + rd + s] & 0xFF) << (m_swap ? (8 * s) : (24 - 8 * s)));
                }
                int tmp = m_tmp_len.value - (4 - s);
                while (s < 4) {
                    m_tmp_len.value = 4 - s;
                    rd = next(m_tmp_buf, m_tmp_off, m_tmp_len);
                    for (int i = 0; i < rd; ++i, ++s) {
                        val[off] = val[off] | ((m_tmp_buf.value[m_tmp_off.value + i] & 0xFF) << (m_swap ? (8 * s) : (24 - 8 * s)));
                    }
                }
                m_tmp_len.value = tmp;
            }
        }
        postread(len * 4);
    }

    /**
     * Read an unsigned long array
     */
    public void read_ulong_array(int[] val, int off, int len) {
        read_long_array(val, off, len);
    }

    /**
     * Read a long long array
     */
    public void read_longlong_array(long[] val, int off, int len) {
        if (0 == len) {
            return;
        }
        alignment(8);
        m_tmp_len.value = len * 8;
        int rd;
        while (m_tmp_len.value > 0) {
            rd = next(m_tmp_buf, m_tmp_off, m_tmp_len);
            for (int i = 0; i < rd - 7; i += 8, ++off) {
                val[off] = (((long) (m_tmp_buf.value[m_tmp_off.value + i] & 0xFF) << (long) (m_swap ? 0 : 56)) | ((long) (m_tmp_buf.value[m_tmp_off.value + i + 1] & 0xFF) << (long) (m_swap ? 8 : 48)) | ((long) (m_tmp_buf.value[m_tmp_off.value + i + 2] & 0xFF) << (long) (m_swap ? 16 : 40)) | ((long) (m_tmp_buf.value[m_tmp_off.value + i + 3] & 0xFF) << (long) (m_swap ? 24 : 32)) | ((long) (m_tmp_buf.value[m_tmp_off.value + i + 4] & 0xFF) << (long) (m_swap ? 32 : 24)) | ((long) (m_tmp_buf.value[m_tmp_off.value + i + 5] & 0xFF) << (long) (m_swap ? 40 : 16)) | ((long) (m_tmp_buf.value[m_tmp_off.value + i + 6] & 0xFF) << (long) (m_swap ? 48 : 8)) | ((long) (m_tmp_buf.value[m_tmp_off.value + i + 7] & 0xFF) << (long) (m_swap ? 56 : 0)));
            }
            if (rd % 8 != 0) {
                val[off] = 0L;
                int rcvd = rd % 8;
                rd -= rd % 8;
                int s = 0;
                for (; s < rcvd; ++s) {
                    val[off] = val[off] | ((long) (m_tmp_buf.value[m_tmp_off.value + rd + s] & 0xFF) << (long) (m_swap ? (8 * s) : (56 - 8 * s)));
                }
                int tmp = m_tmp_len.value - (8 - s);
                while (s < 8) {
                    m_tmp_len.value = 8 - s;
                    rd = next(m_tmp_buf, m_tmp_off, m_tmp_len);
                    for (int i = 0; i < rd; ++i, ++s) {
                        val[off] = val[off] | ((long) (m_tmp_buf.value[m_tmp_off.value + i] & 0xFF) << (long) (m_swap ? (8 * s) : (56 - 8 * s)));
                    }
                }
                m_tmp_len.value = tmp;
            }
        }
        postread(len * 8);
    }

    /**
     * Read an unsigned long long array
     */
    public void read_ulonglong_array(long[] val, int off, int len) {
        read_longlong_array(val, off, len);
    }

    /**
     * Read a float array
     */
    public void read_float_array(float[] val, int off, int len) {
        if (0 == len) {
            return;
        }
        alignment(4);
        m_tmp_len.value = len * 4;
        int rd;
        while (m_tmp_len.value > 0) {
            rd = next(m_tmp_buf, m_tmp_off, m_tmp_len);
            for (int i = 0; i < rd - 3; i += 4, ++off) {
                val[off] = Float.intBitsToFloat(((m_tmp_buf.value[m_tmp_off.value + i] & 0xFF) << (m_swap ? 0 : 24)) | ((m_tmp_buf.value[m_tmp_off.value + i + 1] & 0xFF) << (m_swap ? 8 : 16)) | ((m_tmp_buf.value[m_tmp_off.value + i + 2] & 0xFF) << (m_swap ? 16 : 8)) | ((m_tmp_buf.value[m_tmp_off.value + i + 3] & 0xFF) << (m_swap ? 24 : 0)));
            }
            if (rd % 4 != 0) {
                int m_tmpval = 0;
                val[off] = 0;
                int rcvd = rd % 4;
                rd -= rd % 4;
                int s = 0;
                for (; s < rcvd; ++s) {
                    m_tmpval = m_tmpval | ((m_tmp_buf.value[m_tmp_off.value + rd + s] & 0xFF) << (m_swap ? (8 * s) : (24 - 8 * s)));
                }
                val[off] = Float.intBitsToFloat(m_tmpval);
                int tmp = m_tmp_len.value - (4 - s);
                while (s < 4) {
                    m_tmp_len.value = 4 - s;
                    rd = next(m_tmp_buf, m_tmp_off, m_tmp_len);
                    for (int i = 0; i < rd; ++i, ++s) {
                        m_tmpval = m_tmpval | ((m_tmp_buf.value[m_tmp_off.value + i] & 0xFF) << (m_swap ? (8 * s) : (24 - 8 * s)));
                    }
                    val[off] = Float.intBitsToFloat(m_tmpval);
                }
                m_tmp_len.value = tmp;
            }
        }
        postread(len * 4);
    }

    /**
     * Read a double array
     */
    public void read_double_array(double[] val, int off, int len) {
        if (0 == len) {
            return;
        }
        alignment(8);
        m_tmp_len.value = len * 8;
        int rd;
        while (m_tmp_len.value > 0) {
            rd = next(m_tmp_buf, m_tmp_off, m_tmp_len);
            for (int i = 0; i < rd - 7; i += 8, ++off) {
                val[off] = Double.longBitsToDouble(((long) (m_tmp_buf.value[m_tmp_off.value + i] & 0xFF) << (long) (m_swap ? 0 : 56)) | ((long) (m_tmp_buf.value[m_tmp_off.value + i + 1] & 0xFF) << (long) (m_swap ? 8 : 48)) | ((long) (m_tmp_buf.value[m_tmp_off.value + i + 2] & 0xFF) << (long) (m_swap ? 16 : 40)) | ((long) (m_tmp_buf.value[m_tmp_off.value + i + 3] & 0xFF) << (long) (m_swap ? 24 : 32)) | ((long) (m_tmp_buf.value[m_tmp_off.value + i + 4] & 0xFF) << (long) (m_swap ? 32 : 24)) | ((long) (m_tmp_buf.value[m_tmp_off.value + i + 5] & 0xFF) << (long) (m_swap ? 40 : 16)) | ((long) (m_tmp_buf.value[m_tmp_off.value + i + 6] & 0xFF) << (long) (m_swap ? 48 : 8)) | ((long) (m_tmp_buf.value[m_tmp_off.value + i + 7] & 0xFF) << (long) (m_swap ? 56 : 0)));
            }
            if (rd % 8 != 0) {
                long tmpval = 0;
                val[off] = 0L;
                int rcvd = rd % 8;
                rd -= rd % 8;
                int s = 0;
                for (; s < rcvd; ++s) {
                    tmpval = tmpval | ((long) (m_tmp_buf.value[m_tmp_off.value + rd + s] & 0xFF) << (long) (m_swap ? (8 * s) : (56 - 8 * s)));
                }
                val[off] = Double.longBitsToDouble(tmpval);
                int tmp = m_tmp_len.value - (8 - s);
                while (s < 8) {
                    m_tmp_len.value = 8 - s;
                    rd = next(m_tmp_buf, m_tmp_off, m_tmp_len);
                    for (int i = 0; i < rd; ++i, ++s) {
                        tmpval = tmpval | ((long) (m_tmp_buf.value[m_tmp_off.value + i] & 0xFF) << (long) (m_swap ? (8 * s) : (56 - 8 * s)));
                    }
                    val[off] = Double.longBitsToDouble(tmpval);
                }
                m_tmp_len.value = tmp;
            }
        }
        postread(len * 8);
    }

    /**
     * Read a typecode
     */
    public org.omg.CORBA.TypeCode read_TypeCode() {
        return read_TypeCodeValue(new HashMap());
    }

    private org.omg.CORBA.TypeCode read_TypeCodeValue(HashMap cache) {
        TypeCode typeco = null;
        alignment(4);
        Integer kind_index = NumberCache.getInteger(m_index);
        int kind = read_ulong();
        String id, name;
        TypeCode contain;
        int length;
        switch(kind) {
            case org.omg.CORBA.TCKind._tk_null:
            case org.omg.CORBA.TCKind._tk_void:
            case org.omg.CORBA.TCKind._tk_short:
            case org.omg.CORBA.TCKind._tk_long:
            case org.omg.CORBA.TCKind._tk_ushort:
            case org.omg.CORBA.TCKind._tk_ulong:
            case org.omg.CORBA.TCKind._tk_float:
            case org.omg.CORBA.TCKind._tk_double:
            case org.omg.CORBA.TCKind._tk_boolean:
            case org.omg.CORBA.TCKind._tk_char:
            case org.omg.CORBA.TCKind._tk_octet:
            case org.omg.CORBA.TCKind._tk_any:
            case org.omg.CORBA.TCKind._tk_TypeCode:
            case org.omg.CORBA.TCKind._tk_Principal:
            case org.omg.CORBA.TCKind._tk_longlong:
            case org.omg.CORBA.TCKind._tk_ulonglong:
            case org.omg.CORBA.TCKind._tk_longdouble:
            case org.omg.CORBA.TCKind._tk_wchar:
                typeco = m_orb.get_primitive_tc(TCKind.from_int(kind));
                break;
            case org.omg.CORBA.TCKind._tk_objref:
                begin_encapsulation();
                id = read_string();
                name = read_string();
                end_encapsulation();
                typeco = m_orb.create_interface_tc(id, name);
                break;
            case org.omg.CORBA.TCKind._tk_struct:
                {
                    begin_encapsulation();
                    id = read_string();
                    name = read_string();
                    cache.put(kind_index, id);
                    length = read_ulong();
                    StructMember[] sm = new StructMember[length];
                    for (int i = 0; i < length; i++) {
                        sm[i] = new StructMember();
                        sm[i].name = read_string();
                        sm[i].type = read_TypeCodeValue(cache);
                    }
                    end_encapsulation();
                    typeco = m_orb.create_struct_tc(id, name, sm);
                }
                break;
            case org.omg.CORBA.TCKind._tk_union:
                {
                    begin_encapsulation();
                    id = read_string();
                    name = read_string();
                    cache.put(kind_index, id);
                    TypeCode disc = read_TypeCodeValue(cache);
                    int defaultused = read_long();
                    length = read_ulong();
                    UnionMember[] um = new UnionMember[length];
                    for (int i = 0; i < length; i++) {
                        um[i] = new UnionMember();
                        um[i].label = m_orb.create_any();
                        um[i].label.read_value(this, disc);
                        if (i == defaultused) {
                            um[i].label.insert_octet((byte) 0);
                        }
                        um[i].name = read_string();
                        um[i].type = read_TypeCodeValue(cache);
                    }
                    end_encapsulation();
                    typeco = m_orb.create_union_tc(id, name, disc, um);
                }
                break;
            case org.omg.CORBA.TCKind._tk_enum:
                {
                    begin_encapsulation();
                    id = read_string();
                    name = read_string();
                    length = read_ulong();
                    String[] members = new String[length];
                    for (int i = 0; i < length; i++) {
                        members[i] = read_string();
                    }
                    end_encapsulation();
                    typeco = m_orb.create_enum_tc(id, name, members);
                }
                break;
            case org.omg.CORBA.TCKind._tk_string:
                length = read_ulong();
                typeco = m_orb.create_string_tc(length);
                break;
            case org.omg.CORBA.TCKind._tk_sequence:
                begin_encapsulation();
                contain = read_TypeCodeValue(cache);
                length = read_ulong();
                end_encapsulation();
                typeco = m_orb.create_sequence_tc(length, contain);
                break;
            case org.omg.CORBA.TCKind._tk_array:
                begin_encapsulation();
                contain = read_TypeCodeValue(cache);
                length = read_ulong();
                end_encapsulation();
                typeco = m_orb.create_array_tc(length, contain);
                break;
            case org.omg.CORBA.TCKind._tk_alias:
                begin_encapsulation();
                id = read_string();
                name = read_string();
                cache.put(kind_index, id);
                contain = read_TypeCodeValue(cache);
                end_encapsulation();
                typeco = m_orb.create_alias_tc(id, name, contain);
                break;
            case org.omg.CORBA.TCKind._tk_except:
                {
                    begin_encapsulation();
                    id = read_string();
                    name = read_string();
                    cache.put(kind_index, id);
                    length = read_ulong();
                    StructMember[] sm = new StructMember[length];
                    for (int i = 0; i < length; i++) {
                        sm[i] = new StructMember();
                        sm[i].name = read_string();
                        sm[i].type = read_TypeCodeValue(cache);
                    }
                    end_encapsulation();
                    typeco = m_orb.create_exception_tc(id, name, sm);
                    cache.put(kind_index, typeco);
                }
                break;
            case org.omg.CORBA.TCKind._tk_wstring:
                length = read_ulong();
                typeco = m_orb.create_wstring_tc(length);
                break;
            case org.omg.CORBA.TCKind._tk_fixed:
                {
                    short digits = read_ushort();
                    short scale = read_ushort();
                    typeco = m_orb.create_fixed_tc(digits, scale);
                }
                break;
            case org.omg.CORBA.TCKind._tk_value:
                {
                    begin_encapsulation();
                    id = read_string();
                    name = read_string();
                    cache.put(kind_index, id);
                    short modifier = read_short();
                    contain = read_TypeCodeValue(cache);
                    length = read_ulong();
                    ValueMember[] vm = new ValueMember[length];
                    for (int i = 0; i < length; i++) {
                        vm[i] = new ValueMember();
                        vm[i].name = read_string();
                        vm[i].type = read_TypeCodeValue(cache);
                        vm[i].access = read_short();
                    }
                    end_encapsulation();
                    typeco = m_orb.create_value_tc(id, name, modifier, contain, vm);
                }
                break;
            case org.omg.CORBA.TCKind._tk_value_box:
                begin_encapsulation();
                id = read_string();
                name = read_string();
                cache.put(kind_index, id);
                contain = read_TypeCodeValue(cache);
                end_encapsulation();
                typeco = m_orb.create_value_box_tc(id, name, contain);
                break;
            case org.omg.CORBA.TCKind._tk_native:
                begin_encapsulation();
                id = read_string();
                name = read_string();
                end_encapsulation();
                typeco = m_orb.create_native_tc(id, name);
                break;
            case org.omg.CORBA.TCKind._tk_abstract_interface:
                begin_encapsulation();
                id = read_string();
                name = read_string();
                end_encapsulation();
                typeco = m_orb.create_abstract_interface_tc(id, name);
                break;
            case 0xffffffff:
                {
                    int oldidx = m_index + read_long();
                    Object lookup = cache.get(NumberCache.getInteger(oldidx));
                    if (lookup instanceof String) {
                        typeco = m_orb.create_recursive_tc((String) lookup);
                    } else if (lookup != null) {
                        typeco = (TypeCode) lookup;
                    } else {
                        cancel(new org.omg.CORBA.MARSHAL("Invalid typecode offset", IIOPMinorCodes.MARSHAL_TC_OFFSET, CompletionStatus.COMPLETED_MAYBE));
                    }
                }
                break;
            default:
                cancel(new org.omg.CORBA.MARSHAL("Invalid typecode kind", IIOPMinorCodes.MARSHAL_TC_KIND, CompletionStatus.COMPLETED_MAYBE));
        }
        cache.put(kind_index, typeco);
        return typeco;
    }

    /**
     * Read an any
     */
    public Any read_any() {
        TypeCode typeco = read_TypeCode();
        org.omg.CORBA.Any any = m_orb.create_any();
        any.read_value(this, typeco);
        return any;
    }

    /**
     * Read a principal
     */
    public org.omg.CORBA.Principal read_Principal() {
        byte[] b = new byte[read_ulong()];
        read_octet_array(b, 0, b.length);
        return new org.openorb.orb.core.Principal(b);
    }

    /**
     * Read an object
     */
    public org.omg.CORBA.Object read_Object() {
        org.omg.IOP.IOR ior = org.omg.IOP.IORHelper.read(this);
        if (ior.type_id.length() == 0 && ior.profiles.length == 0) {
            return null;
        }
        return new org.openorb.orb.core.ObjectStub(m_orb, ior);
    }

    /**
     * Read an Object
     */
    public org.omg.CORBA.Object read_Object(Class clz) {
        org.omg.IOP.IOR ior = org.omg.IOP.IORHelper.read(this);
        if (ior.type_id.length() == 0 && ior.profiles.length == 0) {
            return null;
        }
        if (clz == null || !org.omg.CORBA.Object.class.isAssignableFrom(clz)) {
            return new org.openorb.orb.core.ObjectStub(m_orb, ior);
        }
        org.omg.CORBA.portable.ObjectImpl ret = null;
        try {
            ret = (org.omg.CORBA.portable.ObjectImpl) clz.newInstance();
        } catch (final Exception ex) {
            cancel(ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM("Unable to instantiate class", IIOPMinorCodes.BAD_PARAM_OBJ_CLASS, CompletionStatus.COMPLETED_NO), ex));
        }
        org.omg.CORBA.portable.Delegate ndeleg = new org.openorb.orb.core.Delegate(m_orb, ior);
        ret._set_delegate(ndeleg);
        return ret;
    }

    /**
     * Read a context
     */
    public org.omg.CORBA.Context read_Context() {
        org.omg.CORBA.NVList nv = m_orb.create_list(0);
        int max = (read_ulong() / 2);
        for (int i = 0; i < max; i++) {
            org.omg.CORBA.Any a = m_orb.create_any();
            String name = read_string();
            a.insert_string(read_string());
            nv.add_value(name, a, 0);
        }
        org.omg.CORBA.Context context = new org.openorb.orb.core.dii.Context("", null, m_orb);
        context.set_values(nv);
        return context;
    }

    /**
     * Read a fixed
     *
     * @deprecated Loses scale and precision, see
     *              http://www.omg.org/issues/issue3431.txt
     */
    public java.math.BigDecimal read_fixed() {
        return read_fixed((short) -1, (short) -1);
    }

    /**
     * read a fixed.
     */
    public java.math.BigDecimal read_fixed(org.omg.CORBA.TypeCode type) {
        try {
            return read_fixed(type.fixed_digits(), type.fixed_scale());
        } catch (final org.omg.CORBA.TypeCodePackage.BadKind ex) {
            cancel(ExceptionTool.initCause(new org.omg.CORBA.BAD_PARAM("Typecode is not fixed type", IIOPMinorCodes.BAD_PARAM_FIXED_TYPE, CompletionStatus.COMPLETED_NO), ex));
            return null;
        }
    }

    /**
     * read a fixed.
     */
    public java.math.BigDecimal read_fixed(short digits, short scale) {
        StringBuffer value = new StringBuffer("0");
        int count = 0;
        boolean body = false;
        boolean negative = false;
        while (true) {
            byte v = read_octet();
            int hi = (v >> 4) & 0xF;
            int lo = v & 0xF;
            if (hi > 9) {
                cancel(new org.omg.CORBA.MARSHAL("Error in fixed format", IIOPMinorCodes.MARSHAL_FIXED, CompletionStatus.COMPLETED_MAYBE));
            }
            if (body || hi > 0) {
                value.append((char) ('0' + hi));
                count++;
            }
            if (lo < 0xA) {
                value.append((char) ('0' + lo));
                count++;
            } else if (lo == 0xD) {
                negative = true;
                break;
            } else if (lo == 0xC) {
                break;
            } else {
                cancel(new org.omg.CORBA.MARSHAL("Error in fixed format", IIOPMinorCodes.MARSHAL_FIXED, CompletionStatus.COMPLETED_MAYBE));
            }
            body = true;
        }
        if (digits >= 0 && count > digits) {
            cancel(new org.omg.CORBA.MARSHAL("Fixed data does not match type", IIOPMinorCodes.MARSHAL_FIXED, CompletionStatus.COMPLETED_MAYBE));
        }
        java.math.BigDecimal ret;
        try {
            ret = new java.math.BigDecimal(value.toString());
        } catch (final NumberFormatException ex) {
            cancel(ExceptionTool.initCause(new org.omg.CORBA.MARSHAL("Error in fixed format", IIOPMinorCodes.MARSHAL_FIXED, CompletionStatus.COMPLETED_MAYBE), ex));
            return null;
        }
        if (negative) {
            ret = ret.negate();
        }
        if (scale > 0) {
            ret = ret.movePointLeft(scale);
        }
        return ret;
    }

    /**
     * Read a value from a CDR stream
     */
    public java.io.Serializable read_value() {
        return read_value(read_long(), null, null, null, null);
    }

    /**
     * Read a value from a CDR stream
     */
    public java.io.Serializable read_value(String rep_id) {
        return read_value(read_long(), rep_id, null, null, null);
    }

    /**
     * Read a value from a CDR stream
     */
    public java.io.Serializable read_value(Class clz) {
        return read_value(read_long(), null, clz, null, null);
    }

    /**
     * Read a value from a CDR stream
     */
    public java.io.Serializable read_value(org.omg.CORBA.portable.BoxedValueHelper boxhelp) {
        return read_value(read_long(), null, null, boxhelp, null);
    }

    /**
     * Implementation of value reading.
     */
    private java.io.Serializable read_value(int tag, String arg_repo_id, Class clz, org.omg.CORBA.portable.BoxedValueHelper boxhelp, java.io.Serializable target) {
        if (tag == 0) {
            return null;
        }
        if (m_value_cache == null) {
            m_value_cache = new HashMap();
        }
        if (tag == 0xffffffff) {
            int loc = m_index + read_long();
            Object val = m_value_cache.get(NumberCache.getInteger(loc));
            if (val instanceof org.omg.CORBA.MARSHAL) {
                cancel((org.omg.CORBA.MARSHAL) val);
            }
            if (val instanceof java.io.Serializable) {
                return (java.io.Serializable) val;
            }
            cancel(new org.omg.CORBA.portable.IndirectionException(loc));
        }
        if (!((tag >= 0x7fffff00) && (tag <= 0x7fffffff))) {
            cancel(new org.omg.CORBA.MARSHAL("Invalid value tag: 0x" + HexPrintStream.toHex(tag), IIOPMinorCodes.MARSHAL_VALUE, CompletionStatus.COMPLETED_MAYBE));
        }
        int tag_index = m_index - 4;
        String codeBase = null;
        if ((tag & CODEBASE) == CODEBASE) {
            if (peek_long() == 0xffffffff) {
                read_long();
                int loc = m_index + read_long();
                Object val = m_value_cache.get(NumberCache.getInteger(loc));
                if (val == null || !(val instanceof String)) {
                    cancel(new org.omg.CORBA.portable.IndirectionException(loc));
                }
                codeBase = (String) val;
            } else {
                int codebase_index = m_index;
                codeBase = read_string();
                m_value_cache.put(NumberCache.getInteger(codebase_index), codeBase);
            }
        }
        String[] ids = read_typeids(tag);
        boolean old_in_chunked_value = m_in_chunked_value;
        int enclosing_level = m_value_level;
        if ((tag & CHUNK) == CHUNK) {
            int len = peek_long();
            if (!((len >= 0x7fffff00) && (len <= 0x7fffffff))) {
                begin_value_chunk(read_ulong());
            }
            m_in_chunked_value = true;
        }
        ++m_value_level;
        m_value_indirect = tag_index;
        try {
            if (boxhelp != null) {
                java.io.Serializable ret = boxhelp.read_value(this);
                m_value_cache.put(NumberCache.getInteger(m_value_indirect), ret);
                m_value_indirect = -1;
                return ret;
            } else if (target != null) {
                return read_value(target);
            } else {
                java.io.Serializable ret = null;
                if (ids != null) {
                    for (int i = 0; i < ids.length; ++i) {
                        if ((ret = read_value_withtype(ids[i], codeBase, clz)) != null) {
                            break;
                        }
                        if (arg_repo_id != null && ids[i].equals(arg_repo_id)) {
                            break;
                        }
                    }
                } else if (arg_repo_id != null) {
                    ret = read_value_withtype(arg_repo_id, codeBase, clz);
                }
                if (ret == null) {
                    org.omg.CORBA.MARSHAL ex = new org.omg.CORBA.MARSHAL("Unable to locate value factory", org.omg.CORBA.OMGVMCID.value | 1, CompletionStatus.COMPLETED_YES);
                    m_value_cache.put(NumberCache.getInteger(m_value_indirect), ex);
                    m_value_indirect = -1;
                    if (m_continue_level > 0) {
                        throw ex;
                    } else {
                        cancel(ex);
                    }
                }
                if (clz != null && !clz.isInstance(ret)) {
                    cancel(new org.omg.CORBA.BAD_PARAM("Returned type does not match expected type", IIOPMinorCodes.BAD_PARAM_VALUE_CLASS, CompletionStatus.COMPLETED_YES));
                }
                return ret;
            }
        } finally {
            if ((tag & CHUNK) == CHUNK) {
                while (m_value_level > enclosing_level) {
                    if (m_encaps_remain > 0) {
                        force_skip(m_encaps_remain);
                    }
                    m_pending_value_reopen = false;
                    int endtag = read_long();
                    if (endtag >= 0x7fffff00 && endtag <= 0x7FFFFFFF) {
                        try {
                            ++m_continue_level;
                            read_value(endtag, null, null, null, null);
                        } catch (final org.omg.CORBA.MARSHAL ex) {
                            if (ex.minor != (org.omg.CORBA.OMGVMCID.value | 1)) {
                                throw ExceptionTool.initCause(new org.omg.CORBA.MARSHAL(ex.getMessage(), ex.minor, ex.completed), ex);
                            }
                        } finally {
                            --m_continue_level;
                        }
                    } else if (endtag > 0) {
                        begin_value_chunk(endtag);
                    } else {
                        m_value_level = -endtag - 1;
                    }
                }
                m_in_chunked_value = old_in_chunked_value;
                m_pending_value_reopen = old_in_chunked_value;
            } else {
                m_value_level = enclosing_level;
            }
        }
    }

    private void begin_value_chunk(int len) {
        m_encaps_stack.addLast(NumberCache.getInteger(m_encaps_remain));
        m_encaps_stack.addLast(NumberCache.getInteger(m_index));
        m_encaps_stack.addLast(Boolean.TRUE);
        m_encaps_remain = len;
    }

    public String[] read_typeids(int tag) {
        String[] ids = null;
        readids: if ((tag & MASK_TYPE_INFORMATION) != NO_TYPE_INFORMATION) {
            if ((tag & MASK_TYPE_INFORMATION) == MULTIPLE_TYPE_INFORMATION) {
                int count = read_long();
                if (count == 0xFFFFFFFF) {
                    int loc = m_index + read_long();
                    Object val = m_value_cache.get(NumberCache.getInteger(loc));
                    if (val == null || !(val instanceof String[])) {
                        cancel(new org.omg.CORBA.portable.IndirectionException(loc));
                    }
                    ids = (String[]) val;
                    m_value_cache.put(NumberCache.getInteger(m_index - 4), ids);
                    break readids;
                } else {
                    ids = new String[count];
                    m_value_cache.put(NumberCache.getInteger(m_index - 4), ids);
                }
            } else {
                ids = new String[1];
            }
            for (int i = 0; i < ids.length; ++i) {
                if (peek_long() == 0xFFFFFFFF) {
                    read_long();
                    int loc = m_index + read_long();
                    Object val = m_value_cache.get(NumberCache.getInteger(loc));
                    if (val == null || !(val instanceof String)) {
                        cancel(new org.omg.CORBA.portable.IndirectionException(loc));
                    }
                    ids[i] = (String) val;
                    m_value_cache.put(NumberCache.getInteger(m_index - 4), ids[i]);
                } else {
                    alignment(4);
                    int type_index = m_index;
                    ids[i] = read_string();
                    m_value_cache.put(NumberCache.getInteger(type_index), ids[i]);
                }
            }
        }
        return ids;
    }

    private java.io.Serializable read_value_withtype(String repo_id, String codeBase, Class clz) {
        org.omg.CORBA.portable.ValueFactory factory = ((org.omg.CORBA_2_3.ORB) m_orb).lookup_value_factory(repo_id);
        if (factory != null) {
            return factory.read_value(this);
        }
        int temp = m_value_indirect;
        java.io.Serializable ret = value_extended_unmarshal(m_value_indirect, clz, repo_id, codeBase, null);
        if (ret != null) {
            m_value_cache.put(NumberCache.getInteger(temp), ret);
            return ret;
        }
        org.omg.CORBA.portable.BoxedValueHelper boxhelp = null;
        String boxname = null;
        try {
            boxname = RepoIDHelper.idToClass(repo_id, RepoIDHelper.TYPE_HELPER);
            boxhelp = (org.omg.CORBA.portable.BoxedValueHelper) Thread.currentThread().getContextClassLoader().loadClass(boxname).newInstance();
        } catch (Exception ex) {
            getLogger().error("Unable to load or instantiate value-box helper class '" + boxname + "' (TypeId: '" + repo_id + "')!", ex);
        }
        if (boxhelp != null) {
            ret = boxhelp.read_value(this);
            m_value_cache.put(NumberCache.getInteger(m_value_indirect), ret);
            m_value_indirect = -1;
            return ret;
        }
        if (codeBase != null && (factory = loadFactoryWithID(repo_id, codeBase)) != null) {
            return factory.read_value(this);
        }
        return null;
    }

    /**
     * Read value state from the stream. Called externally by factories
     * with uninitialized value. One of the other read_value functions
     * always appears above it in the call stack.
     */
    public java.io.Serializable read_value(java.io.Serializable value) {
        if (m_value_indirect < 0) {
            return read_value(read_long(), null, null, null, value);
        }
        m_value_cache.put(NumberCache.getInteger(m_value_indirect), value);
        if (value instanceof org.omg.CORBA.portable.CustomValue) {
            ((org.omg.CORBA.portable.CustomValue) value).unmarshal(new org.openorb.orb.core.DataInputStream(this));
        } else if (value instanceof org.omg.CORBA.portable.StreamableValue) {
            ((org.omg.CORBA.portable.StreamableValue) value)._read(this);
        } else {
            cancel(new org.omg.CORBA.BAD_PARAM("Unable to read value into class", IIOPMinorCodes.BAD_PARAM_VALUE_CLASS, CompletionStatus.COMPLETED_YES));
        }
        return value;
    }

    /**
     * This function should be overloaded by base types to allow marshaling
     * of extended value types, RMI over IIOP for example. It should return null
     * if the repository ID or class is not unmarshaled by the function. All
     * calls to this function with an offset equal to a previous value must
     * not unmarshal from the stream.
     */
    protected java.io.Serializable value_extended_unmarshal(final int offset, final Class clz, final String repo_id, final String codeBase, final RunTime sendingCtxt) {
        java.io.Serializable result = null;
        if (repo_id.startsWith("RMI:")) {
            m_code_base = codeBase;
            final CDRInputStream thisStream = this;
            result = (java.io.Serializable) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {

                public Object run() {
                    return s_handler.readValue(thisStream, offset, clz, repo_id, sendingCtxt);
                }
            });
        }
        return result;
    }

    /**
     * Read an abstract interface
     *
     * @return the readed object ( An CORBA.Object or a value type )
     */
    public Object read_abstract_interface() {
        if (read_boolean()) {
            return read_Object();
        } else {
            return read_value();
        }
    }

    /**
     * Read an abstract interface
     *
     * @param clz the stub class for an object
     * @return the readed object ( An CORBA.Object or a value type )
     */
    public Object read_abstract_interface(Class clz) {
        if (read_boolean()) {
            return read_Object(clz);
        } else {
            return read_value();
        }
    }

    /**
     * This operation is used to add an indirection for an unmarshaled value type.
     * This method updates the indirection table and puts the offset where the data
     * starts in the IIOP stream to the hash table. Once an indirection is found
     * the data can be accessed by the offset.
     * It is called by the ValueHander as soon as the target object is instantiated.
     */
    public void addIndirect(int offset, Object value) {
        getValueCache().put(NumberCache.getInteger(offset), value);
    }

    /**
     * Return the codebase for the value.
     * This method is called from the value handler.
     *
     * @return The URL describing the place where to load the class for the value.
     */
    public String getValueCodebase() {
        if (m_code_base != null) {
            return m_code_base;
        }
        return get_codebase();
    }

    /**
     * internal function. Called after every read_ operation. Pops
     * encapsulation levels and increments index.
     */
    private void postread(int len) {
        m_index += len;
        if (m_encaps_remain > 0) {
            m_encaps_remain -= len;
            while (m_encaps_remain == 0) {
                boolean value_chunk = ((Boolean) m_encaps_stack.removeLast()).booleanValue();
                int enclen = m_index - ((Integer) m_encaps_stack.removeLast()).intValue();
                m_encaps_remain = ((Integer) m_encaps_stack.removeLast()).intValue() - enclen;
                if (value_chunk) {
                    m_pending_value_reopen = true;
                } else {
                    m_swap = ((Boolean) m_encaps_stack.removeLast()).booleanValue();
                    if (!m_encaps_stack.isEmpty()) {
                        m_in_chunked_value = ((Boolean) m_encaps_stack.getLast()).booleanValue();
                    }
                    m_pending_encaps_close++;
                }
                if (m_encaps_remain < 0) {
                    m_encaps_remain = -1;
                }
            }
        }
    }

    /**
     * Load a OBV factory from its ID.
     */
    protected org.omg.CORBA.portable.ValueFactory loadFactoryWithID(String id, String url) {
        String factoryName = RepoIDHelper.idToClass(id, RepoIDHelper.TYPE_DEFAULT_FACTORY);
        Class clzHelper;
        try {
            clzHelper = loadClassFromURL(factoryName, url);
            if (url != null) {
                clzHelper = loadClassFromURL(factoryName, url);
            } else {
                clzHelper = Thread.currentThread().getContextClassLoader().loadClass(factoryName);
            }
            if (clzHelper != null) {
                return (org.omg.CORBA.portable.ValueFactory) clzHelper.newInstance();
            }
        } catch (Exception ex) {
            getLogger().error("Unexpected exception", ex);
        }
        return null;
    }

    /**
     * Try to load a class from an URL site.
     */
    private Class loadClassFromURL(String className, String url) {
        try {
            if (url == null) {
                return Thread.currentThread().getContextClassLoader().loadClass(className);
            } else {
                ClassLoader loader;
                loader = (ClassLoader) m_value_cache.get(url);
                if (loader == null) {
                    java.util.StringTokenizer token = new java.util.StringTokenizer(url, " ");
                    java.net.URL[] urls = new java.net.URL[token.countTokens()];
                    int pos = 0;
                    for (int i = 0; i < urls.length; ++i) {
                        String tok = token.nextToken();
                        try {
                            urls[pos] = new java.net.URL(tok);
                            ++pos;
                        } catch (java.net.MalformedURLException ex) {
                            getLogger().error("Malformed URL: " + tok, ex);
                        }
                    }
                    if (pos != urls.length) {
                        java.net.URL[] old = urls;
                        urls = new java.net.URL[pos];
                        System.arraycopy(old, 0, urls, 0, pos);
                    }
                    loader = new java.net.URLClassLoader(urls);
                    m_value_cache.put(url, loader);
                }
                Class clz = loader.loadClass(className);
                return clz;
            }
        } catch (final ClassNotFoundException ex) {
            getLogger().error("Unable to find class from an URL : " + className, ex);
        }
        return null;
    }

    /**
     * Provides access to the value cache for subclasses (e.g. RMIInputStream)
     * @return value cache map
     */
    protected Map getValueCache() {
        return m_value_cache;
    }
}
