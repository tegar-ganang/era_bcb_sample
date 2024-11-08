package be.lassi.cues;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;
import be.lassi.domain.LevelValue;

/**
 * Tests class <code>CueChannelLevel</code>.
 */
public class CueChannelLevelTestCase {

    @Test
    public void constructor() {
        CueChannelLevel level = new CueChannelLevel();
        assertEquals(0, level.getChannelIntValue());
        assertEquals(0, level.getSubmasterIntValue());
        assertTrue(level.isDerived());
        assertTrue(level.isActive());
    }

    @Test
    public void equals() {
        CueChannelLevel level1 = new CueChannelLevel();
        CueChannelLevel level2 = new CueChannelLevel();
        assertEquals(level1, level2);
        level1.setChannelValue(0.5f);
        level1.setSubmasterValue(0.6f);
        level2.setChannelValue(0.5f);
        level2.setSubmasterValue(0.6f);
        assertEquals(level1, level2);
        level1.setDerived(false);
        level2.setDerived(false);
        assertEquals(level1, level2);
        level1.setChannelValue(0.1f);
        assertFalse(level1.equals(level2));
        level1.setChannelValue(0.5f);
        level1.setSubmasterValue(0.1f);
        assertFalse(level1.equals(level2));
        level1.setSubmasterValue(0.6f);
        level1.setDerived(true);
        assertFalse(level1.equals(level2));
    }

    @Test
    public void string() {
        assertString("0", 0, 0, true, false);
        assertString("0", 0, 0, true, true);
        assertString("10", 0.1f, 0, true, false);
        assertString("10..20", 0.1f, 0.2f, true, true);
        assertString("-", 0, 0, false, false);
        assertString("-..20", 0, 0.2f, false, true);
        assertString("10", 0.1f, 0.2f, true, false);
        assertString("-..0", 0, 0, false, true);
    }

    @Test
    public void getValue() {
        CueChannelLevel level = new CueChannelLevel();
        assertEquals(level.getValue(), 0f, 0.05);
        level.setChannelValue(0.5f);
        assertEquals(level.getValue(), 0.5f, 0.05);
        level.setSubmasterValue(0.6f);
        assertEquals(level.getValue(), 0.6f, 0.05);
        level.setChannelValue(0.7f);
        assertEquals(level.getValue(), 0.7f, 0.05);
    }

    private void assertString(final String expected, final float channelValue, final float submasterValue, final boolean channelActive, final boolean submasterActive) {
        CueChannelLevel level = new CueChannelLevel();
        LevelValue clv = level.getChannelLevelValue();
        clv.setValue(channelValue);
        clv.setActive(channelActive);
        LevelValue slv = level.getSubmasterLevelValue();
        slv.setValue(submasterValue);
        slv.setActive(submasterActive);
        assertEquals(expected, level.string());
    }
}
