package consciouscode.bonsai.channels;

import java.util.HashMap;
import org.apache.commons.lang.StringUtils;

/**
    A collection of {@link Channel}s, to support the {@link ChannelProvider}
    interface.
    <p>
    A <code>ChannelSupport</code> may be configured with an "update observer",
    which may have handler methods for particular channels of interest.
    For example, while defining a channel named <code>status</code>, the
    <code>ChannelSupport</code> will determine if its update observer has a
    method of the form <code>public void updateStatus()</code>. If the method
    exists, a listener will be created that calls the method whenever the
    channel is updated.

    <p>
    This class is safe for use from multiple threads.
*/
public class ChannelSupport implements ChannelProvider {

    /**
       Creates a <code>ChannelSupport</code> that is its own update observer.
       This constructor is intended for use by subclasses.
    */
    protected ChannelSupport() {
        myUpdateObserver = this;
    }

    /**
       Creates a <code>ChannelSupport</code> with the given update observer.

       @param updateObserver may be null.
    */
    public ChannelSupport(Object updateObserver) {
        myUpdateObserver = updateObserver;
    }

    public synchronized Channel getChannel(String channelName) {
        Object channel = myChannels.get(channelName);
        if (channel == null) {
            throw new IllegalArgumentException("No such channel: " + channelName);
        }
        return (Channel) channel;
    }

    /**
       Associates a new {@link BasicChannel} with a given name.
       Note that the name is only associated with the channel within this
       provider.

       @param channelName must be unique within this provider.  It must not be
       null or empty.
       @return the newly-created channel.
    */
    public synchronized BasicChannel defineChannel(String channelName) {
        BasicChannel channel = new BasicChannel();
        defineChannel(channelName, channel);
        return channel;
    }

    /**
       Associates a given channel with a given name.
       Note that the name is only associated with the channel within this
       provider.
       <p>
       If this object's update observer has a method of the form
       <code>public void update<i>Channel</i>()</code> (where
       <code><i>Channel</i></code> is the capitalized channel name), a
       {@link ChannelListener} will be attached to invoke it.

       @param channelName must be unique within this provider.  It must not be
       null or empty.
       @param channel is the channel to associate with the name. It must not be
       null.
    */
    public synchronized void defineChannel(String channelName, Channel channel) {
        myChannels.put(channelName, channel);
        if (myUpdateObserver != null) {
            try {
                String methodName = "update" + StringUtils.capitalize(channelName);
                ThunkChannelListener listener = new ThunkChannelListener(myUpdateObserver, methodName);
                channel.addChannelListener(listener);
            } catch (IllegalArgumentException e) {
            }
        }
    }

    public final Object getChannelValue(String channelName) {
        return getChannel(channelName).getValue();
    }

    public final void setChannelValue(String channelName, Object value) {
        getChannel(channelName).setValue(value);
    }

    /**
       Maps String to Channel
    */
    private HashMap<String, Channel> myChannels = new HashMap<String, Channel>(8);

    private Object myUpdateObserver;
}
