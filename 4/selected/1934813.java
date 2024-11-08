package be.lassi.domain;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;
import be.lassi.base.DirtyIndicator;
import be.lassi.cues.Cue;
import be.lassi.cues.CueChannelLevel;
import be.lassi.cues.CueDetailFactory;
import be.lassi.cues.CueSubmasterLevel;
import be.lassi.cues.LightCueDetail;

/**
 * Tests class <code>ShowCopier</code>.
 */
public class ShowCopierTestCase {

    private Show oldShow;

    private Show newShow;

    private final DirtyIndicator dirty = new DirtyIndicator();

    @Test
    public void testCopyChannels1() {
        oldShow = ShowBuilder.build(2, 3, 2, "");
        newShow = ShowBuilder.build(dirty, 4, 3, 4, "");
        setChannelName(0, "one");
        setChannelName(1, "two");
        copy();
        assertChannelName("one", 0);
        assertChannelName("two", 1);
        assertChannelName("Channel 3", 2);
        assertChannelName("Channel 4", 3);
    }

    @Test
    public void testCopyChannels2() {
        oldShow = ShowBuilder.build(4, 3, 4, "");
        newShow = ShowBuilder.build(dirty, 2, 3, 2, "");
        setChannelName(0, "one");
        setChannelName(1, "two");
        setChannelName(2, "three");
        setChannelName(3, "four");
        copy();
        assertChannelName("one", 0);
        assertChannelName("two", 1);
    }

    @Test
    public void testCopyDimmers1() {
        oldShow = ShowBuilder.build(2, 3, 2, "");
        newShow = ShowBuilder.build(dirty, 4, 3, 4, "");
        setDimmerName(0, "one");
        setDimmerName(1, "two");
        setDimmerChannel(1, 0);
        copy();
        assertDimmerName("one", 0);
        assertDimmerName("two", 1);
        assertDimmerName("Dimmer 3", 2);
        assertDimmerName("Dimmer 4", 3);
        assertDimmerChannel("", 0);
        assertDimmerChannel("Channel 1", 1);
        assertDimmerChannel("", 2);
        assertDimmerChannel("", 3);
    }

    @Test
    public void testCopyDimmers2() {
        oldShow = ShowBuilder.build(4, 3, 4, "");
        newShow = ShowBuilder.build(dirty, 2, 3, 2, "");
        setDimmerName(0, "one");
        setDimmerName(1, "two");
        setDimmerName(2, "three");
        setDimmerName(3, "four");
        setDimmerChannel(1, 0);
        setDimmerChannel(3, 1);
        copy();
        assertDimmerName("one", 0);
        assertDimmerName("two", 1);
        assertDimmerChannel("", 0);
        assertDimmerChannel("Channel 1", 1);
    }

    @Test
    public void testCopySubmaster1() {
        oldShow = ShowBuilder.build(2, 3, 2, "");
        newShow = ShowBuilder.build(dirty, 4, 4, 4, "");
        setSubmasterName(0, "one");
        setSubmasterName(1, "two");
        setSubmasterName(2, "three");
        setSubmasterLevelActive(0, 0, true);
        setSubmasterLevelActive(0, 1, true);
        setSubmasterLevelValue(0, 1, .1f);
        copy();
        assertSubmasterName("one", 0);
        assertSubmasterName("two", 1);
        assertSubmasterName("three", 2);
        assertSubmasterName("Submaster 4", 3);
        assertSubmasterLevelValue(0f, 0, 0);
        assertSubmasterLevelValue(.1f, 0, 1);
        assertSubmasterLevelValue(0f, 0, 2);
        assertSubmasterLevelActive(true, 0, 0);
        assertSubmasterLevelActive(true, 0, 1);
        assertSubmasterLevelActive(false, 0, 2);
        assertSubmasterLevelActive(false, 0, 3);
    }

    @Test
    public void testCopySubmaster2() {
        oldShow = ShowBuilder.build(4, 4, 4, "");
        newShow = ShowBuilder.build(dirty, 2, 3, 2, "");
        setSubmasterName(0, "one");
        setSubmasterName(1, "two");
        setSubmasterName(2, "three");
        setSubmasterName(3, "four");
        setSubmasterLevelActive(0, 1, true);
        setSubmasterLevelValue(0, 1, .1f);
        copy();
        assertSubmasterName("one", 0);
        assertSubmasterName("two", 1);
        assertSubmasterLevelValue(0f, 0, 0);
        assertSubmasterLevelValue(.1f, 0, 1);
        assertSubmasterLevelActive(false, 0, 0);
        assertSubmasterLevelActive(true, 0, 1);
    }

