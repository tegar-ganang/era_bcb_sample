package de.psisystems.dmachinery.io.protocols.localfile;

import sun.net.www.ParseUtil;
import java.io.*;
import java.security.Permission;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by IntelliJ IDEA.
 * User: stefanpudig
 * Date: Jul 31, 2009
 * Time: 2:03:03 AM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("restriction")
public class FileURLConnection extends URLConnection {

    InputStream is;

    OutputStream os;

    File file;

    String filename;

    Permission permission;

    protected FileURLConnection(URL u, File file) {
        super(u);
        this.file = file;
    }

    public void connect() throws IOException {
        if (file.isDirectory()) {
            throw new IOException("URL points to a directory, not to a file");
        }
        if (!connected) {
            filename = file.toString();
            if (getDoOutput()) {
                os = new BufferedOutputStream(new FileOutputStream(file));
            } else {
                try {
                    is = new BufferedInputStream(new FileInputStream(file));
                } catch (IOException e) {
                    throw new RuntimeException("IO Error" + file.getAbsolutePath(), e);
                }
            }
            connected = true;
        }
    }

    public synchronized InputStream getInputStream() throws IOException {
        connect();
        return is;
    }

    public synchronized OutputStream getOutputStream() throws IOException {
        connect();
        if (os == null) {
            throw new RuntimeException("Init Error getDoOutput not called" + file.getAbsolutePath());
        }
        return os;
    }

    public Permission getPermission() throws IOException {
        if (permission == null) {
            String decodedPath = ParseUtil.decode(url.getPath());
            if (File.separatorChar == '/') {
                permission = new FilePermission(decodedPath, "read,write");
            } else {
                permission = new FilePermission(decodedPath.replace('/', File.separatorChar), "read,write");
            }
        }
        return permission;
    }
}
