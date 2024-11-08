package fr.x9c.cadmium.kernel;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import fr.x9c.cadmium.util.Signal;

/**
 * This class implements signal handling, that is common to all Cadmium runners
 * in the Java Virtual Machine. <br/>
 * Signal identifiers exist in two kinds:
 * <ul>
 *   <li>OCaml representation: as defined in <i>Sys</i> module;</li>
 *   <li>system representation: as defined in <i>BSD</i> standard.</li>
 * </ul>
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
public final class Signals {

    /** Map from OCaml signal identifier to system signal identifier. */
    private static final int[] SIGNALS_ID = { Signal.Kind.ABRT.getNo(), Signal.Kind.ALRM.getNo(), Signal.Kind.FPE.getNo(), Signal.Kind.HUP.getNo(), Signal.Kind.ILL.getNo(), Signal.Kind.INT.getNo(), Signal.Kind.KILL.getNo(), Signal.Kind.PIPE.getNo(), Signal.Kind.QUIT.getNo(), Signal.Kind.SEGV.getNo(), Signal.Kind.TERM.getNo(), Signal.Kind.USR1.getNo(), Signal.Kind.USR2.getNo(), Signal.Kind.CHLD.getNo(), Signal.Kind.CONT.getNo(), Signal.Kind.STOP.getNo(), Signal.Kind.TSTP.getNo(), Signal.Kind.TTIN.getNo(), Signal.Kind.TTOU.getNo(), Signal.Kind.VTALRM.getNo(), Signal.Kind.PROF.getNo() };

    /** Handler (shared by all signals). */
    private static final Signal.Handler SIGNAL_HANDLER = new Signal.Handler() {

        /**
             * Handles the signal by enqueuing it in the list of pending ones.
             * @param signal signal to handle - should not be <tt>null</tt>
             */
        public void handle(final Signal signal) {
            assert signal != null : "null signal";
            enqueueSignal(signal);
        }
    };

    /**
     * List of pending signals, in system representation, with oldest signal
     * at head.
     */
    private static List<Integer> pendingSignals = new LinkedList<Integer>();

    /** Maximum size/length of the list of pending signals. */
    private static final int PENDING_SIGNALS_MAX_LENGTH = 64;

    /**
     * Map from signals to set of Cadmium runners interested in the signal,
     * using system representation.
     */
    private static final Map<Integer, Set<Context>> INTERESTED = new HashMap<Integer, Set<Context>>();

    /**
     * Map from signals identifier (in system representation)
     * to signal instance.
     */
    private static final Map<Integer, Signal> SIGNALS = new HashMap<Integer, Signal>();

    /** Creation of signal map. */
    static {
        for (Signal.Kind k : Signal.Kind.values()) {
            final Signal s = new Signal(k);
            if (s.isValid()) {
                final Signal prev = Signals.SIGNALS.put(s.getNumber(), s);
                assert prev == null : "two signals with same integer id";
            }
        }
    }

    /**
     * No instance of this class.
     */
    private Signals() {
    }

    /**
     * Converts a signal identifier from OCaml to system representation.
     * @param no OCaml signal identifier
     * @return system signal identifier
     */
    public static int ocamlToSystemIdentifier(final int no) {
        if ((no < 0) && (no >= -Signals.SIGNALS_ID.length)) {
            return Signals.SIGNALS_ID[-no - 1];
        } else {
            return no;
        }
    }

    /**
     * Converts a signal identifier from system to OCaml representation.
     * @param no system signal identifier
     * @return OCaml signal identifier
     */
    public static int systemToOCamlIdentifier(final int no) {
        final int len = Signals.SIGNALS_ID.length;
        int i = 0;
        while ((i < len) && (no != Signals.SIGNALS_ID[i])) {
            i++;
        }
        if (i < len) {
            return -i - 1;
        } else {
            return no;
        }
    }

    /**
     * Decodes a set of signals from OCaml representation to system
     * representaion.
     * @param s set of signals to decode
     *          (as a list of OCaml signal identifiers)
     *          - should not be <tt>null</tt>
     * @return set of system signal identifiers
     */
    public static Set<Integer> decodeSignalSet(final Value s) {
        assert s != null : "null s";
        final Set<Integer> res = new HashSet<Integer>();
        Value l = s;
        while (l != Value.EMPTY_LIST) {
            final Block b = l.asBlock();
            res.add(ocamlToSystemIdentifier(b.get(0).asLong()));
            l = b.get(1);
        }
        return res;
    }

    /**
     * Encodes a set of signals from system representation to an OCaml list.
     * @param s set of system signal identifiers - should not be <tt>null</tt>
     * @return corresponding OCaml list
     */
    public static Value encodeSignalSet(final Set<Integer> s) {
        assert s != null : "null s";
        Value res = Value.EMPTY_LIST;
        for (int i : s) {
            final Block cons = Block.createBlock(Block.TAG_CONS, Value.createFromLong(systemToOCamlIdentifier(i)), res);
            res = Value.createFromBlock(cons);
        }
        return res;
    }

    /**
     * Registers a context for interest in a signal.
     * @param sig signal to register interest in
     * @param c context - should not be <tt>null</tt>
     */
    public static synchronized void registerContext(final int sig, final Context c) {
        assert c != null : "null c";
        final Integer s = Integer.valueOf(sig);
        Set<Context> set = Signals.INTERESTED.get(s);
        if (set == null) {
            set = new HashSet<Context>();
            set.add(c);
            Signals.INTERESTED.put(s, set);
            Signal.handle(Signals.SIGNAL_HANDLER, Signals.SIGNALS.get(s));
        } else {
            if (set.isEmpty()) {
                Signal.handle(Signals.SIGNAL_HANDLER, Signals.SIGNALS.get(s));
            }
            set.add(c);
        }
    }

    /**
     * Unregisters a context from interest in a signal.
     * @param sig signal to unregister interest in
     * @param c context - should not be <tt>null</tt>
     */
    public static synchronized void unregisterContext(final int sig, final Context c) {
        assert c != null : "null c";
        final Integer s = Integer.valueOf(sig);
        final Set<Context> set = Signals.INTERESTED.get(s);
        if (set != null) {
            set.remove(c);
            if (set.isEmpty()) {
                Signal.handle(Signal.DEFAULT_HANDLER, Signals.SIGNALS.get(s));
            }
        }
    }

    /**
     * Unregisters a context from interest in all signals.
     * @param c context - should not be <tt>null</tt>
     */
    public static synchronized void unregisterContext(final Context c) {
        assert c != null : "null c";
        for (Map.Entry<Integer, Set<Context>> e : Signals.INTERESTED.entrySet()) {
            final Integer s = e.getKey();
            final Set<Context> set = e.getValue();
            set.remove(c);
            if (set.isEmpty()) {
                Signal.handle(Signal.DEFAULT_HANDLER, Signals.SIGNALS.get(s));
            }
        }
    }

    /**
     * Enqueues a signal to the list of pending signals. <br/>
     * The signal is enqueued if and only if it is supported by the system.<br/>
     * The list of pending signals has a maximum size, oldest signals being
     * discarded to keep the size of the list below this maximum. <br/>
     * If a context with no thread currently executing (<i>e.g.</i> being all
     * waiting for completion of an i/o operation) such that it is interested
     * in the passed signal, then the signal is not enqueued but immediatly
     * executed as a callback to the main thread of this context.
     *
     * @param signal signal to enqueue - should not be <tt>null</tt>
     */
    public static synchronized void enqueueSignal(final Signal signal) {
        assert signal != null : "null signal";
        if (signal.isValid()) {
            final int signum = signal.getNumber();
            final Set<Context> interested = Signals.INTERESTED.get(signum);
            if (interested != null) {
                for (Context ctxt : interested) {
                    if (!ctxt.isRuntimeBusy()) {
                        final Value closure = ctxt.getSignalHandler(signum);
                        final CodeRunner runner = ctxt.getMainCodeRunner();
                        if ((closure != null) && closure.isBlock() && (runner != null)) {
                            try {
                                runner.callback(closure, Value.createFromLong(Signals.systemToOCamlIdentifier(signum)));
                            } catch (final FalseExit fe) {
                                ctxt.setAsyncException(fe);
                                ctxt.getMainThread().interrupt();
                            } catch (final Fail.Exception fe) {
                                ctxt.setAsyncException(fe);
                                ctxt.getMainThread().interrupt();
                            } catch (final Fatal.Exception fe) {
                                final Channel ch = ctxt.getChannel(Channel.STDERR);
                                if ((ch != null) && (ch.asOutputStream() != null)) {
                                    final String msg = fe.getMessage();
                                    final PrintStream err = new PrintStream(ch.asOutputStream(), true);
                                    err.println("Error in signal handler: exception " + msg);
                                    err.close();
                                }
                            } catch (final CadmiumException ie) {
                                final Channel ch = ctxt.getChannel(Channel.STDERR);
                                if ((ch != null) && (ch.asOutputStream() != null)) {
                                    final String msg = ie.getMessage();
                                    final PrintStream err = new PrintStream(ch.asOutputStream(), true);
                                    err.println("Error in signal handler: exception " + msg);
                                    err.close();
                                }
                            }
                            return;
                        }
                    }
                }
            }
            Signals.pendingSignals.add(signal.getNumber());
            while (Signals.pendingSignals.size() > Signals.PENDING_SIGNALS_MAX_LENGTH) {
                Signals.pendingSignals.remove(0);
            }
            synchronized (Signals.pendingSignals) {
                Signals.pendingSignals.notifyAll();
            }
        }
    }

    /**
     * Returns a list of pending signals, from a given set. <br/>
     * Signals use system representation.
     * @param m set of signal that can be returned as pending
     *          - should not be <tt>null</tt>
     * @return the list of pending signals intersected with the passed set
     */
    static synchronized Set<Integer> getPendingSignals(final Set<Integer> m) {
        assert m != null : "null m";
        final Set<Integer> res = new HashSet<Integer>();
        for (Integer s : Signals.pendingSignals) {
            if (m.contains(s)) {
                res.add(s);
            }
        }
        return res;
    }

    /**
     * Process a signal if any has been both caught and an handler has been
     * registered with.
     * @param runner code runner - should not be <tt>null</tt>
     * @return <tt>true</tt> if a signal was actually processed,
     *         <tt>false</tt> otherwise
     * @throws Fail.Exception if thrown by signal handler
     * @throws Fatal.Exception if thrown by signal handler
     */
    public static synchronized boolean processSignal(final CodeRunner runner) throws Fail.Exception, Fatal.Exception, FalseExit, CadmiumException {
        assert runner != null : "null runner";
        final Context ctxt = runner.getContext();
        final Integer signal = Signals.getPendingSignal(ctxt);
        if (signal != null) {
            final Value closure = ctxt.getSignalHandler(signal);
            if ((closure != null) && closure.isBlock()) {
                runner.callback(closure, Value.createFromLong(Signals.systemToOCamlIdentifier(signal)));
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Returns the oldest pending signal according to a given context. <br/>
     * The returned signal is the oldest such as it is neither ignored
     * nor blocked, and there is an handler associated with it.
     * @param ctxt context - should not be <tt>null</tt>
     * @return the oldest pending signal according to a given context,
     *         <tt>null</tt> if no such signal exists
     */
    private static synchronized Integer getPendingSignal(final Context ctxt) {
        assert ctxt != null : "null ctxt";
        final Set<Integer> blocked = ctxt.getBlockedSignals();
        final Set<Integer> ignored = ctxt.getIgnoredSignals();
        final Iterator<Integer> it = Signals.pendingSignals.iterator();
        while (it.hasNext()) {
            final Integer s = it.next();
            if (!blocked.contains(s) && !ignored.contains(s) && ctxt.getSignalHandler(s).isBlock()) {
                it.remove();
                return s;
            }
        }
        return null;
    }

    /**
     * Waits until a signal is caught.
     * @param ctxt context - should not be <tt>null</tt>
     * @param blocked signals to be blocked while waiting
     *                - should not be <tt>null</tt>
     * @return first caught signal, in system representation
     */
    public static synchronized int waitForSignal(final Context ctxt, final Set<Integer> blocked) throws FalseExit, Fail.Exception {
        assert ctxt != null : "null ctxt";
        assert blocked != null : "null blocked";
        final Set<Integer> added = new LinkedHashSet<Integer>();
        for (Integer s : Signals.SIGNALS.keySet()) {
            if (blocked.contains(s) && (!Signals.INTERESTED.containsKey(s) || !Signals.INTERESTED.get(s).contains(ctxt))) {
                added.add(s);
                registerContext(s, ctxt);
            }
        }
        while (true) {
            try {
                synchronized (Signals.pendingSignals) {
                    Signals.pendingSignals.wait();
                }
                final int size = Signals.pendingSignals.size();
                if (size > 0) {
                    final Integer last = Signals.pendingSignals.get(size - 1);
                    if (!blocked.contains(last)) {
                        for (int s : added) {
                            unregisterContext(s, ctxt);
                        }
                        return last;
                    }
                }
            } catch (final InterruptedException ie) {
                for (int s : added) {
                    unregisterContext(s, ctxt);
                }
                final FalseExit fe = FalseExit.createFromContext(ctxt);
                fe.fillInStackTrace();
                throw fe;
            }
        }
    }
}
