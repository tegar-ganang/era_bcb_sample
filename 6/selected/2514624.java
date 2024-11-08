package org.tscribble.bitleech.plugins.ftp.net.impl.commons;

import java.net.URL;
import java.util.Arrays;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;
import org.tscribble.bitleech.core.download.auth.AuthManager;
import org.tscribble.bitleech.core.download.auth.Authentication;
import org.tscribble.bitleech.core.download.auth.IAuthProvider;
import org.tscribble.bitleech.core.download.protocol.AbstractProtocolClient;
import org.tscribble.bitleech.plugins.ftp.auth.FTPAuthentication;

/**
 * @author triston
 *
 * Created on May 06, 2007
 */
public class Commons_FTPClient extends AbstractProtocolClient {

    /**
	 * Logger for this class
	 */
    protected final Logger log = Logger.getLogger("Commons_FTPClient");

    private FTPClient cl;

    private IAuthProvider ap;

    public Commons_FTPClient() {
    }

    public void getDownloadInfo(String _url) throws Exception {
        cl = new FTPClient();
        Authentication auth = new FTPAuthentication();
        cl.connect(getHostName());
        while (!cl.login(auth.getUser(), auth.getPassword())) {
            log.debug("getDownloadInfo() - login error state: " + Arrays.asList(cl.getReplyStrings()));
            ap.setSite(getSite());
            auth = ap.promptAuthentication();
            if (auth == null) throw new Exception("User Cancelled Auth Operation");
        }
        AuthManager.putAuth(getSite(), auth);
        cl.enterLocalPassiveMode();
        FTPFile file = cl.listFiles(new URL(_url).getFile())[0];
        setURL(_url);
        setLastModified(file.getTimestamp().getTimeInMillis());
        setSize(file.getSize());
        setResumable(cl.rest("0") == 350);
        setRangeEnd(getSize() - 1);
    }

    @Override
    public void setAuthProvider(IAuthProvider ap) {
        this.ap = ap;
    }

    @Override
    public IAuthProvider getAuthProvider() {
        return ap;
    }

    public void initGet() throws Exception {
        cl = new FTPClient();
        cl.connect(getHostName());
        Authentication auth = AuthManager.getAuth(getSite());
        if (auth == null) auth = new FTPAuthentication(getSite());
        while (!cl.login(auth.getUser(), auth.getPassword())) {
            ap.setSite(getSite());
            auth = ap.promptAuthentication();
            if (auth == null) throw new Exception("User Cancelled Auth Operation");
        }
        cl.connect(getHostName());
        cl.login(auth.getUser(), auth.getPassword());
        cl.enterLocalPassiveMode();
        cl.setFileType(FTP.BINARY_FILE_TYPE);
        cl.setRestartOffset(getPosition());
        setInputStream(cl.retrieveFileStream(new URL(getURL()).getFile()));
    }

    public void disconnect() {
        try {
            if (cl.isConnected()) cl.quit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cl = null;
        }
    }
}
