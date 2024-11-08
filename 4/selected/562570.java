package com.tiandinet.StrutsArticle.Utils;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * @author Meng Yang
 * @version 1.1
 */
public class FileUpload {

    private int fieldNumber = 20;

    private String[] sourceFileName = new String[fieldNumber];

    private String[] sourceFileExt = new String[fieldNumber];

    private String allow = ".gif.jpg.jpeg.zip.rar";

    private String savePath = ".";

    private String[] objectFileName = new String[fieldNumber];

    private String[] fileField = new String[fieldNumber];

    private ServletInputStream inStream = null;

    private String[] fileMessage = new String[fieldNumber];

    private long size = 100 * 1024;

    private int fileCount = 0;

    private byte[] b = new byte[4096];

    private boolean ok = true;

    private String[] textField = new String[fieldNumber];

    private String[] textFieldValue = new String[fieldNumber];

    private String[] textMessage = new String[fieldNumber];

    private int textCount = 0;

    public String contextPath = "";

    public FileUpload() {
    }

    public void init(javax.servlet.jsp.PageContext pageContext) {
        if (pageContext == null) return;
        String path = pageContext.getServletContext().getRealPath("/");
        path = path.replace('\\', '/');
        int length = path.length();
        if (path.charAt(length - 1) == '/') {
            path = path.substring(0, length - 1);
        }
        this.contextPath = path;
    }

    public void setFieldNumber(int n) {
        this.fieldNumber = n;
    }

    public void setAllow(String allow) {
        this.allow = allow;
    }

    public void setSavePath(String path) {
        this.savePath = path;
    }

    public void setSize(long s) {
        this.size = s;
    }

    public String getAllow() {
        return this.allow;
    }

    public String getSavePath() {
        return this.savePath;
    }

    public int getFileCount() {
        return this.fileCount;
    }

    public String[] getFileField() {
        return this.fileField;
    }

    public String[] getObjectFileName() {
        return this.objectFileName;
    }

    public String[] getSourceFileName() {
        return this.sourceFileName;
    }

    public String[] getFileMessage() {
        return this.fileMessage;
    }

    public int getTextCount() {
        return this.textCount;
    }

    public String[] getTextField() {
        return this.textField;
    }

    public String[] getTextFieldValue() {
        return this.textFieldValue;
    }

    public String[] getTextMessage() {
        return this.textMessage;
    }

    public void doStream(HttpServletRequest request) throws IOException {
        this.inStream = request.getInputStream();
        int readLength = 0;
        int offset = 0;
        String str = "";
        int a = 0;
        int b = 0;
        String formName = "";
        boolean fileExtOk = false;
        String formTextName = "";
        while ((readLength = this.inStream.readLine(this.b, 0, this.b.length)) != -1) {
            str = new String(this.b, 0, readLength);
            if ((offset = str.indexOf("filename=\"")) != -1) {
                a = str.indexOf("name=\"");
                formName = str.substring(a + 6);
                b = formName.indexOf("\"");
                formName = formName.substring(0, b);
                this.fileField[this.fileCount] = formName;
                str = str.substring(offset + 10);
                b = str.indexOf("\"");
                str = str.substring(0, b);
                if (str.equals("") || str == "") {
                    this.fileField[this.fileCount] = "";
                    continue;
                }
                this.sourceFileName[this.fileCount] = str;
                a = this.sourceFileName[this.fileCount].lastIndexOf(".");
                this.sourceFileExt[this.fileCount] = this.sourceFileName[this.fileCount].substring(a + 1);
                fileExtOk = this.checkFileExt(this.fileCount);
                if (fileExtOk) {
                    this.doUpload(this.fileCount);
                    this.fileCount++;
                } else {
                    this.objectFileName[fileCount] = "none";
                    this.fileCount++;
                    continue;
                }
            } else if ((offset = str.indexOf("name=\"")) != -1) {
                formTextName = str.substring(offset + 6);
                a = formTextName.indexOf("\"");
                formTextName = formTextName.substring(0, a);
                this.textField[textCount] = formTextName;
                this.getInputValue(textCount);
                this.textCount++;
            } else {
                continue;
            }
        }
    }

