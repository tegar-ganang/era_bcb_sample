package net.yapbam.backup;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import net.yapbam.util.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

class FTPManager implements AbstractBackupManager {

    private FTPClient client;

    public FTPManager(URL location) throws IOException {
        this.client = new FTPClient();
        String host = location.getHost();
        int port = location.getPort();
        if (port < 0) {
            this.client.connect(host);
        } else {
            this.client.connect(host, port);
        }
        String[] login = StringUtils.split(location.getUserInfo(), ':');
        this.client.login(login[0], login.length > 1 ? login[1] : "");
        if (!this.client.setFileType(FTP.BINARY_FILE_TYPE)) throw new IOException("Unable to set the transfert mode");
    }

    @Override
    public void createFolder(URL folder) throws IOException {
        String path = folder.getPath();
        while (path.charAt(0) == '/') {
            path = path.substring(1);
        }
        String[] paths = StringUtils.split(path, '/');
        for (String p : paths) {
            if (!this.client.changeWorkingDirectory(p)) {
                if (!this.client.makeDirectory(p)) throw new IOException("unable to create folder " + p);
                if (!this.client.changeWorkingDirectory(p)) throw new IOException("unable to access folder " + p);
            }
        }
    }

    @Override
    public void close() {
        if (this.client.isConnected()) try {
            this.client.logout();
            this.client.disconnect();
        } catch (IOException e) {
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    @Override
    public void setTime(URL folder, String fileName, Calendar date) throws IOException {
        String folderPath = folder.getPath();
        if ((folderPath.length() == 0) || (folderPath.charAt(0) != '/')) folderPath = "/" + folderPath;
        if (!this.client.changeWorkingDirectory(folderPath)) throw new IOException("Unable to access to folder " + folderPath);
        String timeVal = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(date.getTime());
        this.client.setModificationTime(fileName, timeVal);
    }

    @Override
    public OutputStream getOutputStream(URL folder, String fileName) throws IOException {
        String folderPath = folder.getPath();
        if ((folderPath.length() == 0) || (folderPath.charAt(0) != '/')) folderPath = "/" + folderPath;
        if (!this.client.changeWorkingDirectory(folderPath)) throw new IOException("Unable to access to folder " + folderPath);
        OutputStream stream = this.client.storeFileStream(fileName);
        if (stream == null) throw new IOException("Unable to write to file " + folderPath + "/" + fileName);
        return stream;
    }

    @Override
    public void close(OutputStream out) throws IOException {
        if (!this.client.completePendingCommand()) throw new IOException();
        out.close();
    }
}
