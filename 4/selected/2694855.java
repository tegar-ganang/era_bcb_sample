package com.google.code.cubeirc.connection.listeners;

import java.util.Iterator;
import org.apache.log4j.Level;
import org.pircbotx.ChannelListEntry;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ChannelInfoEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.ModeEvent;
import org.pircbotx.hooks.events.OpEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.TopicEvent;
import org.pircbotx.hooks.events.UserListEvent;
import org.pircbotx.hooks.events.VoiceEvent;
import com.google.code.cubeirc.connection.CubeIRC;
import com.google.code.cubeirc.connection.data.ChannelMessageResponse;
import com.google.code.cubeirc.connection.data.ChannelOperationEnum;
import com.google.code.cubeirc.connection.data.ChannelOperationResponse;
import com.google.code.cubeirc.connection.data.ChannelResponse;
import com.google.code.cubeirc.connection.data.ChannelTopicResponse;
import com.google.code.cubeirc.connection.data.GenericChannelDataResponse;
import com.google.code.cubeirc.connection.data.GenericUserResponse;
import com.google.code.cubeirc.debug.DebuggerQueue;
import com.google.code.cubeirc.queue.MessageQueue;
import com.google.code.cubeirc.queue.MessageQueueEnum;

public class ChannelListener extends ListenerAdapter<CubeIRC> {

    @Override
    public void onJoin(JoinEvent<CubeIRC> event) throws Exception {
        MessageQueue.addQueue(MessageQueueEnum.CHANNEL_JOIN, new ChannelResponse(event.getChannel(), event.getUser(), event.getTimestamp()));
        DebuggerQueue.addDebug(this, Level.INFO, "User %s join channel %s", event.getUser().getNick(), event.getChannel().getName());
        super.onJoin(event);
    }

    @Override
    public void onPart(PartEvent<CubeIRC> event) throws Exception {
        MessageQueue.addQueue(MessageQueueEnum.CHANNEL_PART, new ChannelResponse(event.getChannel(), event.getUser(), event.getTimestamp()));
        DebuggerQueue.addDebug(this, Level.INFO, "User %s part channel %s", event.getUser().getNick(), event.getChannel().getName());
        super.onPart(event);
    }

    @Override
    public void onTopic(TopicEvent<CubeIRC> event) throws Exception {
        MessageQueue.addQueue(MessageQueueEnum.CHANNEL_TOPIC, new ChannelTopicResponse(event.getChannel(), event.getTopic(), event.getUser().getNick(), event.getDate()));
        super.onTopic(event);
    }

    @Override
    public void onQuit(QuitEvent<CubeIRC> event) throws Exception {
        MessageQueue.addQueue(MessageQueueEnum.USER_QUIT, new GenericUserResponse(event.getUser(), event.getReason()));
        super.onQuit(event);
    }

    @Override
    public void onUserList(UserListEvent<CubeIRC> event) throws Exception {
        MessageQueue.addQueue(MessageQueueEnum.CHANNEL_USERLIST, event.getChannel());
        super.onUserList(event);
    }

    @Override
    public void onMode(ModeEvent<CubeIRC> event) throws Exception {
        MessageQueue.addQueue(MessageQueueEnum.IRC_MODE, new ChannelMessageResponse(event.getChannel(), event.getUser(), event.getMode()));
        super.onMode(event);
    }

    @Override
    public void onOp(OpEvent<CubeIRC> event) throws Exception {
        ChannelOperationEnum co = null;
        if (event.isOp()) co = ChannelOperationEnum.OP; else co = ChannelOperationEnum.DEOP;
        MessageQueue.addQueue(MessageQueueEnum.CHANNEL_OPERATION, new ChannelOperationResponse(event.getChannel(), event.getSource(), event.getRecipient(), co));
        super.onOp(event);
    }

    @Override
    public void onVoice(VoiceEvent<CubeIRC> event) throws Exception {
        ChannelOperationEnum co = null;
        if (event.hasVoice()) co = ChannelOperationEnum.VOICE; else co = ChannelOperationEnum.DEVOICE;
        MessageQueue.addQueue(MessageQueueEnum.CHANNEL_OPERATION, new ChannelOperationResponse(event.getChannel(), event.getSource(), event.getRecipient(), co));
        super.onVoice(event);
    }

    @Override
    public void onChannelInfo(ChannelInfoEvent<CubeIRC> event) throws Exception {
        for (Iterator<ChannelListEntry> it = event.getList().iterator(); it.hasNext(); ) {
            ChannelListEntry cle = it.next();
            MessageQueue.addQueue(MessageQueueEnum.CHANNEL_LIST, new GenericChannelDataResponse(cle.getName(), cle.getUsers(), cle.getTopic()));
        }
        super.onChannelInfo(event);
    }
}
