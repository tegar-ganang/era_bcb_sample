package testsuite.simple;

import java.sql.SQLException;
import testsuite.BaseTestCase;

/**
 * 
 * @author Mark Matthews
 * @version $Id: TransactionTest.java,v 1.1.2.1 2005/05/13 18:58:37 mmatthews
 *          Exp $
 */
public class TransactionTest extends BaseTestCase {

    private static final double DOUBLE_CONST = 25.4312;

    private static final double EPSILON = .0000001;

    /**
	 * Creates a new TransactionTest object.
	 * 
	 * @param name
	 *            DOCUMENT ME!
	 */
    public TransactionTest(String name) {
        super(name);
    }

    /**
	 * Runs all test cases in this test suite
	 * 
	 * @param args
	 */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(TransactionTest.class);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @throws Exception
	 *             DOCUMENT ME!
	 */
    public void setUp() throws Exception {
        super.setUp();
        createTestTable();
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @throws SQLException
	 *             DOCUMENT ME!
	 */
    public void testTransaction() throws SQLException {
        try {
            this.conn.setAutoCommit(false);
            this.stmt.executeUpdate("INSERT INTO trans_test (id, decdata) VALUES (1, 1.0)");
            this.conn.rollback();
            this.rs = this.stmt.executeQuery("SELECT * from trans_test");
            boolean hasResults = this.rs.next();
            assertTrue("Results returned, rollback to empty table failed", (hasResults != true));
            this.stmt.executeUpdate("INSERT INTO trans_test (id, decdata) VALUES (2, " + DOUBLE_CONST + ")");
            this.conn.commit();
            this.rs = this.stmt.executeQuery("SELECT * from trans_test where id=2");
            hasResults = this.rs.next();
            assertTrue("No rows in table after INSERT", hasResults);
            double doubleVal = this.rs.getDouble(2);
            double delta = Math.abs(DOUBLE_CONST - doubleVal);
            assertTrue("Double value returned != " + DOUBLE_CONST, (delta < EPSILON));
        } finally {
            this.conn.setAutoCommit(true);
        }
    }

    private void createTestTable() throws SQLException {
        try {
            this.stmt.executeUpdate("DROP TABLE trans_test");
        } catch (SQLException sqlEx) {
            ;
        }
        this.stmt.executeUpdate("CREATE TABLE trans_test (id INT NOT NULL PRIMARY KEY, decdata DOUBLE) TYPE=InnoDB");
    }
}
