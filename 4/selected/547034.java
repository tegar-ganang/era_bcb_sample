package coopnetclient.frames.clientframetabs;

import coopnetclient.ErrorHandler;
import coopnetclient.Globals;
import coopnetclient.enums.ChatStyles;
import coopnetclient.enums.LaunchMethods;
import coopnetclient.enums.LogTypes;
import coopnetclient.frames.FrameOrganizer;
import coopnetclient.frames.components.ConnectingProgressBar;
import coopnetclient.frames.interfaces.ClosableTab;
import coopnetclient.frames.listeners.ChatInputKeyListener;
import coopnetclient.frames.models.SortedListModel;
import coopnetclient.frames.popupmenus.PlayerListPopupMenu;
import coopnetclient.frames.renderers.RoomPlayerStatusListCellRenderer;
import coopnetclient.protocol.out.Protocol;
import coopnetclient.utils.Logger;
import coopnetclient.utils.RoomData;
import coopnetclient.utils.gamedatabase.GameDatabase;
import coopnetclient.utils.hotkeys.Hotkeys;
import coopnetclient.utils.launcher.Launcher;
import coopnetclient.utils.launcher.launchinfos.DirectPlayLaunchInfo;
import coopnetclient.utils.launcher.launchinfos.DosboxLaunchInfo;
import coopnetclient.utils.launcher.launchinfos.LaunchInfo;
import coopnetclient.utils.launcher.launchinfos.ParameterLaunchInfo;
import coopnetclient.utils.ui.Colorizer;
import coopnetclient.utils.ui.SoundPlayer;
import coopnetclient.utils.ui.UserListFileDropHandler;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DropMode;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class RoomPanel extends javax.swing.JPanel implements ClosableTab {

    public static final String ROOMID_UNSUPPORTED = "ROOMID_UNSUPPORTED";

    private static final String UNREADY = "Unready";

    private static final String READY = "Ready";

    private LaunchInfo launchInfo;

    private RoomData roomData;

    private SortedListModel users;

    private PlayerListPopupMenu popup;

    private HashMap<String, String> gamesettings = new HashMap<String, String>();

    private RoomPlayerStatusListCellRenderer roomStatusListCR;

    private SwingWorker readyDisablerThread;

    private SwingWorker launchDisablerThread;

    private boolean wasReadyBeforeReInit;

    public RoomPanel(RoomData theRoomData) {
        this.roomData = theRoomData;
        this.users = new SortedListModel();
        users.add(Globals.getThisPlayerLoginName());
        initComponents();
        scrl_chatOutput.updateStyle();
        cmb_interface.setToolTipText("<html>Don't use this unless you have connection issues!" + "<br>If you really need to use this, consult with the room host!" + "<br>Both you and the host have to be connected to the same VPN network!" + "<br>Otherwise it won't work!");
        cmb_interface.setVisible(false);
        lbl_interface.setVisible(false);
        if (roomData.isHost()) {
            popup = new PlayerListPopupMenu(true, lst_userList);
            Hotkeys.bindHotKey(Hotkeys.ACTION_LAUNCH);
        } else {
            popup = new PlayerListPopupMenu(false, lst_userList);
        }
        lst_userList.setComponentPopupMenu(popup);
        roomStatusListCR = new RoomPlayerStatusListCellRenderer();
        lst_userList.setCellRenderer(roomStatusListCR);
        lst_userList.setDragEnabled(true);
        lst_userList.setDropMode(DropMode.USE_SELECTION);
        lst_userList.setTransferHandler(new UserListFileDropHandler());
        tp_chatInput.addKeyListener(new ChatInputKeyListener(ChatInputKeyListener.ROOM_CHAT_MODE, roomData.getChannel()));
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
        if (!theRoomData.isHost()) {
            convertToJoinPanel();
        }
        Colorizer.colorize(this);
        chat("", theRoomData.getRoomName(), ChatStyles.USER);
        String channelID = GameDatabase.getShortName(theRoomData.getChannel()).replaceAll(" ", "_");
        chat("", "room://" + channelID + "/" + theRoomData.getRoomID(), ChatStyles.USER);
        prgbar_connecting.setVisible(false);
        decideGameSettingsButtonVisility();
    }

    private void detectVPN() {
        if (cmb_interface.getItemCount() == 1 || roomData.isHost()) {
            cmb_interface.setEnabled(false);
        } else if (cmb_interface.getItemCount() > 1) {
            cmb_interface.setEnabled(true);
        }
    }

    private void decideGameSettingsButtonVisility() {
        if (Launcher.isPlaying()) {
            btn_gameSettings.setEnabled(false);
        }
        if (GameDatabase.getLocalSettingCount(roomData.getChannel(), roomData.getModName()) + GameDatabase.getServerSettingCount(roomData.getChannel(), roomData.getModName()) == 0) {
            btn_gameSettings.setVisible(false);
        }
    }

    public ConnectingProgressBar getConnectingProgressBar() {
        return prgbar_connecting;
    }

    public boolean isHost() {
        return roomData.isHost();
    }

    public RoomData getRoomData() {
        return roomData;
    }

    public void initLauncher() {
        new Thread() {

            @Override
            public void run() {
                try {
                    LaunchMethods method = GameDatabase.getLaunchMethod(roomData.getChannel(), roomData.getModName());
                    if (method == LaunchMethods.PARAMETER) {
                        launchInfo = new ParameterLaunchInfo(roomData);
                    } else if (method == LaunchMethods.DOS) {
                        launchInfo = new DosboxLaunchInfo(roomData);
                    } else {
                        launchInfo = new DirectPlayLaunchInfo(roomData);
                    }
                    Launcher.initialize(launchInfo);
                } catch (Exception e) {
                    ErrorHandler.handle(e);
                }
            }
        }.start();
    }

    public void showSettings() {
        if (btn_gameSettings.isVisible()) {
            FrameOrganizer.openGameSettingsFrame(roomData);
        }
    }

    @Override
    public void requestFocus() {
        tp_chatInput.requestFocusInWindow();
    }

    public void disableGameSettingsFrameButton() {
        btn_gameSettings.setEnabled(false);
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

    public void convertToJoinPanel() {
        btn_launch.setVisible(false);
        cmb_interface.setVisible(true);
        lbl_interface.setVisible(true);
    }

    public void setGameSetting(String key, String value) {
        gamesettings.put(key, value);
    }

    public String getGameSetting(String key) {
        return gamesettings.get(key);
    }

    public void addMember(String playername) {
        users.add(playername);
    }

    public void setAway(String playername) {
        roomStatusListCR.setAway(playername);
    }

    public void unSetAway(String playername) {
        roomStatusListCR.unSetAway(playername);
    }

    public void removeMember(String playername) {
        roomStatusListCR.removePlayer(playername);
        users.removeElement(playername);
        lst_userList.repaint();
    }

    public void chat(String name, String message, ChatStyles modeStyle) {
        scrl_chatOutput.printChatMessage(name, message, modeStyle);
    }

    public boolean updatePlayerName(String oldname, String newname) {
        roomStatusListCR.updateName(oldname, newname);
        if (users.removeElement(oldname)) {
            users.add(newname);
            return true;
        }
        return false;
    }

    public void unReadyPlayer(String playerName) {
        roomStatusListCR.unReadyPlayer(playerName);
    }

    public void readyPlayer(String playerName) {
        roomStatusListCR.readyPlayer(playerName);
    }

    public void setPlaying(String playerName) {
        roomStatusListCR.setPlaying(playerName);
    }

    public void gameClosed(String playerName) {
        roomStatusListCR.gameClosed(playerName);
    }

    public void pressLaunch() {
        btn_launch.doClick();
    }

    public void launch() {
        if (Launcher.isPlaying()) {
            Protocol.launch();
            return;
        }
        if (!Launcher.predictSuccessfulLaunch()) {
            return;
        }
        new Thread() {

            @Override
            public void run() {
                try {
                    Launcher.launch();
                    Protocol.gameClosed(roomData.getChannel());
                    btn_gameSettings.setEnabled(true);
                } catch (Exception e) {
                    ErrorHandler.handle(e);
                }
            }
        }.start();
    }

    public void displayDelayedReinit() {
        btn_ready.setText("Waiting for game to exit...");
    }

    public void displayReInit() {
        SwingUtilities.invokeLater(new Thread() {

            @Override
            public void run() {
                if (btn_ready.getText().equals(UNREADY)) {
                    flipReadyStatus();
                    wasReadyBeforeReInit = true;
                }
                btn_ready.setText("Reinitializing...");
                if (readyDisablerThread != null) {
                    readyDisablerThread.cancel(true);
                }
                btn_ready.setEnabled(false);
                if (launchDisablerThread != null) {
                    launchDisablerThread.cancel(true);
                }
                btn_launch.setEnabled(false);
                cmb_interface.setEnabled(false);
            }
        });
    }

    public void initDone() {
        btn_ready.setText(READY);
        btn_ready.setEnabled(true);
        cmb_interface.setEnabled(true);
        detectVPN();
        if (wasReadyBeforeReInit) {
            flipReadyStatus();
            wasReadyBeforeReInit = false;
        }
    }

    public void initDoneReadyDisabled() {
        btn_ready.setText(READY);
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        btn_ready = new javax.swing.JButton();
        btn_launch = new javax.swing.JButton();
        sp_chatHorizontal = new javax.swing.JSplitPane();
        scrl_userList = new javax.swing.JScrollPane();
        lst_userList = new javax.swing.JList();
        sp_chatVertical = new javax.swing.JSplitPane();
        scrl_chatInput = new javax.swing.JScrollPane();
        tp_chatInput = new javax.swing.JTextPane();
        scrl_chatOutput = new coopnetclient.frames.components.ChatOutput();
        btn_gameSettings = new javax.swing.JButton();
        prgbar_connecting = new coopnetclient.frames.components.ConnectingProgressBar();
        cmb_interface = new javax.swing.JComboBox();
        lbl_interface = new javax.swing.JLabel();
        setFocusable(false);
        setNextFocusableComponent(tp_chatInput);
        setRequestFocusEnabled(false);
        setLayout(new java.awt.GridBagLayout());
        btn_ready.setMnemonic(KeyEvent.VK_R);
        btn_ready.setText("Initializing...");
        btn_ready.setEnabled(false);
        btn_ready.setFocusable(false);
        btn_ready.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clickedbtn_ready(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
        add(btn_ready, gridBagConstraints);
        btn_launch.setMnemonic(KeyEvent.VK_L);
        btn_launch.setText("Launch");
        btn_launch.setEnabled(false);
        btn_launch.setFocusable(false);
        btn_launch.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clickedbtn_launch(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
        add(btn_launch, gridBagConstraints);
        sp_chatHorizontal.setBorder(null);
        sp_chatHorizontal.setDividerSize(3);
        sp_chatHorizontal.setResizeWeight(1.0);
        sp_chatHorizontal.setFocusable(false);
        scrl_userList.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrl_userList.setFocusable(false);
        scrl_userList.setMinimumSize(new java.awt.Dimension(100, 50));
        scrl_userList.setPreferredSize(new java.awt.Dimension(150, 200));
        lst_userList.setModel(users);
        lst_userList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lst_userList.setAutoscrolls(false);
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
        sp_chatHorizontal.setRightComponent(scrl_userList);
        sp_chatVertical.setBorder(null);
        sp_chatVertical.setDividerSize(3);
        sp_chatVertical.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        sp_chatVertical.setResizeWeight(1.0);
        sp_chatVertical.setFocusable(false);
        sp_chatVertical.setMinimumSize(new java.awt.Dimension(22, 49));
        scrl_chatInput.setFocusable(false);
        tp_chatInput.setMinimumSize(new java.awt.Dimension(6, 24));
        tp_chatInput.setNextFocusableComponent(tp_chatInput);
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
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(sp_chatHorizontal, gridBagConstraints);
        btn_gameSettings.setMnemonic(KeyEvent.VK_G);
        btn_gameSettings.setText("Game Settings");
        btn_gameSettings.setFocusable(false);
        btn_gameSettings.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_gameSettingsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
        add(btn_gameSettings, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        add(prgbar_connecting, gridBagConstraints);
        cmb_interface.setModel(new DefaultComboBoxModel(Globals.getMatchingInterfaceIPMap(roomData.getInterfaceIPs()).keySet().toArray()));
        cmb_interface.setSelectedItem(Globals.INTERNET_INTERFACE_NAME);
        cmb_interface.setEnabled(false);
        cmb_interface.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmb_interfaceActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
        add(cmb_interface, gridBagConstraints);
        lbl_interface.setText("Connect on:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
        add(lbl_interface, gridBagConstraints);
    }

    private void clickedbtn_launch(java.awt.event.ActionEvent evt) {
        if (roomData.isHost()) {
            btn_launch.setEnabled(false);
            launchDisablerThread = new SwingWorker() {

                @Override
                protected Object doInBackground() throws Exception {
                    Thread.sleep(1000);
                    return null;
                }

                @Override
                protected void done() {
                    if (!isCancelled() && btn_ready.isEnabled()) {
                        btn_launch.setEnabled(true);
                    }
                }
            };
            launchDisablerThread.execute();
            launch();
        }
    }

    private void clickedbtn_ready(java.awt.event.ActionEvent evt) {
        btn_ready.setEnabled(false);
        cmb_interface.setEnabled(false);
        readyDisablerThread = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                Thread.sleep(1000);
                return null;
            }

            @Override
            protected void done() {
                if (!isCancelled()) {
                    btn_ready.setEnabled(true);
                    cmb_interface.setEnabled(true);
                }
            }
        };
        readyDisablerThread.execute();
        if (btn_ready.getText().equals(READY) && !Launcher.predictSuccessfulLaunch()) {
            wasReadyBeforeReInit = true;
            return;
        }
        flipReadyStatus();
    }

    private void flipReadyStatus() {
        Protocol.flipReadystatus();
        if (btn_ready.getText().equals(READY)) {
            btn_ready.setText(UNREADY);
            btn_launch.setEnabled(true);
            SoundPlayer.playReadySound();
        } else {
            btn_ready.setText(READY);
            if (launchDisablerThread != null) {
                launchDisablerThread.cancel(true);
            }
            btn_launch.setEnabled(false);
            SoundPlayer.playUnreadySound();
        }
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
            String name = (String) lst_userList.getSelectedValue();
            if (name != null && !name.equals("") && !name.equals(Globals.getThisPlayerLoginName())) {
                TabOrganizer.openPrivateChatPanel(name, true);
            }
        }
    }

    private void btn_gameSettingsActionPerformed(java.awt.event.ActionEvent evt) {
        SwingUtilities.invokeLater(new Thread() {

            @Override
            public void run() {
                try {
                    showSettings();
                } catch (Exception e) {
                    ErrorHandler.handle(e);
                }
            }
        });
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

    private void tp_chatInputFocusLost(java.awt.event.FocusEvent evt) {
        for (KeyListener listener : tp_chatInput.getKeyListeners()) {
            if (listener instanceof ChatInputKeyListener) {
                ((ChatInputKeyListener) listener).resetCTRLStatus();
            }
        }
    }

    private void cmb_interfaceActionPerformed(java.awt.event.ActionEvent evt) {
        displayReInit();
        String newInterface = cmb_interface.getSelectedItem().toString();
        Logger.log(LogTypes.LOG, "Changing interface to: " + newInterface);
        cmb_interface.setEnabled(false);
        roomData.setInterfaceKey(newInterface);
        initLauncher();
        cmb_interface.setEnabled(false);
    }

    private javax.swing.JButton btn_gameSettings;

    private javax.swing.JButton btn_launch;

    private javax.swing.JButton btn_ready;

    private javax.swing.JComboBox cmb_interface;

    private javax.swing.JLabel lbl_interface;

    private javax.swing.JList lst_userList;

    private coopnetclient.frames.components.ConnectingProgressBar prgbar_connecting;

    private javax.swing.JScrollPane scrl_chatInput;

    private coopnetclient.frames.components.ChatOutput scrl_chatOutput;

    private javax.swing.JScrollPane scrl_userList;

    private javax.swing.JSplitPane sp_chatHorizontal;

    private javax.swing.JSplitPane sp_chatVertical;

    private javax.swing.JTextPane tp_chatInput;

    @Override
    public void closeTab() {
        if (roomData.isHost()) {
            Protocol.closeRoom();
        } else {
            Protocol.leaveRoom();
        }
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
