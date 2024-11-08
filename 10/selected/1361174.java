package com.dcivision.dms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.dcivision.dms.bean.DmsDocument;
import com.dcivision.dms.core.DmsContentStoreHandler;
import com.dcivision.dms.core.DmsIndexConverter;
import com.dcivision.framework.ApplicationException;
import com.dcivision.framework.GlobalConstant;
import com.dcivision.framework.IndexManager;
import com.dcivision.framework.SessionContainer;

public class LoadFullTextDocumentData {

    public static final String REVISION = "$Revision: 1.9.2.1 $";

    static String author = null;

    static String srcPath = null;

    static String driverStr = null;

    static String dbConnStr = null;

    static String dbUsername = null;

    static String dbPassword = null;

    static String packageName = "com.dcivision.";

    static final String JAVA_INTEGER = "Integer";

    static final String JAVA_DECIMAL = "Float";

    static final String JAVA_STRING = "String";

    static final String JAVA_DATE = "Timestamp";

    static Connection conn = null;

    static String sqlStat = null;

    static Statement stat = null;

    static ResultSet rs = null;

    static ResultSetMetaData rsmd = null;

    static Timestamp currTime = null;

    static DmsDocument dmsDocument = new DmsDocument();

    static String sUdfID = null;

    static String sUdfDetailID = null;

    static String indexPath = "";

    static DmsContentStoreHandler contentStoreHandler = null;

    private static final Log log = LogFactory.getLog(LoadFullTextDocumentData.class);

