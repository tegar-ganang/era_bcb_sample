package fr.x9c.cadmium.primitives.systhreads;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import fr.x9c.cadmium.kernel.Block;
import fr.x9c.cadmium.kernel.ByteCodeRunner;
import fr.x9c.cadmium.kernel.CadmiumThread;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Context;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.FalseExit;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Signals;
import fr.x9c.cadmium.kernel.Value;
import fr.x9c.cadmium.util.Misc;

/**
 * This class provides the primitives for system threads.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.2
 * @since 1.0
 */
@PrimitiveProvider
public final class Threads {

    /**
     * Identifier for slot containing a
     * {@link java.util.concurrent.atomic.AtomicInteger} giving the id of
     * the <i>next</i> thread to be created.
     */
    private static final Object SLOT_ID = new Object();

    /** Index of 'ident' field in 'caml_thread_descr' structure. */
    private static final int IDENT = 0;

    /** Index of 'start_closure' field in 'caml_thread_descr' structure. */
    private static final int START_CLOSURE = 1;

    /** Index of 'terminated' field in 'caml_thread_descr' structure. */
    private static final int TERMINATED = 2;

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
    public static Value caml_thread_initialize(final CodeRunner ctxt, final Value unit) {
        final Context context = ctxt.getContext();
        final Object id = context.getSlot(Threads.SLOT_ID);
        if (id == null) {
            context.createRuntimeLock();
            context.registerSlot(Threads.SLOT_ID, new AtomicInteger(1));
            final CadmiumThread ct = (CadmiumThread) Thread.currentThread();
            final CodeRunner cr = ct.getRunner();
            final Block descr = Block.createBlock(3, 0);
            descr.set(Threads.IDENT, Value.ZERO);
            descr.set(Threads.START_CLOSURE, Value.UNIT);
            descr.set(Threads.TERMINATED, ThreadStatus.createValue(new ThreadStatus(cr, ct)));
            cr.setThreadStatus(Value.createFromBlock(descr));
        }
        return Value.UNIT;
    }

    /**
     * Creates a new thread.
     * @param ctxt context
     * @param clos closure to execute in created thread
     * @return the thread descriptor of the newly created thread
     */
    @Primitive
    public static Value caml_thread_new(final CodeRunner ctxt, final Value clos) {
        final Context context = ctxt.getContext();
        final int id = ((AtomicInteger) context.getSlot(Threads.SLOT_ID)).getAndIncrement();
        final Block descr = Block.createBlock(3, 0);
        final Value descrValue = Value.createFromBlock(descr);
        final CodeRunner cr = ctxt.createNewThread(descrValue);
        cr.setup(clos, Value.UNIT);
        final ThreadStatus ts = new ThreadStatus(cr, null);
        descr.set(Threads.IDENT, Value.createFromLong(id));
        descr.set(Threads.START_CLOSURE, clos);
        descr.set(Threads.TERMINATED, ThreadStatus.createValue(ts));
        ts.getThread().start();
        return Value.createFromBlock(descr);
    }

    /**
     * Returns the thread descriptor of the currently running thread.
     * @param ctxt context
     * @param unit ignored
     * @return the thread descriptor of the currently running thread
     * @throws Fail.Exception if threads are not initialized
     */
    @Primitive
    public static Value caml_thread_self(final CodeRunner ctxt, final Value unit) throws Fail.Exception {
        if (ctxt.getContext().getSlot(Threads.SLOT_ID) == null) {
            Fail.invalidArgument("Thread.self: not initialized");
            return Value.UNIT;
        } else {
            return ((CadmiumThread) Thread.currentThread()).getRunner().getThreadStatus();
        }
    }

    /**
     * Returns the identifier from a thread descriptor.
     * @param ctxt context
     * @param th thread descriptor
     * @return identifier of thread
     */
    @Primitive
    public static Value caml_thread_id(final CodeRunner ctxt, final Value th) {
        return th.asBlock().get(Threads.IDENT);
    }

