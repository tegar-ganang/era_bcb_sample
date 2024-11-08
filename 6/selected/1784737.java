package com.mindtree.techworks.insight.download.ftpbrowse;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileSystemView;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPListParseEngine;
import org.apache.commons.net.ftp.FTPReply;

/**
 * This class is used for browsing a remote file system. This class uses an
 * instance of the {@link org.apache.commons.net.ftp.FTPClient FTPClient}to
 * connect to the underlying FTP Source. Connection timeouts may occur.
 * 
 * @see javax.swing.filechooser.FileSystemView FileSystemView
 * @author Bindul Bhowmik
 * @version $Revision: 27 $ $Date: 2007-12-16 06:58:03 -0500 (Sun, 16 Dec 2007) $
 */
public class FTPRemoteFileSystemView extends FileSystemView {

    /**
	 * The logger instance for this class
	 */
    protected static final Logger logger = Logger.getLogger(FTPRemoteFileSystemView.class.getName());

    /**
	 * Password Authentication to use for anonymous access
	 */
    public static final PasswordAuthentication anonPassAuth = new PasswordAuthentication("anonymous", "insight@mindtree.com".toCharArray());

    /**
	 * The root file system path
	 */
    protected static final String FILE_SYSTEM_ROOT_NAME = "/";

    /**
	 * Seperator character between files
	 */
    public static final String FILE_SEPERATOR = "/";

    /**
	 * The FTPClient Object used to browse the underlying source.
	 */
    private FTPClient ftpClient;

    /**
	 * The URL String to the underlying connection
	 */
    private URL url;

    /**
	 * The Authentication information
	 */
    private PasswordAuthentication passwordAuthentication;

    /**
	 * The default directory of the connection
	 */
    private String homeDirectory;

    /**
	 * Creates a new instance of the FTP Remote File System View.
	 * 
	 * @param url
	 * @param passwordAuthentication
	 */
    public FTPRemoteFileSystemView(URL url, PasswordAuthentication passwordAuthentication) {
        this.url = url;
        if (null != passwordAuthentication) {
            this.passwordAuthentication = passwordAuthentication;
        } else {
            this.passwordAuthentication = anonPassAuth;
        }
    }

    /**
	 * Creates an instance of the <code>FTPRemoteFileSystemView</code>. The
	 * host name supplied should be the same as one would pass to construct an
	 * URL object.
	 * 
	 * @see URL#URL(java.lang.String, java.lang.String, int, java.lang.String)
	 *      URL
	 * @param host The address of the host. This can be a host name or IP
	 *            address.
	 * @param port The port to connect to.
	 * @param passwordAuthentication The credentials to connect with. This can
	 *            be null.
	 * @throws FTPBrowseException If a <code>URL</code> cannot be formed with
	 *             the values.
	 */
    public FTPRemoteFileSystemView(String host, int port, PasswordAuthentication passwordAuthentication) throws FTPBrowseException {
        try {
            url = new URL("ftp", host, port, null);
            if (null != passwordAuthentication) {
                this.passwordAuthentication = passwordAuthentication;
            } else {
                this.passwordAuthentication = anonPassAuth;
            }
        } catch (MalformedURLException e) {
            throw new FTPBrowseException(e.getMessage());
        }
    }

    /**
	 * Creates an instance of the <code>FTPRemoteFileSystemView</code>. The
	 * host name supplied should be the same as one would pass to construct an
	 * URL object.
	 * 
	 * @see URL#URL(java.lang.String, java.lang.String, int, java.lang.String)
	 *      URL
	 * @param host The address of the host. This can be a host name or IP
	 *            address.
	 * @param passwordAuthentication The credentials to use to connect. Can be
	 *            null.
	 * @throws FTPBrowseException If a <code>URL</code> cannot be formed with
	 *             the values.
	 */
    public FTPRemoteFileSystemView(String host, PasswordAuthentication passwordAuthentication) throws FTPBrowseException {
        try {
            url = new URL("ftp", host, null);
            if (null != passwordAuthentication) {
                this.passwordAuthentication = passwordAuthentication;
            } else {
                this.passwordAuthentication = anonPassAuth;
            }
        } catch (MalformedURLException e) {
            throw new FTPBrowseException(e.getMessage());
        }
    }

