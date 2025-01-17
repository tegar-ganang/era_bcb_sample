package net.ontopia.topicmaps.query.spi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributeListImpl;
import net.ontopia.topicmaps.core.TopicMapIF;
import net.ontopia.utils.OntopiaRuntimeException;
import net.ontopia.xml.SAXTracker;
import net.ontopia.xml.DefaultXMLReaderFactory;

/**
 * EXPERIMENTAL: HTTP searcher implementation.<p>
 */
public class HTTPSearcher extends AbstractSearcher {

    /**
   * PUBLIC: The mandatory default constructor.
   */
    public HTTPSearcher() {
    }

    public int getValueType() {
        return SearcherIF.STRING_VALUE;
    }

    public SearchResultIF getResult(String query) {
        return new HTTPSearchResult(query);
    }

    private class HTTPSearchResult extends AbstractSearchResult {

        Iterator hits;

        Hit hit;

        HTTPSearchResult(String query) {
            String url = (String) parameters.get("url");
            if (url.lastIndexOf("?") >= 0) url += "&query=" + URLEncoder.encode(query); else url += "?query=" + URLEncoder.encode(query);
            XMLReader parser;
            try {
                parser = new DefaultXMLReaderFactory().createXMLReader();
            } catch (SAXException e) {
                throw new OntopiaRuntimeException("Problems occurred when creating SAX2 XMLReader", e);
            }
            SearchHandler handler = new SearchHandler();
            parser.setContentHandler(handler);
            try {
                URLConnection conn = new URL(url).openConnection();
                parser.parse(new InputSource(conn.getInputStream()));
            } catch (SAXParseException e) {
                throw new OntopiaRuntimeException("XML parsing problem: " + e.toString() + " at: " + e.getSystemId() + ":" + e.getLineNumber() + ":" + e.getColumnNumber(), e);
            } catch (SAXException e) {
                if (e.getException() instanceof IOException) throw new OntopiaRuntimeException(e.getException());
                throw new OntopiaRuntimeException(e);
            } catch (Exception e) {
                throw new OntopiaRuntimeException(e);
            }
            List _hits = handler.getHits();
            this.hits = _hits.iterator();
        }

        public boolean next() {
            if (hits.hasNext()) {
                this.hit = (Hit) hits.next();
                return true;
            } else {
                this.hit = null;
                return false;
            }
        }

        public Object getValue() {
            return hit.getValue();
        }

        public float getScore() {
            return hit.getScore();
        }

        public void close() {
        }
    }

    ;

    private class SearchHandler extends SAXTracker {

        List hits;

        public List getHits() {
            return hits;
        }

        public void startDocument() throws SAXException {
            super.startDocument();
            hits = new ArrayList();
        }

        public void startElement(String nsuri, String lname, String qname, Attributes attrs) throws SAXException {
            super.startElement(nsuri, lname, qname, attrs);
            String id = attrs.getValue("id");
            String relevance = attrs.getValue("relevance");
            if (id != null && relevance != null) {
                float score;
                try {
                    score = Float.parseFloat(relevance);
                } catch (NumberFormatException e) {
                    score = 0f;
                }
                hits.add(new Hit(id, score));
            }
        }
    }
}
