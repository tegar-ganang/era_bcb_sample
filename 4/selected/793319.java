package org.statcato.spreadsheet;

import org.statcato.file.FileChooserUtils;
import org.statcato.file.ExtensionFileFilter;
import org.statcato.utils.*;
import org.statcato.statistics.BasicStatistics;
import org.statcato.file.FileOperations;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.ListSelectionModel;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.awt.Component;
import javax.swing.text.*;
import javax.swing.undo.*;
import org.apache.poi.hssf.usermodel.*;
import org.statcato.Statcato;

/**
 * A spreadsheet represented by a table of <code>Cell</code>s.
 *   
 * @author Margaret Yau
 * @version %I%, %G%
 * @see Cell
 * @see SpreadsheetModel
 * @since 1.0
 */
public class Spreadsheet extends JTable implements StateEditable {

    private boolean dragged = false;

    private int startDraggedColumn = -1;

    private int lastSelectedColumn = -1;

    private File savedFile = null;

    private boolean changed = false;

    private Statcato app;

    protected UndoableEditSupport undoableEditSupport;

    protected static final String MODEL = "MODEL";

    protected static final String STATUS = "STATUS";

    private StateEdit edit;

    /**
     * Constructor, given the parent frame.
     * 
     * @param app parent frame
     */
    public Spreadsheet(Statcato app) {
        super(new SpreadsheetModel(app));
        this.app = app;
        initialize();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);
        int realColumnIndex = convertColumnIndexToModel(colIndex);
        int realRowIndex = convertRowIndexToModel(rowIndex);
        String contents = getValueAt(realRowIndex, realColumnIndex).getContents();
        return (contents.equals("") ? null : contents);
    }

    /**
     * Constructor, given the parent frame, the number of rows, and
     * the number of columns.
     * 
     * @param app parent frame
     * @param numRows number of rows
     * @param numColumns number of columns
     */
    public Spreadsheet(Statcato app, int numRows, int numColumns) {
        super(new SpreadsheetModel(app, numRows, numColumns));
        this.app = app;
        initialize();
    }

    public void addUndoableEditListener(UndoableEditListener undoableEditListener) {
        undoableEditSupport.addUndoableEditListener(undoableEditListener);
    }

    public void removeUndoableEditListener(UndoableEditListener undoableEditListener) {
        undoableEditSupport.removeUndoableEditListener(undoableEditListener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void storeState(Hashtable state) {
        state.put(MODEL, ((SpreadsheetModel) getModel()).getTabDelimitedValues());
        state.put(STATUS, new Boolean(changed));
    }

    @Override
    public void restoreState(Hashtable state) {
        if (state != null) {
            String model = (String) state.get(MODEL);
            if (model != null) {
                clearAllCells();
                setData(model);
            }
            Object changedStatus = state.get(STATUS);
            if (changedStatus != null) {
                changed = (Boolean) changedStatus;
                if (changed) setChangedStatus(); else setUnchangedStatus();
            }
        } else System.out.println("unexpected error: restore state is null");
    }

    /**
     * Sets up this object.  Sets properties pertaining to {@link JTable}.
     * Adds listeners to table header, table model, and keys.  Initializes
     * adapter for Excel.
     */
    private void initialize() {
        undoableEditSupport = new UndoableEditSupport();
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setDefaultColumnWidths();
        setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        setCellSelectionEnabled(true);
        setFillsViewportHeight(true);
        JTableHeader atableHeader = getTableHeader();
        atableHeader.setReorderingAllowed(false);
        addColumnHeaderMouseListeners();
        getModel().addTableModelListener(this);
        setDragEnabled(true);
        addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    clearSelectedCells();
                } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteSelectedCells();
                } else if (!isEditing() && !e.isActionKey() && !e.isControlDown() && !e.isAltDown() && e.getKeyCode() != KeyEvent.VK_SHIFT) {
                    int rowIndexStart = getSelectedRow();
                    int colIndexStart = getSelectedColumn();
                    if (rowIndexStart == -1 || colIndexStart == -1) return;
                    editCellAt(rowIndexStart, colIndexStart);
                    Component c = getEditorComponent();
                    if (c instanceof JTextComponent) ((JTextComponent) c).setText("");
                } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V) {
                    setChangedStatus();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (!e.isControlDown()) setChangedStatus();
            }
        });
        ExcelAdapter ea = new ExcelAdapter(this);
    }

    /**
     * Sets the status to changed and adds an asterisk to the current tab table.  
     */
    public void setChangedStatus() {
        if (!changed) {
            changed = true;
            app.setCurrentTabTitle(app.getCurrentTabTitle() + "*");
        }
    }

    /**
     * Sets the status to unchanged and removes any existing asterisk in 
     * the current tab title.
     */
    public void setUnchangedStatus() {
        changed = false;
        String title = app.getCurrentTabTitle();
        if (title.endsWith("*")) app.setCurrentTabTitle(app.getCurrentTabTitle().substring(0, title.length() - 1));
    }

    /**
     * Returns the changed status.
     * 
     * @return changed status
     */
    public boolean getChangedStatus() {
        return changed;
    }

    /**
     * Closes the file associated with this spreadsheet, if any.
     */
    public void closeFile() {
        savedFile = null;
    }

    /**
     * Sets the default widths of the table columns.
     */
    public void setDefaultColumnWidths() {
        TableColumn column = null;
        for (int i = 0; i < getColumnCount(); ++i) {
            column = this.getColumnModel().getColumn(i);
            column.setPreferredWidth(70);
        }
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (row == 0) {
            return new VarHeaderRenderer();
        } else return new SpreadsheetCellRenderer();
    }

    /**
     * Sets up mouse and motion listeneres and adds to the table header.
     */
    private void addColumnHeaderMouseListeners() {
        MouseMotionAdapter motionListener = new MouseMotionAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!dragged) return;
                JTableHeader header = getTableHeader();
                TableColumnModel columns = header.getColumnModel();
                int column = header.columnAtPoint(e.getPoint());
                ListSelectionModel selection = columns.getSelectionModel();
                if (column == -1) {
                    selection.clearSelection();
                    return;
                }
                int count = getRowCount();
                if (count != 0) setRowSelectionInterval(0, count - 1);
                if (column < startDraggedColumn) {
                    selection.setSelectionInterval(column, startDraggedColumn);
                } else {
                    selection.setSelectionInterval(startDraggedColumn, column);
                }
            }
        };
        MouseAdapter mouseAdapter = new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                dragged = false;
                startDraggedColumn = -1;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                JTableHeader header = getTableHeader();
                TableColumnModel columns = header.getColumnModel();
                int column = header.columnAtPoint(e.getPoint());
                ListSelectionModel selection = columns.getSelectionModel();
                if (dragged) {
                    selection.clearSelection();
                    dragged = false;
                    startDraggedColumn = -1;
                }
                if (column == -1) return;
                dragged = true;
                int count = getRowCount();
                if (count != 0) setRowSelectionInterval(0, count - 1);
                selection.addSelectionInterval(column, column);
                startDraggedColumn = column;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                JTableHeader header = getTableHeader();
                TableColumnModel columns = header.getColumnModel();
                int column = header.columnAtPoint(e.getPoint());
                if (column == -1) return;
                int count = getRowCount();
                if (count != 0) setRowSelectionInterval(0, count - 1);
                ListSelectionModel selection = columns.getSelectionModel();
                int anchor = selection.getAnchorSelectionIndex();
                int lead = selection.getLeadSelectionIndex();
                if (e.isShiftDown()) {
                    if (lastSelectedColumn != -1) {
                        if (lastSelectedColumn <= column) selection.setSelectionInterval(lastSelectedColumn, column); else selection.setSelectionInterval(column, lastSelectedColumn);
                    }
                } else if (e.isControlDown()) {
                } else selection.setSelectionInterval(column, column);
                lastSelectedColumn = column;
            }
        };
        JTableHeader header = getTableHeader();
        header.addMouseListener(mouseAdapter);
        header.addMouseMotionListener(motionListener);
    }

    /**
     * Converts a vector of column numbers to a vector of their 
     * corresponding column names.
     * 
     * @param ColumnsNumbers vector of <code>Integer</code> (column numbers)
     * @return a vector of <code>String</code> (column descriptions)
     */
    private Vector<String> convertColumnNumbersToDescriptions(Vector<Integer> ColumnsNumbers) {
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        Vector<String> ColumnsDesc = new Vector<String>();
        for (Enumeration e = ColumnsNumbers.elements(); e.hasMoreElements(); ) {
            int col = ((Integer) e.nextElement()).intValue();
            String desc = DataSpreadsheetModel.getColumnName(col);
            String variable = DataSpreadsheetModel.getVariableName(col);
            desc += "  " + (variable.length() > 20 ? variable.substring(0, 16) + "..." : variable);
            ColumnsDesc.addElement(desc);
        }
        return ColumnsDesc;
    }

    /**
     * Populates the given list with columns in this object that contain data.
     * 
     * @param List list to which columns will be added
     */
    @SuppressWarnings("unchecked")
    public void populateColumnsList(JList List) {
        List.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        List.setLayoutOrientation(JList.VERTICAL_WRAP);
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        Vector<Integer> ColumnsNumbers = (Vector<Integer>) DataSpreadsheetModel.getColumnsWithData();
        List.setListData(convertColumnNumbersToDescriptions(ColumnsNumbers));
    }

    /**
     * Populates the given mutable list with columns in this object that 
     * contain data.
     * 
     * @param List mutable list to which columns will be added
     */
    @SuppressWarnings("unchecked")
    public void populateMutableColumnsList(JList List) {
        DefaultListModel listModel = (DefaultListModel) List.getModel();
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        Vector<Integer> ColumnsNumbers = (Vector<Integer>) DataSpreadsheetModel.getColumnsWithData();
        Vector<String> ColumnsLabels = convertColumnNumbersToDescriptions(ColumnsNumbers);
        for (Enumeration e = ColumnsLabels.elements(); e.hasMoreElements(); ) listModel.addElement(e.nextElement());
    }

    /**
     * Populates the given list with all existing columns in this object
     * 
     * @param List list to which columns will be added
     */
    @SuppressWarnings("unchecked")
    public void populateAllColumnsList(JList List) {
        List.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        List.setLayoutOrientation(JList.VERTICAL_WRAP);
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        Vector<Integer> ColumnsNumbers = (Vector<Integer>) DataSpreadsheetModel.getAllColumnNumbers();
        List.setListData(convertColumnNumbersToDescriptions(ColumnsNumbers));
    }

    /**
     * Populates the given combo box with columns in this object that contain 
     * data.
     * 
     * @param Combo combo box to which columns are added
     */
    @SuppressWarnings("unchecked")
    public void populateComboBox(JComboBox Combo) {
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        Vector<Integer> ColumnsNumbers = (Vector<Integer>) DataSpreadsheetModel.getColumnsWithData();
        Vector<String> ColumnsDesc = convertColumnNumbersToDescriptions(ColumnsNumbers);
        Combo.removeAllItems();
        Combo.addItem("");
        Combo.setSelectedItem("");
        for (Enumeration e = ColumnsDesc.elements(); e.hasMoreElements(); ) Combo.addItem(e.nextElement());
    }

    /**
     * Returns the column number embedded in the constructed string
     * "Cx variable_name".
     * 
     * @param str string to be parsed
     * @return integer (column number)
     */
    public int parseColumnNumber(String str) {
        String[] items = str.split(" ");
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        return DataSpreadsheetModel.getColumnNumber(items[0]);
    }

    /**
     * Returns the column at the given column number.
     * 
     * @param col column number
     * @return vector of <code>Cell</code> (column)
     */
    public Vector<Cell> getColumn(int col) {
        return ((SpreadsheetModel) getModel()).getColumn(col);
    }

    /**
     * Returns the row at the given row number.
     * 
     * @param row row number
     * @return vector of <code>Cell</code> (row)
     */
    public Vector<Cell> getRow(int row) {
        return ((SpreadsheetModel) getModel()).getRow(row);
    }

    /**
     * Returns the column number represented by the given column label.
     * 
     * @param str string representing the column label
     * @return column number
     */
    public int getColumnNumber(String str) {
        return ((SpreadsheetModel) getModel()).getColumnNumber(str);
    }

    /**
     * Returns the full column label (Cx variable name) for the given column
     * number.
     *
     * @param column column number
     * @return full column label
     */
    public String getColumnFullLabel(int column) {
        SpreadsheetModel model = (SpreadsheetModel) getModel();
        return model.getColumnLabel(column) + " " + model.getVariableName(column);
    }

    /**
     * Returns a vector of column numbers represented by the given column label
     * range (for example, c1-c30).
     * 
     * @param str a string that has a single column label or one that
     * represents a range of columns (e.g. c1-c30)
     * @return a vector of Integers (column numbers)
     */
    public Vector<Integer> getColumnNumbers(String str) {
        int dash = str.indexOf('-');
        Vector<Integer> columns = new Vector<Integer>();
        if (dash == -1) {
            int col = getColumnNumber(str);
            if (col != -1) columns.addElement(new Integer(col));
        } else {
            int min = getColumnNumber(str.substring(0, dash));
            int max = getColumnNumber(str.substring(dash + 1));
            if (min != -1 && max != -1) {
                if (min > max) {
                    int temp = max;
                    max = min;
                    min = temp;
                }
                for (int i = min; i <= max; ++i) {
                    columns.addElement(new Integer(i));
                }
            }
        }
        return columns;
    }

    /**
     * Returns a vector of column numbers represented by the given string,
     * which contains column labels separated by spaces (e.g. C1  C2-30)
     * A continuous range of columns is specified by Ca-b, where a is the 
     * beginning column number and b is the end column number.
     * 
     * @param str string containing column label delimited by spaces
     * @return a vector of column numbers
     */
    public Vector<Integer> getColumnNumbersFromString(String str) {
        Object[] columns = HelperFunctions.parseString(str).toArray();
        Vector<Integer> nums = new Vector<Integer>();
        for (int x = 0; x < columns.length; x++) {
            String label = (String) columns[x];
            if (label.equals("")) {
                continue;
            } else {
                Vector<Integer> ColumnNums = getColumnNumbers(label);
                if (ColumnNums.size() == 0) {
                    return null;
                } else {
                    for (int i = 0; i < ColumnNums.size(); ++i) {
                        nums.addElement(ColumnNums.elementAt(i));
                    }
                }
            }
        }
        return nums;
    }

    public int[] getColumnNumbersArrayFromString(String str) {
        Vector<Integer> v = getColumnNumbersFromString(str);
        return HelperFunctions.ConvertIntegerVectorToArray(v);
    }

    /**
     * Return the row number represented by the given row label.
     * 
     * @param str string representating the row label
     * @return row number
     */
    public int getRowNumber(String str) {
        return ((SpreadsheetModel) getModel()).getRowNumber(str);
    }

    /**
     * Returns the value at the given row and column numbers.
     * 
     * @param row row number
     * @param col column number
     * @return cell at the given row and column
     */
    @Override
    public Cell getValueAt(int row, int col) {
        return (Cell) ((SpreadsheetModel) getModel()).getValueAt(row, col);
    }

    /**
     * Returns the row number of the last row that contains data.
     * 
     * @return row number of the last non-empty row
     */
    public int getLastRowNumber() {
        int lastRow = 0;
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        for (int r = 1; r < DataSpreadsheetModel.getRowCount(); ++r) {
            boolean hasData = false;
            for (int c = 0; c < DataSpreadsheetModel.getColumnCount(); ++c) {
                Cell data = (Cell) DataSpreadsheetModel.getValueAt(r, c);
                if (SpreadsheetModel.hasData(data)) hasData = true;
            }
            if (hasData) {
                lastRow = r;
            }
        }
        return lastRow;
    }

    /**
     * Sets the column at the given column number to the given vector of 
     * strings.
     * 
     * @param column column number
     * @param data vector of strings
     */
    public void setColumn(int column, Vector<String> data) {
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        edit = new StateEdit(this, "set column " + DataSpreadsheetModel.getColumnLabel(column));
        if (column >= getColumnCount()) {
            for (int c = getColumnCount(); c <= column; ++c) {
                DataSpreadsheetModel.insertColumn(c);
            }
        }
        if (data.size() >= getRowCount()) {
            for (int r = getRowCount(); r <= data.size(); ++r) {
                DataSpreadsheetModel.insertRow(r);
                updateRowHeader();
            }
        }
        DataSpreadsheetModel.setColumn(column, data);
        setChangedStatus();
        edit.end();
        undoableEditSupport.postEdit(edit);
    }

    /**
     * Sets the column at the given column number to the given vector of 
     * Cells.
     * 
     * @param column column number
     * @param data vector of Cells
     */
    public void setCellColumn(int column, Vector<Cell> data) {
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        edit = new StateEdit(this, "set column " + DataSpreadsheetModel.getColumnLabel(column));
        if (column >= getColumnCount()) {
            for (int c = getColumnCount(); c <= column; ++c) {
                DataSpreadsheetModel.insertColumn(c);
            }
        }
        if (data.size() >= getRowCount()) {
            for (int r = getRowCount(); r <= data.size(); ++r) {
                DataSpreadsheetModel.insertRow(r);
                updateRowHeader();
            }
        }
        DataSpreadsheetModel.setCellColumn(column, data);
        setChangedStatus();
        edit.end();
        undoableEditSupport.postEdit(edit);
    }

    /**
     * Set the row at the given row number to the given vector of Strings.
     * 
     * @param row row number
     * @param data vector of Strings
     */
    public void setRow(int row, Vector<String> data) {
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        edit = new StateEdit(this, "set row " + row);
        if (row >= getRowCount()) {
            for (int r = getRowCount(); r <= row; ++r) {
                DataSpreadsheetModel.insertRow(r);
                updateRowHeader();
            }
        }
        if (data.size() >= getColumnCount()) {
            for (int c = getColumnCount(); c <= data.size(); ++c) {
                DataSpreadsheetModel.insertColumn(c);
            }
        }
        DataSpreadsheetModel.setRow(row, data);
        setChangedStatus();
        edit.end();
        undoableEditSupport.postEdit(edit);
    }

    /**
     * Set the row at the given row number to the given vector of cells.
     *
     * @param row row number
     * @param data vector of Cells
     */
    public void setCellRow(int row, Vector<Cell> data) {
        setRow(row, HelperFunctions.ConvertCellVectorToStringVector(data));
    }

    /**
     * Sets the variables row to the given vector of Strings.
     * 
     * @param data vector of Strings
     */
    public void setVariablesRow(Vector<String> data) {
        setRow(0, data);
    }

    /**
     * Sets the variable label for the given column to the given string if
     * the label is empty.
     *
     * @param column    column number
     * @param name      variable label
     */
    public void setVariableLabel(int column, String name) {
        if (!getValueAt(0, column).hasData()) setValueAt(name, 0, column);
    }

    /**
     * Converts the given array of Objects representing column labels
     * to column numbers.
     * 
     * @param Labels array of Objects representing column labels
     * @return array of integer representing column numbers
     */
    public int[] convertColumnLabelsToNumbers(Object[] Labels) {
        int[] columnNumbers = new int[Labels.length];
        for (int i = 0; i < Labels.length; ++i) {
            String value = Labels[i].toString();
            columnNumbers[i] = parseColumnNumber(value);
        }
        return columnNumbers;
    }

    /**
     * Returns the rows containing data at the given columns.
     * 
     * @param columns array of integers (column numbers)
     * @return vector of vector of Cells representing non-empty rows
     */
    public Vector<Vector<Cell>> getRowsWithDataAtGivenColumns(int[] columns) {
        Vector<Vector<Cell>> Rows = new Vector<Vector<Cell>>();
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        int numEmptyRowsSinceLastNonEmpty = 0;
        for (int r = 1; r < DataSpreadsheetModel.getRowCount(); ++r) {
            boolean hasData = false;
            Vector<Cell> Row = new Vector<Cell>(0);
            for (int c = 0; c < columns.length; ++c) {
                Cell data = (Cell) DataSpreadsheetModel.getValueAt(r, columns[c]);
                if (SpreadsheetModel.hasData(data)) hasData = true;
                Row.addElement(data);
            }
            if (hasData) {
                for (int i = 0; i < numEmptyRowsSinceLastNonEmpty; ++i) {
                    Rows.addElement(new Vector<Cell>(0));
                }
                Rows.addElement(Row);
                numEmptyRowsSinceLastNonEmpty = 0;
            } else numEmptyRowsSinceLastNonEmpty++;
        }
        return Rows;
    }

    /**
     * Returns the row number of the last non-empty row.
     * 
     * @return integer (row number of the last non-empty row)
     */
    public int getLastNonEmptyRow() {
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        return DataSpreadsheetModel.getLastNonEmptyRow();
    }

    /**
     * Returns the column number of the last non-empty column in the given row.
     * 
     * @return integer (column number of the last non-empty column)
     */
    public int getLastNonEmptyColumn(int row) {
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        return DataSpreadsheetModel.getLastNonEmptyColumn(row);
    }

    /**
     * Returns the column number of the last non-empty column.
     * 
     * @return integer (column number of the last non-empty column)
     */
    public int getLastNonEmptyColumn() {
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        return DataSpreadsheetModel.getLastNonEmptyColumn();
    }

    /**
     * Puts data in the tab-delimited string in this spreadsheet.
     * Sets the status to changed.
     * 
     * @param data tab-delimited string
     */
    public void setData(String data) {
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        String[] lines = data.split("\\n");
        for (int i = 0; i < lines.length; ++i) {
            String[] items = lines[i].split("\\t");
            for (int j = 0; j < items.length; ++j) {
                setStringValueAt(items[j], i, j);
            }
        }
        setChangedStatus();
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        if (value.getClass() != String.class || !((String) value).equals("")) edit = new StateEdit(this, "set cell R" + row + ", " + DataSpreadsheetModel.getColumnLabel(col));
        if (row >= getRowCount()) {
            for (int r = getRowCount(); r <= row; ++r) {
                DataSpreadsheetModel.insertRow(r);
                updateRowHeader();
            }
        }
        if (col >= getColumnCount()) {
            for (int c = getColumnCount(); c <= col; ++c) {
                DataSpreadsheetModel.insertColumn(c);
            }
        }
        DataSpreadsheetModel.setValueAt(value, row, col);
        if (value.getClass() != String.class || !((String) value).equals("")) {
            edit.end();
            undoableEditSupport.postEdit(edit);
        }
    }

    /**
     * Sets the value at the given row and column numbers to the given string.
     * 
     * @param value string 
     * @param row   row number
     * @param col   column number
     */
    public void setStringValueAt(String value, int row, int col) {
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        if (row >= getRowCount()) {
            for (int r = getRowCount(); r <= row; ++r) {
                DataSpreadsheetModel.insertRow(r);
                updateRowHeader();
            }
        }
        if (col >= getColumnCount()) {
            for (int c = getColumnCount(); c <= col; ++c) {
                DataSpreadsheetModel.insertColumn(c);
            }
        }
        DataSpreadsheetModel.setStringValueAt(value, row, col);
    }

    /**
     * Puts data in the tab-delimited string in this spreadsheet.
     * Does not set status to changed.
     * 
     * @param data tab-delimited string
     */
    public void setDataUnchangedStatus(String data) {
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        String[] lines = data.split("\\n");
        for (int i = 0; i < lines.length; ++i) {
            String[] items = lines[i].split("\\t");
            for (int j = 0; j < items.length; ++j) {
                setStringValueAt(items[j], i, j);
            }
        }
        app.undoManager.discardAllEdits();
    }

    /**
     * Puts data in the given vectors of vectors of strings in this 
     * spreadsheet.
     * 
     * @param data vectors of vectors of strings
     */
    public void setData(Vector<Vector<String>> data) {
        int row = data.size();
        int col = data.elementAt(0).size();
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        if (row >= getRowCount()) {
            for (int r = getRowCount(); r <= row; ++r) {
                DataSpreadsheetModel.insertRow(r);
                updateRowHeader();
            }
        }
        if (col >= getColumnCount()) {
            for (int c = getColumnCount(); c <= col; ++c) {
                DataSpreadsheetModel.insertColumn(c);
            }
        }
        ((SpreadsheetModel) getModel()).setData(data);
        setChangedStatus();
        app.undoManager.discardAllEdits();
    }

    /**
     * Get a subset of this spreadsheet specified by the given row and 
     * column range.
     * 
     * @param minRow minimum row number
     * @param minCol minimum column number
     * @param maxRow maximum row number
     * @param maxCol maximum column number
     * @return a spreadsheet containing a subset of data
     */
    public Spreadsheet getSubTable(int minRow, int minCol, int maxRow, int maxCol) {
        Spreadsheet NewSS = new Spreadsheet(app, maxRow - minRow + 2, maxCol - minCol + 1);
        SpreadsheetModel DataSSModel = (SpreadsheetModel) getModel();
        SpreadsheetModel NewDataSSModel = (SpreadsheetModel) NewSS.getModel();
        Object value;
        int row, col;
        for (int i = minRow; i <= maxRow; ++i) {
            for (int j = minCol; j <= maxCol; ++j) {
                value = DataSSModel.getValueAt(i, j);
                row = i - minRow + 1;
                col = j - minCol;
                NewDataSSModel.setValueAt(value, row, col);
            }
        }
        return NewSS;
    }

    /**
     * Displays debug data.
     */
    public void display() {
        ((SpreadsheetModel) getModel()).printDebugData();
    }

    /**
     * Saves the contents of this spreadsheet to a file of one
     * following formats: excel, csv, tab-delimited.  If this spreadsheet
     * has not been saved previously or save as flag is true, 
     * opens a file chooser to allow user
     * select a file location.
     * 
     * @param frame parent frame
     * @param saveAs whether to save this spreadsheet as
     * @return file containing saved contents
     */
    public File writeToFile(JFrame frame, boolean saveAs) {
        String path = "";
        String extension = "";
        if (savedFile != null && !saveAs) {
            path = savedFile.getPath();
            extension = FileChooserUtils.getExtension(savedFile);
            writeFileHelper(frame, path, extension);
            return savedFile;
        } else {
            JFileChooser fc = new JFileChooser(FileOperations.getRecentDatasheet() == null ? null : FileOperations.getRecentDatasheet().getParentFile());
            ExtensionFileFilter ExcelFilter = new ExtensionFileFilter("Excel (*.xls)", "xls");
            ExtensionFileFilter CSVFilter = new ExtensionFileFilter("Comma-separated values(*.csv)", "csv");
            ExtensionFileFilter TDVFilter = new ExtensionFileFilter("Tab-delimited values (*.txt)", "txt");
            fc.addChoosableFileFilter(ExcelFilter);
            fc.addChoosableFileFilter(CSVFilter);
            fc.addChoosableFileFilter(TDVFilter);
            fc.setAcceptAllFileFilterUsed(false);
            int returnValue = fc.showSaveDialog(frame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                path = file.getPath();
                extension = "";
                javax.swing.filechooser.FileFilter filter = fc.getFileFilter();
                if (filter.equals(ExcelFilter)) {
                    extension = "xls";
                } else if (filter.equals(CSVFilter)) {
                    extension = "csv";
                } else {
                    extension = "txt";
                }
                if (!path.toLowerCase().endsWith("." + extension)) {
                    path += "." + extension;
                    file = new File(path);
                }
                if (file.exists()) {
                    System.out.println("file exists already");
                    Object[] options = { "Overwrite file", "Cancel" };
                    int choice = JOptionPane.showOptionDialog(frame, "The specified file already exists.  Overwrite existing file?", "Overwrite file?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                    if (choice != 0) return null;
                }
                writeFileHelper(frame, path, extension);
                savedFile = file;
                return file;
            }
            return null;
        }
    }

    /**
     * Writes the contents of this spreadsheet to a file at the given
     * file path and extension.
     * 
     * @param frame parent frame
     * @param path file path
     * @param extension file extension
     */
    private void writeFileHelper(JFrame frame, String path, String extension) {
        try {
            BufferedWriter Writer = new BufferedWriter(new FileWriter(path));
            String contents = "";
            if (extension != null && extension.equals("xls")) {
                System.out.println("write excel");
                HSSFWorkbook wb = new HSSFWorkbook();
                HSSFSheet sheet = wb.createSheet("new sheet");
                HSSFRow row;
                SpreadsheetModel sm = (SpreadsheetModel) getModel();
                int lastNonEmptyRow = getLastNonEmptyRow();
                int lastNonEmptyCol;
                for (int i = 0; i <= lastNonEmptyRow; i++) {
                    row = sheet.createRow((short) i);
                    lastNonEmptyCol = getLastNonEmptyColumn(i);
                    for (int j = 0; j <= lastNonEmptyCol; j++) {
                        Cell cell = (Cell) sm.getValueAt(i, j);
                        if (cell.isNumeric()) row.createCell((short) j).setCellValue(cell.getNumValue().doubleValue()); else row.createCell((short) j).setCellValue(cell.getContents());
                    }
                }
                FileOutputStream fileOut = new FileOutputStream(path);
                wb.write(fileOut);
                fileOut.close();
                setUnchangedStatus();
                return;
            }
            if (extension != null && extension.equals("csv")) {
                System.out.println("write csv");
                contents = ((SpreadsheetModel) getModel()).getCommaSeparatedValues();
            } else {
                System.out.println("write txt");
                contents = ((SpreadsheetModel) getModel()).getTabDelimitedValues();
            }
            String[] lines = contents.split("\n");
            for (int i = 0; i < lines.length; ++i) {
                Writer.write(lines[i]);
                Writer.newLine();
            }
            Writer.close();
            setUnchangedStatus();
        } catch (IOException e) {
            HelperFunctions.showErrorDialog(frame, "Write file failed!");
        }
    }

    /**
     * Sets the file that saves the data in this spreadsheet.
     * 
     * @param file
     */
    public void setFile(File file) {
        savedFile = file;
    }

    /**
     * Clear the contents in the selected cells without moving surrouding cells.
     */
    public void clearSelectedCells() {
        int[] selectedColumns = getSelectedColumns();
        int[] selectedRows = getSelectedRows();
        int minRow = BasicStatistics.min(selectedRows);
        int minCol = BasicStatistics.min(selectedColumns);
        int maxRow = BasicStatistics.max(selectedRows);
        int maxCol = BasicStatistics.max(selectedColumns);
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        edit = new StateEdit(this, "clear cells");
        for (int row = minRow; row <= maxRow; ++row) {
            for (int col = minCol; col <= maxCol; ++col) {
                DataSpreadsheetModel.clearCell(row, col);
            }
        }
        setRowSelectionInterval(minRow, minRow);
        setColumnSelectionInterval(minCol, minCol);
        setChangedStatus();
        edit.end();
        undoableEditSupport.postEdit(edit);
    }

    /**
     * Clear the contents in all cells without moving surrouding cells.
     */
    public void clearAllCells() {
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getColumnCount(); j++) {
                DataSpreadsheetModel.clearCell(i, j);
            }
        }
        setChangedStatus();
    }

    /**
     * Deletes the selected cells.
     */
    public void deleteSelectedCells() {
        int[] selectedColumns = getSelectedColumns();
        int[] selectedRows = getSelectedRows();
        int minRow = BasicStatistics.min(selectedRows);
        int minCol = BasicStatistics.min(selectedColumns);
        int maxRow = BasicStatistics.max(selectedRows);
        int maxCol = BasicStatistics.max(selectedColumns);
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        edit = new StateEdit(this, "delete cells");
        if ((maxRow - minRow) == (DataSpreadsheetModel.getRowCount() - 1)) {
            for (int col = minCol; col <= maxCol; ++col) {
                DataSpreadsheetModel.deleteColumn(minCol);
            }
        } else if ((maxCol - minCol) == (DataSpreadsheetModel.getColumnCount() - 1)) {
            if (minRow == 0) {
                DataSpreadsheetModel.deleteRow(0);
                minRow = 1;
            }
            for (int row = minRow; row <= maxRow; ++row) {
                DataSpreadsheetModel.deleteRow(minRow);
            }
        } else {
            DataSpreadsheetModel.deleteCells(minRow, maxRow, minCol, maxCol);
        }
        setRowSelectionInterval(minRow, minRow);
        setColumnSelectionInterval(minCol, minCol);
        setChangedStatus();
        edit.end();
        undoableEditSupport.postEdit(edit);
    }

    /**
     * Inserts a blank row at the end of the spreadsheet.
     */
    public void insertRow() {
        int row = getSelectedRow();
        int col = getSelectedColumn();
        if (row == -1) row = 1;
        if (col == -1) col = 0;
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        DataSpreadsheetModel.insertRow(getRowCount());
        updateRowHeader();
        setRowSelectionInterval(row, row);
        setColumnSelectionInterval(col, col);
        setChangedStatus();
    }

    /**
     * Inserts a blank row above the selected cell.
     */
    public void insertRowAbove() {
        int row = getSelectedRow();
        int col = getSelectedColumn();
        if (row == -1) row = 1;
        if (col == -1) col = 0;
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        edit = new StateEdit(this, "insert row above");
        DataSpreadsheetModel.insertRow(row);
        updateRowHeader();
        setRowSelectionInterval(row, row);
        setColumnSelectionInterval(col, col);
        setChangedStatus();
        edit.end();
        undoableEditSupport.postEdit(edit);
    }

    /**
     * Inserts a blank row below the selected cell.
     */
    public void insertRowBelow() {
        int row = getSelectedRow();
        int col = getSelectedColumn();
        if (row == -1) row = 1;
        if (col == -1) col = 0;
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        edit = new StateEdit(this, "insert row below");
        DataSpreadsheetModel.insertRow(row + 1);
        updateRowHeader();
        setRowSelectionInterval(row + 1, row + 1);
        setColumnSelectionInterval(col, col);
        setChangedStatus();
        edit.end();
        undoableEditSupport.postEdit(edit);
    }

    /**
     * Inserts a blank column at the end of the spreadsheet.
     */
    public void insertColumn() {
        int row = getSelectedRow();
        int col = getSelectedColumn();
        if (col == -1) col = 0;
        if (row == -1) row = 1;
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        DataSpreadsheetModel.insertColumn(getColumnCount());
        setRowSelectionInterval(row, row);
        setColumnSelectionInterval(col, col);
        setChangedStatus();
    }

    /**
     * Inserts a blank column to the left of the selected cell.
     */
    public void insertColumnLeft() {
        int row = getSelectedRow();
        int col = getSelectedColumn();
        if (col == -1) col = 0;
        if (row == -1) row = 1;
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        edit = new StateEdit(this, "insert column left");
        DataSpreadsheetModel.insertColumn(col);
        setRowSelectionInterval(row, row);
        setColumnSelectionInterval(col, col);
        setChangedStatus();
        edit.end();
        undoableEditSupport.postEdit(edit);
    }

    /**
     * Inserts a blank column to the right of the selected cell.
     */
    public void insertColumnRight() {
        int row = getSelectedRow();
        int col = getSelectedColumn();
        if (row == -1) row = 1;
        if (col == -1) col = 0;
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        edit = new StateEdit(this, "insert column right");
        DataSpreadsheetModel.insertColumn(col + 1);
        setRowSelectionInterval(row, row);
        setColumnSelectionInterval(col + 1, col + 1);
        setChangedStatus();
        edit.end();
        undoableEditSupport.postEdit(edit);
    }

    /**
     * Inserts a blank cell above the selected cell.
     */
    public void insertCellAbove() {
        int row = getSelectedRow();
        int col = getSelectedColumn();
        if (row == -1) row = 1;
        if (col == -1) col = 0;
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        edit = new StateEdit(this, "insert cell above");
        DataSpreadsheetModel.insertCell(row, col);
        updateRowHeader();
        setRowSelectionInterval(row, row);
        setColumnSelectionInterval(col, col);
        setChangedStatus();
        edit.end();
        undoableEditSupport.postEdit(edit);
    }

    /**
     * Inserts a blank cell below the selected cell.
     */
    public void insertCellBelow() {
        int row = getSelectedRow();
        int col = getSelectedColumn();
        if (row == -1) row = 1;
        if (col == -1) col = 0;
        SpreadsheetModel DataSpreadsheetModel = (SpreadsheetModel) getModel();
        edit = new StateEdit(this, "insert cell below");
        DataSpreadsheetModel.insertCell(row + 1, col);
        updateRowHeader();
        setRowSelectionInterval(row + 1, row + 1);
        setColumnSelectionInterval(col, col);
        setChangedStatus();
        edit.end();
        undoableEditSupport.postEdit(edit);
    }

    /**
     * Adds one row to the end of the row header table.
     */
    private void updateRowHeader() {
        RowHeaderTable rowHeader = (RowHeaderTable) ((SpreadsheetScrollPane) app.getDatasheetTabbedPane().getSelectedComponent()).getRowHeaderTable();
        rowHeader.addHeaderRow();
    }

    /**
     * Selects all the cells.
     */
    public void selectAllCells() {
        selectAll();
    }
}