    public static void main(String args[]) throws Exception {
        currTime = getCurrentTimestamp();
        String sqlDoc = "";
        String sqlVersion = "";
        String sqlDocVersion = "";
        String sqlContent = "";
        String sqlDocDetail = "";
        String sqlRoot = "";
        java.util.Properties props = new java.util.Properties();
        String path = new LoadDocumentData().getClass().getProtectionDomain().getCodeSource().getLocation().toString().substring(6);
        if (!path.endsWith("/")) {
            path += "/";
        }
        path += "generate.properties";
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        java.io.FileInputStream fis = new java.io.FileInputStream(new java.io.File(path));
        props.load(fis);
        author = props.getProperty("author");
        srcPath = props.getProperty("srcPath");
        driverStr = props.getProperty("driverStr");
        dbConnStr = props.getProperty("dbConnStr");
        dbUsername = props.getProperty("dbUsername");
        dbPassword = props.getProperty("dbPassword");
        openDBConn();
        stat = conn.createStatement();
        Hashtable htTableKey = new Hashtable();
        htTableKey = getTableKey(conn);
        genUserDefinedIndex(conn, htTableKey);
        Integer ownerID = new Integer(0);
        Integer nDocID = new Integer(1);
        Integer nDocDetailID = new Integer(1);
        Integer nVersionID = new Integer(1);
        Integer nContentID = new Integer(1);
        Integer nDmsRootID = new Integer(1);
        Integer nMtmDocVersionID = new Integer(1);
        Integer nParentID = null;
        Integer nRootID = null;
        String sName = "1000000001.txt";
        String sConvertedName = "1000000001";
        if (htTableKey.containsKey("DMS_DOCUMENT")) {
            nDocID = new Integer(((Integer) htTableKey.get("DMS_DOCUMENT")).intValue() + 1);
        }
        if (htTableKey.containsKey("DMS_DOCUMENT_DETAIL")) {
            nDocDetailID = new Integer(((Integer) htTableKey.get("DMS_DOCUMENT_DETAIL")).intValue() + 1);
        }
        if (htTableKey.containsKey("DMS_VERSION")) {
            nVersionID = new Integer(((Integer) htTableKey.get("DMS_VERSION")).intValue() + 1);
        }
        if (htTableKey.containsKey("DMS_CONTENT")) {
            nContentID = new Integer(((Integer) htTableKey.get("DMS_CONTENT")).intValue() + 1);
        }
        if (htTableKey.containsKey("DMS_ROOT")) {
            nDmsRootID = new Integer(((Integer) htTableKey.get("DMS_ROOT")).intValue() + 1);
        }
        if (htTableKey.containsKey("MTM_DOCUMENT_VERSION")) {
            nMtmDocVersionID = new Integer(((Integer) htTableKey.get("MTM_DOCUMENT_VERSION")).intValue() + 1);
        }
        int nStart = (new Integer(args[0])).intValue();
        int nEnd = (new Integer(args[1])).intValue();
        nParentID = new Integer(args[2]);
        Integer nRootParentID = new Integer(args[2]);
        nRootID = new Integer(args[3]);
        String sPhysicalLoc = new String(args[4]);
        indexPath = new String(args[5]);
        System.out.println("rootID : " + nRootID + "  ParentID " + nParentID + " physical Loc = " + sPhysicalLoc);
        String sFieldValue = "";
        PreparedStatement preStat = null;
        String sDocName = "";
        int count = 0;
        int total = 0;
        FileInputStream infile = new FileInputStream(new File(sPhysicalLoc + sName));
        byte[] buffer = new byte[infile.available()];
        infile.read(buffer);
        String inFileData = new String(buffer);
        for (int i = nStart; i <= nEnd; i++) {
            try {
                sFieldValue = "REF" + i;
                sDocName = Calendar.getInstance().getTimeInMillis() + ".tif";
                dmsDocument.setID(nDocID);
                dmsDocument.setDocumentType("D");
                dmsDocument.setParentID(nParentID);
                dmsDocument.setRootID(nRootID);
                dmsDocument.setCreateType("S");
                dmsDocument.setReferenceNo("Ref Num");
                dmsDocument.setDescription("desc");
                dmsDocument.setUdfDetailList(new ArrayList());
                dmsDocument.setEffectiveStartDate(currTime);
                dmsDocument.setItemSize(new Integer(20480));
                dmsDocument.setItemStatus("A");
                dmsDocument.setOwnerID(new Integer(0));
                dmsDocument.setUpdateCount(new Integer(0));
                dmsDocument.setCreatorID(new Integer(0));
                dmsDocument.setCreateDate(currTime);
                dmsDocument.setUpdaterID(new Integer(0));
                dmsDocument.setUpdateDate(currTime);
                dmsDocument.setRecordStatus("A");
                if (count % 500 == 0) {
                    sDocName = "TestDocument" + i;
                    dmsDocument.setDocumentName(sDocName);
                    dmsDocument.setDocumentType("F");
                    sqlDoc = "INSERT INTO DMS_DOCUMENT VALUES(" + nDocID.toString() + ",'" + sDocName + "','F'," + nRootParentID + "," + nRootID.toString() + ", 'S', '" + dmsDocument.getCreateDate().toString() + "', NULL, '" + ownerID + "','Ref Num', 'desc', 0, 'A', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,'A',0,0,'" + dmsDocument.getCreateDate().toString() + "',0,'" + dmsDocument.getCreateDate().toString() + "')";
                    preStat = conn.prepareStatement(sqlDoc);
                    preStat.executeUpdate();
                    nParentID = nDocID;
                    nDocID = new Integer(nDocID.intValue() + 1);
                }
                total = count + nStart;
                System.out.println("xxx Count: " + total + " docID = " + nDocID);
                sDocName = "TestFullText" + i + ".txt";
                dmsDocument.setDocumentName(sDocName);
                sqlDoc = "INSERT INTO DMS_DOCUMENT VALUES(" + nDocID.toString() + ",'" + sDocName + "','D'," + nParentID.toString() + "," + nRootID.toString() + ", 'S','" + dmsDocument.getCreateDate().toString() + "', NULL, '" + ownerID + "','Ref Num', 'desc', 20480, 'A', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'A',0,0,'" + dmsDocument.getCreateDate().toString() + "',0,'" + dmsDocument.getCreateDate().toString() + "')";
                preStat = conn.prepareStatement(sqlDoc);
                preStat.executeUpdate();
                sqlDocDetail = "INSERT INTO DMS_DOCUMENT_DETAIL VALUES(" + nDocDetailID.toString() + "," + nDocID.toString() + "," + sUdfID + "," + sUdfDetailID + ",'" + sFieldValue + "', null, null, 'A',0,0,'" + dmsDocument.getCreateDate().toString() + "',0,'" + dmsDocument.getCreateDate().toString() + "')";
                preStat = conn.prepareStatement(sqlDocDetail);
                preStat.executeUpdate();
                dmsDocument.setUserDefinedFieldID(new Integer(sUdfID));
                sqlContent = " INSERT INTO DMS_CONTENT VALUES (" + nContentID.toString() + "," + sConvertedName + ", 'IMAGE', null, 'TIF', 'A', 0,0,'" + dmsDocument.getCreateDate().toString() + "',0,'" + dmsDocument.getCreateDate().toString() + "')";
                preStat = conn.prepareStatement(sqlContent);
                preStat.executeUpdate();
                dmsDocument.setContentID(nContentID);
                sqlVersion = "INSERT INTO DMS_VERSION VALUES(" + nVersionID.toString() + ", " + nDocID.toString() + ", 1,'ROOT',0," + nContentID.toString() + ",0, 'Ref Num', 'desc', 20480, 'A', null, 'A',0,0,'" + dmsDocument.getCreateDate().toString() + "',0,'" + dmsDocument.getCreateDate().toString() + "')";
                preStat = conn.prepareStatement(sqlVersion);
                preStat.executeUpdate();
                dmsDocument.setVersionID(nVersionID);
                dmsDocument.setVersionID(new Integer(1));
                dmsDocument.setVersionLabel("ROOT");
                sqlDocVersion = "INSERT INTO MTM_DOCUMENT_VERSION VALUES(" + nMtmDocVersionID.toString() + "," + nDocID.toString() + "," + nVersionID.toString() + ",'A', 0, 0,'" + dmsDocument.getCreateDate().toString() + "',0,'" + dmsDocument.getCreateDate().toString() + "')";
                preStat = conn.prepareStatement(sqlDocVersion);
                preStat.executeUpdate();
                nDocID = new Integer(nDocID.intValue() + 1);
                nDocDetailID = new Integer(nDocDetailID.intValue() + 1);
                nVersionID = new Integer(nVersionID.intValue() + 1);
                nContentID = new Integer(nContentID.intValue() + 1);
                nDmsRootID = new Integer(nDmsRootID.intValue() + 1);
                nMtmDocVersionID = new Integer(nMtmDocVersionID.intValue() + 1);
                SessionContainer sessionContainer = new SessionContainer();
                if ("D".equals(dmsDocument.getDocumentType())) {
                    File outFile = new File(sPhysicalLoc + "temp.txt");
                    PrintStream out = new PrintStream(new FileOutputStream(outFile, false), true);
                    out.println(formatNumber(i));
                    out.print(inFileData);
                    try {
                        out.close();
                    } catch (Exception ignore) {
                        out = null;
                    }
                    FileInputStream data = new FileInputStream(outFile);
                    indexDocument(dmsDocument, data, GlobalConstant.OP_MODE_INSERT);
                    try {
                        data.close();
                    } catch (Exception ignore) {
                        data = null;
                    }
                }
                count++;
            } catch (Exception ee) {
                log.error(ee, ee);
                conn.rollback();
            } finally {
                try {
                    preStat.close();
                    conn.rollback();
                } catch (Exception ep) {
                }
            }
        }
        try {
            infile.close();
        } catch (Exception ignore) {
            infile = null;
        }
        PreparedStatement statment = null;
        if (htTableKey.containsKey("DMS_DOCUMENT")) {
            statment = conn.prepareStatement("UPDATE SYS_TABLE_KEY SET TABLE_KEY_MAX=" + nDocID.toString() + " WHERE TABLE_NAME='DMS_DOCUMENT'");
            statment.executeUpdate();
        } else {
            statment = conn.prepareStatement("INSERT INTO SYS_TABLE_KEY VALUES('DMS_DOCUMENT', " + nDocID.toString() + ")");
            statment.executeUpdate();
        }
        if (htTableKey.containsKey("DMS_DOCUMENT_DETAIL")) {
            statment = conn.prepareStatement("UPDATE SYS_TABLE_KEY SET TABLE_KEY_MAX=" + nDocDetailID.toString() + " WHERE TABLE_NAME='DMS_DOCUMENT_DETAIL'");
            statment.executeUpdate();
        } else {
            statment = conn.prepareStatement("INSERT INTO SYS_TABLE_KEY VALUES('DMS_DOCUMENT_DETAIL', " + nDocDetailID.toString() + ")");
            statment.executeUpdate();
        }
        if (htTableKey.containsKey("DMS_VERSION")) {
            statment = conn.prepareStatement("UPDATE SYS_TABLE_KEY SET TABLE_KEY_MAX=" + nVersionID.toString() + " WHERE TABLE_NAME='DMS_VERSION'");
            statment.executeUpdate();
        } else {
            statment = conn.prepareStatement("INSERT INTO SYS_TABLE_KEY VALUES('DMS_VERSION', " + nVersionID.toString() + ")");
            statment.executeUpdate();
        }
        if (htTableKey.containsKey("DMS_CONTENT")) {
            statment = conn.prepareStatement("UPDATE SYS_TABLE_KEY SET TABLE_KEY_MAX=" + nContentID.toString() + " WHERE TABLE_NAME='DMS_CONTENT'");
            statment.executeUpdate();
        } else {
            statment = conn.prepareStatement("INSERT INTO SYS_TABLE_KEY VALUES('DMS_CONTENT', " + nContentID.toString() + ")");
            statment.executeUpdate();
        }
        if (htTableKey.containsKey("MTM_DOCUMENT_VERSION")) {
            statment = conn.prepareStatement("UPDATE SYS_TABLE_KEY SET TABLE_KEY_MAX=" + nMtmDocVersionID.toString() + " WHERE TABLE_NAME='MTM_DOCUMENT_VERSION'");
            statment.executeUpdate();
        } else {
            statment = conn.prepareStatement("INSERT INTO SYS_TABLE_KEY VALUES('MTM_DOCUMENT_VERSION', " + nMtmDocVersionID.toString() + ")");
            statment.executeUpdate();
        }
        statment.close();
        System.out.println("final value: " + " DocumentID " + nDocID + " DocDetailID " + nDocDetailID + " DocVersion " + nVersionID + " DocContent " + nContentID + " nMtmDocVersionID " + nMtmDocVersionID);
        closeDBConn();
    }

