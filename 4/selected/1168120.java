package KFrameWork.Widgets;

import KFrameWork.Communication.dbTransactionClientClass;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import KFrameWork.Base.*;
import KFrameWork.Printing.*;

public class DataBrowserBaseClass extends Object implements ActionListener, MouseListener {

    public static final int CUSTOM_CRITERIA_ROW_COUNT = 50;

    public static final int CHARACTER = 0;

    public static final int NUMERIC = 1;

    public static final int NUMERIC2 = 2;

    public static final int CURRENCY = 3;

    public static final int DATE = 4;

    public static final int TIME = 5;

    protected configurationClass configuration;

    protected logClass log;

    protected Component component;

    protected JTable visualTable;

    protected boolean listenerRegistered = false;

    protected java.util.List listenerList;

    private SQLPreprocessorClass SQLPreprocessor;

    public tableModelClass tableModel;

    private java.util.List labelOperationList;

    private String[][] customCriteriaRowData;

    private java.util.List customOrderData;

    private java.util.List tableHeaderRendererList;

    private ListSelectionModel selectionModel;

    private java.util.Map columnDefaultRender;

    private boolean tableLoaded = false;

    private NumberFormat currencyFormatter;

    private NumberFormat decimalFormatter;

    private boolean doubleClickEnabled = true;

    private long previousTime = 0;

    private long previousX = -1;

    private long previousY = -1;

    public interface tableFillerListenerInterface {

        public abstract void tableFillerActionPerformed(String action);
    }

    public class customRendererClass extends DefaultTableCellRenderer {

        public int columnType;

        private tableModelClass tableModel;

        logClass log;

        public int aligment;

        private Font columnFont;

        public customRendererClass(int columnTypeParam, tableModelClass tableModelParam, logClass logParam) throws KExceptionClass {
            tableModel = tableModelParam;
            setDataType(columnTypeParam);
            log = logParam;
            columnFont = null;
            aligment = SwingConstants.LEFT;
        }

        private void setDataType(int dataType) throws KExceptionClass {
            switch(dataType) {
                case CHARACTER:
                case NUMERIC:
                case NUMERIC2:
                case CURRENCY:
                case DATE:
                case TIME:
                    break;
                default:
                    throw new KExceptionClass("Could not set renderer data type. Type specified is invalid.", null);
            }
            columnType = dataType;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            java.awt.Component result = this;
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (columnFont != null) setFont(columnFont);
            if (tableModel.cellDataCalcHook1 != null) {
                try {
                    customRendererClass newRenderer = new customRendererClass(DataBrowserBaseClass.CHARACTER, tableModel, log);
                    newRenderer.setFont(new Font("arial", Font.PLAIN, 10));
                    newRenderer.setValue(value);
                    newRenderer.setDataType(columnType);
                    if (tableModel.cellDataCalcHook1.cellRenderingHook(row, column, isSelected, newRenderer, tableModel.getColumnName(column), (String) value, tableModel.getRecord(tableModel.getKeyValue(row)), log)) {
                        result = newRenderer;
                    }
                } catch (Exception error) {
                    setText("**ERROR**");
                    log.log(this, "**ERROR**:" + metaUtilsClass.getStackTrace(error));
                }
            }
            if (result instanceof customRendererClass) switch(columnType) {
                case DATE:
                    break;
                case TIME:
                    break;
                case CHARACTER:
                    break;
                case NUMERIC:
                    ((customRendererClass) result).setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
                case NUMERIC2:
                    if (((String) value).equals("")) {
                        ((customRendererClass) result).setText("");
                    } else {
                        try {
                            double numericValue = Double.parseDouble((String) value);
                            ((customRendererClass) result).setText(decimalFormatter.format(numericValue));
                        } catch (Exception error) {
                            ((customRendererClass) result).setText((String) value);
                        }
                        ;
                    }
                    ;
                    ((customRendererClass) result).setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
                case CURRENCY:
                    if (((String) value).equals("")) {
                        ((customRendererClass) result).setText("");
                    } else {
                        try {
                            double numericValue = Double.parseDouble((String) value);
                            ((customRendererClass) result).setText(currencyFormatter.format(numericValue));
                        } catch (Exception error) {
                            ((customRendererClass) result).setText("#" + (String) value + "#");
                        }
                        ;
                    }
                    ;
                    ((customRendererClass) result).setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
            }
            return result;
        }

