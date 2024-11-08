package realcix20.classes.basic;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Vector;
import realcix20.utils.DAO;
import realcix20.utils.ObjectUtil;
import realcix20.utils.Resources;

public class BaseClass {

    private int clsId;

    private String clsName;

    private String basicSQL;

    private Vector columns;

    private Vector rows;

    private Vector rowSets;

    private Vector primaryKeyTypes;

    private String mainTableName;

    private RowType rowType;

    private boolean slayerMaster;

    private boolean i18n;

    private Vector groupnames;

    private Vector headGroupnames;

    private Vector itemGroupnames;

    private Vector pkColumns;

    private int defCL;

    private DAO dao;

    public BaseClass(int clsId, boolean t) {
        this.clsId = clsId;
        dao = DAO.getInstance();
        dao.query(Resources.SELECT_CLS_SQL);
        dao.setInt(1, clsId);
        ResultSet rs = dao.executeQuery();
        try {
            if (rs.next()) {
                clsName = rs.getString("CLSNAME");
                basicSQL = rs.getString("BASICSQL");
                defCL = rs.getInt("DEFCL");
            }
            rs.close();
        } catch (SQLException sqle) {
        }
        initialMainTableName();
        initialColumns();
        initialI18n();
        initialSlayerMaster();
        initialGroupnames();
        initialPkColumns();
        initialRowType();
        initialRows();
    }

    public BaseClass(int clsId) {
        this.clsId = clsId;
        dao = DAO.getInstance();
        dao.query(Resources.SELECT_CLS_SQL);
        dao.setInt(1, clsId);
        ResultSet rs = dao.executeQuery();
        try {
            if (rs.next()) {
                clsName = rs.getString("CLSNAME");
                basicSQL = rs.getString("BASICSQL");
                defCL = rs.getInt("DEFCL");
            }
            rs.close();
        } catch (SQLException sqle) {
        }
        initialMainTableName();
        initialColumns();
        initialI18n();
        initialSlayerMaster();
        initialGroupnames();
        initialPkColumns();
        initialRowType();
        initialRows();
    }

    public int getDefCL() {
        return defCL;
    }

    public void loadChilds(Row row) {
        getRows().clear();
        row.getRowSet().getRows().clear();
        StringBuffer sb = new StringBuffer(getBasicSQL());
        sb.append(" WHERE ");
        Vector whereValues = new Vector();
        Iterator pkColumnIter = pkColumns.iterator();
        while (pkColumnIter.hasNext()) {
            Column pkColumn = (Column) pkColumnIter.next();
            if (pkColumn.getTableName().equals(getMainTableName())) {
                sb.append(pkColumn.getTableName() + "." + pkColumn.getColumnName() + "=? AND ");
                whereValues.add(ObjectUtil.findNewCell(row, pkColumn.getTableName(), pkColumn.getColumnName()).getColumnValue());
            }
        }
        sb.delete(sb.lastIndexOf("AND"), sb.lastIndexOf("AND") + 3);
        String sql = sb.toString();
        DAO dao = DAO.getInstance();
        dao.query(sql);
        for (int i = 0; i < whereValues.size(); i++) {
            dao.setObject(i + 1, whereValues.get(i));
        }
        ResultSet rs = dao.executeQuery();
        try {
            while (rs.next()) {
                Row childRow = new Row(rs);
                row.getRowSet().getRows().add(childRow);
                getRows().add(childRow);
            }
            rs.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    private void initialPkColumns() {
        pkColumns = new Vector();
        Iterator columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if (column.isPrimaryKey()) getPkColumns().add(column);
        }
    }

    private void initialI18n() {
        dao = DAO.getInstance();
        dao.query(Resources.SELECT_PARENT_TABLE_FROM_CLS_TABLES);
        dao.setInt(1, clsId);
        ResultSet rs = dao.executeQuery();
        try {
            if (rs.next()) {
                i18n = rs.getBoolean("ISI18N");
            } else i18n = false;
        } catch (Exception e) {
            e.printStackTrace();
            i18n = false;
        }
    }

    private void initialSlayerMaster() {
        dao = DAO.getInstance();
        dao.query(Resources.SELECT_CHILD_TABLE_FROM_CLS_TABLES);
        dao.setInt(1, clsId);
        ResultSet rs = dao.executeQuery();
        try {
            if (rs.next()) {
                slayerMaster = true;
            } else {
                slayerMaster = false;
            }
            rs.close();
        } catch (SQLException sqle) {
        }
    }

    private void initialMainTableName() {
        dao = DAO.getInstance();
        dao.query(Resources.SELECT_PARENT_TABLE_FROM_CLS_TABLES);
        dao.setInt(1, clsId);
        ResultSet rs = dao.executeQuery();
        try {
            if (rs.next()) mainTableName = rs.getString("TABLENAME");
            rs.close();
        } catch (SQLException sqle) {
        }
    }

    private void initialPrimaryKeyTypes() {
        primaryKeyTypes = new Vector();
        Iterator columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if ((column.isPrimaryKey()) && (column.getTableName().equals(getMainTableName()))) {
                getPrimaryKeyTypes().add(column.getColumnName());
            }
        }
    }

