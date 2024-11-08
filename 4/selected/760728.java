package com.goodcodeisbeautiful.opensearch.osrss10;

import java.io.Reader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import com.goodcodeisbeautiful.test.util.CommonTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author hata
 *
 */
public class DefaultOpenSearchRssTestCase extends CommonTestCase {

    private static final String PRE_ENCODE_TEXT = "<&>\"'";

    public static Test suite() {
        return new TestSuite(DefaultOpenSearchRssTestCase.class);
    }

    private DefaultOpenSearchRss m_osRss;

    private DefaultOpenSearchRss m_osRssWithChannel;

    private DefaultOpenSearchRssChannel m_channel;

    private DefaultOpenSearchRss m_osRssEncodeTest;

    private DefaultOpenSearchRss m_minimumOsRss;

    protected List getSetupFilenames() {
        List l = new LinkedList();
        l.add("sample-osrss10.xml");
        l.add("sample-osrss10-2.xml");
        l.add("sample-osrss10-3.xml");
        return l;
    }

    protected void setUp() throws Exception {
        super.setUp();
        m_osRss = new DefaultOpenSearchRss();
        m_channel = new DefaultOpenSearchRssChannel();
        m_channel.setTitle("TITLE");
        m_channel.setLink(new URL("http://localhost:80/"));
        m_channel.setCopyright("COPYRIGHT");
        m_channel.setDescription("DESC");
        m_channel.setItemsPerPage(10);
        m_channel.setLanguage(new Locale("ja", "JP"));
        m_channel.setStartIndex(11);
        m_channel.setTotalResult(20);
        m_channel.addItem(new DefaultOpenSearchRssItem("TITLE1", new URL("http://localhost:81/"), "DESC1"));
        m_channel.addItem(new DefaultOpenSearchRssItem("TITLE2", new URL("http://localhost:82/"), "DESC2"));
        m_osRssWithChannel = new DefaultOpenSearchRss(m_channel);
        DefaultOpenSearchRssChannel channel = new DefaultOpenSearchRssChannel();
        channel = new DefaultOpenSearchRssChannel();
        channel.setTitle("TITLE" + PRE_ENCODE_TEXT);
        channel.setLink(new URL("http://localhost:80/?" + PRE_ENCODE_TEXT));
        channel.setCopyright("COPYRIGHT" + PRE_ENCODE_TEXT);
        channel.setDescription("DESC" + PRE_ENCODE_TEXT);
        channel.setItemsPerPage(10);
        channel.setLanguage(new Locale("ja" + PRE_ENCODE_TEXT, "JP" + PRE_ENCODE_TEXT));
        channel.setStartIndex(11);
        channel.setTotalResult(20);
        channel.addItem(new DefaultOpenSearchRssItem("TITLE1" + PRE_ENCODE_TEXT, new URL("http://localhost:81/?" + PRE_ENCODE_TEXT), "DESC1" + PRE_ENCODE_TEXT));
        channel.addItem(new DefaultOpenSearchRssItem("TITLE2" + PRE_ENCODE_TEXT, new URL("http://localhost:82/?" + PRE_ENCODE_TEXT), "DESC2" + PRE_ENCODE_TEXT));
        m_osRssEncodeTest = new DefaultOpenSearchRss(channel);
        channel = new DefaultOpenSearchRssChannel();
        channel = new DefaultOpenSearchRssChannel();
        channel.setTitle("TITLE" + PRE_ENCODE_TEXT);
        channel.setLink(new URL("http://localhost:80/?" + PRE_ENCODE_TEXT));
        channel.setDescription("DESC" + PRE_ENCODE_TEXT);
        channel.setItemsPerPage(10);
        channel.setStartIndex(11);
        channel.setTotalResult(20);
        channel.addItem(new DefaultOpenSearchRssItem("TITLE1" + PRE_ENCODE_TEXT, new URL("http://localhost:81/?" + PRE_ENCODE_TEXT), "DESC1" + PRE_ENCODE_TEXT));
        channel.addItem(new DefaultOpenSearchRssItem("TITLE2" + PRE_ENCODE_TEXT, new URL("http://localhost:82/?" + PRE_ENCODE_TEXT), "DESC2" + PRE_ENCODE_TEXT));
        m_minimumOsRss = new DefaultOpenSearchRss(channel);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        m_osRssWithChannel = null;
        m_osRss = null;
    }

    public void testDefaultOpenSearchRss() {
        assertNotNull(m_osRss);
        assertNotNull(m_osRssWithChannel);
    }

