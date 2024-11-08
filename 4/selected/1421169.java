package net.sourceforge.sandirc.gui;

import java.awt.BorderLayout;
import jerklib.Channel;
import jerklib.Session;
import net.sourceforge.sandirc.configuration.StyleConfig;
import net.sourceforge.sandirc.controllers.DefaultPopupHandler;
import net.sourceforge.sandirc.controllers.PopupHandler;
import net.sourceforge.sandirc.gui.text.IRCDocument;
import net.sourceforge.sandirc.gui.text.syntax.SyntaxTextUI;
import net.sourceforge.sandirc.gui.text.syntax.tokens.Token;
import net.sourceforge.sandirc.utils.DateUtils;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.DocumentListener;
import net.sourceforge.sandirc.actions.ActionsContainer;
import net.sourceforge.sandirc.actions.HideTopicAction;
import net.sourceforge.sandirc.actions.MsgAction;
import net.sourceforge.sandirc.actions.QueryAction;
import net.sourceforge.sandirc.actions.ShowUserListAction;
import net.sourceforge.sandirc.actions.UserInfoAction;
import org.jdesktop.jdic.desktop.Desktop;
import org.jdesktop.jdic.desktop.DesktopException;

public class IRCWindow extends JPanel {

    private JTextArea area;

    private JScrollPane areaScroller;

    private JScrollPane topicFieldScroller;

    private JPanel topicPanel, userListPanel;

    private DefaultListModel usersListModel;

    private JTextArea topicField;

    private JPopupMenu usersListPopup;

    private JTextField topicMetaData;

    private SearchPanel searchPanel;

    private JComponent scPanel;

    private JList usersList;

    private JSplitPane sp;

    private IRCWindowContainer container = SandIRCFrame.getInstance().getCurrentWindowContainer();

    /**
     * @param session
     * @param channel
     * @param nick
     * @param type
     */
    public IRCWindow(Session session, Channel channel, String nick, Type type) {
        super(new GridBagLayout());
        initGui(session, channel, nick, type);
    }

