package org.jcvi.common.core.seq.read.trace.sanger.chromat;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.BasicChromatogram;
import org.jcvi.common.core.seq.read.trace.sanger.chromat.ChannelGroup;
import org.jcvi.common.core.symbol.pos.SangerPeak;
import org.jcvi.common.core.symbol.qual.QualitySequence;
import org.jcvi.common.core.symbol.residue.nt.NucleotideSequence;
import org.jcvi.common.core.testUtil.TestUtil;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

public class TestBasicChromatogram {

    ChannelGroup mockChannelGroup = createMock(ChannelGroup.class);

    SangerPeak mockPeaks = createMock(SangerPeak.class);

    NucleotideSequence basecalls = createMock(NucleotideSequence.class);

    QualitySequence qualities = createMock(QualitySequence.class);

    Map<String, String> expectedProperties;

    private static final String PROP_1_KEY = "a key";

    private static final String PROP_2_KEY = "a different key";

    BasicChromatogram sut;

    String id = "id";

    @Before
    public void setup() {
        expectedProperties = new HashMap<String, String>();
        expectedProperties.put(PROP_1_KEY, "a value");
        expectedProperties.put(PROP_2_KEY, "a different value");
        sut = new BasicChromatogram(id, basecalls, qualities, mockPeaks, mockChannelGroup, expectedProperties);
    }

    @Test
    public void fullConstructor() {
        assertEquals(basecalls, sut.getNucleotideSequence());
        assertEquals(mockPeaks, sut.getPeaks());
        assertEquals(mockChannelGroup, sut.getChannelGroup());
        assertEquals(expectedProperties, sut.getComments());
        assertEquals(qualities, sut.getQualities());
    }

    @Test
    public void constructionWithEmptyProperties() {
        BasicChromatogram emptyProps = new BasicChromatogram(id, basecalls, qualities, mockPeaks, mockChannelGroup);
        assertEquals(basecalls, emptyProps.getNucleotideSequence());
        assertEquals(mockPeaks, emptyProps.getPeaks());
        assertEquals(mockChannelGroup, emptyProps.getChannelGroup());
        assertEquals(qualities, sut.getQualities());
        assertEquals(new Properties(), emptyProps.getComments());
    }

    @Test
    public void nullBaseCallsShouldThrowIllegalArugmentException() {
        try {
            new BasicChromatogram(id, (NucleotideSequence) null, qualities, mockPeaks, mockChannelGroup, expectedProperties);
            fail("should throw illegalArgumentException when a parameter is null");
        } catch (IllegalArgumentException e) {
            assertEquals("null parameter", e.getMessage());
        }
    }

    @Test
    public void nullPeaksShouldThrowIllegalArugmentException() {
        try {
            new BasicChromatogram(id, basecalls, qualities, null, mockChannelGroup, expectedProperties);
            fail("should throw illegalArgumentException when a parameter is null");
        } catch (IllegalArgumentException e) {
            assertEquals("null parameter", e.getMessage());
        }
    }

    @Test
    public void nullChannelGroupShouldThrowIllegalArugmentException() {
        try {
            new BasicChromatogram(id, basecalls, qualities, mockPeaks, null, expectedProperties);
            fail("should throw illegalArgumentException when a parameter is null");
        } catch (IllegalArgumentException e) {
            assertEquals("null parameter", e.getMessage());
        }
    }

    @Test
    public void nullPropertiesShouldThrowIllegalArugmentException() {
        try {
            new BasicChromatogram(id, basecalls, qualities, mockPeaks, mockChannelGroup, null);
            fail("should throw illegalArgumentException when a parameter is null");
        } catch (IllegalArgumentException e) {
            assertEquals("null parameter", e.getMessage());
        }
    }

    @Test
    public void copyConstructor() {
        BasicChromatogram copy = new BasicChromatogram(sut);
        assertEquals(basecalls, copy.getNucleotideSequence());
        assertEquals(mockPeaks, copy.getPeaks());
        assertEquals(mockChannelGroup, copy.getChannelGroup());
        assertEquals(expectedProperties, copy.getComments());
    }

    @Test
    public void equalsSameRef() {
        TestUtil.assertEqualAndHashcodeSame(sut, sut);
    }

    @Test
    public void notEqualsNull() {
        assertFalse(sut.equals(null));
    }

    @Test
    public void notEqualsDifferentClass() {
        assertFalse(sut.equals("not a chromatogram"));
    }

    @Test
    public void equalsSameValues() {
        BasicChromatogram copy = new BasicChromatogram(sut);
        TestUtil.assertEqualAndHashcodeSame(sut, copy);
    }

    @Test
    public void notEqualsDifferentBasecalls() {
        NucleotideSequence differentBases = createMock(NucleotideSequence.class);
        BasicChromatogram nullBases = new BasicChromatogram(id, differentBases, qualities, mockPeaks, mockChannelGroup, expectedProperties);
        TestUtil.assertNotEqualAndHashcodeDifferent(sut, nullBases);
    }

    @Test
    public void notEqualsDifferentPeaks() {
        SangerPeak differentPeaks = createMock(SangerPeak.class);
        BasicChromatogram nullPeaks = new BasicChromatogram(id, basecalls, qualities, differentPeaks, mockChannelGroup, expectedProperties);
        TestUtil.assertNotEqualAndHashcodeDifferent(sut, nullPeaks);
    }

    @Test
    public void notEqualsDifferentChannelGroup() {
        ChannelGroup differentChannelGroup = createMock(ChannelGroup.class);
        BasicChromatogram differentChannels = new BasicChromatogram(id, basecalls, qualities, mockPeaks, differentChannelGroup, expectedProperties);
        TestUtil.assertNotEqualAndHashcodeDifferent(sut, differentChannels);
    }

    @Test
    public void notEqualsExtraProperties() {
        HashMap<String, String> differentProperties = new HashMap<String, String>(expectedProperties);
        differentProperties.put("extra key", "extra value");
        BasicChromatogram hasDifferentProperties = new BasicChromatogram(id, basecalls, qualities, mockPeaks, mockChannelGroup, differentProperties);
        TestUtil.assertNotEqualAndHashcodeDifferent(sut, hasDifferentProperties);
    }

    @Test
    public void notEqualsMissingProperties() {
        HashMap<String, String> differentProperties = new HashMap<String, String>(expectedProperties);
        differentProperties.remove(PROP_1_KEY);
        BasicChromatogram hasDifferentProperties = new BasicChromatogram(id, basecalls, qualities, mockPeaks, mockChannelGroup, differentProperties);
        TestUtil.assertNotEqualAndHashcodeDifferent(sut, hasDifferentProperties);
    }
}
