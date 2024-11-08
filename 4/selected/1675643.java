package org.monet.docservice.docprocessor.templates.openxml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.monet.docservice.docprocessor.templates.common.Attributes;
import org.monet.docservice.docprocessor.templates.common.BaseXmlProcessor;

public class BlockProcessor extends BaseXmlProcessor {

    int depth = 0;

    private String blockName;

    private boolean showBlock;

    public BlockProcessor(XMLStreamReader reader, XMLStreamWriter writer, OutputStream underlayingOutputStream) throws XMLStreamException, FactoryConfigurationError {
        super(reader, writer, underlayingOutputStream);
    }

    @Override
    protected boolean handleStartElement(String localName, Attributes attributes) throws IOException, XMLStreamException {
        if (localName.equals("tbl")) {
            depth++;
        }
        return !showBlock;
    }

    @Override
    protected boolean handleEndElement(String localName) throws XMLStreamException, FactoryConfigurationError, IOException {
        if (localName.equals("tbl") && depth > 0) {
            depth--;
            if (depth == 0) {
                Stop();
            }
        }
        return !showBlock;
    }

    @Override
    protected boolean handleContent(String content) {
        return !showBlock;
    }

    public void showBlock(boolean showBlock) {
        this.showBlock = showBlock;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public void Create() throws XMLStreamException, FactoryConfigurationError, IOException {
        InputStream partTemplate = this.repository.getTemplatePart(this.documentId, this.blockName);
        XMLStreamReader onMemoryReader = XMLInputFactory.newInstance().createXMLStreamReader(partTemplate);
        RootProcessor proc = new RootProcessor(onMemoryReader, this.writer, this.underlayingOutputStream);
        proc.setModel(model);
        proc.setPartial(true);
        proc.setRepository(this.repository);
        proc.setDocumentId(this.documentId);
        proc.setNamespceContext(this.getNamespaceContext());
        proc.setNewIdImages(newIdImages);
        proc.setOldIdImages(oldIdImages);
        proc.Start();
    }
}
