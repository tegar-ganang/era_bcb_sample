package calibration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBHelper {

    private static final String s_url = "jdbc:mysql://localhost:3306/";

    private static final String s_dbName = "calibration";

    private static final String s_userName = "root";

    private static final String s_password = "12345";

    private Connection conn = null;

    DBHelper() throws Exception {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        conn = DriverManager.getConnection(s_url + s_dbName, s_userName, s_password);
    }

    public void writeSonar(int distance, int angle, int seqn, int reading) throws SQLException {
        final String tableName = "sonar";
        Statement stmt = null;
        ResultSet rs = null;
        stmt = conn.createStatement();
        String whereClause = " WHERE distance=" + distance + " AND angle=" + angle + " AND seqn=" + seqn;
        String sql = "SELECT * FROM " + tableName + whereClause;
        stmt.executeQuery(sql);
        rs = stmt.getResultSet();
        if (rs.next()) {
            sql = "UPDATE " + tableName + " SET reading=" + reading + whereClause;
        } else {
            sql = "INSERT INTO " + tableName + " VALUES(" + distance + "," + angle + "," + seqn + "," + reading + ")";
        }
        stmt.executeUpdate(sql);
    }

    public void writeTranslation(int expected, int seqn, double actual) throws SQLException {
        writeOthers(expected, seqn, actual, "translation");
    }

    public void writeRotation(int expected, int seqn, double actual) throws SQLException {
        writeOthers(expected, seqn, actual, "rotation");
    }

    private void writeOthers(int expected, int seqn, double actual, String tableName) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        stmt = conn.createStatement();
        String where_clause = " WHERE expected=" + expected + " AND seqn=" + seqn;
        String sql = "SELECT * FROM " + tableName + where_clause;
        stmt.executeQuery(sql);
        rs = stmt.getResultSet();
        if (rs.next()) {
            sql = "UPDATE " + tableName + " SET actual=" + actual + where_clause;
        } else {
            sql = "INSERT INTO " + tableName + " VALUES(" + expected + "," + seqn + "," + actual + ")";
        }
        stmt.executeUpdate(sql);
    }

    public void close() throws SQLException {
        if (conn == null) {
            conn.close();
        }
    }
}
