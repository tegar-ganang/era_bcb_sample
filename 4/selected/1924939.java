package com.pbxworkbench.pbx;

public class CallParameters {

    private static final int CALL_ORIGINATION_TIMEOUT = 30000;

    private IChannelAddress channelAddress;

    private IChannelApplet channelApplet;

    private String callingName;

    private String callingNumber;

    public CallParameters(IChannelAddress channelAddress, IChannelApplet channelApplet, String callingName, String callingNumber) {
        this.channelAddress = channelAddress;
        this.channelApplet = channelApplet;
        this.callingName = callingName;
        this.callingNumber = callingNumber;
    }

    public String getCallingName() {
        return callingName;
    }

    public String getCallingNumber() {
        return callingNumber;
    }

    public IChannelAddress getChannelAddress() {
        return channelAddress;
    }

    public IChannelApplet getChannelApplet() {
        return channelApplet;
    }

    public long getTimeout() {
        return CALL_ORIGINATION_TIMEOUT;
    }

    public String toString() {
        return "address " + channelAddress + " applet " + channelApplet + " name " + callingName + " number " + callingNumber;
    }
}
