package org.jmik.asterisk.model.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import org.apache.log4j.Logger;
import org.asteriskjava.manager.event.ManagerEvent;
import org.asteriskjava.manager.event.MeetMeLeaveEvent;
import org.jmik.asterisk.model.CallListener;
import org.jmik.asterisk.model.impl.Channel.Descriptor;
import org.jmik.asterisk.model.utils.DateFormatter;

/**
 * This class represents a call with more than two participant (thus more than
 * two channels) In Asterisk model conference it's more like a collection of
 * single party calls which are destinated to an extension which is configured
 * to switch the call to a particular conference room,where the information from
 * the callers are mixed and then ditributed to every participants. In this
 * JTAPI model a Call has a set of instances of Connection therefore in our
 * model a ConferenceCall is modeled to have a set of instances of Channel.
 * 
 * @author Michele La Porta
 * 
 */
public class ConferenceCall extends Call {

    private static Logger logger = Logger.getLogger(ConferenceCall.class);

    private Map<Channel.Descriptor, Channel> channels;

    private String roomId;

    public ConferenceCall(String roomId, Date date, Descriptor channelDesc) {
        super(roomId + "|" + DateFormatter.format(channelDesc.getCreationTime()), date, Call.ACTIVE_STATE);
        if (roomId == null) {
            logger.error("roomId is null");
            throw new IllegalArgumentException("roomId can not be null");
        }
        if (channelDesc == null) {
            logger.error("channelDesc can not be null");
            throw new IllegalArgumentException("channelDesc can not be null");
        }
        this.roomId = roomId;
        channels = new HashMap<Channel.Descriptor, Channel>();
        addChannel(channelDesc);
        logger.info("ACTIVE " + this);
    }

    public java.util.Set<Channel> getChannels() {
        return new LinkedHashSet<Channel>(channels.values());
    }

    public void addChannel(Descriptor channelDescriptor) {
        if (channelDescriptor == null) throw new AssertionError("channelDescriptor can not be null");
        Channel channel = new Channel(channelDescriptor, this);
        channels.put(channelDescriptor, channel);
        for (CallListener callListener : listeners) {
            callListener.channelAdded(this, channel);
        }
        logger.info("addChannel " + channel.getDescriptor().getId());
    }

    public void removeChannel(Descriptor channelDescriptor) {
        if (channelDescriptor == null) throw new AssertionError("channel can not be null");
        if (channels.containsKey(channelDescriptor)) {
            Channel channel = (Channel) channels.remove(channelDescriptor);
            for (CallListener callListener : listeners) {
                callListener.channelRemoved(this, channel);
            }
        }
        logger.info("removeChannel " + channel.getDescriptor().getId());
        if (channels.size() == 0) {
            setState(Call.INVALID_STATE, "All Parties Left");
            logger.info("All Parties Left");
        }
    }

    public boolean process(ManagerEvent event) {
        if (getState() == Call.ACTIVE_STATE) {
            if (event instanceof MeetMeLeaveEvent) {
                MeetMeLeaveEvent mmle = (MeetMeLeaveEvent) event;
                logger.info("MeetMeLeaveEvent[" + mmle.getChannel() + "]");
                logger.info("channels " + channels.size());
                for (Channel.Descriptor channelDesc : channels.keySet()) {
                    if (channelDesc.getId().equals(mmle.getChannel())) {
                        removeChannel(channelDesc);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String getRoomId() {
        return roomId;
    }
}
