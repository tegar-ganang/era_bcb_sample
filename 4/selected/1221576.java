package org.dreamspeak.lib;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Observable;
import java.util.Observer;
import org.dreamspeak.lib.audio.InboundVoiceStream;
import org.dreamspeak.lib.data.Channel;
import org.dreamspeak.lib.data.ChannelList;
import org.dreamspeak.lib.data.Message;
import org.dreamspeak.lib.data.Player;
import org.dreamspeak.lib.data.PlayerLeftReason;
import org.dreamspeak.lib.data.PlayerList;
import org.dreamspeak.lib.data.Version;
import org.dreamspeak.lib.events.ContentRecievedListener;
import org.dreamspeak.lib.events.VoiceRecievedListener;
import org.dreamspeak.lib.protocol.LoginState;
import org.dreamspeak.lib.protocol.LoginType;
import org.dreamspeak.lib.protocol.ProtocolException;
import org.dreamspeak.lib.protocol.ReadWorker;
import org.dreamspeak.lib.protocol.SessionLayer;
import org.dreamspeak.lib.protocol.WriteWorker;
import org.dreamspeak.lib.protocol.packets.inbound.InboundPacket;
import org.dreamspeak.lib.protocol.packets.inbound.LoginReply;
import org.dreamspeak.lib.protocol.packets.inbound.Voice;
import org.dreamspeak.lib.protocol.packets.inbound.reliablecontent.ChannelListUpdate;
import org.dreamspeak.lib.protocol.packets.inbound.reliablecontent.MessageRecieved;
import org.dreamspeak.lib.protocol.packets.inbound.reliablecontent.PlayerLeft;
import org.dreamspeak.lib.protocol.packets.inbound.reliablecontent.ReliableContent;
import org.dreamspeak.lib.protocol.packets.inbound.reliablecontent.PlayerListUpdate;
import org.dreamspeak.lib.protocol.packets.outbound.OutboundPacket;
import org.dreamspeak.lib.protocol.packets.outbound.ReliableOutboundPacket;
import org.dreamspeak.lib.protocol.packets.outbound.SendLogin;
import org.dreamspeak.lib.protocol.packets.outbound.reliablecontent.SendDisconnectContent;
import org.dreamspeak.lib.protocol.packets.outbound.reliablecontent.SendLoginChannelContent;
import org.dreamspeak.lib.protocol.packets.outbound.reliablecontent.SendMessage;

/**
 * TODO: Proper documentation
 * 
 * @author avithan
 */
public class Client {

    /**
	 * The default name of the client application that is told to the Server.
	 */
    public static final String DEFAULT_CLIENT_APPLICATION = "DreamSpeak";

    protected DatagramChannel udpChannel;

    protected InetSocketAddress serverAddress;

    protected String nickname;

