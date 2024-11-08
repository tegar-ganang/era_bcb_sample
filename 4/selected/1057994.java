package jmri.jmrit.symbolicprog;

import javax.swing.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.Assert;
import java.util.Vector;

/**
 * Test the HexVariableValue class
 *
 * @author	Bob Jacobsen  Copyright 2001
 * @version     $Revision: 17977 $
 */
public class HexVariableValueTest extends VariableValueTest {

    VariableValue makeVar(String label, String comment, String cvName, boolean readOnly, boolean infoOnly, boolean writeOnly, boolean opsOnly, int cvNum, String mask, int minVal, int maxVal, Vector<CvValue> v, JLabel status, String item) {
        return new HexVariableValue(label, comment, cvName, readOnly, infoOnly, writeOnly, opsOnly, cvNum, mask, minVal, maxVal, v, status, item);
    }

    void setValue(VariableValue var, String val) {
        ((JTextField) var.getCommonRep()).setText(val);
        ((JTextField) var.getCommonRep()).postActionEvent();
    }

    void setReadOnlyValue(VariableValue var, String val) {
        ((HexVariableValue) var).setValue(Integer.valueOf(val).intValue());
    }

    void checkValue(VariableValue var, String comment, String val) {
        String hexval = Integer.toHexString(Integer.valueOf(val).intValue());
        Assert.assertEquals(comment, hexval, ((JTextField) var.getCommonRep()).getText());
    }

    void checkReadOnlyValue(VariableValue var, String comment, String val) {
        String hexval = Integer.toHexString(Integer.valueOf(val).intValue());
        Assert.assertEquals(comment, hexval, ((JLabel) var.getCommonRep()).getText());
    }

    public HexVariableValueTest(String s) {
        super(s);
    }

    public static void main(String[] args) {
        String[] testCaseName = { "-noloading", HexVariableValueTest.class.getName() };
        junit.swingui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(HexVariableValueTest.class);
        return suite;
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HexVariableValueTest.class.getName());
}
