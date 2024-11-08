package phex.common;

import java.awt.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import phex.*;
import phex.msg.*;
import phex.utils.*;

public class Cfg {

    public static final int DEFAULT_SOCKS5_PORT = 1080;

    public static final int DEFAULT_HTTP_PORT = 80;

    public static final int DEFAULT_MAX_MESSAGE_LENGTH = 65536;

    public static final short DEFAULT_LOGGER_VERBOSE_LEVEL = 6;

    public static final boolean DEFAULT_ENABLE_HIT_SNOOPING = true;

    public static final boolean DEFAULT_IS_CHAT_ENABLED = true;

    public static final boolean DEFAULT_ALLOW_TO_BECOME_LEAF = true;

    public static final boolean DEFAULT_ALLOW_TO_BECOME_ULTRAPEER = false;

    public static final boolean DEFAULT_FORCE_UP_CONNECTIONS = false;

    public static final boolean DEFAULT_IS_NOVENDOR_NODE_DISCONNECTED = false;

    public static final int DEFAULT_FREELOADER_FILES = 0;

    public static final int DEFAULT_FREELOADER_SHARE_SIZE = 0;

    public static final int DEFAULT_HOST_ERROR_DISPLAY_TIME = 1000;

    public static final int DEFAULT_TTL = 7;

    public static final int DEFAULT_MAX_NETWORK_TTL = 7;

    public static final int DEFAULT_UP_2_UP_CONNECTIONS = 16;

    public static final int DEFAULT_UP_2_LEAF_CONNECTIONS = 15;

    public static final int DEFAULT_UP_2_PEER_CONNECTIONS = 4;

    public static final int DEFAULT_LEAF_2_UP_CONNECTIONS = 5;

    public static final int DEFAULT_LEAF_2_PEER_CONNECTIONS = 2;

    public static final int DEFAULT_PEER_CONNECTIONS = 4;

    public static final int DEFAULT_MAX_CONNECTTO_HISTORY_SIZE = 10;

    public static final int DEFAULT_MAX_SEARCHTERM_HISTORY_SIZE = 10;

    public static final boolean DEFAULT_ARE_PARTIAL_FILES_SHARED = true;

    /**
     * The default value of the X-Max-TTL header for dynamic queries. 
     */
    public static final int DEFAULT_DYNAMIC_QUERY_MAX_TTL = 3;

    /**
     * The default value to indicate if this node is forced to be ultrapeer.
     */
    public static final boolean DEFAULT_FORCE_TOBE_ULTRAPEER = false;

    /**
     * The default max downloads that are allowed per IP.
     */
    public static final int DEFAULT_MAX_DOWNLOADS_PER_IP = 1;

    /**
     * The default value to indicate whether upload queuing is activated or not.
     */
    public static final boolean DEFAULT_ALLOW_UPLOAD_QUEUING = true;

    /**
     * The default max upload queue slots available.
     */
    public static final int DEFAULT_MAX_UPLOAD_QUEUE_SIZE = 100;

    /**
     * The default min poll time for queued uploads.
     */
    public static final int DEFAULT_MIN_UPLOAD_QUEUE_POLL_TIME = 45;

    /**
     * The default max poll time for queued uploads.
     */
    public static final int DEFAULT_MAX_UPLOAD_QUEUE_POLL_TIME = 120;

    /**
     * The default setting if we accept deflate connections.
     */
    public static final boolean DEFAULT_IS_DEFLATE_CONNECTION_ACCEPTED = true;

    public static final int UNLIMITED_BANDWIDTH = Integer.MAX_VALUE;

    public static int MIN_SEARCH_TERM_LENGTH = 2;

    public static final String GENERAL_GNUTELLA_NETWORK = "<General Gnutella Network>";

    public GUID mProgramClientID = new GUID();

    public String mMyIP = "";

    public int mListeningPort = -1;

    public int mMaxUpload = 4;

    public int mMaxUploadPerIP = 1;

