package com.sardak.blogoommer.ui.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

/**
 * FTP transfer utility.
 * @author Ren� Ghosh
 * 12 nov. 2004
 */
public class FileIO {

    private static final boolean DEBUG = false;

    public static FTPClient getClient(String serverAddress, String login, String password, boolean PASV) throws SocketException, IOException {
        FTPClient client = new FTPClient();
        client.connect(serverAddress);
        if (PASV) {
            client.enterLocalPassiveMode();
        }
        client.login(login, password);
        return client;
    }

    /**
	 * transfer a file to the remote server
	 */
    public static void move(String localFilePath, String remoteFile, String remoteDir, FTPClient client) throws SocketException, IOException {
        if (DEBUG) {
            System.out.println("Storing " + localFilePath + " as " + remoteFile + " in " + remoteDir + " @ " + client.getRemoteAddress());
        }
        client.makeDirectory(remoteDir);
        client.changeWorkingDirectory(remoteDir);
        client.setFileType(FTP.BINARY_FILE_TYPE);
        File localFile = new File(localFilePath);
        if (localFile.exists()) {
            client.storeFile(remoteFile, new FileInputStream(localFile));
        }
    }

    /**
	 * suppress a file in remote server
	 */
    public static void suppress(String remoteFile, String remoteDir, FTPClient client) throws SocketException, IOException {
        client.changeWorkingDirectory(remoteDir);
        client.deleteFile(remoteDir + "/" + remoteFile);
    }

    public static void main(String[] args) {
        try {
            FTPClient client = getClient("ftpperso.free.fr", "rghosh", "oldg1psi", true);
            move("C:/tmp/test.txt", "test.txt", "/", client);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
