package com.entelience.probe;

import com.entelience.util.DateHelper;
import com.entelience.sql.Db;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * Mirror files from *one* directory on an ftp server.
 *
 * Note that all files are downloaded via temporary files, which are only ever
 * renamed to the target filename if the file is downloaded successfully.
 *
 * This, hopefully, allows us to avoid integrating files which were previously
 * only partially downloaded.
 */
public class FtpMirror extends Mirror {

    private final String remoteRootDir;

    private final String server;

    private final int port;

    private final String username;

    private final String password;

    private final boolean recursive;

    private final boolean passiveMode;

    private final boolean binaryMode;

    private final Date before;

    private final Date after;

    private FTPClient client = null;

    /**
     * Construct an FtpMirror for a remote ftp server.
     *
     * @param dir directory to CWD to once connected to the ftp server.
     * @param server remote server hostname or ip address
     * @param port port to connect to on the remote server, usually 21
     * @param username username for login on the remote server
     * @param password password for login on the remote server
     */
    public FtpMirror(String dir, String server, int port, String username, String password, int maxFilesToImport, boolean recursive, boolean passiveMode, boolean binaryMode, Date before, Date after) {
        this.remoteRootDir = dir;
        this.server = server;
        this.port = port;
        this.username = username;
        this.password = password;
        setMaxFilesToMirror(maxFilesToImport);
        this.recursive = recursive;
        this.passiveMode = passiveMode;
        this.binaryMode = binaryMode;
        this.before = before;
        this.after = after;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString());
        sb.append(" remoteRootDir=[").append(remoteRootDir).append(']');
        sb.append(" server=[").append(server).append(']');
        sb.append(" port=[").append(port).append(']');
        sb.append(" username=[").append(username).append(']');
        sb.append(" maxFiles=[").append(getMaxFilesToMirror()).append(']');
        sb.append(" recursive=[").append(recursive).append(']');
        sb.append(" passiveMode=[").append(passiveMode).append(']');
        sb.append(" binaryMode=[").append(binaryMode).append(']');
        if (after != null) sb.append(" after=[").append(after).append(']');
        if (before != null) sb.append(" before=[").append(before).append(']');
        return sb.toString();
    }

    @Override
    public void configure(Map<String, String> params) throws Exception {
        throw new IllegalStateException("Not implemented.");
    }

    @Override
    public void cliConfigure() throws Exception {
        throw new IllegalStateException("Not implemented.");
    }

    /**
     */
    public boolean isConnected() {
        return (client == null);
    }

    /**
     */
    @Override
    public void connect() throws Exception {
        if (client != null) {
            _logger.warn("Already connected.");
            return;
        }
        try {
            _logger.debug("About to connect to ftp server " + server + " port " + port);
            client = new FTPClient();
            client.connect(server, port);
            if (!FTPReply.isPositiveCompletion(client.getReplyCode())) throw new Exception("Unable to connect to FTP server " + server + " port " + port + " got error [" + client.getReplyString() + "]");
            _logger.info("Connected to ftp server " + server + " port " + port);
            _logger.debug(client.getReplyString());
            if (!client.login(username, password)) throw new Exception("Invalid username / password combination for FTP server " + server + " port " + port);
            _logger.debug("Log in successful.");
            _logger.info("FTP server is [" + client.getSystemType() + "]");
            if (passiveMode) {
                client.enterLocalPassiveMode();
                _logger.info("Passive mode selected.");
            } else {
                client.enterLocalActiveMode();
                _logger.info("Active mode selected.");
            }
            if (binaryMode) {
                client.setFileType(FTP.BINARY_FILE_TYPE);
                _logger.info("BINARY mode selected.");
            } else {
                client.setFileType(FTP.ASCII_FILE_TYPE);
                _logger.info("ASCII mode selected.");
            }
            if (client.changeWorkingDirectory(remoteRootDir)) {
                _logger.info("Changed directory to " + remoteRootDir);
            } else {
                throw new Exception("Cannot change directory to [" + remoteRootDir + "] on FTP server " + server + " port " + port);
            }
        } catch (Exception e) {
            _logger.error("Failed to connect to the FTP server " + server + " on port " + port, e);
            disconnect();
            throw e;
        }
    }

    private static final List<Pattern> listMatches = new ArrayList<Pattern>();

    /**
     * Add a pattern that matches files to be fetched from the remote ftp server.
     * Used in addition to select.
     */
    public void addFetch(Pattern p) {
        listMatches.add(p);
    }

    /**
     * List the files that match fetch patterns on the remote ftp server.
     * @param files input/output
     */
    public void list(Db db, List<FileState> files) throws Exception {
        list(db, files, null);
    }

    protected void list(Db db, List<FileState> files, String subDir) throws Exception {
        if (files == null) throw new IllegalArgumentException("files must not be null");
        if (client == null) throw new IllegalStateException("client has gone away");
        String dir = this.remoteRootDir;
        if (subDir != null && !".".equals(subDir)) {
            dir = this.remoteRootDir + "/" + subDir;
        }
        List<String> subDirs = new ArrayList<String>();
        _logger.info("File listing for directory " + dir);
        if (!client.changeWorkingDirectory(dir)) throw new Exception("Cannot change directory to [" + dir + "] on FTP server " + server + " port " + port);
        FTPFile[] list = client.listFiles();
        for (int i = 0; i < list.length; ++i) {
            if (list[i] == null) {
                _logger.warn("Null entry found");
                continue;
            }
            String name = list[i].getName();
            if (name == null || ".".equals(name) || "..".equals(name)) continue;
            if (list[i].isFile()) {
                boolean canRead = false;
                int[] access = new int[] { FTPFile.USER_ACCESS, FTPFile.GROUP_ACCESS, FTPFile.WORLD_ACCESS };
                for (int j = 0; j < access.length; j++) {
                    if (list[i].hasPermission(access[j], FTPFile.READ_PERMISSION)) {
                        canRead = true;
                        break;
                    }
                }
                if (!canRead) {
                    _logger.debug("Ignoring file " + name + " as it is not readable.");
                    continue;
                }
                Date lastModified = list[i].getTimestamp().getTime();
                boolean matches = listMatches.size() == 0;
                Iterator<Pattern> ipatterns = listMatches.iterator();
                while (!matches && ipatterns.hasNext()) {
                    Pattern p = ipatterns.next();
                    Matcher m = p.matcher(name);
                    if (m.find()) matches = true;
                }
                if (!matches) {
                    _logger.debug("Ignoring file " + name + " as it does not match required patterns.");
                    continue;
                }
                if (after != null) {
                    if (!lastModified.after(after)) {
                        _logger.info("Ignoring file " + name + " as its last modification date is before " + DateHelper.HTMLDate(after));
                        continue;
                    }
                }
                if (before != null) {
                    if (!lastModified.before(before)) {
                        _logger.info("Ignoring file " + name + " as its last modification date is after " + DateHelper.HTMLDate(before));
                        continue;
                    }
                }
                String filename = name;
                if (lastModified == null) {
                    filename = DateHelper.filenameString(DateHelper.now()) + "_CURRENT_" + name;
                } else {
                    filename = DateHelper.filenameString(lastModified) + "_VERSION_" + name;
                }
                _logger.info("Adding file " + name);
                RemoteFileState rfs = rfsdb.findOrAdd(filename, name, toUrl(subDir, name), subDir);
                rfs.object = list[i];
                rfs.last_modified = list[i].getTimestamp().getTimeInMillis();
                rfs.addMetadata(new DateMetadata(lastModified));
                rfs.server = server;
                rfs.length = list[i].getSize();
                files.add(rfs);
            }
            if (list[i].isDirectory()) {
                StringBuffer sb = new StringBuffer();
                if (subDir == null || ".".equals(subDir)) sb.append(list[i].getName()); else {
                    sb.append(subDir);
                    sb.append('/');
                    sb.append(list[i].getName());
                }
                subDirs.add(sb.toString());
            }
        }
        _logger.debug("Parsed " + list.length + " files in directory " + dir);
        if (recursive) {
            Iterator<String> i = subDirs.iterator();
            while (i.hasNext()) {
                list(db, files, i.next());
            }
        }
    }

    private String toUrl(String subDir, String filename) {
        StringBuffer sb = new StringBuffer();
        sb.append("ftp://").append(server);
        if (port != 21) sb.append(':').append(port);
        if (remoteRootDir.length() > 0 && remoteRootDir.charAt(0) != '/') {
            sb.append('/');
        }
        sb.append(remoteRootDir);
        if (subDir != null && !".".equals(subDir)) {
            if (subDir.length() > 0) {
                sb.append('/').append(subDir);
            }
        }
        sb.append('/').append(filename);
        return sb.toString();
    }

    private static final int maxTries = 3;

    private static final int waitMilliseconds = 50000;

    /**
     * Returns true if the max-files hasn't been reached
     */
    @Override
    public synchronized boolean canStillFetchFile() {
        return !isMaxMirroredFilesReached();
    }

    /**
     * Sets importedFiles to 0
     */
    public synchronized void reinit() {
        resetMirroredFilesCount();
    }

    /**
     * Fetch files from the remote file server.
     *
     * If there's an error, disconnect and reconnect to the ftp server after waiting a short while.  Retry.
     *
     * @param remote Remote file state previously returned by list method in this mirror
     * @param dir Local directory to store retrieved files in.
     */
    public MirrorReturn fetch(Db db, RemoteFileState remote, File dir, File localRootDir) throws Exception {
        if (remote == null) throw new IllegalArgumentException("remote must not be null");
        if (!(remote.object instanceof FTPFile)) throw new IllegalArgumentException("remote.object must be of type FTPFile");
        if (client == null) throw new IllegalStateException("client has gone away");
        String cwd = this.remoteRootDir;
        if (remote.subDir != null && !".".equals(remote.subDir)) {
            cwd = this.remoteRootDir + "/" + remote.subDir;
        }
        for (int ntries = 0; ntries < maxTries; ++ntries) {
            try {
                if (ntries > 0) {
                    _logger.info(remote.url + " try " + ntries + " sleeping " + waitMilliseconds + " ms");
                    disconnect();
                    try {
                        Thread.sleep(waitMilliseconds);
                    } catch (InterruptedException ie) {
                        _logger.debug("Got interrupted by something", ie);
                    }
                    connect();
                }
                _logger.info("fetching remote " + remote);
                if (!client.changeWorkingDirectory(cwd)) {
                    throw new IllegalStateException("Unable to change directory on the ftp server to [" + cwd + "]");
                }
                InputStream is = null;
                try {
                    String origFilename = ((FTPFile) remote.object).getName();
                    is = client.retrieveFileStream(origFilename);
                    if (is != null) {
                        File newFile = fetchViaTempFile(remote, dir, is);
                        if (newFile == null) {
                            _logger.warn("Error storing " + remote.url);
                        } else {
                            try {
                                is.close();
                            } catch (Exception e) {
                                _logger.debug("Ignored exception " + e.toString());
                            } finally {
                                is = null;
                            }
                            if (!client.completePendingCommand()) throw new Exception("Unable to complete the fetch file " + ((FTPFile) remote.object).getName());
                            LocalFileState local = lfsdb.findOrAdd(newFile.getName(), remote.orig_filename, localRootDir.getAbsolutePath(), remote.subDir, remote);
                            local.transferState(remote);
                            lfsdb.updateMetadata(local);
                            incrementMirroredFilesCount();
                            return new MirrorReturn(remote, local);
                        }
                    }
                    if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
                        throw new Exception("Unable to connect to fetch file " + ((FTPFile) remote.object).getName() + " got error [" + client.getReplyString() + "]");
                    }
                } finally {
                    if (is != null) {
                        is.close();
                        client.abort();
                        disconnect();
                    }
                }
            } catch (Exception e) {
                _logger.warn("Problem downloading " + remote.url, e);
                if (ntries == maxTries - 1) {
                    throw new Exception("Error downloading " + remote.url, e);
                } else continue;
            }
        }
        return null;
    }

    /**
     * Delete a file on the remote ftp server.
     *
     * @param remote Remote file state previously created by this mirror.
     * @return true if the delete command succeeded.
     */
    public boolean delete(RemoteFileState remote) throws Exception {
        if (remote == null) throw new IllegalArgumentException("remote must not be null");
        if (!(remote.object instanceof FTPFile)) throw new IllegalArgumentException("remote.object must be of type FTPFile");
        if (client == null) throw new IllegalStateException("client has gone away");
        FTPFile file = (FTPFile) remote.object;
        String cwd = this.remoteRootDir;
        if (remote.subDir != null && !".".equals(remote.subDir)) {
            cwd = this.remoteRootDir + "/" + remote.subDir;
        }
        if (!client.changeWorkingDirectory(cwd)) {
            throw new IllegalStateException("Unable to change directory on the ftp server to [" + cwd + "]");
        }
        return client.deleteFile(file.getName());
    }

    /**
     * Disconnect from the remote ftp server.
     */
    public void disconnect() {
        if (client != null) {
            try {
                try {
                    try {
                        client.logout();
                    } catch (Exception ex) {
                        _logger.debug("Hiding exception", ex);
                    }
                } finally {
                    try {
                        client.disconnect();
                    } catch (Exception ex) {
                        _logger.debug("Hiding exception", ex);
                    }
                }
            } finally {
                client = null;
            }
        }
    }

    /**
     */
    public Mirror clone() {
        FtpMirror cloned = new FtpMirror(remoteRootDir, server, port, username, password, 0, recursive, passiveMode, binaryMode, before, after);
        return cloned;
    }
}
