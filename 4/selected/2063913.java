package com.volantis.osgi.cm.async;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Iterator;
import java.util.List;

/**
 * Dispatches tasks ({@link Runnable} instances) asynchronously on a background
 * thread.
 */
public class AsynchronousDispatcher {

    /**
     * The queue between the dispatcher and the background thread.
     */
    private final BlockingQueue taskQueue;

    /**
     * The background thread.
     */
    private final DispatcherThread thread;

    /**
     * Initialise.
     */
    public AsynchronousDispatcher() {
        taskQueue = new LinkedBlockingQueue();
        thread = new DispatcherThread(taskQueue);
    }

    /**
     * Queue a task to be processed by the background thread.
     *
     * @param task The task to process.
     */
    public void queueAsynchronousAction(Runnable task) {
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    /**
     * Stop the background thread.
     *
     * <p>This stops the thread cleanly making sure that it has run all its
     * currently queued tasks before exiting. It will not however prevent other
     * tasks from being added to the queue, they will just not be run.</p>
     *
     * todo Prevent other tasks from being queued while waiting for the todo
     * background thread to stop.
     */
    public void stop() {
        queueAsynchronousAction(new Runnable() {

            public void run() {
                throw new TerminateException();
            }
        });
        try {
            thread.join();
            List throwables = thread.getThrowables();
            int count = throwables.size();
            if (count == 1) {
                Throwable t = (Throwable) throwables.get(0);
                if (t != null) {
                    if (t instanceof Error) {
                        throw (Error) t;
                    } else if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    } else {
                        throw new UndeclaredThrowableException(t);
                    }
                }
            } else if (count > 1) {
                StringWriter writer = new StringWriter();
                PrintWriter printWriter = new PrintWriter(writer);
                for (Iterator i = throwables.iterator(); i.hasNext(); ) {
                    Throwable t = (Throwable) i.next();
                    t.printStackTrace(printWriter);
                }
                throw new RuntimeException("The following errors occurred in background thread\n" + writer.getBuffer());
            }
        } catch (InterruptedException e) {
        }
    }
}
