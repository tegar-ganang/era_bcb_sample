package net.sourceforge.antme.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileLock;
import java.util.Properties;
import net.sourceforge.antme.Constants;
import net.sourceforge.antme.Language;
import net.sourceforge.antme.jad.JadFile;
import net.sourceforge.antme.jad.JadFormatException;
import net.sourceforge.antme.tasks.jad.IncrementVersion;
import net.sourceforge.antme.tasks.jad.JarSize;
import net.sourceforge.antme.tasks.jad.RemoveMidlet;
import net.sourceforge.antme.tasks.jad.SetMidlet;
import org.apache.tools.ant.Project;

public class JadEditTaskTest extends BaseTestTask {

    private static final String OUTPUT_JAD_FILE = "output.jad";

    private static final String STANDARD_JAD_PATH = "test/jad/standard.jad";

    public JadEditTaskTest(String s) {
        super(s);
    }

    public void setUp() {
        configureProject("testCases.xml", "dir.test.temp");
        Language.setTestMode(true);
    }

    public void tearDown() {
        cleanTestDirectory();
        Language.setTestMode(false);
    }

    public void testNoFiles() {
        executeBuildExceptionTarget("JadEditTaskTest.testNoFiles");
        assertExceptionMessageIs(JadEditTask.LANG_NEED_IN_OR_OUT);
    }

    public void testNoSuchFile() {
        executeBuildExceptionTarget("JadEditTaskTest.testNoSuchFile");
        assertTrue(getBuildException().getCause() instanceof FileNotFoundException);
    }

    public void testBadLoadNoColon() {
        executeBuildExceptionTarget("JadEditTaskTest.testBadLoadNoColon");
        assertExceptionMessageIs(JadFormatException.LANG_NO_COLON);
    }

    public void testBadEncoding() {
        executeBuildExceptionTarget("JadEditTaskTest.testBadEncoding");
        assertTrue(getBuildException().getCause() instanceof UnsupportedEncodingException);
    }

    public void testGoodLoad() {
        executeTarget("JadEditTaskTest.testGoodLoad");
    }

    private static final String[] GOOD_SETS_VALUES = { JadFile.ATTR_MIDLET_NAME, "test-name", JadFile.ATTR_MIDLET_VERSION, "1.2.3", JadFile.ATTR_MIDLET_VENDOR, "test-vendor", JadFile.ATTR_MIDLET_ICON, "test-icon", JadFile.ATTR_MIDLET_DESCRIPTION, "test-description", JadFile.ATTR_MIDLET_JAR_URL, "http://sourceforge.net/output.jar", JadFile.ATTR_MIDLET_INFO_URL, "http://sourceforge.net/info.txt", JadFile.ATTR_MICROEDITION_CONFIGURATION, "1.1", JadFile.ATTR_MICROEDITION_PROFILE, "2.0", JadFile.ATTR_MIDLET_DATA_SIZE, "656" };

    public void testGoodSets() throws Exception {
        executeTarget("JadEditTaskTest.testGoodSets");
        verifyJadAttributes(OUTPUT_JAD_FILE, GOOD_SETS_VALUES, true);
    }

    public void testMissingValues() {
        for (int i = 0; i < 14; i++) {
            setUp();
            executeBuildExceptionTarget("JadEditTaskTest.testMissingValues" + i);
            assertExceptionMessageContains("testMissingValues[" + i + "]", Constants.LANG_ATTRIBUTE_REQUIRED);
            tearDown();
        }
    }

    public void testMissingName() {
        executeBuildExceptionTarget("JadEditTaskTest.testMissingName");
        assertExceptionMessageIs(Constants.LANG_ATTRIBUTE_REQUIRED + ":name:attribute");
    }

    public void testBadVersion() {
        executeBuildExceptionTarget("JadEditTaskTest.testBadVersion");
        assertExceptionMessageIs(JadFormatException.LANG_INVALID_MIDLET_VERSION);
    }

    public void testBadCldc() {
        executeBuildExceptionTarget("JadEditTaskTest.testBadCldc");
        assertExceptionMessageContains("not a legal value");
    }

    public void testBadMidp() {
        executeBuildExceptionTarget("JadEditTaskTest.testBadMidp");
        assertExceptionMessageContains("not a legal value");
    }

    public void testBadDataSize() {
        executeBuildExceptionTarget("JadEditTaskTest.testBadDataSize");
        assertExceptionMessageContains("NumberFormatException");
    }

    public void testJarSizeExplicit() throws Exception {
        String[] correct = { JadFile.ATTR_MIDLET_JAR_SIZE, "123" };
        executeTarget("JadEditTaskTest.testJarSizeExplicit");
        verifyJadAttributes(OUTPUT_JAD_FILE, correct, true);
    }

