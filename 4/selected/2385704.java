package de.searchworkorange.indexcrawler.Document;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.poi.hpsf.MarkUnsupportedException;
import org.apache.poi.hpsf.NoPropertySetStreamException;
import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hpsf.UnexpectedPropertySetTypeException;
import org.apache.poi.hssf.record.RecordFormatException;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import de.searchworkorange.indexcrawler.Document.exception.CanNotSetDefaultFieldsException;
import de.searchworkorange.indexcrawler.Document.exception.NoInputStreamException;
import de.searchworkorange.indexcrawler.configuration.ConfigurationCollection;
import de.searchworkorange.indexcrawler.crawler.indexer.Indexer;
import de.searchworkorange.indexcrawler.fileDocument.exceptions.FileDocumentException;
import de.searchworkorange.lib.logger.LoggerCollection;

/**
 * 
 * @author Sascha Kriegesmann kriegesmann at vaxnet.de
 */
public class XLSContentDocument extends SimpleDocument {

    private static final boolean CLASSDEBUG = false;

    private static final boolean READ_ERROR_DEBUG = false;

    private Reader reader = null;

    private boolean isNOTEncrypted = true;

    private int PASSWORD_PROTECTED_MASK = 1;

    private SummaryInformation summaryInfo = null;

    private POIFSFileSystem poiFs = null;

    /**
     *
     * @param loggerCol
     * @param config
     * @param indexer
     * @param fileObject
     * @throws FileDocumentException
     */
    public XLSContentDocument(LoggerCollection loggerCol, ConfigurationCollection config, Indexer indexer, Object fileObject) throws FileDocumentException {
        super(loggerCol, config, indexer, fileObject);
    }

    private void createTemporaryNeededObjects() {
        try {
            poiFs = new POIFSFileSystem(getInputStream());
            try {
                Entry summaryEntry = poiFs.getRoot().getEntry(SummaryInformation.DEFAULT_STREAM_NAME);
                if (summaryEntry instanceof DocumentEntry) {
                    try {
                        PropertySet propSet = PropertySetFactory.create(new DocumentInputStream((DocumentEntry) summaryEntry));
                        try {
                            summaryInfo = new SummaryInformation(propSet);
                            if ((summaryInfo.getSecurity() & PASSWORD_PROTECTED_MASK) != 0) {
                                isNOTEncrypted = false;
                                setContentCorrectness(false);
                            } else {
                                isNOTEncrypted = true;
                                setContentCorrectness(true);
                            }
                        } catch (UnexpectedPropertySetTypeException ex) {
                            loggerCol.logException(CLASSDEBUG, this.getClass().getName(), Level.FATAL, ex);
                        }
                    } catch (RecordFormatException ex) {
                        isNOTEncrypted = false;
                        setContentCorrectness(false);
                    } catch (NoPropertySetStreamException ex) {
                        loggerCol.logException(CLASSDEBUG, this.getClass().getName(), Level.FATAL, ex);
                    } catch (MarkUnsupportedException ex) {
                        loggerCol.logException(CLASSDEBUG, this.getClass().getName(), Level.FATAL, ex);
                    }
                }
            } catch (FileNotFoundException ex) {
                fileReadError = true;
                if (READ_ERROR_DEBUG) {
                    loggerCol.logException(CLASSDEBUG, this.getClass().getName(), Level.FATAL, ex);
                }
            }
        } catch (FileNotFoundException ex) {
            if (READ_ERROR_DEBUG) {
                loggerCol.logException(CLASSDEBUG, this.getClass().getName(), Level.FATAL, ex);
            } else {
                loggerCol.logDebug(CLASSDEBUG, this.getClass().getName(), Level.DEBUG, "file not found or no access on file:" + super.canonicalPath);
            }
        } catch (NoInputStreamException ex) {
            if (READ_ERROR_DEBUG) {
                loggerCol.logException(CLASSDEBUG, this.getClass().getName(), Level.FATAL, ex);
            } else {
                loggerCol.logDebug(CLASSDEBUG, this.getClass().getName(), Level.DEBUG, "Can not read from file or no access on file:" + super.canonicalPath);
            }
        } catch (IOException ex) {
            fileReadError = true;
            if (READ_ERROR_DEBUG) {
                loggerCol.logException(CLASSDEBUG, this.getClass().getName(), Level.FATAL, ex);
            }
        }
    }

