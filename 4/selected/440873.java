package org.soda.dpws.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.soda.dpws.DPWSRuntimeException;
import org.soda.dpws.internal.DPWS;
import org.soda.dpws.internal.DPWSContextImpl;
import org.soda.dpws.util.parser.stax.InputFactory;
import org.soda.dpws.util.parser.stax.OutputFactory;
import org.soda.dpws.util.stax.DepthXMLStreamReader;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Common StAX utilities.
 * 
 */
public class STAXUtils {

    private static final String XML_NS = "http://www.w3.org/2000/xmlns/";

    private static final XMLInputFactory xmlInputFactory = new InputFactory();

    private static final XMLOutputFactory xmlOututFactory = new OutputFactory();

    private static final Map<String, Object> factories = new HashMap<String, Object>();

    public static boolean toNextElement(DepthXMLStreamReader dr) {
        if (dr.getEventType() == XMLStreamReader.START_ELEMENT) return true;
        if (dr.getEventType() == XMLStreamReader.END_ELEMENT) return false;
        try {
            int depth = dr.getDepth();
            for (int event = dr.getEventType(); dr.getDepth() >= depth && dr.hasNext(); event = dr.next()) {
                if (event == XMLStreamReader.START_ELEMENT && dr.getDepth() == depth + 1) {
                    return true;
                } else if (event == XMLStreamReader.END_ELEMENT) {
                    depth--;
                }
            }
            return false;
        } catch (XMLStreamException e) {
            throw new DPWSRuntimeException("Couldn't parse stream.", e);
        }
    }

    /**
   * Copies the reader to the writer. The start and end document methods must be
   * handled on the writer manually.
   * 
   * TODO: if the namespace on the reader has been declared previously to where
   * we are in the stream, this probably won't work.
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
                case XMLStreamConstants.END_DOCUMENT:
                case XMLStreamConstants.START_DOCUMENT:
                case XMLStreamConstants.ATTRIBUTE:
                case XMLStreamConstants.NAMESPACE:
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
        if (uri != null) {
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
            if (nsPrefix == null) nsPrefix = "";
            if (nsPrefix.length() == 0) {
                writer.writeDefaultNamespace(nsURI);
            } else {
                writer.writeNamespace(nsPrefix, nsURI);
            }
            if (nsURI.equals(uri) && nsPrefix.equals(prefix)) {
                writeElementNS = false;
            }
        }
        if (writeElementNS) {
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

    public static Document read(DocumentBuilder builder, XMLStreamReader reader, boolean repairing) throws XMLStreamException {
        Document doc = builder.newDocument();
        readDocElements(doc, reader, repairing);
        return doc;
    }

    /**
   * @param parent
   * @return
   */
    private static Document getDocument(Node parent) {
        return (parent instanceof Document) ? (Document) parent : parent.getOwnerDocument();
    }

    /**
   * @param parent
   * @param reader
   * @return
   * @throws XMLStreamException
   */
    private static Element startElement(Node parent, XMLStreamReader reader, boolean repairing) throws XMLStreamException {
        Document doc = getDocument(parent);
        Element e = doc.createElementNS(reader.getNamespaceURI(), reader.getLocalName());
        if (reader.getPrefix() != null) e.setPrefix(reader.getPrefix());
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

    /**
   * @param parent
   * @param reader
   * @param repairing
   * @throws XMLStreamException
   */
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
                    if (parent != null) parent.appendChild(doc.createCDATASection(reader.getText()));
                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    if (parent != null) parent.appendChild(doc.createProcessingInstruction(reader.getPITarget(), reader.getPIData()));
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE:
                    if (parent != null) parent.appendChild(doc.createProcessingInstruction(reader.getPITarget(), reader.getPIData()));
                    break;
                default:
                    break;
            }
            if (reader.hasNext()) {
                event = reader.next();
            }
        }
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

    /**
   * @param out
   * @param encoding
   * @param ctx
   * @return the {@link XMLStreamWriter}
   */
    public static XMLStreamWriter createXMLStreamWriter(OutputStream out, String encoding, DPWSContextImpl ctx) {
        XMLOutputFactory factory = getXMLOutputFactory(ctx);
        if (encoding == null) encoding = "UTF-8";
        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(out, encoding);
            return writer;
        } catch (XMLStreamException e) {
            throw new DPWSRuntimeException("Couldn't parse stream.", e);
        }
    }

    /**
   * @param ctx
   * @return the {@link XMLOutputFactory}
   */
    public static XMLOutputFactory getXMLOutputFactory(DPWSContextImpl ctx) {
        if (ctx == null) return xmlOututFactory;
        String outFactory = (String) ctx.getContextualProperty(DPWS.STAX_OUTPUT_FACTORY);
        if (outFactory != null) {
            XMLOutputFactory xof = (XMLOutputFactory) factories.get(outFactory);
            if (xof == null) {
                xof = (XMLOutputFactory) createFactory(outFactory, ctx);
                factories.put(outFactory, xof);
            }
            return xof;
        }
        return xmlOututFactory;
    }

    /**
   * @param ctx
   * @return the {@link XMLInputFactory}
   */
    public static XMLInputFactory getXMLInputFactory(DPWSContextImpl ctx) {
        if (ctx == null) return xmlInputFactory;
        String inFactory = (String) ctx.getContextualProperty(DPWS.STAX_INPUT_FACTORY);
        if (inFactory != null) {
            XMLInputFactory xif = (XMLInputFactory) factories.get(inFactory);
            if (xif == null) {
                xif = (XMLInputFactory) createFactory(inFactory, ctx);
                factories.put(inFactory, xif);
            }
            return xif;
        }
        return xmlInputFactory;
    }

    /**
   * @param factoryClass
   * @return
   */
    private static Object createFactory(String factory, DPWSContextImpl ctx) {
        Class<?> factoryClass = null;
        try {
            factoryClass = ClassLoaderUtils.loadClass(factory, ctx.getClass());
            return factoryClass.newInstance();
        } catch (Exception e) {
            throw new DPWSRuntimeException("Can't create factory for class : " + factory);
        }
    }

    /**
   * @param in
   * @param encoding
   * @param ctx
   * @return the {@link XMLStreamReader}
   */
    public static XMLStreamReader createXMLStreamReader(InputStream in, String encoding, DPWSContextImpl ctx) {
        XMLInputFactory factory = getXMLInputFactory(ctx);
        if (encoding == null) encoding = "UTF-8";
        try {
            return factory.createXMLStreamReader(in, encoding);
        } catch (XMLStreamException e) {
            throw new DPWSRuntimeException("Couldn't parse stream.", e);
        }
    }

    /**
   * @param reader
   * @return the {@link XMLStreamReader}
   */
    public static XMLStreamReader createXMLStreamReader(Reader reader) {
        return createXMLStreamReader(reader, null);
    }

    /**
   * @param reader
   * @param context
   * @return the {@link XMLStreamReader}
   */
    public static XMLStreamReader createXMLStreamReader(Reader reader, DPWSContextImpl context) {
        XMLInputFactory factory = getXMLInputFactory(context);
        try {
            return factory.createXMLStreamReader(reader);
        } catch (XMLStreamException e) {
            throw new DPWSRuntimeException("Couldn't parse stream.", e);
        }
    }
}
