package gov.sns.apps.viewers.arraypvviewer.utils;

import java.awt.*;
import java.awt.Color;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import gov.sns.tools.swing.FortranNumberFormat;

/**
 *  Shows the table with PVs names and attributes related to the average value,
 *  RMS around the average and wrapping attribute.
 *
 *@author    shishlo
 */
public class ArrayPVsTable {

    private JPanel mainLocalPanel = new JPanel(new BorderLayout());

    private JPanel tablePanel = new JPanel(new BorderLayout());

    private FortranNumberFormat fmtVal = new FortranNumberFormat("G11.4");

    private JPanel leftTablePanel = new JPanel(new BorderLayout());

    private JPanel centerTablePanel = new JPanel(new BorderLayout());

    private JPanel rightTablePanel = new JPanel(new BorderLayout());

    private JTable leftTable = null;

    private JTable centerTable = null;

    private JTable rightTable = null;

    private String[] columnNamesLeftTable = { "Wrap. Phase", "Avrg.", "Sigma" };

    private String[] columnNamesCenterTable = { "PV Name" };

    private String[] columnNamesRightTable = { "Color" };

    private TableModel leftTableModel = null;

    private TableModel centerTableModel = null;

    private TableModel rightTableModel = null;

    private TableModelEvent leftTableModelEvent = null;

    private TableModelEvent centerTableModelEvent = null;

    private TableModelEvent rightTableModelEvent = null;

    private Vector arrayPVs = new Vector();

    private Vector arrayPVsExt = new Vector();

    private Vector changeListenersV = new Vector();

