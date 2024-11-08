package edu.sdsc.cleos;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.nees.daq.ControlPort;
import edu.sdsc.cleos.DaqListener;
import edu.sdsc.cleos.DataThread;
import edu.sdsc.cleos.ISOtoRbnbTime;
import edu.sdsc.cleos.RBNBBase;
import java.io.IOException;
import java.net.UnknownHostException;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import com.rbnb.sapi.*;

public class DaqToRbnb extends RBNBBase {

    private ControlPort controlPort = null;

    private DataThread dataThread = null;

    private DaqListener listener = null;

    private static final String DEFAULT_DAQ_SERVER = "localhost";

    private static final int DEFAULT_DAQ_CONTROL_PORT = 55055;

    private static final int DEFAULT_DAQ_DATA_PORT = 55056;

    private static final String DEFAULT_RBNB_SOURCE_NAME = DEFAULT_RBNB_CLIENT_NAME;

    private String daqServerName = DEFAULT_DAQ_SERVER;

    private int daqControlPort = DEFAULT_DAQ_CONTROL_PORT;

    private int daqDataPort = DEFAULT_DAQ_DATA_PORT;

    private static final int DEFAULT_CACHE_SIZE = 900;

    private int cacheSize = DEFAULT_CACHE_SIZE;

    private static final int DEFAULT_ARCHIVE_SIZE = 0;

    private int archiveSize = DEFAULT_ARCHIVE_SIZE;

    private static final boolean USE_TIME = true;

    private static final long GROUPING_TIME = 100;

    private static final long GROUPING_COUNT = 100;

    /** LJM 060519
    * variable to hold the time (in hours) desired for the length of the ring buffer
    * user to calculate cache and archive.
    */
    private double rbTime = -1.0;

    /** a variable to set what percentage of the archived frames are to be
    * cached by the rbnb server.
    */
    private static final double DEFAULT_CACHE_PERCENT = 10;

    private double rbCachePercent = DEFAULT_CACHE_PERCENT;

    private boolean useTime = USE_TIME;

    private long groupingTime = GROUPING_TIME;

    private long groupingCount = GROUPING_COUNT;

    private Socket controlSocket = null;

    private Socket dataSocket = null;

    private CleosSource source;

    boolean connected = false;

    Thread mainThread;

    private double timeOffset = 0.0;

    public static void main(String[] args) {
        DaqToRbnb control = new DaqToRbnb();
        if (control.parseArgs(args)) {
            control.startThread();
        }
    }

    protected String getCVSVersionString() {
        return ("$LastChangedDate: 2008-04-15 20:12:19 -0400 (Tue, 15 Apr 2008) $\n" + "$LastChangedRevision: 36 $" + "$LastChangedBy: ljmiller.ucsd $" + "$HeadURL: http://oss-dataturbine.googlecode.com/svn/trunk/apps/oss-apps/src/edu/sdsc/cleos/DaqToRbnb.java $");
    }

    protected Options setOptions() {
        Options opt = setBaseOptions(new Options());
        opt.addOption("q", true, "DAQ Server *" + DEFAULT_DAQ_SERVER);
        opt.addOption("c", true, " DAQ Control Port *" + DEFAULT_DAQ_CONTROL_PORT);
        opt.addOption("d", true, " DAQ Data Port *" + DEFAULT_DAQ_DATA_PORT);
        opt.addOption("n", true, " RBNB Source Name *" + DEFAULT_RBNB_SOURCE_NAME);
        opt.addOption("z", true, " cache size *" + DEFAULT_CACHE_SIZE);
        opt.addOption("Z", true, " archive size *" + DEFAULT_ARCHIVE_SIZE);
        opt.addOption("t", true, "milliseconds; amount of time to group records" + "defaults to " + GROUPING_TIME + " milliseconds");
        opt.addOption("m", true, "percentage (%) of the ring buffer specified in -r to cache in memory *" + DEFAULT_CACHE_PERCENT);
        double hours = timeOffset / (60.0 * 60.0);
        opt.addOption("o", true, " time offset, floating point, hours to GMT *" + hours);
        opt.addOption("D", false, " flag to print debug trace of data time stamps");
        opt.addOption("T", false, "flag; use time to group records (default)");
        opt.addOption("K", false, "flag; use count to group records (default is to use Time)");
        opt.addOption("k", true, "number; number of records to group when using count" + "defaults to " + GROUPING_COUNT);
        return opt;
    }

