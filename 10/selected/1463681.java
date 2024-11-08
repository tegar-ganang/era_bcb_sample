package net.sourceforge.jtds.jdbc;

import java.sql.*;

/**
 * @version 1.0
 */
public class SavepointTest extends TestBase {

    public SavepointTest(String name) {
        super(name);
    }

    /**
     * tests if rolling back to a savepoint restores the correct DB state
     */
    public void testSavepoint1() throws Exception {
        Statement stmt = con.createStatement();
        stmt.execute("CREATE TABLE #savepoint1 (data int)");
        stmt.close();
        con.setAutoCommit(false);
        PreparedStatement pstmt = con.prepareStatement("INSERT INTO #savepoint1 (data) VALUES (?)");
        pstmt.setInt(1, 1);
        assertTrue(pstmt.executeUpdate() == 1);
        Savepoint savepoint = con.setSavepoint();
        assertNotNull(savepoint);
        assertTrue(savepoint.getSavepointId() == 1);
        try {
            savepoint.getSavepointName();
            assertTrue(false);
        } catch (SQLException e) {
        }
        pstmt.setInt(1, 2);
        assertTrue(pstmt.executeUpdate() == 1);
        pstmt.close();
        stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT SUM(data) FROM #savepoint1");
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) == 3);
        assertTrue(!rs.next());
        stmt.close();
        rs.close();
        con.rollback(savepoint);
        con.commit();
        stmt = con.createStatement();
        rs = stmt.executeQuery("SELECT SUM(data) FROM #savepoint1");
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) == 1);
        assertTrue(!rs.next());
        stmt.close();
        rs.close();
        con.setAutoCommit(true);
    }

    /**
     * rollback zu einem savepoint, dann test, ob weiter mit dem safepoint-objekt gearbeitet werden kann
     */
    public void testSavepoint2() throws Exception {
        String savepointName = "SAVEPOINT_1";
        Statement stmt = con.createStatement();
        stmt.execute("CREATE TABLE #savepoint2 (data int)");
        stmt.close();
        con.setAutoCommit(false);
        PreparedStatement pstmt = con.prepareStatement("INSERT INTO #savepoint2 (data) VALUES (?)");
        pstmt.setInt(1, 1);
        assertTrue(pstmt.executeUpdate() == 1);
        Savepoint savepoint = con.setSavepoint(savepointName);
        assertNotNull(savepoint);
        assertTrue(savepointName.equals(savepoint.getSavepointName()));
        try {
            savepoint.getSavepointId();
            assertTrue(false);
        } catch (SQLException e) {
        }
        pstmt.setInt(1, 2);
        assertTrue(pstmt.executeUpdate() == 1);
        pstmt.close();
        stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT SUM(data) FROM #savepoint2");
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) == 3);
        assertTrue(!rs.next());
        stmt.close();
        rs.close();
        con.rollback(savepoint);
        try {
            con.rollback(null);
            assertTrue(false);
        } catch (SQLException e) {
        }
        con.rollback(savepoint);
        try {
            con.releaseSavepoint(null);
            assertTrue(false);
        } catch (SQLException e) {
        }
        con.releaseSavepoint(savepoint);
        con.commit();
        stmt = con.createStatement();
        rs = stmt.executeQuery("SELECT SUM(data) FROM #savepoint2");
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) == 1);
        assertTrue(!rs.next());
        stmt.close();
        rs.close();
        con.setAutoCommit(true);
        try {
            con.setSavepoint();
            assertTrue(false);
        } catch (SQLException e) {
        }
        try {
            con.setSavepoint(savepointName);
            assertTrue(false);
        } catch (SQLException e) {
        }
    }

    public void testSavepoint3() throws Exception {
        Statement stmt = con.createStatement();
        stmt.execute("CREATE TABLE #savepoint3 (data int)");
        stmt.close();
        con.setAutoCommit(false);
        PreparedStatement pstmt = con.prepareStatement("INSERT INTO #savepoint3 (data) VALUES (?)");
        pstmt.setInt(1, 1);
        assertTrue(pstmt.executeUpdate() == 1);
        Savepoint savepoint1 = con.setSavepoint();
        assertNotNull(savepoint1);
        assertTrue(savepoint1.getSavepointId() == 1);
        pstmt.setInt(1, 2);
        assertTrue(pstmt.executeUpdate() == 1);
        Savepoint savepoint2 = con.setSavepoint();
        assertNotNull(savepoint2);
        assertTrue(savepoint2.getSavepointId() == 2);
        pstmt.setInt(1, 3);
        assertTrue(pstmt.executeUpdate() == 1);
        Savepoint savepoint3 = con.setSavepoint();
        assertNotNull(savepoint3);
        assertTrue(savepoint3.getSavepointId() == 3);
        pstmt.setInt(1, 4);
        assertTrue(pstmt.executeUpdate() == 1);
        pstmt.close();
        stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT SUM(data) FROM #savepoint3");
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) == 10);
        assertTrue(!rs.next());
        stmt.close();
        rs.close();
        con.releaseSavepoint(savepoint1);
        try {
            con.rollback(savepoint1);
            assertTrue(false);
        } catch (SQLException e) {
        }
        try {
            con.releaseSavepoint(savepoint1);
            assertTrue(false);
        } catch (SQLException e) {
        }
        con.rollback(savepoint2);
        con.rollback(savepoint2);
        con.releaseSavepoint(savepoint2);
        try {
            con.rollback(savepoint3);
            assertTrue(false);
        } catch (SQLException e) {
        }
        try {
            con.releaseSavepoint(savepoint3);
            assertTrue(false);
        } catch (SQLException e) {
        }
        con.commit();
        stmt = con.createStatement();
        rs = stmt.executeQuery("SELECT SUM(data) FROM #savepoint3");
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) == 3);
        assertTrue(!rs.next());
        stmt.close();
        rs.close();
        con.setAutoCommit(true);
    }

    /**
     * Test to ensure savepoint ids restart at 1. Also ensures that the
     * procedure cache is managed properly with savepoints.
     */
    public void testSavepoint4() throws Exception {
        Statement stmt = con.createStatement();
        stmt.execute("CREATE TABLE #savepoint4 (data int)");
        stmt.close();
        con.setAutoCommit(false);
        for (int i = 0; i < 3; i++) {
            System.out.println("iteration: " + i);
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO #savepoint4 (data) VALUES (?)");
            pstmt.setInt(1, 1);
            assertTrue(pstmt.executeUpdate() == 1);
            Savepoint savepoint = con.setSavepoint();
            assertNotNull(savepoint);
            assertTrue(savepoint.getSavepointId() == 1);
            try {
                savepoint.getSavepointName();
                assertTrue(false);
            } catch (SQLException e) {
            }
            pstmt.setInt(1, 2);
            assertTrue(pstmt.executeUpdate() == 1);
            pstmt.close();
            pstmt = con.prepareStatement("SELECT SUM(data) FROM #savepoint4");
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) == 3);
            assertTrue(!rs.next());
            pstmt.close();
            rs.close();
            con.rollback(savepoint);
            pstmt = con.prepareStatement("SELECT SUM(data) FROM #savepoint4");
            rs = pstmt.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) == 1);
            assertTrue(!rs.next());
            pstmt.close();
            rs.close();
            con.rollback();
        }
        con.setAutoCommit(true);
    }

    /**
     * Test to ensure savepoints can be created even when no statements have
     * been issued.
     */
    public void testSavepoint5() throws Exception {
        con.setAutoCommit(false);
        con.setSavepoint();
        con.rollback();
        con.setAutoCommit(true);
    }

    /**
     * test for bug [2818256]
     * 
     * ensure a savepoint is still valid after rollback
     */
    public void testSavepoint6() throws Exception {
        Statement stmt = con.createStatement();
        stmt.execute("CREATE TABLE #savepoint6 (data int)");
        stmt.close();
        con.setAutoCommit(false);
        PreparedStatement pstmt = con.prepareStatement("INSERT INTO #savepoint6 (data) VALUES (?)");
        pstmt.setInt(1, 1);
        assertTrue(pstmt.executeUpdate() == 1);
        Savepoint savepoint1 = con.setSavepoint();
        assertNotNull(savepoint1);
        assertTrue(savepoint1.getSavepointId() == 1);
        pstmt.setInt(1, 2);
        assertTrue(pstmt.executeUpdate() == 1);
        stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT SUM(data) FROM #savepoint6");
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) == 3);
        assertTrue(!rs.next());
        stmt.close();
        rs.close();
        con.rollback(savepoint1);
        stmt = con.createStatement();
        rs = stmt.executeQuery("SELECT SUM(data) FROM #savepoint6");
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) == 1);
        assertTrue(!rs.next());
        stmt.close();
        rs.close();
        pstmt.setInt(1, 2);
        assertTrue(pstmt.executeUpdate() == 1);
        con.rollback(savepoint1);
        stmt = con.createStatement();
        rs = stmt.executeQuery("SELECT SUM(data) FROM #savepoint6");
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) == 1);
        assertTrue(!rs.next());
        stmt.close();
        rs.close();
        con.setAutoCommit(true);
    }

    /**
     * roll back to one savepoint and ensure earlier savepoints are still valid
     */
    public void testSavepoint7() throws Exception {
        Statement stmt = con.createStatement();
        stmt.execute("CREATE TABLE #savepoint7 (data int)");
        stmt.close();
        con.setAutoCommit(false);
        PreparedStatement pstmt = con.prepareStatement("INSERT INTO #savepoint7 (data) VALUES (?)");
        pstmt.setInt(1, 1);
        assertTrue(pstmt.executeUpdate() == 1);
        Savepoint savepoint1 = con.setSavepoint();
        assertNotNull(savepoint1);
        assertTrue(savepoint1.getSavepointId() == 1);
        pstmt.setInt(1, 2);
        assertTrue(pstmt.executeUpdate() == 1);
        Savepoint savepoint2 = con.setSavepoint();
        assertNotNull(savepoint2);
        assertTrue(savepoint2.getSavepointId() == 2);
        pstmt.setInt(1, 3);
        assertTrue(pstmt.executeUpdate() == 1);
        Savepoint savepoint3 = con.setSavepoint();
        assertNotNull(savepoint3);
        assertTrue(savepoint3.getSavepointId() == 3);
        pstmt.setInt(1, 4);
        assertTrue(pstmt.executeUpdate() == 1);
        pstmt.close();
        stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT SUM(data) FROM #savepoint7");
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) == 10);
        assertTrue(!rs.next());
        stmt.close();
        rs.close();
        con.rollback(savepoint3);
        stmt = con.createStatement();
        rs = stmt.executeQuery("SELECT SUM(data) FROM #savepoint7");
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) == 6);
        assertTrue(!rs.next());
        stmt.close();
        rs.close();
        con.rollback(savepoint1);
        stmt = con.createStatement();
        rs = stmt.executeQuery("SELECT SUM(data) FROM #savepoint7");
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) == 1);
        assertTrue(!rs.next());
        stmt.close();
        rs.close();
        con.setAutoCommit(true);
    }

    /**
     * Test to ensure savepoints cannot be created in auto-commit mode
     * (Bug [2021839]).
     */
    public void testSavepoint8() {
        try {
            con.setSavepoint();
            assertTrue(false);
        } catch (SQLException e) {
        }
    }

    /**
     * Test for bug [2021839], connection is rolled back instead of being
     * committed if setSavepoint is the first operation.
     */
    public void testSavepoint9() throws Exception {
        Statement stmt = con.createStatement();
        stmt.execute("CREATE TABLE #savepoint9 (data int)");
        stmt.close();
        con.setAutoCommit(false);
        Savepoint sp = con.setSavepoint();
        PreparedStatement pstmt = con.prepareStatement("INSERT INTO #savepoint9 (data) VALUES (?)");
        pstmt.setInt(1, 1);
        assertTrue(pstmt.executeUpdate() == 1);
        pstmt.close();
        stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT SUM(data) FROM #savepoint9");
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) == 1);
        assertTrue(!rs.next());
        stmt.close();
        rs.close();
        con.commit();
        con.rollback();
        stmt = con.createStatement();
        rs = stmt.executeQuery("SELECT SUM(data) FROM #savepoint9");
        assertTrue(rs.next());
        assertTrue("bug [2021839]", rs.getInt(1) == 1);
        assertTrue(!rs.next());
        stmt.close();
        rs.close();
        con.setAutoCommit(true);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SavepointTest.class);
    }
}
