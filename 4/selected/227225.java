package syntelos.net.shm;

import syntelos.lang.ch.InputStream;
import syntelos.lang.ch.OutputStream;
import alto.io.u.Bits;
import alto.sys.Reference;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

/**
 * <p> Request- Response IPC.  A shared file is used with memory
 * mapping and file locks for communicating between processes on a
 * host.  This SHM IPC is supported in the HTTP client and server via
 * the {@link syntelos.lang.Socket} and its {@link
 * syntelos.sx.Socket} subclass. </p>
 * 
 * <h3>Notes</h3>
 * 
 * <p> This connection requires disconnect.  </p>
 * 
 * <p> Once a stream is opened, the identical stream is always
 * returned from this connection until it is disconnected.  </p>
 * 
 * <p> The streams returned are {@link
 * syntelos.lang.ch.InputStream} and {@link
 * syntelos.lang.ch.OutputStream}.  </p>
 * 
 * <p> This class is not multithread safe within a jvm process.  It
 * should be used by only one thread, or otherwise externally
 * controlled.  Of course this code is multiprocess safe between two
 * jvm processes.  </p>
 * 
 * <h3>General Locking</h3>
 * 
 * <p> The {@link alto.sys.Lock$Semaphore} interface is
 * implemented here over file locking.  File locking and an embedded
 * semaphore are used for managing access to the buffer for Request-
 * Response processing. </p>
 * 
 * <p> All locking, for reading and writing, uses the write locks.
 * The read lock methods are synonyms for write locks. </p>
 * 
 * <p> The designated client (according to instance object
 * construction) first writes a request, the server the reads the
 * request, the server then writes a response, and the client then
 * reads the response.  This sequence is controlled by the semaphore,
 * implemented internally, and controlled by the write lock. </p>
 * 
 * <h3>Request- Response Locking</h3>
 * 
 * <p> This class implements Request- Response sequence locking for
 * Clients and Servers (according to the use of its constructors). </p>
 * 
 * <p> This process requires use of the {@link #lockWriteEnter()} (no
 * args) and {@link #lockWriteExit()} methods defined here and
 * accessed via a {@link syntelos.lang.Socket}. </p>
 * 
 * <p> The write lock enter and exit calls must be implemented with
 * precision to enclose the (entire) scopes of writing and reading
 * each request and response.  </p>
 * 
 * <p> For example in the HTTP client code, the user of a SHM client
 * (within {@link syntelos.net.http.Connection}) must wrap the
 * connect and any writing to the connection output stream in one
 * write lock enter and exit block, and must wrap the get status and
 * other reads and any reading from the connection input stream in
 * another separate write lock enter and exit block. </p>
 * 
 * <h3>SHM buffer addressing</h3>
 * 
 * <p> Internally, this class embeds a CAS style semaphore at SHM
 * buffer offset zero.  For stream I/O, the SHM buffer is positioned
 * to the first offset following this overhead segment. </p>
 * 
 * <p> This class performs this positioning operation on returning a
 * stream object to its users, and on entering the write lock.  </p>
 * 
 * <h3>URL</h3>
 * 
 * <p> A SHM url, for example <code>"shm:/tmp/file"</code>, will
 * produce a SHM buffer of <code>0x3000</code> (12,288) bytes.
 * Optionally include a query parameter named "size" with a value
 * appropriate to {@link java.lang.Integer#decode(java.lang.String)}
 * to change the SHM buffer size.  For example, the following URL will
 * produce a shared memory buffer of <code>0x4000</code> bytes
 * <code>"shm:/tmp/file?size=0x4000"</code>.  The buffer will grow as
 * required at runtime. </p>
 * 
 * 
 * @author jdp
 * @since 1.5
 */
public abstract class Connection extends java.net.URLConnection implements alto.net.Connection, alto.sys.Lock.Semaphore {

    public static final int SIZEOF_SEMAPHORE = 1;

    /**
     * Server side reads first.
     */
    public static final class Server extends Connection {

        public static final byte RQP_READ = 0x1;

        public static final byte RQP_WRITE = 0x2;

        public Server(URL url) {
            super(url, false);
            this.semaphore = RQP_READ;
        }
    }

    /**
     * Client side writes first.
     */
    public static final class Client extends Connection {

        public static final byte RQP_WRITE = 0x0;

        public static final byte RQP_READ = 0x3;

        public Client(URL url) {
            super(url, true);
            this.semaphore = RQP_WRITE;
        }
    }

    private static final boolean ReadLock = true;

    private static final boolean WriteLock = false;

    public static final int DEFAULT_SIZE = 0x3000;

    public static final int CtorSize(URL url) {
        String qs = url.getQuery();
        if (null == qs) return DEFAULT_SIZE; else {
            int idx = qs.indexOf("size=");
            if (-1 < idx) {
                String sz = qs.substring(idx + "size=".length());
                idx = sz.indexOf('&');
                if (0 < idx) sz = sz.substring(0, idx);
                return Integer.decode(sz);
            } else return DEFAULT_SIZE;
        }
    }

    private static final String ALLPERMS = "read,write,execute,delete";

    private static InetAddress LocalHost;

    static {
        try {
            LocalHost = InetAddress.getLocalHost();
        } catch (java.net.UnknownHostException exc) {
            throw new alto.sys.Error.Bug(exc);
        }
    }

