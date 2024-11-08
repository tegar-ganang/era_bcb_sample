package com.danga.memcached;

import java.util.*;
import java.util.zip.*;
import java.nio.*;
import java.net.InetAddress;
import java.nio.channels.*;
import java.io.*;
import java.net.URLEncoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is a Java client for the memcached server available from
 * <a href="http:/www.danga.com/memcached/">http://www.danga.com/memcached/</a>.
 * <br/>
 * Supports setting, adding, replacing, deleting compressed/uncompressed and<br/>
 * serialized (can be stored as string if object is native class) objects to memcached.<br/>
 * <br/>
 * Now pulls SockIO objects from SockIOPool, which is a connection pool.  The server failover<br/>
 * has also been moved into the SockIOPool class.<br/>
 * This pool needs to be initialized prior to the client working.  See javadocs from SockIOPool.<br/>
 * <br/>
 * Some examples of use follow.<br/>
 * <h3>To create cache client object and set params:</h3>
 * <pre>
 * 	MemCachedClient mc = new MemCachedClient();
 * <p/>
 * 	// compression is enabled by default
 * 	mc.setCompressEnable(true);
 * <p/>
 * 	// set compression threshhold to 4 KB (default: 15 KB)
 * 	mc.setCompressThreshold(4096);
 * <p/>
 * 	// turn on storing primitive types as a string representation
 * 	// Should not do this in most cases.
 * 	mc.setPrimitiveAsString(true);
 * </pre>
 * <h3>To store an object:</h3>
 * <pre>
 * 	MemCachedClient mc = new MemCachedClient();
 * 	String key   = "cacheKey1";
 * 	Object value = SomeClass.getObject();
 * 	mc.set(key, value);
 * </pre>
 * <h3>To store an object using a custom server hashCode:</h3>
 * <pre>
 * 	MemCachedClient mc = new MemCachedClient();
 * 	String key   = "cacheKey1";
 * 	Object value = SomeClass.getObject();
 * 	Integer hash = new Integer(45);
 * 	mc.set(key, value, hash);
 * </pre>
 * The set method shown above will always set the object in the cache.<br/>
 * The add and replace methods do the same, but with a slight difference.<br/>
 * <ul>
 * <li>add -- will store the object only if the server does not have an entry for this key</li>
 * <li>replace -- will store the object only if the server already has an entry for this key</li>
 * </ul>
 * <h3>To delete a cache entry:</h3>
 * <pre>
 * 	MemCachedClient mc = new MemCachedClient();
 * 	String key   = "cacheKey1";
 * 	mc.delete(key);
 * </pre>
 * <h3>To delete a cache entry using a custom hash code:</h3>
 * <pre>
 * 	MemCachedClient mc = new MemCachedClient();
 * 	String key   = "cacheKey1";
 * 	Integer hash = new Integer(45);
 * 	mc.delete(key, hashCode);
 * </pre>
 * <h3>To store a counter and then increment or decrement that counter:</h3>
 * <pre>
 * 	MemCachedClient mc = new MemCachedClient();
 * 	String key   = "counterKey";
 * 	mc.storeCounter(key, new Integer(100));
 * 	System.out.println("counter after adding      1: " mc.incr(key));
 * 	System.out.println("counter after adding      5: " mc.incr(key, 5));
 * 	System.out.println("counter after subtracting 4: " mc.decr(key, 4));
 * 	System.out.println("counter after subtracting 1: " mc.decr(key));
 * </pre>
 * <h3>To store a counter and then increment or decrement that counter with custom hash:</h3>
 * <pre>
 * 	MemCachedClient mc = new MemCachedClient();
 * 	String key   = "counterKey";
 * 	Integer hash = new Integer(45);
 * 	mc.storeCounter(key, new Integer(100), hash);
 * 	System.out.println("counter after adding      1: " mc.incr(key, 1, hash));
 * 	System.out.println("counter after adding      5: " mc.incr(key, 5, hash));
 * 	System.out.println("counter after subtracting 4: " mc.decr(key, 4, hash));
 * 	System.out.println("counter after subtracting 1: " mc.decr(key, 1, hash));
 * </pre>
 * <h3>To retrieve an object from the cache:</h3>
 * <pre>
 * 	MemCachedClient mc = new MemCachedClient();
 * 	String key   = "key";
 * 	Object value = mc.get(key);
 * </pre>
 * <h3>To retrieve an object from the cache with custom hash:</h3>
 * <pre>
 * 	MemCachedClient mc = new MemCachedClient();
 * 	String key   = "key";
 * 	Integer hash = new Integer(45);
 * 	Object value = mc.get(key, hash);
 * </pre>
 * <h3>To retrieve an multiple objects from the cache</h3>
 * <pre>
 * 	MemCachedClient mc = new MemCachedClient();
 * 	String[] keys      = { "key", "key1", "key2" };
 * 	Map&lt;Object&gt; values = mc.getMulti(keys);
 * </pre>
 * <h3>To retrieve an multiple objects from the cache with custom hashing</h3>
 * <pre>
 * 	MemCachedClient mc = new MemCachedClient();
 * 	String[] keys      = { "key", "key1", "key2" };
 * 	Integer[] hashes   = { new Integer(45), new Integer(32), new Integer(44) };
 * 	Map&lt;Object&gt; values = mc.getMulti(keys, hashes);
 * </pre>
 * <h3>To flush all items in server(s)</h3>
 * <pre>
 * 	MemCachedClient mc = new MemCachedClient();
 * 	mc.flushAll();
 * </pre>
 * <h3>To get stats from server(s)</h3>
 * <pre>
 * 	MemCachedClient mc = new MemCachedClient();
 * 	Map stats = mc.stats();
 * </pre>
 *
 * @author greg whalin <greg@meetup.com>
 * @author Richard 'toast' Russo <russor@msoe.edu>
 * @author Kevin Burton <burton@peerfear.org>
 * @author Robert Watts <robert@wattsit.co.uk>
 * @author Vin Chawla <vin@tivo.com>
 * @version 1.5a
 */
public class MemCachedClient {

    private static Log log = LogFactory.getLog(MemCachedClient.class);

    private static final String VALUE = "VALUE";

    private static final String STATS = "STAT";

    private static final String ITEM = "ITEM";

    private static final String DELETED = "DELETED";

    private static final String NOTFOUND = "NOT_FOUND";

    private static final String STORED = "STORED";

    private static final String NOTSTORED = "NOT_STORED";

    private static final String OK = "OK";

    private static final String END = "END";

    private static final String ERROR = "ERROR";

    private static final String CLIENT_ERROR = "CLIENT_ERROR";

    private static final String SERVER_ERROR = "SERVER_ERROR";

    private static final byte[] B_END = "END\r\n".getBytes();

    private static final byte[] B_NOTFOUND = "NOT_FOUND\r\n".getBytes();

    private static final byte[] B_DELETED = "DELETED\r\r".getBytes();

    private static final byte[] B_STORED = "STORED\r\r".getBytes();

    private static final int COMPRESS_THRESH = 30720;

    public static final int MARKER_BYTE = 1;

    public static final int MARKER_BOOLEAN = 8192;

    public static final int MARKER_INTEGER = 4;

    public static final int MARKER_LONG = 16384;

    public static final int MARKER_CHARACTER = 16;

    public static final int MARKER_STRING = 32;

    public static final int MARKER_STRINGBUFFER = 64;

    public static final int MARKER_FLOAT = 128;

    public static final int MARKER_SHORT = 256;

    public static final int MARKER_DOUBLE = 512;

    public static final int MARKER_DATE = 1024;

    public static final int MARKER_STRINGBUILDER = 2048;

    public static final int MARKER_BYTEARR = 4096;

    public static final int F_COMPRESSED = 2;

    public static final int F_SERIALIZED = 8;

