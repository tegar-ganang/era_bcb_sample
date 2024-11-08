package cn.imgdpu.net;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import cn.imgdpu.util.GeneralMethod;
import cn.imgdpu.util.SqlProcess;

public class GetFtpFileList extends Thread {

    FTPClient ftpClient;

    String server, user, password;

    public ArrayList<String> fileList;

    ArrayList<String> dirQueue;

    String dirPre;

    int fileCount = 0;

    int errCode;

    String errMsg;

    String encode = "GBK";

    String queFilePath;

    String bufFilePath;

    ArrayList<String> siteList = new ArrayList<String>();

    ArrayList<String> delFileList = new ArrayList<String>();

    public boolean cancel = false;

    public GetFtpFileList(ArrayList<String> siteList) {
        this.siteList = siteList;
    }

    @Override
    public void run() {
        try {
            updateFor: for (int i = 0; i < siteList.size(); i += 3) {
                ArrayList<String> fileListXml = new ArrayList<String>();
                String server = siteList.get(i);
                String user = siteList.get(i + 1);
                String password = siteList.get(i + 2);
                for (int j = 0; j <= 4; j++) {
                    if (this.isInterrupted() || cancel) break updateFor;
                    cn.imgdpu.GSAGUI.setStatusAsyn(server + ":开始获取FTP文件列表...");
                    if (connectServer(server, user, password)) {
                        fileListXml = fileList;
                        SqlProcess.setData("update ftpList set updatetime='" + GeneralMethod.getGeneralMethod().getNowTime() + "' where ip='" + server + "'");
                        SqlProcess.setData("update ftpList set filetable='" + GeneralMethod.getGeneralMethod().getIpToTableName(server) + "' where ip='" + server + "'");
                        cn.imgdpu.util.SqlProcess.setFileList(GeneralMethod.getGeneralMethod().getIpToTableName(server), fileListXml);
                        cn.imgdpu.GSAGUI.setStatusAsyn(server + ":完成获取FTP文件列表!");
                        break;
                    } else {
                        cn.imgdpu.GSAGUI.setStatusAsyn(server + ":错误,10秒后重新连接." + errMsg);
                        Thread.sleep(1000 * 10);
                    }
                }
            }
            cn.imgdpu.compo.FtpFilesUpdateCompo.readSite();
            if (!isInterrupted()) for (String s : delFileList) {
                deleteFile(cn.imgdpu.util.FileUrlConv.UrlConvIo(s));
            }
        } catch (InterruptedException e) {
            cn.imgdpu.util.CatException.getMethod().catException(e, "线程中断异常");
        }
        if (!cn.imgdpu.GSAGUI.shell.isDisposed()) cn.imgdpu.GSAGUI.shell.getDisplay().asyncExec(new Runnable() {

            @Override
            public void run() {
                cn.imgdpu.GSAGUI.setStatus("获取FTP文件列表完成!");
                cn.imgdpu.compo.FtpFilesUpdateCompo.setOK();
            }
        });
    }

