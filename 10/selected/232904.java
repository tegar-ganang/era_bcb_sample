package com.sunstar.sos.dao;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.sunstar.sos.connect.ConnectUtil;
import com.sunstar.sos.dao.helper.SqlUtil;
import com.sunstar.sos.parser.PojoParser;
import com.sunstar.sos.pojo.meta.SubTableMeta;
import com.sunstar.sos.util.SequencesUtil;

/**
 * ��Dao�ӿ�ʵ���࣬�����װ�˶���ݵĴ󲿷ֵĲ������?�����Ҫ�������
 * ��չ������Ҫ����ģ�鼶��������н��У���Ҫ�ڱ���������չ����
 * @author Administrator
 *
 */
public class BaseDao {

    private Class<?> cls;

    private List<String> columnMeta;

    private List<String> columnType;

    private String result;

    private long id;

    private long[] ids;

    private List<Object> orgList;

    private List<Object> subList;

    /**
	 * ȡ��insert����ʱ����ɵ���������ֵ
	 * @return
	 */
    public long getId() {
        return id;
    }

    /**
	 * ���캯��
	 */
    public BaseDao() {
    }

    public BaseDao(Class<?> cls) {
        this.cls = cls;
    }

    /**
	 * ���sql��䣬ȡ��ĳ����¼��ĳ���ֶε�ֵ
	 * @param sql
	 * @return
	 */
    public String getStringValue(String sql) {
        String value = null;
        this.result = null;
        Connection conn = null;
        try {
            conn = ConnectUtil.getConnect();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                value = rs.getString(1);
                break;
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            this.result = e.getMessage();
        } finally {
            ConnectUtil.closeConn(conn);
        }
        return value;
    }

    /**
	 * ���sql��䣬ȡ��ĳ����¼��ĳ���ֶε�ֵ
	 * @param sql
	 * @return
	 */
    public long getLongValue(String sql) {
        long count = 0;
        this.result = null;
        Connection conn = null;
        try {
            conn = ConnectUtil.getConnect();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                count = rs.getLong(1);
                break;
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            this.result = e.getMessage();
        } finally {
            ConnectUtil.closeConn(conn);
        }
        return count;
    }

