package com.san.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class FTPUtil {

    private static Log logger = LogFactory.getLog(FTPUtil.class.getName());

    public static void upload(FTPDetails ftpDetails) {
        FTPClient ftp = new FTPClient();
        try {
            String host = ftpDetails.getHost();
            logger.info("Connecting to ftp host: " + host);
            ftp.connect(host);
            logger.info("Received reply from ftp :" + ftp.getReplyString());
            ftp.login(ftpDetails.getUserName(), ftpDetails.getPassword());
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.makeDirectory(ftpDetails.getRemoterDirectory());
            logger.info("Created directory :" + ftpDetails.getRemoterDirectory());
            ftp.changeWorkingDirectory(ftpDetails.getRemoterDirectory());
            BufferedInputStream ftpInput = new BufferedInputStream(new FileInputStream(new File(ftpDetails.getLocalFilePath())));
            OutputStream storeFileStream = ftp.storeFileStream(ftpDetails.getRemoteFileName());
            IOUtils.copy(ftpInput, storeFileStream);
            logger.info("Copied file : " + ftpDetails.getLocalFilePath() + " >>> " + host + ":/" + ftpDetails.getRemoterDirectory() + "/" + ftpDetails.getRemoteFileName());
            ftpInput.close();
            storeFileStream.close();
            ftp.logout();
            ftp.disconnect();
            logger.info("Logged out. ");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class FTPDetails {

        String host;

        String userName;

        String password;

        String remoterDirectory;

        String remoteFileName;

        String localFilePath;

        public FTPDetails(String host, String userName, String password, String remoterDirectory, String remoteFileName, String localFilePath) {
            this.host = host;
            this.userName = userName;
            this.password = password;
            this.remoterDirectory = remoterDirectory;
            this.remoteFileName = remoteFileName;
            this.localFilePath = localFilePath;
        }

        public String getHost() {
            return host;
        }

        public String getUserName() {
            return userName;
        }

        public String getPassword() {
            return password;
        }

        public String getRemoterDirectory() {
            return remoterDirectory;
        }

        public String getRemoteFileName() {
            return remoteFileName;
        }

        public String getLocalFilePath() {
            return localFilePath;
        }
    }
}
