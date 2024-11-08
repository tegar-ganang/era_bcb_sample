package org.anuta.imdb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * IMDB ftp downloader. Will download file (if there is new one available, and gunzip it
 * @author fedor
 */
public class IMDBDownloader {

    private String ratingsUrl = "ftp://ftp.fu-berlin.de/pub/misc/movies/database/ratings.list.gz";

    private String userName = "anonymous";

    private String password = "grabber@mythtv.org";

    private String outputFile = "ratings.list.gz";

    private String unzippedOutputFile = "ratings.list";

    public static final int RESULT_OK = 0;

    public static final int RESULT_TRANSFER_ERROR = 1;

    public static final int RESULT_NO_NEW_FILE = 2;

    public static final int RESULT_LOGIN_ERROR = 3;

    public static final int RESULT_CONNECTION_REFUSED = 4;

    public static final int RESULT_ERROR = 5;

    private static final Log log = LogFactory.getLog(IMDBDownloader.class);

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public String getRatingsUrl() {
        return ratingsUrl;
    }

    public void setRatingsUrl(String ratingsUrl) {
        this.ratingsUrl = ratingsUrl;
    }

    public int download() {
        FTPClient client = null;
        URL url = null;
        try {
            client = new FTPClient();
            url = new URL(ratingsUrl);
            if (log.isDebugEnabled()) log.debug("Downloading " + url);
            client.connect(url.getHost());
            int reply = client.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                if (log.isErrorEnabled()) log.error("Connection to " + url + " refused");
                return RESULT_CONNECTION_REFUSED;
            }
            if (log.isDebugEnabled()) log.debug("Logging in  l:" + getUserName() + " p:" + getPassword());
            client.login(getUserName(), getPassword());
            client.changeWorkingDirectory(url.getPath());
            FTPFile[] files = client.listFiles(url.getPath());
            if ((files == null) || (files.length != 1)) throw new FileNotFoundException("No remote file");
            FTPFile remote = files[0];
            if (log.isDebugEnabled()) log.debug("Remote file data: " + remote);
            File local = new File(getOutputFile());
            if (local.exists()) {
                if ((local.lastModified() == remote.getTimestamp().getTimeInMillis())) {
                    if (log.isDebugEnabled()) log.debug("File " + local.getAbsolutePath() + " is not changed on the server");
                    return RESULT_NO_NEW_FILE;
                }
            }
            if (log.isDebugEnabled()) log.debug("Setting binary transfer modes");
            client.mode(FTPClient.BINARY_FILE_TYPE);
            client.setFileType(FTPClient.BINARY_FILE_TYPE);
            OutputStream fos = new FileOutputStream(local);
            boolean result = client.retrieveFile(url.getPath(), fos);
            if (log.isDebugEnabled()) log.debug("The transfer result is :" + result);
            fos.flush();
            fos.close();
            local.setLastModified(remote.getTimestamp().getTimeInMillis());
            if (result) uncompress();
            if (result) return RESULT_OK; else return RESULT_TRANSFER_ERROR;
        } catch (MalformedURLException e) {
            return RESULT_ERROR;
        } catch (SocketException e) {
            return RESULT_ERROR;
        } catch (FileNotFoundException e) {
            return RESULT_ERROR;
        } catch (IOException e) {
            return RESULT_ERROR;
        } finally {
            if (client != null) {
                try {
                    if (log.isDebugEnabled()) log.debug("Logging out");
                    client.logout();
                } catch (Exception e) {
                }
                try {
                    if (log.isDebugEnabled()) log.debug("Disconnecting");
                    client.disconnect();
                } catch (Exception e) {
                }
            }
        }
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private void uncompress() throws FileNotFoundException, IOException {
        if (log.isDebugEnabled()) log.debug("Uncompressing gzip file");
        GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(getOutputFile()));
        OutputStream out = new FileOutputStream(getUnzippedOutputFile());
        byte[] buf = new byte[1024];
        int len;
        while ((len = gzipInputStream.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        gzipInputStream.close();
        out.close();
        if (log.isDebugEnabled()) log.debug("All done. File is uncompressed ok");
    }

    public String getUnzippedOutputFile() {
        return unzippedOutputFile;
    }

    public void setUnzippedOutputFile(String unzippedOutputFile) {
        this.unzippedOutputFile = unzippedOutputFile;
    }
}
