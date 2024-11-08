package org.openide.util;

import org.openide.ErrorManager;
import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** Read-many/write-one lock.
* Allows control over resources that
* can be read by several readers at once but only written by one writer.
* <P>
* It is guaranteed that if you are a writer you can also enter the
* mutex as a reader. Conversely, if you are the <em>only</em> reader you
* can enter the mutex as a writer, but you'll be warned because it is very
* deadlock prone (two readers trying to get write access concurently).
* <P>
* If the mutex is used only by one thread, the thread can repeatedly
* enter it as a writer or reader. So one thread can never deadlock itself,
* whichever order operations are performed in.
* <P>
* There is no strategy to prevent starvation.
* Even if there is a writer waiting to enter, another reader might enter
* the section instead.
* <P>
* Examples of use:
*
* <p><code><PRE>
* Mutex m = new Mutex ();
*
* // Grant write access, compute an integer and return it:
* return (Integer)m.writeAccess (new Mutex.Action () {
*   public Object run () {
*     return new Integer (1);
*   }
* });
*
* // Obtain read access, do some computation, possibly throw an IOException:
* try {
*   m.readAccess (new Mutex.ExceptionAction () {
*     public Object run () throws IOException {
*       if (...) throw new IOException ();
*
*       return null;
*     }
*   });
* } catch (MutexException ex) {
*   throw (IOException)ex.getException ();
* }
*
* // check whether you are already in read access
* if (m.isReadAccess ()) {
*   // do your work
* }
* </PRE></code>
*
* @author Ales Novak
*/
public final class Mutex extends Object {

    /** Mutex that allows code to be synchronized with the AWT event dispatch thread.
     * <P>
     * When the Mutex methods are invoked on this mutex, the methods' semantics 
     * change as follows:
     * <UL>
     * <LI>The {@link #isReadAccess} and {@link #isWriteAccess} methods
     *  return <code>true</code> if the current thread is the event dispatch thread
     *  and false otherwise.
     * <LI>The {@link #postReadRequest} and {@link #postWriteRequest} methods
     *  asynchronously execute the {@link java.lang.Runnable} passed in their 
     *  <code>run</code> parameter on the event dispatch thead.
     * <LI>The {@link #readAccess(java.lang.Runnable)} and 
     *  {@link #writeAccess(java.lang.Runnable)} methods asynchronously execute the 
     *  {@link java.lang.Runnable} passed in their <code>run</code> parameter 
     *  on the event dispatch thread, unless the current thread is 
     *  the event dispatch thread, in which case 
     *  <code>run.run()</code> is immediately executed.
     * <LI>The {@link #readAccess(Mutex.Action)},
     *  {@link #readAccess(Mutex.ExceptionAction action)},
     *  {@link #writeAccess(Mutex.Action action)} and
     *  {@link #writeAccess(Mutex.ExceptionAction action)} 
     *  methods synchronously execute the {@link Mutex.ExceptionAction}
     *  passed in their <code>action</code> parameter on the event dispatch thread,
     *  unless the current thread is the event dispatch thread, in which case
     *  <code>action.run()</code> is immediately executed.
     * </UL>
     */
    public static final Mutex EVENT = new Mutex();

    /** this is used from tests to prevent upgrade from readAccess to writeAccess
     * by strictly throwing exception. Otherwise we just notify that using ErrorManager.
     */
    static boolean beStrict;

    /** Lock free */
    private static final int NONE = 0x0;

    /** Enqueue all requests */
    private static final int CHAIN = 0x1;

    /** eXclusive */
    private static final int X = 0x2;

    /** Shared */
    private static final int S = 0x3;

    /** number of modes */
    private static final int MODE_COUNT = 0x4;

    private static final boolean[][] cmatrix = { null, null, { true, false, false, false }, { true, false, false, true } };

    /** granted mode */
    private int grantedMode = NONE;

    /** protects internal data structures */
    private Object LOCK;

    /** threads that - owns or waits for this mutex */
    private Map registeredThreads;

    private int readersNo = 0;

    /** a queue of waiting threads for this mutex */
    private List waiters;

    /** Enhanced constructor that permits specifying an object to use as a lock.
    * The lock is used on entry and exit to {@link #readAccess} and during the
    * whole execution of {@link #writeAccess}. The ability to specify locks
    * allows several <code>Mutex</code>es to synchronize on one object or to synchronize
    * a mutex with another critical section.
    *
    * @param lock lock to use
    */
    public Mutex(Object lock) {
        init(lock);
    }

