package com.xmultra.processor.net;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.net.DefaultSocketFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.xmultra.XmultraConfig;
import com.xmultra.log.Console;
import com.xmultra.log.ErrorLogEntry;
import com.xmultra.log.MessageLogEntry;
import com.xmultra.manager.watch.jcw.JcronMessage;
import com.xmultra.processor.DestinationWriter;
import com.xmultra.processor.Processor;
import com.xmultra.util.DateUtils;
import com.xmultra.util.FileUtils;
import com.xmultra.util.InitMapHolder;
import com.xmultra.util.InvalidConfigFileFormatException;
import com.xmultra.util.NameSelector;
import com.xmultra.util.Strings;
import com.xmultra.util.SyncFlag;

/**
 * FtpGetProcessor gets documents via ftp protocol
 * using NetComponet to do the ftp. Will retry a failed transmission.
 *
 * @author      Albert Tzeng
 * @version     $Revision: #2 $
 * @since       1.2
 */
public class FtpGetProcessor extends Processor {

    /**
    * Updated automatically by source control management.
    */
    public static final String VERSION = "@version $Revision: #2 $";

    private SyncFlag retrySyncFlag;

    private boolean notifyAndStartWaitingFlag;

    private long timeBetweenRetries;

    private FTPClient ftpClient = null;

    private String user = null;

    private String password = null;

    ;

    private String ftpAddress = null;

    private boolean activeMode = false;

    private boolean passiveMode = false;

    private int ftpPort = 21;

    private int transferType = FTP.BINARY_FILE_TYPE;

    private int noRetries = 5;

    private int noRetriesSoFar = 0;

    private String ftpSocketConnectTimeout = null;

    private String ftpSocketConnectTimeoutUnits = null;

    private String ftpSocketReadTimeout = null;

    private String ftpSocketReadTimeoutUnits = null;

    private int socketConnectTimeoutMs;

    private int socketReadTimeoutMs;

    protected Node thisProcessorNode;

    private InitMapHolder initMapHolder;

    private Strings strings;

    private String getDstDir = null;

    private File currentFile = null;

    private File srcDoneLocFile = null;

    private String minFileAge = null;

    private String minFileAgeUnits = null;

    private String maxFileAge = null;

    private String maxFileAgeUnits = null;

    private long maximumFileAgeMilliseconds = Long.MAX_VALUE;

    private String deleteAfterRetrieval = null;

    private String[][] messageInserts;

    private ArrayList<RemoteFile> srcFiles;

    private Node messageNode;

    private Node getLocationsNode;

    private Node getSrcLocationNode;

    private NodeList sourceLocations;

    private NameSelector nameSelector;

    private DestinationWriter destWriter = null;

    public FtpGetProcessor() {
        ftpClient = new FTPClient();
        if (Console.getConsoleMode("9")) {
            ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
        }
    }

