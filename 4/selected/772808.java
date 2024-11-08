package research.customTestNG;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class TestNGTools extends AppoloTest {

    private static String rootDir = "C:/java/webActImport/";

    private static final String backUpIndexFile = "indexBackUp.html";

    private static final String resultIndexFile = "index.html";

    public static void setGroups() {
    }

    public static String[] getTests() {
        String[] appoloTests = { "functional.Sample", "testng.ReportError" };
        return appoloTests;
    }

    public static List<String> includeGroups = new ArrayList<String>();

    public static List<String> excludeGroups = new ArrayList<String>();

    private static List<XmlSuite> suites = new ArrayList<XmlSuite>();

    private static List<XmlSuite> getSuites() {
        for (String testName : getTests()) {
            XmlSuite suite = new XmlSuite();
            suite.setName(testName);
            XmlTest test = new XmlTest(suite);
            test.setName(testName);
            test.setIncludedGroups(includeGroups);
            test.setExcludedGroups(excludeGroups);
            List<XmlClass> classes = new ArrayList<XmlClass>();
            classes.add(new XmlClass(testName));
            test.setXmlClasses(classes);
            suites.add(suite);
        }
        return suites;
    }

    public static void configureAndRunTestNG() throws IOException, XPathFactoryConfigurationException, XPathExpressionException, SAXException, ParserConfigurationException {
        TestListenerAdapter tla = new TestListenerAdapter();
        TestNG testng = new TestNG();
        testng.setXmlSuites(TestNGTools.getSuites());
        testng.addListener(tla);
        addNewBlankReports();
        testng.run();
        uppendTests();
    }

    private static void saveFile(Document doc, File f) {
        Source source = new DOMSource(doc);
        Result result = new StreamResult(f);
        Transformer xformer;
        try {
            xformer = TransformerFactory.newInstance().newTransformer();
            xformer.setOutputProperty(OutputKeys.METHOD, "xml");
            xformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            throw new Error(e);
        } catch (TransformerFactoryConfigurationError e) {
            throw new Error(e);
        } catch (TransformerException e) {
            throw new Error(e);
        }
    }

    private static void addNewBlankReports() throws XPathFactoryConfigurationException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
        String[] tests = getTests();
        for (int i = 0; i < tests.length; i++) {
            File newDirectory = new File(rootDir + "test-output/" + tests[i]);
            if (!newDirectory.exists()) {
                newDirectory.mkdir();
            }
        }
        File newFile = new File(rootDir + "test-output/" + resultIndexFile);
        if (newFile.exists()) {
            try {
                copy(resultIndexFile, backUpIndexFile);
            } catch (XPathFactoryConfigurationException e) {
                e.printStackTrace();
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            FileInputStream isIndex = new FileInputStream(rootDir + "test-output/" + resultIndexFile);
            Document docFile = null;
            DocumentBuilderFactory dBFfactory = DocumentBuilderFactory.newInstance();
            XPathFactory factory = XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI);
            XPath xpath = factory.newXPath();
            docFile = dBFfactory.newDocumentBuilder().parse(isIndex);
            String mainTableXpath = "//table";
            Node mainTableNode = (Node) xpath.evaluate(mainTableXpath, docFile, XPathConstants.NODE);
            for (int i = 0; i < tests.length; i++) {
                String firstResultXpath = "//tr[2]";
                Node firstResultNode = (Node) xpath.evaluate(firstResultXpath, docFile, XPathConstants.NODE);
                String singleResultXpath = "//tr[td/a/text()='" + tests[i] + "']";
                if (xpath.evaluate(singleResultXpath, docFile).equals("")) {
                    Node newNode = ((Element) firstResultNode).cloneNode(true);
                    newNode.getFirstChild().setTextContent(tests[i]);
                    ((Element) mainTableNode).appendChild(newNode);
                }
            }
            saveFile(docFile, newFile);
        }
    }

    private static void uppendTests() throws XPathFactoryConfigurationException, SAXException, IOException, ParserConfigurationException, XPathExpressionException {
        File newFile = new File(rootDir + "test-output/" + backUpIndexFile);
        if (!newFile.exists()) {
            copy(resultIndexFile, backUpIndexFile);
        }
        File indexFile = new File(rootDir + "test-output/" + resultIndexFile);
        File backupFile = new File(rootDir + "test-output/" + backUpIndexFile);
        if (backupFile.exists()) {
            FileInputStream isIndex = new FileInputStream(indexFile);
            FileInputStream isIndexBackUp = new FileInputStream(backupFile);
            Document docIndex = null;
            Document docIndexBackUp = null;
            DocumentBuilderFactory dBFfactory = DocumentBuilderFactory.newInstance();
            XPathFactory factory = XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI);
            XPath xpath = factory.newXPath();
            docIndex = dBFfactory.newDocumentBuilder().parse(isIndex);
            docIndexBackUp = dBFfactory.newDocumentBuilder().parse(isIndexBackUp);
            String[] tests = getTests();
            for (int i = 0; i < tests.length; i++) {
                String testXpath = "//tr[td/a/text()='" + tests[i] + "']";
                String tableXpath = "//table";
                Node table = (Node) xpath.evaluate(tableXpath, docIndexBackUp, XPathConstants.NODE);
                Node newChild = docIndexBackUp.importNode((Node) xpath.evaluate(testXpath, docIndex, XPathConstants.NODE), true);
                if (xpath.evaluate(testXpath, docIndexBackUp).equals("")) {
                    table.appendChild(newChild);
                } else {
                    Node oldChild = (Node) xpath.evaluate(testXpath, docIndexBackUp, XPathConstants.NODE);
                    table.replaceChild(newChild, oldChild);
                }
            }
            docIndexBackUp.normalize();
            saveFile(docIndexBackUp, indexFile);
        }
        backupFile.delete();
    }

    private static void copy(String srcFilename, String dstFilename) throws IOException, XPathFactoryConfigurationException, SAXException, ParserConfigurationException, XPathExpressionException {
        copy(srcFilename, dstFilename, false);
    }

    private static void copy(String srcFilename, String dstFilename, boolean override) throws IOException, XPathFactoryConfigurationException, SAXException, ParserConfigurationException, XPathExpressionException {
        File fileToCopy = new File(rootDir + "test-output/" + srcFilename);
        if (fileToCopy.exists()) {
            File newFile = new File(rootDir + "test-output/" + dstFilename);
            if (!newFile.exists() || override) {
                try {
                    FileChannel srcChannel = new FileInputStream(rootDir + "test-output/" + srcFilename).getChannel();
                    FileChannel dstChannel = new FileOutputStream(rootDir + "test-output/" + dstFilename).getChannel();
                    dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                    srcChannel.close();
                    dstChannel.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
