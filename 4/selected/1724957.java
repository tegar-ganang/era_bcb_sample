package org.gamio.server;

import java.util.List;
import org.gamio.buffer.Buffer;
import org.gamio.channel.Channel;
import org.gamio.channel.ChannelMsgListener;
import org.gamio.mq.InternalMessage;
import org.gamio.mq.MessageQueue;
import org.gamio.system.Context;

/**
 * @author Agemo Cui <agemocui@gamio.org>
 * @version $Rev: 19 $ $Date: 2008-09-26 19:00:58 -0400 (Fri, 26 Sep 2008) $
 */
public final class ServerChannelMsgListener implements ChannelMsgListener {

    public void onMessage(Channel channel, List<Buffer> dataList) {
        MessageQueue mq = Context.getInstance().getMessageQueue();
        String sessionId = channel.getSessionId();
        Integer originalChannelId = channel.getOriginalChannelId();
        String localAddr = channel.getLocalAddr();
        int localPort = channel.getLocalPort();
        String remoteAddr = channel.getRemoteAddr();
        int remotePort = channel.getRemotePort();
        String gateId = channel.getGateId();
        for (Buffer data : dataList) {
            InternalMessage internalMessage = mq.createInternalMessage();
            internalMessage.setSessionId(sessionId);
            internalMessage.setOriginalChannelId(originalChannelId);
            internalMessage.setLocalAddr(localAddr);
            internalMessage.setLocalPort(localPort);
            internalMessage.setRemoteAddr(remoteAddr);
            internalMessage.setRemotePort(remotePort);
            internalMessage.addData(data);
            mq.send(internalMessage, gateId);
        }
    }

    public void onMessageComplete(Channel channel) {
        channel.reactivate();
        Context.getInstance().getChannelManager().onReadRequired(channel);
    }
}
