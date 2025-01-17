package org.jcvi.common.core.seq.read.trace.pyro.sff;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import org.jcvi.common.core.seq.read.trace.pyro.sff.SffCommonHeader;
import org.jcvi.common.core.seq.read.trace.pyro.sff.SffDecoderException;
import org.jcvi.common.core.seq.read.trace.pyro.sff.SffUtil;
import org.jcvi.common.core.testUtil.EasyMockUtil;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

public class TestDefaultSffCommonHeaderDecoder extends AbstractTestDefaultSFFCommonHeaderCodec {

    @Test
    public void valid() throws SffDecoderException, IOException {
        InputStream mockInputStream = createMock(InputStream.class);
        encode(mockInputStream, expectedHeader);
        replay(mockInputStream);
        SffCommonHeader actualHeader = sut.decodeHeader(new DataInputStream(mockInputStream));
        assertEquals(expectedHeader, actualHeader);
        verify(mockInputStream);
    }

    @Test
    public void invalidReadThrowsIOExceptionShouldWrapInSFFDecoderException() throws IOException {
        InputStream mockInputStream = createMock(InputStream.class);
        IOException ioException = new IOException("expected");
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andThrow(ioException);
        replay(mockInputStream);
        try {
            sut.decodeHeader(new DataInputStream(mockInputStream));
            fail("should wrap IOException in SFFDecoderException");
        } catch (SffDecoderException e) {
            assertEquals("error decoding sff file", e.getMessage());
            assertEquals(e.getCause(), ioException);
        }
        verify(mockInputStream);
    }

    @Test
    public void invalidReadFailsMagicNumberShouldThrowSFFDecoderException() throws IOException {
        InputStream mockInputStream = createMock(InputStream.class);
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andAnswer(EasyMockUtil.writeArrayToInputStream(".ZTR".getBytes()));
        replay(mockInputStream);
        try {
            sut.decodeHeader(new DataInputStream(mockInputStream));
            fail("should wrap IOException in SFFDecoderException");
        } catch (IOException e) {
            SffDecoderException decoderException = (SffDecoderException) e.getCause();
            assertEquals("magic number does not match expected", decoderException.getMessage());
            assertNull(decoderException.getCause());
        }
        verify(mockInputStream);
    }

    @Test
    public void invalidReadFailsInvalidVersionShouldThrowSFFDecoderException() throws IOException {
        InputStream mockInputStream = createMock(InputStream.class);
        encodeNotValidVersion(mockInputStream);
        replay(mockInputStream);
        try {
            sut.decodeHeader(new DataInputStream(mockInputStream));
            fail("should wrap IOException in SFFDecoderException");
        } catch (IOException e) {
            SffDecoderException decoderException = (SffDecoderException) e.getCause();
            assertEquals("version not compatible with decoder", decoderException.getMessage());
            assertNull(decoderException.getCause());
        }
        verify(mockInputStream);
    }

    @Test
    public void invalidReadFailsInvalidFormatCodeShouldThrowSFFDecoderException() throws IOException {
        InputStream mockInputStream = createMock(InputStream.class);
        invailidFormatCode(mockInputStream, expectedHeader);
        replay(mockInputStream);
        try {
            sut.decodeHeader(new DataInputStream(mockInputStream));
            fail("should wrap IOException in SFFDecoderException");
        } catch (IOException e) {
            SffDecoderException decoderException = (SffDecoderException) e.getCause();
            assertEquals("unknown flowgram format code", decoderException.getMessage());
            assertNull(decoderException.getCause());
        }
        verify(mockInputStream);
    }

    @Test
    public void invalidReadFailsFlowNotLongEnoughShouldThrowSFFDecoderException() throws IOException {
        InputStream mockInputStream = createMock(InputStream.class);
        invailidFlow(mockInputStream, expectedHeader);
        replay(mockInputStream);
        try {
            sut.decodeHeader(new DataInputStream(mockInputStream));
            fail("should wrap IOException in SFFDecoderException");
        } catch (IOException e) {
            SffDecoderException decoderException = (SffDecoderException) e.getCause();
            assertEquals("error decoding flow", decoderException.getMessage());
            assertTrue(decoderException.getCause() instanceof EOFException);
        }
        verify(mockInputStream);
    }

    @Test
    public void invalidReadFailsKeySequenceNotLongEnoughShouldThrowSFFDecoderException() throws IOException {
        InputStream mockInputStream = createMock(InputStream.class);
        invailidKeySequence(mockInputStream, expectedHeader);
        replay(mockInputStream);
        try {
            sut.decodeHeader(new DataInputStream(mockInputStream));
            fail("should wrap IOException in SFFDecoderException");
        } catch (IOException e) {
            SffDecoderException decoderException = (SffDecoderException) e.getCause();
            assertEquals("error decoding keySequence", decoderException.getMessage());
            assertTrue(decoderException.getCause() instanceof EOFException);
        }
        verify(mockInputStream);
    }

    void encodeNotValidVersion(InputStream mockInputStream) throws IOException {
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andAnswer(EasyMockUtil.writeArrayToInputStream(".sff".getBytes()));
        final byte[] invalidVersion = new byte[] { 0, 0, 0, 2 };
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andAnswer(EasyMockUtil.writeArrayToInputStream(invalidVersion));
    }

