package org.jcvi.common.core.seq.read.trace.pyro.sff;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jcvi.common.core.io.IOUtil;
import org.jcvi.common.core.seq.read.trace.pyro.sff.DefaultSffReadData;
import org.jcvi.common.core.seq.read.trace.pyro.sff.DefaultSffReadDataDecoder;
import org.jcvi.common.core.seq.read.trace.pyro.sff.SffDecoderException;
import org.jcvi.common.core.seq.read.trace.pyro.sff.SffReadData;
import org.jcvi.common.core.seq.read.trace.pyro.sff.SffUtil;
import org.jcvi.common.core.symbol.residue.nt.NucleotideSequence;
import org.jcvi.common.core.symbol.residue.nt.NucleotideSequenceBuilder;
import org.jcvi.common.core.testUtil.EasyMockUtil;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

public class TestDefaultSffReadDataDecoder {

    protected int numberOfFlows = 5;

    protected int numberOfBases = 4;

    protected byte[] qualities = new byte[] { 20, 30, 40, 35 };

    protected short[] values = new short[] { 100, 8, 97, 4, 200 };

    protected byte[] indexes = new byte[] { 1, 2, 2, 0 };

    protected NucleotideSequence bases = new NucleotideSequenceBuilder("TATT").build();

    protected DefaultSffReadDataDecoder sut = DefaultSffReadDataDecoder.INSTANCE;

    protected DefaultSffReadData expectedReadData = new DefaultSffReadData(bases, indexes, values, qualities);

    @Test
    public void valid() throws SffDecoderException, IOException {
        InputStream mockInputStream = createMock(InputStream.class);
        encode(mockInputStream, expectedReadData);
        replay(mockInputStream);
        SffReadData actualReadData = sut.decode(new DataInputStream(mockInputStream), numberOfFlows, numberOfBases);
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
        } catch (SffDecoderException e) {
            assertEquals("error decoding read data", e.getMessage());
            assertEquals(ioException, e.getCause());
        }
        verify(mockInputStream);
    }

    void encode(InputStream mockInputStream, SffReadData readData) throws IOException {
        int basesLength = (int) readData.getBasecalls().getLength();
        int numberOfFlows = readData.getFlowgramValues().length;
        int readDataLength = numberOfFlows * 2 + 3 * numberOfBases;
        long padding = SffUtil.caclulatePaddedBytes(readDataLength);
        for (int i = 0; i < numberOfFlows; i++) {
            EasyMockUtil.putShort(mockInputStream, readData.getFlowgramValues()[i]);
        }
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(basesLength))).andAnswer(EasyMockUtil.writeArrayToInputStream(readData.getFlowIndexPerBase()));
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(basesLength))).andAnswer(EasyMockUtil.writeArrayToInputStream(readData.getBasecalls().toString().getBytes(IOUtil.UTF_8)));
        expect(mockInputStream.read(isA(byte[].class), eq(0), eq(basesLength))).andAnswer(EasyMockUtil.writeArrayToInputStream(readData.getQualities()));
        expect(mockInputStream.skip(padding)).andReturn(padding);
    }
}