    /** Default constructor.
    */
    public Mutex() {
        init(new InternalLock());
    }

    /** @param privileged can enter privileged states of this Mutex
     * This helps avoid creating of custom Runnables.
     */
    public Mutex(Privileged privileged) {
        if (privileged == null) {
            throw new IllegalArgumentException("privileged == null");
        } else {
            init(new InternalLock());
            privileged.setParent(this);
        }
    }

    /** Decides whether two locks are compatible.?
     * @param granted?
     * @param requested?
     * @return <tt>true</tt> iff they are compatible?
     */
    private static boolean compatibleLocks(int granted, int requested) {
        return cmatrix[requested][granted];
    }

    /** Initiates this Mutex */
    private void init(Object lock) {
        this.LOCK = lock;
        this.registeredThreads = new HashMap(7);
        this.waiters = new LinkedList();
    }

    /** Run an action only with read access.
    * See class description re. entering for write access within the dynamic scope.
    * @param action the action to perform
    * @return the object returned from {@link Mutex.Action#run}
    */
    public Object readAccess(Action action) {
        if (this == EVENT) {
            try {
                return doEventAccess(action);
            } catch (MutexException e) {
                InternalError err = new InternalError("Exception from non-Exception Action");
                ErrorManager.getDefault().annotate(err, e.getException());
                throw err;
            }
        }
        Thread t = Thread.currentThread();
        readEnter(t);
        try {
            return action.run();
        } finally {
            leave(t);
        }
    }

    /** Run an action with read access and possibly throw a checked exception.
    * The exception if thrown is then encapsulated
    * in a <code>MutexException</code> and thrown from this method. One is encouraged
    * to catch <code>MutexException</code>, obtain the inner exception, and rethrow it.
    * Here is an example:
    * <p><code><PRE>
    * try {
    *   mutex.readAccess (new ExceptionAction () {
    *     public void run () throws IOException {
    *       throw new IOException ();
    *     }
    *   });
    *  } catch (MutexException ex) {
    *    throw (IOException) ex.getException ();
    *  }
    * </PRE></code>
    * Note that <em>runtime exceptions</em> are always passed through, and neither
    * require this invocation style, nor are encapsulated.
    * @param action the action to execute
    * @return the object returned from {@link Mutex.ExceptionAction#run}
    * @exception MutexException encapsulates a user exception
    * @exception RuntimeException if any runtime exception is thrown from the run method
    * @see #readAccess(Mutex.Action)
    */
    public Object readAccess(ExceptionAction action) throws MutexException {
        if (this == EVENT) {
            return doEventAccess(action);
        }
        Thread t = Thread.currentThread();
        readEnter(t);
        try {
            return action.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new MutexException(e);
        } finally {
            leave(t);
        }
    }

    /** Run an action with read access, returning no result.
    * It may be run asynchronously.
    *
    * @param action the action to perform
    * @see #readAccess(Mutex.Action)
    */
    public void readAccess(final Runnable action) {
        if (this == EVENT) {
            doEvent(action);
            return;
        }
        Thread t = Thread.currentThread();
        readEnter(t);
        try {
            action.run();
        } finally {
            leave(t);
        }
    }

    /** Run an action with write access.
    * The same thread may meanwhile reenter the mutex; see the class description for details.
    *
    * @param action the action to perform
    * @return the result of {@link Mutex.Action#run}
    */
    public Object writeAccess(Action action) {
        if (this == EVENT) {
            try {
                return doEventAccess(action);
            } catch (MutexException e) {
                InternalError err = new InternalError("Exception from non-Exception Action");
                ErrorManager.getDefault().annotate(err, e.getException());
                throw err;
            }
        }
        Thread t = Thread.currentThread();
        writeEnter(t);
        try {
            return action.run();
        } finally {
            leave(t);
        }
    }

