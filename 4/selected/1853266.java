package soc.message;

/**
 * This message means that a chat channel has been destroyed.
 *
 * @author Robert S Thomas
 */
public class SOCDeleteChannel extends SOCMessage {

    /**
     * Name of the channel.
     */
    private String channel;

    /**
     * Create a DeleteChannel message.
     *
     * @param ch  name of the channel
     */
    public SOCDeleteChannel(String ch) {
        messageType = DELETECHANNEL;
        channel = ch;
    }

    /**
     * @return the name of the channel
     */
    public String getChannel() {
        return channel;
    }

    /**
     * DELETECHANNEL sep channel
     *
     * @return the command String
     */
    public String toCmd() {
        return toCmd(channel);
    }

    /**
     * DELETECHANNEL sep channel
     *
     * @param ch  the channel name
     * @return    the command string
     */
    public static String toCmd(String ch) {
        return DELETECHANNEL + sep + ch;
    }

    /**
     * Parse the command String into a DeleteChannel message
     *
     * @param s   the String to parse
     * @return    a Delete Channel message
     */
    public static SOCDeleteChannel parseDataStr(String s) {
        return new SOCDeleteChannel(s);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString() {
        return "SOCDeleteChannel:channel=" + channel;
    }
}
