package be.lassi.cues;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import be.lassi.domain.Level;
import be.lassi.domain.LevelValue;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;
import be.lassi.domain.Submaster;
import be.lassi.domain.Submasters;

/**
 * Tests class <code>CueList</code>.
 */
public class LightCuesTestCase {

    private static final int NUMBER_OF_SUBMASTERS = 12;

    private static final int NUMBER_OF_CHANNELS = 24;

    private Submasters submasters;

    private LightCues lightCues;

    @BeforeMethod
    public void init() {
        Show show = ShowBuilder.build(NUMBER_OF_CHANNELS, NUMBER_OF_SUBMASTERS, 12, "");
        submasters = show.getSubmasters();
        lightCues = new LightCues(submasters);
        lightCues.removeAll();
    }

    @Test
    public void deactivateChannel() {
        TestingCuesListener listener = new TestingCuesListener();
        lightCues.addListener(listener);
        addCue();
        addCue();
        addCue();
        addCue();
        setSubmasterActive(0, 0, false);
        setSubmasterActive(1, 0, false);
        setSubmasterActive(2, 0, false);
        setSubmasterActive(3, 0, false);
        setChannelDerived(2, 0, false);
        lightCues.deactivateChannel(0, 0);
        assertChannelActive(0, 0, false);
        assertChannelActive(1, 0, false);
        assertChannelActive(2, 0, true);
        assertChannelActive(3, 0, true);
    }

    @Test
    public void deactivateSubmaster() {
        addCue();
        addCue();
        addCue();
        addCue();
        assertSubmasterActive(0, 0, false);
        assertSubmasterActive(0, 1, false);
        assertSubmasterActive(0, 2, false);
        assertSubmasterActive(0, 3, false);
        lightCues.setSubmasterChannel(0, 0, 0f);
        lightCues.setSubmasterChannel(0, 1, 1f);
        assertSubmasterActive(0, 0, true);
        assertSubmasterActive(0, 1, true);
        assertSubmasterActive(0, 2, false);
        assertSubmasterActive(0, 3, false);
        lightCues.setCueSubmaster(0, 0, 1f);
        lightCues.setCueSubmaster(2, 0, 1f);
        assertCueSubmasterDerived(0, 0, false);
        assertCueSubmasterDerived(1, 0, true);
        assertCueSubmasterDerived(2, 0, false);
        assertCueSubmasterDerived(3, 0, true);
        assertCueSubmasterActive(0, 0, true);
        assertCueSubmasterActive(1, 0, true);
        assertCueSubmasterActive(2, 0, true);
        assertCueSubmasterActive(3, 0, true);
        assertCueChannelSubmasterActive(0, 0, true);
        assertCueChannelSubmasterActive(1, 0, true);
        assertCueChannelSubmasterActive(2, 0, true);
        assertCueChannelSubmasterActive(3, 0, true);
        assertCueChannelSubmasterActive(0, 1, true);
        assertCueChannelSubmasterActive(1, 1, true);
        assertCueChannelSubmasterActive(2, 1, true);
        assertCueChannelSubmasterActive(3, 1, true);
        assertCueChannelSubmasterActive(0, 2, false);
        assertCueChannelSubmasterActive(1, 2, false);
        assertCueChannelSubmasterActive(2, 2, false);
        assertCueChannelSubmasterActive(3, 2, false);
        assertCueChannelSubmasterActive(0, 3, false);
        assertCueChannelSubmasterActive(1, 3, false);
        assertCueChannelSubmasterActive(2, 3, false);
        assertCueChannelSubmasterActive(3, 3, false);
        lightCues.deactivateCueSubmaster(0, 0);
        assertCueSubmasterValue(0, 0, 0);
        assertCueSubmasterValue(1, 0, 0);
        assertCueSubmasterValue(2, 0, 1f);
        assertCueSubmasterValue(3, 0, 1f);
        assertCueSubmasterActive(0, 0, false);
        assertCueSubmasterActive(1, 0, false);
        assertCueSubmasterActive(2, 0, true);
        assertCueSubmasterActive(3, 0, true);
        assertCueChannelSubmasterActive(0, 0, false);
        assertCueChannelSubmasterActive(1, 0, false);
        assertCueChannelSubmasterActive(2, 0, true);
        assertCueChannelSubmasterActive(3, 0, true);
        assertCueChannelSubmasterActive(0, 1, false);
        assertCueChannelSubmasterActive(1, 1, false);
        assertCueChannelSubmasterActive(2, 1, true);
        assertCueChannelSubmasterActive(3, 1, true);
    }

