package model.channel.set;

import model.channel.priority.*;
import model.util.*;
import model.cue.*;
import java.util.*;

public class CueValueSet implements IValueSet {

    private IPriority iPriority;

    private ACue cue;

    private float fadeLevel;

    private int combineMethod;

    public CueValueSet(ACue _cue) {
        iPriority = new DuckingPriority(1000, 100);
        cue = _cue;
        fadeLevel = 1;
        combineMethod = AVERAGE;
    }

    public void setIPriority(IPriority priority) {
        iPriority = priority;
    }

    public IPriority getIPriority() {
        return iPriority;
    }

    public int getPriority(short address) {
        return iPriority.getPriority(getChannel(address));
    }

    public int getPriority(Channel channel) {
        return iPriority.getPriority(getChannel(channel.address));
    }

    public Channel getChannel(short address) {
        return new Channel(address, getChannelValue(address));
    }

    public short getChannelValue(short address) {
        return (short) (cue.getChannelValue(address) * fadeLevel + 0.5);
    }

    public Channel getChannel(Channel channel) {
        return getChannel(channel.address);
    }

    public Channel getRawChannel(short address) {
        if (fadeLevel == 0) return null;
        Channel c = cue.getRawChannel(address);
        if (c == null) return c;
        return new Channel(address, (short) (c.value * fadeLevel + 0.5));
    }

    public Channel[] getChannels() {
        Channel[] channels = cue.getChannels();
        Channel[] results = new Channel[channels.length];
        for (int i = 0; i < channels.length; i++) results[i] = new Channel(channels[i].address, (short) (channels[i].value * fadeLevel + 0.5));
        return results;
    }

    public Channel[] getAllChannels() {
        Channel[] channels = cue.getAllChannels();
        Channel[] results = new Channel[channels.length];
        for (int i = 0; i < channels.length; i++) results[i] = new Channel(channels[i].address, (short) (channels[i].value * fadeLevel + 0.5));
        return results;
    }

    public Channel[] getChannels(short[] addresses) {
        Channel[] channels = new Channel[addresses.length];
        for (int i = 0; i < addresses.length; i++) channels[i] = getChannel(addresses[i]);
        return channels;
    }

    public void setChannelValue(Channel channel) {
        setChannelValue(channel.address, channel.value);
    }

    public void setChannelValue(short address, short value) {
    }

    public void setChannelValues(Channel[] channels) {
        for (Channel c : channels) setChannelValue(c);
    }

    public int getSource() {
        return cue.getIndex();
    }

    public void resetValues() {
    }

    public void setFadeLevel(float _fadeLevel) {
        fadeLevel = _fadeLevel;
    }

    public float getFadeLevel() {
        return fadeLevel;
    }

    public long getFadeUpMillis() {
        return cue.getFadeUpMillis();
    }

    public long getFadeDownMillis() {
        return cue.getFadeDownMillis();
    }

    public int getCombineMethod() {
        return combineMethod;
    }

    public void setCombineMethod(int _combineMethod) {
        combineMethod = _combineMethod;
    }

    public String toString() {
        return "CueValueSet: " + cue.getSummary();
    }
}