    /** Run an action with write access and possibly throw an exception.
    * Here is an example:
    * <p><code><PRE>
    * try {
    *   mutex.writeAccess (new ExceptionAction () {
    *     public void run () throws IOException {
    *       throw new IOException ();
    *     }
    *   });
    *  } catch (MutexException ex) {
    *    throw (IOException) ex.getException ();
    *  }
    * </PRE></code>
    *
    * @param action the action to execute
    * @return the result of {@link Mutex.ExceptionAction#run}
    * @exception MutexException an encapsulated checked exception, if any
    * @exception RuntimeException if a runtime exception is thrown in the action
    * @see #writeAccess(Mutex.Action)
    * @see #readAccess(Mutex.ExceptionAction)
    */
    public Object writeAccess(ExceptionAction action) throws MutexException {
        if (this == EVENT) {
            return doEventAccess(action);
        }
        Thread t = Thread.currentThread();
        writeEnter(t);
        try {
            return action.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new MutexException(e);
        } finally {
            leave(t);
        }
    }

    /** Run an action with write access and return no result.
    * It may be run asynchronously.
    *
    * @param action the action to perform
    * @see #writeAccess(Mutex.Action)
    * @see #readAccess(Runnable)
    */
    public void writeAccess(final Runnable action) {
        if (this == EVENT) {
            doEvent(action);
            return;
        }
        Thread t = Thread.currentThread();
        writeEnter(t);
        try {
            action.run();
        } finally {
            leave(t);
        }
    }

