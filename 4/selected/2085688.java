package org.cdp1802.upb.impl;

import java.util.ArrayList;
import java.util.List;
import static org.cdp1802.upb.UPBConstants.*;
import org.cdp1802.upb.UPBDeviceI;
import org.cdp1802.upb.UPBLinkI;
import org.cdp1802.upb.UPBLinkDevice;
import org.cdp1802.upb.UPBLinkEvent;
import org.cdp1802.upb.UPBLinkListenerI;
import org.cdp1802.upb.UPBManager;
import org.cdp1802.upb.UPBMessage;
import org.cdp1802.upb.UPBMsgType;
import org.cdp1802.upb.UPBNetworkI;

/**
 * A link is similar to a scene in that it "groups" a number of UPB devices
 * to be controlled with a single command.  Each link/device connection (or
 * association) has it's own level that device should go to when the link
 * is activated as well as a faderate to describe how long that change
 * should take.
 *
 * <P>In UPB terminology, "activating" a link tells all devices associated with
 * that link to go to the level programmed into them for that specific link.  
 * This often means the many devices in the link will go to different levels.
 *
 * <P>"deactivating" a link generally just turns all devices associated with the
 * link off, though it is *possible* the device may treat a deactivate command
 * differently (in short, the spec suggests devices turn off, but doesn't 
 * require it, though most lighting devices do).
 *
 * <P>In addition to activating and deactivating links, you can also command all
 * devices in a link to do the same thing.  For example, you tell all devices
 * in a link to go to 80% over 10 seconds.  
 *
 * <P>It's important to differentiate commanding all devices in a link vs. 
 * activating a link.  When activating, each associated device may go to a
 * different, pre-programmed level at a different pre-programmed rate.  When
 * a link is commanded, all the devices do the exact same thing (based on
 * the command) and ignore any pre-programmed levels for that link.
 *
 * @author gerry
 */
public class UPBLink implements UPBLinkI {

    UPBNetworkI linkNetwork = null;

    int linkID = 0;

    String linkName = "";

    ArrayList<UPBLinkDevice> linkedDevices = new ArrayList<UPBLinkDevice>();

    public UPBLink() {
        ;
    }

    /** Creates a new instance of UPBLink */
    UPBLink(UPBNetworkI theNetwork, int theLinkID) {
        this.linkNetwork = theNetwork;
        this.linkID = theLinkID;
    }

    void debug(String theMessage) {
        linkNetwork.getUPBManager().upbDebug("LINK[" + linkID + "]:: " + theMessage);
    }

    void error(String theMessage) {
        linkNetwork.getUPBManager().upbError("LINK[" + linkID + "]:: " + theMessage);
    }

    /**
   * Get the ID of the network this link is associated with
   *
   * @return network ID link is associated with
   */
    public int getNetworkID() {
        return linkNetwork.getNetworkID();
    }

    /**
   * Return the link ID of this link.  Each link ID is unique within a network.
   *
   * @return link ID for this link
   */
    public int getLinkID() {
        return linkID;
    }

    /**
   * Return the name of this link.  Some links may not have names and that will
   * not affect how they work (though it makes figuring out what is going on
   * a bit harder for people reading the results).
   *
   * @return name of this link
   */
    public String getLinkName() {
        return linkName;
    }

    public void setLinkName(String newName) {
        if ((newName == null) || (newName.length() == 0) || newName.equals(linkName)) return;
        linkName = newName;
    }

    /**
   * Activate all devices associated with this link.
   *
   * <P>Activating means that each device should go to it's own unique pre-programmed
   * level that was set when the device was associated with this link.
   *
   * <P>The distinction here (vs turning setLinkLevel or turnLinkOn/Off) is that
   * each device in the link will tend to have differernt levels it'll go 
   * to.  The setLinkLevel/turnLinkOnOff will set ALL devices in the link to
   * the SAME level (regardless of pre-programmed settings in the link
   * association).
   */
    public void activateLink() {
        if (!linkNetwork.getUPBManager().sendConfirmedMessage(new UPBMessage(this, UPBMsgType.ACTIVATE_LINK))) return;
        linkNetwork.getUPBManager().fireLinkEvent(this, UPBLinkEvent.EventCode.LINK_ACTIVATED);
        for (UPBLinkDevice linkedDevice : linkedDevices) {
            linkedDevice.getDevice().updateInternalDeviceLevel(linkedDevice.getLevel(), linkedDevice.getFadeRate(), linkedDevice.getChannel());
        }
    }