    /**
     * Initializes the Processor object after it has been created.
     *
     * @param n         This Processor's corresponding element in Xmultra's
     *                  main configuration file. A child of the Processor
     *                  element. Has setup info.
     *
     * @param imh       Holds references to utility and log objects.
     *
     * @param sf        Used to communicate between threads.
     *
     * @param stopFlag  Goes false when the thread running in this
     *                  "run()" method should end.
     *
     * @return          True if init was successful.
     */
    public boolean init(Node n, InitMapHolder imh, SyncFlag sf, SyncFlag stopFlag) {
        if (!super.init(n, imh, sf, stopFlag)) {
            return false;
        }
        retrySyncFlag = new SyncFlag(false);
        msgEntry = new MessageLogEntry(this, VERSION);
        errEntry = new ErrorLogEntry(this, VERSION);
        this.strings = new Strings();
        user = xmlParseUtils.getAttributeValueFromNode(aProcessorNode, XmultraConfig.USER_ATTRIBUTE);
        password = xmlParseUtils.getAttributeValueFromNode(aProcessorNode, XmultraConfig.PASSWORD_ATTRIBUTE);
        password = xmlParseUtils.conditionAttributeValue(password, false);
        String ftpPortStr = xmlParseUtils.getAttributeValueFromNode(aProcessorNode, XmultraConfig.PORT_ATTRIBUTE);
        ftpAddress = xmlParseUtils.getAttributeValueFromNode(aProcessorNode, XmultraConfig.ADDRESS_ATTRIBUTE);
        String type = xmlParseUtils.getAttributeValueFromNode(aProcessorNode, XmultraConfig.TRANSFER_TYPE_ATTRIBUTE);
        if (type.equals(XmultraConfig.TRANSFER_TYPE_ASCII)) {
            transferType = FTP.ASCII_FILE_TYPE;
        }
        String noRetriesStr = xmlParseUtils.getAttributeValueFromNode(aProcessorNode, XmultraConfig.NUMBER_RETRIES);
        String secsBetweenRetriesStr = xmlParseUtils.getAttributeValueFromNode(aProcessorNode, XmultraConfig.SECS_BETWEEN_RETRIES);
        String mode = xmlParseUtils.getAttributeValueFromNode(aProcessorNode, XmultraConfig.FTP_MODE_ATTRIBUTE);
        ftpSocketConnectTimeout = xmlParseUtils.getAttributeValueFromNode(aProcessorNode, XmultraConfig.FTP_SOCKET_CONNECT_TIMEOUT);
        ftpSocketConnectTimeoutUnits = xmlParseUtils.getAttributeValueFromNode(aProcessorNode, XmultraConfig.FTP_SOCKET_CONNECT_TIMEOUT_UNITS);
        ftpSocketReadTimeout = xmlParseUtils.getAttributeValueFromNode(aProcessorNode, XmultraConfig.FTP_SOCKET_READ_TIMEOUT);
        ftpSocketReadTimeoutUnits = xmlParseUtils.getAttributeValueFromNode(aProcessorNode, XmultraConfig.FTP_SOCKET_READ_TIMEOUT_UNITS);
        socketConnectTimeoutMs = getTimeoutInfo(ftpSocketConnectTimeout, ftpSocketConnectTimeoutUnits);
        if (socketConnectTimeoutMs < 0) return false;
        socketReadTimeoutMs = getTimeoutInfo(ftpSocketReadTimeout, ftpSocketReadTimeoutUnits);
        if (socketReadTimeoutMs < 0) return false;
        if (mode.equals(XmultraConfig.FTP_MODE_PASSIVE)) passiveMode = true; else activeMode = true;
        Node parentNode = n.getParentNode();
        Node grandParentNode = parentNode.getParentNode();
        if (!grandParentNode.getNodeName().equals(XmultraConfig.CRON_MANAGER_ELEMENT)) {
            srcDoneLocFile = getLocationDirectory(XmultraConfig.SRC_DONE_LOCATION_ELEMENT);
            if (srcDoneLocFile == null) {
                return false;
            }
        }
        try {
            if (ftpPortStr != null && !ftpPortStr.equals("")) {
                ftpPort = Integer.parseInt(ftpPortStr);
            }
            noRetries = Integer.parseInt(noRetriesStr);
            timeBetweenRetries = Long.parseLong(secsBetweenRetriesStr) * XmultraConfig.MS_PER_SECOND;
        } catch (NumberFormatException e) {
            errEntry.setThrowable(e);
            errEntry.setAppContext("init()");
            errEntry.setAppMessage("Bad number format in FtpPort/NumberRetries/" + "SecsBetweenRetries in an FtpProcessor sending to: " + ftpAddress);
            logger.logError(errEntry);
            return false;
        }
        this.initMapHolder = imh;
        this.ftpClient.setSocketFactory(new DefaultSocketFactory() {

            public Socket createSocket(String host, int port) throws IOException {
                Socket socket = new Socket();
                InetSocketAddress addr = new InetSocketAddress(host, port);
                socket.connect(addr, getSocketConnectTimeoutMs());
                socket.setSoTimeout(getSocketReadTimeoutMs());
                return socket;
            }

            public Socket createSocket(InetAddress inetAddress, int port) throws IOException {
                Socket socket = new Socket();
                InetSocketAddress addr = new InetSocketAddress(inetAddress, port);
                socket.connect(addr, getSocketConnectTimeoutMs());
                socket.setSoTimeout(getSocketReadTimeoutMs());
                return socket;
            }
        });
        this.ftpClient.setDataTimeout(this.getSocketReadTimeoutMs());
        this.ftpClient.setDefaultTimeout(this.getSocketReadTimeoutMs());
        return true;
    }

