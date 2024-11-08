package org.regilo.ftp.client;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.regilo.ftp.client.exceptions.FtpException;
import org.regilo.ftp.model.FtpSiteConnector;

public class FtpClient {

    private static final Logger log = Logger.getLogger(FtpClient.class);

    public static final String PATH_SEPARATOR = "/";

    private FtpSiteConnector connector;

    public FtpClient(FtpSiteConnector connector) {
        this.connector = connector;
    }

    /**
	 * Uploads a file via ftp
	 * 
	 * @param localFile the file to upload
	 * @param remoteDirectory remote directory
	 * @param remoteFile remote file name
	 * @param async if true runs as Job
	 * @param createRootDir if true create the remote directory
	 * @param monitor IProgressMonitor to report progress information
	 * @throws FtpException
	 */
    public void upload(final File localFile, String remoteDirectory, String remoteFile, boolean async, final boolean createRootDir, IProgressMonitor monitor) throws FtpException {
        String remotePath = connector.getRemoteDirectory();
        if (remoteDirectory == null) {
            remotePath += PATH_SEPARATOR + remoteFile;
        } else {
            remotePath += remoteDirectory + PATH_SEPARATOR + remoteFile;
        }
        remotePath = FtpUtils.normalize(remotePath);
        final String finalRemotePath = remotePath;
        log.debug("remotePath: " + remotePath);
        if (async) {
            Job uploader = new Job("ftp upload") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        FtpUtils.upload(connector, localFile, finalRemotePath, createRootDir, monitor);
                    } catch (FtpException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
            uploader.schedule();
        } else {
            FtpUtils.upload(connector, localFile, remotePath, createRootDir, monitor);
        }
    }

    public void download(String remoteDirectory, String remoteFile, final File localFile, boolean async, IProgressMonitor monitor) throws FtpException {
        String remotePath = connector.getRemoteDirectory();
        if (remoteDirectory == null) {
            remotePath += PATH_SEPARATOR + remoteFile;
        } else {
            remotePath += remoteDirectory + PATH_SEPARATOR + remoteFile;
        }
        remotePath = FtpUtils.normalize(remotePath);
        final String finalRemotePath = remotePath;
        log.debug("remotePath: " + remotePath);
        if (async) {
            Job downloader = new Job("ftp upload") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        FtpUtils.download(connector, localFile, finalRemotePath, monitor);
                    } catch (FtpException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
            downloader.schedule();
        } else {
            FtpUtils.download(connector, localFile, finalRemotePath, monitor);
        }
    }

    public void chmod(String remoteFile, String mode) {
        String remotePath = connector.getRemoteDirectory();
        remotePath += PATH_SEPARATOR + remoteFile;
        FTPClient ftp = new FTPClient();
        try {
            String hostname = connector.getUrl().getHost();
            ftp.connect(hostname);
            log.info("Connected to " + hostname);
            log.info(ftp.getReplyString());
            boolean loggedIn = ftp.login(connector.getUsername(), connector.getPassword());
            if (loggedIn) {
                String parameters = "chmod " + mode + " " + remotePath;
                ftp.site(parameters);
                ftp.logout();
            }
            ftp.disconnect();
        } catch (SocketException e) {
            log.error("File chmod failed with message: " + e.getMessage());
        } catch (IOException e) {
            log.error("File chmod failed with message: " + e.getMessage());
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
    }

    public void delete(String remoteFile) {
        String remotePath = connector.getRemoteDirectory();
        remotePath += PATH_SEPARATOR + remoteFile;
        FTPClient ftp = new FTPClient();
        try {
            String hostname = connector.getUrl().getHost();
            ftp.connect(hostname);
            log.info("Connected to " + hostname);
            log.info(ftp.getReplyString());
            boolean loggedIn = ftp.login(connector.getUsername(), connector.getPassword());
            if (loggedIn) {
                ftp.deleteFile(remotePath);
                ftp.logout();
            }
            ftp.disconnect();
        } catch (SocketException e) {
            log.error("File chmod failed with message: " + e.getMessage());
        } catch (IOException e) {
            log.error("File chmod failed with message: " + e.getMessage());
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
    }
}