    public int mUploadMaxBandwidth = 102400;

    public int mNetMaxHostToCatch = 1000;

    public int mNetMaxSendQueue = 500;

    public int mSearchMaxConcurrent = 10;

    public int mNetMaxRate = 50000;

    public int mDownloadMaxBandwidth = 102400;

    public boolean mDownloadAutoRemoveCompleted = false;

    public String mDownloadDir = ".";

    public int mDownloadMaxRetry = 999;

    public int mDownloadRetryWait = 30 * 1000;

    public boolean mAutoConnect = true;

    /**
     * @deprecated New variable called peerConnections. Only used to update for
     *             pre 0.8 versions.
     */
    public int mNetMinConn = 4;

    public boolean mAutoCleanup = true;

    /**
     * The max of this value should be 255. The protocol is not able to handle
     * more.
     */
    public int mUploadMaxSearch = 64;

    public boolean mShareBrowseDir = true;

    public int mPushTransferTimeout = 30 * 1000;

    /**
     * @deprecated since build 36 replaced by new security concept
     */
    public ArrayList mNetIgnoredHosts;

    /**
     * @deprecated since build 36 replaced by new security concept
     */
    public Vector mNetInvalidHosts = new Vector();

    /**
     * @deprecated since build 36 replaced by new security concept
     */
    public Vector mFilteredSearchHosts = new Vector();

    public String mCurrentNetwork = GENERAL_GNUTELLA_NETWORK;

    public Vector mNetNetworkHistory = new Vector();

    public boolean mAutoJoin = true;

    public boolean mDisconnectApplyPolicy = true;

    public int mDisconnectDropRatio = 70;

    public boolean mProxyUse = false;

    public String mProxyHost = "";

    public int mProxyPort = DEFAULT_SOCKS5_PORT;

    public boolean useProxyAuthentication = false;

    public String mProxyUserName = "";

    public String mProxyPassword = "";

    public Font mFontMenu = new Font("Dialog", Font.PLAIN, 11);

    public Font mFontLabel = new Font("Dialog", Font.PLAIN, 11);

    public Font mFontTable = new Font("Dialog", Font.PLAIN, 11);

    public String mFindText = "";

    public boolean mFindMatchCase = false;

    public boolean mFindDown = true;

    public boolean mUIDisplayTooltip = true;

    public String mLFClassName;

    public String mUploadDir = "";

    public String mUploadFileExclusions = "";

    public String mUploadFileInclusions = "*";

    public boolean mUploadScanRecursively = true;

    public boolean mUploadAutoRemoveCompleted = false;

    public boolean mPhexPingResponse = true;

    public boolean monitorSearchHistory = false;

    /**
     * The file into which searches should be monitored.
     */
    public String searchMonitorFile = "";

    public int searchHistoryLength = 10;

    /**
     * Indicates if the node is connected to a Local Area Network (LAN).
     */
    public boolean connectedToLAN = true;

    /**
     * Indicates whether Phex minimizes to the background on close of the GUI. If set to
     * false it will shutdown. If set to true on windows system it will go into the
     * sys tray, on all other system it will just minimize.
     */
    public boolean minimizeToBackground = true;

    /**
     * The status if a close options dialog should be displayed or not.
     */
    public boolean showCloseOptionsDialog = true;

    /**
     * The directory where incomplete files are stored.
     */
    public String incompleteDir = ".";

    /**
     * When the HostCatcher finds hosts it will first use the port filter
     * to see if it is allowed to use the host
     */
    public ArrayList filteredCatcherPorts = new ArrayList();

    /**
     * The max number of parallel workers per download file.
     */
    public short maxWorkerPerDownload = 3;

    /**
     * The max number of total parallel workers for all download files.
     */
    public short maxTotalDownloadWorker = 6;

    /**
     * Indicates whether upload queuing is allowed or not.
     */
    public boolean allowUploadQueuing;

    /**
     * The maximal number of upload queue slots available.
     */
    public int maxUploadQueueSize;

