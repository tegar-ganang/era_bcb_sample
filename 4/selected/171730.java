package org.cdp1802.upb;

import java.util.ArrayList;
import java.util.List;
import static org.cdp1802.upb.UPBConstants.*;

/**
 * An abstracted representation of a UPB message.  This is passed (to and from)
 * the underlying media handler by the UPB API
 *
 * <P>Instances of this device are never exposed publicly and are meant only
 * to convey information between UPBDevice implementations and UPB Media Adapters.
 *
 * <P>Link messages may in fact have a device accessible via the getDevice()
 * method.  When this is non-null, the device is the *originator* of the link
 * message (i.e. Device #14 caused Link #3 to be activated).  When there is
 * no source (link activated another way), this is null.
 *
 * @author gerry
 */
public class UPBMessage {

    public static final int UPBMSG_CONTROL_HIGH = 0;

    public static final int UPBMSG_CONTROL_LOW = 1;

    public static final int UPBMSG_NETWORK_ID = 2;

    public static final int UPBMSG_DEST_ID = 3;

    public static final int UPBMSG_SOURCE_ID = 4;

    public static final int UPBMSG_MESSAGE_ID = 5;

    public static final int UPBMSG_BODY = 6;

    static final int UPBMSG_DEVICE_ID = 0xff;

    private UPBMsgType theMsgType = null;

    private UPBDeviceI theDevice = null;

    private UPBLinkI theLink = null;

    private int theLevel = UNASSIGNED_DEVICE_STATE;

    private int theFadeRate = DEFAULT_FADE_RATE;

    private int theChannel = ALL_CHANNELS;

    private int theRegister = -1;

    private int numRegisters = -1;

    private int[] theValues = null;

    UPBQueuedMessageListenerI theListener = null;

    int messageIdent = 0;

    boolean expectAReply = false;

    int originationIdent = -1;

    boolean pimDirect = false;

    public UPBMessage(UPBDeviceI theDevice, UPBMsgType theMsgType) {
        this.theDevice = theDevice;
        this.theMsgType = theMsgType;
    }

    public UPBMessage(UPBDeviceI theDevice, UPBMsgType theMsgType, int theLevel, int theFadeRate, int forChannel) {
        this(theDevice, theMsgType);
        this.theLevel = theLevel;
        this.theFadeRate = theFadeRate;
        this.theChannel = forChannel;
    }

    public UPBMessage(UPBDeviceI theDevice, UPBMsgType theMsgType, int theRegister, int numRegisters) {
        this(theDevice, theMsgType);
        this.theRegister = theRegister;
        this.numRegisters = numRegisters;
    }

    public UPBMessage(UPBDeviceI theDevice, UPBMsgType theMsgType, int theRegister, int[] theValues) {
        this(theDevice, theMsgType);
        this.theRegister = theRegister;
        this.theValues = theValues;
    }

    public UPBMessage(UPBDeviceI theDevice, UPBMsgType theMsgType, int[] theValues) {
        this(theDevice, theMsgType);
        this.theValues = theValues;
    }

    public UPBMessage(UPBLinkI theLink, UPBMsgType theMsgType, int theLevel, int theFadeRate, UPBDeviceI sourceDevice) {
        this.theLink = theLink;
        this.theMsgType = theMsgType;
        this.theLevel = theLevel;
        this.theFadeRate = theFadeRate;
        this.theDevice = sourceDevice;
    }

    public UPBMessage(UPBLinkI theLink, UPBMsgType theMsgType, int theLevel, int theFadeRate) {
        this(theLink, theMsgType, theLevel, theFadeRate, null);
    }

    public UPBMessage(UPBLinkI theLink, UPBMsgType theMsgType, int theLevel) {
        this(theLink, theMsgType, theLevel, DEFAULT_FADE_RATE);
    }

    public UPBMessage(UPBLinkI theLink, UPBMsgType theMsgType) {
        this(theLink, theMsgType, 0);
    }

    public void setMessageListener(UPBQueuedMessageListenerI theListener, int messageIdent, boolean expectAReply) {
        this.theListener = theListener;
        this.messageIdent = messageIdent;
        this.expectAReply = expectAReply;
    }

    public void setOriginationIdent(int theIdent) {
        originationIdent = theIdent;
    }

    public int getOriginationIdent() {
        return originationIdent;
    }

    public boolean isLinkMessage() {
        return theLink != null;
    }

    public boolean isDeviceMessage() {
        return theLink == null;
    }

    public boolean isPIMMessage() {
        return theDevice != null && pimDirect;
    }

    public UPBDeviceI getDevice() {
        return theDevice;
    }

    public UPBLinkI getLink() {
        return theLink;
    }

    public UPBMsgType getMsgType() {
        return theMsgType;
    }

    public int getLevel() {
        return theLevel;
    }

    public int getFadeRate() {
        return theFadeRate;
    }

    public int getChannel() {
        return theChannel;
    }

    public int getRegister() {
        return theRegister;
    }