    public void testJarSizeFile() throws Exception {
        File theFile = new File(STANDARD_JAD_PATH);
        assertTrue(theFile.exists() && theFile.isFile());
        String[] correct = { JadFile.ATTR_MIDLET_JAR_SIZE, Long.toString(theFile.length()) };
        executeTarget("JadEditTaskTest.testJarSizeFile");
        verifyJadAttributes(OUTPUT_JAD_FILE, correct, true);
    }

    public void testJarSizeNeither() {
        executeBuildExceptionTarget("JadEditTaskTest.testJarSizeNeither");
        assertExceptionMessageIs(JarSize.LANG_NEED_VALUE_OR_FILE);
    }

    public void testJarSizeDouble() {
        executeBuildExceptionTarget("JadEditTaskTest.testJarSizeDouble");
        assertExceptionMessageIs(JarSize.LANG_NOT_BOTH_VALUE_AND_FILE);
    }

    public void testJarSizeBadFile() {
        executeBuildExceptionTarget("JadEditTaskTest.testJarSizeBadFile");
        assertExceptionMessageIs(Constants.LANG_NO_SUCH_FILE + ":doesntExist");
    }

    public void testSaveError() throws Exception {
        FileOutputStream stream = null;
        FileLock lock = null;
        try {
            File theFile = new File(getTestDirectory(), OUTPUT_JAD_FILE);
            stream = new FileOutputStream(theFile);
            lock = stream.getChannel().lock();
            executeBuildExceptionTarget("JadEditTaskTest.testSaveError");
            assertExceptionMessageContains("IOException");
        } finally {
            lock.release();
            stream.close();
        }
    }

    public void testAddMidlet() throws Exception {
        executeTarget("JadEditTaskTest.testAddMidlet");
        verifyMidlets(OUTPUT_JAD_FILE, "name1,icon1,className1", "name2,,className2", "name3,icon3,className3");
    }

    public void testClearAddMidlet() throws Exception {
        executeTarget("JadEditTaskTest.testClearAddMidlet");
        verifyMidlets(OUTPUT_JAD_FILE, "name3,icon3,className3", null, null);
    }

    public void testAddMidletMissingName() throws Exception {
        executeBuildExceptionTarget("JadEditTaskTest.testAddMidletMissingName");
        assertExceptionMessageIs(Constants.LANG_ATTRIBUTE_REQUIRED + ":name:addMidlet");
    }

    public void testAddMidletMissingClassName() throws Exception {
        executeBuildExceptionTarget("JadEditTaskTest.testAddMidletMissingClassName");
        assertExceptionMessageIs(Constants.LANG_ATTRIBUTE_REQUIRED + ":className:addMidlet");
    }

    public void testSetMidlet() throws Exception {
        executeTarget("JadEditTaskTest.testSetMidlet");
        verifyMidlets(OUTPUT_JAD_FILE, "name1a,icon1a,className1a", "name2a,,className2a", "name3a,icon3a,className3a");
    }

    public void testSetMidletInvalidNumber() throws Exception {
        executeBuildExceptionTarget("JadEditTaskTest.testSetMidletInvalidNumber");
        assertExceptionMessageIs(SetMidlet.LANG_INVALID_MIDLET_NUMBER);
    }

    public void testRemoveMidletNumber() throws Exception {
        executeTarget("JadEditTaskTest.testRemoveMidletNumber");
        verifyMidlets(OUTPUT_JAD_FILE, "name1,icon1,className1", null, null);
    }

    public void testRemoveMidletName() throws Exception {
        executeTarget("JadEditTaskTest.testRemoveMidletName");
        verifyMidlets(OUTPUT_JAD_FILE, "name2,,className2", null, null);
    }

    public void testRemoveMidletClass() throws Exception {
        executeTarget("JadEditTaskTest.testRemoveMidletClass");
        verifyMidlets(OUTPUT_JAD_FILE, "name2,,className2", null, null);
    }

    public void testRemoveMidletNoAttributes() throws Exception {
        executeBuildExceptionTarget("JadEditTaskTest.testRemoveMidletNoAttributes");
        assertExceptionMessageIs(RemoveMidlet.LANG_NO_ATTRIBUTES);
    }

    public void testRemoveMidletTooManyAttributes() throws Exception {
        executeBuildExceptionTarget("JadEditTaskTest.testRemoveMidletTooManyAttributes");
        assertExceptionMessageIs(RemoveMidlet.LANG_TOO_MANY_ATTRIBUTES);
    }