    /**
     * The minimum poll time for queued uploads.
     */
    public int minUploadQueuePollTime;

    /**
     * The maximum poll time for queued uploads.
     */
    public int maxUploadQueuePollTime;

    /**
     * The maximum number of downloads that are allowed per IP.
     */
    public int maxDownloadsPerIP;

    /**
     * The max number of failed connections in a row a download candidate is
     * allowed to have before it gets dropped.
     */
    public short maxFailedConnectionsInARow = 10;

    /**
     * The total speed in kilo bits per second of the network connection the
     * user has available. This is not the bandwidth the user has available for
     * Phex.
     * The default of 256 matches a DSL/Cable connection.
     */
    public int networkSpeedKbps = 256;

    /**
     * This is the maximal bandwidth in bytes per second Phex is allowed to use
     * in total. This means network, download and upload bandwidth combined.
     * The default of 16384 matches 50% of the bandwidth a 256kbs DSL/Cable connection
     * is able to offer.
     */
    public int maxTotalBandwidth = 16384;

    /**
     * This is introduced to maintain the current version of Phex.
     * After a update of Phex the version in the cfg and the Phex version differs.
     * In this case we know that we need to upgrade the cfg or other stuff to the
     * new Phex version
     */
    public String runningPhexVersion = "";

    /**
     * This is introduced to maintain the current build number of Phex.
     * After a update of Phex the build number in the cfg and the Phex build number
     * differs. In this case we know that we need to upgrade the cfg and maybe
     * also do some other stuff to reach the new Phex version.
     */
    public String runningBuildNumber = "";

    /**
     * Defines if the host is behind a firewall. The trying of push transfers
     * and the QueryResponse QHD depends on it.
     */
    public boolean isBehindFirewall = false;

    /**
     * Defines if a http proxy is used for HTTP connections (not Gnutella
     * connections.
     */
    public boolean isHttpProxyUsed = false;

    /**
     * Defines the name of the http proxy host.
     */
    public String httpProxyHost = "";

    /**
     * Defines the port of the http proxy host.
     */
    public int httpProxyPort = DEFAULT_HTTP_PORT;

    /**
     * Contains the version number of the last update check.
     */
    public String lastUpdateCheckVersion = "0";

    /**
     * Contains the version number of the last beta update check.
     */
    public String lastBetaUpdateCheckVersion = "0";

    /**
     * Contains the time in millis of the last update check.
     */
    public long lastUpdateCheckTime = 0;

    /**
     * The status if a update notification dialog should be displayed or not.
     */
    public boolean showUpdateNotification = true;

    /**
     * The status if a beta update notification dialog should be displayed or not.
     */
    public boolean showBetaUpdateNotification = false;

    public int mSocketTimeout = 60 * 1000;

    public int privateSocketTimeout = 2000;

    /**
     * Timeout for network host connections.
     */
    public int mNetConnectionTimeout = 4 * 1000;

    /**
     * the time after which a automatic candidate search times out
     */
    public int searchRetryTimeout = 30000;

    /**
     * The verbose level of the logger
     * Default value is Logger.SEVERE.value (6)
     * Can't use direct value since Cfg has not finished initialization.
     */
    public short loggerVerboseLevel;

    /**
     * The compact logging type
     */
    public short logType = 0x01;

    /**
     * Defines if log output should also be written to the console.
     */
    public boolean logToConsole = false;

    /**
     * The max length of a log file.
     */
    public long maxLogFileLength = 512 * 1024;

    /**
     * Enables QueryHit Snooping.
     */
    public boolean enableHitSnooping;

    /**
     * The max length a message is allowed to have to be accepted.
     */
    public int maxMessageLength;

    /**
     * Indicates if the chat feature is enabled.
     */
    public boolean isChatEnabled;

    /**
     * Indicates if the node is allowed to connect to ultrapeers.
     * @deprecated replaced by allowToBecomeLeaf in 0.8.1
     */
    public boolean allowUPConnections;

