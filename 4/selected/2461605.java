package be.lassi.ui.sheet;

import static be.lassi.ui.sheet.SheetTableModelHeaders.COLUMN_ID;
import static be.lassi.ui.sheet.SheetTableModelHeaders.COLUMN_NAME;
import static be.lassi.ui.sheet.SheetTableModelHeaders.COLUMN_STAGE;
import static be.lassi.ui.sheet.SheetTableModelHeaders.COLUMN_STAGE_BAR;
import static be.lassi.ui.sheet.SheetTableModelHeaders.COLUMN_SUBMASTER;
import static org.junit.Assert.assertArrayEquals;
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
import be.lassi.domain.Level;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;
import be.lassi.domain.Submaster;
import be.lassi.lanbox.domain.Buffer;
import be.lassi.ui.sheet.cells.CellAction;
import be.lassi.ui.sheet.cells.EmptyCell;
import be.lassi.ui.sheet.cells.LevelBar;

/**
 * Tests class <code>SheetTableModelHeaders</code>.
 */
public class SheetTableModelHeadersTestCase implements TableModelListener {

    private final int NUMBER_OF_CHANNELS = 4;

    private final int NUMBER_OF_SUBMASTERS = 2;

    private ShowContext context;

    private SheetTableModelHeaders model;

    private List<TableModelEvent> events;

    @BeforeMethod
    public void initializeModel() {
        context = new ShowContext();
        Show show = ShowBuilder.build(NUMBER_OF_CHANNELS, NUMBER_OF_SUBMASTERS, 2, "");
        context.setShow(show);
        model = new SheetTableModelHeaders(context);
        model.addTableModelListener(this);
        events = new ArrayList<TableModelEvent>();
    }

    @Test
    public void initialContents() {
        int expectedRowCount = NUMBER_OF_CHANNELS + NUMBER_OF_SUBMASTERS + 1;
        assertEquals(model.getRowCount(), expectedRowCount);
        assertEquals(model.getValueAt(0, COLUMN_ID), "");
        assertEquals(model.getValueAt(0, COLUMN_NAME), "");
        assertEquals(model.getValueAt(0, COLUMN_SUBMASTER), "");
        assertEquals(model.getValueAt(0, COLUMN_STAGE), "");
        assertEquals(model.getValueAt(0, COLUMN_STAGE_BAR), "");
        SheetLabel label = (SheetLabel) model.getValueAt(1, COLUMN_ID);
        assertEquals(label.getText(), "S1");
        assertEquals(model.getValueAt(1, COLUMN_NAME), "Submaster 1");
        assertEquals(model.getValueAt(1, COLUMN_SUBMASTER), "");
        assertEquals(model.getValueAt(1, COLUMN_STAGE), "");
        assertEquals(model.getValueAt(1, COLUMN_STAGE_BAR), "");
        label = (SheetLabel) model.getValueAt(2, COLUMN_ID);
        assertEquals(label.getText(), "S2");
        assertEquals(model.getValueAt(2, COLUMN_NAME), "Submaster 2");
        assertEquals(model.getValueAt(2, COLUMN_SUBMASTER), "");
        assertEquals(model.getValueAt(2, COLUMN_STAGE), "");
        assertEquals(model.getValueAt(2, COLUMN_STAGE_BAR), "");
        label = (SheetLabel) model.getValueAt(3, COLUMN_ID);
        assertEquals(label.getText(), "1");
        assertEquals(model.getValueAt(3, COLUMN_NAME), "Channel 1");
        assertTrue(model.getValueAt(3, COLUMN_SUBMASTER) instanceof EmptyCell);
        Level level = (Level) model.getValueAt(3, COLUMN_STAGE);
        LevelBar levelBar = (LevelBar) model.getValueAt(3, COLUMN_STAGE_BAR);
        assertEquals(level.getIntValue(), 0);
        assertEquals(levelBar.getValue(), 0f);
        label = (SheetLabel) model.getValueAt(6, COLUMN_ID);
        assertEquals(label.getText(), "4");
        assertEquals(model.getValueAt(6, COLUMN_NAME), "Channel 4");
        assertTrue(model.getValueAt(6, COLUMN_SUBMASTER) instanceof EmptyCell);
        assertTrue(model.getValueAt(6, COLUMN_STAGE) instanceof Level);
        level = (Level) model.getValueAt(6, COLUMN_STAGE);
        levelBar = (LevelBar) model.getValueAt(6, COLUMN_STAGE_BAR);
        assertEquals(level.getIntValue(), 0);
        assertEquals(levelBar.getValue(), 0f);
    }

    @Test
    public void submasterLabel() {
        Submaster firstSubmaster = context.getShow().getSubmasters().get(0);
        SheetLabel label = (SheetLabel) model.getValueAt(1, COLUMN_ID);
        assertEquals(label.getText(), "S1");
        firstSubmaster.getLevel(0).setIntValue(10);
        firstSubmaster.getLevel(3).setIntValue(20);
        label = (SheetLabel) model.getValueAt(1, COLUMN_ID);
        assertEquals(label.getText(), "S1*");
    }

