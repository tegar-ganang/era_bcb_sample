package org.anuta.xmltv.beans;

import org.anuta.xmltv.grabber.EPGGrabber;

public class Channel {

    private String channelId;

    private EPGGrabber grabber;

    private String language = "nl";

    private String channelName = "";

    private String channelLogo;

    public String getChannelLogo() {
        return channelLogo;
    }

    public void setChannelLogo(String channelLogo) {
        this.channelLogo = channelLogo;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public EPGGrabber getGrabber() {
        return grabber;
    }

    public void setGrabber(EPGGrabber grabber) {
        this.grabber = grabber;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer().append(getChannelId()).append(" - ").append(getChannelName());
        return sb.toString();
    }

    public String getXmltvChannelId() {
        return getGrabber().getMappedChannelId(getChannelId());
    }
}
