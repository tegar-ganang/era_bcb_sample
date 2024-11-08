package com.spring.rssReader.util;

import com.roha.xmlparsing.XMLElement;
import com.roha.xmlparsing.XMLElementIterator;
import com.roha.xmlparsing.XMLHandler;
import junit.framework.TestCase;
import java.io.StringReader;
import java.util.List;

/**
 * User: ronald
 * Date: May 13, 2004
 * Time: 11:58:36 AM
 */
public abstract class TestXmlParsing extends TestCase {

    public abstract XMLHandler getXmlHandler();

    public void testParsing() throws Exception {
        String xml = "<root><status>ok</status></root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root = handler.parse(new StringReader(xml));
        assertTrue(root.getKey().equals("root"));
        assertTrue(root.getFullName().equals("/root"));
        assertTrue(root.getValue() instanceof List);
        List list = (List) root.getValue();
        assertTrue(list.size() == 1);
        XMLElement status = (XMLElement) list.get(0);
        assertTrue(status.getKey().equals("status"));
        assertTrue(status.getValue().equals("ok"));
        assertTrue(status.getFullName().equals("/root/status"));
    }

    public void testParsingWithAttributes() throws Exception {
        String xml = "<root><status veld=\"veldValue\">ok</status></root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root = handler.parse(new StringReader(xml));
        assertTrue(root.getKey().equals("root"));
        assertTrue(root.getFullName().equals("/root"));
        assertTrue(root.getValue() instanceof List);
        List list = (List) root.getValue();
        assertTrue(list.size() == 1);
        XMLElement status = (XMLElement) list.get(0);
        assertTrue(status.getKey().equals("status"));
        assertTrue(status.getValue().equals("ok"));
        assertTrue(status.getAttribute("veld").equals("veldValue"));
        assertTrue(status.getValue("veld").equals("veldValue"));
        assertTrue(status.getFullName().equals("/root/status"));
    }

    public void testIterator() throws Exception {
        String xml = "<root><status veld=\"veldValue\">ok</status><tabs>" + "<tab id=\"getArticles\">Articles</tab>" + "<tab id=\"getFavourites\">Favourites</tab>" + "<tab id=\"getChannelsWithNews\">Channels with news</tab>" + "<tabdusniet>dusniet</tabdusniet>" + "</tabs>" + "<tab>Losse tab</tab>" + "</root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root = handler.parse(new StringReader(xml));
        XMLElementIterator it = new XMLElementIterator(root, "status");
        while (it.hasNext()) {
            XMLElement xmlElement = (XMLElement) it.next();
            assertTrue(xmlElement.getValue().equals("ok"));
            assertTrue(xmlElement.getValue("veld").equals("veldValue"));
        }
        it = new XMLElementIterator(root, "tab");
        StringBuffer sb = new StringBuffer();
        while (it.hasNext()) {
            XMLElement xmlElement = (XMLElement) it.next();
            sb.append(xmlElement.getValue()).append(",");
        }
        assertTrue(sb.toString().equals("Articles,Favourites,Channels with news,Losse tab,"));
        it = new XMLElementIterator(root, "tabs/tab");
        sb = new StringBuffer();
        while (it.hasNext()) {
            XMLElement xmlElement = (XMLElement) it.next();
            sb.append(xmlElement.getValue()).append(",");
        }
        assertTrue(sb.toString().equals("Articles,Favourites,Channels with news,"));
        it = new XMLElementIterator(root, "/tab");
        assertFalse(it.hasNext());
    }

    public void testIteratorWithAttributes() throws Exception {
        String xml = "<root><status veld=\"veldValue\">ok</status><tabs>" + "<tab id=\"getArticles\" rating=\"1\">Articles</tab>" + "<tab id=\"getFavourites\">Favourites</tab>" + "<tab id=\"getChannelsWithNews\">Channels with news</tab>" + "<tabdusniet>dusniet</tabdusniet>" + "</tabs>" + "<tab rating=\"1\">Losse tab</tab>" + "</root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root = handler.parse(new StringReader(xml));
        XMLElementIterator it = new XMLElementIterator(root, "tab:id=getArticles");
        while (it.hasNext()) {
            XMLElement xmlElement = (XMLElement) it.next();
            assertTrue(xmlElement.getValue().equals("Articles"));
        }
        it = new XMLElementIterator(root, "tab:id=fietsen");
        assertFalse(it.hasNext());
        it = new XMLElementIterator(root, "tab:rating=1");
        StringBuffer sb = new StringBuffer();
        while (it.hasNext()) {
            XMLElement xmlElement = (XMLElement) it.next();
            sb.append(xmlElement.getFullName()).append(" value = ").append(xmlElement.getValue()).append(",");
        }
        assertTrue(sb.toString().equals("/root/tabs/tab value = Articles,/root/tab value = Losse tab,"));
    }

