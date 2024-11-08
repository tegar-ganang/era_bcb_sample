package net.sourceforge.jcoupling2.adapter;

import java.util.EventObject;

public class MessageEvent extends EventObject {

    public MessageEvent(String channelName) {
        super(channelName);
    }

    public String getChannelName() {
        return (String) getSource();
    }
}
