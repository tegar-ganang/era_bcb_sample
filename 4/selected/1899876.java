package org.fcrepo.server.journal.readerwriter.multicast;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ TestMulticastJournalWriterInitializations.class, TestMulticastJournalWriterOperation.class, TestJournalEntrySizeEstimator.class, TestLocalDirectoryTransport.class, org.fcrepo.server.journal.readerwriter.multicast.rmi.AllUnitTests.class })
public class AllUnitTests {

    public static junit.framework.Test suite() throws Exception {
        junit.framework.TestSuite suite = new junit.framework.TestSuite(AllUnitTests.class.getName());
        suite.addTest(TestMulticastJournalWriterInitializations.suite());
        suite.addTest(TestMulticastJournalWriterOperation.suite());
        suite.addTest(TestJournalEntrySizeEstimator.suite());
        suite.addTest(TestLocalDirectoryTransport.suite());
        suite.addTest(org.fcrepo.server.journal.readerwriter.multicast.rmi.AllUnitTests.suite());
        return suite;
    }
}
