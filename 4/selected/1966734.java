package ftraq;

import ftraq.transferqueue.LgTransferQueue;
import ftraq.bookmarklibrary.LgBookmarkLibrary;
import ftraq.bookmarklibrary.LgBookmarkController;
import ftraq.bookmarklibrary.exceptions.UpdateNodeAttributeFailure;
import ftraq.bookmarklibrary.exceptions.OpenFailure;
import ftraq.bookmarklibrary.LgBookmarkItem;
import ftraq.bookmarklibrary.LgBookmarkItemImpl;
import ftraq.settings.LgSettings;
import java.io.File;
import java.net.URI;
import java.security.CodeSource;

public class LgApplicationImpl implements LgApplication {

    private static ftraq.util.Logger logger = ftraq.util.Logger.getInstance(LgApplicationImpl.class);

    private LgTransferQueue _transferQueue = new ftraq.transferqueue.LgTransferQueueImpl();

    private LgBookmarkController _bookmarkLibrary = new LgBookmarkLibrary();

    private LgSettings _settings;

    private java.util.List _navigationSessionList = new java.util.ArrayList();

    private java.util.Set _uiObserverSet = new java.util.HashSet(2);

    private static LgApplication instance;

    private static File _confDirectory;

    private static File _installDirectory;

    private static final String CONFIG_DIR_NAME = "conf";

    private static final String LIBRARY_FILE_NAME = "bookmarklib.xml";

    private LgApplicationImpl() throws ftraq.exceptions.ApplicationStartupFailure {
        try {
            _installDirectory = this._getInstallDirectory();
            if (_installDirectory == null) {
                Exception e = new Exception("Unable to find installation directory.");
                throw new ftraq.exceptions.ApplicationStartupFailure(e);
            } else {
                _confDirectory = new java.io.File(_installDirectory, CONFIG_DIR_NAME);
            }
            if (this._confDirectory.exists() == false) {
                this._createConfDirectoryOnFirstStartup();
            }
            this._settings = new ftraq.settings.LgSettingsImpl(new java.io.File(_confDirectory, "settings.xml"));
            if (this.getSettings().isThisTheFirstStartupOnThisSystem()) {
                if (this.getSettings().isSystemIDListEmpty()) {
                    this._createDefaultSystemID();
                }
            }
            _transferQueue.loadQueue(new java.io.File(_confDirectory, "transferqueue.xml").getAbsolutePath());
            _bookmarkLibrary.open(new java.io.File(_confDirectory, "bookmarklib.xml").getAbsolutePath());
        } catch (Exception e) {
            throw new ftraq.exceptions.ApplicationStartupFailure(e);
        }
    }

    public static synchronized LgApplication instance() {
        if (LgApplicationImpl.instance == null) {
            try {
                LgApplicationImpl.instance = new LgApplicationImpl();
            } catch (ftraq.exceptions.ApplicationStartupFailure f) {
                System.err.println(f.getMessages());
                System.err.println(" ---- begin stack trace ----");
                f.printStackTrace();
                System.err.println(" ---- end stack trace ----");
                System.err.println("We're sorry, but this error is so fatal that FTraq cannot be started.");
                System.exit(-1);
            }
        }
        return LgApplicationImpl.instance;
    }

    /**
    * Creates local working copy of XML files and DTDs included in JAR file.
    */
    private void _createConfDirectoryOnFirstStartup() throws java.io.IOException {
        this._confDirectory.mkdir();
        java.util.jar.JarFile jarFile = new java.util.jar.JarFile(_installDirectory + "/ftraq.jar");
        _extractFileFromJar(jarFile, CONFIG_DIR_NAME + "/settings-1_0.dtd");
        _extractFileFromJar(jarFile, CONFIG_DIR_NAME + "/queue-1_0.dtd");
        _extractFileFromJar(jarFile, CONFIG_DIR_NAME + "/bookmarklib-1_0.dtd");
        _extractFileFromJar(jarFile, CONFIG_DIR_NAME + "/bookmarklib.xml");
        _extractFileFromJar(jarFile, CONFIG_DIR_NAME + "/settings.xml");
    }

