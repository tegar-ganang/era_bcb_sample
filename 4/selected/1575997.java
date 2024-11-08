package jmri.jmrit.symbolicprog;

import java.util.Vector;
import java.awt.Component;
import javax.swing.*;

/**
 * Represents a single Variable value; abstract base class.
 *
 * <p>The "changed" parameter (non-bound, accessed via isChanged)
 * indicates whether a "write changes" or "read changes" operation
 * should handle this object.
 *
 * @author   Bob Jacobsen   Copyright (C) 2001, 2002, 2003, 2004, 2005
 * @author   Howard G. Penny Copyright (C) 2005
 * @version  $Revision: 1.39 $
 */
public abstract class VariableValue extends AbstractValue implements java.beans.PropertyChangeListener {

    private String _label;

    private String _item;

    private String _cvName;

    protected Vector<CvValue> _cvVector;

    protected JLabel _status = null;

    protected String _tooltipText = null;

    public abstract Component getCommonRep();

    public abstract Component getNewRep(String format);

    /**
     * @return String that can (usually) be interpreted as an integer
     */
    public abstract String getValueString();

    /**
     * @return Value as a native-form Object
     */
    public abstract Object getValueObject();

    /**
     * @return User-desired value, which may or may not be an integer
     */
    public String getTextValue() {
        return getValueString();
    }

    /**
     * Set the value from a single number.
     *
     * In some cases, e.g. speed tables, this will result in 
     * complex behavior, where setIntValue(getIntValue()) results 
     * in something unexpected.
     */
    public abstract void setIntValue(int i);

    /**
     * Get the value as a single number.
     *
     * In some cases, e.g. speed tables, this will result in 
     * complex behavior, where setIntValue(getIntValue()) results 
     * in something unexpected.
     */
    public abstract int getIntValue();

    void updatedTextField() {
        log.error("unexpected use of updatedTextField()", new Exception("traceback"));
    }

    /**
     * Always read the contents of this Variable
     */
    public abstract void readAll();

    /**
     * Always write the contents of this Variable
     */
    public abstract void writeAll();

    /**
     * Confirm the contents of this Variable
     */
    public void confirmAll() {
        log.error("should never execute this");
    }

    /**
     * Read the contents of this Variable if it's in a state
     * that indicates it was "changed"
     * @see #isChanged
     */
    public abstract void readChanges();

    /**
     * Write the contents of this Variable if it's in a state
     * that indicates it was "changed"
     * @see #isChanged
     */
    public abstract void writeChanges();

    /**
     * Determine whether this Variable is "changed", so that
     * "read changes" and "write changes" will act on it.
     * @see #considerChanged
     */
    public abstract boolean isChanged();

    /**
     * Default implementation for subclasses to tell if a CV meets a common definition
     * of "changed".  This implementation will only
     * consider a variable to be changed if the underlying CV(s) state is
     * EDITTED, e.g. if the CV(s) has been manually editted.
     * @param c CV to be examined
     * @return true if to be considered changed
     */
    public static boolean considerChanged(CvValue c) {
        int state = c.getState();
        if (state == CvValue.EDITED || state == CvValue.UNKNOWN) {
            return true;
        } else {
            return false;
        }
    }

    public abstract void propertyChange(java.beans.PropertyChangeEvent e);

    public abstract void dispose();

    public abstract Object rangeVal();

    public VariableValue(String label, String comment, String cvName, boolean readOnly, boolean infoOnly, boolean writeOnly, boolean opsOnly, int cvNum, String mask, Vector<CvValue> v, JLabel status, String item) {
        _label = label;
        _comment = comment;
        _cvName = cvName;
        _readOnly = readOnly;
        _infoOnly = infoOnly;
        _writeOnly = writeOnly;
        _opsOnly = opsOnly;
        _cvNum = cvNum;
        _mask = mask;
        _cvVector = v;
        _status = status;
        _item = item;
    }

    /**
     * Create a null object.  Normally only used for tests and to pre-load classes.
     */
    protected VariableValue() {
    }

    public String label() {
        return _label;
    }

    public String item() {
        return _item;
    }

    public String cvName() {
        return _cvName;
    }

    /**
     * Set tooltip text to be used by both the "value" and
     * representations of this Variable.
     * <P>This is expected to be overridden in subclasses to
     * change their internal info.
     * @see #updateRepresentation
     * @param t
     */
    public void setToolTipText(String t) {
        _tooltipText = t;
    }

    /**
     * Add the proper tooltip text to a graphical rep
     * before returning it, sets the visibility
     * @param c
     */
    protected JComponent updateRepresentation(JComponent c) {
        c.setToolTipText(_tooltipText);
        c.setVisible(getAvailable());
        return c;
    }

