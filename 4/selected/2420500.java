package org.cdp1802.upb.impl;

import static org.cdp1802.upb.UPBConstants.*;
import org.cdp1802.upb.UPBDeviceI;
import org.cdp1802.upb.UPBDeviceListenerI;
import org.cdp1802.upb.UPBLinkDevice;
import org.cdp1802.upb.UPBMessage;
import org.cdp1802.upb.UPBMsgType;
import org.cdp1802.upb.UPBNetworkI;
import org.cdp1802.upb.UPBRoomI;

/**
 * A special device representing the wildcard abilities of UPB.
 *
 * Commands to this virtual device generally result in all devices
 * on the network responding (i.e. turnDevice() should turn all willing
 * devices on the UPB network off). 
 *
 * @author gerry
 */
public class UPBWildcardDevice extends UPBDimmerDevice {

    UPBWildcardDevice(UPBNetworkI theNetwork) {
        setDeviceInfo(theNetwork, UPBProduct.getGenericUPBProduct(), 0);
        deviceName = "Wildcard Device";
        deviceRoom = theNetwork.getDefaultRoom();
        firmwareVersion = 0;
        serialNumber = 0;
        upbVersion = 0;
        upbOptions = 0;
        transmitsLinks = false;
        sendsStateReports = false;
    }

    void debug(String theMessage) {
        deviceNetwork.getUPBManager().upbDebug("WILDCARD_DEVICE[" + deviceID + "]:: " + theMessage);
    }

    /** Not applicable to this device.  Always return true. */
    public boolean isDimmable() {
        return true;
    }

    public void setDeviceName(String newName) {
    }

    /** Always returns a "default room". */
    public UPBRoomI getRoom() {
        return deviceRoom;
    }

    public void setRoom(UPBRoomI theRoom) {
    }

    /** Not applicable to this device.  Does nothing. */
    public void addListener(UPBDeviceListenerI theListener) {
    }

    /** Not applicable to this device. Does nothing. */
    public void removeListener(UPBDeviceListenerI theListener) {
    }

    /** Not applicable to this device.  Always returns 0. */
    public int getLinkCount() {
        return 0;
    }

    /** Not applicable to this device. Always returns null. */
    public UPBLinkDevice getLinkAt(int linkIndex) {
        return null;
    }

    /** Not applicable to this device.  Always returns null. */
    public UPBLinkDevice getLinkByID(int linkID) {
        return null;
    }

    boolean addToLink(UPBLink toLink, int deviceLevel, int fadeRate) {
        return false;
    }

    boolean removeFromLink(UPBLink fromLink) {
        return false;
    }

    void removeFromAllLinks() {
    }

    /** Not applicable to this device.  Always returns false */
    public boolean isDeviceStateValid() {
        return false;
    }

    /** Not applicable to this device. Always returns false. */
    public boolean isDeviceOn() {
        return false;
    }

    /******* Device Services *******/
    private void setAllDeviceLevels(int toLevel, int atFadeRate, int forChannel) {
        for (UPBDeviceI theDevice : deviceNetwork.getDevices()) {
            if (theDevice == null) continue;
            theDevice.updateInternalDeviceLevel(toLevel, atFadeRate, forChannel);
        }
    }

    /**
   * Tell ALL willing devices on this network to change their level of the
   * passed channel to the passed level at the passed rate.
   *
   * <P>NOTE: While most devices will respond to a wildcard request,
   * some will not (like IO devices and other specialty devices).
   *
   * @param toLevel new level (0-100) for the devices to goto
   * @param fadeRate rate/speed devices goto new level at
   * @param channel channel that devices should change to reflect this
   */
    public void setDeviceLevel(int toLevel, int fadeRate, int channel) {
        if (!transmitNewDeviceLevel(toLevel, fadeRate, channel)) return;
        setAllDeviceLevels(toLevel, fadeRate, channel);
    }

    /**
   * Tell ALL willing devices on this network to change their level to
   * the passed level at the passed rate.
   *
   * <P>NOTE: While most devices will respond to a wildcard request,
   * some will not (like IO devices and other specialty devices).
   *
   * @param toLevel new level (0-100) for the devices to goto
   * @param fadeRate rate/speed devices goto new level at
   */
    public void setDeviceLevel(int toLevel, int fadeRate) {
        setDeviceLevel(toLevel, fadeRate, ALL_CHANNELS);
    }

    /**  
   * Tell ALL willing devices on this network to change their level to the passed level.
   * The passed level must be between 0 and 100.
   *
   * <P>In this method, the devices will use their default fade rate to effect
   * the change
   *
   * <P>NOTE: While most devices will respond to a wildcard request,
   * some will not (like IO devices and other specialty devices).
   *
   * @param toLevel level to set devices to (0-100).
   */
    public void setDeviceLevel(int toLevel) {
        setDeviceLevel(toLevel, DEFAULT_FADE_RATE, ALL_CHANNELS);
    }

