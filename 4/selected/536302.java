package au.edu.educationau.opensource.dsm.cache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import au.edu.educationau.opensource.dsm.util.Flog;
import au.edu.educationau.opensource.dsm.util.EducationAuUtils;
import au.edu.educationau.opensource.dsm.util.URIEncoder;

/** Disk Cache entry value object */
public class DiskCacheEntry {

    private File file = null;

    private String url = null;

    private int status = DiskCache.IDLE;

    private String root = "";

    private String hashCode = "";

    private Object lock = new Object();

    private long itemRefreshPolicy = DiskCache.POLICY_UNIT_HOUR;

    private boolean flushOutOfCache = false;

    private int responseCode = HttpURLConnection.HTTP_OK;

    /** Class name display */
    public String toString() {
        return "o.m.j.c.DiskCacheEntry";
    }

    /**
	 * Only constructor. URL required.
	 * 
	 * @param url
	 */
    public DiskCacheEntry(String url) {
        this.setURL(url);
        if (null != url) {
            setHashCode(URIEncoder.hashURL(url));
        }
    }

    /**
	 * Loads the data from the URL supplied in the constructor
	 * 
	 * @param root
	 */
    public synchronized void load(String root) {
        load(root, "ASCII");
    }

    /**
	 * Loads the data from the URL supplied in the constructor
	 * 
	 * @param root
	 */
    public synchronized void load(String root, String encoding) {
        try {
            this.setStatus(DiskCache.LOADING);
            this.root = root;
            String prepend = "";
            File tmpFile = new File(root + File.separator + prepend + getHashCode());
            try {
                Object[] fetchedObj = EducationAuUtils.fetch(this.url, this.hashCode, root, prepend, encoding);
                this.setResponseCode(((Integer) fetchedObj[0]).intValue());
                this.setFile((File) fetchedObj[1]);
            } catch (IOException ie) {
                try {
                    responseCode = Integer.parseInt(ie.getMessage().substring(0, 3));
                } catch (NumberFormatException nfe) {
                    responseCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
                }
                if (null != tmpFile && tmpFile.exists()) {
                    this.setFile(tmpFile);
                }
                throw ie;
            }
            this.setStatus(DiskCache.HASDATA);
        } catch (Exception o) {
            setStatus(DiskCache.ERROR);
            Flog.error(toString(), "CACHE GOT ERROR WHILE GETTING url: " + this.url + ": " + o.getMessage(), null);
        }
    }

    /**
	 * Set the status of this Disk Cache entry
	 * 
	 * @param status
	 */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
	 * Get the status of this Disk Cache entry
	 */
    public int getStatus() {
        return this.status;
    }

    /**
	 * Set the responseCode of this Disk Cache entry
	 * 
	 * @param responseCode
	 */
    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    /**
	 * Get the responseCode of this Disk Cache entry
	 */
    public int getResponseCode() {
        return this.responseCode;
    }

    /**
	 * Sets the hash code of this entry
	 */
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    /**
	 * Returns the URL of this entry
	 */
    public String getURL() {
        return this.url;
    }

    /**
	 * Returns the hash code of this entry
	 */
    public String getHashCode() {
        return this.hashCode;
    }

    /**
	 * Sets the entry's file in the disk cache directory. The data is from the
	 * URL
	 * 
	 * @param file
	 */
    public void setFile(File file) {
        if (file.exists()) {
            this.file = file;
        }
    }

    /**
	 * Returns this entry's data in a String form without refreshing
	 * 
	 * @exception IOException
	 */
    public String getData() throws IOException {
        return getData(false, "ASCII");
    }

    /**
	 * Open this URL and read its data, then return it as a string
	 * 
	 * @param refresh
	 *            reload the data on the way back
	 * @exception IOException
	 */
    public String getData(boolean refresh, String encoding) throws IOException {
        Reader is = this.getReader(refresh, encoding);
        StringWriter bos = new StringWriter();
        char chars[] = new char[200];
        int readCount = 0;
        while ((readCount = is.read(chars)) > 0) {
            bos.write(chars, 0, readCount);
        }
        is.close();
        return bos.toString();
    }

    /**
	 * Returns this entry's data in a Reader without refreshing
	 * 
	 * @exception IOException
	 */
    public Reader getReader() throws IOException {
        return getReader(false, "ASCII");
    }

    /**
	 * Returns this entry's data in a Reader without refreshing
	 * 
	 * @param encoding
	 * @exception IOException
	 */
    public Reader getReader(String encoding) throws IOException {
        return getReader(false, encoding);
    }

    /**
	 * Returns this entry's data in a Reader
	 * 
	 * @param refresh
	 *            reload the data on the way back
	 * @exception IOException
	 */
    public Reader getReader(boolean refresh, String encoding) throws IOException {
        if (this.getFile() == null) {
            return null;
        }
        InputStreamReader reader = null;
        synchronized (lock) {
            if (!this.getFile().exists() || refresh) {
                this.load(this.root, encoding);
            }
        }
        try {
            if (null == encoding) {
                reader = new FileReader(this.getFile());
            } else {
                reader = new InputStreamReader(new FileInputStream(this.getFile()), encoding);
            }
        } catch (UnsupportedEncodingException e) {
            Flog.warn(toString(), "Could not Decode file as " + encoding + ". Resorting to FS default encoding.");
            reader = new FileReader(this.getFile());
        }
        return reader;
    }

    /**
	 * Returns this entry's data in a BufferedReader
	 * 
	 * @exception IOException
	 */
    public BufferedReader getBufferedReader() throws IOException {
        BufferedReader reader = null;
        synchronized (lock) {
            if (!this.getFile().exists()) {
                this.load(this.root);
            }
        }
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.getFile()), "UTF8"));
        } catch (UnsupportedEncodingException e) {
            reader = new BufferedReader(new FileReader(this.getFile()));
        }
        return reader;
    }

    public FileInputStream getFileInputStream() throws IOException {
        return new FileInputStream(this.getFile());
    }

    /**
	 * Sets the item refresh policy POLICY_UNIT_HOUR or POLICY_UNIT_DAY
	 * 
	 * @param itemRefreshPolicy
	 *            POLICY_UNIT_HOUR or POLICY_UNIT_DAY
	 */
    public void setItemRefreshPolicy(int itemRefreshPolicy) {
        this.itemRefreshPolicy = itemRefreshPolicy;
    }

    /**
	 * Returns the item refresh policy POLICY_UNIT_HOUR or POLICY_UNIT_DAY
	 * 
	 * @return itemRefreshPolicy
	 */
    public long getItemRefreshPolicy() {
        return this.itemRefreshPolicy;
    }

    /**
	 * Set the url on which this is based.
	 * 
	 * @param url
	 */
    public void setURL(String url) {
        this.url = url;
    }

    /**
	 * Returns the java.io.File object that is associated with this entry
	 * 
	 * @return file the java.io.File object within which the data is stored. Can
	 *         be null.
	 */
    public File getFile() {
        return this.file;
    }

    /**
	 * Remove the entry from the cache after it expires
	 * 
	 * @param flushOutOfCache
	 */
    public void setFlushOutOfCache(boolean flushOutOfCache) {
        this.flushOutOfCache = flushOutOfCache;
    }

    /**
	 * Check if the entry needs to be removed from the cache after it expires
	 * return flushOutOfCache
	 */
    public boolean getFlushOutOfCache() {
        return flushOutOfCache;
    }
}