    public boolean checkFileExt(int fileCount) {
        if ((this.allow.indexOf(sourceFileExt[fileCount])) != -1) {
            return true;
        } else {
            this.fileMessage[fileCount] = "�ϴ��ļ����󣺲������ϴ������͵��ļ���";
            return false;
        }
    }

    public boolean doUpload(int count) {
        String objFileName = Long.toString(new java.util.Date().getTime()) + Integer.toString(count);
        try {
            this.objectFileName[count] = objFileName + "_bak." + this.sourceFileExt[count];
            File objFile = new File(this.contextPath + "/" + this.savePath, this.objectFileName[count]);
            if (objFile.exists()) {
                this.doUpload(count);
            } else {
                objFile.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(objFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            int readLength = 0;
            int offset = 0;
            String str = "";
            long readSize = 0L;
            while ((readLength = this.inStream.readLine(this.b, 0, this.b.length)) != -1) {
                str = new String(this.b, 0, readLength);
                if (str.indexOf("Content-Type:") != -1) {
                    break;
                }
            }
            this.inStream.readLine(this.b, 0, this.b.length);
            while ((readLength = this.inStream.readLine(this.b, 0, b.length)) != -1) {
                str = new String(this.b, 0, readLength);
                if (this.b[0] == 45 && this.b[1] == 45 && this.b[2] == 45 && this.b[3] == 45 && this.b[4] == 45) {
                    break;
                }
                bos.write(this.b, 0, readLength);
                readSize += readLength;
                if (readSize > this.size) {
                    this.fileMessage[count] = "�ϴ��ļ������ļ���С�������ƣ�";
                    this.ok = false;
                    break;
                }
            }
            if (this.ok) {
                bos.flush();
                bos.close();
                int fileLength = (int) (objFile.length());
                byte[] bb = new byte[fileLength - 2];
                FileInputStream fis = new FileInputStream(objFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                bis.read(bb, 0, (fileLength - 2));
                fis.close();
                bis.close();
                this.objectFileName[count] = objFileName + "." + this.sourceFileExt[count];
                File ok_file = new File(this.contextPath + "/" + this.savePath, this.objectFileName[count]);
                ok_file.createNewFile();
                BufferedOutputStream bos_ok = new BufferedOutputStream(new FileOutputStream(ok_file));
                bos_ok.write(bb);
                bos_ok.close();
                objFile.delete();
                this.fileMessage[count] = "OK";
                return true;
            } else {
                bos.flush();
                bos.close();
                File delFile = new File(this.contextPath + "/" + this.savePath, this.objectFileName[count]);
                delFile.delete();
                this.objectFileName[count] = "none";
                return false;
            }
        } catch (Exception e) {
            this.objectFileName[count] = "none";
            this.fileMessage[count] = e.toString();
            return false;
        }
    }

    public boolean getInputValue(int count) {
        String str = "";
        int readLength = 0;
        try {
            this.inStream.readLine(this.b, 0, this.b.length);
            StringBuffer formTextValue = new StringBuffer();
            while ((readLength = this.inStream.readLine(this.b, 0, this.b.length)) != -1) {
                str = new String(this.b, 0, readLength);
                if (this.b[0] == 45 && this.b[1] == 45 && this.b[2] == 45 && this.b[3] == 45 && this.b[4] == 45) {
                    break;
                }
                formTextValue.append(str);
            }
            str = formTextValue.toString();
            this.textFieldValue[count] = str.substring(0, str.length() - 2);
            this.textMessage[count] = "OK";
            return true;
        } catch (Exception e) {
            this.textFieldValue[count] = "td_none";
            this.textMessage[count] = "����ı��ֶδ���" + e.toString();
            return false;
        }
    }
}
