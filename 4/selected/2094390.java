package org.cdp1802.upb;

import static org.cdp1802.upb.UPBConstants.*;

/**
 * Import tools for UPB Importers
 *
 * @author gerry
 */
public abstract class UPBImporter {

    static final int ERROR_VALUE = -1;

    protected String importerName = "UPB_IMPORT";

    protected UPBManager upbManager = null;

    protected UPBMediaAdapterI upbAdapter = null;

    protected UPBNetworkI importNetwork = null;

    protected boolean deviceChangedMap[] = new boolean[251];

    protected boolean importFoundChange = false;

    protected UPBObjectFactoryI factory = null;

    protected UPBImporter(UPBManager theManager, UPBMediaAdapterI theAdapter) {
        upbManager = theManager;
        upbAdapter = theAdapter;
        factory = upbManager.getObjectFactory();
    }

    protected void debug(String theMessage) {
        upbManager.upbDebug(importerName + ":: " + theMessage);
    }

    protected void warn(String theMessage) {
        upbManager.upbWarn(importerName + ":: " + theMessage);
    }

    protected void error(String theMessage) {
        upbManager.upbError(importerName + ":: " + theMessage);
    }

    protected void markChangeFound() {
        importFoundChange = true;
    }

    public boolean changeWasFound() {
        return importFoundChange;
    }

    protected boolean importPrimaryNetwork(int networkID, int networkPassword) {
        if ((importNetwork = upbManager.getNetworkByID(networkID)) == null) {
            importNetwork = upbManager.getPrimaryNetwork();
            if (importNetwork.getNetworkID() == 0) {
                importNetwork.setNetworkID(networkID);
                markChangeFound();
            } else {
                error("Unable to find a matching network # for network ID " + networkID);
                return false;
            }
        }
        if (importNetwork.getNetworkPassword() != networkPassword) {
            importNetwork.setNetworkPassword(networkPassword);
            markChangeFound();
        }
        return true;
    }

    protected boolean importLink(int linkID, String linkName) {
        boolean linkNew = false;
        boolean linkChanged = false;
        UPBLinkI importLink = null;
        if ((importLink = importNetwork.getLinkByID(linkID)) == null) {
            importLink = factory.createLink(importNetwork, linkID);
            linkChanged = true;
            linkNew = true;
        }
        if (!importLink.getLinkName().equals(linkName)) {
            importLink.setLinkName(linkName);
            linkChanged = true;
        }
        if (linkNew) importNetwork.addLink(importLink);
        if (linkChanged) markChangeFound();
        if (!linkNew && linkChanged) upbManager.fireLinkEvent(new UPBLinkEvent(importLink, UPBLinkEvent.EventCode.LINK_ID_CHANGED));
        importLink.removeAllDevices();
        return true;
    }

