package coopnetclient.utils.settings;

import coopnetclient.Globals;
import coopnetclient.enums.OperatingSystems;
import coopnetclient.protocol.out.Protocol;
import coopnetclient.utils.RegistryReader;
import coopnetclient.utils.gamedatabase.GameDatabase;
import java.awt.Color;
import java.awt.event.KeyEvent;
import passwordencrypter.PasswordEncrypter;

/**
 * stores/loads the clients settings
 * 
 *  - Variables for any setting name and default value
 *  - everything static only!
 *  - getters and setters for each variable
 *  - getters and setters automatically cast to the expected type
 *  - getters load default value, if error occurs and save the default value to restore file integrity
 *  - each setter saves the file // maybe change that behaviour for performance tuning
 *
 *  this ensures easy integration of new settings or easy change of default values / file entry names
 *
 *  How to add new settings:
 *      - add entries to private fields (entry name and default value)
 *      - write setter and getter by using the read<TYPE>() and SettingsHelper.writeSetting() functions
 *
 */
public final class Settings {

    private static final String lastValidServerIP = "LastValidServerIP";

    private static final String def_lastValidServerIP = "80.190.240.58";

    private static final String lastValidServerPort = "LastValidServerPort";

    private static final int def_lastValidServerPort = 6667;

    private static final String recieveDest = "FileDestination";

    private static String def_recievedest;

    private static final String sleepEnabled = "SleepModeEnabled";

    private static final boolean def_sleepEnabled = true;

    private static final String firstRun = "FirstRun";

    private static final boolean def_firstRun = true;

    private static final String homeChannel = "HomeChannel";

    private static final String def_homeChannel = "Welcome";

    private static final String autoLogin = "AutoLogin";

    private static final boolean def_autoLogin = false;

    private static final String debugMode = "DebugMode";

    private static final boolean def_debugMode = false;

    private static final String selectedLookAndFeel = "SelectedLAF";

    private static final String def_selectedLookAndFeel = "Metal";

    private static final String useNativeLookAndFeel = "UseNativeLAF";

    private static final boolean def_useNativeLookAndFeel = true;

    private static final String bgColor = "BackgroundColor";

    private static final Color def_bgColor = new Color(240, 240, 240);

    private static final String fgColor = "ForegroundColor";

    private static final Color def_fgColor = Color.BLACK;

    private static final String yourUsernameColor = "YourUsernameColor";

    private static final Color def_UsernameColor = new Color(255, 153, 0);

    private static final String selectionColor = "SelectionColor";

    private static final Color def_SelectionColor = new Color(200, 200, 200);

    private static final String otherUsernamesColor = "OtherUsernamesColor";

    private static final Color def_otherUsernamesColor = new Color(0, 51, 255);

    private static final String friendUsernameColor = "FriendUsernameColor";

    private static final Color def_friendUsernameColor = Color.GREEN.darker();

    private static final String systemMessageColor = "SystemMessageColor";

    private static final Color def_systemMessageColor = new Color(200, 0, 0);

    private static final String whisperMessageColor = "WhisperMessageColor";

    private static final Color def_whisperMessageColor = new Color(0, 153, 204);

    private static final String friendMessageColor = "FriendMessageColor";

    private static final Color def_friendMessageColor = Color.GREEN.darker();

    private static final String nameStyle = "NameStyle";

    private static final String def_nameStyle = "Monospaced";

    private static final String nameSize = "NameSize";

    private static final int def_nameSize = 12;

    private static final String messageStyle = "MessageStyle";

    private static final String def_messageStyle = "Monospaced";

    private static final String messageSize = "MessageSize";

    private static final int def_messageSize = 12;

    private static final String colorizeBody = "ColorizeBody";

    private static final boolean def_colorizeBody = false;

    private static final String colorizeText = "ColorizeText";

    private static final boolean def_colorizeText = true;

    private static final String lastLoginName = "LastLoginName";

    private static final String def_lastLoginName = "";

    private static final String lastLoginPassword = "Style";

    private static final String def_lastLoginPassword = "";

