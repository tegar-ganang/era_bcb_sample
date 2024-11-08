package com.fqr;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;

public class FtpUtil {

    private FTPClient ftpClient;

    private Logger logger = Logger.getLogger(FtpUtil.class);

    public void connect(String ftpHost, int ftpPort, String ftpUser, String ftpPwd) throws IOException {
        ftpClient = new FTPClient();
        ftpClient.setReaderThread(false);
        if (ftpPort == -1) ftpClient.connect(ftpHost); else ftpClient.connect(ftpHost, ftpPort);
        logger.info("FTP Connection Successful: " + ftpHost);
        ftpClient.login(ftpUser, ftpPwd);
    }

    public boolean ftpFile(String sourceFileName, String destinationFileName, String destinationSubDirectory) {
        boolean ftpSuccess = true;
        try {
            logger.info("Source File For FTP: " + sourceFileName);
            logger.info("Destination File For FTP: " + destinationFileName);
            logger.info("Destination Folder For FTP: " + destinationSubDirectory);
            InputStream fileStream = new FileInputStream(sourceFileName);
            if (destinationSubDirectory.equalsIgnoreCase(".")) destinationSubDirectory = "";
            ftpClient.changeWorkingDirectory(destinationSubDirectory);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.storeFile(destinationFileName, fileStream);
            fileStream.close();
        } catch (IOException e) {
            ftpSuccess = false;
            e.printStackTrace();
        }
        return ftpSuccess;
    }

    public void closeFtp() {
        try {
            ftpClient.logout();
            ftpClient.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getFile(String localdirectory, String localFileName, String remoteDirectory, String remoteFileName) {
        if (localFileName == null || localFileName.trim().length() == 0) localFileName = remoteFileName;
        String completeFilePath = localdirectory + localFileName;
        File localFile = new File(completeFilePath);
        if (remoteDirectory.equalsIgnoreCase(".")) remoteDirectory = "";
        try {
            ftpClient.changeWorkingDirectory(remoteDirectory);
            InputStream iOS = ftpClient.retrieveFileStream(remoteFileName);
            FileOutputStream fOS = new FileOutputStream(localFile, false);
            BufferedOutputStream bOS = new BufferedOutputStream(fOS);
            int stream;
            while ((stream = iOS.read()) != -1) {
                bOS.write(stream);
            }
            bOS.close();
            fOS.close();
            iOS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return localFile;
    }
}
