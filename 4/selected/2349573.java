package fr.x9c.cadmium.primitives.unix;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import fr.x9c.cadmium.kernel.Block;
import fr.x9c.cadmium.kernel.CadmiumException;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Context;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.FalseExit;
import fr.x9c.cadmium.kernel.Fatal;
import fr.x9c.cadmium.kernel.Misc;
import fr.x9c.cadmium.kernel.Signals;
import fr.x9c.cadmium.kernel.Value;
import fr.x9c.cadmium.util.Signal;

/**
 * This class provides some utility methods and classes for Unix primitives.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
final class Unix {

    /** Exception message for invalid descriptor. */
    static final String INVALID_DESCRIPTOR_MSG = "invalid descriptor";

    /** Exception message for unsupported socket option. */
    static final String UNSUPPORTED_SOCKOPT_MSG = "unsupported socket option";

    /** Exception message for closed directory listing. */
    static final String DIRLISTING_CLOSED_MSG = "directory listing is closed";

    /** Number of milliseconds per second. */
    static final double MILLISECS_PER_SEC = 1000.0;

    /**
     * No instance of this class.
     */
    private Unix() {
    }

    /**
     * Raises a <i>Unix.Unix_error</i> exception if the corresponding
     * callback has been registered, an <i>Invalid_argument</i> exception
     * otherwise.
     * @param ctxt context - should not be <tt>null</tt>
     * @param prim primitive name - should not be <tt>null</tt>
     * @param e java exception - should not be <tt>null</tt>
     * @throws Fail.Exception always
     */
    static void fail(final CodeRunner ctxt, final String prim, final Exception e) throws Fail.Exception {
        assert ctxt != null : "null ctxt";
        assert prim != null : "null prim";
        assert e != null : "null e";
        final String msg = e.getMessage();
        fail(ctxt, prim, msg != null ? msg : "");
    }

    /**
     * Raises a <i>Unix.Unix_error</i> exception if the corresponding
     * callback has been registered, an <i>Invalid_argument</i> exception
     * otherwise.
     * @param ctxt context - should not be <tt>null</tt>
     * @param prim primitive name - should not be <tt>null</tt>
     * @param msg message - should not be <tt>null</tt>
     * @throws Fail.Exception always
     */
    static void fail(final CodeRunner ctxt, final String prim, final String msg) throws Fail.Exception {
        assert ctxt != null : "null ctxt";
        assert prim != null : "null prim";
        assert msg != null : "null msg";
        final Value exn = ctxt.getContext().getCallback("Unix.Unix_error");
        if (exn == null) {
            Fail.invalidArgument("Exception Unix.Unix_error not initialized, please link unix.cma");
        } else {
            final Block res = Block.createBlock(0, exn, Value.ZERO, Value.createFromBlock(Block.createString(prim)), Value.createFromBlock(Block.createString(msg)));
            Fail.raise(Value.createFromBlock(res));
        }
    }

    /**
     * Constructs an address from an internet address.
     * @param ctxt context - should not be <tt>null</tt>
     * @param addr internet address
     * @return address, as a value of <i>inet_addr</i> type
     * @throws Fail.Exception if addr is <tt>null</tt>
     */
    static Value createInetAddr(final CodeRunner ctxt, final InetAddress addr) throws Fail.Exception {
        assert ctxt != null : "null ctxt";
        if (addr != null) {
            return Value.createFromBlock(Block.createString(addr.getAddress()));
        } else {
            fail(ctxt, "", "unable to get address");
            return Value.UNIT;
        }
    }

    /**
     * Constructs an address from a socket address.
     * @param ctxt context - should not be <tt>null</tt>
     * @param addr socket address
     * @return address, as a value of <i>sockaddr</i> type
     * @throws Fail.Exception if addr is <tt>null</tt>
     */
    static Value createSockAddr(final CodeRunner ctxt, final InetSocketAddress addr) throws Fail.Exception {
        assert ctxt != null : "null ctxt";
        if (addr != null) {
            final Block res = Block.createBlock(1, createInetAddr(ctxt, addr.getAddress()), Value.createFromLong(addr.getPort()));
            return Value.createFromBlock(res);
        } else {
            fail(ctxt, "", "unable to get socket address");
            return Value.UNIT;
        }
    }

    /**
     * Emulates a primitive by running an executable. <br/>
     * Never returns if launched executable wait for any input.
     * @param ctxt context - should not be <tt>null</tt>
     * @param prim primitive to be emulated - should not be <tt>null</tt>
     * @param cmd executable to run - should not be <tt>null</tt>
     * @param args executable arguments
     * @return executable output, <tt>null</tt> if an error occurs
     * @throws Fail.Exception if emulation is disabled
     */
    static String emul(final CodeRunner ctxt, final String prim, final String cmd, final String... args) throws Fail.Exception, FalseExit {
        assert ctxt != null : "null ctxt";
        assert prim != null : "null prim";
        assert cmd != null : "null cmd";
        assert args != null : "null args";
        final Context context = ctxt.getContext();
        if (!context.getParameters().isUnixEmulated()) {
            Fail.invalidArgument("Unix." + prim + " not implemented");
            return null;
        }
        final int len = args.length;
        final String[] commandLine = new String[len + 1];
        commandLine[0] = lookupPath(cmd);
        System.arraycopy(args, 0, commandLine, 1, len);
        final ProcessBuilder pb = new ProcessBuilder(commandLine);
        pb.directory(context.getPwd());
        pb.redirectErrorStream(true);
        try {
            final Process p = pb.start();
            final InputStream out = p.getInputStream();
            final int exitCode = p.waitFor();
            if (exitCode == 0) {
                final StringBuilder sb = new StringBuilder();
                int x = out.read();
                while (x != -1) {
                    sb.append((char) x);
                    x = out.read();
                }
                return sb.toString();
            } else {
                return null;
            }
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(context);
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            return null;
        } catch (final InterruptedException ie) {
            final FalseExit fe = FalseExit.createFromContext(context);
            fe.fillInStackTrace();
            throw fe;
        }
    }

    /**
     * Looks up for a file in path.
     * @param path file path - should not be <tt>null</tt>
     * @return absolute path of file if found in path,
     *         otherwise just returns the parameter
     */
    static String lookupPath(final String path) {
        final String sysPath = System.getenv("PATH");
        if ((path.indexOf(File.separatorChar) == -1) && (sysPath != null)) {
            final String[] pathElements = sysPath.split(File.pathSeparator);
            for (String pathElem : pathElements) {
                final File file = new File(pathElem, path);
                if (file.exists() && file.isFile()) {
                    return file.getAbsolutePath();
                }
            }
        }
        return path;
    }

    /**
     * This class implements a directory listing that can be iterated
     * over and reset.
     */
    static final class DirList {

        /** Index of next element to return. */
        private int nextIndex;

        /** Elements to iterate over. */
        private final String[] elems;

        /** Whether directory listing is still active. */
        private boolean open;

        /** Context, needed for epceptions. */
        private final CodeRunner context;

        /**
         * Constructs a directory listing from a path.
         * @param ctxt context - should not be <tt>null</tt>
         * @param f path of directory to list - should not be <tt>null</tt>
         * @throws Fail.Exception if an error occurs while trying to
         *                        retrieve directory content
         */
        DirList(final CodeRunner ctxt, final File f) throws Fail.Exception {
            assert ctxt != null : "null ctxt";
            assert f != null : "null f";
            this.nextIndex = 0;
            final String[] tmp = f.list();
            if (tmp != null) {
                final int len = tmp.length;
                this.elems = new String[len + 2];
                this.elems[0] = ".";
                this.elems[1] = "..";
                System.arraycopy(tmp, 0, this.elems, 2, len);
                this.open = true;
                this.context = ctxt;
            } else {
                fail(ctxt, "opendir", "unable to get directory content");
                this.elems = null;
                this.open = false;
                this.context = null;
            }
        }

        /**
         * Returns the next element of the directory listing.
         * @return the next element of the directory listing
         * @throws Fail.Exception if there is no element to return
         * @throws Fail.Exception if directory listing is closed
         */
        String next() throws Fail.Exception {
            if (this.open) {
                if (this.nextIndex < this.elems.length) {
                    return this.elems[this.nextIndex++];
                } else {
                    Fail.raiseEndOfFile();
                    return null;
                }
            } else {
                fail(this.context, "readdir", Unix.DIRLISTING_CLOSED_MSG);
                return null;
            }
        }

        /**
         * Resets the directory listing. <br/>
         * The next element to be returned will be the first element.
         * @throws Fail.Exception if directory listing is closed
         */
        void rewind() throws Fail.Exception {
            if (this.open) {
                this.nextIndex = 0;
            } else {
                fail(this.context, "rewinddir", Unix.DIRLISTING_CLOSED_MSG);
            }
        }

        /**
         * Closes the directory listing.
         */
        void close() {
            this.open = false;
        }
    }

    /**
     * This class implements a thread that waits for given time
     * and then raise a signal if thread has not been unactivated.
     */
    static final class AlarmThread extends Thread {

        /** Prefix for thread name. */
        private static final String PREFIX = "Cadmium-AlarmThread-";

        /** Next Identifier for name. */
        private static final AtomicInteger NEXT_ID = new AtomicInteger();

        /** Underlying code runner. */
        private final CodeRunner codeRunner;

        /** Underlying context. */
        private final Context context;

        /** Time before alarm. */
        private long millis;

        /** Interval between two alarms. */
        private final long interval;

        /** Signal to raise. */
        private final Signal.Kind signal;

        /** Whether the thread is activated. */
        private boolean activated;

        /** Starting time of thread. */
        private long start;

        /**
         * Constructs an starts an alarm thread.
         * @param runner underlying code runner - should not be <tt>null</tt>
         * @param val time before alarm in milliseconds - should be > 0
         * @param itv time between two alarms - should be >= 0
         * @param sgn signal to raise - should not be <tt>null</tt>
         */
        AlarmThread(final CodeRunner runner, final long val, final long itv, final Signal.Kind sgn) {
            super(runner.getContext().getThreadGroup(), getNextName());
            assert runner != null : "null runner";
            assert val > 0 : "val should be > 0";
            assert itv >= 0 : "itv should be >= 0";
            assert sgn != null : "null sgn";
            this.codeRunner = runner;
            this.context = runner.getContext();
            this.millis = val;
            this.interval = itv;
            this.signal = sgn;
            this.activated = true;
            start();
        }

        /**
         * Returns the interval between two alarms.
         * @return the interval between two alarms
         */
        long getInterval() {
            return this.interval;
        }

        /**
         * Returns the time remaining before alarm.
         * @return the time remaining before alarm
         */
        long getValue() {
            return this.millis - (System.currentTimeMillis() - this.start);
        }

        /**
         * Unactivates the alarm.
         * @return the time remaining before alarm
         */
        long unactivate() {
            this.activated = false;
            return this.millis - (System.currentTimeMillis() - this.start);
        }

        /**
         * Waits for given time and then raise alarm signal if thread has
         * not been unactivated. Successive raises happen if interval is
         * non-zero.
         */
        @Override
        public void run() {
            do {
                this.start = System.currentTimeMillis();
                try {
                    Thread.sleep(this.millis);
                } catch (final InterruptedException ie) {
                    return;
                }
                if (this.activated) {
                    Signals.enqueueSignal(new Signal(this.signal));
                    try {
                        Signals.processSignal(this.codeRunner);
                    } catch (final FalseExit fe) {
                        this.context.setAsyncException(fe);
                    } catch (final Fail.Exception fe) {
                        final Channel ch = this.context.getChannel(Channel.STDERR);
                        if ((ch != null) && (ch.asOutputStream() != null)) {
                            final String msg = Misc.convertException(fe.asValue(this.context.getGlobalData()));
                            final PrintStream err = new PrintStream(ch.asOutputStream(), true);
                            err.println("Error in signal handler: exception " + msg);
                            err.close();
                        }
                    } catch (final Fatal.Exception fe) {
                        final Channel ch = this.context.getChannel(Channel.STDERR);
                        if ((ch != null) && (ch.asOutputStream() != null)) {
                            final PrintStream err = new PrintStream(ch.asOutputStream(), true);
                            err.println("Error in signal handler: exception " + fe.getMessage());
                            err.close();
                        }
                    } catch (final CadmiumException ie) {
                        final Channel ch = this.context.getChannel(Channel.STDERR);
                        if ((ch != null) && (ch.asOutputStream() != null)) {
                            final PrintStream err = new PrintStream(ch.asOutputStream(), true);
                            err.println("Error in signal handler: exception " + ie.getMessage());
                            err.close();
                        }
                    }
                }
                this.millis = this.interval;
            } while (this.millis > 0);
        }

        /**
         * Returns the next thread name. <br/>
         * Threads names are of the following form:
         * <tt>Cadmium-AlarmThread-<i>id</i></tt> where <i>id</i> is a
         * unique integer identifier that is incremented each time a new
         * thread is created.
         * @return the next thread name
         */
        private static String getNextName() {
            final StringBuilder sb = new StringBuilder(PREFIX);
            sb.append(NEXT_ID.getAndIncrement());
            return sb.toString();
        }
    }
}
