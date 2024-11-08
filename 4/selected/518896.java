package org.cdp1802.upb.impl;

import java.util.ArrayList;
import static org.cdp1802.upb.UPBConstants.INVALID_CHANNEL;
import org.cdp1802.upb.UPBDeviceI;
import org.cdp1802.upb.UPBProductI;

/**
 * Describes a specific manufacturer and product
 *
 * @author gerry
 */
public class UPBProduct implements UPBProductI {

    private static ArrayList<UPBProductI> productList = new ArrayList<UPBProductI>();

    private static UPBProduct genericUPBProduct = null;

    static {
        UPBProduct theProduct = null;
        genericUPBProduct = new UPBProduct(0, "Generic Manufacturer", 0, "Generic UPB Device", 0);
        genericUPBProduct.dimmingCapable = false;
        theProduct = new UPBProduct(5, "(HAI) Home Automation, Inc.", 1, "35A00-1 600W Dimming Switch", 2);
        theProduct.dimmingCapable = true;
        theProduct.deviceClass = UPBDimmerDevice.class;
        theProduct.statusQueryable = true;
        theProduct.supportsFading = true;
        productList.add(theProduct);
        theProduct = new UPBProduct(4, "Simply Automated, Inc.", 1, "UML Lamp Module", 3);
        theProduct.dimmingCapable = true;
        theProduct.deviceClass = UPBDimmerDevice.class;
        theProduct.statusQueryable = true;
        theProduct.transmitComponents = 0;
        theProduct.supportsFading = true;
        productList.add(theProduct);
        theProduct = new UPBProduct(4, "Simply Automated, Inc.", 5, "UMA Appliance Module", 3);
        theProduct.dimmingCapable = false;
        theProduct.statusQueryable = true;
        theProduct.transmitComponents = 0;
        theProduct.supportsFading = false;
        productList.add(theProduct);
        theProduct = new UPBProduct(4, "Simply Automated, Inc.", 7, "URD In-Wall Appliance Module", 3);
        theProduct.dimmingCapable = false;
        theProduct.statusQueryable = true;
        theProduct.transmitComponents = 0;
        theProduct.supportsFading = false;
        productList.add(theProduct);
        theProduct = new UPBProduct(4, "Simply Automated, Inc.", 15, "UCT Tabletop Controller", 1);
        theProduct.deviceClass = UPBControllerDevice.class;
        theProduct.statusQueryable = false;
        theProduct.dimmingCapable = false;
        theProduct.channelCount = 0;
        theProduct.primaryChannel = INVALID_CHANNEL;
        theProduct.transmitComponents = 8;
        theProduct.receiveComponents = 8;
        theProduct.supportsFading = false;
        productList.add(theProduct);
        theProduct = new UPBProduct(4, "Simply Automated, Inc.", 29, "US2-40 Series Dimming Switch", 2);
        theProduct.dimmingCapable = true;
        theProduct.deviceClass = UPBDimmerDevice.class;
        theProduct.statusQueryable = true;
        theProduct.supportsFading = true;
        productList.add(theProduct);
        theProduct = new UPBProduct(0, "Unknown OEM", 40, "UMI-32 3-Input / 2-Output Module", 5);
        theProduct.deviceClass = UPBIODevice.class;
        theProduct.dimmingCapable = false;
        theProduct.statusQueryable = true;
        theProduct.transmitComponents = 3;
        theProduct.channelCount = 2;
        theProduct.supportsFading = false;
        productList.add(theProduct);
    }

    /**
   * Given the manufacturer and product ID, return a descriptor for that product
   * or null if nothing matches
   *
   * @param manufacturerID the ID for the product manufacturer
   * @param productID the ID code for the product
   * @return the UPBProduct for this product or null if nothing matches
   */
    public static UPBProductI getProduct(int manufacturerID, int productID) {
        for (UPBProductI theProduct : productList) {
            if (theProduct.getManufacturerID() != manufacturerID) continue;
            if (theProduct.getProductID() != productID) continue;
            return theProduct;
        }
        System.err.println("ATTENTION:: A new device was found that UPB didn't know before!!");
        System.err.println("   Please report the following, along with the name of the devices");
        System.err.println("   manufacture, the devices product # and the name of the product");
        System.err.println("   to gerry@cdp1802.org so UPB can be updated to include this device.\n");
        System.err.println("     Manufacturer Code/ID: " + manufacturerID);
        System.err.println("          Product Code/ID: " + productID);
        return getGenericUPBProduct();
    }

    /**
   * Get the generic UPB product used when no device matches the manufacturer
   * and/or product ID
   *
   * @return generic UPB product
   */
    public static UPBProductI getGenericUPBProduct() {
        return genericUPBProduct;
    }

    int manufacturerID = 0;

    int productID = 0;

    int productKind = 0;

    String manufacturerName = "";

