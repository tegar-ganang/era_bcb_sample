package org.jcvi.trace.nextera;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.jcvi.assembly.trim.DefaultPrimerTrimmer;
import org.jcvi.assembly.trim.PrimerTrimmer;
import org.jcvi.common.util.Range;
import org.jcvi.common.util.Range.CoordinateSystem;
import org.jcvi.datastore.SimpleDataStore;
import org.jcvi.glyph.nuc.NucleotideDataStore;
import org.jcvi.glyph.nuc.NucleotideEncodedGlyphs;
import org.jcvi.glyph.nuc.datastore.NucleotideDataStoreAdapter;
import org.jcvi.io.IOUtil;
import org.jcvi.trace.fourFiveFour.flowgram.sff.AbstractSffFileVisitor;
import org.jcvi.trace.fourFiveFour.flowgram.sff.DefaultSFFCommonHeader;
import org.jcvi.trace.fourFiveFour.flowgram.sff.DefaultSFFReadHeader;
import org.jcvi.trace.fourFiveFour.flowgram.sff.SFFCommonHeader;
import org.jcvi.trace.fourFiveFour.flowgram.sff.SFFReadData;
import org.jcvi.trace.fourFiveFour.flowgram.sff.SFFReadHeader;
import org.jcvi.trace.fourFiveFour.flowgram.sff.SffParser;
import org.jcvi.trace.fourFiveFour.flowgram.sff.SffWriter;

/**
 * @author dkatzel
 *
 *
 */
public class TrimNexteraSff {

    private static final PrimerTrimmer nexteraTransposonTrimmer = new DefaultPrimerTrimmer(13, .9f, false);

    private static final NucleotideDataStore forwardTransposonDataStore;

    private static final NucleotideDataStore reverseTransposonDataStore;

    static {
        Map<String, NucleotideEncodedGlyphs> forwardTransposon = new HashMap<String, NucleotideEncodedGlyphs>();
        Map<String, NucleotideEncodedGlyphs> revesrseTransposon = new HashMap<String, NucleotideEncodedGlyphs>();
        forwardTransposon.put("5'", TransposonEndSequences.FORWARD);
        revesrseTransposon.put("3'", TransposonEndSequences.REVERSE);
        forwardTransposonDataStore = new NucleotideDataStoreAdapter(new SimpleDataStore<NucleotideEncodedGlyphs>(forwardTransposon));
        reverseTransposonDataStore = new NucleotideDataStoreAdapter(new SimpleDataStore<NucleotideEncodedGlyphs>(revesrseTransposon));
    }

    private DefaultSFFCommonHeader.Builder headerBuilder;

    private long numberOfTrimmedReads = 0;

    private final OutputStream tempOut;

    private final File untrimmedSffFile;

    private final File tempReadDataFile;

    public TrimNexteraSff(File untrimmedSffFile) throws IOException {
        this(untrimmedSffFile, File.createTempFile("nexteraTrimmed", "reads.sff"));
    }

    public TrimNexteraSff(File untrimmedSffFile, File tempReadDataFile) throws IOException {
        tempOut = new FileOutputStream(tempReadDataFile);
        this.untrimmedSffFile = untrimmedSffFile;
        this.tempReadDataFile = tempReadDataFile;
    }

    public void trimAndWriteNewSff(OutputStream out) throws IOException {
        TrimParser trimmer = new TrimParser();
        SffParser.parseSFF(untrimmedSffFile, trimmer);
        tempOut.close();
        headerBuilder.withNoIndex().numberOfReads(numberOfTrimmedReads);
        SffWriter.writeCommonHeader(headerBuilder.build(), out);
        InputStream in = null;
        try {
            in = new FileInputStream(tempReadDataFile);
            IOUtils.copyLarge(in, out);
        } finally {
            IOUtil.closeAndIgnoreErrors(in);
        }
    }

    private class TrimParser extends AbstractSffFileVisitor {

        private SFFReadHeader currentReadHeader;

        @Override
        public boolean visitCommonHeader(SFFCommonHeader commonHeader) {
            headerBuilder = new DefaultSFFCommonHeader.Builder(commonHeader);
            return true;
        }

        @Override
        public boolean visitReadData(SFFReadData readData) {
            Range forwardClearRange = nexteraTransposonTrimmer.trim(readData.getBasecalls(), forwardTransposonDataStore);
            Range reverseClearRange = nexteraTransposonTrimmer.trim(readData.getBasecalls(), reverseTransposonDataStore);
            final Range clearRange;
            if (reverseClearRange.isSubRangeOf(forwardClearRange)) {
                clearRange = Range.buildRange(CoordinateSystem.RESIDUE_BASED, forwardClearRange.getLocalStart(), reverseClearRange.getLocalEnd());
            } else {
                clearRange = forwardClearRange.intersection(reverseClearRange);
            }
            if (forwardClearRange.getStart() != 0) {
                numberOfTrimmedReads++;
                try {
                    DefaultSFFReadHeader.Builder builder = new DefaultSFFReadHeader.Builder(currentReadHeader);
                    builder.qualityClip(clearRange);
                    SffWriter.writeReadHeader(builder.build(), tempOut);
                    SffWriter.writeReadData(readData, tempOut);
                } catch (IOException e) {
                    throw new IllegalStateException("error writing read data to temp", e);
                }
            } else {
                System.out.println("skipping " + currentReadHeader.getName());
            }
            return true;
        }

        @Override
        public boolean visitReadHeader(SFFReadHeader readHeader) {
            currentReadHeader = readHeader;
            return true;
        }
    }

    public static void main(String[] args) throws IOException {
        File sffFile = new File("/usr/local/seq454/2010_10_19/R_2010_10_19_10_35_13_FLX02080319_Administrator_10" + "1910R1BBAY62R2FIBRBAC/D_2010_10_19_22_43_31_dell-2-0-" + "1_signalProcessing/sff/GPQK0ZM02.sff");
        File outputSffFile = new File("/usr/local/scratch/dkatzel/GPQK0ZM02.trimmed.sff_3");
        System.out.println(sffFile.getAbsolutePath());
        File tempFile = File.createTempFile("nextera", "sff.reads", new File("/usr/local/scratch/dkatzel"));
        OutputStream out = new FileOutputStream(outputSffFile);
        TrimNexteraSff trimmer = new TrimNexteraSff(sffFile, tempFile);
        trimmer.trimAndWriteNewSff(out);
        IOUtil.closeAndIgnoreErrors(out);
    }
}
