package de.iritgo.openmetix.comm.chat.chatter;

import de.iritgo.openmetix.comm.chat.action.ChatCloseAction;
import de.iritgo.openmetix.comm.chat.action.ChatMessageAction;
import de.iritgo.openmetix.comm.chat.action.UserJoinAction;
import de.iritgo.openmetix.comm.chat.action.UserLeaveAction;
import de.iritgo.openmetix.core.action.AbstractAction;
import de.iritgo.openmetix.core.network.ClientTransceiver;
import de.iritgo.openmetix.framework.server.Server;
import de.iritgo.openmetix.framework.user.User;
import de.iritgo.openmetix.framework.user.UserRegistry;
import java.util.Iterator;

public class ChatServerManager extends ChatManager {

    public ChatServerManager() {
        super("chat.server", Server.instance().getUserRegistry());
    }

    public void joinChannel(String channelName, long chatter) {
        try {
            super.joinChannel(channelName, chatter);
            notifyMembersUserJoin(getChatChannel(channelName), new Long(chatter));
            notifyUserAboutAllUsers(getChatChannel(channelName), new Long(chatter));
        } catch (UserAllreadyJoindException x) {
        }
    }

    private void notifyMembersUserJoin(final ChatChannel chatChannel, Long userId) {
        ChatterProcessor chatterProcessor = new ChatterProcessor(userId, chatChannel);
        chatterProcessor.doProcessor(new Processor() {

            public AbstractAction getAction(User user, User newUser) {
                ClientTransceiver clientTransceiver = new ClientTransceiver(user.getNetworkChannel());
                clientTransceiver.addReceiver(user.getNetworkChannel());
                UserJoinAction action = new UserJoinAction(newUser.getName(), newUser.getUniqueId(), chatChannel.getName());
                action.setTransceiver(clientTransceiver);
                return action;
            }
        });
    }

    private void notifyUserAboutAllUsers(final ChatChannel chatChannel, Long userId) {
        UserRegistry userRegistry = Server.instance().getUserRegistry();
        User newUser = userRegistry.getUser(userId);
        final ClientTransceiver clientTransceiver = new ClientTransceiver(newUser.getNetworkChannel());
        clientTransceiver.addReceiver(newUser.getNetworkChannel());
        ChatterProcessor chatterProcessor = new ChatterProcessor(userId, chatChannel);
        chatterProcessor.doProcessor(new Processor() {

            public AbstractAction getAction(User user, User newUser) {
                UserJoinAction action = new UserJoinAction(user.getName(), user.getUniqueId(), chatChannel.getName());
                action.setTransceiver(clientTransceiver);
                return action;
            }
        });
    }

    public void leaveChannel(Integer channelId, long chatter) {
        notifyMembersUserLeave((ChatChannel) chatChannels.get(channelId), new Long(chatter));
        super.leaveChannel(channelId, chatter);
    }

    private void notifyMembersUserLeave(final ChatChannel chatChannel, Long userId) {
        ChatterProcessor chatterProcessor = new ChatterProcessor(userId, chatChannel);
        chatterProcessor.doProcessor(new Processor() {

            public AbstractAction getAction(User user, User newUser) {
                ClientTransceiver clientTransceiver = new ClientTransceiver(user.getNetworkChannel());
                clientTransceiver.addReceiver(user.getNetworkChannel());
                UserLeaveAction action = new UserLeaveAction(newUser.getName(), chatChannel.getName().hashCode(), newUser.getUniqueId());
                action.setTransceiver(clientTransceiver);
                return action;
            }
        });
    }

    public void messageChannel(String message, int channelName, long chatter) {
        notifyMembersMessage(message, getChatChannel(channelName), new Long(chatter));
    }

    private void notifyMembersMessage(final String message, final ChatChannel chatChannel, final Long userId) {
        ChatterProcessor chatterProcessor = new ChatterProcessor(userId, chatChannel);
        chatterProcessor.doProcessor(new Processor() {

            public AbstractAction getAction(User user, User newUser) {
                ClientTransceiver clientTransceiver = new ClientTransceiver(user.getNetworkChannel());
                clientTransceiver.addReceiver(user.getNetworkChannel());
                ChatMessageAction action = new ChatMessageAction(message, chatChannel.getChannelId(), userId.longValue(), newUser.getName());
                action.setTransceiver(clientTransceiver);
                return action;
            }
        });
    }

    public void onShutdown() {
        for (Iterator i = chatChannels.values().iterator(); i.hasNext(); ) {
            ChatChannel channel = (ChatChannel) i.next();
            final int channelName = getChatChannel(channel.getName()).getChannelId();
            ChatterProcessor chatterProcessor = new ChatterProcessor(new Long(-1), channel);
            chatterProcessor.doProcessor(new Processor() {

                public AbstractAction getAction(User user, User newUser) {
                    ClientTransceiver clientTransceiver = new ClientTransceiver(user.getNetworkChannel());
                    clientTransceiver.addReceiver(user.getNetworkChannel());
                    ChatCloseAction action = new ChatCloseAction(user.getUniqueId(), channelName);
                    action.setTransceiver(clientTransceiver);
                    return action;
                }
            });
        }
        super.onShutdown();
    }

    public void onUserLogoff(User user) {
        for (Iterator i = chatChannels.values().iterator(); i.hasNext(); ) {
            final ChatChannel channel = (ChatChannel) i.next();
            if (channel.existsChatterInChannel(new Long(user.getUniqueId()))) {
                notifyMembersUserLeave(channel, new Long(user.getUniqueId()));
            }
        }
        super.onUserLogoff(user);
    }
}
