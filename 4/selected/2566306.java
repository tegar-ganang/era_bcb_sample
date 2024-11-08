package jmri.jmrit.symbolicprog;

import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import java.util.Vector;
import jmri.*;

/**
 * Table data model for display of IndexedCvValues in symbolic programmer.
 *
 * This represents the contents of a single decoder, so the
 * Programmer used to access it is a data member.
 *
 * @author    Howard G. Penny   Copyright (C) 2005
 * @author 		Daniel Boudreau Copyright (C) 2007
 * @version   $Revision: 1.19 $
 */
public class IndexedCvTableModel extends javax.swing.table.AbstractTableModel implements ActionListener, PropertyChangeListener {

    static final java.util.ResourceBundle rbt = jmri.jmrit.symbolicprog.SymbolicProgBundle.bundle();

    private int _numRows = 0;

    static final int MAXCVNUM = 256;

    private Vector<CvValue> _indxCvDisplayVector = new Vector<CvValue>();

    private Vector<CvValue> _indxCvAllVector = new Vector<CvValue>(MAXCVNUM + 1);

    public Vector<CvValue> allIndxCvVector() {
        return _indxCvAllVector;
    }

    private Vector<JButton> _indxWriteButtons = new Vector<JButton>();

    private Vector<JButton> _indxReadButtons = new Vector<JButton>();

    private Vector<JButton> _indxCompareButtons = new Vector<JButton>();

    private Programmer mProgrammer;

    private static final int NAMECOLUMN = 0;

    private static final int PICOLUMN = 1;

    private static final int SICOLUMN = 2;

    private static final int CVCOLUMN = 3;

    private static final int VALCOLUMN = 4;

    private static final int STATECOLUMN = 5;

    private static final int READCOLUMN = 6;

    private static final int WRITECOLUMN = 7;

    private static final int COMPARECOLUMN = 8;

    private static final int HIGHESTCOLUMN = COMPARECOLUMN + 1;

    private JLabel _status = null;

    public JLabel getStatusLabel() {
        return _status;
    }

    public IndexedCvTableModel(JLabel status, Programmer pProgrammer) {
        super();
        mProgrammer = pProgrammer;
        _status = status;
        for (int i = 0; i <= MAXCVNUM; i++) _indxCvAllVector.addElement(null);
    }

    /**
     * Find the existing IndexedCV
     * that matches a particular name
     */
    public CvValue getMatchingIndexedCV(String name) {
        for (int i = 0; i < _numRows; i++) {
            CvValue cv = _indxCvAllVector.get(i);
            if (cv == null) {
                log.error("cv == null in getMatchingIndexedCV");
                break;
            }
            if (cv.cvName().equals(name)) {
                return cv;
            }
        }
        return null;
    }

    /**
     * Gives access to the programmer used to reach these Indexed CVs,
     * so you can check on mode, capabilities, etc.
     * @return Programmer object for the Indexed CVs
     */
    public Programmer getProgrammer() {
        return mProgrammer;
    }

    public int getRowCount() {
        return _numRows;
    }

    public int getColumnCount() {
        return HIGHESTCOLUMN;
    }

    public String getColumnName(int col) {
        switch(col) {
            case NAMECOLUMN:
                return rbt.getString("ColumnNameNumber");
            case PICOLUMN:
                return "PI Val";
            case SICOLUMN:
                return "SI Val";
            case CVCOLUMN:
                return "CV Num";
            case VALCOLUMN:
                return rbt.getString("ColumnNameValue");
            case STATECOLUMN:
                return rbt.getString("ColumnNameState");
            case READCOLUMN:
                return rbt.getString("ColumnNameRead");
            case WRITECOLUMN:
                return rbt.getString("ColumnNameWrite");
            case COMPARECOLUMN:
                return rbt.getString("ColumnNameCompare");
            default:
                return "unknown";
        }
    }

    public Class<?> getColumnClass(int col) {
        switch(col) {
            case NAMECOLUMN:
                return String.class;
            case PICOLUMN:
                return String.class;
            case SICOLUMN:
                return String.class;
            case CVCOLUMN:
                return String.class;
            case VALCOLUMN:
                return JTextField.class;
            case STATECOLUMN:
                return String.class;
            case READCOLUMN:
                return JButton.class;
            case WRITECOLUMN:
                return JButton.class;
            case COMPARECOLUMN:
                return JButton.class;
            default:
                return null;
        }
    }

    public boolean isCellEditable(int row, int col) {
        switch(col) {
            case NAMECOLUMN:
                return false;
            case PICOLUMN:
                return false;
            case SICOLUMN:
                return false;
            case CVCOLUMN:
                return false;
            case VALCOLUMN:
                if (_indxCvDisplayVector.elementAt(row).getReadOnly() || _indxCvDisplayVector.elementAt(row).getInfoOnly()) {
                    return false;
                } else {
                    return true;
                }
            case STATECOLUMN:
                return false;
            case READCOLUMN:
                return true;
            case WRITECOLUMN:
                return true;
            case COMPARECOLUMN:
                return true;
            default:
                return false;
        }
    }