    /**
	 *  Constructor for the ArrayPVsTable object
	 *
	 *@param  arrayPVsIn  Description of the Parameter
	 */
    public ArrayPVsTable(Vector arrayPVsIn) {
        arrayPVsExt = arrayPVsIn;
        fmtVal.setFixedLength(true);
        leftTableModel = new AbstractTableModel() {

            @Override
            public String getColumnName(int col) {
                return columnNamesLeftTable[col].toString();
            }

            public int getRowCount() {
                return arrayPVs.size();
            }

            public int getColumnCount() {
                return 3;
            }

            public Object getValueAt(int row, int col) {
                ArrayViewerPV arrPV = (ArrayViewerPV) arrayPVs.get(row);
                if (col == 0) {
                    boolean wrap = arrPV.getWrapDataProperty();
                    return new Boolean(wrap);
                }
                if (col == 1) {
                    return fmtVal.format(arrPV.getAvgValue());
                }
                return fmtVal.format(arrPV.getSigmaValue());
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                if (col == 0) {
                    return true;
                }
                return false;
            }

            @Override
            public void setValueAt(Object value, int row, int col) {
                if (col == 0) {
                    ArrayViewerPV arrPV = (ArrayViewerPV) arrayPVs.get(row);
                    arrPV.setWrapDataProperty(((Boolean) value).booleanValue());
                    fireTableCellUpdated(row, col);
                }
            }

            @Override
            public Class getColumnClass(int c) {
                return getValueAt(0, c).getClass();
            }
        };
        centerTableModel = new AbstractTableModel() {

            @Override
            public String getColumnName(int col) {
                return columnNamesCenterTable[col].toString();
            }

            public int getRowCount() {
                return arrayPVs.size();
            }

            public int getColumnCount() {
                return 1;
            }

            public Object getValueAt(int row, int col) {
                ArrayViewerPV arrPV = (ArrayViewerPV) arrayPVs.get(row);
                return arrPV.getChannelName();
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        rightTableModel = new AbstractTableModel() {

            @Override
            public String getColumnName(int col) {
                return columnNamesRightTable[col].toString();
            }

            public int getRowCount() {
                return arrayPVs.size();
            }

            public int getColumnCount() {
                return 1;
            }

            public Object getValueAt(int row, int col) {
                ArrayViewerPV arrPV = (ArrayViewerPV) arrayPVs.get(row);
                return arrPV.getColor();
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public Class getColumnClass(int c) {
                return getValueAt(0, c).getClass();
            }
        };
        leftTable = new JTable(leftTableModel);
        centerTable = new JTable(centerTableModel);
        rightTable = new JTable(rightTableModel);
        rightTable.setDefaultRenderer(Color.red.getClass(), new TableCellRenderer() {

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel jl = new JLabel(" ");
                jl.setOpaque(true);
                ArrayViewerPV arrPV = (ArrayViewerPV) arrayPVs.get(row);
                jl.setBackground(arrPV.getColor());
                return jl;
            }
        });
        leftTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        leftTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        leftTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        leftTable.setGridColor(Color.black);
        centerTable.setGridColor(Color.black);
        rightTable.setGridColor(Color.black);
        leftTable.setShowGrid(true);
        centerTable.setShowGrid(true);
        rightTable.setShowGrid(true);
        leftTablePanel.add(leftTable.getTableHeader(), BorderLayout.PAGE_START);
        leftTablePanel.add(leftTable, BorderLayout.CENTER);
        centerTablePanel.add(centerTable.getTableHeader(), BorderLayout.PAGE_START);
        centerTablePanel.add(centerTable, BorderLayout.CENTER);
        rightTablePanel.add(rightTable.getTableHeader(), BorderLayout.PAGE_START);
        rightTablePanel.add(rightTable, BorderLayout.CENTER);
        tablePanel.add(leftTablePanel, BorderLayout.WEST);
        tablePanel.add(centerTablePanel, BorderLayout.CENTER);
        tablePanel.add(rightTablePanel, BorderLayout.EAST);
        JScrollPane scrollpane = new JScrollPane(tablePanel);
        mainLocalPanel.add(scrollpane, BorderLayout.CENTER);
        leftTableModel.addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                ActionEvent stateChangedAction = new ActionEvent(arrayPVs, 0, "changed");
                int nL = changeListenersV.size();
                for (int i = 0; i < nL; i++) {
                    ((ActionListener) changeListenersV.get(i)).actionPerformed(stateChangedAction);
                }
            }
        });
        leftTableModelEvent = new TableModelEvent(leftTableModel);
        centerTableModelEvent = new TableModelEvent(centerTableModel);
        rightTableModelEvent = new TableModelEvent(rightTableModel);
        doLayout();
    }

    /**
	 *  Returns the main panel of these tables
	 *
	 *@return    The panel value
	 */
    public JPanel getPanel() {
        return mainLocalPanel;
    }

    /**
	 *  Adds the change listener
	 *
	 *@param  actionListener  The change listener
	 */
    public void addChangeListener(ActionListener actionListener) {
        if (actionListener != null) {
            changeListenersV.add(actionListener);
        }
    }

    /**
	 *  Removes the change listener
	 *
	 *@param  actionListener  The change listener
	 */
    public void removeChangeListener(ActionListener actionListener) {
        changeListenersV.remove(actionListener);
    }

    /**
	 *  Removes all change listeners
	 */
    public void removeChangeListeners() {
        changeListenersV.clear();
    }

    /**
	 *  Sets the font attribute of the ArrayPVsTable object
	 *
	 *@param  fnt  The new font value
	 */
    public void setFont(Font fnt) {
        leftTable.setFont(fnt);
        centerTable.setFont(fnt);
        rightTable.setFont(fnt);
        leftTable.getTableHeader().setFont(fnt);
        centerTable.getTableHeader().setFont(fnt);
        rightTable.getTableHeader().setFont(fnt);
        leftTable.setRowHeight(fnt.getSize() + 2);
        centerTable.setRowHeight(fnt.getSize() + 2);
        rightTable.setRowHeight(fnt.getSize() + 2);
        leftTable.getColumnModel().getColumn(0).setPreferredWidth(fnt.getSize() * 7);
        leftTable.getColumnModel().getColumn(1).setPreferredWidth(fnt.getSize() * 7);
        leftTable.getColumnModel().getColumn(2).setPreferredWidth(fnt.getSize() * 7);
        rightTable.getColumnModel().getColumn(0).setPreferredWidth(fnt.getSize() * 4);
    }

    /**
	 *  Layout all tables
	 */
    public void doLayout() {
        int n = arrayPVsExt.size();
        arrayPVs.clear();
        for (int i = 0; i < n; i++) {
            ArrayViewerPV arrPV = (ArrayViewerPV) arrayPVsExt.get(i);
            if (arrPV.getArrayDataPV().getSwitchOn()) {
                arrayPVs.add(arrPV);
                arrPV.updateAvgAndSigma();
            }
        }
        leftTable.tableChanged(leftTableModelEvent);
        centerTable.tableChanged(centerTableModelEvent);
        rightTable.tableChanged(rightTableModelEvent);
    }
}
