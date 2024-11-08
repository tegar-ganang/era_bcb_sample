package com.flazr.io;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileChannelReader implements BufferReader {

    private static final Logger logger = LoggerFactory.getLogger(FileChannelReader.class);

    private final String absolutePath;

    private final FileChannel in;

    private final long fileSize;

    public FileChannelReader(final String path) {
        this(new File(path));
    }

    public FileChannelReader(final File file) {
        absolutePath = file.getAbsolutePath();
        try {
            in = new FileInputStream(file).getChannel();
            fileSize = in.size();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.info("opened file: {}", absolutePath);
    }

    @Override
    public long size() {
        return fileSize;
    }

    @Override
    public long position() {
        try {
            return in.position();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void position(final long newPosition) {
        try {
            in.position(newPosition);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ChannelBuffer read(final int size) {
        return ChannelBuffers.wrappedBuffer(readBytes(size));
    }

    @Override
    public int readInt() {
        return read(4).readInt();
    }

    @Override
    public long readUnsignedInt() {
        return read(4).readUnsignedInt();
    }

    @Override
    public byte[] readBytes(final int size) {
        final byte[] bytes = new byte[size];
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        try {
            in.read(bb);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bytes;
    }

    @Override
    public void close() {
        try {
            in.close();
        } catch (Exception e) {
            logger.warn("error closing file {}: {}", absolutePath, e.getMessage());
        }
        logger.info("closed file: {}", absolutePath);
    }
}