    protected boolean importDevice(int deviceID, String deviceName, int manufacturerID, int productID, int firmwareVersion, int channelCount, int transmitComponentCount, int receiveComponentCount, String roomName, boolean xmitsLinks) {
        boolean deviceNew = false;
        boolean deviceChanged = false;
        boolean roomChanged = false;
        UPBRoomI oldRoom = null;
        UPBDeviceI importDevice = null;
        UPBProductI importProduct = factory.lookupProduct(manufacturerID, productID);
        if ((importDevice = importNetwork.getDeviceByID(deviceID)) != null) {
            if (importDevice.getClass() != importProduct.getDeviceClass()) {
                if (UPBManager.DEBUG_MODE) debug("Changing device class for device ID " + deviceID + " from " + importDevice.getClass().getName() + " to " + importProduct.getDeviceClass().getName());
                try {
                    UPBDeviceI replacementDevice = (UPBDeviceI) importProduct.getDeviceClass().newInstance();
                    replacementDevice.setDeviceInfo(importNetwork, importProduct, deviceID);
                    replacementDevice.copyFrom(importDevice);
                    importNetwork.addDevice(replacementDevice);
                    for (UPBManager.QualifiedDeviceListener theListener : upbManager.deviceListenerList) {
                        if (theListener.theDevice == importDevice) theListener.theDevice = replacementDevice;
                    }
                    importDevice.releaseResources();
                    importDevice = replacementDevice;
                    deviceChanged = true;
                } catch (Throwable anyError) {
                    error("Unable to create tailored device instance -- " + anyError.getMessage());
                    anyError.printStackTrace();
                    return false;
                }
            }
        } else {
            try {
                importDevice = (UPBDeviceI) importProduct.getDeviceClass().newInstance();
                importDevice.setDeviceInfo(importNetwork, importProduct, deviceID);
                deviceNew = true;
                deviceChanged = true;
            } catch (Throwable anyError) {
                error("Unable to create tailored device instance -- " + anyError.getMessage());
                anyError.printStackTrace();
                return false;
            }
        }
        if (importDevice.getProductInfo() != importProduct) {
            importDevice.setDeviceInfo(importNetwork, importProduct, deviceID);
            deviceChanged = true;
        }
        if (importDevice.getFirmwareVersion() != firmwareVersion) {
            importDevice.setFirmwareVersion(firmwareVersion);
            deviceChanged = true;
        }
        if (channelCount != importDevice.getChannelCount()) {
            if (UPBManager.DEBUG_MODE) debug("Channel Count mismatch on deviceID " + deviceID + " between import file (" + channelCount + ") and device (" + importDevice.getChannelCount() + ") -- updating device");
            importDevice.setChannelCount(channelCount);
        }
        if (transmitComponentCount != importProduct.getTransmitComponentCount()) {
            if (UPBManager.DEBUG_MODE) debug("Transmit Component Count mismatch on deviceID " + deviceID + " between import file (" + transmitComponentCount + ") and product (" + importProduct.getTransmitComponentCount() + ")");
        }
        if (receiveComponentCount != importDevice.getReceiveComponentCount()) {
            if (UPBManager.DEBUG_MODE) debug("Receive Component Count mismatch on deviceID " + deviceID + " between import file (" + receiveComponentCount + ") and device (" + importDevice.getReceiveComponentCount() + ")");
            importDevice.setReceiveComponentCount(receiveComponentCount);
        }
        UPBRoomI importRoom = null;
        if ((importRoom = importNetwork.getRoomNamed(roomName)) == null) {
            importRoom = factory.createRoom(importNetwork, roomName);
            importNetwork.addRoom(importRoom);
        }
        if (importDevice.getRoom() != importRoom) {
            roomChanged = true;
            if (importDevice.getRoom() != null) {
                oldRoom = importDevice.getRoom();
                importDevice.getRoom().getDevices().remove(importDevice);
            }
            importDevice.setRoom(importRoom);
            importDevice.getRoom().getDevices().add(importDevice);
            deviceChanged = true;
        }
        if (!importDevice.getDeviceName().equals(deviceName)) {
            importDevice.setDeviceName(deviceName);
            deviceChanged = true;
        }
        if (xmitsLinks != importDevice.doesTransmitsLinks()) {
            importDevice.setTransmitsLinks(xmitsLinks);
            deviceChanged = true;
        }
        if (deviceNew) importNetwork.addDevice(importDevice);
        if (deviceChanged) deviceChangedMap[deviceID] = true;
        if (deviceChanged) markChangeFound();
        if (!deviceNew && deviceChanged) upbManager.fireDeviceEvent(new UPBDeviceEvent(importDevice, org.cdp1802.upb.UPBDeviceEvent.EventCode.DEVICE_ID_CHANGED, ALL_CHANNELS));
        if (roomChanged) {
            if (oldRoom != null) {
                upbManager.fireRoomEvent(new UPBRoomEvent(oldRoom, UPBRoomEvent.EventCode.DEVICE_REMOVED, importDevice));
            }
            upbManager.fireRoomEvent(new UPBRoomEvent(importDevice.getRoom(), UPBRoomEvent.EventCode.DEVICE_ADDED, importDevice));
        }
        return true;
    }

