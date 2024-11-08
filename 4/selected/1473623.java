package org.jcvi.common.core.seq.read.trace.sanger.chromat;

import java.io.File;
import java.io.IOException;
import org.jcvi.common.core.seq.read.trace.TraceDecoderException;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.scf.SCFChromatogram;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.scf.SCFChromatogramFile;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.scf.SCFChromatogramFileParser;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.scf.SCFChromatogramFile.SCFChromatogramFileBuilderVisitor;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.ztr.ZTRChromatogram;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.ztr.ZTRChromatogramFile;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.ztr.ZTRChromatogramFileParser;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.ztr.ZTRChromatogramFile.ZTRChromatogramFileBuilderVisitor;
import org.jcvi.common.io.fileServer.ResourceFileServer;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author dkatzel
 *
 *
 */
public class TestMixAndMatchChromatogramParsers {

    private static final ResourceFileServer RESOURCES = new ResourceFileServer(TestMixAndMatchChromatogramParsers.class);

    @Test
    public void parseZtrAsScfFile() throws IOException, TraceDecoderException {
        File ztrFile = RESOURCES.getFile("ztr/files/GBKAK82TF.ztr");
        ZTRChromatogram ztr = ZTRChromatogramFile.create(ztrFile);
        SCFChromatogramFileBuilderVisitor visitor = SCFChromatogramFile.createNewBuilderVisitor(ztrFile.getName());
        ZTRChromatogramFileParser.parse(ztrFile, visitor);
        assertValuesMatch(visitor.build(), ztr);
    }

    @Test
    public void parseScfAsZtrFile() throws IOException, TraceDecoderException {
        File scfFile = RESOURCES.getFile("scf/files/GBKAK82TF.scf");
        SCFChromatogram scf = SCFChromatogramFile.create(scfFile);
        ZTRChromatogramFileBuilderVisitor visitor = ZTRChromatogramFile.createNewBuilderVisitor(scfFile.getName());
        SCFChromatogramFileParser.parse(scfFile, visitor);
        assertValuesMatch(scf, visitor.build());
    }

    protected void assertValuesMatch(SCFChromatogram scf, ZTRChromatogram ztr) {
        assertEquals(ztr.getNucleotideSequence(), scf.getNucleotideSequence());
        assertEquals(ztr.getPeaks(), scf.getPeaks());
        assertEquals(ztr.getQualities(), scf.getQualities());
        assertEquals(ztr.getChannelGroup(), scf.getChannelGroup());
        assertEquals(ztr.getNumberOfTracePositions(), scf.getNumberOfTracePositions());
    }
}
