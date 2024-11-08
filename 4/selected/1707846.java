package org.nexopenframework.xml.stream;

import java.io.OutputStream;
import javanet.staxutils.XMLStreamUtils;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.nexopenframework.xml.XmlConstants;
import org.nexopenframework.xml.XmlException;
import org.springframework.util.Assert;
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

/**
 * <p>NexTReT Open Framework</p>
 * 
 * <p>Common StAX utilities</p>
 * 
 * @author <a href="mailto:fme@nextret.net">Francesc Xavier Magdaleno</a>
 * @version 1.0
 * @since 1.0
 */
public abstract class StAXUtils {

    protected StAXUtils() {
    }

    /**
	 * <p></p>
	 * 
	 * @param reader
	 * @param writer
	 */
    public static void copy(XMLStreamReader reader, XMLStreamWriter writer) {
        {
            Assert.notNull(reader, "XMLStreamReader MUST be different from NULL");
            Assert.notNull(writer, "XMLStreamWriter MUST be different from NULL");
        }
        try {
            XMLStreamUtils.copy(reader, writer);
        } catch (XMLStreamException e) {
            throw new XmlException(e);
        }
    }

    /**
	 * <p>Creates a {@link XMLOutputFactory} without reparing namespaces</p>
	 * 
	 * @see #newXMLOutputFactory(boolean)
	 * @return
	 */
    public static XMLOutputFactory newXMLOutputFactory() {
        return newXMLOutputFactory(false);
    }

