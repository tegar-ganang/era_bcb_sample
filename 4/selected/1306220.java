package coopnetclient.frames.clientframe;

import coopnetclient.ErrorHandler;
import coopnetclient.Globals;
import coopnetclient.enums.ChatStyles;
import coopnetclient.enums.ErrorPanelStyle;
import coopnetclient.enums.LaunchMethods;
import coopnetclient.enums.LogTypes;
import coopnetclient.frames.clientframe.tabs.BrowserPanel;
import coopnetclient.frames.clientframe.tabs.ChannelPanel;
import coopnetclient.frames.clientframe.tabs.ConnectingPanel;
import coopnetclient.frames.clientframe.tabs.ErrorPanel;
import coopnetclient.frames.clientframe.tabs.FileTransferPanel;
import coopnetclient.frames.clientframe.tabs.LoginPanel;
import coopnetclient.frames.clientframe.tabs.PasswordRecoveryPanel;
import coopnetclient.frames.clientframe.tabs.PrivateChatPanel;
import coopnetclient.frames.clientframe.tabs.RegisterPanel;
import coopnetclient.frames.clientframe.tabs.RoomPanel;
import coopnetclient.frames.components.TabComponent;
import coopnetclient.utils.Settings;
import coopnetclient.frames.listeners.TabbedPaneColorChangeListener;
import coopnetclient.protocol.out.Protocol;
import coopnetclient.utils.ui.Icons;
import coopnetclient.utils.Logger;
import coopnetclient.utils.RoomData;
import coopnetclient.utils.gamedatabase.GameDatabase;
import coopnetclient.utils.hotkeys.Hotkeys;
import coopnetclient.utils.launcher.Launcher;
import java.awt.Component;
import java.awt.Font;
import java.io.File;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

public class TabOrganizer {

    private static JTabbedPane tabHolder;

    private static Vector<ChannelPanel> channelPanels = new Vector<ChannelPanel>();

    private static RoomPanel roomPanel;

    private static Vector<PrivateChatPanel> privateChatPanels = new Vector<PrivateChatPanel>();

    private static BrowserPanel browserPanel;

    private static ErrorPanel errorPanel;

    private static LoginPanel loginPanel;

    private static RegisterPanel registerPanel;

    private static PasswordRecoveryPanel passwordRecoveryPanel;

    private static FileTransferPanel transferPanel;

    private static ConnectingPanel connectingPanel;

    static {
        tabHolder = Globals.getClientFrame().getTabHolder();
    }

    public static void openChannelPanel(String channelname) {
        int index = -1;
        index = tabHolder.indexOfTab(channelname);
        if (index != -1) {
            tabHolder.setSelectedIndex(index);
            return;
        }
        if (!Settings.getMultiChannel()) {
            if (channelPanels.size() > 1) {
                closeAllButLastChannelPanel();
            }
            if (channelPanels.size() == 1) {
                closeChannelPanel(channelPanels.firstElement());
            }
        }
        ChannelPanel currentchannel = new ChannelPanel(channelname);
        tabHolder.add(currentchannel, 0);
        tabHolder.setTitleAt(0, channelname);
        tabHolder.setTabComponentAt(0, new TabComponent(GameDatabase.getShortName(channelname), currentchannel));
        channelPanels.add(currentchannel);
        if (GameDatabase.getLaunchMethod(channelname, null) == LaunchMethods.CHAT_ONLY) {
            currentchannel.hideRoomList();
        } else {
            if (!GameDatabase.isLaunchable(channelname)) {
                currentchannel.disableButtons();
                currentchannel.printMainChatMessage("SYSTEM", "The game couldn't be detected, please set the path manually at " + "options/manage games to enable playing this game!", ChatStyles.SYSTEM);
            } else {
                currentchannel.setLaunchable(true);
            }
            if (GameDatabase.isBeta(GameDatabase.getIDofGame(channelname))) {
                currentchannel.printMainChatMessage("SYSTEM", "Support for this game is experimental," + " email coopnetbugs@gmail.com if you have problems!", ChatStyles.SYSTEM);
            }
        }
        Globals.getClientFrame().repaint();
        tabHolder.setSelectedComponent(currentchannel);
    }

