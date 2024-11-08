package model.channel;

import model.util.*;
import java.util.*;
import model.cue.*;
import model.channel.set.*;

/**
 * This class stores all the current channel values.  Additionally, it
 * decides what the actual value of a channel will be when it is receiving
 * values from multiple sources.
 * 
 * <p>TODO: I need to rewrite this entire class using a numbered priority system.
 * The current system does not allow enough flexibility for the power I want to 
 * have in the program.  For now, this does work well, and until I have time to 
 * rewrite this entire portion of the program, I will leave it as it is.
 */
public class ChannelValues {

    /**
   * The maximum number of channels for this setup.
   */
    private int maxChannel;

    private ChannelPriorityQueue channelSets;

    /**
   * When a channel value is being changed, the channel source value is passed in with the method.
   * 
   * FADER_SOURCE is used when the channel value is from the virtual faders or keyboard.
   */
    public static final int FADER_SOURCE = -1;

    /**
   * These modes relate to the status of specific channels at any given time.
   * This status decides what the color code around the channel will be.
   * <p>
   * The FADE_UP_MODE is utilized when a channel is being faded up in value by
   * a cue transition.
   */
    public static final int FADE_UP_SOURCE = -4;

    /**
   * These modes relate to the status of specific channels at any given time.
   * This status decides what the color code around the channel will be.
   * <p>
   * The FADE_DOWN_MODE is utilized when a channel is being faded down in value
   * by a cue transition.
   */
    public static final int FADE_DOWN_SOURCE = -5;

    public static final int NETWORK_SOURCE = -6;

    /**
   * This constructor will create a new ChannelValue object with the set maximum
   * number of channels.  It will instantiate all of the channel values to their
   * defaults.
   * 
   * <P>TODO: Update Comments
   * 
   * @param _maxChannel The maximum number of channels for this setup.
   */
    public ChannelValues(int _maxChannel) {
        maxChannel = _maxChannel;
        channelSets = new ChannelPriorityQueue();
    }

    public void removeSet(IValueSet set) {
        channelSets.removeSet(set);
    }

    public void removeSet(int source) {
        channelSets.removeSet(source);
    }

    public void addSet(IValueSet set) {
        channelSets.addSet(set);
    }

    /**
   * This method returns the value of the given channel address.
   * 
   * @param address The address whos value will be returned.
   * @return The value of the given address.
   */
    public short getChannelValue(short address) {
        IValueSet[] sets = channelSets.getHighPrioritySets(address);
        int averageValue = 0;
        int addValue = 0;
        int len = 0;
        boolean hasValue = false;
        for (IValueSet s : sets) {
            if (s.getChannelValue(address) >= 0) {
                hasValue = true;
                if (s.getCombineMethod() == IValueSet.ADD) addValue += s.getChannelValue(address); else if (s.getCombineMethod() == IValueSet.AVERAGE) {
                    averageValue += s.getChannelValue(address);
                    len++;
                }
            }
        }
        if (hasValue == false) return -100;
        if (addValue != 0) return (short) ((1.0 * (addValue + averageValue)) / (len + 1) + 0.5);
        if (len != 0) return (short) ((1.0 * averageValue) / len);
        return 0;
    }

    public short getChannelValue(Channel channel) {
        return getChannelValue(channel.address);
    }

    /**
   * This method returns the Channel object for the given channel address.  Only
   * the address will be used from the given Channel, the value will be ignored.
   * 
   * @param channel The channel to return the Channel object for.
   * @return The Channel object representing the given channel address.
   */
    public Channel getChannel(Channel channel) {
        return new Channel(channel.address, getChannelValue(channel.address));
    }

    /**
   * This method will return Channel objects for the given channels.  Only the address is used
   * from the given channels, the value is ignored.
   * 
   * @param channels The channels to return Channel objects for.
   * @return The array of Channel objects corresponding to the given channels.
   */
    public Channel[] getChannels(Channel[] channels) {
        Channel[] newChannels = new Channel[channels.length];
        for (int i = 0; i < channels.length; i++) newChannels[i] = new Channel(channels[i].address, getChannelValue(channels[i]));
        return newChannels;
    }

