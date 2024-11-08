package org.iqual.chaplin.sync;

import org.iqual.util.LinkedIterator;
import org.iqual.chaplin.sync.TransactionIsReadOnlyException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author Zbynek Slajchrt
 * @since 19.12.2009 20:18:59
 */
public abstract class TransactionImpl implements Transaction {

    private static final Logger logger = Logger.getLogger(TransactionImpl.class.getName());

    private final Set<StmResource> readSet = Collections.synchronizedSet(new LinkedHashSet<StmResource>());

    private final Set<StmResource> writeSet = Collections.synchronizedSet(new LinkedHashSet<StmResource>());

    private final List<Runnable> actions = Collections.synchronizedList(new ArrayList<Runnable>());

    private final long readVersion;

    private final long spinBound;

    private final boolean readOnly;

    private interface ResourceAction<T> {

        void doAction(T element);
    }

    public abstract static class CumulativeException extends RuntimeException {

        private final List<Throwable> resourcesException = new ArrayList<Throwable>();

        public void addException(Throwable t) {
            resourcesException.add(t);
        }

        public List<Throwable> getExceptions() {
            return resourcesException;
        }
    }

    public TransactionImpl(long readVersion, long spinBound, boolean readOnly) {
        this.readVersion = readVersion;
        this.spinBound = spinBound;
        this.readOnly = readOnly;
    }

    public static class CommitFailedException extends CumulativeException {
    }

    public static class RollbackFailedException extends CumulativeException {
    }

    public long getSpinBound() {
        return spinBound;
    }

    public long getReadVersion() {
        return readVersion;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void registerRead(StmResource resource) {
        readSet.add(resource);
    }

    public void registerWrite(StmResource resource) {
        if (isReadOnly()) {
            throw new TransactionIsReadOnlyException();
        }
        writeSet.add(resource);
    }

    public void registerAction(Runnable action) {
        actions.add(action);
    }

    public void commit() {
        if (!isReadOnly()) {
            for (StmResource resource : writeSet) {
                resource.onLockingWriteSet(this);
            }
            final long newClock = incrementGlobalVersionClock();
            if (readVersion + 1 == newClock) {
            } else {
                for (StmResource resource : readSet) {
                    resource.onValidateReadSet(this);
                }
            }
            doActionEnsureAllInvoked(new ResourceAction<StmResource>() {

                public void doAction(StmResource resource) {
                    resource.onCommit(TransactionImpl.this, newClock);
                    resource.onReleaseLocks(TransactionImpl.this);
                }
            }, CommitFailedException.class, writeSet.iterator());
        }
        doActionEnsureAllInvoked(new ResourceAction<StmResource>() {

            public void doAction(StmResource resource) {
                resource.onFinished(TransactionImpl.this, false);
            }
        }, RollbackFailedException.class, new LinkedIterator<StmResource>(writeSet.iterator(), readSet.iterator()));
        executeActions();
    }

    public void rollback() {
        try {
            if (!readOnly) {
                doActionEnsureAllInvoked(new ResourceAction<StmResource>() {

                    public void doAction(StmResource resource) {
                        resource.onReleaseLocks(TransactionImpl.this);
                    }
                }, RollbackFailedException.class, writeSet.iterator());
            }
        } finally {
            doActionEnsureAllInvoked(new ResourceAction<StmResource>() {

                public void doAction(StmResource resource) {
                    resource.onFinished(TransactionImpl.this, true);
                }
            }, RollbackFailedException.class, new LinkedIterator<StmResource>(writeSet.iterator(), readSet.iterator()));
        }
    }

    protected abstract long incrementGlobalVersionClock();

    private <T> void doActionEnsureAllInvoked(ResourceAction<T> action, Class<? extends CumulativeException> exceptionClass, Iterator<T> iterator) {
        CumulativeException cumExc = null;
        while (iterator.hasNext()) {
            T elem = iterator.next();
            try {
                action.doAction(elem);
            } catch (Throwable t) {
                if (cumExc == null) {
                    try {
                        cumExc = exceptionClass.newInstance();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }
                cumExc.addException(t);
            }
        }
        if (cumExc != null) {
            throw cumExc;
        }
    }

    private void executeActions() {
        for (Runnable action : actions) {
            try {
                action.run();
            } catch (Throwable throwable) {
                logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            }
        }
    }
}
