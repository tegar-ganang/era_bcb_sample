package test.com.wutka.jox;

import java.util.Vector;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import com.wutka.jox.JOXBeanDOM;
import com.wutka.jox.JOXBeanBuilder;
import com.wutka.jox.JOXSAXBeanInput;
import com.wutka.jox.JOXBeanOutput;
import com.wutka.jox.JOXBeanWriter;
import com.wutka.jox.JOXBeanReader;
import com.wutka.dtd.DTDParser;
import com.wutka.dtd.DTD;
import java.io.File;
import java.io.StringWriter;
import java.io.StringReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import java.io.IOException;
import java.lang.RuntimeException;

/** Test round trip, from bean->xml->bean. Including sax events, dom creation, 
and string'ified. We are only testing for correctness, not trying to diagnose
individual features, so a failure may require forensics. 
*
*<pre>
* To Test:
* Base bean, with atomics, lists, dates, special characters
*      \                bean->    bean->    bean->
* style \ method         sax->     dom->     String->
*        \              bean      bean      bean
* without dtd             ?         ?         ?
* as attributes           ?         ?         ?
* with dtd                ?         ?         ?
*   (rename, exclude
*   etc.)
* with a dateformat       ?         ?         ?
*</pre>
*/
public class test_RoundTrip extends JoxTestCase {

    public test_RoundTrip(String n) {
        super(n);
    }

    /** bean-> XML String ->bean */
    public void testXML() {
        TestBeanDTD1 sourceBean = (TestBeanDTD1) this.setupComplexTestBean();
        Vector xmlResult = new Vector(4);
        try {
            System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");
            {
                StringWriter w = new StringWriter(1000);
                (new JOXBeanWriter(w, false)).writeObject("MarkTest", sourceBean);
                xmlResult.add(w);
            }
            {
                StringWriter w = new StringWriter(1000);
                (new JOXBeanWriter(w, true)).writeObject("MarkTest", sourceBean);
                xmlResult.add(w);
            }
            {
                StringWriter w = new StringWriter(1000);
                JOXBeanWriter withDate = new JOXBeanWriter(w, false);
                withDate.setDateFormat(new SimpleDateFormat("HH ss mm dd MM z yyyy"));
                withDate.writeObject("MarkTest", sourceBean);
                xmlResult.add(w);
            }
            {
                StringWriter w = new StringWriter(1000);
                (new JOXBeanWriter(this.readDTD("testDOM3.dtd"), w)).writeObject("MarkTest", sourceBean);
                xmlResult.add(w);
            }
        } catch (IOException e) {
            throw new RuntimeException("while making xmlResults, @" + (xmlResult.size() - 1) + " " + e.getMessage());
        }
        int ct = 0;
        try {
            for (Iterator itor = xmlResult.iterator(); itor.hasNext(); ) {
                String axmlResult = ((StringWriter) itor.next()).toString();
                Object configuredBean = (new JOXBeanReader(new StringReader(axmlResult))).readObject(TestBeanDTD1.class);
                assertEquals("XML roundtrip " + ct, sourceBean, configuredBean);
                ct++;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("while processing, @" + ct + " " + e.getMessage() + "\n");
        }
    }

    /** bean->DOM->bean */
    public void testDOM() {
        TestBeanDTD1 sourceBean = (TestBeanDTD1) this.setupComplexTestBean();
        Vector domMaker = new Vector(4);
        try {
            domMaker.add(new JOXBeanDOM(false));
            domMaker.add(new JOXBeanDOM(true));
            JOXBeanDOM withDate = new JOXBeanDOM(false);
            withDate.setDateFormat(new SimpleDateFormat("HH ss mm dd MM z yyyy"));
            domMaker.add(withDate);
            domMaker.add(new JOXBeanDOM(this.readDTD("testDOM3.dtd")));
        } catch (IOException e) {
            throw new RuntimeException("while making domMakers, @" + (domMaker.size() - 1) + " " + e.getMessage());
        }
        int ct = 0;
        try {
            for (Iterator itor = domMaker.iterator(); itor.hasNext(); ) {
                JOXBeanDOM aDomMaker = (JOXBeanDOM) itor.next();
                Document dom = aDomMaker.beanToDocument("MarkTest", sourceBean);
                Element e = dom.getDocumentElement();
                Object configuredBean = (new JOXBeanBuilder(e)).readObject(TestBeanDTD1.class);
                assertEquals("DOM roundtrip " + ct, sourceBean, configuredBean);
                ct++;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("while processing, @" + ct + " " + e.getMessage() + "\n");
        }
    }

    public void testSAX() {
        TestBeanDTD1 sourceBean = (TestBeanDTD1) this.setupComplexTestBean();
        Vector saxMaker = new Vector(4);
        saxMaker.add(new JOXBeanOutput(new JOXSAXBeanInput(new TestBeanDTD1())));
        saxMaker.add(new JOXBeanOutput(new JOXSAXBeanInput(new TestBeanDTD1()), true));
        JOXBeanOutput withDate = new JOXBeanOutput(new JOXSAXBeanInput(new TestBeanDTD1()));
        withDate.setDateFormat(new SimpleDateFormat("HH ss mm dd MM z yyyy"));
        saxMaker.add(withDate);
        saxMaker.add(new JOXBeanOutput(new JOXSAXBeanInput(new TestBeanDTD1()), this.readDTD("testDOM3.dtd")));
        int ct = 0;
        try {
            for (Iterator itor = saxMaker.iterator(); itor.hasNext(); ) {
                JOXBeanOutput aSaxMaker = (JOXBeanOutput) itor.next();
                aSaxMaker.writeObject("MarkTest", sourceBean);
                assertEquals("Sax roundtrip " + ct, sourceBean, ((JOXSAXBeanInput) aSaxMaker.getContentHandler()).getBean());
                ct++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    protected DTD readDTD(String fileName) {
        File dtdFile = this.locateTestFile(fileName);
        DTD dtd = null;
        try {
            DTDParser dtdParser = new DTDParser(dtdFile, false);
            dtd = dtdParser.parse(false);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return dtd;
    }
}
