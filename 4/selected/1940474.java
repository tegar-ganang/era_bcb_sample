package edu.vt.middleware.ldap.dsml;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import javax.naming.directory.SearchResult;
import edu.vt.middleware.ldap.Ldap;
import edu.vt.middleware.ldap.SearchFilter;
import edu.vt.middleware.ldap.TestUtil;
import edu.vt.middleware.ldap.bean.LdapEntry;
import edu.vt.middleware.ldap.bean.LdapResult;
import edu.vt.middleware.ldap.bean.SortedLdapBeanFactory;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * Unit test for {@link Dsmlv1} and {@link Dsmlv2}.
 *
 * @author  Middleware Services
 * @version  $Revision: 1330 $
 */
public class DsmlTest {

    /** Entry created for ldap tests. */
    private static LdapEntry testLdapEntry;

    /**
   * @param  ldifFile  to create.
   *
   * @throws  Exception  On test failure.
   */
    @Parameters({ "createEntry11" })
    @BeforeClass(groups = { "dsmltest" })
    public void createLdapEntry(final String ldifFile) throws Exception {
        final String ldif = TestUtil.readFileIntoString(ldifFile);
        testLdapEntry = TestUtil.convertLdifToEntry(ldif);
        Ldap ldap = TestUtil.createSetupLdap();
        ldap.create(testLdapEntry.getDn(), testLdapEntry.getLdapAttributes().toAttributes());
        ldap.close();
        ldap = TestUtil.createLdap();
        while (!ldap.compare(testLdapEntry.getDn(), new SearchFilter(testLdapEntry.getDn().split(",")[0]))) {
            Thread.sleep(100);
        }
        ldap.close();
    }

    /** @throws  Exception  On test failure. */
    @AfterClass(groups = { "dsmltest" })
    public void deleteLdapEntry() throws Exception {
        final Ldap ldap = TestUtil.createSetupLdap();
        ldap.delete(testLdapEntry.getDn());
        ldap.close();
    }

    /**
   * @param  dn  to search on.
   * @param  filter  to search with.
   *
   * @throws  Exception  On test failure.
   */
    @Parameters({ "dsmlSearchDn", "dsmlSearchFilter" })
    @Test(groups = { "dsmltest" })
    public void searchAndCompareDsmlv1(final String dn, final String filter) throws Exception {
        final Ldap ldap = TestUtil.createLdap();
        final Dsmlv1 dsml = new Dsmlv1();
        final Iterator<SearchResult> iter = ldap.search(dn, new SearchFilter(filter));
        final LdapResult result1 = TestUtil.newLdapResult(iter);
        final StringWriter writer = new StringWriter();
        dsml.outputDsml(result1.toSearchResults().iterator(), writer);
        final StringReader reader = new StringReader(writer.toString());
        final LdapResult result2 = dsml.importDsmlToLdapResult(reader);
        AssertJUnit.assertEquals(result1, result2);
        ldap.close();
    }

    /**
   * @param  dsmlFile  to test with.
   * @param  dsmlSortedFile  to test with.
   *
   * @throws  Exception  On test failure.
   */
    @Parameters({ "dsmlv1Entry", "dsmlv1SortedEntry" })
    @Test(groups = { "dsmltest" })
    public void readAndCompareDsmlv1(final String dsmlFile, final String dsmlSortedFile) throws Exception {
        final Dsmlv1 dsml = new Dsmlv1();
        dsml.setLdapBeanFactory(new SortedLdapBeanFactory());
        final String dsmlStringSorted = TestUtil.readFileIntoString(dsmlSortedFile);
        final Iterator<SearchResult> iter = dsml.importDsml(new StringReader(TestUtil.readFileIntoString(dsmlFile)));
        final StringWriter writer = new StringWriter();
        dsml.outputDsml(iter, writer);
        AssertJUnit.assertEquals(dsmlStringSorted, writer.toString());
    }

    /**
   * @param  dn  to search on.
   * @param  filter  to search with.
   *
   * @throws  Exception  On test failure.
   */
    @Parameters({ "dsmlSearchDn", "dsmlSearchFilter" })
    @Test(groups = { "dsmltest" })
    public void searchAndCompareDsmlv2(final String dn, final String filter) throws Exception {
        final Ldap ldap = TestUtil.createLdap();
        final Dsmlv2 dsml = new Dsmlv2();
        final Iterator<SearchResult> iter = ldap.search(dn, new SearchFilter(filter));
        final LdapResult result1 = TestUtil.newLdapResult(iter);
        final StringWriter writer = new StringWriter();
        dsml.outputDsml(result1.toSearchResults().iterator(), writer);
        final StringReader reader = new StringReader(writer.toString());
        final LdapResult result2 = dsml.importDsmlToLdapResult(reader);
        AssertJUnit.assertEquals(result1, result2);
        ldap.close();
    }

    /**
   * @param  dsmlFile  to test with.
   * @param  dsmlSortedFile  to test with.
   *
   * @throws  Exception  On test failure.
   */
    @Parameters({ "dsmlv2Entry", "dsmlv2SortedEntry" })
    @Test(groups = { "dsmltest" })
    public void readAndCompareDsmlv2(final String dsmlFile, final String dsmlSortedFile) throws Exception {
        final Dsmlv2 dsml = new Dsmlv2();
        dsml.setLdapBeanFactory(new SortedLdapBeanFactory());
        final String dsmlStringSorted = TestUtil.readFileIntoString(dsmlSortedFile);
        final Iterator<SearchResult> iter = dsml.importDsml(new StringReader(TestUtil.readFileIntoString(dsmlFile)));
        final StringWriter writer = new StringWriter();
        dsml.outputDsml(iter, writer);
        AssertJUnit.assertEquals(dsmlStringSorted, writer.toString());
    }
}
