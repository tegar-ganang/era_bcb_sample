package jreceiver.util;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.logging.*;

/**
 * A non-blocking OutputStream writer
 *
 * @author Reed Esau
 * @version $Revision: 1.2 $ $Date: 2002/12/29 00:44:07 $
 */
public class ExpiringWriter {

    /**
     * ctor
     */
    public ExpiringWriter(OutputStream os, long timeout) {
        m_os = os;
        m_timeout = timeout;
        m_target = new Target();
    }

    /**
     * Create a worker thread to do the writing and wait for it to either complete or timeout.
     */
    public synchronized boolean write(byte[] buf, int off, int len) throws IOException {
        if (m_thread != null) throw new IllegalStateException("cannot reuse active writer");
        m_buf = buf;
        m_off = off;
        m_len = len;
        m_is_done = false;
        m_exception_message = null;
        m_thread = new Thread(m_target);
        if (log.isDebugEnabled()) log.debug("write: starting thread, m_thread=" + m_thread.getName());
        m_thread.start();
        try {
            try {
                m_thread.join(m_timeout);
                if (log.isDebugEnabled()) log.debug("write: joined, m_thread=" + m_thread.getName());
            } catch (InterruptedException e) {
                if (log.isDebugEnabled()) log.debug("write: interrupted, reason=" + e.getMessage());
            }
            if (m_is_done == false) {
                if (log.isDebugEnabled()) log.debug("write: not done, probably due to timeout, which is okay");
                return false;
            }
            if (m_exception_message != null) {
                log.warn("write: exception occurred during write, reason=" + m_exception_message);
                return false;
            }
            return true;
        } finally {
            synchronized (this) {
                if (m_thread != null) m_thread.interrupt();
                m_thread = null;
            }
        }
    }

    /**
     * Worker thread
     *
     * Note that it can block on either the write() or the flush().
     */
    protected void doWrite() {
        if (log.isDebugEnabled()) log.debug("doWrite: start");
        try {
            m_os.write(m_buf, m_off, m_len);
            m_os.flush();
        } catch (IOException e) {
            if (log.isDebugEnabled()) log.debug("doWrite: exception=" + e.getMessage());
            m_exception_message = e.getMessage();
        }
        if (log.isDebugEnabled()) log.debug("doWrite: is_done");
        m_is_done = true;
    }

    class Target implements Runnable {

        public void run() {
            doWrite();
        }
    }

    private OutputStream m_os;

    private String m_exception_message;

    private Target m_target;

    private Thread m_thread;

    private boolean m_is_done;

    private byte[] m_buf;

    private int m_len;

    private int m_off;

    private long m_timeout;

    /**
     * logging object
     */
    protected static Log log = LogFactory.getLog(ExpiringWriter.class);
}
