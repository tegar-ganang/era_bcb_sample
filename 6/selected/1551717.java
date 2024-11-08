package com.lc.util.build.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import com.oroinc.net.ftp.FTP;
import com.oroinc.net.ftp.FTPClient;
import com.oroinc.net.ftp.FTPReply;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import com.lc.util.Exceptions;
import com.lc.util.Files;

/**
 * Ant command for retrieving a single file by FTP.<br>
 * This command fixes a bug in Ant's 1.3 FTP command, which does not recognize
 * directory format as returned by some FTP servers.
 * @author Laurent Caillette
 * @version $Revision: 1.1.1.1 $ $Date: 2002/02/19 22:12:04 $
 */
public class SimpleFTPGet extends Task {

    private String host = null;

    public void setHost(String host) {
        this.host = host;
    }

    private int port = 21;

    public void setPort(int port) {
        this.port = port;
    }

    private String userid = "anonymous";

    public void setUserid(String userid) {
        this.userid = userid;
    }

    private String password = "";

    public void setPassword(String password) {
        this.password = password;
    }

    private boolean binary = true;

    public void setBinary(boolean binary) {
        this.binary = binary;
    }

    private String remotedir = null;

    public void setRemotedir(String remotedir) {
        this.remotedir = remotedir;
    }

    private String remotefile = null;

    public void setRemotefile(String remotefile) {
        this.remotefile = remotefile;
    }

    private String localdir = ".";

    public void setLocaldir(String localdir) {
        this.localdir = localdir;
    }

    private String serverpathseparator = "/";

    public void setServerpathseparator(String serverpathseparator) {
        this.serverpathseparator = serverpathseparator;
    }

    public void execute() {
        check();
        FTPClient ftp = new FTPClient();
        try {
            ftp.connect(host, port);
        } catch (IOException ex) {
            throw new BuildException("FTP connexion failed : IOException caught (" + ex.getMessage() + ")");
        }
        if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
            throw new BuildException("FTP connexion failed : " + ftp.getReplyString());
        }
        try {
            try {
                if (!ftp.login(userid, password)) {
                    throw new BuildException("Identification failed");
                }
            } catch (IOException ex) {
                throw new BuildException(ex);
            }
            if (binary) {
                try {
                    ftp.setFileType(FTP.IMAGE_FILE_TYPE);
                } catch (IOException ex) {
                    throw new BuildException(ex);
                }
                if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                    throw new BuildException("Transfer type not supported : " + ftp.getReplyString());
                }
            }
            try {
                ftp.changeWorkingDirectory(remotedir);
            } catch (IOException ex) {
                throw new BuildException(ex);
            }
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                throw new BuildException("Unacessible remote directory : " + ftp.getReplyString());
            }
            String localFullFileName = Files.normalizeDirectoryName(localdir) + remotefile;
            String remoteFullFileName = (remotedir.endsWith(serverpathseparator) ? remotedir : remotedir + serverpathseparator) + remotefile;
            BufferedOutputStream outstream = null;
            try {
                outstream = new BufferedOutputStream(new FileOutputStream(localFullFileName));
                ftp.retrieveFile(remoteFullFileName, outstream);
            } catch (IOException ex) {
                throw new BuildException(ex);
            }
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                throw new BuildException("File retrieval of '" + localFullFileName + "' has failed (" + ftp.getReplyString() + ")");
            }
        } finally {
            try {
                ftp.disconnect();
            } catch (IOException ex) {
                System.err.println("Disconnexion from " + host + ":" + port + " failed");
            }
        }
    }

    private void check() {
        if (host == null) {
            throw new BuildException("'host' is a mandatory attribute");
        }
        if (remotefile == null) {
            throw new BuildException("'remotefile' is a mandatory attribute");
        }
    }
}
