package org.rdv.datapanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.apache.commons.logging.Log;
import org.rdv.rbnb.Channel;
import org.rdv.rbnb.Player;
import org.rdv.ui.ChannelListDataFlavor;
import com.rbnb.sapi.ChannelMap;

/**
 * A Data Panel extension to display numeric data in a tabular form. Maximum and
 * minimum values are also displayed.
 * 
 * @author Jason P. Hanley
 * @author Lawrence J. Miller <ljmiller@sdsc.edu>
 * @since 1.3
 */
public class DigitalTabularDataPanel extends AbstractDataPanel {

    static Log log = org.rdv.LogFactory.getLog(DigitalTabularDataPanel.class.getName());

    /**
	 * The main panel
	 * 
	 * @since 1.3
	 */
    private JPanel mainPanel;

    /**
	 * The container for the tables
	 */
    private Box panelBox;

    /**
	 * The maximum number of column groups
	 */
    private static final int MAX_COLUMN_GROUP_COUNT = 10;

    /**
	 * The current number of column groups
	 */
    private int columnGroupCount;

    /**
	 * The data models for the table
	 */
    private List<DataTableModel> tableModels;

    /**
	 * The tables
	 */
    private List<JTable> tables;

    /**
	 * A map from channels to table indexes
	 */
    private Map<String, Integer> channelTableMap;

    /**
	 * The cell renderer for the data
	 */
    private DoubleTableCellRenderer doubleCellRenderer;

    private DataTableCellRenderer dataCellRenderer;

    /**
	 * The button to set decimal data rendering
	 */
    private JToggleButton decimalButton;

    /**
	 * The button to set engineering data renderering
	 */
    private JToggleButton engineeringButton;

    /**
	 * The group of check box components to control the the min/max columns
	 * visibility
	 */
    private CheckBoxGroup showMinMaxCheckBoxGroup;

    /**
	 * The group of check box components to control the the threshold columns
	 * visibility
	 */
    private CheckBoxGroup showThresholdCheckBoxGroup;

    /**
	 * The last time data was displayed in the UI
	 * 
	 * @since 1.2
	 */
    double lastTimeDisplayed;

    /**
	 * The maximum number of channels allowed in this data panel;
	 */
    private static final int MAX_CHANNELS_PER_COLUMN_GROUP = 40;

    /**
	 * The percentage of the warning value, when a value gets within this much
	 * of a threshold, a yellow background will be shown.
	 */
    private static final double WARNING_PERCENTAGE = 0.25;

    /**
	 * The percentage of the critical value, when a value gets within this much
	 * of a threshold, a red background will be shown.
	 */
    private static final double CRITICAL_PERCENTAGE = 0.1;

    private JCheckBox useOffsetsCheckBox;

    /**
	 * Initialize the data panel.
	 * 
	 * @since 1.2
	 */
    public DigitalTabularDataPanel() {
        super();
        tableModels = new ArrayList<DataTableModel>();
        tables = new ArrayList<JTable>();
        channelTableMap = new HashMap<String, Integer>();
        lastTimeDisplayed = -1;
        initComponents();
        setDataComponent(mainPanel);
    }

