package be.lassi.ui.sheet;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import be.lassi.base.NameListener;
import be.lassi.base.NamedObject;
import be.lassi.context.ShowContext;
import be.lassi.context.ShowContextListener;
import be.lassi.cues.LightCues;
import be.lassi.domain.Channel;
import be.lassi.domain.ChannelLevelListener;
import be.lassi.domain.Controlable;
import be.lassi.domain.Level;
import be.lassi.domain.Submaster;
import be.lassi.lanbox.domain.Buffer;
import be.lassi.ui.sheet.cells.CellAction;
import be.lassi.ui.sheet.cells.EmptyCell;
import be.lassi.ui.sheet.cells.LevelBar;
import be.lassi.util.NLS;
import be.lassi.util.Util;

/**
 * Table model for left side of Sheet window table.
 */
public class SheetTableModelHeaders extends SheetTableModel {

    public static final int COLUMN_ID = 0;

    public static final int COLUMN_NAME = 1;

    public static final int COLUMN_STAGE_BAR = 2;

    public static final int COLUMN_STAGE = 3;

    public static final int COLUMN_SUBMASTER = 4;

    private final String[] columnNames = { NLS.get("sheet.column.id"), NLS.get("sheet.column.description"), NLS.get("sheet.column.stageBar"), NLS.get("sheet.column.stage"), NLS.get("sheet.column.submaster") };

    private final EmptyCell emptyCell = new EmptyCell();

    private int selectedSubmasterIndex = -1;

    private final ChannelNameListener channelNameListener = new ChannelNameListener();

    private final SubmasterNameListener submasterNameListener = new SubmasterNameListener();

    private final ShowContextListener showContextListener = new MyShowContextListener();

