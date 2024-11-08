package goldengate.ftp.core.control;

import goldengate.common.command.ReplyCode;
import goldengate.common.command.exception.CommandAbstractException;
import goldengate.common.command.exception.Reply503Exception;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;
import goldengate.ftp.core.command.AbstractCommand;
import goldengate.ftp.core.command.FtpCommandCode;
import goldengate.ftp.core.command.internal.ConnectionCommand;
import goldengate.ftp.core.command.internal.IncorrectCommand;
import goldengate.ftp.core.config.FtpInternalConfiguration;
import goldengate.ftp.core.data.FtpTransferControl;
import goldengate.ftp.core.session.FtpSession;
import goldengate.ftp.core.utils.FtpChannelUtils;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * Main Network Handler (Control part) implementing RFC 959, 775, 2389, 2428,
 * 3659 and supports XCRC and XMD5 commands.
 *
 * @author Frederic Bregier
 *
 */
public class NetworkHandler extends SimpleChannelHandler {

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(NetworkHandler.class);

    /**
     * Business Handler
     */
    private final BusinessHandler businessHandler;

    /**
     * Internal store for the SessionInterface
     */
    private final FtpSession session;

    /**
     * The associated Channel
     */
    private Channel controlChannel = null;

    /**
     * Constructor from session
     *
     * @param session
     */
    public NetworkHandler(FtpSession session) {
        super();
        this.session = session;
        businessHandler = session.getBusinessHandler();
        businessHandler.setNetworkHandler(this);
    }

    /**
     * @return the businessHandler
     */
    public BusinessHandler getBusinessHandler() {
        return businessHandler;
    }

    /**
     * @return the session
     */
    public FtpSession getFtpSession() {
        return session;
    }

    /**
     *
     * @return the Control Channel
     */
    public Channel getControlChannel() {
        return controlChannel;
    }

