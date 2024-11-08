package jmri.jmrit.symbolicprog;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Extends VariableValue to represent a enumerated variable.
 *
 * @author	Bob Jacobsen   Copyright (C) 2001, 2002, 2003
 * @version	$Revision: 1.30 $
 *
 */
public class EnumVariableValue extends VariableValue implements ActionListener, PropertyChangeListener {

    public EnumVariableValue(String name, String comment, String cvName, boolean readOnly, boolean infoOnly, boolean writeOnly, boolean opsOnly, int cvNum, String mask, int minVal, int maxVal, Vector<CvValue> v, JLabel status, String stdname) {
        super(name, comment, cvName, readOnly, infoOnly, writeOnly, opsOnly, cvNum, mask, v, status, stdname);
        _maxVal = maxVal;
        _minVal = minVal;
    }

    /**
     * Create a null object.  Normally only used for tests and to pre-load classes.
     */
    public EnumVariableValue() {
    }

    public CvValue[] usesCVs() {
        return new CvValue[] { _cvVector.elementAt(getCvNum()) };
    }

    public void nItems(int n) {
        _itemArray = new String[n];
        _valueArray = new int[n];
        _nstored = 0;
    }

    /**
     * Create a new item in the enumeration, with an associated
     * value one more than the last item (or zero if this is the first
     * one added)
     * @param s  Name of the enumeration item
     */
    public void addItem(String s) {
        if (_nstored == 0) {
            addItem(s, 0);
        } else {
            addItem(s, _valueArray[_nstored - 1] + 1);
        }
    }

    /**
     * Create a new item in the enumeration, with a specified
     * associated value.
     * @param s  Name of the enumeration item
     */
    public void addItem(String s, int value) {
        _valueArray[_nstored] = value;
        _itemArray[_nstored++] = s;
    }

    public void lastItem() {
        _value = new JComboBox(_itemArray);
        _value.setActionCommand("");
        _defaultColor = _value.getBackground();
        _value.setBackground(COLOR_UNKNOWN);
        _value.addActionListener(this);
        CvValue cv = _cvVector.elementAt(getCvNum());
        cv.addPropertyChangeListener(this);
        cv.setState(CvValue.FROMFILE);
    }

    public void setToolTipText(String t) {
        super.setToolTipText(t);
        _value.setToolTipText(t);
    }

    JComboBox _value = null;

    private String[] _itemArray = null;

    private int[] _valueArray = null;

    private int _nstored;

    int _maxVal;

    int _minVal;

    Color _defaultColor;

    public void setAvailable(boolean a) {
        _value.setVisible(a);
        for (ComboCheckBox c : comboCBs) c.setVisible(a);
        for (VarComboBox c : comboVars) c.setVisible(a);
        for (ComboRadioButtons c : comboRBs) c.setVisible(a);
        super.setAvailable(a);
    }

    public Object rangeVal() {
        return "enum: " + _minVal + " - " + _maxVal;
    }

    public void actionPerformed(ActionEvent e) {
        if (log.isDebugEnabled()) log.debug(label() + " start action event: " + e);
        if (!(e.getActionCommand().equals(""))) {
            _value.setSelectedItem(e.getActionCommand());
            if (log.isDebugEnabled()) log.debug(label() + " action event was from alternate rep");
        }
        int oldVal = getIntValue();
        CvValue cv = _cvVector.elementAt(getCvNum());
        int oldCv = cv.getValue();
        int newVal = getIntValue();
        int newCv = newValue(oldCv, newVal, getMask());
        if (newCv != oldCv) {
            cv.setValue(newCv);
            if (log.isDebugEnabled()) log.debug(label() + " about to firePropertyChange");
            prop.firePropertyChange("Value", null, Integer.valueOf(oldVal));
            if (log.isDebugEnabled()) log.debug(label() + " returned to from firePropertyChange");
        }
        if (log.isDebugEnabled()) log.debug(label() + " end action event saw oldCv=" + oldCv + " newVal=" + newVal + " newCv=" + newCv);
    }

    public String getValueString() {
        return "" + _value.getSelectedIndex();
    }

    public void setIntValue(int i) {
        selectValue(i);
    }

    public String getTextValue() {
        return _value.getSelectedItem().toString();
    }

    public Object getValueObject() {
        return Integer.valueOf(_value.getSelectedIndex());
    }

    /**
     * Set to a specific value.
     * <P>
     * This searches for the displayed value, and sets the
     * enum to that particular one.  It used to work off an index,
     * but now it looks for the value.
     * <P>
     * If the value is larger than any defined, a new one is created.
     * @param value
     */
    protected void selectValue(int value) {
        if (value > 256) log.error("Saw unreasonable internal value: " + value);
        for (int i = 0; i < _valueArray.length; i++) if (_valueArray[i] == value) {
            _value.setSelectedIndex(i);
            return;
        }
        log.debug("Create new item with value " + value + " count was " + _value.getItemCount() + " in " + label());
        _value.addItem("Reserved value " + value);
        int[] oldArray = _valueArray;
        _valueArray = new int[oldArray.length + 1];
        for (int i = 0; i < oldArray.length; i++) _valueArray[i] = oldArray[i];
        _valueArray[oldArray.length] = value;
        _value.setSelectedItem("Reserved value " + value);
    }

