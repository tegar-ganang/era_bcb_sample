package com.mindtree.techworks.insight.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPFile;
import com.mindtree.techworks.insight.download.ftpbrowse.FTPBrowseException;
import com.mindtree.techworks.insight.receiver.WildCardMatcher;

/**
 * TODO
 * 
 * @see com.mindtree.techworks.insight.download.RemoteClientFactory RemoteClientFactory
 * @see com.mindtree.techworks.insight.download.RemoteClient RemoteClient
 * @author Bindul Bhowmik
 * @version $Revision: 27 $ $Date: 2007-12-16 06:58:03 -0500 (Sun, 16 Dec 2007) $
 */
public class FTPRemoteClient extends RemoteClient {

    static {
        RemoteClientFactory.registerClient("FTP_CLIENT", FTPRemoteClient.class);
    }

    /**
	 * Password Authentication to use for anonymous access
	 */
    public static final PasswordAuthentication anonPassAuth = new PasswordAuthentication("anonymous", "insight@mindtree.com".toCharArray());

    /**
	 * The FTPFileset used to connect to the host
	 */
    private FTPFileset fileset;

    /**
	 * FTPClient used to connect to the host.
	 */
    private FTPClient ftpClient;

    /**
	 * @see com.mindtree.techworks.insight.download.RemoteClient#setFileset(com.mindtree.techworks.insight.download.Fileset)
	 */
    protected void setFileset(Fileset fileset) throws RemoteClientException {
        if (fileset.getType() != Fileset.FTP_FILESET || !(fileset instanceof FTPFileset)) {
            throw new RemoteClientException("This client can handle only FTPFileset");
        }
        this.fileset = (FTPFileset) fileset;
    }

    protected String[] downloadFile(String fileName) throws RemoteClientException {
        isBusy = true;
        checkConnection();
        List matchingFileNames = new ArrayList(5);
        List downloadedFiles = new ArrayList(5);
        FTPFile[] remoteFiles;
        File destinationFile = null;
        FileOutputStream fos = null;
        if (WildCardMatcher.hasWildCardEntries(fileName)) {
            int fileSeperatorIndex = (fileName.lastIndexOf('\\') == -1) ? fileName.lastIndexOf('/') : fileName.lastIndexOf('\\');
            String remoteDirectory = (fileName.substring(0, fileSeperatorIndex));
            String pattern = (fileName.substring(fileSeperatorIndex + 1));
            try {
                remoteFiles = ftpClient.listFiles(remoteDirectory);
                List remoteFilesList = new ArrayList(remoteFiles.length);
                for (int i = 0; i < remoteFiles.length; i++) {
                    remoteFilesList.add(remoteFiles[i].getName());
                }
                matchingFileNames = getMatchingFiles(remoteFilesList, pattern);
            } catch (IOException ie) {
                throw new RemoteClientException("Couldn't open a " + "connection with the remote client", ie);
            }
        } else {
            matchingFileNames.add(fileName);
        }
        for (int i = 0; i < matchingFileNames.size(); i++) {
            fileName = (String) matchingFileNames.get(i);
            try {
                destinationFile = getDestinationFile(fileName, '/');
                fos = new FileOutputStream(destinationFile);
            } catch (FileNotFoundException e) {
                throw new RemoteClientException("Could not write to temporary file.", e);
            } catch (IOException e) {
                throw new RemoteClientException("Could not create temporary file.", e);
            }
            try {
                if (!ftpClient.retrieveFile(fileName, fos)) {
                    throw new RemoteClientException("Could not download file");
                }
                downloadedFiles.add(destinationFile.getAbsolutePath());
            } catch (IOException e) {
                throw new RemoteClientException("Could not download file", e);
            } finally {
                isBusy = false;
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        String[] downloadedFileNames = new String[downloadedFiles.size()];
        for (int i = 0; i < downloadedFiles.size(); i++) {
            downloadedFileNames[i] = (String) downloadedFiles.get(i);
        }
        return downloadedFileNames;
    }

    /**
	 * Returns a vector of filenames matching the given pattern. 
	 * @param remoteFiles	The list of remote files.
	 * @param pattern	The pattern to be matched with.
	 * @return	The matching file names as a vector.
	 */
    private List getMatchingFiles(List remoteFiles, String pattern) {
        List matchingFiles = new ArrayList(5);
        for (int i = 0; i < remoteFiles.size(); i++) {
            String filePath = (String) remoteFiles.get(i);
            int fileSeperatorIndex = (filePath.lastIndexOf('\\') == -1) ? filePath.lastIndexOf('/') : filePath.lastIndexOf('\\');
            String fileName = filePath.substring(fileSeperatorIndex + 1);
            WildCardMatcher matcher = new WildCardMatcher(pattern);
            if (matcher.matches(fileName)) {
                matchingFiles.add(filePath);
            } else {
            }
        }
        return matchingFiles;
    }

    /**
	 * @see com.mindtree.techworks.insight.download.RemoteClient#closeConnection()
	 */
    protected void closeConnection() throws RemoteClientException {
        if (null != ftpClient && ftpClient.isConnected()) {
            try {
                ftpClient.disconnect();
            } catch (IOException e) {
                throw new RemoteClientException("Problem closing connection.", e);
            }
        }
    }

    /**
	 * Creates an FTP Connection
	 * 
	 * @throws RemoteClientException If the connection cannot be opened
	 */
    private synchronized void createFTPConnection() throws RemoteClientException {
        ftpClient = new FTPClient();
        try {
            URL url = fileset.getHostURL();
            PasswordAuthentication passwordAuthentication = fileset.getPasswordAuthentication();
            if (null == passwordAuthentication) {
                passwordAuthentication = anonPassAuth;
            }
            InetAddress inetAddress = InetAddress.getByName(url.getHost());
            if (url.getPort() == -1) {
                ftpClient.connect(inetAddress);
            } else {
                ftpClient.connect(inetAddress, url.getPort());
            }
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                throw new FTPBrowseException(ftpClient.getReplyString());
            }
            ftpClient.login(passwordAuthentication.getUserName(), new StringBuffer().append(passwordAuthentication.getPassword()).toString());
            if (url.getPath().length() > 0) {
                ftpClient.changeWorkingDirectory(url.getPath());
            }
        } catch (UnknownHostException e) {
            throw new RemoteClientException("Host not found.", e);
        } catch (SocketException e) {
            throw new RemoteClientException("Socket cannot be opened.", e);
        } catch (IOException e) {
            throw new RemoteClientException("Socket cannot be opened.", e);
        }
    }

    /**
	 * Private method used to check if an open connection is present, else
	 * creates one.
	 *  
	 * @throws RemoteClientException is thrown if the connection
	 *             cannot be opened.
	 */
    private void checkConnection() throws RemoteClientException {
        if (null == ftpClient || !ftpClient.isConnected()) {
            createFTPConnection();
        }
    }
}
