package general;

import gui.GUIMain;
import gui.LocalPanel;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Scanner;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import state.ProgramState;
import storage.ConfigFile;
import storage.DB;

/**
 * This class contains some things that don't seem to fit into the other categories,
 * 1) Screenshot capture and movement
 * 2) Replay Capture and Movement
 * 3) "/replay" command -> clones replays
 * 4) clipboard handling
 * 5) OS information
 * 6) URL fetcher
 *
 */
public class MiscTools {

    public static final int WINDOWS = 0;

    public static final int OSX = 1;

    public static final int LINUX = 2;

    public static final int OTHER = 3;

    /**
	 * Grabs lastreplay.w3g, moves and renames it to a database specified by the user
	 * @param fileComment --an extra comment the user can leave
	 */
    public static void moveReplay() {
        String replaypattern = ConfigFile.getProperty("replaypattern", "$year$-$month$-$day$ $undertime$ $bannedlist$");
        String replaysavepath = ConfigFile.getProperty("replaylocation", System.getProperty("user.dir") + "\\Replays");
        String wc3path = ConfigFile.getProperty("WC3Path", "");
        if (wc3path.equals("")) {
            System.err.println("Please set your Warcraft III locations in Preferences->Databases");
            return;
        }
        String temp = ConfigFile.getProperty("movereplays", "true");
        boolean movereplays = Boolean.parseBoolean(temp);
        temp = ConfigFile.getProperty("alwaysmove", "true");
        boolean alwaysmove = Boolean.parseBoolean(temp);
        if (!wc3path.toLowerCase().endsWith("replay")) wc3path += "/replay";
        wc3path += "/LastReplay.w3g";
        String saveTo = replaysavepath + "/" + KeyRef.processText(replaypattern) + ".w3g";
        if (movereplays) {
            if (!alwaysmove || packets.Parser.bannedList.equals("")) {
                copyFile(wc3path, saveTo);
            }
        }
    }

    /**
	 * Copies a file in a new thread given a source address, and a destination address
	 * It will make all the necessary directories if they don't exist
	 * @param src --the address of the file to be copied
	 * @param dest --the destination address to copy it too
	 */
    public static void copyFile(final String src, final String dest) {
        Runnable r1 = new Runnable() {

            public void run() {
                try {
                    File inf = new File(dest);
                    if (!inf.exists()) {
                        inf.getParentFile().mkdirs();
                    }
                    FileChannel in = new FileInputStream(src).getChannel();
                    FileChannel out = new FileOutputStream(dest).getChannel();
                    out.transferFrom(in, 0, in.size());
                    in.close();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Error copying file \n" + src + "\n" + dest);
                }
            }
        };
        Thread cFile = new Thread(r1, "copyFile");
        cFile.start();
    }

    /**
	 * Copies the last replay to the same location with the given replayname
	 * 
	 * @param replayname --name of the new replay (without .w3g) can contain KeyRefs
	 */
    public static void cloneReplay(String replayname) {
        String wc3path = ConfigFile.getProperty("WC3Path", "");
        if (wc3path.equals("")) {
            System.err.println("Please set your Warcraft III locations in Preferences->Databases");
            return;
        }
        String replaysavepath = storage.ConfigFile.getProperty("replaylocation", "");
        if (replaysavepath == "") {
            System.err.println("Please set your Database values in Preferences-->Databases");
            return;
        }
        if (!wc3path.toLowerCase().endsWith("replay")) wc3path += "/replay";
        wc3path += "/LastReplay.w3g";
        String saveTo = replaysavepath + "/" + KeyRef.processText(replayname) + ".w3g";
        copyFile(wc3path, saveTo);
    }

    /**
	 * Copies a string to the system clipboard
	 * @param toClipboard the string to be copied
	 * 
	 */
    public static void copyToClipboard(String toClipboard) {
        if (toClipboard.length() < 256) {
            StringSelection ss = new StringSelection(toClipboard);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
            sound.Audio.play(sound.Audio.COPY);
        } else {
            System.err.println("copyToClipboard-Warning: The string-length is greater than 256! ('" + toClipboard + "')");
            sound.Audio.play(sound.Audio.ERROR);
        }
    }

