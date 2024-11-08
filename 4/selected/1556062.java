package org.jcvi.trace.sanger.chromatogram.ztr.chunk;

import java.nio.ByteBuffer;
import org.jcvi.glyph.nuc.DefaultNucleotideEncodedGlyphs;
import org.jcvi.trace.TraceDecoderException;
import org.jcvi.trace.TraceEncoderException;
import org.jcvi.trace.sanger.chromatogram.Channel;
import org.jcvi.trace.sanger.chromatogram.ChannelGroup;
import org.jcvi.trace.sanger.chromatogram.DefaultChannelGroup;
import org.jcvi.trace.sanger.chromatogram.ztr.ZTRChromatogram;
import org.jcvi.trace.sanger.chromatogram.ztr.ZTRChromatogramBuilder;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

public class TestCNF4Chunk {

    private static final String bases = "ACGT-";

    private static final byte[] aconf = new byte[] { 40, 0, 0, 0, 0 };

    private static final byte[] cconf = new byte[] { 0, 30, 0, 0, 0 };

    private static final byte[] gconf = new byte[] { 0, 0, 35, 0, 0 };

    private static final byte[] tconf = new byte[] { 0, 0, 0, 38, 37 };

    Chunk sut = Chunk.CONFIDENCES;

    private static final byte[] encodedBytes;

    static {
        ByteBuffer buf = ByteBuffer.allocate(bases.length() * 4 + 1);
        buf.put((byte) 0);
        buf.put(aconf[0]);
        buf.put(cconf[1]);
        buf.put(gconf[2]);
        buf.put(tconf[3]);
        buf.put(tconf[4]);
        buf.put(cconf[0]);
        buf.put(gconf[0]);
        buf.put(tconf[0]);
        buf.put(aconf[1]);
        buf.put(gconf[1]);
        buf.put(tconf[1]);
        buf.put(aconf[2]);
        buf.put(cconf[2]);
        buf.put(tconf[2]);
        buf.put(aconf[3]);
        buf.put(cconf[3]);
        buf.put(gconf[3]);
        buf.put(aconf[4]);
        buf.put(cconf[4]);
        buf.put(gconf[4]);
        encodedBytes = buf.array();
    }

    @Test
    public void parse() throws TraceDecoderException {
        ZTRChromatogramBuilder struct = new ZTRChromatogramBuilder();
        struct.basecalls(bases);
        sut.parseData(encodedBytes, struct);
        assertArrayEquals(aconf, struct.aConfidence());
        assertArrayEquals(cconf, struct.cConfidence());
        assertArrayEquals(gconf, struct.gConfidence());
        assertArrayEquals(tconf, struct.tConfidence());
    }

    @Test
    public void encode() throws TraceEncoderException {
        ZTRChromatogram chromatogram = createMock(ZTRChromatogram.class);
        ChannelGroup channelGroup = new DefaultChannelGroup(new Channel(aconf, new short[0]), new Channel(cconf, new short[0]), new Channel(gconf, new short[0]), new Channel(tconf, new short[0]));
        expect(chromatogram.getBasecalls()).andReturn(new DefaultNucleotideEncodedGlyphs(bases));
        expect(chromatogram.getChannelGroup()).andReturn(channelGroup);
        replay(chromatogram);
        byte[] actual = sut.encodeChunk(chromatogram);
        assertArrayEquals(encodedBytes, actual);
        verify(chromatogram);
    }
}
