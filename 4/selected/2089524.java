package comdemo.internal.feeds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.red5.server.api.event.IEvent;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import com.thebitstream.comserver.feeds.IResourceFeed;
import com.thebitstream.comserver.nodes.IComserverNode;
import com.thebitstream.comserver.stream.IResourceSink;
import comdemo.internal.media.Handler;

public class AVFeed implements IResourceFeed {

    public List<IResourceSink> sinks = new ArrayList<IResourceSink>();

    private String channel;

    public String getChannel() {
        return channel;
    }

    public List<String> channels = new ArrayList<String>();

    public synchronized void setChannel(String chan) {
        Map<Object, Object> data = new HashMap<Object, Object>();
        data.put("channel", chan);
        sinks.get(0).sendEvent("changeChannel", data);
        this.channel = chan;
    }

    public void removeChannel(String chan) {
        channels.remove(chan);
        if (channel.equals(chan)) {
            getRandomChan();
        }
    }

    public void getRandomChan() {
        int y = channels.size();
        if (y == 0) {
            Map<Object, Object> data = new HashMap<Object, Object>();
            sinks.get(0).sendEvent("blankChannel", data);
            return;
        }
        int rnd = (int) Math.round(Math.random() * (y - 1));
        setChannel(channels.get(rnd));
    }

    public void addChannel(String chan) {
        channels.add(chan);
    }

    public synchronized void dispatchAVEvent(Handler handler, IEvent event) {
        if (channel.equals(handler.getChannel())) {
            long t1 = sinks.get(0).getStream().getCreationTime();
            long t2 = handler.getCreationTime();
            long delta = t2 - t1;
            int t3 = ((IRTMPEvent) event).getTimestamp();
            t3 += delta;
            ((IRTMPEvent) event).setTimestamp(t3);
            sinks.get(0).getStream().dispatchStreamEvent(event);
        }
    }

    @Override
    public void addResourceSink(IResourceSink arg0) {
        sinks.add(arg0);
    }

    @Override
    public void execute(IResourceSink arg0) {
    }

    @Override
    public void onClientAdded(IComserverNode arg0) {
    }

    @Override
    public void onClientRemoved(IComserverNode arg0) {
    }

    @Override
    public void removeResourceSink(IResourceSink arg0) {
        synchronized (sinks) {
            sinks.remove(arg0);
        }
    }
}
