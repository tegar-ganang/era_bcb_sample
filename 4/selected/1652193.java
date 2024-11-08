package org.asteriskjava.manager.event;

/**
 * Abstract base class providing common properties for MeetMe
 * (Asterisk's conference system) events.<p>
 * MeetMe events are implemented in <code>apps/app_meetme.c</code>
 * 
 * @author srt
 * @version $Id: AbstractMeetMeEvent.java 1057 2008-05-20 00:56:28Z srt $
 */
public abstract class AbstractMeetMeEvent extends ManagerEvent {

    private String channel;

    private String uniqueId;

    private String meetMe;

    private Integer userNum;

    /**
     * @param source
     */
    protected AbstractMeetMeEvent(Object source) {
        super(source);
    }

    /**
     * Returns the name of the channel.<p>
     * This property is available since Asterisk 1.4.
     * 
     * @return the name of the channel.
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Sets the name of the channel.<p>
     * This property is available since Asterisk 1.4.
     * 
     * @param channel the name of the channel.
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Returns the unique id of the channel.<p>
     * This property is available since Asterisk 1.4.
     * 
     * @return the unique id of the channel.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Sets the unique id of the channel.<p>
     * This property is available since Asterisk 1.4.
     * 
     * @param uniqueId the unique id of the channel.
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * Returns the conference number.
     * 
     * @return the conference number.
     */
    public String getMeetMe() {
        return meetMe;
    }

    /**
     * Sets the conference number.
     * 
     * @param meetMe the conference number.
     */
    public void setMeetMe(String meetMe) {
        this.meetMe = meetMe;
    }

    /**
     * Returns the index of the user in the conference.<p>
     * This can be used for the "meetme (mute|unmute|kick)" commands.
     * 
     * @return the index of the user in the conference.
     */
    public Integer getUserNum() {
        return userNum;
    }

    /**
     * Sets the index of the user in the conference.
     * 
     * @param userNum the index of the user in the conference.
     */
    public void setUserNum(Integer userNum) {
        this.userNum = userNum;
    }
}
