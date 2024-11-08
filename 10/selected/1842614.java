package com.dcivision.framework;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;

/**
  DataSourceFactory.java

  This class is the singleton to store the datasources used in the system.

    @author          Rollo Chan
    @company         DCIVision Limited
    @creation date   24/06/2003
    @version         $Revision: 1.31.2.1 $
*/
public class DataSourceFactory {

    public static final String REVISION = "$Revision: 1.31.2.1 $";

    public static final int DB_UNKNOWN = -1;

    public static final int DB_MYSQL = 1;

    public static final int DB_POSTGRESQL = 2;

    public static final int DB_SYBASE = 3;

    public static final int DB_MSSQL = 4;

    public static final int DB_ORACLE = 5;

    public static final int DB_INTERBASE = 6;

    public static final int DB_DB2 = 7;

    protected static java.util.Hashtable hashPkKeyLock = null;

    protected static javax.sql.DataSource masterDataSource = null;

    protected static Map externalDataSources = new HashMap();

    protected static org.apache.commons.logging.Log log = null;

    protected static int dbType = -1;

    /**
   * Constructor
   */
    private DataSourceFactory() {
    }

    /**
   * init
   *
   * @param inDataSource  Data Source object
   * @param inLog         Logger object
   */
    public static void init(DataSource inDataSource, Log inLog) {
        if (masterDataSource != null) {
            return;
        }
        hashPkKeyLock = new java.util.Hashtable();
        masterDataSource = inDataSource;
        log = inLog;
        Connection conn = null;
        try {
            conn = DataSourceFactory.getConnection();
            String dbName = "";
            if (conn != null) {
                dbName = conn.getMetaData().getDatabaseProductName();
            }
            log.info("Database Name: " + dbName);
            if ("MYSQL".equals(dbName.toUpperCase())) {
                dbType = DB_MYSQL;
            } else if ("MICROSOFT SQL SERVER".equals(dbName.toUpperCase())) {
                dbType = DB_MSSQL;
            } else if ("ORACLE".equals(dbName.toUpperCase())) {
                dbType = DB_ORACLE;
            }
        } catch (Exception ignore) {
            log.error("Cannot Get Database Type.", ignore);
        } finally {
            try {
                conn.close();
            } catch (Exception ignore) {
            } finally {
                conn = null;
            }
        }
    }

    public static void initExternalDataSource(String key, DataSource inDataSource) {
        if (externalDataSources.get(key) != null) {
            return;
        }
        externalDataSources.put(key, inDataSource);
    }

    public static synchronized Connection getConnection(String key) throws Exception {
        try {
            DataSource extDS = ((DataSource) externalDataSources.get(key));
            Connection conn = extDS == null ? null : extDS.getConnection();
            return (conn);
        } catch (Exception e) {
            log.error("Error during get connection.", e);
            throw e;
        }
    }

    /**
   * getConnection
   *
   * @return Connection from data source
   * @throws Exception
   */
    public static synchronized Connection getConnection() throws Exception {
        try {
            Connection conn = masterDataSource == null ? null : masterDataSource.getConnection();
            return (conn);
        } catch (Exception e) {
            log.error("Error during get connection.", e);
            throw e;
        }
    }

    /**
   * getDatabaseType
   *
   * @return  Database type
   */
    public static int getDatabaseType() {
        return (dbType);
    }

