package net.sourceforge.javautil.common.process;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.javautil.common.StringUtil;
import net.sourceforge.javautil.common.ThreadUtil;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;
import net.sourceforge.javautil.common.io.OutputStreamRegistry;

/**
 * This will allow simple execution and management of a {@link Process}.
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: ExecutableController.java 1564 2009-12-22 16:53:59Z ponderator $
 */
public class ExecutableController {

    protected final Process process;

    protected final String[] execution;

    protected Integer exitValue;

    protected ProcessInput input;

    protected Thread inputThread;

    protected ProcessRunning running;

    protected OutputStreamRegistry registry;

    protected boolean echo = false;

    public ExecutableController(Process process, String... execution) {
        this(process, null, execution);
    }

    public ExecutableController(Process process, InputStream input, String... execution) {
        this.process = process;
        this.execution = execution;
        this.registry = new OutputStreamRegistry();
        if (input != null) this.setInput(input);
        new Thread(this.running = new ProcessRunning(), "Running Watcher [" + StringUtil.join(execution, ' ') + "]").start();
        new Thread(new ProcessOutput(), "Output Watcher [" + StringUtil.join(execution, ' ') + "]").start();
    }

    /**
	 * This will cause output from the process to be directed to this target.
	 * This can be called more than once to have multiple output streams receive
	 * process output.
	 * 
	 * @param target The target output stream
	 */
    public ExecutableController register(OutputStream target) {
        this.registry.addListener("process", target, true);
        return this;
    }

    /**
	 * If a previous input stream was set this will stop it and wait for it to stop
	 * then setup the new input stream.
	 * 
	 * @param input The input stream for the process when requiring input
	 * @return This controller for chaining
	 */
    public ExecutableController setInput(InputStream input) {
        if (this.inputThread != null) {
            this.input.running = false;
            this.inputThread.interrupt();
        }
        this.input = new ProcessInput(input);
        (this.inputThread = new Thread(this.input, "Input Tracker [" + StringUtil.join(execution, ' ') + "]")).start();
        return this;
    }

    /**
	 * @return True if input should be echoed to the output registry, otherwise false
	 */
    public boolean isEcho() {
        return echo;
    }

    public void setEcho(boolean echo) {
        this.echo = echo;
    }

    /**
	 * @return The execution context
	 */
    public String[] getExecution() {
        return execution;
    }

    /**
	 * @return True if the process is currently running
	 */
    public boolean isRunning() {
        return running.running;
    }

    /**
	 * @return The exit value for the process, or null if the process has not completed yet
	 */
    public Integer getExitValue() {
        return this.exitValue;
    }

    /**
	 * @return The process controlled by this controller
	 */
    public Process getProcess() {
        return process;
    }

    /**
	 * This will wait for the process to finish and then set the exit value.
	 *
	 * @author elponderador
	 * @author $Author: ponderator $
	 * @version $Id: ExecutableController.java 1564 2009-12-22 16:53:59Z ponderator $
	 */
    public class ProcessRunning implements Runnable {

        protected boolean running = true;

        public void run() {
            try {
                exitValue = process.waitFor();
                if (inputThread != null) {
                    input.running = false;
                    input.input.close();
                    inputThread.interrupt();
                }
            } catch (Exception e) {
                ThrowableManagerRegistry.caught(e);
            }
            this.running = false;
        }
    }

    /**
	 * This will direct output to the registry for the controller.
	 *
	 * @author elponderador
	 * @author $Author: ponderator $
	 * @version $Id: ExecutableController.java 1564 2009-12-22 16:53:59Z ponderator $
	 */
    public class ProcessOutput implements Runnable {

        public void run() {
            byte[] buffer = new byte[1024];
            try {
                do {
                    try {
                        OutputStream target = registry.getStream("process", false);
                        if (target != null) {
                            int read = (process.getInputStream().read(buffer));
                            if (read != -1) target.write(buffer, 0, read); else break;
                        } else {
                            ThreadUtil.sleep(100);
                        }
                    } catch (Exception e) {
                        ThrowableManagerRegistry.caught(e);
                    }
                } while (running.running || process.getInputStream().available() != 0);
            } catch (IOException e) {
                throw ThrowableManagerRegistry.caught(e);
            }
        }
    }

    /**
	 * For thread's processing different input streams. 
	 *
	 * @author elponderador
	 * @author $Author: ponderator $
	 * @version $Id: ExecutableController.java 1564 2009-12-22 16:53:59Z ponderator $
	 */
    public class ProcessInput implements Runnable {

        protected InputStream input;

        protected boolean running = true;

        private ProcessInput(InputStream input) {
            this.input = input;
        }

        public void run() {
            try {
                byte[] data = new byte[1024];
                int read = -1;
                while (ExecutableController.this.running == null || (running && ExecutableController.this.running.running)) {
                    if (input.available() > 0) {
                        read = input.read(data);
                        if (read == -1) break;
                        process.getOutputStream().write(data, 0, read);
                        process.getOutputStream().flush();
                        if (echo) registry.getStream("process", false).write(data, 0, read);
                    }
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                return;
            } catch (IOException e) {
                throw ThrowableManagerRegistry.caught(e);
            } finally {
                try {
                    this.input.close();
                } catch (IOException e) {
                    ThrowableManagerRegistry.caught(e);
                }
                this.input = null;
            }
        }
    }
}
