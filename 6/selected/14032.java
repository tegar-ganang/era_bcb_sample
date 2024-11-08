package org.mockftpserver.stub.example;

import org.apache.commons.net.ftp.FTPClient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Simple FTP client code example.
 *
 * @author Chris Mair
 * @version $Revision: 151 $ - $Date: 2008-11-07 20:24:38 -0500 (Fri, 07 Nov 2008) $
 */
public class RemoteFile {

    public static final String USERNAME = "user";

    public static final String PASSWORD = "password";

    private String server;

    private int port;

    public String readFile(String filename) throws IOException {
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(server, port);
        ftpClient.login(USERNAME, PASSWORD);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean success = ftpClient.retrieveFile(filename, outputStream);
        ftpClient.disconnect();
        if (!success) {
            throw new IOException("Retrieve file failed: " + filename);
        }
        return outputStream.toString();
    }

    /**
     * Set the hostname of the FTP server
     *
     * @param server - the hostname of the FTP server
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * Set the port number for the FTP server
     *
     * @param port - the port number
     */
    public void setPort(int port) {
        this.port = port;
    }
}
