package be.lassi.ui.sheet;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import be.lassi.context.ShowContext;
import be.lassi.cues.Cue;
import be.lassi.cues.CueChannelLevel;
import be.lassi.cues.CueDetailFactory;
import be.lassi.cues.CueSubmasterLevel;
import be.lassi.cues.Cues;
import be.lassi.cues.LightCueDetail;
import be.lassi.cues.LightCues;
import be.lassi.cues.Timing;
import be.lassi.domain.Channel;
import be.lassi.domain.Group;
import be.lassi.domain.Groups;
import be.lassi.domain.Level;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;
import be.lassi.domain.Submaster;
import be.lassi.lanbox.domain.Time;
import be.lassi.ui.sheet.cells.CellAction;

/**
 * Tests <code>SheetTableModelDetails</code>.
 */
public class SheetTableModelDetailsTestCase implements TableModelListener {

    private final int NUMBER_OF_CHANNELS = 4;

    private final int NUMBER_OF_SUBMASTERS = 2;

    private ShowContext context;

    private SheetTableModelDetails model;

    private final List<TableModelEvent> events = new ArrayList<TableModelEvent>();

    private LightCueDetail lightCueDetail1;

    private LightCueDetail lightCueDetail2;

    private Group group;

    @BeforeMethod
    public void initializeModel() {
        context = new ShowContext();
        Show show = ShowBuilder.build(NUMBER_OF_CHANNELS, NUMBER_OF_SUBMASTERS, 2, "");
        context.setShow(show);
        model = new SheetTableModelDetails(context);
        model.addTableModelListener(this);
        Cue cue1 = new Cue("1", "", "", "L 1");
        Cue cue2 = new Cue("2", "", "", "L 1");
        CueDetailFactory f = new CueDetailFactory(NUMBER_OF_CHANNELS, NUMBER_OF_SUBMASTERS);
        f.update(cue1);
        f.update(cue2);
        context.getShow().getCues().add(cue1);
        context.getShow().getCues().add(cue2);
        lightCueDetail1 = (LightCueDetail) cue1.getDetail();
        lightCueDetail2 = (LightCueDetail) cue2.getDetail();
        Channel channel2 = context.getShow().getChannels().get(1);
        Channel channel4 = context.getShow().getChannels().get(3);
        group = new Group();
        group.add(channel2);
        group.add(channel4);
        group.setEnabled(false);
        Groups groups = context.getShow().getGroups();
        groups.add(group);
    }

    @Test
    public void initialContents() {
        int expectedRowCount = NUMBER_OF_CHANNELS + NUMBER_OF_SUBMASTERS + 1;
        assertEquals(model.getRowCount(), expectedRowCount);
        assertEquals(model.getColumnCount(), 2);
        assertTrue(model.getValueAt(0, 0) instanceof Timing);
        assertTrue(model.getValueAt(1, 0) instanceof CueSubmasterLevel);
        assertTrue(model.getValueAt(2, 0) instanceof CueSubmasterLevel);
        assertTrue(model.getValueAt(3, 0) instanceof CueChannelLevel);
        assertTrue(model.getValueAt(4, 0) instanceof CueChannelLevel);
        assertTrue(model.getValueAt(5, 0) instanceof CueChannelLevel);
        assertTrue(model.getValueAt(6, 0) instanceof CueChannelLevel);
        lightCueDetail1.getTiming().setWaitTime(Time.TIME_1S);
        lightCueDetail1.getSubmasterLevel(0).getLevelValue().setValue(1f);
        lightCueDetail1.getSubmasterLevel(1).getLevelValue().setValue(0.5f);
        lightCueDetail1.getChannelLevel(0).setChannelValue(0.25f);
        lightCueDetail1.getChannelLevel(1).setChannelValue(0.50f);
        lightCueDetail1.getChannelLevel(2).setChannelValue(0.75f);
        lightCueDetail1.getChannelLevel(3).setChannelValue(1.00f);
        assertEquals(((Timing) model.getValueAt(0, 0)).getWaitTime(), Time.TIME_1S);
        assertEquals(((CueSubmasterLevel) model.getValueAt(1, 0)).getIntValue(), 100);
        assertEquals(((CueSubmasterLevel) model.getValueAt(2, 0)).getIntValue(), 50);
        assertEquals(((CueChannelLevel) model.getValueAt(3, 0)).getChannelIntValue(), 25);
        assertEquals(((CueChannelLevel) model.getValueAt(4, 0)).getChannelIntValue(), 50);
        assertEquals(((CueChannelLevel) model.getValueAt(5, 0)).getChannelIntValue(), 75);
        assertEquals(((CueChannelLevel) model.getValueAt(6, 0)).getChannelIntValue(), 100);
    }

