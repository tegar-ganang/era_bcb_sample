package org.firebirdsql.jca;

import java.sql.Connection;
import org.firebirdsql.gds.ISCConstants;
import org.firebirdsql.gds.TransactionParameterBuffer;
import org.firebirdsql.jdbc.FBTpbMapper;
import org.firebirdsql.common.FBTestBase;

/**
 * <a href="mailto:rrokytskyy@users.sourceforge.net">Roman Rokytskyy</a>
 */
public class TestFBTpbMapper extends FBTestBase {

    public static final String TEST_TPB_MAPPING = "org.firebirdsql.jca.test_tpb_mapping";

    public TestFBTpbMapper(String string) {
        super(string);
    }

    FBManagedConnectionFactory mcf;

    protected void setUp() throws Exception {
        super.setUp();
        mcf = super.createFBManagedConnectionFactory();
    }

    /**
     * Test if default isolation level is Connection.TRANSACTION_READ_COMMITTED
     * 
     * @throws Exception if something went wrong.
     */
    public void testDefaultIsolationLevel() throws Exception {
        assertTrue("Default tx isolation level must be READ_COMMITTED", mcf.getDefaultTransactionIsolation() == Connection.TRANSACTION_READ_COMMITTED);
    }

    /**
     * Test custom TPB mapper. This test case constructs customg TPB mapper,
     * assigns it to managed connection factory and checks if correct values
     * are obtained from TPB.
     * 
     * @throws Exception if something went wrong.
     */
    public void testTpbMapper() throws Exception {
        FBTpbMapper mapper = new FBTpbMapper(mcf.getGDS(), TEST_TPB_MAPPING, getClass().getClassLoader());
        mcf.setTpbMapping(TEST_TPB_MAPPING);
        mcf.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        TransactionParameterBuffer tpbValue = mcf.getDefaultTpb().getTransactionParameterBuffer();
        assertTrue("READ_COMMITED must be isc_tpb_read_committed+" + "isc_tpb_no_rec_version+isc_tpb_write+isc_tpb_nowait", tpbValue.hasArgument(ISCConstants.isc_tpb_read_committed) && tpbValue.hasArgument(ISCConstants.isc_tpb_no_rec_version) && tpbValue.hasArgument(ISCConstants.isc_tpb_write) && tpbValue.hasArgument(ISCConstants.isc_tpb_nowait));
        mcf.setDefaultTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        tpbValue = mcf.getDefaultTpb().getTransactionParameterBuffer();
        assertTrue("REPEATABLE_READ must be isc_tpb_consistency+" + "isc_tpb_write+isc_tpb_wait", tpbValue.hasArgument(ISCConstants.isc_tpb_consistency) && tpbValue.hasArgument(ISCConstants.isc_tpb_write) && tpbValue.hasArgument(ISCConstants.isc_tpb_wait));
        mcf.setDefaultTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        tpbValue = mcf.getDefaultTpb().getTransactionParameterBuffer();
        assertTrue("SERIALIZABLE must be isc_tpb_concurrency+" + "isc_tpb_write+isc_tpb_wait", tpbValue.hasArgument(ISCConstants.isc_tpb_concurrency) && tpbValue.hasArgument(ISCConstants.isc_tpb_write) && tpbValue.hasArgument(ISCConstants.isc_tpb_wait));
    }
}
