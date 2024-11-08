package org.apache.jetspeed.cache.disk;

import org.apache.jetspeed.util.URIEncoder;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.threadpool.ThreadPool;
import org.apache.jetspeed.services.urlmanager.URLManager;
import org.apache.jetspeed.services.urlmanager.URLFetcher;
import org.apache.jetspeed.services.urlmanager.URLFetcherDownloader;
import org.apache.jetspeed.services.resources.JetspeedResources;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.servlet.ServletContext;
import org.apache.turbine.services.servlet.TurbineServlet;

/**
   <p>Sample Implementation of the Disk Cache interface.</p>
   <p>Entries are updated when DiskCacheDaemon runs</p>

@see DiskCache
@see org.apache.jetspeed.daemon.impl.DiskCacheDaemon
@author <A HREF="mailto:burton@apache.org">Kevin A. Burton</A>
@author <a href="mailto:sgala@hisitech.com">Santiago Gala</a>
@version $Id: JetspeedDiskCache.java,v 1.50 2004/02/23 02:45:29 jford Exp $
*/
public class JetspeedDiskCache implements DiskCache {

    /**
    The default cache folder
    */
    public static String DEFAULT_CACHE_DIRECTORY = JetspeedResources.getString(JetspeedResources.CACHE_DIRECTORY_KEY, "WEB-INF/cache");

    /**
    Stores instances of JetspeedDiskCaches
    */
    private static Hashtable instances = new Hashtable();

    /**
    This is the directory used to cache the documents.
    */
    private String directory;

    /**
    This is a hashtable with all the entries in this cache.
    */
    private Hashtable entries = new Hashtable();

    /**
     * Static initialization of the logger for this class
     */
    private static final JetspeedLogger logger = JetspeedLogFactoryService.getLogger(JetspeedDiskCache.class.getName());