    String productName = "";

    int deviceRegisterSize = 256;

    int channelCount = 1;

    int primaryChannel = 1;

    int transmitComponents = 1;

    int receiveComponents = 16;

    boolean dimmingCapable = false;

    boolean supportsFading = false;

    boolean statusQueryable = false;

    Class<? extends UPBDeviceI> deviceClass = UPBDevice.class;

    UPBProduct(int manufacturerID, String manufacturerName, int productID, String productName, int productKind) {
        this.manufacturerID = manufacturerID;
        this.manufacturerName = manufacturerName;
        this.productID = productID;
        this.productName = productName;
        this.productKind = productKind;
    }

    /**
   * Get the manufacturer ID code for this product
   *
   * <P>Manufacturer IDs are unique
   *
   * @return manufacturer ID for this product
   */
    public int getManufacturerID() {
        return manufacturerID;
    }

    /**
   * Get name of the products manufacturer
   *
   * @return name of the products manufacturer
   */
    public String getManufacturerName() {
        return manufacturerName;
    }

    /**
   * Get the product ID for this product.  Product IDs are
   * unique within a manufacturer ID.
   *
   * @return product ID 
   */
    public int getProductID() {
        return productID;
    }

    /**
   * Get the name of this product
   *
   * @return name of this product
   */
    public String getProductName() {
        return productName;
    }

    /**
   * Get the kind (category) of this product
   * 
   * @return numeric kind of this product
   */
    public int getProductKind() {
        return productKind;
    }

    /**
   * Get the number of device registers this products devices has
   * 
   * @return number of registers for this products devices
   */
    public int getDeviceRegisterSize() {
        return deviceRegisterSize;
    }

    /**
   * Get the number of channels on this products devices.
   *
   * <P>In UPB terminology, a channel is load that can be controlled
   * in a UPB device.  Most UPB devices have a single channel, but some
   * specialty UPB devices can have >1
   *
   * @return number of channels for this products devices.
   */
    public int getChannelCount() {
        return channelCount;
    }

    /**
   * Get the primary channel number commands should apply to when
   * no specific channel is specified.
   *
   * @return primary channel number (-1 for devices with NO channels)
   */
    public int getPrimaryChannel() {
        return primaryChannel;
    }

    /**
   * Get number of transmit components for this products devices.
   *
   * @return transmit components for this product devices.
   */
    public int getTransmitComponentCount() {
        return transmitComponents;
    }

    /**
   * Get number of receive components for this products devices.
   *
   * <P>NOTE: Often (though not always), the number of receive components
   * tells you how many links the device can be attached to (a link
   * being a receive component).  For most light and appliance
   * modules, this is the case.
   *
   * @return number of receive components for this products devices
   */
    public int getReceiveComponentCount() {
        return receiveComponents;
    }

    /**
   * Is this product generally capable of being dimmed?  Not all 
   * products are and even for products that are dimming capable,
   * the dimming function can be turned off for select devices.
   *
   * <P>So to know if a particular device is dimmable, you need to 
   * check the UPBDevice.isDimmable() property
   *
   * @return true if products devices are capable of being dimmed
   */
    public boolean isDimmingCapable() {
        return dimmingCapable;
    }

    /**
   * Most UPB devices can be asked to send their current state
   * out.  However, some cannot (multi-button controllers is
   * the only device like this I've come across).
   *
   * @return true if products devices can be queried for their current state
   */
    public boolean isStatusQueryable() {
        return statusQueryable;
    }

    /**
   * Many UPB devices support the concept of fading -- that is, not immediatly
   * acheiving a given level, but getting there over time.  In most cases, 
   * whether a device supports fading or not doesn't matter, but in cases
   * where a status request is sent to a device, we may not be getting the
   * right response if we ask for that status and the lamp hasn't acheived
   * it's final level.  In those cases, we need to periodically check back
   * until the devices level is stable.
   *
   * @return true if device supports fading, false if all transitions are instantaenous
   */
    public boolean isFadable() {
        return supportsFading;
    }

    /**
   * Get the class used for instances of devices that use this product
   *
   * <P>The base class (UPBDevice) is fine for devices that can just
   * be turned on and off.  However, more support is needed for dimming
   * devices (UPBDimmerDevice), IO modules (UPBIODevice) and control
   * panels (UPBControllerDevice).  All classes descend from UPBDevice.
   *
   * <P>This class is used when importing the UPStart file or doing device
   * discovery to create appropriate devices instances for each device
   *
   * @return class to use for devices of this product
   */
    public Class<? extends UPBDeviceI> getDeviceClass() {
        return deviceClass;
    }

    /**
   * Get a printable summary of this product
   *
   * @return printable summary of this product
   */
    public String toString() {
        return manufacturerName + "[" + manufacturerID + "], " + productName + "[" + productID + "]";
    }
}
