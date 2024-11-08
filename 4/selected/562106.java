package org.xaware.server.engine.integration;

import java.io.File;
import junit.framework.Assert;
import org.xaware.shared.util.FileUtils;
import org.xaware.testing.util.BaseBdpTestCase;

/**
 * This class performs integration tests which exercise the xa:response instruction.
 * 
 * @author Tim Uttormark
 */
public class ResponseInstructionTestCase extends BaseBdpTestCase {

    private static final String dataFolder = "data/org/xaware/server/engine/integration/response/";

    private static final String TEST_NO_CHILDREN = "TestXaResponseNoChildren.xbd";

    private static final String TEST_ONE_CHILD = "TestXaResponseOneChild.xbd";

    private static final String TEST_MULTI_CHILDREN = "TestXaResponseMultiChildren.xbd";

    private static final String TEST_NESTED = "TestXaResponseNested.xbd";

    private static final String TEST_DATA_PATH_LEGACY_1 = "TestXaResponseDataPath1.xbd";

    private static final String TEST_DATA_PATH_LEGACY_2 = "TestXaResponseDataPath2.xbd";

    private static final String TEST_DATA_PATH_LEGACY_3 = "TestXaResponseDataPath3.xbd";

    private static final String TEST_DATA_PATH_LEGACY_4 = "TestXaResponseDataPath4.xbd";

    private static final String TEST_DATA_PATH_LEGACY_5 = "TestXaResponseDataPath5.xbd";

    private static final String TEST_DATA_PATH_LEGACY_6 = "TestXaResponseDataPath6.xbd";

    private static final String TEST_DATA_PATH_LEGACY_7 = "TestXaResponseDataPath7.xbd";

    private static final String TEST_DATA_PATH_LEGACY_8 = "TestXaResponseDataPath8.xbd";

    private static final String TEST_DATA_PATH_XPATH_1 = "TestXaResponseDataPathXPath1.xbd";

    private static final String TEST_DATA_PATH_XPATH_2 = "TestXaResponseDataPathXPath2.xbd";

    private static final String TEST_DATA_PATH_XPATH_3 = "TestXaResponseDataPathXPath3.xbd";

    private static final String TEST_DATA_PATH_XPATH_4 = "TestXaResponseDataPathXPath4.xbd";

    private static final String TEST_DATA_PATH_XPATH_5 = "TestXaResponseDataPathXPath5.xbd";

    private static final String TEST_DATA_PATH_XPATH_6 = "TestXaResponseDataPathXPath6.xbd";

    private static final String NO_CHILDREN_EXPECTED = "XaResponseNoChildrenExpected.xml";

    private static final String ONE_CHILD_EXPECTED = "XaResponseOneChildExpected.xml";

    private static final String LAST_CHILD_EXPECTED = "XaResponseLastChildExpected.xml";

    private static final String NESTED_EXPECTED = "XaResponseNestedExpected.xml";

    private static final String DATA_PATH_EXPECTED = "XaResponseDataPathExpected.xml";

    private static final String DATA_PATH_INVALID = "XaResponseDataPathInvalid.xml";

    private static final String DATA_PATH_INVALID_2 = "XaResponseDataPathInvalid2.xml";

    private static final String DATA_PATH_MULTIPLE = "XaResponseDataPathMultiple.xml";

    private static final String DATA_PATH_FILE_ENCODED = "XaResponseDataPathFileEncoded.xml";

    private static final String INPUT_XML_FILE_NAME = "../testXPathInputData.xml";

    /**
     * Creates a new test case providing a standard name.
     */
    public ResponseInstructionTestCase() {
        super("ResponseInstructionTestCase");
    }

    /**
     * Returns the relative path to the folder where test artifacts are found.
     */
    @Override
    protected String getDataFolder() {
        return dataFolder;
    }

