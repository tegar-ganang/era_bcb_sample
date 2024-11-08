package net.sourceforge.sandirc.utils;

import net.sourceforge.sandirc.gui.*;
import jerklib.Channel;
import jerklib.Session;
import net.sourceforge.sandirc.gui.IRCWindow.Type;
import net.sourceforge.sandirc.gui.text.IRCDocument;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mohadib
 *
 */
public class WindowUtilites {

    /**
     *
     * @param document
     * @param windows
     * @return
     */
    public static IRCWindow getWindowForDocument(IRCDocument document, List<IRCWindow> windows) {
        for (IRCWindow win : windows) {
            if (win.getDocument().equals(document)) {
                return win;
            }
        }
        return null;
    }

    public static IRCWindowContainer getCurrentWindowContainer() {
        return SandIRCFrame.getInstance().getCurrentWindowContainer();
    }

    public static IRCWindow getSelectedIRCWindow() {
        return getCurrentWindowContainer().getSelectedWindow();
    }

    /**
     *
     * @param session
     * @param windows
     * @return
     */
    public static List<IRCWindow> getWindowsForSession(Session session, List<IRCWindow> windows) {
        List<IRCWindow> returnList = new ArrayList<IRCWindow>();
        for (IRCWindow win : windows) {
            if (session.equals(win.getDocument().getSession())) {
                returnList.add(win);
            }
        }
        return returnList;
    }

    public static List<IRCWindow> getWindowsForNick(String nick, Session session, List<IRCWindow> windows) {
        List<IRCWindow> returnList = new ArrayList<IRCWindow>();
        if (nick.equals(session.getNick())) {
            return windows;
        }
        for (IRCWindow win : windows) {
            IRCDocument doc = win.getDocument();
            if (nick.equals(doc.getNick())) {
                returnList.add(win);
            } else if (doc.getType() != IRCWindow.Type.PRIVATE) {
                Channel chan = doc.getChannel();
                if (chan.getNicks().contains(nick)) {
                    returnList.add(win);
                }
            }
        }
        return returnList;
    }

    /**
     *
     * @param nick
     * @param session
     * @param windows
     * @return
     */
    public static IRCWindow getWindowForPrivateMsg(String nick, Session session, List<IRCWindow> windows) {
        for (IRCWindow window : windows) {
            if (nick.equals(window.getDocument().getNick())) {
                return window;
            }
        }
        return null;
    }

    /**
     *
     * @param channel
     * @param session
     * @param windows
     * @return
     */
    public static IRCWindow getWindowForChannel(Channel channel, Session session, List<IRCWindow> windows) {
        List<IRCWindow> sessionWins = getWindowsForSession(session, windows);
        for (IRCWindow window : sessionWins) {
            if (channel.equals(window.getDocument().getChannel())) {
                return window;
            }
        }
        return null;
    }

    /**
     *
     * @return
     */
    public static IRCWindow getBaseWindow() {
        return null;
    }
}
