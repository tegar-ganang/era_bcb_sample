package be.lassi.cues;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;
import be.lassi.lanbox.domain.ChannelChange;
import be.lassi.lanbox.domain.Time;

/**
 * Tests class <code>Cue</code>.
 */
public class LightCueDetailTestCase {

    private final Timing t = new Timing();

    @Test
    public void testActiveChangesOnly() {
        LightCueDetail cue = new LightCueDetail(t, 3, 3);
        cue.getChannelLevel(0).setChannelValue(1f);
        cue.getChannelLevel(1).setChannelValue(1f);
        cue.getChannelLevel(2).setChannelValue(1f);
        cue.getChannelLevel(1).getChannelLevelValue().setActive(false);
        cue.getChannelLevel(1).getSubmasterLevelValue().setActive(false);
        ChannelChange[] changes = cue.getChannelChanges().toArray();
        assertEquals(changes.length, 2);
        assertEquals(changes[0].getChannelId(), 1);
        assertEquals(changes[1].getChannelId(), 3);
    }

    @Test
    public void equals() {
        LightCueDetail cue1 = new LightCueDetail(t, 10, 20);
        LightCueDetail cue2 = new LightCueDetail(t, 10, 20);
        assertEquals(cue1, cue2);
        cue1.getChannelLevel(0).setDerived(false);
        assertFalse(cue1.equals(cue2));
        cue2.getChannelLevel(0).setDerived(false);
        assertTrue(cue1.equals(cue2));
        cue1.getSubmasterLevel(0).setDerived(false);
        assertFalse(cue1.equals(cue2));
        cue2.getSubmasterLevel(0).setDerived(false);
        assertTrue(cue1.equals(cue2));
        cue1.getTiming().setFadeInTime(Time.TIME_1S);
        assertFalse(cue1.equals(cue2));
        cue2.getTiming().setFadeInTime(Time.TIME_1S);
        assertTrue(cue1.equals(cue2));
    }

    /**
     * Test copy of 'persistent' attributes.
     */
    @Test
    public void copy() {
        LightCueDetail detail1 = new LightCueDetail(t, 2, 4);
        detail1.getChannelLevel(0).setChannelValue(0.4f);
        detail1.getChannelLevel(0).setSubmasterValue(0.5f);
        detail1.getSubmasterLevel(0).getLevelValue().setValue(0.6f);
        LightCueDetail detail2 = detail1.copy();
        assertEquals(detail1.getTiming(), detail2.getTiming());
        assertEquals(detail2.getChannelLevel(0).getChannelLevelValue().getValue(), 0.4f, 0.05f);
        assertEquals(detail2.getChannelLevel(0).getSubmasterValue(), 0.5f, 0.05f);
        assertEquals(detail2.getSubmasterLevel(0).getValue(), 0.6f, 0.05f);
        detail1.getTiming().setFadeInDelay(Time.TIME_2S);
        assertEquals(detail2.getTiming().getFadeInDelay(), Time.TIME_0S);
        detail1.getChannelLevel(0).setChannelValue(0.2f);
        assertEquals(detail2.getChannelLevel(0).getChannelLevelValue().getValue(), 0.4f, 0.05f);
        detail1.getChannelLevel(0).setSubmasterValue(0.3f);
        assertEquals(detail2.getChannelLevel(0).getSubmasterValue(), 0.5f, 0.05f);
        detail1.getSubmasterLevel(0).getLevelValue().setValue(0.4f);
        assertEquals(detail2.getSubmasterLevel(0).getValue(), 0.6f, 0.05f);
    }
}