    /** 
   * Deactivate all devices associated with this link.
   *
   * <P>In general, this means "turn all devices associated with this link off",
   * but some devices may implement deactivate differently.  So while this
   * almost always does what you want, if you really just want to turn 
   * all the lights in a link off, it would be more explicit to use
   * turnLinkOff();
   */
    public void deactivateLink() {
        if (!linkNetwork.getUPBManager().sendConfirmedMessage(new UPBMessage(this, UPBMsgType.DEACTIVATE_LINK))) return;
        linkNetwork.getUPBManager().fireLinkEvent(this, UPBLinkEvent.EventCode.LINK_DEACTIVATED);
        for (UPBLinkDevice linkedDevice : linkedDevices) {
            linkedDevice.getDevice().updateInternalDeviceLevel(0, 0, linkedDevice.getChannel());
        }
    }

    /**
   * Command all devices associated with this link to all go to the
   * passed level at the passed faderate.
   *
   * <P>This is markedly different than activating a link (see the notes
   * on the activateLink() method for a more indepth explanation).
   *
   * @param toLevel new level (0-100, DEFAULT_DIM_LEVEL or LAST_DIM_LEVEL) all devices in the link should go to
   * @param atFadeRate rate devices should use to change level or DEFAULT_FADE_RATE for each device to use it's default rate
   */
    public void setLinkLevel(int toLevel, int atFadeRate) {
        if (!linkNetwork.getUPBManager().sendConfirmedMessage(new UPBMessage(this, UPBMsgType.GOTO, toLevel, atFadeRate))) return;
        linkNetwork.getUPBManager().fireLinkEvent(this, UPBLinkEvent.EventCode.LINK_GOTO, toLevel, atFadeRate, null);
        for (UPBLinkDevice linkedDevice : linkedDevices) {
            linkedDevice.getDevice().updateInternalDeviceLevel(toLevel, linkedDevice.getFadeRate(), linkedDevice.getChannel());
        }
    }

    /**
   * Command all devices associated with this link to all go to the
   * passed level at their default fade rates
   *
   * <P>This is markedly different than activating a link (see the notes
   * on the activateLink() method for a more indepth explanation).
   *
   * @param toLevel new level (0-100, DEFAULT_DIM_LEVEL or LAST_DIM_LEVEL) all devices in the link should go to
   */
    public void setLinkLevel(int toLevel) {
        setLinkLevel(toLevel, DEFAULT_FADE_RATE);
    }

    /**
   * Command all devices associated with this link to all go to level 0 (off)
   *
   * <P>This is markedly different than deactivating a link (see the notes
   * on the deactivateLink() method for a more indepth explanation).
   *
   */
    public void turnLinkOff() {
        setLinkLevel(0, DEFAULT_FADE_RATE);
    }

    /**
   * Command all devices associated with this link to all go to level 100 (on)
   *
   * <P>This is markedly different than activating a link (see the notes
   * on the activateLink() method for a more indepth explanation).
   */
    public void turnLinkOn() {
        setLinkLevel(100, DEFAULT_FADE_RATE);
    }

    /**
   * Command all devices associated with this link to all start fading to the
   * passed level at the passed faderate.
   *
   * <P>This is markedly different than activating a link (see the notes
   * on the activateLink() method for a more indepth explanation).
   *
   * @param toLevel new level (0-100, DEFAULT_DIM_LEVEL or LAST_DIM_LEVEL) all devices in the link should go to
   * @param atFadeRate rate devices should use to change level or DEFAULT_FADE_RATE for each device to use it's default rate
   */
    public void startLinkFade(int toLevel, int atFadeRate) {
        if (!linkNetwork.getUPBManager().sendConfirmedMessage(new UPBMessage(this, UPBMsgType.FADE_START, toLevel, atFadeRate))) return;
        linkNetwork.getUPBManager().fireLinkEvent(this, UPBLinkEvent.EventCode.LINK_START_FADE, toLevel, atFadeRate, null);
        for (UPBLinkDevice linkedDevice : linkedDevices) {
            linkedDevice.getDevice().updateInternalDeviceLevel(toLevel, atFadeRate, linkedDevice.getChannel());
        }
    }

