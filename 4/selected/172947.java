package ces.coffice.webmail.mailmodel.mail;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Random;
import javax.servlet.http.HttpServletResponse;
import ces.coffice.webmail.datamodel.dao.hibernate.AffixDaoHibernate;
import ces.coffice.webmail.datamodel.vo.MailAffix;
import ces.coffice.webmail.util.MemoryConstant;
import ces.coffice.webmail.util.PersonalConfig;
import ces.coral.lang.StringUtil;

/**
 * ��Ҫʵ���ʼ������Ӳ���ϵĲ���
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: �Ϻ�������Ϣ��չ���޹�˾</p>
 * @author ������
 * @@version 1.0.2004.0829
 */
public class MailData {

    /**
     * �û�ID
     */
    private long userId;

    private String userUrl;

    private String userDir;

    private final String MAILDIR = "mailbox";

    public final String RESFILENAME = "mail.properties";

    public String ENCODE = "gb2312";

    private final String MAIL_FLAG_TRUE = "1";

    public final String MAIL_FLAG_TO = "mail.to";

    public final String MAIL_FLAG_CC = "mail.cc";

    public final String MAIL_FLAG_BCC = "mail.bcc";

    private final String MAIL_FLAG_ALIAS = "mail.alias";

    private final String MAIL_FOLDER = "mailbox";

    private final String MAIL_BODY_FILE = ".html";

    private ces.coral.log.Logger log = new ces.coral.log.Logger(MailData.class);

    public String inputEncode = "gb2312";

    private String judgeFile = null;

    private String garbageFile;

    /**
     * ���������userid
     * @param userid long
     */
    public MailData(long userid) {
        this.userId = userid;
        PersonalConfig config = MemoryConstant.getInstance().getPersonalConfig(new Long(userid));
        this.ENCODE = config.getMailEncode();
        this.inputEncode = config.getInputEncode();
        this.userDir = config.getUserDir();
        this.userUrl = config.getTreeUrl();
        this.judgeFile = config.getJudgeFile();
        this.garbageFile = config.getGarbageFile();
    }

    /**
     * @return ���ʼ�ʱ�ж��ռ��Ƿ����յ�·��
     */
    public String getJudgeFile() {
        return this.judgeFile;
    }

    public String getGarbageFile() {
        return garbageFile;
    }

    /**
     *
     * @param judgeFile ���ʼ�ʱ�ж��ռ��Ƿ����յ�·��
     */
    public void setJudgeFile(String judgeFile) {
        this.judgeFile = judgeFile;
    }

    public void setGarbageFile(String garbageFile) {
        this.garbageFile = garbageFile;
    }

    /**
     * �õ��������������뱣���ڱ���Ӳ��
     * @return File
     */
    public File getAffixFile(MailAffix affix) {
        File file = new File(this.getResDir(affix.getMailId()) + affix.getAttachAlias());
        if (file.exists()) return file;
        return null;
    }

    /**
     * ��Ӳ���ϲ�ѯ�ʼ����
     */
    public DBMailModel initMailData(DBMailModel mail) throws Exception {
        if (mail == null) return null;
        if (mail.getID() == 0) return mail;
        mail.setHtmlBody(this.readBody(mail.getID(), mail.getContent()));
        return mail;
    }

    /**
     * �����ʼ�
     * @param mail DBMailModel
     * @throws Exception
     * @return DBMailModel
     */
    public void copyMail(long mailId1, DBMailModel mail) throws Exception {
        this.copyContent(mailId1, mail.getID());
        this.copyRes(mailId1, mail.getID());
    }

    /**
     * �ƶ��ʼ�
     * @param mailId1 long
     * @param mail DBMailModel
     * @throws Exception
     */
    public void moveMail(long mailId1, DBMailModel mail) throws Exception {
        this.moveRes(mailId1, mail.getID());
    }

