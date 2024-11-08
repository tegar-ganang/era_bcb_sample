package org.web3d.x3d.tools.x3db;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.SAXException;

/**
 * JUnit-based tests for X3dCanonicalizer.java.  Most tests results will need
 * further verification via examining individual result files output to the
 * testFiles.canonical directory. </p>
 * @author <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizerTest">Terry Norbraten</a>
 * @version $Id: X3dCanonicalizerTest.java 2366 2008-07-11 01:53:50Z tnorbraten $
 * <p>
 *   <dt><b>History:</b>
 *   <pre><b>
 *     Date:     18 September 2006
 *     Time:     1722
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizerTest">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Initial
 *
 *     Date:     24 November 2006
 *     Time:     1630
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizerTest">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Added testTwoArgArrayAsPathsToMain() to test for acceptance
 *                  of an XML file extension as a destination file to hold the
 *                  c14n form of original X3D scene files
 *
 *     Date:     25 December 2006
 *     Time:     1051
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizerTest">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Added testEmptyAttributeValueRemove() to test for removal of
 *                  empty attribute values
 *
 *     Date:     02 January 2007
 *     Time:     2044
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizerTest">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Added testWhitespaceNormalization() to test for default
 *                  whitespace normalization processing
 *               2) Added testCommaSeperatorRemove() to test for removal of
 *                  comma seperators in MF-type array values
 *
 *     Date:     17 FEB 2007
 *     Time:     1652
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizerTest">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Added testBug1155Fix() to support fix for Bug 1155
 *               2) This test class now extends XMLTestCase for XMLUnit testing
 *               3) Functionality tests added to support compliance with Bug 162
 *                  enabling server-side filtering of non-c14n compliant X3D
 *                  scenes.
 *
 *     Date:     15 MAR 2007
 *     Time:     1630
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizerTest">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Strengthened XMLUnit tests for testing of a-v pair sorting
 *                  and empty a-v pair removal
 *               2) Added OverrideDifferenceListener inner class to override
 *                  differences found with removing commas from MFType arrays
 *                  and when empty a-v pairs are removed
 *               3) Implemented the rest of XMLUnit test cases for each type of
 *                  X3D test file
 *               4) Strengthened tests for null arguments
 *               5) Added test for the validate -v switch
 * 
 *     Date:     06 JUL 2008
 *     Time:     0303Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Bugfix 1727.  This X3dC14n did not expect raw data between 
 *                  elements such as <ds:Signature>data....</ds:Signature>.  Now
 *                  that we are signing and encrypting X3D files, this syntax 
 *                  will be encountered.
 *   </b></pre>
 * </p>
 * @see org.web3d.x3d.tools.x3db.X3dCanonicalizer
 */
public class X3dCanonicalizerTest extends XMLTestCase {

    /** log4j log instance */
    static Logger log = Logger.getLogger(X3dCanonicalizerTest.class);

    private ByteBuffer bb = null;

    private FileChannel fc;

    private RandomAccessFile raf = null;

    /** String copy of the XML file undergoing test */
    private String sc;

    private String testFilesDir = "/www.web3d.org/x3d/tools/canonical/test/testFiles/";

    private X3dCanonicalizer x3dc;

    /** @param testName the name of the test method, or class to run */
    public X3dCanonicalizerTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        XMLUnit.setControlParser("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        XMLUnit.setTestParser("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        XMLUnit.setSAXParserFactory("org.apache.xerces.jaxp.SAXParserFactoryImpl");
        XMLUnit.setTransformerFactory("org.apache.xalan.processor.TransformerFactoryImpl");
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalizeWhitespace(true);
        x3dc = new X3dCanonicalizer(new String[] { testFilesDir + "HelloWorld.x3d" });
        try {
            raf = new RandomAccessFile(testFilesDir + "HelloWorld.x3d", "rwd");
            fc = raf.getChannel();
            bb = ByteBuffer.allocate((int) fc.size());
            fc.read(bb);
            raf.close();
        } catch (IOException ioe) {
            log.info("scene is read-only!  Can not process.");
        }
        bb.flip();
        sc = new String(bb.array());
    }

