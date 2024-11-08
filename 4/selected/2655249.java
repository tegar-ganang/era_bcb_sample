package org.jcvi.common.core.seq.read.trace.sanger.chromat.ztr.chunk;

import java.nio.ByteBuffer;
import org.jcvi.common.core.seq.read.trace.TraceDecoderException;
import org.jcvi.common.core.seq.read.trace.TraceEncoderException;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.Channel;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.ChannelGroup;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.DefaultChannelGroup;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.ztr.ZTRChromatogram;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.ztr.ZTRChromatogramBuilder;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.ztr.chunk.Chunk;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

public class TestSMP4Chunk {

    private static short[] aTraces = new short[] { 0, 0, 2, 4, 5, 3, 2, 0, 0, 0, 1 };

    private static short[] cTraces = new short[] { 7, 5, 2, 0, 1, 0, 2, 1, 1, 0, 1 };

    private static short[] gTraces = new short[] { 1, 0, 0, 2, 1, 0, 3, 8, 4, 2, 0 };

    private static short[] tTraces = new short[] { 0, 0, 2, 4, 2, 3, 2, 0, 5, 8, 25 };

    Chunk sut = Chunk.SMP4;

    private static final byte[] encodedBytes;

    static {
        ByteBuffer buf = ByteBuffer.allocate(aTraces.length * 8 + 2);
        buf.putShort((short) 0);
        for (int i = 0; i < aTraces.length; i++) {
            buf.putShort(aTraces[i]);
        }
        for (int i = 0; i < aTraces.length; i++) {
            buf.putShort(cTraces[i]);
        }
        for (int i = 0; i < aTraces.length; i++) {
            buf.putShort(gTraces[i]);
        }
        for (int i = 0; i < aTraces.length; i++) {
            buf.putShort(tTraces[i]);
        }
        encodedBytes = buf.array();
    }

    @Test
    public void parse() throws TraceDecoderException {
        ZTRChromatogramBuilder struct = new ZTRChromatogramBuilder("id");
        sut.parseData(encodedBytes, struct);
        assertArrayEquals(struct.aPositions(), aTraces);
        assertArrayEquals(struct.cPositions(), cTraces);
        assertArrayEquals(struct.gPositions(), gTraces);
        assertArrayEquals(struct.tPositions(), tTraces);
    }

    @Test
    public void encode() throws TraceEncoderException {
        ZTRChromatogram mockChromatogram = createMock(ZTRChromatogram.class);
        ChannelGroup channelGroup = new DefaultChannelGroup(new Channel(new byte[0], aTraces), new Channel(new byte[0], cTraces), new Channel(new byte[0], gTraces), new Channel(new byte[0], tTraces));
        expect(mockChromatogram.getNumberOfTracePositions()).andReturn(aTraces.length);
        expect(mockChromatogram.getChannelGroup()).andReturn(channelGroup);
        replay(mockChromatogram);
        byte[] actual = sut.encodeChunk(mockChromatogram);
        assertArrayEquals(encodedBytes, actual);
        verify(mockChromatogram);
    }
}
