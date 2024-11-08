package org.jcvi.trace.fourFiveFour.flowgram.sff;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jcvi.testUtil.EasyMockUtil;
import org.jcvi.trace.fourFiveFour.flowgram.sff.SFFDecoderException;
import org.jcvi.trace.fourFiveFour.flowgram.sff.SFFReadData;
import org.jcvi.trace.fourFiveFour.flowgram.sff.SFFUtil;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

public class TestDefaultSFFReadDataCodec_decode {

    protected int numberOfFlows = 5;

    protected int numberOfBases = 4;

    protected byte[] qualities = new byte[] { 20, 30, 40, 35 };

    protected short[] values = new short[] { 100, 8, 97, 4, 200 };

    protected byte[] indexes = new byte[] { 1, 2, 2, 0 };

    protected String bases = "TATT";

    protected DefaultSFFReadDataCodec sut = new DefaultSFFReadDataCodec();

    protected DefaultSFFReadData expectedReadData = new DefaultSFFReadData(bases, indexes, values, qualities);

    @Test
    public void valid() throws SFFDecoderException, IOException {
        InputStream mockInputStream = createMock(InputStream.class);
        encode(mockInputStream, expectedReadData);
        replay(mockInputStream);
        SFFReadData actualReadData = sut.decode(new DataInputStream(mockInputStream), numberOfFlows, numberOfBases);
        assertEquals(expectedReadData, actualReadData);
        verify(mockInputStream);
    }

    @Test
    public void readThrowsIOExceptionShouldWrapInSFFDecoderException() throws IOException {
        InputStream mockInputStream = createMock(InputStream.class);
        IOException ioException = new IOException("expected");
        expect(mockInputStream.read()).andThrow(ioException);
        replay(mockInputStream);
        try {
            sut.decode(new DataInputStream(mockInputStream), numberOfFlows, numberOfBases);
            fail("IOException should be wrapped in SFFDecoderException");
        } catch (SFFDecoderException e) {
            assertEquals("error decoding read data", e.getMessage());
            assertEquals(ioException, e.getCause());
        }
        verify(mockInputStream);
    }

    void encode(InputStream mockInputStream, SFFReadData readData) throws IOException {
        int basesLength = readData.getBasecalls().length();
        int numberOfFlows = readData.getFlowgramValues().length;
        int readDataLength = numberOfFlows * 2 + 3 * numberOfBases;
        long padding = SFFUtil.caclulatePaddedBytes(readDataLength);
        for (int i = 0; i < numberOfFlows; i++) {
            EasyMockUtil.putShort(mockInputStream, readData.getFlowgramValues()[i]);
        }
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(basesLength))).andAnswer(EasyMockUtil.writeArrayToInputStream(readData.getFlowIndexPerBase()));
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(basesLength))).andAnswer(EasyMockUtil.writeArrayToInputStream(readData.getBasecalls().getBytes()));
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(basesLength))).andAnswer(EasyMockUtil.writeArrayToInputStream(readData.getQualities()));
        expect(mockInputStream.skip(padding)).andReturn(padding);
    }
}
