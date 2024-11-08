package apmon;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import apmon.host.cmdExec;

/**
 * Data structure used for sending monitoring data to a MonaLisa module. 
 * The data is packed in UDP datagrams, in XDR format.
 * A datagram has the following structure:
 * - header which contains the ApMon version and the password for the MonALISA
 * host and has the following syntax: v:<ApMon_version>p:<password> 
 * - cluster name (string) 
 * - node name (string)
 * - number of parameters (int)
 * - for each parameter: name (string), value type (int), value
 * <BR>
 * There are two ways to send parameters:
 * 1) a single parameter in a packet (with the function sendParameter() which
 * has several overloaded variants
 * 2) multiple parameters in a packet (with the function sendParameters())
 * <BR>
 * ApMon can be configured to send periodically datagrams with monitoring information
 * concerning the current application or the whole system. Some of the monitoring 
 * information is only available on Linux systems.
 */
public class ApMon {

    static final String APMON_VERSION = "2.2.7";

    public static final int MAX_DGRAM_SIZE = 8192;

    /** < Maximum UDP datagram size. */
    public static final int XDR_STRING = 0;

    /** < Used to code the string data type */
    public static final int XDR_INT32 = 2;

    /** < Used to code the 4 bytes integer data type */
    public static final int XDR_REAL32 = 4;

    /** < Used to code the 4 bytes real data type */
    public static final int XDR_REAL64 = 5;

    /** < Used to code the 8 bytes real data type */
    public static final int DEFAULT_PORT = 8884;

    /** Time interval (in sec) at which job monitoring datagrams are sent. */
    public static final int JOB_MONITOR_INTERVAL = 20;

    /** Time interval (in sec) at which system monitoring datagams are sent. */
    public static final int SYS_MONITOR_INTERVAL = 20;

    /** Time interval (in sec) at which the configuration files are checked for changes. */
    public static final int RECHECK_INTERVAL = 600;

    /** The maxim number of mesages that will be sent to MonALISA */
    public static final int MAX_MSG_RATE = 20;

    /**
     * The number of time intervals at which ApMon sends general system monitoring information (considering the time
     * intervals at which ApMon sends system monitoring information).
     */
    public static final int GEN_MONITOR_INTERVALS = 100;

    /** Constant that indicates this object was initialized from a file. */
    static final int FILE_INIT = 1;

    /** Constant that indicates this object was initialized from a list. */
    static final int LIST_INIT = 2;

    /** Constant that indicates this object was initialized directly. */
    static final int DIRECT_INIT = 3;

    /** The initialization source (can be a file or a list). */
    Object initSource = null;

    /** The initialization type (from file / list / directly). */
    int initType;

    /**
     * The configuration file and the URLs are checked for changes at this numer of seconds (if the network connections
     * are good).
     */
    long recheckInterval = RECHECK_INTERVAL;

    /**
     * If the configuraion URLs cannot be reloaded, the interval until the next attempt will be increased. This is the
     * actual value of the interval that is used by ApMon
     */
    long crtRecheckInterval = RECHECK_INTERVAL;

    String clusterName;

    /** < The name of the monitored cluster. */
    String nodeName;

    /** < The name of the monitored node. */
    Vector destAddresses;

    /** < The IP addresses where the results will be sent. */
    Vector destPorts;

    /** < The ports where the destination hosts listen. */
    Vector destPasswds;

    /** < The Passwords used for the destination hosts. */
    byte[] buf;

    /** < The buffer which holds the message data (encoded in XDR). */
    int dgramSize;

    /**
     * Hashtable which holds the initialization resources (Files, URLs) that must be periodically checked for changes,
     * and their latest modification times
     */
    Hashtable confResources;

    ByteArrayOutputStream baos;

    DatagramSocket dgramSocket;

    /**
     * The background thread which performs operations like checking the configuration file/URLs for changes and sending
     * datagrams with monitoring information.
     */
    BkThread bkThread = null;

    /** Is true if the background thread was started. */
    boolean bkThreadStarted = false;

    /** Protects the variables that hold the settings for the background thread. */
    Object mutexBack = new Object();

    /** Used for the wait/notify mechanism in the background thread. */
    Object mutexCond = new Object();

    /** Indicates if any of the settings for the background thread was changed. */
    boolean condChanged = false;

    /** These flags indicate changes in the monitoring configuration. */
    boolean recheckChanged, jobMonChanged, sysMonChanged;

    /**
     * If this flag is set to true, when the value of a parameter cannot be read from proc/, ApMon will not attempt to
     * include that value in the next datagrams.
     */
    boolean autoDisableMonitoring = true;

    /** If this flag is true, the configuration file / URLs are periodically rechecked for changes. */
    boolean confCheck = false;

    /**
     * If this flag is true, packets with system information taken from /proc are periodically sent to MonALISA
     */
    boolean sysMonitoring = false;

    /**
     * If this flag is true, packets with job information taken from /proc are periodically sent to MonALISA
     */
    boolean jobMonitoring = false;

    /**
     * If this flag is true, packets with general system information taken from /proc are periodically sent to MonALISA
     */
    boolean genMonitoring = false;

    /** Job/System monitoring information obtained from /proc is sent at these time intervals */
    long jobMonitorInterval = JOB_MONITOR_INTERVAL;

    long sysMonitorInterval = SYS_MONITOR_INTERVAL;

    int maxMsgRate = MAX_MSG_RATE;

    /**
     * General system monitoring information is sent at a time interval equal to genMonitorIntervals *
     * sysMonitorInterval.
     */
    int genMonitorIntervals = GEN_MONITOR_INTERVALS;

    /**
     * Hashtables that associate the names of the parameters included in the monitoring datagrams and flags that
     * indicate whether they are active or not.
     */
    long sysMonitorParams, jobMonitorParams, genMonitorParams;

    /**
     * The time when the last datagram with job monitoring information was sent (in milliseconds since the Epoch).
     */
    long lastJobInfoSend;

    /**
     * The time when the last datagram with job monitoring information was sent (in milliseconds since the Epoch).
     */
    long lastSysInfoSend;

    /** The last value for "utime" for the current process that was read from proc/ (only on Linux). */
    double lastUtime;

    /** The last value for "stime" for the current process that was read from proc/ (only on Linux). */
    double lastStime;

    /** The name of the host on which ApMon currently runs. */
    String myHostname = null;

    /** The IP address of the host on which ApMon currently runs. */
    String myIP = null;

    /** The number of CPUs on the machine that runs ApMon. */
    int numCPUs;

    /** The names of the network interfaces on this machine. */
    Vector netInterfaces = new Vector();

    /** The IPs of this machine. */
    Vector allMyIPs = new Vector();

