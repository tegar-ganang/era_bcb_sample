package satmule.persistence;

import java.sql.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** utility class that is a Broker intermediary between database */
public class Broker {

    static Log log = LogFactory.getLog(FpFileName.class);

    /** realices a update, delete or insert int the database*/
    private static int ejecutaUpdate(String database, String SQL) throws Exception {
        int i = 0;
        DBConnectionManager dbm = null;
        Connection bd = null;
        try {
            dbm = DBConnectionManager.getInstance();
            bd = dbm.getConnection(database);
            Statement st = bd.createStatement();
            i = st.executeUpdate(SQL);
            bd.commit();
            st.close();
            dbm.freeConnection(database, bd);
        } catch (Exception e) {
            log.error("SQL error: " + SQL, e);
            Exception excep;
            if (dbm == null) excep = new Exception("Could not obtain pool object DbConnectionManager"); else if (bd == null) excep = new Exception("The Db connection pool could not obtain a database connection"); else {
                bd.rollback();
                excep = new Exception("SQL Error: " + SQL + " error: " + e);
                dbm.freeConnection(database, bd);
            }
            throw excep;
        }
        return i;
    }

    /** makes a insert into de database */
    public static boolean insert(String database, String SQL) throws Exception {
        int i = 0;
        i = ejecutaUpdate(database, SQL);
        return (i == 0) ? false : true;
    }

    /** makes a update into de database */
    public static int update(String database, String SQL) throws Exception {
        int x = ejecutaUpdate(database, SQL);
        if (x == 0) throw new Exception("No registers jet updated in call to Broker.update method");
        return x;
    }

    /** delete sql from the database. */
    public static int delete(String database, String SQL) throws Exception {
        int x = ejecutaUpdate(database, SQL);
        if (x == 0) throw new Exception("No se ha borrado ning�n registro");
        return x;
    }

    /** select consult to the database. 
     * Dont forget to make to the resultSet returned a 
     * resultSet.getStatement.close()
     * so that cursors are properly closed 
     * */
    public static ResultSet select(String database, String SQL) throws Exception {
        ResultSet Res = null;
        DBConnectionManager dbm = null;
        Connection bd = null;
        try {
            dbm = DBConnectionManager.getInstance();
            bd = dbm.getConnection(database);
            Res = bd.createStatement().executeQuery(SQL);
            dbm.freeConnection(database, bd);
        } catch (Exception e) {
            log.error("SQL error: " + SQL, e);
            Exception excep;
            if (dbm == null) excep = new Exception("Could not obtain pool object DbConnectionManager"); else if (bd == null) excep = new Exception("The Db connection pool could not obtain a database connection"); else {
                excep = new Exception("SQL Error : " + SQL + " error: " + e);
                dbm.freeConnection(database, bd);
            }
            throw excep;
        }
        return Res;
    }

    /** eject a prepared statement in the database */
    public static PreparedStatement prepareStatement(String database, String SQL) throws Exception {
        PreparedStatement res = null;
        DBConnectionManager dbm = null;
        Connection bd = null;
        try {
            dbm = DBConnectionManager.getInstance();
            bd = dbm.getConnection(database);
            res = bd.prepareStatement(SQL);
            res.execute();
            bd.commit();
            dbm.freeConnection(database, bd);
        } catch (Exception e) {
            log.error("SQL error: " + SQL, e);
            Exception excep;
            if (dbm == null) excep = new Exception("Could not obtain pool object DbConnectionManager"); else if (bd == null) excep = new Exception("The Db connection pool could not obtain a database connection"); else {
                bd.rollback();
                excep = new Exception("SQL Error : " + SQL + " error: " + e);
                dbm.freeConnection(database, bd);
            }
            throw excep;
        }
        return res;
    }
}
