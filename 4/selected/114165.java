package org.monet.docservice.docprocessor.templates.openxml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.monet.docservice.docprocessor.templates.common.Attributes;
import org.monet.docservice.docprocessor.templates.common.BaseXmlProcessor;
import org.monet.docservice.docprocessor.templates.common.Model;

public class TableProcessor extends BaseXmlProcessor {

    Collection<Model> currentCollection;

    int depth = 0;

    ByteArrayOutputStream onMemoryStream;

    XMLStreamWriter onMemoryWriter;

    XMLStreamWriter fileWriter;

    OutputStream fileUnderlayingStream;

    boolean firstRow = true;

    boolean rowIsATemplate = false;

    boolean ignoreRestOfTemplateRows = false;

    boolean rowsInserted = false;

    private String tableName;

    private int rowLevel = 0;

    public TableProcessor(XMLStreamReader reader, XMLStreamWriter writer, OutputStream underlayingOutputStream) throws XMLStreamException, FactoryConfigurationError {
        super(reader, writer, underlayingOutputStream);
        fileWriter = writer;
        fileUnderlayingStream = underlayingOutputStream;
        onMemoryStream = new ByteArrayOutputStream();
        onMemoryWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(onMemoryStream, "UTF-8");
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        onMemoryWriter.close();
        onMemoryStream.close();
    }

    @Override
    protected boolean handleStartElement(String localName, Attributes attributes) throws IOException, XMLStreamException {
        if (localName.equals("tr")) this.rowLevel++;
        if (rowIsATemplate) return true;
        if (localName.equals("tbl")) {
            depth++;
        } else if (localName.equals("tr") && rowLevel == 1) {
            onMemoryWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(onMemoryStream, "UTF-8");
            this.writer = onMemoryWriter;
            this.underlayingOutputStream = onMemoryStream;
            this.writer.setNamespaceContext(reader.getNamespaceContext());
        } else if (localName.equals("fldChar")) {
            String fldCharType = attributes.getValue("w:fldCharType");
            rowIsATemplate = fldCharType.equals("end");
        } else if (localName.equals("fldSimple")) {
            rowIsATemplate = true;
        }
        return false;
    }

    @Override
    protected boolean handleEndElement(String localName) throws XMLStreamException, FactoryConfigurationError, IOException {
        if (localName.equals("tbl") && depth > 0 && !rowIsATemplate) {
            depth--;
            if (depth == 0) {
                if (!ignoreRestOfTemplateRows) fillRows();
                Stop();
            }
        } else if (localName.equals("tr")) {
            rowLevel--;
            if (rowLevel != 0) return ignoreRestOfTemplateRows || rowIsATemplate;
            writer = fileWriter;
            fileWriter.flush();
            if (rowIsATemplate && !ignoreRestOfTemplateRows) {
                ignoreRestOfTemplateRows = true;
                fillRows();
            } else if (!rowIsATemplate) {
                onMemoryWriter.writeEndElement();
                onMemoryWriter.flush();
                this.underlayingOutputStream = fileUnderlayingStream;
                this.underlayingOutputStream.write(onMemoryStream.toByteArray());
            }
            rowIsATemplate = false;
            onMemoryWriter.close();
            onMemoryStream.reset();
            return true;
        } else if (rowIsATemplate) {
            return true;
        }
        return false;
    }

    private void fillRows() throws IOException, XMLStreamException, FactoryConfigurationError {
        InputStream rowTemplate = this.repository.getTemplatePart(this.documentId, this.tableName);
        this.underlayingOutputStream = fileUnderlayingStream;
        for (Model model : currentCollection) {
            rowTemplate.reset();
            XMLStreamReader onMemoryReader = XMLInputFactory.newInstance().createXMLStreamReader(rowTemplate);
            RootProcessor proc = new RootProcessor(onMemoryReader, fileWriter, underlayingOutputStream);
            proc.setModel(model);
            proc.setPartial(true);
            proc.setRepository(this.repository);
            proc.setDocumentId(this.documentId);
            proc.setNamespceContext(this.getNamespaceContext());
            proc.setNewIdImages(newIdImages);
            proc.setOldIdImages(oldIdImages);
            proc.Start();
            onMemoryReader.close();
        }
    }

    @Override
    protected boolean handleContent(String content) {
        return ignoreRestOfTemplateRows || rowIsATemplate;
    }

    @SuppressWarnings("unchecked")
    public void setCollectionModel(Collection<?> currentCollection) {
        this.currentCollection = (Collection<Model>) currentCollection;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}
