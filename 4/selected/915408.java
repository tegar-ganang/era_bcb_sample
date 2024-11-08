package org.furthurnet.furi;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;

/**
 *  
 */
public class FurthurChatPane extends JSplitPane {

    /**
     * a static pointer to the IrcManager 
     */
    private static IrcManager ircMgr = ServiceManager.getIrcManager();

    /**
     *  
     */
    public boolean mAuthenticated = false;

    /**
     * The table that lists the irc channels, user join status and number of
     * users. 
     */
    JTable ircChannelTable;

    /**
     * The button that when clicked sends the user's message to the selected 
     * channel.   
     */
    JButton ircSendButton;

    /**
     *  The textfield where the user types messages to be sent to the selected
     * channel.
     */
    JTextField ircSendText;

    /**
     *  the list of users in the selected channel.
     */
    JList ircUsersList;

    /**
     * the button when invoked opens an private message dialog
     */
    private JButton instantMsgButton = null;

    /**
     * the checkbox when checked auto scrolls the chat text
     */
    private JCheckBox ircAutoScrollChatText;

    /**
     * boolean to see if channel got changed
     */
    private boolean ircChannelChanged = false;

    /**
     * the model (data store) for the IRC channel table 
     */
    private AbstractTableModel ircChannelModel;

    /**
     * used to sort the IRC channels in the table 
     */
    private TableSorter ircChannelSorter;

    /**
     *  ??
     */
    private JLabel ircChannelStatus;

    /**
     * The model (data) for any channel text
     */
    private StyledDocument ircChatLogDoc;

    /**
     * the style used to set background and foreground colors, font size 
     * and type 
     */
    private Style ircChatLogStyle;

    /**
     * the text pane for any channel text 
     */
    private JTextPane ircChatLogText;

    /**
     * the scrollpane for the text pane 
     */
    private JScrollPane ircChatLogTextScrollPane;

    /**
     *  the button when invoked connects to the IRC server
     */
    private JButton ircconnectButton = null;

    /**
     * used to display IRC server connect delay time
     */
    private int ircConnectDelay = 0;

    /**
     * pointer to the current channel 
     */
    private int ircCurrentChannel = 0;

    /**
     * the button when invoked disconnects from the IRC server
     */
    private JButton ircDisconnectButton = null;

    /**
     * the label that displays the irc channel identifier  
     */
    private JLabel ircInfoLabel;

    /**
     * the button when invoked joins the IRC Channel
     */
    private JButton ircJoinButton = null;

    /**
     * the button when invoked leaves the IRC Channel
     */
    private JButton ircLeaveButton = null;

    /**
     * the checkbox that when selected, pings the irc chat server  
     */
    private JCheckBox ircPingChat;

    /**
     *  
     */
    private JPopupMenu ircPopupMenu;

    /**
     *  the label for the send button
     */
    private JLabel ircSendLabel;

    /**
     *  
     */
    private JPopupMenu ircUserPopupMenu;

    /**
     *  the model (data) for irc users in a channel 
     */
    private IrcUserListModel ircUsersModel;

    /**
     * a pointer to the parent frame 
     */
    private MainFrame mainFrame = null;

    /**
     *  the vector that stores message sent history for the user 
     */
    private Vector messageHistory = new Vector();

    /**
     * a pointer that is used to store current message index in the
     * message history
     */
    private int messagePointer = 0;

    /**
     *  
     */
    FurthurChatPane(MainFrame parent) {
        this.mainFrame = parent;
        this.setOrientation(JSplitPane.VERTICAL_SPLIT);
        init();
        ircMgr.addIrcChannels();
        ircMgr.addServerChangedListener(new IrcServerChangedListener());
        ircMgr.addChannelChangedListener(new IrcChannelChangedListener());
        ircMgr.addUserChangedListener(new IrcUserChangedListener());
        ircMgr.addMsgChangedListener(new IrcMsgChangedListener());
    }

