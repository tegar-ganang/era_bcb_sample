package org.jcvi.trace.sanger.chromatogram.ztr;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import org.jcvi.common.util.Range;
import org.jcvi.glyph.nuc.NucleotideEncodedGlyphs;
import org.jcvi.glyph.phredQuality.QualityEncodedGlyphs;
import org.jcvi.sequence.Peaks;
import org.jcvi.trace.TraceDecoderException;
import org.jcvi.trace.sanger.chromatogram.ChannelGroup;

/**
 * @author dkatzel
 *
 *
 */
public class ZTRChromatogramFile implements ZTRChromatogramFileVisitor, ZTRChromatogram {

    private ZTRChromatogram delegate;

    private ZTRChromatogramBuilder builder;

    public ZTRChromatogramFile() {
        builder = new ZTRChromatogramBuilder();
    }

    public ZTRChromatogramFile(File ztrFile) throws FileNotFoundException, TraceDecoderException {
        this();
        ZTRChromatogramFileParser.parseZTRFile(ztrFile, this);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitFile() {
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitEndOfFile() {
        delegate = builder.build();
        builder = null;
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitBasecalls(String basecalls) {
        builder.basecalls(basecalls);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitPeaks(short[] peaks) {
        builder.peaks(peaks);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitClipRange(Range clipRange) {
        builder.clip(clipRange);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitComments(Map<String, String> comments) {
        builder.properties(comments);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitAPositions(short[] positions) {
        builder.aPositions(positions);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitCPositions(short[] positions) {
        builder.cPositions(positions);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitGPositions(short[] positions) {
        builder.gPositions(positions);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitTPositions(short[] positions) {
        builder.tPositions(positions);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public ChannelGroup getChannelGroup() {
        return delegate.getChannelGroup();
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public Map<String, String> getProperties() {
        return delegate.getProperties();
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public Peaks getPeaks() {
        return delegate.getPeaks();
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public int getNumberOfTracePositions() {
        return delegate.getNumberOfTracePositions();
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public NucleotideEncodedGlyphs getBasecalls() {
        return delegate.getBasecalls();
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public QualityEncodedGlyphs getQualities() {
        return delegate.getQualities();
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public Range getClip() {
        return delegate.getClip();
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitAConfidence(byte[] confidence) {
        builder.aConfidence(confidence);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitCConfidence(byte[] confidence) {
        builder.cConfidence(confidence);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitGConfidence(byte[] confidence) {
        builder.gConfidence(confidence);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitTConfidence(byte[] confidence) {
        builder.tConfidence(confidence);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitNewTrace() {
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitEndOfTrace() {
    }
}
