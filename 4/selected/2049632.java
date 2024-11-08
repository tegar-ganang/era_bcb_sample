package net.sf.webwarp.util.excel.openxml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import net.sf.webwarp.util.collection.DataModel;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * Object containing information for one odt-Document. Package private.
 */
class OpenXmlDocumentImpl implements OpenXmlDocument, Cloneable {

    private static final Logger log = Logger.getLogger(OpenXmlDocumentImpl.class);

    private static final String TEMPLATE_RESOURCE = "sheetTemplate.xml";

    private static final int BUFFER_SIZE = 10 * 1024;

    private byte[] fileData;

    /** Map from sheet name to document. */
    private TreeMap<String, Document> worksheetMap = new TreeMap<String, Document>();

    /** File name of the workbook xml. */
    private static final String WORKBOOK_NAME = "xl/workbook.xml";

    /** The workbook.xml. */
    private Document workbook;

    private Map<Integer, Element> workbookSheetElementsBySheetId;

    private BidiMap workbookSheetIdsByName;

    private boolean workbookChanged = false;

    /**
     * Create new OpenDocument from file data. Package private, use OpenDocumentUtil as factory.
     */
    OpenXmlDocumentImpl(byte[] fileData) {
        if (fileData == null) {
            throw new NullPointerException("fileData");
        }
        if (fileData.length == 0) {
            throw new IllegalArgumentException("zero length fileData");
        }
        if (log.isTraceEnabled()) {
            log.trace("File date length = " + fileData.length);
        }
        this.fileData = fileData;
        readData();
        parseWorkbook();
    }

