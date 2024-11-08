package com.myjavatools.lib;

import static com.myjavatools.lib.Files.getcwd;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Map;
import javax.swing.JOptionPane;

public abstract class Tools {

    private static final boolean DEBUG = false;

    /**
   * Reports a fatal error to sderr and exits, upon a condition
   *
   * @param condition it is fatal error when condition is true
   * @param message the error message
   * <br><br><b>Example</b>:
   * <p><code>fatalError(!file.exists(), "File " + file + " does not exist!")</code>
   */
    public static final void fatalError(final boolean condition, final String message) {
        if (condition) {
            fatalError(message);
        }
    }

    /**
   * Reports a fatal error to sderr and exits
   *
   * @param message the error message
   * <br><br><b>Example</b>:
   * <p><code>fatalError("Your System is Windows!")</code>
   */
    public static final void fatalError(final String message) {
        System.err.println(message);
        System.exit(1);
    }

    /**
   * Reports a fatal exception to stderr and exits
   *
   * @param exception the fatal exception
   *
   * <br><br><b>Example</b>:
   * <p><code>
   * try {<br>
   * &nbsp;&nbsp;String s = null;<br>
   * &nbsp;&nbsp;s.toString();<br>
   * } catch (Exception e) {<br>
   * &nbsp;&nbsp;fatalError(e);<br>
   * }<br></code> prints
   * <p><code>java.lang.NullPointerException<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;at com.myjavatools.util.Tools.main</code>
   */
    public static final void fatalError(final Exception exception) {
        exception.printStackTrace(System.err);
        System.exit(1);
    }

    /**
   * Reports an error message and an exception to sderr and exits
   *
   * @param message the error message
   * @param exception the fatal exception
   *
   * <br><br><b>Example</b>:
   * <p><code>
   * try {<br>
   * &nbsp;&nbsp;String s = null;<br>
   * &nbsp;&nbsp;s.toString();<br>
   * } catch (Exception e) {<br>
   * &nbsp;&nbsp;fatalError("Null pointers are bad!", e);<br>
   * }<br></code> prints
   * <p><code>Null pointers are bad!<br>
   * java.lang.NullPointerException<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;at com.myjavatools.util.Tools.main</code>
   */
    public static final void fatalError(final String message, final Exception exception) {
        System.err.println(message);
        exception.printStackTrace(System.err);
        System.exit(1);
    }

