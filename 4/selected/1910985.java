package model.channel.set;

import model.util.*;
import model.channel.priority.*;
import java.util.*;

public class AGroupValueSet implements IValueSet {

    protected IPriority priority;

    protected Hashtable<Short, Channel> channels;

    protected int source;

    protected int combineMethod;

    protected short value;

    public void setIPriority(IPriority _priority) {
        priority = _priority;
    }

    public IPriority getIPriority() {
        return priority;
    }

    public int getPriority(short address) {
        return priority.getPriority(new Channel(address, value));
    }

    public int getPriority(Channel channel) {
        return priority.getPriority(new Channel(channel.address, value));
    }

    public Channel getChannel(short address) {
        return new Channel(address, getChannelValue(address));
    }

    public Channel getRawChannel(short address) {
        Channel chan = channels.get(address);
        if (chan != null) return new Channel(address, scaledValue(chan.value)); else return null;
    }

    public short getChannelValue(short address) {
        Channel chan = channels.get(address);
        if (chan != null) return scaledValue(chan.value); else return (short) 0;
    }

    public Channel getChannel(Channel channel) {
        if (channels.get(channel.address) != null) return new Channel(channel.address, value); else return new Channel(channel.address, (short) 0);
    }

    public Channel[] getChannels() {
        Channel[] chans = channels.values().toArray(new Channel[channels.size()]);
        Channel[] result = new Channel[chans.length];
        for (int i = 0; i < result.length; i++) result[i] = new Channel(chans[i].address, scaledValue(chans[i].value));
        return result;
    }

    public Channel[] getChannels(short[] addresses) {
        Channel[] result = new Channel[addresses.length];
        for (int i = 0; i < result.length; i++) result[i] = getChannel(addresses[i]);
        return result;
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

    private short scaledValue(short chan) {
        if (chan > 0) return (short) (1.0 * chan * value / 255.0 + 0.5); else return chan;
    }
}
