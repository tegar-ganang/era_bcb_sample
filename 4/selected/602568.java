package com.google.code.cubeirc.connection.listeners;

import org.apache.log4j.Level;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.IncomingFileTransferEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.events.UserModeEvent;
import com.google.code.cubeirc.connection.CubeIRC;
import com.google.code.cubeirc.connection.data.ChannelMessageResponse;
import com.google.code.cubeirc.connection.data.GenericTargetResponse;
import com.google.code.cubeirc.connection.data.PrivateMessageResponse;
import com.google.code.cubeirc.debug.DebuggerQueue;
import com.google.code.cubeirc.queue.MessageQueue;
import com.google.code.cubeirc.queue.MessageQueueEnum;

public class MessagesListener extends ListenerAdapter<CubeIRC> {

    @Override
    public void onPrivateMessage(PrivateMessageEvent<CubeIRC> event) throws Exception {
        DebuggerQueue.addDebug(this, Level.INFO, "Private message from %s: %s", event.getUser().getNick(), event.getMessage());
        MessageQueue.addQueue(MessageQueueEnum.MSG_PRIVATE_IN, new PrivateMessageResponse(event.getUser(), event.getBot().getUserBot(), event.getMessage()));
        super.onPrivateMessage(event);
    }

    @Override
    public void onMessage(MessageEvent<CubeIRC> event) throws Exception {
        DebuggerQueue.addDebug(this, Level.DEBUG, "Channel %s message from %s: %s", event.getChannel().getName(), event.getUser().getNick(), event.getMessage());
        MessageQueue.addQueue(MessageQueueEnum.MSG_CHANNEL_IN, new ChannelMessageResponse(event.getChannel(), event.getUser(), event.getMessage()));
        super.onMessage(event);
    }

    @Override
    public void onUserMode(UserModeEvent<CubeIRC> event) throws Exception {
        MessageQueue.addQueue(MessageQueueEnum.IRC_USERMODE, new GenericTargetResponse(event.getSource(), event.getTarget(), event.getMode()));
        super.onUserMode(event);
    }

    @Override
    public void onIncomingFileTransfer(IncomingFileTransferEvent<CubeIRC> event) throws Exception {
        MessageQueue.addQueue(MessageQueueEnum.DCC_FILE_INCOMING, event.getTransfer());
        super.onIncomingFileTransfer(event);
    }
}
