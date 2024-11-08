package soc.message;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This message lists all the chat channels on a server
 *
 * @author Robert S Thomas
 */
public class SOCChannels extends SOCMessage {

    /**
     * List of channels
     */
    private Vector channels;

    /**
     * Create a Channels Message.
     *
     * @param cl  list of channels
     */
    public SOCChannels(Vector cl) {
        messageType = CHANNELS;
        channels = cl;
    }

    /**
     * @return the list of channels
     */
    public Vector getChannels() {
        return channels;
    }

    /**
     * CHANNELS sep channels
     *
     * @return the command String
     */
    public String toCmd() {
        return toCmd(channels);
    }

    /**
     * CHANNELS sep channels
     *
     * @param cl  the list of channels
     * @return    the command string
     */
    public static String toCmd(Vector cl) {
        String cmd = CHANNELS + sep;
        try {
            Enumeration clEnum = cl.elements();
            cmd += (String) clEnum.nextElement();
            while (clEnum.hasMoreElements()) {
                cmd += (sep2 + (String) clEnum.nextElement());
            }
        } catch (Exception e) {
        }
        return cmd;
    }

    /**
     * Parse the command String into a Channels message
     *
     * @param s   the String to parse
     * @return    a Channels message, or null of the data is garbled
     */
    public static SOCChannels parseDataStr(String s) {
        Vector cl = new Vector();
        StringTokenizer st = new StringTokenizer(s, sep2);
        try {
            while (st.hasMoreTokens()) {
                cl.addElement(st.nextToken());
            }
        } catch (Exception e) {
            System.err.println("SOCChannels parseDataStr ERROR - " + e);
            return null;
        }
        return new SOCChannels((Vector) cl);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString() {
        String s = "SOCChannels:channels=";
        try {
            Enumeration clEnum = channels.elements();
            s += (String) clEnum.nextElement();
            while (clEnum.hasMoreElements()) {
                s += ("," + (String) clEnum.nextElement());
            }
        } catch (Exception e) {
        }
        return s;
    }
}
