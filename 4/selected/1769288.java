package org.codemonkey.swiftsocketserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Worker thread for the server that is dedicated to a single client.
 * <p>
 * Sets up a child thread that listens for client input, while pinging the client to test connection. Since Java cannot detect if a client
 * closes the TCP connection, we manually test the connection by pinging the client. If the client does not respond with
 * {@link #pingPongTimeoutMs}, the server assumes the connection went dead.
 * 
 * @author Benny Bottema
 * @see InputHandler
 * @see SwiftSocketServer
 * @see #checkPing()
 * @see #checkPong()
 * @since 1.0
 */
public class ClientHandler implements Runnable {

    /**
	 * The length of the message id of a message string (binary packet).
	 */
    protected static final int MESSAGEID_LENGTH = 3;

    /**
	 * Interval in milliseconds for ping messages to the client.
	 * 
	 * @see #checkPing()
	 * @see #checkPong()
	 */
    private int pingPongIntervalMs;

    /**
	 * Timeout in milliseconds for a pong message to the client.
	 * 
	 * @see #checkPing()
	 * @see #checkPong()
	 */
    private int pingPongTimeoutMs = 5000;

    /**
	 * Basic logger to log events such as client timeouts and other client related events.
	 */
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class);

    /**
	 * The {@link SwiftSocketServer} instance used to check server status and acquire {@link ClientMessageToServer} messages from.
	 */
    private final SwiftSocketServer server;

    /**
	 * The {@link ClientContext} containing all session data concerning the client. Also contains a session data container for external use
	 * only. External users can utilize this container to store client related data for the duration of the connection.
	 */
    private final ClientContext clientContext;

    private final boolean pingPongMode;

    /**
	 * Constructor, which accepts a {@link SwiftSocketServer} instance and a client {@link ClientContext}. Also resets
	 * {@link ClientContext#getPingtime()} and {@link ClientContext#isPongReceived()} flags.
	 * 
	 * @param server Server used to check on the server status (ie. server is stopping).
	 * @param pingPongMode Indicates whether to perform PingPong polling to the client to detect drop outs.
	 * @param clientContext The context containing the client socket to which this worker thread is dedicated to.
	 * @param pingPongIntervalMs The Ping Pong interval used if {@link #pingPongMode} is turned on.
	 * @param pingPongTimeoutMs The Ping Pong timeout used if {@link #pingPongMode} is turned on.
	 */
    protected ClientHandler(final SwiftSocketServer server, final ClientContext clientContext, final boolean pingPongMode, int pingPongIntervalMs, int pingPongTimeoutMs) {
        this.server = server;
        this.clientContext = clientContext;
        this.pingPongMode = pingPongMode;
        this.pingPongIntervalMs = pingPongIntervalMs;
        this.pingPongTimeoutMs = pingPongTimeoutMs;
    }

    /**
	 * Thread method that runs ping messages to the client and tests for timeouts. Also sets up client thread that waits for input; a new
	 * thread is used since this action is blocking and the thread 'sleeps' until input is available, which would deny us the opportunity to
	 * do ping messages.<br />
	 * <br />
	 * Keeps pinging the client while the server is running.
	 * 
	 * @see java.lang.Runnable#run()
	 * @see #checkPing()
	 * @see #checkPong()
	 * @see SwiftSocketServer#isRunning()
	 */
    @Override
    public final void run() {
        final Thread inputThread = new Thread(new InputHandler());
        inputThread.start();
        if (pingPongMode) {
            ping();
        }
        while (server.isRunning()) {
            if (!clientContext.isClientSaidByeBye()) {
                if (pingPongMode) {
                    if (checkPong()) {
                        checkPing();
                    } else {
                        LOGGER.debug(String.format("dropping client %s, reason: timeout", clientContext));
                        server.disposeOfClient(clientContext);
                        break;
                    }
                } else {
                }
                ServerUtil.defaultSleep();
            } else {
                LOGGER.debug(String.format("dropping client %s, reason: client went away", clientContext.getClientInetAddress()));
                break;
            }
        }
    }

    /**
	 * Sends a ping message to the client, which on turn should respond with a 'pong' notification. A ping is sent only if the following
	 * requirements are met:
	 * <ol>
	 * <li>A pong response has been received; we don't want to send out multiple ping messages</li>
	 * <li>The last ping message time stamp exceeds {@link #PING_INTERVAL}</li>
	 * </ol>
	 * 
	 * @see #ping()
	 * @see #pong()
	 * @see ClientContext#getPingtime()
	 * @see ClientContext#isPongReceived()
	 * @see #PINGPONG_TIMEOUT
	 * @see #PING_INTERVAL
	 * @see #checkPong()
	 */
    private void checkPing() {
        final long pongDelay = new Date().getTime() - clientContext.getPingtime();
        if (clientContext.isPongReceived() && pongDelay > pingPongIntervalMs) {
            ping();
        }
    }

    /**
	 * Evaluates whether the client has sent the last pong message, or else if the client has still time to respond with a pong
	 * notification. In other words: "returns that the client hasn't timed out yet".
	 * 
	 * @return Whether pong hasn't timed out yet.
	 * @see #ping()
	 * @see #pong()
	 * @see ClientContext#getPingtime()
	 * @see ClientContext#isPongReceived()
	 * @see #PINGPONG_TIMEOUT
	 * @see #PING_INTERVAL
	 * @see #checkPing()
	 */
    private boolean checkPong() {
        return clientContext.isPongReceived() || new Date().getTime() - clientContext.getPingtime() < pingPongTimeoutMs;
    }

    /**
	 * Performs a ping message to the client, by using a {@link ServerMessageToClientPingPong}. Resets the response flags so we can keep
	 * track of the time between the ping message and the pong notification.
	 * 
	 * @see ClientContext#getPingtime()
	 * @see ClientContext#isPongReceived()
	 */
    private void ping() {
        sendMessage(new ServerMessageToClientPingPong(null));
        clientContext.setPingtime(new Date().getTime());
        clientContext.setPongReceived(false);
    }

    /**
	 * Invoked by {@link ClientMessageToServerPingPong#execute(ClientHandler)}, when the client is responding with a <em>pong</em>
	 * notification. Sets {@link ClientContext#setPongReceived(boolean)} to <code>true</code>, which is used to determine <em>ping</em>
	 * timeout with.
	 * 
	 * @see ClientContext#setPongReceived(boolean)
	 * @see #checkPong()
	 * @see ClientMessageToServerPingPong
	 */
    public final void pong() {
        clientContext.setPongReceived(true);
    }

    /**
	 * Called by {@link ClientMessageToServerByeBye#execute(ClientHandler)}. Set the 'Bye Bye' lag on the {@link #clientContext} and calls
	 * {@link ClientEndpoint#close()}.
	 */
    protected void clientSaysByeBye() {
        server.disposeOfClient(clientContext);
        try {
            clientContext.getClientEndpoint().close();
        } catch (final IOException e) {
            throw new RuntimeException("error closing socket (already closed?)", e);
        }
    }

    /**
	 * Logs and sends a response message to the current client using {@link #clientContext}, delegating the actual sending to
	 * {@link #sendMessage(ClientContext, int, ServerMessageToClient)}, unless the client has said 'Bye Bye' in which case the message to be
	 * sent is ignored altogether.
	 * 
	 * @param message The server message to send to the client.
	 * @see ClientContext#isClientSaidByeBye()
	 * @see #sendMessage(ClientContext, int, ServerMessageToClient)
	 */
    public void sendMessage(final ServerMessageToClient message) {
        final Logger messageLogger = determineLogger(message);
        messageLogger.debug(String.format("sending message to %s: %s", clientContext.getClientInetAddress(), message.getClass().getSimpleName()));
        if (!clientContext.isClientSaidByeBye()) {
            final int messageId = server.getServerMessageId(message.getClass());
            sendMessage(clientContext, messageId, message);
        } else {
            Logger availableLogger = LOGGER;
            if (!LOGGER.isDebugEnabled() && messageLogger.isDebugEnabled()) {
                availableLogger = messageLogger;
            }
            availableLogger.debug(String.format("dropping message %s, reason: client already said byebye", message));
        }
    }

    /**
	 * Helper method used to send a message to the client.
	 * 
	 * @param clientContext The client to send the message to.
	 * @param messageId The message id, known to both the server and the client.
	 * @param message The {@link ClientMessageToServer} used to physically encode the message as <code>String</code>.
	 */
    private void sendMessage(final ClientContext clientContext, final int messageId, final ServerMessageToClient message) {
        final String paddedMessageId = StringUtils.leftPad(String.valueOf(messageId), MESSAGEID_LENGTH, '0');
        final String datagram = String.format("%s%s\n\0", paddedMessageId, message.encode());
        try {
            clientContext.getClientEndpoint().send(datagram);
        } catch (final IOException e) {
            throw new RuntimeException(String.format("unable to send message to client %s", clientContext.getClientInetAddress()), e);
        }
    }

    /**
	 * Returns the class logger {@link #LOGGER} in case of user defined message types, or a message type specific logger if it is a native
	 * message type. This is so that we're able to differentiate log levels between Ping Pong messages (to mute them effectively) and other
	 * messages we'd still like to see in out logging.
	 * 
	 * @param object The object doing the logging.
	 * @return The correct logger to avoid using log4j on end user classes (who may not support log4j and get due warnings) and still be
	 *         able to mute specific native message types, such as {@link ClientMessageToServerPingPong}.
	 */
    private Logger determineLogger(final Object object) {
        if (object.getClass().getPackage() == ClientHandler.class.getPackage()) {
            return Logger.getLogger(object.getClass());
        } else {
            return LOGGER;
        }
    }

    /**
	 * @return The {@link #clientContext} containing session data for external use.
	 */
    public ClientContext getClientContext() {
        return clientContext;
    }

    /**
	 * Listens to client and converts input to objects of type {@link ClientMessageToServer}. A thread is being used for this, since reading
	 * input is blocking, meaning the thread 'sleeps' while waiting for input. These messages are then added to the client messages queue
	 * for execution by external user.<br />
	 * <strong>Note:</strong> some messages are filtered by the server as they are of server level, such as pong messages.<br />
	 * <br />
	 * Reuses fields from {@link ClientHandler}, such as {@link ClientHandler#clientContext} and {@link ClientHandler#server}. Keeps
	 * listening for input while the server is still running and the client hasn't timed out.
	 * 
	 * @author Benny Bottema
	 * @since 1.0
	 */
    private class InputHandler implements Runnable {

        private static final String ERROR_SOCKET_CLOSED = "socket closed";

        private static final String ERROR_RECV_FAILED = "Software caused connection abort: recv failed";

        private static final String ERROR_CONNECTION_RESET = "Connection reset";

        /**
		 * Thread method that waits for input on the client {@link Socket}. Keeps waiting for input while the server is running and no
		 * PingPong timeout has occurred or the client has gone away.<br />
		 * <br />
		 * The input data is read per line, so messages need to end with a newline character '\n'. Since clients need to conform to the
		 * common denominator, they are required to append a zero (0) byte '\0' as per Flash clients. This character is appended to the
		 * newline '\n' character and needs to be discarded manually using a {@link BufferedReader#read()}.
		 * 
		 * @see SwiftSocketServer#isRunning()
		 * @see ClientHandler#checkPong()
		 */
        @Override
        public void run() {
            while (server.isRunning() && (!pingPongMode || checkPong()) && !clientContext.isClientSaidByeBye()) {
                try {
                    final String line = clientContext.getClientEndpoint().readLine();
                    if (server.isRunning()) {
                        if (line != null) {
                            clientContext.getClientEndpoint().read();
                            handleMessage(clientContext, line);
                        } else {
                            LOGGER.debug(String.format("client %s closed connection non-gracefully", clientContext.getClientInetAddress()));
                            server.disposeOfClient(clientContext);
                            break;
                        }
                    }
                } catch (final SocketException e) {
                    if (e.getMessage().equals(ERROR_SOCKET_CLOSED) || e.getMessage().equals(ERROR_RECV_FAILED) || e.getMessage().equals(ERROR_CONNECTION_RESET)) {
                        if (e.getMessage().equals(ERROR_SOCKET_CLOSED)) {
                            final String msg = "tried to read or write to closed socket (was client %s dropped due to PingPong time-out?)";
                            LOGGER.error(String.format(msg, clientContext.getClientInetAddress()));
                        } else {
                            final String msg = "socket in error state [%s] for client %s";
                            LOGGER.error(String.format(msg, e.getMessage(), clientContext.getClientInetAddress()));
                        }
                        server.disposeOfClient(clientContext);
                        break;
                    }
                    LOGGER.error(e.getMessage(), e);
                } catch (final IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                ServerUtil.defaultSleep();
            }
        }

        /**
		 * Handles a single client message string. Creates a {@link ClientMessageToServer} and adds this to the server message queue unless
		 * it is a server level message, such as a 'pong' notification, in which case the message is executed directly.
		 * 
		 * @param clientContext The client that sent the message being interpreted.
		 * @param datagramMessage The client message string, minus newline character '\n' or zero (0) byte '\0'.
		 * @see #createDecodedExecutableMessage(ClientContext, String)
		 * @see SwiftSocketServer#addClientMessage(ClientMessageToServer)
		 */
        public void handleMessage(final ClientContext clientContext, final String datagramMessage) {
            final ClientMessageToServer<?> message = createDecodedExecutableMessage(clientContext, datagramMessage);
            determineLogger(message).debug(String.format("receiving message from %s: %s", clientContext, message.getClass().getSimpleName()));
            if (message instanceof ClientMessageToServerPingPong) {
                ((ClientMessageToServerPingPong) message).execute(ClientHandler.this);
            } else if (message instanceof ClientMessageToServerByeBye) {
                ((ClientMessageToServerByeBye) message).execute(ClientHandler.this);
            } else {
                server.addClientMessage(message);
            }
        }

        /**
		 * Parses the raw client message into a message id and message content. Passes both values to
		 * {@link #createDecodedExecutableMessage(ClientContext, int, String)} and returns the result. The first three bytes of the raw
		 * message denote the message code.
		 * <p>
		 * In case anything goes wrong, a {@link ClientMessageToServerInvalid} object is returned instead which generates some useful
		 * message for logging purposes. The server user can choose to filter out unknown messages at its own discretion. If a
		 * <code>ClientMessageToServerInvalid</code> is executed however, it will throw a runtime exception with the original message.
		 * 
		 * @param clientMessage The client input string that represents a single message.
		 * @return {@link #createDecodedExecutableMessage(ClientContext, int, String)}
		 * @see #createInvalidMessage(String, Exception, String)
		 */
        protected ClientMessageToServer<?> createDecodedExecutableMessage(final ClientContext clientContext, final String clientMessage) {
            final int messageId;
            try {
                messageId = Integer.parseInt(clientMessage.substring(0, MESSAGEID_LENGTH));
            } catch (final NumberFormatException e) {
                return createInvalidMessage(clientMessage, e, "invalid message identifier");
            }
            final String messageContent = clientMessage.substring(MESSAGEID_LENGTH);
            return createDecodedExecutableMessage(clientContext, messageId, messageContent);
        }

        /**
		 * Creates a {@link ClientMessageToServer} instance based on the message id and message content.
		 * 
		 * @param clientContext The client that sent the message, useful to be able to reply to.
		 * @param messageId The identifier for the message type we need to instantiate.
		 * @param messageContent The message content for the solved message type to decode.
		 * @return A {@link ClientMessageToServer} with identified by <code>messageId</code> decoded from <code>messageContent</code>.
		 * @see SwiftSocketServer#getClientMessageToServerType(int)
		 * @see ClientMessageToServer#decode(String)
		 */
        private final ClientMessageToServer<?> createDecodedExecutableMessage(final ClientContext clientContext, final int messageId, final String messageContent) {
            final Class<? extends ClientMessageToServer<?>> messageType = server.getClientMessageToServerType(messageId);
            ClientMessageToServer<?> message;
            try {
                message = messageType.getConstructor(ClientContext.class).newInstance(clientContext);
            } catch (final NoSuchMethodException e) {
                final String errorMessage = "message type '%s' has no valid constructor";
                return createInvalidMessage(messageContent, e, String.format(errorMessage, messageType.getSimpleName()));
            } catch (final IllegalAccessException e) {
                return createInvalidMessage(messageContent, e, "constructor not visible for message");
            } catch (final InvocationTargetException e) {
                return createInvalidMessage(messageContent, e, "error calling message constructor");
            } catch (final InstantiationException e) {
                return createInvalidMessage(messageContent, e, "unable to construct message");
            }
            message.decode(messageContent);
            return message;
        }

        /**
		 * Generates a {@link ClientMessageToServerInvalid} object with the failure cause. When executed, this message throws a runtime
		 * exception with the original exception.
		 * 
		 * @param originalMessage The original message
		 * @param failureCause The exception that occurred when the original messages was being converted into a
		 *            {@link ClientMessageToServer}.
		 * @param cause A fine grained exception description when the exception <code>failureCause</code> occurred.
		 * @return An instance of {@link ClientMessageToServerInvalid}.
		 */
        private ClientMessageToServer<?> createInvalidMessage(final String originalMessage, final Exception failureCause, final String cause) {
            final ClientMessageToServerInvalid message = new ClientMessageToServerInvalid(clientContext, failureCause);
            message.decode(String.format("%s (original message: '%s')", cause, originalMessage));
            return message;
        }
    }
}
