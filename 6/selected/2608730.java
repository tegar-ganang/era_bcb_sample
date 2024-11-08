package net.narusas.cafelibrary.serial;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.logging.Logger;
import net.narusas.cafelibrary.LibraryStorage;
import net.narusas.util.lang.NOutputStream;
import org.apache.commons.net.ftp.FTPClient;

public class FTPStorage implements LibraryStorage {

    protected static Logger logger = Logger.getLogger("log");

    protected FTPClient client;

    protected FTPAccount account;

    public void write(byte[] serializedLibrary, Object param) throws IOException {
        if (param == null) {
            return;
        }
        account = (FTPAccount) param;
        connect();
        uploads(serializedLibrary);
        disconnect();
    }

    protected void connect() throws SocketException, IOException, LoginFailException {
        logger.info("Connect to FTP Server " + account.getServer());
        client = new FTPClient();
        client.connect(account.getServer());
        if (client.login(account.getId(), account.getPassword()) == false) {
            logger.info("Fail to login with id=" + account.getId());
            throw new LoginFailException(account.getId(), account.getPassword());
        }
    }

    protected void uploads(byte[] serializedLibrary) throws IOException {
        client.changeWorkingDirectory(account.getPath());
        logger.info("Start to upload");
        upload("library.xml", serializedLibrary);
    }

    protected void disconnect() throws IOException {
        client.logout();
        client.disconnect();
    }

    protected void upload(String remoteFileName, byte[] data) throws IOException {
        logger.info("Upload " + remoteFileName);
        client.storeFile(remoteFileName, new ByteArrayInputStream(data));
    }

    public InputStream read() throws IOException {
        if (account == null) {
            return null;
        }
        connect();
        byte[] data = readFromFTP();
        disconnect();
        return new ByteArrayInputStream(data);
    }

    private byte[] readFromFTP() throws IOException {
        InputStream in = client.retrieveFileStream(account.getPath() + "/library.xml");
        if (in == null) {
            return null;
        }
        BufferedInputStream bin = new BufferedInputStream(in, 4096);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        NOutputStream.leftShift(bout, bin);
        return bout.toByteArray();
    }

    protected FTPClient getClient() {
        return client;
    }
}
