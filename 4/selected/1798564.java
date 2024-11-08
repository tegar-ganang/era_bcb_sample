package edu.ucsd.osdt.source.numeric;

import edu.ucsd.osdt.source.BaseSource;
import edu.ucsd.osdt.source.numeric.SeabirdParser;
import edu.ucsd.osdt.util.RBNBBase;
import edu.ucsd.osdt.util.ISOtoRbnbTime;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Source;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.logging.Logger;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

class SeabirdSource extends RBNBBase {

    public static final String DEFAULT_SEABIRD_PORT = "COM1";

    private String seabirdPort = DEFAULT_SEABIRD_PORT;

    public static final int DEFAULT_SAMPLE_PERIOD = 5000;

    private int seabirdSamplePeriod = DEFAULT_SAMPLE_PERIOD;

    private SerialPort serialPort = null;

    private InputStream serialPortInputStream;

    private OutputStream serialPortOutputStream;

    private BufferedWriter writeToBird = null;

    private BufferedReader readFromBird = null;

    private SeabirdParser seabirdParser = null;

    private static final int DEFAULT_CACHE_SIZE = 900;

    private int rbnbCacheSize = DEFAULT_CACHE_SIZE;

    private static final int DEFAULT_ARCHIVE_SIZE = 0;

    private int rbnbArchiveSize = DEFAULT_ARCHIVE_SIZE;

    private ChannelMap cmap = null;

    private double timeOffset = 0.0;

    public static final String DEFAULT_FILE_NAME = "none";

    private String fileName = DEFAULT_FILE_NAME;

    boolean writeFile = false;

    DataOutputStream fileOut = null;