    private static final String userMessageColor = "UserMessageColor";

    private static final Color def_userMessageColor = Color.BLACK;

    private static final String SoundEnabled = "SoundEnabled";

    private static final boolean def_soundEnabled = true;

    private static final String TimeStamps = "TimeStamps";

    private static final boolean def_timeStamps = false;

    private static final String mainFrameMaximised = "MainFrameMaximised";

    private static final int def_mainFrameMaximised = javax.swing.JFrame.NORMAL;

    private static final String mainFrameWidth = "MainFrameWidth";

    private static final int def_mainFrameWidth = 600;

    private static final String mainFrameHeight = "MainFrameHeight";

    private static final int def_mainFrameHeight = 400;

    private static final String channelVerticalSPPosition = "ChannelVerticalSPPosition";

    private static final int def_channelVerticalSPPosition = 150;

    private static final String channelChatHorizontalSPPosition = "ChannelChatHorizontalSPPosition";

    private static final int def_channelChatHorizontalSPPosition = 369;

    private static final String channelChatVerticalSPPosition = "ChannelChatVerticalSPPosition";

    private static final int def_channelChatVerticalSPPosition = 135;

    private static final String wineCommand = "WineCommand";

    private static final String def_wineComamnd = "wine";

    private static final String fileTransferPort = "FiletransferPort";

    private static final int def_fileTransferPort = 2400;

    private static final String quickPanelPostionIsLeft = "QuickPanelPositionIsLeft";

    private static final boolean def_quickPanelPostionIsLeft = true;

    private static final String quickPanelDividerWidth = "QuckPanelDividerWidth";

    private static final int def_quickPanelDividerWidth = 5;

    private static final String contactStatusChangeTextNotification = "ContactStatusChangeTextNotification";

    private static final boolean def_contactStatusChangeTextNotification = true;

    private static final String contactStatusChangeSoundNotification = "ContactStatusChangeSoundNotification";

    private static final boolean def_contactStatusChangeSoundNotification = true;

    private static final String quickPanelToggleBarWidth = "QuickPanelToggleBarWidth";

    private static final int def_quickPanelToggleBarWidth = 10;

    private static final String trayIconEnabled = "TrayIcon";

    private static final boolean def_trayIconEnabled = true;

    private static final String launchHotKeyMask = "HotKeyMask";

    private static final int def_launchHotKeyMask = 10;

    private static final String launchHotKey = "HotKey";

    private static final int def_launchHotKey = KeyEvent.VK_L;

    private static final String multiChannel = "MultiChannel";

    private static final boolean def_multiChannel = true;

    private static final String showOfflineContacts = "ShowOfflineContacts";

    private static final boolean def_showOfflineContacts = false;

    private static final String quickPanelIconSizeIsBig = "QuickPanelIconSizeIsBig";

    private static final boolean def_quickPanelIconSizeIsBig = true;

    private static final String rememberMainFrameSize = "RememberMainFrameSize";

    private static final boolean def_rememberMainFrameSize = false;

    private static final String logUserActivity = "LogUserActicvity";

    private static final boolean def_logUserActivity = true;

    private static final String DOSBoxExecutable = "DOSBox-Executable";

    private static final String def_DOSEmulatorExecutable = "";

    private static final String DOSBoxFullscreen = "DOSBox-Fullscreen";

    private static final boolean def_DOSBoxFullscreen = false;

    private static final String SHOW_MINIMIZE_TO_TRAY_HINT = "ShowMinimizeToTrayHint";

    private static final boolean DEF_SHOW_MINIMIZE_TO_TRAY_HINT = true;

    private Settings() {
    }

    public static void init() {
        if (Globals.getOperatingSystem() != OperatingSystems.LINUX) {
            String recvdir = RegistryReader.read("HKEY_CURRENT_USER\\Software\\Microsoft\\Windows" + "\\CurrentVersion\\Explorer\\Shell Folders\\Desktop");
            if (recvdir == null) {
                recvdir = System.getenv("HOMEPATH");
            }
            def_recievedest = recvdir;
        } else {
            final String home = System.getenv("HOME");
            def_recievedest = home;
        }
    }

