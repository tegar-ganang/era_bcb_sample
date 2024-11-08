package org.jcvi.glk.elvira.nextgen.clc;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import org.jcvi.common.core.Range;
import org.jcvi.common.core.Range.CoordinateSystem;
import org.jcvi.common.core.io.IOUtil;
import org.jcvi.common.core.seq.fastx.fasta.nt.DefaultNucleotideSequenceFastaRecord;
import org.jcvi.common.core.seq.fastx.fasta.pos.DefaultPositionSequenceFastaRecord;
import org.jcvi.common.core.seq.fastx.fasta.qual.DefaultQualityFastaRecord;
import org.jcvi.common.core.symbol.Sequence;
import org.jcvi.common.core.symbol.ShortSymbol;
import org.jcvi.common.core.symbol.qual.QualitySequence;
import org.jcvi.common.core.symbol.residue.nt.NucleotideSequence;
import org.jcvi.common.core.symbol.residue.nt.Nucleotides;
import org.jcvi.common.io.fileServer.DirectoryFileServer;
import org.jcvi.common.io.fileServer.DirectoryFileServer.ReadWriteDirectoryFileServer;
import org.jcvi.glk.elvira.nextgen.ElviraTuple;
import org.jcvi.glk.elvira.nextgen.NextGenUtil;

public final class CLCPipelineUtils {

    private CLCPipelineUtils() {
        throw new IllegalStateException("can not instantiate");
    }

    /**
     * Create a new SangerTrimFilesCreator without appending to existing files.
     * this is the same as {@link #createSangerTrimFilesBuilderFor(ElviraTuple, boolean) createSangerTrimFilesBuilderFor(tuple, false)}.
     * @param tuple
     * @return
     * @throws IOException
     */
    public static SangerTrimFilesCreator createSangerTrimFilesBuilderFor(ElviraTuple tuple) throws IOException {
        return createSangerTrimFilesBuilderFor(tuple, false);
    }

    public static SangerTrimFilesCreator createSangerTrimFilesBuilderFor(ElviraTuple tuple, boolean appendToExistingFiles) throws IOException {
        return createSangerTrimFilesBuilderFor(tuple, NextGenUtil.DEFAULT_FILE_ROOT, appendToExistingFiles);
    }

    public static SangerTrimFilesCreator createSangerTrimFilesBuilderFor(ElviraTuple tuple, File projectRoot) throws IOException {
        return new SangerTrimFilesCreator(tuple, projectRoot, false, false);
    }

    public static SangerTrimFilesCreator createSangerTrimFilesBuilderFor(ElviraTuple tuple, File projectRoot, boolean appendToExistingFiles) throws IOException {
        return new SangerTrimFilesCreator(tuple, projectRoot, appendToExistingFiles, false);
    }

    public static SangerTrimFilesCreator createEditedSangerTrimFilesBuilderFor(ElviraTuple tuple) throws IOException {
        return createEditedSangerTrimFilesBuilderFor(tuple, NextGenUtil.DEFAULT_FILE_ROOT);
    }

    public static SangerTrimFilesCreator createEditedSangerTrimFilesBuilderFor(ElviraTuple tuple, boolean appendToExistingFiles) throws IOException {
        return createEditedSangerTrimFilesBuilderFor(tuple, NextGenUtil.DEFAULT_FILE_ROOT, appendToExistingFiles);
    }

    public static SangerTrimFilesCreator createEditedSangerTrimFilesBuilderFor(ElviraTuple tuple, File projectRoot) throws IOException {
        return createEditedSangerTrimFilesBuilderFor(tuple, projectRoot, false);
    }

    public static SangerTrimFilesCreator createEditedSangerTrimFilesBuilderFor(ElviraTuple tuple, File projectRoot, boolean appendToExistingFiles) throws IOException {
        return new SangerTrimFilesCreator(tuple, projectRoot, appendToExistingFiles, true);
    }

    public static final class SangerTrimFilesCreator implements Closeable {

        PrintWriter trimmedFasta;

        PrintWriter unTrimmedFasta;

        PrintWriter trimPoints;

        PrintWriter qualityTrimPoints;

        PrintWriter vectorTrimPoints;