    public static void updateHighlights() {
        for (ChannelPanel cp : channelPanels) {
            cp.updateHighlights();
        }
        if (roomPanel != null) {
            roomPanel.updateHighlights();
        }
    }

    public static void markTab(Component tab) {
        int idx = tabHolder.indexOfComponent(tab);
        if (idx > -1 && tabHolder.getSelectedIndex() != idx) {
            Component tabComponent = tabHolder.getTabComponentAt(idx);
            if (tabComponent != null) {
                Font f = tabComponent.getFont().deriveFont(Font.BOLD);
                if (f != null) {
                    tabComponent.setFont(f);
                }
            }
        }
    }

    public static void unMarkTab(Component tab) {
        int idx = tabHolder.indexOfComponent(tab);
        if (idx > -1) {
            Component tabComponent = tabHolder.getTabComponentAt(idx);
            if (tabComponent != null) {
                Font f = tabComponent.getFont().deriveFont(Font.PLAIN);
                if (f != null) {
                    tabComponent.setFont(f);
                }
            }
        }
    }

    public static void closeChannelPanel(String channelName) {
        closeChannelPanel(getChannelPanel(channelName));
    }

    public static void closeChannelPanel(ChannelPanel which) {
        Protocol.leaveChannel(which.name);
        channelPanels.remove(which);
        tabHolder.remove(which);
    }

    public static void closeAllButLastChannelPanel() {
        if (channelPanels.size() > 1) {
            ChannelPanel selectedChannel = null;
            if (tabHolder.getSelectedComponent() instanceof ChannelPanel) {
                selectedChannel = (ChannelPanel) tabHolder.getSelectedComponent();
            }
            if (selectedChannel != null) {
                for (int i = 0; i < channelPanels.size(); i++) {
                    while (channelPanels.size() > 1) {
                        if (channelPanels.firstElement() == selectedChannel) {
                            closeChannelPanel(channelPanels.get(1));
                        } else {
                            closeChannelPanel(channelPanels.firstElement());
                        }
                    }
                }
            } else {
                for (int i = channelPanels.size() - 2; i >= 0; i--) {
                    closeChannelPanel(channelPanels.get(i));
                }
            }
        }
    }

    public static ChannelPanel getChannelPanel(String channelName) {
        int index = tabHolder.indexOfTab(channelName);
        if (index != -1) {
            return (ChannelPanel) tabHolder.getComponentAt(index);
        } else {
            return null;
        }
    }

    public static ChannelPanel getChannelPanel(int index) {
        if (index < channelPanels.size()) {
            return channelPanels.get(index);
        } else {
            return null;
        }
    }

    public static void openRoomPanel(RoomData roomData) {
        if (roomPanel == null) {
            roomPanel = new RoomPanel(roomData);
            Globals.closeJoinRoomPasswordFrame();
            tabHolder.insertTab("Room:" + GameDatabase.getShortName(roomData.getChannel()), null, roomPanel, roomData.getChannel(), channelPanels.size());
            tabHolder.setTabComponentAt(channelPanels.size(), new TabComponent("Room:" + GameDatabase.getShortName(roomData.getChannel()), Icons.lobbyIconSmall, roomPanel));
            tabHolder.setSelectedComponent(roomPanel);
            for (ChannelPanel cp : channelPanels) {
                cp.disableButtons();
            }
            roomPanel.initLauncher();
        } else {
            Logger.log(LogTypes.WARNING, "Close the current RoomPanel before opening a new one!");
        }
    }

