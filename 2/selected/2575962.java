package org.apache.batik.dom;

import org.w3c.dom.*;
import java.io.*;
import java.net.*;
import org.apache.batik.dom.util.*;
import org.apache.batik.util.*;
import org.apache.batik.test.*;

/**
 * This class tests the empty string value for an xmlns attribute.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id: NullNamespaceTest.java 475477 2006-11-15 22:44:28Z cam $
 */
public class NullNamespaceTest extends AbstractTest {

    public static String ERROR_GET_ELEMENT_BY_ID_FAILED = "error.get.element.by.id.failed";

    public static String ENTRY_KEY_ID = "entry.key.id";

    protected String testFileName;

    protected String rootTag;

    protected String targetId;

    public NullNamespaceTest(String file, String root, String id) {
        testFileName = file;
        rootTag = root;
        targetId = id;
    }

    public TestReport runImpl() throws Exception {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        DocumentFactory df = new SAXDocumentFactory(GenericDOMImplementation.getDOMImplementation(), parser);
        File f = (new File(testFileName));
        URL url = f.toURL();
        Document doc = df.createDocument(null, rootTag, url.toString(), url.openStream());
        Element e = doc.getElementById(targetId);
        if (e == null) {
            DefaultTestReport report = new DefaultTestReport(this);
            report.setErrorCode(ERROR_GET_ELEMENT_BY_ID_FAILED);
            report.addDescriptionEntry(ENTRY_KEY_ID, targetId);
            report.setPassed(false);
            return report;
        }
        if (e.getNamespaceURI() == null) {
            return reportSuccess();
        }
        DefaultTestReport report = new DefaultTestReport(this);
        report.setErrorCode(TestReport.ERROR_TEST_FAILED);
        report.setPassed(false);
        return report;
    }
}
