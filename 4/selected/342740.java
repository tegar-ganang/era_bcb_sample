package edu.simplemqom.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.net.ProtocolException;
import java.nio.charset.Charset;
import org.apache.log4j.Logger;
import edu.simplemqom.Message;
import edu.simplemqom.MessageBroker;
import edu.simplemqom.MessageBrokerController;
import edu.simplemqom.objects.SMQOMChannel;
import edu.simplemqom.objects.SMQOMQueue;
import edu.simplemqom.objects.SMQOMQueueRights;
import edu.simplemqom.protocol.SMQOMCommandId;
import edu.simplemqom.protocol.SMQOMPackage;
import edu.simplemqom.protocol.SMQOMProtocol;

/**
 * @author Pawe�
 *
 */
public class ClientHandler extends Thread {

    private SMQOMChannel clientChannel = null;

    private HashMap<String, SMQOMQueue> clientWriteQueues;

    private HashMap<String, SMQOMQueue> clientReadQueues;

    private Socket clientSocket;

    private int threadId;

    private DataInputStream input;

    private DataOutputStream output;

    private int sessionId;

    private MessageBrokerController MBController;

    private static Logger logger = MessageBroker.logger;

    private byte[] body;

    private boolean working = true;

    public ClientHandler(Socket socket, int id) {
        super("Simple-MQOM_ClientHanndler-" + id);
        this.MBController = MessageBrokerController.getInstance();
        this.threadId = id;
        this.clientSocket = socket;
        try {
            output = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
            input = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
        } catch (IOException e) {
            logger.error("Client Handler " + this.threadId + ": Can't create streams out from socket");
        }
        clientReadQueues = new HashMap<String, SMQOMQueue>();
        clientWriteQueues = new HashMap<String, SMQOMQueue>();
    }

    @Override
    public void run() {
        logger.info("[Created thread][Simple-MQOM_ClientHanndler-" + threadId + "]");
        int packageDataLn;
        byte commandId;
        byte magic;
        sessionId = -1;
        while (working == true) {
            try {
                SMQOMPackage pack = SMQOMProtocol.readPackageFromStream(input);
                packageDataLn = pack.getDataSize();
                commandId = pack.getCommandId();
                body = pack.getDataBytes();
                logger.debug("Received package size " + pack.getSize() + "\nReceived command id" + pack.getCommandId());
                interpretReceivedCommand(pack);
            } catch (SocketException e) {
                logger.warn("Encountered socket error", e);
            } catch (EOFException e) {
                logger.warn("Client Handler " + this.threadId + ": Connection with client closed without a warning!", e);
                break;
            } catch (IOException e) {
                logger.error("Unexpected client handling error!", e);
            }
        }
        try {
            input.close();
            output.close();
            clientSocket.close();
        } catch (IOException e) {
            logger.error("Error closing client streams and socket", e);
        }
    }

    /**
	 * @param commandId
	 * @param content
	 * @throws IOException
	 */
    private void sendPackage(byte commandId, byte[] content) throws IOException {
        try {
            output.write(new SMQOMPackage(commandId, sessionId, content).getBytes());
            output.flush();
        } catch (IOException e) {
            throw e;
        }
    }