    /**
     * Test the xa:response instruction when it has no child Elements.
     */
    public void testXaResponseNoChildren() {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName(TEST_NO_CHILDREN);
        setExpectedOutputFileName(NO_CHILDREN_EXPECTED);
        getTestHelper().setTestMethodName("testXaResponseNoChildren");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction when it has one child Element.
     */
    public void testXaResponseOneChild() {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName(TEST_ONE_CHILD);
        setExpectedOutputFileName(ONE_CHILD_EXPECTED);
        getTestHelper().setTestMethodName("testXaResponseOneChild");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction when it has multiple children Elements.
     */
    public void testXaResponseMultiChildren() {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName(TEST_MULTI_CHILDREN);
        setExpectedOutputFileName(LAST_CHILD_EXPECTED);
        getTestHelper().setTestMethodName("testXaResponseMultiChildren");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction when contained in a nested BizDoc.
     */
    public void testXaResponseNestedBizDoc() {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName(TEST_NESTED);
        setExpectedOutputFileName(NESTED_EXPECTED);
        getTestHelper().setTestMethodName("testXaResponseNestedBizDoc");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute where the data path uses the legacy syntax pointing
     * to the current element
     */
    public void testXaResponseDataPathLegacyCurrentElem() {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName(TEST_DATA_PATH_LEGACY_1);
        setExpectedOutputFileName(DATA_PATH_EXPECTED);
        getTestHelper().setTestMethodName("testXaResponseDataPathLegacyCurrentElem");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute where the data path uses the legacy syntax pointing
     * to the root element
     */
    public void testXaResponseDataPathLegacyRootElem() {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName(TEST_DATA_PATH_LEGACY_2);
        setExpectedOutputFileName(DATA_PATH_EXPECTED);
        getTestHelper().setTestMethodName("testXaResponseDataPathLegacyRootElem");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute where the data path uses the legacy syntax pointing
     * to the input XML
     */
    public void testXaResponseDataPathLegacyInputXML() {
        clearInputParams();
        setInputXmlFileName("foo.xml");
        setBizDocFileName(TEST_DATA_PATH_LEGACY_3);
        setExpectedOutputFileName(DATA_PATH_EXPECTED);
        getTestHelper().setTestMethodName("testXaResponseDataPathLegacyInputXML");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute where the data path uses the legacy syntax pointing
     * to the parent element
     */
    public void testXaResponseDataPathLegacyParentElem() {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName(TEST_DATA_PATH_LEGACY_4);
        setExpectedOutputFileName(DATA_PATH_EXPECTED);
        getTestHelper().setTestMethodName("testXaResponseDataPathLegacyParentElem");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute where the data path uses the legacy syntax pointing
     * to an invalid path
     */
    public void testXaResponseDataPathLegacyInvalid() {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName(TEST_DATA_PATH_LEGACY_5);
        setExpectedOutputFileName(DATA_PATH_INVALID_2);
        getTestHelper().setTestMethodName("testXaResponseDataPathLegacyInvalid");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute where the data path uses legacy syntax pointing to
     * multiple elements. Expect the last element match in the result.
     */
    public void testXaResponseDataPathLegacyMultipleMatches() {
        clearInputParams();
        setInputXmlFileName(INPUT_XML_FILE_NAME);
        setBizDocFileName(TEST_DATA_PATH_LEGACY_6);
        setExpectedOutputFileName(DATA_PATH_MULTIPLE);
        getTestHelper().setTestMethodName("testXaResponseDataPathLegacyMultipleMatches");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute where the data path contains the name of an XML
     * file.
     */
    public void testXaResponseDataPathFile() {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName(TEST_DATA_PATH_LEGACY_7);
        setExpectedOutputFileName(DATA_PATH_EXPECTED);
        getTestHelper().setTestMethodName("testXaResponseDataPathFile");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute where the data path contains the name of an XML file
     * and xa:encode="yes" indicates that the file contents should be encoded. This is illegal.
     */
    public void testXaResponseDataPathFileEncoded() {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName(TEST_DATA_PATH_LEGACY_8);
        setExpectedOutputFileName(DATA_PATH_FILE_ENCODED);
        getTestHelper().setTestMethodName("testXaResponseDataPathFileEncoded");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute where the data path uses XPath syntax pointing to
     * the current element
     */
    public void testXaResponseDataPathXPathCurrentElem() {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName(TEST_DATA_PATH_XPATH_1);
        setExpectedOutputFileName(DATA_PATH_EXPECTED);
        getTestHelper().setTestMethodName("testXaResponseDataPathXPathCurrentElem");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute where the data path uses XPath syntax pointing to
     * the input XML
     */
    public void testXaResponseDataPathXPathInputXML() {
        clearInputParams();
        setInputXmlFileName("foo.xml");
        setBizDocFileName(TEST_DATA_PATH_XPATH_2);
        setExpectedOutputFileName(DATA_PATH_EXPECTED);
        getTestHelper().setTestMethodName("testXaResponseDataPathXPathInputXML");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute where the data path uses XPath syntax pointing to
     * the root element
     */
    public void testXaResponseDataPathXPathRootElem() {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName(TEST_DATA_PATH_XPATH_3);
        setExpectedOutputFileName(DATA_PATH_EXPECTED);
        getTestHelper().setTestMethodName("testXaResponseDataPathXPathRootElem");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute where the data path uses XPath syntax pointing to an
     * invalid element
     */
    public void testXaResponseDataPathXPathInvalidPath() {
        clearInputParams();
        setInputXmlFileName("foo.xml");
        setBizDocFileName(TEST_DATA_PATH_XPATH_4);
        setExpectedOutputFileName(DATA_PATH_INVALID);
        getTestHelper().setTestMethodName("testXaResponseDataPathXPathInvalidPath");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute where the data path uses XPath syntax pointing to
     * multiple elements. Expect the last element match in the result.
     */
    public void testXaResponseDataPathXPathMultipleMatches() {
        clearInputParams();
        setInputXmlFileName(INPUT_XML_FILE_NAME);
        setBizDocFileName(TEST_DATA_PATH_XPATH_5);
        setExpectedOutputFileName(DATA_PATH_MULTIPLE);
        getTestHelper().setTestMethodName("testXaResponseDataPathXPathMultipleMatches");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute where the data path uses XPath syntax pointing to
     * multiple elements
     */
    public void testXaResponseDataPathXPathSubstitution() {
        clearInputParams();
        setInputXmlFileName(INPUT_XML_FILE_NAME);
        setBizDocFileName(TEST_DATA_PATH_XPATH_6);
        setExpectedOutputFileName(DATA_PATH_EXPECTED);
        getTestHelper().setTestMethodName("testXaResponseDataPathXPathSubstitution");
        evaluateBizDoc();
    }

    /**
     * Demonstrates substitution within a child element of xa:response.
     */
    public void testXAResponseSubstitution() {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName("RespSubstFailure.xbd");
        setExpectedOutputFileName("RespSubstFailure_exp.xml");
        testHelper.setTestMethodName("testXAResponseSubstitution");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute
     * where the data path contains an absolute path to an XML file.
     * 
     * @throws Exception
     */
    public void testXaDataPath_a21_010() throws Exception {
        final File testDataDir = new File(".", getDataFolder());
        Assert.assertTrue("data_path target file not found.", testDataDir.isDirectory());
        clearInputParams();
        addInputParam("absolutePath", testDataDir.getCanonicalPath());
        setInputXmlFileName(null);
        setBizDocFileName("a21_010_dp_pos.xbd");
        setExpectedOutputFileName("a21_010_dp_pos.xml");
        getTestHelper().setTestMethodName("testXaDataPath_a21_010");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute
     * where the data path contains a relative path to an XML file.
     */
    public void testXaDataPath_a21_011() {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName("a21_011_dp_pos.xbd");
        setExpectedOutputFileName("a21_011_dp_pos.xml");
        getTestHelper().setTestMethodName("testXaDataPath_a21_011");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute
     * where the data path contains a URL pointing to an XML file.
     * 
     * Note: this test accesses an external website which can cause
     *       intermittent failures.  This test is disabled to prevent
     *       spurious build failures.
     */
    public void DISABLEDtestXaDataPath_a21_012() {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName("a21_012_dp_pos.xbd");
        setExpectedOutputFileName("a21_012_dp_pos.xml");
        getTestHelper().setTestMethodName("testXaDataPath_a21_012");
        evaluateBizDoc();
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute
     * where the data path contains a relative reference to an XML file
     * in the current directory.
     * 
     * @throws Exception
     */
    public void testXaDataPath_a21_013() throws Exception {
        final File testFile = new File(getDataFolder(), "a21_013_dp_pos_ix.xml");
        Assert.assertTrue("data_path target file not found.", testFile.canRead());
        final File fileInCurrentDir = new File(".", "a21_013_dp_pos_ix.xml");
        Assert.assertTrue("Cannot write test file to current directory", testFile.canWrite());
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName("a21_013_dp_pos.xbd");
        setExpectedOutputFileName("a21_013_dp_pos.xml");
        getTestHelper().setTestMethodName("testXaDataPath_a21_013");
        try {
            FileUtils.copyFile(testFile, fileInCurrentDir);
            evaluateBizDoc();
        } finally {
            fileInCurrentDir.delete();
        }
    }

    /**
     * Test the xa:response instruction with xa:data_path attribute
     * where the data path exercises an Xpath filter to get the
     * second node.  The result is somewhat unintuitive.  The first
     * node of p:shipTo is the implicit text node representing the
     * whitespace between p:shipTo and p:name shown in red below.
     * This makes p:name the second node of p:shipTo.
     * 
     * @throws Exception
     */
    public void testXaDataPath_a31_049() throws Exception {
        clearInputParams();
        setInputXmlFileName(null);
        setBizDocFileName("a31_049_dp_pos.xbd");
        setExpectedOutputFileName("a31_049_dp_pos.xml");
        getTestHelper().setTestMethodName("testXaDataPath_a31_049");
        evaluateBizDoc();
    }
}
