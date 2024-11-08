package sand.gui;

import java.util.ArrayList;
import java.util.List;
import jerklib.Channel;
import jerklib.Session;

public class WindowUtilites {

    public static Window getWindowForDocument(IRCDocument document, List<Window> windows) {
        for (Window win : windows) {
            if (win.getDocument().equals(document)) return win;
        }
        return null;
    }

    public static List<Window> getWindowsForSession(Session session, List<Window> windows) {
        List<Window> returnList = new ArrayList<Window>();
        for (Window win : windows) {
            if (session.equals(win.getDocument().getSession())) {
                returnList.add(win);
            }
        }
        return returnList;
    }

    public static List<Window> getWindowsForNick(String nick, Session session, List<Window> windows) {
        List<Window> returnList = new ArrayList<Window>();
        List<Window> sessionWins = getWindowsForSession(session, windows);
        if (nick.equals(session.getNick())) {
            return sessionWins;
        }
        for (Window win : sessionWins) {
            IRCDocument doc = win.getDocument();
            if (doc.getType() == IRCDocument.Type.PRIV && (nick.equals(doc.getNick()) || session.getNick().equals(nick))) {
                returnList.add(win);
            } else if (doc.getType() != IRCDocument.Type.PRIV) {
                Channel chan = doc.getChannel();
                if (chan.getNicks().contains(nick)) {
                    returnList.add(win);
                }
            }
        }
        return returnList;
    }

    public static Window getWindowForPrivateMsg(String nick, Session session, List<Window> windows) {
        List<Window> sessionWins = getWindowsForSession(session, windows);
        for (Window window : sessionWins) {
            if (nick.equals(window.getDocument().getNick())) {
                return window;
            }
        }
        return null;
    }

    public static Window getWindowForChannel(Channel channel, Session session, List<Window> windows) {
        List<Window> sessionWins = getWindowsForSession(session, windows);
        for (Window window : sessionWins) {
            if (channel.equals(window.getDocument().getChannel())) {
                return window;
            }
        }
        return null;
    }

    public static Window getBaseWindow() {
        return BaseWindow.getInstance().mainWindow;
    }
}
