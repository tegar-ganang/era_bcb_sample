package oojmerge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import oojmerge.barcoder.BarcodeException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Copyright &copy; medge - 2010
 * Version 1.0
 * @author medge
 */
public class OOJMerge {

    public static final long serialVersionUID = VersionControl.serialVersionUID;

    private static final String MAIL_MERGE_PAGE_BREAK = "<style:style style:name=\"mailMergePageBreak\" style:family=\"paragraph\" style:parent-style-name=\"Standard\">" + "<style:paragraph-properties fo:break-after=\"page\" />" + "</style:style>";

    private static final String FIELD_START = "&lt;&lt;";

    private static final String FIELD_END = "&gt;&gt;";

    private static final String NEXT_RECORD_FIELD = "nextrecord";

    private static final String MAIL_MERGE_STYLES = "office:automatic-styles";

    private static final String MAIL_MERGE_PAGE = "<text:p text:style-name=\"mailMergePageBreak\" />";

    private static final String OFFICE_TEXT = "office:text";

    private static final String CONTENT_FILE = "content.xml";

    private static final String STYLES_FILE = "styles.xml";

    private static final String PICTURES_DIR = "Pictures/";

    private static final String MANIFEST_FILE = "manifest.xml";

    private static final String FOR_LOOP_START = "starttable";

    private static final String FOR_LOOP_END = FIELD_START + "endtable" + FIELD_END;

    private static final String BRANCH_START = "if";

    private static final String BRANCH_END = FIELD_START + "endif" + FIELD_END;

    private static final String FRAME_NAME_START = "draw:name=\"Frame";

    private static final String IMAGE_FRAME_WIDTH = "svg:width=\"";

    private static final String IMAGE_FRAME_HEIGHT = "svg:height=\"";

    private static final String IMAGE_NAME_START = "draw:name=\"";

    private static final String IMAGE_BARCODE_START = "mmbarcode_";

    private static final String IMAGE_PICTURE_START = "mmimage_";

    private static final String IMAGE_BARCODENAME_START = IMAGE_NAME_START + IMAGE_BARCODE_START;

    private static final String IMAGE_PICTURENAME_START = IMAGE_NAME_START + IMAGE_PICTURE_START;

    private static final String IMAGE_HREF_START = "xlink:href=\"";

    private static final String IGNORE_TO = "text:sequence-decls";

    private static final String OFFICE_TEXT_TO_ADD = "text:use-soft-page-breaks";

    private static final String FULL_OFFICE_TEXT_HEADER = "<office:text text:use-soft-page-breaks=\"true\">";

    private int barcodeImageSizeFactor = 100;

    public OOJMerge() {
    }

    private void moveFile(InputStream i, OutputStream o) throws IOException {
        int r = 0;
        byte[] b = new byte[1024];
        do {
            r = i.read(b);
            if (r > 0) o.write(b, 0, r);
        } while (r > 0);
    }

    private void addOurPageBreak(XMLContent officeDoc) {
        XMLContent styles = officeDoc.findContentCalled(MAIL_MERGE_STYLES);
        styles.expandElement();
        XMLContent mmPageBreak = new XMLContent();
        mmPageBreak.setContent(MAIL_MERGE_PAGE_BREAK);
        styles.addSubContent(mmPageBreak);
    }

    private XMLContent cloneDocument(XMLContent officeDoc, Element dataRootElement) {
        XMLContent officeText = officeDoc.findContentCalled(OFFICE_TEXT);
        int numberOfRecords = XMLUtil.getChildren(dataRootElement).size();
        int nextRecordCount = officeText.count(NEXT_RECORD_FIELD);
        int loopCount = numberOfRecords - 1;
        if (nextRecordCount > 0) loopCount = numberOfRecords / nextRecordCount;
        List<XMLContent> cloned = new ArrayList<XMLContent>();
        for (int i = 0; i < loopCount; i++) {
            XMLContent pageBreak = new XMLContent();
            pageBreak.setHeader(MAIL_MERGE_PAGE);
            XMLContent officeDocContent = officeText.clone();
            int start = 1;
            int end = officeDocContent.getSubContents().size();
            XMLContent ignoreTo = officeDocContent.findContentCalled(IGNORE_TO);
            if (ignoreTo != null) start = officeDocContent.getIndexOf(ignoreTo) + 1;
            cloned.add(pageBreak);
            cloned.addAll(officeDocContent.getSubContents().subList(start, end));
        }
        officeText.addSubContent(cloned);
        return officeText;
    }

