package buttress.io;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JOptionPane;
import buttress.gui.ConfigDialog;
import buttress.gui.ConsoleDialog;
import buttress.main.Buttress;
import buttress.main.Driver;
import buttress.rss.RssItem;
import buttress.util.BrowserLauncher;

public class Download {

    private static boolean isFirstTorrent = true;

    /**
	 * the number of seconds buttress pauses for when passing the first torrent 
	 * to the client (to allow the client time to start up before it receives
	 * successive torrents).
	 */
    public static final int sleepDuration = 30 * 1000;

    private URL downloadUrl;

    private String torrentName;

    /**
	 * the folder the torrent was saved to (including the terminal "/")
	 * 
	 * @see torrentName for the full torrent path
	 */
    private String savedFileSys;

    /**
	 * constructor
	 * 
	 * @param link URL of the torrent
	 */
    public Download(URL link) {
        this(link, "");
    }

    /**
	 * Useful when trying to pass the torrent to the client
	 * 
	 * @param link
	 * @param localLocation the full pathname of the torrent file, including 
	 * the name of the torrent file. - cannot errorcheck...  no clue how
	 */
    public Download(URL link, String localLocation) {
        downloadUrl = link;
        savedFileSys = localLocation;
        torrentName = downloadUrl.toString().substring(downloadUrl.toString().lastIndexOf("/") + 1);
    }

    /**
	 * returns the name of the torrent
	 *
	 * @return torrentName
	 */
    public String getTorrentName() {
        return torrentName;
    }

    /**
	 * returns the URL of the torrent
	 *
	 * @return the downloadUrl of the torrent
	 */
    public URL getDownloadUrl() {
        return downloadUrl;
    }

    /**
	 * @param item
	 * @param newDownload
	 * @return
	 */
    public boolean downloadTorrent(RssItem item) {
        try {
            saveTorrent();
        } catch (java.io.FileNotFoundException exc) {
            ConsoleDialog.writeError("Torrent not found: " + item.getTitle() + " (" + item.getLocation().getHost() + ")", exc);
            return false;
        } catch (java.io.IOException exc) {
            ConsoleDialog.writeError("Download failed: " + item.getTitle() + " (" + item.getLocation().getHost() + ") " + exc.getLocalizedMessage(), exc);
            return false;
        }
        return true;
    }

    /**
	 * Takes the url and downloads the files to the folder specified in the 
	 * driver's download folder.
	 * 
	 * @see Driver.getDownloadFolder()
	 * @see Buttress.getButtress()
	 * @see Rss.downloadIt(RssItem)
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public void saveTorrent() throws java.io.FileNotFoundException, java.io.IOException {
        URLConnection connection;
        connection = downloadUrl.openConnection();
        connection.setRequestProperty("User-Agent", Driver.agent);
        connection.connect();
        InputStream ourStream = connection.getInputStream();
        BufferedInputStream ourBuffer = new BufferedInputStream(ourStream);
        String saveTo = Buttress.getButtress().getDriver().getDownloadFolder() + RunTorrent.FILE_SEP;
        FileOutputStream ourOut = new FileOutputStream(saveTo + torrentName);
        savedFileSys = saveTo + torrentName;
        int size = ourStream.available();
        if (size == 0) {
            throw new IOException("Cannot read the source file - 0 bytes " + "available to be read.");
        }
        byte[] temp = new byte[size];
        int readIn;
        while ((readIn = ourBuffer.read(temp, 0, size)) != -1) {
            ourOut.write(temp, 0, readIn);
        }
        ourOut.close();
    }

    /**
	 * Downloads with an alternate download program
	 * 
	 * @param driving the driver that contains the location of the alternate 
	 * download program
	 */
    public boolean altDownloadTorrent() {
        Driver driving = Buttress.getButtress().getDriver();
        String execute = driving.getAltDownloaderPath() + " " + downloadUrl;
        if (RunTorrent.operatingSystems.contains(System.getProperty("os.name"))) {
            execute = "\"" + driving.getAltDownloaderPath() + "\" \"" + downloadUrl + "\"";
        }
        ConsoleDialog.writeConsole("About to execute: " + execute);
        try {
            Runtime.getRuntime().exec(execute);
            return true;
        } catch (IOException exc) {
            ConsoleDialog.writeError("Couldn't download torrent " + torrentName + " with " + driving.getAltDownloaderPath().substring(driving.getAltDownloaderPath().lastIndexOf(RunTorrent.FILE_SEP)), exc);
            return false;
        }
    }