    @Test
    public void testCopyGroups1() {
        oldShow = ShowBuilder.build(2, 3, 2, "");
        newShow = ShowBuilder.build(dirty, 4, 3, 4, "");
        Channel channel1 = oldShow.getChannels().get(0);
        Channel channel2 = oldShow.getChannels().get(1);
        Group group1 = new Group("Group 1");
        Group group2 = new Group("Group 2");
        group1.add(channel1);
        group1.add(channel2);
        group2.add(channel2);
        oldShow.getGroups().add(group1);
        oldShow.getGroups().add(group2);
        copy();
        Group newGroup1 = newShow.getGroups().get(0);
        Group newGroup2 = newShow.getGroups().get(1);
        assertEquals(newGroup1.getName(), "Group 1");
        assertEquals(newGroup2.getName(), "Group 2");
        assertEquals(newGroup1.size(), 2);
        assertEquals(newGroup2.size(), 1);
        assertEquals(newGroup1.get(0).getName(), "Channel 1");
        assertEquals(newGroup1.get(1).getName(), "Channel 2");
        assertEquals(newGroup2.get(0).getName(), "Channel 2");
        dirty.clear();
        newGroup1.setName("new");
        assertTrue(dirty.isDirty());
    }

    @Test
    public void copyCues1() {
        oldShow = ShowBuilder.build(4, 2, 4, "");
        newShow = ShowBuilder.build(dirty, 8, 4, 8, "");
        Cue oldCue1 = newCue("1", "page1", "prompt1", "L 1");
        Cue oldCue2 = newCue("2", "page2", "prompt2", "L 2");
        Cue oldCue3 = newCue("3", "page3", "prompt3", "L 3");
        LightCueDetail detail1 = (LightCueDetail) oldCue1.getDetail();
        LightCueDetail detail2 = (LightCueDetail) oldCue2.getDetail();
        LightCueDetail detail3 = (LightCueDetail) oldCue3.getDetail();
        oldShow.getCues().getLightCues().setChannel(0, 0, .5f);
        oldShow.getCues().getLightCues().setCueSubmaster(0, 0, .7f);
        copy();
        Cue newCue1 = newShow.getCues().get(0);
        Cue newCue2 = newShow.getCues().get(1);
        Cue newCue3 = newShow.getCues().get(2);
        LightCueDetail newDetail1 = (LightCueDetail) newCue1.getDetail();
        LightCueDetail newDetail2 = (LightCueDetail) newCue2.getDetail();
        LightCueDetail newDetail3 = (LightCueDetail) newCue3.getDetail();
        assertCue(newCue1, "1", "page1", "prompt1", "L 1");
        assertCue(newCue2, "2", "page2", "prompt2", "L 2");
        assertCue(newCue3, "3", "page3", "prompt3", "L 3");
        assertLightCueDetail(detail1, newDetail1);
        assertLightCueDetail(detail2, newDetail2);
        assertLightCueDetail(detail3, newDetail3);
    }

    @Test
    public void copyCues2() {
        oldShow = ShowBuilder.build(8, 4, 8, "");
        newShow = ShowBuilder.build(dirty, 4, 2, 4, "");
        Cue oldCue1 = newCue("1", "page1", "prompt1", "L 1");
        Cue oldCue2 = newCue("2", "page2", "prompt2", "L 2");
        Cue oldCue3 = newCue("3", "page3", "prompt3", "L 3");
        LightCueDetail detail1 = (LightCueDetail) oldCue1.getDetail();
        LightCueDetail detail2 = (LightCueDetail) oldCue2.getDetail();
        LightCueDetail detail3 = (LightCueDetail) oldCue3.getDetail();
        oldShow.getCues().getLightCues().setChannel(0, 0, .5f);
        oldShow.getCues().getLightCues().setCueSubmaster(0, 0, .7f);
        copy();
        Cue newCue1 = newShow.getCues().get(0);
        Cue newCue2 = newShow.getCues().get(1);
        Cue newCue3 = newShow.getCues().get(2);
        LightCueDetail newDetail1 = (LightCueDetail) newCue1.getDetail();
        LightCueDetail newDetail2 = (LightCueDetail) newCue2.getDetail();
        LightCueDetail newDetail3 = (LightCueDetail) newCue3.getDetail();
        assertCue(newCue1, "1", "page1", "prompt1", "L 1");
        assertCue(newCue2, "2", "page2", "prompt2", "L 2");
        assertCue(newCue3, "3", "page3", "prompt3", "L 3");
        assertLightCueDetail(detail1, newDetail1);
        assertLightCueDetail(detail2, newDetail2);
        assertLightCueDetail(detail3, newDetail3);
    }

    private void copy() {
        new ShowCopier(newShow, oldShow).copy();
    }

    private void setChannelName(final int index, final String name) {
        Channel channel = oldShow.getChannels().get(index);
        channel.setName(name);
    }

    private void assertChannelName(final String expected, final int index) {
        String name = newShow.getChannels().get(index).getName();
        assertEquals(name, expected);
    }

    private void setDimmerName(final int index, final String name) {
        Dimmer dimmer = oldShow.getDimmers().get(index);
        dimmer.setName(name);
    }

    private void assertDimmerName(final String expected, final int index) {
        Dimmer dimmer = newShow.getDimmers().get(index);
        String name = dimmer.getName();
        assertEquals(name, expected);
    }

    private void assertDimmerChannel(final String expected, final int index) {
        Dimmer dimmer = newShow.getDimmers().get(index);
        Channel channel = dimmer.getChannel();
        String name = "";
        if (channel != null) {
            name = channel.getName();
        }
        assertEquals(name, expected);
    }

