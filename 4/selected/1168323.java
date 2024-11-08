package gov.sns.ca;

import gov.sns.tools.transforms.ValueTransform;
import java.util.Hashtable;
import java.util.Map;

/**
 * ChannelFactory is a factory for generating channels.
 *
 * @author  tap
 */
public abstract class ChannelFactory {

    protected static final ChannelFactory defaultFactory;

    protected Map<String, Channel> channelMap;

    static {
        defaultFactory = newFactory();
    }

    /** Creates a new instance of ChannelFactory */
    protected ChannelFactory() {
        channelMap = new Hashtable<String, Channel>();
    }

    /**
     * Initialize the channel system
     * @return true if the initialization was successful and false if not
     */
    public abstract boolean init();

    public abstract void dispose();

    /**  
     * Get a channel associated with the signal name.  If the channel is already 
     * in our map, then return it, otherwise create a new one and add it to our 
     * channel map.
     * @param signalName The PV signal name of the channel
     * @return The channel corresponding to the signal name
     */
    public Channel getChannel(String signalName) {
        Channel channel;
        if (!channelMap.containsKey(signalName)) {
            channel = newChannel(signalName);
            channelMap.put(signalName, channel);
        } else {
            channel = channelMap.get(signalName);
        }
        return channel;
    }

    /**  
     * Get a channel associated with the signal name.  If the channel is already 
     * in our map, then return it, otherwise create a new one and add it to our 
     * channel map.
     * @param signalName The PV signal name of the channel
     * @param transform The channel's value transform
     * @return The channel corresponding to the signal name
     */
    public Channel getChannel(String signalName, ValueTransform transform) {
        return newChannel(signalName, transform);
    }

    /** 
     * Create a concrete channel which makes an appropriate low level channel
     * @return a new channel for the specified signal name
     */
    protected abstract Channel newChannel(String signalName);

    /**
     * Create a new channel for the given signal name and set its value transform.
     * @param signalName The PV signal name
     * @param transform The value transform to use in the channel
     * @return The new channel
     */
    protected Channel newChannel(String signalName, ValueTransform transform) {
        Channel channel = newChannel(signalName);
        channel.setValueTransform(transform);
        return channel;
    }

    /** 
     * Get the default factory which determines the low level channel implementation
     * @return The default channel factory
     */
    public static ChannelFactory defaultFactory() {
        return defaultFactory;
    }

    /** 
     * Get the associated channel system from the channel factory implementation.
     * @return The channel system
     */
    protected abstract ChannelSystem channelSystem();

    /** 
     * get the defualt system which handles static behavior of Channels 
     * @return the channel system associated with the default channel factory
     */
    static ChannelSystem defaultSystem() {
        return defaultFactory.channelSystem();
    }

    /** 
     * Instantiate a new ChannelFactory
     * @return a new channel factory
     */
    protected static ChannelFactory newFactory() {
        return new gov.sns.jca.JcaChannelFactory();
    }
}
