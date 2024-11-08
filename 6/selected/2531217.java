package com.litt.core.net.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;

public class ApacheFtpFile {

    private static final Logger logger = Logger.getLogger(ApacheFtpFile.class);

    private String host;

    private int port = FTPClient.DEFAULT_PORT;

    private String user;

    private String password;

    private String localPath;

    private String filename;

    private String remotePath;

    /**
     * 使用apache的FTP上传文件
     * @param host
     * @param port
     * @param user
     * @param password
     */
    public void ftpUpload() {
        FTPClient ftpclient = null;
        InputStream is = null;
        try {
            ftpclient = new FTPClient();
            ftpclient.connect(host, port);
            if (logger.isDebugEnabled()) {
                logger.debug("FTP连接远程服务器：" + host);
            }
            ftpclient.login(user, password);
            if (logger.isDebugEnabled()) {
                logger.debug("登陆用户：" + user);
            }
            ftpclient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpclient.changeWorkingDirectory(remotePath);
            is = new FileInputStream(localPath + File.separator + filename);
            ftpclient.storeFile(filename, is);
            logger.info("上传文件结束...路径：" + remotePath + "，文件名：" + filename);
            is.close();
            ftpclient.logout();
        } catch (IOException e) {
            logger.error("上传文件失败", e);
        } finally {
            if (ftpclient.isConnected()) {
                try {
                    ftpclient.disconnect();
                } catch (IOException e) {
                    logger.error("断开FTP出错", e);
                }
            }
            ftpclient = null;
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