    @Override
    protected void tearDown() throws Exception {
        bb = null;
        raf = null;
        sc = null;
        x3dc.setSceneString(null);
        x3dc.nullFinalC14nScene();
        x3dc = null;
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(X3dCanonicalizerTest.class);
        return suite;
    }

    /** Test of main method rejection of null arguments */
    @org.junit.Test(expected = NullPointerException.class)
    public void testNullArgs1() {
        log.info("testNullArgs, 1st test");
        try {
            X3dCanonicalizer.main(new String[] {});
        } catch (NullPointerException e) {
            log.warn(e);
        }
    }

    /** Test of main method rejection of null arguments */
    @org.junit.Test(expected = NullPointerException.class)
    public void testNullArgs2() {
        System.out.println();
        log.info("testNullArgs, 2nd test");
        try {
            X3dCanonicalizer.main(new String[] { "" });
        } catch (NullPointerException e) {
            log.warn(e);
        }
    }

    /** Test the before and after results of scene processing */
    public void testGetFinalC14nScene() {
        System.out.println();
        log.info("testGetFinalC14nScene");
        String preResult = sc;
        x3dc = new X3dCanonicalizer(sc);
        x3dc.isCanonical();
        String result = x3dc.getFinalC14nScene();
        assertNotNull(preResult);
        assertNotNull(result);
        try {
            assertXMLEqual(preResult, result);
            log.info("Pre and post c14n String results match");
        } catch (IOException ex) {
            log.fatal(ex);
        } catch (SAXException ex) {
            log.fatal(ex);
        }
    }

    /** Test of main method to accept the scene via path and file name.  XMLUnit
     * test will check for sound XML structure of test document from the
     * control document.
     */
    public void testArrayAsPathToMain() {
        System.out.println();
        log.info("testArrayAsPathToMain");
        X3dCanonicalizer.main(new String[] { testFilesDir + "HelloWorld.x3d" });
        X3dToolsXMLUnitTest.main(new String[] { testFilesDir + "HelloWorld.x3d", testFilesDir + "HelloWorldCanonical.x3d" });
    }

    /**
     * Test of main method to accept arguments consisting of the scene via path
     * and file name and the name of the resulting canonical file with .xml
     * extension.  XMLUnit test will check for sound XML structure of test
     * document against the control document.
     */
    public void testTwoArgArrayAsPathsToMain() {
        System.out.println();
        log.info("testTwoArgArrayAsPathsToMain");
        X3dCanonicalizer.main(new String[] { testFilesDir + "HelloWorld.x3d", testFilesDir + "HelloWorldCanonical.xml" });
        X3dToolsXMLUnitTest.main(new String[] { testFilesDir + "HelloWorld.x3d", testFilesDir + "HelloWorldCanonical.xml" });
    }

    /**
     * Test of main method to reject the scene via path and invalid file
     * extension.
     */
    @org.junit.Test(expected = IllegalArgumentException.class)
    public void testInvalidArgsToMain1() {
        System.out.println();
        log.info("testInvalidArgsToMain1");
        try {
            X3dCanonicalizer.main(new String[] { testFilesDir + "HelloWorld.x3e" });
        } catch (IllegalArgumentException e) {
            log.warn(e);
        }
    }

    /**
     * Test of main method to reject the scene via path and filename with
     * invalid flags.
     */
    @org.junit.Test(expected = IllegalArgumentException.class)
    public void testInvalidArgsToMain2() {
        System.out.println();
        log.info("testInvalidArgsToMain2");
        try {
            X3dCanonicalizer.main(new String[] { "-f", "-t", "-a", testFilesDir + "HelloWorld.x33" });
        } catch (IllegalArgumentException e) {
            log.warn(e);
        }
    }

