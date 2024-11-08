package net.sf.jannot.source.das;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import be.abeel.net.URIFactory;

public class DSN {

    class DSNParser extends DefaultHandler {

        private HashMap<String, List<String>> mastermap = new HashMap<String, List<String>>();

        private Stack<String> parserStack = new Stack<String>();

        private String source = null;

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);
            if (parserStack.peek().equalsIgnoreCase("MAPMASTER")) {
                String s = new String(ch, start, length);
                if (!mastermap.containsKey(s)) mastermap.put(s, new ArrayList<String>());
                mastermap.get(s).add(source);
            }
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            super.endElement(uri, localName, name);
            String stackName = parserStack.pop();
            if (!name.equals(stackName)) {
                throw new SAXException("Tags do not match: expected=" + stackName + "; actual=" + name);
            } else {
            }
        }

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, name, attributes);
            parserStack.push(name);
            if (name.equalsIgnoreCase("SOURCE")) {
                String val = attributes.getValue("id");
                source = val;
            }
        }
    }

    public List<String> getReferences() {
        return new ArrayList<String>(dsn.mastermap.keySet());
    }

    public List<String> getSources(String reference) {
        return dsn.mastermap.get(reference);
    }

    private DSNParser dsn;

    public DSN(String server) throws ParserConfigurationException, SAXException, MalformedURLException, IOException, URISyntaxException {
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        dsn = new DSNParser();
        parser.parse(URIFactory.url(server + "/das/dsn").openStream(), dsn);
    }
}
