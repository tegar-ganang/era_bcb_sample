package org.monet.docservice.docprocessor.templates.opendocument;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.monet.docservice.docprocessor.templates.common.Attributes;
import org.monet.docservice.docprocessor.templates.common.BaseXmlProcessor;
import org.monet.docservice.docprocessor.templates.common.Model;

public class RootProcessor extends BaseXmlProcessor {

    private static final String TABLE_ROW_CELL_TEMPLATE = "TABLE.%s.ROW.%s.CELL.%s";

    private static final String TABLE = "TABLE.";

    private final Pattern TABLE_CELL_FIELD_PATTERN = Pattern.compile("TABLE.(\\w*).ROW.(\\w*).CELL.(\\w*)");

    private final Pattern TABLE_FIELD_PATTERN = Pattern.compile("TABLE.(\\w*)$");

    private boolean bInsideUserFieldDecls = false;

    private boolean bIgnoreContent = false;

    private HashMap<String, List<String>> tables = new HashMap<String, List<String>>();

    private boolean bIgnoreEndElement = false;

    private boolean captureTable = false;

    private Collection<?> currentCollection;

    private int rowIndex = -1;

    public RootProcessor(InputStream documentStream, OutputStream processedDocStream) throws XMLStreamException, FactoryConfigurationError {
        super(documentStream, processedDocStream);
    }

