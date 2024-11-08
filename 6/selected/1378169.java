package uips.support.storage.impl.ftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import uips.support.Consts;
import uips.support.localization.IMessagesInstance;
import uips.support.logging.ILogInstance;
import uips.support.settings.ISettingsInstance;
import uips.support.storage.IFileUtils;
import uips.support.storage.exceptions.StorageException;
import uips.support.storage.interfaces.IUipFile;
import uips.support.storage.interfaces.IUipFilesStorageAccess;

/**
 * Class providing acces for UIP application files stored on ftp server. All
 * downloaded files are first stored to discs chache and then readed from cache.
 *
 * @author Jindrich Basek (basekjin@fit.cvut.cz, CTU Prague, FIT)
 */
public class FtpStorageAccess implements IUipFilesStorageAccess {

    /**
     * Address of ftp server
     */
    private String host;

    /**
     * Port of ftp server
     */
    private int port;

    /**
     * Username for log in to ftp server
     */
    private String username;

    /**
     * Password for log in to ftp server
     */
    private String password;

    /**
     * Path to directory with UIP applications on ftp server.
     */
    private String appsPath;

    /**
     * List with objects providing connections to ftp server
     */
    private List<FtpConnection> connections;

    /**
     * Semaphore guarding access to non used FtpConnections
     */
    private Semaphore semaphore;

    private ILogInstance log;

    private IMessagesInstance messages;

    private ISettingsInstance settings;

    private int simultaneousConnections;

    private IFileUtils fileUtils;

    public FtpStorageAccess() {
    }

    /**
     * Disposes connection to storage, cancel all connections to ftp server
     */
    @Override
    public void dispose() {
        for (FtpConnection ftpConnection : this.connections) {
            ftpConnection.dispose();
        }
    }

    /**
     * Returns list with files witch given extensions included in folder specified
     * by dir parameter and its subfolders.
     *
     * @param dir dir where start looking for files<br>
     * dir path example: uipapplication/media
     * @param allowedExtensions array with allowed extensions - extension are without . (dot)
     * @return list with files witch given extensions included in folder specified
     * by dir parameter and its subfolders.
     * @throws InterruptedException recurse dir lookup interrupted
     */
    @Override
    public List<String> recurseDir(String dir, String allowedExtensions[]) throws InterruptedException {
        List<String> files = new ArrayList<String>(50);
        try {
            getFtpConnection().recurseDir(files, dir, allowedExtensions);
        } catch (IOException ex) {
            this.log.write(Level.SEVERE, ex.toString());
        }
        return files;
    }

    /**
     * Returns non used object providing connection to ftp server. If no free connection
     * is available and exists used connections, method is blocket until some
     * connection is available. If all existing connections are used and is possible
     * to establish new connection to ftp server (depends on
     * Settings.getStorageSimultaneousConnections() - maximum count of simultaneous
     * connections), new conncetion is created and returned.<br>
     * If user wants to use FtpConnection, must call this method.
     * Returned object can be used only ones. After calling some method of this
     * FtpConnection object, object is marked as available for use for others.
     * Behawiour after calling some method of this object for second time
     * is not specified and deadlock or error can ocure.
     * If user wants to call another method, he must call method getFtpConnection
     * again.<br>
     * This does not aply for method of FtpConnection getInputStream. After
     * calling this method FtpConnection stay locked and must be released by
     * method endInputStream. Betwen those two methods of FtpConnection object
     * other methods of FtpConnection object can not be invoked. Behawiour after
     * calling some method of this object between those two is not specified
     * and deadlock or error can ocure.
     *
     * @return non used object providing connection to ftp server
     * @throws IOException can not establish any connection
     * @throws InterruptedException waiting for free connection interrupted
     */
    protected synchronized FtpConnection getFtpConnection() throws IOException, InterruptedException {
        while (true) {
            synchronized (this.connections) {
                for (FtpConnection ftpConnection : this.connections) {
                    if (ftpConnection.lockAvailable()) {
                        this.semaphore.tryAcquire();
                        return ftpConnection;
                    }
                }
                if (this.connections.size() < this.simultaneousConnections) {
                    try {
                        FtpConnection newConnection = new FtpConnection();
                        this.connections.add(newConnection);
                        newConnection.lockAvailable();
                        return newConnection;
                    } catch (StorageException ex) {
                        if (this.connections.isEmpty()) {
                            throw new IOException();
                        }
                    }
                }
            }
            this.semaphore.acquire();
        }
    }

