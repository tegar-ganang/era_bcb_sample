package edu.ucsd.osdt.source.numeric;

import edu.ucsd.osdt.source.BaseSource;
import edu.ucsd.osdt.source.numeric.LoggerNetParser;
import edu.ucsd.osdt.util.ISOtoRbnbTime;
import edu.ucsd.osdt.util.RBNBBase;
import edu.ucsd.osdt.util.MDParserInterface;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

class SdlParser extends RBNBBase implements MDParserInterface {

    private String DEFAULT_SDL_FILE_NAME = "C:\\Program Files\\CUAHSI HIS\\ODM SDL\\Config.xml";

    private String sdlFileName = DEFAULT_SDL_FILE_NAME;

    private String DEFAULT_LN_FILE_NAME = "loggernet.dat";

    private String loggernetFileName = DEFAULT_LN_FILE_NAME;

    private BufferedReader loggernetFileBuffer = null;

    private LoggerNetParser loggernetParser = null;

    private String[] loggernetChannels = null;

    private String[] loggernetUnits = null;

    private static Logger logger = Logger.getLogger(SdlParser.class.getName());

    private ArrayList<Integer> sdlMappedColumns = null;

    public SdlParser() {
        super(new BaseSource(), null);
        sdlMappedColumns = new ArrayList();
        loggernetParser = new LoggerNetParser();
    }

    public String getSdlFileName() {
        return this.sdlFileName;
    }

    public void initRbnb() throws SAPIException, IOException {
        if (0 < rbnbArchiveSize) {
            myBaseSource = new BaseSource(rbnbCacheSize, "append", rbnbArchiveSize);
        } else {
            myBaseSource = new BaseSource(rbnbCacheSize, "none", 0);
        }
        myBaseSource.OpenRBNBConnection(serverName, rbnbClientName);
        logger.config("Set up connection to RBNB on " + serverName + " as source = " + rbnbClientName);
        logger.config(" with RBNB Cache Size = " + rbnbCacheSize + " and RBNB Archive Size = " + rbnbArchiveSize);
        this.cmap = generateCmap();
        myBaseSource.Register(this.cmap);
        myBaseSource.Flush(this.cmap);
        logger.info("registered and flushed: " + this.cmap.toString());
    }

    protected void closeRbnb() {
        if (myBaseSource == null) {
            return;
        }
        if (rbnbArchiveSize > 0) {
            myBaseSource.Detach();
        } else {
            myBaseSource.CloseRBNBConnection();
        }
        logger.config("Closed RBNB connection");
    }

    public ChannelMap getCmap() {
        return cmap;
    }

    public String[] getChannels() {
        return loggernetChannels;
    }

    public String[] getUnits() {
        return loggernetUnits;
    }

    public ChannelMap generateCmap() throws IOException, SAPIException {
        ChannelMap cmapRetval = new ChannelMap();
        StringBuffer mdBuffer = new StringBuffer();
        loggernetFileBuffer.readLine();
        String fileLine1 = loggernetFileBuffer.readLine();
        mdBuffer.append(fileLine1);
        logger.finer("file line 1: " + fileLine1);
        mdBuffer.append("\n");
        String fileLine2 = loggernetFileBuffer.readLine();
        mdBuffer.append(fileLine2);
        logger.finer("file line 2: " + fileLine2);
        mdBuffer.append("\n");
        loggernetFileBuffer.readLine();
        loggernetParser.parse(mdBuffer.toString());
        loggernetChannels = (String[]) loggernetParser.get("channels");
        loggernetUnits = (String[]) loggernetParser.get("units");
        Object[] sdlColumns = sdlMappedColumns.toArray();
        for (int i = 0; i < sdlColumns.length; i++) {
            int sdlColumn = ((Integer) (sdlColumns[i])).intValue();
            logger.finer("sdl column: " + sdlColumn + " maps to loggernet channel: " + loggernetChannels[sdlColumn + 1]);
            logger.finer("make cmap channel \"" + loggernetChannels[sdlColumn + 1] + "\" with units: \"" + loggernetUnits[sdlColumn + 1] + "\"");
            cmapRetval.Add(loggernetChannels[sdlColumn + 1]);
            cmapRetval.PutMime(cmapRetval.GetIndex(loggernetChannels[sdlColumn + 1]), "application/octet-stream");
            cmapRetval.PutUserInfo(cmapRetval.GetIndex(loggernetChannels[sdlColumn + 1]), "units=" + loggernetUnits[sdlColumn + 1]);
        }
        return cmapRetval;
    }