    public static int getLaunchHotKey() {
        return SettingsHelper.readInteger(launchHotKey, def_launchHotKey);
    }

    public static void setLaunchHotKey(int key) {
        SettingsHelper.writeSetting(launchHotKey, String.valueOf(key));
    }

    public static int getLaunchHotKeyMask() {
        return SettingsHelper.readInteger(launchHotKeyMask, def_launchHotKeyMask);
    }

    public static void setLaunchHotKeymask(int keyMask) {
        SettingsHelper.writeSetting(launchHotKeyMask, String.valueOf(keyMask));
    }

    public static boolean getTrayIconEnabled() {
        return SettingsHelper.readBoolean(trayIconEnabled, def_trayIconEnabled);
    }

    public static void setTrayIconEnabled(boolean status) {
        SettingsHelper.writeSetting(trayIconEnabled, String.valueOf(status));
    }

    public static boolean getQuickPanelPostionisLeft() {
        return SettingsHelper.readBoolean(quickPanelPostionIsLeft, def_quickPanelPostionIsLeft);
    }

    public static void setQuickPanelPostionisLeft(boolean status) {
        SettingsHelper.writeSetting(quickPanelPostionIsLeft, String.valueOf(status));
    }

    public static boolean getLogUserActivity() {
        return SettingsHelper.readBoolean(logUserActivity, def_logUserActivity);
    }

    public static void setLogUserActivity(boolean status) {
        SettingsHelper.writeSetting(logUserActivity, String.valueOf(status));
    }

    public static int getQuickPanelToggleBarWidth() {
        return SettingsHelper.readInteger(quickPanelToggleBarWidth, def_quickPanelToggleBarWidth);
    }

    public static void setQuickPanelToggleBarWidth(int width) {
        SettingsHelper.writeSetting(quickPanelToggleBarWidth, String.valueOf(width));
    }

    public static int getQuickPanelDividerWidth() {
        return SettingsHelper.readInteger(quickPanelDividerWidth, def_quickPanelDividerWidth);
    }

    public static void setQuickPanelDividerWidth(int width) {
        SettingsHelper.writeSetting(quickPanelDividerWidth, String.valueOf(width));
    }

    public static boolean getContactStatusChangeTextNotification() {
        return SettingsHelper.readBoolean(contactStatusChangeTextNotification, def_contactStatusChangeTextNotification);
    }

    public static void setContactStatusChangeTextNotification(boolean status) {
        SettingsHelper.writeSetting(contactStatusChangeTextNotification, String.valueOf(status));
    }

    public static boolean getContactStatusChangeSoundNotification() {
        return SettingsHelper.readBoolean(contactStatusChangeSoundNotification, def_contactStatusChangeSoundNotification);
    }

    public static void setContactStatusChangeSoundNotification(boolean status) {
        SettingsHelper.writeSetting(contactStatusChangeSoundNotification, String.valueOf(status));
    }

    /**
     *  public getters and setters used by other classes
     */
    public static boolean getFirstRun() {
        return SettingsHelper.readBoolean(firstRun, def_firstRun);
    }

    public static void setFirstRun(boolean status) {
        SettingsHelper.writeSetting(firstRun, String.valueOf(status));
    }

    public static boolean getSleepEnabled() {
        return SettingsHelper.readBoolean(sleepEnabled, def_sleepEnabled);
    }

    public static void setSleepenabled(boolean enabled) {
        SettingsHelper.writeSetting(sleepEnabled, String.valueOf(enabled));
    }

    public static int getMainFrameMaximised() {
        return SettingsHelper.readInteger(mainFrameMaximised, def_mainFrameMaximised);
    }

    public static void setMainFrameMaximised(int status) {
        SettingsHelper.writeSetting(mainFrameMaximised, String.valueOf(status));
    }

    public static int getMainFrameWidth() {
        return SettingsHelper.readInteger(mainFrameWidth, def_mainFrameWidth);
    }

    public static void setMainFrameWidth(int width) {
        SettingsHelper.writeSetting(mainFrameWidth, String.valueOf(width));
    }

