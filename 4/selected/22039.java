package org.jcvi.common.core.seq.read.trace.sanger.chromat.ab1;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.jcvi.common.core.seq.read.trace.TraceDecoderException;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.BasicChromatogramFile;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.ab1.Ab1FileParser;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.ztr.ZTRChromatogramFileParser;
import org.jcvi.common.io.fileServer.ResourceFileServer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author dkatzel
 *
 *
 */
public class TestAbiChromatogramTraceParserMatchesZTR {

    private static ResourceFileServer RESOURCES = new ResourceFileServer(TestAbiChromatogramTraceParserMatchesZTR.class);

    BasicChromatogramFile expectedZTR;

    String id = "id";

    @Before
    public void setup() throws FileNotFoundException, TraceDecoderException, IOException {
        expectedZTR = new BasicChromatogramFile(id);
        ZTRChromatogramFileParser.parse(RESOURCES.getFile("files/SDBHD01T00PB1A1672F.ztr"), expectedZTR);
    }

    @Test
    public void abiVisitorMatchesZTR() throws FileNotFoundException, TraceDecoderException, IOException {
        BasicChromatogramFile actualAbi = new BasicChromatogramFile(id);
        Ab1FileParser.parse(RESOURCES.getFile("files/SDBHD01T00PB1A1672F.ab1"), actualAbi);
        assertEquals(expectedZTR.getNucleotideSequence(), actualAbi.getNucleotideSequence());
        assertEquals(expectedZTR.getPeaks(), actualAbi.getPeaks());
        assertEquals(expectedZTR.getQualities(), actualAbi.getQualities());
        assertEquals(expectedZTR.getChannelGroup(), actualAbi.getChannelGroup());
        assertEquals(expectedZTR.getNumberOfTracePositions(), actualAbi.getNumberOfTracePositions());
        assertEquals(expectedZTR.getComments(), actualAbi.getComments());
    }
}