    private boolean sanitizeKeys;

    private boolean primitiveAsString;

    private boolean compressEnable;

    private long compressThreshold;

    private String defaultEncoding;

    private SockIOPool pool;

    private String poolName;

    private ClassLoader classLoader;

    private ErrorHandler errorHandler;

    /**
     * Creates a new instance of MemCachedClient.
     */
    public MemCachedClient() {
        init();
    }

    /**
     * Creates a new instance of MemCachedClient
     * accepting a passed in pool name.
     *
     * @param poolName name of SockIOPool
     */
    public MemCachedClient(String poolName) {
        this.poolName = poolName;
        init();
    }

    /**
     * Creates a new instance of MemCacheClient but
     * acceptes a passed in ClassLoader.
     *
     * @param classLoader ClassLoader object.
     */
    public MemCachedClient(ClassLoader classLoader) {
        this.classLoader = classLoader;
        init();
    }

    /**
     * Creates a new instance of MemCacheClient but
     * acceptes a passed in ClassLoader and a passed
     * in ErrorHandler.
     *
     * @param classLoader  ClassLoader object.
     * @param errorHandler ErrorHandler object.
     */
    public MemCachedClient(ClassLoader classLoader, ErrorHandler errorHandler) {
        this.classLoader = classLoader;
        this.errorHandler = errorHandler;
        init();
    }

    /**
     * Creates a new instance of MemCacheClient but
     * acceptes a passed in ClassLoader, ErrorHandler,
     * and SockIOPool name.
     *
     * @param classLoader  ClassLoader object.
     * @param errorHandler ErrorHandler object.
     * @param poolName     SockIOPool name
     */
    public MemCachedClient(ClassLoader classLoader, ErrorHandler errorHandler, String poolName) {
        this.classLoader = classLoader;
        this.errorHandler = errorHandler;
        this.poolName = poolName;
        init();
    }

    /**
     * Initializes client object to defaults.
     * <p/>
     * This enables compression and sets compression threshhold to 15 KB.
     */
    private void init() {
        this.sanitizeKeys = true;
        this.primitiveAsString = false;
        this.compressEnable = true;
        this.compressThreshold = COMPRESS_THRESH;
        this.defaultEncoding = "UTF-8";
        this.poolName = (this.poolName == null) ? "default" : this.poolName;
        this.pool = SockIOPool.getInstance(poolName);
    }

    /**
     * Sets an optional ClassLoader to be used for
     * serialization.
     *
     * @param classLoader the classloader
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Sets an optional ErrorHandler.
     *
     * @param errorHandler the error handler
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Enables/disables sanitizing keys by URLEncoding.
     *
     * @param sanitizeKeys if true, then URLEncode all keys
     */
    public void setSanitizeKeys(boolean sanitizeKeys) {
        this.sanitizeKeys = sanitizeKeys;
    }

    /**
     * Enables storing primitive types as their String values.
     *
     * @param primitiveAsString if true, then store all primitives as their string value.
     */
    public void setPrimitiveAsString(boolean primitiveAsString) {
        this.primitiveAsString = primitiveAsString;
    }

    /**
     * Sets default String encoding when storing primitives as Strings.
     * Default is UTF-8.
     *
     * @param defaultEncoding String
     */
    public void setDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

    /**
     * Enable storing compressed data, provided it meets the threshold requirements.
     * <p/>
     * If enabled, data will be stored in compressed form if it is<br/>
     * longer than the threshold length set with setCompressThreshold(int)<br/>
     * <br/>
     * The default is that compression is enabled.<br/>
     * <br/>
     * Even if compression is disabled, compressed data will be automatically<br/>
     * decompressed.
     *
     * @param compressEnable <CODE>true</CODE> to enable compression, <CODE>false</CODE> to disable compression
     */
    public void setCompressEnable(boolean compressEnable) {
        this.compressEnable = compressEnable;
    }

    /**
     * Sets the required length for data to be considered for compression.
     * <p/>
     * If the length of the data to be stored is not equal or larger than this value, it will
     * not be compressed.
     * <p/>
     * This defaults to 15 KB.
     *
     * @param compressThreshold required length of data to consider compression
     */
    public void setCompressThreshold(long compressThreshold) {
        this.compressThreshold = compressThreshold;
    }

    /**
     * Checks to see if key exists in cache.
     *
     * @param key the key to look for
     * @return true if key found in cache, false if not (or if cache is down)
     */
    public boolean keyExists(String key) {
        return (this.get(key, null, true, false) != null);
    }

    /**
     * Deletes an object from cache given cache key.
     *
     * @param key the key to be removed
     * @return <code>true</code>, if the data was deleted successfully
     */
    public boolean delete(String key) {
        return delete(key, null, null);
    }

    /**
     * Deletes an object from cache given cache key and expiration date.
     *
     * @param key    the key to be removed
     * @param expiry when to expire the record.
     * @return <code>true</code>, if the data was deleted successfully
     */
    public boolean delete(String key, Date expiry) {
        return delete(key, null, expiry);
    }

    /**
     * Deletes an object from cache given cache key, a delete time, and an optional hashcode.
     * <p/>
     * The item is immediately made non retrievable.<br/>
     * Keep in mind {@link #add(String,Object) add} and {@link #replace(String,Object) replace}<br/>
     * will fail when used with the same key will fail, until the server reaches the<br/>
     * specified time. However, {@link #set(String,Object) set} will succeed,<br/>
     * and the new value will not be deleted.
     *
     * @param key      the key to be removed
     * @param hashCode if not null, then the int hashcode to use
     * @param expiry   when to expire the record.
     * @return <code>true</code>, if the data was deleted successfully
     */
    public boolean delete(String key, Integer hashCode, Date expiry) {
        if (key == null) {
            log.error("null value for key passed to delete()");
            return false;
        }
        try {
            key = sanitizeKey(key);
        } catch (UnsupportedEncodingException e) {
            if (errorHandler != null) {
                errorHandler.handleErrorOnDelete(this, e, key);
            }
            log.error("failed to sanitize your key!", e);
            return false;
        }
        if (pool == null) {
            pool = SockIOPool.getInstance(poolName);
        }
        SockIOPool.SockIO sock = pool.getSock(key, hashCode);
        if (sock == null) {
            if (errorHandler != null) {
                errorHandler.handleErrorOnDelete(this, new IOException("no socket to server available"), key);
            }
            return false;
        }
        StringBuilder command = new StringBuilder("delete ").append(key);
        if (expiry != null) {
            command.append(" ").append(expiry.getTime() / 1000);
        }
        command.append("\r\n");
        try {
            sock.write(command.toString().getBytes());
            sock.flush();
            String line = sock.readLine();
            if (DELETED.equals(line)) {
                if (log.isDebugEnabled()) {
                    log.debug("++++ deletion of key: " + key + " from cache was a success");
                }
                sock.close();
                sock = null;
                return true;
            } else if (NOTFOUND.equals(line)) {
                if (log.isDebugEnabled()) {
                    log.debug("++++ deletion of key: " + key + " from cache failed as the key was not found");
                }
            } else {
                log.error("++++ error deleting key: " + key);
                log.error("++++ server response: " + line);
            }
        } catch (IOException e) {
            if (errorHandler != null) {
                errorHandler.handleErrorOnDelete(this, e, key);
            }
            log.error("++++ exception thrown while writing bytes to server on delete");
            log.error(e.getMessage(), e);
            try {
                sock.trueClose();
            } catch (IOException ioe) {
                log.error("++++ failed to close socket : " + sock.toString());
            }
            sock = null;
        }
        if (sock != null) {
            sock.close();
            sock = null;
        }
        return false;
    }