    private void changeEntries(XMLContent entry, Element record) {
        String text = entry.getText();
        if (text != null) {
            boolean replace = false;
            int fldStart = text.indexOf(FIELD_START);
            while (fldStart != -1) {
                int fldEnd = text.indexOf(FIELD_END, fldStart);
                if (fldEnd < fldStart) fldEnd = text.length();
                String fieldName = text.substring(fldStart + FIELD_START.length(), fldEnd);
                text = text.replace(FIELD_START + fieldName + FIELD_END, findNewValue(record, fieldName));
                fldStart = text.indexOf(FIELD_START);
                replace = true;
            }
            fldStart = text.indexOf(FIELD_END);
            while (fldStart != -1) {
                text = text.replace(FIELD_END, "");
                fldStart = text.indexOf(FIELD_END);
                replace = true;
            }
            if (replace) entry.setText(text);
        }
    }

    private int calcPixels(String sizeString) {
        double factor = 1;
        if (sizeString.endsWith("cm")) factor = barcodeImageSizeFactor / 2.54;
        if (sizeString.endsWith("mm")) factor = barcodeImageSizeFactor / 25.4;
        if (sizeString.endsWith("in")) factor = barcodeImageSizeFactor;
        double size = Double.parseDouble(sizeString.substring(0, sizeString.length() - 2));
        return (int) Math.round(size * factor);
    }

    private String addImage(String fieldName, Element record, String mergedFieldName, List<ImageToMerge> imagesToMerge) {
        List<Element> imageRecords = XMLUtil.getChildren(record, "imagefield");
        for (int i = 0; i < imageRecords.size(); i++) {
            Element imageRecord = (Element) imageRecords.get(i);
            String name = XMLUtil.getStringAttr(imageRecord, "name");
            if (name != null && fieldName.endsWith(name)) {
                String fileName = XMLUtil.getTextTrim(imageRecord);
                File file = new File(fileName);
                int extIndx = fileName.lastIndexOf(".");
                String mergedFileName;
                if (extIndx != -1) mergedFileName = mergedFieldName + fileName.substring(extIndx); else mergedFileName = mergedFieldName + ".gif";
                imagesToMerge.add(new ImageToMerge(mergedFileName, file));
                return mergedFileName;
            }
        }
        return null;
    }

    private String addBarcode(String fieldName, Element record, String mergedFieldName, String imgWidth, String imgHeight, List<ImageToMerge> imagesToMerge) {
        List<Element> barcodeRecords = XMLUtil.getChildren(record, "barcodefield");
        for (int i = 0; i < barcodeRecords.size(); i++) {
            Element barcodeRecord = (Element) barcodeRecords.get(i);
            String name = XMLUtil.getStringAttr(barcodeRecord, "name");
            if (name != null && fieldName.equals(name)) {
                String barcodeType = XMLUtil.getStringAttr(barcodeRecord, "type", "code39");
                boolean includeCaption = XMLUtil.getBooleanAttr(barcodeRecord, "caption", true);
                boolean checkDigit = XMLUtil.getBooleanAttr(barcodeRecord, "checkdigit", true);
                String barcodeValue = XMLUtil.getTextTrim(barcodeRecord);
                String mergedFileName = mergedFieldName + ".gif";
                imagesToMerge.add(new ImageToMerge(mergedFileName, barcodeValue, barcodeType, calcPixels(imgWidth), calcPixels(imgHeight), includeCaption, checkDigit));
                return mergedFileName;
            }
        }
        return null;
    }

    private String getFieldName(String fullName) {
        int start = fullName.indexOf("_");
        int end = fullName.lastIndexOf("_");
        if (end != start) return fullName.substring(start + 1, end);
        return fullName.substring(start + 1);
    }

    private String getQuotedValue(String header, String attrName) {
        int start = header.indexOf(attrName);
        if (start != -1) {
            start += attrName.length();
            int end = header.indexOf("\"", start);
            if (end != -1) return header.substring(start, end);
        }
        return "";
    }

