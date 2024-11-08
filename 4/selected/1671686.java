package net.sf.babble.events;

import java.util.EventObject;
import net.sf.babble.UserInfo;

/**
 * This event models a user being kicked from the channel.
 * @version $Id: KickEvent.java 262 2007-02-06 06:55:29 +0000 (Tue, 06 Feb 2007) speakmon $
 * @author Ben Speakmon
 */
public class KickEvent extends EventObject {

    /**
     * Holds value of property user.
     */
    private UserInfo user;

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Holds value of property kickee.
     */
    private String kickee;

    /**
     * Holds value of property reason.
     */
    private String reason;

    /**
     * Creates a new KickEvent.
     * @param source the <code>UserInfo</code> object for the kicker
     */
    public KickEvent(Object source) {
        super(source);
        user = (UserInfo) source;
    }

    /**
     * Returns the <code>UserInfo</code> object for the kicker.
     * @return the kicker
     */
    public UserInfo getUser() {
        return this.user;
    }

    /**
     * Returns the channel in which the kick happened.
     * @return the kick
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Sets the channel in which the kick happened.
     * @param channel the channel
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Returns the nick of the kickee.
     * @return the kickee
     */
    public String getKickee() {
        return this.kickee;
    }

    /**
     * Sets the nick of the kickee.
     * @param kickee the kickee
     */
    public void setKickee(String kickee) {
        this.kickee = kickee;
    }

    /**
     * Returns the reason for the kick.
     * @return the reason
     */
    public String getReason() {
        return this.reason;
    }

    /**
     * Sets the reason for the kick.
     * @param reason the reason
     */
    public void setReason(String reason) {
        this.reason = reason;
    }
}
