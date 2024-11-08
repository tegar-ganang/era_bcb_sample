package realcix20.guis.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;
import org.jdesktop.swingx.JXTable;
import realcix20.classes.basic.BaseClass;
import realcix20.classes.basic.Column;
import realcix20.guis.components.JTableX;
import realcix20.guis.utils.DialogManager;
import realcix20.guis.utils.TxtManager;
import realcix20.guis.views.LayoutEditView;
import realcix20.utils.CSQLUtil;
import realcix20.utils.DAO;
import realcix20.utils.Resources;

public class LayoutEditListener implements ActionListener {

    private LayoutEditView container;

    public LayoutEditListener(LayoutEditView container) {
        this.container = container;
    }

    private String getSQL() {
        DefaultTableModel cModel = (DefaultTableModel) container.getColumnTable().getModel();
        DefaultTableModel cdModel = (DefaultTableModel) container.getColumnDisplayTable().getModel();
        DefaultTableModel cosModel = (DefaultTableModel) container.getColumnOrderSourceTable().getModel();
        DefaultTableModel coModel = (DefaultTableModel) container.getColumnOrderTable().getModel();
        DefaultTableModel caModel = (DefaultTableModel) container.getColumnAllTable().getModel();
        StringBuffer sb = new StringBuffer("");
        sb.append("SELECT ");
        for (int i = 0; i < cdModel.getRowCount(); i++) {
            Column column = (Column) cdModel.getValueAt(i, 0);
            if ((cdModel.getValueAt(i, 1) == null) && (cdModel.getValueAt(i, 2) == null)) {
                sb.append(column.getTableName() + "." + column.getColumnName() + ",");
            } else if (cdModel.getValueAt(i, 1) != null) {
                Boolean isCount = (Boolean) cdModel.getValueAt(i, 1);
                if (isCount) sb.append("COUNT(" + column.getTableName() + "." + column.getColumnName() + ") AS COUNT_" + column.getTableName() + "_" + column.getColumnName() + ","); else {
                    sb.append(column.getTableName() + "." + column.getColumnName() + ",");
                }
            } else if (cdModel.getValueAt(i, 2) != null) {
                Boolean isSum = (Boolean) cdModel.getValueAt(i, 2);
                if (isSum) sb.append("SUM(" + column.getTableName() + "." + column.getColumnName() + ") AS SUM_" + column.getTableName() + "_" + column.getColumnName() + ","); else {
                    sb.append(column.getTableName() + "." + column.getColumnName() + ",");
                }
            }
        }
        sb.replace(sb.length() - 1, sb.length(), " ");
        BaseClass object = container.getObject();
        String basicSQL = object.getBasicSQL();
        if (basicSQL.indexOf("WHERE") == -1) basicSQL = basicSQL.substring(basicSQL.indexOf("FROM")); else {
            basicSQL = basicSQL.substring(basicSQL.indexOf("FROM"), basicSQL.indexOf("WHERE"));
        }
        sb.append(basicSQL + " ");
        boolean hasWhere = false;
        basicSQL = object.getBasicSQL();
        if (basicSQL.indexOf("WHERE") == -1) sb.append("WHERE "); else {
            hasWhere = true;
            basicSQL = basicSQL.substring(basicSQL.indexOf("WHERE"));
            sb.append(basicSQL + " AND ");
        }
        Vector filters = new Vector();
        Vector andors = new Vector();
        for (int i = 0; i < caModel.getRowCount(); i++) {
            if ((caModel.getValueAt(i, 2) != null) && (!caModel.getValueAt(i, 2).toString().trim().equals(""))) {
                hasWhere = true;
                String andorString = (String) caModel.getValueAt(i, 0);
                boolean andor = (andorString.equals("AND"));
                andors.add(andor);
                Column column = (Column) caModel.getValueAt(i, 1);
                String filterString = (String) caModel.getValueAt(i, 2);
                filterString = filterString.replaceAll("~", column.getTableName() + "." + column.getColumnName());
                filters.add(filterString);
            }
        }
        if (hasWhere) {
            for (int i = 0; i < filters.size(); i++) {
                Boolean andor = (Boolean) andors.get(i);
                if (andor) {
                    String filterString = (String) filters.get(i);
                    sb.append(filterString + " AND ");
                }
            }
            for (int i = 0; i < filters.size(); i++) {
                Boolean andor = (Boolean) andors.get(i);
                if (!andor) {
                    String filterString = (String) filters.get(i);
                    sb.append(filterString + " OR  ");
                }
            }
        }
        if (!hasWhere) sb.replace(sb.lastIndexOf("WHERE"), sb.lastIndexOf("WHERE") + 5, ""); else sb.replace(sb.length() - 4, sb.length() - 1, "");
        boolean hasGroup = true;
        boolean hasCountOrSum = false;
        for (int i = 0; i < cdModel.getRowCount(); i++) {
            if ((cdModel.getValueAt(i, 1) == null) && (cdModel.getValueAt(i, 2) == null)) {
            } else if (cdModel.getValueAt(i, 1) != null) {
                Boolean isCount = (Boolean) cdModel.getValueAt(i, 1);
                if (isCount) hasCountOrSum = true;
            } else if (cdModel.getValueAt(i, 2) != null) {
                Boolean isSum = (Boolean) cdModel.getValueAt(i, 2);
                if (isSum) hasCountOrSum = true;
            }
        }
        if (!hasCountOrSum) hasGroup = false;
        if (hasGroup) {
            sb.append("GROUP BY ");
            for (int i = 0; i < cdModel.getRowCount(); i++) {
                Column column = (Column) cdModel.getValueAt(i, 0);
                if ((cdModel.getValueAt(i, 1) == null) && (cdModel.getValueAt(i, 2) == null)) {
                    sb.append(column.getTableName() + "." + column.getColumnName() + ",");
                } else if (cdModel.getValueAt(i, 1) != null) {
                    Boolean isCount = (Boolean) cdModel.getValueAt(i, 1);
                    if (!isCount) sb.append(column.getTableName() + "." + column.getColumnName() + ",");
                } else if (cdModel.getValueAt(i, 2) != null) {
                    Boolean isSum = (Boolean) cdModel.getValueAt(i, 2);
                    if (!isSum) sb.append(column.getTableName() + "." + column.getColumnName() + ",");
                }
            }
            sb.replace(sb.length() - 1, sb.length(), " ");
        }
        boolean hasOrder = false;
        if (coModel.getRowCount() > 0) hasOrder = true;
        if (hasOrder) {
            sb.append("ORDER BY ");
            for (int i = 0; i < coModel.getRowCount(); i++) {
                Column column = (Column) coModel.getValueAt(i, 0);
                Boolean order = false;
                if (coModel.getValueAt(i, 1) != null) order = (Boolean) coModel.getValueAt(i, 1);
                String orderString = "DESC";
                if (order) orderString = "ASC";
                int tempRow = findRow(column, container.getColumnDisplayTable());
                if ((cdModel.getValueAt(tempRow, 1) == null) && (cdModel.getValueAt(tempRow, 2) == null)) {
                    sb.append(column.getTableName() + "." + column.getColumnName() + " " + orderString + ",");
                } else if (cdModel.getValueAt(tempRow, 1) != null) {
                    Boolean isCount = (Boolean) cdModel.getValueAt(tempRow, 1);
                    if (isCount) sb.append("COUNT(" + column.getTableName() + "." + column.getColumnName() + ") " + orderString + ","); else {
                        sb.append(column.getTableName() + "." + column.getColumnName() + " " + orderString + ",");
                        hasGroup = true;
                    }
                } else if (cdModel.getValueAt(tempRow, 2) != null) {
                    Boolean isSum = (Boolean) cdModel.getValueAt(tempRow, 2);
                    if (isSum) sb.append("SUM(" + column.getTableName() + "." + column.getColumnName() + ") " + orderString + ","); else {
                        sb.append(column.getTableName() + "." + column.getColumnName() + " " + orderString + ",");
                        hasGroup = true;
                    }
                }
            }
            sb.replace(sb.length() - 1, sb.length(), " ");
        }
        return sb.toString();
    }

