package net.sf.compositor.util;

import java.util.LinkedList;
import java.util.Queue;

/**
 * The <code>Thread</code> that never dies.
 * <p>
 * This is a self-starting thread: you need never <code>{@link #start()}</code> it.
 * Instead, you get a thread from a {@link ThreadPool} and give it a task with
 * {@link #runTask(Runnable)} or {@link #addTask(Runnable)}.
 * As it's not a daemon thread, you need to {@link #close()} it if you
 * want to stop the JVM without <code>System.exit()</code>. A simple way to do this
 * is to close the {@link ThreadPool}.
 */
public class ImmortalThread extends Thread {

    /** Logging info */
    private static String s_thisClass = StackProbe.getMyClassName() + '.';

    /** Log */
    private static Log s_log = Log.getInstance();

    /** Thread status: <em>no longer running</em>. */
    public static final int CLOSED = 0;

    /** Thread status: <em>available for work</em>. */
    public static final int FREE = 1;

    /** Thread status: <em>taken from pool, awaiting instruction</em>. */
    public static final int ALLOCATED = 2;

    /** Thread status: <em>at work</em>. */
    public static final int BUSY = 3;

    /** Thread status: <em>task(s) waiting, about to start</em>. */
    public static final int QUEUEING = 4;

    /** Holds the tasks for this thread. */
    private Queue<Runnable> m_tasks;

    /**
	 * Holds work status of thread. {@link #CLOSED}, {@link #FREE},
	 * {@link #ALLOCATED}, {@link #BUSY} or {@link #QUEUEING}
	 */
    private int m_state = FREE;

    /** Holds time to wait between checking for exit */
    private int m_sleepTime;

    /** Shows whether this thread will die when it completes its tasks. */
    private boolean m_running = true;

    /** Holds the containing {@link ThreadPool}. */
    private ThreadPool m_threadPool;

    /**
	 * Constructs with the given sleep time and starts the constructed thread.
	 *
	 * @param sleepTime  How long to wait between checking for exit
	 * @param threadPool Who to tell when we're finished
	 */
    public ImmortalThread(final int sleepTime, final ThreadPool threadPool) {
        super();
        final String thisMethod = s_thisClass + "<init>: thread " + this + ": ";
        if (s_log.isOnVerbose()) s_log.write(Log.VERBOSE, thisMethod + "start");
        m_tasks = new LinkedList<Runnable>();
        m_sleepTime = sleepTime;
        m_threadPool = threadPool;
        super.start();
        if (s_log.isOnVerbose()) s_log.write(Log.VERBOSE, thisMethod + "end");
    }

    /**
	 * Calls {@link #ImmortalThread(int,ThreadPool)} with a sleep time of 1 second.
	 */
    public ImmortalThread(final ThreadPool threadPool) {
        this(1000, threadPool);
    }

    /**
	 * Tells this thread to close when it's finished what it's currently doing.
	 */
    public void close() {
        final String thisMethod = s_thisClass + "close: thread " + this + ": ";
        if (s_log.isOnVerbose()) s_log.write(Log.VERBOSE, thisMethod + "start");
        m_running = false;
        if (s_log.isOnVerbose()) s_log.write(Log.VERBOSE, thisMethod + "end");
    }

    /**
	 * You could start this thread, but it will do nothing and return, as this thread class starts itself.
	 * Instead, use {@link #runTask(Runnable)} or {@link #addTask(Runnable)} to make this thread do something.
	 */
    @Override
    public void start() {
        s_log.error("Please don't bother calling ImmortalThread.start");
    }

    /**
	 * Sleeps the calling thread for the given time or until <code>InterruptedException</code>.
	 * No exceptions are thrown.
	 */
    public static void snooze(final long time) {
        try {
            sleep(time);
        } catch (InterruptedException e) {
        }
    }