    @Override
    public Document createDocument() throws CanNotSetDefaultFieldsException {
        Document document = null;
        createTemporaryNeededObjects();
        if (!fileReadError) {
            document = new Document();
            document = setDefaultFieldsToDocument(document);
            Field authorField = getAuthorField();
            if (authorField != null) {
                document.add(authorField);
                if (crit != null) {
                    crit.addUsedCriterion(crit.getAuthor());
                }
            }
            setContentReader();
            Field contentField = getContentField();
            if (contentField != null) {
                document.add(contentField);
                if (crit != null) {
                    crit.addUsedCriterion(crit.getContent());
                }
            }
            Field summaryField = getSummaryField(contentField);
            if (summaryField != null) {
                document.add(summaryField);
                if (crit != null) {
                    crit.addUsedCriterion(crit.getSummary());
                }
            }
        }
        return document;
    }

    private void setContentReader() {
        CharArrayWriter writer = null;
        try {
            writer = new CharArrayWriter();
            if (isNOTEncrypted) {
                try {
                    HSSFWorkbook workbook = new HSSFWorkbook(poiFs, true);
                    HSSFSheet sheet = null;
                    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                        sheet = workbook.getSheetAt(i);
                        Iterator rows = sheet.rowIterator();
                        while (rows.hasNext()) {
                            HSSFRow row = (HSSFRow) rows.next();
                            Iterator cells = row.cellIterator();
                            while (cells.hasNext()) {
                                HSSFCell cell = (HSSFCell) cells.next();
                                switch(cell.getCellType()) {
                                    case HSSFCell.CELL_TYPE_NUMERIC:
                                        String num = Double.toString(cell.getNumericCellValue()).trim();
                                        if (num.length() > 0) {
                                            loggerCol.logDebug(CLASSDEBUG, this.getClass().getName(), Level.DEBUG, num + " ");
                                            writer.write(num + " ");
                                        }
                                        break;
                                    case HSSFCell.CELL_TYPE_STRING:
                                        HSSFRichTextString richTextString = cell.getRichStringCellValue();
                                        String text = richTextString.getString();
                                        if (text.length() > 0) {
                                            loggerCol.logDebug(CLASSDEBUG, this.getClass().getName(), Level.DEBUG, text + " ");
                                            writer.write(text + " ");
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                    }
                    reader = new CharArrayReader(writer.toCharArray());
                } catch (RecordFormatException ex) {
                    setContentCorrectness(false);
                } catch (IllegalArgumentException ex) {
                    setContentCorrectness(false);
                } catch (NullPointerException ex) {
                    setContentCorrectness(false);
                }
            }
        } catch (IOException ex) {
            if (reader != null) {
                IOUtils.closeQuietly(reader);
            }
            if (writer != null) {
                IOUtils.closeQuietly(writer);
            }
            loggerCol.logException(CLASSDEBUG, this.getClass().getName(), Level.FATAL, ex);
        }
    }

    private Field getSummaryField(Field contentField) {
        Field result = null;
        if (summaryInfo != null) {
            if (summaryInfo.getSubject() != null) {
                if (summaryInfo.getSubject().length() != 0) {
                    loggerCol.logDebug(CLASSDEBUG, this.getClass().getName(), Level.DEBUG, "Subject: " + summaryInfo.getSubject().toString());
                    result = new Field(crit.getSummary(), new StringReader(summaryInfo.getSubject()));
                }
            } else {
                result = super.getSummaryFieldFromContentField(contentField);
            }
        }
        return result;
    }

    private Field getContentField() {
        Field field = null;
        if (reader != null) {
            field = new Field(crit.getContent(), reader);
        }
        return field;
    }

    private Field getAuthorField() {
        Field result = null;
        if (summaryInfo != null) {
            if (summaryInfo.getAuthor() != null) {
                if (summaryInfo.getAuthor().length() != 0) {
                    loggerCol.logDebug(CLASSDEBUG, this.getClass().getName(), Level.DEBUG, "Author: " + summaryInfo.getAuthor());
                    result = new Field(crit.getAuthor(), new StringReader(summaryInfo.getAuthor()));
                }
            }
        }
        return result;
    }

    private Field getTitleField() {
        Field result = null;
        if (summaryInfo != null) {
            if (summaryInfo.getTitle() != null) {
                if (summaryInfo.getTitle().length() != 0) {
                    loggerCol.logDebug(CLASSDEBUG, this.getClass().getName(), Level.DEBUG, "Title: " + summaryInfo.getTitle());
                    result = new Field(crit.getTitle(), new StringReader(summaryInfo.getTitle()));
                }
            }
        }
        return result;
    }

    private Field getKeywordsField() {
        Field result = null;
        if (summaryInfo != null) {
            if (summaryInfo.getKeywords() != null) {
                if (summaryInfo.getKeywords().length() != 0) {
                    loggerCol.logDebug(CLASSDEBUG, this.getClass().getName(), Level.DEBUG, "Keywords: " + summaryInfo.getKeywords());
                    result = new Field(crit.getKeywords(), new StringReader(summaryInfo.getKeywords()));
                }
            }
        }
        return result;
    }
}
