package org.amlfilter.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

/**
 * The FTP client service
 * @author Harish Seshadri
 * @version $Id: FTPClientService.java,v 1.1 2008/01/27 23:16:27 sss Exp $
 */
public class FTPClientService extends GenericService {

    private String mRemoteHost;

    private String mUserName;

    private String mPassword;

    /**
	 * Get the remote host
	 * @return The remote host
	 */
    public String getRemoteHost() {
        return mRemoteHost;
    }

    /**
	 * Set the remote host
	 * @param pRemoteHost The remote host
	 */
    public void setRemoteHost(String pRemoteHost) {
        mRemoteHost = pRemoteHost;
    }

    /**
	 * Get the user name
	 * @return The user name
	 */
    public String getUserName() {
        return mUserName;
    }

    /**
	 * Set the user name
	 * @param pUserName The user name
	 */
    public void setUserName(String pUserName) {
        mUserName = pUserName;
    }

    /**
	 * Get the password
	 * @return The password
	 */
    public String getPassword() {
        return mPassword;
    }

    /**
	 * Set the password
	 * @param pPassword The password
	 */
    public void setPassword(String pPassword) {
        mPassword = pPassword;
    }

    /**
	 * Execute FTP
	 * @param pRemoteDirectory Remote directory
	 * @param pLocalDirectory The local directory
	 * @return Retrieved all files successfully
	 * @throws IOException
	 */
    public boolean getFiles(String pRemoteDirectory, String pLocalDirectory) throws IOException {
        final String methodSignature = "boolean getFiles(String,String): ";
        FTPClient fc = new FTPClient();
        fc.connect(getRemoteHost());
        fc.login(getUserName(), getPassword());
        fc.changeWorkingDirectory(pRemoteDirectory);
        FTPFile[] files = fc.listFiles();
        boolean retrieved = false;
        logInfo("Listing Files: ");
        int retrieveCount = 0;
        File tmpFile = null;
        for (int i = 0; i < files.length; i++) {
            tmpFile = new File(files[i].getName());
            if (!tmpFile.isDirectory()) {
                FileOutputStream fos = new FileOutputStream(pLocalDirectory + "/" + files[i].getName());
                retrieved = fc.retrieveFile(files[i].getName(), fos);
                if (false == retrieved) {
                    logInfo("Unable to retrieve file: " + files[i].getName());
                } else {
                    logInfo("Successfully retrieved file: " + files[i].getName());
                    retrieveCount++;
                }
                if (null != fos) {
                    fos.flush();
                    fos.close();
                }
            }
        }
        logInfo("Retrieve count: " + retrieveCount);
        if (retrieveCount > 0) {
            return true;
        }
        return false;
    }

    /**
	 * Execute FTP
	 * @param pRemoteDirectory Remote directory
	 * @param pLocalDirectory The local directory
	 * @param pFileName The file name
	 * @throws IOException
	 */
    public boolean getFile(String pRemoteDirectory, String pLocalDirectory, String pFileName) throws IOException {
        FTPClient fc = new FTPClient();
        fc.connect(getRemoteHost());
        fc.login(getUserName(), getPassword());
        fc.changeWorkingDirectory(pRemoteDirectory);
        String workingDirectory = fc.printWorkingDirectory();
        FileOutputStream fos = null;
        logInfo("Connected to remote host=" + getRemoteHost() + "; userName=" + getUserName() + "; " + "; remoteDirectory=" + pRemoteDirectory + "; localDirectory=" + pLocalDirectory + "; workingDirectory=" + workingDirectory);
        try {
            fos = new FileOutputStream(pLocalDirectory + "/" + pFileName);
            boolean retrieved = fc.retrieveFile(pFileName, fos);
            if (true == retrieved) {
                logInfo("Successfully retrieved file: " + pFileName);
            } else {
                logError("Could not retrieve file: " + pFileName);
            }
            return retrieved;
        } finally {
            if (null != fos) {
                fos.flush();
                fos.close();
            }
        }
    }
}