    public void testZoekElementen() throws Exception {
        String xml = "<root>" + "<books>" + "<book><isbn type=\"literature\">12</isbn><writer>Mulisch</writer><publisher>de Bij</publisher></book>" + "<book><isbn type=\"lecture\">13</isbn><writer>van Kooten</writer><publisher>de Bij</publisher></book>" + "</books>" + "</root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root = handler.parse(new StringReader(xml));
        XMLElementIterator it = new XMLElementIterator(root, "book");
        XMLElement xmlElement = (XMLElement) it.next();
        assertTrue(xmlElement.getValue("isbn").equals("12"));
        xmlElement = (XMLElement) it.next();
        assertTrue(xmlElement.getValue("isbn").equals("13"));
        it = new XMLElementIterator(root, "book:type=lecture");
        xmlElement = (XMLElement) it.next();
        assertTrue(xmlElement.getValue("isbn").equals("13"));
        assertFalse(it.hasNext());
        it = new XMLElementIterator(root, "book:type=literature");
        xmlElement = (XMLElement) it.next();
        assertTrue(xmlElement.getFullName().equals("/root/books/book"));
        assertTrue(xmlElement.getValue("writer").equals("Mulisch"));
        assertFalse(it.hasNext());
    }

    public void testPrint() throws Exception {
        String xml = "<root>" + "<books>" + "<book><isbn type=\"literature\">12</isbn><writer>Mulisch</writer><publisher>de Bij</publisher></book>" + "<book><isbn type=\"lecture\">13</isbn><writer>van Kooten</writer><publisher>de Bij</publisher></book>" + "<book type=\"lecture\"></book>" + "</books>" + "</root>";
        String expectedResult = "<root>\n" + "   <books>\n" + "      <book>\n" + "         <isbn type=\"literature\">12\n" + "         </isbn>\n" + "         <writer>Mulisch\n" + "         </writer>\n" + "         <publisher>de Bij\n" + "         </publisher>\n" + "      </book>\n" + "      <book>\n" + "         <isbn type=\"lecture\">13\n" + "         </isbn>\n" + "         <writer>van Kooten\n" + "         </writer>\n" + "         <publisher>de Bij\n" + "         </publisher>\n" + "      </book>\n" + "      <book type=\"lecture\"/>\n" + "   </books>\n" + "</root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root = handler.parse(new StringReader(xml));
        String result = root.toString();
        assertTrue(result.equals(expectedResult));
    }

    public void testPrintMetSubs() throws Exception {
        String xml = "<root><books><book></book></books></root>";
        String result = "<root>\n" + "   <books>\n" + "      <book/>\n" + "   </books>\n" + "</root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root = handler.parse(new StringReader(xml));
        assertTrue(result.equals(root.toString()));
    }

    public void testMergeOneNonExistingElement() throws Exception {
        String xml1 = "<root></root>";
        String xml2 = "<root><books/></root>";
        String result = "<root>\n   <books/>\n</root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root1 = handler.parse(new StringReader(xml1));
        XMLElement root2 = handler.parse(new StringReader(xml2));
        root1.merge(root2);
        assertTrue(root1.toString().equals(result));
        assertFalse(root2.toString().equals(result));
        root1 = handler.parse(new StringReader(xml2));
        root2 = handler.parse(new StringReader(xml1));
        root1.merge(root2);
        assertTrue(root1.toString().equals(result));
        assertFalse(root2.toString().equals(result));
    }

    public void testMergeTwoDifferentNodesSameLevel() throws Exception {
        String xml1 = "<root><pencils/></root>";
        String xml2 = "<root><books/></root>";
        String resultMergeXml1With2 = "<root>\n   <pencils/>\n   <books/>\n</root>";
        String resultMergeXml2With1 = "<root>\n   <books/>\n   <pencils/>\n</root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root1 = handler.parse(new StringReader(xml1));
        XMLElement root2 = handler.parse(new StringReader(xml2));
        root1.merge(root2);
        assertTrue(root1.toString().equals(resultMergeXml1With2));
        assertFalse(root2.toString().equals(resultMergeXml1With2));
        root1 = handler.parse(new StringReader(xml2));
        root2 = handler.parse(new StringReader(xml1));
        root1.merge(root2);
        assertTrue(root1.toString().equals(resultMergeXml2With1));
        assertFalse(root2.toString().equals(resultMergeXml2With1));
    }