    public int getNumRegisters() {
        return numRegisters;
    }

    public int[] getValues() {
        return theValues;
    }

    public int getMessageIdent() {
        return messageIdent;
    }

    public void setMessageIdent(int newIdent) {
        messageIdent = newIdent;
    }

    public UPBQueuedMessageListenerI getMessageListener() {
        return theListener;
    }

    public boolean isExpectingAReply() {
        return expectAReply;
    }

    public void setDirect() {
        pimDirect = true;
    }

    public void updateMessageListener(UPBQueuedMessageListenerI.UPBMessageState messageState) {
        if (theListener == null) return;
        try {
            theListener.handleUPBQueuedMessageUpdate(this, messageState);
        } catch (Throwable unexpectedError) {
        }
    }

    public String toString() {
        StringBuffer theText = new StringBuffer();
        if (isLinkMessage()) {
            theText.append("Link ");
            theText.append(theLink.toString());
        } else {
            theText.append("Device ");
            theText.append(theDevice.toString());
        }
        switch(theMsgType) {
            case ACTIVATE_LINK:
                theText.append(" ACTIVATED");
                break;
            case DEACTIVATE_LINK:
                theText.append(" DEACTIVATED");
                break;
            case GOTO:
                theText.append(" GOTO level ");
                theText.append(theLevel);
                if (isDeviceMessage()) {
                    if (theDevice.getChannelCount() > 1) {
                        theText.append(" on chan #");
                        theText.append(theChannel);
                    }
                }
                break;
            case REPORT_STATE:
                theText.append(" QUERYING STATE");
                break;
            case FADE_START:
                theText.append(" FADING to level ");
                theText.append(theLevel);
                if (isDeviceMessage()) {
                    if (theDevice.getChannelCount() > 1) {
                        theText.append(" on chan #");
                        theText.append(theChannel);
                    }
                }
                break;
            case FADE_STOP:
                theText.append(" STOPPED FADING");
                break;
            case DEVICE_STATE_RPT:
                theText.append(" STATE REPORT");
                if (isDeviceMessage()) {
                    theText.append(" now @ level ");
                    theText.append(theLevel);
                    if (theDevice.getChannelCount() > 1) {
                        theText.append(" for chan #");
                        theText.append(theChannel);
                    }
                }
                break;
        }
        if (isLinkMessage() && theDevice != null) {
            theText.append(" sent by Device");
            theText.append(theDevice.toString());
        }
        return theText.toString();
    }

    public void releaseResources() {
        theMsgType = null;
        theDevice = null;
        theLink = null;
        theLevel = -1;
        theFadeRate = -1;
        theChannel = -1;
        theRegister = -1;
        numRegisters = -1;
        theValues = null;
        theListener = null;
        messageIdent = 0;
        expectAReply = false;
    }

    public int[] encode() {
        int theMessage[] = null;
        List<Integer> bodyList = new ArrayList<Integer>();
        int netId = 0;
        int destId = 0;
        if (isDeviceMessage()) {
            if (theDevice == null) return null;
            netId = theDevice.getNetworkID();
            destId = theDevice.getDeviceID();
            switch(theMsgType) {
                case GOTO:
                case FADE_START:
                    bodyList.add(encodeLevel(theLevel));
                    if ((theFadeRate != DEFAULT_FADE_RATE) || (theChannel != ALL_CHANNELS)) {
                        bodyList.add(theFadeRate);
                        if (theChannel != ALL_CHANNELS) bodyList.add(theChannel);
                    }
                    break;
                case GET_REGISTER_VALUE:
                    bodyList.add(theRegister);
                    bodyList.add(numRegisters);
                    break;
                case SET_REGISTER_VALUE:
                    bodyList.add(theRegister);
                    for (int val : theValues) {
                        bodyList.add(val);
                    }
                    break;
                case FADE_STOP:
                case REPORT_STATE:
                    break;
                default:
                    return null;
            }
        } else {
            if (theLink == null) return null;
            netId = theLink.getNetworkID();
            destId = theLink.getLinkID();
            switch(theMsgType) {
                case GOTO:
                case FADE_START:
                    bodyList.add(encodeLevel(theLevel));
                    if (theFadeRate != DEFAULT_FADE_RATE) bodyList.add(theFadeRate);
                    break;
                case ACTIVATE_LINK:
                case DEACTIVATE_LINK:
                case FADE_STOP:
                case REPORT_STATE:
                    break;
                default:
                    return null;
            }
        }
        int theMessageSize = bodyList.size() + 6;
        theMessage = new int[theMessageSize];
        theMessage[UPBMSG_CONTROL_HIGH] = isLinkMessage() ? (0x80 | (theMessageSize + 1)) : theMessageSize + 1;
        theMessage[UPBMSG_CONTROL_LOW] = 0x14;
        theMessage[UPBMSG_NETWORK_ID] = netId;
        theMessage[UPBMSG_DEST_ID] = destId;
        theMessage[UPBMSG_SOURCE_ID] = UPBMSG_DEVICE_ID;
        theMessage[UPBMSG_MESSAGE_ID] = theMsgType.getMdid();
        int i = UPBMSG_BODY;
        for (int val : bodyList) {
            theMessage[i++] = val;
        }
        return theMessage;
    }

