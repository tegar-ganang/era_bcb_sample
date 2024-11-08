package com.faunos.util.io;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import com.faunos.util.io.ChannelUtil;
import com.faunos.util.io.MemoryFileChannel;
import com.faunos.util.test.Helper;
import junit.framework.TestCase;

/**
 * 
 * 
 */
public class MemoryFileChannelTest extends TestCase {

    private final ChannelUtil<RuntimeException> channelUtil = new ChannelUtil<RuntimeException>(RuntimeException.class);

    private final Helper env = new Helper() {
    };

    public void testReadWrite() throws Exception {
        final int COUNT = 1024;
        File testFile = env.getTestCaseFile(this);
        FileChannel channel = new RandomAccessFile(testFile, "rw").getChannel();
        testReadWriteImpl(channel, COUNT);
        channel.close();
        channel = new MemoryFileChannel(64 * COUNT);
        testReadWriteImpl(channel, COUNT);
        channel.close();
    }

    public void testTransferFromBoundaryCondition() throws Exception {
        FileChannel channel;
        File testFile = env.getTestCaseFile(this);
        channel = new RandomAccessFile(testFile, "rw").getChannel();
        testTransferFromBoundaryConditionImpl(channel);
        channel.close();
        channel = new MemoryFileChannel(16);
        testTransferFromBoundaryConditionImpl(channel);
        channel.close();
    }

    private void testTransferFromBoundaryConditionImpl(FileChannel channel) throws Exception {
        channel.truncate(0);
        ByteBuffer buffer = ByteBuffer.allocate(16);
        for (int i = 16; i-- > 0; ) buffer.put((byte) i);
        buffer.flip();
        channelUtil.writeRemaining(channel, buffer);
        assertEquals(buffer.limit(), channel.size());
        assertEquals(buffer.limit(), channel.position());
        FileChannel arg = new RandomAccessFile(env.getTestCaseFile(this), "rw").getChannel();
        buffer.rewind().limit(8);
        channelUtil.writeRemaining(arg, buffer);
        arg.position(0);
        long amountWritten = channel.transferFrom(arg, channel.size() - 3, 4);
        assertTrue(amountWritten <= 4);
        assertTrue(amountWritten >= 0);
        amountWritten = channel.transferFrom(arg, channel.size() + 1, 4);
        assertEquals(0, amountWritten);
    }

    private void testReadWriteImpl(FileChannel channel, final int COUNT) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        for (int i = COUNT; i-- > 0; ) {
            buffer.clear();
            buffer.putInt(i).flip();
            channelUtil.writeRemaining(channel, buffer);
        }
        assertEquals(COUNT * 4, channel.position());
        assertEquals(COUNT * 4, channel.size());
        checkReadWriteTestContents(channel, COUNT);
        ByteBuffer[] buffers = new ByteBuffer[COUNT];
        for (int i = COUNT; i-- > 0; ) buffers[i] = ByteBuffer.allocate(4);
        channel.position(0);
        channelUtil.readRemaining(channel, buffers);
        for (int i = 0, count = COUNT; count-- > 0; ++i) {
            buffers[i].rewind();
            assertEquals(count, buffers[i].getInt());
        }
        File workFile = env.getTestCaseFile(this);
        FileChannel sink = new RandomAccessFile(workFile, "rw").getChannel();
        channelUtil.transferTo(channel, 0, channel.position(), sink);
        checkReadWriteTestContents(sink, COUNT);
        {
            buffer.clear();
            buffer.putInt(0);
            buffer.flip();
            channel.position(0);
            for (int i = COUNT; i-- > 0; ) {
                buffer.rewind();
                channelUtil.writeRemaining(channel, buffer);
            }
            channel.truncate(0);
            assertEquals(0, channel.size());
        }
        channelUtil.transferFrom(channel, 0, sink.size(), sink.position(0));
        checkReadWriteTestContents(channel, COUNT);
    }

    private void checkReadWriteTestContents(FileChannel channel, final int COUNT) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        channel.position(0);
        buffer.limit(4);
        for (int i = COUNT; i-- > 0; ) {
            buffer.rewind();
            channelUtil.readRemaining(channel, buffer);
            buffer.rewind();
            assertEquals(i, buffer.getInt());
        }
    }
}
