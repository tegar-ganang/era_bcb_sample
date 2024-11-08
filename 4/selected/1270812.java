package de.iritgo.openmetix.comm.chat.chatter;

import de.iritgo.openmetix.comm.chat.command.UserLeaveCommand;
import de.iritgo.openmetix.comm.chat.gui.ChatGUI;
import de.iritgo.openmetix.core.Engine;
import de.iritgo.openmetix.core.flowcontrol.CountingFlowRule;
import de.iritgo.openmetix.core.flowcontrol.FlowControl;
import de.iritgo.openmetix.core.gui.IDesktopManager;
import de.iritgo.openmetix.core.gui.IDisplay;
import de.iritgo.openmetix.framework.IritgoEngine;
import de.iritgo.openmetix.framework.appcontext.AppContext;
import de.iritgo.openmetix.framework.client.Client;
import de.iritgo.openmetix.framework.user.User;
import de.iritgo.openmetix.framework.user.UserRegistry;
import java.util.Iterator;

public class ChatClientManager extends ChatManager {

    private String displayId;

    public ChatClientManager() {
        super("chat.client", Client.instance().getUserRegistry());
    }

    public ChatClientManager(String displayId) {
        this();
        this.displayId = displayId;
    }

    public void joinChannel(String channelName, long chatter, String chatterName) {
        if (getUserName(chatter) == null) {
            UserRegistry userRegistry = Client.instance().getUserRegistry();
            userRegistry.addUser(new User(chatterName, "", chatter, "", 0));
        }
        try {
            super.joinChannel(channelName, chatter);
        } catch (UserAllreadyJoindException x) {
        }
        ChatGUI chatGUI = getChatGUI();
        if (chatGUI != null) {
            chatGUI.joinChannel(channelName, chatterName);
        }
    }

    public void leaveChannel(Integer channelId, long chatter) {
        ChatChannel chatChannel = (ChatChannel) chatChannels.get(channelId);
        if (chatChannel != null) {
            chatChannel.removeChatter(new Long(chatter));
            UserRegistry userRegistry = Client.instance().getUserRegistry();
            User user = userRegistry.getUser(chatter);
            if (user == null) {
                return;
            }
            if (user.getUniqueId() == chatter) {
                chatChannels.remove(channelId);
            }
        }
    }

    public void leaveChannel(Integer channelId, long chatter, String chatterName) {
        Engine.instance().getFlowControl().ruleSuccess("shutdown.in.progress." + getTypeId());
        ChatGUI chatGUI = getChatGUI();
        if (chatGUI != null) {
            chatGUI.leaveChannel(channelId, chatterName);
        }
        leaveChannel(channelId, chatter);
    }

    public void messageChannel(String message, int channelId, long chatter, String chatterName) {
        super.messageChannel(message, channelId, chatter);
        ChatGUI chatGUI = getChatGUI();
        if (chatGUI != null) {
            chatGUI.addMessage(message, channelId, chatterName);
        }
    }

    private ChatGUI getChatGUI() {
        IDesktopManager displayManager = Client.instance().getClientGUI().getDesktopManager();
        IDisplay display = displayManager.getDisplay("common.chatview");
        if (display != null) {
            return (ChatGUI) display.getGUIPane();
        }
        return null;
    }

    private String getUserName(long userId) {
        UserRegistry userRegistry = Client.instance().getUserRegistry();
        User user = userRegistry.getUser(userId);
        if (user != null) {
            return user.getName();
        }
        return null;
    }

    public Integer getCurrentChannel() {
        ChatGUI chatGUI = getChatGUI();
        if (chatGUI != null) {
            return chatGUI.getCurrentChannel();
        }
        return new Integer(-1);
    }

    public void doShutdownNotify() {
        closeAllChannels();
    }

    public void closeAllChannels() {
        if (!AppContext.instance().isUserLoggedIn()) {
            return;
        }
        Object lockObject = AppContext.instance().getLockObject();
        synchronized (lockObject) {
            User user = AppContext.instance().getUser();
            int channels = 0;
            for (Iterator i = getChatChannelIterator(); i.hasNext(); ) {
                ChatChannel channel = null;
                try {
                    channel = (ChatChannel) i.next();
                } catch (Exception x) {
                    x.printStackTrace();
                    continue;
                }
                UserLeaveCommand userLeaveCommand = new UserLeaveCommand(channel.getChannelId());
                IritgoEngine.instance().getAsyncCommandProcessor().perform(userLeaveCommand);
                ++channels;
            }
            if (channels != 0) {
                FlowControl flowControll = Engine.instance().getFlowControl();
                flowControll.add(new CountingFlowRule("shutdown.in.progress." + getTypeId(), channels));
            }
        }
    }
}