    /** the cluster name that will be included in the monitoring datagrams */
    String sysClusterName = "ApMon_userSend";

    /** the node name that will be included in the monitoring datagrams */
    String sysNodeName = null;

    Vector monJobs = new Vector();

    Hashtable sender = new Hashtable();

    private static Logger logger = Logger.getLogger("apmon");

    static String osName = System.getProperty("os.name");

    /** Java type -> XDR Type mapping **/
    private static Map mValueTypes = new HashMap();

    static {
        try {
            LogManager logManager = LogManager.getLogManager();
            if (logManager.getProperty("handlers") == null) {
                try {
                    FileHandler fh = null;
                    try {
                        fh = new FileHandler("apmon.log");
                        fh.setFormatter(new SimpleFormatter());
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    logger.setUseParentHandlers(false);
                    logger.addHandler(fh);
                    logger.setLevel(Level.INFO);
                } catch (Throwable t) {
                    System.err.println("[ ApMon ] [ static init ] [ logging ] Unable to load default logger props. Cause:");
                    t.printStackTrace();
                }
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "[ ApMon ] [ static init ] [ logging ] uses predefined logging properties");
                }
            }
        } catch (Throwable t) {
            System.err.println("[ ApMon ] [ static init ] [ logging ] Unable to check/load default logger props. Cause:");
            t.printStackTrace();
        }
        mValueTypes.put(String.class.getName(), new Integer(XDR_STRING));
        mValueTypes.put(Short.class.getName(), new Integer(XDR_INT32));
        mValueTypes.put(Integer.class.getName(), new Integer(XDR_INT32));
        mValueTypes.put(Float.class.getName(), new Integer(XDR_REAL64));
        mValueTypes.put(Double.class.getName(), new Integer(XDR_REAL64));
    }

    /**
     * Initializes an ApMon object from a configuration file.
     * 
     * @param filename
     *            The name of the file which contains the addresses and the ports of the destination hosts (see README
     *            for details about the structure of this file).
     * @throws ApMonException
     *             ,
     *             SocketException, IOException
     */
    public ApMon(String filename) throws ApMonException, SocketException, IOException {
        initType = FILE_INIT;
        initMonitoring();
        initSource = filename;
        initialize(filename, true);
        initSenderRef();
    }

    /** Add a job pid to monitorized jobs vector */
    public void addJobToMonitor(int pid, String workDir, String clusterName, String nodeName) {
        MonitoredJob job = new MonitoredJob(pid, workDir, clusterName, nodeName);
        if (!monJobs.contains(job)) monJobs.add(job); else if (logger.isLoggable(Level.WARNING)) logger.warning("Job <" + job + "> already exsist.");
    }

    /** Remove a pid form monitorized jobs vector */
    public void removeJobToMonitor(int pid) {
        int i;
        for (i = 0; i < monJobs.size(); i++) {
            MonitoredJob job = (MonitoredJob) monJobs.elementAt(i);
            if (job.getPid() == pid) {
                monJobs.remove(job);
                break;
            }
        }
    }

    /** This is used to set the cluster and node name for the system-related monitored data. */
    public void setMonitorClusterNode(String cName, String nName) {
        if (cName != null) sysClusterName = cName;
        if (nName != null) sysNodeName = nName;
    }

    /**
     * Initializes an ApMon object from a configuration file.
     * 
     * @param filename
     *            The name of the file which contains the addresses and the ports of the destination hosts (see README
     *            for details about the structure of this file).
     * @param firstTime
     *            If it is true, all the initializations will be done (the object is being constructed now). Else, only
     *            some structures will be reinitialized.
     * @throws ApMonException
     *             ,
     *             SocketException, IOException
     */
    void initialize(String filename, boolean firstTime) throws ApMonException, SocketException, IOException {
        Vector destAddresses = new Vector();
        Vector destPorts = new Vector();
        Vector destPasswds = new Vector();
        Hashtable confRes = new Hashtable();
        try {
            loadFile(filename, destAddresses, destPorts, destPasswds, confRes);
        } catch (Exception e) {
            if (firstTime) {
                if (e instanceof IOException) throw (IOException) e;
                if (e instanceof ApMonException) throw (ApMonException) e;
            } else {
                logger.warning("Configuration not reloaded successfully, keeping the previous one");
                return;
            }
        }
        synchronized (this) {
            arrayInit(destAddresses, destPorts, destPasswds, firstTime);
            this.confResources = confRes;
        }
    }

    /**
     * Initializes an ApMon object from a list with URLs.
     * 
     * @param initSource
     *            The list with URLs. the ports of the destination hosts (see README for details about the structure of
     *            this file).
     * @throws ApMonException
     *             ,
     *             SocketException, IOException
     */
    public ApMon(Vector destList) throws ApMonException, SocketException, IOException {
        initType = LIST_INIT;
        initMonitoring();
        initSource = destList;
        initialize(destList, true);
        initSenderRef();
    }

    /**
     * Initializes an ApMon object from a list with URLs.
     * 
     * @param initSource
     *            The list with URLs.
     * @param firstTime
     *            If it is true, all the initializations will be done (the object is being constructed now). Else, only
     *            some structures will be reinitialized.
     * @throws ApMonException
     *             ,
     *             SocketException, IOException
     */
    void initialize(Vector destList, boolean firstTime) throws ApMonException, SocketException, IOException {
        int i;
        Vector destAddresses = new Vector();
        Vector destPorts = new Vector();
        Vector destPasswds = new Vector();
        String dest;
        Hashtable confRes = new Hashtable();
        logger.info("Initializing destination addresses & ports:");
        try {
            for (i = 0; i < destList.size(); i++) {
                dest = (String) destList.get(i);
                if (dest.startsWith("http")) {
                    loadURL(dest, destAddresses, destPorts, destPasswds, confRes);
                } else {
                    addToDestinations(dest, destAddresses, destPorts, destPasswds);
                }
            }
        } catch (Exception e) {
            if (firstTime) {
                if (e instanceof IOException) throw (IOException) e;
                if (e instanceof ApMonException) throw (ApMonException) e;
                if (e instanceof SocketException) throw (SocketException) e;
            } else {
                logger.warning("Configuration not reloaded successfully, keeping the previous one");
                return;
            }
        }
        synchronized (this) {
            arrayInit(destAddresses, destPorts, destPasswds, firstTime);
            this.confResources = confRes;
        }
    }

    /**
     * Initializes an ApMon data structure, using arrays instead of a file.
     * 
     * @param nDestinations
     *            The number of destination hosts where the results will be sent.
     * @param destAddresses
     *            Array that contains the hostnames or IP addresses of the destination hosts.
     * @param destPorts
     *            The ports where the MonaLisa modules listen on the destination hosts.
     * @throws ApMonException
     *             ,
     *             SocketException, IOException
     */
    public ApMon(Vector destAddresses, Vector destPorts) throws ApMonException, SocketException, IOException {
        this.initType = DIRECT_INIT;
        arrayInit(destAddresses, destPorts, null);
        initSenderRef();
    }

    /**
     * Initializes an ApMon data structure, using arrays instead of a file.
     * 
     * @param nDestinations
     *            The number of destination hosts where the results will be sent.
     * @param destAddresses
     *            Array that contains the hostnames or IP addresses of the destination hosts.
     * @param destPorts
     *            The ports where the MonaLisa modules listen on the destination hosts.
     * @param destPasswds
     *            The passwords for the destination hosts.
     * @throws ApMonException
     *             ,
     *             SocketException, IOException
     */
    public ApMon(Vector destAddresses, Vector destPorts, Vector destPasswds) throws ApMonException, SocketException, IOException {
        this.initType = DIRECT_INIT;
        initMonitoring();
        arrayInit(destAddresses, destPorts, destPasswds);
        initSenderRef();
    }

    /**
     * Parses a configuration file which contains addresses, ports and passwords for the destination hosts and puts the
     * results in the vectors given as parameters.
     * 
     * @param filename
     *            The name of the configuration file.
     * @param destAddresses
     *            Will contain the destination addresses.
     * @param destPorts
     *            Will contain the ports from the destination hosts.
     * @param destPasswds
     *            Will contain the passwords for the destination hosts.
     * @param confRes
     *            Will contain the configuration resources (file, URLs).
     * @throws IOException
     *             ,
     *             ApMonException
     */
    void loadFile(String filename, Vector destAddresses, Vector destPorts, Vector destPasswds, Hashtable confRes) throws IOException, ApMonException {
        String line, tmp;
        BufferedReader in = new BufferedReader(new FileReader(filename));
        confRes.put(new File(filename), new Long(System.currentTimeMillis()));
        logger.info("Loading file " + filename + "...");
        while ((line = in.readLine()) != null) {
            tmp = line.trim();
            if (tmp.length() == 0 || tmp.startsWith("#")) continue;
            if (tmp.startsWith("xApMon_loglevel")) {
                StringTokenizer lst = new StringTokenizer(tmp, " =");
                lst.nextToken();
                setLogLevel(lst.nextToken());
                continue;
            }
            if (tmp.startsWith("xApMon_")) {
                parseXApMonLine(tmp);
                continue;
            }
            addToDestinations(tmp, destAddresses, destPorts, destPasswds);
        }
    }

    /**
     * Parses a web page which contains addresses, ports and passwords for the destination hosts and puts the results in
     * the vectors given as parameters.
     * 
     * @param destAddresses
     *            Will contain the destination addresses.
     * @param destPorts
     *            Will contain the ports from the destination hosts.
     * @param destPasswds
     *            Will contain the passwords for the destination hosts.
     * @param confRes
     *            Will contain the configuration resources (file, URLs).
     */
    void loadURL(String url, Vector destAddresses, Vector destPorts, Vector destPasswds, Hashtable confRes) throws IOException, ApMonException {
        System.setProperty("sun.net.client.defaultConnectTimeout", "5000");
        System.setProperty("sun.net.client.defaultReadTimeout", "5000");
        URL destURL = null;
        try {
            destURL = new URL(url);
        } catch (MalformedURLException e) {
            throw new ApMonException(e.getMessage());
        }
        URLConnection urlConn = destURL.openConnection();
        long lmt = urlConn.getLastModified();
        confRes.put(new URL(url), new Long(lmt));
        logger.info("Loading from URL " + url + "...");
        BufferedReader br = new BufferedReader(new InputStreamReader(destURL.openStream()));
        String destLine;
        while ((destLine = br.readLine()) != null) {
            String tmp2 = destLine.trim();
            if (tmp2.length() == 0 || tmp2.startsWith("#")) continue;
            if (tmp2.startsWith("xApMon_loglevel")) {
                StringTokenizer lst = new StringTokenizer(tmp2, " =");
                lst.nextToken();
                setLogLevel(lst.nextToken());
                continue;
            }
            if (tmp2.startsWith("xApMon_")) {
                parseXApMonLine(tmp2);
                continue;
            }
            addToDestinations(tmp2, destAddresses, destPorts, destPasswds);
        }
        br.close();
    }

    /**
     * Parses a line from a (local or remote) configuration file and adds the address and the port to the vectors that
     * are given as parameters.
     * 
     * @param line
     *            The line to be parsed.
     * @param destAddresses
     *            Contains destination addresses.
     * @param destPorts
     *            Contains the ports from the destination hosts.
     * @param destPasswds
     *            Contains the passwords for the destination hosts.
     */
    void addToDestinations(String line, Vector destAddresses, Vector destPorts, Vector destPasswds) {
        String addr;
        int port = DEFAULT_PORT;
        String tokens[] = line.split("(\\s)+");
        String passwd = "";
        if (tokens == null) return;
        line = tokens[0].trim();
        if (tokens.length > 1) passwd = tokens[1].trim();
        StringTokenizer st = new StringTokenizer(line, ":");
        addr = st.nextToken();
        try {
            if (st.hasMoreTokens()) port = Integer.parseInt(st.nextToken()); else port = DEFAULT_PORT;
        } catch (NumberFormatException e) {
            logger.warning("Wrong address: " + line);
        }
        destAddresses.add(addr);
        destPorts.add(new Integer(port));
        if (passwd != null) destPasswds.add(passwd);
    }

    /**
     * Internal method used to initialize an ApMon data structure.
     * 
     * @param nDestinations
     *            The number of destination hosts where the results will be sent.
     * @param destAddresses
     *            Array that contains the hostnames or IP addresses of the destination hosts.
     * @param destPorts
     *            The ports where the MonaLisa modules listen on the destination hosts.
     * @param destPasswds
     *            The passwords for the destination hosts.
     * @throws ApMonException
     *             ,
     *             SocketException, IOException
     */
    void arrayInit(Vector destAddresses, Vector destPorts, Vector destPasswds) throws ApMonException, SocketException, IOException {
        arrayInit(destAddresses, destPorts, destPasswds, true);
    }

    /**
     * Internal method used to initialize an ApMon data structure.
     * 
     * @param nDestinations
     *            The number of destination hosts where the results will be sent.
     * @param destAddresses
     *            Array that contains the hostnames or IP addresses of the destination hosts.
     * @param destPorts
     *            The ports where the MonaLisa modules listen on the destination hosts.
     * @param destPasswds
     *            The passwords for the destination hosts.
     * @param firstTime
     *            If it is true, all the initializations will be done (the object is being constructed now). Else, only
     *            some of the data structures will be reinitialized.
     * @throws ApMonException
     *             ,
     *             SocketException, IOException
     */
    void arrayInit(Vector destAddresses, Vector destPorts, Vector destPasswds, boolean firstTime) throws ApMonException, SocketException, IOException {
        Vector tmpAddresses, tmpPorts, tmpPasswds;
        if (destAddresses.size() == 0 || destPorts.size() == 0) throw new ApMonException("No destination hosts specified");
        tmpAddresses = new Vector();
        tmpPorts = new Vector();
        tmpPasswds = new Vector();
        for (int i = 0; i < destAddresses.size(); i++) {
            InetAddress inetAddr = InetAddress.getByName((String) destAddresses.get(i));
            String ipAddr = inetAddr.getHostAddress();
            if (!tmpAddresses.contains(ipAddr)) {
                tmpAddresses.add(ipAddr);
                tmpPorts.add(destPorts.get(i));
                if (destPasswds != null) {
                    tmpPasswds.add(destPasswds.get(i));
                }
                logger.info("adding destination: " + ipAddr + ":" + destPorts.get(i));
            }
        }
        synchronized (this) {
            this.destPorts = new Vector(tmpPorts);
            this.destAddresses = new Vector(tmpAddresses);
            this.destPasswds = new Vector(tmpPasswds);
        }
        if (firstTime) {
            cmdExec exec = new cmdExec();
            myHostname = exec.executeCommand("hostname -f", "");
            exec.stopIt();
            sysNodeName = myHostname;
            dgramSocket = new DatagramSocket();
            lastJobInfoSend = System.currentTimeMillis();
            try {
                lastSysInfoSend = BkThread.getBootTime();
            } catch (Exception e) {
                logger.warning("Error reading boot time from /proc/stat/.");
                lastSysInfoSend = 0;
            }
            lastUtime = lastStime = 0;
            BkThread.getNetConfig(netInterfaces, allMyIPs);
            if (allMyIPs.size() > 0) this.myIP = (String) allMyIPs.get(0); else this.myIP = "unknown";
            try {
                baos = new ByteArrayOutputStream();
            } catch (Throwable t) {
                logger.log(Level.WARNING, "", t);
                throw new ApMonException("Got General Exception while encoding:" + t);
            }
        }
        setJobMonitoring(jobMonitoring, jobMonitorInterval);
        setSysMonitoring(sysMonitoring, sysMonitorInterval);
        setGenMonitoring(genMonitoring, genMonitorIntervals);
        setConfRecheck(confCheck, recheckInterval);
    }

    /** For backward compatibility. */
    public void sendTimedParameters(String clusterName, String nodeName, int nParams, Vector paramNames, Vector valueTypes, Vector paramValues, int timestamp) throws ApMonException, UnknownHostException, SocketException, IOException {
        sendTimedParameters(clusterName, nodeName, nParams, paramNames, paramValues, timestamp);
    }

    /**
     * Sends a set of parameters and thier values to the MonALISA module.
     * 
     * @param clusterName
     *            The name of the cluster that is monitored.
     * @param nodeName
     *            The name of the node from the cluster from which the value was taken.
     * @param paramNames
     *            Vector with the names of the parameters.
     * @param paramValues
     *            Vector with the values of the parameters.
     * @throws ApMonException
     *             ,
     *             UnknownHostException, SocketException
     * @param timestamp
     *            The user's timestamp
     */
    public void sendTimedParameters(String clusterName, String nodeName, int nParams, Vector paramNames, Vector paramValues, int timestamp) throws ApMonException, UnknownHostException, SocketException, IOException {
        int i;
        if (!shouldSend()) return;
        if (clusterName != null) {
            this.clusterName = clusterName;
            if (nodeName != null) this.nodeName = new String(nodeName); else {
                this.nodeName = this.myHostname;
            }
        }
        updateSEQ_NR();
        encodeParams(nParams, paramNames, paramValues, timestamp);
        synchronized (this) {
            for (i = 0; i < destAddresses.size(); i++) {
                InetAddress destAddr = InetAddress.getByName((String) destAddresses.get(i));
                int port = ((Integer) destPorts.get(i)).intValue();
                String header = "v:" + APMON_VERSION + "_jp:";
                String passwd = "";
                if (destPasswds != null && destPasswds.size() == destAddresses.size()) {
                    passwd = (String) destPasswds.get(i);
                }
                header += passwd;
                byte[] newBuff = null;
                try {
                    XDROutputStream xdrOS = new XDROutputStream(baos);
                    xdrOS.writeString(header);
                    xdrOS.pad();
                    xdrOS.writeInt(((Integer) sender.get("INSTANCE_ID")).intValue());
                    xdrOS.pad();
                    xdrOS.writeInt(((Integer) sender.get("SEQ_NR")).intValue());
                    xdrOS.pad();
                    xdrOS.flush();
                    byte[] tmpbuf = baos.toByteArray();
                    baos.reset();
                    newBuff = new byte[tmpbuf.length + buf.length];
                    System.arraycopy(tmpbuf, 0, newBuff, 0, tmpbuf.length);
                    System.arraycopy(buf, 0, newBuff, tmpbuf.length, buf.length);
                } catch (Throwable t) {
                    logger.warning("Cannot add ApMon header...." + t);
                    newBuff = buf;
                }
                if (newBuff == null || newBuff.length == 0) {
                    logger.warning("Cannot send null or 0 length buffer!!");
                    continue;
                }
                dgramSize = newBuff.length;
                DatagramPacket dp = new DatagramPacket(newBuff, dgramSize, destAddr, port);
                try {
                    dgramSocket.send(dp);
                } catch (IOException e) {
                    if (logger.isLoggable(Level.WARNING)) logger.warning("Error sending parameters to " + destAddresses.get(i));
                    dgramSocket.close();
                    dgramSocket = new DatagramSocket();
                }
                if (logger.isLoggable(Level.FINE)) {
                    StringBuffer sbLogMsg = new StringBuffer();
                    sbLogMsg.append(" Datagram with size ").append(dgramSize);
                    sbLogMsg.append(" sent to ").append(destAddresses.get(i)).append(", containing parameters:\n");
                    sbLogMsg.append(printParameters(paramNames, paramValues));
                    logger.log(Level.FINE, sbLogMsg.toString());
                }
            }
        }
    }

    /** For backward compatibility. */
    public void sendParameters(String clusterName, String nodeName, int nParams, Vector paramNames, Vector valueTypes, Vector paramValues) throws ApMonException, UnknownHostException, SocketException, IOException {
        sendParameters(clusterName, nodeName, nParams, paramNames, paramValues);
    }

    /**
     * Sends a set of parameters and thier values to the MonALISA module.
     * 
     * @param clusterName
     *            The name of the cluster that is monitored.
     * @param nodeName
     *            The name of the node from the cluster from which the value was taken.
     * @param paramNames
     *            Vector with the names of the parameters.
     * @param paramValues
     *            Vector with the values of the parameters.
     * @throws ApMonException
     *             ,
     *             UnknownHostException, SocketException
     */
    public void sendParameters(String clusterName, String nodeName, int nParams, Vector paramNames, Vector paramValues) throws ApMonException, UnknownHostException, SocketException, IOException {
        sendTimedParameters(clusterName, nodeName, nParams, paramNames, paramValues, -1);
    }

    /** For backward compatibility. */
    public void sendParameter(String clusterName, String nodeName, String paramName, int valueType, Object paramValue) throws ApMonException, UnknownHostException, SocketException, IOException {
        sendParameter(clusterName, nodeName, paramName, paramValue);
    }

    /**
     * Sends a parameter and its value to the MonALISA module.
     * 
     * @param clusterName
     *            The name of the cluster that is monitored. If it is NULL, we keep the same cluster and node name as in
     *            the previous datagram.
     * @param nodeName
     *            The name of the node from the cluster from which the value was taken.
     * @param paramName
     *            The name of the parameter.
     * @param paramValue
     *            The value of the parameter.
     * @throws ApMonException
     *             ,
     *             UnknownHostException, SocketException
     */
    public void sendParameter(String clusterName, String nodeName, String paramName, Object paramValue) throws ApMonException, UnknownHostException, SocketException, IOException {
        Vector paramNames = new Vector();
        paramNames.add(paramName);
        Vector paramValues = new Vector();
        paramValues.add(paramValue);
        sendParameters(clusterName, nodeName, 1, paramNames, paramValues);
    }

    /** For backward compatibility. */
    public void sendTimedParameter(String clusterName, String nodeName, String paramName, int valueType, Object paramValue, int timestamp) throws ApMonException, UnknownHostException, SocketException, IOException {
        sendTimedParameter(clusterName, nodeName, paramName, paramValue, timestamp);
    }

    /**
     * Sends a parameter and its value to the MonALISA module.
     * 
     * @param clusterName
     *            The name of the cluster that is monitored. If it is NULL, we keep the same cluster and node name as in
     *            the previous datagram.
     * @param nodeName
     *            The name of the node from the cluster from which the value was taken.
     * @param paramName
     *            The name of the parameter.
     * @param paramValue
     *            The value of the parameter.
     * @throws ApMonException
     *             ,
     *             UnknownHostException, SocketException
     * @param timestamp
     *            The user's timestamp
     */
    public void sendTimedParameter(String clusterName, String nodeName, String paramName, Object paramValue, int timestamp) throws ApMonException, UnknownHostException, SocketException, IOException {
        Vector paramNames = new Vector();
        paramNames.add(paramName);
        Vector paramValues = new Vector();
        paramValues.add(paramValue);
        sendTimedParameters(clusterName, nodeName, 1, paramNames, paramValues, timestamp);
    }

    /**
     * Sends an integer parameter and its value to the MonALISA module.
     * 
     * @param clusterName
     *            The name of the cluster that is monitored. If it is NULL, we keep the same cluster and node name as in
     *            the previous datagram.
     * @param nodeName
     *            The name of the node from the cluster from which the value was taken.
     * @param paramName
     *            The name of the parameter.
     * @param paramValue
     *            The value of the parameter.
     * @throws ApMonException
     *             ,
     *             UnknownHostException, SocketException
     */
    public void sendParameter(String clusterName, String nodeName, String paramName, int paramValue) throws ApMonException, UnknownHostException, SocketException, IOException {
        sendParameter(clusterName, nodeName, paramName, new Integer(paramValue));
    }

    /**
     * Sends an integer parameter and its value to the MonALISA module.
     * 
     * @param clusterName
     *            The name of the cluster that is monitored. If it is NULL, we keep the same cluster and node name as in
     *            the previous datagram.
     * @param nodeName
     *            The name of the node from the cluster from which the value was taken.
     * @param paramName
     *            The name of the parameter.
     * @param paramValue
     *            The value of the parameter.
     * @throws ApMonException
     *             ,
     *             UnknownHostException, SocketException
     * @param timestamp
     *            The user's timestamp
     */
    public void sendTimedParameter(String clusterName, String nodeName, String paramName, int paramValue, int timestamp) throws ApMonException, UnknownHostException, SocketException, IOException {
        sendTimedParameter(clusterName, nodeName, paramName, new Integer(paramValue), timestamp);
    }

    /**
     * Sends a parameter of type double and its value to the MonALISA module.
     * 
     * @param clusterName
     *            The name of the cluster that is monitored. If it is NULL,we keep the same cluster and node name as in
     *            the previous datagram.
     * @param nodeName
     *            The name of the node from the cluster from which the value was taken.
     * @param paramName
     *            The name of the parameter.
     * @param paramValue
     *            The value of the parameter.
     * @throws ApMonException
     *             ,
     *             UnknownHostException, SocketException
     */
    public void sendParameter(String clusterName, String nodeName, String paramName, double paramValue) throws ApMonException, UnknownHostException, SocketException, IOException {
        sendParameter(clusterName, nodeName, paramName, new Double(paramValue));
    }

    /**
     * Sends an integer parameter and its value to the MonALISA module.
     * 
     * @param clusterName
     *            The name of the cluster that is monitored. If it is NULL, we keep the same cluster and node name as in
     *            the previous datagram.
     * @param nodeName
     *            The name of the node from the cluster from which the value was taken.
     * @param paramName
     *            The name of the parameter.
     * @param paramValue
     *            The value of the parameter.
     * @throws ApMonException
     *             ,
     *             UnknownHostException, SocketException
     * @param timestamp
     *            The user's timestamp
     */
    public void sendTimedParameter(String clusterName, String nodeName, String paramName, double paramValue, int timestamp) throws ApMonException, UnknownHostException, SocketException, IOException {
        sendTimedParameter(clusterName, nodeName, paramName, new Double(paramValue), timestamp);
    }

    /**
     * Checks that the size of the stream does not exceed the maximum size of an UDP diagram.
     */
    void ensureSize(ByteArrayOutputStream baos) throws ApMonException {
        if (baos == null) throw new ApMonException("Null ByteArrayOutputStream");
        if (baos.size() > MAX_DGRAM_SIZE) throw new ApMonException("Maximum datagram size exceeded");
    }

    /**
     * Encodes in the XDR format the data from a ApMon structure. Must be called before sending the data over the
     * newtork.
     * 
     * @throws ApMonException
     */
    void encodeParams(int nParams, Vector paramNames, Vector paramValues, int timestamp) throws ApMonException {
        int i, valType;
        try {
            XDROutputStream xdrOS = new XDROutputStream(baos);
            ensureSize(baos);
            xdrOS.writeString(clusterName);
            xdrOS.pad();
            xdrOS.writeString(nodeName);
            xdrOS.pad();
            xdrOS.writeInt(nParams);
            xdrOS.pad();
            Object oValue;
            for (i = 0; i < nParams; i++) {
                xdrOS.writeString((String) paramNames.get(i));
                xdrOS.pad();
                oValue = paramValues.get(i);
                valType = ((Integer) mValueTypes.get(oValue.getClass().getName())).intValue();
                xdrOS.writeInt(valType);
                xdrOS.pad();
                switch(valType) {
                    case XDR_STRING:
                        xdrOS.writeString((String) paramValues.get(i));
                        break;
                    case XDR_INT32:
                        int ival = ((Integer) paramValues.get(i)).intValue();
                        xdrOS.writeInt(ival);
                        break;
                    case XDR_REAL64:
                        double dval = ((Double) paramValues.get(i)).doubleValue();
                        xdrOS.writeDouble(dval);
                        break;
                    default:
                        throw new ApMonException("Unknown type for XDR encoding");
                }
                xdrOS.pad();
            }
            if (timestamp > 0) {
                xdrOS.writeInt(timestamp);
                xdrOS.pad();
            }
            ensureSize(baos);
            xdrOS.flush();
            buf = baos.toByteArray();
            baos.reset();
            logger.fine("Send buffer length: " + buf.length + "B");
        } catch (Throwable t) {
            logger.log(Level.WARNING, "", t);
            throw new ApMonException("Got General Exception while encoding:" + t);
        }
    }

    /**
     * Returns the value of the confCheck flag. If it is true, the configuration file and/or the URLs are periodically
     * checked for modifications.
     */
    public boolean getConfCheck() {
        boolean val;
        synchronized (mutexBack) {
            val = this.confCheck;
        }
        return val;
    }

    /**
     * Settings for the periodical configuration rechecking feature.
     * 
     * @param confRecheck
     *            If it is true, the configuration rechecking is enabled.
     * @param interval
     *            The time interval at which the verifications are done. The interval will be automatically increased if
     *            ApMon cannot connect to the configuration URLs.
     */
    public void setConfRecheck(boolean confCheck, long interval) {
        int val = -1;
        if (confCheck) logger.info("Enabling configuration reloading (interval " + interval + " s)");
        synchronized (mutexBack) {
            if (initType == DIRECT_INIT) {
                logger.warning("setConfRecheck(): no configuration file/URL to reload\n");
            } else {
                this.confCheck = confCheck;
                if (confCheck) {
                    if (interval > 0) {
                        this.recheckInterval = interval;
                        this.crtRecheckInterval = interval;
                    } else {
                        this.recheckInterval = RECHECK_INTERVAL;
                        this.crtRecheckInterval = RECHECK_INTERVAL;
                    }
                    val = 1;
                } else {
                    if (jobMonitoring == false && sysMonitoring == false) val = 0;
                }
            }
        }
        if (val == 1) {
            setBackgroundThread(true);
            return;
        }
        if (val == 0) {
            setBackgroundThread(false);
            return;
        }
    }

    /**
     * Returns the requested value of the time interval (in seconds) between two recheck operations for the
     * configuration files.
     */
    public long getRecheckInterval() {
        long val;
        synchronized (mutexBack) {
            val = this.recheckInterval;
        }
        return val;
    }

    /**
     * Returns the actual value of the time interval (in seconds) between two recheck operations for the configuration
     * file/URLs.
     */
    long getCrtRecheckInterval() {
        long val;
        synchronized (mutexBack) {
            val = this.crtRecheckInterval;
        }
        return val;
    }

    /**
     * Sets the value of the time interval (in seconds) between two recheck operations for the configuration file/URLs.
     * If the value is negative, the configuration rechecking is
     * turned off.
     */
    public void setRecheckInterval(long val) {
        if (val > 0) setConfRecheck(true, val); else setConfRecheck(false, val);
    }

    void setCrtRecheckInterval(long val) {
        synchronized (mutexBack) {
            crtRecheckInterval = val;
        }
    }

    /**
     * Settings for the job monitoring feature.
     * 
     * @param sysMonitoring
     *            If it is true, the job monitoring is enabled.
     * @param interval
     *            The time interval at which the job monitoring datagrams are sent.
     */
    public void setJobMonitoring(boolean jobMonitoring, long interval) {
        int val = -1;
        if (jobMonitoring) logger.info("Enabling job monitoring, time interval " + interval + " s"); else logger.info("Disabling job monitoring...");
        synchronized (mutexBack) {
            this.jobMonitoring = jobMonitoring;
            this.jobMonChanged = true;
            if (jobMonitoring == true) {
                if (interval > 0) this.jobMonitorInterval = interval; else this.jobMonitorInterval = JOB_MONITOR_INTERVAL;
                val = 1;
            } else {
                if (this.sysMonitoring == false && this.confCheck == false) val = 0;
            }
        }
        if (val == 1) {
            setBackgroundThread(true);
            return;
        }
        if (val == 0) {
            setBackgroundThread(false);
            return;
        }
    }

    /** Returns the value of the interval at which the job monitoring datagrams are sent. */
    public long getJobMonitorInterval() {
        long val;
        synchronized (mutexBack) {
            val = this.jobMonitorInterval;
        }
        return val;
    }

    /** Returns true if the job monitoring is enabled and false otherwise. */
    public boolean getJobMonitoring() {
        boolean val;
        synchronized (mutexBack) {
            val = this.jobMonitoring;
        }
        return val;
    }

    /**
     * Settings for the system monitoring feature.
     * 
     * @param sysMonitoring
     *            If it is true, the system monitoring is enabled.
     * @param interval
     *            The time interval at which the system monitoring datagrams are sent.
     */
    public void setSysMonitoring(boolean sysMonitoring, long interval) {
        int val = -1;
        if (sysMonitoring) logger.info("Enabling system monitoring, time interval " + interval + " s"); else logger.info("Disabling system monitoring...");
        synchronized (mutexBack) {
            this.sysMonitoring = sysMonitoring;
            this.sysMonChanged = true;
            if (sysMonitoring == true) {
                if (interval > 0) this.sysMonitorInterval = interval; else this.sysMonitorInterval = SYS_MONITOR_INTERVAL;
                val = 1;
            } else {
                if (this.jobMonitoring == false && this.confCheck == false) val = 0;
            }
        }
        if (val == 1) {
            setBackgroundThread(true);
            return;
        }
        if (val == 0) {
            setBackgroundThread(false);
            return;
        }
    }

    /** Returns the value of the interval at which the system monitoring datagrams are sent. */
    public long getSysMonitorInterval() {
        long val;
        synchronized (mutexBack) {
            val = this.sysMonitorInterval;
        }
        return val;
    }

    /** Returns true if the job monitoring is enabled and false otherwise. */
    public boolean getSysMonitoring() {
        boolean val;
        synchronized (mutexBack) {
            val = this.sysMonitoring;
        }
        return val;
    }

    /**
     * Settings for the general system monitoring feature.
     * 
     * @param genMonitoring
     *            If it is true, the general system monitoring is enabled.
     * @param interval
     *            The number of time intervals at which the general system monitoring datagrams are sent (a
     *            "time interval" is the time interval between two subsequent system
     *            monitoring datagrams).
     */
    public void setGenMonitoring(boolean genMonitoring, int nIntervals) {
        logger.info("Setting general information monitoring to " + genMonitoring);
        synchronized (mutexBack) {
            this.genMonitoring = genMonitoring;
            this.recheckChanged = true;
            if (genMonitoring == true) {
                if (nIntervals > 0) this.genMonitorIntervals = nIntervals; else this.genMonitorIntervals = GEN_MONITOR_INTERVALS;
            }
        }
        if (genMonitoring && this.sysMonitoring == false) {
            setSysMonitoring(true, SYS_MONITOR_INTERVAL);
        }
    }

    /** Returns true if the general system monitoring is enabled and false otherwise. */
    public boolean getGenMonitoring() {
        boolean val;
        synchronized (mutexBack) {
            val = this.genMonitoring;
        }
        return val;
    }

    public Double getSystemParameter(String paramName) {
        if (bkThread == null) {
            logger.info("The background thread is not started - returning null");
            return null;
        }
        if (bkThread.monitor == null) {
            logger.info("No HostPropertiesMonitor defined - returning null");
            return null;
        }
        HashMap hms = bkThread.monitor.getHashParams();
        if (hms == null) {
            logger.info("No parameters map defined - returning null");
            return null;
        }
        Long paramId = (Long) ApMonMonitoringConstants.HT_SYS_NAMES_TO_CONSTANTS.get("sys_" + paramName);
        if (paramId == null) {
            logger.info("The parameter " + paramName + " does not exist.");
            return null;
        }
        String paramValue = (String) hms.get(paramId);
        double dVal = -1;
        try {
            dVal = Double.parseDouble(paramValue);
        } catch (Exception e) {
            logger.info("Could not obtain parameter value from the map: " + paramName);
            return null;
        }
        return new Double(dVal);
    }

    /** Enables or disables the background thread. */
    void setBackgroundThread(boolean val) {
        boolean stoppedThread = false;
        synchronized (mutexCond) {
            condChanged = false;
            if (val == true) if (!bkThreadStarted) {
                bkThreadStarted = true;
                bkThread = new BkThread(this);
                bkThread.start();
            } else {
                condChanged = true;
                mutexCond.notify();
            }
            if (val == false && bkThreadStarted) {
                bkThread.stopIt();
                condChanged = true;
                mutexCond.notify();
                stoppedThread = true;
                logger.info("[Stopping the thread for configuration reloading...]\n");
            }
        }
        if (stoppedThread) {
            try {
                bkThread.join();
            } catch (Exception e) {
            }
            bkThreadStarted = false;
        }
    }

    /**
     * Sets the ApMon loglevel. The possible values are: "FATAL", "WARNING", "INFO", "FINE", "DEBUG".
     */
    public static void setLogLevel(String newLevel_s) {
        int i;
        String levels_s[] = { "FATAL", "WARNING", "INFO", "FINE", "DEBUG" };
        Level levels[] = { Level.SEVERE, Level.WARNING, Level.INFO, Level.FINE, Level.FINEST };
        for (i = 0; i < 5; i++) if (newLevel_s.equals(levels_s[i])) break;
        if (i >= 5) {
            logger.warning("[ setLogLevel() ] Invalid level value: " + newLevel_s);
            return;
        }
        logger.info("Setting logging level to " + newLevel_s);
        logger.setLevel(levels[i]);
    }

    /**
     * This sets the maxim number of messages that are send to MonALISA in one second. Default, this number is 50.
     */
    public void setMaxMsgRate(int maxRate) {
        this.maxMsgRate = maxRate;
    }

    /**
     * Must be called when the ApMon object is no longer in use. Closes the UDP socket used for sending the parameters
     * and sends a last job monitoring datagram to register the time
     * when the application was finished.
     */
    public void stopIt() {
        if (bkThreadStarted) {
            if (jobMonitoring) {
                logger.info("Sending last job monitoring packet...");
                bkThread.sendJobInfo();
            }
        }
        dgramSocket.close();
        if (bkThread != null) bkThread.monitor.stopIt();
        setBackgroundThread(false);
    }

    /** Initializes the data structures used to configure the monitoring part of ApMon. */
    void initMonitoring() {
        autoDisableMonitoring = true;
        sysMonitoring = false;
        jobMonitoring = false;
        genMonitoring = false;
        confCheck = false;
        recheckInterval = RECHECK_INTERVAL;
        crtRecheckInterval = RECHECK_INTERVAL;
        jobMonitorInterval = JOB_MONITOR_INTERVAL;
        sysMonitorInterval = SYS_MONITOR_INTERVAL;
        sysMonitorParams = 0L;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_CPU_USAGE;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_LOAD1;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_LOAD5;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_LOAD15;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_CPU_USR;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_CPU_SYS;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_CPU_IDLE;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_CPU_NICE;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_MEM_FREE;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_MEM_USAGE;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_MEM_USED;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_PAGES_IN;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_PAGES_OUT;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_NET_IN;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_NET_OUT;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_PROCESSES;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_NET_SOCKETS;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_NET_TCP_DETAILS;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_SWAP_USED;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_SWAP_FREE;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_SWAP_USAGE;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_NET_ERRS;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_UPTIME;
        genMonitorParams = 0L;
        genMonitorParams |= ApMonMonitoringConstants.GEN_HOSTNAME;
        genMonitorParams |= ApMonMonitoringConstants.GEN_IP;
        if (osName.indexOf("Linux") >= 0) {
            genMonitorParams |= ApMonMonitoringConstants.GEN_CPU_MHZ;
            genMonitorParams |= ApMonMonitoringConstants.GEN_NO_CPUS;
            genMonitorParams |= ApMonMonitoringConstants.GEN_TOTAL_MEM;
            genMonitorParams |= ApMonMonitoringConstants.GEN_TOTAL_SWAP;
            genMonitorParams |= ApMonMonitoringConstants.GEN_CPU_VENDOR_ID;
            genMonitorParams |= ApMonMonitoringConstants.GEN_CPU_FAMILY;
            genMonitorParams |= ApMonMonitoringConstants.GEN_CPU_MODEL;
            genMonitorParams |= ApMonMonitoringConstants.GEN_CPU_MODEL_NAME;
            genMonitorParams |= ApMonMonitoringConstants.GEN_BOGOMIPS;
        }
        jobMonitorParams = 0L;
        jobMonitorParams |= ApMonMonitoringConstants.JOB_RUN_TIME;
        jobMonitorParams |= ApMonMonitoringConstants.JOB_CPU_TIME;
        jobMonitorParams |= ApMonMonitoringConstants.JOB_CPU_USAGE;
        jobMonitorParams |= ApMonMonitoringConstants.JOB_MEM_USAGE;
        jobMonitorParams |= ApMonMonitoringConstants.JOB_WORKDIR_SIZE;
        jobMonitorParams |= ApMonMonitoringConstants.JOB_DISK_TOTAL;
        jobMonitorParams |= ApMonMonitoringConstants.JOB_DISK_USED;
        jobMonitorParams |= ApMonMonitoringConstants.JOB_DISK_FREE;
        jobMonitorParams |= ApMonMonitoringConstants.JOB_DISK_USAGE;
        jobMonitorParams |= ApMonMonitoringConstants.JOB_VIRTUALMEM;
        jobMonitorParams |= ApMonMonitoringConstants.JOB_RSS;
        jobMonitorParams |= ApMonMonitoringConstants.JOB_OPEN_FILES;
    }

    /*******************************************************************************************************************************************************************************
     * Parses a "xApMon" line from the configuration file and changes the ApMon settings according to the line.
     */
    protected void parseXApMonLine(String line) {
        boolean flag = false, found;
        String tmp = line.replaceFirst("xApMon_", "");
        StringTokenizer st = new StringTokenizer(tmp, " \t=");
        String param = st.nextToken();
        String value = st.nextToken();
        if (value.equals("on")) flag = true;
        if (value.equals("off")) flag = false;
        synchronized (mutexBack) {
            found = false;
            if (param.equals("job_monitoring")) {
                this.jobMonitoring = flag;
                found = true;
            }
            if (param.equals("sys_monitoring")) {
                this.sysMonitoring = flag;
                found = true;
            }
            if (param.equals("job_interval")) {
                this.jobMonitorInterval = Long.parseLong(value);
                found = true;
            }
            if (param.equals("sys_interval")) {
                this.sysMonitorInterval = Long.parseLong(value);
                found = true;
            }
            if (param.equals("general_info")) {
                this.genMonitoring = flag;
                found = true;
            }
            if (param.equals("conf_recheck")) {
                this.confCheck = flag;
                found = true;
            }
            if (param.equals("recheck_interval")) {
                this.recheckInterval = this.crtRecheckInterval = Long.parseLong(value);
                found = true;
            }
            if (param.equals("maxMsgRate")) {
                this.maxMsgRate = Integer.parseInt(value);
                found = true;
            }
            if (param.equals("auto_disable")) {
                this.autoDisableMonitoring = flag;
                found = true;
            }
        }
        if (found) return;
        synchronized (mutexBack) {
            found = false;
            Long val = null;
            if (param.startsWith("sys")) {
                val = ApMonMonitoringConstants.getSysIdx(param);
                long lval = val.longValue();
                if (flag) {
                    sysMonitorParams |= lval;
                } else {
                    sysMonitorParams &= ~lval;
                }
            } else if (param.startsWith("job")) {
                val = ApMonMonitoringConstants.getJobIdx(param);
                long lval = val.longValue();
                if (flag) {
                    jobMonitorParams |= lval;
                } else {
                    jobMonitorParams &= ~lval;
                }
            }
            if (val == null) {
                logger.warning("Invalid parameter name in the configuration file: " + param);
            } else {
                found = true;
            }
        }
        if (!found) logger.warning("Invalid parameter name in the configuration file: " + param);
    }

    void setSenderRef(Hashtable s) {
        sender = s;
    }

    void initSenderRef() {
        sender.put("SEQ_NR", new Integer(0));
        sender.put("INSTANCE_ID", new Integer((new Random()).nextInt(0x7FFFFFFF)));
    }

    void updateSEQ_NR() {
        Integer seq_nr = (Integer) sender.get("SEQ_NR");
        sender.put("SEQ_NR", new Integer((seq_nr.intValue() + 1) % 2000000000));
    }

    /**
     * Displays the names, values and types of a set of parameters.
     * 
     * @param paramNames
     *            Vector with the parameters' names.
     * @param valueTypes
     *            Vector of Integers which represent the value types of the parameters.
     * @param paramValues
     *            Vector with the values of the parameters.
     */
    String printParameters(Vector paramNames, Vector paramValues) {
        int i;
        StringBuffer res = new StringBuffer();
        for (i = 0; i < paramNames.size(); i++) {
            String name = (String) paramNames.get(i);
            res.append(name).append(paramValues.get(i));
        }
        return res.toString();
    }

    /** don't allow a user to send more than MAX_MSG messages per second, in average */
    protected long prvTime = 0;

    protected double prvSent = 0;

    protected double prvDrop = 0;

    protected long crtTime = 0;

    protected long crtSent = 0;

    protected long crtDrop = 0;

    protected double hWeight = Math.exp(-5.0 / 60.0);

    /**
     * Decide if the current datagram should be sent. This decision is based on the number of messages previously sent.
     */
    public boolean shouldSend() {
        long now = (new Date()).getTime() / 1000;
        boolean doSend;
        if (now != crtTime) {
            prvSent = hWeight * prvSent + (1.0 - hWeight) * crtSent / (now - crtTime);
            prvTime = crtTime;
            logger.log(Level.FINE, "previously sent: " + crtSent + " dropped: " + crtDrop);
            crtTime = now;
            crtSent = 0;
            crtDrop = 0;
        }
        int valSent = (int) (prvSent * hWeight + crtSent * (1.0 - hWeight));
        doSend = true;
        int level = this.maxMsgRate - this.maxMsgRate / 10;
        if (valSent > (this.maxMsgRate - level)) doSend = (new Random()).nextInt(this.maxMsgRate / 10) < (this.maxMsgRate - valSent);
        if (doSend) {
            crtSent++;
        } else {
            crtDrop++;
        }
        return doSend;
    }

    public String getMyHostname() {
        return myHostname;
    }

    public static int getPID() {
        try {
            final java.lang.management.RuntimeMXBean rt = java.lang.management.ManagementFactory.getRuntimeMXBean();
            return Integer.parseInt(rt.getName().split("@")[0]);
        } catch (Throwable t) {
            return -1;
        }
    }
}