    public SeabirdSource() {
        super(new BaseSource(), null);
        logger = Logger.getLogger(SeabirdSource.class.getName());
        rbnbClientName = "Seabird";
        seabirdParser = new SeabirdParser();
        if (writeFile) {
            try {
                fileOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
                logger.config("Opened file: " + fileName);
            } catch (FileNotFoundException fe) {
                logger.severe("Could not open the output file: " + fileName + ": " + fe.toString());
            }
        }
        computeDefaultTimeOffset();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                logger.info("Shutdown hook activated for " + SeabirdSource.class.getName() + ". Exiting.");
                closeRbnb();
                try {
                    closeSerialPort();
                } catch (IOException ioe) {
                    logger.severe("Problem closing serial port.");
                    logger.severe(ioe.toString());
                }
                Runtime.getRuntime().halt(0);
            }
        });
    }

    public void initSerialPort(String portName) throws IOException {
        CommPortIdentifier portId = null;
        Enumeration portList = CommPortIdentifier.getPortIdentifiers();
        ;
        while (portList.hasMoreElements()) {
            portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                logger.fine("Found serial port:" + portId.getName());
                if (portId.getName().equals(portName)) {
                    try {
                        serialPort = (SerialPort) portId.open("Seabird->rxtx", 64);
                        serialPortInputStream = serialPort.getInputStream();
                        readFromBird = new BufferedReader(new InputStreamReader(serialPortInputStream));
                        serialPortOutputStream = serialPort.getOutputStream();
                        writeToBird = new BufferedWriter(new OutputStreamWriter(serialPortOutputStream));
                        serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                        logger.info("Initialized " + portId.getName() + " to 9600n81");
                        return;
                    } catch (Exception e) {
                        throw new IOException(e.toString());
                    }
                }
            }
        }
        throw new IOException("Requested port \"" + portName + "\" not found");
    }

    public void initCmap() throws SAPIException {
        cmap = new ChannelMap();
        String[] rbnbChannels = (String[]) seabirdParser.get("channels");
        String[] rbnbUnits = (String[]) seabirdParser.get("units");
        for (int i = 0; i < rbnbChannels.length; i++) {
            cmap.Add(rbnbChannels[i]);
            cmap.PutMime(cmap.GetIndex(rbnbChannels[i]), "application/octet-stream");
        }
        for (int i = 0; i < rbnbUnits.length; i++) {
            cmap.PutUserInfo(cmap.GetIndex(rbnbChannels[i]), "units=" + rbnbUnits[i]);
        }
        String[] metadataChannels = (String[]) seabirdParser.get("metadata-channels");
        for (int i = 0; i < metadataChannels.length; i++) {
            cmap.Add(metadataChannels[i]);
            cmap.PutMime(cmap.GetIndex(metadataChannels[i]), "text/plain");
        }
    }

    public void initRbnb() throws SAPIException {
        if (rbnbArchiveSize > 0) {
            myBaseSource = new BaseSource(rbnbCacheSize, "append", rbnbArchiveSize);
        } else {
            myBaseSource = new BaseSource(rbnbCacheSize, "none", 0);
        }
        this.initCmap();
        this.myBaseSource.OpenRBNBConnection(getServer(), getRBNBClientName());
        logger.config("Set up connection to RBNB on " + getServer() + " as source = " + getRBNBClientName());
        logger.config(" with RBNB Cache Size = " + rbnbCacheSize + " and RBNB Archive Size = " + rbnbArchiveSize);
        this.myBaseSource.Register(cmap);
        this.myBaseSource.Flush(cmap);
    }

    protected void closeSerialPort() throws IOException {
        if (serialPort != null) {
            serialPort.notifyOnDataAvailable(false);
            serialPort.removeEventListener();
            if (serialPortInputStream != null) {
                serialPortInputStream.close();
                serialPort.close();
            }
            serialPortInputStream = null;
        }
        if (serialPortOutputStream != null) {
            serialPortOutputStream.close();
            serialPortOutputStream = null;
        }
        if (writeToBird != null) {
            writeToBird.close();
        }
        if (readFromBird != null) {
            readFromBird.close();
        }
        writeToBird = null;
        readFromBird = null;
        serialPort = null;
        logger.config("Closed serial port");
    }

    protected void closeRbnb() {
        if (rbnbArchiveSize > 0) {
            this.myBaseSource.Detach();
        } else {
            this.myBaseSource.CloseRBNBConnection();
        }
        logger.config("Closed RBNB connection");
    }

    protected SeabirdParser getParser() {
        return this.seabirdParser;
    }

    protected String getSeabirdStatus() throws IOException {
        String cmd = "ds";
        writeToBird.write("\n\r");
        writeToBird.flush();
        logger.finest("Clear line:" + readFromBird.readLine());
        writeToBird.write(cmd, 0, cmd.length());
        writeToBird.write("\n\r");
        writeToBird.flush();
        logger.finest("Wrote command:" + cmd);
        StringBuffer readBuffer = new StringBuffer();
        String lineRead = null;
        String lastLineString = "output salinity = yes, output sound velocity = no";
        boolean lastLine = false;
        while (!lastLine && ((lineRead = readFromBird.readLine()) != null)) {
            readBuffer.append(lineRead);
            readBuffer.append("\n");
            lastLine = lineRead.matches(lastLineString) || lineRead.matches("S>time out");
        }
        if (writeFile) {
            fileOut.write(readBuffer.toString().getBytes());
        }
        return readBuffer.toString();
    }

    protected String getSeabirdCal() throws IOException {
        String cmd = "dcal";
        writeToBird.write("\n\r");
        writeToBird.flush();
        logger.finest("Clear line:" + readFromBird.readLine());
        writeToBird.write(cmd, 0, cmd.length());
        writeToBird.write("\n\r");
        writeToBird.flush();
        logger.finest("Wrote command:" + cmd);
        StringBuffer readBuffer = new StringBuffer();
        String lineRead = null;
        String lastLineString = "    EXTFREQSF = 9.999942e-01";
        boolean lastLine = false;
        while (!lastLine && ((lineRead = readFromBird.readLine()) != null)) {
            readBuffer.append(lineRead);
            readBuffer.append('\n');
            lastLine = (lineRead.compareTo(lastLineString) == 0) || (lineRead.compareTo("S>time out") == 0);
        }
        return readBuffer.toString();
    }

    public void seabird2RbnbPolling() throws IOException, SAPIException {
        String cmd = "ts";
        String echoCmd = "echo=no";
        writeToBird.write("\n\r");
        writeToBird.flush();
        String lineRead = null;
        do {
            mySleep(seabirdSamplePeriod);
            writeToBird.write(cmd, 0, cmd.length());
            writeToBird.write("\n\r");
            writeToBird.flush();
            mySleep(3000);
            if ((lineRead != null) && (lineRead.compareTo("S>ts") != 0) && (lineRead.compareTo("S>") != 0)) {
                try {
                    if (lineRead.startsWith("ts")) {
                        String[] readTmp = lineRead.split("ts ");
                        lineRead = readTmp[1];
                    }
                    logger.finer("Data line:" + lineRead);
                    double[] dataArray = seabirdParser.getData(lineRead);
                    postData(dataArray);
                } catch (ParseException pe) {
                    logger.fine("Parsing seabird sample string. " + pe.toString());
                } catch (SeabirdException se) {
                    logger.fine("Seabird hiccup. Skipping data point: " + lineRead);
                    logger.fine(se.toString());
                } catch (ArrayIndexOutOfBoundsException ae) {
                    logger.fine("Seabird hiccup. Skipping data point: " + lineRead);
                    logger.fine(ae.toString());
                }
            }
        } while ((readFromBird.ready() && (lineRead = readFromBird.readLine()) != null));
    }

    protected void seabird2RbnbAuto() {
    }

    protected void postData(double[] data) throws SAPIException {
        double adjustedTime = (data[data.length - 1] + timeOffset);
        logger.finer("Date adjusted for timezone:" + adjustedTime);
        logger.finer("Nice adjusted time:" + ISOtoRbnbTime.formatDate((long) adjustedTime * 1000));
        cmap.PutTime(adjustedTime, 0.0);
        if (seabirdParser.getChannels().length < data.length - 1) {
            logger.fine("data[] is of unexpected length: " + Integer.toString(data.length - 1) + " should be: " + seabirdParser.getChannels().length);
            return;
        }
        try {
            for (int i = 0; i < data.length - 1; i++) {
                double[] dataTmp = new double[1];
                dataTmp[0] = data[i];
                cmap.PutDataAsFloat64(cmap.GetIndex(seabirdParser.getChannels()[i]), dataTmp);
                logger.finer("Posted data:" + data[i] + " into channel:" + seabirdParser.getChannels()[i]);
            }
            String[] metadataChannels = (String[]) seabirdParser.get("metadata-channels");
            String model = (String) seabirdParser.get("model");
            String serial = (String) seabirdParser.get("serial");
            cmap.PutDataAsString(cmap.GetIndex(metadataChannels[0]), model);
            cmap.PutDataAsString(cmap.GetIndex(metadataChannels[1]), serial);
            logger.finer("Posted metadata:" + model + ":" + serial + " to channels:" + metadataChannels[0] + ":" + metadataChannels[1]);
            logger.finer("Posted data and metadata");
        } catch (SAPIException sae) {
            sae.printStackTrace();
            throw sae;
        }
        this.myBaseSource.Flush(cmap);
    }

    /*****************************************************************************/
    public static void main(String[] args) {
        SeabirdSource seabird = new SeabirdSource();
        if (!seabird.parseArgs(args)) {
            logger.severe("Unable to process command line. Terminating.");
            System.exit(1);
        }
        try {
            seabird.initSerialPort(seabird.seabirdPort);
            seabird.initRbnb();
        } catch (IOException ioe) {
            logger.severe("Unable to communicate with serial port. Terminating: " + ioe.toString());
            System.exit(2);
        } catch (SAPIException sae) {
            logger.severe("Unable to communicate with DataTurbine server. Terminating: " + sae.toString());
            System.exit(3);
        }
        try {
            logger.info("Polling seabird at a period of: " + seabird.seabirdSamplePeriod + " ms...");
            seabird.seabird2RbnbPolling();
        } catch (IOException ioe) {
            logger.severe("Unable to read serial port: " + ioe.toString());
            ioe.printStackTrace();
        } catch (SAPIException sae) {
            logger.severe("Unable to post data to RBNB: " + sae.toString());
        }
    }

    protected void computeDefaultTimeOffset() {
        Calendar calendar = new GregorianCalendar();
        long tz = calendar.get(Calendar.ZONE_OFFSET);
        long dt = calendar.get(Calendar.DST_OFFSET);
        logger.finer("Time Zone offset: " + (-((double) (tz / 1000)) / (60.0 * 60.0)));
        logger.finer("Daylight Savings Time offset (h): " + (-((double) (dt / 1000)) / (60.0 * 60.0)));
        timeOffset = -(double) ((tz + dt) / 1000);
        logger.finer("timeOffset: " + timeOffset);
    }

    protected Options setOptions() {
        Options opt = setBaseOptions(new Options());
        opt.addOption("P", true, "Serial port to read *" + DEFAULT_SEABIRD_PORT);
        opt.addOption("z", true, "DataTurbine cache size *" + DEFAULT_CACHE_SIZE);
        opt.addOption("Z", true, "Dataturbine archive size *" + DEFAULT_ARCHIVE_SIZE);
        opt.addOption("f", true, "Output file name *" + DEFAULT_FILE_NAME);
        opt.addOption("r", true, "Data sample polling rate (ms) *" + DEFAULT_SAMPLE_PERIOD);
        double hours = timeOffset / (60.0 * 60.0);
        opt.addOption("o", true, " time offset, floating point, hours to GMT *" + hours);
        return opt;
    }

    protected boolean setArgs(CommandLine cmd) throws IllegalArgumentException {
        if (!setBaseArgs(cmd)) return false;
        if (cmd.hasOption('P')) {
            String v = cmd.getOptionValue("P");
            seabirdPort = v;
        }
        if (cmd.hasOption('z')) {
            String a = cmd.getOptionValue('z');
            if (a != null) {
                try {
                    Integer i = new Integer(a);
                    int value = i.intValue();
                    rbnbCacheSize = value;
                } catch (Exception e) {
                    logger.severe("Enter a numeric value for -z option. " + a + " is not valid!");
                    return false;
                }
            }
        }
        if (cmd.hasOption('Z')) {
            String a = cmd.getOptionValue('Z');
            if (a != null) {
                try {
                    Integer i = new Integer(a);
                    int value = i.intValue();
                    rbnbArchiveSize = value;
                } catch (Exception e) {
                    logger.severe("Enter a numeric value for -Z option. " + a + " is not valid!");
                    return false;
                }
            }
        }
        if (cmd.hasOption('f')) {
            String v = cmd.getOptionValue("f");
            writeFile = true;
            fileName = v;
        }
        if (cmd.hasOption('r')) {
            String a = cmd.getOptionValue("r");
            if (a != null) {
                try {
                    Integer i = new Integer(a);
                    int value = i.intValue();
                    seabirdSamplePeriod = value;
                } catch (Exception e) {
                    logger.severe("Enter a numeric value for -r option. " + a + " is not valid!");
                    return false;
                }
            }
        }
        if (cmd.hasOption('o')) {
            String a = cmd.getOptionValue('o');
            if (a != null) try {
                double value = Double.parseDouble(a);
                timeOffset = (value * 60.0 * 60.0);
                logger.config("Timezone offset to: " + timeOffset / (60.0 * 60.0) + "h from GMT");
            } catch (NumberFormatException nf) {
                System.out.println("Error: enter a numeric value for -o option. " + a + " is not valid!");
                return false;
            }
        }
        return true;
    }

    protected void mySleep(int millis) {
        try {
            Thread.sleep(millis / 2);
        } catch (InterruptedException ie) {
            logger.severe("Thread sleep interrupted: " + ie.toString());
        }
    }

    protected class serialPortTimeoutTask extends TimerTask {

        protected long sleepTime;

        public serialPortTimeoutTask(long millis) {
            super();
            sleepTime = millis;
        }

        public void run() {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
                logger.severe("Thread sleep interrupted: " + ie.toString());
            }
        }
    }
}