    /**
     * Indicates that the node is allowed to connect as a leaf to ultrapeers.
     */
    public boolean allowToBecomeLeaf;

    /**
     * Indicates if the node is only accepting ultrapeer connections (as a leaf).
     * This value must always be checked together with allowToBecomeLeaf. If
     * allowToBecomeLeaf is false, a forceUPConnections value of true must be
     * ignored.
     */
    public boolean forceUPConnections;

    /**
     * Indicates if this node is allowed to become a Ultrapeer.
     */
    public boolean allowToBecomeUP;

    /**
     * Indicates if this node force to be a Ultrapeer.
     * This value must always be checked together with allowToBecomeUP. If
     * allowToBecomeUP is false, a forceToBeUltrapeer value of true must be
     * ignored.
     */
    public boolean forceToBeUltrapeer;

    /**
     * The number of ultrapeer to ultrapeer connections the nodes is allowed to
     * have open.
     * TODO2 this value is used for the X-Degree header but to reach high out degree
     * for dynamic query the value should be maintained above 15. There is no
     * way to ensure this yet.
     */
    public int up2upConnections;

    /**
     * The number of ultrapeer to leaf connections the nodes is allowed to
     * have open.
     */
    public int up2leafConnections;

    /**
     * The number of ultrapeer to normal peer connections the nodes is allowed to
     * have open.
     */
    public int up2peerConnections;

    /**
     * The number of leaf to ultrapeer connections the nodes is allowed to
     * have open. The max should be 3.
     */
    public int leaf2upConnections;

    /**
     * The number of leaf to normal peer connections this node is allowed to have
     * open.
     */
    public int leaf2peerConnections;

    /**
     * The number of normal peers the node is allowed to have as a normal peer.
     */
    public int peerConnections;

    /**
     * Indicates if nodes with no vendor code are disconnected.
     */
    public boolean isNoVendorNodeDisconnected;

    /**
     * The number of files a node need to share to not be called a freeloader.
     */
    public int freeloaderFiles;

    /**
     * The number of MB a node need to share to not be called a freeloader.
     */
    public int freeloaderShareSize;

    /**
     * The number of milliseconds a error is displayed in the connection table.
     */
    public int hostErrorDisplayTime;

    /**
     * The TTL Phex uses for messages.
     */
    public int ttl;

    /**
     * The maximim number of hops allowed to be seen in messages otherwise a
     * message is dropped. Also the highest ttl allowed to be seen in messages
     * otherwise the ttl is limited to Cfg.maxNetworkTTL - hops.
     */
    public int maxNetworkTTL;

    /**
     * History of connectTo items.
     */
    public ArrayList connectToHistory;

    /**
     * The max size of the connectToHistory list.
     */
    public int maxConnectToHistorySize;

    /**
     * History of search items.
     */
    public ArrayList searchTermHistory;

    /**
     * The max size of the searchTerm list.
     */
    public int maxSearchTermHistorySize;

    /**
     * Indicates whether partial downloaded files are offered to others for download.
     */
    public boolean arePartialFilesShared;

    /**
     * The total uptime of the last movingTotalUptimeCount starts.
     */
    public long movingTotalUptime;

    /**
     * The number of times the uptime was added to movingTotalUptime.
     */
    public int movingTotalUptimeCount;

    /**
     * The maximal uptime ever seen.
     */
    public long maximalUptime;

    /**
     * Indicates if we accept deflated connections.
     */
    public boolean isDeflateConnectionAccepted;

    private File configFile;

    private Properties mSetting;

    public Cfg(File cfgFile) {
        configFile = cfgFile;
        mSetting = new Properties();
    }

