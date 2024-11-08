package org.xaware.server.engine.controller.transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import org.springframework.jndi.JndiTemplate;
import org.springframework.jndi.TypeMismatchNamingException;
import org.springframework.transaction.HeuristicCompletionException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSuspensionNotSupportedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.jta.JtaAfterCompletionSynchronization;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.xaware.server.engine.IChannelKey;
import org.xaware.server.engine.ITransactionalChannel;
import org.xaware.server.engine.channel.JndiChannelKey;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * The MasterTransactionCoordinator class is responsible to managing
 * and maintaining the state for a single master transaction instance.
 * Each MasterTransactionCoordinator may have many lower-level transactions
 * embedded within it.  The management of these lower-level transactions
 * is delegated to Spring TransactionManager instances specific to
 * the channel type being managed in a particular lower-level transaction.
 * <p>
 * MasterTransactionCoordinator uses the inner TransactionState classes
 * to maintain the state of a single embedded lower-level transaction.
 * 
 * @author Tim Uttormark
 */
class MasterTransactionCoordinator implements SavepointManager {

    private static final XAwareLogger lf = XAwareLogger.getXAwareLogger(MasterTransactionCoordinator.class.getName());

    private static final String CLASS_NAME = "MasterTransactionCoordinator";

    /**
     * TransactionState holds the state of a single lower-level
     * transaction embedded within the master transaction.
     */
    protected abstract static class TransactionState {

        /**
         * The status object returned when the transaction is created.
         * It contains state for a specific lower-level transaction.
         */
        TransactionStatus transactionStatus;

        /**
         * The status object returned when this lower-level
         * transaction is suspended, for use when it is resumed.
         */
        TransactionStatus statusFromSuspend = null;

        /**
         * The savepoint object used to store state when a savepoint
         * is created, for use when it is rolled back or released.
         * Unused because nested transactions are not currently supported.
         */
        Object savepoint = null;
    }

    /**
     * LocalTransactionState holds the state of a single lower-level
     * local transaction embedded within the master transaction.
     */
    protected static class LocalTransactionState extends TransactionState {

        /**
         * The TransactionManager instance managing this local
         * lower-level transaction.
         */
        PlatformTransactionManager transactionManager;

        /**
         * The transactional channel enlisted with this lower-level
         * transaction.  For local transactions, there is only ever one.
         */
        ITransactionalChannel synchronizedChannel;
    }

    /**
     * DistributedTransactionState holds the state of a single lower-level
     * distributed transaction embedded within the master transaction.
     */
    protected static class DistributedTransactionState extends TransactionState {

        /**
         * The JtaTransactionManager instance managing this distributed
         * lower-level transaction.
         */
        XAwareJtaTransactionManager transactionManager;

        /**
         * The JNDI key for accessing the UserTransaction wrapped by
         * the XAwareJtaTransactionManager.
         */
        JndiChannelKey jndiChannelKey;

        /**
         * The transactional channels enlisted with this lower-level
         * transaction.  For distributed transactions, there may be many.
         */
        List<ITransactionalChannel> synchronizedChannels = new ArrayList<ITransactionalChannel>();
    }

    /**
     * A List of all of the local embedded transactions being managed
     * in this master transaction.
     */
    private List<LocalTransactionState> embeddedLocalTransactions;

    /**
     * A List of all the embedded JTA transactions being managed
     * in this master transaction.  There may be many, each using
     * a different provider URL.
     */
    private List<DistributedTransactionState> embeddedJtaTransactions;

    /**
     * A Map containing all of the TransactionalChannels being managed
     * in the scope of this master transaction, i.e., the transaction-scoped
     * channel pool.
     * <p>
     * Each TransactionalChannel also appears in the List of
     * synchronizedChannels owned by a specific lower-level transaction.
     */
    private Map<IChannelKey, ITransactionalChannel> managedChannels;

    /**
     * The Spring TransactionDefinition containing parameters
     * used to begin new lower-level transactions.
     */
    private DefaultTransactionDefinition definition;

    /**
     * Indicates whether the transaction is marked so that it cannot
     * commit, only roll back.
     */
    private boolean isRollbackOnly = false;

    /**
     * Create a new MasterTransactionCoordinator.
     * 
     * @param definition
     *            the Spring TransactionDefinition containing parameters
     *            used to begin new lower-level transactions
     */
    public MasterTransactionCoordinator(TransactionDefinition definition) {
        this.embeddedLocalTransactions = new ArrayList<LocalTransactionState>();
        this.embeddedJtaTransactions = new ArrayList<DistributedTransactionState>();
        this.managedChannels = new HashMap<IChannelKey, ITransactionalChannel>();
        this.definition = new DefaultTransactionDefinition(definition);
    }

