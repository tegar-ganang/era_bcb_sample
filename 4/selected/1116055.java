package org.hsqldb;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * A LockFile variant that capitalizes upon the
 * availability of {@link java.nio.channels.FileLock FileLock}.
 *
 * @author boucherb@users
 * @version 1.7.2
 * @since HSQLDB 1.7.2
 *
 */
final class NIOLockFile extends LockFile {

    static final long MAX_NFS_LOCK_REGION = (1L << 30);

    static final long MIN_LOCK_REGION = MAGIC.length + 8;

    /**
     * A <code>FileChannel</code> object obtained from the super
     * <code>raf</code> attribute. <p>
     *
     * The <code>fc</code> attribute is used to obtain this object's
     * {@link #fl FileLock} attribute.
     */
    private FileChannel fc;

    /**
     * The <code>FileLock</code> object used to lock this object's
     * lock file.
     */
    private FileLock fl;

    /**
     * Tries to obtain a valid NIO lock upon this object's lock file using
     * this object's {@link #fl FileLock} attribute.
     *
     * @return true if a valid lock is obtained, else false.
     * @throws Exception if an error occurs while attempting to obtain the lock
     *
     */
    protected boolean lockImpl() throws Exception {
        boolean isValid;
        if (fl != null && fl.isValid()) {
            return true;
        }
        trace("lockImpl(): fc = raf.getChannel()");
        fc = raf.getChannel();
        trace("lockImpl(): fl = fc.tryLock()");
        fl = null;
        try {
            fl = fc.tryLock(0, MIN_LOCK_REGION, false);
            trace("lockImpl(): fl = " + fl);
        } catch (Exception e) {
            trace(e.toString());
        }
        trace("lockImpl(): f.deleteOnExit()");
        f.deleteOnExit();
        isValid = fl != null && fl.isValid();
        trace("lockImpl():isValid(): " + isValid);
        return isValid;
    }

    /**
     * Tries to release any valid lock held upon this object's lock file using
     * this object's {@link #fl FileLock} attribute.
     *
     * @return true if a valid lock is released, else false
     * @throws Exception if na error occurs while attempting to release the lock
     */
    protected boolean releaseImpl() throws Exception {
        trace("releaseImpl(): fl = " + fl);
        if (fl != null) {
            trace("releaseImpl(): fl.release()");
            fl.release();
            trace("tryRelease(): fl = " + fl);
            fl = null;
        }
        trace("releaseImpl(): fc = " + fc);
        if (fc != null) {
            trace("releaseImpl(): fc.close()");
            fc.close();
            fc = null;
        }
        return true;
    }

    /**
     * Retrieves whether this object's {@link #fl FileLock} attribute represents
     * a valid lock upon this object's lock file.
     *
     * @return true if this object's {@link #fl FileLock} attribute is valid,
     *      else false
     */
    public boolean isValid() {
        return super.isValid() && (fl != null && fl.isValid());
    }

    /**
     * Retrieves the String value: "fl =" + fl
     * @return the String value: "fl =" + fl
     */
    protected String toStringImpl() {
        return "fl =" + fl;
    }
}
