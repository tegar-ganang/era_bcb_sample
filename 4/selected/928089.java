package edu.vt.middleware.ldap.ldif;

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
 * Unit test for {@link Ldif}.
 *
 * @author  Middleware Services
 * @version  $Revision: 1330 $
 */
public class LdifTest {

    /** Entry created for ldap tests. */
    private static LdapEntry testLdapEntry;

    /**
   * @param  ldifFile  to create.
   *
   * @throws  Exception  On test failure.
   */
    @Parameters({ "createEntry12" })
    @BeforeClass(groups = { "ldiftest" })
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
    @AfterClass(groups = { "ldiftest" })
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
    @Parameters({ "ldifSearchDn", "ldifSearchFilter" })
    @Test(groups = { "ldiftest" })
    public void searchAndCompareLdif(final String dn, final String filter) throws Exception {
        final Ldap ldap = TestUtil.createLdap();
        final Ldif ldif = new Ldif();
        final Iterator<SearchResult> iter = ldap.search(dn, new SearchFilter(filter));
        final LdapResult result1 = TestUtil.newLdapResult(iter);
        final StringWriter writer = new StringWriter();
        ldif.outputLdif(result1.toSearchResults().iterator(), writer);
        final StringReader reader = new StringReader(writer.toString());
        final LdapResult result2 = TestUtil.newLdapResult(ldif.importLdif(reader));
        AssertJUnit.assertEquals(result1, result2);
        ldap.close();
    }

    /**
   * @param  ldifFile  to create with
   * @param  ldifSortedFile  to compare with
   *
   * @throws  Exception  On test failure.
   */
    @Parameters({ "ldifEntry", "ldifSortedEntry" })
    @Test(groups = { "ldiftest" })
    public void readAndCompareSortedLdif(final String ldifFile, final String ldifSortedFile) throws Exception {
        final Ldif ldif = new Ldif();
        ldif.setLdapBeanFactory(new SortedLdapBeanFactory());
        final String ldifStringSorted = TestUtil.readFileIntoString(ldifSortedFile);
        final Iterator<SearchResult> iter = ldif.importLdif(new StringReader(TestUtil.readFileIntoString(ldifFile)));
        final StringWriter writer = new StringWriter();
        ldif.outputLdif(iter, writer);
        AssertJUnit.assertEquals(ldifStringSorted, writer.toString());
    }

    /**
   * @param  ldifFileIn  to create with
   * @param  ldifFileOut  to compare with
   *
   * @throws  Exception  On test failure.
   */
    @Parameters({ "multipleLdifResultsIn", "multipleLdifResultsOut" })
    @Test(groups = { "ldiftest" })
    public void readAndCompareMultipleLdif(final String ldifFileIn, final String ldifFileOut) throws Exception {
        final Ldif ldif = new Ldif();
        final String ldifStringIn = TestUtil.readFileIntoString(ldifFileIn);
        Iterator<SearchResult> iter = ldif.importLdif(new StringReader(ldifStringIn));
        final LdapResult ldif1 = TestUtil.newLdapResult(iter);
        final String ldifStringOut = TestUtil.readFileIntoString(ldifFileOut);
        iter = ldif.importLdif(new StringReader(ldifStringOut));
        final LdapResult ldif2 = TestUtil.newLdapResult(iter);
        AssertJUnit.assertEquals(ldif1, ldif2);
    }
}