    /**
     * Gets the transaction scoped ITransactionalChannel instance which matches
     * the type and key provided.
     *
     * @param key
     *            The key for the specific transactional resource instance
     *            desired.
     * 
     * @return the ITransactionalChannel instance in the transaction scoped pool
     *         which matches the type and key provided, or null if no matching
     *         ITransactionalChannel is cached yet.
     * @throws XAwareException
     *             if any error occurs finding the specified channel
     */
    public ITransactionalChannel getTransactionalChannel(IChannelKey key) throws XAwareException {
        if (key.getChannelKeyType() == IChannelKey.Type.JNDI) {
            createJtaTransactionIfNeeded(key);
        }
        ITransactionalChannel tc = managedChannels.get(key);
        if (tc != null) {
            lf.debug("TransactionalChannel found in transaction cache", CLASS_NAME, "getTransactionalChannel");
        }
        return tc;
    }

    /**
     * Resolves the UserTransaction and TransactionManager objects for the
     * JndiChannelKey provided, defaulting values and performing JNDI lookups as
     * necessary.
     * 
     * @param jndiChannelKey
     *            The key object providing the JNDI names and the Context object
     *            to perform the lookup.
     * @throws XAwareException
     *             if either the userTransactionJndiName or the transactionManagerJndiName
     *             is specified but the corresponding object could not be found, or if both
     *             UserTransaction and TransactionManager objects could be found using
     *             default JNDI names.
     */
    public static void resolveUserTransactionAndTransactionManager(JndiChannelKey jndiChannelKey) throws XAwareException {
        if (lf.isFinestEnabled()) {
            lf.finest("JNDI Contents: \n" + JndiChannelKey.dumpJndiTree(jndiChannelKey.getJndiAccessor()));
        }
        boolean utFound = lookupUserTransaction(jndiChannelKey);
        boolean tmFound = lookupTransactionManager(jndiChannelKey);
        if ((!utFound) && (!tmFound)) {
            throw new XAwareException("Failed to resolve either UserTransaction or TransactionManager for JNDI channel.");
        }
        if (!tmFound) {
            lf.warning("Proceeding without TransactionManager -- transaction suspend and resume will not be supported.", CLASS_NAME, "resolveUserTransactionAndTransactionManager");
        }
    }

    /**
     * Look up the JTA UserTransaction in JNDI using the name provided or the
     * default of JtaTransactionManager.DEFAULT_USER_TRANSACTION_NAME.
     * 
     * @param jndiChannelKey
     *            The key object providing the JNDI name and the Context object
     *            to perform the lookup.
     * @return a boolean indicating whether the lookup was successful.
     * @throws XAwareException
     *             if the userTransactionName is specified but the 
     *             UserTransaction object cannot be found via JNDI.
     */
    private static boolean lookupUserTransaction(JndiChannelKey jndiChannelKey) throws XAwareException {
        if (jndiChannelKey.getUserTransaction() != null) {
            return true;
        }
        boolean usingDefault = false;
        String userTransactionJndiName = jndiChannelKey.getUserTransactionName();
        if (userTransactionJndiName == null || userTransactionJndiName.trim().length() == 0) {
            usingDefault = true;
            userTransactionJndiName = JtaTransactionManager.DEFAULT_USER_TRANSACTION_NAME;
        }
        try {
            lf.finer("Retrieving JTA UserTransaction from JNDI location [" + userTransactionJndiName + "]");
            JndiTemplate jndiTemplate = jndiChannelKey.getJndiAccessor().getJndiTemplate();
            Object jndiObject = jndiTemplate.lookup(userTransactionJndiName);
            if (jndiObject == null) {
                throw new NameNotFoundException("JNDI object with [" + userTransactionJndiName + "] not found: JNDI implementation returned null");
            }
            if (!UserTransaction.class.isInstance(jndiObject)) {
                throw new TypeMismatchNamingException(userTransactionJndiName, UserTransaction.class, jndiObject.getClass());
            }
            String msg = "JTA UserTransaction found at JNDI location [" + userTransactionJndiName + "]";
            lf.finer(msg, CLASS_NAME, "lookupUserTransaction");
            jndiChannelKey.setUserTransactionName(userTransactionJndiName);
            jndiChannelKey.setUserTransaction((UserTransaction) jndiObject);
            return true;
        } catch (NamingException ex) {
            String errMsg = "JTA UserTransaction is not available at JNDI location [" + userTransactionJndiName + "]";
            if (usingDefault) {
                lf.finer(errMsg, CLASS_NAME, "lookupUserTransaction");
                return false;
            }
            lf.warning(errMsg, CLASS_NAME, "lookupUserTransaction");
            throw new XAwareException(errMsg, ex);
        }
    }

