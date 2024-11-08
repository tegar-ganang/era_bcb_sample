package ebiNeutrinoSDK.gui.component;

import ebiNeutrino.core.gui.Dialogs.EBIEMailSendReciveStatus;
import ebiNeutrinoSDK.EBIPGFactory;
import ebiNeutrinoSDK.gui.dialogs.EBIExceptionDialog;
import ebiNeutrinoSDK.gui.dialogs.EBIMessage;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EBINeutrinoEMailFunction {

    private EBIPGFactory ebiPGFactory = null;

    private String rSubject = "";

    private String rFrom = "";

    private String rTo = "";

    private String rMessage = "";

    private String rCC = "";

    private java.util.Date rDate = null;

    private String AtID = "";

    private EBIEMailSendReciveStatus sendMail = null;

    public EBINeutrinoEMailFunction(EBIPGFactory ebiPGFactory) {
        this.ebiPGFactory = ebiPGFactory;
    }

    private void saveIfSended(String _to, String _cc, String _subject, String _bodyText, String[] _fileName) {
        boolean haveAttach = false;
        String AttachID;
        if (_fileName != null) {
            if (_fileName.length > 0) {
                haveAttach = true;
            }
        }
        if (haveAttach == true) {
            String qry = "INSERT INTO MAIL_ATTACH SET " + " MAIL_ATTACHID=?," + " FILENAME=?," + " FILEBIN=?";
            try {
                ebiPGFactory.getIEBIDatabase().setAutoCommit(true);
                AttachID = "OUT" + new java.util.Date().getTime();
                for (int i = 0; i < _fileName.length; i++) {
                    File file = new File(_fileName[i]);
                    if (file.isFile()) {
                        InputStream in = readFileGetBlob(file);
                        PreparedStatement prs = ebiPGFactory.getIEBIDatabase().initPreparedStatement(qry);
                        prs.setString(1, AttachID);
                        prs.setString(2, file.getName());
                        prs.setBinaryStream(3, in, ((int) file.length()));
                        ebiPGFactory.getIEBIDatabase().executePreparedStmt(prs);
                    }
                }
                String query = "INSERT INTO MAIL_OUTBOX SET " + "MAIL_FROM=?," + "MAIL_TO=?," + "MAIL_CC=?," + "MAIL_SUBJECT=?," + "MAIL_MESSAGE=?," + "MAIL_DATE=?," + "SETFROM=?";
                if (haveAttach == true) {
                    query += ",ATTACHID=?";
                }
                PreparedStatement ps = ebiPGFactory.getIEBIDatabase().initPreparedStatement(query);
                ps.setString(1, EBIPGFactory.emailFrom);
                ps.setString(2, _to);
                ps.setString(3, _cc);
                ps.setString(4, _subject);
                ps.setString(5, _bodyText);
                ps.setDate(6, new java.sql.Date(new java.util.Date().getTime()));
                ps.setString(7, EBIPGFactory.ebiUser);
                if (haveAttach == true) {
                    ps.setString(8, AttachID);
                }
                ebiPGFactory.getIEBIDatabase().executePreparedStmt(ps);
            } catch (java.sql.SQLException ex) {
                EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
            } finally {
                ebiPGFactory.getIEBIDatabase().setAutoCommit(false);
            }
        }
    }

    public void sendEMailMessage(final String _to, final String _cc, final String _subject, final String _bodyText, final String[] _fileName) {
        final EBIEMailSendReciveStatus sendMail = new EBIEMailSendReciveStatus(EBIPGFactory.getLANG("EBI_LANG_C_SEND_EMAIL"));
        sendMail.setModal(true);
        final Runnable waitRunner = new Runnable() {

            public void run() {
                Properties props = new Properties();
                props.setProperty("mail.transport.protocol", "smtp");
                props.setProperty("mail.host", EBIPGFactory.emailSMTPServer);
                props.setProperty("mail.user", EBIPGFactory.emailSMTPUser);
                props.setProperty("mail.password", EBIPGFactory.emailSMTPPassword);
                int i;
                try {
                    Session session = Session.getDefaultInstance(props, null);
                    Message msg = new MimeMessage(session);
                    msg.setFrom(new InternetAddress(EBIPGFactory.emailFrom, EBIPGFactory.emailFromTitle));
                    InternetAddress[] address = { new InternetAddress(_to) };
                    msg.setRecipients(Message.RecipientType.TO, address);
                    if (!"".equals(_cc)) {
                        InternetAddress[] adressRTo = { new InternetAddress(_cc) };
                        msg.setReplyTo(adressRTo);
                    }
                    msg.setSubject(_subject);
                    MimeBodyPart textPart = new MimeBodyPart();
                    textPart.setContent(_bodyText, "text/html");
                    Multipart mp = new MimeMultipart();
                    mp.addBodyPart(textPart);
                    if (_fileName != null && !"-1".equals(_fileName[0])) {
                        MimeBodyPart[] attachFilePart = new MimeBodyPart[_fileName.length];
                        for (i = 0; i < _fileName.length; i++) {
                            try {
                                attachFilePart[i] = new MimeBodyPart();
                                FileDataSource fds = new FileDataSource(_fileName[i]);
                                attachFilePart[i].setDataHandler(new DataHandler(fds));
                                attachFilePart[i].attachFile(fds.getFile());
                                mp.addBodyPart(attachFilePart[i]);
                            } catch (java.io.FileNotFoundException ex) {
                                EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
                            }
                        }
                    }
                    msg.setContent(mp);
                    Transport.send(msg);
                    sendMail.setVisible(false);
                    saveIfSended(_to, _cc, _subject, _bodyText, _fileName);
                    EBIExceptionDialog.getInstance(EBIPGFactory.getLANG("EBI_LANG_MESSAGE_MESSAGE_SEND")).Show(EBIMessage.INFO_MESSAGE);
                } catch (java.io.UnsupportedEncodingException ex) {
                    EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
                } catch (javax.mail.internet.AddressException ex) {
                    EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
                } catch (javax.mail.SendFailedException ex) {
                    EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
                } catch (java.io.IOException ex) {
                    EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
                } catch (MessagingException ex) {
                    EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
                }
            }
        };
        Thread loaderThread = new Thread(waitRunner, "SendMailThread");
        loaderThread.start();
        sendMail.setVisible(true);
    }

    public void deleteEMail(String id) {
        ResultSet set = null;
        try {
            PreparedStatement prs = ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT * FROM MAIL_DELETED WHERE ID=?");
            prs.setString(1, id);
            set = ebiPGFactory.getIEBIDatabase().executePreparedQuery(prs);
            ebiPGFactory.getIEBIDatabase().setAutoCommit(true);
            set.next();
            PreparedStatement prs1 = ebiPGFactory.getIEBIDatabase().initPreparedStatement("DELETE FROM MAIL_DELETED WHERE ID=?");
            prs1.setString(1, id);
            ebiPGFactory.getIEBIDatabase().executePreparedStmt(prs1);
            if (set.getString("ATTACHID") != null) {
                PreparedStatement prs2 = ebiPGFactory.getIEBIDatabase().initPreparedStatement("DELETE FROM MAIL_ATTACH WHERE MAIL_ATTACHID=?");
                prs2.setString(1, set.getString("ATTACHID"));
                ebiPGFactory.getIEBIDatabase().executePreparedStmt(prs2);
            }
            EBIExceptionDialog.getInstance(EBIPGFactory.getLANG("EBI_LANG_C_INFO_MESSAGE_DELETE_SUCCESSFULLY")).Show(EBIMessage.INFO_MESSAGE);
        } catch (SQLException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
        } finally {
            ebiPGFactory.getIEBIDatabase().setAutoCommit(false);
            if (set != null) {
                try {
                    set.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void moveEMailToFolder(int id, String fromTable, String toTable, int companyID) {
        ResultSet rs = null;
        try {
            ebiPGFactory.getIEBIDatabase().setAutoCommit(true);
            PreparedStatement ps1 = ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT * FROM " + fromTable + "  WHERE ID=?");
            ps1.setInt(1, id);
            rs = ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps1);
            rs.next();
            String sql = "INSERT INTO " + toTable + " SET " + "MAIL_DATE=?," + "SETFROM=?," + "MAIL_FROM=?," + "MAIL_TO=?," + "MAIL_CC=?," + "MAIL_SUBJECT=?," + "MAIL_MESSAGE=?," + "ATTACHID=?";
            if (toTable.equals("MAIL_ASSIGNED")) {
                sql += ", COMPANYID=?";
            }
            String abkzFrom = fromTable.substring(5, 8);
            String abkzTo = toTable.substring(5, 8);
            PreparedStatement ps = ebiPGFactory.getIEBIDatabase().initPreparedStatement(sql);
            ps.setDate(1, new java.sql.Date(new java.util.Date().getTime()));
            ps.setString(2, EBIPGFactory.ebiUser);
            ps.setString(3, rs.getString("MAIL_FROM"));
            ps.setString(4, rs.getString("MAIL_TO"));
            ps.setString(5, rs.getString("MAIL_CC"));
            ps.setString(6, rs.getString("MAIL_SUBJECT"));
            ps.setString(7, rs.getString("MAIL_MESSAGE"));
            String toAssign = "";
            if (rs.getString("ATTACHID") != null) {
                toAssign = rs.getString("ATTACHID").replaceFirst(abkzFrom, abkzTo);
            }
            ps.setString(8, toAssign);
            if (toTable.equals("MAIL_ASSIGNED")) {
                ps.setInt(9, companyID);
            }
            ebiPGFactory.getIEBIDatabase().executePreparedStmt(ps);
            if (rs.getString("ATTACHID") != null) {
                String assignID = rs.getString("AttachID").replaceFirst(abkzFrom, abkzTo);
                PreparedStatement ps3 = ebiPGFactory.getIEBIDatabase().initPreparedStatement("UPDATE  MAIL_ATTACH SET MAIL_ATTACHID=? where MAIL_ATTACHID=?");
                ps3.setString(1, assignID);
                ps3.setString(2, rs.getString("ATTACHID"));
                ebiPGFactory.getIEBIDatabase().executePreparedStmt(ps3);
            }
            PreparedStatement ps4 = ebiPGFactory.getIEBIDatabase().initPreparedStatement("DELETE FROM " + fromTable + " WHERE ID=?");
            ps4.setInt(1, id);
            ebiPGFactory.getIEBIDatabase().executePreparedStmt(ps4);
        } catch (SQLException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
        } finally {
            ebiPGFactory.getIEBIDatabase().setAutoCommit(false);
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public EBIMailBean getEMailFromTableId(int id, String dbTable) {
        ResultSet rs;
        EBIMailBean emailBean = new EBIMailBean();
        try {
            PreparedStatement ps1 = ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT * FROM " + dbTable + " WHERE ID=?");
            ps1.setInt(1, id);
            rs = ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps1);
            rs.next();
            emailBean.setId(rs.getInt("ID"));
            emailBean.setAttachmentId(rs.getInt("ATTACHID"));
            emailBean.setFrom(rs.getString("MAIL_FROM"));
            emailBean.setTo(rs.getString("MAIL_TO"));
            emailBean.setCc(rs.getString("MAIL_CC"));
            emailBean.setSubject(rs.getString("MAIL_SUBJECT"));
            emailBean.setEmailBody(rs.getString("MAIL_MESSAGE"));
            emailBean.setMailDate(rs.getDate("MAIL_DATE"));
            if (rs.getString("ATTACHID") != null) {
                PreparedStatement ps2 = ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT * FROM MAIL_ATTACH WHERE MAIL_ATTACHID=?");
                ps2.setString(1, rs.getString("ATTACHID"));
                ResultSet set = ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps2);
                List<EBIMailAttachmentBean> attList = new ArrayList<EBIMailAttachmentBean>();
                while (set.next()) {
                    EBIMailAttachmentBean attBean = new EBIMailAttachmentBean();
                    attBean.setFileName(set.getString("FILENAME"));
                    attBean.setId(set.getInt("MAIL_ATTACHID"));
                    attList.add(attBean);
                }
                emailBean.setAttachment(attList);
                set.close();
            }
            rs.close();
        } catch (SQLException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
        }
        return emailBean;
    }

    public int getEmailCount(String Table) {
        int ret = 0;
        ResultSet rs;
        try {
            PreparedStatement ps = ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT COUNT(*) FROM " + Table);
            rs = ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps);
            if (rs != null) {
                rs.next();
                ret = rs.getInt(1);
                rs.close();
            }
        } catch (java.sql.SQLException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
            return ret;
        }
        return ret;
    }

    public int getEmailCount(String Table, int pCompanyID) {
        int ret = 0;
        ResultSet rs;
        try {
            PreparedStatement ps = ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT COUNT(*) FROM " + Table + " WHERE COMPANYID=?");
            ps.setInt(1, pCompanyID);
            rs = ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps);
            rs.next();
            ret = rs.getInt(1);
            rs.close();
        } catch (java.sql.SQLException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
            return ret;
        }
        return ret;
    }

    public List<EBIMailBean> getEMailsFromTable(String table) {
        ResultSet rs;
        List<EBIMailBean> emailList = null;
        try {
            PreparedStatement ps = ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT * FROM " + table);
            rs = ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps);
            rs.last();
            if (rs.getRow() > 0) {
                emailList = new ArrayList<EBIMailBean>();
                rs.beforeFirst();
                while (rs.next()) {
                    EBIMailBean emailBean = new EBIMailBean();
                    emailBean.setFrom(rs.getString("MAIL_FROM"));
                    emailBean.setSubject(rs.getString("MAIL_SUBJECT"));
                    emailBean.setMailDate(rs.getDate("MAIL_DATE"));
                    emailBean.setAttachmentId(rs.getString("ATTACHID") == null ? -1 : rs.getInt("ATTACHID"));
                    emailBean.setTo(rs.getString("MAIL_TO"));
                    emailBean.setCc(rs.getString("MAIL_CC"));
                    emailBean.setId(rs.getInt("ID"));
                    emailList.add(emailBean);
                }
            }
            rs.close();
        } catch (java.sql.SQLException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
        }
        return emailList;
    }

    public List<EBIMailBean> getEMailFromCompanyId(int companyID, String dbTable) {
        ResultSet rs;
        List<EBIMailBean> emailList = null;
        try {
            PreparedStatement ps = ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT * FROM " + dbTable + " WHERE COMPANYID=?");
            ps.setInt(1, companyID);
            rs = ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps);
            rs.last();
            if (rs.getRow() > 0) {
                emailList = new ArrayList<EBIMailBean>();
                rs.beforeFirst();
                while (rs.next()) {
                    EBIMailBean emailBean = new EBIMailBean();
                    emailBean.setFrom(rs.getString("MAIL_FROM"));
                    emailBean.setSubject(rs.getString("MAIL_SUBJECT"));
                    emailBean.setMailDate(rs.getDate("MAIL_DATE"));
                    emailBean.setAttachmentId(rs.getString("ATTACHID") == null ? -1 : rs.getInt("ATTACHID"));
                    emailBean.setTo(rs.getString("MAIL_TO"));
                    emailBean.setCc(rs.getString("MAIL_CC"));
                    emailBean.setId(rs.getInt("ID"));
                    emailList.add(emailBean);
                }
            }
            rs.close();
        } catch (java.sql.SQLException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
        }
        return emailList;
    }

    private InputStream readFileGetBlob(File file) {
        InputStream is;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.getLANG("EBI_LANG_ERROR_FILE_NOT_FOUND")).Show(EBIMessage.INFO_MESSAGE);
            return null;
        }
        return is;
    }

    public void saveEMailAttachmentToFileSystem(String attachID, String realName, String fileName, String pathToSave) {
        String FileName;
        InputStream is = null;
        OutputStream fos = null;
        ResultSet rs = null;
        try {
            ebiPGFactory.getIEBIDatabase().setAutoCommit(true);
            PreparedStatement ps = ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT * FROM MAIL_ATTACH WHERE FILENAME=? and MAIL_ATTACHID=?");
            ps.setString(1, realName);
            ps.setString(2, attachID);
            rs = ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps);
            while (rs.next()) {
                is = rs.getBinaryStream("FILEBIN");
                FileName = pathToSave;
                fos = new FileOutputStream(FileName);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer, 0, buffer.length)) != -1) {
                    fos.write(buffer, 0, read);
                }
            }
        } catch (java.sql.SQLException ex) {
            ex.printStackTrace();
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
        } catch (FileNotFoundException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
        } catch (IOException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
        } finally {
            ebiPGFactory.getIEBIDatabase().setAutoCommit(false);
            try {
                if (fos != null) {
                    fos.close();
                }
                if (is != null) {
                    is.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ex) {
            } catch (IOException ex) {
            }
        }
    }
}
