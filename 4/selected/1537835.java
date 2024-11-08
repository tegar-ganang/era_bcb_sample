package net;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import org.apache.log4j.Category;
import util.*;

/**
 * This class handles connections to clients.
 *
 * @author mkurz
 * @created 12. Mai 2002
 * @version 1.0
 */
public class TClientConnection implements Runnable, ITask {

    private static final char COMMAND_END_CHAR = '|';

    private static final char COMMAND_SEP_CHAR = ' ';

    private static final int LONG_TIME_OUT = 2 * 60 * 1000;

    private static final int TIME_OUT = 20000;

    private static final int VERIFY_RESUME_SIZE = 1024;

    private static final DCEncryptionHandler encryptionHandler = new DCEncryptionHandler();

    private static final Category logger = Category.getInstance(TClientConnection.class);

    /**
     * The Download String
     */
    public static final String DOWNLOAD_DIRECTION = "Download";

    /**
     * Description of the Field
     */
    public static final int UPLOAD_BLOCK_SIZE = 64000;

    /**
     * Description of the Field
     */
    public static final String UPLOAD_DIRECTION = "Upload";

    private static ServerSocket serverSocket;

    private boolean bDownloading = false;

    private boolean bUploading = false;

    private int bytesPerSecond;

    private long bytesReceived = 0;

    private Client client;

    private boolean directionRequested = false;

    private DownloadRequest downloadRequest;

    private String eta;

    private long fileLength = -1;

    private String info = "";

    private boolean isServer;

    private long lastAction = 0;

    private long lastBytesReceived;

    private long lastStateChange;

    private ArrayList listeners = new ArrayList();

    private RandomAccessFile localFile;

    private TokenInputStream reader;

    private String remoteKey;

    private long resumeFrom = 0;

    private Socket socket;

    private long startTime;

    private State state = State.CONNECTING;

    private int verifyResumeSize;

    /**
     * Description of the Field
     */
    public String currentDirection = null;

    /**
     * Constructor for the ClientConnection object
     *
     * @param client The Client object
     * @param listener A <code>IClientConnectionListener</code> that listens to
     *      this connection
     * @param isServer Are we an Server ?
     * @exception IOException Exceptions are put thru this class.
     */
    public TClientConnection(Client client, IClientConnectionListener listener, boolean isServer) throws IOException {
        logger.debug("ClientConnection created: " + client.getHost());
        this.client = client;
        addListener(listener);
        this.isServer = isServer;
        new Thread(this).start();
    }

    /**
     * Gets the myNick attribute of the ClientConnection object
     *
     * @return The myNick value
     */
    private String getMyNick() {
        return Settings.getInstance().getMyself().getNick();
    }

    /**
     * Gets the bytesPerSecond attribute of the ClientConnection object
     *
     * @return The bytesPerSecond value
     */
    public int getBytesPerSecond() {
        return bytesPerSecond;
    }

    /**
     * Gets the bytesReceived attribute of the ClientConnection object
     *
     * @return The bytesReceived value
     */
    public long getBytesReceived() {
        return bytesReceived;
    }

    /**
     * Gets the client attribute of the ClientConnection object
     *
     * @return The client value
     */
    public Client getClient() {
        return client;
    }

    /**
     * Gets the download attribute of the ClientConnection object
     *
     * @return The download value
     */
    public DownloadRequest getDownload() {
        return downloadRequest;
    }

    /**
     * Gets the fileLength attribute of the ClientConnection object
     *
     * @return The fileLength value
     */
    public long getFileLength() {
        return fileLength;
    }

    /**
     * Gets the info attribute of the ClientConnection object
     *
     * @return The info value
     */
    public String getInfo() {
        return info;
    }

    /**
     * Gets the state attribute of the ClientConnection object
     *
     * @return The state value
     */
    public State getState() {
        return state;
    }

    /**
     * Sets the state attribute of the ClientConnection object
     *
     * @param state The new state value
     */
    private void setState(State state) {
        this.state = state;
        fireStateChanged();
        if (state == State.UPLOADING) {
            logger.debug("Starting upload thread!");
            TTaskManager.getInstance().removeTask(this);
            TTaskManager.getInstance().removeTask(this);
            new Thread(this).start();
        }
    }