    @Test
    public void selectSubmaster() {
        Submaster firstSubmaster = context.getShow().getSubmasters().get(0);
        firstSubmaster.getLevel(0).setIntValue(10);
        firstSubmaster.getLevel(3).setIntValue(40);
        assertTrue(model.getValueAt(3, COLUMN_SUBMASTER) instanceof EmptyCell);
        assertTrue(model.getValueAt(6, COLUMN_SUBMASTER) instanceof EmptyCell);
        model.setSelectedSubmasterIndex(0);
        assertEquals(((Level) model.getValueAt(3, COLUMN_SUBMASTER)).getIntValue(), 10);
        assertEquals(((Level) model.getValueAt(6, COLUMN_SUBMASTER)).getIntValue(), 40);
        model.setSelectedSubmasterIndex(1);
        assertEquals(((Level) model.getValueAt(3, COLUMN_SUBMASTER)).getIntValue(), 0);
        assertEquals(((Level) model.getValueAt(6, COLUMN_SUBMASTER)).getIntValue(), 0);
        model.setSelectedSubmasterIndex(1);
    }

    @Test
    public void cellEditable() {
        assertFalse(model.isCellEditable(0, COLUMN_ID));
        assertFalse(model.isCellEditable(1, COLUMN_ID));
        assertFalse(model.isCellEditable(2, COLUMN_ID));
        assertFalse(model.isCellEditable(3, COLUMN_ID));
        assertFalse(model.isCellEditable(4, COLUMN_ID));
        assertFalse(model.isCellEditable(5, COLUMN_ID));
        assertFalse(model.isCellEditable(6, COLUMN_ID));
        assertFalse(model.isCellEditable(0, COLUMN_NAME));
        assertTrue(model.isCellEditable(1, COLUMN_NAME));
        assertTrue(model.isCellEditable(2, COLUMN_NAME));
        assertTrue(model.isCellEditable(3, COLUMN_NAME));
        assertTrue(model.isCellEditable(4, COLUMN_NAME));
        assertTrue(model.isCellEditable(5, COLUMN_NAME));
        assertTrue(model.isCellEditable(6, COLUMN_NAME));
        assertFalse(model.isCellEditable(0, COLUMN_SUBMASTER));
        assertFalse(model.isCellEditable(1, COLUMN_SUBMASTER));
        assertFalse(model.isCellEditable(2, COLUMN_SUBMASTER));
        assertFalse(model.isCellEditable(3, COLUMN_SUBMASTER));
        assertFalse(model.isCellEditable(4, COLUMN_SUBMASTER));
        assertFalse(model.isCellEditable(5, COLUMN_SUBMASTER));
        assertFalse(model.isCellEditable(6, COLUMN_SUBMASTER));
        assertFalse(model.isCellEditable(0, COLUMN_STAGE));
        assertFalse(model.isCellEditable(1, COLUMN_STAGE));
        assertFalse(model.isCellEditable(2, COLUMN_STAGE));
        assertFalse(model.isCellEditable(3, COLUMN_STAGE));
        assertFalse(model.isCellEditable(4, COLUMN_STAGE));
        assertFalse(model.isCellEditable(5, COLUMN_STAGE));
        assertFalse(model.isCellEditable(6, COLUMN_STAGE));
        model.setSelectedSubmasterIndex(0);
        assertFalse(model.isCellEditable(0, COLUMN_SUBMASTER));
        assertFalse(model.isCellEditable(1, COLUMN_SUBMASTER));
        assertFalse(model.isCellEditable(2, COLUMN_SUBMASTER));
        assertTrue(model.isCellEditable(3, COLUMN_SUBMASTER));
        assertTrue(model.isCellEditable(4, COLUMN_SUBMASTER));
        assertTrue(model.isCellEditable(5, COLUMN_SUBMASTER));
        assertTrue(model.isCellEditable(6, COLUMN_SUBMASTER));
        assertFalse(model.isCellEditable(0, COLUMN_STAGE_BAR));
        assertFalse(model.isCellEditable(1, COLUMN_STAGE_BAR));
        assertFalse(model.isCellEditable(2, COLUMN_STAGE_BAR));
        assertTrue(model.isCellEditable(3, COLUMN_STAGE_BAR));
        assertTrue(model.isCellEditable(4, COLUMN_STAGE_BAR));
        assertTrue(model.isCellEditable(5, COLUMN_STAGE_BAR));
        assertTrue(model.isCellEditable(6, COLUMN_STAGE_BAR));
    }

    @Test
    public void isRowSelectedSubmaster() {
        assertFalse(model.isRowSelectedSubmaster(1));
        model.setSelectedSubmasterIndex(0);
        assertTrue(model.isRowSelectedSubmaster(1));
    }

