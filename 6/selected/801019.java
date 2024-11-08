package com.kni.etl.ketl.reader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import com.kni.etl.dbutils.ResourcePool;
import com.kni.etl.ketl.ETLOutPort;
import com.kni.etl.ketl.ETLStep;
import com.kni.etl.ketl.exceptions.KETLReadException;
import com.kni.etl.ketl.exceptions.KETLThreadException;
import com.kni.etl.ketl.smp.DefaultReaderCore;
import com.kni.etl.ketl.smp.ETLThreadManager;
import com.kni.etl.util.XMLHelper;
import com.kni.util.net.ftp.DefaultFTPFileListParser;
import com.kni.util.net.ftp.FTP;
import com.kni.util.net.ftp.FTPClient;
import com.kni.util.net.ftp.FTPConnectionClosedException;
import com.kni.util.net.ftp.FTPFile;
import com.kni.util.net.ftp.FTPReply;

/**
 * The Class FTPFileFetcher.
 * 
 * @author nwakefield Creation Date: Mar 17, 2003
 */
public class FTPFileFetcher extends ETLReader implements DefaultReaderCore {

    @Override
    protected String getVersion() {
        return "$LastChangedRevision: 491 $";
    }

    /**
	 * Instantiates a new FTP file fetcher.
	 * 
	 * @param pXMLConfig
	 *            the XML config
	 * @param pPartitionID
	 *            the partition ID
	 * @param pPartition
	 *            the partition
	 * @param pThreadManager
	 *            the thread manager
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public FTPFileFetcher(Node pXMLConfig, int pPartitionID, int pPartition, ETLThreadManager pThreadManager) throws KETLThreadException {
        super(pXMLConfig, pPartitionID, pPartition, pThreadManager);
    }

    /**
	 * The Class FTPWorkerThread.
	 */
    class FTPWorkerThread extends Thread {

        /** The Constant ASCII. */
        public static final String ASCII = "ASCII";

        /** The Constant FALSE. */
        public static final String FALSE = "FALSE";

        /** The b shutdown. */
        protected boolean bShutdown = false;

        /** The ll pending queue. */
        protected LinkedList llPendingQueue = null;

        /** The sleep period. */
        protected int iSleepPeriod = 100;

        /** The b file downloaded. */
        protected boolean bFileDownloaded = false;

        /** The file name. */
        public String fileName;

        /** The user. */
        public String user;

        /** The server. */
        public String server;

        /** The password. */
        private String password;

        /** The transfer type. */
        public String transferType;

        /** The file size. */
        public long fileSize;

        /** The download time. */
        public long downloadTime;

        /** The dest file name. */
        public String destFileName;

        /** The passive mode. */
        String passiveMode;

        /** The last modified date. */
        Long lastModifiedDate;

        /**
		 * ETLJobExecutorThread constructor comment.
		 */
        public FTPWorkerThread() {
            super();
        }

        /**
		 * ETLJobExecutorThread constructor comment.
		 * 
		 * @param target
		 *            java.lang.Runnable
		 */
        public FTPWorkerThread(Runnable target) {
            super(target);
        }

        /**
		 * ETLJobExecutorThread constructor comment.
		 * 
		 * @param target
		 *            java.lang.Runnable
		 * @param name
		 *            java.lang.String
		 */
        public FTPWorkerThread(Runnable target, String name) {
            super(target, name);
        }

        /**
		 * ETLJobExecutorThread constructor comment.
		 * 
		 * @param name
		 *            java.lang.String
		 */
        public FTPWorkerThread(String name) {
            super(name);
        }

        /**
		 * ETLJobExecutorThread constructor comment.
		 * 
		 * @param group
		 *            java.lang.ThreadGroup
		 * @param target
		 *            java.lang.Runnable
		 */
        public FTPWorkerThread(ThreadGroup group, Runnable target) {
            super(group, target);
        }

        /**
		 * ETLJobExecutorThread constructor comment.
		 * 
		 * @param group
		 *            java.lang.ThreadGroup
		 * @param target
		 *            java.lang.Runnable
		 * @param name
		 *            java.lang.String
		 */
        public FTPWorkerThread(ThreadGroup group, Runnable target, String name) {
            super(group, target, name);
        }

