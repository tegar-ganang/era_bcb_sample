package edu.sdsc.rtdsm.framework.data;

import java.util.*;

public class TimeStampedData {

    private long ts;

    Hashtable<String, ChannelData> channelData = new Hashtable<String, ChannelData>();

    public TimeStampedData(long timestamp) {
        this.ts = timestamp;
    }

    public ChannelData getChannelData(String channelName) {
        if (!channelData.containsKey(channelName)) {
            channelData.put(channelName, new ChannelData(channelName));
        }
        return channelData.get(channelName);
    }

    public Enumeration<String> getChannels() {
        return channelData.keys();
    }
}
