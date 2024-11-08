package jmri.jmrit.symbolicprog.tabbedframe;

import jmri.util.davidflanagan.HardcopyWriter;
import jmri.jmrit.symbolicprog.CvTableModel;
import jmri.jmrit.symbolicprog.IndexedCvTableModel;
import jmri.jmrit.symbolicprog.CvValue;
import jmri.jmrit.symbolicprog.DccAddressPanel;
import jmri.jmrit.symbolicprog.FnMapPanel;
import jmri.jmrit.symbolicprog.ValueEditor;
import jmri.jmrit.symbolicprog.ValueRenderer;
import jmri.jmrit.symbolicprog.VariableTableModel;
import jmri.jmrit.symbolicprog.VariableValue;
import jmri.util.jdom.LocaleSelector;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import javax.swing.*;
import java.util.StringTokenizer;
import java.util.Vector;
import java.awt.Color;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import org.jdom.Attribute;
import org.jdom.Element;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides the individual panes for the TabbedPaneProgrammer.
 * Note that this is not only the panes carrying variables, but also the
 * special purpose panes for the CV table, etc.
 *<P>
 * This class implements PropertyChangeListener so that it can be notified
 * when a variable changes its busy status at the end of a programming read/write operation
 *
 * There are four read and write operation types, all of which have to be handled carefully:
 * <DL>
 * <DT>Write Changes<DD>This must write changes that occur after the operation
 *                      starts, because the act of writing a variable/CV may
 *                      change another.  For example, writing CV 1 will mark CV 29 as changed.
 *           <P>The definition of "changed" is operationally in the
 *              {@link jmri.jmrit.symbolicprog.VariableValue#isChanged} member function.
 *
 * <DT>Write All<DD>Like write changes, this might have to go back and re-write a variable
 *                  depending on what has previously happened.  It should write every
 *              variable (at least) once.
 * <DT>Read All<DD>This should read every variable once.
 * <DT>Read Changes<DD>This should read every variable that's marked as changed.
 *          Currently, we use a common definition of changed with the write operations,
 *      and that someday might have to change.
 *
 * </DL>
 *
 * @author    Bob Jacobsen   Copyright (C) 2001, 2003, 2004, 2005, 2006
 * @author    D Miller Copyright 2003
 * @author    Howard G. Penny   Copyright (C) 2005
 * @version   $Revision: 1.81 $
 * @see       jmri.jmrit.symbolicprog.VariableValue#isChanged
 *
 */
public class PaneProgPane extends javax.swing.JPanel implements java.beans.PropertyChangeListener {

    CvTableModel _cvModel;

    IndexedCvTableModel _indexedCvModel;

    VariableTableModel _varModel;

    PaneContainer container;

    boolean _cvTable;

    static final java.util.ResourceBundle rbt = jmri.jmrit.symbolicprog.SymbolicProgBundle.bundle();

    transient ItemListener l1;

    transient ItemListener l2;

    transient ItemListener l3;

    transient ItemListener l4;

    transient ItemListener l5;

    transient ItemListener l6;

    String mName = "";

    /**
     * Create a null object.  Normally only used for tests and to pre-load classes.
     */
    public PaneProgPane() {
    }

    /**
     * Construct the Pane from the XML definition element.
     *
     * @param name  Name to appear on tab of pane
     * @param pane  The JDOM Element for the pane definition
     * @param cvModel Already existing TableModel containing the CV definitions
     * @param icvModel Already existing TableModel containing the Indexed CV definitions
     * @param varModel Already existing TableModel containing the variable definitions
     * @param modelElem "model" element from the Decoder Index, used to check what decoder options are present.
     */
    @SuppressWarnings("unchecked")
    public PaneProgPane(PaneContainer parent, String name, Element pane, CvTableModel cvModel, IndexedCvTableModel icvModel, VariableTableModel varModel, Element modelElem) {
        container = parent;
        mName = name;
        _cvModel = cvModel;
        _indexedCvModel = icvModel;
        _varModel = varModel;
        _cvTable = false;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        boolean showItem = false;
        Attribute nameFmt = pane.getAttribute("nameFmt");
        if (nameFmt != null && nameFmt.getValue().equals("item")) {
            log.debug("Pane " + name + " will show items, not labels, from decoder file");
            showItem = true;
        }
        JPanel p = new JPanel();
        panelList.add(p);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        List<Element> colList = pane.getChildren("column");
        for (int i = 0; i < colList.size(); i++) {
            p.add(newColumn(((colList.get(i))), showItem, modelElem));
        }
        List<Element> rowList = pane.getChildren("row");
        for (int i = 0; i < rowList.size(); i++) {
            p.add(newRow(((rowList.get(i))), showItem, modelElem));
        }
        add(Box.createHorizontalGlue());
        add(new JScrollPane(p));
        JPanel bottom = new JPanel();
        panelList.add(p);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
        enableReadButtons();
        readChangesButton.addItemListener(l1 = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    readChangesButton.setText(rbt.getString("ButtonStopReadChangesSheet"));
                    if (container.isBusy() == false) {
                        prepReadPane(true);
                        prepGlassPane(readChangesButton);
                        container.getBusyGlassPane().setVisible(true);
                        readPaneChanges();
                    }
                } else {
                    stopProgramming();
                    readChangesButton.setText(rbt.getString("ButtonReadChangesSheet"));
                    if (container.isBusy()) {
                        readChangesButton.setEnabled(false);
                    }
                }
            }
        });
        readAllButton.addItemListener(l2 = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    readAllButton.setText(rbt.getString("ButtonStopReadSheet"));
                    if (container.isBusy() == false) {
                        prepReadPane(false);
                        prepGlassPane(readAllButton);
                        container.getBusyGlassPane().setVisible(true);
                        readPaneAll();
                    }
                } else {
                    stopProgramming();
                    readAllButton.setText(rbt.getString("ButtonReadFullSheet"));
                    if (container.isBusy()) {
                        readAllButton.setEnabled(false);
                    }
                }
            }
        });
        writeChangesButton.setToolTipText(rbt.getString("TipWriteHighlightedSheet"));
        writeChangesButton.addItemListener(l3 = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    writeChangesButton.setText(rbt.getString("ButtonStopWriteChangesSheet"));
                    if (container.isBusy() == false) {
                        prepWritePane(true);
                        prepGlassPane(writeChangesButton);
                        container.getBusyGlassPane().setVisible(true);
                        writePaneChanges();
                    }
                } else {
                    stopProgramming();
                    writeChangesButton.setText(rbt.getString("ButtonWriteChangesSheet"));
                    if (container.isBusy()) {
                        writeChangesButton.setEnabled(false);
                    }
                }
            }
        });
        writeAllButton.setToolTipText(rbt.getString("TipWriteAllSheet"));
        writeAllButton.addItemListener(l4 = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    writeAllButton.setText(rbt.getString("ButtonStopWriteSheet"));
                    if (container.isBusy() == false) {
                        prepWritePane(false);
                        prepGlassPane(writeAllButton);
                        container.getBusyGlassPane().setVisible(true);
                        writePaneAll();
                    }
                } else {
                    stopProgramming();
                    writeAllButton.setText(rbt.getString("ButtonWriteFullSheet"));
                    if (container.isBusy()) {
                        writeAllButton.setEnabled(false);
                    }
                }
            }
        });
        enableConfirmButtons();
        confirmChangesButton.addItemListener(l5 = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    confirmChangesButton.setText(rbt.getString("ButtonStopConfirmChangesSheet"));
                    if (container.isBusy() == false) {
                        prepConfirmPane(true);
                        prepGlassPane(confirmChangesButton);
                        container.getBusyGlassPane().setVisible(true);
                        confirmPaneChanges();
                    }
                } else {
                    stopProgramming();
                    confirmChangesButton.setText(rbt.getString("ButtonConfirmChangesSheet"));
                    if (container.isBusy()) {
                        confirmChangesButton.setEnabled(false);
                    }
                }
            }
        });
        confirmAllButton.addItemListener(l6 = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    confirmAllButton.setText(rbt.getString("ButtonStopConfirmSheet"));
                    if (container.isBusy() == false) {
                        prepConfirmPane(false);
                        prepGlassPane(confirmAllButton);
                        container.getBusyGlassPane().setVisible(true);
                        confirmPaneAll();
                    }
                } else {
                    stopProgramming();
                    confirmAllButton.setText(rbt.getString("ButtonConfirmFullSheet"));
                    if (container.isBusy()) {
                        confirmAllButton.setEnabled(false);
                    }
                }
            }
        });
        bottom.add(readChangesButton);
        bottom.add(writeChangesButton);
        if (_cvTable) bottom.add(confirmChangesButton);
        bottom.add(readAllButton);
        bottom.add(writeAllButton);
        if (_cvTable) bottom.add(confirmAllButton);
        if (_cvModel.getProgrammer() != null) add(bottom);
    }

    public String getName() {
        return mName;
    }

    public String toString() {
        return getName();
    }

    /**
     * Enable the read all and read changes button if possible.
     * This checks to make sure this is appropriate, given
     * the attached programmer's capability.
     */
    void enableReadButtons() {
        readChangesButton.setToolTipText(rbt.getString("TipReadChangesSheet"));
        readAllButton.setToolTipText(rbt.getString("TipReadAllSheet"));
        if (_cvModel.getProgrammer() != null && !_cvModel.getProgrammer().getCanRead()) {
            readChangesButton.setEnabled(false);
            readAllButton.setEnabled(false);
            readChangesButton.setToolTipText(rbt.getString("TipNoRead"));
            readAllButton.setToolTipText(rbt.getString("TipNoRead"));
        } else {
            readChangesButton.setEnabled(true);
            readAllButton.setEnabled(true);
        }
    }

    /**
     * Enable the compare all and compare changes button if possible.
     * This checks to make sure this is appropriate, given
     * the attached programmer's capability.
     */
    void enableConfirmButtons() {
        confirmChangesButton.setToolTipText(rbt.getString("TipConfirmChangesSheet"));
        confirmAllButton.setToolTipText(rbt.getString("TipConfirmAllSheet"));
        if (_cvModel.getProgrammer() != null && !_cvModel.getProgrammer().getCanRead()) {
            confirmChangesButton.setEnabled(false);
            confirmAllButton.setEnabled(false);
            confirmChangesButton.setToolTipText(rbt.getString("TipNoRead"));
            confirmAllButton.setToolTipText(rbt.getString("TipNoRead"));
        } else {
            confirmChangesButton.setEnabled(true);
            confirmAllButton.setEnabled(true);
        }
    }

    /**
     * This remembers the variables on this pane for the Read/Write sheet
     * operation.  They are stored as a list of Integer objects, each of which
     * is the index of the Variable in the VariableTable.
     */
    List<Integer> varList = new ArrayList<Integer>();

    int varListIndex;

    /**
     * This remembers the CVs on this pane for the Read/Write sheet
     * operation.  They are stored as a list of Integer objects, each of which
     * is the index of the CV in the CVTable. Note that variables are handled
     * separately, and the CVs that are represented by variables are not
     * entered here.  So far (sic), the only use of this is for the cvtable rep.
     */
    List<Integer> cvList = new ArrayList<Integer>();

    int cvListIndex;

    /**
     * This remembers the indexed CVs on this pane for the Read/Write sheet
     * operation.  They are stored as a list of Integer objects, each of which
     * is the index of the indexed CV in the VariableTable. This is done so that
     * we can read/write them as a variable.  So far (sic), the only use of this is
     * for the IndexedCvTable rep.
     */
    List<Integer> indexedCvList = new ArrayList<Integer>();

    int indexedCvListIndex;

    JToggleButton readChangesButton = new JToggleButton(rbt.getString("ButtonReadChangesSheet"));

    JToggleButton readAllButton = new JToggleButton(rbt.getString("ButtonReadFullSheet"));

    JToggleButton writeChangesButton = new JToggleButton(rbt.getString("ButtonWriteChangesSheet"));

    JToggleButton writeAllButton = new JToggleButton(rbt.getString("ButtonWriteFullSheet"));

    JToggleButton confirmChangesButton = new JToggleButton(rbt.getString("ButtonConfirmChangesSheet"));

    JToggleButton confirmAllButton = new JToggleButton(rbt.getString("ButtonConfirmFullSheet"));

    /**
     * Estimate the number of CVs that will be accessed when
     * reading or writing the contents of this pane.
     *
     * @param read true if counting for read, false for write
     * @param changes true if counting for a *Changes operation;
     *          false, if counting for a *All operation
     * @return the total number of CV reads/writes needed for this pane
     */
    public int countOpsNeeded(boolean read, boolean changes) {
        Set<Integer> set = new HashSet<Integer>(cvList.size() + varList.size() + 50);
        return makeOpsNeededSet(read, changes, set).size();
    }

    /**
     * Produce a set of CVs that will be accessed when
     * reading or writing the contents of this pane.
     *
     * @param read true if counting for read, false for write
     * @param changes true if counting for a *Changes operation;
     *          false, if counting for a *All operation
     * @param set The set to fill.  Any CVs already in here will
     *      not be duplicated, which provides a way to aggregate
     *      a set of CVs across multiple panes.
     * @return the same set as the parameter, for convenient
     *      chaining of operations.
     */
    public Set<Integer> makeOpsNeededSet(boolean read, boolean changes, Set<Integer> set) {
        for (int i = 0; i < varList.size(); i++) {
            int varNum = varList.get(i).intValue();
            VariableValue var = _varModel.getVariable(varNum);
            if (!changes || (changes && var.isChanged())) {
                CvValue[] cvs = var.usesCVs();
                for (int j = 0; j < cvs.length; j++) {
                    CvValue cv = cvs[j];
                    if (!changes || VariableValue.considerChanged(cv)) set.add(Integer.valueOf(cv.number()));
                }
            }
        }
        return set;
    }

    private void prepGlassPane(AbstractButton activeButton) {
        container.prepGlassPane(activeButton);
    }

    void enableButtons(boolean stat) {
        if (stat) {
            enableReadButtons();
            enableConfirmButtons();
        } else {
            readChangesButton.setEnabled(stat);
            readAllButton.setEnabled(stat);
            confirmChangesButton.setEnabled(stat);
            confirmAllButton.setEnabled(stat);
        }
        writeChangesButton.setEnabled(stat);
        writeAllButton.setEnabled(stat);
    }

    boolean justChanges;

    /**
     * Invoked by "Read changes on sheet" button, this sets in motion a
     * continuing sequence of "read" operations on the
     * variables & CVs in the Pane.  Only variables in states
     * marked as "changed" will be read.
     *
     * @return true is a read has been started, false if the pane is complete.
     */
    public boolean readPaneChanges() {
        if (log.isDebugEnabled()) log.debug("readPane starts with " + varList.size() + " vars, " + cvList.size() + " cvs " + indexedCvList.size() + " indexed cvs");
        prepReadPane(true);
        return nextRead();
    }

    /**
     * Prepare this pane for a read operation.
     * <P>The read mechanism only reads
     * variables in certain states (and needs to do that to handle error
     * processing right now), so this is implemented by first
     * setting all variables and CVs on this pane to TOREAD via this method
     *
     */
    public void prepReadPane(boolean onlyChanges) {
        if (log.isDebugEnabled()) log.debug("start prepReadPane with onlyChanges=" + onlyChanges);
        justChanges = onlyChanges;
        enableButtons(false);
        if (justChanges == true) {
            readChangesButton.setEnabled(true);
            readChangesButton.setSelected(true);
        } else {
            readAllButton.setSelected(true);
            readAllButton.setEnabled(true);
        }
        if (container.isBusy() == false) {
            container.enableButtons(false);
        }
        setToRead(justChanges, true);
        varListIndex = 0;
        cvListIndex = 0;
        indexedCvListIndex = 0;
    }

    /**
     * Invoked by "Read Full Sheet" button, this sets in motion a
     * continuing sequence of "read" operations on the
     * variables & CVs in the Pane.  The read mechanism only reads
     * variables in certain states (and needs to do that to handle error
     * processing right now), so this is implemented by first
     * setting all variables and CVs on this pane to TOREAD
     * in prepReadPaneAll, then starting the execution.
     *
     * @return true is a read has been started, false if the pane is complete.
     */
    public boolean readPaneAll() {
        if (log.isDebugEnabled()) log.debug("readAllPane starts with " + varList.size() + " vars, " + cvList.size() + " cvs " + indexedCvList.size() + " indexed cvs");
        prepReadPane(false);
        return nextRead();
    }

    /**
     * Set the "ToRead" parameter in all variables and CVs on this pane
     */
    void setToRead(boolean justChanges, boolean startProcess) {
        if (!container.isBusy() || (!startProcess)) {
            for (int i = 0; i < varList.size(); i++) {
                int varNum = varList.get(i).intValue();
                VariableValue var = _varModel.getVariable(varNum);
                if (justChanges) {
                    if (var.isChanged()) {
                        var.setToRead(startProcess);
                    } else {
                        var.setToRead(false);
                    }
                } else {
                    var.setToRead(startProcess);
                }
            }
            for (int i = 0; i < cvList.size(); i++) {
                int cvNum = cvList.get(i).intValue();
                CvValue cv = _cvModel.getCvByRow(cvNum);
                if (justChanges) {
                    if (VariableValue.considerChanged(cv)) {
                        cv.setToRead(startProcess);
                    } else {
                        cv.setToRead(false);
                    }
                } else {
                    cv.setToRead(startProcess);
                }
            }
            for (int i = 0; i < indexedCvList.size(); i++) {
                CvValue icv = _indexedCvModel.getCvByRow(i);
                if (justChanges) {
                    if (VariableValue.considerChanged(icv)) {
                        icv.setToRead(startProcess);
                    } else {
                        icv.setToRead(false);
                    }
                } else {
                    icv.setToRead(startProcess);
                }
            }
        }
    }

    /**
     * Set the "ToWrite" parameter in all variables and CVs on this pane
     */
    void setToWrite(boolean justChanges, boolean startProcess) {
        if (log.isDebugEnabled()) log.debug("start setToWrite method with " + justChanges + "," + startProcess);
        if (!container.isBusy() || (!startProcess)) {
            log.debug("about to start setToWrite of varList");
            for (int i = 0; i < varList.size(); i++) {
                int varNum = varList.get(i).intValue();
                VariableValue var = _varModel.getVariable(varNum);
                if (justChanges) {
                    if (var.isChanged()) {
                        var.setToWrite(startProcess);
                    } else {
                        var.setToWrite(false);
                    }
                } else {
                    var.setToWrite(startProcess);
                }
            }
            log.debug("about to start setToWrite of cvList");
            for (int i = 0; i < cvList.size(); i++) {
                int cvNum = cvList.get(i).intValue();
                CvValue cv = _cvModel.getCvByRow(cvNum);
                if (justChanges) {
                    if (VariableValue.considerChanged(cv)) {
                        cv.setToWrite(startProcess);
                    } else {
                        cv.setToWrite(false);
                    }
                } else {
                    cv.setToWrite(startProcess);
                }
            }
            log.debug("about to start setToWrite of indexedCvList");
            for (int i = 0; i < indexedCvList.size(); i++) {
                CvValue icv = _indexedCvModel.getCvByRow(i);
                if (justChanges) {
                    if (VariableValue.considerChanged(icv)) {
                        icv.setToWrite(startProcess);
                    } else {
                        icv.setToWrite(false);
                    }
                } else {
                    icv.setToWrite(startProcess);
                }
            }
        }
        log.debug("end setToWrite method");
    }

    void executeRead(VariableValue var) {
        setBusy(true);
        if (_programmingVar != null) log.error("listener already set at read start");
        _programmingVar = var;
        _read = true;
        _programmingVar.addPropertyChangeListener(this);
        if (justChanges) {
            _programmingVar.readChanges();
        } else {
            _programmingVar.readAll();
        }
    }

    void executeWrite(VariableValue var) {
        setBusy(true);
        if (_programmingVar != null) log.error("listener already set at write start");
        _programmingVar = var;
        _read = false;
        _programmingVar.addPropertyChangeListener(this);
        if (justChanges) {
            _programmingVar.writeChanges();
        } else {
            _programmingVar.writeAll();
        }
    }

    /**
     * If there are any more read operations to be done on this pane,
     * do the next one.
     * <P>
     * Each invocation of this method reads one variable or CV; completion
     * of that request will cause it to happen again, reading the next one, until
     * there's nothing left to read.
     * <P>
     * @return true is a read has been started, false if the pane is complete.
     */
    boolean nextRead() {
        while ((varList.size() >= 0) && (varListIndex < varList.size())) {
            int varNum = varList.get(varListIndex).intValue();
            int vState = _varModel.getState(varNum);
            VariableValue var = _varModel.getVariable(varNum);
            if (log.isDebugEnabled()) log.debug("nextRead var index " + varNum + " state " + vState + "  label: " + var.label());
            varListIndex++;
            if (var.isToRead() || vState == VariableValue.UNKNOWN) {
                if (log.isDebugEnabled()) log.debug("start read of variable " + _varModel.getLabel(varNum));
                executeRead(var);
                if (log.isDebugEnabled()) log.debug("return from starting var read");
                return true;
            }
        }
        while ((cvList.size() >= 0) && (cvListIndex < cvList.size())) {
            int cvNum = cvList.get(cvListIndex).intValue();
            CvValue cv = _cvModel.getCvByRow(cvNum);
            if (log.isDebugEnabled()) log.debug("nextRead cv index " + cvNum + " state " + cv.getState());
            cvListIndex++;
            if (cv.isToRead() || cv.getState() == CvValue.UNKNOWN) {
                if (log.isDebugEnabled()) log.debug("start read of cv " + cvNum);
                setBusy(true);
                if (_programmingCV != null) log.error("listener already set at read start");
                _programmingCV = _cvModel.getCvByRow(cvNum);
                _read = true;
                _programmingCV.addPropertyChangeListener(this);
                _programmingCV.read(_cvModel.getStatusLabel());
                if (log.isDebugEnabled()) log.debug("return from starting CV read");
                return true;
            }
        }
        while ((indexedCvList.size() >= 0) && (indexedCvListIndex < indexedCvList.size())) {
            int indxVarNum = indexedCvList.get(indexedCvListIndex).intValue();
            int indxState = _varModel.getState(indxVarNum);
            if (log.isDebugEnabled()) log.debug("nextRead indexed cv @ row index " + indexedCvListIndex + " state " + indxState);
            VariableValue iCv = _varModel.getVariable(indxVarNum);
            indexedCvListIndex++;
            if (iCv.isToRead() || indxState == VariableValue.UNKNOWN) {
                String sz = "start read of indexed cv " + (_indexedCvModel.getCvByRow(indexedCvListIndex - 1)).cvName();
                if (log.isDebugEnabled()) log.debug(sz);
                setBusy(true);
                if (_programmingIndexedCV != null) log.error("listener already set at read start");
                _programmingIndexedCV = _varModel.getVariable(indxVarNum);
                _read = true;
                _programmingIndexedCV.addPropertyChangeListener(this);
                _programmingIndexedCV.readAll();
                if (log.isDebugEnabled()) log.debug("return from starting indexed CV read");
                return true;
            }
        }
        if (log.isDebugEnabled()) log.debug("nextRead found nothing to do");
        readChangesButton.setSelected(false);
        readAllButton.setSelected(false);
        setBusy(false);
        container.paneFinished();
        return false;
    }

    /**
     * If there are any more compare operations to be done on this pane,
     * do the next one.
     * <P>
     * Each invocation of this method compare one CV; completion
     * of that request will cause it to happen again, reading the next one, until
     * there's nothing left to read.
     * <P>
     * @return true is a compare has been started, false if the pane is complete.
     */
    boolean nextConfirm() {
        while ((cvList.size() >= 0) && (cvListIndex < cvList.size())) {
            int cvNum = cvList.get(cvListIndex).intValue();
            CvValue cv = _cvModel.getCvByRow(cvNum);
            if (log.isDebugEnabled()) log.debug("nextConfirm cv index " + cvNum + " state " + cv.getState());
            cvListIndex++;
            if (cv.isToRead()) {
                if (log.isDebugEnabled()) log.debug("start confirm of cv " + cvNum);
                setBusy(true);
                if (_programmingCV != null) log.error("listener already set at confirm start");
                _programmingCV = _cvModel.getCvByRow(cvNum);
                _read = true;
                _programmingCV.addPropertyChangeListener(this);
                _programmingCV.confirm(_cvModel.getStatusLabel());
                if (log.isDebugEnabled()) log.debug("return from starting CV confirm");
                return true;
            }
        }
        while ((indexedCvList.size() >= 0) && (indexedCvListIndex < indexedCvList.size())) {
            int indxVarNum = indexedCvList.get(indexedCvListIndex).intValue();
            int indxState = _varModel.getState(indxVarNum);
            if (log.isDebugEnabled()) log.debug("nextRead indexed cv @ row index " + indexedCvListIndex + " state " + indxState);
            VariableValue iCv = _varModel.getVariable(indxVarNum);
            indexedCvListIndex++;
            if (iCv.isToRead()) {
                String sz = "start confirm of indexed cv " + (_indexedCvModel.getCvByRow(indexedCvListIndex - 1)).cvName();
                if (log.isDebugEnabled()) log.debug(sz);
                setBusy(true);
                if (_programmingIndexedCV != null) log.error("listener already set at confirm start");
                _programmingIndexedCV = _varModel.getVariable(indxVarNum);
                _read = true;
                _programmingIndexedCV.addPropertyChangeListener(this);
                _programmingIndexedCV.confirmAll();
                if (log.isDebugEnabled()) log.debug("return from starting indexed CV confirm");
                return true;
            }
        }
        if (log.isDebugEnabled()) log.debug("nextConfirm found nothing to do");
        confirmChangesButton.setSelected(false);
        confirmAllButton.setSelected(false);
        setBusy(false);
        container.paneFinished();
        return false;
    }

    /**
     * Invoked by "Write changes on sheet" button, this sets in motion a
     * continuing sequence of "write" operations on the
     * variables in the Pane.  Only variables in isChanged states
     * are written; other states don't
     * need to be.
     * <P>
     * Returns true if a write has been started, false if the pane is complete.
     */
    public boolean writePaneChanges() {
        if (log.isDebugEnabled()) log.debug("writePaneChanges starts");
        prepWritePane(true);
        boolean val = nextWrite();
        if (log.isDebugEnabled()) log.debug("writePaneChanges returns " + val);
        return val;
    }

    /**
     * Invoked by "Write full sheet" button to write all CVs.
     * <P>
     * Returns true if a write has been started, false if the pane is complete.
     */
    public boolean writePaneAll() {
        prepWritePane(false);
        return nextWrite();
    }

    /**
     * Prepare a "write full sheet" operation.
     */
    public void prepWritePane(boolean onlyChanges) {
        if (log.isDebugEnabled()) log.debug("start prepWritePane with " + onlyChanges);
        justChanges = onlyChanges;
        enableButtons(false);
        if (justChanges == true) {
            writeChangesButton.setEnabled(true);
            writeChangesButton.setSelected(true);
        } else {
            writeAllButton.setSelected(true);
            writeAllButton.setEnabled(true);
        }
        if (container.isBusy() == false) {
            container.enableButtons(false);
        }
        setToWrite(justChanges, true);
        varListIndex = 0;
        cvListIndex = 0;
        indexedCvListIndex = 0;
        log.debug("end prepWritePane");
    }

    boolean nextWrite() {
        log.debug("start nextWrite");
        while ((varList.size() >= 0) && (varListIndex < varList.size())) {
            int varNum = varList.get(varListIndex).intValue();
            int vState = _varModel.getState(varNum);
            VariableValue var = _varModel.getVariable(varNum);
            if (log.isDebugEnabled()) log.debug("nextWrite var index " + varNum + " state " + VariableValue.stateNameFromValue(vState) + " isToWrite: " + var.isToWrite() + " label:" + var.label());
            varListIndex++;
            if (var.isToWrite() || vState == VariableValue.UNKNOWN) {
                log.debug("start write of variable " + _varModel.getLabel(varNum));
                executeWrite(var);
                if (log.isDebugEnabled()) log.debug("return from starting var write");
                return true;
            }
        }
        while ((cvList.size() >= 0) && (cvListIndex < cvList.size())) {
            int cvNum = cvList.get(cvListIndex).intValue();
            CvValue cv = _cvModel.getCvByRow(cvNum);
            if (log.isDebugEnabled()) log.debug("nextWrite cv index " + cvNum + " state " + cv.getState());
            cvListIndex++;
            if (cv.isToWrite() || cv.getState() == CvValue.UNKNOWN) {
                if (log.isDebugEnabled()) log.debug("start write of cv index " + cvNum);
                setBusy(true);
                if (_programmingCV != null) log.error("listener already set at write start");
                _programmingCV = _cvModel.getCvByRow(cvNum);
                _read = false;
                _programmingCV.addPropertyChangeListener(this);
                _programmingCV.write(_cvModel.getStatusLabel());
                if (log.isDebugEnabled()) log.debug("return from starting cv write");
                return true;
            }
        }
        while ((indexedCvList.size() >= 0) && (indexedCvListIndex < indexedCvList.size())) {
            int indxVarNum = indexedCvList.get(indexedCvListIndex).intValue();
            int indxState = _varModel.getState(indxVarNum);
            if (log.isDebugEnabled()) log.debug("nextWrite indexed cv @ row index " + indexedCvListIndex + " state " + indxState);
            VariableValue iCv = _varModel.getVariable(indxVarNum);
            indexedCvListIndex++;
            if (iCv.isToWrite() || indxState == VariableValue.UNKNOWN) {
                String sz = "start write of indexed cv " + (_indexedCvModel.getCvByRow(indexedCvListIndex - 1)).cvName();
                if (log.isDebugEnabled()) log.debug(sz);
                setBusy(true);
                if (_programmingIndexedCV != null) log.error("listener already set at read start");
                _programmingIndexedCV = _varModel.getVariable(indxVarNum);
                _read = true;
                _programmingIndexedCV.addPropertyChangeListener(this);
                _programmingIndexedCV.writeAll();
                if (log.isDebugEnabled()) log.debug("return from starting indexed CV read");
                return true;
            }
        }
        if (log.isDebugEnabled()) log.debug("nextWrite found nothing to do");
        writeChangesButton.setSelected(false);
        writeAllButton.setSelected(false);
        setBusy(false);
        container.paneFinished();
        log.debug("return from nextWrite with nothing to do");
        return false;
    }

    /**
     * Prepare this pane for a compare operation.
     * <P>The read mechanism only reads
     * variables in certain states (and needs to do that to handle error
     * processing right now), so this is implemented by first
     * setting all variables and CVs on this pane to TOREAD via this method
     *
     */
    public void prepConfirmPane(boolean onlyChanges) {
        if (log.isDebugEnabled()) log.debug("start prepReadPane with onlyChanges=" + onlyChanges);
        justChanges = onlyChanges;
        enableButtons(false);
        if (justChanges) {
            confirmChangesButton.setEnabled(true);
            confirmChangesButton.setSelected(true);
        } else {
            confirmAllButton.setSelected(true);
            confirmAllButton.setEnabled(true);
        }
        if (container.isBusy() == false) {
            container.enableButtons(false);
        }
        setToRead(justChanges, true);
        varListIndex = 0;
        cvListIndex = 0;
        indexedCvListIndex = 0;
    }

    /**
     * Invoked by "Compare changes on sheet" button, this sets in motion a
     * continuing sequence of "confirm" operations on the
     * variables & CVs in the Pane.  Only variables in states
     * marked as "changed" will be checked.
     *
     * @return true is a confirm has been started, false if the pane is complete.
     */
    public boolean confirmPaneChanges() {
        if (log.isDebugEnabled()) log.debug("confirmPane starts with " + varList.size() + " vars, " + cvList.size() + " cvs " + indexedCvList.size() + " indexed cvs");
        prepConfirmPane(true);
        return nextConfirm();
    }

    /**
     * Invoked by "Compare Full Sheet" button, this sets in motion a
     * continuing sequence of "confirm" operations on the
     * variables & CVs in the Pane.  The read mechanism only reads
     * variables in certain states (and needs to do that to handle error
     * processing right now), so this is implemented by first
     * setting all variables and CVs on this pane to TOREAD
     * in prepReadPaneAll, then starting the execution.
     *
     * @return true is a confirm has been started, false if the pane is complete.
     */
    public boolean confirmPaneAll() {
        if (log.isDebugEnabled()) log.debug("confirmAllPane starts with " + varList.size() + " vars, " + cvList.size() + " cvs " + indexedCvList.size() + " indexed cvs");
        prepConfirmPane(false);
        return nextConfirm();
    }

    VariableValue _programmingVar = null;

    CvValue _programmingCV = null;

    VariableValue _programmingIndexedCV = null;

    boolean _read = true;

    private boolean _busy = false;

    public boolean isBusy() {
        return _busy;
    }

    protected void setBusy(boolean busy) {
        boolean oldBusy = _busy;
        _busy = busy;
        if (!busy && !container.isBusy()) {
            enableButtons(true);
        }
        if (oldBusy != busy) firePropertyChange("Busy", Boolean.valueOf(oldBusy), Boolean.valueOf(busy));
    }

    private int retry = 0;

    /**
     * Get notification of a variable property change, specifically "busy" going to
     * false at the end of a programming operation. If we're in a programming
     * operation, we then continue it by reinvoking the nextRead/writePane operation.
     */
    public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (_programmingVar == null && _programmingCV == null && _programmingIndexedCV == null) {
            log.warn("unexpected propertChange: " + e);
            return;
        } else if (log.isDebugEnabled()) log.debug("property changed: " + e.getPropertyName() + " new value: " + e.getNewValue());
        if (e.getSource() == _programmingVar && e.getPropertyName().equals("Busy") && ((Boolean) e.getNewValue()).equals(Boolean.FALSE)) {
            if (_programmingVar.getState() == VariableValue.UNKNOWN) {
                if (retry == 0) {
                    varListIndex--;
                    retry++;
                } else {
                    retry = 0;
                }
            }
            replyWhileProgrammingVar();
            return;
        } else if (e.getSource() == _programmingCV && e.getPropertyName().equals("Busy") && ((Boolean) e.getNewValue()).equals(Boolean.FALSE)) {
            if (_programmingCV.getState() == CvValue.UNKNOWN) {
                if (retry == 0) {
                    cvListIndex--;
                    retry++;
                } else {
                    retry = 0;
                }
            }
            replyWhileProgrammingCV();
            return;
        } else if (e.getSource() == _programmingIndexedCV && e.getPropertyName().equals("Busy") && ((Boolean) e.getNewValue()).equals(Boolean.FALSE)) {
            if (_programmingIndexedCV.getState() == VariableValue.UNKNOWN) {
                if (retry == 0) {
                    indexedCvListIndex--;
                    retry++;
                } else {
                    retry = 0;
                }
            }
            replyWhileProgrammingIndxCV();
            return;
        } else {
            if (log.isDebugEnabled() && e.getPropertyName().equals("Busy")) log.debug("ignoring change of Busy " + e.getNewValue() + " " + (((Boolean) e.getNewValue()).equals(Boolean.FALSE)));
            return;
        }
    }

    public void replyWhileProgrammingVar() {
        if (log.isDebugEnabled()) log.debug("correct event for programming variable, restart operation");
        _programmingVar.removePropertyChangeListener(this);
        _programmingVar = null;
        restartProgramming();
    }

    public void replyWhileProgrammingCV() {
        if (log.isDebugEnabled()) log.debug("correct event for programming CV, restart operation");
        _programmingCV.removePropertyChangeListener(this);
        _programmingCV = null;
        restartProgramming();
    }

    public void replyWhileProgrammingIndxCV() {
        if (log.isDebugEnabled()) log.debug("correct event for programming Indexed CV, restart operation");
        _programmingIndexedCV.removePropertyChangeListener(this);
        _programmingIndexedCV = null;
        restartProgramming();
    }

    void restartProgramming() {
        log.debug("start restartProgramming");
        if (_read && readChangesButton.isSelected()) nextRead(); else if (_read && readAllButton.isSelected()) nextRead(); else if (_read && confirmChangesButton.isSelected()) nextConfirm(); else if (_read && confirmAllButton.isSelected()) nextConfirm(); else if (writeChangesButton.isSelected()) nextWrite(); else if (writeAllButton.isSelected()) nextWrite(); else {
            if (log.isDebugEnabled()) log.debug("No operation to restart");
            if (isBusy()) {
                container.paneFinished();
                setBusy(false);
            }
        }
        log.debug("end restartProgramming");
    }

    void stopProgramming() {
        log.debug("start stopProgramming");
        setToRead(false, false);
        setToWrite(false, false);
        varListIndex = varList.size();
        cvListIndex = cvList.size();
        indexedCvListIndex = indexedCvList.size();
        log.debug("end stopProgramming");
    }

    /**
     * Create a single column from the JDOM column Element
     */
    @SuppressWarnings("unchecked")
    public JPanel newColumn(Element element, boolean showStdName, Element modelElem) {
        JPanel c = new JPanel();
        panelList.add(c);
        GridBagLayout g = new GridBagLayout();
        GridBagConstraints cs = new GridBagConstraints();
        c.setLayout(g);
        List<Element> elemList = element.getChildren();
        if (log.isDebugEnabled()) log.debug("newColumn starting with " + elemList.size() + " elements");
        for (int i = 0; i < elemList.size(); i++) {
            cs.gridy++;
            cs.gridx = 0;
            Element e = (elemList.get(i));
            String name = e.getName();
            if (log.isDebugEnabled()) log.debug("newColumn processing " + name + " element");
            if (name.equals("display")) {
                newVariable(e, c, g, cs, showStdName);
            } else if (name.equals("separator")) {
                JSeparator j = new JSeparator(javax.swing.SwingConstants.HORIZONTAL);
                cs.fill = GridBagConstraints.BOTH;
                cs.gridwidth = GridBagConstraints.REMAINDER;
                g.setConstraints(j, cs);
                c.add(j);
                cs.gridwidth = 1;
            } else if (name.equals("label")) {
                JLabel l = new JLabel(e.getAttribute("label").getValue());
                l.setAlignmentX(1.0f);
                cs.fill = GridBagConstraints.BOTH;
                cs.gridwidth = GridBagConstraints.REMAINDER;
                if (log.isDebugEnabled()) {
                    log.debug("Add label: " + l.getText() + " cs: " + cs.gridwidth + " " + cs.fill + " " + cs.gridx + " " + cs.gridy);
                }
                g.setConstraints(l, cs);
                c.add(l);
                cs.fill = GridBagConstraints.NONE;
                cs.gridwidth = 1;
            } else if (name.equals("cvtable")) {
                log.debug("starting to build CvTable pane");
                JTable cvTable = new JTable(_cvModel);
                JScrollPane cvScroll = new JScrollPane(cvTable);
                cvTable.setDefaultRenderer(JTextField.class, new ValueRenderer());
                cvTable.setDefaultRenderer(JButton.class, new ValueRenderer());
                cvTable.setDefaultEditor(JTextField.class, new ValueEditor());
                cvTable.setDefaultEditor(JButton.class, new ValueEditor());
                cvTable.setRowHeight(new JButton("X").getPreferredSize().height);
                cvScroll.setColumnHeaderView(cvTable.getTableHeader());
                cvTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                cs.gridwidth = GridBagConstraints.REMAINDER;
                g.setConstraints(cvScroll, cs);
                c.add(cvScroll);
                cs.gridwidth = 1;
                for (int j = 0; j < _cvModel.getRowCount(); j++) {
                    cvList.add(Integer.valueOf(j));
                }
                _cvTable = true;
                log.debug("end of building CvTable pane");
            } else if (name.equals("indxcvtable")) {
                log.debug("starting to build IndexedCvTable pane");
                JTable indxcvTable = new JTable(_indexedCvModel);
                JScrollPane cvScroll = new JScrollPane(indxcvTable);
                indxcvTable.setDefaultRenderer(JTextField.class, new ValueRenderer());
                indxcvTable.setDefaultRenderer(JButton.class, new ValueRenderer());
                indxcvTable.setDefaultEditor(JTextField.class, new ValueEditor());
                indxcvTable.setDefaultEditor(JButton.class, new ValueEditor());
                indxcvTable.setRowHeight(new JButton("X").getPreferredSize().height);
                indxcvTable.setPreferredScrollableViewportSize(new Dimension(700, indxcvTable.getRowHeight() * 14));
                cvScroll.setColumnHeaderView(indxcvTable.getTableHeader());
                cs.gridwidth = GridBagConstraints.REMAINDER;
                g.setConstraints(cvScroll, cs);
                c.add(cvScroll);
                cs.gridwidth = 1;
                for (int j = 0; j < _indexedCvModel.getRowCount(); j++) {
                    String sz = "CV" + _indexedCvModel.getName(j);
                    int in = _varModel.findVarIndex(sz);
                    indexedCvList.add(Integer.valueOf(in));
                }
                _cvTable = true;
                log.debug("end of building IndexedCvTable pane");
            } else if (name.equals("fnmapping")) {
                FnMapPanel l = new FnMapPanel(_varModel, varList, modelElem);
                fnMapList.add(l);
                cs.gridwidth = GridBagConstraints.REMAINDER;
                g.setConstraints(l, cs);
                c.add(l);
                cs.gridwidth = 1;
            } else if (name.equals("dccaddress")) {
                JPanel l = addDccAddressPanel(e);
                cs.gridwidth = GridBagConstraints.REMAINDER;
                g.setConstraints(l, cs);
                c.add(l);
                cs.gridwidth = 1;
            } else if (name.equals("row")) {
                cs.gridwidth = GridBagConstraints.REMAINDER;
                JPanel l = newRow(e, showStdName, modelElem);
                panelList.add(l);
                g.setConstraints(l, cs);
                c.add(l);
                cs.gridwidth = 1;
            } else {
                log.error("No code to handle element of type " + e.getName() + " in newColumn");
            }
        }
        c.add(Box.createVerticalGlue());
        return c;
    }

    /**
     * Create a single row from the JDOM column Element
     */
    @SuppressWarnings("unchecked")
    public JPanel newRow(Element element, boolean showStdName, Element modelElem) {
        JPanel c = new JPanel();
        panelList.add(c);
        GridBagLayout g = new GridBagLayout();
        GridBagConstraints cs = new GridBagConstraints();
        c.setLayout(g);
        List<Element> elemList = element.getChildren();
        if (log.isDebugEnabled()) log.debug("newRow starting with " + elemList.size() + " elements");
        for (int i = 0; i < elemList.size(); i++) {
            cs.gridy = 0;
            cs.gridx++;
            Element e = elemList.get(i);
            String name = e.getName();
            if (log.isDebugEnabled()) log.debug("newRow processing " + name + " element");
            if (name.equals("display")) {
                newVariable(e, c, g, cs, showStdName);
            } else if (name.equals("separator")) {
                JSeparator j = new JSeparator(javax.swing.SwingConstants.VERTICAL);
                cs.fill = GridBagConstraints.BOTH;
                cs.gridheight = GridBagConstraints.REMAINDER;
                g.setConstraints(j, cs);
                c.add(j);
                cs.fill = GridBagConstraints.NONE;
                cs.gridheight = 1;
            } else if (name.equals("label")) {
                JLabel l = new JLabel(LocaleSelector.getAttribute(e, "label"));
                l.setAlignmentX(1.0f);
                cs.gridheight = GridBagConstraints.REMAINDER;
                g.setConstraints(l, cs);
                c.add(l);
                cs.gridheight = 1;
            } else if (name.equals("cvtable")) {
                log.debug("starting to build CvTable pane");
                JTable cvTable = new JTable(_cvModel);
                JScrollPane cvScroll = new JScrollPane(cvTable);
                cvTable.setDefaultRenderer(JTextField.class, new ValueRenderer());
                cvTable.setDefaultRenderer(JButton.class, new ValueRenderer());
                cvTable.setDefaultEditor(JTextField.class, new ValueEditor());
                cvTable.setDefaultEditor(JButton.class, new ValueEditor());
                cvTable.setRowHeight(new JButton("X").getPreferredSize().height);
                cvScroll.setColumnHeaderView(cvTable.getTableHeader());
                cvTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                cs.gridheight = GridBagConstraints.REMAINDER;
                g.setConstraints(cvScroll, cs);
                c.add(cvScroll);
                cs.gridheight = 1;
                for (int j = 0; j < _cvModel.getRowCount(); j++) {
                    cvList.add(Integer.valueOf(j));
                }
                _cvTable = true;
                log.debug("end of building CvTable pane");
            } else if (name.equals("indxcvtable")) {
                log.debug("starting to build IndexedCvTable pane");
                JTable indxcvTable = new JTable(_indexedCvModel);
                JScrollPane cvScroll = new JScrollPane(indxcvTable);
                indxcvTable.setDefaultRenderer(JTextField.class, new ValueRenderer());
                indxcvTable.setDefaultRenderer(JButton.class, new ValueRenderer());
                indxcvTable.setDefaultEditor(JTextField.class, new ValueEditor());
                indxcvTable.setDefaultEditor(JButton.class, new ValueEditor());
                indxcvTable.setRowHeight(new JButton("X").getPreferredSize().height);
                indxcvTable.setPreferredScrollableViewportSize(new Dimension(700, indxcvTable.getRowHeight() * 14));
                cvScroll.setColumnHeaderView(indxcvTable.getTableHeader());
                cs.gridwidth = GridBagConstraints.REMAINDER;
                g.setConstraints(cvScroll, cs);
                c.add(cvScroll);
                cs.gridwidth = 1;
                for (int j = 0; j < _indexedCvModel.getRowCount(); j++) {
                    String sz = "CV" + _indexedCvModel.getName(j);
                    int in = _varModel.findVarIndex(sz);
                    indexedCvList.add(Integer.valueOf(in));
                }
                _cvTable = true;
                log.debug("end of building IndexedCvTable pane");
            } else if (name.equals("fnmapping")) {
                FnMapPanel l = new FnMapPanel(_varModel, varList, modelElem);
                fnMapList.add(l);
                cs.gridheight = GridBagConstraints.REMAINDER;
                g.setConstraints(l, cs);
                c.add(l);
                cs.gridheight = 1;
            } else if (name.equals("dccaddress")) {
                JPanel l = addDccAddressPanel(e);
                cs.gridheight = GridBagConstraints.REMAINDER;
                g.setConstraints(l, cs);
                c.add(l);
                cs.gridheight = 1;
            } else if (name.equals("column")) {
                cs.gridheight = GridBagConstraints.REMAINDER;
                JPanel l = newColumn(e, showStdName, modelElem);
                panelList.add(l);
                g.setConstraints(l, cs);
                c.add(l);
                cs.gridheight = 1;
            } else {
                log.error("No code to handle element of type " + e.getName() + " in newRow");
            }
        }
        c.add(Box.createVerticalGlue());
        return c;
    }

    /**
     * Add the representation of a single variable.  The
     * variable is defined by a JDOM variable Element from the XML file.
     */
    public void newVariable(Element var, JComponent col, GridBagLayout g, GridBagConstraints cs, boolean showStdName) {
        String name = var.getAttribute("item").getValue();
        int i = _varModel.findVarIndex(name);
        if (i < 0) {
            if (log.isDebugEnabled()) log.debug("Variable \"" + name + "\" not found, omitted");
            return;
        }
        Attribute attr;
        String layout = "left";
        if ((attr = var.getAttribute("layout")) != null && attr.getValue() != null) layout = attr.getValue();
        String label = name;
        if (!showStdName) {
            label = _varModel.getLabel(i);
        }
        String temp = LocaleSelector.getAttribute(var, "label");
        if (temp != null) label = temp;
        JComponent rep = getRepresentation(name, var);
        if (i >= 0) varList.add(Integer.valueOf(i));
        JLabel l = new WatchingLabel(" " + label + " ", rep);
        if (layout.equals("left")) {
            cs.anchor = GridBagConstraints.EAST;
            g.setConstraints(l, cs);
            col.add(l);
            cs.gridx = GridBagConstraints.RELATIVE;
            cs.anchor = GridBagConstraints.WEST;
            g.setConstraints(rep, cs);
            col.add(rep);
        } else if (layout.equals("right")) {
            cs.anchor = GridBagConstraints.EAST;
            g.setConstraints(rep, cs);
            col.add(rep);
            cs.gridx = GridBagConstraints.RELATIVE;
            cs.anchor = GridBagConstraints.WEST;
            g.setConstraints(l, cs);
            col.add(l);
        } else if (layout.equals("below")) {
            cs.anchor = GridBagConstraints.CENTER;
            g.setConstraints(rep, cs);
            col.add(rep);
            cs.gridy++;
            cs.anchor = GridBagConstraints.WEST;
            g.setConstraints(l, cs);
            col.add(l);
        } else if (layout.equals("above")) {
            cs.anchor = GridBagConstraints.WEST;
            g.setConstraints(l, cs);
            col.add(l);
            cs.gridy++;
            cs.anchor = GridBagConstraints.CENTER;
            g.setConstraints(rep, cs);
            col.add(rep);
        } else {
            log.error("layout internally inconsistent: " + layout);
            return;
        }
    }

    /**
     * Get a GUI representation of a particular variable for display.
     * @param name Name used to look up the Variable object
     * @param var XML Element which might contain a "format" attribute to be used in the {@link VariableValue#getNewRep} call
     * from the Variable object; "tooltip" elements are also processed here.
     * @return JComponent representing this variable
     */
    public JComponent getRepresentation(String name, Element var) {
        int i = _varModel.findVarIndex(name);
        JComponent rep = null;
        String format = "default";
        Attribute attr;
        if ((attr = var.getAttribute("format")) != null && attr.getValue() != null) format = attr.getValue();
        if (i >= 0) {
            rep = getRep(i, format);
            rep.setMaximumSize(rep.getPreferredSize());
            if ((attr = var.getAttribute("tooltip")) != null && attr.getValue() != null && rep.getToolTipText() == null) rep.setToolTipText(attr.getValue());
        }
        return rep;
    }

    JComponent getRep(int i, String format) {
        return (JComponent) (_varModel.getRep(i, format));
    }

    /** list of fnMapping objects to dispose */
    ArrayList<FnMapPanel> fnMapList = new ArrayList<FnMapPanel>();

    /** list of JPanel objects to removeAll */
    ArrayList<JPanel> panelList = new ArrayList<JPanel>();

    public void dispose() {
        if (log.isDebugEnabled()) log.debug("dispose");
        removeAll();
        readChangesButton.removeItemListener(l1);
        readAllButton.removeItemListener(l2);
        writeChangesButton.removeItemListener(l3);
        writeAllButton.removeItemListener(l4);
        confirmChangesButton.removeItemListener(l5);
        confirmAllButton.removeItemListener(l6);
        l1 = l2 = l3 = l4 = l5 = l6 = null;
        if (_programmingVar != null) _programmingVar.removePropertyChangeListener(this);
        if (_programmingCV != null) _programmingCV.removePropertyChangeListener(this);
        if (_programmingIndexedCV != null) _programmingIndexedCV.removePropertyChangeListener(this);
        _programmingVar = null;
        _programmingCV = null;
        _programmingIndexedCV = null;
        varList.clear();
        varList = null;
        cvList.clear();
        cvList = null;
        indexedCvList.clear();
        indexedCvList = null;
        for (int i = 0; i < panelList.size(); i++) {
            panelList.get(i).removeAll();
        }
        panelList.clear();
        panelList = null;
        for (int i = 0; i < fnMapList.size(); i++) {
            fnMapList.get(i).dispose();
        }
        fnMapList.clear();
        fnMapList = null;
        readChangesButton = null;
        writeChangesButton = null;
        _cvModel = null;
        _indexedCvModel = null;
        _varModel = null;
    }

    public boolean includeInPrint() {
        return print;
    }

    public void includeInPrint(boolean inc) {
        print = inc;
    }

    boolean print = false;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "SBSC_USE_STRINGBUFFER_CONCATENATION")
    public void printPane(HardcopyWriter w) {
        if (varList.size() == 0 && cvList.size() == 0) return;
        int col1Width = w.getCharactersPerLine() / 2 - 3 - 5;
        int col2Width = w.getCharactersPerLine() / 2 - 3 + 5;
        try {
            String spaces = "";
            for (int i = 0; i < col1Width; i++) {
                spaces = spaces + " ";
            }
            String heading1 = rbt.getString("PrintHeadingField");
            String heading2 = rbt.getString("PrintHeadingSetting");
            String s;
            int interval = spaces.length() - heading1.length();
            w.setFontStyle(Font.BOLD);
            s = mName.toUpperCase();
            w.write(s, 0, s.length());
            w.writeBorders();
            w.write(w.getCurrentLineNumber(), 0, w.getCurrentLineNumber(), w.getCharactersPerLine() + 1);
            s = "\n";
            w.write(s, 0, s.length());
            if (cvList.size() == 0) {
                w.setFontStyle(Font.BOLD + Font.ITALIC);
                s = "   " + heading1 + spaces.substring(0, interval) + "   " + heading2;
                w.write(s, 0, s.length());
                w.writeBorders();
                s = "\n";
                w.write(s, 0, s.length());
            }
            w.setFontStyle(Font.PLAIN);
            Vector<String> printedVariables = new Vector<String>(10, 5);
            for (int i = 0; i < varList.size(); i++) {
                int varNum = varList.get(i).intValue();
                VariableValue var = _varModel.getVariable(varNum);
                String name = var.label();
                if (name == null) name = var.item();
                boolean alreadyPrinted = false;
                for (int j = 0; j < printedVariables.size(); j++) {
                    if (printedVariables.elementAt(j).toString() == name) alreadyPrinted = true;
                }
                if (alreadyPrinted == true) continue;
                printedVariables.addElement(name);
                String value = var.getTextValue();
                String originalName = name;
                String originalValue = value;
                name = name + " (CV" + var.getCvNum() + ")";
                int nameLeftIndex = 0;
                int nameRightIndex = name.length();
                int valueLeftIndex = 0;
                int valueRightIndex = value.length();
                String trimmedName;
                String trimmedValue;
                while ((valueLeftIndex < value.length()) || (nameLeftIndex < name.length())) {
                    if (name.substring(nameLeftIndex).length() > col1Width) {
                        for (int j = 0; j < col1Width; j++) {
                            String delimiter = name.substring(nameLeftIndex + col1Width - j - 1, nameLeftIndex + col1Width - j);
                            if (delimiter.equals(" ") || delimiter.equals(";") || delimiter.equals(",")) {
                                nameRightIndex = nameLeftIndex + col1Width - j;
                                break;
                            }
                        }
                        trimmedName = name.substring(nameLeftIndex, nameRightIndex);
                        nameLeftIndex = nameRightIndex;
                        int space = spaces.length() - trimmedName.length();
                        s = "   " + trimmedName + spaces.substring(0, space);
                    } else {
                        trimmedName = name.substring(nameLeftIndex);
                        int space = spaces.length() - trimmedName.length();
                        s = "   " + trimmedName + spaces.substring(0, space);
                        name = "";
                        nameLeftIndex = 0;
                    }
                    if (value.substring(valueLeftIndex).length() > col2Width) {
                        for (int j = 0; j < col2Width; j++) {
                            String delimiter = value.substring(valueLeftIndex + col2Width - j - 1, valueLeftIndex + col2Width - j);
                            if (delimiter.equals(" ") || delimiter.equals(";") || delimiter.equals(",")) {
                                valueRightIndex = valueLeftIndex + col2Width - j;
                                break;
                            }
                        }
                        trimmedValue = value.substring(valueLeftIndex, valueRightIndex);
                        valueLeftIndex = valueRightIndex;
                        s = s + "   " + trimmedValue;
                    } else {
                        trimmedValue = value.substring(valueLeftIndex);
                        s = s + "   " + trimmedValue;
                        valueLeftIndex = 0;
                        value = "";
                    }
                    w.write(s, 0, s.length());
                    w.writeBorders();
                    s = "\n";
                    w.write(s, 0, s.length());
                }
                float v = Float.valueOf(java.lang.System.getProperty("java.version").substring(0, 3)).floatValue();
                if (originalName.equals("Speed Table") && v < 1.5) {
                    int speedFrameLineHeight = 11;
                    s = "\n";
                    int pageSize = w.getLinesPerPage();
                    int here = w.getCurrentLineNumber();
                    if (pageSize - here < speedFrameLineHeight) {
                        for (int j = 0; j < (pageSize - here); j++) {
                            w.writeBorders();
                            w.write(s, 0, s.length());
                        }
                    }
                    JWindow speedWindow = new JWindow();
                    speedWindow.setSize(512, 165);
                    speedWindow.getContentPane().setBackground(Color.white);
                    speedWindow.getContentPane().setLayout(null);
                    StringTokenizer valueTokens = new StringTokenizer(originalValue, ",", false);
                    int speedVals[] = new int[28];
                    int k = 0;
                    while (valueTokens.hasMoreTokens()) {
                        speedVals[k] = Integer.parseInt(valueTokens.nextToken());
                        k++;
                    }
                    for (int j = 0; j < 28; j++) {
                        JProgressBar printerBar = new JProgressBar(JProgressBar.VERTICAL, 0, 127);
                        printerBar.setBounds(52 + j * 15, 19, 10, 127);
                        printerBar.setValue(speedVals[j] / 2);
                        printerBar.setBackground(Color.white);
                        printerBar.setForeground(Color.darkGray);
                        printerBar.setBorder(BorderFactory.createLineBorder(Color.black));
                        speedWindow.getContentPane().add(printerBar);
                        JLabel barValLabel = new JLabel(Integer.toString(speedVals[j]), SwingConstants.CENTER);
                        barValLabel.setBounds(50 + j * 15, 4, 15, 15);
                        barValLabel.setFont(new java.awt.Font("Monospaced", 0, 7));
                        speedWindow.getContentPane().add(barValLabel);
                        JLabel barCvLabel = new JLabel(Integer.toString(67 + j), SwingConstants.CENTER);
                        barCvLabel.setBounds(50 + j * 15, 150, 15, 15);
                        barCvLabel.setFont(new java.awt.Font("Monospaced", 0, 7));
                        speedWindow.getContentPane().add(barCvLabel);
                    }
                    JLabel cvLabel = new JLabel("Value");
                    cvLabel.setFont(new java.awt.Font("Monospaced", 0, 7));
                    cvLabel.setBounds(25, 4, 26, 15);
                    speedWindow.getContentPane().add(cvLabel);
                    JLabel valueLabel = new JLabel("CV");
                    valueLabel.setFont(new java.awt.Font("Monospaced", 0, 7));
                    valueLabel.setBounds(37, 150, 13, 15);
                    speedWindow.getContentPane().add(valueLabel);
                    w.write(speedWindow);
                    for (int j = 0; j < speedFrameLineHeight; j++) {
                        w.writeBorders();
                        w.write(s, 0, s.length());
                    }
                }
            }
            if (cvList.size() > 0) {
                int cvCount = cvList.size();
                w.setFontStyle(Font.BOLD);
                s = "         Value               Value               Value               Value";
                w.write(s, 0, s.length());
                w.writeBorders();
                s = "\n";
                w.write(s, 0, s.length());
                s = "   CV   Dec Hex        CV   Dec Hex        CV   Dec Hex        CV   Dec Hex";
                w.write(s, 0, s.length());
                w.writeBorders();
                s = "\n";
                w.write(s, 0, s.length());
                w.setFontStyle(0);
                int tableHeight = cvCount / 4;
                if (cvCount % 4 > 0) tableHeight++;
                String[] cvStrings = new String[4 * tableHeight];
                for (int j = 0; j < cvStrings.length; j++) cvStrings[j] = "";
                for (int i = 0; i < cvList.size(); i++) {
                    int cvNum = cvList.get(i).intValue();
                    CvValue cv = _cvModel.getCvByRow(cvNum);
                    int num = cv.number();
                    int value = cv.getValue();
                    String numString = Integer.toString(num);
                    String valueString = Integer.toString(value);
                    String valueStringHex = Integer.toHexString(value).toUpperCase();
                    if (value < 16) valueStringHex = "0" + valueStringHex;
                    for (int j = 1; j < 3; j++) {
                        if (numString.length() < 3) numString = " " + numString;
                    }
                    for (int j = 1; j < 3; j++) {
                        if (valueString.length() < 3) valueString = " " + valueString;
                    }
                    s = "  " + numString + "   " + valueString + "  " + valueStringHex + " ";
                    cvStrings[i] = s;
                }
                String temp;
                boolean swap = false;
                do {
                    swap = false;
                    for (int i = 0; i < _cvModel.getRowCount() - 1; i++) {
                        if (Integer.parseInt(cvStrings[i + 1].substring(2, 5).trim()) < Integer.parseInt(cvStrings[i].substring(2, 5).trim())) {
                            temp = cvStrings[i + 1];
                            cvStrings[i + 1] = cvStrings[i];
                            cvStrings[i] = temp;
                            swap = true;
                        }
                    }
                } while (swap == true);
                for (int i = 0; i < tableHeight; i++) {
                    s = cvStrings[i] + "    " + cvStrings[i + tableHeight] + "    " + cvStrings[i + tableHeight * 2] + "    " + cvStrings[i + tableHeight * 3];
                    w.write(s, 0, s.length());
                    w.writeBorders();
                    s = "\n";
                    w.write(s, 0, s.length());
                }
            }
            s = "\n";
            w.writeBorders();
            w.write(s, 0, s.length());
            w.writeBorders();
            w.write(s, 0, s.length());
        } catch (IOException e) {
            log.warn("error during printing: " + e);
        }
    }

    private JPanel addDccAddressPanel(Element e) {
        JPanel l;
        String at = LocaleSelector.getAttribute(e, "label");
        if (at != null) l = new DccAddressPanel(_varModel, at); else l = new DccAddressPanel(_varModel);
        panelList.add(l);
        int iVar;
        iVar = _varModel.findVarIndex("Short Address");
        if (iVar >= 0) varList.add(Integer.valueOf(iVar)); else log.debug("addDccAddressPanel did not find Short Address");
        iVar = _varModel.findVarIndex("Address Format");
        if (iVar >= 0) varList.add(Integer.valueOf(iVar)); else log.debug("addDccAddressPanel did not find Address Format");
        iVar = _varModel.findVarIndex("Long Address");
        if (iVar >= 0) varList.add(Integer.valueOf(iVar)); else log.debug("addDccAddressPanel did not find Long Address");
        iVar = _varModel.findVarIndex("Consist Address");
        if (iVar >= 0) varList.add(Integer.valueOf(iVar)); else log.debug("addDccAddressPanel did not find CV19 Consist Address");
        return l;
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PaneProgPane.class.getName());
}