    /** Test of correct processing of whitespace normalization in scenes */
    public void testWhitespaceNormalization() {
        System.out.println();
        log.info("testWhitespaceNormalization");
        X3dCanonicalizer.main(new String[] { testFilesDir + "TestWhitespaceNormalization.x3d" });
        X3dToolsXMLUnitTest.main(new String[] { testFilesDir + "TestWhitespaceNormalization.x3d", testFilesDir + "TestWhitespaceNormalizationCanonical.x3d" });
    }

    /** Test of correct processing of comments contained in scenes */
    public void testComment() {
        System.out.println();
        log.info("testComment");
        X3dCanonicalizer.main(new String[] { testFilesDir + "TestComment.x3d" });
        X3dToolsXMLUnitTest.main(new String[] { testFilesDir + "TestComment.x3d", testFilesDir + "TestCommentCanonical.x3d" });
    }

    /** Test of correct processing of internally declared DOCTYPEs */
    public void testInternalDOCTYPEs() {
        System.out.println();
        log.info("testInternalDOCTYPEs, 1st test");
        X3dCanonicalizer.main(new String[] { testFilesDir + "TestInternalDTDQuadTreeExamples.x3d" });
        X3dToolsXMLUnitTest.main(new String[] { testFilesDir + "TestInternalDTDQuadTreeExamples.x3d", testFilesDir + "TestInternalDTDQuadTreeExamplesCanonical.x3d" });
        System.out.println();
        log.info("testInternalDOCTYPEs, 2nd test");
        X3dCanonicalizer.main(new String[] { testFilesDir + "TestInternalSubsetDeclaration.x3d" });
        X3dToolsXMLUnitTest.main(new String[] { testFilesDir + "TestInternalSubsetDeclaration.x3d", testFilesDir + "TestInternalSubsetDeclarationCanonical.x3d" });
    }

    /** Test of correct processing of CDATA sections of a scene */
    public void testCDATA() {
        System.out.println();
        log.info("testCDATA");
        X3dCanonicalizer.main(new String[] { testFilesDir + "TestCDATAFigure30.1ScriptSlidingBall.x3d" });
        X3dToolsXMLUnitTest.main(new String[] { testFilesDir + "TestCDATAFigure30.1ScriptSlidingBall.x3d", testFilesDir + "TestCDATAFigure30.1ScriptSlidingBallCanonical.x3d" });
    }

    /** Test of correct processing of closing element as singletons */
    public void testElementCloseAsSingleton() {
        System.out.println();
        log.info("testElementCloseAsSingleton");
        X3dCanonicalizer.main(new String[] { testFilesDir + "TestElementCloseAsSingleton.x3d" });
        X3dToolsXMLUnitTest.main(new String[] { testFilesDir + "TestElementCloseAsSingleton.x3d", testFilesDir + "TestElementCloseAsSingletonCanonical.x3d" });
    }

    /**
     * Test of correct processing of apostrophes within SFString and MFString[]
     * a character entity &apos;
     */
    public void testApostropheResolution() {
        System.out.println();
        log.info("testApostropheResolution");
        X3dCanonicalizer.main(new String[] { testFilesDir + "TestApostropheResolution.x3d" });
        X3dToolsXMLUnitTest.main(new String[] { testFilesDir + "TestApostropheResolution.x3d", testFilesDir + "TestApostropheResolutionCanonical.x3d" });
    }

    /**
     * Test of correct processing of apostrophes within SFString and MFString[]
     * a character entity &quot;
     */
    public void testQuoteResolution() {
        System.out.println();
        log.info("testQuoteResolution");
        X3dCanonicalizer.main(new String[] { testFilesDir + "TestQuoteResolution.x3d" });
        X3dToolsXMLUnitTest.main(new String[] { testFilesDir + "TestQuoteResolution.x3d", testFilesDir + "TestQuoteResolutionCanonical.x3d" });
    }

