package openr66.context.task.localexec;

import goldengate.commandexec.client.LocalExecClientHandler;
import goldengate.commandexec.client.LocalExecClientPipelineFactory;
import goldengate.commandexec.utils.LocalExecResult;
import goldengate.common.future.GgFuture;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;
import java.net.InetSocketAddress;
import openr66.protocol.configuration.Configuration;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

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

    /**
     * Initialize the LocalExec Client context
     */
    public static void initialize() {
        bootstrapLocalExec = new ClientBootstrap(new NioClientSocketChannelFactory(Configuration.configuration.getLocalPipelineExecutor(), Configuration.configuration.getLocalPipelineExecutor()));
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
    public void runOneCommand(String command, long delay, boolean waitFor, GgFuture futureCompletion) {
        LocalExecClientHandler clientHandler = (LocalExecClientHandler) channel.getPipeline().getLast();
        clientHandler.initExecClient();
        ChannelFuture lastWriteFuture = null;
        String line = delay + " " + command + "\n";
        lastWriteFuture = channel.write(line);
        if (!waitFor) {
            futureCompletion.setSuccess();
            logger.info("Exec OK with {}", command);
        }
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
            if (waitFor) {
                futureCompletion.setSuccess();
            }
            logger.info("Exec OK with {}", command);
        } else if (result.status == 1) {
            logger.warn("Exec in warning with {}", command);
            if (waitFor) {
                futureCompletion.setSuccess();
            }
        } else {
            logger.error("Status: " + result.status + " Exec in error with " + command + "\n" + result.result);
            if (waitFor) {
                futureCompletion.cancel();
            }
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
