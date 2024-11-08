package org.orbeon.oxf.processor;

import org.apache.log4j.Logger;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.common.ValidationException;
import org.orbeon.oxf.processor.generator.TidyConfig;
import org.orbeon.oxf.processor.serializer.CachedSerializer;
import org.orbeon.oxf.util.LoggerFactory;
import org.orbeon.oxf.xml.ForwardingContentHandler;
import org.orbeon.oxf.xml.TransformerUtils;
import org.orbeon.oxf.xml.XMLUtils;
import org.orbeon.oxf.xml.dom4j.LocationData;
import org.w3c.dom.Document;
import org.w3c.tidy.Configuration;
import org.w3c.tidy.Tidy;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import java.io.*;

/**
 * Intercept either an OutputStream or a Writer.
 *
 * This implementation holds a buffer for either a Writer or an Output Stream. The buffer can then
 * be parsed.
 */
public class StreamInterceptor {

    private static Logger logger = LoggerFactory.createLogger(StreamInterceptor.class);

    private StringWriter writer;

    private ByteArrayOutputStream byteStream;

    ;

    private String encoding = CachedSerializer.DEFAULT_ENCODING;

    private String contentType = ProcessorUtils.DEFAULT_CONTENT_TYPE;

    public Writer getWriter() {
        if (byteStream != null) throw new IllegalStateException("getWriter is called after getOutputStream was already called.");
        if (writer == null) writer = new StringWriter();
        return writer;
    }

    public OutputStream getOutputStream() {
        if (writer != null) throw new IllegalStateException("getOutputStream is called after getWriter was already called.");
        if (byteStream == null) byteStream = new ByteArrayOutputStream();
        return byteStream;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void parse(ContentHandler contentHandler) {
        parse(contentHandler, null, false);
    }

    public void parse(ContentHandler contentHandler, boolean fragment) {
        parse(contentHandler, null, fragment);
    }

    public void parse(ContentHandler contentHandler, TidyConfig tidyConfig, boolean fragment) {
        try {
            InputSource inputSource = null;
            String stringContent = null;
            if (writer != null) {
                stringContent = writer.toString();
                if (stringContent.length() > 0) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Document to parse in filter: ");
                        logger.debug(stringContent);
                    }
                    inputSource = new InputSource(new StringReader(stringContent));
                }
            } else if (byteStream != null) {
                byte[] byteContent = byteStream.toByteArray();
                if (byteContent.length > 0) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Document to parse in filter: ");
                        logger.debug(new String(byteContent, encoding));
                    }
                    inputSource = new InputSource(new ByteArrayInputStream(byteContent));
                    if (encoding != null) inputSource.setEncoding(encoding);
                }
            } else {
                throw new OXFException("Filtered resource did not call getWriter() or getOutputStream().");
            }
            if (inputSource != null) {
                if (ProcessorUtils.HTML_CONTENT_TYPE.equals(contentType)) {
                    Tidy tidy = new Tidy();
                    if (tidyConfig != null) {
                        tidy.setShowWarnings(tidyConfig.isShowWarnings());
                        tidy.setQuiet(tidyConfig.isQuiet());
                    }
                    InputStream inputStream;
                    if (writer == null) {
                        inputStream = inputSource.getByteStream();
                        tidy.setCharEncoding(TidyConfig.getTidyEncoding(encoding));
                    } else {
                        inputStream = new ByteArrayInputStream(stringContent.getBytes("utf-8"));
                        tidy.setCharEncoding(Configuration.UTF8);
                    }
                    Document document = tidy.parseDOM(inputStream, null);
                    Transformer transformer = TransformerUtils.getIdentityTransformer();
                    if (fragment) {
                        transformer.transform(new DOMSource(document), new SAXResult(new ForwardingContentHandler(contentHandler) {

                            public void startDocument() {
                            }

                            public void endDocument() {
                            }
                        }));
                    } else {
                        transformer.transform(new DOMSource(document), new SAXResult(contentHandler));
                    }
                } else {
                    SAXParser parser = XMLUtils.newSAXParser();
                    XMLReader reader = parser.getXMLReader();
                    if (fragment) {
                        reader.setContentHandler(new ForwardingContentHandler(contentHandler) {

                            public void startDocument() {
                            }

                            public void endDocument() {
                            }
                        });
                    } else {
                        reader.setContentHandler(contentHandler);
                    }
                    reader.parse(inputSource);
                }
            }
        } catch (SAXParseException e) {
            throw new ValidationException(e.getMessage(), new LocationData(e));
        } catch (Exception e) {
            throw new OXFException(e);
        }
    }
}