    /**
     * Look up the JTA TransactionManager in JNDI using the name provided 
     * or by trying a series of commonly used names found in 
     * JtaTransactionManager.FALLBACK_TRANSACTION_MANAGER_NAMES.
     * 
     * @param jndiChannelKey
     *            The key object providing the JNDI name and the Context object
     *            to perform the lookup.
     * @return a boolean indicating whether the lookup was successful.
     * @throws XAwareException
     *             if the transactionManagerName is specified but the 
     *             TransactionManager object cannot be found via JNDI.
     */
    private static boolean lookupTransactionManager(JndiChannelKey jndiChannelKey) throws XAwareException {
        if (jndiChannelKey.getTransactionManager() != null) {
            return true;
        }
        UserTransaction ut = jndiChannelKey.getUserTransaction();
        if (ut instanceof TransactionManager) {
            lf.finer("JTA UserTransaction object [" + ut + "] implements TransactionManager");
            jndiChannelKey.setTransactionManager((TransactionManager) ut);
            jndiChannelKey.setTransactionManagerName(jndiChannelKey.getUserTransactionName());
            return true;
        }
        JndiTemplate jndiTemplate = jndiChannelKey.getJndiAccessor().getJndiTemplate();
        String transactionManagerJndiName = jndiChannelKey.getTransactionManagerName();
        if (transactionManagerJndiName != null && transactionManagerJndiName.trim().length() > 0) {
            try {
                lf.finer("Retrieving JTA TransactionManager from JNDI location [" + transactionManagerJndiName + "]");
                Object jndiObject = jndiTemplate.lookup(transactionManagerJndiName);
                if (jndiObject == null) {
                    String errMsg = "JNDI object with [" + transactionManagerJndiName + "] not found: JNDI implementation returned null";
                    lf.warning(errMsg, CLASS_NAME, "lookupTransactionManger");
                    throw new NameNotFoundException(errMsg);
                }
                if (!UserTransaction.class.isInstance(jndiObject)) {
                    throw new TypeMismatchNamingException(transactionManagerJndiName, TransactionManager.class, jndiObject.getClass());
                }
                String msg = "JTA TransactionManager found at JNDI location [" + transactionManagerJndiName + "]";
                lf.finer(msg, CLASS_NAME, "lookupTransactionManger");
                jndiChannelKey.setTransactionManagerName(transactionManagerJndiName);
                jndiChannelKey.setTransactionManager((TransactionManager) jndiObject);
                return true;
            } catch (NamingException ex) {
                String errMsg = "JTA TransactionManager is not available at JNDI location [" + transactionManagerJndiName + "]";
                lf.warning(errMsg, CLASS_NAME, "lookupTransactionManger");
                throw new XAwareException(errMsg, ex);
            }
        }
        for (String jndiName : JtaTransactionManager.FALLBACK_TRANSACTION_MANAGER_NAMES) {
            try {
                Object jndiObject = jndiTemplate.lookup(jndiName);
                if ((jndiObject != null) && (TransactionManager.class.isInstance(jndiObject))) {
                    String msg = "JTA TransactionManager found at JNDI location [" + jndiName + "]";
                    lf.finer(msg, CLASS_NAME, "lookupTransactionManger");
                    jndiChannelKey.setTransactionManagerName(jndiName);
                    jndiChannelKey.setTransactionManager((TransactionManager) jndiObject);
                    return true;
                }
            } catch (NamingException ex) {
                continue;
            }
        }
        lf.finer("No JTA TransactionManager found at any fallback JNDI location.", CLASS_NAME, "lookupTransactionManger");
        jndiChannelKey.setTransactionManagerName(null);
        return false;
    }

    /**
     * Adds the transactionalChannel instance provided into the transaction
     * scoped cache.
     * 
     * @param key
     *            The key for the specific transactional channel instance
     *            desired.
     * @param transactionalChannel
     *            the transactionalChannel instance to be stored in transaction
     *            scoped cache.
     * @throws XAwareException
     *             if any error occurs enlisting the specified channel in a
     *             transaction.
     */
    public void setTransactionalChannel(IChannelKey key, ITransactionalChannel transactionalChannel) throws XAwareException {
        ITransactionalChannel.Type transactionalChannelType = transactionalChannel.getTransactionalChannelType();
        if (transactionalChannelType == ITransactionalChannel.Type.LOCAL_JDBC) {
            createNewLocalTransaction(transactionalChannel);
        } else {
            enlistResourceInJtaTransaction(key, transactionalChannel);
        }
        managedChannels.put(key, transactionalChannel);
    }

    /**
     * Creates a lower-level transaction to manage a local channel. Gets the
     * local TransactionManager from the channel itself.
     * 
     * @param transactionalChannel
     *            the channel to enlist in the newly created lower-level
     *            transaction.
     */
    private void createNewLocalTransaction(ITransactionalChannel transactionalChannel) {
        LocalTransactionState localTransactionState = new LocalTransactionState();
        localTransactionState.transactionManager = transactionalChannel.getLocalTransactionManager();
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(definition);
        transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        localTransactionState.transactionStatus = localTransactionState.transactionManager.getTransaction(definition);
        assert (localTransactionState.transactionStatus.isNewTransaction());
        embeddedLocalTransactions.add(localTransactionState);
        localTransactionState.synchronizedChannel = transactionalChannel;
        lf.fine("Creating new lower-level local transaction for " + transactionalChannel.getTransactionalChannelType() + " channel.", CLASS_NAME, "createNewLocalTransaction");
    }