    public void load() {
        loadDefaultValues();
        try {
            FileInputStream is = new FileInputStream(configFile);
            mSetting.load(is);
            is.close();
        } catch (FileNotFoundException exp) {
        } catch (Exception exp) {
            Logger.logError(exp);
        }
        deserializeSimpleFields();
        deserializeComplexFields();
        handlePhexVersionAdjustments();
        if (mListeningPort == -1) {
            Random random = new Random(System.currentTimeMillis());
            mListeningPort = random.nextInt();
            mListeningPort = mListeningPort < 0 ? -mListeningPort : mListeningPort;
            mListeningPort %= 20000;
            mListeningPort += 4000;
        }
        updateHTTPProxySettings();
        if (mLFClassName == null) {
            if (Environment.getInstance().isMacOSX()) {
                mLFClassName = UIManager.getSystemLookAndFeelClassName();
            } else {
                mLFClassName = UIManager.getCrossPlatformLookAndFeelClassName();
            }
        }
        File dir = new File(mDownloadDir);
        dir.mkdirs();
        dir = new File(incompleteDir);
        dir.mkdirs();
    }

    public void save() {
        Logger.logMessage(Logger.FINEST, Logger.GLOBAL, "Saving configuration.");
        mSetting.clear();
        serializeSimpleFields();
        serializeComplexField();
        try {
            FileOutputStream os = new FileOutputStream(configFile);
            mSetting.store(os, "PHEX Config Values");
            os.close();
        } catch (IOException exp) {
            Logger.logError(exp);
        }
    }

    private void loadDefaultValues() {
        maxDownloadsPerIP = DEFAULT_MAX_DOWNLOADS_PER_IP;
        enableHitSnooping = DEFAULT_ENABLE_HIT_SNOOPING;
        maxMessageLength = DEFAULT_MAX_MESSAGE_LENGTH;
        isChatEnabled = DEFAULT_IS_CHAT_ENABLED;
        allowToBecomeLeaf = DEFAULT_ALLOW_TO_BECOME_LEAF;
        forceUPConnections = DEFAULT_FORCE_UP_CONNECTIONS;
        forceToBeUltrapeer = DEFAULT_FORCE_TOBE_ULTRAPEER;
        allowToBecomeUP = DEFAULT_ALLOW_TO_BECOME_ULTRAPEER;
        isNoVendorNodeDisconnected = DEFAULT_IS_NOVENDOR_NODE_DISCONNECTED;
        freeloaderFiles = DEFAULT_FREELOADER_FILES;
        freeloaderShareSize = DEFAULT_FREELOADER_SHARE_SIZE;
        hostErrorDisplayTime = DEFAULT_HOST_ERROR_DISPLAY_TIME;
        ttl = DEFAULT_TTL;
        maxNetworkTTL = DEFAULT_MAX_NETWORK_TTL;
        up2upConnections = DEFAULT_UP_2_UP_CONNECTIONS;
        up2leafConnections = DEFAULT_UP_2_LEAF_CONNECTIONS;
        up2peerConnections = DEFAULT_UP_2_PEER_CONNECTIONS;
        leaf2upConnections = DEFAULT_LEAF_2_UP_CONNECTIONS;
        leaf2peerConnections = DEFAULT_LEAF_2_PEER_CONNECTIONS;
        peerConnections = DEFAULT_PEER_CONNECTIONS;
        arePartialFilesShared = DEFAULT_ARE_PARTIAL_FILES_SHARED;
        allowUploadQueuing = DEFAULT_ALLOW_UPLOAD_QUEUING;
        maxUploadQueueSize = DEFAULT_MAX_UPLOAD_QUEUE_SIZE;
        minUploadQueuePollTime = DEFAULT_MIN_UPLOAD_QUEUE_POLL_TIME;
        maxUploadQueuePollTime = DEFAULT_MAX_UPLOAD_QUEUE_POLL_TIME;
        isDeflateConnectionAccepted = DEFAULT_IS_DEFLATE_CONNECTION_ACCEPTED;
        loggerVerboseLevel = DEFAULT_LOGGER_VERBOSE_LEVEL;
        connectToHistory = new ArrayList();
        maxConnectToHistorySize = DEFAULT_MAX_CONNECTTO_HISTORY_SIZE;
        searchTermHistory = new ArrayList();
        maxSearchTermHistorySize = DEFAULT_MAX_SEARCHTERM_HISTORY_SIZE;
    }

