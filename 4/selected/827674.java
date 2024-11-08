package net.sourceforge.xml1wire;

import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.OneWireException;
import java.util.Enumeration;
import java.util.Vector;

/**
 * The OWDevice class is the class from which all default 1-wire devices are derived.
 *
 */
public class OWDevice {

    /**
 * Returns the cluster in which the device lives
 */
    public String getCluster() {
        return this.clusterDescription;
    }

    /**
 * Returns the text description of the device.
 *
 * @return Its textual description.
 */
    public String getDescription() {
        return Description;
    }

    /**
 * Returns the device's channel mask byte.
 *
 * @return The channel mask.
 */
    public byte getChannelMask() {
        return ChannelMask;
    }

    /**
 * Returns an enumeration of the 1-wire addresses of the 1-wire components in the device.
 *
 * @return An enumeration of net addresses as {@link String}s.
 */
    public Enumeration getNetAddresses() {
        return OWNetAddresses.elements();
    }

    /**
 * Returns a single 1-wire address as a string.
 *
 * Note that some devices may be represented by more than one address; this merely
 * is a convenience that returns the first address.
 *
 * @return The first address.
 * @see #getNetAddresses
 */
    public String getNetAddress() {
        return (String) OWNetAddresses.elementAt(0);
    }

    /**
 * Returns an enumeration of the OneWireContainer objects for the 1-wire components
 * in the device.
 *
 * @return An enumeration of {@link OneWireContainers}.
 */
    public Enumeration getContainers() {
        return OWContainers.elements();
    }

    /**
 * Returns a single 1-wire container from the device.
 *
 * Note that some devices may be served by more than one container; this merely
 * is a convenience that returns the first container.
 *
 * @return The container.
 * @see #getContainers
 */
    public OneWireContainer getContainer() {
        return (OneWireContainer) OWContainers.elementAt(0);
    }

    /**
 * Returns true if the device is currently present on the network, false otherwise.
 *
 * @return True if the device is present, false otherwise.
 */
    public boolean isPresent() throws OneWireException {
        return ((OneWireContainer) OWContainers.elementAt(0)).isPresent();
    }

    /**
 * Sets the description of the cluster in which the device lives.
 */
    public void setCluster(String clusterName) {
        this.clusterDescription = clusterName;
    }

    /**
 * The text description of the device.
 */
    protected String Description;

    /**
 *The channel mask fo raccessing the device.
 */
    protected byte ChannelMask;

    /**
 * Collection of 1-wire addresses of the components in the device.
 */
    protected Vector OWNetAddresses;

    /**
 * Collection of the OneWireContainer objects for the components making up the device.
 */
    protected Vector OWContainers;

    /**
   * Description of the cluster containing the device
   */
    protected String clusterDescription;
}