    public static void genUserDefinedIndex(Connection conn, Hashtable htTableKey) throws Exception {
        java.util.Date today = new java.util.Date();
        String sqlItem1 = "";
        int nUdfID = 1;
        int nUdfDetailID = 1;
        if (htTableKey.containsKey("SYS_USER_DEFINED_INDEX")) {
            nUdfID = ((Integer) htTableKey.get("SYS_USER_DEFINED_INDEX")).intValue() + 1;
        }
        if (htTableKey.containsKey("SYS_USER_DEFINED_INDEX_DETAIL")) {
            nUdfDetailID = ((Integer) htTableKey.get("SYS_USER_DEFINED_INDEX_DETAIL")).intValue() + 1;
        }
        sUdfID = (new Integer(nUdfID)).toString();
        sUdfDetailID = (new Integer(nUdfDetailID)).toString();
        StringBuffer sbUdf = new StringBuffer();
        sbUdf.append("SELECT ID, USER_DEFINED_TYPE FROM SYS_USER_DEFINED_INDEX WHERE USER_DEFINED_TYPE = 'Document Reference' ");
        PreparedStatement preStatUdf = null;
        ResultSet rsUdf = null;
        preStatUdf = conn.prepareStatement(sbUdf.toString());
        rsUdf = preStatUdf.executeQuery();
        if (rsUdf.next()) {
            sUdfID = (new Integer(rsUdf.getInt("ID"))).toString();
            String detail = "SELECT ID FROM SYS_USER_DEFINED_INDEX_DETAIL WHERE USER_DEFINED_ID = " + sUdfID;
            PreparedStatement preStatUdfDetail = conn.prepareStatement(detail);
            ResultSet rsUdfDetail = preStatUdfDetail.executeQuery();
            if (rsUdfDetail.next()) {
                sUdfDetailID = (new Integer(rsUdfDetail.getInt("ID"))).toString();
            }
            preStatUdfDetail.close();
            rsUdfDetail.close();
        } else {
            System.out.println("Inside create udf.....");
            String insertUdf = sqlItem1 = "INSERT INTO SYS_USER_DEFINED_INDEX VALUES(" + sUdfID + ", 0, 'D', 'Document Reference','Document Reference', 'N', '0', 'A', 0, 0,'" + dmsDocument.getCreateDate().toString() + "',0,'" + dmsDocument.getCreateDate().toString() + "')";
            String insertUdfDetail = sqlItem1 = "INSERT INTO SYS_USER_DEFINED_INDEX_DETAIL VALUES(" + sUdfDetailID + "," + sUdfID + ",'Reference Number','Y', 'I', 'A', 0, 0, '" + dmsDocument.getCreateDate().toString() + "',0,'" + dmsDocument.getCreateDate().toString() + "')";
            preStatUdf = conn.prepareStatement(insertUdf);
            PreparedStatement preStatUdfDetail = conn.prepareStatement(insertUdfDetail);
            preStatUdf.executeUpdate();
            preStatUdfDetail.executeUpdate();
            preStatUdfDetail.close();
        }
        preStatUdf.close();
        rsUdf.close();
        System.out.println("UDF = " + sUdfID + " detail = " + sUdfDetailID);
    }

