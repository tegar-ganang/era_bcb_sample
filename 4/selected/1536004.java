package goldengate.ftp.exec.exec;

import goldengate.commandexec.client.LocalExecClientHandler;
import goldengate.commandexec.client.LocalExecClientPipelineFactory;
import goldengate.commandexec.utils.LocalExecResult;
import goldengate.common.future.GgFuture;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;
import goldengate.ftp.exec.config.FileBasedConfiguration;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;

/**
 * Client to execute external command through GoldenGate Local Exec
 *
 * @author Frederic Bregier
 *
 */
public class LocalExecClient {

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(LocalExecClient.class);

    public static InetSocketAddress address;

    private static ClientBootstrap bootstrapLocalExec;

    private static LocalExecClientPipelineFactory localExecClientPipelineFactory;

    private static OrderedMemoryAwareThreadPoolExecutor localPipelineExecutor;

    private static class GgThreadFactory implements ThreadFactory {

        private String GlobalName;

        public GgThreadFactory(String globalName) {
            GlobalName = globalName;
        }

        @Override
        public Thread newThread(Runnable arg0) {
            Thread thread = new Thread(arg0);
            thread.setName(GlobalName + thread.getName());
            return thread;
        }
    }

    /**
     * Initialize the LocalExec Client context
     */
    public static void initialize(FileBasedConfiguration config) {
        localPipelineExecutor = new OrderedMemoryAwareThreadPoolExecutor(config.CLIENT_THREAD * 100, config.maxGlobalMemory / 10, config.maxGlobalMemory, 500, TimeUnit.MILLISECONDS, new GgThreadFactory("LocalExecutor"));
        bootstrapLocalExec = new ClientBootstrap(new NioClientSocketChannelFactory(localPipelineExecutor, localPipelineExecutor));
        localExecClientPipelineFactory = new LocalExecClientPipelineFactory();
        bootstrapLocalExec.setPipelineFactory(localExecClientPipelineFactory);
    }

    /**
     * To be called when the server is shutting down to release the resources
     */
    public static void releaseResources() {
        if (bootstrapLocalExec == null) {
            return;
        }
        bootstrapLocalExec.releaseExternalResources();
        localExecClientPipelineFactory.releaseResources();
    }

    private Channel channel;

    private LocalExecResult result;

    public LocalExecClient() {
    }

    public LocalExecResult getLocalExecResult() {
        return result;
    }

    /**
     * Run one command with a specific allowed delay for execution.
     * The connection must be ready (done with connect()).
     * @param command
     * @param delay
     * @param futureCompletion
     */
    public void runOneCommand(String command, long delay, GgFuture futureCompletion) {
        LocalExecClientHandler clientHandler = (LocalExecClientHandler) channel.getPipeline().getLast();
        clientHandler.initExecClient();
        ChannelFuture lastWriteFuture = null;
        String line = delay + " " + command + "\n";
        lastWriteFuture = channel.write(line);
        if (lastWriteFuture != null) {
            if (delay <= 0) {
                lastWriteFuture.awaitUninterruptibly();
            } else {
                lastWriteFuture.awaitUninterruptibly(delay);
            }
        }
        LocalExecResult localExecResult = clientHandler.waitFor(delay * 2);
        result = localExecResult;
        if (futureCompletion == null) {
            return;
        }
        if (result.status == 0) {
            futureCompletion.setSuccess();
            logger.info("Exec OK with {}", command);
        } else if (result.status == 1) {
            logger.warn("Exec in warning with {}", command);
            futureCompletion.setSuccess();
        } else {
            logger.error("Status: " + result.status + " Exec in error with " + command + "\n" + result.result);
            futureCompletion.cancel();
        }
    }

    /**
     * Connect to the Server
     */
    public boolean connect() {
        ChannelFuture future = bootstrapLocalExec.connect(address);
        channel = future.awaitUninterruptibly().getChannel();
        if (!future.isSuccess()) {
            logger.error("Client Not Connected", future.getCause());
            return false;
        }
        return true;
    }

    /**
     * Disconnect from the server
     */
    public void disconnect() {
        channel.close().awaitUninterruptibly();
    }
}