    /**
   * Command all devices associated with this link to start fading to the passed
   * level at their default fade rates.
   *
   * <P>This is markedly different than activating a link (see the notes
   * on the activateLink() method for a more indepth explanation).
   *
   * @param toLevel new level (0-100, DEFAULT_DIM_LEVEL or LAST_DIM_LEVEL) all devices in the link should go to
   */
    public void startLinkFade(int toLevel) {
        startLinkFade(toLevel, DEFAULT_FADE_RATE);
    }

    /**
   * Command all devices to stop any in-progress fading and hold the level they
   * were at when they received the stop-fade command
   *
   * <P>This is markedly different than activating a link (see the notes
   * on the activateLink() method for a more indepth explanation).
   */
    public void stopLinkFade() {
        if (!linkNetwork.getUPBManager().sendConfirmedMessage(new UPBMessage(this, UPBMsgType.FADE_STOP))) return;
        linkNetwork.getUPBManager().fireLinkEvent(this, UPBLinkEvent.EventCode.LINK_STOP_FADE);
        for (UPBLinkDevice linkedDevice : linkedDevices) {
            linkNetwork.getUPBManager().queueStateRequest(linkedDevice.getDevice());
        }
    }

    /**
   * Get the number of devices associated with this link
   *
   * @return number of devices associated with link
   */
    public int getLinkedDeviceCount() {
        return linkedDevices.size();
    }

    /**
   * Get the link/device record at the passed index
   *
   * @param deviceIndex index of the link/device to return
   * @return link/device record at that index or null if invalid index passed
   */
    public UPBLinkDevice getLinkedDeviceAt(int deviceIndex) {
        return linkedDevices.get(deviceIndex);
    }

    /**
   * Scan the devices associated with link and if one of them matches
   * the passed device ID, return it
   *
   * @param deviceID device ID to scan this link for
   * @param theChannel channel in device to scan for (-1 means find first/any instance of device)
   * @return link/device record that matches the device ID or null if no match (device not associated with this link)
   */
    public UPBLinkDevice getLinkedDeviceById(int deviceID, int theChannel) {
        for (UPBLinkDevice theLinkedDevice : linkedDevices) {
            if (theLinkedDevice.getDevice().getDeviceID() != deviceID) continue;
            if ((theChannel != -1) && (theChannel != theLinkedDevice.getChannel())) continue;
            return theLinkedDevice;
        }
        return null;
    }

    /**
   * Return a collection of all link/device records describing devices
   * associated with this link.
   *
   * <P>The collection is in device ID order
   *
   * @return collection of all devices associated with this link
   */
    public List<UPBLinkDevice> getLinkedDevices() {
        return linkedDevices;
    }

    boolean addDevice(UPBLinkDevice theLinkedDevice) {
        if (theLinkedDevice.getDevice() instanceof UPBWildcardDevice) return false;
        if (getLinkedDeviceById(theLinkedDevice.getDevice().getDeviceID(), theLinkedDevice.getChannel()) != null) return false;
        int linkCount = theLinkedDevice.getDevice().getLinks().size();
        if (linkCount >= theLinkedDevice.getDevice().getReceiveComponentCount()) return false;
        int insertAt = -1;
        for (int deviceIndex = 0; deviceIndex < linkedDevices.size(); deviceIndex++) {
            if (linkedDevices.get(deviceIndex).getDevice().getDeviceID() > theLinkedDevice.getDevice().getDeviceID()) {
                insertAt = deviceIndex;
                linkedDevices.add(deviceIndex, theLinkedDevice);
                break;
            }
        }
        if (insertAt == -1) linkedDevices.add(theLinkedDevice);
        insertAt = -1;
        for (int linkIndex = 0; linkIndex < theLinkedDevice.getDevice().getLinks().size(); linkIndex++) {
            if (theLinkedDevice.getDevice().getLinks().get(linkIndex).getLink().getLinkID() > linkID) {
                insertAt = linkIndex;
                theLinkedDevice.getDevice().getLinks().add(linkIndex, theLinkedDevice);
                break;
            }
        }
        if (insertAt == -1) theLinkedDevice.getDevice().getLinks().add(theLinkedDevice);
        linkNetwork.getUPBManager().fireLinkEvent(this, UPBLinkEvent.EventCode.LINK_DEVICE_ADDED, theLinkedDevice);
        linkNetwork.getUPBManager().fireDeviceEvent(theLinkedDevice.getDevice(), org.cdp1802.upb.UPBDeviceEvent.EventCode.LINK_ADDED, theLinkedDevice);
        return true;
    }