    public void testDefaultOpenSearchRssWithReader() throws Exception {
        DefaultOpenSearchRss osrss10 = new DefaultOpenSearchRss(getContents("sample-osrss10.xml").getBytes());
        OpenSearchRssChannel channel = osrss10.getChannel();
        assertNull(channel.getCopyright());
        assertEquals("Search results for License at Archtea", channel.getDescription());
        assertEquals(3, channel.getItemsPerPage());
        assertNull(channel.getLanguage());
        assertEquals("http://localhost:12345/archtea/search?q=License", channel.getLink().toExternalForm());
        assertEquals(1, channel.getStartIndex());
        assertEquals("Archtea - License", channel.getTitle());
        assertEquals(123, channel.getTotalResult());
        List items = channel.items();
        assertEquals(3, items.size());
        for (int i = 0; i < items.size(); i++) {
            OpenSearchRssItem item = (OpenSearchRssItem) items.get(i);
            assertEquals("desc" + (i + 1), item.getDescription());
            assertEquals("http://localhost:12345/link" + (i + 1), item.getLink().toExternalForm());
            assertEquals("title" + (i + 1), item.getTitle());
        }
        osrss10 = new DefaultOpenSearchRss(getContents("sample-osrss10-2.xml").getBytes());
        channel = osrss10.getChannel();
        assertEquals("unknown", channel.getCopyright());
        assertEquals("Search results for License at Archtea", channel.getDescription());
        assertEquals(3, channel.getItemsPerPage());
        assertEquals(new Locale("en", "US"), channel.getLanguage());
        assertEquals("http://localhost:12345/archtea/search?q=License", channel.getLink().toExternalForm());
        assertEquals(1, channel.getStartIndex());
        assertEquals("Archtea - License", channel.getTitle());
        assertEquals(123, channel.getTotalResult());
        items = channel.items();
        assertEquals(3, items.size());
        for (int i = 0; i < items.size(); i++) {
            OpenSearchRssItem item = (OpenSearchRssItem) items.get(i);
            assertEquals("desc" + (i + 1), item.getDescription());
            assertEquals("http://localhost:12345/link" + (i + 1), item.getLink().toExternalForm());
            assertEquals("title" + (i + 1), item.getTitle());
        }
    }

    public void testGetChannel() {
        assertEquals(m_channel, m_osRssWithChannel.getChannel());
    }

    public void testSetChannel() {
        assertNotSame(m_channel, m_osRss.getChannel());
        m_osRss.setChannel(m_channel);
        assertEquals(m_channel, m_osRss.getChannel());
    }

    public void testGetXmlns() {
        assertEquals("http://a9.com/-/spec/opensearchrss/1.0/", m_osRss.getXmlns());
    }

    public void testGetReader() throws Exception {
        Reader r = m_osRssWithChannel.getReader();
        char[] buff = new char[1024];
        int len = r.read(buff);
        while (len != -1) {
            len = r.read(buff);
        }
    }