    private void changePictureHeader(XMLContent imageEntry, String mergedFileName) {
        XMLContent pictureEntry = imageEntry.findContentHeaded(IMAGE_HREF_START);
        if (pictureEntry != null) {
            String pictureHeader = pictureEntry.getHeader();
            int filenameStart = pictureHeader.indexOf("/") + 1;
            int filenameEnd = pictureHeader.indexOf("\"", filenameStart);
            String newPictureHeader = pictureHeader.substring(0, filenameStart) + mergedFileName + pictureHeader.substring(filenameEnd);
            pictureEntry.setHeader(newPictureHeader);
        }
    }

    private void processImages(List<XMLContent> fullContent, Element record, int recordNumber, int contIndex, List<ImageToMerge> imagesToMerge) {
        List<XMLContent> imageEntries = new ArrayList<XMLContent>();
        List<XMLContent> barcodeEntries = new ArrayList<XMLContent>();
        int subIndex = contIndex;
        boolean finished = false;
        int imageIndx = 0;
        while (!finished) {
            XMLContent c = fullContent.get(subIndex);
            String text = c.getText();
            String header = c.getHeader();
            if (text != null && text.contains(FIELD_START + NEXT_RECORD_FIELD + FIELD_END)) finished = true;
            if (header != null && header.equals(MAIL_MERGE_PAGE)) finished = true;
            if (!finished) {
                if (header.contains(IMAGE_PICTURENAME_START)) imageEntries.add(c);
                if (header.contains(IMAGE_BARCODENAME_START)) barcodeEntries.add(c);
            }
            subIndex++;
            finished |= subIndex >= fullContent.size();
        }
        for (XMLContent imageEntry : imageEntries) {
            String header = imageEntry.getHeader();
            int start = header.indexOf(IMAGE_PICTURE_START);
            int end = header.indexOf("\"", start + IMAGE_PICTURE_START.length());
            String fullName = header.substring(start, end);
            String fieldName = getFieldName(fullName);
            String newName = "xx_" + fieldName + "_" + recordNumber + "_" + imageIndx++;
            String mergedFileName = addImage(fieldName, record, newName, imagesToMerge);
            imageEntry.setHeader(header.replace(fullName, newName));
            if (mergedFileName == null) imageEntry.setActive(false); else changePictureHeader(imageEntry, mergedFileName);
        }
        for (XMLContent barcodeEntry : barcodeEntries) {
            String header = barcodeEntry.getHeader();
            int start = header.indexOf(IMAGE_BARCODE_START);
            int end = header.indexOf("\"", start + IMAGE_BARCODE_START.length());
            String fullName = header.substring(start, end);
            String fieldName = getFieldName(fullName);
            String imgWidth = getQuotedValue(header, IMAGE_FRAME_WIDTH);
            String imgHeight = getQuotedValue(header, IMAGE_FRAME_HEIGHT);
            String newName = "xx_" + fieldName + "_" + recordNumber + "_" + imageIndx++;
            String mergedFileName = addBarcode(fieldName, record, newName, imgWidth, imgHeight, imagesToMerge);
            barcodeEntry.setHeader(header.replace(fullName, newName));
            if (mergedFileName == null) barcodeEntry.setActive(false); else changePictureHeader(barcodeEntry, mergedFileName);
        }
    }

    private boolean startAndEndTagsLineUp(String text, int start, int end) {
        int startTagCount = 0;
        int tagCount = 0;
        int indx = -1;
        while ((indx = text.indexOf("<", indx + 1)) != -1) {
            startTagCount++;
            tagCount++;
        }
        indx = -1;
        while ((indx = text.indexOf(">", indx + 1)) != -1) startTagCount--;
        indx = -1;
        while ((indx = text.indexOf("/>", indx + 1)) != -1) tagCount--;
        while ((indx = text.indexOf("</", indx + 1)) != -1) tagCount -= 2;
        return startTagCount == 0 && tagCount == 0;
    }

