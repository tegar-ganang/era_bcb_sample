package org.jcvi.trace.sanger.chromatogram.scf;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.jcvi.io.fileServer.ResourceFileServer;
import org.jcvi.trace.TraceDecoderException;
import org.jcvi.trace.sanger.chromatogram.Chromatogram;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author dkatzel
 *
 *
 */
public class TestVersion2Parser {

    private static final ResourceFileServer RESOURCES = new ResourceFileServer(TestVersion2Parser.class);

    @Test
    public void version2MatchesVersion3() throws TraceDecoderException, FileNotFoundException, IOException {
        Chromatogram version2 = (Chromatogram) Version2SCFCodec.INSTANCE.decode(RESOURCES.getFile("files/version2.scf"));
        Chromatogram version3 = (Chromatogram) Version3SCFCodec.INSTANCE.decode(RESOURCES.getFile("files/version3.scf"));
        assertEquals(version3.getBasecalls().decode(), version2.getBasecalls().decode());
        assertEquals(version3.getQualities().decode(), version2.getQualities().decode());
        assertEquals(version3.getPeaks().getData().decode(), version2.getPeaks().getData().decode());
        assertEquals(version3.getNumberOfTracePositions(), version2.getNumberOfTracePositions());
        assertEquals(version3.getChannelGroup(), version2.getChannelGroup());
    }
}