    /**
     * ��ȡ�ռ��� ��
     */
    public DBMailModel readToCcBcc(DBMailModel mail) throws Exception {
        if (mail == null) return mail;
        if (mail.getID() == 0) return mail;
        File file = new File(this.getMailDir(mail.getID()) + "/" + this.RESFILENAME);
        this.recursiveDir(file.getParentFile());
        Properties properties = null;
        if (file.exists()) {
            FileInputStream in = new FileInputStream(file);
            properties = this.load(in);
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (properties != null) {
            String to = (String) properties.get(this.MAIL_FLAG_TO);
            String cc = (String) properties.get(this.MAIL_FLAG_CC);
            String bcc = (String) properties.get(this.MAIL_FLAG_BCC);
            mail.setMailTo(to == null ? "" : new String(to.getBytes(), this.ENCODE));
            mail.setCc(cc == null ? "" : new String(cc.getBytes(), this.ENCODE));
            mail.setBcc(bcc == null ? "" : new String(bcc.getBytes(), this.ENCODE));
        }
        return mail;
    }

    /**
     * ��ȡ����
     */
    public String readBody(long mailId, String content) throws Exception {
        File file = new File(this.getMailDir(mailId) + content);
        this.recursiveDir(file.getParentFile());
        if (file.exists()) {
            InputStream in = new FileInputStream(file);
            byte[] body = new byte[in.available()];
            in.read(body);
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return new String(body, this.ENCODE);
        }
        return "";
    }

    /**
     * �õ��ʼ��ļ�Ŀ¼
     */
    public String getMailDir(long mailId) {
        return this.userDir + "/" + this.MAILDIR + "/" + String.valueOf(mailId) + "/";
    }

    /**
     * �õ���Դ�ļ�Ŀ¼
     * @param mailId long
     * @return String
     */
    public String getResDir(long mailId) {
        return this.userDir + "/" + this.MAILDIR + "/" + String.valueOf(mailId) + "/" + "res" + "/";
    }

    /**
     * ɾ���ʼ���������
     * @param mailId long
     * @return String
     */
    public void delMail(long mailId) {
        File file = new File(this.getMailDir(mailId));
        deltree(file);
    }

    /**
     * �����ʼ���Ӳ��
     */
    public void saveMail(DBMailModel mail) throws Exception {
        this.saveMailRes(mail);
        this.saveAffix(mail);
        this.saveContent(mail);
        this.saveToCcBcc(mail);
    }

    /**
     * ���渽��
     * @param mail DBMailModel
     * @throws Exception
     */
    public void saveAffix(DBMailModel mail) throws Exception {
        if (mail == null) return;
        for (int i = 0; i < mail.getAffixCount(); i++) {
            this.saveAffix(mail.getAffix(i), mail.getID());
        }
    }

    /**
     * �����������õ�����Դ
     * @param body String
     */
    public void saveMailRes(DBMailModel mail) {
        if (mail == null) return;
        if (mail.getHtmlBody() == null) mail.setHtmlBody("");
    }

    /**
     * ���ʼ�ʱ�����������
     * @throws Exception
     */
    public void mailGetRightBody(DBMailModel mail) {
        if (mail == null) return;
        if (mail.getHtmlBody() == null) mail.setHtmlBody("");
        for (int i = 0; i < mail.getAffixCount(); i++) {
            MailAffix o = mail.getAffix(i);
            if (o instanceof AffixModle) {
                AffixModle affix = null;
                affix = (AffixModle) o;
                if (this.MAIL_FLAG_TRUE.equals(affix.getFlag())) {
                    if (affix.getContent_ID() != null && affix.getContent_ID().length() > 0) {
                        mail.setHtmlBody(StringUtil.replaceAll(mail.getHtmlBody(), "cid:" + affix.getContent_ID(), "res/" + affix.getAttachAlias()));
                    }
                }
            }
        }
    }

    /**
     * ��������
     * @param body String
     */
    public void saveContent(DBMailModel mail) throws Exception {
        this.mailGetRightBody(mail);
        if (mail == null) return;
        if (mail.getID() == 0) return;
        File file = null;
        if (!(mail.getContent() != null && mail.getContent().length() > 0)) {
            file = this.getFileName(this.getMailDir(mail.getID()), String.valueOf(mail.getID()) + this.MAIL_BODY_FILE);
            mail.setContent(file.getName());
        } else {
            file = this.getFileName(this.getMailDir(mail.getID()), String.valueOf(mail.getID()) + this.MAIL_BODY_FILE);
            mail.setContent(file.getName());
        }
        if (!file.exists()) {
            file.createNewFile();
        }
        java.io.BufferedOutputStream out = new java.io.BufferedOutputStream(new FileOutputStream(file));
        if (mail.getHtmlBody() == null) mail.setHtmlBody("");
        out.write(mail.getHtmlBody().getBytes());
        if (out != null) {
            try {
                out.flush();
                out.close();
            } catch (IOException ex1) {
                ex1.printStackTrace();
            }
        }
    }

    /**
      /**
      * �����ռ���
      * @param mail WebMailFacade
      */
    public void saveToCcBcc(DBMailModel mail) throws Exception {
        if (mail == null) return;
        if (mail.getID() == 0) return;
        File file = new File(this.getMailDir(mail.getID()) + "/" + this.RESFILENAME);
        this.recursiveDir(file.getParentFile());
        if (file.exists()) file.createNewFile();
        Properties p = new Properties();
        p.setProperty(this.MAIL_FLAG_TO, mail.getMailTo() == null ? "" : mail.getMailTo());
        p.setProperty(this.MAIL_FLAG_CC, mail.getCc() == null ? "" : mail.getCc());
        p.setProperty(this.MAIL_FLAG_BCC, mail.getBcc() == null ? "" : mail.getBcc());
        OutputStream out = new FileOutputStream(file);
        this.store(out, p);
        if (out != null) out.close();
    }

    /**
       * �ƶ���Դ�ļ�
       */
    public void moveRes(long mailId1, long mailId2) throws Exception {
        File file1 = new File(this.getMailDir(mailId1));
        File file2 = new File(this.getMailDir(mailId2));
        if (file1.isDirectory()) {
            file1.renameTo(file2);
        }
    }

    /**
     * ���������ļ�
     */
    public void copyContent(long mailId1, long mailId2) throws Exception {
        File file1 = new File(this.getMailDir(mailId1) + "/");
        File file2 = new File(this.getMailDir(mailId2) + "/");
        this.recursiveDir(file2);
        if (file1.isDirectory()) {
            File[] files = file1.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isFile()) {
                        File file2s = new File(file2.getAbsolutePath() + "/" + files[i].getName());
                        if (!file2s.exists()) {
                            file2s.createNewFile();
                            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file2s));
                            BufferedInputStream in = new BufferedInputStream(new FileInputStream(files[i]));
                            int read;
                            while ((read = in.read()) != -1) {
                                out.write(read);
                            }
                            out.flush();
                            if (in != null) {
                                try {
                                    in.close();
                                } catch (IOException ex1) {
                                    ex1.printStackTrace();
                                }
                            }
                            if (out != null) {
                                try {
                                    out.close();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * ������Դ�ļ�
     */
    public void copyRes(long mailId1, long mailId2) throws Exception {
        File file1 = new File(this.getResDir(mailId1));
        File file2 = new File(this.getResDir(mailId2));
        this.recursiveDir(file2);
        if (file1.isDirectory()) {
            File[] files = file1.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isFile()) {
                        File file2s = new File(file2.getAbsolutePath() + "/" + files[i].getName());
                        if (!file2s.exists()) {
                            file2s.createNewFile();
                            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file2s));
                            BufferedInputStream in = new BufferedInputStream(new FileInputStream(files[i]));
                            int read;
                            while ((read = in.read()) != -1) {
                                out.write(read);
                            }
                            out.flush();
                            if (in != null) {
                                try {
                                    in.close();
                                } catch (IOException ex1) {
                                    ex1.printStackTrace();
                                }
                            }
                            if (out != null) {
                                try {
                                    out.close();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * ���渽��
     */
    public void saveAffix(MailAffix affix, long mailId) throws Exception {
        if (!(affix instanceof AffixModle)) return;
        AffixModle modle = (AffixModle) affix;
        InputStream input = modle.getInputStream();
        if (input != null) {
            File affixFile = null;
            if (modle.getAttachAlias() == null) {
                affixFile = this.getFileName(this.getResDir(mailId), modle.getAttachName());
                modle.setAttachAlias(affixFile.getName());
            } else {
                affixFile = new File(this.getResDir(mailId) + modle.getAttachAlias());
            }
            if (!affixFile.exists()) {
                affixFile.createNewFile();
            }
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(affixFile));
            BufferedInputStream bis = new BufferedInputStream(input);
            int attach = 0;
            while ((attach = bis.read()) != -1) {
                bos.write(attach);
                bos.flush();
            }
            bos.close();
            bis.close();
        }
    }

    /**
     * ɾ���
     */
    public void delAffix(MailAffix affix, long mailId) throws Exception {
        File file = new File(this.getResDir(mailId) + affix.getAttachAlias());
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * ��������
     */
    public void copyAffix(MailAffix affix, long mailId1, long mailId2) throws Exception {
        File file = new File(this.getResDir(mailId1) + affix.getAttachAlias());
        if (file.exists()) {
            File file2 = new File(this.getResDir(mailId2) + affix.getAttachAlias());
            if (!file2.exists()) {
                file2.createNewFile();
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file2));
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
                int read;
                while ((read = in.read()) != -1) {
                    out.write(read);
                }
                out.flush();
                in.close();
                out.close();
            }
        } else {
            log.debug(file.getAbsolutePath() + file.getName() + "�����ڣ������ļ�ʧ�ܣ���������");
        }
    }

    /**
     * �жϵ�ǰ�ļ��ļ����Ƿ���ڣ������ڣ������ļ���
     * @param directory File
     * @throws Exception
     */
    public void recursiveDir(File directory) throws Exception {
        if (!directory.exists()) recursiveDir(directory.getParentFile()); else return;
        directory.mkdir();
    }

    private void store(OutputStream out, Properties p) throws IOException {
        BufferedWriter awriter;
        awriter = new BufferedWriter(new OutputStreamWriter(out, this.ENCODE));
        writeln(awriter, "#" + "madebycesmailofgybing");
        writeln(awriter, "#" + new Date().toString());
        for (Enumeration e = p.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            String val = (String) p.get(key);
            if (key != null) key = key.replace('\n', ' ');
            if (val != null) val = val.replace('\n', ' ');
            writeln(awriter, key + "=" + val);
        }
        awriter.flush();
    }

    private void writeln(BufferedWriter bw, String s) throws IOException {
        bw.write(s);
        bw.newLine();
    }

    /**
     * �������ļ�
     * @param inStream InputStream
     * @throws IOException
     * @return Properties
     */
    public Properties load(InputStream inStream) throws IOException {
        Properties p = new Properties();
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream, this.ENCODE));
        while (true) {
            String line = in.readLine();
            if (line == null) return p;
            if (line.length() > 0) {
                int len = line.length();
                int keyStart;
                for (keyStart = 0; keyStart < len; keyStart++) if (" \t\r\n\f".indexOf(line.charAt(keyStart)) == -1) break;
                if (keyStart == len) continue;
                char firstChar = line.charAt(keyStart);
                if ((firstChar != '#') && (firstChar != '!')) {
                    while (continueLine(line)) {
                        String nextLine = in.readLine();
                        if (nextLine == null) nextLine = "";
                        String loppedLine = line.substring(0, len - 1);
                        int startIndex;
                        for (startIndex = 0; startIndex < nextLine.length(); startIndex++) if (" \t\r\n\f".indexOf(nextLine.charAt(startIndex)) == -1) break;
                        nextLine = nextLine.substring(startIndex, nextLine.length());
                        line = new String(loppedLine + nextLine);
                        len = line.length();
                    }
                    int separatorIndex;
                    for (separatorIndex = keyStart; separatorIndex < len; separatorIndex++) {
                        char currentChar = line.charAt(separatorIndex);
                        if (currentChar == '\\') separatorIndex++; else if ("=: \t\r\n\f".indexOf(currentChar) != -1) break;
                    }
                    int valueIndex;
                    for (valueIndex = separatorIndex; valueIndex < len; valueIndex++) if (" \t\r\n\f".indexOf(line.charAt(valueIndex)) == -1) break;
                    if (valueIndex < len) if ("=:".indexOf(line.charAt(valueIndex)) != -1) valueIndex++;
                    while (valueIndex < len) {
                        if (" \t\r\n\f".indexOf(line.charAt(valueIndex)) == -1) break;
                        valueIndex++;
                    }
                    String key = line.substring(keyStart, separatorIndex);
                    String value = (separatorIndex < len) ? line.substring(valueIndex, len) : "";
                    p.put(key, value);
                }
            }
        }
    }

    private boolean continueLine(String line) {
        int slashCount = 0;
        int index = line.length() - 1;
        while ((index >= 0) && (line.charAt(index--) == '\\')) slashCount++;
        return (slashCount % 2 == 1);
    }

    /**
     * �õ�һ�������ڵ��ļ���
     * @param path String
     * @param filename String
     * @return String
     */
    private File getFileName(String path, String filename) throws Exception {
        this.recursiveDir(new File(path));
        String houzhui = "";
        int p;
        if ((p = filename.indexOf(".")) >= 0) {
            houzhui = filename.substring(p, filename.length());
        }
        String fileName = new ces.coral.encrypt.MD5().getMD5ofStr(filename);
        Random random = new Random();
        int i = Math.abs(random.nextInt()) % 15;
        if (fileName.length() > 20) {
            fileName = "g" + i + fileName.substring(i, 19);
        }
        File file = new File(path + fileName + houzhui);
        while (file.exists()) {
            random = new Random();
            i = Math.abs(random.nextInt()) % 15;
            log.debug(fileName + houzhui + " ����!");
            file = null;
            fileName = new ces.coral.encrypt.MD5().getMD5ofStr(i + filename);
            if (fileName.length() > 20) {
                fileName = "g" + fileName.substring(i, 19);
            }
            file = new File(path + fileName + houzhui);
        }
        return file;
    }

    private void deltree(File file) {
        if (file == null) return;
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    deltree(files[i]);
                }
            }
        }
        file.delete();
    }

    /**
     * ���ظ�����Դ
     * @param mailId long
     * @param affixId long
     * @param response Response
     */
    public void downloadAffix(long mailId, long affixId, HttpServletResponse response, String encode) throws Exception {
        AffixDaoHibernate dao = new AffixDaoHibernate();
        MailAffix affix = dao.findById(affixId);
        if (affix == null) throw new Exception("û�д˸���ID=" + affixId);
        String fileName = affix.getAttachName() == null ? "δ֪" : affix.getAttachName();
        response.setHeader("Content-disposition", "attachment;filename=" + new String(affix.getAttachName().getBytes(), encode));
        File file = new File(this.getResDir(mailId) + affix.getAttachAlias());
        if (file != null && file.exists()) {
            InputStream in = new BufferedInputStream(new FileInputStream(file));
            if (in != null) {
                OutputStream out = response.getOutputStream();
                int i;
                while ((i = in.read()) != -1) {
                    out.write(i);
                }
                out.flush();
                in.close();
            }
        } else {
            throw new Exception("��������Ѿ���ʧ:mailId = " + mailId + "affixId=" + affixId);
        }
    }
}
