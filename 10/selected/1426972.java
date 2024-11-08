package ranab;

import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.http.*;

/**
 * Counter servlet - the data is stored in hypersonic database.
 * Table structure
 * <pre>
 * CREATE TABLE COUNT_DATA (
 *   COUNT_PK INTEGER PRIMARY KEY,
 *   COUNT_NUM INTEGER NOT NULL
 * ) 
 * </pre>
 */
public class CounterServlet extends HttpServlet {

    /**
     * Read current count
     */
    private static final String READ_STATEMENT = "SELECT COUNT_NUM FROM COUNT_DATA WHERE COUNT_PK=1";

    /**
     * update count 
     */
    private static final String WRITE_STATEMENT = "UPDATE COUNT_DATA SET COUNT_NUM=? WHERE COUNT_PK=1";

    /**
     * JDBC Driver class
     */
    private static final String JDBC_DRIVER = "org.hsqldb.jdbcDriver";

    /**
     * JDBC URL
     */
    private static final String JDBC_URL = "jdbc:hsqldb:/members/QcGd6xgZqTV1bPr6wLc7KX4GpM4NgEMn/data/db/countDB";

    /**
     * Database user
     */
    private static final String JDBC_USER = "sa";

    /**
     * Database password
     */
    private static final String JDBC_PASSWORD = "";

    private Connection mConnection;

    private PreparedStatement mSelectStmt = null;

    private PreparedStatement mUpdateStmt = null;

    /**
     * Initialize database
     */
    public void init(ServletConfig servletconfig) throws ServletException {
        super.init(servletconfig);
        try {
            Class.forName(JDBC_DRIVER);
            mConnection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            mConnection.setAutoCommit(false);
            mSelectStmt = mConnection.prepareStatement(READ_STATEMENT);
            mUpdateStmt = mConnection.prepareStatement(WRITE_STATEMENT);
        } catch (Throwable th) {
            th.printStackTrace();
            throw new ServletException(th);
        }
    }

    /**
     * Send the current count data
     */
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/plain");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-control", "no-cache");
        PrintWriter os = response.getWriter();
        int count = readCount();
        os.print("c0=" + count);
        os.close();
    }

    /**
     * Read current count data and increment it
     */
    private synchronized int readCount() {
        int count = 0;
        try {
            ResultSet rs = mSelectStmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
            rs.close();
            mUpdateStmt.setInt(1, count + 1);
            mUpdateStmt.executeUpdate();
            mConnection.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                mConnection.rollback();
            } catch (Exception ex1) {
            }
            count = 0;
        }
        return count;
    }

    /**
     * Destroy servlet
     */
    public void destroy() {
        try {
            mSelectStmt.close();
        } catch (Throwable th) {
        }
        try {
            mUpdateStmt.close();
        } catch (Throwable th) {
        }
        try {
            mConnection.close();
        } catch (Throwable th) {
        }
        super.destroy();
    }

    public static String getStackTrace(Exception exception) {
        String s = "";
        try {
            StringWriter stringwriter = new StringWriter();
            PrintWriter printwriter = new PrintWriter(stringwriter);
            exception.printStackTrace(printwriter);
            printwriter.close();
            stringwriter.close();
            s = stringwriter.toString();
        } catch (Exception _ex) {
        }
        return s;
    }
}