    protected boolean importChannelInfo(int deviceID, int theChannel, boolean isDimmable, int defaultFadeRate) {
        boolean deviceChanged = false;
        UPBDeviceI importDevice = importNetwork.getDeviceByID(deviceID);
        if (importDevice == null) {
            error("Channel record references device ID " + deviceID + " which is an unknown device");
            return false;
        }
        if (importDevice.isDimmable(theChannel) != isDimmable) {
            importDevice.setDimmable(isDimmable, theChannel);
            deviceChanged = true;
        }
        if (importDevice instanceof UPBDimmableI) {
            UPBDimmableI dimDevice = (UPBDimmableI) importDevice;
            if (dimDevice.getDefaultFadeRate(theChannel) != defaultFadeRate) {
                dimDevice.setDefaultFadeRate(defaultFadeRate, theChannel);
                deviceChanged = true;
            }
        }
        if (deviceChanged) markChangeFound();
        if (deviceChanged) upbManager.fireDeviceEvent(new UPBDeviceEvent(importDevice, org.cdp1802.upb.UPBDeviceEvent.EventCode.DEVICE_ID_CHANGED, theChannel));
        return true;
    }

    protected boolean importDeviceLink(int deviceID, int linkID, int theChannel, int theLevel, int fadeRate) {
        UPBDeviceI importDevice = importNetwork.getDeviceByID(deviceID);
        UPBLinkI importLink = importNetwork.getLinkByID(linkID);
        if (importDevice == null) {
            error("Link record references device ID " + deviceID + " which is an unknown device");
            return false;
        }
        if (importLink == null) {
            error("Link record references link ID " + linkID + " which is an unknown link");
            return false;
        }
        if ((theChannel > 0) && (importDevice instanceof UPBDimmableI)) {
            if (fadeRate == ((UPBDimmableI) importDevice).getDefaultFadeRate(theChannel)) fadeRate = DEFAULT_FADE_RATE;
        }
        UPBLinkDevice linkedDevice = importLink.getLinkedDeviceById(importDevice.getDeviceID(), theChannel);
        if (linkedDevice != null) {
            if ((linkedDevice.getLevel() == theLevel) && (linkedDevice.getFadeRate() == fadeRate)) return true;
            linkedDevice.theLevel = theLevel;
            linkedDevice.theFadeRate = fadeRate;
            markChangeFound();
            upbManager.fireLinkEvent(new UPBLinkEvent(importLink, UPBLinkEvent.EventCode.LINK_DEVICE_CHANGED, linkedDevice));
            return true;
        }
        markChangeFound();
        importLink.addDevice(importDevice, theLevel, fadeRate, ALL_CHANNELS);
        return true;
    }

    protected void markImportStarting() {
        if (upbManager.importInProgress) return;
        importFoundChange = false;
        for (int deviceIndex = 0; deviceIndex < deviceChangedMap.length; deviceIndex++) {
            deviceChangedMap[deviceIndex] = false;
        }
        upbManager.importInProgress = true;
        upbManager.fireManagerEvent(new UPBManagerEvent(UPBManagerEvent.EventCode.IMPORT_STARTED));
    }

    protected void markImportFailed() {
        if (!upbManager.importInProgress) return;
        upbManager.importInProgress = false;
        upbManager.fireManagerEvent(new UPBManagerEvent(UPBManagerEvent.EventCode.IMPORT_FAILED));
    }

    protected void markImportComplete() {
        if (!upbManager.importInProgress) return;
        for (UPBDeviceI theDevice : importNetwork.getDevices()) {
            if (theDevice == null) continue;
            UPBProductI upbProduct = null;
            if (deviceChangedMap[theDevice.getDeviceID()] && upbProduct.isStatusQueryable()) {
                upbManager.queueStateRequest(theDevice);
            }
        }
        upbManager.importInProgress = false;
        upbManager.fireManagerEvent(new UPBManagerEvent(UPBManagerEvent.EventCode.IMPORT_COMPLETE));
    }

    /**
   * Start the import process based on settings installed into
   * this importer.
   *
   * If the import succeeds, true is returned
   *
   * @return true if import succeeds
   */
    public abstract boolean startImport();
}