    public String getExtension(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int i = filename.lastIndexOf('.');
            int j = filename.lastIndexOf('/');
            if ((i > -1) && (i < (filename.length() - 1)) && (i > j)) {
                return filename.substring(i + 1);
            }
        }
        return "-";
    }

    public ArrayList<String> getQueue(String filePath) {
        return getFileToArr(filePath);
    }

    public ArrayList<String> addQueue(ArrayList<String> arr, String str) {
        boolean flag = false;
        for (Iterator<String> iter = arr.iterator(); iter.hasNext(); ) {
            String element = iter.next();
            if (element.equals(str)) {
                flag = true;
                break;
            }
        }
        if (!flag) arr.add(str);
        return arr;
    }

    public boolean deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.isFile() && file.exists()) {
            file.delete();
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<String> getFileToArr(String filePath) {
        BufferedReader bufReader;
        ArrayList<String> arr = new ArrayList<String>();
        try {
            bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(cn.imgdpu.util.FileUrlConv.UrlConvIo(filePath))));
            String str;
            while ((str = bufReader.readLine()) != null) {
                arr.add(str);
            }
            bufReader.close();
        } catch (IOException e) {
            cn.imgdpu.util.CatException.getMethod().catException(e, "IO异常");
        }
        return arr;
    }

    public void setArrToFile(ArrayList<String> arr, String filePath) {
        BufferedWriter bufWriter;
        if (arr != null) {
            try {
                bufWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cn.imgdpu.util.FileUrlConv.UrlConvIo(filePath))));
                for (Iterator<String> iter = arr.iterator(); iter.hasNext(); ) {
                    String element = iter.next();
                    bufWriter.write(element + "\n");
                }
                bufWriter.flush();
                bufWriter.close();
            } catch (IOException e) {
                cn.imgdpu.util.CatException.getMethod().catException(e, "IO异常");
            }
        }
    }

    public boolean connectServer(String server) {
        return connectServer(server, "anonymous", "anonymous");
    }

    public boolean connectServer(String server, String user, String password) {
        boolean result = true;
        try {
            if (user.equals("")) {
                user = "anonymous";
                password = "anonymous";
            }
            this.server = server;
            this.user = user;
            this.password = password;
            ftpClient = new FTPClient();
            ftpClient.setControlEncoding(encode);
            ftpClient.connect(server);
            ftpClient.setSoTimeout(1000 * 30);
            ftpClient.setDefaultTimeout(1000 * 30);
            ftpClient.setConnectTimeout(1000 * 30);
            ftpClient.enterLocalPassiveMode();
            ftpClient.login(user, password);
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                ftpClient.disconnect();
                return false;
            }
            queFilePath = "data\\" + this.server + ".que";
            bufFilePath = "data\\" + this.server + ".buf";
            startGetList();
        } catch (java.net.SocketTimeoutException e1) {
            errMsg = ftpClient.getReplyString();
            errCode = ftpClient.getReplyCode();
            result = false;
            setArrToFile(dirQueue, queFilePath);
            setArrToFile(fileList, bufFilePath);
            cn.imgdpu.util.CatException.getMethod().catException(e1, "连接超时");
        } catch (Exception e) {
            errMsg = ftpClient.getReplyString();
            errCode = ftpClient.getReplyCode();
            result = false;
            setArrToFile(dirQueue, queFilePath);
            setArrToFile(fileList, bufFilePath);
            cn.imgdpu.util.CatException.getMethod().catException(e, "未知异常");
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException ioe) {
                    cn.imgdpu.util.CatException.getMethod().catException(ioe, "IO异常");
                }
            }
        }
        return result;
    }

    public void startGetList() throws Exception {
        fileList = new ArrayList<String>();
        dirQueue = getQueue(queFilePath);
        if (dirQueue.size() != 0) {
            cn.imgdpu.GSAGUI.setStatusAsyn("载入上次缓存文件...");
            fileList = getFileToArr(bufFilePath);
            fileCount = fileList.size() / 4;
            dirPre = dirQueue.get(0);
        } else {
            dirQueue.add("/");
        }
        String dirNext;
        while (dirQueue.size() != 0 && !cancel) {
            if (dirPre != null) dirQueue.remove(0);
            if (dirQueue.size() > 0) {
                dirNext = dirQueue.get(0);
                listFile(dirNext);
                dirPre = dirNext;
            }
        }
        if (!cancel) {
            delFileList.add(queFilePath);
            delFileList.add(bufFilePath);
        } else {
            setArrToFile(dirQueue, queFilePath);
            setArrToFile(fileList, bufFilePath);
        }
    }

    public ArrayList<String> listFile(String dir) throws Exception {
        cn.imgdpu.GSAGUI.setStatusAsyn(this.server + ":目录(" + fileCount + ") " + dir);
        if (this.isInterrupted()) {
            cancel = true;
            setArrToFile(dirQueue, queFilePath);
            setArrToFile(fileList, bufFilePath);
            return null;
        }
        FTPFile[] myftpFiles = null;
        try {
            myftpFiles = ftpClient.listFiles(dir);
        } catch (java.net.BindException e1) {
            cn.imgdpu.util.CatException.getMethod().catException(e1, "未知异常");
            try {
                Thread.sleep(2000);
                myftpFiles = ftpClient.listFiles(dir);
            } catch (InterruptedException e2) {
                cn.imgdpu.util.CatException.getMethod().catException(e2, "线程中断异常");
            }
        }
        for (FTPFile ftpFile : myftpFiles) {
            if (!ftpFile.getName().equals("..") && !ftpFile.getName().equals(".") && ftpFile.isDirectory()) {
                String dirNext = dir + ftpFile.getName() + "/";
                if (dirNext.contains("/xxxxxxx/")) {
                } else {
                    addQueue(dirQueue, dirNext);
                }
            } else if (ftpFile.isFile()) {
                StringBuffer buf = new StringBuffer();
                if (!this.user.equals("anonymous")) {
                    buf.append("ftp://").append(this.user).append(":").append(this.password).append("@");
                } else buf.append("ftp://");
                fileCount++;
                buf.append(this.server).append(dir).append(ftpFile.getName());
                fileList.add((ftpFile.getName()));
                fileList.add((buf.toString()));
                fileList.add(GeneralMethod.getGeneralMethod().getExtension(ftpFile.getName()));
                fileList.add(String.valueOf(ftpFile.getSize()));
            }
        }
        return fileList;
    }
}
