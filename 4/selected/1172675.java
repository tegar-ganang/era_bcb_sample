package com.genia.toolbox.spring.transaction.bean.impl;

import java.util.ConcurrentModificationException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import com.genia.toolbox.basics.exception.technical.TechnicalException;
import com.genia.toolbox.spring.transaction.bean.EnlistableResource;

/**
 * A list that participate to transactions.
 * 
 * @param <E>
 *          the type of the object to wrapp
 */
public abstract class AbstractEnlistableResource<E> implements EnlistableResource {

    /**
   * this class represents the default view of a transactional object.
   */
    public class TransactionalView {

        /**
     * set the value of the transactional object in the current context.
     * 
     * @param object
     *          the new value of the object
     */
        public void setObject(E object) {
            if (baseObject == object) {
                return;
            }
            if (getWritingLock()) {
                releaseWritingLock();
            }
            baseObject = object;
        }

        /**
     * start a new transaction for the current object.
     * 
     * @param definition
     *          {@link TransactionDefinition} instance (can be <code>null</code>
     *          for defaults), describing propagation behavior, isolation level,
     *          timeout etc
     * @param status
     *          the {@link TransactionStatus} of the transaction
     */
        private void startTransaction(TransactionDefinition definition, TransactionStatus status) {
            transactionalViews.set(new TransactionalViewInTransaction(this, definition, status, getObjectForReading()));
        }

        /**
     * commit the transaction defined by {@link TransactionStatus}. A
     * {@link #prepare(TransactionStatus)} must have been called.
     * 
     * @param status
     *          the {@link TransactionStatus} of the transaction
     * @throws TechnicalException
     *           if an error occurred
     */
        protected void commit(TransactionStatus status) throws TechnicalException {
            throw new TechnicalException(new IllegalTransactionStateException("Trying to commit a non-existent transaction."));
        }

        /**
     * returns the object representing the current state of the transactional
     * object, but only read method must be called on this element. This allows
     * to implement copy-on-write comportment.
     * 
     * @return the object representing the current state of the transactional
     *         object, but only read method must be called on this element
     */
        protected E getObjectForReading() {
            return baseObject;
        }

        /**
     * returns the object representing the current state of the transactional
     * object. Write method can be called on this element, so a writing lock is
     * acquired.
     * 
     * @return the object representing the current state of the transactional
     *         object
     */
        protected E getObjectForWriting() {
            if (getWritingLock()) {
                releaseWritingLock();
            }
            baseObject = cloneObjectForTransaction(baseObject);
            return baseObject;
        }

        /**
     * returns how much time to wait for a lock in second.
     * 
     * @return how much time to wait for a lock in second
     */
        protected int getTimeout() {
            return DEFAULT_TIMEOUT;
        }

        /**
     * try to obtain a lock for writing to the object. Only one thread can
     * obtain such a lock. If another thread keep the lock for longer than the
     * timeout, this method will fail returning <code>false</code>.
     * 
     * @return whether the lock has been obtained
     */
        protected boolean getWritingLock() {
            synchronized (lock) {
                long time = System.currentTimeMillis();
                while (Thread.currentThread() != writerThread && ((System.currentTimeMillis() - time) < (1000l * getTimeout()))) {
                    if (writerThread == null || !writerThread.isAlive()) {
                        writerThread = Thread.currentThread();
                    } else {
                        try {
                            lock.wait(1000l * getTimeout());
                        } catch (InterruptedException e) {
                        }
                    }
                }
                return Thread.currentThread() == writerThread;
            }
        }

        /**
     * prepare the transaction defined by {@link TransactionStatus}. If this
     * method succeed, a {@link #commit(TransactionStatus)} must succeed or the
     * transactional integrity of the application won't be insured.
     * 
     * @param status
     *          the {@link TransactionStatus} of the transaction
     * @throws TechnicalException
     *           if an error occurred
     */
        protected void prepare(TransactionStatus status) throws TechnicalException {
            throw new TechnicalException(new IllegalTransactionStateException("Trying to prepare a non-existent transaction."));
        }

