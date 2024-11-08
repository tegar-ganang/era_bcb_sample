package com.google.code.guidatv.model;

import java.io.Serializable;
import java.util.List;

public class Schedule implements Serializable {

    private Channel channel;

    private List<Transmission> transmissions;

    public Schedule() {
    }

    public Schedule(Channel channel, List<Transmission> transmissions) {
        init(channel, transmissions);
    }

    public void init(Channel channel, List<Transmission> transmissions) {
        this.channel = channel;
        this.transmissions = transmissions;
    }

    public Channel getChannel() {
        return channel;
    }

    public List<Transmission> getTransmissions() {
        return transmissions;
    }

    @Override
    public String toString() {
        return "DaySchedule [" + (channel != null ? "channel=" + channel + ", " : "") + (transmissions != null ? "transmissions=" + transmissions : "") + "]";
    }
}
