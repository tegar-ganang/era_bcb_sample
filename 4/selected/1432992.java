package fr.x9c.cadmium.primitives.stdlib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import fr.x9c.cadmium.kernel.Block;
import fr.x9c.cadmium.kernel.ByteCodeParameters;
import fr.x9c.cadmium.kernel.ByteCodeRunner;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Context;
import fr.x9c.cadmium.kernel.Debugger;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.FalseExit;
import fr.x9c.cadmium.kernel.Fatal;
import fr.x9c.cadmium.kernel.Interpreter;
import fr.x9c.cadmium.kernel.Misc;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;
import fr.x9c.cadmium.primitives.unix.Chmod;
import fr.x9c.cadmium.util.RandomAccessInputStream;
import fr.x9c.cadmium.util.StreamCopyThread;

/**
 * Implements all primitives from 'sys.c'.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Sys {

    /** Conversion of open flags. */
    private static final int[] SYS_OPEN_FLAGS = { Channel.O_RDONLY, Channel.O_WRONLY, Channel.O_APPEND | Channel.O_WRONLY, Channel.O_CREAT, Channel.O_TRUNC, Channel.O_EXCL, Channel.O_BINARY, Channel.O_TEXT, Channel.O_NONBLOCK };

    /** Cadmium word size. */
    private static final Value WORD_SIZE = Value.createFromLong(32);

    /** Number of milliseconds per second. */
    private static final double MILLISECS_PER_SEC = 1000.0;

    /**
     * No instance of this class.
     */
    private Sys() {
    }

    /**
     * According to context, either exits the JVM or just stops the program.
     * @param ctxt context
     * @param retCode return code of interpreted program
     * @return <i>unit</i> (never returns ...)
     * @throws FalseExit if false exit is activated for this code runner
     */
    @Primitive
    public static Value caml_sys_exit(final CodeRunner ctxt, final Value retCode) throws Fail.Exception, FalseExit, Fatal.Exception {
        final Context c = ctxt.getContext();
        if (!c.isNative()) {
            Debugger.handleEvent((ByteCodeRunner) ctxt, Debugger.EventKind.PROGRAM_EXIT);
        }
        if (c.getParameters().isExitStoppingJVM()) {
            System.exit(retCode.asLong());
            return Value.UNIT;
        } else {
            c.getThreadGroup().interrupt();
            c.interruptAdditionalThreads();
            throw new FalseExit(c, retCode.asLong());
        }
    }

    /**
     * Opens a file.
     * @param ctxt context
     * @param path file to open
     * @param flags access control
     * @param perm file permissions if file is created
     * @return file descriptor
     * @throws Fail.Exception if file does not exist
     */
    @Primitive
    public static Value caml_sys_open(final CodeRunner ctxt, final Value path, final Value flags, final Value perm) throws Fail.Exception, FalseExit {
        final Context context = ctxt.getContext();
        final int flg = Misc.convertFlagList(flags, Sys.SYS_OPEN_FLAGS);
        if (context.getFileHook() != null) {
            try {
                final InputStream emb = context.getFileHook().getInputStream(context.resourceNameFromPath(path));
                if (emb != null) {
                    final InputStream res = (new RandomAccessInputStream(emb)).createInputStream();
                    if (res != null) {
                        if ((flg & (Channel.O_WRONLY | Channel.O_APPEND | Channel.O_CREAT | Channel.O_TRUNC)) != 0) {
                            sysError(path.asBlock().asString(), "unable to open embedded file in write mode");
                        }
                        return Value.createFromLong(context.addChannel(new Channel(res)));
                    }
                }
            } catch (final Exception e) {
            }
        }
        final File file = context.getRealFile(path);
        final int perms = perm.asLong();
        try {
            context.enterBlockingSection();
            final Value res = Value.createFromLong(context.addChannel(new Channel(ctxt, file, flg, perms)));
            context.leaveBlockingSection();
            return res;
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(context);
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            context.leaveBlockingSection();
            sysError(path.asBlock().asString(), ioe.toString());
            return Value.UNIT;
        }
    }

    /**
     * Closes a file.
     * @param ctxt context
     * @param fd file descriptor of file to close
     * @return <i>unit</i>
     */
    @Primitive
    public static Value caml_sys_close(final CodeRunner ctxt, final Value fd) throws FalseExit, Fail.Exception {
        try {
            ctxt.getContext().closeChannel(fd.asLong());
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(ctxt.getContext());
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
        }
        return Value.UNIT;
    }

    /**
     * Tests whether a file exists.
     * @param ctxt context
     * @param name file path
     * @return {@link fr.x9c.cadmium.kernel.Value#TRUE} if the file exists,
     *         {@link fr.x9c.cadmium.kernel.Value#FALSE} otherwise
     */
    @Primitive
    public static Value caml_sys_file_exists(final CodeRunner ctxt, final Value name) {
        final Context context = ctxt.getContext();
        if (context.getFileHook() != null) {
            try {
                final InputStream tmp = context.getInputStreamForPath(name);
                if (tmp != null) {
                    try {
                        tmp.close();
                    } catch (final Exception e) {
                    }
                    return Value.TRUE;
                }
            } catch (final Exception e) {
            }
        }
        try {
            return context.getRealFile(name).exists() ? Value.TRUE : Value.FALSE;
        } catch (final SecurityException se) {
            return Value.FALSE;
        }
    }

    /**
     * Tests whether a file path denotes a directory.
     * @param ctxt context
     * @param name file path
     * @return {@link fr.x9c.cadmium.kernel.Value#TRUE} if the file path
     *         denotes a directory,
     *         {@link fr.x9c.cadmium.kernel.Value#FALSE} otherwise
     */
    @Primitive
    public static Value caml_sys_is_directory(final CodeRunner ctxt, final Value name) {
        try {
            return ctxt.getContext().getRealFile(name).isDirectory() ? Value.TRUE : Value.FALSE;
        } catch (final SecurityException se) {
            return Value.FALSE;
        }
    }

    /**
     * Removes/deletes a file.
     * @param ctxt context
     * @param name file path
     * @return <i>unit</i>
     * @throws if file cannot be deleted
     */
    @Primitive
    public static Value caml_sys_remove(final CodeRunner ctxt, final Value name) throws Fail.Exception {
        try {
            if (!ctxt.getContext().getRealFile(name).delete()) {
                sysError(name.asBlock().asString(), "unable to delete file");
            }
            return Value.UNIT;
        } catch (final SecurityException se) {
            sysError(name.asBlock().asString(), se.toString());
            return Value.UNIT;
        }
    }

    /**
     * Renames a file.
     * @param ctxt context
     * @param oldName path of file to rename
     * @param newName new path for file
     * @return <i>unit</i>
     * @throws Fail.Exception if file cannot be renamed
     */
    @Primitive
    public static Value caml_sys_rename(final CodeRunner ctxt, final Value oldName, final Value newName) throws Fail.Exception {
        final Context context = ctxt.getContext();
        try {
            if (!context.getRealFile(oldName).renameTo(context.getRealFile(newName))) {
                sysError(null, "unable to rename file");
            }
            return Value.UNIT;
        } catch (final SecurityException se) {
            sysError(null, se.toString());
            return Value.UNIT;
        }
    }

    /**
     * Changes the current working directory.
     * @param ctxt context
     * @param path new current working directory, as a string
     * @return <i>unit</i>
     * @throws Fail.Exception if path does not exist or is not a directory
     */
    @Primitive
    public static Value caml_sys_chdir(final CodeRunner ctxt, final Value path) throws Fail.Exception {
        final Context context = ctxt.getContext();
        try {
            final File f = context.getRealFile(path);
            if (f.isDirectory()) {
                context.setPwd(f);
            } else {
                sysError(path.asBlock().asString(), "not a directory");
            }
        } catch (final SecurityException se) {
            sysError(path.asBlock().asString(), se.toString());
        }
        return Value.UNIT;
    }

    /**
     * Returns the current working directory.
     * @param ctxt context
     * @param unit ignored
     * @return the current working directory, as a string
     * @throws Fail.Exception if an i/o error occurs
     */
    @Primitive
    public static Value caml_sys_getcwd(final CodeRunner ctxt, final Value unit) throws Fail.Exception, FalseExit {
        try {
            return Value.createFromBlock(Block.createString(ctxt.getContext().getPwd().getCanonicalPath()));
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(ctxt.getContext());
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            sysError(null, ioe.toString());
        } catch (final SecurityException se) {
            sysError(null, se.toString());
        }
        return Value.UNIT;
    }

    /**
     * Returns the value of an environment variable.
     * @param ctxt context
     * @param var variable name
     * @return the value of name <tt>var</tt> in the environment, if any
     * @throws Fail.Exception <i>Not_found</i> if there is no variable named
     *         <tt>var</tt>
     */
    @Primitive
    public static Value caml_sys_getenv(final CodeRunner ctxt, final Value var) throws Fail.Exception {
        String res = null;
        try {
            res = System.getenv(var.asBlock().asString());
        } catch (final Exception e) {
        }
        if (res == null) {
            Fail.raiseNotFound();
        }
        return Value.createFromBlock(Block.createString(res));
    }

    /**
     * Returns "argv" (that is program arguments) as a couple:
     * <i>(interpreted_file, arguments_array)</i>.
     * @param ctxt context
     * @param unit ignored
     * @return <i>(interpreted_file, arguments_array)</i> <br/>
     *         <i>interpreted_file</i> being equal to
     *         {@link fr.x9c.cadmium.kernel.Context#NO_FILE} if interpreted
     *         program is not read from a file
     */
    @Primitive
    public static Value caml_sys_get_argv(final CodeRunner ctxt, final Value unit) {
        final Context c = ctxt.getContext();
        final String[] args = c.getParameters().getArguments();
        final Block b = Block.createBlock(0, Value.createFromBlock(Block.createString(c.getFile())), args.length > 0 ? Value.createFromBlock(Block.createStringArray(args)) : c.getAtom(0));
        return Value.createFromBlock(b);
    }

    /**
     * Executes an OS command. <br/>
     * Tries to execute the passed command as an embedded bytecode is embedded
     * mode is enabled.
     * @param ctxt context
     * @param cmd string value representing the command to execute
     * @return the exit code of the process associated with the command
     * @throws Fail.Exception if command execution fails
     */
    @Primitive
    public static Value caml_sys_system_command(final CodeRunner ctxt, final Value cmd) throws Fail.Exception, FalseExit {
        final Context context = ctxt.getContext();
        final String command = cmd.asBlock().asString();
        final List<String> tokens = Misc.parseCommandLine(command);
        if ((tokens.size() > 0) && (tokens.get(0).charAt(0) == '(')) {
            final StringBuilder sb = new StringBuilder();
            for (String t : tokens) {
                sb.append(t);
                sb.append(" ");
            }
            tokens.clear();
            tokens.add("sh");
            tokens.add("-c");
            tokens.add(sb.toString());
        }
        String redir = (tokens.size() >= 2) ? tokens.get(tokens.size() - 2) : "";
        String redirIn = null;
        String redirOut = null;
        String redirErr = null;
        boolean redirOutAppend = false;
        while ((redir != null) && ((redir.equals(">") || redir.equals(">>") || redir.equals("2>") || redir.equals("<")))) {
            if (redir.equals(">")) {
                redirOut = tokens.remove(tokens.size() - 1);
                redirOutAppend = false;
            } else if (redir.equals(">>")) {
                redirOut = tokens.remove(tokens.size() - 1);
                redirOutAppend = true;
            } else if (redir.equals("2>")) {
                redirErr = tokens.remove(tokens.size() - 1);
            } else if (redir.equals("<")) {
                redirIn = tokens.remove(tokens.size() - 1);
            } else {
                assert false : "invalid case";
                tokens.remove(tokens.size() - 1);
            }
            tokens.remove(tokens.size() - 1);
            redir = (tokens.size() >= 2) ? tokens.get(tokens.size() - 2) : "";
        }
        final String tmpExecutable = tokens.size() >= 1 ? tokens.get(0) : null;
        final String executable = tmpExecutable != null && !tmpExecutable.contains("/") ? "/usr/local/bin/" + tmpExecutable : null;
        if ((context.getFileHook() != null) && (executable != null)) {
            final String resource = context.resourceNameFromPath(Value.createFromBlock(Block.createString(executable)));
            final InputStream is = context.getFileHook().getInputStream(resource);
            if (is != null) {
                try {
                    final RandomAccessInputStream rais = new RandomAccessInputStream(is);
                    final List<String> tokensCopy = new LinkedList<String>(tokens);
                    final int size = tokensCopy.size();
                    final String[] args = new String[size];
                    tokensCopy.toArray(args);
                    final InputStream in;
                    if (redirIn != null) {
                        final Value f = Value.createFromBlock(Block.createString(redirIn));
                        in = new FileInputStream(context.getRealFile(f));
                    } else {
                        final Channel parentIn = context.getChannel(Channel.STDIN);
                        in = parentIn != null ? parentIn.asInputStream() : System.in;
                    }
                    final OutputStream out;
                    if (redirOut != null) {
                        final Value f = Value.createFromBlock(Block.createString(redirOut));
                        out = new FileOutputStream(context.getRealFile(f), redirOutAppend);
                    } else {
                        final Channel parentOut = context.getChannel(Channel.STDOUT);
                        out = parentOut != null ? parentOut.asOutputStream() : System.out;
                    }
                    final OutputStream err;
                    if (redirErr != null) {
                        final Value f = Value.createFromBlock(Block.createString(redirErr));
                        err = new FileOutputStream(context.getRealFile(f));
                    } else {
                        final Channel parentErr = context.getChannel(Channel.STDERR);
                        err = parentErr != null ? parentErr.asOutputStream() : System.err;
                    }
                    final ByteCodeParameters params = new ByteCodeParameters(args, false, false, in, out instanceof PrintStream ? (PrintStream) out : new PrintStream(out, true), err instanceof PrintStream ? (PrintStream) err : new PrintStream(err, true), false, false, false, false, "Unix", true, executable, false, "fr.x9c.cadmium.primitives.stdlib.Sys", false, false, false, false, 64 * 1024, 64 * 1024, new String[0], true);
                    final Interpreter interp = new Interpreter(params, context.getPwd(), rais);
                    final Value exit = interp.execute();
                    return exit.isLong() ? exit : Value.MINUS_ONE;
                } catch (final Exception e) {
                    return Value.MINUS_ONE;
                }
            }
        }
        try {
            final ProcessBuilder pb = new ProcessBuilder(tokens);
            pb.directory(context.getPwd());
            context.enterBlockingSection();
            final Process p = pb.start();
            if (redirOut != null) {
                final Value f = Value.createFromBlock(Block.createString(redirOut));
                final Thread t = new StreamCopyThread(new FileOutputStream(context.getRealFile(f), redirOutAppend), p.getInputStream());
                t.start();
            } else {
                final Channel out = context.getChannel(Channel.STDOUT);
                if (out != null) {
                    final Thread t = new StreamCopyThread(out.asOutputStream(), p.getInputStream());
                    t.start();
                }
            }
            if (redirErr != null) {
                final Value f = Value.createFromBlock(Block.createString(redirErr));
                final Thread t = new StreamCopyThread(new FileOutputStream(context.getRealFile(f)), p.getErrorStream());
                t.start();
            } else {
                final Channel err = context.getChannel(Channel.STDERR);
                if (err != null) {
                    final Thread t = new StreamCopyThread(err.asOutputStream(), p.getErrorStream());
                    t.start();
                }
            }
            if (redirIn != null) {
                final Value f = Value.createFromBlock(Block.createString(redirIn));
                final Thread t = new StreamCopyThread(p.getOutputStream(), new FileInputStream(context.getRealFile(f)));
                t.start();
            } else {
                final Channel in = context.getChannel(Channel.STDIN);
                if (in != null) {
                    final Thread t = new StreamCopyThread(p.getOutputStream(), in.asInputStream());
                    t.start();
                }
            }
            final int res = p.waitFor();
            context.leaveBlockingSection();
            return Value.createFromLong(res);
        } catch (final SecurityException se) {
            context.leaveBlockingSection();
            sysError(command, se.toString());
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(context);
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            context.leaveBlockingSection();
            sysError(command, ioe.toString());
        } catch (final IllegalArgumentException iae) {
            sysError(command, iae.toString());
        } catch (final InterruptedException ie) {
            final FalseExit fe = FalseExit.createFromContext(context);
            fe.fillInStackTrace();
            throw fe;
        }
        return Value.UNIT;
    }

    /**
     * Returns the (real) time elapsed since the start of the interpreted
     * program in seconds, as a float.
     * @param ctxt context
     * @param unit ignored
     * @return time elapsed since start of program interpretation
     */
    @Primitive
    public static Value caml_sys_time(final CodeRunner ctxt, final Value unit) {
        final double res = ((double) (System.currentTimeMillis() - ctxt.getContext().getStart())) / MILLISECS_PER_SEC;
        return Value.createFromBlock(Block.createDouble(res));
    }

    /**
     * Returns the system random seed.
     * @param ctxt context
     * @param unit ignored
     * @return <tt>System.currentTimeMillis() / 1000</tt>
     */
    @Primitive
    public static Value caml_sys_random_seed(final CodeRunner ctxt, final Value unit) {
        return Value.createFromLong((int) (System.currentTimeMillis() / ((long) MILLISECS_PER_SEC)));
    }

    /**
     * Returns the system configuration as a couple:
     * <i>(os_type, long_size_in_bits)</i>.
     * @param ctxt context
     * @param unit ignored
     * @return the system configuration
     */
    @Primitive
    public static Value caml_sys_get_config(final CodeRunner ctxt, final Value unit) {
        final Block b = Block.createBlock(0, Value.createFromBlock(Block.createString(ctxt.getContext().getParameters().getOS())), Sys.WORD_SIZE);
        return Value.createFromBlock(b);
    }

    /**
     * Returns a block/array containing all files in the given directory
     * as strings.
     * @param ctxt context
     * @param path directory to list
     * @return a string array with all the file present in the passed path
     * @throws Fail.Exception if directory content cannot be read
     */
    @Primitive
    public static Value caml_sys_read_directory(final CodeRunner ctxt, final Value path) throws Fail.Exception {
        try {
            final Context context = ctxt.getContext();
            final String[] f = context.getRealFile(path).list();
            if ((f == null) || (f.length == 0)) {
                return context.getAtom(0);
            } else {
                return Value.createFromBlock(Block.createStringArray(f));
            }
        } catch (final SecurityException se) {
            sysError(path.asBlock().asString(), se.toString());
            return Value.UNIT;
        }
    }

    /**
     * Raises a system error.
     * @param arg message prefix - may be <tt>null</tt>
     * @param ex exception message
     * @throws Fail.Exception always
     */
    static void sysError(final String arg, final String ex) throws Fail.Exception {
        if (arg == null) {
            Fail.raiseSysError(ex);
        } else {
            final StringBuilder sb = new StringBuilder(arg);
            sb.append(": ");
            sb.append(ex);
            Fail.raiseSysError(sb.toString());
        }
    }

    /**
     * Raises a system i/o error.
     * @param arg message prefix - may be <tt>null</tt>
     * @param ex exception message
     * @throws Fail.Exception always
     */
    static void sysIOError(final String arg, final String ex) throws Fail.Exception {
        sysError(arg, ex);
    }
}
