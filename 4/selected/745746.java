package edu.sdsc.rtdsm.dataint;

import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import com.rbnb.sapi.*;
import java.util.*;
import java.io.*;
import edu.sdsc.rtdsm.framework.sink.*;
import edu.sdsc.rtdsm.framework.util.*;
import edu.sdsc.rtdsm.framework.data.*;
import edu.sdsc.rtdsm.drivers.turbine.*;
import edu.sdsc.rtdsm.drivers.turbine.util.*;
import edu.sdsc.rtdsm.dig.sites.lake.*;
import edu.sdsc.rtdsm.dig.sites.*;
import edu.sdsc.rtdsm.dig.dsw.*;

/**
 * The class responsible for storing the configuration parameters
 * of the given sink. This class also has methods to parse the 
 * Lake configuration xml. The structure of the xml is given in
 * <tt>sinkConfig.dtd</tt>. An example sink configuration is given below
 * <br>
 *   <tt>
 *   <SinkConfig>
 *     <sink name="theSink">
 *       <mainChannels>
 *         <orbParams orbType="DataTurbine">
 *           <server uri="localhost:3333" username="me" password="mine" feedbackServer="localhost" feedbackPort="7878">
 *             <source name="12GREENSPN00000701400000B9F366A" feedbackReqd="yes"/>
 *           </server>
 *         </orbParams>
 *       </mainChannels>
 *     </sink>
 *   </SinkConfig>
 *   </tt>
 *   @author Vinay Kolar
 *   @version 1.0
 **/
public class LakeSinkConfig {

    public static final String URI_TAG = "uri";

    public static final String USERNAME_TAG = "username";

    public static final String PASSWORD_TAG = "password";

    public static final String NAME_TAG = "name";

    public static final String SRC_NAME_TAG = "source";

    public static final int DEFAULT_CACHE_SIZE = 100;

    public static final String DEFAULT_ARCHIVE_MODE = "yes";

    public static final int DEFAULT_ARCHIVE_SIZE = 300;

    public static final int MONITOR_MODE = 0;

    public static final int SUBSCRIBE_MODE = 1;

    public static final int POLL_MODE = 2;

    /** The name of the sink */
    public String sinkName;

    /** XML file name */
    public String fileName;

    private LakeSinkControlChannelListener controlListener;

    TurbineSinkConfig controlSinkConfig;

    /** Handle to the DataTurbine Sink configuration xml */
    public TurbineSinkConfig sinkConfig = null;

    /** 
   * The vector which contains the channel names that the sink has subscribed
   */
    public Vector<String> channelNames = new Vector<String>();

    /** 
   * The vector which contains the channel datatypes that the sink has 
   * subscribed. There is a one-to-one correspondence between
   * <tt>channelNames</tt> and this vector. The elements in this vector
   * can be Integer objects of the datatype constants as defined in
   * <tt>edu.sdsc.rtdsm.framework.util.Constants<tt> class
   * @see edu.sdsc.rtdsm.framework.util.Constants
   */
    public Vector<Integer> channelDataTypes = new Vector<Integer>();

    public Vector<Integer> channelPollIntervals = new Vector<Integer>();

    public TurbineServer server = new TurbineServer();

    public int requestMode;

    /** The handle to the instance which implements the callback method. This
   * method of the instance <tt>callbackHandler</tt> will be called upon
   * data arrival
   */
    public SinkCallBackListener callbackHandler;

    public long timeout;

    private Hashtable<TurbineServer, DswSink> controlSinkHash = new Hashtable<TurbineServer, DswSink>();

    /** 
   * The constructor
   * @param fileName The name of the xml file
   * @param sinkName The name of the sink.
   */
    public LakeSinkConfig(String fileName, String sinkName, SinkCallBackListener listener) {
        this(fileName, sinkName);
        this.callbackHandler = listener;
    }

    public LakeSinkConfig(String fileName, String sinkName) {
        this.sinkName = sinkName;
        this.fileName = fileName;
        this.controlSinkConfig = new TurbineSinkConfig(sinkName + Constants.LAKE_CONTROL_SINK_SUFFIX);
    }

    /**
   * @return The name of the sink
   */
    public String getName() {
        return sinkName;
    }

