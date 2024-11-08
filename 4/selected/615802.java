package tyrex.tm.impl;

import java.io.PrintWriter;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.xa.Xid;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.XAException;
import tyrex.tm.TyrexTransactionManager;
import tyrex.tm.XAResourceCallback;
import tyrex.util.Messages;

/**
 * Implements a local transaction manager. The transaction manager
 * allows the application server to manage transactions on the local
 * thread through the {@link TransactionManager} interface.
 * <p>
 * Nested transactions are supported if the server configuration
 * indicates so, but all nested transactions appear as flat
 * transactions to the resources and are not registered with the
 * transaction server.
 *
 * @author <a href="arkin@intalio.com">Assaf Arkin</a>
 * @version $Revision: 1.19 $ $Date: 2001/10/05 22:15:34 $
 * @see Tyrex#recycleThread
 * @see TransactionDomain
 * @see TransactionImpl
 */
final class TransactionManagerImpl implements TransactionManager, Status, TyrexTransactionManager {

    /**
     * The transaction domain to which this manager belongs.
     */
    private TransactionDomainImpl _txDomain;

    TransactionManagerImpl(TransactionDomainImpl txDomain) {
        if (txDomain == null) throw new IllegalArgumentException("Argument 'txDomain' is null");
        _txDomain = txDomain;
    }

    public void begin() throws NotSupportedException, SystemException {
        Thread thread;
        ThreadContext context;
        XAResourceHolder[] resources;
        TransactionImpl tx;
        thread = Thread.currentThread();
        context = ThreadContext.getThreadContext(thread);
        tx = context._tx;
        if (tx != null && tx._status != STATUS_COMMITTED && tx._status != STATUS_ROLLEDBACK) {
            if (!_txDomain.getNestedTransactions()) throw new NotSupportedException(Messages.message("tyrex.tx.noNested")); else {
                tx = _txDomain.createTransaction(tx, 0);
                context._tx = tx;
                return;
            }
        } else tx = _txDomain.createTransaction(null, 0);
        if (_txDomain.enlistThread(tx, context, thread)) {
            resources = context.getXAResourceHolders();
            if (resources != null) enlistResources(tx, resources);
        }
    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        Thread thread;
        ThreadContext context;
        TransactionImpl tx;
        thread = Thread.currentThread();
        context = ThreadContext.getThreadContext(thread);
        tx = context._tx;
        if (tx == null) throw new IllegalStateException(Messages.message("tyrex.tx.inactive"));
        tx.commit();
        _txDomain.delistThread(context, thread);
    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        Thread thread;
        ThreadContext context;
        TransactionImpl tx;
        thread = Thread.currentThread();
        context = ThreadContext.getThreadContext(thread);
        tx = context._tx;
        if (tx == null) throw new IllegalStateException(Messages.message("tyrex.tx.inactive"));
        tx.rollback();
        _txDomain.delistThread(context, thread);
    }

    public int getStatus() {
        ThreadContext context;
        TransactionImpl tx;
        context = ThreadContext.getThreadContext();
        tx = context._tx;
        if (tx == null) return Status.STATUS_NO_TRANSACTION; else return tx._status;
    }

    public Transaction getTransaction() {
        return ThreadContext.getThreadContext()._tx;
    }

    public void resume(Transaction tx) throws InvalidTransactionException, IllegalStateException, SystemException {
        Thread thread;
        ThreadContext context;
        TransactionImpl txImpl;
        if (tx == null) throw new IllegalArgumentException("Argument tx is null");
        if (!(tx instanceof TransactionImpl)) throw new InvalidTransactionException(Messages.message("tyrex.tx.resumeForeign"));
        txImpl = (TransactionImpl) tx;
        thread = Thread.currentThread();
        context = ThreadContext.getThreadContext(thread);
        if (context._tx != null) throw new IllegalStateException(Messages.message("tyrex.tx.resumeOverload"));
        synchronized (tx) {
            if (txImpl.getTimedOut()) throw new InvalidTransactionException(Messages.message("tyrex.tx.timedOut"));
            if (txImpl._status != Status.STATUS_ACTIVE && txImpl._status != Status.STATUS_MARKED_ROLLBACK) throw new InvalidTransactionException(Messages.message("tyrex.tx.inactive"));
            try {
                ((TransactionImpl) txImpl.getTopLevel()).resumeAndEnlistResources(context.getXAResourceHolders());
            } catch (RollbackException except) {
            }
            _txDomain.enlistThread(txImpl, context, thread);
        }
    }

