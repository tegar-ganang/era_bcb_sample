package coopnetclient.utils;

import coopnetclient.Globals;
import coopnetclient.enums.OperatingSystems;
import coopnetclient.protocol.out.Protocol;
import coopnetclient.utils.gamedatabase.GameDatabase;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.Vector;
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
 *      - write setter and getter by using the read<TYPE>() and writeSetting() functions
 *
 */
public class Settings {

    private static final Properties data;

    private static final String SETTINGS_DIR;

    private static final String FAVOURITES_FILE;

    private static final String SETTINGS_FILE;

    private static final Vector<String> favourites;

    static {
        if (Globals.getOperatingSystem() == OperatingSystems.WINDOWS) {
            SETTINGS_DIR = System.getenv("APPDATA") + "/Coopnet";
            String recvdir = RegistryReader.read("HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders\\Desktop");
            if (recvdir == null) {
                recvdir = System.getenv("HOMEPATH");
            }
            def_recievedest = recvdir;
        } else {
            SETTINGS_DIR = System.getenv("HOME") + "/.coopnet";
            def_recievedest = System.getenv("HOME");
        }
        FAVOURITES_FILE = SETTINGS_DIR + "/favourites";
        SETTINGS_FILE = SETTINGS_DIR + "/settings";
        data = new java.util.Properties();
        load();
        favourites = new Vector<String>();
        loadFavourites();
    }

    private static final String lastValidServerIP = "LastValidServerIP", lastValidServerPort = "LastValidServerPort", recieveDest = "FileDestination", sleepEnabled = "SleepModeEnabled", firstRun = "FirstRun", homeChannel = "HomeChannel", autoLogin = "AutoLogin", debugMode = "DebugMode", selectedLookAndFeel = "SelectedLAF", useNativeLookAndFeel = "UseNativeLAF", bgColor = "BackgroundColor", fgColor = "ForegroundColor", yourUsernameColor = "YourUsernameColor", selectionColor = "SelectionColor", otherUsernamesColor = "OtherUsernamesColor", friendUsernameColor = "FriendUsernameColor", systemMessageColor = "SystemMessageColor", whisperMessageColor = "WhisperMessageColor", friendMessageColor = "FriendMessageColor", nameStyle = "NameStyle", nameSize = "NameSize", messageStyle = "MessageStyle", messageSize = "MessageSize", colorizeBody = "ColorizeBody", colorizeText = "ColorizeText", lastLoginName = "LastLoginName", lastLoginPassword = "Style", userMessageColor = "UserMessageColor", SoundEnabled = "SoundEnabled", TimeStamps = "TimeStamps", mainFrameMaximised = "MainFrameMaximised", mainFrameWidth = "MainFrameWidth", mainFrameHeight = "MainFrameHeight", channelVerticalSPPosition = "ChannelVerticalSPPosition", channelChatHorizontalSPPosition = "ChannelChatHorizontalSPPosition", channelChatVerticalSPPosition = "ChannelChatVerticalSPPosition", wineCommand = "WineCommand", fileTransferPort = "FiletransferPort", quickPanelPostionisLeft = "QuickPanelPositionIsLeft", quickPanelDividerWidth = "QuckPanelDividerWidth", contactStatusChangeTextNotification = "ContactStatusChangeTextNotification", contactStatusChangeSoundNotification = "ContactStatusChangeSoundNotification", quickPanelToggleBarWidth = "QuickPanelToggleBarWidth", trayIconEnabled = "TrayIcon", launchHotKeyMask = "HotKeyMask", launchHotKey = "HotKey", multiChannel = "MultiChannel", showOfflineContacts = "ShowOfflineContacts", quickPanelIconSizeIsBig = "QuickPanelIconSizeIsBig", rememberMainFrameSize = "RememberMainFrameSize", logUserActivity = "LogUserActicvity";

    private static final String def_lastValidServerIP = "subes.dyndns.org";

    private static final int def_lastValidServerPort = 6667;

    private static final boolean def_firstRun = true;

    private static final boolean def_sleepEnabled = true;

