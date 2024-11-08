package org.dom4j.io;

import junit.textui.TestRunner;
import java.io.StringReader;
import java.io.StringWriter;
import org.dom4j.AbstractTestCase;
import org.dom4j.Document;

/**
 * A simple test harness to check that the XML Writer works
 * 
 * @author <a href="mailto:james.strachan@metastuff.com">James Strachan </a>
 * @version $Revision: 1.3 $
 */
public class WriteUnmergedTextTest extends AbstractTestCase {

    protected static final boolean VERBOSE = true;

    private String inputText = "<?xml version = \"1.0\"?>" + "<TestEscapedEntities><TEXT>Test using &lt; " + "&amp; &gt;</TEXT></TestEscapedEntities>";

    public static void main(String[] args) {
        TestRunner.run(WriteUnmergedTextTest.class);
    }

    public String readwriteText(OutputFormat outFormat, boolean mergeAdjacentText) throws Exception {
        StringWriter out = new StringWriter();
        StringReader in = new StringReader(inputText);
        SAXReader reader = new SAXReader();
        reader.setMergeAdjacentText(mergeAdjacentText);
        Document document = reader.read(in);
        XMLWriter writer = (outFormat == null) ? new XMLWriter(out) : new XMLWriter(out, outFormat);
        writer.write(document);
        writer.close();
        String outText = out.toString();
        return outText;
    }

    public void testWithoutFormatNonMerged() throws Exception {
        String outText = readwriteText(null, false);
        if (VERBOSE) {
            log("Text output is [");
            log(outText);
            log("]. Done");
        }
        assertTrue("Output text contains \"&amp;\"", outText.lastIndexOf("&amp;") >= 0);
        assertTrue("Output text contains \"&lt;\"", outText.lastIndexOf("&lt;") >= 0);
    }

    public void testWithCompactFormatNonMerged() throws Exception {
        String outText = readwriteText(OutputFormat.createCompactFormat(), false);
        if (VERBOSE) {
            log("Text output is [");
            log(outText);
            log("]. Done");
        }
        assertTrue("Output text contains \"&amp;\"", outText.lastIndexOf("&amp;") >= 0);
        assertTrue("Output text contains \"&lt;\"", outText.lastIndexOf("&lt;") >= 0);
    }

    public void testWithPrettyPrintFormatNonMerged() throws Exception {
        String outText = readwriteText(OutputFormat.createPrettyPrint(), false);
        if (VERBOSE) {
            log("Text output is [");
            log(outText);
            log("]. Done");
        }
        assertTrue("Output text contains \"&amp;\"", outText.lastIndexOf("&amp;") >= 0);
        assertTrue("Output text contains \"&lt;\"", outText.lastIndexOf("&lt;") >= 0);
    }

    public void testWithoutFormatMerged() throws Exception {
        String outText = readwriteText(null, true);
        if (VERBOSE) {
            log("Text output is [");
            log(outText);
            log("]. Done");
        }
        assertTrue("Output text contains \"&amp;\"", outText.lastIndexOf("&amp;") >= 0);
        assertTrue("Output text contains \"&lt;\"", outText.lastIndexOf("&lt;") >= 0);
    }

    public void testWithCompactFormatMerged() throws Exception {
        String out = readwriteText(OutputFormat.createCompactFormat(), true);
        if (VERBOSE) {
            log("Text output is [");
            log(out);
            log("]. Done");
        }
        assertTrue("Output text contains \"&amp;\"", out.lastIndexOf("&amp;") >= 0);
        assertTrue("Output text contains \"&lt;\"", out.lastIndexOf("&lt;") >= 0);
    }

    public void testWithPrettyPrintFormatMerged() throws Exception {
        String outText = readwriteText(OutputFormat.createPrettyPrint(), true);
        if (VERBOSE) {
            log("Text output is [");
            log(outText);
            log("]. Done");
        }
        assertTrue("Output text contains \"&amp;\"", outText.lastIndexOf("&amp;") >= 0);
        assertTrue("Output text contains \"&lt;\"", outText.lastIndexOf("&lt;") >= 0);
    }
}
