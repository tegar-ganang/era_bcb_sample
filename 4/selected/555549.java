package org.demis.kobold.fetcher;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.demis.kobold.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.SocketException;
import java.util.Calendar;
import javax.net.ssl.SSLServerSocketFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;

/**
 * @version 1.0
 * @author <a href="mailto:demis27@demis27.net">Stéphane kermabon</a>
 */
public class POP3FetcherImpl implements POP3Fetcher {

    private final Log logger = LogFactory.getLog(POP3FetcherImpl.class);

    private FetcherConfiguration configuration = null;

    private POP3Client pop3;

    public POP3FetcherImpl() {
    }

    @Override
    public Message[] fetch() throws FetcherException {
        try {
            connectToServer();
            login();
            Message[] messages = readMessages();
            logout();
            disconnectToServer();
            return messages;
        } catch (FetcherException ex) {
            throw new FetcherException(ex.getMessage(), ex.getCause(), ex.getCode());
        } finally {
            pop3 = null;
        }
    }

    @Override
    public void delete(int id) throws FetcherException {
        try {
            connectToServer();
            login();
            deleteMessage(id);
            logout();
            disconnectToServer();
        } catch (FetcherException ex) {
            throw new FetcherException(ex.getMessage(), ex.getCause(), ex.getCode());
        } finally {
            pop3 = null;
        }
    }

    @Override
    public void setConfiguration(FetcherConfiguration configuration) {
        this.configuration = configuration;
    }

    private void connectToServer() throws FetcherException {
        if (configuration.getMailbox() == null || configuration.getMailServer() == null || configuration.getMailServer().getHost() == null) {
            logger.error("no mail server and mailbox are configure");
            throw new FetcherException(FetcherExceptionCode.BAD_FETCHER_CONFIGURATION);
        }
        pop3 = new POP3Client();
        pop3.setServerSocketFactory(SSLServerSocketFactory.getDefault());
        pop3.setDefaultTimeout(60000);
        try {
            pop3.connect(configuration.getMailServer().getHost(), configuration.getMailServer().getPort());
        } catch (SocketException ex) {
            logger.error("Error when try to connect to Pop3 server " + configuration.getMailServer().toString());
            pop3 = null;
            throw new FetcherException(ex, FetcherExceptionCode.SERVER_CONNECTION_FAILED);
        } catch (IOException ex) {
            logger.error("Error when try to connect to Pop3 server " + configuration.getMailServer().toString());
            pop3 = null;
            throw new FetcherException(ex, FetcherExceptionCode.SERVER_CONNECTION_FAILED);
        }
        logger.info("Connect to POP3 server " + configuration.getMailServer().getHost());
    }

    private void deleteMessage(int id) throws FetcherException {
        try {
            pop3.deleteMessage(id);
        } catch (IOException ex) {
            logger.error("Error when try to delete message #" + id);
            throw new FetcherException(ex, FetcherExceptionCode.SERVER_CONNECTION_FAILED);
        }
    }

    private void login() throws FetcherException {
        if (pop3 == null) {
            logger.error("Error you are not connected to pop3 server " + configuration.getMailServer().toString());
            throw new FetcherException(FetcherExceptionCode.NOT_CONNECTED);
        }
        try {
            pop3.login(configuration.getMailbox().getLogin(), configuration.getMailbox().getPassword());
        } catch (IOException ex) {
            logger.error("Error when try to login to mailbox " + configuration.getMailbox().toString());
            throw new FetcherException(ex, FetcherExceptionCode.MAILBOX_LOGIN_ERROR);
        }
        logger.info("Login to POP3 server " + configuration.getMailServer().getHost() + " with login " + configuration.getMailbox().getLogin());
    }

    private synchronized Message[] readMessages() throws FetcherException {
        if (pop3 == null) {
            logger.error("Error you are not connected to pop3 server " + configuration.getMailServer().toString());
            throw new FetcherException(FetcherExceptionCode.NOT_CONNECTED);
        }
        Message[] messagesResult = null;
        try {
            POP3MessageInfo[] messages = pop3.listMessages();
            if (messages != null) {
                messagesResult = new Message[messages.length];
                logger.info("They are " + messages.length + " message(s) on configuration.getMailbox() ");
                for (int i = 0; i < Math.min(configuration.getMaxFetchedMessage(), messages.length); i++) {
                    POP3MessageInfo message = messages[i];
                    logger.debug("Reading message " + message.number + " on configuration.getMailbox() ");
                    Reader reader = pop3.retrieveMessage(message.number);
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    char[] buffer = new char[4096];
                    int read = 0;
                    while ((read = reader.read(buffer)) >= 0) {
                        String readString = new String(buffer, 0, read);
                        output.write(readString.getBytes());
                        logger.trace("read " + read + " chars : " + readString);
                    }
                    reader.close();
                    output.close();
                    Message messageResult = new MessageImpl();
                    messageResult.setBinaryContent(output.toByteArray());
                    Calendar calendar = Calendar.getInstance();
                    messageResult.setReceivedTime(calendar.getTimeInMillis());
                    messageResult.setId(message.number);
                    messagesResult[message.number - 1] = messageResult;
                }
                if (configuration.isDeleteAfterFetch()) {
                    for (POP3MessageInfo message : messages) {
                        logger.debug("Deleting message " + message.number + " on configuration.getMailbox() ");
                        pop3.deleteMessage(message.number);
                    }
                }
            } else {
                logger.info("They are no message on configuration.getMailbox() ");
            }
        } catch (IOException ex) {
            logger.error("Error when try to read mailbox messages " + configuration.getMailbox().toString());
            throw new FetcherException(ex, FetcherExceptionCode.MAILBOX_LOGIN_ERROR);
        }
        return messagesResult;
    }

    private void logout() throws FetcherException {
        if (pop3 == null) {
            logger.error("Error you are not connected to pop3 server " + configuration.getMailServer().toString());
            throw new FetcherException(FetcherExceptionCode.NOT_CONNECTED);
        }
        try {
            pop3.logout();
        } catch (IOException ex) {
            logger.error("Error when try to logout mailserver " + configuration.getMailServer().toString());
            throw new FetcherException(ex, FetcherExceptionCode.MAILBOX_LOGOUT_ERROR);
        }
        logger.info("Logout to POP3 server " + configuration.getMailServer().getHost() + " with login " + configuration.getMailbox().getLogin());
    }

    private void disconnectToServer() throws FetcherException {
        if (pop3 == null) {
            logger.error("Error you are not connected to pop3 server " + configuration.getMailServer().toString());
            throw new FetcherException(FetcherExceptionCode.NOT_CONNECTED);
        }
        try {
            pop3.disconnect();
            pop3 = null;
        } catch (IOException ex) {
            logger.error("Error when try to disconnect to mailserver " + configuration.getMailServer().toString());
            throw new FetcherException(ex, FetcherExceptionCode.SERVER_DISCONNECTION_FAILED);
        }
        logger.info("Disconnect to POP3 server " + configuration.getMailServer().getHost());
    }
}
