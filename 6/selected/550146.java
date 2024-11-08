package com.baldwin.www.common;

import java.io.*;
import java.util.ArrayList;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;

public class FtpClientClass {

    private FTPClient ftpClient;

    private String host = "";

    private String user = "";

    private String password = "";

    private int port = 21;

    private String sDir = "/httpdocs/video";

    String strOut = "";

    final int bigfont = 15;

    boolean connected = false;

    static FileInputStream br;

    static FileOutputStream bw;

    static PrintWriter pw = null;

    public FtpClientClass(String h, String u, String pass, int port) {
        this.host = h;
        this.user = u;
        this.password = pass;
    }

    public boolean connentServer() {
        boolean result = false;
        try {
            ftpClient = new FTPClient();
            ftpClient.setDefaultPort(port);
            ftpClient.setControlEncoding("GBK");
            strOut = strOut + "Connecting to host " + host + "\r\n";
            ftpClient.connect(host);
            if (!ftpClient.login(user, password)) return false;
            FTPClientConfig conf = new FTPClientConfig(getSystemKey(ftpClient.getSystemName()));
            conf.setServerLanguageCode("zh");
            ftpClient.configure(conf);
            strOut = strOut + "User " + user + " login OK.\r\n";
            if (!ftpClient.changeWorkingDirectory(sDir)) {
                ftpClient.makeDirectory(sDir);
                ftpClient.changeWorkingDirectory(sDir);
            }
            strOut = strOut + "Directory: " + sDir + "\r\n";
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            strOut = strOut + "Connect Success.\r\n";
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    public void disconnectServer() {
        if (ftpClient != null) {
            try {
                ftpClient.disconnect();
                strOut = strOut + "Disconnecting to host " + host + "\r\n";
            } catch (IOException e) {
                e.printStackTrace();
            }
            ftpClient = null;
        }
    }

    private static String getSystemKey(String systemName) {
        String[] values = systemName.split(" ");
        if (values != null && values.length > 0) {
            return values[0];
        } else {
            return null;
        }
    }

    public String uploadFile(String filename, String newname) throws Exception {
        String result = "";
        FileInputStream is = null;
        try {
            File file_in = new File(filename);
            result = result + " �ϴ��ļ�:" + newname + "(" + filename + ").��С:" + file_in.length() + "B\r\n";
            is = new FileInputStream(file_in);
            if (!file_in.exists()) return "�ļ�������\r\n";
            if (file_in.length() == 0) return "�ļ�Ϊ���ļ�!\r\n";
            if (!ftpClient.storeFile(newname, is)) {
                result = result + "�ļ��ϴ�ʧ��!";
            } else {
                result = result + "�ļ��ϴ��ɹ�.\r\n";
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = result + "�ļ��ϴ�ʧ��!";
        } finally {
            if (is != null) is.close();
        }
        return result;
    }

    /**
     *writeFileIndex
     *����Ҫ����������ļ�Ŀ¼��д��ָ�����ļ���
     *@param filePath ��Ҫ�������ļ���Ŀ¼
     *@param filename ָ����д���ļ���
     *@return ��
     */
    public static ArrayList<String> writeFileIndex(String filePath, String fileName) {
        ArrayList<String> arr = new ArrayList<String>();
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] temp = new File(filePath).list();
        String str = "";
        File source_file = null;
        int count = 0;
        for (int i = 0; i < temp.length; i++) {
            source_file = new File(filePath + File.separator + temp[i]);
            if (source_file.isFile()) {
                if (source_file.getName().indexOf("ftp_bak_") < 0 && !source_file.getName().equals("source.txt")) {
                    count++;
                    str = String.format("%s|%s|%s|%s", filePath + File.separator + temp[i], filePath, source_file.getName(), source_file.length());
                    pw.println(str);
                    arr.add(filePath + File.separator + temp[i]);
                } else {
                    File destfile = new File(filePath, source_file.getName().replace("ftp_bak_", ""));
                    source_file.renameTo(destfile);
                }
            } else {
            }
            source_file = null;
        }
        source_file = null;
        pw.flush();
        pw.close();
        if (count == 0) {
            File s_file = new File(fileName);
            s_file.delete();
        }
        return arr;
    }

    /**
     *writeFileIndex
     *����Ҫ����������ļ�Ŀ¼��д��ָ�����ļ���
     *@param filePath ��Ҫ�������ļ���Ŀ¼
     *@param filename ָ����д���ļ���
     *@return ��
     */
    public void renameFiles(String filePath) {
        String[] temp = new File(filePath).list();
        File source_file = null;
        for (int i = 0; i < temp.length; i++) {
            source_file = new File(filePath + File.separator + temp[i]);
            if (source_file.isFile()) {
                if (source_file.getName().indexOf("ftp_bak_") < 0 || source_file.getName().equals("source.txt")) {
                } else {
                    System.out.println(source_file.getName());
                    File destfile = new File(filePath, source_file.getName().replace("ftp_bak_", ""));
                    source_file.renameTo(destfile);
                }
            }
            source_file = null;
        }
        source_file = null;
        return;
    }

    public static ArrayList<String> AnalyseFileIndex(String fileName) {
        ArrayList<String> arr = new ArrayList<String>();
        File src_file = new File(fileName);
        if (!src_file.exists() || src_file.length() == 0) return arr;
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter("target.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.equals("")) continue;
                String[] param = line.split("\\|");
                if (param.length < 4) continue;
                File sourceFile = new File(param[0]);
                File targetFile = new File(param[1] + File.separator + "ftp_bak_" + param[2]);
                if (sourceFile.isFile()) {
                    if (sourceFile.renameTo(targetFile)) {
                        pw.println(String.format("%s", targetFile.getPath()));
                        arr.add(targetFile.getPath());
                    }
                }
            }
            br.close();
            pw.flush();
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

    public boolean uploadFile(File file) throws IOException {
        boolean ret = false;
        ftpClient.initiateListParsing();
        FTPFile[] remoteFiles = ftpClient.listFiles();
        if (file.isFile()) {
            String name = file.getName();
            OutputStream os = ftpClient.storeFileStream(name);
            if (os != null) {
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                FTPFile remoteFile = existsFile(remoteFiles, file);
                if (remoteFile != null && raf.length() >= remoteFile.getSize()) {
                    raf.seek(remoteFile.getSize());
                }
                System.out.println("start putting file:" + name);
                int b;
                while ((b = raf.read()) != -1) {
                    os.write(b);
                    os.flush();
                }
                raf.close();
                os.close();
                if (ftpClient.completePendingCommand()) {
                    System.out.println("done!");
                    ret = true;
                    file.delete();
                } else {
                    ret = false;
                    System.out.println("can't put file:" + name);
                }
            } else {
                ret = false;
                System.out.println("can't put file:" + name);
            }
        }
        return ret;
    }

    public void uploadFiles(File localDir) throws IOException {
        ftpClient.initiateListParsing();
        FTPFile[] remoteFiles = ftpClient.listFiles();
        File[] localFiles = localDir.listFiles();
        for (File file : localFiles) {
            if (file.isFile()) {
                String name = file.getName();
                OutputStream os = ftpClient.storeFileStream(name);
                if (os != null) {
                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    FTPFile remoteFile = existsFile(remoteFiles, file);
                    if (remoteFile != null && raf.length() >= remoteFile.getSize()) {
                        raf.seek(remoteFile.getSize());
                    }
                    System.out.println("start putting file:" + name);
                    int b;
                    while ((b = raf.read()) != -1) {
                        os.write(b);
                        os.flush();
                    }
                    raf.close();
                    os.close();
                    if (ftpClient.completePendingCommand()) {
                        System.out.println("done!");
                    } else {
                        System.out.println("can't put file:" + name);
                    }
                } else {
                    System.out.println("can't put file:" + name);
                }
            }
        }
    }

    public void uploadFiles(ArrayList<String> arr) throws IOException {
        ftpClient.initiateListParsing();
        FTPFile[] remoteFiles = ftpClient.listFiles();
        File file = null;
        File t_file = null;
        for (int i = 0; i < arr.size(); i++) {
            file = new File(arr.get(i));
            t_file = new File(arr.get(i).replace("ftp_bak_", ""));
            if (file.isFile()) {
                String name = file.getName();
                String t_name = t_file.getName();
                OutputStream os = ftpClient.storeFileStream(t_name);
                if (os != null) {
                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    FTPFile remoteFile = existsFile(remoteFiles, t_file);
                    if (remoteFile != null && raf.length() >= remoteFile.getSize()) {
                        raf.seek(remoteFile.getSize());
                    }
                    System.out.println("start putting file:" + name);
                    int b;
                    while ((b = raf.read()) != -1) {
                        os.write(b);
                        os.flush();
                    }
                    raf.close();
                    os.close();
                    if (ftpClient.completePendingCommand()) {
                        if (file.exists()) file.delete();
                        System.out.println("done!");
                    } else {
                        System.out.println("can't put file:" + name);
                        if (file.exists()) {
                            if (!file.renameTo(t_file)) file.delete();
                        }
                    }
                } else {
                    System.out.println("can't put file:" + name);
                    if (file.exists()) {
                        if (!file.renameTo(t_file)) file.delete();
                    }
                }
            }
        }
        file = null;
    }

    private static FTPFile existsFile(FTPFile[] remoteFiles, File file) {
        for (FTPFile remoteFile : remoteFiles) {
            if (file.getName().equals(remoteFile.getName())) {
                return remoteFile;
            }
        }
        return null;
    }

    public String uploadFiles(String filename) throws Exception {
        String newname = "";
        if (filename.indexOf("\\") > -1) newname = filename.substring(filename.lastIndexOf("\\") + 1); else if (filename.indexOf("/") > -1) newname = filename.substring(filename.lastIndexOf("/") + 1); else newname = filename;
        return uploadFile(filename, newname);
    }

    public long downloadFile(String filename, String filePath) throws Exception {
        long result = 0;
        FileOutputStream os = null;
        try {
            java.io.File outfile = new java.io.File(filePath + filename);
            os = new FileOutputStream(outfile);
            ftpClient.retrieveFile(filename, os);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) os.close();
        }
        return result;
    }

    public boolean deleteFile(String filename) throws Exception {
        if (ftpClient != null) {
            try {
                ftpClient.deleteFile(filename);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void main(String[] args) {
        FtpClientClass ftpClass = new FtpClientClass(Constants.FTPHOST, Constants.FTPUSER, Constants.FTPPASS, Constants.FTPPORT);
        String filePath = Constants.FILEDIR;
        String fileName = Constants.FILEDIR + File.separator + "source.txt";
        ftpClass.renameFiles(filePath);
    }
}
