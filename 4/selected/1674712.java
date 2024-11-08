package com.jawise.serviceadapter.convert.soap.binding;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.apache.log4j.Logger;
import org.codehaus.xfire.XFireRuntimeException;
import org.codehaus.xfire.util.ClassLoaderUtils;
import org.codehaus.xfire.util.stax.DepthXMLStreamReader;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import com.jawise.serviceadapter.convert.MessageContext;

public class SoapXmlUtil {

    private static final String XML_NS = "http://www.w3.org/2000/xmlns/";

    private static Logger logger = Logger.getLogger(SoapXmlUtil.class);

    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    private static final XMLOutputFactory xmlOututFactory = XMLOutputFactory.newInstance();

    private static boolean inFactoryConfigured;

    @SuppressWarnings("unchecked")
    private static final Map factories = new HashMap();

    /**
	 * @param in
	 * @param encoding
	 * @param ctx
	 * @return
	 * @throws Exception
	 */
    public static XMLStreamReader createXMLStreamReader(InputStream in, String encoding, MessageContext ctx) throws Exception {
        XMLInputFactory factory = getXMLInputFactory(ctx);
        if (encoding == null) encoding = "UTF-8";
        try {
            return factory.createXMLStreamReader(in, encoding);
        } catch (XMLStreamException e) {
            throw e;
        }
    }

    public static XMLInputFactory getXMLInputFactory(MessageContext ctx) throws Exception {
        if (ctx == null) return xmlInputFactory;
        Object inFactoryObj = ctx.get("inputfactory");
        if (inFactoryObj instanceof XMLInputFactory) {
            return (XMLInputFactory) inFactoryObj;
        } else if (inFactoryObj instanceof String) {
            String inFactory = (String) inFactoryObj;
            XMLInputFactory xif = (XMLInputFactory) factories.get(inFactory);
            if (xif == null) {
                xif = (XMLInputFactory) createFactory(inFactory, ctx);
                configureFactory(xif, ctx);
                factories.put(inFactory, xif);
            }
            return xif;
        }
        if (!inFactoryConfigured) {
            configureFactory(xmlInputFactory, ctx);
            inFactoryConfigured = true;
        }
        return xmlInputFactory;
    }

