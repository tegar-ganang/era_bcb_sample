package org.jmik.asterisk.model.impl;

import java.util.Date;
import org.apache.log4j.Logger;
import org.asteriskjava.manager.event.HangupEvent;
import org.asteriskjava.manager.event.ManagerEvent;

/**
 * This class represents a call with only one participant (thus one channel).
 * It happens when someone call an extension which is configured to switch
 * the call to an Asterisk's build-in IVR.
 * 
 * @author Michele La Porta
 */
public class SinglePartyCall extends Call {

    private static Logger logger = Logger.getLogger(SinglePartyCall.class);

    private Channel channel;

    public SinglePartyCall(String callId, Date date, int callState, Channel.Descriptor channelDescriptor) {
        super(callId, date, callState);
        if (channelDescriptor == null) throw new IllegalArgumentException("channelDescriptor can not be null");
        this.channel = new Channel(channelDescriptor, this);
        logger.info("ACTIVE " + this);
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean process(ManagerEvent event) {
        if (getState() == Call.ACTIVE_STATE) {
            if (event instanceof HangupEvent) {
                HangupEvent he = (HangupEvent) event;
                if (he.getChannel().equals(channel.getDescriptor().getId())) {
                    setState(Call.INVALID_STATE, "Call Ended");
                    logger.info("Call Ended");
                    return true;
                }
            }
        }
        return false;
    }
}