    /**
   * This method will return an array of Channel objects for every channel
   * with a non-zero value.  
   * 
   * @return The array of Channel objects for every channel with a non-zero value.
   */
    public short[] getChannelValues(short[] addresses) {
        short[] values = new short[addresses.length];
        for (int i = 0; i < addresses.length; i++) values[i] = getChannelValue(addresses[i]);
        return values;
    }

    /**
   * This method will return an array of Channel objects for every channel
   * with a non-zero value.  
   * 
   * @return The array of Channel objects for every channel with a non-zero value.
   */
    public Channel[] getChannels() {
        ArrayList<Channel> channels = new ArrayList<Channel>();
        for (short i = 1; i <= maxChannel; i++) {
            short value = getChannelValue(i);
            if (value != 0) channels.add(new Channel(i, value));
        }
        return channels.toArray(new Channel[0]);
    }

    public Channel[] getAllChannels() {
        Channel[] channels = new Channel[maxChannel];
        for (short i = 0; i < maxChannel; i++) channels[i] = new Channel((short) (i + 1), getChannelValue((short) (i + 1)));
        return channels;
    }

    /**
   * This is a specilized form of getChannels() used when creating new cues.
   * In addition to returning all channels with a non-zero value, it will also
   * return channels with a hard-ducked fader value (-100). 
   * 
   * @return The array of Channel objects for cue creation.
   */
    public Channel[] getChannelsForCue() {
        ArrayList<Channel> channels = new ArrayList<Channel>();
        IValueSet faderSet = channelSets.getSet((int) FADER_SOURCE);
        for (short i = 1; i <= maxChannel; i++) {
            IValueSet[] sets = channelSets.getHighPrioritySets(i);
            short value = 0;
            short len = 0;
            for (IValueSet s : sets) {
                if (s.getChannelValue(i) >= 0) {
                    value += s.getChannelValue(i);
                    len++;
                }
            }
            if (faderSet.getChannelValue(i) == -100) value = -100; else if (len == 0) value = -100; else value /= len;
            if (value != 0) channels.add(new Channel(i, value));
        }
        return channels.toArray(new Channel[0]);
    }

    /**
   * This method will update the value for the given address, value pair.
   * 
   * @param channel The channel object with the address, value pair to be updated.
   * @param source The source of the new channel value.
   */
    public void updateChannel(Channel channel, int source) {
        IValueSet set = channelSets.getSet(source);
        ((AValueSet) set).setChannelValue(channel);
    }

    public void updateChannels(Channel[] channels, int source) {
        IValueSet set = channelSets.getSet(source);
        for (Channel c : channels) ((AValueSet) set).setChannelValue(c);
    }

    public float[] getChannelSource(Channel channel) {
        ArrayList<Float> results = new ArrayList<Float>();
        IValueSet[] sets = channelSets.getPriorityOrderSets(channel.address);
        float add = channel.address;
        for (IValueSet s : sets) {
            Channel c = s.getRawChannel(channel.address);
            if (s.getRawChannel(channel.address) != null) results.add((float) s.getSource());
        }
        float[] r = new float[results.size()];
        for (int i = 0; i < results.size(); i++) r[i] = results.get(i);
        return r;
    }

    public float[][] getChannelSources(Channel[] channels) {
        float[][] sources = new float[channels.length][];
        for (int i = 0; i < channels.length; i++) sources[i] = getChannelSource(channels[i]);
        return sources;
    }

    public IValueSet getValueSet(int source) {
        return channelSets.getSet(source);
    }

    /**
   * This method will return a Channel object with the value from the given source for each given 
   * channel address.
   * 
   * @param channels The channels that you want the values from the given source for.  Only the
   * address parameter from these channels will the used, the value will be ignored.
   * @param source The source to get the values from.
   * @return The array containing the channel objects for the given channels with the value of
   * that channel from the given source.
   */
    public Channel[] getChannels(Channel[] channels, int source) {
        IValueSet set = channelSets.getSet(source);
        Channel[] results = new Channel[channels.length];
        for (int i = 0; i < channels.length; i++) results[i] = set.getChannel(channels[i]);
        return results;
    }

    public Channel[] getChannels(int source) {
        return channelSets.getSet(source).getChannels();
    }

    public Channel[] resetValues(int source) {
        IValueSet set = channelSets.getSet(source);
        Channel[] results = set.getChannels();
        set.resetValues();
        return results;
    }

    public void removeAllCues() {
        channelSets.removeAllCues();
    }
}
