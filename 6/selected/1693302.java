package com.rhythm.commons.net.ftp;

import java.io.IOException;
import java.net.UnknownHostException;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

/**
 *
 * @author mlee
 */
public class SimpleFTPSClient extends SimpleFTPClient {

    /**
     * Default contstructor for a new instance of a <code>SimpleFTPSClient</code> using SSL
     */
    public SimpleFTPSClient() {
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
    @Override
    public boolean connect(String host, String userName, String password) throws IOException, UnknownHostException {
        try {
            if (ftpClient != null) if (ftpClient.isConnected()) ftpClient.disconnect();
            ftpClient = new FTPSClient("SSL", false);
            boolean success = false;
            ftpClient.connect(host);
            int reply = ftpClient.getReplyCode();
            if (FTPReply.isPositiveCompletion(reply)) success = ftpClient.login(userName, password);
            if (!success) ftpClient.disconnect();
            return success;
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }
}
