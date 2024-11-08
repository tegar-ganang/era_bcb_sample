package buttress.main;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import buttress.gui.AgreementBox;
import buttress.gui.ConfigDialog;
import buttress.gui.ConsoleDialog;
import buttress.gui.Gui;
import buttress.rss.Rss;
import buttress.rss.saver.RssLoader;
import buttress.util.UpdateChecker;
import buttress.util.email.Email;
import buttress.util.email.EmailBuilder;

public class Buttress {

    /**
	 * (OPENING SENTENCE)
	 * (MORE COMMENTS)
	 * 
	 * @author nd1030
	 * @version 0.0.0.0
	 * <DT><B>
	 * Date:
	 * </B></DT><DD>
	 * YY-MM-DD - Nov 9, 2004; 3:26:07 PM
	 * </DD>
	 */
    public class ButIniProp extends Properties {

        public ButIniProp() {
            super();
        }

        public final String altDownloader = "altDownloaderPath";

        public final String useAltDownloader = "useAltDownloader";

        public final String version = "ButtressVersion";

        public final String bitPath = "bitTorrentPath";

        public final String downloadFolder = "downloadFolder";

        public final String delay = "feedDelay";

        public final String locX = "GuiXLocation";

        public final String locY = "GuiYLocation";

        public final String width = "GuiWidth";

        public final String height = "GuiHeight";

        public final String start = "autoStartTorrents";

        public final String sendEmail = "sendMail";

        public final String lastUpdate = "lastUpdated";
    }

    private static Buttress myBut;

    private Gui myGui;

    private Driver myDriver;

    public static final String INI_FILENAME = "buttress.ini";

    public static final String FEED_FILTER_FILENAME = "FeedAndFilter.txt";

    private boolean debugBut = false;

    private boolean firstRun = false;

    /**
	 * Start here...  The main main thingie.
	 * @param args
	 */
    public Buttress(String[] args) {
        if (args.length > 0 && args[0].equals("debugBut")) {
            debugBut = true;
            ConsoleDialog.writeConsole("***DEV MODE***");
        }
        myGui = new Gui(debugBut);
        myDriver = new Driver();
    }

