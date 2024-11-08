package org.monet.docservice.docprocessor.templates.portabledocument;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.monet.docservice.core.exceptions.ApplicationException;
import org.monet.docservice.core.log.Logger;
import org.monet.docservice.docprocessor.configuration.Configuration;
import org.monet.docservice.docprocessor.templates.DocumentProcessor;
import org.monet.docservice.docprocessor.templates.common.Model;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.google.inject.Inject;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfFormField;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfRectangle;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.RadioCheckField;
import com.itextpdf.text.pdf.TextField;

public class PdfProcessor implements DocumentProcessor {

    private static final String COLLECTIONS_XPATH = "collections/*";

    private Logger logger;

    private Configuration configuration;

    private Formatter formatter;

    private Model model;

    private Map<Integer, List<String>> plainFields = new HashMap<Integer, List<String>>();

    private Map<String, List<FieldInfo>> collectionsFields = new HashMap<String, List<FieldInfo>>();

    private com.itextpdf.text.Document document;

    @Inject
    public void injectLogger(Logger logger) {
        this.logger = logger;
    }

    @Inject
    public void injectConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Inject
    public void injectFormatter(Formatter formatter) {
        this.formatter = formatter;
    }

    public void setModel(Model model) {
        logger.debug("setModel(%s)", model);
        this.model = model;
    }

    public void setDocumentId(String documentId) {
    }