    public static int getMainFrameHeight() {
        return SettingsHelper.readInteger(mainFrameHeight, def_mainFrameHeight);
    }

    public static void setMainFrameHeight(int height) {
        SettingsHelper.writeSetting(mainFrameHeight, String.valueOf(height));
    }

    public static int getChannelVerticalSPPosition() {
        return SettingsHelper.readInteger(channelVerticalSPPosition, def_channelVerticalSPPosition);
    }

    public static void setChannelVerticalSPPosition(int position) {
        SettingsHelper.writeSetting(channelVerticalSPPosition, String.valueOf(position));
    }

    public static int getChannelChatHorizontalSPPosition() {
        return SettingsHelper.readInteger(channelChatHorizontalSPPosition, def_channelChatHorizontalSPPosition);
    }

    public static void setChannelChatHorizontalSPPosition(int position) {
        SettingsHelper.writeSetting(channelChatHorizontalSPPosition, String.valueOf(position));
    }

    public static int getChannelChatVerticalSPPosition() {
        return SettingsHelper.readInteger(channelChatVerticalSPPosition, def_channelChatVerticalSPPosition);
    }

    public static void setChannelChatVerticalSPPosition(int position) {
        SettingsHelper.writeSetting(channelChatVerticalSPPosition, String.valueOf(position));
    }

    public static boolean getSoundEnabled() {
        return SettingsHelper.readBoolean(SoundEnabled, def_soundEnabled);
    }

    public static void setSoundEnabled(boolean bool) {
        SettingsHelper.writeSetting(SoundEnabled, String.valueOf(bool));
    }

    public static boolean getTimeStampEnabled() {
        return SettingsHelper.readBoolean(TimeStamps, def_timeStamps);
    }

    public static void setTimeStampEnabled(boolean bool) {
        SettingsHelper.writeSetting(TimeStamps, String.valueOf(bool));
    }

    public static String getLastValidServerIP() {
        return SettingsHelper.readString(lastValidServerIP, def_lastValidServerIP);
    }

    public static void setLastValidServerIP(String ip) {
        SettingsHelper.writeSetting(lastValidServerIP, ip);
    }

    public static int getLastValidServerPort() {
        return SettingsHelper.readInteger(lastValidServerPort, def_lastValidServerPort);
    }

    public static void setLastValidServerPort(int port) {
        SettingsHelper.writeSetting(lastValidServerPort, String.valueOf(port));
    }

    public static int getFiletTansferPort() {
        return SettingsHelper.readInteger(fileTransferPort, def_fileTransferPort);
    }

    public static void setFiletTansferPort(int port) {
        SettingsHelper.writeSetting(fileTransferPort, String.valueOf(port));
    }

    public static boolean getAutoLogin() {
        return SettingsHelper.readBoolean(autoLogin, def_autoLogin);
    }

    public static void setAutoLogin(boolean bool) {
        SettingsHelper.writeSetting(autoLogin, String.valueOf(bool));
    }

    public static String getHomeChannel() {
        String ch = SettingsHelper.readString(homeChannel, def_homeChannel);
        if (ch.length() == 3) {
            return GameDatabase.getGameName(ch);
        } else {
            return ch;
        }
    }

    public static void setHomeChannel(String channel) {
        SettingsHelper.writeSetting(homeChannel, GameDatabase.getIDofGame(channel));
    }

    public static String getDOSBoxExecutable() {
        return SettingsHelper.readString(DOSBoxExecutable, def_DOSEmulatorExecutable);
    }

    public static void setDOSBoxExecutable(String path) {
        SettingsHelper.writeSetting(DOSBoxExecutable, path);
    }

    public static boolean getDOSBoxFullscreen() {
        return SettingsHelper.readBoolean(DOSBoxFullscreen, def_DOSBoxFullscreen);
    }

    public static void setDOSBoxFullscreen(boolean value) {
        SettingsHelper.writeSetting(DOSBoxFullscreen, String.valueOf(value));
    }

    public static boolean getDebugMode() {
        return SettingsHelper.readBoolean(debugMode, def_debugMode);
    }