    /**
	 * Don't call this method.  The constructor {@link #ImmortalThread(int,ThreadPool)}
	 * starts the thread, and so calls this method, thereby running tasks as
	 * specified with {@link #runTask(Runnable)} or {@link #addTask(Runnable)}.
	 * This method never ends, so thread never dies.  As it's not a daemon
	 * thread, you need to {@link #close()} it if you want to stop the JVM
	 * without <code>System.exit()</code>.
	 */
    @Override
    public void run() {
        final String thisMethod = s_thisClass + "run: ";
        final boolean logVerbose = s_log.isOnVerbose();
        if (logVerbose) s_log.write(Log.VERBOSE, thisMethod + "start");
        Runnable task = null;
        try {
            while (m_running) {
                while (m_running && task == null) {
                    if (logVerbose) s_log.write(Log.VERBOSE, thisMethod + "waiting for task.");
                    try {
                        synchronized (this) {
                            wait(m_sleepTime);
                        }
                    } catch (InterruptedException e) {
                        s_log.warn(thisMethod + "wait interrupted: " + e);
                    }
                    if (logVerbose) s_log.write(Log.VERBOSE, thisMethod + "stopped waiting for task.");
                    task = nextTask();
                    if (logVerbose && task != null) s_log.write(Log.VERBOSE, thisMethod + "task is: " + task);
                }
                if (task != null) {
                    if (logVerbose) s_log.write(Log.VERBOSE, thisMethod + "running task...");
                    try {
                        task.run();
                    } catch (Exception e) {
                        s_log.error(thisMethod + e, e);
                        if (s_log.isOnDebug()) s_log.write(Log.DEBUG, thisMethod + "ah, ah, ah, ah, staying alive");
                    } catch (Error e) {
                        s_log.error(thisMethod + "re-throwing " + e, e);
                        throw e;
                    }
                    synchronized (this) {
                        if (logVerbose) s_log.write(Log.VERBOSE, thisMethod + "task over - calling notifyAll on " + this);
                        notifyAll();
                        task = nextTask();
                        if (logVerbose) s_log.write(Log.VERBOSE, thisMethod + "next task: " + task);
                        if (task == null) {
                            if (logVerbose) s_log.write(Log.VERBOSE, thisMethod + "freeing thread...");
                            m_state = FREE;
                            try {
                                m_threadPool.freeElement(this);
                            } catch (ResourcePoolException e) {
                                s_log.warn(thisMethod + "freeing this thread in pool: " + e);
                            }
                        }
                    }
                }
            }
        } finally {
            synchronized (this) {
                m_state = CLOSED;
            }
        }
        if (s_log.isOnTrace()) s_log.write(Log.TRACE, thisMethod + "end");
    }

    private synchronized Runnable nextTask() {
        Runnable result = null;
        if (m_tasks.size() > 0) {
            result = m_tasks.remove();
            m_state = BUSY;
        }
        return result;
    }

    /**
	 * Runs a single task.
	 *
	 * @param task  A <code>runnable</code> object that performs some task
	 * @throws IllegalThreadStateException  when state is not {@link #ALLOCATED}
	 * @throws NullPointerException  when <code>task</code> is <code>null</code>
	 * @see #addTask(Runnable)
	 */
    public void runTask(final Runnable task) throws IllegalThreadStateException {
        final String thisMethod = s_thisClass + "runTask: ";
        final boolean logVerbose = s_log.isOnVerbose();
        if (logVerbose) s_log.write(Log.VERBOSE, thisMethod + "start");
        if (task == null) {
            throw new NullPointerException(thisMethod + "task is null");
        }
        synchronized (this) {
            if (logVerbose) s_log.write(Log.VERBOSE, thisMethod + "checking state...");
            if (m_state != ALLOCATED) {
                if (logVerbose) s_log.write(Log.VERBOSE, thisMethod + "thread not allocated - throwing IllegalThreadStateException...");
                throw new IllegalThreadStateException("Thread not ALLOCATED");
            }
            if (logVerbose) s_log.write(Log.VERBOSE, thisMethod + "adding task (" + task + ")...");
            m_tasks.add(task);
            if (logVerbose) s_log.write(Log.VERBOSE, thisMethod + "setting state...");
            m_state = QUEUEING;
            notify();
        }
        if (logVerbose) s_log.write(Log.VERBOSE, thisMethod + "end");
    }

