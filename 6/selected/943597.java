package com.dna.motion.tools;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPListParseEngine;

public class FTPSynchronizer {

    private FTPClient client = null;

    public void synchronize(String ftpServer, String user, String password) throws SocketException, IOException {
        client = new FTPClient();
        client.connect(ftpServer);
        client.login(user, password);
        this.loadContents("/");
    }

    private void loadContents(String directory) throws IOException {
        FTPListParseEngine engine = client.initiateListParsing(directory);
        client.listNames();
        this.saveContents(engine);
    }

    private void saveContents(FTPListParseEngine engine) throws IOException {
        while (engine.hasNext()) {
            FTPFile[] files = engine.getFiles();
            this.saveContentsFactory(files);
        }
    }

    private void saveContentsFactory(FTPFile[] files) throws IOException {
        for (FTPFile ftpFile : files) {
            if (ftpFile.isFile()) {
                this.saveContent(ftpFile);
            } else {
                this.loadContents(ftpFile.getLink());
            }
        }
    }

    private void saveContent(FTPFile ftpFile) throws IOException {
        OutputStream outStream = new FileOutputStream(ftpFile.getName());
        client.retrieveFile(ftpFile.getLink(), outStream);
    }
}
