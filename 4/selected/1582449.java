package edu.sdsc.rtdsm.drivers.turbine.util;

import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import com.rbnb.sapi.*;
import java.util.*;
import edu.sdsc.rtdsm.framework.src.*;
import edu.sdsc.rtdsm.framework.util.*;

public class TurbineSrcConfig implements SrcConfig {

    public static final String STORAGE_TAG = "storage";

    public static final String ARCHIVE_MODE_TAG = "archiveMode";

    public static final String ARCHIVE_SIZE_TAG = "archiveSize";

    public static final String CACHE_SIZE_TAG = "cacheSize";

    public static final String SERVER_TAG = "server";

    public static final String URI_TAG = "uri";

    public static final String USERNAME_TAG = "username";

    public static final String PASSWORD_TAG = "password";

    public static final String CHANNEL_TAG = "channel";

    public static final String CH_NAME_TAG = "name";

    public static final String CH_DATATYPE_TAG = "dataType";

    public static final int DEFAULT_CACHE_SIZE = 100;

    public static final String DEFAULT_ARCHIVE_MODE = "none";

    public static final int DEFAULT_ARCHIVE_SIZE = 0;

    public String srcName;

    public int bufferingType;

    public int cacheSize = DEFAULT_CACHE_SIZE;

    public String archiveMode = DEFAULT_ARCHIVE_MODE;

    public int archiveSize = DEFAULT_ARCHIVE_SIZE;

    public Vector<String> channelNames = new Vector<String>();

    public Vector<Integer> channelDataTypes = new Vector<Integer>();

    public TurbineServer server = new TurbineServer();

    public TurbineSrcConfig(String name) {
        srcName = name;
    }

    public String getName() {
        return srcName;
    }

    public void parse(Element orbType) {
        NodeList orbList = orbType.getChildNodes();
        for (int k = 0; k < orbList.getLength(); k++) {
            Node node_k = orbList.item(k);
            if (node_k.getNodeType() == Node.ELEMENT_NODE) {
                Element ele = (Element) node_k;
                String tagName = ((Element) node_k).getTagName();
                if (tagName.equals(TurbineSrcConfig.STORAGE_TAG)) {
                    if (ele.hasAttribute(TurbineSrcConfig.CACHE_SIZE_TAG)) {
                        cacheSize = Integer.parseInt(ele.getAttribute(TurbineSrcConfig.CACHE_SIZE_TAG));
                    }
                    if (ele.hasAttribute(TurbineSrcConfig.ARCHIVE_MODE_TAG)) {
                        archiveMode = ele.getAttribute(TurbineSrcConfig.ARCHIVE_MODE_TAG);
                    }
                    if (ele.hasAttribute(TurbineSrcConfig.ARCHIVE_SIZE_TAG)) {
                        archiveSize = Integer.parseInt(ele.getAttribute(TurbineSrcConfig.ARCHIVE_SIZE_TAG));
                    }
                } else if (tagName.equals(TurbineSrcConfig.SERVER_TAG)) {
                    parseServer(node_k);
                } else {
                    throw new IllegalStateException("The xml document has an invalid " + "element:" + tagName);
                }
            }
        }
        printSrcConfig();
    }

    public void printSrcConfig() {
        Debugger.debug(Debugger.TRACE, "Source params:");
        Debugger.debug(Debugger.TRACE, "name= " + srcName);
        Debugger.debug(Debugger.TRACE, "cacheSize= " + cacheSize);
        Debugger.debug(Debugger.TRACE, "archiveMode= " + archiveMode);
        Debugger.debug(Debugger.TRACE, "archiveSize= " + archiveSize);
        Debugger.debug(Debugger.TRACE, "\nServer params:");
        Debugger.debug(Debugger.TRACE, "Server name:" + server.serverAddr);
        Debugger.debug(Debugger.TRACE, "Server username:" + server.userName);
        Debugger.debug(Debugger.TRACE, "Server password:" + server.password);
        Debugger.debug(Debugger.TRACE, "\nChannel params:");
        for (int i = 0; i < channelNames.size(); i++) {
            Debugger.debug(Debugger.TRACE, "\t" + (i + 1) + " Channel name:" + channelNames.elementAt(i));
            Debugger.debug(Debugger.TRACE, "\t" + (i + 1) + " Channel type:" + channelDataTypes.elementAt(i));
        }
    }