    public boolean addDevice(UPBDeviceI theDevice, int theLevel, int theFadeRate, int forChannel) {
        UPBLinkDevice newLink = new UPBLinkDevice(this, theDevice, theLevel, theFadeRate, forChannel);
        if (!addDevice(newLink)) {
            newLink.releaseResources();
            return false;
        }
        return true;
    }

    boolean addDevice(UPBDeviceI theDevice, int theLevel, int theFadeRate) {
        return addDevice(theDevice, theLevel, theFadeRate, ALL_CHANNELS);
    }

    boolean addDevice(UPBDevice theDevice, int theLevel) {
        return addDevice(theDevice, theLevel, DEFAULT_FADE_RATE);
    }

    boolean addDevice(UPBDevice theDevice) {
        return addDevice(theDevice, 100);
    }

    public boolean removeDevice(UPBDeviceI theDevice, int theChannel) {
        UPBLinkDevice linkedDevice = null;
        boolean deviceRemoved = false;
        for (int deviceIndex = linkedDevices.size() - 1; deviceIndex >= 0; deviceIndex--) {
            if ((linkedDevice = linkedDevices.get(deviceIndex)).getDevice() != theDevice) continue;
            if ((theChannel != -1) && (theChannel != linkedDevice.getChannel())) continue;
            linkedDevices.remove(deviceIndex);
            theDevice.removeLinkDevice(linkedDevice);
            linkNetwork.getUPBManager().fireLinkEvent(this, UPBLinkEvent.EventCode.LINK_DEVICE_REMOVED, linkedDevice);
            linkNetwork.getUPBManager().fireDeviceEvent(theDevice, org.cdp1802.upb.UPBDeviceEvent.EventCode.LINK_REMOVED, linkedDevice);
            linkedDevice.releaseResources();
            deviceRemoved = true;
        }
        return deviceRemoved;
    }

    boolean removeDevice(UPBLinkDevice theLinkedDevice) {
        return removeDevice(theLinkedDevice.getDevice(), theLinkedDevice.getChannel());
    }

    public void removeAllDevices() {
        UPBLinkDevice linkedDevice = null;
        for (int deviceIndex = linkedDevices.size() - 1; deviceIndex >= 0; deviceIndex--) {
            linkedDevice = linkedDevices.get(deviceIndex);
            linkedDevices.remove(deviceIndex);
            linkedDevice.getDevice().removeLinkDevice(linkedDevice);
            linkNetwork.getUPBManager().fireLinkEvent(this, UPBLinkEvent.EventCode.LINK_DEVICE_REMOVED, linkedDevice);
            linkNetwork.getUPBManager().fireDeviceEvent(linkedDevice.getDevice(), org.cdp1802.upb.UPBDeviceEvent.EventCode.LINK_REMOVED, linkedDevice);
            linkedDevice.releaseResources();
        }
    }

    /**
   * Add a link event listener to this link
   *
   * <P>NOTE: This is the same as UPBManager.getManager().addLinkListener(theListener, this);
   *
   * @param theListener listener to receive events about this link
   */
    public void addListener(UPBLinkListenerI theListener) {
        linkNetwork.getUPBManager().addLinkListener(theListener, this);
    }

