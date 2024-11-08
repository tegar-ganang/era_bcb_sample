package com.mebigfatguy.tomailer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * the main processer for converting web pages to email pages
 */
public class ToMailerDelegate {

    private static final String PROCESSED_CLASS = "_TOMAILER_PROCESSED_";

    private String rootUrl;

    private byte[] mailer;

    private DocumentBuilderFactory dbf;

    private XPathFactory xpf;

    private TransformerFactory tf;

    private final CssParser parser = new CssParser();

    /**
	 * constructs the delegate and orchestrates the conversion of the page
	 * 
	 * @param is the stream that represents the web page
	 * @param url the base url to use for relative links
	 * 
	 * @throws ToMailerException if conversion fails
	 */
    public ToMailerDelegate(InputStream is, String url) throws ToMailerException {
        rootUrl = url;
        if (!rootUrl.endsWith("/")) rootUrl += "/";
        try {
            dbf = DocumentBuilderFactory.newInstance();
            xpf = XPathFactory.newInstance();
            tf = TransformerFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setEntityResolver(new ToMailerEntityResolver());
            Document d = db.parse(is);
            parseEmbeddedCss(d);
            parseExternalCss(d);
            CssItem[] items = parser.prioritizeCss();
            embedStyles(d, items);
            stripClassAttributes(d);
            adjustRelativeLinks(d);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.transform(new DOMSource(d), new StreamResult(baos));
            mailer = baos.toByteArray();
        } catch (SAXException se) {
            throw new ToMailerException("Failed parsing html document", se);
        } catch (ParserConfigurationException pce) {
            throw new ToMailerException("Failed parsing html document", pce);
        } catch (TransformerException te) {
            throw new ToMailerException("Failed parsing html document", te);
        } catch (XPathExpressionException xpee) {
            throw new ToMailerException("Failed to generate a proper xpath", xpee);
        } catch (IOException ioe) {
            throw new ToMailerException("Failed parsing html document", ioe);
        }
    }

    /**
	 * get the generated email version
	 * 
	 * @return an array of bytes that represents the page
	 */
    public byte[] getMailer() {
        return mailer;
    }

    /**
	 * looks for and parses css that is found in style tags directly in the html
	 * 
	 * @param d the document to parse
	 * @throws XPathExpressionException if the xpath expressions are misformed
	 */
    private void parseEmbeddedCss(Document d) throws XPathExpressionException {
        XPath xp = xpf.newXPath();
        XPathExpression xpe = xp.compile("//style");
        NodeList nl = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            parser.add(e.getTextContent());
            Element parent = (Element) e.getParentNode();
            parent.removeChild(e);
        }
    }

    /**
	 * looks for and parses css that is found in references of link tags to external resources
	 * 
	 * @param d the document to parse
	 * @throws XPathExpressionException if the xpath expressions are misformed
	 * @throws IOException if the external css can't be fetched
	 */
    private void parseExternalCss(Document d) throws XPathExpressionException, IOException {
        InputStream is = null;
        try {
            XPath xp = xpf.newXPath();
            XPathExpression xpe = xp.compile("//link[@type='text/css']/@href");
            NodeList nl = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) {
                Attr a = (Attr) nl.item(i);
                String url = a.getValue();
                URL u = new URL(url);
                is = new BufferedInputStream(u.openStream());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(is, baos);
                parser.add(new String(baos.toByteArray(), "UTF-8"));
                Element linkNode = a.getOwnerElement();
                Element parent = (Element) linkNode.getParentNode();
                parent.removeChild(linkNode);
                IOUtils.closeQuietly(is);
                is = null;
            }
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    /**
	 * inject css into style attributes of the appropriate elements
	 * 
	 * @param d the document to parse
	 * @param items the css items ordered by importance
	 * 
	 * @throws XPathExpressionException if the xpath used is invalid
	 */
    private void embedStyles(Document d, CssItem[] items) throws XPathExpressionException {
        XPath xp = xpf.newXPath();
        for (CssItem item : items) {
            String path = buildXPathFromSelector(item.getSelector());
            XPathExpression xpe = xp.compile(path);
            NodeList nl = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) {
                Element e = (Element) nl.item(i);
                StringBuilder style = new StringBuilder(e.getAttribute("style"));
                for (String styleItem : item.getStyles()) style.append(styleItem);
                e.setAttribute("class", PROCESSED_CLASS);
                e.setAttribute("style", style.toString());
            }
        }
    }

    /**
	 * returns an xpath that represents a css selector. The xpath is embellished
	 * so that elements that have already been processed are ignored.
	 * 
	 * @param selector the css item to build an xpath out of
	 * 
	 * @return the xpath
	 */
    private String buildXPathFromSelector(String selector) {
        StringBuilder path = new StringBuilder();
        path.append("/");
        String[] selectors = selector.split("\\s+");
        for (int i = 0; i < selectors.length; i++) {
            String selPath = selectors[i];
            char c = selPath.charAt(0);
            boolean lastPath = (i == (selectors.length - 1));
            switch(c) {
                case '#':
                    path.append("/*[@id='").append(selPath.substring(1));
                    if (lastPath) {
                        path.append("' and (not(@class) or (@class!='").append(PROCESSED_CLASS).append("))");
                    }
                    path.append("']");
                    break;
                case '.':
                    path.append("/*[@class='").append(selPath.substring(1)).append("']");
                    break;
                default:
                    path.append("/").append(selPath);
                    if (lastPath) {
                        path.append("[not(@class) or (@class!='").append(PROCESSED_CLASS).append("')]");
                    }
                    break;
            }
        }
        return path.toString();
    }

    /**
	 * remoevs all class attributes from a document
	 * 
	 * @param d the document to remove the attributes from
	 * @throws XPathExpressionException if the xpath used is invalid
	 */
    private void stripClassAttributes(Document d) throws XPathExpressionException {
        XPath xp = xpf.newXPath();
        XPathExpression xpe = xp.compile("//@class");
        NodeList nl = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);
        for (int i = 0; i < nl.getLength(); i++) {
            Attr a = (Attr) nl.item(i);
            a.getOwnerElement().removeAttribute(a.getName());
        }
    }

    /**
	 * finds all relative links and adjusts them by prepending the root url to them
	 * so that the urls can be used from email
	 * 
	 * @param d the document to find urls in
	 * @throws XPathExpressionException if the xpath used is invalid
	 */
    private void adjustRelativeLinks(Document d) throws XPathExpressionException {
        XPath xp = xpf.newXPath();
        XPathExpression xpe = xp.compile("//img/@src|//a/@href");
        NodeList nl = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);
        for (int i = 0; i < nl.getLength(); i++) {
            Attr a = (Attr) nl.item(i);
            String url = a.getValue();
            if (!url.startsWith("http")) {
                url = rootUrl + (url.startsWith("/") ? url.substring(1) : url);
                a.setValue(url);
            }
        }
    }
}
