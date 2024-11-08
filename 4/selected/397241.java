package model;

import model.util.*;

/**
 * This class stores all the current channel values.  Additionally, it
 * decides what the actual value of a channel will be when it is receiving
 * values from multiple sources.
 * 
 * TODO: I need to rewrite this entire class using a numbered priority system.
 * The current system does not allow enough flexibility for the power I want to 
 * have in the program.  For now, this does work well, and until I have time to 
 * rewrite this entire portion of the program, I will leave it as it is.
 */
public class ChannelValues {

    /**
   * The maximum number of channels for this setup.
   */
    private int maxChannel;

    /**
   * The array of all current values from the virtual faders or keyboard.  The array index
   * corresponds to the address of the value.
   */
    private short[] faderValues;

    /**
   * The array of all current values from cues.  The array index corresponds to the address
   * of the value.
   */
    private short[] cueValues;

    /**
   * The array of all current values from submasters.  The array index corresponds to the address
   * of the value.
   * 
   * TODO: Submasters are not actually implemented in the view yet, but they should be fully functional
   * on the backend.
   */
    private short[] submasterValues;

    /**
   * When a channel value is being changed, the channel source value is passed in with the method.
   * 
   * FADER_SOURCE is used when the channel value is from the virtual faders or keyboard.
   */
    public static final int FADER_SOURCE = 1;

    /**
   * When a channel value is being changed, the channel source value is passed in with the method.
   * 
   * CUE_SOURCE is used when the channel value is from a cue.
   */
    public static final int CUE_SOURCE = 2;

    /**
   * When a channel value is being changed, the channel source value is passed in with the method.
   * 
   * SUBMASTER_SOURCE is used when the channel value is from a submaster.
   */
    public static final int SUBMASTER_SOURCE = 3;

    /**
   * These modes relate to the status of specific channels at any given time.
   * This status decides what the color code around the channel will be.
   * 
   * The DEFAULT_VALUE_MODE is utilized when a channel does not have any value
   * assigned to it, and is simply the default vale of 0.
   */
    public static final int DEFAULT_VALUE_MODE = 0;

    /**
   * These modes relate to the status of specific channels at any given time.
   * This status decides what the color code around the channel will be.
   * 
   * The FADER_VALUE_MODE is utilized when the value on this channel is manually
   * set through the virual faders (or keyboard input).
   */
    public static final int FADER_VALUE_MODE = 1;

    /**
   * These modes relate to the status of specific channels at any given time.
   * This status decides what the color code around the channel will be.
   * 
   * The CUE_VALUE_MODE is utilized when the value on this channel is from the
   * current cue.
   */
    public static final int CUE_VALUE_MODE = 2;

    /**
   * These modes relate to the status of specific channels at any given time.
   * This status decides what the color code around the channel will be.
   * 
   * The SUBMASTER_VALUE_MODE is utilized when the current value on this channel
   * is from the submasters.
   */
    public static final int SUBMASTER_VALUE_MODE = 3;

    /**
   * This constructor will create a new ChannelValue object with the set maximum
   * number of channels.  It will instantiate all of the channel values to their
   * defaults.
   * 
   * @param _maxChannel The maximum number of channels for this setup.
   */
    public ChannelValues(int _maxChannel) {
        maxChannel = _maxChannel;
        faderValues = new short[maxChannel + 1];
        cueValues = new short[maxChannel + 1];
        submasterValues = new short[maxChannel + 1];
        for (int i = 0; i <= maxChannel; i++) {
            faderValues[i] = -1;
            cueValues[i] = -1;
            submasterValues[i] = -1;
        }
    }

    /**
   * This method will update the value for the given address, value pair.
   * 
   * @param channel The channel object with the address, value pair to be updated.
   * @param source The source of the new channel value.
   */
    public void updateChannel(Channel channel, int source) {
        switch(source) {
            case FADER_SOURCE:
                {
                    if (channel.value < 0) {
                        if (faderValues[channel.address] < 0) faderValues[channel.address] = -100; else faderValues[channel.address] = -1;
                    } else faderValues[channel.address] = channel.value;
                    break;
                }
            case CUE_SOURCE:
                cueValues[channel.address] = channel.value;
                break;
            case SUBMASTER_SOURCE:
                submasterValues[channel.address] = channel.value;
                break;
        }
    }