        public void setHorizontalAlignment(int alignmentParam) {
            aligment = alignmentParam;
            super.setHorizontalAlignment(aligment);
        }
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

    /** Creates new DataBrowserBaseClass */
    public DataBrowserBaseClass(configurationClass configurationParam, logClass logParam, boolean showKeyFieldParam, JTable tableParam, Component componentParam) throws KExceptionClass {
        super();
        configuration = configurationParam;
        log = logParam;
        component = componentParam;
        visualTable = tableParam;
        SQLPreprocessor = new SQLPreprocessorClass(configuration, log, showKeyFieldParam);
        tableModel = new tableModelClass(configuration, log, component, SQLPreprocessor, visualTable);
        selectionModel = visualTable.getSelectionModel();
        currencyFormatter = NumberFormat.getCurrencyInstance();
        decimalFormatter = new DecimalFormat("0.00000");
        labelOperationList = new ArrayList();
        customOrderData = new ArrayList();
        columnDefaultRender = new HashMap();
        listenerList = new ArrayList();
        tableHeaderRendererList = new ArrayList();
        visualTable.addMouseListener(this);
        doubleClickEnabled = true;
        log.log(this, "Constructed successfully.");
    }

    public void setDoubleClickEnabled(boolean doubleClickEnabledParam) {
        doubleClickEnabled = doubleClickEnabledParam;
    }

