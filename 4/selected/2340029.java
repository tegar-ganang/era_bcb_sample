package jhomenet.commons.hw;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import com.jgoodies.binding.beans.Model;

/**
 * This is the superclass of all registered hardware as part of the jHomenet system.
 * It provides general functionality required for all hardware and is a the default
 * implementation of the <code>RegisteredHardware</code> interface.
 * <p>
 * The class extends the JGoodies abstract <code>Model</code> superclass used to
 * minimizes the effort required to provide change support for bound and constrained
 * Bean properties.
 * 
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public abstract class HomenetHardware extends Model implements RegisteredHardware, Serializable {

    /**
	 * Serial version ID information - used for the serialization process.
	 */
    private static final long serialVersionUID = 00001;

    /**
	 * Logging mechanism.
	 */
    private static Logger logger = Logger.getLogger(HomenetHardware.class.getName());

    /**
	 * Hardware ID (used primarily for persistence).
	 */
    private Long id;

    /**
	 * The physical hardware address.
	 */
    protected String hardwareAddr;

    /**
	 * A general description about the hardware. Examples might include Roof
	 * Temperature Sensor or Basement Humidity Sensor or Garage Light Switch.
	 */
    private static final String defaultHardwareSetupDescription = "Default setup";

    protected String hardwareSetupDescription = defaultHardwareSetupDescription;

    public static final String PROPERTYNAME_HWSETUPDESC = "hardwareSetupDescription";

    /**
	 * Define the number of input channels for the sensor.
	 */
    private Integer numChannels;

    /**
	 * List of channels.
	 */
    private List<Channel> channels;

    /**
	 * Channel description property name.
	 */
    public static final String PROPERTYNAME_CHANNELDESC = "channelDescription";

    /**
	 * The hardware application desription. This is a non-editable value and should
	 * be set a single time at object creation. 
	 */
    private final String hardwareAppDescription;

    /**
	 * Default constructor.
	 * 
	 * @param hardwareAddr The physical hardware address
	 * @param hardwareSetupDescription The hardware setup description
	 * @param hardwareappDescription The application hardware description
	 * @param numChannels The number of communication channels
	 */
    public HomenetHardware(String hardwareAddr, String hardwareSetupDescription, String hardwareAppDescription, Integer numChannels) {
        super();
        if (hardwareAppDescription == null) throw new IllegalArgumentException("Hardware application description cannot be null!");
        if (numChannels == null) throw new IllegalArgumentException("Number of I/O channels cannot be null!");
        this.hardwareAppDescription = hardwareAppDescription;
        this.numChannels = numChannels;
        this.channels = new ArrayList<Channel>(numChannels.intValue());
        initializeChannels(numChannels);
        setHardwareAddr(hardwareAddr);
        if (hardwareSetupDescription == null) this.hardwareSetupDescription = ""; else this.hardwareSetupDescription = hardwareSetupDescription;
        initializeHardware();
    }

    /**
	 * Constructor.
	 * 
	 * @param hardwareAppDescription
	 * @param numChannels
	 */
    public HomenetHardware(String hardwareAppDescription, Integer numChannels) {
        this(null, null, hardwareAppDescription, numChannels);
    }

    /**
	 * A constructor used to make an identical copy of the hardware object.
	 * 
	 * @param hardware
	 */
    public HomenetHardware(HomenetHardware hardware) {
        this(hardware.getHardwareAddr(), hardware.getHardwareSetupDescription(), hardware.getAppHardwareDescription(), hardware.getNumChannels());
        for (int channel = 0; channel < hardware.getNumChannels(); channel++) this.setChannelDescription(channel, hardware.getChannelDescription(channel));
    }

    /**
	 * 
	 * @param numChannels
	 */
    private void initializeChannels(Integer numChannels) {
        for (int channel = 0; channel < numChannels; channel++) {
            channels.add(channel, new Channel(channel));
        }
    }

    /**
	 * Initialize the hardware. Classes may override this method in order
	 * to run initialization code.
	 */
    protected void initializeHardware() {
    }

    /**
	 * Set the hardware ID.
	 * 
	 * @param id
	 */
    private void setId(Long id) {
        this.id = id;
    }

    /**
	 * Get the hardware ID.
	 * 
	 * @return
	 */
    public Long getId() {
        return this.id;
    }

    /**
	 * @see jhomenet.commons.hw.RegisteredHardware#getAppHardwareDescription()
	 */
    public final String getAppHardwareDescription() {
        return this.hardwareAppDescription;
    }

    /**
	 * Make a copy of the hardware object.
	 * 
	 * @return A copy of the hardware object
	 */
    public abstract HomenetHardware copy();

    /**
	 * @see jhomenet.commons.hw.RegisteredHardware#addHardwareListener(jhomenet.commons.hw.HardwareListener)
	 */
    public void addHardwareListener(HardwareListener hardwareListener) {
        logger.debug("Attempting to add hardware listener: " + hardwareListener.toString());
        this.addPropertyChangeListener(hardwareListener);
    }

    /**
	 * @see jhomenet.commons.hw.RegisteredHardware#remoteHardwareListener(jhomenet.commons.hw.HardwareListener)
	 */
    public void removeHardwareListener(HardwareListener hardwareListener) {
        logger.debug("Attempting to remove hardware listener: " + hardwareListener.toString());
        this.removePropertyChangeListener(hardwareListener);
    }

    /**
	 * Set the hardware Id.
	 *
	 * @param hardwareAddr
	 */
    private void setHardwareAddr(String hardwareAddr) {
        this.hardwareAddr = hardwareAddr;
    }

    /**
	 * @see jhomenet.commons.hw.Hardware#getHardwareAddr()
	 */
    public String getHardwareAddr() {
        return hardwareAddr;
    }

    /**
	 * @see jhomenet.commons.hw.RegisteredHardware#setHardwareSetupDescription(java.lang.String)
	 */
    public void setHardwareSetupDescription(String desc) {
        logger.debug("Setting hardware setup description to: " + desc);
        String oldValue = getHardwareSetupDescription();
        this.hardwareSetupDescription = desc;
        firePropertyChange(PROPERTYNAME_HWSETUPDESC, oldValue, desc);
    }

    /**
	 * @see jhomenet.commons.hw.RegisteredHardware#getHardwareSetupDescription()
	 */
    public String getHardwareSetupDescription() {
        return hardwareSetupDescription;
    }

    /**
	 * @see jhomenet.commons.hw.RegisteredHardware#getHardwareClassname()
	 */
    public String getHardwareClassname() {
        return this.getClass().getName();
    }

    /**
	 * Get the number of input channels the sensor supports.
	 *
	 * @return The number of input channels the sensor supports
	 */
    public Integer getNumChannels() {
        return numChannels.intValue();
    }

    /**
	 * @see jhomenet.commons.hw.RegisteredHardware#setChannelDescription(java.lang.Integer, java.lang.String)
	 */
    public void setChannelDescription(Integer channel, String description) {
        if (channel < 0 || channel > this.numChannels.intValue()) throw new IllegalArgumentException("Illegal channel value: " + channel);
        logger.debug("Setting channel description: CH-" + channel + ": " + description);
        Channel oldChannelDesc = this.channels.get(channel);
        this.channels.set(channel, new Channel(channel, description));
        firePropertyChange(PROPERTYNAME_CHANNELDESC, oldChannelDesc, this.channels.get(channel));
    }

    /**
	 * @see jhomenet.commons.hw.RegisteredHardware#getChannelDescription(java.lang.Integer)
	 */
    public String getChannelDescription(Integer channel) {
        if (channel < 0 || channel > this.numChannels.intValue()) throw new IllegalArgumentException("Illegal channel value: " + channel);
        if (this.channels.size() == 0) return ""; else return this.channels.get(channel).getDescription();
    }

    /**
	 * @see jhomenet.commons.hw.RegisteredHardware#getChannels()
	 */
    public List<Channel> getChannels() {
        return this.channels;
    }

    /**
	 * 
	 * @param channels
	 */
    private void setChannels(List<Channel> channels) {
        this.channels = channels;
    }

    /**
	 * @see jhomenet.commons.hw.RegisteredHardware#getChannel(java.lang.Integer)
	 */
    public Channel getChannel(Integer channel) {
        return this.channels.get(channel);
    }

    /**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final HomenetHardware other = (HomenetHardware) obj;
        if (hardwareAddr == null) {
            if (other.hardwareAddr != null) return false;
        } else if (!hardwareAddr.equals(other.hardwareAddr)) return false;
        return true;
    }

    /**
	 * @see java.lang.Object#hashCode()
	 */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hardwareAddr == null) ? 0 : hardwareAddr.hashCode());
        return result;
    }

    /**
	 * Return the string representation of the hardware object.
	 * 
	 * @see java.lang.Object#toString()
	 */
    @Override
    public String toString() {
        return "[" + this.getHardwareSetupDescription() + "]";
    }
}