    public String getComment() {
        return _comment;
    }

    private String _comment;

    public boolean getReadOnly() {
        return _readOnly;
    }

    private boolean _readOnly;

    public boolean getInfoOnly() {
        return _infoOnly;
    }

    private boolean _infoOnly;

    public boolean getWriteOnly() {
        return _writeOnly;
    }

    private boolean _writeOnly;

    public boolean getOpsOnly() {
        return _opsOnly;
    }

    private boolean _opsOnly;

    public int getCvNum() {
        return _cvNum;
    }

    private int _cvNum;

    public String getMask() {
        return _mask;
    }

    private String _mask;

    public int getState() {
        return _state;
    }

    public void setState(int state) {
        switch(state) {
            case UNKNOWN:
                setColor(COLOR_UNKNOWN);
                break;
            case EDITED:
                setColor(COLOR_EDITED);
                break;
            case READ:
                setColor(COLOR_READ);
                break;
            case STORED:
                setColor(COLOR_STORED);
                break;
            case FROMFILE:
                setColor(COLOR_FROMFILE);
                break;
            case SAME:
                setColor(COLOR_SAME);
                break;
            case DIFF:
                setColor(COLOR_DIFF);
                break;
            default:
                log.error("Inconsistent state: " + _state);
        }
        if (_state != state || _state == UNKNOWN) prop.firePropertyChange("State", Integer.valueOf(_state), Integer.valueOf(state));
        _state = state;
    }

    private int _state = UNKNOWN;

    /**
     * Simple implementation for the case of a single CV. Intended
     * to be sufficient for many subclasses.
     */
    public void setToRead(boolean state) {
        if (getInfoOnly() || getWriteOnly() || !getAvailable()) state = false;
        _cvVector.elementAt(getCvNum()).setToRead(state);
    }

    /**
     * Simple implementation for the case of a single CV. Intended
     * to be sufficient for many subclasses.
     */
    public boolean isToRead() {
        return _cvVector.elementAt(getCvNum()).isToRead();
    }

    /**
     * Simple implementation for the case of a single CV. Intended
     * to be sufficient for many subclasses.
     */
    public void setToWrite(boolean state) {
        if (getInfoOnly() || getReadOnly() || !getAvailable()) {
            state = false;
        }
        if (log.isDebugEnabled()) log.debug("setToWrite(" + state + ") for " + label() + " via CvNum " + getCvNum());
        _cvVector.elementAt(getCvNum()).setToWrite(state);
    }

    /**
     * Simple implementation for the case of a single CV. Intended
     * to be sufficient for many subclasses.
     */
    public boolean isToWrite() {
        return _cvVector.elementAt(getCvNum()).isToWrite();
    }

    /**
     * Propogate a state change here to the CVs that are related, which will
     * in turn propagate back to here
     */
    public abstract void setCvState(int state);

    /**
     *  A variable is busy during read, write operations
     */
    public boolean isBusy() {
        return _busy;
    }

    protected void setBusy(boolean newBusy) {
        boolean oldBusy = _busy;
        _busy = newBusy;
        if (newBusy != oldBusy) prop.firePropertyChange("Busy", Boolean.valueOf(oldBusy), Boolean.valueOf(newBusy));
    }

    private boolean _busy = false;

    protected int maskVal(String maskString) {
        int mask = 0;
        for (int i = 0; i < 8; i++) {
            mask = mask << 1;
            try {
                if (maskString.charAt(i) == 'V') {
                    mask = mask + 1;
                }
            } catch (StringIndexOutOfBoundsException e) {
                log.error("mask /" + maskString + "/ could not be handled for variable " + label());
            }
        }
        return mask;
    }

    /**
     * Find number of places to shift a value left to align
     * if with a mask.  For example, as mask of "XXVVVXXX"
     * means that the value 5 needs to be shifted left 3 places
     * before being masked and stored as XX101XXX
     */
    protected int offsetVal(String maskString) {
        int offset = 0;
        for (int i = 0; i < 8; i++) {
            if (maskString.charAt(i) == 'V') {
                offset = 7 - i;
            }
        }
        return offset;
    }

    /**
     *
     * @param oldCv Value of the CV before this update is applied
     * @param newVal Value for this variable (e.g. not the CV value)
     * @param maskString The bit mask for this variable in character form
     * @return int new value for the CV
     */
    protected int newValue(int oldCv, int newVal, String maskString) {
        int mask = maskVal(maskString);
        int offset = offsetVal(maskString);
        return (oldCv & ~mask) + ((newVal << offset) & mask);
    }

    /**
     * Provide access to CVs referenced by this operation
     */
    public abstract CvValue[] usesCVs();

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(VariableValue.class.getName());
}
