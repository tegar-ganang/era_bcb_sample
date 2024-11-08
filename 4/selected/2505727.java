package com.google.code.guidatv.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ChannelEntry implements Serializable {

    private Channel channel;

    private List<Transmission> transmissions = new ArrayList<Transmission>();

    public ChannelEntry() {
    }

    public Channel getChannel() {
        return channel;
    }

    public List<Transmission> getTransmissions() {
        return transmissions;
    }

    public void add(Transmission transmission) {
        transmissions.add(transmission);
    }

    @Override
    public String toString() {
        return "ChannelEntry [" + (channel != null ? "channel=" + channel + ", " : "") + (transmissions != null ? "transmissions=" + transmissions : "") + "]";
    }
}