        PrintWriter untrimmedQualityFasta;

        PrintWriter untrimmedPeaksFasta;

        File chromatDir;

        private SangerTrimFilesCreator(ElviraTuple tuple, File projectRoot, boolean append, boolean editedTraces) throws IOException {
            File sampleRoot = NextGenUtil.getSampleRootDirFor(tuple, projectRoot);
            ReadWriteDirectoryFileServer sangerOutputDir = DirectoryFileServer.createReadWriteDirectoryFileServer(new File(sampleRoot, "sanger"));
            ReadWriteDirectoryFileServer chromatOutputDir = DirectoryFileServer.createReadWriteDirectoryFileServer(new File(sampleRoot, "mapping/chromat_dir"));
            chromatDir = chromatOutputDir.getRootDir();
            final String trimmedFastaFilename = String.format("%s_%s_%s_final.fasta", tuple.getProject(), tuple.getCollectionCode(), tuple.getSampleId());
            trimmedFasta = new PrintWriter(new FileOutputStream(new File(sangerOutputDir.getRootDir(), trimmedFastaFilename), append));
            final String untrimmedFastaFilename = String.format("%s_%s_%s_final.fasta.untrimmed", tuple.getProject(), tuple.getCollectionCode(), tuple.getSampleId());
            unTrimmedFasta = new PrintWriter(new FileOutputStream(new File(sangerOutputDir.getRootDir(), untrimmedFastaFilename), append));
            final String trimpointsFilename = String.format("%s_%s_%s_final.fasta.trimpoints", tuple.getProject(), tuple.getCollectionCode(), tuple.getSampleId());
            trimPoints = new PrintWriter(new FileOutputStream(new File(sangerOutputDir.getRootDir(), trimpointsFilename), append));
            if (editedTraces) {
                final String untrimmedQualFilename = String.format("%s_%s_%s_final.qual.untrimmed", tuple.getProject(), tuple.getCollectionCode(), tuple.getSampleId());
                untrimmedQualityFasta = new PrintWriter(new FileOutputStream(new File(sangerOutputDir.getRootDir(), untrimmedQualFilename), append));
                final String untrimmedPeaksFilename = String.format("%s_%s_%s_final.pos.untrimmed", tuple.getProject(), tuple.getCollectionCode(), tuple.getSampleId());
                untrimmedPeaksFasta = new PrintWriter(new FileOutputStream(new File(sangerOutputDir.getRootDir(), untrimmedPeaksFilename), append));
                final String qualTrimpointsFilename = String.format("%s_%s_%s_final.fasta.clb.trimpoints", tuple.getProject(), tuple.getCollectionCode(), tuple.getSampleId());
                qualityTrimPoints = new PrintWriter(new FileOutputStream(new File(sangerOutputDir.getRootDir(), qualTrimpointsFilename), append));
                final String vectorTrimpointsFilename = String.format("%s_%s_%s_final.fasta.clv.trimpoints", tuple.getProject(), tuple.getCollectionCode(), tuple.getSampleId());
                vectorTrimPoints = new PrintWriter(new FileOutputStream(new File(sangerOutputDir.getRootDir(), vectorTrimpointsFilename), append));
            }
        }

        public SangerTrimFilesCreator addTrimmedRead(String readId, NucleotideSequence fullLengthBasecalls, Range clearRange) {
            return addTrimmedRead(readId, fullLengthBasecalls, null, null, clearRange);
        }

        public SangerTrimFilesCreator addTrimmedRead(String readId, NucleotideSequence fullLengthBasecalls, Range qualityClearRange, Range vectorClearRange, Range clearRange) {
            unTrimmedFasta.print(new DefaultNucleotideSequenceFastaRecord(readId, fullLengthBasecalls));
            trimmedFasta.print(new DefaultNucleotideSequenceFastaRecord(readId, Nucleotides.asString(fullLengthBasecalls.asList(clearRange))));
            writeTrimPoints(trimPoints, readId, clearRange);
            if (qualityClearRange != null) {
                writeTrimPoints(qualityTrimPoints, readId, qualityClearRange);
            }
            if (vectorClearRange != null) {
                writeTrimPoints(vectorTrimPoints, readId, vectorClearRange);
            }
            return this;
        }

