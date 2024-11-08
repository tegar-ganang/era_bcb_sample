package be.lassi.control;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import be.lassi.context.ShowContext;
import be.lassi.cues.Cue;
import be.lassi.cues.CueDetailFactory;
import be.lassi.cues.LightCueDetail;
import be.lassi.domain.Show;
import be.lassi.preferences.AllPreferences;

/**
 * Tests class <code>SheetControl</code>.
 */
public class SheetControlTestCase {

    private final ShowContext context;

    private final List<LevelControl> levelControls;

    private final LightCueDetail detail;

    public SheetControlTestCase() {
        AllPreferences preferences = new AllPreferences();
        preferences.getConnectionProperties().setEnabled(false);
        preferences.getConnectionProperties().setUdpPort("");
        context = new ShowContext(preferences);
        context.setShow(new Show(24, 8, 48, ""));
        Cue cue1 = new Cue("1", "", "", "L 1");
        new CueDetailFactory(context.getShow().getNumberOfChannels(), context.getShow().getNumberOfSubmasters()).update(cue1);
        Cue cue2 = new Cue("2", "", "", "L 1");
        new CueDetailFactory(context.getShow().getNumberOfChannels(), context.getShow().getNumberOfSubmasters()).update(cue2);
        context.getShow().getCues().add(cue1);
        context.getShow().getCues().add(cue2);
        LevelControl levelControl1 = new LevelControl("1");
        LevelControl levelControl2 = new LevelControl("2");
        LevelControl levelControl3 = new LevelControl("3");
        levelControl1.setLevel(0);
        levelControl2.setLevel(0);
        levelControl3.setLevel(0);
        levelControls = new ArrayList<LevelControl>();
        levelControls.add(levelControl1);
        levelControls.add(levelControl2);
        levelControls.add(levelControl3);
        detail = context.getShow().getCues().getLightCues().getDetail(0);
        detail.getSubmasterLevel(0).setValue(0.1f);
        detail.getSubmasterLevel(1).setValue(0.2f);
        detail.getSubmasterLevel(2).setValue(0.3f);
        detail.getSubmasterLevel(3).setValue(0.4f);
        detail.getSubmasterLevel(4).setValue(0.5f);
        detail.getSubmasterLevel(5).setValue(0.6f);
        detail.getSubmasterLevel(6).setValue(0.7f);
        detail.getSubmasterLevel(7).setValue(0.8f);
        detail.getChannelLevel(0).setChannelValue(0.01f);
        detail.getChannelLevel(1).setChannelValue(0.11f);
        detail.getChannelLevel(2).setChannelValue(0.21f);
        detail.getChannelLevel(3).setChannelValue(0.31f);
        detail.getChannelLevel(4).setChannelValue(0.41f);
        detail.getChannelLevel(5).setChannelValue(0.51f);
        detail.getChannelLevel(6).setChannelValue(0.61f);
        detail.getChannelLevel(7).setChannelValue(0.71f);
    }

    @Test
    public void testSetFocusSubmaster() {
        SheetControl sheetControl = new SheetControl(context);
        sheetControl.setLevelControls(levelControls);
        assertSubmaster(-1, 0);
        assertSubmaster(-1, 1);
        assertSubmaster(-1, 2);
        assertSubmaster(-1, 3);
        assertSubmaster(-1, 4);
        assertLevelControlValue(0, 0);
        assertLevelControlValue(0, 1);
        assertLevelControlValue(0, 2);
        sheetControl.setFocusSubmaster(0, 0);
        assertSubmaster(0, 0);
        assertSubmaster(1, 1);
        assertSubmaster(2, 2);
        assertSubmaster(-1, 3);
        assertSubmaster(-1, 4);
        assertLevelControlValue(10, 0);
        assertLevelControlValue(20, 1);
        assertLevelControlValue(30, 2);
        sheetControl.setFocusSubmaster(0, 2);
        assertSubmaster(0, 0);
        assertSubmaster(1, 1);
        assertSubmaster(2, 2);
        assertSubmaster(-1, 3);
        assertSubmaster(-1, 4);
        sheetControl.setFocusSubmaster(0, 3);
        assertSubmaster(-1, 0);
        assertSubmaster(-1, 1);
        assertSubmaster(-1, 2);
        assertSubmaster(0, 3);
        assertSubmaster(1, 4);
        assertSubmaster(2, 5);
        assertSubmaster(-1, 6);
        assertLevelControlValue(40, 0);
        assertLevelControlValue(50, 1);
        assertLevelControlValue(60, 2);
        sheetControl.setFocusSubmaster(0, 6);
        assertSubmaster(-1, 0);
        assertSubmaster(-1, 1);
        assertSubmaster(-1, 2);
        assertSubmaster(-1, 3);
        assertSubmaster(-1, 4);
        assertSubmaster(-1, 5);
        assertSubmaster(0, 6);
        assertSubmaster(1, 7);
        assertLevelControlValue(70, 0);
        assertLevelControlValue(80, 1);
        assertLevelControlValue(0, 2);
    }