    /**
	 * Initialize the container and add the header.
	 * 
	 * @since 1.2
	 */
    private void initComponents() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        panelBox = Box.createHorizontalBox();
        dataCellRenderer = new DataTableCellRenderer();
        doubleCellRenderer = new DoubleTableCellRenderer();
        showMinMaxCheckBoxGroup = new CheckBoxGroup();
        showThresholdCheckBoxGroup = new CheckBoxGroup();
        addColumn();
        mainPanel.add(panelBox, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        JPanel offsetsPanel = new JPanel();
        useOffsetsCheckBox = new JCheckBox("Use Offsets");
        useOffsetsCheckBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                boolean checked = ((JCheckBox) ae.getSource()).isSelected();
                useOffsetRenderer(checked);
            }
        });
        offsetsPanel.add(useOffsetsCheckBox);
        JButton takeOffsetsButton = new JButton("Take Offsets");
        takeOffsetsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                useOffsetsCheckBox.setSelected(true);
                for (int i = 0; i < columnGroupCount; i++) {
                    ((DataTableModel) tableModels.get(i)).takeOffsets();
                    ((DataTableModel) tableModels.get(i)).useOffsets(true);
                    ((JTable) tables.get(i)).repaint();
                }
            }
        });
        offsetsPanel.add(takeOffsetsButton);
        buttonPanel.add(offsetsPanel, BorderLayout.WEST);
        JPanel formatPanel = new JPanel();
        decimalButton = new JToggleButton("Decimal", true);
        decimalButton.setSelected(true);
        decimalButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                useEngineeringRenderer(false);
            }
        });
        formatPanel.add(decimalButton);
        engineeringButton = new JToggleButton("Engineering");
        engineeringButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                useEngineeringRenderer(true);
            }
        });
        formatPanel.add(engineeringButton);
        ButtonGroup formatButtonGroup = new ButtonGroup();
        formatButtonGroup.add(decimalButton);
        formatButtonGroup.add(engineeringButton);
        buttonPanel.add(formatPanel, BorderLayout.EAST);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.addMouseListener(new MouseInputAdapter() {
        });
    }

    private void addColumn() {
        if (columnGroupCount == MAX_COLUMN_GROUP_COUNT) {
            return;
        }
        if (columnGroupCount != 0) {
            panelBox.add(Box.createHorizontalStrut(7));
        }
        final int tableIndex = columnGroupCount;
        final DataTableModel tableModel = new DataTableModel();
        final JTable table = new JTable(tableModel);
        table.setDragEnabled(true);
        table.setName(DigitalTabularDataPanel.class.getName() + " JTable #" + Integer.toString(columnGroupCount));
        if (showThresholdCheckBoxGroup.isSelected()) {
            tableModel.setThresholdVisible(true);
        }
        if (showMinMaxCheckBoxGroup.isSelected()) {
            tableModel.setMaxMinVisible(true);
            table.getColumn("Min").setCellRenderer(doubleCellRenderer);
            table.getColumn("Max").setCellRenderer(doubleCellRenderer);
        }
        table.getColumn("Value").setCellRenderer(dataCellRenderer);
        tables.add(table);
        tableModels.add(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(table);
        panelBox.add(tableScrollPane);
        JPopupMenu popupMenu = new JPopupMenu();
        final JMenuItem copyMenuItem = new JMenuItem("Copy");
        copyMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                TransferHandler.getCopyAction().actionPerformed(new ActionEvent(table, ae.getID(), ae.getActionCommand(), ae.getWhen(), ae.getModifiers()));
            }
        });
        popupMenu.add(copyMenuItem);
        popupMenu.addSeparator();
        JMenuItem printMenuItem = new JMenuItem("Print...");
        printMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    table.print(JTable.PrintMode.FIT_WIDTH);
                } catch (PrinterException pe) {
                }
            }
        });
        popupMenu.add(printMenuItem);
        popupMenu.addSeparator();
        final JCheckBoxMenuItem showMaxMinMenuItem = new JCheckBoxMenuItem("Show min/max columns", false);
        showMaxMinMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                setMaxMinVisible(showMaxMinMenuItem.isSelected());
            }
        });
        showMinMaxCheckBoxGroup.addCheckBox(showMaxMinMenuItem);
        popupMenu.add(showMaxMinMenuItem);
        final JCheckBoxMenuItem showThresholdMenuItem = new JCheckBoxMenuItem("Show threshold columns", false);
        showThresholdMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                setThresholdVisible(showThresholdMenuItem.isSelected());
            }
        });
        showThresholdCheckBoxGroup.addCheckBox(showThresholdMenuItem);
        popupMenu.add(showThresholdMenuItem);
        popupMenu.addSeparator();
        JMenuItem blankRowMenuItem = new JMenuItem("Insert blank row");
        blankRowMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                tableModel.addBlankRow();
            }
        });
        popupMenu.add(blankRowMenuItem);
        final JMenuItem removeSelectedRowsMenuItem = new JMenuItem("Remove selected rows");
        removeSelectedRowsMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeSelectedRows(tableIndex);
            }
        });
        popupMenu.add(removeSelectedRowsMenuItem);
        popupMenu.addSeparator();
        JMenu numberOfColumnsMenu = new JMenu("Number of columns");
        numberOfColumnsMenu.addMenuListener(new MenuListener() {

            public void menuSelected(MenuEvent me) {
                JMenu menu = (JMenu) me.getSource();
                for (int j = 0; j < MAX_COLUMN_GROUP_COUNT; j++) {
                    JMenuItem menuItem = menu.getItem(j);
                    boolean selected = (j == (columnGroupCount - 1));
                    menuItem.setSelected(selected);
                }
            }

            public void menuDeselected(MenuEvent me) {
            }

            public void menuCanceled(MenuEvent me) {
            }
        });
        for (int i = 0; i < MAX_COLUMN_GROUP_COUNT; i++) {
            final int number = i + 1;
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(Integer.toString(number));
            item.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    setNumberOfColumns(number);
                }
            });
            numberOfColumnsMenu.add(item);
        }
        popupMenu.add(numberOfColumnsMenu);
        popupMenu.addPopupMenuListener(new PopupMenuListener() {

            public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
                boolean anyRowsSelected = table.getSelectedRowCount() > 0;
                copyMenuItem.setEnabled(anyRowsSelected);
                removeSelectedRowsMenuItem.setEnabled(anyRowsSelected);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
            }

            public void popupMenuCanceled(PopupMenuEvent arg0) {
            }
        });
        table.setComponentPopupMenu(popupMenu);
        tableScrollPane.setComponentPopupMenu(popupMenu);
        panelBox.revalidate();
        columnGroupCount++;
        properties.setProperty("numberOfColumns", Integer.toString(columnGroupCount));
    }

    private void removeColumn() {
        if (columnGroupCount == 1) {
            return;
        }
        columnGroupCount--;
        Iterator i = channels.iterator();
        while (i.hasNext()) {
            String channelName = (String) i.next();
            if (channelTableMap.get(channelName) == columnGroupCount) {
                removeChannel(channelName);
            }
        }
        tables.remove(columnGroupCount);
        tableModels.remove(columnGroupCount);
        panelBox.remove(columnGroupCount * 2);
        panelBox.remove(columnGroupCount * 2 - 1);
        panelBox.revalidate();
    }

    private void setNumberOfColumns(int columns) {
        if (columnGroupCount < columns) {
            for (int i = columnGroupCount; i < columns; i++) {
                addColumn();
            }
        } else if (columnGroupCount > columns) {
            for (int i = columnGroupCount; i > columns; i--) {
                removeColumn();
            }
        }
    }

    /**
	 * Remove all the rows in the specified table that are selected. If a
	 * selected row is not blank, the channel in that row will be removed.
	 * 
	 * @param tableIndex
	 *            the index of the table
	 */
    private void removeSelectedRows(int tableIndex) {
        List<String> selectedChannels = new ArrayList<String>();
        JTable table = tables.get(tableIndex);
        int[] selectedRows = table.getSelectedRows();
        table.clearSelection();
        DataTableModel tableModel = tableModels.get(tableIndex);
        for (int j = selectedRows.length - 1; j >= 0; j--) {
            int row = selectedRows[j];
            if (tableModel.isBlankRow(row)) {
                tableModel.deleteBlankRow(row);
            } else {
                DataRow dataRow = tableModel.getRowAt(row);
                selectedChannels.add(dataRow.getName());
            }
        }
        for (String channelName : selectedChannels) {
            removeChannel(channelName);
        }
    }

    private void useOffsetRenderer(boolean useOffset) {
        useOffsetsCheckBox.setSelected(useOffset);
        for (int i = 0; i < columnGroupCount; i++) {
            ((DataTableModel) tableModels.get(i)).useOffsets(useOffset);
            ((JTable) tables.get(i)).repaint();
        }
        if (useOffset) {
            properties.setProperty("useoffset", "true");
        } else properties.setProperty("useoffset", "false");
    }

    private void useEngineeringRenderer(boolean useEngineeringRenderer) {
        dataCellRenderer.setShowEngineeringFormat(useEngineeringRenderer);
        doubleCellRenderer.setShowEngineeringFormat(useEngineeringRenderer);
        for (JTable table : tables) {
            table.repaint();
        }
        if (useEngineeringRenderer) {
            engineeringButton.setSelected(true);
            properties.setProperty("renderer", "engineering");
        } else {
            decimalButton.setSelected(true);
            properties.remove("renderer");
        }
    }

    private void setMaxMinVisible(boolean maxMinVisible) {
        if (showMinMaxCheckBoxGroup.isSelected() != maxMinVisible) {
            for (int i = 0; i < this.columnGroupCount; i++) {
                DataTableModel tableModel = tableModels.get(i);
                JTable table = tables.get(i);
                tableModel.setMaxMinVisible(maxMinVisible);
                table.getColumn("Value").setCellRenderer(dataCellRenderer);
                if (maxMinVisible) {
                    table.getColumn("Min").setCellRenderer(doubleCellRenderer);
                    table.getColumn("Max").setCellRenderer(doubleCellRenderer);
                }
                if (showThresholdCheckBoxGroup.isSelected()) {
                    table.getColumn("Min Thresh").setCellRenderer(doubleCellRenderer);
                    table.getColumn("Max Thresh").setCellRenderer(doubleCellRenderer);
                }
            }
            showMinMaxCheckBoxGroup.setSelected(maxMinVisible);
            if (maxMinVisible) {
                properties.setProperty("maxMinVisible", "true");
            } else {
                properties.remove("maxMinVisible");
            }
        }
    }

    private void setThresholdVisible(boolean thresholdVisible) {
        if (showThresholdCheckBoxGroup.isSelected() != thresholdVisible) {
            for (int i = 0; i < columnGroupCount; i++) {
                DataTableModel tableModel = tableModels.get(i);
                JTable table = tables.get(i);
                tableModel.setThresholdVisible(thresholdVisible);
                table.getColumn("Value").setCellRenderer(dataCellRenderer);
                if (showMinMaxCheckBoxGroup.isSelected()) {
                    table.getColumn("Min").setCellRenderer(doubleCellRenderer);
                    table.getColumn("Max").setCellRenderer(doubleCellRenderer);
                }
                if (thresholdVisible) {
                    table.getColumn("Min Thresh").setCellRenderer(doubleCellRenderer);
                    table.getColumn("Max Thresh").setCellRenderer(doubleCellRenderer);
                }
            }
            showThresholdCheckBoxGroup.setSelected(thresholdVisible);
            if (thresholdVisible) {
                properties.setProperty("thresholdVisible", "true");
            } else {
                properties.remove("thresholdVisible");
            }
        }
    }

    void clearData() {
        for (int i = 0; i < columnGroupCount; i++) {
            ((DataTableModel) tableModels.get(i)).clearData();
        }
        lastTimeDisplayed = -1;
    }

    public boolean supportsMultipleChannels() {
        return true;
    }

    /**
	 * an overridden method from
	 * 
	 * @see org.rdv.datapanel.AbstractDataPanel for the
	 * @see DropTargetListener interface
	 */
    public void drop(DropTargetDropEvent e) {
        try {
            int dropAction = e.getDropAction();
            if (dropAction == DnDConstants.ACTION_LINK) {
                DataFlavor channelListDataFlavor = new ChannelListDataFlavor();
                Transferable tr = e.getTransferable();
                if (e.isDataFlavorSupported(channelListDataFlavor)) {
                    e.acceptDrop(DnDConstants.ACTION_LINK);
                    e.dropComplete(true);
                    double clickX = e.getLocation().getX();
                    int compWidth = dataComponent.getWidth();
                    final int tableNum = (int) (clickX * columnGroupCount / compWidth);
                    final List channels = (List) tr.getTransferData(channelListDataFlavor);
                    new Thread() {

                        public void run() {
                            for (int i = 0; i < channels.size(); i++) {
                                String channel = (String) channels.get(i);
                                boolean status;
                                if (supportsMultipleChannels()) {
                                    status = addChannel(channel, tableNum);
                                } else {
                                    status = setChannel(channel);
                                }
                                if (!status) {
                                }
                            }
                        }
                    }.start();
                } else {
                    e.rejectDrop();
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (UnsupportedFlavorException ufe) {
            ufe.printStackTrace();
        }
    }

    public boolean addChannel(String channelName) {
        int tableNumber = 0;
        Integer index = channelTableMap.get(channelName);
        if (index != null) {
            tableNumber = index;
        }
        if (channelName.startsWith("BLANK ")) {
            tableNumber = Integer.parseInt(channelName.substring(6));
        }
        return addChannel(channelName, tableNumber);
    }

    public boolean addChannel(String channelName, int tableNum) {
        if (tableNum >= MAX_COLUMN_GROUP_COUNT) {
            return false;
        }
        if (tableNum >= columnGroupCount) {
            setNumberOfColumns(tableNum + 1);
        }
        if (tableModels.get(tableNum).getRowCount() >= MAX_CHANNELS_PER_COLUMN_GROUP) {
            return false;
        }
        if (channelName.startsWith("BLANK ")) {
            tableModels.get(tableNum).addBlankRow();
            return true;
        }
        if (channels.contains(channelName)) {
            return false;
        }
        channelTableMap.put(channelName, tableNum);
        if (super.addChannel(channelName)) {
            return true;
        } else {
            channelTableMap.remove(channelName);
            return false;
        }
    }

    protected void channelAdded(String channelName) {
        String labelText = channelName;
        Channel channel = rbnbController.getChannel(channelName);
        String unit = null;
        if (channel != null) {
            unit = channel.getMetadata("units");
            if (unit != null) {
                labelText += "(" + unit + ")";
            }
        }
        String lowerThresholdString = (String) (lowerThresholds.get(channelName));
        String upperThresholdString = (String) (upperThresholds.get(channelName));
        double lowerThreshold = Double.NEGATIVE_INFINITY;
        double upperThreshold = Double.POSITIVE_INFINITY;
        if (lowerThresholdString != null) {
            try {
                lowerThreshold = Double.parseDouble(lowerThresholdString);
            } catch (java.lang.NumberFormatException nfe) {
                log.warn("Non-numeric lower threshold in metadata: " + lowerThresholdString);
            }
        }
        if (upperThresholdString != null) {
            try {
                upperThreshold = Double.parseDouble(upperThresholdString);
            } catch (java.lang.NumberFormatException nfe) {
                log.warn("Non-numeric upper threshold in metadata: " + upperThresholdString);
            }
        }
        int tableNumber = channelTableMap.get(channelName);
        tableModels.get(tableNumber).addRow(channelName, unit, lowerThreshold, upperThreshold);
        properties.setProperty("channelTable_" + channelName, Integer.toString(tableNumber));
    }

    protected void channelRemoved(String channelName) {
        int tableNumber = channelTableMap.get(channelName);
        tableModels.get(tableNumber).deleteRow(channelName);
        channelTableMap.remove(channelName);
        properties.remove("channelTable_" + channelName);
        properties.remove("minThreshold_" + channelName);
        properties.remove("maxThreshold_" + channelName);
    }

    public void postTime(double time) {
        if (time < this.time) {
            lastTimeDisplayed = -1;
        }
        super.postTime(time);
        for (int j = 0; j < columnGroupCount; j++) {
            boolean rowCleared = false;
            DataTableModel tableModel = (DataTableModel) tableModels.get(j);
            for (int k = 0; k < tableModel.getRowCount(); k++) {
                DataRow row = tableModel.getRowAt(k);
                if (row.isCleared()) {
                    continue;
                }
                double timestamp = row.getTime();
                if (timestamp <= time - timeScale || timestamp > time) {
                    row.clearData();
                    rowCleared = true;
                }
            }
            if (rowCleared) {
                tableModel.fireTableDataChanged();
            }
        }
        if (channelMap == null) {
            return;
        }
        Iterator i = channels.iterator();
        while (i.hasNext()) {
            String channelName = (String) i.next();
            int channelIndex = channelMap.GetIndex(channelName);
            if (channelIndex != -1) {
                postDataTabular(channelName, channelIndex);
            }
        }
        if (state != Player.STATE_MONITORING) {
            lastTimeDisplayed = time;
        }
    }

    private void postDataTabular(String channelName, int channelIndex) {
        double[] times = channelMap.GetTimes(channelIndex);
        int startIndex = -1;
        double dataStartTime;
        if (lastTimeDisplayed == time) {
            dataStartTime = time - timeScale;
        } else {
            dataStartTime = lastTimeDisplayed;
        }
        for (int i = 0; i < times.length; i++) {
            if (times[i] > dataStartTime && times[i] <= time) {
                startIndex = i;
                break;
            }
        }
        if (startIndex == -1) {
            return;
        }
        int endIndex = startIndex;
        for (int i = times.length - 1; i > startIndex; i--) {
            if (times[i] <= time) {
                endIndex = i;
                break;
            }
        }
        for (int i = startIndex; i <= endIndex; i++) {
            double data;
            int typeID = channelMap.GetType(channelIndex);
            switch(typeID) {
                case ChannelMap.TYPE_FLOAT64:
                    data = channelMap.GetDataAsFloat64(channelIndex)[i];
                    break;
                case ChannelMap.TYPE_FLOAT32:
                    data = channelMap.GetDataAsFloat32(channelIndex)[i];
                    break;
                case ChannelMap.TYPE_INT64:
                    data = channelMap.GetDataAsInt64(channelIndex)[i];
                    break;
                case ChannelMap.TYPE_INT32:
                    data = channelMap.GetDataAsInt32(channelIndex)[i];
                    break;
                case ChannelMap.TYPE_INT16:
                    data = channelMap.GetDataAsInt16(channelIndex)[i];
                    break;
                case ChannelMap.TYPE_INT8:
                    data = channelMap.GetDataAsInt8(channelIndex)[i];
                    break;
                case ChannelMap.TYPE_STRING:
                case ChannelMap.TYPE_UNKNOWN:
                case ChannelMap.TYPE_BYTEARRAY:
                default:
                    return;
            }
            for (int ii = 0; ii < columnGroupCount; ii++) {
                ((DataTableModel) tableModels.get(ii)).updateData(channelName, times[i], data);
            }
        }
    }

    public List<String> subscribedChannels() {
        List<String> allChannels = new ArrayList<String>();
        for (int i = 0; i < tableModels.size(); i++) {
            DataTableModel tableModel = tableModels.get(i);
            for (int j = 0; j < tableModel.getRowCount(); j++) {
                DataRow dataRow = tableModel.getRowAt(j);
                String name = dataRow.getName();
                if (name.length() == 0) {
                    name = "BLANK " + i;
                }
                allChannels.add(name);
            }
        }
        return allChannels;
    }

    public void setProperty(String key, String value) {
        super.setProperty(key, value);
        if (key != null && value != null) {
            if (key.equals("renderer") && value.equals("engineering")) {
                useEngineeringRenderer(true);
            } else if (key.equals("maxMinVisible") && value.equals("true")) {
                setMaxMinVisible(true);
            } else if (key.equals("thresholdVisible") && value.equals("true")) {
                setThresholdVisible(true);
            } else if (key.equals("numberOfColumns")) {
                int columns = Integer.parseInt(value);
                setNumberOfColumns(columns);
            } else if (key.startsWith("channelTable_")) {
                String channelName = key.substring(13);
                int tableIndex = Integer.parseInt(value);
                channelTableMap.put(channelName, tableIndex);
            } else if (key.startsWith("minThreshold_")) {
                String channelName = key.substring(13);
                try {
                    Double.parseDouble(value);
                    lowerThresholds.put(channelName, value);
                    properties.put(key, value);
                } catch (NumberFormatException e) {
                }
            } else if (key.startsWith("maxThreshold_")) {
                String channelName = key.substring(13);
                try {
                    Double.parseDouble(value);
                    upperThresholds.put(channelName, value);
                    properties.put(key, value);
                } catch (NumberFormatException e) {
                }
            } else if (key.startsWith("useoffset") && value.equals("true")) {
                useOffsetRenderer(true);
            }
        }
    }

    public String toString() {
        return "Tabular Data Panel";
    }

    /** an inner class to implement the data model in this design pattern */
    private class DataTableModel extends AbstractTableModel {

        /** serialization version identifier */
        private static final long serialVersionUID = 1706987603986659555L;

        private String[] columnNames = { "Name", "Value", "Unit", "Min", "Max", "Min Thresh", "Max Thresh" };

        private ArrayList<DataRow> rows;

        private boolean cleared;

        private boolean maxMinVisible;

        private boolean thresholdVisible;

        private boolean useOffsets;

        private final DataRow BLANK_ROW = new DataRow(new String(), null, (Double.NEGATIVE_INFINITY), Double.POSITIVE_INFINITY);

        public DataTableModel() {
            super();
            rows = new ArrayList<DataRow>();
            cleared = true;
            maxMinVisible = false;
            thresholdVisible = false;
            useOffsets = false;
        }

        public boolean isCellEditable(int row, int col) {
            return (col == this.findColumn("Min Thresh") || col == this.findColumn("Max Thresh"));
        }

        public void addRow(String name, String unit, double lowerThresh, double upperThresh) {
            addRow(new DataRow(name, unit, lowerThresh, upperThresh));
        }

        public void addBlankRow() {
            addRow(BLANK_ROW);
        }

        public boolean isBlankRow(int row) {
            return rows.get(row) == BLANK_ROW;
        }

        public void deleteBlankRow(int row) {
            if (isBlankRow(row)) {
                rows.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }

        public void addRow(DataRow dataRow) {
            rows.add(dataRow);
            int row = rows.size() - 1;
            fireTableRowsInserted(row, row);
        }

        public void deleteRow(String name) {
            for (int i = 0; i < rows.size(); i++) {
                DataRow dataRow = (DataRow) rows.get(i);
                if (dataRow.name.equals(name)) {
                    rows.remove(dataRow);
                    this.fireTableRowsDeleted(i, i);
                    break;
                }
            }
        }

        public void updateData(String name, double time, double data) {
            cleared = false;
            String chanUnit = null;
            Channel channel = rbnbController.getChannel(name);
            if (channel != null) chanUnit = channel.getMetadata("units");
            for (int i = 0; i < rows.size(); i++) {
                DataRow dataRow = (DataRow) rows.get(i);
                if (dataRow.name.equals(name)) {
                    dataRow.setData(time, data, chanUnit);
                    fireTableDataChanged();
                    break;
                }
            }
        }

        public void clearData() {
            cleared = true;
            for (int i = 0; i < rows.size(); i++) {
                DataRow dataRow = (DataRow) rows.get(i);
                dataRow.clearData();
            }
            fireTableDataChanged();
        }

        public void takeOffsets() {
            for (int i = 0; i < rows.size(); i++) {
                DataRow dataRow = (DataRow) rows.get(i);
                dataRow.setOffset(dataRow.getData());
            }
            fireTableDataChanged();
        }

        public void useOffsets(boolean useOffsets) {
            if (this.useOffsets != useOffsets) {
                this.useOffsets = useOffsets;
                fireTableDataChanged();
            }
        }

        /** a method that will get a data row from its index */
        public DataRow getRowAt(int rowdex) {
            return ((DataRow) rows.get(rowdex));
        }

        public int getRowCount() {
            return rows.size();
        }

        public int getColumnCount() {
            int retval = columnNames.length;
            retval -= (maxMinVisible) ? 0 : 2;
            retval -= (thresholdVisible) ? 0 : 2;
            return retval;
        }

        public String getColumnName(int col) {
            if (col < 3 || maxMinVisible) {
                return columnNames[col];
            } else {
                return columnNames[col + 2];
            }
        }

        public Object getValueAt(int row, int col) {
            if (col != 0 && cleared) {
                return new String();
            }
            if (row == -1) {
                return null;
            }
            DataRow dataRow = (DataRow) rows.get(row);
            if (dataRow == BLANK_ROW) {
                return null;
            }
            String[] nameSplit = dataRow.getName().split("/");
            if (col > 2 && !maxMinVisible) {
                col += 2;
            }
            switch(col) {
                case 0:
                    return (nameSplit[nameSplit.length - 1]);
                case 1:
                    return dataRow.isCleared() ? null : new Double(dataRow.getData() - (useOffsets ? dataRow.getOffset() : 0));
                case 2:
                    return dataRow.getUnit();
                case 3:
                    return dataRow.isCleared() ? null : new Double(dataRow.getMinimum() - (useOffsets ? dataRow.getOffset() : 0));
                case 4:
                    return dataRow.isCleared() ? null : new Double(dataRow.getMaximum() - (useOffsets ? dataRow.getOffset() : 0));
                case 5:
                    return (dataRow.minThresh == Double.NEGATIVE_INFINITY) ? null : new Double(dataRow.minThresh);
                case 6:
                    return (dataRow.maxThresh == Double.POSITIVE_INFINITY) ? null : new Double(dataRow.maxThresh);
                default:
                    return null;
            }
        }

        /**
		 * Called when the user updates a cell in the table. This is used to let
		 * the user set the thresholds.
		 * 
		 * @param o
		 *            the value entered by the user
		 * @param row
		 *            the row of the cell
		 * @param col
		 *            the column of the cell
		 */
        public void setValueAt(Object o, int row, int col) {
            DataRow dataRow = (DataRow) rows.get(row);
            if (dataRow == BLANK_ROW) {
                return;
            }
            if (o == null) {
                return;
            }
            if (col > 2 && !maxMinVisible) {
                col += 2;
            }
            String value = o.toString().trim();
            switch(col) {
                case 5:
                    try {
                        double minThreshold;
                        if (value.length() == 0) {
                            minThreshold = Double.NEGATIVE_INFINITY;
                        } else {
                            minThreshold = Double.parseDouble(value);
                        }
                        if (minThreshold <= dataRow.maxThresh) {
                            dataRow.minThresh = minThreshold;
                            properties.setProperty("minThreshold_" + dataRow.getName(), Double.toString(minThreshold));
                            fireTableCellUpdated(row, col);
                        }
                    } catch (Throwable e) {
                    }
                    break;
                case 6:
                    try {
                        double maxThreshold;
                        if (value.length() == 0) {
                            maxThreshold = Double.POSITIVE_INFINITY;
                        } else {
                            maxThreshold = Double.parseDouble(value);
                        }
                        if (maxThreshold >= dataRow.minThresh) {
                            dataRow.maxThresh = maxThreshold;
                            properties.setProperty("maxThreshold_" + dataRow.getName(), Double.toString(maxThreshold));
                            fireTableCellUpdated(row, col);
                        }
                    } catch (Throwable e) {
                    }
                    break;
            }
        }

        public boolean getMaxMinVisibile() {
            return maxMinVisible;
        }

        public boolean getThresholdVisible() {
            return thresholdVisible;
        }

        public void setMaxMinVisible(boolean maxMinVisible) {
            if (this.maxMinVisible != maxMinVisible) {
                this.maxMinVisible = maxMinVisible;
                fireTableStructureChanged();
            }
        }

        public void setThresholdVisible(boolean thresholdVisible) {
            if (this.thresholdVisible != thresholdVisible) {
                this.thresholdVisible = thresholdVisible;
                fireTableStructureChanged();
            }
        }
    }

    private class DataRow {

        String name;

        String unit;

        double min;

        double max;

        double minThresh;

        double maxThresh;

        double time;

        double value;

        double offset;

        boolean cleared;

        public DataRow(String name, String unit, double lowerThresh, double upperThresh) {
            this.name = name;
            this.unit = unit;
            this.minThresh = lowerThresh;
            this.maxThresh = upperThresh;
            clearData();
        }

        public String getName() {
            return name;
        }

        public String getUnit() {
            return unit;
        }

        public double getData() {
            return value;
        }

        public void setData(double timestamp, double data, String unit) {
            time = timestamp;
            value = data;
            this.unit = unit;
            min = cleared ? data : Math.min(min, data);
            max = cleared ? data : Math.max(max, data);
            cleared = false;
        }

        public double getTime() {
            return time;
        }

        public double getMinimum() {
            return min;
        }

        public double getMaximum() {
            return max;
        }

        public double getOffset() {
            return offset;
        }

        public void setOffset(double offset) {
            this.offset = offset;
        }

        public void clearData() {
            cleared = true;
        }

        public boolean isCleared() {
            return cleared;
        }
    }

    private class DoubleTableCellRenderer extends DefaultTableCellRenderer {

        /** serialization version identifier */
        private static final long serialVersionUID = 1835123361189568703L;

        private boolean showEngineeringFormat;

        private DecimalFormat decimalFormatter;

        private DecimalFormat engineeringFormatter;

        public DoubleTableCellRenderer() {
            this(false);
        }

        public DoubleTableCellRenderer(boolean showEngineeringFormat) {
            super();
            this.showEngineeringFormat = showEngineeringFormat;
            setHorizontalAlignment(SwingConstants.RIGHT);
            decimalFormatter = new DecimalFormat("0.000000000");
            engineeringFormatter = new DecimalFormat("##0.000000000E0");
        }

        public void setValue(Object value) {
            if (value != null && value instanceof Number) {
                if (showEngineeringFormat) {
                    setText(engineeringFormatter.format(value));
                } else {
                    setText(decimalFormatter.format(value));
                }
            } else {
                super.setValue(value);
            }
        }

        public void setShowEngineeringFormat(boolean showEngineeringFormat) {
            this.showEngineeringFormat = showEngineeringFormat;
        }
    }

    /**
	 * An inner class that renders the data value cell by coloring it when it is
	 * outside or approaching the threshold intervals.
	 */
    private class DataTableCellRenderer extends DoubleTableCellRenderer {

        /** serialization version identifier */
        private static final long serialVersionUID = -2910432979644162805L;

        public DataTableCellRenderer() {
            super();
        }

        /**
		 * a method that will color the data value cell based on it's proximity
		 * to some threshold
		 */
        public Component getTableCellRendererComponent(JTable aTable, Object aNumberValue, boolean aIsSelected, boolean aHasFocus, int aRow, int aColumn) {
            Component renderer = super.getTableCellRendererComponent(aTable, aNumberValue, aIsSelected, aHasFocus, aRow, aColumn);
            if (aIsSelected) {
                return renderer;
            } else if (aNumberValue == null || !(aNumberValue instanceof Number)) {
                renderer.setBackground(Color.white);
                renderer.setForeground(Color.black);
                return renderer;
            }
            double numberValue = ((Number) aNumberValue).doubleValue();
            DataRow dataRow = ((DataTableModel) aTable.getModel()).getRowAt(aRow);
            double minThresh = dataRow.minThresh;
            double maxThresh = dataRow.maxThresh;
            boolean criticalOnly = (minThresh == Double.NEGATIVE_INFINITY) || (maxThresh == Double.POSITIVE_INFINITY);
            if (criticalOnly) {
                if (numberValue < minThresh || numberValue > maxThresh) {
                    renderer.setBackground(Color.red);
                    renderer.setForeground(Color.white);
                } else {
                    renderer.setBackground(Color.white);
                    renderer.setForeground(Color.black);
                }
                return renderer;
            }
            double warningThresh = WARNING_PERCENTAGE * (maxThresh - minThresh);
            double criticalThresh = CRITICAL_PERCENTAGE * (maxThresh - minThresh);
            double warningMinThresh = minThresh + warningThresh;
            double criticalMinThresh = minThresh + criticalThresh;
            double warningMaxThresh = maxThresh - warningThresh;
            double criticalMaxThresh = maxThresh - criticalThresh;
            if (numberValue < criticalMinThresh || numberValue > criticalMaxThresh) {
                renderer.setBackground(Color.red);
                renderer.setForeground(Color.white);
            } else if (numberValue < warningMinThresh || numberValue > warningMaxThresh) {
                renderer.setBackground(Color.yellow);
                renderer.setForeground(Color.black);
            } else {
                renderer.setBackground(Color.white);
                renderer.setForeground(Color.black);
            }
            return renderer;
        }
    }

    class CheckBoxGroup {

        boolean selected;

        List<AbstractButton> checkBoxes;

        public CheckBoxGroup() {
            this(false);
        }

        public CheckBoxGroup(boolean selected) {
            this.selected = selected;
            checkBoxes = new ArrayList<AbstractButton>();
        }

        public void addCheckBox(AbstractButton checkBox) {
            checkBoxes.add(checkBox);
            checkBox.setSelected(selected);
        }

        public void removeCheckBox(AbstractButton checkBox) {
            checkBoxes.remove(checkBox);
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            for (AbstractButton checkBox : checkBoxes) {
                checkBox.setSelected(selected);
            }
        }

        public boolean isSelected() {
            return selected;
        }
    }
}
