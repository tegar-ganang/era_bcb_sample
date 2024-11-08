package com.tysanclan.site.projectewok.ws.mumble;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Jeroen Steenbeeke
 */
@XmlRootElement(name = "user")
public class MumbleUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private int secondsOnline;

    private int channel;

    private String name;

    private String state;

    private boolean deaf;

    private boolean mute;

    @XmlElement(name = "online")
    public int getSecondsOnline() {
        return secondsOnline;
    }

    public void setSecondsOnline(int secondsOnline) {
        this.secondsOnline = secondsOnline;
    }

    @XmlElement(name = "channel")
    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    @XmlElement(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "state")
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @XmlElement(name = "deaf", type = Boolean.class)
    public boolean isDeaf() {
        return deaf;
    }

    public void setDeaf(boolean deaf) {
        this.deaf = deaf;
    }

    @XmlElement(name = "mute", type = Boolean.class)
    public boolean isMute() {
        return mute;
    }

    public void setMute(boolean mute) {
        this.mute = mute;
    }
}
