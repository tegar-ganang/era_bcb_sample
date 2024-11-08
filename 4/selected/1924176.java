package org.jwebsocket.plugins.channels;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.async.IOFuture;
import org.jwebsocket.server.TokenServer;
import org.jwebsocket.token.Token;

/**
 * Class that represents the subscriber of a channel
 * 
 * @author puran
 * @version $Id: Subscriber.java 1275 2011-01-02 08:25:12Z fivefeetfurther $
 */
public class Subscriber {

    private String mId;

    private WebSocketConnector mConnector;

    private TokenServer mTokenServer;

    private Date mLoggedInTime;

    private List<String> mChannels = new ArrayList<String>();

    /**
	 * Default constructor
	 */
    public Subscriber(String id, Date loggedInTime, List<String> channels) {
        this.mId = id;
        this.mLoggedInTime = loggedInTime;
        this.mChannels = channels;
        this.mConnector = null;
    }

    /**
	 * Subscriber constructor
	 * @param theConnector the low-level WebSocket connector object for this subscriber
	 * @param theServer the token server instance
	 * @param loggedInTime the first time the subscriber logged in
	 */
    public Subscriber(WebSocketConnector theConnector, TokenServer theServer, Date loggedInTime) {
        this.mId = theConnector.getId();
        this.mConnector = theConnector;
        this.mTokenServer = theServer;
        this.mLoggedInTime = loggedInTime;
    }

    /**
	 * @return the id
	 */
    public String getId() {
        return mId;
    }

    /**
	 * @return the connector
	 */
    public WebSocketConnector getConnector() {
        return mConnector;
    }

    /**
	 * @return the channels
	 */
    public List<String> getChannels() {
        return mChannels;
    }

    /**
	 * Add the channel id to the list of channels this subscriber is
	 * subscribed
	 * @param channel the channel object
	 */
    public void addChannel(String channel) {
        this.mChannels.add(channel);
    }

    /**
	 * Removes the channel from the subscriber list of channels
	 * @param channel the channel id to remove.
	 */
    public void removeChannel(String channel) {
        if (this.mChannels != null) {
            this.mChannels.remove(channel);
        }
    }

    /**
	 * @return the loggedInTime
	 */
    public Date getLoggedInTime() {
        return mLoggedInTime;
    }

    /**
	 * Sends the token data asynchronously to the token server
	 * @param token the token data
	 * @return future object for IO status
	 */
    public IOFuture sendTokenAsync(Token token) {
        return mTokenServer.sendTokenAsync(mConnector, token);
    }

    public void sendToken(Token token) {
        mTokenServer.sendToken(mConnector, token);
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mId == null) ? 0 : mId.hashCode());
        return result;
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Subscriber other = (Subscriber) obj;
        if (mId == null) {
            if (other.mId != null) {
                return false;
            }
        } else if (!mId.equals(other.mId)) {
            return false;
        }
        return true;
    }
}
