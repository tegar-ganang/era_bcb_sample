package architecture.ext.sync.ftp;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import architecture.ext.sync.client.DataSyncClient;
import architecture.ext.sync.client.DataSyncException;

public class FTPDataSyncClient implements DataSyncClient {

    private Log log = LogFactory.getLog(getClass());

    private FTPClient ftp = new FTPClient();

    private String hostname;

    private int port;

    private String username;

    private String password;

    private String directory = null;

    private String localDirectory;

    public List<Map> getDataAsList(String entityName) {
        return null;
    }

    public String getLocalDirectory() {
        return localDirectory;
    }

    public void setLocalDirectory(String localDirectory) {
        this.localDirectory = localDirectory;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getWorkingDirectory() {
        return directory;
    }

    public void setWorkingDirectory(String directory) {
        this.directory = directory;
    }

    public void setAutodetectUTF8(boolean auto) {
        ftp.setAutodetectUTF8(true);
    }

    public void retrieveFiles() throws DataSyncException {
        try {
            ftp.connect(hostname, port);
            boolean success = ftp.login(username, password);
            log.info("FTP Login:" + success);
            if (success) {
                System.out.println(directory);
                ftp.changeWorkingDirectory(directory);
                ftp.setFileType(FTP.ASCII_FILE_TYPE);
                ftp.enterLocalPassiveMode();
                ftp.setRemoteVerificationEnabled(false);
                FTPFile[] files = ftp.listFiles();
                for (FTPFile file : files) {
                    ftp.setFileType(file.getType());
                    log.debug(file.getName() + "," + file.getSize());
                    FileOutputStream output = new FileOutputStream(localDirectory + file.getName());
                    try {
                        ftp.retrieveFile(file.getName(), output);
                    } finally {
                        IOUtils.closeQuietly(output);
                    }
                }
            }
        } catch (Exception e) {
            throw new DataSyncException(e);
        } finally {
            try {
                ftp.disconnect();
            } catch (IOException e) {
            }
        }
    }
}
