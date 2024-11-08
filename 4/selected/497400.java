package coopnetclient.frames.clientframetabs;

import coopnetclient.ErrorHandler;
import coopnetclient.Globals;
import coopnetclient.enums.ChatStyles;
import coopnetclient.enums.ErrorPanelStyle;
import coopnetclient.enums.LaunchMethods;
import coopnetclient.enums.LogTypes;
import coopnetclient.frames.FrameOrganizer;
import coopnetclient.frames.components.TabComponent;
import coopnetclient.utils.settings.Settings;
import coopnetclient.frames.listeners.TabbedPaneColorChangeListener;
import coopnetclient.protocol.out.Protocol;
import coopnetclient.utils.ui.Icons;
import coopnetclient.utils.Logger;
import coopnetclient.utils.RoomData;
import coopnetclient.utils.ui.SoundPlayer;
import coopnetclient.utils.gamedatabase.GameDatabase;
import coopnetclient.utils.hotkeys.Hotkeys;
import coopnetclient.utils.launcher.Launcher;
import coopnetclient.threads.EdtRunner;
import java.awt.Component;
import java.awt.Font;
import java.io.File;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

public final class TabOrganizer {

    private static final String ROOM = "Room:";

    private static final String SYSTEM = "SYSTEM";

    private static final String TRANSFERS = "Transfers";

    private static JTabbedPane tabHolder;

    private static Vector<ChannelPanel> channelPanels = new Vector<ChannelPanel>();

    private static RoomPanel roomPanel;

    private static Vector<PrivateChatPanel> privateChatPanels = new Vector<PrivateChatPanel>();

    private static ErrorPanel errorPanel;

    private static LoginPanel loginPanel;

    private static RegisterPanel registerPanel;

    private static PasswordRecoveryPanel passwordRecoveryPanel;

    private static FileTransferPanel transferPanel;

    private static ConnectingPanel connectingPanel;

    private TabOrganizer() {
    }

    public static void init(JTabbedPane tabHolder) {
        TabOrganizer.tabHolder = tabHolder;
    }

