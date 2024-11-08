package org.smartcti.freeswitch.inbound.event;

public class ConferenceEvent extends BaseInboundEvent {

    /**
	 * 
	 */
    private static final long serialVersionUID = 5274611732944068808L;

    private String conferenceName;

    private Integer conferenceSize;

    private String conferenceProfileName;

    private String uniqueID;

    private String channelName;

    private String action;

    public static String AddNemberAction = "add-member";

    public static String DelNemberAction = "del-member";

    public ConferenceEvent() {
        super();
    }

    public String getConferenceName() {
        return conferenceName;
    }

    public void setConferenceName(String conferenceName) {
        this.conferenceName = conferenceName;
    }

    public Integer getConferenceSize() {
        return conferenceSize;
    }

    public void setConferenceSize(Integer conferenceSize) {
        this.conferenceSize = conferenceSize;
    }

    public String getConferenceProfileName() {
        return conferenceProfileName;
    }

    public void setConferenceProfileName(String conferenceProfileName) {
        this.conferenceProfileName = conferenceProfileName;
    }

    public String getUniqueID() {
        return uniqueID;
    }

    public void setUniqueID(String uniqueID) {
        this.uniqueID = uniqueID;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