    /**
   * Turn ALL willing devices on this network on to the passed level using their
   * default fade rate.
   *
   * <P>NOTE: While most devices will respond to a wildcard request,
   * some will not (like IO devices and other specialty devices).
   *
   * @param toLevel level to set all devices to (0-100)
   */
    public void turnDeviceOn(int toLevel) {
        setDeviceLevel(toLevel, DEFAULT_FADE_RATE, ALL_CHANNELS);
    }

    /**
   * Turn ALL willing devices on this network on at 100% at the default fade rate
   *
   * <P>NOTE: While most devices will respond to a wildcard request,
   * some will not (like IO devices and other specialty devices).
   */
    public void turnDeviceOn() {
        setDeviceLevel(100);
    }

    /**
   * Turn ALL willing devices on this network off (that is, to level 0)
   *
   * <P>NOTE: While most devices will respond to a wildcard request,
   * some will not (like IO devices and other specialty devices).
   */
    public void turnDeviceOff() {
        turnDeviceOn(0);
    }

    /**
   * Tell ALL willing devices on this network to start changing their level to the
   * passed level over the passed amount of time.
   *
   * <P>This is very similar to the setDeviceLevel() method (in fact, they do the
   * same basic thing), but it's usually used with a later stopFade to halt
   * the transition to the level where it is at.  This is really only of value
   * for longer fade rates.
   *
   * <P>NOTE: While most devices will respond to a wildcard request,
   * some will not (like IO devices and other specialty devices).
   *
   * @param toLevel level to set devices to (0-100)
   * @param atFadeRate how fast should the change take
   * @param forChannel channel on all devices to affect
   */
    public void startFade(int toLevel, int atFadeRate, int forChannel) {
        if (!deviceNetwork.getUPBManager().sendConfirmedMessage(new UPBMessage(this, UPBMsgType.FADE_START, toLevel, atFadeRate, forChannel))) return;
        setAllDeviceLevels(toLevel, atFadeRate, forChannel);
    }

    /**
   * Tell ALL willing devices on this network to start changing their level to the
   * passed level over the passed amount of time.
   *
   * <P>This is very similar to the setDeviceLevel() method (in fact, they do the
   * same basic thing), but it's usually used with a later stopFade to halt
   * the transition to the level where it is at.  This is really only of value
   * for longer fade rates.
   *
   * <P>NOTE: While most devices will respond to a wildcard request,
   * some will not (like IO devices and other specialty devices).
   *
   * @param toLevel level to set devices to (0-100)
   * @param atFadeRate how fast should the change take
   */
    public void startFade(int toLevel, int atFadeRate) {
        startFade(toLevel, atFadeRate, ALL_CHANNELS);
    }

    /**
   * Tell ALL willing devices on this network to start changing their level to the
   * passed level at their default fade rate.
   *
   * <P>This is very similar to the setDeviceLevel() method (in fact, they do the
   * same basic thing), but it's usually used with a later stopFade to halt
   * the transition to the level where it is at.  This is really only of value
   * for longer fade rates.
   *
   * <P>NOTE: While most devices will respond to a wildcard request,
   * some will not (like IO devices and other specialty devices).
   *
   * @param toLevel level to set devices to (0-100)
   */
    public void startFade(int toLevel) {
        startFade(toLevel, DEFAULT_FADE_RATE, ALL_CHANNELS);
    }

    /**
   * Tell ALL willing devices on this network that are currently in the process of
   * fading to a new level to stop the fade immediatly and leave their level 
   * at whatever it was when the device recieved the command.
   *
   * <P>NOTE: Stopping fading affects all channels 
   *
   * <P>NOTE: While most devices will respond to a wildcard request,
   * some will not (like IO devices and other specialty devices).
   */
    public void stopFade() {
        if (!deviceNetwork.getUPBManager().sendConfirmedMessage(new UPBMessage(this, UPBMsgType.FADE_STOP))) return;
        for (UPBDeviceI theDevice : deviceNetwork.getDevices()) {
            if (theDevice == null) continue;
            deviceNetwork.getUPBManager().queueStateRequest(theDevice);
        }
    }

    public void receiveMessage(UPBMessage theMessage) {
        switch(theMessage.getMsgType()) {
            case GOTO:
            case FADE_START:
                setAllDeviceLevels(theMessage.getLevel(), theMessage.getFadeRate(), theMessage.getChannel());
                break;
            case FADE_STOP:
                for (UPBDeviceI theDevice : deviceNetwork.getDevices()) {
                    if (theDevice == null) continue;
                    deviceNetwork.getUPBManager().queueStateRequest(theDevice);
                }
                break;
        }
    }

    void customCopyFrom(UPBDeviceI parentDevice) {
    }

    public void copyFrom(UPBDeviceI parentDevice) {
    }

    public String toString() {
        return deviceName + " (" + deviceID + ")";
    }
}
