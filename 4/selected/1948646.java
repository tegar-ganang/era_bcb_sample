package syntelos.net.raf;

import alto.sys.Reference;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import alto.io.u.Bits;

/**
 * <p> Simple file IPC.  A file is used with file locks and an
 * embedded semaphore to communicate between processes on a host.
 * This IPC is implemented with mutually exclusive write locks.  </p>
 * 
 * <h3>Notes</h3>
 * 
 * <p> The initial application is the Viz framework, wherein multiple
 * processes (servers and clients), coordinate the positioning of
 * their windows.  Each Viz class acquires an integer key by locking
 * the shared file and incrementing a contained counter.  </p>
 * 
 * <p> This connection requires disconnect.  </p>
 * 
 * <p> Once a stream is opened, the identical stream is always
 * returned from this connection until it is disconnected.  </p>
 * 
 * <p> This class is not multithread safe within a jvm process.  It
 * should be used by only one thread, or otherwise externally
 * controlled.  Of course this code is multiprocess safe between two
 * jvm processes.  </p>
 * 
 * <h3>General Locking</h3>
 * 
 * <p> Read locks are implemented as shared locks, which are platform
 * dependent. </p>
 * 
 * <h3>URL</h3>
 * 
 * <p> A RAF url, for example <code>"shm:/tmp/file"</code>, will
 * produce a RAF buffer of <code>0x3000</code> (12,288) bytes.
 * Optionally include a query parameter named "size" with a value
 * appropriate to {@link java.lang.Integer#decode(java.lang.String)}
 * to change the RAF buffer size.  For example, the following URL will
 * produce a shared memory buffer of <code>0x4000</code> bytes
 * <code>"shm:/tmp/file?size=0x4000"</code>.  The buffer will grow as
 * required at runtime. </p>
 * 
 * 
 * @author jdp
 * @since 1.5
 */
public class Connection extends java.net.URLConnection implements alto.net.Connection, alto.sys.Lock.Semaphore {

    public static final int SIZEOF_SEMAPHORE = 1;

    private static final boolean ReadLock = true;

    private static final boolean WriteLock = false;

    private static final String ALLPERMS = "read,write,execute,delete";

    private static InetAddress LocalHost;

    static {
        try {
            LocalHost = InetAddress.getLocalHost();
        } catch (java.net.UnknownHostException exc) {
            throw new alto.sys.Error.Bug(exc);
        }
    }

    private final File file;

    private final RandomAccessFile raf;

    private final FileChannel channel;

    private InputStream in;

    private OutputStream out;

    private Reference reference;

    protected volatile byte semaphore;

    protected volatile FileLock lock;

    private volatile java.lang.Thread connected;

    /**
     * Constructor via {@link Handler}
     * @param url raf:file 
     */
    public Connection(URL url) {
        super(url);
        String file_path = url.getPath();
        this.file = new File(file_path);
        try {
            this.raf = new RandomAccessFile(this.file, "rw");
            this.raf.seek(0L);
            new java.io.FilePermission(file_path, ALLPERMS);
            this.channel = this.raf.getChannel();
        } catch (java.io.IOException exc) {
            throw new alto.sys.Error.State(file_path, exc);
        }
        Shutdown shutdown = new Shutdown(this);
        java.lang.Runtime.getRuntime().addShutdownHook(shutdown);
    }

    public Reference getReference() {
        return this.reference;
    }

    public void setReference(Reference r) {
        this.reference = r;
    }

    public final InetAddress getLocalHost() {
        return LocalHost;
    }

    public final java.lang.String getHost() {
        return "localhost";
    }

    public final java.lang.String getPath() {
        return this.url.getPath();
    }

    public final int getPort() {
        return -1;
    }

    /**
     * Position buffer to start of payload section, after private
     * semaphore section.
     */
    public final void reset() {
        try {
            this.raf.seek(SIZEOF_SEMAPHORE);
        } catch (java.io.IOException exc) {
            throw new alto.sys.Error.State(exc);
        }
    }

    /**
     * <p> Called by {@link syntelos.lang.Socket#accept()} from
     * {@link syntelos.sx.Server}.  The SHM server has one Server
     * thread per SHM Connection. </p>
     */
    public boolean accept() {
        return true;
    }

    public final void connect() throws java.io.IOException {
        java.lang.Thread connected = this.connected;
        if (null != connected && java.lang.Thread.currentThread() != connected) throw new alto.sys.Error.Bug("mis connected"); else {
            this.connected = java.lang.Thread.currentThread();
            if (null == this.in) this.in = this.openInputStream(); else this.in.reinit();
            if (null == this.out) this.out = this.openOutputStream(); else this.out.reinit();
        }
    }

    public final void release() {
        if (this.isNotOpen()) this.disconnect(); else if (java.lang.Thread.currentThread() == this.connected) this.connected = null;
    }

    /**
     * Close channel, and release existing streams.
     */
    public final void disconnect() {
        this.connected = null;
        try {
            this.channel.close();
        } catch (java.io.IOException ignore) {
        }
        try {
            this.raf.close();
        } catch (java.io.IOException ignore) {
        }
        try {
            this.in.close();
        } catch (java.io.IOException ignore) {
        }
        try {
            this.out.close();
        } catch (java.io.IOException ignore) {
        }
    }

    public final File getFile() {
        return this.file;
    }

    public final FileChannel getChannel() {
        return this.channel;
    }

    public final boolean isOpen() {
        return this.channel.isOpen();
    }

    public final boolean isNotOpen() {
        return (!this.channel.isOpen());
    }

    protected InputStream openInputStream() throws java.io.IOException {
        return new InputStream(this.raf, SIZEOF_SEMAPHORE);
    }

