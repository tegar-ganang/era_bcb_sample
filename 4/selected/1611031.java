package jmri.jmrit.symbolicprog;

import javax.swing.*;
import java.util.Vector;

/**
 * Representation of a short address (CV1).
 * <P>
 * This is a decimal value, extended to modify the other CVs when
 * written.  The CVs to be modified and there new values are
 * stored in two arrays for simplicity.
 * <P>
 * 
 * The NMRA has decided that writing CV1 causes other CVs to update
 * within the decoder (CV19 for consisting, CV29 for short/long
 * address). We want DP to overwrite those _after_ writing CV1,
 * so that the DP values are forced to be the correct ones.
 * 
 * @author	    Bob Jacobsen   Copyright (C) 2001, 2006, 2007
 * @version     $Revision: 1.15 $
 *
 */
public class ShortAddrVariableValue extends DecVariableValue {

    public ShortAddrVariableValue(String name, String comment, String cvName, boolean readOnly, boolean infoOnly, boolean writeOnly, boolean opsOnly, int cvNum, String mask, Vector<CvValue> v, JLabel status, String stdname) {
        super(name, comment, cvName, readOnly, infoOnly, writeOnly, opsOnly, cvNum, mask, 1, 127, v, status, stdname);
        firstFreeSpace = 0;
        setModifiedCV(19);
        setModifiedCV(29);
    }

    /**
     * Register a CV to be modified regardless of
     * current value
     */
    public void setModifiedCV(int cvNum) {
        if (firstFreeSpace >= maxCVs) {
            log.error("too many CVs registered for changes!");
            return;
        }
        cvNumbers[firstFreeSpace] = cvNum;
        newValues[firstFreeSpace] = -10;
        firstFreeSpace++;
    }

    /**
     * Change CV values due to change in short address
     */
    private void updateCvForAddrChange() {
        for (int i = 0; i < firstFreeSpace; i++) {
            CvValue cv = _cvVector.elementAt(cvNumbers[i]);
            if (cv == null) continue;
            if (cvNumbers[i] != cv.number()) log.error("CV numbers don't match: " + cvNumbers[i] + " " + cv.number());
            cv.setToWrite(true);
            cv.setState(EDITED);
            if (log.isDebugEnabled()) log.debug("Mark to write " + cv.number());
        }
    }

    int firstFreeSpace = 0;

    static final int maxCVs = 20;

    int[] cvNumbers = new int[maxCVs];

    int[] newValues = new int[maxCVs];

    public void writeChanges() {
        if (getReadOnly()) log.error("unexpected writeChanges operation when readOnly is set");
        setBusy(true);
        updateCvForAddrChange();
        _cvVector.elementAt(getCvNum()).write(_status);
    }

    public void writeAll() {
        if (getReadOnly()) log.error("unexpected writeAll operation when readOnly is set");
        setBusy(true);
        updateCvForAddrChange();
        _cvVector.elementAt(getCvNum()).write(_status);
    }

    public void dispose() {
        super.dispose();
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ShortAddrVariableValue.class.getName());
}
