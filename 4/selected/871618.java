package gov.sns.xal.smf.impl;

import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.qualify.*;
import gov.sns.ca.*;
import gov.sns.tools.data.*;
import java.util.*;

/**
 * Electromagnet is the base class representation of an electromagnet.
 *
 * @author  tap
 */
public abstract class Electromagnet extends Magnet {

    /** the node type */
    public static final String s_strType = "emag";

    /** field readback handle */
    public static final String FIELD_RB_HANDLE = "fieldRB";

    /** indicates whether to use the actual field readback or the field setting in the getField() method */
    protected boolean _useFieldReadback;

    /** the ID of this magnet's main power supply */
    protected String mainSupplyId;

    static {
        registerType();
    }

    private static void registerType() {
        ElementTypeManager typeManager = ElementTypeManager.defaultManager();
        typeManager.registerType(Electromagnet.class, s_strType);
        typeManager.registerType(Electromagnet.class, "electromagnet");
    }

    /** Constructor */
    public Electromagnet(final String strId) {
        super(strId);
        _useFieldReadback = true;
    }

    /** 
     * Update the node with data from the provided adaptor.
	 * @param adaptor The data provider
     */
    @Override
    public void update(final DataAdaptor adaptor) throws NumberFormatException {
        super.update(adaptor);
        final DataAdaptor powerSupplyAdaptor = adaptor.childAdaptor("ps");
        if (powerSupplyAdaptor != null) {
            updatePowerSupplies(powerSupplyAdaptor);
        }
    }

    /**
     * Update data from the power supply data adaptor.  Get the main 
     * power supply information.
     * @param powerSupplyAdaptor The data provider of power supply information.
     */
    protected void updatePowerSupplies(final DataAdaptor powerSupplyAdaptor) {
        mainSupplyId = powerSupplyAdaptor.stringValue("main");
    }

    /** 
     * Encode data from the node into the provided adaptor.  Overrides to provide
     * support for power supplies.
     * @param adaptor The data store
     */
    @Override
    public void write(final DataAdaptor adaptor) {
        super.write(adaptor);
        DataAdaptor powerSupplyAdaptor = adaptor.createChild("ps");
        writePowerSupplies(powerSupplyAdaptor);
    }

    /**
     * Write data to the power supply data adaptor.  Put the information about
     * the main power supply into the data adaptor.
     * @param powerSupplyAdaptor The data sink for the power supply information
     */
    protected void writePowerSupplies(final DataAdaptor powerSupplyAdaptor) {
        powerSupplyAdaptor.setValue("main", mainSupplyId);
    }

    /**
	 * Set whether or not to use the field readback in the getField() method.
	 * @param useFieldReadback true to use the field readback and false to use the field setting.
	 */
    public void setUseFieldReadback(final boolean useFieldReadback) {
        _useFieldReadback = useFieldReadback;
    }

    /**
	 * Determines whether the field readback is used in the getField() method.
	 * @return true if the field readback is used in getField() and false if instead the field setting is used.
	 */
    public boolean useFieldReadback() {
        return _useFieldReadback;
    }

    /** 
     * Get the channel handles.  Overrides the default method to add handles from
     * the main power supply.
     * @return The channel handles associated with this node
     */
    @Override
    public Collection<String> getHandles() {
        Collection<String> handles = new HashSet<String>(super.getHandles());
        try {
            handles.addAll(getMainSupply().getChannelSuite().getHandles());
        } catch (NullPointerException exception) {
            System.err.println("exception getting handles from the main supply \"" + getMainSupply() + "\" for electromagnet: " + getId());
            throw exception;
        }
        return handles;
    }

    /**
     * Get the channel corresponding to the specified handle.  Override the
     * inherited method to check the main supply if the channel suite does 
     * not contain the handle.
     * @param handle The handle for the channel to get.
     * @return The channel associated with this node and the specified handle or null if there is no match.
     * @throws gov.sns.xal.smf.NoSuchChannelException if no such channel as specified by the handle is associated with this node.
     */
    @Override
    public Channel getChannel(final String handle) throws NoSuchChannelException {
        try {
            return super.getChannel(handle);
        } catch (NoSuchChannelException exception) {
            return getMainSupply().getChannel(handle);
        }
    }

    /**
     * Get the main power supply for this magnet.
     * @return The main power supply for this magnet
     */
    public MagnetMainSupply getMainSupply() {
        return getAccelerator().getMagnetMainSupply(mainSupplyId);
    }

    /**
     * Set the cycle enable state of the magnet.  If enabled, the magnet will 
     * be cycled when the field is set.
     * @param enable True to enable cycling; false to disable cycling.
     */
    public void setCycleEnable(final boolean enable) throws ConnectionException, PutException {
        getMainSupply().setCycleEnable(enable);
    }

    /** 
     * Gets the cycle state of the magnet.  The magnet may be in one of three 
     * states: cycle is invalid (field changed in reverse direction of initial setting), 
     * cycling in progress or cycle is valid
     * @return One of CYCLE_INVALID, CYCLING or CYCLE_VALID
     */
    public int getCycleState() throws ConnectionException, GetException {
        return getMainSupply().getCycleState();
    }

    /** 
	 * Get the field in this electromagnet via ca.
	 * @return the field in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc. 
	 */
    public double getField() throws ConnectionException, GetException {
        return (_useFieldReadback) ? getFieldReadback() : getTotalFieldSetting();
    }

    /** 
	 * Get the field in this electromagnet via ca.
	 * @return the readback field in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc. 
	 */
    public double getFieldReadback() throws ConnectionException, GetException {
        Channel fieldRBChannel = getAndConnectChannel(FIELD_RB_HANDLE);
        return toFieldFromCA(fieldRBChannel.getValDbl());
    }