    private String handleSameElementLoops(List<XMLContent> fullContent, int contIndx, Element record, String fieldName) {
        List<Element> tables = XMLUtil.getChildren(record, "tabledata");
        XMLContent tableFull = fullContent.get(contIndx);
        String text = tableFull.getText();
        String tableTag = FIELD_START + fieldName + FIELD_END;
        int startTag = text.indexOf(tableTag);
        int start = startTag + tableTag.length();
        tableTag = FOR_LOOP_END;
        int end = text.indexOf(tableTag) + FOR_LOOP_END.length();
        while (!startAndEndTagsLineUp(text.substring(start, end), start, end)) end++;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < tables.size(); i++) {
            Element table = (Element) tables.get(i);
            String name = XMLUtil.getStringAttr(table, "name");
            if (name != null && fieldName.endsWith(name)) {
                List<Element> tableRows = XMLUtil.getChildren(table, "tablerow");
                int rowCount = tableRows.size();
                for (int row = 0; row < rowCount; row++) {
                    String tableContent = text.substring(start, end);
                    Element tableRow = (Element) tableRows.get(row);
                    int fldStart = tableContent.indexOf(FIELD_START);
                    while (fldStart != -1) {
                        int fldEnd = tableContent.indexOf(FIELD_END, fldStart);
                        String tableFieldName = tableContent.substring(fldStart + FIELD_START.length(), fldEnd);
                        tableContent = tableContent.replace(FIELD_START + tableFieldName + FIELD_END, findNewValue(tableRow, tableFieldName));
                        fldStart = tableContent.indexOf(FIELD_START);
                    }
                    buf.append(tableContent);
                }
            }
        }
        return text.substring(0, startTag) + buf.toString() + text.substring(end);
    }

    private int handleLoops(List<XMLContent> fullContent, int startLoop, int endLoop, Element record, String fieldName) {
        List<Element> tables = XMLUtil.getChildren(record, "tabledata");
        XMLContent tableStart = fullContent.get(startLoop);
        String text = tableStart.getText();
        String tableTag = FIELD_START + fieldName + FIELD_END;
        int start = text.indexOf(tableTag);
        tableStart.setText(text.substring(0, start) + text.substring(start + tableTag.length()));
        XMLContent tableEnd = fullContent.get(endLoop);
        text = tableEnd.getText();
        tableTag = FOR_LOOP_END;
        start = text.indexOf(tableTag);
        tableEnd.setText(text.substring(0, start) + text.substring(start + tableTag.length()));
        for (int i = 0; i < tables.size(); i++) {
            Element table = (Element) tables.get(i);
            String name = XMLUtil.getStringAttr(table, "name");
            if (name != null && fieldName.endsWith(name)) {
                List<Element> tableRows = XMLUtil.getChildren(table, "tablerow");
                int rowCount = tableRows.size();
                for (int row = 0; row < rowCount; row++) {
                    Element tableRow = (Element) tableRows.get(row);
                    for (int loop = startLoop; loop <= endLoop; loop++) {
                        XMLContent insideTable = fullContent.get(loop);
                        if (insideTable.getLevel() == tableStart.getLevel()) {
                            XMLContent tableStartParent = tableStart.getParent();
                            int insertIndex = tableStartParent.getIndexOf(tableStart);
                            XMLContent newEntry = insideTable.clone();
                            tableStartParent.addSubContent(insertIndex, newEntry);
                            changeEntries(newEntry, tableRow);
                            List<XMLContent> newSubEntries = newEntry.getAllContent();
                            for (XMLContent newSubEntry : newSubEntries) changeEntries(newSubEntry, tableRow);
                        }
                    }
                }
            }
        }
        for (int i = startLoop + 1; i <= endLoop; i++) fullContent.get(i).setActive(false);
        return endLoop;
    }

    private void handleBranches(List<XMLContent> fullContent, int startIf, int endIf, Element record, String fieldName) {
        boolean keep = false;
        List<Element> branches = XMLUtil.getChildren(record, "branch");
        for (int i = 0; i < branches.size(); i++) {
            Element branch = (Element) branches.get(i);
            String name = XMLUtil.getStringAttr(branch, "name");
            if (name != null && fieldName.endsWith(name)) {
                keep = XMLUtil.getBooleanAttr(branch, "set");
            }
        }
        if (!keep) {
            if (startIf == endIf) {
                XMLContent c = fullContent.get(startIf);
                String text = c.getText();
                int start = text.indexOf(FIELD_START + fieldName);
                int end = text.indexOf(BRANCH_END, start);
                c.setText(text.substring(0, start) + text.substring(end + BRANCH_END.length()));
            } else {
                for (int i = startIf; i <= endIf; i++) fullContent.get(i).setActive(false);
            }
        }
    }

    private String getNewValue(Element fieldElement) {
        if (fieldElement == null) return "";
        boolean verbatim = XMLUtil.getBooleanAttr(fieldElement, "verbatim");
        String res = XMLUtil.getTextTrim(fieldElement);
        for (XMLReservedReplacer xmlReplace : XMLReservedReplacer.XML_REPLACERS) res = xmlReplace.replace(res, verbatim);
        return res;
    }

    private String findNewValue(Element record, String fieldName) {
        if (record != null) {
            List<Element> fields = XMLUtil.getChildren(record, "field");
            for (int i = 0; i < fields.size(); i++) {
                Element field = (Element) fields.get(i);
                String name = XMLUtil.getStringAttr(field, "name");
                if (name != null && name.equalsIgnoreCase(fieldName)) return getNewValue(field);
            }
        }
        return "";
    }

    private void handleMerge(XMLContent officeText, Element dataRootElement, List<ImageToMerge> imagesToMerge) {
        boolean processedImages = false;
        int recordNumber = 0;
        List<XMLContent> fullContent = officeText.getAllContent();
        int contIndex = 1;
        while (contIndex < fullContent.size()) {
            XMLContent c = fullContent.get(contIndex);
            List<Element> records = XMLUtil.getChildren(dataRootElement, "record");
            Element record;
            if (recordNumber < records.size()) record = records.get(recordNumber); else record = null;
            if (record != null) {
                if (!processedImages) {
                    processImages(fullContent, record, recordNumber, contIndex, imagesToMerge);
                    processedImages = true;
                }
                String text = c.getText();
                String header = c.getHeader();
                if (text != null) {
                    boolean replace = false;
                    int fldStart = text.indexOf(FIELD_START);
                    while (fldStart != -1) {
                        int fldEnd = text.indexOf(FIELD_END, fldStart);
                        if (fldEnd == -1) {
                            break;
                        }
                        String fieldName = text.substring(fldStart + FIELD_START.length(), fldEnd);
                        if (fieldName.equalsIgnoreCase(NEXT_RECORD_FIELD)) {
                            recordNumber++;
                            processedImages = false;
                        } else if (fieldName.startsWith(FOR_LOOP_START)) {
                            int endLoop = -1;
                            int nextIndex = contIndex;
                            while (nextIndex < fullContent.size() && endLoop == -1) {
                                XMLContent next = fullContent.get(nextIndex);
                                String nextText = next.getText();
                                if (nextText != null && nextText.contains(FOR_LOOP_END)) endLoop = nextIndex;
                                nextIndex++;
                            }
                            if (endLoop != -1) {
                                if (contIndex == endLoop) text = handleSameElementLoops(fullContent, contIndex, record, fieldName); else contIndex = handleLoops(fullContent, contIndex, endLoop, record, fieldName);
                            }
                        } else if (fieldName.startsWith(BRANCH_START)) {
                            int endIf = -1;
                            int nextIndex = contIndex;
                            while (nextIndex < fullContent.size() && endIf == -1) {
                                XMLContent next = fullContent.get(nextIndex);
                                String nextText = next.getText();
                                if (nextText != null && nextText.contains(BRANCH_END)) endIf = nextIndex;
                                nextIndex++;
                            }
                            if (endIf != -1) handleBranches(fullContent, contIndex, endIf, record, fieldName);
                        }
                        text = text.replace(FIELD_START + fieldName + FIELD_END, findNewValue(record, fieldName));
                        fldStart = text.indexOf(FIELD_START);
                        replace = true;
                    }
                    if (replace) c.setText(text);
                }
                if (header != null && header.equals(MAIL_MERGE_PAGE)) {
                    processedImages = false;
                    recordNumber++;
                }
            } else {
                c.setActive(false);
            }
            contIndex++;
        }
    }

    private void repairFrameCount(XMLContent officeText) {
        int frameNumber = 0;
        List<XMLContent> fullContent = officeText.getAllContent();
        for (XMLContent c : fullContent) {
            String header = c.getHeader();
            if (header != null && header.contains(FRAME_NAME_START)) {
                int frameStart = header.indexOf(FRAME_NAME_START) + FRAME_NAME_START.length();
                int frameEnd = header.indexOf("\"", frameStart);
                String newHeader = header.substring(0, frameStart) + ++frameNumber + header.substring(frameEnd);
                c.setHeader(newHeader);
            }
        }
    }

    public String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) writer.write(buffer, 0, n);
            } finally {
            }
            return writer.toString();
        } else return "";
    }

    private XMLContent mergeFile(InputStream is, OutputStream os, Element dataRootElement, List<ImageToMerge> imagesToMerge) throws IOException {
        StringBuilder buf = new StringBuilder(convertStreamToString(is));
        int lineBreak = buf.indexOf("\n");
        int firstElement = buf.indexOf("<", lineBreak);
        String fHeader = buf.substring(0, lineBreak);
        String lineSep = buf.substring(lineBreak, firstElement);
        String content = buf.substring(firstElement);
        XMLContent officeDoc = new XMLContent();
        officeDoc.setContent(content);
        addOurPageBreak(officeDoc);
        XMLContent officeText = cloneDocument(officeDoc, dataRootElement);
        if (!officeText.getHeader().contains(OFFICE_TEXT_TO_ADD)) officeText.setHeader(FULL_OFFICE_TEXT_HEADER);
        handleMerge(officeText, dataRootElement, imagesToMerge);
        repairFrameCount(officeText);
        os.write(fHeader.getBytes());
        os.write(lineSep.getBytes());
        os.write(officeDoc.getContent().getBytes());
        return officeText;
    }

    private String mergeSingleFile(String fromFileName, String toFileName, Element dataRootElement) throws FileNotFoundException, IOException, BarcodeException {
        List<ImageToMerge> imagesToMerge = new ArrayList<ImageToMerge>();
        List<ImageInFile> imagesInFile = new ArrayList<ImageInFile>();
        StoredManifest manifest = null;
        StoredStyle style = null;
        FileInputStream fis = new FileInputStream(fromFileName);
        FileOutputStream fos = new FileOutputStream(toFileName);
        ZipInputStream zi = new ZipInputStream(fis);
        ZipOutputStream zo = new ZipOutputStream(fos);
        ZipEntry ze = zi.getNextEntry();
        XMLContent officeText = null;
        while (ze != null) {
            if (ze.getName().endsWith(CONTENT_FILE)) {
                ZipEntry ze1 = new ZipEntry(ze.getName().replace(File.pathSeparatorChar, '/'));
                zo.putNextEntry(ze1);
                officeText = mergeFile(zi, zo, dataRootElement, imagesToMerge);
                for (ImageToMerge imageToMerge : imagesToMerge) imageToMerge.addToODF(zo);
            } else if (ze.getName().contains(PICTURES_DIR)) {
                imagesInFile.add(new ImageInFile(ze.getName(), zi));
            } else if (ze.getName().endsWith(MANIFEST_FILE)) {
                manifest = new StoredManifest(zi);
            } else if (ze.getName().endsWith(STYLES_FILE)) {
                ZipEntry ze1 = new ZipEntry(ze.getName().replace(File.pathSeparatorChar, '/'));
                zo.putNextEntry(ze1);
                style = new StoredStyle(zi);
                zo.write(style.getData());
            } else {
                ZipEntry ze1 = new ZipEntry(ze.getName().replace(File.pathSeparatorChar, '/'));
                zo.putNextEntry(ze1);
                moveFile(zi, zo);
            }
            zo.flush();
            ze = zi.getNextEntry();
        }
        if (officeText != null) {
            List<String> fileNames = new ArrayList<String>();
            for (ImageInFile imageInFile : imagesInFile) {
                String fileName = imageInFile.getFileName();
                if (officeText.containsImageFile(fileName) || (style != null && style.containsImageFile(fileName))) {
                    ZipEntry ze1 = new ZipEntry(fileName.replace(File.pathSeparatorChar, '/'));
                    zo.putNextEntry(ze1);
                    zo.write(imageInFile.getData());
                    zo.flush();
                    fileNames.add(fileName);
                }
            }
            for (ImageToMerge imageToMerge : imagesToMerge) fileNames.add(imageToMerge.getImageName());
            if (manifest != null) {
                manifest.setAllPictures(fileNames);
                ZipEntry ze1 = new ZipEntry("META-INF/manifest.xml");
                zo.putNextEntry(ze1);
                zo.write(manifest.getData());
                zo.flush();
            }
        }
        zo.close();
        zi.close();
        fis.close();
        fos.close();
        return toFileName;
    }

    private String newToFileName(String toFileName, String filename) {
        StringBuilder buf = new StringBuilder();
        int p = toFileName.lastIndexOf('.');
        buf.append(toFileName.substring(0, p));
        buf.append("_");
        buf.append(filename);
        buf.append(toFileName.substring(p));
        return buf.toString();
    }

    private String[] mergeSeparateFiles(String fromFileName, String toFileName, Element dataRootElement) throws FileNotFoundException, IOException, ParserConfigurationException, BarcodeException {
        List<Element> records = XMLUtil.getChildren(dataRootElement, "record");
        int tally = 0;
        String[] toFileNames = new String[records.size()];
        for (Object obj : records) {
            Element el = (Element) obj;
            String filename = XMLUtil.getStringAttr(el, "filename");
            if (filename == null) return null;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document newDataDocument = builder.newDocument();
            Element newRootElement = newDataDocument.createElement("mailmerge");
            newRootElement.appendChild(newDataDocument.importNode(el, true));
            newDataDocument.appendChild(newRootElement);
            String newToFileName = newToFileName(toFileName, filename);
            toFileNames[tally++] = mergeSingleFile(fromFileName, newToFileName, newRootElement);
        }
        return toFileNames;
    }

    private String newFileName(String fromFileName) throws IOException {
        int i = 1;
        File f = null;
        StringBuilder buf = new StringBuilder();
        DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date d = new Date();
        do {
            buf.setLength(0);
            int p = fromFileName.lastIndexOf(".");
            buf.append(fromFileName.substring(0, p));
            buf.append("_");
            buf.append(df.format(d));
            buf.append("_");
            buf.append(i);
            buf.append(fromFileName.substring(p));
            i++;
            f = new File(buf.toString());
        } while (f.exists());
        return buf.toString();
    }

    public String[] merge(String fromFileName, String toFileName, Document dataDocument) throws ParserConfigurationException, FileNotFoundException, IOException, BarcodeException {
        Element dataRootElement = dataDocument.getDocumentElement();
        if (XMLUtil.getBooleanAttr(dataRootElement, "separatefile")) return mergeSeparateFiles(fromFileName, toFileName, dataRootElement);
        return new String[] { mergeSingleFile(fromFileName, toFileName, dataRootElement) };
    }

    public String[] merge(String fromFileName, Document dataDocument) throws ParserConfigurationException, FileNotFoundException, IOException, BarcodeException {
        return merge(fromFileName, newFileName(fromFileName), dataDocument);
    }

    public String[] merge(String fromFileName, String toFileName, String dataFileName) throws ParserConfigurationException, FileNotFoundException, IOException, SAXException, BarcodeException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        return merge(fromFileName, toFileName, parser.parse(new File(dataFileName)));
    }

    public String[] merge(String fromFileName, String dataFileName) throws ParserConfigurationException, FileNotFoundException, IOException, SAXException, BarcodeException {
        return merge(fromFileName, newFileName(fromFileName), dataFileName);
    }

    private static void showUsage() {
        System.out.println("Usage is:");
        System.out.println("java -cp jdom.jar" + File.pathSeparator + "OOJMerge.jar " + OOJMerge.class.getName() + " -f fromFileName [-t toFileName] -d dataFileName");
        System.out.println("where:");
        System.out.println("\tfromFileName is the template file");
        System.out.println("\ttoFileName is the result file - optional. Creates a default named file if missing.");
        System.out.println("\tdataFileName is the data xml file");
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            showUsage();
        } else {
            String fromFileName = null;
            String toFileName = null;
            String dataFileName = null;
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-f") && (i + 1) < args.length) fromFileName = args[i + 1]; else if (args[i].equals("-t") && (i + 1) < args.length) toFileName = args[i + 1]; else if (args[i].equals("-d") && (i + 1) < args.length) dataFileName = args[i + 1];
            }
            if (fromFileName != null && dataFileName != null) {
                OOJMerge merge = new OOJMerge();
                try {
                    String[] resultFiles = toFileName != null ? merge.merge(fromFileName, toFileName, dataFileName) : merge.merge(fromFileName, dataFileName);
                    for (String resultFile : resultFiles) System.out.println(resultFile);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                showUsage();
            }
        }
    }
}
