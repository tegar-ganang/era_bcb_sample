package org.orbeon.oxf.processor.serializer.legacy;

import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.ProcessorInput;
import org.orbeon.oxf.processor.serializer.HttpTextSerializer;
import org.orbeon.oxf.processor.serializer.SerializerContentHandler;
import org.orbeon.oxf.xml.TransformerUtils;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.Writer;

public class TextSerializer extends HttpTextSerializer {

    public static String DEFAULT_CONTENT_TYPE = "text/plain";

    public static String DEFAULT_METHOD = "text";

    protected String getDefaultContentType() {
        return DEFAULT_CONTENT_TYPE;
    }

    protected void readInput(PipelineContext context, ProcessorInput input, Config config, Writer writer) {
        TransformerHandler identity = TransformerUtils.getIdentityTransformerHandler();
        TransformerUtils.applyOutputProperties(identity.getTransformer(), config.method != null ? config.method : DEFAULT_METHOD, null, null, null, getEncoding(config, null, DEFAULT_ENCODING), true, null, false, DEFAULT_INDENT_AMOUNT);
        identity.setResult(new StreamResult(writer));
        readInputAsSAX(context, INPUT_DATA, new SerializerContentHandler(identity, writer, isSerializeXML11()));
    }
}
