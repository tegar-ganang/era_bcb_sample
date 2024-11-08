package be.lassi.ui.sheet;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import java.io.IOException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import be.lassi.context.ShowContext;
import be.lassi.control.device.Control;
import be.lassi.control.device.ControlReader;
import be.lassi.control.device.LevelControl;
import be.lassi.control.midi.MidiPreferences;
import be.lassi.cues.Cue;
import be.lassi.cues.CueDetailFactory;
import be.lassi.cues.LightCueDetail;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;
import be.lassi.preferences.AllPreferences;
import be.lassi.ui.sheet.cells.CellFocusListener;

/**
 * Tests class <code>SheetPresentationModel</code>.
 */
public class SheetPresentationModelTestCase {

    private static final int SUBMASTER_COUNT = 24;

    private static final int CHANNEL_COUNT = 32;

    private SheetPresentationModel model;

    private ShowContext context;

    private LightCueDetail detail;

    @BeforeMethod
    public void init() {
        AllPreferences preferences = new AllPreferences();
        preferences.getConnectionPreferences().setEnabled(false);
        context = new ShowContext(preferences);
        Show show = ShowBuilder.build(CHANNEL_COUNT, SUBMASTER_COUNT, 48, "");
        context.setShow(show);
        context.getPreferences().getMidiPreferences().setEnabled(true);
        Cue cue1 = new Cue("1", "", "", "L 1");
        new CueDetailFactory(context.getShow().getNumberOfChannels(), context.getShow().getNumberOfSubmasters()).update(cue1);
        Cue cue2 = new Cue("2", "", "", "L 1");
        new CueDetailFactory(context.getShow().getNumberOfChannels(), context.getShow().getNumberOfSubmasters()).update(cue2);
        context.getShow().getCues().add(cue1);
        context.getShow().getCues().add(cue2);
        detail = context.getShow().getCues().getLightCues().getDetail(0);
        detail.getSubmasterLevel(0).getLevelValue().setValue(0.11f);
        detail.getSubmasterLevel(1).getLevelValue().setValue(0.12f);
        detail.getSubmasterLevel(2).getLevelValue().setValue(0.13f);
        detail.getSubmasterLevel(3).getLevelValue().setValue(0.14f);
        detail.getSubmasterLevel(4).getLevelValue().setValue(0.15f);
        detail.getSubmasterLevel(5).getLevelValue().setValue(0.16f);
        detail.getSubmasterLevel(6).getLevelValue().setValue(0.17f);
        detail.getSubmasterLevel(7).getLevelValue().setValue(0.18f);
        detail.getSubmasterLevel(8).getLevelValue().setValue(0.19f);
        detail.getSubmasterLevel(9).getLevelValue().setValue(0.20f);
        detail.getSubmasterLevel(10).getLevelValue().setValue(0.21f);
        detail.getSubmasterLevel(11).getLevelValue().setValue(0.22f);
        detail.getChannelLevel(0).setChannelValue(0.51f);
        detail.getChannelLevel(1).setChannelValue(0.52f);
        detail.getChannelLevel(2).setChannelValue(0.53f);
        detail.getChannelLevel(3).setChannelValue(0.54f);
        detail.getChannelLevel(4).setChannelValue(0.55f);
        detail.getChannelLevel(5).setChannelValue(0.56f);
        detail.getChannelLevel(6).setChannelValue(0.57f);
        detail.getChannelLevel(7).setChannelValue(0.58f);
        detail.getChannelLevel(8).setChannelValue(0.59f);
        detail.getChannelLevel(9).setChannelValue(0.60f);
        detail.getChannelLevel(10).setChannelValue(0.61f);
        detail.getChannelLevel(11).setChannelValue(0.62f);
        detail.getChannelLevel(12).setChannelValue(0.63f);
        detail.getChannelLevel(13).setChannelValue(0.64f);
        detail.getChannelLevel(14).setChannelValue(0.65f);
        detail.getChannelLevel(15).setChannelValue(0.66f);
        detail.getChannelLevel(16).setChannelValue(0.67f);
        detail.getChannelLevel(17).setChannelValue(0.68f);
        detail.getChannelLevel(18).setChannelValue(0.69f);
        detail.getChannelLevel(19).setChannelValue(0.70f);
        model = new SheetPresentationModel(context);
    }

    @AfterMethod
    public void closeContext() {
        context.close();
    }

    @Test
    public void testInitialStatusText() {
        assertEquals(model.getStatusText().getString(), "");
    }

    @Test
    public void testHooverOverDetail() {
        assertEquals(model.getStatusText().getString(), "");
        model.hooverOverDetailAt(0, 0);
        assertEquals(model.getStatusText().getString(), "Cue 1");
        model.hooverOverDetailAt(0, 1);
        assertEquals(model.getStatusText().getString(), "Cue 2");
    }