    /**
     * Sets the last action time for Timeout processing.
     */
    protected void setActionPerformed() {
        lastAction = System.currentTimeMillis();
    }

    /**
     * Sets the download attribute of the ClientConnection object
     *
     * @param dr The new download value
     */
    public void setDownload(DownloadRequest dr) {
        downloadRequest = dr;
    }

    /**
     * Closes the file
     */
    private void closeFile() {
        if (localFile != null) {
            try {
                localFile.close();
            } catch (Exception e) {
            }
            localFile = null;
            downloadRequest = null;
        }
    }

    /**
     * processCommand takes a String with one or multiple commands, parses them
     * and takes aproperate actions.
     *
     * @param cmdString Description of the Parameter
     * @exception IOException Description of the Exception
     */
    private void processCommand(String cmdString) throws IOException {
        int separatorIndex = cmdString.indexOf(String.valueOf(COMMAND_SEP_CHAR));
        logger.debug("Got: " + cmdString);
        String cmdStart = null;
        String cmdData = null;
        if (cmdString.length() > 1 && separatorIndex > 0 && separatorIndex < (cmdString.length() - 1)) {
            cmdStart = cmdString.substring(0, separatorIndex);
            cmdData = cmdString.substring(separatorIndex + 1);
        } else {
            cmdStart = cmdString.substring(0, cmdString.length());
        }
        if ("$Lock".equals(cmdStart)) {
            logger.debug(cmdString);
            String cmdKey = cmdData.substring(0, cmdData.toUpperCase().indexOf(" PK="));
            String privateKey = cmdData.substring(cmdData.toUpperCase().indexOf(" PK=") + 4);
            logger.debug("Recieved Challenge/Response request recieved, we're Logging in");
            remoteKey = encryptionHandler.calculateValidationKey(cmdKey);
            if (Settings.getInstance().isActive() && this.currentDirection != this.UPLOAD_DIRECTION) {
                sendCommand("$MyNick", getMyNick());
                sendCommand("$Lock", "EXTENDEDPROTOCOLABCABCABCABCABCABC Pk=JAVADC2BBBB");
            }
            if (client != null && client.hasDownloads()) {
                requestDirection(DOWNLOAD_DIRECTION, false);
            }
        } else if ("$Direction".equals(cmdStart)) {
            logger.debug("Got $Direction:" + cmdData);
            String direction = cmdData.substring(0, cmdData.indexOf(" "));
            logger.debug("#" + direction + "#");
            if (DOWNLOAD_DIRECTION.equals(direction)) {
                this.currentDirection = UPLOAD_DIRECTION;
                logger.debug("Setting currentDirection:" + this.currentDirection);
            } else {
                this.currentDirection = DOWNLOAD_DIRECTION;
                logger.debug("Setting currentDirection:" + this.currentDirection);
            }
            if (!directionRequested) {
                requestDirection(this.currentDirection, true);
            }
        } else if ("$GetListLen".equals(cmdStart)) {
            File file = ShareManager.getInstance().getFile("MyList.DcLst");
            sendCommand("$ListLen", Long.toString(file.length()));
        } else if ("$Get".equals(cmdStart)) {
            String filename = null;
            long fileOffset = -1;
            filename = cmdData.substring(0, cmdData.indexOf("$"));
            fileOffset = SafeParser.parseLong(cmdData.substring(cmdData.indexOf("$") + 1), -1);
            if (Settings.getInstance().reserveUploadSlot() && fileOffset > -1) {
                File file = ShareManager.getInstance().getFile(filename);
                bUploading = true;
                if (file == null) {
                    sendMaxedOut();
                } else {
                    if (fileOffset == 1) {
                        fileOffset--;
                    }
                    localFile = new RandomAccessFile(file, "r");
                    logger.debug("Searching position:" + (fileOffset));
                    localFile.seek(fileOffset);
                    bytesReceived = fileOffset;
                    fileLength = file.length();
                    sendCommand("$FileLength", Long.toString(fileLength));
                }
            } else {
                sendMaxedOut();
            }
        } else if ("$MaxedOut".equals(cmdStart)) {
            logger.debug("Recieved MaxedOUT!");
            throw new IOException();
        } else if ("$Key".equals(cmdStart)) {
            if (!DOWNLOAD_DIRECTION.equals(currentDirection)) {
                logger.debug("Requesting upload direction!");
                requestDirection(UPLOAD_DIRECTION, true);
                setState(State.COMMAND_UPLOAD);
            } else {
                if (Settings.getInstance().isActive()) {
                    client.checkDownloads();
                }
                setState(State.COMMAND_DOWNLOAD);
            }
        } else if ("$Send".equals(cmdStart)) {
            logger.debug("Got send!");
            setState(State.UPLOADING);
        } else if ("$MyNick".equals(cmdStart)) {
            client.setNick(cmdData);
        } else if ("$Supports".equals(cmdStart)) {
            getExtensions(cmdString);
        } else if ("$Error".equals(cmdStart)) {
            fireDownloadFailed();
        } else if ("$FileLength".equals(cmdStart)) {
            if (downloadRequest.isSegment()) {
                fileLength = downloadRequest.getSegment().y - downloadRequest.getSegment().x;
            } else {
                fileLength = SafeParser.parseLong(cmdData, 0);
            }
            sendCommand("$Send", "");
            setState(verifyResumeSize > 0 ? State.RESUMING : State.DOWNLOADING);
        } else {
            logger.error("Command unrecognized:" + cmdStart + " " + cmdData);
            disconnect();
        }
    }