    /**
	 * @return Returns the passwordAuthentication.
	 */
    public PasswordAuthentication getPasswordAuthentication() {
        return passwordAuthentication;
    }

    /**
	 * @return Returns the url.
	 */
    public URL getUrl() {
        return url;
    }

    /**
	 * If a connection to the server is still open, disconnects it.
	 */
    public void disconnect() {
        if (null != ftpClient && ftpClient.isConnected()) {
            try {
                ftpClient.disconnect();
            } catch (IOException e) {
                logger.log(Level.WARNING, "IOEx while disconnecting", e);
            }
        }
    }

    /**
	 * Creates an FTP Connection
	 * 
	 * @throws FTPBrowseException If the connection cannot be opened
	 */
    private synchronized void createFTPConnection() throws FTPBrowseException {
        ftpClient = new FTPClient();
        try {
            InetAddress inetAddress = InetAddress.getByName(url.getHost());
            if (url.getPort() == -1) {
                ftpClient.connect(inetAddress);
            } else {
                ftpClient.connect(inetAddress, url.getPort());
            }
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                throw new FTPBrowseException(ftpClient.getReplyString());
            }
            if (null != passwordAuthentication) {
                ftpClient.login(passwordAuthentication.getUserName(), new StringBuffer().append(passwordAuthentication.getPassword()).toString());
            }
            if (url.getPath().length() > 0) {
                ftpClient.changeWorkingDirectory(url.getPath());
            }
            homeDirectory = ftpClient.printWorkingDirectory();
        } catch (UnknownHostException e) {
            throw new FTPBrowseException(e.getMessage());
        } catch (SocketException e) {
            throw new FTPBrowseException(e.getMessage());
        } catch (FTPBrowseException e) {
            throw e;
        } catch (IOException e) {
            throw new FTPBrowseException(e.getMessage());
        }
    }

    /**
	 * Private method used to check if an open connection is present, else
	 * creates one.
	 * 
	 * @throws FTPBrowseException FTPBrowseException is thrown if the connection
	 *             cannot be opened.
	 */
    private void checkConnection() throws FTPBrowseException {
        if (null == ftpClient || !ftpClient.isConnected()) {
            createFTPConnection();
        }
    }

    /**
	 * @see javax.swing.filechooser.FileSystemView#createNewFolder(java.io.File)
	 */
    public File createNewFolder(File containingDir) throws IOException {
        throw new FTPBrowseException("This file system view supports READ ONLY support ONLY!");
    }

    /**
	 * In the remote view home and default directory are considered to be the
	 * same.
	 * 
	 * @see javax.swing.filechooser.FileSystemView#getDefaultDirectory()
	 */
    public File getDefaultDirectory() {
        return getHomeDirectory();
    }

    /**
	 * @see javax.swing.filechooser.FileSystemView#getHomeDirectory()
	 */
    public File getHomeDirectory() {
        try {
            checkConnection();
            if (homeDirectory.equals(FILE_SYSTEM_ROOT_NAME)) {
                return getRoots()[0];
            }
            FTPFileFile ftpFileFile = null;
            try {
                String parent = homeDirectory.substring(0, homeDirectory.lastIndexOf(FILE_SEPERATOR));
                ftpClient.changeWorkingDirectory(parent);
                FTPListParseEngine ftpListParseEngine = ftpClient.initiateListParsing();
                FTPFile[] returnedFiles = ftpListParseEngine.getFiles();
                String dirName = homeDirectory.substring(homeDirectory.lastIndexOf(FILE_SEPERATOR) + 1);
                for (int i = 0; i < returnedFiles.length; i++) {
                    if (returnedFiles[i].getName().equals(dirName)) {
                        returnedFiles[i].setName(parent + FILE_SEPERATOR + returnedFiles[i].getName());
                        ftpFileFile = new FTPFileFile(returnedFiles[i], this);
                    }
                }
            } catch (FTPBrowseException e) {
                logger.log(Level.WARNING, "Problem browsing file system", e);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Problem browsing file system", e);
            }
            return ftpFileFile;
        } catch (FTPBrowseException e) {
            logger.log(Level.WARNING, "FTBEx", e);
        }
        return null;
    }

    /**
	 * @see javax.swing.filechooser.FileSystemView#getRoots()
	 */
    public File[] getRoots() {
        FTPFileFile[] ftpFiles = null;
        try {
            checkConnection();
            FTPFile ftpFile = new FTPFile();
            ftpFile.setName(FILE_SYSTEM_ROOT_NAME);
            ftpFile.setType(FTPFile.DIRECTORY_TYPE);
            FTPFileFile ftpFileFile = new FTPFileFile(ftpFile, this);
            ftpFiles = new FTPFileFile[1];
            ftpFiles[0] = ftpFileFile;
        } catch (FTPBrowseException e) {
            logger.log(Level.WARNING, "Could not get root file", e);
        }
        return ftpFiles;
    }

    /**
	 * @see javax.swing.filechooser.FileSystemView#createFileObject(java.io.File,
	 *      java.lang.String)
	 */
    public File createFileObject(File dir, String filename) {
        logger.fine("Calling Super with: " + dir.toString() + " " + filename);
        return super.createFileObject(dir, filename);
    }

    /**
	 * @see javax.swing.filechooser.FileSystemView#createFileObject(java.lang.String)
	 */
    public File createFileObject(String path) {
        logger.fine("Calling Super with: " + path);
        return super.createFileObject(path);
    }

    /**
	 * @see javax.swing.filechooser.FileSystemView#getChild(java.io.File,
	 *      java.lang.String)
	 */
    public File getChild(File parent, String fileName) {
        if (parent instanceof FTPFileFile) {
            FTPFile parentDir = ((FTPFileFile) parent).getFtpFile();
            FTPFileFile returnedFile = null;
            try {
                checkConnection();
                ftpClient.changeWorkingDirectory(parentDir.getName());
                FTPListParseEngine ftpListParseEngine = ftpClient.initiateListParsing();
                FTPFile[] returnedFiles = ftpListParseEngine.getFiles();
                if (fileName.indexOf(FILE_SEPERATOR) > -1) {
                    fileName = fileName.substring(fileName.lastIndexOf(FILE_SEPERATOR) + 1);
                }
                for (int i = 0; i < returnedFiles.length; i++) {
                    if (returnedFiles[i].getName().equals(fileName)) {
                        returnedFiles[i].setName(parentDir.getName() + FILE_SEPERATOR + returnedFiles[i].getName());
                        returnedFile = new FTPFileFile(returnedFiles[i], this);
                    }
                }
            } catch (FTPBrowseException e) {
                logger.log(Level.WARNING, "Problem browsing file system", e);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Problem browsing file system", e);
            }
            return returnedFile;
        } else {
            logger.fine("Calling Super with: " + parent.toString() + " " + fileName);
            return super.getChild(parent, fileName);
        }
    }

    /**
	 * @see javax.swing.filechooser.FileSystemView#getFiles(java.io.File,
	 *      boolean)
	 */
    public synchronized File[] getFiles(File dir, boolean useFileHiding) {
        if (dir instanceof FTPFileFile && dir.isDirectory()) {
            FTPFile ftpFile = ((FTPFileFile) dir).getFtpFile();
            String name = ftpFile.getName();
            try {
                checkConnection();
                String pwd = ftpClient.printWorkingDirectory();
                if (null == pwd || !pwd.equals(name)) {
                    ftpClient.changeWorkingDirectory(name);
                }
                pwd = ftpClient.printWorkingDirectory();
                FTPListParseEngine ftpListParseEngine = ftpClient.initiateListParsing();
                FTPFile[] files = ftpListParseEngine.getFiles();
                FTPFileFile[] ftpFiles = new FTPFileFile[files.length];
                for (int i = 0; i < files.length; i++) {
                    files[i].setName(pwd + FILE_SEPERATOR + files[i].getName());
                    ftpFiles[i] = new FTPFileFile(files[i], this);
                }
                return ftpFiles;
            } catch (FTPBrowseException e) {
                logger.log(Level.WARNING, "Could not connect to host", e);
                return new FTPFileFile[0];
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not operate on host", e);
                return new FTPFileFile[0];
            }
        }
        logger.fine("Calling Super with: " + dir.toString() + " " + String.valueOf(useFileHiding));
        return super.getFiles(dir, useFileHiding);
    }

    /**
	 * @see javax.swing.filechooser.FileSystemView#getParentDirectory(java.io.File)
	 */
    public File getParentDirectory(File dir) {
        if (dir instanceof FTPFileFile) {
            FTPFile ftpFile = ((FTPFileFile) dir).getFtpFile();
            String name = ftpFile.getName();
            if (name.equals(FILE_SYSTEM_ROOT_NAME)) {
                return null;
            }
            String parent = name.substring(0, name.lastIndexOf(FILE_SEPERATOR));
            if (parent.equals(FILE_SYSTEM_ROOT_NAME)) {
                return getRoots()[0];
            }
            String pparent = parent.substring(0, parent.lastIndexOf(FILE_SEPERATOR));
            if (pparent.length() == 0) {
                pparent = FILE_SYSTEM_ROOT_NAME;
            }
            FTPFileFile parentFile = null;
            try {
                checkConnection();
                ftpClient.changeWorkingDirectory(pparent);
                FTPListParseEngine ftpListParseEngine = ftpClient.initiateListParsing();
                FTPFile[] returnedFiles = ftpListParseEngine.getFiles();
                String parentName = parent.substring(parent.lastIndexOf(FILE_SEPERATOR) + 1);
                for (int i = 0; i < returnedFiles.length; i++) {
                    if (returnedFiles[i].getName().equals(parentName)) {
                        returnedFiles[i].setName(pparent + FILE_SEPERATOR + returnedFiles[i].getName());
                        parentFile = new FTPFileFile(returnedFiles[i], this);
                    }
                }
            } catch (FTPBrowseException e) {
                logger.log(Level.WARNING, "Problem browsing file system", e);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Problem browsing file system", e);
            }
            if (null == parentFile) {
                parentFile = (FTPFileFile) getRoots()[0];
            }
            return parentFile;
        }
        logger.fine("Calling Super with: " + dir.toString());
        return super.getParentDirectory(dir);
    }

    /**
	 * @see javax.swing.filechooser.FileSystemView#getSystemDisplayName(java.io.File)
	 */
    public String getSystemDisplayName(File f) {
        if (f instanceof FTPFileFile) {
            FTPFile ftpFile = ((FTPFileFile) f).getFtpFile();
            String name = ftpFile.getName();
            if (FILE_SYSTEM_ROOT_NAME.equals(name)) {
                return url.getHost();
            } else {
                return f.getName();
            }
        } else {
            logger.fine("Calling Super with: " + f.getPath());
            return super.getSystemDisplayName(f);
        }
    }

    /**
	 * Always returns null. The super class uses this to return special folder
	 * names such as 'Desktop' on Windows.
	 * 
	 * @see javax.swing.filechooser.FileSystemView#getSystemTypeDescription(java.io.File)
	 */
    public String getSystemTypeDescription(File f) {
        return null;
    }

    /**
	 * @see javax.swing.filechooser.FileSystemView#isComputerNode(java.io.File)
	 */
    public boolean isComputerNode(File dir) {
        if (dir instanceof FTPFileFile) {
            FTPFile ftpFile = ((FTPFileFile) dir).getFtpFile();
            String name = ftpFile.getName();
            if (FILE_SYSTEM_ROOT_NAME.equals(name)) {
                return true;
            } else {
                return false;
            }
        } else {
            return super.isComputerNode(dir);
        }
    }

    /**
	 * Returns false, drives not supported on remote systems
	 * 
	 * @see javax.swing.filechooser.FileSystemView#isDrive(java.io.File)
	 */
    public boolean isDrive(File dir) {
        return false;
    }

    /**
	 * Determines if the file is a real file or a link to another file.
	 * 
	 * @see javax.swing.filechooser.FileSystemView#isFileSystem(java.io.File)
	 * @return <code>true</code> if it is an absolute file or
	 *         <code>false</code>
	 */
    public boolean isFileSystem(File f) {
        if (f instanceof FTPFileFile) {
            FTPFile ftpFile = ((FTPFileFile) f).getFtpFile();
            return !ftpFile.isSymbolicLink();
        }
        logger.fine("Calling Super for: " + f.toString());
        return super.isFileSystem(f);
    }

    /**
	 * @see javax.swing.filechooser.FileSystemView#isFileSystemRoot(java.io.File)
	 */
    public boolean isFileSystemRoot(File dir) {
        if (dir instanceof FTPFileFile) {
            FTPFile ftpFile = ((FTPFileFile) dir).getFtpFile();
            String name = ftpFile.getName();
            if (FILE_SYSTEM_ROOT_NAME.equals(name)) {
                return true;
            } else {
                return false;
            }
        }
        logger.fine("Calling Super for: " + dir.toString());
        return super.isFileSystemRoot(dir);
    }

    /**
	 * Returns false. No floppy drives are viewable.
	 * 
	 * @see javax.swing.filechooser.FileSystemView#isFloppyDrive(java.io.File)
	 */
    public boolean isFloppyDrive(File dir) {
        return false;
    }

    /**
	 * Hidden files are not supported now. Maybe later!
	 * 
	 * @see javax.swing.filechooser.FileSystemView#isHiddenFile(java.io.File)
	 */
    public boolean isHiddenFile(File f) {
        return false;
    }

    /**
	 * @see javax.swing.filechooser.FileSystemView#isParent(java.io.File,
	 *      java.io.File)
	 */
    public boolean isParent(File folder, File file) {
        if (folder instanceof FTPFileFile && file instanceof FTPFileFile) {
            FTPFileFile calculatedParent = (FTPFileFile) getParentDirectory(file);
            String parentPath = ((FTPFileFile) folder).getFtpFile().getName();
            if (parentPath.equals(calculatedParent.getFtpFile().getName())) {
                return true;
            } else {
                return false;
            }
        }
        logger.fine("Calling Super for: " + folder.toString() + " " + file.toString());
        return super.isParent(folder, file);
    }

    /**
	 * @see javax.swing.filechooser.FileSystemView#isRoot(java.io.File)
	 */
    public boolean isRoot(File f) {
        if (f instanceof FTPFileFile) {
            FTPFile ftpFile = ((FTPFileFile) f).getFtpFile();
            String name = ftpFile.getName();
            if (FILE_SYSTEM_ROOT_NAME.equals(name)) {
                return true;
            } else {
                return false;
            }
        }
        logger.fine("Calling super for: " + f.toString());
        return super.isRoot(f);
    }

    /**
	 * @see javax.swing.filechooser.FileSystemView#isTraversable(java.io.File)
	 */
    public Boolean isTraversable(File f) {
        if (f instanceof FTPFileFile) {
            FTPFile ftpFile = ((FTPFileFile) f).getFtpFile();
            return new Boolean(ftpFile.isDirectory());
        }
        logger.fine("Calling super for: " + f.toString());
        return super.isTraversable(f);
    }
}
