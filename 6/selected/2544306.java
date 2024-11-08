package ro.wpcs.traser.client.storage.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Properties;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;
import ro.wpcs.traser.client.ApplicationConstants;
import ro.wpcs.traser.client.storage.AccessDeniedException;
import ro.wpcs.traser.client.storage.Credentials;
import ro.wpcs.traser.client.storage.FileNotFoundException;
import ro.wpcs.traser.client.storage.IRemoteStorageProvider;
import ro.wpcs.traser.client.storage.RemoteStorageException;
import ro.wpcs.traser.client.storage.UnsupportedRemoteStorageException;

/**
 * FTP Provider
 * 
 * @author marius.staicu, Tomita Militaru
 * 
 */
public final class FtpProvider implements IRemoteStorageProvider {

    /** Class logger. */
    public static final Logger logger = Logger.getLogger(FtpProvider.class);

    /** FTP format. */
    private static final String FTP = "ftp://";

    /** Configuration file */
    private Properties prop = null;

    @Override
    public boolean canHandle(final String remoteFile) {
        boolean flag = true;
        if (!remoteFile.substring(0, FTP.length()).equalsIgnoreCase(FTP)) {
            flag = false;
        }
        return flag;
    }

    @Override
    public boolean fileExists(final Credentials credentials, final String remoteFile) throws AccessDeniedException, UnsupportedRemoteStorageException {
        loadConfig();
        FTPClient ftp;
        FTPFile[] files;
        int index;
        boolean flag = false;
        try {
            ftp = getFTP(credentials, remoteFile);
            files = ftp.listFiles();
            for (index = 0; index < files.length; index++) {
                if (files[index].getName().equals(extractFilename(remoteFile))) {
                    ftp.disconnect();
                    flag = true;
                }
            }
        } catch (IOException e) {
            new RemoteStorageException(e);
        }
        return flag;
    }

    @Override
    public void loadDocument(final Credentials credentials, final String remoteFile, final OutputStream output) throws AccessDeniedException, UnsupportedRemoteStorageException, FileNotFoundException, IOException {
        loadConfig();
        FTPClient ftp = getFTP(credentials, remoteFile);
        ftp.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
        ftp.retrieveFile(extractFilename(remoteFile), output);
        ftp.disconnect();
    }

    @Override
    public void saveDocument(final Credentials credentials, final String remoteFile, final InputStream input) throws AccessDeniedException, UnsupportedRemoteStorageException, IOException {
        loadConfig();
        FTPClient ftp = getFTP(credentials, remoteFile);
        if (!fileExists(credentials, remoteFile)) {
            int index;
            String[] folders;
            ftp.changeWorkingDirectory("/");
            folders = getPathName(remoteFile).split("/");
            for (index = 1; index < folders.length - 1; index++) {
                if (!isFolder(ftp, folders[index])) {
                    ftp.makeDirectory(folders[index]);
                }
                ftp.changeWorkingDirectory(folders[index]);
            }
        }
        ftp.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
        ftp.storeFile(extractFilename(remoteFile), input);
        input.close();
        ftp.disconnect();
    }

    public void renameDocument(final Credentials credentials, final String remoteFile, final String renamedRemoteFile) throws NumberFormatException, SocketException, AccessDeniedException, IOException, UnsupportedRemoteStorageException {
        loadConfig();
        logger.info("Remote File from renameDocument: " + remoteFile);
        FTPClient ftp = getFTP(credentials, remoteFile);
        ftp.changeWorkingDirectory(getPathName(remoteFile));
        ftp.printWorkingDirectory();
        ftp.rename(extractFilename(remoteFile), extractFilename(renamedRemoteFile));
        ftp.disconnect();
    }

    public void deleteDocument(final Credentials credentials, final String remoteFile) throws AccessDeniedException, UnsupportedRemoteStorageException, IOException {
        loadConfig();
        FTPClient ftp = getFTP(credentials, remoteFile);
        ftp.deleteFile(extractFilename(remoteFile));
        ftp.disconnect();
    }

    public void deleteFolder(final Credentials credentials, final String remoteFolder) throws NumberFormatException, SocketException, AccessDeniedException, IOException {
        loadConfig();
        FTPClient ftp = getFTP(credentials, remoteFolder);
        ftp.removeDirectory(extractFilename(remoteFolder));
        ftp.disconnect();
    }