    /**
     * Constructs object implementing IUipFile interface that provides access
     * to specific file, reads file content
     *
     * @param filePath path to file<br>
     * file path example: uipapplication/media/image.jpg<br>
     * dir path example: uipapplication/media
     * @return object implementing IUipFile interface that provides access
     * to specific file, reads file content
     */
    @Override
    public IUipFile getFile(String filePath) {
        return new FtpUipFile(this, this.appsPath, filePath, this.fileUtils);
    }

    /**
     * Instance of this class provide connection to ftp server and methods
     * for downloading files from ftp server and getting informations about
     * them.
     *
     * @author Jindrich Basek (basekjin@fit.cvut.cz, CTU Prague, FIT)
     */
    class FtpConnection {

        /**
         * ftp client
         */
        private final FTPClient ftpClient;

        /**
         * If true FtpConnection is not used and available for use
         */
        private Boolean available;

        /**
         * Make this FtpConnection used
         *
         * @return true if connection was locked for caller, otherwise false
         * if connection is in use yet
         */
        public boolean lockAvailable() {
            synchronized (this.available) {
                if (this.available.booleanValue()) {
                    this.available = false;
                    return true;
                }
                return false;
            }
        }

        /**
         * Make this FtpConnection unused
         */
        public void unlockAvailable() {
            synchronized (this.available) {
                this.available = true;
                getSemaphore().release();
            }
        }

        /**
         * Makes ne instance of FtpConnection
         *
         * @throws StorageException Error connecting to ftp server
         */
        public FtpConnection() throws StorageException {
            this.ftpClient = new FTPClient();
            this.available = true;
            try {
                connectFtp();
            } catch (IOException ex) {
                throw new StorageException(ex.toString());
            }
        }

        /**
         * Connects to ftp server.
         *
         * @throws IOException Error connecting to ftp
         */
        protected final void connectFtp() throws IOException {
            try {
                if (!this.ftpClient.isConnected()) {
                    this.ftpClient.connect(getHost(), getPort());
                    getLog().write(Level.INFO, String.format(getMessages().getString("FtpSuccessfullyConnected"), getHost()));
                    int reply = this.ftpClient.getReplyCode();
                    if (!FTPReply.isPositiveCompletion(reply)) {
                        this.ftpClient.disconnect();
                        throw new IOException(String.format(getMessages().getString("FtpErrorConnectingRefused"), getHost()));
                    }
                    if (getUsername() != null) {
                        if (!this.ftpClient.login(getUsername(), getPassword())) {
                            this.ftpClient.logout();
                            disconnectFtp();
                            throw new IOException(String.format(getMessages().getString("FtpErrorAuthorizing"), getHost()));
                        }
                    }
                    this.ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    this.ftpClient.enterLocalPassiveMode();
                    getLog().write(Level.INFO, String.format(getMessages().getString("FtpSuccessfullyAuthorized"), getHost()));
                }
            } catch (IOException ex) {
                disconnectFtp();
                throw new IOException(String.format(getMessages().getString("FtpErrorConnecting"), getHost(), ex.toString()));
            }
        }

        /**
         * Disconnects from ftp server and removes this instance of FtpConnection.
         */
        protected final void disconnectFtp() {
            if (this.ftpClient != null && this.ftpClient.isConnected()) {
                try {
                    synchronized (getConnections()) {
                        getConnections().remove(this);
                        this.ftpClient.disconnect();
                        getLog().write(Level.INFO, String.format(getMessages().getString("FtpDisconnected"), getHost()));
                    }
                } catch (IOException ee) {
                }
            }
        }

        /**
         * Disconnects from ftp server.
         */
        protected final void dispose() {
            if (this.ftpClient != null && this.ftpClient.isConnected()) {
                try {
                    this.ftpClient.disconnect();
                    getLog().write(Level.INFO, String.format(getMessages().getString("FtpDisconnected"), getHost()));
                } catch (IOException ee) {
                }
            }
        }

