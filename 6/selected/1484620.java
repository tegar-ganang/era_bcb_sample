package org.curjent.example.agent.ftpxfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.curjent.example.agent.logger.Logger;

/**
 * FTP functionality. Wraps the apache commons FTP client and adds functionality
 * specific to this example.
 */
class FTPTransferClient {

    private final String remote;

    private final String local;

    private FTPClient ftp;

    private static final Logger logger = Logger.INSTANCE;

    FTPTransferClient(String remote, String local) {
        this.remote = remote;
        this.local = local;
    }

    String connect() throws IOException {
        String reply = null;
        if (ftp == null) {
            FTPClient ftp = new FTPClient();
            ftp.connect(remote);
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                throw new IOException("Connection failed: " + remote);
            }
            reply = ftp.getReplyString();
            if (!ftp.login("anonymous", "")) {
                throw new IOException("Login failed: " + remote);
            }
            if (!ftp.setFileType(FTP.BINARY_FILE_TYPE)) {
                throw new IOException("Setting binary file type failed: " + remote);
            }
            this.ftp = ftp;
        }
        return reply;
    }

    void disconnect() throws IOException {
        if (ftp != null) {
            FTPClient ftp = this.ftp;
            this.ftp = null;
            ftp.disconnect();
        }
    }

    FTPFile[] list(String dir) throws IOException {
        connect();
        FTPFile[] files = ftp.listFiles(dir);
        if (files == null) {
            throw new IOException("List files failed: " + dir);
        }
        for (FTPFile file : files) {
            if (file == null) {
                throw new IOException("List files failed: " + dir);
            }
        }
        return files;
    }

    void retrieve(String remote) throws IOException {
        connect();
        File local = local(remote);
        FileOutputStream fos = new FileOutputStream(local);
        try {
            ftp.retrieveFile(remote, fos);
            fos.flush();
            fos.close();
            fos = null;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                    local.delete();
                } catch (Throwable exception) {
                    logger.log("File cleanup failed: " + local, exception);
                }
            }
        }
    }

    private File local(String remote) throws IOException {
        File local = new File(this.local, remote.substring(1));
        File parent = local.getParentFile();
        if (parent == null || (!parent.mkdirs() && !parent.isDirectory())) {
            throw new IOException("Make directories failed: " + local);
        }
        return local;
    }
}