    /**
     * Creates a new lower-level JTA transaction to match the key
     * provided if one does not already exist.
     * 
     * @param key
     *            The key for the specific transactional channel instance
     *            desired.
     */
    private void createJtaTransactionIfNeeded(IChannelKey key) throws XAwareException {
        resolveUserTransactionAndTransactionManager((JndiChannelKey) key);
        JndiChannelKey jndiChannelKey = (JndiChannelKey) key;
        for (DistributedTransactionState jtaTransaction : embeddedJtaTransactions) {
            if (JndiChannelKey.providerUrlsEqual(jtaTransaction.jndiChannelKey, jndiChannelKey)) {
                if (!equalsOrBothNull(jtaTransaction.transactionManager.getUserTransactionName(), jndiChannelKey.getUserTransactionName())) {
                    String errMsg = "Error -- cannot support simultaneous JTA transactions from the same server but with different UserTransactions";
                    lf.warning(errMsg, CLASS_NAME, "createJtaTransactionIfNeeded");
                    throw new XAwareTransactionException(errMsg);
                }
                if (!equalsOrBothNull(jtaTransaction.transactionManager.getTransactionManagerName(), jndiChannelKey.getTransactionManagerName())) {
                    String errMsg = "Error -- cannot support simultaneous JTA transactions from the same server but with different TransactionManagers";
                    lf.warning(errMsg, CLASS_NAME, "createJtaTransactionIfNeeded");
                    throw new XAwareTransactionException(errMsg);
                }
                return;
            }
        }
        DistributedTransactionState newJtaTransaction = new DistributedTransactionState();
        if (jndiChannelKey.getUserTransaction() == null) {
            if (jndiChannelKey.getTransactionManager() == null) {
                String errMsg = "Error -- cannot create a lower-level JTA transaction without either a UserTransaction or a TransactionManager.";
                lf.warning(errMsg, CLASS_NAME, "createJtaTransactionIfNeeded");
                throw new XAwareTransactionException(errMsg);
            }
            newJtaTransaction.transactionManager = new XAwareJtaTransactionManager(jndiChannelKey.getTransactionManager(), jndiChannelKey.getTransactionManagerName());
        } else if (jndiChannelKey.getTransactionManager() == null) {
            newJtaTransaction.transactionManager = new XAwareJtaTransactionManager(jndiChannelKey.getUserTransaction(), jndiChannelKey.getUserTransactionName());
        } else {
            newJtaTransaction.transactionManager = new XAwareJtaTransactionManager(jndiChannelKey.getUserTransaction(), jndiChannelKey.getUserTransactionName(), jndiChannelKey.getTransactionManager(), jndiChannelKey.getTransactionManagerName());
        }
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(definition);
        transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        newJtaTransaction.transactionStatus = newJtaTransaction.transactionManager.getTransaction(definition);
        newJtaTransaction.jndiChannelKey = jndiChannelKey;
        embeddedJtaTransactions.add(newJtaTransaction);
        lf.fine("Creating new lower-level JTA transaction.", CLASS_NAME, "createJtaTransactionIfNeeded");
    }

    /**
     * Enlists the channel provided with the appropriate lower-level JTA
     * transaction.
     * 
     * @param key
     *            The key for the specific transactional channel instance
     *            desired.
     * @param transactionalChannel
     *            the channel to enlist in the lower-level JTA transaction.
     */
    private void enlistResourceInJtaTransaction(IChannelKey key, ITransactionalChannel transactionalChannel) throws XAwareException {
        JndiChannelKey jndiChannelKey = (JndiChannelKey) key;
        for (DistributedTransactionState jtaTransaction : embeddedJtaTransactions) {
            if (JndiChannelKey.providerUrlsEqual(jtaTransaction.jndiChannelKey, jndiChannelKey) && equalsOrBothNull(jtaTransaction.transactionManager.getUserTransactionName(), jndiChannelKey.getUserTransactionName()) && equalsOrBothNull(jtaTransaction.transactionManager.getTransactionManagerName(), jndiChannelKey.getTransactionManagerName())) {
                if (jtaTransaction.synchronizedChannels.contains(transactionalChannel)) {
                    lf.debug(transactionalChannel.getTransactionalChannelType() + " channel already enlisted in lower-level JTA transaction.", CLASS_NAME, "enlistResourceInJtaTransaction");
                    return;
                }
                lf.finer("Enlisting " + transactionalChannel.getTransactionalChannelType() + " channel in lower-level JTA transaction.", CLASS_NAME, "enlistResourceInJtaTransaction");
                jtaTransaction.synchronizedChannels.add(transactionalChannel);
                return;
            }
        }
        String errMsg = "Error -- JTA transaction not found for " + transactionalChannel.getTransactionalChannelType() + " channel.";
        lf.warning(errMsg, CLASS_NAME, "enlistResourceInJtaTransaction");
        throw new XAwareTransactionException(errMsg);
    }

    /**
     * Checks for equality of two objects, allowing for either to be null.
     * 
     * @param obj1
     *            the first object to check
     * @param obj2
     *            the second  object to check
     * @return true if the objects provided are both null or if they are both
     *         non-null and equal.
     */
    private static boolean equalsOrBothNull(Object obj1, Object obj2) {
        if (obj1 == null) {
            return (obj2 == null);
        }
        if (obj2 == null) {
            return false;
        }
        return obj1.equals(obj2);
    }