    /**
	 * <p></p>
	 * 
	 * @param reparingNamespace
	 * @return
	 */
    public static XMLOutputFactory newXMLOutputFactory(boolean reparingNamespace) {
        XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
        outFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, (reparingNamespace) ? Boolean.TRUE : Boolean.FALSE);
        return outFactory;
    }

    /**
	 * <p></p>
	 * 
	 * <p>WANING: Carefully used in a concurrent environment due the fact that always 
	 *    create a {@link XMLOutputFactory}</p>
	 * 
	 * @param out
	 * @return
	 */
    public static XMLStreamWriter createXMLStreamWriter(OutputStream out) {
        return createXMLStreamWriter(out, null, false);
    }

    /**
	 * <p>WANING: Carefully used in a concurrent environment due the fact that always 
	 *    create a {@link XMLOutputFactory}</p>
	 * @param out
	 * @return
	 */
    public static XMLStreamWriter createXMLStreamWriter(OutputStream out, String encoding) {
        return createXMLStreamWriter(out, encoding, false);
    }

    /**
     *  <p></p>
     * 
     *  <p>WANING: Carefully used in a concurrent environment due the fact that always 
	 *    create a {@link XMLOutputFactory}</p>
	 *    
     * @param out
     * @param encoding
     * @return
     */
    public static XMLStreamWriter createXMLStreamWriter(OutputStream out, String encoding, boolean reparingNamespace) {
        {
            Assert.notNull(out, "OutputStream MUST be different from NULL");
        }
        try {
            XMLOutputFactory outFactory = newXMLOutputFactory(reparingNamespace);
            XMLStreamWriter writer = (encoding != null) ? outFactory.createXMLStreamWriter(out, encoding) : outFactory.createXMLStreamWriter(out);
            return writer;
        } catch (XMLStreamException e) {
            throw new XmlException(e);
        }
    }

    /**
	 * <p>Returns true if currently at the start of an element, otherwise move forwards to the next
     * element start and return true, otherwise false is returned if the end of the stream is reached.</p>
	 * 
	 * @param in
	 * @return
	 * @throws XMLStreamException
	 */
    public static boolean skipToStartOfElement(XMLStreamReader in) throws XMLStreamException {
        for (int code = in.getEventType(); code != XMLStreamConstants.END_DOCUMENT; code = in.next()) {
            if (code == XMLStreamConstants.START_ELEMENT) {
                return true;
            }
        }
        return false;
    }

    /**
	 * @param element
	 * @param streamWriter
	 * @param repairing
	 * @throws XSMException 
	 */
    public static Document read(DocumentBuilder builder, XMLStreamReader reader, boolean repairing) {
        {
            Assert.notNull(builder, "DocumentBuilder MUST be different from NULL");
            Assert.notNull(reader, "XMLStreamReader MUST be different from NULL");
        }
        try {
            Document doc = builder.newDocument();
            readDocElements(doc, reader, repairing);
            return doc;
        } catch (XMLStreamException e) {
            throw new XmlException(e);
        }
    }

    /**
	 * <p>Writes a document to an {@link XMLStreamWriter}. This method uses
	 *    the {@link XMLStreamWriter#writeStartDocument()} and the 
	 *    {@link XMLStreamWriter#writeEndDocument()} 
	 *    and obtains the root element which passes to the writeElement 
	 *    method of this class</p>
	 * 
	 * @param doc
	 * @param writer
	 * @param repairing
	 * @throws XmlException if some Stream exception appears in the process
	 * @see {@link #writeElement(Element, XMLStreamWriter, boolean)}
	 */
    public static void writeDocument(Document doc, XMLStreamWriter writer, boolean repairing) {
        {
            Assert.notNull(doc, "Docuemnt MUST be different from NULL");
            Assert.notNull(writer, "XMLStreamWriter MUST be different from NULL");
        }
        try {
            writer.writeStartDocument();
            Element element = doc.getDocumentElement();
            writeElement(element, writer, repairing);
            writer.writeEndDocument();
        } catch (XMLStreamException e) {
            throw new XmlException(e);
        }
    }

    /**
     * <p>Writes an Element to an {@link XMLStreamWriter}.  The writer must already
     * have started the document (via writeStartDocument()). Also, this probably
     * won't work with just a fragment of a document. The Element should be
     * the root element of the document.</p>
     * 
     * @param e
     * @param writer
     * @throws XMLStreamException
     */
    public static void writeElement(Element e, XMLStreamWriter writer, boolean repairing) {
        try {
            String prefix = e.getPrefix();
            String ns = e.getNamespaceURI();
            String localName = e.getLocalName();
            if (prefix == null) {
                prefix = "";
            }
            if (localName == null) {
                throw new IllegalStateException("Element's local name cannot be null!");
            }
            String decUri = writer.getNamespaceContext().getNamespaceURI(prefix);
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
                    if (name.equals("xmlns") && attrPrefix.length() == 0) {
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
        } catch (XMLStreamException ex) {
            throw new XmlException(ex);
        }
    }

    /**
     * @param n
     * @param writer
     * @param repairing
     */
    public static void writeNode(Node n, XMLStreamWriter writer, boolean repairing) {
        try {
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
        } catch (XMLStreamException e) {
            throw new XmlException(e);
        }
    }

    /**
     * @param parent
     * @param reader
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
                        if (reader.hasNext()) {
                            reader.next();
                        }
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

    /**
     * @param parent
     * @return
     */
    protected static Document getDocument(Node parent) {
        return (parent instanceof Document) ? (Document) parent : parent.getOwnerDocument();
    }

    /**
     * @param parent
     * @param reader
     * @return
     * @throws XMLStreamException
     */
    protected static Element startElement(Node parent, XMLStreamReader reader, boolean repairing) throws XMLStreamException {
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
            att = e.getAttributeNodeNS(XmlConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + prefix);
        } else {
            att = e.getAttributeNode("xmlns");
        }
        if (att != null && att.getNodeValue().equals(namespaceURI)) return true;
        if (e.getParentNode() instanceof Element) return isDeclared((Element) e.getParentNode(), namespaceURI, prefix);
        return false;
    }

    private static void declare(Element node, String uri, String prefix) {
        if (prefix != null && prefix.length() > 0) {
            node.setAttributeNS(XmlConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + prefix, uri);
        } else {
            if (uri != null) {
                node.setAttributeNS(XmlConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", uri);
            }
        }
    }
}
