package org.apache.batik.dom.svg;

import java.io.File;
import java.net.URL;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.apache.batik.test.AbstractTest;
import org.apache.batik.test.DefaultTestReport;
import org.apache.batik.test.TestReport;
import org.apache.batik.util.XMLResourceDescriptor;

/**
 * This class tests the cloneNode method.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id: CloneNodeTest.java 475477 2006-11-15 22:44:28Z cam $
 */
public class CloneNodeTest extends AbstractTest {

    protected String testFileName;

    protected String targetId;

    public CloneNodeTest(String file, String id) {
        testFileName = file;
        targetId = id;
    }

    public TestReport runImpl() throws Exception {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory df = new SAXSVGDocumentFactory(parser);
        File f = (new File(testFileName));
        URL url = f.toURL();
        Document doc = df.createDocument(url.toString(), url.openStream());
        Element e = doc.getElementById(targetId);
        if (e == null) {
            DefaultTestReport report = new DefaultTestReport(this);
            report.setErrorCode("error.get.element.by.id.failed");
            report.addDescriptionEntry("entry.key.id", targetId);
            report.setPassed(false);
            return report;
        }
        Element celt = (Element) e.cloneNode(true);
        NamedNodeMap attrs = e.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            String ns = attr.getNamespaceURI();
            String name = (ns == null) ? attr.getNodeName() : attr.getLocalName();
            String val = attr.getNodeValue();
            String val2 = celt.getAttributeNS(ns, name);
            if (!val.equals(val2)) {
                DefaultTestReport report = new DefaultTestReport(this);
                report.setErrorCode("error.attr.comparison.failed");
                report.addDescriptionEntry("entry.attr.name", name);
                report.setPassed(false);
                return report;
            }
        }
        return reportSuccess();
    }
}
