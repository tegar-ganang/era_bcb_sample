package com.baldwin.www.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Properties;
import java.util.TreeSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * ��ʵJDK����Ҳ��֧��FTP�����İ�jre/lib�µ�rt.jar��������SUN��DOC���沢û���ṩ��Ӧ�ĵ���
 * ��Ϊ������İ���ٷ�֧�֣����鲻Ҫʹ�á����ǿ���ʹ�õ����ṩ�İ�apache.commons��
 * apache.commons�İ����ĵ�������ʹ��
 * ����IBMҲ���ṩһ��ftp����û���ù�����Ȥ�Ŀ���ȥ�о�һ��
 * @commons-net��http://apache.mirror.phpchina.com/commons/net/binaries/commons-net-1.4.1.zip
 * @jakarta-oro��http://mirror.vmmatrix.net/apache/jakarta/oro/source/jakarta-oro-2.0.8.zip 
 * @commons-io��http://apache.mirror.phpchina.com/commons/io/binaries/commons-io-1.3.2-bin.zip
 * @author ��������
 * @2007-08-03
 */
public class MiniFtp {

    private static String username;

    private static String password;

    private static String ip;

    private static int port;

    private static Properties property = null;

    private static String configFile;

    private static FTPClient ftpClient = null;

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");

    private static final String[] FILE_TYPES = { "�ļ�", "Ŀ¼", "�������", "δ֪����" };

    public static void main(String[] args) {
        setConfigFile("woxingwosu.properties");
        connectServer();
        listAllRemoteFiles();
        changeWorkingDirectory("webroot");
        setFileType(FTP.BINARY_FILE_TYPE);
        uploadFile("woxingwosu.xml", "myfile.xml");
        renameFile("viewDetail.jsp", "newName.jsp");
        deleteFile("UpdateData.class");
        loadFile("UpdateData.java", "loadFile.java");
        closeConnect();
    }

    /**
     * �ϴ��ļ�
     * @param localFilePath--�����ļ�·��
     * @param newFileName--�µ��ļ���
     */
    public static void uploadFile(String localFilePath, String newFileName) {
        connectServer();
        BufferedInputStream buffIn = null;
        try {
            buffIn = new BufferedInputStream(new FileInputStream(localFilePath));
            ftpClient.storeFile(newFileName, buffIn);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (buffIn != null) buffIn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * �����ļ�
     * @param remoteFileName --�������ϵ��ļ���
     * @param localFileName--�����ļ���
     */
    public static void loadFile(String remoteFileName, String localFileName) {
        connectServer();
        BufferedOutputStream buffOut = null;
        try {
            buffOut = new BufferedOutputStream(new FileOutputStream(localFileName));
            ftpClient.retrieveFile(remoteFileName, buffOut);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (buffOut != null) buffOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * �г��������������ļ���Ŀ¼
     */
    public static void listAllRemoteFiles() {
    }

    /**
     * �г����������ļ���Ŀ¼
     * @param regStr --ƥ���������ʽ
     */
    @SuppressWarnings("unchecked")
    public static void closeConnect() {
        try {
            if (ftpClient != null) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ���������ļ�
     * @param configFile
     */
    public static void setConfigFile(String configFile) {
        MiniFtp.configFile = configFile;
    }

    /**
     * ���ô����ļ�������[�ı��ļ����߶������ļ�]
     * @param fileType--BINARY_FILE_TYPE��ASCII_FILE_TYPE 
     */
    public static void setFileType(int fileType) {
        try {
            connectServer();
            ftpClient.setFileType(fileType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ��չʹ��
     * @return
     */
    protected static FTPClient getFtpClient() {
        connectServer();
        return ftpClient;
    }

    /**
     * ���ò���
     * @param configFile --����������ļ�
     */
    private static void setArg(String configFile) {
        property = new Properties();
        BufferedInputStream inBuff = null;
        try {
            inBuff = new BufferedInputStream(new FileInputStream(configFile));
            property.load(inBuff);
            username = property.getProperty("username");
            password = property.getProperty("password");
            ip = property.getProperty("ip");
            port = Integer.parseInt(property.getProperty("port"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inBuff != null) inBuff.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ���ӵ�������
     */
    public static void connectServer() {
        if (ftpClient == null) {
            int reply;
            try {
                setArg(configFile);
                ftpClient = new FTPClient();
                ftpClient.setDefaultPort(port);
                ftpClient.configure(getFtpConfig());
                ftpClient.connect(ip);
                ftpClient.login(username, password);
                ftpClient.setDefaultPort(port);
                System.out.print(ftpClient.getReplyString());
                reply = ftpClient.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftpClient.disconnect();
                    System.err.println("FTP server refused connection.");
                }
            } catch (Exception e) {
                System.err.println("��¼ftp��������" + ip + "��ʧ��");
                e.printStackTrace();
            }
        }
    }

    /**
     * ���뵽��������ĳ��Ŀ¼��
     * @param directory
     */
    public static void changeWorkingDirectory(String directory) {
        try {
            connectServer();
            ftpClient.changeWorkingDirectory(directory);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * ���ص���һ��Ŀ¼
     */
    public static void changeToParentDirectory() {
        try {
            connectServer();
            ftpClient.changeToParentDirectory();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * ɾ���ļ�
     */
    public static void deleteFile(String filename) {
        try {
            connectServer();
            ftpClient.deleteFile(filename);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * �������ļ� 
     * @param oldFileName --ԭ�ļ���
     * @param newFileName --���ļ���
     */
    public static void renameFile(String oldFileName, String newFileName) {
        try {
            connectServer();
            ftpClient.rename(oldFileName, newFileName);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * ����FTP�ͷ��˵�����--һ����Բ�����
     * @return
     */
    private static FTPClientConfig getFtpConfig() {
        FTPClientConfig ftpConfig = new FTPClientConfig(FTPClientConfig.SYST_UNIX);
        ftpConfig.setServerLanguageCode(FTP.DEFAULT_CONTROL_ENCODING);
        return ftpConfig;
    }

    /**
     * ת��[ISO-8859-1 ->  GBK]
     *��ͬ��ƽ̨��Ҫ��ͬ��ת��
     * @param obj
     * @return
     */
    private static String iso8859togbk(Object obj) {
        try {
            if (obj == null) return ""; else return new String(obj.toString().getBytes("iso-8859-1"), "GBK");
        } catch (Exception e) {
            return "";
        }
    }
}
