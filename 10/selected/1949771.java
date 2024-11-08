package pt.gotham.gardenia.business;

import java.util.*;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import net.sf.jasperreports.engine.JasperRunManager;
import org.apache.log4j.Logger;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.File;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.PageSize;

/**
 * @author     appj
 * @created    02 July 2004
 */
public class Document {

    protected static Logger logger = Logger.getLogger(Document.class);

    /**  Constructor for the Document object  */
    public Document() {
    }

    /**
     *  Returns a hashmap containing the next serial number for the document
     *  type, or null if the documentType does not exist. HashMap contains the
     *  following keys, with respective values: SerialNumber - The next serial
     *  number SerialNumberPrefix - the prefix SerialNumberWithPrefix - the full
     *  doc. identification (serialNumberPrefix+serialNumber). No operations on 
     *  db is performed.
     *
     * @param  conn              Description of the Parameter
     * @param  documentTypeID    Description of the Parameter
     * @return                   The documentNextSerialNumber value
     * @exception  SQLException  Description of the Exception
     */
    protected static HashMap getDocumentNextSerialNumber(Connection conn, String documentTypeID) throws SQLException {
        return getDocumentNextSerialNumber(conn, documentTypeID, false);
    }

    /**
     *  Returns a hashmap containing the next serial number for the document
     *  type, or null if the documentType does not exist. HashMap contains the
     *  following keys, with respective values: SerialNumber - The next serial
     *  number SerialNumberPrefix - the prefix SerialNumberWithPrefix - the full
     *  doc. identification (serialNumberPrefix+serialNumber)
     *
     * @param  conn              Description of the Parameter
     * @param  documentTypeID    Description of the Parameter
     * @param  doIncrement       Description of the Parameter
     * @return                   The documentNextSerialNumber value
     * @exception  SQLException  Description of the Exception
     */
    protected static HashMap getDocumentNextSerialNumber(Connection conn, String documentTypeID, boolean doIncrement) throws SQLException {
        String serialNumberNext = "";
        String serialNumberPrefix = "";
        PreparedStatement pstmt = conn.prepareStatement("select SerialNumberNext, SerialNumberPrefix from tbl_DocumentType where DocumentTypeID=?");
        pstmt.setString(1, documentTypeID);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            serialNumberNext = rs.getString(1);
            serialNumberPrefix = rs.getString(2);
        } else {
            pstmt.close();
            return null;
        }
        pstmt.close();
        if (doIncrement) {
            pstmt = conn.prepareStatement("update tbl_DocumentType set SerialNumberNext = SerialNumberNext+1 where DocumentTypeID=?");
            pstmt.setString(1, documentTypeID);
            pstmt.executeUpdate();
        }
        pstmt.close();
        HashMap retVal = new HashMap();
        retVal.put("SerialNumber", serialNumberNext);
        retVal.put("SerialNumberPrefix", serialNumberPrefix);
        retVal.put("SerialNumberWithPrefix", serialNumberPrefix + serialNumberNext);
        return retVal;
    }

    /**
     *  Returns a hashmap containing a temporary serial number for the document
     *  type, or null if the documentType does not exist. HashMap contains the
     *  following keys, with respective values: SerialNumber - The next serial
     *  number SerialNumberPrefix - the prefix SerialNumberWithPrefix - the full
     *  doc. identification (serialNumberPrefix+serialNumber)
     *
     * @return                   The documentNextSerialNumber value
     */
    public static HashMap getDocumentTempSerialNumber() {
        String serialNumberNext = "";
        String serialNumberPrefix = "TEMP";
        serialNumberNext = "" + Math.round(Math.random() * 1000000);
        HashMap retVal = new HashMap();
        retVal.put("SerialNumber", serialNumberNext);
        retVal.put("SerialNumberPrefix", serialNumberPrefix);
        retVal.put("SerialNumberWithPrefix", serialNumberPrefix + serialNumberNext);
        return retVal;
    }

    /**
     *  Returns a hashmap containing the next serial number for the document
     *  type, or null if the documentType does not exist. HashMap contains the
     *  following keys, with respective values: SerialNumber - The next serial
     *  number SerialNumberPrefix - the prefix SerialNumberWithPrefix - the full
     *  doc. identification (serialNumberPrefix+serialNumber)
     *
     * @param  conn              Description of the Parameter
     * @param  documentTypeID    Description of the Parameter
     * @param  doIncrement       Description of the Parameter
     * @return                   The documentNextSerialNumber value
     * @exception  SQLException  Description of the Exception
     */
    protected static HashMap setDocumentNextSerialNumber(Connection conn, String documentID) throws SQLException {
        String serialNumberNext = "";
        String serialNumberPrefix = "";
        String documentTypeID = "";
        PreparedStatement pstmt = conn.prepareStatement("select dt.SerialNumberNext, dt.SerialNumberPrefix, dt.DocumentTypeID from tbl_DocumentType dt, tbl_Document d where d.DocumentID=? and d.DocumentTypeID=dt.DocumentTypeID");
        pstmt.setString(1, documentID);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            serialNumberNext = rs.getString(1);
            serialNumberPrefix = rs.getString(2);
            documentTypeID = rs.getString(3);
        } else {
            pstmt.close();
            return null;
        }
        pstmt.close();
        pstmt = conn.prepareStatement("update tbl_DocumentType set SerialNumberNext = SerialNumberNext+1 where DocumentTypeID=?");
        pstmt.setString(1, documentTypeID);
        pstmt.executeUpdate();
        pstmt.close();
        pstmt = conn.prepareStatement("update tbl_Document set SerialNumber=?, SerialNumberPrefix=? where DocumentID=?");
        pstmt.setString(1, serialNumberNext);
        pstmt.setString(2, serialNumberPrefix);
        pstmt.setString(3, documentID);
        pstmt.executeUpdate();
        pstmt.close();
        HashMap retVal = new HashMap();
        retVal.put("SerialNumber", serialNumberNext);
        retVal.put("SerialNumberPrefix", serialNumberPrefix);
        retVal.put("SerialNumberWithPrefix", serialNumberPrefix + serialNumberNext);
        return retVal;
    }

    /**
     *  This method checks if there are any needed changes on db, due to a
     *  document status change If a document changes its status from EDT to OPEN
     *  and from OPEN to CNL, and from OPEN to EDT again, it will perform any
     *  needed Document Stock changes, and create the PDF with the Document, if
     *  this document is of type Sells.
     *
     * @param  conn                Description of the Parameter
     * @param  documentID          Description of the Parameter
     * @param  lastStatus          Description of the Parameter
     * @param  newStatus           Description of the Parameter
     * @param  doStatusUpdateOnDB  Description of the Parameter
     * @exception  SQLException    Description of the Exception
     */
    public static HashMap processDocumentStatusChange(Connection conn, String documentID, String lastStatus, String newStatus, boolean doStatusUpdateOnDB) throws SQLException {
        logger.debug("Inside Document.changeDocumentStatus() method !!!");
        String documentTypeID = null;
        String stockMovementType = "NON";
        String documentType = "NON";
        String createPrintableDocument = "NO";
        HashMap newValues = null;
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select DocumentTypeID, Status from tbl_Document where DocumentID=" + documentID);
            if (rs.next()) {
                documentTypeID = rs.getString(1);
                if (lastStatus == null) lastStatus = rs.getString(2);
            } else {
                logger.error("Document.changeDocumentStatus() - DocumentTypeID does not exist!!!");
                return newValues;
            }
            if ("CNL".equals(lastStatus)) {
                logger.info("Last status is CNL-Canceled. Exiting method and  ignoring operation...");
                return newValues;
            }
            rs = stmt.executeQuery("select StockMovementType, DocumentType, CreatePrintableDocument from tbl_DocumentType where DocumentTypeID='" + documentTypeID + "'");
            if (rs.next()) {
                stockMovementType = rs.getString(1);
                documentType = rs.getString(2);
                createPrintableDocument = rs.getString(3);
            } else {
                logger.error("Error getting info about DocumentType for documentTypeID=" + documentTypeID);
            }
            stmt.close();
            int operation = 0;
            if (!newStatus.equals(lastStatus)) {
                if (!lastStatus.equals("CLD") && newStatus.equals("OPN")) {
                    operation = 1;
                } else if (!lastStatus.equals("OPN") && newStatus.equals("CLD")) {
                    operation = 1;
                } else if (lastStatus.equals("OPN") && !newStatus.equals("CLD")) {
                    operation = 2;
                } else if (lastStatus.equals("CLD") && !newStatus.equals("OPN")) {
                    operation = 2;
                } else {
                    operation = 0;
                }
            }
            logger.debug("BEFORE UPDATE: DocumentID=" + documentID + " DocumentTypeID=" + documentTypeID + " lastStatus=" + lastStatus + " newStatus=" + newStatus + " operation=" + operation + " stockMovementType=" + stockMovementType + " createPrintableDocument=" + createPrintableDocument);
            if (!newStatus.equals(lastStatus) && "EDT".equals(lastStatus)) {
                try {
                    newValues = setDocumentNextSerialNumber(conn, documentID);
                } catch (Exception ex) {
                    logger.error("ERROR GETTING/SETTING THE DOCUMENT SERIAL NUMBER: " + ex, ex);
                    throw new SQLException("" + ex);
                }
            }
            if ("YES".equals(createPrintableDocument) && (!newStatus.equals(lastStatus)) && "EDT".equals(lastStatus)) {
                try {
                    createDocumentDataFiles(conn, documentID);
                } catch (Exception ex) {
                    logger.error("ERROR creating the REPORT!!!: " + ex, ex);
                    throw new SQLException("" + ex);
                }
            }
            if ("YES".equals(createPrintableDocument) && (!newStatus.equals(lastStatus)) && "CNL".equals(newStatus)) {
                try {
                    cancelDocumentDataFiles(conn, documentID);
                } catch (Exception ex) {
                    logger.error("ERROR canceling the document: " + ex, ex);
                    throw new SQLException("" + ex);
                }
            }
            if (doStatusUpdateOnDB) {
                PreparedStatement pstmt = conn.prepareStatement("update tbl_Document set Status=?, LastStatusDateTime=? where DocumentID=?");
                pstmt.setString(1, newStatus);
                pstmt.setTimestamp(2, new Timestamp(Calendar.getInstance().getTimeInMillis()));
                pstmt.setString(3, documentID);
                pstmt.executeUpdate();
                pstmt.close();
            }
            if (operation == 1) {
                if ("ADD".equals(stockMovementType)) {
                    addDocumentItemsToStock(conn, documentID);
                } else if ("SUB".equals(stockMovementType)) {
                    removeDocumentItemsFromStock(conn, documentID);
                }
            } else if (operation == 2) {
                if ("ADD".equals(stockMovementType)) {
                    removeDocumentItemsFromStock(conn, documentID);
                } else if ("SUB".equals(stockMovementType)) {
                    addDocumentItemsToStock(conn, documentID);
                }
            } else {
            }
        } catch (Exception e) {
            logger.error("Error: " + e, e);
            throw new SQLException("" + e);
        }
        return newValues;
    }

    /**
     *  This method adds or removes (according to param operationIsAdd) the items in document to the product stock
     *
     * @param  conn              Description of the Parameter
     * @param  documentID        Description of the Parameter
     * @param  operationIsAdd    Description of the Parameter
     * @exception  SQLException  Description of the Exception
     */
    protected static void updateDocumentItemsOnStock(Connection conn, String documentID, boolean operationIsAdd) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        ArrayList updtQueries = new ArrayList();
        boolean lastAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            rs = stmt.executeQuery("select di.ProductID, di.Quantity from tbl_DocumentItem di,tbl_Product p" + " where p.ProductID = di.ProductID and p.ProductType in ('BOS','SOS','BSS') and di.Quantity is not null and di.DocumentID = " + documentID);
            while (rs.next()) {
                String productID = rs.getString(1);
                double qty = rs.getDouble(2);
                logger.debug("OK. Result = productID=" + productID + " and qty=" + qty + " and operationIsAdd=" + operationIsAdd);
                if (operationIsAdd) {
                    updtQueries.add(" update tbl_Product set StockCurrent = StockCurrent + " + qty + " where ProductID = " + productID);
                } else {
                    updtQueries.add(" update tbl_Product set StockCurrent = StockCurrent - " + qty + " where ProductID = " + productID);
                }
            }
            rs.close();
            for (Iterator it = updtQueries.iterator(); it.hasNext(); ) {
                String sql = (String) it.next();
                logger.debug("Executing: " + sql);
                stmt.execute(sql);
            }
            conn.commit();
            stmt.close();
            stmt = null;
            conn.setAutoCommit(lastAutoCommit);
        } catch (SQLException ex) {
            try {
                conn.rollback();
                conn.setAutoCommit(lastAutoCommit);
            } catch (SQLException ex1) {
                ex1.printStackTrace();
            }
            if (stmt != null) {
                stmt.close();
            }
            throw ex;
        }
    }

    /**
     *  Adds a feature to the DocumentItemsToStock attribute of the
     *  DocumentUpdateEvent object
     *
     * @param  conn              The feature to be added to the
     *      DocumentItemsToStock attribute
     * @param  documentID        The feature to be added to the
     *      DocumentItemsToStock attribute
     * @exception  SQLException  Description of the Exception
     */
    public static void addDocumentItemsToStock(Connection conn, String documentID) throws SQLException {
        updateDocumentItemsOnStock(conn, documentID, true);
    }

    /**
     *  This method remove the items in document from the product stock
     *
     * @param  conn              Description of the Parameter
     * @param  documentID        Description of the Parameter
     * @exception  SQLException  Description of the Exception
     */
    public static void removeDocumentItemsFromStock(Connection conn, String documentID) throws SQLException {
        updateDocumentItemsOnStock(conn, documentID, false);
    }

    /**
     *  This method allows to import the lines from a document to another.
     *
     * @param  conn              Description of the Parameter
     * @param  originDocumentID        Description of the Parameter
     * @param  destinationDocumentID        Description of the Parameter
     * @exception  SQLException  Description of the Exception
     */
    public static void importDocumentLines(Connection conn, String originDocumentID, String destinationDocumentID) throws SQLException {
        boolean defaultAutoCommit = conn.getAutoCommit();
        String sqlQuery = "select ProductID,Description,PricePerUnit,Quantity,DiscountPCT,VATPCT,TotalNoVATPrice,TotalPrice from tbl_DocumentItem where DocumentID=?";
        String sqlInsert = "insert into tbl_DocumentItem (ProductID,Description,PricePerUnit,Quantity,DiscountPCT,VATPCT,TotalNoVATPrice,TotalPrice,DocumentID) values (?,?,?,?,?,?,?,?,?)";
        PreparedStatement pstmt1 = null;
        PreparedStatement pstmt2 = null;
        try {
            pstmt1 = conn.prepareStatement(sqlQuery);
            pstmt2 = conn.prepareStatement(sqlInsert);
            conn.setAutoCommit(false);
            pstmt1.setString(1, originDocumentID);
            ResultSet rs = pstmt1.executeQuery();
            while (rs.next()) {
                pstmt2.setInt(1, rs.getInt(1));
                pstmt2.setString(2, rs.getString(2));
                pstmt2.setDouble(3, rs.getDouble(3));
                pstmt2.setDouble(4, rs.getDouble(4));
                pstmt2.setDouble(5, rs.getDouble(5));
                pstmt2.setDouble(6, rs.getDouble(6));
                pstmt2.setDouble(7, rs.getDouble(7));
                pstmt2.setDouble(8, rs.getDouble(8));
                pstmt2.setString(9, destinationDocumentID);
                pstmt2.executeUpdate();
            }
            rs.close();
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
        } finally {
            conn.setAutoCommit(defaultAutoCommit);
            if (pstmt1 != null) pstmt1.close();
            if (pstmt2 != null) pstmt2.close();
        }
    }

    public static Map getDocumentDataFilesParameters(Connection conn, String documentID) throws Exception {
        String documentReportDefDir = "";
        String documentReportDefFileName = "";
        String companyLogoImage = "";
        String documentReportDataDir = "";
        String defaultLocale = "pt_PT";
        int numberOfCopies = 1;
        String documentReportPropFileName = "";
        ResourceBundle resourceBundle = null;
        Properties documentReportProps = new Properties();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select distinct a.ParameterValue, b.ParameterValue, c.ParameterValue, d.ParameterValue, e.ParameterValue, f.ParameterValue " + " from tbl_Parameter a, tbl_Parameter b, tbl_Parameter c, tbl_Parameter d, tbl_Parameter e, tbl_Parameter f  where " + " a.ParameterID='DocumentReportDefDir' and b.ParameterID='DocumentReportDefFileName' and c.ParameterID='CompanyLogoImage' and " + " d.ParameterID='DocumentReportDataDir' and e.ParameterID='DefaultLocale' and f.ParameterID='DocumentReportPropFileName'");
            if (rs.next()) {
                documentReportDefDir = rs.getString(1);
                documentReportDefFileName = rs.getString(2);
                companyLogoImage = rs.getString(3);
                documentReportDataDir = rs.getString(4);
                defaultLocale = rs.getString(5);
                documentReportPropFileName = rs.getString(6);
            }
            rs.close();
            rs = stmt.executeQuery("select dt.NumberOfCopies from tbl_DocumentType dt, tbl_Document d where dt.DocumentTypeID=d.DocumentTypeID and d.DocumentID=" + documentID);
            if (rs.next()) {
                numberOfCopies = rs.getInt(1);
            }
            rs.close();
            stmt.close();
            resourceBundle = I18NMessages.getResourceBundle(defaultLocale);
            documentReportProps.load(new FileInputStream(documentReportDefDir + "/" + documentReportPropFileName));
        } catch (Exception ex) {
            logger.error("ViewDocumentReport.java: Error getting parameters from DB: " + ex);
            ex.printStackTrace();
            throw ex;
        }
        HashMap result = new HashMap();
        result.putAll(getDocumentDataFilesPath(documentReportDataDir, documentID));
        result.put("documentReportDefDir", documentReportDefDir);
        result.put("documentReportDefFileName", documentReportDefFileName);
        result.put("companyLogoImage", companyLogoImage);
        result.put("documentReportDataDir", documentReportDataDir);
        result.put("defaultLocale", defaultLocale);
        result.put("documentReportPropFileName", documentReportPropFileName);
        result.put("resourceBundle", resourceBundle);
        result.put("documentReportProps", documentReportProps);
        result.put("numberOfCopies", new Integer(numberOfCopies));
        return result;
    }

    public static Map getDocumentDataFilesPath(String documentReportDataDir, String documentID) {
        HashMap result = new HashMap();
        result.put("basePDFFile", documentReportDataDir + "/DocumentBase_" + documentID + ".pdf");
        result.put("originalPDFFile", documentReportDataDir + "/DocumentOriginal_" + documentID + ".pdf");
        result.put("duplicatePDFFile", documentReportDataDir + "/DocumentDuplicate_" + documentID + ".pdf");
        result.put("xmlFile", documentReportDataDir + "/Document_" + documentID + ".xml");
        return result;
    }

    /**
     *  This method creates all needed files for a document (XML files with document, 
     *  all PDF files holding the base, the original and the duplicate of documents, 
     *  keeping them at the directory configured in the tbl_Parameter
     *
     * @param  conn           Description of the Parameter
     * @param  documentID     Description of the Parameter
     * @exception  Exception  Description of the Exception
     */
    public static void createDocumentDataFiles(Connection conn, String documentID) throws Exception {
        createDocumentDataFiles(conn, documentID, null);
    }

    public static void createDocumentDataFiles(Connection conn, String documentID, Map parms) throws Exception {
        if (parms == null) parms = getDocumentDataFilesParameters(conn, documentID);
        createDocumentDataBasePDF(conn, documentID, (String) parms.get("documentReportDefDir"), (String) parms.get("documentReportDefFileName"), (String) parms.get("companyLogoImage"), (ResourceBundle) parms.get("resourceBundle"), (Properties) parms.get("documentReportProps"), (String) parms.get("basePDFFile"));
        createDocumentDataOriginalPDF((String) parms.get("basePDFFile"), ((Integer) parms.get("numberOfCopies")).intValue(), (ResourceBundle) parms.get("resourceBundle"), (Properties) parms.get("documentReportProps"), (String) parms.get("originalPDFFile"));
        createDocumentDataDuplicatePDF((String) parms.get("basePDFFile"), ((Integer) parms.get("numberOfCopies")).intValue(), (ResourceBundle) parms.get("resourceBundle"), (Properties) parms.get("documentReportProps"), (String) parms.get("duplicatePDFFile"));
    }

    /**
     *  This method cancel all the document files (base, originalPDF, duplicatePDF, etc),  
     *  keeping them at the directory configured in the tbl_Parameter
     *
     * @param  conn           Description of the Parameter
     * @param  documentID     Description of the Parameter
     * @exception  Exception  Description of the Exception
     */
    public static void cancelDocumentDataFiles(Connection conn, String documentID) throws Exception {
        cancelDocumentDataFiles(conn, documentID, null);
    }

    public static void cancelDocumentDataFiles(Connection conn, String documentID, Map parms) throws Exception {
        logger.debug("cancelDocumentDataFiles() - Entering method with documentID=" + documentID);
        if (parms == null) parms = getDocumentDataFilesParameters(conn, documentID);
        String baseFileStr = (String) parms.get("basePDFFile");
        String origFileStr = (String) parms.get("originalPDFFile");
        String dupFileStr = (String) parms.get("duplicatePDFFile");
        File baseFile = new File(baseFileStr);
        File origFile = new File(origFileStr);
        File dupFile = new File(dupFileStr);
        if (!(baseFile.isFile() && origFile.isFile() && dupFile.isFile())) {
            logger.debug("cancelDocumentDataFiles() - no file exists... creating files first...");
            createDocumentDataFiles(conn, documentID, parms);
        }
        ResourceBundle rb = (ResourceBundle) parms.get("resourceBundle");
        String textCancel = "ANULADO";
        if (rb != null) {
            textCancel = rb.getString("doc.doc01.print.Cancel");
        }
        logger.debug("cancelDocumentDataFiles() - canceling original file " + origFileStr);
        processCancelDocumentDataPDF(origFileStr, new FileOutputStream(origFileStr + ".tmp.pdf"), textCancel, (Properties) parms.get("documentReportProps"));
        File tmpFile = new File(origFileStr + ".tmp.pdf");
        if (!tmpFile.renameTo(origFile)) {
            throw new Exception("Cannot rename and cancel original document on file " + origFileStr);
        }
        logger.debug("cancelDocumentDataFiles() - canceling duplicate file " + dupFileStr);
        processCancelDocumentDataPDF(dupFileStr, new FileOutputStream(dupFileStr + ".tmp.pdf"), textCancel, (Properties) parms.get("documentReportProps"));
        tmpFile = new File(dupFileStr + ".tmp.pdf");
        if (!tmpFile.renameTo(dupFile)) {
            throw new Exception("Cannot rename and cancel duplicate document on file " + dupFileStr);
        }
    }

    /**
     *  This method creates a PDF File with the document print view, keeping it
     *  in the directory configured in the tbl_Parameter
     *
     * @param  conn           Description of the Parameter
     * @param  documentID     Description of the Parameter
     * @exception  Exception  Description of the Exception
     */
    protected static void createDocumentDataBasePDF(Connection conn, String documentID, String documentReportDefDir, String documentReportDefFileName, String companyLogoImage, ResourceBundle resourceBundle, Properties prop, String pdfFile) throws Exception {
        HashMap params = new HashMap();
        params.put("DocumentID", new Integer(documentID));
        params.put("ReportsBaseDir", documentReportDefDir);
        params.put("CompanyLogoImage", companyLogoImage);
        params.put("REPORT_RESOURCE_BUNDLE", resourceBundle);
        String reportFile = documentReportDefDir + "/" + documentReportDefFileName;
        JasperRunManager.runReportToPdfFile(reportFile, pdfFile, params, conn);
    }

    public static void createDocumentDataOriginalPDF(String mainPDFDocument, int nCopies, ResourceBundle rb, Properties prop, String pdfFile) throws Exception {
        String textOriginal = "ORIGINAL";
        String textCopy = "CÓPIA %CN";
        if (rb != null) {
            textOriginal = rb.getString("doc.doc01.print.Original");
            textCopy = rb.getString("doc.doc01.print.Copy");
        }
        processDocumentDataPDF(mainPDFDocument, new FileOutputStream(pdfFile), nCopies, textOriginal, textCopy, prop);
    }

    public static void createDocumentDataDuplicatePDF(String mainPDFDocument, int nCopies, ResourceBundle rb, Properties prop, String pdfFile) throws Exception {
        String textDuplicate = "DUPLICADO";
        String textCopy = "CÓPIA %CN";
        if (rb != null) {
            textDuplicate = rb.getString("doc.doc01.print.DuplicateOriginal");
            textCopy = rb.getString("doc.doc01.print.DuplicateCopy");
        }
        processDocumentDataPDF(mainPDFDocument, new FileOutputStream(pdfFile), nCopies, textDuplicate, textCopy, prop);
    }

    protected static void processCancelDocumentDataPDF(String pdfDocumentPath, OutputStream outputStream, String text, Properties prop) throws Exception {
        logger.debug("processCancelDocumentDataPDF() - Canceling PDF document " + pdfDocumentPath);
        int tpx = Integer.parseInt(prop.getProperty("cancelTextPositionX", "190"));
        int tpy = Integer.parseInt(prop.getProperty("cancelTextPositionY", "620"));
        int trd = Integer.parseInt(prop.getProperty("cancelTextRotateDegrees", "45"));
        int tfs = Integer.parseInt(prop.getProperty("cancelTextFontSize", "24"));
        int tcr = Integer.parseInt(prop.getProperty("cancelTextColorR", "0"));
        int tcg = Integer.parseInt(prop.getProperty("cancelTextColorG", "0"));
        int tcb = Integer.parseInt(prop.getProperty("cancelTextColorB", "0"));
        PdfReader originalDoc = new PdfReader(pdfDocumentPath);
        int nPages = originalDoc.getNumberOfPages();
        PdfStamper resultDoc = new PdfStamper(originalDoc, outputStream);
        for (int currentPage = 1; currentPage <= nPages; currentPage++) {
            logger.debug("processCancelDocumentDataPDF() - Adding canceled label " + text + " to page " + currentPage + " of " + nPages + " on document " + pdfDocumentPath);
            addPDFLabelText(text, resultDoc.getOverContent(currentPage), tpx, tpy, trd, tfs, tcr, tcg, tcb);
        }
        resultDoc.close();
        logger.debug("processCancelDocumentDataPDF() - Finished doc.");
    }

    protected static void processDocumentDataPDF(String pdfDocumentPath, OutputStream outputStream, int nCopies, String originalText, String copiesText, Properties prop) throws Exception {
        int tpx = Integer.parseInt(prop.getProperty("copyTextPositionX", "270"));
        int tpy = Integer.parseInt(prop.getProperty("copyTextPositionY", "680"));
        int trd = Integer.parseInt(prop.getProperty("copyTextRotateDegrees", "90"));
        int tfs = Integer.parseInt(prop.getProperty("copyTextFontSize", "14"));
        int tcr = Integer.parseInt(prop.getProperty("copyTextColorR", "211"));
        int tcg = Integer.parseInt(prop.getProperty("copyTextColorG", "211"));
        int tcb = Integer.parseInt(prop.getProperty("copyTextColorB", "211"));
        PdfReader originalDoc = new PdfReader(pdfDocumentPath);
        int nPages = originalDoc.getNumberOfPages();
        int currentPage = 1;
        PdfStamper resultDoc = new PdfStamper(originalDoc, outputStream);
        addPDFLabelText(originalText, resultDoc.getOverContent(currentPage), tpx, tpy, trd, tfs, tcr, tcg, tcb);
        currentPage = nPages;
        for (int nCopy = 0; nCopy < nCopies; nCopy++) {
            for (int i = 1; i <= nPages; i++) {
                currentPage++;
                resultDoc.insertPage(currentPage, PageSize.A4);
                PdfContentByte under = resultDoc.getUnderContent(currentPage);
                under.addTemplate(resultDoc.getImportedPage(originalDoc, i), 1, 0, 0, 1, 0, 0);
                String copyTextWithPageNumber = copiesText.replaceAll("%CN", "" + (nCopy + 1));
                if (i == 1) addPDFLabelText(copyTextWithPageNumber, resultDoc.getOverContent(currentPage), tpx, tpy, trd, tfs, tcr, tcg, tcb);
            }
        }
        resultDoc.close();
    }

    protected static void addPDFLabelText(String textToAdd, PdfContentByte cb, int textPositionX, int textPositionY, int textRotateDegrees, int textFontSize, int textColorR, int textColorG, int textColorB) throws Exception {
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.EMBEDDED);
        cb.beginText();
        cb.setFontAndSize(bf, textFontSize);
        cb.setRGBColorFill(textColorR, textColorG, textColorB);
        cb.showTextAligned(Element.ALIGN_CENTER, textToAdd, textPositionX, textPositionY, textRotateDegrees);
        cb.endText();
    }
}
