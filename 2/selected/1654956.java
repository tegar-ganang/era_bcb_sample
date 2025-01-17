package net.rptools.maptool.client.ui.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import sun.net.ftp.FtpClient;

/**
 * @author crash
 *
 */
class FTPClientConn {

    private static final String PROTOCOL = "ftp://";

    private static final String TYPE_IMAGE = ";type=i";

    public final String host;

    public final String user;

    protected final String password;

    protected final String proto_user_pswd_host;

    public FTPClientConn(String _host, String _user, String _password) {
        host = _host;
        user = _user;
        password = _password;
        if (user == null) proto_user_pswd_host = PROTOCOL + host + "/"; else if (password == null) proto_user_pswd_host = PROTOCOL + encodeUser(user) + "@" + host + "/"; else proto_user_pswd_host = PROTOCOL + encodeUser(user) + ":" + password + "@" + host + "/";
    }

    private static String encodeUser(String u) {
        return u.replace("@", "%40");
    }

    /**
	 * <p>
	 * Since this method uses a separate connection to the FTP server, the full pathname
	 * to the directory must be specified.
	 * </p>
	 * <p>
	 * Note that the directory separator may be "/"
	 * locally and in URLs, but FTP servers are not required to support it.  This means
	 * that we really should be starting at the top of the tree and issuing <code>cd()</code>
	 * calls for the entire pathname, creating any that fail along the way. Too much work
	 * for now.  FIXME
	 * </p>
	 * @param dir the full pathname to the directory to create
	 * @return success or failure
	 */
    public int mkdir(String dir) {
        int result = -1;
        FTPCommand myftp = null;
        try {
            myftp = new FTPCommand(host);
            myftp.login(user, password);
            result = myftp.mkdir(dir);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (myftp != null) {
                try {
                    myftp.closeServer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
	 * <p>
	 * Since this method uses a separate connection to the FTP server, the full pathname
	 * to the file must be specified.
	 * </p>
	 * <p>
	 * Note that the directory separator may be "/"
	 * locally and in URLs, but FTP servers are not required to support it.  This means
	 * that we really should be starting at the top of the tree and issuing <code>cd()</code>
	 * calls for the entire pathname, creating any that fail along the way. Too much work
	 * for now.  FIXME
	 * </p>
	 * @param filename the full pathname to the file to remove
	 * @return success or failure
	 */
    public int remove(String filename) {
        int result = -1;
        FTPCommand myftp = null;
        try {
            myftp = new FTPCommand(host);
            myftp.login(user, password);
            result = myftp.remove(filename);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (myftp != null) {
                try {
                    myftp.closeServer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    protected URL makeURL(String directory, String targetfile) throws MalformedURLException {
        return new URL(proto_user_pswd_host + (directory == null ? "" : directory + "/") + targetfile + TYPE_IMAGE);
    }

    protected InputStream openDownloadStream(String targetfile) throws IOException {
        return openDownloadStream(null, targetfile);
    }

    protected InputStream openDownloadStream(String dir, String targetfile) throws IOException {
        URL url = makeURL(dir, targetfile);
        URLConnection urlc = url.openConnection();
        urlc.setDoInput(true);
        InputStream is = urlc.getInputStream();
        return is;
    }

    protected OutputStream openUploadStream(String targetfile) throws IOException {
        return openUploadStream(null, targetfile);
    }

    protected OutputStream openUploadStream(String dir, String targetfile) throws IOException {
        URL url = makeURL(dir, targetfile);
        URLConnection urlc = url.openConnection();
        urlc.setDoOutput(true);
        OutputStream os = urlc.getOutputStream();
        return os;
    }
}