    public void parseServer(Node serverNode) {
        if (serverNode.getNodeType() == Node.ELEMENT_NODE && ((Element) serverNode).getTagName().equals(TurbineSrcConfig.SERVER_TAG)) {
            Element serverEle = (Element) serverNode;
            if (!serverEle.hasAttribute(TurbineSrcConfig.URI_TAG)) {
                throw new IllegalStateException("The server element in " + " the data config xml document has " + "has missing attribute: " + TurbineSrcConfig.URI_TAG);
            }
            server.serverAddr = serverEle.getAttribute(TurbineSrcConfig.URI_TAG);
            if (serverEle.hasAttribute(TurbineSrcConfig.USERNAME_TAG)) {
                server.userName = serverEle.getAttribute(TurbineSrcConfig.USERNAME_TAG);
            }
            if (serverEle.hasAttribute(TurbineSrcConfig.PASSWORD_TAG)) {
                server.password = serverEle.getAttribute(TurbineSrcConfig.PASSWORD_TAG);
            }
            NodeList channelList = serverNode.getChildNodes();
            Debugger.debug(Debugger.TRACE, "channelList==== " + channelList);
            parseChannels(channelList);
        }
    }

    public void parseChannels(NodeList channelList) {
        for (int ch = 0; ch < channelList.getLength(); ch++) {
            Node channelNode = channelList.item(ch);
            Debugger.debug(Debugger.TRACE, "ch=" + ch + " Node type: " + channelNode.getNodeType() + " Node name:" + channelNode.getNodeName());
            if (channelNode.getNodeType() == Node.ELEMENT_NODE && ((Element) channelNode).getTagName().equals(TurbineSrcConfig.CHANNEL_TAG)) {
                Element channelEle = (Element) channelNode;
                if (!channelEle.hasAttribute(TurbineSrcConfig.CH_NAME_TAG)) {
                    throw new IllegalStateException("The channel element in " + " the data config xml document has " + "has missing attribute: " + TurbineSrcConfig.CH_NAME_TAG);
                }
                channelNames.add(channelEle.getAttribute(TurbineSrcConfig.CH_NAME_TAG));
                if (!channelEle.hasAttribute(TurbineSrcConfig.CH_DATATYPE_TAG)) {
                    throw new IllegalStateException("The channel element in " + " the data config xml document has " + "has missing attribute: " + TurbineSrcConfig.CH_DATATYPE_TAG);
                }
                String dt = channelEle.getAttribute(TurbineSrcConfig.CH_DATATYPE_TAG);
                if ("double".equals(dt)) {
                    channelDataTypes.add(Constants.DATATYPE_DOUBLE_OBJ);
                } else {
                    throw new IllegalStateException("Only double data " + "are supported for testing. More will be " + "included later");
                }
            }
        }
    }

    public TurbineServer getServer() {
        return server;
    }

    public Vector<String> getChannelNames() {
        return channelNames;
    }

    public Vector<Integer> getChannelDataTypes() {
        return channelDataTypes;
    }

    public void resetChannelVecs(Vector<String> channelVec, Vector<Integer> channelDatatypeVec) {
        if (channelVec.size() != channelDatatypeVec.size()) {
            throw new IllegalArgumentException("The channel vector and its " + "datatypes should be of the same size");
        }
        channelNames = channelVec;
        channelDataTypes = new Vector<Integer>();
        for (int i = 0; i < channelDatatypeVec.size(); i++) {
            if (channelDatatypeVec.elementAt(i) == Constants.DATATYPE_DOUBLE_OBJ) {
                channelDataTypes.addElement(Constants.DATATYPE_DOUBLE_OBJ);
            } else {
                throw new IllegalStateException("Only \"double\" data " + "is currently handled. More to be supported in future");
            }
        }
    }

    public void setServer(TurbineServer server) {
        this.server = server;
    }

    public void setChannelInfo(Vector channelVec, Vector channelDatatypeVec) {
        channelNames = channelVec;
        channelDataTypes = channelDatatypeVec;
    }
}