    /** Tests whether this thread has already entered the mutex in read access.
     * If it returns true, calling <code>readAccess</code>
     * will be executed immediatelly
     * without any blocking.
     * Calling <code>postWriteAccess</code> will delay the execution
     * of its <code>Runnable</code> until a readAccess section is over
     * and calling <code>writeAccess</code> is strongly prohibited and will
     * result in a warning as a deadlock prone behaviour.
     * <p><strong>Warning:</strong> since a thread with write access automatically
     * has effective read access as well (whether or not explicitly requested), if
     * you want to check whether a thread can read some data, you should check for
     * either kind of access, e.g.:
     * <pre>assert myMutex.isReadAccess() || myMutex.isWriteAccess();</pre>
     *
     * @return true if the thread is in read access section
     * @since 4.48
     */
    public boolean isReadAccess() {
        if (this == EVENT) {
            return javax.swing.SwingUtilities.isEventDispatchThread();
        }
        Thread t = Thread.currentThread();
        ThreadInfo info;
        synchronized (LOCK) {
            info = getThreadInfo(t);
            if (info != null) {
                if (info.counts[S] > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Tests whether this thread has already entered the mutex in write access.
     * If it returns true, calling <code>writeAccess</code> will be executed
     * immediatelly without any other blocking. <code>postReadAccess</code>
     * will be delayed until a write access runnable is over.
     *
     * @return true if the thread is in write access section
     * @since 4.48
     */
    public boolean isWriteAccess() {
        if (this == EVENT) {
            return javax.swing.SwingUtilities.isEventDispatchThread();
        }
        Thread t = Thread.currentThread();
        ThreadInfo info;
        synchronized (LOCK) {
            info = getThreadInfo(t);
            if (info != null) {
                if (info.counts[X] > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Posts a read request. This request runs immediately iff
     * this Mutex is in the shared mode or this Mutex is not contended
     * at all.
     *
     * This request is delayed if this Mutex is in the exclusive
     * mode and is held by this thread, until the exclusive is left.
     *
     * Finally, this request blocks, if this Mutex is in the exclusive
     * mode and is held by another thread.
     *
     * <p><strong>Warning:</strong> this method blocks.</p>
     *
     * @param run runnable to run
     */
    public void postReadRequest(final Runnable run) {
        postRequest(S, run);
    }

    /** Posts a write request. This request runs immediately iff
     * this Mutex is in the "pure" exclusive mode, i.e. this Mutex
     * is not reentered in shared mode after the exclusive mode
     * was acquired. Otherwise it is delayed until all read requests
     * are executed.
     *
     * This request runs immediately if this Mutex is not contended at all.
     *
     * This request blocks if this Mutex is in the shared mode.
     *
     * <p><strong>Warning:</strong> this method blocks.</p>
     * @param run runnable to run
     */
    public void postWriteRequest(Runnable run) {
        postRequest(X, run);
    }

    /** toString */
    public String toString() {
        if (this == EVENT) {
            return "Mutex.EVENT";
        }
        String newline = System.getProperty("line.separator");
        StringBuffer sbuff = new StringBuffer(512);
        synchronized (LOCK) {
            sbuff.append("threads: ").append(registeredThreads).append(newline);
            sbuff.append("readersNo: ").append(readersNo).append(newline);
            sbuff.append("waiters: ").append(waiters).append(newline);
            sbuff.append("grantedMode: ").append(grantedMode).append(newline);
        }
        return sbuff.toString();
    }

    /** enters this mutex for writing */
    private void writeEnter(Thread t) {
        enter(X, t, true);
    }

    /** enters this mutex for reading */
    private void readEnter(Thread t) {
        enter(S, t, true);
    }

    /** enters this mutex with given mode
    * @param requested one of S, X
    * @param t
    */
    private boolean enter(int requested, Thread t, boolean block) {
        QueueCell cell = null;
        int loopc = 0;
        for (; ; ) {
            loopc++;
            synchronized (LOCK) {
                ThreadInfo info = getThreadInfo(t);
                if (info != null) {
                    if (grantedMode == NONE) {
                        throw new IllegalStateException();
                    }
                    if (((info.mode == S) && (grantedMode == X)) || ((info.mode == X) && (grantedMode == S))) {
                        throw new IllegalStateException();
                    }
                    if ((info.mode == X) || (info.mode == requested)) {
                        if (info.forced) {
                            info.forced = false;
                        } else {
                            if ((requested == X) && (info.counts[S] > 0)) {
                                IllegalStateException e = new IllegalStateException("WARNING: Going from readAccess to writeAccess, see #10778: http://www.netbeans.org/issues/show_bug.cgi?id=10778 ");
                                if (beStrict) {
                                    throw e;
                                }
                                ErrorManager.getDefault().notify(e);
                            }
                            info.counts[requested]++;
                            if ((requested == S) && (info.counts[requested] == 1)) {
                                readersNo++;
                            }
                        }
                        return true;
                    } else if (canUpgrade(info.mode, requested)) {
                        IllegalStateException e = new IllegalStateException("WARNING: Going from readAccess to writeAccess, see #10778: http://www.netbeans.org/issues/show_bug.cgi?id=10778 ");
                        if (beStrict) {
                            throw e;
                        }
                        ErrorManager.getDefault().notify(e);
                        info.mode = X;
                        info.counts[requested]++;
                        info.rsnapshot = info.counts[S];
                        if (grantedMode == S) {
                            grantedMode = X;
                        } else if (grantedMode == X) {
                            throw new IllegalStateException();
                        }
                        return true;
                    } else {
                        IllegalStateException e = new IllegalStateException("WARNING: Going from readAccess to writeAccess through queue, see #10778: http://www.netbeans.org/issues/show_bug.cgi?id=10778 ");
                        if (beStrict) {
                            throw e;
                        }
                        ErrorManager.getDefault().notify(e);
                    }
                } else {
                    if (isCompatible(requested)) {
                        grantedMode = requested;
                        registeredThreads.put(t, info = new ThreadInfo(t, requested));
                        if (requested == S) {
                            readersNo++;
                        }
                        return true;
                    }
                }
                if (!block) {
                    return false;
                }
                grantedMode = CHAIN;
                cell = chain(requested, t, 0);
            }
            cell.sleep();
        }
    }

    /** privilegedEnter serves for processing posted requests */
    private boolean reenter(Thread t, int mode) {
        if (mode == S) {
            if ((grantedMode != NONE) && (grantedMode != S)) {
                throw new IllegalStateException(this.toString());
            }
            enter(mode, t, true);
            return false;
        }
        ThreadInfo tinfo = getThreadInfo(t);
        boolean chainFromLeaveX = ((grantedMode == CHAIN) && (tinfo != null) && (tinfo.counts[X] > 0));
        if ((grantedMode == X) || (grantedMode == NONE) || chainFromLeaveX) {
            enter(mode, t, true);
            return false;
        } else {
            if (readersNo == 0) {
                throw new IllegalStateException(this.toString());
            }
            ThreadInfo info = new ThreadInfo(t, mode);
            registeredThreads.put(t, info);
            readersNo += 2;
            grantedMode = CHAIN;
            return true;
        }
    }

    /** @param t holds S (one entry) and wants X, grantedMode != NONE && grantedMode != X */
    private void privilegedEnter(Thread t, int mode) {
        boolean decrease = true;
        ThreadInfo info;
        synchronized (LOCK) {
            info = getThreadInfo(t);
        }
        for (; ; ) {
            QueueCell cell;
            synchronized (LOCK) {
                if (decrease) {
                    decrease = false;
                    readersNo -= 2;
                }
                grantedMode = CHAIN;
                cell = chain(mode, t, Integer.MAX_VALUE);
                if (readersNo == 0) {
                    if (waiters.get(0) == cell) {
                        waiters.remove(0);
                        return;
                    } else {
                        grantedMode = NONE;
                        wakeUpOthers();
                    }
                }
            }
            cell.sleep();
        }
    }

    /** Leaves this mutex */
    private void leave(Thread t) {
        ThreadInfo info;
        int postedMode = NONE;
        boolean needLock = false;
        synchronized (LOCK) {
            info = getThreadInfo(t);
            switch(grantedMode) {
                case NONE:
                    throw new IllegalStateException();
                case CHAIN:
                    if (info.counts[X] > 0) {
                        postedMode = leaveX(info);
                    } else if (info.counts[S] > 0) {
                        postedMode = leaveS(info);
                    } else {
                        throw new IllegalStateException();
                    }
                    break;
                case X:
                    postedMode = leaveX(info);
                    break;
                case S:
                    postedMode = leaveS(info);
                    break;
            }
            if (postedMode != NONE) {
                int runsize = info.getRunnableCount(postedMode);
                if (runsize != 0) {
                    needLock = reenter(t, postedMode);
                }
            }
        }
        if ((postedMode != NONE) && (info.getRunnableCount(postedMode) > 0)) {
            try {
                if (needLock) {
                    privilegedEnter(t, postedMode);
                }
                List runnables = info.dequeue(postedMode);
                final int size = runnables.size();
                for (int i = 0; i < size; i++) {
                    try {
                        Runnable r = (Runnable) runnables.get(i);
                        r.run();
                    } catch (Exception e) {
                        ErrorManager.getDefault().notify(e);
                    } catch (StackOverflowError e) {
                        e.printStackTrace();
                        ErrorManager.getDefault().notify(e);
                    } catch (ThreadDeath td) {
                        throw td;
                    } catch (Error e) {
                        ErrorManager.getDefault().notify(e);
                    }
                }
                runnables = null;
            } finally {
                leave(t);
            }
        }
    }

    /** Leaves the lock supposing that info.counts[X] is greater than zero */
    private int leaveX(ThreadInfo info) {
        if ((info.counts[X] <= 0) || (info.rsnapshot > info.counts[S])) {
            throw new IllegalStateException();
        }
        if (info.rsnapshot == info.counts[S]) {
            info.counts[X]--;
            if (info.counts[X] == 0) {
                info.rsnapshot = 0;
                if (info.counts[S] > 0) {
                    info.mode = grantedMode = S;
                } else {
                    info.mode = grantedMode = NONE;
                    registeredThreads.remove(info.t);
                }
                if (info.getRunnableCount(S) > 0) {
                    wakeUpReaders();
                    return S;
                }
                wakeUpOthers();
            }
        } else {
            if (info.counts[S] <= 0) {
                throw new IllegalStateException();
            }
            if (--info.counts[S] == 0) {
                if (readersNo <= 0) {
                    throw new IllegalStateException();
                }
                readersNo--;
                return X;
            }
        }
        return NONE;
    }

    /** Leaves the lock supposing that info.counts[S] is greater than zero */
    private int leaveS(ThreadInfo info) {
        if ((info.counts[S] <= 0) || (info.counts[X] > 0)) {
            throw new IllegalStateException();
        }
        info.counts[S]--;
        if (info.counts[S] == 0) {
            info.mode = NONE;
            registeredThreads.remove(info.t);
            if (readersNo <= 0) {
                throw new IllegalStateException();
            }
            readersNo--;
            if (readersNo == 0) {
                grantedMode = NONE;
                if (info.getRunnableCount(X) > 0) {
                    return X;
                }
                wakeUpOthers();
            } else if (info.getRunnableCount(X) > 0) {
                return X;
            } else if ((grantedMode == CHAIN) && (readersNo == 1)) {
                for (int i = 0; i < waiters.size(); i++) {
                    QueueCell qc = (QueueCell) waiters.get(i);
                    synchronized (qc) {
                        if (qc.isGotOut()) {
                            waiters.remove(i--);
                            continue;
                        }
                        ThreadInfo tinfo = getThreadInfo(qc.t);
                        if (tinfo != null) {
                            if (tinfo.mode == S) {
                                if (qc.mode != X) {
                                    throw new IllegalStateException();
                                }
                                if (waiters.size() == 1) {
                                    grantedMode = X;
                                }
                                tinfo.mode = X;
                                waiters.remove(i);
                                qc.wakeMeUp();
                            }
                        }
                        break;
                    }
                }
            }
        }
        return NONE;
    }

    /** Adds this thread to the queue of waiting threads
    * @warning LOCK must be held
    */
    private QueueCell chain(final int requested, final Thread t, final int priority) {
        QueueCell qc = new QueueCell(requested, t);
        qc.priority2 = priority;
        final int size = waiters.size();
        if (size == 0) {
            waiters.add(qc);
        } else if (qc.getPriority() == Integer.MAX_VALUE) {
            waiters.add(0, qc);
        } else {
            QueueCell cursor;
            int i = 0;
            do {
                cursor = (QueueCell) waiters.get(i);
                if (cursor.getPriority() < qc.getPriority()) {
                    waiters.add(i, qc);
                    break;
                }
                i++;
            } while (i < size);
            if (i == size) {
                waiters.add(qc);
            }
        }
        return qc;
    }

    /** Scans through waiters and wakes up them */
    private void wakeUpOthers() {
        if ((grantedMode == X) || (grantedMode == CHAIN)) {
            throw new IllegalStateException();
        }
        if (waiters.size() == 0) {
            return;
        }
        for (int i = 0; i < waiters.size(); i++) {
            QueueCell qc = (QueueCell) waiters.get(i);
            synchronized (qc) {
                if (qc.isGotOut()) {
                    waiters.remove(i--);
                    continue;
                }
                if (compatibleLocks(grantedMode, qc.mode)) {
                    waiters.remove(i--);
                    qc.wakeMeUp();
                    grantedMode = qc.mode;
                    if (getThreadInfo(qc.t) == null) {
                        ThreadInfo ti = new ThreadInfo(qc.t, qc.mode);
                        ti.forced = true;
                        if (qc.mode == S) {
                            readersNo++;
                        }
                        registeredThreads.put(qc.t, ti);
                    }
                } else {
                    grantedMode = CHAIN;
                    break;
                }
            }
        }
    }

    private void wakeUpReaders() {
        assert (grantedMode == NONE) || (grantedMode == S);
        if (waiters.size() == 0) {
            return;
        }
        for (int i = 0; i < waiters.size(); i++) {
            QueueCell qc = (QueueCell) waiters.get(i);
            synchronized (qc) {
                if (qc.isGotOut()) {
                    waiters.remove(i--);
                    continue;
                }
                if (qc.mode == S) {
                    waiters.remove(i--);
                    qc.wakeMeUp();
                    grantedMode = S;
                    if (getThreadInfo(qc.t) == null) {
                        ThreadInfo ti = new ThreadInfo(qc.t, qc.mode);
                        ti.forced = true;
                        readersNo++;
                        registeredThreads.put(qc.t, ti);
                    }
                }
            }
        }
    }

    /** Posts new request for current thread
    * @param mutexMode mutex mode for which the action is rquested
    * @param run the action
    */
    private void postRequest(int mutexMode, Runnable run) {
        if (this == EVENT) {
            doEventRequest(run);
            return;
        }
        Thread t = Thread.currentThread();
        ThreadInfo info;
        synchronized (LOCK) {
            info = getThreadInfo(t);
            if (info != null) {
                if ((mutexMode == info.mode) && (info.counts[(S + X) - mutexMode] == 0)) {
                    enter(mutexMode, t, true);
                } else {
                    info.enqueue(mutexMode, run);
                    return;
                }
            }
        }
        if (info == null) {
            enter(mutexMode, t, true);
            try {
                run.run();
            } finally {
                leave(t);
            }
            return;
        }
        try {
            run.run();
        } finally {
            leave(t);
        }
    }

    /** @param requested is requested mode of locking
    * @return <tt>true</tt> if and only if current mode and requested mode are compatible
    */
    private boolean isCompatible(int requested) {
        return compatibleLocks(grantedMode, requested);
    }

    private ThreadInfo getThreadInfo(Thread t) {
        return (ThreadInfo) registeredThreads.get(t);
    }

    private boolean canUpgrade(int threadGranted, int requested) {
        return (threadGranted == S) && (requested == X) && (readersNo == 1);
    }

    /** Runs the runnable in event queue, either immediatelly,
    * or it posts it into the queue.
    */
    private static void doEvent(Runnable run) {
        if (EventQueue.isDispatchThread()) {
            run.run();
        } else {
            EventQueue.invokeLater(run);
        }
    }

    /** Methods for access to event queue.
    * @param run runabble to post later
    */
    private static void doEventRequest(Runnable run) {
        EventQueue.invokeLater(run);
    }

    /** Methods for access to event queue and waiting for result.
    * @param run runabble to post later
    */
    private static Object doEventAccess(final ExceptionAction run) throws MutexException {
        if (isDispatchThread()) {
            try {
                return run.run();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new MutexException(e);
            }
        }
        final Throwable[] arr = new Throwable[1];
        try {
            final Object[] res = new Object[1];
            EventQueue.invokeAndWait(new Runnable() {

                public void run() {
                    try {
                        res[0] = run.run();
                    } catch (Exception e) {
                        arr[0] = e;
                    } catch (LinkageError e) {
                        arr[0] = e;
                    } catch (StackOverflowError e) {
                        arr[0] = e;
                    }
                }
            });
            if (arr[0] == null) {
                return res[0];
            }
        } catch (InterruptedException e) {
            arr[0] = e;
        } catch (InvocationTargetException e) {
            arr[0] = e;
        }
        if (arr[0] instanceof RuntimeException) {
            throw (RuntimeException) arr[0];
        }
        throw notifyException(ErrorManager.EXCEPTION, arr[0]);
    }

    /** @return true iff current thread is EventDispatchThread */
    static boolean isDispatchThread() {
        boolean dispatch = EventQueue.isDispatchThread();
        if (!dispatch && (Utilities.getOperatingSystem() == Utilities.OS_SOLARIS)) {
            dispatch = (Thread.currentThread().getClass().getName().indexOf("EventDispatchThread") >= 0);
        }
        return dispatch;
    }

    /** Notify exception and returns new MutexException */
    private static final MutexException notifyException(int severity, Throwable t) {
        if (t instanceof InvocationTargetException) {
            t = unfoldInvocationTargetException((InvocationTargetException) t);
        }
        if (t instanceof Error) {
            annotateEventStack(t);
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            annotateEventStack(t);
            throw (RuntimeException) t;
        }
        MutexException exc = new MutexException((Exception) t);
        ErrorManager.getDefault().annotate(exc, t);
        return exc;
    }

    private static final void annotateEventStack(Throwable t) {
        ErrorManager.getDefault().annotate(t, new Exception("Caught here in mutex"));
    }

    private static final Throwable unfoldInvocationTargetException(InvocationTargetException e) {
        Throwable ret;
        do {
            ret = e.getTargetException();
            if (ret instanceof InvocationTargetException) {
                e = (InvocationTargetException) ret;
            } else {
                e = null;
            }
        } while (e != null);
        return ret;
    }

    /** Action to be executed in a mutex without throwing any checked exceptions.
    * Unchecked exceptions will be propagated to calling code.
    */
    public static interface Action extends ExceptionAction {

        /** Execute the action.
        * @return any object, then returned from {@link Mutex#readAccess(Mutex.Action)} or {@link Mutex#writeAccess(Mutex.Action)}
        */
        public Object run();
    }

    /** Action to be executed in a mutex, possibly throwing checked exceptions.
    * May throw a checked exception, in which case calling
    * code should catch the encapsulating exception and rethrow the
    * real one.
    * Unchecked exceptions will be propagated to calling code without encapsulation.
    */
    public static interface ExceptionAction {

        /** Execute the action.
        * Can throw an exception.
        * @return any object, then returned from {@link Mutex#readAccess(Mutex.ExceptionAction)} or {@link Mutex#writeAccess(Mutex.ExceptionAction)}
        * @exception Exception any exception the body needs to throw
        */
        public Object run() throws Exception;
    }

    private static final class ThreadInfo {

        /** t is forcibly sent from waiters to enter() by wakeUpOthers() */
        boolean forced;

        /** ThreadInfo for this Thread */
        final Thread t;

        /** granted mode */
        int mode;

        /** enter counter */
        int[] counts;

        /** queue of runnable rquests that are to be executed (in X mode) right after S mode is left
        * deadlock avoidance technique
        */
        List[] queues;

        /** value of counts[S] when the mode was upgraded
        * rsnapshot works as follows:
        * if a thread holds the mutex in the S mode and it reenters the mutex
        * and requests X and the mode can be granted (no other readers) then this
        * variable is set to counts[S]. This is used in the leave method in the X branch.
        * (X mode is granted by other words)
        * If rsnapshot is less than counts[S] then the counter is decremented etc. If the rsnapshot is
        * equal to count[S] then count[X] is decremented. If the X counter is zeroed then
        * rsnapshot is zeroed as well and current mode is downgraded to S mode.
        * rsnapshot gets less than counts[S] if current mode is X and the mutex is reentered
        * with S request.
        */
        int rsnapshot;

        public ThreadInfo(Thread t, int mode) {
            this.t = t;
            this.mode = mode;
            this.counts = new int[MODE_COUNT];
            this.queues = new List[MODE_COUNT];
            counts[mode] = 1;
        }

        public String toString() {
            return super.toString() + " thread: " + t + " mode: " + mode + " X: " + counts[2] + " S: " + counts[3];
        }

        /** Adds the Runnable into the queue of waiting requests */
        public void enqueue(int mode, Runnable run) {
            if (queues[mode] == null) {
                queues[mode] = new ArrayList(13);
            }
            queues[mode].add(run);
        }

        /** @return a List of enqueued Runnables - may be null */
        public List dequeue(int mode) {
            List ret = queues[mode];
            queues[mode] = null;
            return ret;
        }

        public int getRunnableCount(int mode) {
            return ((queues[mode] == null) ? 0 : queues[mode].size());
        }
    }

    /** This class is defined only for better understanding of thread dumps where are informations like
    * java.lang.Object@xxxxxxxx owner thread_x
    *   wait for enter thread_y
    */
    private static final class InternalLock {

        InternalLock() {
        }
    }

    private static final class QueueCell {

        int mode;

        Thread t;

        boolean signal;

        boolean left;

        /** priority of the cell */
        int priority2;

        public QueueCell(int mode, Thread t) {
            this.mode = mode;
            this.t = t;
            this.left = false;
            this.priority2 = 0;
        }

        public String toString() {
            return super.toString() + " mode: " + mode + " thread: " + t;
        }

        /** @return priority of this cell */
        public long getPriority() {
            return ((priority2 == 0) ? t.getPriority() : priority2);
        }

        /** @return true iff the thread left sleep */
        public boolean isGotOut() {
            return left;
        }

        /** current thread will sleep until wakeMeUp is called
        * if wakeMeUp was already called then the thread will not sleep
        */
        public synchronized void sleep() {
            try {
                while (!signal) {
                    try {
                        wait();
                        return;
                    } catch (InterruptedException e) {
                        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
                    }
                }
            } finally {
                left = true;
            }
        }

        /** sends signal to a sleeper - to a thread that is in the sleep() */
        public void wakeMeUp() {
            signal = true;
            notifyAll();
        }
    }

    /** Provides access to Mutex's internal methods.
     *
     * This class can be used when one wants to avoid creating a
     * bunch of Runnables. Instead,
     * <pre>
     * try {
     *     enterXAccess ();
     *     yourCustomMethod ();
     * } finally {
     *     exitXAccess ();
     * }
     * </pre>
     * can be used.
     *
     * You must, however, control the related Mutex, i.e. you must be creator of
     * the Mutex.
     *
     * @since 1.17
     */
    public static final class Privileged {

        private Mutex parent;

        final void setParent(Mutex parent) {
            this.parent = parent;
        }

        public void enterReadAccess() {
            parent.readEnter(Thread.currentThread());
        }

        public void enterWriteAccess() {
            parent.writeEnter(Thread.currentThread());
        }

        public void exitReadAccess() {
            parent.leave(Thread.currentThread());
        }

        public void exitWriteAccess() {
            parent.leave(Thread.currentThread());
        }
    }
}
