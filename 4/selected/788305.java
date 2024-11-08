package org.dita.dost.writer;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.junit.Assert.assertEquals;
import org.dita.dost.TestUtils;
import org.dita.dost.exception.DITAOTException;
import org.dita.dost.module.Content;
import org.dita.dost.module.ContentImpl;
import org.dita.dost.reader.ConrefPushReader;
import org.dita.dost.util.Constants;
import org.dita.dost.util.FileUtils;
import org.dita.dost.writer.ConrefPushParser;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TestConrefPushParser {

    private static final File resourceDir = new File("test-stub", TestConrefPushParser.class.getSimpleName());

    private static final File srcDir = new File(resourceDir, "src");

    private static File tempDir;

    private static File inputFile;

    @BeforeClass
    public static void setUp() throws IOException {
        tempDir = TestUtils.createTempDir(TestConrefPushParser.class);
        inputFile = new File(tempDir, "conrefpush_stub.xml");
        FileUtils.copyFile(new File(srcDir, "conrefpush_stub.xml"), inputFile);
        FileUtils.copyFile(new File(srcDir, "conrefpush_stub2.xml"), new File(tempDir, "conrefpush_stub2.xml"));
    }

    @Test
    public void testWrite() throws DITAOTException, ParserConfigurationException, SAXException, IOException {
        final ConrefPushParser parser = new ConrefPushParser();
        final ConrefPushReader reader = new ConrefPushReader();
        reader.read(inputFile.getAbsolutePath());
        final Set<Map.Entry<String, Hashtable<String, String>>> pushSet = (Set<Map.Entry<String, Hashtable<String, String>>>) reader.getContent().getCollection();
        final Iterator<Map.Entry<String, Hashtable<String, String>>> iter = pushSet.iterator();
        if (iter.hasNext()) {
            final Map.Entry<String, Hashtable<String, String>> entry = iter.next();
            FileUtils.copyFile(new File(srcDir, "conrefpush_stub2_backup.xml"), new File(entry.getKey()));
            final Content content = new ContentImpl();
            content.setValue(entry.getValue());
            parser.setContent(content);
            parser.write(entry.getKey());
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document document = builder.parse(new File(entry.getKey()));
            final Element elem = document.getDocumentElement();
            NodeList nodeList = elem.getChildNodes();
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < nodeList.getLength(); j++) {
                    if (nodeList.item(j).getNodeType() == Node.ELEMENT_NODE) {
                        nodeList = nodeList.item(j).getChildNodes();
                        break;
                    }
                }
            }
            Element element;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    element = (Element) node;
                    if (element.getAttributes().getNamedItem("id") != null && element.getAttributes().getNamedItem("id").getNodeValue().equals("A")) {
                        node = element.getPreviousSibling();
                        while (node.getNodeType() != Node.ELEMENT_NODE) {
                            node = node.getPreviousSibling();
                        }
                        assertEquals("<li class=\"- topic/li task/step \"><ph class=\"- topic/ph task/cmd \">before</ph></li>", nodeToString((Element) node));
                    } else if (element.getAttributes().getNamedItem("id") != null && element.getAttributes().getNamedItem("id").getNodeValue().equals("B")) {
                        node = element.getNextSibling();
                        while (node.getNodeType() != Node.ELEMENT_NODE) {
                            node = node.getNextSibling();
                        }
                        assertEquals("<li class=\"- topic/li task/step \"><ph class=\"- topic/ph task/cmd \">after</ph></li>", nodeToString((Element) node));
                        node = node.getNextSibling();
                        while (node.getNodeType() != Node.ELEMENT_NODE) {
                            node = node.getNextSibling();
                        }
                        assertEquals("<li class=\"- topic/li task/step \" id=\"C\"><ph class=\"- topic/ph task/cmd \">replace</ph></li>", nodeToString((Element) node));
                    }
                }
            }
        }
    }

    private String nodeToString(final Element elem) {
        final StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Constants.LESS_THAN).append(elem.getNodeName());
        final NamedNodeMap namedNodeMap = elem.getAttributes();
        for (int i = 0; i < namedNodeMap.getLength(); i++) {
            stringBuffer.append(Constants.STRING_BLANK).append(namedNodeMap.item(i).getNodeName()).append(Constants.EQUAL).append(Constants.QUOTATION + namedNodeMap.item(i).getNodeValue() + Constants.QUOTATION);
        }
        stringBuffer.append(Constants.GREATER_THAN);
        final NodeList nodeList = elem.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                stringBuffer.append(nodeToString((Element) node));
            }
            if (node.getNodeType() == Node.TEXT_NODE) {
                stringBuffer.append(node.getNodeValue());
            }
        }
        stringBuffer.append("</").append(elem.getNodeName()).append(Constants.GREATER_THAN);
        return stringBuffer.toString();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        TestUtils.forceDelete(tempDir);
    }
}