    public RootProcessor(XMLStreamReader documentStream, XMLStreamWriter processedDocStream, OutputStream underlayingOutputStream) throws XMLStreamException, FactoryConfigurationError {
        super(documentStream, processedDocStream, underlayingOutputStream);
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    protected boolean handleStartElement(String localName, Attributes attributes) throws IOException, XMLStreamException {
        if (localName.equals("user-field-decls")) {
            this.bInsideUserFieldDecls = true;
        } else if (localName.equals("user-field-decl") && this.bInsideUserFieldDecls) {
            String sName = attributes.getValue("text:name");
            if (sName.startsWith(TABLE)) {
                bIgnoreEndElement = processFieldDecl(sName);
                return bIgnoreEndElement;
            } else if (this.model.isPropertyAString(sName)) {
                writeProperty(localName, sName);
                bIgnoreEndElement = false;
                return true;
            }
        } else if (localName.equals("user-field-get")) {
            this.bIgnoreContent = true;
            String fieldName = attributes.getValue("text:name");
            if (!detectTableField(fieldName, attributes)) {
                Matcher matcher = TABLE_CELL_FIELD_PATTERN.matcher(fieldName);
                if (matcher.find()) {
                    writeUserFieldGet(localName, fieldName.replaceFirst("ROW.(\\w*).CELL", String.format("ROW.%d.CELL", rowIndex)));
                    return true;
                }
            }
        } else if (localName.equals("radio")) {
            String fieldName = attributes.getValue("form:name");
            String label = attributes.getValue("form:label");
            if (this.model.isPropertyAString(label)) {
                writeProperty(fieldName, label);
                addProperty("current-selected", "true", fieldName, label);
                addProperty("selected", "true", fieldName, label);
            }
            return true;
        } else if (localName.equals("table") && captureTable) {
            TableProcessor tableProcessor = new TableProcessor(this.reader, this.writer, this.underlayingOutputStream);
            tableProcessor.setCollectionModel(currentCollection);
            tableProcessor.setPartial(true);
            tableProcessor.setNamespceContext(this.getNamespaceContext());
            tableProcessor.Start();
            captureTable = false;
            return true;
        } else if (localName.equals("_8E03AB25A2E342ea84854A32DEA84BBC")) {
            return true;
        }
        return false;
    }

    private boolean detectTableField(String fieldName, Attributes attributes) throws IllegalArgumentException {
        Matcher matcher = TABLE_FIELD_PATTERN.matcher(fieldName);
        if (matcher.find()) {
            fieldName = matcher.group(1);
            if (model.isPropertyACollection(fieldName)) {
                currentCollection = model.getPropertyAsCollection(fieldName);
                return captureTable = true;
            }
        }
        return false;
    }

    private boolean processFieldDecl(String sName) {
        Matcher matcher = TABLE_CELL_FIELD_PATTERN.matcher(sName);
        if (matcher.find()) {
            String tableId = matcher.group(1);
            String cellId = matcher.group(3);
            if (!tables.containsKey(tableId)) tables.put(tableId, new ArrayList<String>());
            List<String> tableCells = tables.get(tableId);
            if (!tableCells.contains(cellId)) tableCells.add(cellId);
            return true;
        }
        return false;
    }

    private void writeUserFieldGet(String sElementLocalName, String sFieldName) throws XMLStreamException {
        writer.writeStartElement(reader.getPrefix(), sElementLocalName, reader.getNamespaceURI());
        int attrbCount = reader.getAttributeCount();
        for (int i = 0; i < attrbCount; i++) {
            String namespaceURI = reader.getAttributeNamespace(i);
            String namespacePrefix = reader.getAttributePrefix(i);
            String attrbLocalName = reader.getAttributeLocalName(i);
            String value;
            if (attrbLocalName.equals("name")) value = sFieldName; else value = reader.getAttributeValue(i);
            if (namespaceURI == null && (namespacePrefix == null || namespacePrefix.length() == 0)) writer.writeAttribute(attrbLocalName, value); else if (namespacePrefix == null || namespacePrefix.length() == 0) writer.writeAttribute(namespaceURI, attrbLocalName, value); else writer.writeAttribute(namespacePrefix, namespaceURI, attrbLocalName, value);
        }
    }

    private void writeProperty(String sElementLocalName, String sPropertyKey) throws XMLStreamException {
        writer.writeStartElement(reader.getPrefix(), sElementLocalName, reader.getNamespaceURI());
        int attrbCount = reader.getAttributeCount();
        for (int i = 0; i < attrbCount; i++) {
            String namespaceURI = reader.getAttributeNamespace(i);
            String namespacePrefix = reader.getAttributePrefix(i);
            String attrbLocalName = reader.getAttributeLocalName(i);
            String value;
            if (attrbLocalName.equals("string-value")) value = this.model.isPropertyAString(sPropertyKey) ? this.model.getPropertyAsString(sPropertyKey) : String.format("Field %s not found in document.", sPropertyKey); else value = reader.getAttributeValue(i);
            if (namespaceURI == null && (namespacePrefix == null || namespacePrefix.length() == 0)) writer.writeAttribute(attrbLocalName, value); else if (namespacePrefix == null || namespacePrefix.length() == 0) writer.writeAttribute(namespaceURI, attrbLocalName, value); else writer.writeAttribute(namespacePrefix, namespaceURI, attrbLocalName, value);
        }
    }

    private void addProperty(String property, String value, String fieldName, String label) throws XMLStreamException {
        String namespaceURI = reader.getAttributeNamespace(0);
        String namespacePrefix = reader.getAttributePrefix(0);
        String attrbLocalName = property;
        writer.writeAttribute(namespacePrefix, namespaceURI, attrbLocalName, value);
    }

    protected boolean handleContent(String content) throws IOException, XMLStreamException {
        return bIgnoreContent;
    }

    @Override
    protected boolean handleEndElement(String localName) throws XMLStreamException {
        if (localName.equals("user-field-decls")) {
            this.bInsideUserFieldDecls = false;
            for (String tableId : tables.keySet()) {
                Collection<Model> tableModels = this.model.getPropertyAsCollection(tableId);
                int i = 0;
                for (Model model : tableModels) {
                    for (String cellId : tables.get(tableId)) {
                        writer.writeStartElement("text", "user-field-decl", "urn:oasis:names:tc:opendocument:xmlns:text:1.0");
                        writer.writeAttribute("office", "urn:oasis:names:tc:opendocument:xmlns:office:1.0", "value-type", "string");
                        writer.writeAttribute("office", "urn:oasis:names:tc:opendocument:xmlns:office:1.0", "string-value", model.getPropertyAsString(cellId));
                        writer.writeAttribute("text", "urn:oasis:names:tc:opendocument:xmlns:text:1.0", "name", String.format(TABLE_ROW_CELL_TEMPLATE, tableId, i, cellId));
                        writer.writeEndElement();
                    }
                    i++;
                }
            }
        } else if (localName.equals("user-field-decl")) {
            boolean ignoreEnd = bIgnoreEndElement;
            bIgnoreEndElement = false;
            return ignoreEnd;
        } else if (localName.equals("user-field-get")) {
            this.bIgnoreContent = false;
        }
        return localName.equals("_8E03AB25A2E342ea84854A32DEA84BBC");
    }
}
