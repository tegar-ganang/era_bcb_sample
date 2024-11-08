package net.sourceforge.sandirc.gui;

import net.infonode.tabbedpanel.TabDragEvent;
import net.sourceforge.sandirc.utils.WindowUtilites;
import jerklib.Channel;
import jerklib.Session;
import net.infonode.tabbedpanel.Tab;
import net.infonode.tabbedpanel.TabDropDownListVisiblePolicy;
import net.infonode.tabbedpanel.TabLayoutPolicy;
import net.infonode.tabbedpanel.TabStateChangedEvent;
import net.infonode.tabbedpanel.TabbedPanel;
import net.infonode.tabbedpanel.titledtab.TitledTab;
import net.sourceforge.sandirc.InputListener;
import net.sourceforge.sandirc.UserInputHandler;
import net.sourceforge.sandirc.actions.DisconnectAction;
import net.sourceforge.sandirc.constants.IRCWindowConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.infonode.tabbedpanel.TabAdapter;
import net.sourceforge.sandirc.controllers.DocumentAdapterHandler;

/**
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 */
public class IRCWindowContainer extends TabbedPanel implements SandIRCContainer {

    private Session session;

    private IRCWindow noticeWindow;

    private Icon frameIcon;

    private final List<InputListener> listeners = new ArrayList<InputListener>(10);

    public IRCWindowContainer(Session session) {
        this();
        this.session = session;
    }

    public IRCWindowContainer() {
        listeners.add(UserInputHandler.getDefault());
        String m_location = "/net/sourceforge/sandirc/icons/Frame.gif";
        URL m_url = getClass().getResource(m_location);
        frameIcon = new ImageIcon(m_url);
        getProperties().setTabReorderEnabled(true);
        getProperties().setEnsureSelectedTabVisible(true);
        getProperties().setTabLayoutPolicy(TabLayoutPolicy.SCROLLING);
        getProperties().setTabDropDownListVisiblePolicy(TabDropDownListVisiblePolicy.TABS_NOT_VISIBLE);
        JButton closeAllButton = createCloseAllTabsButton(this);
        closeAllButton.addActionListener(new DisconnectAction());
        closeAllButton.setToolTipText("Close session");
        setTabAreaComponents(new JComponent[] { closeAllButton });
        addTabListener(new TabAdapter() {

            public void tabSelected(TabStateChangedEvent arg0) {
                TitledTab tab = (TitledTab) arg0.getTab();
                if (tab == null) {
                    return;
                }
                tab.getProperties().getNormalProperties().setTitleComponentVisible(false);
                tab.getProperties().getNormalProperties().setTextVisible(true);
            }
        });
    }

