package com.entelience.probe;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import com.entelience.util.DateHelper;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.Util;

public class FtpArchive extends Archive {

    private static org.apache.log4j.Logger _logger = com.entelience.util.Logs.getProbeLogger();

    private final String remoteRootDir;

    private final String server;

    private final int port;

    private final String username;

    private final String password;

    private final String subDir;

    private final boolean passiveMode;

    private final boolean binaryMode;

    private FTPClient client = null;

    /**
     * Construct an archive object.
     *
     * @param trialRun if true then we don't actually do anything.
     *
     */
    protected FtpArchive(boolean trialRun, String directory, String server, int port, String remoteRootDir, String username, String password, boolean passiveMode, boolean binaryMode) throws Exception {
        super(trialRun, directory);
        this.server = server;
        this.port = port;
        this.remoteRootDir = remoteRootDir;
        this.username = username;
        this.password = password;
        this.subDir = DateHelper.filenameString(DateHelper.now());
        this.passiveMode = passiveMode;
        this.binaryMode = binaryMode;
        connect();
        disconnect();
    }

    protected boolean archive(LocalFileState processed) throws Exception {
        if (!super.archive(processed)) return false;
        if (archivedFile == null) {
            return false;
        }
        return archive(archivedFile);
    }

    /**
     * Handle archiving a file.
     *
     * @param processed a file that is been processed and is ready to be archived.
     *
     * @return if the file is archived, false if not.
     */
    protected boolean archive(File toArchive) {
        if (trialRun) return false;
        try {
            connect();
            if (!client.changeWorkingDirectory(subDir)) {
                if (client.makeDirectory(subDir)) {
                    _logger.debug("Created directory " + subDir);
                    if (!client.changeWorkingDirectory(subDir)) {
                        throw new Exception("Cannot create and go to directory " + subDir);
                    }
                } else {
                    throw new Exception("Cannot create directory " + subDir);
                }
            }
            OutputStream os = null;
            FileInputStream fis = null;
            try {
                os = client.storeFileStream(toArchive.getName());
                fis = new FileInputStream(toArchive);
                Util.copyStream(fis, os, com.entelience.util.StaticConfig.ioBufferSize);
            } finally {
                if (os != null) try {
                    os.close();
                } catch (Exception e) {
                    _logger.debug("Ignored exception " + e);
                }
                if (fis != null) try {
                    fis.close();
                } catch (Exception e) {
                    _logger.debug("Ignored exception " + e);
                }
            }
            if (!client.completePendingCommand()) {
                throw new Exception("File transfer failed : FTP client did not complete pending command");
            }
            _logger.info("File " + toArchive.getName() + " archved on server " + server);
            return true;
        } catch (Exception e) {
            _logger.error("Error when archiving file " + toArchive.getName() + " to FTP server " + server, e);
            return false;
        } finally {
            disconnect();
        }
    }

    private void connect() throws Exception {
        if (client != null) throw new IllegalStateException("Already connected.");
        try {
            _logger.debug("About to connect to ftp server " + server + " port " + port);
            client = new FTPClient();
            client.connect(server, port);
            if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
                throw new Exception("Unable to connect to FTP server " + server + " port " + port + " got error [" + client.getReplyString() + "]");
            }
            _logger.info("Connected to ftp server " + server + " port " + port);
            _logger.debug(client.getReplyString());
            _logger.debug("FTP server is [" + client.getSystemName() + "]");
            if (!client.login(username, password)) {
                throw new Exception("Invalid username / password combination for FTP server " + server + " port " + port);
            }
            _logger.debug("Log in successful.");
            if (passiveMode) {
                client.enterLocalPassiveMode();
                _logger.debug("Passive mode selected.");
            } else {
                client.enterLocalActiveMode();
                _logger.debug("Active mode selected.");
            }
            if (binaryMode) {
                client.setFileType(FTP.BINARY_FILE_TYPE);
                _logger.debug("BINARY mode selected.");
            } else {
                client.setFileType(FTP.ASCII_FILE_TYPE);
                _logger.debug("ASCII mode selected.");
            }
            if (client.changeWorkingDirectory(remoteRootDir)) {
                _logger.debug("Changed directory to " + remoteRootDir);
            } else {
                if (client.makeDirectory(remoteRootDir)) {
                    _logger.debug("Created directory " + remoteRootDir);
                    if (client.changeWorkingDirectory(remoteRootDir)) {
                        _logger.debug("Changed directory to " + remoteRootDir);
                    } else {
                        throw new Exception("Cannot change directory to [" + remoteRootDir + "] on FTP server " + server + " port " + port);
                    }
                } else {
                    throw new Exception("Cannot create directory [" + remoteRootDir + "] on FTP server " + server + " port " + port);
                }
            }
        } catch (Exception e) {
            disconnect();
            throw e;
        }
    }

    /**
     * Disconnect from the remote ftp server.
     */
    private void disconnect() {
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
}
