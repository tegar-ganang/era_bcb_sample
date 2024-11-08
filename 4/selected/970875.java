package org.cdp1802.upb;

import static org.cdp1802.upb.UPBConstants.*;

/**
 * An event descring something changing to a UPB device
 *
 * @author gerry
 */
public class UPBDeviceEvent {

    public enum EventCode {

        /** Device state/dim level has changed */
        DEVICE_STATE_CHANGED, /** Devices configuration (id, name, room, options, etc) changed */
        DEVICE_ID_CHANGED, /** Device registers have been updated */
        DEVICE_REGISTERS_CHANGED, /** Device has been associated with a new link */
        LINK_ADDED, /** Device has been dis-associated with a previous link */
        LINK_REMOVED, /** Device has started to fade */
        DEVICE_START_FADE, /** Device fading has stopped */
        DEVICE_STOP_FADE
    }

    ;

    UPBDeviceI eventDevice = null;

    UPBLinkI eventLink = null;

    UPBLinkDevice eventLinkDevice = null;

    EventCode eventCode = null;

    int eventChannel = ALL_CHANNELS;

    int originationIdent = -1;

    UPBDeviceEvent(UPBDeviceI theDevice, EventCode theEventCode, int theChannel) {
        this.eventDevice = theDevice;
        this.eventCode = theEventCode;
        this.eventChannel = theChannel;
    }

    UPBDeviceEvent(UPBDeviceI theDevice, EventCode theEventCode, UPBLinkDevice theLinkDevice) {
        this.eventDevice = theDevice;
        this.eventLinkDevice = theLinkDevice;
        this.eventLink = theLinkDevice.getLink();
        this.eventCode = theEventCode;
        this.eventChannel = theLinkDevice.getChannel();
    }

    /**
   * Get the device associated with this event
   *
   * @return device associated with this event
   */
    public UPBDeviceI getDevice() {
        return eventDevice;
    }

    /**
   * Get the event code describing this event
   *
   * @return event code describing this event
   */
    public EventCode getEventCode() {
        return eventCode;
    }

    /**
   * For LINK_ADDED and LINK_REMOVED, the link the device was added/removed
   * from.  null in other events
   *
   * @return link device added/removed from or null for non-link events
   */
    public UPBLinkI getLink() {
        return eventLink;
    }

    /**
   * For LINK_ADDED/REMOVED, this link/device record associated with
   * the event.  null in other events.
   *
   * <P>The link/device record not only ties a device and link together, but
   * describes what dim level the device should go to and at what fade
   * rate with the link is activated.
   *
   * @return link/devce record or null for non-link events
   */
    public UPBLinkDevice getLinkDevice() {
        return eventLinkDevice;
    }

    /**
   * Get channel this event applies to.
   *
   * @return channel this event applies to
   */
    public int getChannel() {
        return eventChannel;
    }

    void releaseResources() {
        eventDevice = null;
        eventLink = null;
        eventLinkDevice = null;
        eventCode = null;
    }
}
