package com.myJava.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import com.myJava.util.log.Logger;

public class FrameworkConfiguration {

    private static FrameworkConfiguration instance = new FrameworkConfiguration();

    /**
     * Number of iterations used during the key derivation process
     */
    public static String KEY_ENCRYPTION_KG_ITER = "encryption.keygen.iterations";

    /**
     * Static salt added during the key derivation process
     */
    public static String KEY_ENCRYPTION_KG_SALT = "encryption.keygen.salt";

    /**
     * Static salt encoding
     */
    public static String KEY_ENCRYPTION_KG_SALT_ENC = "encryption.keygen.salt.encoding";

    /**
     * Algorithm used during the key derivation process
     */
    public static String KEY_ENCRYPTION_KG_ALG = "encryption.keygen.algorithm";

    /**
     * Activate verbose mode for filediff tools
     */
    public static String KEY_DELTA_DEBUG = "delta.debug";

    /**
     * Maximum number of FTP connections on a remote server
     */
    public static String KEY_FTP_MAX_PROXIES = "ftp.max.proxies";

    /**
     * Activate FTP verbose mode
     */
    public static String KEY_FTP_DEBUG = "ftp.debug";

    /**
     * Tchnical delay - used by the filetool class
     */
    public static String KEY_FT_DELAY = "filetool.delay";

    /**
     * Buffer size for the filetool class
     */
    public static String KEY_FT_BUFFER_SIZE = "filetool.buffer.size";

    /**
     * Number of ms before Areca will send a "noop" instruction to the ftp server
     */
    public static String KEY_FTP_NOOP_DELAY = "ftp.noop.delay";

    /**
     * Local cache size for ftp files' data
     */
    public static String KEY_FTP_CACHE_SIZE = "ftp.cache.size";

    /**
     * Use (or not) a local cache for ftp files' data
     */
    public static String KEY_FTP_USE_CACHE = "ftp.use.cache";

    /**
     * Used by the HashFileSystemDriver class
     */
    public static String KEY_HASH_CACHE_SIZE = "hash.cache.size";

    /**
     * Used by the HashFileSystemDriver class
     */
    public static String KEY_HASH_USE_CACHE = "hash.use.cache";

    /**
     * Browser list (used to display the online help)
     */
    public static String KEY_OS_BROWSERS = "os.browsers";

    /**
     * Available SSE protocols
     */
    public static String KEY_SSE_PROTOCOLS = "sse.protocols";

    /**
     * Buffer size used for zip classes
     */
    public static String KEY_ZIP_BUFFER = "zip.buffer.size";

    /**
     * Number of digits for multivolume zip files
     */
    public static String KEY_ZIP_MV_DIGITS = "zip.mv.digits";

    /**
     * Log level (1=error; 8=finest)
     */
    public static String KEY_LOG_LEVEL = "log.level";

    /**
     * Always use a buffered for I/O
     */
    public static String KEY_FS_USE_BUFFER = "fs.use.buffer";

    /**
     * I/O buffer size
     */
    public static String KEY_FS_BUFFER_SIZE = "fs.buffer.size";

    /**
     * Verbose cache access
     */
    public static String KEY_FS_CACHE_DEBUG = "fs.cache.debug";

    /**
     * Activate or not zip CRC checks
     */
    public static String KEY_ZIP_ENTRY_CHECK_ENABLE = "zip.crc.enable";

    /**
     * Maximum file path length
     */
    public static String KEY_MAX_FILEPATH_LENGTH = "fs.max.filepath";

    /**
     * Tells whether file path length must be checked and explicit errors raised.
     * <BR>If value = -1 : Areca will check on windows and ignore on other operating systems
     * <BR>If value = 0 : Areca won't check, regardless to the operating system
     * <BR>If value = 1 : Areca will check, regardless to the operating system
     */
    public static String KEY_FORCE_FILEPATH_LENGTH_CHECK = "fs.max.filepath.check.force";

    /**
     * Number of days the log files are kept before being deleted
     */
    public static String KEY_DEFAULT_LOG_HISTORY = "log.default.history";