    public void registerListener(tableFillerListenerInterface listenerParam) {
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
            ((tableFillerListenerInterface) listener.next()).tableFillerActionPerformed(actionParam);
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

    public void bindDefaultParameter(String parameterName, String parameterValue) {
        SQLPreprocessor.bindDefaultParameter(parameterName, parameterValue);
    }

    public void bindDefaultParameter(String parameterName, long parameterValue) {
        SQLPreprocessor.bindDefaultParameter(parameterName, String.valueOf(parameterValue));
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
    public void bindCustomParameter(String parameterName, String parameterValue) {
        SQLPreprocessor.bindCustomParameter(parameterName, parameterValue);
    }

    public void bindCustomParameter(String parameterName, long parameterValue) {
        SQLPreprocessor.bindCustomParameter(parameterName, String.valueOf(parameterValue));
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
            customRendererClass newRenderer = new customRendererClass(CHARACTER, tableModel, log);
            if (tableModel.isTableEditable()) {
                String columnName = tableModel.getColumnName(i);
                Iterator headerInfo = SQLPreprocessor.headerFieldNameList.iterator();
                while (headerInfo.hasNext()) {
                    SQLPreprocessorClass.fieldInformationClass currentHeaderInfo = (SQLPreprocessorClass.fieldInformationClass) headerInfo.next();
                    if (currentHeaderInfo.headerName.equals(columnName)) {
                        if (!currentHeaderInfo.columnEditable) ((JLabel) newRenderer).setBackground(new Color(204, 204, 204));
                    }
                }
            }
            Boolean flag = (Boolean) columnDefaultRender.get(tableModel.getColumnName(i));
            boolean defaultRender = flag == null ? false : flag.booleanValue();
            if (tableModel.getColumnClass(i).getName().equalsIgnoreCase("java.lang.String") && !defaultRender) {
                TableColumn theColumn = visualTable.getColumn(tableModel.getColumnName(i));
                theColumn.setCellRenderer((TableCellRenderer) newRenderer);
            }
            if (SQLPreprocessor.getFieldType1(i).equals("numeric")) {
                customRendererClass renderer = (customRendererClass) getColumnCellRenderer(tableModel.getColumnName(i));
                if (renderer != null) renderer.setDataType(NUMERIC);
                log.log(this, " auto setting " + tableModel.getColumnName(i) + " to numeric");
            }
            if (SQLPreprocessor.getFieldType1(i).equals("datetime")) {
                customRendererClass renderer = (customRendererClass) getColumnCellRenderer(tableModel.getColumnName(i));
                if (renderer != null) renderer.setDataType(DATE);
                log.log(this, " auto setting " + tableModel.getColumnName(i) + " to datetime");
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

    public void setBrowserReadWrite(boolean flag, String tableAlias) {
        tableModel.setTableEditable(flag, tableAlias);
        log.log(this, "Set browser read and write " + flag);
    }

    public void saveBrowserChanges(tableFillerDataWriterInterface dataWriter) throws KExceptionClass {
        tableModel.saveChanges(dataWriter);
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
        component.repaint();
    }

    /**   Reload the table as the new setting applied.  */
    public void refresh() throws KExceptionClass {
        tableModel.refresh();
        visualTable.scrollRectToVisible(new Rectangle());
        tableModel.fireTableDataChanged();
        calculateOperatios();
        component.repaint();
    }

    /**   Reload the table as the new setting applied.  */
    public void softRefresh() throws KExceptionClass {
        tableModel.load(visualTable.getSelectedRow());
        tableModel.fireTableDataChanged();
        calculateOperatios();
        component.repaint();
    }

    /**   Reload the table as the new setting applied.  */
    public void displayRefresh() throws KExceptionClass {
        tableModel.fireTableDataChanged();
        component.repaint();
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
        component.repaint();
    }

    /** This method is for retrieving previous custom input orders. */
    public java.util.List GetCustomOrderData() {
        return customOrderData;
    }

    /**   Return the key field value of current selected table row.  */
    public long getSelectedRowKey() throws KExceptionClass {
        int index = visualTable.getSelectedRow();
        if (index == -1) {
            throw new KExceptionClass("\nDebe seleccionar un registro!", null);
        }
        return tableModel.getKeyValue(index);
    }

    /**   Return the key field values of current multi selected rows marked by Check Box. 
     *    column_index indicates the Check Box column.  */
    public Vector getCheckSelectedRowKeys(int column_index) throws KExceptionClass {
        if (column_index >= tableModel.getColumnCount()) throw new KExceptionClass("*** Method calling error **** \n" + " Column with index [" + column_index + "] does not exist !", null);
        Vector column_data = tableModel.getColumnData(column_index);
        Vector indexes = new Vector();
        for (int i = 0; i < column_data.size(); i++) {
            Boolean obj = (Boolean) column_data.get(i);
            if (obj.booleanValue()) indexes.add(new Integer(i));
        }
        if (indexes.size() == 0) {
            throw new KExceptionClass("\nYou need to chose records!", null);
        }
        Vector selectedKeys = new Vector();
        for (int i = 0; i < indexes.size(); i++) {
            int index = ((Integer) indexes.get(i)).intValue();
            selectedKeys.add(new Long(tableModel.getKeyValue(index)));
        }
        return selectedKeys;
    }

    /**   Return the key field values of current multi selected table rows.  */
    public Vector getMultiSelectedRowKeys() throws KExceptionClass {
        int[] indexes = visualTable.getSelectedRows();
        if (indexes.length == 0) {
            throw new KExceptionClass("\nDebe seleccionar un registro!", null);
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
            throw new KExceptionClass("\nDebe seleccionar un registro!", null);
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
            throw new KExceptionClass("\nDebe seleccionar un registro!", null);
        }
        return (visualTable.getColumnName(index));
    }

    /**   Set up label and operation list  
          This method should be called before first call to method calculateOperation() */
    public void saveSQLOperation(Object visualComponent, String sqlOperation, boolean reflectCustomFilter) throws KExceptionClass {
        saveSQLOperation(visualComponent, sqlOperation, NUMERIC, reflectCustomFilter);
    }

    /**   Set up label and operation list  
          This method should be called before first call to method calculateOperation() */
    public void saveSQLOperation(Object visualComponent, String sqlOperation, int dataType, boolean reflectCustomFilter) throws KExceptionClass {
        if (tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'saveLabelOperation' method is called in wrong order !", null);
        switch(dataType) {
            case CHARACTER:
            case NUMERIC:
            case NUMERIC2:
            case DATE:
            case TIME:
            case CURRENCY:
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

    /** Set the Font for the column 
        This method should be called after initializeTable method called */
    public void adjustColumnFont(String columnName, Font font) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'setColumnFont' method is called in wrong order !", null);
        customRendererClass renderer = (customRendererClass) getColumnCellRenderer(columnName);
        if (renderer != null) renderer.columnFont = font;
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
            log.log(this, metaUtilsClass.getStackTrace(error));
            String message;
            message = "*** Colud not display table *** [";
            message += error.toString();
            message += "]\n Column not found in display -> [" + columnName + "]";
            metaUtilsClass.showErrorMessage(component, message);
        }
        ;
    }

    /** Set the data type for the column 
        This method should be called after initializeTable method called */
    public void adjustColumnType(String columnName, int type) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'setColumnType' method is called in wrong order !", null);
        customRendererClass renderer = (customRendererClass) getColumnCellRenderer(columnName);
        if (renderer != null) renderer.setDataType(type);
        log.log(this, " user setting " + columnName + " to " + type);
    }

    /** Sets the justification for the column 
        This method should be called after initializeTable method called */
    public void adjustColumnJustification(String columnName, int alignment) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'setColumnJustification' method is called in wrong order !", null);
        customRendererClass renderer = (customRendererClass) getColumnCellRenderer(columnName);
        if (renderer != null) renderer.setHorizontalAlignment(alignment);
    }

    /** Sets the foreground color for the column 
        This method should be called after initializeTable method called */
    public void adjustColumnForegroundColor(String columnName, Color fgColor) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'setColumnForegroundColor' method is called in wrong order !", null);
        customRendererClass renderer = (customRendererClass) getColumnCellRenderer(columnName);
        if (renderer != null) renderer.setForeground(fgColor);
    }

    /** Sets the background color for the column 
        This method should be called after initializeTable method called */
    public void adjustColumnBackgroundColor(String columnName, Color bgColor) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'setColumnBackgroundColor' method is called in wrong order !", null);
        customRendererClass renderer = (customRendererClass) getColumnCellRenderer(columnName);
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
        setColumnNames(aliasName, fieldName, headerName, colEditable, false);
    }

    public void setColumnNames(String aliasName, String fieldName, String headerName, boolean colEditable, boolean defaultRender) throws KExceptionClass {
        if (tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'setColumnNames' method is called in wrong order !", null);
        if (colEditable && (!aliasName.equals(tableModel.getEditableTableAlias()))) throw new KExceptionClass("*** Method calling error **** \n" + "Column [" + headerName + "] is not editable !", null);
        SQLPreprocessor.setColumnNames(aliasName, fieldName, headerName, colEditable);
        columnDefaultRender.put(headerName, new Boolean(defaultRender));
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

    /** Gets the table column type via column name 
        This method should be called after initializeTable.*/
    public int getColumnType(String columnName) throws KExceptionClass {
        if (!tableLoaded) throw new KExceptionClass("*** Method calling error **** \n" + "'getColumnType' method is called in wrong order !", null);
        customRendererClass renderer = (customRendererClass) getColumnCellRenderer(columnName);
        if (renderer != null) return renderer.columnType; else return CHARACTER;
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

    /**
     * Using a JTextField, JCheckBox, or JComboBox as an Editor 
     * If you are setting the editor for a single column of cells (using the TableColumn setCellEditor method),
     * you specify the editor using an argument that adheres to the TableCellEditor interface. 
     * The DefaultCellEditor class implements this interface and provides constructors 
     * to let you specify an editing component that is a JTextField, JCheckBox, or JComboBox.
     * For example: TableColumn theColumn = table.getColumnModel().getColumn(index);
     *              theColumn.setCellEditor(new DefaultCellEditor(comboBox));
     * 
     * Usually you do not have to explicitly specify a check box as an editor, 
     * since columns with Boolean data automatically use a check box renderer and editor. 
     * Similar situation to JTextField is true.
     */
    public void adjustColumnEditor(int column_index, TableCellEditor CellEditor) throws KExceptionClass {
        if (column_index >= tableModel.getColumnCount()) throw new KExceptionClass("*** Method calling error **** \n" + " Column with index [" + column_index + "] does not exist !", null);
        if (!tableModel.isCellEditable(0, column_index)) throw new KExceptionClass("\nColumn [" + column_index + " ] is not editable!", null);
        TableColumn theColumn = visualTable.getColumnModel().getColumn(column_index);
        theColumn.setCellEditor(CellEditor);
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
                case DATE:
                case TIME:
                case CHARACTER:
                    break;
                case NUMERIC:
                    if (currentOperation.visualComponent instanceof JLabel) ((JLabel) currentOperation.visualComponent).setHorizontalAlignment(SwingConstants.RIGHT);
                    if (currentOperation.visualComponent instanceof JTextField) ((JTextField) currentOperation.visualComponent).setHorizontalAlignment(JTextField.RIGHT);
                    break;
                case NUMERIC2:
                    try {
                        double numericValue = Double.parseDouble(result);
                        result = decimalFormatter.format(numericValue);
                    } catch (Exception error) {
                        result = "0.00000";
                    }
                    if (currentOperation.visualComponent instanceof JLabel) ((JLabel) currentOperation.visualComponent).setHorizontalAlignment(SwingConstants.RIGHT);
                    if (currentOperation.visualComponent instanceof JTextField) ((JTextField) currentOperation.visualComponent).setHorizontalAlignment(JTextField.RIGHT);
                    break;
                case CURRENCY:
                    try {
                        double numericValue = Double.parseDouble(result);
                        result = currencyFormatter.format(numericValue);
                    } catch (Exception error) {
                        result = "$ 0.00";
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
        return (Long.parseLong((String) (evaluateOperation(" COUNT( ALL 1 ) AS COUNT1 ", applyCustomFilters)).get("COUNT1")));
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
        String command = event.getActionCommand();
        if (command.equals(NEW_ACTION)) newButtonActionPerformed(); else if (command.equals(EDIT_ACTION)) editButtonActionPerformed(); else if (command.equals(DELETE_ACTION)) deleteButtonActionPerformed(); else if (command.equals(SAVE_ACTION)) saveButtonActionPerformed(); else if (command.equals(COPY_ACTION)) copyButtonActionPerformed(); else if (command.equals(SORT_ACTION)) sortButtonActionPerformed(); else if (command.equals(FILTER_ACTION)) filterButtonActionPerformed(); else if (command.equals(REFRESH_ACTION)) refreshButtonActionPerformed(); else if (command.equals(PRINT_ACTION)) printButtonActionPerformed();
        notifyListeners(command);
    }

    public static final String MOUSE_RELEASED_ACTION = "mouseReleased";

    public static final String MOUSE_ENTER_ACTION = "mouseEntered";

    public static final String MOUSE_PRESSED_ACTION = "mousePressed";

    public static final String MOUSE_EXITED_ACTION = "mouseExited";

    public static final String MOUSE_DOUBLECLICK_ACTION = "doubleclick";

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

    public void mouseClicked(java.awt.event.MouseEvent event) {
        try {
            if (((System.currentTimeMillis() - previousTime) < Long.parseLong(configuration.getField("double_click_timer"))) && (event.getX() == previousX) && (event.getY() == previousY)) mouseDoubleClickPerformed(event);
            previousTime = System.currentTimeMillis();
            previousX = event.getX();
            previousY = event.getY();
        } catch (Exception error) {
            log.log(this, metaUtilsClass.getStackTrace(error));
            metaUtilsClass.showErrorMessage(component, error.toString());
        }
    }

    public void newButtonActionPerformed() {
    }

    public void editButtonActionPerformed() {
    }

    public void deleteButtonActionPerformed() {
    }

    public void saveButtonActionPerformed() {
    }

    public void copyButtonActionPerformed() {
        try {
            metaUtilsClass.stopTableCellEditing(visualTable);
            int col = visualTable.getSelectedColumn();
            int anchorRow = selectionModel.getAnchorSelectionIndex();
            int[] rowArray = visualTable.getSelectedRows();
            if (col == -1) throw new KExceptionClass("\nDebe seleccionar una celda!", null);
            if (visualTable.isCellEditable(anchorRow, col)) {
                Object value = visualTable.getValueAt(anchorRow, col);
                for (int i = 0; i < rowArray.length; i++) visualTable.setValueAt(value, rowArray[i], col);
                visualTable.repaint();
            }
        } catch (Exception error) {
            log.log(this, metaUtilsClass.getStackTrace(error));
            metaUtilsClass.showErrorMessage(component, error.toString());
        }
    }

    public void sortButtonActionPerformed() {
        try {
            log.log(this, "Openning setOrderScreen...");
            setOrderDialogClass setOrderScreen = new setOrderDialogClass(configuration, log, metaUtilsClass.getParentFrame(component), this);
            log.log(this, "Openning setOrderScreen completed.");
            setOrderScreen.show();
        } catch (Exception error) {
            log.log(this, metaUtilsClass.getStackTrace(error));
            metaUtilsClass.showErrorMessage(component, error.toString());
        }
    }

    public void filterButtonActionPerformed() {
        try {
            log.log(this, "Openning setCriteriaScreen...");
            setCriteriaDialogClass setCriteriaScreen = new setCriteriaDialogClass(configuration, log, metaUtilsClass.getParentFrame(component), this);
            log.log(this, "Openning setCriteriaScreen completed.");
            setCriteriaScreen.show();
        } catch (Exception error) {
            log.log(this, metaUtilsClass.getStackTrace(error));
            metaUtilsClass.showErrorMessage(component, error.toString());
        }
    }

    public void refreshButtonActionPerformed() {
        try {
            refresh();
        } catch (Exception error) {
            log.log(this, metaUtilsClass.getStackTrace(error));
            metaUtilsClass.showErrorMessage(component, error.toString());
        }
    }

    public void printButtonActionPerformed() {
        try {
            print("REPORT", "");
        } catch (Exception error) {
            log.log(this, metaUtilsClass.getStackTrace(error));
            metaUtilsClass.showErrorMessage(component, error.toString());
        }
    }

    public void mouseClickPerformed(java.awt.event.MouseEvent event) {
    }

    public void mouseDoubleClickPerformed(java.awt.event.MouseEvent event) {
        if (doubleClickEnabled == true) {
            try {
                try {
                    metaUtilsClass.cursorWait(metaUtilsClass.getParentFrame(component));
                    editButtonActionPerformed();
                    notifyListeners(MOUSE_DBL_CLICK);
                } finally {
                    metaUtilsClass.cursorNormal(metaUtilsClass.getParentFrame(component));
                }
            } catch (Exception error) {
                log.log(this, metaUtilsClass.getStackTrace(error));
                metaUtilsClass.showErrorMessage(component, error.toString());
            }
        }
    }

    public void mouseReleasedPerformed(java.awt.event.MouseEvent event) {
    }

    public void mouseEnteredPerformed(java.awt.event.MouseEvent event) {
    }

    public void mousePressedPerformed(java.awt.event.MouseEvent event) {
    }

    public void mouseExitedPerformed(java.awt.event.MouseEvent event) {
    }

    public void print(String report_name, String report_owner) throws KExceptionClass {
        log.log(this, "Start print job...");
        dbTransactionClientClass dbTransaction = new dbTransactionClientClass(configuration, log, configuration.getField("server_address"), configuration.getField("SESSION"), configuration.getField("SESSIONKEY"));
        prepareTransactionWithBrowserSQL(dbTransaction);
        dbTransaction.executeQuery(0, 655356);
        printSectionClass headerSection = new printSectionClass(configuration, log, 500, 90);
        headerSection.setFont(new Font("arial", Font.PLAIN, 9));
        headerSection.printText(report_owner, 370, 30);
        headerSection.printText(metaUtilsClass.time(), 442, 40);
        headerSection.setFont(new Font("arial", Font.PLAIN, 12));
        headerSection.printText(report_name, 212, 50);
        KPrintJobClass printJob = new KPrintJobClass(configuration, log, metaUtilsClass.getParentFrame(component));
        DBPrinterClass DBPrinter = new DBPrinterClass(configuration, log, dbTransaction, printJob, 0, 655356);
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
            int fieldAlignment = printSectionClass.LEFT;
            customRendererClass renderer = (customRendererClass) getColumnCellRenderer(readableName);
            if (renderer != null) {
                switch(renderer.aligment) {
                    case SwingConstants.LEFT:
                        fieldAlignment = printSectionClass.LEFT;
                        break;
                    case SwingConstants.CENTER:
                        fieldAlignment = printSectionClass.RIGHT;
                        break;
                    case SwingConstants.RIGHT:
                        fieldAlignment = printSectionClass.RIGHT;
                        break;
                    default:
                        fieldAlignment = printSectionClass.LEFT;
                }
            }
            DBPrinter.addField(fieldName, readableName, fieldWidth, fieldAlignment);
        }
        DBPrinter.addSummary(firstFieldName, DBPrinterClass.COUNT, null, "-Records", 0);
        printSettingDialogClass printSettingDialog = new printSettingDialogClass(configuration, log, metaUtilsClass.getParentFrame(component), DBPrinter);
        if (printSettingDialog.dialogCloseResult()) {
            printJob.startPrintJob();
            printJob.setDefaultFont(new Font("arial", Font.PLAIN, 9));
            printJob.SetHeader(headerSection, KPrintJobClass.CENTER);
            printJob.setLeftMargin(50);
            printJob.setBottomMargin(40);
            DBPrinter.print();
            printJob.endPrintJob();
        }
        log.log(this, "Print job handler finished.");
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
