package org.exist.http.sleepy.impl;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.log4j.Logger;
import org.exist.external.org.apache.commons.io.output.ByteArrayOutputStream;
import org.exist.http.servlets.RequestWrapper;
import org.exist.memtree.DocumentBuilderReceiver;
import org.exist.memtree.MemTreeBuilder;
import org.exist.util.MimeTable;
import org.exist.util.MimeType;
import org.exist.util.io.CachingFilterInputStream;
import org.exist.util.io.FilterInputStreamCache;
import org.exist.util.io.MemoryMappedFileFilterInputStreamCache;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 *
 * @author Adam Retter <adam.retter@googlemail.com>
 */
public class RequestBodyParser {

    protected static final Logger LOG = Logger.getLogger(RequestBodyParser.class);

    public static Item parse(XQueryContext context, RequestWrapper request) throws XPathException {
        if (request.getContentLength() == -1 || request.getContentLength() == 0) {
            return null;
        }
        InputStream is = null;
        FilterInputStreamCache cache = null;
        try {
            cache = new MemoryMappedFileFilterInputStreamCache();
            is = new CachingFilterInputStream(cache, request.getInputStream());
            is.mark(Integer.MAX_VALUE);
        } catch (IOException ioe) {
            throw new XPathException("An IO exception occurred: " + ioe.getMessage(), ioe);
        }
        Item result = null;
        try {
            if (is != null && request.getContentLength() > 0) {
                String contentType = request.getContentType();
                if (contentType != null) {
                    if (contentType.indexOf(";") > -1) {
                        contentType = contentType.substring(0, contentType.indexOf(";"));
                    }
                    MimeType mimeType = MimeTable.getInstance().getContentType(contentType);
                    if (mimeType != null && !mimeType.isXMLType()) {
                        result = BinaryValueFromInputStream.getInstance(context, new Base64BinaryValueType(), is);
                    }
                }
                if (result == null) {
                    result = parseAsXml(context, is);
                }
                if (result == null) {
                    String encoding = request.getCharacterEncoding();
                    if (encoding == null) {
                        encoding = "UTF-8";
                    }
                    try {
                        is.reset();
                        result = parseAsString(is, encoding);
                    } catch (IOException ioe) {
                        throw new XPathException("An IO exception occurred: " + ioe.getMessage(), ioe);
                    }
                }
            }
        } finally {
            if (cache != null) {
                try {
                    cache.invalidate();
                } catch (IOException ioe) {
                    LOG.error(ioe.getMessage(), ioe);
                }
            }
            if (is != null && !(result instanceof BinaryValue)) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    LOG.error(ioe.getMessage(), ioe);
                }
            }
        }
        return result;
    }

    private static NodeValue parseAsXml(XQueryContext context, InputStream is) {
        NodeValue result = null;
        XMLReader reader = null;
        context.pushDocumentContext();
        try {
            InputSource src = new InputSource(new CloseShieldInputStream(is));
            reader = context.getBroker().getBrokerPool().getParserPool().borrowXMLReader();
            MemTreeBuilder builder = context.getDocumentBuilder();
            DocumentBuilderReceiver receiver = new DocumentBuilderReceiver(builder, true);
            reader.setContentHandler(receiver);
            reader.parse(src);
            Document doc = receiver.getDocument();
            result = (NodeValue) doc;
        } catch (SAXException saxe) {
        } catch (IOException ioe) {
        } finally {
            context.popDocumentContext();
            if (reader != null) {
                context.getBroker().getBrokerPool().getParserPool().returnXMLReader(reader);
            }
        }
        return result;
    }

    private static StringValue parseAsString(InputStream is, String encoding) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int read = -1;
        while ((read = is.read(buf)) > -1) {
            bos.write(buf, 0, read);
        }
        String s = new String(bos.toByteArray(), encoding);
        return new StringValue(s);
    }
}
