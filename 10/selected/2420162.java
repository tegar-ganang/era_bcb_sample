package de.sonivis.tool.mwapiconnector.tests;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.sonivis.tool.core.ModelManager;
import de.sonivis.tool.core.datamodel.InfoSpace;
import de.sonivis.tool.core.datamodel.dao.hibernate.InteractionRelationDAO;
import de.sonivis.tool.core.datamodel.exceptions.CannotConnectToDatabaseException;
import de.sonivis.tool.core.tests.AbstractFilledReadOnlyDatabaseTestCase;
import de.sonivis.tool.mediawikiconnector.transformers.DiscussionTransformer;
import de.sonivis.tool.mwapiconnector.datamodel.extension.DiscussionRelation;

/**
 * Test case for the {@link DiscussionTransformer} class.
 * 
 * @author Andreas Erber
 * @version $Revision$, $Date$
 */
public class DiscussionTransformerTest extends AbstractFilledReadOnlyDatabaseTestCase {

    /**
	 * Logger.
	 */
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscussionTransformerTest.class);

    /**
	 * {@inheritDoc}
	 * 
	 * @see de.sonivis.tool.core.tests.AbstractFilledReadOnlyDatabaseTestCase#setUp()
	 */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @see AbstractFilledReadOnlyDatabaseTestCase#tearDown()
	 */
    @Override
    protected void tearDown() {
        super.tearDown();
    }

    /**
	 * Test method for {@link DiscussionTransformer#transform(String, InfoSpace, IProgressMonitor)}
	 * .
	 */
    public void testTransform() {
        final InteractionRelationDAO iaDAO = new InteractionRelationDAO();
        Integer existingDiscussionCount = null;
        try {
            existingDiscussionCount = iaDAO.count(infoSpace, DiscussionRelation.class);
        } catch (final CannotConnectToDatabaseException e1) {
            fail("Persistence store is not available.");
        }
        if (existingDiscussionCount != null && existingDiscussionCount > 0) {
            Session s = null;
            Transaction tx = null;
            try {
                s = ModelManager.getInstance().getCurrentSession();
                tx = s.beginTransaction();
                s.createSQLQuery("DELETE infospaceitem, interactionrelation " + "USING infospaceitem LEFT JOIN interactionrelation " + "ON interactionrelation.id = infospaceitem.id " + "WHERE infospaceitem.type LIKE '%DiscussionRelation'").executeUpdate();
                s.flush();
                s.clear();
                tx.commit();
            } catch (final HibernateException he) {
                tx.rollback();
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Exception when attempting to remove all existing discussion relations - transaction rolled back!", he);
                }
                fail("Unable to remove existing DiscussionRelation entities in persistence store before running method under test.");
            } catch (final CannotConnectToDatabaseException e) {
                fail("Persistence store is not available.");
            } finally {
                s.close();
            }
        }
        try {
            new DiscussionTransformer().transform(this.infoSpace.getExtractorFQN(), this.infoSpace, new NullProgressMonitor());
        } catch (final CannotConnectToDatabaseException e) {
            fail("Persistence store is not available.");
        }
        try {
            existingDiscussionCount = iaDAO.count(infoSpace, DiscussionRelation.class);
        } catch (final CannotConnectToDatabaseException e1) {
            fail("Persistence store is not available.");
        }
        if (existingDiscussionCount == null || existingDiscussionCount == 0) {
            fail("Persistence store does not contain any DiscussionRelation entities after running the method under test. This might not actually be wrong but is unexpected.");
        }
    }
}