    /**
     * Perform an actual rollback of the master transaction represented by
     * this MasterTransactionCoordinator.
     * <p>
     * This implementation attempts to perform a coordinated rollback across
     * all of the lower-level transactions enlisted with the master
     * transaction.
     * <p>
     * It first invokes the beforeCompletion() Synchronization callback for
     * all transactional channels enlisted with each lower-level
     * transaction. Then it attempts to rollback each lower-level
     * transaction. Finally it invokes the afterCompletion() Synchronization
     * callback for all transactional channels enlisted with each
     * lower-level transaction.
     * 
     * @throws TransactionException
     *             in case of system errors. This may be a
     *             HeuristicCompletionException if some lower-level
     *             transactions failed to rollback successfully.
     */
    public void doRollback() throws TransactionException {
        try {
            doBeforeCompletionSynchronizations();
        } finally {
            int status = TransactionSynchronization.STATUS_ROLLED_BACK;
            try {
                boolean rolledBackConsistently = rollbackEnlistedTransactions();
                if (!rolledBackConsistently) {
                    status = TransactionSynchronization.STATUS_UNKNOWN;
                }
            } finally {
                doAfterCompletionSynchronizations(status);
            }
        }
    }

    /**
     * Perform an actual commit of the master transaction represented by
     * this MasterTransactionCoordinator.
     * <p>
     * This implementation attempts to perform a coordinated commit across all
     * of the lower-level transactions enlisted with the master transaction.
     * <p>
     * It first invokes the beforeCompletion() Synchronization callback for all
     * transactional channels enlisted with each lower-level transaction. Then
     * it attempts to commit each lower-level transaction. Finally it invokes
     * the afterCompletion() Synchronization callback for all transactional
     * channels enlisted with each lower-level transaction.
     * <p>
     * If any of the lower-level transactions fails to commit, then any
     * remaining lower-level transactions which have not yet been committed
     * will be rolled back.
     * 
     * @throws TransactionException
     *             in case of commit or system errors. This may be a
     *             HeuristicCompletionException if some lower-level transactions
     *             committed but others failed to commit and were rolled back.
     *             It may also be an UnexpectedRollbackException if none of the
     *             lower-level transactions committed and the entire transaction
     *             rolled back.
     */
    public void doCommit() throws TransactionException {
        int status = TransactionSynchronization.STATUS_COMMITTED;
        try {
            doBeforeCommitSynchronizations();
        } finally {
            try {
                doBeforeCompletionSynchronizations();
            } finally {
                try {
                    if (isRollbackOnly()) {
                        boolean rolledBackConsistently = rollbackEnlistedTransactions();
                        if (rolledBackConsistently) {
                            status = TransactionSynchronization.STATUS_ROLLED_BACK;
                        } else {
                            status = TransactionSynchronization.STATUS_UNKNOWN;
                        }
                        throw new UnexpectedRollbackException("Failed to commit, transaction marked setRollbackOnly.");
                    } else {
                        try {
                            commitEnlistedTransactions();
                        } catch (UnexpectedRollbackException e) {
                            status = TransactionSynchronization.STATUS_ROLLED_BACK;
                            throw e;
                        } catch (HeuristicCompletionException e) {
                            status = TransactionSynchronization.STATUS_UNKNOWN;
                            throw e;
                        }
                    }
                } finally {
                    doAfterCompletionSynchronizations(status);
                }
            }
        }
    }

    /**
     * Apply beforeCommit() synchronization callbacks to all
     * transactional channels enlisted in the master transaction.
     * <p>
     * Any exceptions that result will trigger setRollbackOnly on the master
     * transaction.
     */
    private void doBeforeCommitSynchronizations() {
        for (DistributedTransactionState jtaTransaction : embeddedJtaTransactions) {
            for (ITransactionalChannel transactionalChannel : jtaTransaction.synchronizedChannels) {
                try {
                    transactionalChannel.beforeCommit(false);
                } catch (Exception e) {
                    lf.warning("Error occurred in beforeCommit of embeddedJtaTransaction", CLASS_NAME, "doBeforeCommitSynchronizations", e);
                    jtaTransaction.transactionStatus.setRollbackOnly();
                }
            }
        }
        for (LocalTransactionState localTransaction : embeddedLocalTransactions) {
            try {
                localTransaction.synchronizedChannel.beforeCommit(false);
            } catch (Exception e) {
                lf.warning("Error occurred in beforeCommit of a local transaction", CLASS_NAME, "doBeforeCommitSynchronizations", e);
                localTransaction.transactionStatus.setRollbackOnly();
            }
        }
    }

    /**
     * Apply beforeCompletion() synchronization callbacks to all
     * transactional channels enlisted in the master transaction.
     * <p>
     * Any exceptions that result will be logged but will not trigger
     * setRollbackOnly on the master transaction.
     */
    private void doBeforeCompletionSynchronizations() {
        for (DistributedTransactionState jtaTransaction : embeddedJtaTransactions) {
            for (ITransactionalChannel transactionalChannel : jtaTransaction.synchronizedChannels) {
                try {
                    transactionalChannel.beforeCompletion();
                } catch (Exception e) {
                    lf.warning("Error occurred in beforeCompletion of embeddedJtaTransaction", CLASS_NAME, "doBeforeCompletionSynchronizations", e);
                }
            }
        }
        for (LocalTransactionState localTransaction : embeddedLocalTransactions) {
            try {
                localTransaction.synchronizedChannel.beforeCompletion();
            } catch (Exception e) {
                lf.warning("Error occurred in beforeCompletion of a local transaction", CLASS_NAME, "doBeforeCompletionSynchronizations", e);
            }
        }
    }

