package be.lassi.cues;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import be.lassi.base.Dirty;
import be.lassi.base.DirtyIndicator;
import be.lassi.domain.Level;
import be.lassi.domain.LevelValue;
import be.lassi.domain.Setup;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;
import be.lassi.domain.Submasters;

/**
 * Tests class <code>LightCueCalculator</code>.
 */
public class LightCueCalculatorTestCase {

    private static final int CHANNEL_COUNT = 6;

    private static final int SUBMASTER_COUNT = 3;

    private Submasters submasters;

    private NewLightCues cues;

    private final Dirty dirty = new DirtyIndicator();

    @BeforeMethod
    public void init() {
        Show show = ShowBuilder.build(dirty, CHANNEL_COUNT, SUBMASTER_COUNT, 0, "");
        submasters = show.getSubmasters();
        cues = new NewLightCues();
    }

    @Test
    public void defaultLevelValues() {
        addCue();
        addCue();
        String cueLevels = " s0@0* s1@0* s2@0* 0@0..-* 1@0..-* 2@0..-* 3@0..-* 4@0..-* 5@0..-*";
        String channelLevels = " 0@0 1@0 2@0 3@0 4@0 5@0";
        assertCue(0, cueLevels);
        assertCue(1, cueLevels);
        assertChannelLevels(0, channelLevels);
        assertChannelLevels(1, channelLevels);
        LightCueCalculator calculator = new LightCueCalculator(submasters, cues, CHANNEL_COUNT);
        calculator.calculate();
        new LevelPrinter().print(submasters, cues);
        assertCue(0, cueLevels);
        assertCue(1, cueLevels);
        assertChannelLevels(0, channelLevels);
        assertChannelLevels(1, channelLevels);
    }

    @Test
    public void activeValues() {
        addCue();
        addCue();
        Setup.cue(cues.get(0), "0@10");
        Setup.cue(cues.get(1), "0@50");
        LightCueCalculator calculator = new LightCueCalculator(submasters, cues, CHANNEL_COUNT);
        calculator.calculate();
        new LevelPrinter().print(submasters, cues);
        assertCue(0, " s0@0* s1@0* s2@0* 0@10..- 1@0..-* 2@0..-* 3@0..-* 4@0..-* 5@0..-*");
        assertCue(1, " s0@0* s1@0* s2@0* 0@50..- 1@0..-* 2@0..-* 3@0..-* 4@0..-* 5@0..-*");
        assertChannelLevels(0, " 0@10 1@0 2@0 3@0 4@0 5@0");
        assertChannelLevels(1, " 0@50 1@0 2@0 3@0 4@0 5@0");
    }

    @Test
    public void deriveActiveValue() {
        addCue();
        addCue();
        Setup.cue(cues.get(0), "0@20");
        LightCueCalculator calculator = new LightCueCalculator(submasters, cues, CHANNEL_COUNT);
        calculator.calculate();
        new LevelPrinter().print(submasters, cues);
        assertCue(0, " s0@0* s1@0* s2@0* 0@20..- 1@0..-* 2@0..-* 3@0..-* 4@0..-* 5@0..-*");
        assertCue(1, " s0@0* s1@0* s2@0* 0@20..-* 1@0..-* 2@0..-* 3@0..-* 4@0..-* 5@0..-*");
        assertChannelLevels(0, " 0@20 1@0 2@0 3@0 4@0 5@0");
        assertChannelLevels(1, " 0@20 1@0 2@0 3@0 4@0 5@0");
    }

    @Test
    public void deriveNonActiveValue() {
        addCue();
        addCue();
        Setup.cue(cues.get(0), "0@-");
        LightCueCalculator calculator = new LightCueCalculator(submasters, cues, CHANNEL_COUNT);
        calculator.calculate();
        new LevelPrinter().print(submasters, cues);
        assertCue(0, " s0@0* s1@0* s2@0* 0@-..- 1@0..-* 2@0..-* 3@0..-* 4@0..-* 5@0..-*");
        assertCue(1, " s0@0* s1@0* s2@0* 0@-..-* 1@0..-* 2@0..-* 3@0..-* 4@0..-* 5@0..-*");
        assertChannelLevels(0, " 0@- 1@0 2@0 3@0 4@0 5@0");
        assertChannelLevels(1, " 0@- 1@0 2@0 3@0 4@0 5@0");
    }

    /**
     * Tests the contribution of channel levels within a submaster
     * definition, on the submaster level of individual channels.
     *
     * Makes sure the submaster levels in the channels are also
     * present in subsequent derived channels.
     */
    @Test
    public void calculateChannelSubmasterLevelValues() {
        Setup.submaster(submasters.get(0), "0@0 1@60 2@100");
        addCue();
        addCue();
        addCue();
        Setup.cue(cues.get(0), "s0@0");
        Setup.cue(cues.get(1), "s0@50");
        Setup.cue(cues.get(2), "s0@100");
        new LevelPrinter().print(submasters, cues);
        LightCueCalculator calculator = new LightCueCalculator(submasters, cues, CHANNEL_COUNT);
        calculator.calculate();
        new LevelPrinter().print(submasters, cues);
        assertCue(0, " s0@0 s1@0* s2@0* 0@0..0* 1@0..0* 2@0..0* 3@0..-* 4@0..-* 5@0..-*");
        assertCue(1, " s0@50 s1@0* s2@0* 0@0..0* 1@0..30* 2@0..50* 3@0..-* 4@0..-* 5@0..-*");
        assertCue(2, " s0@100 s1@0* s2@0* 0@0..0* 1@0..60* 2@0..100* 3@0..-* 4@0..-* 5@0..-*");
        assertChannelLevels(0, " 0@0 1@0 2@0 3@0 4@0 5@0");
        assertChannelLevels(1, " 0@0 1@30 2@50 3@0 4@0 5@0");
        assertChannelLevels(2, " 0@0 1@60 2@100 3@0 4@0 5@0");
    }

