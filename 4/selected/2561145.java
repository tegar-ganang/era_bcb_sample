package org.xaware.functoids;

import java.io.File;
import java.io.IOException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.xaware.shared.util.FileUtils;
import org.xaware.shared.util.FunctoidConfigUtil;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.testing.util.BaseBdpTestCase;
import junit.framework.Assert;

/**
 * Test class to test the FunctoidConfigUtil functionality.
 * 
 * @author satish
 *
 */
public class TestFunctoidConfig extends BaseBdpTestCase {

    /**Holds the XAware home directory.*/
    public static final String xaHomeDir = System.getProperty("xaware.home");

    /**Holds the xaware namespace.*/
    public static final Namespace xaNamespace = XAwareConstants.xaNamespace;

    /**Holds the conf directory path to be appended to xaware.home*/
    public static final String configPath = "/conf/";

    /**Holds the 'XAFunctoids.xml' file name*/
    public static final String functoidConfigFile = "XAFunctoids.xml";

    /**Holds "emptyFunctoids.xml" file name*/
    public static final String emptyFuntoidsFile = "emptyFunctoids.xml";

    /**Holds "emptyFunctoids.xml" file name*/
    public static final String xaFunctoidsFile = "testXAFunctoids.xml";

    /**Holds "missingXAFunctoids.xml" file name*/
    public static final String missingXAFunctoidsElementFile = "missingXAFunctoids.xml";

    /**Holds "singleFunctoidElement.xml" file name*/
    public static final String singleFunctoidElementFile = "singleFunctoidElement.xml";

    /**Holds the String constant for 'Append strings' string.*/
    private static final String APPEND_STRINGS = "Append strings";

    /**Holds the String constant for 'functoids' string.*/
    private static final String FUNCTOIDS = "functoids";

    /**Holds the String constant for 'functoid' string.*/
    private static final String FUNCTOID = "functoid";

    /**Holds the file created from the XAFunctoids.xml file path.*/
    public static File configFile = new File(xaHomeDir + configPath + functoidConfigFile);

    /**
	 * Constructor that takes a name parameter.
	 */
    public TestFunctoidConfig(String p_name) {
        super(p_name);
    }

    @Override
    public void setUp() throws Exception {
        confirmFileExists();
    }

    @Override
    public void tearDown() throws Exception {
        replaceOriginalFile();
        FunctoidConfigUtil.getInstance().reloadConfig();
    }

    /**
	 * replaces the original file back in the conf directory.
	 */
    private static void replaceOriginalFile() {
        changeTestFile(getOriginalFilePath());
    }

    /**
	 * Tests getInstance() method returning fucntoid config object. 
	 * 
	 */
    public void testGetInstance() {
        assertNotNull(FunctoidConfigUtil.getInstance());
    }

    /**
	 * Check singleton object.
	 * Two concurrent calls should give the same instance of the 
	 * functoidConfig object.
	 */
    public void testSingletonConfig() {
        assertEquals(FunctoidConfigUtil.getInstance(), FunctoidConfigUtil.getInstance());
    }

    /**
	 * Tests if the class reloads the configuration properly.
	 */
    public void testReloadConfig() {
        assertTrue(FunctoidConfigUtil.getInstance().reloadConfig());
    }

    /**
	 * Checks the getXAFunctoidsElement using a file in which 
	 * the xa:functoids element is missing.
	 */
    public void testMissingXAFunctoids() {
        changeTestFile(getDataFolder() + missingXAFunctoidsElementFile);
        FunctoidConfigUtil.getInstance().reloadConfig();
        assertNull(FunctoidConfigUtil.getInstance().getXAFunctoidsElement());
    }

    /**
	 * Checks the getXAFunctoidsElement using a file with 
	 * the xa:functoids element now present.
	 */
    public void testXAFunctoidsElement() {
        changeTestFile(getDataFolder() + xaFunctoidsFile);
        FunctoidConfigUtil.getInstance().reloadConfig();
        assertNotNull(FunctoidConfigUtil.getInstance().getXAFunctoidsElement());
    }

    /**
	 * Tests the getFuncotidElement method.
	 * with null parameter. 
	 */
    public void testGetFunctoidElementWithNullParam() {
        changeTestFile(getDataFolder() + xaFunctoidsFile);
        FunctoidConfigUtil.getInstance().reloadConfig();
        assertNull(FunctoidConfigUtil.getInstance().getFunctoidElement(null));
    }

    /**
	 * Tests the getFuncotidElement method.
	 * with an empty string parameter. 
	 */
    public void testGetFunctoidElementWithNoName() {
        changeTestFile(getDataFolder() + xaFunctoidsFile);
        FunctoidConfigUtil.getInstance().reloadConfig();
        assertNull(FunctoidConfigUtil.getInstance().getFunctoidElement(""));
    }

    /**
	 * Tests the getFuncotidElement method.
	 * with a valid parameter.
	 * The parameter name reflects the functoid element name in
	 * the singleFuntoidElement.xml file. 
	 */
    public void testGetFuntoidElementWithValidName() {
        changeTestFile(getDataFolder() + xaFunctoidsFile);
        FunctoidConfigUtil.getInstance().reloadConfig();
        Element expected = getTestFunctoidElement();
        if (expected == null) return;
        Element actual = FunctoidConfigUtil.getInstance().getFunctoidElement(APPEND_STRINGS);
        assertEquals(expected.getName(), actual.getName());
    }

    /**
	 * Tests the getFuncotidElement method.
	 * with a valid parameter.
	 * The parameter name reflects the functoid element name in
	 * the singleFuntoidElement.xml file. 
	 */
    private Element getTestFunctoidElement() {
        Document doc = null;
        try {
            doc = FileUtils.getDocumentFromFile(getDataFolder() + singleFunctoidElementFile);
        } catch (JDOMException e) {
            Assert.fail("Failed to load document from test file." + e.getMessage());
            return null;
        } catch (IOException e) {
            Assert.fail("Failed to load document from test file." + e.getMessage());
            return null;
        }
        return doc.getRootElement().getChild(FUNCTOIDS, xaNamespace).getChild(FUNCTOID, xaNamespace);
    }

    /**
	 * Returns the data folder path containing functoid test files.
     * 
     * @return the data folder containing the functoid test files
     */
    protected String getDataFolder() {
        return "data/org/xaware/functoids/";
    }

    private static String getOriginalFilePath() {
        return xaHomeDir + "/../../src/org/xaware/shared/conf/" + functoidConfigFile;
    }

    /**
     * Confirms that the config file exists in the conf directory.
     * If not present, places a test file in the location.
     */
    private static void confirmFileExists() {
        if (!configFile.exists()) {
            try {
                FileUtils.copyFile(new File(getOriginalFilePath()), configFile);
            } catch (IOException e) {
                Assert.fail("Failed to copy file from original path to " + configFile + " " + e.getMessage());
            }
        }
    }

    /**
     * Replaces the test file in the conf directory with 
     * the given new test file.
     * @param filePath file path.
     */
    private static void changeTestFile(String filePath) {
        try {
            FileUtils.copyFile(new File(filePath), configFile);
        } catch (IOException e) {
            Assert.fail("Failed to copy the test file to " + configFile + " " + e.getMessage());
        }
    }
}