    public boolean parse(String mdFromSdl) {
        try {
            Document document;
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = documentBuilder.parse(sdlFileName);
            XPath xp = XPathFactory.newInstance().newXPath();
            Node node = (Node) xp.evaluate("/Config/File[1]", document, XPathConstants.NODE);
            NodeList firstLevelList = node.getChildNodes();
            logger.finer("first level # of nodes: " + firstLevelList.getLength());
            for (int i = 0; i < firstLevelList.getLength(); i++) {
                Node l1Node = firstLevelList.item(i);
                if (l1Node.getNodeName().compareTo("DataSeriesMapping") == 0) {
                    logger.finer("got a data series");
                    NodeList secondLevelList = l1Node.getChildNodes();
                    for (int j = 0; j < secondLevelList.getLength(); j++) {
                        Node l2Node = secondLevelList.item(j);
                        if (l2Node.getNodeName().compareTo("ValueColumnName") == 0) {
                            String columnLabel = l2Node.getChildNodes().item(0).getNodeValue();
                            logger.finer("got a value column: " + columnLabel);
                            sdlMappedColumns.add(new Integer(getColumnNumber(columnLabel)));
                        }
                    }
                }
            }
            logger.finer("# of elements in arraylist: " + sdlMappedColumns.size());
        } catch (Exception e) {
            logger.severe("sumpin happened: " + e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected int getColumnNumber(String columnLabelString) {
        Pattern pattern = Pattern.compile("Column(\\d)?", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(columnLabelString);
        matcher.find();
        int columnNumber = Integer.parseInt(matcher.group(1));
        logger.finer("got column number: " + Integer.toString(columnNumber));
        return columnNumber;
    }

    protected void postBogus() throws SAPIException {
        String[] cmapChannels = cmap.GetChannelList();
        for (int i = 0; i < cmapChannels.length; i++) {
            double[] doubleArray = new double[1];
            doubleArray[0] = (double) i;
            cmap.PutTimeAuto("timeofday");
            cmap.PutDataAsFloat64(i, doubleArray);
            myBaseSource.Flush(cmap);
            logger.info("flushed: " + doubleArray[0] + " to channel: " + cmap.GetName(i));
        }
    }

    public double getRbnbTimestamp(String instrTimestamp) {
        return -1;
    }

    public void initFile() throws FileNotFoundException {
        loggernetFileBuffer = new BufferedReader(new FileReader(loggernetFileName));
    }

    protected Options setOptions() {
        Options opt = setBaseOptions(new Options());
        opt.addOption("d", true, "SDL configuration file name *" + DEFAULT_SDL_FILE_NAME);
        opt.addOption("f", true, "Input LoggerNet file name *" + DEFAULT_LN_FILE_NAME);
        opt.addOption("z", true, "DataTurbine cache size *" + DEFAULT_CACHE_SIZE);
        opt.addOption("Z", true, "Dataturbine archive size *" + DEFAULT_ARCHIVE_SIZE);
        return opt;
    }

    protected boolean setArgs(CommandLine cmd) throws IllegalArgumentException {
        if (!setBaseArgs(cmd)) return false;
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
        if (cmd.hasOption('d')) {
            String v = cmd.getOptionValue("d");
            sdlFileName = v;
        }
        if (cmd.hasOption('f')) {
            String v = cmd.getOptionValue("f");
            loggernetFileName = v;
        }
        return true;
    }

    /************************************************************/
    public static void main(String[] args) {
        SdlParser sparse = new SdlParser();
        ChannelMap sdlCmap = null;
        if (!sparse.parseArgs(args)) {
            logger.severe("Unable to process command line. Terminating.");
            System.exit(1);
        }
        try {
            sparse.initFile();
            sparse.parse(null);
            sparse.initRbnb();
            sdlCmap = sparse.getCmap();
            logger.info(sdlCmap.toString());
            sparse.postBogus();
            sparse.closeRbnb();
        } catch (FileNotFoundException fne) {
            logger.severe("couldn't find the specified loggernet file");
        } catch (IOException ioe) {
            logger.severe(ioe.toString());
        } catch (SAPIException sae) {
            logger.severe(sae.toString());
            sae.printStackTrace();
        }
    }
}
