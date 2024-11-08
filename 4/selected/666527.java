package org.jcvi.common.core.seq.read.trace.sanger.chromat.ztr.chunk;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.jcvi.common.core.seq.read.trace.TraceDecoderException;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.ztr.chunk.Chunk;
import org.jcvi.common.core.testUtil.EasyMockUtil;
import org.junit.Before;
import org.junit.Test;
import static org.easymock.EasyMock.*;

public class TestChunk {

    Chunk sut = Chunk.BASE;

    private InputStream mockInputStream;

    IOException expectedException = new IOException("expected");

    @Before
    public void setup() {
        mockInputStream = createMock(InputStream.class);
    }

    @Test
    public void readLength() throws TraceDecoderException, IOException {
        int length = 20;
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt(length);
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andAnswer(EasyMockUtil.writeArrayToInputStream(buf.array()));
        replay(mockInputStream);
        assertEquals(length, sut.readLength(mockInputStream));
        verify(mockInputStream);
    }

    @Test
    public void readLengthThrowsIOExceptionShouldWrapInTraceDecoderException() throws IOException {
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andThrow(expectedException);
        replay(mockInputStream);
        try {
            sut.readLength(mockInputStream);
            fail("should throw TraceDecoderException which WrapsIOException");
        } catch (TraceDecoderException e) {
            assertEquals("error reading chunk length", e.getMessage());
            assertEquals(expectedException, e.getCause());
        }
        verify(mockInputStream);
    }

    @Test
    public void readLengthNotEnoughBytesReadShouldWrapInTraceDecoderException() throws IOException {
        byte[] tooSmall = new byte[] { 1, 2, 3 };
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andAnswer(EasyMockUtil.writeArrayToInputStream(tooSmall));
        expect(mockInputStream.read(isA(byte[].class), eq(3), eq(1))).andReturn(-1);
        replay(mockInputStream);
        try {
            sut.readLength(mockInputStream);
            fail("should throw TraceDecoderException when too small");
        } catch (TraceDecoderException e) {
            assertEquals("error reading chunk length", e.getMessage());
            TraceDecoderException cause = (TraceDecoderException) e.getCause();
            assertEquals("invalid metaData length", cause.getMessage());
        }
        verify(mockInputStream);
    }

    @Test
    public void readMetaData() throws IOException, TraceDecoderException {
        int lengthToSkip = 1234;
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt(lengthToSkip);
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andAnswer(EasyMockUtil.writeArrayToInputStream(buf.array()));
        expect(mockInputStream.skip(lengthToSkip)).andReturn((long) lengthToSkip);
        replay(mockInputStream);
        sut.readMetaData(mockInputStream);
        verify(mockInputStream);
    }

    @Test
    public void readMetaDataSkipThrowsException() throws IOException {
        int lengthToSkip = 1234;
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt(lengthToSkip);
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andAnswer(EasyMockUtil.writeArrayToInputStream(buf.array()));
        expect(mockInputStream.skip(lengthToSkip)).andThrow(expectedException);
        replay(mockInputStream);
        try {
            sut.readMetaData(mockInputStream);
            fail("should wrap IOException in TraceDecoderException");
        } catch (TraceDecoderException e) {
            assertEquals("error reading chunk meta data", e.getMessage());
            assertEquals(expectedException, e.getCause());
        }
        verify(mockInputStream);
    }
}
