package be.lassi.ui.sheet;

import static be.lassi.util.Util.newArrayList;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import be.lassi.base.LListListener;
import be.lassi.base.Listener;
import be.lassi.context.ShowContext;
import be.lassi.context.ShowContextListener;
import be.lassi.control.device.Control;
import be.lassi.control.device.LevelControl;
import be.lassi.control.midi.MidiPreferences;
import be.lassi.cues.Cue;
import be.lassi.cues.CueChannelLevel;
import be.lassi.cues.CueSubmasterLevel;
import be.lassi.cues.Cues;
import be.lassi.cues.CuesListener;
import be.lassi.cues.LightCueDetail;
import be.lassi.cues.LightCues;
import be.lassi.cues.Timing;
import be.lassi.domain.Channel;
import be.lassi.domain.Group;
import be.lassi.domain.Submaster;
import be.lassi.ui.sheet.actions.SheetActions;
import be.lassi.ui.sheet.cells.CellFocus;
import be.lassi.ui.sheet.cells.CellFocusListener;
import be.lassi.ui.widgets.TimingField;
import com.jgoodies.binding.beans.PropertyAdapter;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

public class SheetPresentationModel implements ShowContextListener {

    private static final int FOCUS_NONE = 0;

    private static final int FOCUS_STAGE = 1;

    private static final int FOCUS_CHANNEL = 2;

    private static final int FOCUS_SUBMASTER = 3;

    protected static final int COLUMN_WIDTH = 55;

    private static final int DEFAULT_GROUP_SIZE = 4;

    private final ShowContext context;

    private final SheetActions actions;

    private final CuesListener cuesListener;

    private final SheetTableModelHeaders tableModelHeaders;

    private final SheetTableModelDetails tableModelDetails;

    private final TableMouseWheelListener mouseWheelListener;

    private final HeaderSelectionListener headerSelectionListener;

    private final MouseListener detailHeaderMouseListener = new DetailHeaderMouseListener();

    private final MouseListener scrollPaneMouseListener = new ScrollPaneMouseListener();

    private final GroupsListener groupsListener = new GroupsListener();

    private final GroupsListListener groupsListListener = new GroupsListListener();

    private final Listener groupListener = new GroupListener();

    private final ValueHolder statusText = new ValueHolder("");

    private final CellFocusListener cellFocusListener = new MyCellFocusListener();

    private final CellFocus cellFocus = new CellFocus(cellFocusListener);

    private int focus = FOCUS_NONE;

    /**
     * Index of the range that is currently selected.  Each range contains Control.sheetGroupSize
     * number of submasters or channels.  Example: if sheetGroupSize is 8, then channel range 0
     * contains channels 0 to 7, range 1 contains channel 8 to 15, etc.  A value of -1 indicates
     * that no range is currently selected.
     */
    private int currentRange = -1;

    private int currentCueIndex = -1;

    private int currentChannelIndexes[] = new int[0];

    private final LevelControlListener levelControlListener = new LevelControlListener();

    private final ValueModel midiEnabled;

    public SheetPresentationModel(final ShowContext context) {
        this.context = context;
        actions = new SheetActions(context);
        tableModelHeaders = new SheetTableModelHeaders(context);
        tableModelDetails = new SheetTableModelDetails(context);
        mouseWheelListener = new TableMouseWheelListener();
        headerSelectionListener = new HeaderSelectionListener();
        cuesListener = createCuesListener();
        context.addShowContextListener(this);
        installGroupListeners();
        for (LevelControl levelControl : getControl().getLevelControls()) {
            levelControl.getHolder().add(levelControlListener);
        }
        context.getPreferences().getMidiPreferences().addPropertyChangeListener(MidiPreferences.ENABLED, new PropertyChangeListener() {

            public void propertyChange(final PropertyChangeEvent evt) {
                boolean enabled = context.getPreferences().getMidiPreferences().isEnabled();
                if (!enabled) {
                    resetControlFocus();
                    tableModelDetails.fireTableDataChanged();
                    tableModelHeaders.fireTableDataChanged();
                } else {
                    cellFocus.reset();
                }
            }
        });
        MidiPreferences mp = context.getPreferences().getMidiPreferences();
        midiEnabled = new PropertyAdapter<MidiPreferences>(mp, MidiPreferences.ENABLED);
        actions.adjust();
    }

    public ValueModel getMidiEnabled() {
        return midiEnabled;
    }