    public static void closeRoomPanel() {
        if (roomPanel != null) {
            tabHolder.remove(roomPanel);
            int index = tabHolder.indexOfTab(roomPanel.getRoomData().getChannel());
            if (index != -1) {
                tabHolder.setSelectedIndex(index);
            }
            if (roomPanel.isHost()) {
                Hotkeys.unbindHotKey(Hotkeys.ACTION_LAUNCH);
            }
            roomPanel = null;
            if (!Launcher.isPlaying()) {
                Launcher.deInitialize();
            }
            Globals.closeGameSettingsFrame();
            for (ChannelPanel cp : channelPanels) {
                cp.enableButtons();
            }
        }
    }

    public static RoomPanel getRoomPanel() {
        return roomPanel;
    }

    public static void openPrivateChatPanel(String title, boolean setFocus) {
        int index = tabHolder.indexOfTab(title);
        if (index == -1) {
            PrivateChatPanel pc = new PrivateChatPanel(title);
            tabHolder.add(title, pc);
            tabHolder.setTabComponentAt(tabHolder.indexOfComponent(pc), new TabComponent(title, Icons.privateChatIconSmall, pc));
            privateChatPanels.add(pc);
            if (setFocus) {
                tabHolder.setSelectedComponent(pc);
                pc.requestFocus();
            } else {
                if (Settings.getColorizeBody()) {
                    ChangeListener[] listeners = tabHolder.getChangeListeners();
                    for (int i = 0; i < listeners.length; i++) {
                        if (listeners[i] instanceof TabbedPaneColorChangeListener) {
                            TabbedPaneColorChangeListener cl = (TabbedPaneColorChangeListener) listeners[i];
                            cl.updateBG();
                            break;
                        }
                    }
                }
            }
        } else {
            if (setFocus) {
                tabHolder.setSelectedIndex(tabHolder.indexOfTab(title));
                tabHolder.getSelectedComponent().requestFocus();
            }
        }
    }

    public static void updateMuteBanStatus(String username) {
        for (int i = 0; i < privateChatPanels.size(); i++) {
            if (privateChatPanels.get(i).getPartner().equals(username)) {
                privateChatPanels.get(i).updateMuteBanStatus();
            }
        }
    }

    public static void closePrivateChatPanel(PrivateChatPanel which) {
        tabHolder.remove(which);
        privateChatPanels.remove(which);
    }

    public static PrivateChatPanel getPrivateChatPanel(String title) {
        int index = tabHolder.indexOfTab(title);
        if (index != -1) {
            JPanel panel = (JPanel) tabHolder.getComponentAt(index);
            if (panel instanceof PrivateChatPanel) {
                return (PrivateChatPanel) panel;
            } else {
                Logger.log(LogTypes.WARNING, "The Panel \"" + title + "\" is not a PrivateChatPanel!");
            }
        }
        return null;
    }

    public static void openBrowserPanel(String url) {
        if (browserPanel == null) {
            browserPanel = new BrowserPanel(url);
            String title = "Beginner's Guide";
            tabHolder.addTab(title, browserPanel);
            tabHolder.setTabComponentAt(tabHolder.indexOfComponent(browserPanel), new TabComponent(title, browserPanel));
            tabHolder.setSelectedComponent(browserPanel);
        } else {
            tabHolder.setSelectedComponent(browserPanel);
            browserPanel.openUrl(url);
        }
        Globals.getClientFrame().repaint();
    }

    public static void closeBrowserPanel() {
        tabHolder.remove(browserPanel);
        browserPanel = null;
    }