    public void updateChannels(Channel[] channels, int source) {
        for (int i = 0; i < channels.length; i++) updateChannel(channels[i], source);
    }

    /**
   * This method will update the value for a set of address, value pairs from
   * the given sources.  The length of the channel and source arrays must be the same.
   * 
   * @param channels The array of address, value pairs to be updated.  This array must
   * be the same length as the sources array.
   * @param sources The array of sources for the updated channels.  This array must be
   * the same length as the channels array.
   */
    public void updateChannels(Channel[] channels, int[] sources) {
        for (int i = 0; i < channels.length; i++) updateChannel(channels[i], sources[i]);
    }

    /**
   * This method will return the value of the channel at the given
   * address. 
   * 
   * TODO: This is one of the methods that needs to be overhauled and replaced
   * with a priority system to get rid fo the mess of if statements and special
   * cases.
   *
   * @param address The address to return the value of.
   * @return The value of the channel at address.
   */
    public short getChannelValue(short address) {
        short value;
        if (faderValues[address] >= 0) {
            value = faderValues[address];
        } else {
            if (submasterValues[address] > 0) {
                if (cueValues[address] > 0) {
                    if (submasterValues[address] >= cueValues[address]) value = submasterValues[address]; else value = cueValues[address];
                } else {
                    value = submasterValues[address];
                }
            } else {
                if (cueValues[address] > 0) value = cueValues[address]; else {
                    if (faderValues[address] == -100) value = -100; else {
                        if (cueValues[address] == -100) value = -100; else value = 0;
                    }
                }
            }
        }
        return value;
    }

    /**
   * This method will return the value of this channel.
   * 
   * @param channel The Channel object with the address to retrieve
   * the value for.  Only channel.address will be used; the passed in
   * value will be ignored.
   * @return The value of the channel at the address.
   */
    public short getChannelValue(Channel channel) {
        return getChannelValue(channel.address);
    }

    /**
   * This method will return the source of the channel at the given address.  If
   * a value has multiple sources, it will return them in order of priority.
   * 
   * TODO: This is the other method that needs to be redone with the priority system
   * to replace this mess of if statements and special cases.  For now, this works well
   * and won't be changed until I'm ready to rewrite the whole class.
   * 
   * @param address The address to return the source for.
   * @return The source of the value at the given address.
   */
    public int getChannelSource(short address) {
        int sources;
        if (faderValues[address] >= 0) {
            if (submasterValues[address] > 0) {
                if (cueValues[address] > 0) {
                    if (faderValues[address] >= submasterValues[address]) {
                        if (submasterValues[address] >= cueValues[address]) sources = FADER_SOURCE * 100 + SUBMASTER_SOURCE * 10 + CUE_SOURCE; else sources = FADER_SOURCE * 100 + CUE_SOURCE * 10 + SUBMASTER_SOURCE;
                    } else {
                        if (submasterValues[address] >= cueValues[address]) {
                            sources = FADER_SOURCE * 100 + SUBMASTER_SOURCE * 10 + CUE_SOURCE;
                        } else {
                            sources = CUE_SOURCE * 100 + SUBMASTER_SOURCE * 10 + FADER_SOURCE;
                        }
                    }
                } else {
                    sources = FADER_SOURCE * 10 + SUBMASTER_SOURCE;
                }
            } else {
                if (cueValues[address] > 0) {
                    sources = FADER_SOURCE * 10 + CUE_SOURCE;
                } else {
                    sources = FADER_SOURCE;
                }
            }
        } else {
            if (submasterValues[address] > 0) {
                if (cueValues[address] > 0) {
                    if (submasterValues[address] >= cueValues[address]) sources = SUBMASTER_SOURCE * 10 + CUE_SOURCE; else sources = CUE_SOURCE * 10 + SUBMASTER_SOURCE;
                } else {
                    sources = SUBMASTER_SOURCE;
                }
            } else {
                if (cueValues[address] > 0) sources = CUE_SOURCE; else sources = 0;
            }
        }
        if (faderValues[address] == -100) sources = 10 * sources + FADER_SOURCE;
        if (cueValues[address] == -100) sources = 10 * sources + CUE_SOURCE;
        return sources;
    }

