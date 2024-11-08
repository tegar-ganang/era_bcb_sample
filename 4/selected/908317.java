package ebiCRMMail;

import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;
import com.sun.mail.util.BASE64DecoderStream;
import ebiCRM.EBICRMModule;
import ebiCRM.table.models.MyTableModelAvailableEmails;
import ebiNeutrinoSDK.EBIPGFactory;
import ebiNeutrinoSDK.gui.component.EBIVisualPanelTemplate;
import ebiNeutrinoSDK.gui.dialogs.EBIDialogExt;
import ebiNeutrinoSDK.gui.dialogs.EBIExceptionDialog;
import ebiNeutrinoSDK.gui.dialogs.EBIMessage;
import ebiNeutrinoSDK.utils.EBIConstant;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import sun.misc.IOUtils;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

public class EBICRMMailClientFunctions {

    private EBICRMModule ebiModule = null;

    private MyTableModelAvailableEmails tabMod = null;

    private String rSubject = "";

    private String rFrom = "";

    private String rTo = "";

    private String rMessage = "";

    private String rCC = "";

    private java.util.Date rDate = null;

    private String AtID = "";

    public EBIEMailSendReceiveStatus sendMail = null;

    private String[] fileNames = null;

    private boolean ret = false;

    public String emailText = "";

    public String from = "";

    public String to = "";

    public String cc = "";

    public String subject = "";

    public static boolean showWindow = false;

    public EBICRMMailClientFunctions(EBICRMModule modul, MyTableModelAvailableEmails mod) {
        ebiModule = modul;
        tabMod = mod;
    }

    public EBICRMMailClientFunctions(EBICRMModule modul) {
        ebiModule = modul;
    }

    public void saveAndSentEmail() {
        sendIfOk();
    }

    public void saveIfSended() {
        boolean haveAttach = false;
        String AttachID = "";
        if (ebiModule.getComposerMail().listModel.getSize() > 0) {
            haveAttach = true;
        }
        try {
            if (haveAttach == true) {
                String qry = "INSERT INTO MAIL_ATTACH SET " + " MAIL_ATTACHID=?," + " FILENAME=?," + " FILEBIN=?";
                ebiModule.ebiPGFactory.getIEBIDatabase().setAutoCommit(true);
                AttachID = "OUT" + new java.util.Date().getTime();
                for (int i = 0; i < ebiModule.getComposerMail().listModel.getSize(); i++) {
                    InputStream in = readFileGetBlob(new File(ebiModule.getComposerMail().listModel.get(i).toString()));
                    File file = new File(ebiModule.getComposerMail().listModel.get(i).toString());
                    PreparedStatement prs = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement(qry);
                    prs.setString(1, AttachID);
                    prs.setString(2, new File(ebiModule.getComposerMail().listModel.get(i).toString()).getName());
                    prs.setBinaryStream(3, in, ((int) file.length()));
                    ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedStmt(prs);
                }
            }
            String query = "INSERT INTO MAIL_OUTBOX SET " + "MAIL_FROM=?," + "MAIL_TO=?," + "MAIL_CC=?," + "MAIL_SUBJECT=?," + "MAIL_MESSAGE=?," + "MAIL_DATE=?," + "SETFROM=?";
            if (haveAttach == true) {
                query += ",ATTACHID=?";
            }
            PreparedStatement ps = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement(query);
            ps.setString(1, EBIPGFactory.emailFrom);
            ps.setString(2, ebiModule.getComposerMail().jTextTo.getText());
            ps.setString(3, ebiModule.getComposerMail().jTextCC.getText());
            ps.setString(4, ebiModule.getComposerMail().jTextSubject.getText());
            ps.setString(5, ebiModule.getComposerMail().jEditorMessageView.getHTMLContent());
            ps.setDate(6, new java.sql.Date(new java.util.Date().getTime()));
            ps.setString(7, EBIPGFactory.ebiUser);
            if (haveAttach == true) {
                ps.setString(8, AttachID);
            }
            ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedStmt(ps);
        } catch (SQLException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
        } finally {
            ebiModule.ebiPGFactory.getIEBIDatabase().setAutoCommit(false);
        }
    }

