package org.speakmon.coffeehouse.swingui;

import javax.swing.*;
import java.util.Comparator;
import org.speakmon.coffeehouse.User;
import org.speakmon.coffeehouse.ServerListener;
import org.speakmon.coffeehouse.protocol.IRCMessageFactory;
import java.awt.Color;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * GuiUser.java
 *
 *
 * Created: Thu Dec 12 01:42:57 2002
 *
 * @author <a href="mailto:ben@speakmon.org">Ben Speakmon</a>
 * @version 1.0
 */
final class GuiUser {

    public static final Comparator ORDER = new ORDER();

    private User user;

    private ImageIcon userIcon;

    private JPopupMenu popupMenu;

    private Color[] nickColors = new Color[] { Color.WHITE, Color.BLACK, Color.BLUE, Color.GREEN, Color.RED, new Color(128, 0, 0), new Color(128, 0, 128), Color.ORANGE, Color.YELLOW, new Color(0, 255, 0), new Color(0, 128, 128), Color.CYAN, new Color(0, 0, 255), Color.PINK, Color.DARK_GRAY, Color.LIGHT_GRAY };

    public GuiUser(User userObj) {
        this.user = new User(userObj);
        if (user.isOp()) {
            userIcon = UserIcons.getUserIcon("op");
        } else if (user.isVoiced()) {
            userIcon = UserIcons.getUserIcon("voice");
        }
    }

    public ImageIcon getIcon() {
        return userIcon;
    }

    public User getUser() {
        return user;
    }

    public String getNick() {
        return user.getVisibleNick();
    }

