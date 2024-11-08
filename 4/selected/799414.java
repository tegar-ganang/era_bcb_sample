package gov.sns.xal.smf.impl;

import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.qualify.*;
import gov.sns.ca.*;
import gov.sns.tools.data.*;
import java.util.*;

/**
 * TrimmedQuadrupole is a subclass of Quadrupole that has a trim power supply in addition to a main power supply.
 * @author  tap
 */
public class TrimmedQuadrupole extends Quadrupole {

    public static final String s_strType = "QT";

    public static final String HORIZONTAL_TYPE = "QTH";

    public static final String VERTICAL_TYPE = "QTV";

    /** unique ID for this magnet's trim supply */
    protected String _trimSupplyID;

    static {
        registerType();
    }

    /**
     * Register type for qualification.  These are the types that are common to all instances.  The <code>isKindOf</code> method handles the 
     * type qualification specific to an instance.
     * @see #isKindOf
     */
    private static void registerType() {
        ElementTypeManager typeManager = ElementTypeManager.defaultManager();
        typeManager.registerType(TrimmedQuadrupole.class, s_strType);
        typeManager.registerType(TrimmedQuadrupole.class, "trimmedquad");
    }

    /**
     * Constructor
	 * @param strID this magnet's unique node ID
     */
    public TrimmedQuadrupole(final String strID) {
        super(strID);
    }

    /**
     * Update data from the power supply data adaptor.
     * @param powerSupplyAdaptor The data provider of power supply information.
     */
    @Override
    protected void updatePowerSupplies(final DataAdaptor powerSupplyAdaptor) {
        super.updatePowerSupplies(powerSupplyAdaptor);
        _trimSupplyID = powerSupplyAdaptor.stringValue("trim");
    }

    /**
     * Write data to the power supply data adaptor.
     * @param powerSupplyAdaptor The data sink for the power supply information
     */
    @Override
    protected void writePowerSupplies(final DataAdaptor powerSupplyAdaptor) {
        super.writePowerSupplies(powerSupplyAdaptor);
        powerSupplyAdaptor.setValue("trim", _trimSupplyID);
    }

    /** 
     * Get the channel handles.  Overrides the default method to add handles from the trim power supply.
     * @return The channel handles associated with this node
     */
    @Override
    public Collection<String> getHandles() {
        Collection<String> handles = new HashSet<String>(super.getHandles());
        try {
            handles.addAll(getTrimSupply().getChannelSuite().getHandles());
        } catch (NullPointerException exception) {
            System.err.println("exception getting handles from the trim supply \"" + getTrimSupply() + "\" for trimmed quadrupole: " + getId());
            throw exception;
        }
        return handles;
    }

    /**
     * Get the channel corresponding to the specified handle.  Check the trim supply if the channel suite or main supply does not contain the handle
     * @param handle The handle for the channel to get.
     * @return The channel associated with this node and the specified handle or null if there is no match.
     * @throws gov.sns.xal.smf.NoSuchChannelException if no such channel as specified by the handle is associated with this node.
     */
    @Override
    public Channel getChannel(final String handle) throws NoSuchChannelException {
        try {
            return super.getChannel(handle);
        } catch (NoSuchChannelException exception) {
            return getTrimSupply().getChannelSuite().getChannel(handle);
        }
    }

    /**
     * Get the trim power supply for this magnet.
     * @return The trim power supply for this magnet
     */
    public MagnetTrimSupply getTrimSupply() {
        return getAccelerator().getMagnetTrimSupply(_trimSupplyID);
    }

    /** 
	 * Set the trim power supply field contribution in the magnet.  If cycle enable 
     * is true then the magnet is cycled before the field is set to the specified value.
	 * @param newField is the new field level in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc.
     */
    public void setTrimField(final double newField) throws ConnectionException, PutException {
        getTrimSupply().setField(newField);
    }

    /**
	 * Get the value to which the trim supply's field contribution is set.  Note that this is not the readback.
	 * @return the field setting in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc.
	 */
    public double getTrimFieldSetting() throws ConnectionException, GetException {
        return getTrimSupply().getFieldSetting();
    }

    /**
	 * Get the value to which the field is set including both the main supply and trim supply contributions.  
	 * Note that this is not the readback.
	 * @return the field setting in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc.
	 */
    @Override
    public double getTotalFieldSetting() throws ConnectionException, GetException {
        return getFieldSetting() + getTrimFieldSetting();
    }

    /** Get the trim power supply current in this electromagnet via ca (A) */
    public double getTrimCurrent() throws ConnectionException, GetException {
        return getTrimSupply().getCurrent();
    }

    /** 
	 * set the trim power supply current in the magnet (A)
	 * @param newCurrent is the new current (A)
     */
    public void setTrimCurrent(final double newCurrent) throws ConnectionException, PutException {
        getTrimSupply().setCurrent(newCurrent);
    }

    /**
     * Get the orientation of the magnet as defined by MagnetType.  The orientation
     * of the quad is determined by its type: QTH or QTV
     * @return One of HORIZONTAL or VERTICAL
     */
    @Override
    public int getOrientation() {
        return (_type.equalsIgnoreCase(HORIZONTAL_TYPE)) ? HORIZONTAL : VERTICAL;
    }
}
