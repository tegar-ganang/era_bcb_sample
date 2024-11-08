package com.beem.project.beem.smack.avatar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jivesoftware.smack.util.Cache;

/**
 * An avatar cache which store the avatars in memory.
 */
public class MemoryAvatarCache implements AvatarCache {

    private static final int BUFFER_SIZE = 1024;

    private Cache<String, byte[]> mCache;

    /**
	 * Create a MemoryAvatarCache.
	 * 
	 * @param maxSize
	 *            the maximum number of objects the cache will hold. -1 means the cache has no max size.
	 * @param maxlifetime
	 *            the maximum amount of time (in ms) objects can exist in cache before being deleted.
	 *            -1 means objects never expire.
	 */
    public MemoryAvatarCache(final int maxSize, final long maxlifetime) {
        mCache = new Cache<String, byte[]>(maxSize, maxlifetime);
    }

    @Override
    public void put(String key, byte[] data) throws IOException {
        mCache.put(key, data);
    }

    @Override
    public void put(String key, InputStream in) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            byte[] data = new byte[BUFFER_SIZE];
            int nbread;
            while ((nbread = in.read(data)) != -1) os.write(data, 0, nbread);
        } finally {
            in.close();
            os.close();
        }
        mCache.put(key, os.toByteArray());
    }

    @Override
    public byte[] get(String key) throws IOException {
        return mCache.get(key);
    }

    @Override
    public boolean contains(String key) {
        return mCache.containsKey(key);
    }
}