    protected String login;

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (this.loginState != LoginState.DISCONNECTED) throw new IllegalStateException("Password cannot be changed while connected.");
        this.password = password;
    }

    public void setLogin(String login) {
        if (this.loginState != LoginState.DISCONNECTED) throw new IllegalStateException("Login cannot be changed while connected.");
        this.login = login;
    }

    public String getDefaultChannel() {
        return defaultChannel;
    }

    public void setDefaultChannel(String defaultChannel) {
        if (this.loginState != LoginState.DISCONNECTED) throw new IllegalStateException("DefaultChannel cannot be changed while connected.");
        this.defaultChannel = defaultChannel;
    }

    public String getDefaultChannelPassword() {
        return defaultChannelPassword;
    }

    public void setDefaultChannelPassword(String defaultChannelPassword) {
        if (this.loginState != LoginState.DISCONNECTED) throw new IllegalStateException("DefaultChannelPassword cannot be changed while connected.");
        this.defaultChannelPassword = defaultChannelPassword;
    }

    public LoginType getLoginType() {
        return loginType;
    }

    public void setLoginType(LoginType loginType) {
        if (this.loginState != LoginState.DISCONNECTED) throw new IllegalStateException("LoginType cannot be changed while connected.");
        this.loginType = loginType;
    }

    String password;

    String defaultChannel;

    String defaultChannelPassword;

    String clientApplicationName;

    Version clientVersion;

    String clientOS;

    LoginState loginState;

    LoginType loginType;

    ReadWorker readWorker;

    WriteWorker writeWorker;

    SessionLayer sessionLayer;

    final ChannelList channelList;

    final PlayerList playerList;

    Version serverVersion;

    String serverName;

    String serverWelcomeMessage;

    Player player;

    final InboundVoiceStream inboundVoiceStream;

    int clientAcknowledgmentNumber;

    int sessionKey;

    int clientId;

    protected boolean autoReconnect;

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public Channel getCurrentChannel() {
        if (player == null) return null;
        return player.getCurrentChannel();
    }

    public Player getPlayer() {
        return player;
    }

    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    /**
	 * Creates a new Dreamspeak client object.
	 * 
	 * @param serverAddress
	 * @param nickname
	 * @param login
	 * @param password
	 * @param defaultChannel
	 * @param defaultChannelPassword
	 * @param clientApplication
	 * @param clientOS
	 * @param connect
	 */
    public Client(InetSocketAddress serverAddress, String nickname, String login, String password, String defaultChannel, String defaultChannelPassword, String clientApplicationName, String clientOS, Version clientVersion) {
        channelList = new ChannelList();
        playerList = new PlayerList(channelList);
        this.serverAddress = serverAddress;
        this.nickname = nickname;
        this.login = login;
        this.password = password;
        this.clientApplicationName = clientApplicationName;
        this.clientOS = clientOS;
        this.clientVersion = clientVersion;
        this.defaultChannel = defaultChannel;
        this.serverName = null;
        this.serverVersion = null;
        this.serverWelcomeMessage = null;
        inboundVoiceStream = new InboundVoiceStream();
        loginType = (login == null) ? LoginType.Anonymous : LoginType.Account;
        loginState = LoginState.DISCONNECTED;
        clientAcknowledgmentNumber = 1;
    }

    public Client(InetSocketAddress serverAddress, String nickname, String login, String password, String defaultChannel, String defaultChannelPassword) {
        this(serverAddress, nickname, login, password, defaultChannel, defaultChannelPassword, DEFAULT_CLIENT_APPLICATION, System.getProperty("os.name"), Version.DEFAULT_CLIENT_VERSION);
    }

    public Client(InetSocketAddress serverAddress, String nickname, String login, String password) {
        this(serverAddress, nickname, login, password, null, null);
    }

    public Client(InetSocketAddress serverAddress, String nickname) {
        this(serverAddress, nickname, null, null);
    }

    public Client(String hostname, int port, String nickname) {
        this(new InetSocketAddress(hostname, port), nickname);
    }

    public Client(String hostname, String nickname) {
        this(new InetSocketAddress(hostname, 8767), nickname);
    }

    public Client() {
        this(null, null, null, null, null, null, null, null, null);
    }

    public void connect() throws IllegalStateException, IOException, ProtocolException {
        if (loginState != LoginState.DISCONNECTED) {
            throw new IllegalStateException("The client is no longer at init state. Maybe it is already connected?");
        }
        udpChannel = DatagramChannel.open();
        udpChannel.socket().setSoTimeout(SOCKET_READ_TIMEOUT);
        udpChannel.configureBlocking(true);
        udpChannel.connect(serverAddress);
        writeWorker = new WriteWorker(udpChannel);
        writeWorker.startWorking();
        OutboundPacket cp = new SendLogin(this.clientApplicationName, this.clientOS, this.nickname, this.login, this.password, this.clientVersion);
        writeWorker.writePacketSynchroniously(cp);
        ByteBuffer b = ByteBuffer.allocate(512);
        int loginReplySize = udpChannel.read(b);
        b.limit(loginReplySize);
        LoginReply p = null;
        try {
            p = (LoginReply) InboundPacket.getPacketHandler(b);
        } catch (ProtocolException pe) {
            throw pe;
        }
        if (p.getLoginState() != LoginState.LOGGEDIN) {
            throw new ProtocolException("Could not join server. Reason: " + p.getLoginState().toString());
        }
        sessionKey = p.getSessionKey();
        clientId = p.getClientId();
        serverName = p.getServerName();
        serverWelcomeMessage = p.getWelcomeMessage();
        serverVersion = p.getServerVersion();
        readWorker = new ReadWorker(udpChannel);
        readWorker.startWorking();
        sessionLayer = new SessionLayer(sessionKey, clientId, readWorker, writeWorker);
        sessionLayer.onTimeout.addObserver(onServerTimeout);
        sessionLayer.addContentRecievedListener(onServerContent);
        sessionLayer.addVoiceRecievedListener(onVoiceData);
        sessionLayer.addVoiceRecievedListener(inboundVoiceStream.onVoiceRecieved);
        sessionLayer.startWorking();
        SendLoginChannelContent sendLoginChannelPayload = new SendLoginChannelContent();
        OutboundPacket[] slc = ReliableOutboundPacket.encapsulatePayload(sessionKey, clientId, clientAcknowledgmentNumber++, sendLoginChannelPayload);
        writeWorker.writePacketSynchroniously(slc);
    }

    final Observer onServerTimeout = new Observer() {

        public void update(Observable o, Object arg) {
            try {
                if (isAutoReconnect()) {
                    reconnect();
                } else {
                    disconnect(false);
                }
            } catch (IOException pe) {
                pe.printStackTrace();
            }
        }
    };

    /**
	 * Default handler for ChannelList updates - may be changed by the user.
	 */
    final ContentRecievedListener onChannelListUpdate = new ContentRecievedListener() {

        public void onContentRecieved(ReliableContent content) {
            ChannelListUpdate rplu = (ChannelListUpdate) content;
            rplu.processUpdate(channelList);
        }
    };

    /**
	 * TODO: make more general
	 */
    public void sendMessage(Player p, String message) {
        SendMessage sendMessagePayload = SendMessage.toPlayer(p, message);
        OutboundPacket[] slc = ReliableOutboundPacket.encapsulatePayload(sessionKey, clientId, clientAcknowledgmentNumber++, sendMessagePayload);
        try {
            writeWorker.writePacketSynchroniously(slc);
        } catch (IOException ioe) {
        }
    }

    /**
	 * Default handler for PlayerList updates - may be changed by the user.
	 */
    final ContentRecievedListener onPlayerListUpdate = new ContentRecievedListener() {

        public void onContentRecieved(ReliableContent content) {
            PlayerListUpdate rplu = (PlayerListUpdate) content;
            rplu.processUpdate(getPlayerList());
            if (player == null) {
                player = playerList.get(clientId);
            }
            if (content instanceof PlayerLeft) {
                PlayerLeft pl = (PlayerLeft) content;
                if (pl.getPlayerId() == clientId && pl.getLeftReason() == PlayerLeftReason.Kicked) {
                    try {
                        disconnect(false);
                        return;
                    } catch (IOException ioe) {
                    }
                } else if (pl.getLeftReason() == PlayerLeftReason.ServerClosed) {
                    try {
                        disconnect(false);
                        return;
                    } catch (IOException ioe) {
                    }
                }
            }
        }
    };

    /**
	 * Default handler for incoming Messages
	 */
    final ContentRecievedListener onMessageRecieved = new ContentRecievedListener() {

        public void onContentRecieved(ReliableContent content) {
            MessageRecieved mr = (MessageRecieved) content;
            Message message = mr.getMessage();
            message.getSender();
        }
    };

    final ContentRecievedListener onServerContent = new ContentRecievedListener() {

        public void onContentRecieved(ReliableContent content) {
            if (content instanceof ChannelListUpdate && onChannelListUpdate != null) {
                onChannelListUpdate.onContentRecieved(content);
            } else if (content instanceof PlayerListUpdate && onPlayerListUpdate != null) {
                onPlayerListUpdate.onContentRecieved(content);
            } else if (content instanceof MessageRecieved && onMessageRecieved != null) {
                onMessageRecieved.onContentRecieved(content);
            }
        }
    };

    final VoiceRecievedListener onVoiceData = new VoiceRecievedListener() {

        public void onVoiceDataRecieved(Voice voiceData) {
            Player player = playerList.get(voiceData.getClientId());
            player.setTalking();
        }
    };

    public InboundVoiceStream getInboundVoiceStream() {
        return inboundVoiceStream;
    }

    public static final long DISCONNECT_ACK_TIMEOUT = 500;

    public static final int SOCKET_READ_TIMEOUT = 10000;

    public void disconnect(boolean sayGoodbye) throws IOException {
        try {
            if (sayGoodbye) {
                ReliableOutboundPacket[] disconnectPacket = ReliableOutboundPacket.encapsulatePayload(sessionKey, clientId, clientAcknowledgmentNumber, new SendDisconnectContent());
                writeWorker.writePacketSynchroniously(disconnectPacket);
                synchronized (this) {
                    try {
                        wait(DISCONNECT_ACK_TIMEOUT);
                    } catch (InterruptedException ie) {
                    }
                }
            }
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            sessionLayer.stopWorking();
            readWorker.stopWorking();
            writeWorker.stopWorking();
            sessionLayer = null;
            readWorker = null;
            writeWorker = null;
            udpChannel.close();
            playerList.clear();
            channelList.clear();
            loginState = LoginState.DISCONNECTED;
        }
    }

    public void reconnect() throws IOException {
        disconnect(true);
        this.clientAcknowledgmentNumber = 1;
        connect();
    }

    public void setTimeoutInterval(int timeoutInterval) {
        sessionLayer.setTimeoutInterval(timeoutInterval);
    }

    public int getTimeoutInterval() {
        return sessionLayer.getTimeoutInterval();
    }

    public ChannelList getChannelList() {
        return channelList;
    }

    public PlayerList getPlayerList() {
        return playerList;
    }
}
