package org.boticelli.plugin.chat;

import org.boticelli.util.Util;
import f00f.net.irc.martyr.InCommand;
import f00f.net.irc.martyr.commands.ChannelModeCommand;
import f00f.net.irc.martyr.commands.CtcpMessage;
import f00f.net.irc.martyr.commands.JoinCommand;
import f00f.net.irc.martyr.commands.KickCommand;
import f00f.net.irc.martyr.commands.MessageCommand;
import f00f.net.irc.martyr.commands.NickCommand;
import f00f.net.irc.martyr.commands.QuitCommand;
import f00f.net.irc.martyr.commands.TopicCommand;

public class ChatMessage {

    private MessageType type;

    private String source, message;

    public ChatMessage(MessageType type, String source, String message) {
        this.type = type;
        this.source = source;
        this.message = message;
    }

    public static ChatMessage create(InCommand command, String channelName, boolean isPrivate) {
        if (command instanceof CtcpMessage) {
            MessageCommand action = (MessageCommand) command;
            return new ChatMessage(MessageType.ACTION, action.getSource().getSource(), action.getMessage());
        } else if (command instanceof MessageCommand) {
            MessageCommand msg = (MessageCommand) command;
            return new ChatMessage(isPrivate ? MessageType.PRIVMSG : MessageType.MESSAGE, msg.getSource().getSource(), msg.getMessage());
        } else if (command instanceof JoinCommand) {
            JoinCommand join = (JoinCommand) command;
            return new ChatMessage(MessageType.INFO, join.getUser().getSource(), "has joined " + join.getChannel());
        } else if (command instanceof QuitCommand) {
            QuitCommand quit = (QuitCommand) command;
            return new ChatMessage(MessageType.INFO, quit.getUser().getSource(), "has left " + channelName + " : " + quit.getReason());
        } else if (command instanceof KickCommand) {
            KickCommand kick = (KickCommand) command;
            return new ChatMessage(MessageType.INFO, kick.getKicker().getSource(), "has kicked " + kick.getKicked().getSource() + " from " + kick.getChannel() + " : " + kick.getComment());
        } else if (command instanceof NickCommand) {
            NickCommand nick = (NickCommand) command;
            return new ChatMessage(MessageType.INFO, nick.getOldNick(), "is now known as " + nick.getNick());
        } else if (command instanceof ChannelModeCommand) {
            ChannelModeCommand mode = (ChannelModeCommand) command;
            String source = mode.getPrefix();
            if (source == null) {
                source = "null";
            }
            return new ChatMessage(MessageType.INFO, source, " sets " + mode.render());
        } else if (command instanceof TopicCommand) {
            TopicCommand topic = (TopicCommand) command;
            String source = Util.split(topic.getSourceString().substring(1), " ").get(0);
            return new ChatMessage(MessageType.INFO, source, "changes the topic to: " + topic.getTopic());
        }
        return null;
    }

    public MessageType getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return super.toString() + ": type = " + type + ", source = " + source + ", message = " + message;
    }
}