    private static void configureFactory(XMLInputFactory xif, MessageContext ctx) {
        Boolean value = getBooleanProperty(ctx, XMLInputFactory.IS_VALIDATING);
        if (value != null) {
            xif.setProperty(XMLInputFactory.IS_VALIDATING, value);
        }
        value = getBooleanProperty(ctx, XMLInputFactory.IS_NAMESPACE_AWARE);
        if (value != null) {
            xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, value);
        }
        value = getBooleanProperty(ctx, XMLInputFactory.IS_COALESCING);
        if (value != null) {
            xif.setProperty(XMLInputFactory.IS_COALESCING, value);
        }
        value = getBooleanProperty(ctx, XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES);
        if (value != null) {
            xif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, value);
        }
        value = getBooleanProperty(ctx, XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES);
        if (value != null) {
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, value);
        }
    }

    /**
	 * @return
	 * @throws Exception
	 */
    @SuppressWarnings("unchecked")
    public static XMLOutputFactory getXMLOutputFactory(MessageContext ctx) throws Exception {
        if (ctx == null) return xmlOututFactory;
        Object outFactoryObj = ctx.get("outputfactory");
        if (outFactoryObj instanceof XMLOutputFactory) {
            return (XMLOutputFactory) outFactoryObj;
        } else if (outFactoryObj instanceof String) {
            String outFactory = (String) outFactoryObj;
            XMLOutputFactory xof = (XMLOutputFactory) factories.get(outFactory);
            if (xof == null) {
                xof = (XMLOutputFactory) createFactory(outFactory, ctx);
                factories.put(outFactory, xof);
            }
            return xof;
        }
        return xmlOututFactory;
    }

    @SuppressWarnings("unchecked")
    private static Object createFactory(String factory, MessageContext ctx) throws Exception {
        Class factoryClass = null;
        try {
            factoryClass = ClassLoaderUtils.loadClass(factory, ctx.getClass());
            return factoryClass.newInstance();
        } catch (Exception e) {
            logger.error("Can't create factory for class : " + factory, e);
            throw e;
        }
    }

    private static Boolean getBooleanProperty(MessageContext ctx, String name) {
        Object value = ctx.get(name);
        if (value != null) {
            return Boolean.valueOf(value.toString());
        }
        return null;
    }

    public static boolean toNextElement(DepthXMLStreamReader dr) throws XMLStreamException {
        if (dr.getEventType() == XMLStreamReader.START_ELEMENT) return true;
        if (dr.getEventType() == XMLStreamReader.END_ELEMENT) return false;
        int depth = dr.getDepth();
        for (int event = dr.getEventType(); dr.getDepth() >= depth && dr.hasNext(); event = dr.next()) {
            if (event == XMLStreamReader.START_ELEMENT && dr.getDepth() == depth + 1) {
                return true;
            } else if (event == XMLStreamReader.END_ELEMENT) {
                depth--;
            }
        }
        return false;
    }

    public static Document read(DocumentBuilder builder, XMLStreamReader reader, boolean repairing) throws XMLStreamException {
        Document doc = builder.newDocument();
        readDocElements(doc, reader, repairing);
        return doc;
    }

    private static Document getDocument(Node parent) {
        return (parent instanceof Document) ? (Document) parent : parent.getOwnerDocument();
    }

    private static Element startElement(Node parent, XMLStreamReader reader, boolean repairing) throws XMLStreamException {
        Document doc = getDocument(parent);
        Element e = doc.createElementNS(reader.getNamespaceURI(), reader.getLocalName());
        if (reader.getPrefix() != null && reader.getPrefix() != "") {
            e.setPrefix(reader.getPrefix());
        }
        parent.appendChild(e);
        for (int ns = 0; ns < reader.getNamespaceCount(); ns++) {
            String uri = reader.getNamespaceURI(ns);
            String prefix = reader.getNamespacePrefix(ns);
            declare(e, uri, prefix);
        }
        for (int att = 0; att < reader.getAttributeCount(); att++) {
            String name = reader.getAttributeLocalName(att);
            String prefix = reader.getAttributePrefix(att);
            if (prefix != null && prefix.length() > 0) name = prefix + ":" + name;
            Attr attr = doc.createAttributeNS(reader.getAttributeNamespace(att), name);
            attr.setValue(reader.getAttributeValue(att));
            e.setAttributeNode(attr);
        }
        reader.next();
        readDocElements(e, reader, repairing);
        if (repairing && !isDeclared(e, reader.getNamespaceURI(), reader.getPrefix())) {
            declare(e, reader.getNamespaceURI(), reader.getPrefix());
        }
        return e;
    }

    private static void declare(Element node, String uri, String prefix) {
        if (prefix != null && prefix.length() > 0) {
            node.setAttributeNS(XML_NS, "xmlns:" + prefix, uri);
        } else {
            if (uri != null) {
                node.setAttributeNS(XML_NS, "xmlns", uri);
            }
        }
    }

    private static boolean isDeclared(Element e, String namespaceURI, String prefix) {
        Attr att;
        if (prefix != null && prefix.length() > 0) {
            att = e.getAttributeNodeNS(XML_NS, "xmlns:" + prefix);
        } else {
            att = e.getAttributeNode("xmlns");
        }
        if (att != null && att.getNodeValue().equals(namespaceURI)) return true;
        if (e.getParentNode() instanceof Element) return isDeclared((Element) e.getParentNode(), namespaceURI, prefix);
        return false;
    }

    public static void readDocElements(Node parent, XMLStreamReader reader, boolean repairing) throws XMLStreamException {
        Document doc = getDocument(parent);
        int event = reader.getEventType();
        while (reader.hasNext()) {
            switch(event) {
                case XMLStreamConstants.START_ELEMENT:
                    startElement(parent, reader, repairing);
                    if (parent instanceof Document) {
                        if (reader.hasNext()) reader.next();
                        return;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    return;
                case XMLStreamConstants.NAMESPACE:
                    break;
                case XMLStreamConstants.ATTRIBUTE:
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if (parent != null) {
                        parent.appendChild(doc.createTextNode(reader.getText()));
                    }
                    break;
                case XMLStreamConstants.COMMENT:
                    if (parent != null) {
                        parent.appendChild(doc.createComment(reader.getText()));
                    }
                    break;
                case XMLStreamConstants.CDATA:
                    parent.appendChild(doc.createCDATASection(reader.getText()));
                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    parent.appendChild(doc.createProcessingInstruction(reader.getPITarget(), reader.getPIData()));
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE:
                    parent.appendChild(doc.createProcessingInstruction(reader.getPITarget(), reader.getPIData()));
                    break;
                default:
                    break;
            }
            if (reader.hasNext()) {
                event = reader.next();
            }
        }
    }

    public static XMLStreamWriter createXMLStreamWriter(OutputStream out, String encoding, MessageContext ctx) throws Exception {
        XMLOutputFactory factory = getXMLOutputFactory(ctx);
        if (encoding == null) encoding = "UTF-8";
        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(out, encoding);
            return writer;
        } catch (XMLStreamException e) {
            throw new XFireRuntimeException("Couldn't parse stream.", e);
        }
    }

    /**
	 * Copies the reader to the writer. The start and end document methods must
	 * be handled on the writer manually.
	 * 
	 * TODO: if the namespace on the reader has been declared previously to
	 * where we are in the stream, this probably won't work.
	 * 
	 * @param reader
	 * @param writer
	 * @throws XMLStreamException
	 */
    public static void copy(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        int read = 0;
        int event = reader.getEventType();
        while (reader.hasNext()) {
            switch(event) {
                case XMLStreamConstants.START_ELEMENT:
                    read++;
                    writeStartElement(reader, writer);
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    writer.writeEndElement();
                    read--;
                    if (read <= 0) return;
                    break;
                case XMLStreamConstants.CHARACTERS:
                    writer.writeCharacters(reader.getText());
                    break;
                case XMLStreamConstants.START_DOCUMENT:
                case XMLStreamConstants.END_DOCUMENT:
                case XMLStreamConstants.ATTRIBUTE:
                case XMLStreamConstants.NAMESPACE:
                    break;
                case XMLStreamConstants.CDATA:
                    writer.writeCData(reader.getText());
                    break;
                default:
                    break;
            }
            event = reader.next();
        }
    }

    private static void writeStartElement(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        String local = reader.getLocalName();
        String uri = reader.getNamespaceURI();
        String prefix = reader.getPrefix();
        if (prefix == null) {
            prefix = "";
        }
        String boundPrefix = writer.getPrefix(uri);
        boolean writeElementNS = false;
        if (boundPrefix == null || !prefix.equals(boundPrefix)) {
            writeElementNS = true;
        }
        if (uri != null && uri.length() > 0) {
            if (prefix.length() == 0) {
                writer.writeStartElement(local);
                writer.setDefaultNamespace(uri);
            } else {
                writer.writeStartElement(prefix, local, uri);
                writer.setPrefix(prefix, uri);
            }
        } else {
            writer.writeStartElement(reader.getLocalName());
        }
        for (int i = 0; i < reader.getNamespaceCount(); i++) {
            String nsURI = reader.getNamespaceURI(i);
            String nsPrefix = reader.getNamespacePrefix(i);
            if (nsURI == null) nsURI = "";
            if (nsPrefix == null) nsPrefix = "";
            if (nsPrefix.length() == 0) {
                writer.writeDefaultNamespace(nsURI);
            } else {
                writer.writeNamespace(nsPrefix, nsURI);
            }
            if (uri != null && nsURI.equals(uri) && nsPrefix.equals(prefix)) {
                writeElementNS = false;
            }
        }
        if (writeElementNS && uri != null) {
            if (prefix == null || prefix.length() == 0) {
                writer.writeDefaultNamespace(uri);
            } else {
                writer.writeNamespace(prefix, uri);
            }
        }
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String ns = reader.getAttributeNamespace(i);
            String nsPrefix = reader.getAttributePrefix(i);
            if (ns == null || ns.length() == 0) {
                writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            } else if (nsPrefix == null || nsPrefix.length() == 0) {
                writer.writeAttribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            } else {
                writer.writeAttribute(reader.getAttributePrefix(i), reader.getAttributeNamespace(i), reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            }
        }
    }

    /**
	 * Writes an Element to an XMLStreamWriter. The writer must already have
	 * started the doucment (via writeStartDocument()). Also, this probably
	 * won't work with just a fragment of a document. The Element should be the
	 * root element of the document.
	 * 
	 * @param e
	 * @param writer
	 * @throws XMLStreamException
	 */
    public static void writeElement(Element e, XMLStreamWriter writer, boolean repairing) throws XMLStreamException {
        String prefix = e.getPrefix();
        String ns = e.getNamespaceURI();
        String localName = e.getLocalName();
        if (prefix == null) prefix = "";
        if (localName == null) {
            localName = e.getNodeName();
            if (localName == null) throw new IllegalStateException("Element's local name cannot be null!");
        }
        String decUri = null;
        NamespaceContext ctxt = writer.getNamespaceContext();
        if (ctxt != null) {
            decUri = ctxt.getNamespaceURI(prefix);
        }
        boolean declareNamespace = (decUri == null || !decUri.equals(ns));
        if (ns == null || ns.length() == 0) {
            writer.writeStartElement(localName);
        } else {
            writer.writeStartElement(prefix, localName, ns);
        }
        NamedNodeMap attrs = e.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            String name = attr.getNodeName();
            String attrPrefix = "";
            int prefixIndex = name.indexOf(':');
            if (prefixIndex != -1) {
                attrPrefix = name.substring(0, prefixIndex);
                name = name.substring(prefixIndex + 1);
            }
            if (attrPrefix.equals("xmlns")) {
                writer.writeNamespace(name, attr.getNodeValue());
                if (name.equals(prefix) && attr.getNodeValue().equals(ns)) {
                    declareNamespace = false;
                }
            } else {
                if (name.equals("xmlns") && attrPrefix.equals("")) {
                    writer.writeNamespace("", attr.getNodeValue());
                    if (attr.getNodeValue().equals(ns)) {
                        declareNamespace = false;
                    }
                } else {
                    writer.writeAttribute(attrPrefix, attr.getNamespaceURI(), name, attr.getNodeValue());
                }
            }
        }
        if (declareNamespace && repairing) {
            writer.writeNamespace(prefix, ns);
        }
        NodeList nodes = e.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            writeNode(n, writer, repairing);
        }
        writer.writeEndElement();
    }

    public static void writeNode(Node n, XMLStreamWriter writer, boolean repairing) throws XMLStreamException {
        if (n instanceof Element) {
            writeElement((Element) n, writer, repairing);
        } else if (n instanceof Text) {
            writer.writeCharacters(((Text) n).getNodeValue());
        } else if (n instanceof CDATASection) {
            writer.writeCData(((CDATASection) n).getData());
        } else if (n instanceof Comment) {
            writer.writeComment(((Comment) n).getData());
        } else if (n instanceof EntityReference) {
            writer.writeEntityRef(((EntityReference) n).getNodeValue());
        } else if (n instanceof ProcessingInstruction) {
            ProcessingInstruction pi = (ProcessingInstruction) n;
            writer.writeProcessingInstruction(pi.getTarget(), pi.getData());
        }
    }
}
