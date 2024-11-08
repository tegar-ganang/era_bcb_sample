package org.jcvi.trace.sanger.chromatogram;

import java.util.Map;
import org.jcvi.glyph.nuc.NucleotideEncodedGlyphs;
import org.jcvi.glyph.phredQuality.QualityEncodedGlyphs;
import org.jcvi.sequence.Peaks;

/**
 * {@code BasicChromatogramFile} is a Chromatogram implementation
 * that is also a {@link ChromatogramFileVisitor}.  This chromatogram
 * object gets built by listening to the visit messages
 * it receives from a {@link Chromatogram} parser.
 * @author dkatzel
 *
 *
 */
public class BasicChromatogramFile implements Chromatogram, ChromatogramFileVisitor {

    private Chromatogram delegate;

    private BasicChromatogramBuilder builder;

    public BasicChromatogramFile() {
        builder = new BasicChromatogramBuilder();
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
