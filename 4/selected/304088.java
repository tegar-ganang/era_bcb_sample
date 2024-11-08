package org.cdp1802.upb.impl;

import java.util.ArrayList;
import java.util.List;
import static org.cdp1802.upb.UPBConstants.*;
import org.cdp1802.upb.UPBDeviceEvent;
import org.cdp1802.upb.UPBDeviceI;
import org.cdp1802.upb.UPBDeviceListenerI;
import org.cdp1802.upb.UPBLinkI;
import org.cdp1802.upb.UPBLinkDevice;
import org.cdp1802.upb.UPBManager;
import org.cdp1802.upb.UPBMessage;
import org.cdp1802.upb.UPBMsgType;
import org.cdp1802.upb.UPBNetworkI;
import org.cdp1802.upb.UPBProductI;
import org.cdp1802.upb.UPBRoomI;

/**
 * Basic UPBDevice.  This provides minimum support for UPB functions
 * and serves as the base class for descendent classes that expose
 * functionality of more advanced devices.
 *
 * <P>NOTES ON CHANNELS
 *
 * <P>Most devices have only a single channel (that is, in most cases, a channel is
 * a controllable load).  Some have more than one and a very few have none at all.
 *
 * <P>As such, here is the channel numbering scheme:
 *
 * <P>
 *  -1 - INVALID_CHANNEL -- used only for devices with no channels at all<BR/>
 *   0 - ALL_CHANNELS -- used to address all channels of a device as one (broadcast)<BR/>
 *   n - Channel #, 1 to n (where n is the number of channels on the device)
 *
 * <P>Devices that have more than channel tend to have a primary channel which is
 * often, though not necessarily, channel 1.  The UPBProduct for a device indicates
 * what the default channel should be for a device.
 * @author gerry
 */
public class UPBDevice implements UPBDeviceI {

    UPBNetworkI deviceNetwork = null;

    int deviceID = 0;

    int upbOptions = 0;

    int upbVersion = 0;

    UPBProductI upbProduct = null;

    int firmwareVersion = 0;

    int serialNumber = 0;

    int channelCount = ALL_CHANNELS;

    int primaryChannel = INVALID_CHANNEL;

    int receiveComponentCount = 0;

    boolean transmitsLinks = false;

    boolean sendsStateReports = true;

    boolean supportsFading = false;

    String deviceName = "";

    UPBRoomI deviceRoom = null;

    ArrayList<UPBLinkDevice> deviceLinks = new ArrayList<UPBLinkDevice>();

    boolean isDimmable[] = null;

    int deviceState[] = null;

    boolean deviceStateValid = false;

    UPBDevice() {
    }

    UPBDevice(UPBNetworkI theNetwork, UPBProductI theProduct, int deviceID) {
        setDeviceInfo(theNetwork, theProduct, deviceID);
    }

    public void setDeviceInfo(UPBNetworkI theNetwork, UPBProductI theProduct, int deviceID) {
        this.deviceNetwork = theNetwork;
        this.upbProduct = theProduct;
        this.deviceID = deviceID;
        channelCount = upbProduct.getChannelCount();
        primaryChannel = upbProduct.getPrimaryChannel();
        receiveComponentCount = upbProduct.getReceiveComponentCount();
        deviceState = new int[(getChannelCount() < 1) ? 1 : getChannelCount()];
        isDimmable = new boolean[(getChannelCount() < 1) ? 1 : getChannelCount()];
        for (int chanIndex = 0; chanIndex < deviceState.length; chanIndex++) {
            deviceState[chanIndex] = UNASSIGNED_DEVICE_STATE;
            isDimmable[chanIndex] = upbProduct.isDimmingCapable();
        }
    }

    void debug(String theMessage) {
        deviceNetwork.getUPBManager().upbDebug("DEVICE[" + deviceID + "]:: " + theMessage);
    }

    void error(String theMessage) {
        deviceNetwork.getUPBManager().upbError("DEVICE[" + deviceID + "]:: " + theMessage);
    }

