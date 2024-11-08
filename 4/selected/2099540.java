package com.sleepycat.je.dbi;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.je.EnvironmentLockedException;
import com.sleepycat.je.EnvironmentMutableConfig;
import com.sleepycat.je.EnvironmentNotFoundException;
import com.sleepycat.je.latch.LatchSupport;

/**
 * Singleton collection of environments.  Responsible for environment open and
 * close, supporting this from multiple threads by synchronizing on the pool.
 *
 * To avoid multiple environment openings from being blocked by recovery
 * getEnvironment() the EnvironmentImpl constructor is broken into two parts,
 * with the second part doing the recovery.
 *
 * When synchronizing on two or more of the following objects the
 * synchronization order must be as follows.  Synchronization is not performed
 * in constructors, of course, because no other thread can access the object.
 *
 * Synchronization order:  Environment, DbEnvPool, EnvironmentImpl, Evictor
 *
 * Environment ctor                                 NOT synchronized
 *   calls DbEnvPool.getEnvironment                 NOT synchronized
 *     synchronized (DbEnvPool)                     synchronized
 *     creates new EnvironmentImpl                  NOT synchronized
 *       add environment to envs
 *     end synchronized (DbEnvPool)
 *     calls EnvironmentImpl.finishInit()           synchronized
 *       calls RecoveryManager.recover,buildTree    NOT synchronized
 *         calls Evictor.addEnvironment             synchronized
 *
 * EnvironmentImpl.reinit                           NOT synchronized
 *   calls DbEnvPool.reinitEnvironment              synchronized
 *     calls EnvironmentImpl.doReinit               synchronized
 *       calls RecoveryManager.recover,buildTree    NOT synchronized
 *         calls Evictor.addEnvironment             synchronized
 *
 * Environment.close                                synchronized
 *   calls EnvironmentImpl.close                    NOT synchronized
 *     calls DbEnvPool.closeEnvironment             synchronized
 *       calls EnvironmentImpl.doClose              synchronized
 *         calls Evictor.removeEnvironment          synchronized
 *
 * Environment.setMutableConfig                     synchronized
 *   calls EnvironmentImpl.setMutableConfig         NOT synchronized
 *     calls DbEnvPool.setMutableConfig             synchronized
 *       calls EnvironmentImpl.doSetMutableConfig   synchronized
 */
public class DbEnvPool {

    private static DbEnvPool pool = new DbEnvPool();

    private final Map<String, EnvironmentImpl> envs;

    private final Set<EnvironmentImpl> sharedCacheEnvs;

    /**
     * Enforce singleton behavior.
     */
    private DbEnvPool() {
        envs = new HashMap<String, EnvironmentImpl>();
        sharedCacheEnvs = new HashSet<EnvironmentImpl>();
    }

    /**
     * Access the singleton instance.
     */
    public static DbEnvPool getInstance() {
        return pool;
    }

    public synchronized int getNSharedCacheEnvironments() {
        return sharedCacheEnvs.size();
    }

    private EnvironmentImpl getAnySharedCacheEnv() {
        Iterator<EnvironmentImpl> iter = sharedCacheEnvs.iterator();
        return iter.hasNext() ? iter.next() : null;
    }

    /**
     * Find a single environment, used by Environment handles and by command
     * line utilities.
     */
    public EnvironmentImpl getEnvironment(File envHome, EnvironmentConfig config, boolean checkImmutableParams, boolean openIfNeeded, RepConfigProxy repConfigProxy) throws EnvironmentNotFoundException, EnvironmentLockedException {
        String environmentKey = null;
        EnvironmentImpl envImpl = null;
        synchronized (this) {
            environmentKey = getEnvironmentMapKey(envHome);
            envImpl = envs.get(environmentKey);
            if (envImpl != null) {
                if (envImpl.isReplicated() && (repConfigProxy == null) && !config.getReadOnly()) {
                    throw new UnsupportedOperationException("This environment was previously opened for " + "replication. It cannot be re-opened in read/write " + "mode for standalone operation.");
                }
                envImpl.checkIfInvalid();
                if (checkImmutableParams) {
                    envImpl.checkImmutablePropsForEquality(DbInternal.getProps(config));
                }
                envImpl.incReferenceCount();
            } else {
                if (openIfNeeded) {
                    EnvironmentImpl sharedCacheEnv = config.getSharedCache() ? getAnySharedCacheEnv() : null;
                    envImpl = (repConfigProxy == null) ? new EnvironmentImpl(envHome, config, sharedCacheEnv) : loadRepImpl(envHome, config, sharedCacheEnv, repConfigProxy);
                    assert config.getSharedCache() == envImpl.getSharedCache();
                    envImpl.incReferenceCount();
                    envs.put(environmentKey, envImpl);
                }
            }
        }
        if (envImpl != null) {
            boolean success = false;
            try {
                envImpl.finishInit(config);
                synchronized (this) {
                    addToSharedCacheEnvs(envImpl);
                }
                success = true;
            } finally {
                if (!success) {
                    envs.remove(environmentKey);
                }
            }
        }
        return envImpl;
    }

