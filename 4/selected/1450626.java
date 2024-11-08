package coopnetclient.frames.clientframe;

import coopnetclient.Client;
import coopnetclient.ErrorHandler;
import coopnetclient.Globals;
import coopnetclient.frames.clientframe.tabs.ChannelPanel;
import coopnetclient.frames.clientframe.tabs.PrivateChatPanel;
import coopnetclient.frames.clientframe.tabs.RoomPanel;
import coopnetclient.protocol.out.Protocol;
import coopnetclient.enums.ChatStyles;
import coopnetclient.frames.clientframe.quickpanel.QuickPanel;
import coopnetclient.utils.Settings;
import coopnetclient.frames.components.FavMenuItem;
import coopnetclient.frames.clientframe.tabs.LoginPanel;
import coopnetclient.frames.listeners.HyperlinkMouseListener;
import coopnetclient.utils.FileDownloader;
import coopnetclient.utils.gamedatabase.GameDatabase;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;

public class ClientFrame extends javax.swing.JFrame {

    private static final int DIVIDERWIDTH = Settings.getQuickPanelDividerWidth();

    private static boolean quickPanelVisibility = false;

    private static Integer lastdividerposition = null;

    private static boolean quickPanelOrientationIsLeft = true;

    private static boolean quickPanelFlashing = false;

    private static Border openQuickbarBorder = BorderFactory.createLoweredBevelBorder();

    private static Border closedQuickbarBorder = BorderFactory.createRaisedBevelBorder();

    private static QuickPanel pnl_QuickPanel;

    /** Creates new form ClientFrame */
    public ClientFrame() {
        pnl_QuickPanel = new QuickPanel(Globals.getContactList());
        pnl_QuickPanel.setPreferredSize(new Dimension(210, 100));
        initComponents();
        pnl_toggleQuickBarLeft.setPreferredSize(new Dimension(Settings.getQuickPanelToggleBarWidth(), 10));
        pnl_toggleQuickBarRight.setPreferredSize(new Dimension(Settings.getQuickPanelToggleBarWidth(), 10));
        pnl_toggleQuickBarLeft.setMinimumSize(new Dimension(Settings.getQuickPanelToggleBarWidth(), 10));
        pnl_toggleQuickBarRight.setMinimumSize(new Dimension(Settings.getQuickPanelToggleBarWidth(), 10));
        setQuickPanelPosition(Settings.getQuickPanelPostionisLeft());
        slp_mainSplitPanel.setDividerSize(0);
        refreshFavourites();
        refreshInstalledGames();
        updateMenu();
        if (Settings.getTrayIconEnabled()) {
            Globals.addTrayIcon();
        }
    }

    public QuickPanel getQuickPanel() {
        return pnl_QuickPanel;
    }

    public void updateStatus() {
        if (Globals.getLoggedInStatus() == false) {
            setQuickPanelVisibility(false);
            lastdividerposition = null;
        }
        if (!Globals.getConnectionStatus()) {
            mi_connection.setText("Connect");
        } else {
            mi_connection.setText("Disconnect");
        }
        m_user.setEnabled(Globals.getLoggedInStatus());
        m_channels.setEnabled(Globals.getLoggedInStatus());
    }

    public void clientTooOldMode() {
        if (Globals.getConnectionStatus()) {
            Client.disconnect();
        }
        enableUpdate();
        mi_connection.setEnabled(false);
    }

