package jmri.jmrit.symbolicprog;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.text.Document;

/**
 * Extends VariableValue to represent a variable
 * split across two CVs.
 * <P>The mask represents the part of the value that's
 * present in the first CV; higher-order bits are loaded to the
 * second CV.
 * <P>The original use is for addresses of stationary (accessory)
 * <P>Factor and Offset are applied when going <i>to</i> value
 * of the variable <i>to</> the CV values:
 *<PRE>
 Value to put in CVs = ((value in text field) - Offset)/Factor
 Value to put in text field = ((value in CVs) * Factor) + Offset
 *</PRE>
 * decoders.
 * @author			Bob Jacobsen   Copyright (C) 2002, 2003, 2004
 * @version			$Revision: 1.29 $
 *
 */
public class SplitVariableValue extends VariableValue implements ActionListener, PropertyChangeListener, FocusListener {

    public SplitVariableValue(String name, String comment, String cvName, boolean readOnly, boolean infoOnly, boolean writeOnly, boolean opsOnly, int cvNum, String mask, int minVal, int maxVal, Vector<CvValue> v, JLabel status, String stdname, int pSecondCV, int pFactor, int pOffset, String uppermask) {
        super(name, comment, cvName, readOnly, infoOnly, writeOnly, opsOnly, cvNum, mask, v, status, stdname);
        _maxVal = maxVal;
        _minVal = minVal;
        _value = new JTextField("0", 5);
        _defaultColor = _value.getBackground();
        _value.setBackground(COLOR_UNKNOWN);
        mFactor = pFactor;
        mOffset = pOffset;
        _value.addActionListener(this);
        _value.addFocusListener(this);
        mSecondCV = pSecondCV;
        lowerbitmask = maskVal(mask);
        lowerbitoffset = offsetVal(mask);
        upperbitmask = maskVal(uppermask);
        upperbitoffset = offsetVal(uppermask);
        String t = mask;
        while (t.length() > 0) {
            if (!t.startsWith("V")) upperbitoffset++;
            t = t.substring(1);
        }
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " upper mask " + uppermask + " had offsetVal=" + offsetVal(uppermask) + " so upperbitoffset=" + upperbitoffset);
        CvValue cv = (_cvVector.elementAt(getCvNum()));
        cv.addPropertyChangeListener(this);
        cv.setState(CvValue.FROMFILE);
        CvValue cv1 = (_cvVector.elementAt(getSecondCvNum()));
        cv1.addPropertyChangeListener(this);
        cv1.setState(CvValue.FROMFILE);
    }

    public CvValue[] usesCVs() {
        return new CvValue[] { _cvVector.elementAt(getCvNum()), _cvVector.elementAt(getSecondCvNum()) };
    }

    int mSecondCV;

    int mFactor;

    int mOffset;

    public int getSecondCvNum() {
        return mSecondCV;
    }

    int lowerbitmask;

    int lowerbitoffset;

    int upperbitmask;

    int upperbitoffset;

    public void setToolTipText(String t) {
        super.setToolTipText(t);
        _value.setToolTipText(t);
    }

    int _maxVal;

    int _minVal;

    public Object rangeVal() {
        return "Split value";
    }

    String oldContents = "";

    void enterField() {
        oldContents = _value.getText();
    }

    void exitField() {
        if (_value != null && !oldContents.equals(_value.getText())) {
            int newVal = ((Integer.valueOf(_value.getText()).intValue()) - mOffset) / mFactor;
            int oldVal = ((Integer.valueOf(oldContents).intValue()) - mOffset) / mFactor;
            updatedTextField();
            prop.firePropertyChange("Value", Integer.valueOf(oldVal), Integer.valueOf(newVal));
        }
    }

    void updatedTextField() {
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " enter updatedTextField in SplitVal");
        CvValue cv1 = _cvVector.elementAt(getCvNum());
        CvValue cv2 = _cvVector.elementAt(getSecondCvNum());
        int newEntry;
        try {
            newEntry = Integer.valueOf(_value.getText()).intValue();
        } catch (java.lang.NumberFormatException ex) {
            newEntry = 0;
        }
        int newVal = (newEntry - mOffset) / mFactor;
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " lo cv was " + cv1.getValue() + " mask=" + lowerbitmask + " offset=" + lowerbitoffset);
        int newCv1 = ((newVal << lowerbitoffset) & lowerbitmask) | (~lowerbitmask & cv1.getValue());
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " hi cv was " + cv2.getValue() + " mask=" + upperbitmask + " offset=" + upperbitoffset);
        int newCv2 = (((newVal << upperbitoffset) >> 8) & upperbitmask) | (~upperbitmask & cv2.getValue());
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " new value " + newVal + " gives first=" + newCv1 + " second=" + newCv2);
        cv1.setValue(newCv1);
        cv2.setValue(newCv2);
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " exit updatedTextField");
    }

    /** ActionListener implementations */
    public void actionPerformed(ActionEvent e) {
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " actionPerformed");
        int newVal = ((Integer.valueOf(_value.getText()).intValue()) - mOffset) / mFactor;
        updatedTextField();
        prop.firePropertyChange("Value", null, Integer.valueOf(newVal));
    }

    /** FocusListener implementations */
    public void focusGained(FocusEvent e) {
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " focusGained");
        enterField();
    }

    public void focusLost(FocusEvent e) {
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " focusLost");
        exitField();
    }

    public String getValueString() {
        return _value.getText();
    }

    public void setIntValue(int i) {
        setValue(i);
    }

    public int getIntValue() {
        return ((Integer.valueOf(_value.getText()).intValue()) - mOffset) / mFactor;
    }

    public Object getValueObject() {
        return Integer.valueOf(_value.getText());
    }

    public Component getCommonRep() {
        if (getReadOnly()) {
            JLabel r = new JLabel(_value.getText());
            updateRepresentation(r);
            return r;
        } else return _value;
    }

    public void setValue(int value) {
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " enter setValue " + value);
        int oldVal;
        try {
            oldVal = (Integer.valueOf(_value.getText()).intValue() - mOffset) / mFactor;
        } catch (java.lang.NumberFormatException ex) {
            oldVal = -999;
        }
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " setValue with new value " + value + " old value " + oldVal);
        _value.setText(String.valueOf(value * mFactor + mOffset));
        if (oldVal != value || getState() == VariableValue.UNKNOWN) actionPerformed(null);
        prop.firePropertyChange("Value", Integer.valueOf(oldVal), Integer.valueOf(value * mFactor + mOffset));
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " exit setValue " + value);
    }

    Color _defaultColor;

    void setColor(Color c) {
        if (c != null) _value.setBackground(c); else _value.setBackground(_defaultColor);
    }

    public Component getNewRep(String format) {
        JTextField value = new VarTextField(_value.getDocument(), _value.getText(), 5, this);
        if (getReadOnly() || getInfoOnly()) {
            value.setEditable(false);
        }
        reps.add(value);
        return updateRepresentation(value);
    }

    public void setAvailable(boolean a) {
        _value.setVisible(a);
        for (Component c : reps) c.setVisible(a);
        super.setAvailable(a);
    }

    java.util.List<Component> reps = new java.util.ArrayList<Component>();

    private int _progState = 0;

    private static final int IDLE = 0;

    private static final int READING_FIRST = 1;

    private static final int READING_SECOND = 2;

    private static final int WRITING_FIRST = 3;

    private static final int WRITING_SECOND = 4;

    /**
     * Notify the connected CVs of a state change from above
     * @param state
     */
    public void setCvState(int state) {
        (_cvVector.elementAt(getCvNum())).setState(state);
    }

    public boolean isChanged() {
        CvValue cv1 = (_cvVector.elementAt(getCvNum()));
        CvValue cv2 = (_cvVector.elementAt(getSecondCvNum()));
        return (considerChanged(cv1) || considerChanged(cv2));
    }

    public void readChanges() {
        if (isChanged()) readAll();
    }

    public void writeChanges() {
        if (isChanged()) writeAll();
    }

    public void readAll() {
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " splitval read() invoked");
        setToRead(false);
        setBusy(true);
        if (_progState != IDLE) log.warn("CV " + getCvNum() + "," + getSecondCvNum() + " programming state " + _progState + ", not IDLE, in read()");
        _progState = READING_FIRST;
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " invoke CV read");
        (_cvVector.elementAt(getCvNum())).read(_status);
    }

    public void writeAll() {
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " write() invoked");
        if (getReadOnly()) log.error("CV " + getCvNum() + "," + getSecondCvNum() + " unexpected write operation when readOnly is set");
        setToWrite(false);
        setBusy(true);
        if (_progState != IDLE) log.warn("CV " + getCvNum() + "," + getSecondCvNum() + " Programming state " + _progState + ", not IDLE, in write()");
        _progState = WRITING_FIRST;
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " invoke CV write");
        (_cvVector.elementAt(getCvNum())).write(_status);
    }

    public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " property changed event - name: " + e.getPropertyName());
        if (e.getPropertyName().equals("Busy") && ((Boolean) e.getNewValue()).equals(Boolean.FALSE)) {
            switch(_progState) {
                case IDLE:
                    if (log.isDebugEnabled()) log.error("CV " + getCvNum() + "," + getSecondCvNum() + " Busy goes false with state IDLE");
                    return;
                case READING_FIRST:
                    if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " Busy goes false with state READING_FIRST");
                    if (getState() != UNKNOWN) {
                        _progState = READING_SECOND;
                        (_cvVector.elementAt(getSecondCvNum())).read(_status);
                    } else {
                        if (log.isDebugEnabled()) log.debug("First read failed, abort second read");
                        _progState = IDLE;
                        setBusy(false);
                    }
                    return;
                case READING_SECOND:
                    if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " Busy goes false with state READING_SECOND");
                    _progState = IDLE;
                    (_cvVector.elementAt(getCvNum())).setState(READ);
                    (_cvVector.elementAt(getSecondCvNum())).setState(READ);
                    setBusy(false);
                    return;
                case WRITING_FIRST:
                    if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " Busy goes false with state WRITING_FIRST");
                    _progState = WRITING_SECOND;
                    (_cvVector.elementAt(getSecondCvNum())).write(_status);
                    return;
                case WRITING_SECOND:
                    if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " Busy goes false with state WRITING_SECOND");
                    _progState = IDLE;
                    super.setState(STORED);
                    setBusy(false);
                    return;
                default:
                    log.error("CV " + getCvNum() + "," + getSecondCvNum() + " Unexpected state found: " + _progState);
                    _progState = IDLE;
                    return;
            }
        } else if (e.getPropertyName().equals("State")) {
            CvValue cv = _cvVector.elementAt(getCvNum());
            if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " State changed to " + cv.getState());
            setState(cv.getState());
        } else if (e.getPropertyName().equals("Value")) {
            CvValue cv0 = _cvVector.elementAt(getCvNum());
            CvValue cv1 = _cvVector.elementAt(getSecondCvNum());
            int newVal = ((cv0.getValue() & lowerbitmask) >> lowerbitoffset) + (((cv1.getValue() & upperbitmask) * 256) >> upperbitoffset);
            if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " set value to " + newVal + " based on cv0=" + cv0.getValue() + " cv1=" + cv1.getValue());
            setValue(newVal);
            if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " in property change after setValue call, cv0=" + cv0.getValue() + " cv1=" + cv1.getValue());
            setState(cv0.getState());
            switch(_progState) {
                case IDLE:
                    if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " Value changed with state IDLE");
                    return;
                case READING_FIRST:
                    if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " Value changed with state READING_FIRST");
                    return;
                case READING_SECOND:
                    if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " Value changed with state READING_SECOND");
                    return;
                default:
                    log.error("CV " + getCvNum() + "," + getSecondCvNum() + " Unexpected state found: " + _progState);
                    _progState = IDLE;
                    return;
            }
        }
    }

    JTextField _value = null;

    public class VarTextField extends JTextField {

        VarTextField(Document doc, String text, int col, SplitVariableValue var) {
            super(doc, text, col);
            _var = var;
            setBackground(_var._value.getBackground());
            addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    thisActionPerformed(e);
                }
            });
            addFocusListener(new java.awt.event.FocusListener() {

                public void focusGained(FocusEvent e) {
                    if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " focusGained");
                    enterField();
                }

                public void focusLost(FocusEvent e) {
                    if (log.isDebugEnabled()) log.debug("CV " + getCvNum() + "," + getSecondCvNum() + " focusLost");
                    exitField();
                }
            });
            _var.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

                public void propertyChange(java.beans.PropertyChangeEvent e) {
                    originalPropertyChanged(e);
                }
            });
        }

        SplitVariableValue _var;

        void thisActionPerformed(java.awt.event.ActionEvent e) {
            _var.actionPerformed(e);
        }

        void originalPropertyChanged(java.beans.PropertyChangeEvent e) {
            if (e.getPropertyName().equals("State")) {
                setBackground(_var._value.getBackground());
            }
        }
    }

    public void dispose() {
        if (log.isDebugEnabled()) log.debug("dispose");
        if (_value != null) _value.removeActionListener(this);
        (_cvVector.elementAt(getCvNum())).removePropertyChangeListener(this);
        (_cvVector.elementAt(getSecondCvNum())).removePropertyChangeListener(this);
        _value = null;
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SplitVariableValue.class.getName());
}
