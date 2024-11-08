package com.knowgate.hipermail;

import com.knowgate.debug.DebugFile;
import com.knowgate.dataobjs.DB;
import com.knowgate.dataobjs.DBBind;
import com.knowgate.dataobjs.DBSubset;
import com.knowgate.jdc.JDCConnection;
import com.knowgate.misc.Gadgets;
import com.knowgate.misc.MD5;
import com.knowgate.dfs.StreamPipe;
import java.io.File;
import java.io.StringBufferInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.HashMap;
import java.math.BigDecimal;
import javax.mail.BodyPart;
import javax.mail.Address;
import javax.mail.Part;
import javax.mail.Message;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.ParseException;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeMultipart;
import javax.mail.MessagingException;
import javax.mail.FolderClosedException;
import org.htmlparser.beans.StringBean;

/**
 * MIME messages stored at database BLOB columns or MBOX files
 * @author Sergio Montoro Ten
 * @version 1.0
 */
public class DBMimeMessage extends MimeMessage implements MimePart, Part {

    private String sGuid;

    private Folder oFolder;

    private Address[] aAddrs;

    private HashMap oHeaders;

    /**
   * Create an empty message
   * @param oMailSession
   */
    public DBMimeMessage(Session oMailSession) {
        super(oMailSession);
        sGuid = null;
        oFolder = null;
        oHeaders = null;
    }

    public DBMimeMessage(MimeMessage oMsg) throws MessagingException {
        super(oMsg);
        sGuid = Gadgets.generateUUID();
        oHeaders = null;
    }

    public DBMimeMessage(Session oMailSession, InputStream oInStrm) throws MessagingException {
        super(oMailSession, oInStrm);
        sGuid = Gadgets.generateUUID();
        oHeaders = null;
    }

    public DBMimeMessage(Folder oFldr, InputStream oInStrm) throws MessagingException, ClassCastException {
        super(((DBStore) oFldr.getStore()).getSession(), oInStrm);
        setFolder(oFldr);
        sGuid = Gadgets.generateUUID();
        oHeaders = null;
    }

    public DBMimeMessage(Folder oFldr, MimeMessage oMsg) throws MessagingException {
        super(oMsg);
        setFolder(oFldr);
        sGuid = Gadgets.generateUUID();
        oHeaders = null;
    }

    public DBMimeMessage(Folder oFldr, DBMimeMessage oMsg) throws MessagingException {
        super(oMsg);
        setFolder(oFldr);
        sGuid = oMsg.getMessageGuid();
        oHeaders = null;
    }

    public DBMimeMessage(Folder oFldr, String sMsgGuid) throws MessagingException {
        super(((DBStore) oFldr.getStore()).getSession());
        sGuid = sMsgGuid;
        setFolder(oFldr);
        oHeaders = null;
    }

    public Folder getFolder() {
        if (oFolder == null) return super.getFolder(); else return oFolder;
    }

    public void setFolder(Folder oFldr) {
        oFolder = oFldr;
    }

    public String getMessageGuid() {
        if (null == sGuid) sGuid = Gadgets.generateUUID();
        return sGuid;
    }

    public void setMessageGuid(String sId) {
        sGuid = sId;
    }

    public Flags getFlags() throws MessagingException {
        Object oFlag;
        if (oFolder == null) return super.getFlags(); else {
            Flags oRetVal = null;
            Statement oStmt = null;
            ResultSet oRSet = null;
            try {
                Flags.Flag[] aFlags = new Flags.Flag[] { Flags.Flag.RECENT, Flags.Flag.ANSWERED, Flags.Flag.DELETED, Flags.Flag.DRAFT, Flags.Flag.FLAGGED, Flags.Flag.SEEN };
                oStmt = ((DBFolder) oFolder).getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                oRSet = oStmt.executeQuery("SELECT " + DB.bo_recent + "," + DB.bo_answered + "," + DB.bo_deleted + "," + DB.bo_draft + "," + DB.bo_flagged + "," + DB.bo_recent + "," + DB.bo_seen + " FROM " + DB.k_mime_msgs + " WHERE " + DB.gu_mimemsg + "='" + getMessageGuid() + "'");
                if (oRSet.next()) {
                    oRetVal = new Flags();
                    for (int f = 1; f <= 6; f++) {
                        oFlag = oRSet.getObject(f);
                        if (!oRSet.wasNull()) {
                            if (oFlag.getClass().equals(Short.TYPE)) {
                                if (((Short) oFlag).shortValue() == (short) 1) oRetVal.add(aFlags[f - 1]);
                            } else {
                                if (Integer.parseInt(oFlag.toString()) != 0) oRetVal.add(aFlags[f - 1]);
                            }
                        }
                    }
                }
                oRSet.close();
                oRSet = null;
                oStmt.close();
                oStmt = null;
                return oRetVal;
            } catch (SQLException sqle) {
                if (oStmt != null) {
                    try {
                        oStmt.close();
                    } catch (Exception ignore) {
                    }
                }
                if (oRSet != null) {
                    try {
                        oRSet.close();
                    } catch (Exception ignore) {
                    }
                }
            }
        }
        return null;
    }