    @Test
    public void testDummyColumn() {
        Cues cues = context.getShow().getCues();
        assertEquals(model.getColumnCount(), 2);
        cues.remove(0);
        assertEquals(model.getColumnCount(), 1);
        cues.remove(0);
        assertEquals(model.getColumnCount(), 1);
        assertTrue(model.isDummyColumnShown());
        assertEquals(model.getValueAt(0, 0), "");
        assertEquals(model.getValueAt(1, 0), "");
        assertEquals(model.getValueAt(2, 0), "");
        assertEquals(model.getValueAt(3, 0), "");
        assertEquals(model.getValueAt(4, 0), "");
        assertEquals(model.getValueAt(5, 0), "");
        assertEquals(model.getValueAt(6, 0), "");
        assertFalse(model.isCellEditable(0, 0));
        assertFalse(model.isCellEditable(1, 0));
        assertFalse(model.isCellEditable(2, 0));
        assertFalse(model.isCellEditable(3, 0));
        assertFalse(model.isCellEditable(4, 0));
        assertFalse(model.isCellEditable(5, 0));
        assertFalse(model.isCellEditable(6, 0));
    }

    @Test
    public void testGetColumnName() {
        Cues cues = context.getShow().getCues();
        assertEquals(model.getColumnName(0), "1");
        assertEquals(model.getColumnName(1), "2");
        cues.remove(0);
        cues.remove(0);
        assertEquals(model.getColumnName(0), "Dummy");
    }

    @Test
    public void testIsCurrentCueColumn() {
        assertFalse(model.isCurrentCueColumn(0));
        assertTrue(model.isCurrentCueColumn(1));
        Cues cues = context.getShow().getCues();
        cues.setCurrent(0);
        assertTrue(model.isCurrentCueColumn(0));
        assertFalse(model.isCurrentCueColumn(1));
        cues.setCurrent(1);
        assertFalse(model.isCurrentCueColumn(0));
        assertTrue(model.isCurrentCueColumn(1));
        cues.remove(0);
        cues.remove(0);
        assertFalse(model.isCurrentCueColumn(0));
    }

    @Test
    public void initialContents_Group() {
        group.setEnabled(true);
        int expectedRowCount = 2 + NUMBER_OF_SUBMASTERS + 1;
        assertEquals(model.getRowCount(), expectedRowCount);
        lightCueDetail1.getTiming().setWaitTime(Time.TIME_1S);
        lightCueDetail1.getSubmasterLevel(0).getLevelValue().setValue(1f);
        lightCueDetail1.getSubmasterLevel(1).getLevelValue().setValue(0.5f);
        lightCueDetail1.getChannelLevel(0).setChannelValue(0.25f);
        lightCueDetail1.getChannelLevel(1).setChannelValue(0.50f);
        lightCueDetail1.getChannelLevel(2).setChannelValue(0.75f);
        lightCueDetail1.getChannelLevel(3).setChannelValue(1.00f);
        assertEquals(((CueChannelLevel) model.getValueAt(3, 0)).getChannelIntValue(), 50);
        assertEquals(((CueChannelLevel) model.getValueAt(4, 0)).getChannelIntValue(), 100);
    }

    @Test
    public void updateTiming() {
        model.setSelected(0, 0, true);
        model.setSelected(0, 1, true);
        model.setValueAt("5", 0, 0);
        assertEquals(lightCueDetail1.getTiming().getFadeInTime(), Time.TIME_5S);
        assertEquals(lightCueDetail2.getTiming().getFadeInTime(), Time.TIME_5S);
    }

    @Test
    public void updateSubmasterLevel() {
        model.setSelected(1, 0, true);
        model.setValueAt("10", 1, 0);
        assertEquals(lightCueDetail1.getSubmasterLevel(0).getIntValue(), 10);
    }

