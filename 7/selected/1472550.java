package com.ibm.realtime.flexotask.scheduling;

import com.ibm.realtime.flexotask.FlexotaskRunner;
import com.ibm.realtime.flexotask.util.ESystem;

/**
 * A convenient partial implementation of both FlexotaskRunner and FlexotaskSchedulerRunnable that assumes
 * either a single threaded scheduler or a scheduler with a distinguished "master" thread and some number
 * of slave threads.  This class becomes both the FlexotaskSchedulerRunnable of the master thread and the
 * FlexotaskRunner that the DelgatingFlexotaskRunner delegates to.  The master (or sole) thread is responsible
 * for managing the start / stop / shutdown logic.
 * <p>Any subclass must implement <b>runFlexotasks</b>.  This method is called repeatedly when the graph is running and not
 *   otherwise.  It should return with reasonable frequency to allow for a response to a stop or shutdown request.
 *   It should return iff it is safe to do so.  Returning <b>true</b> causes the graph to terminate immediately.
 *   For a single-threaded scheduler, implementing this method (only) is usually sufficient.
 * <p>If the scheduler is multithreaded, these additional tasks should be considered.
 * <ol><li>Override the propagateRunning(), propagateShutdown(), and/or propagateStopped() methods to propagate the running, shutdown, or
 *   stopped states to other threads.  The mechanism is up to the subclass.
 * <li>Override getThreadCoordinationLockCount to return the number of locks needed for interthread synchronization and management.
 *   <em>All synchronization must be done with locks obtained only in this way.  Synchronized methods except those of the locks themselves
 *   should not be used!</em>.  Override setSchedulerLocks in this class (and in other scheduler runnables) so that the actual assigned locks
 *   can be stored conveniently.  Do not use the lock in position 0 of the lock vector: it is reserved by parent class.
 * <li>When threads are cloned and stared in cloneAndStartThread(), All threads (master and slave) are started at once.
 *   The run() method in the slave thread should be overwritten to manipulate the behavior of the slave threads by the master.
 *   The protected variables <b>otherThreads</b> and <b>otherRunnables</b> give convenient access to the runnables of other threads.
 *   Bear in mind that stopping other threads should be done with interthread handshaking and not by using the deprecated
 *   <b>Thread.stop()</b> method (which, in addition to be deprecated, is irrevocable).
 * </ol>
 * <p>Designed to support multiple non-flexotask threads calling start(), stop(),
 *   and shutdown() while maintaining safety.  However, note that "safe" behavior can still
 *   be surprising (for example, one thread can do a shutdown() causing another thread to
 *   get an IllegalStateException in a "simultaneous" start()).
 */
public abstract class FlexotaskSingleThreadRunner extends FlexotaskSchedulerRunnable implements FlexotaskRunner {

    /** Value of threadState that indicates the Flexotask thread is starting */
    protected static final int THREAD_STARTING = 0;

    /** Value of threadState that indicates that the Flexotask thread is stopped, waiting for a start() */
    protected static final int THREAD_STOPPED = 1;

    /** Value of threadState that indicates that the Flexotask thread is running, doing real work. */
    protected static final int THREAD_RUNNING = 2;

    /** Value of threadState that indicates that the Flexotask thread has begun to shut down or has exited */
    protected static final int THREAD_SHUT_DOWN = 3;

    /** Debug flag */
    private static final boolean debug = false;

    /** Indicates that no request is pending */
    private static final int REQUEST_EMPTY = 0;

    /** Indicates that a thread start() has been requested */
    private static final int REQUEST_START = 1;

    /** Indicates that a thread stop() has been requested */
    private static final int REQUEST_STOP = 2;

    /** Indicates that a thread shutdown() has been requested */
    private static final int REQUEST_SHUTDOWN = 3;

    /** Other scheduler threads besides the one running this runnable */
    protected Thread[] otherThreads;

    /** The runnables for otherThreads (one for one) */
    protected FlexotaskSchedulerRunnable[] otherRunnables;

    /** Indicates the current state of the Flexotask thread */
    protected int threadState = THREAD_STARTING;

    /** An Object to use for synchronization */
    private Object lock;

    /** Indicates the current state of the non-flexotask thread making requests through this Runner */
    private int requestState = REQUEST_EMPTY;

