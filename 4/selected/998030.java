package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkState;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.io.Closeables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * This class can be used to execute a separate process and read it's output in
 * a convenient way. It is only useful for processes which handle only one task
 * and exit afterwards.
 *
 * This class is not thread-safe, it assumes that never two of it's methods are
 * executed simultaneously.
 *
 * When an instance of this class is created, the corresponding process is
 * started immediately. Then some text may be written to stdin of the process
 * with the {@link #println(String)} method. Afterwards {@link #join()} has to be
 * called, which reads the output from the process and calls the handle* methods.
 * This method blocks, i.e. when it returns the process has terminated. Now the
 * get* methods may be used to get the output of the process.
 *
 * @param <E> The type of the exceptions the handle* methods may throw.
 */
public class ProcessExecutor<E extends Exception> {

    private final String name;

    private final Class<E> exceptionClass;

    private final Writer in;

    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(3));

    private final ListenableFuture<?> outFuture;

    private final ListenableFuture<?> errFuture;

    private final ListenableFuture<Integer> processFuture;

    private final List<String> output = new ArrayList<String>();

    private final List<String> errorOutput = new ArrayList<String>();

    private boolean finished = false;

    protected final LogManager logger;

    /**
   * Create an instance and immediately execute the supplied command.
   *
   * Whenever a line is read on stdout or stderr of the process,
   * the {@link #handleOutput(String)} or the {@link #handleErrorOutput(String)}
   * are called respectively.
   *
   * It is strongly advised to call {@link #join()} sometimes, as otherwise
   * there may be resources not being cleaned up properly.
   * Also exceptions thrown by the handling methods would get swallowed.
   *
   * @see Runtime#exec(String[])
   * @param cmd The command with arguments to execute.
   * @throws IOException If the process cannot be executed.
   */
    public ProcessExecutor(final LogManager logger, Class<E> exceptionClass, String... cmd) throws IOException {
        Preconditions.checkNotNull(cmd);
        Preconditions.checkArgument(cmd.length > 0);
        this.logger = Preconditions.checkNotNull(logger);
        this.exceptionClass = Preconditions.checkNotNull(exceptionClass);
        this.name = cmd[0];
        logger.log(Level.FINEST, "Executing", name);
        logger.log(Level.ALL, (Object[]) cmd);
        final Process process = Runtime.getRuntime().exec(cmd);
        processFuture = executor.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws E {
                logger.log(Level.FINEST, "Waiting for", name);
                try {
                    int exitCode = process.waitFor();
                    logger.log(Level.FINEST, name, "has terminated normally");
                    handleExitCode(exitCode);
                    return exitCode;
                } catch (InterruptedException e) {
                    process.destroy();
                    while (true) {
                        try {
                            int exitCode = process.waitFor();
                            logger.log(Level.FINEST, name, "has terminated after it was cancelled");
                            Thread.currentThread().interrupt();
                            return exitCode;
                        } catch (InterruptedException _) {
                        }
                    }
                }
            }
        });
        in = new OutputStreamWriter(process.getOutputStream());
        outFuture = executor.submit(new Callable<Void>() {

            @Override
            public Void call() throws E, IOException {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        handleOutput(line);
                    }
                } catch (IOException e) {
                    if (processFuture.isCancelled()) {
                        logger.logDebugException(e, "IOException after process was killed");
                    } else {
                        throw e;
                    }
                } finally {
                    Closeables.closeQuietly(reader);
                }
                return null;
            }
        });
        errFuture = executor.submit(new Callable<Void>() {

            @Override
            public Void call() throws E, IOException {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        handleErrorOutput(line);
                    }
                } catch (IOException e) {
                    if (processFuture.isCancelled()) {
                        logger.logDebugException(e, "IOException after process was killed");
                    } else {
                        throw e;
                    }
                } finally {
                    Closeables.closeQuietly(reader);
                }
                return null;
            }
        });
        FutureCallback<Object> cancelProcessOnFailure = new FutureCallback<Object>() {

            @Override
            public void onFailure(Throwable e) {
                if (!processFuture.isCancelled()) {
                    logger.logUserException(Level.WARNING, e, "Killing " + name + " due to error in output handling");
                    processFuture.cancel(true);
                } else {
                    logger.logDebugException(e, "Error in output handling after " + name + " was already killed");
                }
            }

            @Override
            public void onSuccess(Object pArg0) {
            }
        };
        Futures.addCallback(outFuture, cancelProcessOnFailure);
        Futures.addCallback(errFuture, cancelProcessOnFailure);
        executor.shutdown();
    }

    /**
   * Write a String to the process. May only be called before {@link #join()}
   * was called, as afterwards the process is not running anymore.
   * @throws IOException
   */
    public void println(String s) throws IOException {
        print(s + "\n");
    }

    /**
   * Write a String to the process. May only be called before {@link #join()}
   * was called, as afterwards the process is not running anymore.
   * @throws IOException
   */
    public void print(String s) throws IOException {
        checkState(!finished, "Cannot write to process that has already terminated.");
        in.write(s);
        in.flush();
    }

    /**
   * Sends the EOF (end of file) signal to stdin of the process.
   * @throws IOException
   */
    public void sendEOF() throws IOException {
        checkState(!finished, "Cannot write to process that has already terminated.");
        in.close();
    }

    /**
   * Wait for the process to terminate.
   *
   * @param timelimit Maximum time to wait for process (in milliseconds)
   * @return The exit code of the process.
   * @throws IOException
   * @throws E passed from the handle* methods.
   * @throws TimeoutException If timeout is hit.
   * @throws InterruptedException
   */
    public int join(final long timelimit) throws IOException, E, TimeoutException, InterruptedException {
        try {
            Integer exitCode = null;
            try {
                if (timelimit > 0) {
                    exitCode = processFuture.get(timelimit, TimeUnit.MILLISECONDS);
                } else {
                    exitCode = processFuture.get();
                }
            } catch (CancellationException e) {
            }
            outFuture.get();
            errFuture.get();
            if (exitCode == null) {
                throw new InterruptedException();
            }
            return exitCode;
        } catch (TimeoutException e) {
            logger.log(Level.WARNING, "Killing", name, "due to timeout");
            processFuture.cancel(true);
            throw e;
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Killing", name, "due to user interrupt");
            processFuture.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            Throwables.propagateIfPossible(t, IOException.class, exceptionClass);
            throw new UnexpectedCheckedException("output handling of external process " + name, t);
        } finally {
            assert processFuture.isDone();
            Concurrency.waitForTermination(executor);
            Closeables.closeQuietly(in);
            finished = true;
        }
    }

    /**
   * Wait for the process to terminate and read all of it's output. Whenever a
   * line is read on stdout or stderr of the process, the {@link #handleOutput(String)}
   * or the {@link #handleErrorOutput(String)} are called respectively.
   *
   * @return The exit code of the process.
   * @throws IOException
   * @throws E passed from the handle* methods.
   * @throws InterruptedException
   */
    public int join() throws IOException, E, InterruptedException {
        try {
            return join(0);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    /**
   * Handle one line of output from the process. This method may be overwritten
   * by clients. The default implementation logs the line on level ALL and adds it
   * to a list which may later be retrieved with {@link #getOutput()}. It never
   * throws an exception (but client implementations may do so).
   *
   * This method will be called in a new thread.
   */
    protected void handleOutput(String line) throws E {
        logger.log(Level.ALL, name, "output:", line);
        output.add(line);
    }

    /**
   * Handle one line of stderr output from the process. This method may be overwritten
   * by clients. The default implementation logs the line on level WARNING and adds it
   * to a list which may later be retrieved with {@link #getErrorOutput()}. It never
   * throws an exception (but client implementations may do so).
   *
   * This method will be called in a new thread.
   */
    protected void handleErrorOutput(String line) throws E {
        logger.log(Level.WARNING, name, "error output:", line);
        errorOutput.add(line);
    }

    /**
   * Handle the exit code of the process. This method may be overwritten
   * by clients. The default implementation logs the code on level WARNING, if
   * it is non-zero.
   *
   * This method will be called in a new thread.
   */
    protected void handleExitCode(int code) throws E {
        if (code != 0) {
            logger.log(Level.WARNING, "Exit code from", name, "was", code);
        }
    }

    /**
   * Checks whether the process has finished already.
   * This is true exactly if {@link #join()} has been called.
   */
    public boolean isFinished() {
        return finished;
    }

    /**
   * Returns the complete output of the process.
   * May only be called after {@link #join()} has been called.
   */
    public List<String> getOutput() {
        checkState(finished, "Cannot get output while process is not yet finished");
        return output;
    }

    /**
   * Returns the complete output to stderr of the process.
   * May only be called after {@link #join()} has been called.
   */
    public List<String> getErrorOutput() {
        checkState(finished, "Cannot get error output while process is not yet finished");
        return errorOutput;
    }
}
