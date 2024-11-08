package goldengate.ftp.core.command.internal;

import goldengate.common.command.ReplyCode;
import goldengate.common.command.exception.Reply500Exception;
import goldengate.common.command.exception.Reply501Exception;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;
import goldengate.ftp.core.command.AbstractCommand;
import goldengate.ftp.core.config.FtpConfiguration;
import goldengate.ftp.core.utils.FtpChannelUtils;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;

/**
 * Internal shutdown command that will shutdown the FTP service with a password
 *
 * @author Frederic Bregier
 *
 */
public class INTERNALSHUTDOWN extends AbstractCommand {

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(INTERNALSHUTDOWN.class);

    /**
     *
     * @author Frederic Bregier
     *
     */
    private class ShutdownChannelFutureListener implements ChannelFutureListener {

        private final FtpConfiguration configuration;

        protected ShutdownChannelFutureListener(FtpConfiguration configuration) {
            this.configuration = configuration;
        }

        public void operationComplete(ChannelFuture arg0) throws Exception {
            Channels.close(arg0.getChannel());
            FtpChannelUtils.teminateServer(configuration);
        }
    }

    public void exec() throws Reply501Exception, Reply500Exception {
        if (!getSession().getAuth().isAdmin()) {
            throw new Reply500Exception("Command Not Allowed");
        }
        if (!hasArg()) {
            throw new Reply501Exception("Shutdown Need password");
        }
        String password = getArg();
        if (!getConfiguration().checkPassword(password)) {
            throw new Reply501Exception("Shutdown Need a correct password");
        }
        logger.warn("Shutdown...");
        getSession().setReplyCode(ReplyCode.REPLY_221_CLOSING_CONTROL_CONNECTION, "System shutdown");
        getSession().getNetworkHandler().writeIntermediateAnswer().addListener(new ShutdownChannelFutureListener(getConfiguration()));
    }
}
