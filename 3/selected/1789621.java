package vogar;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * Caches content by MD5.
 */
public final class Md5Cache {

    private final Log log;

    private final String keyPrefix;

    private final FileCache fileCache;

    /**
     * Creates a new cache accessor. There's only one directory on disk, so 'keyPrefix' is really
     * just a convenience for humans inspecting the cache.
     */
    public Md5Cache(Log log, String keyPrefix, FileCache fileCache) {
        this.log = log;
        this.keyPrefix = keyPrefix;
        this.fileCache = fileCache;
    }

    public boolean getFromCache(File output, String key) {
        if (fileCache.existsInCache(key)) {
            fileCache.copyFromCache(key, output);
            return true;
        }
        return false;
    }

    /**
     * Returns an ASCII hex representation of the MD5 of the content of 'file'.
     */
    private static String md5(File file) {
        byte[] digest = null;
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            byte[] bytes = new byte[8192];
            FileInputStream in = new FileInputStream(file);
            try {
                int byteCount;
                while ((byteCount = in.read(bytes)) > 0) {
                    digester.update(bytes, 0, byteCount);
                }
                digest = digester.digest();
            } finally {
                in.close();
            }
        } catch (Exception cause) {
            throw new RuntimeException("Unable to compute MD5 of \"" + file + "\"", cause);
        }
        return (digest == null) ? null : byteArrayToHexString(digest);
    }

    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(Integer.toHexString((b >> 4) & 0xf));
            result.append(Integer.toHexString(b & 0xf));
        }
        return result.toString();
    }

    /**
     * Returns the appropriate key for a dex file corresponding to the contents of 'classpath'.
     * Returns null if we don't think it's possible to cache the given classpath.
     */
    public String makeKey(Classpath classpath) {
        String key = keyPrefix;
        for (File element : classpath.getElements()) {
            if (!element.toString().endsWith(".jar")) {
                return null;
            }
            key += "-" + md5(element);
        }
        return key;
    }

    /**
     * Returns a key corresponding to the MD5ed contents of {@code file}.
     */
    public String makeKey(File file) {
        return keyPrefix + "-" + md5(file);
    }

    /**
     * Copy the file 'content' into the cache with the given 'key'.
     * This method assumes you're using the appropriate key for the content (and has no way to
     * check because the key is a function of the inputs that made the content, not the content
     * itself).
     * We accept a null so the caller doesn't have to pay attention to whether we think we can
     * cache the content or not.
     */
    public void insert(String key, File content) {
        if (key == null) {
            return;
        }
        log.verbose("inserting " + key);
        fileCache.copyToCache(content, key);
    }
}
