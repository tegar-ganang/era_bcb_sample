package net.sourceforge.jcoupling2.persistence;

import net.sourceforge.jcoupling2.exception.JCouplingException;

/**
 * A <code>channel</code> object is a technology-agnostic abstraction of a "conduit". It serves to pass messages between
 * communication endpoints. For a <code>channel</code> to become persistent it must first be added to the
 * {@link ChannelRepository ChannelRepository}.
 */
public class Channel {

    private Integer channelID = null;

    private String channelName = null;

    private String fingerprintTableName;

    private Integer middlewareAdapterID;

    private boolean isTimeDecoupled = false;

    private boolean isWSDLBacked = false;

    private boolean supportsInbound = false;

    private boolean supportsInvoke = false;

    private String wsdlChannelOperationName;

    private String wsdlChannelPortType = null;

    private String wsdlChannelUrl = null;

    private String msgSchemaIn = null;

    private String msgSchemaOut = null;

    private DataMapper dataMapper = null;

    public Channel() {
        dataMapper = new DataMapper();
    }

    /**
	 * create a new channel AND make it persistent
	 * 
	 * @param chName
	 * @param mwAdapterName
	 * @param fpTableName
	 * @throws JCouplingException
	 */
    public Channel(String chName, Integer mwAdapterID) throws JCouplingException {
        this();
        channelName = chName;
        setChannelID(dataMapper.addChannel(this));
        setMiddlewareAdapterID(mwAdapterID);
    }

    public Channel(String chName) {
        this.setChannelName(chName);
    }

    public Channel(Integer chID) {
        this.setChannelID(chID);
    }

    public Integer getChannelID() {
        return channelID;
    }

    public void setChannelID(Integer channelID) {
        this.channelID = channelID;
    }

    public String getFingerprintTableName() {
        return fingerprintTableName;
    }

    public void setFingerprintTableName(String fingerprintTableName) {
        this.fingerprintTableName = fingerprintTableName;
    }

    public boolean getIsTimeDecoupled() {
        return isTimeDecoupled;
    }

    public void setIsTimeDecoupled(boolean timeDecoupled) {
        isTimeDecoupled = timeDecoupled;
    }

    public boolean getIsWSDLBacked() {
        return isWSDLBacked;
    }

    public void setIsWSDLBacked(boolean WSDLBacked) {
        isWSDLBacked = WSDLBacked;
    }

    public String getMsgSchemaIn() {
        return msgSchemaIn;
    }

    public void setMsgSchemaIn(String msgSchemaIn) {
        this.msgSchemaIn = msgSchemaIn;
    }

    public String getMsgSchemaOut() {
        return msgSchemaOut;
    }

    public void setMsgSchemaOut(String msgSchemaOut) {
        this.msgSchemaOut = msgSchemaOut;
    }

    public boolean supportsInbound() {
        return supportsInbound;
    }

    public void setSupportsInbound(boolean supInbound) {
        supportsInbound = supInbound;
    }

    public boolean supportsInvoke() {
        return supportsInvoke;
    }

    public void setSupportsInvoke(boolean supInvoke) {
        this.supportsInvoke = supInvoke;
    }

    public String getWsdlChannelOperationName() {
        return wsdlChannelOperationName;
    }

    public void setWsdlChannelOperationName(String wsdlChannelOperationName) {
        this.wsdlChannelOperationName = wsdlChannelOperationName;
    }

    public String getWsdlChannelPortType() {
        return wsdlChannelPortType;
    }

    public void setWsdlChannelPortType(String wsdlChannelPortType) {
        this.wsdlChannelPortType = wsdlChannelPortType;
    }

    public String getWsdlChannelUrl() {
        return wsdlChannelUrl;
    }

    public void setWsdlChannelUrl(String wsdlChannelUrl) {
        this.wsdlChannelUrl = wsdlChannelUrl;
    }

    public int compareTo(Channel arg0) {
        return 0;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setMiddlewareAdapterID(Integer middlewareAdapterid) {
        this.middlewareAdapterID = middlewareAdapterid;
    }

    public Integer getMiddlewareAdapterID() {
        return middlewareAdapterID;
    }

    public void setChannelName(String chName) {
        channelName = chName;
    }
}
