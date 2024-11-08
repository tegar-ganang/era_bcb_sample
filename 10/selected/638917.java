package com.knowgate.hipermail;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Blob;
import java.sql.Types;
import java.sql.Statement;
import java.sql.CallableStatement;
import java.math.BigDecimal;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import javax.mail.Session;
import javax.mail.Folder;
import javax.mail.UIDFolder;
import javax.mail.Store;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.StoreClosedException;
import javax.mail.FolderClosedException;
import javax.mail.MessagingException;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Multipart;
import javax.mail.URLName;
import javax.mail.BodyPart;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.ParseException;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;
import java.util.Properties;
import java.util.NoSuchElementException;
import com.sun.mail.util.BASE64DecoderStream;
import com.knowgate.jdc.JDCConnection;
import com.knowgate.debug.DebugFile;
import com.knowgate.dfs.FileSystem;
import com.knowgate.dataobjs.DB;
import com.knowgate.dataobjs.DBTable;
import com.knowgate.dataobjs.DBBind;
import com.knowgate.dataobjs.DBSubset;
import com.knowgate.dataobjs.DBPersist;
import com.knowgate.dataobjs.DBSubset;
import com.knowgate.hipergate.Category;
import com.knowgate.misc.Gadgets;
import com.knowgate.misc.MD5;
import com.knowgate.hipergate.Product;
import com.knowgate.hipergate.ProductLocation;

/**
 * <p>A subclass of javax.mail.Folder providing storage for MimeMessages at database
 * LONGVARBINARY columns and MBOX files.</p>
 * Folders are also a subclass of com.knowgate.hipergate.Category<br>
 * Category behaviour is obtained by delegation to a private Category instance.<br>
 * For each DBFolder there is a corresponding row at k_categories database table.
 * @author Sergio Montoro Ten
 * @version 1.0
 */
public class DBFolder extends Folder {

    public static final int MODE_MBOX = 64;

    public static final int MODE_BLOB = 128;

    private int iOpenMode;

    private Category oCatg;

    private JDCConnection oConn;

    private String sFolderDir, sFolderName;

    protected DBFolder(Store oStor, String sName) {
        super(oStor);
        oCatg = new Category();
        iOpenMode = 0;
        sFolderName = sName;
    }

    protected JDCConnection getConnection() {
        return ((DBStore) getStore()).getConnection();
    }

    /**
   * Get instance of com.knowgate.hipergate.Category object
   */
    public Category getCategory() {
        return oCatg;
    }

    /**
   * Append messages to this DBFolder
   * @param msgs Array of messages to be appended
   * @throws MessagingException
   */
    public void appendMessages(Message[] msgs) throws MessagingException {
        for (int m = 0; m < msgs.length; m++) appendMessage((MimeMessage) msgs[m]);
    }

