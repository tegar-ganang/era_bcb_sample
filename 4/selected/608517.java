package org.orbeon.oxf.processor.serializer;

import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.pipeline.api.ExternalContext;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.ProcessorImpl;
import org.orbeon.oxf.processor.ProcessorInput;
import org.orbeon.oxf.processor.ProcessorOutput;
import org.orbeon.oxf.processor.XMLConstants;
import org.orbeon.oxf.util.ContentHandlerWriter;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * Legacy HTTP text serializer. This is deprecated by HttpSerializer.
 */
public abstract class HttpTextSerializer extends HttpSerializerBase {

    private static final String DEFAULT_TEXT_DOCUMENT_ELEMENT = "document";

    protected final void readInput(PipelineContext pipelineContext, ExternalContext.Response response, ProcessorInput input, Object _config, OutputStream outputStream) {
        Config config = (Config) _config;
        String encoding = getEncoding(config, null, DEFAULT_ENCODING);
        if (response != null) {
            String contentType = getContentType(config, null, getDefaultContentType());
            if (contentType != null) response.setContentType(contentType + "; charset=" + encoding);
        }
        try {
            Writer writer = new OutputStreamWriter(outputStream, encoding);
            readInput(pipelineContext, input, config, writer);
        } catch (UnsupportedEncodingException e) {
            throw new OXFException(e);
        }
    }

    protected boolean isSerializeXML11() {
        return getPropertySet().getBoolean("serialize-xml-11", false).booleanValue();
    }

    /**
     * This must be overridden by subclasses.
     */
    protected abstract void readInput(PipelineContext context, ProcessorInput input, Config config, Writer writer);

    /**
     * This method is use when the legacy serializer is used in the new converter mode. In this
     * case, the converter exposes a "data" output, and the processor's start() method is not
     * called.
     */
    public ProcessorOutput createOutput(String name) {
        if (!name.equals(OUTPUT_DATA)) throw new OXFException("Invalid output created: " + name);
        ProcessorOutput output = new ProcessorImpl.CacheableTransformerOutputImpl(getClass(), name) {

            public void readImpl(PipelineContext pipelineContext, ContentHandler contentHandler) {
                Writer writer = new ContentHandlerWriter(contentHandler);
                Config config = readConfig(pipelineContext);
                String encoding = getEncoding(config, null, DEFAULT_ENCODING);
                String contentType = getContentType(config, null, getDefaultContentType());
                try {
                    AttributesImpl attributes = new AttributesImpl();
                    contentHandler.startPrefixMapping(XMLConstants.XSI_PREFIX, XMLConstants.XSI_URI);
                    contentHandler.startPrefixMapping(XMLConstants.XSD_PREFIX, XMLConstants.XSD_URI);
                    attributes.addAttribute(XMLConstants.XSI_URI, "type", "xsi:type", "CDATA", XMLConstants.XS_STRING_QNAME.getQualifiedName());
                    if (contentType != null) attributes.addAttribute("", "content-type", "content-type", "CDATA", contentType + "; charset=" + encoding);
                    contentHandler.startDocument();
                    contentHandler.startElement("", DEFAULT_TEXT_DOCUMENT_ELEMENT, DEFAULT_TEXT_DOCUMENT_ELEMENT, attributes);
                    readInput(pipelineContext, getInputByName(INPUT_DATA), config, writer);
                    contentHandler.endElement("", DEFAULT_TEXT_DOCUMENT_ELEMENT, DEFAULT_TEXT_DOCUMENT_ELEMENT);
                    contentHandler.endDocument();
                } catch (SAXException e) {
                    throw new OXFException(e);
                }
            }
        };
        addOutput(name, output);
        return output;
    }
}