    @Test
    public void channelUpdateStopsAtInactiveChannel() {
        addCue();
        addCue();
        addCue();
        addCue();
        setSubmasterActive(0, 0, false);
        setSubmasterActive(1, 0, false);
        setSubmasterActive(2, 0, false);
        setSubmasterActive(3, 0, false);
        lightCues.deactivateChannel(2, 0);
        lightCues.setChannel(0, 0, 1f);
        assertChannelValue(0, 0, 1f);
        assertChannelValue(1, 0, 1f);
        assertChannelValue(2, 0, 0);
        assertChannelValue(3, 0, 0);
    }

    @Test
    public void insert() {
        lightCues.insert(0, newCue("1", "L1"));
        assertEquals(lightCues.get(0).getNumber(), "1");
        assertEquals(lightCues.get(0).getLightCueIndex(), 0);
    }

    @Test
    public void insertUpdateLightCueIndexes() {
        lightCues.insert(0, newCue("1", "L 1"));
        lightCues.insert(0, newCue("2", "L 1"));
        lightCues.insert(0, newCue("3", "L 1"));
        assertEquals(lightCues.get(0).getNumber(), "3");
        assertEquals(lightCues.get(1).getNumber(), "2");
        assertEquals(lightCues.get(2).getNumber(), "1");
        assertEquals(lightCues.get(0).getLightCueIndex(), 0);
        assertEquals(lightCues.get(1).getLightCueIndex(), 1);
        assertEquals(lightCues.get(2).getLightCueIndex(), 2);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void insertIllegalCueType() {
        lightCues.insert(0, newCue("1", "A 1"));
    }

    @Test
    public void channelLevelChanged() {
        addCue();
        addCue();
        TestingCuesListener listener = new TestingCuesListener();
        lightCues.addListener(listener);
        assertEquals(listener.getNumberOfChannelChanges(), 0);
        lightCues.setChannel(0, 0, 0.5f);
        assertEquals(listener.getNumberOfChannelChanges(), 2);
        assertEquals(listener.getChannelChangeCueIndex(0), 0);
        assertEquals(listener.getChannelChangeIndex(0), 0);
        assertEquals(listener.getChannelChangeCueIndex(1), 1);
        assertEquals(listener.getChannelChangeIndex(1), 0);
    }

    @Test
    public void derivedChannels() {
        derivedChannels(0);
        derivedChannels(NUMBER_OF_CHANNELS - 1);
    }

    @Test
    public void derivedChannelsAfterAdd() {
        addCue();
        lightCues.setChannel(0, 0, 0.1f);
        addCue();
        assertChannelDerived(1, 0, true);
        assertChannelValue(1, 0, 0.1f);
    }

    @Test
    public void derivedSubmasters() {
        assertDerivedSubmasters(0);
        assertDerivedSubmasters(submasters.getNumberOfSubmasters() - 1);
    }

    @Test
    public void insertDeriving() {
        addCue();
        lightCues.setChannel(0, 0, 0.5f);
        lightCues.setCueSubmaster(0, 0, 0.7f);
        addCue();
        new CuePrinter(System.out).print(lightCues);
        Cue cue = newCue("Inserted", "L 1");
        LightCueDetail detail = (LightCueDetail) cue.getDetail();
        detail.getChannelLevel(1).setChannelValue(0.3f);
        detail.getChannelLevel(1).setDerived(false);
        detail.getSubmasterLevel(1).getLevelValue().setValue(0.4f);
        detail.getSubmasterLevel(1).setDerived(false);
        lightCues.insert(1, cue);
        new CuePrinter(System.out).print(lightCues);
        assertChannelDerived(0, 0, false);
        assertChannelDerived(1, 0, true);
        assertChannelDerived(2, 0, true);
        assertChannelDerived(0, 1, true);
        assertChannelDerived(1, 1, false);
        assertChannelDerived(2, 1, true);
        assertCueSubmasterDerived(0, 0, false);
        assertCueSubmasterDerived(1, 0, true);
        assertCueSubmasterDerived(2, 0, true);
        assertCueSubmasterDerived(0, 1, true);
        assertCueSubmasterDerived(1, 1, false);
        assertCueSubmasterDerived(2, 1, true);
        assertChannelValue(0, 0, 0.5f);
        assertChannelValue(1, 0, 0.5f);
        assertChannelValue(2, 0, 0.5f);
        assertChannelValue(0, 1, 0.0f);
        assertChannelValue(1, 1, 0.3f);
        assertChannelValue(2, 1, 0.3f);
        assertCueSubmasterValue(0, 0, 0.7f);
        assertCueSubmasterValue(1, 0, 0.7f);
        assertCueSubmasterValue(2, 0, 0.7f);
        assertCueSubmasterValue(0, 1, 0.0f);
        assertCueSubmasterValue(1, 1, 0.4f);
        assertCueSubmasterValue(2, 1, 0.4f);
    }

    @Test
    public void resetChannel() {
        addCue();
        addCue();
        addCue();
        lightCues.setChannel(0, 0, 0.5f);
        lightCues.setChannel(1, 0, 0.3f);
        assertChannelDerived(0, 0, false);
        assertChannelDerived(1, 0, false);
        assertChannelDerived(2, 0, true);
        assertChannelValue(0, 0, 0.5f);
        assertChannelValue(1, 0, 0.3f);
        assertChannelValue(2, 0, 0.3f);
        lightCues.resetChannel(1, 0);
        assertChannelDerived(0, 0, false);
        assertChannelDerived(1, 0, true);
        assertChannelDerived(2, 0, true);
        assertChannelValue(0, 0, 0.5f);
        assertChannelValue(1, 0, 0.5f);
        assertChannelValue(2, 0, 0.5f);
    }

    @Test
    public void resetChannelCueZero() {
        addCue();
        addCue();
        addCue();
        lightCues.setChannel(0, 0, 0.5f);
        assertChannelDerived(0, 0, false);
        assertChannelDerived(1, 0, true);
        assertChannelDerived(2, 0, true);
        assertChannelValue(0, 0, 0.5f);
        assertChannelValue(1, 0, 0.5f);
        assertChannelValue(2, 0, 0.5f);
        lightCues.resetChannel(0, 0);
        assertChannelDerived(0, 0, true);
        assertChannelDerived(1, 0, true);
        assertChannelDerived(2, 0, true);
        assertChannelValue(0, 0, 0f);
        assertChannelValue(1, 0, 0f);
        assertChannelValue(2, 0, 0f);
    }

    @Test
    public void resetSubmaster() {
        addCue();
        addCue();
        addCue();
        lightCues.setCueSubmaster(0, 0, 0.5f);
        lightCues.setCueSubmaster(1, 0, 0.3f);
        assertCueSubmasterDerived(0, 0, false);
        assertCueSubmasterDerived(1, 0, false);
        assertCueSubmasterDerived(2, 0, true);
        assertCueSubmasterValue(0, 0, 0.5f);
        assertCueSubmasterValue(1, 0, 0.3f);
        assertCueSubmasterValue(2, 0, 0.3f);
        lightCues.resetSubmaster(1, 0);
        assertCueSubmasterDerived(0, 0, false);
        assertCueSubmasterDerived(1, 0, true);
        assertCueSubmasterDerived(2, 0, true);
        assertCueSubmasterValue(0, 0, 0.5f);
        assertCueSubmasterValue(1, 0, 0.5f);
        assertCueSubmasterValue(2, 0, 0.5f);
    }

    @Test
    public void resetSubmasterCueZero() {
        addCue();
        addCue();
        addCue();
        lightCues.setCueSubmaster(0, 0, 0.5f);
        assertCueSubmasterDerived(0, 0, false);
        assertCueSubmasterDerived(1, 0, true);
        assertCueSubmasterDerived(2, 0, true);
        assertCueSubmasterValue(0, 0, 0.5f);
        assertCueSubmasterValue(1, 0, 0.5f);
        assertCueSubmasterValue(2, 0, 0.5f);
        lightCues.resetSubmaster(0, 0);
        assertCueSubmasterDerived(0, 0, true);
        assertCueSubmasterDerived(1, 0, true);
        assertCueSubmasterDerived(2, 0, true);
        assertCueSubmasterValue(0, 0, 0f);
        assertCueSubmasterValue(1, 0, 0f);
        assertCueSubmasterValue(2, 0, 0f);
    }

    @Test
    public void submaster() {
        addCue();
        addCue();
        setSubmasterLevelValue(0, 0, 0.8f);
        lightCues.setCueSubmaster(0, 0, 0.5f);
        CueChannelLevel level = getDetail(0).getChannelLevel(0);
        assertLevelValue("value 0", 0.4f, level.getValue());
        assertLevelValue("channel value 0", 0f, level.getChannelLevelValue().getValue());
        assertLevelValue("submaster value 0", 0.4f, level.getSubmasterValue());
    }

    @Test
    public void submaster2() {
        addCue();
        addCue();
        setSubmasterLevelValue(0, 0, 0.30f);
        setSubmasterLevelValue(1, 0, 0.60f);
        setSubmasterLevelValue(2, 0, 0.90f);
        lightCues.setCueSubmaster(0, 0, 0.5f);
        assertLevelValue("(1a)", 0.15f, getDetail(0).getChannelLevel(0).getValue());
        assertLevelValue("(1b)", 0.15f, getDetail(1).getChannelLevel(0).getValue());
        lightCues.setCueSubmaster(0, 1, 0.5f);
        assertLevelValue("(2a)", 0.30f, getDetail(0).getChannelLevel(0).getValue());
        assertLevelValue("(2b)", 0.30f, getDetail(1).getChannelLevel(0).getValue());
        lightCues.setCueSubmaster(0, 2, 1f);
        assertLevelValue("(3a)", 0.90f, getDetail(0).getChannelLevel(0).getValue());
        assertLevelValue("(3b)", 0.90f, getDetail(1).getChannelLevel(0).getValue());
    }

    @Test
    public void submasterLevelChanged() {
        TestingCuesListener listener = new TestingCuesListener();
        lightCues.addListener(listener);
        addCue();
        addCue();
        assertEquals(listener.getNumberOfSubmasterChanges(), 0);
        lightCues.setCueSubmaster(0, 0, 0.5f);
        assertEquals(listener.getNumberOfSubmasterChanges(), 2);
        assertEquals(listener.getSubmasterChangeCueIndex(0), 0);
        assertEquals(listener.getSubmasterChangeIndex(0), 0);
        assertEquals(listener.getSubmasterChangeCueIndex(1), 1);
        assertEquals(listener.getSubmasterChangeIndex(1), 0);
    }

    private void assertChannelDerived(final int cueIndex, final int channelIndex, final boolean expected) {
        boolean derived = getDetail(cueIndex).getChannelLevel(channelIndex).isDerived();
        StringBuilder b = new StringBuilder();
        b.append("(channelLevel.isDerived() at cueIndex=");
        b.append(cueIndex);
        b.append(" , and channelIndex");
        b.append(channelIndex);
        b.append(")");
        String message = b.toString();
        assertTrue(expected == derived, message);
    }

    private void assertChannelValue(final int cueIndex, final int channelIndex, final float expected) {
        float value = getDetail(cueIndex).getChannelLevel(channelIndex).getValue();
        StringBuilder b = new StringBuilder();
        b.append("(channelLevel.getValue() at cueIndex=");
        b.append(cueIndex);
        b.append(" , and channelIndex");
        b.append(channelIndex);
        b.append(")");
        String message = b.toString();
        assertLevelValue(message, expected, value);
    }

    private void assertChannelActive(final int cueIndex, final int channelIndex, final boolean expected) {
        LevelValue value = getDetail(cueIndex).getChannelLevel(channelIndex).getChannelLevelValue();
        assertEquals(value.isActive(), expected);
    }

    private void assertCueSubmasterActive(final int cueIndex, final int submasterIndex, final boolean expected) {
        LevelValue value = getDetail(cueIndex).getSubmasterLevel(submasterIndex).getLevelValue();
        assertEquals(value.isActive(), expected);
    }

    private void assertCueChannelSubmasterActive(final int cueIndex, final int channelIndex, final boolean expected) {
        LevelValue value = getDetail(cueIndex).getChannelLevel(channelIndex).getSubmasterLevelValue();
        assertEquals(value.isActive(), expected);
    }

    private void assertCueSubmasterDerived(final int cueIndex, final int submasterIndex, final boolean expected) {
        boolean derived = getDetail(cueIndex).getSubmasterLevel(submasterIndex).isDerived();
        StringBuilder b = new StringBuilder();
        b.append("(submasterLevel.isDerived() at cueIndex=");
        b.append(cueIndex);
        b.append(" , and submasterIndex");
        b.append(submasterIndex);
        b.append(")");
        String message = b.toString();
        assertTrue(expected == derived, message);
    }

    private void assertCueSubmasterValue(final int cueIndex, final int submasterIndex, final float expected) {
        float value = getDetail(cueIndex).getSubmasterLevel(submasterIndex).getValue();
        StringBuilder b = new StringBuilder();
        b.append("(submasterLevel.getValue() at cueIndex=");
        b.append(cueIndex);
        b.append(" , and submasterIndex");
        b.append(submasterIndex);
        b.append(")");
        String message = b.toString();
        assertLevelValue(message, expected, value);
    }

    private void assertLevelValue(final String message, final float expectedValue, final float actualValue) {
        assertEquals(actualValue, expectedValue, 0.001f, message);
    }

    /**
     * Verify derived updates with <code>CueList.setChannel()</code>.
     *
     * @param channelIndex
     */
    private void derivedChannels(final int channelIndex) {
        addCue();
        addCue();
        addCue();
        addCue();
        assertChannelDerived(0, channelIndex, true);
        assertChannelDerived(1, channelIndex, true);
        assertChannelDerived(2, channelIndex, true);
        assertChannelDerived(3, channelIndex, true);
        assertChannelValue(0, channelIndex, 0f);
        assertChannelValue(1, channelIndex, 0f);
        assertChannelValue(2, channelIndex, 0f);
        assertChannelValue(3, channelIndex, 0f);
        lightCues.setChannel(0, channelIndex, 0.5f);
        assertChannelDerived(0, channelIndex, false);
        assertChannelDerived(1, channelIndex, true);
        assertChannelDerived(2, channelIndex, true);
        assertChannelDerived(3, channelIndex, true);
        assertChannelValue(0, channelIndex, 0.5f);
        assertChannelValue(1, channelIndex, 0.5f);
        assertChannelValue(2, channelIndex, 0.5f);
        assertChannelValue(3, channelIndex, 0.5f);
        lightCues.setChannel(2, channelIndex, 0.3f);
        assertChannelDerived(0, channelIndex, false);
        assertChannelDerived(1, channelIndex, true);
        assertChannelDerived(2, channelIndex, false);
        assertChannelDerived(3, channelIndex, true);
        assertChannelValue(0, channelIndex, 0.5f);
        assertChannelValue(1, channelIndex, 0.5f);
        assertChannelValue(2, channelIndex, 0.3f);
        assertChannelValue(3, channelIndex, 0.3f);
    }

    private void assertDerivedSubmasters(final int submasterIndex) {
        addCue();
        addCue();
        addCue();
        addCue();
        assertCueSubmasterDerived(0, submasterIndex, true);
        assertCueSubmasterDerived(1, submasterIndex, true);
        assertCueSubmasterDerived(2, submasterIndex, true);
        assertCueSubmasterDerived(3, submasterIndex, true);
        assertCueSubmasterValue(0, submasterIndex, 0f);
        assertCueSubmasterValue(1, submasterIndex, 0f);
        assertCueSubmasterValue(2, submasterIndex, 0f);
        assertCueSubmasterValue(3, submasterIndex, 0f);
        lightCues.setCueSubmaster(0, submasterIndex, 0.5f);
        assertCueSubmasterDerived(0, submasterIndex, false);
        assertCueSubmasterDerived(1, submasterIndex, true);
        assertCueSubmasterDerived(2, submasterIndex, true);
        assertCueSubmasterDerived(3, submasterIndex, true);
        assertCueSubmasterValue(0, submasterIndex, 0.5f);
        assertCueSubmasterValue(1, submasterIndex, 0.5f);
        assertCueSubmasterValue(2, submasterIndex, 0.5f);
        assertCueSubmasterValue(3, submasterIndex, 0.5f);
        lightCues.setCueSubmaster(2, submasterIndex, 0.3f);
        assertCueSubmasterDerived(0, submasterIndex, false);
        assertCueSubmasterDerived(1, submasterIndex, true);
        assertCueSubmasterDerived(2, submasterIndex, false);
        assertCueSubmasterDerived(3, submasterIndex, true);
        assertCueSubmasterValue(0, submasterIndex, 0.5f);
        assertCueSubmasterValue(1, submasterIndex, 0.5f);
        assertCueSubmasterValue(2, submasterIndex, 0.3f);
        assertCueSubmasterValue(3, submasterIndex, 0.3f);
    }

    private void assertSubmasterActive(final int submasterIndex, final int channelIndex, final boolean expected) {
        Submaster submaster = submasters.get(submasterIndex);
        Level level = submaster.getLevel(channelIndex);
        assertEquals(level.isActive(), expected);
    }

    private LightCueDetail getDetail(final int cueIndex) {
        return lightCues.getDetail(cueIndex);
    }

    private void setSubmasterLevelValue(final int submasterIndex, final int channelIndex, final float value) {
        Level level = submasters.get(submasterIndex).getLevel(channelIndex);
        level.setValue(value);
        level.setActive(true);
    }

    private void addCue() {
        int index = lightCues.size();
        String number = Integer.toString(index + 1);
        Cue cue = newCue(number, "L 1");
        lightCues.insert(index, cue);
    }

    private Cue newCue(final String number, final String description) {
        Cue cue = new Cue(number, "", "", description);
        new CueDetailFactory(NUMBER_OF_CHANNELS, NUMBER_OF_SUBMASTERS).update(cue);
        return cue;
    }

    private void setSubmasterActive(final int cueIndex, final int channelIndex, final boolean active) {
        lightCues.getDetail(cueIndex).getChannelLevel(channelIndex).getSubmasterLevelValue().setActive(active);
    }

    private void setChannelDerived(final int cueIndex, final int channelIndex, final boolean derived) {
        lightCues.getDetail(cueIndex).getChannelLevel(channelIndex).setDerived(derived);
    }
}
