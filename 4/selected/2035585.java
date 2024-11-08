package org.exist.xquery.functions.request;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.log4j.Logger;
import org.exist.dom.QName;
import org.exist.external.org.apache.commons.io.output.ByteArrayOutputStream;
import org.exist.http.servlets.RequestWrapper;
import org.exist.memtree.DocumentBuilderReceiver;
import org.exist.memtree.MemTreeBuilder;
import org.exist.util.MimeTable;
import org.exist.util.MimeType;
import org.exist.util.io.CachingFilterInputStream;
import org.exist.util.io.FilterInputStreamCache;
import org.exist.util.io.MemoryMappedFileFilterInputStreamCache;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.Variable;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.Base64BinaryValueType;
import org.exist.xquery.value.BinaryValue;
import org.exist.xquery.value.BinaryValueFromInputStream;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.JavaObjectValue;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * @author Wolfgang Meier <wolfgang@exist-db.org>
 * @author Adam retter <adam@exist-db.org>
 */
public class GetData extends BasicFunction {

    protected static final Logger logger = Logger.getLogger(GetData.class);

    public static final FunctionSignature signature = new FunctionSignature(new QName("get-data", RequestModule.NAMESPACE_URI, RequestModule.PREFIX), "Returns the content of a POST request. " + "If the HTTP Content-Type header in the request identifies it as a binary document, then xs:base64Binary is returned. " + "If its not a binary document, we attempt to parse it as XML and return a document-node(). " + "If its not a binary or XML document, any other data type is returned as an xs:string representation or " + "an empty sequence if there is no data to be read.", null, new FunctionReturnSequenceType(Type.ITEM, Cardinality.ZERO_OR_ONE, "the content of a POST request"));

    public GetData(XQueryContext context) {
        super(context, signature);
    }

    @Override
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {
        RequestModule myModule = (RequestModule) context.getModule(RequestModule.NAMESPACE_URI);
        Variable var = myModule.resolveVariable(RequestModule.REQUEST_VAR);
        if (var == null || var.getValue() == null) {
            throw new XPathException(this, "No request object found in the current XQuery context.");
        }
        if (var.getValue().getItemType() != Type.JAVA_OBJECT) {
            throw new XPathException(this, "Variable $request is not bound to an Java object.");
        }
        JavaObjectValue value = (JavaObjectValue) var.getValue().itemAt(0);
        if (!(value.getObject() instanceof RequestWrapper)) {
            throw new XPathException(this, "Variable $request is not bound to a Request object.");
        }
        RequestWrapper request = (RequestWrapper) value.getObject();
        if (request.getContentLength() == -1 || request.getContentLength() == 0) {
            return Sequence.EMPTY_SEQUENCE;
        }
        InputStream is = null;
        FilterInputStreamCache cache = null;
        try {
            cache = new MemoryMappedFileFilterInputStreamCache();
            is = new CachingFilterInputStream(cache, request.getInputStream());
            is.mark(Integer.MAX_VALUE);
        } catch (IOException ioe) {
            throw new XPathException(this, "An IO exception occurred: " + ioe.getMessage(), ioe);
        }
        Sequence result = Sequence.EMPTY_SEQUENCE;
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
                if (result == Sequence.EMPTY_SEQUENCE) {
                    result = parseAsXml(is);
                }
                if (result == Sequence.EMPTY_SEQUENCE) {
                    String encoding = request.getCharacterEncoding();
                    if (encoding == null) {
                        encoding = "UTF-8";
                    }
                    try {
                        is.reset();
                        result = parseAsString(is, encoding);
                    } catch (IOException ioe) {
                        throw new XPathException(this, "An IO exception occurred: " + ioe.getMessage(), ioe);
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

    private Sequence parseAsXml(InputStream is) {
        Sequence result = Sequence.EMPTY_SEQUENCE;
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

    private Sequence parseAsString(InputStream is, String encoding) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int read = -1;
        while ((read = is.read(buf)) > -1) {
            bos.write(buf, 0, read);
        }
        String s = new String(bos.toByteArray(), encoding);
        return new StringValue(s);
    }
}