    /**
     * Run firstly executeChannelClosed.
     *
     * @see org.jboss.netty.channel.SimpleChannelHandler#channelClosed(org.jboss.netty.channel.ChannelHandlerContext,
     *      org.jboss.netty.channel.ChannelStateEvent)
     */
    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        if (session == null || session.getDataConn() == null || session.getDataConn().getFtpTransferControl() == null) {
            super.channelClosed(ctx, e);
            return;
        }
        int limit = 100;
        while (session.getDataConn().getFtpTransferControl().isFtpTransferExecuting()) {
            Thread.sleep(10);
            limit--;
            if (limit <= 0) {
                logger.warn("Waiting for transfer finished but 1s is not enough");
                break;
            }
        }
        businessHandler.executeChannelClosed();
        businessHandler.clear();
        session.clear();
        super.channelClosed(ctx, e);
    }

    /**
     * Initialiaze the Handler.
     *
     * @see org.jboss.netty.channel.SimpleChannelHandler#channelConnected(org.jboss.netty.channel.ChannelHandlerContext,
     *      org.jboss.netty.channel.ChannelStateEvent)
     */
    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        Channel channel = e.getChannel();
        controlChannel = channel;
        session.setControlConnected();
        FtpChannelUtils.addCommandChannel(channel, session.getConfiguration());
        if (isStillAlive()) {
            AbstractCommand command = new ConnectionCommand(getFtpSession());
            session.setNextCommand(command);
            businessHandler.executeChannelConnected(channel);
            messageRunAnswer();
            getFtpSession().setReady(true);
        }
    }

    /**
     * If the service is going to shutdown, it sends back a 421 message to the
     * connection
     *
     * @return True if the service is alive, else False if the system is going
     *         down
     */
    private boolean isStillAlive() {
        if (session.getConfiguration().isShutdown) {
            session.setExitErrorCode("Service is going down: disconnect");
            writeFinalAnswer();
            return false;
        }
        return true;
    }

    /**
     * Default exception task: close the current connection after calling
     * exceptionLocalCaught and writing if possible the current replyCode.
     *
     * @see org.jboss.netty.channel.SimpleChannelHandler#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext,
     *      org.jboss.netty.channel.ExceptionEvent)
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        Throwable e1 = e.getCause();
        Channel channel = e.getChannel();
        if (session == null) {
            logger.warn("NO SESSION", e1);
            return;
        }
        if (e1 instanceof ConnectException) {
            ConnectException e2 = (ConnectException) e1;
            logger.warn("Connection impossible since {} with Channel {}", e2.getMessage(), e.getChannel());
        } else if (e1 instanceof ChannelException) {
            ChannelException e2 = (ChannelException) e1;
            logger.warn("Connection (example: timeout) impossible since {} with Channel {}", e2.getMessage(), e.getChannel());
        } else if (e1 instanceof ClosedChannelException) {
            logger.debug("Connection closed before end");
        } else if (e1 instanceof CommandAbstractException) {
            CommandAbstractException e2 = (CommandAbstractException) e1;
            logger.warn("Command Error Reply {}", e2.getMessage());
            session.setReplyCode(e2);
            businessHandler.afterRunCommandKo(e2);
            if (channel.isConnected()) {
                writeFinalAnswer();
            }
            return;
        } else if (e1 instanceof NullPointerException) {
            NullPointerException e2 = (NullPointerException) e1;
            logger.warn("Null pointer Exception", e2);
            try {
                if (session != null) {
                    session.setExitErrorCode("Internal error: disconnect");
                    if (businessHandler != null && session.getDataConn() != null) {
                        businessHandler.exceptionLocalCaught(e);
                        if (channel.isConnected()) {
                            writeFinalAnswer();
                        }
                    }
                }
            } catch (NullPointerException e3) {
            }
            return;
        } else if (e1 instanceof IOException) {
            IOException e2 = (IOException) e1;
            logger.warn("Connection aborted since {} with Channel {}", e2.getMessage(), e.getChannel());
        } else {
            logger.warn("Unexpected exception from downstream" + " Ref Channel: {}" + e.getChannel().toString(), e1.getMessage());
        }
        session.setExitErrorCode("Internal error: disconnect");
        businessHandler.exceptionLocalCaught(e);
        if (channel.isConnected()) {
            writeFinalAnswer();
        }
    }

    /**
     * Simply call messageRun with the received message
     *
     * @see org.jboss.netty.channel.SimpleChannelHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext,
     *      org.jboss.netty.channel.MessageEvent)
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        if (isStillAlive()) {
            while (!session.isReady()) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e1) {
                }
            }
            String message = (String) e.getMessage();
            AbstractCommand command = FtpCommandCode.getFromLine(getFtpSession(), message);
            logger.debug("RECVMSG: {} CMD: {}", message, command.getCommand());
            if (!FtpCommandCode.isSpecialCommand(command.getCode())) {
                boolean notFinished = true;
                FtpTransferControl control = session.getDataConn().getFtpTransferControl();
                for (int i = 0; i < FtpInternalConfiguration.RETRYNB * 100; i++) {
                    if (control.isFtpTransferExecuting() || (!session.isCurrentCommandFinished())) {
                        try {
                            Thread.sleep(FtpInternalConfiguration.RETRYINMS);
                        } catch (InterruptedException e1) {
                            break;
                        }
                    } else {
                        notFinished = false;
                        break;
                    }
                }
                if (notFinished) {
                    session.setReplyCode(ReplyCode.REPLY_503_BAD_SEQUENCE_OF_COMMANDS, "Previous transfer command is not finished yet");
                    businessHandler.afterRunCommandKo(new Reply503Exception(session.getReplyCode().getMesg()));
                    writeIntermediateAnswer();
                    return;
                }
            }
            session.setReplyCode(ReplyCode.REPLY_200_COMMAND_OKAY, null);
            if (session.getCurrentCommand().isNextCommandValid(command)) {
                session.setNextCommand(command);
                messageRunAnswer();
            } else {
                command = new IncorrectCommand();
                command.setArgs(getFtpSession(), message, null, FtpCommandCode.IncorrectSequence);
                session.setNextCommand(command);
                messageRunAnswer();
            }
        }
    }

    /**
     * Write the current answer and eventually close channel if necessary (421
     * or 221)
     *
     * @return True if the channel is closed due to the code
     */
    private boolean writeFinalAnswer() {
        if (session.getReplyCode() == ReplyCode.REPLY_421_SERVICE_NOT_AVAILABLE_CLOSING_CONTROL_CONNECTION || session.getReplyCode() == ReplyCode.REPLY_221_CLOSING_CONTROL_CONNECTION) {
            session.getDataConn().getFtpTransferControl().clear();
            writeIntermediateAnswer().addListener(ChannelFutureListener.CLOSE);
            return true;
        }
        writeIntermediateAnswer();
        session.setCurrentCommandFinished();
        return false;
    }

    /**
     * Write an intermediate Answer from Business before last answer also set by
     * the Business
     *
     * @return the ChannelFuture associated with the write
     */
    public ChannelFuture writeIntermediateAnswer() {
        return Channels.write(controlChannel, session.getAnswer());
    }

    /**
     * Execute one command and write the following answer
     */
    private void messageRunAnswer() {
        boolean error = false;
        try {
            businessHandler.beforeRunCommand();
            AbstractCommand command = session.getCurrentCommand();
            logger.debug("Run {}", command.getCommand());
            command.exec();
            businessHandler.afterRunCommandOk();
        } catch (CommandAbstractException e) {
            error = true;
            session.setReplyCode(e);
            businessHandler.afterRunCommandKo(e);
        }
        if (error || session.getCurrentCommand().getCode() != FtpCommandCode.INTERNALSHUTDOWN) {
            writeFinalAnswer();
        }
    }
}