    private File _getInstallDirectory() {
        CodeSource source = ftraq.Main.class.getProtectionDomain().getCodeSource();
        if (source == null) return null;
        File installDir;
        try {
            URI sourceURI = new URI(source.getLocation().toString());
            installDir = new File(sourceURI);
        } catch (java.net.URISyntaxException e) {
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (!installDir.isDirectory()) {
            installDir = installDir.getParentFile();
        }
        return installDir;
    }

    /**
    * Extracts a single file from a JAR archive.
    *
    * Files are extracted with subdirectories.
    *
    * @param i_jarFile the JAR file from which we want to extract a file.
    * @param i_fileName the name of the file in the JAR file, which will
    *   also be the file's local name.
    * @throws java.io.IOException if there is a read/write error.
    *
    */
    private void _extractFileFromJar(java.util.jar.JarFile i_jarFile, String i_fileName) throws java.io.IOException {
        java.io.InputStream inputStream = i_jarFile.getInputStream(i_jarFile.getEntry(i_fileName));
        java.io.BufferedReader bufferedReader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
        java.io.OutputStream outputStream = new java.io.FileOutputStream(_installDirectory + "/" + i_fileName);
        java.io.BufferedWriter bufferedWriter = new java.io.BufferedWriter(new java.io.OutputStreamWriter(outputStream));
        String nextLine = bufferedReader.readLine();
        while (nextLine != null) {
            bufferedWriter.write(nextLine + '\n');
            nextLine = bufferedReader.readLine();
        }
        bufferedReader.close();
        bufferedWriter.close();
        inputStream.close();
        outputStream.close();
    }

    public ftraq.transferqueue.LgTransferQueue getQueue() {
        return this._transferQueue;
    }

    public LgBookmarkController getLibrary() {
        return this._bookmarkLibrary;
    }

    public ftraq.settings.LgSettings getSettings() {
        return this._settings;
    }

    public LgNavigationSession createNavigationSession(LgBookmarkItem i_site) throws ftraq.fs.exceptions.FileSystemNotAvailableFailure {
        ftraq.fs.LgFileSystemBrowser localBrowser;
        ftraq.fs.LgFileSystemBrowser remoteBrowser;
        ftraq.fs.LgFileSystem localFileSystem = ftraq.fs.local.LocalFileSystem.getInstance();
        ftraq.fs.LgFileSystem remoteFileSystem = ftraq.fs.ftp.FtpFileSystem.getInstance(i_site.getHost(), i_site.getPort(), i_site.getUsername(), i_site.getPassword());
        String localDir = i_site.getLocalDir();
        if (localDir == null || localDir.equals("")) {
            localBrowser = new ftraq.fs.LgFileSystemBrowserImpl(localFileSystem);
        } else {
            localBrowser = new ftraq.fs.LgFileSystemBrowserImpl(localFileSystem, localDir);
        }
        String remoteDir = i_site.getRemoteDir();
        if (remoteDir == null || remoteDir.equals("")) {
            remoteBrowser = new ftraq.fs.LgFileSystemBrowserImpl(remoteFileSystem);
        } else {
            remoteBrowser = new ftraq.fs.LgFileSystemBrowserImpl(remoteFileSystem, remoteDir);
        }
        LgNavigationSession newSession = new LgNavigationSessionImpl(localBrowser, remoteBrowser);
        synchronized (this._navigationSessionList) {
            this._navigationSessionList.add(newSession);
        }
        this.notifyObserversCompleteUpdate();
        this.notifyObserversCompleteUpdate();
        return newSession;
    }

    public ftraq.transferqueue.LgTransferQueueItem createQueueItem(String i_sourceUrl, String i_targetDirectoryUrl) {
        ftraq.transferqueue.LgTransferQueueItem newItem = new ftraq.transferqueue.LgTransferQueueItemImpl(i_sourceUrl, i_targetDirectoryUrl, this.getQueue());
        this._transferQueue.addQueueItem(newItem);
        return newItem;
    }

    public void closeNavigationSession(LgNavigationSession i_session) {
        synchronized (this._navigationSessionList) {
            this._navigationSessionList.remove(i_session);
        }
        i_session.closeSession();
        this.notifyObserversCompleteUpdate();
    }

    public LgNavigationSession[] getActiveNavigationSessions() {
        synchronized (this._navigationSessionList) {
            return (LgNavigationSession[]) this._navigationSessionList.toArray(new LgNavigationSession[this._navigationSessionList.size()]);
        }
    }

    private UrlPattern getMatchingPattern(String i_URL) throws InvalidURLExc {
        logger.info("trying to understand URL " + i_URL);
        UrlPattern userNamePattern = new UrlPatternUsername();
        if (userNamePattern.fits(i_URL)) {
            logger.info("it's an url with user name: ");
            logger.info(userNamePattern.getUsername() + "; " + userNamePattern.getPassword() + "; " + userNamePattern.getHostname() + "; " + userNamePattern.getPortNr() + "; " + userNamePattern.getAbsolutePath());
            return userNamePattern;
        }
        UrlPattern simplePattern = new UrlPatternAnonymous();
        if (simplePattern.fits(i_URL)) {
            logger.info("it's an url without user name: ");
            logger.info(simplePattern.getHostname() + "; " + simplePattern.getPortNr() + "; " + simplePattern.getAbsolutePath());
            return simplePattern;
        }
        throw new InvalidURLExc(i_URL, "the url doesn't match any known URL pattern");
    }

    public void closeApplication() {
        try {
            this._settings.commit();
            this._bookmarkLibrary.save();
            this._transferQueue.saveQueue();
        } catch (Exception e) {
            logger.error("failed to save the settings ", e);
        }
        System.exit(0);
    }

    private String _parsePathNameFromLocalUrl(String i_localURL) {
        int beginningOfPath = 0;
        if (i_localURL.lastIndexOf("//") != -1) {
            beginningOfPath = i_localURL.lastIndexOf("//") + 2;
        } else {
            beginningOfPath = i_localURL.indexOf(":/") + 2;
        }
        String path = i_localURL.substring(beginningOfPath);
        if (i_localURL.indexOf("\\") == -1 && path.startsWith("/") == false) {
            path = "/" + path;
        }
        return path;
    }

    public ftraq.fs.LgDirectoryEntry createDirectoryEntryFromUrl(String i_URL) throws InvalidURLExc, ftraq.fs.exceptions.GetDirectoryEntryFailure {
        try {
            if (i_URL.startsWith("file:/")) {
                ftraq.fs.LgFileSystem fileSystem = ftraq.fs.local.LocalFileSystem.getInstance();
                int beginningOfPath = 0;
                if (i_URL.lastIndexOf("//") != -1) {
                    beginningOfPath = i_URL.lastIndexOf("//") + 2;
                } else {
                    beginningOfPath = i_URL.indexOf(":/") + 2;
                }
                String path = this._parsePathNameFromLocalUrl(i_URL);
                return fileSystem.getDirectoryEntry(path);
            }
            UrlPattern urlPattern = this.getMatchingPattern(i_URL);
            ftraq.fs.LgFileSystem fileSystem = null;
            String protocol = urlPattern.getProtocol().toLowerCase();
            if (protocol.equals("ftp")) {
                try {
                    int portNr = 21;
                    try {
                        portNr = Integer.parseInt(urlPattern.getPortNr());
                    } catch (Exception e) {
                    }
                    String userName = urlPattern.getUsername();
                    String password = urlPattern.getPassword();
                    if (userName == null) {
                        userName = "anonymous";
                        password = "abc@def.de";
                    }
                    fileSystem = ftraq.fs.ftp.FtpFileSystem.getInstance(urlPattern.getHostname(), portNr, userName, password);
                } catch (Exception e) {
                    throw new InvalidURLExc(i_URL, "failed to get neccessary information from url: " + e.getClass().getName() + ": " + e.getMessage());
                }
            }
            String absolutePath = urlPattern.getAbsolutePath();
            if (absolutePath == null) {
                absolutePath = "/";
            }
            fileSystem.makeAvailable();
            return fileSystem.getDirectoryEntry(absolutePath);
        } catch (InvalidURLExc e) {
            throw e;
        } catch (Exception e) {
            throw new ftraq.fs.exceptions.GetDirectoryEntryFailure(e, i_URL);
        }
    }

    public ftraq.fs.LgDirectoryEntry createLocalDirectoryEntry(String i_URL, String i_systemIDString) throws ftraq.fs.exceptions.DirectoryMappingFailure, InvalidURLExc, ftraq.fs.exceptions.GetDirectoryEntryFailure {
        logger.info("trying to create a directory entry for " + i_URL + " on " + i_systemIDString);
        if (i_URL.startsWith("file:/") == false) {
            throw new InvalidURLExc(i_URL, "local urls must start with 'file:/'");
        }
        String pathName = this._parsePathNameFromLocalUrl(i_URL);
        logger.info("the absolute path name is '" + pathName + "'");
        ftraq.settings.LgSystemID systemIDForTheUrl = this._settings.getSystemID(i_systemIDString);
        if (systemIDForTheUrl == null) {
            throw new ftraq.fs.exceptions.DirectoryMappingFailure("unknown system id string: " + i_systemIDString);
        }
        logger.info("this URL was created on system " + systemIDForTheUrl.getName());
        if (systemIDForTheUrl.equals(this._settings.getCurrentSystemID())) {
            return this.createDirectoryEntryFromUrl(i_URL);
        }
        ftraq.settings.LgDirectoryMap map = this._settings.findDirectoryMappingForPathName(systemIDForTheUrl.getName(), pathName);
        if (map == null) {
            throw new ftraq.fs.exceptions.DirectoryMappingFailure("no directory mapping matches the path " + pathName);
        }
        String originalPartitionName = map.getDirectoryMap().get(systemIDForTheUrl.getName());
        if (originalPartitionName == null) {
            throw new ftraq.fs.exceptions.DirectoryMappingFailure("there is no entry for the path " + pathName + " in the directory mapping");
        }
        logger.info("the path name matches the '" + originalPartitionName + "' entry in the mapping list");
        String mappedPartitionName = map.getDirectoryMap().get(this.getSettings().getCurrentSystemID().getName());
        if (mappedPartitionName == null) {
            throw new ftraq.fs.exceptions.DirectoryMappingFailure("the partition " + originalPartitionName + " on " + systemIDForTheUrl.getName() + " is not available on the current system");
        }
        logger.info("that partition should be available as '" + mappedPartitionName + "' on the current system");
        try {
            ftraq.fs.LgDirectory mappedDirectory = ftraq.fs.local.LocalFileSystem.getInstance().getDirectory(mappedPartitionName);
            mappedDirectory.getDirectoryListing();
            logger.info("the local directory '" + mappedDirectory + "' exists.");
        } catch (Exception e) {
            throw new ftraq.fs.exceptions.DirectoryMappingFailure(e, "failed to access the mapped partition " + mappedPartitionName);
        }
        String mappedPathName = mappedPartitionName + pathName.substring(pathName.indexOf(originalPartitionName) + originalPartitionName.length());
        logger.info("the new absolut path name is '" + mappedPathName + "'");
        String finalMappedPathName = ftraq.fs.local.LocalFileSystem.getInstance().replacePathSeparators(mappedPathName);
        logger.info("finally, with replaced slashes it is '" + finalMappedPathName + "'");
        return ftraq.fs.local.LocalFileSystem.getInstance().getDirectoryEntry(finalMappedPathName);
    }

    public LgBookmarkItem createBookmarkEntryFromUrl(String i_URL) throws InvalidURLExc {
        UrlPattern urlPattern = this.getMatchingPattern(i_URL);
        LgBookmarkItem bookmarkEntry = new LgBookmarkItemImpl();
        if (urlPattern.getProtocol() != null && urlPattern.getProtocol().toLowerCase().equals("ftp") == false) {
            throw new InvalidURLExc(i_URL, "the only currently supported protocol is ftp");
        }
        try {
            bookmarkEntry.setHost(urlPattern.getHostname());
            if (urlPattern.getPortNr() != null) {
                try {
                    bookmarkEntry.setPort(Integer.parseInt(urlPattern.getPortNr()));
                } catch (Exception e) {
                    throw new InvalidURLExc(i_URL, "failed to set port numer on bookmark entry:\n" + e.getMessage());
                }
            } else {
                bookmarkEntry.setPort(21);
            }
            if (urlPattern.getAbsolutePath() != null) {
                bookmarkEntry.setRemoteDir(urlPattern.getAbsolutePath());
            }
            if (urlPattern.getUsername() != null) {
                bookmarkEntry.setUsername(urlPattern.getUsername());
            }
            if (urlPattern.getUsername() != null) {
                bookmarkEntry.setPassword(urlPattern.getPassword());
            }
        } catch (UpdateNodeAttributeFailure up) {
            throw new InvalidURLExc(i_URL, "failed to update node attribute:\n" + up.getMessages());
        }
        return bookmarkEntry;
    }

    private void _createDefaultSystemID() {
        String currentSystemIDString = this.getSettings().getCurrentSystemString();
        this.getSettings().addNewSystemID(System.getProperty("os.name"), currentSystemIDString);
    }

    /**
    * attach an UiObserver that should be informed whenever the subjects state changes
    */
    public void addObserver(ftraq.gui.UiObserver i_observer) {
        this._uiObserverSet.add(i_observer);
    }

    /**
     * remove an UiObserver from the list of observers to be informed
     */
    public void removeObserver(ftraq.gui.UiObserver i_observer) {
        this._uiObserverSet.remove(i_observer);
    }

    /**
     * notfiy all attached UiObservers that the complete GUI needs to be updated
     */
    public void notifyObserversCompleteUpdate() {
        java.util.Iterator it = this._uiObserverSet.iterator();
        while (it.hasNext()) {
            ((ftraq.gui.UiObserver) it.next()).updateGui();
        }
    }

    /**
     * notfiy all attached UiObservers that the status line should be updated.
     */
    public void notifyObserversUpdateStatusLine() {
        java.util.Iterator it = this._uiObserverSet.iterator();
        while (it.hasNext()) {
            ((ftraq.gui.UiObserver) it.next()).updateStatusLine();
        }
    }

    public ftraq.fs.LgTextFileEditor createTextfileEditor() {
        return new ftraq.fs.LgTextFileEditorImpl();
    }
}

class UrlElementNotAvailableExc extends ftraq.FTraqExc {

