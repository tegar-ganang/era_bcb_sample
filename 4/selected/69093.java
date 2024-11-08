package goldengate.commandexec.server;

import goldengate.commandexec.utils.LocalExecDefaultResult;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.RejectedExecutionException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * Handles a server-side channel for LocalExec.
 *
 *
 */
public class LocalExecServerHandler extends SimpleChannelUpstreamHandler {

    private long delay = LocalExecDefaultResult.MAXWAITPROCESS;

    protected LocalExecServerPipelineFactory factory = null;

    protected static boolean isShutdown = false;

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(LocalExecServerHandler.class);

    protected boolean answered = false;

    /**
     * Is the Local Exec Server going Shutdown
     * @param channel associated channel
     * @return True if in Shutdown
     */
    public static boolean isShutdown(Channel channel) {
        if (isShutdown) {
            channel.write(LocalExecDefaultResult.ConnectionRefused.result);
            channel.write(LocalExecDefaultResult.ENDOFCOMMAND).awaitUninterruptibly();
            Channels.close(channel);
            return true;
        }
        return false;
    }

    /**
     * Print stack trace
     * @param thread
     * @param stacks
     */
    private static void printStackTrace(Thread thread, StackTraceElement[] stacks) {
        System.err.print(thread.toString() + " : ");
        for (int i = 0; i < stacks.length - 1; i++) {
            System.err.print(stacks[i].toString() + " ");
        }
        System.err.println(stacks[stacks.length - 1].toString());
    }

    /**
     * Shutdown thread
     * @author Frederic Bregier
     *
     */
    private static class GGLEThreadShutdown extends Thread {

        long delay = 3000;

        LocalExecServerPipelineFactory factory;

        public GGLEThreadShutdown(LocalExecServerPipelineFactory factory) {
            this.factory = factory;
        }

        @Override
        public void run() {
            Timer timer = null;
            timer = new Timer(true);
            GGLETimerTask ggleTimerTask = new GGLETimerTask();
            timer.schedule(ggleTimerTask, delay);
            factory.releaseResources();
            System.exit(0);
        }
    }

    /**
     * TimerTask to terminate the server
     * @author Frederic Bregier
     *
     */
    private static class GGLETimerTask extends TimerTask {

        /**
         * Internal Logger
         */
        private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(GGLETimerTask.class);

        @Override
        public void run() {
            logger.error("System will force EXIT");
            Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
            for (Thread thread : map.keySet()) {
                printStackTrace(thread, map.get(thread));
            }
            System.exit(0);
        }
    }

    /**
     * Constructor with a specific delay
     * @param newdelay
     */
    public LocalExecServerHandler(LocalExecServerPipelineFactory factory, long newdelay) {
        this.factory = factory;
        delay = newdelay;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        if (isShutdown(ctx.getChannel())) {
            answered = true;
            return;
        }
        answered = false;
        factory.addChannel(ctx.getChannel());
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        this.factory.removeChannel(e.getChannel());
    }