    /**
     * Append data on the server; only the key and the value are specified.<br/>
     * <strong>Causion: only string will be append with no error!</strong>
     *
     * @param key   key to append data under
     * @param value value to append
     * @return true, if the data was successfully appended
     */
    public boolean append(String key, Object value) {
        return set("append", key, value, null, null, true, 0);
    }

    /**
     * Cas stores data to the server; only the key, value are specified.
     *
     * @param key     key to store data under
     * @param value   value to store
     * @param cascode cas unique code
     * @return true, if the data was successfully stored
     */
    public boolean cas(String key, Object value, long cascode) {
        return set("cas", key, value, null, null, primitiveAsString, cascode);
    }

    /**
     * Cas stores data to the server; only the key, value, and a hashcode are specified.
     *
     * @param key      key to store data under
     * @param value    value to store
     * @param cascode  cas unique code
     * @param hashCode if not null, then the int hashcode to use
     * @return true, if the data was successfully stored
     */
    public boolean cas(String key, Object value, long cascode, Integer hashCode) {
        return set("cas", key, value, null, hashCode, primitiveAsString, 0);
    }

    /**
     * Cas stores data to the server; only the key, value, and an expiration time are specified.
     *
     * @param key     key to store data under
     * @param value   value to store
     * @param cascode cas unique code
     * @param expiry  when to expire the record
     * @return true, if the data was successfully stored
     */
    public boolean cas(String key, Object value, long cascode, Date expiry) {
        return set("cas", key, value, expiry, null, primitiveAsString, cascode);
    }

    /**
     * Cas stores data to the server; the key, value, hashcode, and an expiration time are specified.
     *
     * @param key      key to store data under
     * @param value    value to store
     * @param cascode  cas unique code
     * @param expiry   when to expire the record
     * @param hashCode if not null, then the int hashcode to use
     * @return true, if the data was successfully stored
     */
    public boolean cas(String key, Object value, long cascode, Date expiry, Integer hashCode) {
        return set("cas", key, value, expiry, hashCode, primitiveAsString, cascode);
    }

    /**
     * Stores data on the server; only the key and the value are specified.
     *
     * @param key   key to store data under
     * @param value value to store
     * @return true, if the data was successfully stored
     */
    public boolean set(String key, Object value) {
        return set("set", key, value, null, null, primitiveAsString, 0);
    }

    /**
     * Stores data on the server; only the key and the value are specified.
     *
     * @param key      key to store data under
     * @param value    value to store
     * @param hashCode if not null, then the int hashcode to use
     * @return true, if the data was successfully stored
     */
    public boolean set(String key, Object value, Integer hashCode) {
        return set("set", key, value, null, hashCode, primitiveAsString, 0);
    }

    /**
     * Stores data on the server; the key, value, and an expiration time are specified.
     *
     * @param key    key to store data under
     * @param value  value to store
     * @param expiry when to expire the record
     * @return true, if the data was successfully stored
     */
    public boolean set(String key, Object value, Date expiry) {
        return set("set", key, value, expiry, null, primitiveAsString, 0);
    }

    /**
     * Stores data on the server; the key, value, and an expiration time are specified.
     *
     * @param key      key to store data under
     * @param value    value to store
     * @param expiry   when to expire the record
     * @param hashCode if not null, then the int hashcode to use
     * @return true, if the data was successfully stored
     */
    public boolean set(String key, Object value, Date expiry, Integer hashCode) {
        return set("set", key, value, expiry, hashCode, primitiveAsString, 0);
    }

    /**
     * Adds data to the server; only the key and the value are specified.
     *
     * @param key   key to store data under
     * @param value value to store
     * @return true, if the data was successfully stored
     */
    public boolean add(String key, Object value) {
        return set("add", key, value, null, null, primitiveAsString, 0);
    }

    /**
     * Adds data to the server; the key, value, and an optional hashcode are passed in.
     *
     * @param key      key to store data under
     * @param value    value to store
     * @param hashCode if not null, then the int hashcode to use
     * @return true, if the data was successfully stored
     */
    public boolean add(String key, Object value, Integer hashCode) {
        return set("add", key, value, null, hashCode, primitiveAsString, 0);
    }

    /**
     * Adds data to the server; the key, value, and an expiration time are specified.
     *
     * @param key    key to store data under
     * @param value  value to store
     * @param expiry when to expire the record
     * @return true, if the data was successfully stored
     */
    public boolean add(String key, Object value, Date expiry) {
        return set("add", key, value, expiry, null, primitiveAsString, 0);
    }

    /**
     * Adds data to the server; the key, value, and an expiration time are specified.
     *
     * @param key      key to store data under
     * @param value    value to store
     * @param expiry   when to expire the record
     * @param hashCode if not null, then the int hashcode to use
     * @return true, if the data was successfully stored
     */
    public boolean add(String key, Object value, Date expiry, Integer hashCode) {
        return set("add", key, value, expiry, hashCode, primitiveAsString, 0);
    }

    /**
     * Updates data on the server; only the key and the value are specified.
     *
     * @param key   key to store data under
     * @param value value to store
     * @return true, if the data was successfully stored
     */
    public boolean replace(String key, Object value) {
        return set("replace", key, value, null, null, primitiveAsString, 0);
    }

    /**
     * Updates data on the server; only the key and the value and an optional hash are specified.
     *
     * @param key      key to store data under
     * @param value    value to store
     * @param hashCode if not null, then the int hashcode to use
     * @return true, if the data was successfully stored
     */
    public boolean replace(String key, Object value, Integer hashCode) {
        return set("replace", key, value, null, hashCode, primitiveAsString, 0);
    }

    /**
     * Updates data on the server; the key, value, and an expiration time are specified.
     *
     * @param key    key to store data under
     * @param value  value to store
     * @param expiry when to expire the record
     * @return true, if the data was successfully stored
     */
    public boolean replace(String key, Object value, Date expiry) {
        return set("replace", key, value, expiry, null, primitiveAsString, 0);
    }

    /**
     * Updates data on the server; the key, value, and an expiration time are specified.
     *
     * @param key      key to store data under
     * @param value    value to store
     * @param expiry   when to expire the record
     * @param hashCode if not null, then the int hashcode to use
     * @return true, if the data was successfully stored
     */
    public boolean replace(String key, Object value, Date expiry, Integer hashCode) {
        return set("replace", key, value, expiry, hashCode, primitiveAsString, 0);
    }

