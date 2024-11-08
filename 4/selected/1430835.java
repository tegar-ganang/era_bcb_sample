package org.asteriskjava.manager.action;

/**
 * The PlayDTMFAction plays a DTMF digit on the specified channel.<p>
 * It is definied in <code>apps/app_senddtmf.c</code>.<p>
 * Available since Asterisk 1.2.8
 * 
 * @since 0.3
 * @author srt
 * @version $Id: PlayDtmfAction.java 938 2007-12-31 03:23:38Z srt $
 */
public class PlayDtmfAction extends AbstractManagerAction {

    /**
     * Serializable version identifier
     */
    private static final long serialVersionUID = 9002288048692675696L;

    private String channel;

    private String digit;

    /**
     * Creates a new empty PlayDtmfAction.
     */
    public PlayDtmfAction() {
    }

    /**
     * Creates a new PlayDtmfAction that sends the given DTMF digit to the given channel.
     * 
     * @param channel the name of the channel to send the digit to.
     * @param digit the DTML digit to play.
     */
    public PlayDtmfAction(String channel, String digit) {
        this.channel = channel;
        this.digit = digit;
    }

    /**
     * Returns the name of this action, i.e. "PlayDTMF".
     */
    @Override
    public String getAction() {
        return "PlayDTMF";
    }

    /**
     * Returns the name of the channel to send the digit to.
     * 
     * @return the name of the channel to send the digit to.
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Sets the name of the channel to send the digit to.
     * 
     * @param channel the name of the channel to send the digit to.
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Returns the DTMF digit to play.
     * 
     * @return the DTMF digit to play.
     */
    public String getDigit() {
        return digit;
    }

    /**
     * Sets the DTMF digit to play.
     * 
     * @param digit the DTMF digit to play.
     */
    public void setDigit(String digit) {
        this.digit = digit;
    }
}
