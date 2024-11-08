package textfilter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;
import common.threads.WorkerThreadPool;
import common.threads.WorkRequest;

/**
 *  A class that properly manages a "Runtime.exec()" call, using separate
 *	threads to properly deal with the various streams and avoid deadlocks.
 *
 *	@author		Marcelo Vanzin
 *  @version	$Id: CmdExec.java 23 2005-12-12 04:28:56Z vanza $
 */
public class CmdExec {

    private Process process;

    private InputTask stdin;

    private OutputTask stdout;

    private OutputTask stderr;

    private WorkRequest stdinReq;

    private WorkRequest stdoutReq;

    private WorkRequest stderrReq;

    private Timer toTimer;

    private TimerTask toTask;

    private int waiting;

    private int timeout;

    private String cmd;

    private String argLine;

    private ArrayList args;

    public CmdExec() {
        this(null);
    }

    public CmdExec(String cmd) {
        this.cmd = cmd;
        this.timeout = 0;
        this.waiting = 2;
    }

    /** Starts the execution of the command and returns the created process. */
    public Process execute() throws IOException {
        String[] command = null;
        String commandLine = null;
        if (argLine != null) {
            StringBuffer sb = new StringBuffer();
            sb.append(cmd);
            sb.append(' ');
            sb.append(argLine);
            if (args != null) {
                for (Iterator i = args.iterator(); i.hasNext(); ) {
                    sb.append(' ').append(i.next());
                }
            }
            commandLine = sb.toString();
        } else {
            if (args != null) {
                command = new String[args.size() + 1];
                command[0] = cmd;
                for (int i = 0; i < args.size(); i++) {
                    command[i + 1] = (String) args.get(i);
                }
            } else {
                commandLine = cmd;
            }
        }
        if (command != null) {
            process = Runtime.getRuntime().exec(command);
        } else {
            process = Runtime.getRuntime().exec(commandLine);
        }
        if (stdin != null) {
            stdin.dest = process.getOutputStream();
            waiting++;
        }
        stdout = new OutputTask();
        stdout.inData = process.getInputStream();
        stderr = new OutputTask();
        stderr.inData = process.getErrorStream();
        if (stdin != null) {
            WorkRequest[] reqs = WorkerThreadPool.getSharedInstance().addRequests(new Runnable[] { stdin, stdout, stderr });
            stdinReq = reqs[0];
            stdoutReq = reqs[1];
            stderrReq = reqs[2];
        } else {
            WorkRequest[] reqs = WorkerThreadPool.getSharedInstance().addRequests(new Runnable[] { stdout, stderr });
            stdoutReq = reqs[0];
            stderrReq = reqs[1];
        }
        if (timeout > 0) {
            toTimer = new Timer();
            toTask = new Timeout();
            toTimer.schedule(toTask, timeout);
        }
        return process;
    }

    /**
	 *	Sets the command that will be run. Note that if you use the array of
	 *	arguments, this command should be the name of a command that's executable,
	 *	and not contain any arguments. If you're using the argument line
	 *	approach, then it's ok to add arguments here, since the two strings
	 *	will be concatenated. It's also OK to pass arguments if no other
	 *	args or argument line are provided.
	 *
	 *	<p>Just be aware that mixing the argument line with the array of
	 *	arguments may break things, since the arguments in the array will
	 *	not be given any special treatment such as escaping characters or
	 *	adding quotes.</p>
	 */
    public void setCommand(String command) {
        this.cmd = command;
    }

    /**
	 *	Sets the argument line for the command. This will cause the
	 *	{@link #setCommand(String) command} to be executed by calling
	 *	Runtime.getRuntime().exec(String). The string will be a concatenation
	 *	of the command, this argument line and anything that might have been
	 *	added to the argument array.
	 */
    public void setArgumentLine(String args) {
        this.argLine = args;
    }

