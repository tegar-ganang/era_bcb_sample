package org.xaware.server.engine.channel.ftp;

import java.io.IOException;
import java.net.SocketException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.xaware.server.engine.IScopedChannel;
import org.xaware.shared.util.ExceptionMessageHelper;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * This class implements IScopedChannel for FTP. It allows us to keep a
 * connection open as long as necessary, providing access to the FTP client.
 * 
 * @author Vasu Thadaka
 */
public class FTPTemplate implements IScopedChannel {

    /** Name of this class used for logging. */
    private static final String className = FTPTemplate.class.getName();

    /** Logger instance */
    private static final XAwareLogger logger = XAwareLogger.getXAwareLogger(className);

    /** Our Channel object holds properties */
    private FTPChannelSpecification channelObject = null;

    /** The ftp client which we will give out to whoever wants it */
    private FTPClient ftpClient;

    /** Don't initialize more than once */
    private boolean initialized = false;

    /**
	 * Private Constructor.
	 */
    private FTPTemplate() {
    }

    /**
	 * Default Constructor.
	 */
    public FTPTemplate(FTPBizDriver driver) throws XAwareException {
        super();
        channelObject = (FTPChannelSpecification) driver.createChannelObject();
    }

    /**
	 * implement the interface's close() - close open stream
	 * 
	 */
    public void close(boolean success) {
        if (ftpClient != null) {
            try {
                ftpClient.quit();
            } catch (IOException e) {
                logger.finest("Error occured while closing connection:" + e.getMessage());
            }
        }
        initialized = false;
    }

    /**
	 * @return the channelObject
	 */
    public FTPChannelSpecification getChannelObject() {
        return channelObject;
    }

    /**
	 * @return the connector
	 */
    public FTPClient getConnector() throws XAwareException {
        if (!initialized || (ftpClient != null && !ftpClient.isConnected())) {
            initResources();
        }
        return ftpClient;
    }

    /**
	 * implement the interface's getScopedChannelType() Get the scoped channel
	 * type for this scoped channel instance.
	 * 
	 * @return the IScopedChannel.Type for this IScopedChannel instance.
	 */
    public Type getScopedChannelType() {
        return Type.FTP;
    }

    /**
	 * Initialize ourself
	 * 
	 * @throws XAwareException
	 */
    public void initResources() throws XAwareException {
        final String methodName = "initResources";
        if (!initialized) {
            String host = channelObject.getProperty(XAwareConstants.BIZDRIVER_HOST);
            if (host == null || host.trim().length() == 0) {
                throw new XAwareException("xa:host must be specified.");
            }
            String portString = channelObject.getProperty(XAwareConstants.BIZDRIVER_PORT);
            if (portString == null || portString.trim().length() == 0) {
                throw new XAwareException("xa:port must be specified.");
            }
            int port = 0;
            try {
                port = Integer.parseInt(portString);
            } catch (Exception exception) {
                throw new XAwareException("xa:port must be numeric.");
            }
            String remoteVerification = channelObject.getProperty(XAwareConstants.XAWARE_FTP_REMOTE_VERIFICATION);
            String userName = channelObject.getProperty(XAwareConstants.BIZDRIVER_USER);
            String password = channelObject.getProperty(XAwareConstants.BIZDRIVER_PWD);
            String proxyUser = channelObject.getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYUSER);
            String proxyPassword = channelObject.getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYPASSWORD);
            ftpClient = new FTPClient();
            logger.finest("Connecting to host:" + host + " Port:" + port, className, methodName);
            try {
                ftpClient.connect(host, port);
                if (remoteVerification != null && remoteVerification.trim().equals(XAwareConstants.XAWARE_YES)) {
                    ftpClient.setRemoteVerificationEnabled(true);
                } else {
                    ftpClient.setRemoteVerificationEnabled(false);
                }
                final int reply = ftpClient.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftpClient.disconnect();
                    String errorMessage = "FTP server refused connection. Error Code:" + reply;
                    logger.severe(errorMessage, className, methodName);
                    throw new XAwareException(errorMessage);
                }
                logger.finest("Logging in User:" + userName + " PWD:" + password, className, methodName);
                if (!ftpClient.login(userName, password)) {
                    ftpClient.logout();
                    String errorMessage = "Error logging into server. Please check credentials.";
                    logger.severe(errorMessage, className, methodName);
                    throw new XAwareException(errorMessage);
                }
                if (proxyUser != null && proxyUser.trim().length() > 0) {
                    logger.finest("Logging in again proxy UID:" + proxyUser + " proxy password:" + proxyPassword, className, methodName);
                    if (!ftpClient.login(proxyUser, proxyPassword)) {
                        ftpClient.logout();
                        String errorMessage = "Error logging using proxy user/pwd. Please check proxy credentials.";
                        logger.severe(errorMessage, className, methodName);
                        throw new XAwareException(errorMessage);
                    }
                }
            } catch (SocketException e) {
                String errorMessage = "SocketException: " + ExceptionMessageHelper.getExceptionMessage(e);
                logger.severe(errorMessage, className, methodName);
                throw new XAwareException(errorMessage, e);
            } catch (IOException e) {
                String errorMessage = "IOException: " + ExceptionMessageHelper.getExceptionMessage(e);
                logger.severe(errorMessage, className, methodName);
                throw new XAwareException(errorMessage, e);
            }
            logger.finest("Connected to host:" + host + " Port:" + port, className, methodName);
            initialized = true;
        }
    }
}