    public String toString() {
        return "GuiUser (" + user.toString() + ")";
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof GuiUser) {
            GuiUser user = (GuiUser) obj;
            return this.getUser().equals(user.getUser());
        } else {
            return false;
        }
    }

    /**
     * Describe <code>hashCode</code> method here.
     *
     * @return an <code>int</code> value
     */
    public int hashCode() {
        return user.hashCode();
    }

    protected JPopupMenu getPopupMenu() {
        popupMenu = new JPopupMenu();
        JMenu userInfoMenu = new JMenu(getNick());
        popupMenu.add(userInfoMenu);
        JMenuItem menuItem = new JMenuItem("User: " + user.getUsername() + "@" + user.getHostname());
        userInfoMenu.add(menuItem);
        menuItem = new JMenuItem("Real name: " + user.getRealname());
        userInfoMenu.add(menuItem);
        menuItem = new JMenuItem("Server: " + user.getServername());
        userInfoMenu.add(menuItem);
        menuItem = new JMenuItem("Last Msg: ");
        userInfoMenu.add(menuItem);
        JMenu ctcpMenu = new JMenu("CTCP");
        popupMenu.add(ctcpMenu);
        ActionListener ctcpActionListener = new CtcpActionListener();
        menuItem = new JMenuItem("FINGER");
        menuItem.addActionListener(ctcpActionListener);
        ctcpMenu.add(menuItem);
        menuItem = new JMenuItem("VERSION");
        menuItem.addActionListener(ctcpActionListener);
        ctcpMenu.add(menuItem);
        menuItem = new JMenuItem("USERINFO");
        menuItem.addActionListener(ctcpActionListener);
        ctcpMenu.add(menuItem);
        menuItem = new JMenuItem("CLIENTINFO");
        menuItem.addActionListener(ctcpActionListener);
        ctcpMenu.add(menuItem);
        menuItem = new JMenuItem("PING");
        menuItem.addActionListener(ctcpActionListener);
        ctcpMenu.add(menuItem);
        menuItem = new JMenuItem("TIME");
        menuItem.addActionListener(ctcpActionListener);
        ctcpMenu.add(menuItem);
        JMenu modeMenu = new JMenu("Mode");
        popupMenu.add(modeMenu);
        ActionListener modeActionListener = new ModeActionListener();
        menuItem = new JMenuItem("Give Voice");
        menuItem.addActionListener(modeActionListener);
        modeMenu.add(menuItem);
        menuItem = new JMenuItem("Take Voice");
        menuItem.addActionListener(modeActionListener);
        modeMenu.add(menuItem);
        modeMenu.addSeparator();
        menuItem = new JMenuItem("Give Ops");
        menuItem.addActionListener(modeActionListener);
        modeMenu.add(menuItem);
        menuItem = new JMenuItem("Take Ops");
        menuItem.addActionListener(modeActionListener);
        modeMenu.add(menuItem);
        JMenu infoMenu = new JMenu("Info");
        popupMenu.add(infoMenu);
        ActionListener infoActionListener = new InfoActionListener();
        menuItem = new JMenuItem("Who");
        menuItem.addActionListener(infoActionListener);
        infoMenu.add(menuItem);
        menuItem = new JMenuItem("Whois");
        menuItem.addActionListener(infoActionListener);
        infoMenu.add(menuItem);
        menuItem = new JMenuItem("DNS Lookup");
        menuItem.addActionListener(infoActionListener);
        infoMenu.add(menuItem);
        menuItem = new JMenuItem("Trace");
        menuItem.addActionListener(infoActionListener);
        infoMenu.add(menuItem);
        menuItem = new JMenuItem("UserHost");
        menuItem.addActionListener(infoActionListener);
        infoMenu.add(menuItem);
        JMenu kbMenu = new JMenu("Kick/Ban");
        popupMenu.add(kbMenu);
        ActionListener kbActionListener = new KickBanActionListener();
        menuItem = new JMenuItem("Kick");
        menuItem.addActionListener(kbActionListener);
        kbMenu.add(menuItem);
        menuItem = new JMenuItem("Ban");
        menuItem.addActionListener(kbActionListener);
        kbMenu.add(menuItem);
        return popupMenu;
    }

    private final class CtcpActionListener implements ActionListener {

        /**
	 * Describe <code>actionPerformed</code> method here.
	 *
	 * @param actionEvent an <code>ActionEvent</code> value
	 */
        public void actionPerformed(ActionEvent actionEvent) {
            JMenuItem source = (JMenuItem) actionEvent.getSource();
            String ctcpCommand = source.getText();
            String nick = getNick();
            user.getChannel().getServer().write(IRCMessageFactory.createCtcpClientMessage(nick, ctcpCommand));
        }
    }

    private final class ModeActionListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent) {
            JMenuItem source = (JMenuItem) actionEvent.getSource();
            String modeCommand = source.getText();
            String nick = getNick();
            ServerListener chan = user.getChannel();
            StringBuffer commandBuf = new StringBuffer(32);
            if (modeCommand.equals("Give Voice")) {
                commandBuf.append("/mode ").append(chan.getName());
                commandBuf.append(" +v ").append(nick);
            } else if (modeCommand.equals("Take Voice")) {
                commandBuf.append("/mode ").append(chan.getName());
                commandBuf.append(" -v ").append(nick);
            } else if (modeCommand.equals("Give Ops")) {
                commandBuf.append("/mode ").append(chan.getName());
                commandBuf.append(" +o ").append(nick);
            } else if (modeCommand.equals("Take Ops")) {
                commandBuf.append("/mode ").append(chan.getName());
                commandBuf.append(" -o ").append(nick);
            }
            chan.getServer().write(IRCMessageFactory.createClientMessage(commandBuf.toString(), chan.getName()));
        }
    }

    private final class InfoActionListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent) {
            JMenuItem source = (JMenuItem) actionEvent.getSource();
            String infoCommand = source.getText();
            ServerListener chan = user.getChannel();
            StringBuffer commandBuf = new StringBuffer(32);
            if (infoCommand.equals("Who")) {
                commandBuf.append("/who ").append(getNick());
            } else if (infoCommand.equals("Whois")) {
                commandBuf.append("/whois ").append(getNick());
            } else if (infoCommand.equals("DNS Lookup")) {
            } else if (infoCommand.equals("Trace")) {
                commandBuf.append("/trace ").append(getNick());
            } else if (infoCommand.equals("UserHost")) {
                commandBuf.append("/userhost ").append(getNick());
            }
            chan.getServer().write(IRCMessageFactory.createClientMessage(commandBuf.toString(), chan.getName()));
        }
    }

    private final class KickBanActionListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent) {
            JMenuItem source = (JMenuItem) actionEvent.getSource();
            String kbCommand = source.getText();
            ServerListener chan = user.getChannel();
            StringBuffer commandBuf = new StringBuffer(32);
            if (kbCommand.equals("Kick")) {
                commandBuf.append("/kick ").append(chan.getName());
                commandBuf.append(" ").append(user.getNick());
            } else if (kbCommand.equals("Ban")) {
                commandBuf.append("/mode ").append(chan.getName());
                commandBuf.append(" +b *!*@*.");
            }
            chan.getServer().write(IRCMessageFactory.createClientMessage(commandBuf.toString(), chan.getName()));
        }
    }

    private static final class ORDER implements Comparator {

        public int compare(Object a, Object b) {
            if (a instanceof GuiUser || b instanceof GuiUser) {
                GuiUser user1 = (GuiUser) a;
                GuiUser user2 = (GuiUser) b;
                return user1.getUser().compareTo(user2.getUser());
            } else {
                throw new IllegalArgumentException("tried to compare unrelated objects: " + a + "/" + b);
            }
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else {
                return false;
            }
        }
    }
}
