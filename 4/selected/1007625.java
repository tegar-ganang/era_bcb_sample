package com.google.code.cubeirc.connection.listeners;

import org.apache.log4j.Level;
import org.pircbotx.Channel;
import org.pircbotx.ReplyConstants;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.ServerResponseEvent;
import com.google.code.cubeirc.connection.CubeIRC;
import com.google.code.cubeirc.connection.data.ChannelErrorResponse;
import com.google.code.cubeirc.connection.data.GenericChannelResponse;
import com.google.code.cubeirc.connection.data.GenericUserResponse;
import com.google.code.cubeirc.connection.data.ServerResponse;
import com.google.code.cubeirc.debug.DebuggerQueue;
import com.google.code.cubeirc.queue.MessageQueue;
import com.google.code.cubeirc.queue.MessageQueueEnum;

public class ServerListener extends ListenerAdapter<CubeIRC> {

    @Override
    public void onServerResponse(ServerResponseEvent<CubeIRC> event) throws Exception {
        MessageQueue.addQueue(MessageQueueEnum.IRC_RESPONSE, new ServerResponse(event.getCode(), event.getResponse()));
        checkChannelError(event);
        super.onServerResponse(event);
    }

    @Override
    public void onNotice(NoticeEvent<CubeIRC> event) throws Exception {
        DebuggerQueue.addDebug(this, Level.INFO, "NOTICE: User %s Message %s", event.getUser().getNick(), event.getMessage());
        if (event.getChannel() != null) MessageQueue.addQueue(MessageQueueEnum.CHANNEL_NOTICE, new GenericChannelResponse(event.getChannel(), event.getUser(), event.getNotice())); else MessageQueue.addQueue(MessageQueueEnum.IRC_NOTICE, new GenericUserResponse(event.getUser(), event.getNotice()));
        super.onNotice(event);
    }

    private void checkChannelError(ServerResponseEvent<CubeIRC> event) {
        if (event.getCode() == ReplyConstants.ERR_INVITEONLYCHAN || event.getCode() == ReplyConstants.ERR_CHANNELISFULL || event.getCode() == ReplyConstants.ERR_NOPRIVILEGES) {
            String nick = event.getBot().getNick() + " ";
            String ev = event.getResponse().replace(nick, "");
            Channel ch = event.getBot().getChannel(ev.split(" :")[0]);
            String message = ev.split(" :")[1];
            MessageQueue.addQueue(MessageQueueEnum.IRC_ERROR_CHANNEL, new ChannelErrorResponse(event.getCode(), ch, message));
        }
    }
}
