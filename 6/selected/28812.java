package dnl.jexem.camouflaj;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

/**
 * 
 * @author Daniel Orr
 * 
 */
public class FtpCommandManager extends DirBasedCommandManager {

    private String ftpHost;

    private String userName;

    private String password;

    FTPClient ftpClient = new FTPClient();

    public FtpCommandManager() {
    }

    public FtpCommandManager(String ftpHost, String userName, String password) throws IOException {
        this.ftpHost = ftpHost;
        this.userName = userName;
        this.password = password;
        initFtp();
    }

    public FtpCommandManager(String ftpHost, String userName, String password, String workingDir) throws IOException {
        this.ftpHost = ftpHost;
        this.userName = userName;
        this.password = password;
        this.workingDir = workingDir;
        initFtp();
    }

    private void initFtp() throws IOException {
        ftpClient.setConnectTimeout(5000);
        ftpClient.connect(ftpHost);
        ftpClient.login(userName, password);
        if (workingDir != null) {
            ftpClient.changeWorkingDirectory(workingDir);
        }
        logger.info("Connection established.");
    }

    @Override
    public void setWorkingDir(String targetDir) {
        super.setWorkingDir(targetDir);
        if (workingDir != null) {
            try {
                ftpClient.changeWorkingDirectory(workingDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getFtpHost() {
        return ftpHost;
    }

    public void setFtpHost(String ftpHost) {
        this.ftpHost = ftpHost;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    protected boolean waitForFile(String fileName, long timeout) {
        long t1 = System.currentTimeMillis();
        while (System.currentTimeMillis() - t1 < timeout) {
            FTPFile[] listFiles;
            try {
                listFiles = ftpClient.listFiles(fileName);
                if (listFiles.length != 0) {
                    return true;
                }
                Thread.sleep(5000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    protected void deleteFile(String fileName) throws IOException {
        ftpClient.deleteFile(fileName);
        FTPFile[] listFiles = ftpClient.listFiles(fileName);
        if (listFiles.length > 0) {
            throw new IllegalStateException("Cannot delete remote files. please change remote account configuration.");
        }
    }

    /**
	 * 
	 * @param dir
	 *            remote dir
	 * @param fileNamePrefix
	 *            only files with this prefix will be returned.
	 * @return matching filenames
	 */
    @Override
    protected Set<String> getFilesFromWorkDir(String fileNamePrefix) {
        try {
            FTPFile[] listFiles = ftpClient.listFiles();
            if (listFiles.length == 0) {
                return Collections.emptySet();
            }
            Set<String> results = new HashSet<String>();
            for (FTPFile ftpFile : listFiles) {
                if (ftpFile.getName().startsWith(fileNamePrefix)) {
                    results.add(ftpFile.getName());
                }
            }
            return results;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void saveFileImpl(String fileName, String content) {
        try {
            ftpClient.storeFile(fileName, new ByteArrayInputStream(content.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String readFileContentsImpl(String fileName) {
        try {
            FTPFile[] listFiles = ftpClient.listFiles(fileName);
            if (listFiles.length == 0) {
                return null;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ftpClient.retrieveFile(fileName, baos);
            String content = new String(baos.toByteArray());
            return content;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected Date getTimeStamp(String fileName) {
        try {
            FTPFile[] listFiles = ftpClient.listFiles(fileName);
            FTPFile commandXml = listFiles[0];
            return commandXml.getTimestamp().getTime();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected boolean fileExists(String fileName) throws IOException {
        FTPFile[] listFiles;
        listFiles = ftpClient.listFiles(fileName);
        if (listFiles.length != 0) {
            return true;
        }
        return false;
    }
}
