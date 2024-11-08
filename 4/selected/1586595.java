package KFramework30.Widgets;

import KFramework30.Base.KBusinessObjectClass;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import java.lang.reflect.Constructor;
import javax.swing.JDialog;
import KFramework30.Base.*;
import KFramework30.Printing.*;
import KFramework30.Communication.dbTransactionClientClass;
import KFramework30.Communication.persistentObjectManagerClass;
import KFramework30.Widgets.DataBrowser.TableCellRenderers.*;
import KFramework30.Widgets.DataBrowser.recordClass;
import KFramework30.Widgets.DataBrowser.tableModelClass;
import KFramework30.Widgets.DataBrowser.tableHeaderRendererClass;
import KFramework30.Widgets.DataBrowser.UI.setOrderDialogClass;
import KFramework30.Widgets.DataBrowser.UI.setCriteriaDialogClass;
import KFramework30.Widgets.DataBrowser.cellWriterInterface;
import KFramework30.Widgets.DataBrowser.cellRenderingHookInterface;
import KFramework30.Widgets.DataBrowser.SQLPreprocessorClass;
import KFramework30.Widgets.DataBrowser.KBrowserDataWriterInterface;
import KFramework30.Widgets.KDialogDirectorClass.KDialogInterface;
import KFramework30.Widgets.DataBrowser.KTableCellEditorBaseClass;
import KFramework30.Widgets.DataBrowser.KTableCellRendererBaseClass;

public class KDataBrowserBaseClass extends Object implements ActionListener, MouseListener {

    public static final int CUSTOM_CRITERIA_ROW_COUNT = 50;

    public static final int BROWSER_COLUMN_TYPE_CHARACTER = 0;

    public static final int BROWSER_COLUMN_TYPE_NUMERIC = 1;

    public static final int BROWSER_COLUMN_TYPE_NUMERIC2 = 2;

    public static final int BROWSER_COLUMN_TYPE_CURRENCY = 3;

    public static final int BROWSER_COLUMN_TYPE_DATE = 4;

    public static final int BROWSER_COLUMN_TYPE_TIME = 5;

    protected KConfigurationClass configuration;

    protected KLogClass log;

    protected JTable visualTable;

    protected boolean listenerRegistered = false;

    protected java.util.List listenerList;

    KBrowserDataWriterInterface dataWriter;

    Class<? extends KBusinessObjectClass> pdcObjectClass;

    Class<? extends JDialog> pdcEditorClass;

    private SQLPreprocessorClass SQLPreprocessor;

    public tableModelClass tableModel;

    private java.util.List labelOperationList;

    private String[][] customCriteriaRowData;

    private java.util.List customOrderData;

    private java.util.List tableHeaderRendererList;

    private ListSelectionModel selectionModel;

    private java.util.Map columnDefaultEditor;

    private java.util.Map columnDefaultRender;

    private boolean tableLoaded = false;

    private NumberFormat currencyFormatter;

    private NumberFormat decimalFormatter;

    private boolean doubleClickEnabled = true;

    private long previousTime = 0;

    private long previousX = -1;

    private long previousY = -1;

    public java.awt.Window parentWindow;

    public interface tableToolbarActionPerformedNotificationInterface {

        public abstract void tableToolbarButtonClickedNotification(String action);
    }

    private class labelOperationClass {

        Object visualComponent;

        String operation;

        int displayType;

        boolean reflectCustomFilter;

        public labelOperationClass(Object visualComponentParam, String operationParam, int displayTypeParam, boolean reflectCustomFilterParam) {
            visualComponent = visualComponentParam;
            operation = operationParam;
            displayType = displayTypeParam;
            reflectCustomFilter = reflectCustomFilterParam;
        }
    }