    public void enableUpdate() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                mi_update.setEnabled(true);
                mi_update.repaint();
            }
        });
    }

    public void flashQuickPanelToggler() {
        if (!quickPanelFlashing && !quickPanelVisibility) {
            quickPanelFlashing = true;
            new Thread() {

                @Override
                public void run() {
                    while (quickPanelFlashing) {
                        if (Settings.getColorizeBody()) {
                            pnl_toggleQuickBarLeft.setBackground(Settings.getSelectionColor());
                            pnl_toggleQuickBarRight.setBackground(Settings.getSelectionColor());
                        } else {
                            pnl_toggleQuickBarLeft.setBackground(getHoverEffectColor());
                            pnl_toggleQuickBarRight.setBackground(getHoverEffectColor());
                        }
                        try {
                            sleep(1000);
                        } catch (Exception e) {
                        }
                        if (Settings.getColorizeBody()) {
                            pnl_toggleQuickBarRight.setBackground(Settings.getBackgroundColor());
                            pnl_toggleQuickBarLeft.setBackground(Settings.getBackgroundColor());
                        } else {
                            pnl_toggleQuickBarRight.setBackground((Color) UIManager.get("Panel.background"));
                            pnl_toggleQuickBarLeft.setBackground((Color) UIManager.get("Panel.background"));
                        }
                        try {
                            sleep(1000);
                        } catch (Exception e) {
                        }
                    }
                    if (Settings.getColorizeBody()) {
                        pnl_toggleQuickBarRight.setBackground(Settings.getBackgroundColor());
                        pnl_toggleQuickBarLeft.setBackground(Settings.getBackgroundColor());
                    } else {
                        pnl_toggleQuickBarRight.setBackground((Color) UIManager.get("Panel.background"));
                        pnl_toggleQuickBarLeft.setBackground((Color) UIManager.get("Panel.background"));
                    }
                }
            }.start();
        }
    }

    public void printMainChatMessage(String channel, String name, String message, ChatStyles modeStyle) {
        ChannelPanel cp = TabOrganizer.getChannelPanel(channel);
        if (cp != null) {
            cp.printMainChatMessage(name, message, modeStyle);
            TabOrganizer.markTab(cp);
        }
    }

    public void printPrivateChatMessage(String sender, String message) {
        PrivateChatPanel privatechat = TabOrganizer.getPrivateChatPanel(sender);
        if (privatechat == null) {
            TabOrganizer.openPrivateChatPanel(sender, false);
            privatechat = TabOrganizer.getPrivateChatPanel(sender);
        }
        privatechat.append(sender, message, ChatStyles.WHISPER);
        if (!privatechat.isVisible()) {
            printToVisibleChatbox(sender, message, ChatStyles.WHISPER_NOTIFICATION, false);
        }
    }

    public boolean updatePlayerName(String oldname, String newname) {
        boolean found = false;
        for (int i = 0; TabOrganizer.getChannelPanel(i) != null; i++) {
            found = TabOrganizer.getChannelPanel(i).updatePlayerName(oldname, newname) || found;
        }
        if (TabOrganizer.getRoomPanel() != null) {
            found = TabOrganizer.getRoomPanel().updatePlayerName(oldname, newname) || found;
        }
        TabOrganizer.updateTitleOnTab(oldname, newname);
        found = Globals.getContactList().updateName(oldname, newname) || found;
        return found;
    }

    public void setQuickPanelVisibility(boolean visibility) {
        if (visibility) {
            if (lastdividerposition == null) {
                if (quickPanelOrientationIsLeft) {
                    lastdividerposition = pnl_QuickPanel.getPreferredSize().width;
                } else {
                    lastdividerposition = slp_mainSplitPanel.getWidth() - (pnl_QuickPanel.getPreferredSize().width + DIVIDERWIDTH + slp_mainSplitPanel.getInsets().right);
                }
            }
            slp_mainSplitPanel.setDividerSize(DIVIDERWIDTH);
            slp_mainSplitPanel.setDividerLocation(lastdividerposition);
            pnl_toggleQuickBarLeft.setBorder(openQuickbarBorder);
            pnl_toggleQuickBarRight.setBorder(openQuickbarBorder);
        } else {
            lastdividerposition = slp_mainSplitPanel.getDividerLocation();
            slp_mainSplitPanel.setDividerSize(0);
            pnl_toggleQuickBarLeft.setBorder(closedQuickbarBorder);
            pnl_toggleQuickBarRight.setBorder(closedQuickbarBorder);
            if (quickPanelOrientationIsLeft) {
                slp_mainSplitPanel.setDividerLocation(0);
            } else {
                slp_mainSplitPanel.setDividerLocation(slp_mainSplitPanel.getWidth());
            }
        }
        pnl_QuickPanel.setVisible(visibility);
        quickPanelVisibility = visibility;
        mi_showQuickbar.setSelected(visibility);
    }

    /**
     * positions the quickbar on the left or right accordint to the boolean value
     */
    public void setQuickPanelPosition(boolean left) {
        if (left) {
            slp_mainSplitPanel.setLeftComponent(pnl_QuickPanel);
            slp_mainSplitPanel.setRightComponent(tabpn_tabs);
            slp_mainSplitPanel.setResizeWeight(0.0);
            pnl_QuickPanel.setTabAlignment(true);
            quickPanelOrientationIsLeft = true;
            pnl_toggleQuickBarLeft.setVisible(true);
            pnl_toggleQuickBarRight.setVisible(false);
            slp_mainSplitPanel.setDividerLocation(0);
        } else {
            slp_mainSplitPanel.setLeftComponent(tabpn_tabs);
            slp_mainSplitPanel.setRightComponent(pnl_QuickPanel);
            slp_mainSplitPanel.setResizeWeight(1.0);
            pnl_QuickPanel.setTabAlignment(false);
            quickPanelOrientationIsLeft = false;
            pnl_toggleQuickBarLeft.setVisible(false);
            pnl_toggleQuickBarRight.setVisible(true);
            slp_mainSplitPanel.setDividerLocation(slp_mainSplitPanel.getWidth());
        }
        pnl_QuickPanel.setSize(0, pnl_QuickPanel.getHeight());
        slp_mainSplitPanel.revalidate();
        slp_mainSplitPanel.resetToPreferredSizes();
        this.pack();
        this.repaint();
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        tabpn_tabs = new javax.swing.JTabbedPane();
        slp_mainSplitPanel = new javax.swing.JSplitPane();
        pnl_toggleQuickBarLeft = new javax.swing.JPanel();
        pnl_toggleQuickBarRight = new javax.swing.JPanel();
        mbar = new javax.swing.JMenuBar();
        m_main = new javax.swing.JMenu();
        mi_connection = new javax.swing.JMenuItem();
        mi_update = new javax.swing.JMenuItem();
        mi_quit = new javax.swing.JMenuItem();
        m_user = new javax.swing.JMenu();
        mi_profile = new javax.swing.JMenuItem();
        mi_showMuteBanList = new javax.swing.JMenuItem();
        mi_showQuickbar = new javax.swing.JCheckBoxMenuItem();
        m_channels = new javax.swing.JMenu();
        mi_channelList = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        mi_addCurrentToFav = new javax.swing.JMenuItem();
        mi_makeHome = new javax.swing.JMenuItem();
        mi_seperator = new javax.swing.JSeparator();
        m_Favourites = new javax.swing.JMenu();
        m_installedGames = new javax.swing.JMenu();
        m_tabs = new javax.swing.JMenu();
        mi_prevTab = new javax.swing.JMenuItem();
        mi_nextTab = new javax.swing.JMenuItem();
        mi_closeTab = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        mi_showTransfers = new javax.swing.JMenuItem();
        m_options = new javax.swing.JMenu();
        mi_clientSettings = new javax.swing.JMenuItem();
        mi_manageGames = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        mi_Sounds = new javax.swing.JCheckBoxMenuItem();
        m_help = new javax.swing.JMenu();
        mi_guide = new javax.swing.JMenuItem();
        mi_faq = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        mi_bugReport = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        mi_about = new javax.swing.JMenuItem();
        tabpn_tabs.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        tabpn_tabs.setFocusable(false);
        tabpn_tabs.setMinimumSize(new java.awt.Dimension(200, 350));
        tabpn_tabs.setPreferredSize(new java.awt.Dimension(400, 350));
        tabpn_tabs.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabpn_tabsStateChanged(evt);
            }
        });
        tabpn_tabs.addContainerListener(new java.awt.event.ContainerAdapter() {

            public void componentAdded(java.awt.event.ContainerEvent evt) {
                tabpn_tabsComponentAdded(evt);
            }

            public void componentRemoved(java.awt.event.ContainerEvent evt) {
                tabpn_tabsComponentRemoved(evt);
            }
        });
        tabpn_tabs.addInputMethodListener(new java.awt.event.InputMethodListener() {

            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
                tabpn_tabsCaretPositionChanged(evt);
            }

            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
            }
        });
        setTitle("CoopnetClient " + Globals.CLIENT_VERSION);
        setMinimumSize(new java.awt.Dimension(600, 400));
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());
        slp_mainSplitPanel.setBorder(null);
        slp_mainSplitPanel.setResizeWeight(0.5);
        slp_mainSplitPanel.setFocusable(false);
        slp_mainSplitPanel.setMinimumSize(new java.awt.Dimension(600, 400));
        slp_mainSplitPanel.setPreferredSize(new java.awt.Dimension(600, 350));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(slp_mainSplitPanel, gridBagConstraints);
        pnl_toggleQuickBarLeft.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        pnl_toggleQuickBarLeft.setFocusable(false);
        pnl_toggleQuickBarLeft.setMinimumSize(new java.awt.Dimension(5, 10));
        pnl_toggleQuickBarLeft.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseEntered(java.awt.event.MouseEvent evt) {
                pnl_toggleQuickBarLeftMouseEntered(evt);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                pnl_toggleQuickBarLeftMouseExited(evt);
            }

            public void mousePressed(java.awt.event.MouseEvent evt) {
                pnl_toggleQuickBarLeftMousePressed(evt);
            }
        });
        javax.swing.GroupLayout pnl_toggleQuickBarLeftLayout = new javax.swing.GroupLayout(pnl_toggleQuickBarLeft);
        pnl_toggleQuickBarLeft.setLayout(pnl_toggleQuickBarLeftLayout);
        pnl_toggleQuickBarLeftLayout.setHorizontalGroup(pnl_toggleQuickBarLeftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 1, Short.MAX_VALUE));
        pnl_toggleQuickBarLeftLayout.setVerticalGroup(pnl_toggleQuickBarLeftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 463, Short.MAX_VALUE));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(pnl_toggleQuickBarLeft, gridBagConstraints);
        pnl_toggleQuickBarRight.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        pnl_toggleQuickBarRight.setFocusable(false);
        pnl_toggleQuickBarRight.setMinimumSize(new java.awt.Dimension(5, 10));
        pnl_toggleQuickBarRight.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                pnl_toggleQuickBarRightMousePressed(evt);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                pnl_toggleQuickBarRightMouseExited(evt);
            }

            public void mouseEntered(java.awt.event.MouseEvent evt) {
                pnl_toggleQuickBarRightMouseEntered(evt);
            }
        });
        javax.swing.GroupLayout pnl_toggleQuickBarRightLayout = new javax.swing.GroupLayout(pnl_toggleQuickBarRight);
        pnl_toggleQuickBarRight.setLayout(pnl_toggleQuickBarRightLayout);
        pnl_toggleQuickBarRightLayout.setHorizontalGroup(pnl_toggleQuickBarRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 1, Short.MAX_VALUE));
        pnl_toggleQuickBarRightLayout.setVerticalGroup(pnl_toggleQuickBarRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 463, Short.MAX_VALUE));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(pnl_toggleQuickBarRight, gridBagConstraints);
        mbar.setFocusable(false);
        m_main.setMnemonic(KeyEvent.VK_L);
        m_main.setText("Client");
        mi_connection.setMnemonic(KeyEvent.VK_O);
        mi_connection.setText("Disconnect");
        mi_connection.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_connectionActionPerformed(evt);
            }
        });
        m_main.add(mi_connection);
        mi_update.setMnemonic(KeyEvent.VK_U);
        mi_update.setText("Update Client");
        mi_update.setEnabled(false);
        mi_update.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_updateActionPerformed(evt);
            }
        });
        m_main.add(mi_update);
        mi_quit.setMnemonic(KeyEvent.VK_Q);
        mi_quit.setText("Quit");
        mi_quit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_quitActionPerformed(evt);
            }
        });
        m_main.add(mi_quit);
        mbar.add(m_main);
        m_user.setMnemonic(KeyEvent.VK_U);
        m_user.setText("User");
        m_user.setEnabled(false);
        mi_profile.setMnemonic(KeyEvent.VK_P);
        mi_profile.setText("Edit Profile...");
        mi_profile.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_profileActionPerformed(evt);
            }
        });
        m_user.add(mi_profile);
        mi_showMuteBanList.setMnemonic(KeyEvent.VK_M);
        mi_showMuteBanList.setText("Edit Mute/Ban List...");
        mi_showMuteBanList.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_showMuteBanListActionPerformed(evt);
            }
        });
        m_user.add(mi_showMuteBanList);
        mi_showQuickbar.setMnemonic(KeyEvent.VK_Q);
        mi_showQuickbar.setText("Show Quickbar");
        mi_showQuickbar.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_showQuickbarActionPerformed(evt);
            }
        });
        m_user.add(mi_showQuickbar);
        mbar.add(m_user);
        m_channels.setMnemonic(KeyEvent.VK_A);
        m_channels.setText("Channels");
        m_channels.setEnabled(false);
        mi_channelList.setMnemonic(KeyEvent.VK_C);
        mi_channelList.setText("Open Channel List");
        mi_channelList.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_channelListActionPerformed(evt);
            }
        });
        m_channels.add(mi_channelList);
        m_channels.add(jSeparator4);
        mi_addCurrentToFav.setMnemonic(KeyEvent.VK_D);
        mi_addCurrentToFav.setText("Add Current to Favourites");
        mi_addCurrentToFav.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_addCurrentToFavActionPerformed(evt);
            }
        });
        m_channels.add(mi_addCurrentToFav);
        mi_makeHome.setMnemonic(KeyEvent.VK_H);
        mi_makeHome.setText("Make This my Homechannel");
        mi_makeHome.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_makeHomeActionPerformed(evt);
            }
        });
        m_channels.add(mi_makeHome);
        m_channels.add(mi_seperator);
        m_Favourites.setMnemonic(KeyEvent.VK_F);
        m_Favourites.setText("Favourites");
        m_channels.add(m_Favourites);
        m_installedGames.setMnemonic(KeyEvent.VK_I);
        m_installedGames.setText("Installed Games");
        m_channels.add(m_installedGames);
        mbar.add(m_channels);
        m_tabs.setMnemonic(KeyEvent.VK_T);
        m_tabs.setText("Tabs");
        mi_prevTab.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PAGE_UP, java.awt.event.InputEvent.CTRL_MASK));
        mi_prevTab.setMnemonic(KeyEvent.VK_P);
        mi_prevTab.setText("Previous Tab");
        mi_prevTab.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_prevTabActionPerformed(evt);
            }
        });
        m_tabs.add(mi_prevTab);
        mi_nextTab.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PAGE_DOWN, java.awt.event.InputEvent.CTRL_MASK));
        mi_nextTab.setMnemonic(KeyEvent.VK_N);
        mi_nextTab.setText("Next Tab");
        mi_nextTab.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_nextTabActionPerformed(evt);
            }
        });
        m_tabs.add(mi_nextTab);
        mi_closeTab.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_MASK));
        mi_closeTab.setMnemonic(KeyEvent.VK_C);
        mi_closeTab.setText("CloseTab");
        mi_closeTab.setEnabled(false);
        mi_closeTab.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_closeTabActionPerformed(evt);
            }
        });
        m_tabs.add(mi_closeTab);
        m_tabs.add(jSeparator5);
        mi_showTransfers.setMnemonic(KeyEvent.VK_S);
        mi_showTransfers.setText("Show Transfers");
        mi_showTransfers.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_showTransfersActionPerformed(evt);
            }
        });
        m_tabs.add(mi_showTransfers);
        mbar.add(m_tabs);
        m_options.setMnemonic(KeyEvent.VK_O);
        m_options.setText("Options");
        mi_clientSettings.setMnemonic(KeyEvent.VK_E);
        mi_clientSettings.setText("Edit Settings...");
        mi_clientSettings.setActionCommand("Client settings");
        mi_clientSettings.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_clientSettingsActionPerformed(evt);
            }
        });
        m_options.add(mi_clientSettings);
        mi_manageGames.setMnemonic(KeyEvent.VK_M);
        mi_manageGames.setText("Manage Games...");
        mi_manageGames.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_manageGamesActionPerformed(evt);
            }
        });
        m_options.add(mi_manageGames);
        m_options.add(jSeparator2);
        mi_Sounds.setMnemonic(KeyEvent.VK_S);
        mi_Sounds.setSelected(true);
        mi_Sounds.setText("Sounds");
        mi_Sounds.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_SoundsActionPerformed(evt);
            }
        });
        m_options.add(mi_Sounds);
        mbar.add(m_options);
        m_help.setMnemonic(KeyEvent.VK_H);
        m_help.setText("Help");
        mi_guide.setMnemonic(KeyEvent.VK_B);
        mi_guide.setText("Beginner's Guide");
        mi_guide.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_guideActionPerformed(evt);
            }
        });
        m_help.add(mi_guide);
        mi_faq.setMnemonic(KeyEvent.VK_F);
        mi_faq.setText("FAQ");
        mi_faq.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_faqActionPerformed(evt);
            }
        });
        m_help.add(mi_faq);
        m_help.add(jSeparator1);
        mi_bugReport.setMnemonic(KeyEvent.VK_R);
        mi_bugReport.setText("Report a Bug...");
        mi_bugReport.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_bugReportActionPerformed(evt);
            }
        });
        m_help.add(mi_bugReport);
        m_help.add(jSeparator3);
        mi_about.setMnemonic(KeyEvent.VK_A);
        mi_about.setText("About");
        mi_about.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mi_aboutActionPerformed(evt);
            }
        });
        m_help.add(mi_about);
        mbar.add(m_help);
        setJMenuBar(mbar);
        getAccessibleContext().setAccessibleName("Client");
        pack();
    }

    private void mi_profileActionPerformed(java.awt.event.ActionEvent evt) {
        Protocol.editProfile();
    }

    private void mi_quitActionPerformed(java.awt.event.ActionEvent evt) {
        Client.quit(true);
    }

    private void mi_clientSettingsActionPerformed(java.awt.event.ActionEvent evt) {
        Globals.openSettingsFrame();
    }

    private void mi_aboutActionPerformed(java.awt.event.ActionEvent evt) {
        String aboutMessage = "This software was developed by people who played " + "\nFallout Tactics multiplayer on Gamespy Arcade." + "\n\nIt aims to be a free GSA-like application that doesn't " + "\nannoy the player with advertisements and bugs." + "\n\nFuture plans include support for many different games" + "\nand added functionality to meet the gamers needs." + "\n\nVisit us at:" + "\n<html>&nbsp;&nbsp;&nbsp;&nbsp;<a href='http://coopnet.sourceforge.net'><font size='2'>http://coopnet.sourceforge.net</font></a></html>" + "\n\n\n<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + "<i>Thank you for choosing Coopnet!</i></html>\n ";
        JOptionPane.showMessageDialog(Globals.getClientFrame(), aboutMessage, "About Coopnet", JOptionPane.PLAIN_MESSAGE);
    }

    private void clearFavourites() {
        m_Favourites.removeAll();
        m_Favourites.setEnabled(false);
    }

    private void addFavourite(String channelname) {
        m_Favourites.add(new FavMenuItem(channelname));
        m_Favourites.setEnabled(true);
    }

    public void refreshFavourites() {
        clearFavourites();
        for (String s : Settings.getFavouritesByName()) {
            addFavourite(s);
        }
        m_Favourites.revalidate();
        pnl_QuickPanel.refreshFavourites();
    }

    private void clearInstalledGames() {
        m_installedGames.removeAll();
        m_installedGames.setEnabled(false);
    }

    private void addInstalledGame(String channelname) {
        m_installedGames.add(new FavMenuItem(channelname));
        m_installedGames.setEnabled(true);
    }

    public void refreshInstalledGames() {
        clearInstalledGames();
        for (String s : GameDatabase.getInstalledGameNames()) {
            addInstalledGame(s);
        }
        m_installedGames.revalidate();
    }

    /**
     * Prints the message to a currently visible chatbox(room or main window)<P>
     * Usage:<ul> 
     * <li> name - the name of the sender
     * <li> message - the message to be printed
     * <li> mode : defines the style of the printed text, can be system or chat or whisper
     * 
     */
    public void printToVisibleChatbox(String name, String message, ChatStyles modeStyle, boolean popupEnabled) {
        Component tc = tabpn_tabs.getSelectedComponent();
        if (tc == null || tc instanceof LoginPanel) {
            if (popupEnabled) {
                JOptionPane.showMessageDialog(Globals.getClientFrame(), message, "Message", JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (tc instanceof ChannelPanel) {
            ChannelPanel cp = (ChannelPanel) tc;
            cp.printMainChatMessage(name, message, modeStyle);
        } else if (tc instanceof RoomPanel) {
            RoomPanel rp = (RoomPanel) tc;
            rp.chat(name, message, modeStyle);
        } else if (tc instanceof PrivateChatPanel) {
            PrivateChatPanel pcp = (PrivateChatPanel) tc;
            pcp.append(name, message, modeStyle);
        } else {
            ChannelPanel cp = TabOrganizer.getChannelPanel(0);
            if (cp != null) {
                cp.printMainChatMessage(name, message, modeStyle);
            }
        }
    }

    public void updateMenu() {
        mi_Sounds.setSelected(Settings.getSoundEnabled());
    }

    protected JTabbedPane getTabHolder() {
        return tabpn_tabs;
    }

    private void mi_channelListActionPerformed(java.awt.event.ActionEvent evt) {
        Globals.openChannelListFrame();
    }

    private void tabpn_tabsStateChanged(javax.swing.event.ChangeEvent evt) {
        TabOrganizer.putFocusOnTab(null);
        TabOrganizer.unMarkTab(tabpn_tabs.getSelectedComponent());
        updateTabNavigationMenuItems();
    }

    private void tabpn_tabsComponentAdded(java.awt.event.ContainerEvent evt) {
        TabOrganizer.putFocusOnTab(null);
        updateTabNavigationMenuItems();
    }

    private void tabpn_tabsComponentRemoved(java.awt.event.ContainerEvent evt) {
        TabOrganizer.putFocusOnTab(null);
        updateTabNavigationMenuItems();
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        Client.quit(false);
    }

    private void mi_SoundsActionPerformed(java.awt.event.ActionEvent evt) {
        coopnetclient.utils.Settings.setSoundEnabled(mi_Sounds.isSelected());
    }

    private void mi_addCurrentToFavActionPerformed(java.awt.event.ActionEvent evt) {
        Component c = tabpn_tabs.getSelectedComponent();
        if (c instanceof ChannelPanel) {
            ChannelPanel cp = (ChannelPanel) c;
            Settings.addFavouriteByName(cp.name);
        }
    }

    private void mi_manageGamesActionPerformed(java.awt.event.ActionEvent evt) {
        Globals.openManageGamesFrame();
    }

    private void mi_guideActionPerformed(java.awt.event.ActionEvent evt) {
        HyperlinkMouseListener.openURL("http://coopnet.sourceforge.net/guide.html");
    }

    public void invokeUpdate() {
        new Thread() {

            @Override
            public void run() {
                try {
                    int n = JOptionPane.showConfirmDialog(null, "<html>Would you like to update your CoopnetClient now?<br>" + "(The client will close and update itself)", "Client outdated", JOptionPane.YES_NO_OPTION);
                    if (n == JOptionPane.YES_OPTION) {
                        try {
                            FileDownloader.downloadFile("http://coopnet.sourceforge.net/latestUpdater.php", Globals.getResourceAsString("CoopnetUpdater.jar"));
                            Runtime rt = Runtime.getRuntime();
                            rt.exec("java -jar CoopnetUpdater.jar", null, Client.getCurrentDirectory());
                            Client.quit(true);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    ErrorHandler.handleException(e);
                }
            }
        }.start();
    }

    private void mi_updateActionPerformed(java.awt.event.ActionEvent evt) {
        invokeUpdate();
    }

    private void mi_bugReportActionPerformed(java.awt.event.ActionEvent evt) {
        Globals.openBugReportFrame();
    }

    private void pnl_toggleQuickBarLeftMouseEntered(java.awt.event.MouseEvent evt) {
        if (Globals.getLoggedInStatus()) {
            if (Settings.getColorizeBody()) {
                pnl_toggleQuickBarLeft.setBackground(Settings.getSelectionColor());
            } else {
                pnl_toggleQuickBarLeft.setBackground(getHoverEffectColor());
            }
        }
    }

    private void pnl_toggleQuickBarLeftMouseExited(java.awt.event.MouseEvent evt) {
        if (Globals.getLoggedInStatus()) {
            if (Settings.getColorizeBody()) {
                pnl_toggleQuickBarLeft.setBackground(Settings.getBackgroundColor());
            } else {
                pnl_toggleQuickBarLeft.setBackground((Color) UIManager.get("Panel.background"));
            }
        }
    }

    private void pnl_toggleQuickBarLeftMousePressed(java.awt.event.MouseEvent evt) {
        if (Globals.getLoggedInStatus()) {
            quickPanelVisibility = !quickPanelVisibility;
            setQuickPanelVisibility(quickPanelVisibility);
            quickPanelFlashing = false;
        }
    }

    private void pnl_toggleQuickBarRightMouseEntered(java.awt.event.MouseEvent evt) {
        if (Globals.getLoggedInStatus()) {
            if (Settings.getColorizeBody()) {
                pnl_toggleQuickBarRight.setBackground(Settings.getSelectionColor());
            } else {
                pnl_toggleQuickBarRight.setBackground(getHoverEffectColor());
            }
        }
    }

    private void pnl_toggleQuickBarRightMouseExited(java.awt.event.MouseEvent evt) {
        if (Globals.getLoggedInStatus()) {
            if (Settings.getColorizeBody()) {
                pnl_toggleQuickBarRight.setBackground(Settings.getBackgroundColor());
            } else {
                pnl_toggleQuickBarRight.setBackground((Color) UIManager.get("Panel.background"));
            }
        }
    }

    private void pnl_toggleQuickBarRightMousePressed(java.awt.event.MouseEvent evt) {
        if (Globals.getLoggedInStatus()) {
            quickPanelVisibility = !quickPanelVisibility;
            setQuickPanelVisibility(quickPanelVisibility);
            quickPanelFlashing = false;
        }
    }

    private void mi_showMuteBanListActionPerformed(java.awt.event.ActionEvent evt) {
        if (Globals.getLoggedInStatus()) {
            Globals.openMuteBanTableFrame();
        }
    }

    private void mi_showQuickbarActionPerformed(java.awt.event.ActionEvent evt) {
        setQuickPanelVisibility(mi_showQuickbar.isSelected());
    }

    private void mi_connectionActionPerformed(java.awt.event.ActionEvent evt) {
        if (!Globals.getConnectionStatus()) {
            Client.startConnection();
        } else {
            Client.disconnect();
        }
    }

    private void mi_makeHomeActionPerformed(java.awt.event.ActionEvent evt) {
        Component c = tabpn_tabs.getSelectedComponent();
        if (c instanceof ChannelPanel) {
            ChannelPanel cp = (ChannelPanel) c;
            Settings.setHomeChannel(cp.name);
        }
    }

    private void mi_faqActionPerformed(java.awt.event.ActionEvent evt) {
        HyperlinkMouseListener.openURL("http://coopnet.sourceforge.net/faq.html");
    }

    private void mi_showTransfersActionPerformed(java.awt.event.ActionEvent evt) {
        TabOrganizer.openTransferPanel(true);
    }

    private void mi_nextTabActionPerformed(java.awt.event.ActionEvent evt) {
        if (tabpn_tabs.getSelectedIndex() < (tabpn_tabs.getTabCount() - 1)) {
            tabpn_tabs.setSelectedIndex((tabpn_tabs.getSelectedIndex() + 1));
        } else {
            tabpn_tabs.setSelectedIndex(0);
        }
        updateTabNavigationMenuItems();
    }

    private void mi_prevTabActionPerformed(java.awt.event.ActionEvent evt) {
        if (tabpn_tabs.getSelectedIndex() > 0) {
            tabpn_tabs.setSelectedIndex((tabpn_tabs.getSelectedIndex() - 1));
        } else {
            tabpn_tabs.setSelectedIndex(tabpn_tabs.getTabCount() - 1);
        }
        updateTabNavigationMenuItems();
    }

    private void mi_closeTabActionPerformed(java.awt.event.ActionEvent evt) {
        if (tabpn_tabs.getSelectedComponent() instanceof ClosableTab) {
            ClosableTab ctab = ((ClosableTab) tabpn_tabs.getSelectedComponent());
            if (ctab.isCurrentlyClosable()) {
                ctab.closeTab();
            }
        }
        updateTabNavigationMenuItems();
    }

    private void tabpn_tabsCaretPositionChanged(java.awt.event.InputMethodEvent evt) {
        updateTabNavigationMenuItems();
    }

    private void updateTabNavigationMenuItems() {
        if (tabpn_tabs.getSelectedComponent() instanceof ClosableTab) {
            ClosableTab ctab = (ClosableTab) tabpn_tabs.getSelectedComponent();
            mi_closeTab.setEnabled(ctab.isCurrentlyClosable());
        }
        mi_nextTab.setEnabled(tabpn_tabs.getTabCount() > 1);
        mi_prevTab.setEnabled(tabpn_tabs.getTabCount() > 1);
    }

    private Color getHoverEffectColor() {
        Color clr = null;
        clr = (Color) UIManager.get("List.selectionBackground");
        if (clr == null) {
            clr = (Color) UIManager.get("List[Selected].textBackground");
        }
        return clr;
    }

    private javax.swing.JSeparator jSeparator1;

    private javax.swing.JSeparator jSeparator2;

    private javax.swing.JSeparator jSeparator3;

    private javax.swing.JSeparator jSeparator4;

    private javax.swing.JSeparator jSeparator5;

    private javax.swing.JMenu m_Favourites;

    private javax.swing.JMenu m_channels;

    private javax.swing.JMenu m_help;

    private javax.swing.JMenu m_installedGames;

    private javax.swing.JMenu m_main;

    private javax.swing.JMenu m_options;

    private javax.swing.JMenu m_tabs;

    private javax.swing.JMenu m_user;

    private javax.swing.JMenuBar mbar;

    private javax.swing.JCheckBoxMenuItem mi_Sounds;

    private javax.swing.JMenuItem mi_about;

    private javax.swing.JMenuItem mi_addCurrentToFav;

    private javax.swing.JMenuItem mi_bugReport;

    private javax.swing.JMenuItem mi_channelList;

    private javax.swing.JMenuItem mi_clientSettings;

    private javax.swing.JMenuItem mi_closeTab;

    private javax.swing.JMenuItem mi_connection;

    private javax.swing.JMenuItem mi_faq;

    private javax.swing.JMenuItem mi_guide;

    private javax.swing.JMenuItem mi_makeHome;

    private javax.swing.JMenuItem mi_manageGames;

    private javax.swing.JMenuItem mi_nextTab;

    private javax.swing.JMenuItem mi_prevTab;

    private javax.swing.JMenuItem mi_profile;

    private javax.swing.JMenuItem mi_quit;

    private javax.swing.JSeparator mi_seperator;

    private javax.swing.JMenuItem mi_showMuteBanList;

    private javax.swing.JCheckBoxMenuItem mi_showQuickbar;

    private javax.swing.JMenuItem mi_showTransfers;

    private javax.swing.JMenuItem mi_update;

    private javax.swing.JPanel pnl_toggleQuickBarLeft;

    private javax.swing.JPanel pnl_toggleQuickBarRight;

    private javax.swing.JSplitPane slp_mainSplitPanel;

    private javax.swing.JTabbedPane tabpn_tabs;
}