    public Transaction suspend() {
        Thread thread;
        ThreadContext context;
        TransactionImpl tx;
        thread = Thread.currentThread();
        context = ThreadContext.getThreadContext(thread);
        tx = context._tx;
        if (tx == null) return null;
        tx = (TransactionImpl) tx.getTopLevel();
        context._tx = null;
        synchronized (tx) {
            _txDomain.delistThread(context, thread);
            if (tx._status == STATUS_ACTIVE || tx._status == STATUS_MARKED_ROLLBACK) {
                try {
                    tx.suspendResources();
                } catch (SystemException except) {
                }
                return tx;
            } else return null;
        }
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        ThreadContext context;
        TransactionImpl tx;
        context = ThreadContext.getThreadContext();
        tx = context._tx;
        if (tx == null) throw new IllegalStateException(Messages.message("tyrex.tx.inactive"));
        tx.setRollbackOnly();
    }

    public void setTransactionTimeout(int seconds) {
        _txDomain.setTransactionTimeout(seconds);
    }

    public Transaction getTransaction(Xid xid) {
        return _txDomain.findTransaction(xid);
    }

    public Transaction getTransaction(String xid) {
        return _txDomain.findTransaction(xid);
    }

    public void dumpTransactionList(PrintWriter writer) {
        _txDomain.dumpTransactionList(writer);
    }

    public void dumpCurrentTransaction(PrintWriter writer) {
        TransactionImpl tx;
        if (writer == null) throw new IllegalArgumentException("Argument writer is null");
        tx = (TransactionImpl) getTransaction();
        if (tx == null) writer.println("No transaction associated with current thread"); else {
            writer.println("  Transaction " + tx._xid + " " + Util.getStatus(tx._status));
            writer.println("  Started " + Util.fromClock(tx._started) + " time-out " + Util.fromClock(tx._timeout));
        }
    }

    /**
     * Returns the transaction currently associated with the given
     * thread, or null if the thread is not associated with any
     * transaction. This method is equivalent to calling {@link
     * TransactionManager#getTransaction} from within the thread.
     *
     * @param thread The thread to lookup
     * @return The transaction currently associated with that thread
     */
    public Transaction getTransaction(Thread thread) {
        return ThreadContext.getThreadContext()._tx;
    }

    public void enlistResource(XAResource xaResource) throws SystemException {
        enlistResource(xaResource, null);
    }

    public void enlistResource(XAResource xaResource, XAResourceCallback callback) throws SystemException {
        ThreadContext context;
        TransactionImpl tx;
        if (xaResource == null) throw new IllegalArgumentException("Argument xaResource is null");
        context = ThreadContext.getThreadContext();
        context.add(xaResource, callback);
        tx = context._tx;
        if (tx != null) {
            try {
                ((TransactionImpl) tx.getTopLevel()).enlistResource(xaResource, callback);
            } catch (IllegalStateException except) {
            } catch (RollbackException except) {
            }
        }
    }

    public void delistResource(XAResource xaResource, int flag) {
        ThreadContext context;
        TransactionImpl tx;
        if (xaResource == null) throw new IllegalArgumentException("Argument xaResource is null");
        if (flag != XAResource.TMSUCCESS && flag != XAResource.TMFAIL) throw new IllegalArgumentException("Invalid value for flag");
        context = ThreadContext.getThreadContext();
        context.remove(xaResource);
        tx = context._tx;
        if (tx != null) {
            try {
                tx.getTopLevel().delistResource(xaResource, flag);
            } catch (SystemException except) {
            } catch (IllegalStateException except) {
            }
        }
    }

    /**
     * Called to resume the current transaction, but does not attempt to
     * associate the resources with this transaction. This method is used
     * during the synchronization.
     */
    protected void internalResume(TransactionImpl tx) throws IllegalStateException, SystemException {
        Thread thread;
        ThreadContext context;
        if (tx == null) throw new IllegalArgumentException("Argument tx is null");
        thread = Thread.currentThread();
        context = ThreadContext.getThreadContext(thread);
        if (context._tx != null) throw new IllegalStateException(Messages.message("tyrex.tx.resumeOverload"));
        synchronized (tx) {
            _txDomain.enlistThread(tx, context, thread);
        }
    }

    /**
     * Enlist the XA resources in the specified transaction. The
     * transaction is assumed to be a top-level transaction.
     *
     * @param tx The top-level transaction
     * @param xaResources The array of XA resources to be enlisted
     * in the transaction. Can be null.
     * @throws SystemException if there is a problem enlisting the 
     * resources.
     */
    private void enlistResources(TransactionImpl tx, XAResourceHolder[] xaResourceHolders) throws SystemException {
        if (xaResourceHolders != null) {
            try {
                for (int i = xaResourceHolders.length; i-- > 0; ) tx.enlistResource(xaResourceHolders[i]._xaResource, xaResourceHolders[i]._callback);
            } catch (Exception except) {
                try {
                    rollback();
                } catch (Exception except2) {
                }
                if (except instanceof SystemException) throw (SystemException) except; else throw new NestedSystemException(except);
            }
        }
    }
}