    /**
	 * ���ȷ�����ֶ�ֵ�����ж�ĳ����¼�Ƿ����
	 * @param colName
	 * @param value
	 * @return
	 */
    public boolean exist(String colName, String value) {
        boolean bool = false;
        this.result = null;
        Connection conn = null;
        try {
            PojoParser parser = PojoParser.getInstances();
            conn = ConnectUtil.getConnect();
            PreparedStatement ps = conn.prepareStatement("select count(*) from " + parser.getTableName(cls) + " where " + colName + "=?");
            if (parser.getColType(cls, colName).equals("long")) ps.setLong(1, Long.valueOf(value).longValue()); else ps.setString(1, value);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getLong(1) != 0) bool = true;
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            this.result = e.getMessage();
        } finally {
            ConnectUtil.closeConn(conn);
        }
        return bool;
    }

    /**
	 * ��������б�ĳ����¼�Ƿ����
	 * @param condtion
	 * @return
	 */
    public boolean exist(String condtion) {
        boolean bool = false;
        this.result = null;
        Connection conn = null;
        try {
            PojoParser parser = PojoParser.getInstances();
            conn = ConnectUtil.getConnect();
            PreparedStatement ps = conn.prepareStatement("select count(*) from " + parser.getTableName(cls) + " where " + condtion);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getLong(1) != 0) bool = true;
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            this.result = e.getMessage();
        } finally {
            ConnectUtil.closeConn(conn);
        }
        return bool;
    }

    /**
	 * �������ȡ��ĳ�е����ֵ
	 * @param maxCol
	 * @param condition
	 * @return
	 */
    public String max(String maxCol, String condition) {
        String value = null;
        this.result = null;
        Connection conn = null;
        try {
            PojoParser parser = PojoParser.getInstances();
            conn = ConnectUtil.getConnect();
            PreparedStatement ps = conn.prepareStatement("select max(" + maxCol + ") from " + parser.getTableName(cls) + " where " + condition);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (PojoParser.getInstances().getColType(cls, maxCol).equals("long")) value = String.valueOf(rs.getLong(1)); else value = rs.getString(1);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            this.result = e.getMessage();
        } finally {
            ConnectUtil.closeConn(conn);
        }
        return value;
    }

    /**
	 * ȡ�ü�¼��
	 * @param sql
	 * @return
	 */
    public long count(String sql) {
        long count = 0;
        this.result = null;
        Connection conn = null;
        try {
            conn = ConnectUtil.getConnect();
            PreparedStatement ps = conn.prepareStatement("select count(*) " + sql.substring(sql.toLowerCase().indexOf("from")));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                count = rs.getLong(1);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            this.result = e.getMessage();
        } finally {
            ConnectUtil.closeConn(conn);
        }
        return count;
    }

    /**
	 * ����һ����¼��Ϊ�¼�¼���������������ֵ
	 * @param id
	 * @return
	 */
    public boolean copy(long id) {
        boolean bool = false;
        this.result = null;
        Connection conn = null;
        Object vo = null;
        try {
            PojoParser parser = PojoParser.getInstances();
            conn = ConnectUtil.getConnect();
            conn.setAutoCommit(false);
            String sql = SqlUtil.getInsertSql(this.getCls());
            vo = this.findById(conn, "select * from " + parser.getTableName(cls) + " where " + parser.getPriamryKey(cls) + "=" + id);
            String pk = parser.getPriamryKey(cls);
            this.getClass().getMethod("set" + SqlUtil.getFieldName(pk), new Class[] { long.class }).invoke(vo, new Object[] { 0 });
            PreparedStatement ps = conn.prepareStatement(sql);
            setPsParams(ps, vo);
            ps.executeUpdate();
            ps.close();
            conn.commit();
            bool = true;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ex) {
            }
            this.result = e.getMessage();
        } finally {
            this.closeConnectWithTransaction(conn);
        }
        return bool;
    }

    /**
	 * ����һ����¼��ͬʱ���ƶ�Ӧ���ӱ��¼
	 * @param subCls
	 * @param subCol
	 * @param id
	 * @return
	 */
    public boolean copy(Class<?> subCls, String subCol, long id) {
        boolean bool = false;
        this.result = null;
        Connection conn = null;
        Object vo = null;
        try {
            conn = ConnectUtil.getConnect();
            conn.setAutoCommit(false);
            PojoParser parser = PojoParser.getInstances();
            String sql = SqlUtil.getInsertSql(this.getCls());
            vo = this.findById(conn, "select * from " + parser.getTableName(cls) + " where " + parser.getPriamryKey(cls) + "=" + id);
            String pk = parser.getPriamryKey(cls);
            this.getCls().getMethod("set" + SqlUtil.getFieldName(pk), new Class[] { long.class }).invoke(vo, new Object[] { 0 });
            PreparedStatement ps = conn.prepareStatement(sql);
            setPsParams(ps, vo);
            ps.executeUpdate();
            ps.close();
            long key = this.id;
            parser = PojoParser.getInstances();
            sql = SqlUtil.getInsertSql(subCls);
            Class<?> clses = this.cls;
            this.cls = subCls;
            ps = conn.prepareStatement("select * from " + parser.getTableName(subCls) + " where " + subCol + "=" + id);
            this.assembleObjToList(ps);
            ps = conn.prepareStatement(sql);
            ids = new long[orgList.size()];
            Method m = subCls.getMethod("set" + SqlUtil.getFieldName(subCol), new Class[] { long.class });
            for (int i = 0; i < orgList.size(); ++i) {
                Object obj = orgList.get(i);
                subCls.getMethod("set" + SqlUtil.getFieldName(parser.getPriamryKey(subCls)), new Class[] { long.class }).invoke(obj, new Object[] { 0 });
                m.invoke(obj, new Object[] { key });
                setPsParams(ps, obj);
                ps.addBatch();
                if ((i % 100) == 0) ps.executeBatch();
                ids[i] = this.id;
            }
            ps.executeBatch();
            ps.close();
            conn.commit();
            this.cls = clses;
            this.id = key;
            bool = true;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            this.result = e.getMessage();
        } finally {
            this.closeConnectWithTransaction(conn);
        }
        return bool;
    }

    private void clearObjectsList() {
        if (orgList == null) orgList = new ArrayList<Object>(); else orgList.clear();
    }

    /**
	 * ����һ��������¼
	 * @param obj
	 * @return
	 */
    public boolean update(Object[] obj) {
        boolean bool = false;
        this.result = null;
        if (obj == null || obj.length == 0) return bool;
        this.setCls(obj[0].getClass());
        Connection conn = null;
        try {
            conn = ConnectUtil.getConnect();
            PojoParser parser = PojoParser.getInstances();
            conn.setAutoCommit(false);
            String sql = SqlUtil.getUpdateSql(this.getCls());
            String primaryKey = parser.getPriamryKey(cls);
            Method m = this.getCls().getMethod("get" + SqlUtil.getFieldName(primaryKey), new Class[] {});
            String whereSql = " ";
            String rel = "";
            for (int i = 0; i < obj.length; ++i) {
                Object key = m.invoke(obj[i], new Object[] {});
                if (parser.getPriamryType(cls).equals("long")) whereSql += rel + primaryKey + "=" + key; else whereSql += rel + primaryKey + "='" + key + "'";
                if (i == 0) rel = " or ";
            }
            String selectSql = "select * from " + parser.getTableName(cls) + " where " + whereSql;
            PreparedStatement ps = conn.prepareStatement(selectSql);
            this.assembleObjToList(ps);
            for (int i = 0; i < obj.length; ++i) {
                String key = m.invoke(obj[i], new Object[] {}) + "";
                for (int j = 0; j < orgList.size(); ++j) {
                    String v = m.invoke(this.orgList.get(i), new Object[] {}) + "";
                    if (key.equals(v)) {
                        assignBeforeUpdate(obj[i], this.orgList.get(j));
                        break;
                    }
                }
            }
            ps = conn.prepareStatement(sql);
            for (int i = 0; i < obj.length; ++i) {
                processUpdateParams(ps, obj[i]);
                ps.addBatch();
                if ((i % 100) == 0) ps.executeBatch();
            }
            ps.executeBatch();
            ps.close();
            conn.commit();
            bool = true;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
            }
            this.result = e.getMessage();
        } finally {
            closeConnectWithTransaction(conn);
        }
        return bool;
    }

    /**
	 * ����һ����¼
	 * @param obj
	 * @return
	 */
    public boolean update(Object obj) {
        boolean bool = false;
        this.result = null;
        if (obj == null) return bool;
        this.setCls(obj.getClass());
        Connection conn = null;
        Object vo = null;
        try {
            conn = ConnectUtil.getConnect();
            PojoParser parser = PojoParser.getInstances();
            String sql = SqlUtil.getUpdateSql(this.getCls());
            Method m = obj.getClass().getMethod("get" + SqlUtil.getFieldName(parser.getPriamryKey(cls)), new Class[] {});
            Object key = m.invoke(obj, new Object[] {});
            if (parser.getPriamryType(cls).equals("long")) vo = this.findById(conn, "select * from " + parser.getTableName(cls) + " where " + parser.getPriamryKey(cls) + "=" + key); else vo = this.findById(conn, "select * from " + parser.getTableName(cls) + " where " + parser.getPriamryKey(cls) + "='" + key + "'");
            assignBeforeUpdate(obj, vo);
            PreparedStatement ps = conn.prepareStatement(sql);
            processUpdateParams(ps, obj);
            ps.executeUpdate();
            ps.close();
            bool = true;
        } catch (Exception e) {
            this.result = e.getMessage();
        } finally {
            ConnectUtil.closeConn(conn);
        }
        return bool;
    }

    private void assignBeforeUpdate(Object obj, Object vo) throws Exception {
        PojoParser parser = PojoParser.getInstances();
        List<String> cols = parser.getColsName(cls);
        List<String> types = parser.getColsType(cls);
        for (int i = 0; i < cols.size(); ++i) {
            String name = SqlUtil.getFieldName(cols.get(i));
            Method m = vo.getClass().getDeclaredMethod("get" + name, new Class[] {});
            Object v1 = m.invoke(obj, new Object[] {});
            Object v2 = m.invoke(vo, new Object[] {});
            if (types.get(i).equals("long")) {
                if ((Long) v1 == 0 && (Long) v2 != 0) {
                    m = vo.getClass().getMethod("set" + name, new Class[] { long.class });
                    m.invoke(obj, new Object[] { v2 });
                }
            } else if (types.get(i).equals("string")) {
                if (v1 == null && v2 != null) {
                    m = vo.getClass().getMethod("set" + name, new Class[] { String.class });
                    m.invoke(obj, new Object[] { v2 });
                }
            } else if (types.get(i).equals("date")) {
                if (v1 == null && v2 != null) {
                    m = vo.getClass().getMethod("set" + name, new Class[] { Date.class });
                    m.invoke(obj, new Object[] { new Date(((Date) v2).getTime()) });
                }
            } else {
                if ((Double) v1 == 0 && (Double) v2 != 0) {
                    m = vo.getClass().getMethod("set" + name, new Class[] { double.class });
                    m.invoke(obj, new Object[] { v2 });
                }
            }
        }
    }

    private void processUpdateParams(PreparedStatement ps, Object obj) throws Exception {
        Class<? extends Object> clses = obj.getClass();
        PojoParser pojo = PojoParser.getInstances();
        List<String> cols = pojo.getColsName(cls);
        List<String> types = pojo.getColsType(cls);
        for (int i = 0; i < cols.size(); ++i) {
            Method m = clses.getMethod("get" + SqlUtil.getFieldName(cols.get(i)), new Class[] {});
            Object value = m.invoke(obj, new Object[] {});
            if (types.get(i).equals("long")) {
                ps.setLong(i + 1, (Long) value);
            } else if (types.get(i).equals("string")) {
                ps.setString(i + 1, (String) value);
            } else if (types.get(i).equals("date")) {
                if (value != null) ps.setTimestamp(i + 1, new java.sql.Timestamp(((Date) value).getTime())); else ps.setTimestamp(i + 1, null);
            } else {
                ps.setDouble(i + 1, (Double) value);
            }
        }
        Object value = clses.getMethod("get" + SqlUtil.getFieldName(pojo.getPriamryKey(cls)), new Class[] {}).invoke(obj, new Object[] {});
        if (pojo.getPriamryType(cls).equals("long")) {
            ps.setLong(cols.size() + 1, ((Long) value).longValue());
        } else {
            ps.setString(cols.size() + 1, (String) value);
        }
    }

    private void assembleObjToList(PreparedStatement ps) throws Exception {
        this.clearObjectsList();
        ResultSet rs = ps.executeQuery();
        ResultSetMetaData rsmd = rs.getMetaData();
        int colCnt = rsmd.getColumnCount();
        processColumnMeta(rsmd);
        while (rs.next()) {
            orgList.add(processResultSet(rs, colCnt));
        }
        rs.close();
        ps.close();
    }

    private void deleteSubTable(Connection conn, List<Object> data) throws Exception {
        PojoParser parser = PojoParser.getInstances();
        if (parser.getSubTables(cls) != null && data.size() > 0) {
            List<SubTableMeta> subs = parser.getSubTables(cls);
            for (SubTableMeta meta : subs) {
                if (meta.isDelete()) {
                    String rel = "";
                    System.out.println(this.cls);
                    Method m = this.cls.getMethod("get" + SqlUtil.getFieldName(parser.getPriamryKey(cls)), new Class[] {});
                    String subWhere = " where ";
                    for (int i = 0; i < data.size(); ++i) {
                        Object o = data.get(i);
                        this.subList.add(o);
                        if (parser.getPriamryType(cls).equals("long")) subWhere += rel + meta.getSubCol() + "=" + m.invoke(o, new Object[] {}); else subWhere += rel + meta.getSubCol() + "='" + m.invoke(o, new Object[] {}) + "'";
                        rel = " or ";
                    }
                    System.out.println(subWhere);
                    PreparedStatement ps = conn.prepareStatement("select * from " + meta.getSubTable() + subWhere);
                    Class<?> subCls = this.cls;
                    this.cls = meta.getSubCls();
                    this.assembleObjToList(ps);
                    List<Object> subdata = new ArrayList<Object>();
                    for (Object o : this.orgList) subdata.add(o);
                    ps.close();
                    if (subdata.size() > 0) {
                        ps = conn.prepareStatement("delete from " + meta.getSubTable() + subWhere);
                        ps.executeUpdate();
                        ps.close();
                    }
                    deleteSubTable(conn, subdata);
                    this.cls = subCls;
                }
            }
        }
    }

    /**
	 * ���sql���ɾ���¼
	 * @param sql
	 * @return
	 */
    public boolean delete(String sql) {
        boolean bool = false;
        this.result = null;
        Connection conn = null;
        try {
            if (this.subList == null) this.subList = new ArrayList<Object>(); else this.subList.clear();
            conn = ConnectUtil.getConnect();
            conn.setAutoCommit(false);
            List<Object> data = deleteData(conn, "select * " + sql.substring(sql.toLowerCase().indexOf(" from ")), sql);
            this.deleteSubTable(conn, data);
            conn.commit();
            bool = true;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ex) {
            }
            this.result = e.getMessage();
        } finally {
            this.closeConnectWithTransaction(conn);
        }
        return bool;
    }

    /**
	 * ���colName�ֶεĶ�Ӧֵ����ɾ���¼
	 * @param colName
	 * @param ids
	 * @return
	 */
    public boolean delete(String colName, String[] ids) {
        boolean bool = false;
        this.result = null;
        Connection conn = null;
        try {
            if (this.subList == null) this.subList = new ArrayList<Object>(); else this.subList.clear();
            conn = ConnectUtil.getConnect();
            conn.setAutoCommit(false);
            PojoParser parser = PojoParser.getInstances();
            String where = SqlUtil.getWhereSql(cls, colName, ids);
            List<Object> data = deleteData(conn, "select * from " + parser.getTableName(cls) + where, "delete from " + parser.getTableName(cls) + where);
            this.deleteSubTable(conn, data);
            conn.commit();
            bool = true;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ex) {
            }
            this.result = e.getMessage();
        } finally {
            this.closeConnectWithTransaction(conn);
        }
        return bool;
    }

    /**
	 * �������ֵ����ɾ���¼
	 * @param ids
	 * @return
	 */
    public boolean delete(String[] ids) {
        boolean bool = false;
        this.result = null;
        if (ids == null || ids.length == 0) return bool;
        Connection conn = null;
        try {
            conn = ConnectUtil.getConnect();
            conn.setAutoCommit(false);
            PojoParser parser = PojoParser.getInstances();
            String where = SqlUtil.getWhereSql(cls, null, ids);
            List<Object> data = deleteData(conn, "select * from " + parser.getTableName(cls) + where, "delete from " + parser.getTableName(cls) + where);
            this.deleteSubTable(conn, data);
            conn.commit();
            bool = true;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ex) {
            }
            this.result = e.getMessage();
            e.printStackTrace();
        } finally {
            this.closeConnectWithTransaction(conn);
        }
        return bool;
    }

    private List<Object> deleteData(Connection conn, String selectSql, String deleteSql) throws Exception {
        if (this.subList == null) this.subList = new ArrayList<Object>(); else this.subList.clear();
        PreparedStatement ps = conn.prepareStatement(selectSql);
        this.assembleObjToList(ps);
        List<Object> data = new ArrayList<Object>();
        for (Object o : this.orgList) data.add(o);
        ps = conn.prepareStatement(deleteSql);
        ps.executeUpdate();
        ps.close();
        return data;
    }

    /**
	 * ��������һ���������¼������ݿ�
	 * @param objs
	 * @return
	 */
    public boolean save(Object[] objs) {
        boolean bool = false;
        this.result = null;
        if (objs == null || objs.length == 0) return bool;
        Connection conn = null;
        try {
            conn = ConnectUtil.getConnect();
            conn.setAutoCommit(false);
            String sql = SqlUtil.getInsertSql(this.getCls());
            PreparedStatement ps = conn.prepareStatement(sql);
            ids = new long[objs.length];
            for (int i = 0; i < objs.length; ++i) {
                setPsParams(ps, objs[i]);
                ps.addBatch();
                if ((i % 100) == 0) ps.executeBatch();
                ids[i] = this.id;
            }
            ps.executeBatch();
            ps.close();
            bool = true;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
            }
            this.result = e.getMessage();
        } finally {
            closeConnectWithTransaction(conn);
        }
        return bool;
    }

    /**
	 * ����һ����¼����ݿ�
	 * @param obj
	 * @return
	 */
    public boolean save(Object obj) {
        boolean bool = false;
        this.result = null;
        if (obj == null) return bool;
        Connection conn = null;
        try {
            conn = ConnectUtil.getConnect();
            conn.setAutoCommit(false);
            String sql = SqlUtil.getInsertSql(this.getCls());
            PreparedStatement ps = conn.prepareStatement(sql);
            setPsParams(ps, obj);
            ps.executeUpdate();
            ps.close();
            conn.commit();
            bool = true;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
            }
            this.result = e.getMessage();
        } finally {
            this.closeConnectWithTransaction(conn);
        }
        return bool;
    }

    private void setPsParams(PreparedStatement ps, Object obj) throws Exception {
        PojoParser pojo = PojoParser.getInstances();
        Method m = obj.getClass().getMethod("get" + SqlUtil.getFieldName(pojo.getPriamryKey(cls)), new Class[] {});
        if (pojo.getPriamryType(cls).equals("long")) {
            this.id = (Long) m.invoke(obj, new Object[] {});
            if (id == 0) id = SequencesUtil.getInstance().getPrimaryKey(pojo.getTableName(cls));
            ps.setLong(1, id);
            m = obj.getClass().getMethod("set" + SqlUtil.getFieldName(pojo.getPriamryKey(cls)), new Class[] { long.class });
            m.invoke(obj, new Object[] { this.id });
        } else {
            ps.setString(1, String.valueOf(m.invoke(obj, new Object[] {})));
        }
        int offset = 2;
        List<String> cols = pojo.getColsName(cls);
        List<String> types = pojo.getColsType(cls);
        for (int i = 0; i < cols.size(); ++i) {
            String colType = types.get(i);
            m = obj.getClass().getMethod("get" + SqlUtil.getFieldName(cols.get(i)), new Class[] {});
            if (colType.equals("long")) {
                ps.setLong(i + offset, (Long) m.invoke(obj, new Object[] {}));
            } else if (colType.equals("string")) {
                ps.setString(i + offset, (String) m.invoke(obj, new Object[] {}));
            } else if (colType.equals("double")) {
                ps.setDouble(i + offset, (Double) m.invoke(obj, new Object[] {}));
            } else {
                Date d = (Date) m.invoke(obj, new Object[] {});
                if (d != null) ps.setTimestamp(i + offset, new java.sql.Timestamp(d.getTime())); else ps.setTimestamp(i + offset, null);
            }
        }
    }

    /**
	 * ���sql���������������ļ�¼�������ص�һ����¼�����û���򷵻�null
	 * @param sql
	 * @return
	 */
    public Object findBySql(String sql) {
        Object vo = null;
        this.result = null;
        Connection conn = null;
        try {
            conn = ConnectUtil.getConnect();
            vo = this.findById(conn, sql);
        } catch (Exception e) {
            this.result = e.getMessage();
        } finally {
            ConnectUtil.closeConn(conn);
        }
        return vo;
    }

    /**
	 * �������ֵ���Ҷ�Ӧ�ļ�¼�������ڣ�����null
	 * @param id
	 * @return
	 */
    public Object findById(long id) {
        Object vo = null;
        this.result = null;
        Connection conn = null;
        try {
            conn = ConnectUtil.getConnect();
            PojoParser parser = PojoParser.getInstances();
            String key = parser.getPriamryKey(cls);
            vo = this.findById(conn, "select * from " + parser.getTableName(cls) + " where " + key + "=" + id);
        } catch (Exception e) {
            this.result = e.getMessage();
        } finally {
            ConnectUtil.closeConn(conn);
        }
        return vo;
    }

    /**
	 * ���sql���������������ļ�¼�������ص�һ����¼�����û���򷵻�null
	 * @param conn
	 * @param sql
	 * @return
	 * @throws Exception
	 */
    private Object findById(Connection conn, String sql) throws Exception {
        Object vo = null;
        PreparedStatement ps = conn.prepareStatement(sql);
        this.assembleObjToList(ps);
        if (this.orgList.size() != 0) vo = this.getOrgList().get(0);
        return vo;
    }

    /**
	 * �������ֵ���Ҷ�Ӧ�ļ�¼�������ڣ�����null
	 * @param id
	 * @return
	 */
    public Object findById(String id) {
        Object vo = null;
        this.result = null;
        Connection conn = null;
        try {
            conn = ConnectUtil.getConnect();
            PojoParser parser = PojoParser.getInstances();
            String key = parser.getPriamryKey(cls);
            vo = this.findById(conn, "select * from " + parser.getTableName(cls) + " where " + key + "='" + id + "'");
        } catch (Exception e) {
            this.result = e.getMessage();
        } finally {
            ConnectUtil.closeConn(conn);
        }
        return vo;
    }

    /**
	 * ִ�в�ѯ��û�з�ҳ����
	 * @param sql
	 * @return
	 */
    public List<Object> search(String sql) {
        List<Object> list = null;
        this.result = null;
        Connection conn = null;
        try {
            conn = ConnectUtil.getConnect();
            PreparedStatement ps = conn.prepareStatement(sql);
            this.assembleObjToList(ps);
            list = this.getOrgList();
        } catch (Exception e) {
            this.result = e.getMessage();
        } finally {
            ConnectUtil.closeConn(conn);
        }
        return list;
    }

    /**
	 * ִ�в�ѯ�����з�ҳ����
	 * @param sql 
	 * @param pageNo ҳ��
	 * @param size ÿҳ��¼��
	 * @return
	 */
    public List<Object> searchPage(String sql, int page, int size) {
        List<Object> list = null;
        this.result = null;
        Connection conn = null;
        try {
            conn = ConnectUtil.getConnect();
            sql = SqlUtil.getQuerySql(sql);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, (page - 1) * size);
            if (ConnectUtil.getDatabaseName().indexOf("MYSQL") != -1 || ConnectUtil.getDatabaseName().indexOf("HSQL") != -1) ps.setInt(2, size); else ps.setInt(2, page * size);
            this.assembleObjToList(ps);
            list = this.getOrgList();
        } catch (Exception e) {
            this.result = e.getMessage();
        } finally {
            ConnectUtil.closeConn(conn);
        }
        return list;
    }

    public boolean execSql(String sql) {
        boolean bool = false;
        this.result = null;
        Connection conn = null;
        try {
            conn = ConnectUtil.getConnect();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.executeUpdate();
            ps.close();
            bool = true;
        } catch (Exception e) {
            this.result = e.getMessage();
        } finally {
            ConnectUtil.closeConn(conn);
        }
        return bool;
    }

    private Object processResultSet(ResultSet rs, int colCnt) throws Exception {
        Object vo = this.cls.newInstance();
        for (int i = 0; i < colCnt; ++i) {
            processData(vo, rs, i);
        }
        return vo;
    }

    private void processData(Object vo, ResultSet rs, int index) throws Exception {
        String name = SqlUtil.getFieldName(this.getColName(index));
        if (name.equals("RowNum")) return;
        if (getColType(index).equals("java.lang.String")) {
            Method m = vo.getClass().getMethod("set" + name, new Class[] { String.class });
            m.invoke(vo, new Object[] { rs.getString(index + 1) });
        } else if (getColType(index).equals("java.lang.Long")) {
            Method m = vo.getClass().getMethod("set" + name, new Class[] { long.class });
            m.invoke(vo, new Object[] { new Long(rs.getLong(index + 1)) });
        } else if (getColType(index).equals("java.lang.Integer")) {
            Method m = vo.getClass().getMethod("set" + name, new Class[] { long.class });
            m.invoke(vo, new Object[] { new Long(rs.getInt(index + 1)) });
        } else if (getColType(index).equals("java.lang.Short")) {
            Method m = vo.getClass().getMethod("set" + name, new Class[] { long.class });
            m.invoke(vo, new Object[] { new Long(rs.getShort(index + 1)) });
        } else if (getColType(index).equals("java.lang.Byte")) {
            Method m = vo.getClass().getMethod("set" + name, new Class[] { long.class });
            m.invoke(vo, new Object[] { new Long(rs.getByte(index + 1)) });
        } else if (getColType(index).equals("java.sql.Date")) {
            Method m = vo.getClass().getMethod("set" + name, new Class[] { Date.class });
            Date d = new Date();
            java.sql.Date date = rs.getDate(index + 1);
            if (date != null) {
                d.setTime(date.getTime());
                m.invoke(vo, new Object[] { d });
            } else m.invoke(vo, new Object[] { null });
        } else if (getColType(index).equals("java.sql.Timestamp")) {
            Method m = vo.getClass().getMethod("set" + name, new Class[] { Date.class });
            Date d = new Date();
            java.sql.Timestamp date = rs.getTimestamp(index + 1);
            if (date != null) {
                d.setTime(date.getTime());
                m.invoke(vo, new Object[] { d });
            } else m.invoke(vo, new Object[] { null });
        } else if (getColType(index).equals("java.sql.Time")) {
            Method m = vo.getClass().getMethod("set" + name, new Class[] { Date.class });
            Date d = new Date();
            java.sql.Time date = rs.getTime(index + 1);
            if (date != null) {
                d.setTime(date.getTime());
                m.invoke(vo, new Object[] { d });
            } else m.invoke(vo, new Object[] { null });
        } else if (getColType(index).equals("java.lang.Double")) {
            Method m = vo.getClass().getMethod("set" + name, new Class[] { double.class });
            m.invoke(vo, new Object[] { new Double(rs.getDouble(index + 1)) });
        } else if (getColType(index).equals("java.lang.Float")) {
            Method m = vo.getClass().getMethod("set" + name, new Class[] { double.class });
            m.invoke(vo, new Object[] { new Float(rs.getFloat(index + 1)) });
        }
    }

    private void processColumnMeta(ResultSetMetaData rsmd) throws Exception {
        int colCnt = rsmd.getColumnCount();
        if (columnMeta == null) columnMeta = new ArrayList<String>(colCnt);
        columnMeta.clear();
        if (columnType == null) columnType = new ArrayList<String>(colCnt);
        columnType.clear();
        for (int i = 1; i <= colCnt; ++i) {
            columnMeta.add(rsmd.getColumnName(i));
            columnType.add(rsmd.getColumnClassName(i));
        }
    }

    public Class<?> getCls() {
        return cls;
    }

    public BaseDao setCls(Class<?> cls) {
        this.cls = cls;
        return this;
    }

    public String getColType(int index) {
        if (columnType == null) return ""; else if (index == -1 || index >= columnType.size()) return "";
        return (String) columnType.get(index);
    }

    public String getColName(int index) {
        if (columnMeta == null) return ""; else if (index == -1 || index >= columnMeta.size()) return "";
        return (String) columnMeta.get(index);
    }

    public List<String> getColumnMeta() {
        return columnMeta;
    }

    public List<String> getColumnType() {
        return columnType;
    }

    public long[] getIds() {
        return ids;
    }

    public List<Object> getOrgList() {
        return orgList;
    }

    public String getResult() {
        return result;
    }

    private void closeConnectWithTransaction(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (Exception ex) {
        }
        ConnectUtil.closeConn(conn);
    }

    public List<Object> getSubList() {
        return subList;
    }
}
