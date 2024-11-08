package com.simpleftp.ftp.server.junit;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.net.ftp.FTPClient;
import com.simpleftp.ftp.server.FtpServer;
import com.simpleftp.ftp.server.client.ui.ClientController;
import com.simpleftp.ftp.server.utils.FtpConstants;
import junit.framework.TestCase;

public class FtpServerTestBase extends TestCase {

    protected FtpServer server;

    protected FTPClient client;

    protected ClientController adminClientCtrl;

    protected TestView responseReceiver;

    public FtpServerTestBase(String testCase) {
        super(testCase);
        server = new FtpServer();
        client = new FTPClient();
        responseReceiver = new TestView();
        adminClientCtrl = new ClientController(responseReceiver);
    }

    public void setUp() throws Exception {
        super.setUp();
        adminClientCtrl.performAction("USER admin");
        adminClientCtrl.performAction("PASS password");
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public FtpServer getServer() {
        return server;
    }

    public void login(String username, String password) throws IOException {
        client.login(username, password);
    }

    public boolean put(String localFile, String remote) {
        boolean result = false;
        try {
            InputStream in = this.getClass().getClassLoader().getResourceAsStream(localFile);
            result = client.storeFile(remote, in);
        } catch (IOException e) {
            return false;
        }
        return result;
    }

    public void get(File localFile, String remote, int mode) {
        try {
            client.setFileType(mode);
            InputStream inRemote = client.retrieveFileStream(remote);
            OutputStream localOut = new FileOutputStream(localFile);
            byte[] buffer = new byte[FtpConstants.bufferSize];
            while (true) {
                int read = inRemote.read(buffer, 0, buffer.length);
                if (read == -1) break;
                localOut.write(buffer, 0, read);
            }
            localOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
