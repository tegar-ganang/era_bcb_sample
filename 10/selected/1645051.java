package cn.myapps.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import javax.sql.DataSource;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import cn.myapps.base.dao.PersistenceUtils;
import cn.myapps.core.deploy.application.ejb.ApplicationProcess;
import cn.myapps.core.deploy.application.ejb.ApplicationVO;
import cn.myapps.core.dynaform.document.dql.DB2SQLFunction;
import cn.myapps.core.dynaform.document.dql.HsqldbSQLFunction;
import cn.myapps.core.dynaform.document.dql.MssqlSQLFunction;
import cn.myapps.core.dynaform.document.dql.MysqlSQLFunction;
import cn.myapps.core.dynaform.document.dql.OracleSQLFunction;
import cn.myapps.core.dynaform.document.dql.SQLFunction;
import cn.myapps.core.dynaform.form.ddlutil.SQLBuilder;
import cn.myapps.core.table.model.Column;
import cn.myapps.core.table.model.Table;

public class DbTypeUtil {

    public static final Logger log = Logger.getLogger(DbTypeUtil.class);

    public static final String DBTYPE_ORACLE = "ORACLE";

    public static final String DBTYPE_MSSQL = "MSSQL";

    public static final String DBTYPE_MYSQL = "MYSQL";

    public static final String DBTYPE_HSQLDB = "HSQLDB";

    public static final String DBTYPE_DB2 = "DB2";

    private static HashMap _dbTypes = new HashMap();

