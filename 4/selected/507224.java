package com.spring.rssReader.util;

import com.roha.xmlparsing.GeneralSAXHandler;
import com.roha.xmlparsing.XMLElement;
import com.roha.xmlparsing.XMLElementIterator;
import junit.framework.TestCase;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * User: Ronald
 * Date: 20-mei-2004
 * Time: 13:14:55
 */
public class TestSAXParsing extends TestCase {

    public void testParsing() throws IOException, SAXException {
        String xml = "<root><status>ok</status></root>";
        GeneralSAXHandler handler = new GeneralSAXHandler();
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

    public void testParsingWithAttributes() throws IOException, SAXException {
        String xml = "<root><status veld=\"veldValue\">ok</status></root>";
        GeneralSAXHandler handler = new GeneralSAXHandler();
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

    public void testIterator() throws IOException, SAXException {
        String xml = "<root><status veld=\"veldValue\">ok</status><tabs>" + "<tab id=\"getArticles\">Articles</tab>" + "<tab id=\"getFavourites\">Favourites</tab>" + "<tab id=\"getChannelsWithNews\">Channels with news</tab>" + "<tabdusniet>dusniet</tabdusniet>" + "</tabs>" + "<tab>Losse tab</tab>" + "</root>";
        GeneralSAXHandler handler = new GeneralSAXHandler();
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

    public void testIteratorWithAttributes() throws IOException, SAXException {
        String xml = "<root><status veld=\"veldValue\">ok</status><tabs>" + "<tab id=\"getArticles\" rating=\"1\">Articles</tab>" + "<tab id=\"getFavourites\">Favourites</tab>" + "<tab id=\"getChannelsWithNews\">Channels with news</tab>" + "<tabdusniet>dusniet</tabdusniet>" + "</tabs>" + "<tab rating=\"1\">Losse tab</tab>" + "</root>";
        GeneralSAXHandler handler = new GeneralSAXHandler();
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

    public void testZoekElementen() throws IOException, SAXException {
        String xml = "<root>" + "<boeken>" + "<boek><isbn type=\"literatuur\">12</isbn><schrijver>Mulisch</schrijver><uitgever>de Bij</uitgever></boek>" + "<boek><isbn type=\"lectuur\">13</isbn><schrijver>van Kooten</schrijver><uitgever>de Bij</uitgever></boek>" + "</boeken>" + "</root>";
        GeneralSAXHandler handler = new GeneralSAXHandler();
        XMLElement root = handler.parse(new StringReader(xml));
        XMLElementIterator it = new XMLElementIterator(root, "boek");
        XMLElement xmlElement = (XMLElement) it.next();
        assertTrue(xmlElement.getValue("isbn").equals("12"));
        xmlElement = (XMLElement) it.next();
        assertTrue(xmlElement.getValue("isbn").equals("13"));
        it = new XMLElementIterator(root, "boek:type=lectuur");
        xmlElement = (XMLElement) it.next();
        assertTrue(xmlElement.getValue("isbn").equals("13"));
    }
}