    @Test
    public void changedChannelName() {
        context.getShow().getChannels().get(0).setName("New");
        assertEquals(model.getValueAt(3, COLUMN_NAME), "New");
        assertEquals(events.get(0).getColumn(), TableModelEvent.ALL_COLUMNS);
    }

    @Test
    public void changedSubmasterName() {
        context.getShow().getSubmasters().get(0).setName("New");
        assertEquals(model.getValueAt(1, COLUMN_NAME), "New");
        assertEquals(events.get(0).getColumn(), TableModelEvent.ALL_COLUMNS);
    }

    @Test
    public void changedChannelLevel() {
        int[] values = { 255, 127, 255, 127 };
        Buffer buffer = context.getLanbox().getMixer();
        buffer.setLevelValues(values);
        assertEquals(((Level) model.getValueAt(3, COLUMN_STAGE)).getIntValue(), 100);
        assertEquals(events.get(0).getColumn(), 3);
    }

    @Test
    public void setChannelName() {
        model.setValueAt("New", 3, COLUMN_NAME);
        assertEquals(context.getShow().getChannels().get(0).getName(), "New");
    }

    @Test
    public void setSubmasterName() {
        model.setValueAt("New", 1, COLUMN_NAME);
        assertEquals(context.getShow().getSubmasters().get(0).getName(), "New");
    }

    @Test
    public void setSubmasterLevel() {
        model.setSelectedSubmasterIndex(0);
        model.setValueAt("10", 3, COLUMN_SUBMASTER);
        assertEquals(context.getShow().getSubmasters().get(0).getLevel(0).getIntValue(), 10);
    }

    @Test
    public void keyAction() {
        model.setSelectedSubmasterIndex(0);
        int[] rows = { 3 };
        int[] cols = { COLUMN_SUBMASTER };
        boolean actionPerformed = model.keyAction(CellAction.ACTION_SHIFT_UP, rows, cols);
        assertTrue(actionPerformed);
        assertEquals(context.getShow().getSubmasters().get(0).getLevel(0).getIntValue(), 10);
    }

    @Test
    public void keyActionNoSubmasterSelected() {
        int[] rows = { 3 };
        int[] cols = { COLUMN_SUBMASTER };
        boolean actionPerformed = model.keyAction(CellAction.ACTION_SHIFT_UP, rows, cols);
        assertFalse(actionPerformed);
    }

    @Test
    public void keyActionTimingRow() {
        int[] rows = { 0 };
        int[] cols = { COLUMN_SUBMASTER };
        boolean actionPerformed = model.keyAction(CellAction.ACTION_SHIFT_UP, rows, cols);
        assertFalse(actionPerformed);
    }

    @Test
    public void keyActionReadOnlyColumn() {
        int[] rows = { 3 };
        int[] cols = { COLUMN_ID };
        boolean actionPerformed = model.keyAction(CellAction.ACTION_SHIFT_UP, rows, cols);
        assertFalse(actionPerformed);
    }

    @Test
    public void getColumnCount() {
        SheetPreferences preferences = context.getPreferences().getSheetPreferences();
        assertEquals(model.getColumnCount(), 5);
        preferences.setColumnIdEnabled(false);
        assertEquals(model.getColumnCount(), 4);
        preferences.setColumnNameEnabled(false);
        assertEquals(model.getColumnCount(), 3);
        preferences.setColumnStageBarEnabled(false);
        assertEquals(model.getColumnCount(), 2);
        preferences.setColumnStageEnabled(false);
        assertEquals(model.getColumnCount(), 1);
        preferences.setColumnSubmasterEnabled(false);
        assertEquals(model.getColumnCount(), 0);
    }

    @Test
    public void getColumnName() {
        SheetPreferences preferences = context.getPreferences().getSheetPreferences();
        String[] expected1 = { "Id", "Description", "Stage", "%", "SUB" };
        assertColumnNames(expected1);
        preferences.setColumnNameEnabled(false);
        String[] expected2 = { "Id", "Stage", "%", "SUB" };
        assertColumnNames(expected2);
        preferences.setColumnStageEnabled(false);
        String[] expected3 = { "Id", "Stage", "SUB" };
        assertColumnNames(expected3);
        preferences.setColumnIdEnabled(false);
        String[] expected4 = { "Stage", "SUB" };
        assertColumnNames(expected4);
        preferences.setColumnStageBarEnabled(false);
        String[] expected5 = { "SUB" };
        assertColumnNames(expected5);
        preferences.setColumnSubmasterEnabled(false);
        String[] expected6 = {};
        assertColumnNames(expected6);
    }

    private void assertColumnNames(final String[] expectedColumnNames) {
        String[] columnNames = new String[model.getColumnCount()];
        for (int i = 0; i < columnNames.length; i++) {
            columnNames[i] = model.getColumnName(i);
        }
        assertArrayEquals(expectedColumnNames, columnNames);
    }

    public void tableChanged(final TableModelEvent e) {
        events.add(e);
    }
}