    /**
	 * @param item
	 */
    public boolean openBrowserDownload() {
        try {
            BrowserLauncher.openURL(downloadUrl.toString());
        } catch (IOException exc) {
            BrowserLauncher.browserFailed(exc);
            ConsoleDialog.writeError(exc.getLocalizedMessage(), exc);
            return false;
        }
        return true;
    }

    /**
	 * savedFileSys must be set before calling this method
	 * 
	 * @return
	 */
    public boolean passTorrentToClient() {
        boolean worked;
        Driver driving = Buttress.getButtress().getDriver();
        if (!errorFree()) {
            ConsoleDialog.writeError("The torrent: " + torrentName + "cannot " + "be passed to the client because we do not know where on " + "the local file system it is.", new RuntimeException("savedFileSys not set, torrent " + "location unknown"));
            return false;
        }
        RunTorrent runner = new RunTorrent(driving.getBitPath(), savedFileSys, torrentName);
        try {
            runner.startTorrent();
            worked = true;
        } catch (IOException exc) {
            worked = false;
            ConsoleDialog.writeError("Couldn't start torrent with your client: " + torrentName + " (" + downloadUrl.getHost() + ") " + exc.getLocalizedMessage() + "\nTry checking your paths in File->Config", exc);
            JOptionPane.showMessageDialog(Buttress.getButtress().getMyGui(), "Your BitTorrent client couldn't be started." + "\n\nPlease check the path to your client." + "\n(\"Path to BitTorrent:\")", "Torrents Can't Be Started", JOptionPane.ERROR_MESSAGE);
            ConfigDialog.showConfigMenu();
        }
        if (worked) {
            sleepOnFirstRun();
        }
        return worked;
    }

    /**
	 * error check the download, make sure it was saved correctly in the first 
	 * place.
	 * 
	 * @return true if it worked, false if things went wrong.
	 */
    private boolean errorFree() {
        if (savedFileSys.equals("")) {
            return false;
        }
        return true;
    }

    /**
	 * if it's the first torrent run and there's more than 1 torrent to run, 
	 * sleep for duration to let client start up before giving it more 
	 * torrents. if it couldn't sleep for the duration, try to make it sleep 
	 * next time too so that the client has time to start up.
	 */
    private void sleepOnFirstRun() {
        if (isFirstTorrent && Buttress.getButtress().getDriver().getTotalNumNewTorrents() - 1 > 1) {
            ConsoleDialog.writeConsole("Waiting " + sleepDuration / 1000 + " seconds for client to start.");
            boolean doSet = true;
            try {
                Thread.sleep(sleepDuration);
            } catch (InterruptedException exc) {
                doSet = false;
                ConsoleDialog.writeError("Did not successfully wait.  " + "Will try waiting again.", exc);
            }
            if (doSet) {
                isFirstTorrent = false;
                ConsoleDialog.writeConsole("Successfully waited.  " + "Loading remaining torrents.");
            }
        }
    }

    /**
	 * The location of the file in the local file system, equal to "" (an empty 
	 * String) until it's set (Buttress saves the location of the torrent).
	 * 
	 * @return Returns the savedFileSys.
	 */
    public String getSavedFileSys() {
        return savedFileSys;
    }

    /**
	 * Returns the location of the file in the local file system, equal to "" 
	 * (an empty String) until it's set (Buttress saves the location of the 
	 * torrent).
	 * 
	 * @param savedFileSys The savedFileSys to set.
	 */
    public void setSavedFileSys(String savedFileSys) {
        this.savedFileSys = savedFileSys;
    }
}