        /**
     * propagate the new value of the object to the current transactional view.
     * 
     * @param newValue
     *          the new value of this transactional view
     */
        protected void propagateNewValue(E newValue) {
            baseObject = newValue;
        }

        /**
     * release the writing lock, if the current thread posses it.
     */
        protected void releaseWritingLock() {
            synchronized (lock) {
                if (Thread.currentThread() == writerThread) {
                    writerThread = null;
                }
                lock.notifyAll();
            }
        }

        /**
     * resume the current {@link TransactionalView} from the
     * {@link TransactionalView} given in parameter.
     * 
     * @param transactionalView
     *          the {@link TransactionalView} to resume from
     * @param commit
     *          whether to commit the new value or to rollback it
     */
        protected void resumeFrom(TransactionalView transactionalView, boolean commit) {
            transactionalViews.set(this);
            if (commit) {
                propagateNewValue(transactionalView.getObjectForReading());
            }
            if (getObjectForReading() == baseObject) {
                releaseWritingLock();
            }
        }

        /**
     * rollback the transaction defined by {@link TransactionStatus}.
     * 
     * @param status
     *          the {@link TransactionStatus} of the transaction
     * @throws TechnicalException
     *           if an error occurred
     */
        protected void rollback(TransactionStatus status) throws TechnicalException {
            throw new TechnicalException(new IllegalTransactionStateException("Trying to rollback a non-existent transaction."));
        }
    }

    /**
   * this class represents the view of a transactional object inside a
   * transactions.
   */
    private class TransactionalViewInTransaction extends TransactionalView {

        /**
     * {@link TransactionDefinition} instance (can be <code>null</code> for
     * defaults), describing propagation behavior, isolation level, timeout etc.
     */
        private final transient TransactionDefinition definition;

        /**
     * the current value of the object for this transaction, or
     * <code>null</code> if the value did not change since the begining of the
     * transaction.
     */
        private transient E newObject;

        /**
     * Wether a write action occurred or not.
     */
        private transient boolean hasBeenWritten;

        /**
     * the value of the object when this transaction started.
     */
        private final transient E startingObject;

        /**
     * the {@link TransactionStatus} of the transaction.
     */
        private final transient TransactionStatus status;

        /**
     * the {@link TransactionalView} surrounding this view.
     */
        private final transient TransactionalView surrondingTransactionView;

        /**
     * constructor.
     * 
     * @param surrondingTransactionView
     *          the {@link TransactionalView} surrounding this view.
     * @param definition
     *          {@link TransactionDefinition} instance (can be <code>null</code>
     *          for defaults), describing propagation behavior, isolation level,
     *          timeout etc
     * @param status
     *          the {@link TransactionStatus} of the transaction
     * @param startingObject
     *          the value of the object when this transaction starts
     */
        public TransactionalViewInTransaction(TransactionalView surrondingTransactionView, TransactionDefinition definition, TransactionStatus status, E startingObject) {
            this.surrondingTransactionView = surrondingTransactionView;
            this.definition = definition;
            this.status = status;
            this.startingObject = startingObject;
            this.newObject = null;
            this.hasBeenWritten = false;
        }

        /**
     * set the value of the transactional object in the current context.
     * 
     * @param object
     *          the new value of the object
     * @see com.genia.toolbox.spring.transaction.bean.impl.AbstractEnlistableResource.TransactionalView#setObject(java.lang.Object)
     */
        @Override
        public void setObject(E object) {
            if (object == getObjectForReading()) {
                return;
            }
            if (!getWritingLock()) {
                status.setRollbackOnly();
            }
            setNewObject(object);
        }

