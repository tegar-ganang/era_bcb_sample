package org.xaware.testing.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import junit.framework.Assert;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.springframework.core.io.Resource;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.xaware.api.BizViewRequestOptions;
import org.xaware.api.BizViewResponse;
import org.xaware.api.IBizViewRequestOptions;
import org.xaware.api.XABizView;
import org.xaware.server.resources.ResourceHelper;
import org.xaware.shared.util.FileUtils;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareParsingException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * Base class for Test Helper objects. This class provides the basic methods for manipulating BizDocuments and
 * BizComponents needed to complete the testing objective.
 * 
 * @author hcurtis
 */
public class XAwareNewTestHelper {

    /** Constant for the System property name (SPN_) for the directory holding the lax files. */
    private static final String SPN_LAX_DIR = "lax.dir";

    /** Constant for the System property name (SPN_) for jaas config. */
    private static final String SPN_JAVA_SECURITY_AUTH_LOGIN_CONFIG = "java.security.auth.login.config";

    /** Constant for the System property name (SPN_) for the XAware home directory. */
    private static final String SPN_XAWARE_HOME = "xaware.home";

    private static SVNWCClient svnWorkingCopyClient;

    protected Class testCase = null;

    protected XAwareLogger log = null;

    protected String xawareHome = null;

    protected String expectedRootElement = "Document";

    protected String testDataFolder = null;

    protected List<String> generatedFiles = new ArrayList<String>();

    protected String testMethodName = null;

    private Exception expectedException;

