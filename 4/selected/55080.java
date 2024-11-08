package com.knowgate.scheduler;

import java.util.Properties;
import com.knowgate.debug.DebugFile;

/**
 * WorkerThread Pool
 * @author Sergio Montoro Ten
 * @version 1.0
 */
public class WorkerThreadPool {

    private WorkerThread aThreads[];

    private long aStartTime[];

    private Properties oEnvProps;

    /**
   * <p>Create WorkerThreadPool</p>
   * thread Pool size is readed from maxschedulerthreads property of oEnvironmentProps,
   * the default value is 1.
   * Each thread is given the name WorkerThread_<i>n</i>
   * @param oAtomConsumer Atom Consumer Object to be used
   * @param oEnvironmentProps Environment Properties collection
   * (usually readed from hipergate.cnf)
   */
    public WorkerThreadPool(AtomConsumer oAtomConsumer, Properties oEnvironmentProps) {
        int nThreads = Integer.parseInt(oEnvironmentProps.getProperty("maxschedulerthreads", "1"));
        if (DebugFile.trace) DebugFile.writeln("maxschedulerthreads=" + String.valueOf(nThreads));
        oEnvProps = oEnvironmentProps;
        aThreads = new WorkerThread[nThreads];
        aStartTime = new long[nThreads];
        for (int t = 0; t < nThreads; t++) {
            if (DebugFile.trace) DebugFile.writeln("new WorkerThread(" + String.valueOf(t) + ")");
            aThreads[t] = new WorkerThread(this, oAtomConsumer);
            aThreads[t].setName("WorkerThread_" + String.valueOf(t));
        }
    }

    /**
   * Get Pool Size
   */
    public int size() {
        return aThreads.length;
    }

    /**
   * Get Environment properties collection from hipergate.cnf
   */
    public Properties getProperties() {
        return oEnvProps;
    }

    /**
   * Get Environment property
   * @return
   */
    public String getProperty(String sKey) {
        return oEnvProps.getProperty(sKey);
    }

    /**
   * Launch all WorkerThreads and start consuming atoms from queue.
   */
    public void launchAll() {
        for (int t = 0; t < aThreads.length; t++) {
            if (!aThreads[t].isAlive()) {
                aStartTime[t] = new java.util.Date().getTime();
                aThreads[t].start();
            }
        }
    }

    /**
   * Count of currently active WorkerThreads
   */
    public int livethreads() {
        int iLive = 0;
        for (int t = 0; t < aThreads.length; t++) {
            if (aThreads[t].isAlive()) {
                iLive++;
            }
        }
        return iLive;
    }

    public WorkerThread[] threads() {
        return aThreads;
    }

    /**
   * Register a thread callback object for each thread in this pool
   * @param oNewCallback WorkerThreadCallback subclass instance
   * @throws IllegalArgumentException If a callback with same name has oNewCallback was already registered
   */
    public void registerCallback(WorkerThreadCallback oNewCallback) throws IllegalArgumentException {
        final int iThreads = aThreads.length;
        for (int t = 0; t < iThreads; t++) aThreads[t].registerCallback(oNewCallback);
    }

    /**
    * Unregister a thread callback object for each thread in this pool
    * @param sCallbackName Name of callback to be unregistered
    */
    public void unregisterCallback(String sCallbackName) {
        final int iThreads = aThreads.length;
        for (int t = 0; t < iThreads; t++) aThreads[t].unregisterCallback(sCallbackName);
    }

    /**
    * <p>Halt all pooled threads commiting any pending operations before stoping</p>
    * If a thread is dead-locked by any reason halting it will not cause any effect.<br>
    * halt() method only sends a signals to the each WokerThread telling it that must
    * finish pending operations and stop.
    */
    public void haltAll() {
        final int iThreads = aThreads.length;
        for (int t = 0; t < iThreads; t++) aThreads[t].halt();
    }

    public void stopAll() {
        final int iThreads = aThreads.length;
        for (int t = 0; t < iThreads; t++) if (aThreads[t].isAlive()) aThreads[t].stop();
    }
}
