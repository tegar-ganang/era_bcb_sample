package model.cue;

import model.util.*;
import java.util.*;

/**
 * This class represents an individual cue.  It contains the channel assignments for the cue,
 * the cue number, name, description, fade up time, and fade down time.
 */
public class ACue {

    private Hashtable<Short, Channel> channels;

    private float number;

    private String name;

    private String desc;

    private int maxChannel;

    private long fadeUpMillis;

    private long fadeDownMillis;

    private int index;

    /**
   * This constructor creates a new cueset with the default cue 0
   * 
   * @param _index The unique index number for this cue.
   * @param _maxChannel The maximum number of channels for this cue.
   */
    public ACue(int _index, int _maxChannel) {
        number = 0;
        name = "All Off";
        desc = "This is the default cue, with all values set to 0";
        fadeUpMillis = 3000;
        fadeDownMillis = 3000;
        maxChannel = _maxChannel;
        index = _index;
        channels = new Hashtable<Short, Channel>();
    }

    /**
   * This constructor will create a cue with a set number, name, description, fade up time, and fade down time.
   * 
   * @param _number The cue's number
   * @param _name The cue's name
   * @param _desc The cue's description
   * @param _fadeUpMillis The fade up time for this cue, in milliseconds
   * @param _fadeDownMillis The fade down time for this cue, in milliseconds
   * @param _index The unique index number of this cue
   * @param _maxChannel The maximum number of channels possible for this show
   */
    public ACue(float _number, String _name, String _desc, long _fadeUpMillis, long _fadeDownMillis, int _index, int _maxChannel) {
        number = _number;
        name = _name;
        desc = _desc;
        fadeUpMillis = _fadeUpMillis;
        fadeDownMillis = _fadeDownMillis;
        maxChannel = _maxChannel;
        index = _index;
        channels = new Hashtable<Short, Channel>();
    }

    /**
   * This constructor will create a cue with a set number, name, description, fade up time, fade down time, 
   * and starting channel values.
   * 
   * @param _number The cue's number
   * @param _name The cue's name
   * @param _desc The cue's description
   * @param _fadeUpMillis The fade up time for this cue, in milliseconds
   * @param _fadeDownMillis The fade down time for this cue, in milliseconds
   * @param _index The unique index number for this cue
   * @param _maxChannel The maximum number of channels possible for this show
   * @param _channels This hashtable contains any number of address, channel pairs with the default values for
   * this cue.  Any channels that are not present in the hashtable will have an assumed value of 0.
   */
    public ACue(float _number, String _name, String _desc, long _fadeUpMillis, long _fadeDownMillis, int _index, int _maxChannel, Hashtable<Short, Channel> _channels) {
        number = _number;
        name = _name;
        maxChannel = _maxChannel;
        desc = _desc;
        fadeUpMillis = _fadeUpMillis;
        fadeDownMillis = _fadeDownMillis;
        index = _index;
        channels = _channels;
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
        Channel c = channels.get(address);
        if (c != null) {
            if (c.value >= 0) return c.value; else return -100;
        } else return 0;
    }

    public Channel getRawChannel(short address) {
        return channels.get(address);
    }

    /**
   * This method returns an array of channel values.
   * 
   * @param addresses The array of channel addresses to return the values for.
   * @return The channel values for the addresses passed into the method.
   */
    public short[] getChannelValues(short[] addresses) {
        short[] values = new short[addresses.length];
        for (int i = 0; i < addresses.length; i++) values[i] = getChannelValue(addresses[i]);
        return values;
    }

    /**
   * This method returns an array of Channel objects for the given addresses.
   * 
   * @param addresses The addresses to return Channel objects for.
   * @return The Channel objects for the given addresses from this cue.
   */
    public Channel[] getChannels(short[] addresses) {
        Channel[] c = new Channel[addresses.length];
        for (int i = 0; i < addresses.length; i++) c[i] = getChannel(addresses[i]);
        return c;
    }

    /**
   * This method will return a channel object for the given address.
   * 
   * @param address The address to return the Channel object for.
   * @return The Channel object of the particular address in this cue.
   */
    public Channel getChannel(short address) {
        Channel c = channels.get(address);
        if (c != null) return c;
        return new Channel(address, (short) 0);
    }

    /**
   * This method will return a channel object for the given channel object
   * address.  Only the address will be used from the given channel object,
   * the value will be ignored.
   * 
   * @param channel The Channel object with the address to return a Channel
   * object for.
   * @return The Channel object for the address in this cue.
   */
    public Channel getChannel(Channel channel) {
        Channel c = channels.get(channel.address);
        if (c != null) return c;
        return new Channel(channel.address, (short) 0);
    }

    /**
   * This method will return all non-zero channels for this cue.
   * 
   * @return An array containing all the non-zero channels in this cue.
   */
    public Channel[] getChannels() {
        return channels.values().toArray(new Channel[0]);
    }

    /**
   * This method will return the complete set of Channel objects for this cue.  It will
   * return a value for all channels 1 through maxChannel.  For the channels that don't
   * have an entry in the hashtable, this method will return the default value of 0.
   * 
   * @return An array of length maxChannels which contains the Channel objects for
   * every address in this cue.
   */
    public Channel[] getAllChannels() {
        Channel[] c = new Channel[maxChannel];
        for (short i = 0; i < maxChannel; i++) c[i] = getChannel((short) (i + 1));
        return c;
    }

    /**
   * This method will set the value of the given channel in this cue.
   * 
   * @param channel The Channel object with the address, value pair.
   */
    public void setChannelValue(Channel channel) {
        channels.put(channel.address, channel);
    }