    UrlElementNotAvailableExc() {
        super("one element of the url is missing", null);
    }
}

abstract class UrlPattern {

    protected String _protocol;

    protected String _username;

    protected String _password;

    protected String _hostname;

    protected String _portnr;

    protected String _absolutePath;

    abstract boolean fits(String i_url);

    String getProtocol() {
        return this._protocol;
    }

    String getUsername() {
        return this._username;
    }

    String getPassword() {
        return this._password;
    }

    String getHostname() {
        return this._hostname;
    }

    String getPortNr() {
        return this._portnr;
    }

    String getAbsolutePath() {
        return this._absolutePath;
    }

    protected String findAndRemoveProtocol(StringBuffer io_urlBuffer) {
        int indexOfUrlSeparator = io_urlBuffer.toString().indexOf("://");
        if (indexOfUrlSeparator != -1) {
            String protocol = io_urlBuffer.substring(0, indexOfUrlSeparator);
            io_urlBuffer.delete(0, indexOfUrlSeparator + 3);
            return protocol;
        }
        return null;
    }

    protected String findAndRemoveHostName(StringBuffer io_urlBuffer) {
        int indexOfSlash = io_urlBuffer.toString().indexOf('/');
        int indexOfColon = io_urlBuffer.toString().indexOf(':');
        int endOfHostName = indexOfSlash;
        if (indexOfSlash == -1) {
            endOfHostName = io_urlBuffer.length();
        }
        if (indexOfColon > -1 && indexOfColon < endOfHostName) {
            endOfHostName = indexOfColon;
        }
        String hostName = io_urlBuffer.substring(0, endOfHostName);
        io_urlBuffer.delete(0, endOfHostName);
        return hostName.toString();
    }

