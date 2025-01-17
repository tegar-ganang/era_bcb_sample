package org.das2.system;

import org.das2.util.ExceptionHandler;
import org.das2.util.DasExceptionHandler;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.logging.Logger;
import org.das2.DasApplication;

/** Utility class for synchronous execution.
 * This class maintains a pool of threads that are used to execute arbitrary
 * code.  This class also serves as a central place to catch and handle
 * unchecked exceptions.
 *
 * The {@link #invokeLater(java.lang.Runnable)} method is similar to the
 * SwingUtilities {@link javax.swing.SwingUtilities#invokeLater(java.lang.Runnable)}
 * method, except that the request is not executed on the event thread.
 *
 * The {@link #invokeLater(java.lang.Runnable, java.lang.Object)},
 * the {@link #invokeAfter(java.lang.Runnable, java.lang.Object)},
 * and the {@link #waitFor(java.lang.Object)} methods are designed to work
 * together.  Both of the first two methods execute code asynchronously with
 * respect to the calling thread.  Multiple requests made with a call to
 * invokeLater that specified the same lock can execute at the same time,
 * but not while a request made with the invokeAfter with the same lock
 * is processing.  Any requests made before an invokeAfter request with the
 * same lock will finish before that invokeAfter request begins. An
 * invokeAfter request will finish before any requests with the same lock made
 * after that invokeAfter request begins.  The {@link #waitFor(java.lang.Object)}
 * method will cause the calling thread to block until all requests with the
 * specified lock finish.
 */
public final class RequestProcessor {

    private static final BlockingRequestQueue queue = new BlockingRequestQueue();

    private static final WeakHashMap runnableQueueMap = new WeakHashMap();

    private static final Runner runner = new Runner();

    private static final Runnable SHUTDOWN = new Runnable() {

        public void run() {
        }
    };

    private static int maxThreadCount = 10;

    private static int threadCount = 0;

    private static final Object THREAD_COUNT_LOCK = new Object();

    private static final Logger logger = DasLogger.getLogger(DasLogger.SYSTEM_LOG);

    private static int threadOrdinal = 0;

    private RequestProcessor() {
    }

    private static void setJob(Runnable job) {
        RequestThread thread = (RequestThread) Thread.currentThread();
        thread.setJob(job);
    }

    private static class RequestThread extends Thread {

        private WeakReference job;

        private RequestThread(Runnable run, String name) {
            super(run, name);
        }

        private void setJob(Runnable job) {
            this.job = new WeakReference(job);
        }

        private Runnable getJob() {
            return (Runnable) job.get();
        }
    }

    private static void newThread() {
        String name = "RequestProcessor[" + (threadOrdinal++) + "]";
        RequestThread t = new RequestThread(runner, name);
        t.setPriority(Thread.NORM_PRIORITY);
        t.start();
    }

    /** Executes run.run() asynchronously on a thread from the thread pool.
     * @param run the task to be executed.
     */
    public static void invokeLater(Runnable run) {
        logger.fine("invokeLater " + run);
        synchronized (THREAD_COUNT_LOCK) {
            if (threadCount < maxThreadCount) {
                newThread();
            }
        }
        queue.add(run);
    }

    /** Executes run.run() asynchronously on a thread from the thread pool.
     * The task will not be executed until after all requests made with
     * {@link #invokeAfter(java.lang.Runnable, java.lang.Object)} with the same
     * lock have finished.
     * @param run the taks to be executed.
     * @param lock associates run with other tasks.
     */
    public static void invokeLater(Runnable run, Object lock) {
        logger.fine("invokeLater " + run + " " + lock);
        synchronized (THREAD_COUNT_LOCK) {
            if (threadCount < maxThreadCount) {
                newThread();
            }
        }
        synchronized (runnableQueueMap) {
            RunnableQueue rq = (RunnableQueue) runnableQueueMap.get(lock);
            if (rq == null) {
                rq = new RunnableQueue();
                runnableQueueMap.put(lock, rq);
            }
            rq.add(run, false);
            queue.add(rq);
        }
    }

