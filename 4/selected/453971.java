package org.orbeon.oxf.processor.serializer.legacy;

import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.ProcessorInput;
import org.orbeon.oxf.processor.ProcessorImpl;
import org.orbeon.oxf.processor.serializer.HttpTextSerializer;
import org.orbeon.oxf.processor.serializer.SerializerContentHandler;
import org.orbeon.oxf.xml.TransformerUtils;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.Writer;

public class XMLSerializer extends HttpTextSerializer {

    public static final String DEFAULT_CONTENT_TYPE = "application/xml";

    public static final String DEFAULT_METHOD = "xml";

    public static final String DEFAULT_VERSION = "1.0";

    protected String getDefaultContentType() {
        return DEFAULT_CONTENT_TYPE;
    }

    protected void readInput(PipelineContext context, ProcessorInput input, Config config, Writer writer) {
        TransformerHandler identity = TransformerUtils.getIdentityTransformerHandler();
        if (config.publicDoctype != null && config.systemDoctype == null) throw new OXFException("XML Serializer must have a system doctype if a public doctype is present");
        TransformerUtils.applyOutputProperties(identity.getTransformer(), config.method != null ? config.method : DEFAULT_METHOD, config.version != null ? config.version : DEFAULT_VERSION, config.publicDoctype != null ? config.publicDoctype : null, config.systemDoctype != null ? config.systemDoctype : null, getEncoding(config, null, DEFAULT_ENCODING), config.omitXMLDeclaration, config.standalone, config.indent, config.indentAmount);
        identity.setResult(new StreamResult(writer));
        ProcessorImpl.readInputAsSAX(context, input, new SerializerContentHandler(identity, writer, isSerializeXML11()));
    }
}