    /**
   * This method will change the value of the given address to the given
   * value.
   * 
   * @param address The address to change the value at.
   * @param value The new value of the given address.
   */
    public void setChannelValue(short address, short value) {
        channels.put(address, new Channel(address, value));
    }

    /**
   * This method will set a series of channel values in the cue.
   * 
   * @param c The Channel objects with the new address, value pairs.
   */
    public void setChannelValues(Channel[] c) {
        for (int i = 0; i < c.length; i++) setChannelValue(c[i]);
    }

    /**
   * This method will return the cue's name.
   * 
   * @return The cue's name.
   */
    public String getName() {
        return name;
    }

    /**
   * This method will set the cue's name.
   * 
   * @param _name The cue's new name.
   */
    public void setName(String _name) {
        name = _name;
    }

    /**
   * This method will get the cue's number.
   * 
   * @return The cue's number.
   */
    public float getNumber() {
        return number;
    }

    /**
   * This method will set the cue's number.
   * 
   * @param _number The cue's number.
   */
    public void setNumber(float _number) {
        number = _number;
    }

    /**
   * This method will return the cue's description.
   * 
   * @return The cue's description.
   */
    public String getDesc() {
        return desc;
    }

    /**
   * This method will set the cue's description.
   * 
   * @param _desc The cue's new description.
   */
    public void setDesc(String _desc) {
        desc = _desc;
    }

    /**
   * This method returns a string containing the cue's number, fade up and fade down times,
   * and name.  This is the description displayed in the cue window in the user interface.
   * 
   * @return The basic information about this cue in the format of "1.0: Fade up 3.5: Fade down 1.0: Act I Intro"
   */
    public String getSummary() {
        if (fadeUpMillis == fadeDownMillis) return number + ": Fade " + Double.toString(fadeUpMillis / 1000.0) + ": " + name; else return number + ": Fade up " + Double.toString(fadeUpMillis / 1000.0) + ": Fade down " + Double.toString(fadeDownMillis / 1000.0) + ": " + name;
    }

    public int getIndex() {
        return index;
    }

    /**
   * This method returns the fade up time in milliseconds.
   * 
   * @return The fade up time in milliseconds.
   */
    public long getFadeUpMillis() {
        return fadeUpMillis;
    }

    /**
   * This method sets the fade up time in milliseconds.
   * 
   * @param _fadeUpMillis The fade up time in milliseconds.
   */
    public void setFadeUpMillis(long _fadeUpMillis) {
        fadeUpMillis = _fadeUpMillis;
    }

    /**
   * This method returns the fade down time in milliseconds.
   * 
   * @return The fade down time in milliseconds.
   */
    public long getFadeDownMillis() {
        return fadeDownMillis;
    }

    /**
   * This method sets the fade down time in milliseconds.
   * 
   * @param _fadeDownMillis The fade down time in milliseconds.
   */
    public void setFadeDownMillis(long _fadeDownMillis) {
        fadeDownMillis = _fadeDownMillis;
    }

    /**
   * This method is used for saving cues to a file.  It will return the 
   * cue's information in an XML format to be saved in the file.  The
   * channel values are stored in the following format:
   * 
   * <channelValue>
   *    <address>
   *       14
   *    </address>
   *    <value>
   *       57
   *    </value>
   * </channelValue>
   * 
   * Each channel gets a separate channelValue set.  Data is only saved for 
   * channels with non-zero values.
   * 
   * @return A String with the data for this cue.
   */
    public String getFileData() {
        Channel[] cs = channels.values().toArray(new Channel[0]);
        String fileData = "";
        for (Channel c : cs) {
            fileData += GeneralUtils.genDataSet("channelValue", GeneralUtils.genDataSet("address", Short.toString(c.address)) + GeneralUtils.genDataSet("value", Short.toString(c.value)));
        }
        return fileData;
    }

    /**
   * This method takes in the data from a saved cue file, and returns a 
   * new cue.
   * 
   * @param data The data from the saved cue file.
   * @param int cueMaxChannel The maximum number of channels for this cue.
   * @return The ACue represented by the saved cue file.
   */
    public static ACue extractFile(String data, int cueMaxChannel) {
        Float cueNumber = Float.parseFloat(GeneralUtils.parseDataSet("number", data));
        String cueName = GeneralUtils.parseDataSet("name", data);
        String cueDesc = GeneralUtils.parseDataSet("description", data);
        long cueFadeUpMillis = Long.parseLong(GeneralUtils.parseDataSet("fadeUpTime", data));
        long cueFadeDownMillis = Long.parseLong(GeneralUtils.parseDataSet("fadeDownTime", data));
        Hashtable<Short, Channel> cueChannels = extractChannels(GeneralUtils.parseDataSet("channelValues", data));
        int cueIndex = Integer.parseInt(GeneralUtils.parseDataSet("index", data));
        return new ACue(cueNumber, cueName, cueDesc, cueFadeUpMillis, cueFadeDownMillis, cueIndex, cueMaxChannel, cueChannels);
    }

    /**
   * This method parses out of the channel section of the saved cue file.  
   * 
   * @param data The data for the channelValues section of the saved cue file.
   * @return The hashtable with the address, channel pairs for the cue in the 
   * saved cue file.
   */
    private static Hashtable<Short, Channel> extractChannels(String data) {
        String[] chanData = GeneralUtils.parseDataSets("channelValue", data);
        Hashtable<Short, Channel> cueChannels = new Hashtable<Short, Channel>();
        for (String chan : chanData) {
            Short addr = Short.parseShort(GeneralUtils.parseDataSet("address", chan));
            Short val = Short.parseShort(GeneralUtils.parseDataSet("value", chan));
            cueChannels.put(addr, new Channel(addr, val));
        }
        return cueChannels;
    }

    public void resetValues() {
        channels.clear();
    }
}