    /**
     * Request a direction, returns true if we have the direction, otherwise
     * false.
     *
     * @param direction The String with the Direction in it.
     * @param force Should we force it ?
     * @return Returns <code>true</code> if we have the connection otherwise
     *      <code>false</code> is returned.
     * @exception IOException Currently the is never an Exception thrown
     */
    private boolean requestDirection(String direction, boolean force) throws IOException {
        logger.debug("requestDirection currentDirection:" + currentDirection);
        logger.debug("requestDirection force:" + force);
        if (directionRequested && currentDirection.equals(direction) && !force) {
            return true;
        }
        if (!directionRequested && direction != null && remoteKey != null) {
            logger.debug("requestDirection - requesting direction");
            int rnd = (int) (Math.random() * 10000) + 1;
            sendCommand("$Direction", direction + " " + rnd);
            sendCommand("$Key", remoteKey);
            directionRequested = true;
            return true;
        }
        return false;
    }

    /**
     * Sends a command without data.
     *
     * @param command The <code>String</code> containing command to send.
     * @exception IOException Currently the is never an Exception thrown
     */
    private void sendCommand(String command) throws IOException {
        sendCommand(command, "");
    }

    /**
     * Sends a command with data.
     *
     * @param command The <code>String</code> containing command to send.
     * @param data The <code>String</code> containing data to send.
     * @exception IOException Exception is comming from the <code>OutputStream</code>
     *      of the <code>Socket</code>.
     */
    private void sendCommand(String command, String data) throws IOException {
        if (socket != null) {
            OutputStream os = socket.getOutputStream();
            logger.debug("Sending command: " + command + COMMAND_SEP_CHAR + data);
            try {
                os.write((command + COMMAND_SEP_CHAR + data + COMMAND_END_CHAR).getBytes("ISO-8859-1"));
                os.flush();
                os.flush();
            } catch (IOException e) {
                disconnect();
                throw e;
            }
        } else {
            logger.debug("Socket was NULL " + this);
        }
    }

    /**
     * Send that we don�t have any slots open for downloading.
     */
    private void sendMaxedOut() {
        try {
            sendCommand("$MaxedOut");
        } catch (IOException e) {
        }
        disconnect();
    }

    /**
     * Starts a Download.
     *
     * @return Returns <code>true</code> if started otherwiese <code>false</code>
     *      .
     * @exception IOException Description of the Exception
     */
    private boolean startDownload() throws IOException {
        boolean bReturn = true;
        if (Settings.getInstance().reserveDownloadSlot() == true) {
            bDownloading = true;
            File file = new File(downloadRequest.getLocalFilename());
            localFile = new RandomAccessFile(file, "rw");
            if (downloadRequest.isSegment()) {
                long segmentsize = downloadRequest.getSegment().y - downloadRequest.getSegment().x;
                System.out.println(file.getName() + " " + segmentsize + " " + file.length() + " " + file.canWrite());
                if (file.exists()) {
                    if (file.length() == segmentsize) {
                        System.out.println("Found Complete Segment");
                        fireDownloadComplete();
                    }
                } else {
                    resumeFrom = downloadRequest.getSegment().x;
                    bytesReceived = 0;
                    verifyResumeSize = 0;
                }
            } else {
                if (downloadRequest.isResume() && file.exists() && file.isFile() && file.canRead()) {
                    verifyResumeSize = (int) Math.min(file.length(), VERIFY_RESUME_SIZE);
                    bytesReceived = file.length();
                    resumeFrom = file.length() - verifyResumeSize;
                    localFile.seek(resumeFrom);
                } else {
                    resumeFrom = 0;
                    verifyResumeSize = 0;
                    bytesReceived = 0;
                }
            }
            sendCommand("$Get", downloadRequest.getSearchResult().getFilename() + "$" + (resumeFrom + 1));
            setState(State.WAITING);
            startTime = System.currentTimeMillis();
            bReturn = true;
        } else {
            bReturn = false;
        }
        return bReturn;
    }

