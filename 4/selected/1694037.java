package com.usoog.hextd.server;

import com.usoog.commons.gamecore.USOOGGameCore;
import com.usoog.commons.gamecore.UserInfo;
import com.usoog.commons.gamecore.message.MessageFetch;
import com.usoog.commons.gamecore.message.MessageGameJoin;
import com.usoog.commons.gamecore.message.MessagePing;
import com.usoog.commons.gamecore.message.MessagePong;
import com.usoog.commons.gamecore.message.MessageReplay;
import com.usoog.commons.gamecore.message.MessageUnknown;
import com.usoog.commons.gamecore.message.MessageUserAuth;
import com.usoog.commons.gamecore.message.MessageUserInfo;
import com.usoog.hextd.Server;
import com.usoog.commons.network.MessageListener;
import com.usoog.commons.network.NetworkConnection;
import com.usoog.commons.network.message.Message;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ServerUser handles the dealings of a single connection of a user to the
 * server. It keep track of things like who the user is, which channel the user
 * is in and through which NetworkConnection the user is connected to the
 * server.
 */
public class ServerUser implements MessageListener {

    /**
	 * Commands are used to encapsulate code into something that can be put in a
	 * Map
	 *
	 * TODO: set back to private after DataNucleus fixes it's schemaTool
	 */
    public interface MessageCommand {

        public void execute(Message message);
    }

    /**
	 * The map of commands per message
	 */
    private Map<String, MessageCommand> messageCommands = new HashMap<String, MessageCommand>();

    private Server server;

    private Channel channel;

    private boolean authenticated = false;

    private NetworkConnection connection;

    /**
	 * The in-game id of the player.
	 */
    private int playerId = 0;

    private boolean ready = false;

    private UserInfo userInfo;

    public ServerUser(Server server) {
        this.server = server;
        makeMessageCommands();
    }

    public void setConnection(NetworkConnection connection) {
        this.connection = connection;
        this.connection.addMessageListener(this);
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
        this.ready = false;
    }

    public void doAuth(String token) {
        System.out.println("ServerUser::doAuth: Doing auth for '" + token + "'");
        authenticated = server.authenticatePlayer(this, token);
        if (authenticated) {
            System.out.println("ServerUser::doAuth: Found user '" + userInfo.getName() + "'");
            MessageUserInfo m = new MessageUserInfo(userInfo);
            m.setSenderId(USOOGGameCore.SERVER_ID);
            sendMessage(m);
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public NetworkConnection getConnection() {
        return connection;
    }

    public void sendMessage(Message message) {
        connection.sendMessage(message);
    }

    /**
	 * Create the objects that handle the different messages.
	 */
    private void makeMessageCommands() {
        messageCommands.put(MessageUserAuth.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                MessageUserAuth mua = (MessageUserAuth) message;
                doAuth(mua.getToken());
            }
        });
        messageCommands.put(MessageGameJoin.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                if (isAuthenticated()) {
                    MessageGameJoin mjc = (MessageGameJoin) message;
                    server.playerToChannel(ServerUser.this, mjc.getId());
                }
            }
        });
        messageCommands.put(MessageReplay.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                MessageReplay mrl = (MessageReplay) message;
                mrl.setSenderId(userInfo.getUserId());
                server.addSpReplay(mrl);
            }
        });
        messageCommands.put(MessageFetch.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                MessageFetch mf = (MessageFetch) message;
                server.fetchFromServer(mf, ServerUser.this);
            }
        });
        messageCommands.put(MessagePing.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                sendMessage(new MessagePong());
            }
        });
        messageCommands.put(MessageUserInfo.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                MessageUserInfo mpi = (MessageUserInfo) message;
                if (userInfo != null) {
                    userInfo.updateFrom(mpi.getUserInfo(), false);
                }
            }
        });
        messageCommands.put(MessageUnknown.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                MessageUnknown mu = (MessageUnknown) message;
                System.out.println("ServerUser::lineReceived: Unknown message: " + mu.getMess());
            }
        });
    }

    @Override
    public void messageReceived(Message message) {
        if (this.channel != null) {
            this.channel.receiveMessage(this, message);
        } else {
            MessageCommand command = messageCommands.get(message.getKey());
            if (command != null) {
                command.execute(message);
            } else {
                Logger.getLogger(ServerUser.class.getName()).log(Level.WARNING, "Unhandled message type: {0}", message.getKey());
            }
        }
    }

    @Override
    public void connectionClosed() {
        inputStopped();
    }

    @Override
    public void connectionLost(String reason) {
        inputStopped();
    }

    public void inputStopped() {
        if (channel != null) {
            channel.playerLeft(this, "nowhere (Disconnected)");
            channel = null;
        }
        connection.removeMessageListener(this);
        server.playerDisconnected(this);
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
        userInfo.setPlayerId(playerId);
    }

    public int getUserId() {
        return userInfo.getUserId();
    }

    public String getUserName() {
        return userInfo.getName();
    }

    public String getUserRights() {
        return userInfo.getUserRights();
    }

    public int getRank() {
        return userInfo.getRank();
    }

    public int getGames() {
        return userInfo.getGames();
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        System.out.println("ServerUser::setReady: " + getUserName() + ": ready: " + ready);
        this.ready = ready;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }
}