    /** Read content from fileData. */
    private void readData() {
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(fileData));
        try {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                if (zipEntry.getName().equals(WORKBOOK_NAME)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Load workbook entry.");
                    }
                    workbook = readDocumentFromStream(zipInputStream);
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        } catch (IOException ex) {
            log.error("Exception while reading worksheet", ex);
            throw new IllegalArgumentException("Exception reading worksheet, fileData is probably not an open xml file.", ex);
        } catch (DocumentException ex) {
            log.error("Exception while parsing worksheet", ex);
            throw new IllegalArgumentException(WORKBOOK_NAME + " does not seem to be a valid xml", ex);
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }
        if (workbook == null) {
            log.error("No workbook entry found.");
            throw new IllegalArgumentException(WORKBOOK_NAME + " is not available, fileData is probably not an open xml file.");
        }
    }

    /**
     * Read required information from workbook.
     */
    @SuppressWarnings("unchecked")
    private void parseWorkbook() {
        Element workbookElement = workbook.getRootElement();
        List<Element> sheetElements = workbookElement.element("sheets").elements("sheet");
        workbookSheetElementsBySheetId = new HashMap<Integer, Element>(sheetElements.size());
        workbookSheetIdsByName = new DualHashBidiMap();
        for (Element sheetElement : sheetElements) {
            String name = sheetElement.attributeValue("name");
            String sheetIdString = sheetElement.attributeValue("sheetId");
            Integer sheetId = Integer.valueOf(sheetIdString);
            workbookSheetElementsBySheetId.put(sheetId, sheetElement);
            workbookSheetIdsByName.put(name, sheetId);
        }
    }

    /**
     * @see ch.orcasys.util.excel.openxml.OpenXmlDocument#writeDataSheet(ch.orcasys.util.collection.DataModel, int)
     */
    public void setDataSheet(int sheetNumber, DataModel dataModel) {
        if (sheetNumber < 1) {
            throw new IllegalArgumentException("sheetNumber must be >= 1, is " + sheetNumber);
        }
        Document worksheet;
        try {
            SAXReader reader = new SAXReader();
            worksheet = reader.read(this.getClass().getResourceAsStream(TEMPLATE_RESOURCE));
        } catch (DocumentException e) {
            throw new RuntimeException("Unexpected DocumentExcpetion while reading template " + TEMPLATE_RESOURCE, e);
        }
        Element root = worksheet.getRootElement();
        Element sheetData = root.element("sheetData");
        Element rowElement = sheetData.addElement("row");
        for (String columnName : dataModel.getColumnNames()) {
            addCell(rowElement, columnName);
        }
        for (Iterator<DataModel.RowObject> iter = dataModel.iterator(); iter.hasNext(); ) {
            DataModel.RowObject rowObject = iter.next();
            rowElement = sheetData.addElement("row");
            for (int i = 0; i < rowObject.getColumnCount(); i++) {
                addCell(rowElement, rowObject.getValueAt(i));
            }
        }
        String sheetName = "xl/worksheets/sheet" + sheetNumber + ".xml";
        worksheetMap.put(sheetName, worksheet);
    }

    private void addCell(Element rowElement, Object cellObject) {
        Element cellElement = rowElement.addElement("c");
        if (cellObject == null) {
            return;
        }
        String text;
        if (cellObject instanceof String) {
            cellElement.addAttribute("t", "str");
            text = (String) cellObject;
        } else if (cellObject instanceof Number) {
            cellElement.addAttribute("t", "n");
            text = String.valueOf(((Number) cellObject).doubleValue());
        } else if (cellObject instanceof Boolean) {
            cellElement.addAttribute("t", "b");
            if (((Boolean) cellObject).booleanValue()) {
                text = "1";
            } else {
                text = "0";
            }
        } else {
            Double excelDate = OpenXmlUtil.toExcelDate(cellObject);
            if (excelDate != null) {
                cellElement.addAttribute("t", "n");
                text = excelDate.toString();
            } else {
                log.warn("Unknown data type found, add as String: " + cellObject.getClass().getName());
                cellElement.addAttribute("t", "str");
                text = cellObject.toString();
            }
        }
        Element valueElement = cellElement.addElement("v");
        valueElement.setText(text);
    }

    private void replaceData() {
        if (worksheetMap.isEmpty() && workbookChanged == false) {
            return;
        }
        byte[] buffer = new byte[BUFFER_SIZE];
        ByteArrayOutputStream out = new ByteArrayOutputStream(2 * fileData.length);
        ZipOutputStream zipOutputStream = new ZipOutputStream(out);
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(fileData));
        try {
            zipOutputStream.putNextEntry(new ZipEntry(WORKBOOK_NAME));
            writeDocumentToStream(workbook, zipOutputStream);
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                if (worksheetMap.containsKey(zipEntry.getName())) {
                    if (log.isDebugEnabled()) {
                        log.debug("Removed old sheet: " + zipEntry.getName());
                    }
                } else if (WORKBOOK_NAME.equals(zipEntry.getName())) {
                    if (log.isDebugEnabled()) {
                        log.debug("Removed old workbook: " + zipEntry.getName());
                    }
                } else {
                    zipOutputStream.putNextEntry(new ZipEntry(zipEntry.getName()));
                    int read = zipInputStream.read(buffer);
                    while (read > -1) {
                        zipOutputStream.write(buffer, 0, read);
                        read = zipInputStream.read(buffer);
                    }
                }
                zipEntry = zipInputStream.getNextEntry();
            }
            for (Map.Entry<String, Document> entry : worksheetMap.entrySet()) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                writeDocumentToStream(entry.getValue(), zipOutputStream);
            }
        } catch (IOException ex) {
            log.error("Exception while adding or replacing worksheet", ex);
            throw new IllegalArgumentException("fileData is probably not an open xml file: " + ex);
        } finally {
            IOUtils.closeQuietly(zipInputStream);
            IOUtils.closeQuietly(zipOutputStream);
        }
        byte[] result = out.toByteArray();
        if (log.isDebugEnabled()) {
            log.debug("Added or replace worksheet. File data changed from " + fileData.length + " bytes to " + result.length + " bytes.");
        }
        fileData = result;
        worksheetMap.clear();
        workbookChanged = false;
    }

    private Document readDocumentFromStream(InputStream in) throws IOException, DocumentException {
        String content = IOUtils.toString(in, "UTF-8");
        return DocumentHelper.parseText(content);
    }

    private void writeDocumentToStream(Document worksheet, OutputStream out) throws IOException {
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(format);
        writer.setOutputStream(out);
        writer.write(worksheet);
        writer.flush();
    }

    public byte[] getFileData() {
        replaceData();
        return fileData;
    }

    public OpenXmlDocument cloneDocument() {
        try {
            return (OpenXmlDocumentImpl) this.clone();
        } catch (CloneNotSupportedException e) {
            log.fatal("Unexpected " + e, e);
            throw new RuntimeException("Unexpected " + e, e);
        }
    }

    public Integer getSheetNumberForName(String sheetName) {
        return (Integer) workbookSheetIdsByName.get(sheetName);
    }

    public String getSheetName(int sheetNumber) {
        return (String) workbookSheetIdsByName.getKey(sheetNumber);
    }

    public void setSheetName(int sheetNumberInt, String sheetName) {
        Integer sheetNumber = Integer.valueOf(sheetNumberInt);
        Element sheetElement = workbookSheetElementsBySheetId.get(sheetNumber);
        if (sheetElement == null) {
            throw new IllegalArgumentException("No such sheet number: " + sheetNumber);
        }
        if (!OpenXmlUtil.isValidSheetName(sheetName)) {
            throw new IllegalArgumentException("Not a valid sheet name: '" + sheetName + "'");
        }
        String oldSheetName = (String) workbookSheetIdsByName.getKey(sheetNumber);
        if (oldSheetName.equals(sheetName)) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Rename sheet " + sheetNumber + " from '" + oldSheetName + "' to '" + sheetName + "'");
        }
        workbookSheetIdsByName.put(sheetName, sheetNumber);
        sheetElement.attribute("name").setValue(sheetName);
        workbookChanged = true;
    }

    public int getSheetCount() {
        return workbookSheetElementsBySheetId.size();
    }
}
