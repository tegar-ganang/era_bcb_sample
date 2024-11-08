package org.jcvi.common.core.seq.read.trace.sanger.chromat.scf;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.jcvi.common.core.seq.read.trace.TraceDecoderException;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.Chromatogram;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.scf.SCFCodecs;
import org.jcvi.common.io.fileServer.ResourceFileServer;
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
        Chromatogram version2 = (Chromatogram) SCFCodecs.VERSION_2.decode(RESOURCES.getFile("files/version2.scf"));
        Chromatogram version3 = (Chromatogram) SCFCodecs.VERSION_3.decode(RESOURCES.getFile("files/version3.scf"));
        assertEquals(version3.getNucleotideSequence().asList(), version2.getNucleotideSequence().asList());
        assertEquals(version3.getQualities().asList(), version2.getQualities().asList());
        assertEquals(version3.getPeaks().getData().asList(), version2.getPeaks().getData().asList());
        assertEquals(version3.getNumberOfTracePositions(), version2.getNumberOfTracePositions());
        assertEquals(version3.getChannelGroup(), version2.getChannelGroup());
    }
}