    /**
   * Return the network ID associated with this device.  All devices are associated
   * with a single network
   *
   * @return network associated with this device
   */
    public int getNetworkID() {
        return deviceNetwork.getNetworkID();
    }

    /**
   * Return this devices ID (or device number).  Each device in a network has
   * a unique ID.
   *
   * @return device ID for this device
   */
    public int getDeviceID() {
        return deviceID;
    }

    /**
   * Get the product record that further describes the type of device and
   * it capabilities this device actually is.
   *
   * @return product/manufacturer data for this device
   */
    public UPBProductI getProductInfo() {
        return upbProduct;
    }

    /**
   * Return the number of channels this device has
   *
   * @return number of channels or 0 if device has none
   */
    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(int theCount) {
        channelCount = theCount;
    }

    /**
   * Return primary channel for the device (or -1 if no channels)
   *
   * @return primary channel for device or -1
   */
    public int getPrimaryChannel() {
        return primaryChannel;
    }

    /**
   * Get the number of receive components for this device 
   *
   * @return number of receive components
   */
    public int getReceiveComponentCount() {
        return receiveComponentCount;
    }

    public void setReceiveComponentCount(int theCount) {
        receiveComponentCount = theCount;
    }

    /**
   * Get this devices firmware version.  Major version number is in the
   * high 8 bits, minor version in the lower 8 bits.
   *
   * <P>NOTE: Firmware versions are entirely under the control of each
   * manufacture and as such, only have meaning withing a specific
   * product of a specific manufacturer.
   *
   * @return version of the firmware on this device
   */
    public int getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(int version) {
        firmwareVersion = version;
    }

    /**
   * Get the serial number stamped into this device.
   *
   * NOTE: Many devices are currently returning 0 as the serial
   * number.
   *
   * @return serial number of this device, if any
   */
    public int getSerialNumber() {
        return serialNumber;
    }

    /**
   * Return the UPB option register for this device.  See the UPB protocol
   * description for the meaning of it.
   *
   * @return UPB options for this device
   */
    public int getUPBOptions() {
        return upbOptions;
    }

    /**
   * Return the current UPB protocol version support by this device.
   * Major version is in the high 8 bits, minor version in the low
   *
   * @return UPB protocol version this device supports
   */
    public int getUPBVersion() {
        return upbVersion;
    }

    /**
   * UPB devices that transmit messages can either transmit direct device
   * commands or more commonly transmit link commands in response to them
   * being controller.  A device can have only one setting for all it's
   * transmitted messages.
   *
   * @return true if device transmits links, false if direct device messages
   */
    public boolean doesTransmitsLinks() {
        return transmitsLinks;
    }

    public void setTransmitsLinks(boolean transmitsLinks) {
        this.transmitsLinks = transmitsLinks;
    }

    /**
   * Is this device dimmable or not.  Being dimmable is a combination of whether
   * the underlying device is actually capable of dimming and if the device has
   * been configured to dim or not (you can configure dimming modules to not
   * dim and they will only have an on and off status).
   *
   * <P>NOTE: The base UPBDevice class does not have tools for setting and
   * querying the devices level.  If this is true, this is likely an instance
   * of UPBDimmerDevice.  Casting to UPBDimmerDevice will allow such control
   * (use instanceof to be sure before casting).
   *
   * @param theChannel channel to check to see if dimmable or not
   * @return is this device capable of being dimmed or not
   */
    public boolean isDimmable(int theChannel) {
        return upbProduct.isDimmingCapable() && isDimmable[theChannel - 1];
    }

    public void setDimmable(boolean dimmable, int theChannel) {
        isDimmable[theChannel - 1] = dimmable;
    }

