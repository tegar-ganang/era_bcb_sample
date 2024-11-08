import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import com.kni.etl.EngineConstants;
import com.kni.etl.Metadata;
import com.kni.etl.dbutils.ResourcePool;
import com.kni.etl.ketl.KETLCluster.Server;
import com.kni.etl.util.XMLHelper;
import com.kni.util.ExternalJarLoader;

/**
 * The Class ExecuteJob.
 */
class ETLDaemonMonitor {

    /**
	 * ExecuteJob constructor comment.
	 */
    public ETLDaemonMonitor() {
        super();
    }

    /**
	 * Extract arguments.
	 * 
	 * @param pArg
	 *            the arg
	 * @param pVarName
	 *            the var name
	 * 
	 * @return the string
	 */
    public static String extractArguments(String pArg, String pVarName) {
        String result = null;
        int argPos = -1;
        argPos = pArg.indexOf(pVarName);
        if (argPos != -1) {
            String fields = pArg.substring(pVarName.length());
            if (fields.length() > 0) {
                result = fields;
            }
        }
        return (result);
    }

    /**
	 * Extract multiple arguments.
	 * 
	 * @param pArg
	 *            the arg
	 * @param pVarName
	 *            the var name
	 * 
	 * @return the string[]
	 */
    public static String[] extractMultipleArguments(String pArg, String pVarName) {
        String[] result = null;
        int argPos = -1;
        argPos = pArg.indexOf(pVarName);
        if (argPos != -1) {
            String fields = pArg.substring(pVarName.length(), pArg.indexOf(")", pVarName.length()));
            if (fields.indexOf(',') != -1) {
                StringTokenizer st = new StringTokenizer(fields, ",");
                int nFields = st.countTokens();
                result = new String[nFields];
                int pos = 0;
                while (st.hasMoreTokens()) {
                    result[pos] = st.nextToken();
                    pos++;
                }
            } else if (fields.length() > 0) {
                result = new String[1];
                result[0] = fields;
            }
        }
        return (result);
    }

    /**
	 * Connect to server.
	 * 
	 * @param xmlConfig
	 *            the xml config
	 * @param pServerName
	 *            the server name
	 * 
	 * @return the metadata
	 * 
	 * @throws Exception
	 *             the exception
	 */
    private static Metadata connectToServer(Document xmlConfig, String pServerName) throws Exception {
        Node nCurrentServer;
        String password;
        String url;
        String driver;
        String mdprefix;
        String username;
        Metadata md = null;
        nCurrentServer = XMLHelper.findElementByName(xmlConfig, "SERVER", "NAME", pServerName);
        if (nCurrentServer == null) {
            throw new Exception("ERROR: Server " + pServerName + " not found!");
        }
        username = XMLHelper.getChildNodeValueAsString(nCurrentServer, "USERNAME", null, null, null);
        password = XMLHelper.getChildNodeValueAsString(nCurrentServer, "PASSWORD", null, null, null);
        url = XMLHelper.getChildNodeValueAsString(nCurrentServer, "URL", null, null, null);
        driver = XMLHelper.getChildNodeValueAsString(nCurrentServer, "DRIVER", null, null, null);
        mdprefix = XMLHelper.getChildNodeValueAsString(nCurrentServer, "MDPREFIX", null, null, null);
        String passphrase = XMLHelper.getChildNodeValueAsString(nCurrentServer, "PASSPHRASE", null, null, null);
        try {
            Metadata mds = new Metadata(true, passphrase);
            mds.setRepository(username, password, url, driver, mdprefix);
            pServerName = XMLHelper.getAttributeAsString(nCurrentServer.getAttributes(), "NAME", pServerName);
            ResourcePool.setMetadata(mds);
            md = ResourcePool.getMetadata();
        } catch (Exception e1) {
            throw new Exception("ERROR: Connecting to metadata - " + e1.getMessage());
        }
        return md;
    }

    private static enum EXIT_CODES {

        SUCCESS, INVALIDSTATE, INVALIDARGUMENTS, METADATA_ERROR
    }

    ;

    private static RandomAccessFile lckFile;

    private static FileChannel channel;

    private static FileLock exLck;

    private static final String MONITOR_LOCK = "ketlMonitor.lck";

