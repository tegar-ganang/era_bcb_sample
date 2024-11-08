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
import de.sonivis.tool.core.datamodel.extension.Collaboration;
import de.sonivis.tool.core.tests.AbstractFilledReadOnlyDatabaseTestCase;
import de.sonivis.tool.mediawikiconnector.transformers.CollaborationTransformer;

/**
 * 
 * 
 * @author Andreas Erber
 * @version $Revision$, $Date$
 */
public class CollaborationTransformerTest extends AbstractFilledReadOnlyDatabaseTestCase {

    /**
	 * Logger.
	 */
    private static final Logger LOGGER = LoggerFactory.getLogger(CollaborationTransformerTest.class);

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
	 * Test method for
	 * {@link CollaborationTransformer#transform(String, InfoSpace, IProgressMonitor)}
	 */
    public void testTransform() {
        final InteractionRelationDAO iaDAO = new InteractionRelationDAO();
        Integer existingCollCount = null;
        try {
            existingCollCount = iaDAO.count(infoSpace, Collaboration.class);
        } catch (final CannotConnectToDatabaseException e1) {
            fail("Persistence store is not available.");
        }
        if (existingCollCount != null && existingCollCount > 0) {
            Session s = null;
            Transaction tx = null;
            try {
                s = ModelManager.getInstance().getCurrentSession();
                tx = s.beginTransaction();
                s.createSQLQuery("DELETE FROM infospaceitem, interactionrelation" + "USING infospaceitem LEFT JOIN interactionrelation " + "ON interactionrelation.id = infospaceitem.id " + "WHERE infospaceitem.type LIKE '%Collaboration'").executeUpdate();
                s.flush();
                s.clear();
                tx.commit();
            } catch (final HibernateException he) {
                tx.rollback();
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Exception when attempting to remove all existing collaborations - transaction rolled back!", he);
                }
                fail("Unable to remove existing Collaboration entities in persistence store before running method under test.");
            } catch (final CannotConnectToDatabaseException e) {
                fail("Persistence store is not available.");
            } finally {
                s.close();
            }
        }
        try {
            new CollaborationTransformer().transform(this.infoSpace.getExtractorFQN(), this.infoSpace, new NullProgressMonitor());
        } catch (final CannotConnectToDatabaseException e) {
            fail("Persistence store is not available.");
        }
        try {
            existingCollCount = iaDAO.count(infoSpace, Collaboration.class);
        } catch (final CannotConnectToDatabaseException e1) {
            fail("Persistence store is not available.");
        }
        if (existingCollCount == null || existingCollCount == 0) {
            fail("Persistence store does not contain any Collaboration entities after running the method under test. This might not actually be wrong but is unexpected.");
        }
    }
}