    /**
   * Copy a DBMimeMessage from another DBFolder to this DBFolder
   * @param oSrcMsg Source message.
   * @return GUID of new message
   * @throws MessagingException
   */
    public String copyMessage(DBMimeMessage oSrcMsg) throws MessagingException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBFolder.copyMessage()");
            DebugFile.incIdent();
        }
        BigDecimal oPg = null;
        BigDecimal oPos = null;
        int iLen = 0;
        String sId = null;
        try {
            String sSQL = "SELECT " + DB.pg_message + "," + DB.id_message + "," + DB.nu_position + "," + DB.len_mimemsg + " FROM " + DB.k_mime_msgs + " WHERE " + DB.gu_mimemsg + "=";
            if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + "'" + oSrcMsg.getMessageGuid() + "')");
            PreparedStatement oStmt = getConnection().prepareStatement(sSQL + "?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            oStmt.setString(1, oSrcMsg.getMessageGuid());
            ResultSet oRSet = oStmt.executeQuery();
            oRSet.next();
            oPg = oRSet.getBigDecimal(1);
            sId = oRSet.getString(2);
            oPos = oRSet.getBigDecimal(3);
            iLen = oRSet.getInt(4);
            oRSet.close();
            oStmt.close();
        } catch (SQLException sqle) {
            try {
                getConnection().rollback();
            } catch (Exception ignore) {
            }
            if (DebugFile.trace) {
                DebugFile.writeln("DBFolder.copyMessage() SQLException " + sqle.getMessage());
                DebugFile.decIdent();
            }
            throw new MessagingException("DBFolder.copyMessage() SQLException " + sqle.getMessage(), sqle);
        }
        if (null == oPg) throw new MessagingException("DBFolder.copyMessage() Source Message not found");
        DBFolder oSrcFldr = (DBFolder) oSrcMsg.getFolder();
        MboxFile oMboxSrc = null;
        MimeMessage oMimeSrc;
        String sNewGuid = null;
        try {
            if ((oSrcFldr.mode & MODE_MBOX) != 0) {
                oMboxSrc = new MboxFile(oSrcFldr.getFile(), MboxFile.READ_ONLY);
                InputStream oInStrm = oMboxSrc.getMessageAsStream(oPos.longValue(), iLen);
                oMimeSrc = new MimeMessage(Session.getDefaultInstance(new Properties()), oInStrm);
                oInStrm.close();
                oMboxSrc.close();
                oMboxSrc = null;
                String sId2 = oMimeSrc.getMessageID();
                if ((sId != null) && (sId2 != null)) {
                    if (!sId.trim().equals(sId2.trim())) {
                        throw new MessagingException("MessageID " + sId + " at database does not match MessageID " + oMimeSrc.getMessageID() + " at MBOX file " + oSrcFldr.getFile().getName() + " for message index " + oPg.toString());
                    }
                }
                appendMessage(oMimeSrc);
            } else {
                ByteArrayOutputStream oByOutStrm = new ByteArrayOutputStream();
                oSrcMsg.writeTo(oByOutStrm);
                ByteArrayInputStream oByInStrm = new ByteArrayInputStream(oByOutStrm.toByteArray());
                oByOutStrm.close();
                oMimeSrc = new MimeMessage(Session.getDefaultInstance(new Properties()), oByInStrm);
                oByInStrm.close();
                appendMessage(oMimeSrc);
            }
        } catch (Exception e) {
            if (oMboxSrc != null) {
                try {
                    oMboxSrc.close();
                } catch (Exception ignore) {
                }
            }
            try {
                oSrcFldr.getConnection().rollback();
            } catch (Exception ignore) {
            }
            if (DebugFile.trace) {
                DebugFile.writeln("DBFolder.copyMessage() " + e.getClass().getName() + e.getMessage());
                DebugFile.decIdent();
            }
            throw new MessagingException(e.getMessage(), e);
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBFolder.copyMessage() : " + sNewGuid);
        }
        return sNewGuid;
    }

    /**
   * Move a DBMimeMessage from another DBFolder to this DBFolder
   * @param oSrcMsg Source message
   * @throws MessagingException
   */
    public void moveMessage(DBMimeMessage oSrcMsg) throws MessagingException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBFolder.moveMessage()");
            DebugFile.incIdent();
        }
        JDCConnection oConn = null;
        PreparedStatement oStmt = null;
        ResultSet oRSet = null;
        BigDecimal oPg = null;
        BigDecimal oPos = null;
        int iLen = 0;
        try {
            oConn = ((DBStore) getStore()).getConnection();
            oStmt = oConn.prepareStatement("SELECT " + DB.pg_message + "," + DB.nu_position + "," + DB.len_mimemsg + " FROM " + DB.k_mime_msgs + " WHERE " + DB.gu_mimemsg + "=?");
            oStmt.setString(1, oSrcMsg.getMessageGuid());
            oRSet = oStmt.executeQuery();
            if (oRSet.next()) {
                oPg = oRSet.getBigDecimal(1);
                oPos = oRSet.getBigDecimal(2);
                iLen = oRSet.getInt(3);
            }
            oRSet.close();
            oRSet = null;
            oStmt.close();
            oStmt = null;
            oConn.setAutoCommit(false);
            oStmt = oConn.prepareStatement("UPDATE " + DB.k_categories + " SET " + DB.len_size + "=" + DB.len_size + "-" + String.valueOf(iLen) + " WHERE " + DB.gu_category + "=?");
            oStmt.setString(1, ((DBFolder) (oSrcMsg.getFolder())).getCategory().getString(DB.gu_category));
            oStmt.executeUpdate();
            oStmt.close();
            oStmt = null;
            oStmt = oConn.prepareStatement("UPDATE " + DB.k_categories + " SET " + DB.len_size + "=" + DB.len_size + "+" + String.valueOf(iLen) + " WHERE " + DB.gu_category + "=?");
            oStmt.setString(1, getCategory().getString(DB.gu_category));
            oStmt.executeUpdate();
            oStmt.close();
            oStmt = null;
            oConn.commit();
        } catch (SQLException sqle) {
            if (null != oRSet) {
                try {
                    oRSet.close();
                } catch (Exception ignore) {
                }
            }
            if (null != oStmt) {
                try {
                    oStmt.close();
                } catch (Exception ignore) {
                }
            }
            if (null != oConn) {
                try {
                    oConn.rollback();
                } catch (Exception ignore) {
                }
            }
            throw new MessagingException(sqle.getMessage(), sqle);
        }
        if (null == oPg) throw new MessagingException("Source message not found");
        if (null == oPos) throw new MessagingException("Source message position is not valid");
        DBFolder oSrcFldr = (DBFolder) oSrcMsg.getFolder();
        MboxFile oMboxSrc = null, oMboxThis = null;
        try {
            oMboxSrc = new MboxFile(oSrcFldr.getFile(), MboxFile.READ_WRITE);
            oMboxThis = new MboxFile(oSrcFldr.getFile(), MboxFile.READ_WRITE);
            oMboxThis.appendMessage(oMboxSrc, oPos.longValue(), iLen);
            oMboxThis.close();
            oMboxThis = null;
            oMboxSrc.purge(new int[] { oPg.intValue() });
            oMboxSrc.close();
            oMboxSrc = null;
        } catch (Exception e) {
            if (oMboxThis != null) {
                try {
                    oMboxThis.close();
                } catch (Exception ignore) {
                }
            }
            if (oMboxSrc != null) {
                try {
                    oMboxSrc.close();
                } catch (Exception ignore) {
                }
            }
            throw new MessagingException(e.getMessage(), e);
        }
        try {
            oConn = ((DBStore) getStore()).getConnection();
            BigDecimal dNext = getNextMessage();
            String sCatGuid = getCategory().getString(DB.gu_category);
            oStmt = oConn.prepareStatement("UPDATE " + DB.k_mime_msgs + " SET " + DB.gu_category + "=?," + DB.pg_message + "=? WHERE " + DB.gu_mimemsg + "=?");
            oStmt.setString(1, sCatGuid);
            oStmt.setBigDecimal(2, dNext);
            oStmt.setString(3, oSrcMsg.getMessageGuid());
            oStmt.executeUpdate();
            oStmt.close();
            oStmt = null;
            oConn.commit();
        } catch (SQLException sqle) {
            if (null != oStmt) {
                try {
                    oStmt.close();
                } catch (Exception ignore) {
                }
            }
            if (null != oConn) {
                try {
                    oConn.rollback();
                } catch (Exception ignore) {
                }
            }
            throw new MessagingException(sqle.getMessage(), sqle);
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBFolder.moveMessage()");
        }
    }

    /**
   * This method is not implemented and will always raise UnsupportedOperationException
   * @throws UnsupportedOperationException
   */
    public boolean create(int type) throws MessagingException {
        throw new UnsupportedOperationException("DBFolder.create()");
    }

    /**
   * Create DBFolder with given name under current user mailroot Category
   * @param sFolderName Folder Name
   * @return <b>true</b>
   * @throws MessagingException
   */
    public boolean create(String sFolderName) throws MessagingException {
        try {
            String sGuid = ((DBStore) getStore()).getUser().getMailFolder(getConnection(), Category.makeName(getConnection(), sFolderName));
            oCatg = new Category(getConnection(), sGuid);
        } catch (SQLException sqle) {
            throw new MessagingException(sqle.getMessage(), sqle);
        }
        return true;
    }

    /**
   * Open this DBFolder
   * @param mode {READ_ONLY|READ_WRITE}
   * @throws MessagingException
   */
    public void open(int mode) throws MessagingException {
        final int ALL_OPTIONS = READ_ONLY | READ_WRITE | MODE_MBOX | MODE_BLOB;
        if (DebugFile.trace) {
            DebugFile.writeln("DBFolder.open(" + String.valueOf(mode) + ")");
            DebugFile.incIdent();
        }
        if ((0 == (mode & READ_ONLY)) && (0 == (mode & READ_WRITE))) {
            if (DebugFile.trace) DebugFile.decIdent();
            throw new MessagingException("Folder must be opened in either READ_ONLY or READ_WRITE mode");
        } else if (ALL_OPTIONS != (mode | ALL_OPTIONS)) {
            if (DebugFile.trace) DebugFile.decIdent();
            throw new MessagingException("Invalid DBFolder open() option mode");
        } else {
            if ((0 == (mode & MODE_MBOX)) && (0 == (mode & MODE_BLOB))) mode = mode | MODE_MBOX;
            iOpenMode = mode;
            oConn = ((DBStore) getStore()).getConnection();
            if ((iOpenMode & MODE_MBOX) != 0) {
                String sFolderUrl;
                try {
                    sFolderUrl = Gadgets.chomp(getStore().getURLName().getFile(), File.separator) + oCatg.getPath(oConn);
                    if (DebugFile.trace) DebugFile.writeln("mail folder directory is " + sFolderUrl);
                    if (sFolderUrl.startsWith("file://")) sFolderDir = sFolderUrl.substring(7); else sFolderDir = sFolderUrl;
                } catch (SQLException sqle) {
                    iOpenMode = 0;
                    oConn = null;
                    if (DebugFile.trace) DebugFile.decIdent();
                    throw new MessagingException(sqle.getMessage(), sqle);
                }
                try {
                    File oDir = new File(sFolderDir);
                    if (!oDir.exists()) {
                        FileSystem oFS = new FileSystem();
                        oFS.mkdirs(sFolderUrl);
                    }
                } catch (IOException ioe) {
                    iOpenMode = 0;
                    oConn = null;
                    if (DebugFile.trace) DebugFile.decIdent();
                    throw new MessagingException(ioe.getMessage(), ioe);
                } catch (SecurityException se) {
                    iOpenMode = 0;
                    oConn = null;
                    if (DebugFile.trace) DebugFile.decIdent();
                    throw new MessagingException(se.getMessage(), se);
                } catch (Exception je) {
                    iOpenMode = 0;
                    oConn = null;
                    if (DebugFile.trace) DebugFile.decIdent();
                    throw new MessagingException(je.getMessage(), je);
                }
                JDCConnection oConn = getConnection();
                PreparedStatement oStmt = null;
                ResultSet oRSet = null;
                boolean bHasFilePointer;
                try {
                    oStmt = oConn.prepareStatement("SELECT NULL FROM " + DB.k_x_cat_objs + " WHERE " + DB.gu_category + "=? AND " + DB.id_class + "=15", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                    oStmt.setString(1, getCategory().getString(DB.gu_category));
                    oRSet = oStmt.executeQuery();
                    bHasFilePointer = oRSet.next();
                    oRSet.close();
                    oRSet = null;
                    oStmt.close();
                    oStmt = null;
                    if (!bHasFilePointer) {
                        oConn.setAutoCommit(false);
                        Product oProd = new Product();
                        oProd.put(DB.gu_owner, oCatg.getString(DB.gu_owner));
                        oProd.put(DB.nm_product, oCatg.getString(DB.nm_category));
                        oProd.store(oConn);
                        ProductLocation oLoca = new ProductLocation();
                        oLoca.put(DB.gu_product, oProd.getString(DB.gu_product));
                        oLoca.put(DB.gu_owner, oCatg.getString(DB.gu_owner));
                        oLoca.put(DB.pg_prod_locat, 1);
                        oLoca.put(DB.id_cont_type, 1);
                        oLoca.put(DB.id_prod_type, "MBOX");
                        oLoca.put(DB.len_file, 0);
                        oLoca.put(DB.xprotocol, "file://");
                        oLoca.put(DB.xhost, "localhost");
                        oLoca.put(DB.xpath, Gadgets.chomp(sFolderDir, File.separator));
                        oLoca.put(DB.xfile, oCatg.getString(DB.nm_category) + ".mbox");
                        oLoca.put(DB.xoriginalfile, oCatg.getString(DB.nm_category) + ".mbox");
                        oLoca.store(oConn);
                        oStmt = oConn.prepareStatement("INSERT INTO " + DB.k_x_cat_objs + " (" + DB.gu_category + "," + DB.gu_object + "," + DB.id_class + ") VALUES (?,?,15)");
                        oStmt.setString(1, oCatg.getString(DB.gu_category));
                        oStmt.setString(2, oProd.getString(DB.gu_product));
                        oStmt.executeUpdate();
                        oStmt.close();
                        oStmt = null;
                        oConn.commit();
                    }
                } catch (SQLException sqle) {
                    if (DebugFile.trace) {
                        DebugFile.writeln("SQLException " + sqle.getMessage());
                        DebugFile.decIdent();
                    }
                    if (oStmt != null) {
                        try {
                            oStmt.close();
                        } catch (SQLException ignore) {
                        }
                    }
                    if (oConn != null) {
                        try {
                            oConn.rollback();
                        } catch (SQLException ignore) {
                        }
                    }
                    throw new MessagingException(sqle.getMessage(), sqle);
                }
            } else {
                sFolderDir = null;
            }
            if (DebugFile.trace) {
                DebugFile.decIdent();
                String sMode = "";
                if ((iOpenMode & READ_WRITE) != 0) sMode += " READ_WRITE ";
                if ((iOpenMode & READ_ONLY) != 0) sMode += " READ_ONLY ";
                if ((iOpenMode & MODE_BLOB) != 0) sMode += " MODE_BLOB ";
                if ((iOpenMode & MODE_MBOX) != 0) sMode += " MODE_MBOX ";
                DebugFile.writeln("End DBFolder.open() :");
            }
        }
    }

    /**
   * Close this folder
   * @param expunge
   * @throws MessagingException
   */
    public void close(boolean expunge) throws MessagingException {
        if (expunge) expunge();
        iOpenMode = 0;
        oConn = null;
        sFolderDir = null;
    }

    public boolean delete(boolean recurse) throws MessagingException {
        try {
            return oCatg.delete(getConnection());
        } catch (SQLException sqle) {
            throw new MessagingException(sqle.getMessage(), sqle);
        }
    }

    public Folder getFolder(String name) throws MessagingException {
        return ((DBStore) getStore()).getFolder(name);
    }

    /**
   * This method is not implemented and will always raise UnsupportedOperationException
   * @throws UnsupportedOperationException
   */
    public boolean hasNewMessages() throws MessagingException {
        throw new UnsupportedOperationException("DBFolder.hasNewMessages()");
    }

    public boolean renameTo(Folder f) throws MessagingException, StoreClosedException, NullPointerException {
        String[] aLabels = new String[] { "en", "es", "fr", "de", "it", "pt", "ca", "ja", "cn", "tw", "fi", "ru", "pl", "nl", "xx" };
        PreparedStatement oUpdt = null;
        if (!((DBStore) getStore()).isConnected()) throw new StoreClosedException(getStore(), "Store is not connected");
        if (oCatg.isNull(DB.gu_category)) throw new NullPointerException("Folder is closed");
        try {
            oUpdt = getConnection().prepareStatement("DELETE FROM " + DB.k_cat_labels + " WHERE " + DB.gu_category + "=?");
            oUpdt.setString(1, oCatg.getString(DB.gu_category));
            oUpdt.executeUpdate();
            oUpdt.close();
            oUpdt.getConnection().prepareStatement("INSERT INTO " + DB.k_cat_labels + " (" + DB.gu_category + "," + DB.id_language + "," + DB.tr_category + "," + DB.url_category + ") VALUES (?,?,?,NULL)");
            oUpdt.setString(1, oCatg.getString(DB.gu_category));
            for (int l = 0; l < aLabels.length; l++) {
                oUpdt.setString(2, aLabels[l]);
                oUpdt.setString(3, f.getName().substring(0, 1).toUpperCase() + f.getName().substring(1).toLowerCase());
                oUpdt.executeUpdate();
            }
            oUpdt.close();
            oUpdt = null;
            getConnection().commit();
        } catch (SQLException sqle) {
            try {
                if (null != oUpdt) oUpdt.close();
            } catch (SQLException ignore) {
            }
            try {
                getConnection().rollback();
            } catch (SQLException ignore) {
            }
            throw new MessagingException(sqle.getMessage(), sqle);
        }
        return true;
    }

    public boolean exists() throws MessagingException, StoreClosedException {
        if (!((DBStore) getStore()).isConnected()) throw new StoreClosedException(getStore(), "Store is not connected");
        try {
            return oCatg.exists(getConnection());
        } catch (SQLException sqle) {
            throw new MessagingException(sqle.getMessage(), sqle);
        }
    }

    public Message[] expunge() throws MessagingException {
        Statement oStmt = null;
        CallableStatement oCall = null;
        PreparedStatement oUpdt = null;
        ResultSet oRSet;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBFolder.expunge()");
            DebugFile.incIdent();
        }
        if (0 == (iOpenMode & READ_WRITE)) {
            if (DebugFile.trace) DebugFile.decIdent();
            throw new javax.mail.FolderClosedException(this, "Folder is not open is READ_WRITE mode");
        }
        if ((0 == (iOpenMode & MODE_MBOX)) && (0 == (iOpenMode & MODE_BLOB))) {
            if (DebugFile.trace) DebugFile.decIdent();
            throw new javax.mail.FolderClosedException(this, "Folder is not open in MBOX nor BLOB mode");
        }
        MboxFile oMBox = null;
        DBSubset oDeleted = new DBSubset(DB.k_mime_msgs, DB.gu_mimemsg + "," + DB.pg_message, DB.bo_deleted + "=1 AND " + DB.gu_category + "='" + oCatg.getString(DB.gu_category) + "'", 100);
        try {
            int iDeleted = oDeleted.load(getConnection());
            File oFile = getFile();
            if (oFile.exists() && iDeleted > 0) {
                oMBox = new MboxFile(oFile, MboxFile.READ_WRITE);
                int[] msgnums = new int[iDeleted];
                for (int m = 0; m < iDeleted; m++) msgnums[m] = oDeleted.getInt(1, m);
                oMBox.purge(msgnums);
                oMBox.close();
            }
            oStmt = oConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            oRSet = oStmt.executeQuery("SELECT p." + DB.file_name + " FROM " + DB.k_mime_parts + " p," + DB.k_mime_msgs + " m WHERE p." + DB.gu_mimemsg + "=m." + DB.gu_mimemsg + " AND m." + DB.id_disposition + "='reference' AND m." + DB.bo_deleted + "=1 AND m." + DB.gu_category + "='" + oCatg.getString(DB.gu_category) + "'");
            while (oRSet.next()) {
                String sFileName = oRSet.getString(1);
                if (!oRSet.wasNull()) {
                    try {
                        File oRef = new File(sFileName);
                        oRef.delete();
                    } catch (SecurityException se) {
                        if (DebugFile.trace) DebugFile.writeln("SecurityException " + sFileName + " " + se.getMessage());
                    }
                }
            }
            oRSet.close();
            oRSet = null;
            oStmt.close();
            oStmt = null;
            oFile = getFile();
            oStmt = oConn.createStatement();
            oStmt.executeUpdate("UPDATE " + DB.k_categories + " SET " + DB.len_size + "=" + String.valueOf(oFile.length()) + " WHERE " + DB.gu_category + "='" + getCategory().getString(DB.gu_category) + "'");
            oStmt.close();
            oStmt = null;
            if (oConn.getDataBaseProduct() == JDCConnection.DBMS_POSTGRESQL) {
                oStmt = oConn.createStatement();
                for (int d = 0; d < iDeleted; d++) oStmt.executeQuery("SELECT k_sp_del_mime_msg('" + oDeleted.getString(0, d) + "')");
                oStmt.close();
                oStmt = null;
            } else {
                oCall = oConn.prepareCall("{ call k_sp_del_mime_msg(?) }");
                for (int d = 0; d < iDeleted; d++) {
                    oCall.setString(1, oDeleted.getString(0, d));
                    oCall.execute();
                }
                oCall.close();
                oCall = null;
            }
            if (oFile.exists() && iDeleted > 0) {
                BigDecimal oUnit = new BigDecimal(1);
                oStmt = oConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                oRSet = oStmt.executeQuery("SELECT MAX(" + DB.pg_message + ") FROM " + DB.k_mime_msgs + " WHERE " + DB.gu_category + "='getCategory().getString(DB.gu_category)'");
                oRSet.next();
                BigDecimal oMaxPg = oRSet.getBigDecimal(1);
                if (oRSet.wasNull()) oMaxPg = new BigDecimal(0);
                oRSet.close();
                oRSet = null;
                oStmt.close();
                oStmt = null;
                oMaxPg = oMaxPg.add(oUnit);
                oStmt = oConn.createStatement();
                oStmt.executeUpdate("UPDATE " + DB.k_mime_msgs + " SET " + DB.pg_message + "=" + DB.pg_message + "+" + oMaxPg.toString() + " WHERE " + DB.gu_category + "='" + getCategory().getString(DB.gu_category) + "'");
                oStmt.close();
                oStmt = null;
                DBSubset oMsgSet = new DBSubset(DB.k_mime_msgs, DB.gu_mimemsg + "," + DB.pg_message, DB.gu_category + "='" + getCategory().getString(DB.gu_category) + "' ORDER BY " + DB.pg_message, 1000);
                int iMsgCount = oMsgSet.load(oConn);
                oMBox = new MboxFile(oFile, MboxFile.READ_ONLY);
                long[] aPositions = oMBox.getMessagePositions();
                oMBox.close();
                if (iMsgCount != aPositions.length) {
                    throw new IOException("DBFolder.expunge() Message count of " + String.valueOf(aPositions.length) + " at MBOX file " + oFile.getName() + " does not match message count at database index of " + String.valueOf(iMsgCount));
                }
                oMaxPg = new BigDecimal(0);
                oUpdt = oConn.prepareStatement("UPDATE " + DB.k_mime_msgs + " SET " + DB.pg_message + "=?," + DB.nu_position + "=? WHERE " + DB.gu_mimemsg + "=?");
                for (int m = 0; m < iMsgCount; m++) {
                    oUpdt.setBigDecimal(1, oMaxPg);
                    oUpdt.setBigDecimal(2, new BigDecimal(aPositions[m]));
                    oUpdt.setString(3, oMsgSet.getString(0, m));
                    oUpdt.executeUpdate();
                    oMaxPg = oMaxPg.add(oUnit);
                }
                oUpdt.close();
            }
            oConn.commit();
        } catch (SQLException sqle) {
            try {
                if (oMBox != null) oMBox.close();
            } catch (Exception e) {
            }
            try {
                if (oStmt != null) oStmt.close();
            } catch (Exception e) {
            }
            try {
                if (oCall != null) oCall.close();
            } catch (Exception e) {
            }
            try {
                if (oConn != null) oConn.rollback();
            } catch (Exception e) {
            }
            throw new MessagingException(sqle.getMessage(), sqle);
        } catch (IOException sqle) {
            try {
                if (oMBox != null) oMBox.close();
            } catch (Exception e) {
            }
            try {
                if (oStmt != null) oStmt.close();
            } catch (Exception e) {
            }
            try {
                if (oCall != null) oCall.close();
            } catch (Exception e) {
            }
            try {
                if (oConn != null) oConn.rollback();
            } catch (Exception e) {
            }
            throw new MessagingException(sqle.getMessage(), sqle);
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBFolder.expunge()");
        }
        return null;
    }

    public String getFullName() {
        try {
            if (oCatg.exists(getConnection())) return oCatg.getPath(getConnection()); else return null;
        } catch (SQLException sqle) {
            return null;
        }
    }

    public String getFilePath() {
        return Gadgets.chomp(sFolderDir, File.separator) + oCatg.getString(DB.nm_category) + ".mbox";
    }

    /**
   * Get MBOX file that holds messages for this DBFolder
   * @return java.io.File object representing MBOX file.
   */
    public File getFile() {
        return new File(getFilePath());
    }

    public String getName() {
        return sFolderName == null ? oCatg.getString(DB.nm_category) : sFolderName;
    }

    public URLName getURLName() throws MessagingException, StoreClosedException {
        if (!((DBStore) getStore()).isConnected()) throw new StoreClosedException(getStore(), "Store is not connected");
        com.knowgate.acl.ACLUser oUsr = ((DBStore) getStore()).getUser();
        return new URLName("jdbc://", "localhost", -1, oCatg.getString(DB.gu_category), oUsr.getString(DB.gu_user), oUsr.getString(DB.tx_pwd));
    }

    protected Message getMessage(String sMsgId, int IdType) throws MessagingException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBFolder.getMessage(" + sMsgId + ")");
            DebugFile.incIdent();
        }
        if (!isOpen()) {
            if (DebugFile.trace) DebugFile.decIdent();
            throw new javax.mail.FolderClosedException(this, "Folder is closed");
        }
        DBMimeMessage oRetVal = null;
        PreparedStatement oStmt = null;
        ResultSet oRSet = null;
        String sSQL;
        try {
            switch(IdType) {
                case 1:
                    sSQL = "SELECT * FROM " + DB.k_mime_msgs + " WHERE " + DB.gu_mimemsg + "=?";
                    if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + ")");
                    oStmt = oConn.prepareStatement(sSQL, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                    oStmt.setString(1, sMsgId);
                    break;
                case 2:
                    sSQL = "SELECT * FROM " + DB.k_mime_msgs + " WHERE " + DB.id_message + "=?";
                    if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + ")");
                    oStmt = oConn.prepareStatement(sSQL, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                    oStmt.setString(1, sMsgId);
                    break;
                case 3:
                    sSQL = "SELECT * FROM " + DB.k_mime_msgs + " WHERE " + DB.pg_message + "=?";
                    if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + ")");
                    oStmt = oConn.prepareStatement(sSQL, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                    oStmt.setBigDecimal(1, new java.math.BigDecimal(sMsgId));
                    break;
            }
            oRSet = oStmt.executeQuery();
            if (oRSet.next()) {
                MimeMultipart oParts = new MimeMultipart();
                if (DebugFile.trace) DebugFile.writeln("ResultSet.getBinaryStream(" + DB.by_content + ")");
                InputStream oLongVarBin = oRSet.getBinaryStream(DB.by_content);
                if (!oRSet.wasNull()) {
                    if (DebugFile.trace) DebugFile.writeln("MimeMultipart.addBodyPart(new MimeBodyPart(InputStream)");
                    oParts.addBodyPart(new MimeBodyPart(oLongVarBin));
                }
                oRetVal = new DBMimeMessage(((DBStore) getStore()).getSession());
                oRetVal.setMessageGuid(oRSet.getString(DB.gu_mimemsg));
                oRetVal.setFolder(this);
                String sFrom = oRSet.getString(DB.tx_email_from);
                if (!oRSet.wasNull()) {
                    InternetAddress oFrom = new InternetAddress(oRSet.getString(DB.tx_email_from), oRSet.getString(DB.nm_from));
                    oRetVal.setFrom(oFrom);
                }
                String sReplyTo = oRSet.getString(DB.tx_email_reply);
                if (!oRSet.wasNull()) {
                    InternetAddress oReply = new InternetAddress(oRSet.getString(DB.tx_email_reply));
                    oRetVal.setReplyTo(new Address[] { oReply });
                }
                oRetVal.setRecipients(Message.RecipientType.TO, oRetVal.getRecipients(Message.RecipientType.TO));
                oRetVal.setRecipients(Message.RecipientType.CC, oRetVal.getRecipients(Message.RecipientType.CC));
                oRetVal.setRecipients(Message.RecipientType.BCC, oRetVal.getRecipients(Message.RecipientType.BCC));
                oRetVal.setContentID(oRSet.getString(DB.id_message));
                oRetVal.setContentMD5(oRSet.getString(DB.tx_md5));
                oRetVal.setDescription(oRSet.getString(DB.de_mimemsg));
                oRetVal.setDisposition(oRSet.getString(DB.id_disposition));
                oRetVal.setFlag(Flags.Flag.ANSWERED, oRSet.getShort(DB.bo_answered) != 0);
                oRetVal.setFlag(Flags.Flag.DELETED, oRSet.getShort(DB.bo_deleted) != 0);
                oRetVal.setFlag(Flags.Flag.DRAFT, oRSet.getShort(DB.bo_draft) != 0);
                oRetVal.setFlag(Flags.Flag.FLAGGED, oRSet.getShort(DB.bo_flagged) != 0);
                oRetVal.setFlag(Flags.Flag.RECENT, oRSet.getShort(DB.bo_recent) != 0);
                oRetVal.setFlag(Flags.Flag.SEEN, oRSet.getShort(DB.bo_seen) != 0);
                oRetVal.setSentDate(oRSet.getTimestamp(DB.dt_sent));
                String sSubject = oRSet.getString(DB.tx_subject);
                if (!oRSet.wasNull()) if (sSubject.length() > 0) oRetVal.setSubject(oRSet.getString(DB.tx_subject));
                if (DebugFile.trace) DebugFile.writeln("MimeMessage.setContent(MimeMultipart)");
                oRetVal.setContent(oParts);
            }
            oRSet.close();
            oRSet = null;
            oStmt.close();
            oStmt = null;
        } catch (SQLException sqle) {
            try {
                if (oRSet != null) oRSet.close();
            } catch (SQLException ignore) {
            }
            try {
                if (oStmt != null) oStmt.close();
            } catch (SQLException ignore) {
            }
            throw new MessagingException(sqle.getMessage(), sqle);
        } catch (UnsupportedEncodingException uee) {
            try {
                if (oRSet != null) oRSet.close();
            } catch (SQLException ignore) {
            }
            try {
                if (oStmt != null) oStmt.close();
            } catch (SQLException ignore) {
            }
            throw new MessagingException(uee.getMessage(), uee);
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBFolder.getMessage() : " + (oRetVal != null ? "[MimeMessage]" : "null"));
        }
        return oRetVal;
    }

    private void saveMimeParts(MimeMessage oMsg, String sMsgCharSeq, String sBoundary, String sMsgGuid, String sMsgId, int iPgMessage, int iOffset) throws MessagingException, OutOfMemoryError {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBFolder.saveMimeParts([Connection], [MimeMessage], " + sBoundary + ", " + sMsgGuid + "," + sMsgId + ", " + String.valueOf(iPgMessage) + ", " + String.valueOf(iOffset) + ", [Properties])");
            DebugFile.incIdent();
        }
        PreparedStatement oStmt = null;
        Blob oContentTxt;
        ByteArrayOutputStream byOutPart;
        int iPrevPart = 0, iThisPart = 0, iNextPart = 0, iPartStart = 0;
        try {
            MimeMultipart oParts = (MimeMultipart) oMsg.getContent();
            final int iParts = oParts.getCount();
            if (DebugFile.trace) DebugFile.writeln("message has " + String.valueOf(iParts) + " parts");
            if (iParts > 0) {
                if (sMsgCharSeq != null && sBoundary != null && ((iOpenMode & MODE_MBOX) != 0)) {
                    iPrevPart = sMsgCharSeq.indexOf(sBoundary, iPrevPart);
                    if (iPrevPart > 0) {
                        iPrevPart += sBoundary.length();
                        if (DebugFile.trace) DebugFile.writeln("found message boundary token at " + String.valueOf(iPrevPart));
                    }
                }
                String sSQL = "INSERT INTO " + DB.k_mime_parts + "(gu_mimemsg,id_message,pg_message,nu_offset,id_part,id_content,id_type,id_disposition,len_part,de_part,tx_md5,file_name,by_content) VALUES ('" + sMsgGuid + "',?,?,?,?,?,?,?,?,?,NULL,?,?)";
                if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + ")");
                oStmt = oConn.prepareStatement(sSQL);
                for (int p = 0; p < iParts; p++) {
                    if (DebugFile.trace) DebugFile.writeln("processing part " + String.valueOf(p));
                    BodyPart oPart = oParts.getBodyPart(p);
                    byOutPart = new ByteArrayOutputStream(oPart.getSize() > 0 ? oPart.getSize() : 131072);
                    oPart.writeTo(byOutPart);
                    if (sMsgCharSeq != null && sBoundary != null && iPrevPart > 0) {
                        iThisPart = sMsgCharSeq.indexOf(sBoundary, iPrevPart);
                        if (iThisPart > 0) {
                            if (DebugFile.trace) DebugFile.writeln("found part " + String.valueOf(p + iOffset) + " boundary at " + String.valueOf(iThisPart));
                            iPartStart = iThisPart + sBoundary.length();
                            while (iPartStart < sMsgCharSeq.length()) {
                                if (sMsgCharSeq.charAt(iPartStart) != ' ' && sMsgCharSeq.charAt(iPartStart) != '\r' && sMsgCharSeq.charAt(iPartStart) != '\n' && sMsgCharSeq.charAt(iPartStart) != '\t') break; else iPartStart++;
                            }
                        }
                        iNextPart = sMsgCharSeq.indexOf(sBoundary, iPartStart);
                        if (iNextPart < 0) {
                            if (DebugFile.trace) DebugFile.writeln("no next part found");
                            iNextPart = sMsgCharSeq.length();
                        } else {
                            if (DebugFile.trace) DebugFile.writeln("next part boundary found at " + String.valueOf(iNextPart));
                        }
                    }
                    String sContentType = oPart.getContentType();
                    if (sContentType != null) sContentType = MimeUtility.decodeText(sContentType);
                    boolean bForwardedAttachment = false;
                    if ((null != sContentType) && (null != ((DBStore) getStore()).getSession())) {
                        if (DebugFile.trace) DebugFile.writeln("Part Content-Type: " + sContentType.replace('\r', ' ').replace('\n', ' '));
                        if (sContentType.toUpperCase().startsWith("MULTIPART/ALTERNATIVE") || sContentType.toUpperCase().startsWith("MULTIPART/RELATED") || sContentType.toUpperCase().startsWith("MULTIPART/SIGNED")) {
                            try {
                                ByteArrayInputStream byInStrm = new ByteArrayInputStream(byOutPart.toByteArray());
                                MimeMessage oForwarded = new MimeMessage(((DBStore) getStore()).getSession(), byInStrm);
                                saveMimeParts(oForwarded, sMsgCharSeq, getPartsBoundary(oForwarded), sMsgGuid, sMsgId, iPgMessage, iOffset + iParts);
                                byInStrm.close();
                                byInStrm = null;
                                bForwardedAttachment = true;
                            } catch (Exception e) {
                                if (DebugFile.trace) DebugFile.writeln(e.getClass().getName() + " " + e.getMessage());
                            }
                        }
                    }
                    if (!bForwardedAttachment) {
                        oStmt.setString(1, sMsgId);
                        oStmt.setBigDecimal(2, new BigDecimal(iPgMessage));
                        if ((iPartStart > 0) && ((iOpenMode & MODE_MBOX) != 0)) oStmt.setBigDecimal(3, new BigDecimal(iPartStart)); else oStmt.setNull(3, oConn.getDataBaseProduct() == JDCConnection.DBMS_ORACLE ? Types.NUMERIC : Types.DECIMAL);
                        oStmt.setInt(4, p + iOffset);
                        oStmt.setString(5, ((javax.mail.internet.MimeBodyPart) oPart).getContentID());
                        oStmt.setString(6, Gadgets.left(sContentType, 254));
                        oStmt.setString(7, Gadgets.left(oPart.getDisposition(), 100));
                        if ((iOpenMode & MODE_MBOX) != 0) oStmt.setInt(8, iNextPart - iPartStart); else oStmt.setInt(8, oPart.getSize() > 0 ? oPart.getSize() : byOutPart.size());
                        if (oPart.getDescription() != null) oStmt.setString(9, Gadgets.left(MimeUtility.decodeText(oPart.getDescription()), 254)); else oStmt.setNull(9, Types.VARCHAR);
                        if (DebugFile.trace) DebugFile.writeln("file name is " + oPart.getFileName());
                        if (oPart.getFileName() != null) oStmt.setString(10, Gadgets.left(MimeUtility.decodeText(oPart.getFileName()), 254)); else oStmt.setNull(10, Types.VARCHAR);
                        if ((iOpenMode & MODE_BLOB) != 0) oStmt.setBinaryStream(11, new ByteArrayInputStream(byOutPart.toByteArray()), byOutPart.size()); else oStmt.setNull(11, Types.LONGVARBINARY);
                        if (DebugFile.trace) DebugFile.writeln("PreparedStatement.executeUpdate()");
                        oStmt.executeUpdate();
                    }
                    byOutPart.close();
                    byOutPart = null;
                    oContentTxt = null;
                    if ((iOpenMode & MODE_MBOX) != 0) iPrevPart = iNextPart;
                }
                if (DebugFile.trace) DebugFile.writeln("PreparedStatement.close()");
                oStmt.close();
            }
        } catch (SQLException e) {
            if (DebugFile.trace) {
                DebugFile.writeln("SQLException " + e.getMessage());
                DebugFile.decIdent();
            }
            if (null != oStmt) {
                try {
                    oStmt.close();
                } catch (Exception ignore) {
                }
            }
            try {
                if (null != oConn) oConn.rollback();
            } catch (Exception ignore) {
            }
            throw new MessagingException(e.getMessage(), e);
        } catch (IOException e) {
            if (DebugFile.trace) {
                DebugFile.writeln("IOException " + e.getMessage());
                DebugFile.decIdent();
            }
            if (null != oStmt) {
                try {
                    oStmt.close();
                } catch (Exception ignore) {
                }
            }
            throw new MessagingException(e.getMessage(), e);
        } catch (Exception e) {
            if (DebugFile.trace) {
                DebugFile.writeln(e.getClass().getName() + " " + e.getMessage());
                DebugFile.decIdent();
            }
            if (null != oStmt) {
                try {
                    oStmt.close();
                } catch (Exception ignore) {
                }
            }
            throw new MessagingException(e.getMessage(), e);
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBFolder.saveMimeParts()");
        }
    }

    private static String getPartsBoundary(MimeMessage oMsg) throws MessagingException {
        String sBoundary = null;
        String sContentType = oMsg.getContentType();
        if (null != sContentType) {
            int iTypeLen = sContentType.length();
            int iBoundary = sContentType.indexOf("boundary");
            if (iBoundary > 0) {
                int iEq = sContentType.indexOf("=", iBoundary + 8);
                if (iEq > 0) {
                    iEq++;
                    while (iEq < iTypeLen) {
                        char cAt = sContentType.charAt(iEq);
                        if (cAt != ' ' && cAt != '"') break; else iEq++;
                    }
                    if (iEq < iTypeLen) {
                        int iEnd = iEq;
                        while (iEnd < iTypeLen) {
                            char cAt = sContentType.charAt(iEnd);
                            if (cAt != '"' && cAt != ';' && cAt != '\r' && cAt != '\n' && cAt != '\t') iEnd++; else break;
                        }
                        if (iEnd == iTypeLen) sBoundary = sContentType.substring(iEq); else sBoundary = sContentType.substring(iEq, iEnd);
                    }
                }
            }
        }
        return sBoundary;
    }

    private BigDecimal getNextMessage() throws SQLException {
        JDCConnection oConn = ((DBStore) getStore()).getConnection();
        PreparedStatement oStmt = oConn.prepareStatement("SELECT MAX(" + DB.pg_message + ") FROM " + DB.k_mime_msgs + " WHERE " + DB.gu_category + "=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        oStmt.setString(1, getCategory().getString(DB.gu_category));
        ResultSet oRSet = oStmt.executeQuery();
        oRSet.next();
        BigDecimal oNext = oRSet.getBigDecimal(1);
        if (oRSet.wasNull()) oNext = new BigDecimal(0); else oNext = oNext.add(new BigDecimal(1));
        oRSet.close();
        oStmt.close();
        return oNext;
    }

    public void appendMessage(MimeMessage oMsg) throws FolderClosedException, StoreClosedException, MessagingException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBFolder.appendMessage()");
            DebugFile.incIdent();
        }
        final String EmptyString = "";
        if (!((DBStore) getStore()).isConnected()) {
            if (DebugFile.trace) DebugFile.decIdent();
            throw new StoreClosedException(getStore(), "Store is not connected");
        }
        if (0 == (iOpenMode & READ_WRITE)) {
            if (DebugFile.trace) DebugFile.decIdent();
            throw new javax.mail.FolderClosedException(this, "Folder is not open is READ_WRITE mode");
        }
        if ((0 == (iOpenMode & MODE_MBOX)) && (0 == (iOpenMode & MODE_BLOB))) {
            if (DebugFile.trace) DebugFile.decIdent();
            throw new javax.mail.FolderClosedException(this, "Folder is not open in MBOX nor BLOB mode");
        }
        String gu_mimemsg;
        if (oMsg.getClass().getName().equals("com.knowgate.hipermail.DBMimeMessage")) {
            gu_mimemsg = ((DBMimeMessage) oMsg).getMessageGuid();
            if (((DBMimeMessage) oMsg).getFolder() == null) ((DBMimeMessage) oMsg).setFolder(this);
        } else {
            gu_mimemsg = Gadgets.generateUUID();
        }
        String gu_workarea = ((DBStore) getStore()).getUser().getString(DB.gu_workarea);
        int iSize = oMsg.getSize();
        if (DebugFile.trace) DebugFile.writeln("MimeMessage.getSize() = " + String.valueOf(iSize));
        String sContentType, sContentID, sMessageID, sDisposition, sContentMD5, sDescription, sFileName, sEncoding, sSubject, sPriority, sMsgCharSeq;
        long lPosition = -1;
        try {
            sMessageID = oMsg.getMessageID();
            if (sMessageID == null || EmptyString.equals(sMessageID)) {
                try {
                    sMessageID = oMsg.getHeader("X-Qmail-Scanner-Message-ID", null);
                } catch (Exception ignore) {
                }
            }
            if (sMessageID != null) sMessageID = MimeUtility.decodeText(sMessageID);
            sContentType = oMsg.getContentType();
            if (sContentType != null) sContentType = MimeUtility.decodeText(sContentType);
            sContentID = oMsg.getContentID();
            if (sContentID != null) sContentID = MimeUtility.decodeText(sContentID);
            sDisposition = oMsg.getDisposition();
            if (sDisposition != null) sDisposition = MimeUtility.decodeText(sDisposition);
            sContentMD5 = oMsg.getContentMD5();
            if (sContentMD5 != null) sContentMD5 = MimeUtility.decodeText(sContentMD5);
            sDescription = oMsg.getDescription();
            if (sDescription != null) sDescription = MimeUtility.decodeText(sDescription);
            sFileName = oMsg.getFileName();
            if (sFileName != null) sFileName = MimeUtility.decodeText(sFileName);
            sEncoding = oMsg.getEncoding();
            if (sEncoding != null) sEncoding = MimeUtility.decodeText(sEncoding);
            sSubject = oMsg.getSubject();
            if (sSubject != null) sSubject = MimeUtility.decodeText(sSubject);
            sPriority = null;
            sMsgCharSeq = null;
        } catch (UnsupportedEncodingException uee) {
            throw new MessagingException(uee.getMessage(), uee);
        }
        BigDecimal dPgMessage = null;
        try {
            dPgMessage = getNextMessage();
        } catch (SQLException sqle) {
            throw new MessagingException(sqle.getMessage(), sqle);
        }
        String sBoundary = getPartsBoundary(oMsg);
        if (DebugFile.trace) DebugFile.writeln("part boundary is \"" + (sBoundary == null ? "null" : sBoundary) + "\"");
        if (sMessageID == null) sMessageID = gu_mimemsg; else if (sMessageID.length() == 0) sMessageID = gu_mimemsg;
        Timestamp tsSent;
        if (oMsg.getSentDate() != null) tsSent = new Timestamp(oMsg.getSentDate().getTime()); else tsSent = null;
        Timestamp tsReceived;
        if (oMsg.getReceivedDate() != null) tsReceived = new Timestamp(oMsg.getReceivedDate().getTime()); else tsReceived = new Timestamp(new java.util.Date().getTime());
        try {
            String sXPriority = oMsg.getHeader("X-Priority", null);
            if (sXPriority == null) sPriority = null; else {
                sPriority = "";
                for (int x = 0; x < sXPriority.length(); x++) {
                    char cAt = sXPriority.charAt(x);
                    if (cAt >= (char) 48 || cAt <= (char) 57) sPriority += cAt;
                }
                sPriority = Gadgets.left(sPriority, 10);
            }
        } catch (MessagingException msge) {
            if (DebugFile.trace) DebugFile.writeln("MessagingException " + msge.getMessage());
        }
        boolean bIsSpam = false;
        try {
            String sXSpam = oMsg.getHeader("X-Spam-Flag", null);
            if (sXSpam != null) bIsSpam = (sXSpam.toUpperCase().indexOf("YES") >= 0 || sXSpam.toUpperCase().indexOf("TRUE") >= 0 || sXSpam.indexOf("1") >= 0);
        } catch (MessagingException msge) {
            if (DebugFile.trace) DebugFile.writeln("MessagingException " + msge.getMessage());
        }
        if (DebugFile.trace) DebugFile.writeln("MimeMessage.getFrom()");
        Address[] aFrom = null;
        try {
            aFrom = oMsg.getFrom();
        } catch (AddressException adre) {
            if (DebugFile.trace) DebugFile.writeln("From AddressException " + adre.getMessage());
        }
        InternetAddress oFrom;
        if (aFrom != null) {
            if (aFrom.length > 0) oFrom = (InternetAddress) aFrom[0]; else oFrom = null;
        } else oFrom = null;
        if (DebugFile.trace) DebugFile.writeln("MimeMessage.getReplyTo()");
        Address[] aReply = null;
        InternetAddress oReply;
        try {
            aReply = oMsg.getReplyTo();
        } catch (AddressException adre) {
            if (DebugFile.trace) DebugFile.writeln("Reply-To AddressException " + adre.getMessage());
        }
        if (aReply != null) {
            if (aReply.length > 0) oReply = (InternetAddress) aReply[0]; else oReply = null;
        } else {
            if (DebugFile.trace) DebugFile.writeln("no reply-to address found");
            oReply = null;
        }
        if (DebugFile.trace) DebugFile.writeln("MimeMessage.getRecipients()");
        Address[] oTo = null;
        Address[] oCC = null;
        Address[] oBCC = null;
        try {
            oTo = oMsg.getRecipients(MimeMessage.RecipientType.TO);
            oCC = oMsg.getRecipients(MimeMessage.RecipientType.CC);
            oBCC = oMsg.getRecipients(MimeMessage.RecipientType.BCC);
        } catch (AddressException adre) {
            if (DebugFile.trace) DebugFile.writeln("Recipient AddressException " + adre.getMessage());
        }
        Properties pFrom = new Properties(), pTo = new Properties(), pCC = new Properties(), pBCC = new Properties();
        if (DebugFile.trace) DebugFile.writeln("MimeMessage.getFlags()");
        Flags oFlgs = oMsg.getFlags();
        if (oFlgs == null) oFlgs = new Flags();
        MimePart oText = null;
        ByteArrayOutputStream byOutStrm = null;
        File oFile = null;
        MboxFile oMBox = null;
        if ((iOpenMode & MODE_MBOX) != 0) {
            try {
                if (DebugFile.trace) DebugFile.writeln("new File(" + Gadgets.chomp(sFolderDir, File.separator) + oCatg.getStringNull(DB.nm_category, "null") + ".mbox)");
                oFile = getFile();
                lPosition = oFile.length();
                if (DebugFile.trace) DebugFile.writeln("message position is " + String.valueOf(lPosition));
                oMBox = new MboxFile(oFile, MboxFile.READ_WRITE);
                if (DebugFile.trace) DebugFile.writeln("new ByteArrayOutputStream(" + String.valueOf(iSize > 0 ? iSize : 16000) + ")");
                byOutStrm = new ByteArrayOutputStream(iSize > 0 ? iSize : 16000);
                oMsg.writeTo(byOutStrm);
                sMsgCharSeq = byOutStrm.toString("ISO8859_1");
                byOutStrm.close();
            } catch (IOException ioe) {
                try {
                    if (oMBox != null) oMBox.close();
                } catch (Exception ignore) {
                }
                if (DebugFile.trace) DebugFile.decIdent();
                throw new MessagingException(ioe.getMessage(), ioe);
            }
        }
        try {
            if (oMsg.getClass().getName().equals("com.knowgate.hipermail.DBMimeMessage")) oText = ((DBMimeMessage) oMsg).getBody(); else {
                oText = new DBMimeMessage(oMsg).getBody();
            }
            if (DebugFile.trace) DebugFile.writeln("ByteArrayOutputStream byOutStrm = new ByteArrayOutputStream(" + oText.getSize() + ")");
            byOutStrm = new ByteArrayOutputStream(oText.getSize() > 0 ? oText.getSize() : 8192);
            oText.writeTo(byOutStrm);
            if (null == sContentMD5) {
                MD5 oMd5 = new MD5();
                oMd5.Init();
                oMd5.Update(byOutStrm.toByteArray());
                sContentMD5 = Gadgets.toHexString(oMd5.Final());
                oMd5 = null;
            }
        } catch (IOException ioe) {
            if (DebugFile.trace) DebugFile.decIdent();
            throw new MessagingException("IOException " + ioe.getMessage(), ioe);
        } catch (OutOfMemoryError oom) {
            if (DebugFile.trace) DebugFile.decIdent();
            throw new MessagingException("OutOfMemoryError " + oom.getMessage());
        }
        String sSQL = "INSERT INTO " + DB.k_mime_msgs + "(gu_mimemsg,gu_workarea,gu_category,id_type,id_content,id_message,id_disposition,len_mimemsg,tx_md5,de_mimemsg,file_name,tx_encoding,tx_subject,dt_sent,dt_received,tx_email_from,nm_from,tx_email_reply,nm_to,id_priority,bo_answered,bo_deleted,bo_draft,bo_flagged,bo_recent,bo_seen,bo_spam,pg_message,nu_position,by_content) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + ")");
        PreparedStatement oStmt = null;
        try {
            oStmt = oConn.prepareStatement(sSQL);
            oStmt.setString(1, gu_mimemsg);
            oStmt.setString(2, gu_workarea);
            if (oCatg.isNull(DB.gu_category)) oStmt.setNull(3, Types.CHAR); else oStmt.setString(3, oCatg.getString(DB.gu_category));
            oStmt.setString(4, Gadgets.left(sContentType, 254));
            oStmt.setString(5, Gadgets.left(sContentID, 254));
            oStmt.setString(6, Gadgets.left(sMessageID, 254));
            oStmt.setString(7, Gadgets.left(sDisposition, 100));
            if ((iOpenMode & MODE_MBOX) != 0) {
                iSize = sMsgCharSeq.length();
                oStmt.setInt(8, iSize);
            } else {
                if (iSize >= 0) oStmt.setInt(8, iSize); else oStmt.setNull(8, Types.INTEGER);
            }
            oStmt.setString(9, Gadgets.left(sContentMD5, 32));
            oStmt.setString(10, Gadgets.left(sDescription, 254));
            oStmt.setString(11, Gadgets.left(sFileName, 254));
            oStmt.setString(12, Gadgets.left(sEncoding, 16));
            oStmt.setString(13, Gadgets.left(sSubject, 254));
            oStmt.setTimestamp(14, tsSent);
            oStmt.setTimestamp(15, tsReceived);
            if (null == oFrom) {
                oStmt.setNull(16, Types.VARCHAR);
                oStmt.setNull(17, Types.VARCHAR);
            } else {
                oStmt.setString(16, Gadgets.left(oFrom.getAddress(), 254));
                oStmt.setString(17, Gadgets.left(oFrom.getPersonal(), 254));
            }
            if (null == oReply) oStmt.setNull(18, Types.VARCHAR); else oStmt.setString(18, Gadgets.left(oReply.getAddress(), 254));
            Address[] aRecipients;
            String sRecipientName;
            aRecipients = oMsg.getRecipients(MimeMessage.RecipientType.TO);
            if (null != aRecipients) if (aRecipients.length == 0) aRecipients = null;
            if (null != aRecipients) {
                sRecipientName = ((InternetAddress) aRecipients[0]).getPersonal();
                if (null == sRecipientName) sRecipientName = ((InternetAddress) aRecipients[0]).getAddress();
                oStmt.setString(19, Gadgets.left(sRecipientName, 254));
            } else {
                aRecipients = oMsg.getRecipients(MimeMessage.RecipientType.CC);
                if (null != aRecipients) {
                    if (aRecipients.length > 0) {
                        sRecipientName = ((InternetAddress) aRecipients[0]).getPersonal();
                        if (null == sRecipientName) sRecipientName = ((InternetAddress) aRecipients[0]).getAddress();
                        oStmt.setString(19, Gadgets.left(sRecipientName, 254));
                    } else oStmt.setNull(19, Types.VARCHAR);
                } else {
                    aRecipients = oMsg.getRecipients(MimeMessage.RecipientType.BCC);
                    if (null != aRecipients) {
                        if (aRecipients.length > 0) {
                            sRecipientName = ((InternetAddress) aRecipients[0]).getPersonal();
                            if (null == sRecipientName) sRecipientName = ((InternetAddress) aRecipients[0]).getAddress();
                            oStmt.setString(19, Gadgets.left(sRecipientName, 254));
                        } else oStmt.setNull(19, Types.VARCHAR);
                    } else {
                        oStmt.setNull(19, Types.VARCHAR);
                    }
                }
            }
            if (null == sPriority) oStmt.setNull(20, Types.VARCHAR); else oStmt.setString(20, sPriority);
            if (oConn.getDataBaseProduct() == JDCConnection.DBMS_ORACLE) {
                if (DebugFile.trace) DebugFile.writeln("PreparedStatement.setBigDecimal(21, ...)");
                oStmt.setBigDecimal(21, new BigDecimal(oFlgs.contains(Flags.Flag.ANSWERED) ? "1" : "0"));
                oStmt.setBigDecimal(22, new BigDecimal(oFlgs.contains(Flags.Flag.DELETED) ? "1" : "0"));
                oStmt.setBigDecimal(23, new BigDecimal(0));
                oStmt.setBigDecimal(24, new BigDecimal(oFlgs.contains(Flags.Flag.FLAGGED) ? "1" : "0"));
                oStmt.setBigDecimal(25, new BigDecimal(oFlgs.contains(Flags.Flag.RECENT) ? "1" : "0"));
                oStmt.setBigDecimal(26, new BigDecimal(oFlgs.contains(Flags.Flag.SEEN) ? "1" : "0"));
                oStmt.setBigDecimal(27, new BigDecimal(bIsSpam ? "1" : "0"));
                oStmt.setBigDecimal(28, dPgMessage);
                if ((iOpenMode & MODE_MBOX) != 0) oStmt.setBigDecimal(29, new BigDecimal(lPosition)); else oStmt.setNull(29, Types.NUMERIC);
                if (DebugFile.trace) DebugFile.writeln("PreparedStatement.setBinaryStream(30, new ByteArrayInputStream(" + String.valueOf(byOutStrm.size()) + "))");
                if (byOutStrm.size() > 0) oStmt.setBinaryStream(30, new ByteArrayInputStream(byOutStrm.toByteArray()), byOutStrm.size()); else oStmt.setNull(30, Types.LONGVARBINARY);
            } else {
                if (DebugFile.trace) DebugFile.writeln("PreparedStatement.setShort(21, ...)");
                oStmt.setShort(21, (short) (oFlgs.contains(Flags.Flag.ANSWERED) ? 1 : 0));
                oStmt.setShort(22, (short) (oFlgs.contains(Flags.Flag.DELETED) ? 1 : 0));
                oStmt.setShort(23, (short) (0));
                oStmt.setShort(24, (short) (oFlgs.contains(Flags.Flag.FLAGGED) ? 1 : 0));
                oStmt.setShort(25, (short) (oFlgs.contains(Flags.Flag.RECENT) ? 1 : 0));
                oStmt.setShort(26, (short) (oFlgs.contains(Flags.Flag.SEEN) ? 1 : 0));
                oStmt.setShort(27, (short) (bIsSpam ? 1 : 0));
                oStmt.setBigDecimal(28, dPgMessage);
                if ((iOpenMode & MODE_MBOX) != 0) oStmt.setBigDecimal(29, new BigDecimal(lPosition)); else oStmt.setNull(29, Types.NUMERIC);
                if (DebugFile.trace) DebugFile.writeln("PreparedStatement.setBinaryStream(30, new ByteArrayInputStream(" + String.valueOf(byOutStrm.size()) + "))");
                if (byOutStrm.size() > 0) oStmt.setBinaryStream(30, new ByteArrayInputStream(byOutStrm.toByteArray()), byOutStrm.size()); else oStmt.setNull(30, Types.LONGVARBINARY);
            }
            if (DebugFile.trace) DebugFile.writeln("Statement.executeUpdate()");
            oStmt.executeUpdate();
            oStmt.close();
            oStmt = null;
        } catch (SQLException sqle) {
            try {
                if (oMBox != null) oMBox.close();
            } catch (Exception ignore) {
            }
            try {
                if (null != oStmt) oStmt.close();
                oStmt = null;
            } catch (Exception ignore) {
            }
            try {
                if (null != oConn) oConn.rollback();
            } catch (Exception ignore) {
            }
            throw new MessagingException(DB.k_mime_msgs + " " + sqle.getMessage(), sqle);
        }
        if ((iOpenMode & MODE_BLOB) != 0) {
            try {
                byOutStrm.close();
            } catch (IOException ignore) {
            }
            byOutStrm = null;
        }
        try {
            Object oContent = oMsg.getContent();
            if (oContent instanceof MimeMultipart) {
                try {
                    saveMimeParts(oMsg, sMsgCharSeq, sBoundary, gu_mimemsg, sMessageID, dPgMessage.intValue(), 0);
                } catch (MessagingException msge) {
                    try {
                        if (oMBox != null) oMBox.close();
                    } catch (Exception ignore) {
                    }
                    try {
                        oConn.rollback();
                    } catch (Exception ignore) {
                    }
                    throw new MessagingException(msge.getMessage(), msge.getNextException());
                }
            }
        } catch (Exception xcpt) {
            try {
                if (oMBox != null) oMBox.close();
            } catch (Exception ignore) {
            }
            try {
                oConn.rollback();
            } catch (Exception ignore) {
            }
            throw new MessagingException("MimeMessage.getContent() " + xcpt.getMessage(), xcpt);
        }
        sSQL = "SELECT " + DB.gu_contact + "," + DB.gu_company + "," + DB.tx_name + "," + DB.tx_surname + "," + DB.tx_surname + " FROM " + DB.k_member_address + " WHERE " + DB.tx_email + "=? AND " + DB.gu_workarea + "=? UNION SELECT " + DB.gu_user + ",'****************************USER'," + DB.nm_user + "," + DB.tx_surname1 + "," + DB.tx_surname2 + " FROM " + DB.k_users + " WHERE (" + DB.tx_main_email + "=? OR " + DB.tx_alt_email + "=?) AND " + DB.gu_workarea + "=?";
        if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + ")");
        PreparedStatement oAddr = null;
        try {
            oAddr = oConn.prepareStatement(sSQL, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ResultSet oRSet;
            InternetAddress oInetAdr;
            String sTxEmail, sGuCompany, sGuContact, sGuUser, sTxName, sTxSurname1, sTxSurname2, sTxPersonal;
            if (oFrom != null) {
                oAddr.setString(1, oFrom.getAddress());
                oAddr.setString(2, gu_workarea);
                oAddr.setString(3, oFrom.getAddress());
                oAddr.setString(4, oFrom.getAddress());
                oAddr.setString(5, gu_workarea);
                oRSet = oAddr.executeQuery();
                if (oRSet.next()) {
                    sGuContact = oRSet.getString(1);
                    if (oRSet.wasNull()) sGuContact = "null";
                    sGuCompany = oRSet.getString(2);
                    if (oRSet.wasNull()) sGuCompany = "null";
                    if (sGuCompany.equals("****************************USER")) {
                        sTxName = oRSet.getString(3);
                        if (oRSet.wasNull()) sTxName = "";
                        sTxSurname1 = oRSet.getString(4);
                        if (oRSet.wasNull()) sTxSurname1 = "";
                        sTxSurname2 = oRSet.getString(4);
                        if (oRSet.wasNull()) sTxSurname2 = "";
                        sTxPersonal = Gadgets.left(sTxName + " " + sTxSurname1 + " " + sTxSurname2, 254).replace(',', ' ').trim();
                    } else sTxPersonal = "null";
                    pFrom.put(oFrom.getAddress(), sGuContact + "," + sGuCompany + "," + sTxPersonal);
                } else pFrom.put(oFrom.getAddress(), "null,null,null");
                oRSet.close();
            }
            if (DebugFile.trace) DebugFile.writeln("from count = " + pFrom.size());
            if (oTo != null) {
                for (int t = 0; t < oTo.length; t++) {
                    oInetAdr = (InternetAddress) oTo[t];
                    sTxEmail = Gadgets.left(oInetAdr.getAddress(), 254);
                    oAddr.setString(1, sTxEmail);
                    oAddr.setString(2, gu_workarea);
                    oAddr.setString(3, sTxEmail);
                    oAddr.setString(4, sTxEmail);
                    oAddr.setString(5, gu_workarea);
                    oRSet = oAddr.executeQuery();
                    if (oRSet.next()) {
                        sGuContact = oRSet.getString(1);
                        if (oRSet.wasNull()) sGuContact = "null";
                        sGuCompany = oRSet.getString(2);
                        if (oRSet.wasNull()) sGuCompany = "null";
                        if (sGuCompany.equals("****************************USER")) {
                            sTxName = oRSet.getString(3);
                            if (oRSet.wasNull()) sTxName = "";
                            sTxSurname1 = oRSet.getString(4);
                            if (oRSet.wasNull()) sTxSurname1 = "";
                            sTxSurname2 = oRSet.getString(4);
                            if (oRSet.wasNull()) sTxSurname2 = "";
                            sTxPersonal = Gadgets.left(sTxName + " " + sTxSurname1 + " " + sTxSurname2, 254).replace(',', ' ').trim();
                        } else sTxPersonal = "null";
                        pTo.put(sTxEmail, sGuContact + "," + sGuCompany + "," + sTxPersonal);
                    } else pTo.put(sTxEmail, "null,null,null");
                    oRSet.close();
                }
            }
            if (DebugFile.trace) DebugFile.writeln("to count = " + pTo.size());
            if (oCC != null) {
                for (int c = 0; c < oCC.length; c++) {
                    oInetAdr = (InternetAddress) oCC[c];
                    sTxEmail = Gadgets.left(oInetAdr.getAddress(), 254);
                    oAddr.setString(1, sTxEmail);
                    oAddr.setString(2, gu_workarea);
                    oAddr.setString(3, sTxEmail);
                    oAddr.setString(4, sTxEmail);
                    oAddr.setString(5, gu_workarea);
                    oRSet = oAddr.executeQuery();
                    if (oRSet.next()) {
                        sGuContact = oRSet.getString(1);
                        if (oRSet.wasNull()) sGuContact = "null";
                        sGuCompany = oRSet.getString(2);
                        if (oRSet.wasNull()) sGuCompany = "null";
                        if (sGuCompany.equals("****************************USER")) {
                            sTxName = oRSet.getString(3);
                            if (oRSet.wasNull()) sTxName = "";
                            sTxSurname1 = oRSet.getString(4);
                            if (oRSet.wasNull()) sTxSurname1 = "";
                            sTxSurname2 = oRSet.getString(4);
                            if (oRSet.wasNull()) sTxSurname2 = "";
                            sTxPersonal = Gadgets.left(sTxName + " " + sTxSurname1 + " " + sTxSurname2, 254).replace(',', ' ').trim();
                        } else sTxPersonal = "null";
                        pCC.put(sTxEmail, sGuContact + "," + sGuCompany + "," + sTxPersonal);
                    } else pCC.put(sTxEmail, "null,null,null");
                    oRSet.close();
                }
            }
            if (DebugFile.trace) DebugFile.writeln("cc count = " + pCC.size());
            if (oBCC != null) {
                for (int b = 0; b < oBCC.length; b++) {
                    oInetAdr = (InternetAddress) oBCC[b];
                    sTxEmail = Gadgets.left(oInetAdr.getAddress(), 254);
                    oAddr.setString(1, sTxEmail);
                    oAddr.setString(2, gu_workarea);
                    oAddr.setString(3, sTxEmail);
                    oAddr.setString(4, sTxEmail);
                    oAddr.setString(5, gu_workarea);
                    oRSet = oAddr.executeQuery();
                    if (oRSet.next()) {
                        sGuContact = oRSet.getString(1);
                        if (oRSet.wasNull()) sGuContact = "null";
                        sGuCompany = oRSet.getString(2);
                        if (oRSet.wasNull()) sGuCompany = "null";
                        if (sGuCompany.equals("****************************USER")) {
                            sTxName = oRSet.getString(3);
                            if (oRSet.wasNull()) sTxName = "";
                            sTxSurname1 = oRSet.getString(4);
                            if (oRSet.wasNull()) sTxSurname1 = "";
                            sTxSurname2 = oRSet.getString(4);
                            if (oRSet.wasNull()) sTxSurname2 = "";
                            sTxPersonal = Gadgets.left(sTxName + " " + sTxSurname1 + " " + sTxSurname2, 254).replace(',', ' ').trim();
                        } else sTxPersonal = "null";
                        pBCC.put(sTxEmail, sGuContact + "," + sGuCompany);
                    } else pBCC.put(sTxEmail, "null,null,null");
                    oRSet.close();
                }
            }
            if (DebugFile.trace) DebugFile.writeln("bcc count = " + pBCC.size());
            oAddr.close();
            sSQL = "INSERT INTO " + DB.k_inet_addrs + " (gu_mimemsg,id_message,tx_email,tp_recipient,gu_user,gu_contact,gu_company,tx_personal) VALUES ('" + gu_mimemsg + "','" + sMessageID + "',?,?,?,?,?,?)";
            if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + ")");
            oStmt = oConn.prepareStatement(sSQL);
            java.util.Enumeration oMailEnum;
            String[] aRecipient;
            if (!pFrom.isEmpty()) {
                oMailEnum = pFrom.keys();
                while (oMailEnum.hasMoreElements()) {
                    sTxEmail = (String) oMailEnum.nextElement();
                    aRecipient = Gadgets.split(pFrom.getProperty(sTxEmail), ',');
                    oStmt.setString(1, sTxEmail);
                    oStmt.setString(2, "from");
                    if (aRecipient[0].equals("null") && aRecipient[1].equals("null")) {
                        oStmt.setNull(3, Types.CHAR);
                        oStmt.setNull(4, Types.CHAR);
                        oStmt.setNull(5, Types.CHAR);
                    } else if (aRecipient[1].equals("****************************USER")) {
                        oStmt.setString(3, aRecipient[0]);
                        oStmt.setNull(4, Types.CHAR);
                        oStmt.setNull(5, Types.CHAR);
                    } else {
                        oStmt.setNull(3, Types.CHAR);
                        oStmt.setString(4, aRecipient[0].equals("null") ? null : aRecipient[0]);
                        oStmt.setString(5, aRecipient[1].equals("null") ? null : aRecipient[1]);
                    }
                    if (aRecipient[2].equals("null")) oStmt.setNull(6, Types.VARCHAR); else oStmt.setString(6, aRecipient[2]);
                    if (DebugFile.trace) DebugFile.writeln("Statement.executeUpdate()");
                    oStmt.executeUpdate();
                }
            }
            if (!pTo.isEmpty()) {
                oMailEnum = pTo.keys();
                while (oMailEnum.hasMoreElements()) {
                    sTxEmail = (String) oMailEnum.nextElement();
                    aRecipient = Gadgets.split(pTo.getProperty(sTxEmail), ',');
                    oStmt.setString(1, sTxEmail);
                    oStmt.setString(2, "to");
                    if (aRecipient[0].equals("null") && aRecipient[1].equals("null")) {
                        oStmt.setNull(3, Types.CHAR);
                        oStmt.setNull(4, Types.CHAR);
                        oStmt.setNull(5, Types.CHAR);
                    } else if (aRecipient[1].equals("****************************USER")) {
                        oStmt.setString(3, aRecipient[0]);
                        oStmt.setNull(4, Types.CHAR);
                        oStmt.setNull(5, Types.CHAR);
                    } else {
                        oStmt.setNull(3, Types.CHAR);
                        oStmt.setString(4, aRecipient[0].equals("null") ? null : aRecipient[0]);
                        oStmt.setString(5, aRecipient[1].equals("null") ? null : aRecipient[1]);
                    }
                    if (aRecipient[2].equals("null")) oStmt.setNull(6, Types.VARCHAR); else oStmt.setString(6, aRecipient[2]);
                    if (DebugFile.trace) DebugFile.writeln("Statement.executeUpdate()");
                    oStmt.executeUpdate();
                }
            }
            if (!pCC.isEmpty()) {
                oMailEnum = pCC.keys();
                while (oMailEnum.hasMoreElements()) {
                    sTxEmail = (String) oMailEnum.nextElement();
                    aRecipient = Gadgets.split(pCC.getProperty(sTxEmail), ',');
                    oStmt.setString(1, sTxEmail);
                    oStmt.setString(2, "cc");
                    if (aRecipient[0].equals("null") && aRecipient[1].equals("null")) {
                        oStmt.setNull(3, Types.CHAR);
                        oStmt.setNull(4, Types.CHAR);
                        oStmt.setNull(5, Types.CHAR);
                    } else if (aRecipient[1].equals("****************************USER")) {
                        oStmt.setString(3, aRecipient[0]);
                        oStmt.setString(4, null);
                        oStmt.setString(5, null);
                    } else {
                        oStmt.setString(3, null);
                        oStmt.setString(4, aRecipient[0].equals("null") ? null : aRecipient[0]);
                        oStmt.setString(5, aRecipient[1].equals("null") ? null : aRecipient[1]);
                    }
                    if (aRecipient[2].equals("null")) oStmt.setNull(6, Types.VARCHAR); else oStmt.setString(6, aRecipient[2]);
                    if (DebugFile.trace) DebugFile.writeln("Statement.executeUpdate()");
                    oStmt.executeUpdate();
                }
            }
            if (!pBCC.isEmpty()) {
                oMailEnum = pBCC.keys();
                while (oMailEnum.hasMoreElements()) {
                    sTxEmail = (String) oMailEnum.nextElement();
                    aRecipient = Gadgets.split(pBCC.getProperty(sTxEmail), ',');
                    oStmt.setString(1, sTxEmail);
                    oStmt.setString(2, "bcc");
                    if (aRecipient[0].equals("null") && aRecipient[1].equals("null")) {
                        oStmt.setNull(3, Types.CHAR);
                        oStmt.setNull(4, Types.CHAR);
                        oStmt.setNull(5, Types.CHAR);
                    } else if (aRecipient[1].equals("****************************USER")) {
                        oStmt.setString(3, aRecipient[0]);
                        oStmt.setNull(4, Types.CHAR);
                        oStmt.setNull(5, Types.CHAR);
                    } else {
                        oStmt.setNull(3, Types.CHAR);
                        oStmt.setString(4, aRecipient[0].equals("null") ? null : aRecipient[0]);
                        oStmt.setString(5, aRecipient[1].equals("null") ? null : aRecipient[1]);
                    }
                    if (aRecipient[2].equals("null")) oStmt.setNull(6, Types.VARCHAR); else oStmt.setString(6, aRecipient[2]);
                    oStmt.executeUpdate();
                }
            }
            oStmt.close();
            oStmt = null;
            oStmt = oConn.prepareStatement("UPDATE " + DB.k_categories + " SET " + DB.len_size + "=" + DB.len_size + "+" + String.valueOf(iSize) + " WHERE " + DB.gu_category + "=?");
            oStmt.setString(1, getCategory().getString(DB.gu_category));
            oStmt.executeUpdate();
            oStmt.close();
            oStmt = null;
            if ((iOpenMode & MODE_MBOX) != 0) {
                if (DebugFile.trace) DebugFile.writeln("MboxFile.appendMessage(" + (oMsg.getContentID() != null ? oMsg.getContentID() : "") + ")");
                oMBox.appendMessage(sMsgCharSeq);
                oMBox.close();
                oMBox = null;
            }
            if (DebugFile.trace) DebugFile.writeln("Connection.commit()");
            oConn.commit();
        } catch (SQLException sqle) {
            try {
                if (oMBox != null) oMBox.close();
            } catch (Exception ignore) {
            }
            try {
                if (null != oStmt) oStmt.close();
                oStmt = null;
            } catch (Exception ignore) {
            }
            try {
                if (null != oAddr) oAddr.close();
                oAddr = null;
            } catch (Exception ignore) {
            }
            try {
                if (null != oConn) oConn.rollback();
            } catch (Exception ignore) {
            }
            throw new MessagingException(sqle.getMessage(), sqle);
        } catch (IOException ioe) {
            try {
                if (oMBox != null) oMBox.close();
            } catch (Exception ignore) {
            }
            try {
                if (null != oStmt) oStmt.close();
                oStmt = null;
            } catch (Exception ignore) {
            }
            try {
                if (null != oAddr) oAddr.close();
                oAddr = null;
            } catch (Exception ignore) {
            }
            try {
                if (null != oConn) oConn.rollback();
            } catch (Exception ignore) {
            }
            throw new MessagingException(ioe.getMessage(), ioe);
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBFolder.appendMessage() : " + gu_mimemsg);
        }
    }

    public Message getMessage(int msgnum) throws MessagingException {
        return getMessage(String.valueOf(msgnum), 3);
    }

    public DBMimeMessage getMessageByGuid(String sMsgGuid) throws MessagingException {
        return (DBMimeMessage) getMessage(sMsgGuid, 1);
    }

    public DBMimeMessage getMessageByID(String sMsgGuid) throws MessagingException {
        return (DBMimeMessage) getMessage(sMsgGuid, 2);
    }

    public int getMessageCount() throws FolderClosedException, MessagingException {
        PreparedStatement oStmt = null;
        ResultSet oRSet = null;
        Object oCount;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBFolder.getMessageCount()");
            DebugFile.incIdent();
        }
        if (!isOpen()) {
            if (DebugFile.trace) DebugFile.decIdent();
            throw new javax.mail.FolderClosedException(this, "Folder is closed");
        }
        try {
            oStmt = getConnection().prepareStatement("SELECT COUNT(*) FROM " + DB.k_mime_msgs + " WHERE " + DB.gu_category + "=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            oStmt.setString(1, oCatg.getStringNull(DB.gu_category, null));
            oRSet = oStmt.executeQuery();
            oRSet.next();
            oCount = oRSet.getObject(1);
            oRSet.close();
            oRSet = null;
            oStmt.close();
            oStmt = null;
        } catch (SQLException sqle) {
            oCount = new Integer(0);
            try {
                if (null != oRSet) oRSet.close();
            } catch (Exception ignore) {
            }
            try {
                if (null != oStmt) oStmt.close();
            } catch (Exception ignore) {
            }
            throw new MessagingException(sqle.getMessage(), sqle);
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBFolder.getMessageCount() : " + oCount.toString());
        }
        return Integer.parseInt(oCount.toString());
    }

    public Folder getParent() throws MessagingException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBFolder.getParent()");
            DebugFile.incIdent();
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBFolder.getParent() : null");
        }
        return null;
    }

    public Flags getPermanentFlags() {
        Flags oFlgs = new Flags();
        oFlgs.add(Flags.Flag.DELETED);
        oFlgs.add(Flags.Flag.ANSWERED);
        oFlgs.add(Flags.Flag.DRAFT);
        oFlgs.add(Flags.Flag.SEEN);
        oFlgs.add(Flags.Flag.RECENT);
        oFlgs.add(Flags.Flag.FLAGGED);
        return oFlgs;
    }

    public char getSeparator() throws MessagingException {
        return '/';
    }

    public Folder[] list(String pattern) throws MessagingException {
        return null;
    }

    public int getType() throws MessagingException {
        return iOpenMode;
    }

    public boolean isOpen() {
        return (iOpenMode != 0);
    }

    public Properties getMessageHeaders(String sMsgId) throws FolderClosedException, SQLException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBFolder.getMessageHeaders()");
            DebugFile.incIdent();
        }
        if (!isOpen()) {
            if (DebugFile.trace) DebugFile.decIdent();
            throw new javax.mail.FolderClosedException(this, "Folder is closed");
        }
        Properties oRetVal;
        PreparedStatement oStmt;
        if (sMsgId.length() == 32) {
            if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(SELECT " + DB.gu_mimemsg + "," + DB.id_message + "," + DB.pg_message + "," + DB.tx_subject + " FROM " + DB.k_mime_msgs + " WHERE " + DB.gu_mimemsg + "='" + sMsgId + "' OR " + DB.id_message + "='" + sMsgId + "') AND " + DB.bo_deleted + "<>1)");
            oStmt = getConnection().prepareStatement("SELECT " + DB.gu_mimemsg + "," + DB.id_message + "," + DB.pg_message + "," + DB.tx_subject + " FROM " + DB.k_mime_msgs + " WHERE (" + DB.gu_mimemsg + "=? OR " + DB.id_message + "=?) AND " + DB.bo_deleted + "<>1", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            oStmt.setString(1, sMsgId);
            oStmt.setString(2, sMsgId);
        } else {
            if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(SELECT " + DB.gu_mimemsg + "," + DB.id_message + "," + DB.pg_message + "," + DB.tx_subject + " FROM " + DB.k_mime_msgs + " WHERE " + DB.id_message + "='" + sMsgId + "')");
            oStmt = getConnection().prepareStatement("SELECT " + DB.gu_mimemsg + "," + DB.id_message + "," + DB.pg_message + "," + DB.tx_subject + " FROM " + DB.k_mime_msgs + " WHERE " + DB.id_message + "=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            oStmt.setString(1, sMsgId);
        }
        ResultSet oRSet = oStmt.executeQuery();
        if (oRSet.next()) {
            oRetVal = new Properties();
            String s;
            BigDecimal d;
            oRetVal.put(DB.gu_mimemsg, oRSet.getString(1));
            s = oRSet.getString(2);
            if (!oRSet.wasNull()) oRetVal.put(DB.id_message, s);
            d = oRSet.getBigDecimal(3);
            if (!oRSet.wasNull()) oRetVal.put(DB.pg_message, d.toString());
            s = oRSet.getString(4);
            if (!oRSet.wasNull()) oRetVal.put(DB.tx_subject, s);
        } else oRetVal = null;
        oRSet.close();
        oStmt.close();
        if (DebugFile.trace) {
            DebugFile.decIdent();
            if (oRetVal == null) DebugFile.writeln("End DBFolder.getMessageHeaders() : null"); else DebugFile.writeln("End DBFolder.getMessageHeaders() : Properties[" + String.valueOf(oRetVal.size()) + "]");
        }
        return oRetVal;
    }

    public int importMbox(String sMboxFilePath) throws FileNotFoundException, IOException, MessagingException {
        MimeMessage oMsg;
        InputStream oMsgStrm;
        Session oSession = ((DBStore) getStore()).getSession();
        MboxFile oInputMbox = new MboxFile(sMboxFilePath, MboxFile.READ_ONLY);
        final int iMsgCount = oInputMbox.getMessageCount();
        for (int m = 0; m < iMsgCount; m++) {
            oMsgStrm = oInputMbox.getMessageAsStream(m);
            oMsg = new MimeMessage(oSession, oMsgStrm);
            appendMessage(oMsg);
            oMsgStrm.close();
        }
        oInputMbox.close();
        return iMsgCount;
    }

    public static final short ClassId = 800;
}