    /**
     * Test of correctly sorting attribute-value (a-v) pairs containing DEF, USE
     * and containerField attributes
     */
    public void testAttributeSort() {
        System.out.println();
        log.info("testAttributeSort");
        X3dCanonicalizer.main(new String[] { testFilesDir + "TestAttributeSort.x3d" });
        X3dToolsXMLUnitTest.main(new String[] { testFilesDir + "TestAttributeSort.x3d", testFilesDir + "TestAttributeSortCanonical.x3d" });
    }

    /** Test of correctly eliminating empty a-v pairs */
    public void testEmptyAttributeValuePairRemove() {
        System.out.println();
        log.info("testEmptyAttributeValuePairRemove");
        X3dCanonicalizer.main(new String[] { testFilesDir + "TestEmptyAttributeValuePairRemove.x3d" });
        X3dToolsXMLUnitTest.main(new String[] { testFilesDir + "TestEmptyAttributeValuePairRemove.x3d", testFilesDir + "TestEmptyAttributeValuePairRemoveCanonical.x3d" });
    }

    /**
     * Test of correctly eliminating comma seperators between MF-type array
     * values
     */
    public void testRemoveCommaSeperator() {
        System.out.println();
        log.info("testRemoveCommaSeperator");
        X3dCanonicalizer.main(new String[] { testFilesDir + "TestRemoveCommaSeperator.x3d" });
        X3dToolsXMLUnitTest.main(new String[] { testFilesDir + "TestRemoveCommaSeperator.x3d", testFilesDir + "TestRemoveCommaSeperatorCanonical.x3d" });
    }

    /**
     * Tests that *Canonical.xml file grows, or shrinks according to original
     * scene content proportion.  Supports fix for Bug 1155.  Essentially, this
     * test first creates a larger control *Canonical.xml than the test.  The
     * test, on purpose, is smaller, but named to be the *Canonical.xml when
     * processed.  This tests the FileChannel's failure to completely overwrite
     * all previous content especially if newer content is less than the
     * original.  A FileChannel must be truncated to subsequent content size,
     * and its pointer reset to 0 before overwriting new content to an existing
     * file.
     */
    public void testBug1155Fix() {
        System.out.println();
        log.info("testBug1155Fix: 1st process run");
        x3dc.isCanonical();
        x3dc.close();
        System.out.println();
        log.info("testBug1155Fix: 2nd process run");
        X3dCanonicalizer.main(new String[] { testFilesDir + "Test30DTD.x3d", testFilesDir + "HelloWorldCanonical.xml" });
        try {
            assertXMLEqual(new FileReader(testFilesDir + "Test30DTD.x3d"), new FileReader(testFilesDir + "HelloWorldCanonical.xml"));
            log.info("Smaller file successfully overwrote larger one");
        } catch (FileNotFoundException ex) {
            log.warn(ex);
        } catch (IOException ex) {
            log.warn(ex);
        } catch (SAXException ex) {
            log.warn(ex);
        }
    }

    /** Test for existence of c14n compliant scenes */
    public void testC14nCompliantScene() {
        System.out.println();
        log.info("testC14nCompliantScene");
        x3dc = new X3dCanonicalizer(new String[] { testFilesDir + "C14nHelloWorld.x3d" });
        assertTrue(x3dc.isCanonical());
    }

    /** Test of main method parsing for validate switch */
    public void testValidateSwitch() {
        System.out.println();
        log.info("testValidateSwitch");
        X3dCanonicalizer.main(new String[] { "-v", testFilesDir + "HelloWorld.x3d" });
    }

    /** Test of Bugfix 1727 */
    public void testBug1727Fix() {
        System.out.println();
        log.info("testBug1727Fix");
        x3dc = new X3dCanonicalizer(new String[] { testFilesDir + "TestHelloWorldSigned.x3d" });
        assertTrue(x3dc.isDigitallySigned());
    }
}