    public void testMergeTwoDifferentTreesWithDifferentNamesOnAllLevels() throws Exception {
        String xml1 = "<root>" + "   <pencil/>" + "</root>";
        String xml2 = "<root>" + "   <books>" + "      <book/>" + "   </books>" + "</root>";
        String resultMergeXml1WithXml2 = "<root>\n" + "   <pencil/>\n" + "   <books>\n" + "      <book/>\n" + "   </books>\n" + "</root>";
        String resultMergeXml2WithXml1 = "<root>\n" + "   <books>\n" + "      <book/>\n" + "   </books>\n" + "   <pencil/>\n" + "</root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root1 = handler.parse(new StringReader(xml1));
        XMLElement root2 = handler.parse(new StringReader(xml2));
        root1.merge(root2);
        assertTrue(root1.toString().equals(resultMergeXml1WithXml2));
        assertFalse(root2.toString().equals(resultMergeXml1WithXml2));
        root1 = handler.parse(new StringReader(xml2));
        root2 = handler.parse(new StringReader(xml1));
        root1.merge(root2);
        assertTrue(root1.toString().equals(resultMergeXml2WithXml1));
        assertFalse(root2.toString().equals(resultMergeXml2WithXml1));
    }

    public void testMergeTwoDifferentTreesWithSomeSameNamesOnDifferentLevels() throws Exception {
        String xml1 = "<root>" + "   <book/>" + "</root>";
        String xml2 = "<root>" + "   <books>" + "      <book/>" + "   </books>" + "</root>";
        String resultMergeXml1WithXml2 = "<root>\n" + "   <book/>\n" + "   <books>\n" + "      <book/>\n" + "   </books>\n" + "</root>";
        String resultMergeXml2WithXml1 = "<root>\n" + "   <books>\n" + "      <book/>\n" + "   </books>\n" + "   <book/>\n" + "</root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root1 = handler.parse(new StringReader(xml1));
        XMLElement root2 = handler.parse(new StringReader(xml2));
        root1.merge(root2);
        assertTrue(root1.toString().equals(resultMergeXml1WithXml2));
        assertFalse(root2.toString().equals(resultMergeXml1WithXml2));
        root1 = handler.parse(new StringReader(xml2));
        root2 = handler.parse(new StringReader(xml1));
        root1.merge(root2);
        assertTrue(root1.toString().equals(resultMergeXml2WithXml1));
        assertFalse(root2.toString().equals(resultMergeXml2WithXml1));
    }

    public void testMergeTwoSameNodesWithOneExtraSubNode() throws Exception {
        String xml1 = "<root>" + "   <books/>" + "</root>";
        String xml2 = "<root>" + "   <books>" + "      <book/>" + "   </books>" + "</root>";
        String result = "<root>\n" + "   <books>\n" + "      <book/>\n" + "   </books>\n" + "</root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root1 = handler.parse(new StringReader(xml1));
        XMLElement root2 = handler.parse(new StringReader(xml2));
        root1.merge(root2);
        assertTrue(root1.toString().equals(result));
        assertFalse(root2.toString().equals(result));
        root1 = handler.parse(new StringReader(xml2));
        root2 = handler.parse(new StringReader(xml1));
        root1.merge(root2);
        assertTrue(root1.toString().equals(result));
        assertFalse(root2.toString().equals(result));
    }

    public void testMergeWithData() throws Exception {
        String xml1 = "<root>" + "   <books>Verzameling boeken</books>" + "</root>";
        String xml2 = "<root>" + "</root>";
        String result = "<root>\n" + "   <books>Verzameling boeken\n" + "   </books>\n" + "</root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root1 = handler.parse(new StringReader(xml1));
        XMLElement root2 = handler.parse(new StringReader(xml2));
        root1.merge(root2);
        assertTrue(root1.toString().equals(result));
        assertFalse(root2.toString().equals(result));
        root1 = handler.parse(new StringReader(xml2));
        root2 = handler.parse(new StringReader(xml1));
        root1.merge(root2);
        assertTrue(root1.toString().equals(result));
        assertFalse(root2.toString().equals(result));
    }

