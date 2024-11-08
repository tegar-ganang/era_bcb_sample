package org.openorb.orb.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InterruptedIOException;
import java.io.EOFException;
import org.omg.CORBA.IntHolder;
import org.omg.CORBA.OctetSeqHolder;
import org.openorb.orb.util.Trace;

/**
 * A storage buffer holds binary data for further processing. MarshalBuffers
 * and BufferSource objects output a stream of StorageBuffers.
 * The SocketQueue class uses StorageBuffer parameters when sending data.
 * CDRInpuStream uses StorageBuffer instance to unmarshal GIOP data from.
 * This class always represents a full IIOP message and it is used to
 * the client side for unmarshalling and on the server side for marshalling
 * GIOP into the IIOP.
 *
 * @author Unknown
 */
public class StorageBuffer {

    private int m_avail;

    private Scrap m_mark;

    private int m_markavail;

    private Scrap m_head;

    private Scrap m_temp_head = new Scrap();

    private Scrap m_tail;

    private boolean m_read_write_mode;

    /**
     * Public constructor, Create storage buffer from bytes. The bytes are
     * considered read-only.
     */
    public StorageBuffer(byte[] buf, int off, int len) {
        if (off < 0 || len < 0 || len + off > buf.length) {
            throw new IndexOutOfBoundsException();
        }
        m_head = new Scrap();
        m_avail = len;
        m_head.m_fBuffer = buf;
        m_head.m_fOffset = off;
        m_head.m_fLength = len;
        m_head.m_fPosition = len;
        m_head.m_fMode = Scrap.SCRAP_MODE_READONLY;
    }

    /**
     * public constructor, read from stream.
     */
    public StorageBuffer(InputStream is, int total_len) throws IOException {
        this(null, 0, 0, is, total_len);
    }

    /**
     * Public constructor, read part from buffer and remainder from stream.
     * A whole buffer is always read. If the reading thread is interrupted
     * the buffer will be completely read and the interrupted status reset.
     *
     * @param buf Prefix bytes. Copied.
     * @param off Offset into prefix buffer.
     * @param len Length of data in prefix buffer.
     * @param is Input stream for remainder of data.
     * @param total_len total length including data from prexix and stream.
     */
    public StorageBuffer(byte[] buf, int off, int len, InputStream is, int total_len) throws IOException {
        if (buf != null && (off < 0 || len < 0 || len + off > buf.length || (is != null && len > total_len))) {
            throw new IndexOutOfBoundsException();
        }
        Scrap t = new Scrap();
        Scrap h = t;
        boolean interrupt = false;
        m_avail = total_len;
        if (buf == null || len == 0) {
            t.m_fPosition = 0;
            t.m_fBuffer = null;
        } else if (is == null) {
            t.m_fBuffer = new byte[len];
            t.m_fOffset = 0;
            t.m_fLength = len;
            t.m_fPosition = len;
            t.m_fMode = Scrap.SCRAP_MODE_NORMAL;
            System.arraycopy(buf, off, t.m_fBuffer, 0, len);
            m_head = h;
            m_avail = len;
            return;
        } else {
            int alloc = (total_len > Scrap.SCRAP_SIZE_DEFAULT) ? Scrap.SCRAP_SIZE_DEFAULT : total_len;
            t.m_fBuffer = new byte[alloc];
            t.m_fOffset = 0;
            t.m_fLength = alloc;
            t.m_fPosition = alloc;
            t.m_fMode = Scrap.SCRAP_MODE_NORMAL;
            System.arraycopy(buf, off, t.m_fBuffer, 0, len);
            alloc -= len;
            total_len -= len;
            int pos = len;
            while (alloc > 0) {
                int ret;
                try {
                    ret = is.read(t.m_fBuffer, pos, alloc);
                } catch (InterruptedIOException ex) {
                    interrupt = true;
                    ret = ex.bytesTransferred;
                }
                if (ret >= 0) {
                    pos += ret;
                    alloc -= ret;
                    total_len -= ret;
                } else {
                    if (interrupt) {
                        Thread.currentThread().interrupt();
                    }
                    throw new EOFException("EOF reached when reading message");
                }
            }
        }
        while (total_len > 0) {
            int alloc = (total_len > Scrap.SCRAP_SIZE_DEFAULT) ? Scrap.SCRAP_SIZE_DEFAULT : total_len;
            Scrap nex = new Scrap();
            nex.m_fBuffer = new byte[alloc];
            nex.m_fOffset = 0;
            nex.m_fLength = alloc;
            nex.m_fPosition = t.m_fPosition + alloc;
            nex.m_fMode = Scrap.SCRAP_MODE_NORMAL;
            int pos = 0;
            while (alloc > 0) {
                int ret;
                try {
                    ret = is.read(nex.m_fBuffer, pos, alloc);
                } catch (InterruptedIOException ex) {
                    interrupt = true;
                    ret = ex.bytesTransferred;
                }
                if (ret >= 0) {
                    pos += ret;
                    alloc -= ret;
                    total_len -= ret;
                } else {
                    if (interrupt) {
                        Thread.currentThread().interrupt();
                    }
                    throw new EOFException("error: EOF reached when reading message");
                }
            }
            t.m_fNext = nex;
            t = nex;
        }
        if (interrupt) {
            Thread.currentThread().interrupt();
        }
        if (h.m_fBuffer == null) {
            m_head = h.m_fNext;
        } else {
            m_head = h;
        }
    }

