package org.fcrepo.server.journal;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ org.fcrepo.server.journal.helpers.AllUnitTests.class, org.fcrepo.server.journal.readerwriter.AllUnitTests.class, org.fcrepo.server.journal.xmlhelpers.AllUnitTests.class, TestJournalRoundTrip.class })
public class AllUnitTests {

    public static junit.framework.Test suite() throws Exception {
        junit.framework.TestSuite suite = new junit.framework.TestSuite(AllUnitTests.class.getName());
        suite.addTest(org.fcrepo.server.journal.helpers.AllUnitTests.suite());
        suite.addTest(org.fcrepo.server.journal.readerwriter.AllUnitTests.suite());
        suite.addTest(org.fcrepo.server.journal.xmlhelpers.AllUnitTests.suite());
        suite.addTest(TestJournalRoundTrip.suite());
        return suite;
    }
}
