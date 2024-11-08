package de.schwarzrot.data.access.support;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import de.schwarzrot.app.domain.User;
import de.schwarzrot.data.access.Repository;
import de.schwarzrot.data.access.jdbc.JDBCModelScanner;
import de.schwarzrot.data.access.jdbc.JDBCTestBase;
import de.schwarzrot.data.access.jdbc.JDBCUtil;
import de.schwarzrot.data.meta.DatabaseModel;
import de.schwarzrot.data.meta.IndexDefinition;
import de.schwarzrot.data.meta.SchemaDefinition;
import de.schwarzrot.data.meta.TableDefinition;
import junit.framework.TestSuite;

public class DBUtilTest extends JDBCTestBase {

    private static DatabaseModel dbm;

    private DbUtil dbUtil;

    private Repository repository;

    public DBUtilTest(String testCase) {
        super(testCase);
    }

    public void testMySQL() throws Exception {
        performDbUtilTests("mySQL");
    }

    public void testPgSQL() throws Exception {
        performDbUtilTests("pgSQL");
    }

    public void testMyMeta() throws Exception {
        testMeta("mySQL");
    }

    public void testPgMeta() throws Exception {
        testMeta("pgSQL");
    }

    protected void testMeta(String dbType) throws Exception {
        Connection conn = connect();
        JDBCModelScanner ms = new JDBCModelScanner(conn);
        DbUtil dbUtil = JDBCUtil.getUtil(conn);
        DatabaseModel dbm = ms.scan();
        assertNotNull(dbm);
        dumpSchemaCreation(dbm, dbUtil, dbType);
        conn.close();
    }

    protected String getUser() {
        return "rednose";
    }

    protected void dumpSchemaCreation(DatabaseModel model, DbUtil dbUtil, String dbVariant) throws Exception {
        String sql;
        System.out.println();
        System.out.println("-- *********************************************************************** --");
        System.out.println("--    Start of database schema creation for database-type: " + dbVariant);
        System.out.println("-- ======================================================================= --");
        System.out.println();
        sql = dbUtil.genDBCreation(model) + dbUtil.getTerminator();
        System.out.println(sql);
        sql = dbUtil.genUserCreation("vaUser", "secret") + dbUtil.getTerminator();
        System.out.println(sql);
        sql = dbUtil.genDBAccess(model, getUser()) + dbUtil.getTerminator();
        System.out.println(sql);
        for (SchemaDefinition sd : model.getSchemata().values()) {
            sql = dbUtil.genSchemaCreation(sd) + dbUtil.getTerminator();
            System.out.println(sql);
            sql = dbUtil.genSchemaAccess(sd, getUser()) + dbUtil.getTerminator();
            System.out.println(sql);
            System.out.println();
            for (TableDefinition td : sd.getTables().values()) {
                sql = dbUtil.genTableRemoval(td, true) + dbUtil.getTerminator();
                System.out.println(sql);
                if (dbUtil.needExternalSequenceForSerial()) {
                    sql = dbUtil.genTablePKCreation(td) + dbUtil.getTerminator();
                    System.out.println(sql);
                }
                sql = dbUtil.genTableCreation(td) + dbUtil.getTerminator();
                System.out.println(sql);
                if (dbUtil.needExternalSequenceForSerial()) {
                    sql = dbUtil.genTablePKMod(td) + dbUtil.getTerminator();
                    System.out.println(sql);
                    sql = dbUtil.genAccessRightsForPK(td, getUser()) + dbUtil.getTerminator();
                    System.out.println(sql);
                    System.out.println();
                }
                sql = dbUtil.genAccessRightsFor(td, getUser()) + dbUtil.getTerminator();
                System.out.println(sql);
                for (IndexDefinition id : td.getIndices()) {
                    sql = dbUtil.genIndexCreation(id) + dbUtil.getTerminator();
                    System.out.println(sql);
                }
                System.out.println();
            }
        }
        System.out.println();
        System.out.println("-- ======================================================================= --");
        System.out.println("--    End of database schema creation for database-type: " + dbVariant);
        System.out.println("-- *********************************************************************** --");
        System.out.println();
    }

    protected void performDbUtilTests(String dbVariant) throws Exception {
        assertNotNull("oups, need a model for test!", dbm);
        assertNotNull("Huh? - dbUtil may not be null!", dbUtil);
        dumpSchemaCreation(dbm, dbUtil, dbVariant);
        performInsertTest();
    }

    protected void performInsertTest() throws Exception {
        Connection conn = connect();
        EntityDescriptor ed = repository.getEntityDescriptor(User.class);
        User testUser = new User();
        Date now = new Date();
        conn.setAutoCommit(false);
        testUser.setUsername("rednose");
        testUser.setUCreated("dbUtilTest");
        testUser.setUModified("dbUtilTest");
        testUser.setDtCreated(now);
        testUser.setDtModified(now);
        String sql = dbUtil.genInsert(ed, testUser);
        Statement st = conn.createStatement();
        long id = 0;
        System.err.println("Insert: " + sql);
        int rv = st.executeUpdate(sql, dbUtil.supportsGeneratedKeyQuery() ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
        if (rv > 0) {
            if (dbUtil.supportsGeneratedKeyQuery()) {
                ResultSet rs = st.getGeneratedKeys();
                if (rs.next()) id = rs.getLong(1);
            } else {
                id = queryId(ed, now, "dbUtilTest", conn, dbUtil);
            }
            if (id > 0) testUser.setId(id); else rv = 0;
        }
        conn.rollback();
        assertTrue("oups, insert failed?", id != 0);
        System.err.println("successfully created user with id #" + id + " temporarily");
    }

    protected long queryId(EntityDescriptor ed, Date now, String userName, Connection conn, DbUtil dbUtil) throws Exception {
        long rv = 0;
        String sql = dbUtil.genInsertIdQuery(ed, now, userName);
        Statement st = conn.createStatement();
        System.out.println("gonna execute sql: " + sql);
        ResultSet rs = st.executeQuery(sql);
        if (rs.next()) rv = rs.getLong(ed.getPhysName("id"));
        return rv;
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new DBUtilTest("testMyMeta"));
        suite.addTest(new DBUtilTest("testPgMeta"));
        return suite;
    }
}
