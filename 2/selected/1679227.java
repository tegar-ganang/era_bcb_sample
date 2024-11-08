package org.virbo.datasource;

import java.util.logging.Level;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.filesystem.FileObject;
import org.das2.util.filesystem.FileSystem;
import ftpfs.FTPBeanFileSystemFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.das2.fsm.FileStorageModelNew;
import org.das2.util.DasProgressMonitorInputStream;
import org.das2.util.filesystem.FileSystemSettings;
import org.das2.util.filesystem.LocalFileSystem;
import org.das2.util.filesystem.URIException;
import org.das2.util.filesystem.VFSFileSystemFactory;
import org.virbo.aggregator.AggregatingDataSourceFactory;
import org.virbo.dsops.Ops;

/**
 *
 * Works with DataSourceRegistry to translate a URI into a DataSource.  Also,
 * will provide completions.
 *
 * @author jbf
 *
 */
public class DataSetURI {

    private static final Object ACTION_WAIT_EXISTS = "WAIT_EXISTS";

    private static final Object ACTION_DOWNLOAD = "DOWNLOAD";

    private static final Object ACTION_USE_CACHE = "USE_CACHE";

    private static final Logger logger = Logger.getLogger("virbo.datasource");

    static {
        DataSourceRegistry registry = DataSourceRegistry.getInstance();
        registry.discoverFactories();
        registry.discoverRegistryEntries();
    }

    static {
        FileSystem.registerFileSystemFactory("zip", new zipfs.ZipFileSystemFactory());
        FileSystem.registerFileSystemFactory("ftp", new FTPBeanFileSystemFactory());
        FileSystem.registerFileSystemFactory("sftp", new VFSFileSystemFactory());
        FileSystem.settings().setPersistence(FileSystemSettings.Persistence.EXPIRES);
        if (FileSystemSettings.hasAllPermission()) {
            File apDataHome = new File(AutoplotSettings.settings().resolveProperty(AutoplotSettings.PROP_FSCACHE));
            FileSystem.settings().setLocalCacheDir(apDataHome);
        }
    }

    static WeakHashMap<DataSource, DataSourceFactory> dsToFactory = new WeakHashMap<DataSource, DataSourceFactory>();