    public void process(String sourceDocumentPath) {
        logger.debug("process(%s)", sourceDocumentPath);
        try {
            if (this.model.isEmpty()) return;
            File outTempFile = new File(this.configuration.getPath(Configuration.PATH_TEMP) + File.separator + UUID.randomUUID().toString() + ".pdf");
            FileOutputStream outputStream = new FileOutputStream(outTempFile);
            this.document = new com.itextpdf.text.Document();
            PdfWriter writer = PdfWriter.getInstance(this.document, outputStream);
            if (this.configuration.getBoolean(Configuration.GENERATE_PDF_A)) writer.setPDFXConformance(PdfWriter.PDFA1B);
            this.document.open();
            PdfContentByte finalPdfContent = writer.getDirectContent();
            PdfReader reader = new PdfReader(sourceDocumentPath);
            AcroFields fields = reader.getAcroFields();
            classifyFields(fields);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                String sPageInfo = fields.getField(String.format("PageInfo:%s", page));
                Map<String, PdfCollection> pageInfo;
                if (sPageInfo != null) pageInfo = parsePageInfo(sPageInfo); else pageInfo = new HashMap<String, PdfCollection>();
                PdfImportedPage importedPage = null;
                if (pageInfo.size() > 0) {
                    Iterator<PdfCollection> pageCollectionIterator = pageInfo.values().iterator();
                    boolean pageCreated = false;
                    while (pageCollectionIterator.hasNext()) {
                        PdfCollection collection = pageCollectionIterator.next();
                        if (!this.model.isPropertyACollection(collection.getId())) continue;
                        Collection<Model> modelCollection = this.model.getPropertyAsCollection(collection.getId());
                        Iterator<Model> iterator = modelCollection.iterator();
                        Model currentModel;
                        int currentPage = 0, currentRow = collection.getPageSize();
                        float currentOffset = 0.0f;
                        float offset = calculateOffset(fields, collectionsFields.get(collection.getId()), collection);
                        if (!iterator.hasNext()) {
                            this.document.newPage();
                            importedPage = writer.getImportedPage(reader, page);
                            finalPdfContent.addTemplate(importedPage, 0, 0);
                        }
                        while (iterator.hasNext()) {
                            if (currentRow == collection.getPageSize() && (!pageCreated || collection.isMultipage())) {
                                currentRow = 0;
                                currentOffset = 0;
                                currentPage++;
                                pageCreated = true;
                                this.document.newPage();
                                importedPage = writer.getImportedPage(reader, page);
                                finalPdfContent.addTemplate(importedPage, 0, 0);
                            }
                            if (currentRow == collection.getPageSize()) {
                                if (!collection.isMultipage() && !(pageInfo.size() > 1)) break;
                                currentRow = 0;
                            }
                            currentModel = iterator.next();
                            for (FieldInfo fieldInfo : collectionsFields.get(collection.getId())) {
                                if (fieldInfo.parts[1].equals(collection.getOffsetField()) && !fieldInfo.parts[2].equals("0")) continue;
                                Rectangle fieldRect = null;
                                if (fieldInfo.rect != null) fieldRect = new Rectangle(fieldInfo.rect.left(), fieldInfo.rect.top() - currentOffset, fieldInfo.rect.right(), fieldInfo.rect.bottom() - currentOffset); else fieldRect = new Rectangle(0, currentOffset, 0, currentOffset);
                                fillField(fields, writer, currentModel, fieldInfo.originalName, fieldInfo.originalName + currentRow, fieldInfo.parts[1], fields.getFieldItem(fieldInfo.originalName), fieldRect);
                            }
                            currentOffset += offset;
                            currentRow++;
                        }
                    }
                } else {
                    this.document.newPage();
                    importedPage = writer.getImportedPage(reader, page);
                    finalPdfContent.addTemplate(importedPage, 0, 0);
                    fillPlainFields(page, fields, finalPdfContent, writer);
                }
            }
            this.document.close();
            outputStream.close();
            reader.close();
            reader = new PdfReader(outTempFile.getAbsolutePath());
            outputStream = new FileOutputStream(sourceDocumentPath, false);
            PdfStamper stamper = new PdfStamper(reader, outputStream);
            stamper.setFormFlattening(true);
            stamper.close();
            outputStream.close();
            reader.close();
            if (outTempFile.exists()) outTempFile.delete();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException("Error updating document.");
        }
    }

    /***
   * Process all fields of a page that aren't in a collection
   * 
   */
    private void fillPlainFields(int page, AcroFields fields, PdfContentByte content, PdfWriter writer) throws Exception {
        logger.debug("fillPlainFields(%s, %s, %s, %s)", page, fields, content, writer);
        for (String sField : this.plainFields.get(Integer.valueOf(page))) {
            if (sField.equals("PageInfo")) continue;
            AcroFields.Item fieldItem = fields.getFieldItem(sField);
            PdfArray pdfRect = (PdfArray) fieldItem.getMerged(0).get(PdfName.RECT);
            if (pdfRect == null) continue;
            Rectangle fieldRect;
            int fieldType = fields.getFieldType(sField);
            if (fieldType == AcroFields.FIELD_TYPE_CHECKBOX || fieldType == AcroFields.FIELD_TYPE_RADIOBUTTON) fieldRect = new Rectangle(0, 0, 0, 0); else fieldRect = new Rectangle(pdfRect.getAsNumber(0).floatValue(), pdfRect.getAsNumber(3).floatValue(), pdfRect.getAsNumber(2).floatValue(), pdfRect.getAsNumber(1).floatValue());
            fillField(fields, writer, this.model, sField, sField, sField, fieldItem, fieldRect);
        }
    }

    /***
   * Fill a single field 
   */
    private void fillField(AcroFields fields, PdfWriter writer, Model model, String oldFieldName, String newFieldName, String propertyName, AcroFields.Item fieldItem, Rectangle fieldRect) throws Exception {
        logger.debug("fillField(%s, %s, %s, %s, %s, %s, %s, %s)", fields, writer, model, oldFieldName, newFieldName, propertyName, fieldItem, fieldRect);
        int fieldType = fields.getFieldType(oldFieldName);
        switch(fieldType) {
            case AcroFields.FIELD_TYPE_TEXT:
                fillTextField(fields, writer, fieldRect, model, oldFieldName, newFieldName, propertyName);
                break;
            case AcroFields.FIELD_TYPE_CHECKBOX:
                fillCheckField(writer, model, oldFieldName, newFieldName, propertyName, fieldRect);
                break;
            case AcroFields.FIELD_TYPE_RADIOBUTTON:
                fillRadioField(fields, writer, model, oldFieldName, newFieldName, propertyName, fieldItem, fieldRect);
                break;
        }
    }

    /***
   * Fill a field of type Check 
   */
    private void fillCheckField(PdfWriter writer, Model model, String oldFieldName, String newFieldName, String propertyName, Rectangle fieldRect) throws IOException, DocumentException {
        logger.debug("fillCheckField(%s, %s, %s, %s, %s, %s)", writer, model, oldFieldName, newFieldName, propertyName, fieldRect);
        Boolean fieldValue = model.isPropertyAString(oldFieldName) ? Boolean.parseBoolean(model.getPropertyAsString(propertyName)) : false;
        RadioCheckField radioCheckField = new RadioCheckField(writer, fieldRect, newFieldName, Boolean.toString(true));
        radioCheckField.setCheckType(RadioCheckField.TYPE_CHECK);
        radioCheckField.setChecked(fieldValue);
        writer.addAnnotation(radioCheckField.getCheckField());
    }

    /***
   * Fill a field of type Radio 
   */
    private void fillRadioField(AcroFields fields, PdfWriter writer, Model model, String oldFieldName, String newFieldName, String propertyName, AcroFields.Item fieldItem, Rectangle fieldRect) throws IOException, DocumentException {
        logger.debug("fillRadioField(%s, %s, %s, %s, %s, %s, %s, %s)", fields, writer, model, oldFieldName, newFieldName, propertyName, fieldItem, fieldRect);
        RadioCheckField radioCheckField = new RadioCheckField(writer, fieldRect, newFieldName, null);
        radioCheckField.setCheckType(RadioCheckField.TYPE_CIRCLE);
        PdfFormField radioGroup = radioCheckField.getRadioGroup(true, false);
        String fieldValue = model.isPropertyAString(propertyName) ? model.getPropertyAsString(propertyName) : null;
        ListIterator<PdfObject> kidsIterator = fieldItem.getMerged(0).getAsArray(PdfName.KIDS).listIterator();
        int i = 0;
        while (kidsIterator.hasNext()) {
            PdfDictionary radioDictionary = (PdfDictionary) PdfReader.getPdfObject((PdfObject) kidsIterator.next());
            PdfArray kidRect = radioDictionary.getAsArray(PdfName.RECT);
            Iterator<?> iterator = radioDictionary.getAsDict(PdfName.AP).getAsDict(PdfName.N).getKeys().iterator();
            while (iterator.hasNext()) {
                String iterValue = PdfName.decodeName(iterator.next().toString());
                if (!iterValue.equals("Off")) {
                    radioCheckField.setOnValue(iterValue);
                    radioCheckField.setChecked(iterValue.equals(fieldValue));
                    break;
                }
            }
            radioCheckField.setBox(new Rectangle(kidRect.getAsNumber(0).floatValue() - fieldRect.getLeft(), kidRect.getAsNumber(3).floatValue() - fieldRect.getBottom(), kidRect.getAsNumber(2).floatValue() - fieldRect.getRight(), kidRect.getAsNumber(1).floatValue() - fieldRect.getTop()));
            PdfFormField kidField = radioCheckField.getRadioField();
            radioGroup.addKid(kidField);
            i++;
        }
        writer.addAnnotation(radioGroup);
    }

    /***
   * Fill a field of type Text 
   * @throws FactoryConfigurationError 
   * @throws XMLStreamException 
   */
    private void fillTextField(AcroFields fields, PdfWriter writer, Rectangle fieldRect, Model model, String oldFieldName, String newFieldName, String propertyName) throws IOException, DocumentException, XMLStreamException, FactoryConfigurationError {
        logger.debug("fillTextField(%s, %s, %s, %s, %s, %s, %s)", fields, writer, model, oldFieldName, newFieldName, propertyName, fieldRect);
        TextField textField = new TextField(writer, fieldRect, newFieldName);
        PdfDictionary fieldDict = fields.getFieldItem(oldFieldName).getWidget(0);
        fields.decodeGenericDictionary(fieldDict, textField);
        PdfNumber maxLength = fieldDict.getAsNumber(PdfName.MAXLEN);
        String text;
        if (model.isPropertyAString(propertyName)) {
            text = model.getPropertyAsString(propertyName);
            if ((maxLength != null) && (text.length() > maxLength.intValue())) text = text.substring(0, maxLength.intValue());
        } else text = "ERROR: Field not found in model.";
        this.document.add(new Chunk(oldFieldName));
        BaseFont baseFont = textField.getFont() != null ? textField.getFont() : BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, true);
        Phrase phrase = this.formatter.formatString(text).withFont(new Font(baseFont)).ofSize(textField.getFontSize()).asPhrase();
        ColumnText ct = new ColumnText(writer.getDirectContent());
        ct.setSimpleColumn(fieldRect.getLeft() + 3, fieldRect.getBottom() + 6, fieldRect.getRight(), fieldRect.getTop());
        ct.addText(phrase);
        ct.go();
    }

    private float calculateOffset(AcroFields fields, List<FieldInfo> fieldsInfos, PdfCollection collectionInfo) {
        logger.debug("calculateOffset(%s, %s, %s)", fields, fieldsInfos, collectionInfo);
        String field0 = null, field1 = null;
        for (FieldInfo fi : fieldsInfos) {
            if (fi.parts[1].equals(collectionInfo.getOffsetField())) {
                if (field0 == null) field0 = fi.originalName; else {
                    field1 = fi.originalName;
                    break;
                }
            }
        }
        if (field0 == null || field1 == null) throw new ApplicationException(String.format("Invalid info in collection %s, offset field %s not defined correctly.", collectionInfo.getId(), collectionInfo.getOffsetField()));
        PdfArray rect0 = (PdfArray) fields.getFieldItem(field0).getValue(0).get(PdfName.RECT);
        PdfArray rect1 = (PdfArray) fields.getFieldItem(field1).getValue(0).get(PdfName.RECT);
        float offset = 0.0f, bottomRect0, bottomRect1;
        bottomRect0 = rect0.getAsNumber(3).floatValue();
        bottomRect1 = rect1.getAsNumber(3).floatValue();
        if (bottomRect0 > bottomRect1) offset = bottomRect0 - bottomRect1; else offset = bottomRect1 - bottomRect0;
        return offset;
    }

    private class FieldInfo {

        public String[] parts;

        public String originalName;

        public PdfRectangle rect;
    }

    private Map<String, PdfCollection> parsePageInfo(String pageInfo) {
        logger.debug("parsePageInfo(%s)", pageInfo);
        try {
            Map<String, PdfCollection> parsedPageInfo = new HashMap<String, PdfCollection>();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            ByteArrayInputStream pageInfoInputStream = new ByteArrayInputStream(pageInfo.getBytes());
            Document doc = db.parse(pageInfoInputStream);
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression ruleXPath = xpath.compile(COLLECTIONS_XPATH);
            NodeList nodes = (NodeList) ruleXPath.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node n = nodes.item(i);
                NamedNodeMap attributes = n.getAttributes();
                PdfCollection collection = new PdfCollection();
                collection.setId(attributes.getNamedItem("id").getNodeValue());
                collection.setPageSize(Integer.parseInt(attributes.getNamedItem("pageSize").getNodeValue()));
                collection.setOffsetField(attributes.getNamedItem("offsetField").getNodeValue());
                Node multipageNode = attributes.getNamedItem("multipage");
                if (multipageNode != null) collection.setMultipage(Boolean.parseBoolean(multipageNode.getNodeValue()));
                parsedPageInfo.put(collection.getId(), collection);
            }
            return parsedPageInfo;
        } catch (Exception e) {
            throw new ApplicationException("Error parsing page info. Invalid template.");
        }
    }

    private void classifyFields(AcroFields fields) {
        logger.debug("classifyFields(%s)", fields);
        for (Object sField : fields.getFields().keySet()) {
            String fieldName = (String) sField;
            String[] parts = fieldName.split(":");
            if (parts.length > 1) {
                if (!this.collectionsFields.containsKey(parts[0])) this.collectionsFields.put(parts[0], new ArrayList<FieldInfo>());
                FieldInfo fieldInfo = new FieldInfo();
                fieldInfo.parts = parts;
                fieldInfo.originalName = fieldName;
                int fieldType = fields.getFieldType(fieldName);
                if (fieldType == AcroFields.FIELD_TYPE_TEXT) {
                    PdfArray rectArray = (PdfArray) fields.getFieldItem(fieldInfo.originalName).getValue(0).get(PdfName.RECT);
                    fieldInfo.rect = new PdfRectangle(rectArray.getAsNumber(0).floatValue(), rectArray.getAsNumber(1).floatValue(), rectArray.getAsNumber(2).floatValue(), rectArray.getAsNumber(3).floatValue());
                }
                this.collectionsFields.get(parts[0]).add(fieldInfo);
            } else {
                AcroFields.Item fieldItem = fields.getFieldItem(fieldName);
                for (int i = 0; i < fieldItem.size(); i++) {
                    Integer page = Integer.valueOf(fieldItem.getPage(i));
                    if (!this.plainFields.containsKey(page)) this.plainFields.put(page, new ArrayList<String>());
                    this.plainFields.get(page).add(fieldName);
                }
            }
        }
    }
}
