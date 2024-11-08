package comdemo.internal.media;

import org.red5.server.api.event.IEvent;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import comdemo.internal.feeds.AVFeed;

public class Handler implements IStreamListener {

    private AVFeed feed;

    private String channel;

    private long creationTime;

    public Handler(String channel, AVFeed feed, long creationTime) {
        this.channel = channel;
        this.feed = feed;
        this.creationTime = creationTime;
    }

    @Override
    public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
        IEvent event = (IEvent) packet;
        feed.dispatchAVEvent(this, event);
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getChannel() {
        return channel;
    }
}