    /**
   * Remove a link event listener of this link
   *
   * <P>NOTE: This is the same as UPBManager.getManager().removeLinkListener(theListener, this);
   *
   * @param theListener listener to remove from list of this links event recipients
   */
    public void removeListener(UPBLinkListenerI theListener) {
        linkNetwork.getUPBManager().removeLinkListener(theListener, this);
    }

    public void receiveMessage(UPBMessage theMessage) {
        int theLevel = 0, theFadeRate = 0, theChannel = 0;
        switch(theMessage.getMsgType()) {
            case ACTIVATE_LINK:
                if (UPBManager.DEBUG_MODE) debug("Got Link ACTIVATE");
                linkNetwork.getUPBManager().fireLinkEvent(this, UPBLinkEvent.EventCode.LINK_ACTIVATED, DEFAULT_DIM_LEVEL, DEFAULT_FADE_RATE, theMessage.getDevice());
                for (UPBLinkDevice linkedDevice : linkedDevices) {
                    linkedDevice.getDevice().updateInternalDeviceLevel(linkedDevice.getLevel(), linkedDevice.getFadeRate(), linkedDevice.getChannel());
                }
                break;
            case DEACTIVATE_LINK:
                if (UPBManager.DEBUG_MODE) debug("Got Link DEACTIVATE");
                linkNetwork.getUPBManager().fireLinkEvent(this, UPBLinkEvent.EventCode.LINK_DEACTIVATED, DEFAULT_DIM_LEVEL, DEFAULT_FADE_RATE, theMessage.getDevice());
                for (UPBLinkDevice linkedDevice : linkedDevices) {
                    linkedDevice.getDevice().updateInternalDeviceLevel(0, linkedDevice.getFadeRate(), linkedDevice.getChannel());
                }
                break;
            case GOTO:
                if (UPBManager.DEBUG_MODE) debug("Got Link GOTO");
                theLevel = theMessage.getLevel();
                theFadeRate = theMessage.getFadeRate();
                theChannel = theMessage.getChannel();
                linkNetwork.getUPBManager().fireLinkEvent(this, UPBLinkEvent.EventCode.LINK_GOTO, theLevel, theMessage.getFadeRate(), theMessage.getDevice());
                for (UPBLinkDevice linkedDevice : linkedDevices) {
                    linkedDevice.getDevice().updateInternalDeviceLevel(theLevel, theFadeRate, theChannel);
                }
                break;
            case FADE_START:
                if (UPBManager.DEBUG_MODE) debug("Got Link START FADE");
                theLevel = theMessage.getLevel();
                theFadeRate = theMessage.getFadeRate();
                theChannel = theMessage.getChannel();
                linkNetwork.getUPBManager().fireLinkEvent(this, UPBLinkEvent.EventCode.LINK_START_FADE, theLevel, theMessage.getFadeRate(), theMessage.getDevice());
                for (UPBLinkDevice linkedDevice : linkedDevices) {
                    linkedDevice.getDevice().updateInternalDeviceLevel(theLevel, theFadeRate, theChannel);
                }
                break;
            case FADE_STOP:
                if (UPBManager.DEBUG_MODE) debug("Got Link STOP FADE");
                linkNetwork.getUPBManager().fireLinkEvent(this, UPBLinkEvent.EventCode.LINK_STOP_FADE, theMessage.getDevice());
                for (UPBLinkDevice linkedDevice : linkedDevices) {
                    linkNetwork.getUPBManager().queueStateRequest(linkedDevice.getDevice());
                }
                break;
            default:
                if (UPBManager.DEBUG_MODE) debug("Got an unhandled report code of " + theMessage.getMsgType().toString());
                break;
        }
    }

    /**
   * Return a description and ID for this link
   *
   * @return name and ID of this link, in printable form
   */
    public String toString() {
        return linkName + " (" + linkID + ")";
    }

    void releaseResources() {
        if (linkNetwork != null) linkNetwork.getUPBManager().removeAllRelatedLinkListeners(this);
        if (linkedDevices != null) {
            for (UPBLinkDevice theLinkedDevice : linkedDevices) {
                theLinkedDevice.releaseResources();
            }
            linkedDevices.clear();
        }
        linkNetwork = null;
        linkID = -1;
        linkName = null;
        linkedDevices = null;
    }
}
