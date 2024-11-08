package soc.message;

import java.util.StringTokenizer;

/**
 * This message means that someone is joining a channel
 *
 * @author Robert S Thomas
 */
public class SOCJoin extends SOCMessage {

    /**
     * symbol to represent a null password
     */
    private static String NULLPASS = "\t";

    /**
     * Nickname of the joining member
     */
    private String nickname;

    /**
     * Optional password
     */
    private String password;

    /**
     * Name of channel
     */
    private String channel;

    /**
     * Host name
     */
    private String host;

    /**
     * Create a Join message.
     *
     * @param nn  nickname
     * @param pw  password
     * @param hn  host name
     * @param ch  name of chat channel
     */
    public SOCJoin(String nn, String pw, String hn, String ch) {
        messageType = JOIN;
        nickname = nn;
        password = pw;
        channel = ch;
        host = hn;
    }

    /**
     * @return the nickname
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the host name
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the channel name
     */
    public String getChannel() {
        return channel;
    }

    /**
     * JOIN sep nickname sep2 password sep2 host sep2 channel
     *
     * @return the command String
     */
    public String toCmd() {
        return toCmd(nickname, password, host, channel);
    }

    /**
     * JOIN sep nickname sep2 password sep2 host sep2 channel
     *
     * @param nn  the nickname
     * @param pw  the password
     * @param hn  the host name
     * @param ch  the channel name
     * @return    the command string
     */
    public static String toCmd(String nn, String pw, String hn, String ch) {
        String temppw = pw;
        if (temppw.equals("")) {
            temppw = NULLPASS;
        }
        return JOIN + sep + nn + sep2 + temppw + sep2 + hn + sep2 + ch;
    }

    /**
     * Parse the command String into a Join message
     *
     * @param s   the String to parse
     * @return    a Join message, or null of the data is garbled
     */
    public static SOCJoin parseDataStr(String s) {
        String nn;
        String pw;
        String hn;
        String ch;
        StringTokenizer st = new StringTokenizer(s, sep2);
        try {
            nn = st.nextToken();
            pw = st.nextToken();
            hn = st.nextToken();
            ch = st.nextToken();
            if (pw.equals(NULLPASS)) {
                pw = "";
            }
        } catch (Exception e) {
            return null;
        }
        return new SOCJoin(nn, pw, hn, ch);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString() {
        String s = "SOCJoin:nickname=" + nickname + "|password=" + password + "|host=" + host + "|channel=" + channel;
        return s;
    }
}