    /**
	 * Adds a task to run.
	 *
	 * @param task  A <code>runnable</code> object that performs some task
	 * @throws IllegalThreadStateException  if state is not {@link #FREE} or
	 *         {@link #ALLOCATED} or {@link #QUEUEING}
	 * @throws NullPointerException  when <code>task</code> is <code>null</code>
	 */
    public void addTask(final Runnable task) throws IllegalThreadStateException {
        if (task == null) {
            throw new NullPointerException("Task is null");
        }
        synchronized (this) {
            if (m_state != ALLOCATED && m_state != QUEUEING && m_state != BUSY) {
                throw new IllegalThreadStateException("Thread not ALLOCATED, QUEUEING or BUSY.");
            }
            m_tasks.add(task);
            m_state = QUEUEING;
            notify();
        }
    }

    public int getThreadState() {
        return m_state;
    }

    /**
	 * Sets state of this thread to {@link #ALLOCATED}.
	 */
    protected synchronized void allocate() {
        if (m_state == FREE) {
            m_state = ALLOCATED;
        } else {
            throw new IllegalThreadStateException("Thread not FREE.");
        }
    }

    public void setSleepTime(final int i) {
        m_sleepTime = i;
    }

    public int getSleepTime() {
        return m_sleepTime;
    }

    /**
	 * Waits at most <code>millis</code> milliseconds for this thread to finish its task(s).
	 * A timeout of <code>0</code> means to wait forever.
	 * <p>
	 * This should override <code>Thread.join(long)</code>, but that's final,
	 * so we have to give this a different name. 8-(
	 *
	 * @param millis The time to wait in milliseconds
	 * @throws InterruptedException if another thread has interrupted the current thread.
	 *         The <i>interrupted status</i> of the current thread is cleared when this exception is thrown.
	 * @throws IllegalArgumentException if <code>millis</code> &lt; 0
	 */
    public synchronized void join_(final long millis) throws InterruptedException {
        final String thisMethod = s_thisClass + "join_: ";
        if (s_log.isOnVerbose()) s_log.write(Log.VERBOSE, thisMethod + "start");
        final long base = System.currentTimeMillis();
        long now = 0;
        if (millis < 0) {
            throw new IllegalArgumentException("Timeout value is negative");
        }
        if (millis == 0) {
            while (getThreadState() != FREE) {
                if (s_log.isOnVerbose()) s_log.write(Log.VERBOSE, thisMethod + "waiting...");
                wait(0);
                if (s_log.isOnVerbose()) s_log.write(Log.VERBOSE, thisMethod + "finished waiting");
            }
        } else {
            while (getThreadState() != FREE) {
                final long delay = millis - now;
                if (delay <= 0) {
                    break;
                }
                if (s_log.isOnVerbose()) s_log.write(Log.VERBOSE, thisMethod + "waiting...");
                wait(delay);
                if (s_log.isOnVerbose()) s_log.write(Log.VERBOSE, thisMethod + "finished waiting");
                now = System.currentTimeMillis() - base;
            }
        }
        if (s_log.isOnVerbose()) s_log.write(Log.VERBOSE, thisMethod + "end");
    }

    /**
	 * Waits at most <code>millis</code> milliseconds plus
	 * <code>nanos</code> nanoseconds for this thread to finish its task(s).
	 * <p>
	 * This should override <code>Thread.join(long,int)</code>, but that's final,
	 * so we have to give this a different name. 8-(
	 *
	 * @param millis The time to wait in milliseconds
	 * @param nanos 0-999999 additional nanoseconds to wait - subject to some rounding 8-)
	 * @throws IllegalArgumentException if the value of millis is negative or the value of nanos is not in the range 0-999999
	 * @throws InterruptedException if another thread has interrupted the current thread.
	 *         The <i>interrupted status</i> of the current thread is cleared when this exception is thrown.
	 */
    public synchronized void join_(final long millis, final int nanos) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("Timeout value is negative");
        }
        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException("Nanosecond timeout value out of range");
        }
        long actualMillis = millis;
        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            actualMillis++;
        }
        join_(actualMillis);
    }

    /**
	 * Waits for this thread to finish its task(s).
	 * <p>
	 * This should override <code>Thread.join()</code>, but that's final,
	 * so we have to give this a different name. 8-(
	 *
	 * @throws InterruptedException if another thread has interrupted the current thread.
	 *         The <i>interrupted status</i> of the current thread is cleared when this exception is thrown.
	 */
    public void join_() throws InterruptedException {
        join_(0);
    }
}
