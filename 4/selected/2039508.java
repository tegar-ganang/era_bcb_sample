package org.jcvi.trace.sanger.chromatogram;

import java.util.HashMap;
import java.util.Map;
import org.jcvi.CommonUtil;
import org.jcvi.glyph.encoder.RunLengthEncodedGlyphCodec;
import org.jcvi.glyph.nuc.DefaultNucleotideEncodedGlyphs;
import org.jcvi.glyph.nuc.NucleotideEncodedGlyphs;
import org.jcvi.glyph.nuc.NucleotideGlyph;
import org.jcvi.glyph.phredQuality.DefaultQualityEncodedGlyphs;
import org.jcvi.glyph.phredQuality.PhredQuality;
import org.jcvi.glyph.phredQuality.QualityEncodedGlyphs;
import org.jcvi.sequence.Peaks;

/**
 * <code>Chromatgoram</code> is an implementation
 * of {@link Trace} which is used to reference Sanger
 * Chromatograms.
 * @author dkatzel
 *
 *
 */
public class BasicChromatogram implements Chromatogram {

    private static final RunLengthEncodedGlyphCodec RUN_LENGTH_CODEC = RunLengthEncodedGlyphCodec.DEFAULT_INSTANCE;

    private final ChannelGroup channelGroup;

    private final NucleotideEncodedGlyphs basecalls;

    private final Peaks peaks;

    private final QualityEncodedGlyphs qualities;

    /**
     * Used to store the TEXT properties of a ZTR file.
     */
    private Map<String, String> properties;

    public BasicChromatogram(Chromatogram c) {
        this(c.getBasecalls(), c.getQualities(), c.getPeaks(), c.getChannelGroup(), c.getProperties());
    }

    public BasicChromatogram(NucleotideEncodedGlyphs basecalls, QualityEncodedGlyphs qualities, Peaks peaks, ChannelGroup channelGroup) {
        this(basecalls, qualities, peaks, channelGroup, new HashMap<String, String>());
    }

    public BasicChromatogram(String basecalls, byte[] qualities, Peaks peaks, ChannelGroup channelGroup, Map<String, String> properties) {
        this(new DefaultNucleotideEncodedGlyphs(NucleotideGlyph.getGlyphsFor(basecalls)), new DefaultQualityEncodedGlyphs(RUN_LENGTH_CODEC, PhredQuality.valueOf(qualities)), peaks, channelGroup, properties);
    }

    public BasicChromatogram(NucleotideEncodedGlyphs basecalls, QualityEncodedGlyphs qualities, Peaks peaks, ChannelGroup channelGroup, Map<String, String> properties) {
        canNotBeNull(basecalls, peaks, channelGroup, properties);
        this.peaks = peaks;
        this.properties = properties;
        this.channelGroup = channelGroup;
        this.basecalls = basecalls;
        this.qualities = qualities;
    }

    private void canNotBeNull(Object... objects) {
        for (Object obj : objects) {
            if (obj == null) {
                throw new IllegalArgumentException("null parameter");
            }
        }
    }

    public NucleotideEncodedGlyphs getBasecalls() {
        return basecalls;
    }

    @Override
    public Peaks getPeaks() {
        return peaks;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getChannelGroup().hashCode();
        result = prime * result + basecalls.hashCode();
        result = prime * result + peaks.hashCode();
        result = prime * result + properties.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Chromatogram)) {
            return false;
        }
        final Chromatogram other = (Chromatogram) obj;
        return CommonUtil.similarTo(getBasecalls(), other.getBasecalls()) && CommonUtil.similarTo(getPeaks(), other.getPeaks()) && CommonUtil.similarTo(getChannelGroup(), other.getChannelGroup()) && CommonUtil.similarTo(getProperties(), other.getProperties());
    }

    /**
     * @return the channelGroup
     */
    public ChannelGroup getChannelGroup() {
        return channelGroup;
    }

    @Override
    public QualityEncodedGlyphs getQualities() {
        return qualities;
    }

    @Override
    public int getNumberOfTracePositions() {
        return getChannelGroup().getAChannel().getPositions().array().length;
    }
}