    void encode(InputStream mockInputStream, SffCommonHeader header) throws IOException {
        final short keyLength = (short) (header.getKeySequence().getLength());
        int size = 31 + header.getNumberOfFlowsPerRead() + keyLength;
        long padding = SffUtil.caclulatePaddedBytes(size);
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andAnswer(EasyMockUtil.writeArrayToInputStream(".sff".getBytes()));
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andAnswer(EasyMockUtil.writeArrayToInputStream(new byte[] { 0, 0, 0, 1 }));
        EasyMockUtil.putUnSignedLong(mockInputStream, header.getIndexOffset());
        EasyMockUtil.putUnSignedInt(mockInputStream, header.getIndexLength());
        EasyMockUtil.putUnSignedInt(mockInputStream, header.getNumberOfReads());
        EasyMockUtil.putUnSignedShort(mockInputStream, (short) (size + padding));
        EasyMockUtil.putUnSignedShort(mockInputStream, keyLength);
        EasyMockUtil.putUnSignedShort(mockInputStream, header.getNumberOfFlowsPerRead());
        EasyMockUtil.putByte(mockInputStream, (byte) 1);
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(header.getNumberOfFlowsPerRead()))).andAnswer(EasyMockUtil.writeArrayToInputStream(header.getFlowSequence().toString().getBytes()));
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq((int) keyLength))).andAnswer(EasyMockUtil.writeArrayToInputStream(header.getKeySequence().toString().getBytes()));
        expect(mockInputStream.skip(padding)).andReturn(padding);
    }

    void invailidFormatCode(InputStream mockInputStream, SffCommonHeader header) throws IOException {
        final short keyLength = (short) (header.getKeySequence().getLength());
        int size = 31 + header.getNumberOfFlowsPerRead() + keyLength;
        long padding = SffUtil.caclulatePaddedBytes(size);
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andAnswer(EasyMockUtil.writeArrayToInputStream(".sff".getBytes()));
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andAnswer(EasyMockUtil.writeArrayToInputStream(new byte[] { 0, 0, 0, 1 }));
        EasyMockUtil.putUnSignedLong(mockInputStream, header.getIndexOffset());
        EasyMockUtil.putUnSignedInt(mockInputStream, header.getIndexLength());
        EasyMockUtil.putUnSignedInt(mockInputStream, header.getNumberOfReads());
        EasyMockUtil.putUnSignedShort(mockInputStream, (short) (size + padding));
        EasyMockUtil.putUnSignedShort(mockInputStream, keyLength);
        EasyMockUtil.putUnSignedShort(mockInputStream, header.getNumberOfFlowsPerRead());
        EasyMockUtil.putByte(mockInputStream, (byte) 2);
    }

    void invailidFlow(InputStream mockInputStream, SffCommonHeader header) throws IOException {
        final short keyLength = (short) (header.getKeySequence().getLength());
        int size = 31 + header.getNumberOfFlowsPerRead() + keyLength;
        long padding = SffUtil.caclulatePaddedBytes(size);
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andAnswer(EasyMockUtil.writeArrayToInputStream(".sff".getBytes()));
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andAnswer(EasyMockUtil.writeArrayToInputStream(new byte[] { 0, 0, 0, 1 }));
        EasyMockUtil.putUnSignedLong(mockInputStream, header.getIndexOffset());
        EasyMockUtil.putUnSignedInt(mockInputStream, header.getIndexLength());
        EasyMockUtil.putUnSignedInt(mockInputStream, header.getNumberOfReads());
        EasyMockUtil.putUnSignedShort(mockInputStream, (short) (size + padding));
        EasyMockUtil.putUnSignedShort(mockInputStream, keyLength);
        EasyMockUtil.putUnSignedShort(mockInputStream, header.getNumberOfFlowsPerRead());
        EasyMockUtil.putByte(mockInputStream, (byte) 1);
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(header.getNumberOfFlowsPerRead()))).andAnswer(EasyMockUtil.writeArrayToInputStream(header.getFlowSequence().toString().substring(1).getBytes()));
        expect(mockInputStream.read(isA(byte[].class), eq(11), eq(1))).andReturn(-1);
    }

    void invailidKeySequence(InputStream mockInputStream, SffCommonHeader header) throws IOException {
        final short keyLength = (short) (header.getKeySequence().getLength());
        int size = 31 + header.getNumberOfFlowsPerRead() + keyLength;
        long padding = SffUtil.caclulatePaddedBytes(size);
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andAnswer(EasyMockUtil.writeArrayToInputStream(".sff".getBytes()));
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(4))).andAnswer(EasyMockUtil.writeArrayToInputStream(new byte[] { 0, 0, 0, 1 }));
        EasyMockUtil.putUnSignedLong(mockInputStream, header.getIndexOffset());
        EasyMockUtil.putUnSignedInt(mockInputStream, header.getIndexLength());
        EasyMockUtil.putUnSignedInt(mockInputStream, header.getNumberOfReads());
        EasyMockUtil.putUnSignedShort(mockInputStream, (short) (size + padding));
        EasyMockUtil.putUnSignedShort(mockInputStream, keyLength);
        EasyMockUtil.putUnSignedShort(mockInputStream, header.getNumberOfFlowsPerRead());
        EasyMockUtil.putByte(mockInputStream, (byte) 1);
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(header.getNumberOfFlowsPerRead()))).andAnswer(EasyMockUtil.writeArrayToInputStream(header.getFlowSequence().toString().getBytes()));
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq((int) keyLength))).andAnswer(EasyMockUtil.writeArrayToInputStream(header.getKeySequence().toString().substring(1).getBytes()));
        expect(mockInputStream.read(isA(byte[].class), eq(3), eq(1))).andReturn(-1);
    }
}
