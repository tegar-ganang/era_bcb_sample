package com.cidero.upnp;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.cybergarage.xml.XML;

/**
 *
 *  Derived class of CDSItem used to represent audio broadcast
 *  content. This includes internet radio, as well as locally-generated
 *  broadcast (custom home shoutcast streams)
 *  
 */
public class CDSAudioBroadcast extends CDSAudioItem {

    static String upnpClass = "object.item.audioItem.audioBroadcast";

    String region;

    String radioCallSign;

    String radioStationID;

    String radioBand;

    String channelNr;

    public CDSAudioBroadcast() {
    }

    public CDSAudioBroadcast(Node node) {
        super(node);
        NodeList children = node.getChildNodes();
        for (int n = 0; n < children.getLength(); n++) {
            String nodeName = children.item(n).getNodeName();
            if (nodeName.equals("upnp:region")) region = CDS.getSingleTextNodeValue(children.item(n)); else if (nodeName.equals("upnp:radioCallSign")) radioCallSign = CDS.getSingleTextNodeValue(children.item(n)); else if (nodeName.equals("upnp:radioStationID")) radioStationID = CDS.getSingleTextNodeValue(children.item(n)); else if (nodeName.equals("upnp:radioBand")) radioBand = CDS.getSingleTextNodeValue(children.item(n)); else if (nodeName.equals("upnp:channelNr")) channelNr = CDS.getSingleTextNodeValue(children.item(n)); else if (nodeName.equals("upnp:artist")) setCreator(CDS.getSingleTextNodeValue(children.item(n)));
        }
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getRegion() {
        return region;
    }

    public void setRadioCallSign(String radioCallSign) {
        this.radioCallSign = radioCallSign;
    }

    public String getRadioCallSign() {
        return radioCallSign;
    }

    public void setRadioStationID(String radioStationID) {
        this.radioStationID = radioStationID;
    }

    public String getRadioStationID() {
        return radioStationID;
    }

    public void setRadioBand(String radioBand) {
        this.radioBand = radioBand;
    }

    public String getRadioBand() {
        return radioBand;
    }

    public void setChannelNr(String channelNr) {
        this.channelNr = channelNr;
    }

    public String getChannelNr() {
        return channelNr;
    }

    /**
   *  Get object class
   *
   *  @return  UPNP class string
   */
    public String getUPNPClass() {
        return upnpClass;
    }

    public String attributesToXML(CDSFilter filter) {
        return super.attributesToXML(filter);
    }

    public String elementsToXML(CDSFilter filter) {
        StringBuffer buf = new StringBuffer();
        buf.append(super.elementsToXML(filter));
        if ((region != null) && filter.propertyEnabled("upnp:region")) {
            XML.appendXMLElementLine(buf, "  ", "upnp:region", region);
        }
        if ((radioCallSign != null) && filter.propertyEnabled("upnp:radioCallSign")) {
            XML.appendXMLElementLine(buf, "  ", "upnp:radioCallSign", radioCallSign);
        }
        if ((radioStationID != null) && filter.propertyEnabled("upnp:radioStationID")) {
            XML.appendXMLElementLine(buf, "  ", "upnp:radioStationID", radioStationID);
        }
        if ((radioBand != null) && filter.propertyEnabled("upnp:radioBand")) {
            XML.appendXMLElementLine(buf, "  ", "upnp:radioBand", radioBand);
        }
        if ((channelNr != null) && filter.propertyEnabled("upnp:channelNr")) {
            XML.appendXMLElementLine(buf, "  ", "upnp:channelNr", channelNr);
        }
        return buf.toString();
    }

    public static void main(String[] args) {
        CDSAudioBroadcast obj = new CDSAudioBroadcast();
        obj.setId("10");
        obj.setParentId("1");
        obj.setTitle("Radio Paradise");
        obj.setCreator("Bill & Co ");
        obj.setRestricted(false);
        obj.setWriteStatus("WRITABLE");
        CDSResource resource = new CDSResource();
        resource.setName("http://www.radioparadise.com/musiclinks/rp_128.m3u");
        resource.setProtocolInfo("http-get:*:audio/mpegurl:*");
        resource.setSize(5000000);
        resource.setDuration("03:40");
        obj.addResource(resource);
        obj.setGenre("Adult Contemporary");
        CDSFilter filter = new CDSFilter("*");
        System.out.println(obj.toXML(filter));
    }
}
