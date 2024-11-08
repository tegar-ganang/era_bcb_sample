package org.orbeon.oxf.processor.serializer.legacy;

import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.ProcessorInput;
import org.orbeon.oxf.processor.ProcessorImpl;
import org.orbeon.oxf.processor.serializer.HttpTextSerializer;
import org.orbeon.oxf.processor.serializer.SerializerContentHandler;
import org.orbeon.oxf.xml.TransformerUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.Writer;

public class HTMLSerializer extends HttpTextSerializer {

    public static final String DEFAULT_CONTENT_TYPE = "text/html";

    public static final String DEFAULT_METHOD = "html";

    public static final String DEFAULT_PUBLIC_DOCTYPE = "-//W3C//DTD HTML 4.01 Transitional//EN";

    public static final String DEFAULT_SYSTEM_DOCTYPE = null;

    public static final String DEFAULT_VERSION = "4.01";

    protected void readInput(PipelineContext context, ProcessorInput input, Config config, Writer writer) {
        TransformerHandler identity = TransformerUtils.getIdentityTransformerHandler();
        TransformerUtils.applyOutputProperties(identity.getTransformer(), config.method != null ? config.method : DEFAULT_METHOD, config.version != null ? config.version : null, config.publicDoctype != null ? config.publicDoctype : null, config.systemDoctype != null ? config.systemDoctype : null, getEncoding(config, null, DEFAULT_ENCODING), config.omitXMLDeclaration, config.standalone, config.indent, config.indentAmount);
        identity.setResult(new StreamResult(writer));
        ProcessorImpl.readInputAsSAX(context, input, new StripNamespaceContentHandler(identity, writer, isSerializeXML11()));
    }

    protected static class StripNamespaceContentHandler extends SerializerContentHandler {

        public StripNamespaceContentHandler(ContentHandler contentHandler, Writer writer, boolean serializeXML11) {
            super(contentHandler, writer, serializeXML11);
        }

        public void startPrefixMapping(String s, String s1) throws SAXException {
        }

        public void endPrefixMapping(String s) throws SAXException {
        }
    }

    protected String getDefaultContentType() {
        return DEFAULT_CONTENT_TYPE;
    }
}