    public void testRemoveMidletNoSuchName() throws Exception {
        executeTarget("JadEditTaskTest.testRemoveMidletNoSuchName");
        assertLogContainsSubstring(Project.MSG_WARN, RemoveMidlet.LANG_NO_SUCH_NAME + ":nonExistentName");
    }

    public void testRemoveMidletNoSuchNumber() throws Exception {
        executeTarget("JadEditTaskTest.testRemoveMidletNoSuchNumber");
        assertLogContainsSubstring(Project.MSG_WARN, RemoveMidlet.LANG_NO_SUCH_NUMBER + ":99");
    }

    public void testRemoveMidletNoSuchClass() throws Exception {
        executeTarget("JadEditTaskTest.testRemoveMidletNoSuchClass");
        assertLogContainsSubstring(Project.MSG_WARN, RemoveMidlet.LANG_NO_SUCH_CLASS + ":nonExistentName");
    }

    public void testConditionIfRun() {
        executeTarget("JadEditTaskTest.testConditionIfRun");
        File theFile = new File(getTestDirectory(), OUTPUT_JAD_FILE);
        assertTrue(theFile.exists());
    }

    public void testConditionIfDontRun() {
        executeTarget("JadEditTaskTest.testConditionIfDontRun");
        File theFile = new File(getTestDirectory(), OUTPUT_JAD_FILE);
        assertFalse(theFile.exists());
    }

    public void testConditionUnlessRun() {
        executeTarget("JadEditTaskTest.testConditionUnlessRun");
        File theFile = new File(getTestDirectory(), OUTPUT_JAD_FILE);
        assertTrue(theFile.exists());
    }

    public void testConditionUnlessDontRun() {
        executeTarget("JadEditTaskTest.testConditionUnlessDontRun");
        File theFile = new File(getTestDirectory(), OUTPUT_JAD_FILE);
        assertFalse(theFile.exists());
    }

    public void testConditionIfUnlessConflict() {
        executeTarget("JadEditTaskTest.testConditionIfUnlessConflict");
        File theFile = new File(getTestDirectory(), OUTPUT_JAD_FILE);
        assertFalse(theFile.exists());
    }

    private void verifyJadAttributes(String filename, String[] contents, boolean only) throws Exception {
        Properties file = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(getTestDirectory(), filename));
            file.load(fis);
        } finally {
            if (fis != null) fis.close();
        }
        for (int i = 0; i < contents.length; i += 2) {
            String value = file.getProperty(contents[i]);
            assertEquals("JadFile (" + filename + ") item '" + contents[i] + "'", contents[i + 1], value);
        }
        if (only) {
            if (file.size() != contents.length / 2) {
                fail("JadFile (" + filename + ") contained unexpected attributes");
            }
        }
    }

    private void verifyMidlets(String path, String one, String two, String three) throws Exception {
        Properties file = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(getTestDirectory(), path));
            file.load(fis);
        } finally {
            if (fis != null) fis.close();
        }
        assertEquals(one, file.getProperty("MIDlet-1"));
        assertEquals(two, file.getProperty("MIDlet-2"));
        assertEquals(three, file.getProperty("MIDlet-3"));
    }

    private static String[] incrementValues = { "JadEditTaskTest.testVersionIncrement0", "0.0.1", "JadEditTaskTest.testVersionIncrement1", "1.0.1", "JadEditTaskTest.testVersionIncrement2", "1.2.1", "JadEditTaskTest.testVersionIncrement3", "1.2.4", "JadEditTaskTest.testVersionIncrement99a", "1.2.0", "JadEditTaskTest.testVersionIncrement99b", "2.0.0" };

    public void testIncrements() throws Exception {
        for (int i = 0; i < incrementValues.length; i += 2) {
            setUp();
            executeTarget(incrementValues[i]);
            JadFile file = JadFile.loadJadFile(new File(getTestDirectory(), OUTPUT_JAD_FILE), "UTF-8");
            assertEquals(file.getAttribute(JadFile.ATTR_MIDLET_VERSION), incrementValues[i + 1]);
            tearDown();
        }
    }

    public void testVersionIncrementOverflow() {
        executeBuildExceptionTarget("JadEditTaskTest.testVersionIncrementOverflow");
        assertExceptionMessageIs(IncrementVersion.LANG_VERSION_TOO_LARGE);
    }

    public void testVersionIncrementBad() {
        executeBuildExceptionTarget("JadEditTaskTest.testVersionIncrementBad");
        assertExceptionMessageIs(JadFormatException.LANG_INVALID_MIDLET_VERSION);
    }
}
