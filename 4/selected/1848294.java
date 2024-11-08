package de.fzi.wikipipes.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.parsers.DOMParser;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Translate InputStreams to XML-handlers
 * 
 * @author voelkel
 * @author kurz
 * 
 */
public class Util {

    private static final Log log = LogFactory.getLog(Util.class);

    public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

    /**
	 * Read & clean as valid XML
	 * 
	 * @param in
	 * @return
	 */
    public static org.dom4j.Document getInputStreamAsDocument(InputStream inputstream) {
        org.cyberneko.html.parsers.DOMParser parser = new org.cyberneko.html.parsers.DOMParser();
        InputStream in = Util.cleanInputStream(inputstream);
        InputSource is = new InputSource(in);
        try {
            parser.parse(is);
            org.w3c.dom.Document w3cDocument = parser.getDocument();
            DOMReader xmlReader = new DOMReader();
            org.dom4j.Document dom4jDocument = xmlReader.read(w3cDocument);
            return dom4jDocument;
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static org.dom4j.Document getReaderAsDocument(Reader in) {
        DOMParser parser = new org.cyberneko.html.parsers.DOMParser();
        InputSource is = new InputSource(in);
        try {
            parser.parse(is);
            org.w3c.dom.Document w3cDocument = parser.getDocument();
            DOMReader xmlReader = new DOMReader();
            org.dom4j.Document dom4jDocument = xmlReader.read(w3cDocument);
            return dom4jDocument;
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * @param url
	 * @return an InputStream - Note: User has to close it!
	 */
    public static InputStream getInputStreamFromUrl(String url) {
        HttpClient httpclient = new HttpClient();
        GetMethod get = new GetMethod(url);
        try {
            httpclient.executeMethod(get);
            if (get.getStatusCode() == 200) {
                return get.getResponseBodyAsStream();
            } else {
                log.warn("Could not load from " + url + " got a " + get.getStatusCode());
                return null;
            }
        } catch (HttpException e) {
            throw new RuntimeException("Could not GET from " + url, e);
        } catch (IOException e) {
            throw new RuntimeException("Could not GET from " + url, e);
        } finally {
        }
    }

    /**
	 * @param httpClient
	 * @param url
	 * @return an InputStream - Note: User has to close it!
	 */
    public static InputStream getInputStreamFromUrl(HttpClient httpClient, String url) {
        GetMethod get = new GetMethod(url);
        try {
            httpClient.executeMethod(get);
            if (get.getStatusCode() == 200) {
                return get.getResponseBodyAsStream();
            } else {
                log.warn("Could not load from " + url + " got a " + get.getStatusCode());
                return null;
            }
        } catch (HttpException e) {
            throw new RuntimeException("Could not GET from " + url, e);
        } catch (IOException e) {
            throw new RuntimeException("Could not GET from " + url, e);
        } finally {
        }
    }

    public static String getMimetypeFromUrl(String url) {
        HttpClient httpclient = new HttpClient();
        GetMethod get = new GetMethod(url);
        try {
            httpclient.executeMethod(get);
            if (get.getStatusCode() == 200) {
                Header header = get.getRequestHeader(HTTP_HEADER_CONTENT_TYPE);
                if (header == null) return null;
                return header.getValue();
            } else {
                return null;
            }
        } catch (HttpException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            get.releaseConnection();
        }
    }

    /**
	 * 
	 * @param file
	 * @return a dom4j Document
	 */
    public static Document loadXMLFile(File file) {
        SAXReader reader = new SAXReader();
        try {
            FileReader fr = new FileReader(file);
            return reader.read(fr);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * 
	 * @param xml
	 *            file as dom4 Document
	 * @param xslt
	 *            as dom4j Document
	 * @return content of page an dom4j Document
	 */
    public static Document transform(Document in, Document xslt) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        DocumentSource styleSource = new DocumentSource(xslt);
        Transformer transformer = factory.newTransformer(styleSource);
        DocumentSource source = new DocumentSource(in);
        DocumentResult result = new DocumentResult();
        transformer.transform(source, result);
        return result.getDocument();
    }

    public static String transformToString(Reader in, Document xslt) throws UnsupportedEncodingException, TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        DocumentSource styleSource = new DocumentSource(xslt);
        Transformer transformer = factory.newTransformer(styleSource);
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        StreamSource source = new StreamSource(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter outw = new OutputStreamWriter(out, "UTF-8");
        StreamResult result = new StreamResult(outw);
        transformer.transform(source, result);
        return out.toString("UTF-8");
    }

    private static Map<String, Templates> templatesCache = new HashMap<String, Templates>();

    private static SAXParserFactory spf = SAXParserFactory.newInstance();

    static {
        spf.setNamespaceAware(true);
        spf.setValidating(false);
    }

    public static final String XHTML10Strict_PUBLICID = "-//W3C//DTD XHTML 1.0 Strict//EN";

    public static final String XHTML10Strict_SYSTEMID = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd";

    public static final String XHTML10Transitional_PUBLICID = "-//W3C//DTD XHTML 1.0 Transitional//EN";

    public static final String XHTML10Transitional_SYSTEMID = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd";

    /**
	 * 
	 * @param in
	 * @param xsltResourceName
	 *            - to be loaded from the classpath
	 * @return transformer result
	 * @throws TransformerConfigurationException
	 * @throws UnsupportedEncodingException
	 */
    public static String transformToStringAndBufferXslt(Reader in, String xsltResourceName) throws TransformerConfigurationException, UnsupportedEncodingException {
        XMLReader xmlreader;
        try {
            xmlreader = spf.newSAXParser().getXMLReader();
        } catch (SAXException e1) {
            throw new RuntimeException(e1);
        } catch (ParserConfigurationException e1) {
            throw new RuntimeException(e1);
        }
        xmlreader.setEntityResolver(new EntityResolver() {

            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                log.debug("Resolving " + publicId + " " + systemId);
                if (XHTML10Strict_PUBLICID.equals(publicId) && XHTML10Strict_SYSTEMID.equals(systemId)) {
                    return new InputSource(new StringReader(""));
                } else if (XHTML10Transitional_PUBLICID.equals(publicId) && XHTML10Transitional_SYSTEMID.equals(systemId)) {
                    return new InputSource(new StringReader(""));
                } else {
                    throw new RuntimeException("Could not resolve " + publicId + " " + systemId);
                }
            }
        });
        SAXSource saxsource = new SAXSource(xmlreader, new InputSource(in));
        log.debug("Get transformer");
        Templates templates = getTemplates(xsltResourceName);
        Transformer transformer = templates.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        URIResolver uriResolver = transformer.getURIResolver();
        URIResolver proxyUriResolver = new ProxyUriResolver(uriResolver);
        transformer.setURIResolver(proxyUriResolver);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter outw = new OutputStreamWriter(out, "UTF-8");
        StreamResult result = new StreamResult(outw);
        try {
            log.debug("Starting transformation...");
            transformer.transform(saxsource, result);
            log.debug("Done transformation");
        } catch (TransformerException e) {
            if (log.isDebugEnabled()) {
                log.debug("Could not transform");
            }
            throw new RuntimeException(e);
        }
        transformer.reset();
        return out.toString("UTF-8");
    }

    private static Templates getTemplates(String xsltResourceName) throws TransformerConfigurationException {
        Templates templates = templatesCache.get(xsltResourceName);
        if (templates == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            InputStream xsltIn = cl.getResourceAsStream(xsltResourceName);
            InputStreamReader xslt = new InputStreamReader(xsltIn);
            TransformerFactory factory = TransformerFactory.newInstance();
            StreamSource styleSource = new StreamSource(xslt);
            templates = factory.newTemplates(styleSource);
            templatesCache.put(xsltResourceName, templates);
            log.debug("Created transformer for '" + xsltResourceName + "' and put in cache.");
        } else {
            log.debug("Re-using cached transformer. Yeah!");
        }
        return templates;
    }

    public static String transformToString(Reader in, Reader xslt) throws TransformerException, UnsupportedEncodingException {
        TransformerFactory factory = TransformerFactory.newInstance();
        StreamSource styleSource = new StreamSource(xslt);
        Transformer transformer = factory.newTransformer(styleSource);
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        Document doc = Util.getReaderAsDocument(in);
        doc.setDocType(null);
        String xmlstring = Util.replaceInvalidXmlCharacters(doc.asXML());
        StreamSource source = new StreamSource(Util.getStringAsReader(xmlstring));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter outw = new OutputStreamWriter(out, "UTF-8");
        StreamResult result = new StreamResult(outw);
        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            if (log.isDebugEnabled()) {
                log.debug("Could not transform: \n" + doc.asXML());
            }
            throw e;
        }
        return out.toString("UTF-8");
    }

    public static String transformToString(Document in, Document xslt) throws TransformerException, IOException {
        TransformerFactory factory = TransformerFactory.newInstance();
        DocumentSource styleSource = new DocumentSource(xslt);
        Transformer transformer = factory.newTransformer(styleSource);
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        DocumentSource source = new DocumentSource(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter outw = new OutputStreamWriter(out, "UTF-8");
        StreamResult result = new StreamResult(outw);
        transformer.transform(source, result);
        return out.toString("UTF-8");
    }

    /**
	 * Code snippet taken from Jakarta Commons IO IOUtils
	 * 
	 * @param reader
	 * @param writer
	 * @return
	 * @throws IOException
	 */
    public static long copy(Reader reader, Writer writer) {
        assert reader != null;
        assert writer != null;
        char[] buffer = new char[1024 * 4];
        long count = 0;
        int n = 0;
        try {
            while (-1 != (n = reader.read(buffer))) {
                writer.write(buffer, 0, n);
                count += n;
            }
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return count;
    }

    public static String getAsString(Reader reader) {
        assert reader != null;
        StringWriter sw = new StringWriter();
        copy(reader, sw);
        return sw.getBuffer().toString();
    }

    public static Reader getAsReader(File file) {
        try {
            return new FileReader(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not find " + file.getAbsolutePath());
        }
    }

    public static Reader getDocumentAsReader(Document xhtml) {
        StringWriter sw = new StringWriter();
        XMLUtil.toCanonicalXML(xhtml, sw);
        String xml = sw.getBuffer().toString();
        return new StringReader(xml);
    }

    public static Reader getStringAsReader(String s) {
        return new StringReader(s);
    }

    public static FileWriter getFileWriter(File f) {
        try {
            return new FileWriter(f);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream getStringAsInputStream(String s) {
        byte[] buffer = s.getBytes();
        InputStream in = new ByteArrayInputStream(buffer);
        return in;
    }

    /**
	 * @param is
	 *            a string like attachment:NAME/parentname:NAME/URLtoRessource
	 *            attachment
	 *            :manual.pdf/parentname:Eclipse/http://ontoworld.org/manual.pdf
	 */
    public static void storeSubpage(File rootRepoDirectory, String s) {
        if (s.startsWith("attachment:")) {
            s = s.replaceFirst("attachment:", "");
            String name = s.substring(0, s.indexOf('/'));
            s = s.replaceFirst(name + "/parentname:", "");
            String parent = s.substring(0, s.indexOf('/'));
            s = s.replaceFirst(parent + "/", "");
            File attachStorageDir = new File(rootRepoDirectory.getAbsolutePath() + "\\" + parent + "." + "attachments");
            attachStorageDir.mkdirs();
            File file = new File(attachStorageDir, name);
            FileWriter writer = getFileWriter(file);
            InputStream in = null;
            Reader reader = null;
            if (s.startsWith("file:///")) {
                reader = getAsReader(new File(s.replaceFirst("file:///", "")));
            } else {
                in = getInputStreamFromUrl(s);
                reader = new BufferedReader(new InputStreamReader(in));
            }
            copy(reader, writer);
        }
    }

    public static Document parseDocumentWithTagsoup(Document indoc) {
        org.ccil.cowan.tagsoup.Parser parser = new org.ccil.cowan.tagsoup.Parser();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        org.ccil.cowan.tagsoup.XMLWriter writer = new org.ccil.cowan.tagsoup.XMLWriter(new OutputStreamWriter(out));
        writer.setOutputProperty(org.ccil.cowan.tagsoup.XMLWriter.METHOD, "xml");
        writer.setOutputProperty(org.ccil.cowan.tagsoup.XMLWriter.OMIT_XML_DECLARATION, "yes");
        parser.setContentHandler(writer);
        try {
            parser.parse(new InputSource(getDocumentAsReader(indoc)));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document w3doc = builder.parse(new ByteArrayInputStream(out.toByteArray()));
            DOMReader xmlReader = new DOMReader();
            org.dom4j.Document dom4jDocument = xmlReader.read(w3doc);
            return dom4jDocument;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Reader tagsoup(Reader reader) {
        try {
            org.ccil.cowan.tagsoup.Parser parser = new org.ccil.cowan.tagsoup.Parser();
            StringWriter out = new StringWriter();
            org.ccil.cowan.tagsoup.XMLWriter writer = new org.ccil.cowan.tagsoup.XMLWriter(out);
            writer.setOutputProperty(org.ccil.cowan.tagsoup.XMLWriter.METHOD, "xml");
            writer.setOutputProperty(org.ccil.cowan.tagsoup.XMLWriter.OMIT_XML_DECLARATION, "yes");
            writer.setOutputProperty(org.ccil.cowan.tagsoup.XMLWriter.INDENT, "yes");
            parser.setContentHandler(writer);
            InputSource input;
            input = new InputSource(reader);
            parser.parse(input);
            out.flush();
            String s = out.getBuffer().toString();
            return new StringReader(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * add and set content- and property-attributes in a-elements
	 * 
	 * @param stifDocument
	 *            : stif dom4j document
	 * @param wiki
	 *            : wikireposity
	 */
    public static void addContentAndPropertyAttributesToAElements(Document stifDocument, AbstractWebWikiRepository wiki) {
        for (Object o : stifDocument.selectNodes("//*[@class=\"stif-internal\"]")) {
            Element e = (Element) o;
            e.addAttribute("property", "stif:key");
            e.addAttribute("content", wiki.getPageURL(e.attributeValue("href")));
        }
        for (Object o : stifDocument.selectNodes("//*[@class=\"stif-external\"]")) {
            Element e = (Element) o;
            e.addAttribute("property", "stif:key");
            e.addAttribute("content", e.attributeValue("href"));
        }
    }

    /**
	 * replace invalid xml characters with '-'
	 * 
	 * @param xmlstring
	 * @return cleaned xmlstring
	 */
    public static String replaceInvalidXmlCharacters(String xmlstring) {
        String result = xmlstring;
        result = result.replace("&#26;", "-").replace("&#7;", "-");
        return result.replace("&#12;", "-").replace("&#11;", "-");
    }

    /**
	 * replace character chains '<http' and '<mailto' with &lt;http and
	 * &lt;mailto
	 * 
	 * @param inputStream
	 * @return cleaned inputStream
	 * @throws UnsupportedEncodingException
	 */
    public static InputStream cleanInputStream(InputStream inputStream) {
        Reader reader = new InputStreamReader(inputStream);
        StringWriter writer = new StringWriter();
        copy(reader, writer);
        String buffer = writer.getBuffer().toString();
        buffer = buffer.replaceAll("<http", "&lt;http");
        buffer = buffer.replace("<mailto", "&lt;mailto");
        buffer = buffer.replace("<ftp", "&lt;ftp");
        buffer = buffer.replace("<wiki", "&lt;wiki");
        return new ByteArrayInputStream(buffer.getBytes());
    }
}
