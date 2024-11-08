package com.google.code.cubeirc.connection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.net.ssl.SSLSocketFactory;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Level;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.exception.IrcException;
import org.pircbotx.exception.NickAlreadyInUseException;
import org.pircbotx.hooks.ListenerAdapter;
import com.google.code.cubeirc.base.Base;
import com.google.code.cubeirc.config.ConnectionSettings;
import com.google.code.cubeirc.config.UserSettings;
import com.google.code.cubeirc.connection.data.ChannelMessageResponse;
import com.google.code.cubeirc.connection.data.PrivateMessageResponse;
import com.google.code.cubeirc.connection.listeners.ChannelListener;
import com.google.code.cubeirc.connection.listeners.ConnectListener;
import com.google.code.cubeirc.connection.listeners.MOTDListener;
import com.google.code.cubeirc.connection.listeners.MessagesListener;
import com.google.code.cubeirc.connection.listeners.ServerListener;
import com.google.code.cubeirc.editor.TemplateManager;
import com.google.code.cubeirc.queue.MessageQueue;
import com.google.code.cubeirc.queue.MessageQueueEnum;
import com.google.code.cubeirc.queue.MessageQueueEvent;

public class Connection extends Base {

    @Getter
    @Setter
    public static CubeIRC ircclient = new CubeIRC();

    @Getter
    @Setter
    public static User userInfo = getIrcclient().getUserBot();

    @Getter
    @Setter
    public static Thread th_client;

    public Connection(String name) {
        super(name);
    }

    /**
	 * Try to connect to server passing ConnectionSettings
	 * @param cs
	 */
    private void AttemptConnect(final UserSettings us, final ConnectionSettings cs) {
        setTh_client(new Thread(new Runnable() {

            @Override
            public void run() {
                MessageQueue.addQueue(MessageQueueEnum.IRC_CONNECTING, cs);
                int tries = 0;
                boolean connected = false;
                while (!connected && tries < 3) try {
                    {
                        if (!cs.getServerPassword().equals("")) getIrcclient().connect(cs.getServer(), Integer.parseInt(cs.getPort()), cs.getServerPassword()); else if (cs.isSsl()) getIrcclient().connect(cs.getServer(), Integer.parseInt(cs.getPort()), SSLSocketFactory.getDefault()); else getIrcclient().connect(cs.getServer(), Integer.parseInt(cs.getPort()));
                        connected = true;
                        setUserInfo(getIrcclient().getUserBot());
                    }
                } catch (NumberFormatException ex) {
                } catch (NickAlreadyInUseException e) {
                } catch (IOException e) {
                } catch (IrcException e) {
                }
            }
        }));
        getTh_client().start();
    }

    public void Connect(UserSettings us, ConnectionSettings cs) {
        addDefaultListeners();
        setEncode();
        getIrcclient().changeNick(us.getNickname());
        getIrcclient().setAutoNickChange(true);
        getIrcclient().setName(us.getNickname());
        getIrcclient().setVersion(CubeIRC.VERSION);
        getIrcclient().setLogin(cs.getIdent());
        getIrcclient().setMessageDelay(0);
        try {
            AttemptConnect(us, cs);
        } catch (Exception ex) {
            addDebug(Level.ERROR, "Error during connecting to server %s error: %s", cs.getServer(), ex.getMessage());
        }
    }

    private void setEncode() {
        try {
            getIrcclient().setEncoding("utf-8");
        } catch (UnsupportedEncodingException ex) {
            addDebug(Level.ERROR, "Error during setting utf-8 on IRC client");
        }
    }

    public void addListenerHandler(ListenerAdapter<CubeIRC> listener) {
        addDebug(Level.DEBUG, "Adding listener adapter %s", listener.getClass().getName());
        getIrcclient().getListenerManager().addListener(listener);
    }

    public void SendMessage(String dest, String text) {
        getIrcclient().sendMessage(dest, text);
    }

    public void addDefaultListeners() {
        addDebug(Level.DEBUG, "Adding default listeners");
        addListenerHandler(new ConnectListener());
        addListenerHandler(new ChannelListener());
        addListenerHandler(new ServerListener());
        addListenerHandler(new MOTDListener());
        addListenerHandler(new MessagesListener());
        addDebug(Level.DEBUG, "Listeners: %s objects", getIrcclient().getListenerManager().getListeners().size());
    }

    public void Disconnect() {
        if (getIrcclient() != null) {
            if (getIrcclient().isConnected()) {
                addDebug(Level.DEBUG, "Disconnecting from server %s", getIrcclient().getServer());
                getIrcclient().disconnect();
            }
        }
    }

    public Boolean isConnected() {
        return getIrcclient().isConnected();
    }

    public static void Op(Channel channel, User user) {
        getIrcclient().op(channel, user);
    }

    public static void Deop(Channel channel, User user) {
        getIrcclient().deOp(channel, user);
    }

    public static void Voice(Channel channel, User user) {
        getIrcclient().voice(channel, user);
    }

    public static void Devoice(Channel channel, User user) {
        getIrcclient().deVoice(channel, user);
    }

    public static void Kick(Channel channel, User user, String reason) {
        if (reason != "") getIrcclient().kick(channel, user, reason); else getIrcclient().kick(channel, user);
    }

    public static void UpdateUsers(Channel channel) {
    }

    @Override
    public boolean Close() {
        Disconnect();
        setIrcclient(null);
        setTh_client(null);
        return true;
    }

    @Override
    public void actionPerformed(MessageQueueEvent e) {
        if (e.getMsgtype() == MessageQueueEnum.MSG_PRIVATE_OUT) {
            PrivateMessageResponse pmr = (PrivateMessageResponse) e.getData();
            SendMessage(pmr.getDestination().getNick(), TemplateManager.replace(pmr.getMessage()));
        }
        if (e.getMsgtype() == MessageQueueEnum.CHANNEL_USR_QUIT) {
            getIrcclient().partChannel((Channel) e.getData());
        }
        if (e.getMsgtype() == MessageQueueEnum.CHANNEL_USR_JOIN) {
            String channel = e.getData().toString();
            addDebug(Level.INFO, "Joining channel(s) %s", channel);
            if (channel.contains(",")) {
                String[] channels = channel.split(",");
                for (int i = 0; i < channels.length; i++) {
                    getIrcclient().joinChannel(channels[i]);
                }
            } else {
                getIrcclient().joinChannel(channel);
            }
        }
        if (e.getMsgtype() == MessageQueueEnum.MSG_CHANNEL_OUT) {
            ChannelMessageResponse mug = (ChannelMessageResponse) e.getData();
            getIrcclient().sendMessage(mug.getChannel().getName(), TemplateManager.replace(mug.getMessage()));
        }
        super.actionPerformed(e);
    }
}