    public static synchronized void openErrorPanel(final ErrorPanelStyle mode, final Throwable e) {
        SwingUtilities.invokeLater(new Thread() {

            @Override
            public void run() {
                try {
                    if (mode == ErrorPanelStyle.PROTOCOL_VERSION_MISMATCH) {
                        errorPanel = new ErrorPanel(mode, null);
                        tabHolder.addTab("Client too old", null, errorPanel);
                        tabHolder.setTabComponentAt(tabHolder.indexOfComponent(errorPanel), new TabComponent("Client too old", Icons.errorIconSmall, errorPanel));
                        tabHolder.setSelectedComponent(errorPanel);
                    } else {
                        if (errorPanel == null || errorPanel.hasException() == false && e != null) {
                            errorPanel = new ErrorPanel(mode, e);
                            tabHolder.addTab("Error", null, errorPanel);
                            tabHolder.setTabComponentAt(tabHolder.indexOfComponent(errorPanel), new TabComponent("Error", Icons.errorIconSmall, errorPanel));
                            tabHolder.setSelectedComponent(errorPanel);
                        } else {
                            Logger.log(LogTypes.WARNING, "We don't need another error tab, this error may be caused by the first one!");
                            tabHolder.setSelectedComponent(errorPanel);
                        }
                    }
                    Globals.getClientFrame().repaint();
                } catch (Exception e) {
                    ErrorHandler.handleException(e);
                }
            }
        });
    }

    public static void closeErrorPanel() {
        tabHolder.remove(errorPanel);
        errorPanel = null;
    }

    public static synchronized void openLoginPanel() {
        if (loginPanel == null) {
            SwingUtilities.invokeLater(new Thread() {

                @Override
                public void run() {
                    try {
                        closeConnectingPanel();
                        loginPanel = new LoginPanel();
                        tabHolder.addTab("Login", loginPanel);
                        tabHolder.setSelectedComponent(loginPanel);
                    } catch (Exception e) {
                        ErrorHandler.handleException(e);
                    }
                }
            });
        } else {
            Logger.log(LogTypes.WARNING, "There's already a LoginPanel !");
            tabHolder.setSelectedComponent(loginPanel);
        }
    }

    public static void closeLoginPanel() {
        tabHolder.remove(loginPanel);
        loginPanel = null;
    }

    public static void openRegisterPanel(final String loginname) {
        if (registerPanel == null) {
            SwingUtilities.invokeLater(new Thread() {

                @Override
                public void run() {
                    try {
                        registerPanel = new RegisterPanel(loginname);
                        tabHolder.addTab("Register", registerPanel);
                        tabHolder.setSelectedComponent(registerPanel);
                    } catch (Exception e) {
                        ErrorHandler.handleException(e);
                    }
                }
            });
        } else {
            Logger.log(LogTypes.WARNING, "There's an opened RegisterPanel already!");
            tabHolder.setSelectedComponent(registerPanel);
        }
    }

    public static void closeRegisterPanel() {
        tabHolder.remove(registerPanel);
        registerPanel = null;
    }

    public static void openConnectingPanel() {
        if (connectingPanel == null) {
            SwingUtilities.invokeLater(new Thread() {

                @Override
                public void run() {
                    try {
                        connectingPanel = new ConnectingPanel();
                        tabHolder.addTab("Connecting", connectingPanel);
                        tabHolder.setSelectedComponent(connectingPanel);
                    } catch (Exception e) {
                        ErrorHandler.handleException(e);
                    }
                }
            });
        } else {
            Logger.log(LogTypes.WARNING, "There's an opened ConnectingPanel already!");
            tabHolder.setSelectedComponent(connectingPanel);
        }
    }

    public static void closeConnectingPanel() {
        tabHolder.remove(connectingPanel);
        connectingPanel = null;
    }

    public static void openPasswordRecoveryPanel() {
        if (passwordRecoveryPanel == null) {
            SwingUtilities.invokeLater(new Thread() {

                @Override
                public void run() {
                    try {
                        passwordRecoveryPanel = new PasswordRecoveryPanel();
                        tabHolder.addTab("Password recovery", passwordRecoveryPanel);
                        tabHolder.setSelectedComponent(passwordRecoveryPanel);
                    } catch (Exception e) {
                        ErrorHandler.handleException(e);
                    }
                }
            });
        } else {
            Logger.log(LogTypes.WARNING, "There's an open LoginPanel already!");
            tabHolder.setSelectedComponent(passwordRecoveryPanel);
        }
    }

