package com.pbxworkbench.pbx.applet;

import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;
import com.pbxworkbench.pbx.ChannelAppletId;
import com.pbxworkbench.pbx.ChannelAppletLocation;
import com.pbxworkbench.pbx.IChannelApplet;
import com.pbxworkbench.pbx.asterisk.ChannelAppletAgiScript;

public abstract class AbstractChannelApplet implements IChannelApplet {

    private ChannelAppletId id = ChannelAppletId.NULL_ID;

    public void setId(ChannelAppletId id) {
        this.id = id;
    }

    public ChannelAppletId getId() {
        return id;
    }

    public ChannelAppletLocation getChannelAppletLocation() {
        String scriptName = ChannelAppletAgiScript.class.getName();
        return new ChannelAppletLocation("/" + scriptName + "?appId=" + getId());
    }

    public abstract void run(AgiRequest request, AgiChannel channel) throws AgiException;

    public void onException(Exception e) {
    }
}
