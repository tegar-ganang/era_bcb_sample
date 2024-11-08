package nl.langits.util.remote.spi;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import javax.naming.CompoundName;
import nl.langits.util.remote.IRemoteRepositoryHandler;

/**
 * A remote repository handler for the ftp protocol.
 * <br>
 * Copyright (c) 2004 by Niels Lang <br>
 * License of use: <a href="http://www.gnu.org/copyleft/lesser.html">Lesser
 * General Public License (LGPL) </a>, no warranty <br>
 * 
 * @author Niels Lang, mailto:nlang@gmx.net .
 */
public class FTPRepositoryHandler implements IRemoteRepositoryHandler {

    private final URL FTP_URL;

    public FTPRepositoryHandler(URL remoteRepositoryURL) {
        if (!remoteRepositoryURL.getProtocol().equals("ftp")) throw new IllegalArgumentException("No ftp url provided: " + remoteRepositoryURL);
        FTP_URL = remoteRepositoryURL;
    }

    /**
     * @see nl.langits.util.remote.IRemoteRepositoryHandler#putFile(javax.naming.CompoundName, java.io.FileInputStream)
     */
    public void putFile(CompoundName file, FileInputStream fileInput) throws IOException {
        System.out.println("PUTTING file " + file);
        createDirs(file, 0);
        FTPClient client = initFTPSession();
        for (int i = 0; i < file.size() - 1; i++) client.cd(file.get(i));
        OutputStream outStream = client.put(file.get(file.size() - 1));
        for (int byteIn = fileInput.read(); byteIn != -1; byteIn = fileInput.read()) outStream.write(byteIn);
        fileInput.close();
        outStream.close();
        client.closeServer();
    }

    /**
     * Recursively mkdir's all dirs, if necessary. 
     */
    protected void createDirs(CompoundName file, int startLevel) throws IOException {
        FTPClient ftpClient = initFTPSession();
        for (int i = 0; i < startLevel; i++) ftpClient.cd(file.get(i));
        for (int i = startLevel; i < file.size() - 1; i++) {
            try {
                System.out.println("Trying CD to " + file.get(i));
                ftpClient.cd(file.get(i));
            } catch (Exception e) {
                ftpClient.closeServer();
                ftpClient = initFTPSession();
                for (int y = 0; y < i; y++) ftpClient.cd(file.get(y));
                System.out.println("Trying MKD " + file.get(i));
                ftpClient.mkdir(file.get(i));
                ftpClient.closeServer();
                createDirs(file, i);
            }
        }
        ftpClient.closeServer();
    }

    protected FTPClient initFTPSession() throws IOException {
        FTPClient result = new FTPClient(FTP_URL.getHost());
        String userInfo = FTP_URL.getUserInfo();
        String user = userInfo;
        String pw = null;
        if (userInfo.indexOf(':') != -1) {
            user = userInfo.substring(0, userInfo.indexOf(':'));
            pw = userInfo.substring(user.length() + 1);
        }
        result.login(user, pw);
        result.cd(FTP_URL.getPath());
        return result;
    }
}
