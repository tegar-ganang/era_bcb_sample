package de.sonivis.tool.mwapiconnector.tests;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import junit.framework.TestCase;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.sonivis.tool.core.CorePlugin;
import de.sonivis.tool.core.ModelManager;
import de.sonivis.tool.core.datamodel.InfoSpace;
import de.sonivis.tool.core.datamodel.exceptions.CannotConnectToDatabaseException;
import de.sonivis.tool.core.tests.AbstractEmptyDatabaseTestCase;
import de.sonivis.tool.mwapiconnector.datamodel.extension.Category;
import de.sonivis.tool.mwapiconnector.extractors.ApiExtractor;
import de.sonivis.tool.mwapiconnector.extractors.ApiExtractorArguments;
import de.sonivis.tool.mwapiconnector.extractors.IApiExtractorArguments;

/**
 * Test case for the MediaWiki {@link ApiExtractor}.
 * 
 * @author Andreas Erber
 * @version $Revision$, $Date$
 */
public class ApiExtractorTest extends AbstractEmptyDatabaseTestCase {

    /**
	 * Class logging.
	 */
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExtractor.class);

    /**
	 * Representative of the class under test.
	 */
    private ApiExtractor ae = null;

    /**
	 * Absolute or relative path to a file.
	 */
    private String path = "";

    /**
	 * {@inheritDoc}
	 * 
	 * @see TestCase#setUp()
	 */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final URL wikiUrl = new URL("http://en.wikipedia.org/w/");
        final IApiExtractorArguments iaea = new ApiExtractorArguments(wikiUrl, null, null);
        this.ae = new ApiExtractor(iaea);
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @see TestCase#tearDown()
	 */
    @Override
    protected void tearDown() {
        this.ae = null;
        super.tearDown();
    }

    /**
	 * Test method for {@link ApiExtractor#getAllCategories()}.
	 */
    public final void testGetAllCategories() {
        Set<Category> categories = null;
        try {
            categories = this.ae.getAllCategories(this.infoSpace);
        } catch (final CannotConnectToDatabaseException e) {
            fail("Persistence store is not available.");
        }
        if (categories == null) {
            fail("Result is null.");
        } else if (categories.isEmpty()) {
            fail("Result is empty.");
        }
        LOGGER.info("Could retrieve " + categories.size() + " categories for specified wiki");
        for (final Category cat : categories) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(cat.getTitle());
            }
        }
    }

    /**
	 * Test method for {@link ApiExtractor#getAllNamespaces()}.
	 */
    public final void testGetAllNamespaces() {
        Map<String, Integer> namespaces = null;
        try {
            namespaces = this.ae.getAllNamespaces(this.infoSpace);
        } catch (final CannotConnectToDatabaseException e) {
            fail("Persistence store is not available.");
        }
        if (namespaces == null) {
            fail("Result is null.");
        } else if (namespaces.isEmpty()) {
            fail("Result is empty.");
        }
        LOGGER.info("Could retrieve " + namespaces.size() + " namespaces for specified wiki");
        for (final Entry<String, Integer> namespace : namespaces.entrySet()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(namespace.getKey() + " (" + namespace.getValue() + ")");
            }
        }
    }

    /**
	 * Test method for {@link ApiExtractor#extractFromCategories(InfoSpace, Collection)}
	 */
    public final void testExtractFromCategories() {
        Set<Category> categories = null;
        try {
            categories = this.ae.getAllCategories(this.infoSpace);
        } catch (final CannotConnectToDatabaseException e1) {
            fail("Persistence store is not available.");
        }
        final Collection<Category> cats = new HashSet<Category>();
        for (final Category cat : categories) {
            if (cat.getTitle().startsWith("H")) {
                cats.add(cat);
            }
        }
        Session s = null;
        Transaction tx = null;
        try {
            s = ModelManager.getInstance().getCurrentSession();
            tx = s.beginTransaction();
            s.createSQLQuery("TRUNCATE actorcontentelementrelation;").executeUpdate();
            s.createSQLQuery("TRUNCATE contextrelation;").executeUpdate();
            s.createSQLQuery("TRUNCATE interactionrelation;").executeUpdate();
            s.createSQLQuery("TRUNCATE actor;").executeUpdate();
            s.createSQLQuery("TRUNCATE contentelement;").executeUpdate();
            s.createSQLQuery("TRUNCATE infospaceitemproperty;").executeUpdate();
            s.createSQLQuery("TRUNCATE infospaceitem;").executeUpdate();
            s.flush();
            s.clear();
            tx.commit();
        } catch (final HibernateException he) {
            tx.rollback();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Exception occurred when trying to delete test InfoSpace - transactionwas rolled back. InfoSpace and possibly several contained entities were not deleted. Must be deleted by hand.");
            }
            he.printStackTrace();
            throw he;
        } catch (final CannotConnectToDatabaseException e) {
            fail("Persistence store is not available.");
        } finally {
            s.close();
        }
    }

    public void testExtractFromNamespaceSelection() {
        CorePlugin.getDefault().getPreferenceStore().setValue("isNamespaceSelectionExtract", true);
        final IProgressMonitor monitor = new NullProgressMonitor();
        try {
            this.ae.extract(this.infoSpace, monitor);
        } catch (final Exception e) {
            if (e instanceof MalformedURLException) {
                fail("Wiki URL is incorrect");
            }
            if (e instanceof CannotConnectToDatabaseException) {
                fail("Persistence store is not available.");
            }
            e.printStackTrace();
            fail("Exception of type " + e.getClass().getSimpleName() + " occurred.");
        }
    }

    /**
	 * This test case has to be customized before being run!
	 * <p>
	 * The path to the file to be used has to be set in {@link #path}. The corresponding wiki URL
	 * has to be adapted in {@link #setUp()} accordingly.
	 * </p>
	 * <p>
	 * If you want to keep the result of this operation you might want to comment the database
	 * emptying instructions at the end of the method. If you do so, the test will fail in the end
	 * due to super class' {@link #tearDown()} failing to remove the {@link InfoSpace} instance from
	 * the database.
	 * </p>
	 */
    public void testExtractFromArticleSelection() {
        fail("Test needs to be customized before being run. See method comment for details.");
        CorePlugin.getDefault().getPreferenceStore().setValue("isArticleSelectionExtract", true);
        CorePlugin.getDefault().getPreferenceStore().setValue("ArticleSelectionFileName", this.path);
        final IProgressMonitor monitor = new NullProgressMonitor();
        try {
            this.ae.extract(this.infoSpace, monitor);
        } catch (final Exception e) {
            if (e instanceof MalformedURLException) {
                fail("Wiki URL is incorrect");
            }
            if (e instanceof CannotConnectToDatabaseException) {
                fail("Persistence store is not available.");
            }
            e.printStackTrace();
            fail("Exception of type " + e.getClass().getSimpleName() + " occurred.");
        }
        Session s = null;
        Transaction tx = null;
        try {
            s = ModelManager.getInstance().getCurrentSession();
            tx = s.beginTransaction();
            s.createSQLQuery("TRUNCATE actorcontentelementrelation;").executeUpdate();
            s.createSQLQuery("TRUNCATE contextrelation;").executeUpdate();
            s.createSQLQuery("TRUNCATE interactionrelation;").executeUpdate();
            s.createSQLQuery("TRUNCATE actor;").executeUpdate();
            s.createSQLQuery("TRUNCATE contentelement;").executeUpdate();
            s.createSQLQuery("TRUNCATE infospaceitemproperty;").executeUpdate();
            s.createSQLQuery("TRUNCATE infospaceitem;").executeUpdate();
            s.flush();
            s.clear();
            tx.commit();
        } catch (final HibernateException he) {
            tx.rollback();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Exception occurred when trying to delete test InfoSpace - transaction was rolled back. InfoSpace and possibly several contained entities were not deleted. Must be deleted by hand.", he);
            }
            throw he;
        } catch (final CannotConnectToDatabaseException e) {
            fail("Persistence store is not available.");
        } finally {
            s.close();
        }
    }
}
