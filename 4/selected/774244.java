package com.gever.goa.dailyoffice.mailmgr.dao.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import com.gever.exception.DefaultException;
import com.gever.goa.dailyoffice.mailmgr.dao.MailConstant;
import com.gever.goa.dailyoffice.mailmgr.dao.MailMgrDAO;
import com.gever.goa.dailyoffice.mailmgr.dao.MailMgrFactory;
import com.gever.goa.dailyoffice.mailmgr.dao.Pop3MailMgrDAO;
import com.gever.goa.dailyoffice.mailmgr.vo.MailAttchVO;
import com.gever.goa.dailyoffice.mailmgr.vo.MailErrorInfoVO;
import com.gever.goa.dailyoffice.mailmgr.vo.MailVO;
import com.gever.goa.dailyoffice.mailmgr.vo.Pop3ConfigVO;
import com.gever.goa.web.util.UploadFile;
import com.gever.jdbc.BaseDAO;
import com.gever.jdbc.sqlhelper.DefaultSQLHelper;
import com.gever.jdbc.sqlhelper.SQLHelper;
import com.gever.util.IdMng;

public class Pop3MailMgrDAOImp extends BaseDAO implements Pop3MailMgrDAO {

    public Pop3MailMgrDAOImp(String dbData) {
        super(dbData);
    }

    private Store myStore;

    public ArrayList aryErrList = new ArrayList();

    public ArrayList aryResult = new ArrayList();

    private String protocol = new String("");

    private String userID = new String("");

    private String password = new String("");

    private String server = new String("");

    private String port = new String("110");

    private boolean bIsDel = true;

    private String serverPath = new String("");

    private static StringBuffer sbContent = new StringBuffer();

    private static int level = 0;

    private static boolean showmsg = true;

    private String fileName = new String("");

    private StringBuffer strBuf = new StringBuffer();

    private ArrayList attchList = new ArrayList();

    StringBuffer strBufErr = new StringBuffer();

    private UploadFile uploadFile = new UploadFile();

    private MailMgrDAO mailMgrDao = null;

    public static String getISOFileName(Part body) {
        boolean flag = true;
        String[] cdis;
        if (body != null) {
            try {
                cdis = body.getHeader("Content-Disposition");
                if (cdis == null) {
                    flag = false;
                    cdis = body.getHeader("Content-Type");
                }
            } catch (Exception e) {
                return null;
            }
            if (cdis == null) {
                return null;
            }
            if (cdis[0] == null) {
                return null;
            }
            if (flag) {
                int pos = cdis[0].indexOf("filename=");
                if (pos < 0) {
                    return null;
                }
                if (cdis[0].charAt(cdis[0].length() - 1) == '"') {
                    return cdis[0].substring(pos + 10, cdis[0].length() - 1);
                }
                return cdis[0].substring(pos + 9, cdis[0].length());
            } else {
                int pos = cdis[0].indexOf("name=");
                if (pos < 0) {
                    return null;
                }
                if (cdis[0].charAt(cdis[0].length() - 1) == '"') {
                    return cdis[0].substring(pos + 6, cdis[0].length() - 1);
                }
                return cdis[0].substring(pos + 5, cdis[0].length());
            }
        } else {
            return null;
        }
    }

