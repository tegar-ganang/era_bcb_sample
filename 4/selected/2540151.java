package com.cameocontrol.cameo.control;

import java.util.Hashtable;
import java.util.Iterator;
import com.cameocontrol.cameo.file.adt.CueData;

public abstract class BasicCue implements ConsoleCue {

    protected Hashtable<Integer, ConsoleChannel> _channels;

    protected String _discription;

    public BasicCue() {
        _channels = new Hashtable<Integer, ConsoleChannel>();
        _discription = "";
    }

    public BasicCue(String d) {
        this();
        _discription = d;
    }

    public BasicCue(ConsoleCue cue) {
        this(cue.getDiscription());
        for (Integer chan : cue) _channels.put(new Integer(chan), new CameoChannel(cue.getLevel(chan)));
    }

    public int getHighestChannel() {
        int highest = 0;
        for (Integer i : _channels.keySet()) if (i > highest) highest = i;
        return highest;
    }

    private ConsoleChannel getChan(int x) {
        if (_channels.containsKey(new Integer(x))) return _channels.get(new Integer(x));
        return new CameoChannel();
    }

    private void setChan(int x, short l) {
        _channels.put(new Integer(x), new CameoChannel(l));
    }

    private void plusChan(int x, short l) {
        if (_channels.contains(x)) _channels.put(new Integer(x), new CameoChannel((short) (_channels.get(x).getLevel() + l)));
    }

    private void minusChan(int x, short l) {
        if (_channels.contains(x)) _channels.put(new Integer(x), new CameoChannel((short) (_channels.get(x).getLevel() - l)));
    }

    public Hashtable<Integer, ConsoleChannel> getChannels() {
        return _channels;
    }

    public ConsoleChannel getChannel(int x) {
        return getChan(x);
    }

    public short getLevel(int x) {
        return getChan(x).getLevel();
    }

    public short[] getLevels(int start, int end) {
        short[] ls = new short[end - start + 1];
        for (int i = start; i < end; i++) ls[i] = getChan(i).getLevel();
        return ls;
    }

    public String getDiscription() {
        return _discription;
    }

    public void setDiscription(String d) {
        _discription = d;
    }

    public void setLevel(int i, short l) {
        setChan(i, l);
    }

    public void setLevel(ChannelSet channels, short l) {
        for (Integer i : channels) setChan(i, l);
    }

    public void setOut(int i) {
        setChan(i, ConsoleChannel.OUT);
    }

    public void setOut(ChannelSet channels) {
        for (Integer i : channels) setChan(i, ConsoleChannel.OUT);
    }

    public void plus(int i, short l) {
        plusChan(i, l);
    }

    public void plus(ChannelSet channels, short l) {
        for (Integer i : channels) plusChan(i, l);
    }

    public void minus(int i, short l) {
        minusChan(i, l);
    }

    public void minus(ChannelSet channels, short l) {
        for (Integer i : channels) minusChan(i, l);
    }

    public Iterator<Integer> iterator() {
        return _channels.keySet().iterator();
    }

    public ConsoleCue duplicate() {
        CameoCue cue = new CameoCue(_discription);
        for (Integer chan : this) cue.setLevel(chan, _channels.get(chan).getLevel());
        return cue;
    }

    public CueData distill() {
        CueData cd = new CueData();
        cd.setDescription(_discription);
        for (Integer chan : this) if (_channels.get(chan).getLevel() != ConsoleChannel.OUT) cd.addLevel(chan, _channels.get(chan).getLevel());
        return cd;
    }

    public void extractFrom(CueData data) {
        _discription = data.getDescription();
        _channels = new Hashtable<Integer, ConsoleChannel>();
        for (Integer chan : data.keySet()) _channels.put(chan, new CameoChannel(data.get(chan).shortValue()));
    }
}
