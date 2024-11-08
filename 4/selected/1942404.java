package model.channel.set;

import model.util.*;
import model.channel.priority.*;
import java.util.*;

public abstract class AValueSet implements IValueSet {

    protected IPriority iPriority;

    protected Hashtable<Short, Channel> channels;

    protected int source;

    protected int combineMethod;

    public AValueSet(int _source, IPriority _iPriority) {
        source = _source;
        iPriority = _iPriority;
        combineMethod = AVERAGE;
        channels = new Hashtable<Short, Channel>();
    }

    public void setIPriority(IPriority _iPriority) {
        iPriority = _iPriority;
    }

    public IPriority getIPriority() {
        return iPriority;
    }

    public int getPriority(Channel channel) {
        return iPriority.getPriority(channels.get(channel.address));
    }

    public int getPriority(short address) {
        return iPriority.getPriority(channels.get(address));
    }

    /**
   * This method will return a channel object for the given address.
   * 
   * @param address The address to return the Channel object for.
   * @return The Channel object of the particular address in this cue.
   */
    public Channel getChannel(short address) {
        Channel c = channels.get(address);
        if (c != null) return c; else return new Channel(address, (short) 0);
    }

    public Channel getRawChannel(short address) {
        return channels.get(address);
    }

    /**
   * This method will return the value of the given channel
   * in this cue.
   * 
   * @param address The address to return the value for
   * @return The value of the channel.  If the cue's hashtable does not have a 
   * predefined value for this channel, it will return the default value of 0.
   */
    public short getChannelValue(short address) {
        return getChannel(address).value;
    }

    public Channel getChannel(Channel channel) {
        return getChannel(channel.address);
    }

    public Channel[] getChannels() {
        return channels.values().toArray(new Channel[0]);
    }

    public Channel[] getChannels(short[] addresses) {
        Channel[] c = new Channel[addresses.length];
        for (int i = 0; i < addresses.length; i++) c[i] = new Channel(addresses[i], getChannelValue(addresses[i]));
        return c;
    }

    /**
   * This method will set the value of the given channel in this cue.
   * 
   * @param channel The Channel object with the address, value pair.
   */
    public void setChannelValue(Channel channel) {
        if (channel.value == -100 && channels.get(channel.address) != null) channels.remove(channel.address); else channels.put(channel.address, channel);
    }

    public void setChannelValue(short address, short value) {
        setChannelValue(new Channel(address, value));
    }

    public void setChannelValues(Channel[] chls) {
        for (Channel c : chls) setChannelValue(c);
    }

    public int getSource() {
        return source;
    }

    public void resetValues() {
        channels.clear();
    }

    public int getCombineMethod() {
        return combineMethod;
    }

    public void setCombineMethod(int _combineMethod) {
        combineMethod = _combineMethod;
    }
}