    public void testCheckTags() throws Exception {
        Reader r = m_osRssWithChannel.getReader();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(true);
        if (!setFactoryAttributes(factory)) {
            return;
        }
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new TestHandler());
        Document document = builder.parse(new InputSource(r));
        Element docElem = document.getDocumentElement();
        assertEquals("Check rss tag.", "rss", docElem.getTagName());
        assertEquals("Check rss version.", "2.0", docElem.getAttribute("version"));
        assertEquals("Check opensearch extension.", "http://a9.com/-/spec/opensearchrss/1.0/", docElem.getAttribute("xmlns:openSearch"));
        NodeList nodeList = docElem.getChildNodes();
        Element channelElem = null;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if ("channel".equals(node.getNodeName())) {
                assertTrue(node instanceof Element);
                channelElem = (Element) node;
                break;
            }
        }
        assertNotNull(channelElem);
        checkTagText(channelElem, "title", "TITLE");
        checkTagText(channelElem, "link", "http://localhost:80/");
        checkTagText(channelElem, "description", "DESC");
        checkTagText(channelElem, "language", "ja-jp");
        checkTagText(channelElem, "openSearch:totalResults", "20");
        checkTagText(channelElem, "openSearch:startIndex", "11");
        checkTagText(channelElem, "openSearch:itemsPerPage", "10");
        nodeList = channelElem.getChildNodes();
        int itemCount = 0;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if ("item".equals(node.getNodeName())) {
                itemCount++;
                checkTagText((Element) node, "title", "TITLE" + itemCount);
                checkTagText((Element) node, "link", "http://localhost:8" + itemCount + "/");
                checkTagText((Element) node, "description", "DESC" + itemCount);
            }
        }
        assertEquals("Check testItemCount ", 2, itemCount);
    }

    public void testEncodeTest() throws Exception {
        Reader r = m_osRssEncodeTest.getReader();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(true);
        if (!setFactoryAttributes(factory)) {
            return;
        }
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new TestHandler());
        Document document = builder.parse(new InputSource(r));
        Element docElem = document.getDocumentElement();
        assertEquals("Check rss tag.", "rss", docElem.getTagName());
        assertEquals("Check rss version.", "2.0", docElem.getAttribute("version"));
        assertEquals("Check opensearch extension.", "http://a9.com/-/spec/opensearchrss/1.0/", docElem.getAttribute("xmlns:openSearch"));
        NodeList nodeList = docElem.getChildNodes();
        Element channelElem = null;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if ("channel".equals(node.getNodeName())) {
                assertTrue(node instanceof Element);
                channelElem = (Element) node;
                break;
            }
        }
        assertNotNull(channelElem);
        checkTagText(channelElem, "title", "TITLE" + PRE_ENCODE_TEXT);
        checkTagText(channelElem, "link", "http://localhost:80/?" + PRE_ENCODE_TEXT);
        checkTagText(channelElem, "description", "DESC" + PRE_ENCODE_TEXT);
        checkTagText(channelElem, "language", "ja" + PRE_ENCODE_TEXT + "-jp" + PRE_ENCODE_TEXT);
        checkTagText(channelElem, "openSearch:totalResults", "20");
        checkTagText(channelElem, "openSearch:startIndex", "11");
        checkTagText(channelElem, "openSearch:itemsPerPage", "10");
        nodeList = channelElem.getChildNodes();
        int itemCount = 0;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if ("item".equals(node.getNodeName())) {
                itemCount++;
                checkTagText((Element) node, "title", "TITLE" + itemCount + PRE_ENCODE_TEXT);
                checkTagText((Element) node, "link", "http://localhost:8" + itemCount + "/?" + PRE_ENCODE_TEXT);
                checkTagText((Element) node, "description", "DESC" + itemCount + PRE_ENCODE_TEXT);
            }
        }
        assertEquals("Check testItemCount ", 2, itemCount);
    }

    public void testCheckEncodedTextDirectly() throws Exception {
        Reader r = m_osRssEncodeTest.getReader();
        StringBuffer buff = new StringBuffer();
        char[] charTmp = new char[8192];
        int len = r.read(charTmp);
        while (len != -1) {
            buff.append(new String(charTmp, 0, len));
            len = r.read(charTmp);
        }
        String readText = new String(buff);
        Pattern p = Pattern.compile("<title>([^/]*)</title>");
        Matcher m = p.matcher(readText);
        assertTrue(m.find());
        assertEquals("TITLE&lt;&amp;&gt;&quot;&apos;", m.group(1));
        assertTrue(m.find());
        assertEquals("TITLE1&lt;&amp;&gt;&quot;&apos;", m.group(1));
        assertTrue(m.find());
        assertEquals("TITLE2&lt;&amp;&gt;&quot;&apos;", m.group(1));
        assertFalse(m.find());
        p = Pattern.compile("<link>([^<]*)</link>");
        m = p.matcher(readText);
        assertTrue(m.find());
        assertEquals("http://localhost:80/?&lt;&amp;&gt;&quot;&apos;", m.group(1));
        assertTrue(m.find());
        assertEquals("http://localhost:81/?&lt;&amp;&gt;&quot;&apos;", m.group(1));
        assertTrue(m.find());
        assertEquals("http://localhost:82/?&lt;&amp;&gt;&quot;&apos;", m.group(1));
        assertFalse(m.find());
        p = Pattern.compile("<description>([^<]*)</description>");
        m = p.matcher(readText);
        assertTrue(m.find());
        assertEquals("DESC&lt;&amp;&gt;&quot;&apos;", m.group(1));
        assertTrue(m.find());
        assertEquals("DESC1&lt;&amp;&gt;&quot;&apos;", m.group(1));
        assertTrue(m.find());
        assertEquals("DESC2&lt;&amp;&gt;&quot;&apos;", m.group(1));
        assertFalse(m.find());
        p = Pattern.compile("<copyright>([^<]*)</copyright>");
        m = p.matcher(readText);
        assertTrue(m.find());
        assertEquals("COPYRIGHT&lt;&amp;&gt;&quot;&apos;", m.group(1));
        assertFalse(m.find());
        p = Pattern.compile("<language>([^<]*)</language>");
        m = p.matcher(readText);
        assertTrue(m.find());
        assertEquals("ja&lt;&amp;&gt;&quot;&apos;-jp&lt;&amp;&gt;&quot;&apos;", m.group(1));
        assertFalse(m.find());
    }

    public void testMinimumValue() throws Exception {
        Reader r = m_minimumOsRss.getReader();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(true);
        if (!setFactoryAttributes(factory)) {
            return;
        }
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new TestHandler());
        Document document = builder.parse(new InputSource(r));
        Element docElem = document.getDocumentElement();
        assertEquals("Check rss tag.", "rss", docElem.getTagName());
        assertEquals("Check rss version.", "2.0", docElem.getAttribute("version"));
        assertEquals("Check opensearch extension.", "http://a9.com/-/spec/opensearchrss/1.0/", docElem.getAttribute("xmlns:openSearch"));
        NodeList nodeList = docElem.getChildNodes();
        Element channelElem = null;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if ("channel".equals(node.getNodeName())) {
                assertTrue(node instanceof Element);
                channelElem = (Element) node;
                break;
            }
        }
        assertNotNull(channelElem);
        checkTagText(channelElem, "title", "TITLE" + PRE_ENCODE_TEXT);
        checkTagText(channelElem, "link", "http://localhost:80/?" + PRE_ENCODE_TEXT);
        checkTagText(channelElem, "description", "DESC" + PRE_ENCODE_TEXT);
        checkTagText(channelElem, "copyright", null);
        checkTagText(channelElem, "language", null);
        checkTagText(channelElem, "openSearch:totalResults", "20");
        checkTagText(channelElem, "openSearch:startIndex", "11");
        checkTagText(channelElem, "openSearch:itemsPerPage", "10");
        nodeList = channelElem.getChildNodes();
        int itemCount = 0;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if ("item".equals(node.getNodeName())) {
                itemCount++;
                checkTagText((Element) node, "title", "TITLE" + itemCount + PRE_ENCODE_TEXT);
                checkTagText((Element) node, "link", "http://localhost:8" + itemCount + "/?" + PRE_ENCODE_TEXT);
                checkTagText((Element) node, "description", "DESC" + itemCount + PRE_ENCODE_TEXT);
            }
        }
        assertEquals("Check testItemCount ", 2, itemCount);
    }

    public void testDefaultOpenSearchRssReaderNoValueInOSSTag() throws Exception {
        DefaultOpenSearchRss osrss10 = new DefaultOpenSearchRss(getContents("sample-osrss10-3.xml").getBytes());
        assertEquals("Check default items per page", 10, osrss10.getChannel().getItemsPerPage());
        assertEquals("Check default total count.", 3, osrss10.getChannel().getTotalResult());
        assertEquals("Check start index if the value is not set.", 1, osrss10.getChannel().getStartIndex());
    }

    public void testXmlCtrlCodes() throws Exception {
        String s = getContents("sample-osrss10.xml");
        s = s.replaceFirst("title1", "\r\n\t" + (char) 0x1b + (char) 0x08);
        assertTrue(s.indexOf((char) 0x1b) != -1);
        s = s.replaceFirst("desc1", "\r\n\t" + (char) 0x1b + (char) 0x08);
        assertTrue(s.indexOf((char) 0x1b) != -1);
        new DefaultOpenSearchRss(s.getBytes());
    }

    public static void checkTagText(Element parent, String tagName, String checkText) {
        Node node = parent.getFirstChild();
        boolean checked = false;
        while (node != null) {
            if (node.getNodeName().equals(tagName)) {
                assertFalse(checked);
                assertEquals("Check for " + tagName, checkText, node.getFirstChild().getNodeValue());
                checked = true;
            }
            node = node.getNextSibling();
        }
    }

    public class TestHandler implements ErrorHandler {

        public void warning(SAXParseException exception) throws SAXException {
            System.out.println(exception.getMessage());
            fail("Warning reported" + exception);
        }

        public void error(SAXParseException exception) throws SAXException {
            System.out.println(exception.getMessage());
            fail("Error reported" + exception);
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            System.out.println(exception.getMessage());
            fail("FatalError reported" + exception);
        }
    }

    public static boolean setFactoryAttributes(DocumentBuilderFactory factory) {
        try {
            factory.setAttribute("http://xml.org/sax/features/validation", Boolean.TRUE);
            factory.setAttribute("http://apache.org/xml/features/validation/dynamic", Boolean.TRUE);
            factory.setAttribute("http://xml.org/sax/features/namespaces", Boolean.TRUE);
            factory.setAttribute("http://apache.org/xml/features/validation/schema", Boolean.TRUE);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }
}
