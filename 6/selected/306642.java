package at.ac.univie.zsu.aguataplan.video.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.CopyStreamAdapter;
import org.apache.commons.net.io.CopyStreamException;
import org.apache.commons.net.io.Util;
import org.apache.log4j.Logger;
import at.ac.univie.zsu.aguataplan.exception.FtpHandlingException;
import at.ac.univie.zsu.aguataplan.util.Constants;
import at.ac.univie.zsu.aguataplan.video.IVideoService;

/**
 * @author gerry
 * 
 */
public class FtpHandling implements Constants {

    private static Logger log = Logger.getLogger(FtpHandling.class);

    private IVideoService iVideoService;

    private FTPClient ftp;

    private long size;

    private String server;

    private int port;

    private String username;

    private String password;

    public FtpHandling(IVideoService iVideoService, String server, int port, String username, String password) {
        super();
        this.iVideoService = iVideoService;
        this.server = server;
        this.port = port;
        this.username = username;
        this.password = password;
        ftp = new FTPClient();
    }

    /**
	 * connect to ftp server
	 * 
	 * @return true if connected to server otherwise false
	 */
    private int connect() {
        if (ftp.isConnected()) {
            log.debug("Already connected to: " + getConnectionString());
            return RET_OK;
        }
        try {
            ftp.connect(server, port);
            ftp.login(username, password);
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
        } catch (SocketException e) {
            log.error(e.toString());
            return RET_ERR_SOCKET;
        } catch (UnknownHostException e) {
            log.error(e.toString());
            return RET_ERR_UNKNOWN_HOST;
        } catch (FTPConnectionClosedException e) {
            log.error(e.toString());
            return RET_ERR_FTP_CONN_CLOSED;
        } catch (IOException e) {
            log.error(e.toString());
            return RET_ERR_IO;
        }
        if (ftp.isConnected()) {
            log.debug("Connected to " + getConnectionString());
            return RET_OK;
        }
        log.debug("Could not connect to " + getConnectionString());
        return RET_ERR_NOT_CONNECTED;
    }

    /**
	 * disconnect from ftp server
	 */
    private void disconnect() {
        if (!ftp.isConnected()) {
            return;
        }
        String s = getFTPServerInformationSimple();
        try {
            ftp.disconnect();
        } catch (IOException e) {
            log.error(e.toString());
        }
        log.debug("disconnected from " + s);
    }

    /**
	 * get information about ftp connection or an empty string if client is not
	 * connected
	 * 
	 * @return
	 */
    private String getFTPServerInformationSimple() {
        if (!ftp.isConnected()) {
            return "";
        }
        StringBuffer sb = new StringBuffer("");
        InetAddress remoteAddress = ftp.getRemoteAddress();
        int remotePort = ftp.getRemotePort();
        sb.append(remoteAddress.toString() + ":" + remotePort);
        return sb.toString();
    }

    /**
	 * get servername and port
	 * 
	 * @return
	 */
    private String getConnectionString() {
        return server + ":" + port;
    }