    /**
     * Makes this class runnable. This method is called when the Thread.start()
     * method is invoked.
     */
    public void run() {
        try {
            runIt();
        } catch (Throwable t) {
            if (super.shuttingDownProcessor == true) {
                super.shuttingDownProcessor = false;
                return;
            }
            try {
                errEntry.setThrowable(t);
                errEntry.setAppContext("run()");
                errEntry.setDocInfo(currentFile + "");
                errEntry.setAppMessage("Final try/catch caught 'uncaught' exception");
                errEntry.setSubjectSendEmail("Final try/catch block caught 'uncaught' exception");
                logger.logError(errEntry);
            } catch (Exception e) {
            }
        }
        super.shuttingDownProcessor = false;
    }

    /**
     * Makes this class runnable. This method is called
     * when the Thread.start() method is invoked.
     * Has the main loop of this thread.
     */
    public void runIt() {
        super.notifyAndStartWaiting();
        noRetriesSoFar = 0;
        notifyAndStartWaitingFlag = true;
        while (!processorStopSyncFlag.getFlag()) {
            if (!processList()) {
                continue;
            }
            if (processorStopSyncFlag.getFlag()) break;
            if (notifyAndStartWaitingFlag) {
                super.notifyAndStartWaiting();
                noRetriesSoFar = 0;
                notifyAndStartWaitingFlag = true;
            }
        }
        msgEntry.setAppContext("run()");
        msgEntry.setMessageText("Exiting FtpGetProcessor with '" + ftpAddress + "' address.");
        logger.logProcess(msgEntry);
        processorSyncFlag.setFlag(false);
        processorStopSyncFlag.setFlag(false);
    }

    /**
     * Process the list
     */
    private boolean processList() {
        List messageList = listHolder.getList();
        try {
            int messageListIndex = listHolder.getIndex();
            String messageClassName = (messageList.get(messageListIndex)).getClass().getName();
            for (; messageListIndex < messageList.size(); messageListIndex++) {
                if (!getFile(messageClassName, messageList, messageListIndex)) {
                    continue;
                }
                super.currentObjBeingProcessed = messageList.get(messageListIndex);
                for (int i = 0; i < this.sourceLocations.getLength(); i++) {
                    String remoteHomeDir = logonToServer(ftpClient, ftpAddress, noRetries);
                    if (remoteHomeDir == null) {
                        return false;
                    }
                    ftpClient.setFileType(transferType);
                    Element sourceLocation = (Element) this.sourceLocations.item(i);
                    String sourceDirectory = sourceLocation.getAttribute("Directory");
                    String sourceFile = sourceLocation.getAttribute("File");
                    try {
                        sourceDirectory = XmultraConfig.replaceSymbols(sourceDirectory);
                        sourceFile = XmultraConfig.replaceSymbols(sourceFile);
                    } catch (InvalidConfigFileFormatException icffe) {
                        errEntry.setThrowable(icffe);
                        errEntry.setAppContext("processList()");
                        errEntry.setAppMessage("Unable to parse date-time symbol in \"" + sourceDirectory + "\" or \"" + sourceFile + "\"");
                        logger.logError(errEntry);
                    }
                    boolean traverseSubdirectories = false;
                    if (sourceDirectory != null && sourceDirectory.endsWith("/*")) {
                        traverseSubdirectories = true;
                        sourceDirectory = sourceDirectory.substring(0, sourceDirectory.length() - 2);
                    }
                    if (!(sourceDirectory == null) && !(sourceDirectory.trim().equals("")) && !(sourceDirectory.equals("."))) {
                        if (!changeRemoteDir(ftpClient, sourceDirectory)) {
                            continue;
                        }
                    }
                    boolean fileListCreated = listRemoteFiles(ftpClient, sourceDirectory, sourceFile, traverseSubdirectories);
                    if (!fileListCreated) {
                        continue;
                    }
                    long sleepTime = calculateSleepTime(minFileAge, minFileAgeUnits);
                    HashMap<String, Long> prevFileSizeHashMap = getProfile(ftpClient);
                    logoutAndDisconnect(ftpClient);
                    if (prevFileSizeHashMap.size() == 0) {
                        continue;
                    }
                    long endTime = System.currentTimeMillis() + sleepTime;
                    long msRemaining = sleepTime;
                    while (msRemaining > 0L) {
                        try {
                            Thread.sleep(msRemaining);
                            msRemaining = endTime - System.currentTimeMillis();
                        } catch (InterruptedException e) {
                            msRemaining = endTime - System.currentTimeMillis();
                        }
                    }
                    remoteHomeDir = logonToServer(ftpClient, ftpAddress, noRetries);
                    if (remoteHomeDir == null) {
                        return false;
                    }
                    ftpClient.setFileType(transferType);
                    if (!(sourceDirectory == null) && !(sourceDirectory.trim().equals("")) && !(sourceDirectory.equals("."))) {
                        if (!changeRemoteDir(ftpClient, sourceDirectory)) {
                            continue;
                        }
                    }
                    HashMap currentFileSizeHashMap = getProfile(ftpClient);
                    ftpClient.changeWorkingDirectory(remoteHomeDir);
                    compareAndRetrieveFiles(ftpClient, prevFileSizeHashMap, currentFileSizeHashMap, messageClassName);
                }
                if (processorStopSyncFlag.getFlag()) {
                    logoutAndDisconnect(ftpClient);
                    return true;
                }
            }
            listHolder.setIndex(messageListIndex);
            logoutAndDisconnect(ftpClient);
            notifyAndStartWaitingFlag = true;
            return true;
        } catch (IOException e) {
            if (noRetriesSoFar++ < noRetries) {
                waitBetweenRetry();
                notifyAndStartWaitingFlag = false;
            } else {
                notifyAndStartWaitingFlag = true;
                errEntry.setThrowable(e);
                errEntry.setAppContext("processList()");
                errEntry.setAppMessage("Unable to get file after " + (noRetriesSoFar - 1) + " retries. Max Retries.");
                errEntry.setSubjectSendEmail("Ftp unable to get file");
                logger.logError(errEntry);
            }
            return false;
        }
    }