    /**
	 * Takes a screenshot, naming it with the default_pattern and puts it in the default folder
	 */
    public static void screenshot() {
        sound.Audio.play(sound.Audio.CLING);
        String pattern1 = storage.ConfigFile.getProperty("sspattern", "$year$-$month$-$day$ at $undertime$");
        screenshot(pattern1);
    }

    /**
	 * Captures a screenshot and saves it to the given destination with the specified pattern
	 * 
	 * @param pattern : string pattern of the screenshot's name
	 * @param dest : File folder where it will be stored
	 */
    public static void screenshot(String pattern) {
        String sssavepath = storage.ConfigFile.getProperty("sslocation", "");
        if (sssavepath.equals("")) {
            System.err.println("Please set your Database values in Preferences-->Databases");
        }
        try {
            Toolkit thiskit = Toolkit.getDefaultToolkit();
            Dimension size = thiskit.getScreenSize();
            Rectangle rect = new Rectangle(0, 0, size.width, size.height);
            Robot bot = new Robot();
            BufferedImage image = bot.createScreenCapture(rect);
            File outfile = new File(sssavepath + "\\" + KeyRef.processText(pattern) + ".jpg");
            ImageIO.write(image, "jpg", outfile);
        } catch (AWTException e) {
            System.err.println("Error in capturing Screenshot");
            e.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Error Making Screenshot File");
            e1.printStackTrace();
        }
    }

    /**
	 * @return Constant representation of the System OS
	 */
    public static int getOS() {
        String os = System.getProperty("os.name");
        if (os.contains("Windows")) {
            return WINDOWS;
        } else if (os.contains("Mac")) {
            return OSX;
        } else if (os.contains("Linux")) {
            return LINUX;
        } else {
            return OTHER;
        }
    }

    /**
	 * Converts bytes to their Hexadecimal Equivalent
	 * 
	 * @param b : a byte b (in ascii)?
	 * @return : returns a hexidecimal string representation of b
	 */
    public static String byteToHex(byte b) {
        String s = "";
        int i = new Integer(b);
        String tmp = Integer.toHexString(i);
        if (tmp.length() == 1) {
            tmp = "0" + tmp;
        }
        if (tmp.length() == 8) {
            tmp = tmp.substring(6);
        }
        s += tmp + " ";
        return s;
    }