    /**
     * Constructs a new instance.
     *
     * @param context the show context
     */
    public SheetTableModelHeaders(final ShowContext context) {
        super(context);
        context.addShowContextListener(showContextListener);
        showContextListener.postShowChange();
        listenForChannelLevelChanges();
        PropertyChangeListener listener = new PreferencesChangeListener();
        SheetPreferences preferences = getContext().getPreferences().getSheetPreferences();
        preferences.addPropertyChangeListener(SheetPreferences.COLUMN_ID_ENABLED, listener);
        preferences.addPropertyChangeListener(SheetPreferences.COLUMN_NAME_ENABLED, listener);
        preferences.addPropertyChangeListener(SheetPreferences.COLUMN_STAGE_BAR_ENABLED, listener);
        preferences.addPropertyChangeListener(SheetPreferences.COLUMN_STAGE_ENABLED, listener);
        preferences.addPropertyChangeListener(SheetPreferences.COLUMN_SUBMASTER_ENABLED, listener);
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount() {
        int columnCount = 0;
        SheetPreferences preferences = getContext().getPreferences().getSheetPreferences();
        if (preferences.isColumnIdEnabled()) {
            columnCount++;
        }
        if (preferences.isColumnNameEnabled()) {
            columnCount++;
        }
        if (preferences.isColumnStageBarEnabled()) {
            columnCount++;
        }
        if (preferences.isColumnStageEnabled()) {
            columnCount++;
        }
        if (preferences.isColumnSubmasterEnabled()) {
            columnCount++;
        }
        return columnCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(final int col) {
        int columnIndex = getColumnIndex(col);
        return columnNames[columnIndex];
    }

    /**
     * {@inheritDoc}
     */
    public Object getValueAt(final int row, final int col) {
        Object value = "?";
        int columnIndex = getColumnIndex(col);
        if (isRowTiming(row)) {
            value = "";
        } else {
            if (isRowSubmaster(row)) {
                int submasterIndex = getSubmasterIndex(row);
                value = getValueSubmasterAt(submasterIndex, columnIndex);
            } else {
                int channelIndex = getChannelIndex(row);
                value = getValueChannelAt(channelIndex, columnIndex);
            }
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int row, final int col) {
        boolean result = true;
        int columnIndex = getColumnIndex(col);
        if (columnIndex == COLUMN_ID || columnIndex == COLUMN_STAGE) {
            result = false;
        } else {
            if (isRowTiming(row)) {
                result = false;
            } else {
                if (columnIndex == COLUMN_SUBMASTER) {
                    if (isRowSubmaster(row)) {
                        result = false;
                    } else {
                        if (selectedSubmasterIndex == -1) {
                            result = false;
                        }
                    }
                } else if (columnIndex == COLUMN_STAGE_BAR) {
                    if (isRowSubmaster(row)) {
                        result = false;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean isColumnWithSpecialGridLine(final int col) {
        int columnIndex = getColumnIndex(col);
        return columnIndex == COLUMN_SUBMASTER;
    }

    /**
     * Indicates whether the row with given index is the row with
     * the currently selected submaster.
     *
     * @param row the row index (first row is 0)
     * @return boolean true if row contains selected submaster
     */
    public boolean isRowSelectedSubmaster(final int row) {
        boolean result = false;
        if (selectedSubmasterIndex != -1) {
            result = row == getRowSubmaster(selectedSubmasterIndex);
        }
        return result;
    }

    /**
     *
     *
     */
    @Override
    public boolean keyAction(final int actionId, final int[] rows, final int[] cols) {
        boolean actionPerformed = false;
        for (int row : rows) {
            for (int col : cols) {
                if (isRowChannel(row)) {
                    if (col == COLUMN_SUBMASTER) {
                        if (selectedSubmasterIndex != -1) {
                            keyActionSubmasterLevel(actionId, row, col);
                            actionPerformed = true;
                        }
                    }
                }
            }
        }
        return actionPerformed;
    }

    /**
     * Method setSelectedSubmasterIndex.
     *
     * @param submasterIndex
     */
    public void setSelectedSubmasterIndex(final int submasterIndex) {
        if (submasterIndex != selectedSubmasterIndex) {
            for (int row = 0; row < getRowCount(); row++) {
                fireTableCellUpdated(row, COLUMN_SUBMASTER);
            }
            fireTableRowUpdated(getRowSubmaster(selectedSubmasterIndex));
            selectedSubmasterIndex = submasterIndex;
            fireTableRowUpdated(getRowSubmaster(selectedSubmasterIndex));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object value, final int row, final int col) {
        int columnIndex = getColumnIndex(col);
        if (isRowSubmaster(row)) {
            setValueSubmasterAt(value, getSubmasterIndex(row), columnIndex);
        } else {
            setValueChannelAt(value, getChannelIndex(row), columnIndex);
        }
    }

    private Submaster getSelectedSubmaster() {
        return getShow().getSubmasters().get(selectedSubmasterIndex);
    }

    private Object getValueChannelAt(final int channelIndex, final int col) {
        Object value = "?";
        Channel channel = getShow().getChannels().get(channelIndex);
        switch(col) {
            case COLUMN_ID:
                value = buildLabel(channel);
                break;
            case COLUMN_NAME:
                value = channel.getName();
                break;
            case COLUMN_STAGE_BAR:
                Level level = getChannelLevel(channelIndex);
                value = new LevelBar(level);
                break;
            case COLUMN_STAGE:
                value = getChannelLevel(channelIndex);
                break;
            case COLUMN_SUBMASTER:
                value = emptyCell;
                if (selectedSubmasterIndex != -1) {
                    value = getSelectedSubmaster().getLevel(channelIndex);
                }
                break;
            default:
        }
        return value;
    }

    private Object getValueSubmasterAt(final int subMasterIndex, final int col) {
        Object value = "?";
        Submaster submaster = getShow().getSubmasters().get(subMasterIndex);
        switch(col) {
            case COLUMN_ID:
                value = buildLabel(submaster);
                break;
            case COLUMN_NAME:
                value = submaster.getName();
                break;
            case COLUMN_STAGE_BAR:
                value = "";
                break;
            case COLUMN_STAGE:
                value = "";
                break;
            case COLUMN_SUBMASTER:
                value = "";
                break;
            default:
        }
        return value;
    }

    private void keyActionSubmasterLevel(final int actionId, final int row, final int col) {
        int channelIndex = getChannelIndex(row);
        Level level = getSelectedSubmaster().getLevel(channelIndex);
        int value = level.getIntValue();
        value = CellAction.getLevelValue(actionId, value);
        level.setIntValue(value);
        level.setActive(true);
        updateSelectedSubmasterLevel(channelIndex, level.getValue());
        fireTableCellUpdated(row, col);
    }

    private void updateSelectedSubmasterLevel(final int channelIndex, final float value) {
        if (selectedSubmasterIndex != -1) {
            LightCues lightCues = getShow().getCues().getLightCues();
            lightCues.setSubmasterChannel(selectedSubmasterIndex, channelIndex, value);
        }
    }

    private void listenForChannelLevelChanges() {
        Buffer buffer = getContext().getLanbox().getMixer();
        int levelCount = buffer.getLevels().size();
        for (int i = 0; i < levelCount; i++) {
            ChannelLevelListener changeListener = new ChannelLevelListener(i) {

                @Override
                public void levelChanged(final int channelId) {
                    int row = getRowChannelWithId(channelId);
                    fireTableCellUpdated(row, COLUMN_STAGE);
                    fireTableCellUpdated(row, COLUMN_STAGE_BAR);
                }
            };
            Level level = buffer.getLevels().get(i);
            level.add(changeListener);
        }
    }

    private void setValueChannelAt(final Object value, final int channelIndex, final int col) {
        if (col == COLUMN_NAME) {
            getShow().getChannels().get(channelIndex).setName((String) value);
        } else if (col == COLUMN_SUBMASTER) {
            if (selectedSubmasterIndex != -1) {
                LightCues lightCues = getShow().getCues().getLightCues();
                String stringValue = (String) value;
                stringValue = stringValue.trim();
                if ("-".equals(stringValue)) {
                    lightCues.deactivateSubmasterLevel(selectedSubmasterIndex, channelIndex);
                } else {
                    float floatValue = 0f;
                    if (stringValue.length() > 0) {
                        int intValue = Util.toInt(stringValue);
                        floatValue = (((float) intValue) / 100) + 0.001f;
                    }
                    lightCues.setSubmasterChannel(selectedSubmasterIndex, channelIndex, floatValue);
                }
            }
        }
    }

    private void setValueSubmasterAt(final Object value, final int submasterIndex, final int col) {
        Submaster submaster = getShow().getSubmasters().get(submasterIndex);
        if (col == COLUMN_NAME) {
            submaster.setName((String) value);
        }
    }

    private Level getChannelLevel(final int channelIndex) {
        Buffer buffer = getContext().getLanbox().getMixer();
        return buffer.getLevels().get(channelIndex);
    }

    private int getColumnIndex(final int col) {
        int result = -1;
        int columnCount = 0;
        SheetPreferences preferences = getContext().getPreferences().getSheetPreferences();
        if (preferences.isColumnIdEnabled()) {
            if (col == columnCount) {
                result = COLUMN_ID;
            }
            columnCount++;
        }
        if (result == -1 && preferences.isColumnNameEnabled()) {
            if (col == columnCount) {
                result = COLUMN_NAME;
            }
            columnCount++;
        }
        if (result == -1 && preferences.isColumnStageBarEnabled()) {
            if (col == columnCount) {
                result = COLUMN_STAGE_BAR;
            }
            columnCount++;
        }
        if (result == -1 && preferences.isColumnStageEnabled()) {
            if (col == columnCount) {
                result = COLUMN_STAGE;
            }
            columnCount++;
        }
        if (result == -1 && preferences.isColumnSubmasterEnabled()) {
            if (col == columnCount) {
                result = COLUMN_SUBMASTER;
            }
            columnCount++;
        }
        return result;
    }

    private class PreferencesChangeListener implements PropertyChangeListener {

        public void propertyChange(final PropertyChangeEvent evt) {
            fireTableStructureChanged();
            updateColumnWidths();
        }
    }

    private class ChannelNameListener implements NameListener {

        public void nameChanged(final NamedObject object) {
            fireTableDataChanged();
        }
    }

    private class SubmasterNameListener implements NameListener {

        public void nameChanged(final NamedObject object) {
            fireTableDataChanged();
        }
    }

    public void updateColumnWidths() {
        int columnCount = getColumnModel().getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            int columnIndex = getColumnIndex(i);
            int width = 55;
            if (columnIndex == COLUMN_NAME) {
                width = 150;
            }
            getColumnModel().getColumn(i).setMinWidth(width);
            getColumnModel().getColumn(i).setPreferredWidth(width);
        }
    }

    private class MyShowContextListener implements ShowContextListener {

        public void postShowChange() {
            for (Channel channel : getShow().getChannels()) {
                channel.addNameListener(channelNameListener);
            }
            for (int i = 0; i < getShow().getNumberOfSubmasters(); i++) {
                Submaster submaster = getShow().getSubmasters().get(i);
                submaster.addNameListener(submasterNameListener);
            }
            fireTableDataChanged();
        }

        public void preShowChange() {
            for (Channel channel : getShow().getChannels()) {
                channel.removeNameListener(channelNameListener);
            }
            for (int i = 0; i < getShow().getNumberOfSubmasters(); i++) {
                Submaster submaster = getShow().getSubmasters().get(i);
                submaster.removeNameListener(submasterNameListener);
            }
        }
    }

    private SheetLabel buildLabel(final Controlable controlable) {
        String text = controlable.getLabel();
        SheetLabel label = new SheetLabel(text);
        label.setControlled(controlable.isControlled());
        label.setSheetGroupId(controlable.getSheetGroupId());
        return label;
    }
}