    @Test
    public void testSetFocusChannel() {
        SheetControl sheetControl = new SheetControl(context);
        sheetControl.setLevelControls(levelControls);
        int[] channelIndexes = { 2, 1, 3, 0, 4, 5, 6, 7 };
        assertChannel(-1, channelIndexes[0]);
        assertChannel(-1, channelIndexes[1]);
        assertChannel(-1, channelIndexes[2]);
        assertChannel(-1, channelIndexes[3]);
        assertChannel(-1, channelIndexes[4]);
        assertLevelControlValue(0, 0);
        assertLevelControlValue(0, 1);
        assertLevelControlValue(0, 2);
        sheetControl.setFocusChannel(0, 0, channelIndexes);
        assertChannel(0, channelIndexes[0]);
        assertChannel(1, channelIndexes[1]);
        assertChannel(2, channelIndexes[2]);
        assertChannel(-1, channelIndexes[3]);
        assertChannel(-1, channelIndexes[4]);
        assertLevelControlValue(21, 0);
        assertLevelControlValue(11, 1);
        assertLevelControlValue(31, 2);
        sheetControl.setFocusChannel(0, 2, channelIndexes);
        assertChannel(0, channelIndexes[0]);
        assertChannel(1, channelIndexes[1]);
        assertChannel(2, channelIndexes[2]);
        assertChannel(-1, channelIndexes[3]);
        assertChannel(-1, channelIndexes[4]);
        sheetControl.setFocusChannel(0, 3, channelIndexes);
        assertChannel(-1, channelIndexes[0]);
        assertChannel(-1, channelIndexes[1]);
        assertChannel(-1, channelIndexes[2]);
        assertChannel(0, channelIndexes[3]);
        assertChannel(1, channelIndexes[4]);
        assertChannel(2, channelIndexes[5]);
        assertChannel(-1, channelIndexes[6]);
        assertLevelControlValue(1, 0);
        assertLevelControlValue(41, 1);
        assertLevelControlValue(51, 2);
        sheetControl.setFocusChannel(0, 6, channelIndexes);
        assertChannel(-1, channelIndexes[0]);
        assertChannel(-1, channelIndexes[1]);
        assertChannel(-1, channelIndexes[2]);
        assertChannel(-1, channelIndexes[3]);
        assertChannel(-1, channelIndexes[4]);
        assertChannel(-1, channelIndexes[5]);
        assertChannel(0, channelIndexes[6]);
        assertChannel(1, channelIndexes[7]);
        assertLevelControlValue(61, 0);
        assertLevelControlValue(71, 1);
        assertLevelControlValue(0, 2);
    }

    private void assertSubmaster(final int expectedLevelControlIndex, final int submasterIndex) {
        LevelControl expectedLevelControl = null;
        if (expectedLevelControlIndex >= 0) {
            expectedLevelControl = levelControls.get(expectedLevelControlIndex);
        }
        LevelControl levelControl = detail.getSubmasterLevel(submasterIndex).getLevelControl();
        assertEquals(expectedLevelControl, levelControl);
    }

    private void assertChannel(final int expectedLevelControlIndex, final int channelIndex) {
        LevelControl expectedLevelControl = null;
        if (expectedLevelControlIndex >= 0) {
            expectedLevelControl = levelControls.get(expectedLevelControlIndex);
        }
        LevelControl levelControl = detail.getChannelLevel(channelIndex).getLevelControl();
        assertEquals(expectedLevelControl, levelControl);
    }

    private void assertLevelControlValue(final int expectedValue, final int levelControlIndex) {
        assertEquals(expectedValue, levelControls.get(levelControlIndex).getLevel());
    }
}
