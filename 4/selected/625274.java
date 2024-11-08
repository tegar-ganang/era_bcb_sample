package org.gromurph.javascore.model;

import org.gromurph.javascore.manager.RegattaManager;
import org.gromurph.util.Util;

/**
 * Unit test scripts for Regatta class
 */
public class RegattaTests extends org.gromurph.util.UtilTestCase {

    private static String BASEDIR = Util.getWorkingDirectory() + "/testregattas/";

    double ERR_MARGIN = 0.00001;

    public void test101() {
        assertTrue(doFullCheck("0000-Test-v101.regatta"));
    }

    public void test102Swedish() {
        assertTrue(doFullCheck("102-Swedish-test.regatta"));
    }

    public void test192() {
        assertTrue(doFullCheck("0000-Test-v192.regatta"));
    }

    public void test201() {
        assertTrue(doFullCheck("0000-Test-v201.regatta"));
    }

    public boolean doFullCheck(String rname) {
        int i = 0;
        try {
            Regatta baseline = RegattaManager.readRegattaFromDisk(BASEDIR, rname);
            assertNotNull("Unable to read: " + rname, baseline);
            Regatta tester = RegattaManager.readRegattaFromDisk(BASEDIR, rname);
            assertNotNull("Unable to read: " + rname, tester);
            String n = baseline.getName();
            i++;
            assertEquals(n + " init equals", tester, baseline);
            i++;
            assertEquals(n + " equals clone()", tester, tester.clone());
            i++;
            tester.scoreRegatta();
            readwriteCheck(tester);
            i++;
            readwriteCheck(baseline);
            return true;
        } catch (Exception e) {
            System.out.println();
            System.out.print("Exception in RegattaTests.doFullCheck( rname=");
            System.out.print(rname);
            System.out.println("), step=" + i);
            e.printStackTrace(System.out);
            return false;
        }
    }

    protected void readwriteCheck(Regatta reg) throws java.io.IOException {
        new RegattaManager(reg).writeRegattaToDisk(Util.getWorkingDirectory(), "test.regatta");
        Regatta reg2 = RegattaManager.readRegattaFromDisk(Util.getWorkingDirectory(), "test.regatta");
        assertEquals("readwrite " + reg.getName(), reg, reg2);
    }

    public RegattaTests(String name) {
        super(name);
    }
}