    /** Creates new KDataBrowserBaseClass */
    public KDataBrowserBaseClass(KConfigurationClass configurationParam, KLogClass logParam, boolean showKeyFieldParam, JTable tableParam, java.awt.Window parentWindowParam, Class<? extends KBusinessObjectClass> pdcObjectClassParam, Class<? extends JDialog> pdcEditorClassParam) throws KExceptionClass {
        super();
        configuration = configurationParam;
        log = logParam;
        parentWindow = parentWindowParam;
        visualTable = tableParam;
        pdcEditorClass = pdcEditorClassParam;
        pdcObjectClass = pdcObjectClassParam;
        SQLPreprocessor = new SQLPreprocessorClass(configuration, log, showKeyFieldParam, parentWindow);
        tableModel = new tableModelClass(configuration, log, parentWindow, SQLPreprocessor, visualTable);
        selectionModel = visualTable.getSelectionModel();
        currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault());
        decimalFormatter = new DecimalFormat("0.00000");
        labelOperationList = new ArrayList();
        customOrderData = new ArrayList();
        columnDefaultRender = new HashMap();
        listenerList = new ArrayList();
        tableHeaderRendererList = new ArrayList();
        visualTable.addMouseListener(this);
        visualTable.setRowHeight(25);
        doubleClickEnabled = true;
        log.log(this, "Constructed successfully.");
    }

    public void setDoubleClickEnabled(boolean doubleClickEnabledParam) {
        doubleClickEnabled = doubleClickEnabledParam;
    }

    public void addButtonActionListener(tableToolbarActionPerformedNotificationInterface listenerParam) {
        listenerList.add(listenerParam);
        listenerRegistered = true;
    }

    public JTable getJTable() {
        return visualTable;
    }

    public void setCellWriter(cellWriterInterface cellWriterParam) {
        tableModel.setCellWriter(cellWriterParam);
    }

    public void setCellDisplayHook(cellRenderingHookInterface cellDisplayHookParam) {
        tableModel.setCellDisplayHook(cellDisplayHookParam);
    }

    /** called manually at child class discretion
     */
    public void notifyListeners(String actionParam) {
        if (!listenerRegistered) return;
        Iterator listener = listenerList.iterator();
        while (listener.hasNext()) {
            ((tableToolbarActionPerformedNotificationInterface) listener.next()).tableToolbarButtonClickedNotification(actionParam);
        }
    }

    /**   Initialize SQL statement
		This method is called only once after constructor, and before any other
		method. 
	*/
    public void initializeSQLQuery(String SQLSelect, String DBTable, String keyFieldParam) throws KExceptionClass {
        SQLPreprocessor.initializeSQLQuery(SQLSelect, DBTable, keyFieldParam);
    }

    /**   Add the criteria for SQL query.  */
    public void setDefaultParameters(java.util.List parameters) throws KExceptionClass {
        SQLPreprocessor.setDefaultParameters(parameters);
    }

    /**   Add the criteria for SQL query.  */
    public java.util.List getDefaultParameters() throws KExceptionClass {
        return (SQLPreprocessor.getDefaultParameters());
    }

    public void bindDefaultParameter1(String parameterName, Object parameterValue) {
        SQLPreprocessor.bindDefaultParameter(parameterName, parameterValue);
    }

    public void setDefaultCriteria(String criteria) throws KExceptionClass {
        SQLPreprocessor.setDefaultCriteria(criteria);
    }

    public String getDefaultCriteria() {
        return (SQLPreprocessor.getDefaultCriteria());
    }

    public void clearDefaultCriteria() {
        SQLPreprocessor.clearDefaultCriteria();
    }

    public void setDefaultOrder(String order) {
        SQLPreprocessor.setDefaultOrder(order);
    }

    public void clearDefaultOrder() {
        SQLPreprocessor.clearDefaultOrder();
    }

    /** Following two methods are for temporary custom sorting and filtering */
    public void bindCustomParameter1(String parameterName, Object parameterValue) {
        SQLPreprocessor.bindCustomParameter(parameterName, parameterValue);
    }

    public void addCustomCriteria(java.util.List filters) throws KExceptionClass {
        SQLPreprocessor.addCustomCriteria(filters);
    }

    public void addCustomCriteria(String filter) throws KExceptionClass {
        SQLPreprocessor.addCustomCriteria(filter);
    }

    public void clearCustomCriteria() {
        SQLPreprocessor.clearCustomCriteria();
    }

    public void setCustomOrder(java.util.List orderList) throws KExceptionClass {
        SQLPreprocessor.setCustomOrder(orderList);
    }

    /** Load the data into table */
    public void initializeTable() throws KExceptionClass {
        SQLPreprocessor.assembleSQL();
        tableModel.load(0);
        visualTable.setModel(tableModel);
        visualTable.createDefaultColumnsFromModel();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            TableColumn theColumn = visualTable.getColumn(tableModel.getColumnName(i));
            KTableCellRendererBaseClass newRenderer = new TextBoxCellRendererClass(BROWSER_COLUMN_TYPE_CHARACTER, tableModel, log);
            theColumn.setCellRenderer((TableCellRenderer) newRenderer);
            theColumn.setCellEditor(new TextBoxCellEditorClass(BROWSER_COLUMN_TYPE_CHARACTER, tableModel, log));
            if (SQLPreprocessor.isFieldNumeric(i)) {
                TextBoxCellRendererClass renderer = (TextBoxCellRendererClass) getColumnCellRenderer(tableModel.getColumnName(i));
                renderer.setDataType(BROWSER_COLUMN_TYPE_NUMERIC);
                log.log(this, " auto setting " + tableModel.getColumnName(i) + " to numeric");
            }
        }
        tableLoaded = true;
        calculateOperatios();
        Iterator renderers = tableHeaderRendererList.iterator();
        while (renderers.hasNext()) {
            tableHeaderRendererClass renderer = (tableHeaderRendererClass) renderers.next();
            TableColumnModel colModel = visualTable.getColumnModel();
            TableColumn col = colModel.getColumn(colModel.getColumnIndex(renderer.getColumnName()));
            col.setHeaderRenderer(renderer);
        }
        customCriteriaRowData = new String[CUSTOM_CRITERIA_ROW_COUNT][tableModel.getColumnCount()];
    }

    public void setBrowserSaveListener(KBrowserDataWriterInterface dataWriterParam) {
        dataWriter = dataWriterParam;
        log.log(this, "Set browser read and write ");
    }

    public void stopTableCellEditing() {
        KMetaUtilsClass.stopTableCellEditing(visualTable);
    }

    public void saveBrowserChanges() throws KExceptionClass {
        stopTableCellEditing();
        if (dataWriter != null) {
            tableModel.saveChanges(dataWriter);
        }
        refresh();
    }

    public boolean isLoaded() {
        return (tableLoaded);
    }

    /**   Reload the table as the beginning.  */
    public void resetToDefaults() throws KExceptionClass {
        tableModel.resetToDefaults();
        visualTable.scrollRectToVisible(new Rectangle());
        tableModel.fireTableDataChanged();
        calculateOperatios();
        parentWindow.repaint();
    }

    /**   Reload the table as the new setting applied.  */
    public void refresh() throws KExceptionClass {
        tableModel.refresh();
        visualTable.scrollRectToVisible(new Rectangle());
        tableModel.fireTableDataChanged();
        calculateOperatios();
        visualTable.repaint();
    }

    /**   Reload the table as the new setting applied.  */
    public void softRefresh() throws KExceptionClass {
        tableModel.load(visualTable.getSelectedRow());
        tableModel.fireTableDataChanged();
        calculateOperatios();
        parentWindow.repaint();
    }

    /**   Reload the table as the new setting applied.  */
    public void displayRefresh() throws KExceptionClass {
        tableModel.fireTableDataChanged();
        parentWindow.repaint();
    }

    /** This method is for retrieving previous custom input criteria. 
        It should be called after initializeTable method called */
    public String[][] GetCustomCriteriaRowData() throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'GetCustomCriteriaRowData' method is called in wrong order !", null);
        return customCriteriaRowData;
    }

    /** This method is for keeping previous custom input criteria. 
        And change the column header color if the column's criteria is set.*/
    public void setCustomCriteriaRowData(String[][] data) {
        customCriteriaRowData = data;
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            TableColumnModel colModel = visualTable.getColumnModel();
            TableColumn column = colModel.getColumn(i);
            TableCellRenderer renderer = column.getHeaderRenderer();
            boolean defaultBehavior = true;
            for (int j = 0; j < CUSTOM_CRITERIA_ROW_COUNT; j++) {
                if (customCriteriaRowData[j][i] != null && customCriteriaRowData[j][i].trim().length() != 0) {
                    if (renderer == null) {
                        renderer = new DefaultTableCellRenderer();
                        column.setHeaderRenderer(renderer);
                        ((JLabel) renderer).setHorizontalAlignment(JLabel.CENTER);
                    }
                    ((JComponent) renderer).setForeground(new Color(0, 0, 200));
                    defaultBehavior = false;
                    break;
                }
            }
            if (defaultBehavior && renderer != null) {
                if (renderer instanceof DefaultTableCellRenderer) column.setHeaderRenderer(null); else ((JComponent) renderer).setForeground(new Color(0, 0, 0));
            }
        }
        visualTable.getTableHeader().resizeAndRepaint();
    }

    /** This method is for retrieving previous custom input orders. */
    public java.util.List GetCustomOrderData() {
        return customOrderData;
    }

    /**   Return the key field value of current selected table row.  */
    public long getSelectedRowKey() throws KExceptionClass {
        int index = visualTable.getSelectedRow();
        if (index == -1) {
            throw new KExceptionClass("\nPlease select a record", null);
        }
        return tableModel.getKeyValue(index);
    }

    /**   Return the key field value of current selected table row.  */
    public long getRowOID(int row) throws KExceptionClass {
        return tableModel.getKeyValue(row);
    }

    /**   Return the key field values of current multi selected table rows.  */
    public Vector getMultiSelectedRowKeys() throws KExceptionClass {
        int[] indexes = visualTable.getSelectedRows();
        if (indexes.length == 0) {
            throw new KExceptionClass("\nYou need to chose records!", null);
        }
        Vector selectedKeys = new Vector();
        for (int i = 0; i < indexes.length; i++) {
            selectedKeys.add(new Long(tableModel.getKeyValue(indexes[i])));
        }
        return selectedKeys;
    }

    public Vector getMultiSelectedRowKeysAsStrings() throws KExceptionClass {
        int[] indexes = visualTable.getSelectedRows();
        if (indexes.length == 0) {
            throw new KExceptionClass("\nYou need to chose records!", null);
        }
        Vector selectedKeys = new Vector();
        for (int i = 0; i < indexes.length; i++) {
            selectedKeys.add(String.valueOf(tableModel.getKeyValue(indexes[i])));
        }
        return selectedKeys;
    }

    /**   Return the visual header of current selected table column.  */
    public String getSelectedColumnVisualHeader() throws KExceptionClass {
        int index = visualTable.getSelectedColumn();
        if (index == -1) {
            throw new KExceptionClass("\nYou need to chose records!", null);
        }
        return (visualTable.getColumnName(index));
    }

    /**   Set up label and operation list  
          This method should be called before first call to method calculateOperation() */
    public void saveSQLOperation(Object visualComponent, String sqlOperation, boolean reflectCustomFilter) throws KExceptionClass {
        saveSQLOperation(visualComponent, sqlOperation, BROWSER_COLUMN_TYPE_NUMERIC, reflectCustomFilter);
    }

    /**   Set up label and operation list  
          This method should be called before first call to method calculateOperation() */
    public void saveSQLOperation(Object visualComponent, String sqlOperation, int dataType, boolean reflectCustomFilter) throws KExceptionClass {
        if (tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'saveLabelOperation' method is called in wrong order !", null);
        switch(dataType) {
            case BROWSER_COLUMN_TYPE_CHARACTER:
            case BROWSER_COLUMN_TYPE_NUMERIC:
            case BROWSER_COLUMN_TYPE_NUMERIC2:
            case BROWSER_COLUMN_TYPE_DATE:
            case BROWSER_COLUMN_TYPE_TIME:
            case BROWSER_COLUMN_TYPE_CURRENCY:
                break;
            default:
                throw new KExceptionClass("Could not set label operation data type. Type specified is invalid.", null);
        }
        labelOperationList.add(new labelOperationClass(visualComponent, sqlOperation, dataType, reflectCustomFilter));
    }

    /** Sets the Font for the table 
        This method can be called before or after initializeTable method called */
    public void setTableFont(Font font) {
        visualTable.setFont(font);
    }

    /** Get the renderer of a column by column name 
     */
    private TableCellRenderer getColumnCellRenderer(String columnName) throws KExceptionClass {
        TableColumn theColumn;
        try {
            theColumn = visualTable.getColumn(columnName);
        } catch (Exception error) {
            String message = new String("Error while configuring table.\n" + "Could not get renderer by column name, column name not found [" + columnName + "]");
            throw new KExceptionClass(message, error);
        }
        ;
        return theColumn.getCellRenderer();
    }

    /** Get the renderer of a column by column name 
     */
    private TableCellEditor getColumnCellEditor(String columnName) throws KExceptionClass {
        TableColumn theColumn;
        try {
            theColumn = visualTable.getColumn(columnName);
        } catch (Exception error) {
            String message = new String("Error while configuring table.\n" + "Could not get renderer by column name, column name not found [" + columnName + "]");
            throw new KExceptionClass(message, error);
        }
        ;
        return theColumn.getCellEditor();
    }

    /** Set the Font for the column 
        This method should be called after initializeTable method called */
    public void adjustColumnFont(String columnName, Font font) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'setColumnFont' method is called in wrong order !", null);
        KTableCellRendererBaseClass renderer = (KTableCellRendererBaseClass) getColumnCellRenderer(columnName);
        if (renderer != null) renderer.setColumnFont(font);
    }

    /** Sets the width for the column 
        This method should be called after initializeTable method called */
    public void adjustColumnWidth(String columnName, int width) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'setColumnWidth' method is called in wrong order !", null);
        TableColumn theColumn;
        try {
            theColumn = visualTable.getColumn(columnName);
            theColumn.setPreferredWidth(width);
        } catch (Exception error) {
            log.log(this, KMetaUtilsClass.getStackTrace(error));
            String message;
            message = "*** Colud not display table *** [";
            message += error.toString();
            message += "]\n Column not found in display -> [" + columnName + "]";
            KMetaUtilsClass.showErrorMessageFromException(parentWindow, error);
        }
        ;
    }

    /** Set the data type for the column 
        This method should be called after initializeTable method called */
    public void adjustColumnType(String columnName, int type) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'setColumnType' method is called in wrong order !", null);
        KTableCellRendererBaseClass renderer = (KTableCellRendererBaseClass) getColumnCellRenderer(columnName);
        if (renderer != null && renderer instanceof TextBoxCellRendererClass) ((TextBoxCellRendererClass) renderer).setDataType(type);
        KTableCellEditorBaseClass editor = (KTableCellEditorBaseClass) getColumnCellEditor(columnName);
        if (editor != null && editor instanceof TextBoxCellEditorClass) ((TextBoxCellEditorClass) editor).setColumnType(type);
        log.log(this, " user setting " + columnName + " to " + type);
    }

    /** Sets the justification for the column 
        This method should be called after initializeTable method called */
    public void adjustColumnJustification(String columnName, int alignment) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'setColumnJustification' method is called in wrong order !", null);
        KTableCellRendererBaseClass renderer = (KTableCellRendererBaseClass) getColumnCellRenderer(columnName);
        if (renderer != null) renderer.setHorizontalAlignment(alignment);
    }

    /** Sets the foreground color for the column 
        This method should be called after initializeTable method called */
    public void adjustColumnForegroundColor(String columnName, Color fgColor) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'setColumnForegroundColor' method is called in wrong order !", null);
        KTableCellRendererBaseClass renderer = (KTableCellRendererBaseClass) getColumnCellRenderer(columnName);
        if (renderer != null) renderer.setForeground(fgColor);
    }

    /** Sets the background color for the column 
        This method should be called after initializeTable method called */
    public void adjustColumnBackgroundColor(String columnName, Color bgColor) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'setColumnBackgroundColor' method is called in wrong order !", null);
        KTableCellRendererBaseClass renderer = (KTableCellRendererBaseClass) getColumnCellRenderer(columnName);
        if (renderer != null) renderer.setBackground(bgColor);
    }

    /** Build a list to match DB table field name with table alias name and 
     JTable header name. This method should be called before initializeTable.
     Added variable columnDefaultRender is a hash map, which contains header name 
     and Boolean flag pair for custom column render setting. True means to use default render. 
     Default value for the flag is false..*/
    public void setColumnNames(String aliasName, String fieldName, String headerName) throws KExceptionClass {
        if (tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'setColumnNames' method is called in wrong order !", null);
        SQLPreprocessor.setColumnNames(aliasName, fieldName, headerName);
    }

    public void setColumnNames(String aliasName, String fieldName, String headerName, boolean colEditable) throws KExceptionClass {
        if (tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'setColumnNames' method is called in wrong order !", null);
        SQLPreprocessor.setColumnNames(aliasName, fieldName, headerName, colEditable);
    }

    /** Gets the table column names 
        This method should be called after initializeTable.*/
    public void getColumnNames(java.util.List nameList) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'getColumnNames' method is called in wrong order !", null);
        if (nameList != null) {
            int len = tableModel.getColumnCount();
            for (int i = 0; i < len; i++) nameList.add(tableModel.getColumnName(i));
        }
    }

    /** Return the table value at the selected row under the column name  */
    public String getSelectedFieldValue(String ColumnName) throws KExceptionClass {
        int row = visualTable.getSelectedRow();
        if (row == -1) {
            throw new KExceptionClass("\nDebe seleccionar un registro!", null);
        }
        return tableModel.getTheFieldValue(row, ColumnName);
    }

    public boolean isColumnNumeric(String columnName) throws KExceptionClass {
        int columnType = getColumnType(columnName);
        switch(columnType) {
            case KDataBrowserBaseClass.BROWSER_COLUMN_TYPE_NUMERIC:
            case KDataBrowserBaseClass.BROWSER_COLUMN_TYPE_NUMERIC2:
            case KDataBrowserBaseClass.BROWSER_COLUMN_TYPE_CURRENCY:
                return true;
        }
        return false;
    }

    public int getColumnType(String columnName) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'getColumnType' method is called in wrong order !", null);
        KTableCellRendererBaseClass renderer = (KTableCellRendererBaseClass) getColumnCellRenderer(columnName);
        if (renderer != null) return renderer.getColumnType(); else return BROWSER_COLUMN_TYPE_CHARACTER;
    }

    /** Gets the current SQL
        This method should be called after initializeTable.*/
    public void prepareTransactionWithBrowserSQL(dbTransactionClientClass dbTransaction) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'getSQL' method is called in wrong order !", null);
        SQLPreprocessor.prepareDBTransactionForTable(dbTransaction);
    }

    /** Gets the current SQL transaction with default criteria */
    public void prepareDefaultDBTransactionForTable(dbTransactionClientClass dbTransaction) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'getSQL' method is called in wrong order !", null);
        SQLPreprocessor.prepareDBTransactionForTable(dbTransaction);
    }

    /** Gets the current SQL
        This method should be called after initializeTable.*/
    public void prepareDefaultDBTransactionForTable(dbTransactionClientClass dbTransaction, String orderBy) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'getSQL' method is called in wrong order !", null);
        SQLPreprocessor.prepareDefaultDBTransactionForTable(dbTransaction, orderBy);
    }

    /** Gets the current SQL
        This method should be called after initializeTable.*/
    public void prepareCustomFieldsDBTransaction(String customFields, dbTransactionClientClass dbTransaction, boolean reflectCustomFilter) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'getSQL' method is called in wrong order !", null);
        SQLPreprocessor.prepapreCustomFieldsDBTransaction(customFields, dbTransaction, reflectCustomFilter);
    }

    public void setColumnRenderer(String columnName, KTableCellRendererBaseClass renderer) throws KExceptionClass {
        TableColumn column = visualTable.getColumn(columnName);
        column.setCellRenderer(renderer);
    }

    public void setColumnEditor(String columnName, KTableCellEditorBaseClass CellEditor) throws KExceptionClass {
        TableColumn column = visualTable.getColumn(columnName);
        if (!tableModel.isCellEditable(0, column.getModelIndex())) throw new KExceptionClass("\nColumn [" + column.getModelIndex() + " ] is not editable!", null);
        column.setCellEditor(CellEditor);
    }

    /**
     * Save renderers and subscribe operations
     */
    public void adjustHeaderRenderer(tableHeaderRendererClass renderer) throws KExceptionClass {
        tableHeaderRendererList.add(renderer);
        Object[][] operArray = renderer.getOperations();
        for (int i = 0; i < operArray.length; i++) {
            if (operArray[i][1] != null) saveSQLOperation(operArray[i][0], (String) operArray[i][1], ((Integer) operArray[i][2]).intValue(), true);
        }
    }

    /**   Execute SQL operations and display results in visual components.  */
    private void calculateOperatios() throws KExceptionClass {
        int index = 0;
        String OperationsWithCustomFilter = "";
        String OperationsWithoutCustomFilter = "";
        Iterator labelList = labelOperationList.iterator();
        while (labelList.hasNext()) {
            index++;
            labelOperationClass currentOperation = (labelOperationClass) labelList.next();
            if (currentOperation.reflectCustomFilter) {
                if (OperationsWithCustomFilter.length() > 0) OperationsWithCustomFilter += ", ";
                OperationsWithCustomFilter += currentOperation.operation + " AS CUSTOMFILTER" + index;
            } else {
                if (OperationsWithoutCustomFilter.length() > 0) OperationsWithoutCustomFilter += ", ";
                OperationsWithoutCustomFilter += currentOperation.operation + " AS NOCUSTOMFILTER" + index;
            }
        }
        Map dbResult = new HashMap();
        if (OperationsWithCustomFilter.length() > 0) dbResult.putAll(SQLPreprocessor.executeSQLOperation(OperationsWithCustomFilter, true));
        if (OperationsWithoutCustomFilter.length() > 0) dbResult.putAll(SQLPreprocessor.executeSQLOperation(OperationsWithoutCustomFilter, false));
        String result = "";
        index = 0;
        labelList = labelOperationList.iterator();
        while (labelList.hasNext()) {
            index++;
            labelOperationClass currentOperation = (labelOperationClass) labelList.next();
            if (currentOperation.reflectCustomFilter) result = (String) dbResult.get("CUSTOMFILTER" + index); else result = (String) dbResult.get("NOCUSTOMFILTER" + index);
            switch(currentOperation.displayType) {
                case BROWSER_COLUMN_TYPE_DATE:
                case BROWSER_COLUMN_TYPE_TIME:
                case BROWSER_COLUMN_TYPE_CHARACTER:
                    break;
                case BROWSER_COLUMN_TYPE_NUMERIC:
                    if (currentOperation.visualComponent instanceof JLabel) ((JLabel) currentOperation.visualComponent).setHorizontalAlignment(SwingConstants.RIGHT);
                    if (currentOperation.visualComponent instanceof JTextField) ((JTextField) currentOperation.visualComponent).setHorizontalAlignment(JTextField.RIGHT);
                    break;
                case BROWSER_COLUMN_TYPE_NUMERIC2:
                    try {
                        double numericValue = KMetaUtilsClass.getDecimalNumericValueFromString(result);
                        result = decimalFormatter.format(numericValue);
                    } catch (Exception error) {
                        result = decimalFormatter.format(0);
                    }
                    if (currentOperation.visualComponent instanceof JLabel) ((JLabel) currentOperation.visualComponent).setHorizontalAlignment(SwingConstants.RIGHT);
                    if (currentOperation.visualComponent instanceof JTextField) ((JTextField) currentOperation.visualComponent).setHorizontalAlignment(JTextField.RIGHT);
                    break;
                case BROWSER_COLUMN_TYPE_CURRENCY:
                    try {
                        double numericValue = KMetaUtilsClass.getDecimalNumericValueFromString(result);
                        result = KMetaUtilsClass.toCurrencyString(numericValue);
                    } catch (Exception error) {
                        result = KMetaUtilsClass.toCurrencyString(0);
                    }
                    if (currentOperation.visualComponent instanceof JLabel) ((JLabel) currentOperation.visualComponent).setHorizontalAlignment(SwingConstants.RIGHT);
                    if (currentOperation.visualComponent instanceof JTextField) ((JTextField) currentOperation.visualComponent).setHorizontalAlignment(JTextField.RIGHT);
                    break;
            }
            if (currentOperation.visualComponent instanceof JLabel) ((JLabel) currentOperation.visualComponent).setText(result);
            if (currentOperation.visualComponent instanceof JTextField) ((JTextField) currentOperation.visualComponent).setText(result);
        }
        Iterator renderers = tableHeaderRendererList.iterator();
        while (renderers.hasNext()) {
            ((tableHeaderRendererClass) renderers.next()).update(tableModel);
        }
    }

    public AbstractMap evaluateOperation(String SQLformula, boolean applyCustomFilters) throws KExceptionClass {
        HashMap dbResult = new HashMap();
        dbResult.putAll(SQLPreprocessor.executeSQLOperation(SQLformula, applyCustomFilters));
        return (dbResult);
    }

    public long dataBaseRowCount(boolean applyCustomFilters) throws KExceptionClass {
        return (KMetaUtilsClass.getIntegralNumericValueFromString((String) (evaluateOperation(" COUNT( ALL 1 ) AS COUNT1 ", applyCustomFilters)).get("COUNT1")));
    }

    public static final String NEW_ACTION = "new";

    public static final String EDIT_ACTION = "edit";

    public static final String DELETE_ACTION = "delete";

    public static final String SAVE_ACTION = "save";

    public static final String COPY_ACTION = "copy";

    public static final String SORT_ACTION = "sort";

    public static final String FILTER_ACTION = "filter";

    public static final String REFRESH_ACTION = "refresh";

    public static final String PRINT_ACTION = "print";

    public static final String MOUSE_DBL_CLICK = "double_click";

    public void actionPerformed(java.awt.event.ActionEvent event) {
        try {
            String command = event.getActionCommand();
            stopTableCellEditing();
            if (command.equals(NEW_ACTION)) newButtonActionPerformed(); else if (command.equals(EDIT_ACTION)) editButtonActionPerformed(); else if (command.equals(DELETE_ACTION)) deleteButtonActionPerformed(); else if (command.equals(SAVE_ACTION)) saveButtonActionPerformed(); else if (command.equals(COPY_ACTION)) copyButtonActionPerformed(); else if (command.equals(SORT_ACTION)) sortButtonActionPerformed(); else if (command.equals(FILTER_ACTION)) filterButtonActionPerformed(); else if (command.equals(REFRESH_ACTION)) refreshButtonActionPerformed(); else if (command.equals(PRINT_ACTION)) printButtonActionPerformed();
            notifyListeners(command);
            refresh();
        } catch (Exception error) {
            log.log(this, KMetaUtilsClass.getStackTrace(error));
            KMetaUtilsClass.showErrorMessageFromException(parentWindow, error);
        }
    }

    public static final String MOUSE_RELEASED_ACTION = "mouseReleased";

    public static final String MOUSE_ENTER_ACTION = "mouseEntered";

    public static final String MOUSE_PRESSED_ACTION = "mousePressed";

    public static final String MOUSE_EXITED_ACTION = "mouseExited";

    public static final String MOUSE_DOUBLECLICK_ACTION = "doubleclick";

    public void newButtonActionPerformed() {
        newButtonActionPerformed(new HashMap());
    }

    public void newButtonActionPerformed(Map foreingKeyMap) {
        try {
            KMetaUtilsClass.cursorWait(parentWindow);
            JDialog Dialog = getPDCEditorWidow();
            ((KDialogInterface) Dialog).initializeDialog(KDialogInterface.CREATE_NEW_MODE, -1L, foreingKeyMap);
            KMetaUtilsClass.cursorNormal(parentWindow);
            Dialog.setVisible(true);
        } catch (KExceptionClass error) {
            log.log(this, error.longMessage);
            KMetaUtilsClass.showErrorMessageFromException(parentWindow, error);
        } catch (Exception error) {
            log.log(this, KMetaUtilsClass.getStackTrace(error));
            KMetaUtilsClass.showErrorMessageFromException(parentWindow, error);
        } finally {
            KMetaUtilsClass.cursorNormal(parentWindow);
        }
    }

    public void editButtonActionPerformed() {
        try {
            editButtonActionPerformed(getSelectedRowKey());
        } catch (KExceptionClass error) {
            log.log(this, error.longMessage);
            KMetaUtilsClass.showErrorMessageFromException(parentWindow, error);
        } catch (Exception error) {
            log.log(this, KMetaUtilsClass.getStackTrace(error));
            KMetaUtilsClass.showErrorMessageFromException(parentWindow, error);
        }
    }

    public void editButtonActionPerformed(long id) {
        try {
            KMetaUtilsClass.cursorWait(parentWindow);
            JDialog Dialog = getPDCEditorWidow();
            ((KDialogInterface) Dialog).initializeDialog(KDialogInterface.EDIT_MODE, id, null);
            KMetaUtilsClass.cursorNormal(parentWindow);
            Dialog.setVisible(true);
        } catch (KExceptionClass error) {
            log.log(this, error.longMessage);
            KMetaUtilsClass.showErrorMessageFromException(parentWindow, error);
        } catch (Exception error) {
            log.log(this, KMetaUtilsClass.getStackTrace(error));
            KMetaUtilsClass.showErrorMessageFromException(parentWindow, error);
        } finally {
            KMetaUtilsClass.cursorNormal(parentWindow);
        }
    }

    public void deleteButtonActionPerformed() {
        try {
            deleteButtonActionPerformed(getSelectedRowKey());
        } catch (KExceptionClass error) {
            log.log(this, error.longMessage);
            KMetaUtilsClass.showErrorMessageFromException(parentWindow, error);
        } catch (Exception error) {
            log.log(this, KMetaUtilsClass.getStackTrace(error));
            KMetaUtilsClass.showErrorMessageFromException(parentWindow, error);
        }
    }

    public void deleteButtonActionPerformed(long id) {
        try {
            String message = "Are you sure you want to delete?";
            if (KMetaUtilsClass.showConfirmationMessage(parentWindow, message).equals(" OK ")) {
                persistentObjectManagerClass persistentObjectManager = new persistentObjectManagerClass(configuration, log);
                KMetaUtilsClass.cursorWait(parentWindow);
                persistentObjectManager.delete(id, pdcObjectClass.getName());
                KMetaUtilsClass.cursorNormal(parentWindow);
            }
        } catch (KExceptionClass error) {
            log.log(this, error.longMessage);
            KMetaUtilsClass.showErrorMessageFromException(parentWindow, error);
        } catch (Exception error) {
            log.log(this, KMetaUtilsClass.getStackTrace(error));
            KMetaUtilsClass.showErrorMessageFromException(parentWindow, error);
        } finally {
            KMetaUtilsClass.cursorNormal(parentWindow);
        }
    }

    public JDialog getPDCEditorWidow() throws KExceptionClass {
        try {
            log.log(this, "Openning editor " + pdcEditorClass.getName());
            Class[] editorConstructorRequiredParam = new Class[] { KConfigurationClass.class, KLogClass.class, java.awt.Window.class };
            Object[] editorConstructorActualArguments = new Object[] { configuration, log, parentWindow };
            Constructor editorConstructor;
            try {
                editorConstructor = pdcEditorClass.getConstructor(editorConstructorRequiredParam);
            } catch (NoSuchMethodException error) {
                throw new KExceptionClass("Object Editor " + pdcEditorClass.getName() + " does not provide the required constructor (KConfigurationClass, KLogClass, java.awt.Frame) ", error);
            }
            return ((JDialog) editorConstructor.newInstance(editorConstructorActualArguments));
        } catch (Exception error) {
            log.log(this, KMetaUtilsClass.getStackTrace(error));
            throw new KExceptionClass("Error openning window " + pdcEditorClass.getName() + " :" + error.toString(), error);
        }
    }

    public void saveButtonActionPerformed() throws KExceptionClass {
        saveBrowserChanges();
    }

    /** witht this api, in a selected set of rows in a column the value of the selected field is copied to the range selected  */
    public void copyButtonActionPerformed() {
        try {
            KMetaUtilsClass.stopTableCellEditing(visualTable);
            int col = visualTable.getSelectedColumn();
            int anchorRow = selectionModel.getAnchorSelectionIndex();
            int[] rowArray = visualTable.getSelectedRows();
            if (col == -1) throw new KExceptionClass("\nYou need to chose a field", null);
            if (visualTable.isCellEditable(anchorRow, col)) {
                Object value = visualTable.getValueAt(anchorRow, col);
                for (int i = 0; i < rowArray.length; i++) visualTable.setValueAt(value, rowArray[i], col);
                visualTable.repaint();
            }
        } catch (Exception error) {
            log.log(this, KMetaUtilsClass.getStackTrace(error));
            KMetaUtilsClass.showErrorMessageFromException(parentWindow, error);
        }
    }

    public void sortButtonActionPerformed() throws KExceptionClass {
        log.log(this, "Openning setOrderScreen...");
        setOrderDialogClass setOrderScreen = new setOrderDialogClass(configuration, log, parentWindow, this);
        log.log(this, "Openning setOrderScreen completed.");
        setOrderScreen.setVisible(true);
    }

    public void filterButtonActionPerformed() throws KExceptionClass {
        log.log(this, "Openning setCriteriaScreen...");
        setCriteriaDialogClass setCriteriaScreen = new setCriteriaDialogClass(configuration, log, parentWindow, this);
        log.log(this, "Openning setCriteriaScreen completed.");
        setCriteriaScreen.setVisible(true);
    }

    public void refreshButtonActionPerformed() throws KExceptionClass {
    }

    public void printButtonActionPerformed() throws KExceptionClass {
        print("REPORT", "");
    }

    public void print(String report_name, String report_owner) throws KExceptionClass {
        try {
            log.log(this, "Start print job...");
            dbTransactionClientClass dbTransaction = new dbTransactionClientClass(configuration, log);
            prepareTransactionWithBrowserSQL(dbTransaction);
            dbTransaction.executeQuery(0, 655356);
            KPrintSectionClass headerSection = new KPrintSectionClass(configuration, log, 500, 90);
            headerSection.setFont(new Font("arial", Font.PLAIN, 9));
            headerSection.printText(report_owner, 370, 30);
            headerSection.printText(KMetaUtilsClass.time(), 442, 40);
            headerSection.setFont(new Font("arial", Font.PLAIN, 12));
            headerSection.printText(report_name, 212, 50);
            KPrintJobClass printJob = new KPrintJobClass(configuration, log);
            KPrintDataTableClass DBPrinter = new KPrintDataTableClass(configuration, log, dbTransaction, printJob, 0, 655356);
            String firstFieldName = "";
            java.util.List headerNames = SQLPreprocessor.getHeaderNames();
            Iterator headerIterator = headerNames.iterator();
            int countFields = 0;
            while (headerIterator.hasNext()) {
                countFields++;
                if (countFields > tableModel.getColumnCount()) break;
                String readableName = (String) headerIterator.next();
                if (firstFieldName.length() == 0) firstFieldName = readableName;
                String fieldName = SQLPreprocessor.getFieldName1(readableName);
                TableColumn theColumn = visualTable.getColumn(readableName);
                int fieldWidth = (int) (theColumn.getPreferredWidth() * 0.65);
                int fieldAlignment = KPrintSectionClass.LEFT;
                KTableCellRendererBaseClass renderer = (KTableCellRendererBaseClass) getColumnCellRenderer(readableName);
                if (renderer != null) {
                    switch(renderer.getColumnAligment()) {
                        case SwingConstants.LEFT:
                            fieldAlignment = KPrintSectionClass.LEFT;
                            break;
                        case SwingConstants.CENTER:
                            fieldAlignment = KPrintSectionClass.RIGHT;
                            break;
                        case SwingConstants.RIGHT:
                            fieldAlignment = KPrintSectionClass.RIGHT;
                            break;
                        default:
                            fieldAlignment = KPrintSectionClass.LEFT;
                    }
                }
                DBPrinter.addField(fieldName, readableName, fieldWidth, fieldAlignment);
            }
            DBPrinter.addSummary(firstFieldName, KPrintDataTableClass.COUNT, null, "-Records", 0);
            printSettingDialogClass printSettingDialog = new printSettingDialogClass(configuration, log, parentWindow, DBPrinter);
            if (printSettingDialog.dialogCloseResult()) {
                printJob.startPrintJob();
                printJob.setDefaultFont(new Font("arial", Font.PLAIN, 9));
                printJob.SetHeader(headerSection, KPrintJobClass.CENTER);
                printJob.setLeftMargin(50);
                printJob.setBottomMargin(40);
                DBPrinter.print();
                printJob.submitPrintJob();
            }
            log.log(this, "Print job handler finished.");
        } catch (Exception error) {
        } finally {
        }
    }

    public void mouseClickPerformed(java.awt.event.MouseEvent event) {
    }

    public void mouseDoubleClickPerformed(java.awt.event.MouseEvent event) {
        if (doubleClickEnabled == true) {
            try {
                editButtonActionPerformed();
                notifyListeners(MOUSE_DBL_CLICK);
                refresh();
            } catch (Exception error) {
                log.log(this, KMetaUtilsClass.getStackTrace(error));
                KMetaUtilsClass.showErrorMessageFromException(parentWindow, error);
            }
        }
    }

    public void mouseClicked(java.awt.event.MouseEvent event) {
        try {
            if (((System.currentTimeMillis() - previousTime) < Long.parseLong(configuration.getField("double_click_timer"))) && (event.getX() == previousX) && (event.getY() == previousY)) mouseDoubleClickPerformed(event);
            previousTime = System.currentTimeMillis();
            previousX = event.getX();
            previousY = event.getY();
        } catch (Exception error) {
            log.log(this, KMetaUtilsClass.getStackTrace(error));
            KMetaUtilsClass.showErrorMessageFromException(parentWindow, error);
        }
    }

    public void mouseReleased(java.awt.event.MouseEvent event) {
        mouseReleasedPerformed(event);
    }

    public void mouseEntered(java.awt.event.MouseEvent event) {
        mouseEnteredPerformed(event);
    }

    public void mousePressed(java.awt.event.MouseEvent event) {
        mousePressedPerformed(event);
    }

    public void mouseExited(java.awt.event.MouseEvent event) {
        mouseExitedPerformed(event);
    }

    public void mouseReleasedPerformed(java.awt.event.MouseEvent event) {
    }

    public void mouseEnteredPerformed(java.awt.event.MouseEvent event) {
    }

    public void mousePressedPerformed(java.awt.event.MouseEvent event) {
    }

    public void mouseExitedPerformed(java.awt.event.MouseEvent event) {
    }

    public void setCacheSize(int size) {
        tableModel.cacheSize = size;
    }

    public recordClass getRecord(long OID) throws KExceptionClass {
        return (tableModel.getRecord(OID));
    }

    public java.util.List getDataCache() {
        return (tableModel.getTableDataCache());
    }

    public java.util.List getTableDataHeaders() {
        return (tableModel.getTableDataHeaders());
    }

    public String getTableDataAsString() {
        String result = new String();
        {
            java.util.List headers = tableModel.getTableDataHeaders();
            Iterator headerIterator = headers.iterator();
            while (headerIterator.hasNext()) {
                result += headerIterator.next() + "\t";
            }
            result += "\n\n";
        }
        {
            java.util.List data = tableModel.getTableDataCache();
            Iterator rowIterator = data.iterator();
            while (rowIterator.hasNext()) {
                recordClass record = (recordClass) rowIterator.next();
                for (int index = 0; index < record.getRecordLength(); index++) {
                    result += record.getValueAt(index) + "\t";
                }
                result += "\n";
            }
            result += "\n\n";
        }
        return (result);
    }

    public java.util.List getAllTableData() {
        return (tableModel.getTableDataCache());
    }

    public String getTableDataAsHtmlTable() {
        String result = new String();
        result = "<table border=2 >\n";
        {
            java.util.List headers = tableModel.getTableDataHeaders();
            Iterator headerIterator = headers.iterator();
            result += "<tr>\n";
            while (headerIterator.hasNext()) {
                result += "<td>";
                result += "<b>" + headerIterator.next() + "</b>";
            }
            result += "</tr>\n";
        }
        {
            java.util.List data = tableModel.getTableDataCache();
            Iterator rowIterator = data.iterator();
            while (rowIterator.hasNext()) {
                recordClass record = (recordClass) rowIterator.next();
                result += "<tr>";
                for (int index = 0; index < record.getRecordLength(); index++) {
                    result += "<td>";
                    result += record.getValueAt(index);
                    if (index + 1 >= tableModel.getColumnCount()) break;
                }
                result += "</tr>\n";
            }
        }
        result += "</table>";
        return (result);
    }

    public void markChanged(long OID, String changedParam) throws KExceptionClass {
        recordClass record = getRecord(OID);
        record.setValueAt(record.getRecordLength() - 2, changedParam);
    }

    public void setVisibleColumnCount(int visibleColumnCountParam) {
        tableModel.setVisibleColumnCount(visibleColumnCountParam);
    }
}
