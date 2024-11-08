package org.h2.store.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * File which uses NIO FileChannel.
 */
public class FileObjectDiskChannel implements FileObject {

    private final String name;

    private FileChannel channel;

    FileObjectDiskChannel(String fileName, String mode) throws FileNotFoundException {
        this.name = fileName;
        RandomAccessFile file = new RandomAccessFile(fileName, mode);
        channel = file.getChannel();
    }

    public void close() throws IOException {
        channel.close();
    }

    public long getFilePointer() throws IOException {
        return channel.position();
    }

    public String getName() {
        return name;
    }

    public long length() throws IOException {
        return channel.size();
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return;
        }
        ByteBuffer buf = ByteBuffer.wrap(b);
        buf.position(off);
        buf.limit(off + len);
        channel.read(buf);
    }

    public void seek(long pos) throws IOException {
        channel.position(pos);
    }

    public void setFileLength(long newLength) throws IOException {
        if (newLength <= channel.size()) {
            long oldPos = channel.position();
            channel.truncate(newLength);
            if (oldPos > newLength) {
                oldPos = newLength;
            }
            channel.position(oldPos);
        } else {
            ByteBuffer b = ByteBuffer.allocate(1);
            channel.write(b, newLength - 1);
        }
    }

    public void sync() throws IOException {
        channel.force(true);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(b);
        buf.position(off);
        buf.limit(off + len);
        channel.write(buf);
    }
}
