package fr.x9c.cadmium.primitives.threads;

import fr.x9c.cadmium.kernel.CadmiumThread;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Context;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.FalseExit;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;
import fr.x9c.cadmium.primitives.systhreads.ThreadStatus;

/**
 * This class provides the primitives for vm threads.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Threads {

    /** Index of 'terminated' field in 'caml_thread_descr' structure. */
    private static final int TERMINATED = 2;

    /** Value indicating that I/O is done. */
    private static final Value RESUMED_IO = Value.createFromLong(3);

    /** Number of milliseconds per second. */
    private static final double MILLISECS_PER_SEC = 1000.0;

    /**
     * No instance of this class.
     */
    private Threads() {
    }

    /**
     * Initializes the thread system.
     * @param ctxt context
     * @param unit ignored
     * @return <i>unit</i>
     */
    @Primitive
    public static Value thread_initialize(final CodeRunner ctxt, final Value unit) {
        final Value res = fr.x9c.cadmium.primitives.systhreads.Threads.caml_thread_initialize(ctxt, unit);
        return res;
    }

    /**
     * Does nothing.
     * @param ctxt context
     * @param unit ignored
     * @return <i>unit</i>
     */
    @Primitive
    public static Value thread_initialize_preemption(final CodeRunner ctxt, final Value unit) {
        return Value.UNIT;
    }

    /**
     * Creates a new thread.
     * @param ctxt context
     * @param clos closure to execute in created thread
     * @return the thread descriptor of the newly created thread
     */
    @Primitive
    public static Value thread_new(final CodeRunner ctxt, final Value clos) {
        return fr.x9c.cadmium.primitives.systhreads.Threads.caml_thread_new(ctxt, clos);
    }

    /**
     * Returns the identifier from a thread descriptor.
     * @param ctxt context
     * @param th thread descriptor
     * @return identifier of thread
     */
    @Primitive
    public static Value thread_id(final CodeRunner ctxt, final Value th) {
        return fr.x9c.cadmium.primitives.systhreads.Threads.caml_thread_id(ctxt, th);
    }

    /**
     * Pauses the current thread and runs another thread.
     * @param ctxt context
     * @param unit ignored
     * @return <i>unit</i>
     * @throws FalseExit if another threads has exited the program
     * @throws Fail.Exception if an asynchronous exception should be thrown
     */
    @Primitive
    public static Value thread_yield(final CodeRunner ctxt, final Value unit) throws FalseExit, Fail.Exception {
        return fr.x9c.cadmium.primitives.systhreads.Threads.caml_thread_yield(ctxt, unit);
    }

    /**
     * Pauses the current thread and runs another thread. <br/>
     * Equivalent to {@link #thread_yield(CodeRunner, Value)}.
     * @param ctxt context
     * @param unit ignored
     * @return <i>unit</i>
     */
    @Primitive
    public static Value thread_request_reschedule(final CodeRunner ctxt, final Value unit) throws FalseExit, Fail.Exception {
        return thread_yield(ctxt, unit);
    }

    /**
     * Suspends the current thread.
     * @param ctxt context
     * @param unit ignored
     * @return <i>unit</i>
     */
    @Primitive
    public static Value thread_sleep(final CodeRunner ctxt, final Value unit) throws FalseExit, Fail.Exception {
        final Context context = ctxt.getContext();
        final Thread current = Thread.currentThread();
        if (current instanceof CadmiumThread) {
            context.enterBlockingSection();
            ((CadmiumThread) current).pause();
            context.leaveBlockingSection();
        } else {
            assert false : "current thread should be a CadmiumThread";
        }
        return Value.UNIT;
    }

    /**
     * Does nothing.
     * @param ctxt context
     * @param fd ignored
     * @return <i>unit</i>
     */
    @Primitive
    public static Value thread_wait_read(final CodeRunner ctxt, final Value fd) {
        return Value.UNIT;
    }

    /**
     * Does nothing.
     * @param ctxt context
     * @param fd ignored
     * @return <i>unit</i>
     */
    @Primitive
    public static Value thread_wait_write(final CodeRunner ctxt, final Value fd) {
        return Value.UNIT;
    }

    /**
     * Does nothing.
     * @param ctxt context
     * @param fd ignored
     * @return {@link #RESUMED_IO}, that is 3
     */
    @Primitive
    public static Value thread_wait_timed_read(final CodeRunner ctxt, final Value fd) {
        return Threads.RESUMED_IO;
    }

    /**
     * Does nothing.
     * @param ctxt context
     * @param fd ignored
     * @return {@link #RESUMED_IO}, that is 3
     */
    @Primitive
    public static Value thread_wait_timed_write(final CodeRunner ctxt, final Value fd) {
        return Threads.RESUMED_IO;
    }

    /**
     * Pauses the current thread and runs another thread.
     * @param ctxt context
     * @param fd ignored
     * @return <i>unit</i>
     */
    @Primitive
    public static Value thread_select(final CodeRunner ctxt, final Value fd) throws FalseExit, Fail.Exception {
        return thread_yield(ctxt, fd);
    }

    /**
     * Tests whether an input channel is ready.
     * @param ctxt context
     * @param vchan channel
     * @return {@link fr.x9c.cadmium.kernel.Value#TRUE}
     */
    @Primitive
    public static Value thread_inchan_ready(final CodeRunner ctxt, final Value vchan) {
        return Value.TRUE;
    }

    /**
     * Tests whether an output channel is ready.
     * @param ctxt context
     * @param vchan ignored
     * @param vsize ignored
     * @return {@link fr.x9c.cadmium.kernel.Value#TRUE}
     */
    @Primitive
    public static Value thread_outchan_ready(final CodeRunner ctxt, final Value vchan, final Value vsize) {
        return Value.TRUE;
    }

    /**
     * Pauses the current thread for a given time.
     * @param ctxt context
     * @param time pause duration
     * @return <i>unit</i>
     */
    @Primitive
    public static Value thread_delay(final CodeRunner ctxt, final Value time) throws FalseExit, Fail.Exception {
        final Context context = ctxt.getContext();
        context.enterBlockingSection();
        try {
            Thread.sleep((long) (time.asBlock().asDouble() * Threads.MILLISECS_PER_SEC));
        } catch (final InterruptedException ie) {
            final FalseExit fe = FalseExit.createFromContext(context);
            fe.fillInStackTrace();
            throw fe;
        }
        context.leaveBlockingSection();
        return Value.UNIT;
    }

    /**
     * Joins a thread, that is waits for it to die.
     * @param ctxt context
     * @param th thread to join
     * @return <i>unit</i>
     */
    @Primitive
    public static Value thread_join(final CodeRunner ctxt, final Value th) throws FalseExit, Fail.Exception {
        return fr.x9c.cadmium.primitives.systhreads.Threads.caml_thread_join(ctxt, th);
    }

    /**
     * Does nothing.
     * @param ctxt context
     * @param pid ignored
     * @return <i>unit</i>
     */
    @Primitive
    public static Value thread_wait_pid(final CodeRunner ctxt, final Value pid) {
        return Value.UNIT;
    }

    /**
     * Resumes a suspended thread.
     * @param ctxt context
     * @param th thread to resume
     * @return <i>unit</i>
     * @throws Fail.Exception if thread is not alive or not suspended
     */
    @Primitive
    public static Value thread_wakeup(final CodeRunner ctxt, final Value th) throws Fail.Exception {
        final ThreadStatus ts = (ThreadStatus) th.asBlock().get(Threads.TERMINATED).asBlock().asCustom();
        if (!ts.isAlive()) {
            Fail.failWith("Thread.wakeup: killed thread");
        } else if (!ts.isSuspended()) {
            Fail.failWith("Thread.wakeup: thread not suspended");
        } else {
            ts.resume();
        }
        return Value.UNIT;
    }

    /**
     * Returns the thread descriptor of the currently running thread.
     * @param ctxt context
     * @param unit ignored
     * @return the thread descriptor of the currently running thread
     * @throws Fail.Exception if threads are not initiliazed
     */
    @Primitive
    public static Value thread_self(final CodeRunner ctxt, final Value unit) throws Fail.Exception {
        return fr.x9c.cadmium.primitives.systhreads.Threads.caml_thread_self(ctxt, unit);
    }

    /**
     * Kills a given thread.
     * @param ctxt context
     * @param th thread to kill
     * @return <i>unit</i>
     */
    @Primitive
    public static Value thread_kill(final CodeRunner ctxt, final Value th) {
        ((ThreadStatus) th.asBlock().get(Threads.TERMINATED).asBlock().asCustom()).terminate();
        ctxt.getContext().enterBlockingSection();
        return Value.UNIT;
    }

    /**
     * Prints the uncaught exception of a thread.
     * @param ctxt context
     * @param exn uncaucght exception
     * @return <i>unit</i>
     */
    @Primitive
    public static Value thread_uncaught_exception(final CodeRunner ctxt, final Value exn) throws FalseExit, Fail.Exception {
        return fr.x9c.cadmium.primitives.systhreads.Threads.caml_thread_uncaught_exception(ctxt, exn);
    }
}
