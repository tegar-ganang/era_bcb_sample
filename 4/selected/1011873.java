package org.jcvi.trace.sanger.chromatogram.scf.section;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.jcvi.glyph.EncodedGlyphs;
import org.jcvi.glyph.nuc.NucleotideGlyph;
import org.jcvi.glyph.num.ShortGlyph;
import org.jcvi.io.IOUtil;
import org.jcvi.sequence.Confidence;
import org.jcvi.trace.sanger.chromatogram.ChannelGroup;
import org.jcvi.trace.sanger.chromatogram.ChromatogramFileVisitor;
import org.jcvi.trace.sanger.chromatogram.scf.SCFChromatogram;
import org.jcvi.trace.sanger.chromatogram.scf.SCFChromatogramBuilder;
import org.jcvi.trace.sanger.chromatogram.scf.SCFChromatogramFileVisitor;

public class Version3BasesSectionCodec extends AbstractBasesSectionCodec {

    @Override
    protected void readBasesData(DataInputStream in, SCFChromatogramBuilder c, int numberOfBases) throws IOException {
        c.peaks(parsePeaks(in, numberOfBases));
        setConfidences(c, parseConfidenceData(in, numberOfBases));
        c.basecalls(parseBasecalls(in, numberOfBases));
        c.substitutionConfidence(parseSpareConfidence(in, numberOfBases).array()).insertionConfidence(parseSpareConfidence(in, numberOfBases).array()).deletionConfidence(parseSpareConfidence(in, numberOfBases).array());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBasesData(DataInputStream in, ChromatogramFileVisitor visitor, int numberOfBases) throws IOException {
        visitor.visitPeaks(parsePeaks(in, numberOfBases));
        byte[][] confidences = parseConfidenceData(in, numberOfBases);
        visitor.visitAConfidence(confidences[0]);
        visitor.visitCConfidence(confidences[1]);
        visitor.visitGConfidence(confidences[2]);
        visitor.visitTConfidence(confidences[3]);
        visitor.visitBasecalls(parseBasecalls(in, numberOfBases));
        if (visitor instanceof SCFChromatogramFileVisitor) {
            SCFChromatogramFileVisitor scfVisitor = (SCFChromatogramFileVisitor) visitor;
            scfVisitor.visitSubstitutionConfidence(parseSpareConfidence(in, numberOfBases).array());
            scfVisitor.visitInsertionConfidence(parseSpareConfidence(in, numberOfBases).array());
            scfVisitor.visitDeletionConfidence(parseSpareConfidence(in, numberOfBases).array());
        }
    }

    private ByteBuffer parseSpareConfidence(DataInputStream in, int numberOfBases) throws IOException {
        byte[] spare = new byte[numberOfBases];
        int bytesRead = IOUtil.blockingRead(in, spare, 0, numberOfBases);
        if (bytesRead != numberOfBases) {
            throw new IOException("could not read all the spare confidences");
        }
        return ByteBuffer.wrap(spare);
    }

    private short[] parsePeaks(DataInputStream in, int numberOfBases) throws IOException {
        short[] peaks = new short[numberOfBases];
        for (int i = 0; i < numberOfBases; i++) {
            peaks[i] = (short) in.readInt();
        }
        return peaks;
    }

    private byte[][] parseConfidenceData(DataInputStream in, int numberOfBases) throws IOException {
        byte[][] probability = new byte[4][numberOfBases];
        for (int i = 0; i < 4; i++) {
            int bytesRead = IOUtil.blockingRead(in, probability[i], 0, numberOfBases);
            if (bytesRead != numberOfBases) {
                throw new IOException("could not read all the confidences");
            }
        }
        return probability;
    }

    private String parseBasecalls(DataInputStream in, int numberOfBases) throws IOException {
        byte[] bases = new byte[numberOfBases];
        int bytesRead = IOUtil.blockingRead(in, bases, 0, numberOfBases);
        if (bytesRead != numberOfBases) {
            throw new IOException(String.format("could not read all the bases %d out of %d", bytesRead, numberOfBases));
        }
        for (int i = 0; i < numberOfBases; i++) {
            if (bases[i] == 0) {
                bases[i] = (byte) 'N';
            }
        }
        return new String(bases);
    }

    @Override
    protected void writeBasesDataToBuffer(ByteBuffer buffer, SCFChromatogram c, int numberOfBases) {
        final ChannelGroup channelGroup = c.getChannelGroup();
        final ByteBuffer aConfidence = ByteBuffer.wrap(channelGroup.getAChannel().getConfidence().getData());
        final ByteBuffer cConfidence = ByteBuffer.wrap(channelGroup.getCChannel().getConfidence().getData());
        final ByteBuffer gConfidence = ByteBuffer.wrap(channelGroup.getGChannel().getConfidence().getData());
        final ByteBuffer tConfidence = ByteBuffer.wrap(channelGroup.getTChannel().getConfidence().getData());
        bulkPutPeaks(buffer, c.getPeaks().getData());
        bulkPut(buffer, aConfidence, numberOfBases);
        bulkPut(buffer, cConfidence, numberOfBases);
        bulkPut(buffer, gConfidence, numberOfBases);
        bulkPut(buffer, tConfidence, numberOfBases);
        bulkPut(buffer, c.getBasecalls());
        bulkPutWithPadding(buffer, c.getSubstitutionConfidence(), numberOfBases);
        bulkPutWithPadding(buffer, c.getInsertionConfidence(), numberOfBases);
        bulkPutWithPadding(buffer, c.getDeletionConfidence(), numberOfBases);
    }

    private void bulkPut(ByteBuffer buffer, EncodedGlyphs<NucleotideGlyph> basecalls) {
        for (NucleotideGlyph glyph : basecalls.decode()) {
            buffer.put((byte) glyph.getCharacter().charValue());
        }
    }

    private void bulkPutPeaks(ByteBuffer buffer, EncodedGlyphs<ShortGlyph> peaks) {
        for (ShortGlyph glyph : peaks.decode()) {
            buffer.putInt(glyph.getNumber().intValue());
        }
    }

    private void bulkPutWithPadding(ByteBuffer buffer, Confidence optionalConfidence, int numberOfBases) {
        if (optionalConfidence != null && optionalConfidence.getData() != null && optionalConfidence.getData().length > 0) {
            bulkPut(buffer, ByteBuffer.wrap(optionalConfidence.getData()), numberOfBases);
        } else {
            for (int i = 0; i < numberOfBases; i++) {
                buffer.put((byte) 0);
            }
        }
    }

    private void bulkPut(ByteBuffer buffer, ByteBuffer data, int expectedSize) {
        buffer.put(data);
        for (int i = data.position(); i < expectedSize; i++) {
            buffer.put((byte) 0);
        }
        data.rewind();
    }
}