    /**
     * Performs the actual rollback on all lower-level transactions
     * enlisted with the master transaction.
     * 
     * @return a boolean indicating whether the rollback was successfully
     *         applied across all lower-level transactions
     */
    private boolean rollbackEnlistedTransactions() {
        boolean success = true;
        for (DistributedTransactionState jtaTransaction : embeddedJtaTransactions) {
            try {
                jtaTransaction.transactionManager.rollback(jtaTransaction.transactionStatus);
            } catch (Exception e) {
                lf.warning("Enlisted distributed transaction failed to rollback: ", CLASS_NAME, "rollbackEnlistedTransactions", e);
                success = false;
            }
        }
        for (LocalTransactionState localTransaction : embeddedLocalTransactions) {
            try {
                localTransaction.transactionManager.rollback(localTransaction.transactionStatus);
            } catch (Exception e) {
                lf.warning("Enlisted local transaction failed to rollback: ", CLASS_NAME, "rollbackEnlistedTransactions", e);
                success = false;
            }
        }
        return success;
    }

    /**
     * Performs the actual commit on all lower-level transactions enlisted
     * with the master transaction.
     *
     * @throws TransactionException
     *             in case of commit or system errors. This may be a
     *             HeuristicCompletionException if some lower-level
     *             transactions committed but others failed to commit and
     *             were rolled back. It may also be an
     *             UnexpectedRollbackException if none of the lower-level
     *             transactions committed and the entire transaction rolled
     *             back.
     */
    private void commitEnlistedTransactions() throws TransactionException {
        boolean rollback = false;
        boolean committedSome = false;
        Throwable rollbackCause = null;
        for (DistributedTransactionState jtaTransaction : embeddedJtaTransactions) {
            if (rollback) {
                try {
                    jtaTransaction.transactionManager.rollback(jtaTransaction.transactionStatus);
                } catch (Exception e) {
                    lf.warning("Enlisted JTA transaction failed to rollback: ", CLASS_NAME, "commitEnlistedTransactions", e);
                }
            } else {
                try {
                    jtaTransaction.transactionManager.commit(jtaTransaction.transactionStatus);
                    committedSome = true;
                } catch (Exception e) {
                    lf.warning("Enlisted JTA transaction failed to commit: ", CLASS_NAME, "commitEnlistedTransactions", e);
                    rollback = true;
                    rollbackCause = e;
                }
            }
        }
        for (LocalTransactionState localTransaction : embeddedLocalTransactions) {
            if (rollback) {
                try {
                    localTransaction.transactionManager.rollback(localTransaction.transactionStatus);
                } catch (Exception e) {
                    lf.warning("Enlisted transaction failed to rollback: ", CLASS_NAME, "commitEnlistedTransactions", e);
                }
            } else {
                try {
                    localTransaction.transactionManager.commit(localTransaction.transactionStatus);
                    committedSome = true;
                } catch (Exception e) {
                    lf.warning("Enlisted transaction failed to commit: ", CLASS_NAME, "commitEnlistedTransactions", e);
                    rollback = true;
                    rollbackCause = e;
                }
            }
        }
        if (rollback) {
            if (committedSome) {
                throw new HeuristicCompletionException(HeuristicCompletionException.STATE_MIXED, rollbackCause);
            } else {
                throw new UnexpectedRollbackException("Commit failed, rolled back transaction instead.", rollbackCause);
            }
        }
    }

    /**
     * Apply afterCompletion() synchronization callbacks to all transactional
     * channels enlisted in the master transaction.
     * <p>
     * Any exceptions that result will only be logged.
     * 
     * @param status
     *            the TransactionSynchronization status value indicating the
     *            status of the transaction completion.
     */
    private void doAfterCompletionSynchronizations(int status) {
        for (DistributedTransactionState jtaTransaction : embeddedJtaTransactions) {
            doJtaAfterCompletionSynchronizations(status, jtaTransaction);
        }
        for (LocalTransactionState localTransaction : embeddedLocalTransactions) {
            try {
                localTransaction.synchronizedChannel.afterCompletion(status);
            } catch (Exception e) {
                lf.warning("Transactional channel failed in afterCompletion: ", CLASS_NAME, "doAfterCompletionSynchronizations", e);
            }
        }
    }

