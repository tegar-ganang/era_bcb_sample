package net.sf.jerkbot.commands.impl;

import jerklib.Session;
import net.sf.jerkbot.commands.MessageContext;

/**
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 *         MessageContext implementation
 * @version 0.0.1
 */
final class MessageContextImpl implements MessageContext {

    private String rawText;

    private String commandName;

    private String userTarget;

    private String channel;

    private String hostName;

    private String userName;

    private String sender;

    private String message;

    private int triggerCount;

    private boolean privateMessage;

    private Session session;

    public Session getSession() {
        return session;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getUserTarget() {
        return userTarget;
    }

    public String getMessage() {
        return message;
    }

    public String getSender() {
        return sender;
    }

    public String getChannel() {
        return channel;
    }

    public String getHostName() {
        return hostName;
    }

    public String getRawText() {
        return rawText;
    }

    public boolean isPrivateMessage() {
        return privateMessage;
    }

    public int getTriggerCount() {
        return triggerCount;
    }

    public String getUserName() {
        return userName;
    }

    static final class Builder {

        private MessageContextImpl impl = new MessageContextImpl();

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public MessageContext build() {
            return impl;
        }

        public Builder session(Session session) {
            impl.session = session;
            return this;
        }

        public Builder userTarget(String userTarget) {
            impl.userTarget = userTarget;
            return this;
        }

        public Builder commandName(String commandName) {
            impl.commandName = commandName;
            return this;
        }

        public Builder message(String message) {
            impl.rawText = message;
            return this;
        }

        public Builder channel(String channel) {
            impl.channel = channel;
            return this;
        }

        public Builder username(String userName) {
            impl.userName = userName;
            return this;
        }

        public Builder hostname(String hostName) {
            impl.hostName = hostName;
            return this;
        }

        public Builder sender(String sender) {
            impl.sender = sender;
            return this;
        }

        public Builder parsedMessage(String parsedMessage) {
            impl.message = parsedMessage;
            return this;
        }

        public Builder triggerCount(int triggerCount) {
            impl.triggerCount = triggerCount;
            return this;
        }

        public Builder privateMessage(boolean privateMessage) {
            impl.privateMessage = privateMessage;
            return this;
        }
    }
}