    /**
     * Use reflection to create a RepImpl, to avoid introducing HA compilation
     * dependencies to non-replication code.
     */
    private EnvironmentImpl loadRepImpl(File envHome, EnvironmentConfig config, EnvironmentImpl sharedCacheEnv, RepConfigProxy repConfigProxy) throws DatabaseException {
        final String repClassName = "com.sleepycat.je.rep.impl.RepImpl";
        final String envImplName = "com.sleepycat.je.dbi.EnvironmentImpl";
        final String repProxy = "com.sleepycat.je.dbi.RepConfigProxy";
        try {
            final Class<?> repClass = Class.forName(repClassName);
            return (EnvironmentImpl) repClass.getConstructor(envHome.getClass(), config.getClass(), Class.forName(envImplName), Class.forName(repProxy)).newInstance(envHome, config, sharedCacheEnv, repConfigProxy);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw EnvironmentFailureException.unexpectedException(e);
        } catch (Exception e) {
            throw EnvironmentFailureException.unexpectedException(e);
        }
    }

    private void addToSharedCacheEnvs(EnvironmentImpl envImpl) throws DatabaseException {
        if (envImpl.getSharedCache() && !sharedCacheEnvs.contains(envImpl)) {
            sharedCacheEnvs.add(envImpl);
            assert envImpl.getEvictor().checkEnv(envImpl);
            resetSharedCache(-1, envImpl);
        }
    }

    /**
     * Called by EnvironmentImpl.setMutableConfig to perform the
     * setMutableConfig operation while synchronized on the DbEnvPool.
     *
     * In theory we shouldn't need to synchronize here when
     * envImpl.getSharedCache() is false; however, we synchronize
     * unconditionally to standardize the synchronization order and avoid
     * accidental deadlocks.
     */
    synchronized void setMutableConfig(EnvironmentImpl envImpl, EnvironmentMutableConfig mutableConfig) throws DatabaseException {
        envImpl.doSetMutableConfig(mutableConfig);
        if (envImpl.getSharedCache()) {
            resetSharedCache(envImpl.getMemoryBudget().getMaxMemory(), envImpl);
        }
    }

    /**
     * Called by EnvironmentImpl.close to perform the close operation while
     * synchronized on the DbEnvPool.
     */
    synchronized void closeEnvironment(EnvironmentImpl envImpl, boolean doCheckpoint, boolean doCheckLeaks) {
        if (envImpl.decReferenceCount()) {
            try {
                envImpl.doClose(doCheckpoint, doCheckLeaks);
            } finally {
                removeEnvironment(envImpl);
            }
        }
    }

    /**
     * Called by EnvironmentImpl.closeAfterInvalid to perform the close
     * operation while synchronized on the DbEnvPool.
     */
    synchronized void closeEnvironmentAfterInvalid(EnvironmentImpl envImpl) throws DatabaseException {
        try {
            envImpl.doCloseAfterInvalid();
        } finally {
            removeEnvironment(envImpl);
        }
    }

    /**
     * Removes an EnvironmentImpl from the pool after it has been closed.  This
     * method is called while synchronized.  Note that the environment was
     * removed from the SharedEvictor by EnvironmentImpl.shutdownEvictor.
     */
    private void removeEnvironment(EnvironmentImpl envImpl) throws DatabaseException {
        String environmentKey = getEnvironmentMapKey(envImpl.getEnvironmentHome());
        boolean found = envs.remove(environmentKey) != null;
        if (sharedCacheEnvs.remove(envImpl)) {
            assert found && envImpl.getSharedCache();
            assert !envImpl.getEvictor().checkEnv(envImpl);
            if (sharedCacheEnvs.isEmpty()) {
                envImpl.getEvictor().shutdown();
            } else {
                envImpl.getMemoryBudget().subtractCacheUsage();
                resetSharedCache(-1, null);
            }
        } else {
            assert !found || !envImpl.getSharedCache();
        }
        if (envs.isEmpty()) {
            LatchSupport.clearNotes();
        }
    }

    /**
     * For unit testing only.
     */
    public synchronized void clear() {
        envs.clear();
    }

    public synchronized Collection<EnvironmentImpl> getEnvImpls() {
        return envs.values();
    }

    String getEnvironmentMapKey(File file) throws DatabaseException {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw EnvironmentFailureException.unexpectedException(e);
        }
    }

    /**
     * Resets the memory budget for all environments with a shared cache.
     *
     * @param newMaxMemory is the new total cache budget or is less than 0 if
     * the total should remain unchanged.  A total greater than zero is given
     * when it has changed via setMutableConfig.
     *
     * @param skipEnv is an environment that should not be reset, or null.
     * Non-null is passed when an environment has already been reset because
     * it was just created or the target of setMutableConfig.
     */
    private void resetSharedCache(long newMaxMemory, EnvironmentImpl skipEnv) throws DatabaseException {
        for (EnvironmentImpl envImpl : sharedCacheEnvs) {
            if (envImpl != skipEnv) {
                envImpl.getMemoryBudget().reset(newMaxMemory, false, envImpl.getConfigManager());
            }
        }
    }
}