    /**
	 * When merging 2 nodes on the same level with the same kind of string data, the master will always be updated by
	 * the journal value.
	 * @throws Exception
	 */
    public void testMergeWithDataInSameNode() throws Exception {
        String xml1 = "<root>" + "   <books>Verzameling boeken</books>" + "</root>";
        String xml2 = "<root>" + "   <books>Verzameling boeken over Java</books>" + "</root>";
        String resultMergeXml1WithXml2 = "<root>\n" + "   <books>Verzameling boeken over Java\n" + "   </books>\n" + "</root>";
        String resultMergeXml2WithXml1 = "<root>\n" + "   <books>Verzameling boeken\n" + "   </books>\n" + "</root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root1 = handler.parse(new StringReader(xml1));
        XMLElement root2 = handler.parse(new StringReader(xml2));
        root1.merge(root2);
        assertTrue(root1.toString().equals(resultMergeXml1WithXml2));
        assertFalse(root2.toString().equals(resultMergeXml1WithXml2));
        root1 = handler.parse(new StringReader(xml2));
        root2 = handler.parse(new StringReader(xml1));
        root1.merge(root2);
        assertTrue(root1.toString().equals(resultMergeXml2WithXml1));
        assertFalse(root2.toString().equals(resultMergeXml2WithXml1));
    }

    /**
	 * If one of the nodes contains data and the other node contains subnodes, then the data will always loose and be
	 * replaced with the nodes.
	 * @throws Exception
	 */
    public void testMergeDataWithSubNodes() throws Exception {
        String xml1 = "<root>" + "   <books>Verzameling boeken</books>" + "</root>";
        String xml2 = "<root>" + "   <books>" + "     <book/>" + "  </books>" + "</root>";
        String result = "<root>\n" + "   <books>\n" + "      <book/>\n" + "   </books>\n" + "</root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root1 = handler.parse(new StringReader(xml1));
        XMLElement root2 = handler.parse(new StringReader(xml2));
        root1.merge(root2);
        assertTrue(root1.toString().equals(result));
        assertFalse(root2.toString().equals(result));
        root1 = handler.parse(new StringReader(xml2));
        root2 = handler.parse(new StringReader(xml1));
        root1.merge(root2);
        assertTrue(root1.toString().equals(result));
        assertFalse(root2.toString().equals(result));
    }

    public void testMergeNodesWithAttributes() throws Exception {
        String xml1 = "<root>" + "   <books>" + "      <book id=\"book1\" writer=\"Mulish\" type=\"literature\"/>" + "      <book id=\"book2\" writer=\"van het Reve\" type=\"literature\"/>" + "      <book id=\"book3\" writer=\"Claus\" type=\"literature\"/>" + "   </books>" + "</root>";
        String xml2 = "<root>" + "   <books>" + "      <book id=\"book101\" writer=\"van Kooten\" type=\"lecture\"/>" + "  </books>" + "</root>";
        String resultMergeXml1WithXml2 = "<root>\n" + "   <books>\n" + "      <book id=\"book1\" writer=\"Mulish\" type=\"literature\"/>\n" + "      <book id=\"book2\" writer=\"van het Reve\" type=\"literature\"/>\n" + "      <book id=\"book3\" writer=\"Claus\" type=\"literature\"/>\n" + "      <book id=\"book101\" writer=\"van Kooten\" type=\"lecture\"/>\n" + "   </books>\n" + "</root>";
        String resultMergeXml2WithXml1 = "<root>\n" + "   <books>\n" + "      <book id=\"book101\" writer=\"van Kooten\" type=\"lecture\"/>\n" + "      <book id=\"book1\" writer=\"Mulish\" type=\"literature\"/>\n" + "      <book id=\"book2\" writer=\"van het Reve\" type=\"literature\"/>\n" + "      <book id=\"book3\" writer=\"Claus\" type=\"literature\"/>\n" + "   </books>\n" + "</root>";
        XMLHandler handler = getXmlHandler();
        XMLElement root1 = handler.parse(new StringReader(xml1));
        XMLElement root2 = handler.parse(new StringReader(xml2));
        root1.merge(root2);
        assertTrue(root1.toString().equals(resultMergeXml1WithXml2));
        assertFalse(root2.toString().equals(resultMergeXml1WithXml2));
        root1 = handler.parse(new StringReader(xml2));
        root2 = handler.parse(new StringReader(xml1));
        root1.merge(root2);
        assertTrue(root1.toString().equals(resultMergeXml2WithXml1));
        assertFalse(root2.toString().equals(resultMergeXml2WithXml1));
    }
}
