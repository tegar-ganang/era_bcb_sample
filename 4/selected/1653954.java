package org.jcvi.common.core.seq.read.trace.pyro.sff;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jcvi.common.core.Range.CoordinateSystem;
import org.jcvi.common.core.seq.read.trace.pyro.sff.SffDecoderException;
import org.jcvi.common.core.seq.read.trace.pyro.sff.SffReadHeader;
import org.jcvi.common.core.seq.read.trace.pyro.sff.SffUtil;
import org.jcvi.common.core.testUtil.EasyMockUtil;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.jcvi.common.core.testUtil.EasyMockUtil.*;
import static org.easymock.EasyMock.*;

public class TestSffeadHeaderDecoder extends AbstractTestSFFReadHeaderCodec {

    @Test
    public void valid() throws SffDecoderException, IOException {
        InputStream mockInputStream = createMock(InputStream.class);
        encodeHeader(mockInputStream, expectedReadHeader);
        replay(mockInputStream);
        SffReadHeader actualReadHeader = sut.decodeReadHeader(new DataInputStream(mockInputStream));
        assertEquals(actualReadHeader, expectedReadHeader);
        verify(mockInputStream);
    }

    @Test
    public void sequenceNameLengthEncodedIncorrectlyShouldThrowIOException() throws IOException {
        InputStream mockInputStream = createMock(InputStream.class);
        encodeHeaderWithWrongSequenceLength(mockInputStream, expectedReadHeader);
        replay(mockInputStream);
        try {
            sut.decodeReadHeader(new DataInputStream(mockInputStream));
            fail("should throw SFFDecoderException if name length encoded wrong");
        } catch (IOException e) {
            Throwable cause = e.getCause();
            assertEquals("error decoding seq name", cause.getMessage());
        }
        verify(mockInputStream);
    }

    @Test
    public void readThrowsIOExceptionShouldWrapInSFFDecoderException() throws IOException {
        InputStream mockInputStream = createMock(InputStream.class);
        IOException expectedIOException = new IOException("expected");
        expect(mockInputStream.read()).andThrow(expectedIOException);
        replay(mockInputStream);
        try {
            sut.decodeReadHeader(new DataInputStream(mockInputStream));
            fail("should wrap IOException in SFFDecoderException");
        } catch (SffDecoderException e) {
            assertEquals("error trying to decode read header", e.getMessage());
            assertEquals(expectedIOException, e.getCause());
        }
        verify(mockInputStream);
    }

    void encodeHeader(InputStream mockInputStream, SffReadHeader readHeader) throws IOException {
        final String seqName = readHeader.getId();
        final int nameLength = seqName.length();
        int unpaddedLength = 16 + nameLength;
        final long padds = SffUtil.caclulatePaddedBytes(unpaddedLength);
        putShort(mockInputStream, (short) (padds + unpaddedLength));
        putShort(mockInputStream, (short) nameLength);
        putInt(mockInputStream, readHeader.getNumberOfBases());
        putShort(mockInputStream, (short) readHeader.getQualityClip().getBegin(CoordinateSystem.RESIDUE_BASED));
        putShort(mockInputStream, (short) readHeader.getQualityClip().getEnd(CoordinateSystem.RESIDUE_BASED));
        putShort(mockInputStream, (short) readHeader.getAdapterClip().getBegin(CoordinateSystem.RESIDUE_BASED));
        putShort(mockInputStream, (short) readHeader.getAdapterClip().getEnd(CoordinateSystem.RESIDUE_BASED));
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(nameLength))).andAnswer(EasyMockUtil.writeArrayToInputStream(seqName.getBytes()));
        expect(mockInputStream.skip(padds)).andReturn(padds);
    }

    void encodeHeaderWithWrongSequenceLength(InputStream mockInputStream, SffReadHeader readHeader) throws IOException {
        final String seqName = readHeader.getId();
        final int nameLength = seqName.length();
        int unpaddedLength = 16 + nameLength;
        final long padds = SffUtil.caclulatePaddedBytes(unpaddedLength);
        putShort(mockInputStream, (short) (padds + unpaddedLength));
        putShort(mockInputStream, (short) (nameLength + 1));
        putInt(mockInputStream, readHeader.getNumberOfBases());
        putShort(mockInputStream, (short) readHeader.getQualityClip().getBegin());
        putShort(mockInputStream, (short) readHeader.getQualityClip().getEnd());
        putShort(mockInputStream, (short) readHeader.getAdapterClip().getBegin());
        putShort(mockInputStream, (short) readHeader.getAdapterClip().getEnd());
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(nameLength + 1))).andAnswer(EasyMockUtil.writeArrayToInputStream(seqName.getBytes()));
        expect(mockInputStream.read(isA(byte[].class), eq(13), eq(1))).andReturn(-1);
    }
}
