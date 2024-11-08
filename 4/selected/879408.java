package org.monet.docservice.docprocessor.templates.openxml.partsextractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.monet.docservice.docprocessor.templates.common.Attributes;

public class RootProcessor extends BaseXmlProcessor {

    private static Pattern mergeFieldValue = Pattern.compile("MERGEFIELD\\s+\"?([^\\s|\"]*)\"?\\s+\\\\\\*\\s+MERGEFORMAT");

    private static Pattern tableFieldValue = Pattern.compile("[Table|table|TABLE]\\(([^\\)]*)\\)");

    private static Pattern blockFieldValue = Pattern.compile("[Block|block|BLOCK]\\(([^\\)]*)\\)");

    private boolean captureTable = false;

    private boolean captureBlock = false;

    private boolean insideField = false;

    private boolean insideFieldValue = false;

    private boolean insideFieldValueContent = false;

    private boolean insideInstrText = false;

    private boolean valueInserted = false;

    private String valueOfField = null;

    private StringBuilder fieldIdentifier = new StringBuilder();

    private String currentTableName;

    private String currentBlockName;

    public RootProcessor(InputStream documentStream, OutputStream processedDocStream) throws XMLStreamException, FactoryConfigurationError {
        super(documentStream, processedDocStream);
    }

    public RootProcessor(XMLStreamReader documentStream, XMLStreamWriter processedDocStream, OutputStream underlayingOutputStream) throws XMLStreamException, FactoryConfigurationError {
        super(documentStream, processedDocStream, underlayingOutputStream);
    }

    protected boolean handleStartElement(String localName, Attributes attributes) throws IOException, XMLStreamException {
        if (localName.equals("fldChar")) {
            String fldCharType = attributes.getValue("w:fldCharType");
            if (fldCharType.equals("begin")) {
                insideField = true;
                valueInserted = false;
            } else if (fldCharType.equals("separate")) {
                insideFieldValue = true;
                insideInstrText = false;
            } else if (fldCharType.equals("end")) {
                insideFieldValue = insideField = false;
                valueOfField = null;
            }
        } else if (localName.equals("fldSimple")) {
            fieldIdentifier.append(attributes.getValue("w:instr"));
            insideField = true;
            insideFieldValue = true;
            valueInserted = false;
            insideInstrText = false;
        } else if (localName.equals("instrText") && insideField) {
            insideInstrText = true;
        } else if (localName.equals("t") && insideFieldValue) {
            insideFieldValueContent = true;
        } else if (localName.equals("tbl")) {
            if (captureTable) {
                TableProcessor tableProcessor = new TableProcessor(this.reader, this.writer, this.underlayingOutputStream);
                tableProcessor.setPartial(true);
                tableProcessor.setNamespceContext(this.getNamespaceContext());
                tableProcessor.setRepository(this.repository);
                tableProcessor.setTableName(this.currentTableName);
                tableProcessor.setTemplateId(this.templateId);
                tableProcessor.Start();
                this.captureTable = false;
                this.currentTableName = null;
                return true;
            } else if (captureBlock) {
                BlockProcessor blockProcessor = new BlockProcessor(this.reader);
                blockProcessor.setPartial(true);
                blockProcessor.setNamespceContext(this.getNamespaceContext());
                blockProcessor.setRepository(this.repository);
                blockProcessor.setBlockName(this.currentBlockName);
                blockProcessor.setTemplateId(this.templateId);
                blockProcessor.Start();
                this.captureBlock = false;
                this.currentBlockName = null;
            }
        } else if (localName.equals("_8E03AB25A2E342ea84854A32DEA84BBC")) {
            return true;
        }
        return false;
    }

    protected boolean handleContent(String content) throws IOException, XMLStreamException {
        if (insideFieldValueContent) {
            insideFieldValueContent = false;
            if (!valueInserted) {
                handleTableField();
                if (writer != null) writer.writeCharacters(valueOfField);
                return true;
            } else {
                return true;
            }
        } else {
            if (insideInstrText) {
                fieldIdentifier.append(content);
            }
            return false;
        }
    }

    private void handleTableField() {
        Matcher mergeFieldValueMatcher = mergeFieldValue.matcher(fieldIdentifier.toString());
        fieldIdentifier = new StringBuilder();
        if (mergeFieldValueMatcher.find()) {
            String mergeFieldValueKey = mergeFieldValueMatcher.group(1);
            Matcher tableFieldValueMatcher = tableFieldValue.matcher(mergeFieldValueKey);
            if (tableFieldValueMatcher.find()) {
                this.currentTableName = tableFieldValueMatcher.group(1);
                valueOfField = "";
                captureTable = true;
            } else {
                Matcher blockFieldValueMatcher = blockFieldValue.matcher(mergeFieldValueKey);
                if (blockFieldValueMatcher.find()) {
                    this.currentBlockName = blockFieldValueMatcher.group(1);
                    valueOfField = "";
                    captureBlock = true;
                }
            }
        }
        valueInserted = true;
    }

    @Override
    protected boolean handleEndElement(String localName) {
        if (localName.equals("t") && insideFieldValueContent && !valueInserted) {
            handleTableField();
        }
        if (localName.equals("t")) insideFieldValueContent = false;
        if (localName.equals("fldSimple")) {
            insideFieldValue = insideField = false;
            valueOfField = "";
            fieldIdentifier = new StringBuilder();
        }
        return localName.equals("_8E03AB25A2E342ea84854A32DEA84BBC");
    }
}
