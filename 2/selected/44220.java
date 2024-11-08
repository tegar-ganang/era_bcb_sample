package org.apache.batik.dom;

import org.w3c.dom.*;
import java.io.*;
import java.net.*;
import org.apache.batik.dom.util.*;
import org.apache.batik.util.*;
import org.apache.batik.test.*;

/**
 * To test the Java serialization.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id: SerializationTest.java 475477 2006-11-15 22:44:28Z cam $
 */
public class SerializationTest extends AbstractTest {

    protected String testFileName;

    protected String rootTag;

    protected String parserClassName = XMLResourceDescriptor.getXMLParserClassName();

    public SerializationTest(String file, String root) {
        testFileName = file;
        rootTag = root;
    }

    public TestReport runImpl() throws Exception {
        DocumentFactory df = new SAXDocumentFactory(GenericDOMImplementation.getDOMImplementation(), parserClassName);
        File f = (new File(testFileName));
        URL url = f.toURL();
        Document doc = df.createDocument(null, rootTag, url.toString(), url.openStream());
        File ser1 = File.createTempFile("doc1", "ser");
        File ser2 = File.createTempFile("doc2", "ser");
        try {
            ObjectOutputStream oos;
            oos = new ObjectOutputStream(new FileOutputStream(ser1));
            oos.writeObject(doc);
            oos.close();
            ObjectInputStream ois;
            ois = new ObjectInputStream(new FileInputStream(ser1));
            doc = (Document) ois.readObject();
            ois.close();
            oos = new ObjectOutputStream(new FileOutputStream(ser2));
            oos.writeObject(doc);
            oos.close();
        } catch (IOException e) {
            DefaultTestReport report = new DefaultTestReport(this);
            report.setErrorCode("io.error");
            report.addDescriptionEntry("message", e.getClass().getName() + ": " + e.getMessage());
            report.addDescriptionEntry("file.name", testFileName);
            report.setPassed(false);
            return report;
        }
        InputStream is1 = new FileInputStream(ser1);
        InputStream is2 = new FileInputStream(ser2);
        for (; ; ) {
            int i1 = is1.read();
            int i2 = is2.read();
            if (i1 == -1 && i2 == -1) {
                return reportSuccess();
            }
            if (i1 != i2) {
                DefaultTestReport report = new DefaultTestReport(this);
                report.setErrorCode("difference.found");
                report.addDescriptionEntry("file.name", testFileName);
                report.setPassed(false);
                return report;
            }
        }
    }
}