    public void bindChangeTimingField(final TimingField field) {
        field.setTiming(context.getDefaultTiming());
        field.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(final FocusEvent e) {
                Timing timing = field.getTiming();
                context.setChangeTiming(timing);
            }
        });
        field.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                Timing timing = field.getTiming();
                context.setChangeTiming(timing);
            }
        });
    }

    public void bindDefaultTimingField(final TimingField field) {
        field.setTiming(context.getDefaultTiming());
        field.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(final FocusEvent e) {
                Timing timing = field.getTiming();
                context.setDefaultTiming(timing);
            }
        });
        field.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                Timing timing = field.getTiming();
                context.setDefaultTiming(timing);
            }
        });
    }

    public SheetActions getActions() {
        return actions;
    }

    public CellFocusListener getCellFocusListener() {
        return cellFocusListener;
    }

    public CellFocus getCellFocus() {
        return cellFocus;
    }

    public MouseListener getDetailHeaderMouseListener() {
        return detailHeaderMouseListener;
    }

    public ValueModel getDirtyModel() {
        return context.getDirtyShow().getModel();
    }

    public ListSelectionListener getHeaderSelectionListener() {
        return headerSelectionListener;
    }

    public TableMouseWheelListener getMouseWheelListener() {
        return mouseWheelListener;
    }

    public MouseListener getScrollPaneMouseListener() {
        return scrollPaneMouseListener;
    }

    public ValueHolder getStatusText() {
        return statusText;
    }

    public SheetTableModelDetails getTableModelDetails() {
        return tableModelDetails;
    }

    public SheetTableModelHeaders getTableModelHeaders() {
        return tableModelHeaders;
    }

    public void hooverOverDetailAt(final int row, final int column) {
        StringBuilder b = new StringBuilder();
        if (row != -1) {
            String rowText = tableModelDetails.getRowText(row);
            b.append(rowText);
            Cue cue = tableModelDetails.getCue(column);
            if (cue != null) {
                if (rowText.length() > 0) {
                    b.append("  |  ");
                }
                b.append("Cue ");
                b.append(cue.getNumber());
                if (cue.getPrompt().trim().length() > 0) {
                    b.append("  |  ");
                    b.append(cue.getPrompt());
                }
            }
        }
        String text = b.toString();
        statusText.setValue(text);
    }

    public void postShowChange() {
        getLightCues().addListener(cuesListener);
        installGroupListeners();
        resizeColumns();
    }

    public void preShowChange() {
        getLightCues().removeListener(cuesListener);
        uninstallGroupListeners();
    }

    private void channelLevelChanged() {
        if (currentCueIndex != -1) {
            int index = currentRange * getControl().getSheetGroupSize();
            for (LevelControl levelControl : getControl().getLevelControls()) {
                updateChannel(levelControl, index++);
            }
        }
    }

    private void updateChannel(final LevelControl levelControl, final int index) {
        if (index < currentChannelIndexes.length) {
            int channelIndex = currentChannelIndexes[index];
            int old = getCues().getLightCues().getDetail(currentCueIndex).getChannelLevel(channelIndex).getChannelIntValue();
            if (old != levelControl.getLevel()) {
                float value = levelControl.getLevel() / 100f;
                getCues().getLightCues().setChannel(currentCueIndex, channelIndex, value);
            }
        }
    }

    private CuesListener createCuesListener() {
        return new CuesListener() {

            public void added(final int index, final Cue definition) {
                actions.adjust();
                tableStructureChanged();
            }

            public void channelLevelChanged(final int cueIndex, final int channelIndex) {
                tableModelDetails.fireCueChannelUpdated(cueIndex, channelIndex);
            }

            public void cueNumbersChanged() {
                tableStructureChanged();
            }

            public void currentChanged() {
                actions.adjust();
                tableModelDetails.fireTableDataChanged();
            }

            public void removed(final int index, final Cue definition) {
                actions.adjust();
                tableStructureChanged();
            }

            public void selectionChanged() {
                actions.adjust();
                tableModelDetails.fireTableDataChanged();
            }

            public void submasterLevelChanged(final int cueIndex, final int submasterIndex) {
                tableModelDetails.fireCueSubmasterUpdated(cueIndex, submasterIndex);
            }
        };
    }

    private void deassign() {
        if (focus == FOCUS_STAGE) {
            deassignStage();
        } else if (focus == FOCUS_CHANNEL) {
            deassignChannels();
        } else if (focus == FOCUS_SUBMASTER) {
            deassignSubmasters();
        } else {
        }
    }

    private void deassignChannels() {
        if (currentRange >= 0 && currentCueIndex >= 0) {
            int start = currentRange * getControl().getSheetGroupSize();
            int end = start + getControl().getLevelControls().size();
            if (end > currentChannelIndexes.length) {
                end = currentChannelIndexes.length;
            }
            LightCueDetail detail = getDetail(currentCueIndex);
            for (int i = start; i < end; i++) {
                int channelIndex = currentChannelIndexes[i];
                Channel channel = context.getShow().getChannels().get(channelIndex);
                channel.setLevelControl(null);
                tableModelHeaders.fireTableDataChanged();
                CueChannelLevel level = detail.getChannelLevel(channelIndex);
                level.setLevelControl(null);
            }
        }
        currentRange = -1;
        currentCueIndex = -1;
        currentChannelIndexes = new int[0];
    }

    private void deassignStage() {
    }

    private void deassignSubmasters() {
        if (currentRange >= 0 && currentCueIndex >= 0) {
            int start = currentRange * getControl().getSheetGroupSize();
            int end = start + getControl().getLevelControls().size();
            if (end > context.getShow().getNumberOfSubmasters()) {
                end = context.getShow().getNumberOfSubmasters();
            }
            LightCueDetail detail = getDetail(currentCueIndex);
            for (int i = start; i < end; i++) {
                Submaster submaster = context.getShow().getSubmasters().get(i);
                submaster.setLevelControl(null);
                tableModelHeaders.fireTableDataChanged();
                CueSubmasterLevel level = detail.getSubmasterLevel(i);
                level.setLevelControl(null);
            }
        }
        currentRange = -1;
        currentCueIndex = -1;
    }

    private void doSetFocus(final int row, final int column) {
        if (isMidiControlEnabled()) {
            tableModelDetails.setFocus(cellFocusListener, row, column);
        }
    }

    /**
     * @param cueIndex
     * @param channelIndex
     * @param channelIndexes
     */
    private void doSetFocusChannel(final int cueIndex, final int channelIndex, final int[] channelIndexes) {
        if (focus != FOCUS_CHANNEL) {
            deassign();
        }
        focus = FOCUS_CHANNEL;
        int newRange = channelIndex / getGroupSize();
        if (currentRange != newRange || currentCueIndex != cueIndex) {
            deassignChannels();
            currentRange = newRange;
            currentCueIndex = cueIndex;
            currentChannelIndexes = channelIndexes;
            int index = currentRange * getGroupSize();
            int end = index + getControl().getLevelControls().size();
            if (end > channelIndexes.length) {
                end = channelIndexes.length;
            }
            LightCueDetail detail = getDetail(currentCueIndex);
            for (LevelControl levelControl : getControl().getLevelControls()) {
                if (index < end) {
                    int channelId = channelIndexes[index];
                    Channel channel = context.getShow().getChannels().get(channelId);
                    channel.setLevelControl(levelControl);
                    CueChannelLevel level = detail.getChannelLevel(channelId);
                    level.setLevelControl(levelControl);
                    levelControl.getHolder().setValue(level.getChannelIntValue(), levelControlListener);
                } else {
                    levelControl.setLevel(0);
                }
                index++;
            }
        }
        tableModelDetails.fireTableRowsUpdated(0, tableModelDetails.getRowCount());
        tableModelHeaders.fireTableDataChanged();
    }

    private void doSetFocusSubmaster(final int cueIndex, final int submasterIndex) {
        if (focus != FOCUS_SUBMASTER) {
            deassign();
        }
        focus = FOCUS_SUBMASTER;
        int newRange = submasterIndex / getControl().getSheetGroupSize();
        if (currentRange != newRange || currentCueIndex != cueIndex) {
            deassignSubmasters();
            currentRange = newRange;
            currentCueIndex = cueIndex;
            int index = currentRange * getControl().getSheetGroupSize();
            int end = index + getControl().getLevelControls().size();
            if (end > context.getShow().getNumberOfSubmasters()) {
                end = context.getShow().getNumberOfSubmasters();
            }
            LightCueDetail detail = getDetail(currentCueIndex);
            for (LevelControl levelControl : getControl().getLevelControls()) {
                if (index < end) {
                    CueSubmasterLevel level = detail.getSubmasterLevel(index);
                    Submaster submaster = context.getShow().getSubmasters().get(index);
                    submaster.setLevelControl(levelControl);
                    level.setLevelControl(levelControl);
                    levelControl.getHolder().setValue(level.getIntValue(), levelControlListener);
                } else {
                    levelControl.setLevel(0);
                }
                index++;
            }
        }
        tableModelDetails.fireTableRowsUpdated(0, tableModelDetails.getRowCount());
        tableModelHeaders.fireTableDataChanged();
    }

    private Cues getCues() {
        return context.getShow().getCues();
    }

    private LightCueDetail getDetail(final int cueIndex) {
        return getCues().getLightCues().getDetail(cueIndex);
    }

    private LightCues getLightCues() {
        return getCues().getLightCues();
    }

    private void installGroupListeners() {
        context.getShow().getGroups().getListeners().add(groupsListener);
        context.getShow().getGroups().addGroupsListener(groupsListListener);
        for (Group group : context.getShow().getGroups()) {
            group.getListeners().add(groupListener);
        }
    }

    private boolean isMidiControlEnabled() {
        return context.getPreferences().getMidiPreferences().isEnabled();
    }

    private void levelChanged() {
        if (focus == FOCUS_CHANNEL) {
            channelLevelChanged();
        } else if (focus == FOCUS_SUBMASTER) {
            submasterLevelChanged();
        }
    }

    private void resetControlFocus() {
        cellFocus.reset();
        if (focus != FOCUS_NONE) {
            deassign();
        }
        focus = FOCUS_NONE;
    }

    private void resizeColumns() {
        setColumnWidth(COLUMN_WIDTH);
        if (tableModelDetails.isDummyColumnShown()) {
            setColumnWidth(0, 0);
        }
    }

    /**
     * Set all columns to given width.
     *
     * @param width
     */
    private void setColumnWidth(final int width) {
        for (int i = 0; i < tableModelDetails.getColumnCount(); i++) {
            setColumnWidth(i, width);
        }
    }

    /**
     * Set column with given index to given width.
     *
     * @param colIndex
     * @param width
     */
    private void setColumnWidth(final int colIndex, final int width) {
        tableModelDetails.getColumnModel().getColumn(colIndex).setMinWidth(width);
        tableModelDetails.getColumnModel().getColumn(colIndex).setPreferredWidth(width);
    }

    private void submasterLevelChanged() {
        if (currentCueIndex != -1) {
            int submasterIndex = currentRange * getControl().getGroupSize();
            for (LevelControl levelControl : getControl().getLevelControls()) {
                updateSubmaster(levelControl, submasterIndex++);
            }
        }
    }

    private void updateSubmaster(final LevelControl levelControl, final int submasterIndex) {
        if (submasterIndex < context.getShow().getNumberOfSubmasters()) {
            int old = getCues().getLightCues().getDetail(currentCueIndex).getSubmasterLevel(submasterIndex).getIntValue();
            if (old != levelControl.getLevel()) {
                float value = levelControl.getLevel() / 100f;
                getCues().getLightCues().setCueSubmaster(currentCueIndex, submasterIndex, value);
            }
        }
    }

    private void tableStructureChanged() {
        tableModelDetails.fireTableStructureChanged();
        resizeColumns();
    }

    private void uninstallGroupListeners() {
        context.getShow().getGroups().getListeners().remove(groupsListener);
        context.getShow().getGroups().removeGroupsListener(groupsListListener);
        for (Group group : context.getShow().getGroups()) {
            group.getListeners().remove(groupListener);
        }
    }

    private Control getControl() {
        return context.getControlHolder().getValue();
    }

    private int getGroupSize() {
        int groupSize = getControl().getSheetGroupSize();
        if (groupSize == 0) {
            groupSize = DEFAULT_GROUP_SIZE;
        }
        return groupSize;
    }

    public class TableMouseWheelListener implements MouseWheelListener {

        List<MouseWheelListener> originalListeners = newArrayList();

        public void install(final JComponent component) {
            originalListeners.clear();
            component.removeMouseWheelListener(this);
            MouseWheelListener[] listeners = component.getListeners(MouseWheelListener.class);
            for (MouseWheelListener listener : listeners) {
                originalListeners.add(listener);
                component.removeMouseWheelListener(listener);
            }
            component.addMouseWheelListener(this);
        }

        public void mouseWheelMoved(final MouseWheelEvent e) {
            int delta = e.getWheelRotation() < 0 ? +1 : -1;
            if (e.isControlDown()) {
                update(delta);
            } else if (e.isAltDown() || e.isMetaDown()) {
                update(100 * delta);
            } else if (e.isShiftDown()) {
                update(5 * delta);
            } else {
                for (MouseWheelListener listener : originalListeners) {
                    listener.mouseWheelMoved(e);
                }
            }
        }

        private void update(final int delta) {
            tableModelDetails.scroll(delta);
        }
    }

    private class DetailHeaderMouseListener extends MouseAdapter {

        @Override
        public void mouseClicked(final MouseEvent event) {
            int cueIndex = getCueIndex(event);
            if (cueIndex >= 0) {
                if (event.isPopupTrigger()) {
                    popup(event);
                } else {
                    new CueSelector(getCues()).mouseClicked(event, cueIndex);
                }
            }
        }

        @Override
        public void mousePressed(final MouseEvent event) {
            if (event.isPopupTrigger()) {
                popup(event);
            }
        }

        @Override
        public void mouseReleased(final MouseEvent event) {
            if (event.isPopupTrigger()) {
                popup(event);
            }
        }

        private int getCueIndex(final MouseEvent event) {
            int cueIndex = -1;
            JTableHeader tableHeader = (JTableHeader) event.getSource();
            int lightCueIndex = tableHeader.columnAtPoint(event.getPoint());
            if (lightCueIndex >= 0) {
                Cue cue = getCues().getLightCues().get(lightCueIndex);
                cueIndex = getCues().indexOf(cue);
            }
            return cueIndex;
        }

        private void popup(final MouseEvent event) {
            int cueIndex = getCueIndex(event);
            actions.adjust(cueIndex);
            JPopupMenu popupMenu = actions.getPopupMenu();
            Component source = (Component) event.getSource();
            popupMenu.show(source, event.getX(), event.getY());
        }
    }

    private class GroupListener implements Listener {

        public void changed() {
            tableModelDetails.fireTableDataChanged();
            tableModelHeaders.fireTableDataChanged();
        }
    }

    private class GroupsListener implements Listener {

        public void changed() {
            resetControlFocus();
            tableModelDetails.fireTableDataChanged();
            tableModelHeaders.fireTableDataChanged();
        }
    }

    private class GroupsListListener implements LListListener<Group> {

        public void added(final int[] indices, final List<Group> groups) {
            resetControlFocus();
            for (Group group : groups) {
                group.getListeners().add(groupListener);
            }
            tableModelDetails.fireTableDataChanged();
        }

        public void removed(final int[] indices, final List<Group> groups) {
            resetControlFocus();
            for (Group group : groups) {
                group.getListeners().remove(groupListener);
            }
            tableModelDetails.fireTableDataChanged();
        }
    }

    private class HeaderSelectionListener implements ListSelectionListener {

        public void valueChanged(final ListSelectionEvent e) {
            ListSelectionModel table = (ListSelectionModel) e.getSource();
            int row = table.getLeadSelectionIndex();
            if (row < 0) {
            } else {
                if (tableModelHeaders.isRowSubmaster(row)) {
                    tableModelHeaders.setSelectedSubmasterIndex(tableModelHeaders.getSubmasterIndex(row));
                } else {
                }
            }
        }
    }

    private class LevelControlListener implements Listener {

        public void changed() {
            levelChanged();
        }
    }

    private class MyCellFocusListener implements CellFocusListener {

        public void setFocus(final int row, final int column) {
            doSetFocus(row, column);
            hooverOverDetailAt(row, column);
        }

        public void setFocusChannel(final int cueIndex, final int channelIndex, final int[] channelIndexes) {
            doSetFocusChannel(cueIndex, channelIndex, channelIndexes);
        }

        public void setFocusSubmaster(final int cueIndex, final int submasterIndex) {
            doSetFocusSubmaster(cueIndex, submasterIndex);
        }
    }

    private class ScrollPaneMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(final MouseEvent event) {
            popup(event);
        }

        @Override
        public void mouseReleased(final MouseEvent event) {
            popup(event);
        }

        private void popup(final MouseEvent event) {
            if (event.isPopupTrigger()) {
                actions.adjust(-1);
                JPopupMenu popupMenu = actions.getPopupMenu();
                Component source = (Component) event.getSource();
                popupMenu.show(source, event.getX(), event.getY());
            }
        }
    }
}
