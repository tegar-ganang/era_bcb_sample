package org.jcvi.common.core.seq.read.trace.sanger.chromat.scf.section;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.scf.section.SectionDecoderException;
import org.jcvi.common.core.testUtil.EasyMockUtil;
import org.junit.Test;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class TestCommentSectionDecoder extends AbstractTestCommentSection {

    @Test
    public void valid() throws SectionDecoderException {
        final String scfCommentAsString = convertPropertiesToSCFComment(expectedComments);
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(scfCommentAsString.getBytes()));
        expect(mockHeader.getCommentOffset()).andReturn(currentOffset);
        expect(mockHeader.getCommentSize()).andReturn(scfCommentAsString.length());
        replay(mockHeader);
        long newOffset = sut.decode(in, currentOffset, mockHeader, chromaStruct);
        verify(mockHeader);
        assertEquals(expectedComments, chromaStruct.properties());
        assertEquals(scfCommentAsString.length(), newOffset);
    }

    @Test
    public void validMustSkipToStartOfCommentSection() throws Exception {
        final String scfCommentAsString = convertPropertiesToSCFComment(expectedComments);
        int distanceToSkip = 200;
        InputStream mockInputStream = createMock(InputStream.class);
        expect(mockInputStream.skip(distanceToSkip)).andReturn((long) distanceToSkip);
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(scfCommentAsString.length()))).andAnswer(EasyMockUtil.writeArrayToInputStream(scfCommentAsString.getBytes()));
        expect(mockHeader.getCommentOffset()).andReturn(distanceToSkip);
        expect(mockHeader.getCommentSize()).andReturn(scfCommentAsString.length());
        replay(mockHeader, mockInputStream);
        long newOffset = sut.decode(new DataInputStream(mockInputStream), currentOffset, mockHeader, chromaStruct);
        verify(mockHeader, mockInputStream);
        assertEquals(expectedComments, chromaStruct.properties());
        assertEquals(scfCommentAsString.length() + distanceToSkip, newOffset);
    }

    @Test
    public void readThrowsIOExceptionShouldWrapInSectionParserException() throws IOException {
        InputStream mockInputStream = createMock(InputStream.class);
        IOException expectedException = new IOException("expected");
        expect(mockHeader.getCommentOffset()).andReturn(currentOffset);
        int commentLength = 10;
        expect(mockHeader.getCommentSize()).andReturn(commentLength);
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(commentLength))).andThrow(expectedException);
        replay(mockHeader, mockInputStream);
        try {
            sut.decode(new DataInputStream(mockInputStream), currentOffset, mockHeader, chromaStruct);
            fail("should throw SectionParserException on error");
        } catch (SectionDecoderException e) {
        }
        verify(mockHeader);
        assertNull(chromaStruct.properties());
    }
}
