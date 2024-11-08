package coopnetclient.frames.clientframetabs;

import coopnetclient.Globals;
import coopnetclient.frames.listeners.ChatInputKeyListener;
import coopnetclient.frames.renderers.ChannelRoomStatusRenderer;
import coopnetclient.frames.renderers.UsersInRoomTableCellRenderer;
import coopnetclient.frames.models.RoomTableModel;
import coopnetclient.protocol.out.Protocol;
import coopnetclient.enums.ChatStyles;
import coopnetclient.frames.FrameOrganizer;
import coopnetclient.frames.interfaces.ClosableTab;
import coopnetclient.frames.renderers.ChannelStatusListCellRenderer;
import coopnetclient.utils.gamedatabase.GameDatabase;
import coopnetclient.frames.models.ChannelStatusListModel;
import coopnetclient.frames.popupmenus.PlayerListPopupMenu;
import coopnetclient.frames.renderers.RoomNameRenderer;
import coopnetclient.threads.ErrSwingWorker;
import coopnetclient.utils.Logger;
import coopnetclient.utils.settings.Settings;
import coopnetclient.utils.ui.UserListFileDropHandler;
import coopnetclient.utils.launcher.Launcher;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import javax.swing.DropMode;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

public class ChannelPanel extends JPanel implements ClosableTab {

    public String id;

    private ChannelStatusListModel users;

    private RoomTableModel rooms;

    private PlayerListPopupMenu popup;

    public String name;

    public boolean isLaunchable;

    private ChannelStatusListCellRenderer renderer;

    /** Creates new form ChannelPanel */
    public ChannelPanel(String name) {
        this.name = name;
        id = GameDatabase.getIDofGame(name);
        users = new ChannelStatusListModel();
        renderer = new ChannelStatusListCellRenderer(users);
        initComponents();
        coopnetclient.utils.ui.Colorizer.colorize(this);
        scrl_chatOutput.updateStyle();
        rooms = new RoomTableModel(tbl_roomList, users);
        tbl_roomList.setModel(rooms);
        tbl_roomList.setAutoCreateRowSorter(true);
        tbl_roomList.setRowHeight(35);
        tbl_roomList.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        tbl_roomList.getColumnModel().getColumn(0).setMinWidth(45);
        tbl_roomList.getColumnModel().getColumn(0).setMaxWidth(46);
        tbl_roomList.getColumnModel().getColumn(0).setPreferredWidth(45);
        tbl_roomList.getColumnModel().getColumn(1).setPreferredWidth(800);
        tbl_roomList.getColumnModel().getColumn(2).setPreferredWidth(300);
        tbl_roomList.getColumnModel().getColumn(3).setMinWidth(65);
        tbl_roomList.getColumnModel().getColumn(3).setMaxWidth(66);
        tbl_roomList.getColumnModel().getColumn(3).setPreferredWidth(66);
        ChannelRoomStatusRenderer picrend = new ChannelRoomStatusRenderer();
        picrend.setHorizontalAlignment(SwingConstants.CENTER);
        tbl_roomList.setDefaultRenderer(RoomTableModel.RoomType.class, picrend);
        DefaultTableCellRenderer rend = new DefaultTableCellRenderer();
        rend.setHorizontalAlignment(SwingConstants.CENTER);
        rend.putClientProperty("html.disable", Boolean.TRUE);
        tbl_roomList.setDefaultRenderer(String.class, rend);
        UsersInRoomTableCellRenderer userrend = new UsersInRoomTableCellRenderer(rooms);
        userrend.setHorizontalAlignment(SwingConstants.CENTER);
        tbl_roomList.setDefaultRenderer(RoomTableModel.PlayersInRoom.class, userrend);
        RoomNameRenderer roomnamerenderer = new RoomNameRenderer(rooms);
        tbl_roomList.setDefaultRenderer(RoomTableModel.RoomName.class, roomnamerenderer);
        tp_chatInput.addKeyListener(new ChatInputKeyListener(ChatInputKeyListener.CHANNEL_CHAT_MODE, this.name));
        popup = new PlayerListPopupMenu(false, lst_userList);
        lst_userList.setComponentPopupMenu(popup);
        lst_userList.setDragEnabled(true);
        lst_userList.setDropMode(DropMode.USE_SELECTION);
        lst_userList.setTransferHandler(new UserListFileDropHandler());
        scrl_chatOutput.getTextPane().addKeyListener(new java.awt.event.KeyAdapter() {

            @Override
            public void keyTyped(java.awt.event.KeyEvent evt) {
                char c = evt.getKeyChar();
                if (!evt.isControlDown()) {
                    tp_chatInput.setText(tp_chatInput.getText() + c);
                    tp_chatInput.requestFocusInWindow();
                    scrl_chatOutput.getTextPane().setSelectionStart(scrl_chatOutput.getTextPane().getDocument().getLength());
                    scrl_chatOutput.getTextPane().setSelectionEnd(scrl_chatOutput.getTextPane().getDocument().getLength());
                }
            }
        });
        disableButtons();
        enableButtons();
        String message = GameDatabase.getWelcomeMessage(name);
        if (message != null && message.length() > 0) {
            printMainChatMessage(name, message, ChatStyles.DEFAULT);
        }
    }