    protected boolean setArgs(CommandLine cmd) {
        if (!setBaseArgs(cmd)) return false;
        if (cmd.hasOption('q')) {
            String a = cmd.getOptionValue('q');
            if (a != null) daqServerName = a;
        }
        if (cmd.hasOption('c')) {
            String a = cmd.getOptionValue('c');
            if (a != null) {
                try {
                    daqControlPort = Integer.parseInt(a);
                } catch (NumberFormatException nf) {
                    System.out.println("Error: enter a numeric value for -c (control port). " + a + " is not valid!");
                    return false;
                }
            }
        }
        if (cmd.hasOption('d')) {
            String a = cmd.getOptionValue('d');
            if (a != null) {
                try {
                    daqDataPort = Integer.parseInt(a);
                } catch (NumberFormatException nf) {
                    System.out.println("Error: enter a numeric value for -d (data port). " + a + " is not valid!");
                    return false;
                }
            }
        }
        if (cmd.hasOption('n')) {
            String a = cmd.getOptionValue('n');
            if (a != null) {
                rbnbClientName = a;
            }
        }
        if (cmd.hasOption('z')) {
            String a = cmd.getOptionValue('z');
            if (a != null) try {
                Integer i = new Integer(a);
                int value = i.intValue();
                cacheSize = value;
            } catch (Exception e) {
                System.out.println("Error: enter a numeric value for -z option. " + a + " is not valid!");
                return false;
            }
        }
        if (cmd.hasOption('Z')) {
            String a = cmd.getOptionValue('Z');
            if (a != null) try {
                Integer i = new Integer(a);
                int value = i.intValue();
                archiveSize = value;
            } catch (Exception e) {
                System.out.println("Error: enter a numeric value for -Z option. " + a + " is not valid!");
                return false;
            }
        }
        if (cmd.hasOption('t')) {
            String a = cmd.getOptionValue('t');
            if (a != null) {
                try {
                    Long l = new Long(a);
                    long value = l.longValue();
                    groupingTime = value;
                } catch (NumberFormatException nf) {
                    System.out.println("Error:  enter a numeric value for -t option. " + a + " is not valid!");
                    return false;
                }
            }
        }
        if (cmd.hasOption('m')) {
            if (!cmd.hasOption('r')) {
                System.out.println("Error: parameter -m is only used by this program in " + "conjunction with the -r parameter");
                return false;
            } else {
                String a = cmd.getOptionValue('m');
                if (a != null) {
                    try {
                        double value = Double.parseDouble(a);
                        rbCachePercent = value;
                    } catch (NumberFormatException nf) {
                        System.out.println("Error:  enter a numeric value for -m option. " + a + " is not valid!");
                        return false;
                    }
                }
            }
        }
        if (cmd.hasOption('o')) {
            String a = cmd.getOptionValue('o');
            if (a != null) try {
                double value = Double.parseDouble(a);
                timeOffset = (long) (value * 60.0 * 60.0);
            } catch (NumberFormatException nf) {
                System.out.println("Error: enter a numeric value for -o option. " + a + " is not valid!");
                return false;
            }
        }
        if (cmd.hasOption('D')) {
            ISOtoRbnbTime.DEBUG = true;
        }
        if (cmd.hasOption('T')) {
            useTime = true;
        }
        if (cmd.hasOption('K')) {
            useTime = false;
        }
        if (cmd.hasOption('k')) {
            String a = cmd.getOptionValue('k');
            if (a != null) {
                try {
                    Long l = new Long(a);
                    long value = l.longValue();
                    groupingCount = value;
                } catch (NumberFormatException nf) {
                    System.out.println("Error: enter a numeric value for -k option. " + a + " is not valid!");
                    return false;
                }
            }
        }
        if ((archiveSize > 0) && (archiveSize < cacheSize)) {
            System.out.println("Error: non-zero archiveSize = " + archiveSize + " must be greater then " + "or equal to cacheSize = " + cacheSize);
            return false;
        }
        System.out.println("Arguments to DaqToRbnb...");
        System.out.println("  DAQ: server = " + daqServerName + "; control port = " + daqControlPort + "; data port = " + daqDataPort);
        System.out.println("  RBNB: server = " + getServer() + "; source name = " + getRBNBClientName());
        System.out.println("  Time offset (in seconds) = " + timeOffset + ", which is " + timeOffset / (60.0 * 60.0) + " hours");
        if (useTime) {
            System.out.println("  Records from the DAQ will be flushed to RBNB" + " every " + groupingTime + " milliseconds.");
        } else {
            System.out.println("  Records from the DAQ will be flushed to RBNB" + " every " + groupingCount + " records.");
        }
        System.out.println("   The DataTurbine ring buffer will be created " + "with " + archiveSize + "frames and with a cache size of " + cacheSize + "frames");
        System.out.println("  Use DaqToRbnb -h to see optional parameters");
        return true;
    }

