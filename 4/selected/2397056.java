package be.lassi.ui.sheet;

import be.lassi.context.ShowContext;
import be.lassi.cues.Cue;
import be.lassi.cues.CueChannelLevel;
import be.lassi.cues.CueSubmasterLevel;
import be.lassi.cues.LightCueDetail;
import be.lassi.cues.Timing;
import be.lassi.domain.Channel;
import be.lassi.domain.Submaster;
import be.lassi.lanbox.domain.Time;
import be.lassi.ui.sheet.cells.CellAction;
import be.lassi.ui.sheet.cells.CellFocusListener;
import be.lassi.ui.sheet.parse.ParserLevel;
import be.lassi.ui.sheet.parse.ParserTiming;
import be.lassi.ui.util.table.SelectableTableModel;

/**
 *
 *
 *
 */
public class SheetTableModelDetails extends SheetTableModel implements SelectableTableModel {

    /**
     * Constructs a new instance.
     *
     * @param context the show context
     */
    public SheetTableModelDetails(final ShowContext context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount() {
        int columnCount = getCues().size();
        if (columnCount == 0) {
            columnCount = 1;
        }
        return columnCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(final int col) {
        String name = "Dummy";
        if (!isDummyColumn(col)) {
            name = getCue(col).getNumber();
        }
        return name;
    }

    /**
     * Get the cue at given column index.
     *
     * @param col the column index
     * @return the Cue at given index
     */
    public Cue getCue(final int col) {
        return getCues().get(col);
    }

    /**
     * Gets the light cue detail of the cue at given column index.
     *
     * @param col the column index
     * @return the light cue detail
     */
    public LightCueDetail getDetail(final int col) {
        return (LightCueDetail) getCue(col).getDetail();
    }

    /**
     * {@inheritDoc}
     */
    public Object getValueAt(final int row, final int col) {
        Object value = "";
        if (!isDummyColumn(col)) {
            if (isRowTiming(row)) {
                value = getDetail(col).getTiming();
            } else {
                LightCueDetail detail = getDetail(col);
                if (isRowSubmaster(row)) {
                    value = detail.getSubmasterLevel(getSubmasterIndex(row));
                } else {
                    value = detail.getChannelLevel(getChannelIndex(row));
                }
            }
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int row, final int col) {
        return !isDummyColumn(col);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object value, final int row, final int col) {
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getColumnCount(); j++) {
                if (isSelected(i, j)) {
                    doSetValueAt(value, i, j);
                }
            }
        }
    }

    private void doSetValueAt(final Object value, final int row, final int col) {
        if (isRowTiming(row)) {
            Timing timing = new Timing();
            ParserTiming.parse((String) value, timing);
            getDetail(col).setTiming(timing);
            getCue(col).setDescriptionTemp("L " + value);
        } else {
            setValueLevelAt((String) value, row, col);
        }
    }

    private void setValueLevelAt(final String valueString, final int row, final int col) {
        float levelValue = 0f;
        String string = valueString.trim();
        if (string.length() == 0) {
            resetLevelValueAt(row, col);
        } else if ("-".equals(string)) {
            deactivateLevelValueAt(row, col);
        } else {
            levelValue = ParserLevel.parse(string);
            setLevelValueAt(levelValue, row, col);
        }
    }

    private void resetLevelValueAt(final int row, final int col) {
        if (isRowSubmaster(row)) {
            int submasterIndex = getSubmasterIndex(row);
            getCues().resetSubmaster(getCueIndex(col), submasterIndex);
        } else {
            int channelIndex = getChannelIndex(row);
            getCues().resetChannel(getCueIndex(col), channelIndex);
        }
    }

    private void deactivateLevelValueAt(final int row, final int col) {
        if (isRowSubmaster(row)) {
            int submasterIndex = getSubmasterIndex(row);
            getCues().deactivateCueSubmaster(getCueIndex(col), submasterIndex);
        } else {
            int channelIndex = getChannelIndex(row);
            getCues().deactivateChannel(getCueIndex(col), channelIndex);
        }
    }

    private void setLevelValueAt(final float levelValue, final int row, final int col) {
        if (isRowSubmaster(row)) {
            int submasterIndex = getSubmasterIndex(row);
            getCues().setCueSubmaster(getCueIndex(col), submasterIndex, levelValue);
        } else {
            int channelIndex = getChannelIndex(row);
            getCues().setChannel(getCueIndex(col), channelIndex, levelValue);
        }
    }

    @Override
    public boolean keyAction(final int actionId, final int[] rows, final int[] cols) {
        boolean actionPerformed = false;
        for (int row = 0; row < getRowCount(); row++) {
            for (int column = 0; column < getColumnCount(); column++) {
                if (isSelected(row, column)) {
                    if (isRowTiming(row)) {
                    } else {
                        LightCueDetail cue = getDetail(column);
                        if (isRowSubmaster(row)) {
                            CueSubmasterLevel l = cue.getSubmasterLevel(getSubmasterIndex(row));
                            int oldValue = l.getIntValue();
                            int newValue = CellAction.getLevelValue(actionId, oldValue);
                            getCues().setCueSubmaster(getCueIndex(column), getSubmasterIndex(row), newValue / 100f);
                        } else {
                            CueChannelLevel l = cue.getChannelLevel(getChannelIndex(row));
                            int oldValue = l.getChannelIntValue();
                            int newValue = CellAction.getLevelValue(actionId, oldValue);
                            getCues().setChannel(getCueIndex(column), getChannelIndex(row), newValue / 100f);
                        }
                        fireTableCellUpdated(row, column);
                        actionPerformed = true;
                    }
                }
            }
        }
        return actionPerformed;
    }

    private int getCueIndex(final int col) {
        return col;
    }

    @Override
    public boolean isCurrentCueColumn(final int col) {
        boolean current = false;
        if (!isDummyColumn(col)) {
            current = getCue(col).isCurrent();
        }
        return current;
    }

    /**
     * Indicates whether given column is the dummy column that is inserted
     * so that the column headers are shown in the header rows when there are no cues.
     *
     * @param col the column index
     * @return true is this is the dummy column
     */
    private boolean isDummyColumn(final int col) {
        return col == 0 && getCues().size() == 0;
    }

    public boolean isDummyColumnShown() {
        return isDummyColumn(0);
    }

    @Override
    public void postShowChange() {
        super.postShowChange();
        fireTableStructureChanged();
    }

    private float delta(final float value, final int delta) {
        float result = value + delta / 100f;
        if (result > 1f) {
            result = 1f;
        } else if (result < 0f) {
            result = 0f;
        }
        return result;
    }

    public boolean isSelected(final int row, final int column) {
        boolean selected = false;
        if (!isDummyColumn(column)) {
            LightCueDetail detail = getDetail(column);
            if (isRowTiming(row)) {
                selected = detail.isTimingSelected();
            } else {
                if (isRowChannel(row)) {
                    selected = detail.getChannelLevel(getChannelIndex(row)).isSelected();
                } else if (isRowSubmaster(row)) {
                    selected = detail.getSubmasterLevel(getSubmasterIndex(row)).isSelected();
                }
            }
        }
        return selected;
    }

    public void scroll(final int delta) {
        for (int row = 0; row < getRowCount(); row++) {
            for (int column = 0; column < getColumnCount(); column++) {
                if (isSelected(row, column)) {
                    LightCueDetail detail = (LightCueDetail) getCue(column).getDetail();
                    if (isRowTiming(row)) {
                        Timing timing = detail.getTiming();
                        if (!timing.isSplitFade()) {
                            Time time = timing.getFadeInTime();
                            int id = time.getId();
                            id += delta;
                            if (id < 0) {
                                id = 0;
                            } else if (id > Time.FOREVER.getId()) {
                                id = Time.FOREVER.getId();
                            }
                            time = Time.get(id);
                            timing.setFadeInTime(time);
                            fireTableCellUpdated(row, column);
                        }
                    } else if (isRowSubmaster(row)) {
                        int submasterIndex = getSubmasterIndex(row);
                        float value = detail.getSubmasterLevel(submasterIndex).getValue();
                        value = delta(value, delta);
                        getCues().setCueSubmaster(getCueIndex(column), submasterIndex, value);
                        fireTableCellUpdated(row, column);
                    } else {
                        int channelIndex = getChannelIndex(row);
                        float value = detail.getChannelLevel(channelIndex).getChannelLevelValue().getValue();
                        value = delta(value, delta);
                        getCues().setChannel(getCueIndex(column), channelIndex, value);
                        fireTableCellUpdated(row, column);
                    }
                }
            }
        }
    }

    public void setSelected(final int row, final int column, final boolean selected) {
        if (!isDummyColumn(column)) {
            LightCueDetail detail = getDetail(column);
            if (isRowTiming(row)) {
                detail.setTimingSelected(selected);
                fireTableCellUpdated(row, column);
            } else {
                if (isRowChannel(row)) {
                    CueChannelLevel level = detail.getChannelLevel(getChannelIndex(row));
                    boolean oldSelected = level.isSelected();
                    if (selected != oldSelected) {
                        level.setSelected(selected);
                        fireTableCellUpdated(row, column);
                    }
                } else if (isRowSubmaster(row)) {
                    CueSubmasterLevel level = detail.getSubmasterLevel(getSubmasterIndex(row));
                    boolean oldSelected = level.isSelected();
                    if (selected != oldSelected) {
                        level.setSelected(selected);
                        fireTableCellUpdated(row, column);
                    }
                }
            }
        }
    }

    /**
     * Gets a text representation of the information at given row.
     *
     * @param row the row index
     * @return the row text
     */
    public String getRowText(final int row) {
        String value = "";
        if (isRowChannel(row)) {
            int index = getChannelIndex(row);
            Channel channel = getContext().getShow().getChannels().get(index);
            value = "[" + (channel.getId() + 1) + "] " + channel.getName();
        } else if (isRowSubmaster(row)) {
            int index = getSubmasterIndex(row);
            Submaster submaster = getContext().getShow().getSubmasters().get(index);
            value = "[S" + (submaster.getId() + 1) + "] " + submaster.getName();
        }
        return value;
    }

    public void setFocus(final CellFocusListener cellFocusListener, final int row, final int column) {
        if (!isRowTiming(row)) {
            int cueIndex = getCueIndex(column);
            if (isRowSubmaster(row)) {
                int submasterIndex = getSubmasterIndex(row);
                cellFocusListener.setFocusSubmaster(cueIndex, submasterIndex);
            } else {
                int channelIndex = row - getShow().getNumberOfSubmasters() - 1;
                int firstRow = getShow().getNumberOfSubmasters() + 1;
                int lastRow = getRowCount();
                int channelIndexes[] = new int[lastRow - firstRow];
                for (int i = firstRow; i < lastRow; i++) {
                    channelIndexes[i - firstRow] = getChannelIndex(i);
                }
                cellFocusListener.setFocusChannel(cueIndex, channelIndex, channelIndexes);
            }
        }
    }
}