    public void run() {
        synchronized (lock) {
            threadState = THREAD_STOPPED;
            lock.notifyAll();
        }
        print("Thread started");
        for (; ; ) {
            boolean freshRun = true;
            while (threadState == THREAD_RUNNING) {
                if (freshRun) {
                    print("Thread running exotasks");
                    propagateRunning();
                }
                try {
                    if (runFlexotasks(freshRun)) {
                        threadState = THREAD_SHUT_DOWN;
                        return;
                    }
                } catch (RuntimeException e) {
                    threadState = THREAD_SHUT_DOWN;
                    throw e;
                } catch (Error e) {
                    threadState = THREAD_SHUT_DOWN;
                    throw e;
                }
                freshRun = false;
                if (!processCommand(requestState)) {
                    propagateShutdown();
                    return;
                }
            }
            print("Thread stopped, waiting for command");
            propagateStopped();
            while (threadState == THREAD_STOPPED) {
                int state;
                synchronized (lock) {
                    while ((state = requestState) == REQUEST_EMPTY) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
                if (!processCommand(state)) {
                    propagateShutdown();
                    return;
                }
            }
        }
    }

    /**
   * Method to be implemented by each extender containing the actual
   *   logic to run exotasks.   Should run as long as it isn't safe to
   *   stop, and return whenever it is safe to stop.  Will be called again
   *   immediately if there is no request to stop
   * @param freshRun true on the first call and the first call after every
   *   period of being stopped.
   * @return true if the graph should terminate, false if more executions are possible
   */
    public abstract boolean runFlexotasks(boolean freshRun);

    public void setSchedulerLocks(Object[] locks) {
        lock = locks[0];
    }

    public void shutdown() {
        synchronized (lock) {
            waitForOwnership();
            requestState = REQUEST_SHUTDOWN;
            lock.notifyAll();
            waitFor(THREAD_SHUT_DOWN);
            requestState = REQUEST_EMPTY;
            lock.notifyAll();
        }
    }

    public void start() {
        synchronized (lock) {
            waitForOwnership();
            if (threadState == THREAD_SHUT_DOWN) {
                throw new IllegalStateException("Trying to start a dead FlexotaskGraph");
            }
            requestState = REQUEST_START;
            lock.notifyAll();
            waitFor(THREAD_RUNNING);
            requestState = REQUEST_EMPTY;
            lock.notifyAll();
        }
    }

    public void stop() {
        synchronized (lock) {
            waitForOwnership();
            if (threadState == THREAD_SHUT_DOWN) {
                return;
            }
            requestState = REQUEST_STOP;
            lock.notifyAll();
            waitFor(THREAD_STOPPED);
            requestState = REQUEST_EMPTY;
            lock.notifyAll();
        }
    }

    /**
   * Subclasses override this to express how many coordination locks they need.  The default is 0.
   * @return the number of coordination locks required
   */
    protected int getThreadCoordinationLockCount() {
        return 0;
    }

    /**
   * Subclasses override if they need to propagate the running state to other threads.  The mechanism is up to the subclass
   */
    protected void propagateRunning() {
    }

    /**
   * Subclasses override if they need to propagate the shutdown state to other threads.  The mechanism is up to the subclass
   */
    protected void propagateShutdown() {
    }

    /**
   * Subclasses override if they need to propagate the stopped state to other threads.  The mechanism is up to the subclass
   */
    protected void propagateStopped() {
    }

    /**
   * Subroutine to print debugging messages iff requested.  Note: when this is called from
   *   the exotask scheduling thread, the message must be a simple string literal to avoid
   *   illegal allocations.
   * @param msg the message to print
   */
    private void print(String msg) {
        if (debug) {
            ESystem.err.println(msg);
        }
    }

    /**
   * Subroutine of run: looks for a command and processes one if present
   * @return true if the run method should keep running
   */
    private boolean processCommand(int command) {
        if (command == REQUEST_EMPTY) {
            return true;
        }
        synchronized (lock) {
            switch(command) {
                case REQUEST_START:
                    threadState = THREAD_RUNNING;
                    lock.notifyAll();
                    print("Thread processed start command");
                    break;
                case REQUEST_STOP:
                    threadState = THREAD_STOPPED;
                    lock.notifyAll();
                    print("Thread processed stop command");
                    break;
                default:
                    threadState = THREAD_SHUT_DOWN;
                    lock.notifyAll();
                    print("Thread processed shutdown command");
                    break;
            }
            while (requestState == command) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                }
            }
            print("Thread got ack");
        }
        return threadState != THREAD_SHUT_DOWN;
    }

    /**
   * Subroutine to wait for the exotask thread to enter a particular state
   * @param state the desired state
   */
    private void waitFor(int state) {
        while (threadState != state) {
            print("Requestor waiting for thread state " + state + ", current state is " + threadState);
            try {
                lock.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    /**
   * Subroutine to cause the requesting thread to wait until it is the only requesting thread and
   * the exotask thread has at least reached its initial dispatch point.
   * Always called from a synchronized method.
   */
    private void waitForOwnership() {
        while (threadState == THREAD_STARTING) {
            print("Requestor waiting for thread to start");
            try {
                lock.wait();
            } catch (InterruptedException e) {
            }
        }
        while (requestState != REQUEST_EMPTY) {
            print("Requestor waiting for other requests to finish");
            try {
                lock.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    /**
   * Convenient version of cloneAndStartThread when there are no slave threads
   * @param toClone the FlexotaskSingleThreadRunner (extended) that is to be cloned and started
   * @param threadFactory the FlexotaskThreadFactory for the current FlexotaskGraph
   * @return the FlexotaskRunner object that the scheduler should return to the validator
   */
    public static FlexotaskRunner cloneAndStartThread(FlexotaskSingleThreadRunner toClone, FlexotaskThreadFactory threadFactory) {
        return cloneAndStartThread(toClone, threadFactory, null);
    }

    /**
   * Method for use by schedulers to correctly clone an object whose class extends this
   *   one onto the scheduler heap, also cloning any other FlexotaskSchedulerRunnables for slave
   *   threads.  It creates all necessary threads and starts the thread associated with this
   *   runnable ("the master thread").  The slave threads are not started automatically; it is up to the
   *   master thread to manage them.
   * @param toClone the FlexotaskSingleThreadRunner (extended) that is to be cloned and started
   * @param threadFactory the FlexotaskThreadFactory for the current FlexotaskGraph
   * @param otherRunnables the FlexotaskSchedulerRunnables for the other threads to be created (if any); null or the empty array if none
   * @return the FlexotaskRunner object that the scheduler should return to the validator
   */
    public static FlexotaskRunner cloneAndStartThread(FlexotaskSingleThreadRunner toClone, FlexotaskThreadFactory threadFactory, FlexotaskSchedulerRunnable[] otherRunnables) {
        int nRunnables = otherRunnables == null ? 1 : otherRunnables.length + 1;
        FlexotaskSchedulerRunnable[] runnables = new FlexotaskSchedulerRunnable[nRunnables];
        runnables[0] = toClone;
        if (nRunnables > 1) {
            System.arraycopy(otherRunnables, 0, runnables, 1, nRunnables - 1);
        }
        if (nRunnables > 1) {
            toClone.otherRunnables = new FlexotaskSchedulerRunnable[nRunnables - 1];
            for (int i = 0; i < nRunnables - 1; i++) {
                toClone.otherRunnables[i] = runnables[i + 1];
            }
            toClone.otherThreads = new Thread[nRunnables - 1];
        }
        Object[] theLocks = new Object[toClone.getThreadCoordinationLockCount() + 1];
        for (int i = 0; i < theLocks.length; i++) {
            theLocks[i] = new Object();
        }
        Thread[] allThreads = threadFactory.createThreads(runnables, theLocks);
        FlexotaskSingleThreadRunner clonedMasterRunnable = (FlexotaskSingleThreadRunner) runnables[0];
        if (nRunnables > 1) {
            for (int i = 0; i < nRunnables - 1; i++) {
                clonedMasterRunnable.otherThreads[i] = allThreads[i + 1];
            }
        }
        for (int i = 0; i < nRunnables; i++) {
            allThreads[i].start();
        }
        return new DelegatingFlexotaskRunner(clonedMasterRunnable);
    }
}