    /**
   * Displays an error message
   *
   * @param msg the message to display
   * @return always false
   *
   * <br><br><b>Example</b>:
   * <li><code>bark("Code Red Alert!")</code>.</li>
   */
    public static boolean bark(final String msg) {
        JOptionPane.showConfirmDialog(null, msg, "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        return false;
    }

    /**
   * Displays an informative message
   *
   * @param msg the message to display
   * @return always false
   *
   * <br><br><b>Example</b>:
   * <li><code>inform("To beguile the time, be like the time.")</code>.</li>
   */
    public static boolean inform(final String msg) {
        JOptionPane.showConfirmDialog(null, msg, "FYI", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
        return false;
    }

    /**
   * Displays a message and receives user's choice (yes/no)
   *
   * @param msg the messge to display
   * @return true if user clicked "Yes", otherwise returns "No"
   *
   * <br><br><b>Example</b>:
   * <li><code>if (!whether("Want this program to proceed?")) System.exit(1);<code></li>
   */
    public static boolean whether(final String msg) {
        return JOptionPane.showConfirmDialog(null, msg + "?", "Confirmation required", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /**
   * Collects garbage, reports back
   * @return an array of three Longs, containing total memory,
   * free memory before and after gc
   */
    public static Long[] gc() {
        final Long total = new Long(Runtime.getRuntime().totalMemory() / 1000);
        final Long free1 = new Long(Runtime.getRuntime().freeMemory() / 1000);
        System.gc();
        final Long free2 = new Long(Runtime.getRuntime().freeMemory() / 1000);
        return new Long[] { total, free1, free2 };
    }

    /**
   * Collects garbage, reports back
   * @return a string with memory usage report
   */
    public static String freeMemory() {
        return MessageFormat.format("Memory usage: total = {0}, free: {1} -> {2}", (Object) gc());
    }

    /**
   * gets values of all environment variables
   *
   * @return Map<String,String>
   *
   * The returned map contains environment variable names as keys and their values as values.
   */
    public static Map<String, String> getEnv() {
        String output = getCommandOutput("env");
        if (DEBUG) {
            System.out.println("getEnv: env returned\n" + output + "\n-----------------------");
        }
        if (output == null) {
            output = getCommandOutput("cmd /c set");
            if (DEBUG) {
                System.out.println("getEnv: cmd /c set returned\n" + output + "\n-----------------------");
            }
        }
        if (output == null) {
            return null;
        }
        final String[] env = output.replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n");
        final Map<String, String> result = new java.util.TreeMap<String, String>();
        for (final String element : env) {
            final String[] nv = element.split("=");
            if (nv.length > 1) {
                result.put(nv[0].trim(), nv[1].trim());
            }
        }
        return result;
    }

    private static class Runner extends Thread {

        public static final int ERR_CANNOT_START = -1;

        public static final int PROC_KILLED = -2;

        public int exitCode = -1;

        private String cmd;

        private Process process;

        private String dir;

        private PrintWriter out;

        private PrintWriter err;

        private Reader in = null;

        private Reader processOut;

        private Reader processErr;

        private PrintWriter processIn;

        private String[] env = null;

        private boolean needKill;

        Runner(final String cmd, final String dir, final PrintStream out, final PrintStream err) {
            this.cmd = cmd;
            this.dir = dir;
            this.out = new PrintWriter(out);
            this.err = new PrintWriter(err);
        }

        Runner(final String cmd, final String dir, final InputStream in, final PrintStream out, final PrintStream err) {
            this.cmd = cmd;
            this.dir = dir;
            this.in = new BufferedReader(new InputStreamReader(in));
            this.out = new PrintWriter(out);
            this.err = new PrintWriter(err);
        }

        Runner(final String cmd, final String dir, final Reader in, final PrintWriter out, final PrintWriter err, final Map<String, String> environment) {
            this.cmd = cmd;
            this.dir = dir;
            this.in = in;
            this.out = out;
            this.err = err;
            this.env = Strings.toStrings(environment);
        }

        void relieve() {
            if (process != null) {
                Files.pipe(processOut, out);
                Files.pipe(processErr, err);
                Files.pipe(in, processIn);
            }
        }

        static void pipe(final InputStream in, final OutputStream out, final boolean isBlocking) throws IOException {
            final byte[] buf = new byte[65500];
            int nread;
            int navailable;
            int total = 0;
            synchronized (in) {
                while (((navailable = isBlocking ? Integer.MAX_VALUE : in.available()) > 0) && ((nread = in.read(buf, 0, Math.min(buf.length, navailable))) >= 0)) {
                    out.write(buf, 0, nread);
                    total += nread;
                }
            }
            out.flush();
        }

        @Override
        public void run() {
            try {
                setLowestPriority();
                process = Runtime.getRuntime().exec(cmd, env, new File(dir));
                processOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
                processErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                processIn = new PrintWriter(process.getOutputStream());
                while (true) {
                    try {
                        if (needKill) {
                            process.destroy();
                            exitCode = PROC_KILLED;
                            break;
                        }
                        exitCode = process.exitValue();
                        break;
                    } catch (final Exception e) {
                        relieve();
                        try {
                            Thread.sleep(200);
                        } catch (final InterruptedException ie) {
                        }
                    }
                }
            } catch (final Exception e) {
                if (DEBUG) {
                    e.printStackTrace(System.err);
                }
                if (err != null) {
                    err.println(e);
                    e.printStackTrace(err);
                }
            }
            relieve();
            try {
                process.getInputStream().close();
                process.getErrorStream().close();
            } catch (final Exception e) {
            }
            ;
        }
    }

    /**
   * Runs a command in current directory
   * @param cmd the command to run; append '&' if no need to wait
   * @return false if return code is not 0
   *
   * <br><br><b>Examples</b>:
   * <li><code>runCommand("notepad&")> launches Notepad</li>
   * <li><code>runCommand("rmdir xxx")> returns <b>false</b>
   * (unless you had a directory 'xxx' and it was just removed).</li>
   */
    public static boolean runCommand(final String cmd) {
        return runCommand(cmd, ".");
    }

    /**
   * Runs a command in specified directory
   * @param cmd the command to run; append '&' if no need to wait
   * @param dir starting directory name
   * @return false if return code is not 0
   *
   * <br><br><b>Example</b>:
   * <li><code>runCommand("cmd /c dir .", "C:\\Program Files")>.</li>
   */
    public static boolean runCommand(final String cmd, final String dir) {
        return runCommand(cmd, dir, System.out, System.err);
    }

    /**
   * Runs a command in current directory
   * @param cmd the command to run; append '&' if no need to wait
   * @param out command output stream
   * @param err command error stream
   * @return false if return code is not 0
   *
   * <br><br><b>Example</b>:
   * <li><code>runCommand("cmd /c dir .", "C:\\Program Files", System.out, System.err)>.</li>
   */
    public static boolean runCommand(final String cmd, final PrintStream out, final PrintStream err) {
        return runCommand(cmd, ".", out, err);
    }

    /**
   * Runs a command in specified directory
   * @param cmd the command to run; append '&' if no need to wait
   * @param dir starting directory name
   * @param out command output stream
   * @param err command error stream
   * @return false if return code is not 0
   *
   * <br><br><b>Example</b>:
   * <li><code>runCommand("cmd /c dir .", "C:\\Program Files", System.out, System.err)>.</li>
   */
    public static boolean runCommand(final String cmd, final String dir, final PrintStream out, final PrintStream err) {
        return runCommand(cmd, dir, null, out, err);
    }

    /**
   * Runs a command in current directory
   * @param cmd the command to run; append '&' if no need to wait
   * @param in  command input stream
   * @param out command output stream
   * @param err command error stream
   * @return false if return code is not 0
   *
   * <br><br><b>Example</b>:
   * <li><code>runCommand("cmd /c dir .", "C:\\Program Files", System.in, System.out, System.err)>.</li>
   */
    public static boolean runCommand(final String cmd, final InputStream in, final PrintStream out, final PrintStream err) {
        return runCommand(cmd, ".", in, out, err);
    }

    /**
   * Runs a command in specified directory
   * @param cmd the command to run; append '&' if no need to wait
   * @param dir starting directory name
   * @param in  command input stream
   * @param out command output stream
   * @param err command error stream
   * @return false if return code is not 0
   *
   * <br><br><b>Example</b>:
   * <li><code>runCommand("cmd /c dir .", "C:\\Program Files", System.in, System.out, System.err)>.</li>
   */
    public static boolean runCommand(final String cmd, final String dir, final InputStream in, PrintStream out, PrintStream err) {
        final Reader br = in == null ? null : new BufferedReader(new InputStreamReader(in));
        if (err == null) {
            err = out;
        }
        if (err == null) {
            err = System.err;
        }
        if (out == null) {
            out = System.out;
        }
        return runCommand(cmd, dir, br, new PrintWriter(out), new PrintWriter(err));
    }

    /**
   * Runs a command in specified directory
   * @param cmd the command to run; append '&' if no need to wait
   * @param dir starting directory name
   * @param in  command input reader
   * @param out command output writer
   * @param err command error writer
   * @return false if return code is not 0
   *
   * <br><br><b>Example</b>:
   * <li><code>runCommand("cmd /c dir .", "C:\\Program Files", System.in, System.out, System.err)>.</li>
   */
    public static boolean runCommand(final String cmd, final String dir, final Reader in, final PrintWriter out, final PrintWriter err) {
        return runCommand(cmd, dir, in, out, err, null);
    }

    public static boolean runCommand(String cmd, final String dir, final Reader in, final PrintWriter out, final PrintWriter err, final Map<String, String> environment) {
        final boolean isParallel = cmd.endsWith("&");
        if (isParallel) {
            cmd = cmd.substring(0, cmd.length() - 1);
        }
        final Runner process = new Runner(cmd, dir, in, out, err, environment);
        process.start();
        if (isParallel) {
            return true;
        }
        while (process.isAlive()) {
            try {
                Thread.sleep(500);
            } catch (final Exception e) {
            }
        }
        System.gc();
        if (process.exitCode == -1) {
            return false;
        } else if (process.exitCode != 0) {
            if (err != null) {
                err.println("runner: exit_code = " + process.exitCode);
            }
            return false;
        } else {
            return true;
        }
    }

    /**
   * returns as string the output of a command
   * @param command String the command to run
   * @return String command output (from STDOUT)
   */
    public static String getCommandOutput(final String command) {
        final StringWriter w = new StringWriter();
        runCommand(command, getcwd(), null, new PrintWriter(w), null);
        if (w.getBuffer().length() > 0) {
            return w.toString();
        } else {
            return null;
        }
    }

    /**
   * Wraps command line argument into quotes if it contains spaces
   * @param arg
   * @return wrapped value
   */
    public static String commandLineArg(final String arg) {
        return (arg.indexOf(' ') < 0) ? arg : ("\"" + arg + "\"");
    }

    /**
   * sets priority and returns previous priority
   * @param priority int
   * @return int the priority that was before
   */
    public static int setPriority(final int priority) {
        final int currentPriority = Thread.currentThread().getPriority();
        Thread.currentThread().setPriority(priority);
        return currentPriority;
    }

    /**
   * sets the lowest priority, Thread.MIN_PRIORITY
   * @return int the priority that was before
   */
    public static int setLowestPriority() {
        return setPriority(Thread.MIN_PRIORITY);
    }
}