    public static Collection getTableNames(String applicationId) {
        Collection rtn = new ArrayList();
        Connection conn = null;
        ResultSet tableSet = null;
        try {
            conn = getConnection(applicationId);
            String catalog = null;
            String schemaPattern = null;
            String dbType = getDBType(applicationId);
            String schema = getSchema(conn, dbType);
            if (dbType.equals(DBTYPE_ORACLE)) {
                schemaPattern = schema;
            } else if (dbType.equals(DBTYPE_MSSQL)) {
                schemaPattern = schema;
            } else if (dbType.equals(DBTYPE_MYSQL)) {
                catalog = schema;
            } else if (dbType.equals(DBTYPE_HSQLDB)) {
                schemaPattern = schema;
            } else if (dbType.equals(DBTYPE_DB2)) {
                schemaPattern = schema;
            }
            DatabaseMetaData metaData = conn.getMetaData();
            tableSet = metaData.getTables(catalog, schemaPattern, null, new String[] { "TABLE" });
            while (tableSet.next()) {
                String tableName = tableSet.getString(3);
                rtn.add(tableName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    tableSet.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return rtn;
    }

    /**
	 * 
	 * @param applicationId
	 *            应用ID
	 * @return
	 */
    public static Collection getTables(String applicationId) {
        return getTables(null, applicationId);
    }

    /**
	 * 
	 * @param tableName
	 *            数据表名称
	 * @param applicationId
	 *            应用ID
	 * @return 表集合
	 */
    public static Collection getTables(String tableName, String applicationId) {
        Collection rtn = new ArrayList();
        Connection conn = null;
        ResultSet tableSet = null;
        try {
            conn = getConnection(applicationId);
            String catalog = null;
            String schemaPattern = null;
            String dbType = getDBType(applicationId);
            String schema = getSchema(conn, dbType);
            if (dbType.equals(DBTYPE_ORACLE)) {
                schemaPattern = schema;
            } else if (dbType.equals(DBTYPE_MSSQL)) {
                schemaPattern = schema;
            } else if (dbType.equals(DBTYPE_MYSQL)) {
                catalog = schema;
            } else if (dbType.equals(DBTYPE_HSQLDB)) {
                schemaPattern = schema;
            } else if (dbType.equals(DBTYPE_DB2)) {
                schemaPattern = schema;
            }
            DatabaseMetaData metaData = conn.getMetaData();
            tableSet = metaData.getTables(catalog, schemaPattern, tableName, new String[] { "TABLE" });
            while (tableSet.next()) {
                tableName = tableSet.getString(3);
                Table table = new Table(tableName.toUpperCase());
                ResultSet columnSet = metaData.getColumns(catalog, schemaPattern, tableName, null);
                try {
                    while (columnSet.next()) {
                        String name = columnSet.getString(4);
                        int typeCode = columnSet.getInt(5);
                        Column column = new Column("", name.toUpperCase(), typeCode);
                        table.getColumns().add(column);
                    }
                } catch (Exception e) {
                    throw e;
                } finally {
                    columnSet.close();
                }
                rtn.add(table);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    tableSet.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return rtn;
    }

    /**
	 * 
	 * @param tableName
	 *            数据表名称
	 * @param applicationId
	 *            应用ID
	 * @return 数据库表
	 */
    public static Table getTable(String tableName, String applicationId) {
        Collection tables = getTables(tableName, applicationId);
        if (tables != null && !tables.isEmpty()) {
            return (Table) tables.iterator().next();
        }
        return null;
    }

    /**
	 * Get the connection
	 * 
	 * @return the connection
	 * @throws Exception
	 */
    protected static Connection getConnection(String applicationid) throws Exception {
        ApplicationProcess process = (ApplicationProcess) ProcessFactory.createProcess(ApplicationProcess.class);
        ApplicationVO appvo = (ApplicationVO) process.doView(applicationid);
        if (appvo != null) {
            String username = appvo.getDbusername();
            String password = appvo.getDbpassword();
            String driver = appvo.getDbdriver();
            String url = appvo.getDburl();
            String poolsize = !StringUtils.isBlank(appvo.getDbpoolsize()) ? appvo.getDbpoolsize() : "10";
            String timeout = !StringUtils.isBlank(appvo.getDbtimeout()) ? appvo.getDbtimeout() : "5000";
            DataSource dataSource = PersistenceUtils.getDBCPDataSource(username, password, driver, url, poolsize, timeout);
            return dataSource.getConnection();
        }
        return null;
    }

    public int executeBatch(String[] commands, String applicationid) throws Exception {
        Statement statement = null;
        int errors = 0;
        int commandCount = 0;
        Connection conn = null;
        try {
            conn = getConnection(applicationid);
            conn.setAutoCommit(false);
            statement = conn.createStatement();
            for (int i = 0; i < commands.length; i++) {
                String command = commands[i];
                if (command.trim().length() == 0) {
                    continue;
                }
                commandCount++;
                try {
                    log.info("executing SQL: " + command);
                    int results = statement.executeUpdate(command);
                    log.info("After execution, " + results + " row(s) have been changed");
                } catch (SQLException ex) {
                    throw ex;
                }
            }
            conn.commit();
            log.info("Executed " + commandCount + " SQL command(s) with " + errors + " error(s)");
        } catch (SQLException ex) {
            if (conn != null) {
                conn.rollback();
            }
            throw ex;
        } catch (Exception e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            statement.close();
        }
        return errors;
    }

    public int executeBatch(String sql, String applicationid) throws Exception {
        String[] commands = sql.split(SQLBuilder.SQL_DELIMITER);
        return executeBatch(commands, applicationid);
    }

    public static SQLFunction getSQLFunction(String applicationId) throws Exception {
        String dbType = getDBType(applicationId);
        if (dbType.equals(DBTYPE_ORACLE)) {
            return new OracleSQLFunction();
        } else if (dbType.equals(DBTYPE_MSSQL)) {
            return new MssqlSQLFunction();
        } else if (dbType.equals(DBTYPE_MYSQL)) {
            return new MysqlSQLFunction();
        } else if (dbType.equals(DBTYPE_HSQLDB)) {
            return new HsqldbSQLFunction();
        } else if (dbType.equals(DBTYPE_DB2)) {
            return new DB2SQLFunction();
        }
        return null;
    }

    public static String getDBType(String applicationId) throws Exception {
        String rtn = null;
        if (applicationId != null) {
            rtn = (String) _dbTypes.get(applicationId);
        }
        if (rtn == null) {
            ApplicationProcess process = (ApplicationProcess) ProcessFactory.createProcess(ApplicationProcess.class);
            ApplicationVO appvo = (ApplicationVO) process.doView(applicationId);
            rtn = appvo.getDbtype();
            _dbTypes.put(applicationId, rtn);
        }
        return rtn;
    }

    public static String getSchema(Connection conn, String dbType) {
        if (dbType.equals(DBTYPE_ORACLE) || dbType.equals(DBTYPE_DB2)) {
            try {
                return conn.getMetaData().getUserName().trim().toUpperCase();
            } catch (SQLException sqle) {
                return "";
            }
        } else if (dbType.equals(DBTYPE_MYSQL)) {
            try {
                String schema = conn.getMetaData().getURL().trim().toUpperCase();
                if (schema.indexOf("?USE") > 0) {
                    schema = schema.substring(schema.lastIndexOf("/") + 1, schema.indexOf("?USE"));
                } else {
                    schema = schema.substring(schema.lastIndexOf("/") + 1);
                }
                return schema;
            } catch (SQLException sqle) {
                return "";
            }
        } else if (dbType.equals(DBTYPE_MSSQL)) {
            try {
                ResultSet rs = conn.getMetaData().getSchemas();
                if (rs != null) {
                    if (rs.next()) return rs.getString(1).trim().toUpperCase();
                }
            } catch (SQLException sqle) {
                return "";
            }
        } else if (dbType.equals(DBTYPE_HSQLDB)) {
            return "public".toUpperCase();
        }
        return "";
    }

    /**
	 * 取得application中的username作为schema为单独创建sql
	 * 
	 * @param applicationId
	 * @return
	 * @throws Exception
	 */
    public static String getSchema(String applicationId) throws Exception {
        ApplicationProcess process = (ApplicationProcess) ProcessFactory.createProcess(ApplicationProcess.class);
        ApplicationVO appvo = (ApplicationVO) process.doView(applicationId);
        String dbType = appvo.getDbtype();
        if (dbType.equals(DBTYPE_ORACLE) || dbType.equals(DBTYPE_DB2)) {
            return appvo.getDbusername() != null ? appvo.getDbusername().toUpperCase() : null;
        } else if (dbType.equals(DBTYPE_MYSQL)) {
            String schema = appvo.getDburl().trim().toUpperCase();
            if (schema.indexOf("?USE") > 0) {
                schema = schema.substring(schema.lastIndexOf("/") + 1, schema.indexOf("?USE"));
            } else {
                schema = schema.substring(schema.lastIndexOf("/") + 1);
            }
            return schema;
        } else if (dbType.equals(DBTYPE_MSSQL)) {
            return "DBO";
        } else if (dbType.equals(DBTYPE_HSQLDB)) {
            return "public".toUpperCase();
        }
        return "";
    }

    public static synchronized void remove(String application) {
        _dbTypes.remove(application);
    }

    public static void main(String[] args) {
        String tableName = "tlk_fm_dayoff_copy27";
        String applicationId = "01b98ff4-8d8c-b3c0-8d30-ece2aa60d534";
        try {
            Table table = getTable(tableName, applicationId);
            System.out.println(table.getColumns());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