    @Test
    public void testMidiEnabled() {
        MidiPreferences mp = context.getPreferences().getMidiPreferences();
        mp.setEnabled(true);
        assertEquals(model.getMidiEnabled().getValue(), Boolean.TRUE);
        mp.setEnabled(false);
        assertEquals(model.getMidiEnabled().getValue(), Boolean.FALSE);
        model.getMidiEnabled().setValue(true);
        assertTrue(mp.isEnabled());
        model.getMidiEnabled().setValue(false);
        assertFalse(mp.isEnabled());
    }

    @Test
    public void testSetFocusSubmaster() throws IOException {
        Control control = new ControlReader().read("library/controls/BCF2000-faders-and-encoders.xml");
        context.getControlHolder().setValue(control);
        assertSubmasters(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1);
        assertLevels(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        CellFocusListener cfl = model.getCellFocusListener();
        int row = 1 + 0;
        cfl.setFocus(row, 0);
        assertSubmasters(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1);
        assertLevels(11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 0, 0, 0, 0);
        row = 1 + 2;
        cfl.setFocus(row, 0);
        assertSubmasters(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1);
        assertLevels(11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 0, 0, 0, 0);
        row = 1 + 9;
        cfl.setFocus(row, 0);
        assertSubmasters(-1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
        assertLevels(19, 20, 21, 22, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    @Test
    public void testSetFocusChannel() throws IOException {
        Control control = new ControlReader().read("library/controls/BCF2000-faders-and-encoders.xml");
        context.getControlHolder().setValue(control);
        assertChannels(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1);
        assertLevels(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        CellFocusListener cfl = model.getCellFocusListener();
        int row = 1 + SUBMASTER_COUNT + 0;
        cfl.setFocus(row, 0);
        assertChannels(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1);
        assertLevels(51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64);
        row = 1 + SUBMASTER_COUNT + 9;
        cfl.setFocus(row, 0);
        assertChannels(-1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1);
        assertLevels(59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 0, 0, 0, 0);
        row = 1 + SUBMASTER_COUNT + 16;
        cfl.setFocus(row, 0);
        assertChannels(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8);
        assertLevels(67, 68, 69, 70, 0, 0, 0, 0);
    }

    @Test
    public void testSetFocusChannelWithFadersOnly() throws IOException {
        Control control = new ControlReader().read("library/controls/BCF2000-faders.xml");
        context.getControlHolder().setValue(control);
        assertChannels(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1);
        assertLevels(0, 0, 0, 0, 0, 0, 0, 0);
        CellFocusListener cfl = model.getCellFocusListener();
        int row = 1 + SUBMASTER_COUNT + 0;
        cfl.setFocus(row, 0);
        assertChannels(0, 1, 2, 3, 4, 5, 6, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1);
        assertLevels(51, 52, 53, 54, 55, 56, 57, 58);
        row = 1 + SUBMASTER_COUNT + 9;
        cfl.setFocus(row, 0);
        assertChannels(-1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1);
        assertLevels(59, 60, 61, 62, 63, 64, 65, 66);
        row = 1 + SUBMASTER_COUNT + 16;
        cfl.setFocus(row, 0);
        assertChannels(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, -1);
        assertLevels(67, 68, 69, 70, 0, 0, 0, 0);
    }

    private void assertSubmasters(final Integer... levelControlIndexes) {
        for (int submasterIndex = 0; submasterIndex < levelControlIndexes.length; submasterIndex++) {
            LevelControl expectedLevelControl = null;
            if (levelControlIndexes[submasterIndex] >= 0) {
                expectedLevelControl = getLevelControl(levelControlIndexes[submasterIndex]);
            }
            LevelControl levelControl = detail.getSubmasterLevel(submasterIndex).getLevelControl();
            assertEquals(levelControl, expectedLevelControl, "submaster " + (submasterIndex + 1));
        }
    }

    private void assertChannels(final Integer... levelControlIndexes) {
        for (int channelIndex = 0; channelIndex < levelControlIndexes.length; channelIndex++) {
            LevelControl expectedLevelControl = null;
            if (levelControlIndexes[channelIndex] >= 0) {
                expectedLevelControl = getLevelControl(levelControlIndexes[channelIndex]);
            }
            LevelControl levelControl = detail.getChannelLevel(channelIndex).getLevelControl();
            assertEquals(levelControl, expectedLevelControl, "channel " + (channelIndex + 1));
        }
    }

    private void assertLevels(final Integer... levels) {
        for (int i = 0; i < levels.length; i++) {
            int level = getLevelControl(i).getLevel();
            int expectedLevel = levels[i].intValue();
            assertEquals(level, expectedLevel, "level[" + i + "]");
        }
    }

    private LevelControl getLevelControl(final int index) {
        return context.getControlHolder().getValue().getLevelControl(index);
    }
}