    /**
     * returns the explicit extension, or the file extension if found, or null.
     * The extension will not contain a period.
     * @param surl
     * @return the extension found, or null if no period is found in the filename.
     */
    public static String getExt(String surl) {
        if (surl == null) throw new NullPointerException();
        String explicitExt = getExplicitExt(surl);
        if (explicitExt != null) {
            return explicitExt;
        } else {
            URISplit split = URISplit.parse(surl);
            if (split.file != null) {
                int i0 = split.file.lastIndexOf('/');
                if (i0 == -1) return null;
                int i1 = split.file.lastIndexOf('.');
                if (i1 != -1 && i1 > i0) {
                    return split.file.substring(i1 + 1);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    /**
     * return the extension prefix of the URI, if specified.  
     * @param surl
     * @return null or an extension like "tsds"
     */
    public static String getExplicitExt(String surl) {
        URISplit split = URISplit.parse(surl);
        if (split.vapScheme == null) return null;
        int i = split.vapScheme.indexOf("+");
        if (i != -1) {
            return split.vapScheme.substring(i + 1);
        } else {
            return null;
        }
    }

    /**
     * split the url string (http://www.example.com/data/myfile.nc?myVariable) into:
     *   path, the directory with http://www.example.com/data/
     *   file, the file, http://www.example.com/data/myfile.nc
     *   ext, the extenion, .nc
     *   params, myVariable or null
     * @deprecated use URISplit.parse(surl);
     */
    public static URISplit parse(String surl) {
        return URISplit.parse(surl);
    }

    /**
     * @deprecated use URISplit.format(split);
     */
    public static String format(URISplit split) {
        return URISplit.format(split);
    }

    /**
     * get the data source for the URL.
     * @throws IllegalArgumentException if the url extension is not supported.
     */
    public static DataSource getDataSource(URI uri) throws Exception {
        DataSourceFactory factory = getDataSourceFactory(uri, new NullProgressMonitor());
        if (factory == null) {
            throw new IllegalArgumentException("unable to resolve URI: " + uri);
        }
        DataSource result = factory.getDataSource(uri);
        dsToFactory.put(result, factory);
        return result;
    }

    public static DataSource getDataSource(String surl) throws Exception {
        return getDataSource(getURIValid(surl));
    }

    /**
     * Prefix the URL with the datasource extension if necessary, so that
     * the URL would resolve to the dataSource.  This is to support TimeSeriesBrowse,
     * and any time a resouce URL must be understood out of context.
     *
     * TODO: note ds.getURI() should return the fully-qualified URI, so this is
     * no longer necessary.
     * 
     * @param surl
     * @param dataSource
     * @return
     */
    public static String getDataSourceUri(DataSource ds) {
        DataSourceFactory factory = dsToFactory.get(ds);
        if (factory instanceof AggregatingDataSourceFactory) {
            return ds.getURI();
        }
        if (factory == null) {
            return ds.getURI();
        } else {
            URISplit split = URISplit.parse(ds.getURI());
            String fext;
            fext = DataSourceRegistry.getInstance().getExtensionFor(factory).substring(1);
            if (DataSourceRegistry.getInstance().hasSourceByExt(split.ext)) {
                DataSourceFactory f2 = DataSourceRegistry.getInstance().getSource(split.ext);
                if (!factory.getClass().isInstance(f2)) {
                    split.vapScheme = "vap+" + fext;
                }
            } else {
                split.vapScheme = "vap+" + fext;
            }
            return URISplit.format(split);
        }
    }

    /**
     * check that the string uri is aggregating by looking for %Y's (etc) in the
     * file part of the URI.
     * @param surl
     * @return
     */
    public static boolean isAggregating(String surl) {
        int iquest = surl.indexOf("?");
        if (iquest > 0) surl = surl.substring(0, iquest);
        surl = surl.replaceAll("%25", "%");
        int ipercy = surl.lastIndexOf("%Y");
        if (ipercy == -1) ipercy = surl.lastIndexOf("$Y");
        if (ipercy == -1) ipercy = surl.lastIndexOf("%y");
        if (ipercy == -1) ipercy = surl.lastIndexOf("$y");
        if (ipercy != -1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * returns the URI to be interpreted by the DataSource.  This identifies
     * a file (or database) resource that can be passed to VFS.
     * @param uri, the URI understood in the context of all datasources.  This should contain "vap" or "vap+" for the scheme.
     * @return the URI for the datasource resource, or null if it is not valid.
     */
    public static URI getResourceURI(URI uri) {
        URISplit split = URISplit.parse(uri);
        return split.resourceUri;
    }

    /**
     * returns the URI to be interpreted by the DataSource.  For file-based
     * data sources, this will probably be the filename plus server-side
     * parameters, and can be converted to a URL.
     *
     * Changes:
     *   20090916: client-side parameters removed from URI.
     * @param uri, the URI understood in the context of all datasources.  This should contain "vap" or "vap+" for the scheme.
     * @return the URI for the datasource resource, or null if it is not valid.
     */
    public static URI getResourceURI(String surl) {
        if (surl.matches("file\\:[A-Z]\\:\\\\.*")) {
            surl = "file://" + surl.substring(5).replace('\\', '/');
        }
        if (surl.matches("file\\:/[A-Z]\\:\\\\.*")) {
            surl = "file://" + surl.substring(5).replace('\\', '/');
        }
        URISplit split = URISplit.parse(surl);
        return split.resourceUri;
    }

    /**
     * returns a downloadable URL from the Autoplot URI, perhaps popping off the
     * data source specifier.  This assumes that the resource is a URL,
     * and getResourceURI().toURL() should be used to handle all cases.
     * 
     * @param uri An Autoplot URI.
     * @return a URL that can be downloaded, or null if it is not found.
     */
    public static URL getWebURL(URI uri) {
        try {
            URI uri1 = getResourceURI(uri);
            if (uri1 == null) return null;
            URL rurl = uri1.toURL();
            String surl = rurl.toString();
            return new URL(surl);
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * creates a new URI from the new URI, in the context of the old URI.  For
     * example, if the old URI had parameters and the new is just a file, then
     * use the old URI but replace the file.
     * @param context
     * @param newUri
     * @return
     */
    static String newUri(String context, String newUri) {
        URISplit scontext = URISplit.parse(context, 0, false);
        URISplit newURLSplit = URISplit.parse(newUri);
        if (newURLSplit.file != null && !newURLSplit.file.equals("")) scontext.file = newURLSplit.file;
        if (newURLSplit.params != null && !newURLSplit.params.equals("")) scontext.params = newURLSplit.params;
        return URISplit.format(scontext);
    }

    public static class NonResourceException extends IllegalArgumentException {

        public NonResourceException(String msg) {
            super(msg);
        }
    }

    /**
     * for now, just hide the URI stuff from clients, let's not mess with factories
     * @param url
     * @return
     */
    public static DataSourceFormat getDataSourceFormat(URI uri) {
        int i = uri.getScheme().indexOf(".");
        String ext;
        if (i != -1) {
            ext = uri.getScheme().substring(0, i);
        } else {
            int i2 = uri.getScheme().indexOf("+");
            if (i2 != -1) {
                ext = uri.getScheme().substring(i2 + 1);
            } else {
                URL url = getWebURL(uri);
                String file = url.getPath();
                i = file.lastIndexOf(".");
                ext = i == -1 ? "" : file.substring(i);
            }
        }
        return DataSourceRegistry.getInstance().getFormatByExt(ext);
    }

    /**
     * get the datasource factory for the URL.
     */
    public static DataSourceFactory getDataSourceFactory(URI uri, ProgressMonitor mon) throws IOException, IllegalArgumentException {
        String suri = DataSetURI.fromUri(uri);
        if (isAggregating(suri)) {
            String eext = DataSetURI.getExplicitExt(suri);
            if (eext != null) {
                DataSourceFactory delegateFactory = DataSourceRegistry.getInstance().getSource(eext);
                AggregatingDataSourceFactory factory = new AggregatingDataSourceFactory();
                factory.setDelegateDataSourceFactory(delegateFactory);
                return factory;
            } else {
                return new AggregatingDataSourceFactory();
            }
        }
        String ext = DataSetURI.getExplicitExt(suri);
        if (ext != null && !suri.startsWith("vap+X:")) {
            return DataSourceRegistry.getInstance().getSource(ext);
        }
        URI resourceUri;
        try {
            String resourceSuri = uri.getRawSchemeSpecificPart();
            resourceUri = new URI(resourceSuri);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
        ext = DataSetURI.getExt(uri.toASCIIString());
        if (ext == null) ext = "";
        DataSourceFactory factory = null;
        factory = DataSourceRegistry.getInstance().getSource(ext);
        if (factory == null && (resourceUri.getScheme().equals("http") || resourceUri.getScheme().equals("https"))) {
            URL url = resourceUri.toURL();
            mon.setTaskSize(-1);
            mon.started();
            mon.setProgressMessage("doing HEAD request to find dataset type");
            URLConnection c = url.openConnection();
            String mime = c.getContentType();
            if (mime == null) {
                throw new IOException("failed to connect");
            }
            String cd = c.getHeaderField("Content-Disposition");
            if (cd != null) {
                int i0 = cd.indexOf("filename=\"");
                i0 += "filename=\"".length();
                int i1 = cd.indexOf("\"", i0);
                String filename = cd.substring(i0, i1);
                i0 = filename.lastIndexOf(".");
                ext = filename.substring(i0);
            }
            mon.finished();
            factory = DataSourceRegistry.getInstance().getSourceByMime(mime);
        }
        if (factory == null) {
            if (ext.equals("") || ext.equals("X")) {
                throw new NonResourceException("resource has no extension or mime type");
            } else {
                factory = DataSourceRegistry.getInstance().getSource(ext);
            }
        }
        if (factory == null) {
            throw new IllegalArgumentException("Unsupported extension: " + ext);
        }
        return factory;
    }

    /**
     *
     * split the parameters into name,value pairs.
     *
     * items without equals (=) are inserted as "arg_N"=name.
     * @deprecated use URISplit.parseParams
     */
    public static LinkedHashMap<String, String> parseParams(String params) {
        return URISplit.parseParams(params);
    }

    /**
     * @deprecated use URISplit.parseParams
     */
    public static String formatParams(Map parms) {
        return URISplit.formatParams(parms);
    }

    public static InputStream getInputStream(URL url, ProgressMonitor mon) throws IOException {
        URISplit split = URISplit.parse(url.toString());
        try {
            URI spath = getWebURL(DataSetURI.toUri(split.path)).toURI();
            FileSystem fs = FileSystem.create(spath);
            FileObject fo = fs.getFileObject(split.file.substring(split.path.length()));
            if (!fo.isLocal()) {
                logger.log(Level.INFO, "getInputStream(URL): downloading file {0} from {1}", new Object[] { fo.getNameExt(), url.toString() });
            }
            return fo.getInputStream(mon);
        } catch (URISyntaxException ex) {
            throw new IOException("URI Syntax Exception: " + ex.getMessage());
        }
    }

    public static InputStream getInputStream(URI uri, ProgressMonitor mon) throws IOException {
        URISplit split = URISplit.parse(uri);
        FileSystem fs;
        fs = FileSystem.create(DataSetURI.toUri(split.path));
        String filename = split.file.substring(split.path.length());
        if (fs instanceof LocalFileSystem) filename = DataSourceUtil.unescape(filename);
        FileObject fo = fs.getFileObject(filename);
        if (!fo.isLocal()) {
            logger.log(Level.INFO, "getInputStream(URI): downloading file {0} from {1}/{2}", new Object[] { fo.getNameExt(), fs.getRootURI(), filename });
        }
        return fo.getInputStream(mon);
    }

    /**
     * canonical method for converting string from the wild into a URI-safe string.
     * The string should already have a scheme part, such as "http" or "file".
     * @param surl
     * @deprecated use toURI().toURL() instead.
     * @return
     * @throws java.net.MalformedURLException
     */
    public static URL toURL(String surl) throws MalformedURLException {
        surl = surl.replaceAll(" ", "%20");
        return new URL(surl);
    }

    /**
     * canonical method for converting string from the wild into a URI-safe string.
     * This contains the code that converts a colloquial string URI into a
     * properly formed URI.
     * For example:
     *    space is converted to "%20"
     *    %Y is converted to $Y
     * This does not add file: or vap:.  Pluses are only changed in the params part.
     * @param suri
     * @throws IllegalArgumentException if the URI cannot be made safe.
     * @return
     */
    public static URI toUri(String suri) {
        try {
            if (!URISplit.isUriEncoded(suri)) {
                suri = suri.replaceAll("%([^0-9])", "%25$1");
                suri = suri.replaceAll("<", "%3C");
                suri = suri.replaceAll(">", "%3E");
                suri = suri.replaceAll(" ", "%20");
            }
            return new URI(suri);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * canonical method for converting URI to human-readable string, containing
     * spaces and other illegal characters.  Note pluses in the query part
     * are interpreted as spaces.
     * See also URISplit.uriDecode,etc.
     * @param uri
     * @return
     */
    public static String fromUri(URI uri) {
        String surl = uri.toASCIIString();
        int i = surl.indexOf("?");
        String query = i == -1 ? "" : surl.substring(i);
        if (i != -1) {
            return URISplit.uriDecode(surl.substring(0, i)) + query;
        } else {
            return URISplit.uriDecode(surl);
        }
    }

    /**
     * Legacy behavior was to convert pluses into spaces in URIs.  This caused problems
     * distinguishing spaces from pluses, so we dropped this as the default behavior.
     * If data sources are to support legacy URIs, then they should use this routine
     * to mimic the behavior.
     * This checks if the URI already contains spaces, and will not convert if there
     * are already spaces.
     * @param ssheet
     * @return
     */
    public static String maybePlusToSpace(String ssheet) {
        if (ssheet.contains(" ")) return ssheet;
        return ssheet.replaceAll("\\+", " ");
    }

    /**
     * return a file reference for the url.  This is initially to fix the problem
     * for Windows where new URL( "file://c:/myfile.dat" ).getPath() -> "/myfile.dat".
     *
     */
    public static File getFile(URL url, ProgressMonitor mon) throws IOException {
        URISplit split = URISplit.parse(url.toString());
        try {
            if (split.path == null || split.path.length() == 0) {
                throw new IllegalArgumentException("expected file but didn't find one, check URI for question mark");
            }
            FileSystem fs = FileSystem.create(getWebURL(toUri(split.path)).toURI());
            String filename = split.file.substring(split.path.length());
            if (fs instanceof LocalFileSystem) filename = DataSourceUtil.unescape(filename);
            FileObject fo = fs.getFileObject(filename);
            if (!fo.isLocal()) {
                logger.log(Level.FINE, "getFile: downloading file {0} from {1}", new Object[] { fo.getNameExt(), url.toString() });
            } else {
                logger.log(Level.FINE, "using local copy of {0}", fo.getNameExt());
            }
            File tfile;
            if (fo.exists()) {
                tfile = fo.getFile(mon);
            } else {
                FileObject foz = fs.getFileObject(filename + ".gz");
                if (foz.exists()) {
                    File fz = foz.getFile(mon);
                    File tfile1 = new File(fz.getPath().substring(0, fz.getPath().length() - 3) + ".temp");
                    tfile = new File(fz.getPath().substring(0, fz.getPath().length() - 3));
                    org.das2.util.filesystem.FileSystemUtil.unzip(fz, tfile1);
                    if (tfile.exists()) {
                        if (!tfile.delete()) {
                            throw new IllegalArgumentException("unable to delete " + tfile);
                        }
                    }
                    if (!tfile1.renameTo(tfile)) {
                        throw new IllegalArgumentException("unable to rename " + tfile1 + " to " + tfile);
                    }
                } else {
                    throw new FileNotFoundException("File not found: " + url);
                }
            }
            return tfile;
        } catch (URISyntaxException ex) {
            throw new IOException("URI Syntax Exception: " + ex.getMessage());
        }
    }

    private static void checkNonHtml(File tfile, URL source) throws HtmlResponseIOException, FileNotFoundException {
        FileInputStream fi = null;
        HtmlResponseIOException ex2 = null;
        try {
            fi = new FileInputStream(tfile);
            byte[] magic = new byte[5];
            int bytes = fi.read(magic);
            if (bytes == 5) {
                String ss = new String(magic);
                if (DataSourceUtil.isHtmlStream(ss)) {
                    ex2 = new HtmlResponseIOException("file appears to be html: " + tfile, source);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(DataSetURI.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fi != null) fi.close();
            } catch (IOException ex) {
                Logger.getLogger(DataSetURI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (ex2 != null) throw ex2;
        return;
    }

    /**
     * return a file reference for the url.  The file must have a nice name
     * on the server, and cannot be the result of a server query with parameters. (Use 
     * downloadResourceAsTempFile for this).
     * This is initially to fix the problem
     * for Windows where new URL( "file://c:/myfile.dat" ).getPath() -> "/myfile.dat".
     * @param allowHtml skip html test that tests for html content.
     */
    public static File getFile(String suri, boolean allowHtml, ProgressMonitor mon) throws IOException {
        URISplit split = URISplit.parse(suri);
        try {
            FileSystem fs = FileSystem.create(toUri(split.path), mon);
            String filename = split.file.substring(split.path.length());
            FileObject fo = fs.getFileObject(filename);
            File tfile = fo.getFile(mon);
            if (!allowHtml && tfile.exists()) checkNonHtml(tfile, new URL(split.file));
            return tfile;
        } catch (URIException ex) {
            throw new IOException(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            if (ex.getMessage().startsWith("root does not exist")) {
                throw new IOException(ex.getMessage());
            } else if (ex.getMessage().contains("unable to create")) {
                IOException ex2 = new IOException(ex.getMessage());
                ex2.initCause(ex);
                throw ex2;
            } else if (ex.getMessage().contains("unable to delete")) {
                IOException ex2 = new IOException(ex.getMessage());
                ex2.initCause(ex);
                throw ex2;
            } else {
                IOException ex2 = new IOException("Unsupported protocol: " + suri);
                ex2.initCause(ex);
                throw ex2;
            }
        }
    }

    /**
     * retrieve the file specified in the URI, possibly using the VFS library to
     * download the resource to a local cache.  The URI should be a downloadable
     * file, and not the vap scheme URI.
     * @param uri resource to download, such as "sftp://user@host/file.dat."
     * @param mon
     * @return
     * @throws IOException
     */
    public static File getFile(URI uri, ProgressMonitor mon) throws IOException {
        String suri = fromUri(uri);
        return getFile(suri, false, mon);
    }

    /**
     * @see DataSetURI.downloadResourceAsTempFile
     * @param url the address to download.
     * @param mon a progress monitor.
     * @return a File in the FileSystemCache.  The file will have question marks and ampersands removed.
     * @throws IOException
     */
    public static File downloadResourceAsTempFile(URL url, ProgressMonitor mon) throws IOException {
        return downloadResourceAsTempFile(url, -1, mon);
    }

    /**
     * This was introduced when we needed access to a URL with arguments.  This allows us
     * to download the file in a script and then read the file.  It will put the
     * file in the directory, and the parameters are encoded in the name.
     * Note this cannot be used to download HTML content, because checkNonHtml is
     * called.  We may introduce "downloadHtmlResourceAsTempFile" or similar if
     * it's needed.
     *
     * This will almost always download, very little caching is done.  We allow subsequent
     * calls within 10 seconds to use the same file (by default).  The timeoutSeconds parameter
     * can be used to set this to any limit.
     * 
     * This is not deleted if the file is already local.  Do not delete this file
     * yourself, it should be deleted when the process exits.
     * TODO: what about Tomcat and other long java processes?
     * 
     * @param url the address to download.
     * @param timeoutSeconds if positive, the number of seconds to allow use of a downloaded resource.  If -1, then the default ten seconds is used.
     * @param mon a progress monitor.
     * @return a File in the FileSystemCache.  The file will have question marks and ampersands removed.
     * @throws IOException
     */
    public static File downloadResourceAsTempFile(URL url, int timeoutSeconds, ProgressMonitor mon) throws IOException {
        if (timeoutSeconds == -1) timeoutSeconds = 10;
        URISplit split = URISplit.parse(url.toString());
        if (split.file.startsWith("file:/")) {
            if (split.params != null && split.params.length() > 0) {
                throw new IllegalArgumentException("local file URLs cannot have arguments");
            }
            try {
                return new File(new URL(split.file).toURI());
            } catch (URISyntaxException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
        File local = FileSystem.settings().getLocalCacheDir();
        FileSystem fs = FileSystem.create(toUri(split.path));
        String id = fs.getLocalRoot().toString().substring(FileSystem.settings().getLocalCacheDir().toString().length());
        File localCache = new File(local, "temp");
        localCache = new File(localCache, id);
        if (!localCache.exists()) {
            if (!localCache.mkdirs()) {
                throw new IOException("unable to make directory: " + localCache);
            }
        }
        String filename = split.file.substring(split.path.length());
        if (split.params != null && split.params.length() > 0) {
            String safe = split.params;
            safe = safe.replaceAll("\\+", "_");
            safe = safe.replaceAll("-", ".");
            safe = Ops.safeName(safe);
            filename = filename.replaceAll("@", "_") + "@" + safe.replaceAll("@", "_");
        } else {
            filename = filename.replaceAll("@", "_") + "@";
        }
        if (filename.length() > 50) {
            String[] ss = filename.split("@", -2);
            String base = ss[0];
            if (base.length() > 50) base = base.substring(0, 50);
            String args = ss[1];
            if (args.length() > 0) args = String.format("%09x", args.hashCode());
            filename = base + String.format("%09x", ss[0].hashCode()) + "@" + args;
        }
        filename = new File(localCache, filename).toString();
        Object action = "";
        File result = new File(filename);
        File newf = new File(filename + ".temp");
        synchronized (DataSetURI.class) {
            if (result.exists() && (System.currentTimeMillis() - result.lastModified()) / 1000 < timeoutSeconds && !newf.exists()) {
                logger.log(Level.FINE, "using young temp file {0}", result);
                action = ACTION_USE_CACHE;
            } else if (newf.exists()) {
                logger.log(Level.FINE, "waiting for other thread to load temp resource {0}", newf);
                action = ACTION_WAIT_EXISTS;
            } else {
                File newName = result;
                while (newName.exists()) {
                    String[] ss = filename.toString().split("@", -2);
                    if (ss.length == 2) {
                        filename = ss[0] + "@" + ss[1] + "@0";
                    } else {
                        int i = Integer.parseInt(ss[2]);
                        filename = ss[0] + "@" + ss[1] + "@" + (i + 1);
                    }
                    newName = new File(filename);
                }
                if (!newName.equals(result)) {
                    if (!result.renameTo(newName)) {
                        System.err.println("unable to move old file out of the way.  Using alternate name " + newName);
                        result = newName;
                        newf = new File(filename + ".temp");
                    }
                }
                logger.log(Level.FINE, "this thread will downloading temp resource {0}", newf);
                action = ACTION_DOWNLOAD;
                OutputStream out = new FileOutputStream(result);
                out.write("DataSetURI.downloadResourceAsTempFile: This placeholding temporary file should not be used.\n".getBytes());
                out.close();
                OutputStream outf = new FileOutputStream(newf);
                outf.close();
            }
        }
        if (action == ACTION_USE_CACHE) {
            return result;
        } else if (action == ACTION_WAIT_EXISTS) {
            long t0 = System.currentTimeMillis();
            mon.setProgressMessage("waiting for resource");
            mon.started();
            try {
                while (newf.exists()) {
                    try {
                        Thread.sleep(300);
                        if (System.currentTimeMillis() - t0 > 60000) {
                            logger.log(Level.FINE, "waiting for other process to finish loading %s...{0}", newf);
                        }
                        if (mon.isCancelled()) {
                            throw new InterruptedIOException("cancel pressed");
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(DataSetURI.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } finally {
                mon.finished();
            }
            return result;
        } else {
            boolean fail = true;
            try {
                InputStream in;
                logger.log(Level.FINE, "reading URL {0}", url);
                URLConnection urlc = url.openConnection();
                urlc.setConnectTimeout(3000);
                in = new DasProgressMonitorInputStream(urlc.getInputStream(), mon);
                OutputStream out = new FileOutputStream(newf);
                DataSourceUtil.transfer(Channels.newChannel(in), Channels.newChannel(out));
                fail = false;
            } finally {
                if (fail) {
                    newf.delete();
                    result.delete();
                }
            }
        }
        result.deleteOnExit();
        checkNonHtml(newf, url);
        synchronized (DataSetURI.class) {
            if (!result.delete()) {
                throw new IllegalArgumentException("unable to delete " + result + " to make way for " + newf);
            }
            if (!newf.renameTo(result)) {
                throw new IllegalArgumentException("unable to rename " + newf + " to " + result);
            }
        }
        return result;
    }

    /**
     * retrieve the file specified in the URI, possibly using the VFS library to
     * download the resource to a local cache.  The URI should be a downloadable
     * file, and not the vap scheme URI.
     * @param uri resource to download, such as "sftp://user@host/file.dat."
     * @param mon
     * @return
     * @throws IOException
     */
    public static File getFile(String uri, ProgressMonitor mon) throws IOException {
        return getFile(uri, false, mon);
    }

    /**
     * get the file, allowing it to have "<html>" in the front.  Normally this is not
     * allowed because of http://sourceforge.net/tracker/?func=detail&aid=3379717&group_id=199733&atid=970682
     * @param url
     * @param mon
     * @return
     * @throws IOException
     */
    public static File getHtmlFile(URL url, ProgressMonitor mon) throws IOException {
        return getFile(url.toString(), true, mon);
    }

    /**
     * get a URI from the string which is believed to be valid.  This was introduced
     * because a number of codes called getURI without checking for null, which could be
     * returned when the URI could not be parsed ("This is not a uri").  Codes that
     * didn't check would produce a null pointer exception, and now they will produce
     * a more accurate error. 
     * @param surl
     * @return
     * @throws URISyntaxException
     */
    public static URI getURIValid(String surl) throws URISyntaxException {
        URI result = getURI(surl);
        if (result == null) {
            throw new IllegalArgumentException("URI cannot be formed from \"" + surl + "\"");
        } else {
            return result;
        }
    }

    /**
     * canonical method for getting the Autoplot URI.  If no protocol is specified, then file:// is
     * used.  Note URIs may contain prefix like vap+bin:http://www.cdf.org/data.cdf.  The
     * result will start with an Autoplot scheme like "vap:" or "vap+cdf:"
     *
     * Note 20111117: "vap+cdaweb:" -> URI( "vap+cdaweb:file:///"  that's why this works to toUri doesn't.
     * @return the URI or null if it's clearly not a URI.
     * 
     */
    public static URI getURI(String surl) throws URISyntaxException {
        URISplit split = URISplit.maybeAddFile(surl, 0);
        if (split == null) return null;
        surl = split.surl;
        if (surl.endsWith("://")) {
            surl += "/";
        }
        surl = surl.replaceAll("%([^0-9])", "%25$1");
        surl = surl.replaceAll("<", "%3C");
        surl = surl.replaceAll(">", "%3E");
        surl = surl.replaceAll(" ", "%20");
        if (split.vapScheme != null) {
            if (split.vapScheme.contains(" ")) {
                split.vapScheme = split.vapScheme.replace(" ", "+");
            }
            surl = split.vapScheme + ":" + surl;
        }
        surl = URISplit.format(URISplit.parse(surl));
        if (!(surl.startsWith("vap"))) {
            URISplit split2 = URISplit.parse(surl);
            String vapScheme = URISplit.implicitVapScheme(split2);
            if (vapScheme.contains("&")) {
                throw new IllegalArgumentException("Address contains ampersand in what looks like a filename: " + surl);
            }
            if (vapScheme.equals("")) {
                vapScheme = "vap+X";
            }
            surl = vapScheme + ":" + surl;
        }
        URI result = new URI(surl);
        return result;
    }

    /**
     * canonical method for getting the URL.  These will always be web-downloadable 
     * URLs.
     * @return null or the URL if available.
     */
    public static URL getURL(String surl) throws MalformedURLException {
        try {
            URI uri = getURIValid(surl);
            return getWebURL(uri);
        } catch (URISyntaxException ex) {
            throw new MalformedURLException(ex.getMessage());
        }
    }

    public static class CompletionResult {

        public String completion;

        public String doc;

        public String completable;

        public String label;

        public boolean maybePlot;

        protected CompletionResult(String completion, String doc) {
            this(completion, doc, null, false);
        }

        protected CompletionResult(String completion, String doc, boolean maybePlot) {
            this(completion, null, doc, null, maybePlot);
        }

        protected CompletionResult(String completion, String doc, String completable, boolean maybePlot) {
            this(completion, null, doc, completable, maybePlot);
        }

        /**
         * @param completion
         * @param label the presentation string
         * @param doc a descriptive string, for example used in a tooltip
         * @param completable the string that is being completed. (not used)
         * @param maybePlot true indicates accepting the completion should result in a valid URI.
         */
        protected CompletionResult(String completion, String label, String doc, String completable, boolean maybePlot) {
            this.completion = completion;
            this.completable = completable;
            this.label = label != null ? label : (completable != null ? completable : completion);
            this.doc = doc;
            this.maybePlot = maybePlot;
        }
    }

    /**
     * this is never used in the application code.  It must be left over from an earlier system.
     * This is used in Test005, some scripts, and IDL codes, so don't delete it!
     * @param surl
     * @param carotpos
     * @param mon
     * @return
     * @throws Exception
     */
    public static List<CompletionResult> getCompletions(final String surl, final int carotpos, ProgressMonitor mon) throws Exception {
        if (carotpos == 0 || (!surl.substring(0, carotpos).contains(":") && (carotpos < 4 && surl.substring(0, carotpos).equals("vap".substring(0, carotpos)) || (surl.length() > 3 && surl.substring(0, 3).equals("vap"))))) {
            return getTypesCompletions(surl, carotpos, mon);
        }
        URISplit split = URISplit.parse(surl, carotpos, true);
        if (split.file == null || (split.resourceUriCarotPos > split.file.length()) && DataSourceRegistry.getInstance().hasSourceByExt(DataSetURI.getExt(surl))) {
            return getFactoryCompletions(URISplit.format(split), split.formatCarotPos, mon);
        } else {
            int firstSlashAfterHost = split.authority == null ? 0 : split.authority.length();
            if (split.resourceUriCarotPos <= firstSlashAfterHost) {
                return getHostCompletions(URISplit.format(split), split.formatCarotPos, mon);
            } else {
                return getFileSystemCompletions(URISplit.format(split), split.formatCarotPos, true, true, null, mon);
            }
        }
    }

    public static List<CompletionResult> getHostCompletions(final String surl, final int carotpos, ProgressMonitor mon) throws IOException {
        URISplit split = URISplit.parse(surl.substring(0, carotpos));
        String prefix;
        String surlDir;
        if (split.path == null) {
            prefix = "";
            surlDir = "";
        } else {
            prefix = split.file.substring(split.path.length());
            surlDir = split.path;
        }
        mon.setLabel("getting list of cache hosts");
        String[] s;
        if (split.scheme == null) {
            List<DataSetURI.CompletionResult> completions = new ArrayList<DataSetURI.CompletionResult>();
            s = new String[] { "ftp://", "http://", "https://", "file:///", "sftp://" };
            for (int j = 0; j < s.length; j++) {
                completions.add(new DataSetURI.CompletionResult(s[j] + surl + "/", s[j] + surl + "/"));
            }
            return completions;
        }
        File cacheF = new File(FileSystem.settings().getLocalCacheDir(), split.scheme);
        if (!cacheF.exists()) return Collections.emptyList();
        s = cacheF.list();
        boolean foldCase = true;
        if (foldCase) {
            prefix = prefix.toLowerCase();
        }
        List<DataSetURI.CompletionResult> completions = new ArrayList<DataSetURI.CompletionResult>(s.length);
        for (int j = 0; j < s.length; j++) {
            String scomp = foldCase ? s[j].toLowerCase() : s[j];
            if (scomp.startsWith(prefix)) {
                String result1 = s[j] + "/";
                String[] s2 = new File(cacheF, result1).list();
                while (s2.length == 1 && new File(cacheF, result1 + "/" + s2[0]).isDirectory()) {
                    result1 += s2[0] + "/";
                    s2 = new File(cacheF, result1).list();
                }
                completions.add(new DataSetURI.CompletionResult(surlDir + result1, result1, null, surl.substring(0, carotpos), true));
            }
        }
        if (completions.size() == 1) {
            if ((completions.get(0).completion).equals(surlDir + prefix + "/")) {
            }
        }
        return completions;
    }

    public static List<CompletionResult> getFileSystemAggCompletions(final String surl, final int carotpos, ProgressMonitor mon) throws IOException, URISyntaxException {
        URISplit split = URISplit.parse(surl.substring(0, carotpos), carotpos, false);
        String prefix = URISplit.uriDecode(split.file.substring(split.path.length()));
        String surlDir = URISplit.uriDecode(split.path);
        mon.setLabel("getting remote listing");
        FileSystem fs = null;
        String[] s;
        fs = FileSystem.create(DataSetURI.toUri(surlDir));
        s = fs.listDirectory("/");
        Arrays.sort(s);
        boolean foldCase = Boolean.TRUE.equals(fs.getProperty(FileSystem.PROP_CASE_INSENSITIVE));
        if (foldCase) {
            prefix = prefix.toLowerCase();
        }
        List<DataSetURI.CompletionResult> completions = new ArrayList<DataSetURI.CompletionResult>(5);
        String[] s2 = new String[s.length];
        for (int i = 0; i < s.length; i++) {
            s2[i] = surlDir + s[i];
        }
        if (s2.length > 0) {
            List<String> files = new LinkedList(Arrays.asList(s2));
            List<String> saggs = DataSourceUtil.findAggregations(files, true);
            for (String sagg : saggs) {
                sagg = URISplit.removeParam(sagg, "timerange");
                completions.add(new DataSetURI.CompletionResult(sagg, "Use aggregation", true));
            }
        }
        return completions;
    }

    /**
     *
     * @param surl
     * @param carotpos
     * @param inclAgg include aggregations it sees.  These are a guess.
     * @param inclFiles include files as well as aggregations.
     * @param acceptPattern  if non-null, files and aggregations much match this.
     * @param mon
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public static List<CompletionResult> getFileSystemCompletions(final String surl, final int carotpos, boolean inclAgg, boolean inclFiles, String acceptPattern, ProgressMonitor mon) throws IOException, URISyntaxException {
        URISplit split = URISplit.parse(surl.substring(0, carotpos), carotpos, false);
        String prefix = URISplit.uriDecode(split.file.substring(split.path.length()));
        String surlDir = URISplit.uriDecode(split.path);
        mon.setLabel("getting remote listing");
        FileSystem fs = null;
        String[] s;
        if (surlDir.equals("file:") || surlDir.equals("file://")) {
            surlDir = "file:///";
            CompletionResult t0;
            if (split.vapScheme != null) {
                t0 = new CompletionResult(split.vapScheme + ":" + "file:///", "need three slashes");
            } else {
                t0 = new CompletionResult("file:///", "need three slashes");
            }
            List<DataSetURI.CompletionResult> completions = Collections.singletonList(t0);
            return completions;
        }
        boolean onlyAgg = false;
        String prefixPrefix = "";
        if (surlDir.contains("$Y")) {
            int ip = surlDir.indexOf("$Y");
            String s1 = surlDir.substring(0, ip);
            String s2 = surlDir.substring(ip, surlDir.length() - 1);
            FileSystem fsp = FileSystem.create(DataSetURI.toUri(s1), mon);
            FileStorageModelNew fsm = FileStorageModelNew.create(fsp, s2);
            fs = fsp;
            List<String> ss = new ArrayList();
            String[] ss2 = fsm.getNamesFor(null);
            int nn = Math.min(2, ss2.length);
            for (int i = 0; i < nn; i++) {
                if (i == 1) i = ss2.length - 1;
                FileSystem fsm2 = FileSystem.create(DataSetURI.toUri(s1 + ss2[i]));
                String[] ss3 = fsm2.listDirectory("/");
                for (int ii = 0; ii < ss3.length; ii++) {
                    ss3[ii] = ss2[i] + '/' + ss3[ii];
                }
                ss.addAll(Arrays.asList(ss3));
            }
            s = ss.toArray(new String[ss.size()]);
            surlDir = s1;
            onlyAgg = true;
            prefixPrefix = s2 + '/';
        } else {
            if (surlDir.startsWith("file:/") && !(surlDir.contains(".zip/") || surlDir.contains(".ZIP/"))) {
                if (!new File(new URL(split.path).getPath()).exists()) {
                    throw new FileNotFoundException("directory does not exist: " + split.path);
                }
            }
            fs = FileSystem.create(DataSetURI.toUri(surlDir), mon);
            s = fs.listDirectory("/");
        }
        if (acceptPattern != null) {
            Pattern p = Pattern.compile(acceptPattern);
            List<String> res = new ArrayList<String>(s.length);
            for (int i = 0; i < s.length; i++) {
                if (s[i].endsWith("/")) {
                    res.add(s[i]);
                } else if (p.matcher(s[i]).matches()) {
                    res.add(s[i]);
                }
            }
            s = res.toArray(new String[res.size()]);
        }
        Arrays.sort(s, new Comparator<String>() {

            public int compare(String o1, String o2) {
                boolean d1 = o1.startsWith(".");
                boolean d2 = o2.startsWith(".");
                if (d1 == d2) {
                    return o1.compareTo(o2);
                } else if (d1) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        boolean foldCase = Boolean.TRUE.equals(fs.getProperty(FileSystem.PROP_CASE_INSENSITIVE));
        if (foldCase) {
            prefix = prefix.toLowerCase();
        }
        if (prefixPrefix.length() > 0) {
            prefix = prefixPrefix + prefix;
        }
        List<DataSetURI.CompletionResult> completions = new ArrayList<DataSetURI.CompletionResult>(s.length);
        String[] s2 = new String[s.length];
        for (int i = 0; i < s.length; i++) {
            s2[i] = surlDir + s[i];
        }
        if (s2.length > 0 && inclAgg) {
            List<String> files = new LinkedList(Arrays.asList(s2));
            List<String> saggs = DataSourceUtil.findAggregations(files, true, onlyAgg);
            if (onlyAgg) {
                completions.clear();
            }
            for (String sagg : saggs) {
                URISplit split2 = URISplit.parse(sagg);
                Map<String, String> params2 = URISplit.parseParams(split2.params);
                String tr = params2.remove("timerange");
                if (params2.size() == 0) {
                    split2.params = null;
                } else {
                    split2.params = URISplit.formatParams(params2);
                }
                if (split2.vapScheme != null && !sagg.startsWith(split2.vapScheme)) split2.vapScheme = null;
                String scomp = URISplit.format(split2);
                if (split2.vapScheme == null && split.vapScheme != null) split2.vapScheme = split.vapScheme;
                sagg = URISplit.format(split2);
                scomp = scomp.substring(surlDir.length());
                if (scomp.startsWith(prefix)) {
                    completions.add(new DataSetURI.CompletionResult(sagg, "Use aggregation (" + tr + " available)", true));
                }
            }
        }
        if (!onlyAgg) {
            for (int j = 0; j < s.length; j++) {
                String scomp = foldCase ? s[j].toLowerCase() : s[j];
                if (scomp.startsWith(prefix)) {
                    if (s[j].endsWith("contents.html")) {
                        s[j] = s[j].substring(0, s[j].length() - "contents.html".length());
                    }
                    if (s[j].endsWith(".zip") || s[j].endsWith(".ZIP")) s[j] = s[j] + "/";
                    if (!(inclFiles || s[j].endsWith("/"))) continue;
                    String completion = surlDir + s[j];
                    completion = DataSetURI.newUri(surl, completion);
                    String label = s[j];
                    String completable = surl.substring(0, carotpos);
                    boolean maybePlot = true;
                    if (completion.startsWith("file://" + completable)) {
                        completion = completion.substring(7);
                    }
                    completions.add(new DataSetURI.CompletionResult(completion, label, null, completable, maybePlot));
                }
            }
        }
        if (completions.size() == 1) {
            if ((completions.get(0)).completion.equals(surlDir + prefix + "/")) {
            }
        }
        return completions;
    }

    public static List<CompletionResult> getTypesCompletions(String surl, int carotpos, ProgressMonitor mon) throws Exception {
        List<CompletionContext> exts = DataSourceRegistry.getPlugins();
        List<CompletionResult> completions = new ArrayList();
        String prefix = surl.substring(0, carotpos);
        String suffix = "";
        if (surl.startsWith("vap:")) {
            suffix = surl.substring(4);
        }
        for (CompletionContext cc : exts) {
            if (cc.completable.startsWith(prefix)) {
                completions.add(new CompletionResult(cc.completable + suffix, cc.completable, null, cc.completable, false));
            }
        }
        String labelPrefix = "";
        return completions;
    }

    public static List<CompletionResult> getFactoryCompletions(String surl1, int carotPos, ProgressMonitor mon) throws Exception {
        CompletionContext cc = new CompletionContext();
        URISplit split = URISplit.parse(surl1);
        int qpos = surl1.lastIndexOf('?', carotPos);
        if (qpos == -1 && surl1.contains(":") && (surl1.endsWith(":") || surl1.contains("&"))) {
            qpos = surl1.indexOf(":");
        }
        if (qpos == -1 && surl1.contains(":") && split.file == null) {
            qpos = surl1.indexOf(":");
        }
        cc.surl = surl1;
        cc.surlpos = carotPos;
        List<CompletionResult> result = new ArrayList<CompletionResult>();
        if (qpos != -1 && qpos < carotPos) {
            if (qpos == -1) {
                qpos = surl1.length();
            }
            int eqpos = surl1.lastIndexOf('=', carotPos - 1);
            int amppos = surl1.lastIndexOf('&', carotPos - 1);
            if (amppos == -1) {
                amppos = qpos;
            }
            if (eqpos > amppos) {
                cc.context = CompletionContext.CONTEXT_PARAMETER_VALUE;
                cc.completable = surl1.substring(eqpos + 1, carotPos);
                cc.completablepos = carotPos - (eqpos + 1);
            } else {
                cc.context = CompletionContext.CONTEXT_PARAMETER_NAME;
                cc.completable = surl1.substring(amppos + 1, carotPos);
                cc.completablepos = carotPos - (amppos + 1);
                if (surl1.length() > carotPos && surl1.charAt(carotPos) != '&') {
                    surl1 = surl1.substring(0, carotPos) + '&' + surl1.substring(carotPos);
                    split = URISplit.parse(surl1);
                }
            }
        } else {
            cc.context = CompletionContext.CONTEXT_FILE;
            qpos = surl1.indexOf('?', carotPos);
            if (qpos == -1) {
                cc.completable = surl1;
            } else {
                cc.completable = surl1.substring(0, qpos);
            }
            cc.completablepos = carotPos;
        }
        if (cc.context == CompletionContext.CONTEXT_PARAMETER_NAME) {
            DataSourceFactory factory = getDataSourceFactory(getURIValid(surl1), new NullProgressMonitor());
            if (factory == null) {
                throw new IllegalArgumentException("unable to find data source factory");
            }
            String suri = CompletionContext.get(CompletionContext.CONTEXT_FILE, cc);
            if (suri == null) {
                suri = cc.surl;
            }
            URI uri = DataSetURI.getURIValid(suri);
            cc.resourceURI = DataSetURI.getResourceURI(uri);
            cc.params = split.params;
            List<CompletionContext> completions = factory.getCompletions(cc, mon);
            Map params = URISplit.parseParams(split.params);
            for (int i = 0; i < 3; i++) {
                params.remove("arg_" + i);
            }
            int i = 0;
            for (CompletionContext cc1 : completions) {
                String paramName = cc1.implicitName != null ? cc1.implicitName : cc1.completable;
                if (paramName.indexOf("=") != -1) {
                    paramName = paramName.substring(0, paramName.indexOf("="));
                }
                boolean dontYetHave = !params.containsKey(paramName);
                boolean startsWith = cc1.completable.startsWith(cc.completable);
                if (startsWith) {
                    LinkedHashMap paramsCopy = new LinkedHashMap(params);
                    if (cc1.implicitName != null) {
                        paramsCopy.put(cc1.implicitName, cc1.completable);
                    } else {
                        paramsCopy.put(cc1.completable, null);
                    }
                    String ss = (split.vapScheme == null ? "" : (split.vapScheme + ":")) + split.file + "?" + URISplit.formatParams(paramsCopy);
                    if (dontYetHave == false) {
                        continue;
                    }
                    result.add(new CompletionResult(ss, cc1.label, cc1.doc, surl1.substring(0, carotPos), cc1.maybePlot));
                    i = i + 1;
                }
            }
            return result;
        } else if (cc.context == CompletionContext.CONTEXT_PARAMETER_VALUE) {
            String file = CompletionContext.get(CompletionContext.CONTEXT_FILE, cc);
            DataSourceFactory factory = getDataSourceFactory(getURIValid(surl1), mon);
            if (file != null) {
                URI uri = DataSetURI.getURIValid(file);
                cc.resourceURI = DataSetURI.getResourceURI(uri);
            }
            cc.params = split.params;
            if (factory == null) {
                throw new IllegalArgumentException("unable to find data source factory");
            }
            List<CompletionContext> completions = factory.getCompletions(cc, mon);
            int i = 0;
            for (CompletionContext cc1 : completions) {
                if (cc1.completable.startsWith(cc.completable)) {
                    String ss = CompletionContext.insert(cc, cc1);
                    if (split.vapScheme != null && !ss.startsWith(split.vapScheme)) ss = split.vapScheme + ":" + ss;
                    result.add(new CompletionResult(ss, cc1.label, cc1.doc, surl1.substring(0, carotPos), cc1.maybePlot));
                    i = i + 1;
                }
            }
            return result;
        } else {
            try {
                mon.setProgressMessage("listing directory");
                mon.started();
                String surl = CompletionContext.get(CompletionContext.CONTEXT_FILE, cc);
                if (surl == null) {
                    throw new MalformedURLException("unable to process");
                }
                int surlPos = cc.surl.indexOf(surl);
                if (surlPos == -1) surlPos = 0;
                int newCarotPos = carotPos - surlPos;
                int i = surl.lastIndexOf("/", newCarotPos - 1);
                String surlDir;
                if (i <= 0) {
                    surlDir = surl;
                } else if (surl.charAt(i - 1) == '/') {
                    surlDir = surl.substring(0, i + 1);
                } else {
                    surlDir = surl.substring(0, i + 1);
                }
                URI url = getURIValid(surlDir);
                String prefix = surl.substring(i + 1, newCarotPos);
                FileSystem fs = FileSystem.create(getWebURL(url), new NullProgressMonitor());
                String[] s = fs.listDirectory("/");
                mon.finished();
                for (int j = 0; j < s.length; j++) {
                    if (s[j].startsWith(prefix)) {
                        CompletionContext cc1 = new CompletionContext(CompletionContext.CONTEXT_FILE, surlDir + s[j]);
                        result.add(new CompletionResult(CompletionContext.insert(cc, cc1), cc1.label, cc1.doc, surl1.substring(0, carotPos), true));
                    }
                }
            } catch (MalformedURLException ex) {
                result = Collections.singletonList(new CompletionResult("Malformed URI", "Something in the URL prevents processing", surl1.substring(0, carotPos), false));
            } catch (FileSystem.FileSystemOfflineException ex) {
                result = Collections.singletonList(new CompletionResult("FileSystem offline", "FileSystem is offline.", surl1.substring(0, carotPos), false));
            } finally {
                mon.finished();
            }
            return result;
        }
    }

    /** call this to trigger initialization */
    public static void init() {
    }

    public static void main(String[] args) throws MalformedURLException, IOException {
        System.err.println(getResourceURI("file:C:\\documents and settings\\jbf\\pngwalk"));
        URL url = new URL("http://apps-pw/hudson/job/autoplot-release/lastSuccessfulBuild/artifact/autoplot/VirboAutoplot/dist/logo64x64.png");
        File x = downloadResourceAsTempFile(url, new NullProgressMonitor());
        System.err.println(x);
    }
}