    protected String findAndRemovePortNr(StringBuffer io_urlBuffer) {
        int indexOfSlash = io_urlBuffer.toString().indexOf('/');
        if (indexOfSlash == -1) indexOfSlash = io_urlBuffer.length();
        String portNrString = io_urlBuffer.substring(1, indexOfSlash);
        io_urlBuffer.delete(0, indexOfSlash);
        return portNrString;
    }

    protected String findAndRemoveUsername(StringBuffer io_urlBuffer) {
        int indexOfAt = io_urlBuffer.toString().lastIndexOf('@');
        int indexOfColon = io_urlBuffer.toString().indexOf(':');
        int endOfUsername = indexOfAt;
        if (indexOfColon < indexOfAt && indexOfColon != -1) {
            endOfUsername = indexOfColon;
        }
        String username = io_urlBuffer.substring(0, indexOfColon);
        io_urlBuffer.delete(0, endOfUsername);
        return username;
    }

    protected String findAndRemovePassword(StringBuffer io_urlBuffer) {
        int endOfPassword = io_urlBuffer.toString().lastIndexOf('@');
        String password = io_urlBuffer.substring(1, endOfPassword);
        io_urlBuffer.delete(0, endOfPassword + 1);
        return password;
    }
}

class UrlPatternUsername extends UrlPattern {