    private void setDimmerChannel(final int dimmerIndex, final int channelIndex) {
        Dimmer dimmer = oldShow.getDimmers().get(dimmerIndex);
        Channel channel = oldShow.getChannels().get(channelIndex);
        dimmer.setChannel(channel);
    }

    private void setSubmasterName(final int index, final String name) {
        Submaster submaster = oldShow.getSubmasters().get(index);
        submaster.setName(name);
    }

    private void assertSubmasterName(final String expected, final int index) {
        Submaster submaster = newShow.getSubmasters().get(index);
        String name = submaster.getName();
        assertEquals(name, expected);
    }

    private void setSubmasterLevelValue(final int submasterIndex, final int channelIndex, final float value) {
        Submaster submaster = oldShow.getSubmasters().get(submasterIndex);
        submaster.setLevelValue(channelIndex, value);
    }

    private void assertSubmasterLevelValue(final float expected, final int submasterIndex, final int channelIndex) {
        Submaster submaster = newShow.getSubmasters().get(submasterIndex);
        float value = submaster.getLevelValue(channelIndex);
        assertEquals(value, expected, 0.001f);
    }

    private void setSubmasterLevelActive(final int submasterIndex, final int channelIndex, final boolean active) {
        Submaster submaster = oldShow.getSubmasters().get(submasterIndex);
        Level level = submaster.getLevel(channelIndex);
        level.setActive(active);
    }

    private void assertSubmasterLevelActive(final boolean expected, final int submasterIndex, final int channelIndex) {
        Submaster submaster = newShow.getSubmasters().get(submasterIndex);
        boolean value = submaster.isChannelActive(channelIndex);
        assertEquals(value, expected);
    }

    private void assertLightCueDetail(final LightCueDetail detail1, final LightCueDetail detail2) {
        assertChannelLevels(detail1, detail2);
        assertSubmasterLevels(detail1, detail2);
        assertEquals(detail1.getTiming(), detail2.getTiming());
    }

    private void assertChannelLevels(final LightCueDetail detail1, final LightCueDetail detail2) {
        int channelCount1 = detail1.getNumberOfChannels();
        int channelCount2 = detail2.getNumberOfChannels();
        int channelCount = Math.min(channelCount1, channelCount2);
        for (int i = 0; i < channelCount; i++) {
            CueChannelLevel level1 = detail1.getChannelLevel(i);
            CueChannelLevel level2 = detail2.getChannelLevel(i);
            String message = "cueChannelLevel[" + i + "]";
            assertEquals(level1, level2, message);
        }
        for (int i = channelCount; i < channelCount2; i++) {
            CueChannelLevel level = detail2.getChannelLevel(i);
            String message = "cueChannelLevel[" + i + "]";
            assertTrue(level.isDerived(), message);
            assertDefaultCueChannelLevel(message, level);
        }
    }

    private void assertDefaultCueChannelLevel(final String message, final CueChannelLevel level) {
        assertDefaultLevelValue(message, level.getChannelLevelValue());
        assertDefaultLevelValue(message, level.getSubmasterLevelValue());
    }

    private void assertSubmasterLevels(final LightCueDetail detail1, final LightCueDetail detail2) {
        int submasterCount1 = detail1.getNumberOfSubmasters();
        int submasterCount2 = detail2.getNumberOfSubmasters();
        int submasterCount = Math.min(submasterCount1, submasterCount2);
        for (int i = 0; i < submasterCount; i++) {
            CueSubmasterLevel level1 = detail1.getSubmasterLevel(i);
            CueSubmasterLevel level2 = detail2.getSubmasterLevel(i);
            String message = "submasterLevel[" + i + "]";
            assertEquals(level1, level2, message);
        }
        for (int i = submasterCount; i < submasterCount2; i++) {
            CueSubmasterLevel level = detail2.getSubmasterLevel(i);
            String message = "submasterLevel[" + i + "]";
            assertTrue(level.isDerived(), message);
            assertDefaultLevelValue(message, level.getLevelValue());
        }
    }

    private void assertDefaultLevelValue(final String message, final LevelValue levelValue) {
        assertEquals(levelValue.getIntValue(), 0, message);
        assertFalse(levelValue.isActive(), message);
    }

    private void assertCue(final Cue cue, final String number, final String page, final String prompt, final String description) {
        assertEquals(cue.getNumber(), number);
        assertEquals(cue.getPage(), page);
        assertEquals(cue.getPrompt(), prompt);
        assertEquals(cue.getDescription(), description);
    }

    private Cue newCue(final String number, final String page, final String prompt, final String description) {
        Cue cue = new Cue(number, page, prompt, description);
        int channelCount = oldShow.getNumberOfChannels();
        int submasterCount = oldShow.getNumberOfSubmasters();
        CueDetailFactory f = new CueDetailFactory(channelCount, submasterCount);
        f.update(cue);
        oldShow.getCues().add(cue);
        return cue;
    }
}