        /**
     * chech that the transaction defined by {@link TransactionStatus} can be
     * prepared or commited. If this method succeed, a
     * {@link #commit(TransactionStatus)} must succeed or the transactional
     * integrity of the application won't be insured.
     * 
     * @param status
     *          the {@link TransactionStatus} of the transaction
     * @param action
     *          description of the action being made
     * @throws TechnicalException
     *           if an error occurred
     */
        private void checkForPrepareOrCommit(TransactionStatus status, String action) throws TechnicalException {
            if (status != this.status) {
                throw new TechnicalException(new IllegalTransactionStateException("Trying to " + action + " a transaction this is not the current one."));
            }
            if (status.isRollbackOnly()) {
                surrondingTransactionView.resumeFrom(this, false);
                throw new TechnicalException(new IllegalTransactionStateException("Trying to " + action + " a transaction that is rollback only."));
            }
            if (hasBeenWritten && surrondingTransactionView.getObjectForReading() != startingObject) {
                surrondingTransactionView.resumeFrom(this, false);
                throw new TechnicalException(new ConcurrentModificationException("Trying to modify the object while another modification happened while we were in this transaction."));
            }
        }

        /**
     * commit the transaction defined by {@link TransactionStatus}. A
     * {@link #prepare(TransactionStatus)} must have been called.
     * 
     * @param status
     *          the {@link TransactionStatus} of the transaction
     * @throws TechnicalException
     *           if an error occurred
     */
        @Override
        protected void commit(TransactionStatus status) throws TechnicalException {
            synchronized (lock) {
                checkForPrepareOrCommit(status, "commit");
                surrondingTransactionView.resumeFrom(this, hasBeenWritten);
            }
        }

        /**
     * returns the object representing the current state of the transactional
     * object, but only read method must be called on this element. This allows
     * to implement copy-on-write comportment.
     * 
     * @return the oject representing the current state of the transactional
     *         object, but only read method must be called on this element
     * @see com.genia.toolbox.basics.transaction.bean.impl.AbstractEnlistableResource.TransactionalView#getObjectForReading()
     */
        @Override
        protected E getObjectForReading() {
            if (!hasBeenWritten) {
                return startingObject;
            }
            return newObject;
        }

        /**
     * returns the object representing the current state of the transactional
     * object. Write method can be called on this element, so a writing lock is
     * acquired.
     * 
     * @return the object representing the current state of the transactional
     *         object
     * @see com.genia.toolbox.basics.transaction.bean.impl.AbstractEnlistableResource.TransactionalView#getObjectForWriting()
     */
        @Override
        protected E getObjectForWriting() {
            if (!hasBeenWritten) {
                if (!getWritingLock()) {
                    status.setRollbackOnly();
                }
                setNewObject(cloneObjectForTransaction(startingObject));
            }
            return newObject;
        }

        /**
     * returns how much time to wait for a lock in second.
     * 
     * @return how much time to wait for a lock in second
     */
        @Override
        protected int getTimeout() {
            if (definition == null || definition.getTimeout() == -1) {
                return DEFAULT_TIMEOUT;
            }
            return definition.getTimeout();
        }

        /**
     * prepare the transaction defined by {@link TransactionStatus}. If this
     * method succeed, a {@link #commit(TransactionStatus)} must succeed or the
     * transactional integrity of the application won't be insured.
     * 
     * @param status
     *          the {@link TransactionStatus} of the transaction
     * @throws TechnicalException
     *           if an error occurred
     */
        @Override
        protected void prepare(TransactionStatus status) throws TechnicalException {
            synchronized (lock) {
                checkForPrepareOrCommit(status, "prepare");
            }
        }

        /**
     * setter for the newObject property, it also keep hasBeenWritten
     * up-to-date.
     * 
     * @param newObject
     *          the new value of the object
     */
        private void setNewObject(E newObject) {
            hasBeenWritten = true;
            this.newObject = newObject;
        }

        /**
     * propagate the new value of the object to the current transactional view.
     * 
     * @param newValue
     *          the new value of this transactional view
     */
        @Override
        protected void propagateNewValue(E newValue) {
            if (hasBeenWritten || startingObject != newValue) {
                setNewObject(newValue);
            }
        }