    boolean fits(String i_url) {
        try {
            StringBuffer urlBuffer = new StringBuffer(i_url);
            super._protocol = super.findAndRemoveProtocol(urlBuffer);
            super._username = super.findAndRemoveUsername(urlBuffer);
            if (urlBuffer.charAt(0) == ':') {
                super._password = super.findAndRemovePassword(urlBuffer);
            }
            super._hostname = super.findAndRemoveHostName(urlBuffer);
            if (urlBuffer.length() == 0) {
                return true;
            }
            if (urlBuffer.charAt(0) == ':') {
                super._portnr = super.findAndRemovePortNr(urlBuffer);
            }
            if (urlBuffer.length() != 0) {
                super._absolutePath = urlBuffer.toString();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

class UrlPatternAnonymous extends UrlPattern {

    boolean fits(String i_url) {
        try {
            StringBuffer urlBuffer = new StringBuffer(i_url);
            super._protocol = super.findAndRemoveProtocol(urlBuffer);
            super._hostname = super.findAndRemoveHostName(urlBuffer);
            if (urlBuffer.length() == 0) {
                return true;
            }
            if (urlBuffer.charAt(0) == ':') {
                super._portnr = super.findAndRemovePortNr(urlBuffer);
            }
            if (urlBuffer.length() != 0) {
                super._absolutePath = urlBuffer.toString();
            }
            super._username = "anonymous";
            super._password = "abc@def.de";
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
