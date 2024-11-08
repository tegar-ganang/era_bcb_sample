package com.cidero.upnp;

import org.w3c.dom.Node;

/**
 *
 *  Derived class of CDSVideoItem used to represent a movie
 *  (as opposed, for example, a continuous TV broadcast or a music video
 *  clip)
 *  
 */
public class CDSMovie extends CDSVideoItem {

    static String upnpClass = "object.item.videoItem.movie";

    String storageMedium;

    String DVDRegionCode;

    String channelName;

    String scheduledStartTime;

    String scheduledEndTime;

    public CDSMovie() {
    }

    public CDSMovie(Node node) {
        super(node);
    }

    public void setStorageMedium(String storageMedium) {
        this.storageMedium = storageMedium;
    }

    public String getStorageMedium() {
        return storageMedium;
    }

    public void setDVDRegionCode(String DVDRegionCode) {
        this.DVDRegionCode = DVDRegionCode;
    }

    public String getDVDRegionCode() {
        return DVDRegionCode;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setScheduledStartTime(String scheduledStartTime) {
        this.scheduledStartTime = scheduledStartTime;
    }

    public String getScheduledStartTime() {
        return scheduledStartTime;
    }

    public void setScheduledEndTime(String scheduledEndTime) {
        this.scheduledEndTime = scheduledEndTime;
    }

    public String getSheduledEndTime() {
        return scheduledEndTime;
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
        String elementXML = super.elementsToXML(filter);
        return elementXML;
    }

    public static void main(String[] args) {
        CDSMovie obj = new CDSMovie();
        obj.setId("10");
        obj.setParentId("1");
        obj.setTitle("Terminator");
        obj.setRestricted(false);
        obj.setWriteStatus("WRITABLE");
        obj.setGenre("Action");
        obj.setActor("Arnold");
        obj.setDirector("James Cameron");
        obj.setDescription("Machines rule the future");
        CDSFilter filter = new CDSFilter("*");
        System.out.println(obj.toXML(filter));
    }
}
