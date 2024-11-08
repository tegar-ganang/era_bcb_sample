package ao.dd.shell.impl.transfer.ftp.apache;

import ao.dd.shell.ShellUtils;
import ao.dd.shell.def.ShellFile;
import ao.dd.shell.def.TransferAgent;
import ao.dd.shell.impl.transfer.stream.CountingInputStream;
import ao.dd.shell.impl.transfer.stream.CountingOutputStream;
import ao.dd.shell.impl.transfer.stream.ThrottledInputStream;
import ao.dd.shell.impl.transfer.stream.ThrottledOutputStream;
import ao.util.time.Stopwatch;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.log4j.Logger;
import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class ApacheFtpAgent implements TransferAgent {

    private static final Logger LOG = Logger.getLogger(ApacheFtpAgent.class);

    private static final long DEFAULT_PER_SEC = 4 * 1000 * 1000 / 1000 / 8;

    public static void main(String[] args) throws Exception {
        ApacheFtpAgent client = new ApacheFtpAgent("x", "x", "x");
        client.throttle(1024 * 20);
        try {
            System.out.println(client.upload("C:/~/temp/err.xml", "/prod_logfiles/e.xml"));
        } finally {
            client.close();
        }
    }

    private FTPClient client;

    private final boolean isSecure;

    private boolean throttle = false;

    private long bytesPerSecond = DEFAULT_PER_SEC;

    private final String host;

    private final String user;

    private final String pass;

    public ApacheFtpAgent(String hostname, String username, String password) {
        this(hostname, username, password, false);
    }

    public ApacheFtpAgent(String hostname, String username, String password, boolean isSecure) {
        host = hostname;
        user = username;
        pass = password;
        this.isSecure = isSecure;
    }

    @Override
    public ShellFile file(String remoteFilePath) {
        try {
            List<ShellFile> files = getFiles(stripTrailingSlash(stripInitialSlash(remoteFilePath)));
            if (files.isEmpty()) return null;
            if (files.size() == 1) return files.get(0);
            throw new AssertionError("Cannot have more than one file: " + files);
        } catch (Throwable e) {
            LOG.error("File listing failed", e);
            return null;
        }
    }

    @Override
    public List<ShellFile> files(String inRemoteFilePath) {
        try {
            return getFiles(appendTrailingSlash(stripInitialSlash(inRemoteFilePath)));
        } catch (Throwable e) {
            LOG.error("File listing failed", e);
            return null;
        }
    }

    private List<ShellFile> getFiles(String inRemotePath) throws IOException {
        FTPFile[] remoteFiles = client().listFiles(inRemotePath);
        List<ShellFile> files = new ArrayList<ShellFile>();
        for (FTPFile remoteFile : remoteFiles) {
            files.add(new ShellFile(remoteFile));
        }
        return files;
    }

    @Override
    public boolean makeDirs(String remoteDirPath) {
        return ShellUtils.makeDirs(this, remoteDirPath);
    }

    @Override
    public boolean makeDir(String remoteDirPath) {
        try {
            int reply = client().mkd(stripInitialSlash(remoteDirPath));
            return FTPReply.isPositiveCompletion(reply);
        } catch (IOException e) {
            LOG.error("creating directory failed", e);
            return false;
        }
    }

    @Override
    public void throttle(long maxBytesPerSecond) {
        bytesPerSecond = maxBytesPerSecond;
        throttle();
    }

    public void throttle() {
        throttle = true;
    }

    @Override
    public void unThrottle() {
        throttle = false;
    }

    @Override
    public boolean download(String remotePath, String toFile) {
        return download(remotePath, new File(toFile));
    }

    @Override
    public boolean download(String remotePath, File to) {
        OutputStream sink = null;
        try {
            sink = new FileOutputStream(to);
            return download(remotePath, sink, to.toString(), to);
        } catch (IOException e) {
            LOG.error("Download failed " + remotePath + " -> " + to, e);
            return false;
        } finally {
            if (sink != null) {
                try {
                    sink.close();
                } catch (IOException e) {
                    LOG.error("Close downloaded file failed: " + sink, e);
                }
            }
        }
    }

    @Override
    public boolean download(String remoteFilename, OutputStream to) {
        return download(remoteFilename, to, to.toString(), null);
    }

    public boolean download(String remoteFilename, OutputStream to, String destinationName, File destinationFile) {
        Stopwatch timer = new Stopwatch();
        try {
            if (!(to instanceof BufferedOutputStream)) {
                to = new BufferedOutputStream(to);
            }
            if (throttle) {
                to = new ThrottledOutputStream(to, bytesPerSecond);
            }
            boolean success;
            long length;
            if (destinationFile == null) {
                CountingOutputStream counter = new CountingOutputStream(to);
                success = client().retrieveFile(remoteFilename, counter);
                length = counter.totalWritten();
            } else {
                success = client().retrieveFile(remoteFilename, to);
                length = destinationFile.length();
            }
            LOG.debug("Downloaded " + ShellUtils.transferDetails(length, timer.timingMillis()));
            return success;
        } catch (IOException e) {
            LOG.error("Download failed " + remoteFilename + " -> " + destinationName, e);
            return false;
        }
    }

    @Override
    public boolean upload(String filePath, String toFilePath) {
        return upload(new File(filePath), toFilePath);
    }

    @Override
    public boolean upload(File file, String toFilePath) {
        InputStream source = null;
        try {
            source = new BufferedInputStream(new FileInputStream(file));
            return upload(source, toFilePath);
        } catch (FileNotFoundException e) {
            LOG.error("upload failed " + file + " -> " + toFilePath, e);
            return false;
        } finally {
            if (source != null) {
                try {
                    source.close();
                } catch (IOException e) {
                    LOG.error("unable to close " + file, e);
                }
            }
        }
    }

    @Override
    public boolean upload(InputStream source, String toFilePath) {
        try {
            if (!(source instanceof BufferedInputStream)) {
                source = new BufferedInputStream(source);
            }
            if (throttle) {
                source = new ThrottledInputStream(source, bytesPerSecond);
            }
            LOG.debug("Available: " + source.available());
            CountingInputStream countingSource = new CountingInputStream(source);
            Stopwatch timer = new Stopwatch();
            boolean success = client().storeFile(toFilePath, countingSource);
            if (success) {
                LOG.debug("Uploaded " + ShellUtils.transferDetails(countingSource.totalRead(), timer.timingMillis()));
            } else {
                LOG.warn("Upload failed.");
            }
            return success;
        } catch (IOException e) {
            LOG.error("Upload failed: " + source + " -> " + toFilePath, e);
            return false;
        }
    }

    private String stripInitialSlash(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private String appendTrailingSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    private String stripTrailingSlash(String path) {
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private FTPClient client() {
        try {
            return clientChecked();
        } catch (IOException e) {
            LOG.debug("FTP connection failed", e);
            return null;
        }
    }

    private FTPClient clientChecked() throws IOException {
        if (client != null) return client;
        try {
            return (client = connectFtps());
        } catch (NoSuchAlgorithmException e) {
            LOG.error("FTP connection failed", e);
            throw new RuntimeException(e);
        }
    }

    private FTPClient connectFtps() throws NoSuchAlgorithmException, IOException {
        FTPClient apacheClient;
        if (isSecure) {
            apacheClient = new FTPSClient(true);
        } else {
            apacheClient = new FTPClient();
        }
        apacheClient.addProtocolCommandListener(new LogFtpListener(LOG));
        if (isSecure) {
            apacheClient.connect(host, 990);
        } else {
            apacheClient.connect(host);
        }
        if (!apacheClient.login(user, pass)) {
            throw new IllegalArgumentException("Unrecognized Username/Password");
        }
        apacheClient.setFileType(FTPClient.BINARY_FILE_TYPE);
        apacheClient.getStatus();
        apacheClient.help();
        apacheClient.enterLocalPassiveMode();
        return apacheClient;
    }

    @Override
    public boolean open() {
        return (client() != null);
    }

    @Override
    public void openChecked() throws IOException {
        clientChecked();
    }

    @Override
    public void close() {
        LOG.debug("logging out");
        if (client != null) {
            try {
                client.logout();
                client.disconnect();
            } catch (Exception e) {
                LOG.error("ftp logout failed");
            }
            client = null;
        }
    }
}
