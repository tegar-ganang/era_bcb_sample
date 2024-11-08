package org.jmik.asterisk.model.impl;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.log4j.Logger;
import org.asteriskjava.manager.event.ManagerEvent;
import org.jmik.asterisk.model.CallListener;

/**
 * This class models a telephone call,the information flowing
 * between the service provider and the call participants.
 * The information flow between a call participant and the provider through
 * an its logical conduct (Channel).
 * 
 * @author Michele La Porta
 *
 */
public class Call {

    protected static Logger logger = Logger.getLogger(Call.class);

    public static final int IDLE_STATE = 0;

    public static final int CONNECTING_STATE = 1;

    public static final int ACTIVE_STATE = 2;

    public static final int INVALID_STATE = -1;

    protected Channel channel;

    protected String id;

    protected int state;

    protected java.util.Date creationTime;

    protected java.util.Date invalidationTime;

    protected String reasonForStateChange;

    protected Set<CallListener> listeners;

    Call() {
        state = INVALID_STATE;
    }

    public Call(String id, Date creationTime, int state) {
        if (id == null) {
            logger.error("id is null");
            throw new IllegalArgumentException("id cannot be null");
        }
        if (creationTime == null) {
            logger.error("creationTime is null");
            throw new IllegalArgumentException("creationTime cannot be null");
        }
        this.creationTime = creationTime;
        this.id = id;
        this.state = state;
        listeners = new LinkedHashSet<CallListener>();
        if (logger.isDebugEnabled()) logger.debug("Call " + this + "state " + this.state);
    }

    protected void setState(int state, String reasonForStateChange) {
        int oldState = this.state;
        this.state = state;
        this.reasonForStateChange = reasonForStateChange;
        if (logger.isDebugEnabled()) logger.debug("setState listeners size " + listeners.size() + " " + listeners);
        for (CallListener callListener : new LinkedHashSet<CallListener>(listeners)) callListener.stateChanged(oldState, this);
    }

    public boolean process(ManagerEvent event) {
        return false;
    }

    public void addListener(CallListener listener) {
        listeners.add(listener);
        if (logger.isDebugEnabled()) logger.debug("addListener " + listeners);
    }

    public void removeListener(CallListener listener) {
        listeners.remove(listener);
        if (logger.isDebugEnabled()) logger.debug("removeListener " + listeners);
    }

    public String getId() {
        return id;
    }

    public java.util.Date getCreationTime() {
        return creationTime;
    }

    public java.util.Date getInvalidationTime() {
        return invalidationTime;
    }

    public void setInvalidationTime(java.util.Date invalidationTime) {
        this.invalidationTime = invalidationTime;
    }

    public int getState() {
        return state;
    }

    public String getReasonForStateChange() {
        return reasonForStateChange;
    }

    public void setReasonForStateChange(String reasonForStateChange) {
        this.reasonForStateChange = reasonForStateChange;
    }

    public Channel getChannel() {
        return channel;
    }
}