    public static Hashtable getTableKey(Connection conn) throws Exception {
        StringBuffer sbTableKey = new StringBuffer();
        sbTableKey.append("SELECT * FROM SYS_TABLE_KEY");
        Hashtable htTableKey = new Hashtable();
        PreparedStatement preStatTableKey = null;
        ResultSet rsTableKey = null;
        preStatTableKey = conn.prepareStatement(sbTableKey.toString());
        rsTableKey = preStatTableKey.executeQuery();
        while (rsTableKey.next()) {
            htTableKey.put(rsTableKey.getString(1).trim(), new Integer(rsTableKey.getInt(2)));
        }
        preStatTableKey.close();
        rsTableKey.close();
        return htTableKey;
    }

    public static void openDBConn() throws Exception {
        Class.forName(driverStr);
        conn = DriverManager.getConnection(dbConnStr, dbUsername, dbPassword);
        conn.setAutoCommit(true);
    }

    public static void closeDBConn() throws Exception {
        try {
            rs.close();
        } catch (Exception ignore) {
        } finally {
            rs = null;
        }
        try {
            stat.close();
        } catch (Exception ignore) {
        } finally {
            stat = null;
        }
        try {
            conn.close();
        } catch (Exception ignore) {
        } finally {
            conn = null;
        }
    }