    /**
   * <p>Get message recipients</p>
   * This method read recipients from a message stored at k_inet_addrs table
   * or if message is not already stored at k_inet_addrs then it delegates
   * behaviour to parent class MimMessage..getAllRecipients()
   * @return If this message is stored at the database then this method returns
   * an array of DBInetAddr objects. If this message has not been stored yet then
   * this method returns an array of javax.mail.internet.InternetAddress objects
   * @throws MessagingException
   * @throws NullPointerException
   * @throws IllegalArgumentException
   */
    public Address[] getAllRecipients() throws MessagingException, NullPointerException, IllegalArgumentException {
        DBSubset oAddrs;
        int iAddrs;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBMimeMessage.getAllRecipients()");
            DebugFile.incIdent();
        }
        if (oFolder == null) {
            if (DebugFile.trace) {
                DebugFile.writeln("Message is not stored at any Folder or Folder is closed");
                DebugFile.decIdent();
            }
            return super.getAllRecipients();
        } else {
            if (oFolder.getClass().getName().equals("com.knowgate.hipermail.DBFolder")) {
                if (((DBFolder) oFolder).getConnection() == null) {
                    if (DebugFile.trace) DebugFile.decIdent();
                    throw new MessagingException("DBMimeMessage.getAllRecipients() not connected to the database");
                }
                oAddrs = new DBSubset(DB.k_inet_addrs, DB.gu_mimemsg + "," + DB.id_message + "," + DB.tx_email + "," + DB.tx_personal + "," + DB.tp_recipient + "," + DB.gu_user + "," + DB.gu_contact + "," + DB.gu_company, DB.gu_mimemsg + "=?", 10);
                try {
                    iAddrs = oAddrs.load(((DBFolder) oFolder).getConnection(), new Object[] { sGuid });
                } catch (SQLException sqle) {
                    if (DebugFile.trace) DebugFile.decIdent();
                    throw new MessagingException(sqle.getMessage(), sqle);
                }
                if (iAddrs > 0) {
                    aAddrs = new DBInetAddr[iAddrs];
                    for (int a = 0; a < iAddrs; a++) {
                        aAddrs[a] = new DBInetAddr(oAddrs.getString(0, a), oAddrs.getString(1, a), oAddrs.getString(2, a), oAddrs.getStringNull(3, a, null), oAddrs.getString(4, a), oAddrs.getStringNull(5, a, null), oAddrs.getStringNull(6, a, null), oAddrs.getStringNull(7, a, null));
                    }
                } else {
                    aAddrs = null;
                }
            } else {
                DebugFile.writeln("message Folder type is " + oFolder.getClass().getName());
                if (DebugFile.trace) DebugFile.decIdent();
                aAddrs = super.getAllRecipients();
            }
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBMimeMessage.getAllRecipients()");
        }
        return aAddrs;
    }

    public Address[] getRecipients(Message.RecipientType cTpRecipient) throws MessagingException {
        int a;
        int iRecipients = 0;
        String sType;
        DBInetAddr[] aRecipients = null;
        DBInetAddr oAdr;
        if (oFolder == null) {
            return super.getRecipients(cTpRecipient);
        }
        if (aAddrs == null) getAllRecipients();
        if (aAddrs != null) {
            for (a = 0; a < aAddrs.length; a++) {
                oAdr = ((DBInetAddr) aAddrs[a]);
                if ((oAdr.getStringNull(DB.tp_recipient, "").equalsIgnoreCase("to") && Message.RecipientType.TO.equals(cTpRecipient)) || (oAdr.getStringNull(DB.tp_recipient, "").equalsIgnoreCase("cc") && Message.RecipientType.CC.equals(cTpRecipient)) || (oAdr.getStringNull(DB.tp_recipient, "").equalsIgnoreCase("bcc") && Message.RecipientType.BCC.equals(cTpRecipient))) iRecipients++;
            }
            aRecipients = new DBInetAddr[iRecipients];
            int iRecipient = 0;
            for (a = 0; a < aAddrs.length; a++) {
                oAdr = ((DBInetAddr) aAddrs[a]);
                if ((oAdr.getStringNull(DB.tp_recipient, "").equalsIgnoreCase("to") && Message.RecipientType.TO.equals(cTpRecipient)) || (oAdr.getStringNull(DB.tp_recipient, "").equalsIgnoreCase("cc") && Message.RecipientType.CC.equals(cTpRecipient)) || (oAdr.getStringNull(DB.tp_recipient, "").equalsIgnoreCase("bcc") && Message.RecipientType.BCC.equals(cTpRecipient))) aRecipients[iRecipient++] = (DBInetAddr) aAddrs[a];
            }
        }
        return aRecipients;
    }

    public Address getFromRecipient() throws MessagingException {
        DBInetAddr oFrom = null;
        if (aAddrs == null) getAllRecipients();
        if (aAddrs != null) {
            for (int a = 0; a < aAddrs.length && oFrom == null; a++) {
                if (((DBInetAddr) (aAddrs[a])).getStringNull(DB.tp_recipient, "").equals("from")) oFrom = (DBInetAddr) (aAddrs[a]);
            }
        }
        return oFrom;
    }

    public void cacheHeaders() throws SQLException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBMimeMessage.cacheHeaders()");
            DebugFile.incIdent();
        }
        PreparedStatement oStmt = null;
        ResultSet oRSet = null;
        oStmt = ((DBFolder) oFolder).getConnection().prepareStatement("SELECT " + DB.id_type + "," + DB.tx_subject + "," + DB.id_message + "," + DB.len_mimemsg + "," + DB.tx_md5 + "," + DB.de_mimemsg + "," + DB.tx_encoding + "," + DB.dt_sent + "," + DB.dt_received + "," + DB.dt_readed + "," + DB.bo_spam + "," + DB.id_compression + "," + DB.id_priority + " FROM " + DB.k_mime_msgs + " WHERE " + DB.gu_mimemsg + "=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        oStmt.setString(1, getMessageGuid());
        oRSet = oStmt.executeQuery();
        if (oRSet.next()) {
            oHeaders = new HashMap(23);
            oHeaders.put("Content-Type", oRSet.getString(1));
            oHeaders.put("Subject", oRSet.getString(2));
            oHeaders.put("Message-ID", oRSet.getString(3));
            oHeaders.put("Date", oRSet.getDate(8));
            oHeaders.put("Date-Received", oRSet.getDate(9));
            oHeaders.put("Date-Readed", oRSet.getDate(10));
            oHeaders.put("X-Spam-Flag", oRSet.getObject(11));
            oHeaders.put("Compression", oRSet.getString(12));
            oHeaders.put("X-Priority", oRSet.getString(13));
        }
        oRSet.close();
        oStmt.close();
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBMimeMessage.cacheHeaders()");
        }
    }

    public String getContentType() throws MessagingException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBMimeMessage.getContentType()");
            DebugFile.incIdent();
        }
        String sRetVal;
        if (oFolder == null) {
            if (DebugFile.trace) {
                DebugFile.writeln("Message is not stored at any Folder or Folder is closed");
            }
            sRetVal = super.getContentType();
        } else {
            try {
                if (null == oHeaders) cacheHeaders();
                if (null == oHeaders) sRetVal = super.getContentType(); else sRetVal = (String) oHeaders.get("Content-Type");
            } catch (SQLException sqle) {
                throw new MessagingException(sqle.getMessage(), sqle);
            }
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBMimeMessage.getContentType() : " + sRetVal);
        }
        return sRetVal;
    }

    public Date getSentDate() throws MessagingException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBMimeMessage.getSentDate()");
            DebugFile.incIdent();
        }
        Date dtRetVal;
        if (oFolder == null) {
            if (DebugFile.trace) {
                DebugFile.writeln("Message is not stored at any Folder or Folder is closed");
            }
            dtRetVal = super.getSentDate();
        } else {
            try {
                if (null == oHeaders) cacheHeaders();
                if (null == oHeaders) dtRetVal = super.getSentDate(); else dtRetVal = (java.util.Date) oHeaders.get("Date");
            } catch (SQLException sqle) {
                throw new MessagingException(sqle.getMessage(), sqle);
            }
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            if (dtRetVal == null) DebugFile.writeln("End DBMimeMessage.getSentDate() : null"); else DebugFile.writeln("End DBMimeMessage.getSentDate() : " + dtRetVal.toString());
        }
        return dtRetVal;
    }

    public String getSubject() throws MessagingException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBMimeMessage.getSubject()");
            DebugFile.incIdent();
        }
        String sRetVal;
        if (oFolder == null) {
            if (DebugFile.trace) {
                DebugFile.writeln("Message is not stored at any Folder or Folder is closed");
            }
            sRetVal = super.getSubject();
        } else {
            try {
                if (null == oHeaders) cacheHeaders();
                if (null == oHeaders) sRetVal = super.getSubject(); else sRetVal = (String) oHeaders.get("Subject");
            } catch (SQLException sqle) {
                throw new MessagingException(sqle.getMessage(), sqle);
            }
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBMimeMessage.getSubject() : " + sRetVal);
        }
        return sRetVal;
    }

    public MimePart getMessageBody() throws ParseException, MessagingException, IOException {
        MimePart oRetVal = null;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBMimeMessage.getMessageBody([MimeMessage])");
            DebugFile.incIdent();
        }
        Object oContent = getContent();
        if (DebugFile.trace) {
            if (null == oContent) DebugFile.writeln("message content is null"); else DebugFile.writeln("message content class is " + oContent.getClass().getName());
        }
        String sContentClass = oContent.getClass().getName();
        if (sContentClass.equals("javax.mail.internet.MimeMultipart")) {
            MimeMultipart oParts = (MimeMultipart) oContent;
            int iParts = oParts.getCount();
            MimePart oPart, oNext;
            String sType, sPrevType, sNextType;
            for (int p = 0; p < iParts; p++) {
                oPart = (MimeBodyPart) oParts.getBodyPart(0);
                sType = oPart.getContentType().toUpperCase();
                if (p < iParts - 1) sNextType = ((MimeBodyPart) oParts.getBodyPart(p + 1)).getContentType().toUpperCase(); else sNextType = "";
                if (p > 0 && iParts > 1) sPrevType = ((MimeBodyPart) oParts.getBodyPart(p - 1)).getContentType().toUpperCase(); else sPrevType = "";
                if ((iParts <= 1) && (sType.startsWith("TEXT/PLAIN") || sType.startsWith("TEXT/HTML"))) {
                    if (DebugFile.trace) DebugFile.writeln("parts=" + String.valueOf(iParts) + ", content-type=" + oPart.getContentType());
                    oRetVal = oPart;
                    break;
                } else if (((p == 0) && (iParts > 1) && sType.startsWith("TEXT/PLAIN") && sNextType.startsWith("TEXT/HTML"))) {
                    if (DebugFile.trace) DebugFile.writeln("parts=" + String.valueOf(iParts) + ", part=0, content-type=" + oPart.getContentType() + ", next-type=" + sNextType);
                    oRetVal = ((MimeBodyPart) oParts.getBodyPart(p + 1));
                    break;
                } else if ((p == 1) && sType.startsWith("TEXT/PLAIN") && sPrevType.startsWith("TEXT/HTML")) {
                    if (DebugFile.trace) DebugFile.writeln("parts=" + String.valueOf(iParts) + ", part=1, content-type=" + oPart.getContentType() + ", prev-type=" + sPrevType);
                    oRetVal = ((MimeBodyPart) oParts.getBodyPart(p - 1));
                    break;
                } else {
                    oRetVal = DBMimePart.getMessagePart(oPart, p);
                }
            }
        } else if (sContentClass.equals("java.lang.String")) {
            oRetVal = new MimeBodyPart();
            oRetVal.setText((String) oContent);
        } else {
            throw new MessagingException("Unparsed Mime Content " + oContent.getClass().getName());
        }
        if (null == oRetVal) {
            oRetVal = new MimeBodyPart();
            oRetVal.setText("");
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBMimeMessage.getMessageBody() : " + oRetVal.getContentType());
        }
        return oRetVal;
    }

    public void setFlag(Flags.Flag oFlg, boolean bFlg) throws MessagingException {
        String sColunm;
        super.setFlag(oFlg, bFlg);
        if (oFlg.equals(Flags.Flag.ANSWERED)) sColunm = DB.bo_answered; else if (oFlg.equals(Flags.Flag.DELETED)) sColunm = DB.bo_deleted; else if (oFlg.equals(Flags.Flag.DRAFT)) sColunm = DB.bo_draft; else if (oFlg.equals(Flags.Flag.FLAGGED)) sColunm = DB.bo_flagged; else if (oFlg.equals(Flags.Flag.RECENT)) sColunm = DB.bo_recent; else if (oFlg.equals(Flags.Flag.SEEN)) sColunm = DB.bo_seen; else sColunm = null;
        if (null != sColunm && oFolder instanceof DBFolder) {
            JDCConnection oConn = null;
            PreparedStatement oUpdt = null;
            try {
                oConn = ((DBFolder) oFolder).getConnection();
                String sSQL = "UPDATE " + DB.k_mime_msgs + " SET " + sColunm + "=" + (bFlg ? "1" : "0") + " WHERE " + DB.gu_mimemsg + "='" + getMessageGuid() + "'";
                if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + ")");
                oUpdt = oConn.prepareStatement(sSQL);
                oUpdt.executeUpdate();
                oUpdt.close();
                oUpdt = null;
                oConn.commit();
                oConn = null;
            } catch (SQLException e) {
                if (null != oConn) {
                    try {
                        oConn.rollback();
                    } catch (Exception ignore) {
                    }
                }
                if (null != oUpdt) {
                    try {
                        oUpdt.close();
                    } catch (Exception ignore) {
                    }
                }
                if (DebugFile.trace) DebugFile.decIdent();
                throw new MessagingException(e.getMessage(), e);
            }
        }
    }

    public void saveChanges() throws MessagingException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBMimeMessage.saveChanges()");
            DebugFile.incIdent();
        }
        Flags oFlgs = getFlags();
        String sSQL;
        if (oFolder instanceof DBFolder) {
            JDCConnection oConn = null;
            try {
                oConn.commit();
                oConn = null;
            } catch (SQLException e) {
                if (null != oConn) {
                    try {
                        oConn.rollback();
                    } catch (Exception ignore) {
                    }
                }
                if (DebugFile.trace) DebugFile.decIdent();
                throw new MessagingException(e.getMessage(), e);
            }
        } else {
            super.saveChanges();
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBMimeMessage.saveChanges()");
        }
    }

    /**
   * Get message parts as an array of DBMimePart objects
   * @return DBMimeMultiPart if this message folder is of type DBFolder
   * or another type of Object if this message folder is another subclass of javax.mail.Folder
   * such as POP3Folder.
   * @throws MessagingException
   * @throws IOException
   * @throws NullPointerException If this message Folder is <b>null</b>
   */
    public Multipart getParts() throws MessagingException, IOException, NullPointerException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBMimeMessage.getParts()");
            DebugFile.incIdent();
        }
        if (oFolder == null) {
            if (DebugFile.trace) DebugFile.decIdent();
            throw new NullPointerException("DBMimeMessage.getContent() : Folder for message cannot be null");
        }
        if (DebugFile.trace) DebugFile.writeln("Folder type " + oFolder.getClass().getName());
        Multipart oRetVal;
        if (oFolder.getClass().getName().equals("com.knowgate.hipermail.DBFolder")) {
            if (sGuid == null) {
                if (DebugFile.trace) DebugFile.decIdent();
                throw new NullPointerException("DBMimeMessage.getContent() : message GUID cannot be null");
            }
            PreparedStatement oStmt = null;
            ResultSet oRSet = null;
            DBMimeMultipart oMultiPart = new DBMimeMultipart((Part) this);
            try {
                if (DebugFile.trace) {
                    DebugFile.writeln("Connection.prepareStatement(SELECT id_part,id_content,id_disposition,len_part,de_part,tx_md5,id_encoding,file_name,id_type FROM " + DB.k_mime_parts + " WHERE " + DB.gu_mimemsg + "='" + sGuid + "')");
                }
                oStmt = ((DBFolder) oFolder).getConnection().prepareStatement("SELECT id_part,id_content,id_disposition,len_part,de_part,tx_md5,id_encoding,file_name,id_type FROM " + DB.k_mime_parts + " WHERE " + DB.gu_mimemsg + "=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                oStmt.setString(1, sGuid);
                oRSet = oStmt.executeQuery();
                while (oRSet.next()) {
                    if (DebugFile.trace) DebugFile.writeln("DBMimeMultipart.addBodyPart(" + sGuid + "," + String.valueOf(oRSet.getInt(1)) + "," + oRSet.getString(2) + "," + oRSet.getString(9) + "," + oRSet.getString(6) + "," + oRSet.getString(5) + "," + oRSet.getString(3) + "," + oRSet.getString(7) + "," + oRSet.getString(8) + "," + String.valueOf(oRSet.getInt(4)));
                    MimePart oPart = new DBMimePart(oMultiPart, oRSet.getInt(1), oRSet.getString(2), oRSet.getString(9), oRSet.getString(6), oRSet.getString(5), oRSet.getString(3), oRSet.getString(7), oRSet.getString(8), oRSet.getInt(4));
                    oMultiPart.addBodyPart(oPart);
                }
                oRSet.close();
                oRSet = null;
                oStmt.close();
                oStmt = null;
            } catch (SQLException sqle) {
                try {
                    if (oRSet != null) oRSet.close();
                } catch (Exception e) {
                }
                try {
                    if (oStmt != null) oStmt.close();
                } catch (Exception e) {
                }
                throw new MessagingException(sqle.getMessage(), sqle);
            }
            oRetVal = oMultiPart;
        } else {
            oRetVal = (MimeMultipart) super.getContent();
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBMimeMessage.getParts() : " + (oRetVal == null ? "null" : oRetVal.getClass().getName()));
        }
        return oRetVal;
    }

    public MimePart getBody() throws ParseException, MessagingException, IOException {
        MimePart oRetVal = null;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBMimeMessage.getBody([MimeMessage])");
            DebugFile.incIdent();
        }
        Object oContent = null;
        try {
            oContent = super.getContent();
        } catch (Exception xcpt) {
            DebugFile.decIdent();
            throw new ParseException("MimeMessage.getContent() ParseException cause " + xcpt.getClass().getName() + " " + (xcpt.getMessage() == null ? "" : xcpt.getMessage()));
        }
        if (DebugFile.trace) {
            if (null == oContent) DebugFile.writeln("message content is null"); else DebugFile.writeln("message content class is " + oContent.getClass().getName());
        }
        String sContentClass = oContent.getClass().getName();
        if (sContentClass.equals("javax.mail.internet.MimeMultipart")) {
            MimeMultipart oParts = (MimeMultipart) oContent;
            int iParts = oParts.getCount();
            MimePart oPart, oNext;
            String sType, sPrevType, sNextType;
            for (int p = 0; p < iParts; p++) {
                oPart = (MimePart) oParts.getBodyPart(0);
                sType = oPart.getContentType().toUpperCase();
                if (p < iParts - 1) sNextType = ((MimeBodyPart) oParts.getBodyPart(p + 1)).getContentType().toUpperCase(); else sNextType = "";
                if (p > 0 && iParts > 1) sPrevType = ((MimeBodyPart) oParts.getBodyPart(p - 1)).getContentType().toUpperCase(); else sPrevType = "";
                if ((iParts <= 1) && (sType.startsWith("TEXT/PLAIN") || sType.startsWith("TEXT/HTML"))) {
                    if (DebugFile.trace) DebugFile.writeln("parts=" + String.valueOf(iParts) + ", content-type=" + oPart.getContentType());
                    oRetVal = oPart;
                    break;
                } else if (((p == 0) && (iParts > 1) && sType.startsWith("TEXT/PLAIN") && sNextType.startsWith("TEXT/HTML"))) {
                    if (DebugFile.trace) DebugFile.writeln("parts=" + String.valueOf(iParts) + ", part=0, content-type=" + oPart.getContentType() + ", next-type=" + sNextType);
                    oRetVal = ((MimeBodyPart) oParts.getBodyPart(p + 1));
                    break;
                } else if ((p == 1) && sType.startsWith("TEXT/PLAIN") && sPrevType.startsWith("TEXT/HTML")) {
                    if (DebugFile.trace) DebugFile.writeln("parts=" + String.valueOf(iParts) + ", part=1, content-type=" + oPart.getContentType() + ", prev-type=" + sPrevType);
                    oRetVal = ((MimeBodyPart) oParts.getBodyPart(p - 1));
                    break;
                } else {
                    oRetVal = DBMimePart.getMessagePart(oPart, p);
                }
            }
        } else if (sContentClass.equals("java.lang.String")) {
            oRetVal = new MimeBodyPart();
            oRetVal.setText((String) oContent);
        } else if (oContent instanceof InputStream) {
            if (DebugFile.trace) DebugFile.writeln("No data handler found for Content-Type, decoding as ISO-8859-1 string");
            InputStream oInStrm = (InputStream) oContent;
            ByteArrayOutputStream oBaStrm = new ByteArrayOutputStream();
            StreamPipe oPipe = new StreamPipe();
            oPipe.between(oInStrm, oBaStrm);
            oRetVal = new MimeBodyPart();
            oRetVal.setText(oBaStrm.toString("ISO8859_1"));
        } else {
            throw new MessagingException("Unparsed Mime Content " + oContent.getClass().getName());
        }
        if (null == oRetVal) {
            oRetVal = new MimeBodyPart();
            oRetVal.setText("");
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBMimeMessage.getBody() : " + oRetVal.getContentType());
        }
        return oRetVal;
    }

    /**
   * Get message body text into a StringBuffer
   * @param oBuffer StringBuffer
   * @throws MessagingException
   * @throws IOException
   * @throws ClassCastException
   */
    public void getText(StringBuffer oBuffer) throws MessagingException, IOException, ClassCastException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBMimeMessage.getText()");
            DebugFile.incIdent();
        }
        if (null == oFolder) {
            Multipart oParts = (Multipart) super.getContent();
            if (DebugFile.trace) DebugFile.writeln("MimeBodyPart = MimeMultipart.getBodyPart(0)");
            BodyPart oPart0 = oParts.getBodyPart(0);
            if (DebugFile.trace) {
                if (null == oPart0) DebugFile.writeln("part 0 is null"); else DebugFile.writeln("part 0 is " + oPart0.getClass().getName());
            }
            DBMimePart.parseMimePart(oBuffer, null, null, getMessageID() != null ? getMessageID() : getContentID(), (MimePart) oPart0, 0);
        } else {
            InputStream oInStrm;
            PreparedStatement oStmt = null;
            ResultSet oRSet = null;
            MimeBodyPart oBody = null;
            String sFolderNm = null;
            String sType = "multipart/";
            try {
                sFolderNm = ((DBFolder) oFolder).getCategory().getStringNull(DB.nm_category, null);
                if (getMessageGuid() != null) {
                    oStmt = ((DBFolder) oFolder).getConnection().prepareStatement("SELECT " + DB.id_type + "," + DB.by_content + " FROM " + DB.k_mime_msgs + " WHERE " + DB.gu_mimemsg + "=?");
                    oStmt.setString(1, getMessageGuid());
                } else {
                    oStmt = ((DBFolder) oFolder).getConnection().prepareStatement("SELECT " + DB.id_type + "," + DB.by_content + " FROM " + DB.k_mime_msgs + " WHERE " + DB.id_message + "=? AND " + DB.gu_category + "=?");
                    oStmt.setString(1, getMessageID());
                    oStmt.setString(2, ((DBFolder) oFolder).getCategory().getString(DB.gu_category));
                }
                oRSet = oStmt.executeQuery();
                if (oRSet.next()) {
                    sType = oRSet.getString(1);
                    oInStrm = oRSet.getBinaryStream(2);
                    if (!oRSet.wasNull()) {
                        oBody = new MimeBodyPart(oInStrm);
                        oInStrm.close();
                    }
                }
                oRSet.close();
                oRSet = null;
                oStmt.close();
                oStmt = null;
            } catch (SQLException sqle) {
                if (oRSet != null) {
                    try {
                        oRSet.close();
                    } catch (Exception ignore) {
                    }
                }
                if (oStmt != null) {
                    try {
                        oStmt.close();
                    } catch (Exception ignore) {
                    }
                }
                throw new MessagingException(sqle.getMessage(), sqle);
            }
            if (oBody != null) {
                if (sType.startsWith("text/")) oBuffer.append(oBody.getContent()); else DBMimePart.parseMimePart(oBuffer, null, sFolderNm, getMessageID() != null ? getMessageID() : getContentID(), oBody, 0);
            }
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBMimeMessage.getText() : " + String.valueOf(oBuffer.length()));
        }
    }

    public String getText() throws MessagingException, IOException {
        StringBuffer oStrBuff = new StringBuffer(16000);
        getText(oStrBuff);
        return oStrBuff.toString();
    }

    public void getTextPlain(StringBuffer oBuffer) throws MessagingException, IOException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBMimeMessage.getTextPlain()");
            DebugFile.incIdent();
        }
        boolean bHasPlainTextVersion = false;
        if (getContentType().startsWith("text/plain")) {
            getText(oBuffer);
        } else if (getContentType().startsWith("text/html")) {
            StringBuffer oHtmlBuff = new StringBuffer();
            getText(oHtmlBuff);
            StringBean oStrBn = new StringBean();
            oStrBn.setInputHTML(oHtmlBuff.toString());
            oBuffer.append(oStrBn.getStrings());
        } else {
            if (DebugFile.trace) DebugFile.writeln("Multipart = DBMimeMessage.getParts()");
            Multipart oParts = getParts();
            final int iParts = oParts.getCount();
            MimePart oPart;
            int p;
            for (p = 0; p < iParts && !bHasPlainTextVersion; p++) {
                oPart = (MimePart) oParts.getBodyPart(p);
                String sType = oPart.getContentType();
                if (null != sType) sType = sType.toLowerCase();
                String sDisp = oPart.getDisposition();
                if (null == sDisp) sDisp = "inline"; else if (sDisp.length() == 0) sDisp = "inline";
                if (DebugFile.trace) DebugFile.writeln("scanning part " + String.valueOf(p) + sDisp + " " + sType.replace('\r', ' ').replace('\n', ' '));
                if (sType.startsWith("text/plain") && sDisp.equalsIgnoreCase("inline")) {
                    bHasPlainTextVersion = true;
                    DBMimePart.parseMimePart(oBuffer, null, getFolder().getName(), getMessageID() != null ? getMessageID() : getContentID(), oPart, p);
                }
            }
            if (DebugFile.trace) {
                if (bHasPlainTextVersion) DebugFile.writeln("MimeMultipart has plain text version at part " + String.valueOf(p)); else DebugFile.writeln("MimeMultipart has no plain text version, converting part 0 from HTML");
            }
            if (!bHasPlainTextVersion) {
                oPart = (MimeBodyPart) oParts.getBodyPart(0);
                StringBuffer oHtml = new StringBuffer();
                DBMimePart.parseMimePart(oHtml, null, getFolder().getName(), getMessageID() != null ? getMessageID() : getContentID(), oPart, 0);
                StringBean oSB = new StringBean();
                oSB.setInputHTML(oHtml.toString());
                String sStrs = oSB.getStrings();
                if (DebugFile.trace) {
                    DebugFile.writeln("StringBean.getStrings(");
                    if (null != sStrs) DebugFile.write(sStrs); else DebugFile.write("null");
                    DebugFile.writeln(")");
                }
                oBuffer.append(sStrs);
            }
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBMimeMessage.getTextPlain() : " + String.valueOf(oBuffer.length()));
        }
    }

    public void writeTo(OutputStream oOutStrm) throws IOException, FolderClosedException, MessagingException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBMimeMessage.writeTo([OutputStream])");
            DebugFile.incIdent();
        }
        if (getFolder() == null) {
            DebugFile.decIdent();
            throw new MessagingException("No folder for message");
        }
        DBFolder oDBF = (DBFolder) getFolder();
        if ((oDBF.getType() & DBFolder.MODE_MBOX) != 0) {
            if (oDBF.getConnection() == null) {
                if (DebugFile.trace) DebugFile.decIdent();
                throw new FolderClosedException(oDBF, "Folder is closed");
            }
            PreparedStatement oStmt = null;
            ResultSet oRSet = null;
            BigDecimal oPos = null;
            int iLen = 0;
            try {
                oStmt = oDBF.getConnection().prepareStatement("SELECT " + DB.nu_position + "," + DB.len_mimemsg + " FROM " + DB.k_mime_msgs + " WHERE " + DB.gu_mimemsg + "=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                oStmt.setString(1, getMessageGuid());
                oRSet = oStmt.executeQuery();
                boolean bFound = oRSet.next();
                if (bFound) {
                    oPos = oRSet.getBigDecimal(1);
                    iLen = oRSet.getInt(2);
                }
                oRSet.close();
                oRSet = null;
                oStmt.close();
                oStmt = null;
                if (!bFound) {
                    if (DebugFile.trace) DebugFile.writeln("MimeMessage.writeTo(" + oOutStrm.getClass().getName() + ")");
                    super.writeTo(oOutStrm);
                    if (DebugFile.trace) {
                        DebugFile.decIdent();
                        DebugFile.writeln("End DBMimeMessage.writeTo()");
                    }
                    return;
                }
            } catch (SQLException sqle) {
                if (oRSet != null) {
                    try {
                        oRSet.close();
                    } catch (Exception ignore) {
                    }
                }
                if (oStmt != null) {
                    try {
                        oStmt.close();
                    } catch (Exception ignore) {
                    }
                }
            }
            File oFile = oDBF.getFile();
            MboxFile oMBox = new MboxFile(oFile, MboxFile.READ_ONLY);
            InputStream oInStrm = oMBox.getMessageAsStream(oPos.longValue(), iLen);
            StreamPipe oPipe = new StreamPipe();
            oPipe.between(oInStrm, oOutStrm);
            oInStrm.close();
            oMBox.close();
        } else {
            Multipart oDBParts = getParts();
            MimeMultipart oMimeParts = new MimeMultipart();
            for (int p = 0; p < oDBParts.getCount(); p++) {
                oMimeParts.addBodyPart(oDBParts.getBodyPart(p));
                super.setContent(oMimeParts);
            }
            super.writeTo(oOutStrm);
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBMimeMessage.writeTo()");
        }
    }

    /**
     * <p>Delete message from database</p>
     * This method calls stored procedure k_sp_del_mime_msg<br>
     * @param oConn JDBC database connection
     * @param sFolderId Folder GUID (k_mime_msgs.gu_category)
     * @param sMimeMsgId Message GUID (k_mime_msgs.gu_mimemsg)
     * @throws SQLException
  */
    public static void delete(JDCConnection oConn, String sFolderId, String sMimeMsgId) throws SQLException, IOException {
        Statement oStmt;
        CallableStatement oCall;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBMimeMessage.delete([Connection], " + sMimeMsgId + ")");
            DebugFile.incIdent();
        }
        oStmt = oConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        ResultSet oRSet = oStmt.executeQuery("SELECT " + DB.file_name + " FROM " + DB.k_mime_parts + " WHERE " + DB.gu_mimemsg + "='" + sMimeMsgId + "' AND " + DB.id_disposition + "='reference'");
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
        if (oConn.getDataBaseProduct() == JDCConnection.DBMS_POSTGRESQL) {
            oStmt = oConn.createStatement();
            oStmt.executeQuery("SELECT k_sp_del_mime_msg('" + sMimeMsgId + "')");
            oStmt.close();
        } else {
            oCall = oConn.prepareCall("{ call k_sp_del_mime_msg(?) }");
            oCall.setString(1, sMimeMsgId);
            oCall.execute();
            oCall.close();
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBMimeMessage.delete()");
        }
    }

    public static String getGuidFromId(JDCConnection oConn, String sMsgId) throws SQLException {
        String sMsgGuid;
        switch(oConn.getDataBaseProduct()) {
            case JDCConnection.DBMS_POSTGRESQL:
                PreparedStatement oStmt = oConn.prepareStatement("SELECT k_sp_get_mime_msg(?)");
                oStmt.setString(1, sMsgId);
                ResultSet oRSet = oStmt.executeQuery();
                oRSet.next();
                sMsgGuid = oRSet.getString(1);
                oRSet.close();
                oRSet = null;
                oStmt.close();
                oStmt = null;
                break;
            default:
                CallableStatement oCall = oConn.prepareCall("{ call k_sp_get_mime_msg(?,?) }");
                oCall.setString(1, sMsgId);
                oCall.registerOutParameter(2, Types.CHAR);
                oCall.execute();
                sMsgGuid = oCall.getString(2);
                if (sMsgGuid != null) sMsgGuid = sMsgGuid.trim();
                oCall.close();
                oCall = null;
        }
        return sMsgGuid;
    }

    public static final short ClassId = 822;
}
