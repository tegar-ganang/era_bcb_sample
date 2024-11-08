package com.ingenico.tools.nio.channel;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import com.ingenico.tools.nio.ByteBuffer;
import com.ingenico.tools.nio.SampleBuffer;
import com.ingenico.tools.nio.SignedByteBuffer;

public class ByteFileChannel extends SampleFileChannel {

    protected final RandomAccessFile file;

    protected final SampleSign sign;

    public ByteFileChannel(final String sourceFileName, final SampleSign sign) throws IOException {
        this.file = new RandomAccessFile(sourceFileName, "r");
        this.channel = file.getChannel();
        this.sign = sign;
    }

    public ByteFileChannel(final File file, final SampleSign sign) throws IOException {
        this.file = new RandomAccessFile(file, "r");
        this.channel = this.file.getChannel();
        this.sign = sign;
    }

    @Override
    public int read(final SampleBuffer dst) throws IOException {
        return channel.read(dst.toByteBuffer());
    }

    @Override
    public int read(final SampleBuffer dst, long position) throws IOException {
        return channel.read(dst.toByteBuffer(), position);
    }

    @Override
    public final RandomAccessChannel position(long newPosition) throws IOException {
        channel.position(newPosition);
        return this;
    }

    @Override
    public final long position() throws IOException {
        return channel.position();
    }

    @Override
    public SampleBuffer allocate(int samplesWindowLength) {
        if (SampleSign.SIGNED == sign) {
            return new SignedByteBuffer(java.nio.ByteBuffer.allocate(samplesWindowLength));
        } else {
            return new ByteBuffer(java.nio.ByteBuffer.allocate(samplesWindowLength));
        }
    }

    @Override
    public long size() throws IOException {
        return channel.size();
    }
}