        /**
         * Downoads file from ftp server and stores it in disc cache. If file is
         * stored in disc cache yet and is same like on ftp server file is not
         * downloaded.
         *
         * @param absoluteFilePath absolute path to file on ftp server
         * @param filePath relative path to file on ftp server
         * @throws IOException Error downloading file
         */
        public void cacheFile(String absoluteFilePath, String filePath) throws IOException {
            try {
                try {
                    connectFtp();
                    File file = new File(getSettings().getStorageCacheFolder() + Consts.FileSeparator + filePath.replace('/', Consts.FileSeparatorChar));
                    long lastMod = lastModificationPrivate(absoluteFilePath).getTimeInMillis();
                    if (!file.exists() || (file.exists() && file.lastModified() != lastMod)) {
                        file.mkdirs();
                        file.delete();
                        file.createNewFile();
                        FileOutputStream fos = new FileOutputStream(file);
                        this.ftpClient.retrieveFile(absoluteFilePath, fos);
                        fos.close();
                        file.setLastModified(lastMod);
                        getLog().write(Level.FINE, String.format(getMessages().getString("FileCached"), filePath));
                    }
                } catch (IOException e) {
                    getLog().write(Level.SEVERE, e.getLocalizedMessage());
                    throw e;
                }
            } finally {
                unlockAvailable();
            }
        }

        /**
         * Checks if file is directory. Returns nothing.
         * Throws exception if directory does not exist and is not readable.
         *
         * @param absoluteFilePath path to file
         * @throws IOException Problem openning or reading directory
         */
        public void readableDirectory(String absoluteFilePath) throws IOException {
            try {
                connectFtp();
                FTPFile list[] = null;
                try {
                    list = this.ftpClient.listFiles(absoluteFilePath);
                } catch (IOException ex) {
                    disconnectFtp();
                    throw ex;
                }
                if (list == null) {
                    throw new IOException();
                }
                if (list.length == 1) {
                    if (list[0].isFile() && list[0].getName().equals(absoluteFilePath.substring(absoluteFilePath.lastIndexOf('/') + 1, absoluteFilePath.length()))) {
                        throw new IOException();
                    }
                }
            } finally {
                unlockAvailable();
            }
        }

        /**
         * Returns length of file in bytes.
         *
         * @param absoluteFilePath path to file
         * @return length of file in bytes
         * @throws IOException Problem openning or reading file
         */
        public long length(String absoluteFilePath) throws IOException {
            try {
                connectFtp();
                FTPFile list[] = null;
                try {
                    list = this.ftpClient.listFiles(absoluteFilePath);
                } catch (IOException ex) {
                    disconnectFtp();
                    throw ex;
                }
                if (list == null) {
                    throw new IOException();
                }
                if (list.length == 1) {
                    if (list[0].isFile() && list[0].getName().equals(absoluteFilePath.substring(absoluteFilePath.lastIndexOf('/') + 1, absoluteFilePath.length()))) {
                        return list[0].getSize();
                    }
                }
                throw new IOException();
            } finally {
                unlockAvailable();
            }
        }

        /**
         * Returns last modification time of file.
         *
         * @param absoluteFilePath path to file
         * @return timestamp - time of last mod
         * @throws IOException Problem openning or reading file
         */
        public Calendar lastModification(String absoluteFilePath) throws IOException {
            try {
                connectFtp();
                return lastModificationPrivate(absoluteFilePath);
            } finally {
                unlockAvailable();
            }
        }

        /**
         * Returns last modification time of file. Does not unlock FtpConnection.
         *
         * @param absoluteFilePath path to file
         * @return timestamp - time of last mod
         * @throws IOException Problem openning or reading file
         */
        private Calendar lastModificationPrivate(String absoluteFilePath) throws IOException {
            FTPFile list[] = null;
            try {
                list = this.ftpClient.listFiles(absoluteFilePath);
            } catch (IOException ex) {
                disconnectFtp();
                throw ex;
            }
            if (list == null) {
                throw new IOException();
            }
            if (list.length == 1) {
                if (list[0].isFile() && list[0].getName().equals(absoluteFilePath.substring(absoluteFilePath.lastIndexOf('/') + 1, absoluteFilePath.length()))) {
                    return list[0].getTimestamp();
                }
            }
            throw new IOException();
        }

        /**
         * Returns list with files witch given extensions included in folder specified
         * by dir parameter and its subfolders.
         *
         * @param files List with files
         * @param dir dir where start looking for files<br>
         * dir path example: uipapplication/media
         * @param allowedExtensions array with allowed extensions - extension are without . (dot)
         * @throws IOException Problem openning or reading file
         */
        public void recurseDir(List<String> files, String dir, String allowedExtensions[]) throws IOException {
            try {
                String wDir = getAppsPath() + "/" + dir;
                recurseDirFirst(wDir, files, allowedExtensions);
            } finally {
                unlockAvailable();
            }
        }

