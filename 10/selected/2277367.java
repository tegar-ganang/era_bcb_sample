package freetds;

import java.sql.*;
import java.math.BigDecimal;
import com.internetcds.jdbc.tds.Tds;
import com.internetcds.util.Logger;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.AssertionFailedError;

/**
 * test getting timestamps from the database.
 *
 */
public class TimestampTest extends DatabaseTestCase {

    public TimestampTest(String name) {
        super(name);
    }

    public static void main(String args[]) {
        boolean loggerActive = args.length > 0;
        try {
            Logger.setActive(loggerActive);
        } catch (java.io.IOException ex) {
            throw new RuntimeException("Unexpected Exception " + ex + " occured in main");
        }
        if (args.length > 0) {
            junit.framework.TestSuite s = new TestSuite();
            for (int i = 0; i < args.length; i++) {
                s.addTest(new TimestampTest(args[i]));
            }
            junit.textui.TestRunner.run(s);
        } else junit.textui.TestRunner.run(TimestampTest.class);
    }

    public void testBigint0000() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0000");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0000 " + "  (i  bigint  not null, " + "   s  char(10) not null) ");
        final int rowsToAdd = 20;
        int count = 0;
        for (int i = 1; i <= rowsToAdd; i++) {
            String sql = "insert into #t0000 values (" + i + ", 'row" + i + "')";
            count += stmt.executeUpdate(sql);
        }
        assertTrue(count == rowsToAdd);
        PreparedStatement pStmt = cx.prepareStatement("select i from #t0000 where i = ?");
        pStmt.setLong(1, 7);
        ResultSet rs = pStmt.executeQuery();
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        assertTrue(rs.getLong(1) == 7);
        assertTrue("Expected no result set", !rs.next());
        pStmt.setLong(1, 8);
        rs = pStmt.executeQuery();
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        assertTrue(rs.getLong(1) == 8);
        assertTrue("Expected no result set", !rs.next());
    }

    public void testTimestamps0001() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0001");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0001             " + "  (t1 datetime not null,       " + "   t2 datetime null,           " + "   t3 smalldatetime not null,  " + "   t4 smalldatetime null)");
        PreparedStatement pStmt = cx.prepareStatement("insert into #t0001 values (?, '1998-03-09 15:35:06.4',        " + "                          ?, '1998-03-09 15:35:00')");
        Timestamp t0 = new Timestamp(98, 2, 9, 15, 35, 6, 400000000);
        Timestamp t1 = new Timestamp(98, 2, 9, 15, 35, 0, 0);
        pStmt.setTimestamp(1, t0);
        pStmt.setTimestamp(2, t1);
        int count = pStmt.executeUpdate();
        assertTrue(count == 1);
        pStmt = cx.prepareStatement("select t1, t2, t3, t4 from #t0001");
        ResultSet rs = stmt.executeQuery("select t1, t2, t3, t4 from #t0001");
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        assertEquals(t0, rs.getTimestamp(1));
        assertEquals(t0, rs.getTimestamp(2));
        assertEquals(t1, rs.getTimestamp(3));
        assertEquals(t1, rs.getTimestamp(4));
    }

    public void testTimestamps0004() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0004");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0004 " + "  (mytime  datetime not null, " + "   mytime2 datetime null,     " + "   mytime3 datetime null     )");
        PreparedStatement pStmt = cx.prepareStatement("insert into #t0004 values ('1998-09-09 15:35:05', ?, ?)");
        Timestamp t0 = new Timestamp(98, 8, 9, 15, 35, 5, 0);
        pStmt.setTimestamp(1, t0);
        pStmt.setTimestamp(2, t0);
        int count = pStmt.executeUpdate();
        pStmt.setNull(2, java.sql.Types.TIMESTAMP);
        count = pStmt.executeUpdate();
        pStmt = cx.prepareStatement("select mytime, mytime2, mytime3 from #t0004");
        ResultSet rs = pStmt.executeQuery();
        assertNotNull(rs);
        Timestamp t1, t2, t3;
        assertTrue("Expected a result set", rs.next());
        t1 = rs.getTimestamp(1);
        t2 = rs.getTimestamp(2);
        t3 = rs.getTimestamp(3);
        assertEquals(t1, t2);
        assertEquals(t1, t3);
        assertEquals(t2, t3);
        assertTrue("Expected a result set", rs.next());
        t1 = rs.getTimestamp(1);
        t2 = rs.getTimestamp(2);
        t3 = rs.getTimestamp(3);
        assertEquals(t1, t2);
        assertTrue(t1 != t3);
        assertTrue(t2 != t3);
    }

    public void testEscape(String sql, String expected) throws Exception {
        String tmp = Tds.toNativeSql(sql, Tds.SQLSERVER);
        assertEquals(tmp, expected);
    }

    public void testEscapes0006() throws Exception {
        testEscape("select * from tmp where d={d 1999-09-19}", "select * from tmp where d='19990919'");
        testEscape("select * from tmp where d={d '1999-09-19'}", "select * from tmp where d='19990919'");
        testEscape("select * from tmp where t={t 12:34:00}", "select * from tmp where t='12:34:00'");
        testEscape("select * from tmp where ts={ts 1998-12-15 12:34:00.1234}", "select * from tmp where ts='19981215 12:34:00.123'");
        testEscape("select * from tmp where ts={ts 1998-12-15 12:34:00}", "select * from tmp where ts='19981215 12:34:00.000'");
        testEscape("select * from tmp where ts={ts 1998-12-15 12:34:00.1}", "select * from tmp where ts='19981215 12:34:00.100'");
        testEscape("select * from tmp where ts={ts 1998-12-15 12:34:00}", "select * from tmp where ts='19981215 12:34:00.000'");
        testEscape("select * from tmp where d={d 1999-09-19}", "select * from tmp where d='19990919'");
        testEscape("select * from tmp where a like '\\%%'", "select * from tmp where a like '\\%%'");
        testEscape("select * from tmp where a like 'b%%' {escape 'b'}", "select * from tmp where a like '\\%%' ");
        testEscape("select * from tmp where a like 'bbb' {escape 'b'}", "select * from tmp where a like 'bbb' ");
        testEscape("select * from tmp where a='{fn user}'", "select * from tmp where a='{fn user}'");
        testEscape("select * from tmp where a={fn user()}", "select * from tmp where a= user_name() ");
    }

    public void testPreparedStatement0007() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0007");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0007 " + "  (i  integer  not null, " + "   s  char(10) not null) ");
        final int rowsToAdd = 20;
        int count = 0;
        for (int i = 1; i <= rowsToAdd; i++) {
            String sql = "insert into #t0007 values (" + i + ", 'row" + i + "')";
            count += stmt.executeUpdate(sql);
        }
        assertTrue(count == rowsToAdd);
        PreparedStatement pStmt = cx.prepareStatement("select s from #t0007 where i = ?");
        pStmt.setInt(1, 7);
        ResultSet rs = pStmt.executeQuery();
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        assertEquals(rs.getString(1).trim(), "row7");
        pStmt.setInt(1, 8);
        rs = pStmt.executeQuery();
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        assertEquals(rs.getString(1).trim(), "row8");
        assertTrue("Expected no result set", !rs.next());
    }

    public void testPreparedStatement0008() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0008");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0008              " + "  (i  integer  not null,      " + "   s  char(10) not null)      ");
        PreparedStatement pStmt = cx.prepareStatement("insert into #t0008 values (?, ?)");
        final int rowsToAdd = 8;
        final String theString = "abcdefghijklmnopqrstuvwxyz";
        int count = 0;
        for (int i = 1; i <= rowsToAdd; i++) {
            pStmt.setInt(1, i);
            pStmt.setString(2, theString.substring(0, i));
            count += pStmt.executeUpdate();
        }
        assertTrue(count == rowsToAdd);
        ResultSet rs = stmt.executeQuery("select s, i from #t0008");
        assertNotNull(rs);
        count = 0;
        while (rs.next()) {
            count++;
            assertTrue(rs.getString(1).trim().length() == rs.getInt(2));
        }
        assertTrue(count == rowsToAdd);
    }

    public void testPreparedStatement0009() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0009");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0009 " + "  (i  integer  not null,      " + "   s  char(10) not null)      ");
        cx.setAutoCommit(false);
        PreparedStatement pStmt = cx.prepareStatement("insert into #t0009 values (?, ?)");
        int rowsToAdd = 8;
        final String theString = "abcdefghijklmnopqrstuvwxyz";
        int count = 0;
        for (int i = 1; i <= rowsToAdd; i++) {
            pStmt.setInt(1, i);
            pStmt.setString(2, theString.substring(0, i));
            count += pStmt.executeUpdate();
        }
        assertTrue(count == rowsToAdd);
        cx.rollback();
        stmt = cx.createStatement();
        ResultSet rs = stmt.executeQuery("select s, i from #t0009");
        assertNotNull(rs);
        count = 0;
        while (rs.next()) {
            count++;
            assertTrue(rs.getString(1).trim().length() == rs.getInt(2));
        }
        assertTrue(count == 0);
        cx.commit();
        rowsToAdd = 6;
        count = 0;
        for (int i = 1; i <= rowsToAdd; i++) {
            pStmt.setInt(1, i);
            pStmt.setString(2, theString.substring(0, i));
            count += pStmt.executeUpdate();
        }
        assertTrue(count == rowsToAdd);
        cx.commit();
        rs = stmt.executeQuery("select s, i from #t0009");
        count = 0;
        while (rs.next()) {
            count++;
            assertTrue(rs.getString(1).trim().length() == rs.getInt(2));
        }
        assertTrue(count == rowsToAdd);
        cx.commit();
        cx.setAutoCommit(true);
    }

    public void testTransactions0010() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0010");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0010 " + "  (i  integer  not null,      " + "   s  char(10) not null)      ");
        cx.setAutoCommit(false);
        PreparedStatement pStmt = cx.prepareStatement("insert into #t0010 values (?, ?)");
        int rowsToAdd = 8;
        final String theString = "abcdefghijklmnopqrstuvwxyz";
        int count = 0;
        for (int i = 1; i <= rowsToAdd; i++) {
            pStmt.setInt(1, i);
            pStmt.setString(2, theString.substring(0, i));
            count += pStmt.executeUpdate();
        }
        assertTrue(count == rowsToAdd);
        cx.rollback();
        stmt = cx.createStatement();
        ResultSet rs = stmt.executeQuery("select s, i from #t0010");
        assertNotNull(rs);
        count = 0;
        while (rs.next()) {
            count++;
            assertTrue(rs.getString(1).trim().length() == rs.getInt(2));
        }
        assertTrue(count == 0);
        cx.commit();
        rowsToAdd = 6;
        for (int j = 1; j <= 2; j++) {
            count = 0;
            for (int i = 1; i <= rowsToAdd; i++) {
                pStmt.setInt(1, i + ((j - 1) * rowsToAdd));
                pStmt.setString(2, theString.substring(0, i));
                count += pStmt.executeUpdate();
            }
            assertTrue(count == rowsToAdd);
            cx.commit();
        }
        rs = stmt.executeQuery("select s, i from #t0010");
        count = 0;
        while (rs.next()) {
            count++;
            int i = rs.getInt(2);
            if (i > rowsToAdd) i -= rowsToAdd;
            assertTrue(rs.getString(1).trim().length() == i);
        }
        assertTrue(count == (2 * rowsToAdd));
        cx.commit();
        cx.setAutoCommit(true);
    }

    public void testEmptyResults0011() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0011");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0011 " + "  (mytime  datetime not null, " + "   mytime2 datetime null     )");
        ResultSet rs = stmt.executeQuery("select mytime, mytime2 from #t0011");
        assertNotNull(rs);
        assertTrue("Expected no result set", !rs.next());
        stmt.close();
        stmt = cx.createStatement();
        rs = stmt.executeQuery("select mytime, mytime2 from #t0011");
        assertTrue("Expected no result set", !rs.next());
        stmt.close();
    }

    public void testEmptyResults0012() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0012");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0012 " + "  (mytime  datetime not null, " + "   mytime2 datetime null     )");
        PreparedStatement pStmt = cx.prepareStatement("select mytime, mytime2 from #t0012");
        ResultSet rs = pStmt.executeQuery();
        assertNotNull(rs);
        assertTrue("Expected no result set", !rs.next());
        rs.close();
        rs = pStmt.executeQuery();
        assertTrue("Expected no result set", !rs.next());
        pStmt.close();
    }

    public void testEmptyResults0013() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0013");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0013 " + "  (mytime  datetime not null, " + "   mytime2 datetime null     )");
        PreparedStatement pStmt = cx.prepareStatement("select mytime, mytime2 from #t0013");
        ResultSet rs1 = stmt.executeQuery("select mytime, mytime2 from #t0013");
        assertNotNull(rs1);
        ResultSet rs2 = pStmt.executeQuery();
        assertNotNull(rs2);
        assertTrue("Expected no result set", !rs1.next());
        assertTrue("Expected no result set", !rs2.next());
    }

    public void testForBrowse0014() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0014");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0014 (i integer not null)");
        PreparedStatement pStmt = cx.prepareStatement("insert into #t0014 values (?)");
        final int rowsToAdd = 100;
        int count = 0;
        for (int i = 1; i <= rowsToAdd; i++) {
            pStmt.setInt(1, i);
            count += pStmt.executeUpdate();
        }
        assertTrue(count == rowsToAdd);
        pStmt = cx.prepareStatement("select i from #t0014 for browse");
        ResultSet rs = pStmt.executeQuery();
        assertNotNull(rs);
        count = 0;
        while (rs.next()) {
            int n = rs.getInt("i");
            count++;
        }
        assertTrue(count == rowsToAdd);
        rs = stmt.executeQuery("select * from #t0014");
        assertNotNull(rs);
        count = 0;
        while (rs.next()) {
            int n = rs.getInt("i");
            count++;
        }
        assertTrue(count == rowsToAdd);
        rs = stmt.executeQuery("select * from #t0014");
        assertNotNull(rs);
        count = 0;
        while (rs.next() && count < 5) {
            int n = rs.getInt("i");
            count++;
        }
        assertTrue(count == 5);
        rs = stmt.executeQuery("select * from #t0014");
        assertNotNull(rs);
        count = 0;
        while (rs.next()) {
            int n = rs.getInt("i");
            count++;
        }
        assertTrue(count == rowsToAdd);
    }

    public void testMultipleResults0015() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0015");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0015 " + "  (i  integer  not null,      " + "   s  char(10) not null)      ");
        PreparedStatement pStmt = cx.prepareStatement("insert into #t0015 values (?, ?)");
        int rowsToAdd = 8;
        final String theString = "abcdefghijklmnopqrstuvwxyz";
        int count = 0;
        for (int i = 1; i <= rowsToAdd; i++) {
            pStmt.setInt(1, i);
            pStmt.setString(2, theString.substring(0, i));
            count += pStmt.executeUpdate();
        }
        assertTrue(count == rowsToAdd);
        stmt = cx.createStatement();
        ResultSet rs = stmt.executeQuery("select s from #t0015 select i from #t0015");
        assertNotNull(rs);
        count = 0;
        while (rs.next()) {
            count++;
        }
        assertTrue(count == rowsToAdd);
        assertTrue(stmt.getMoreResults());
        rs = stmt.getResultSet();
        assertNotNull(rs);
        count = 0;
        while (rs.next()) {
            count++;
        }
        assertTrue(count == rowsToAdd);
        rs = stmt.executeQuery("select i, s from #t0015");
        count = 0;
        while (rs.next()) {
            count++;
        }
        assertTrue(count == rowsToAdd);
        cx.close();
    }

    public void testMissingParameter0016() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0016");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0016 " + "  (i  integer  not null,      " + "   s  char(10) not null)      ");
        stmt = cx.createStatement();
        final int rowsToAdd = 20;
        int count = 0;
        for (int i = 1; i <= rowsToAdd; i++) {
            String sql = "insert into #t0016 values (" + i + ", 'row" + i + "')";
            count += stmt.executeUpdate(sql);
        }
        assertTrue(count == rowsToAdd);
        PreparedStatement pStmt = cx.prepareStatement("select s from #t0016 where i=? and s=?");
        try {
            ResultSet rs = pStmt.executeQuery();
            assertTrue("Failed to throw exception", false);
        } catch (SQLException e) {
            assertTrue((e.getMessage().equals("parameter #1 has not been set") || e.getMessage().equals("parameter #2 has not been set")));
        }
        pStmt.clearParameters();
        try {
            pStmt.setInt(1, 7);
            pStmt.setString(2, "row7");
            pStmt.clearParameters();
            ResultSet rs = pStmt.executeQuery();
            assertTrue("Failed to throw exception", false);
        } catch (SQLException e) {
            assertTrue((e.getMessage().equals("parameter #1 has not been set") || e.getMessage().equals("parameter #2 has not been set")));
        }
        pStmt.clearParameters();
        try {
            pStmt.setInt(1, 7);
            ResultSet rs = pStmt.executeQuery();
            assertTrue("Failed to throw exception", false);
        } catch (SQLException e) {
            assertTrue(e.getMessage().equals("parameter #2 has not been set"));
        }
        pStmt.clearParameters();
        try {
            pStmt.setString(2, "row7");
            ResultSet rs = pStmt.executeQuery();
            assertTrue("Failed to throw exception", false);
        } catch (SQLException e) {
            assertTrue(e.getMessage().equals("parameter #1 has not been set"));
        }
    }

    Object[][] getDatatypes() {
        return new Object[][] { { "float(6)", "65.4321", new BigDecimal("65.4321") }, { "binary(5)", "0x1213141516", new byte[] { 0x12, 0x13, 0x14, 0x15, 0x16 } }, { "varbinary(4)", "0x1718191A", new byte[] { 0x17, 0x18, 0x19, 0x1A } }, { "varchar(8)", "'12345678'", new String("12345678") }, { "datetime", "'19990815 21:29:59.01'", new Timestamp(99, 7, 15, 21, 29, 59, 10000000) }, { "smalldatetime", "'19990215 20:45'", new Timestamp(99, 1, 15, 20, 45, 0, 0) }, { "float(6)", "65.4321", new Float(65.4321) }, { "float(14)", "1.123456789", new Double(1.123456789) }, { "real", "7654321.0", new Double(7654321.0) }, { "int", "4097", new Integer(4097) }, { "float(6)", "65.4321", new BigDecimal("65.4321") }, { "float(14)", "1.123456789", new BigDecimal("1.123456789") }, { "decimal(10,3)", "1234567.089", new BigDecimal("1234567.089") }, { "numeric(5,4)", "1.2345", new BigDecimal("1.2345") }, { "smallint", "4094", new Short((short) 4094) }, { "smallint", "127", new Byte((byte) 127) }, { "smallint", "-128", new Byte((byte) -128) }, { "money", "19.95", new BigDecimal("19.95") }, { "smallmoney", "9.97", new BigDecimal("9.97") }, { "bit", "1", Boolean.TRUE }, { "image", "0x0a0a0b", new byte[] { 0x0a, 0x0a, 0x0b } } };
    }

    public void testOutputParams() throws Exception {
        Connection cx = getConnection();
        Statement stmt = cx.createStatement();
        dropProcedure("#freetds_outputTest");
        Object[][] datatypes = getDatatypes();
        for (int i = 0; i < datatypes.length; i++) {
            String valueToAssign;
            if (datatypes[i][0].equals("image")) valueToAssign = ""; else valueToAssign = " = " + datatypes[i][1];
            String sql = "create procedure #freetds_outputTest " + "@a1 " + datatypes[i][0] + " = null out " + "as select @a1 " + valueToAssign;
            stmt.executeUpdate(sql);
            for (int pass = 0; pass < 2; pass++) {
                CallableStatement cstmt = cx.prepareCall("{call #freetds_outputTest[(?)]}");
                int jtype = getType(datatypes[i][2]);
                if (pass == 1) cstmt.setObject(1, null, jtype, 10);
                if (jtype == java.sql.Types.NUMERIC || jtype == java.sql.Types.DECIMAL) {
                    cstmt.registerOutParameter(1, jtype, 10);
                    if (pass == 0) cstmt.setObject(1, datatypes[i][2], jtype, 10);
                } else if (jtype == java.sql.Types.VARCHAR) {
                    cstmt.registerOutParameter(1, jtype, 2000);
                    if (pass == 0) cstmt.setObject(1, datatypes[i][2]);
                } else {
                    cstmt.registerOutParameter(1, jtype);
                    if (pass == 0) cstmt.setObject(1, datatypes[i][2]);
                }
                if (!cstmt.execute()) while (cstmt.getUpdateCount() != -1 && cstmt.getMoreResults()) ;
                if (jtype == java.sql.Types.VARBINARY) {
                    assertTrue(compareBytes(cstmt.getBytes(1), (byte[]) datatypes[i][2]) == 0);
                } else if (datatypes[i][2] instanceof Number) {
                    Number n = (Number) cstmt.getObject(1);
                    assertEquals("Failed on " + datatypes[i][0], ((Number) cstmt.getObject(1)).doubleValue(), ((Number) datatypes[i][2]).doubleValue(), 0.001);
                } else {
                    assertEquals("Failed on " + datatypes[i][0], cstmt.getObject(1), datatypes[i][2]);
                }
            }
            stmt.executeUpdate(" drop procedure #freetds_outputTest");
        }
    }

    public void testDatatypes0017() throws Exception {
        Connection cx = getConnection();
        Statement stmt = cx.createStatement();
        dropTable("#t0017");
    }

    public void testStatements0020() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0020a");
        dropTable("#t0020b");
        dropTable("#t0020c");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0020a ( " + "  i1   int not null,     " + "  s1   char(10) not null " + ")                        " + "");
        stmt.executeUpdate("create table #t0020b ( " + "  i2a   int not null,     " + "  i2b   int not null,     " + "  s2   char(10) not null " + ")                        " + "");
        stmt.executeUpdate("create table #t0020c ( " + "  i3   int not null,     " + "  s3   char(10) not null " + ")                        " + "");
        int nextB = 1;
        int nextC = 1;
        for (int i = 1; i < 500; i++) {
            stmt.executeUpdate("insert into #t0020a " + "  values(" + i + ", " + "         'row" + i + "') " + "");
            for (int j = nextB; (nextB % 5) != 0; j++, nextB++) {
                stmt.executeUpdate("insert into #t0020b " + " values(" + i + ", " + "        " + j + ", " + "        'row" + i + "." + j + "' " + "        )" + "");
                for (int k = nextC; (nextC % 3) != 0; k++, nextC++) {
                    stmt.executeUpdate("insert into #t0020c " + " values(" + j + ", " + "        'row" + i + "." + j + "." + k + "' " + "        )" + "");
                }
            }
        }
        Statement stmtA = cx.createStatement();
        PreparedStatement stmtB = cx.prepareStatement("select i2b, s2 from #t0020b where i2a=?");
        PreparedStatement stmtC = cx.prepareStatement("select s3 from #t0020c where i3=?");
        ResultSet rs1 = stmtA.executeQuery("select i1 from #t0020a");
        assertNotNull(rs1);
        while (rs1.next()) {
            int i1 = rs1.getInt("i1");
            stmtB.setInt(1, rs1.getInt("i1"));
            ResultSet rs2 = stmtB.executeQuery();
            assertNotNull(rs2);
            while (rs2.next()) {
                stmtC.setInt(1, rs2.getInt(1));
                ResultSet rs3 = stmtC.executeQuery();
                assertNotNull(rs3);
                if (rs3.next()) {
                }
            }
        }
    }

    public void testBlob0021() throws Exception {
        byte smallarray[] = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10 };
        byte array1[] = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };
        String bigtext1 = "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "abcdefghijklmnop" + "";
        Connection cx = getConnection();
        Statement stmt = cx.createStatement();
        dropTable("#t0021");
        stmt.executeUpdate("create table #t0021 ( " + " mybinary         binary(16) not null, " + " myimage          image not null, " + " mynullimage      image null, " + " mytext           text not null, " + " mynulltext       text null) ");
        PreparedStatement insert = cx.prepareStatement("insert into #t0021(     " + " mybinary,             " + " myimage,              " + " mynullimage,          " + " mytext,               " + " mynulltext            " + ")                      " + "values(?, ?, ?, ?, ?)  ");
        insert.setBytes(1, smallarray);
        insert.setBytes(2, array1);
        insert.setBytes(3, array1);
        insert.setString(4, "abcd");
        insert.setString(5, "defg");
        int count = insert.executeUpdate();
        assertTrue(count == 1);
        ResultSet rs = stmt.executeQuery("select * from #t0021");
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        byte[] a1 = rs.getBytes("myimage");
        byte[] a2 = rs.getBytes("mynullimage");
        assertTrue(compareBytes(a1, array1) == 0);
        assertTrue(compareBytes(a2, array1) == 0);
    }

    public void testNestedStatements0022() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0022a");
        dropTable("#t0022b");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0022a " + "  (i   integer not null, " + "   str char(254) not null) ");
        stmt.executeUpdate("create table #t0022b             " + "  (i   integer not null,      " + "   t   datetime not null)     ");
        PreparedStatement pStmtA = cx.prepareStatement("insert into #t0022a values (?, ?)");
        PreparedStatement pStmtB = cx.prepareStatement("insert into #t0022b values (?, getdate())");
        final int rowsToAdd = 1000;
        int count = 0;
        for (int i = 1; i <= rowsToAdd; i++) {
            pStmtA.setInt(1, i);
            String tmp = "";
            while (tmp.length() < 240) {
                tmp = tmp + "row " + i + ". ";
            }
            pStmtA.setString(2, tmp);
            count += pStmtA.executeUpdate();
            pStmtB.setInt(1, i);
            pStmtB.executeUpdate();
        }
        assertTrue(count == rowsToAdd);
        Statement stmtA = cx.createStatement();
        Statement stmtB = cx.createStatement();
        count = 0;
        ResultSet rsA = stmtA.executeQuery("select * from #t0022a");
        assertNotNull(rsA);
        while (rsA.next()) {
            count++;
            ResultSet rsB = stmtB.executeQuery("select * from #t0022b where i=" + rsA.getInt("i"));
            assertNotNull(rsB);
            assertTrue("Expected a result set", rsB.next());
            assertTrue("Expected no result set", !rsB.next());
        }
        assertTrue(count == rowsToAdd);
    }

    public void testPrimaryKeyFloat0023() throws Exception {
        Double d[] = { new Double(-1.0), new Double(1234.543), new Double(0.0), new Double(1), new Double(-2.0), new Double(0.14), new Double(0.79), new Double(1000000.12345), new Double(-1000000.12345), new Double(1000000), new Double(-1000000), new Double(1.7E+308), new Double(1.7E-307) };
        Connection cx = getConnection();
        dropTable("#t0023");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("" + "create table #t0023 " + "  (pk   float not null, " + "   type char(30) not null, " + "   b    bit, " + "   str  char(30) not null, " + "   t int identity(1,1), " + "   primary key (pk, type))    ");
        PreparedStatement pStmt = cx.prepareStatement("insert into #t0023 (pk, type, b, str) values(?, 'prepared', 0, ?)");
        for (int i = 0; i < d.length; i++) {
            pStmt.setDouble(1, d[i].doubleValue());
            pStmt.setString(2, (d[i]).toString());
            int preparedCount = pStmt.executeUpdate();
            assertTrue(preparedCount == 1);
            int adhocCount = stmt.executeUpdate("" + "insert into #t0023        " + " (pk, type, b, str)      " + " values(" + "   " + d[i] + ",         " + "       'adhoc',          " + "       1,                " + "   '" + d[i] + "')       ");
            assertTrue(adhocCount == 1);
        }
        int count = 0;
        ResultSet rs = stmt.executeQuery("select * from #t0023 where type='prepared' order by t");
        assertNotNull(rs);
        while (rs.next()) {
            assertEquals(d[count].toString(), "" + rs.getDouble("pk"));
            count++;
        }
        assertTrue(count == d.length);
        count = 0;
        rs = stmt.executeQuery("select * from #t0023 where type='adhoc' order by t");
        while (rs.next()) {
            assertEquals(d[count].toString(), "" + rs.getDouble("pk"));
            count++;
        }
        assertTrue(count == d.length);
    }

    public void testPrimaryKeyReal0024() throws Exception {
        Float d[] = { new Float(-1.0), new Float(1234.543), new Float(0.0), new Float(1), new Float(-2.0), new Float(0.14), new Float(0.79), new Float(1000000.12345), new Float(-1000000.12345), new Float(1000000), new Float(-1000000), new Float(3.4E+38), new Float(3.4E-38) };
        Connection cx = getConnection();
        dropTable("#t0024");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("" + "create table #t0024                  " + "  (pk   real not null,             " + "   type char(30) not null,          " + "   b    bit,                        " + "   str  char(30) not null,          " + "   t int identity(1,1), " + "    primary key (pk, type))    ");
        PreparedStatement pStmt = cx.prepareStatement("insert into #t0024 (pk, type, b, str) values(?, 'prepared', 0, ?)");
        for (int i = 0; i < d.length; i++) {
            pStmt.setFloat(1, d[i].floatValue());
            pStmt.setString(2, (d[i]).toString());
            int preparedCount = pStmt.executeUpdate();
            assertTrue(preparedCount == 1);
            int adhocCount = stmt.executeUpdate("" + "insert into #t0024        " + " (pk, type, b, str)      " + " values(" + "   " + d[i] + ",         " + "       'adhoc',          " + "       1,                " + "   '" + d[i] + "')       ");
            assertTrue(adhocCount == 1);
        }
        int count = 0;
        ResultSet rs = stmt.executeQuery("select * from #t0024 where type='prepared' order by t");
        assertNotNull(rs);
        while (rs.next()) {
            String s1 = d[count].toString().trim();
            String s2 = ("" + rs.getFloat("pk")).trim();
            assertTrue(s1.equalsIgnoreCase(s2));
            count++;
        }
        assertTrue(count == d.length);
        count = 0;
        rs = stmt.executeQuery("select * from #t0024 where type='adhoc' order by t");
        while (rs.next()) {
            String s1 = d[count].toString().trim();
            String s2 = ("" + rs.getFloat("pk")).trim();
            assertTrue(s1.equalsIgnoreCase(s2));
            count++;
        }
        assertTrue(count == d.length);
    }

    public void testGetBoolean0025() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0025");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0025 " + "  (i      integer, " + "   b      bit,     " + "   s      char(5), " + "   f      float)   ");
        assertTrue(stmt.executeUpdate("insert into #t0025 values(0, 0, 'false', 0.0)") == 1);
        assertTrue(stmt.executeUpdate("insert into #t0025 values(0, 0, 'N', 0.0)") == 1);
        assertTrue(stmt.executeUpdate("insert into #t0025 values(1, 1, 'true', 7.0)") == 1);
        assertTrue(stmt.executeUpdate("insert into #t0025 values(2, 1, 'Y', -5.0)") == 1);
        ResultSet rs = stmt.executeQuery("select * from #t0025 order by i");
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        assertTrue(!rs.getBoolean("i"));
        assertTrue(!rs.getBoolean("b"));
        assertTrue(!rs.getBoolean("s"));
        assertTrue(!rs.getBoolean("f"));
        assertTrue("Expected a result set", rs.next());
        assertTrue(!rs.getBoolean("i"));
        assertTrue(!rs.getBoolean("b"));
        assertTrue(!rs.getBoolean("s"));
        assertTrue(!rs.getBoolean("f"));
        assertTrue("Expected a result set", rs.next());
        assertTrue(rs.getBoolean("i"));
        assertTrue(rs.getBoolean("b"));
        assertTrue(rs.getBoolean("s"));
        assertTrue(rs.getBoolean("f"));
        assertTrue("Expected a result set", rs.next());
        assertTrue(rs.getBoolean("i"));
        assertTrue(rs.getBoolean("b"));
        assertTrue(rs.getBoolean("s"));
        assertTrue(rs.getBoolean("f"));
        assertTrue("Expected no result set", !rs.next());
    }

    public void testErrors0036() throws Exception {
        Connection cx = getConnection();
        Statement stmt = cx.createStatement();
        final int numberToTest = 5;
        for (int i = 0; i < numberToTest; i++) {
            String table = "#t0036_do_not_create_" + i;
            try {
                stmt.executeUpdate("drop table " + table);
                assertTrue("Did not expect to reach here", false);
            } catch (SQLException e) {
                assertTrue(e.getMessage().startsWith("Cannot drop the table '" + table + "', because it does"));
            }
        }
    }

    public void testTimestamps0037() throws Exception {
        Connection cx = getConnection();
        Statement stmt = cx.createStatement();
        ResultSet rs = stmt.executeQuery("select                                    " + "  convert(smalldatetime, '1999-01-02') a, " + "  convert(smalldatetime, null)         b, " + "  convert(datetime, '1999-01-02')      c, " + "  convert(datetime, null)              d  " + "");
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        assertTrue(rs.getDate("a") != null);
        assertTrue(rs.getDate("b") == null);
        assertTrue(rs.getDate("c") != null);
        assertTrue(rs.getDate("d") == null);
        assertTrue(rs.getTime("a") != null);
        assertTrue(rs.getTime("b") == null);
        assertTrue(rs.getTime("c") != null);
        assertTrue(rs.getTime("d") == null);
        assertTrue(rs.getTimestamp("a") != null);
        assertTrue(rs.getTimestamp("b") == null);
        assertTrue(rs.getTimestamp("c") != null);
        assertTrue(rs.getTimestamp("d") == null);
        assertTrue("Expected no result set", !rs.next());
    }

    public void testConnection0039() throws Exception {
        for (int i = 0; i < 2000; i++) {
            Connection conn = getConnection();
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery("select 5");
            assertNotNull(resultSet);
            resultSet.close();
            statement.close();
            conn.close();
        }
    }

    public void testPreparedStatement0040() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0040");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0040 (" + " c255 char(255)     not null, " + " v255 varchar(255)  not null) ");
        PreparedStatement pStmt1 = cx.prepareStatement("insert into #t0040 values (?, ?)");
        PreparedStatement pStmt2 = cx.prepareStatement("select c255, v255 from #t0040 order by c255");
        String along = getLongString('a');
        String blong = getLongString('b');
        pStmt1.setString(1, along);
        pStmt1.setString(2, along);
        int count = pStmt1.executeUpdate();
        assertTrue(count == 1);
        count = stmt.executeUpdate("" + "insert into #t0040 values ( " + "'" + blong + "', " + "'" + blong + "')");
        assertTrue(count == 1);
        ResultSet rs = pStmt2.executeQuery();
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        assertEquals(rs.getString("c255"), along);
        assertEquals(rs.getString("v255"), along);
        assertTrue("Expected a result set", rs.next());
        assertEquals(rs.getString("c255"), blong);
        assertEquals(rs.getString("v255"), blong);
        assertTrue("Expected no result set", !rs.next());
        rs = stmt.executeQuery("select c255, v255 from #t0040 order by c255");
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        assertEquals(rs.getString("c255"), along);
        assertEquals(rs.getString("v255"), along);
        assertTrue("Expected a result set", rs.next());
        assertEquals(rs.getString("c255"), blong);
        assertEquals(rs.getString("v255"), blong);
        assertTrue("Expected no result set", !rs.next());
    }

    public void testPreparedStatement0041() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0041");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0041 " + "  (i  integer  not null, " + "   s  text     not null) ");
        PreparedStatement pStmt = cx.prepareStatement("insert into #t0041 values (?, ?)");
        final int rowsToAdd = 400;
        final String theString = getLongString(400);
        int count = 0;
        for (int i = 1; i <= rowsToAdd; i++) {
            pStmt.setInt(1, i);
            pStmt.setString(2, theString.substring(0, i));
            count += pStmt.executeUpdate();
        }
        assertTrue(count == rowsToAdd);
        ResultSet rs = stmt.executeQuery("select s, i from #t0041");
        assertNotNull(rs);
        count = 0;
        while (rs.next()) {
            rs.getString("s");
            count++;
        }
        assertTrue(count == rowsToAdd);
    }

    public void testPreparedStatement0042() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0042");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0042 (s char(5) null, i integer null, j integer not null)");
        PreparedStatement pStmt1 = cx.prepareStatement("insert into #t0042 (s, i, j) values (?, ?, ?)");
        PreparedStatement pStmt2 = cx.prepareStatement("select i from #t0042 order by j");
        pStmt1.setString(1, "hello");
        pStmt1.setNull(2, java.sql.Types.INTEGER);
        pStmt1.setInt(3, 1);
        int count = pStmt1.executeUpdate();
        assertTrue(count == 1);
        pStmt1.setInt(2, 42);
        pStmt1.setInt(3, 2);
        count = pStmt1.executeUpdate();
        assertTrue(count == 1);
        ResultSet rs = pStmt2.executeQuery();
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        rs.getInt(1);
        assertTrue(rs.wasNull());
        assertTrue("Expected a result set", rs.next());
        assertTrue(rs.getInt(1) == 42);
        assertTrue(!rs.wasNull());
        assertTrue("Expected no result set", !rs.next());
    }

    public void testResultSet0043() throws Exception {
        Connection cx = getConnection();
        Statement stmt = cx.createStatement();
        try {
            ResultSet rs = stmt.executeQuery("select 1");
            assertNotNull(rs);
            rs.getInt(1);
            assertTrue("Did not expect to reach here", false);
        } catch (SQLException e) {
            assertTrue(e.getMessage().startsWith("No current row in the result set"));
        }
    }

    public void testResultSet0044() throws Exception {
        Connection cx = getConnection();
        Statement stmt = cx.createStatement();
        ResultSet rs = stmt.executeQuery("select 1");
        assertNotNull(rs);
        rs.close();
        try {
            assertTrue("Expected no result set", !rs.next());
        } catch (SQLException e) {
            assertTrue(e.getMessage().startsWith("result set is cl"));
        }
    }

    public void testResultSet0045() throws Exception {
        try {
            Connection cx = getConnection();
            Statement stmt = cx.createStatement();
            ResultSet rs = stmt.executeQuery("select 1");
            assertNotNull(rs);
            assertTrue("Expected a result set", rs.next());
            rs.getInt(1);
            assertTrue("Expected no result set", !rs.next());
            rs.getInt(1);
            assertTrue("Did not expect to reach here", false);
        } catch (java.sql.SQLException e) {
            assertTrue(e.getMessage().startsWith("No current row in the result set"));
        }
    }

    public void testMetaData0046() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0046");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0046 (" + "   i integer identity, " + "   a integer not null, " + "   b integer null ) ");
        int count = stmt.executeUpdate("insert into #t0046 (a, b) values (-2, -3)");
        assertTrue(count == 1);
        ResultSet rs = stmt.executeQuery("select i, a, b, 17 c from #t0046");
        assertNotNull(rs);
        ResultSetMetaData md = rs.getMetaData();
        assertNotNull(md);
        assertTrue(md.isAutoIncrement(1));
        assertTrue(!md.isAutoIncrement(2));
        assertTrue(!md.isAutoIncrement(3));
        assertTrue(!md.isAutoIncrement(4));
        assertTrue(md.isReadOnly(1));
        assertTrue(!md.isReadOnly(2));
        assertTrue(!md.isReadOnly(3));
        assertTrue(md.isReadOnly(4));
        assertEquals(md.isNullable(1), java.sql.ResultSetMetaData.columnNoNulls);
        assertEquals(md.isNullable(2), java.sql.ResultSetMetaData.columnNoNulls);
        assertEquals(md.isNullable(3), java.sql.ResultSetMetaData.columnNullable);
        rs.close();
    }

    public void testTimestamps0047() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0047");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0047 " + "(                               " + "  t1   datetime not null,       " + "  t2   datetime null,           " + "  t3   smalldatetime not null,  " + "  t4   smalldatetime null       " + ")");
        String query = "insert into #t0047 (t1, t2, t3, t4) " + " values('2000-01-02 19:35:01.333', " + "        '2000-01-02 19:35:01.333', " + "        '2000-01-02 19:35:01.333', " + "        '2000-01-02 19:35:01.333'  " + ")";
        int count = stmt.executeUpdate(query);
        assertTrue(count == 1);
        ResultSet rs = stmt.executeQuery("select t1, t2, t3, t4 from #t0047");
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        java.sql.Timestamp t1 = rs.getTimestamp("t1");
        java.sql.Timestamp t2 = rs.getTimestamp("t2");
        java.sql.Timestamp t3 = rs.getTimestamp("t3");
        java.sql.Timestamp t4 = rs.getTimestamp("t4");
        java.sql.Timestamp r1 = new java.sql.Timestamp(100, 0, 2, 19, 35, 1, 333000000);
        java.sql.Timestamp r2 = new java.sql.Timestamp(100, 0, 2, 19, 35, 0, 0);
        assertEquals(r1, t1);
        assertEquals(r1, t2);
        assertEquals(r2, t3);
        assertEquals(r2, t4);
    }

    public void testTimestamps0048() throws Exception {
        Connection cx = getConnection();
        dropTable("#t0048");
        Statement stmt = cx.createStatement();
        stmt.executeUpdate("create table #t0048              " + "(                               " + "  t1   datetime not null,       " + "  t2   datetime null,           " + "  t3   smalldatetime not null,  " + "  t4   smalldatetime null       " + ")");
        java.sql.Timestamp r1;
        java.sql.Timestamp r2;
        r1 = new java.sql.Timestamp(100, 0, 2, 19, 35, 01, 0 * 1000000);
        r2 = new java.sql.Timestamp(100, 0, 2, 19, 35, 0, 0);
        java.sql.PreparedStatement pstmt = cx.prepareStatement("insert into #t0048 (t1, t2, t3, t4) values(?, ?, ?, ?)");
        pstmt.setTimestamp(1, r1);
        pstmt.setTimestamp(2, r1);
        pstmt.setTimestamp(3, r1);
        pstmt.setTimestamp(4, r1);
        int count = pstmt.executeUpdate();
        assertTrue(count == 1);
        pstmt.close();
        ResultSet rs = stmt.executeQuery("select t1, t2, t3, t4 from #t0048");
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        java.sql.Timestamp t1 = rs.getTimestamp("t1");
        java.sql.Timestamp t2 = rs.getTimestamp("t2");
        java.sql.Timestamp t3 = rs.getTimestamp("t3");
        java.sql.Timestamp t4 = rs.getTimestamp("t4");
        assertEquals(r1, t1);
        assertEquals(r1, t2);
        assertEquals(r2, t3);
        assertEquals(r2, t4);
    }

    public void testDecimalConversion0058() throws Exception {
        Connection cx = getConnection();
        Statement stmt = cx.createStatement();
        ResultSet rs = stmt.executeQuery("select convert(DECIMAL(4,0), 0)");
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        assertTrue(rs.getInt(1) == 0);
        assertTrue("Expected no result set", !rs.next());
        rs = stmt.executeQuery("select convert(DECIMAL(4,0), 1)");
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        assertTrue(rs.getInt(1) == 1);
        assertTrue("Expected no result set", !rs.next());
        rs = stmt.executeQuery("select convert(DECIMAL(4,0), -1)");
        assertNotNull(rs);
        assertTrue("Expected a result set", rs.next());
        assertTrue(rs.getInt(1) == -1);
        assertTrue("Expected no result set", !rs.next());
    }
}