        /**
     * rollback the transaction defined by {@link TransactionStatus}.
     * 
     * @param status
     *          the {@link TransactionStatus} of the transaction
     * @throws TechnicalException
     *           if an error occurred
     */
        @Override
        protected void rollback(TransactionStatus status) throws TechnicalException {
            synchronized (lock) {
                if (status != this.status) {
                    throw new TechnicalException(new IllegalTransactionStateException("Trying to rollback a transaction this is not the current one."));
                }
                surrondingTransactionView.resumeFrom(this, false);
            }
        }
    }

    /**
   * default time to wait for the writer lock before cancelling transactions.
   */
    public static final int DEFAULT_TIMEOUT = 60;

    /**
   * the current thread that does writing modifications.
   */
    private transient Thread writerThread = null;

    /**
   * the base object.
   */
    private transient E baseObject;

    /**
   * the lock object to use.
   */
    private final transient Object lock = new Object();

    /**
   * the local transactions views.
   */
    private final transient ThreadLocal<TransactionalView> transactionalViews = new ThreadLocal<TransactionalView>();

    /**
   * constructor.
   */
    public AbstractEnlistableResource() {
    }

    /**
   * constructor.
   * 
   * @param baseObject
   *          the initial value of the wrapped object.
   */
    public AbstractEnlistableResource(E baseObject) {
        this.baseObject = baseObject;
    }

    /**
   * clone the originalObject to generate a transactional view of it.
   * 
   * @param originalObject
   *          the object to clone
   * @return a clone of the originalObject.
   */
    public abstract E cloneObjectForTransaction(E originalObject);

    /**
   * commit the transaction defined by {@link TransactionStatus}. A
   * {@link #prepare(TransactionStatus)} must have been called.
   * 
   * @param status
   *          the {@link TransactionStatus} of the transaction
   * @throws TechnicalException
   *           if an error occurred
   * @see com.genia.toolbox.basics.transaction.bean.EnlistableResource#commit(org.springframework.transaction.TransactionStatus)
   */
    public void commit(TransactionStatus status) throws TechnicalException {
        getTransactionalView().commit(status);
    }

    /**
   * returns the current transactionnal view.
   * 
   * @return the current transactionnal view
   */
    public TransactionalView getTransactionalView() {
        TransactionalView res = transactionalViews.get();
        if (res == null) {
            res = new TransactionalView();
            transactionalViews.set(res);
        }
        return res;
    }

    /**
   * prepare the transaction defined by {@link TransactionStatus}. If this
   * method succeed, a {@link #commit(TransactionStatus)} must succeed or the
   * transactional integrity of the application won't be insured.
   * 
   * @param status
   *          the {@link TransactionStatus} of the transaction
   * @throws TechnicalException
   *           if an error occurred
   * @see com.genia.toolbox.basics.transaction.bean.EnlistableResource#prepare(org.springframework.transaction.TransactionStatus)
   */
    public void prepare(TransactionStatus status) throws TechnicalException {
        getTransactionalView().prepare(status);
    }

    /**
   * rollback the transaction defined by {@link TransactionStatus}.
   * 
   * @param status
   *          the {@link TransactionStatus} of the transaction
   * @throws TechnicalException
   *           if an error occurred
   * @see com.genia.toolbox.basics.transaction.bean.EnlistableResource#rollback(org.springframework.transaction.TransactionStatus)
   */
    public void rollback(TransactionStatus status) throws TechnicalException {
        getTransactionalView().rollback(status);
    }

    /**
   * start a new transaction for the current object.
   * 
   * @param definition
   *          {@link TransactionDefinition} instance (can be <code>null</code>
   *          for defaults), describing propagation behavior, isolation level,
   *          timeout etc
   * @param status
   *          the {@link TransactionStatus} of the transaction
   * @throws TechnicalException
   *           if an error occurred
   * @see com.genia.toolbox.basics.transaction.bean.EnlistableResource#startTransaction(org.springframework.transaction.TransactionDefinition,
   *      org.springframework.transaction.TransactionStatus)
   */
    public void startTransaction(TransactionDefinition definition, TransactionStatus status) throws TechnicalException {
        getTransactionalView().startTransaction(definition, status);
    }
}
