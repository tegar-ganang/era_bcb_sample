package com.dsb.bar.barkas.admin.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * @author pdsb
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DataTable {

    public Table table;

    private int dataTableIndex;

    private DataTableListener listener;

    private String[] columnNames;

    private String[][] data;

    private DataTableCellFormatter[] columnFormatter;

    private boolean[] dataEditable;

    private boolean[] dataChanged;

    private boolean[] dataNumber;

    private String[] originalText;

    private boolean tableDirty;

    private boolean dataTableDisposed;

    private TableItem currentRow;

    private String tableSort;

    private int tableSortColumn;

    private boolean tableSortAscending;

    private String otherID;

    private ControlEditor editor;

    private TableCursor cursor;

    public DataTable(Table table, int dataTableIndex, DataTableListener listener) {
        this.table = table;
        final Table t = table;
        this.dataTableIndex = dataTableIndex;
        this.listener = listener;
        tableSortColumn = -1;
        tableSort = "";
        tableSortAscending = true;
        dataTableDisposed = false;
        otherID = "";
        cursor = new TableCursor(table, SWT.NONE);
        editor = new ControlEditor(cursor);
        editor.grabHorizontal = true;
        editor.grabVertical = true;
        cursor.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                t.setSelection(new TableItem[] { cursor.getRow() });
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                final Text text = new Text(cursor, SWT.NONE);
                TableItem row = cursor.getRow();
                int column = cursor.getColumn();
                if (!dataEditable[column]) {
                    text.dispose();
                    return;
                }
                text.setText(row.getText(column));
                text.setSelection(0, text.getText().length());
                text.addKeyListener(new KeyAdapter() {

                    public void keyPressed(KeyEvent e) {
                        if (e.character == SWT.CR) {
                            TableItem row = cursor.getRow();
                            int column = cursor.getColumn();
                            String val;
                            if (columnFormatter[column] != null) val = columnFormatter[column].unformat(text.getText()); else val = text.getText();
                            if (!val.equals(originalText[column])) dataChanged[column] = true; else dataChanged[column] = false;
                            row.setText(column, val);
                            text.dispose();
                            currentRow = row;
                            saveChanges();
                        }
                        if (e.character == SWT.ESC) {
                            text.dispose();
                        }
                    }
                });
                text.addFocusListener(new FocusAdapter() {

                    public void focusLost(FocusEvent e) {
                        if (!text.isDisposed()) text.dispose();
                    }
                });
                editor.setEditor(text);
                text.setVisible(true);
                text.setFocus();
            }
        });
        cursor.addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
            }
        });
        table.addKeyListener(new KeyAdapter() {

            public void keyReleased(KeyEvent e) {
                if (e.keyCode == SWT.CONTROL && (e.stateMask & SWT.SHIFT) != 0) return;
                if (e.keyCode == SWT.SHIFT && (e.stateMask & SWT.CONTROL) != 0) return;
                if (e.keyCode != SWT.CONTROL && (e.stateMask & SWT.CONTROL) != 0) return;
                if (e.keyCode != SWT.SHIFT && (e.stateMask & SWT.SHIFT) != 0) return;
                TableItem[] selection = t.getSelection();
                TableItem row = (selection.length == 0) ? t.getItem(t.getTopIndex()) : selection[0];
                t.showItem(row);
                cursor.setSelection(row, 0);
                cursor.setVisible(true);
                cursor.setFocus();
            }
        });
    }

    public void setOtherID(String otherID) {
        this.otherID = otherID;
    }

    public String getOtherID() {
        return this.otherID;
    }

    public void saveChanges() {
        if (currentRow == null) return;
        int rowNumber = -1;
        for (int i = 0; i < table.getItemCount(); i++) {
            if (currentRow.equals(table.getItem(i))) {
                rowNumber = i;
                break;
            }
        }
        if (rowNumber > -1) {
            for (int col = 0; col < table.getColumnCount(); col++) data[rowNumber][col] = table.getItem(rowNumber).getText(col);
            listener.rowUpdated(dataTableIndex, columnNames, data[rowNumber], dataChanged);
        }
        for (int i = 0; i < dataChanged.length; i++) dataChanged[i] = false;
    }

    public void emptyTable() {
        if (editor != null && editor.getEditor() != null) {
            ((Text) (editor.getEditor())).setText("");
        }
        if (table != null) {
            table.removeAll();
        }
        dataTableDisposed = true;
    }

    public void setSort(String column, boolean ascending) {
        tableSort = column;
        tableSortAscending = ascending;
        tableSortColumn = -1;
        if (columnNames == null) return;
        for (int i = 0; i < columnNames.length; i++) {
            if (columnNames[i].equals(column)) {
                tableSortColumn = i;
                break;
            }
        }
        sortTable(tableSortColumn, ascending);
        tableDirty = true;
        refreshTable();
    }

    public void setSort(String column) {
        if (column != tableSort) {
            if (column != null && column.equals(tableSort)) tableSortAscending = !tableSortAscending; else tableSortAscending = true;
            tableSortColumn = -1;
            for (int i = 0; i < columnNames.length; i++) {
                if (columnNames[i].equals(column)) {
                    tableSortColumn = i;
                    break;
                }
            }
            tableSort = column;
            sortTable(tableSortColumn, tableSortAscending);
            tableDirty = true;
            refreshTable();
        } else if (column != null) {
            tableSortAscending = !tableSortAscending;
            sortTable(tableSortColumn, tableSortAscending);
            tableDirty = true;
            refreshTable();
        }
    }

    public void refreshTable() {
        if (data == null || data.length == 0 || table.isDisposed() || dataTableDisposed) return;
        int rowCount = data.length;
        int colCount = columnNames.length;
        if (table.getColumnCount() < colCount) {
            int tableCC = table.getColumnCount();
            for (int i = 0; i < tableCC; i++) {
                table.getColumn(i).setText(columnNames[i]);
            }
            for (int i = tableCC; i < colCount; i++) {
                TableColumn tc = new TableColumn(table, SWT.NONE);
                if (columnNames[i] != null) tc.setText(columnNames[i]);
                tc.addSelectionListener(new SelectionAdapter() {

                    public void widgetSelected(SelectionEvent evt) {
                        sortTableClick(evt);
                    }
                });
            }
            for (int i = tableCC; i < colCount; i++) table.getColumn(i).pack();
        } else if (table.getColumnCount() > colCount) {
            for (int i = 0; i < colCount; i++) {
                table.getColumn(i).setText(columnNames[i]);
            }
            TableColumn[] cols = table.getColumns();
            for (int i = colCount; i < cols.length; i++) cols[i].dispose();
        } else {
            for (int i = 0; i < colCount; i++) {
                if (columnNames[i] != null) table.getColumn(i).setText(columnNames[i]);
            }
        }
        if (table.getItemCount() < rowCount) {
            int tableRC = table.getItemCount();
            for (int row = 0; row < tableRC; row++) {
                for (int col = 0; col < colCount; col++) {
                    if (data[row][col] != null) {
                        if (columnFormatter[col] != null) table.getItem(row).setText(col, columnFormatter[col].format(data[row][col])); else table.getItem(row).setText(col, data[row][col]);
                    } else {
                        if (columnFormatter[col] != null) table.getItem(row).setText(col, columnFormatter[col].format("null")); else table.getItem(row).setText(col, "null");
                    }
                }
            }
            for (int row = tableRC; row < rowCount; row++) {
                TableItem ti = new TableItem(table, SWT.NONE);
                for (int col = 0; col < colCount; col++) {
                    if (data[row][col] != null) {
                        if (columnFormatter[col] != null) ti.setText(col, columnFormatter[col].format(data[row][col])); else ti.setText(col, data[row][col]);
                    } else {
                        if (columnFormatter[col] != null) ti.setText(col, columnFormatter[col].format("null")); else ti.setText(col, "null");
                    }
                }
            }
        } else if (table.getItemCount() > rowCount) {
            for (int row = 0; row < rowCount; row++) {
                for (int col = 0; col < colCount; col++) {
                    if (data[row][col] != null) {
                        if (columnFormatter[col] != null) table.getItem(row).setText(col, columnFormatter[col].format(data[row][col])); else table.getItem(row).setText(col, data[row][col]);
                    } else {
                        if (columnFormatter[col] != null) table.getItem(row).setText(col, columnFormatter[col].format("null")); else table.getItem(row).setText(col, "null");
                    }
                }
            }
            TableItem[] items = table.getItems();
            for (int row = rowCount; row < items.length; row++) items[row].dispose();
        } else {
            for (int row = 0; row < rowCount; row++) {
                for (int col = 0; col < colCount; col++) {
                    if (data[row][col] != null) {
                        if (columnFormatter[col] != null) table.getItem(row).setText(col, columnFormatter[col].format(data[row][col])); else table.getItem(row).setText(col, data[row][col]);
                    } else {
                        if (columnFormatter[col] != null) table.getItem(row).setText(col, columnFormatter[col].format("null")); else table.getItem(row).setText(col, "null");
                    }
                }
            }
        }
        if (tableDirty) {
            for (int i = 0; i < table.getItemCount(); i++) table.getItem(i).setChecked(false);
            tableDirty = false;
        }
        if (table.getItemCount() > 0 && !cursor.isDisposed()) {
            table.setSelection(table.getTopIndex());
            cursor.setSelection(table.getTopIndex(), cursor.getColumn());
            cursor.setVisible(true);
        }
        dataChanged = new boolean[colCount];
        dataEditable = new boolean[colCount];
        originalText = new String[colCount];
        dataNumber = new boolean[colCount];
        for (int i = 0; i < colCount; i++) {
            dataChanged[i] = false;
            if (columnNames[i] != null) {
                dataEditable[i] = listener.isColumnEditable(dataTableIndex, columnNames[i]);
                dataNumber[i] = listener.isColumnNumeric(dataTableIndex, columnNames[i]);
            } else {
                dataEditable[i] = false;
                dataNumber[i] = false;
            }
            originalText[i] = "";
        }
    }

    private void sortTableClick(SelectionEvent evt) {
        TableColumn tc = (TableColumn) evt.widget;
        setSort(tc.getText());
    }

    public void forceReloadData() {
        loadData();
        sortTable(tableSortColumn, tableSortAscending);
        refreshTable();
    }

    /**
	 * 
	 */
    private void loadData() {
        if (listener == null) return;
        String[][] all = listener.getData(dataTableIndex, "", otherID);
        if (all == null || all.length < 1) return;
        columnNames = all[0];
        data = new String[all.length - 1][];
        for (int i = 0; i < all.length - 1; i++) data[i] = all[i + 1];
        columnFormatter = new DataTableCellFormatter[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) columnFormatter[i] = listener.getColumnFormatter(dataTableIndex, columnNames[i]);
        return;
    }

    private void setData(String[] columnNames, String[][] data) {
        this.columnNames = columnNames;
        this.data = data;
        tableDirty = true;
        refreshTable();
    }

    public String[][] getData() {
        return this.data;
    }

    private void sortTable(int whichColumn, boolean ascending) {
        if (whichColumn < 0 || whichColumn >= columnNames.length) return;
        String data2[][] = new String[data.length][];
        for (int i = 0; i < data.length; i++) data2[i] = null;
        if (dataNumber[whichColumn]) {
            for (int i = 0; i < data.length; i++) {
                try {
                    insertDataElementNumeric(data2, data[i], whichColumn, ascending);
                } catch (Exception e) {
                }
            }
        } else {
            for (int i = 0; i < data.length; i++) {
                try {
                    insertDataElement(data2, data[i], whichColumn, ascending);
                } catch (Exception e) {
                }
            }
        }
        data = data2;
    }

    /**
	 * @param data2
	 * @param strings
	 * @param whichColumn
	 * @param ascending
	 */
    private void insertDataElement(String[][] data2, String[] elem, int whichColumn, boolean ascending) {
        for (int i = 0; i < data2.length; i++) {
            if (data2[i] == null) {
                data2[i] = elem;
                return;
            }
            if (ascending) {
                if (elem[whichColumn] == null || data2[i][whichColumn] == null || elem[whichColumn].compareToIgnoreCase(data2[i][whichColumn]) < 0) {
                    for (int k = data2.length - 1; k > i; k--) data2[k] = data2[k - 1];
                    data2[i] = elem;
                    return;
                }
            } else {
                if (elem[whichColumn] != null && data2[i][whichColumn] != null && elem[whichColumn].compareToIgnoreCase(data2[i][whichColumn]) > 0) {
                    for (int k = data2.length - 1; k > i; k--) data2[k] = data2[k - 1];
                    data2[i] = elem;
                    return;
                }
            }
        }
    }

    /**
	 * @param data2
	 * @param strings
	 * @param whichColumn
	 * @param ascending
	 */
    private void insertDataElementNumeric(String[][] data2, String[] elem, int whichColumn, boolean ascending) {
        for (int i = 0; i < data2.length; i++) {
            if (data2[i] == null) {
                data2[i] = elem;
                return;
            }
            if (ascending) {
                if (elem[whichColumn] == null || data2[i][whichColumn] == null || numericCompare(elem[whichColumn], data2[i][whichColumn]) < 0) {
                    for (int k = data2.length - 1; k > i; k--) data2[k] = data2[k - 1];
                    data2[i] = elem;
                    return;
                }
            } else {
                if (elem[whichColumn] != null && data2[i][whichColumn] != null && numericCompare(elem[whichColumn], data2[i][whichColumn]) > 0) {
                    for (int k = data2.length - 1; k > i; k--) data2[k] = data2[k - 1];
                    data2[i] = elem;
                    return;
                }
            }
        }
    }

    /**
	 * @param string
	 * @param string2
	 * @return
	 */
    private int numericCompare(String a, String b) {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        long al, bl;
        try {
            al = Long.parseLong(a);
        } catch (NumberFormatException nfe) {
            return numericCompareDouble(a, b);
        }
        try {
            bl = Long.parseLong(b);
        } catch (NumberFormatException nfe) {
            return numericCompareDouble(a, b);
        }
        if (al < bl) return -1;
        if (al > bl) return 1;
        return 0;
    }

    /**
	 * @param a
	 * @param b
	 * @return
	 */
    private int numericCompareDouble(String a, String b) {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        double ad, bd;
        try {
            ad = Double.parseDouble(a);
        } catch (NumberFormatException nfe) {
            return a.compareToIgnoreCase(b);
        }
        try {
            bd = Double.parseDouble(b);
        } catch (NumberFormatException nfe) {
            return a.compareToIgnoreCase(b);
        }
        if (ad < bd) return -1;
        if (ad > bd) return 1;
        return 0;
    }

    public void dispose() {
        this.emptyTable();
        this.editor.dispose();
        this.cursor.dispose();
    }
}