    private static final boolean def_autoLogin = false;

    private static final boolean def_logUserActivity = true;

    private static final boolean def_debugMode = false;

    private static final String def_selectedLookAndFeel = "Metal";

    private static final boolean def_useNativeLookAndFeel = true;

    private static final Color def_bgColor = new Color(240, 240, 240);

    private static final Color def_fgColor = Color.BLACK;

    private static final Color def_yourUsernameColor = new Color(255, 153, 0);

    private static final Color def_otherUsernamesColor = new Color(0, 51, 255);

    private static final Color def_systemMessageColor = new Color(200, 0, 0);

    private static final Color def_whisperMessageColor = new Color(0, 153, 204);

    private static final Color def_userMessageColor = Color.BLACK;

    private static final Color def_friendUsernameColor = Color.GREEN.darker();

    private static final Color def_friendMessageColor = Color.GREEN.darker();

    private static final Color def_SelectionColor = new Color(200, 200, 200);

    private static final String def_nameStyle = "Monospaced";

    private static final String def_recievedest;

    private static final int def_nameSize = 12;

    private static final String def_messageStyle = "Monospaced";

    private static final String def_homeChannel = "Welcome";

    private static final int def_messageSize = 12;

    private static final boolean def_colorizeBody = false;

    private static final boolean def_colorizeText = true;

    private static final String def_lastLoginName = "";

    private static final String def_lastLoginPassword = "";

    private static final boolean def_soundEnabled = true;

    private static final boolean def_timeStamps = false;

    private static final boolean def_quickPanelPostionIsLeft = true;

    private static final int def_mainFrameMaximised = javax.swing.JFrame.NORMAL;

    private static final int def_mainFrameWidth = 600;

    private static final int def_mainFrameHeight = 400;

    private static final int def_channelVerticalSPPosition = 150;

    private static final int def_channelChatHorizontalSPPosition = 369;

    private static final int def_channelChatVerticalSPPosition = 135;

    private static final String def_wineComamnd = "wine";

    private static final int def_fileTransferPort = 2300;

    private static final int def_quickPanelDividerWidth = 5;

    private static final int def_quickPanelToggleBarWidth = 10;

    private static final boolean def_contactStatusChangeTextNotification = true;

    private static final boolean def_contactStatusChangeSoundNotification = true;

    private static final boolean def_trayIconEnabled = false;

    private static final int def_launchHotKeyMask = 10;

    private static final int def_launchHotKey = KeyEvent.VK_L;

    private static final boolean def_multiChannel = true;

    private static final boolean def_showOfflineContacts = false;

    private static final boolean def_quickPanelIconSizeIsBig = true;

    private static final boolean def_rememberMainFrameSize = false;

    public static void resetSettings() {
        data.clear();
        save();
        favourites.clear();
        saveFavourites();
    }

