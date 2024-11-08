package pub.test;

import junit.framework.*;
import pub.db.*;
import pub.utils.Log;
import java.sql.*;

public class RollBackDataBaseTest extends DatabaseTestCase {

    private TermTable termdb;

    public void setUp() throws Exception {
        super.setUp();
        termdb = new TermTable(conn);
    }

    private void executeUpdate(String s) throws SQLException {
        Statement stmt = conn.createStatement();
        try {
            stmt.executeUpdate(s);
        } finally {
            stmt.close();
        }
    }

    public void testRollBackWithError() throws Exception {
        int countTerms = termdb.countNonobsoleteTerms();
        conn.setAutoCommit(false);
        try {
            executeUpdate("insert into pub_term (name ) values" + "(\"term_test\" ) ");
            executeUpdate("insert into pub_term (name2 ) values" + "(\"term3\" )");
        } catch (Exception e) {
            Log.getLogger(this.getClass()).debug(e);
            conn.rollback();
        } finally {
            conn.setAutoCommit(true);
        }
        assertEquals(countTerms, termdb.countNonobsoleteTerms());
    }

    public void testNoRollBack() throws Exception {
        try {
            executeUpdate("insert into pub_term (name ) values" + "(\"term_test\" ) ");
            executeUpdate("insert into pub_term (name2 ) values" + "(\"term3\" )");
        } catch (Exception e) {
        }
        assertEquals(1, termdb.countNonobsoleteTerms());
    }

    public void testRollBackWithoutError() throws Exception {
        conn.setAutoCommit(false);
        try {
            executeUpdate("insert into pub_term (name ) values" + "(\"term_test\" ) ");
            executeUpdate("insert into pub_term (name ) values" + "(\"term3\" )");
        } catch (Exception e) {
            conn.rollback();
        } finally {
            conn.setAutoCommit(true);
        }
        assertEquals(2, termdb.countNonobsoleteTerms());
    }
}
