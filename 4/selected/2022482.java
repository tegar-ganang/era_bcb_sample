package net.rootnode.loomchild.util.xml;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import net.rootnode.loomchild.util.exceptions.IORuntimeException;
import net.rootnode.loomchild.util.exceptions.XmlException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class Util {

    public static XMLReader getXmlReader(Schema schema) {
        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setValidating(false);
            parserFactory.setNamespaceAware(true);
            if (schema != null) {
                parserFactory.setSchema(schema);
            }
            SAXParser saxParser = parserFactory.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setEntityResolver(new IgnoreDTDEntityResolver());
            return xmlReader;
        } catch (ParserConfigurationException e) {
            throw new XmlException("SAX Parser configuration error.", e);
        } catch (SAXException e) {
            throw new XmlException("Error creating XMLReader.", e);
        }
    }

    public static XMLReader getXmlReader() {
        return getXmlReader(null);
    }

    public static Schema getSchema(Reader reader) {
        return getSchema(new Reader[] { reader });
    }

    public static Schema getSchema(Reader[] readerArray) {
        try {
            Source[] sourceArray = new Source[readerArray.length];
            for (int i = 0; i < readerArray.length; ++i) {
                Reader reader = readerArray[i];
                Source source = new StreamSource(reader);
                sourceArray[i] = source;
            }
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(sourceArray);
            return schema;
        } catch (SAXException e) {
            throw new XmlException("Error creating XML Schema.", e);
        }
    }

    public static Source getSource(Reader reader, Schema schema) {
        Source source = new SAXSource(getXmlReader(schema), new InputSource(reader));
        return source;
    }

    public static JAXBContext getContext(String context) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(context);
            return jaxbContext;
        } catch (JAXBException e) {
            throw new XmlException("Error creating JAXB context", e);
        }
    }

    public static JAXBContext getContext(String context, ClassLoader classLoader) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(context, classLoader);
            return jaxbContext;
        } catch (JAXBException e) {
            throw new XmlException("Error creating JAXB context", e);
        }
    }

    public static JAXBContext getContext(Class<?>... classesToBeBound) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(classesToBeBound);
            return jaxbContext;
        } catch (JAXBException e) {
            throw new XmlException("Error creating JAXB context", e);
        }
    }

    public static Templates getTemplates(Reader reader) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Source source = new StreamSource(reader);
            Templates templates;
            templates = factory.newTemplates(source);
            return templates;
        } catch (TransformerConfigurationException e) {
            throw new XmlException("Error creating XSLT templates.", e);
        }
    }

    public static void transform(Templates templates, Schema schema, Reader reader, Writer writer, Map<String, Object> parameterMap) {
        try {
            Source source = getSource(reader, schema);
            Result result = new StreamResult(writer);
            Transformer transformer = templates.newTransformer();
            transformer.setErrorListener(new TransformationErrorListener());
            for (Map.Entry<String, Object> entry : parameterMap.entrySet()) {
                transformer.setParameter(entry.getKey(), entry.getValue());
            }
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            throw new XmlException("Error creating XSLT transformer.", e);
        } catch (TransformerException e) {
            throw new XmlException("XSLT transformer error.", e);
        }
    }

    public static void transform(Templates templates, Reader reader, Writer writer, Map<String, Object> parameterMap) {
        transform(templates, null, reader, writer, parameterMap);
    }

    public static void transform(Templates templates, Schema schema, Reader reader, Writer writer) {
        Map<String, Object> parameterMap = Collections.emptyMap();
        transform(templates, schema, reader, writer, parameterMap);
    }

    public static void transform(Templates templates, Reader reader, Writer writer) {
        transform(templates, null, reader, writer);
    }

    public static void validate(Schema schema, Reader reader) {
        try {
            Source source = new StreamSource(reader);
            Validator validator = schema.newValidator();
            validator.validate(source);
        } catch (SAXException e) {
            throw new XmlException("Validation error.", e);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }
}
