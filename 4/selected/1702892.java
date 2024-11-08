package org.openmim.mn2.model;

import org.openmim.mn2.controller.IRCChannelUtil;
import org.openmim.mn2.controller.MN2Factory;
import squirrel_util.Lang;
import squirrel_util.StringUtil;

public class IRCChannelImpl extends RoomImpl implements IRCChannel {

    private String channelName;

    private String channelNameCanonical;

    public IRCChannelImpl(String channelName, MN2Factory MN2Factory) {
        Lang.ASSERT(IRCChannelUtil.isChannelName(channelName), "channelName must be must be at least one char long, and must start with '#' or '&', but it is " + StringUtil.toPrintableString(channelName));
        this.channelName = channelName;
        this.channelNameCanonical = MN2Factory.getNameConvertor().toCanonicalIRCChannelName(channelName);
    }

    public String getChannelNameCanonical() {
        return channelNameCanonical;
    }

    public void setInviteOnly(boolean b) {
    }

    public void setChannelKeyUsed(boolean b) {
    }

    public void setPrivateChannelMode(boolean b) {
    }

    public void setSecret(boolean b) {
    }

    public void setChannelKey(String key) {
    }

    public void setLimited(boolean b) {
    }

    public void setLimit(int limit) {
    }

    public void setModerated(boolean b) {
    }

    public void setNoExternalMessages(boolean b) {
    }

    public void setOnlyOpsChangeTopic(boolean b) {
    }

    public void setTopic(String topicText) {
    }

    public void addRole(IRCChannelParticipant role) {
    }

    public void parts(IRCChannelParticipant user, String partsComment) {
    }

    public void joins(IRCChannelParticipant user) {
    }

    public void kicked(IRCChannelParticipant user, User kicker, String comment) {
    }

    public void meParts(IRCChannelParticipant me) {
    }

    public void addIRCChannelListener(IRCChannelListener listener) {
    }

    public void removeIRCChannelListener(IRCChannelListener listener) {
    }

    public String getChannelName() {
        return channelName;
    }

    public String toString() {
        return getChannelName();
    }
}