    private final boolean isClient;

    private final File file;

    private final RandomAccessFile raf;

    private final FileChannel channel;

    private final MappedByteBuffer map;

    private final Shutdown shutdown;

    private final InputStream in;

    private final OutputStream out;

    private Reference reference;

    protected volatile byte semaphore;

    private volatile FileLock lock;

    private volatile java.lang.Thread connected;

    /**
     * Client constructor via {@link Handler}
     * @param url shm:file 
     */
    private Connection(URL url) {
        this(url, true, CtorSize(url));
    }

    /**
     * Server constructor, with 'false' second argument.
     * @param url shm:file 
     * @param isClient True for client, false for server
     */
    private Connection(URL url, boolean isClient) {
        this(url, isClient, CtorSize(url));
    }

    /**
     * @param url shm:file 
     * @param isClient True for client, false for server
     * @param size Fixed buffer size.
     */
    private Connection(URL url, boolean isClient, int size) {
        super(url);
        this.isClient = isClient;
        String file_path = url.getPath();
        this.file = new File(file_path);
        boolean found = (this.file.exists());
        try {
            this.raf = new RandomAccessFile(this.file, "rw");
            this.raf.setLength(size);
            this.raf.seek(0L);
            if (this.isClient || (!found)) {
                this.raf.write(Connection.Client.RQP_WRITE);
                this.raf.seek(0L);
            }
            new java.io.FilePermission(file_path, ALLPERMS);
            this.channel = this.raf.getChannel();
            this.map = this.channel.map(FileChannel.MapMode.READ_WRITE, 0L, size);
            this.in = new InputStream(this.map, SIZEOF_SEMAPHORE);
            this.out = new OutputStream(this.map, SIZEOF_SEMAPHORE);
        } catch (java.io.IOException exc) {
            throw new alto.sys.Error.State(file_path, exc);
        }
        this.shutdown = new Shutdown(this);
        java.lang.Runtime.getRuntime().addShutdownHook(this.shutdown);
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
        this.map.position(SIZEOF_SEMAPHORE);
    }

    /**
     * <p> Called by {@link syntelos.lang.Socket#accept()} from
     * {@link syntelos.sx.Server}.  The SHM server has one Server
     * thread per SHM Connection. </p>
     */
    public boolean accept() {
        if (null != this.connected) throw new alto.sys.Error.Bug("already connected"); else return true;
    }

    public final void connect() throws java.io.IOException {
        java.lang.Thread connected = this.connected;
        if (null != connected && java.lang.Thread.currentThread() != connected) throw new alto.sys.Error.Bug("mis connected"); else {
            this.connected = java.lang.Thread.currentThread();
            this.in.reinit();
            this.out.reinit();
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
            this.shutdown.close();
        } catch (java.lang.Exception ignore) {
        }
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

    private boolean cas(byte from, byte to) {
        this.map.position(0);
        byte compare = this.map.get(0);
        if (compare == from) {
            this.map.put(0, to);
            this.reset();
            return true;
        } else return false;
    }

    /**
     * Increment CAS semaphore state.
     * 
     * <p> Correct I/O sequences are achieved using the write lock and
     * semaphore.  In the Request- Response sequence, the CAS
     * semaphore has four states, described by the following diagram.
     * 
     * <pre>
     *  Client Write(Request)    0   -&gt;  1
     *  Server Read(Request)     1   -&gt;  2
     *  Server Write(Response)   2   -&gt;  3
     *  Client Read(Response)    3   -&gt;  0
     * </pre>
     * 
     * As this pattern repeats, each correspondent predicts the CAS number
     * pair for its next step, and polls the write lock and it's embedded
     * semaphore until it is able to hold it. </p>
     * 
     * @see #undo()
     */
    protected byte next() {
        byte re = this.semaphore;
        if (this.isClient) {
            switch(this.semaphore) {
                case Connection.Client.RQP_READ:
                    this.semaphore = Connection.Client.RQP_WRITE;
                    break;
                case Connection.Client.RQP_WRITE:
                default:
                    this.semaphore = Connection.Client.RQP_READ;
                    break;
            }
        } else {
            switch(this.semaphore) {
                case Connection.Server.RQP_WRITE:
                    this.semaphore = Connection.Server.RQP_READ;
                    break;
                case Connection.Server.RQP_READ:
                default:
                    this.semaphore = Connection.Server.RQP_WRITE;
                    break;
            }
        }
        return re;
    }

    /**
     * Given a semaphore state, return the subsequent state for the
     * system.
     */
    protected byte subsequent(byte state) {
        switch(state) {
            case Connection.Client.RQP_WRITE:
                return Connection.Server.RQP_READ;
            case Connection.Server.RQP_READ:
                return Connection.Server.RQP_WRITE;
            case Connection.Server.RQP_WRITE:
                return Connection.Client.RQP_READ;
            case Connection.Client.RQP_READ:
                return Connection.Client.RQP_WRITE;
            default:
                throw new alto.sys.Error.State(String.valueOf(state));
        }
    }

    /**
     * Inverse of {@link #next()}.  Return the state of the semphore
     * sequence for this connection to what it was before the last
     * call to {@link #next()}.
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