    /**
     * Change the delay to the specific value. Need to be called before any receive message.
     * @param newdelay
     */
    public void setNewDelay(long newdelay) {
        delay = newdelay;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent evt) {
        answered = false;
        String request = (String) evt.getMessage();
        String response;
        response = LocalExecDefaultResult.NoStatus.status + " " + LocalExecDefaultResult.NoStatus.result;
        boolean isLocallyShutdown = false;
        ExecuteWatchdog watchdog = null;
        try {
            if (request.length() == 0) {
                response = LocalExecDefaultResult.NoCommand.status + " " + LocalExecDefaultResult.NoCommand.result;
            } else {
                String[] args = request.split(" ");
                int cpt = 0;
                long tempDelay;
                try {
                    tempDelay = Long.parseLong(args[0]);
                    cpt++;
                } catch (NumberFormatException e) {
                    tempDelay = delay;
                }
                if (tempDelay < 0) {
                    isShutdown = true;
                    logger.warn("Shutdown order received");
                    isLocallyShutdown = isShutdown(evt.getChannel());
                    try {
                        Thread.sleep(-tempDelay);
                    } catch (InterruptedException e) {
                    }
                    Thread thread = new GGLEThreadShutdown(factory);
                    thread.start();
                    return;
                }
                String binary = args[cpt++];
                File exec = new File(binary);
                if (exec.isAbsolute()) {
                    if (!exec.canExecute()) {
                        logger.error("Exec command is not executable: " + request);
                        response = LocalExecDefaultResult.NotExecutable.status + " " + LocalExecDefaultResult.NotExecutable.result;
                        return;
                    }
                }
                CommandLine commandLine = new CommandLine(binary);
                for (; cpt < args.length; cpt++) {
                    commandLine.addArgument(args[cpt]);
                }
                DefaultExecutor defaultExecutor = new DefaultExecutor();
                ByteArrayOutputStream outputStream;
                outputStream = new ByteArrayOutputStream();
                PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(outputStream);
                defaultExecutor.setStreamHandler(pumpStreamHandler);
                int[] correctValues = { 0, 1 };
                defaultExecutor.setExitValues(correctValues);
                if (tempDelay > 0) {
                    watchdog = new ExecuteWatchdog(tempDelay);
                    defaultExecutor.setWatchdog(watchdog);
                }
                int status = -1;
                try {
                    status = defaultExecutor.execute(commandLine);
                } catch (ExecuteException e) {
                    if (e.getExitValue() == -559038737) {
                        try {
                            Thread.sleep(LocalExecDefaultResult.RETRYINMS);
                        } catch (InterruptedException e1) {
                        }
                        try {
                            status = defaultExecutor.execute(commandLine);
                        } catch (ExecuteException e1) {
                            pumpStreamHandler.stop();
                            logger.error("Exception: " + e.getMessage() + " Exec in error with " + commandLine.toString());
                            response = LocalExecDefaultResult.BadExecution.status + " " + LocalExecDefaultResult.BadExecution.result;
                            try {
                                outputStream.close();
                            } catch (IOException e2) {
                            }
                            return;
                        } catch (IOException e1) {
                            pumpStreamHandler.stop();
                            logger.error("Exception: " + e.getMessage() + " Exec in error with " + commandLine.toString());
                            response = LocalExecDefaultResult.BadExecution.status + " " + LocalExecDefaultResult.BadExecution.result;
                            try {
                                outputStream.close();
                            } catch (IOException e2) {
                            }
                            return;
                        }
                    } else {
                        pumpStreamHandler.stop();
                        logger.error("Exception: " + e.getMessage() + " Exec in error with " + commandLine.toString());
                        response = LocalExecDefaultResult.BadExecution.status + " " + LocalExecDefaultResult.BadExecution.result;
                        try {
                            outputStream.close();
                        } catch (IOException e2) {
                        }
                        return;
                    }
                } catch (IOException e) {
                    pumpStreamHandler.stop();
                    logger.error("Exception: " + e.getMessage() + " Exec in error with " + commandLine.toString());
                    response = LocalExecDefaultResult.BadExecution.status + " " + LocalExecDefaultResult.BadExecution.result;
                    try {
                        outputStream.close();
                    } catch (IOException e2) {
                    }
                    return;
                }
                pumpStreamHandler.stop();
                if (defaultExecutor.isFailure(status) && watchdog != null && watchdog.killedProcess()) {
                    logger.error("Exec is in Time Out");
                    response = LocalExecDefaultResult.TimeOutExecution.status + " " + LocalExecDefaultResult.TimeOutExecution.result;
                    try {
                        outputStream.close();
                    } catch (IOException e2) {
                    }
                } else {
                    response = status + " " + outputStream.toString();
                    try {
                        outputStream.close();
                    } catch (IOException e2) {
                    }
                }
            }
        } finally {
            if (isLocallyShutdown) {
                return;
            }
            evt.getChannel().write(response + "\n");
            answered = true;
            if (watchdog != null) {
                watchdog.stop();
            }
            logger.info("End of Command: " + request + " : " + response);
            evt.getChannel().write(LocalExecDefaultResult.ENDOFCOMMAND);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        if (answered) {
            logger.debug("Exception while answered: ", e.getCause());
        } else {
            logger.error("Unexpected exception from downstream while not answered.", e.getCause());
        }
        Throwable e1 = e.getCause();
        if (e1 instanceof CancelledKeyException) {
        } else if (e1 instanceof ClosedChannelException) {
        } else if (e1 instanceof NullPointerException) {
            if (e.getChannel().isConnected()) {
                e.getChannel().close();
            }
        } else if (e1 instanceof IOException) {
            if (e.getChannel().isConnected()) {
                e.getChannel().close();
            }
        } else if (e1 instanceof RejectedExecutionException) {
            if (e.getChannel().isConnected()) {
                e.getChannel().close();
            }
        }
    }
}
