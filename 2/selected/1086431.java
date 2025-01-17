package org.das2.util.filesystem;

import java.util.logging.Logger;
import org.das2.util.monitor.CancelledOperationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.das2.util.Base64;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;
import org.das2.util.FileUtil;
import org.das2.util.monitor.NullProgressMonitor;

/**
 *
 * @author  Jeremy
 */
public class HttpFileSystem extends WebFileSystem {

    public static final int LISTING_TIMEOUT_MS = 200000;

    /** Creates a new instance of WebFileSystem */
    private HttpFileSystem(URI root, File localRoot) {
        super(root, localRoot);
    }

    public static HttpFileSystem createHttpFileSystem(URI rooturi) throws FileSystemOfflineException, UnknownHostException {
        try {
            String auth = rooturi.getAuthority();
            String[] ss = auth.split("@");
            URL root;
            if (ss.length > 3) {
                throw new IllegalArgumentException("user info section can contain at most two at (@) symbols");
            } else if (ss.length == 3) {
                StringBuilder userInfo = new StringBuilder(ss[0]);
                for (int i = 1; i < 2; i++) userInfo.append("%40").append(ss[i]);
                auth = ss[2];
                try {
                    URI rooturi2 = new URI(rooturi.getScheme() + "://" + userInfo.toString() + "@" + auth + rooturi.getPath());
                    rooturi = rooturi2;
                } catch (URISyntaxException ex) {
                    throw new IllegalArgumentException("unable to handle: " + rooturi);
                }
            }
            root = rooturi.toURL();
            HttpURLConnection urlc = (HttpURLConnection) root.openConnection();
            urlc.setConnectTimeout(3000);
            String userInfo = null;
            try {
                userInfo = KeyChain.getDefault().getUserInfo(root);
            } catch (CancelledOperationException ex) {
                throw new FileSystemOfflineException("user cancelled credentials");
            }
            if (userInfo != null) {
                String encode = Base64.encodeBytes(userInfo.getBytes());
                urlc.setRequestProperty("Authorization", "Basic " + encode);
            }
            boolean offline = true;
            boolean connectFail = true;
            byte[] buf = new byte[2048];
            try {
                urlc.connect();
                InputStream is = urlc.getInputStream();
                int ret = 0;
                while ((ret = is.read(buf)) > 0) {
                }
                is.close();
                connectFail = false;
            } catch (IOException ex) {
                ex.printStackTrace();
                if (FileSystem.settings().isAllowOffline()) {
                    logger.info("remote filesystem is offline, allowing access to local cache.");
                } else {
                    throw new FileSystemOfflineException("" + urlc.getResponseCode() + ": " + urlc.getResponseMessage());
                }
                InputStream err = urlc.getErrorStream();
                if (err != null) {
                    int ret = 0;
                    while ((ret = err.read(buf)) > 0) {
                    }
                    err.close();
                }
            }
            if (!connectFail) {
                if (urlc.getResponseCode() != HttpURLConnection.HTTP_OK && urlc.getResponseCode() != HttpURLConnection.HTTP_FORBIDDEN) {
                    if (urlc.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        KeyChain.getDefault().clearUserPassword(root);
                        if (userInfo == null) {
                            String port = root.getPort() == -1 ? "" : (":" + root.getPort());
                            URL rootAuth = new URL(root.getProtocol() + "://" + "user@" + root.getHost() + port + root.getFile());
                            try {
                                URI rootAuthUri = rootAuth.toURI();
                                return createHttpFileSystem(rootAuthUri);
                            } catch (URISyntaxException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    } else {
                        offline = false;
                    }
                } else {
                    offline = false;
                }
            }
            File local;
            if (FileSystemSettings.hasAllPermission()) {
                local = localRoot(rooturi);
                logger.log(Level.FINER, "initializing httpfs {0} at {1}", new Object[] { root, local });
            } else {
                local = null;
                logger.log(Level.FINER, "initializing httpfs {0} in applet mode", root);
            }
            HttpFileSystem result = new HttpFileSystem(rooturi, local);
            result.offline = offline;
            return result;
        } catch (FileSystemOfflineException e) {
            throw e;
        } catch (UnknownHostException e) {
            throw e;
        } catch (IOException e) {
            throw new FileSystemOfflineException(e, rooturi);
        }
    }

    protected void downloadFile(String filename, File f, File partFile, ProgressMonitor monitor) throws IOException {
        Lock lock = getDownloadLock(filename, f, monitor);
        if (lock == null) {
            return;
        }
        logger.log(Level.FINE, "downloadFile({0})", filename);
        try {
            URL remoteURL = new URL(root.toString() + filename);
            URLConnection urlc = remoteURL.openConnection();
            String userInfo;
            try {
                userInfo = KeyChain.getDefault().getUserInfo(root);
            } catch (CancelledOperationException ex) {
                throw new IOException("user cancelled at credentials entry");
            }
            if (userInfo != null) {
                String encode = Base64.encodeBytes(userInfo.getBytes());
                urlc.setRequestProperty("Authorization", "Basic " + encode);
            }
            HttpURLConnection hurlc = (HttpURLConnection) urlc;
            if (hurlc.getResponseCode() == 404) {
                logger.log(Level.INFO, "{0} URL: {1}", new Object[] { hurlc.getResponseCode(), remoteURL });
                throw new FileNotFoundException("not found: " + remoteURL);
            } else if (hurlc.getResponseCode() != 200) {
                logger.log(Level.INFO, "{0} URL: {1}", new Object[] { hurlc.getResponseCode(), remoteURL });
                throw new IOException(hurlc.getResponseCode() + ": " + hurlc.getResponseMessage() + "\n" + remoteURL);
            }
            Date d = null;
            List<String> sd = urlc.getHeaderFields().get("Last-Modified");
            if (sd != null && sd.size() > 0) {
                d = new Date(sd.get(sd.size() - 1));
            }
            monitor.setTaskSize(urlc.getContentLength());
            if (!f.getParentFile().exists()) {
                logger.log(Level.FINE, "make dirs {0}", f.getParentFile());
                FileSystemUtil.maybeMkdirs(f.getParentFile());
            }
            if (partFile.exists()) {
                logger.log(Level.FINE, "clobber file {0}", f);
                if (!partFile.delete()) {
                    logger.log(Level.INFO, "Unable to clobber file {0}, better use it for now.", f);
                    return;
                }
            }
            if (partFile.createNewFile()) {
                InputStream in;
                in = urlc.getInputStream();
                logger.log(Level.FINE, "transferring bytes of {0}", filename);
                FileOutputStream out = new FileOutputStream(partFile);
                monitor.setLabel("downloading file");
                monitor.started();
                try {
                    copyStream(in, out, monitor);
                    monitor.finished();
                    out.close();
                    in.close();
                    if (d != null) {
                        try {
                            partFile.setLastModified(d.getTime() + 10);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    if (f.exists()) {
                        logger.log(Level.FINE, "deleting old file {0}", f);
                        if (!f.delete()) {
                            throw new IllegalArgumentException("unable to delete " + f);
                        }
                    }
                    if (!partFile.renameTo(f)) {
                        throw new IllegalArgumentException("rename failed " + partFile + " to " + f);
                    }
                } catch (IOException e) {
                    out.close();
                    in.close();
                    if (partFile.exists() && !partFile.delete()) {
                        throw new IllegalArgumentException("unable to delete " + partFile);
                    }
                    throw e;
                }
            } else {
                throw new IOException("couldn't create local file: " + f);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * this is introduced to support checking if the symbol foo/bar is a folder by checking
     * for a 303 redirect.
     *   EXIST->Boolean
     *   REAL_NAME->String
     * @param f
     * @throws java.io.IOException
     */
    protected Map<String, Object> getHeadMeta(String f) throws IOException, CancelledOperationException {
        String realName = f;
        boolean exists;
        try {
            URL ur = new URL(this.root.toURL(), f);
            HttpURLConnection connect = (HttpURLConnection) ur.openConnection();
            String userInfo = KeyChain.getDefault().getUserInfo(ur);
            if (userInfo != null) {
                String encode = Base64.encodeBytes(userInfo.getBytes());
                connect.setRequestProperty("Authorization", "Basic " + encode);
            }
            connect.setRequestMethod("HEAD");
            HttpURLConnection.setFollowRedirects(false);
            connect.connect();
            HttpURLConnection.setFollowRedirects(true);
            if (connect.getResponseCode() == 303) {
                String surl = connect.getHeaderField("Location");
                if (surl.startsWith(root.toString())) {
                    realName = surl.substring(root.toString().length());
                }
                connect.disconnect();
                ur = new URL(this.root.toURL(), realName);
                connect = (HttpURLConnection) ur.openConnection();
                connect.setRequestMethod("HEAD");
                connect.connect();
            }
            exists = connect.getResponseCode() != 404;
            Map<String, Object> result = new HashMap<String, Object>();
            result.putAll(connect.getHeaderFields());
            connect.disconnect();
            return result;
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /** dumb method looks for / in parent directory's listing.  Since we have
     * to list the parent, then IOException can be thrown.
     * 
     */
    public boolean isDirectory(String filename) throws IOException {
        if (localRoot == null) {
            return filename.endsWith("/");
        }
        File f = new File(localRoot, filename);
        if (f.exists()) {
            return f.isDirectory();
        } else {
            if (filename.endsWith("/")) {
                return true;
            } else {
                File parentFile = f.getParentFile();
                String parent = getLocalName(parentFile);
                if (!parent.endsWith("/")) {
                    parent = parent + "/";
                }
                String[] list = listDirectory(parent);
                String lookFor;
                if (filename.startsWith("/")) {
                    lookFor = filename.substring(1) + "/";
                } else {
                    lookFor = filename + "/";
                }
                for (int i = 0; i < list.length; i++) {
                    if (list[i].equals(lookFor)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public void resetListingCache() {
        if (!FileUtil.deleteWithinFileTree(localRoot, ".listing")) {
            throw new IllegalArgumentException("unable to delete all .listing files");
        }
    }

    /**
     * return the File for the cached listing, even if it does not exist.
     * @param directory
     * @return
     */
    private File listingFile(String directory) {
        File f = new File(localRoot, directory);
        try {
            FileSystemUtil.maybeMkdirs(f);
        } catch (IOException ex) {
            throw new IllegalArgumentException("unable to mkdir " + f, ex);
        }
        File listing = new File(localRoot, directory + ".listing");
        return listing;
    }

    public boolean isListingCached(String directory) {
        File listing = listingFile(directory);
        if (listing.exists() && (System.currentTimeMillis() - listing.lastModified()) < LISTING_TIMEOUT_MS) {
            logger.fine(String.format("listing date is %f5.2 seconds old", ((System.currentTimeMillis() - listing.lastModified()) / 1000.)));
            return true;
        } else {
            return false;
        }
    }

    public String[] listDirectory(String directory) throws IOException {
        if (protocol != null && protocol instanceof AppletHttpProtocol) {
            InputStream in = null;
            URL[] list;
            try {
                in = protocol.getInputStream(new WebFileObject(this, directory, new Date()), new NullProgressMonitor());
                list = HtmlUtil.getDirectoryListing(getURL(directory), in);
            } catch (CancelledOperationException ex) {
                throw new IllegalArgumentException(ex);
            } finally {
                if (in != null) in.close();
            }
            String[] result;
            result = new String[list.length];
            int n = directory.length();
            for (int i = 0; i < list.length; i++) {
                URL url = list[i];
                result[i] = getLocalName(url).substring(n);
            }
            return result;
        }
        directory = toCanonicalFolderName(directory);
        String[] result;
        if (isListingCached(directory)) {
            logger.log(Level.FINE, "using cached listing for {0}", directory);
            File listing = listingFile(directory);
            URL[] list = null;
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(listing);
                list = HtmlUtil.getDirectoryListing(getURL(directory), fin);
            } catch (CancelledOperationException ex) {
                throw new IllegalArgumentException(ex);
            } finally {
                if (fin != null) fin.close();
            }
            result = new String[list.length];
            int n = directory.length();
            for (int i = 0; i < list.length; i++) {
                URL url = list[i];
                result[i] = getLocalName(url).substring(n);
            }
            return result;
        }
        boolean successOrCancel = false;
        if (this.isOffline()) {
            File f = new File(localRoot, directory);
            if (!f.exists()) throw new FileSystemOfflineException("unable to list " + f + " when offline");
            String[] listing = f.list();
            return listing;
        }
        while (!successOrCancel) {
            logger.log(Level.FINE, "list {0}", directory);
            URL[] list;
            try {
                URL listUrl = getURL(directory);
                String file = listUrl.getFile();
                if (file.charAt(file.length() - 1) != '/') {
                    listUrl = new URL(listUrl.toString() + '/');
                }
                File listing = listingFile(directory);
                downloadFile(directory, listing, new File(listing.toString() + ".part"), new NullProgressMonitor());
                FileInputStream fin = null;
                try {
                    fin = new FileInputStream(listing);
                    list = HtmlUtil.getDirectoryListing(getURL(directory), fin);
                } finally {
                    if (fin != null) fin.close();
                }
                result = new String[list.length];
                int n = directory.length();
                for (int i = 0; i < list.length; i++) {
                    URL url = list[i];
                    result[i] = getLocalName(url).substring(n);
                }
                return result;
            } catch (CancelledOperationException ex) {
                throw new IOException("user cancelled at credentials");
            } catch (IOException ex) {
                if (isOffline()) {
                    System.err.println("** using local listing because remote is not available");
                    System.err.println("or some other error occurred. **");
                    File localFile = new File(localRoot, directory);
                    return localFile.list();
                } else {
                    throw ex;
                }
            }
        }
        return (new String[] { "should not get here" });
    }

    @Override
    public String[] listDirectory(String directory, String regex) throws IOException {
        directory = toCanonicalFilename(directory);
        if (!isDirectory(directory)) {
            throw new IllegalArgumentException("is not a directory: " + directory);
        }
        String[] listing = listDirectory(directory);
        Pattern pattern = Pattern.compile(regex + "/?");
        ArrayList result = new ArrayList();
        for (int i = 0; i < listing.length; i++) {
            if (pattern.matcher(listing[i]).matches()) {
                result.add(listing[i]);
            }
        }
        return (String[]) result.toArray(new String[result.size()]);
    }
}
