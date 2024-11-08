package portochat.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import portochat.common.User;
import portochat.common.protocol.ChannelJoinPart;
import portochat.common.protocol.ChannelList;
import portochat.common.protocol.ChannelStatus;
import portochat.common.protocol.ChatMessage;
import portochat.common.protocol.DefaultData;
import portochat.common.protocol.Ping;
import portochat.common.protocol.Pong;
import portochat.common.protocol.ServerMessage;
import portochat.common.protocol.ServerMessageEnum;
import portochat.common.protocol.UserConnection;
import portochat.common.protocol.UserData;
import portochat.common.protocol.UserList;
import portochat.common.socket.TCPSocket;
import portochat.common.socket.event.NetEvent;
import portochat.common.socket.event.NetListener;

/**
 * This class handles the server connection, and populating the user/channel
 * databases with the incoming messages.
 * 
 * @author Mike
 */
public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private TCPSocket tcpSocket = null;

    private UserDatabase userDatabase = null;

    private ChannelDatabase channelDatabase = null;

    /**
     * Public constructor
     */
    public Server() {
        userDatabase = UserDatabase.getInstance();
        channelDatabase = ChannelDatabase.getInstance();
    }

    /**
     * Binds to a port to listen on
     * 
     * @param port the port number
     * 
     * @return true if successful
     */
    public boolean bind(int port) {
        boolean success = true;
        try {
            tcpSocket = new TCPSocket("Server");
            success = tcpSocket.bind(port);
            if (success) {
                tcpSocket.addListener(new ServerHandler());
                logger.log(Level.INFO, "Server bound to port: {0}", port);
            } else {
                logger.log(Level.SEVERE, "Server unable to bind to port: {0}", port);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Couldn't listen on port: " + port, ex);
            success = false;
        }
        return success;
    }

    /**
     * Shuts the server down
     */
    public void shutdown() {
        ArrayList<Socket> userSocketList = (ArrayList<Socket>) userDatabase.getSocketList();
        for (Socket socket : userSocketList) {
            try {
                socket.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        tcpSocket.disconnect();
    }

    /**
     * This class handles incoming messages to the server
     */
    private class ServerHandler implements NetListener {

        /**
         * This method handles the incoming events
         */
        @Override
        public void incomingMessage(NetEvent event) {
            Socket socket = (Socket) event.getSource();
            DefaultData defaultData = event.getData();
            User user = userDatabase.getSocketOfUser(socket);
            if (defaultData instanceof UserData) {
                UserData userData = (UserData) defaultData;
                String oldUserName = user.getName();
                boolean rename = (oldUserName != null);
                boolean success = false;
                if (!rename) {
                    success = userDatabase.addUser(userData.getName(), socket);
                } else {
                    success = userDatabase.renameUser(user, userData.getName());
                    logger.log(Level.INFO, "Renamed of {0} to {1} was {2}", new Object[] { oldUserName, userData.getName(), success ? "successful!" : "unsuccessful!" });
                }
                ServerMessage serverMessage = new ServerMessage();
                if (success) {
                    if (!rename) {
                        UserConnection userConnection = new UserConnection();
                        userConnection.setUser(user);
                        userConnection.setConnected(true);
                        ArrayList<Socket> userSocketList = (ArrayList<Socket>) userDatabase.getSocketList();
                        sendToAllSockets(userSocketList, userConnection);
                    } else {
                        channelDatabase.renameUser(oldUserName, userData.getName());
                    }
                    serverMessage.setMessageEnum(ServerMessageEnum.USERNAME_SET);
                    serverMessage.setAdditionalMessage(userData.getName());
                } else {
                    serverMessage.setMessageEnum(ServerMessageEnum.ERROR_USERNAME_IN_USE);
                    serverMessage.setAdditionalMessage(userData.getName());
                }
                tcpSocket.writeData(socket, serverMessage);
            } else if (defaultData instanceof Ping) {
                Pong pong = new Pong();
                pong.setTimestamp(((Ping) defaultData).getTimestamp());
                tcpSocket.writeData(socket, pong);
            } else if (defaultData instanceof ChatMessage) {
                ChatMessage chatMessage = ((ChatMessage) defaultData);
                chatMessage.setFromUser(user);
                if (chatMessage.isChannel()) {
                    ArrayList<Socket> userSocketList = (ArrayList<Socket>) channelDatabase.getSocketsOfUsersInChannel(chatMessage.getTo(), chatMessage.getFromUser());
                    if (userSocketList != null) {
                        for (Socket userSocket : userSocketList) {
                            tcpSocket.writeData(userSocket, chatMessage);
                        }
                    } else {
                        ServerMessage serverMessage = new ServerMessage();
                        serverMessage.setMessageEnum(ServerMessageEnum.ERROR_CHANNEL_NON_EXISTENT);
                        serverMessage.setAdditionalMessage(chatMessage.getTo());
                        tcpSocket.writeData(socket, serverMessage);
                    }
                } else {
                    Socket toUserSocket = userDatabase.getUserOfSocket(chatMessage.getTo());
                    if (toUserSocket != null) {
                        tcpSocket.writeData(toUserSocket, chatMessage);
                    } else {
                        ServerMessage serverMessage = new ServerMessage();
                        serverMessage.setMessageEnum(ServerMessageEnum.ERROR_USER_NON_EXISTENT);
                        serverMessage.setAdditionalMessage(chatMessage.getTo());
                        tcpSocket.writeData(socket, serverMessage);
                    }
                }
            } else if (defaultData instanceof UserList) {
                UserList userList = ((UserList) defaultData);
                String channel = userList.getChannel();
                if (channel != null) {
                    userList.setUserList(channelDatabase.getUsersInChannel(channel));
                } else {
                    userList.setUserList(userDatabase.getUserList());
                }
                tcpSocket.writeData(socket, userList);
            } else if (defaultData instanceof UserConnection) {
                UserConnection userConnection = ((UserConnection) defaultData);
                if (!userConnection.isConnected()) {
                    boolean success = userDatabase.removeUser(userConnection.getUser());
                    ArrayList<String> userChannelList = (ArrayList<String>) channelDatabase.getUserChannels(userConnection.getUser());
                    channelDatabase.removeUserFromAllChannels(userConnection.getUser());
                    for (String channel : userChannelList) {
                        if (!channelDatabase.channelExists(channel)) {
                            notifyChannelStatusChange(channel, false);
                        }
                    }
                }
                ArrayList<Socket> userSocketList = (ArrayList<Socket>) userDatabase.getSocketList();
                sendToAllSockets(userSocketList, userConnection);
                logger.info(userConnection.toString());
            } else if (defaultData instanceof ChannelList) {
                ChannelList channelList = ((ChannelList) defaultData);
                channelList.setChannelList(channelDatabase.getListOfChannels());
                tcpSocket.writeData(socket, channelList);
            } else if (defaultData instanceof ChannelJoinPart) {
                ChannelJoinPart channelJoinPart = ((ChannelJoinPart) defaultData);
                channelJoinPart.setUser(user);
                if (channelJoinPart.hasJoined()) {
                    if (!channelDatabase.channelExists(channelJoinPart.getChannel())) {
                        notifyChannelStatusChange(channelJoinPart.getChannel(), true);
                    }
                    channelDatabase.addUserToChannel(channelJoinPart.getChannel(), channelJoinPart.getUser());
                } else {
                    channelDatabase.removeUserFromChannel(channelJoinPart.getChannel(), channelJoinPart.getUser());
                    if (!channelDatabase.channelExists(channelJoinPart.getChannel())) {
                        notifyChannelStatusChange(channelJoinPart.getChannel(), false);
                    }
                }
                ArrayList<Socket> userSocketList = (ArrayList<Socket>) channelDatabase.getSocketsOfUsersInChannel(channelJoinPart.getChannel(), channelJoinPart.getUser());
                sendToAllSockets(userSocketList, channelJoinPart);
                logger.info(channelJoinPart.toString());
            } else {
                logger.log(Level.WARNING, "Unhandled message: {0}", defaultData);
            }
        }
    }

    private void sendToAllSockets(List<Socket> userSocketList, DefaultData data) {
        if (userSocketList != null && userSocketList.size() > 0) {
            for (Socket userSocket : userSocketList) {
                tcpSocket.writeData(userSocket, data);
            }
        }
    }

    /**
     * Notifies of channel status change (creation/deletions)
     * 
     * @param channel
     * @param created true if created, false if deleted
     */
    private void notifyChannelStatusChange(String channel, boolean created) {
        ChannelStatus channelStatus = new ChannelStatus();
        channelStatus.setChannel(channel);
        channelStatus.setCreated(created);
        ArrayList<Socket> userSocketList = (ArrayList<Socket>) userDatabase.getSocketList();
        sendToAllSockets(userSocketList, channelStatus);
    }
}
