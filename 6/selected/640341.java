package net.sf.clairv.index.transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.commons.net.ftp.FTPClient;

/**
 * Implements an FTP transporter based on Commons Net.
 * 
 * @author qiuyin
 * 
 */
public class FtpTransporter extends Transporter {

    public void transport(File file) throws TransportException {
        FTPClient client = new FTPClient();
        try {
            client.connect(getOption("host"));
            client.login(getOption("username"), getOption("password"));
            client.changeWorkingDirectory(getOption("remotePath"));
            transportRecursive(client, file);
            client.disconnect();
        } catch (Exception e) {
            throw new TransportException(e);
        }
    }

    public void transportRecursive(FTPClient client, File file) throws IOException {
        if (file.isFile() && file.canRead()) {
            client.storeFile(file.getName(), new FileInputStream(file));
        } else if (file.isDirectory()) {
            client.makeDirectory(file.getName());
            client.changeWorkingDirectory(file.getName());
            File[] fileList = file.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                transportRecursive(client, fileList[i]);
            }
        }
    }
}
