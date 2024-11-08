package org.eaiframework;

import java.io.Serializable;

/**
 * The Value Object holds the receiver of a filter definition.
 */
public class Receiver implements Serializable {

    /**
	 * Generated Serial Version UID.
	 */
    private static final long serialVersionUID = 1374226188851390470L;

    /**
	 * The id of the channel this receiver will listen to.
	 */
    private String channelId;

    /**
	 * Tells if the receiver should be synchronous or not.
	 */
    private boolean isSynchronous;

    /**
	 * If it is synchronous, the interval in milliseconds between
	 * reads.
	 */
    private long interval;

    /**
	 * 
	 */
    private String synchronous;

    /**
	 * @return the channelId
	 */
    public String getChannelId() {
        return channelId;
    }

    /**
	 * @param channelId the channelId to set
	 */
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    /**
	 * @return the isSynchronous
	 */
    public boolean isSynchronous() {
        return isSynchronous;
    }

    /**
	 * @param isSynchronous the isSynchronous to set
	 */
    public void setSynchronous(boolean isSynchronous) {
        this.isSynchronous = isSynchronous;
    }

    /**
	 * @return the interval
	 */
    public long getInterval() {
        return interval;
    }

    /**
	 * @param interval the interval to set
	 */
    public void setInterval(long interval) {
        this.interval = interval;
    }

    /**
	 * @return Returns the synchronous.
	 */
    public String getSynchronous() {
        return synchronous;
    }

    /**
	 * @param synchronous The synchronous to set.
	 */
    public void setSynchronous(String synchronous) {
        if (synchronous.equals("true")) {
            this.setSynchronous(true);
        } else {
            this.setSynchronous(false);
        }
        this.synchronous = synchronous;
    }
}
