package com.ibm.csdl.baseframe.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.ibm.csdl.ecm.ta.critsitEX.db.DatabaseConn;

public abstract class BaseDao {

    protected Connection dbConnection = null;

    private List mappingList = new ArrayList();

    protected String insertSql;

    protected String deleteSql;

    protected String selectSql;

    protected String updateSql;

    protected String searchSql;

    public BaseDao() {
        insertSql = null;
        deleteSql = null;
        selectSql = null;
        updateSql = null;
        searchSql = null;
        this.setSql();
    }

    public BaseDao(Connection conn) {
        insertSql = null;
        deleteSql = null;
        selectSql = null;
        updateSql = null;
        searchSql = null;
        this.dbConnection = conn;
        this.setSql();
    }

    public void deleteObject(String id) throws SQLException {
        boolean selfConnection = true;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            if (dbConnection == null) {
                DatabaseConn dbConn = new DatabaseConn();
                conn = dbConn.getConnection();
                conn.setAutoCommit(false);
            } else {
                conn = dbConnection;
                selfConnection = false;
            }
            stmt = conn.prepareStatement(this.deleteSql);
            stmt.setString(1, id);
            stmt.executeUpdate();
            if (selfConnection) conn.commit();
        } catch (Exception e) {
            if (selfConnection && conn != null) conn.rollback();
            throw new SQLException(e.getMessage());
        } finally {
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
            if (selfConnection && conn != null) {
                conn.close();
                conn = null;
            }
        }
    }

    public void insertObject(Object obj) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            if (dbConnection == null) {
                DatabaseConn dbConn = new DatabaseConn();
                conn = dbConn.getConnection();
                conn.setAutoCommit(false);
            } else {
                conn = dbConnection;
            }
            stmt = conn.prepareStatement(this.insertSql);
            this.add(stmt, obj);
            stmt.execute();
            if (dbConnection == null) conn.commit();
        } catch (Exception e) {
            if (dbConnection == null && conn != null) conn.rollback();
            e.printStackTrace();
            throw new SQLException(e.getMessage());
        } finally {
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
            if (dbConnection == null && conn != null) {
                conn.close();
                conn = null;
            }
        }
    }

    public void saveObject(Object obj) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            if (dbConnection == null) {
                DatabaseConn dbConn = new DatabaseConn();
                conn = dbConn.getConnection();
                conn.setAutoCommit(false);
            } else {
                conn = dbConnection;
            }
            System.out.println("updateSql" + updateSql);
            stmt = conn.prepareStatement(this.updateSql);
            this.save(stmt, obj);
            stmt.execute();
            if (dbConnection == null) conn.commit();
        } catch (Exception e) {
            if (dbConnection == null && conn != null) conn.rollback();
            e.printStackTrace();
            throw new SQLException(e.getMessage());
        } finally {
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
            if (dbConnection == null && conn != null) {
                conn.close();
                conn = null;
            }
        }
    }

    public Object getObject(String id) throws SQLException {
        if (id == null) {
            return null;
        }
        Object obj = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (dbConnection == null) {
                DatabaseConn dbConn = new DatabaseConn();
                conn = dbConn.getConnection();
            } else {
                conn = dbConnection;
            }
            stmt = conn.prepareStatement(this.selectSql);
            System.out.println("*****************" + selectSql);
            stmt.setString(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) obj = get(rs);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException(e.getMessage());
        } finally {
            if (rs != null) {
                rs.close();
                rs = null;
            }
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
            if (dbConnection == null && conn != null) {
                conn.close();
                conn = null;
            }
        }
        return obj;
    }

    private MappingItem getMappingItem(String attr) {
        MappingItem result = null;
        for (int i = 0; i < this.mappingList.size(); i++) {
            MappingItem mappingItem = (MappingItem) mappingList.get(i);
            if (mappingItem.getAttributeName().compareToIgnoreCase(attr) == 0) {
                result = mappingItem;
                break;
            }
        }
        return result;
    }

    public List searchObject(SearchObject so) throws SQLException {
        List result = new ArrayList();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (dbConnection == null) {
                DatabaseConn dbConn = new DatabaseConn();
                conn = dbConn.getConnection();
            } else {
                conn = dbConnection;
            }
            String sql = this.searchSql;
            List condList = so.getConditionList();
            if (condList != null && condList.size() > 0) sql = sql + " and (";
            for (int j = 0; condList != null && j < condList.size(); j++) {
                List itemList = ((Condition) condList.get(j)).getConditionItemList();
                if (condList.size() != 1) sql = sql + " (";
                sql = sql + " 1=1 ";
                System.out.println(itemList.size() + "itemList");
                for (int k = 0; k < itemList.size(); k++) {
                    ConditionItem item = (ConditionItem) itemList.get(k);
                    String name = item.getName();
                    String opt = item.getOpt();
                    String val = item.getVal();
                    if (opt.compareToIgnoreCase("LIKE") == 0) val = "%" + val + "%";
                    MappingItem mappingItem = this.getMappingItem(name);
                    if (mappingItem != null) {
                        if (mappingItem.getType() == MappingItem.INT) {
                            sql = sql + " and " + mappingItem.getFieldName() + " " + opt + " " + val + " ";
                        } else if (mappingItem.getType() == MappingItem.DATE) {
                            sql = sql + " and char(date(" + mappingItem.getFieldName() + ")) " + opt + " '" + val + "' ";
                        } else {
                            sql = sql + " and " + mappingItem.getFieldName() + " " + opt + " '" + val + "' ";
                        }
                    }
                }
                if (condList.size() != 1 && j == condList.size() - 1) sql = sql + ") "; else if (condList.size() != 1 && j < condList.size() - 1) sql = sql + ") or ";
            }
            if (condList != null && condList.size() > 0) sql = sql + " ) ";
            List orderByList = so.getOrderByList();
            Map orderFormMap = so.getOrderFormMap();
            if (orderByList != null && orderByList.size() > 0) {
                System.out.println("====================1=====================");
                sql = sql + " ORDER BY ";
                Iterator it = orderByList.iterator();
                String name = null;
                while (it.hasNext()) {
                    String o = (String) it.next();
                    name = o;
                    System.out.println("====================2=====================");
                    MappingItem mappingItem = this.getMappingItem(name);
                    if (!it.hasNext()) {
                        sql = sql + mappingItem.getFieldName() + orderFormMap.get(name);
                    } else {
                        sql = sql + mappingItem.getFieldName() + orderFormMap.get(name) + ", ";
                    }
                }
            }
            System.out.println("Search sql:" + sql);
            stmt = conn.prepareStatement(sql);
            System.out.println(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                Object obj = search(rs);
                result.add(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException(e.getMessage());
        } finally {
            if (rs != null) {
                rs.close();
                rs = null;
            }
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
            if (dbConnection == null && conn != null) {
                conn.close();
                conn = null;
            }
        }
        return result;
    }

    /**
	 * All 'LIKE' operation will compare value in lower case
	 * @param so
	 * @return
	 * @throws SQLException
	 */
    public List searchObject2(SearchObject so) throws SQLException {
        List result = new ArrayList();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (dbConnection == null) {
                DatabaseConn dbConn = new DatabaseConn();
                conn = dbConn.getConnection();
            } else {
                conn = dbConnection;
            }
            String sql = this.searchSql;
            List condList = so.getConditionList();
            if (condList != null && condList.size() > 0) sql = sql + " and (";
            for (int j = 0; condList != null && j < condList.size(); j++) {
                List itemList = ((Condition) condList.get(j)).getConditionItemList();
                if (condList.size() != 1) sql = sql + " (";
                sql = sql + " 1=1 ";
                System.out.println(itemList.size() + "itemList");
                for (int k = 0; k < itemList.size(); k++) {
                    ConditionItem item = (ConditionItem) itemList.get(k);
                    String name = item.getName();
                    String opt = item.getOpt();
                    String val = item.getVal();
                    if (opt.compareToIgnoreCase("LIKE") == 0) val = "%" + val + "%";
                    MappingItem mappingItem = this.getMappingItem(name);
                    if (mappingItem != null) {
                        if (opt.compareToIgnoreCase("LIKE") == 0 && mappingItem.getType() == MappingItem.STRING) {
                            sql = sql + " and lower(" + mappingItem.getFieldName() + ") " + opt + " '" + val.toLowerCase() + "' ";
                        } else {
                            if (mappingItem.getType() == MappingItem.INT) {
                                sql = sql + " and " + mappingItem.getFieldName() + " " + opt + " " + val + " ";
                            } else if (mappingItem.getType() == MappingItem.DATE) {
                                sql = sql + " and char(" + mappingItem.getFieldName() + ") " + opt + " '" + val + "' ";
                            } else {
                                sql = sql + " and " + mappingItem.getFieldName() + " " + opt + " '" + val + "' ";
                            }
                        }
                    }
                }
                if (condList.size() != 1 && j == condList.size() - 1) sql = sql + ") "; else if (condList.size() != 1 && j < condList.size() - 1) sql = sql + ") or ";
            }
            if (condList != null && condList.size() > 0) sql = sql + " ) ";
            List orderByList = so.getOrderByList();
            Map orderFormMap = so.getOrderFormMap();
            if (orderByList != null && orderByList.size() > 0) {
                System.out.println("====================1=====================");
                sql = sql + " ORDER BY ";
                Iterator it = orderByList.iterator();
                String name = null;
                while (it.hasNext()) {
                    String o = (String) it.next();
                    name = o;
                    System.out.println("====================2=====================");
                    MappingItem mappingItem = this.getMappingItem(name);
                    if (!it.hasNext()) {
                        sql = sql + mappingItem.getFieldName() + orderFormMap.get(name);
                    } else {
                        sql = sql + mappingItem.getFieldName() + orderFormMap.get(name) + ", ";
                    }
                }
            }
            System.out.println("Search sql:" + sql);
            stmt = conn.prepareStatement(sql);
            System.out.println(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                Object obj = search(rs);
                result.add(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException(e.getMessage());
        } finally {
            if (rs != null) {
                rs.close();
                rs = null;
            }
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
            if (dbConnection == null && conn != null) {
                conn.close();
                conn = null;
            }
        }
        return result;
    }

    protected void addMappingItem(MappingItem mappingItem) {
        this.mappingList.add(mappingItem);
    }

    protected abstract void setSql();

    protected abstract void add(PreparedStatement stmt, Object obj) throws SQLException;

    protected abstract void save(PreparedStatement stmt, Object obj) throws SQLException;

    protected abstract Object get(ResultSet rs) throws SQLException;

    protected abstract Object search(ResultSet rs) throws SQLException;
}