    /**
     * Stores data to cache.
     * <p/>
     * If data does not already exist for this key on the server, or if the key is being<br/>
     * deleted, the specified value will not be stored.<br/>
     * The server will automatically delete the value when the expiration time has been reached.<br/>
     * <br/>
     * If compression is enabled, and the data is longer than the compression threshold<br/>
     * the data will be stored in compressed form.<br/>
     * <br/>
     * As of the current release, all objects stored will use java serialization.
     *
     * @param cmdname  action to take (set, add, replace)
     * @param key      key to store cache under
     * @param value    object to cache
     * @param expiry   expiration
     * @param hashCode if not null, then the int hashcode to use
     * @param asString store this object as a string?
     * @param cascode  if not 0, will put cascode
     * @return true/false indicating success
     */
    private boolean set(String cmdname, String key, Object value, Date expiry, Integer hashCode, boolean asString, long cascode) {
        if (cmdname == null || cmdname.trim().equals("") || key == null) {
            log.error("key is null or cmd is null/empty for set()");
            return false;
        }
        try {
            key = sanitizeKey(key);
        } catch (UnsupportedEncodingException e) {
            if (errorHandler != null) {
                errorHandler.handleErrorOnSet(this, e, key);
            }
            log.error("failed to sanitize your key!", e);
            return false;
        }
        if (value == null) {
            log.error("trying to store a null value to cache");
            return false;
        }
        if (pool == null) {
            pool = SockIOPool.getInstance(poolName);
        }
        SockIOPool.SockIO sock = pool.getSock(key, hashCode);
        if (sock == null) {
            if (errorHandler != null) {
                errorHandler.handleErrorOnSet(this, new IOException("no socket to server available"), key);
            }
            return false;
        }
        if (expiry == null) {
            expiry = new Date(0);
        }
        int flags = 0;
        byte[] val;
        if (NativeHandler.isHandled(value)) {
            if (asString) {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("++++ storing data as a string for key: " + key + " for class: " + value.getClass().getName());
                    }
                    val = value.toString().getBytes(defaultEncoding);
                } catch (UnsupportedEncodingException ue) {
                    if (errorHandler != null) {
                        errorHandler.handleErrorOnSet(this, ue, key);
                    }
                    log.error("invalid encoding type used: " + defaultEncoding, ue);
                    sock.close();
                    sock = null;
                    return false;
                }
            } else {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Storing with native handler...");
                    }
                    flags |= NativeHandler.getMarkerFlag(value);
                    val = NativeHandler.encode(value);
                } catch (Exception e) {
                    if (errorHandler != null) {
                        errorHandler.handleErrorOnSet(this, e, key);
                    }
                    log.error("Failed to native handle obj", e);
                    sock.close();
                    sock = null;
                    return false;
                }
            }
        } else {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("++++ serializing for key: " + key + " for class: " + value.getClass().getName());
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                (new ObjectOutputStream(bos)).writeObject(value);
                val = bos.toByteArray();
                flags |= F_SERIALIZED;
            } catch (IOException e) {
                if (errorHandler != null) {
                    errorHandler.handleErrorOnSet(this, e, key);
                }
                log.error("failed to serialize obj", e);
                log.error(value.toString());
                sock.close();
                sock = null;
                return false;
            }
        }
        if (compressEnable && val.length > compressThreshold) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("++++ trying to compress data");
                    log.debug("++++ size prior to compression: " + val.length);
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream(val.length);
                GZIPOutputStream gos = new GZIPOutputStream(bos);
                gos.write(val, 0, val.length);
                gos.finish();
                val = bos.toByteArray();
                flags |= F_COMPRESSED;
                if (log.isDebugEnabled()) {
                    log.debug("++++ compression succeeded, size after: " + val.length);
                }
            } catch (IOException e) {
                if (errorHandler != null) {
                    errorHandler.handleErrorOnSet(this, e, key);
                }
                log.error("IOException while compressing stream: " + e.getMessage());
                log.error("storing data uncompressed");
            }
        }
        try {
            String cmd = (cascode <= 0) ? String.format("%s %s %d %d %d\r\n", cmdname, key, flags, (expiry.getTime() / 1000), val.length) : String.format("%s %s %d %d %d %d\r\n", cmdname, key, flags, (expiry.getTime() / 1000), val.length, cascode);
            sock.write(cmd.getBytes());
            sock.write(val);
            sock.write("\r\n".getBytes());
            sock.flush();
            String line = sock.readLine();
            if (log.isDebugEnabled()) {
                log.debug("++++ memcache cmd (result code): " + cmd + " (" + line + ")");
            }
            if (STORED.equals(line)) {
                if (log.isDebugEnabled()) {
                    log.debug("++++ data successfully stored for key: " + key);
                }
                sock.close();
                sock = null;
                return true;
            } else if (NOTSTORED.equals(line)) {
                if (log.isDebugEnabled()) {
                    log.debug("++++ data not stored in cache for key: " + key);
                }
            } else {
                log.error("++++ error storing data in cache for key: " + key + " -- length: " + val.length);
                log.error("++++ server response: " + line);
            }
        } catch (IOException e) {
            if (errorHandler != null) {
                errorHandler.handleErrorOnSet(this, e, key);
            }
            log.error("++++ exception thrown while writing bytes to server on set");
            log.error(e.getMessage(), e);
            try {
                sock.trueClose();
            } catch (IOException ioe) {
                log.error("++++ failed to close socket : " + sock.toString());
            }
            sock = null;
        }
        if (sock != null) {
            sock.close();
            sock = null;
        }
        return false;
    }

    /**
     * Store a counter to memcached given a key
     *
     * @param key     cache key
     * @param counter number to store
     * @return true/false indicating success
     */
    public boolean storeCounter(String key, long counter) {
        return set("set", key, counter, null, null, true, 0);
    }

    /**
     * Store a counter to memcached given a key
     *
     * @param key     cache key
     * @param counter number to store
     * @return true/false indicating success
     */
    public boolean storeCounter(String key, Long counter) {
        return set("set", key, counter, null, null, true, 0);
    }

    /**
     * Store a counter to memcached given a key
     *
     * @param key      cache key
     * @param counter  number to store
     * @param hashCode if not null, then the int hashcode to use
     * @return true/false indicating success
     */
    public boolean storeCounter(String key, Long counter, Integer hashCode) {
        return set("set", key, counter, null, hashCode, true, 0);
    }

    /**
     * Returns value in counter at given key as long.
     *
     * @param key cache ket
     * @return counter value or -1 if not found
     */
    public long getCounter(String key) {
        return getCounter(key, null);
    }

    /**
     * Returns value in counter at given key as long.
     *
     * @param key      cache ket
     * @param hashCode if not null, then the int hashcode to use
     * @return counter value or -1 if not found
     */
    public long getCounter(String key, Integer hashCode) {
        if (key == null) {
            log.error("null key for getCounter()");
            return -1;
        }
        long counter = -1;
        try {
            counter = Long.parseLong((String) get(key, hashCode, true, false));
        } catch (Exception ex) {
            if (errorHandler != null) {
                errorHandler.handleErrorOnGet(this, ex, key);
            }
            log.warn(String.format("Failed to parse Long value for key: %s", key));
        }
        return counter;
    }

    /**
     * Thread safe way to initialize and increment a counter.
     *
     * @param key key where the data is stored
     * @return value of incrementer
     */
    public long addOrIncr(String key) {
        return addOrIncr(key, 0, null);
    }

    /**
     * Thread safe way to initialize and increment a counter.
     *
     * @param key key where the data is stored
     * @param inc value to set or increment by
     * @return value of incrementer
     */
    public long addOrIncr(String key, long inc) {
        return addOrIncr(key, inc, null);
    }

    /**
     * Thread safe way to initialize and increment a counter.
     *
     * @param key      key where the data is stored
     * @param inc      value to set or increment by
     * @param hashCode if not null, then the int hashcode to use
     * @return value of incrementer
     */
    public long addOrIncr(String key, long inc, Integer hashCode) {
        boolean ret = set("add", key, inc, null, hashCode, true, 0);
        if (ret) {
            return inc;
        } else {
            return incrdecr("incr", key, inc, hashCode);
        }
    }

    /**
     * Thread safe way to initialize and decrement a counter.
     *
     * @param key key where the data is stored
     * @return value of incrementer
     */
    public long addOrDecr(String key) {
        return addOrDecr(key, 0, null);
    }

    /**
     * Thread safe way to initialize and decrement a counter.
     *
     * @param key key where the data is stored
     * @param inc value to set or increment by
     * @return value of incrementer
     */
    public long addOrDecr(String key, long inc) {
        return addOrDecr(key, inc, null);
    }

    /**
     * Thread safe way to initialize and decrement a counter.
     *
     * @param key      key where the data is stored
     * @param inc      value to set or increment by
     * @param hashCode if not null, then the int hashcode to use
     * @return value of incrementer
     */
    public long addOrDecr(String key, long inc, Integer hashCode) {
        boolean ret = set("add", key, inc, null, hashCode, true, 0);
        if (ret) {
            return inc;
        } else {
            return incrdecr("decr", key, inc, hashCode);
        }
    }

    /**
     * Increment the value at the specified key by 1, and then return it.
     *
     * @param key key where the data is stored
     * @return -1, if the key is not found, the value after incrementing otherwise
     */
    public long incr(String key) {
        return incrdecr("incr", key, 1, null);
    }

    /**
     * Increment the value at the specified key by passed in val.
     *
     * @param key key where the data is stored
     * @param inc how much to increment by
     * @return -1, if the key is not found, the value after incrementing otherwise
     */
    public long incr(String key, long inc) {
        return incrdecr("incr", key, inc, null);
    }

    /**
     * Increment the value at the specified key by the specified increment, and then return it.
     *
     * @param key      key where the data is stored
     * @param inc      how much to increment by
     * @param hashCode if not null, then the int hashcode to use
     * @return -1, if the key is not found, the value after incrementing otherwise
     */
    public long incr(String key, long inc, Integer hashCode) {
        return incrdecr("incr", key, inc, hashCode);
    }

    /**
     * Decrement the value at the specified key by 1, and then return it.
     *
     * @param key key where the data is stored
     * @return -1, if the key is not found, the value after incrementing otherwise
     */
    public long decr(String key) {
        return incrdecr("decr", key, 1, null);
    }

    /**
     * Decrement the value at the specified key by passed in value, and then return it.
     *
     * @param key key where the data is stored
     * @param inc how much to increment by
     * @return -1, if the key is not found, the value after incrementing otherwise
     */
    public long decr(String key, long inc) {
        return incrdecr("decr", key, inc, null);
    }

    /**
     * Decrement the value at the specified key by the specified increment, and then return it.
     *
     * @param key      key where the data is stored
     * @param inc      how much to increment by
     * @param hashCode if not null, then the int hashcode to use
     * @return -1, if the key is not found, the value after incrementing otherwise
     */
    public long decr(String key, long inc, Integer hashCode) {
        return incrdecr("decr", key, inc, hashCode);
    }

    /**
     * Increments/decrements the value at the specified key by inc.
     * <p/>
     * Note that the server uses a 32-bit unsigned integer, and checks for<br/>
     * underflow. In the event of underflow, the result will be zero.  Because<br/>
     * Java lacks unsigned types, the value is returned as a 64-bit integer.<br/>
     * The server will only decrement a value if it already exists;<br/>
     * if a value is not found, -1 will be returned.
     *
     * @param cmdname  increment/decrement
     * @param key      cache key
     * @param inc      amount to incr or decr
     * @param hashCode if not null, then the int hashcode to use
     * @return new value or -1 if not exist
     */
    private long incrdecr(String cmdname, String key, long inc, Integer hashCode) {
        if (key == null) {
            log.error("null key for incrdecr()");
            return -1;
        }
        try {
            key = sanitizeKey(key);
        } catch (UnsupportedEncodingException e) {
            if (errorHandler != null) {
                errorHandler.handleErrorOnGet(this, e, key);
            }
            log.error("failed to sanitize your key!", e);
            return -1;
        }
        if (pool == null) {
            pool = SockIOPool.getInstance(poolName);
        }
        SockIOPool.SockIO sock = pool.getSock(key, hashCode);
        if (sock == null) {
            if (errorHandler != null) {
                errorHandler.handleErrorOnSet(this, new IOException("no socket to server available"), key);
            }
            return -1;
        }
        try {
            String cmd = String.format("%s %s %d\r\n", cmdname, key, inc);
            if (log.isDebugEnabled()) {
                log.debug("++++ memcache incr/decr command: " + cmd);
            }
            sock.write(cmd.getBytes());
            sock.flush();
            String line = sock.readLine();
            if (line.matches("\\d+")) {
                sock.close();
                try {
                    return Long.parseLong(line);
                } catch (Exception ex) {
                    if (errorHandler != null) {
                        errorHandler.handleErrorOnGet(this, ex, key);
                    }
                    log.error(String.format("Failed to parse Long value for key: %s", key));
                }
            } else if (NOTFOUND.equals(line)) {
                if (log.isInfoEnabled()) {
                    log.info("++++ key not found to incr/decr for key: " + key);
                }
            } else {
                log.error("++++ error incr/decr key: " + key);
                log.error("++++ server response: " + line);
            }
        } catch (IOException e) {
            if (errorHandler != null) {
                errorHandler.handleErrorOnGet(this, e, key);
            }
            log.error("++++ exception thrown while writing bytes to server on incr/decr");
            log.error(e.getMessage(), e);
            try {
                sock.trueClose();
            } catch (IOException ioe) {
                log.error("++++ failed to close socket : " + sock.toString());
            }
            sock = null;
        }
        if (sock != null) {
            sock.close();
            sock = null;
        }
        return -1;
    }

    public GetsObject gets(String key) {
        GetsObject o = (GetsObject) get(key, null, false, true);
        if (o == null) return new GetsObject(0, null);
        return o;
    }

    /**
     * Retrieve a key from the server, using a specific hash.
     * <p/>
     * If the data was compressed or serialized when compressed, it will automatically<br/>
     * be decompressed or serialized, as appropriate. (Inclusive or)<br/>
     * <br/>
     * Non-serialized data will be returned as a string, so explicit conversion to<br/>
     * numeric types will be necessary, if desired<br/>
     *
     * @param key key where data is stored
     * @return the object that was previously stored, or null if it was not previously stored
     */
    public Object get(String key) {
        return get(key, null, false, false);
    }

    /**
     * Retrieve a key from the server, using a specific hash.
     * <p/>
     * If the data was compressed or serialized when compressed, it will automatically<br/>
     * be decompressed or serialized, as appropriate. (Inclusive or)<br/>
     * <br/>
     * Non-serialized data will be returned as a string, so explicit conversion to<br/>
     * numeric types will be necessary, if desired<br/>
     *
     * @param key      key where data is stored
     * @param hashCode if not null, then the int hashcode to use
     * @return the object that was previously stored, or null if it was not previously stored
     */
    public Object get(String key, Integer hashCode) {
        return get(key, hashCode, false, false);
    }

    /**
     * Retrieve a key from the server, using a specific hash.
     * <p/>
     * If the data was compressed or serialized when compressed, it will automatically<br/>
     * be decompressed or serialized, as appropriate. (Inclusive or)<br/>
     * <br/>
     * Non-serialized data will be returned as a string, so explicit conversion to<br/>
     * numeric types will be necessary, if desired<br/>
     *
     * @param key        key where data is stored
     * @param hashCode   if not null, then the int hashcode to use
     * @param asString   if true, then return string val
     * @param hasCascode if true, will use gets command and return Object array.
     * @return the object that was previously stored, or null if it was not previously stored
     */
    public Object get(String key, Integer hashCode, boolean asString, boolean hasCascode) {
        if (key == null) {
            log.error("key is null for get()");
            return null;
        }
        try {
            key = sanitizeKey(key);
        } catch (UnsupportedEncodingException e) {
            if (errorHandler != null) {
                errorHandler.handleErrorOnGet(this, e, key);
            }
            log.error("failed to sanitize your key!", e);
            return null;
        }
        if (pool == null) {
            pool = SockIOPool.getInstance(poolName);
        }
        SockIOPool.SockIO sock = pool.getSock(key, hashCode);
        if (sock == null) {
            if (errorHandler != null) {
                errorHandler.handleErrorOnGet(this, new IOException("no socket to server available"), key);
            }
            return null;
        }
        Map<String, StringBuilder> cmdMap = new HashMap<String, StringBuilder>();
        cmdMap.put(sock.getHost(), new StringBuilder(String.format((hasCascode ? "gets" : "get") + " %s", key)));
        sock.close();
        sock = null;
        Map<String, Object> hm = new HashMap<String, Object>();
        (new NIOLoader()).doMulti(asString, cmdMap, new String[] { key }, hm);
        Object ret = (hm.containsKey(key)) ? hm.get(key) : null;
        if (ret == null) return null;
        if (hasCascode) return ret;
        return ((GetsObject) ret).getObject();
    }

    /**
     * Retrieve multiple objects from the memcache.
     * <p/>
     * This is recommended over repeated calls to {@link #get(String) get()}, since it<br/>
     * is more efficient.<br/>
     *
     * @param keys String array of keys to retrieve
     * @return Object array ordered in same order as key array containing results
     */
    public Object[] getMultiArray(String[] keys) {
        return getMultiArray(keys, null, false);
    }

    /**
     * Retrieve multiple objects from the memcache.
     * <p/>
     * This is recommended over repeated calls to {@link #get(String) get()}, since it<br/>
     * is more efficient.<br/>
     *
     * @param keys      String array of keys to retrieve
     * @param hashCodes if not null, then the Integer array of hashCodes
     * @return Object array ordered in same order as key array containing results
     */
    public Object[] getMultiArray(String[] keys, Integer[] hashCodes) {
        return getMultiArray(keys, hashCodes, false);
    }

    /**
     * Retrieve multiple objects from the memcache.
     * <p/>
     * This is recommended over repeated calls to {@link #get(String) get()}, since it<br/>
     * is more efficient.<br/>
     *
     * @param keys      String array of keys to retrieve
     * @param hashCodes if not null, then the Integer array of hashCodes
     * @param asString  if true, retrieve string vals
     * @return Object array ordered in same order as key array containing results
     */
    public Object[] getMultiArray(String[] keys, Integer[] hashCodes, boolean asString) {
        Map<String, Object> data = getMulti(keys, hashCodes, asString);
        if (data == null) {
            return null;
        }
        Object[] res = new Object[keys.length];
        for (int i = 0; i < keys.length; i++) {
            res[i] = data.get(keys[i]);
        }
        return res;
    }

    /**
     * Retrieve multiple objects from the memcache.
     * <p/>
     * This is recommended over repeated calls to {@link #get(String) get()}, since it<br/>
     * is more efficient.<br/>
     *
     * @param keys String array of keys to retrieve
     * @return a hashmap with entries for each key is found by the server,
     *         keys that are not found are not entered into the hashmap, but attempting to
     *         retrieve them from the hashmap gives you null.
     */
    public Map<String, Object> getMulti(String[] keys) {
        return getMulti(keys, null, false);
    }

    /**
     * Retrieve multiple keys from the memcache.
     * <p/>
     * This is recommended over repeated calls to {@link #get(String) get()}, since it<br/>
     * is more efficient.<br/>
     *
     * @param keys      keys to retrieve
     * @param hashCodes if not null, then the Integer array of hashCodes
     * @return a hashmap with entries for each key is found by the server,
     *         keys that are not found are not entered into the hashmap, but attempting to
     *         retrieve them from the hashmap gives you null.
     */
    public Map<String, Object> getMulti(String[] keys, Integer[] hashCodes) {
        return getMulti(keys, hashCodes, false);
    }

    /**
     * Retrieve multiple keys from the memcache.
     * <p/>
     * This is recommended over repeated calls to {@link #get(String) get()}, since it<br/>
     * is more efficient.<br/>
     *
     * @param keys      keys to retrieve
     * @param hashCodes if not null, then the Integer array of hashCodes
     * @param asString  if true then retrieve using String val
     * @return a hashmap with entries for each key is found by the server,
     *         keys that are not found are not entered into the hashmap, but attempting to
     *         retrieve them from the hashmap gives you null.
     */
    public Map<String, Object> getMulti(String[] keys, Integer[] hashCodes, boolean asString) {
        if (keys == null || keys.length == 0) {
            log.error("missing keys for getMulti()");
            return null;
        }
        Map<String, StringBuilder> cmdMap = new HashMap<String, StringBuilder>();
        for (int i = 0; i < keys.length; ++i) {
            String key = keys[i];
            if (key == null) {
                log.error("null key, so skipping");
                continue;
            }
            Integer hash = null;
            if (hashCodes != null && hashCodes.length > i) {
                hash = hashCodes[i];
            }
            String cleanKey;
            try {
                cleanKey = sanitizeKey(key);
            } catch (UnsupportedEncodingException e) {
                if (errorHandler != null) {
                    errorHandler.handleErrorOnGet(this, e, key);
                }
                log.error("failed to sanitize your key!", e);
                continue;
            }
            if (pool == null) {
                pool = SockIOPool.getInstance(poolName);
            }
            SockIOPool.SockIO sock = pool.getSock(cleanKey, hash);
            if (sock == null) {
                if (errorHandler != null) {
                    errorHandler.handleErrorOnGet(this, new IOException("no socket to server available"), key);
                }
                continue;
            }
            if (!cmdMap.containsKey(sock.getHost())) {
                cmdMap.put(sock.getHost(), new StringBuilder("get"));
            }
            cmdMap.get(sock.getHost()).append(" ").append(cleanKey);
            sock.close();
        }
        if (log.isDebugEnabled()) {
            log.debug("multi get socket count : " + cmdMap.size());
        }
        Map<String, Object> ret = new HashMap<String, Object>(keys.length);
        (new NIOLoader()).doMulti(asString, cmdMap, keys, ret);
        for (String key : keys) {
            String cleanKey;
            try {
                cleanKey = sanitizeKey(key);
            } catch (UnsupportedEncodingException e) {
                if (errorHandler != null) {
                    errorHandler.handleErrorOnGet(this, e, key);
                }
                log.error("failed to sanitize your key!", e);
                continue;
            }
            if (!key.equals(cleanKey) && ret.containsKey(cleanKey)) {
                ret.put(key, ret.get(cleanKey));
                ret.remove(cleanKey);
            }
            if (!ret.containsKey(key)) {
                ret.put(key, null);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("++++ memcache: got back " + ret.size() + " results");
        }
        return ret;
    }

    /**
     * This method loads the data from cache into a Map.
     * <p/>
     * Pass a SockIO object which is ready to receive data and a HashMap<br/>
     * to store the results.
     *
     * @param input    socket waiting to pass back data
     * @param hm       hashmap to store data into
     * @param asString if true, and if we are using NativehHandler, return string val
     * @throws IOException if io exception happens while reading from socket
     */
    private void loadMulti(LineInputStream input, Map<String, Object> hm, boolean asString) throws IOException {
        while (true) {
            long cascode = 0;
            String line = input.readLine();
            if (log.isDebugEnabled()) {
                log.debug("++++ line: " + line);
            }
            if (line.startsWith(VALUE)) {
                String[] info = line.split(" ");
                String key = info[1];
                int flag = Integer.parseInt(info[2]);
                int length = Integer.parseInt(info[3]);
                if (info.length == 5) {
                    cascode = Integer.parseInt(info[4]);
                }
                if (log.isDebugEnabled()) {
                    log.debug("++++ key: " + key);
                    log.debug("++++ flags: " + flag);
                    log.debug("++++ length: " + length);
                    if (info.length == 5) log.debug("++++ cascode: " + cascode);
                }
                byte[] buf = new byte[length];
                input.read(buf);
                input.clearEOL();
                Object o;
                if ((flag & F_COMPRESSED) == F_COMPRESSED) {
                    try {
                        GZIPInputStream gzi = new GZIPInputStream(new ByteArrayInputStream(buf));
                        ByteArrayOutputStream bos = new ByteArrayOutputStream(buf.length);
                        int count;
                        byte[] tmp = new byte[2048];
                        while ((count = gzi.read(tmp)) != -1) {
                            bos.write(tmp, 0, count);
                        }
                        buf = bos.toByteArray();
                        gzi.close();
                    } catch (IOException e) {
                        if (errorHandler != null) {
                            errorHandler.handleErrorOnGet(this, e, key);
                        }
                        log.error("++++ IOException thrown while trying to uncompress input stream for key: " + key);
                        log.error(e.getMessage(), e);
                        throw new NestedIOException("++++ IOException thrown while trying to uncompress input stream for key: " + key, e);
                    }
                }
                if ((flag & F_SERIALIZED) != F_SERIALIZED) {
                    if (primitiveAsString || asString) {
                        if (log.isDebugEnabled()) {
                            log.debug("++++ retrieving object and stuffing into a string.");
                        }
                        o = new String(buf, defaultEncoding);
                    } else {
                        try {
                            o = NativeHandler.decode(buf, flag);
                        } catch (Exception e) {
                            if (errorHandler != null) {
                                errorHandler.handleErrorOnGet(this, e, key);
                            }
                            log.error("++++ Exception thrown while trying to deserialize for key: " + key, e);
                            throw new NestedIOException(e);
                        }
                    }
                } else {
                    ContextObjectInputStream ois = new ContextObjectInputStream(new ByteArrayInputStream(buf), classLoader);
                    try {
                        o = ois.readObject();
                        if (log.isDebugEnabled()) {
                            log.debug("++++ deserializing " + o.getClass());
                        }
                    } catch (ClassNotFoundException e) {
                        if (errorHandler != null) {
                            errorHandler.handleErrorOnGet(this, e, key);
                        }
                        log.error("++++ ClassNotFoundException thrown while trying to deserialize for key: " + key, e);
                        throw new NestedIOException("+++ failed while trying to deserialize for key: " + key, e);
                    }
                }
                hm.put(key, new GetsObject(cascode, o));
            } else if (END.equals(line)) {
                if (log.isDebugEnabled()) {
                    log.debug("++++ finished reading from cache server");
                }
                break;
            }
        }
    }

    private String sanitizeKey(String key) throws UnsupportedEncodingException {
        return (sanitizeKeys) ? URLEncoder.encode(key, "UTF-8") : key;
    }

    /**
     * Invalidates the entire cache.
     * <p/>
     * Will return true only if succeeds in clearing all servers.
     *
     * @return success true/false
     */
    public boolean flushAll() {
        return flushAll(null);
    }

    /**
     * Invalidates the entire cache.
     * <p/>
     * Will return true only if succeeds in clearing all servers.
     * If pass in null, then will try to flush all servers.
     *
     * @param servers optional array of host(s) to flush (host:port)
     * @return success true/false
     */
    public boolean flushAll(String[] servers) {
        if (pool == null) {
            pool = SockIOPool.getInstance(poolName);
        }
        if (pool == null) {
            log.error("++++ unable to get SockIOPool instance");
            return false;
        }
        servers = (servers == null) ? pool.getServers() : servers;
        if (servers == null || servers.length <= 0) {
            log.error("++++ no servers to flush");
            return false;
        }
        boolean success = true;
        for (String server : servers) {
            SockIOPool.SockIO sock = pool.getConnection(server);
            if (sock == null) {
                log.error("++++ unable to get connection to : " + server);
                success = false;
                if (errorHandler != null) {
                    errorHandler.handleErrorOnFlush(this, new IOException("no socket to server available"));
                }
                continue;
            }
            String command = "flush_all\r\n";
            try {
                sock.write(command.getBytes());
                sock.flush();
                String line = sock.readLine();
                success = (OK.equals(line)) && success;
            } catch (IOException e) {
                if (errorHandler != null) {
                    errorHandler.handleErrorOnFlush(this, e);
                }
                log.error("++++ exception thrown while writing bytes to server on flushAll");
                log.error(e.getMessage(), e);
                try {
                    sock.trueClose();
                } catch (IOException ioe) {
                    log.error("++++ failed to close socket : " + sock.toString());
                }
                success = false;
                sock = null;
            }
            if (sock != null) {
                sock.close();
                sock = null;
            }
        }
        return success;
    }

    /**
     * Retrieves stats for all servers.
     * <p/>
     * Returns a map keyed on the servername.
     * The value is another map which contains stats
     * with stat name as key and value as value.
     *
     * @return Stats map
     */
    public Map stats() {
        return stats(null);
    }

    /**
     * Retrieves stats for passed in servers (or all servers).
     * <p/>
     * Returns a map keyed on the servername.
     * The value is another map which contains stats
     * with stat name as key and value as value.
     *
     * @param servers string array of servers to retrieve stats from, or all if this is null
     * @return Stats map
     */
    public Map stats(String[] servers) {
        return stats(servers, "stats\r\n", STATS);
    }

    /**
     * Retrieves stats items for all servers.
     * <p/>
     * Returns a map keyed on the servername.
     * The value is another map which contains item stats
     * with itemname:number:field as key and value as value.
     *
     * @return Stats map
     */
    public Map statsItems() {
        return statsItems(null);
    }

    /**
     * Retrieves stats for passed in servers (or all servers).
     * <p/>
     * Returns a map keyed on the servername.
     * The value is another map which contains item stats
     * with itemname:number:field as key and value as value.
     *
     * @param servers string array of servers to retrieve stats from, or all if this is null
     * @return Stats map
     */
    public Map statsItems(String[] servers) {
        return stats(servers, "stats items\r\n", STATS);
    }

    /**
     * Retrieves stats items for all servers.
     * <p/>
     * Returns a map keyed on the servername.
     * The value is another map which contains slabs stats
     * with slabnumber:field as key and value as value.
     *
     * @return Stats map
     */
    public Map statsSlabs() {
        return statsSlabs(null);
    }

    /**
     * Retrieves stats for passed in servers (or all servers).
     * <p/>
     * Returns a map keyed on the servername.
     * The value is another map which contains slabs stats
     * with slabnumber:field as key and value as value.
     *
     * @param servers string array of servers to retrieve stats from, or all if this is null
     * @return Stats map
     */
    public Map statsSlabs(String[] servers) {
        return stats(servers, "stats slabs\r\n", STATS);
    }

    /**
     * Retrieves items cachedump for all servers.
     * <p/>
     * Returns a map keyed on the servername.
     * The value is another map which contains cachedump stats
     * with the cachekey as key and byte size and unix timestamp as value.
     *
     * @param slabNumber the item number of the cache dump
     * @param limit      int
     * @return Stats map
     */
    public Map statsCacheDump(int slabNumber, int limit) {
        return statsCacheDump(null, slabNumber, limit);
    }

    /**
     * Retrieves stats for passed in servers (or all servers).
     * <p/>
     * Returns a map keyed on the servername.
     * The value is another map which contains cachedump stats
     * with the cachekey as key and byte size and unix timestamp as value.
     *
     * @param servers    string array of servers to retrieve stats from, or all if this is null
     * @param slabNumber the item number of the cache dump
     * @param limit      int
     * @return Stats map
     */
    public Map statsCacheDump(String[] servers, int slabNumber, int limit) {
        return stats(servers, String.format("stats cachedump %d %d\r\n", slabNumber, limit), ITEM);
    }

    private Map stats(String[] servers, String command, String lineStart) {
        if (command == null || command.trim().equals("")) {
            log.error("++++ invalid / missing command for stats()");
            return null;
        }
        if (pool == null) {
            pool = SockIOPool.getInstance(poolName);
        }
        if (pool == null) {
            log.error("++++ unable to get SockIOPool instance");
            return null;
        }
        servers = (servers == null) ? pool.getServers() : servers;
        if (servers == null || servers.length <= 0) {
            log.error("++++ no servers to check stats");
            return null;
        }
        Map<String, Map> statsMaps = new HashMap<String, Map>();
        for (String server : servers) {
            SockIOPool.SockIO sock = pool.getConnection(server);
            if (sock == null) {
                log.error("++++ unable to get connection to : " + server);
                if (errorHandler != null) {
                    errorHandler.handleErrorOnStats(this, new IOException("no socket to server available"));
                }
                continue;
            }
            try {
                sock.write(command.getBytes());
                sock.flush();
                Map<String, String> stats = new HashMap<String, String>();
                while (true) {
                    String line = sock.readLine();
                    if (log.isDebugEnabled()) {
                        log.debug("++++ line: " + line);
                    }
                    if (line.startsWith(lineStart)) {
                        String[] info = line.split(" ", 3);
                        String key = info[1];
                        String value = info[2];
                        if (log.isDebugEnabled()) {
                            log.debug("++++ key  : " + key);
                            log.debug("++++ value: " + value);
                        }
                        stats.put(key, value);
                    } else if (END.equals(line)) {
                        if (log.isDebugEnabled()) {
                            log.debug("++++ finished reading from cache server");
                        }
                        break;
                    } else if (line.startsWith(ERROR) || line.startsWith(CLIENT_ERROR) || line.startsWith(SERVER_ERROR)) {
                        log.error("++++ failed to query stats");
                        log.error("++++ server response: " + line);
                        break;
                    }
                    statsMaps.put(server, stats);
                }
            } catch (IOException e) {
                if (errorHandler != null) {
                    errorHandler.handleErrorOnStats(this, e);
                }
                log.error("++++ exception thrown while writing bytes to server on stats");
                log.error(e.getMessage(), e);
                try {
                    sock.trueClose();
                } catch (IOException ioe) {
                    log.error("++++ failed to close socket : " + sock.toString());
                }
                sock = null;
            }
            if (sock != null) {
                sock.close();
                sock = null;
            }
        }
        return statsMaps;
    }

    @Override
    public String toString() {
        log.error("hi!");
        return "Client: " + super.toString();
    }

    protected final class NIOLoader {

        protected Selector selector;

        protected int numConns = 0;

        protected Connection[] conns;

        private final class Connection {

            public List<ByteBuffer> incoming = new ArrayList<ByteBuffer>();

            public ByteBuffer outgoing;

            public SockIOPool.SockIO sock;

            public SocketChannel channel;

            private boolean isDone = false;

            public Connection(SockIOPool.SockIO sock, StringBuilder request) throws IOException {
                if (log.isDebugEnabled()) {
                    log.debug("setting up connection to " + sock.getHost());
                }
                this.sock = sock;
                outgoing = ByteBuffer.wrap(request.append("\r\n").toString().getBytes());
                channel = sock.getChannel();
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_WRITE, this);
            }

            public void close() {
                try {
                    if (isDone) {
                        if (log.isDebugEnabled()) {
                            log.debug("++++ gracefully closing connection to " + sock.getHost());
                        }
                        channel.configureBlocking(true);
                        sock.close();
                        return;
                    }
                } catch (IOException e) {
                    log.warn("++++ memcache: unexpected error closing normally");
                }
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("forcefully closing connection to " + sock.getHost());
                    }
                    channel.close();
                    sock.trueClose();
                } catch (IOException ignoreMe) {
                }
            }

            public boolean isDone() {
                if (isDone) {
                    return true;
                }
                int maxBuf = incoming.size() - 1;
                int strPos = B_END.length - 1;
                int bi = maxBuf;
                while (bi >= 0 && strPos >= 0) {
                    ByteBuffer buf = incoming.get(bi);
                    int pos = buf.position() - 1;
                    while (pos >= 0 && strPos >= 0) {
                        if (buf.get(pos--) != B_END[strPos--]) {
                            return false;
                        }
                    }
                    bi--;
                }
                isDone = strPos < 0;
                return isDone;
            }

            public ByteBuffer getBuffer() {
                int last = incoming.size() - 1;
                if (last >= 0 && incoming.get(last).hasRemaining()) {
                    return incoming.get(last);
                } else {
                    ByteBuffer newBuf = ByteBuffer.allocate(8192);
                    incoming.add(newBuf);
                    return newBuf;
                }
            }

            public String toString() {
                return "Connection to " + sock.getHost() + " with " + incoming.size() + " bufs; done is " + isDone;
            }
        }

        public void doMulti(boolean asString, Map<String, StringBuilder> sockKeys, String[] keys, Map<String, Object> ret) {
            long timeRemaining = 0;
            try {
                selector = Selector.open();
                conns = new Connection[sockKeys.keySet().size()];
                numConns = 0;
                for (String host : sockKeys.keySet()) {
                    if (pool == null) {
                        pool = SockIOPool.getInstance(poolName);
                    }
                    SockIOPool.SockIO sock = pool.getConnection(host);
                    conns[numConns++] = new Connection(sock, sockKeys.get(host));
                }
                long startTime = System.currentTimeMillis();
                if (pool == null) {
                    pool = SockIOPool.getInstance(poolName);
                }
                long timeout = pool.getMaxBusy();
                timeRemaining = timeout;
                while (numConns > 0 && timeRemaining > 0) {
                    int n = selector.select(5000);
                    if (n > 0) {
                        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                        while (it.hasNext()) {
                            SelectionKey key = it.next();
                            it.remove();
                            handleKey(key);
                        }
                    } else {
                        log.error("selector timed out waiting for activity");
                    }
                    timeRemaining = timeout - (System.currentTimeMillis() - startTime);
                }
            } catch (IOException e) {
                handleError(e, keys);
                return;
            } finally {
                if (log.isDebugEnabled()) {
                    log.debug("Disconnecting; numConns=" + numConns + "  timeRemaining=" + timeRemaining);
                }
                try {
                    selector.close();
                } catch (IOException ignoreMe) {
                }
                for (Connection c : conns) {
                    if (c != null) {
                        c.close();
                    }
                }
            }
            for (Connection c : conns) {
                try {
                    if (c.incoming.size() > 0 && c.isDone()) {
                        loadMulti(new ByteBufArrayInputStream(c.incoming), ret, asString);
                    }
                } catch (Exception e) {
                    log.warn("Caught the aforementioned exception on " + c);
                }
            }
        }

        private void handleError(Throwable e, String[] keys) {
            if (errorHandler != null) {
                errorHandler.handleErrorOnGet(MemCachedClient.this, e, keys);
            }
            log.error("++++ exception thrown while getting from cache on getMulti");
            log.error(e.getMessage());
        }

        private void handleKey(SelectionKey key) throws IOException {
            if (log.isDebugEnabled()) {
                log.debug("handling selector op " + key.readyOps() + " for key " + key);
            }
            if (key.isReadable()) {
                readResponse(key);
            } else if (key.isWritable()) {
                writeRequest(key);
            }
        }

        public void writeRequest(SelectionKey key) throws IOException {
            ByteBuffer buf = ((Connection) key.attachment()).outgoing;
            SocketChannel sc = (SocketChannel) key.channel();
            if (buf.hasRemaining()) {
                if (log.isDebugEnabled()) {
                    log.debug("writing " + buf.remaining() + "B to " + ((SocketChannel) key.channel()).socket().getInetAddress());
                }
                sc.write(buf);
            }
            if (!buf.hasRemaining()) {
                if (log.isDebugEnabled()) {
                    log.debug("switching to read mode for server " + ((SocketChannel) key.channel()).socket().getInetAddress());
                }
                key.interestOps(SelectionKey.OP_READ);
            }
        }

        public void readResponse(SelectionKey key) throws IOException {
            Connection conn = (Connection) key.attachment();
            InetAddress remote = conn.channel.socket().getInetAddress();
            ByteBuffer buf = conn.getBuffer();
            int count = conn.channel.read(buf);
            if (count > 0) {
                if (log.isDebugEnabled()) {
                    log.debug("read  " + count + " from " + remote);
                }
                if (conn.isDone()) {
                    if (log.isDebugEnabled()) {
                        log.debug("connection done to  " + remote);
                    }
                    numConns--;
                }
            }
        }
    }
}