    /**
     * List of directories on which Areca is allowed to write.
     * <BR>If left empty, Areca will be able to write anywhere
     */
    public static String KEY_WRITABLE_DIRECTORIES = "fs.writable.directories";

    /**
     * Size of the buffer used by the "filediff" classes
     */
    public static String KEY_DELTA_LINKEDLIST_BUFFER_SIZE = "delta.linkedlist.buffer.size";

    /**
     * Size of the hashmap used by the "filediff" classes
     */
    public static String KEY_DELTA_HASHMAP_SIZE = "delta.hashmap.size";

    /**
     * Multiplier used by the "filediff" classes to product the quickHash value
     */
    public static String KEY_DELTA_QUICKHASH_MULTIPLIER = "delta.quickhash.multiplier";

    /**
     * Modulus used by the "filediff" classes to product the quickHash value
     */
    public static String KEY_DELTA_QUICKHASH_MODULUS = "delta.quickhash.modulus";

    /**
     * Filesystem accessor used to read/write file attributes (permissions, owner, group, ACL, extended attributes)
     * <BR>The current accessors are :
     * <BR>- com.myJava.file.metadata.windows.WindowsMetaDataAccessor on Windows
     * <BR>- com.myJava.file.metadata.posix.basic.DefaultMetaDataAccessor on Posix systems (only handles basic attributes, permissions, owner and group)
     * <BR>- com.myJava.file.metadata.posix.jni.JNIMetaDataAccessor : This advanced accessor uses JNI and native C code to access permissions, owner, group, ACL and extended attributes. It is only available for the systems the C code has been compiled for. Check Areca's website.
     */
    public static String KEY_FILESYSTEM_ACCESSOR = "filesystem.accessor.impl";

    /**
     * Algorithm that is used to generate file's hashcode
     */
    public static String KEY_FILE_CONTENT_HASH_ALGORITHM = "file.hash.algorithm";

    /**
     * Maximum number of cached mount points
     */
    public static String KEY_FS_MAX_MOUNT_POINTS = "fs.max.cached.mountpoints";

    /**
     * Temporary directory (defaults to the platform's standard temporary directory)
     */
    public static String KEY_TMP_DIRECTORY = "fs.tmp.directory";

    /**
     * Handler which is used to browse urls and open files
     */
    public static String KEY_VIEWER_HANDLER = "viewer.handler.impl";

    /**
     * Maximum number of messages that are kept in memory for the "ThreadLocal" log processor
     */
    public static String KEY_MAX_INLINE_LOG_MESSAGES = "threadlocal.log.max.messages";

    /**
     * Log level for the "ThreadLocal" log processor
     */
    public static String KEY_INLINE_LOG_LEVEL = "threadlocal.log.level";

    public static int DEF_ENCRYPTION_KG_ITER = 96731;

    public static String DEF_ENCRYPTION_KG_SALT = "ù%${{²]}}[|`è€$£^¤*!§:/..;;,,_?\"\\°à@@%µ";

    public static String DEF_ENCRYPTION_KG_SALT_ENC = "UTF-8";

    public static String DEF_ENCRYPTION_KG_ALG = "PBKDF2WithHmacSHA1";

    public static boolean DEF_DELTA_DEBUG = false;

    public static int DEF_FTP_MAX_PROXIES = 3;

    public static long DEF_FTP_NOOP_DELAY = 20000;

    public static boolean DEF_FTP_DEBUG = false;

    public static int DEF_FT_DELAY = 100;

    public static int DEF_FT_BUFFER_SIZE = 100000;

    public static int DEF_FTP_CACHE_SIZE = 300;

    public static boolean DEF_FTP_USE_CACHE = true;

    public static int DEF_HASH_CACHE_SIZE = 500;

    public static boolean DEF_HASH_USE_CACHE = true;

