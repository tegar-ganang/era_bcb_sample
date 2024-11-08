package org.cdp1802.upb;

import static org.cdp1802.upb.UPBConstants.*;

/**
 * This describes the association of a link and a device.  It not only is
 * the record that "connects" the two, but it also describes what level the
 * device should go to when the link is activated and what fade rate to 
 * use to do it.
 *
 * @author gerry
 */
public class UPBLinkDevice {

    UPBDeviceI theDevice = null;

    UPBLinkI theLink = null;

    int theLevel = 100;

    int theFadeRate = DEFAULT_FADE_RATE;

    int theChannel = ALL_CHANNELS;

    public UPBLinkDevice(UPBLinkI theLink, UPBDeviceI theDevice, int theLevel, int theFadeRate, int theChannel) {
        this.theLink = theLink;
        this.theDevice = theDevice;
        this.theLevel = theLevel;
        this.theFadeRate = theFadeRate;
        this.theChannel = theChannel;
    }

    /**
   * Get the device described by this association
   *
   * @return device associated with the link
   */
    public UPBDeviceI getDevice() {
        return theDevice;
    }

    public void setDevice(UPBDeviceI device) {
        theDevice = device;
    }

    /**
   * Get the link described by this association
   *
   * @return link associated with this device
   */
    public UPBLinkI getLink() {
        return theLink;
    }

    /**
   * Get the level the device should go to when the link is
   * activated.
   *
   * @return level to set device when link is activated
   */
    public int getLevel() {
        return theLevel;
    }

    /**
   * Get the rate the device should change to it's new level 
   * when the link is activated.
   *
   * @return fade rate for device when link is activated
   */
    public int getFadeRate() {
        return theFadeRate;
    }

    /**
   * Get the channel this link applies to on the device.
   *
   * @return channel for this link
   */
    public int getChannel() {
        return theChannel;
    }

    /**
   * Get a printable description of this association
   *
   * @return printable associated including device name/id and link name/id
   */
    public String toString() {
        return "Device " + theDevice + " linked to " + theLink;
    }

    public void releaseResources() {
        theDevice = null;
        theLink = null;
    }
}