    @Test
    public void updateChannelLevel() {
        model.setSelected(3, 0, true);
        model.setValueAt("10", 3, 0);
        assertEquals(lightCueDetail1.getChannelLevel(0).getChannelIntValue(), 10);
    }

    @Test
    public void updateChannelLevel_Group() {
        group.setEnabled(true);
        model.setSelected(3, 0, true);
        model.setValueAt("10", 3, 0);
        model.setSelected(3, 0, false);
        model.setSelected(4, 0, true);
        model.setValueAt("20", 4, 0);
        assertEquals(lightCueDetail1.getChannelLevel(1).getChannelIntValue(), 10);
        assertEquals(lightCueDetail1.getChannelLevel(3).getChannelIntValue(), 20);
    }

    @Test
    public void resetSubmasterLevel() {
        model.setValueAt("", 1, 1);
        assertTrue(lightCueDetail1.getSubmasterLevel(0).isDerived());
    }

    @Test
    public void resetChannelLevel() {
        lightCueDetail1.getChannelLevel(0).setDerived(true);
        model.setValueAt("", 3, 1);
        assertTrue(lightCueDetail1.getChannelLevel(0).isDerived());
    }

    @Test
    public void resetChannelLevel_Group() {
        lightCueDetail1.getChannelLevel(0).setDerived(false);
        lightCueDetail1.getChannelLevel(1).setDerived(false);
        lightCueDetail1.getChannelLevel(2).setDerived(false);
        lightCueDetail1.getChannelLevel(3).setDerived(false);
        group.setEnabled(true);
        model.setSelected(3, 0, true);
        model.setValueAt("", 3, 0);
        assertFalse(lightCueDetail1.getChannelLevel(3).isDerived());
        assertTrue(lightCueDetail1.getChannelLevel(1).isDerived());
        assertFalse(lightCueDetail1.getChannelLevel(3).isDerived());
        assertFalse(lightCueDetail1.getChannelLevel(3).isDerived());
    }

    @Test
    public void makeChannelCellDerived() {
        model.setSelected(3, 0, true);
        model.setValueAt("10", 3, 0);
        model.setSelected(3, 0, false);
        model.setSelected(3, 1, true);
        model.setValueAt("20", 3, 1);
        CueChannelLevel level = lightCueDetail2.getChannelLevel(0);
        assertFalse(level.isDerived());
        assertEquals(level.getChannelIntValue(), 20);
        model.setValueAt("", 3, 1);
        assertTrue(level.isDerived());
        assertEquals(level.getChannelIntValue(), 10);
    }

    @Test
    public void makeSubmasterCellDerived() {
        Submaster submaster1 = context.getShow().getSubmasters().get(0);
        Level level1 = submaster1.getLevel(0);
        level1.setIntValue(30);
        CueChannelLevel channelLevel = lightCueDetail2.getChannelLevel(0);
        CueSubmasterLevel submasterLevel = lightCueDetail2.getSubmasterLevel(0);
        model.setSelected(1, 0, true);
        model.setValueAt("100", 1, 0);
        model.setSelected(1, 0, false);
        model.setSelected(1, 1, true);
        model.setValueAt("50", 1, 1);
        assertFalse(submasterLevel.isDerived());
        assertEquals(submasterLevel.getIntValue(), 50);
        assertEquals(channelLevel.getSubmasterIntValue(), 15);
        model.setValueAt("", 1, 1);
        assertTrue(submasterLevel.isDerived());
        assertEquals(submasterLevel.getIntValue(), 100);
        assertEquals(channelLevel.getSubmasterIntValue(), 30);
    }

    @Test
    public void makeChannelCellNotDerived() {
        CueChannelLevel level = lightCueDetail2.getChannelLevel(0);
        assertTrue(level.isDerived());
        assertEquals(level.getChannelIntValue(), 0);
        model.setSelected(3, 1, true);
        int[] rows = { 3 };
        int[] cols = { 1 };
        model.keyAction(CellAction.ACTION_SHIFT_UP, rows, cols);
        assertFalse(level.isDerived());
        assertEquals(level.getChannelIntValue(), 10);
    }