    public static boolean lockMonitorInstance() {
        try {
            if (lckFile == null) {
                lckFile = new RandomAccessFile(new File(MONITOR_LOCK), "rw");
            }
            channel = lckFile.getChannel();
            exLck = channel.tryLock(1, 1, false);
            if (exLck != null) {
                return true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
        ResourcePool.logError("A " + MONITOR_LOCK + " file exists! A monitor maybe already running.");
        if (exLck != null) {
            try {
                exLck.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (lckFile != null) {
            try {
                lckFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            lckFile = null;
        }
        System.exit(EXIT_CODES.INVALIDSTATE.ordinal());
        return false;
    }

    /**
	 * Starts the application.
	 * 
	 * @param args
	 *            an array of command-line arguments
	 */
    public static void main(java.lang.String[] args) {
        String ketldir = System.getenv("KETLDIR");
        if (ketldir == null) {
            ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.WARNING_MESSAGE, "KETLDIR not set, defaulting to working dir");
            ketldir = ".";
        }
        if (lockMonitorInstance() == false) ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.WARNING_MESSAGE, "A monitor lock could not be assigned, duplicate monitors exist, if you want to restart the monitor, identify the process and kill it");
        ExternalJarLoader.loadJars(new File(ketldir + File.separator + "conf" + File.separator + "Extra.Libraries"), "ketlextralibs", ";");
        String configFile = null;
        for (String element : args) {
            if ((configFile == null) && (element.indexOf("CONFIGFILE=") != -1)) {
                configFile = ETLDaemonMonitor.extractArguments(element, "CONFIGFILE=");
            }
        }
        if (configFile == null) configFile = Metadata.getKETLPath() + File.separator + "xml" + File.separator + "MonitorConfig.xml";
        String server = null, fromAddress = null, toAddress = null, mailHost = null;
        Integer pollTime = null, notifyTime = null;
        try {
            Document config = XMLHelper.readXMLFromFile(configFile);
            Node cluster = XMLHelper.findElementByName(config, "CLUSTER", null, null);
            server = XMLHelper.getAttributeAsString(cluster.getAttributes(), "NAME", null);
            pollTime = Integer.parseInt(XMLHelper.getChildNodeValueAsString(cluster, "PING", null, null, "60"));
            notifyTime = Integer.parseInt(XMLHelper.getChildNodeValueAsString(cluster, "NOTIFYTIME", null, null, "300"));
            Node failEmail = XMLHelper.getElementByName(cluster, "FAILEMAIL", null, null);
            if (failEmail != null) {
                fromAddress = XMLHelper.getChildNodeValueAsString(failEmail, "FROMADDRESS", null, null, null);
                toAddress = XMLHelper.getChildNodeValueAsString(failEmail, "TOADDRESS", null, null, null);
                mailHost = XMLHelper.getChildNodeValueAsString(failEmail, "MAILHOST", null, null, null);
            }
        } catch (Exception e1) {
            ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.ERROR_MESSAGE, "Loading Monitors.xml - " + e1.getMessage());
            System.exit(EXIT_CODES.METADATA_ERROR.ordinal());
        }
        if (pollTime == null) pollTime = 60;
        if (notifyTime == null) notifyTime = 300;
        if (server == null) {
            System.out.println("Wrong arguments:  CLUSTER=<KETL_MD_NAME> {POLLTIME=<SECONDS>} {NOTIFYTIME=<SECONDS>} {FAILEMAIL=[MailHost,FromAddress,ToAddress]}");
            System.out.println("example:  CLUSTER=TEST");
            System.exit(EXIT_CODES.INVALIDARGUMENTS.ordinal());
        }
        Metadata md = null;
        InetAddress thisIp;
        try {
            md = ETLDaemonMonitor.connectToServer(Metadata.LoadConfigFile(null, Metadata.CONFIG_FILE), server);
        } catch (Exception e1) {
            ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.ERROR_MESSAGE, "Connecting to metadata - " + e1.getMessage());
            if (mailHost != null) {
                try {
                    Metadata.sendEmailDirect(fromAddress, toAddress, mailHost, "Monitor Cannot Connect To " + server + " Metadata", "The monitor for cluster " + server + " failed to connect to the metadata, the cluster is probably in an offline state", 16384);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.exit(EXIT_CODES.METADATA_ERROR.ordinal());
            }
        }
        try {
            String monitorName;
            thisIp = InetAddress.getLocalHost();
            monitorName = thisIp.getHostName();
            Set<Integer> warningSent = new HashSet<Integer>();
            while (true) {
                try {
                    for (Server s : md.getAliveServers()) {
                        long diff = (s.mSystemTime.getTime() - s.mLastPing.getTime()) / 1000;
                        if (diff >= notifyTime) {
                            if (warningSent.contains(s.mServerID) == false) {
                                md.sendEmailToAll("KETL Server " + s.mName + " Offline", "Monitor " + monitorName + " has detected that KETL server " + s.mName + " appears to be offline and has not pinged the metadata in " + diff + " seconds.");
                                warningSent.add(s.mServerID);
                            }
                        } else {
                            if (warningSent.remove(s.mServerID)) {
                                md.sendEmailToAll("KETL Server " + s.mName + " Recovered", "Monitor " + monitorName + " has detected that KETL server " + s.mName + " appears to be online again and pinged the metadata in last " + diff + " seconds.");
                            }
                        }
                        File f = new File(EngineConstants.MONITORPATH + File.separator + s.mName + ".monitor");
                        FileWriter fw = new FileWriter(f);
                        fw.append(Long.toString(diff));
                        fw.close();
                    }
                    Thread.sleep(pollTime * 1000);
                } catch (Exception e) {
                    if (mailHost != null) {
                        md.sendEmailDirect(fromAddress, toAddress, mailHost, "Monitor Cannot Connect To " + server + " Metadata", "The monitor for cluster " + server + " failed to connect to the metadata, the cluster is probably in an offline state", 16384);
                        Thread.sleep(600 * 1000);
                    }
                }
            }
        } catch (Exception e) {
            ResourcePool.logMessage(e);
        }
        ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.ERROR_MESSAGE, "Invalid state reached.");
        exit(md, EXIT_CODES.INVALIDSTATE.ordinal());
    }

    private static void exit(Metadata md, int exitCode) {
        if (md != null) md.closeMetadata();
        System.exit(exitCode);
    }
}
