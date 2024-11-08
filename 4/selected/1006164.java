package org.openmim.irc.channel_list;

import squirrel_util.Lang;

public class ChannelListItem {

    private String channelName;

    private String channelNameLowercased;

    private int population;

    private String topic;

    private String topicStripped;

    public ChannelListItem(String s, int i, String s1) {
        Lang.ASSERT_NOT_NULL(s, "channelName");
        Lang.ASSERT_NOT_NULL(s1, "topic");
        Lang.ASSERT_POSITIVE(i, "population");
        channelName = s;
        population = i;
        topic = s1;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getChannelNameLowercased() {
        synchronized (channelName) {
            if (channelNameLowercased == null) channelNameLowercased = channelName.toLowerCase();
        }
        return channelNameLowercased;
    }

    public int getPopulation() {
        return population;
    }

    public String getTopic() {
        return topic;
    }

    public String getTopicStripped() {
        synchronized (topic) {
            Lang.NOT_IMPLEMENTED();
        }
        return null;
    }
}