    private boolean saveSettings() {
        DefaultTableModel cModel = (DefaultTableModel) container.getColumnTable().getModel();
        DefaultTableModel cdModel = (DefaultTableModel) container.getColumnDisplayTable().getModel();
        DefaultTableModel cosModel = (DefaultTableModel) container.getColumnOrderSourceTable().getModel();
        DefaultTableModel coModel = (DefaultTableModel) container.getColumnOrderTable().getModel();
        DefaultTableModel caModel = (DefaultTableModel) container.getColumnAllTable().getModel();
        String sql = getSQL();
        int clsId = container.getObject().getClsId();
        int layout = container.getLayout_C();
        String csql = container.getCSQL();
        DAO dao = DAO.getInstance();
        dao.setAutoCommit(false);
        boolean flag = false;
        if (container.getCommand().equals("ADD") || container.getCommand().equals("COPY")) {
            dao.update(Resources.INSERT_CL_SQL);
            dao.setInt(1, clsId);
            dao.setInt(2, layout);
            dao.setString(3, sql);
            dao.setBoolean(4, false);
            dao.setString(5, csql);
            flag = dao.executeUpdate();
        } else if (container.getCommand().equals("EDIT")) {
            dao.update(Resources.UPDATE_CL_SQL);
            dao.setString(1, sql);
            dao.setString(2, csql);
            dao.setInt(3, clsId);
            dao.setInt(4, layout);
            flag = dao.executeUpdate();
        }
        if (flag) {
            dao.update(Resources.DELETE_CLFIELDS_SQL);
            dao.setInt(1, clsId);
            dao.setInt(2, layout);
            dao.executeUpdate();
            for (int i = 0; i < cdModel.getRowCount(); i++) {
                Column column = (Column) cdModel.getValueAt(i, 0);
                dao.update(Resources.INSERT_CLFIELDS_SQL);
                dao.setInt(1, clsId);
                dao.setInt(2, layout);
                dao.setString(3, column.getTableName());
                dao.setString(4, column.getColumnName());
                dao.setInt(5, i + 1);
                dao.setObject(6, cdModel.getValueAt(i, 2));
                dao.setObject(7, cdModel.getValueAt(i, 1));
                int tempRow = findRow(column, container.getColumnAllTable());
                String andorString = (String) caModel.getValueAt(tempRow, 0);
                if (andorString.equals("AND")) dao.setObject(8, true); else dao.setObject(8, false);
                dao.setObject(9, caModel.getValueAt(tempRow, 2));
                tempRow = findRow(column, container.getColumnOrderTable());
                if (tempRow == -1) dao.setObject(10, null); else dao.setInt(10, tempRow + 1);
                if (tempRow == -1) dao.setObject(11, null); else dao.setObject(11, coModel.getValueAt(tempRow, 1));
                flag = dao.executeUpdate();
                if (!flag) {
                    dao.rollback();
                    dao.setAutoCommit(true);
                    return false;
                }
            }
            for (int i = 0; i < caModel.getRowCount(); i++) {
                Column column = (Column) caModel.getValueAt(i, 1);
                String filterString = (String) caModel.getValueAt(i, 2);
                if ((filterString != null) && (filterString.trim().length() > 0)) {
                    dao.query(Resources.SELECT_CLFIELDS_BY_CLSID_AND_LAYOUT_AND_TABLENAME_AND_COLUMNNAME);
                    dao.setObject(1, clsId);
                    dao.setObject(2, layout);
                    dao.setObject(3, column.getTableName());
                    dao.setObject(4, column.getColumnName());
                    ResultSet rs = dao.executeQuery();
                    boolean displayed = false;
                    try {
                        if (rs.next()) {
                            displayed = true;
                        }
                        rs.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (!displayed) {
                        dao.update(Resources.INSERT_CLFIELDS_SQL);
                        dao.setInt(1, clsId);
                        dao.setInt(2, layout);
                        dao.setString(3, column.getTableName());
                        dao.setString(4, column.getColumnName());
                        dao.setInt(5, 0);
                        dao.setObject(6, null);
                        dao.setObject(7, null);
                        int tempRow = findRow(column, container.getColumnAllTable());
                        String andorString = (String) caModel.getValueAt(tempRow, 0);
                        if (andorString.equals("AND")) dao.setObject(8, true); else dao.setObject(8, false);
                        dao.setObject(9, caModel.getValueAt(tempRow, 2));
                        dao.setObject(10, null);
                        dao.setObject(11, null);
                        flag = dao.executeUpdate();
                        if (!flag) {
                            dao.rollback();
                            dao.setAutoCommit(true);
                            return false;
                        }
                    }
                }
            }
            if (flag) {
                dao.commit();
                if (container.getCommand().equals("ADD") || container.getCommand().equals("COPY")) {
                    setDefaultName();
                }
                dao.setAutoCommit(true);
                return true;
            } else {
                dao.rollback();
                dao.setAutoCommit(true);
                return false;
            }
        } else {
            dao.rollback();
            dao.setAutoCommit(true);
            return false;
        }
    }

    private void setDefaultName() {
        DAO dao = DAO.getInstance();
        String txtId = "CL." + container.getObject().getClsId() + "." + container.getLayout_C();
        String lang = Resources.getLanguage();
        String long_c = TxtManager.getTxt("CLS." + container.getObject().getClsId()) + " " + TxtManager.getTxt("CL") + " " + container.getLayout_C();
        dao.update(Resources.INSERT_TXT_SQL);
        dao.setString(1, txtId);
        dao.setString(2, lang);
        dao.setString(3, long_c);
        dao.executeUpdate();
    }

    private boolean validate() {
        boolean result = true;
        String sql = getSQL();
        DAO dao = DAO.getInstance();
        dao.update(sql);
        try {
            dao.executeUpdate();
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    private boolean validateCSQL() {
        boolean result = true;
        String sql = getSQL();
        String csql = container.getCSQL();
        if ((sql != null) && (!sql.trim().equals(""))) {
            sql = CSQLUtil.add(sql, csql);
        }
        DAO dao = DAO.getInstance();
        dao.update(sql);
        try {
            dao.executeUpdate();
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    private void itemLeftAndRight(JXTable ordinaryTable, JTableX sourceTable) {
        itemLeftAndRight(ordinaryTable, (JXTable) sourceTable);
    }

    private void itemLeftAndRight(JTableX ordinaryTable, JXTable sourceTable) {
        itemLeftAndRight((JXTable) ordinaryTable, sourceTable);
    }

    private void itemLeftAndRight(JXTable ordinaryTable, JXTable sourceTable) {
        if (sourceTable.getSelectedRow() != -1) {
            int row = sourceTable.getSelectedRow();
            DefaultTableModel cModel = (DefaultTableModel) container.getColumnTable().getModel();
            DefaultTableModel cdModel = (DefaultTableModel) container.getColumnDisplayTable().getModel();
            DefaultTableModel cosModel = (DefaultTableModel) container.getColumnOrderSourceTable().getModel();
            DefaultTableModel coModel = (DefaultTableModel) container.getColumnOrderTable().getModel();
            DefaultTableModel caModel = (DefaultTableModel) container.getColumnAllTable().getModel();
            if ((sourceTable == container.getColumnTable()) && (ordinaryTable == container.getColumnDisplayTable())) {
                Column column = (Column) cModel.getValueAt(row, 0);
                Vector rowData = new Vector();
                rowData.add(column);
                if (column.isCountble()) rowData.add(new Boolean(false)); else rowData.add(null);
                if (column.isSumble()) rowData.add(new Boolean(false)); else rowData.add(null);
                cdModel.addRow(rowData);
                rowData = new Vector();
                rowData.add(column);
                cosModel.addRow(rowData);
                cModel.removeRow(row);
            } else if ((sourceTable == container.getColumnDisplayTable()) && (ordinaryTable == container.getColumnTable())) {
                Column column = (Column) cdModel.getValueAt(row, 0);
                Vector rowData = new Vector();
                rowData.add(column);
                cModel.addRow(rowData);
                int tempRow = findRow(column, container.getColumnOrderSourceTable());
                if (tempRow != -1) cosModel.removeRow(tempRow);
                tempRow = findRow(column, container.getColumnOrderTable());
                if (tempRow != -1) coModel.removeRow(tempRow);
                cdModel.removeRow(row);
            } else if ((sourceTable == container.getColumnOrderSourceTable()) && (ordinaryTable == container.getColumnOrderTable())) {
                Column column = (Column) cosModel.getValueAt(row, 0);
                Vector rowData = new Vector();
                rowData.add(column);
                rowData.add(new Boolean(false));
                coModel.addRow(rowData);
                cosModel.removeRow(row);
            } else if ((sourceTable == container.getColumnOrderTable()) && (ordinaryTable == container.getColumnOrderSourceTable())) {
                Column column = (Column) coModel.getValueAt(row, 0);
                Vector rowData = new Vector();
                rowData.add(column);
                cosModel.addRow(rowData);
                coModel.removeRow(row);
            }
        }
        container.updateDisplayColumnTable();
    }

    private int findRow(Column column, JXTable table) {
        int row = -1;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            Column tempColumn = null;
            if (table.equals(container.getColumnAllTable())) tempColumn = (Column) model.getValueAt(i, 1); else tempColumn = (Column) model.getValueAt(i, 0);
            if (column == tempColumn) {
                row = i;
                break;
            }
        }
        return row;
    }

    private void itemUpAndDown(JXTable table, String command) {
        if (table.getSelectedRow() != -1) {
            int selectedRow = table.getSelectedRow();
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            if (command.equals("UP")) {
                if (selectedRow != 0) {
                    model.moveRow(selectedRow, selectedRow, selectedRow - 1);
                    table.getSelectionModel().setSelectionInterval(selectedRow, selectedRow - 1);
                }
            } else if (command.equals("DOWN")) {
                if (selectedRow != model.getRowCount() - 1) {
                    model.moveRow(selectedRow, selectedRow, selectedRow + 1);
                    table.getSelectionModel().setSelectionInterval(selectedRow, selectedRow + 1);
                }
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Save Layout")) {
            if (validate()) {
                if (validateCSQL()) {
                    saveSettings();
                    container.getContainer().updateReportViewTable(container.getLayout_C());
                    container.getContainer().setEnabled(true);
                    container.getContainer().updateTree();
                    container.dispose();
                } else {
                    DialogManager.showMessageDialog(container, TxtManager.getTxt("INFORMATION.LAYOUTCSQLERROR"));
                }
            } else {
                DialogManager.showMessageDialog(container, TxtManager.getTxt("INFORMATION.LAYOUTFILTERERROR"));
            }
        } else if (e.getActionCommand().equals("Cancel")) {
            container.getContainer().setEnabled(true);
            container.dispose();
        } else if (e.getActionCommand().equals("Display Item up")) itemUpAndDown(container.getColumnDisplayTable(), "UP"); else if (e.getActionCommand().equals("Display Item down")) itemUpAndDown(container.getColumnDisplayTable(), "DOWN"); else if (e.getActionCommand().equals("Order Item up")) itemUpAndDown(container.getColumnOrderTable(), "UP"); else if (e.getActionCommand().equals("Order Item down")) itemUpAndDown(container.getColumnOrderTable(), "DOWN"); else if (e.getActionCommand().equals("Display Item Left")) itemLeftAndRight(container.getColumnDisplayTable(), container.getColumnTable()); else if (e.getActionCommand().equals("Display Item Right")) itemLeftAndRight(container.getColumnTable(), container.getColumnDisplayTable()); else if (e.getActionCommand().equals("Order Item Left")) itemLeftAndRight(container.getColumnOrderTable(), container.getColumnOrderSourceTable()); else if (e.getActionCommand().equals("Order Item Right")) itemLeftAndRight(container.getColumnOrderSourceTable(), container.getColumnOrderTable());
    }
}