    public void sendIfOk() {
        final Runnable waitRunner = new Runnable() {

            public void run() {
                ebiModule.getComposerMail().jButtonSendEmail.setEnabled(false);
                if (ebiModule.getComposerMail().listModel.getSize() > 0) {
                    fileNames = new String[ebiModule.getComposerMail().listModel.getSize()];
                    for (int i = 0; i < ebiModule.getComposerMail().listModel.getSize(); i++) {
                        fileNames[i] = ebiModule.getComposerMail().listModel.get(i).toString();
                    }
                }
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        ret = sendMsgAttachFile(ebiModule.getComposerMail().jTextTo.getText(), ebiModule.getComposerMail().jTextCC.getText(), ebiModule.getComposerMail().jTextSubject.getText(), ebiModule.getComposerMail().jEditorMessageView.getHTMLContent(), fileNames);
                        if (ret == true) {
                            saveIfSended();
                            ebiModule.getComposerMail().setUnEditable(false);
                        }
                        ebiModule.getstorangeVPanel().item1.setText(EBIPGFactory.getLANG("EBI_LANG_OUTBOX") + "(" + getEmailCount("MAIL_OUTBOX") + ")");
                        ebiModule.getstorangeVPanel().updateFolder();
                        ebiModule.getstorangeVPanel().jListnames.updateUI();
                    }
                });
            }
        };
        Thread loaderThread = new Thread(waitRunner, "SendMailThread");
        loaderThread.start();
    }

    private boolean sendMsgAttachFile(String _to, String _cc, String _subject, String _bodyText, String[] _fileName) {
        boolean debug = false;
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", EBIPGFactory.emailSMTPServer);
        props.setProperty("mail.user", EBIPGFactory.emailSMTPUser);
        props.setProperty("mail.password", EBIPGFactory.emailSMTPPassword);
        int i;
        try {
            Session session = Session.getDefaultInstance(props, null);
            session.setDebug(debug);
            Message msg = new MimeMessage(session);
            try {
                msg.setFrom(new InternetAddress(EBIPGFactory.emailFrom, EBIPGFactory.emailFromTitle));
                InternetAddress[] address = InternetAddress.parse(_to);
                msg.setRecipients(Message.RecipientType.TO, address);
                if (!_cc.equals("")) {
                    InternetAddress[] adressRTo = InternetAddress.parse(_cc);
                    msg.setReplyTo(adressRTo);
                }
                msg.setSubject(_subject);
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setContent(_bodyText, "text/html");
                Multipart mp = new MimeMultipart();
                mp.addBodyPart(textPart);
                if (_fileName != null) {
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
                EBIExceptionDialog.getInstance(EBIPGFactory.getLANG("EBI_LANG_MESSAGE_MESSAGE_SEND")).Show(EBIMessage.INFO_MESSAGE);
            } catch (java.io.UnsupportedEncodingException ex) {
                EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
                return false;
            } catch (javax.mail.internet.AddressException ex) {
                EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
                return false;
            } catch (javax.mail.SendFailedException ex) {
                EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
                return false;
            } catch (java.io.IOException ex) {
                EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
                return false;
            }
        } catch (MessagingException e) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(e)).Show(EBIMessage.ERROR_MESSAGE);
            return false;
        } finally {
            sendMail.setVisible(false);
            ebiModule.getComposerMail().jButtonSendEmail.setEnabled(true);
        }
        return true;
    }

    public void deleteEMail(String id) {
        if (!ebiModule.selectedTable.equals("MAIL_DELETED")) {
            copyFromTo(Integer.parseInt(id), ebiModule.selectedTable, "MAIL_DELETED");
        } else {
            ResultSet set = null;
            try {
                ebiModule.ebiPGFactory.getIEBIDatabase().setAutoCommit(true);
                PreparedStatement prs = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT * FROM MAIL_DELETED WHERE ID=?");
                prs.setString(1, id);
                set = ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedQuery(prs);
                set.next();
                PreparedStatement prs1 = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement("DELETE FROM MAIL_DELETED WHERE ID=?");
                prs1.setString(1, id);
                ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedStmt(prs1);
                if (set.getString("ATTACHID") != null) {
                    PreparedStatement prs2 = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement("DELETE FROM MAIL_ATTACH WHERE MAIL_ATTACHID=?");
                    prs2.setString(1, set.getString("ATTACHID"));
                    ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedStmt(prs2);
                }
                EBIExceptionDialog.getInstance(EBIPGFactory.getLANG("EBI_LANG_C_INFO_MESSAGE_DELETE_SUCCESSFULLY")).Show(EBIMessage.INFO_MESSAGE);
                ebiModule.getComposerMail().setUnEditable(false);
                ebiModule.getstorangeVPanel().item3.setText(EBIPGFactory.getLANG("EBI_LANG_DELETED") + "(" + getEmailCount("MAIL_DELETED") + ")");
            } catch (SQLException ex) {
                EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
            } finally {
                ebiModule.ebiPGFactory.getIEBIDatabase().setAutoCommit(false);
                if (set != null) {
                    try {
                        set.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        this.tabMod = ebiModule.getAvailableEMailPanel().tabModel;
        showEmailTypeBox(ebiModule.selectedTable);
        ebiModule.getstorangeVPanel().updateFolder();
        ebiModule.getComposerMail().setUnEditable(false);
    }

    public void receiveEmail() {
        final Runnable waitRunner = new Runnable() {

            public void run() {
                try {
                    String host = EBIPGFactory.emailPOPServer;
                    String user = EBIPGFactory.emailPOPPUser;
                    String password = EBIPGFactory.emailPOPPPassword;
                    String protocol = EBIPGFactory.emailProtocolName;
                    Session session = Session.getDefaultInstance(System.getProperties(), null);
                    Store store = session.getStore(protocol);
                    store.connect(host, -1, user, password);
                    Folder folder = store.getFolder("INBOX");
                    folder.open(Folder.READ_WRITE);
                    int totalMessages = folder.getMessageCount();
                    if (totalMessages == 0) {
                        EBIExceptionDialog.getInstance(EBIPGFactory.getLANG("EBI_LANG_C_ERROR_NO_MESSAGE_FOUND")).Show(EBIMessage.INFO_MESSAGE);
                        sendMail.setVisible(false);
                        folder.close(false);
                        store.close();
                        return;
                    }
                    Message[] messages;
                    if (getEmailCount("MAIL_INBOX") <= 0) {
                        messages = folder.getMessages();
                    } else {
                        messages = folder.getMessages(totalMessages - 5, totalMessages);
                    }
                    for (int i = 0; i < messages.length; i++) {
                        setMessage(messages[i]);
                        saveReceivedEmail(messages[i]);
                        if (EBIPGFactory.emailDeleteMessageFromServer) {
                            messages[i].setFlag(Flags.Flag.DELETED, true);
                        }
                        AtID = "";
                    }
                    folder.close(EBIPGFactory.emailDeleteMessageFromServer);
                    store.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(e)).Show(EBIMessage.ERROR_MESSAGE);
                }
                sendMail.setVisible(false);
                showEmailTypeBox("MAIL_INBOX");
                ebiModule.getstorangeVPanel().item0.setText(EBIPGFactory.getLANG("EBI_LANG_INBOX") + "(" + getEmailCount("MAIL_INBOX") + ")");
                ebiModule.getstorangeVPanel().jListnames.updateUI();
            }
        };
        Thread loaderThread = new Thread(waitRunner, "ReceiveMailThread");
        loaderThread.start();
    }

    private void dumpPart(Multipart multipart) throws Exception {
        try {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String dispo = "";
                if (bodyPart.getDisposition() != null) {
                    dispo = bodyPart.getDisposition();
                }
                if (!dispo.equals(BodyPart.ATTACHMENT) && !dispo.equals(BodyPart.INLINE)) {
                    if (bodyPart.getContent() instanceof MimeMultipart) {
                        BodyPart obj1 = ((MimeMultipart) bodyPart.getContent()).getBodyPart(1);
                        if (obj1 != null && obj1.getContent() != null) {
                            this.rMessage = obj1.getContent().toString();
                        }
                        break;
                    } else {
                        this.rMessage = MimeUtility.decodeText(bodyPart.getContent().toString());
                    }
                    continue;
                }
                InputStream is = bodyPart.getInputStream();
                AtID = "INB" + new java.util.Date().getTime();
                String qry = "INSERT INTO MAIL_ATTACH SET " + " MAIL_ATTACHID=?," + " FILENAME=?," + " FILEBIN=?";
                ebiModule.ebiPGFactory.getIEBIDatabase().setAutoCommit(true);
                PreparedStatement prs = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement(qry);
                prs.setString(1, AtID);
                prs.setString(2, MimeUtility.decodeText(bodyPart.getFileName()));
                prs.setBlob(3, is);
                ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedStmt(prs);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
            AtID = "";
        } catch (SQLException ex) {
            ex.printStackTrace();
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
            AtID = "";
        } finally {
            ebiModule.ebiPGFactory.getIEBIDatabase().setAutoCommit(false);
        }
    }

    private void setMessage(Message m) {
        Address[] a;
        try {
            if ((a = m.getFrom()) != null) {
                for (int j = 0; j < a.length; j++) {
                    this.rFrom = MimeUtility.decodeText(a[j].toString());
                }
            }
            if ((a = m.getRecipients(Message.RecipientType.TO)) != null) {
                for (int j = 0; j < a.length; j++) {
                    this.rTo = MimeUtility.decodeText(a[j].toString());
                }
            }
            if ((a = m.getReplyTo()) != null) {
                for (int j = 0; j < a.length; j++) {
                    this.rCC = MimeUtility.decodeText(a[j].toString());
                }
            }
            this.rSubject = m.getSubject();
            this.rDate = m.getSentDate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void saveReceivedEmail(Message msg) {
        String query = "INSERT INTO MAIL_INBOX SET " + "MAIL_FROM=?," + "MAIL_TO=?," + "MAIL_CC=?," + "MAIL_SUBJECT=?," + "MAIL_MESSAGE=?," + "MAIL_DATE=?," + "SETFROM=?";
        ResultSet checkSet = null;
        try {
            ebiModule.ebiPGFactory.getIEBIDatabase().setAutoCommit(true);
            PreparedStatement ps1 = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT MAIL_FROM,MAIL_SUBJECT FROM " + "MAIL_INBOX WHERE MAIL_FROM=?" + " AND MAIL_SUBJECT=? ");
            ps1.setString(1, rFrom);
            ps1.setString(2, rSubject);
            checkSet = ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps1);
            sendMail.setInfoText(rSubject);
            checkSet.last();
            if (checkSet.getRow() <= 0) {
                try {
                    if (msg.getContent() instanceof Multipart) {
                        dumpPart((Multipart) msg.getContent());
                        if (!this.AtID.equals("")) {
                            query += ",ATTACHID=?";
                        }
                    } else {
                        this.rMessage = MimeUtility.decodeText(msg.getContent().toString());
                    }
                } catch (UnsupportedEncodingException ex) {
                    this.rMessage = convertStreamToString(msg.getInputStream(), "utf8");
                }
                PreparedStatement ps = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement(query);
                ps.setString(1, this.rFrom);
                ps.setString(2, this.rTo);
                ps.setString(3, this.rCC);
                ps.setString(4, this.rSubject);
                ps.setString(5, this.rMessage);
                ps.setDate(6, new Date(this.rDate.getTime()) == null ? new Date(new java.util.Date().getTime()) : new Date(this.rDate.getTime()));
                ps.setString(7, EBIPGFactory.ebiUser);
                if (!this.AtID.equals("")) {
                    ps.setString(8, this.AtID);
                }
                ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedStmt(ps);
            }
        } catch (java.sql.SQLException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
        } catch (Exception ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
        } finally {
            this.rMessage = "";
            ebiModule.ebiPGFactory.getIEBIDatabase().setAutoCommit(false);
            if (checkSet != null) {
                try {
                    checkSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String convertStreamToString(InputStream is, String ecoding) throws IOException {
        String str = "";
        if (is != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, ecoding));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
                str = writer.toString();
                writer.close();
            }
        }
        return str;
    }

    public void copyFromTo(int id, String fromTable, String toTable) {
        ResultSet rs = null;
        try {
            ebiModule.ebiPGFactory.getIEBIDatabase().setAutoCommit(true);
            PreparedStatement ps1 = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT * FROM " + fromTable + "  WHERE ID=?");
            ps1.setInt(1, id);
            rs = ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps1);
            rs.next();
            String sql = "INSERT INTO " + toTable + " SET " + "MAIL_DATE=?," + "SETFROM=?," + "MAIL_FROM=?," + "MAIL_TO=?," + "MAIL_CC=?," + "MAIL_SUBJECT=?," + "MAIL_MESSAGE=?," + "ATTACHID=?";
            if (toTable.equals("MAIL_ASSIGNED")) {
                sql += ", COMPANYID=?";
            }
            String abkzFrom = fromTable.substring(5, 8);
            String abkzTo = toTable.substring(5, 8);
            PreparedStatement ps = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement(sql);
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
                ps.setInt(9, ebiModule.companyID);
            }
            ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedStmt(ps);
            if (rs.getString("ATTACHID") != null) {
                String assignID = rs.getString("AttachID").replaceFirst(abkzFrom, abkzTo);
                PreparedStatement ps2 = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement("UPDATE  MAIL_ATTACH SET MAIL_ATTACHID=? where MAIL_ATTACHID=?");
                ps2.setString(1, assignID);
                ps2.setString(2, rs.getString("ATTACHID"));
                ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedStmt(ps2);
            }
            PreparedStatement ps3 = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement("DELETE FROM " + fromTable + " WHERE ID=?");
            ps3.setInt(1, id);
            ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedStmt(ps3);
        } catch (java.sql.SQLException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
        } finally {
            ebiModule.ebiPGFactory.getIEBIDatabase().setAutoCommit(false);
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void showEMailOnComposer(int id, String dbTable) {
        ResultSet rs = null;
        ResultSet set = null;
        try {
            PreparedStatement ps = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT * FROM " + dbTable + " WHERE ID=?");
            ps.setInt(1, id);
            rs = ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps);
            if (rs != null) {
                if (rs.next()) {
                    this.from = rs.getString("MAIL_FROM");
                    this.to = rs.getString("MAIL_TO");
                    this.subject = rs.getString("MAIL_SUBJECT");
                    this.cc = rs.getString("MAIL_CC");
                    emailText = rs.getString("MAIL_MESSAGE");
                    if (!emailText.toLowerCase().contains("<html")) {
                        ebiModule.getComposerMail().jEditorMessageView.setHTMLContent(emailText.replaceAll("\n", "<br>"));
                        emailText = emailText.replaceAll("\n", "<br>");
                    }
                    if (!EBICRMMailClientFunctions.showWindow) {
                        ebiModule.getComposerMail().setUnEditable(false);
                        ebiModule.getComposerMail().jComboFrom.setText(from);
                        ebiModule.getComposerMail().jTextTo.setText(to);
                        ebiModule.getComposerMail().jTextSubject.setText(subject);
                        ebiModule.getComposerMail().jTextCC.setText(cc);
                        ebiModule.getComposerMail().jEditorMessageView.setHTMLContent(emailText);
                    } else if (EBICRMMailClientFunctions.showWindow) {
                        JFrame viewEmail = new JFrame();
                        viewEmail.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        viewEmail.setSize(new Dimension(800, 700));
                        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
                        viewEmail.setLocation((((int) d.getWidth() - 800) / 2), 10);
                        viewEmail.setName("viewEmail");
                        viewEmail.setTitle(from + "  -  " + subject);
                        viewEmail.setResizable(true);
                        final JWebBrowser webBrowser = new JWebBrowser();
                        webBrowser.setBarsVisible(false);
                        webBrowser.setStatusBarVisible(true);
                        webBrowser.setHTMLContent(emailText);
                        viewEmail.setLayout(new BorderLayout());
                        viewEmail.add(webBrowser, BorderLayout.CENTER);
                        viewEmail.setVisible(true);
                        EBICRMMailClientFunctions.showWindow = false;
                    }
                    PreparedStatement ps1 = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT MAIL_ATTACHID,FILENAME FROM MAIL_ATTACH WHERE MAIL_ATTACHID=?");
                    ps1.setString(1, rs.getString("ATTACHID"));
                    set = ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps1);
                    ebiModule.getComposerMail().listModel.clear();
                    while (set.next()) {
                        ebiModule.getComposerMail().listModel.addElement(set.getString("FILENAME"));
                        ebiModule.getComposerMail().AttachID = set.getString("MAIL_ATTACHID");
                    }
                    emailText = "";
                }
            }
        } catch (java.sql.SQLException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (set != null) {
                    set.close();
                }
            } catch (Exception e) {
                EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(e)).Show(EBIMessage.ERROR_MESSAGE);
            }
        }
    }

    public int getEmailCount(String Table) {
        int ret = 0;
        ResultSet rs;
        try {
            PreparedStatement ps = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT COUNT(ID) FROM " + Table);
            rs = ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps);
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

    public int getEmailCount(int pCompanyID) {
        int ret = 0;
        ResultSet rs = null;
        try {
            PreparedStatement ps = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT COUNT(COMPANYID) FROM MAIL_ASSIGNED WHERE COMPANYID=?");
            ps.setInt(1, pCompanyID);
            rs = ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps);
            if (rs != null) {
                rs.next();
                ret = rs.getInt(1);
            }
        } catch (java.sql.SQLException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
            return ret;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
            }
        }
        return ret;
    }

    public void showEmailTypeBox(String table) {
        ResultSet rs = null;
        try {
            PreparedStatement ps = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT * FROM " + table);
            rs = ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps);
            rs.last();
            if (rs.getRow() > 0) {
                tabMod.data = new Object[rs.getRow()][7];
                rs.beforeFirst();
                int i = 0;
                while (rs.next()) {
                    tabMod.data[i][0] = rs.getString("MAIL_FROM");
                    tabMod.data[i][1] = rs.getString("MAIL_SUBJECT");
                    tabMod.data[i][2] = rs.getDate("MAIL_DATE") == null ? "" : ebiModule.ebiPGFactory.getDateToString(rs.getDate("MAIL_DATE"));
                    tabMod.data[i][3] = rs.getString("ATTACHID") == null ? EBIPGFactory.getLANG("EBI_LANG_NO") : EBIPGFactory.getLANG("EBI_LANG_YES");
                    tabMod.data[i][4] = rs.getString("MAIL_TO");
                    tabMod.data[i][5] = rs.getString("MAIL_CC");
                    tabMod.data[i][6] = rs.getString("ID");
                    i++;
                }
            } else {
                tabMod.data = new Object[][] { { EBIPGFactory.getLANG("EBI_LANG_C_NO_EMAILS"), "", "", "", "", "" } };
            }
        } catch (SQLException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
        } finally {
            tabMod.fireTableDataChanged();
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
    }

    public void showEmailTypeBox(String table, int pCompanyID) {
        ResultSet rs = null;
        try {
            PreparedStatement ps = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT * FROM " + table + " WHERE COMPANYID=?");
            ps.setInt(1, pCompanyID);
            rs = ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps);
            rs.last();
            if (rs.getRow() > 0) {
                tabMod.data = new Object[rs.getRow()][7];
                rs.beforeFirst();
                int i = 0;
                while (rs.next()) {
                    tabMod.data[i][0] = rs.getString("MAIL_FROM");
                    tabMod.data[i][1] = rs.getString("MAIL_SUBJECT");
                    tabMod.data[i][2] = rs.getString("MAIL_DATE") == null ? "" : rs.getString("MAIL_DATE");
                    tabMod.data[i][3] = rs.getString("ATTACHID") == null ? EBIPGFactory.getLANG("EBI_LANG_NO") : EBIPGFactory.getLANG("EBI_LANG_YES");
                    tabMod.data[i][4] = rs.getString("MAIL_TO");
                    tabMod.data[i][5] = rs.getString("MAIL_CC");
                    tabMod.data[i][6] = rs.getString("ID");
                    i++;
                }
            } else {
                tabMod.data = new Object[][] { { EBIPGFactory.getLANG("EBI_LANG_C_NO_EMAILS"), "", "", "", "", "" } };
            }
        } catch (SQLException ex) {
            EBIExceptionDialog.getInstance(EBIPGFactory.printStackTrace(ex)).Show(EBIMessage.ERROR_MESSAGE);
        } finally {
            tabMod.fireTableDataChanged();
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception ex) {
            }
        }
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

    public void saveEMailAttach(String attachID, String realName, String fileName, String pathToSave) {
        String FileName;
        InputStream is = null;
        OutputStream fos = null;
        ResultSet rs = null;
        try {
            ebiModule.ebiPGFactory.getIEBIDatabase().setAutoCommit(true);
            PreparedStatement ps = ebiModule.ebiPGFactory.getIEBIDatabase().initPreparedStatement("SELECT * FROM MAIL_ATTACH WHERE FILENAME=? and MAIL_ATTACHID=?");
            ps.setString(1, realName);
            ps.setString(2, attachID);
            rs = ebiModule.ebiPGFactory.getIEBIDatabase().executePreparedQuery(ps);
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
            ebiModule.ebiPGFactory.getIEBIDatabase().setAutoCommit(false);
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
