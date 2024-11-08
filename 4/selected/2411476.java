package com.ingenico.tools.nio.channel;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import com.ingenico.tools.nio.FloatBuffer;
import com.ingenico.tools.nio.SampleBuffer;

public class FloatFileChannel extends SampleFileChannel {

    protected final RandomAccessFile file;

    protected final SampleSign sign;

    protected final ByteOrder order;

    public FloatFileChannel(final String sourceFileName, final SampleSign sign, final ByteOrder order) throws IOException {
        this.file = new RandomAccessFile(sourceFileName, "r");
        if ((this.file.length() % 4) != 0) {
            file.close();
            throw new IOException("File length does not match");
        }
        this.channel = file.getChannel();
        this.sign = sign;
        this.order = order;
    }

    public FloatFileChannel(final File file, final SampleSign sign, final ByteOrder order) throws IOException {
        this.file = new RandomAccessFile(file, "r");
        if ((this.file.length() % 4) != 0) {
            this.file.close();
            throw new IOException("File length does not match");
        }
        this.channel = this.file.getChannel();
        this.sign = sign;
        this.order = order;
    }

    @Override
    public int read(final SampleBuffer dst) throws IOException {
        return channel.read(dst.toByteBuffer()) / 4;
    }

    @Override
    public int read(final SampleBuffer dst, long position) throws IOException {
        return channel.read(dst.toByteBuffer(), position) / 4;
    }

    @Override
    public final RandomAccessChannel position(long newPosition) throws IOException {
        channel.position(newPosition * 4);
        return this;
    }

    @Override
    public final long position() throws IOException {
        return channel.position() / 4;
    }

    @Override
    public SampleBuffer allocate(int samplesWindowLength) {
        return new FloatBuffer(java.nio.ByteBuffer.allocate(samplesWindowLength * 4)).order(order);
    }

    @Override
    public long size() throws IOException {
        return channel.size() / 4;
    }
}
