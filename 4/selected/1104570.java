package com.ingenico.tools.nio.channel;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import com.ingenico.tools.nio.SampleBuffer;
import com.ingenico.tools.nio.ShortBuffer;
import com.ingenico.tools.nio.SignedShortBuffer;

public class ShortFileChannel extends SampleFileChannel {

    protected final RandomAccessFile file;

    protected final SampleSign sign;

    protected final ByteOrder order;

    public ShortFileChannel(final String sourceFileName, final SampleSign sign, final ByteOrder order) throws IOException {
        this.file = new RandomAccessFile(sourceFileName, "r");
        if ((this.file.length() % 2) != 0) {
            file.close();
            throw new IOException("File length does not match");
        }
        this.channel = file.getChannel();
        this.sign = sign;
        this.order = order;
    }

    public ShortFileChannel(final File file, final SampleSign sign, final ByteOrder order) throws IOException {
        this.file = new RandomAccessFile(file, "r");
        if ((this.file.length() % 2) != 0) {
            this.file.close();
            throw new IOException("File length does not match");
        }
        this.channel = this.file.getChannel();
        this.sign = sign;
        this.order = order;
    }

    @Override
    public int read(final SampleBuffer dst) throws IOException {
        return channel.read(dst.toByteBuffer()) / 2;
    }

    @Override
    public int read(final SampleBuffer dst, long position) throws IOException {
        return channel.read(dst.toByteBuffer(), position) / 2;
    }

    @Override
    public final RandomAccessChannel position(long newPosition) throws IOException {
        channel.position(newPosition * 2);
        return this;
    }

    @Override
    public final long position() throws IOException {
        return channel.position() / 2;
    }

    @Override
    public SampleBuffer allocate(int samplesWindowLength) {
        if (SampleSign.SIGNED == sign) {
            return new SignedShortBuffer(java.nio.ByteBuffer.allocate(samplesWindowLength * 2)).order(order);
        } else {
            return new ShortBuffer(java.nio.ByteBuffer.allocate(samplesWindowLength * 2)).order(order);
        }
    }

    @Override
    public long size() throws IOException {
        return channel.size() / 2;
    }
}
