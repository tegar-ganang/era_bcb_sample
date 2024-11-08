package se.vgregion.webbisar.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.StringTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class FileHandler {

    private static final Log LOGGER = LogFactory.getLog(FileHandler.class);

    private String host;

    private int port;

    private String userName;

    private String password;

    public FileHandler(String ftpConfig) {
        LOGGER.info("FTP CONFIGURATION IS: " + ftpConfig);
        StringTokenizer t = new StringTokenizer(ftpConfig, ";");
        try {
            this.host = t.nextToken();
            this.port = Integer.parseInt(t.nextToken());
            this.userName = t.nextToken();
            this.password = t.nextToken();
        } catch (NumberFormatException e) {
            LOGGER.fatal("FTP CONFIGURATION: Failed to parse!!");
        }
    }

    public void writeTempFile(String fileName, String sessionId, InputStream is) throws FTPException {
        FTPClient ftp = connect();
        try {
            ftp.makeDirectory("temp");
            ftp.changeWorkingDirectory("temp");
            ftp.makeDirectory(sessionId);
            ftp.changeWorkingDirectory(sessionId);
            ftp.storeFile(fileName, is);
            ftp.logout();
        } catch (IOException e) {
            LOGGER.error("Could not write tempfile " + fileName, e);
        } finally {
            try {
                ftp.disconnect();
            } catch (IOException e) {
            }
        }
    }

    private FTPClient connect() throws FTPException {
        try {
            FTPClient ftp = new FTPClient();
            ftp.connect(host, port);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
            }
            ftp.login(userName, password);
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            return ftp;
        } catch (SocketException e) {
            throw new FTPException("Failed to connect to server", e);
        } catch (IOException e) {
            throw new FTPException("Failed to connect to server", e);
        }
    }
}
