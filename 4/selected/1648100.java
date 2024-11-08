package org.jcvi.trace.sanger.chromatogram.abi;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.jcvi.io.fileServer.ResourceFileServer;
import org.jcvi.trace.TraceDecoderException;
import org.jcvi.trace.sanger.chromatogram.BasicChromatogramFile;
import org.jcvi.trace.sanger.chromatogram.ztr.ZTRChromatogramFileParser;
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

    @Before
    public void setup() throws FileNotFoundException, TraceDecoderException, IOException {
        expectedZTR = new BasicChromatogramFile();
        ZTRChromatogramFileParser.parseZTRFile(RESOURCES.getFile("files/SDBHD01T00PB1A1672F.ztr"), expectedZTR);
    }

    @Test
    public void abiVisitorMatchesZTR() throws FileNotFoundException, TraceDecoderException, IOException {
        BasicChromatogramFile actualAbi = new BasicChromatogramFile();
        Ab1FileParser.parseAb1File(RESOURCES.getFile("files/SDBHD01T00PB1A1672F.ab1"), actualAbi);
        assertEquals(expectedZTR.getBasecalls(), actualAbi.getBasecalls());
        assertEquals(expectedZTR.getPeaks(), actualAbi.getPeaks());
        assertEquals(expectedZTR.getQualities(), actualAbi.getQualities());
        assertEquals(expectedZTR.getChannelGroup(), actualAbi.getChannelGroup());
        assertEquals(expectedZTR.getNumberOfTracePositions(), actualAbi.getNumberOfTracePositions());
        assertEquals(expectedZTR.getProperties(), actualAbi.getProperties());
    }
}
