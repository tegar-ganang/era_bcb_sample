package edu.vt.middleware.ldap.io;

import java.io.StringReader;
import java.io.StringWriter;
import edu.vt.middleware.ldap.AbstractTest;
import edu.vt.middleware.ldap.Connection;
import edu.vt.middleware.ldap.LdapEntry;
import edu.vt.middleware.ldap.LdapResult;
import edu.vt.middleware.ldap.SearchFilter;
import edu.vt.middleware.ldap.SearchOperation;
import edu.vt.middleware.ldap.SearchRequest;
import edu.vt.middleware.ldap.SortBehavior;
import edu.vt.middleware.ldap.TestUtil;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * Unit test for {@link Dsmlv1Reader}, {@link Dsmlv1Writer},
 * {@link Dsmlv2Reader}, and {@link Dsmlv2Writer}.
 *
 * @author  Middleware Services
 * @version  $Revision: 2165 $
 */
public class DsmlTest extends AbstractTest {

    /** Entry created for ldap tests. */
    private static LdapEntry testLdapEntry;

    /**
   * @param  ldifFile  to create.
   *
   * @throws  Exception  On test failure.
   */
    @Parameters("createEntry13")
    @BeforeClass(groups = { "dsml" })
    public void createLdapEntry(final String ldifFile) throws Exception {
        final String ldif = TestUtil.readFileIntoString(ldifFile);
        testLdapEntry = TestUtil.convertLdifToResult(ldif).getEntry();
        super.createLdapEntry(testLdapEntry);
    }

    /** @throws  Exception  On test failure. */
    @AfterClass(groups = { "dsml" })
    public void deleteLdapEntry() throws Exception {
        super.deleteLdapEntry(testLdapEntry.getDn());
    }

    /**
   * @param  dn  to search on.
   * @param  filter  to search with.
   *
   * @throws  Exception  On test failure.
   */
    @Parameters({ "dsmlSearchDn", "dsmlSearchFilter" })
    @Test(groups = { "dsml" })
    public void searchAndCompareDsmlv1(final String dn, final String filter) throws Exception {
        final Connection conn = TestUtil.createConnection();
        try {
            conn.open();
            final SearchOperation search = new SearchOperation(conn);
            final LdapResult result1 = search.execute(new SearchRequest(dn, new SearchFilter(filter))).getResult();
            final StringWriter writer = new StringWriter();
            final Dsmlv1Writer dsmlWriter = new Dsmlv1Writer(writer);
            dsmlWriter.write(result1);
            final StringReader reader = new StringReader(writer.toString());
            final Dsmlv1Reader dsmlReader = new Dsmlv1Reader(reader);
            final LdapResult result2 = dsmlReader.read();
            AssertJUnit.assertEquals(result1, result2);
        } finally {
            conn.close();
        }
    }

    /**
   * @param  dsmlFile  to test with.
   * @param  dsmlSortedFile  to test with.
   *
   * @throws  Exception  On test failure.
   */
    @Parameters({ "dsmlv1Entry", "dsmlv1SortedEntry" })
    @Test(groups = { "dsml" })
    public void readAndCompareDsmlv1(final String dsmlFile, final String dsmlSortedFile) throws Exception {
        final String dsmlStringSorted = TestUtil.readFileIntoString(dsmlSortedFile);
        final Dsmlv1Reader dsmlReader = new Dsmlv1Reader(new StringReader(TestUtil.readFileIntoString(dsmlFile)), SortBehavior.SORTED);
        final LdapResult result = dsmlReader.read();
        final StringWriter writer = new StringWriter();
        final Dsmlv1Writer dsmlWriter = new Dsmlv1Writer(writer);
        dsmlWriter.write(result);
        AssertJUnit.assertEquals(dsmlStringSorted, writer.toString());
    }
}