    public static void closePasswordRecoveryPanel() {
        tabHolder.remove(passwordRecoveryPanel);
        passwordRecoveryPanel = null;
    }

    public static LoginPanel getLoginPanel() {
        return loginPanel;
    }

    public static RegisterPanel getRegisterPanel() {
        return registerPanel;
    }

    public static void openTransferPanel(boolean bringToFrontOnCreate) {
        if (transferPanel == null) {
            transferPanel = new FileTransferPanel(Globals.getTransferModel());
            int index = channelPanels.size();
            if (roomPanel != null) {
                ++index;
            }
            tabHolder.insertTab("Transfers", null, transferPanel, null, index);
            tabHolder.setTabComponentAt(index, new TabComponent("Transfers", Icons.transferIcon, transferPanel));
            if (bringToFrontOnCreate) {
                tabHolder.setSelectedComponent(transferPanel);
            } else {
                markTab(transferPanel);
            }
        } else {
            tabHolder.setSelectedComponent(transferPanel);
        }
        Globals.getClientFrame().repaint();
    }

    public static void closeTransferPanel() {
        tabHolder.remove(transferPanel);
        transferPanel = null;
    }

    public static FileTransferPanel getTransferPanel() {
        return transferPanel;
    }

    public static boolean sendFile(String reciever, File file) {
        if (transferPanel != null) {
            markTab(transferPanel);
        } else {
            openTransferPanel(false);
        }
        return Globals.getTransferModel().addSendTransfer(reciever, file.getName(), file);
    }

    public static void recieveFile(String peerName, String size, String fileName, String ip, String port) {
        if (transferPanel != null) {
            markTab(transferPanel);
        } else {
            openTransferPanel(false);
        }
        Globals.getTransferModel().addRecieveTransfer(peerName, size, fileName, ip, port);
    }

    public static void cancelFileSendingOnClose() {
        Globals.getTransferModel().cancelOrRefuseOnQuit();
    }

    /*******************************************************************/
    public static void updateTitleOnTab(String oldTitle, String newTitle) {
        int index = -1;
        while ((index = tabHolder.indexOfTab(oldTitle)) != -1) {
            tabHolder.setTitleAt(index, newTitle);
        }
    }

    public static void putFocusOnTab(String title) {
        if (title != null) {
            int index = tabHolder.indexOfTab(title);
            if (index != -1) {
                tabHolder.setSelectedIndex(index);
                tabHolder.getSelectedComponent().requestFocus();
            }
        } else {
            if (tabHolder.getSelectedComponent() != null) {
                tabHolder.getSelectedComponent().requestFocus();
            }
        }
    }

    public static void updateSleepMode() {
        for (ChannelPanel cp : channelPanels) {
            cp.updateSleepMode();
        }
    }

    public static void closeAllTabs() {
        SwingUtilities.invokeLater(new Thread() {

            @Override
            public void run() {
                try {
                    closeRoomPanel();
                    closeBrowserPanel();
                    closeErrorPanel();
                    closeLoginPanel();
                    closeTransferPanel();
                    closeConnectingPanel();
                    closePasswordRecoveryPanel();
                    closeRegisterPanel();
                    while (channelPanels.size() > 0) {
                        closeChannelPanel(channelPanels.get(0));
                    }
                    while (privateChatPanels.size() > 0) {
                        closePrivateChatPanel(privateChatPanels.get(0));
                    }
                    if (tabHolder.getTabCount() > 0) {
                        String text = "TabOrganizer.closeAllTabs() did not manually close all tabs! The following tabs were still open:";
                        for (int i = 0; i < tabHolder.getTabCount(); i++) {
                            text += "\n\t\t" + tabHolder.getComponentAt(i).getClass().getName();
                        }
                        ErrorHandler.handleException(new IllegalStateException(text));
                    }
                } catch (Throwable t) {
                    ErrorHandler.handleException(t);
                }
            }
        });
    }
}
