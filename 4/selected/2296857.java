package soc.message;

/**
 * This message means that a new chat channel has been created.
 *
 * @author Robert S Thomas
 */
public class SOCNewChannel extends SOCMessage {

    /**
     * Name of the new channel.
     */
    private String channel;

    /**
     * Create a NewChannel message.
     *
     * @param ch  name of new channel
     */
    public SOCNewChannel(String ch) {
        messageType = NEWCHANNEL;
        channel = ch;
    }

    /**
     * @return the name of the channel
     */
    public String getChannel() {
        return channel;
    }

    /**
     * NEWCHANNEL sep channel
     *
     * @return the command String
     */
    public String toCmd() {
        return toCmd(channel);
    }

    /**
     * NEWCHANNEL sep channel
     *
     * @param ch  the new channel name
     * @return    the command string
     */
    public static String toCmd(String ch) {
        return NEWCHANNEL + sep + ch;
    }

    /**
     * Parse the command String into a NewChannel message
     *
     * @param s   the String to parse
     * @return    a NewChannel message
     */
    public static SOCNewChannel parseDataStr(String s) {
        return new SOCNewChannel(s);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString() {
        return "SOCNewChannel:channel=" + channel;
    }
}
