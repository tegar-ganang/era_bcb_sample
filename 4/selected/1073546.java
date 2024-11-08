package be.lassi.domain;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;
import be.lassi.base.DirtyStub;
import be.lassi.cues.Cue;
import be.lassi.cues.CueDetailFactory;
import be.lassi.cues.CueSubmasterLevel;
import be.lassi.cues.LightCueDetail;

/**
 * Tests class <code>Setup</code>
 */
public class SetupTestCase {

    @Test
    public void testSetupSubmaster() {
        Submaster submaster = new Submaster(3, "");
        assertSubmasterLevel(submaster, 0, 0, false);
        assertSubmasterLevel(submaster, 1, 0, false);
        assertSubmasterLevel(submaster, 2, 0, false);
        Setup.submaster(submaster, "0@0 1@100");
        assertSubmasterLevel(submaster, 0, 0, true);
        assertSubmasterLevel(submaster, 1, 100, true);
        assertSubmasterLevel(submaster, 2, 0, false);
        Setup.submaster(submaster, "0@80");
        assertSubmasterLevel(submaster, 0, 80, true);
        assertSubmasterLevel(submaster, 1, 100, true);
        assertSubmasterLevel(submaster, 2, 0, false);
        Setup.submaster(submaster, "1@-");
        assertSubmasterLevel(submaster, 0, 80, true);
        assertSubmasterLevel(submaster, 1, 0, false);
        assertSubmasterLevel(submaster, 2, 0, false);
    }

    @Test
    public void testSetupCue() {
        Cue cue = new Cue(new DirtyStub(), "", "", "", "L 2");
        new CueDetailFactory(3, 2).update(cue);
        LightCueDetail detail = (LightCueDetail) cue.getDetail();
        Setup.cue(detail, "s0@10 s1@60 0@100 1@80 2@-");
        assertCueSubmasterLevel(detail, 0, 10, true);
        assertCueSubmasterLevel(detail, 1, 60, true);
        assertCueChannelLevel(detail, 0, 100, true);
        assertCueChannelLevel(detail, 1, 80, true);
        assertCueChannelLevel(detail, 2, 0, false);
        Setup.cue(detail, "s0@- 1@-");
        assertCueSubmasterLevel(detail, 0, 0, false);
        assertCueSubmasterLevel(detail, 1, 60, true);
        assertCueChannelLevel(detail, 0, 100, true);
        assertCueChannelLevel(detail, 1, 0, false);
        assertCueChannelLevel(detail, 2, 0, false);
    }

    private void assertSubmasterLevel(final Submaster submaster, final int channelIndex, final int intValue, final boolean active) {
        Level level = submaster.getLevel(channelIndex);
        assertEquals(intValue, level.getIntValue());
        assertEquals(active, level.isActive());
    }

    private void assertCueSubmasterLevel(final LightCueDetail cue, final int submasterIndex, final int intValue, final boolean active) {
        CueSubmasterLevel level = cue.getSubmasterLevel(submasterIndex);
        assertEquals(intValue, level.getIntValue());
        assertEquals(active, level.isActive());
    }

    private void assertCueChannelLevel(final LightCueDetail cue, final int channelIndex, final int intValue, final boolean active) {
        LevelValue levelValue = cue.getChannelLevel(channelIndex).getChannelLevelValue();
        assertEquals(intValue, levelValue.getIntValue());
        assertEquals(active, levelValue.isActive());
    }
}
