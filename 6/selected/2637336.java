package org.tscribble.bitleech.plugins.ftp.net.impl.edt;

import java.io.InputStream;
import java.net.PasswordAuthentication;
import java.net.URL;
import org.tscribble.bitleech.core.download.DownloadWorker;
import org.tscribble.bitleech.core.download.auth.Authentication;
import org.tscribble.bitleech.core.download.auth.IAuthProvider;
import org.tscribble.bitleech.core.download.protocol.AbstractProtocolClient;
import org.tscribble.bitleech.core.download.protocol.IProtocolClient;
import org.tscribble.bitleech.plugins.http.auth.HTTPAuthProvider;
import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPReply;
import com.enterprisedt.net.ftp.FTPTransferType;

/**
 * @author triston
 *
 * Created  May 07, 2006
 */
public class Edt_FTPClient extends AbstractProtocolClient {

    private boolean initSuccess;

    private InputStream input;

    private FTPClient cl;

    private URL url;

    private String user = "anonymous";

    private String pass = "anonymous@test.com";

    private IAuthProvider ap;

    private Authentication auth;

    private FTPReply reply;

    Edt_FTPClient() {
    }

    @Override
    public void setAuthProvider(IAuthProvider ap) {
        this.ap = ap;
    }

    @Override
    public IAuthProvider getAuthProvider() {
        return ap;
    }

    public void getDownloadInfo(String _url) throws Exception {
        url = new URL(_url);
        cl.setRemoteHost(url.getHost());
        cl.connect();
        reply = cl.getLastValidReply();
        while ((reply == null) || !reply.getReplyCode().equals("230")) {
            try {
                ap.setSite(getURL());
                auth = ap.promptAuthentication();
                user = auth.getUser();
                pass = auth.getPassword();
                cl.login(user, pass);
            } catch (Exception e) {
                e.printStackTrace();
            }
            reply = cl.getLastValidReply();
        }
        setURL(_url);
        setSize(cl.size(url.getFile()));
        cl.quit();
    }

    public int read(byte[] buffer, int offset, int length) throws Exception {
        return input.read(buffer, offset, length);
    }

    public void initGet() throws Exception {
        cl = new FTPClient();
        URL url = new URL(getURL());
        cl.setRemoteHost(url.getHost());
        cl.connect();
        cl.login(user, pass);
        cl.setType(FTPTransferType.BINARY);
        cl.setConnectMode(FTPConnectMode.PASV);
        cl.restart(getPosition());
    }

    public void disconnect() {
        try {
            cl.quit();
            input.close();
        } catch (Exception e) {
        } finally {
            cl = null;
            input = null;
        }
    }
}