    public String getName(int row) {
        return "" + _indxCvDisplayVector.elementAt(row).cvName();
    }

    public String getValString(int row) {
        return "" + _indxCvDisplayVector.elementAt(row).getValue();
    }

    public int getCvByName(String name) {
        int row = 0;
        while (row < _numRows) {
            if (_indxCvDisplayVector.elementAt(row).cvName().compareTo(name) == 0) {
                return row;
            }
            row++;
        }
        return -1;
    }

    public CvValue getCvByRow(int row) {
        return _indxCvDisplayVector.elementAt(row);
    }

    public CvValue getCvByNumber(int row) {
        return _indxCvAllVector.elementAt(row);
    }

    public Object getValueAt(int row, int col) {
        if (row >= _indxCvDisplayVector.size()) {
            log.debug("row greater than cv index");
            return "Error";
        }
        switch(col) {
            case NAMECOLUMN:
                return "" + (_indxCvDisplayVector.elementAt(row)).cvName();
            case PICOLUMN:
                return "" + (_indxCvDisplayVector.elementAt(row)).piVal();
            case SICOLUMN:
                return "" + (_indxCvDisplayVector.elementAt(row)).siVal();
            case CVCOLUMN:
                return "" + (_indxCvDisplayVector.elementAt(row)).iCv();
            case VALCOLUMN:
                return (_indxCvDisplayVector.elementAt(row)).getTableEntry();
            case STATECOLUMN:
                int state = (_indxCvDisplayVector.elementAt(row)).getState();
                switch(state) {
                    case CvValue.UNKNOWN:
                        return rbt.getString("CvStateUnknown");
                    case CvValue.READ:
                        return rbt.getString("CvStateRead");
                    case CvValue.EDITED:
                        return rbt.getString("CvStateEdited");
                    case CvValue.STORED:
                        return rbt.getString("CvStateStored");
                    case CvValue.FROMFILE:
                        return rbt.getString("CvStateFromFile");
                    case CvValue.SAME:
                        return rbt.getString("CvStateSame");
                    case CvValue.DIFF:
                        return rbt.getString("CvStateDiff") + " " + (_indxCvDisplayVector.elementAt(row)).getDecoderValue();
                    default:
                        return "inconsistent";
                }
            case READCOLUMN:
                return _indxReadButtons.elementAt(row);
            case WRITECOLUMN:
                return _indxWriteButtons.elementAt(row);
            case COMPARECOLUMN:
                return _indxCompareButtons.elementAt(row);
            default:
                return "unknown";
        }
    }

    public void setValueAt(Object value, int row, int col) {
        switch(col) {
            case VALCOLUMN:
                if ((_indxCvDisplayVector.elementAt(row)).getValue() != ((Integer) value).intValue()) {
                    (_indxCvDisplayVector.elementAt(row)).setValue(((Integer) value).intValue());
                }
                break;
            default:
                break;
        }
    }

    private int _row;

    public void actionPerformed(ActionEvent e) {
        if (log.isDebugEnabled()) log.debug("action command: " + e.getActionCommand());
        char b = e.getActionCommand().charAt(0);
        int row = Integer.valueOf(e.getActionCommand().substring(1)).intValue();
        _row = row;
        if (log.isDebugEnabled()) log.debug("event on " + b + " row " + row);
        if (b == 'R') {
            indexedRead();
        } else if (b == 'C') {
            indexedCompare();
        } else {
            indexedWrite();
        }
    }

    private int _progState = 0;

    private static final int IDLE = 0;

    private static final int WRITING_PI4R = 1;

    private static final int WRITING_PI4W = 2;

    private static final int WRITING_SI4R = 3;

    private static final int WRITING_SI4W = 4;

    private static final int READING_CV = 5;

    private static final int WRITING_CV = 6;

    private static final int WRITING_PI4C = 7;

    private static final int WRITING_SI4C = 8;

    private static final int COMPARE_CV = 9;

    /**
     * Count number of retries done
     */
    private int retries = 0;

    /**
     * Define maximum number of retries of read/write operations before moving on
     */
    private static final int RETRY_MAX = 2;

    public void indexedRead() {
        if (_progState != IDLE) log.warn("Programming state " + _progState + ", not IDLE, in read()");
        if ((_indxCvDisplayVector.elementAt(_row)).siVal() >= 0) {
            _progState = WRITING_PI4R;
        } else {
            _progState = WRITING_SI4R;
        }
        retries = 0;
        if (log.isDebugEnabled()) log.debug("invoke PI write for CV read");
        (_indxCvDisplayVector.elementAt(_row)).writePI(_status);
    }