    /**
	 * upload file to ftp server
	 * 
	 * @param dirInput
	 *            local directory
	 * @param fileInput
	 *            local file
	 * @param dirOutput
	 *            remote directory
	 * @param fileOutput
	 *            remote file
	 * @throws FtpHandlingException
	 */
    public synchronized void upload(String dirInput, String fileInput, String dirOutput, String fileOutput) throws FtpHandlingException {
        int retVal;
        if ((retVal = connect()) != RET_OK) {
            throw new FtpHandlingException("Could not connect to ftp server", retVal);
        }
        long t;
        t = System.currentTimeMillis();
        String inFile = dirInput + Sep + fileInput;
        String outFile = dirOutput + Sep + fileOutput;
        try {
            ftp.makeDirectory(dirOutput);
            InputStream is = new FileInputStream(inFile);
            OutputStream os = ftp.storeFileStream(outFile);
            int bs = ftp.getBufferSize();
            File fs = new File(inFile);
            final long length = fs.length();
            size = length / 10;
            Util.copyStream(is, os, bs, length, new CopyStreamAdapter() {

                @Override
                public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                    if (totalBytesTransferred > size) {
                        size = size + length / 10;
                        double d = (double) totalBytesTransferred / streamSize;
                        log.debug("% " + d);
                        int progress = (int) Math.floor(d * 100);
                        if (iVideoService != null) {
                            iVideoService.setProgressFtp((int) Math.floor(progress));
                        }
                    }
                }
            });
            os.close();
            is.close();
        } catch (SecurityException e) {
            throw new FtpHandlingException(e.toString(), RET_ERR_SECURITY);
        } catch (FileNotFoundException e) {
            throw new FtpHandlingException(e.toString(), RET_ERR_FILE_NOT_FOUND);
        } catch (FTPConnectionClosedException e) {
            throw new FtpHandlingException(e.toString(), RET_ERR_FTP_CONN_CLOSED);
        } catch (CopyStreamException e) {
            throw new FtpHandlingException(e.toString(), RET_ERR_COPY_STREAM);
        } catch (IOException e) {
            throw new FtpHandlingException(e.toString(), RET_ERR_IO);
        }
        t = (System.currentTimeMillis() - t);
        log.info(inFile + " --> " + outFile + " done in " + t + "ms");
        disconnect();
    }

    /**
	 * remove file from ftp server
	 * 
	 * @param dirInput
	 * @param fileInput
	 * @throws FtpHandlingException
	 */
    public synchronized void delete(String dirInput, String fileInput) throws FtpHandlingException {
        int retVal;
        if ((retVal = connect()) != RET_OK) {
            throw new FtpHandlingException("Could not connect to ftp server", retVal);
        }
        long t;
        t = System.currentTimeMillis();
        String inFile = dirInput + Sep + fileInput;
        boolean deleted;
        try {
            deleted = ftp.deleteFile(inFile);
            if (!deleted) {
                throw new FtpHandlingException(inFile + " not deleted!", RET_ERR_FILE_DELETE_FAILED);
            }
        } catch (IOException e) {
            throw new FtpHandlingException(e.toString(), RET_ERR_FILE_DELETE_FAILED);
        }
        t = (System.currentTimeMillis() - t);
        log.info(inFile + " deleted in " + t + "ms");
        disconnect();
    }

    public synchronized void download(String dirInput, String fileInput, String dirOutput, String fileOutput) throws FtpHandlingException {
        int retVal;
        if ((retVal = connect()) != RET_OK) {
            throw new FtpHandlingException("Could not connect to ftp server", retVal);
        }
        long t;
        t = System.currentTimeMillis();
        String inFile = dirInput + Sep + fileInput;
        String outFile = dirOutput + Sep + fileOutput;
        try {
            InputStream is = ftp.retrieveFileStream(inFile);
            OutputStream os = new FileOutputStream(outFile);
            int bs = ftp.getBufferSize();
            FTPFile file = null;
            file = ftp.mlistFile(inFile);
            final long length = file.getSize();
            size = length / 10;
            Util.copyStream(is, os, bs, length, new CopyStreamAdapter() {

                @Override
                public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                    if (totalBytesTransferred > size) {
                        size = size + length / 10;
                        double d = (double) totalBytesTransferred / streamSize;
                        log.debug("% " + d);
                    }
                }
            });
            os.close();
            is.close();
        } catch (SecurityException e) {
            throw new FtpHandlingException(e.toString(), RET_ERR_SECURITY);
        } catch (FileNotFoundException e) {
            throw new FtpHandlingException(e.toString(), RET_ERR_FILE_NOT_FOUND);
        } catch (FTPConnectionClosedException e) {
            throw new FtpHandlingException(e.toString(), RET_ERR_FTP_CONN_CLOSED);
        } catch (CopyStreamException e) {
            throw new FtpHandlingException(e.toString(), RET_ERR_COPY_STREAM);
        } catch (IOException e) {
            throw new FtpHandlingException(e.toString(), RET_ERR_IO);
        }
        t = (System.currentTimeMillis() - t);
        log.info(inFile + " --> " + outFile + " done in " + t + "ms");
        disconnect();
    }
}