    private void initialRowSets() {
        rowSets = new Vector();
        Iterator rowIter = rows.iterator();
        while (rowIter.hasNext()) {
            Row row = (Row) rowIter.next();
            boolean findRowSet = false;
            for (int i = 0; i < rowSets.size(); i++) {
                RowSet rowSet = (RowSet) rowSets.get(i);
                Iterator primaryKeyIter = rowSet.getPrimaryKeys().iterator();
                boolean inThisRowSet = true;
                while (primaryKeyIter.hasNext()) {
                    PrimaryKey primaryKey = (PrimaryKey) primaryKeyIter.next();
                    Iterator cellIter = row.getOldCells().iterator();
                    boolean isPrimaryKey = false;
                    while (cellIter.hasNext()) {
                        Cell cell = (Cell) cellIter.next();
                        if (cell.getTableName().equals(mainTableName)) {
                            PrimaryKey tempPrimaryKey = new PrimaryKey(cell.getColumnName(), cell.getColumnValue());
                            if (primaryKey.equals(tempPrimaryKey)) {
                                isPrimaryKey = true;
                                break;
                            }
                        }
                    }
                    if (!isPrimaryKey) {
                        inThisRowSet = false;
                        break;
                    }
                }
                if (inThisRowSet) {
                    rowSet.getRows().add(row);
                    row.setRowSet(rowSet);
                    findRowSet = true;
                    break;
                }
            }
            if (!findRowSet) {
                RowSet rowSet = new RowSet();
                Iterator cellIter = row.getOldCells().iterator();
                while (cellIter.hasNext()) {
                    Cell cell = (Cell) cellIter.next();
                    for (int k = 0; k < primaryKeyTypes.size(); k++) {
                        String columnName = (String) primaryKeyTypes.get(k);
                        if ((cell.getColumnName().equals(columnName)) && (cell.getTableName().equals(mainTableName))) {
                            PrimaryKey primaryKey = new PrimaryKey(cell.getColumnName(), cell.getColumnValue());
                            rowSet.getPrimaryKeys().add(primaryKey);
                            break;
                        }
                    }
                }
                rowSet.getRows().add(row);
                row.setRowSet(rowSet);
                rowSets.add(rowSet);
            }
        }
    }

    private void initialRowType() {
        dao = DAO.getInstance();
        dao.query(basicSQL);
        ResultSet rs = dao.executeQuery();
        setRowType(new RowType(rs));
        try {
            rs.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    private void initialRows() {
        rows = new Vector();
        dao = DAO.getInstance();
        dao.query(basicSQL);
        ResultSet rs = dao.executeQuery();
        try {
            int i = 0;
            while (rs.next()) {
                Row row = new Row(rs);
                getRows().add(row);
                i++;
            }
            rs.close();
        } catch (SQLException sqle) {
        }
    }

    private void initialColumns() {
        columns = new Vector();
        dao = DAO.getInstance();
        dao.query(Resources.SELECT_CLS_TABLES_FIELDS_SQL);
        dao.setInt(1, clsId);
        ResultSet rs = dao.executeQuery();
        try {
            while (rs.next()) {
                String tableName = rs.getString("TABLENAME");
                String columnName = rs.getString("COLUMNNAME");
                Column column = new Column(clsId, tableName, columnName);
                columns.add(column);
            }
            rs.close();
        } catch (SQLException sqle) {
        }
    }

    private void initialGroupnames() {
        groupnames = new Vector();
        int groupName = -1;
        Iterator columnIter = getColumns().iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if (column.getGroupName() != groupName) {
                groupName = column.getGroupName();
                getGroupnames().add(new Integer(column.getGroupName()));
            }
        }
        initialHeadGroupnames();
        initialItemGroupnames();
    }

    private void initialHeadGroupnames() {
        headGroupnames = new Vector();
        int groupName = -1;
        Iterator columnIter = getColumns().iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if ((column.getGroupName() != groupName) && (column.isHeadField())) {
                groupName = column.getGroupName();
                getHeadGroupnames().add(new Integer(column.getGroupName()));
            }
        }
    }