    public void indexedWrite() {
        if ((_indxCvDisplayVector.elementAt(_row)).getReadOnly()) {
            log.error("unexpected write operation when readOnly is set");
        }
        if (_progState != IDLE) log.warn("Programming state " + _progState + ", not IDLE, in write()");
        if ((_indxCvDisplayVector.elementAt(_row)).siVal() >= 0) {
            _progState = WRITING_PI4W;
        } else {
            _progState = WRITING_SI4W;
        }
        retries = 0;
        if (log.isDebugEnabled()) log.debug("invoke PI write for CV write");
        (_indxCvDisplayVector.elementAt(_row)).writePI(_status);
    }

    public void indexedCompare() {
        if (_progState != IDLE) log.warn("Programming state " + _progState + ", not IDLE, in read()");
        if ((_indxCvDisplayVector.elementAt(_row)).siVal() >= 0) {
            _progState = WRITING_PI4C;
        } else {
            _progState = WRITING_SI4C;
        }
        retries = 0;
        if (log.isDebugEnabled()) log.debug("invoke PI write for CV compare");
        (_indxCvDisplayVector.elementAt(_row)).writePI(_status);
    }

    public void propertyChange(PropertyChangeEvent e) {
        if (log.isDebugEnabled()) log.debug("Property changed: " + e.getPropertyName());
        if (e.getPropertyName().equals("Busy") && ((Boolean) e.getNewValue()).equals(Boolean.FALSE)) {
            switch(_progState) {
                case IDLE:
                    if (log.isDebugEnabled()) log.error("Busy goes false with state IDLE");
                    return;
                case WRITING_PI4R:
                case WRITING_PI4C:
                case WRITING_PI4W:
                    if (log.isDebugEnabled()) log.debug("Busy goes false with state WRITING_PI");
                    if ((retries < RETRY_MAX) && ((_indxCvDisplayVector.elementAt(_row)).getState() != CvValue.STORED)) {
                        log.debug("retry");
                        retries++;
                        (_indxCvDisplayVector.elementAt(_row)).writePI(_status);
                        return;
                    }
                    retries = 0;
                    if (_progState == WRITING_PI4R) _progState = WRITING_SI4R; else if (_progState == WRITING_PI4C) _progState = WRITING_SI4C; else _progState = WRITING_SI4W;
                    (_indxCvDisplayVector.elementAt(_row)).writeSI(_status);
                    return;
                case WRITING_SI4R:
                case WRITING_SI4C:
                case WRITING_SI4W:
                    if (log.isDebugEnabled()) log.debug("Busy goes false with state WRITING_SI");
                    if ((retries < RETRY_MAX) && ((_indxCvDisplayVector.elementAt(_row)).getState() != CvValue.STORED)) {
                        log.debug("retry");
                        retries++;
                        (_indxCvDisplayVector.elementAt(_row)).writeSI(_status);
                        return;
                    }
                    retries = 0;
                    if (_progState == WRITING_SI4R) {
                        _progState = READING_CV;
                        (_indxCvDisplayVector.elementAt(_row)).readIcV(_status);
                    } else if (_progState == WRITING_SI4C) {
                        _progState = COMPARE_CV;
                        (_indxCvDisplayVector.elementAt(_row)).confirmIcV(_status);
                    } else {
                        _progState = WRITING_CV;
                        (_indxCvDisplayVector.elementAt(_row)).writeIcV(_status);
                    }
                    return;
                case READING_CV:
                    if (log.isDebugEnabled()) log.debug("Finished reading the Indexed CV");
                    if ((retries < RETRY_MAX) && ((_indxCvDisplayVector.elementAt(_row)).getState() != CvValue.READ)) {
                        log.debug("retry");
                        retries++;
                        (_indxCvDisplayVector.elementAt(_row)).readIcV(_status);
                        return;
                    }
                    retries = 0;
                    _progState = IDLE;
                    return;
                case COMPARE_CV:
                    if (log.isDebugEnabled()) log.debug("Finished reading the Indexed CV for compare");
                    if ((retries < RETRY_MAX) && ((_indxCvDisplayVector.elementAt(_row)).getState() != CvValue.SAME) && ((_indxCvDisplayVector.elementAt(_row)).getState() != CvValue.DIFF)) {
                        log.debug("retry");
                        retries++;
                        (_indxCvDisplayVector.elementAt(_row)).confirmIcV(_status);
                        return;
                    }
                    retries = 0;
                    _progState = IDLE;
                    return;
                case WRITING_CV:
                    if (log.isDebugEnabled()) log.debug("Finished writing the Indexed CV");
                    if ((retries < RETRY_MAX) && ((_indxCvDisplayVector.elementAt(_row)).getState() != CvValue.STORED)) {
                        log.debug("retry");
                        retries++;
                        (_indxCvDisplayVector.elementAt(_row)).writeIcV(_status);
                        return;
                    }
                    retries = 0;
                    _progState = IDLE;
                    return;
                default:
                    log.error("Unexpected state found: " + _progState);
                    _progState = IDLE;
                    return;
            }
        }
        fireTableDataChanged();
    }

