package org.monet.docservice.docprocessor.templates.openxml.partsextractor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.monet.docservice.docprocessor.templates.common.Attributes;

public class TableProcessor extends BaseXmlProcessor {

    int depth = 0;

    ByteArrayOutputStream onMemoryStream;

    XMLStreamWriter onMemoryWriter;

    XMLStreamWriter fileWriter;

    OutputStream fileUnderlayingStream;

    boolean firstRow = true;

    boolean rowIsATemplate = false;

    boolean ignoreRestOfTemplateRows = false;

    boolean isfetchingTableRow = false;

    int tableRowLevel = 0;

    String tableName;

    public TableProcessor(XMLStreamReader reader, XMLStreamWriter writer, OutputStream underlayingOutputStream) throws XMLStreamException, FactoryConfigurationError {
        super(reader, writer, underlayingOutputStream);
        fileWriter = writer;
        fileUnderlayingStream = underlayingOutputStream;
        onMemoryStream = new ByteArrayOutputStream();
        onMemoryWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(onMemoryStream, "UTF-8");
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        onMemoryWriter.close();
        onMemoryStream.close();
    }

    @Override
    protected boolean handleStartElement(String localName, Attributes attributes) throws IOException, XMLStreamException {
        if (localName.equals("tbl")) {
            if (depth == 0) {
                isfetchingTableRow = true;
                tableRowLevel = 0;
            }
            depth++;
        } else if (localName.equals("tr")) {
            this.tableRowLevel++;
            if (isfetchingTableRow) {
                isfetchingTableRow = false;
                onMemoryWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(onMemoryStream, "UTF-8");
                this.writer = onMemoryWriter;
                this.underlayingOutputStream = onMemoryStream;
                this.writer.setNamespaceContext(reader.getNamespaceContext());
            }
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
        if (localName.equals("tbl") && depth > 0) {
            depth--;
            if (depth == 0) Stop();
        } else if (localName.equals("tr") && !this.isfetchingTableRow && this.tableRowLevel == 1) {
            writer = fileWriter;
            if (fileWriter != null) fileWriter.flush();
            if (rowIsATemplate && !ignoreRestOfTemplateRows) {
                ignoreRestOfTemplateRows = true;
                onMemoryWriter.writeEndElement();
                onMemoryWriter.flush();
                ByteArrayOutputStream nsSupportStream = new ByteArrayOutputStream();
                XMLStreamWriter nsSupportWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(nsSupportStream);
                nsSupportWriter.writeStartElement("_8E03AB25A2E342ea84854A32DEA84BBC");
                for (Namespace n : this.getNamespaceContext().values()) nsSupportWriter.writeNamespace(n.Prefix, n.URI);
                nsSupportWriter.writeCharacters("");
                nsSupportWriter.flush();
                nsSupportStream.write(onMemoryStream.toByteArray());
                nsSupportWriter.writeEndDocument();
                nsSupportWriter.flush();
                this.underlayingOutputStream = fileUnderlayingStream;
                ByteArrayInputStream tempInputStream = new ByteArrayInputStream(nsSupportStream.toByteArray());
                this.repository.addTemplatePart(this.templateId, this.tableName, tempInputStream);
                tempInputStream.reset();
                XMLStreamReader onMemoryReader = XMLInputFactory.newInstance().createXMLStreamReader(tempInputStream);
                RootProcessor proc = new RootProcessor(onMemoryReader, fileWriter, underlayingOutputStream);
                proc.setPartial(true);
                proc.setNamespceContext(this.getNamespaceContext());
                proc.setRepository(this.repository);
                proc.setTemplateId(this.templateId);
                proc.Start();
            } else if (!rowIsATemplate) {
                isfetchingTableRow = true;
                onMemoryWriter.writeEndElement();
                onMemoryWriter.flush();
            }
            onMemoryWriter.close();
            onMemoryStream.reset();
            this.tableRowLevel--;
            return true;
        }
        if (localName.equals("tr")) {
            this.tableRowLevel--;
        }
        return false;
    }

    @Override
    protected boolean handleContent(String content) {
        return false;
    }
}