    private void initialItemGroupnames() {
        itemGroupnames = new Vector();
        int groupName = -1;
        Iterator columnIter = getColumns().iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if ((column.getGroupName() != groupName) && (column.isItemField())) {
                groupName = column.getGroupName();
                getItemGroupnames().add(new Integer(column.getGroupName()));
            }
        }
    }

    public boolean classInsert() {
        boolean flag = true;
        dao = DAO.getInstance();
        dao.setAutoCommit(false);
        dao.query(Resources.SELECT_CLS_TABLES_DESC_SQL);
        dao.setInt(1, clsId);
        ResultSet rs = dao.executeQuery();
        try {
            while (rs.next()) {
                String mainTableName = rs.getString("PARENTTABLE");
                boolean isMainTable = (mainTableName == null);
                String tableName = rs.getString("TABLENAME");
                if (mainTableName == null) {
                    mainTableName = tableName;
                }
                Vector tableColumns = new Vector();
                Iterator columnsIter = columns.iterator();
                while (columnsIter.hasNext()) {
                    Column column = (Column) columnsIter.next();
                    if (column.getTableName().equals(tableName)) {
                        tableColumns.add(column);
                    }
                }
                int columnsCount = tableColumns.size();
                StringBuffer sb = new StringBuffer("INSERT INTO ");
                sb.append(tableName + "(");
                for (int i = 0; i < columnsCount - 1; i++) {
                    Column column = (Column) tableColumns.get(i);
                    sb.append(column.getColumnName() + ",");
                }
                Column column = (Column) tableColumns.get(columnsCount - 1);
                sb.append(column.getColumnName() + ") VALUES(");
                for (int i = 0; i < columnsCount - 1; i++) {
                    sb.append("?,");
                }
                sb.append("?)");
                String sql = sb.toString();
                dao.update(sql);
                int rowsCount = rows.size();
                for (int i = 0; i < rowsCount; i++) {
                    Row row = (Row) rows.get(i);
                    if (row.isAdd()) {
                        for (int j = 1; j <= tableColumns.size(); j++) {
                            column = (Column) tableColumns.get(j - 1);
                            Vector newCells = row.getNewCells();
                            int cellsCount = newCells.size();
                            for (int k = 0; k <= cellsCount; k++) {
                                Cell cell = (Cell) newCells.get(k);
                                if ((cell.getColumnName().equals(column.getColumnName())) && (cell.getTableName().equals(column.getTableName()))) {
                                    dao.setObject(j, cell.getColumnValue());
                                    break;
                                }
                            }
                        }
                        if (!dao.executeUpdate()) {
                            flag = false;
                            break;
                        }
                    }
                }
                if (flag) {
                    dao.commit();
                } else {
                    dao.rollback();
                    break;
                }
            }
            rs.close();
            dao.setAutoCommit(true);
        } catch (SQLException sqle) {
        }
        return flag;
    }

    public boolean classInsert_WholeObject(Row row, boolean test) {
        boolean result = true;
        String mainTableName = getMainTableName();
        StringBuffer sb = new StringBuffer("INSERT INTO ");
        sb.append(mainTableName + "(");
        Iterator columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if (column.getTableName().equals(mainTableName)) sb.append(column.getColumnName() + ",");
        }
        sb.delete(sb.lastIndexOf(","), sb.lastIndexOf(",") + 1);
        sb.append(") VALUES(");
        columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if (column.getTableName().equals(mainTableName)) sb.append(("?,"));
        }
        sb.delete(sb.lastIndexOf(","), sb.lastIndexOf(",") + 1);
        sb.append(")");
        String sql = sb.toString();
        DAO dao = DAO.getInstance();
        dao.update(sql);
        dao.setAutoCommit(false);
        int currentParameter = 1;
        columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if (column.getTableName().equals(mainTableName)) {
                Object value = getNewCellValue(column, row);
                dao.setObject(currentParameter, value);
                currentParameter++;
            }
        }
        result = dao.executeUpdate();
        if (test) {
            dao.rollback();
        } else {
            if (!result) {
                dao.rollback();
            } else {
                dao.commit();
                if (isSlayerMaster()) {
                    Iterator rowIter = row.getRowSet().getRows().iterator();
                    while (rowIter.hasNext()) {
                        Row childRow = (Row) rowIter.next();
                        if (childRow != row) {
                            if (!childRow.isDelete()) classInsert_Child(childRow, false);
                        }
                    }
                }
            }
        }
        dao.setAutoCommit(true);
        return result;
    }

    public boolean classInsert_Child(Row row, boolean test) {
        boolean result = true;
        String tableName = getChildTableName();
        StringBuffer sb = new StringBuffer("INSERT INTO ");
        sb.append(tableName + "(");
        Iterator columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if (column.getTableName().equals(tableName)) sb.append(column.getColumnName() + ",");
        }
        sb.delete(sb.lastIndexOf(","), sb.lastIndexOf(",") + 1);
        sb.append(") VALUES(");
        columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if (column.getTableName().equals(tableName)) sb.append(("?,"));
        }
        sb.delete(sb.lastIndexOf(","), sb.lastIndexOf(",") + 1);
        sb.append(")");
        String sql = sb.toString();
        DAO dao = DAO.getInstance();
        dao.update(sql);
        dao.setAutoCommit(false);
        int currentParameter = 1;
        columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if (column.getTableName().equals(tableName)) {
                Object value = getNewCellValue(column, row);
                dao.setObject(currentParameter, value);
                currentParameter++;
            }
        }
        result = dao.executeUpdate();
        if (test) dao.rollback(); else {
            if (!result) dao.rollback(); else dao.commit();
        }
        dao.setAutoCommit(true);
        return result;
    }

    public boolean classDelete_WholeObject(Row row) {
        dao.setAutoCommit(false);
        boolean flag = true;
        String mainTableName = getMainTableName();
        String childTableName = getChildTableName();
        if (childTableName != null) {
            StringBuffer sb = new StringBuffer("DELETE FROM ");
            sb.append(childTableName + " WHERE ");
            Vector whereColumns = new Vector();
            Iterator pkColumnIter = pkColumns.iterator();
            while (pkColumnIter.hasNext()) {
                Column pkColumn = (Column) pkColumnIter.next();
                if (pkColumn.getTableName().equals(mainTableName)) {
                    whereColumns.add(pkColumn);
                    sb.append(pkColumn.getColumnName() + "=? AND ");
                }
            }
            sb.delete(sb.lastIndexOf("AND"), sb.lastIndexOf("AND") + 3);
            String sql = sb.toString();
            DAO dao = DAO.getInstance();
            dao.update(sql);
            for (int i = 0; i < whereColumns.size(); i++) {
                Column column = (Column) columns.get(i);
                Cell cell = ObjectUtil.findNewCell(row, column.getTableName(), column.getColumnName());
                dao.setObject(i + 1, cell.getColumnValue());
            }
            dao.executeUpdate();
        }
        StringBuffer sb = new StringBuffer("DELETE FROM ");
        sb.append(mainTableName + " WHERE ");
        Vector whereColumns = new Vector();
        Iterator pkColumnIter = pkColumns.iterator();
        while (pkColumnIter.hasNext()) {
            Column pkColumn = (Column) pkColumnIter.next();
            if (pkColumn.getTableName().equals(mainTableName)) {
                whereColumns.add(pkColumn);
                sb.append(pkColumn.getColumnName() + "=? AND ");
            }
        }
        sb.delete(sb.lastIndexOf("AND"), sb.lastIndexOf("AND") + 3);
        String sql = sb.toString();
        dao.update(sql);
        for (int i = 0; i < whereColumns.size(); i++) {
            Column column = (Column) columns.get(i);
            Cell cell = ObjectUtil.findNewCell(row, column.getTableName(), column.getColumnName());
            dao.setObject(i + 1, cell.getColumnValue());
        }
        dao.executeUpdate();
        dao.commit();
        dao.setAutoCommit(true);
        return true;
    }

    public boolean classDelete_Child(Row row) {
        boolean flag = true;
        dao.setAutoCommit(false);
        String childTableName = getChildTableName();
        if (childTableName != null) {
            StringBuffer sb = new StringBuffer("DELETE FROM ");
            sb.append(childTableName + " WHERE ");
            Vector whereColumns = new Vector();
            Iterator pkColumnIter = pkColumns.iterator();
            while (pkColumnIter.hasNext()) {
                Column pkColumn = (Column) pkColumnIter.next();
                if (pkColumn.getTableName().equals(childTableName)) {
                    whereColumns.add(pkColumn);
                    sb.append(pkColumn.getColumnName() + "=? AND ");
                }
            }
            sb.delete(sb.lastIndexOf("AND"), sb.lastIndexOf("AND") + 3);
            String sql = sb.toString();
            DAO dao = DAO.getInstance();
            dao.update(sql);
            for (int i = 0; i < whereColumns.size(); i++) {
                Column column = (Column) whereColumns.get(i);
                Cell cell = ObjectUtil.findNewCell(row, column.getTableName(), column.getColumnName());
                dao.setObject(i + 1, cell.getColumnValue());
            }
            dao.executeUpdate();
            dao.commit();
            dao.setAutoCommit(true);
        }
        return true;
    }

    public boolean classUpdate_WholeObject(Row row, boolean test) {
        boolean result = true;
        String mainTableName = getMainTableName();
        Vector needModifyColumns = new Vector();
        Vector whereColumns = new Vector();
        StringBuffer sb = new StringBuffer("UPDATE ");
        sb.append(mainTableName + " SET ");
        Iterator columnIterator = getColumns().iterator();
        while (columnIterator.hasNext()) {
            Column column = (Column) columnIterator.next();
            if (column.getTableName().equals(mainTableName)) {
                needModifyColumns.add(column);
                sb.append(column.getColumnName() + "=?,");
            }
        }
        sb.delete(sb.lastIndexOf(","), sb.lastIndexOf(",") + 1);
        sb.append(" WHERE ");
        columnIterator = getColumns().iterator();
        while (columnIterator.hasNext()) {
            Column column = (Column) columnIterator.next();
            if ((column.isPrimaryKey()) && (column.getTableName().equals(mainTableName))) {
                whereColumns.add(column);
                sb.append(column.getColumnName() + "=? AND ");
            }
        }
        sb.delete(sb.lastIndexOf("AND"), sb.lastIndexOf("AND") + 3);
        String sql = sb.toString();
        DAO dao = DAO.getInstance();
        dao.update(sql);
        dao.setAutoCommit(false);
        int currentParameter = 1;
        columnIterator = needModifyColumns.iterator();
        while (columnIterator.hasNext()) {
            Column column = (Column) columnIterator.next();
            Object value = getNewCellValue(column, row);
            dao.setObject(currentParameter, value);
            currentParameter++;
        }
        columnIterator = whereColumns.iterator();
        while (columnIterator.hasNext()) {
            Column column = (Column) columnIterator.next();
            Object value = getOldCellValue(column, row);
            dao.setObject(currentParameter, value);
            currentParameter++;
        }
        result = dao.executeUpdate();
        if (test) {
            dao.rollback();
        } else {
            if (!result) {
                dao.rollback();
            } else {
                dao.commit();
                Iterator rowIter = row.getRowSet().getRows().iterator();
                while (rowIter.hasNext()) {
                    Row childRow = (Row) rowIter.next();
                    if (childRow.isDelete()) classDelete_Child(childRow); else if (childRow.isAdd()) classInsert_Child(childRow, false); else if (childRow.isModify()) classUpdate_Child(childRow, false);
                }
            }
        }
        dao.setAutoCommit(true);
        return result;
    }

    public boolean classUpdate_Child(Row row, boolean test) {
        boolean result = true;
        String childTableName = getChildTableName();
        Vector needModifyColumns = new Vector();
        Vector whereColumns = new Vector();
        StringBuffer sb = new StringBuffer("UPDATE ");
        sb.append(childTableName + " SET ");
        Iterator columnIterator = getColumns().iterator();
        while (columnIterator.hasNext()) {
            Column column = (Column) columnIterator.next();
            if (column.getTableName().equals(childTableName)) {
                needModifyColumns.add(column);
                sb.append(column.getColumnName() + "=?,");
            }
        }
        sb.delete(sb.lastIndexOf(","), sb.lastIndexOf(",") + 1);
        sb.append(" WHERE ");
        columnIterator = getColumns().iterator();
        while (columnIterator.hasNext()) {
            Column column = (Column) columnIterator.next();
            if ((column.isPrimaryKey()) && (column.getTableName().equals(childTableName))) {
                whereColumns.add(column);
                sb.append(column.getColumnName() + "=? AND ");
            }
        }
        sb.delete(sb.lastIndexOf("AND"), sb.lastIndexOf("AND") + 3);
        String sql = sb.toString();
        DAO dao = DAO.getInstance();
        dao.update(sql);
        dao.setAutoCommit(false);
        int currentParameter = 1;
        columnIterator = needModifyColumns.iterator();
        while (columnIterator.hasNext()) {
            Column column = (Column) columnIterator.next();
            Object value = getNewCellValue(column, row);
            dao.setObject(currentParameter, value);
            currentParameter++;
        }
        columnIterator = whereColumns.iterator();
        while (columnIterator.hasNext()) {
            Column column = (Column) columnIterator.next();
            Object value = getOldCellValue(column, row);
            dao.setObject(currentParameter, value);
            currentParameter++;
        }
        result = dao.executeUpdate();
        if (test) dao.rollback(); else {
            if (!result) dao.rollback(); else dao.commit();
        }
        dao.setAutoCommit(true);
        return result;
    }

    public String getChildTableName() {
        String tableName = null;
        dao = DAO.getInstance();
        dao.query(Resources.SELECT_CHILD_TABLE_FROM_CLS_TABLES);
        dao.setInt(1, clsId);
        ResultSet rs = dao.executeQuery();
        try {
            if (rs.next()) tableName = rs.getString("TABLENAME");
            rs.close();
        } catch (Exception e) {
        }
        return tableName;
    }

    public boolean classDelete() {
        boolean flag = true;
        dao = DAO.getInstance();
        dao.setAutoCommit(false);
        dao.query(Resources.SELECT_CLS_TABLES_DESC_SQL);
        dao.setInt(1, clsId);
        ResultSet rs = dao.executeQuery();
        try {
            while (rs.next()) {
                String tableName = rs.getString("TABLENAME");
                Vector tableColumns = new Vector();
                Iterator columnsIter = columns.iterator();
                while (columnsIter.hasNext()) {
                    Column column = (Column) columnsIter.next();
                    if (column.getTableName().equals(tableName)) {
                        tableColumns.add(column);
                    }
                }
                Vector whereColumns = new Vector();
                int columnsCount = tableColumns.size();
                StringBuffer sb = new StringBuffer("DELETE FROM ");
                sb.append(tableName + " WHERE ");
                for (int i = 0; i < columnsCount; i++) {
                    Column column = (Column) tableColumns.get(i);
                    if (column.isPrimaryKey()) {
                        whereColumns.add(column);
                        sb.append(column.getColumnName() + "=? AND ");
                    }
                }
                sb.delete(sb.lastIndexOf("AND"), sb.lastIndexOf("AND") + 3);
                String sql = sb.toString();
                dao.update(sql);
                int rowsCount = rows.size();
                for (int i = 0; i < rowsCount; i++) {
                    Row row = (Row) rows.get(i);
                    if (row.isDelete()) {
                        for (int j = 1; j <= whereColumns.size(); j++) {
                            Column column = (Column) whereColumns.get(j - 1);
                            Vector oldCells = row.getOldCells();
                            int cellsCount = oldCells.size();
                            for (int k = 0; k <= cellsCount; k++) {
                                Cell cell = (Cell) oldCells.get(k);
                                if ((cell.getColumnName().equals(column.getColumnName())) && (cell.getTableName().equals(column.getTableName()))) {
                                    dao.setObject(j, cell.getColumnValue());
                                    break;
                                }
                            }
                        }
                        if (!dao.executeUpdate()) {
                            flag = false;
                            break;
                        } else {
                            this.rows.remove(row);
                        }
                    }
                }
                if (flag) {
                    dao.commit();
                } else {
                    dao.rollback();
                    break;
                }
            }
            rs.close();
            dao.setAutoCommit(true);
        } catch (SQLException sqle) {
        }
        return flag;
    }

    public boolean classUpdate() {
        boolean flag = true;
        dao = DAO.getInstance();
        dao.setAutoCommit(false);
        dao.query(Resources.SELECT_CLS_TABLES_SQL);
        dao.setInt(1, clsId);
        ResultSet rs = dao.executeQuery();
        try {
            while (rs.next()) {
                String mainTableName = rs.getString("PARENTTABLE");
                boolean isMainTable = (mainTableName == null);
                String tableName = rs.getString("TABLENAME");
                if (mainTableName == null) {
                    mainTableName = tableName;
                }
                Vector tableColumns = new Vector();
                Iterator columnsIter = columns.iterator();
                while (columnsIter.hasNext()) {
                    Column column = (Column) columnsIter.next();
                    if (column.getTableName().equals(tableName)) {
                        tableColumns.add(column);
                    }
                }
                Vector needModifyColumns = new Vector();
                Vector whereColumns = new Vector();
                int columnsCount = tableColumns.size();
                StringBuffer sb = new StringBuffer("UPDATE ");
                sb.append(tableName + " SET ");
                for (int i = 0; i < columnsCount - 1; i++) {
                    Column column = (Column) tableColumns.get(i);
                    needModifyColumns.add(column);
                    sb.append(column.getColumnName() + "=?,");
                }
                Column column = (Column) tableColumns.get(columnsCount - 1);
                needModifyColumns.add(column);
                sb.append(column.getColumnName() + "=? WHERE ");
                for (int i = 0; i < columnsCount; i++) {
                    column = (Column) tableColumns.get(i);
                    if (column.isPrimaryKey()) {
                        whereColumns.add(column);
                        sb.append(column.getColumnName() + "=? AND ");
                    }
                }
                sb.delete(sb.lastIndexOf("AND"), sb.lastIndexOf("AND") + 3);
                String sql = sb.toString();
                dao.update(sql);
                int rowsCount = rows.size();
                for (int i = 0; i < rowsCount; i++) {
                    Row row = (Row) rows.get(i);
                    if (row.isModify()) {
                        if (isMainTable) {
                            for (int j = 1; j <= needModifyColumns.size(); j++) {
                                column = (Column) needModifyColumns.get(j - 1);
                                Vector newCells = row.getNewCells();
                                int cellsCount = newCells.size();
                                for (int k = 0; k <= cellsCount; k++) {
                                    Cell cell = (Cell) newCells.get(k);
                                    if ((cell.getColumnName().equals(column.getColumnName())) && (cell.getTableName().equals(column.getTableName()))) {
                                        dao.setObject(j, cell.getColumnValue());
                                        break;
                                    }
                                }
                            }
                        } else {
                            for (int j = 1; j <= needModifyColumns.size(); j++) {
                                column = (Column) needModifyColumns.get(j - 1);
                                Vector newCells = row.getNewCells();
                                int cellsCount = newCells.size();
                                for (int k = 0; k <= cellsCount; k++) {
                                    Cell cell = (Cell) newCells.get(k);
                                    if ((cell.getColumnName().equals(column.getColumnName())) && (cell.getTableName().equals(column.getTableName()))) {
                                        if (!cell.getTableName().equals(mainTableName)) {
                                            Iterator newCellIter = newCells.iterator();
                                            while (newCellIter.hasNext()) {
                                                Cell tempCell = (Cell) newCellIter.next();
                                                if ((tempCell.getColumnName().equals(cell.getColumnName())) && (tempCell.getTableName().equals(mainTableName))) {
                                                    cell.setColumnValue(tempCell.getColumnValue());
                                                }
                                            }
                                        }
                                        dao.setObject(j, cell.getColumnValue());
                                        break;
                                    }
                                }
                            }
                        }
                        for (int j = 1; j <= whereColumns.size(); j++) {
                            column = (Column) whereColumns.get(j - 1);
                            Vector oldCells = row.getOldCells();
                            int cellsCount = oldCells.size();
                            for (int k = 0; k <= cellsCount; k++) {
                                Cell cell = (Cell) oldCells.get(k);
                                if (cell.getColumnName().equals(column.getColumnName())) {
                                    dao.setObject(j + needModifyColumns.size(), cell.getColumnValue());
                                    break;
                                }
                            }
                        }
                        if (!dao.executeUpdate()) {
                            flag = false;
                            break;
                        }
                    }
                }
                if (flag) {
                    dao.commit();
                } else {
                    dao.rollback();
                    break;
                }
            }
            rs.close();
            dao.setAutoCommit(true);
        } catch (SQLException sqle) {
        }
        return flag;
    }

    public Object getNewCellValue(Column column, Row row) {
        Object value = null;
        Iterator cellIter = row.getNewCells().iterator();
        while (cellIter.hasNext()) {
            Cell cell = (Cell) cellIter.next();
            if ((cell.getColumnName().equals(column.getColumnName())) && (cell.getTableName().equals(column.getTableName()))) {
                value = cell.getColumnValue();
                break;
            }
        }
        return value;
    }

    public Object getOldCellValue(Column column, Row row) {
        Object value = null;
        Iterator cellIter = row.getOldCells().iterator();
        while (cellIter.hasNext()) {
            Cell cell = (Cell) cellIter.next();
            if ((cell.getColumnName().equals(column.getColumnName())) && (cell.getTableName().equals(column.getTableName()))) {
                value = cell.getColumnValue();
                break;
            }
        }
        return value;
    }

    public int getIndexFieldCount() {
        int count = 0;
        Iterator columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if (column.isIndexField()) count++;
        }
        return count;
    }

    public int getGroupCount() {
        int groupName = -1;
        int groupCount = 0;
        Iterator columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if (column.getGroupName() != groupName) {
                groupName = column.getGroupName();
                groupCount++;
            }
        }
        return groupCount;
    }

    public int getHeadGroupCount() {
        int groupName = -1;
        int groupCount = 0;
        Iterator columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if ((column.getGroupName() != groupName) && (column.isHeadField())) {
                groupName = column.getGroupName();
                groupCount++;
            }
        }
        return groupCount;
    }

    public int getItemGroupCount() {
        int groupName = -1;
        int groupCount = 0;
        Iterator columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if ((column.getGroupName() != groupName) && (column.isItemField())) {
                groupName = column.getGroupName();
                groupCount++;
            }
        }
        return groupCount;
    }

    public int getGroupIndexFieldCount(int groupName) {
        int count = 0;
        Iterator columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if (column.getGroupName() == groupName) {
                if (column.isIndexField()) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getGroupHeadFieldCount(int groupName) {
        int count = 0;
        Iterator columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if (column.getGroupName() == groupName) {
                if (column.isHeadField()) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getGroupItemTableFieldCount(int groupName) {
        int count = 0;
        Iterator columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if (column.getGroupName() == groupName) {
                if (column.isItemTableField()) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getGroupItemFieldCount(int groupName) {
        int count = 0;
        Iterator columnIter = columns.iterator();
        while (columnIter.hasNext()) {
            Column column = (Column) columnIter.next();
            if (column.getGroupName() == groupName) {
                if (column.isItemField()) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getClsId() {
        return clsId;
    }

    public String getClsName() {
        return clsName;
    }

    public String getBasicSQL() {
        return basicSQL;
    }

    public Vector getColumns() {
        return columns;
    }

    public Vector getRows() {
        return rows;
    }

    public RowType getRowType() {
        return rowType;
    }

    public void setRowType(RowType rowType) {
        this.rowType = rowType;
    }

    public Vector getRowSets() {
        return rowSets;
    }

    public Vector getPrimaryKeyTypes() {
        return primaryKeyTypes;
    }

    public String getMainTableName() {
        return mainTableName;
    }

    public boolean isSlayerMaster() {
        return slayerMaster;
    }

    public Vector getGroupnames() {
        return groupnames;
    }

    public Vector getHeadGroupnames() {
        return headGroupnames;
    }

    public Vector getItemGroupnames() {
        return itemGroupnames;
    }

    public Vector getPkColumns() {
        return pkColumns;
    }

    public boolean isI18n() {
        return i18n;
    }
}
