package alto.sys;

import alto.lang.HttpMessage;
import java.nio.channels.FileChannel;

/**
 * Class used by {@link File#openOutputStream()}.  
 * 
 * <h3>File locking</h3>
 * 
 * <p> The file locking channel (and this output stream) must remain
 * open until the file lock is released.  To accomplish this, and for
 * rational server side file writing, the {@link File} class permits
 * only one open instance of this class at a time. </p>
 * 
 * <h3>File versioning</h3>
 * 
 * <p> The target file is never version "current".  It is typically
 * the "temporary" file, which is subsequently committed to the
 * "current" file on a call to "commit" or "flush" in this class. </p>
 * 
 * 
 * @see File
 * @see FileTransaction
 * @author jdp
 * @since 1.6
 */
public final class FileOutputStream extends java.io.FileOutputStream implements java.nio.channels.WritableByteChannel, alto.io.Output {

    /**
     * Ensure containing directory
     */
    public static final File Ctor(File file) throws java.io.FileNotFoundException {
        java.io.File dir = file.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) throw new java.io.FileNotFoundException("Unable to create directory '" + dir.getPath() + "'.");
        }
        return file;
    }

    protected static final char[] CRLFchar = { '\r', '\n' };

    protected static final byte[] CRLFbyte = { '\r', '\n' };

    private File file;

    private FileTransaction transaction;

    private boolean commit, needstat;

    private HttpMessage message;

    /**
     * {@link File} writes not transactional, or {@link FileTransaction} channel
     * lock constructor not written to.
     */
    public FileOutputStream(File named, HttpMessage message) throws java.io.FileNotFoundException, java.lang.SecurityException {
        super(Ctor(named));
        this.needstat = true;
        this.commit = true;
        this.file = named;
        this.message = message;
    }

    /**
     * {@link FileTransaction} transactional writes to temporary.
     */
    public FileOutputStream(FileTransaction transaction, HttpMessage message) throws java.io.FileNotFoundException, java.lang.SecurityException {
        super(Ctor(transaction.getFileTemp()));
        this.transaction = transaction;
        this.message = message;
    }

    /**
     * Flush and commit (cause commit on close)
     * @see #close()
     * @see FileTransaction#commit()
     */
    public void flush() throws java.io.IOException {
        super.flush();
        this.commit = true;
    }

    /**
     * Commit (cause commit on close)
     * @see #close()
     * @see FileTransaction#commit()
     */
    public void commit() throws java.io.IOException {
        this.commit = true;
    }

    /**
     * Don't commit, default state until {@link #flush()} or {@link
     * #commit()}
     * 
     * @see #isDiscard()
     * @see #isCommit()
     */
    public void discard() throws java.io.IOException {
        this.commit = false;
    }

    /**
     * @return True after {@link #flush()} or {@link #commit()}
     */
    public boolean isCommit() throws java.io.IOException {
        return this.commit;
    }

    /**
     * @return True until {@link #flush()} or {@link #commit()}
     */
    public boolean isDiscard() throws java.io.IOException {
        return (!this.commit);
    }

    public boolean isOpen() {
        return (this.getChannel().isOpen());
    }

    /**
     * Indempotent close (once) then commit or discard from
     * transaction constructor.  Close from channel constructor.
     * 
     * @see FileTransaction#commit()
     */
    public synchronized void close() throws java.io.IOException {
        FileTransaction transaction = this.transaction;
        this.transaction = null;
        try {
            super.close();
            if (this.commit) {
                if (null != transaction) transaction.commit();
            } else if (null != transaction) transaction.discard();
        } finally {
            if (null != transaction) transaction.release(); else if (this.needstat) {
                this.needstat = false;
                try {
                    if (this.commit) this.file.stat(this.message); else this.file.stat(false);
                } finally {
                    this.file = null;
                }
            }
        }
    }

    public int write(java.nio.ByteBuffer src) throws java.io.IOException {
        return this.getChannel().write(src);
    }

    public void print(char ch) throws java.io.IOException {
        if (ch < 0x80) this.write(ch); else {
            char[] cary = new char[] { ch };
            byte[] bary = alto.io.u.Utf8.encode(cary);
            this.write(bary, 0, bary.length);
        }
    }

    public void print(String string) throws java.io.IOException {
        if (null != string) {
            char[] cary = string.toCharArray();
            if (0 < cary.length) {
                byte[] bary = alto.io.u.Utf8.encode(cary);
                this.write(bary, 0, bary.length);
            }
        }
    }

    public void println(char ch) throws java.io.IOException {
        char[] cary = alto.io.u.Chbuf.cat(ch, CRLFchar);
        byte[] bary = alto.io.u.Utf8.encode(cary);
        this.write(bary, 0, bary.length);
    }

    public void println() throws java.io.IOException {
        byte[] bary = CRLFbyte;
        this.write(bary, 0, 2);
    }

    public void println(String string) throws java.io.IOException {
        char[] cary = null;
        if (null != string) cary = string.toCharArray();
        cary = alto.io.u.Chbuf.cat(cary, CRLFchar);
        byte[] bary = alto.io.u.Utf8.encode(cary);
        this.write(bary, 0, bary.length);
    }
}
