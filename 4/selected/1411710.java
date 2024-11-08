package sand.gui;

import java.awt.Color;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import jerklib.Channel;
import jerklib.Session;

public class IRCDocument extends PlainDocument {

    private final Session session;

    private final Channel channel;

    private final Type type;

    private String nick;

    private final DateFormat df = new SimpleDateFormat("hh:mm");

    private JTextArea area;

    public enum Type {

        MAIN, CHANNEL, PRIV
    }

    public IRCDocument(Session session, Channel channel, String nick, Type type, JTextArea area) {
        this.area = area;
        this.session = session;
        this.channel = channel;
        this.nick = nick;
        this.type = type;
    }

    public Session getSession() {
        return session;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getNick() {
        return nick;
    }

    public Type getType() {
        return type;
    }

    public static String formatNick(String nick) {
        return "< " + nick + " > ";
    }

    public void insertDefault(String data) {
        try {
            insertString(getLength(), data + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        area.repaint();
    }

    public void insertMsg(String nick, String data) {
        String msg = formatNick(nick) + data + "\n";
        try {
            insertString(getLength(), msg, null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