    /**
   * Parses the mainChannel element of the xml configuration.
   * @param mainChNode the main channel Element
   */
    public void parseMainChannel(Element mainChNode) {
        NodeList orbList = mainChNode.getChildNodes();
        Debugger.debug(Debugger.TRACE, "ParseMainChannels");
        for (int i = 0; i < orbList.getLength(); i++) {
            Node node = orbList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String tagName = ((Element) node).getTagName();
                if (tagName.equals(Constants.LAKE_CONFIG_XML_ORBPARAMS_TAG)) {
                    parseOrbParams((Element) node);
                    break;
                }
            }
        }
    }

    /**
   * Parses the orbParams element of the xml configuration.
   * @param orbParams the orbParams element
   */
    public void parseOrbParams(Element orbParams) {
        NodeList sourceList = orbParams.getChildNodes();
        String orbType = orbParams.getAttribute(Constants.LAKE_CONFIG_XML_ORB_TYPE_TAG);
        Debugger.debug(Debugger.TRACE, "ParseOrbParams");
        if (Constants.LAKE_CONFIG_XML_DATA_TURBINE_ORBTYPE_STR.equals(orbType)) {
            sinkConfig = new TurbineSinkConfig(sinkName);
        } else {
            throw new IllegalArgumentException("Currently only DataTurbine support is " + "provided for Sinks. ");
        }
        for (int i = 0; i < sourceList.getLength(); i++) {
            Node node = sourceList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String tagName = ((Element) node).getTagName();
                if (tagName.equals(Constants.LAKE_CONFIG_XML_SERVER_TAG)) {
                    parseServer((Element) node);
                }
            }
        }
    }

    /**
   * Parses the sink element of the xml configuration.
   * @param sink the sink element
   */
    private void parseSink(Element sinkEle) {
        Debugger.debug(Debugger.TRACE, "parseSink");
        String tagName = sinkEle.getTagName();
        if (sinkName.equals(sinkEle.getAttribute(Constants.LAKE_CONFIG_XML_NAME_TAG))) {
            Debugger.debug(Debugger.TRACE, "actually Parsing");
            NodeList sinkList = sinkEle.getChildNodes();
            Debugger.debug(Debugger.TRACE, "Parsing sink... " + sinkList.getLength());
            for (int i = 0; i < sinkList.getLength(); i++) {
                Node node = sinkList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    tagName = ((Element) node).getTagName();
                    Debugger.debug(Debugger.TRACE, "Parsing sink... " + node);
                    if (tagName.equals(Constants.LAKE_CONFIG_XML_MAIN_CHANNEL_TAG)) {
                        parseMainChannel((Element) node);
                    }
                }
            }
        }
    }

    /**
   * The method which parses the xml configuration file. The filename is
   * stored as instance variable <tt>fileName</tt>. This method loads
   * the configuration parameters of the sink from the xml file
   */
    public void parse() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document document = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new File(fileName));
            Node topNode = document.getDocumentElement();
            NodeList topList = document.getDocumentElement().getChildNodes();
            for (int i = 0; i < topList.getLength(); i++) {
                Node node = topList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getTagName().equals(Constants.LAKE_CONFIG_XML_SINK_TAG)) {
                    parseSink((Element) node);
                }
            }
            sinkConfig.printSinkData(Debugger.TRACE);
        } catch (ClassCastException cce) {
            cce.printStackTrace();
        } catch (SAXException sxe) {
            Exception x = sxe;
            if (sxe.getException() != null) x = sxe.getException();
            x.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        controlListener = new LakeSinkControlChannelListener(sinkName, controlSinkConfig, callbackHandler);
        controlSinkConfig.setCallBackListener(controlListener);
        controlListener.listenToControlChannels();
    }

    public LakeSinkControlChannelListener getControlChannelListener() {
        return controlListener;
    }

    /**
   * Parses the server element of the xml configuration.
   * @param serverEle the server element
   */
    public void parseServer(Element serverEle) {
        if (!serverEle.hasAttribute(Constants.LAKE_CONFIG_XML_SERVER_URI_TAG)) {
            throw new IllegalStateException("The server element in " + "the data config xml document has " + "has missing attribute: " + Constants.LAKE_CONFIG_XML_SERVER_URI_TAG);
        }
        String userName = null, password = null;
        String serverAddr = serverEle.getAttribute(Constants.LAKE_CONFIG_XML_SERVER_URI_TAG);
        if (serverEle.hasAttribute(Constants.LAKE_CONFIG_XML_SERVER_USERNAME_TAG)) {
            userName = serverEle.getAttribute(Constants.LAKE_CONFIG_XML_SERVER_USERNAME_TAG);
        }
        if (serverEle.hasAttribute(Constants.LAKE_CONFIG_XML_SERVER_PASSWORD_TAG)) {
            password = serverEle.getAttribute(Constants.LAKE_CONFIG_XML_SERVER_PASSWORD_TAG);
        }
        String feedbackHost = null;
        int feedbackPort = -1;
        if (serverEle.hasAttribute(TurbineSinkConfig.FEEDBACK_SERVER_TAG)) {
            feedbackHost = serverEle.getAttribute(TurbineSinkConfig.FEEDBACK_SERVER_TAG);
        }
        if (serverEle.hasAttribute(TurbineSinkConfig.FEEDBACK_PORT_TAG)) {
            feedbackPort = Integer.parseInt(serverEle.getAttribute(TurbineSinkConfig.FEEDBACK_PORT_TAG));
        }
        TurbineServer server = sinkConfig.getTurbineServer(serverAddr, userName, password);
        if (feedbackHost != null && feedbackPort != -1) {
            server.createFeedbackAgent(feedbackHost, feedbackPort, false);
        }
        addControlChannel(server);
        NodeList sourceList = serverEle.getChildNodes();
        for (int s = 0; s < sourceList.getLength(); s++) {
            Node sourceNode = sourceList.item(s);
            if (sourceNode.getNodeType() == Node.ELEMENT_NODE && ((Element) sourceNode).getTagName().equals(Constants.LAKE_CONFIG_XML_SOURCE_TAG)) {
                String sourceName = parseSource((Element) sourceNode);
                Element sourceEle = (Element) sourceNode;
                boolean fbReqd = false;
                Debugger.debug(Debugger.TRACE, "Checking feedback for source " + sourceName + " has tag = " + sourceEle.hasAttribute(Constants.LAKE_CONFIG_XML_SRC_FEEDBACK_REQD_TAG));
                if (sourceEle.hasAttribute(Constants.LAKE_CONFIG_XML_SRC_FEEDBACK_REQD_TAG)) {
                    String tmp = sourceEle.getAttribute(Constants.LAKE_CONFIG_XML_SRC_FEEDBACK_REQD_TAG);
                    if ("yes".equals(tmp)) {
                        fbReqd = true;
                    }
                }
                if (fbReqd) {
                    Debugger.debug(Debugger.TRACE, "Enabling feedback for source " + sourceName);
                    sinkConfig.enableFeedback(server, sourceName);
                }
                addSourceChannels(sourceName, server);
            }
        }
    }

    private void addControlChannel(TurbineServer server) {
        TurbineServer newServer = controlSinkConfig.getTurbineServer(server.getServerAddr(), server.getUsername(), server.getPassword());
        String reqPath = Constants.LAKE_CONTROL_SOURCE_NAME + "/" + Constants.LAKE_CONTROL_SOURCE_CHANNEL_NAME;
        newServer.addSinkChannel(reqPath, Constants.DATATYPE_STRING_OBJ, new Integer(TurbineSinkConfig.MONITOR_MODE), new Integer(-1));
    }

    /**
   * The method searchs for the SensorMetaData for the given <tt>source</tt>
   * sensor and subscribes the source channels for the given ORB. For the 
   * DataTurbine orb, it subscribes in "Monitor" mode with "-1" timeout
   * (indicating infinite wait till the data arrives)
   * @param sourceName The name of the source (sensor ID)
   * @param server The server under which the sink is present
   */
    public void addSourceChannels(String sourceName, TurbineServer server) {
        SensorMetaData smd = SensorMetaDataManager.getInstance().getSensorMetaDataIfPresent(sourceName);
        if (smd == null) {
            Debugger.debug(Debugger.RECORD, "Requesting Meta data for \"" + sourceName + "\"");
            SiteMetaDataRequester mdr = new SiteMetaDataRequester(sourceName);
            smd = mdr.call();
        }
        Vector<String> completeChannelStrs = new Vector<String>();
        Vector<Integer> channelDatatypeVec = new Vector<Integer>();
        Vector<Integer> reqModeVec = new Vector<Integer>();
        Vector<Integer> intervalOrToutVec = new Vector<Integer>();
        Vector<String> actChannelsVec = smd.getChannels();
        for (int i = 0; i < actChannelsVec.size(); i++) {
            completeChannelStrs.addElement(sourceName + "/" + actChannelsVec.elementAt(i));
            reqModeVec.addElement(new Integer(TurbineSinkConfig.MONITOR_MODE));
            intervalOrToutVec.addElement(new Integer(-1));
        }
        server.resetSinkWrapperChannelVecs(completeChannelStrs, smd.getChannelDatatypes(), reqModeVec, intervalOrToutVec);
    }

    /**
   * Parses the source element of the xml configuration.
   * @param sourceEle the source element
   */
    public String parseSource(Element sourceEle) {
        if (!sourceEle.hasAttribute(Constants.LAKE_CONFIG_XML_NAME_TAG)) {
            throw new IllegalStateException("The server element in " + "the data config xml document has " + "has missing attribute: " + Constants.LAKE_CONFIG_XML_NAME_TAG);
        }
        return sourceEle.getAttribute(Constants.LAKE_CONFIG_XML_NAME_TAG);
    }

    /**
   * Returns the handle to the sinkConfig object
   * @return The handle to the sink config of the current instance
   */
    public SinkConfig getSinkConfig() {
        return sinkConfig;
    }
}