    public static UPBMessage decode(int[] encoded) {
        UPBManager upbManager = UPBManager.getManager();
        UPBMessage parsedMessage = null;
        int parsedLevel = UNASSIGNED_DEVICE_STATE;
        int parsedFadeRate = DEFAULT_FADE_RATE;
        int parsedChannel = ALL_CHANNELS;
        boolean linkPacket = (encoded[UPBMSG_CONTROL_HIGH] & 0x80) != 0;
        int networkID = encoded[UPBMSG_NETWORK_ID];
        int destID = encoded[UPBMSG_DEST_ID];
        int sourceID = encoded[UPBMSG_SOURCE_ID];
        int messageID = encoded[UPBMSG_MESSAGE_ID];
        UPBMsgType parsedMsgType = UPBMsgType.getMsgType(messageID);
        int messageSet = (messageID >> 5) & 0x07;
        int targetID = destID;
        if (!linkPacket && (messageSet >= 4) && (messageSet <= 6)) targetID = sourceID;
        UPBNetworkI targetNetwork = upbManager.getNetworkByID(networkID);
        if (targetNetwork == null) {
            System.err.println("Recieved message from/for network #" + networkID + " which is not a known network -- message ignored");
            return null;
        }
        if (linkPacket) {
            UPBLinkI theLink = targetNetwork.getLinkByID(targetID);
            if (theLink == null) {
                if (UPBManager.DEBUG_MODE) upbManager.upbDebug("UPBMSG:: Unable to find LINK with ID " + targetID);
                return null;
            }
            UPBDeviceI sourceDevice = null;
            if ((sourceID > 0) && (sourceID <= 250)) {
                sourceDevice = targetNetwork.getDeviceByID(sourceID);
            }
            switch(parsedMsgType) {
                case ACTIVATE_LINK:
                case DEACTIVATE_LINK:
                case FADE_STOP:
                    parsedLevel = DEFAULT_DIM_LEVEL;
                    parsedFadeRate = DEFAULT_FADE_RATE;
                    break;
                case GOTO:
                case FADE_START:
                    if (encoded.length > UPBMSG_BODY) parsedLevel = encoded[UPBMSG_BODY];
                    if (encoded.length > UPBMSG_BODY + 1) parsedFadeRate = encoded[UPBMSG_BODY + 1];
                    parsedLevel = decodeLevel(parsedLevel);
                    break;
                default:
                    return null;
            }
            parsedMessage = new UPBMessage(theLink, parsedMsgType, parsedLevel, parsedFadeRate, sourceDevice);
        } else {
            UPBDeviceI theDevice = targetNetwork.getDeviceByID(targetID);
            if (theDevice == null) {
                if (UPBManager.DEBUG_MODE) upbManager.upbDebug("UPBMSG:: Unable to find DEVICE with ID " + targetID);
                return null;
            }
            switch(parsedMsgType) {
                case GOTO:
                case FADE_START:
                    if (encoded.length > UPBMSG_BODY) parsedLevel = encoded[UPBMSG_BODY];
                    if (encoded.length > UPBMSG_BODY + 1) parsedFadeRate = encoded[UPBMSG_BODY + 1];
                    if (encoded.length > UPBMSG_BODY + 2) parsedChannel = encoded[UPBMSG_BODY + 2];
                    parsedLevel = decodeLevel(parsedLevel);
                    parsedMessage = new UPBMessage(theDevice, parsedMsgType, parsedLevel, parsedFadeRate, parsedChannel);
                    break;
                case FADE_STOP:
                    parsedMessage = new UPBMessage(theDevice, parsedMsgType);
                    break;
                case DEVICE_STATE_RPT:
                    int[] vals = new int[encoded.length - UPBMSG_BODY];
                    int i = 0;
                    for (int chanIndex = UPBMSG_BODY; chanIndex < encoded.length; chanIndex++) {
                        vals[i++] = encoded[chanIndex];
                    }
                    parsedMessage = new UPBMessage(theDevice, UPBMsgType.DEVICE_STATE_RPT, vals);
                default:
                    return null;
            }
        }
        return parsedMessage;
    }

    private int encodeLevel(int theInternalLevel) {
        if ((theInternalLevel >= 0) && (theInternalLevel <= 100)) return theInternalLevel;
        if (theInternalLevel == LAST_DIM_LEVEL) return 255;
        if (theInternalLevel == DEFAULT_DIM_LEVEL) return 100;
        return 100;
    }

    private static int decodeLevel(int theInternalLevel) {
        if ((theInternalLevel >= 0) && (theInternalLevel <= 100)) return theInternalLevel;
        return LAST_DIM_LEVEL;
    }
}