    /**
	 *	Adds an argument to the argument array. If no
	 *	{@link #setArgumentLine(String) argument&nbsp;line} is set, then the
	 *	command will be executed by calling Runtime.getRuntime().exec(String[]),
	 *	with each argument in the array as a separate argument to the command.
	 */
    public void addArgument(String arg) {
        if (args == null) args = new ArrayList();
        args.add(arg);
    }

    /**
	 *	Sets the stream that will be used to read data to pass to the command
	 *	via stdin.
	 */
    public void setStdin(InputStream in) {
        if (stdin == null) stdin = new InputTask();
        stdin.in = in;
        stdin.inStr = null;
    }

    /**
	 *	Sets the given string as input to the process. The string will be fed
	 *	to the process via stdin, not as an argument.
	 */
    public void setStdin(String value) {
        if (stdin == null) stdin = new InputTask();
        stdin.inStr = value;
        stdin.in = null;
    }

    /**
	 *	This should be called after the process is done, and returns a byte
	 *	array containing the output of the program. If the process is still
	 *	running, this call will block till it finishes.
	 */
    public byte[] getStdout() {
        if (stdout != null) {
            try {
                stdoutReq.waitFor();
            } catch (InterruptedException ie) {
            }
            return stdout.out.toByteArray();
        }
        return null;
    }

    /**
	 *	This should be called after the process is done, and returns a byte
	 *	array containing the error output of the program. If the process is
	 *	still running, this call will block till it finishes.
	 */
    public byte[] getStderr() {
        if (stderr != null) {
            try {
                stderrReq.waitFor();
            } catch (InterruptedException ie) {
            }
            return stderr.out.toByteArray();
        }
        return null;
    }

    /**
	 *	Sets a timeout for the executing command. If nothing is seen in
	 *	stdout or stderr, or nothing is read from stdin, within this interval,
	 *	the program will be killed. Set to 0 for "no timeout" (= default).
	 */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
	 *	Pipes the input stream into the output stream, using blocks of 1024
	 *	bytes. Does not close any streams. After a successful read and write,
	 *	the timeout timer is reset (if enabled).
	 */
    private void pipe(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer, 0, buffer.length)) != -1) {
            out.write(buffer, 0, read);
            out.flush();
            if (toTask != null) {
                synchronized (this) {
                    toTask.cancel();
                    toTimer.schedule(toTask, timeout);
                }
            }
        }
    }

    /** Thread to handle sending data to the external process. */
    private class InputTask implements Runnable {

        public String inStr;

        public InputStream in;

        public OutputStream dest;

        public void run() {
            try {
                if (inStr != null) {
                    in = new ByteArrayInputStream(inStr.getBytes());
                }
                if (in != null) {
                    BufferedOutputStream out = new BufferedOutputStream(dest);
                    pipe(in, out);
                    in.close();
                    out.close();
                }
            } catch (IOException ioe) {
                Log.log(Log.ERROR, this, ioe);
            }
            if (toTimer != null) synchronized (CmdExec.this) {
                waiting--;
                if (waiting == 0 && toTimer != null) toTimer.cancel();
            }
        }
    }

    /** Thread to handle reading data from the external process. */
    private class OutputTask implements Runnable {

        public InputStream inData;

        public ByteArrayOutputStream out;

        public void run() {
            try {
                BufferedInputStream in = new BufferedInputStream(inData);
                out = new ByteArrayOutputStream();
                pipe(in, out);
                out.close();
            } catch (IOException ioe) {
                Log.log(Log.ERROR, this, ioe);
            }
            if (toTimer != null) synchronized (CmdExec.this) {
                waiting--;
                if (waiting == 0 && toTimer != null) toTimer.cancel();
            }
        }
    }

    /** Handles timing out the external process. */
    private class Timeout extends TimerTask {

        public void run() {
            Log.log(Log.WARNING, this, "External process timed out.");
            process.destroy();
            synchronized (CmdExec.this) {
                toTimer.cancel();
            }
        }
    }
}