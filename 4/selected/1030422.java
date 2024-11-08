package jelb.features;

import java.util.Hashtable;
import jelb.common.IChatListener;
import jelb.common.IChatter;
import jelb.common.OpenChannels;
import jelb.messaging.GetActiveChannels;
import jelb.messaging.IMessageListener;
import jelb.messaging.RawText;
import jelb.netio.Protocol.Channel;

public class Chatting {

    private Hashtable<Channel, IChatListener> listeners;

    private OpenChannels channels;

    private IChatter chatter;

    public Chatting(IChatter chatter) {
        this.chatter = chatter;
        this.listeners = new Hashtable<Channel, IChatListener>();
        this.chatter.addListener(new IMessageListener<GetActiveChannels>() {

            @Override
            public GetActiveChannels getMessageInstance() {
                return new GetActiveChannels();
            }

            @Override
            public boolean handle(GetActiveChannels message) {
                channels = message.getOpenChannels();
                return false;
            }
        });
    }

    public Integer getOpenChannelIndex(Channel channel) {
        if (this.channels != null) return this.channels.getChannelIndex(channel);
        return null;
    }

    public boolean sayOnChannel(Channel channel, String message) {
        if (this.channels == null) return false;
        boolean canSayOnChannel = this.channels.contains(channel);
        if (!canSayOnChannel) canSayOnChannel = this.joinChannel(channel);
        if (canSayOnChannel) {
            this.chatter.send(new RawText(String.format("@@%s %s", channel.getStringIdentifier(), message)));
        }
        return canSayOnChannel;
    }

    public boolean joinChannel(Channel channel) {
        if (this.channels == null) return false;
        if (this.channels.hasFreeChannelToOpen()) {
            this.chatter.send(new RawText(String.format("#jc %s", channel.getStringIdentifier())));
            return true;
        }
        return false;
    }

    public void leaveChannel(Channel channel) {
        this.chatter.send(new RawText(String.format("#lc %s", channel.getStringIdentifier())));
    }

    public void registerChatListener(Channel channel, IChatListener chatlistener) {
        this.listeners.put(channel, chatlistener);
    }
}