    private JScrollPane initTextArea(Session session, Channel channel, String nick, Type type) {
        area = new JTextArea();
        area.setBackground(StyleConfig.getInstance().getProfile().get(1000000001).bg);
        area.setUI(new SyntaxTextUI());
        final IRCDocument doc = new IRCDocument(session, channel, nick, type, area, StyleConfig.getInstance().getProfile());
        DocumentListener[] listeners = doc.getDocumentListeners();
        for (DocumentListener l : listeners) {
            if (l.getClass().getName().equals("javax.swing.text.DefaultCaret$Handler")) {
                doc.removeDocumentListener(l);
            }
        }
        area.setEditable(false);
        area.setLineWrap(true);
        area.addMouseListener(new DefaultPopupHandler());
        searchPanel = new SearchPanel(area);
        area.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "Search");
        area.getActionMap().put("Search", new SearchAction("Search"));
        area.addMouseListener(new MouseAdapter() {

            public void mousePressed(final MouseEvent e) {
                int offs = area.viewToModel(e.getPoint());
                final Token t = doc.getTokenForOffset(offs);
                if (t != null) {
                    if (t.type == 1005) {
                        try {
                            Desktop.browse(new URL(t.data));
                        } catch (MalformedURLException e1) {
                            e1.printStackTrace();
                        } catch (DesktopException e1) {
                            e1.printStackTrace();
                        }
                    } else if (t.type == 1002 | t.type == 1006 | t.type == 1007) {
                        doc.getSession().whois(t.data);
                    } else {
                        System.out.println(t.type + " " + t.data);
                    }
                } else {
                    System.err.println("Token was null");
                }
            }
        });
        areaScroller = new JScrollPane(area);
        return areaScroller;
    }

    private JPanel initTopicPanel() {
        topicPanel = new JPanel(new GridBagLayout());
        Border outsideBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);
        Border insideBorder = BorderFactory.createTitledBorder("Channel Topic");
        topicPanel.setBorder(new CompoundBorder(outsideBorder, insideBorder));
        topicField = new JTextArea();
        topicField.setOpaque(false);
        topicField.setLineWrap(true);
        topicField.setWrapStyleWord(true);
        topicField.setBorder(BorderFactory.createEmptyBorder());
        topicField.setMargin(new Insets(0, 0, 0, 0));
        topicField.setEditable(false);
        topicField.setAlignmentX(Component.LEFT_ALIGNMENT);
        topicFieldScroller = new JScrollPane(topicField);
        topicFieldScroller.setBorder(BorderFactory.createEmptyBorder());
        topicMetaData = new JTextField();
        topicMetaData.setBackground(topicPanel.getBackground());
        topicMetaData.setBorder(null);
        topicMetaData.setEditable(false);
        Bag bag = new Bag();
        topicPanel.add(topicFieldScroller, bag.fillX());
        topicPanel.add(topicMetaData, bag.nextY());
        return topicPanel;
    }

    public JList getUsersList() {
        return usersList;
    }

    private JPanel initUserList() {
        userListPanel = new JPanel(new GridBagLayout());
        usersListModel = new DefaultListModel();
        usersList = new JList(usersListModel);
        usersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        usersList.setCellRenderer(new UsersListCellRender());
        usersListPopup = new JPopupMenu();
        usersList.addMouseListener(new PopupHandler(usersListPopup) {

            public void mouseReleased(MouseEvent e) {
                if (usersList.getSelectedIndex() == -1) return;
                super.mouseReleased(e);
            }
        });
        usersList.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                int index = usersList.getSelectedIndex();
                if (index == -1) {
                    return;
                }
                if (e.getClickCount() == 2) {
                    try {
                        String selectedNick = usersList.getSelectedValue().toString();
                        container.getPrivateMessageWindow(selectedNick, getDocument().getSession());
                    } catch (Exception err) {
                    }
                }
            }
        });
        Class[] popupActionsClasses = { MsgAction.class, QueryAction.class, UserInfoAction.class };
        for (Class m_class : popupActionsClasses) {
            usersListPopup.add(ActionsContainer.getINSTANCE().getActionForClass(m_class));
        }
        String loc = "/net/sourceforge/sandirc/icons/close16.gif";
        URL iconURL = getClass().getResource(loc);
        Icon closeIcon = new ImageIcon(iconURL);
        JButton closeButton = new ImageButton(closeIcon);
        closeButton.setBackground(getBackground());
        closeButton.setFocusable(false);
        closeButton.setToolTipText("Hide users list");
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setOpaque(false);
        closeButton.setMargin(null);
        closeButton.addActionListener(ActionsContainer.getINSTANCE().getActionForClass(ShowUserListAction.class));
        Bag bag = new Bag();
        userListPanel.add(new JLabel("Users"), bag.fillX());
        userListPanel.add(closeButton, bag.nextX().fillNone());
        userListPanel.add(new JScrollPane(usersList), bag.resetX().nextY().fillBoth().colspan(2));
        return userListPanel;
    }

    private void initGui(Session session, Channel channel, String nick, Type type) {
        setBackground(new Color(239, 235, 231));
        scPanel = new JPanel(new BorderLayout());
        JScrollPane pane = initTextArea(session, channel, nick, type);
        scPanel.add(pane, BorderLayout.CENTER);
        if (channel != null) {
            JPanel panel = new JPanel(new GridBagLayout());
            Bag bag = new Bag();
            panel.add(initTopicPanel(), bag.fillX());
            panel.add(scPanel, bag.nextY().fillBoth());
            sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, panel, initUserList());
            sp.setResizeWeight(0.80F);
            sp.setDividerLocation(0.80F);
            sp.setDividerSize(5);
            add(sp, new Bag().fillBoth());
            updateUsersList();
            topicField.setText(channel.getTopic());
            String meta = "Set by " + channel.getTopicSetter();
            try {
                meta += " on " + DateUtils.getTime(channel.getTopicSetTime());
            } catch (Exception exe) {
            }
            topicMetaData.setText(meta);
        } else {
            add(scPanel, new Bag().fillBoth());
        }
    }

    public void showUserList(boolean b) {
        userListPanel.setVisible(b);
        if (b) {
            sp.setDividerLocation(0.88F);
            sp.setDividerSize(5);
        } else {
            sp.setDividerSize(0);
        }
        revalidate();
    }

    public boolean isUserListShowing() {
        return userListPanel.isVisible();
    }

    public JPanel getTopicPanel() {
        return topicPanel;
    }

    public void showTopic(boolean b) {
        topicPanel.setVisible(b);
    }

    public void toggelTopicVisible() {
        boolean b = !topicPanel.isVisible();
        Action action = ActionsContainer.getINSTANCE().getActionForClass(HideTopicAction.class);
        if (b) {
            action.putValue(Action.NAME, "Hide topic");
        } else {
            action.putValue(Action.NAME, "Show topic");
        }
        topicPanel.setVisible(b);
    }

    private class NickComparator implements Comparator<String> {

        public int compare(String o1, String o2) {
            return o1.toLowerCase().compareTo(o2.toLowerCase());
        }
    }

    public void updateUsersList() {
        usersListModel.removeAllElements();
        Channel channel = getDocument().getChannel();
        List<String> muteableNicks = new ArrayList<String>(channel.getNicks());
        Session session = getDocument().getSession();
        Map<String, String> nickPrefixMap = session.getServerInformation().getNickPrefixMap();
        Map<String, String> modeNicksMap = new HashMap<String, String>();
        for (String prefix : nickPrefixMap.keySet()) {
            List<String> modeNicks = channel.getNicksForMode(nickPrefixMap.get(prefix));
            Collections.sort(modeNicks, new NickComparator());
            for (String nick : modeNicks) {
                if (!modeNicksMap.containsKey(nick)) {
                    modeNicksMap.put(nick, prefix);
                    muteableNicks.remove(nick);
                    usersListModel.addElement(prefix + nick);
                }
            }
        }
        Collections.sort(muteableNicks, new NickComparator());
        for (String aNick : muteableNicks) {
            usersListModel.addElement(aNick);
        }
    }

    public void insertMsg(String nick, String data) {
        BoundedRangeModel model = areaScroller.getVerticalScrollBar().getModel();
        if ((model.getExtent() + model.getValue() + 1) >= model.getMaximum()) {
            getDocument().insertMsg(nick, data);
            area.setCaretPosition(area.getText().length());
        } else {
            getDocument().insertMsg(nick, data);
        }
    }

    public void insertDefault(String msg) {
        BoundedRangeModel model = areaScroller.getVerticalScrollBar().getModel();
        if ((model.getExtent() + model.getValue() + 1) >= model.getMaximum()) {
            getDocument().insertDefault(msg);
            area.setCaretPosition(area.getText().length());
        } else {
            getDocument().insertDefault(msg);
        }
    }

    class UsersListCellRender extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String nick = value.toString();
            if (nick.startsWith("@") || nick.startsWith("+")) nick = nick.substring(1);
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setFont(new Font("verdana", Font.PLAIN, 12));
            if (nick.equals(IRCWindow.this.getDocument().getSession().getNick())) {
                label.setForeground(Color.RED);
            } else {
                label.setForeground(Color.BLACK);
            }
            return label;
        }
    }

    class SearchAction extends AbstractAction {

        public SearchAction(String name) {
            super(name);
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            if (!searchPanel.isShowing()) {
                scPanel.add(searchPanel, BorderLayout.SOUTH);
                scPanel.invalidate();
                scPanel.revalidate();
                scPanel.repaint();
            }
            searchPanel.focusSearchField();
        }
    }

    public IRCDocument getDocument() {
        return (IRCDocument) area.getDocument();
    }

    public Session getSession() {
        return getDocument().getSession();
    }

    public Type getType() {
        return getDocument().getType();
    }

    public Channel getChannel() {
        return getDocument().getChannel();
    }

    public String getNick() {
        return getDocument().getNick();
    }

    public String getWindowName() {
        Channel ch = getChannel();
        if (ch != null) return ch.getName();
        String nick = getNick();
        if (nick != null) return nick;
        return null;
    }

    public enum Type {

        MAIN, CHANNEL, PRIVATE
    }
}