    public XAwareNewTestHelper(final Class testCase, final String dataFolder) {
        String errMsg = null;
        this.testCase = testCase;
        this.testDataFolder = dataFolder;
        log = XAwareLogger.getXAwareLogger(testCase.getName());
        xawareHome = System.getProperty(XAwareNewTestHelper.SPN_XAWARE_HOME);
        if (xawareHome == null) {
            errMsg = "System Property xaware.home has not been created.";
            log.error(errMsg);
        } else {
            System.setProperty(XAwareNewTestHelper.SPN_JAVA_SECURITY_AUTH_LOGIN_CONFIG, xawareHome + File.separator + "conf" + File.separator + "jaas.config");
            System.setProperty(XAwareNewTestHelper.SPN_LAX_DIR, xawareHome + File.separator);
        }
        try {
            XABizView.initialize("servlet");
        } catch (XAwareException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
        final Element logElem = org.xaware.shared.util.logging.LogConfigUtil.getInstance().getLogElement();
        if (logElem != null) {
            org.xaware.shared.util.logging.XAwareLogger.getXAwareLogger(XAwareNewTestHelper.class.getName());
        } else {
            errMsg = "No logging specification in XAsystem.xml";
            log.error(errMsg);
            Assert.fail(errMsg);
        }
    }

    /**
     * This is the main evaluation method with all the parameters. It is overloaded by method signatures that allow
     * fewer parameters. Use setExpectedException() to provide the exception to be compared when it is expected to have
     * an exception thrown by execution of the BizDocument.
     * 
     * @param expectedResponse
     * @param bizDocFileName
     * @param inputParams
     * @param saveOutput
     * @param transformOutputToExpected
     * @param os
     * @param resourcePath
     * @param inputXML
     */
    public void evaluateBizDoc(final String expectedResponse, final String bizDocFileName, final Map<String, Object> inputParams, final boolean saveOutput, final boolean transformOutputToExpected, final OutputStream os, final String resourcePath, final String inputXML) {
        Document bizDocResult = null;
        Exception actualException = null;
        final IBizViewRequestOptions options = new BizViewRequestOptions();
        options.setBizViewName(testDataFolder + bizDocFileName);
        if (inputXML != null) {
            if (inputXML.trim().startsWith("<")) {
                options.setInputXmlSerialized(inputXML);
            } else {
                options.setInputXmlResourceName(inputXML);
            }
        }
        if (inputParams != null) {
            options.setInputParams(inputParams);
        }
        if (os != null) {
            options.setOutputStream(os);
        }
        if (resourcePath != null) {
            options.setResourcePath(resourcePath);
        }
        try {
            final BizViewResponse response = XABizView.loadAndExecute(options);
            bizDocResult = response.getResultingDocument();
            actualException = response.getExecutionException();
        } catch (final Exception e) {
            actualException = e;
        }
        evaluateExecutionResults(expectedResponse, bizDocFileName, saveOutput, transformOutputToExpected, bizDocResult, actualException);
    }

    /**
     * 
     * @param expectedResponse
     * @param bizDocFileName
     * @param saveOutput
     * @param transformOutputToExpected
     * @param bizDocResult
     * @param actualException
     */
    public void evaluateExecutionResults(final String expectedResponse, final String bizDocFileName, final boolean saveOutput, final boolean transformOutputToExpected, final Document bizDocResult, final Exception actualException) {
        if (actualException != null) {
            if (expectedException != null) {
                try {
                    Assert.assertEquals(expectedException.getMessage(), actualException.getMessage());
                    Assert.assertEquals(expectedException.getClass().getName(), actualException.getClass().getName());
                    return;
                } catch (final Error e) {
                    System.out.println("Expected exception: " + expectedException);
                    System.out.println("Actual exception: " + actualException);
                    throw e;
                }
            }
            actualException.printStackTrace(System.out);
            Assert.fail(actualException.getMessage());
        }
        final Element output = bizDocResult.getRootElement();
        Assert.assertNotNull("No output from BizDocument " + bizDocFileName, output);
        if (saveOutput) {
            dumpXMLStructure(output, getDumpFileName());
            createExpectedOutputFile(expectedResponse, transformOutputToExpected, output);
        }
        final XMLComparator comparator = new XMLComparator(log);
        if (expectedResponse != null) {
            comparator.compareExpected(testDataFolder + expectedResponse, output);
        }
        log.info("Successfully compared expected output with BizDocument output");
    }

    /**
     * Evaluation method for when inputXML is provided as an already parsed Document. Where possible, the overloaded
     * version of this method which takes the inputXML as a String should be used instead.
     * 
     * @param expectedResponse
     * @param bizDocFileName
     * @param inputParams
     * @param inputXmlDoc
     * @param saveOutput
     * @param transformOutputToExpected
     * @param os
     * @param resourcePath
     */
    public void evaluateBizDoc(final String expectedResponse, final String bizDocFileName, final Map<String, Object> inputParams, final Document inputXmlDoc, final boolean saveOutput, final boolean transformOutputToExpected, final OutputStream os, final String resourcePath) {
        Document bizDocResult = null;
        Exception actualException = null;
        final IBizViewRequestOptions options = new BizViewRequestOptions();
        options.setBizViewName(testDataFolder + bizDocFileName);
        if (inputXmlDoc != null) {
            options.setInputXmlDocument(inputXmlDoc);
        }
        if (inputParams != null) {
            options.setInputParams(inputParams);
        }
        if (os != null) {
            options.setOutputStream(os);
        }
        if (resourcePath != null) {
            options.setResourcePath(resourcePath);
        }
        try {
            final BizViewResponse response = XABizView.loadAndExecute(options);
            bizDocResult = response.getResultingDocument();
            evaluateExecutionResults(expectedResponse, bizDocFileName, saveOutput, transformOutputToExpected, bizDocResult, actualException);
        } catch (final Exception e) {
            actualException = e;
        }
    }

    /**
     * Reads in the InputXML from a file and then delegates to another evaluateBizDoc().
     * 
     * @param expectedOutputFileName
     * @param bizDocFileName
     * @param inputParms
     * @param inputXmlFileName
     * @param saveOutput
     * @param transformOutputToExpected
     * @param os
     * @param resourcePath
     */
    public void evaluateBizDoc(final String expectedOutputFileName, final String bizDocFileName, final Map<String, Object> inputParms, final String inputXmlFileName, final boolean saveOutput, final boolean transformOutputToExpected, final OutputStream os, final String resourcePath) {
        final String inputXML = readInputXmlFileIntoString(inputXmlFileName);
        evaluateBizDoc(expectedOutputFileName, bizDocFileName, inputParms, saveOutput, transformOutputToExpected, os, resourcePath, inputXML);
    }

    /**
     * This method does the actual work of executing the BizDoc. It is used when outstreaming is involved. so that the
     * outstreamed results can be compared.
     * 
     * @param actualOutputFileName
     * @param expectedOutputFileName
     * @param bizDocFileName
     * @param inputParms
     * @param saveOutput
     * @param transformOutputToExpected
     * @param inputXML
     * @param expectedBizDocStructureFileName
     */
    public void evaluateBizDocStreamedOutput(final String actualOutputFileName, final String expectedOutputFileName, final String bizDocFileName, final Map<String, Object> inputParms, final boolean saveOutput, final boolean transformOutputToExpected, final String inputXML, final String expectedBizDocStructureFileName) {
        Document bizDocResult = null;
        final IBizViewRequestOptions options = new BizViewRequestOptions();
        options.setBizViewName(testDataFolder + bizDocFileName);
        if (inputXML != null) {
            options.setInputXmlSerialized(inputXML);
        }
        if (inputParms != null) {
            options.setInputParams(inputParms);
        }
        try {
            final BizViewResponse response = XABizView.loadAndExecute(options);
            bizDocResult = response.getResultingDocument();
        } catch (final Exception e) {
            if (expectedException != null) {
                Assert.assertEquals(expectedException.getMessage(), e.getMessage());
                Assert.assertEquals(expectedException.getClass().getName(), e.getClass().getName());
                return;
            }
            e.printStackTrace(System.out);
            Assert.fail(e.getMessage());
        }
        final Element output = bizDocResult.getRootElement();
        Assert.assertNotNull("No output from BizDocument " + bizDocFileName, output);
        Assert.assertTrue("Expected BizDocument root output to be <Document>", expectedRootElement.equals(output.getName()));
        if (saveOutput) {
            dumpXMLStructure(output, getDumpFileName());
            createExpectedOutputFileFromStreamOut(expectedOutputFileName, transformOutputToExpected, actualOutputFileName);
            createExpectedOutputFile(expectedBizDocStructureFileName, transformOutputToExpected, output);
        }
        final XMLComparator comparator = new XMLComparator(log);
        comparator.compareExpected(testDataFolder + expectedOutputFileName, testDataFolder + actualOutputFileName);
        comparator.compareExpected(testDataFolder + expectedBizDocStructureFileName, output);
        log.info("Successfully compared expected output with BizDocument output");
    }

    /**
     * Delegates to the other evaluateBizDocStreamedOutput() handing in an empty map and a null Input XML String.
     * 
     * @param actualOutputFileName
     * @param expectedOutputFileName
     * @param bizDocFileName
     * @param saveOutput
     */
    public void evaluateBizDocStreamedOutput(final String actualOutputFileName, final String expectedOutputFileName, final String bizDocFileName, final boolean saveOutput, final String expectedBizDocStructureFileName) {
        evaluateBizDocStreamedOutput(actualOutputFileName, expectedOutputFileName, bizDocFileName, new HashMap<String, Object>(0), saveOutput, false, (String) null, expectedBizDocStructureFileName);
    }

    /**
     * Reads the inputXML file into a String and delegates to the other evaluateBizDocStreamedOutput().
     * 
     * @param actualOutputFileName
     * @param expectedOutputFileName
     * @param bizDocFileName
     * @param inputParms
     * @param saveOutput
     * @param transformOutputToExpected
     * @param inputXMLFileName
     */
    public void evaluateBizDocStreamedOutput(final String actualOutputFileName, final String expectedOutputFileName, final String bizDocFileName, final Map<String, Object> inputParms, final String inputXmlFileName, final boolean saveOutput, final boolean transformOutputToExpected, final String expectedBizDocStructureFileName) {
        final String inputXML = readInputXmlFileIntoString(inputXmlFileName);
        evaluateBizDocStreamedOutput(actualOutputFileName, expectedOutputFileName, bizDocFileName, inputParms, saveOutput, transformOutputToExpected, inputXML, expectedBizDocStructureFileName);
    }

    /**
     * Transforms the inputXMLDoc into a String and delegates to the other evaluateBizDocStreamedOutput().
     * 
     * @param actualOutputFileName
     * @param expectedOutputFileName
     * @param bizDocFile
     * @param params
     * @param inputXmlDoc
     * @param dumpResults
     * @param saveOutputAsExpected
     */
    public void evaluateBizDocStreamedOutput(final String actualOutputFileName, final String expectedOutputFileName, final String bizDocFileName, final Map<String, Object> inputParms, final Document inputXmlDoc, final boolean saveOutput, final boolean transformOutputToExpected, final String expectedBizDocStructureFileName) {
        final String inputXML = (new XMLOutputter()).outputString(inputXmlDoc);
        evaluateBizDocStreamedOutput(actualOutputFileName, expectedOutputFileName, bizDocFileName, inputParms, saveOutput, transformOutputToExpected, inputXML, expectedBizDocStructureFileName);
    }

    /**
     * @param expectedOutputFileName
     * @param transformOutputToExpected
     * @param actualOutputFileName
     * @throws XAwareParsingException
     */
    private void createExpectedOutputFileFromStreamOut(final String expectedOutputFileName, boolean transformOutputToExpected, final String actualOutputFileName) {
        if (!transformOutputToExpected) {
            return;
        }
        Document outDoc = null;
        Element outElem = null;
        try {
            outDoc = FileUtils.getDocumentFromFile(testDataFolder + actualOutputFileName);
            outElem = outDoc.getRootElement();
        } catch (final JDOMException e) {
            Assert.fail("JDOM Exception parsing file:" + e.getMessage() + "  File :" + actualOutputFileName);
        } catch (final IOException e) {
            Assert.fail("IO Exception parsing file:" + e.getMessage() + "  File :" + actualOutputFileName);
        }
        createExpectedOutputFile(expectedOutputFileName, transformOutputToExpected, outElem);
    }

    /**
     * 
     * @param actualOutputFileName
     * @param expectedOutputFileName
     * @param bizDocFile
     * @param params
     * @param inputXmlDoc
     * @param dumpResults
     * @param transformOutputToExpected
     */
    public void evaluateBizDocStreamedOutputFileContents(final String actualOutputFileName, final String expectedOutputFileName, final boolean transformOutputToExpected) {
        final File actualOutputFile = new File(testDataFolder + actualOutputFileName);
        Assert.assertTrue("Streaming output file not created", actualOutputFile.isFile());
        addGeneratedFile(actualOutputFile);
        if (transformOutputToExpected) {
            Document actualStreamedOutput = null;
            try {
                actualStreamedOutput = FileUtils.getDocumentFromFile(testDataFolder + actualOutputFileName);
            } catch (final JDOMException e) {
                Assert.fail("JDOM Exception parsing file: " + e.getMessage() + "  File: " + actualOutputFileName);
            } catch (final IOException e) {
                Assert.fail("IO Exception reading file: " + e.getMessage() + "  File: " + actualOutputFileName);
            }
            final Element outputElem = actualStreamedOutput.getRootElement();
            createExpectedOutputFile(expectedOutputFileName, transformOutputToExpected, outputElem);
        }
        final XMLComparator comparator = new XMLComparator(log);
        comparator.compareExpected(testDataFolder + expectedOutputFileName, testDataFolder + actualOutputFileName);
        log.info("Successfully compared expected streaming output with actual output");
    }

    /**
     * Reads an inputXML file contents into a String.
     * 
     * @param inputXMLFileName
     *            the name of the input XML file.
     * @return a String containing the contents of the file.
     */
    private String readInputXmlFileIntoString(final String inputXMLFileName) {
        String inputXML = null;
        if (inputXMLFileName != null) {
            final String fileName = testDataFolder + inputXMLFileName;
            try {
                inputXML = FileUtils.getFileContentsAsString(fileName);
            } catch (final IOException ioe) {
                final String errMsg = "IO Exception reading file:" + ioe.getMessage() + "  File name:" + fileName;
                log.error(errMsg, ioe);
                ioe.printStackTrace(System.out);
                Assert.fail(errMsg);
            }
        }
        return inputXML;
    }

    public void createExpectedOutputFile(final String expectedResponse, final boolean transformOutputToExpected, final Element output) {
        if (transformOutputToExpected) {
            final Element expected = new Element("TESTCASE");
            final List children = output.getChildren();
            final ArrayList<Element> clonedChildren = new ArrayList<Element>(children.size());
            final Iterator childrenIterator = children.iterator();
            while (childrenIterator.hasNext()) {
                final Element aChild = (Element) childrenIterator.next();
                clonedChildren.add((Element) aChild.clone());
            }
            expected.setContent(clonedChildren);
            dumpXMLStructure(expected, expectedResponse);
        }
    }

    /**
     * @return
     */
    public String getDumpFileName() {
        String nameFragment = testCase.getName();
        if (testMethodName != null) {
            nameFragment += "_" + testMethodName;
        }
        return "BizComp_" + nameFragment + "_Output";
    }

    /**
     * Load up an XML Document into a JDOM structure
     * 
     * @param fName
     * @return Return the loaded JDOM structure's root node
     */
    public Document loadXMLStructure(final String jdomName) {
        final String fName = testDataFolder + jdomName;
        Document inputDoc = null;
        if (jdomName != null) {
            try {
                inputDoc = ResourceHelper.loadXmlResourceIntoDocument(fName);
            } catch (final JDOMException e) {
                log.error("JDOM Exception parsing file:" + e.getMessage() + "  File name:" + fName, e);
                Assert.fail("Failed to parse the input XML " + jdomName);
            } catch (final IOException ioe) {
                log.error("IO Exception reading file:" + ioe.getMessage() + "  File name:" + fName, ioe);
                Assert.fail("Failed to read the input XML file" + jdomName);
            }
        }
        return inputDoc;
    }

    /**
     * Write out the DOM structure to and XML file
     * 
     * @param topElement -
     *            The top DOM element that will define the XML structure to be written to in the output XML file
     * @param name -
     *            The file name of the dumped XML
     */
    public void dumpXMLStructure(final Element topElement, final String name) {
        String localName = name;
        if (!localName.toLowerCase().endsWith(".xml") && !localName.toLowerCase().endsWith(".xbc") && !localName.toLowerCase().endsWith(".xbd") && !localName.toLowerCase().endsWith(".xdr")) {
            localName += ".xml";
        }
        FileOutputStream fileStream = null;
        File fileOut = null;
        try {
            final Format pretty = Format.getPrettyFormat();
            pretty.setTextMode(org.jdom.output.Format.TextMode.TRIM_FULL_WHITE);
            final XMLOutputter xmlOutputter = new XMLOutputter(pretty);
            fileOut = new File(testDataFolder + localName);
            if (fileOut.exists()) {
                fileOut.delete();
            }
            fileStream = new FileOutputStream(fileOut);
            xmlOutputter.output(topElement, new OutputStreamWriter(fileStream));
            log.info("Wrote XML " + name + " data to " + fileOut.getPath());
        } catch (final Exception e) {
            final String errMsg = "Failed to write out the XML structure to " + localName + "; " + e.getMessage();
            log.error(errMsg, e);
            Assert.fail(errMsg);
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (final IOException e) {
                }
            }
            addGeneratedFile(fileOut);
        }
    }