    public int getIntValue() {
        if (_value.getSelectedIndex() >= _valueArray.length || _value.getSelectedIndex() < 0) log.error("trying to get value " + _value.getSelectedIndex() + " too large" + " for array length " + _valueArray.length + " in var " + label());
        return _valueArray[_value.getSelectedIndex()];
    }

    public Component getCommonRep() {
        return _value;
    }

    public void setValue(int value) {
        int oldVal = getIntValue();
        selectValue(value);
        if (oldVal != value || getState() == VariableValue.UNKNOWN) prop.firePropertyChange("Value", null, Integer.valueOf(value));
    }

    public Component getNewRep(String format) {
        if (format.equals("checkbox")) {
            ComboCheckBox b = new ComboCheckBox(_value, this);
            comboCBs.add(b);
            updateRepresentation(b);
            return b;
        } else if (format.equals("radiobuttons")) {
            ComboRadioButtons b = new ComboRadioButtons(_value, this);
            comboRBs.add(b);
            updateRepresentation(b);
            return b;
        } else if (format.equals("onradiobutton")) {
            ComboRadioButtons b = new ComboOnRadioButton(_value, this);
            comboRBs.add(b);
            updateRepresentation(b);
            return b;
        } else if (format.equals("offradiobutton")) {
            ComboRadioButtons b = new ComboOffRadioButton(_value, this);
            comboRBs.add(b);
            updateRepresentation(b);
            return b;
        } else {
            VarComboBox b = new VarComboBox(_value.getModel(), this);
            comboVars.add(b);
            updateRepresentation(b);
            return b;
        }
    }

    private List<ComboCheckBox> comboCBs = new ArrayList<ComboCheckBox>();

    private List<VarComboBox> comboVars = new ArrayList<VarComboBox>();

    private List<ComboRadioButtons> comboRBs = new ArrayList<ComboRadioButtons>();

    void setColor(Color c) {
        if (c != null) _value.setBackground(c); else _value.setBackground(_defaultColor);
    }

    /**
     * Notify the connected CVs of a state change from above
     * @param state
     */
    public void setCvState(int state) {
        _cvVector.elementAt(getCvNum()).setState(state);
    }

    public boolean isChanged() {
        CvValue cv = _cvVector.elementAt(getCvNum());
        return considerChanged(cv);
    }

    public void readChanges() {
        if (isToRead() && !isChanged()) log.debug("!!!!!!! unacceptable combination in readChanges: " + label());
        if (isChanged() || isToRead()) readAll();
    }

    public void writeChanges() {
        if (isToWrite() && !isChanged()) log.debug("!!!!!! unacceptable combination in writeChanges: " + label());
        if (isChanged() || isToWrite()) writeAll();
    }

    public void readAll() {
        setToRead(false);
        setBusy(true);
        _cvVector.elementAt(getCvNum()).read(_status);
    }

    public void writeAll() {
        setToWrite(false);
        if (getReadOnly()) log.error("unexpected write operation when readOnly is set");
        setBusy(true);
        _cvVector.elementAt(getCvNum()).write(_status);
    }

    public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("Busy")) {
            if (((Boolean) e.getNewValue()).equals(Boolean.FALSE)) {
                setToRead(false);
                setToWrite(false);
                setBusy(false);
            }
        } else if (e.getPropertyName().equals("State")) {
            CvValue cv = _cvVector.elementAt(getCvNum());
            if (cv.getState() == STORED) setToWrite(false);
            if (cv.getState() == READ) setToRead(false);
            setState(cv.getState());
        } else if (e.getPropertyName().equals("Value")) {
            CvValue cv = _cvVector.elementAt(getCvNum());
            int newVal = (cv.getValue() & maskVal(getMask())) >>> offsetVal(getMask());
            setValue(newVal);
        }
    }

    public class VarComboBox extends JComboBox {

        VarComboBox(ComboBoxModel m, EnumVariableValue var) {
            super(m);
            _var = var;
            _l = new java.beans.PropertyChangeListener() {

                public void propertyChange(java.beans.PropertyChangeEvent e) {
                    if (log.isDebugEnabled()) log.debug("VarComboBox saw property change: " + e);
                    originalPropertyChanged(e);
                }
            };
            setBackground(_var._value.getBackground());
            _var.addPropertyChangeListener(_l);
        }

        EnumVariableValue _var;

        transient java.beans.PropertyChangeListener _l = null;

        void originalPropertyChanged(java.beans.PropertyChangeEvent e) {
            if (e.getPropertyName().equals("State")) {
                setBackground(_var._value.getBackground());
            }
        }

        public void dispose() {
            if (_var != null && _l != null) _var.removePropertyChangeListener(_l);
            _l = null;
            _var = null;
        }
    }

    public void dispose() {
        if (log.isDebugEnabled()) log.debug("dispose");
        _cvVector.elementAt(getCvNum()).removePropertyChangeListener(this);
        disposeReps();
    }

    void disposeReps() {
        if (_value != null) _value.removeActionListener(this);
        for (int i = 0; i < comboCBs.size(); i++) {
            comboCBs.get(i).dispose();
        }
        for (int i = 0; i < comboVars.size(); i++) {
            comboVars.get(i).dispose();
        }
        for (int i = 0; i < comboRBs.size(); i++) {
            comboRBs.get(i).dispose();
        }
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EnumVariableValue.class.getName());
}
