package com.dcivision.framework.taglib.channel;

public class ChannelTagAdapter {

    private ChannelTag tag = null;

    private AbstractChannelTagFormatter tagFormatter = null;

    public ChannelTagAdapter(ChannelTag tag) {
        this.tag = tag;
    }

    public AbstractChannelTagFormatter getChannelTagFormatter() {
        tagFormatter = new ChannelTagFormatter_1();
        tagFormatter.setChannelTag(tag);
        return tagFormatter;
    }
}
