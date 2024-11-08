package felper.transport;

import org.apache.log4j.Logger;
import felper.event.FelperEvent;
import felper.event.FelperEventListener;
import felper.event.FelperMessageReceiveEvent;
import felper.exception.transport.FelperNotEstablishedConnectionException;
import felper.exception.transport.FelperNotSentMessageException;
import felper.message.Message;
import felper.transport.TransportManagerInterface;
import felper.user.UserConnection;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.HashMap;

/**
 * @author Andrey Golubev
 */
public class TransportManager implements TransportManagerInterface {

    /**
     * Socket Server
     */
    private ServerSocket serverSocket;

    private LinkedList<FelperEventListener> felperEventsReceivers = new LinkedList<FelperEventListener>();

    private HashMap<UserConnection, Socket> sockets = new HashMap<UserConnection, Socket>();

    private static final Logger logger = Logger.getLogger(TransportManager.class);

    /**
     * Construct new Transport Layer
     *
     * @param serverSocketPort port number for server socket
     */
    public TransportManager(int serverSocketPort) {
        try {
            serverSocket = new ServerSocket(serverSocketPort);
        } catch (IOException e) {
            logger.fatal("Can`t create Server Socket for Transport Layer", e);
            System.exit(1);
        }
        if (logger.isInfoEnabled()) logger.info("Transport Layer is Started on port: " + serverSocketPort);
        ServerSocketThread serverSocketThread = new ServerSocketThread();
        serverSocketThread.start();
    }

    public void finalize() throws Throwable {
    }

    /**
     * Send message to peer
     *
     * @param peerConnection Peer Connection of user
     * @param message        Message to send
     */
    public void sendMessageToPeer(UserConnection userConnection, Message message) throws FelperNotSentMessageException {
        SendMessageThread sendMessageThread = new SendMessageThread(userConnection, message);
        sendMessageThread.start();
        if (sendMessageThread.isInterrupted()) throw new FelperNotSentMessageException();
    }

    public UserConnection connectToPeer(InetSocketAddress userAddress) throws FelperNotEstablishedConnectionException {
        if (logger.isDebugEnabled()) logger.debug("Connecting peer: " + userAddress);
        try {
            UserConnection newUserConnection = new UserConnection();
            Socket newSocket = new Socket(userAddress.getAddress(), userAddress.getPort());
            sockets.put(newUserConnection, newSocket);
            return newUserConnection;
        } catch (Exception e) {
            logger.warn("Can`t connect to address " + userAddress + " Unknown IP Address: ", e);
            throw new FelperNotEstablishedConnectionException("Can`t connect to address " + userAddress);
        }
    }

    /**
     * Add Felper Event Listener
     *
     * @param eventListener service, who want to receive events from Transport Layer
     */
    public void registerEventListener(FelperEventListener felperEventListener) {
        this.felperEventsReceivers.add(felperEventListener);
        if (logger.isInfoEnabled()) logger.info("New Service Event Listener is Added to Transport Layer: " + felperEventListener);
    }

    /**
     * Inner class for new thread for accepting new inbound connection.
     */
    private class AcceptSocketThread extends Thread {

        /**
         * inner attribute to store current socket
         */
        private Socket socket;

        /**
         * Constructor for thread
         *
         * @param newSocket accepted socket
         */
        AcceptSocketThread(Socket newSocket) {
            this.socket = newSocket;
        }

        public void run() {
            this.setName(this.getName() + "-AcceptSocketThread");
            if (logger.isDebugEnabled()) logger.debug("Accept Socket Thread is Started: ");
            Message newMessage = null;
            try {
                ObjectInput newSocketObjectInputStream = new ObjectInputStream(this.socket.getInputStream());
                newMessage = (Message) newSocketObjectInputStream.readObject();
            } catch (IOException e) {
                logger.warn("Can`t accept new socket:", e);
            } catch (ClassNotFoundException e) {
                logger.warn("Can`t receive message:", e);
            }
            if (logger.isDebugEnabled()) logger.debug("Receive message: " + newMessage);
            if (felperEventsReceivers != null) {
                for (FelperEventListener eventReceiver : felperEventsReceivers) {
                    FelperMessageReceiveEvent newEvent = new FelperMessageReceiveEvent();
                    newEvent.setMessage(newMessage);
                    eventReceiver.eventPerfomed(newEvent);
                    if (logger.isDebugEnabled()) logger.debug("Send New Event:" + newEvent);
                }
            }
        }
    }

    /**
     * Inner class for new thread for creating new Server Socket.
     */
    private class ServerSocketThread extends Thread {

        public void run() {
            this.setName(this.getName() + "-ServerSocketThread");
            if (logger.isDebugEnabled()) logger.debug("Server Socket Thread is Started: ");
            while (true) {
                try {
                    AcceptSocketThread acceptSocketThread = new AcceptSocketThread(serverSocket.accept());
                    acceptSocketThread.start();
                } catch (IOException e) {
                    logger.warn("Can`t accept new socket:", e);
                }
            }
        }
    }

    /**
     * Inner class for new thread for sending message to peer.
     */
    private class SendMessageThread extends Thread {

        /**
         * inner attribute to store current PeerConnection
         */
        private UserConnection threadUserConnection;

        /**
         * inner attribute to store current Message
         */
        private Message threadMessage;

        /**
         * Constructor for thread.
         *
         * @param peerConnection PeerConnection of user
         * @param message        message to send
         */
        SendMessageThread(UserConnection userConnection, Message message) {
            this.threadUserConnection = userConnection;
            this.threadMessage = message;
        }

        public void uncaughtException(Thread thread, Throwable exception) throws FelperNotSentMessageException {
            if (exception instanceof IOException) throw new FelperNotSentMessageException();
        }

        public void run() {
            this.setName(this.getName() + "-SendMessageThread");
            if (logger.isDebugEnabled()) logger.debug("Send Message Thread is Started: ");
            try {
                Socket socketSender = sockets.get(this.threadUserConnection);
                if (socketSender == null) this.interrupt();
                ObjectOutput socketSenderObjectOutputStream = new ObjectOutputStream(socketSender.getOutputStream());
                socketSenderObjectOutputStream.writeObject(this.threadMessage);
                socketSenderObjectOutputStream.flush();
                socketSenderObjectOutputStream.close();
                if (logger.isDebugEnabled()) logger.debug("Message is Sent to: " + socketSender.getInetAddress().getHostAddress() + " Message: " + this.threadMessage);
            } catch (Exception e) {
                logger.warn("Can`t send message to peer: ", e);
                this.interrupt();
            }
        }
    }
}
