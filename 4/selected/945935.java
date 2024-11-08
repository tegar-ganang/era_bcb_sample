package org.jostraca.test;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.jostraca.Service;
import org.jostraca.Property;
import org.jostraca.BasicTemplatePath;
import org.jostraca.util.FileUtil;
import org.jostraca.util.WayPointRecorder;
import org.jostraca.util.BasicWayPoint;
import org.jostraca.util.ListUtil;
import org.jostraca.util.PropertySet;
import org.jostraca.util.UserMessageHandler;
import org.jostraca.util.CommandLineUserMessageHandler;
import java.io.File;
import java.util.Date;
import java.util.Vector;

/** Test of the dependency tracking logic. Templates should
 *  only be recompiled/generated under certain conditions, that is,
 *  only when necessary.
 */
public class DependsTest extends TestCase {

    public DependsTest(String pName) {
        super(pName);
    }

    public static TestSuite suite() {
        return new TestSuite(DependsTest.class);
    }

    public static void main(String[] pArgs) {
        TestRunner.run(suite());
    }

    private String iDependsFolder;

    private String iConfigFolder;

    public void setUp() throws Exception {
        iDependsFolder = FileUtil.findFile("org/jostraca/test/depends/").getAbsolutePath() + File.separator;
        System.out.println("iDependsFolder:" + iDependsFolder);
        iConfigFolder = new File(FileUtil.findFile("org/jostraca"), "../../../conf").getAbsolutePath() + File.separator;
        System.out.println("iConfigFolder:" + iConfigFolder);
    }

    /** always compile and execute code writer */
    public void testAlways() throws Exception {
        System.out.println();
        String tm = "always.jtm";
        String tmG = "always.txt";
        String tmW = "AlwaysWriter.java";
        String[] before = new String[] { tmG, tm };
        String[] after = new String[] { tm, tmG };
        ensureCodeWriter(tm, tmW, "");
        appendToFile(tm, "a");
        setLastModifiedAscending(before);
        System.out.println();
        WayPointRecorder.clear();
        generate(tm, "");
        Vector wpV = new Vector();
        wpV.addElement(BasicWayPoint.SavingCodeWriter.make(iDependsFolder + "AlwaysWriter.java"));
        wpV.addElement(BasicWayPoint.CompilingCodeWriter.make(iDependsFolder + "AlwaysWriter.java"));
        wpV.addElement(BasicWayPoint.ExecutingCodeWriter.make(iDependsFolder + "AlwaysWriter.java"));
        assertTrue(WayPointRecorder.matches(wpV));
        checkLastModifiedAscending(after);
    }

    /** Code writer executed despite generated being uptodate a no meta data available. 
   *  File states: generated older than template
   *  Template:    executed, not compiled
   */
    public void testNoMeta() throws Exception {
        System.out.println();
        String tm = "nometa.jtm";
        String tmG = "nometa.txt";
        String tmW = "NometaWriter.java";
        String[] before = new String[] { tm, tmW, tmG };
        String[] after = new String[] { tm, tmW, tmG };
        ensureCodeWriter(tm, tmW, "");
        setLastModifiedAscending(before);
        System.out.println();
        WayPointRecorder.clear();
        generate(tm, "");
        Vector wpV = new Vector();
        wpV.addElement(BasicWayPoint.SavingCodeWriter.make(iDependsFolder + tmW));
        wpV.addElement(BasicWayPoint.ExecutingCodeWriter.make(iDependsFolder + tmW));
        System.out.println(wpV);
        System.out.println(WayPointRecorder.makeString());
        assertTrue(WayPointRecorder.matches(wpV));
        setLastModifiedAscending(after);
    }

    /** A compile should not be necessary if code writer has not changed.
   *  File states: generated older than template
   *  Template:    executed, not compiled
   */
    public void testNoCompile() throws Exception {
        System.out.println();
        String tm = "nocompile.jtm";
        String tmG = "nocompile.txt";
        String tmO = String.valueOf((new Date()).getTime());
        String tmW = "NocompileWriter.java";
        String[] before = new String[] { tmG, tm, tmW };
        String[] after = new String[] { tm, tmW, tmG };
        ensureCodeWriter(tm, tmW, tmO);
        setLastModifiedAscending(before);
        System.out.println();
        WayPointRecorder.clear();
        generate(tm, tmO);
        System.out.println(WayPointRecorder.makeString());
        Vector wpV = new Vector();
        wpV.addElement(BasicWayPoint.SavingCodeWriter.make(iDependsFolder + tmW));
        wpV.addElement(BasicWayPoint.ExecutingCodeWriter.make(iDependsFolder + tmW));
        assertTrue(WayPointRecorder.matches(wpV));
        checkLastModifiedAscending(after);
    }

