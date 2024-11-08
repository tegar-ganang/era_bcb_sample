package org.monet.docservice.docprocessor.templates.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.monet.docservice.docprocessor.data.Repository;

public abstract class BaseXmlProcessor {

    protected class Namespace {

        public Namespace(String prefix, String Uri) {
            Prefix = prefix;
            URI = Uri;
        }

        public String Prefix;

        public String URI;
    }

    protected abstract boolean handleStartElement(String localName, Attributes attributes) throws IOException, XMLStreamException;

    protected abstract boolean handleEndElement(String localName) throws XMLStreamException, FactoryConfigurationError, IOException;

    protected abstract boolean handleContent(String content) throws IOException, XMLStreamException;

    protected XMLStreamReader reader;

    protected XMLStreamWriter writer;

    protected OutputStream underlayingOutputStream;

    protected Model model;

    protected Repository repository;

    protected String documentId;

    protected List<String> newIdImages;

    protected List<String> oldIdImages;

    private boolean stop = false;

    private boolean isPartial = false;

    private HashMap<String, Namespace> namespaceContext = new HashMap<String, Namespace>();

    private HashMap<String, String> modifiedAttributes = new HashMap<String, String>();

    public BaseXmlProcessor(XMLStreamReader reader, XMLStreamWriter writer, OutputStream underlayingOutputStream) {
        this.reader = reader;
        this.writer = writer;
        this.underlayingOutputStream = underlayingOutputStream;
    }

    public BaseXmlProcessor(InputStream documentStream, OutputStream processedDocStream) throws XMLStreamException, FactoryConfigurationError {
        this.reader = XMLInputFactory.newInstance().createXMLStreamReader(documentStream);
        this.writer = XMLOutputFactory.newInstance().createXMLStreamWriter(processedDocStream, "UTF-8");
        this.underlayingOutputStream = processedDocStream;
    }

    public void setNamespceContext(Map<String, Namespace> context) {
        namespaceContext.putAll(context);
    }

    public Map<String, Namespace> getNamespaceContext() {
        return namespaceContext;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Model getModel() {
        return this.model;
    }

    public void Start() throws IOException, XMLStreamException {
        Process();
    }

    public void Stop() {
        stop = true;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public List<String> getNewIdImages() {
        return newIdImages;
    }

    public void setNewIdImages(List<String> newIdImages) {
        this.newIdImages = newIdImages;
    }

    public List<String> getOldIdImages() {
        return oldIdImages;
    }

    public void setOldIdImages(List<String> oldIdImages) {
        this.oldIdImages = oldIdImages;
    }

    protected void Process() throws IOException, XMLStreamException {
        try {
            int eventType = reader.getEventType();
            while (reader.hasNext()) {
                switch(eventType) {
                    case XMLStreamConstants.START_DOCUMENT:
                        if (!isPartial) {
                            writer.writeStartDocument(reader.getEncoding(), reader.getVersion());
                            writer.setNamespaceContext(reader.getNamespaceContext());
                        }
                        break;
                    case XMLStreamConstants.END_DOCUMENT:
                        if (!isPartial) writer.writeEndDocument();
                        break;
                    case XMLStreamConstants.CDATA:
                        writer.writeCData(reader.getText());
                        break;
                    case XMLStreamConstants.COMMENT:
                        writer.writeComment(reader.getText());
                        break;
                    case XMLStreamConstants.DTD:
                        writer.writeDTD(reader.getText());
                        break;
                    case XMLStreamConstants.ENTITY_REFERENCE:
                        writer.writeEntityRef(reader.getLocalName());
                        break;
                    case XMLStreamConstants.NAMESPACE:
                        _processNamespaces();
                        break;
                    case XMLStreamConstants.PROCESSING_INSTRUCTION:
                        String pIData = reader.getPIData();
                        if (pIData == null) writer.writeProcessingInstruction(reader.getPITarget()); else writer.writeProcessingInstruction(reader.getPITarget(), pIData);
                        break;
                    case XMLStreamConstants.SPACE:
                        writer.writeCharacters(reader.getText());
                        break;
                    case XMLStreamConstants.START_ELEMENT:
                        this.modifiedAttributes.clear();
                        if (_processStartElement()) {
                            _processNamespaces();
                            _processAttributes();
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (!handleEndElement(reader.getLocalName())) writer.writeEndElement();
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        if (!handleContent(reader.getText())) {
                            writer.writeCharacters(reader.getText());
                        }
                        break;
                    case XMLStreamConstants.ATTRIBUTE:
                        _processAttributes();
                        break;
                }
                if (stop) return;
                eventType = reader.next();
            }
            if (!isPartial) writer.close();
            reader.close();
        } catch (FactoryConfigurationError e) {
            e.printStackTrace();
        }
    }

    private boolean _processStartElement() throws IOException, XMLStreamException {
        String URI = reader.getNamespaceURI();
        String prefix = reader.getPrefix();
        String elementLocalName = reader.getLocalName();
        if (!handleStartElement(elementLocalName, new Attributes(reader, this.modifiedAttributes))) {
            if (URI == null && prefix == null) writer.writeStartElement(elementLocalName); else if (prefix == null) writer.writeStartElement(URI, elementLocalName); else writer.writeStartElement(prefix, elementLocalName, URI);
            return true;
        } else {
            return false;
        }
    }

    private void _processNamespaces() throws XMLStreamException {
        int namespaceCount = reader.getNamespaceCount();
        for (int i = 0; i < namespaceCount; i++) {
            writer.writeNamespace(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
            this.namespaceContext.put(reader.getNamespacePrefix(i), new Namespace(reader.getNamespacePrefix(i), reader.getNamespaceURI(i)));
        }
    }

    private void _processAttributes() throws XMLStreamException, UnsupportedEncodingException, IOException {
        int attrbCount = reader.getAttributeCount();
        for (int i = 0; i < attrbCount; i++) {
            String namespacePrefix = reader.getAttributePrefix(i);
            String attrbLocalName = reader.getAttributeLocalName(i);
            String value = null;
            String key = namespacePrefix + ":" + attrbLocalName;
            if (this.modifiedAttributes.containsKey(key)) {
                value = this.modifiedAttributes.get(key);
            } else {
                value = reader.getAttributeValue(i).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
            }
            writer.flush();
            String sData = null;
            if (namespacePrefix == null || namespacePrefix.length() == 0) sData = String.format(" %s=\"%s\"", attrbLocalName, value); else sData = String.format(" %s:%s=\"%s\"", namespacePrefix, attrbLocalName, value);
            underlayingOutputStream.write(sData.getBytes("UTF-8"));
            underlayingOutputStream.flush();
        }
    }

    public void setPartial(boolean isPartial) {
        this.isPartial = isPartial;
    }

    public boolean isPartial() {
        return isPartial;
    }
}
