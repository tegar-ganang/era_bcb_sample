package com.rubixinfotech.SKJava.Messages;

import com.rubixinfotech.SKJava.SKJMessage;

/**
 * This message is created by SKJava in response to a QueryAllChannelGroups
 * operation.  This message is interally constructed by parsing out the response
 * to the {@link XL_ChannelGroupPopulationQueryAck}, which has the names stored in
 * a rather cryptic manner (which co-incidentally, is easy to parse out in C/C++).
 * 
 */
public class SKJQueryAllChannelGroupsAck extends SKJMessage {

    protected String[] m_ChannelGroups;

    protected int Status;

    protected SKJQueryAllChannelGroupsAck(int Status, String[] channelGroups) {
        m_ChannelGroups = channelGroups;
        this.Status = Status;
    }

    /**
	 * Retrieves the list of channel-group names
	 * 
	 * @return An array of channel group names
	 */
    public String[] getChannelGroups() {
        return m_ChannelGroups;
    }

    /**
	 * Retrieves the channel group at a specific index in the list
	 * 
	 * @param  The index of interest
	 * @return A channel group name at the specified index
	 */
    public String getChannelGroup(int i) {
        if ((i > 0) && (i < m_ChannelGroups.length)) return m_ChannelGroups[i]; else return "";
    }

    /**
	 * Returns the status of the query
	 * 
	 * @return A SwitchKit status code
	 */
    public int getStatus() {
        return Status;
    }
}