    /** Executes run.run() asynchronously on a thread from the thread pool.
     * The task will not be executed until after all requests made with
     * {@link #invokeAfter(java.lang.Runnable, java.lang.Object)} or
     * {@link #invokeLater(java.lang.Runnable, java.lang.Object)} with the same
     * lock have finished.
     * @param run the taks to be executed.
     * @param lock associates run with other tasks.
     */
    public static void invokeAfter(Runnable run, Object lock) {
        logger.fine("invokeAfter " + run + " " + lock);
        synchronized (THREAD_COUNT_LOCK) {
            if (threadCount < maxThreadCount) {
                newThread();
            }
        }
        synchronized (runnableQueueMap) {
            RunnableQueue rq = (RunnableQueue) runnableQueueMap.get(lock);
            if (rq == null) {
                rq = new RunnableQueue();
                runnableQueueMap.put(lock, rq);
            }
            rq.add(run, true);
            queue.add(rq);
        }
    }

    /** Blocks until all tasks with the same lock have finished.
     * @param lock
     * @throws InterruptedException if the current thread is
     *      interrupted while waiting.
     */
    public static void waitFor(Object lock) throws InterruptedException {
        WaitTask wt = new WaitTask();
        synchronized (wt) {
            while (true) {
                invokeLater(wt, lock);
                wt.wait();
                return;
            }
        }
    }

    public static void shutdown() {
        queue.add(SHUTDOWN);
    }

    private static class Runner implements Runnable {

        public void run() {
            synchronized (THREAD_COUNT_LOCK) {
                threadCount++;
            }
            try {
                while (true) {
                    try {
                        Runnable run = queue.remove();
                        if (run == SHUTDOWN) {
                            queue.add(run);
                            break;
                        }
                        logger.fine("running " + run);
                        if (run != null) {
                            setJob(run);
                            run.run();
                            logger.fine("completed " + run);
                        }
                        synchronized (THREAD_COUNT_LOCK) {
                            if (threadCount > maxThreadCount) {
                                break;
                            }
                        }
                    } catch (ThreadDeath td) {
                        throw td;
                    } catch (Throwable t) {
                        logger.fine("uncaught exception " + t);
                        ExceptionHandler eh = DasApplication.getDefaultApplication().getExceptionHandler();
                        if (eh == null) {
                            DasExceptionHandler.handleUncaught(t);
                        } else {
                            eh.handleUncaught(t);
                        }
                        Thread.interrupted();
                    }
                }
            } finally {
                synchronized (THREAD_COUNT_LOCK) {
                    threadCount--;
                }
            }
        }
    }

    private static class WaitTask implements Runnable {

        public synchronized void run() {
            notifyAll();
        }
    }

    private static class RunnableQueue implements Runnable {

        private LinkedList list = new LinkedList();

        private int readCount = 0;

        private Object writer;

        public void run() {
            Runnable run = null;
            RequestEntry entry = null;
            Logger logger = DasLogger.getLogger(DasLogger.SYSTEM_LOG);
            while (run == null) {
                synchronized (this) {
                    entry = (RequestEntry) list.getFirst();
                    if (entry.async && readCount == 0 && writer == null) {
                        list.removeFirst();
                        writer = entry;
                        run = entry.run;
                    } else if (!entry.async && writer == null) {
                        list.removeFirst();
                        readCount++;
                        run = entry.run;
                    }
                }
            }
            logger.fine("Starting :" + run);
            run.run();
            logger.fine("Finished :" + run);
            synchronized (this) {
                if (entry.async) {
                    writer = null;
                } else {
                    readCount--;
                }
                notifyAll();
            }
        }

        synchronized void add(Runnable run, boolean async) {
            RequestEntry entry = new RequestEntry();
            entry.run = run;
            entry.async = async;
            list.add(entry);
        }
    }

    private static class RequestEntry {

        Runnable run;

        boolean async;
    }

    private static class BlockingRequestQueue {

        private LinkedList list;

        BlockingRequestQueue() {
            list = new LinkedList();
        }

        synchronized void add(Runnable r) {
            list.add(r);
            notify();
        }

        synchronized Runnable remove() {
            while (list.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException ie) {
                }
                ;
            }
            return (Runnable) list.removeFirst();
        }
    }
}