    /** Get the integrated field in this electromagnet 
        T-m/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc. */
    public double getFieldInt() throws ConnectionException, GetException {
        return getField() * getEffLength();
    }

    /** Set the main power supply field contribution in the magnet.  If cycle enable 
     * is true then the magnet is cycled before the field is set to the specified value.
     * @param newField is the new field level in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc.
     */
    public void setField(final double newField) throws ConnectionException, PutException {
        getMainSupply().setField(toCAFromField(newField));
    }

    /**
	 * Get the value to which the main power supply's field contribution is set.  Note that this is not the readback.
	 * @return the field setting in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc.
	 */
    public double getFieldSetting() throws ConnectionException, GetException {
        return toFieldFromCA(getMainSupply().getFieldSetting());
    }

    /**
	 * Get the value to which the field is set including both the main supply and possible trim supply contributions.  
	 * Note that this is not the readback.
	 * @return the field setting in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc.
	 */
    public double getTotalFieldSetting() throws ConnectionException, GetException {
        return getFieldSetting();
    }

    /**
	 * Convert the raw channel access value to get the field.
	 * @param rawValue the raw channel value
	 * @return the magnetic field in T/m^(n-1)
	 */
    public final double toFieldFromCA(final double rawValue) {
        return rawValue * getPolarity();
    }

    /**
	 * Convert the field value to a channel access value.
	 * @param field the magnetic field in T/m^(n-1)
	 * @return the channel access value
	 */
    public final double toCAFromField(final double field) {
        return field * getPolarity();
    }

    /** Get the field upper settable limit of the main power supply
        in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc.
    */
    public double upperFieldLimit() throws ConnectionException, GetException {
        final MagnetMainSupply powerSupply = getMainSupply();
        return Math.max(toFieldFromCA(powerSupply.lowerFieldLimit()), toFieldFromCA(powerSupply.upperFieldLimit()));
    }

    /** Get the field lower settable limit of the main power supply
       in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc.
    */
    public double lowerFieldLimit() throws ConnectionException, GetException {
        final MagnetMainSupply powerSupply = getMainSupply();
        return Math.min(toFieldFromCA(powerSupply.lowerFieldLimit()), toFieldFromCA(powerSupply.upperFieldLimit()));
    }

    /** Get the field upper settable limit of the main power supply in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc. */
    public double upperDisplayFieldLimit() throws ConnectionException, GetException {
        final MagnetMainSupply powerSupply = getMainSupply();
        return Math.max(toFieldFromCA(powerSupply.lowerDisplayFieldLimit()), toFieldFromCA(powerSupply.upperDisplayFieldLimit()));
    }

    /** Get the field lower settable limit of the main power supply in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc. */
    public double lowerDisplayFieldLimit() throws ConnectionException, GetException {
        final MagnetMainSupply powerSupply = getMainSupply();
        return Math.min(toFieldFromCA(powerSupply.lowerDisplayFieldLimit()), toFieldFromCA(powerSupply.upperDisplayFieldLimit()));
    }

    /** Get the field upper settable limit of the main power supply in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc. */
    public double upperWarningFieldLimit() throws ConnectionException, GetException {
        final MagnetMainSupply powerSupply = getMainSupply();
        return Math.max(toFieldFromCA(powerSupply.lowerWarningFieldLimit()), toFieldFromCA(powerSupply.upperWarningFieldLimit()));
    }

    /** Get the field lower settable limit of the main power supply in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc. */
    public double lowerWarningFieldLimit() throws ConnectionException, GetException {
        final MagnetMainSupply powerSupply = getMainSupply();
        return Math.min(toFieldFromCA(powerSupply.lowerWarningFieldLimit()), toFieldFromCA(powerSupply.upperWarningFieldLimit()));
    }

    /** Get the field upper settable limit of the main power supply in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc. */
    public double upperAlarmFieldLimit() throws ConnectionException, GetException {
        final MagnetMainSupply powerSupply = getMainSupply();
        return Math.max(toFieldFromCA(powerSupply.lowerAlarmFieldLimit()), toFieldFromCA(powerSupply.upperAlarmFieldLimit()));
    }

    /** Get the field lower settable limit of the main power supply in T/(m^(n-1)), where n = 1 for dipole, 2 for quad, etc. */
    public double lowerAlarmFieldLimit() throws ConnectionException, GetException {
        final MagnetMainSupply powerSupply = getMainSupply();
        return Math.min(toFieldFromCA(powerSupply.lowerAlarmFieldLimit()), toFieldFromCA(powerSupply.upperAlarmFieldLimit()));
    }

    /** Get the main power supply current in this electromagnet via ca (A) */
    public double getCurrent() throws ConnectionException, GetException {
        return getMainSupply().getCurrent();
    }

    /** set the main power supply current in the magnet (A)
       @param newCurrent is the new current (A)
    */
    public void setCurrent(final double newCurrent) throws ConnectionException, PutException {
        getMainSupply().setCurrent(newCurrent);
    }

    /** get the main power supply current lower settable limit (A) */
    public double upperCurrentLimit() throws ConnectionException, GetException {
        return getMainSupply().upperCurrentLimit();
    }

    /** get the main power supply current lower settable limit (A) */
    public double lowerCurrentLimit() throws ConnectionException, GetException {
        return getMainSupply().lowerCurrentLimit();
    }

    /**
     * Since this is an electro-magnet we override the inherited method to 
     * advertise this characteristic.
     * @return false since all Electromagnet instances are not permanent magnets.
     */
    @Override
    public boolean isPermanent() {
        return false;
    }
}
