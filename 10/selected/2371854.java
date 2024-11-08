package org.yass.dao.schema;

import javax.persistence.EntityManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author svenduzont
 */
public abstract class Schema {

    /**
     *
     */
    protected static final Log LOG = LogFactory.getLog(Schema.class);

    private final EntityManager entityManagerProxy;

    /**
	 * @param entityManager
	 */
    public Schema(final EntityManager entityManager) {
        super();
        entityManagerProxy = entityManager;
    }

    /**
	 * 
	 * @param table
	 * @param column
	 * @return
	 */
    protected boolean columnExists(final String table, final String column) {
        try {
            entityManagerProxy.createNativeQuery("select " + column + " from " + table + " where 0 = 1").getSingleResult();
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    /**
     *
     */
    public abstract boolean execute();

    public abstract boolean execute(boolean create);

    /**
	 * 
	 * @param query
	 */
    protected void executeUpdate(final String query) {
        try {
            entityManagerProxy.getTransaction().begin();
            entityManagerProxy.createNativeQuery(query).executeUpdate();
            entityManagerProxy.getTransaction().commit();
        } catch (final Exception e) {
            LOG.debug("Error executing update " + query, e);
            entityManagerProxy.getTransaction().rollback();
        }
    }

    /**
	 * 
	 * @param indexName
	 * @return
	 */
    protected boolean indexExists(final String indexName) {
        try {
            return entityManagerProxy.createNativeQuery("select count(*) from SYS.SYSCONGLOMERATES where 	CONGLOMERATENAME = ?1").setParameter(1, indexName).getResultList().size() != 0;
        } catch (final Exception e) {
            return false;
        }
    }

    /**
	 * 
	 * @param table
	 * @return
	 */
    protected boolean tableExists(final String table) {
        try {
            entityManagerProxy.createNativeQuery("select 1 from " + table).getResultList();
        } catch (final Exception e) {
            return false;
        }
        return true;
    }
}
