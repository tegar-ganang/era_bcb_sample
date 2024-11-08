package org.codemonkey.swiftsocketclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Worker thread for the client server that is dedicated to the singular server.
 * <p>
 * Sets up a child thread that listens for server input, while sending Pong response messages if required and handling messages sent to the
 * server.
 * 
 * @author Benny Bottema
 * @see SwiftSocketClient
 * @see #pong()
 * @since 1.0
 */
public class ServerHandler implements Runnable {

    /**
	 * Basic logger to log events such as client timeouts and other client related events.
	 */
    private static final Logger LOGGER = Logger.getLogger(ServerHandler.class);

    /**
	 * The length of the message id's of a message string (binary packet).
	 */
    protected static final int MESSAGEID_LENGTH = 3;

    private static final String ERROR_SOCKET_CLOSED = "socket closed";

    private static final String ERROR_RECV_FAILED = "Software caused connection abort: recv failed";

    private static final String ERROR_CONNECTION_RESET = "Connection reset";

    /**
	 * The {@link SwiftSocketClient} instance used to acquire {@link ClientMessageToServer} messages from.
	 */
    private final SwiftSocketClient client;

    /**
	 * The {@link ServerContext} containing all session data concerning the server. Also contains a session data container for external use
	 * only. External users can utilize this container to store server related data for the duration of the connection.
	 */
    private final ServerContext serverContext;

    /**
	 * Constructor, which accepts a {@link SwiftSocketClient} instance and a client {@link ServerContext}.
	 * 
	 * @param client Client used to check on the client status (ie. client is stopping).
	 * @param serverContext The context containing the server socket to which this worker thread is dedicated to.
	 */
    protected ServerHandler(final SwiftSocketClient client, final ServerContext serverContext) {
        this.client = client;
        this.serverContext = serverContext;
    }

    /**
	 * Logs and sends a message to the server using {@link #serverContext}, delegating the actual sending to
	 * {@link #sendResponse(ServerContext, int, ClientMessageToServer)}, unless the client has said 'Bye Bye' in which case the message to
	 * be sent is ignored altogether.
	 * 
	 * @param message The client message to send to the server.
	 * @see ServerContext#isServerSaidByeBye()
	 * @see #sendResponse(ServerContext, int, ClientMessageToServer)
	 */
    public void sendMessage(final ClientMessageToServer message) {
        final Logger messageLogger = determineLogger(message);
        messageLogger.debug(String.format("sending message to %s: %s", serverContext.getServerInetAddress(), message.getClass().getSimpleName()));
        if (!serverContext.isServerSaidByeBye()) {
            final int messageId = client.getClientMessageId(message.getClass());
            sendResponse(serverContext, messageId, message);
        } else {
            Logger availableLogger = LOGGER;
            if (!LOGGER.isDebugEnabled() && messageLogger.isDebugEnabled()) {
                availableLogger = messageLogger;
            }
            availableLogger.debug(String.format("dropping message %s, reason: server already said byebye", message));
        }
    }