    private JButton createCloseAllTabsButton(final TabbedPanel tabbedPanel) {
        final JButton closeButton = createXButton();
        closeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int tabCount = tabbedPanel.getTabCount();
                for (int i = 0; i < tabCount; i++) removeTab(getTabAt(0));
            }
        });
        return closeButton;
    }

    public IRCWindow getSelectedWindow() {
        return (IRCWindow) getSelectedTab().getContentComponent();
    }

    private IRCWindow createPrivateMessageWindow(String nick) {
        final IRCWindow win = new IRCWindow(session, null, nick, IRCWindow.Type.PRIVATE);
        final TitledTab tab = new TitledTab(nick, frameIcon, win, null);
        win.getDocument().addDocumentListener(new DocumentAdapterHandler() {

            public void insertUpdate(DocumentEvent e) {
                if (!getSelectedWindow().equals(win)) {
                    tab.getProperties().getNormalProperties().setTitleComponentVisible(true);
                    tab.getProperties().getNormalProperties().setTextVisible(false);
                }
            }
        });
        JLabel label = new JLabel(nick);
        label.setForeground(Color.RED);
        tab.setNormalStateTitleComponent(label);
        tab.getProperties().getNormalProperties().setTitleComponentVisible(false);
        tab.getProperties().getNormalProperties().setTextVisible(true);
        tab.getProperties().getHighlightedProperties().setTitleComponentVisible(true);
        tab.putClientProperty(IRCWindowConstants.IRCWINDOW_NICK_PROPERTY, nick);
        tab.putClientProperty(IRCWindowConstants.IRCWINDOW_TYPE_PROPERTY, IRCWindowConstants.IRCWINDOW_TYPE_VALUES.CHANNEL_WINDOW);
        tab.putClientProperty(IRCWindowConstants.IRCWINDOW_DISCUSSION_TYPE_PROPERTY, IRCWindowConstants.DISCUSSION_TYPE.PRIVATE_DISCUSSION);
        addTab(tab);
        tab.setHighlightedStateTitleComponent(createCloseTabButton(tab));
        setSelectedTab(tab);
        return win;
    }

    public IRCWindow getPrivateMessageWindow(String nick, Session session) {
        IRCWindow win = null;
        if (nick == null) {
            return noticeWindow;
        }
        System.out.println("Calling look for " + nick);
        win = WindowUtilites.getWindowForPrivateMsg(nick, session, getIRCWindows());
        if (win == null) {
            win = createPrivateMessageWindow(nick);
        }
        return win;
    }

    public List<IRCWindow> getIRCWindows() {
        List<IRCWindow> li = new ArrayList<IRCWindow>();
        int count = getTabCount();
        for (int i = 0; i < count; i++) {
            Tab tab = getTabAt(i);
            IRCWindow win = (IRCWindow) tab.getContentComponent();
            li.add(win);
        }
        return li;
    }

    private void createIRCChannelWindow(Channel channel) {
        final IRCWindow win = new IRCWindow(session, channel, null, IRCWindow.Type.CHANNEL);
        final TitledTab tab = new TitledTab(channel.getName(), frameIcon, win, null);
        win.getDocument().addDocumentListener(new DocumentAdapterHandler() {

            public void insertUpdate(DocumentEvent e) {
                if (!getSelectedWindow().equals(win)) {
                    tab.getProperties().getNormalProperties().setTitleComponentVisible(true);
                    tab.getProperties().getNormalProperties().setTextVisible(false);
                }
            }
        });
        JLabel label = new JLabel(channel.getName());
        label.setForeground(Color.RED);
        tab.setNormalStateTitleComponent(label);
        tab.getProperties().getNormalProperties().setTitleComponentVisible(false);
        tab.getProperties().getNormalProperties().setTextVisible(true);
        tab.getProperties().getHighlightedProperties().setTitleComponentVisible(true);
        tab.putClientProperty(IRCWindowConstants.IRCWINDOW_TYPE_PROPERTY, IRCWindowConstants.IRCWINDOW_TYPE_VALUES.CHANNEL_WINDOW);
        tab.putClientProperty(IRCWindowConstants.IRCWINDOW_DISCUSSION_TYPE_PROPERTY, IRCWindowConstants.DISCUSSION_TYPE.PUBLIC_DISCUSSION);
        tab.putClientProperty(IRCWindowConstants.IRCWINDOW_CHANNEL_PROPERTY, channel);
        addTab(tab);
        tab.setHighlightedStateTitleComponent(createCloseTabButton(tab));
        setSelectedTab(tab);
    }

    public IRCWindow getNoticeWindow() {
        if (noticeWindow == null) {
            createNoticeWindow();
        }
        return noticeWindow;
    }

    private void createNoticeWindow() {
        noticeWindow = new IRCWindow(session, null, null, IRCWindow.Type.PRIVATE);
        Tab tab = new TitledTab("NOTICE", frameIcon, noticeWindow, null);
        addTab(tab);
        setSelectedTab(tab);
    }

    private JButton createCloseTabButton(final TitledTab tab) {
        JButton closeButton = createXButton();
        closeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Object windowType = tab.getClientProperty(IRCWindowConstants.IRCWINDOW_DISCUSSION_TYPE_PROPERTY);
                if (windowType != null) {
                    if (windowType.equals(IRCWindowConstants.DISCUSSION_TYPE.PUBLIC_DISCUSSION)) {
                        System.out.println("Parting closetab");
                        IRCWindow window = (IRCWindow) tab.getContentComponent();
                        Channel s = window.getDocument().getChannel();
                        String message = "SandIRC client, you know you want, some come and get it";
                        s.part(message);
                        removeTab(tab);
                    } else {
                        removeTab(tab);
                    }
                }
            }
        });
        return closeButton;
    }

    private JButton createXButton() {
        String loc = "/net/sourceforge/sandirc/icons/close16.gif";
        URL iconURL = getClass().getResource(loc);
        Icon closeIcon = new ImageIcon(iconURL);
        JButton closeButton = new JButton(closeIcon);
        closeButton.setBackground(getBackground());
        closeButton.setFocusable(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setOpaque(false);
        closeButton.setMargin(null);
        closeButton.setFont(closeButton.getFont().deriveFont(Font.BOLD).deriveFont((float) 10));
        closeButton.setBorder(new EmptyBorder(1, 1, 1, 1));
        return closeButton;
    }

    public void closeChannelTab(Channel channel) {
        Tab win = null;
        int count = getTabCount();
        for (int i = 0; i < count; i++) {
            Tab tab = getTabAt(i);
            Object windowType = tab.getClientProperty(IRCWindowConstants.IRCWINDOW_DISCUSSION_TYPE_PROPERTY);
            if (windowType == null) {
                continue;
            }
            if (windowType.equals(IRCWindowConstants.DISCUSSION_TYPE.PUBLIC_DISCUSSION)) {
            }
            Object channelProp = tab.getClientProperty(IRCWindowConstants.IRCWINDOW_CHANNEL_PROPERTY);
            if (channelProp == null) {
                continue;
            }
            if (channelProp.equals(channel)) {
                win = tab;
                break;
            }
        }
        if (win != null) {
            removeTab(win);
        }
    }

    public IRCWindow findWindowByChannel(Channel channel) {
        IRCWindow win = null;
        int count = getTabCount();
        for (int i = 0; i < count; i++) {
            Tab tab = getTabAt(i);
            Object publicDiscussionProperty = tab.getClientProperty(IRCWindowConstants.IRCWINDOW_DISCUSSION_TYPE_PROPERTY);
            if (publicDiscussionProperty == null) {
                continue;
            }
            Object channelProp = tab.getClientProperty(IRCWindowConstants.IRCWINDOW_CHANNEL_PROPERTY);
            if (channelProp == null) {
                continue;
            }
            if (channelProp.equals(channel)) {
                win = (IRCWindow) tab.getContentComponent();
                return win;
            }
        }
        if (win == null) {
            createIRCChannelWindow(channel);
            return win;
        }
        return null;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public String getName() {
        if (session == null || session.getRequestedConnection() == null) return null;
        return session.getRequestedConnection().getHostName();
    }

    public void addInputListener(InputListener listener) {
        listeners.add(listener);
    }

    public void removeInputListener(InputListener listener) {
        listeners.remove(listener);
    }

    public void receiveInput(String input) {
        IRCWindow window = getSelectedWindow();
        for (InputListener listener : listeners) listener.receiveInput(window, input);
    }

    public JComponent getComponent() {
        return this;
    }
}
