package eu.vph.predict.vre.in_silico.util.vfs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * Utility to assist FTP file transfer.
 *
 */
public class FTPUtil {

    private FTPClient ftpClient;

    private static final Log log = LogFactory.getLog(FTPUtil.class);

    /**
   * Set up anonymous connection to server.
   * 
   * @param server Server to connect to.
   */
    public FTPUtil(final String server) {
        log.debug("~ftp.FTPUtil() : Creating object");
        ftpClient = new FTPClient();
        try {
            ftpClient.connect(server);
            ftpClient.login("anonymous", "");
            ftpClient.setConnectTimeout(120000);
            ftpClient.setSoTimeout(120000);
            final int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                final String errMsg = "Non-positive completion connecting FTPClient";
                log.warn("~ftp.FTPUtil() : [" + errMsg + "]");
            }
        } catch (IOException ioe) {
            final String errMsg = "Cannot connect and login to ftpClient [" + ioe.getMessage() + "]";
            log.warn("~ftp.FTPUtil() : [" + errMsg + "]");
            ioe.printStackTrace();
        }
    }

    /**
   * Retrieve a directory listing.
   * 
   * @param ftpLocation Directory location.
   * @param regexPattern Filtering results pattern (or null for no filtering).
   * @param sorted Ordered results.
   * @return Directory listing.
   */
    public List<String> directoryListing(final String ftpLocation, final String regexPattern, final boolean sorted) {
        log.debug("~ftp.directoryListing(String) : Listing for [" + ftpLocation + "]");
        final Pattern pattern = (null == regexPattern ? null : Pattern.compile(regexPattern));
        final List<String> directoryListing = new ArrayList<String>();
        try {
            final FTPFile[] ftpFiles = ftpClient.listFiles(ftpLocation);
            log.debug("~ftp.directoryListing(String) : Listed [" + ftpFiles.length + "]");
            for (int fileIdx = 0; fileIdx < ftpFiles.length; fileIdx++) {
                final String fileName = ftpFiles[fileIdx].getName();
                log.debug("~ftp.directoryListing(String) : Listed file name [" + fileName + "]");
                if (pattern != null) {
                    if (pattern.matcher(fileName).find()) {
                        directoryListing.add(fileName);
                    } else {
                        log.debug("~ftp.directoryListing(String) : Rejected as unmatched pattern");
                    }
                } else {
                    directoryListing.add(fileName);
                }
            }
            if (sorted && directoryListing.size() > 1) Collections.sort(directoryListing);
        } catch (IOException ioe) {
            log.warn("~ftp.directoryListing(String) : IOException [" + ioe.getMessage() + "]");
            ioe.printStackTrace();
        } finally {
            doFinally();
        }
        return directoryListing;
    }

    /**
   * Retrieve an ASCII file from FTP and return is as a string
   * 
   * @param fileFTPLocation File location on FTP server.
   * @return retrieved file as string (or null if file not found)
   */
    public String downloadASCIIFileAsString(final String fileFTPLocation) {
        log.debug("~ftp.downloadASCIIFileAsString(String) : Request to download [" + fileFTPLocation + "]");
        String returnString = null;
        try {
            ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
            final OutputStream outputStream = new ByteArrayOutputStream();
            final FTPFile[] ftpFiles = ftpClient.listFiles(fileFTPLocation);
            if (ftpFiles.length > 0) {
                log.debug("~ftp.downloadASCIIFileAsString(String) : [" + ftpFiles.length + "] files found for [" + fileFTPLocation + "]");
                ftpClient.retrieveFile(fileFTPLocation, outputStream);
                outputStream.flush();
                outputStream.close();
                returnString = outputStream.toString();
            } else {
                log.debug("~ftp.downloadASCIIFileAsString(String) : [" + fileFTPLocation + "] file not found");
            }
        } catch (IOException ioe) {
            log.warn("~ftp.downloadASCIIFileAsString(String) : Failed to download .. [" + ioe.getMessage() + "]");
            ioe.printStackTrace();
        } finally {
            doFinally();
        }
        return returnString;
    }

    /**
   * Common tidy-up routine.
   * 
   * @param ftpClient
   */
    private void doFinally() {
        log.debug("~doFinally() : Closing connection");
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                final String msg = "Disconnecting FTPClient";
                log.debug("~ftp.doFinally() : [" + msg + "]");
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException ioe) {
                final String errMsg = "IOException disconnecting FTPClient [" + ioe.getMessage() + "]";
                log.warn("~ftp.doFinally() : [" + errMsg + "]");
            }
        }
    }
}
