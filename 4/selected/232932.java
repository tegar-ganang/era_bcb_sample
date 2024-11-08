package org.apache.axiom.om.impl.serialize;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttachmentAccessor;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMSerializer;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.MTOMXMLStreamWriter;
import org.apache.axiom.om.impl.util.OMSerializerUtil;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.activation.DataHandler;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;

/** Class StreamingOMSerializer */
public class StreamingOMSerializer implements XMLStreamConstants, OMSerializer {

    Log log = LogFactory.getLog(StreamingOMSerializer.class);

    private static int namespaceSuffix = 0;

    public static final String NAMESPACE_PREFIX = "ns";

    /** Field depth */
    private int depth = 0;

    public static final QName XOP_INCLUDE = new QName("http://www.w3.org/2004/08/xop/include", "Include");

    private boolean inputHasAttachments = false;

    private boolean skipEndElement = false;

    /**
     * Method serialize.
     *
     * @param node
     * @param writer
     * @throws XMLStreamException
     */
    public void serialize(XMLStreamReader node, XMLStreamWriter writer) throws XMLStreamException {
        serialize(node, writer, true);
    }

    /**
     * @param node
     * @param writer
     * @param startAtNext indicate if reading should start at next event or current event
     * @throws XMLStreamException
     */
    public void serialize(XMLStreamReader node, XMLStreamWriter writer, boolean startAtNext) throws XMLStreamException {
        if (node instanceof OMAttachmentAccessor) {
            inputHasAttachments = true;
        }
        serializeNode(node, writer, startAtNext);
    }

