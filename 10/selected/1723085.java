package net.os.utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.util.StringUtils;

public final class SQLUtils {

    private static final Log logger = LogFactory.getLog(SQLUtils.class);

    public static List<Map<String, String>> buildSqlList(final JdbcTemplate jt, final String sql, final Object[] dates) {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        jt.query(new PreparedStatementCreator() {

            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                final PreparedStatement result = con.prepareStatement(sql);
                SQLUtils.buildStatement(result, dates);
                return result;
            }
        }, new RowCallbackHandler() {

            @Override
            public void processRow(final ResultSet rs) throws SQLException {
                final Map<String, String> map = new HashMap<String, String>();
                final ResultSetMetaData rsmd = rs.getMetaData();
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    final String cName = rsmd.getColumnLabel(i + 1);
                    final String cValeu = rs.getString(cName);
                    map.put(cName, cValeu);
                }
                list.add(map);
            }
        });
        return list;
    }

    /**
	 * ��һ����ѯ�л�ȡĳһ���ֶε�ֵ
	 * 
	 * @param sql String
	 * @param st Statement
	 * @return List
	 */
    public static List<String[]> getValuesFromSQL(final String sql, final Statement st, final String[] column) throws Exception {
        ResultSet rs = null;
        try {
            rs = st.executeQuery(sql);
            final List<String[]> result = new ArrayList<String[]>();
            while (rs.next()) {
                final String[] values = new String[column.length];
                for (int i = 0; i < values.length; i++) {
                    values[i] = rs.getString(column[i]);
                }
                result.add(values);
            }
            return result;
        } finally {
            closeAll(rs, null, null);
        }
    }

    /**
	 * ��һ����ѯ�л�ȡĳһ���ֶε�ֵ
	 * 
	 * @param sql String
	 * @param st Statement
	 * @return List
	 */
    public static List<String> getValuesFromSQL(final String sql, final Statement st) throws Exception {
        ResultSet rs = null;
        try {
            rs = st.executeQuery(sql);
            final List<String> result = new ArrayList<String>();
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            return result;
        } finally {
            closeAll(rs, null, null);
        }
    }

    public static List<String> getValuesFromSQL(final String sql, final Connection conn) throws Exception {
        Statement st = null;
        ResultSet rs = null;
        final List<String> result = new ArrayList<String>();
        try {
            st = conn.createStatement();
            rs = st.executeQuery(sql);
            final ResultSetMetaData rsmd = rs.getMetaData();
            while (rs.next()) {
                final StringBuffer buf = new StringBuffer();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    buf.append(rs.getObject(i));
                    if (i++ != rsmd.getColumnCount()) {
                        buf.append("_");
                    }
                }
                result.add(buf.toString());
            }
        } finally {
            closeAll(rs, st, null);
        }
        return result;
    }

    /**
	 * ��SQL��Ҫ��ѯ�ֶε�˳������ķ���
	 * 
	 * @param sql
	 * @param values
	 * @return
	 * @throws Exception
	 */
    public static ResultSet getResultFromSQL(final String sql, final Object[] values, final Connection conn) throws Exception {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = conn.prepareStatement(sql);
            if (values != null) {
                SQLUtils.buildStatement(st, values);
            }
            rs = st.executeQuery();
            return rs;
        } finally {
            closeAll(null, st, null);
        }
    }

    public static final String getInsertSql(final String tableName, final List<String> lables) {
        final StringBuffer buf = new StringBuffer();
        try {
            buf.append("INSERT INTO ").append(tableName).append(" (");
            final StringBuffer vBuf = new StringBuffer();
            boolean isFirst = true;
            if (lables != null) for (final String label : lables) {
                if (isFirst) isFirst = false; else {
                    buf.append(",");
                    vBuf.append(",");
                }
                buf.append(label);
                vBuf.append("?");
            }
            buf.append(" ) ").append(" VALUES(").append(vBuf.toString()).append(")");
        } catch (final Exception e) {
        }
        return buf.toString();
    }

    /**
	 * ��ָ֯����ݿ�(��connΪ���Դ)�ı�tableName�������insert���
	 * 
	 * @param tableName
	 * @param conn
	 * @return
	 */
    public static final String getInsertSql(final String sql, final String tableName, final Connection conn) {
        final StringBuffer buf = new StringBuffer();
        try {
            final List<String> lables = getTableLabels(sql, conn);
            buf.append("INSERT INTO ").append(tableName).append(" (");
            final StringBuffer vBuf = new StringBuffer();
            boolean isFirst = true;
            for (final String label : lables) {
                if (isFirst) isFirst = false; else {
                    buf.append(",");
                    vBuf.append(",");
                }
                buf.append(label);
                vBuf.append("?");
            }
            buf.append(" ) ").append(" VALUES(").append(vBuf.toString()).append(")");
        } catch (final Exception e) {
        }
        return buf.toString();
    }

    public static String getInsertSQL(final String tablename, final int columnsLen) {
        final StringBuffer sql = new StringBuffer("insert into " + tablename + " values(?");
        for (int i = 1; i < columnsLen; i++) {
            sql.append(",?");
        }
        sql.append(")");
        return sql.toString();
    }

    public static String getDatabaseName(final Connection conn) {
        try {
            return conn.getMetaData().getDatabaseProductName();
        } catch (final SQLException e) {
            return "";
        }
    }

    public static String getDatabaseName(final JdbcTemplate jt) {
        Connection con = null;
        try {
            try {
                con = jt.getDataSource().getConnection();
                return con.getMetaData().getDatabaseProductName();
            } catch (final SQLException e) {
                return "";
            }
        } finally {
            SQLUtils.closeAll(null, null, con);
        }
    }

    public static String getInsertSQL(final String tablename, final String[] columnsNames, final int columnsLen) {
        final StringBuffer sql = new StringBuffer("insert into " + tablename);
        if (null != columnsNames && columnsNames.length == columnsLen) {
            sql.append(" (");
            final boolean isFirst = true;
            for (final String columnName : columnsNames) {
                if (!isFirst) sql.append(", ");
                sql.append(columnName);
            }
            sql.append(" )");
        }
        sql.append(" values (?");
        for (int i = 1; i < columnsLen; i++) {
            sql.append(",?");
        }
        sql.append(")");
        return sql.toString();
    }

    public static void getFieldValues(final Object[] values) {
        for (int i = 0; i < values.length; i++) {
            final String[] arr = values[i].toString().split("=");
            if (arr.length > 1) {
                values[i] = values[i].toString().split("=")[1];
            }
        }
    }

    public static void insertIntoDatabase(final String tablename, final String line, final Statement pst, final String split) throws Exception {
        try {
            final String[] arr = line.split(String.valueOf(split.charAt(0)));
            String sql = "";
            for (final String str : arr) {
                if (sql.length() > 0) sql += ",";
                sql += "'" + str + "'";
            }
            sql = "insert into " + tablename + " values(" + sql + ")";
            pst.execute(sql);
        } catch (final Exception e) {
            throw e;
        }
    }

    public static Object[] getArrayFormObject(final Object obj, final String split) {
        Object[] array = null;
        if (obj instanceof String) {
            if (split.length() == 1) array = String.valueOf(obj).split(String.valueOf(split.charAt(0))); else array = StringUtils.split((String) obj, split);
        } else if (obj instanceof List) {
            array = ((List<?>) obj).toArray();
        } else if (obj instanceof String[]) {
            array = (String[]) obj;
        }
        return array;
    }

    public static void buildStatement(final PreparedStatement ps, final Object[] parameters) throws SQLException {
        int i = 1;
        for (final Object parameter : parameters) {
            if (parameter == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else if (parameter instanceof String) {
                ps.setString(i++, (String) parameter);
            } else if (parameter instanceof Byte) {
                ps.setByte(i++, ((Byte) parameter).byteValue());
            } else if (parameter instanceof java.util.Date) {
                ps.setTimestamp(i++, new Timestamp(((java.util.Date) parameter).getTime()));
            } else if (parameter instanceof byte[]) {
                ps.setBytes(i++, (byte[]) parameter);
            } else {
                ps.setObject(i++, parameter);
            }
        }
    }

    public static void rollback(final Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (final SQLException ex) {
                logger.error(ex);
            }
        }
    }

    /**
	 * 
	 * @param jdbcResultSet
	 * @param statement
	 * @param connection @ deprecated ������ֻ�ʺϹر���ͨ�����ӣ����ǵ�ϵͳ�н�������Spring��������ݿ����Ӽ��Ƶ���
	 *            ʹ�� {@link #closeAllForPool(ResultSet, Statement, Connection)}
	 */
    public static void closeAll(final ResultSet rs, Statement st, final Connection conn) {
        try {
            if (rs != null) rs.close();
        } catch (final SQLException e) {
        }
        try {
            if (st != null) st.close();
        } catch (final SQLException e) {
            st = null;
        }
        try {
            if (conn != null) conn.close();
        } catch (final SQLException e) {
        }
    }

    public static int getSize(final Connection conn, final String table, final String where, final Object[] params) throws SQLException {
        int size = 0;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            final String sql = "select count(*) from " + table + " where " + where;
            ps = conn.prepareStatement(sql);
            buildStatement(ps, params);
            rs = ps.executeQuery();
            if (rs.next()) {
                size = rs.getInt(1);
            }
        } finally {
            closeAll(rs, ps, null);
        }
        return size;
    }

    public static String toString(SQLWarning sw) {
        final StringBuffer sb = new StringBuffer();
        final String s = System.getProperty("line.separator");
        do {
            sb.append("error code = ").append(sw.getErrorCode()).append(s);
            sb.append("localized message = ");
            sb.append(sw.getLocalizedMessage()).append(s);
            sb.append("message = ").append(sw.getMessage()).append(s);
            sb.append("sqlstate = ").append(sw.getSQLState()).append(s);
            sw = sw.getNextWarning();
        } while (sw != null);
        return sb.toString();
    }

    public static String toString(SQLException ex) {
        final StringBuffer sb = new StringBuffer();
        final String s = System.getProperty("line.separator");
        do {
            sb.append("error code = ").append(ex.getErrorCode()).append(s);
            sb.append("localized message = ");
            sb.append(ex.getLocalizedMessage()).append(s);
            sb.append("message = ").append(ex.getMessage()).append(s);
            sb.append("sqlstate = ").append(ex.getSQLState()).append(s);
            ex = ex.getNextException();
        } while (ex != null);
        return sb.toString();
    }

    /**
	 * ��һ�����ּ�����ϳɱ�׼SQL����е�IN����е�����
	 * 
	 * @param valueArr
	 * @param withParentheses ָ��IN�����'()'�Ƿ񱻼��뷵��ֵ
	 * @return
	 */
    public static String buildSqlInValues(final List<Integer> valueArr, final boolean withParentheses) {
        final StringBuffer sqlIn = new StringBuffer();
        if (withParentheses) sqlIn.append(" (");
        boolean isFirst = true;
        for (final int value : valueArr) {
            if (isFirst) isFirst = false; else sqlIn.append(", ");
            sqlIn.append(value);
        }
        if (withParentheses) sqlIn.append(")");
        return sqlIn.toString();
    }

    /**
	 * ִ��һ��SQL���,��������е�һ�е�һ�е�ֵ����,�����߱���ָ����ݿ�����
	 * 
	 * @param <T>
	 * @param querySql
	 * @param tType
	 * @param conn
	 * @return
	 */
    public static final <T> T query(final String querySql, final Class<T> tType, final Connection conn) {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            final String cName = tType.getCanonicalName();
            final boolean isTimestamp = "java.sql.Timestamp".equals(cName);
            final boolean isDouble = "java.lang.Double".equals(cName);
            final boolean isInteger = "java.lang.Integer".equals(cName);
            final boolean isFloat = "java.lang.Float".equals(cName);
            final boolean isShot = "java.lang.Short".equals(cName);
            final boolean isLong = "java.lang.Long".equals(cName);
            final boolean isBigDecimal = "class java.math.BigDecimal".equals(cName);
            final boolean isString = "java.lang.String".equals(cName);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(querySql);
            Object value = null;
            if (rs.next()) {
                if (isTimestamp) value = rs.getTimestamp(1); else if (isString) {
                    value = StringsUtils.trimNull(rs.getString(1));
                } else if (isDouble) value = rs.getDouble(1); else if (isInteger) value = rs.getInt(1); else if (isFloat) value = rs.getFloat(1); else if (isShot) value = rs.getShort(1); else if (isLong) value = rs.getLong(1); else if (isBigDecimal) value = rs.getBigDecimal(1); else value = rs.getObject(1);
            }
            if (null == value) value = tType.newInstance();
            return tType.cast(value);
        } catch (final Exception e) {
            logger.error(e);
        } finally {
            closeAll(rs, stmt, null);
        }
        try {
            return tType.newInstance();
        } catch (final Exception e) {
            return null;
        }
    }

    /**
	 * @param conn
	 * @param sql
	 * @return ִ�гɹ�����true
	 * @throws SQLException
	 */
    public static final int executeSql(final Connection conn, final String sql) throws SQLException {
        return executeSql(conn, sql, false);
    }

    /**
	 * 
	 * @param conn
	 * @param sql
	 * @param rollback
	 * @return ִ�гɹ�����true
	 * @throws SQLException
	 */
    public static final int executeSql(final Connection conn, final String sql, final boolean rollback) throws SQLException {
        if (null == sql) return 0;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            final int updated = stmt.executeUpdate(sql);
            return updated;
        } catch (final SQLException e) {
            if (rollback) conn.rollback();
            throw e;
        } finally {
            closeAll(null, stmt, null);
        }
    }

    public static final boolean containsColumn(final ResultSet rs, final String columnLabel) {
        try {
            return rs.findColumn(columnLabel) != -1;
        } catch (final Exception e) {
            return false;
        }
    }

    public static final <T> T getObjectValue(final ResultSet rs, final String columnLabel, final Class<T> tType) {
        try {
            return tType.cast(rs.getObject(columnLabel));
        } catch (final Exception e) {
            try {
                return tType.newInstance();
            } catch (final Exception e1) {
                return tType.cast(new Object());
            }
        }
    }

    public static final Object getObjectValue(final ResultSet rs, final String columnLabel) {
        return getObjectValue(rs, columnLabel, new Object());
    }

    public static final Object getObjectValue(final ResultSet rs, final String columnLabel, final Object defValue) {
        try {
            Object obj = rs.getObject(columnLabel);
            if (obj instanceof java.util.Date) obj = rs.getTimestamp(columnLabel);
            return obj;
        } catch (final Exception e) {
            return defValue;
        }
    }

    public static final <T> Map<String, T> executeQueryAsLinkedHashMap(final String sql, final String keyColumn, final String valueColumn, final Class<T> t, final Connection conn) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            return executeQueryAsMap(sql, keyColumn, valueColumn, t, new LinkedHashMap<String, T>(), stmt);
        } catch (final Exception e) {
        } finally {
            closeAll(null, stmt, null);
        }
        return new LinkedHashMap<String, T>();
    }

    public static final <T> Map<String, T> executeQueryAsHashMap(final String sql, final String keyColumn, final String valueColumn, final Class<T> t, final Connection conn) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            return executeQueryAsMap(sql, keyColumn, valueColumn, t, new HashMap<String, T>(), stmt);
        } catch (final Exception e) {
        } finally {
            closeAll(null, stmt, null);
        }
        return new HashMap<String, T>();
    }

    public static final <T> Map<String, T> executeQueryAsHashMap(final String sql, final String keyColumn, final String valueColumn, final Class<T> t, final Statement stmt) {
        return executeQueryAsMap(sql, keyColumn, valueColumn, t, new HashMap<String, T>(), stmt);
    }

    public static final <T> Map<String, T> executeQueryAsMap(final String sql, final String keyColumn, final String valueColumn, final Class<T> t, final Map<String, T> result, final Statement stmt) {
        ResultSet rs = null;
        try {
            final String cName = t.getCanonicalName();
            final boolean isTimestamp = "java.sql.Timestamp".equals(cName);
            final boolean isDouble = "java.lang.Double".equals(cName);
            final boolean isInteger = "java.lang.Integer".equals(cName);
            final boolean isFloat = "java.lang.Float".equals(cName);
            final boolean isShot = "java.lang.Short".equals(cName);
            final boolean isLong = "java.lang.Long".equals(cName);
            final boolean isBigDecimal = "class java.math.BigDecimal".equals(cName);
            final boolean isString = "java.lang.String".equals(cName);
            rs = stmt.executeQuery(sql);
            Object value;
            while (rs.next()) {
                if (isTimestamp) value = rs.getTimestamp(valueColumn); else if (isString) {
                    value = StringsUtils.trimNull(rs.getString(valueColumn));
                } else if (isDouble) value = rs.getDouble(valueColumn); else if (isInteger) value = rs.getInt(valueColumn); else if (isFloat) value = rs.getFloat(valueColumn); else if (isShot) value = rs.getShort(valueColumn); else if (isLong) value = rs.getLong(valueColumn); else if (isBigDecimal) value = rs.getBigDecimal(valueColumn); else value = rs.getObject(valueColumn);
                if (null == value) value = t.newInstance();
                result.put(rs.getString(keyColumn), t.cast(value));
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            closeAll(rs, null, null);
        }
        return result;
    }

    public static final <T> List<T> executeAsList(final String sql, final String valueColumn, final Class<T> t, final JdbcTemplate jt) {
        final List<T> result = new ArrayList<T>();
        final String cName = t.getCanonicalName();
        final boolean isTimestamp = "java.sql.Timestamp".equals(cName);
        final boolean isDouble = "java.lang.Double".equals(cName);
        final boolean isInteger = "java.lang.Integer".equals(cName);
        final boolean isFloat = "java.lang.Float".equals(cName);
        final boolean isShot = "java.lang.Short".equals(cName);
        final boolean isLong = "java.lang.Long".equals(cName);
        final boolean isBigDecimal = "class java.math.BigDecimal".equals(cName);
        final boolean isString = "java.lang.String".equals(cName);
        jt.query(sql, new RowCallbackHandler() {

            @Override
            public void processRow(final ResultSet rs) throws SQLException {
                Object value = null;
                if (isTimestamp) value = rs.getTimestamp(valueColumn); else if (isString) {
                    value = StringsUtils.trimNull(rs.getString(valueColumn));
                } else if (isDouble) value = rs.getDouble(valueColumn); else if (isInteger) value = rs.getInt(valueColumn); else if (isFloat) value = rs.getFloat(valueColumn); else if (isShot) value = rs.getShort(valueColumn); else if (isLong) value = rs.getLong(valueColumn); else if (isBigDecimal) value = rs.getBigDecimal(valueColumn); else value = rs.getObject(valueColumn);
                result.add(t.cast(value));
            }
        });
        return result;
    }

    /**
	 * @param sql Ҫ��ѯ��SQL
	 * @param conn ��ݿ�����
	 * @param t ��ȡ����ֵ���������
	 * @return List&lt;Map&lt;String, T&gt;&gt; ��Զ����Ϊ��.
	 */
    public static final <T> List<Map<String, T>> executeQuery(final String sql, final Connection conn, final Class<T> t, final List<String> header, final Object[] objects) {
        final List<Map<String, T>> result = new ArrayList<Map<String, T>>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            if (objects != null) {
                SQLUtils.buildStatement(stmt, objects);
            }
            rs = stmt.executeQuery(sql);
            Map<String, T> item;
            Object value;
            String colLabel;
            if (header.size() == 0) {
                final ResultSetMetaData rsmd = rs.getMetaData();
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    colLabel = rsmd.getColumnLabel(i + 1);
                    if (header.contains(colLabel)) continue;
                    header.add(colLabel);
                }
            }
            final String cName = t.getCanonicalName();
            final boolean isTimestamp = "java.sql.Timestamp".equals(cName);
            final boolean isDouble = "java.lang.Double".equals(cName);
            final boolean isInteger = "java.lang.Integer".equals(cName);
            final boolean isFloat = "java.lang.Float".equals(cName);
            final boolean isShot = "java.lang.Short".equals(cName);
            final boolean isLong = "java.lang.Long".equals(cName);
            final boolean isBigDecimal = "class java.math.BigDecimal".equals(cName);
            final boolean isString = "java.lang.String".equals(cName);
            while (rs.next()) {
                item = new HashMap<String, T>();
                for (final String column : header) {
                    if (isTimestamp) value = rs.getTimestamp(column); else if (isString) {
                        value = StringsUtils.trimNull(rs.getString(column));
                    } else if (isDouble) value = rs.getDouble(column); else if (isInteger) value = rs.getInt(column); else if (isFloat) value = rs.getFloat(column); else if (isShot) value = rs.getShort(column); else if (isLong) value = rs.getLong(column); else if (isBigDecimal) value = rs.getBigDecimal(column); else value = rs.getObject(column);
                    if (null == value) value = "";
                    item.put(column, t.cast(value));
                }
                result.add(item);
            }
        } catch (final Exception e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println(sql);
        } finally {
            closeAll(rs, stmt, null);
        }
        return result;
    }

    public static final <T> List<Map<String, T>> executeQuery(final String sql, final Connection conn, final Class<T> t) {
        return executeQuery(sql, conn, t, new ArrayList<String>(), null);
    }

    public static int[] executeBatchs(final Connection conn, final String[] batchSqls) throws SQLException {
        return executeBatchs(conn, batchSqls, false);
    }

    public static int[] executeBatchs(final Connection conn, final String[] batchSqls, final boolean rollback) throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            for (final String sql : batchSqls) stmt.addBatch(sql);
            return stmt.executeBatch();
        } catch (final SQLException e) {
            if (rollback) conn.rollback();
            throw e;
        } finally {
            SQLUtils.closeAll(null, stmt, null);
        }
    }

    /**
	 * ����ĳһ�����һЩ�ֶ���Ϣ
	 * 
	 * @param tableName
	 * @param conn
	 * @param fields
	 * @param values
	 */
    public static final void executeSQL(final String sql, final Connection conn, final Object[] values) throws SQLException {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement(sql);
            if (values != null && values.length > 0) buildStatement(pst, values);
            pst.execute();
        } finally {
            SQLUtils.closeAll(null, pst, null);
        }
    }

    public static final boolean executeSQLS(final List<String> sqlList) throws Exception {
        return executeSQLS(sqlList);
    }

    public static final boolean executeSQLS(final List<String> sqlList, final Connection conn) throws Exception {
        return executeSQLS(sqlList, conn, "DELETE ", "DROP ");
    }

    public static final boolean executeSQLS(final List<String> sqlList, final Connection conn, final String... skips) throws Exception {
        if (null == sqlList || sqlList.isEmpty()) return true;
        final boolean result = true;
        Statement stmt = null;
        String sql = "";
        try {
            String tmpSql;
            int updated, sqlIdx = 0;
            for (final String sqlItem : sqlList) {
                sql = sqlItem;
                if (null == stmt) {
                    stmt = conn.createStatement();
                }
                try {
                    logger.info(StringsUtils.u("ִ������SQL�еĵ� ", ++sqlIdx, " ��SQL..."));
                    updated = stmt.executeUpdate(sql);
                    logger.info(StringsUtils.u("����SQL�еĵ� ", sqlIdx, " ��SQLִ��Ӱ�� ", updated, " ����¼..."));
                } catch (final SQLException sqle) {
                    tmpSql = sql.toUpperCase().trim();
                    if (StringsUtils.likesIn(tmpSql, skips)) {
                        logger.warn(StringsUtils.u("ִ��SQL:", sql, "ʱ������һ���쳣[", sqle.getMessage(), "],��������ִ��..."));
                        continue;
                    }
                    throw sqle;
                }
            }
        } catch (final Exception e) {
            logger.error("����ִ�ж���SQL�е�SQLʱ������һ������:" + sql, e);
            throw e;
        } finally {
            closeAll(null, stmt, null);
        }
        return result;
    }

    /**
	 * �����ж���ݿ���Ƿ����
	 * 
	 * @param tableName Ҫ��֤�Ƿ���ڵ���ݿ��
	 * @param conn ����ʱ����ָ��Ҫ��֤��Ŀ����ݿ�
	 * @return �����ڷ���true,���򷵻�false;
	 */
    public static final boolean isTableExists(final String tableName, final Connection conn) {
        final String sql = StringsUtils.u("SELECT 1 FROM ", tableName, " t WHERE 1>1");
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) return true;
            return true;
        } catch (final Exception e) {
        } finally {
            SQLUtils.closeAll(rs, stmt, null);
        }
        return false;
    }

    /**
	 * �����ж�ָ����ݿ������Ƿ����
	 * @param tableName Ҫ��֤�Ƿ�����е���ݿ��
	 * @param columnName Ҫ��֤�Ƿ���ڵ������
	 * @param conn ����ʱ����ָ��Ҫ��֤��Ŀ����ݿ�
	 * @return �����ڷ���true,���򷵻�false;
	 */
    public static final boolean isColumnExists(final String tableName, final String columnName, final Connection conn) {
        final String sql = StringsUtils.u("SELECT ", columnName, " FROM ", tableName, " t WHERE 1>1");
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) return true;
            return true;
        } catch (final Exception e) {
        } finally {
            SQLUtils.closeAll(rs, stmt, null);
        }
        return false;
    }

    public static final List<String> getTableLabels(final String tableName, final Connection conn) {
        final List<String> result = new ArrayList<String>();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            final String sql = StringsUtils.u("SELECT * FROM (", tableName, ") WHERE 1>1");
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            final ResultSetMetaData metaData = rs.getMetaData();
            final int colsLen = metaData.getColumnCount() + 1;
            for (int i = 1; i < colsLen; i++) {
                result.add(metaData.getColumnLabel(i).toUpperCase());
            }
        } catch (final Exception e) {
        } finally {
            closeAll(rs, stmt, null);
        }
        return result;
    }

    /**
	 * ��ȡ���Ľṹ�����ص���Ϣ��ֻ�������е��������ԣ�������ʾ��������ͣ���ݴ�С��������CLASSNAME)
	 * 
	 * @param tableName
	 * @param conn
	 * @return
	 */
    public static final List<Map<String, String>> getTableStruct(final String tableName, final Connection conn) {
        final List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            final String sql = StringsUtils.u("SELECT * FROM ", tableName, " WHERE 1>1");
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            final ResultSetMetaData metaData = rs.getMetaData();
            Map<String, String> rowMap;
            final int colsLen = metaData.getColumnCount() + 1;
            for (int i = 1; i < colsLen; i++) {
                rowMap = new HashMap<String, String>();
                rowMap.put("FIELD", metaData.getColumnName(i));
                rowMap.put("ZHCN", metaData.getColumnLabel(i));
                rowMap.put("DATATYPE", metaData.getColumnTypeName(i));
                rowMap.put("DATACLASS", metaData.getColumnClassName(i));
                rowMap.put("DATASIZE", String.valueOf(metaData.getColumnDisplaySize(i)));
                result.add(rowMap);
            }
        } catch (final Exception e) {
        } finally {
            closeAll(rs, stmt, null);
        }
        return result;
    }

    /**
	 * ��ȡ��ṹʱ���й�����
	 * 
	 * @author QianFei.Xu;E-Mail:qianfei.xu@rosense.cn
	 * @time May 13, 2009 4:33:26 PM
	 */
    public interface IColumnFilter {

        public boolean accept(String column);
    }

    ;

    /**
	 * @see {@link #getInsertSql(String, Connection, IColumnFilter)}
	 * @param tableName
	 * @param conn
	 * @return
	 */
    public static final String getInsertSql(final String tableName, final Connection conn) {
        return getInsertSql(tableName, conn, null);
    }

    /**
	 * ��ָ֯����ݿ�(��connΪ���Դ)�ı�tableName�������insert���
	 * 
	 * @param tableName
	 * @param conn
	 * @return
	 */
    public static final String getInsertSql(final String tableName, final Connection conn, final IColumnFilter filter) {
        final StringBuffer buf = new StringBuffer();
        try {
            final List<String> lables = getTableLabels(tableName, conn);
            buf.append("INSERT INTO ").append(tableName).append(" (");
            final StringBuffer vBuf = new StringBuffer();
            boolean isFirst = true;
            for (final String label : lables) {
                if (null != filter && !filter.accept(label)) continue;
                if (isFirst) isFirst = false; else {
                    buf.append(",");
                    vBuf.append(",");
                }
                buf.append(label);
                vBuf.append("?");
            }
            buf.append(" ) ").append(" VALUES(").append(vBuf.toString()).append(")");
        } catch (final Exception e) {
        }
        return buf.toString();
    }

    public static final String getInsertSql(final Collection<String> lables, final String tableName) {
        final StringBuffer buf = new StringBuffer();
        try {
            buf.append("INSERT INTO ").append(tableName).append(" (");
            final StringBuffer vBuf = new StringBuffer();
            boolean isFirst = true;
            for (final String label : lables) {
                if (isFirst) isFirst = false; else {
                    buf.append(",");
                    vBuf.append(",");
                }
                buf.append(label);
                vBuf.append("?");
            }
            buf.append(" ) ").append(" VALUES(").append(vBuf.toString()).append(")");
        } catch (final Exception e) {
        }
        return buf.toString();
    }

    public static final int dbToDbDataCopy(final String sourceSql, final Connection sourceConn, final String targetTableName, final String targetSql, final Connection targetConn, final int batchSize, final boolean rollback) throws Exception {
        Statement stmt = null;
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        int gSuccLen = 0;
        try {
            pstmt = targetConn.prepareStatement(targetSql);
            int rowsLen = 0, gRowsLen = 0;
            int colsLen = org.apache.commons.lang.StringUtils.countMatches(targetSql, "?");
            colsLen += 1;
            stmt = sourceConn.createStatement();
            rs = stmt.executeQuery(sourceSql);
            final ResultSetMetaData rsmd = rs.getMetaData();
            int metaData;
            logger.info(StringsUtils.u("��ʼ��ݿ���.... ��Ҫ��Ŀ����ݱ��в��������:", colsLen));
            while (rs.next()) {
                gRowsLen++;
                for (int i = 1; i < colsLen; i++) {
                    metaData = rsmd.getColumnType(i);
                    if (metaData == Types.DATE || metaData == Types.TIME || metaData == Types.TIMESTAMP) {
                        pstmt.setTimestamp(i, rs.getTimestamp(i));
                    } else pstmt.setObject(i, rs.getObject(i));
                }
                rowsLen++;
                pstmt.addBatch();
                if (rowsLen >= batchSize) {
                    gSuccLen += flushStatement(targetConn, pstmt, rollback);
                    rowsLen = 0;
                }
            }
            if (rowsLen > 0) gSuccLen += flushStatement(targetConn, pstmt, rollback);
            logger.info(StringsUtils.u("�� ", targetTableName, " ���й����Բ���:", gRowsLen, "�����,�ɹ�������:", gSuccLen));
        } catch (final SQLException sqle) {
            if (rollback) {
                try {
                    targetConn.rollback();
                } catch (final SQLException e1) {
                    e1.printStackTrace();
                }
            }
            throw sqle;
        } catch (final Exception e) {
            throw e;
        } finally {
            closeAll(rs, stmt, null);
            closeAll(null, pstmt, null);
        }
        return gSuccLen;
    }

    static final int flushStatement(final Connection conn, final Statement stmt, final boolean rollback) {
        try {
            final int updRows[] = stmt.executeBatch();
            return updRows.length;
        } catch (final SQLException e) {
            if (rollback) {
                try {
                    conn.rollback();
                } catch (final SQLException e1) {
                    e1.printStackTrace();
                }
            }
        } finally {
            try {
                stmt.clearBatch();
            } catch (final Exception e) {
            }
        }
        return 0;
    }

    public static final boolean createTableFromDB(final Connection srcConn, final String srcTable, final Connection targetConn, final String targetTable) throws Exception {
        Statement srcStmt = null, tgtStmt = null;
        ResultSet srcRs = null;
        final StringBuffer buf = new StringBuffer();
        try {
            final String sql = StringsUtils.u("SELECT * FROM ", srcTable, " WHERE 1>1");
            srcStmt = srcConn.createStatement();
            srcRs = srcStmt.executeQuery(sql);
            final ResultSetMetaData mdata = srcRs.getMetaData();
            final int colsLen = mdata.getColumnCount() + 1;
            buf.append("CREATE TABLE ").append(targetTable).append(" ( ");
            String colType;
            for (int i = 1; i < colsLen; i++) {
                if (i > 1) buf.append(" , ");
                colType = mdata.getColumnTypeName(i);
                buf.append(mdata.getColumnName(i)).append(" ").append(colType);
                if ("VARCHAR2".equals(colType) || "VARCHAR".equals(colType)) {
                    buf.append("(").append(mdata.getColumnDisplaySize(i)).append(")");
                }
            }
            buf.append(" ) ");
            tgtStmt = targetConn.createStatement();
            tgtStmt.execute(buf.toString());
            return true;
        } catch (final Exception e) {
            System.out.println(buf.toString());
            throw e;
        } finally {
            closeAll(srcRs, srcStmt, null);
            closeAll(null, tgtStmt, null);
        }
    }

    public static final boolean createTableFromDB1(final Connection srcConn, final String srcTable, final Connection targetConn, final String targetTable, final int var64ToAny, final List<String> endsVarList, final Map<String, String> customFields) throws Exception {
        Statement srcStmt = null, tgtStmt = null;
        ResultSet srcRs = null;
        final String cp1 = srcConn.getMetaData().getDatabaseProductName(), cp2 = targetConn.getMetaData().getDatabaseProductName();
        final StringBuffer buf = new StringBuffer();
        try {
            final String sql = StringsUtils.u("SELECT * FROM ", srcTable, " WHERE 1>1");
            srcStmt = srcConn.createStatement();
            srcRs = srcStmt.executeQuery(sql);
            final ResultSetMetaData mdata = srcRs.getMetaData();
            final int colsLen = mdata.getColumnCount() + 1;
            buf.append("CREATE TABLE ").append(targetTable).append(" ( ");
            String colType;
            for (int i = 1; i < colsLen; i++) {
                if (i > 1) buf.append(" , ");
                colType = mdata.getColumnTypeName(i);
                final String cName = mdata.getColumnName(i);
                buf.append(cName).append(" ");
                if (null != customFields && customFields.containsKey(cName)) {
                    buf.append(customFields.get(cName));
                    continue;
                }
                if (cp1.equals("Oracle") && cp2.equals("MySQL")) {
                    if ("VARCHAR2".equals(colType) || "VARCHAR".equals(colType)) {
                        buf.append("VARCHAR");
                        buf.append("(");
                        boolean endsFlag = true;
                        if (var64ToAny != -1) {
                            for (final String ev : endsVarList) {
                                if (cName.endsWith(ev)) {
                                    endsFlag = false;
                                    break;
                                }
                            }
                            if (endsFlag) {
                                buf.append(var64ToAny);
                            } else {
                                buf.append(mdata.getColumnDisplaySize(i));
                            }
                        } else {
                            buf.append(mdata.getColumnDisplaySize(i));
                        }
                        buf.append(")");
                    } else if ("NUMBER".equals(colType)) {
                        buf.append("DOUBLE");
                    } else if ("DATE".equals(colType)) {
                        buf.append("DATETIME");
                    } else {
                        buf.append(colType);
                    }
                } else if (cp1.equals("MySQL") && cp2.equals("Oracle")) {
                    if ("VARCHAR2".equals(colType) || "VARCHAR".equals(colType)) {
                        buf.append("VARCHAR2");
                        buf.append("(").append(mdata.getColumnDisplaySize(i)).append(")");
                    } else if ("DOUBLE".equals(colType)) {
                        buf.append("NUMBER");
                    } else {
                        buf.append(colType);
                    }
                } else {
                    buf.append(colType);
                }
            }
            buf.append(" ) ");
            tgtStmt = targetConn.createStatement();
            tgtStmt.execute(buf.toString());
            return true;
        } catch (final Exception e) {
            System.out.println(buf.toString());
            e.printStackTrace();
            throw e;
        } finally {
            closeAll(srcRs, srcStmt, null);
            closeAll(null, tgtStmt, null);
        }
    }

    public static final String getSql(final Connection con, final String function, final String querySql, final int rowStart, final int rowEnd) throws Exception {
        final StringBuffer finalSql = new StringBuffer();
        final DatabaseMetaData dmd = con.getMetaData();
        final String dbName = dmd.getDatabaseProductName().toLowerCase();
        if (dbName.equals("oracle")) {
            finalSql.append("SELECT /*+ FIRST_ROWS */arr.* ");
            if (function != null) {
                finalSql.append(function);
            }
            finalSql.append(" FROM (SELECT AFR.*, ROWNUM RN FROM (");
            finalSql.append(querySql);
            finalSql.append(" ) AFR WHERE ROWNUM <= ");
            finalSql.append(rowEnd);
            finalSql.append(" )arr WHERE RN > ");
            finalSql.append(rowStart);
            return finalSql.toString();
        } else if (dbName.equals("sybase")) {
            finalSql.append("exec rosense.GetDataByLine(");
            finalSql.append(querySql);
            finalSql.append(",");
            finalSql.append(rowStart);
            finalSql.append(",");
            finalSql.append(rowEnd);
            finalSql.append(" )");
            return finalSql.toString();
        }
        return null;
    }

    public static final int getTotalRows(final String querySql, final Connection con) {
        Statement stmt = null;
        ResultSet rs = null;
        final String countSql = "SELECT COUNT(*) FROM " + querySql;
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(countSql);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtils.closeAll(rs, stmt, null);
        }
        return 0;
    }

    /**
	 * ����������
	 * 
	 * @throws PmDataImportException
	 */
    public static final void beginTransactions(final Connection connection) throws SQLException {
        if (null != connection) try {
            connection.setAutoCommit(false);
        } catch (final SQLException e) {
            throw e;
        }
    }

    /**
	 * ��������̣������������ύ�������������Ϊ�Զ��ύ
	 * 
	 * @throws PmDataImportException
	 */
    public static final void commitTransactions(final Connection connection) throws SQLException {
        if (null != connection) {
            try {
                connection.commit();
                connection.setAutoCommit(true);
            } catch (final SQLException e) {
                throw e;
            }
        }
    }

    /**
	 * ȡ������ͨ������������ʧ��ʱ���� ���˷���ͬʱ�Ὣ������ӵ�Commit״̬����Ϊ�Զ�
	 * 
	 * @throws PmDataImportException
	 */
    public static final void rollbackTransactions(final Connection connection) throws SQLException {
        if (null != connection) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (final SQLException e) {
                throw e;
            }
        }
    }

    public static final int flushStatementUnSafe(final Statement stmt) throws SQLException {
        if (null == stmt) return 0;
        final int rows = stmt.executeBatch().length;
        stmt.clearBatch();
        stmt.clearWarnings();
        return rows;
    }

    /**
	 * ִ��Statement��Batch
	 * 
	 * @param stmt
	 * @return Ӱ����ݵ���Ŀ��
	 */
    public static final int flushStatement(final Statement stmt) {
        if (null == stmt) return 0;
        int rows = 0;
        try {
            rows = stmt.executeBatch().length;
            stmt.clearBatch();
            stmt.clearWarnings();
        } catch (final Throwable t) {
            t.printStackTrace();
        }
        return rows;
    }

    /**
	 * @param sql Ҫ��ѯ��SQL
	 * @param conn ��ݿ�����
	 * @param t ��ȡ����ֵ���������
	 * @return List&lt;Map&lt;String, T&gt;&gt; ��Զ����Ϊ��.
	 */
    public static final <T> List<Map<String, T>> executeQuery(final String sql, final Connection conn, final Class<T> t, final List<String> header) {
        final List<Map<String, T>> result = new ArrayList<Map<String, T>>();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            Map<String, T> item;
            Object value;
            if (header.size() == 0) {
                final ResultSetMetaData rsmd = rs.getMetaData();
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    header.add(rsmd.getColumnLabel(i + 1));
                }
            }
            final String cName = t.getCanonicalName();
            final boolean isTimestamp = "java.sql.Timestamp".equals(cName);
            final boolean isDouble = "java.lang.Double".equals(cName);
            final boolean isInteger = "java.lang.Integer".equals(cName);
            final boolean isFloat = "java.lang.Float".equals(cName);
            final boolean isShot = "java.lang.Short".equals(cName);
            final boolean isLong = "java.lang.Long".equals(cName);
            final boolean isBigDecimal = "class java.math.BigDecimal".equals(cName);
            final boolean isString = "java.lang.String".equals(cName);
            while (rs.next()) {
                item = new HashMap<String, T>();
                for (final String column : header) {
                    if (isTimestamp) value = rs.getTimestamp(column); else if (isString) {
                        value = StringsUtils.trimNull(rs.getString(column));
                    } else if (isDouble) value = rs.getDouble(column); else if (isInteger) value = rs.getInt(column); else if (isFloat) value = rs.getFloat(column); else if (isShot) value = rs.getShort(column); else if (isLong) value = rs.getLong(column); else if (isBigDecimal) value = rs.getBigDecimal(column); else value = rs.getObject(column);
                    if (null == value) value = t.newInstance();
                    item.put(column, t.cast(value));
                }
                result.add(item);
            }
        } catch (final Exception e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println(sql);
        } finally {
            closeAll(rs, stmt, null);
        }
        return result;
    }

    public static String getSql1(final Connection con, final String querySql) {
        final StringBuffer finalSql = new StringBuffer();
        DatabaseMetaData dmd = null;
        String dbName = null;
        try {
            dmd = con.getMetaData();
            dbName = dmd.getDatabaseProductName().toLowerCase();
            if (dbName.equals("oracle")) {
                finalSql.append(" SELECT * FROM (");
                finalSql.append(querySql);
                finalSql.append(" ) WHERE ROWNUM =1 ");
                return finalSql.toString();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * driver;url;username;password
	 * 
	 * @param connection
	 * @return
	 */
    public static Connection createConnection(final Properties properties) {
        if (null == properties) return null;
        final String driver = properties.getProperty("driver");
        final String url = properties.getProperty("url");
        String user = properties.getProperty("user");
        if (null == user) user = properties.getProperty("username");
        String pass = properties.getProperty("pass");
        if (null == pass) pass = properties.getProperty("password");
        try {
            Class.forName(driver);
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException("���ܼ���ָ������:" + driver);
        }
        try {
            return DriverManager.getConnection(url, user, pass);
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * ���?ͬ��ݿ�ؼ����ֶ�ʹ�� ���磺TOΪ��ݿ�ؼ��� Oralce��ݿ��˫���ʹ�ã�"TO" Mysql��ݿ��`TO`
	 * 
	 * @param text
	 * @param repl
	 * @param dsType ��ݿ�����
	 * @return
	 */
    public static String replaceKeywords(final String text, final String repl, final String dsType) {
        if ("mysql".equalsIgnoreCase(dsType)) {
            return StringsUtils.replace(text, repl, "`");
        } else if ("oracle".equalsIgnoreCase(dsType)) {
            return StringsUtils.replace(text, repl, "\"");
        }
        return text;
    }

    /**
	 * ��ݿ��б�׼��ʶ��ĳ���
	 */
    public static int MAXIDLENGTH = 30;

    /**
	 * �˷������ڳ��Ի�ȡһ����׼���ȵ��ַ���Ϊ��ݿ�ϵͳ�б�׼�ı�ʶ��,������ַ��ʱ����ȡ����Ĳ�����Ϊ��ʶ���
	 * 
	 * @param srcStr
	 * @return
	 */
    public static final String getStandardStr(final String srcStr) {
        if (srcStr.length() >= MAXIDLENGTH) {
            final String tmpStr = srcStr.substring(srcStr.length() - MAXIDLENGTH, srcStr.length());
            if (Character.isDigit(tmpStr.charAt(0)) || tmpStr.charAt(0) == '_') return StringsUtils.unionString("S", tmpStr.substring(1));
            return tmpStr;
        }
        return srcStr;
    }

    public static int MAX_IN_LENGTH = 900;

    @SuppressWarnings("unchecked")
    public static final <T> Collection<Collection<T>> splitForIN(final Collection<T> values) {
        final int size = values.size();
        final Collection<Collection<T>> result = new ArrayList<Collection<T>>();
        if (size <= MAX_IN_LENGTH) {
            result.add(values);
            return result;
        }
        final Object[] objs = values.toArray();
        Object[] swapArr;
        int idx = 0, tgtIdx, tgtLen;
        while (true) {
            if (idx >= size) break;
            tgtIdx = idx + MAX_IN_LENGTH;
            if (tgtIdx > size) tgtIdx = size;
            tgtLen = tgtIdx - idx;
            swapArr = new Object[tgtLen];
            System.arraycopy(objs, idx, swapArr, 0, tgtLen);
            result.add(new ArrayList<T>((Collection<? extends T>) Arrays.asList(swapArr)));
            idx = tgtIdx;
        }
        return result;
    }

    public static List<Map<String, Object>> getListFromSQL(final String sql, final Object[] values, final Connection conn) throws Exception {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = conn.prepareStatement(sql);
            if (values != null) {
                SQLUtils.buildStatement(st, values);
            }
            rs = st.executeQuery();
            final List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
            while (rs.next()) {
                final Map<String, Object> map = new HashMap<String, Object>();
                for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
                    map.put(rs.getMetaData().getColumnName(i + 1), rs.getObject(i + 1));
                }
                result.add(map);
            }
            return result;
        } finally {
            closeAll(rs, st, null);
        }
    }

    public static final <T> Map<String, Map<String, T>> executeQueryAsMap(final Connection conn, final String sql, final String keyColumn, final Class<T> t) {
        final Map<String, Map<String, T>> result = new HashMap<String, Map<String, T>>();
        if (StringsUtils.isBlank(sql)) return result;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            final String cName = t.getCanonicalName();
            final boolean isTimestamp = "java.sql.Timestamp".equals(cName);
            final boolean isDouble = "java.lang.Double".equals(cName);
            final boolean isInteger = "java.lang.Integer".equals(cName);
            final boolean isFloat = "java.lang.Float".equals(cName);
            final boolean isShot = "java.lang.Short".equals(cName);
            final boolean isLong = "java.lang.Long".equals(cName);
            final boolean isBigDecimal = "class java.math.BigDecimal".equals(cName);
            final boolean isString = "java.lang.String".equals(cName);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            final List<String> header = new ArrayList<String>();
            final ResultSetMetaData rsmd = rs.getMetaData();
            for (int i = 0; i < rsmd.getColumnCount(); i++) {
                header.add(rsmd.getColumnLabel(i + 1));
            }
            Map<String, T> item;
            Object value;
            while (rs.next()) {
                item = new HashMap<String, T>();
                for (final String column : header) {
                    if (keyColumn.equals(column)) continue;
                    if (isTimestamp) value = rs.getTimestamp(column); else if (isString) {
                        value = StringsUtils.trimNull(rs.getString(column));
                    } else if (isDouble) value = rs.getDouble(column); else if (isInteger) value = rs.getInt(column); else if (isFloat) value = rs.getFloat(column); else if (isShot) value = rs.getShort(column); else if (isLong) value = rs.getLong(column); else if (isBigDecimal) value = rs.getBigDecimal(column); else value = rs.getObject(column);
                    if (null == value) value = t.newInstance();
                    item.put(column, t.cast(value));
                }
                result.put(rs.getString(keyColumn), item);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            closeAll(rs, stmt, null);
        }
        return result;
    }

    public static final Map<String, Map<String, String>> executeQueryAsMap(final Connection conn, final String sql, final String keyColumn) {
        return executeQueryAsMap(conn, sql, keyColumn, String.class);
    }

    public static final <T> Map<String, List<Map<String, T>>> executeQueryAsGroupMap(final Connection conn, final String sql, final String keyColumn, final Class<T> t) {
        final Map<String, List<Map<String, T>>> result = new HashMap<String, List<Map<String, T>>>();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            final List<String> header = new ArrayList<String>();
            final ResultSetMetaData rsmd = rs.getMetaData();
            for (int i = 0; i < rsmd.getColumnCount(); i++) {
                header.add(rsmd.getColumnLabel(i + 1));
            }
            final String cName = t.getCanonicalName();
            final boolean isTimestamp = "java.sql.Timestamp".equals(cName);
            final boolean isDouble = "java.lang.Double".equals(cName);
            final boolean isInteger = "java.lang.Integer".equals(cName);
            final boolean isFloat = "java.lang.Float".equals(cName);
            final boolean isShot = "java.lang.Short".equals(cName);
            final boolean isLong = "java.lang.Long".equals(cName);
            final boolean isBigDecimal = "class java.math.BigDecimal".equals(cName);
            final boolean isString = "java.lang.String".equals(cName);
            String keyValue;
            Object value;
            List<Map<String, T>> item;
            Map<String, T> listItem;
            while (rs.next()) {
                keyValue = rs.getString(keyColumn);
                item = result.get(keyValue);
                if (null == item) {
                    item = new ArrayList<Map<String, T>>();
                    result.put(keyValue, item);
                }
                listItem = new HashMap<String, T>();
                for (final String column : header) {
                    if (column.equals(keyColumn)) continue;
                    if (isTimestamp) value = rs.getTimestamp(column); else if (isString) {
                        value = String.valueOf(rs.getString(column));
                    } else if (isDouble) value = rs.getDouble(column); else if (isInteger) value = rs.getInt(column); else if (isFloat) value = rs.getFloat(column); else if (isShot) value = rs.getShort(column); else if (isLong) value = rs.getLong(column); else if (isBigDecimal) value = rs.getBigDecimal(column); else value = rs.getObject(column);
                    if (null == value) value = t.newInstance();
                    listItem.put(column, t.cast(value));
                }
                item.add(listItem);
            }
        } catch (final Exception e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println(sql);
        } finally {
            closeAll(rs, stmt, null);
        }
        return result;
    }

    public static List<String> getTableLabels(final ResultSetMetaData metaData) {
        final List<String> result = new ArrayList<String>();
        try {
            if (metaData == null) {
                return result;
            }
            final int colsLen = metaData.getColumnCount() + 1;
            for (int i = 1; i < colsLen; i++) {
                result.add(metaData.getColumnLabel(i).toUpperCase());
            }
        } catch (final Exception e) {
        }
        return result;
    }

    public static List<String> getTableLabels(final JdbcTemplate jt, final String sql) {
        final List<String> result = new ArrayList<String>();
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = jt.getDataSource().getConnection();
            st = conn.createStatement();
            rs = st.executeQuery(sql);
            result.addAll(getTableLabels(rs.getMetaData()));
        } catch (final Exception e) {
        } finally {
            closeAll(rs, st, conn);
        }
        return result;
    }

    public static final Collection<String> syncTableStruct(final String tableName, final Connection connection, final Collection<String> newColumns, final String addAsType) throws SQLException {
        final Collection<String> addedColumns = new ArrayList<String>();
        final List<String> oldColumns = getTableLabels(tableName, connection);
        for (String newColumn : newColumns) {
            newColumn = newColumn.toUpperCase();
            if (oldColumns.contains(newColumn)) continue;
            addedColumns.add(newColumn);
        }
        if (addedColumns.isEmpty()) return addedColumns;
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            for (final String addCol : addedColumns) {
                stmt.addBatch(StringsUtils.u("ALTER TABLE ", tableName, " ADD ", addCol, " ", addAsType));
            }
            flushStatement(stmt);
        } catch (final SQLException sqle) {
            throw sqle;
        } finally {
            closeAll(null, stmt, null);
        }
        return addedColumns;
    }

    /**
	 * ��õ���ǰ׺Ϊ
	 * @param jt
	 * @return
	 * @throws SQLException
	 */
    public static final Collection<String> getTablesByPri(final Connection conn, final String tabPri) throws SQLException {
        final Collection<String> result = new ArrayList<String>();
        ResultSet rs = null;
        try {
            rs = conn.getMetaData().getTables(null, null, null, new String[] { "TABLE" });
            while (rs.next()) {
                final String tableName = rs.getString("TABLE_NAME");
                if (tableName.toUpperCase().startsWith(tabPri)) {
                    result.add(tableName);
                }
            }
        } finally {
            SQLUtils.closeAll(rs, null, null);
        }
        return result;
    }

    /**
	 * ��õ���ǰ׺Ϊ
	 * @param jt
	 * @return
	 * @throws SQLException
	 */
    public static final Collection<String> getTablesByPri(final JdbcTemplate jt, final String tabPri) throws SQLException {
        Connection conn = null;
        try {
            conn = jt.getDataSource().getConnection();
            return getTablesByPri(conn, tabPri);
        } finally {
            SQLUtils.closeAll(null, null, conn);
        }
    }
}