    /**
   * indexDocument - Index the document to index search engine.
   *
   * @param dmsDocument      The document object.
   * @param data             The document data stream.
   * @param mode             The mode of operation.
   * @throws ApplicationException
   */
    public static void indexDocument(DmsDocument dmsDocument, InputStream data, String mode) throws ApplicationException {
        indexDocument(dmsDocument, data, mode, null, false);
    }

    /**
   * indexDocument - Index the document to index search engine.
   *
   * @param dmsDocument      The document object.
   * @param data             The document data stream.
   * @param mode             The mode of operation.
   * @param fields           The fields need to be updated. (Applicable for UPDATE MODE Only)
   * @param excludeFlag      Whether the "fields" arguments are excluding fields. (Applicable for UPDATE MODE Only)
   * @throws ApplicationException
   */
    public static void indexDocument(DmsDocument dmsDocument, InputStream data, String mode, String[] fields, boolean excludeFlag) throws ApplicationException {
        IndexManager idxHandler = new IndexManager(indexPath);
        idxHandler.insertDocument(DmsIndexConverter.getIndexDocument(dmsDocument, data));
    }

    public static String formatNumber(int num) {
        StringBuffer result = new StringBuffer();
        NumberFormat df = NumberFormat.getInstance();
        df.setGroupingUsed(false);
        df.setMinimumIntegerDigits(6);
        String numValue = df.format(num);
        System.out.println("num value = " + numValue);
        result.append("FullText" + numValue.substring(0, 2) + " ");
        result.append("FullText" + numValue.substring(2, 4) + " ");
        result.append("FullText" + numValue.substring(4) + " ");
        return result.toString();
    }

    public static java.sql.Timestamp getCurrentTimestamp() {
        java.util.Calendar tmp = java.util.Calendar.getInstance();
        tmp.clear(java.util.Calendar.MILLISECOND);
        return (new java.sql.Timestamp(tmp.getTime().getTime()));
    }
}