        /**
		 * ETLJobExecutorThread constructor comment.
		 * 
		 * @param group
		 *            java.lang.ThreadGroup
		 * @param name
		 *            java.lang.String
		 */
        public FTPWorkerThread(ThreadGroup group, String name) {
            super(group, name);
        }

        /**
		 * Insert the method's description here. Creation date: (5/3/2002
		 * 6:49:24 PM)
		 * 
		 * @param strUser
		 *            the str user
		 * @param strPassword
		 *            the str password
		 * @param strServer
		 *            the str server
		 * @param strTransferType
		 *            the str transfer type
		 * @param strFileName
		 *            the str file name
		 * @param strDestFileName
		 *            the str dest file name
		 * @param strPassiveMode
		 *            the str passive mode
		 * @param lExpectedSize
		 *            the l expected size
		 * @param lModifiedDate
		 *            the l modified date
		 * 
		 * @return boolean
		 */
        public boolean getFile(String strUser, String strPassword, String strServer, String strTransferType, String strFileName, String strDestFileName, String strPassiveMode, Long lExpectedSize, Long lModifiedDate) {
            this.server = strServer;
            this.user = strUser;
            this.password = strPassword;
            this.fileName = strFileName;
            this.transferType = strTransferType;
            this.fileSize = lExpectedSize.longValue();
            this.passiveMode = strPassiveMode;
            char pathSeperator = '\\';
            this.lastModifiedDate = lModifiedDate;
            String destFileName = strFileName;
            if (strFileName != null) {
                int endOfPath = strFileName.lastIndexOf(pathSeperator);
                if (endOfPath == -1) {
                    pathSeperator = '/';
                    endOfPath = strFileName.lastIndexOf(pathSeperator);
                }
                if (endOfPath != -1) {
                    destFileName = strFileName.substring(endOfPath + 1);
                }
            }
            pathSeperator = '\\';
            if (strDestFileName != null) {
                String pathName = null;
                int endOfPath = strDestFileName.lastIndexOf(pathSeperator);
                if (endOfPath == -1) {
                    pathSeperator = '/';
                    endOfPath = strDestFileName.lastIndexOf(pathSeperator);
                }
                if (endOfPath != -1) {
                    pathName = strDestFileName.substring(0, endOfPath);
                }
                if (pathName != null) {
                    if (endOfPath == (pathName.length() - 1)) {
                        this.destFileName = pathName + destFileName;
                    } else {
                        this.destFileName = pathName + pathSeperator + destFileName;
                    }
                } else {
                    this.destFileName = destFileName;
                }
            } else {
                this.destFileName = strFileName;
            }
            return true;
        }

        /**
		 * Insert the method's description here. Creation date: (5/7/2002
		 * 11:23:02 AM)
		 * 
		 * @return com.kni.etl.ETLJobExecutorStatus
		 */
        public boolean fileDownloaded() {
            return this.bFileDownloaded;
        }

        /**
		 * Insert the method's description here. Creation date: (5/7/2002
		 * 11:52:15 AM)
		 * 
		 * @return true, if initialize
		 */
        protected boolean initialize() {
            return true;
        }

        /**
		 * Loops on the job queue, taking each job and running with it. Creation
		 * date: (5/3/2002 5:43:04 PM)
		 */
        @Override
        public void run() {
            boolean bSuccess;
            if (this.initialize() == false) {
                this.bFileDownloaded = false;
                return;
            }
            bSuccess = this.downloadFile();
            if (bSuccess) {
                this.bFileDownloaded = true;
            }
            this.terminate();
        }