    /**
	 * @param pack
	 * @throws IOException
	 */
    private void interpretReceivedCommand(SMQOMPackage pack) throws IOException {
        if (pack.getCommandId() == SMQOMCommandId.INIT_CONNECT) {
            handleConnectionInit();
        } else if (pack.getCommandId() == SMQOMCommandId.DISCONNECT) {
            handleDisconnect();
        } else if (pack.getCommandId() == SMQOMCommandId.REQ_CHANNEL) {
            handleChannelRequest(pack);
        } else if (pack.getCommandId() == SMQOMCommandId.REQ_QUEUE) {
            handleQueueRequest(pack);
        } else if (pack.getCommandId() == SMQOMCommandId.REQ_MSG_SND) {
            handleSendMessage(pack);
        } else if (pack.getCommandId() == SMQOMCommandId.REQ_MSG_REC) {
            handleGetMessage(pack);
        } else if (pack.getCommandId() == SMQOMCommandId.REQ_QUEUE_EXISTS) {
            handleQueueExists(pack);
        } else if (pack.getCommandId() == SMQOMCommandId.REQ_QUEUE_LEN) {
            handleQueueLength(pack);
        } else if (pack.getCommandId() == SMQOMCommandId.REQ_QUEUE_MAX_LEN) {
            handleQueueMaxLength(pack);
        } else if (pack.getCommandId() == SMQOMCommandId.KEEP_ALIVE) {
            handleKeepAlive();
        }
    }

    /**
	 * 
	 */
    private void handleConnectionInit() {
        if (MBController.ipValidation(clientSocket.getInetAddress())) {
            try {
                sendPackage(SMQOMCommandId.ACCEPT_CLIENT_CONNECT, null);
                SMQOMPackage pack = SMQOMProtocol.readPackageFromStream(input);
                if (SMQOMProtocol.isAcknowledgement(pack)) {
                    logger.debug("Client Handler " + this.threadId + ": Connection reestablished." + clientSocket.getInetAddress().getHostAddress());
                }
            } catch (IOException e) {
                logger.error("Error handling CONNECTION INIT command.", e);
            }
        } else {
            try {
                sendPackage(SMQOMCommandId.DENY_CONNECT, null);
                SMQOMPackage pack = SMQOMProtocol.readPackageFromStream(input);
                if (SMQOMProtocol.isAcknowledgement(pack)) {
                    logger.debug("Client Handler " + this.threadId + ": Denied connection to " + clientSocket.getInetAddress().getHostAddress());
                }
            } catch (IOException e) {
                logger.error("", e);
            }
        }
    }

    /**
	 * 
	 */
    private void handleDisconnect() {
        try {
            sendPackage(SMQOMCommandId.DISCONNECT_ACK, null);
            working = false;
            System.out.println("Client handler " + this.threadId + ": Connection closed on client's demand");
        } catch (IOException e) {
            logger.error("IOException when handling client disconnect request", e);
        }
    }

    /**
	 * @param pack
	 */
    private void handleChannelRequest(SMQOMPackage pack) {
        try {
            if (pack.getDataSize() > 0) {
                String channelName = new String(pack.getDataBytes());
                clientChannel = MBController.getChannel(channelName);
                if (clientChannel != null) {
                    logger.trace("Granting channel access to client.");
                    logger.trace("Client Handler " + this.threadId + ": Channel " + channelName + " found!");
                    sendPackage(SMQOMCommandId.SND_CHANNEL_ACK, null);
                    SMQOMPackage pkg = SMQOMProtocol.readPackageFromStream(input);
                    if (SMQOMProtocol.isAcknowledgement(pkg)) {
                        logger.trace("Client Handler " + this.threadId + ": Client has accepted channel's granting");
                    }
                } else {
                    output.write(new SMQOMPackage(SMQOMCommandId.SND_REQ_DENY, sessionId, null).getBytes());
                    output.flush();
                    SMQOMPackage pkg = SMQOMProtocol.readPackageFromStream(input);
                    if (SMQOMProtocol.isAcknowledgement(pkg)) {
                        logger.trace("Client Handler " + this.threadId + ": Client has accepted channel granting's denial.");
                    }
                }
            } else {
                output.write(new SMQOMPackage(SMQOMCommandId.SND_REQ_DENY, sessionId, null).getBytes());
                output.flush();
                SMQOMPackage pkg = SMQOMProtocol.readPackageFromStream(input);
                if (SMQOMProtocol.isAcknowledgement(pkg)) {
                    logger.trace("Client Handler " + this.threadId + ": Client has accepted channel granting's denial.");
                }
            }
        } catch (IOException e) {
            logger.error("IOException when handling client disconnect request", e);
        }
    }