    public static String getNickname() {
        return ServiceManager.getCfg().mIrcNickname;
    }

    public static void startIrcMgr() {
        if (ServiceManager.getCfg().mAutoConnectIrc && (getNickname() != null) && (getNickname().length() > 0)) {
            try {
                ServiceManager.getIdentd().startup();
                ircMgr.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void clearIrcWindows() {
        clearIrcWindows(true);
    }

    public void clearIrcWindows(boolean clearChannelCache) {
        ircChatLogText.setText("");
        ircUsersList.removeAll();
        if (clearChannelCache) {
            Vector list = ircMgr.getChannels();
            for (int i = 0; i < list.size(); i++) ((IrcChannel) list.elementAt(i)).clearAll();
        }
    }

    public void enableIrcButtons() {
        boolean connected = (ircMgr.getIsConnected()) && (mAuthenticated);
        ircconnectButton.setEnabled(!connected);
        ircDisconnectButton.setEnabled(connected);
        ircJoinButton.setEnabled(connected);
        ircLeaveButton.setEnabled(connected);
        instantMsgButton.setEnabled(connected);
    }

    public void init() {
        setupPopupMenus();
        JPanel ircChannelPanel = new JPanel(new BorderLayout());
        ircChannelPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4), BorderFactory.createEtchedBorder()), BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        ircInfoLabel = new JLabel("IRC:");
        ircChannelModel = new IrcChannelTableModel();
        ircChannelSorter = new TableSorter(ircChannelModel);
        ircChannelTable = new JTable(ircChannelSorter);
        ircChannelTable.getSelectionModel().addListSelectionListener(new IrcChannelSelectionHandler());
        ircChannelTable.setCellSelectionEnabled(false);
        ircChannelTable.setColumnSelectionAllowed(false);
        ircChannelTable.setRowSelectionAllowed(true);
        ircChannelTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ircChannelTable.addMouseListener(new MouseHandler());
        ircChannelSorter.addMouseListenerToHeaderInTable(ircChannelTable);
        ircconnectButton = new JButton(Res.getStr("Main.ChatTab.IrcConnectButton.Title"));
        ircDisconnectButton = new JButton(Res.getStr("Main.ChatTab.IrcDisconnectButton.Title"));
        ircJoinButton = new JButton(Res.getStr("Main.ChatTab.IrcJoinButton.Title"));
        ircLeaveButton = new JButton(Res.getStr("Main.ChatTab.IrcLeaveButton.Title"));
        instantMsgButton = new JButton(Res.getStr("Main.ChatTab.InstantMsgButton.Title"));
        ircconnectButton.setToolTipText(Res.getStr("Main.ChatTab.IrcConnectButton.ToolTip"));
        ircDisconnectButton.setToolTipText(Res.getStr("Main.ChatTab.IrcDisconnectButton.ToolTip"));
        ircJoinButton.setToolTipText(Res.getStr("Main.ChatTab.IrcJoinButton.ToolTip"));
        ircLeaveButton.setToolTipText(Res.getStr("Main.ChatTab.IrcLeaveButton.ToolTip"));
        instantMsgButton.setToolTipText(Res.getStr("Main.ChatTab.InstantMsgButton.ToolTip"));
        ircconnectButton.addActionListener(new ActionIRCConnect(mainFrame, null, null));
        ircDisconnectButton.addActionListener(new ActionIRCDisconnect(mainFrame, null, null));
        ircJoinButton.addActionListener(new ActionIRCJoinChannel(mainFrame, null, null));
        ircLeaveButton.addActionListener(new ActionIRCLeaveChannel(mainFrame, null, null));
        instantMsgButton.addActionListener(new ActionIRCPrivateChat(mainFrame, null, null));
        instantMsgButton.setRequestFocusEnabled(false);
        enableIrcButtons();
        ircconnectButton.setIcon(Res.getIcon("ConnectIrc.Icon"));
        ircDisconnectButton.setIcon(Res.getIcon("DisconnectIrc.Icon"));
        ircJoinButton.setIcon(Res.getIcon("JoinIrc.Icon"));
        ircLeaveButton.setIcon(Res.getIcon("LeaveIrc.Icon"));
        instantMsgButton.setIcon(Res.getIcon("PrivateIrc.Icon"));
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 2));
        JPanel innerpanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        innerpanel.add(ircconnectButton);
        innerpanel.add(ircDisconnectButton);
        buttonPanel.add(innerpanel);
        innerpanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        innerpanel.add(ircJoinButton);
        innerpanel.add(ircLeaveButton);
        buttonPanel.add(innerpanel);
        JPanel topPanel = new JPanel();
        JPanel chatInfoPanel = new JPanel(new BorderLayout());
        topPanel.setLayout(new GridLayout(2, 1));
        topPanel.add(buttonPanel);
        topPanel.add(chatInfoPanel);
        chatInfoPanel.add(BorderLayout.WEST, ircInfoLabel);
        ircChannelPanel.add(BorderLayout.NORTH, topPanel);
        ircChannelPanel.add(BorderLayout.CENTER, new JScrollPane(ircChannelTable));
        JPanel ircChatPanel = new JPanel(new BorderLayout());
        ircChatPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4), BorderFactory.createEtchedBorder()), BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        JPanel ircChannelStatusPanel = new JPanel(new BorderLayout());
        ircChannelStatus = new JLabel("Channel:");
        ircChannelStatusPanel.add(BorderLayout.CENTER, ircChannelStatus);
        ircAutoScrollChatText = new JCheckBox(Res.getStr("Main.ChatTab.ScrollChatLogCheckBox.Title"), true);
        ircPingChat = new JCheckBox(Res.getStr("Main.ChatTab.PingChatCheckBox.Title"), true);
        ircPingChat.setToolTipText(Res.getStr("Main.ChatTab.PingChatCheckBox.ToolTip"));
        chatInfoPanel.add(BorderLayout.EAST, ircPingChat);
        ircPingChat.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (ircPingChat.isSelected()) {
                    ServiceManager.getCfg().mChatPing = true;
                } else {
                    ServiceManager.getCfg().mChatPing = false;
                }
            }
        });
        ircChannelStatusPanel.add(BorderLayout.EAST, ircAutoScrollChatText);
        JPanel ircChatLogPanel = new JPanel(new BorderLayout());
        JPanel ircChatInnerLogPanel = new JPanel(new BorderLayout());
        ircChatLogText = new JTextPane();
        ircChatLogText.setEditable(false);
        ircChatLogText.setBorder(BorderFactory.createEtchedBorder());
        ircChatLogDoc = (StyledDocument) ircChatLogText.getDocument();
        ircChatLogStyle = ircChatLogDoc.addStyle("ChatPreferencesStyle", null);
        JPanel ircChatPreferencesPanel = new IrcChatPreferencesPanel((BaseFrame) mainFrame, ircChatLogText, ircChatLogStyle);
        ircChatLogTextScrollPane = new JScrollPane(ircChatLogText);
        ircChatInnerLogPanel.add(BorderLayout.CENTER, ircChatLogTextScrollPane);
        ircChatInnerLogPanel.add(BorderLayout.SOUTH, ircChatPreferencesPanel);
        JPanel ircUserListPanel = new JPanel(new BorderLayout());
        ircUsersModel = new IrcUserListModel();
        ircUsersList = new JList(ircUsersModel);
        ircUsersList.addMouseListener(new MouseHandler());
        ircUserListPanel.setPreferredSize(new Dimension(130, 300));
        ircUserListPanel.add(BorderLayout.CENTER, new JScrollPane(ircUsersList));
        ircUserListPanel.setBorder(BorderFactory.createEtchedBorder());
        JPanel instantMsgPanel = new JPanel(new FlowLayout());
        instantMsgPanel.add(instantMsgButton);
        ircUserListPanel.add(instantMsgPanel, BorderLayout.SOUTH);
        ircChatLogPanel.add(BorderLayout.NORTH, ircChannelStatusPanel);
        ircChatLogPanel.add(BorderLayout.CENTER, ircChatInnerLogPanel);
        ircChatLogPanel.add(BorderLayout.EAST, ircUserListPanel);
        JPanel ircSendChatPanel = new JPanel(new BorderLayout());
        ircSendChatPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0), BorderFactory.createEtchedBorder()), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        ircSendLabel = new JLabel(Res.getStr("Main.ChatTab.MessageLabel.Title") + "  ", JLabel.RIGHT);
        ircSendText = new JTextField();
        ircSendText.addKeyListener(new IrcSendChatKeyHandler());
        ircSendButton = new JButton(Res.getStr("Main.ChatTab.SendButton.Title"));
        ircSendButton.addActionListener(new IrcChatHandler());
        ircSendButton.setIcon(Res.getIcon("SendIrc.Icon"));
        ircSendChatPanel.add(BorderLayout.WEST, ircSendLabel);
        ircSendChatPanel.add(BorderLayout.CENTER, ircSendText);
        ircSendChatPanel.add(BorderLayout.EAST, ircSendButton);
        ircChatPanel.add(BorderLayout.CENTER, ircChatLogPanel);
        ircChatPanel.add(BorderLayout.SOUTH, ircSendChatPanel);
        ircChannelPanel.setPreferredSize(new Dimension(780, 155));
        ircChatPanel.setPreferredSize(new Dimension(780, 275));
        this.setBorder(BorderFactory.createEmptyBorder());
        this.setDividerSize(6);
        this.setTopComponent(ircChannelPanel);
        this.setBottomComponent(ircChatPanel);
        this.setDividerLocation(150);
    }

    public void invalidIrcNickname() {
    }

    public void refreshIrcChatPane() {
        Vector list = ircMgr.getChannels();
        if (list.size() <= ircCurrentChannel) return;
        IrcChannel channel;
        try {
            channel = (IrcChannel) list.elementAt(ircCurrentChannel);
        } catch (Exception e) {
            return;
        }
        String logText = channel.getChatLogText(false);
        try {
            ircChatLogText.setDocument(new DefaultStyledDocument());
            ircChatLogDoc = (StyledDocument) ircChatLogText.getDocument();
            ircChatLogDoc.insertString(0, logText, ircChatLogStyle);
        } catch (BadLocationException ex) {
            ;
        }
    }

    public void setIrcFocus() {
        try {
            if (mainFrame.isChatTabSelected()) {
                ircSendText.requestFocus();
                ircSendButton.requestDefaultFocus();
            }
        } catch (Exception e) {
        }
    }

    public void setNickname(String _nickname, String _key) {
        if ((_nickname == null) || (_nickname.length() == 0)) {
            ServiceManager.getCfg().mIrcNickname = "";
            ServiceManager.getCfg().save();
            ircMgr.disconnect();
        } else {
            try {
                ServiceManager.getCfg().mIrcNickname = _nickname;
                if ((_key != null) && (_key.length() > 0)) ServiceManager.getCfg().mIrcKey = _key;
                ServiceManager.getCfg().save();
                ircMgr.disconnect();
                try {
                    synchronized (this) {
                        wait(5000);
                    }
                } catch (Exception e) {
                }
                ircMgr.connect();
            } catch (Exception e) {
            }
        }
    }

    IrcChannel getSelectedChannel() {
        int row = ircChannelTable.getSelectedRow();
        if (row < 0 || row >= ircChannelTable.getRowCount()) return null;
        row = ircChannelSorter.indexes[row];
        Vector list = ircMgr.getChannels();
        return (IrcChannel) list.elementAt(row);
    }

    void invokeLater(String methodName) {
        SwingUtilities.invokeLater(new Invoker(this, methodName));
    }

    void ircInitiatePrivateChat() {
        String nick = ircUsersModel.getIrcNickAt(ircUsersList.getSelectedIndex());
        if (nick == null) return;
        if (nick.startsWith("@") || nick.startsWith("+")) {
            nick = nick.substring(1);
        }
        ircMgr.createPrivateChat(ServiceManager.getCfg().mIrcNickname, nick);
    }

    void ircSendChat() {
        String text = ircSendText.getText().trim();
        if (text.length() == 0) return;
        int row = ircChannelTable.getSelectedRow();
        if (row < 0 || row >= ircChannelTable.getRowCount()) return;
        Vector list = ircMgr.getChannels();
        IrcChannel channel = (IrcChannel) list.elementAt(row);
        channel.send(text);
        ircSendText.setText("");
    }

    void ircWhois() {
        String nick = (String) ircUsersList.getSelectedValue();
        if (nick == null) return;
        if (nick.startsWith("@") || nick.startsWith("+")) {
            nick = nick.substring(1);
        }
        ircMgr.sendWhois(nick);
    }

    void joinFurthurChannel() {
        try {
            Vector list = ircMgr.getChannels();
            for (int i = 0; i < list.size(); i++) {
                if (((IrcChannel) list.elementAt(i)).getName().equals("#furthur")) {
                    ((IrcChannel) list.elementAt(i)).join();
                    ircChannelModel.fireTableDataChanged();
                    ircChannelTable.clearSelection();
                    int visibleRow = 0;
                    for (int j = 0; j < ircChannelSorter.indexes.length; j++) {
                        if (ircChannelSorter.indexes[j] == 0) {
                            visibleRow = j;
                            break;
                        }
                    }
                    ircChannelTable.addRowSelectionInterval(visibleRow, visibleRow);
                }
            }
        } catch (Exception e) {
        }
    }

    void joinIrcChannel() {
        IrcChannel channel = getSelectedChannel();
        int rowSelected = ircChannelTable.getSelectedRow();
        if (channel == null) {
            JOptionPane.showMessageDialog(this, "Select the channel that you would like to join from the list below.", "Select Channel", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!ServiceManager.getIrcManager().getIsConnected()) {
            JOptionPane.showMessageDialog(this, "You must connect to the chat server before joining a channel.", "Not Connected", JOptionPane.ERROR_MESSAGE);
            return;
        }
        channel.join();
        ircChannelModel.fireTableDataChanged();
        ircChannelTable.setRowSelectionInterval(rowSelected, rowSelected);
    }

    void leaveIrcChannel() {
        IrcChannel channel = getSelectedChannel();
        if (channel == null) {
            JOptionPane.showMessageDialog(this, "Select the channel that you would like to leave from the list below.", "Select Channel", JOptionPane.ERROR_MESSAGE);
            return;
        }
        channel.part();
        ircChannelModel.fireTableDataChanged();
    }

    void nextChannel() {
        int row = ircChannelTable.getSelectedRow();
        if (row < 0) {
            row = -1;
        }
        row++;
        if (row >= ircChannelTable.getRowCount()) {
            row = 0;
        }
        if (row < ircChannelTable.getRowCount()) {
            ircChannelTable.setRowSelectionInterval(row, row);
            ircChannelTable.scrollRectToVisible(ircChannelTable.getCellRect(row, 0, true));
        }
    }

    void prevChannel() {
        int row = ircChannelTable.getSelectedRow();
        if (row < 0) {
            row = ircChannelTable.getRowCount();
        }
        row--;
        if (row < 0) {
            row = ircChannelTable.getRowCount() - 1;
        }
        if (row >= 0) {
            ircChannelTable.setRowSelectionInterval(row, row);
            ircChannelTable.scrollRectToVisible(ircChannelTable.getCellRect(row, 0, true));
        }
    }

    void refreshHandler() {
        if ((ircMgr.getRemoteHost() != null) && (!mAuthenticated)) {
            String text = "Furthur is attempting to connect to the chat server.\nThis could take up to a minute...";
            ircConnectDelay++;
            for (int i = 0; i < ircConnectDelay; i++) text += "...";
            try {
                ircChatLogText.setDocument(new DefaultStyledDocument());
                ircChatLogDoc = (StyledDocument) ircChatLogText.getDocument();
                ircChatLogDoc.insertString(0, text, ircChatLogStyle);
            } catch (BadLocationException ex) {
                ;
            }
        } else ircConnectDelay = 0;
    }

    void removeIrcChannel() {
        int row = ircChannelTable.getSelectedRow();
        if (row < 0 || row >= ircChannelTable.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Select the channel that you would like to remove from the list below.", "Select Channel", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Vector list = ircMgr.getChannels();
        IrcChannel channel = (IrcChannel) list.elementAt(row);
        channel.part();
        ircMgr.removeIrcChannel(channel);
        ircMgr.serializeChannels();
        ServiceManager.getCfg().save();
    }

    void updateIrcInfoLabel() {
        StringBuffer buf = new StringBuffer();
        boolean connected = false;
        Host rhost = ircMgr.getRemoteHost();
        if (rhost == null) {
            mAuthenticated = false;
            buf.append("Not connected to chat server yet");
        } else if (mAuthenticated) {
            buf.append("Connected to Furthur chat server ");
            buf.append("as ");
            buf.append(ServiceManager.getCfg().mIrcNickname);
            connected = true;
            joinFurthurChannel();
        } else if (rhost.getStatus() == Host.sStatusConnected) {
            buf.append("Connecting... ");
        } else {
            buf.append("Trying server...");
        }
        ircUsersList.setEnabled(connected);
        ircChannelStatus.setEnabled(connected);
        ircAutoScrollChatText.setEnabled(connected);
        ircPingChat.setEnabled(connected);
        ircPingChat.setSelected(ServiceManager.getCfg().mChatPing);
        ircChatLogText.setEnabled(connected);
        ircSendLabel.setEnabled(connected);
        ircSendText.setEnabled(connected);
        ircSendButton.setEnabled(connected);
        ircInfoLabel.setText(buf.toString());
    }

    private void processEnterChatMessage() {
        String text = ircSendText.getText().trim();
        if (text.length() == 0) return;
        int row = ircChannelTable.getSelectedRow();
        if (row < 0 || row >= ircChannelTable.getRowCount()) return;
        Vector list = ircMgr.getChannels();
        IrcChannel channel = (IrcChannel) list.elementAt(row);
        channel.send(text);
        ircSendText.setText("");
        int messageHistorySize = messageHistory.size();
        while (messageHistorySize > ServiceManager.getCfg().mMaxMessageHistorySize) {
            messageHistory.remove(0);
            messageHistorySize = messageHistory.size();
        }
        if (messageHistorySize != 0) {
            messageHistory.add((messageHistory.size() - 1), text);
            messageHistory.set((messageHistory.size() - 1), "");
        } else {
            messageHistory.add(0, text);
            messageHistory.add(1, "");
        }
        messagePointer = messageHistory.size() - 1;
    }

    private void setupPopupMenus() {
        ircPopupMenu = new JPopupMenu();
        mainFrame.populatePopupMenu(ircPopupMenu, "IrcChannelTable.PopupMenu");
        ircUserPopupMenu = new JPopupMenu();
        mainFrame.populatePopupMenu(ircUserPopupMenu, "IrcUserList.PopupMenu");
    }

    class MouseHandler extends MouseAdapter implements MouseListener {

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                if (e.getSource() == ircUsersList) {
                    ircInitiatePrivateChat();
                }
            }
        }

        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popupMenu((Component) e.getSource(), e.getX(), e.getY());
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popupMenu((Component) e.getSource(), e.getX(), e.getY());
            }
        }

        private void popupMenu(Component source, int x, int y) {
            if (source == ircChannelTable) {
                ircPopupMenu.show(source, x, y);
            } else if (source == ircUsersList) {
                ircUserPopupMenu.show(source, x, y);
            }
        }
    }

    private class IrcChannelChangedListener implements IDataChangedListener {

        public void dataChanged(Object source) {
            ircChannelChanged = true;
            mainFrame.fireTableChanged(ircChannelTable, ircChannelModel);
            ircChannelChanged = false;
        }
    }

    private class IrcChannelSelectionHandler implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            if (ircChannelChanged) return;
            int row = ircChannelTable.getSelectedRow();
            if (row < 0 || row >= ircChannelTable.getRowCount()) return;
            row = ircChannelSorter.indexes[row];
            Vector list = ircMgr.getChannels();
            if (row >= list.size()) {
                return;
            }
            try {
                ((IrcChannel) list.elementAt(ircCurrentChannel)).setIsCurrent(false);
            } catch (Exception e2) {
            }
            IrcChannel channel = (IrcChannel) list.elementAt(row);
            channel.setIsCurrent(true);
            ircCurrentChannel = row;
            String logText = channel.getChatLogText(false);
            try {
                ircChatLogText.setDocument(new DefaultStyledDocument());
                ircChatLogDoc = (StyledDocument) ircChatLogText.getDocument();
                ircChatLogDoc.insertString(0, logText, ircChatLogStyle);
            } catch (BadLocationException ex) {
                ;
            }
            if (ircAutoScrollChatText.isSelected() && logText.length() > 0) {
                ircChatLogText.setCaretPosition(ircChatLogDoc.getLength());
            }
            ircChannelStatus.setText(channel.getName() + ": " + channel.getTopic());
            ((IrcUserListModel) ircUsersModel).fireChanged(channel);
            mainFrame.refreshAllActions();
        }
    }

    private class IrcChannelTableModel extends AbstractTableModel {

        public Class getColumnClass(int col) {
            return String.class;
        }

        public int getColumnCount() {
            return 3;
        }

        public String getColumnName(int col) {
            switch(col) {
                case 0:
                    return "Channel";
                case 1:
                    return "Joined";
                case 2:
                    return "Users";
                case 3:
                    return "Messages";
            }
            return "";
        }

        public int getRowCount() {
            Vector list = ircMgr.getChannels();
            return (list.size());
        }

        public Object getValueAt(int row, int col) {
            Vector list = ircMgr.getChannels();
            if (row >= list.size()) {
                return "";
            }
            IrcChannel channel = (IrcChannel) list.elementAt(row);
            switch(col) {
                case 0:
                    {
                        return channel.getDescription();
                    }
                case 1:
                    {
                        return (channel.getJoined() ? "yes" : "no");
                    }
                case 2:
                    {
                        return (channel.getJoined() ? "" + channel.getUserCount() : "");
                    }
                case 3:
                    {
                        return (channel.getJoined() ? "" + channel.getMsgCountStr() : "");
                    }
            }
            return "";
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public void setValueAt(Object value, int row, int col) {
        }
    }

    private class IrcChatHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (!mainFrame.isChatTabSelected()) return;
            processEnterChatMessage();
            ircSendText.requestFocus();
        }
    }

    private class IrcMsgChangedListener implements IDataChangedListener {

        public void dataChanged(Object source) {
            int row = ircChannelTable.getSelectedRow();
            if (row < 0 || row >= ircChannelTable.getRowCount()) return;
            row = ircChannelSorter.indexes[row];
            Vector list = ircMgr.getChannels();
            if (row >= list.size()) {
                return;
            }
            IrcChannel channel = (IrcChannel) list.elementAt(row);
            if (channel != (IrcChannel) source) {
                return;
            }
            String logText = channel.getLastMsgText();
            if (logText.startsWith(ServiceManager.getCfg().mIrcTopicString)) {
                channel.setTopic(logText.substring(ServiceManager.getCfg().mIrcTopicString.length()).trim());
                ircChannelStatus.setText(channel.getName() + ": " + channel.getTopic());
            }
            if (!logText.startsWith(ServiceManager.getCfg().mIrcFilterString)) {
                try {
                    ircChatLogDoc.insertString(ircChatLogDoc.getLength(), logText, ircChatLogStyle);
                } catch (BadLocationException ex) {
                    ;
                }
                if (ircAutoScrollChatText.isSelected() && logText.length() > 0) ircChatLogText.setCaretPosition(ircChatLogDoc.getLength());
            }
        }
    }

    private class IrcSendChatKeyHandler implements KeyListener {

        public void keyPressed(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.VK_UP) {
                if (messagePointer > 0) {
                    --messagePointer;
                    ircSendText.setText((String) messageHistory.get(messagePointer));
                } else {
                    if (messageHistory.size() != 0) {
                        ircSendText.setText((String) messageHistory.get(messagePointer));
                    }
                }
            } else if (event.getKeyCode() == KeyEvent.VK_DOWN) {
                if (messagePointer == 0) {
                    if (messageHistory.size() != 0) {
                        ++messagePointer;
                        ircSendText.setText((String) messageHistory.get(messagePointer));
                    }
                } else if (messagePointer < messageHistory.size()) {
                    if (messagePointer == (messageHistory.size() - 1)) {
                        ircSendText.setText("");
                    } else {
                        ++messagePointer;
                        ircSendText.setText((String) messageHistory.get(messagePointer));
                    }
                } else {
                    ircSendText.setText("");
                    --messagePointer;
                }
            }
        }

        public void keyReleased(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                processEnterChatMessage();
            }
        }

        public void keyTyped(KeyEvent event) {
        }
    }

    private class IrcServerChangedListener implements IDataChangedListener {

        public void dataChanged(Object source) {
            invokeLater("updateIrcInfoLabel");
        }
    }

    private class IrcUserChangedListener implements IDataChangedListener {

        public void dataChanged(Object source) {
            ((IrcUserListModel) ircUsersModel).fireChanged(source);
        }
    }

    private class IrcUserListModel extends AbstractListModel {

        public Object getElementAt(int index) {
            int row = ircChannelTable.getSelectedRow();
            if (row < 0 || row >= ircChannelTable.getRowCount()) return "";
            row = ircChannelSorter.indexes[row];
            Vector list = ircMgr.getChannels();
            if (row >= list.size()) {
                return "";
            }
            IrcChannel channel = (IrcChannel) list.elementAt(row);
            try {
                return ((IrcUser) (channel.getUsers().elementAt(index))).getNick();
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }

        public String getIrcNickAt(int index) {
            int row = ircChannelTable.getSelectedRow();
            if (row < 0 || row >= ircChannelTable.getRowCount()) return "";
            row = ircChannelSorter.indexes[row];
            Vector list = ircMgr.getChannels();
            if (row >= list.size()) {
                return "";
            }
            IrcChannel channel = (IrcChannel) list.elementAt(row);
            try {
                return ((IrcUser) (channel.getUsers().elementAt(index))).getIrcNick();
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }

        public int getSize() {
            int row = ircChannelTable.getSelectedRow();
            if (row < 0 || row >= ircChannelTable.getRowCount()) return 0;
            row = ircChannelSorter.indexes[row];
            Vector list = ircMgr.getChannels();
            if (row >= list.size()) {
                return 0;
            }
            IrcChannel channel = (IrcChannel) list.elementAt(row);
            return channel.getUserCount();
        }

        void fireChanged(Object source) {
            int row = ircChannelTable.getSelectedRow();
            if (row < 0 || row >= ircChannelTable.getRowCount()) return;
            row = ircChannelSorter.indexes[row];
            Vector list = ircMgr.getChannels();
            if (row >= list.size()) {
                return;
            }
            IrcChannel channel = (IrcChannel) list.elementAt(row);
            fireContentsChanged(source, 0, channel.getUserCount());
        }
    }
}