    /**
     * Method serializeNode.
     *
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    protected void serializeNode(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        serializeNode(reader, writer, true);
    }

    protected void serializeNode(XMLStreamReader reader, XMLStreamWriter writer, boolean startAtNext) throws XMLStreamException {
        boolean useCurrentEvent = !startAtNext;
        while (reader.hasNext() || useCurrentEvent) {
            int event = 0;
            if (useCurrentEvent) {
                event = reader.getEventType();
                useCurrentEvent = false;
            } else {
                event = reader.next();
            }
            if (event == START_ELEMENT) {
                serializeElement(reader, writer);
                depth++;
            } else if (event == ATTRIBUTE) {
                serializeAttributes(reader, writer);
            } else if (event == CHARACTERS) {
                serializeText(reader, writer);
            } else if (event == COMMENT) {
                serializeComment(reader, writer);
            } else if (event == CDATA) {
                serializeCData(reader, writer);
            } else if (event == END_ELEMENT) {
                serializeEndElement(writer);
                depth--;
            } else if (event == START_DOCUMENT) {
                depth++;
            } else if (event == END_DOCUMENT) {
                if (depth != 0) depth--;
                try {
                    serializeEndElement(writer);
                } catch (Exception e) {
                }
            }
            if (depth == 0) {
                break;
            }
        }
    }

    /**
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    protected void serializeElement(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        ArrayList writePrefixList = null;
        ArrayList writeNSList = null;
        String ePrefix = reader.getPrefix();
        ePrefix = (ePrefix != null && ePrefix.length() == 0) ? null : ePrefix;
        String eNamespace = reader.getNamespaceURI();
        eNamespace = (eNamespace != null && eNamespace.length() == 0) ? null : eNamespace;
        if (this.inputHasAttachments && XOP_INCLUDE.getNamespaceURI().equals(eNamespace)) {
            String eLocalPart = reader.getLocalName();
            if (XOP_INCLUDE.getLocalPart().equals(eLocalPart)) {
                if (serializeXOPInclude(reader, writer)) {
                    skipEndElement = true;
                    return;
                }
            }
        }
        boolean setPrefixFirst = OMSerializerUtil.isSetPrefixBeforeStartElement(writer);
        if (!setPrefixFirst) {
            if (eNamespace != null) {
                if (ePrefix == null) {
                    if (!OMSerializerUtil.isAssociated("", eNamespace, writer)) {
                        if (writePrefixList == null) {
                            writePrefixList = new ArrayList();
                            writeNSList = new ArrayList();
                        }
                        writePrefixList.add("");
                        writeNSList.add(eNamespace);
                    }
                    writer.writeStartElement("", reader.getLocalName(), eNamespace);
                } else {
                    if (!OMSerializerUtil.isAssociated(ePrefix, eNamespace, writer)) {
                        if (writePrefixList == null) {
                            writePrefixList = new ArrayList();
                            writeNSList = new ArrayList();
                        }
                        writePrefixList.add(ePrefix);
                        writeNSList.add(eNamespace);
                    }
                    writer.writeStartElement(ePrefix, reader.getLocalName(), eNamespace);
                }
            } else {
                writer.writeStartElement(reader.getLocalName());
            }
        }
        int count = reader.getNamespaceCount();
        for (int i = 0; i < count; i++) {
            String prefix = reader.getNamespacePrefix(i);
            prefix = (prefix != null && prefix.length() == 0) ? null : prefix;
            String namespace = reader.getNamespaceURI(i);
            namespace = (namespace != null && namespace.length() == 0) ? null : namespace;
            String newPrefix = OMSerializerUtil.generateSetPrefix(prefix, namespace, writer, false, setPrefixFirst);
            if (newPrefix != null) {
                if (writePrefixList == null) {
                    writePrefixList = new ArrayList();
                    writeNSList = new ArrayList();
                }
                if (!writePrefixList.contains(newPrefix)) {
                    writePrefixList.add(newPrefix);
                    writeNSList.add(namespace);
                }
            }
        }
        String newPrefix = OMSerializerUtil.generateSetPrefix(ePrefix, eNamespace, writer, false, setPrefixFirst);
        if (newPrefix != null) {
            if (writePrefixList == null) {
                writePrefixList = new ArrayList();
                writeNSList = new ArrayList();
            }
            if (!writePrefixList.contains(newPrefix)) {
                writePrefixList.add(newPrefix);
                writeNSList.add(eNamespace);
            }
        }
        count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String prefix = reader.getAttributePrefix(i);
            prefix = (prefix != null && prefix.length() == 0) ? null : prefix;
            String namespace = reader.getAttributeNamespace(i);
            namespace = (namespace != null && namespace.length() == 0) ? null : namespace;
            if (prefix == null && namespace != null) {
                String writerPrefix = writer.getPrefix(namespace);
                writerPrefix = (writerPrefix != null && writerPrefix.length() == 0) ? null : writerPrefix;
                prefix = (writerPrefix != null) ? writerPrefix : generateUniquePrefix(writer.getNamespaceContext());
            }
            newPrefix = OMSerializerUtil.generateSetPrefix(prefix, namespace, writer, true, setPrefixFirst);
            if (newPrefix != null) {
                if (writePrefixList == null) {
                    writePrefixList = new ArrayList();
                    writeNSList = new ArrayList();
                }
                if (!writePrefixList.contains(newPrefix)) {
                    writePrefixList.add(newPrefix);
                    writeNSList.add(namespace);
                }
            }
        }
        if (setPrefixFirst) {
            if (eNamespace != null) {
                if (ePrefix == null) {
                    writer.writeStartElement("", reader.getLocalName(), eNamespace);
                } else {
                    writer.writeStartElement(ePrefix, reader.getLocalName(), eNamespace);
                }
            } else {
                writer.writeStartElement(reader.getLocalName());
            }
        }
        if (writePrefixList != null) {
            for (int i = 0; i < writePrefixList.size(); i++) {
                String prefix = (String) writePrefixList.get(i);
                String namespace = (String) writeNSList.get(i);
                if (prefix != null) {
                    if (namespace == null) {
                        writer.writeNamespace(prefix, "");
                    } else {
                        writer.writeNamespace(prefix, namespace);
                    }
                } else {
                    writer.writeDefaultNamespace(namespace);
                }
            }
        }
        count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String prefix = reader.getAttributePrefix(i);
            prefix = (prefix != null && prefix.length() == 0) ? null : prefix;
            String namespace = reader.getAttributeNamespace(i);
            namespace = (namespace != null && namespace.length() == 0) ? null : namespace;
            if (prefix == null && namespace != null) {
                prefix = writer.getPrefix(namespace);
                if (prefix == null || "".equals(prefix)) {
                    for (int j = 0; j < writePrefixList.size(); j++) {
                        if (namespace.equals((String) writeNSList.get(j))) {
                            prefix = (String) writePrefixList.get(j);
                        }
                    }
                }
            } else if (namespace != null) {
                String writerPrefix = writer.getPrefix(namespace);
                if (!prefix.equals(writerPrefix) && !"".equals(writerPrefix)) {
                    prefix = writerPrefix;
                }
            }
            if (namespace != null) {
                writer.writeAttribute(prefix, namespace, reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            } else {
                writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            }
        }
    }

    /**
     * Method serializeEndElement.
     *
     * @param writer
     * @throws XMLStreamException
     */
    protected void serializeEndElement(XMLStreamWriter writer) throws XMLStreamException {
        if (this.skipEndElement) {
            skipEndElement = false;
            return;
        }
        writer.writeEndElement();
    }