    public void setAway(String playername) {
        users.setAway(playername);
    }

    public void unSetAway(String playername) {
        users.unSetAway(playername);
    }

    public void gameClosed(String playername) {
        users.playerClosedGame(playername);
        rooms.setLaunchedStatus(playername, false);
    }

    public void hideRoomList() {
        pnl_roomActions.setVisible(false);
        sp_vertical.setDividerSize(0);
    }

    public void setPlayingStatus(String player) {
        users.playerLaunchedGame(player);
        rooms.setLaunchedStatus(player, true);
    }

    public void updateSleepMode() {
        scrl_chatOutput.getTextPane().setEnabled(!Globals.getSleepModeStatus());
        if (Globals.getSleepModeStatus()) {
            scrl_chatOutput.getTextPane().setToolTipText("<html>Sleep mode: Channel chat is inactive!<br>" + "Press refresh button or write a chat message to exit sleep mode.");
        } else {
            scrl_chatOutput.getTextPane().setToolTipText(null);
        }
    }

    @Override
    public void requestFocus() {
        tp_chatInput.requestFocusInWindow();
    }

    public void customCodeForColoring() {
        if (coopnetclient.utils.settings.Settings.getColorizeText()) {
            tp_chatInput.setForeground(coopnetclient.utils.settings.Settings.getUserMessageColor());
        }
        if (tp_chatInput.getText().length() > 0) {
            tp_chatInput.setText(tp_chatInput.getText());
        } else {
            tp_chatInput.setText("\n");
            tp_chatInput.setText("");
        }
        if (coopnetclient.utils.settings.Settings.getColorizeBody()) {
            scrl_chatOutput.getTextPane().setBackground(coopnetclient.utils.settings.Settings.getBackgroundColor());
        }
    }

    public void setLaunchable(boolean value) {
        this.isLaunchable = value;
        enableButtons();
    }

    public void addPlayerToRoom(String hostname, String playername) {
        rooms.addPlayerToRoom(hostname, playername);
        users.playerEnteredRoom(playername);
    }

    public void addRoomToTable(String roomname, String modName, String hostname, int maxplayers, int type) {
        rooms.addRoomToTable(roomname, modName, hostname, maxplayers, type);
        users.playerEnteredRoom(hostname);
    }

    public void addPlayerToChannel(String name) {
        users.playerEnteredChannel(name);
    }

    public void disableButtons() {
        btn_create.setEnabled(false);
        btn_join.setEnabled(false);
    }

    public void enableButtons() {
        if (this.isLaunchable) {
            if (Launcher.isPlaying()) {
                if (TabOrganizer.getRoomPanel() == null && Launcher.getLaunchedGame().equals(name) && !Launcher.isPlayingInstantLaunch()) {
                    btn_create.setEnabled(true);
                    if (tbl_roomList.getSelectedRow() != -1) {
                        btn_join.setEnabled(true);
                    } else {
                        btn_join.setEnabled(false);
                    }
                }
            } else {
                if (TabOrganizer.getRoomPanel() == null) {
                    btn_create.setEnabled(true);
                    if (tbl_roomList.getSelectedRow() != -1) {
                        btn_join.setEnabled(true);
                    } else {
                        btn_join.setEnabled(false);
                    }
                }
            }
        }
    }

    public int getSelectedRoomListRowIndex() {
        return tbl_roomList.getSelectedRow();
    }

    public void printMainChatMessage(String name, String message, ChatStyles modeStyle) {
        scrl_chatOutput.printChatMessage(name, message, modeStyle);
    }

    public void removePlayerFromChannel(String playername) {
        users.playerLeftChannel(playername);
    }

