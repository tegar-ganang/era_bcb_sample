package x.java.nio.channels;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * 
 * Virtual FileChannel backed by a local file channel
 * FIXME -- this is a quick hack to support channel
 * 
 * @author qiangli
 * 
 */
public class FileChannel extends java.nio.channels.FileChannel {

    private java.nio.channels.FileChannel channel = null;

    private java.io.File file = null;

    public FileChannel(java.io.File file, java.nio.channels.FileChannel channel) {
        this.file = file;
        this.channel = channel;
        throw new RuntimeException("FIXME - Not tested");
    }

    public FileChannel(java.io.RandomAccessFile file) {
        x.java.io.RandomAccessFile raf = (x.java.io.RandomAccessFile) file;
        this.file = raf.getFile();
        this.channel = raf.getChannel();
        throw new RuntimeException("FIXME - Not tested");
    }

    public void force(boolean metaData) throws IOException {
        store();
    }

    public FileLock lock(long position, long size, boolean shared) throws IOException {
        return channel.lock(position, size, shared);
    }

    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        return channel.map(mode, position, size);
    }

    public long position() throws IOException {
        return channel.position();
    }

    public java.nio.channels.FileChannel position(long newPosition) throws IOException {
        return channel.position(newPosition);
    }

    public int read(ByteBuffer dst, long position) throws IOException {
        return channel.read(dst, position);
    }

    public int read(ByteBuffer dst) throws IOException {
        return channel.read(dst);
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return channel.read(dsts, offset, length);
    }

    public long size() throws IOException {
        return channel.size();
    }

    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        return channel.transferFrom(src, position, count);
    }

    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        return channel.transferTo(position, count, target);
    }

    public java.nio.channels.FileChannel truncate(long size) throws IOException {
        return channel.truncate(size);
    }

    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return channel.tryLock();
    }

    public int write(ByteBuffer src, long position) throws IOException {
        return channel.write(src, position);
    }

    public int write(ByteBuffer src) throws IOException {
        return channel.write(src);
    }

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        return channel.write(srcs, offset, length);
    }

    protected void implCloseChannel() throws IOException {
        store();
    }

    private void store() throws IOException {
        OutputStream os = file.toURL().openConnection().getOutputStream();
        WritableByteChannel wbc = Channels.newChannel(os);
        int written = 0;
        long count = channel.size();
        while (written < count) {
            written += channel.transferTo(written, count - written, wbc);
        }
        os.close();
    }

    public java.nio.channels.FileChannel getChannel() {
        return this.channel;
    }

    public java.io.File getFile() {
        return this.file;
    }
}
