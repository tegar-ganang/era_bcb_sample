package org.postgresql.test.jdbc2;

import org.postgresql.test.TestUtil;
import junit.framework.TestCase;
import java.sql.*;

public class ConnectionTest extends TestCase {

    private Connection con;

    public ConnectionTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        con = TestUtil.openDB();
        TestUtil.createTable(con, "test_a", "imagename name,image oid,id int4");
        TestUtil.createTable(con, "test_c", "source text,cost money,imageid int4");
        TestUtil.closeDB(con);
    }

    protected void tearDown() throws Exception {
        TestUtil.closeDB(con);
        con = TestUtil.openDB();
        TestUtil.dropTable(con, "test_a");
        TestUtil.dropTable(con, "test_c");
        TestUtil.closeDB(con);
    }

    public void testCreateStatement() throws Exception {
        con = TestUtil.openDB();
        Statement stat = con.createStatement();
        assertNotNull(stat);
        stat.close();
        stat = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        assertNotNull(stat);
        stat.close();
    }

    public void testPrepareStatement() throws Exception {
        con = TestUtil.openDB();
        String sql = "select source,cost,imageid from test_c";
        PreparedStatement stat = con.prepareStatement(sql);
        assertNotNull(stat);
        stat.close();
        stat = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        assertNotNull(stat);
        stat.close();
    }

    public void testPrepareCall() {
    }

    public void testNativeSQL() throws Exception {
        con = TestUtil.openDB();
        assertEquals("DATE  '2005-01-24'", con.nativeSQL("{d '2005-01-24'}"));
    }

    public void testTransactions() throws Exception {
        con = TestUtil.openDB();
        Statement st;
        ResultSet rs;
        con.setAutoCommit(false);
        assertTrue(!con.getAutoCommit());
        con.setAutoCommit(true);
        assertTrue(con.getAutoCommit());
        st = con.createStatement();
        st.executeUpdate("insert into test_a (imagename,image,id) values ('comttest',1234,5678)");
        con.setAutoCommit(false);
        st.executeUpdate("update test_a set image=9876 where id=5678");
        con.commit();
        rs = st.executeQuery("select image from test_a where id=5678");
        assertTrue(rs.next());
        assertEquals(9876, rs.getInt(1));
        rs.close();
        st.executeUpdate("update test_a set image=1111 where id=5678");
        con.rollback();
        rs = st.executeQuery("select image from test_a where id=5678");
        assertTrue(rs.next());
        assertEquals(9876, rs.getInt(1));
        rs.close();
        TestUtil.closeDB(con);
    }

    public void testIsClosed() throws Exception {
        con = TestUtil.openDB();
        assertTrue(!con.isClosed());
        TestUtil.closeDB(con);
        assertTrue(con.isClosed());
    }

    public void testWarnings() throws Exception {
        con = TestUtil.openDB();
        String testStr = "This Is OuR TeSt message";
        assertTrue(con instanceof org.postgresql.PGConnection);
        con.clearWarnings();
        ((org.postgresql.jdbc2.AbstractJdbc2Connection) con).addWarning(new SQLWarning(testStr));
        SQLWarning warning = con.getWarnings();
        assertNotNull(warning);
        assertEquals(testStr, warning.getMessage());
        con.clearWarnings();
        assertTrue(con.getWarnings() == null);
        TestUtil.closeDB(con);
    }

    public void testTransactionIsolation() throws Exception {
        con = TestUtil.openDB();
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
        con.setAutoCommit(false);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
        con.setAutoCommit(true);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
        con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, con.getTransactionIsolation());
        con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
        con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, con.getTransactionIsolation());
        con.setAutoCommit(false);
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, con.getTransactionIsolation());
        con.setAutoCommit(true);
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, con.getTransactionIsolation());
        con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
        con.setAutoCommit(false);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
        con.commit();
        con.getTransactionIsolation();
        con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        Statement stmt = con.createStatement();
        stmt.executeQuery("SELECT 1");
        stmt.close();
        try {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            fail("Expected an exception when changing transaction isolation mid-transaction");
        } catch (SQLException e) {
        }
        con.rollback();
        TestUtil.closeDB(con);
    }

    public void testTypeMaps() throws Exception {
        con = TestUtil.openDB();
        java.util.Map oldmap = con.getTypeMap();
        java.util.Map newmap = new java.util.HashMap();
        con.setTypeMap(newmap);
        assertEquals(newmap, con.getTypeMap());
        con.setTypeMap(oldmap);
        assertEquals(oldmap, con.getTypeMap());
        TestUtil.closeDB(con);
    }

    /**
     * Closing a Connection more than once is not an error.
     */
    public void testDoubleClose() throws Exception {
        con = TestUtil.openDB();
        con.close();
        con.close();
    }
}