    /**
	 * Sends emails, if allowed.  If Buttress didn't finish loading, quits and 
	 * informs user.
	 * 
	 * @param e the caught Exception 
	 * @param description the explanation of the error.
	 */
    private void catchMailableError(Exception e, String description) {
        new EmailBuilder(e, description);
        if (myGui == null || !myGui.isDisplaying()) {
            JOptionPane.showMessageDialog(myBut.getMyGui(), "Please restart Buttress, it did not load correctly and " + "is now quitting.", "Please Restart Buttress", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    /**
	 * The main program for the Gui class
	 * 
	 * @param args String[] The command line arguments
	 */
    public static void main(String[] args) {
        setLookAndFeel();
        myBut = new Buttress(args);
        try {
            myBut.go();
        } catch (Exception e) {
            myBut.catchMailableError(e, "Thrown on Buttress load.");
        }
    }

    /**
	 * the executable operations...  placed in this method to avoid repeating 
	 * code in case of error.
	 */
    private void go() {
        checkconfig();
        if (!isDebug()) {
            UpdateChecker.checkAndQuery(true);
        }
        displayGui();
        loadRsses();
        dispConfigOnFirstRun();
        startDriving(myDriver);
    }

    /**
	 * if this is the first run of the program, it'll display the config menu.
	 */
    private void dispConfigOnFirstRun() {
        if (isFirstRun()) {
            if (myGui.isShowing()) {
                ConfigDialog.showConfigMenu();
            } else {
                EventQueue.invokeLater(new Runnable() {

                    public void run() {
                        ConfigDialog.showConfigMenu();
                    }
                });
            }
        }
    }

    /**
	 * 
	 */
    private void loadRsses() {
        Vector loader = RssLoader.loadRsses();
        if (loader != null) {
            for (int i = 0; i < loader.size(); i++) {
                myBut.getMyGui().addFeed((Rss) loader.get(i), false);
            }
        }
        myBut.getMyGui().listFilterOwners();
    }

    /**
	 * sets the look and feel to the local system
	 */
    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            ConsoleDialog.writeError("Failed to set native platform look and feel", e);
        }
    }

    /**
	 * Checks the ini file for the paths and any other configs
	 * <P>
	 * if the ini file has not been created yet it will make one with the 
	 * default values
	 */
    private void checkconfig() {
        try {
            File ini = new File(INI_FILENAME);
            if (ini.createNewFile()) {
                setFirstRun(true);
                if (!AgreementBox.agree(myGui)) {
                    ini.delete();
                    System.exit(0);
                }
                firstRun();
            } else {
                if (!readConfig()) {
                    JOptionPane.showMessageDialog(myBut.getMyGui(), "Your settings could not be read.\n\n" + "Please check the config menu to verify " + "your settings.");
                }
            }
        } catch (Exception e) {
            ConsoleDialog.writeError("Something went wrong while checking " + "the config file: " + e.getLocalizedMessage(), e);
        }
    }

    /**
	 * sets whether or not it's the first run of the program
	 * 
	 *
	 * @param b boolean
	 * @return boolean
	 */
    private boolean setFirstRun(boolean b) {
        return firstRun = b;
    }

    /**
	 * returns whether or not it's the first run of the program
	 *
	 * @return firstRun boolean
	 */
    public boolean isFirstRun() {
        return firstRun;
    }

    /**
	 * The code to run the first time the program runs.  Values that cannot be 
	 * set by the user must be set here (gui location, default start time, etc).
	 * 
	 */
    private void firstRun() {
        Gui theGui = Buttress.getButtress().getMyGui();
        writeConfig(theGui.getLocation(), theGui.getSize());
    }

    /**
	 * writes the varables to the ini file
	 *
	 * @param myLoc Point
	 * @return boolean
	 */
    public boolean writeConfig(Point myLoc, Dimension mySize) {
        boolean goodWrite = true;
        ButIniProp p = new ButIniProp();
        FileOutputStream out = null;
        if (myLoc == null) {
            myLoc = new Point(myGui.getLocation());
        }
        if (mySize == null) {
            mySize = new Dimension(myGui.getSize());
        }
        p.put(p.version, Driver.agent);
        try {
            p.put(p.bitPath, myDriver.getBitPath());
        } catch (NullPointerException exc) {
            p.put(p.bitPath, Driver.DEFAULT_BT_PATH);
        }
        try {
            p.put(p.downloadFolder, myDriver.getDownloadFolder());
        } catch (NullPointerException exc) {
            p.put(p.downloadFolder, Driver.DEFAULT_DOWNLOAD_FOLDER);
        }
        try {
            p.put(p.altDownloader, myDriver.getAltDownloaderPath());
        } catch (NullPointerException exc) {
            p.put(p.altDownloader, Driver.DEFAULT_ALT_DOWNLOADER);
        }
        try {
            p.put(p.lastUpdate, new Long(UpdateChecker.getLastUpdated().getTime()).toString());
        } catch (NullPointerException exc) {
            p.put(p.lastUpdate, new Long(UpdateChecker.DEFAULT_UPDATE.getTime()).toString());
        }
        p.put(p.delay, new Integer(myDriver.getTimeDelay()).toString());
        p.put(p.locX, new Integer(myLoc.x).toString());
        p.put(p.locY, new Integer(myLoc.y).toString());
        p.put(p.width, new Integer(mySize.width).toString());
        p.put(p.height, new Integer(mySize.height).toString());
        p.put(p.start, new Boolean(myDriver.isAutoStart()).toString());
        p.put(p.sendEmail, new Boolean(Email.isSendEmail()).toString());
        p.put(p.useAltDownloader, new Boolean(myDriver.useAltDownloader()).toString());
        try {
            out = new FileOutputStream(INI_FILENAME);
        } catch (FileNotFoundException exc) {
            ConsoleDialog.writeError("Your " + INI_FILENAME + " file cannot" + " be found,\nyour settings were not saved.", exc);
            return goodWrite = false;
        }
        try {
            p.store(out, "/* properties updated */");
        } catch (IOException exc) {
            ConsoleDialog.writeError("The " + INI_FILENAME + " file cannot" + " be saved.", exc);
            return goodWrite = false;
        }
        if (myBut.getMyGui() != null) {
            ConsoleDialog.writeConsole("Updated ini file");
        }
        return goodWrite;
    }

    /**
	 * reads the configuration (ini) file
	 *
	 * @return goodRead
	 */
    public boolean readConfig() {
        boolean goodRead = true;
        int x, y;
        int width, height;
        ButIniProp p = new ButIniProp();
        try {
            p.load(new FileInputStream(INI_FILENAME));
        } catch (IOException exc) {
            ConsoleDialog.writeError("Your " + INI_FILENAME + " file cannot" + " be found," + "\nyour settings could not be read.", exc);
            goodRead = false;
        }
        if (null == myDriver.setBitPath(p.getProperty(p.bitPath))) {
            myDriver.setBitPath(Driver.DEFAULT_BT_PATH);
            goodRead = false;
        }
        if (null == myDriver.setDownloadFolder(p.getProperty(p.downloadFolder))) {
            myDriver.setDownloadFolder(Driver.DEFAULT_DOWNLOAD_FOLDER);
            goodRead = false;
        }
        myDriver.setChangedDownloadLocation(!myDriver.getDownloadFolder().equals(Driver.DEFAULT_DOWNLOAD_FOLDER));
        if (null == myDriver.setAltDownloaderPath(p.getProperty(p.altDownloader))) {
            myDriver.setAltDownloaderPath(Driver.DEFAULT_ALT_DOWNLOADER);
            goodRead = false;
        }
        if (null == p.getProperty(p.lastUpdate)) {
            UpdateChecker.setLastUpdated(UpdateChecker.DEFAULT_UPDATE);
        } else {
            UpdateChecker.setLastUpdated(new Date(new Long(p.getProperty(p.lastUpdate)).longValue()));
        }
        try {
            myDriver.setTimeDelay(Integer.parseInt(p.getProperty(p.delay)));
        } catch (NumberFormatException exc) {
            myDriver.setTimeDelay(Driver.MIN_DELAY);
            goodRead = false;
        } catch (NullPointerException exc) {
            myDriver.setTimeDelay(Driver.MIN_DELAY);
            goodRead = false;
        }
        try {
            x = Integer.parseInt(p.getProperty(p.locX));
        } catch (NumberFormatException exc) {
            x = Gui.DEFAULT_X_LOC;
            goodRead = false;
        } catch (NullPointerException exc) {
            x = Gui.DEFAULT_X_LOC;
            goodRead = false;
        }
        try {
            y = Integer.parseInt(p.getProperty(p.locY));
        } catch (NumberFormatException exc) {
            y = Gui.DEFAULT_Y_LOC;
            goodRead = false;
        } catch (NullPointerException exc) {
            y = Gui.DEFAULT_Y_LOC;
            goodRead = false;
        }
        try {
            width = Integer.parseInt(p.getProperty(p.width));
        } catch (NumberFormatException exc) {
            width = Gui.DEFAULT_WIDTH;
            goodRead = false;
        } catch (NullPointerException exc) {
            width = Gui.DEFAULT_WIDTH;
            goodRead = false;
        }
        try {
            height = Integer.parseInt(p.getProperty(p.height));
        } catch (NumberFormatException exc) {
            height = Gui.DEFAULT_HEIGHT;
            goodRead = false;
        } catch (NullPointerException exc) {
            height = Gui.DEFAULT_HEIGHT;
            goodRead = false;
        }
        try {
            myDriver.setAutoStart(new Boolean(p.getProperty(p.start)).booleanValue());
        } catch (NullPointerException exc) {
            myDriver.setAutoStart(Driver.DEFAULT_USE_AUTO_START);
            goodRead = false;
        }
        try {
            Email.setSendEmail(new Boolean(p.getProperty(p.sendEmail)).booleanValue());
        } catch (NullPointerException exc) {
            Email.setSendEmail(Email.isSendEmail());
            goodRead = false;
        }
        try {
            myDriver.setUseAltDownloader(new Boolean(p.getProperty(p.useAltDownloader)).booleanValue());
        } catch (NullPointerException exc) {
            myDriver.setUseAltDownloader(Driver.DEFAULT_USE_ALT_DOWNLOADER);
            goodRead = false;
        }
        myGui.setLocation(x, y);
        myGui.setSize(width, height);
        return goodRead;
    }

    /**
	 * Set the final display actions here and displays Gui.  Makes sure the GUI 
	 * is set on screen correctly.
	 * 
	 */
    private void displayGui() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Point placeHere = new Point(myGui.getLocation());
        Dimension sizeMe = new Dimension(myGui.getSize());
        if (myGui.getSize().height > screenSize.height) {
            sizeMe.height = screenSize.height;
        }
        if (myGui.getSize().width > screenSize.width) {
            sizeMe.width = screenSize.width;
        }
        if (myGui.getLocation().x + myGui.getSize().width > screenSize.width) {
            placeHere.x = screenSize.width - myGui.getSize().width;
        } else if (myGui.getLocation().x < 0) {
            placeHere.x = 0;
        }
        if (myGui.getLocation().y + myGui.getSize().height > screenSize.height) {
            placeHere.y = screenSize.height - myGui.getSize().height;
        } else if (myGui.getLocation().y < 0) {
            placeHere.y = 0;
        }
        myGui.setLocation(placeHere);
        myGui.setSize(sizeMe);
        EventQueue.invokeLater(new GuiShower(myGui));
    }