    /**
    Create an instance of the document cache.
    */
    private JetspeedDiskCache(String directory) {
        this.directory = directory;
        if (DEFAULT_CACHE_DIRECTORY.equals("use-servlet-temp")) {
            String tempdir = new String("WEB-INF/cache");
            try {
                ServletContext sc = TurbineServlet.getServletContext();
                tempdir = sc.getAttribute("javax.servlet.context.tempdir").toString() + "/jetspeed/cache";
                if (logger.isDebugEnabled()) {
                    logger.debug("DISK CACHE: will create cache in servlet temp directory " + tempdir);
                }
            } catch (Exception e) {
                logger.error("DISK CACHE: problems creating cache in servlet temp directory " + " falling back to WEB-INF/cache : " + e);
            }
            this.directory = tempdir;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("DISK CACHE: will use cache in user configured directory " + directory);
            }
        }
    }

    /**

   Create entries in the hashtable corresponding to the cached files...
    
    @see DiskCache#getEntries
    */
    private void initEntries() {
        logger.info("Disk Cache init Entries...");
        try {
            org.apache.turbine.services.TurbineServices.getInstance().getService(org.apache.jetspeed.services.urlmanager.URLManagerService.SERVICE_NAME).init();
        } catch (Throwable t) {
            logger.error("initEntries: Unable to start URLManagerService", t);
        }
        File temp = new File(directory);
        String files[] = temp.list();
        if (files == null) {
            logger.error("DiskCache.initEntries: Error!!! - The cache directory cannot be found: " + directory);
        }
        for (int i = 0; i < files.length; ++i) {
            if (files[i].indexOf("http_") == 0) {
                logger.info("Initializing cache entry: " + files[i]);
                JetspeedDiskCacheEntry ent = new JetspeedDiskCacheEntry(new File(getRoot(), files[i]));
                logger.info("Adding cache entry for " + ent.getSourceURL());
                String interned = ent.getSourceURL().intern();
                entries.put(interned, ent);
                URLManager.register(interned, URLManager.STATUS_OK, "Recovered from cache");
            }
        }
        logger.info("Disk Cache init Entries DONE.");
    }

    /**
    Get a list of all the documents within the cache...

    Modified to create the entries in the hashtable...
    
    @see DiskCache#getEntries
    */
    public DiskCacheEntry[] getEntries() {
        Vector diskEntries = new Vector();
        Enumeration cacheEntries = entries.elements();
        logger.info("Calling JetspeedDiskCache getEntries");
        while (cacheEntries.hasMoreElements()) {
            diskEntries.addElement(cacheEntries.nextElement());
        }
        DiskCacheEntry[] found = new DiskCacheEntry[diskEntries.size()];
        diskEntries.copyInto(found);
        return found;
    }

    /**
    Return the root of this DiskCache
    
    @see DiskCache#getRoot
    */
    public String getRoot() {
        new File(this.directory).mkdirs();
        return this.directory;
    }

    /**
    @see DiskCache#getEntry( String url )
    */
    public DiskCacheEntry getEntry(String url) throws IOException {
        return getEntry(url, false);
    }

    /**
    Force this URL to update
    */
    public DiskCacheEntry getEntry(String url, boolean force) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("You must specify a URL to obtain an entry from the cache");
        }
        String interned = url.intern();
        JetspeedDiskCacheEntry entry = (JetspeedDiskCacheEntry) entries.get(interned);
        if (entry != null) {
            if (force) {
                logger.info("Refreshing local URL!!!" + interned);
                URLFetcher.refresh(interned);
            }
            return entry;
        }
        logger.warn("Cache getEntry Called with " + url);
        if (DiskCacheUtils.isLocal(url)) {
            String local = DiskCacheUtils.getLocalURL(url).intern();
            JetspeedDiskCacheEntry dce = (JetspeedDiskCacheEntry) entries.get(local);
            if (dce == null) {
                logger.info("Adding Local to cache list: " + local);
                dce = new JetspeedDiskCacheEntry(local);
                entries.put(local, dce);
                URLManager.register(local, URLManager.STATUS_OK, "Local added");
            }
            logger.info("Returning local cached URL");
            return dce;
        }
        if (DiskCacheUtils.isCacheable(url)) {
            if ((DiskCacheUtils.isCached(this, url) == false) || force) {
                this.add(url, true);
            }
            return this.getEntry(url, force);
        } else {
            logger.info("DiskCache: this URL can't be stored in cache... providing it directly." + url);
            return new JetspeedDiskCacheEntry(url);
        }
    }

    /**
     Get an Entry given a Reader and the URL from which it has been fetched.
    @see DiskCache#getEntry( String url, Reader is )
    */
    public DiskCacheEntry getEntry(String url, Reader is) throws IOException {
        String uri = URIEncoder.encode(url);
        String oldfilename = this.getRoot() + "/old." + uri;
        String filename = DiskCacheUtils.getFile(this, url).getAbsolutePath();
        String newfilename = this.getRoot() + "/new." + uri;
        File file = new File(DiskCacheUtils.getFile(this, url).getAbsolutePath());
        File newfile = new File(newfilename);
        OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(newfile), "utf-8");
        char chars[] = new char[200];
        int readCount = 0;
        while ((readCount = is.read(chars)) > 0) {
            os.write(chars, 0, readCount);
        }
        is.close();
        os.close();
        File oldfile = new File(oldfilename);
        if (oldfile.exists()) oldfile.delete();
        if (newfile.exists() && newfile.length() > 0) {
            file = new File(filename);
            file.renameTo(oldfile);
            newfile.renameTo(file);
        }
        try {
            if (oldfile.exists()) oldfile.delete();
        } catch (Exception e) {
            logger.info("Exception " + e.getMessage() + " while deleting " + oldfilename, e);
        }
        JetspeedDiskCacheEntry dce = (JetspeedDiskCacheEntry) entries.get(url.intern());
        if (dce != null) {
            dce.setFile(file);
            return dce;
        } else {
            return this.getEntry(url, false);
        }
    }

    /**
    @see DiskCache#remove( String url )
    */
    public void remove(String url) throws IOException {
        String uri = URIEncoder.encode(url);
        if (DiskCacheUtils.isCached(this, url)) {
            entries.remove(url.intern());
            URLManager.unregister(url.intern());
            File file = DiskCacheUtils.getFile(this, url);
            if (file.exists()) {
                file.delete();
            }
        }
        String oldfilename = this.getRoot() + "/old." + uri;
        File file = new File(oldfilename);
        if (file.exists()) {
            file.delete();
        }
        String newfilename = this.getRoot() + "/new." + uri;
        file = new File(newfilename);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
    @see DiskCache#add( String url )
    */
    public void add(String url) throws IOException {
        add(url, false);
    }

    /**
    @see DiskCache#add( String url )
    */
    public void add(String url, boolean force) throws IOException {
        String interned = url.intern();
        this.fetch(url, DiskCacheUtils.getFile(this, url).getAbsolutePath(), force);
        if (entries.get(interned) != null) return;
        entries.put(interned, new JetspeedDiskCacheEntry(interned));
        URLManager.register(interned, URLManager.STATUS_OK, "Added by Program");
    }

    /**

    @see DiskCache#fetch( String url, String cache )
    @param url the url to retrieve
    @param cache what file to store it in.
    */
    public String fetch(String url, String cache) throws IOException {
        return fetch(url, cache, false);
    }

    /**
    Pulls in the remote URL from the net and saves it to disk

    @see DiskCache#fetch( String url, String cache )
    @param url the url to retrieve
    @param cache what file to store it in.
    */
    public String fetch(String url, String cache, boolean force) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("url cannot be null");
        }
        if (cache == null) {
            throw new IllegalArgumentException("cache cannot be null");
        }
        try {
            Reader is = URLFetcher.fetch(url, force);
            OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(cache), "utf-8");
            char chars[] = new char[200];
            int readCount = 0;
            while ((readCount = is.read(chars)) > 0) {
                os.write(chars, 0, readCount);
            }
            is.close();
            os.close();
        } catch (MalformedURLException e) {
            logger.error("Error in URL", e);
        }
        return cache;
    }

    /**
    @see DiskCache#refresh
    */
    public void refresh(String url) {
        ThreadPool.process(new URLFetcherDownloader(url));
    }

    /**
    Return the default instance of the JetspeedDiskCache cache.
    */
    public static JetspeedDiskCache getInstance() {
        return JetspeedDiskCache.getInstance(DEFAULT_CACHE_DIRECTORY);
    }

    /**
    Return the default instance of the JetspeedDiskCache cache.
    
    @param location A directory to store the cache at.
    */
    public static JetspeedDiskCache getInstance(String directory) {
        synchronized (JetspeedDiskCache.instances) {
            JetspeedDiskCache cache = (JetspeedDiskCache) JetspeedDiskCache.instances.get(directory);
            if (cache == null) {
                cache = new JetspeedDiskCache(directory);
                JetspeedDiskCache.instances.put(directory, cache);
                logger.info("DISK CACHE: Initing cache for " + directory);
                cache.initEntries();
                logger.info("DISK CACHE: Inited cache:" + directory);
            }
            return cache;
        }
    }

    /**
    */
    public boolean isCached(String url) {
        return entries.containsKey(url.intern());
    }
}