    /**
     * Prints the uncaught exception of a thread.
     * @param ctxt context
     * @param exn uncaucght exception
     * @return <i>unit</i>
     */
    @Primitive
    public static Value caml_thread_uncaught_exception(final CodeRunner ctxt, final Value exn) throws FalseExit, Fail.Exception {
        final CodeRunner cr = ((CadmiumThread) Thread.currentThread()).getRunner();
        final Channel err = ctxt.getContext().getChannel(Channel.STDERR);
        if ((err != null) && (err.asOutputStream() != null)) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Thread ");
            sb.append(cr.getThreadStatus().asBlock().get(Threads.IDENT).asLong());
            sb.append(" killed on uncaught exception ");
            sb.append(fr.x9c.cadmium.kernel.Misc.convertException(exn));
            sb.append('\n');
            try {
                err.asOutputStream().write(Misc.convertStringToBytes(sb.toString()));
            } catch (final InterruptedIOException iioe) {
                final FalseExit fe = FalseExit.createFromContext(ctxt.getContext());
                fe.fillInStackTrace();
                throw fe;
            } catch (final IOException ioe) {
            }
            if (ctxt.getContext().isBacktraceActive()) {
                ctxt.printExceptionBacktrace(new PrintStream(err.asOutputStream(), true));
            }
        }
        return Value.UNIT;
    }

    /**
     * Stops the execution of the current thread.
     * @param ctxt context
     * @param unit ignored
     * @return <i>unit</i>
     * @throws Fail.Exception if threads are not initialized
     */
    @Primitive
    public static Value caml_thread_exit(final CodeRunner ctxt, final Value unit) throws Fail.Exception {
        final Context context = ctxt.getContext();
        if (context.getSlot(Threads.SLOT_ID) == null) {
            Fail.invalidArgument("Thread.exit: not initialized");
        } else {
            final CodeRunner cr = ((CadmiumThread) Thread.currentThread()).getRunner();
            ((ThreadStatus) cr.getThreadStatus().asBlock().get(Threads.TERMINATED).asBlock().asCustom()).terminate();
            context.enterBlockingSection();
        }
        return Value.UNIT;
    }

    /**
     * Pauses the current thread and runs another one.
     * @param ctxt context
     * @param th ignored
     * @return <i>unit</i>
     * @throws FalseExit if another threads has exited the program
     * @throws Fail.Exception if an asynchronous exception should be thrown
     */
    @Primitive
    public static Value caml_thread_yield(final CodeRunner ctxt, final Value th) throws FalseExit, Fail.Exception {
        final Context context = ctxt.getContext();
        context.enterBlockingSection();
        Thread.yield();
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
    public static Value caml_thread_join(final CodeRunner ctxt, final Value th) throws FalseExit, Fail.Exception {
        final Context context = ctxt.getContext();
        context.enterBlockingSection();
        ((ThreadStatus) th.asBlock().get(Threads.TERMINATED).asBlock().asCustom()).waitTerm(context);
        context.leaveBlockingSection();
        return Value.UNIT;
    }

    /**
     * Creates a mutex, wrapped as a custom value.
     * @param ctxt context
     * @param unit ignored
     * @return a new mutex, wrapped as a custom value
     */
    @Primitive
    public static Value caml_mutex_new(final CodeRunner ctxt, final Value unit) {
        final Block b = Block.createCustom(CustomMutex.SIZE, CustomMutex.OPS);
        b.setCustom(new Mutex());
        return Value.createFromBlock(b);
    }

    /**
     * Acquires lock of a mutex.
     * @param ctxt context
     * @param wrapper mutex, as a custom value
     * @return <i>unit</i>
     */
    @Primitive
    public static Value caml_mutex_lock(final CodeRunner ctxt, final Value wrapper) throws FalseExit, Fail.Exception {
        final Context context = ctxt.getContext();
        final Mutex mut = (Mutex) wrapper.asBlock().asCustom();
        if (mut.tryLock()) {
            return Value.UNIT;
        }
        context.enterBlockingSection();
        mut.lock(context);
        context.leaveBlockingSection();
        return Value.UNIT;
    }

    /**
     * Releases lock of a mutex.
     * @param ctxt context
     * @param wrapper mutex, as, a custom value
     * @return <i>unit</i>
     * @throws Fail.Exception if mutex is invalid
     */
    @Primitive
    public static Value caml_mutex_unlock(final CodeRunner ctxt, final Value wrapper) throws Fail.Exception, FalseExit {
        final Context context = ctxt.getContext();
        try {
            ((Mutex) wrapper.asBlock().asCustom()).unlock();
        } catch (final IllegalMonitorStateException imse) {
            Fail.raiseSysError("Mutex.unlock:" + imse.toString());
        }
        return Value.UNIT;
    }

    /**
     * Acquires lock of a mutex, if it is not held by another thread.
     * @param ctxt context
     * @param wrapper mutex, as a custom value
     * @return <i>true</i> if the lock was acquired,
     *         <i>false</i> otherwise
     */
    @Primitive
    public static Value caml_mutex_try_lock(final CodeRunner ctxt, final Value wrapper) {
        return ((Mutex) wrapper.asBlock().asCustom()).tryLock() ? Value.TRUE : Value.FALSE;
    }

    /**
     * Creates a condition, wrapped as a custom value.
     * @param ctxt context
     * @param unit ignored
     * @return a new condition, wrapped as a custom value
     */
    @Primitive
    public static Value caml_condition_new(final CodeRunner ctxt, final Value unit) {
        final Block b = Block.createCustom(CustomCondition.SIZE, CustomCondition.OPS);
        b.setCustom(new Object());
        return Value.createFromBlock(b);
    }

    /**
     * Waits for a condition value.
     * @param ctxt context
     * @param wcond condition to wait for
     * @param wmut mutex to acquire
     * @return <i>unit</i>
     * @throws Fail.Exception if condition is invalid
     */
    @Primitive
    public static Value caml_condition_wait(final CodeRunner ctxt, final Value wcond, final Value wmut) throws Fail.Exception, FalseExit {
        final Context context = ctxt.getContext();
        final Object cond = wcond.asBlock().asCustom();
        try {
            ((Mutex) wmut.asBlock().asCustom()).unlock();
        } catch (final IllegalMonitorStateException imse) {
            Fail.raiseSysError("Condition.wait: " + imse.toString());
        }
        context.enterBlockingSection();
        synchronized (cond) {
            try {
                cond.wait();
            } catch (final InterruptedException ie) {
                final FalseExit fe = FalseExit.createFromContext(context);
                fe.fillInStackTrace();
                throw fe;
            }
        }
        context.leaveBlockingSection();
        return Value.UNIT;
    }

    /**
     * Wakes up one thread waiting on a condition.
     * @param ctxt context
     * @param wrapper condition, wrapped as a custom value
     * @return <i>unit</i>
     */
    @Primitive
    public static Value caml_condition_signal(final CodeRunner ctxt, final Value wrapper) throws FalseExit, Fail.Exception {
        final Context context = ctxt.getContext();
        final Object cond = wrapper.asBlock().asCustom();
        synchronized (cond) {
            cond.notify();
        }
        return Value.UNIT;
    }

    /**
     * Wakes up all threads waiting on a condition.
     * @param ctxt context
     * @param wrapper condition, wrapped as a custom value
     * @return <i>unit</i>
     */
    @Primitive
    public static Value caml_condition_broadcast(final CodeRunner ctxt, final Value wrapper) throws FalseExit, Fail.Exception {
        final Context context = ctxt.getContext();
        final Object cond = wrapper.asBlock().asCustom();
        synchronized (cond) {
            cond.notifyAll();
        }
        return Value.UNIT;
    }

    /**
     * Sets the signal mask.
     * @param ctxt context
     * @param cmd what to do with passed signals :
     *            <ul>
     *              <li><tt>0</tt>: waits for passed signals</li>
     *              <li><tt>1</tt>: waits for union of current and passed
     *                              signals</li>
     *              <li><tt>2</tt>: waits for intersection of current and
     *                              complement of passed signals</li>
     *            </ul>
     * @param sigs signals
     * @return the old list of signals waited for
     */
    @Primitive
    public static Value caml_thread_sigmask(final CodeRunner ctxt, final Value cmd, final Value sigs) {
        return Signals.encodeSignalSet(ctxt.getContext().blockSignals(cmd.asLong(), Signals.decodeSignalSet(sigs)));
    }

    /**
     * Waits for a signal.
     * @param ctxt context
     * @param sigs signals to wait for
     * @return the first captured signal
     */
    @Primitive
    public static Value caml_wait_signal(final CodeRunner ctxt, final Value sigs) throws FalseExit, Fail.Exception {
        final Context context = ctxt.getContext();
        context.enterBlockingSection();
        final int res = ctxt.getContext().waitSignal(Signals.decodeSignalSet(sigs));
        context.leaveBlockingSection();
        return Value.createFromLong(Signals.systemToOCamlIdentifier(res));
    }
}
