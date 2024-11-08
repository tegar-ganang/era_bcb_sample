package org.apache.batik.dom;

import org.w3c.dom.*;
import java.io.*;
import java.net.*;
import org.apache.batik.dom.util.*;
import org.apache.batik.util.*;
import org.apache.batik.test.*;

/**
 * This class tests the getElementsByTagNameNS method.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id: GetElementsByTagNameNSTest.java 475477 2006-11-15 22:44:28Z cam $
 */
public class GetElementsByTagNameNSTest extends AbstractTest {

    protected String testFileName;

    protected String rootTag;

    protected String tagName;

    public GetElementsByTagNameNSTest(String file, String root, String tag) {
        testFileName = file;
        rootTag = root;
        tagName = tag;
    }

    public TestReport runImpl() throws Exception {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        DocumentFactory df = new SAXDocumentFactory(GenericDOMImplementation.getDOMImplementation(), parser);
        File f = (new File(testFileName));
        URL url = f.toURL();
        Document doc = df.createDocument(null, rootTag, url.toString(), url.openStream());
        Element root = doc.getDocumentElement();
        NodeList lst = root.getElementsByTagNameNS(null, tagName);
        if (lst.getLength() != 1) {
            DefaultTestReport report = new DefaultTestReport(this);
            report.setErrorCode("error.getElementByTagNameNS.failed");
            report.setPassed(false);
            return report;
        }
        Node n;
        while ((n = root.getFirstChild()) != null) {
            root.removeChild(n);
        }
        if (lst.getLength() != 0) {
            DefaultTestReport report = new DefaultTestReport(this);
            report.setErrorCode("error.getElementByTagNameNS.failed");
            report.setPassed(false);
            return report;
        }
        return reportSuccess();
    }
}