    /**
     * package protected constructor. Used by MarshallBuffer.
     */
    StorageBuffer(Scrap head, int len) {
        m_head = head;
        m_tail = null;
        m_avail = len;
    }

    /**
     * package protected constructor. Used by MarshallBuffer when
     * creating preview buffers.
     */
    StorageBuffer(Scrap head, Scrap tail) {
        m_head = head;
        m_tail = tail;
        m_avail = tail.m_fPosition - tail.m_fLength - head.m_fPosition + head.m_fLength;
    }

    /**
     * Available bytes
     */
    public int available() {
        return m_avail;
    }

    /**
     * Writes entire buffer to output stream.
     */
    public void writeTo(OutputStream os) throws IOException {
        if (m_head == null) {
            throw new EOFException("Buffer is empty");
        }
        boolean interrupt = Thread.interrupted();
        Trace.isGIOPHeaderOK(m_head.m_fBuffer, 0);
        if (m_read_write_mode) {
            while (m_head != m_tail) {
                if (m_head.m_fMode == Scrap.SCRAP_MODE_READONLY) {
                    byte[] tmp = new byte[m_head.m_fLength];
                    System.arraycopy(m_head.m_fBuffer, m_head.m_fOffset, tmp, 0, m_head.m_fLength);
                    m_head.m_fBuffer = tmp;
                    m_head.m_fOffset = 0;
                    m_head.m_fMode = Scrap.SCRAP_MODE_NORMAL;
                }
                int d = 0;
                while (d < m_head.m_fLength) {
                    try {
                        os.write(m_head.m_fBuffer, m_head.m_fOffset + d, m_head.m_fLength - d);
                        break;
                    } catch (InterruptedIOException ex) {
                        interrupt = true;
                        d += ex.bytesTransferred;
                    }
                }
                m_head = m_head.m_fNext;
            }
        } else {
            while (m_head != m_tail) {
                int d = 0;
                while (d < m_head.m_fLength) {
                    try {
                        os.write(m_head.m_fBuffer, m_head.m_fOffset + d, m_head.m_fLength - d);
                        break;
                    } catch (InterruptedIOException ex) {
                        interrupt = true;
                        d += ex.bytesTransferred;
                    }
                }
                m_head = m_head.m_fNext;
            }
        }
        m_head = null;
        m_avail = 0;
        if (interrupt) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Read the next piece of the buffer. This buffer is modifyable if
     * in m_read_write_mode.
     *
     * @param buf Out parameter, holds pointer to scratch space on return.
     * @param off Out parameter, holds buffer offset on return.
     * @param len InOut parameter, Length of requested buffer. Holds
     * unallocated length on return. (ie: original - return)
     *
     * @return Length of returned buf. If end of buffer has been reached
     * -1 is returned.
     */
    public int next(OctetSeqHolder buf, IntHolder off, IntHolder len) {
        if (m_head == null) {
            buf.value = null;
            off.value = 0;
            return -1;
        }
        if (len.value < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (m_read_write_mode && m_head.m_fMode == Scrap.SCRAP_MODE_READONLY) {
            byte[] copy = new byte[m_head.m_fLength];
            System.arraycopy(m_head.m_fBuffer, m_head.m_fOffset, copy, 0, m_head.m_fLength);
            m_head.m_fBuffer = copy;
            m_head.m_fOffset = 0;
            m_head.m_fMode = Scrap.SCRAP_MODE_NORMAL;
        }
        buf.value = m_head.m_fBuffer;
        off.value = m_head.m_fOffset;
        if (len.value < m_head.m_fLength) {
            int olen = len.value;
            len.value = 0;
            if ((m_mark == null && m_tail == null) || m_head == m_temp_head) {
                m_head.m_fOffset += olen;
                m_head.m_fLength -= olen;
            } else {
                m_temp_head.m_fBuffer = m_head.m_fBuffer;
                m_temp_head.m_fOffset = m_head.m_fOffset + olen;
                m_temp_head.m_fLength = m_head.m_fLength - olen;
                m_temp_head.m_fMode = m_head.m_fMode | Scrap.SCRAP_MODE_SHARED;
                m_temp_head.m_fPosition = m_head.m_fPosition;
                m_temp_head.m_fNext = m_head.m_fNext;
                m_head = m_temp_head;
            }
            m_avail -= olen;
            return olen;
        } else {
            int olen = m_head.m_fLength;
            len.value -= olen;
            if ((m_head = m_head.m_fNext) == m_tail) {
                m_head = null;
            }
            m_avail -= olen;
            return olen;
        }
    }

    /**
     * Skip over bytes.
     *
     * @param len InOut parameter, Length of requested skip. Holds
     * unskipped length on return. (ie: original - return)
     *
     * @return Length of returned buf. If end of buffer has been reached
     * -1 is returned.
     */
    public int skip(IntHolder len) {
        if (m_head == null) {
            return -1;
        }
        int skip = 0;
        while (m_head != null && len.value >= m_head.m_fLength) {
            len.value -= m_head.m_fLength;
            m_avail -= m_head.m_fLength;
            skip += m_head.m_fLength;
            if ((m_head = m_head.m_fNext) == m_tail) {
                m_head = null;
            }
        }
        if (m_head != null && len.value > 0) {
            if ((m_mark == null && m_tail == null) || m_head == m_temp_head) {
                m_head.m_fOffset += len.value;
                m_head.m_fLength -= len.value;
            } else {
                m_temp_head.m_fBuffer = m_head.m_fBuffer;
                m_temp_head.m_fOffset = m_head.m_fOffset + len.value;
                m_temp_head.m_fLength = m_head.m_fLength - len.value;
                m_temp_head.m_fMode = m_head.m_fMode | Scrap.SCRAP_MODE_SHARED;
                m_temp_head.m_fPosition = m_head.m_fPosition;
                m_temp_head.m_fNext = m_head.m_fNext;
                m_head = m_temp_head;
            }
            skip += len.value;
            m_avail -= len.value;
            len.value = 0;
        }
        return skip;
    }

    /**
     * Move the entire buffer into a single byte array. Calls to this function
     * should generaly be avoided, call mark and next to iterate over the buffer
     * instead.
     *
     * @return the linearized buffer or null when an exception during
     *         System.arraycopy() occured.
     */
    public byte[] linearize() {
        if (m_head == null) {
            return new byte[0];
        }
        if (m_head.m_fLength != m_head.m_fBuffer.length || m_head.m_fNext != m_tail || m_head.m_fMode != Scrap.SCRAP_MODE_NORMAL || m_head.m_fOffset != 0) {
            byte[] buf = new byte[m_avail];
            Scrap nex = m_head;
            int dest = 0;
            while (nex != m_tail) {
                try {
                    System.arraycopy(nex.m_fBuffer, nex.m_fOffset, buf, dest, nex.m_fLength);
                } catch (Exception ex) {
                    Trace.signalIllegalCondition(null, "An exception occured during linearization!");
                    return null;
                }
                dest += nex.m_fLength;
                nex = nex.m_fNext;
            }
            m_head.m_fBuffer = buf;
            m_head.m_fOffset = 0;
            m_head.m_fMode = Scrap.SCRAP_MODE_NORMAL;
            m_head.m_fPosition = m_head.m_fPosition - m_head.m_fLength + m_avail;
            m_head.m_fLength = m_avail;
            m_head.m_fNext = m_tail;
        }
        return m_head.m_fBuffer;
    }

    /**
     * Check if the buffer is in read write mode. In read write mode buffers
     * returned from the next operation can be safely modified, modifying the
     * contents of the buffer.
     */
    public boolean isReadWriteMode() {
        return m_read_write_mode;
    }

    /**
     * Set read write mode. In read write mode buffers
     * returned from the next operation can be safely modified, modifying the
     * contents of the buffer.
     */
    public void setReadWriteMode(boolean readWriteMode) {
        m_read_write_mode = readWriteMode;
    }

    /**
     * Mark current buffer position for future reset.
     */
    public boolean mark() {
        if (m_mark == null) {
            m_mark = m_head;
            m_markavail = m_avail;
            return true;
        }
        return false;
    }

    /**
     * reset buffer position to position marked with mark operation.
     */
    public boolean reset() {
        if (m_mark != null) {
            m_head = m_mark;
            m_avail = m_markavail;
            m_mark = null;
            return true;
        }
        return false;
    }
}