    /**
	 * @param pack
	 */
    private void handleQueueRequest(SMQOMPackage pack) {
        if (pack.getDataSize() > 0) {
            SMQOMQueue requestedQueue = null;
            byte rights = pack.getDataBytes()[0];
            byte[] qName = new byte[pack.getDataSize() - 1];
            System.arraycopy(pack.getDataBytes(), 1, qName, 0, qName.length);
            String queueName = new String(qName);
            logger.trace("Client requested queue " + queueName + " with rights " + rights);
            if (clientChannel != null && clientChannel.getQueueRights(queueName, rights) == true) {
                requestedQueue = clientChannel.getQueue(queueName);
            }
            if (requestedQueue != null) {
                try {
                    sendPackage(SMQOMCommandId.SND_QUEUE_ACK, null);
                    SMQOMPackage pkg = SMQOMProtocol.readPackageFromStream(input);
                    if (SMQOMProtocol.isAcknowledgement(pkg)) {
                        if ((rights == SMQOMQueueRights.CONSUMER || rights == SMQOMQueueRights.ADMIN) && !clientReadQueues.containsKey(requestedQueue.getName())) {
                            clientReadQueues.put(requestedQueue.getName(), requestedQueue);
                        }
                        if ((rights == SMQOMQueueRights.PRODUCER || rights == SMQOMQueueRights.ADMIN) && !clientWriteQueues.containsKey(requestedQueue.getName())) {
                            clientWriteQueues.put(requestedQueue.getName(), requestedQueue);
                        }
                        logger.trace("Client Handler " + this.threadId + ": Client has accepted queue's granting.");
                    } else {
                    }
                } catch (IOException e) {
                    logger.error("IOException when granting queue to client.", e);
                }
            } else {
                try {
                    sendPackage(SMQOMCommandId.SND_REQ_DENY, null);
                    SMQOMPackage pkg = SMQOMProtocol.readPackageFromStream(input);
                    if (SMQOMProtocol.isAcknowledgement(pkg)) {
                        logger.trace("Client Handler " + this.threadId + ": Client has accepted queue granting's denial.");
                    }
                } catch (IOException e) {
                    logger.error("IOException when denying queue acces for client", e);
                }
            }
        } else {
            try {
                sendPackage(SMQOMCommandId.SND_REQ_DENY, null);
                SMQOMPackage pkg = SMQOMProtocol.readPackageFromStream(input);
                if (SMQOMProtocol.isAcknowledgement(pkg)) {
                    logger.trace("Client Handler " + this.threadId + ": Client has accepted queue granting's denial.");
                }
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
    }

    /**
	 * @param pkg
	 */
    private void handleSendMessage(SMQOMPackage pkg) {
        try {
            if (pkg.getDataSize() > 0) {
                SMQOMPackage pack = null;
                String queuName = new String(pkg.getDataBytes());
                SMQOMQueue queue = clientWriteQueues.get(queuName);
                if (queue == null) {
                    logger.error("Client sends message to queue not opened for writing");
                    sendPackage(SMQOMCommandId.SND_REQ_MSG_DENY, null);
                    pack = SMQOMProtocol.readPackageFromStream(input);
                    if (SMQOMProtocol.isAcknowledgement(pack)) {
                        logger.trace("Received Acknowledgement for message denial");
                    }
                    return;
                }
                sendPackage(SMQOMCommandId.SND_REQ_MSG_ACK, null);
                logger.trace("Receiving message...");
                pack = SMQOMProtocol.readPackageFromStream(input);
                if (pack.getCommandId() != SMQOMCommandId.SND_MSG_BODY) {
                    throw new ProtocolException("Unexpected Package. Expected message SND_MSG_BODY");
                }
                Message message = Message.parseBytes(pack.getDataBytes());
                message.setId(MBController.getNewMessageId());
                if (message.getCorrelationId() == 0) {
                    message.setCorrelationId(message.getId());
                }
                logger.trace("Received message: " + message);
                if (queue.offer(message)) {
                    logger.trace("Message in queue...");
                    try {
                        sendPackage(SMQOMCommandId.SND_BODY_ACK, message.getHeader());
                    } catch (IOException e) {
                        throw e;
                    }
                    logger.trace("Current length of messages destination is: " + clientChannel.getQueue(message.getDestination()).size());
                } else {
                    logger.trace("Unable to put message in queue...");
                    try {
                        sendPackage(SMQOMCommandId.SND_REQ_MSG_DENY, null);
                        pack = SMQOMProtocol.readPackageFromStream(input);
                        if (SMQOMProtocol.isAcknowledgement(pack)) {
                            logger.trace("Received Acknowledgement for put message denial");
                        }
                        return;
                    } catch (IOException e) {
                        logger.error("Error sending SND_REQ_MSG_DENY", e);
                    }
                }
            } else {
                logger.error("Received unknown package");
                try {
                    sendPackage(SMQOMCommandId.UNKNOWN_PACKAGE, null);
                } catch (IOException e) {
                    logger.error("Error sending unknown package when receiving message", e);
                }
                throw new ProtocolException("Unknow Package. Received message of size 0.");
            }
        } catch (IOException e) {
            logger.error("IOException when handling SEND MESSAGE request.", e);
        }
    }

    /**
	 * @param pkg
	 */
    private void handleGetMessage(SMQOMPackage pkg) {
        logger.trace("Handling GET Message request");
        SMQOMPackage pack = null;
        SMQOMQueue queue = clientReadQueues.get(new String(pkg.getDataBytes(), Charset.forName("UTF-8")));
        if (queue == null) {
            try {
                logger.error("Client sends message to queue not opened for writing");
                sendPackage(SMQOMCommandId.SND_REQ_MSG_DENY, null);
                pack = SMQOMProtocol.readPackageFromStream(input);
                if (SMQOMProtocol.isAcknowledgement(pack)) {
                    logger.trace("Received Acknowledgement for GET MESSAGE denial");
                }
            } catch (ProtocolException e) {
                logger.error("Received protocol exception when denying SEND MESSAGE", e);
            } catch (IOException e) {
                logger.error("IOException when denying SEND MESSAGE", e);
            } finally {
            }
            return;
        }
        Message message = queue.poll();
        if (message != null) {
            try {
                logger.debug("Sending message header to a client...");
                sendPackage(SMQOMCommandId.SND_MSG_REC_ACK, message.getHeader());
                pack = SMQOMProtocol.readPackageFromStream(input);
                if (pack.getCommandId() == SMQOMCommandId.REQ_MSG_REC_BODY && pack.getDataSize() == 0) {
                    logger.debug("Sending message body to a client...");
                    sendPackage(SMQOMCommandId.SND_MSG_REC_BODY, message.getBody());
                    pack = SMQOMProtocol.readPackageFromStream(input);
                    if (pack.getCommandId() == SMQOMCommandId.REQ_MSG_REC_BODY_ACK && pack.getDataSize() == 0) {
                        logger.trace("Message Id: " + message.getId() + " succesfully sent to client");
                    } else {
                        logger.debug("Client hasn't confirmed receiving message");
                        try {
                            queue.offerFirst(message, 60, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            logger.warn("Thread interrupted whe inserting message back to queue", e);
                        }
                    }
                } else if (pack.getCommandId() == SMQOMCommandId.REQ_MSG_REC_CANCELL) {
                    logger.debug("Client has cancelled message request");
                    sendPackage(SMQOMCommandId.ACK, null);
                    try {
                        queue.offerFirst(message, 60, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        logger.warn("Thread interrupted whe inserting message back to queue", e);
                    }
                } else {
                    try {
                        queue.offerFirst(message, 60, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        logger.warn("Thread interrupted whe inserting message back to queue", e);
                    }
                    throw new ProtocolException("Unexpected package! Expected MSG_REC_BODY_ACK or MSG_REC_CANCELL.");
                }
            } catch (IOException e) {
                logger.error("Error sending message to client.", e);
            }
        } else {
            try {
                sendPackage(SMQOMCommandId.SND_MSG_REC_ERR, new String("EMPTY_QUEUE").getBytes());
            } catch (IOException e) {
                logger.error("Error sending message to client.", e);
            }
        }
    }

    /**
	 * @param pack
	 * @throws IOException
	 */
    private void handleQueueExists(SMQOMPackage pack) throws IOException {
        if (pack.getDataSize() > 0) {
            String queueName = new String(pack.getDataBytes());
            if (clientChannel.getQueue(queueName) != null) {
                sendPackage(SMQOMCommandId.SND_QUEUE_EXISTS_ACK, null);
            } else {
                sendPackage(SMQOMCommandId.SND_REQ_DENY, null);
            }
        } else {
            logger.trace("Received _REQ_QUEUE_EXISTS package without queue name specified)");
            sendPackage(SMQOMCommandId.UNKNOWN_PACKAGE, null);
        }
    }

    /**
	 * @param pack
	 * @throws IOException
	 */
    private void handleQueueLength(SMQOMPackage pack) throws IOException {
        if (pack.getDataSize() > 0) {
            String queueName = new String(pack.getDataBytes());
            SMQOMQueue queue = clientReadQueues.get(queueName);
            if (queue == null) {
                queue = clientWriteQueues.get(queueName);
            }
            if (queue != null) {
                int size = queue.getCurrentLength();
                byte[] body = SMQOMProtocol.intToByteArray(size);
                sendPackage(SMQOMCommandId.SND_QUEUE_LEN, body);
            } else {
                sendPackage(SMQOMCommandId.SND_REQ_DENY, null);
            }
        } else {
            logger.trace("Received REQ_QUEUE_LEN package without queue name specified)");
            sendPackage(SMQOMCommandId.UNKNOWN_PACKAGE, null);
        }
    }

    /**
	 * @param pack
	 * @throws IOException
	 */
    private void handleQueueMaxLength(SMQOMPackage pack) throws IOException {
        if (pack.getDataSize() > 0) {
            String queueName = new String(pack.getDataBytes());
            SMQOMQueue queue = clientReadQueues.get(queueName);
            if (queue == null) {
                queue = clientWriteQueues.get(queueName);
            }
            if (queue != null) {
                int maxLn = queue.getMaxLength();
                byte[] body = SMQOMProtocol.intToByteArray(maxLn);
                sendPackage(SMQOMCommandId.SND_QUEUE_LEN, body);
            } else {
                sendPackage(SMQOMCommandId.SND_REQ_DENY, null);
            }
        } else {
            logger.trace("Received REQ_QUEUE_LEN package without queue name specified)");
            sendPackage(SMQOMCommandId.UNKNOWN_PACKAGE, null);
        }
    }

    /**
	 * 
	 */
    private void handleKeepAlive() {
        try {
            sendPackage(SMQOMCommandId.KEEP_ALIVE, null);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    /**
	 * @return
	 */
    private boolean isClientAlive() {
        try {
            sendPackage(SMQOMCommandId.KEEP_ALIVE, null);
            SMQOMPackage pack = SMQOMProtocol.readPackageFromStream(input);
            if (pack.getCommandId() == SMQOMCommandId.KEEP_ALIVE) {
                return true;
            } else {
                return false;
            }
        } catch (ProtocolException e) {
            logger.error("Protocol exception within KEEP ALIVE procedure", e);
            return false;
        } catch (IOException e) {
            logger.error("IOException within KEEP ALIVE procedure", e);
            return false;
        }
    }
}
