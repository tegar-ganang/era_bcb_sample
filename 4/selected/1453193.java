package org.fcrepo.server.journal.readerwriter.multicast;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ org.fcrepo.server.journal.readerwriter.multicast.rmi.AllIntegrationTests.class })
public class AllIntegrationTests {

    public static junit.framework.Test suite() throws Exception {
        junit.framework.TestSuite suite = new junit.framework.TestSuite(AllIntegrationTests.class.getName());
        suite.addTest(org.fcrepo.server.journal.readerwriter.multicast.rmi.AllIntegrationTests.suite());
        return suite;
    }
}