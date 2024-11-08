package com.webhiker.dreambox.api.service;

import org.w3c.dom.Element;
import com.webhiker.dreambox.api.Utils;

/**
 * The Class Event represents a single EPG event for the currently showing channel EPG data.
 */
public class Audio {

    /** The Constant ID. */
    private static final String PID = "pid";

    /** The Constant DATE. */
    private static final String SELECTED = "selected";

    /** The Constant TIME. */
    private static final String NAME = "name";

    /** The details. */
    private String pid, name, channel;

    private boolean selected;

    /**
	 * Instantiates a new event.
	 * 
	 * @param event the event
	 */
    public Audio(Element element, int index) {
        setPid(Utils.getElement(element, PID, 0).getTextContent());
        setName(Utils.getElement(element, NAME, 0).getTextContent());
        setSelected(Integer.parseInt(Utils.getElement(element, SELECTED, 0).getTextContent()) == 1);
        setChannel(Integer.toString(index));
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String toString() {
        return getName();
    }

    public boolean equals(Object obj) {
        if (obj instanceof Audio) {
            if (!getPid().equals(((Audio) obj).getPid())) return false;
            if (!getName().equals(((Audio) obj).getName())) return false;
            if (isSelected() != ((Audio) obj).isSelected()) return false;
            return true;
        }
        return false;
    }

    public String getChannel() {
        return channel;
    }

    private void setChannel(String channel) {
        this.channel = channel;
    }
}
