package com.lowagie.text.pdf;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.BufferUnderflowException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A {@link java.nio.MappedByteBuffer} wrapped as a {@link java.io.RandomAccessFile}
 *
 * @author Joakim Sandstroem
 * Created on 6.9.2006
 */
public class MappedRandomAccessFile {

    private MappedByteBuffer mappedByteBuffer = null;

    private FileChannel channel = null;

    /**
     * Constructs a new MappedRandomAccessFile instance
     * @param filename String
     * @param mode String r, w or rw
     * @throws FileNotFoundException
     * @throws IOException
     */
    public MappedRandomAccessFile(String filename, String mode) throws FileNotFoundException, IOException {
        if (mode.equals("rw")) init(new java.io.RandomAccessFile(filename, mode).getChannel(), FileChannel.MapMode.READ_WRITE); else init(new FileInputStream(filename).getChannel(), FileChannel.MapMode.READ_ONLY);
    }

    /**
     * initializes the channel and mapped bytebuffer
     * @param channel FileChannel
     * @param mapMode FileChannel.MapMode
     * @throws IOException
     */
    private void init(FileChannel channel, FileChannel.MapMode mapMode) throws IOException {
        this.channel = channel;
        this.mappedByteBuffer = channel.map(mapMode, 0L, channel.size());
        mappedByteBuffer.load();
    }

    /**
     * @since 2.0.8
     */
    public FileChannel getChannel() {
        return channel;
    }

    /**
     * @see java.io.RandomAccessFile#read()
     * @return int next integer or -1 on EOF
     */
    public int read() {
        try {
            byte b = mappedByteBuffer.get();
            int n = b & 0xff;
            return n;
        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    /**
     * @see java.io.RandomAccessFile#read(byte[], int, int)
     * @param bytes byte[]
     * @param off int offset
     * @param len int length
     * @return int bytes read or -1 on EOF
     */
    public int read(byte bytes[], int off, int len) {
        int pos = mappedByteBuffer.position();
        int limit = mappedByteBuffer.limit();
        if (pos == limit) return -1;
        int newlimit = pos + len - off;
        if (newlimit > limit) {
            len = limit - pos;
        }
        mappedByteBuffer.get(bytes, off, len);
        return len;
    }

    /**
     * @see java.io.RandomAccessFile#getFilePointer()
     * @return long
     */
    public long getFilePointer() {
        return mappedByteBuffer.position();
    }

    /**
     * @see java.io.RandomAccessFile#seek(long)
     * @param pos long position
     */
    public void seek(long pos) {
        mappedByteBuffer.position((int) pos);
    }

    /**
     * @see java.io.RandomAccessFile#length()
     * @return long length
     */
    public long length() {
        return mappedByteBuffer.limit();
    }

    /**
     * @see java.io.RandomAccessFile#close()
     * Cleans the mapped bytebuffer and closes the channel
     */
    public void close() throws IOException {
        clean(mappedByteBuffer);
        mappedByteBuffer = null;
        if (channel != null) channel.close();
        channel = null;
    }

    /**
     * invokes the close method
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * invokes the clean method on the ByteBuffer's cleaner
     * @param buffer ByteBuffer
     * @return boolean true on success
     */
    public static boolean clean(final java.nio.ByteBuffer buffer) {
        if (buffer == null || !buffer.isDirect()) return false;
        Boolean b = (Boolean) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                Boolean success = Boolean.FALSE;
                try {
                    Method getCleanerMethod = buffer.getClass().getMethod("cleaner", (Class[]) null);
                    getCleanerMethod.setAccessible(true);
                    Object cleaner = getCleanerMethod.invoke(buffer, (Object[]) null);
                    Method clean = cleaner.getClass().getMethod("clean", (Class[]) null);
                    clean.invoke(cleaner, (Object[]) null);
                    success = Boolean.TRUE;
                } catch (Exception e) {
                }
                return success;
            }
        });
        return b.booleanValue();
    }
}