    /**
   * Is this device dimmable or not.  Being dimmable is a combination of whether
   * the underlying device is actually capable of dimming and if the device has
   * been configured to dim or not (you can configure dimming modules to not
   * dim and they will only have an on and off status).
   *
   * <P>NOTE: The base UPBDevice class does not have tools for setting and
   * querying the devices level.  If this is true, this is likely an instance
   * of UPBDimmerDevice.  Casting to UPBDimmerDevice will allow such control
   * (use instanceof to be sure before casting).
   *
   * @return is this device capable of being dimmed or not
   */
    public boolean isDimmable() {
        return isDimmable(upbProduct.getPrimaryChannel());
    }

    /**
   * Some UPB switches have buttons on them (wall switches in particular) and they
   * can be configured to send a status report anytime they are manually changed
   * (i.e. someone turns them on or off or dims them).
   *
   * <P>NOTE: While most switches default to this being turned off, it VERY important you
   * configure the switches to turn this on or UPB4Java will not be able to
   * completely track where your lights are set.
   *
   * @return true if this device is configured to send status reports, false if not
   */
    public boolean doesSendStateReports() {
        return sendsStateReports;
    }

    public boolean isStatusQueryable() {
        return upbProduct.isStatusQueryable();
    }

    /**
   * Get the textual name for this device
   *
   * @return name of this device (max 16 chars)
   */
    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String newName) {
        if ((newName == null) || (newName.length() == 0) || newName.equals(deviceName)) return;
        deviceName = newName;
        deviceNetwork.getUPBManager().fireDeviceEvent(this, UPBDeviceEvent.EventCode.DEVICE_ID_CHANGED, ALL_CHANNELS);
    }

    /**
   * Get the room this device is in.
   *
   * @return instance of the room this device is in/assigned to
   */
    public UPBRoomI getRoom() {
        return deviceRoom;
    }

    public void setRoom(UPBRoomI theRoom) {
        if ((theRoom == null) || (theRoom == deviceRoom)) return;
        if (deviceRoom != null) deviceRoom.removeDevice(this);
        deviceRoom = theRoom;
        if (deviceRoom != null) deviceRoom.addDevice(this);
        deviceNetwork.getUPBManager().fireDeviceEvent(this, UPBDeviceEvent.EventCode.DEVICE_ID_CHANGED, ALL_CHANNELS);
    }

    /**
   * Add a listener that will receives events generated by this device
   *
   * <P>NOTE: This is just shorthand for UPBManager.getManager().addDeviceListener(theListener, this);
   *
   * @param theListener listener to receive this devices events
   */
    public void addListener(UPBDeviceListenerI theListener) {
        deviceNetwork.getUPBManager().addDeviceListener(theListener, this);
    }

    /**
   * Remove a listener for this device
   *
   * <P>NOTE: This is just shorthand for UPBManager.getManager().removeDeviceListener(theListener, this);
   *
   * @param theListener the listener to remove
   */
    public void removeListener(UPBDeviceListenerI theListener) {
        deviceNetwork.getUPBManager().removeDeviceListener(theListener, this);
    }

    /**
   * Request this device send us it's current status
   *
   * <P>NOTE: if there is a request to send this devices status already
   * in the queue, this will be skipped
   */
    public void requestStateFromDevice() {
        deviceNetwork.getUPBManager().queueStateRequest(this);
    }

    /**
   * Determine if there is a pending request for the devices
   * state.  
   *
   * @return true if there is a pending state request for this device
   */
    public boolean isStateRequestPending() {
        return deviceNetwork.getUPBManager().isStateRequestQueued(this);
    }

    /**
   * Get the number of links this device is associated with.  Most
   * devices have a maximum of 16 links they can be associated with, but
   * it can vary.
   *
   * @return number of links device is currently associated with
   */
    public int getLinkCount() {
        return deviceLinks.size();
    }

    /**
   * Get a link associated with this device.  If the passed index is
   * invalid (too high/low), null is returned.  Indicies start at 0.
   *
   * @param linkIndex index into the links to return
   * @return instance of the link/device record or null if bad index
   */
    public UPBLinkDevice getLinkAt(int linkIndex) {
        if ((linkIndex < 0) || (linkIndex >= deviceLinks.size())) return null;
        return deviceLinks.get(linkIndex);
    }

    /**
   * Given a specific link ID, see if this device is associated with it
   * and if so, return the link/device record describing that association
   *
   * @param linkID link ID to check device for
   * @return instance of the link/device record or null if device is not associated with the passed link ID
   */
    public UPBLinkDevice getLinkByID(int linkID) {
        for (UPBLinkDevice devLink : deviceLinks) {
            if (devLink.getLink().getLinkID() == linkID) return devLink;
        }
        return null;
    }

    public void removeLinkDevice(UPBLinkDevice linkDevice) {
        deviceLinks.remove(linkDevice);
    }

    /**
   * Return a collection of links for this device, useful for
   * iterating over all the links.
   *
   * <P>Links are returned in Link ID order.
   *
   * @return collection of the links this device is associated with
   */
    public List<UPBLinkDevice> getLinks() {
        return deviceLinks;
    }

    boolean addToLink(UPBLinkI toLink, int deviceLevel, int fadeRate, int theChannel) {
        return toLink.addDevice(this, deviceLevel, fadeRate, theChannel);
    }

    boolean removeFromLink(UPBLinkI fromLink, int fromChannel) {
        return fromLink.removeDevice(this, fromChannel);
    }

    boolean removeFromLink(UPBLinkI fromLink) {
        return removeFromLink(fromLink, -1);
    }

    void removeFromAllLinks() {
        UPBLinkDevice devLink;
        for (int linkIndex = deviceLinks.size() - 1; linkIndex >= 0; linkIndex--) {
            if ((devLink = deviceLinks.get(linkIndex)) == null) continue;
            devLink.getLink().removeDevice(this, -1);
            devLink.releaseResources();
        }
        deviceLinks.clear();
    }

    /**
   * Is the devices state known?  Only true after we have received
   * some sort of status update on the device
   *
   * @return true of our status is OK, false if not yet known
   */
    public boolean isDeviceStateValid() {
        if (deviceStateValid) return true;
        for (int chanIndex = 0; chanIndex < deviceState.length; chanIndex++) {
            if (deviceState[chanIndex] == UNASSIGNED_DEVICE_STATE) return false;
        }
        return (deviceStateValid = true);
    }

    /**
   * Is this device On?  UPB does not really have a concept of on and off,
   * but generally any device at level 0 is off and any level above 0 is
   * on.
   *
   * @param forChannel check the state for the channel (channels are 1 based (i.e. channel #1 is passed as 1)
   * @return true if device is on (not at level 0) or false if off
   */
    public boolean isDeviceOn(int forChannel) {
        if ((forChannel < 1) || (forChannel > deviceState.length)) return false;
        return deviceState[forChannel - 1] > 0;
    }

    /**
   * Is this device On?  UPB does not really have a concept of on and off,
   * but generally any device at level 0 is off and any level above 0 is
   * on.
   *
   * <P>NOTE: This checks the primary channel for the device which is almost
   * always what you want (most devices have only a single channel);
   *
   * @return true if device is on (not at level 0) or false if off
   */
    public boolean isDeviceOn() {
        return isDeviceOn(upbProduct.getPrimaryChannel());
    }

    /**
   * Tell the channel on this UPB device to turn on.  For dimming devices, ON means go to
   * 100%.  For no dimming devices, the ON concept is a bit simpler.
   *
   * <P>If this is a change for the device, a device state message will be sent to listeners
   *
   * @param forChannel channel to turn on
   */
    public void turnDeviceOn(int forChannel) {
        transmitNewDeviceLevel(DEFAULT_DIM_LEVEL, DEFAULT_FADE_RATE, forChannel);
    }

    /**
   * Tell all channels on this UPB device to turn on.  For dimming devices, ON means go to
   * 100%.  For no dimming devices, the ON concept is a bit simpler.
   *
   * <P>If this is a change for the device, a device state message will be sent to listeners
   *
   */
    public void turnDeviceOn() {
        transmitNewDeviceLevel(DEFAULT_DIM_LEVEL, DEFAULT_FADE_RATE, ALL_CHANNELS);
    }

    /**
   * Tell the channel on this UPB device to turn off.  For dimming devices, OFF means go to
   * 0%.  For no dimming devices, the OFF concept is a bit simpler.
   *
   * <P>If this is a change for the device, a device state message will be sent to listeners
   * 
   * @param forChannel channel to turn off
   */
    public void turnDeviceOff(int forChannel) {
        transmitNewDeviceLevel(0, DEFAULT_FADE_RATE, forChannel);
    }

    /**
   * Tell all channels on this UPB device to turn off.  For dimming devices, OFF means go to
   * 0%.  For no dimming devices, the OFF concept is a bit simpler.
   *
   * <P>If this is a change for the device, a device state message will be sent to listeners
   */
    public void turnDeviceOff() {
        transmitNewDeviceLevel(0, DEFAULT_FADE_RATE, ALL_CHANNELS);
    }

    void deviceStateChanged(int forChannel) {
        deviceNetwork.getUPBManager().fireDeviceEvent(this, UPBDeviceEvent.EventCode.DEVICE_STATE_CHANGED, forChannel);
    }

    /**
   * Update the internal state of this devices level, but do not send any
   * commands to the UPB network.  
   *
   * <P>If the new level is not a change, we discard and return
   *
   * <P>If it's a change to a new, invalid level, we do not apply it, but we do
   * queue a request to the device to ask what it's real level is
   *
   * <P>Otherwise, install the new level and fire an event to tell folks
   * about the new level
   *
   * @param newLevel new level to apply
   * @param atFadeRate fade rate to apply at
   * @param forChannel channel to apply level change to
   */
    public void updateInternalDeviceLevel(int newLevel, int atFadeRate, int forChannel) {
        if ((newLevel < 0) || (newLevel > 100)) {
            if (UPBManager.DEBUG_MODE) debug("Got request to set light to non-normative level -- queueing status request to confirm");
            deviceNetwork.getUPBManager().queueStateRequest(this);
            return;
        }
        int lowChannel = 0, highChannel = deviceState.length - 1;
        if (forChannel > 0) {
            lowChannel = forChannel - 1;
            highChannel = forChannel - 1;
        }
        for (int chanIndex = lowChannel; chanIndex <= highChannel; chanIndex++) {
            if (newLevel == deviceState[chanIndex]) continue;
            if (UPBManager.DEBUG_MODE) debug("Level changing on channel " + chanIndex + " from " + deviceState[chanIndex] + " to " + newLevel);
            deviceState[chanIndex] = newLevel;
            deviceStateChanged(chanIndex + 1);
        }
    }

    void updateInternalDeviceLevels(int[] vals) {
        for (int i = 0; i < vals.length; i++) {
            int newLevel = vals[i];
            int forChannel = i + 1;
            if ((newLevel < 0) || (newLevel > 100)) {
                if (UPBManager.DEBUG_MODE) debug("Got request to set light to non-normative level -- queueing status request to confirm");
                deviceNetwork.getUPBManager().queueStateRequest(this);
                return;
            }
            int lowChannel = 0, highChannel = deviceState.length - 1;
            if (forChannel > 0) {
                lowChannel = forChannel - 1;
                highChannel = forChannel - 1;
            }
            for (int chanIndex = lowChannel; chanIndex <= highChannel; chanIndex++) {
                if (newLevel == deviceState[chanIndex]) continue;
                if (UPBManager.DEBUG_MODE) debug("Level changing on channel " + chanIndex + " from " + deviceState[chanIndex] + " to " + newLevel);
                deviceState[chanIndex] = newLevel;
                deviceStateChanged(chanIndex + 1);
            }
        }
    }

    /**
   * Given the passed parameters, send a command out telling the device
   * to go to this level at this rate on this channel.  If the command
   * was successful, then return true.  If the command was not confirmed
   * or timed out, return fase
   *
   * <P>This should NEVER be called directly -- it is a helper/intercept point 
   * used exclusively by transmitNewDeviceLevel
   *
   * <P>Descendent classes can override this to differently format the message
   *
   * @param theLevel level to set device to (0-100 or DEFAULT_DIM_LEVEL OR LAST_DIM_LEVEL
   * @param theFadeRate rate level should fade to at
   * @param theChannel channel to set level for
   * @return true of the device level was confirmed received by the device, false if not or timedout
   */
    boolean sendNewDeviceLevelMessage(int theLevel, int theFadeRate, int theChannel) {
        return deviceNetwork.getUPBManager().sendConfirmedMessage(new UPBMessage(this, UPBMsgType.GOTO, theLevel, theFadeRate, theChannel));
    }

    /** 
   * Invoked to post an update to the devices internal state after a command
   * to a device has been successfully sent.
   *
   * <P>This should NEVER be called directly -- it is a helper/intercept point 
   * used exclusively by transmitNewDeviceLevel
   *
   * <P>Descendent classes can override this to differntly handle applying/interpretting
   * updates in their own way
   * 
   * @param theLevel new level to install for device
   * @param theFadeRate new fade rate level was applied at
   * @param theChannel channel level/fade rate were applied to
   */
    void installNewDeviceLevel(int theLevel, int theFadeRate, int theChannel) {
        updateInternalDeviceLevel(theLevel, theFadeRate, theChannel);
    }

    /**
   * Given the passed candidate device level, fade rate and channel, determine
   * if this represents a real change to the device that should result in a 
   * command being sent to the device.  If true is returned, this is new and requires
   * a command be sent to the device.  If false, it's not new and no command 
   * should be sent.
   *
   * <P>This should NEVER be called directly -- it is a helper/intercept point 
   * used exclusively by transmitNewDeviceLevel
   *
   * <P>Descendent classes can override this to allow more complex determinations
   * about whether the new level represents a real change or not.
   *
   * @param newDeviceLevel candidate new level for device
   * @param atFadeRate candidate new fade rate
   * @param forChannel candidate channel for new level
   * @param true if these values are a change and require a command be sent to the device to effect the new level
   */
    boolean isReallyNewDeviceLevel(int newDeviceLevel, int atFadeRate, int forChannel) {
        if (!isDeviceStateValid()) return true;
        if ((newDeviceLevel < 0) || (newDeviceLevel > 100)) return true;
        int lowChannel = 0, highChannel = deviceState.length - 1;
        if (forChannel > 0) {
            lowChannel = forChannel - 1;
            highChannel = forChannel - 1;
        }
        for (int chanIndex = lowChannel; chanIndex <= highChannel; chanIndex++) {
            if (newDeviceLevel != deviceState[chanIndex]) return true;
        }
        return false;
    }

    /**
   * Given the passed level values, determine if they are a real change for the
   * device or not.  If not, return true and leave.  If they are a real change,
   * attempt to send a command to the device and if that command is successful,
   * install the values as the devices current state
   * 
   * <P>Descendent classes can override this to differently control and effect
   * changes to a devices level
   *
   * @param newDeviceLevel new level to conditionally set device to (0-100, DEFAULT_DIM_LEVEL or LAST_DIM_LEVEL)
   * @param atFadeRate fade rate to set device level (0-100 or DEFAULT_FADE_RATE)
   * @param toChannel channel to apply level to
   * @return true if the device is updated (either already updated or successfully commanded the device), false if there was an error and the update was not applied
   */
    boolean transmitNewDeviceLevel(int newDeviceLevel, int atFadeRate, int toChannel) {
        if (!isReallyNewDeviceLevel(newDeviceLevel, atFadeRate, toChannel)) return true;
        if (!sendNewDeviceLevelMessage(newDeviceLevel, atFadeRate, toChannel)) {
            if (UPBManager.DEBUG_MODE) debug("Request to set device level failed -- Asking device for current state");
            deviceNetwork.getUPBManager().queueStateRequest(this);
            return false;
        }
        installNewDeviceLevel(newDeviceLevel, atFadeRate, toChannel);
        if (UPBManager.DEBUG_MODE) debug("Confirmed request to set device to level succeedded -- updating internal state");
        return true;
    }

    /**
   * Parse and install a device state report.  Most times, this means just installing
   * the devices and firing a message
   *
   * <P>Descendent classes can override this to different parse status reports
   *
   * @param theMessage status report message
   */
    void receiveDeviceStateReport(UPBMessage theMessage) {
        deviceNetwork.getUPBManager().cancelStateRequest(this);
        updateInternalDeviceLevels(theMessage.getValues());
    }

    /**
   * Receive a GOTO type message -- a command to direct the device to 
   * a new level (presumably sent by someone else -- not us
   *
   * @param theMessage message received meant to change a devices level
   */
    void receiveDeviceGotoCommand(UPBMessage theMessage) {
        updateInternalDeviceLevel(theMessage.getLevel(), theMessage.getFadeRate(), theMessage.getChannel());
    }

    /**
   * Process a newly received message for this device.  
   *
   * <P>NOTE: In the case of wildcard commands, the device in the passed message 
   * may not match this device.  But in any other case, it will match this 
   * device
   *
   * @param theMessage message to be received by this device
   */
    public void receiveMessage(UPBMessage theMessage) {
        switch(theMessage.getMsgType()) {
            case GOTO:
            case FADE_START:
                receiveDeviceGotoCommand(theMessage);
                break;
            case FADE_STOP:
                deviceNetwork.getUPBManager().queueStateRequest(this);
                break;
            case DEVICE_STATE_RPT:
                receiveDeviceStateReport(theMessage);
                break;
        }
    }

    void customCopyFrom(UPBDevice parentDevice) {
    }

    public void copyFrom(UPBDeviceI origDevice) {
        UPBDevice parentDevice = (UPBDevice) origDevice;
        deviceNetwork = parentDevice.deviceNetwork;
        deviceID = parentDevice.deviceID;
        upbOptions = parentDevice.upbOptions;
        upbVersion = parentDevice.upbVersion;
        upbProduct = parentDevice.upbProduct;
        firmwareVersion = parentDevice.firmwareVersion;
        serialNumber = parentDevice.serialNumber;
        deviceName = parentDevice.deviceName;
        setRoom(parentDevice.deviceRoom);
        deviceStateValid = parentDevice.deviceStateValid;
        deviceState = parentDevice.deviceState;
        deviceLinks.clear();
        deviceLinks.addAll(parentDevice.deviceLinks);
        for (UPBLinkDevice theDeviceLink : deviceLinks) {
            theDeviceLink.setDevice(this);
        }
        customCopyFrom(parentDevice);
    }

    /**
   * Get a printable summary of this device composed of it's name and ID
   *
   * @return printable id of this device including it's name and ID
   */
    public String toString() {
        return deviceName + " (" + deviceID + ")";
    }

    /**
   * Release resources when the device is no longer needed
   *
   * <P>NOTE: Only the UPBManager should ever invoke this.  Anyone else
   * doing so will surely break things badly
   */
    public void releaseResources() {
        if (deviceNetwork != null) deviceNetwork.getUPBManager().removeAllRelatedDeviceListeners(this);
        if (deviceRoom != null) deviceRoom.removeDevice(this);
        if (deviceLinks != null) removeFromAllLinks();
        isDimmable = null;
        deviceState = null;
        deviceNetwork = null;
        deviceID = -1;
        upbProduct = null;
        deviceName = null;
        deviceLinks = null;
        deviceRoom = null;
    }
}