    public static void setDebugMode(boolean bool) {
        SettingsHelper.writeSetting(debugMode, String.valueOf(bool));
    }

    public static String getSelectedLookAndFeel() {
        return SettingsHelper.readString(selectedLookAndFeel, def_selectedLookAndFeel);
    }

    public static void setSelectedLookAndFeel(String string) {
        SettingsHelper.writeSetting(selectedLookAndFeel, string);
    }

    public static boolean getUseNativeLookAndFeel() {
        return SettingsHelper.readBoolean(useNativeLookAndFeel, def_useNativeLookAndFeel);
    }

    public static void setUseNativeLookAndFeel(boolean bool) {
        SettingsHelper.writeSetting(useNativeLookAndFeel, String.valueOf(bool));
    }

    public static Color getBackgroundColor() {
        return SettingsHelper.readColor(bgColor, def_bgColor);
    }

    public static void setBackgroundColor(Color color) {
        SettingsHelper.writeSetting(bgColor, String.valueOf(color.getRGB()));
    }

    public static Color getSelectionColor() {
        return SettingsHelper.readColor(selectionColor, def_SelectionColor);
    }

    public static void setSelectionColor(Color color) {
        SettingsHelper.writeSetting(selectionColor, String.valueOf(color.getRGB()));
    }

    public static Color getForegroundColor() {
        return SettingsHelper.readColor(fgColor, def_fgColor);
    }

    public static void setForegroundColor(Color color) {
        SettingsHelper.writeSetting(fgColor, String.valueOf(color.getRGB()));
    }

    public static Color getYourUsernameColor() {
        return SettingsHelper.readColor(yourUsernameColor, def_UsernameColor);
    }

    public static void setYourUsernameColor(Color color) {
        SettingsHelper.writeSetting(yourUsernameColor, String.valueOf(color.getRGB()));
    }

    public static Color getOtherUsernamesColor() {
        return SettingsHelper.readColor(otherUsernamesColor, def_otherUsernamesColor);
    }

    public static void setOtherUsernamesColor(Color color) {
        SettingsHelper.writeSetting(otherUsernamesColor, String.valueOf(color.getRGB()));
    }

    public static Color getSystemMessageColor() {
        return SettingsHelper.readColor(systemMessageColor, def_systemMessageColor);
    }

    public static void setSystemMessageColor(Color color) {
        SettingsHelper.writeSetting(systemMessageColor, String.valueOf(color.getRGB()));
    }

    public static Color getWhisperMessageColor() {
        return SettingsHelper.readColor(whisperMessageColor, def_whisperMessageColor);
    }

    public static void setWhisperMessageColor(Color color) {
        SettingsHelper.writeSetting(whisperMessageColor, String.valueOf(color.getRGB()));
    }

    public static String getNameStyle() {
        return SettingsHelper.readString(nameStyle, def_nameStyle);
    }

    public static void setNameStyle(String style) {
        SettingsHelper.writeSetting(nameStyle, style);
    }

    public static int getNameSize() {
        return SettingsHelper.readInteger(nameSize, def_nameSize);
    }

    public static void setNameSize(int size) {
        SettingsHelper.writeSetting(nameSize, String.valueOf(size));
    }

    public static String getMessageStyle() {
        return SettingsHelper.readString(messageStyle, def_messageStyle);
    }

    public static void setMessageStyle(String style) {
        SettingsHelper.writeSetting(messageStyle, style);
    }

    public static int getMessageSize() {
        return SettingsHelper.readInteger(messageSize, def_messageSize);
    }

    public static void setMessageSize(int size) {
        SettingsHelper.writeSetting(messageSize, String.valueOf(size));
    }

    public static Color getFriendMessageColor() {
        return SettingsHelper.readColor(friendMessageColor, def_friendMessageColor);
    }

    public static void setFriendMessageColor(Color color) {
        SettingsHelper.writeSetting(friendMessageColor, String.valueOf(color.getRGB()));
    }