    public void removePlayerFromRoom(String hostname, String playername) {
        rooms.removePlayerFromRoom(hostname, playername);
        users.playerLeftRoom(playername);
    }

    public void removeRoomFromTable(String hostname) {
        statusSetOnRoomClose(hostname);
        rooms.removeElement(hostname);
    }

    private void statusSetOnRoomClose(String hostname) {
        int idx = rooms.indexOf(hostname);
        if (idx == -1) {
            return;
        }
        if (idx == tbl_roomList.getSelectedRow()) {
            tbl_roomList.clearSelection();
            enableButtons();
        }
        String userList = rooms.getUserList(idx);
        String[] userArray = userList.split("<br>");
        for (String s : userArray) {
            users.playerLeftRoom(s);
        }
    }

    public boolean updatePlayerName(String oldname, String newname) {
        boolean found = false;
        found = rooms.updateName(oldname, newname) || found;
        found = users.updateName(oldname, newname) || found;
        return found;
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        sp_vertical = new javax.swing.JSplitPane();
        sp_chatHorizontal = new javax.swing.JSplitPane();
        sp_chatVertical = new javax.swing.JSplitPane();
        scrl_chatInput = new javax.swing.JScrollPane();
        tp_chatInput = new javax.swing.JTextPane();
        scrl_chatOutput = new coopnetclient.frames.components.ChatOutput();
        pnl_userList = new javax.swing.JPanel();
        scrl_userList = new javax.swing.JScrollPane();
        lst_userList = new javax.swing.JList();
        pnl_roomActions = new javax.swing.JPanel();
        btn_create = new javax.swing.JButton();
        btn_join = new javax.swing.JButton();
        btn_refresh = new javax.swing.JButton();
        scrl_roomList = new javax.swing.JScrollPane();
        tbl_roomList = new javax.swing.JTable();
        setFocusable(false);
        setPreferredSize(new java.awt.Dimension(350, 400));
        sp_vertical.setDividerSize(10);
        sp_vertical.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        sp_vertical.setResizeWeight(0.5);
        sp_vertical.setFocusable(false);
        sp_vertical.setOneTouchExpandable(true);
        sp_vertical.setPreferredSize(new java.awt.Dimension(350, 400));
        sp_chatHorizontal.setBorder(null);
        sp_chatHorizontal.setDividerSize(3);
        sp_chatHorizontal.setResizeWeight(1.0);
        sp_chatHorizontal.setFocusable(false);
        sp_chatHorizontal.setPreferredSize(new java.awt.Dimension(350, 200));
        sp_chatVertical.setBorder(null);
        sp_chatVertical.setDividerSize(3);
        sp_chatVertical.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        sp_chatVertical.setResizeWeight(1.0);
        sp_chatVertical.setFocusable(false);
        sp_chatVertical.setPreferredSize(new java.awt.Dimension(350, 100));
        scrl_chatInput.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrl_chatInput.setFocusable(false);
        scrl_chatInput.setMinimumSize(new java.awt.Dimension(7, 24));
        tp_chatInput.setFocusCycleRoot(false);
        tp_chatInput.setPreferredSize(new java.awt.Dimension(6, 24));
        tp_chatInput.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                tp_chatInputFocusLost(evt);
            }
        });
        scrl_chatInput.setViewportView(tp_chatInput);
        sp_chatVertical.setRightComponent(scrl_chatInput);
        sp_chatVertical.setLeftComponent(scrl_chatOutput);
        sp_chatHorizontal.setLeftComponent(sp_chatVertical);
        pnl_userList.setFocusable(false);
        pnl_userList.setPreferredSize(new java.awt.Dimension(150, 80));
        pnl_userList.setLayout(new java.awt.GridBagLayout());
        scrl_userList.setFocusable(false);
        scrl_userList.setMinimumSize(new java.awt.Dimension(100, 50));
        lst_userList.setModel(users);
        lst_userList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lst_userList.setAutoscrolls(false);
        lst_userList.setCellRenderer(renderer);
        lst_userList.setFixedCellHeight(20);
        lst_userList.setFocusable(false);
        lst_userList.setMinimumSize(new java.awt.Dimension(30, 50));
        lst_userList.setPreferredSize(null);
        lst_userList.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lst_userListMouseClicked(evt);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                lst_userListMouseExited(evt);
            }
        });
        lst_userList.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {

            public void mouseMoved(java.awt.event.MouseEvent evt) {
                lst_userListMouseMoved(evt);
            }
        });
        scrl_userList.setViewportView(lst_userList);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnl_userList.add(scrl_userList, gridBagConstraints);
        sp_chatHorizontal.setRightComponent(pnl_userList);
        sp_vertical.setBottomComponent(sp_chatHorizontal);
        pnl_roomActions.setFocusable(false);
        pnl_roomActions.setMinimumSize(new java.awt.Dimension(100, 70));
        pnl_roomActions.setPreferredSize(new java.awt.Dimension(350, 200));
        btn_create.setMnemonic(KeyEvent.VK_C);
        btn_create.setText("Create");
        btn_create.setFocusable(false);
        btn_create.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                create(evt);
            }
        });
        btn_join.setMnemonic(KeyEvent.VK_J);
        btn_join.setText("Join");
        btn_join.setFocusable(false);
        btn_join.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                join(evt);
            }
        });
        btn_refresh.setMnemonic(KeyEvent.VK_R);
        btn_refresh.setText("Refresh");
        btn_refresh.setFocusable(false);
        btn_refresh.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refresh(evt);
            }
        });
        scrl_roomList.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrl_roomList.setAutoscrolls(true);
        scrl_roomList.setFocusable(false);
        scrl_roomList.setMaximumSize(null);
        scrl_roomList.setMinimumSize(null);
        scrl_roomList.setPreferredSize(new java.awt.Dimension(100, 50));
        tbl_roomList.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null, null, null }, { null, null, null, null }, { null, null, null, null }, { null, null, null, null }, { null, null, null, null }, { null, null, null, null }, { null, null, null, null }, { null, null, null, null }, { null, null, null, null }, { null, null, null, null } }, new String[] { "Title 1", "Title 2", "Title 3", "Title 4" }));
        tbl_roomList.setFillsViewportHeight(true);
        tbl_roomList.setFocusable(false);
        tbl_roomList.setMaximumSize(null);
        tbl_roomList.setMinimumSize(null);
        tbl_roomList.setPreferredSize(null);
        tbl_roomList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tbl_roomList.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tbl_roomListMouseClicked(evt);
            }

            public void mousePressed(java.awt.event.MouseEvent evt) {
                tbl_roomListMousePressed(evt);
            }
        });
        scrl_roomList.setViewportView(tbl_roomList);
        javax.swing.GroupLayout pnl_roomActionsLayout = new javax.swing.GroupLayout(pnl_roomActions);
        pnl_roomActions.setLayout(pnl_roomActionsLayout);
        pnl_roomActionsLayout.setHorizontalGroup(pnl_roomActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnl_roomActionsLayout.createSequentialGroup().addContainerGap().addComponent(btn_create).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btn_join).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btn_refresh).addContainerGap(139, Short.MAX_VALUE)).addComponent(scrl_roomList, javax.swing.GroupLayout.DEFAULT_SIZE, 348, Short.MAX_VALUE));
        pnl_roomActionsLayout.setVerticalGroup(pnl_roomActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnl_roomActionsLayout.createSequentialGroup().addContainerGap().addGroup(pnl_roomActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btn_create).addComponent(btn_join).addComponent(btn_refresh)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(scrl_roomList, javax.swing.GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE)));
        pnl_roomActionsLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] { btn_create, btn_join, btn_refresh });
        sp_vertical.setLeftComponent(pnl_roomActions);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(sp_vertical, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(sp_vertical, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE));
    }

    private void create(java.awt.event.ActionEvent evt) {
        FrameOrganizer.openCreateRoomFrame(this.name);
    }

    private void join(java.awt.event.ActionEvent evt) {
        try {
            if (rooms.isSelectedRoomPassworded()) {
                FrameOrganizer.openJoinRoomPasswordFrame(this.name, rooms.getSelectedHostName());
                return;
            }
            String tmp = null;
            tmp = rooms.getSelectedHostName();
            if (tmp != null) {
                Protocol.joinRoom(tmp, "");
                disableButtons();
            }
        } catch (Exception e) {
            Logger.log(e);
        }
    }

    private void refresh(java.awt.event.ActionEvent evt) {
        rooms.clear();
        users.refresh();
        Protocol.refreshRoomsAndPlayers(this.name);
        if (Settings.getSleepEnabled() && Globals.getSleepModeStatus()) {
            Globals.setSleepModeStatus(false);
        }
        btn_refresh.setEnabled(false);
        new ErrSwingWorker() {

            @Override
            protected Object handledDoInBackground() throws Exception {
                Thread.sleep(3000);
                return null;
            }

            @Override
            protected void handledDone() {
                btn_refresh.setEnabled(true);
            }
        }.execute();
    }

    private void lst_userListMouseClicked(java.awt.event.MouseEvent evt) {
        if (lst_userList.getModel().getElementAt(lst_userList.locationToIndex(evt.getPoint())).equals(Globals.getThisPlayerLoginName())) {
            lst_userList.clearSelection();
        } else {
            lst_userList.setSelectedIndex(lst_userList.locationToIndex(evt.getPoint()));
        }
        if (evt.getButton() == MouseEvent.BUTTON2) {
            String player = lst_userList.getModel().getElementAt(lst_userList.locationToIndex(evt.getPoint())).toString();
            if (Globals.isHighlighted(player)) {
                Globals.unSetHighlightOn(player);
            } else {
                Globals.setHighlightOn(player);
            }
        } else if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1) {
            String selectedname = (String) lst_userList.getSelectedValue();
            if (selectedname != null && !selectedname.equals("") && !selectedname.equals(Globals.getThisPlayerLoginName())) {
                TabOrganizer.openPrivateChatPanel(selectedname, true);
                TabOrganizer.putFocusOnTab(selectedname);
            }
        }
    }

    private void tbl_roomListMouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.getClickCount() == 2 && btn_join.isEnabled() && evt.getButton() == MouseEvent.BUTTON1) {
            btn_join.doClick();
        }
    }

    private void lst_userListMouseMoved(java.awt.event.MouseEvent evt) {
        if (!popup.isVisible()) {
            int idx = lst_userList.locationToIndex(evt.getPoint());
            Rectangle rec = lst_userList.getCellBounds(idx, idx);
            if (rec == null) {
                return;
            }
            if (!rec.contains(evt.getPoint())) {
                lst_userList.clearSelection();
                return;
            }
            if (idx == lst_userList.getSelectedIndex()) {
                return;
            }
            String selected = lst_userList.getModel().getElementAt(idx).toString();
            if (selected != null && selected.length() > 0) {
                if (!selected.equals(Globals.getThisPlayerLoginName())) {
                    lst_userList.setSelectedIndex(idx);
                } else {
                    lst_userList.clearSelection();
                }
            } else {
                lst_userList.clearSelection();
            }
        }
    }

    private void lst_userListMouseExited(java.awt.event.MouseEvent evt) {
        if (!popup.isVisible()) {
            lst_userList.clearSelection();
        }
    }

    private void tbl_roomListMousePressed(java.awt.event.MouseEvent evt) {
        enableButtons();
    }

    private void tp_chatInputFocusLost(java.awt.event.FocusEvent evt) {
        for (KeyListener listener : tp_chatInput.getKeyListeners()) {
            if (listener instanceof ChatInputKeyListener) {
                ((ChatInputKeyListener) listener).resetCTRLStatus();
            }
        }
    }

    public int getChannelChatHorizontalposition() {
        return sp_chatHorizontal.getDividerLocation();
    }

    public int getChannelChatVerticalposition() {
        return sp_chatVertical.getDividerLocation();
    }

    public int getChannelVerticalposition() {
        return sp_vertical.getDividerLocation();
    }

    public RoomTableModel getTableModel() {
        return rooms;
    }

    private javax.swing.JButton btn_create;

    private javax.swing.JButton btn_join;

    private javax.swing.JButton btn_refresh;

    private javax.swing.JList lst_userList;

    private javax.swing.JPanel pnl_roomActions;

    private javax.swing.JPanel pnl_userList;

    private javax.swing.JScrollPane scrl_chatInput;

    private coopnetclient.frames.components.ChatOutput scrl_chatOutput;

    private javax.swing.JScrollPane scrl_roomList;

    private javax.swing.JScrollPane scrl_userList;

    private javax.swing.JSplitPane sp_chatHorizontal;

    private javax.swing.JSplitPane sp_chatVertical;

    private javax.swing.JSplitPane sp_vertical;

    private javax.swing.JTable tbl_roomList;

    private javax.swing.JTextPane tp_chatInput;

    @Override
    public void closeTab() {
        TabOrganizer.closeChannelPanel(this);
    }

    @Override
    public boolean isCurrentlyClosable() {
        return true;
    }

    public void updateHighlights() {
        scrl_chatOutput.updateHighlights();
        lst_userList.repaint();
    }

    public void updateStyle() {
        scrl_chatOutput.updateStyle();
    }
}