    /**
     * Checks for Timeout and disconnects if one occures.
     */
    protected void checkTimedOut() {
        if (lastAction != 0 && ((lastAction + LONG_TIME_OUT) < System.currentTimeMillis())) {
            logger.debug("Connection timed out!");
            disconnect();
        }
    }

    /**
     * The download has completed, so call all our Listeners.
     */
    protected void fireDownloadComplete() {
        IClientConnectionListener l[] = (IClientConnectionListener[]) listeners.toArray(new IClientConnectionListener[listeners.size()]);
        for (int i = 0; i < l.length; i++) {
            l[i].downloadComplete(this);
        }
        if (!client.hasDownloads() && !downloadRequest.getSearchResult().getFilename().equals("MyList.DcLst")) {
            setState(State.WAITING);
        } else {
            System.out.println("leaving connection open");
        }
    }

    /**
     * The download has failed, so call all our Listeners.
     */
    protected void fireDownloadFailed() {
        IClientConnectionListener l[] = (IClientConnectionListener[]) listeners.toArray(new IClientConnectionListener[listeners.size()]);
        for (int i = 0; i < l.length; i++) {
            l[i].downloadFailed(this);
        }
    }

    /**
     * The state of the connection has changes, so call all our Listeners.
     */
    protected void fireStateChanged() {
        IClientConnectionListener l[] = (IClientConnectionListener[]) listeners.toArray(new IClientConnectionListener[listeners.size()]);
        for (int i = 0; i < l.length; i++) {
            l[i].stateChanged(this);
        }
    }