    private String get(String key) {
        String value = (String) mSetting.get(key);
        if (value != null) value = value.trim();
        return value;
    }

    private String get(String key, String defaultVal) {
        String value = get(key);
        if (value == null) return defaultVal;
        return value;
    }

    private void set(String key, String value) {
        if (value != null) {
            mSetting.put(key, value);
        }
    }

    private void set(String key, long value) {
        mSetting.put(key, String.valueOf(value));
    }

    private void set(String key, boolean value) {
        mSetting.put(key, value ? "true" : "false");
    }

    private void serializeSimpleFields() {
        Field[] fields = this.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            String name = fields[i].getName();
            int modifiers = fields[i].getModifiers();
            Class type = fields[i].getType();
            if (!Modifier.isPublic(modifiers) || Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) {
                continue;
            }
            try {
                if (type.getName().equals("int")) {
                    set(name, fields[i].getInt(this));
                } else if (type.getName().equals("short")) {
                    set(name, fields[i].getShort(this));
                } else if (type.getName().equals("long")) {
                    set(name, fields[i].getLong(this));
                } else if (type.getName().equals("boolean")) {
                    set(name, fields[i].getBoolean(this));
                } else if (type.getName().equals("java.lang.String")) {
                    set(name, (String) fields[i].get(this));
                }
            } catch (Exception exp) {
                Logger.logError(exp, "Error in field: " + name);
            }
        }
    }

    private void serializeComplexField() {
        try {
            set("mProgramClientID", mProgramClientID.toHexString());
            StringBuffer buffer = new StringBuffer(16 * mNetIgnoredHosts.size());
            for (int i = 0; i < mNetIgnoredHosts.size(); i++) {
                String[] parts = (String[]) mNetIgnoredHosts.get(i);
                buffer.append(parts[0]);
                buffer.append('.');
                buffer.append(parts[1]);
                buffer.append('.');
                buffer.append(parts[2]);
                buffer.append('.');
                buffer.append(parts[3]);
                buffer.append(' ');
            }
            set("mNetIgnoredHosts", buffer.toString());
            buffer.setLength(0);
            for (int i = 0; i < mFilteredSearchHosts.size(); i++) {
                String[] parts = (String[]) mFilteredSearchHosts.elementAt(i);
                buffer.append(parts[0]);
                buffer.append('.');
                buffer.append(parts[1]);
                buffer.append('.');
                buffer.append(parts[2]);
                buffer.append('.');
                buffer.append(parts[3]);
                buffer.append(' ');
            }
            set("mFilteredSearchHosts", buffer.toString());
            buffer.setLength(0);
            for (int i = 0; i < mNetInvalidHosts.size(); i++) {
                String[] parts = (String[]) mNetInvalidHosts.elementAt(i);
                buffer.append(parts[0]);
                buffer.append('.');
                buffer.append(parts[1]);
                buffer.append('.');
                buffer.append(parts[2]);
                buffer.append('.');
                buffer.append(parts[3]);
                buffer.append(' ');
            }
            set("mNetInvalidHosts", buffer.toString());
            int size = filteredCatcherPorts.size();
            buffer.setLength(0);
            for (int i = 0; i < size; i++) {
                buffer.append((String) filteredCatcherPorts.get(i));
                buffer.append(" ");
            }
            set("filteredCatcherPorts", buffer.toString());
            buffer.setLength(0);
            for (int i = 0; i < mNetNetworkHistory.size(); i++) {
                buffer.append(mNetNetworkHistory.elementAt(i));
                buffer.append(" ");
            }
            set("mNetNetworkHistory", buffer.toString());
            buffer.setLength(0);
            for (int i = 0; i < connectToHistory.size(); i++) {
                buffer.append(connectToHistory.get(i));
                buffer.append(" ");
            }
            set("connectToHistory", buffer.toString());
            buffer.setLength(0);
            for (int i = 0; i < searchTermHistory.size(); i++) {
                buffer.append(searchTermHistory.get(i));
                buffer.append(',');
            }
            set("searchTermHistory", buffer.toString());
            set("mFontMenu", mFontMenu.getName() + ";" + mFontMenu.getStyle() + ";" + mFontMenu.getSize());
            set("mFontLabel", mFontLabel.getName() + ";" + mFontLabel.getStyle() + ";" + mFontLabel.getSize());
            set("mFontTable", mFontTable.getName() + ";" + mFontTable.getStyle() + ";" + mFontTable.getSize());
        } catch (Exception e) {
            Logger.logError(e);
        }
    }

    private void deserializeSimpleFields() {
        Field[] fields = this.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            String name = fields[i].getName();
            int modifiers = fields[i].getModifiers();
            Class type = fields[i].getType();
            String value = "";
            if (!Modifier.isPublic(modifiers) || Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) {
                continue;
            }
            try {
                value = get(name);
                if (value == null) {
                    continue;
                }
                if (type.getName().equals("int")) {
                    fields[i].setInt(this, Integer.parseInt(value));
                } else if (type.getName().equals("short")) {
                    fields[i].setShort(this, Short.parseShort(value));
                } else if (type.getName().equals("long")) {
                    fields[i].setLong(this, Long.parseLong(value));
                } else if (type.getName().equals("boolean")) {
                    fields[i].setBoolean(this, value.equals("true"));
                } else if (type.getName().equals("java.lang.String")) {
                    fields[i].set(this, value);
                }
            } catch (Exception exp) {
                Logger.logError(exp, "Error in field: " + name + ", value: " + value);
            }
        }
    }

    private void deserializeComplexFields() {
        try {
            try {
                mProgramClientID.fromHexString(get("mProgramClientID"));
            } catch (Exception e) {
            }
            String ignoredHosts = get("mNetIgnoredHosts", "");
            StringTokenizer tokens = new StringTokenizer(ignoredHosts);
            if (mNetIgnoredHosts == null) {
                mNetIgnoredHosts = new ArrayList(tokens.countTokens());
            } else {
                mNetIgnoredHosts.clear();
                mNetInvalidHosts.ensureCapacity(tokens.countTokens());
            }
            while (tokens.hasMoreTokens()) {
                mNetIgnoredHosts.add(IPUtils.splitIP2Parts(tokens.nextToken()));
            }
            mNetInvalidHosts.trimToSize();
            String filteredSearchHosts = get("mFilteredSearchHosts", "");
            if (filteredSearchHosts != null) {
                tokens = new StringTokenizer(filteredSearchHosts);
                while (tokens.hasMoreTokens()) {
                    mFilteredSearchHosts.addElement(IPUtils.splitIP2Parts(tokens.nextToken()));
                }
            }
            String invalidHosts = get("mNetInvalidHosts");
            if (invalidHosts != null) {
                tokens = new StringTokenizer(invalidHosts);
                while (tokens.hasMoreTokens()) {
                    mNetInvalidHosts.addElement(IPUtils.splitIP2Parts(tokens.nextToken()));
                }
            }
            String invalidPorts = get("filteredCatcherPorts");
            if (invalidPorts != null) {
                tokens = new StringTokenizer(invalidPorts, " ");
                while (tokens.hasMoreTokens()) {
                    filteredCatcherPorts.add(tokens.nextToken());
                }
            }
            String networkHistory = get("mNetNetworkHistory", "");
            {
                tokens = new StringTokenizer(networkHistory);
                while (tokens.hasMoreTokens()) {
                    mNetNetworkHistory.addElement(tokens.nextToken());
                }
            }
            String connectToList = get("connectToHistory", "");
            if (connectToList != null) {
                tokens = new StringTokenizer(connectToList);
                while (tokens.hasMoreTokens()) {
                    connectToHistory.add(tokens.nextToken());
                }
            }
            String searchTermList = get("searchTermHistory", "");
            if (searchTermList != null) {
                tokens = new StringTokenizer(searchTermList, ",");
                while (tokens.hasMoreTokens()) {
                    searchTermHistory.add(tokens.nextToken().trim());
                }
            }
            String font;
            font = get("mFontMenu");
            if (font != null) {
                try {
                    tokens = new StringTokenizer(font, ";");
                    mFontMenu = new Font(tokens.nextToken(), Integer.parseInt(tokens.nextToken()), Integer.parseInt(tokens.nextToken()));
                } catch (Exception e) {
                }
            }
            font = get("mFontLabel");
            if (font != null) {
                try {
                    tokens = new StringTokenizer(font, ";");
                    mFontLabel = new Font(tokens.nextToken(), Integer.parseInt(tokens.nextToken()), Integer.parseInt(tokens.nextToken()));
                } catch (Exception e) {
                }
            }
            font = get("mFontTable");
            if (font != null) {
                try {
                    tokens = new StringTokenizer(font, ";");
                    mFontTable = new Font(tokens.nextToken(), Integer.parseInt(tokens.nextToken()), Integer.parseInt(tokens.nextToken()));
                } catch (Exception e) {
                }
            }
        } catch (Exception exp) {
            Logger.logError(exp);
        }
    }

    /**
     * For a HTTPURLConnection java uses configured proxy settings.
     */
    public void updateHTTPProxySettings() {
        System.setProperty("http.agent", Environment.getPhexVendor());
        if (isHttpProxyUsed) {
            System.setProperty("http.proxyHost", httpProxyHost);
            System.setProperty("http.proxyPort", String.valueOf(httpProxyPort));
        } else {
            System.setProperty("http.proxyHost", "");
            System.setProperty("http.proxyPort", "");
        }
    }

    private void handlePhexVersionAdjustments() {
        if ((runningPhexVersion == null || runningPhexVersion.length() == 0) && (runningBuildNumber == null || runningBuildNumber.length() == 0)) {
            return;
        }
        if (runningPhexVersion == null || runningPhexVersion.length() == 0) {
            runningPhexVersion = "0.6";
        }
        if (VersionUtils.compare("0.7", runningPhexVersion) > 0) {
            updatesFor0_7();
        }
        if (VersionUtils.compare("0.8", runningPhexVersion) > 0) {
            updatesFor0_8();
        }
        if (runningBuildNumber == null || runningBuildNumber.length() == 0) {
            runningBuildNumber = "35";
        }
        if (VersionUtils.compare("36", runningBuildNumber) > 0) {
            updatesForBuild36();
        }
        if (VersionUtils.compare("42", runningBuildNumber) > 0) {
            updatesForBuild42();
        }
        runningBuildNumber = Environment.getInstance().getProperty("build.number");
        runningPhexVersion = Res.getStr("Program.Version");
        save();
    }

    private void updatesFor0_7() {
        try {
            File downloadListFile = Environment.getInstance().getPhexConfigFile(EnvironmentConstants.XML_DOWNLOAD_FILE_NAME);
            if (downloadListFile.exists()) {
                FileUtils.copyFile(downloadListFile, new File(downloadListFile.getAbsolutePath() + ".v0.6.4"));
            }
        } catch (IOException exp) {
            Logger.logError(exp);
        }
        runningPhexVersion = "0.7";
    }

    private void updatesFor0_8() {
        peerConnections = mNetMinConn;
        runningPhexVersion = "0.8";
        mUploadMaxSearch = 64;
        mNetMaxHostToCatch = 1000;
    }

    private void updatesForBuild36() {
        runningBuildNumber = "36";
        allowToBecomeLeaf = allowUPConnections;
    }

    private void updatesForBuild42() {
        runningBuildNumber = "42";
        if (up2upConnections < DEFAULT_UP_2_UP_CONNECTIONS) {
            up2upConnections = DEFAULT_UP_2_UP_CONNECTIONS;
        }
    }
}