    /**
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    protected void serializeText(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeCharacters(reader.getText());
    }

    /**
     * Method serializeCData.
     *
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    protected void serializeCData(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeCData(reader.getText());
    }

    /**
     * Method serializeComment.
     *
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    protected void serializeComment(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeComment(reader.getText());
    }

    /**
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    protected void serializeAttributes(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        int count = reader.getAttributeCount();
        String prefix = null;
        String namespaceName = null;
        String writerPrefix = null;
        for (int i = 0; i < count; i++) {
            prefix = reader.getAttributePrefix(i);
            namespaceName = reader.getAttributeNamespace(i);
            namespaceName = (namespaceName == null) ? "" : namespaceName;
            writerPrefix = writer.getPrefix(namespaceName);
            if (!"".equals(namespaceName)) {
                if (writerPrefix != null && (prefix == null || prefix.equals(""))) {
                    writer.writeAttribute(writerPrefix, namespaceName, reader.getAttributeLocalName(i), reader.getAttributeValue(i));
                } else if (prefix != null && !"".equals(prefix) && !prefix.equals(writerPrefix)) {
                    writer.writeNamespace(prefix, namespaceName);
                    writer.writeAttribute(prefix, namespaceName, reader.getAttributeLocalName(i), reader.getAttributeValue(i));
                } else {
                    prefix = generateUniquePrefix(writer.getNamespaceContext());
                    writer.writeNamespace(prefix, namespaceName);
                    writer.writeAttribute(prefix, namespaceName, reader.getAttributeLocalName(i), reader.getAttributeValue(i));
                }
            } else {
                writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            }
        }
    }

    /**
     * Generates a unique namespace prefix that is not in the scope of the NamespaceContext
     *
     * @param nsCtxt
     * @return string
     */
    private String generateUniquePrefix(NamespaceContext nsCtxt) {
        String prefix = NAMESPACE_PREFIX + namespaceSuffix++;
        while (nsCtxt.getNamespaceURI(prefix) != null) {
            prefix = NAMESPACE_PREFIX + namespaceSuffix++;
        }
        return prefix;
    }

    /**
     * Method serializeNamespace.
     *
     * @param prefix
     * @param URI
     * @param writer
     * @throws XMLStreamException
     */
    private void serializeNamespace(String prefix, String URI, XMLStreamWriter writer) throws XMLStreamException {
        String prefix1 = writer.getPrefix(URI);
        if (prefix1 == null) {
            writer.writeNamespace(prefix, URI);
            writer.setPrefix(prefix, URI);
        }
    }

    /**
     * Inspect the current element and if it is an
     * XOP Include then write it out as inlined or optimized.
     * @param reader
     * @param writer
     * @return true if inlined
     */
    protected boolean serializeXOPInclude(XMLStreamReader reader, XMLStreamWriter writer) {
        String cid = ElementHelper.getContentID(reader);
        DataHandler dh = getDataHandler(cid, (OMAttachmentAccessor) reader);
        if (dh == null) {
            return false;
        }
        OMFactory omFactory = OMAbstractFactory.getOMFactory();
        OMText omText = omFactory.createOMText(dh, true);
        omText.setContentID(cid);
        MTOMXMLStreamWriter mtomWriter = (writer instanceof MTOMXMLStreamWriter) ? (MTOMXMLStreamWriter) writer : null;
        if (mtomWriter != null && mtomWriter.isOptimized() && mtomWriter.isOptimizedThreshold(omText)) {
            mtomWriter.writeOptimized(omText);
            return false;
        }
        omText.setOptimize(false);
        try {
            writer.writeCharacters(omText.getText());
            return true;
        } catch (XMLStreamException e) {
            return false;
        }
    }

    private DataHandler getDataHandler(String cid, OMAttachmentAccessor oaa) {
        DataHandler dh = null;
        String blobcid = cid;
        if (blobcid.startsWith("cid:")) {
            blobcid = blobcid.substring(4);
        }
        if (oaa != null) {
            dh = oaa.getDataHandler(blobcid);
        }
        if (dh == null) {
            blobcid = getNewCID(cid);
            if (blobcid.startsWith("cid:")) {
                blobcid = blobcid.substring(4);
            }
            if (oaa != null) {
                dh = oaa.getDataHandler(blobcid);
            }
        }
        return dh;
    }

    /**
     * @param cid
     * @return cid with translated characters
     */
    private String getNewCID(String cid) {
        String cid2 = cid;
        try {
            cid2 = java.net.URLDecoder.decode(cid, "UTF-8");
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("getNewCID decoding " + cid + " as UTF-8 decoding error: " + e);
            }
        }
        return cid2;
    }
}