    /**
	 * Creates the FTP connection and sets the working directory to the location
	 * of the remoteFile.
	 * 
	 * @param credentials
	 *            The credentials used to access the remote resource
	 * @param remoteFile
	 *            The remote resource location
	 * @return A FTPClient object with the connection to the server
	 * @throws NumberFormatException
	 *             Invalid Port format
	 * @throws SocketException
	 * @throws IOException
	 *             An i/o exception while transferring the content
	 * @throws AccessDeniedException
	 *             The credentials aren't valid
	 */
    public FTPClient getFTP(final Credentials credentials, final String remoteFile) throws NumberFormatException, SocketException, IOException, AccessDeniedException {
        String fileName = extractFilename(remoteFile);
        String fileDirectory = getPathName(remoteFile).substring(0, getPathName(remoteFile).indexOf(fileName));
        FTPClient ftp;
        ftp = new FTPClient();
        loadConfig();
        logger.info("FTP connection to: " + extractHostname(remoteFile));
        logger.info("FTP PORT: " + prop.getProperty("port"));
        ftp.connect(extractHostname(remoteFile), Integer.parseInt(prop.getProperty("port")));
        int reply = ftp.getReplyCode();
        if (!(FTPReply.isPositiveCompletion(reply))) {
            return null;
        }
        ftp.setFileTransferMode(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
        ftp.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
        if (!ftp.login(credentials.getUserName(), credentials.getPassword())) {
            throw new AccessDeniedException(prop.getProperty("login_message"));
        }
        if (fileDirectory != null) {
            ftp.changeWorkingDirectory(fileDirectory);
        }
        return ftp;
    }

    /**
	 * Extracts from a remoteFile the hostname of a FTP pathname
	 * 
	 * @param remoteFile
	 *            The remote resource location
	 * @return A string representing the hostname
	 */
    private String extractHostname(final String remoteFile) {
        return remoteFile.substring(6, remoteFile.indexOf('/', FTP.length()));
    }

    /**
	 * Extracts from a remoteFile the hostname of a FTP pathname
	 * 
	 * @param remoteFile
	 *            The remote resource location
	 * @return A string representing the pathname (ex.
	 *         /FTPfolder1/FTPfolder2/../file.ext)
	 */
    private String getPathName(final String remoteFile) {
        return remoteFile.substring(remoteFile.indexOf('/', FTP.length()));
    }

    /**
	 * Extracts from a remoteFile the filename from FTP pathname
	 * 
	 * @param remoteFile
	 *            The remote resource location
	 * @return A string representing the filename
	 */
    public static String extractFilename(final String remoteFile) {
        String filename = null;
        for (int i = remoteFile.length() - 1; i >= 0; i--) {
            if (remoteFile.charAt(i) == '\\' || remoteFile.charAt(i) == '/') {
                filename = remoteFile.substring(i + 1);
                break;
            }
        }
        return filename;
    }

    /**
	 * Checks if a folder exists
	 * 
	 * @param ftp
	 *            FTP connection
	 * @param folder
	 *            The searched folder
	 * @return If folder is found <code>true</code> else returns
	 *         <code>false</code>
	 * @throws IOException
	 *             An i/o exception while transferring the content
	 */
    private boolean isFolder(final FTPClient ftp, final String folder) throws IOException {
        int index;
        FTPFile[] files;
        boolean flag = false;
        files = ftp.listFiles();
        for (index = 0; index < files.length; index++) {
            if (files[index].getName().equals(folder)) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    /**
	 * Loads the property file for the port settings and messages. Should be
	 * called before using the class.
	 */
    public boolean loadConfig() {
        prop = ApplicationConstants.getConfiguration();
        return true;
    }

    public boolean checkConnection() {
        if (loadConfig()) {
            Credentials credentials = new Credentials(prop.getProperty("username"), prop.getProperty("password"));
            try {
                FTPClient ftp = getFTP(credentials, prop.getProperty("host") + "/");
                if (ftp != null) {
                    logger.info("Valid ftp settings");
                    return true;
                } else {
                    return false;
                }
            } catch (NumberFormatException e) {
                logger.info(e.getMessage());
                return false;
            } catch (SocketException e) {
                logger.info(e.getMessage());
                return false;
            } catch (AccessDeniedException e) {
                logger.info(e.getMessage());
                return false;
            } catch (IOException e) {
                logger.info(e.getMessage());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean checkConnection(String host, Credentials credentials) {
        try {
            FTPClient ftp = getFTP(credentials, host + "/");
            if (ftp != null) {
                logger.info("Valid ftp settings");
                return true;
            } else {
                logger.info("Invalid ftp settings");
                return false;
            }
        } catch (NumberFormatException e) {
            logger.info(e.getMessage());
            return false;
        } catch (SocketException e) {
            logger.info(e.getMessage());
            return false;
        } catch (AccessDeniedException e) {
            logger.info(e.getMessage());
            return false;
        } catch (IOException e) {
            logger.info(e.getMessage());
            return false;
        }
    }
}