    public DaqToRbnb() {
        computeDefaultTimeOffset();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                try {
                    if (connected) {
                        disconnect();
                    }
                } catch (Exception e) {
                    System.out.println("Unexpected Error! closing " + DaqToRbnb.class.getName());
                }
            }
        });
    }

    private void computeDefaultTimeOffset() {
        Calendar calendar = new GregorianCalendar();
        long tz = calendar.get(Calendar.ZONE_OFFSET);
        long dt = calendar.get(Calendar.DST_OFFSET);
        System.out.println("Default time: Time Zone offset: " + (-((double) (tz / 1000)) / (60.0 * 60.0)));
        System.out.println("Default time: Daylight Savings Time offset (in hours): " + (-((double) (dt / 1000)) / (60.0 * 60.0)));
        timeOffset = -(double) ((tz + dt) / 1000);
    }

    private void connect() {
        try {
            System.out.println("- Connecting to DAQ:");
            startDaqConnections();
            connectToDaqControl();
            connected = true;
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void startDaqConnections() throws UnknownHostException, IOException {
        System.out.println("  Opening Control Socket - " + daqServerName + ":" + daqControlPort);
        controlSocket = new Socket(daqServerName, daqControlPort);
        try {
            Thread.sleep(5000);
        } catch (Exception ignore) {
        }
        System.out.println("  Opening Data Socket - " + daqServerName + ":" + daqDataPort);
        dataSocket = new Socket(daqServerName, daqDataPort);
    }

    private void connectToDaqControl() throws UnknownHostException, IOException {
        controlPort = null;
        ControlPort port = null;
        port = new ControlPort(controlSocket);
        try {
            Thread.sleep(1000);
        } catch (Exception ignore) {
        }
        controlPort = port;
        System.out.println("  ControlPort connected.");
    }

    private void disconnect() {
        if (source == null) return;
        try {
            System.out.println(new java.util.Date() + " - Disconnecting:");
            System.out.println("  Closing Sockets...");
            controlSocket.close();
            dataSocket.close();
            System.out.println("  Closing RBNB connection...");
            if (archiveSize > 0) {
                source.Detach();
            } else source.CloseRBNBConnection();
            source = null;
            connected = false;
            System.out.println(new java.util.Date() + " - Disconnected successfuly.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void postMetadata(String[] channelList, String[] unitList) {
        int length = channelList.length;
        if (length > unitList.length) length = unitList.length;
        if (length > 0) {
            int index[] = new int[length];
            ChannelMap cm = new ChannelMap();
            for (int i = 0; i < length; i++) {
                try {
                    index[i] = cm.Add(channelList[i]);
                } catch (Exception e) {
                    System.out.println("Error: failed to set channel for metadata: " + channelList[i]);
                }
            }
            for (int i = 0; i < length; i++) {
                try {
                    cm.PutUserInfo(index[i], "units=" + unitList[i]);
                } catch (Exception e) {
                    System.out.println("Error: failed to put metadata for channel: " + channelList[i]);
                }
            }
            try {
                source.Register(cm);
            } catch (Exception e) {
                System.out.println("Error: failed to register units metadata");
            }
        }
    }

    private void exec() throws SAPIException, IOException {
        if (controlPort == null) return;
        dataThread = new DataThread(dataSocket);
        if (archiveSize > 0) {
            source = new CleosSource(cacheSize, "append", archiveSize);
        } else {
            source = new CleosSource(cacheSize, "none", 0);
        }
        source.OpenRBNBConnection(getServer(), getRBNBClientName());
        System.out.println("Set up connection to RBNB on " + getServer() + " as source = " + getRBNBClientName());
        System.out.println("with RBNB Cache Size = " + cacheSize + "and RBNB Archive Size = " + archiveSize);
        listener = new MyListener();
        dataThread.addListener(listener);
        setupChannels();
        dataThread.start();
    }

    /**
	 * Get the list of channels from the DAQ, register them on the RBNB server,
	 * and subscribe to them to receive their data. This method will also register
	 * any channel metadata provided by the DAQ.  
	 * 
	 * @throws IOException    if there is an error retrieving the channels and
	 *                        their metadata from the DAQ
	 * @throws SAPIException  if there is an error registering the metadata with
	 *                        the RBNB server
	 */
    private void setupChannels() throws IOException, SAPIException {
        String[] channelList = controlPort.getChannels();
        String[] unitList = controlPort.getUnits();
        System.out.println("Preparing to listen to DAQ Channels: " + channelList[0]);
        for (int i = 1; i < channelList.length; i++) System.out.print(", " + channelList[i]);
        System.out.println(".");
        System.out.println("With corresponding units: " + unitList[0]);
        for (int i = 1; i < unitList.length; i++) System.out.print(", " + unitList[i]);
        System.out.println(".");
        if (unitList.length == channelList.length) postMetadata(channelList, unitList); else System.out.println("Warning, channel list and unit list are of " + "different lengths: units not posted!");
        for (int i = 0; i < channelList.length; i++) listener.registerChannel(channelList[i]);
    }

    private class MyListener implements DaqListener {

        ChannelMap map;

        Hashtable channelIndex = new Hashtable();

        private double lastTimePosted = 0;

        private double lastTimeFlushed = 0;

        private long recordCount = -1;

        MyListener() {
            map = new ChannelMap();
        }

        public void postData(String channel, double data) throws SAPIException {
            Integer itsIndex = (Integer) channelIndex.get(channel);
            if (itsIndex == null) {
                System.out.println("Error: Unexpected null channel index in postData!");
                return;
            }
            int index = itsIndex.intValue();
            double dataArray[] = new double[1];
            dataArray[0] = data;
            map.PutDataAsFloat64(index, dataArray);
        }

        public void postEvent(String type, String content) throws SAPIException {
        }

        public void postString(String channel, String stringData) throws SAPIException {
            Integer itsIndex = (Integer) channelIndex.get(channel);
            if (itsIndex == null) {
                System.out.println("Unexpected null channel index in postData!");
                return;
            }
            int index = itsIndex.intValue();
            map.PutDataAsString(index, stringData);
        }

        public void postTimestamp(double time) throws SAPIException {
            lastTimePosted = time;
            map.PutTime(time + timeOffset, 0.0);
        }

        public void endTick() throws SAPIException {
            recordCount++;
            if (useTime) {
                if ((lastTimePosted - lastTimeFlushed) >= (groupingTime / 1000.0)) {
                    source.Flush(map);
                    lastTimeFlushed = lastTimePosted;
                }
            } else {
                if (recordCount > groupingCount) {
                    source.Flush(map);
                    recordCount = 0;
                }
            }
        }

        public void registerChannel(String channelName) throws SAPIException {
            if (channelIndex.containsKey(channelName)) {
                return;
            }
            try {
                controlPort.subscribe(channelName);
            } catch (IOException e) {
                throw new SAPIException("Subscribe to channel failed: " + e);
            }
            int index = map.Add(channelName);
            channelIndex.put(channelName, new Integer(index));
            map.PutMime(index, "application/octet-stream");
        }
    }

    public void startThread() {
        Runnable r = new Runnable() {

            public void run() {
                runWork();
            }
        };
        mainThread = new Thread(r, "DaqToRbnb");
        mainThread.start();
        System.out.println("DaqToRbnb: Started main thread.");
    }

    public void stopThread() {
        if (!connected) return;
        mainThread.interrupt();
        System.out.println("DaqToRbnb: Stopped thread.");
    }

    /**
	 * 
	 */
    private void runWork() {
        while (true) {
            connect();
            if (connected) {
                try {
                    exec();
                    while (isRunning()) {
                        try {
                            Thread.sleep(5 * 1000);
                        } catch (Exception ignore) {
                        }
                    }
                } catch (Throwable t) {
                }
                disconnect();
                if (dataThread.isRunning()) {
                    try {
                        dataThread.removeListener(listener);
                        dataThread.stop();
                    } catch (Throwable ignore) {
                    }
                }
                dataThread = null;
            }
            try {
                Thread.sleep(10 * 1000);
            } catch (Exception ignore) {
            }
        }
    }

    public boolean isRunning() {
        return (connected && dataThread.isRunning());
    }

    public boolean start() {
        if (isRunning()) return false;
        if (connected) disconnect();
        startThread();
        return true;
    }

    public boolean stop() {
        if (!isRunning()) return false;
        stopThread();
        disconnect();
        return true;
    }
}