        /**
         * Prodecure of recurse direktory lookup. Searchs for files in folder and its subfolders.
         *
         * @param item Directory for lookup
         * @param files List with files
         * @param allowedExtensions array with allowed extensions - extension are without . (dot)
         * @throws IOException Problem openning or reading file
         */
        private void recurseDirFirst(String item, List<String> files, String allowedExtensions[]) throws IOException {
            FTPFile list[] = null;
            try {
                list = this.ftpClient.listFiles(item);
            } catch (IOException ex) {
                disconnectFtp();
                throw ex;
            }
            if (list == null) {
                return;
            }
            int offset = getAppsPath().length() + 1;
            for (int i = 0; i < list.length; i++) {
                for (int j = 0; j < allowedExtensions.length; j++) {
                    if (list[i].isFile() && list[i].getName().toLowerCase().endsWith("." + allowedExtensions[j])) {
                        files.add(item.substring(offset) + (item.substring(offset).endsWith("/") ? "" : "/") + list[i].getName());
                        break;
                    }
                }
                if (list[i].isDirectory() && !list[i].getName().startsWith(".svn")) {
                    recurseDirFirst((item.endsWith("/") ? item : item + "/") + list[i].getName(), files, allowedExtensions);
                }
            }
        }

        /**
         * Returns InputStream for file stored on ftp server. Does not unlock
         * FtpConnection.
         *
         * @param absoluteFilePath absolute path to file on ftp server
         * @return InputStream for file stored on ftp server
         * @throws IOException Problem retreiving InputStream
         */
        public InputStream getInputStream(String absoluteFilePath) throws IOException {
            connectFtp();
            return this.ftpClient.retrieveFileStream(absoluteFilePath);
        }

        /**
         * Unlock connection previously locked by getInputStream method and completes
         * comunication with ftp server.
         *
         * @throws IOException Error ocured
         */
        public void endInputStream() throws IOException {
            try {
                this.ftpClient.completePendingCommand();
            } finally {
                unlockAvailable();
            }
        }
    }

    @Override
    public void initialize(IFileUtils fileUtils1, ILogInstance log1, IMessagesInstance messages1, ISettingsInstance settings1) throws StorageException {
        this.fileUtils = fileUtils1;
        this.log = log1;
        this.messages = messages1;
        this.settings = settings1;
        Map<String, String> settingsMap = this.settings.getFileStorage().getPropertiesMap();
        String connectionString = settingsMap.get(Consts.UipsSettingNameConnectionString);
        URI appsUri;
        try {
            this.simultaneousConnections = Integer.parseInt(settingsMap.get(Consts.UipsSettingNameStorageSimultaneousConnections));
            appsUri = new URI(connectionString);
        } catch (Exception e) {
            throw new StorageException(e);
        }
        String path = this.settings.getApplicationsRoot();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (!appsUri.getScheme().toLowerCase().equals("ftp")) {
            throw new StorageException(String.format(this.messages.getString("NotSupporetTypeOfUipAppliactionsStorage"), appsUri.getScheme()));
        }
        this.host = appsUri.getHost();
        if (this.port == 0) {
            this.port = 21;
        }
        this.port = appsUri.getPort();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        this.appsPath = path;
        String userInfos[] = appsUri.getUserInfo().split(":");
        if (userInfos.length > 0) {
            this.username = userInfos[0];
            if (userInfos.length > 1) {
                this.password = userInfos[1];
            } else {
                this.password = "";
            }
        } else {
            this.username = null;
            this.password = "";
        }
        this.semaphore = new Semaphore(1);
        this.connections = new ArrayList<FtpConnection>(this.simultaneousConnections);
        this.connections.add(new FtpConnection());
    }

    public String getAppsPath() {
        return this.appsPath;
    }

    public List<FtpConnection> getConnections() {
        return this.connections;
    }

    public ILogInstance getLog() {
        return this.log;
    }

    public String getHost() {
        return this.host;
    }

    public IMessagesInstance getMessages() {
        return this.messages;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public int getPort() {
        return this.port;
    }

    public Semaphore getSemaphore() {
        return this.semaphore;
    }

    public ISettingsInstance getSettings() {
        return this.settings;
    }
}