    public static String[] DEF_OS_BROWSERS = { "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };

    public static String[] DEF_SSE_PROTOCOLS = { "TLS", "SSL" };

    public static int DEF_ZIP_BUFFER = 2048;

    public static int DEF_ZIP_MV_DIGITS = 2;

    public static int DEF_LOG_LEVEL = 8;

    public static boolean DEF_FS_USE_BUFFER = true;

    public static int DEF_FS_BUFFER_SIZE = 200000;

    public static boolean DEF_FS_CACHE_DEBUG = false;

    public static boolean DEF_ZIP_ENTRY_CHECK_ENABLE = true;

    public static long DEF_MAX_FILEPATH_LENGTH = 256;

    public static int DEF_FORCE_FILEPATH_LENGTH_CHECK = -1;

    public static int DEF_DEFAULT_LOG_HISTORY = 10;

    public static String[] DEF_WRITABLE_DIRECTORIES = new String[] {};

    public static int DEF_DELTA_LINKEDLIST_BUFFER_SIZE = 200 * 1024;

    public static int DEF_DELTA_HASHMAP_SIZE = 10007;

    public static int DEF_DELTA_QUICKHASH_MULTIPLIER = 691 * 13 * 11;

    public static int DEF_DELTA_QUICKHASH_MODULUS = 4013423 * 17;

    public static String DEF_FILESYSTEM_ACCESSOR = "com.myJava.file.metadata.posix.jni.JNIMetaDataAccessor";

    public static String DEF_FILE_CONTENT_HASH_ALGORITHM = "SHA";

    public static int DEF_FS_MAX_MOUNT_POINTS = 2000;

    public static String DEF_TMP_DIRECTORY = null;

    public static String DEF_VIEWER_HANDLER = "com.myJava.system.viewer.DesktopViewerHandler";

    public static int DEF_MAX_INLINE_LOG_MESSAGES = 200;

    public static int DEF_INLINE_LOG_LEVEL = 3;

    private static String VM_PROPS_PREFIX = "launcher.d.";

    private String strUrl = null;

    private Properties props = new Properties();

    public static synchronized FrameworkConfiguration getInstance() {
        return instance;
    }

    public static synchronized void setInstance(FrameworkConfiguration i) {
        instance = i;
    }

    public FrameworkConfiguration() {
        init();
    }

    public FrameworkConfiguration(String url) {
        this.strUrl = url;
        init();
    }

    public Properties getProperties() {
        return props;
    }

    protected void init() {
        if (this.strUrl != null) {
            InputStream in = null;
            try {
                URL url = ClassLoader.getSystemClassLoader().getResource(strUrl);
                if (url != null) {
                    in = url.openStream();
                    if (in != null) {
                        props.load(in);
                    }
                }
            } catch (IOException e) {
                Logger.defaultLogger().error("Error during framework properties loading", e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    public int getInlineLogLevel() {
        return getProperty(KEY_INLINE_LOG_LEVEL, DEF_INLINE_LOG_LEVEL);
    }

    public int getMaxInlineLogMessages() {
        return getProperty(KEY_MAX_INLINE_LOG_MESSAGES, DEF_MAX_INLINE_LOG_MESSAGES);
    }

    public int getMaxCachedMountPoints() {
        return getProperty(KEY_FS_MAX_MOUNT_POINTS, DEF_FS_MAX_MOUNT_POINTS);
    }

    public int getEncryptionKGIters() {
        return getProperty(KEY_ENCRYPTION_KG_ITER, DEF_ENCRYPTION_KG_ITER);
    }

    public String getTemporaryDirectory() {
        return getProperty(KEY_TMP_DIRECTORY, (String) DEF_TMP_DIRECTORY);
    }

    public String getFileSystemAccessorImpl() {
        return getProperty(KEY_FILESYSTEM_ACCESSOR, DEF_FILESYSTEM_ACCESSOR);
    }

    public String getViewerHandlerImpl() {
        return getProperty(KEY_VIEWER_HANDLER, DEF_VIEWER_HANDLER);
    }

    public String getEncryptionKGAlg() {
        return getProperty(KEY_ENCRYPTION_KG_ALG, DEF_ENCRYPTION_KG_ALG);
    }

    public String getEncryptionKGSalt() {
        return getProperty(KEY_ENCRYPTION_KG_SALT, DEF_ENCRYPTION_KG_SALT);
    }

    public String getEncryptionKGSaltEncoding() {
        return getProperty(KEY_ENCRYPTION_KG_SALT_ENC, DEF_ENCRYPTION_KG_SALT_ENC);
    }

    public Map getJavaProperties() {
        return getPropertiesMap(VM_PROPS_PREFIX);
    }

    private Map getPropertiesMap(String prefix) {
        Enumeration enu = this.props.keys();
        HashMap map = new HashMap();
        while (enu.hasMoreElements()) {
            String key = (String) enu.nextElement();
            if (key.startsWith(prefix)) {
                map.put(key.substring(prefix.length()), props.get(key));
            }
        }
        return map;
    }

    public boolean isDeltaDebugMode() {
        return getProperty(KEY_DELTA_DEBUG, DEF_DELTA_DEBUG);
    }

    public String getFileHashAlgorithm() {
        return getProperty(KEY_FILE_CONTENT_HASH_ALGORITHM, DEF_FILE_CONTENT_HASH_ALGORITHM);
    }

    public int getFileToolDelay() {
        return getProperty(KEY_FT_DELAY, DEF_FT_DELAY);
    }

    public int getDeltaQuickHashModulus() {
        return getProperty(KEY_DELTA_QUICKHASH_MODULUS, DEF_DELTA_QUICKHASH_MODULUS);
    }

    public int getDeltaQuickHashMultiplier() {
        return getProperty(KEY_DELTA_QUICKHASH_MULTIPLIER, DEF_DELTA_QUICKHASH_MULTIPLIER);
    }

    public int getDeltaHashMapSize() {
        return getProperty(KEY_DELTA_HASHMAP_SIZE, DEF_DELTA_HASHMAP_SIZE);
    }

    public boolean isFTPDebugMode() {
        return getProperty(KEY_FTP_DEBUG, DEF_FTP_DEBUG);
    }

    public boolean isRemoteCacheMode() {
        return getProperty(KEY_FTP_USE_CACHE, DEF_FTP_USE_CACHE);
    }

    public boolean isFSCacheDebug() {
        return getProperty(KEY_FS_CACHE_DEBUG, DEF_FS_CACHE_DEBUG);
    }

    public long getMaxFilePath() {
        return getProperty(KEY_MAX_FILEPATH_LENGTH, DEF_MAX_FILEPATH_LENGTH);
    }

    public int getForceMaxFilePathCheck() {
        return getProperty(KEY_FORCE_FILEPATH_LENGTH_CHECK, DEF_FORCE_FILEPATH_LENGTH_CHECK);
    }

    public int getDefaultLogHistory() {
        return getProperty(KEY_DEFAULT_LOG_HISTORY, DEF_DEFAULT_LOG_HISTORY);
    }

    public boolean isHashCacheMode() {
        return getProperty(KEY_HASH_USE_CACHE, DEF_HASH_USE_CACHE);
    }

    public boolean isZipEntryCheckEnabled() {
        return getProperty(KEY_ZIP_ENTRY_CHECK_ENABLE, DEF_ZIP_ENTRY_CHECK_ENABLE);
    }

    public int getZipMvDigits() {
        return getProperty(KEY_ZIP_MV_DIGITS, DEF_ZIP_MV_DIGITS);
    }

    public int getHashCacheSize() {
        return getProperty(KEY_HASH_CACHE_SIZE, DEF_HASH_CACHE_SIZE);
    }

    public long getFTPNoopDelay() {
        return getProperty(KEY_FTP_NOOP_DELAY, DEF_FTP_NOOP_DELAY);
    }

    public int getFTPCacheSize() {
        return getProperty(KEY_FTP_CACHE_SIZE, DEF_FTP_CACHE_SIZE);
    }

    public int getLogLevel() {
        return getProperty(KEY_LOG_LEVEL, DEF_LOG_LEVEL);
    }

    public int getMaxFTPProxies() {
        return getProperty(KEY_FTP_MAX_PROXIES, DEF_FTP_MAX_PROXIES);
    }

    public int getFileToolBufferSize() {
        return getProperty(KEY_FT_BUFFER_SIZE, DEF_FT_BUFFER_SIZE);
    }

    public String[] getWritableDirectories() {
        return getProperty(KEY_WRITABLE_DIRECTORIES, DEF_WRITABLE_DIRECTORIES);
    }

    public String[] getOSBrowsers() {
        return getProperty(KEY_OS_BROWSERS, DEF_OS_BROWSERS);
    }

    public int getZipBufferSize() {
        return getProperty(KEY_ZIP_BUFFER, DEF_ZIP_BUFFER);
    }

    public boolean useFileSystemBuffer() {
        return getProperty(KEY_FS_USE_BUFFER, DEF_FS_USE_BUFFER);
    }

    public int getFileSystemBufferSize() {
        return getProperty(KEY_FS_BUFFER_SIZE, DEF_FS_BUFFER_SIZE);
    }

    public String[] getSSEProtocols() {
        return getProperty(KEY_SSE_PROTOCOLS, DEF_SSE_PROTOCOLS);
    }

    protected String getProperty(String key, String defaultValue) {
        String p = props.getProperty(key);
        if (p == null) {
            return defaultValue;
        } else {
            return p;
        }
    }

    public int getDeltaLinkedListBufferSize() {
        return getProperty(KEY_DELTA_LINKEDLIST_BUFFER_SIZE, DEF_DELTA_LINKEDLIST_BUFFER_SIZE);
    }

    protected boolean getProperty(String key, boolean defaultValue) {
        String p = props.getProperty(key);
        if (p == null) {
            return defaultValue;
        } else {
            return p.equalsIgnoreCase("true");
        }
    }

    protected int getProperty(String key, int defaultValue) {
        String p = props.getProperty(key);
        if (p == null) {
            return defaultValue;
        } else {
            return Integer.parseInt(p);
        }
    }

    protected long getProperty(String key, long defaultValue) {
        String p = props.getProperty(key);
        if (p == null) {
            return defaultValue;
        } else {
            return Long.parseLong(p);
        }
    }

    protected double getProperty(String key, double defaultValue) {
        String p = props.getProperty(key);
        if (p == null) {
            return defaultValue;
        } else {
            return Double.parseDouble(p);
        }
    }

    protected String[] getProperty(String key, String[] defaultValue) {
        String p = props.getProperty(key);
        if (p == null) {
            return defaultValue;
        } else {
            List data = new ArrayList();
            StringTokenizer stt = new StringTokenizer(p, ",");
            while (stt.hasMoreTokens()) {
                String t = stt.nextToken().trim();
                if (t.length() != 0) {
                    data.add(t);
                }
            }
            return (String[]) data.toArray(new String[0]);
        }
    }

    public static Properties getDefaults(Class cls) {
        Properties p = new Properties();
        Field[] fields = cls.getFields();
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            if (f.getName().startsWith("KEY_")) {
                String defName = "DEF_" + f.getName().substring(4);
                try {
                    Field def = cls.getField(defName);
                    String key = (String) f.get(cls);
                    Object value = def.get(cls);
                    String str = "";
                    if (value != null) {
                        if (value instanceof String[]) {
                            String[] data = (String[]) value;
                            for (int j = 0; j < data.length; j++) {
                                if (j != 0) {
                                    str += ", ";
                                }
                                str += data[j];
                            }
                        } else {
                            str = value.toString();
                        }
                    }
                    p.setProperty(key, str);
                } catch (Exception e) {
                    Logger.defaultLogger().error(e);
                }
            }
        }
        return p;
    }

    public Properties getAll() {
        Properties p = getDefaults(this.getClass());
        p.putAll(props);
        return p;
    }

    public String toFullString(Class cls) {
        Properties p = getDefaults(cls);
        p.putAll(props);
        return p.toString();
    }

    public String toString() {
        return this.props.toString();
    }
}