    /**
     * Add to the list of generated files. At the end of the test these files may be removed from the system by calling
     * removeGeneratedFiles()
     * 
     * @param f -
     *            File
     */
    public void addGeneratedFile(final File f) {
        generatedFiles.add(f.getAbsolutePath());
    }

    /**
     * Remove all files stored in the generatedFiles array list.
     */
    public void removeGeneratedFiles() {
        final Iterator iFiles = generatedFiles.iterator();
        while (iFiles.hasNext()) {
            final File genFile = new File((String) iFiles.next());
            if (genFile.exists()) {
                if (!genFile.delete()) {
                    log.warn("Failed to remove " + genFile.getAbsolutePath());
                } else {
                    log.info("Removed temp test file " + genFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * This method will change all the file path's seperator characters to be consistent with the local OS
     * 
     * @param filePath -
     *            The file path that will be changed as required
     * @return Returns the new file path where all the file seperators agree with the standard file seperator of the
     *         resident OS
     */
    public String fixFileSeparators(final String filePath) {
        final File file = new File(filePath);
        return file.getAbsolutePath();
    }

    /**
     * @return Returns the xawareHome.
     */
    public String getXawareHome() {
        return xawareHome;
    }

    /**
     * @return Returns the log.
     */
    public XAwareLogger getLog() {
        return log;
    }

    /**
     * @return Returns the testMethodName.
     */
    public String getTestMethodName() {
        return testMethodName;
    }

    /**
     * @param psTestMethodName
     *            The testMethodName to set.
     */
    public void setTestMethodName(final String psTestMethodName) {
        this.testMethodName = psTestMethodName;
    }

    /**
     * @return Returns the expectedRootElement.
     */
    public String getExpectedRootElement() {
        return expectedRootElement;
    }

    /**
     * @param expectedRootElement
     *            The expectedRootElement to set.
     */
    public void setExpectedRootElement(final String expectedRootElement) {
        this.expectedRootElement = expectedRootElement;
    }

    /**
     * Sets the value for the expected exception that will be used to provide a message for comparison with the
     * exception caught when the biz document execution throws.
     * 
     * @param p_exception
     */
    public void setExpectedException(final Exception p_exception) {
        expectedException = p_exception;
    }

    public void evaluateTextFileContents(final String outputFileName, final String expectedOutputFileName) {
        BufferedReader outputBR = null;
        BufferedReader expectedBR = null;
        try {
            try {
                final Resource outputFile = ResourceHelper.getResource(outputFileName);
                outputBR = new BufferedReader(new InputStreamReader(outputFile.getInputStream()));
            } catch (IOException e) {
                Assert.fail("Failed to read " + outputFileName);
            }
            try {
                final Resource expectedFile = ResourceHelper.getResource(expectedOutputFileName);
                expectedBR = new BufferedReader(new InputStreamReader(expectedFile.getInputStream()));
            } catch (IOException e) {
                Assert.fail("Failed to read " + expectedOutputFileName);
            }
            String outputStr = null;
            String expectedStr = null;
            int i = 0;
            do {
                outputStr = outputBR.readLine();
                expectedStr = expectedBR.readLine();
                i++;
                if (outputStr != null && expectedStr != null) {
                    Assert.assertEquals(expectedStr, outputStr);
                } else if (outputStr == null && expectedStr == null) {
                } else {
                    Assert.fail("Lines " + i + " do not match: expected[" + expectedStr + "] vs. actual[" + outputStr + "].");
                }
            } while (outputStr != null && expectedStr != null);
        } catch (final IOException e) {
            String msg = e.getMessage();
            if (msg == null) {
                msg = e.getClass().getName();
            }
            Assert.fail(msg);
        } finally {
            if (outputBR != null) {
                try {
                    outputBR.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
            if (expectedBR != null) {
                try {
                    expectedBR.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void copyTextOutputFileToExpected(final String outputFileName, final String expectedOutputFileName) {
        try {
            FileUtils.copyFile(outputFileName, expectedOutputFileName);
        } catch (final IOException e) {
            String msg = e.getMessage();
            if (msg == null) {
                msg = e.getClass().getName();
            }
            Assert.fail(msg);
        }
    }

    /**
     * Reverts a file in your local working directory back to its base revision.
     * 
     * @param fileToRevert
     *            the File to be reverted
     * @param recurseDirs
     *            true if the reversion of files should recurse directories
     * @throws SVNException
     *             if any problem occurs reverting the specified file.
     */
    public static void revertFile(final File fileToRevert, final boolean recurseDirs) throws SVNException {
        getSVNWCClient().doRevert(fileToRevert, recurseDirs);
    }

    /**
     * Performs lazy initialization of the SVN client.
     * 
     * @return a reference to the initialized SVN client.
     */
    private static SVNWCClient getSVNWCClient() {
        if (svnWorkingCopyClient == null) {
            System.setProperty("svnkit.upgradeWC", "false");
            final ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
            final ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager();
            final SVNClientManager svnClientManager = SVNClientManager.newInstance(options, authManager);
            svnWorkingCopyClient = svnClientManager.getWCClient();
        }
        return svnWorkingCopyClient;
    }
}