    /**
   * This method will return the priority of the address in the
   * Channel object.
   * 
   * @param channel The channel object to look up the source for.  The
   * value will be ignored and only the address used.
   */
    public int getChannelSource(Channel channel) {
        return getChannelSource(channel.address);
    }

    /**
   * This method will return the channel object for a given address.
   * 
   * @param address The address to return the channel object for.
   * @return The Channel object for the given address.
   */
    public Channel getChannel(short address) {
        return new Channel(address, getChannelValue(address));
    }

    /**
   * This method will return the Channel object for the given Channel object.
   * The value of the given Channel object will be ignored, and only the address
   * used.
   * 
   * @param channel The channel to return the Channel object for.
   * @return The channel object for the given channel.
   */
    public Channel getChannel(Channel channel) {
        return getChannel(channel.address);
    }

    /**
   * This method will return Channel objects for the given addresses.
   * 
   * @param addresses The array of addresses to return Channel objects for.
   * @return The array of Channel for the addresses given.
   */
    public Channel[] getChannels(short[] addresses) {
        Channel[] channels = new Channel[addresses.length];
        for (int i = 0; i < channels.length; i++) channels[i] = new Channel(addresses[i], getChannelValue(addresses[i]));
        return channels;
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
   * This method will return the channel values from the startAddress to the endAddress.  The 
   * returned values include both the startAddress value and endAddress value.
   * 
   * @param startAddress The starting address for the values returned.  This address will be included
   * in the returned values.
   * @param endAddress The ending address for the values returned.  This address will be included in 
   * the returning values.
   * @return The array of the channel values.
   */
    public short[] getChannelValues(short startAddress, short endAddress) {
        short[] values = new short[endAddress - startAddress + 1];
        for (short i = startAddress; i <= endAddress; i++) values[i - startAddress] = getChannelValue(i);
        return values;
    }

    /**
   * This method will return the sources for the given range of addresses.  The returned range
   * will include both the start and end addresses.
   * 
   * @param startAddress The start address for the range of results.  This address will be included
   * in the results.
   * @param endAddress The end address for the range of results.  This address will be included in the
   * results.
   * @return The array of channel sources for the given address range.
   */
    public int[] getChannelSources(short startAddress, short endAddress) {
        int[] values = new int[endAddress - startAddress + 1];
        for (short i = startAddress; i <= endAddress; i++) values[i - startAddress] = getChannelSource(i);
        return values;
    }

    /**
   * This will return a Channel object for all channels with a non-zero value.
   * 
   * @return The array of Channel objects for every channel with a non-zero value.
   */
    public Channel[] getChannels() {
        int cnt = 0;
        for (short i = 1; i <= maxChannel; i++) {
            if (getChannelValue(i) != 0) cnt++;
        }
        Channel[] channels = new Channel[cnt];
        cnt = 0;
        for (short i = 1; i <= maxChannel; i++) {
            short value = getChannelValue(i);
            if (value != 0) {
                channels[cnt] = new Channel(i, value);
                cnt++;
            }
        }
        return channels;
    }

    /**
   * This will return all the channel values for channels that either arn't zero, or
   * have a fader value of -100.  These values are used for dealing with cues.
   * 
   * @return The array of Channel objects.
   */
    public Channel[] getChannelsForCue() {
        int cnt = 0;
        for (short i = 1; i <= maxChannel; i++) {
            if (getChannelValue(i) != 0 || faderValues[i] == -100) cnt++;
        }
        Channel[] channels = new Channel[cnt];
        cnt = 0;
        for (short i = 1; i <= maxChannel; i++) {
            short value = getChannelValue(i);
            if (value != 0 || faderValues[i] == -100) {
                if (faderValues[i] == -100) channels[cnt] = new Channel(i, (short) -100); else channels[cnt] = new Channel(i, value);
                cnt++;
            }
        }
        return channels;
    }

    /**
   * This will return the Channel objects for all channels up to the maxChannel
   * value.  
   * 
   * @return The array of all Channel objects up to the value of maxChannel.
   */
    public Channel[] getAllChannels() {
        Channel[] channels = new Channel[maxChannel];
        for (int i = 0; i < maxChannel; i++) channels[i] = getChannel((short) (i + 1));
        return channels;
    }

    /**
   * This method will return the Channel objects for any channel with a non-zero
   * cue value.  It will return the cue value for that channel.
   * 
   * @return The array of Channel objects for channels with non-zero cue values.
   */
    public Channel[] getCueChannels() {
        int cnt = 0;
        for (short i = 1; i <= maxChannel; i++) {
            if (cueValues[i] != 0) cnt++;
        }
        Channel[] channels = new Channel[cnt];
        cnt = 0;
        for (short i = 1; i <= maxChannel; i++) {
            if (cueValues[i] != 0) {
                channels[cnt] = new Channel(i, cueValues[i]);
                cnt++;
            }
        }
        return channels;
    }

    /**
   * This method will return the Channel objects for the given channels.  These channel objects will contain
   * the cue values on those channels.
   * 
   * @param channels The cue values for these channels will be returned.  The value from these channels is ignored.
   * @return The Channel objects for the given channels with the cue values.
   */
    public Channel[] getCueChannels(Channel[] channels) {
        Channel[] cueChannels = new Channel[channels.length];
        for (int i = 0; i < channels.length; i++) cueChannels[i] = new Channel(channels[i].address, cueValues[channels[i].address]);
        return cueChannels;
    }

    /**
   * This method will return the values for the given addresses.
   * 
   * @param addresses The addresses to return the values for.
   * @return The array of channel values for the given addresses.
   */
    public short[] getChannelValues(short[] addresses) {
        short[] values = new short[addresses.length];
        for (int i = 0; i < addresses.length; i++) values[i] = getChannelValue(addresses[i]);
        return values;
    }

    /**
   * This method will return the sources for the given channel addresses.
   * 
   * @param addresses The array of address to return the sources for.
   * @return The array of channel sources corresponding to the given addresses.
   */
    public int[] getChannelSources(short[] addresses) {
        int[] values = new int[addresses.length];
        for (int i = 0; i < addresses.length; i++) values[i] = getChannelSource(addresses[i]);
        return values;
    }

    /**
   * This method will return the sources for the given channels.  Only the address is used from
   * the given channel objects, the value is ignored.
   * 
   * @param channels The channels to return the sources for.
   * @return The array containing the sources for the given channels.
   */
    public int[] getChannelSources(Channel[] channels) {
        int[] values = new int[channels.length];
        for (int i = 0; i < channels.length; i++) values[i] = getChannelSource(channels[i].address);
        return values;
    }

    /**
   * This method will return the source for all channels through maxChannels.
   * 
   * @return The array containing the sources for all channels.
   */
    public int[] getAllChannelSources() {
        int[] values = new int[maxChannel];
        for (int i = 0; i < maxChannel; i++) values[i] = getChannelSource((short) (i + 1));
        return values;
    }

    /**
   * This method resets all the fader values back to their defaults.
   * 
   * @return The addresses of the of the fader values that were changed.
   */
    public short[] resetFaderValues() {
        int cnt = 0;
        for (int i = 1; i <= maxChannel; i++) {
            if (faderValues[i] != -1) cnt++;
        }
        short[] changedAddrs = new short[cnt];
        cnt = 0;
        for (int i = 1; i <= maxChannel; i++) {
            if (faderValues[i] != -1) {
                faderValues[i] = -1;
                changedAddrs[cnt] = (short) i;
                cnt++;
            }
        }
        return changedAddrs;
    }
}
