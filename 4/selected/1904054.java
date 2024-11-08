package goldengate.commandexec.client;

import goldengate.commandexec.utils.LocalExecDefaultResult;
import goldengate.commandexec.utils.LocalExecResult;
import goldengate.common.future.GgFuture;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * Handles a client-side channel for LocalExec
 *
 *
 */
public class LocalExecClientHandler extends SimpleChannelUpstreamHandler {

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(LocalExecClientHandler.class);

    private LocalExecResult result;

    private StringBuilder back;

    private boolean firstMessage = true;

    private GgFuture future;

    protected LocalExecClientPipelineFactory factory = null;

    /**
     * Constructor
     */
    public LocalExecClientHandler(LocalExecClientPipelineFactory factory) {
        this.factory = factory;
    }

    /**
     * Initialize the client status for a new execution
     */
    public void initExecClient() {
        this.result = new LocalExecResult(LocalExecDefaultResult.NoStatus);
        this.back = new StringBuilder();
        this.firstMessage = true;
        this.future = new GgFuture(true);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        initExecClient();
        factory.addChannel(ctx.getChannel());
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        this.factory.removeChannel(e.getChannel());
    }

    /**
     * When closed, <br>
     * If no messaged were received => NoMessage error is set to future<br>
     * Else if an error was detected => Set the future to error (with or without exception)<br>
     * Else if no error occurs => Set success to the future<br>
     *
     *
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelClosed(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
     */
    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        logger.debug("ChannelClosed");
        if (!future.isDone()) {
            finalizeMessage();
        }
        super.channelClosed(ctx, e);
    }

    /**
     * Finalize a message
     */
    private void finalizeMessage() {
        if (firstMessage) {
            logger.warn(result.status + " " + result.result);
            result.set(LocalExecDefaultResult.NoMessage);
        } else {
            result.result = back.toString();
        }
        if (result.status < 0) {
            if (result.exception != null) {
                this.future.setFailure(result.exception);
            } else {
                this.future.cancel();
            }
        } else {
            this.future.setSuccess();
        }
    }

    /**
     * Waiting for the close of the exec
     * @return The LocalExecResult
     */
    public LocalExecResult waitFor(long delay) {
        if (delay <= 0) {
            this.future.awaitUninterruptibly();
        } else {
            this.future.awaitUninterruptibly(delay);
        }
        result.isSuccess = this.future.isSuccess();
        return result;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        String mesg = (String) e.getMessage();
        if (firstMessage) {
            firstMessage = false;
            int pos = mesg.indexOf(' ');
            try {
                result.status = Integer.parseInt(mesg.substring(0, pos));
            } catch (NumberFormatException e1) {
                result.set(LocalExecDefaultResult.BadTransmition);
                back.append(mesg);
                ctx.getChannel().close();
                return;
            }
            mesg = mesg.substring(pos + 1);
            result.result = mesg;
            back.append(mesg);
        } else if (LocalExecDefaultResult.ENDOFCOMMAND.startsWith(mesg)) {
            logger.debug("Receive End of Command");
            this.finalizeMessage();
        } else {
            back.append('\n');
            back.append(mesg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        logger.error("Unexpected exception from downstream while get information: " + firstMessage, e.getCause());
        if (firstMessage) {
            firstMessage = false;
            result.set(LocalExecDefaultResult.BadTransmition);
            result.exception = (Exception) e.getCause();
            back = new StringBuilder("Error in LocalExec: ");
            back.append(result.exception.getMessage());
            back.append('\n');
        } else {
            back.append("\nERROR while receiving answer: ");
            result.exception = (Exception) e.getCause();
            back.append(result.exception.getMessage());
            back.append('\n');
        }
        e.getChannel().close();
    }
}