    private String logonToServer(FTPClient ftpClient, String ftpAddress, int noRetries) {
        String remoteHomeDir = null;
        noRetriesSoFar = 0;
        while (true) {
            try {
                ftpClient.connect(ftpAddress, ftpPort);
                int reply = ftpClient.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftpClient.disconnect();
                    throw new IOException();
                }
                if (!ftpClient.login(user, password)) {
                    throw new IOException();
                }
                remoteHomeDir = ftpClient.printWorkingDirectory();
                msgEntry.setAppContext("logonToServer()");
                msgEntry.setMessageText("Logged into FTP server " + ftpAddress + ":" + ftpPort + " as user " + user);
                logger.logProcess(msgEntry);
                break;
            } catch (IOException e) {
                logoutAndDisconnect(ftpClient);
                if (noRetriesSoFar++ < noRetries) {
                    waitBetweenRetry();
                    notifyAndStartWaitingFlag = false;
                } else {
                    notifyAndStartWaitingFlag = true;
                    errEntry.setThrowable(e);
                    errEntry.setAppContext("logonToServer()");
                    errEntry.setAppMessage("Unable to login after " + (noRetriesSoFar - 1) + " retries. Max Retries.\n" + "Address:" + ftpAddress + "\n" + "User:" + user);
                    errEntry.setSubjectSendEmail("Unable to login to " + ftpAddress + " after " + (noRetriesSoFar - 1) + " retries.");
                    logger.logError(errEntry);
                    break;
                }
            }
        }
        return remoteHomeDir;
    }

    /**
     * Get the "message" specifications for retrieving one or more files
     * from each of one or more locations.
     *
     * @param messageClassName JcronMessage or java.io.File
     * @param fileList list of GetMessage files from JdirWatch
     * @param fileListIndex starting position in file list
     *
     * @return true if successful; false otherwise
     */
    private boolean getFile(String messageClassName, List fileList, int fileListIndex) {
        if (messageClassName.endsWith(XmultraConfig.JCRON_MESSAGE)) {
            JcronMessage jcronMessage = ((JcronMessage) fileList.get(fileListIndex));
            this.getDstDir = jcronMessage.getDstDir();
            this.minFileAge = jcronMessage.getMinFileAge();
            this.minFileAgeUnits = jcronMessage.getMinFileAgeUnits();
            this.maxFileAge = jcronMessage.getMaxFileAge();
            this.maxFileAgeUnits = jcronMessage.getMaxFileAgeUnits();
            this.deleteAfterRetrieval = jcronMessage.getDeleteAfterRetrieval();
            this.messageNode = jcronMessage.getMessageNode();
            this.getLocationsNode = xmlParseUtils.getChildNode(messageNode, XmultraConfig.GET_LOCATIONS_ELEMENT);
            this.getSrcLocationNode = xmlParseUtils.getChildNode(getLocationsNode, XmultraConfig.GET_SRC_LOCATION_ELEMENT);
            this.messageInserts = jcronMessage.getMessageInserts();
        } else {
            currentFile = (File) fileList.get(fileListIndex);
            if (currentFile == null || !currentFile.isFile()) {
                return false;
            }
            GetMessage getMessage = new GetMessage(this.initMapHolder, currentFile, true);
            this.getDstDir = getMessage.getDstDir();
            this.minFileAge = getMessage.getMinFileAge();
            this.minFileAgeUnits = getMessage.getMinFileAgeUnits();
            this.maxFileAge = getMessage.getMaxFileAge();
            this.maxFileAgeUnits = getMessage.getMaxFileAgeUnits();
            this.deleteAfterRetrieval = getMessage.getDeleteAfterRetrieval();
            this.getSrcLocationNode = getMessage.getSrcLocationNode();
            this.getLocationsNode = getMessage.getGetLocationsNode();
            this.messageInserts = getMessage.getMessageInserts();
        }
        this.sourceLocations = ((Element) this.getLocationsNode).getElementsByTagName(XmultraConfig.GET_SRC_LOCATION_ELEMENT);
        this.maximumFileAgeMilliseconds = calculateMaximumFileAge(this.maxFileAge, this.maxFileAgeUnits);
        this.nameSelector = new NameSelector(this.getSrcLocationNode, this.initMapHolder);
        return true;
    }

    /**
     * Find the files and build files' profiles for either Jcron or Jdir
     *
     * @param ftpClient The NetComponents client used for FTP access.
     * @param traverseSubdirectories flag indicating whether to look in subdirectories of current directory
     *
     * @return true if successful; false otherwise
     */
    private boolean listRemoteFiles(FTPClient ftpClient, String sourceDirectory, String sourceFile, boolean traverseSubdirectories) {
        if (this.activeMode) ftpClient.enterLocalActiveMode();
        if (this.passiveMode) ftpClient.enterLocalPassiveMode();
        if (this.getDstDir != null) {
            this.getDstDir = fileUtils.addRoot(this.getDstDir);
            File ftpDstPath = new File(this.getDstDir);
            if (!ftpDstPath.exists()) {
                msgEntry.setAppContext("listRemoteFiles()");
                msgEntry.setMessageText("The destination location is not found.");
                logger.logProcess(msgEntry);
                return false;
            }
        } else {
            if (!initializeDestWriter()) {
                return false;
            }
        }
        try {
            if (sourceFile == null || sourceFile.equals("")) {
                FTPFile[] files = ftpClient.listFiles(".");
                this.srcFiles = new ArrayList<RemoteFile>(files.length);
                for (int i = 0; i < files.length; i++) {
                    FTPFile file = files[i];
                    if (file.isDirectory() || !this.nameSelector.isIncluded(file.getName())) {
                        continue;
                    }
                    if (getFileAge(file) > this.maximumFileAgeMilliseconds) {
                        continue;
                    }
                    this.srcFiles.add(new RemoteFile(sourceDirectory + "/" + file.getName(), file));
                }
                if (traverseSubdirectories) {
                    for (FTPFile file : files) {
                        if (file.isDirectory()) {
                            FTPFile[] subdirectoryFiles = ftpClient.listFiles(file.getName());
                            for (FTPFile subdirFile : subdirectoryFiles) {
                                if (subdirFile.isDirectory() || !this.nameSelector.isIncluded(subdirFile.getName())) {
                                    continue;
                                }
                                if (getFileAge(subdirFile) > this.maximumFileAgeMilliseconds) {
                                    continue;
                                }
                                this.srcFiles.add(new RemoteFile(sourceDirectory + "/" + file.getName() + "/" + subdirFile.getName(), subdirFile));
                            }
                        }
                    }
                }
            } else {
                this.srcFiles = new ArrayList<RemoteFile>(1);
                FTPFile[] files = ftpClient.listFiles(sourceFile);
                this.srcFiles.add(new RemoteFile(sourceDirectory + "/" + sourceFile, files[0]));
            }
        } catch (FTPConnectionClosedException e) {
            this.errEntry.setThrowable(e);
            this.errEntry.setAppContext("listRemoteFiles()");
            this.errEntry.setAppMessage("Can't list files from remote directory");
            this.logger.logError(errEntry);
            return false;
        } catch (IOException e) {
            this.errEntry.setThrowable(e);
            this.errEntry.setAppContext("listRemoteFiles()");
            this.errEntry.setAppMessage("Can't list files from address: " + this.ftpAddress + "  directory: " + sourceDirectory);
            this.errEntry.setSubjectSendEmail("Can't list files from remote directory");
            this.logger.logError(errEntry);
            return false;
        }
        if (this.srcFiles == null) {
            return false;
        }
        return true;
    }

    private long calculateSleepTime(String minFileAge, String minFileAgeUnits) {
        long sleepTime = 60000;
        try {
            sleepTime = Long.parseLong(minFileAge) * XmultraConfig.getTimeMultiplier(minFileAgeUnits);
        } catch (Exception e) {
            this.errEntry.setThrowable(e);
            this.errEntry.setAppContext("calculateSleepTime(" + minFileAge + "," + minFileAgeUnits + ")");
            this.errEntry.setAppMessage("Unable to parse a MinFileAge of '" + minFileAge + "' or a minFileAgeUnits of '" + minFileAgeUnits + "'.");
            this.logger.logError(errEntry);
        }
        return sleepTime;
    }

    /**
     * Calculate maximum age of file to be retrieved.
     *
     * @param maxFileAge age from config file
     * @param maxFileAgeUnits time units from config file
     *
     * @return maximum age in milliseconds
     */
    private long calculateMaximumFileAge(String maxFileAge, String maxFileAgeUnits) {
        long maximumAge = Long.MAX_VALUE;
        try {
            if (maxFileAge != null && !maxFileAge.equals("")) {
                maximumAge = Long.parseLong(maxFileAge) * XmultraConfig.getTimeMultiplier(maxFileAgeUnits);
            }
        } catch (Exception e) {
            this.errEntry.setThrowable(e);
            this.errEntry.setAppContext("calculateMaximumFileAge(" + maxFileAge + "," + maxFileAgeUnits + ")");
            this.errEntry.setAppMessage("Unable to parse a MaxFileAge of '" + maxFileAge + "' or a maxFileAgeUnits of '" + maxFileAgeUnits + "'.");
            this.logger.logError(errEntry);
        }
        return maximumAge;
    }

    /**
     * Compute age of remote file.
     *
     * @param file FTP file object
     *
     * @return file age in milliseconds
     */
    private long getFileAge(FTPFile file) {
        long fileTime = file.getTimestamp().getTimeInMillis();
        long now = new Date().getTime();
        return now - fileTime;
    }

    private HashMap<String, Long> getProfile(FTPClient ftpClient) throws IOException {
        HashMap<String, Long> fileSizeHashMap = new HashMap<String, Long>();
        for (int i = 0; i < this.srcFiles.size(); i++) {
            FTPFile currentFtpFile = srcFiles.get(i).getFtpFile();
            if (!currentFtpFile.isFile()) {
                continue;
            }
            fileSizeHashMap.put(this.srcFiles.get(i).getPath(), new Long(currentFtpFile.getSize()));
        }
        return fileSizeHashMap;
    }

    private void compareAndRetrieveFiles(FTPClient ftpClient, HashMap prevFileSizeHashMap, HashMap currentFileSizeHashMap, String messageClassName) throws IOException {
        boolean retrievedFile = false;
        for (int i = 0; i < this.srcFiles.size(); i++) {
            RemoteFile srcFile = this.srcFiles.get(i);
            Object myPrevFileSizeObj = prevFileSizeHashMap.get(srcFile.getPath());
            if (myPrevFileSizeObj == null) {
                continue;
            }
            Object myCurrentFileSizeObj = currentFileSizeHashMap.get(srcFile.getPath());
            if (myCurrentFileSizeObj == null) {
                continue;
            }
            long myPrevFileSize = ((Long) myPrevFileSizeObj).longValue();
            long myCurrentFileSize = ((Long) myCurrentFileSizeObj).longValue();
            if (myPrevFileSize == myCurrentFileSize) {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                retrievedFile = false;
                try {
                    retrievedFile = ftpClient.retrieveFile(srcFile.getPath(), byteStream);
                } catch (SocketTimeoutException ste) {
                    msgEntry.setDocInfo(srcFile.getPath());
                    msgEntry.setError("Socket read timeout of " + this.getSocketReadTimeoutMs() + "ms exceeded while retrieving file from " + ftpAddress);
                    logger.logWarning(msgEntry);
                }
                if (!retrievedFile) {
                    byteStream.close();
                    msgEntry.setDocInfo(srcFile.getPath());
                    msgEntry.setError("Could not retrieve file from '" + ftpAddress + "'");
                    logger.logWarning(msgEntry);
                    continue;
                }
                byteStream.close();
                String ftpData = byteStream.toString(FileUtils.ISO_LATIN_ENCODING);
                ftpData = appendInsertions(ftpData, messageClassName, srcFile);
                boolean errorWritingFiles = false;
                String destFileName = srcFile.getPath();
                destFileName = destFileName.replace("/", "_");
                destFileName = destFileName.replace("\\", "_");
                long timestamp = srcFile.getFtpFile().getTimestamp().getTimeInMillis();
                if (this.getDstDir != null) {
                    File destFile = new File(this.getDstDir, destFileName);
                    if (!fileUtils.writeFile(destFile, ftpData, timestamp)) {
                        errorWritingFiles = true;
                    }
                } else if (initializeDestWriter()) {
                    File destFile = new File(destFileName);
                    boolean copyData = true;
                    boolean roundRobin = false;
                    String[] filesWritten = destWriter.writeToLocations(destFile, ftpData, timestamp, copyData, roundRobin);
                    if (filesWritten == null) {
                        errorWritingFiles = true;
                    }
                } else {
                    errorWritingFiles = true;
                }
                if (deleteAfterRetrieval.equals("Yes")) {
                    if (!errorWritingFiles) {
                        ftpClient.deleteFile(srcFile.getPath());
                    } else {
                        msgEntry.setDocInfo(srcFile.getPath());
                        msgEntry.setError("Error writing file. Document not deleted from '" + ftpAddress + "'");
                        logger.logWarning(msgEntry);
                    }
                }
                if (!errorWritingFiles) {
                    msgEntry.setDocInfo(srcFile.getPath());
                    msgEntry.setMessageText("Document successfully retrieved from '" + ftpAddress + "'");
                    logger.logProcess(msgEntry);
                }
                processorSyncFlag.restartWaitUntilFalse();
            }
        }
    }

    /**
     * Append insertions and write file
     *
     * @param fileString contents of file
     * @param messageClassName Jcron or Jdir
     * @param file RemoteFile object being processed
     */
    private String appendInsertions(String fileString, String messageClassName, RemoteFile file) {
        for (int j = this.messageInserts.length - 1; j >= 0; j--) {
            String insertionPattern = this.messageInserts[j][0];
            try {
                insertionPattern = XmultraConfig.replaceSymbols(insertionPattern);
            } catch (InvalidConfigFileFormatException icffe) {
                this.errEntry.setThrowable(icffe);
                this.errEntry.setAppContext("appendInsertions()");
                this.errEntry.setAppMessage("Unable to parse symbols in message inserts: \"" + insertionPattern + "\"");
                this.logger.logError(errEntry);
                return "";
            }
            String insertValue = this.messageInserts[j][1];
            insertValue = this.strings.substitute(XmultraConfig.FILE_TIMESTAMP_SYMBOL, Long.toString(file.getFtpFile().getTimestamp().getTimeInMillis()), insertValue);
            insertValue = this.strings.substitute(XmultraConfig.FILE_PATH_SYMBOL, file.getPath(), insertValue);
            insertValue = this.strings.substitute(XmultraConfig.FILE_NAME_SYMBOL, file.getFtpFile().getName(), insertValue);
            try {
                insertValue = XmultraConfig.replaceSymbols(insertValue);
            } catch (InvalidConfigFileFormatException icffe) {
                this.errEntry.setThrowable(icffe);
                this.errEntry.setAppContext("appendInsertions()");
                this.errEntry.setAppMessage("Unable to parse symbols in message inserts: \"" + insertValue + "\"");
                this.logger.logError(errEntry);
                return "";
            }
            insertionPattern = xmlParseUtils.conditionAttributeValue(insertionPattern, false);
            insertValue = xmlParseUtils.conditionAttributeValue(insertValue, false);
            if (strings.matches(insertionPattern, fileString)) {
                fileString = strings.getPreMatch() + strings.toString() + insertValue + strings.getPostMatch();
            }
        }
        if (!messageClassName.endsWith(XmultraConfig.JCRON_MESSAGE)) {
            if (!Console.getConsoleMode("9")) {
                fileUtils.moveFileToDoneLocation(currentFile, srcDoneLocFile.toString());
            }
        }
        return fileString;
    }

    /**
     * Changes into the passed in directory.
     *
     * @param ftpClient
     *            This FTPClient object.
     *
     * @param directory
     *            The directory to change into.
     *
     * @return True if it was possible to change to the requested directory.
     */
    private boolean changeRemoteDir(FTPClient ftpClient, String directory) {
        try {
            if (!ftpClient.changeWorkingDirectory(directory)) {
                throw new IOException();
            }
            return true;
        } catch (FTPConnectionClosedException e) {
            this.errEntry.setThrowable(e);
            this.errEntry.setAppContext("changeRemoteDir()");
            this.errEntry.setAppMessage("FTPConnection was Closed");
            this.logger.logError(errEntry);
            return false;
        } catch (IOException e) {
            this.errEntry.setThrowable(e);
            this.errEntry.setAppContext("changeRemoteDir()");
            this.errEntry.setAppMessage("Cannot change into remote directory. server: " + this.ftpAddress + "  directory: " + directory);
            this.errEntry.setSubjectSendEmail("Cannot change into remote directory");
            this.logger.logError(errEntry);
            return false;
        }
    }

    /**
     * Wait for a period of time.
     */
    private void waitBetweenRetry() {
        try {
            retrySyncFlag.waitUntilTrue(timeBetweenRetries);
        } catch (InterruptedException e) {
            errEntry.setThrowable(e);
            errEntry.setAppContext("processorCall(node)");
            logger.logError(errEntry);
        }
    }

    /**
     * Logs out and disconnects.
     *
     * @param ftpClient An FTPClient that has been logged into.
     */
    private void logoutAndDisconnect(FTPClient ftpClient) {
        if (ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException ioe) {
            }
        }
    }

    /**
     * Sets up the DestWriter, if it hasn't been done yet.
     *
     * @return True if correctly setup.
     */
    private boolean initializeDestWriter() {
        if (destWriter == null) {
            destWriter = new DestinationWriter();
            try {
                if (!destWriter.init(this.initMapHolder, super.aProcessorNode, new DateUtils())) {
                    throw new InvalidConfigFileFormatException();
                }
            } catch (InvalidConfigFileFormatException e) {
                this.errEntry.setThrowable(e);
                this.errEntry.setAppContext("initializeDestWriter()");
                this.errEntry.setAppMessage("Error reading a DestinationLocation directory");
                this.errEntry.setSubjectSendEmail("Error reading a DestinationLocation directory");
                this.logger.logError(errEntry);
                destWriter = null;
                return false;
            }
        }
        return true;
    }

    private int getSocketConnectTimeoutMs() {
        return this.socketConnectTimeoutMs;
    }

    private int getSocketReadTimeoutMs() {
        return this.socketReadTimeoutMs;
    }

    /**
     * Used to cleanup the processor before it is killed.
     */
    public void cleanUp() {
        listHolder.setIndex(listHolder.getIndex() + 1);
        if ((ftpClient != null) && ftpClient.isConnected() && (ftpClient.getReplyCode() != 150)) {
            logoutAndDisconnect(ftpClient);
        }
    }

    /**
     * Gets the time a processor can run before it is timed out.
     *
     * @param timeoutValue The time allowed for the processor to complete task
     *
     * @param timeoutUnits The units of timeoutValue
     *
     * @return The time the Processor is allowed to complete its task.
     */
    private int getTimeoutInfo(String timeoutValue, String timeoutUnits) {
        int timeout;
        long timeoutMultiplier;
        try {
            timeoutMultiplier = XmultraConfig.getTimeMultiplier(timeoutUnits);
        } catch (Exception e) {
            errEntry.setThrowable(e);
            errEntry.setAppContext("getTimeoutInfo(String, String)");
            errEntry.setAppMessage("Unable to parse Timeout " + "attributes in Manager");
            logger.logError(errEntry);
            return -1;
        }
        timeout = Integer.parseInt(timeoutValue);
        timeout *= timeoutMultiplier;
        return timeout;
    }

    protected class RemoteFile {

        private String path;

        private FTPFile file;

        RemoteFile(String path, FTPFile file) {
            this.path = path;
            this.file = file;
        }

        public String getPath() {
            return this.path;
        }

        public FTPFile getFtpFile() {
            return this.file;
        }
    }
}
