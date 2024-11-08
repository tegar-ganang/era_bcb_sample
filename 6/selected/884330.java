package gc3d.ftp;

import java.io.IOException;
import java.io.Serializable;
import org.apache.commons.net.ftp.FTPReply;

/**
 * @author Pierrick
 *
 */
public class GridPovFTP implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private String host;

    private String username;

    private String password;

    private int port;

    public GridPovFTP(String host, int port, String uname, String passwd) throws IOException {
        this.host = host;
        this.username = uname;
        this.password = passwd;
        this.port = port;
    }

    public GridPovFTP clone() {
        GridPovFTP cloneGridPovFTP = null;
        try {
            cloneGridPovFTP = (GridPovFTP) super.clone();
            cloneGridPovFTP.host = new String(this.host);
            cloneGridPovFTP.username = new String(this.username);
            cloneGridPovFTP.password = new String(this.password);
            cloneGridPovFTP.port = this.port;
        } catch (CloneNotSupportedException cnse) {
            cnse.printStackTrace();
        }
        return cloneGridPovFTP;
    }

    public void mkdirs(String path) throws IOException {
        GridFTP ftp = new GridFTP();
        ftp.setDefaultPort(port);
        System.out.println(this + ".mkdirs " + path);
        try {
            ftp.connect(host);
            ftp.login(username, password);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                throw new IOException("FTP server refused connection.");
            }
            ftp.mkdirs(path);
            ftp.logout();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean storeFile(String local, String remote, int retrylimit) throws IOException {
        boolean succed = false;
        try {
            succed = this.storeFile(local, remote);
        } catch (IOException ioe) {
            System.err.println(this + ".storeFile IOException:" + ioe.getMessage() + " limit:" + retrylimit);
        }
        if (!succed) {
            if (retrylimit > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                succed = this.storeFile(local, remote, retrylimit - 1);
            } else {
                throw new IOException("Unable to store the file remotly after retries");
            }
        }
        return succed;
    }

    public boolean storeFile(String local, String remote) throws IOException {
        boolean stored = false;
        GridFTP ftp = new GridFTP();
        ftp.setDefaultPort(port);
        System.out.println(this + ".storeFile " + remote);
        try {
            ftp.connect(host);
            ftp.login(username, password);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                return false;
            }
            ftp.put(local, remote);
            ftp.logout();
            stored = true;
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return stored;
    }

    public boolean getFile(String local, String remote, int retrylimit) throws IOException {
        boolean succed = false;
        try {
            succed = this.getFile(local, remote);
        } catch (IOException ioe) {
            System.err.println(this + ".getFile IOException:" + ioe.getMessage() + " limit:" + retrylimit);
        }
        if (!succed) {
            if (retrylimit > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                succed = this.getFile(local, remote, retrylimit - 1);
            } else {
                throw new IOException("Unable to get the remote file after retries");
            }
        }
        return succed;
    }

    public boolean getFile(String local, String remote) throws IOException {
        boolean result = false;
        GridFTP ftp = new GridFTP();
        ftp.setDefaultPort(port);
        System.out.println(this + ".getFile " + remote);
        try {
            ftp.connect(host);
            ftp.login(username, password);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                return false;
            }
            ftp.get(local, remote);
            ftp.logout();
            result = true;
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println(this + ".getFile return " + result);
        return result;
    }

    public String getLink() {
        return "ftp://" + host + ':' + port + '/';
    }

    public void setHost(String host) {
        this.host = host;
    }
}
