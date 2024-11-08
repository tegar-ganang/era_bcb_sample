package syntelos.net.shm;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import alto.io.u.Hex;

/**
 * <p> An eight byte shared memory semaphore using file locking,
 * exports an atomic compare and set operation. </p>
 * 
 * <p> 
 * </p>
 * 
 * @author jdp
 * @since 1.5
 */
public final class Semaphore extends java.lang.Object implements alto.sys.Lock {

    private static final boolean ReadLock = true;

    private static final boolean WriteLock = false;

    private static final String ALLPERMS = "read,write,execute,delete";

    private final URL url;

    private final RandomAccessFile file;

    private final FileChannel channel;

    private final MappedByteBuffer map;

    private volatile FileLock lock;

    public Semaphore(URL url) {
        super();
        if (null != url) {
            this.url = url;
            String file_path = url.getPath() + ".semaphore";
            try {
                this.file = new RandomAccessFile(file_path, "rw");
                this.file.writeLong(0L);
                new java.io.FilePermission(file_path, ALLPERMS);
                this.channel = this.file.getChannel();
                this.file.setLength(8);
                this.map = this.channel.map(FileChannel.MapMode.READ_WRITE, 0L, 8);
            } catch (java.io.IOException exc) {
                throw new alto.sys.Error.State(file_path, exc);
            }
        } else throw new java.lang.IllegalArgumentException();
    }

    public URL getURL() {
        return this.url;
    }

    public boolean cas(long from, long to) {
        this.lockWriteEnter();
        try {
            this.map.position(0);
            byte[] bits = new byte[8];
            this.map.get(bits, 0, 8);
            long read = Hex.Long(bits);
            if (read == from) {
                this.map.position(0);
                bits = Hex.Long(to, bits);
                this.map.put(bits, 0, 8);
                return true;
            } else return false;
        } finally {
            this.lockWriteExit();
        }
    }

    public boolean lockReadEnterTry() {
        if (null != this.lock) throw new alto.sys.Error.State("lock open"); else {
            try {
                this.lock = this.channel.tryLock(0L, Long.MAX_VALUE, ReadLock);
                return (null != this.lock);
            } catch (java.io.IOException exc) {
                throw new alto.sys.Error.State("lock failed", exc);
            }
        }
    }

    public void lockReadEnter() {
        if (null != this.lock) throw new alto.sys.Error.State("lock open"); else {
            try {
                this.lock = this.channel.lock(0L, Long.MAX_VALUE, ReadLock);
            } catch (java.io.IOException exc) {
                throw new alto.sys.Error.State("lock failed", exc);
            }
        }
    }

    public void lockReadExit() {
        if (null == this.lock) throw new alto.sys.Error.State("lock closed"); else {
            try {
                this.lock.release();
                this.lock = null;
            } catch (java.io.IOException exc) {
                throw new alto.sys.Error.State("lock release failed", exc);
            }
        }
    }

    public boolean lockWriteEnterTry() {
        if (null != this.lock) throw new alto.sys.Error.State("lock open"); else {
            try {
                this.lock = this.channel.tryLock(0L, Long.MAX_VALUE, WriteLock);
                return (null != this.lock);
            } catch (java.io.IOException exc) {
                throw new alto.sys.Error.State("lock failed", exc);
            }
        }
    }

    public void lockWriteEnter() {
        if (null != this.lock) throw new alto.sys.Error.State("lock open"); else {
            try {
                this.lock = this.channel.lock(0L, Long.MAX_VALUE, WriteLock);
            } catch (java.io.IOException exc) {
                throw new alto.sys.Error.State("lock failed", exc);
            }
        }
    }

    public void lockWriteExit() {
        if (null == this.lock) throw new alto.sys.Error.State("lock closed"); else {
            try {
                this.lock.release();
                this.lock = null;
            } catch (java.io.IOException exc) {
                throw new alto.sys.Error.State("lock release failed", exc);
            }
        }
    }
}