    /**
     * Apply afterCompletion() synchronization callbacks to all transactional
     * channels enlisted in a JTA transaction.  It the JTA transaction is not a new
     * transaction, then attempt is made to register the transaction synchronizations
     * with the TransactionManager which owns the UserTransaction, so that the
     * synchronization callbacks will be performed when the UserTransaction actually
     * commits.
     * <p>
     * Any exceptions that result will only be logged.
     * <p>
     * This method implementation is an adaptation of code from Spring's JtaTransactionManager.
     * This class manages its own synchronized resources explicitly, not making use of
     * Spring's transaction synchronization. 
     * 
     * @param status
     *            the TransactionSynchronization status value indicating the
     *            status of the transaction completion.
     * @param jtaTransaction
     *            the DistributedTransactionState of the JTA transaction being completed.
     */
    private void doJtaAfterCompletionSynchronizations(int status, DistributedTransactionState jtaTransaction) {
        if (!jtaTransaction.transactionStatus.isNewTransaction()) {
            TransactionManager jtaTransactionManager = jtaTransaction.transactionManager.getTransactionManager();
            if (jtaTransactionManager != null) {
                try {
                    Transaction transaction = jtaTransactionManager.getTransaction();
                    if (transaction != null) {
                        transaction.registerSynchronization(new JtaAfterCompletionSynchronization(jtaTransaction.synchronizedChannels));
                        return;
                    } else {
                        lf.info("Participating in existing JTA transaction, but no current JTA Transaction available: " + "cannot register Spring after-completion callbacks with outer JTA transaction - " + "processing Spring after-completion callbacks with outcome status 'unknown'", CLASS_NAME, "doAfterCompletionSynchronizations");
                        status = TransactionSynchronization.STATUS_UNKNOWN;
                    }
                } catch (RollbackException ex) {
                    lf.info("Participating in existing JTA transaction that has been marked rollback-only: " + "cannot register Spring after-completion callbacks with outer JTA transaction - " + "immediately performing Spring after-completion callbacks with outcome status 'rollback'", CLASS_NAME, "doAfterCompletionSynchronizations");
                    status = TransactionSynchronization.STATUS_ROLLED_BACK;
                } catch (IllegalStateException ex) {
                    throw new NoTransactionException("No active JTA transaction");
                } catch (SystemException ex) {
                    throw new TransactionSystemException("JTA failure on registerSynchronization", ex);
                }
            } else {
                lf.info("Participating in existing JTA transaction, but no JTA TransactionManager available: " + "cannot register Spring after-completion callbacks with outer JTA transaction - " + "processing Spring after-completion callbacks with outcome status 'unknown'", CLASS_NAME, "doAfterCompletionSynchronizations");
                status = TransactionSynchronization.STATUS_UNKNOWN;
            }
        }
        for (ITransactionalChannel transactionalChannel : jtaTransaction.synchronizedChannels) {
            try {
                transactionalChannel.afterCompletion(status);
            } catch (Exception e) {
                lf.warning("Transactional channel failed in afterCompletion: ", CLASS_NAME, "doAfterCompletionSynchronizations", e);
            }
        }
    }

    /**
     * Suspends the master transaction by suspending all lower-level
     * transactions enlisted with the master transaction.
     * <p>
     * There is no assurance that the group of transactional channels used in
     * the suspended master transaction will match the new master transaction
     * that replaces it. Also, the lower-level transactions that will be
     * embedded within a master transaction are not known at the time that the
     * master transaction begins; these are created dynamically as new
     * transactional channels are enlisted. Therefore, lower-level transactions
     * are not created based on the propagation levels in the xa:transaction
     * attribute. Instead, lower-level transactions begin when their channels
     * are enlisted via setTransactionalChannel(), and end when the master
     * transaction commits or rolls back.
     * <p>
     * When a master transaction is suspended, each lower-level transction is
     * parked by using its transaction manager to begin a new dummy inactive
     * transaction using PROPAGATION_NOT_SUPPORTED. The new dummy transaction is
     * never used. When the master transaction is resumed, each dummy
     * lower-level transaction is rolled back, which causes each corresponding
     * parked lower-level transaction to be resumed.
     * <p>
     * It is possible that one of the transactional channels which was used in
     * the suspended master transaction is enlisted in the new master
     * transaction which replaces it. When this occurs, the creation of the new
     * lower-level transaction for that channel will cause the previously
     * created dummy lower-level transaction to be suspended. Then when the new
     * master transaction completes, this dummy lower-level transaction will be
     * resumed. Soon thereafter the previous master transaction will be resumed,
     * which will cause this dummy lower-level transaction to be rolled back,
     * which will trigger the resumption of the corresponding parked lower-level
     * transaction.
     * 
     * @throws TransactionException
     *             if any error occurs suspending any of the enlisted
     *             lower-level transactions.
     */
    public void doSuspend() {
        Exception parkedException = null;
        TransactionDefinition suspensionTriggeringXactDefn = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        for (DistributedTransactionState jtaTransaction : embeddedJtaTransactions) {
            try {
                jtaTransaction.statusFromSuspend = jtaTransaction.transactionManager.getTransaction(suspensionTriggeringXactDefn);
            } catch (Exception e) {
                lf.warning("Failed to suspend lower-level distributed transaction", CLASS_NAME, "doSuspend", e);
                parkedException = e;
            }
        }
        for (LocalTransactionState localTransaction : embeddedLocalTransactions) {
            try {
                localTransaction.statusFromSuspend = localTransaction.transactionManager.getTransaction(suspensionTriggeringXactDefn);
            } catch (Exception e) {
                lf.warning("Failed to suspend lower-level local transaction", CLASS_NAME, "doSuspend", e);
                parkedException = e;
            }
        }
        if (parkedException != null) {
            String msg = "Failed to suspend master transaction";
            lf.warning(msg, CLASS_NAME, "doSuspend");
            throw new TransactionSuspensionNotSupportedException(msg, parkedException);
        }
    }

