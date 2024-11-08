package jmri.jmrit.symbolicprog;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.util.Vector;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.text.Document;

/**
 * Extends VariableValue to represent a NMRA long address
 * @author			Bob Jacobsen   Copyright (C) 2001, 2002
 * @version			$Revision: 1.24 $
 *
 */
public class LongAddrVariableValue extends VariableValue implements ActionListener, PropertyChangeListener, FocusListener {

    public LongAddrVariableValue(String name, String comment, String cvName, boolean readOnly, boolean infoOnly, boolean writeOnly, boolean opsOnly, int cvNum, String mask, int minVal, int maxVal, Vector<CvValue> v, JLabel status, String stdname) {
        super(name, comment, cvName, readOnly, infoOnly, writeOnly, opsOnly, cvNum, mask, v, status, stdname);
        _maxVal = maxVal;
        _minVal = minVal;
        _value = new JTextField("0", 5);
        _defaultColor = _value.getBackground();
        _value.setBackground(COLOR_UNKNOWN);
        _value.addActionListener(this);
        _value.addFocusListener(this);
        CvValue cv = (_cvVector.elementAt(getCvNum()));
        cv.addPropertyChangeListener(this);
        cv.setState(CvValue.FROMFILE);
        CvValue cv1 = (_cvVector.elementAt(getCvNum() + 1));
        cv1.addPropertyChangeListener(this);
        cv1.setState(CvValue.FROMFILE);
    }

    public CvValue[] usesCVs() {
        return new CvValue[] { _cvVector.elementAt(getCvNum()), _cvVector.elementAt(getCvNum() + 1) };
    }

    public void setToolTipText(String t) {
        super.setToolTipText(t);
        _value.setToolTipText(t);
    }

    int _maxVal;

    int _minVal;

    public Object rangeVal() {
        return "Long address";
    }

    String oldContents = "";

    void enterField() {
        oldContents = _value.getText();
    }

    void exitField() {
        if (_value != null && !oldContents.equals(_value.getText())) {
            int newVal = Integer.valueOf(_value.getText()).intValue();
            int oldVal = Integer.valueOf(oldContents).intValue();
            updatedTextField();
            prop.firePropertyChange("Value", Integer.valueOf(oldVal), Integer.valueOf(newVal));
        }
    }

    void updatedTextField() {
        if (log.isDebugEnabled()) log.debug("actionPerformed");
        CvValue cv17 = _cvVector.elementAt(getCvNum());
        CvValue cv18 = _cvVector.elementAt(getCvNum() + 1);
        int newVal;
        try {
            newVal = Integer.valueOf(_value.getText()).intValue();
        } catch (java.lang.NumberFormatException ex) {
            newVal = 0;
        }
        int newCv17 = ((newVal / 256) & 0x3F) | 0xc0;
        int newCv18 = newVal & 0xFF;
        cv17.setValue(newCv17);
        cv18.setValue(newCv18);
        if (log.isDebugEnabled()) log.debug("new value " + newVal + " gives CV17=" + newCv17 + " CV18=" + newCv18);
    }

    /** ActionListener implementations */
    public void actionPerformed(ActionEvent e) {
        if (log.isDebugEnabled()) log.debug("actionPerformed");
        int newVal = Integer.valueOf(_value.getText()).intValue();
        updatedTextField();
        prop.firePropertyChange("Value", null, Integer.valueOf(newVal));
    }

    /** FocusListener implementations */
    public void focusGained(FocusEvent e) {
        if (log.isDebugEnabled()) log.debug("focusGained");
        enterField();
    }

    public void focusLost(FocusEvent e) {
        if (log.isDebugEnabled()) log.debug("focusLost");
        exitField();
    }

    public String getValueString() {
        return _value.getText();
    }

    public void setIntValue(int i) {
        setValue(i);
    }

    public int getIntValue() {
        return Integer.valueOf(_value.getText()).intValue();
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
        int oldVal;
        try {
            oldVal = Integer.valueOf(_value.getText()).intValue();
        } catch (java.lang.NumberFormatException ex) {
            oldVal = -999;
        }
        if (log.isDebugEnabled()) log.debug("setValue with new value " + value + " old value " + oldVal);
        _value.setText("" + value);
        if (oldVal != value || getState() == VariableValue.UNKNOWN) actionPerformed(null);
        prop.firePropertyChange("Value", Integer.valueOf(oldVal), Integer.valueOf(value));
    }

    Color _defaultColor;

    void setColor(Color c) {
        if (c != null) _value.setBackground(c); else _value.setBackground(_defaultColor);
    }