    /**
     * Adds a Listener for us.
     *
     * @param listener The listener to add.
     */
    public void addListener(IClientConnectionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Disconnect the client
     */
    public void disconnect() {
        if (bDownloading) {
            Settings.getInstance().releaseDownloadSlot();
            bDownloading = false;
            bUploading = false;
        }
        if (bUploading) {
            Settings.getInstance().releaseUploadSlot();
            bDownloading = false;
            bUploading = false;
        }
        TTaskManager.getInstance().removeTask(this);
        try {
            socket.close();
        } catch (Exception e) {
            logger.error(e);
        }
        try {
            serverSocket.close();
        } catch (Exception e) {
            logger.error(e);
        }
        socket = null;
        serverSocket = null;
        if (bytesReceived != fileLength) {
            fireDownloadFailed();
        }
        IClientConnectionListener l[] = (IClientConnectionListener[]) listeners.toArray(new IClientConnectionListener[listeners.size()]);
        for (int i = 0; i < l.length; i++) {
            l[i].disconnected(this);
        }
        closeFile();
        setState(State.NOT_CONNECTED);
    }

    /**
     * Removes a Listener from us.
     *
     * @param listener The listener to remove.
     */
    public void removeListener(IClientConnectionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Main processing method for the ClientConnection object
     */
    public void run() {
        if (this.state == State.UPLOADING) {
            while (this.state == State.UPLOADING) {
                runTask();
            }
            TTaskManager.getInstance().addTask(this);
            logger.debug("Stopping upload thread!");
            return;
        }
        final State s;
        try {
            ClientConnectionManager.getInstance().addConnection(TClientConnection.this);
            logger.debug("Connecting to: " + client.getHost());
            if (isServer) {
                if (serverSocket == null) {
                    serverSocket = new ServerSocket(client.getHost().getPort());
                }
                serverSocket.setSoTimeout(TIME_OUT);
                socket = serverSocket.accept();
                HostInfo realHost = new HostInfo(socket.getInetAddress().getHostAddress(), client.getHost().getPort());
                client.setHost(realHost);
                ClientManager.getInstance().addClient(client);
            } else {
                socket = new Socket(client.getHost().getHost(), client.getHost().getPort());
            }
            socket.setReceiveBufferSize(65535);
            reader = new TokenInputStream(new BufferedInputStream(socket.getInputStream()), COMMAND_END_CHAR);
            TTaskManager.getInstance().addTask(this);
            TTaskManager.getInstance().addEvent(new ITask() {

                public void runTask() {
                    try {
                        if (!isServer) {
                            sendCommand("$MyNick", getMyNick());
                            sendCommand("$Lock", "EXTENDEDPROTOCOLABCABCABCABCABCABC Pk=JAVADC2BBBB");
                            sendCommand("$Supports", Features.getSupportCommands());
                        }
                        setState(State.LOGIN);
                    } catch (IOException e) {
                        logger.error("error running " + e);
                        disconnect();
                    }
                }
            });
        } catch (IOException e) {
            final String problem = e.toString();
            TTaskManager.getInstance().addEvent(new ITask() {

                public void runTask() {
                    gui.StatusBar.getInstance().setStatus(2, "ClientConnection Error " + "(" + problem + ")", java.awt.Color.red);
                    disconnect();
                }
            });
        }
    }

    /**
     * Description of the Method
     */
    public synchronized void runTask() {
        try {
            if (state == State.LOGIN || state == State.WAITING || state == State.COMMAND_UPLOAD) {
                if (state == State.WAITING && ((System.currentTimeMillis() - startTime) > TIME_OUT)) {
                    disconnect();
                }
                String cmd = reader.readToken();
                if (cmd != null) {
                    logger.debug("Got command - processing: " + cmd);
                    processCommand(cmd);
                    setActionPerformed();
                }
            } else if (state == State.COMMAND_DOWNLOAD) {
                if (downloadRequest != null) {
                    startDownload();
                    setActionPerformed();
                }
            } else if (state == State.RESUMING) {
                if (reader.available() >= verifyResumeSize) {
                    byte[] buffer = new byte[verifyResumeSize];
                    int x1 = reader.read(buffer);
                    byte[] fileBlock = new byte[verifyResumeSize];
                    int x2 = localFile.read(fileBlock);
                    if (!Arrays.equals(buffer, fileBlock)) {
                        logger.error("Tried resume, but files weren't equal");
                        gui.StatusBar.getInstance().setStatus(2, "Could not resume, files don't match", java.awt.Color.red);
                        fireDownloadFailed();
                        disconnect();
                    } else {
                        logger.debug("File resumed!");
                        if (bytesReceived == fileLength) {
                            fireDownloadComplete();
                            closeFile();
                            setState(State.COMMAND_DOWNLOAD);
                        } else {
                            setState(State.DOWNLOADING);
                        }
                    }
                    setActionPerformed();
                }
            } else if (state == State.DOWNLOADING) {
                int size = (int) Math.min(reader.available(), fileLength - bytesReceived);
                if (size > 0) {
                    byte[] buffer = new byte[size];
                    reader.read(buffer);
                    localFile.write(buffer);
                    bytesReceived += size;
                    long time = System.currentTimeMillis();
                    if (time - lastStateChange > 5000) {
                        bytesPerSecond = (int) ((bytesReceived - lastBytesReceived) * 1000 / (time - lastStateChange));
                        lastBytesReceived = bytesReceived;
                        lastStateChange = time;
                        double duration = time - startTime;
                        double averageSpeed = ((double) (bytesReceived - resumeFrom)) / duration + 0.000000000001;
                        double timeLeft = ((double) (fileLength - bytesReceived)) / averageSpeed;
                        eta = ByteConverter.getTimeString((long) timeLeft);
                    }
                    info = (bytesReceived * 100 / fileLength) + "%  " + ByteConverter.byteToShortString(bytesPerSecond) + "/s  " + ByteConverter.byteToShortString(bytesReceived) + " / " + ByteConverter.byteToShortString(fileLength) + "  (" + eta + ")";
                    if (bytesReceived == fileLength) {
                        fireDownloadComplete();
                        closeFile();
                        setState(State.COMMAND_DOWNLOAD);
                    }
                    setActionPerformed();
                    fireStateChanged();
                }
            } else if (state == State.UPLOADING) {
                if (!UserManager.getInstance().isUserBlocked(client.getNick())) {
                    String cmd = reader.readToken();
                    if (cmd != null) {
                        logger.debug("Processing command: " + cmd);
                        processCommand(cmd);
                        setActionPerformed();
                    }
                    int size = (int) Math.min(UPLOAD_BLOCK_SIZE, fileLength - bytesReceived);
                    logger.debug("File length was:" + fileLength);
                    logger.debug("Bytes recieveD:" + bytesReceived);
                    logger.debug("Size is:" + size);
                    if (size > 0) {
                        logger.debug("Loading buffer with:" + size + "bytes");
                        byte[] buffer = new byte[size];
                        int readBytes = localFile.read(buffer);
                        if (readBytes > -1) {
                            socket.getOutputStream().write(buffer, 0, readBytes);
                            socket.getOutputStream().flush();
                            bytesReceived += readBytes;
                        } else {
                            logger.debug("Read beyound file size!!");
                            socket.getOutputStream().flush();
                            setActionPerformed();
                            logger.debug("Transfer done, cleaning up!2");
                            closeFile();
                            Settings.getInstance().releaseUploadSlot();
                            setState(State.COMMAND_UPLOAD);
                            disconnect();
                        }
                        logger.debug("Actually read:" + readBytes);
                        long time = System.currentTimeMillis();
                        if (time - lastStateChange > 5000) {
                            bytesPerSecond = (int) ((bytesReceived - lastBytesReceived) * 1000 / (time - lastStateChange));
                            lastBytesReceived = bytesReceived;
                            lastStateChange = time;
                        }
                        info = (bytesReceived * 100 / fileLength) + "%  " + ByteConverter.byteToShortString(bytesPerSecond) + "/s  " + ByteConverter.byteToShortString(bytesReceived) + " / " + ByteConverter.byteToShortString(fileLength);
                        fireStateChanged();
                        setActionPerformed();
                    }
                    if (bytesReceived >= fileLength) {
                        socket.getOutputStream().flush();
                        setActionPerformed();
                        logger.debug("Transfer done, cleaning up!");
                        closeFile();
                        Settings.getInstance().releaseUploadSlot();
                        setState(State.COMMAND_UPLOAD);
                    } else {
                        sendMaxedOut();
                        setState(State.WAITING);
                    }
                }
                sendMaxedOut();
                setState(State.WAITING);
            }
        } catch (Exception e) {
            System.out.println("error running2 " + e);
            disconnect();
        }
        checkTimedOut();
    }

    /**
     * Checks the given Extensionstring and activates the support if possible
     * @author zomk3
     * @created 2002-05-20
     * @version 1.0
     * @param strExt The Extensions to check
     */
    private void getExtensions(String strExt) {
        if (strExt.indexOf("BZList") != -1) client.setBZSupport(true);
    }

    /**
     * The state for an ClientConnection.
     *
     * @author mkurz
     * @created 12. Mai 2002
     */
    public static class State extends Enum {

        /**
         * Description of the Field
         */
        public static final State COMMAND_DOWNLOAD = new State("Idle download mode");

        /**
         * Description of the Field
         */
        public static final State COMMAND_UPLOAD = new State("Idle upload mode");

        /**
         * Description of the Field
         */
        public static final State CONNECTING = new State("Connecting");

        /**
         * Description of the Field
         */
        public static final State DOWNLOADING = new State("Downloading");

        /**
         * Description of the Field
         */
        public static final State LOGIN = new State("Login");

        /**
         * Description of the Field
         */
        public static final State NOT_CONNECTED = new State("Not connected");

        /**
         * Description of the Field
         */
        public static final State RESUMING = new State("Resuming");

        /**
         * Description of the Field
         */
        public static final State SETTING_DIRECTION = new State("Setting direction");

        /**
         * Description of the Field
         */
        public static final State UPLOADING = new State("Uploading");

        /**
         * Description of the Field
         */
        public static final State WAITING = new State("Waiting");

        private String info = "";

        /**
         * Constructor for the State object
         *
         * @param name Description of the Parameter
         */
        private State(String name) {
            super(name);
        }
    }
}
