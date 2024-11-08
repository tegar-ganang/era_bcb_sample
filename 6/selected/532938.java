package com.webstersmalley.picweb.offline.deploy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

/**
 * Implements an FTP deployer.
 * 
 * @author Matthew Smalley
 */
public class FTPDeployer implements Deployer {

    /** Logger for the class. */
    Logger log = Logger.getLogger(FTPDeployer.class);

    private FTPClient ftp = new FTPClient();

    public FTPDeployer(String hostname, int port, String remoteDir, String username, String password) {
        this.hostname = hostname;
        this.port = port;
        this.remoteDir = remoteDir;
        this.username = username;
        this.password = password;
    }

    private String hostname;

    private int port;

    private String remoteDir;

    private String username;

    private String password;

    public void deploy(String baseDir) throws IOException {
        deploy(baseDir, false);
    }

    public void deploy(String baseDir, boolean clean) throws IOException {
        try {
            ftp.connect(hostname, port);
            log.debug("Connected to: " + hostname + ":" + port);
            ftp.login(username, password);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IOException("Error logging onto ftp server. FTPClient returned code: " + reply);
            }
            log.debug("Logged in");
            ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
            if (clean) {
                deleteDir(remoteDir);
            }
            storeFolder(baseDir, remoteDir);
        } finally {
            ftp.disconnect();
        }
    }

    private void storeFolder(String folder, String remotePath) throws IOException {
        log.debug("Storing folder: " + folder + " at remote path: " + remotePath);
        ftp.changeWorkingDirectory(remotePath);
        File local = new File(folder);
        if (!local.isDirectory()) {
            throw new RuntimeException("Was asked to send a folder, but a file has been given");
        }
        File[] children = local.listFiles();
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (child.isDirectory()) {
                storeFolder(child.getAbsolutePath(), remotePath + "/" + child.getName());
            } else {
                storeFile(child, remotePath);
            }
        }
    }

    private void storeFile(File f, String remotePath) throws IOException {
        log.debug("Storing file: " + f.getAbsolutePath() + " at remote path: " + remotePath);
        makeDirs(remotePath);
        ftp.changeWorkingDirectory(remotePath);
        FileInputStream fis = new FileInputStream(f);
        ftp.storeFile(f.getName(), fis);
        fis.close();
    }

    private void makeDirs(String remotePath) throws IOException {
        log.debug("Attempting to make directory: " + remotePath);
        File dir = new File(remotePath);
        String parentPath = dir.getParent();
        if (parentPath == null) {
            return;
        }
        try {
            ftp.changeWorkingDirectory(parentPath);
        } catch (IOException e) {
            makeDirs(parentPath);
            ftp.changeWorkingDirectory(parentPath);
        }
        ftp.makeDirectory(dir.getName());
    }

    private void deleteDir(String remotePath) throws IOException {
        log.debug("Deleting: " + remotePath);
        try {
            ftp.changeWorkingDirectory(remotePath);
        } catch (IOException e) {
            return;
        }
        deleteAll();
        ftp.changeToParentDirectory();
        ftp.removeDirectory(remotePath);
    }

    private void deleteAll() throws IOException {
        FTPFile[] children = ftp.listFiles();
        for (int i = 0; i < children.length; i++) {
            FTPFile child = children[i];
            if (child.isDirectory()) {
                ftp.changeWorkingDirectory(child.getName());
                deleteAll();
                ftp.changeToParentDirectory();
                log.debug("Removing directory: " + child.getName());
                ftp.removeDirectory(child.getName());
            } else if (child.isFile()) {
                log.debug("Removing file: " + child.getName());
                ftp.deleteFile(child.getName());
            }
        }
    }

    public static void main(String[] args) {
    }
}
