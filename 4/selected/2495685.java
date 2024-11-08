package org.obe.server.j2ee.ejb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.obe.server.j2ee.repository.TransactionUtil;
import javax.ejb.CreateException;
import javax.ejb.SessionSynchronization;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the transaction lifecycle.  Enables OBE to know when to use read-write
 * entity beans instead of read-only beans (if the read-write version for a
 * particular entity class has already been used within the current
 * transaction).
 *
 * @author Adrian Price.
 * @ejb:bean type="Stateful"
 * name="TxHelper"
 * display-name="OBE Transaction Helper"
 * local-jndi-name="org/obe/ejb/TxHelperLocal"
 * transaction-type="Container"
 * view-type="local"
 * @ejb:home extends="javax.ejb.EJBHome"
 * local-extends="javax.ejb.EJBLocalHome"
 * local-package="org.obe.server.j2ee"
 * @ejb:interface local-extends="javax.ejb.EJBLocalObject"
 * local-package="org.obe.server.j2ee"
 * @ejb:permission unchecked="true"
 * @ejb:transaction type="Supports"
 * @weblogic:transaction-isolation ${transaction.isolation}
 * @ejb:resource-ref res-name="jdbc/TxDataSource"
 * res-type="javax.sql.DataSource"
 * res-auth="Container"
 * @jboss:resource-manager res-man-class="javax.sql.DataSource"
 * res-man-name="jdbc/TxDataSource"
 * res-man-jndi-name="java:/${xdoclet.DataSource}"
 * @weblogic:resource-description res-ref-name="jdbc/TxDataSource"
 * jndi-name="${xdoclet.DataSource}"
 * @weblogic:pool max-beans-in-free-pool="50"
 * initial-beans-in-free-pool="0"
 * @weblogic:cache idle-timeout-seconds="0"
 */
public class TxHelperEJB extends AbstractSessionEJB implements SessionSynchronization {

    private static final long serialVersionUID = 1284341740343653371L;

    private static final Log _logger = LogFactory.getLog(TxHelperEJB.class);

    private String _threadId;

    private transient Map _modifiedEntities;

    protected Log getLogger() {
        return _logger;
    }

    public void afterBegin() {
        if (_logger.isDebugEnabled()) _logger.debug("afterBegin()");
    }

    public void afterCompletion(boolean committed) {
        if (_logger.isDebugEnabled()) _logger.debug("afterCompletion(" + committed + ')');
    }

    public void beforeCompletion() {
        if (_logger.isDebugEnabled()) _logger.debug("beforeCompletion()");
        _modifiedEntities.clear();
    }

    public void ejbActivate() {
        if (getLogger().isDebugEnabled()) getLogger().debug("ejbActivate(), threadId=" + _threadId);
        _modifiedEntities = new HashMap();
    }

    /**
     * Creates a new TxHelper bean.
     *
     * @param threadId The ID of the thread from which the bean is being created.
     * @throws CreateException
     * @ejb:create-method
     */
    public void ejbCreate(String threadId) throws CreateException {
        if (_logger.isDebugEnabled()) _logger.debug("ejbCreate(" + threadId + ')');
        _threadId = threadId;
        _modifiedEntities = new HashMap();
    }

    public void ejbPassivate() {
        if (getLogger().isDebugEnabled()) getLogger().debug("ejbPassivate(), threadId=" + _threadId);
        TransactionUtil.cleanup(_threadId);
        _modifiedEntities = null;
    }

    public void ejbRemove() {
        if (getLogger().isDebugEnabled()) getLogger().debug("ejbRemove(), threadId=" + _threadId);
        TransactionUtil.cleanup(_threadId);
        _modifiedEntities = null;
    }

    /**
     * Checks whether the bean is still alive. (Stateful session beans can time
     * out or be removed by a runtime exception).
     *
     * @ejb:interface-method
     */
    public void ping() {
    }

    /**
     * Marks the specified entity class as having been accessed in read-write
     * mode.
     *
     * @param entityTag A unique tag representing the entity class.
     * @ejb:interface-method
     */
    public void markReadWrite(String entityTag) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("Marking txn entity tag '" + entityTag + "' read/write");
        }
        _modifiedEntities.put(entityTag, entityTag);
    }

    /**
     * Checks whether the specified entity class has been accessed in read-write
     * mode in the current transaction.
     *
     * @param entityTag A unique tag representing the entity class.
     * @return <code>true</code> if the entity class has been accessed in
     *         read-write mode, otherwise <code>false</code>.
     * @ejb:interface-method
     */
    public boolean isReadWrite(String entityTag) {
        boolean isReadWrite = _modifiedEntities.containsKey(entityTag);
        if (_logger.isDebugEnabled()) {
            _logger.debug("Checking txn entity tag '" + entityTag + "' for read/write access. Returning: " + isReadWrite);
        }
        return isReadWrite;
    }
}
