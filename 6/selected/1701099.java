package org.regilo.ftp.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.regilo.ftp.client.exceptions.FtpException;
import org.regilo.ftp.model.FtpSiteConnector;

public class FtpUtils {

    private static final Logger log = Logger.getLogger(FtpUtils.class);

    /**
	 * 
	 * 
	 * @param connector
	 * @param localFile
	 * @param remotePath
	 * @param createRootDir
	 * @param monitor
	 * @throws FtpException
	 */
    protected static void upload(FtpSiteConnector connector, File localFile, String remotePath, boolean createRootDir, IProgressMonitor monitor) throws FtpException {
        FTPClient ftp = new FTPClient();
        try {
            String hostname = connector.getUrl().getHost();
            ftp.connect(hostname);
            log.info("Connected to " + hostname);
            log.info(ftp.getReplyString());
            boolean loggedIn = ftp.login(connector.getUsername(), connector.getPassword());
            if (loggedIn) {
                log.info("User " + connector.getUsername() + " logged in");
                ftp.setFileTransferMode(FTPClient.BINARY_FILE_TYPE);
                ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
                ftp.enterLocalPassiveMode();
                FtpUtils.store(ftp, localFile, remotePath, createRootDir, monitor);
                ftp.logout();
            } else {
                throw new FtpException("Invalid login");
            }
            ftp.disconnect();
        } catch (Exception e) {
            log.error("File upload failed with message: " + e.getMessage());
            throw new FtpException("File upload failed with message: " + e.getMessage());
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                    throw new FtpException("File upload failed with message: " + ioe.getMessage());
                }
            }
        }
    }

    /**
	 * 
	 * 
	 * @param connector
	 * @param localFile
	 * @param remotePath
	 * @param monitor
	 * @throws FtpException
	 */
    protected static void download(FtpSiteConnector connector, File localFile, String remotePath, final IProgressMonitor monitor) throws FtpException {
        if (!localFile.exists()) {
            FTPClient ftp = new FTPClient();
            try {
                FTPClientConfig conf = new FTPClientConfig(FTPClientConfig.SYST_UNIX);
                ftp.configure(conf);
                String hostname = connector.getUrl().getHost();
                ftp.connect(hostname);
                log.info("Connected to " + hostname);
                log.info(ftp.getReplyString());
                boolean loggedIn = ftp.login(connector.getUsername(), connector.getPassword());
                if (loggedIn) {
                    log.info("downloading file: " + remotePath);
                    ftp.setFileTransferMode(FTPClient.BINARY_FILE_TYPE);
                    ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
                    ftp.enterLocalPassiveMode();
                    final long fileSize = getFileSize(ftp, remotePath);
                    FileOutputStream dfile = new FileOutputStream(localFile);
                    ftp.retrieveFile(remotePath, dfile, new CopyStreamListener() {

                        public int worked = 0;

                        public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                            int percent = percent(fileSize, totalBytesTransferred);
                            int delta = percent - worked;
                            if (delta > 0) {
                                if (monitor != null) {
                                    monitor.worked(delta);
                                }
                                worked = percent;
                            }
                        }

                        public void bytesTransferred(CopyStreamEvent event) {
                        }

                        private int percent(long totalBytes, long totalBytesTransferred) {
                            long percent = (totalBytesTransferred * 100) / totalBytes;
                            return Long.valueOf(percent).intValue();
                        }
                    });
                    dfile.flush();
                    dfile.close();
                    ftp.logout();
                } else {
                    throw new FtpException("Invalid login");
                }
                ftp.disconnect();
            } catch (SocketException e) {
                log.error("File download failed with message: " + e.getMessage());
                throw new FtpException("File download failed with message: " + e.getMessage());
            } catch (IOException e) {
                log.error("File download failed with message: " + e.getMessage());
                throw new FtpException("File download failed with message: " + e.getMessage());
            } finally {
                if (ftp.isConnected()) {
                    try {
                        ftp.disconnect();
                    } catch (IOException ioe) {
                        throw new FtpException("File download failed with message: " + ioe.getMessage());
                    }
                }
            }
        }
    }

    /**
	 * 
	 * 
	 * @param ftp
	 * @param localFile
	 * @param remotePath
	 * @param createDir
	 * @param monitor
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws FtpException
	 */
    protected static void store(FTPClient ftp, File localFile, String remotePath, boolean createDir, final IProgressMonitor monitor) throws FileNotFoundException, IOException, FtpException {
        if (localFile.isFile()) {
            if (monitor != null) {
                monitor.subTask(localFile.getName());
            }
            boolean stored = ftp.storeFile(remotePath, new FileInputStream(localFile), new CopyStreamListener() {

                public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                    if (monitor != null) {
                        monitor.worked(1);
                    }
                }

                public void bytesTransferred(CopyStreamEvent event) {
                }
            });
            if (!stored) {
                throw new FtpException("Cannot store " + localFile.getName() + " to " + remotePath);
            }
        } else if (localFile.isDirectory()) {
            String newDirectory = remotePath;
            if (createDir) {
                newDirectory = remotePath;
                ftp.makeDirectory(newDirectory);
            }
            String[] files = localFile.list();
            for (int i = 0; i < files.length; i++) {
                store(ftp, new File(localFile, files[i]), newDirectory + FtpClient.PATH_SEPARATOR + files[i], true, monitor);
            }
        }
    }

    /**
	 * Check path for correctness
	 * 
	 * @param remotePath
	 * @return
	 */
    protected static String normalize(String remotePath) {
        return remotePath;
    }

    /**
	 * Return the size of a remote file
	 * 
	 * @param ftp
	 * @param remoteFile
	 * @return
	 */
    protected static long getFileSize(FTPClient ftp, String remoteFile) {
        long size = 0;
        try {
            FTPFile[] files = ftp.listFiles(remoteFile);
            if (files.length == 1) {
                FTPFile ftpFile = files[0];
                if (ftpFile != null) {
                    size = ftpFile.getSize();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return size;
    }
}