    public void testNoGenerate() throws Exception {
        Service.activateTracking(new File("/tmp/jostraca/track"));
        System.out.println();
        String tm = "nogen.jtm";
        String tmG = "nogen.txt";
        String tmR = "nogen-input.txt";
        String tmW = "NogenWriter.java";
        String[] before = new String[] { tmR, tm, tmG };
        String[] after = new String[] { tmR, tm, tmG };
        ensureCodeWriter(tm, tmW, tmR);
        setLastModifiedAscending(before);
        System.out.println();
        WayPointRecorder.clear();
        generate(tm, tmR);
        Vector wpV = new Vector();
        wpV.addElement(BasicWayPoint.GenerationUptodate.make(iDependsFolder + tm));
        System.out.println(wpV);
        System.out.println(WayPointRecorder.makeString());
        assertTrue(WayPointRecorder.matches(wpV));
        checkLastModifiedAscending(after);
    }

    public void testResourceModified() throws Exception {
        System.out.println();
        String tm = "resmod.jtm";
        String tmG = "resmod.txt";
        String tmR = "resmod-input.txt";
        String tmW = "ResmodWriter.java";
        String[] before = new String[] { tm, tmG, tmR };
        String[] after = new String[] { tm, tmR, tmG };
        ensureCodeWriter(tm, tmW, tmR);
        setLastModifiedAscending(before);
        System.out.println();
        WayPointRecorder.clear();
        generate(tm, tmR);
        System.out.println(WayPointRecorder.makeString());
        Vector wpV = new Vector();
        wpV.addElement(BasicWayPoint.SavingCodeWriter.make(iDependsFolder + tmW));
        wpV.addElement(BasicWayPoint.ExecutingCodeWriter.make(iDependsFolder + tmW));
        assertTrue(WayPointRecorder.matches(wpV));
        checkLastModifiedAscending(after);
    }

    public void appendToFile(String pFile, String pAppend) throws Exception {
        FileUtil.writeFile(iDependsFolder + pFile, FileUtil.readFile(iDependsFolder + pFile) + pAppend);
    }

    public void ensureCodeWriter(String pTemplate, String pCodeWriter, String pOptions) throws Exception {
        File cwF = new File(iDependsFolder, pCodeWriter);
        if (!cwF.exists()) {
            generate(pTemplate, pOptions);
        }
    }

    public void setLastModifiedAscending(String[] pFiles) throws Exception {
        long interval = 2000;
        long t = (new Date()).getTime();
        int numFiles = pFiles.length;
        t -= interval;
        for (int fI = numFiles - 1; fI > -1; fI--) {
            File f = new File(iDependsFolder + "/" + pFiles[fI]);
            f.setLastModified(t);
            System.out.println("[" + f.lastModified() + "]" + f);
            t -= interval;
        }
    }

    public void checkLastModifiedAscending(String[] pFiles) throws Exception {
        long last = 0;
        File lastf = null;
        int numFiles = pFiles.length;
        for (int fI = 0; fI < numFiles; fI++) {
            File f = new File(iDependsFolder + "/" + pFiles[fI]);
            System.out.println("[" + f.lastModified() + "]" + f);
            long fl = f.lastModified();
            if (fl < last) {
                throw new Exception(f + "[" + fl + "] < " + lastf + "[" + last + "]");
            }
            last = fl;
            lastf = f;
        }
    }

    public void generate(String pTemplate, String pOptions) throws Exception {
        Service s = new Service();
        s.setTemplatePaths(ListUtil.make(new BasicTemplatePath(iDependsFolder + pTemplate)));
        s.setConfigFolder(iConfigFolder);
        s.addPropertySet(Service.CONF_system, s.loadBaseConfigFiles(new File(iConfigFolder, "system.conf")));
        PropertySet props = new PropertySet();
        props.set(Property.main_OutputFolder, iDependsFolder);
        props.set(Property.main_WorkFolder, iDependsFolder);
        props.set(Property.main_CodeWriterOptions, pOptions);
        s.addPropertySet(Service.CONF_cmdline, props);
        UserMessageHandler umh = new CommandLineUserMessageHandler();
        umh.setThreshold(umh.DEBUG);
        s.setUserMessageHandler(umh);
        s.build();
    }
}