    /**
     * store the settings in options file
     */
    private static void save() {
        try {
            data.store(new FileOutputStream(SETTINGS_FILE), "Coopnet settings");
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Load the settings from the options file, or create one with default values if it doesn exist
     */
    private static void load() {
        try {
            if (!new File(SETTINGS_DIR).exists()) {
                new File(SETTINGS_DIR).mkdir();
            }
            data.load(new FileInputStream(SETTINGS_FILE));
        } catch (Exception ex) {
        }
    }

    private static String readString(String entry, String defaultValue) {
        String ret = data.getProperty(entry);
        if (ret == null) {
            writeSetting(entry, defaultValue);
            ret = defaultValue;
        }
        return ret;
    }

    private static int readInteger(String entry, int defaultValue) {
        boolean error = false;
        int ret = 0;
        String get = data.getProperty(entry);
        if (get != null) {
            try {
                ret = Integer.parseInt(get);
            } catch (NumberFormatException e) {
                error = true;
            }
        }
        if (error || get == null) {
            writeSetting(entry, String.valueOf(defaultValue));
            return defaultValue;
        }
        return ret;
    }

    private static float readFloat(String entry, float defaultValue) {
        boolean error = false;
        float ret = 0;
        String get = data.getProperty(entry);
        if (get != null) {
            try {
                ret = Float.parseFloat(get);
            } catch (NumberFormatException e) {
                error = true;
            }
        }
        if (error || get == null) {
            writeSetting(entry, String.valueOf(defaultValue));
            return defaultValue;
        }
        return ret;
    }

    private static boolean readBoolean(String entry, boolean defaultValue) {
        boolean error = false;
        boolean ret = false;
        String get = data.getProperty(entry);
        if (get != null) {
            if (get.equalsIgnoreCase("true")) {
                ret = true;
            } else if (get.equalsIgnoreCase("false")) {
                ret = false;
            } else {
                error = true;
            }
        }
        if (error || get == null) {
            writeSetting(entry, String.valueOf(defaultValue));
            return defaultValue;
        }
        return ret;
    }

    private static Color readColor(String entry, Color defaultValue) {
        boolean error = false;
        Color ret = null;
        try {
            ret = new Color(Integer.parseInt(data.getProperty(entry)));
        } catch (Exception e) {
            error = true;
        }
        if (error || ret == null) {
            ret = defaultValue;
            writeSetting(entry, String.valueOf(defaultValue.getRGB()));
        }
        return ret;
    }

    /**
     * settings writer used by the real setters
     */
    private static void writeSetting(String entry, String value) {
        data.setProperty(entry, value);
        save();
    }

    public static int getLaunchHotKey() {
        return readInteger(launchHotKey, def_launchHotKey);
    }

    public static void setLaunchHotKey(int key) {
        writeSetting(launchHotKey, String.valueOf(key));
    }

    public static int getLaunchHotKeyMask() {
        return readInteger(launchHotKeyMask, def_launchHotKeyMask);
    }

    public static void setLaunchHotKeymask(int keyMask) {
        writeSetting(launchHotKeyMask, String.valueOf(keyMask));
    }

    public static boolean getTrayIconEnabled() {
        return readBoolean(trayIconEnabled, def_trayIconEnabled);
    }

    public static void setTrayIconEnabled(boolean status) {
        writeSetting(trayIconEnabled, String.valueOf(status));
    }

    public static boolean getQuickPanelPostionisLeft() {
        return readBoolean(quickPanelPostionisLeft, def_quickPanelPostionIsLeft);
    }

    public static void setQuickPanelPostionisLeft(boolean status) {
        writeSetting(quickPanelPostionisLeft, String.valueOf(status));
    }

    public static boolean getLogUserActivity() {
        return readBoolean(logUserActivity, def_logUserActivity);
    }

    public static void setLogUserActivity(boolean status) {
        writeSetting(logUserActivity, String.valueOf(status));
    }

    public static int getQuickPanelToggleBarWidth() {
        return readInteger(quickPanelToggleBarWidth, def_quickPanelToggleBarWidth);
    }

    public static void setQuickPanelToggleBarWidth(int width) {
        writeSetting(quickPanelToggleBarWidth, String.valueOf(width));
    }

    public static int getQuickPanelDividerWidth() {
        return readInteger(quickPanelDividerWidth, def_quickPanelDividerWidth);
    }

    public static void setQuickPanelDividerWidth(int width) {
        writeSetting(quickPanelDividerWidth, String.valueOf(width));
    }

    public static boolean getContactStatusChangeTextNotification() {
        return readBoolean(contactStatusChangeTextNotification, def_contactStatusChangeTextNotification);
    }

    public static void setContactStatusChangeTextNotification(boolean status) {
        writeSetting(contactStatusChangeTextNotification, String.valueOf(status));
    }

    public static boolean getContactStatusChangeSoundNotification() {
        return readBoolean(contactStatusChangeSoundNotification, def_contactStatusChangeSoundNotification);
    }

    public static void setContactStatusChangeSoundNotification(boolean status) {
        writeSetting(contactStatusChangeSoundNotification, String.valueOf(status));
    }

    /**
     *  public getters and setters used by other classes
     */
    public static boolean getFirstRun() {
        return readBoolean(firstRun, def_firstRun);
    }

    public static void setFirstRun(boolean status) {
        writeSetting(firstRun, String.valueOf(status));
    }

    public static boolean getSleepEnabled() {
        return readBoolean(sleepEnabled, def_sleepEnabled);
    }

    public static void setSleepenabled(boolean enabled) {
        writeSetting(sleepEnabled, String.valueOf(enabled));
    }

    public static int getMainFrameMaximised() {
        return readInteger(mainFrameMaximised, def_mainFrameMaximised);
    }

    public static void setMainFrameMaximised(int status) {
        writeSetting(mainFrameMaximised, String.valueOf(status));
    }

    public static int getMainFrameWidth() {
        return readInteger(mainFrameWidth, def_mainFrameWidth);
    }

    public static void setMainFrameWidth(int width) {
        writeSetting(mainFrameWidth, String.valueOf(width));
    }

    public static int getMainFrameHeight() {
        return readInteger(mainFrameHeight, def_mainFrameHeight);
    }

    public static void setMainFrameHeight(int height) {
        writeSetting(mainFrameHeight, String.valueOf(height));
    }

    public static int getChannelVerticalSPPosition() {
        return readInteger(channelVerticalSPPosition, def_channelVerticalSPPosition);
    }

    public static void setChannelVerticalSPPosition(int position) {
        writeSetting(channelVerticalSPPosition, String.valueOf(position));
    }

    public static int getChannelChatHorizontalSPPosition() {
        return readInteger(channelChatHorizontalSPPosition, def_channelChatHorizontalSPPosition);
    }

    public static void setChannelChatHorizontalSPPosition(int position) {
        writeSetting(channelChatHorizontalSPPosition, String.valueOf(position));
    }

    public static int getChannelChatVerticalSPPosition() {
        return readInteger(channelChatVerticalSPPosition, def_channelChatVerticalSPPosition);
    }

    public static void setChannelChatVerticalSPPosition(int position) {
        writeSetting(channelChatVerticalSPPosition, String.valueOf(position));
    }

    public static boolean getSoundEnabled() {
        return readBoolean(SoundEnabled, def_soundEnabled);
    }

    public static void setSoundEnabled(boolean bool) {
        writeSetting(SoundEnabled, String.valueOf(bool));
    }

    public static boolean getTimeStampEnabled() {
        return readBoolean(TimeStamps, def_timeStamps);
    }

    public static void setTimeStampEnabled(boolean bool) {
        writeSetting(TimeStamps, String.valueOf(bool));
    }

    public static String getLastValidServerIP() {
        return readString(lastValidServerIP, def_lastValidServerIP);
    }

    public static void setLastValidServerIP(String ip) {
        writeSetting(lastValidServerIP, ip);
    }

    public static int getLastValidServerPort() {
        return readInteger(lastValidServerPort, def_lastValidServerPort);
    }

    public static void setLastValidServerPort(int port) {
        writeSetting(lastValidServerPort, String.valueOf(port));
    }

    public static int getFiletTansferPort() {
        return readInteger(fileTransferPort, def_fileTransferPort);
    }

    public static void setFiletTansferPort(int port) {
        writeSetting(fileTransferPort, String.valueOf(port));
    }

    public static boolean getAutoLogin() {
        return readBoolean(autoLogin, def_autoLogin);
    }

    public static void setAutoLogin(boolean bool) {
        writeSetting(autoLogin, String.valueOf(bool));
    }

    public static String getHomeChannel() {
        String ch = readString(homeChannel, def_homeChannel);
        if (ch.length() == 3) {
            return GameDatabase.getGameName(ch);
        } else {
            return ch;
        }
    }

    public static void setHomeChannel(String channel) {
        writeSetting(homeChannel, GameDatabase.getIDofGame(channel));
    }

    public static boolean getDebugMode() {
        return readBoolean(debugMode, def_debugMode);
    }

    public static void setDebugMode(boolean bool) {
        writeSetting(debugMode, String.valueOf(bool));
    }

    public static String getSelectedLookAndFeel() {
        return readString(selectedLookAndFeel, def_selectedLookAndFeel);
    }

    public static void setSelectedLookAndFeel(String string) {
        writeSetting(selectedLookAndFeel, string);
    }

    public static boolean getUseNativeLookAndFeel() {
        return readBoolean(useNativeLookAndFeel, def_useNativeLookAndFeel);
    }

    public static void setUseNativeLookAndFeel(boolean bool) {
        writeSetting(useNativeLookAndFeel, String.valueOf(bool));
    }

    public static Color getBackgroundColor() {
        return readColor(bgColor, def_bgColor);
    }

    public static void setBackgroundColor(Color color) {
        writeSetting(bgColor, String.valueOf(color.getRGB()));
    }

    public static Color getSelectionColor() {
        return readColor(selectionColor, def_SelectionColor);
    }

    public static void setSelectionColor(Color color) {
        writeSetting(selectionColor, String.valueOf(color.getRGB()));
    }

    public static Color getForegroundColor() {
        return readColor(fgColor, def_fgColor);
    }

    public static void setForegroundColor(Color color) {
        writeSetting(fgColor, String.valueOf(color.getRGB()));
    }

    public static Color getYourUsernameColor() {
        return readColor(yourUsernameColor, def_yourUsernameColor);
    }

    public static void setYourUsernameColor(Color color) {
        writeSetting(yourUsernameColor, String.valueOf(color.getRGB()));
    }

    public static Color getOtherUsernamesColor() {
        return readColor(otherUsernamesColor, def_otherUsernamesColor);
    }

    public static void setOtherUsernamesColor(Color color) {
        writeSetting(otherUsernamesColor, String.valueOf(color.getRGB()));
    }

    public static Color getSystemMessageColor() {
        return readColor(systemMessageColor, def_systemMessageColor);
    }

    public static void setSystemMessageColor(Color color) {
        writeSetting(systemMessageColor, String.valueOf(color.getRGB()));
    }

    public static Color getWhisperMessageColor() {
        return readColor(whisperMessageColor, def_whisperMessageColor);
    }

    public static void setWhisperMessageColor(Color color) {
        writeSetting(whisperMessageColor, String.valueOf(color.getRGB()));
    }

    public static String getNameStyle() {
        return readString(nameStyle, def_nameStyle);
    }

    public static void setNameStyle(String style) {
        writeSetting(nameStyle, style);
    }

    public static int getNameSize() {
        return readInteger(nameSize, def_nameSize);
    }

    public static void setNameSize(int size) {
        writeSetting(nameSize, String.valueOf(size));
    }

    public static String getMessageStyle() {
        return readString(messageStyle, def_messageStyle);
    }

    public static void setMessageStyle(String style) {
        writeSetting(messageStyle, style);
    }

    public static int getMessageSize() {
        return readInteger(messageSize, def_messageSize);
    }

    public static void setMessageSize(int size) {
        writeSetting(messageSize, String.valueOf(size));
    }

    public static Color getFriendMessageColor() {
        return readColor(friendMessageColor, def_friendMessageColor);
    }

    public static void setFriendMessageColor(Color color) {
        writeSetting(friendMessageColor, String.valueOf(color.getRGB()));
    }

    public static Color getFriendUsernameColor() {
        return readColor(friendUsernameColor, def_friendUsernameColor);
    }

    public static void setFriendUsernameColor(Color color) {
        writeSetting(friendUsernameColor, String.valueOf(color.getRGB()));
    }

    public static Color getUserMessageColor() {
        return readColor(userMessageColor, def_userMessageColor);
    }

    public static void setUserMessageColor(Color color) {
        writeSetting(userMessageColor, String.valueOf(color.getRGB()));
    }

    public static boolean getColorizeBody() {
        return readBoolean(colorizeBody, def_colorizeBody);
    }

    public static void setColorizeBody(boolean bool) {
        writeSetting(colorizeBody, String.valueOf(bool));
    }

    public static boolean getColorizeText() {
        return readBoolean(colorizeText, def_colorizeText);
    }

    public static void setColorizeText(boolean bool) {
        writeSetting(colorizeText, String.valueOf(bool));
    }

    public static String getRecieveDestination() {
        return readString(recieveDest, def_recievedest);
    }

    public static void setRecieveDestination(String path) {
        writeSetting(recieveDest, path);
    }

    public static String getWineCommand() {
        return readString(wineCommand, def_wineComamnd);
    }

    public static void setWineCommand(String path) {
        writeSetting(wineCommand, path);
    }

    public static String getLastLoginName() {
        return readString(lastLoginName, def_lastLoginName);
    }

    public static void setLastLoginName(String name) {
        writeSetting(lastLoginName, name);
    }

    public static String getLastLoginPassword() {
        return PasswordEncrypter.decodePassword(readString(lastLoginPassword, def_lastLoginPassword));
    }

    public static void setLastLoginPassword(String pw) {
        writeSetting(lastLoginPassword, PasswordEncrypter.encodePassword(PasswordEncrypter.encryptPassword(pw)));
    }

    public static void setMultiChannel(boolean enabled) {
        writeSetting(multiChannel, String.valueOf(enabled));
    }

    public static boolean getMultiChannel() {
        return readBoolean(multiChannel, def_multiChannel);
    }

    public static boolean isQuickPanelIconSizeBig() {
        return readBoolean(quickPanelIconSizeIsBig, def_quickPanelIconSizeIsBig);
    }

    public static void setIsQuickPanelIconSizeBig(boolean bool) {
        writeSetting(quickPanelIconSizeIsBig, String.valueOf(bool));
    }

    public static void setShowOfflineContacts(boolean enabled) {
        boolean refreshContacts = Settings.getShowOfflineContacts() == false && enabled == true;
        writeSetting(showOfflineContacts, String.valueOf(enabled));
        if (refreshContacts) {
            Protocol.refreshContacts();
        } else {
            Globals.getContactList().updateShowOfflineContacts();
        }
    }

    public static boolean getShowOfflineContacts() {
        return readBoolean(showOfflineContacts, def_showOfflineContacts);
    }

    public static void setRememberMainFrameSize(boolean enabled) {
        writeSetting(rememberMainFrameSize, String.valueOf(enabled));
    }

    public static boolean getRememberMainFrameSize() {
        return readBoolean(rememberMainFrameSize, def_rememberMainFrameSize);
    }

    public static void addFavouriteByName(String channel) {
        String ID = GameDatabase.getIDofGame(channel);
        if (!favourites.contains(ID)) {
            favourites.add(ID);
            saveFavourites();
        }
    }

    public static Vector<String> getFavouritesByID() {
        Vector<String> favs = new Vector<String>();
        favs.addAll(favourites);
        return favs;
    }

    public static Vector<String> getFavouritesByName() {
        Vector<String> favs = new Vector<String>();
        for (String ID : favourites) {
            favs.add(GameDatabase.getGameName(ID));
        }
        return favs;
    }

    public static void removeFavourite(String ID) {
        favourites.remove(ID);
        saveFavourites();
    }

    private static void saveFavourites() {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter(FAVOURITES_FILE));
        } catch (Exception ex) {
        }
        for (String s : favourites) {
            pw.println(s);
        }
        pw.close();
        if (Globals.getClientFrame() != null) {
            Globals.getClientFrame().refreshFavourites();
        }
    }

    public static void loadFavourites() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(FAVOURITES_FILE));
        } catch (FileNotFoundException ex) {
            return;
        }
        favourites.clear();
        Boolean done = false;
        String input;
        while (!done) {
            try {
                input = br.readLine();
                if (input == null) {
                    done = true;
                    continue;
                }
            } catch (IOException ex) {
                return;
            }
            if (input.length() == 3) {
                favourites.add(input);
            } else {
                favourites.add(GameDatabase.getIDofGame(input));
            }
        }
        try {
            br.close();
        } catch (Exception e) {
        }
    }
}