        private void writeTrimPoints(PrintWriter writer, String id, Range range) {
            writer.printf("%s\t%d\t%d%n", id, range.getBegin(CoordinateSystem.RESIDUE_BASED), range.getEnd(CoordinateSystem.RESIDUE_BASED));
        }

        public SangerTrimFilesCreator addEditedTrimmedRead(String readId, NucleotideSequence fullLengthBasecalls, QualitySequence fullLengthQualities, Sequence<ShortSymbol> fullLengthPositions, Range clearRange) {
            return addEditedTrimmedRead(readId, fullLengthBasecalls, fullLengthQualities, fullLengthPositions, null, null, clearRange);
        }

        public SangerTrimFilesCreator addEditedTrimmedRead(String readId, NucleotideSequence fullLengthBasecalls, QualitySequence fullLengthQualities, Sequence<ShortSymbol> fullLengthPositions, Range qualityClearRange, Range vectorClearRange, Range clearRange) {
            addTrimmedRead(readId, fullLengthBasecalls, qualityClearRange, vectorClearRange, clearRange);
            untrimmedQualityFasta.print(new DefaultQualityFastaRecord(readId, fullLengthQualities));
            untrimmedPeaksFasta.print(new DefaultPositionSequenceFastaRecord<Sequence<ShortSymbol>>(readId, fullLengthPositions));
            return this;
        }

        public SangerTrimFilesCreator addEditedTrimmedRead(String readId, NucleotideSequence fullLengthBasecalls, QualitySequence fullLengthQualities, Sequence<ShortSymbol> fullLengthPositions, Range qualityClearRange, Range vectorClearRange, Range clearRange, InputStream scfStream) throws IOException {
            addTrimmedRead(readId, fullLengthBasecalls, qualityClearRange, vectorClearRange, clearRange, scfStream);
            untrimmedQualityFasta.print(new DefaultQualityFastaRecord(readId, fullLengthQualities));
            untrimmedPeaksFasta.print(new DefaultPositionSequenceFastaRecord<Sequence<ShortSymbol>>(readId, fullLengthPositions));
            return this;
        }

        public SangerTrimFilesCreator addEditedTrimmedRead(String readId, NucleotideSequence fullLengthBasecalls, QualitySequence fullLengthQualities, Sequence<ShortSymbol> fullLengthPositions, Range clearRange, InputStream scfStream) throws IOException {
            addTrimmedRead(readId, fullLengthBasecalls, clearRange, scfStream);
            untrimmedQualityFasta.print(new DefaultQualityFastaRecord(readId, fullLengthQualities));
            untrimmedPeaksFasta.print(new DefaultPositionSequenceFastaRecord<Sequence<ShortSymbol>>(readId, fullLengthPositions));
            return this;
        }

        public SangerTrimFilesCreator addTrimmedRead(String readId, NucleotideSequence fullLengthBasecalls, Range clearRange, InputStream scfStream) throws IOException {
            return addTrimmedRead(readId, fullLengthBasecalls, null, null, clearRange, scfStream);
        }

        public SangerTrimFilesCreator addTrimmedRead(String readId, NucleotideSequence fullLengthBasecalls, Range qualityClearRange, Range vectorClearRange, Range clearRange, InputStream scfStream) throws IOException {
            addTrimmedRead(readId, fullLengthBasecalls, qualityClearRange, vectorClearRange, clearRange);
            addChromatogramOnly(readId, scfStream);
            return this;
        }

        public SangerTrimFilesCreator addChromatogramOnly(String readId, InputStream scfChromatogramStream) throws IOException {
            OutputStream out = new FileOutputStream(new File(chromatDir, readId + ".scf"));
            IOUtil.copy(scfChromatogramStream, out);
            IOUtil.closeAndIgnoreErrors(scfChromatogramStream, out);
            return this;
        }

        @Override
        public void close() throws IOException {
            IOUtil.closeAndIgnoreErrors(trimPoints, unTrimmedFasta, trimmedFasta, untrimmedPeaksFasta, untrimmedQualityFasta, qualityTrimPoints, vectorTrimPoints);
        }
    }
}
