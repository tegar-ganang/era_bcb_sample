package coopnetclient.utils.launcher;

import coopnetclient.Globals;
import coopnetclient.enums.ChatStyles;
import coopnetclient.enums.LaunchMethods;
import coopnetclient.enums.LogTypes;
import coopnetclient.frames.FrameOrganizer;
import coopnetclient.frames.clientframetabs.TabOrganizer;
import coopnetclient.frames.clientframetabs.RoomPanel;
import coopnetclient.protocol.out.Protocol;
import coopnetclient.utils.Logger;
import coopnetclient.utils.RoomData;
import coopnetclient.utils.settings.Settings;
import coopnetclient.utils.gamedatabase.GameDatabase;
import coopnetclient.utils.gamedatabase.GameSetting;
import coopnetclient.utils.launcher.launchhandlers.DosboxLaunchHandler;
import coopnetclient.utils.launcher.launchhandlers.JDPlayLaunchHandler;
import coopnetclient.utils.launcher.launchhandlers.LaunchHandler;
import coopnetclient.utils.launcher.launchhandlers.ParameterLaunchHandler;
import coopnetclient.utils.launcher.launchinfos.DosboxLaunchInfo;
import coopnetclient.utils.launcher.launchinfos.DirectPlayLaunchInfo;
import coopnetclient.utils.launcher.launchinfos.LaunchInfo;
import coopnetclient.utils.launcher.launchinfos.ParameterLaunchInfo;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public class Launcher {

    private static boolean isInitialized;

    private static LaunchInfo launchedGameInfo;

    private static LaunchHandler launchHandler;

    private static SwingWorker delayedReinitThread;

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static boolean isPlaying() {
        return launchedGameInfo != null;
    }

    public static boolean isPlayingInstantLaunch() {
        return launchedGameInfo != null && launchedGameInfo.getRoomData().isInstant();
    }

    public static String getLaunchedGame() {
        if (launchedGameInfo == null) {
            return null;
        }
        return launchedGameInfo.getRoomData().getChannel();
    }

    public static void initialize(LaunchInfo launchInfo) {
        if (launchInfo == null) {
            throw new IllegalArgumentException("launchInfo must not be null!");
        }
        if (delayedReinitThread != null) {
            delayedReinitThread.cancel(true);
        }
        if (!isPlaying()) {
            if (launchInfo instanceof DirectPlayLaunchInfo) {
                launchHandler = new JDPlayLaunchHandler();
            } else if (launchInfo instanceof ParameterLaunchInfo) {
                launchHandler = new ParameterLaunchHandler();
            } else if (launchInfo instanceof DosboxLaunchInfo) {
                launchHandler = new DosboxLaunchHandler();
            }
            TempGameSettings.initalizeGameSettings(launchInfo.getRoomData().getChannel(), launchInfo.getRoomData().getModName());
            synchronized (launchHandler) {
                isInitialized = launchHandler.initialize(launchInfo);
                if (TabOrganizer.getRoomPanel() != null) {
                    int numSettings = GameDatabase.getGameSettings(launchInfo.getRoomData().getChannel(), launchInfo.getRoomData().getModName()).size();
                    if (isInitialized && numSettings == 0) {
                        TabOrganizer.getRoomPanel().initDone();
                    } else {
                        TabOrganizer.getRoomPanel().initDoneReadyDisabled();
                    }
                }
            }
            if (launchInfo instanceof ParameterLaunchInfo || launchInfo instanceof DosboxLaunchInfo) {
                if (!launchInfo.getRoomData().isInstant() && TabOrganizer.getRoomPanel() != null && GameDatabase.getGameSettings(launchInfo.getRoomData().getChannel(), launchInfo.getRoomData().getModName()).size() > 0) {
                    FrameOrganizer.openGameSettingsFrame(launchInfo.getRoomData());
                }
            }
        } else {
            determineInitializeActionWhenAlreadyPlaying(launchInfo);
        }
    }

    private static void determineInitializeActionWhenAlreadyPlaying(LaunchInfo curLaunchInfo) {
        if (curLaunchInfo.getRoomData().isInstant()) {
        } else if (launchedGameInfo.getRoomData().isHost() && curLaunchInfo.getRoomData().isHost() || curLaunchInfo.getRoomData().getRoomID() == (launchedGameInfo.getRoomData().getRoomID())) {
            if (TabOrganizer.getRoomPanel() != null) {
                TabOrganizer.getRoomPanel().initDone();
            }
        } else {
            if (TabOrganizer.getRoomPanel() != null) {
                TabOrganizer.getRoomPanel().displayDelayedReinit();
                delayedReinitThread = new SwingWorker() {

                    RoomPanel roomPanelToDelayReinitOn = TabOrganizer.getRoomPanel();

                    @Override
                    protected Object doInBackground() throws Exception {
                        if (roomPanelToDelayReinitOn != null) {
                            while (isPlaying()) {
                                Thread.sleep(1000);
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        if (!isCancelled() && roomPanelToDelayReinitOn != null && roomPanelToDelayReinitOn.equals(TabOrganizer.getRoomPanel())) {
                            roomPanelToDelayReinitOn.displayReInit();
                            roomPanelToDelayReinitOn.initLauncher();
                        }
                    }
                };
                delayedReinitThread.execute();
            }
        }
    }

    public static boolean predictSuccessfulLaunch() {
        if (isPlaying()) {
            return true;
        } else if (!isInitialized) {
            Logger.log(LogTypes.LAUNCHER, "predictSuccessfulLaunch() called while Launcher was not initialized!");
            return false;
        } else {
            synchronized (launchHandler) {
                return launchHandler.predictSuccessfulLaunch();
            }
        }
    }

    public static void launch() {
        if (isInitialized()) {
            for (int i = 0; TabOrganizer.getChannelPanel(i) != null; i++) {
                TabOrganizer.getChannelPanel(i).disableButtons();
            }
            synchronized (launchHandler) {
                launchedGameInfo = launchHandler.getLaunchInfo();
                if (Settings.getSleepEnabled()) {
                    Globals.setSleepModeStatus(true);
                }
                boolean launchResult = launchHandler.launch();
                Globals.setSleepModeStatus(false);
                boolean doPrint = true;
                if (launchHandler instanceof JDPlayLaunchHandler) {
                    JDPlayLaunchHandler handler = (JDPlayLaunchHandler) launchHandler;
                    if (handler.isSearchAborted()) {
                        handler.resetAbortSearchFlag();
                        doPrint = false;
                    }
                }
                if (doPrint) {
                    if (!launchResult) {
                        FrameOrganizer.getClientFrame().printSystemMessage("Launch failed, maybe the game is not setup properly or a process closed unexpectedly!", false);
                    } else {
                        FrameOrganizer.getClientFrame().printSystemMessage("Game closed.", false);
                    }
                }
                launchedGameInfo = null;
                for (int i = 0; TabOrganizer.getChannelPanel(i) != null; i++) {
                    TabOrganizer.getChannelPanel(i).enableButtons();
                }
            }
        } else {
            throw new IllegalStateException("The game has to be initialized before launching it!");
        }
    }

    public static void deInitialize() {
        if (launchHandler != null && launchHandler instanceof JDPlayLaunchHandler) {
            JDPlayLaunchHandler handler = (JDPlayLaunchHandler) launchHandler;
            handler.abortSearch();
        }
        isInitialized = false;
        launchHandler = null;
    }

    public static void updatePlayerName() {
        LaunchHandler toWorkOn = launchHandler;
        if (toWorkOn != null) {
            synchronized (toWorkOn) {
                toWorkOn.updatePlayerName();
            }
        }
    }

    public static boolean initInstantLaunch(RoomData roomData) {
        FrameOrganizer.getClientFrame().printSystemMessage("Initializing game ...", false);
        LaunchInfo launchInfo;
        LaunchMethods method = GameDatabase.getLaunchMethod(roomData.getChannel(), roomData.getModName());
        if (method == LaunchMethods.PARAMETER) {
            launchInfo = new ParameterLaunchInfo(roomData);
        } else if (method == LaunchMethods.DOS) {
            launchInfo = new DosboxLaunchInfo(roomData);
        } else {
            throw new IllegalArgumentException("You can't instantlaunch from " + method.toString() + " channel! GameName: " + roomData.getChannel() + " ModName: " + roomData.getModName());
        }
        if (isPlaying()) {
            boolean showJOptionPane = false;
            if (!roomData.getChannel().equals(launchedGameInfo.getRoomData().getChannel())) {
                showJOptionPane = true;
            } else if (!roomData.getIP().equals(launchedGameInfo.getRoomData().getIP())) {
                showJOptionPane = true;
            } else if (!(roomData.getModName() == null && launchedGameInfo.getRoomData().getModName() == null)) {
                if (!roomData.getModName().equals(launchedGameInfo.getRoomData().getModName())) {
                    showJOptionPane = true;
                }
            }
            if (showJOptionPane) {
                while (isPlaying()) {
                    int option = JOptionPane.showConfirmDialog(null, "<html>Coopnet has detected that the game \"<b>" + launchHandler.getLaunchInfo().getBinaryName() + "</b>\" is already running.<br>" + "You have to <b>close the game</b> to proceed launching.<br>" + "<br>" + "Press ok to retry or press cancel to abort the launch.", "WARNING: Another game is already running", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (option == JOptionPane.CANCEL_OPTION) {
                        return false;
                    }
                }
            }
        }
        Launcher.initialize(launchInfo);
        if (!Launcher.isInitialized()) {
            Protocol.closeRoom();
            Protocol.gameClosed(roomData.getChannel());
            TabOrganizer.getChannelPanel(roomData.getChannel()).enableButtons();
        }
        return Launcher.isInitialized();
    }

    public static void instantLaunch() {
        instantLaunch(false);
    }

    public static void instantLaunch(boolean launchClickedFromGameSettingsFrame) {
        if (Launcher.isInitialized()) {
            final String channel = launchHandler.getLaunchInfo().getRoomData().getChannel();
            TabOrganizer.getChannelPanel(channel).disableButtons();
            if (launchHandler.getLaunchInfo().getRoomData().isHost() && (isPlaying() || launchHandler.processExists())) {
                new Thread() {

                    @Override
                    public void run() {
                        String channelcopy = channel;
                        int ret = JOptionPane.showConfirmDialog(null, "<html>Coopnet has detected that the game \"<b>" + launchHandler.getLaunchInfo().getBinaryName() + "</b>\" is already running.<br>" + "Please make sure the other players can <b>connect to a running server</b> there<br>" + "or <b>close the game</b> before confirming this message.<br>" + "<br>" + "<br>If the game is still running after you have confirmed this message," + "<br>Coopnet will create the room without launching your game.", "WARNING: Game is already running", JOptionPane.OK_CANCEL_OPTION);
                        if (ret == JOptionPane.OK_OPTION) {
                            if (launchHandler.processExists()) {
                                Protocol.createRoom(launchHandler.getLaunchInfo().getRoomData());
                                while (launchHandler != null && launchHandler.processExists()) {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException ex) {
                                    }
                                }
                                Protocol.gameClosed(channelcopy);
                            }
                        } else {
                            Launcher.deInitialize();
                            TabOrganizer.getChannelPanel(channelcopy).enableButtons();
                        }
                    }
                }.start();
            } else if (!launchHandler.getLaunchInfo().getRoomData().isHost() && !launchClickedFromGameSettingsFrame) {
                FrameOrganizer.openGameSettingsFrame(launchHandler.getLaunchInfo().getRoomData());
            } else {
                if (launchHandler.getLaunchInfo().getRoomData().isHost()) {
                    Protocol.createRoom(launchHandler.getLaunchInfo().getRoomData());
                    String mv = TempGameSettings.getGameSettingValue("map");
                    if (mv != null && mv.length() > 0) {
                        Protocol.sendSetting("map", TempGameSettings.getGameSettingValue("map"));
                    }
                    for (GameSetting gs : TempGameSettings.getGameSettings()) {
                        Protocol.sendSetting(gs.getName(), gs.getValue());
                    }
                }
                Launcher.launch();
                Launcher.deInitialize();
                Protocol.gameClosed(channel);
                TabOrganizer.getChannelPanel(channel).enableButtons();
            }
        }
    }
}