    /**
	 * Helper method used to send a message to the server.
	 * 
	 * @param serverContext The server to send the message to.
	 * @param messageId The message id, known to both the server and the client.
	 * @param message The {@link ClientMessageToServer} used to physically encode the message as <code>String</code>.
	 */
    private void sendResponse(final ServerContext serverContext, final int messageId, final ClientMessageToServer message) {
        final String paddedMessageId = StringUtils.leftPad(String.valueOf(messageId), MESSAGEID_LENGTH, '0');
        final String datagram = String.format("%s%s\n\0", paddedMessageId, message.encode());
        try {
            serverContext.getServerEndpoint().send(datagram);
        } catch (final IOException e) {
            throw new RuntimeException(String.format("unable to send message to client %s", serverContext.getServerInetAddress()), e);
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
        final boolean isNativeObject = object.getClass().getPackage() == ServerHandler.class.getPackage();
        return (isNativeObject) ? Logger.getLogger(object.getClass()) : LOGGER;
    }

    /**
	 * @return The {@link #serverContext} containing session data for external use.
	 */
    public ServerContext getServerContext() {
        return serverContext;
    }

    /**
	 * Called by {@link ServerMessageToClientPingPong#execute(ServerHandler)}. Sends a {@link ClientMessageToServerPingPong} to the server
	 * using {@link #sendMessage(ClientMessageToServer)}.
	 */
    public void pong() {
        sendMessage(new ClientMessageToServerPingPong());
    }

    /**
	 * Listens to client and converts input to objects of type {@link ServerMessageToClient}. Waits for input on the server
	 * {@link ServerEndpoint}. A thread is being used for this, since reading input is blocking, meaning the thread 'sleeps' while waiting
	 * for input. These messages are then added to the server stack for execution by external user.<br />
	 * <strong>Note:</strong> some messages are filtered by the server as they are of server level, such as ping messages.
	 * <p>
	 * Reuses fields from {@link ServerHandler}, such as {@link ServerHandler#client} and {@link ServerHandler#serverContext}. Keeps
	 * listening for input while the server is still running and the server hasn't dropped the connection.
	 * <p>
	 * The input data is read per line, so messages need to end with a newline character '\n'. Since clients need to conform to the lowest
	 * common denominator, they are required to append a zero (0) byte '\0' as per Flash clients. This character is appended to the newline
	 * '\n' character and needs to be discarded manually using a {@link BufferedReader#read()}.
	 * <p>
	 * Each complete line received serves as a datagram and is handled by {@link #handleMessage(String)}.
	 * 
	 * @see #handleMessage(String)
	 * @see SwiftSocketClient#isRunning()
	 */
    @Override
    public void run() {
        while (client.isRunning() && !serverContext.isServerSaidByeBye()) {
            try {
                final String line = serverContext.getServerEndpoint().readLine();
                if (client.isRunning()) {
                    if (line != null) {
                        serverContext.getServerEndpoint().read();
                        handleMessage(line);
                    } else {
                        LOGGER.debug(String.format("server %s closed connection non-gracefully", serverContext.getServerInetAddress()));
                        serverContext.setServerSaidByeBye(true);
                        client.removeServerHandler();
                        break;
                    }
                }
            } catch (final SocketException e) {
                if (e.getMessage().equals(ERROR_SOCKET_CLOSED) || e.getMessage().equals(ERROR_RECV_FAILED) || e.getMessage().equals(ERROR_CONNECTION_RESET)) {
                    if (e.getMessage().equals(ERROR_SOCKET_CLOSED)) {
                        final String msg = "tried to read or write to closed socket %s";
                        LOGGER.error(String.format(msg, serverContext.getServerInetAddress()));
                    } else {
                        final String msg = "socket in error state [%s] for server %s";
                        LOGGER.error(String.format(msg, e.getMessage(), serverContext.getServerInetAddress()));
                    }
                    serverContext.setServerSaidByeBye(true);
                    client.removeServerHandler();
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
	 * Handles a single server message string. Creates a {@link ServerMessageToClient} and adds this to the client message queue unless it
	 * is a server level message, such as a 'pong' notification, in which case the message is executed directly.
	 * 
	 * @param datagramMessage The client message string, minus newline character '\n' and zero (0) byte '\0'.
	 * @see #createDecodedExecutableResponse(String)
	 * @see SwiftSocketClient#addServerResponse(ServerMessageToClient)
	 */
    public void handleMessage(final String datagramMessage) {
        final ServerMessageToClient<?> response = createDecodedExecutableResponse(datagramMessage);
        determineLogger(response).debug(String.format("receiving response from %s: %s", serverContext, response.getClass().getSimpleName()));
        if (response instanceof ServerMessageToClientPingPong) {
            ((ServerMessageToClientPingPong) response).execute(ServerHandler.this);
        } else {
            client.addServerResponse(response);
        }
    }

    /**
	 * Parses the raw server message into a message id and message content. Passes both values to
	 * {@link #createDecodedExecutableMessage(int, String)} and returns the result. The first {@value #MESSAGEID_LENGTH} bytes of the raw
	 * message denote the message code.
	 * <p>
	 * In case anything goes wrong, a {@link ServerMessageToClientInvalid} object is returned instead which generates some useful message
	 * for logging purposes. The server user can choose to filter out unknown messages at its own discretion. If a
	 * <code>ServerMessageToClientInvalid</code> is executed however, it will throw a runtime exception with the original message.
	 * 
	 * @param datagramMessage The server input string that represents a single message.
	 * @return {@link #createDecodedExecutableMessage(int, String)}
	 * @see #createInvalidMessage(String, Exception, String)
	 */
    protected ServerMessageToClient<?> createDecodedExecutableResponse(final String datagramMessage) {
        final int responseCode;
        try {
            responseCode = Integer.parseInt(datagramMessage.substring(0, MESSAGEID_LENGTH));
        } catch (final NumberFormatException e) {
            return createInvalidMessage(datagramMessage, e, "invalid response identifier");
        }
        final String responseBody = datagramMessage.substring(MESSAGEID_LENGTH);
        return createDecodedExecutableMessage(responseCode, responseBody);
    }

    /**
	 * Creates a {@link ServerMessageToClient} instance based on the message id and message content.
	 * 
	 * @param messageId The identifier for the message type we need to instantiate.
	 * @param messageContent The message content for the solved message type to decode.
	 * @return A {@link ServerMessageToClient} with identified by <code>messageId</code> decoded from <code>messageContent</code>.
	 * @see SwiftSocketClient#getServerMessageToClientType(int)
	 * @see ServerMessageToClient#decode(String)
	 */
    private final ServerMessageToClient<?> createDecodedExecutableMessage(final int messageId, final String messageContent) {
        final Class<? extends ServerMessageToClient<?>> responseType = client.getServerMessageToClientType(messageId);
        ServerMessageToClient<?> response;
        try {
            response = responseType.newInstance();
        } catch (final IllegalAccessException e) {
            return createInvalidMessage(messageContent, e, "constructor not visible for message");
        } catch (final InstantiationException e) {
            return createInvalidMessage(messageContent, e, "unable to construct message");
        }
        response.decode(messageContent);
        return response;
    }

    /**
	 * Generates a {@link ServerMessageToClientInvalid} object with the failure cause. When executed, this message throws a runtime
	 * exception with the original exception.
	 * 
	 * @param originalMessage The original message
	 * @param failureCause The exception that occurred when the original messages was being converted into a {@link ServerMessageToClient}.
	 * @param cause A fine grained exception description when the exception <code>failureCause</code> occurred.
	 * @return An instance of {@link ServerMessageToClientInvalid}.
	 */
    private ServerMessageToClient<?> createInvalidMessage(final String originalMessage, final Exception failureCause, final String cause) {
        final ServerMessageToClientInvalid response = new ServerMessageToClientInvalid(serverContext, failureCause);
        response.decode(String.format("%s (original response: '%s')", cause, originalMessage));
        return response;
    }
}