        /**
		 * Download file.
		 * 
		 * @return true, if successful
		 */
        private boolean downloadFile() {
            FTPClient ftp = new FTPClient();
            try {
                int reply;
                ftp.connect(this.server);
                ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Connected to " + this.server);
                reply = ftp.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftp.disconnect();
                    ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "FTP server refused connection.");
                    return false;
                }
            } catch (IOException e) {
                if (ftp.isConnected()) {
                    try {
                        ftp.disconnect();
                    } catch (IOException f) {
                        return false;
                    }
                }
                ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "FTP Could not connect to server.");
                ResourcePool.LogException(e, this);
                return false;
            }
            try {
                if (!ftp.login(this.user, this.password)) {
                    ftp.logout();
                    ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "FTP login failed.");
                    return false;
                }
                ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Remote system is " + ftp.getSystemName());
                if ((this.transferType != null) && (this.transferType.compareTo(FTPWorkerThread.ASCII) == 0)) {
                    ftp.setFileType(FTP.ASCII_FILE_TYPE);
                } else {
                    ftp.setFileType(FTP.BINARY_FILE_TYPE);
                }
                if ((this.passiveMode != null) && this.passiveMode.equalsIgnoreCase(FTPWorkerThread.FALSE)) {
                    ftp.enterLocalActiveMode();
                } else {
                    ftp.enterLocalPassiveMode();
                }
            } catch (FTPConnectionClosedException e) {
                ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Server closed connection.");
                ResourcePool.LogException(e, this);
                return false;
            } catch (IOException e) {
                ResourcePool.LogException(e, this);
                return false;
            }
            OutputStream output;
            try {
                java.util.Date startDate = new java.util.Date();
                output = new FileOutputStream(this.destFileName);
                ftp.retrieveFile(this.fileName, output);
                File f = new File(this.destFileName);
                if (f.exists() && (this.lastModifiedDate != null)) {
                    f.setLastModified(this.lastModifiedDate.longValue());
                }
                java.util.Date endDate = new java.util.Date();
                this.downloadTime = endDate.getTime() - startDate.getTime();
                double iDownLoadTime = ((this.downloadTime + 1) / 1000) + 1;
                ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Download Complete, Rate = " + (this.fileSize / (iDownLoadTime * 1024)) + " Kb/s, Seconds = " + iDownLoadTime);
                this.downloadTime = (this.downloadTime + 1) / 1000;
                if (ftp.isConnected()) {
                    ftp.disconnect();
                }
            } catch (FTPConnectionClosedException e) {
                ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, e.getMessage());
                ResourcePool.LogException(e, this);
                return false;
            } catch (IOException e) {
                ResourcePool.LogException(e, this);
                return false;
            }
            return true;
        }

        /**
		 * This is the publicly accessible function to set the "shutdown" flag
		 * for the thread. It will no longer process any new jobs, but finish
		 * what it's working on. It will then call the internal terminate()
		 * function to close down anything it needs to. BRIAN: should we make
		 * this final? Creation date: (5/3/2002 6:50:09 PM)
		 */
        public void shutdown() {
            this.bShutdown = true;
        }

        /**
		 * Terminate.
		 * 
		 * @return true, if successful
		 */
        protected boolean terminate() {
            return true;
        }

        @Override
        public String toString() {
            return "FTPWorkerThread: Server->" + this.server + ", File->" + this.fileName;
        }
    }

    /** The Constant ASCII. */
    public static final String ASCII = "ASCII";

    /** The Constant BINARY. */
    public static final String BINARY = "BINARY";

    /** The Constant DESTINATIONFILE. */
    public static final String DESTINATIONFILE = "DESTINATIONFILE";

    /** The Constant DESTINATIONPATH. */
    public static final String DESTINATIONPATH = "DESTINATIONPATH";

    /** The Constant DOWNLOAD_TIME. */
    public static final String DOWNLOAD_TIME = "DOWNLOADTIME";

    /** The Constant FALSE. */
    public static final String FALSE = "FALSE";

    /** The Constant FILENAME. */
    public static final String FILENAME = "FILENAME";

    /** The Constant FILESIZE. */
    public static final String FILESIZE = "FILESIZE";

    /** The Constant REQUIREDFILECOUNT. */
    public static final String REQUIREDFILECOUNT = "REQUIREDFILECOUNT";

    /** The Constant MAX_PARALLEL_CONNECTIONS. */
    public static final String MAX_PARALLEL_CONNECTIONS = "MAXPARALLELCONNECTIONS";

    /** The Constant PASSIVEMODE. */
    public static final String PASSIVEMODE = "PASSIVEMODE";

    /** The Constant PASSWORD. */
    public static final String PASSWORD = "PASSWORD";

    /** The Constant PATH. */
    public static final String PATH = "PATH";

    /** The Constant SERVER. */
    public static final String SERVER = "SERVER";

    /** The Constant SOURCEPATH. */
    public static final String SOURCEPATH = "SOURCEPATH";

    /** The Constant TRANSFER_TYPE. */
    public static final String TRANSFER_TYPE = "TRANSFER_TYPE";

    /** The Constant SYNC. */
    public static final String SYNC = "SYNC";

    /** The Constant USER. */
    public static final String USER = "USER";

    /** The FILESIZ e_ POS. */
    static int FILESIZE_POS = 1;

    /** The FILENAM e_ POS. */
    static int FILENAME_POS = 0;

    /** The IGNOREFIL e_ POS. */
    static int IGNOREFILE_POS = 2;

    /** The FILEDAT e_ POS. */
    static int FILEDATE_POS = 3;

    /** The PASSWOR d_ POS. */
    static int PASSWORD_POS = 8;

    /** The PASSIVEMOD e_ POS. */
    static int PASSIVEMODE_POS = 7;

    /** The SERVE r_ POS. */
    static int SERVER_POS = 9;

    /** The TRANSFE r_ TYP e_ POS. */
    static int TRANSFER_TYPE_POS = 4;

    /** The DOWNLOA d_ ELEMENTS. */
    static int DOWNLOAD_ELEMENTS = 10;

    /** The DEFAUL t_ MA x_ PARALLE l_ CONNECTIONS. */
    static int DEFAULT_MAX_PARALLEL_CONNECTIONS = 20;

    /** The DESTINATIO n_ POS. */
    static int DESTINATION_POS = 6;

    /** The USE r_ POS. */
    static int USER_POS = 5;

    /** The ms required tags. */
    String[] msRequiredTags = { FTPFileFetcher.USER, FTPFileFetcher.PASSWORD, FTPFileFetcher.SERVER, FTPFileFetcher.SOURCEPATH, FTPFileFetcher.DESTINATIONPATH };

    /** The connection cnt. */
    int connectionCnt = 1;

    /** The file cnt. */
    int fileCnt = 0;

    /** The files found. */
    boolean filesFound = false;

    /** The files to download. */
    ArrayList filesToDownload = null;

    /** The synchronize files. */
    boolean synchronizeFiles = true;

    ArrayList ftpThreadPool = null;

    /** The max parallel connections. */
    int iMaxParallelConnections;

    /**
	 * Gets the filenames for each complete parameter list.
	 * 
	 * @param iParamList
	 *            The param list
	 * 
	 * @return the filenames for each complete parameter list
	 */
    int getFilenamesForEachCompleteParameterList(int iParamList) {
        String strPath;
        String strUser;
        String strPassword;
        String strServer;
        String strTransferType;
        String strDestinationPath;
        String strPassiveMode;
        String strRequiredFileCount;
        strPath = this.getParameterValue(iParamList, FTPFileFetcher.SOURCEPATH);
        strUser = this.getParameterValue(iParamList, FTPFileFetcher.USER);
        strPassword = this.getParameterValue(iParamList, FTPFileFetcher.PASSWORD);
        strServer = this.getParameterValue(iParamList, FTPFileFetcher.SERVER);
        strTransferType = this.getParameterValue(iParamList, FTPFileFetcher.TRANSFER_TYPE);
        strDestinationPath = this.getParameterValue(iParamList, FTPFileFetcher.DESTINATIONPATH);
        strPassiveMode = this.getParameterValue(iParamList, FTPFileFetcher.PASSIVEMODE);
        strRequiredFileCount = this.getParameterValue(iParamList, FTPFileFetcher.REQUIREDFILECOUNT);
        boolean binaryTransfer = true;
        if ((strTransferType != null) && (strTransferType.compareTo(FTPFileFetcher.ASCII) == 0)) {
            binaryTransfer = false;
        }
        boolean passiveMode = true;
        if ((strPassiveMode != null) && (strPassiveMode.equalsIgnoreCase(FTPFileFetcher.FALSE))) {
            passiveMode = false;
        }
        int iRequiredFileCount = -1;
        if (strRequiredFileCount != null) {
            try {
                iRequiredFileCount = Integer.parseInt(strRequiredFileCount);
            } catch (NumberFormatException e) {
                ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Required file count parameters is an invalid number \"" + strRequiredFileCount + "\"");
                return -1;
            }
        }
        if ((strUser != null) && (strPassword != null) && (strServer != null) && (strPath != null) && (strDestinationPath != null)) {
            Object[] files = null;
            files = this.getFilenamesFromFTPServer(strUser, strPassword, strServer, strDestinationPath, binaryTransfer, strPath, passiveMode);
            if ((files == null) || (files.length == 0)) {
                ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "No files found on server " + strServer + " for search string " + strPath);
                return 0;
            } else if ((files != null) && (iRequiredFileCount != -1) && (iRequiredFileCount != files.length)) {
                ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Number of files does not match required file count of " + iRequiredFileCount + ", actual files found " + files.length);
                return -1;
            }
            if (this.filesToDownload == null) {
                this.filesToDownload = new ArrayList();
            }
            for (Object element : files) {
                Object[] tmp = new Object[FTPFileFetcher.DOWNLOAD_ELEMENTS];
                if (((Object[]) element)[FTPFileFetcher.IGNOREFILE_POS] == null) {
                    tmp[FTPFileFetcher.FILENAME_POS] = ((Object[]) element)[FTPFileFetcher.FILENAME_POS];
                    tmp[FTPFileFetcher.FILESIZE_POS] = ((Object[]) element)[FTPFileFetcher.FILESIZE_POS];
                    tmp[FTPFileFetcher.FILEDATE_POS] = ((Object[]) element)[FTPFileFetcher.FILEDATE_POS];
                    tmp[FTPFileFetcher.USER_POS] = strUser;
                    tmp[FTPFileFetcher.SERVER_POS] = strServer;
                    tmp[FTPFileFetcher.PASSWORD_POS] = strPassword;
                    tmp[FTPFileFetcher.TRANSFER_TYPE_POS] = strTransferType;
                    tmp[FTPFileFetcher.DESTINATION_POS] = strDestinationPath;
                    tmp[FTPFileFetcher.PASSIVEMODE_POS] = strPassiveMode;
                    this.filesToDownload.add(tmp);
                } else {
                    ResourcePool.LogMessage("Ignoring file " + ((Object[]) element)[FTPFileFetcher.FILENAME_POS] + " as it matches destination");
                }
            }
            return files.length;
        }
        return 0;
    }

    /**
	 * Gets the filenames from FTP server.
	 * 
	 * @param strUser
	 *            the str user
	 * @param strPassword
	 *            the str password
	 * @param strServer
	 *            the str server
	 * @param strDestinationPath
	 *            the str destination path
	 * @param bBinaryTransfer
	 *            the b binary transfer
	 * @param searchString
	 *            the search string
	 * @param bPassiveMode
	 *            the b passive mode
	 * 
	 * @return the filenames from FTP server
	 */
    public Object[] getFilenamesFromFTPServer(String strUser, String strPassword, String strServer, String strDestinationPath, boolean bBinaryTransfer, String searchString, boolean bPassiveMode) {
        Object[] result = null;
        FTPClient ftp = this.getFTPConnection(strUser, strPassword, strServer, bBinaryTransfer, "Directory listing connection.", bPassiveMode);
        if (ftp == null) {
            ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "FTP: Could not connect to server.");
            return null;
        }
        FTPFile[] fList;
        try {
            fList = ftp.listFiles(new DefaultFTPFileListParser(), searchString);
            char pathSeperator = '\\';
            String pathName = null;
            int endOfPath = searchString.lastIndexOf(pathSeperator);
            if (endOfPath == -1) {
                pathSeperator = '/';
                endOfPath = searchString.lastIndexOf(pathSeperator);
            }
            if (endOfPath != -1) {
                pathName = searchString.substring(0, endOfPath);
            }
            if (fList != null) {
                ArrayList tmpFiles = new ArrayList();
                for (FTPFile element : fList) {
                    Object[] tmp = new Object[4];
                    String fileName;
                    if (pathName != null) {
                        pathSeperator = '\\';
                        endOfPath = element.getName().lastIndexOf(pathSeperator);
                        String fn = element.getName();
                        if (endOfPath == -1) {
                            pathSeperator = '/';
                            endOfPath = fn.lastIndexOf(pathSeperator);
                        }
                        if (endOfPath != -1) {
                            tmp[FTPFileFetcher.FILENAME_POS] = pathName + pathSeperator + element.getName().substring(endOfPath + 1);
                            fileName = element.getName().substring(endOfPath + 1);
                        } else {
                            tmp[FTPFileFetcher.FILENAME_POS] = pathName + pathSeperator + element.getName();
                            fileName = element.getName();
                        }
                    } else {
                        tmp[FTPFileFetcher.FILENAME_POS] = element.getName();
                        fileName = element.getName();
                    }
                    tmp[FTPFileFetcher.FILESIZE_POS] = new Long(element.getSize());
                    tmp[FTPFileFetcher.FILEDATE_POS] = new Long(element.getTimestamp().getTimeInMillis());
                    try {
                        long mCreationDate = element.getTimestamp().getTimeInMillis();
                        if (strDestinationPath != null) {
                            fileName = strDestinationPath + fileName;
                        }
                        File f = new File(fileName);
                        if (f.exists() && (f.lastModified() == mCreationDate) && (f.length() == element.getSize())) {
                            tmp[FTPFileFetcher.IGNOREFILE_POS] = "";
                        }
                    } catch (Exception e) {
                        if (this.synchronizeFiles) {
                            ResourcePool.LogMessage("Warning: FTP server does not support file synchronization.");
                        }
                    }
                    tmpFiles.add(tmp);
                }
                if (tmpFiles.size() > 0) {
                    result = new Object[tmpFiles.size()];
                    tmpFiles.toArray(result);
                }
            }
        } catch (IOException e1) {
            ResourcePool.LogException(e1, this);
            ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, searchString + " caused IO Exception, file will be ignored");
        }
        if (ftp.isConnected()) {
            try {
                ftp.disconnect();
            } catch (IOException f) {
            }
        }
        return result;
    }

    /**
	 * Gets the FTP connection.
	 * 
	 * @param strUser
	 *            the str user
	 * @param strPassword
	 *            the str password
	 * @param strServer
	 *            the str server
	 * @param binaryTransfer
	 *            the binary transfer
	 * @param connectionNote
	 *            the connection note
	 * @param passiveMode
	 *            the passive mode
	 * 
	 * @return the FTP connection
	 */
    private FTPClient getFTPConnection(String strUser, String strPassword, String strServer, boolean binaryTransfer, String connectionNote, boolean passiveMode) {
        FTPClient ftp = new FTPClient();
        try {
            int reply;
            ftp.connect(strServer);
            ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Connected to " + strServer + ", " + connectionNote);
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "FTP server refused connection.");
                return null;
            }
        } catch (IOException e) {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException f) {
                    return null;
                }
            }
            ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "FTP Could not connect to server.");
            ResourcePool.LogException(e, this);
            return null;
        }
        try {
            if (!ftp.login(strUser, strPassword)) {
                ftp.logout();
                ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "FTP login failed.");
                return null;
            }
            ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Remote system is " + ftp.getSystemName() + ", " + connectionNote);
            if (binaryTransfer) {
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
            } else {
                ftp.setFileType(FTP.ASCII_FILE_TYPE);
            }
            if (passiveMode) {
                ftp.enterLocalPassiveMode();
            } else {
                ftp.enterLocalActiveMode();
            }
        } catch (FTPConnectionClosedException e) {
            ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Server closed connection.");
            ResourcePool.LogException(e, this);
            return null;
        } catch (IOException e) {
            ResourcePool.LogException(e, this);
            return null;
        }
        return ftp;
    }

    @Override
    public int initialize(Node xmlSourceNode) throws KETLThreadException {
        int res = super.initialize(xmlSourceNode);
        if (res != 0) {
            return res;
        }
        NamedNodeMap nmAttrs = xmlSourceNode.getAttributes();
        this.iMaxParallelConnections = XMLHelper.getAttributeAsInt(nmAttrs, FTPFileFetcher.MAX_PARALLEL_CONNECTIONS, 0);
        if (this.iMaxParallelConnections == 0) {
            ResourcePool.LogMessage(this, ResourcePool.WARNING_MESSAGE, "Defaulting to max parallel ftp connections of " + this.iMaxParallelConnections);
            this.iMaxParallelConnections = FTPFileFetcher.DEFAULT_MAX_PARALLEL_CONNECTIONS;
        }
        this.synchronizeFiles = XMLHelper.getAttributeAsBoolean(nmAttrs, FTPFileFetcher.SYNC, true);
        int filesToGet = 0;
        if (this.maParameters != null) {
            for (int i = 0; i < this.maParameters.size(); i++) {
                filesToGet = filesToGet + this.getFilenamesForEachCompleteParameterList(i);
            }
        }
        if (filesToGet == 0) {
            ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "No files found on any server, check parameter lists");
            return 3;
        } else if (filesToGet == -1) {
            ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Failing job see previous error");
            return 3;
        }
        ArrayList tmpArrayList = new ArrayList();
        for (int i = 0; i < this.filesToDownload.size(); i++) {
            Object[] fileDetails = (Object[]) this.filesToDownload.get(i);
            boolean alreadyListed = false;
            for (int p = 0; p < tmpArrayList.size(); p++) {
                Object[] cmpFileDetails = (Object[]) tmpArrayList.get(p);
                if ((((String) cmpFileDetails[FTPFileFetcher.SERVER_POS]).compareTo((String) fileDetails[FTPFileFetcher.SERVER_POS]) == 0) && (((String) cmpFileDetails[FTPFileFetcher.USER_POS]).compareTo((String) fileDetails[FTPFileFetcher.USER_POS]) == 0) && (((String) cmpFileDetails[FTPFileFetcher.PASSWORD_POS]).compareTo((String) fileDetails[FTPFileFetcher.PASSWORD_POS]) == 0) && (((String) cmpFileDetails[FTPFileFetcher.FILENAME_POS]).compareTo((String) fileDetails[FTPFileFetcher.FILENAME_POS]) == 0) && (((String) cmpFileDetails[FTPFileFetcher.DESTINATION_POS]).compareTo((String) fileDetails[FTPFileFetcher.DESTINATION_POS]) == 0) && (((String) cmpFileDetails[FTPFileFetcher.SERVER_POS]).compareTo((String) fileDetails[FTPFileFetcher.SERVER_POS]) == 0)) {
                    ResourcePool.LogMessage(this, ResourcePool.WARNING_MESSAGE, "Parameter list results in duplicate file search," + " duplicate file will be downloaded once only " + (String) fileDetails[FTPFileFetcher.FILENAME_POS]);
                    alreadyListed = true;
                }
            }
            if (alreadyListed == false) {
                tmpArrayList.add(fileDetails);
            }
        }
        this.filesToDownload = tmpArrayList;
        if (this.filesToDownload != null) {
            ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Total files to be downloaded = " + this.filesToDownload.size());
        }
        return 0;
    }

    public int getNextRecord(Object[] pResultArray, Class[] pExpectedDataTypes, int pRecordWidth) throws KETLReadException {
        if (this.ftpThreadPool == null) {
            this.ftpThreadPool = new ArrayList();
        }
        FTPWorkerThread ftpWorkerThread = null;
        while (((this.filesToDownload != null) && (this.filesToDownload.size() != 0)) && (this.ftpThreadPool.size() <= this.iMaxParallelConnections)) {
            FTPWorkerThread newFTPWorkerThread = new FTPWorkerThread();
            Object[] filesToDownload = (Object[]) this.filesToDownload.remove(0);
            newFTPWorkerThread.getFile((String) filesToDownload[FTPFileFetcher.USER_POS], (String) filesToDownload[FTPFileFetcher.PASSWORD_POS], (String) filesToDownload[FTPFileFetcher.SERVER_POS], (String) filesToDownload[FTPFileFetcher.TRANSFER_TYPE_POS], (String) filesToDownload[FTPFileFetcher.FILENAME_POS], (String) filesToDownload[FTPFileFetcher.DESTINATION_POS], (String) filesToDownload[FTPFileFetcher.PASSIVEMODE_POS], (Long) filesToDownload[FTPFileFetcher.FILESIZE_POS], (Long) filesToDownload[FTPFileFetcher.FILEDATE_POS]);
            this.ftpThreadPool.add(newFTPWorkerThread);
            newFTPWorkerThread.start();
        }
        int finishedThread = -1;
        while ((this.ftpThreadPool.size() > 0) && (finishedThread == -1)) {
            for (int i = 0; i < this.ftpThreadPool.size(); i++) {
                ftpWorkerThread = (FTPWorkerThread) this.ftpThreadPool.get(i);
                if (ftpWorkerThread.bFileDownloaded) {
                    finishedThread = i;
                    i = this.ftpThreadPool.size();
                }
            }
            if (finishedThread == -1) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ie) {
                    throw new KETLReadException(ie);
                }
            }
        }
        if (finishedThread != -1) {
            ftpWorkerThread = (FTPWorkerThread) this.ftpThreadPool.remove(finishedThread);
            int pos = 0;
            for (ETLOutPort element : this.mOutPorts) {
                if (element.isUsed()) {
                    if (element.isConstant()) pResultArray[pos++] = element.getConstantValue(); else if (element.mstrName.compareTo(FTPFileFetcher.FILENAME) == 0) {
                        pResultArray[pos++] = ftpWorkerThread.fileName;
                    } else if (element.mstrName.compareTo(FTPFileFetcher.SERVER) == 0) {
                        pResultArray[pos++] = (ftpWorkerThread.server);
                    } else if (element.mstrName.compareTo(FTPFileFetcher.USER) == 0) {
                        pResultArray[pos++] = (ftpWorkerThread.user);
                    } else if (element.mstrName.compareTo(FTPFileFetcher.TRANSFER_TYPE) == 0) {
                        pResultArray[pos++] = (ftpWorkerThread.transferType);
                    } else if (element.mstrName.compareTo(FTPFileFetcher.FILESIZE) == 0) {
                        pResultArray[pos++] = (ftpWorkerThread.fileSize);
                    } else if (element.mstrName.compareTo(FTPFileFetcher.DOWNLOAD_TIME) == 0) {
                        pResultArray[pos++] = (ftpWorkerThread.downloadTime);
                    } else if (element.mstrName.compareTo(FTPFileFetcher.DESTINATIONFILE) == 0) {
                        pResultArray[pos++] = (ftpWorkerThread.destFileName);
                    }
                }
            }
        } else {
            return DefaultReaderCore.COMPLETE;
        }
        return 1;
    }

    /**
	 * The Class FTPFetchOutPort.
	 */
    class FTPFetchOutPort extends ETLOutPort {

        /**
		 * Instantiates a new FTP fetch out port.
		 * 
		 * @param esOwningStep
		 *            the es owning step
		 * @param esSrcStep
		 *            the es src step
		 */
        public FTPFetchOutPort(ETLStep esOwningStep, ETLStep esSrcStep) {
            super(esOwningStep, esSrcStep);
        }

        @Override
        protected void setPortClass() throws ClassNotFoundException {
            String type = XMLHelper.getAttributeAsString(this.getXMLConfig().getAttributes(), "NAME", null);
            String dtype = XMLHelper.getAttributeAsString(this.getXMLConfig().getAttributes(), "DATATYPE", null);
            if (dtype != null && type != null) {
                if (type.equalsIgnoreCase(FTPFileFetcher.DOWNLOAD_TIME) || type.equalsIgnoreCase(FTPFileFetcher.FILESIZE)) {
                    type = "LONG";
                } else {
                    type = "STRING";
                }
                this.getXMLConfig().setAttribute("DATATYPE", type);
            }
            super.getPortClass();
        }
    }

    @Override
    protected ETLOutPort getNewOutPort(ETLStep srcStep) {
        return new FTPFetchOutPort(this, srcStep);
    }

    @Override
    protected void close(boolean success, boolean jobFailed) {
    }
}