    @Test
    public void makeChannelCellNotDerived_Group() {
        group.setEnabled(true);
        model.setSelected(3, 1, true);
        int[] rows = { 3 };
        int[] cols = { 1 };
        model.keyAction(CellAction.ACTION_SHIFT_UP, rows, cols);
        CueChannelLevel level = lightCueDetail2.getChannelLevel(1);
        assertFalse(level.isDerived());
        assertEquals(level.getChannelIntValue(), 10);
    }

    @Test
    public void makeSubmasterCellNotDerived() {
        CueSubmasterLevel submasterLevel = lightCueDetail2.getSubmasterLevel(0);
        assertTrue(submasterLevel.isDerived());
        assertEquals(submasterLevel.getIntValue(), 0);
        model.setSelected(1, 1, true);
        int[] rows = { 1 };
        int[] cols = { 1 };
        model.keyAction(CellAction.ACTION_SHIFT_UP, rows, cols);
        assertFalse(submasterLevel.isDerived());
        assertEquals(submasterLevel.getIntValue(), 10);
    }

    public void tableChanged(final TableModelEvent e) {
        events.add(e);
    }

    @Test
    public void getRowTextTimingRow() {
        assertEquals(model.getRowText(0), "");
    }

    @Test
    public void getRowTextSubmasterRow() {
        assertEquals(model.getRowText(1), "[S1] Submaster 1");
        assertEquals(model.getRowText(2), "[S2] Submaster 2");
    }

    @Test
    public void getRowTextChannelRow() {
        assertEquals(model.getRowText(3), "[1] Channel 1");
        assertEquals(model.getRowText(4), "[2] Channel 2");
        assertEquals(model.getRowText(5), "[3] Channel 3");
        assertEquals(model.getRowText(6), "[4] Channel 4");
        group.setEnabled(true);
        assertEquals(model.getRowText(3), "[2] Channel 2");
        assertEquals(model.getRowText(4), "[4] Channel 4");
    }

    @Test
    public void scrollNothing() {
        model.scroll(1);
        initialContents();
    }

    @Test
    public void scrollTimingCells() {
    }

    @Test
    public void scrollSubmasterCells() {
        model.setSelected(1, 0, true);
        model.setSelected(2, 1, true);
        assertSubmasterLevel(0, 0, 0);
        assertSubmasterLevel(0, 0, 1);
        assertSubmasterLevel(0, 1, 0);
        assertSubmasterLevel(0, 1, 1);
        model.scroll(5);
        assertSubmasterLevel(5, 0, 0);
        assertSubmasterLevel(0, 0, 1);
        assertSubmasterLevel(5, 1, 0);
        assertSubmasterLevel(5, 1, 1);
        model.scroll(-3);
        assertSubmasterLevel(2, 0, 0);
        assertSubmasterLevel(0, 0, 1);
        assertSubmasterLevel(2, 1, 0);
        assertSubmasterLevel(2, 1, 1);
    }

    @Test
    public void scrollChannelsCells() {
        model.setSelected(3, 0, true);
        model.setSelected(4, 1, true);
        assertChannelLevel(0, 0, 0);
        assertChannelLevel(0, 0, 1);
        assertChannelLevel(0, 1, 0);
        assertChannelLevel(0, 1, 1);
        model.scroll(5);
        assertChannelLevel(5, 0, 0);
        assertChannelLevel(0, 0, 1);
        assertChannelLevel(5, 1, 0);
        assertChannelLevel(5, 1, 1);
        model.scroll(-3);
        assertChannelLevel(2, 0, 0);
        assertChannelLevel(0, 0, 1);
        assertChannelLevel(2, 1, 0);
        assertChannelLevel(2, 1, 1);
    }

    private void assertSubmasterLevel(final int expectedLevel, final int cueIndex, final int submasterIndex) {
        LightCueDetail detail = getDetail(cueIndex);
        int level = detail.getSubmasterLevel(submasterIndex).getIntValue();
        assertEquals(level, expectedLevel);
    }

    private void assertChannelLevel(final int expectedLevel, final int cueIndex, final int channelIndex) {
        LightCueDetail detail = getDetail(cueIndex);
        int level = detail.getChannelLevel(channelIndex).getChannelIntValue();
        assertEquals(level, expectedLevel);
    }

    private LightCueDetail getDetail(final int cueIndex) {
        LightCues cues = context.getShow().getCues().getLightCues();
        LightCueDetail detail = cues.getDetail(cueIndex);
        return detail;
    }
}