    /**
	 *
	 * @param withAFullGasTank Driver
	 */
    private void startDriving(Driver withAFullGasTank) {
        withAFullGasTank.startThread();
        withAFullGasTank.doCheck();
    }

    /**
	 * @return Returns the program's gui
	 */
    public Gui getMyGui() {
        return myGui;
    }

    /**
	 * @return whether or not debug is on
	 */
    public boolean isDebug() {
        return debugBut;
    }

    /**
	 * @param isBug sets the debug value
	 */
    public void setDebug(boolean isBug) {
        debugBut = isBug;
    }

    /**
	 * @return the driver
	 */
    public Driver getDriver() {
        return myDriver;
    }

    public static Buttress getButtress() {
        return myBut;
    }

    /**
	 * @return Returns the fEED_FILTER_FILENAME.
	 */
    public static String getFEED_FILTER_FILENAME() {
        return FEED_FILTER_FILENAME;
    }

    /**
	 * @return Returns the iNI_FILENAME.
	 */
    public static String getINI_FILENAME() {
        return INI_FILENAME;
    }

    /**
	 * Looks strange, but is a wonderful thing...  Makes the display threadsafe
	 *
	 * <p>Title: </p>
	 * <p>Description: </p>
	 * <p>Copyright: Copyright (c) 2004</p>
	 * <p>Company: </p>
	 * @author nick
	 */
    private class GuiShower implements Runnable {

        final Gui frame;

        public GuiShower(Gui frame) {
            this.frame = frame;
        }

        public void run() {
            frame.setVisible(true);
        }
    }
}
