package org.mortbay.webapps.jettyplus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DBTest {

    private static Log log = LogFactory.getLog(DBTest.class);

    public static final String COMMIT = "commit";

    public static final String ROLLBACK = "rollback";

    private static boolean initDone = false;

    private static DataSource xadatasource = null;

    private static DataSource datasource = null;

    private static DataSource pooledDataSource = null;

    private static Context context = null;

    private static String selectString = null;

    private static String updateString = null;

    private static void init() {
        try {
            if (context == null) {
                context = new InitialContext();
                log.info("<<< Retrieving environment >>>");
                selectString = (String) context.lookup("java:comp/env/select");
                updateString = (String) context.lookup("java:comp/env/update");
                log.info("<<< Environment retrieved >>>");
                log.info("<<< Retrieving XADataSource >>>");
                xadatasource = (DataSource) context.lookup("java:comp/env/jdbc/myDB");
                log.info("<<< XADataSource retrieved >>>");
                log.info("<<< Retrieving non-XADataSource >>>");
                datasource = (DataSource) context.lookup("java:comp/env/jdbc/myNonXADataSource");
                log.info("<<< DataSource retrieved:" + datasource + " >>>");
                log.info("<<< Retrieving pooled DataSource >>>");
                pooledDataSource = (DataSource) context.lookup("java:comp/env/jdbc/myPooledDataSource");
                log.info("<<< Pooled DataSource retrieved:" + pooledDataSource + " >>>");
                log.info("<<< Retrieving JNDI test value >>>");
                log.info("java:comp/env/my/trivial/name=" + (Integer) context.lookup("java:comp/env/my/trivial/name"));
                log.info("<<< Retrieved >>>");
            }
        } catch (Exception e) {
            log.error("init", e);
        }
    }

    public static void doItPooled() {
        Connection c = null;
        Statement s = null;
        int f;
        try {
            init();
            log.info("<<< performing update on pooled datasource >>>");
            c = pooledDataSource.getConnection();
            s = c.createStatement();
            s.executeUpdate("update testdata set foo=foo + 1 where id=1");
            c.commit();
            log.info("<<< update done >>>");
        } catch (Exception e) {
            log.error("doItPooled", e);
        } finally {
            try {
                s.close();
                c.close();
            } catch (Exception x) {
                log.error("Problem closing connection", x);
            }
        }
    }

    public static void doItNonXA() {
        Connection c = null;
        Statement s = null;
        int f;
        try {
            init();
            log.info("<<< performing update on non-XA datasource >>>");
            c = datasource.getConnection();
            s = c.createStatement();
            s.executeUpdate("update testdata set foo=foo + 1 where id=1");
            c.commit();
            log.info("<<< update done >>>");
        } catch (Exception e) {
            log.error("doItNonXA", e);
        } finally {
            try {
                s.close();
                c.close();
            } catch (Exception x) {
                log.error("Problem closing connection/statement", x);
            }
        }
    }

    /** 
     * @param action 
     */
    public static void doIt(String action) {
        int f = -1;
        Statement s = null;
        Connection connection = null;
        try {
            init();
            log.info("<<< Looking up UserTransaction >>>");
            UserTransaction usertransaction = (UserTransaction) context.lookup("java:comp/UserTransaction");
            log.info("<<< beginning the transaction >>>");
            usertransaction.begin();
            log.info("<<< Connecting to xadatasource >>>");
            connection = xadatasource.getConnection();
            log.info("<<< Connected >>>");
            s = connection.createStatement();
            s.executeUpdate("update testdata set foo=foo + 1 where id=1");
            if ((action != null) && action.equals("commit")) {
                log.info("<<< committing the transaction >>>");
                usertransaction.commit();
            } else {
                log.info("<<< rolling back the transaction >>>");
                usertransaction.rollback();
            }
            log.info("<<< transaction complete >>>");
        } catch (Exception e) {
            log.error("doIt", e);
        } finally {
            try {
                s.close();
                connection.close();
            } catch (Exception x) {
                log.error("problem closing statement/connection", x);
            }
        }
    }

    public static int readFoo() {
        init();
        return readFoo(datasource);
    }

    /** Read the value of foo from the db
     * @return 
     */
    private static int readFoo(DataSource ds) {
        int fooValue = -1;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultset = null;
        try {
            init();
            log.info("<<< Getting connection for reading foo >>>");
            connection = ds.getConnection();
            statement = connection.createStatement();
            resultset = statement.executeQuery(selectString);
            if (resultset.next()) {
                fooValue = resultset.getInt(2);
            } else log.warn("<<< No result from db read >>>");
            log.info("<<< Read foo from db = " + fooValue + " >>>");
            return fooValue;
        } catch (Exception e) {
            log.error("Problem reading value of foo", e);
            return fooValue;
        } finally {
            try {
                if (resultset != null) resultset.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (Exception e) {
                log.error("readFoo", e);
            }
        }
    }

    /** Get current value of foo
     * @return value of foo as a string
     */
    public String getFoo() {
        return String.valueOf(readFoo());
    }
}
