package ru.dpelevin.http.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Utility class for html to xml transformations.
 * 
 * @author Dmitry Pelevin
 */
public final class HtmlHelper {

    /** The transformer factory. */
    private static javax.xml.transform.TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory.newInstance();

    /** The Constant log. */
    private static final Log log = LogFactory.getLog(HtmlHelper.class.getClass());

    /** The xsl transformers. */
    private static Hashtable<String, Transformer> xslTransformers = new Hashtable<String, Transformer>();

    /**
	 * Instantiates a new html helper.
	 */
    private HtmlHelper() {
    }

    /**
	 * Convert html to xml.
	 * 
	 * @param htmlInputStream
	 *            the html input stream
	 * @param encoding
	 *            the encoding
	 * 
	 * @return the document
	 * 
	 * @throws SAXException
	 *             the SAX exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws TransformerException
	 *             the transformer exception
	 */
    public static Document convertHtmlToXml(final InputStream htmlInputStream, final String encoding) throws SAXException, IOException, TransformerException {
        Parser p = new Parser();
        javax.xml.parsers.DocumentBuilder db;
        try {
            db = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            log.error("", e);
            throw new RuntimeException();
        }
        Document document = db.newDocument();
        InputSource iSource = new InputSource(htmlInputStream);
        iSource.setEncoding(encoding);
        Source transformerSource = new SAXSource(p, iSource);
        Transformer xslTransformer = TransformerFactory.newInstance().newTransformer();
        Result result = new DOMResult(document);
        xslTransformer.transform(transformerSource, result);
        htmlInputStream.close();
        return document;
    }

    /**
	 * Convert html to xml.
	 * 
	 * @param htmlInputStream
	 *            the html input stream
	 * @param classpathXsltResource
	 *            the classpath xslt resource
	 * @param encoding
	 *            the encoding
	 * 
	 * @return the document
	 */
    public static Document convertHtmlToXml(final InputStream htmlInputStream, final String classpathXsltResource, final String encoding) {
        Parser p = new Parser();
        javax.xml.parsers.DocumentBuilder db;
        try {
            db = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            log.error("", e);
            throw new RuntimeException();
        }
        Document document = db.newDocument();
        InputStream is = htmlInputStream;
        if (log.isDebugEnabled()) {
            ByteArrayOutputStream baos;
            baos = new ByteArrayOutputStream();
            try {
                IOUtils.copy(is, baos);
            } catch (IOException e) {
                log.error("Fail to make input stream copy.", e);
            }
            IOUtils.closeQuietly(is);
            ByteArrayInputStream byteArrayInputStream;
            byteArrayInputStream = new ByteArrayInputStream(baos.toByteArray());
            try {
                IOUtils.toString(new ByteArrayInputStream(baos.toByteArray()), "UTF-8");
            } catch (IOException e) {
                log.error("", e);
            }
            IOUtils.closeQuietly(byteArrayInputStream);
            is = new ByteArrayInputStream(baos.toByteArray());
        }
        try {
            InputSource iSource = new InputSource(is);
            iSource.setEncoding(encoding);
            Source transformerSource = new SAXSource(p, iSource);
            Result result = new DOMResult(document);
            Transformer xslTransformer = getTransformerByName(classpathXsltResource, false);
            try {
                xslTransformer.transform(transformerSource, result);
            } catch (TransformerException e) {
                throw new RuntimeException(e);
            }
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                log.warn("", e);
            }
        }
        return document;
    }

    /**
	 * Gets the transformer by name.
	 * 
	 * @param xslName
	 *            the xsl name
	 * @param forceCreation
	 *            the force creation
	 * 
	 * @return the transformer by name
	 */
    protected static Transformer getTransformerByName(final String xslName, final boolean forceCreation) {
        if (forceCreation) {
            Transformer t = loadTransformer(xslName);
            xslTransformers.put(xslName, t);
            return t;
        }
        Transformer t = xslTransformers.get(xslName);
        if (t != null) {
            return t;
        }
        t = loadTransformer(xslName);
        xslTransformers.put(xslName, t);
        return t;
    }

    /**
	 * Load transformer.
	 * 
	 * @param xslName
	 *            the xsl name
	 * 
	 * @return the transformer
	 */
    private static Transformer loadTransformer(final String xslName) {
        InputStream is = HtmlHelper.class.getClassLoader().getResourceAsStream(xslName);
        if (is == null) {
            String message = "Can't find xsl for name " + xslName;
            log.error(message);
            throw new IllegalArgumentException(message);
        }
        try {
            Transformer tf;
            try {
                tf = transformerFactory.newTransformer(new StreamSource(is));
            } catch (TransformerConfigurationException e) {
                log.error("", e);
                throw new RuntimeException();
            }
            return tf;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                log.warn("", e);
            }
        }
    }
}