    public Component getNewRep(String format) {
        return updateRepresentation(new VarTextField(_value.getDocument(), _value.getText(), 5, this));
    }

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
        CvValue cv2 = (_cvVector.elementAt(getCvNum() + 1));
        return (considerChanged(cv1) || considerChanged(cv2));
    }

    public void setToRead(boolean state) {
        (_cvVector.elementAt(getCvNum())).setToRead(state);
        (_cvVector.elementAt(getCvNum() + 1)).setToRead(state);
    }

    public boolean isToRead() {
        return (_cvVector.elementAt(getCvNum())).isToRead() || (_cvVector.elementAt(getCvNum() + 1)).isToRead();
    }

    public void setToWrite(boolean state) {
        (_cvVector.elementAt(getCvNum())).setToWrite(state);
        (_cvVector.elementAt(getCvNum() + 1)).setToWrite(state);
    }

    public boolean isToWrite() {
        return (_cvVector.elementAt(getCvNum())).isToWrite() || (_cvVector.elementAt(getCvNum() + 1)).isToWrite();
    }

    public void readChanges() {
        if (isChanged()) readAll();
    }

    public void writeChanges() {
        if (isChanged()) writeAll();
    }

    public void readAll() {
        if (log.isDebugEnabled()) log.debug("longAddr read() invoked");
        setToRead(false);
        setBusy(true);
        if (_progState != IDLE) log.warn("Programming state " + _progState + ", not IDLE, in read()");
        _progState = READING_FIRST;
        if (log.isDebugEnabled()) log.debug("invoke CV read");
        (_cvVector.elementAt(getCvNum())).read(_status);
    }

    public void writeAll() {
        if (log.isDebugEnabled()) log.debug("write() invoked");
        if (getReadOnly()) log.error("unexpected write operation when readOnly is set");
        setToWrite(false);
        setBusy(true);
        if (_progState != IDLE) log.warn("Programming state " + _progState + ", not IDLE, in write()");
        _progState = WRITING_FIRST;
        if (log.isDebugEnabled()) log.debug("invoke CV write");
        (_cvVector.elementAt(getCvNum())).write(_status);
    }

    public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (log.isDebugEnabled()) log.debug("property changed event - name: " + e.getPropertyName());
        if (e.getPropertyName().equals("Busy") && ((Boolean) e.getNewValue()).equals(Boolean.FALSE)) {
            switch(_progState) {
                case IDLE:
                    if (log.isDebugEnabled()) log.error("Busy goes false with state IDLE");
                    return;
                case READING_FIRST:
                    if (log.isDebugEnabled()) log.debug("Busy goes false with state READING_FIRST");
                    _progState = READING_SECOND;
                    (_cvVector.elementAt(getCvNum() + 1)).read(_status);
                    return;
                case READING_SECOND:
                    if (log.isDebugEnabled()) log.debug("Busy goes false with state READING_SECOND");
                    _progState = IDLE;
                    (_cvVector.elementAt(getCvNum())).setState(READ);
                    (_cvVector.elementAt(getCvNum() + 1)).setState(READ);
                    setBusy(false);
                    return;
                case WRITING_FIRST:
                    if (log.isDebugEnabled()) log.debug("Busy goes false with state WRITING_FIRST");
                    _progState = WRITING_SECOND;
                    (_cvVector.elementAt(getCvNum() + 1)).write(_status);
                    return;
                case WRITING_SECOND:
                    if (log.isDebugEnabled()) log.debug("Busy goes false with state WRITING_SECOND");
                    _progState = IDLE;
                    super.setState(STORED);
                    setBusy(false);
                    return;
                default:
                    log.error("Unexpected state found: " + _progState);
                    _progState = IDLE;
                    return;
            }
        } else if (e.getPropertyName().equals("State")) {
            CvValue cv = _cvVector.elementAt(getCvNum());
            if (log.isDebugEnabled()) log.debug("CV State changed to " + cv.getState());
            setState(cv.getState());
        } else if (e.getPropertyName().equals("Value")) {
            CvValue cv0 = _cvVector.elementAt(getCvNum());
            CvValue cv1 = _cvVector.elementAt(getCvNum() + 1);
            int newVal = (cv0.getValue() & 0x3f) * 256 + cv1.getValue();
            setValue(newVal);
            setState(cv0.getState());
            switch(_progState) {
                case IDLE:
                    if (log.isDebugEnabled()) log.debug("Value changed with state IDLE");
                    return;
                case READING_FIRST:
                    if (log.isDebugEnabled()) log.debug("Value changed with state READING_FIRST");
                    return;
                case READING_SECOND:
                    if (log.isDebugEnabled()) log.debug("Value changed with state READING_SECOND");
                    return;
                default:
                    log.error("Unexpected state found: " + _progState);
                    _progState = IDLE;
                    return;
            }
        }
    }

    JTextField _value = null;

    public class VarTextField extends JTextField {

        VarTextField(Document doc, String text, int col, LongAddrVariableValue var) {
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
                    if (log.isDebugEnabled()) log.debug("focusGained");
                    enterField();
                }

                public void focusLost(FocusEvent e) {
                    if (log.isDebugEnabled()) log.debug("focusLost");
                    exitField();
                }
            });
            _var.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

                public void propertyChange(java.beans.PropertyChangeEvent e) {
                    originalPropertyChanged(e);
                }
            });
        }

        LongAddrVariableValue _var;

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
        (_cvVector.elementAt(getCvNum() + 1)).removePropertyChangeListener(this);
        _value = null;
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LongAddrVariableValue.class.getName());
}
