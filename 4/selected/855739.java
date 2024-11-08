package soc.message;

import java.util.StringTokenizer;

/**
 * This message means that the server has authorized
 * this client to join a channel
 *
 * @author Robert S Thomas
 */
public class SOCJoinAuth extends SOCMessage {

    /**
     * Nickname of the joining member
     */
    private String nickname;

    /**
     * Name of channel
     */
    private String channel;

    /**
     * Create a JoinAuth message.
     *
     * @param nn  nickname
     * @param ch  name of chat channel
     */
    public SOCJoinAuth(String nn, String ch) {
        messageType = JOINAUTH;
        nickname = nn;
        channel = ch;
    }

    /**
     * @return the nickname
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * @return the channel name
     */
    public String getChannel() {
        return channel;
    }

    /**
     * JOINAUTH sep nickname sep2 channel
     *
     * @return the command String
     */
    public String toCmd() {
        return toCmd(nickname, channel);
    }

    /**
     * JOINAUTH sep nickname sep2 channel
     *
     * @param nn  the neckname
     * @param ch  the channel name
     * @return    the command string
     */
    public static String toCmd(String nn, String ch) {
        return JOINAUTH + sep + nn + sep2 + ch;
    }

    /**
     * Parse the command String into a Join message
     *
     * @param s   the String to parse
     * @return    a Join message, or null of the data is garbled
     */
    public static SOCJoinAuth parseDataStr(String s) {
        String nn;
        String ch;
        StringTokenizer st = new StringTokenizer(s, sep2);
        try {
            nn = st.nextToken();
            ch = st.nextToken();
        } catch (Exception e) {
            return null;
        }
        return new SOCJoinAuth(nn, ch);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString() {
        String s = "SOCJoinAuth:nickname=" + nickname + "|channel=" + channel;
        return s;
    }
}