    /**
   * getNextSequence
   *
   * Normally, should call this function to generate the primary key(ID) in
   * DAO.
   *
   * @param seqNum  The sequence number name
   * @param conn    Database connection
   * @return        Next sequence number
   * @throws ApplicationException
   */
    public static synchronized Integer getNextSequence(String seqNum) throws ApplicationException {
        Connection dbConn = null;
        java.sql.PreparedStatement preStat = null;
        java.sql.ResultSet rs = null;
        boolean noTableMatchFlag = false;
        int currID = 0;
        int nextID = 0;
        try {
            dbConn = getConnection();
        } catch (Exception e) {
            log.error("Error Getting Connection.", e);
            throw new ApplicationException("errors.framework.db_conn", e);
        }
        synchronized (hashPkKeyLock) {
            if (hashPkKeyLock.get(seqNum) == null) {
                hashPkKeyLock.put(seqNum, new Object());
            }
        }
        synchronized (hashPkKeyLock.get(seqNum)) {
            synchronized (dbConn) {
                try {
                    preStat = dbConn.prepareStatement("SELECT TABLE_KEY_MAX FROM SYS_TABLE_KEY WHERE TABLE_NAME=?");
                    preStat.setString(1, seqNum);
                    rs = preStat.executeQuery();
                    if (rs.next()) {
                        currID = rs.getInt(1);
                    } else {
                        noTableMatchFlag = true;
                    }
                } catch (Exception e) {
                    log.error(e, e);
                    try {
                        dbConn.close();
                    } catch (Exception ignore) {
                    } finally {
                        dbConn = null;
                    }
                    throw new ApplicationException("errors.framework.get_next_seq", e, seqNum);
                } finally {
                    try {
                        rs.close();
                    } catch (Exception ignore) {
                    } finally {
                        rs = null;
                    }
                    try {
                        preStat.close();
                    } catch (Exception ignore) {
                    } finally {
                        preStat = null;
                    }
                }
                if (noTableMatchFlag) {
                    try {
                        currID = 0;
                        preStat = dbConn.prepareStatement("INSERT INTO SYS_TABLE_KEY(TABLE_NAME, TABLE_KEY_MAX) VALUES(?, ?)", java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE, java.sql.ResultSet.CONCUR_UPDATABLE);
                        preStat.setString(1, seqNum);
                        preStat.setInt(2, currID);
                        preStat.executeUpdate();
                    } catch (Exception e) {
                        log.error(e, e);
                        try {
                            dbConn.close();
                        } catch (Exception ignore) {
                        } finally {
                            dbConn = null;
                        }
                        throw new ApplicationException("errors.framework.get_next_seq", e, seqNum);
                    } finally {
                        try {
                            preStat.close();
                        } catch (Exception ignore) {
                        } finally {
                            preStat = null;
                        }
                    }
                }
                try {
                    int updateCnt = 0;
                    nextID = currID;
                    do {
                        nextID++;
                        preStat = dbConn.prepareStatement("UPDATE SYS_TABLE_KEY SET TABLE_KEY_MAX=? WHERE TABLE_NAME=? AND TABLE_KEY_MAX=?", java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE, java.sql.ResultSet.CONCUR_UPDATABLE);
                        preStat.setInt(1, nextID);
                        preStat.setString(2, seqNum);
                        preStat.setInt(3, currID);
                        updateCnt = preStat.executeUpdate();
                        currID++;
                        if (updateCnt == 0 && (currID % 2) == 0) {
                            Thread.sleep(50);
                        }
                    } while (updateCnt == 0);
                    dbConn.commit();
                    return (new Integer(nextID));
                } catch (Exception e) {
                    log.error(e, e);
                    try {
                        dbConn.rollback();
                    } catch (Exception ignore) {
                    }
                    throw new ApplicationException("errors.framework.get_next_seq", e, seqNum);
                } finally {
                    try {
                        preStat.close();
                    } catch (Exception ignore) {
                    } finally {
                        preStat = null;
                    }
                    try {
                        dbConn.close();
                    } catch (Exception ignore) {
                    } finally {
                        dbConn = null;
                    }
                }
            }
        }
    }

    /** format the given array of column names to a valid concatenated statement
   * under the corresponding database type, with the given separator inserted between column names
   * @param columnNames column names to be concatenate in the sql statement
   * @param separator separator appears between each column names
   * @return return the formatted sql statement
   */
    public static String formatSQLConcatStatement(String[] columnNames, String separator) {
        int nType = getDatabaseType();
        StringBuffer concatStat = new StringBuffer("");
        if ((columnNames != null) && (columnNames.length > 0)) {
            if (nType == DB_MSSQL) {
                concatStat.append("(");
                for (int i = 0; i < columnNames.length; i++) {
                    if (i != 0) {
                        concatStat.append("'" + separator + "'+");
                    }
                    concatStat.append("CAST(" + columnNames[i] + " AS varchar(4000)) +");
                }
                concatStat.delete(concatStat.length() - 1, concatStat.length());
                concatStat.append(")");
            } else if (nType == DB_ORACLE) {
                concatStat.append("(");
                for (int i = 0; i < columnNames.length; i++) {
                    if (i != 0) {
                        concatStat.append("'" + separator + "'||");
                    }
                    concatStat.append(columnNames[i] + "||");
                }
                concatStat.delete(concatStat.length() - 2, concatStat.length());
                concatStat.append(")");
            } else if (nType == DB_MYSQL) {
                concatStat.append("CONCAT_WS('" + separator + "',");
                for (int i = 0; i < columnNames.length; i++) {
                    concatStat.append((i == 0 ? "" : ",") + columnNames[i]);
                }
                concatStat.append(") ");
            }
        }
        return concatStat.toString();
    }

    /** format the given array of column names to a valid concatenated statement
   * under the corresponding database type, with a white space inserted between column names
   * @return return the formatted sql statement
   * @param columnNames column names to be concatenate in the sql statement
   */
    public static String formatSQLConcatStatement(String[] columnNames) {
        return formatSQLConcatStatement(columnNames, " ");
    }

    /**
   * Retrieve the datasource
   * @return the wrapped datasource instance
   */
    public static DataSource getDataSource() {
        return masterDataSource;
    }

    public static void main(String[] argv) {
        String[] str = { "A", "B", "C" };
        String separator = " ";
        String sResult = "";
        sResult = DataSourceFactory.formatSQLConcatStatement(str, separator);
        log.debug("1: " + sResult);
    }
}