    public static void updateSettings() {
        if (!Settings.getMultiChannel()) {
            TabOrganizer.closeAllButLastChannelPanel();
        }
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
                currentchannel.printMainChatMessage(SYSTEM, "The game couldn't be detected, please set the path manually at " + "options/manage games to enable playing this game!", ChatStyles.SYSTEM);
            } else {
                currentchannel.setLaunchable(true);
            }
            if (GameDatabase.isBeta(GameDatabase.getIDofGame(channelname))) {
                currentchannel.printMainChatMessage(SYSTEM, "Support for this game is experimental," + " email coopnetbugs@gmail.com if you have problems!", ChatStyles.SYSTEM);
            }
        }
        FrameOrganizer.getClientFrame().repaint();
        tabHolder.setSelectedComponent(currentchannel);
    }

    public static void updateHighlights() {
        for (ChannelPanel cp : channelPanels) {
            cp.updateHighlights();
        }
        if (roomPanel != null) {
            roomPanel.updateHighlights();
        }
        for (PrivateChatPanel pcp : privateChatPanels) {
            pcp.updateHighlights();
        }
    }

    public static void updateStyle() {
        for (ChannelPanel cp : channelPanels) {
            cp.updateStyle();
        }
        if (roomPanel != null) {
            roomPanel.updateStyle();
        }
        for (PrivateChatPanel pcp : privateChatPanels) {
            pcp.updateStyle();
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
            FrameOrganizer.closeJoinRoomPasswordFrame();
            tabHolder.insertTab(ROOM + GameDatabase.getShortName(roomData.getChannel()), null, roomPanel, roomData.getChannel(), channelPanels.size());
            tabHolder.setTabComponentAt(channelPanels.size(), new TabComponent(ROOM + GameDatabase.getShortName(roomData.getChannel()), Icons.lobbyIconSmall, roomPanel));
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
            SoundPlayer.playRoomCloseSound();
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
            FrameOrganizer.closeGameSettingsFrame();
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

    public static synchronized void openErrorPanel(final ErrorPanelStyle mode, final Throwable e) {
        new EdtRunner() {

            private static final String CLIENT_TOO_OLD = "Client too old";

            private static final String ERROR = "Error";

            @Override
            public void handledRun() throws Throwable {
                if (mode == ErrorPanelStyle.PROTOCOL_VERSION_MISMATCH) {
                    errorPanel = new ErrorPanel(mode, null);
                    tabHolder.addTab(CLIENT_TOO_OLD, null, errorPanel);
                    tabHolder.setTabComponentAt(tabHolder.indexOfComponent(errorPanel), new TabComponent(CLIENT_TOO_OLD, Icons.errorIconSmall, errorPanel));
                    tabHolder.setSelectedComponent(errorPanel);
                } else {
                    if (errorPanel == null || !errorPanel.hasException() && e != null) {
                        errorPanel = new ErrorPanel(mode, e);
                        tabHolder.addTab(ERROR, null, errorPanel);
                        tabHolder.setTabComponentAt(tabHolder.indexOfComponent(errorPanel), new TabComponent(ERROR, Icons.errorIconSmall, errorPanel));
                        tabHolder.setSelectedComponent(errorPanel);
                    } else {
                        Logger.log(LogTypes.WARNING, "We don't need another error tab, this error may be caused by the first one!");
                        tabHolder.setSelectedComponent(errorPanel);
                    }
                }
                FrameOrganizer.getClientFrame().repaint();
            }
        }.invokeLater();
    }

    public static void closeErrorPanel() {
        tabHolder.remove(errorPanel);
        errorPanel = null;
    }

    public static synchronized void openLoginPanel() {
        if (loginPanel == null) {
            new EdtRunner() {

                @Override
                public void handledRun() throws Throwable {
                    closeConnectingPanel();
                    loginPanel = new LoginPanel();
                    tabHolder.addTab("Login", loginPanel);
                    tabHolder.setSelectedComponent(loginPanel);
                }
            }.invokeLater();
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
            new EdtRunner() {

                @Override
                public void handledRun() throws Throwable {
                    registerPanel = new RegisterPanel(loginname);
                    tabHolder.addTab("Register", registerPanel);
                    tabHolder.setSelectedComponent(registerPanel);
                }
            }.invokeLater();
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
            new EdtRunner() {

                @Override
                public void handledRun() throws Throwable {
                    connectingPanel = new ConnectingPanel();
                    tabHolder.addTab("Connecting", connectingPanel);
                    tabHolder.setSelectedComponent(connectingPanel);
                }
            }.invokeLater();
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
            new EdtRunner() {

                @Override
                public void handledRun() throws Throwable {
                    passwordRecoveryPanel = new PasswordRecoveryPanel();
                    tabHolder.addTab("Password recovery", passwordRecoveryPanel);
                    tabHolder.setSelectedComponent(passwordRecoveryPanel);
                }
            }.invokeLater();
        } else {
            Logger.log(LogTypes.WARNING, "There's an open LoginPanel already!");
            tabHolder.setSelectedComponent(passwordRecoveryPanel);
        }
    }

    public static PasswordRecoveryPanel getPasswordRecoveryPanel() {
        return passwordRecoveryPanel;
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
            tabHolder.insertTab(TRANSFERS, null, transferPanel, null, index);
            tabHolder.setTabComponentAt(index, new TabComponent(TRANSFERS, Icons.transferIcon, transferPanel));
            if (bringToFrontOnCreate) {
                tabHolder.setSelectedComponent(transferPanel);
            } else {
                markTab(transferPanel);
            }
        } else {
            tabHolder.setSelectedComponent(transferPanel);
        }
        FrameOrganizer.getClientFrame().repaint();
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
    public static void updatePrivateChatName(String oldName, String newName) {
        int index = -1;
        while ((index = tabHolder.indexOfTab(oldName)) != -1) {
            tabHolder.setTitleAt(index, newName);
            ((TabComponent) tabHolder.getTabComponentAt(index)).setTitle(newName);
            Component comp = tabHolder.getComponentAt(index);
            if (comp instanceof PrivateChatPanel) {
                ((PrivateChatPanel) comp).setPartner(newName);
            }
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
        new EdtRunner() {

            @Override
            public void handledRun() throws Throwable {
                closeRoomPanel();
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
                    String text = "TabOrganizer.closeAllTabs() did not manually close all tabs!" + " The following tabs were still open:";
                    for (int i = 0; i < tabHolder.getTabCount(); i++) {
                        text += "\n\t\t" + tabHolder.getComponentAt(i).getClass().getName();
                    }
                    ErrorHandler.handle(new IllegalStateException(text));
                }
            }
        }.invokeLater();
    }
}
