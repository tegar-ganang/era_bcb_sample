package com.knitml.validation;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.commons.io.IOUtils;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.knitml.core.common.Parameters;
import com.knitml.core.model.Pattern;
import com.knitml.core.xml.EntityResolverWrapper;
import com.knitml.core.xml.PluggableSchemaResolver;
import com.knitml.core.xml.Schemas;
import com.knitml.engine.common.KnittingEngineException;
import com.knitml.validation.context.KnittingContext;
import com.knitml.validation.context.KnittingContextFactory;
import com.knitml.validation.visitor.instruction.Visitor;
import com.knitml.validation.visitor.instruction.VisitorFactory;

public class ValidationProgram {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(ValidationProgram.class);

    private KnittingContextFactory contextFactory;

    private VisitorFactory visitorFactory;

    public ValidationProgram(KnittingContextFactory ctxFactory, VisitorFactory visitorFactory) {
        this.contextFactory = ctxFactory;
        this.visitorFactory = visitorFactory;
    }

    public Pattern validate(Parameters parameters) throws SAXException, JiBXException, IOException, KnittingEngineException {
        KnittingContext context = contextFactory.createKnittingContext();
        Pattern pattern = parameters.getPattern();
        Reader reader = parameters.getReader();
        if (pattern == null && reader == null) {
            throw new IllegalArgumentException("One of pattern or reader must be specified " + "in the Parameters object");
        }
        if (pattern == null) {
            if (parameters.isCheckSyntax()) {
                StringWriter writer = new StringWriter();
                IOUtils.copy(reader, writer);
                reader.close();
                writer.close();
                Source source = new StreamSource(new StringReader(writer.toString()));
                checkSyntax(source);
                reader = new StringReader(writer.toString());
            }
            IBindingFactory factory = BindingDirectory.getFactory(Pattern.class);
            IUnmarshallingContext uctx = factory.createUnmarshallingContext();
            pattern = (Pattern) uctx.unmarshalDocument(reader);
        }
        Visitor visitor = visitorFactory.findVisitorFromClassName(pattern);
        visitor.visit(pattern, context);
        Writer writer = parameters.getWriter();
        if (writer != null) {
            try {
                IBindingFactory factory = BindingDirectory.getFactory(Pattern.class);
                IMarshallingContext mctx = factory.createMarshallingContext();
                mctx.setOutput(writer);
                mctx.getXmlWriter().setIndentSpaces(2, null, ' ');
                mctx.getXmlWriter().writeXMLDecl("1.0", "UTF-8", null);
                mctx.marshalDocument(pattern);
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }
        return pattern;
    }

    protected void checkSyntax(Source source) throws IOException, SAXException {
        EntityResolver entityResolver = new PluggableSchemaResolver(this.getClass().getClassLoader());
        LSResourceResolver resourceResolver = new EntityResolverWrapper(entityResolver);
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setResourceResolver(resourceResolver);
        InputSource patternSchema = entityResolver.resolveEntity(null, Schemas.CURRENT_PATTERN_SCHEMA);
        Source patternSchemaSource = new StreamSource(patternSchema.getByteStream());
        Schema schema = factory.newSchema(patternSchemaSource);
        Validator validator = schema.newValidator();
        validator.validate(source);
    }
}