    protected OutputStream openOutputStream() throws java.io.IOException {
        return new OutputStream(this.raf, SIZEOF_SEMAPHORE);
    }

    public final java.io.InputStream getInputStream() throws java.io.IOException {
        InputStream in = this.in;
        if (null == in) throw new java.io.IOException("not connected"); else {
            in.reinit();
            return in;
        }
    }

    public final java.io.OutputStream getOutputStream() throws java.io.IOException {
        OutputStream out = this.out;
        if (null == out) throw new java.io.IOException("not connected"); else {
            out.reinit();
            return out;
        }
    }

    public void write(alto.lang.HttpMessage container) throws java.io.IOException {
        throw new UnsupportedOperationException();
    }

    public final boolean lockReadEnterTry() {
        return this.lockWriteEnterTry();
    }

    public final void lockReadEnter() {
        this.lockWriteEnter();
    }

    public final void lockReadExit() {
        this.lockWriteExit();
    }

    public final boolean lockWriteEnterTry() {
        {
            java.lang.Thread connected = this.connected;
            if (null != connected && java.lang.Thread.currentThread() != connected) throw new alto.sys.Error.Bug("mis connected");
        }
        byte cas = this.next();
        byte cas1 = this.subsequent(cas);
        if (this._lockWriteEnterTry(cas, cas1)) return true; else {
            this.undo();
            return false;
        }
    }

    public final boolean lockWriteEnterTry(byte cur, byte nex) {
        {
            java.lang.Thread connected = this.connected;
            if (null != connected && java.lang.Thread.currentThread() != connected) throw new alto.sys.Error.Bug("mis connected");
        }
        return this._lockWriteEnterTry(cur, nex);
    }

    private final boolean _lockWriteEnterTry(byte cur, byte nex) {
        if (null == this.lock) {
            try {
                this.lock = this.channel.tryLock(0L, Long.MAX_VALUE, WriteLock);
                return (null != this.lock);
            } catch (java.nio.channels.ClosedChannelException exc) {
                throw new alto.sys.Error.State("closed channel", exc);
            } catch (java.io.IOException exc) {
                String name = Thread.currentThread().getName();
                synchronized (System.err) {
                    System.err.print(name + ' ');
                    exc.printStackTrace(System.err);
                }
                return false;
            }
        } else return true;
    }

    /**
     * Use internal Rquest- Response CAS sequencing for a client or
     * server according to the constructor that created this object.
     */
    public final void lockWriteEnter() {
        {
            java.lang.Thread connected = this.connected;
            if (null != connected && java.lang.Thread.currentThread() != connected) throw new alto.sys.Error.Bug("mis connected");
        }
        if (null == this.lock) {
            byte cas = this.next();
            byte cas1 = this.subsequent(cas);
            long poll = 2L;
            while (!this._lockWriteEnter(cas, cas1)) {
                try {
                    synchronized (this) {
                        this.wait(poll++);
                        if (10L > poll) poll = 2L;
                    }
                } catch (java.lang.InterruptedException exc) {
                    this.undo();
                    throw new alto.sys.Error.State(exc);
                }
            }
        }
        return;
    }

    /**
     * Use external CAS sequence. 
     */
    public final boolean lockWriteEnter(byte cur, byte nex) {
        {
            java.lang.Thread connected = this.connected;
            if (null != connected && java.lang.Thread.currentThread() != connected) throw new alto.sys.Error.Bug("mis connected");
        }
        return this._lockWriteEnter(cur, nex);
    }

    private final boolean _lockWriteEnter(byte cas_curr, byte cas_next) {
        if (null == this.lock) {
            try {
                this.lock = this.channel.lock(0L, Long.MAX_VALUE, WriteLock);
                if (this.cas(cas_curr, cas_next)) return true; else {
                    FileLock lock = this.lock;
                    this.lock = null;
                    try {
                        lock.release();
                        return false;
                    } catch (java.nio.channels.ClosedChannelException exc) {
                        return false;
                    }
                }
            } catch (java.nio.channels.OverlappingFileLockException exc) {
                return false;
            } catch (java.nio.channels.ClosedChannelException exc) {
                throw new alto.sys.Error.State("channel closed", exc);
            } catch (java.io.IOException exc) {
                throw new alto.sys.Error.State("lock failed", exc);
            }
        } else return true;
    }

    public final void lockWriteExit() {
        if (null != this.lock) {
            try {
                this.lock.release();
                this.lock = null;
            } catch (java.nio.channels.ClosedChannelException exc) {
                return;
            } catch (java.io.IOException exc) {
                throw new alto.sys.Error.State("lock failed", exc);
            }
        }
    }

    private boolean cas(byte from, byte to) throws java.io.IOException {
        this.raf.seek(0L);
        byte compare = (byte) (this.raf.read() & 0xff);
        if (compare == from) {
            this.raf.seek(0L);
            this.raf.write((int) (to & 0xff));
            this.reset();
            return true;
        } else return false;
    }

    public byte getSemaphore() {
        return this.semaphore;
    }

    public void setSemaphore(byte value) {
        this.semaphore = value;
    }

    /**
     * @return This semaphore value
     * @see #undo()
     */
    protected byte next() {
        byte re = this.semaphore;
        return re;
    }

    /**
     * @return Zero
     */
    protected byte subsequent(byte state) {
        return 0;
    }

    /**
     * Inverse of {@link #next()}.  Return the state of the semphore
     * sequence for this connection to what it was before the last
     * call to {@link #next()}.
     * @return The value returned from calling this {@link #next()}.
     */
    protected void undo() {
        this.next();
    }

    public final boolean isShm() {
        return true;
    }

    public final boolean isNotShm() {
        return (!this.isShm());
    }
}
