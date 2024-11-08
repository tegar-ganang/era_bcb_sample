package net.sf.babble.events;

import java.util.EventObject;
import net.sf.babble.UserInfo;

/**
 * This event models a public notice message to a channel.
 * @version $Id: PublicNoticeEvent.java 262 2007-02-06 06:55:29 +0000 (Tue, 06 Feb 2007) speakmon $
 * @author Ben Speakmon
 */
public class PublicNoticeEvent extends EventObject {

    /**
     * Holds value of property user.
     */
    private UserInfo user;

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Holds value of property notice.
     */
    private String notice;

    /**
     * Creates a new PublicNoticeEvent.
     * @param source the <code>UserInfo</code> for the user that sent the notice
     */
    public PublicNoticeEvent(Object source) {
        super(source);
        user = (UserInfo) source;
    }

    /**
     * Returns the <code>UserInfo</code> for the user that sent the notice.
     * @return the sender's <code>UserInfo</code>
     */
    public UserInfo getUser() {
        return this.user;
    }

    /**
     * Returns the channel the notice was sent to.
     * @return the destination channel
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Sets the channel the notice was sent to.
     * @param channel the destination channel
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Returns the notice text.
     * @return the notice text
     */
    public String getNotice() {
        return this.notice;
    }

    /**
     * Sets the notice text.
     * @param notice the notice text
     */
    public void setNotice(String notice) {
        this.notice = notice;
    }
}
