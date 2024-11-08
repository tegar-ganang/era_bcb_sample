package org.one.stone.soup.wiki.ftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.Util;
import org.one.stone.soup.exception.ExceptionHelper;
import org.one.stone.soup.stringhelper.StringArrayHelper;
import org.one.stone.soup.wiki.jar.manager.SystemAPI;

public class FtpAPI extends SystemAPI {

    private Hashtable connections = new Hashtable();

    public FTPClient connect(String host, String userId, String password, String alias) throws IOException {
        FTPClient client = null;
        if (connections.get(alias) != null) {
            client = (FTPClient) connections.get(alias);
            if (client.isConnected() == false) {
                client.connect(host);
            }
        } else {
            client = new FTPClient();
            client.connect(host);
            client.login(userId, password);
            connections.put(alias, client);
        }
        return client;
    }

    public void disconnect(String alias) throws IOException {
        FTPClient client = (FTPClient) connections.get(alias);
        client.disconnect();
        connections.remove(alias);
    }

    public String listFiles(String alias, String remoteDirectory) throws Exception {
        String data = null;
        try {
            FTPClient client = (FTPClient) connections.get(alias);
            client.cwd(remoteDirectory);
            FTPFile[] files = client.listFiles();
            data = "List:<br/>";
            for (int loop = 0; loop < files.length; loop++) {
                data += "  " + files[loop].getName() + "<br/>";
            }
        } catch (Exception e) {
            String[] stack = ExceptionHelper.getStackTrace(e);
            return StringArrayHelper.arrayToString(stack, "<br/>");
        }
        return data;
    }

    public String downloadFile(String alias, String remoteDirectory, String remoteFile, String pageName, String attachmentName) throws Exception {
        try {
            FTPClient client = (FTPClient) connections.get(alias);
            client.cwd(remoteDirectory);
            InputStream iStream = client.retrieveFileStream(remoteFile);
            OutputStream oStream = this.getBuilder().getFileManager().getAttachmentOutputStream(pageName, attachmentName, this.getBuilder().getSystemLogin());
            Util.copyStream(iStream, oStream);
            iStream.close();
            oStream.close();
            client.completePendingCommand();
        } catch (Exception e) {
            String[] stack = ExceptionHelper.getStackTrace(e);
            return StringArrayHelper.arrayToString(stack, "<br/>");
        }
        return "";
    }

    public String downloadFile(String alias, String remoteDirectory, String remoteFile, File file) throws Exception {
        try {
            FTPClient client = (FTPClient) connections.get(alias);
            client.cwd(remoteDirectory);
            InputStream iStream = client.retrieveFileStream(remoteFile);
            OutputStream oStream = new FileOutputStream(file);
            Util.copyStream(iStream, oStream);
            iStream.close();
            oStream.close();
            client.completePendingCommand();
        } catch (Exception e) {
            String[] stack = ExceptionHelper.getStackTrace(e);
            return StringArrayHelper.arrayToString(stack, "<br/>");
        }
        return "";
    }
}
