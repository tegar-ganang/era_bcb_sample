package org.jcvi.common.core.seq.read.trace.sanger.chromat;

import java.util.HashMap;
import java.util.Map;
import org.jcvi.common.core.symbol.RunLengthEncodedGlyphCodec;
import org.jcvi.common.core.symbol.pos.SangerPeak;
import org.jcvi.common.core.symbol.qual.EncodedQualitySequence;
import org.jcvi.common.core.symbol.qual.PhredQuality;
import org.jcvi.common.core.symbol.qual.QualitySequence;
import org.jcvi.common.core.symbol.residue.nt.NucleotideSequence;
import org.jcvi.common.core.symbol.residue.nt.NucleotideSequenceBuilder;
import org.jcvi.common.core.util.CommonUtil;

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

    private final NucleotideSequence basecalls;

    private final SangerPeak peaks;

    private final QualitySequence qualities;

    private final String id;

    /**
     * Used to store the TEXT properties of a ZTR file.
     */
    private Map<String, String> properties;

    public BasicChromatogram(Chromatogram c) {
        this(c.getId(), c.getNucleotideSequence(), c.getQualities(), c.getPeaks(), c.getChannelGroup(), c.getComments());
    }

    public BasicChromatogram(String id, NucleotideSequence basecalls, QualitySequence qualities, SangerPeak peaks, ChannelGroup channelGroup) {
        this(id, basecalls, qualities, peaks, channelGroup, new HashMap<String, String>());
    }

    public BasicChromatogram(String id, String basecalls, byte[] qualities, SangerPeak peaks, ChannelGroup channelGroup, Map<String, String> comments) {
        this(id, new NucleotideSequenceBuilder(basecalls).build(), new EncodedQualitySequence(RUN_LENGTH_CODEC, PhredQuality.valueOf(qualities)), peaks, channelGroup, comments);
    }

    public BasicChromatogram(String id, NucleotideSequence basecalls, QualitySequence qualities, SangerPeak peaks, ChannelGroup channelGroup, Map<String, String> comments) {
        canNotBeNull(id, basecalls, peaks, channelGroup, comments);
        this.id = id;
        this.peaks = peaks;
        this.properties = comments;
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

    @Override
    public String getId() {
        return id;
    }

    public NucleotideSequence getNucleotideSequence() {
        return basecalls;
    }

    @Override
    public SangerPeak getPeaks() {
        return peaks;
    }

    public Map<String, String> getComments() {
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
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Chromatogram)) {
            return false;
        }
        final Chromatogram other = (Chromatogram) obj;
        return CommonUtil.similarTo(getNucleotideSequence(), other.getNucleotideSequence()) && CommonUtil.similarTo(getPeaks(), other.getPeaks()) && CommonUtil.similarTo(getChannelGroup(), other.getChannelGroup()) && CommonUtil.similarTo(getComments(), other.getComments());
    }

    /**
     * @return the channelGroup
     */
    public ChannelGroup getChannelGroup() {
        return channelGroup;
    }

    @Override
    public QualitySequence getQualities() {
        return qualities;
    }

    @Override
    public int getNumberOfTracePositions() {
        return getChannelGroup().getAChannel().getPositions().array().length;
    }
}
