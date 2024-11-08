package ar.edu.unicen.exa.server.communication.processors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import ar.edu.unicen.exa.server.AppListenerImpl;
import ar.edu.unicen.exa.server.communication.tasks.CleanupChannelTask;
import ar.edu.unicen.exa.server.player.Player;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.app.NameNotBoundException;
import com.sun.sgs.app.ObjectNotFoundException;
import common.elearning.chat.ChatCommands;
import common.messages.IMessage;
import common.messages.requests.MsgChat;
import common.processors.IProcessor;

public class PChat extends ServerMsgProcessor {

    private static PChat INSTANCE = new PChat();

    private static final Logger LOGGER = Logger.getLogger(PChat.class.getName());

    private final List<String> channelsCreated = new ArrayList<String>();

    public PChat() {
        super();
        channelsCreated.add(ChatCommands.GLOBAL_CHANNEL_NAME);
    }

    @Override
    public IProcessor factoryMethod() {
        return INSTANCE;
    }

    public static PChat getINSTANCE() {
        return INSTANCE;
    }

    public static void setINSTANCE(PChat iNSTANCE) {
        INSTANCE = iNSTANCE;
    }

    public void removeChannelfromlist(String channel) {
        this.channelsCreated.remove(channel);
    }

    /**
	 * Echos the given string back to the sending session as a direct message.
	 * 
	 * @param message
	 *            the message to echo
	 */
    private void echo(String message) {
        MsgChat m = new MsgChat();
        m.setCommand(ChatCommands.PONG);
        m.setMsg(message);
        this.getPlayerAssociated().getSession().send(m.toByteBuffer());
    }

    /**
	 * Handle a private message. First, decode it to find the target
	 * ClientSession. Then repackage the message to include the sender's id, and
	 * send to the target.
	 * 
	 * @param message
	 *            the private message
	 */
    private void pmReceived(MsgChat msg) {
        String targetId = msg.getMsg();
        this.addToChannel(ChatCommands.PM_PREFIX + msg.getSender() + targetId);
        Player target = (Player) AppContext.getDataManager().getBinding(targetId);
        MsgChat newMsg = new MsgChat();
        newMsg.setCommand(ChatCommands.PM);
        newMsg.setSender(this.getPlayerAssociated().getSession().getName());
        newMsg.setMsg(targetId);
        target.send(newMsg);
    }

    private void addToChannel(String channelName) {
        ChannelManager channelMgr = AppContext.getChannelManager();
        Channel channel;
        boolean newChannel = false;
        try {
            channel = channelMgr.getChannel(channelName);
        } catch (NameNotBoundException e) {
            channel = channelMgr.createChannel(channelName, new ar.edu.unicen.exa.server.chat.ChatChannelListener(), Delivery.RELIABLE);
            newChannel = true;
            if (!this.channelsCreated.contains(channelName)) {
                channelsCreated.add(channelName);
            }
            LOGGER.info("New channel created: " + channelName);
        }
        MsgChat changeMsg = new MsgChat(ChatCommands.JOINED, channelName, this.getPlayerAssociated().getSession().getName());
        channel.send(null, changeMsg.toByteBuffer());
        channel.join(this.getPlayerAssociated().getSession());
        if (newChannel) {
            sendNewChannelNotification(channelName);
        }
        sendMembersNotification(channel);
    }

    private void removeFromChannel(String channelName) {
        ChannelManager channelMgr = AppContext.getChannelManager();
        Channel channel;
        MsgChat m = new MsgChat();
        try {
            channel = channelMgr.getChannel(channelName);
        } catch (NameNotBoundException e) {
            return;
        }
        if (this.getPlayerAssociated().getSession().isConnected()) {
            channel.leave(this.getPlayerAssociated().getSession());
        }
        m.setCommand(ChatCommands.LEFT);
        m.setSender(channel.getName());
        m.setMsg(this.getPlayerAssociated().getSession().getName());
        channel.send(null, m.toByteBuffer());
        AppContext.getTaskManager().scheduleTask(new CleanupChannelTask(m, channelName));
    }

    private void sendNewChannelNotification(String newchannel) {
        Channel global = AppContext.getChannelManager().getChannel(ChatCommands.GLOBAL_CHANNEL_NAME);
        MsgChat newChannelmsg = new MsgChat(ChatCommands.NEW_CHANNEL, "", newchannel);
        global.send(null, newChannelmsg.toByteBuffer());
    }

    private void sendMembersNotification(Channel channel) {
        MsgChat m = new MsgChat();
        m.setCommand(ChatCommands.MEMBERS);
        StringBuilder listMessage = new StringBuilder();
        m.setSender(channel.getName());
        Iterator<ClientSession> iter = channel.getSessions();
        while (iter.hasNext()) {
            ClientSession member = iter.next();
            listMessage.append(member.getName());
            listMessage.append(' ');
        }
        m.setMsg(listMessage.toString());
        this.getPlayerAssociated().getSession().send(m.toByteBuffer());
    }

    private void disconnect() {
        try {
            AppContext.getDataManager().removeObject(this.getPlayerAssociated().getSession());
        } catch (ObjectNotFoundException e) {
        }
    }

    private void channelMsgReceived(String channel, String msg) {
        ChannelManager channelMgr = AppContext.getChannelManager();
        Channel ch = channelMgr.getChannel(channel);
        MsgChat message = new MsgChat();
        message.setCommand(ChatCommands.CHANNEL_MSG);
        message.setMsg(channel + " " + msg);
        message.setSender(this.getPlayerAssociated().getSession().getName());
        ch.send(null, message.toByteBuffer());
    }

    private void sendChannelList() {
        MsgChat msg = new MsgChat();
        msg.setCommand(ChatCommands.CHANNEL_LIST);
        String channelsList = "";
        for (String channel : channelsCreated) {
            channelsList += channel + " ";
        }
        msg.setMsg(channelsList.trim());
        this.getPlayerAssociated().send(msg);
    }

    @Override
    public void process(IMessage msg) {
        MsgChat m = (MsgChat) msg;
        String[] args = m.getMsg().split(" ", 2);
        if (m.getCommand().equals(ChatCommands.JOIN)) {
            addToChannel(m.getMsg());
        } else if (m.getCommand().equals(ChatCommands.JOIN_GLOBAL)) {
            addToChannel(ChatCommands.GLOBAL_CHANNEL_NAME);
            sendChannelList();
        } else if (m.getCommand().equals(ChatCommands.LEAVE)) {
            removeFromChannel(m.getMsg());
        } else if (m.getCommand().equals(ChatCommands.PING)) {
            echo(m.getMsg());
        } else if (m.getCommand().equals(ChatCommands.PM)) {
            pmReceived(m);
        } else if (m.getCommand().equals(ChatCommands.CHANNEL_MSG)) {
            channelMsgReceived(args[0], args[1]);
        } else if (m.getCommand().equals(ChatCommands.DISCONNECT)) {
            disconnect();
        } else {
            System.out.println("Comando invalido");
        }
    }
}