    /**
     * Resumes the master transaction by completing the each of the dummy
     * lower-level transactions that were begun when the master transaction was
     * suspended. This triggers each of the corresponding parked lower-level
     * transactions to be resumed.
     * 
     * @throws TransactionException
     *             if any error occurs resuming any of the enlisted lower-level
     *             transactions.
     * 
     * @see MasterTransactionCoordinator#doSuspend()
     */
    public void doResume() {
        Exception parkedException = null;
        for (DistributedTransactionState jtaTransaction : embeddedJtaTransactions) {
            try {
                jtaTransaction.transactionManager.rollback(jtaTransaction.statusFromSuspend);
            } catch (Exception e) {
                parkedException = e;
            }
        }
        for (LocalTransactionState localTransaction : embeddedLocalTransactions) {
            try {
                localTransaction.transactionManager.rollback(localTransaction.statusFromSuspend);
            } catch (Exception e) {
                parkedException = e;
            }
        }
        if (parkedException != null) {
            String msg = "Failed to resume master transaction";
            lf.warning(msg, CLASS_NAME, "doResume");
            throw new TransactionSystemException(msg, parkedException);
        }
    }

    /**
     * Return whether the transaction is internally marked as rollback-only.
     * Can, for example, check the JTA UserTransaction.
     * 
     * @return whether the transaction is internally marked as rollback-only.
     */
    public boolean isRollbackOnly() {
        if (isRollbackOnly) {
            return true;
        }
        for (DistributedTransactionState jtaTransaction : embeddedJtaTransactions) {
            if (jtaTransaction.transactionStatus.isRollbackOnly()) {
                return true;
            }
        }
        for (LocalTransactionState localTransaction : embeddedLocalTransactions) {
            if (localTransaction.transactionStatus.isRollbackOnly()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set rollback only on the master transaction.  Also set it
     * on each enlisted transaction.
     */
    public void setRollbackOnly() {
        isRollbackOnly = true;
        for (DistributedTransactionState jtaTransaction : embeddedJtaTransactions) {
            jtaTransaction.transactionStatus.setRollbackOnly();
        }
        for (LocalTransactionState localTransaction : embeddedLocalTransactions) {
            localTransaction.transactionStatus.setRollbackOnly();
        }
    }

    /**
     * @see org.springframework.transaction.SavepointManager#createSavepoint()
     */
    public Object createSavepoint() throws TransactionException {
        if (embeddedJtaTransactions.size() > 0) {
            throw new NestedTransactionNotSupportedException("Nested transactions are not supported with distributed channels");
        }
        for (LocalTransactionState localTransaction : embeddedLocalTransactions) {
            SavepointManager savepointManager = getSavepointManager(localTransaction);
            localTransaction.savepoint = savepointManager.createSavepoint();
        }
        return this;
    }

    /**
     * @see org.springframework.transaction.SavepointManager#rollbackToSavepoint(java.lang.Object)
     */
    public void rollbackToSavepoint(Object savepoint) throws TransactionException {
        if (embeddedJtaTransactions.size() > 0) {
            throw new NestedTransactionNotSupportedException("Nested transactions are not supported with distributed channels");
        }
        for (LocalTransactionState localTransaction : embeddedLocalTransactions) {
            SavepointManager savepointManager = getSavepointManager(localTransaction);
            savepointManager.rollbackToSavepoint(localTransaction.savepoint);
        }
    }

    /**
     * @see org.springframework.transaction.SavepointManager#releaseSavepoint(java.lang.Object)
     */
    public void releaseSavepoint(Object savepoint) throws TransactionException {
        if (embeddedJtaTransactions.size() > 0) {
            throw new NestedTransactionNotSupportedException("Nested transactions are not supported with distributed channels");
        }
        for (LocalTransactionState localTransaction : embeddedLocalTransactions) {
            SavepointManager savepointManager = getSavepointManager(localTransaction);
            savepointManager.releaseSavepoint(localTransaction.savepoint);
        }
    }

    private SavepointManager getSavepointManager(LocalTransactionState localTransaction) {
        TransactionStatus txStatus = localTransaction.transactionStatus;
        if (!(txStatus instanceof DefaultTransactionStatus)) {
            throw new NestedTransactionNotSupportedException("Transaction Manager [" + localTransaction.transactionManager + "] does not support savepoints");
        }
        DefaultTransactionStatus dfltTxStatus = (DefaultTransactionStatus) txStatus;
        Object transaction = dfltTxStatus.getTransaction();
        if (!dfltTxStatus.isTransactionSavepointManager()) {
            throw new NestedTransactionNotSupportedException("Transaction object [" + transaction + "] does not support savepoints");
        }
        return (SavepointManager) transaction;
    }
}
