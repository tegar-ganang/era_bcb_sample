package net.sourceforge.sandirc.gui;

import jerklib.Channel;
import jerklib.Session;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 *
 * @author Propriétaire
 */
public class IRCDocument extends PlainDocument {

    private final Session session;

    private final Channel channel;

    private final Type type;

    private String nick;

    private final DateFormat df = new SimpleDateFormat("hh:mm");

    private JTextArea area;

    /**
     *
     * @param session
     * @param channel
     * @param nick
     * @param type
     * @param area
     */
    public IRCDocument(Session session, Channel channel, String nick, Type type, JTextArea area) {
        this.area = area;
        this.session = session;
        this.channel = channel;
        this.nick = nick;
        this.type = type;
    }

    /**
     *
     * @return
     */
    public Session getSession() {
        return session;
    }

    /**
     *
     * @return
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     *
     * @return
     */
    public String getNick() {
        return nick;
    }

    /**
     *
     * @return
     */
    public Type getType() {
        return type;
    }

    /**
     *
     * @param nick
     * @return
     */
    public static String formatNick(String nick) {
        return "< " + nick + " > ";
    }

    /**
     *
     * @param data
     */
    public void insertDefault(String data) {
        try {
            insertString(getLength(), data + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        area.repaint();
    }

    /**
     *
     * @param nick
     * @param data
     */
    public void insertMsg(String nick, String data) {
        String msg = formatNick(nick) + data + "\n";
        try {
            insertString(getLength(), msg, null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public enum Type {

        MAIN, CHANNEL, PRIV
    }
}