    @Test
    public void calculateDerivedSubmasterLevelValues() {
        Setup.submaster(submasters.get(0), "0@0 1@60 2@100");
        addCue();
        addCue();
        addCue();
        addCue();
        addCue();
        addCue();
        Setup.cue(cues.get(1), "s0@10");
        Setup.cue(cues.get(4), "s0@50");
        LightCueCalculator calculator = new LightCueCalculator(submasters, cues, CHANNEL_COUNT);
        calculator.calculate();
        new LevelPrinter().print(submasters, cues);
        assertCue(0, " s0@0* s1@0* s2@0* 0@0..0* 1@0..0* 2@0..0* 3@0..-* 4@0..-* 5@0..-*");
        assertCue(1, "  s0@10 s1@0* s2@0* 0@0..0* 1@0..6* 2@0..10* 3@0..-* 4@0..-* 5@0..-*");
        assertCue(2, " s0@10* s1@0* s2@0* 0@0..0* 1@0..6* 2@0..10* 3@0..-* 4@0..-* 5@0..-*");
        assertCue(3, " s0@10* s1@0* s2@0* 0@0..0* 1@0..6* 2@0..10* 3@0..-* 4@0..-* 5@0..-*");
        assertCue(4, " s0@50 s1@0* s2@0* 0@0..0* 1@0..30* 2@0..50* 3@0..-* 4@0..-* 5@0..-*");
        assertCue(5, " s0@50* s1@0* s2@0* 0@0..0* 1@0..30* 2@0..50* 3@0..-* 4@0..-* 5@0..-*");
    }

    public void calculateDerivedChannelSubmasterLevelValues() {
    }

    public void calculateDerivedChannelLevelValues() {
    }

    public void performance() {
    }

    private void addCue() {
        Cue cue = new Cue(dirty, "", "", "", "L 2");
        new CueDetailFactory(CHANNEL_COUNT, SUBMASTER_COUNT).update(cue);
        LightCueDetail detail = (LightCueDetail) cue.getDetail();
        cues.add(detail);
    }

    private void updateCueSubmasterLevel(final int cueIndex, final int submasterIndex, final int value, final boolean active) {
        LightCueDetail detail = cues.get(cueIndex);
        CueSubmasterLevel cueSubmasterLevel = detail.getSubmasterLevel(submasterIndex);
        LevelValue levelValue = cueSubmasterLevel.getLevelValue();
        levelValue.setIntValue(value);
        levelValue.setActive(active);
    }

    private void updateCueChannelLevel(final int cueIndex, final int channelIndex, final int value, final boolean active) {
        LightCueDetail detail = cues.get(cueIndex);
        CueChannelLevel cueChannelLevel = detail.getChannelLevel(channelIndex);
        LevelValue levelValue = cueChannelLevel.getChannelLevelValue();
        levelValue.setIntValue(value);
        levelValue.setActive(active);
    }

    private void updateCueChannelSubmasterLevel(final int cueIndex, final int channelIndex, final float value, final boolean active) {
        LightCueDetail detail = cues.get(cueIndex);
        CueChannelLevel cueChannelLevel = detail.getChannelLevel(channelIndex);
        LevelValue levelValue = cueChannelLevel.getSubmasterLevelValue();
        levelValue.setValue(value);
        levelValue.setActive(active);
    }

    private void setupSubmasterChannelLevel(final int submasterIndex, final int channelIndex, final int value, final boolean active) {
        Level level = submasters.get(submasterIndex).getLevel(channelIndex);
        level.setIntValue(value);
        level.setActive(active);
    }

    private void assertCueSubmaster(final int cueIndex, final int submasterIndex, final int expectedValue, final boolean expectedActive, final boolean expectedDerived) {
        LightCueDetail detail = cues.get(cueIndex);
        CueSubmasterLevel cueSubmasterLevel = detail.getSubmasterLevel(submasterIndex);
        assertEquals(cueSubmasterLevel.getIntValue(), expectedValue);
        assertEquals(cueSubmasterLevel.isActive(), expectedActive);
        assertEquals(cueSubmasterLevel.isDerived(), expectedDerived);
    }

    /**
     * Sets all level values to zero and not active in all cues, both for
     * cue submaster levels and cue channel levels.
     */
    private void updateCueLevelsInactive() {
        for (int cueIndex = 0; cueIndex < cues.size(); cueIndex++) {
            for (int submasterIndex = 0; submasterIndex < SUBMASTER_COUNT; submasterIndex++) {
                updateCueSubmasterLevel(cueIndex, submasterIndex, 0, false);
            }
            for (int channelIndex = 0; channelIndex < CHANNEL_COUNT; channelIndex++) {
                updateCueChannelLevel(cueIndex, channelIndex, 0, false);
                updateCueChannelSubmasterLevel(cueIndex, channelIndex, 0f, false);
            }
        }
    }

    private void assertCue(final int cueIndex, final String expectedLevels) {
        String levels = LevelPrinter.toString(cues.get(cueIndex));
        assertEquals(levels, expectedLevels);
    }

    private void assertChannelLevels(final int cueIndex, final String expectedLevels) {
        String levels = LevelPrinter.channelLevels(cues.get(cueIndex));
        assertEquals(levels, expectedLevels);
    }
}
