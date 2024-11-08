package com.rhythm.commons.net.ftp;

import com.rhythm.commons.collections.Lists;
import com.rhythm.commons.io.Streams;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * Class acts as a simplified wrapper for <code>org.apache.commons.net.ftp.FTPClient</code>.
 * 
 * @author Michael J. Lee
 * @see org.apache.commons.net.ftp.FTPClient;
 */
public class SimpleFTPClient {

    protected static final FileTransferMode DEFAULT_FILE_TRANSFER_MODE = FileTransferMode.BINARY;

    protected FileTransferMode fileTransferMode;

    protected FTPClient ftpClient;

    /**
     * Default contstructor for a new instance of a <code>SimpleFTPClient</code>
     */
    public SimpleFTPClient() {
    }

    /**
     * Attempts to close the connection to the FTP server and restores
     * connection parameters to the default values.
     *
     * @throws IOException 
     */
    public void disconnect() throws IOException {
        try {
            ftpClient.disconnect();
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /**
     * Connects to the given host using the provided user name and password
     * @param host
     * @param userName
     * @param password
     * @return
     * @throws java.io.IOException
     * @throws java.net.UnknownHostException
     */
    public boolean connect(String host, String userName, String password) throws IOException, UnknownHostException {
        try {
            if (ftpClient != null) {
                if (ftpClient.isConnected()) {
                    ftpClient.disconnect();
                }
            }
            ftpClient = new FTPClient();
            boolean success = false;
            ftpClient.connect(host);
            int reply = ftpClient.getReplyCode();
            if (FTPReply.isPositiveCompletion(reply)) {
                success = ftpClient.login(userName, password);
            }
            if (!success) {
                ftpClient.disconnect();
            }
            return success;
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /**
     * Turns passive transfer mode on or off using the PASV command.
     *
     * @param setPassive true to activate passive mode, otherwise active mode.
     */
    public void setPassiveMode(boolean setPassive) {
        if (setPassive) {
            ftpClient.enterLocalPassiveMode();
        } else {
            ftpClient.enterLocalActiveMode();
        }
    }

    /**
     * Sets the file transfer mode
     * @param fileTransferMode
     * @return
     * @throws java.io.IOException
     */
    public boolean setFileTransferMode(FileTransferMode fileTransferMode) throws IOException {
        try {
            this.fileTransferMode = fileTransferMode;
            return ftpClient.setFileType(fileTransferMode.toInt());
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    public boolean downloadFile(String serverFile, File destination) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(destination);
            return ftpClient.retrieveFile(serverFile, out);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        } finally {
            Streams.close(out);
        }
    }

    /**
     * Attempts to upload the given source <code>File</code> to the 
     * destination FTP server. If no <code>FileTransferMode</code> has
     * been set a default will be used.
     * 
     * @param source
     * @param destination
     * @return
     * @throws java.io.IOException
     */
    public boolean uploadFile(File source, String destination) throws IOException {
        FileInputStream in = null;
        try {
            if (ftpClient == null || !ftpClient.isConnected()) {
                throw new IOException("The current instance of the " + "FTP client is either closed or was never opened. Connection " + "to the FTP server is requried before calling any FTP commands.");
            }
            if (fileTransferMode == null) {
                setFileTransferMode(DEFAULT_FILE_TRANSFER_MODE);
            }
            in = new FileInputStream(source);
            ftpClient.storeFile(destination, in);
            int reply = ftpClient.getReplyCode();
            return (FTPReply.isPositiveCompletion(reply));
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        } finally {
            Streams.close(in);
        }
    }

    /** Get the list of files in the current directory as a Vector of Strings
     * (excludes subdirectories)
     * @return
     * @throws IOException
     */
    public String[] listFileNames() throws IOException {
        try {
            FTPFile[] files = ftpClient.listFiles();
            List<String> fileNames = Lists.newArrayList();
            for (int i = 0; i < files.length; i++) {
                if (!files[i].isDirectory()) {
                    fileNames.add(files[i].getName());
                }
            }
            return Lists.toArray(fileNames);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /**
     * Returns if the given fileName is in the current list of files
     * @param fileName a file name to check
     * @return <code>true</code> if the given file exists, otherwise false.
     * @throws IOException 
     */
    public boolean containsFileName(String fileName) throws IOException {
        try {
            FTPFile[] files = ftpClient.listFiles();
            for (FTPFile aFile : files) {
                if (!aFile.isDirectory()) {
                    if (aFile.getName().equals(fileName)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    public boolean changeDirectory(String pathName) throws IOException {
        return ftpClient.changeWorkingDirectory(pathName);
    }
}
