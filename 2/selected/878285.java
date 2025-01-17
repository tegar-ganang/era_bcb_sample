package org.mortbay.resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.mortbay.log.Log;
import org.mortbay.util.IO;

public class JarResource extends URLResource {

    protected transient JarURLConnection _jarConnection;

    JarResource(URL url) {
        super(url, null);
    }

    JarResource(URL url, boolean useCaches) {
        super(url, null, useCaches);
    }

    public synchronized void release() {
        _jarConnection = null;
        super.release();
    }

    protected boolean checkConnection() {
        super.checkConnection();
        try {
            if (_jarConnection != _connection) newConnection();
        } catch (IOException e) {
            Log.ignore(e);
            _jarConnection = null;
        }
        return _jarConnection != null;
    }

    /**
     * @throws IOException Sub-classes of <code>JarResource</code> may throw an IOException (or subclass) 
     */
    protected void newConnection() throws IOException {
        _jarConnection = (JarURLConnection) _connection;
    }

    /**
     * Returns true if the respresenetd resource exists.
     */
    public boolean exists() {
        if (_urlString.endsWith("!/")) return checkConnection(); else return super.exists();
    }

    public File getFile() throws IOException {
        return null;
    }

    public InputStream getInputStream() throws java.io.IOException {
        checkConnection();
        if (!_urlString.endsWith("!/")) return new FilterInputStream(super.getInputStream()) {

            public void close() throws IOException {
                this.in = IO.getClosedStream();
            }
        };
        URL url = new URL(_urlString.substring(4, _urlString.length() - 2));
        InputStream is = url.openStream();
        return is;
    }

    public static void extract(Resource resource, File directory, boolean deleteOnExit) throws IOException {
        if (Log.isDebugEnabled()) Log.debug("Extract " + resource + " to " + directory);
        String urlString = resource.getURL().toExternalForm().trim();
        int endOfJarUrl = urlString.indexOf("!/");
        int startOfJarUrl = (endOfJarUrl >= 0 ? 4 : 0);
        if (endOfJarUrl < 0) throw new IOException("Not a valid jar url: " + urlString);
        URL jarFileURL = new URL(urlString.substring(startOfJarUrl, endOfJarUrl));
        String subEntryName = (endOfJarUrl + 2 < urlString.length() ? urlString.substring(endOfJarUrl + 2) : null);
        boolean subEntryIsDir = (subEntryName != null && subEntryName.endsWith("/") ? true : false);
        if (Log.isDebugEnabled()) Log.debug("Extracting entry = " + subEntryName + " from jar " + jarFileURL);
        InputStream is = jarFileURL.openConnection().getInputStream();
        JarInputStream jin = new JarInputStream(is);
        JarEntry entry;
        boolean shouldExtract;
        while ((entry = jin.getNextJarEntry()) != null) {
            String entryName = entry.getName();
            if ((subEntryName != null) && (entryName.startsWith(subEntryName))) {
                if (subEntryIsDir) {
                    entryName = entryName.substring(subEntryName.length());
                    if (!entryName.equals("")) {
                        shouldExtract = true;
                    } else shouldExtract = false;
                } else shouldExtract = true;
            } else if ((subEntryName != null) && (!entryName.startsWith(subEntryName))) {
                shouldExtract = false;
            } else {
                shouldExtract = true;
            }
            if (!shouldExtract) {
                if (Log.isDebugEnabled()) Log.debug("Skipping entry: " + entryName);
                continue;
            }
            File file = new File(directory, entryName);
            if (entry.isDirectory()) {
                if (!file.exists()) file.mkdirs();
            } else {
                File dir = new File(file.getParent());
                if (!dir.exists()) dir.mkdirs();
                FileOutputStream fout = null;
                try {
                    fout = new FileOutputStream(file);
                    IO.copy(jin, fout);
                } finally {
                    IO.close(fout);
                }
                if (entry.getTime() >= 0) file.setLastModified(entry.getTime());
            }
            if (deleteOnExit) file.deleteOnExit();
        }
    }

    public void extract(File directory, boolean deleteOnExit) throws IOException {
        extract(this, directory, deleteOnExit);
    }
}