    /**
	 * ����Զ���ʼ�������
	 * 
	 * @return �ɹ����,true:Ϊ�ɹ�,falseΪʧ��
	 */
    public boolean mailConnection() {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "pop3");
        props.put("mail.pop3.host", server);
        props.put("mail.pop3.port", String.valueOf(port));
        props.put("mail.pop3.auth", "true");
        try {
            PopupAuthenticator auth = new PopupAuthenticator(userID, password);
            Session mailSession = Session.getInstance(props, auth);
            Store mailStore = mailSession.getStore("pop3");
            mailStore.connect(server, Integer.parseInt(this.port), userID, password);
            this.setMyStore(mailStore);
            return true;
        } catch (javax.mail.AuthenticationFailedException e) {
            e.printStackTrace();
            return false;
        } catch (javax.mail.MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
	 * �洢�ʼ���������
	 * 
	 * @return �ɹ����
	 */
    public boolean saveMails() {
        return false;
    }

    /**
	 * �����ʼ�
	 * 
	 * @param vo
	 *            �ʼ���ӳ����
	 * @param aryAttch
	 *            ����ϸ���嵥
	 * @return �ɹ����
	 */
    public String sendMails(MailVO vo, List aryAttch) {
        return "";
    }

    /**
	 * �����ʼ�
	 * 
	 * @param mailMap
	 *            �ʼ�������Ϣ
	 * @param info
	 *            �ʼ���ӳ����
	 * @param aryList
	 *            ����ϸ���嵥
	 * @return �ɹ����
	 */
    public String sendMails(HashMap mailMap, MailVO info, ArrayList aryList) {
        Properties props = new Properties();
        String smtpServer = new String("");
        String fromEmail = new String();
        String toEmail = new String();
        String ccEmail = new String("");
        String bccEmail = new String("");
        String mailNote = new String("");
        String strRealPath = new String("");
        int iCheck = 0;
        Pop3ConfigVO configView = (Pop3ConfigVO) mailMap.get("view");
        mailNote = (String) mailMap.get("mainNote");
        boolean bCheck = false;
        protocol = "smtp";
        smtpServer = setSmtpServer(configView);
        setSmtpPort(configView);
        bCheck = configView.getSmtp_auth().equals("1");
        setSendMailProperty(props, smtpServer, bCheck);
        fromEmail = configView.getShow_address();
        toEmail = info.getReceive_address();
        ccEmail = info.getCopy_send();
        bccEmail = info.getDense_send();
        HashMap errMap = new HashMap();
        errMap.put("server", configView.getSmtp_server());
        errMap.put("from", fromEmail);
        if (bCheck) {
            userID = configView.getSmtp_name();
            password = configView.getSmtp_pwd();
        }
        try {
            Session mailSession = null;
            mailSession = Session.getInstance(props, null);
            mailSession = setMailSession(props, bCheck);
            Message msg = new MimeMessage(mailSession);
            msg.setFrom(new InternetAddress(fromEmail));
            if (!toEmail.equals("")) {
                ++iCheck;
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            }
            if (!ccEmail.equals("")) {
                ++iCheck;
                msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccEmail));
            }
            if (!bccEmail.equals("")) {
                ++iCheck;
                msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bccEmail));
            }
            if (iCheck == 0) return "";
            msg.setSentDate(new Date());
            msg.setSubject(info.getTitle());
            Multipart mult = new MimeMultipart();
            MimeBodyPart mBody = new MimeBodyPart();
            mBody.setText(mailNote);
            mult.addBodyPart(mBody);
            setAttachBodyPart(aryList, strRealPath, mult);
            msg.setContent(mult);
            Transport transport = mailSession.getTransport("smtp");
            transport.connect(smtpServer, Integer.parseInt(port), userID, password);
            transport.send(msg);
        } catch (javax.mail.SendFailedException e) {
            e.printStackTrace();
            boolean bErr = true;
            Address list[] = e.getInvalidAddresses();
            if (list != null) {
                errMap.put("list", list);
                bErr = false;
            }
            Address unlist[] = e.getValidUnsentAddresses();
            if (unlist != null) {
                errMap.put("unlist", unlist);
                bErr = false;
            }
            Address sentlist[] = e.getValidSentAddresses();
            if (sentlist != null) {
                errMap.put("sentlist", unlist);
                bErr = false;
            }
            this.procSmtpError(errMap, bErr);
        } catch (javax.mail.AuthenticationFailedException e) {
            e.printStackTrace();
            this.procSmtpError(errMap, true);
        } catch (javax.mail.MessagingException e) {
            e.printStackTrace();
            this.procSmtpError(errMap, true);
        }
        mailMap.put("errorMap", errMap);
        return this.strBufErr.toString();
    }

    /**
	 * ɾ��Զ�̷��������ʼ�
	 * 
	 * @param aryList
	 *            pop3�ʼ��ʺ��嵥
	 * @return �ɹ���� :trueΪ��,falseΪ��
	 */
    public List deleteMails(List aryList) {
        this.procEmails((ArrayList) aryList, MailConstant.DELETE_ACTION);
        return this.aryResult;
    }

    /**
	 * ͳ��Զ�̷��������ʼ�����
	 * 
	 * @param aryList
	 *            pop3�ʼ��ʺ��嵥
	 * @return �ɹ���� :trueΪ��,falseΪ��
	 */
    public List countEmails(List aryList) {
        procEmails((ArrayList) aryList, MailConstant.COUNT_ACTION);
        return this.aryResult;
    }

    /**
	 * ����������ʼ������ʼ�(����ɾ��,ͳ���ʼ�)
	 * 
	 * @param aryList
	 *            ���е�pop3���ʺ�
	 * @param action
	 *            ��������
	 * @return �ɹ����,trueΪ��,falseΪ��
	 */
    public boolean procEmails(ArrayList aryList, int action) {
        boolean bIsRead = true;
        int iCount = 0;
        aryResult = new ArrayList();
        for (int idx = 0; idx < aryList.size(); idx++) {
            Pop3ConfigVO popView = new Pop3ConfigVO();
            HashMap resultMap = new HashMap();
            popView = (Pop3ConfigVO) aryList.get(idx);
            this.userID = popView.getPop3_account();
            this.protocol = "pop3";
            this.password = popView.getPop3_pwd();
            this.server = popView.getPop3_address();
            server = this.setPop3Server(popView);
            this.setPop3Port(popView);
            try {
                if (this.mailConnection() == false) {
                    resultMap.put("ID", String.valueOf(idx));
                    resultMap.put("POP3SERVER", this.server);
                    resultMap.put("COUNT", "����ʧ��");
                    aryResult.add(resultMap);
                    continue;
                }
                Folder folder = this.myStore.getDefaultFolder();
                if (folder == null) {
                    continue;
                }
                Folder popFolder = folder.getFolder("INBOX");
                bIsRead = openPopFolder(popFolder);
                switch(action) {
                    case MailConstant.DELETE_ACTION:
                        Message[] msgList = popFolder.getMessages();
                        if (bIsRead == true) {
                            for (int iNext = 0; iNext < msgList.length; iNext++) {
                                msgList[iNext].setFlag(Flags.Flag.DELETED, true);
                            }
                        }
                        iCount = popFolder.getMessageCount();
                        resultMap.put("ID", String.valueOf(idx));
                        resultMap.put("POP3SERVER", this.server);
                        resultMap.put("COUNT", (bIsRead == false) ? "������ɾ��" : String.valueOf(iCount));
                        aryResult.add(resultMap);
                        popFolder.close(true);
                        break;
                    case MailConstant.COUNT_ACTION:
                        iCount = popFolder.getMessageCount();
                        resultMap.put("ID", String.valueOf(idx));
                        resultMap.put("POP3SERVER", this.server);
                        resultMap.put("COUNT", String.valueOf(iCount));
                        aryResult.add(resultMap);
                        popFolder.close(false);
                        break;
                    default:
                        break;
                }
                this.myStore.close();
            } catch (MessagingException e) {
                e.printStackTrace();
                resultMap.put("ID", String.valueOf(idx));
                resultMap.put("POP3SERVER", this.server);
                resultMap.put("COUNT", "�������");
                aryResult.add(resultMap);
                continue;
            }
        }
        return true;
    }

    private ArrayList initProcEmailTestInfo(ArrayList aryList) {
        aryList = new ArrayList();
        Pop3ConfigVO testConfigVo = new Pop3ConfigVO();
        testConfigVo.setPop3_account("test");
        testConfigVo.setPop3_address("YFS007:110");
        testConfigVo.setPop3_name("test@YFS007");
        testConfigVo.setPop3_pwd("test");
        testConfigVo.setShow_address("test@YFS007");
        testConfigVo.setSmtp_server("YFS007:25");
        testConfigVo.setSmtp_auth("1");
        testConfigVo.setSmtp_name("test");
        testConfigVo.setSmtp_pwd("test");
        aryList.add(testConfigVo);
        return aryList;
    }

    /**
	 * ���ط������ʼ������ʼ�
	 * 
	 * @param aryList
	 *            ���е�pop3���ʺ�
	 * @return ���ش�����ʾ��Ϣ
	 */
    public String downLoadEmails(List aryList) {
        aryResult = new ArrayList();
        boolean bErr = false;
        for (int idx = 0; idx < aryList.size(); idx++) {
            HashMap errMap = new HashMap();
            Pop3ConfigVO popView = (Pop3ConfigVO) aryList.get(idx);
            this.userID = popView.getPop3_account();
            this.protocol = "pop3";
            this.password = popView.getPop3_pwd();
            server = setPop3Server(popView);
            setPop3Port(popView);
            if ("1".equals(popView.getDel_flag())) {
                bIsDel = false;
            }
            errMap.put("ID", String.valueOf(idx));
            errMap.put("POP3SERVER", this.server);
            errMap.put("NAME", popView.getPop3_name());
            bErr = true;
            if (this.mailConnection() == false) {
                errMap.put("Connection", "�ʼ�����ʧ��");
                bErr = false;
                procPop3Error(errMap, bErr);
                continue;
            }
            errMap.put("Connection", "�ɹ����ӵ�POP3");
            try {
                Folder folder = this.myStore.getDefaultFolder();
                if (folder == null) {
                    errMap.put("ERROR", "û���ʼ��ļ���");
                    bErr = false;
                    procPop3Error(errMap, bErr);
                    continue;
                }
                System.out.println("--- message count in folder is " + folder.getMessageCount());
                Folder popFolder = folder.getFolder("INBOX");
                this.openPopFolder(popFolder);
                errMap.put("Message", String.valueOf(popFolder.getMessageCount()));
                insertMail(popFolder.getMessages(), popView.getIncept_mail_dir(), popView.getUser_code(), errMap);
                popFolder.close(true);
                this.myStore.close();
                procPop3Error(errMap, bErr);
            } catch (MessagingException e) {
                e.printStackTrace();
                continue;
            } catch (DefaultException e) {
                e.printStackTrace();
            }
        }
        return strBufErr.toString();
    }

    private List initDownLoadTestInfo(List aryList) {
        aryList = new ArrayList();
        Pop3ConfigVO testConfigVo = new Pop3ConfigVO();
        testConfigVo.setPop3_account("test");
        testConfigVo.setPop3_address("YFS007:110");
        testConfigVo.setPop3_name("test@YFS007");
        testConfigVo.setPop3_pwd("test");
        testConfigVo.setShow_address("test@YFS007");
        testConfigVo.setSmtp_server("YFS007:25");
        testConfigVo.setSmtp_auth("1");
        testConfigVo.setSmtp_name("test");
        testConfigVo.setSmtp_pwd("test");
        testConfigVo.setIncept_mail_dir(MailConstant.DIR_RECIEVE_FOLDER);
        testConfigVo.setUser_code("1");
        aryList.add(testConfigVo);
        return aryList;
    }

    /**
	 * �������ʼ�
	 * 
	 * @param listMsg
	 *            �����ʼ��嵥
	 * @param strMailType
	 *            �浽�ĸ��ʼ�λ��
	 * @param iUserID
	 *            ��ǰ���û���
	 * @param hMap
	 *            ����ӳ����
	 * @return �ɹ��� true Ϊ��,false Ϊ��
	 * @throws MessagingException
	 * @throws GeneralException
	 */
    private boolean insertMail(Message[] listMsg, String mailDirId, String strUserID, HashMap hMap) throws MessagingException, DefaultException {
        int badMailCount = 0;
        int goodMailCount = 0;
        ArrayList aryErr = new ArrayList();
        SQLHelper helper = new DefaultSQLHelper(super.dbData);
        helper.setAutoClose(false);
        setMailMgrDao();
        try {
            helper.begin();
            for (int iNext = 0; iNext < listMsg.length; iNext++) {
                Message tempMessage = listMsg[iNext];
                MailVO recievedPop3MailInfo = initRecievedPop3MailInfo(mailDirId, strUserID, tempMessage);
                MailErrorInfoVO errorInfo = new MailErrorInfoVO();
                mailMgrDao.isSavaOwnFileOutOfCapacity(recievedPop3MailInfo, errorInfo);
                String strRet = toString(errorInfo.getErrorMsg());
                if (strRet.length() > 0) {
                    badMailCount++;
                    strRet = "<BR>&nbsp;&nbsp;&nbsp;&nbsp;<font color=red>����\"" + (toString(recievedPop3MailInfo.getTitle()).equals("") ? "û������" : recievedPop3MailInfo.getTitle()) + "\"���ʼ�ʱ,����ʼ��ռ䲻��</font>";
                    aryErr.add(strRet);
                    continue;
                }
                try {
                    sbContent.setLength(0);
                    level = 0;
                    userId = recievedPop3MailInfo.getUser_code();
                    dumpPart(tempMessage);
                    recievedPop3MailInfo.setContent(sbContent.toString());
                    System.out.println(" note--------> " + sbContent.toString());
                    mailMgrDao.setAttachList(this.attchList);
                    if (mailMgrDao.insertMail(recievedPop3MailInfo, userId, helper) == false) {
                        ++badMailCount;
                    }
                    this.setAttchList(new ArrayList());
                    System.out.println("--------DELETED---------" + Flags.Flag.DELETED + " isdel = " + this.bIsDel);
                    if (bIsDel) tempMessage.setFlag(Flags.Flag.DELETED, true);
                    ++goodMailCount;
                } catch (IOException e) {
                    ++badMailCount;
                    e.printStackTrace();
                } catch (MessagingException e) {
                    ++badMailCount;
                    e.printStackTrace();
                }
                helper.commit();
                recievedPop3MailInfo = null;
            }
        } catch (DefaultException ex) {
            ex.printStackTrace();
            helper.rollback();
        } finally {
            helper.end();
        }
        hMap.put("nosize", aryErr);
        hMap.put("goodMails", String.valueOf(goodMailCount));
        hMap.put("badMails", String.valueOf(badMailCount));
        return false;
    }

    private void setPop3Port(Pop3ConfigVO popView) {
        this.port = popView.getPop3_address().substring(popView.getPop3_address().indexOf(":") + 1);
    }

    private String setPop3Server(Pop3ConfigVO popView) {
        String pop3Server = "";
        pop3Server = popView.getPop3_address().substring(0, popView.getPop3_address().indexOf(":"));
        return pop3Server;
    }

    private boolean openPopFolder(Folder popFolder) throws MessagingException {
        boolean bIsRead = true;
        try {
            popFolder.open(Folder.READ_WRITE);
        } catch (MessagingException ex) {
            popFolder.open(Folder.READ_ONLY);
            bIsRead = false;
        }
        return bIsRead;
    }

    private void setMailMgrDao() {
        this.mailMgrDao = MailMgrFactory.getInstance().createMailMgr(super.dbData);
    }

    private MailVO initRecievedPop3MailInfo(String mailDirId, String strUserID, Message tempMessage) throws MessagingException {
        MailVO recievedPop3MailInfo = new MailVO();
        recievedPop3MailInfo.setMail_dir_id(mailDirId);
        recievedPop3MailInfo.setMail_size(String.valueOf(tempMessage.getSize()));
        recievedPop3MailInfo.setMail_type(String.valueOf(MailConstant.POP3_MAIL));
        recievedPop3MailInfo.setRe_flag("0");
        recievedPop3MailInfo.setRead_flag("0");
        recievedPop3MailInfo.setUser_code(strUserID);
        recievedPop3MailInfo.setReceive_address(String.valueOf(recievedPop3MailInfo.getUser_code()));
        try {
            recievedPop3MailInfo.setTitle(MimeUtility.decodeText(tempMessage.getSubject()));
        } catch (java.io.UnsupportedEncodingException e) {
        }
        setPop3MailRelatedAddress(recievedPop3MailInfo, tempMessage);
        try {
            setPop3MailSendDate(recievedPop3MailInfo, tempMessage);
        } catch (Exception xe) {
        }
        return recievedPop3MailInfo;
    }

    private void setPop3MailRelatedAddress(MailVO info, Message tempMessage) throws MessagingException {
        Address[] fromList = tempMessage.getFrom();
        if (fromList != null) {
            info.setPost_address(((InternetAddress) fromList[0]).getAddress());
            info.setPost_username(((InternetAddress) fromList[0]).getAddress());
        }
        Address[] ccList = tempMessage.getRecipients(Message.RecipientType.CC);
        if ((ccList != null) && ccList.length > 0) {
            info.setCopy_send(((InternetAddress) ccList[0]).getAddress());
        }
        Address[] bccList = tempMessage.getRecipients(Message.RecipientType.BCC);
        if ((bccList != null) && bccList.length > 0) {
            info.setDense_send(((InternetAddress) bccList[0]).getAddress());
        }
    }

    private void setPop3MailSendDate(MailVO info, Message tempMessage) throws MessagingException {
        Date date = tempMessage.getSentDate();
        System.out.println("-------");
        java.util.Calendar now = java.util.Calendar.getInstance();
        if (tempMessage.getSentDate() != null) now.setTime(tempMessage.getSentDate());
        int iMonth = date.getMonth();
        int iDate = date.getDate();
        String month = setNumGreaterThanNine(iMonth);
        String dateNum = setNumGreaterThanNine(iDate);
        StringBuffer strDate = new StringBuffer();
        strDate.setLength(0);
        strDate.append(1900 + date.getYear()).append("-");
        strDate.append(month).append("-").append(dateNum);
        strDate.append(" ").append(date.getHours()).append(":");
        strDate.append(date.getMinutes()).append(":").append(date.getSeconds());
        info.setSend_date(strDate.toString());
    }

    private String setNumGreaterThanNine(int iMonth) {
        String month = (iMonth < 9) ? "0" + String.valueOf(iMonth + 1) : String.valueOf(iMonth + 1);
        return month;
    }

    /**
	 * �����ʼ�:�÷������õݹ��㷨���������ʼ��Ŀ�֮��Ĺ�ϵ
	 * 
	 * @param p
	 *            �����ʼ��ӿ���(��MimeMessage,MultiPart����ʵ������ӿ�)
	 * @throws MessagingException
	 * @throws IOException
	 */
    public void dumpPart(Part p) throws MessagingException, IOException {
        String ct = p.getContentType();
        String strFile = new String();
        if ((p.getFileName() != null)) {
            if (!p.getFileName().equals("")) {
                this.setFileName(MimeUtility.decodeText(p.getFileName()));
            }
        }
        Object content = p.getContent();
        String strValue = new String("");
        if (p.isMimeType("text/plain")) {
            System.out.println("���� Asccii���ʽ");
            System.out.println("---------------------------");
            String disp = toString(p.getDisposition());
            if (!disp.equals(Part.ATTACHMENT)) {
                strValue = MimeUtility.decodeText((String) p.getContent());
                sbContent.append(strValue);
            }
            strValue = null;
            disp = null;
        } else if (p.isMimeType("text/html")) {
            System.out.println("����  html ��ʽ");
            System.out.println("---------------------------");
            String disp = toString(p.getDisposition());
            if (!disp.equals(Part.ATTACHMENT)) {
                strValue = MimeUtility.decodeText((String) p.getContent());
                sbContent.append(strValue);
            }
            strValue = null;
            disp = null;
        } else if (p.isMimeType("multipart/*")) {
            System.out.println("---���ڶ������ʼ�-----------");
            System.out.println("---------------------------");
            Multipart mp = (Multipart) p.getContent();
            level++;
            int count = mp.getCount();
            for (int i = 0; i < count; i++) dumpPart(mp.getBodyPart(i));
            level--;
        } else if (p.isMimeType("message/rfc822")) {
            level++;
            System.out.println("This is a Nested Message");
            System.out.println("----------- ����InternetЭ�� ----------------");
            dumpPart((Part) p.getContent());
            level--;
        } else {
            Object o = p.getContent();
            if (o instanceof String) {
                System.out.println("���ִ���");
                System.out.println("------------ String ---------------");
                sbContent.append((String) o);
            } else {
                System.out.println("���ڲ�֪��������");
                System.out.println("---------- unkown type-----------------");
                pr(o.toString());
            }
        }
        if (level != 0 && !p.isMimeType("multipart/*")) {
            String disp = p.getDisposition();
            System.out.println("------> " + level + "  disp---> " + disp);
            if (disp != null && (disp.equalsIgnoreCase(Part.ATTACHMENT) || disp.equals(Part.INLINE))) {
                try {
                    System.out.println("�Ǹ���---------------------------");
                    strBuf.setLength(0);
                    strBuf.append(serverPath).append(uploadFile.getDir()).append("\\mail_web\\");
                    if (this.getFileName() == null) this.setFileName("����");
                    String fileDirName = strBuf.toString();
                    System.out.println("--- file dir is " + fileDirName);
                    uploadFile.createDirtory(fileDirName);
                    String realFileName = IdMng.getModuleID(String.valueOf(userId)) + this.getExtName(this.getFileName());
                    strBuf.append(realFileName);
                    String filePathName = strBuf.toString();
                    File file = new File(filePathName);
                    if (file.exists()) throw new IOException("�ļ��Ѵ���");
                    OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                    InputStream is = p.getInputStream();
                    int c;
                    while ((c = is.read()) != -1) os.write(c);
                    os.close();
                    String insertFilePath = uploadFile.getDir() + "mail_web/" + realFileName;
                    this.addAttchList(this.getFileName(), insertFilePath);
                } catch (IOException ex) {
                    System.out.println("------�洢����ʧ��--------: " + ex);
                }
                System.out.println("---------------------------");
            }
        }
    }

    private void addAttachFile(Part p) throws MessagingException {
        String disp = p.getDisposition();
        System.out.println("------> " + level + "  disp---> " + disp);
        int i = 0;
        if (disp != null && (disp.equalsIgnoreCase(Part.ATTACHMENT) || disp.equals(Part.INLINE))) {
            uploadOneAttachFile(p);
            System.out.println("-----------------" + i + "----------");
            i++;
        }
    }

    private void addHtmlMessage(Part p) throws UnsupportedEncodingException, IOException, MessagingException {
        String disp = toString(p.getDisposition());
        String strValue = new String("");
        if (!disp.equals(Part.ATTACHMENT)) {
            strValue = MimeUtility.decodeText((String) p.getContent());
            sbContent.append(strValue);
        }
    }

    private void addAscciiMessage(Part p) throws UnsupportedEncodingException, IOException, MessagingException {
        System.out.println("���� Asccii���ʽ");
        System.out.println("---------------------------");
        String disp = toString(p.getDisposition());
        String strValue = new String("");
        if (!disp.equals(Part.ATTACHMENT)) {
            strValue = MimeUtility.decodeText((String) p.getContent());
            sbContent.append(strValue);
        }
    }

    private void setFileNameThroughPart(Part p) throws UnsupportedEncodingException, MessagingException {
        if ((p.getFileName() != null)) {
            if (!p.getFileName().equals("")) {
                this.setFileName(MimeUtility.decodeText(p.getFileName()));
            }
        }
    }

    private void uploadOneAttachFile(Part p) throws MessagingException {
        try {
            System.out.println("�Ǹ���---------------------------");
            if (this.getFileName() == null) {
                this.setFileName("����");
            }
            strBuf.setLength(0);
            strBuf.append(serverPath).append(uploadFile.getDir()).append("\\mail_web\\");
            String fileDirName = strBuf.toString();
            System.out.println("--- file dir is " + fileDirName);
            System.out.println("--------------gggggggggggggggg----------");
            uploadFile.createDirtory(fileDirName);
            String realFileName = IdMng.getModuleID(String.valueOf(userId)) + this.getExtName(this.getFileName());
            strBuf.append(realFileName);
            String filePathName = strBuf.toString();
            uploadAttachFileToServer(p, filePathName);
            System.out.println("--------------fffffffffffffffffffff-----------------");
            System.out.println("--- dir in upload file : " + uploadFile.getDir());
            String insertFilePath = uploadFile.getDir() + "\\mail_web\\" + realFileName;
            this.addAttchList(this.getFileName(), insertFilePath);
        } catch (IOException ex) {
        }
    }

    private void uploadAttachFileToServer(Part p, String filePathName) throws MessagingException, IOException {
        File file = new File(filePathName);
        if (file.exists()) throw new IOException("�ļ��Ѵ���");
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        InputStream is = p.getInputStream();
        int c;
        while ((c = is.read()) != -1) os.write(c);
        os.close();
    }

    private Session setMailSession(Properties props, boolean bCheck) {
        Session mailSession;
        if (bCheck) {
            PopupAuthenticator auth = new PopupAuthenticator(userID, password);
            mailSession = Session.getInstance(props, auth);
        } else {
            mailSession = Session.getInstance(props, null);
        }
        return mailSession;
    }

    private void setSendMailProperty(Properties props, String smtpServer, boolean bCheck) {
        props.put("mail.transport.protocol", protocol);
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.smtp.port", String.valueOf(this.port));
        if (bCheck) {
            props.put("mail.smtp.auth", "true");
        }
    }

    private void setSmtpPort(Pop3ConfigVO configView) {
        this.port = configView.getSmtp_server().substring(configView.getSmtp_server().indexOf(":") + 1);
    }

    private String setSmtpServer(Pop3ConfigVO configView) {
        String smtpServer;
        smtpServer = configView.getSmtp_server().substring(0, configView.getSmtp_server().indexOf(":"));
        return smtpServer;
    }

    private void setAttachBodyPart(ArrayList aryList, String strRealPath, Multipart mult) throws MessagingException {
        for (int idx = 0; idx < aryList.size(); idx++) {
            MailAttchVO view = (MailAttchVO) aryList.get(idx);
            strRealPath = serverPath + view.getFile_path();
            System.out.println("--- file path : " + strRealPath);
            MimeBodyPart mAttch = new MimeBodyPart();
            mAttch.setDisposition(Part.ATTACHMENT);
            FileDataSource fds = new FileDataSource(strRealPath);
            mAttch.setDataHandler(new DataHandler(fds));
            try {
                mAttch.setFileName(view.getAttch_name());
            } catch (Exception e) {
                e.printStackTrace();
            }
            mult.addBodyPart(mAttch);
        }
    }

    /**
	 * ������
	 * 
	 * @param errMap
	 *            ����ӳ�� ��
	 * @param bErr
	 *            �Ƿ���ȷ
	 */
    private void procSmtpError(HashMap errMap, boolean bErr) {
        if (bErr == true) {
            strBufErr.append("&nbsp;&nbsp;&nbsp;&nbsp; ���ӷ���������");
            strBufErr.append("<BR><BR>");
        }
        Address[] list = (Address[]) errMap.get("list");
        if (list != null) {
            for (int idx = 0; idx < list.length; idx++) {
                strBufErr.append("&nbsp;&nbsp;&nbsp;&nbsp; ��Ч���ʼ���ַ").append(list[idx]).append("<BR>");
                bErr = false;
            }
        }
        list = (Address[]) errMap.get("unlist");
        if (list != null) {
            for (int idx = 0; idx < list.length; idx++) {
                strBufErr.append("&nbsp;&nbsp;&nbsp;&nbsp; ��Ч���ʼ���ַ").append(list[idx]).append("<BR>");
                bErr = false;
            }
        }
        list = (Address[]) errMap.get("sentlist");
        if (list != null) {
            for (int idx = 0; idx < list.length; idx++) {
                strBufErr.append("&nbsp;&nbsp;&nbsp;&nbsp; ��Ч���ʼ���ַ").append(list[idx]).append("<BR>");
                bErr = false;
            }
        }
    }

    private void procPop3Error(HashMap errMap, boolean bErr) {
        strBufErr.append("  �ʺ����:").append(((String) errMap.get("NAME")));
        strBufErr.append("&nbsp;&nbsp;&nbsp;&nbsp;  POP3�ʼ�������:").append(((String) errMap.get("POP3SERVER")));
        strBufErr.append("&nbsp;&nbsp;&nbsp;&nbsp; ����״��:").append(((String) errMap.get("Connection")));
        strBufErr.append("<BR>");
        if (bErr == true) {
            strBufErr.append("&nbsp;&nbsp;&nbsp;&nbsp;  �ʼ�����:").append(((String) errMap.get("Message")));
            strBufErr.append("&nbsp;&nbsp;&nbsp;&nbsp;  ���ճɹ��ʼ�����:").append(((String) errMap.get("goodMails")));
            strBufErr.append("&nbsp;&nbsp;&nbsp;&nbsp;  ����ʧ���ʼ�����:<font color=red>").append(((String) errMap.get("badMails"))).append("</font>");
            ArrayList aryErr = new ArrayList();
            aryErr = (ArrayList) errMap.get("nosize");
            for (int idx = 0; idx < aryErr.size(); idx++) {
                strBufErr.append("&nbsp;&nbsp;&nbsp;&nbsp;").append((String) aryErr.get(idx));
            }
        }
        strBufErr.append("<BR>");
        strBufErr.append("<BR>");
    }

    /**
	 * �õ��ļ���չ��
	 * 
	 * @param strFileName
	 *            �ļ�����
	 * @return ��չ��
	 */
    private String getExtName(String strFileName) {
        if (strFileName == null) return "";
        int iPos = 0;
        iPos = strFileName.lastIndexOf(".");
        if (iPos < 0) return ""; else return strFileName.substring(iPos);
    }

    /**
	 * �ȴ浽�Ѽ���
	 * 
	 * @param strFileName
	 *            �ļ���
	 * @param strFilePath
	 *            �ļ�·��
	 */
    private void addAttchList(String strFileName, String strFilePath) {
        MailAttchVO attachVO = new MailAttchVO();
        System.out.println("--- addAttchList file path : " + strFilePath);
        attachVO.setFile_path(strFilePath);
        attachVO.setAttch_name(strFileName);
        attchList.add(attachVO);
    }

    /**
	 * ��gbk�ַ�תΪunicode�ַ�
	 * 
	 * @param s
	 *            Դ�ִ�
	 * @return unicode���ִ�
	 */
    public static String toUnicode(String s) {
        String v = null;
        if (s != null) {
            try {
                byte[] bytes = s.getBytes("ISO-8859-1");
                v = new String(bytes, "gb2312");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return v;
    }

    /**
	 * nullת�ɿ��ִ�
	 * 
	 * @param strValue
	 *            ԭ�ַ�
	 * @return �ִ�
	 */
    private String toString(String strValue) {
        return ((strValue == null) ? "" : (String) strValue);
    }

    /**
	 * <p>
	 * Title:��֤����
	 * </p>
	 * <p>
	 * Description:����ʱ����
	 * </p>
	 * <p>
	 * Copyright: Copyright (c) 2003
	 * </p>
	 * <p>
	 * Company: GEVER
	 * </p>
	 * 
	 * @author Hu.Walker
	 * @version 0.9
	 */
    class PopupAuthenticator extends Authenticator {

        private String m_username = null;

        private String m_userpass = null;

        public void setUsername(String username) {
            m_username = username;
        }

        public void setUserpass(String userpass) {
            m_userpass = userpass;
        }

        public PopupAuthenticator(String user, String pass) {
            super();
            setUsername(user);
            setUserpass(pass);
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(m_username, m_userpass);
        }
    }

    static String indentStr = "                                            ";

    private String userId;

    public static void pr(String s) {
        if (showmsg) System.out.println(indentStr.substring(0, level * 2) + s);
    }

    private void setBIsDel(Pop3ConfigVO popView) {
        this.bIsDel = ("1".equals(popView.getDel_flag()) ? false : true);
        System.out.println("-------del ---bIsDel--" + bIsDel + "---popView.getDel_flag()= " + popView.getDel_flag());
    }

    public ArrayList getAryErrList() {
        return aryErrList;
    }

    public ArrayList getAryResult() {
        return aryResult;
    }

    public ArrayList getAttchList() {
        return attchList;
    }

    public boolean isBIsDel() {
        return bIsDel;
    }

    private String getFileName() {
        return fileName;
    }

    public int getLevel() {
        return level;
    }

    public Store getMyStore() {
        return myStore;
    }

    public String getPassword() {
        return password;
    }

    public String getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public StringBuffer getSbContent() {
        return sbContent;
    }

    public String getServer() {
        return server;
    }

    public String getServerPath() {
        return serverPath;
    }

    public boolean isShowmsg() {
        return showmsg;
    }

    public StringBuffer getStrBuf() {
        return strBuf;
    }

    public StringBuffer getStrBufErr() {
        return strBufErr;
    }

    public void setAryErrList(ArrayList aryErrList) {
        this.aryErrList = aryErrList;
    }

    public void setAryResult(ArrayList aryResult) {
        this.aryResult = aryResult;
    }

    public void setAttchList(ArrayList attchList) {
        this.attchList = attchList;
    }

    public void setBIsDel(boolean bIsDel) {
        this.bIsDel = bIsDel;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setMyStore(Store myStore) {
        this.myStore = myStore;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setSbContent(StringBuffer sbContent) {
        this.sbContent = sbContent;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setServerPath(String serverPath) {
        this.serverPath = serverPath;
    }

    public void setShowmsg(boolean showmsg) {
        this.showmsg = showmsg;
    }

    public void setStrBuf(StringBuffer strBuf) {
        this.strBuf = strBuf;
    }

    public void setStrBufErr(StringBuffer strBufErr) {
        this.strBufErr = strBufErr;
    }
}
