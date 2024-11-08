package com.legstar.xsd.def;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.w3c.dom.Document;
import com.legstar.cobol.gen.CopybookGenerator;
import com.legstar.cobol.model.CobolDataItem;
import com.legstar.dom.DocumentFactory;
import com.legstar.dom.InvalidDocumentException;
import com.legstar.xsd.InvalidXsdException;
import com.legstar.xsd.XsdMappingException;
import com.legstar.xsd.XsdNavigator;
import com.legstar.xsd.XsdReader;
import com.legstar.xsd.XsdRootElement;
import com.legstar.xsd.XsdToCobolStringResult;
import com.legstar.xsd.cob.Xsd2CobMapper;

/**
 * XSD to COBOL Translator API.
 * <p/>
 * Takes raw XML Schema of XML Schema imbedded in a WSDL and turns the content
 * into 2 outputs:
 * <ul>
 * <li>An XML Schema with COBOL annotations</li>
 * <li>COBOL structures descriptions mapping the XML schema elements</li>
 * </ul>
 * 
 */
public class Xsd2Cob {

    /** Configuration data. */
    private Xsd2CobModel _model;

    /** Logger. */
    private final Log _log = LogFactory.getLog(getClass());

    /**
     * Construct the translator.
     */
    public Xsd2Cob() {
        this(null);
    }

    /**
     * Construct the translator.
     * 
     * @param model the configuration data
     */
    public Xsd2Cob(final Xsd2CobModel model) {
        if (model == null) {
            _model = new Xsd2CobModel();
        } else {
            _model = model;
        }
    }

    /**
     * Execute the translation from XML schema to COBOL-annotated XML Schema and
     * COBOL structure.
     * 
     * @return the XML Schema and the COBOL structure as strings
     */
    public XsdToCobolStringResult translate() throws InvalidXsdException {
        return translate(parse(getModel().getInputXsdUri()));
    }

    /**
     * Execute the translation from XML schema to COBOL-annotated XML Schema and
     * COBOL structure.
     * 
     * @param uri the XML schema URI
     * @return the XML Schema and the COBOL structure as strings
     */
    public XsdToCobolStringResult translate(final URI uri) throws InvalidXsdException {
        return translate(parse(uri));
    }

    /**
     * Execute the translation from XML schema to COBOL-annotated XML Schema and
     * COBOL structure.
     * <p/>
     * Convenience methods when input is a string.
     * 
     * @param xml the XML schema source
     * @return the XML Schema and the COBOL structure as strings
     */
    public XsdToCobolStringResult translate(final String xml) throws InvalidXsdException {
        return translate(parse(xml));
    }

    /**
     * Execute the translation from XML schema to COBOL-annotated XML Schema and
     * COBOL structure.
     * 
     * @param schema the XML schema
     * @return the XML Schema and the COBOL structure as strings
     */
    public XsdToCobolStringResult translate(final XmlSchema schema) throws InvalidXsdException {
        return translate(schema, getModel().getNewRootElements(), null);
    }

    /**
     * Execute the translation from XML schema to COBOL-annotated XML Schema and
     * COBOL structure.
     * 
     * @param schema the XML schema
     * @param newRootElements a set of new elements to add to the XML schema
     * @param a map of complex types to java classes used to annotate complex
     *            types when they originate from a java class
     * @return the XML Schema and the COBOL structure as strings
     */
    public XsdToCobolStringResult translate(final XmlSchema schema, List<XsdRootElement> newRootElements, Map<String, String> complexTypeToJavaClassMap) throws InvalidXsdException {
        if (_log.isDebugEnabled()) {
            _log.debug("Translating with options:" + getModel().toString());
        }
        try {
            XmlSchema resultSchema = schema;
            if (getModel().getNewTargetNamespace() != null && !getModel().getNewTargetNamespace().equals(resultSchema.getTargetNamespace())) {
                resultSchema = XsdReader.switchTargetNamespace(resultSchema, getModel().getNewTargetNamespace());
            }
            if (newRootElements != null) {
                XsdReader.addRootElements(newRootElements, resultSchema);
            }
            Xsd2CobAnnotator annotator = new Xsd2CobAnnotator(getModel().getXsdConfig(), complexTypeToJavaClassMap);
            annotator.setUp();
            XsdNavigator visitor = new XsdNavigator(resultSchema, annotator);
            visitor.visit();
            if (getModel().getCustomXsltFileName() != null) {
                resultSchema = customize(resultSchema, getModel().getCustomXsltFileName());
            }
            Xsd2CobMapper mapper = new Xsd2CobMapper();
            visitor = new XsdNavigator(resultSchema, mapper);
            visitor.visit();
            StringBuilder copyBook = new StringBuilder();
            for (CobolDataItem cobolDataItem : mapper.getRootDataItems()) {
                copyBook.append(CopybookGenerator.generate(cobolDataItem));
            }
            return new XsdToCobolStringResult(toString(resultSchema), copyBook.toString());
        } catch (IOException e) {
            throw new InvalidXsdException(e);
        } catch (XsdMappingException e) {
            throw new InvalidXsdException(e);
        } catch (TransformerException e) {
            throw new InvalidXsdException(e);
        }
    }

    /**
     * Parse a file and generate an XML Schema.
     * 
     * @param xml the input XML content (an XSD or WSDL)
     * @return an XML schema
     * @throws InvalidXsdException if something fails
     */
    protected XmlSchema parse(final String xml) throws InvalidXsdException {
        try {
            Document doc = DocumentFactory.parse(xml);
            return XsdReader.read(doc);
        } catch (InvalidDocumentException e) {
            throw new InvalidXsdException(e);
        }
    }

    /**
     * Parse a file and generate an XML Schema.
     * 
     * @param uri the input XML URI
     * @return an XML schema
     * @throws InvalidXsdException if something fails
     */
    protected XmlSchema parse(final URI uri) throws InvalidXsdException {
        try {
            Document doc = DocumentFactory.parse(uri);
            return XsdReader.read(doc);
        } catch (InvalidDocumentException e) {
            throw new InvalidXsdException(e);
        }
    }

    /**
     * Prints an XML schema in a String.
     * 
     * @param schema the XML schema
     * @return the content as a string
     * @throws TransformerException if formatting of output fails
     */
    protected String toString(final XmlSchema schema) throws TransformerException {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        try {
            tFactory.setAttribute("indent-number", "4");
        } catch (IllegalArgumentException e) {
            _log.warn("Unable to set indent-number on transfomer factory", e);
        }
        StringWriter writer = new StringWriter();
        Source source = new DOMSource(schema.getAllSchemas()[0]);
        Result result = new StreamResult(writer);
        Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        transformer.transform(source, result);
        writer.flush();
        return writer.toString();
    }

    /**
     * Customize the XML schema by applying an XSLT stylesheet.
     * 
     * @param schema the XML schema
     * @param xsltFileName the XSLT stylesheet
     * @return the transformed XML schema as a string
     * @throws TransformerException if XSLT transform fails
     */
    protected XmlSchema customize(final XmlSchema schema, final String xsltFileName) throws TransformerException {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        StringWriter writer = new StringWriter();
        Source source = new DOMSource(schema.getAllSchemas()[0]);
        Result result = new StreamResult(writer);
        Source xsltSource = new StreamSource(new File(xsltFileName));
        Transformer transformer = tFactory.newTransformer(xsltSource);
        transformer.transform(source, result);
        writer.flush();
        StringReader reader = new StringReader(writer.toString());
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        return schemaCol.read(reader, null);
    }

    /**
     * @return the configuration data
     */
    public Xsd2CobModel getModel() {
        return _model;
    }
}