    /**
     * return is the current row or the row of an existing Indexed CV
     */
    public int addIndxCV(int row, String cvName, int piCv, int piVal, int siCv, int siVal, int iCv, boolean readOnly, boolean infoOnly, boolean writeOnly) {
        int existingRow = getCvByName(cvName);
        if (existingRow == -1) {
            row = _numRows++;
            CvValue indxCv = new CvValue(row, cvName, piCv, piVal, siCv, siVal, iCv, mProgrammer);
            indxCv.setReadOnly(readOnly);
            indxCv.setInfoOnly(infoOnly);
            _indxCvAllVector.setElementAt(indxCv, row);
            _indxCvDisplayVector.addElement(indxCv);
            indxCv.addPropertyChangeListener(this);
            JButton bw = new JButton("Write");
            _indxWriteButtons.addElement(bw);
            JButton br = new JButton("Read");
            _indxReadButtons.addElement(br);
            JButton bc = new JButton("Compare");
            _indxCompareButtons.addElement(bc);
            if (infoOnly || readOnly) {
                if (writeOnly) {
                    bw.setEnabled(true);
                    bw.setActionCommand("W" + row);
                    bw.addActionListener(this);
                } else {
                    bw.setEnabled(false);
                }
                if (infoOnly) {
                    br.setEnabled(false);
                    bc.setEnabled(false);
                } else {
                    br.setEnabled(true);
                    br.setActionCommand("R" + row);
                    br.addActionListener(this);
                    bc.setEnabled(true);
                    bc.setActionCommand("C" + row);
                    bc.addActionListener(this);
                }
            } else {
                bw.setEnabled(true);
                bw.setActionCommand("W" + row);
                bw.addActionListener(this);
                if (writeOnly) {
                    br.setEnabled(false);
                    bc.setEnabled(false);
                } else {
                    br.setEnabled(true);
                    br.setActionCommand("R" + row);
                    br.addActionListener(this);
                    bc.setEnabled(true);
                    bc.setActionCommand("C" + row);
                    bc.addActionListener(this);
                }
            }
            if (log.isDebugEnabled()) log.debug("addIndxCV adds row at " + row);
            fireTableDataChanged();
        } else {
            if (log.isDebugEnabled()) log.debug("addIndxCV finds existing row of " + existingRow + " with numRows " + _numRows);
            row = existingRow;
        }
        if (row > -1 && row < _indxCvAllVector.size()) {
            CvValue indxcv = _indxCvAllVector.elementAt(row);
            if (readOnly) indxcv.setReadOnly(readOnly);
            if (infoOnly) {
                indxcv.setReadOnly(infoOnly);
                indxcv.setInfoOnly(infoOnly);
            }
            if (writeOnly) indxcv.setWriteOnly(writeOnly);
        }
        return row;
    }

    public boolean decoderDirty() {
        int len = _indxCvDisplayVector.size();
        for (int i = 0; i < len; i++) {
            if (((_indxCvDisplayVector.elementAt(i))).getState() == CvValue.EDITED) {
                if (log.isDebugEnabled()) log.debug("CV decoder dirty due to " + ((_indxCvDisplayVector.elementAt(i))).number());
                return true;
            }
        }
        return false;
    }

    public void dispose() {
        if (log.isDebugEnabled()) log.debug("dispose");
        for (int i = 0; i < _indxWriteButtons.size(); i++) {
            _indxWriteButtons.elementAt(i).removeActionListener(this);
        }
        for (int i = 0; i < _indxReadButtons.size(); i++) {
            _indxReadButtons.elementAt(i).removeActionListener(this);
        }
        for (int i = 0; i < _indxCompareButtons.size(); i++) {
            _indxCompareButtons.elementAt(i).removeActionListener(this);
        }
        for (int i = 0; i < _indxCvDisplayVector.size(); i++) {
            (_indxCvDisplayVector.elementAt(i)).removePropertyChangeListener(this);
        }
        _indxCvDisplayVector.removeAllElements();
        _indxCvDisplayVector = null;
        _indxCvAllVector.removeAllElements();
        _indxCvAllVector = null;
        _indxWriteButtons.removeAllElements();
        _indxWriteButtons = null;
        _indxReadButtons.removeAllElements();
        _indxReadButtons = null;
        _indxCompareButtons.removeAllElements();
        _indxCompareButtons = null;
        _status = null;
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(IndexedCvTableModel.class.getName());
}
