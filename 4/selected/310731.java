package org.asteriskjava.fastagi.command;

/**
 * Hangs up the specified channel. If no channel name is given, hangs up the
 * current channel.
 * 
 * @author srt
 * @version $Id: HangupCommand.java 938 2007-12-31 03:23:38Z srt $
 */
public class HangupCommand extends AbstractAgiCommand {

    /**
     * Serial version identifier.
     */
    private static final long serialVersionUID = 3904959746380281145L;

    /**
     * The name of the channel to hangup or <code>null</code> for the current
     * channel.
     */
    private String channel;

    /**
     * Creates a new HangupCommand that hangs up the current channel.
     */
    public HangupCommand() {
        super();
    }

    /**
     * Creates a new HangupCommand that hangs up the given channel.
     * 
     * @param channel the name of the channel to hangup.
     */
    public HangupCommand(String channel) {
        super();
        this.channel = channel;
    }

    /**
     * Returns the name of the channel to hangup.
     * 
     * @return the name of the channel to hangup or <code>null</code> for the
     *         current channel.
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Sets the name of the channel to hangup.
     * 
     * @param channel the name of the channel to hangup or <code>null</code>
     *            for the current channel.
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    @Override
    public String buildCommand() {
        return "HANGUP" + (channel == null ? "" : " " + escapeAndQuote(channel));
    }
}