    /**
	 * Gets a URL from a JAR filepath
	 * @param filePath  --location of the JAR file
	 * @return a URL representation of the file
	 */
    public static URL getURLFromJar(String filePath) {
        URL url = null;
        try {
            url = new File(filePath).toURI().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    /**
	 * TODO : fix this documentation...
	 * Adds a URL
	 * @param u --a URL
	 * @throws IOException
	 */
    @SuppressWarnings("unchecked")
    public static void addURL(URL u) throws IOException {
        URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        URL urls[] = sysLoader.getURLs();
        for (int i = 0; i < urls.length; i++) {
            if (urls[i].toString().equalsIgnoreCase(u.toString())) {
                System.err.println("URL " + u + " is already in the CLASSPATH");
                return;
            }
        }
        Class sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(sysLoader, new Object[] { u });
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }
    }

    /**
	 * Changes the account to a different one identified by account, forces a reload of
	 * localpanel in GUI, and some other heavy processes...
	 * 
	 * @param account --the name of the account to change to
	 * @return --true if the account exists, or is already the current one, false otherwise
	 */
    public static boolean changeAccount(String account) {
        ProgramState pg = ProgramState.instance();
        DB db = DB.instance();
        if (pg.getBnetName().equalsIgnoreCase(account)) return true;
        pg.setBnetName(account);
        try {
            if (db.getHostID(account) < 0) return false;
            pg.setBanlistName(db.getHostBanlistName(db.getHostID(account)));
            pg.setPassword(db.getHostPassword(db.getHostID(account)));
            pg.setRealm(db.getHostRealmInt(db.getHostID(account)));
            pg.setForumName(db.getHostForumName(db.getHostID(account)));
            pg.setForumPassword(db.getHostForumName(db.getHostID(account)));
            String passwordCount = "";
            for (int i = 0; i < pg.getPassword().length(); i++) passwordCount += "*";
            GUIMain.instance().tabs.remove(GUIMain.localPanel);
            GUIMain.localPanel = new LocalPanel();
            GUIMain.instance().tabs.insertTab("Local Bans", null, GUIMain.localPanel, "Bans you have created!", 1);
            db.showBans(db.getDatabaseID(pg.getBnetName()), 0);
            db.showBans(db.getDatabaseID(pg.getBnetName()), 1);
        } catch (SQLException e1) {
            System.err.println("Could not retreive account information to set to Program State");
            e1.printStackTrace();
        }
        if (!(pg.getBanlistName() == null) && !(pg.getBanlistName().equals(""))) {
            GUIMain.instance().getBuddiesBans();
        } else {
            GUIMain.instance().removeBuddiesTab();
        }
        return true;
    }

    /**
	 * Converts a MAC address to a (hex) strring
	 * @param mac - an array of bytes representing a MAC address
	 * @return - The hex words to mac
	 */
    public static String macToString(byte[] mac) {
        String macString = "";
        for (int i = 0; i < mac.length; i++) {
            macString += byteToHex(mac[i]).trim();
            if (i != mac.length - 1) {
                macString += ":";
            }
        }
        return macString;
    }

    /**
	 * Called before selecting a Layout, it builds a list of keylayout files in the Keylayout directory
	 * for use with a JComboBox
	 * @return - A Hashmap of "keylayout language - short name", File Path
	 */
    public static HashMap<String, String> buildKeylayoutMap(String keylayoutDir) {
        HashMap<String, String> keyLayouts = new HashMap<String, String>();
        File keyDir = new File(keylayoutDir);
        if (keyDir.exists() && keyDir.isDirectory()) {
            for (File f : keyDir.listFiles()) {
                if (f.getName().toLowerCase().endsWith(".txt") && !f.getName().toLowerCase().endsWith("howto.txt")) {
                    Scanner inf = null;
                    try {
                        inf = new Scanner(f);
                    } catch (FileNotFoundException e) {
                    }
                    String shortName = inf.nextLine();
                    keyLayouts.put(inf.nextLine() + " - " + shortName, f.getPath());
                    inf.close();
                }
            }
        } else {
            if (keyLayouts.isEmpty()) {
                keyLayouts = buildKeylayoutMap("../" + keylayoutDir);
                if (keyLayouts.isEmpty()) {
                    keyLayouts = buildKeylayoutMap(System.getProperty("user.dir"));
                }
            }
        }
        return keyLayouts;
    }

    /**
	 * Opens supplied URL (as String) in the system's default browser
	 * 
	 * @param linkURL - URL of address to be opened in default browser
	 * @param parentComponent - determines the Frame in which the dialog is displayed
	 */
    @SuppressWarnings("unchecked")
    public static void openLink(String linkURL, Component parentComponent) {
        String osName = System.getProperty("os.name");
        try {
            if (osName.startsWith("Mac OS")) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
                openURL.invoke(null, new Object[] { linkURL });
            } else if (osName.startsWith("Windows")) Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + linkURL); else {
                String[] browsers = { "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
                String browser = null;
                for (int count = 0; count < browsers.length && browser == null; count++) if (Runtime.getRuntime().exec(new String[] { "which", browsers[count] }).waitFor() == 0) browser = browsers[count];
                if (browser == null) throw new Exception("Could not find web browser"); else Runtime.getRuntime().exec(new String[] { browser, linkURL });
            }
        } catch (Exception E) {
            JOptionPane.showMessageDialog(null, "Couldn't launch your default browser, you can find the link at\n" + linkURL);
        }
    }
}