    public static Color getFriendUsernameColor() {
        return SettingsHelper.readColor(friendUsernameColor, def_friendUsernameColor);
    }

    public static void setFriendUsernameColor(Color color) {
        SettingsHelper.writeSetting(friendUsernameColor, String.valueOf(color.getRGB()));
    }

    public static Color getUserMessageColor() {
        return SettingsHelper.readColor(userMessageColor, def_userMessageColor);
    }

    public static void setUserMessageColor(Color color) {
        SettingsHelper.writeSetting(userMessageColor, String.valueOf(color.getRGB()));
    }

    public static boolean getColorizeBody() {
        return SettingsHelper.readBoolean(colorizeBody, def_colorizeBody);
    }

    public static void setColorizeBody(boolean bool) {
        SettingsHelper.writeSetting(colorizeBody, String.valueOf(bool));
    }

    public static boolean getColorizeText() {
        return SettingsHelper.readBoolean(colorizeText, def_colorizeText);
    }

    public static void setColorizeText(boolean bool) {
        SettingsHelper.writeSetting(colorizeText, String.valueOf(bool));
    }

    public static String getRecieveDestination() {
        return SettingsHelper.readString(recieveDest, def_recievedest);
    }

    public static void setRecieveDestination(String path) {
        SettingsHelper.writeSetting(recieveDest, path);
    }

    public static String getWineCommand() {
        return SettingsHelper.readString(wineCommand, def_wineComamnd);
    }

    public static void setWineCommand(String path) {
        SettingsHelper.writeSetting(wineCommand, path);
    }

    public static String getLastLoginName() {
        return SettingsHelper.readString(lastLoginName, def_lastLoginName);
    }

    public static void setLastLoginName(String name) {
        SettingsHelper.writeSetting(lastLoginName, name);
    }

    public static String getLastLoginPassword() {
        return PasswordEncrypter.decodePassword(SettingsHelper.readString(lastLoginPassword, def_lastLoginPassword));
    }

    public static void setLastLoginPassword(String pw) {
        SettingsHelper.writeSetting(lastLoginPassword, PasswordEncrypter.encodePassword(PasswordEncrypter.encryptPassword(pw)));
    }

    public static void setMultiChannel(boolean enabled) {
        SettingsHelper.writeSetting(multiChannel, String.valueOf(enabled));
    }

    public static boolean getMultiChannel() {
        return SettingsHelper.readBoolean(multiChannel, def_multiChannel);
    }

    public static boolean isQuickPanelIconSizeBig() {
        return SettingsHelper.readBoolean(quickPanelIconSizeIsBig, def_quickPanelIconSizeIsBig);
    }

    public static void setIsQuickPanelIconSizeBig(boolean bool) {
        SettingsHelper.writeSetting(quickPanelIconSizeIsBig, String.valueOf(bool));
    }

    public static void setShowOfflineContacts(boolean enabled) {
        boolean refreshContacts = !Settings.getShowOfflineContacts() && enabled;
        SettingsHelper.writeSetting(showOfflineContacts, String.valueOf(enabled));
        if (refreshContacts) {
            Protocol.refreshContacts();
        } else {
            Globals.getContactList().updateShowOfflineContacts();
        }
    }

    public static boolean getShowOfflineContacts() {
        return SettingsHelper.readBoolean(showOfflineContacts, def_showOfflineContacts);
    }

    public static void setRememberMainFrameSize(boolean enabled) {
        SettingsHelper.writeSetting(rememberMainFrameSize, String.valueOf(enabled));
    }

    public static boolean getRememberMainFrameSize() {
        return SettingsHelper.readBoolean(rememberMainFrameSize, def_rememberMainFrameSize);
    }

    public static void setShowMinimizeToTrayHint(boolean value) {
        SettingsHelper.writeSetting(SHOW_MINIMIZE_TO_TRAY_HINT, String.valueOf(value));
    }

    public static boolean getShowMinimizeToTrayHint() {
        return SettingsHelper.readBoolean(SHOW_MINIMIZE_TO_TRAY_HINT, DEF_SHOW_MINIMIZE_TO_TRAY_HINT);
    }
}
