package net.sf.cacheannotations.impl;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.sf.cacheannotations.AnnoCache;
import net.sf.cacheannotations.AnnoCacheManager;
import net.sf.cacheannotations.CacheException;
import net.sf.cacheannotations.CacheKeyGenerator;
import net.sf.cacheannotations.CacheSelector;
import net.sf.cacheannotations.Cached;
import net.sf.cacheannotations.TTLGenerator;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Logic for the AnnotationCachingAspect
 * 
 * 
 */
public class CachingAspectHelper<E> {

    private static Logger logger = Logger.getLogger(CachingAspectHelper.class);

    private boolean throwsOnCacheError = false;

    static ThreadLocal<Info> info = new ThreadLocal<Info>();

    private AnnoCacheManager<E> cacheManager;

    private Map<Class<? extends CacheKeyGenerator>, CacheKeyGenerator> keyGenerators = new HashMap<Class<? extends CacheKeyGenerator>, CacheKeyGenerator>();

    private Map<Class<? extends TTLGenerator>, TTLGenerator> ttlGenerators = new HashMap<Class<? extends TTLGenerator>, TTLGenerator>();

    private Map<Class<? extends CacheSelector>, CacheSelector> cacheSelectors = new HashMap<Class<? extends CacheSelector>, CacheSelector>();

    public void setCacheManager(AnnoCacheManager<E> cacheManager) {
        this.cacheManager = cacheManager;
    }

    public Object go(ProceedingJoinPoint jp, Cached cached) throws Throwable {
        Object result;
        long now = 0;
        if (logger.isDebugEnabled()) {
            now = System.nanoTime();
        }
        try {
            Method m = ((MethodSignature) jp.getSignature()).getMethod();
            Serializable key = getKeyGenerator(cached).generateKey(cached, jp.getArgs(), m, jp.getSignature().toLongString());
            int ttl = -777;
            boolean wasCached;
            String cacheName = getCacheSelector(cached).getCacheName(cached, jp.getArgs(), m, jp.getSignature().toLongString());
            AnnoCache<E> cache = null;
            if (cacheName == null) {
                cache = cacheManager.getCache("default");
            } else {
                cache = cacheManager.getCache(cacheName);
            }
            if (cache != null) {
                E element = cache.get(key);
                if (element == null) {
                    element = cache.generateElement(key, jp.proceed());
                    ttl = getTTLGenerator(cached).getTTL(cached, jp.getArgs(), m);
                    if (ttl >= 0) {
                        cache.setTtl(element, ttl);
                    }
                    cache.put(element);
                    wasCached = false;
                } else {
                    wasCached = true;
                }
                result = cache.getValue(element);
            } else {
                wasCached = true;
                throw new CacheException("no cache found with name " + cacheName);
            }
            if (info.get() != null && !info.get().set) {
                info.get().cacheKey = key;
                info.get().cacheKeyGenerator = getKeyGenerator(cached).getClass();
                info.get().cacheName = cacheName;
                info.get().fromFache = wasCached;
                info.get().ttl = ttl;
                info.get().ttlGenerator = getTTLGenerator(cached).getClass();
                info.get().cacheSelector = getCacheSelector(cached).getClass();
                info.get().nano = now - System.nanoTime();
            }
        } catch (CacheException e) {
            if (throwsOnCacheError) {
                throw new RuntimeException(e);
            } else {
                logger.warn("could not read or write from cache", e);
                if (info.get() != null && !info.get().set) {
                    info.get().ex = e;
                }
                result = jp.proceed();
            }
        } finally {
            if (info.get() != null) {
                info.get().set = true;
            }
        }
        if (logger.isDebugEnabled()) {
            long duration = System.nanoTime() - now;
            logger.debug(jp.toLongString() + " took " + (duration) + " nanoseconds");
        }
        return result;
    }

    private CacheSelector getCacheSelector(Cached cached) throws CacheException {
        CacheSelector cs;
        cs = cacheSelectors.get(cached.cacheSelector());
        if (cs == null) {
            try {
                cs = cached.cacheSelector().newInstance();
                cacheSelectors.put(cached.cacheSelector(), cs);
            } catch (Exception e) {
                throw new CacheException(e);
            }
        }
        return cs;
    }

    private TTLGenerator getTTLGenerator(Cached cached) throws CacheException {
        TTLGenerator ttlg;
        ttlg = ttlGenerators.get(cached.ttlGenerator());
        if (ttlg == null) {
            try {
                ttlg = cached.ttlGenerator().newInstance();
                ttlGenerators.put(cached.ttlGenerator(), ttlg);
            } catch (Exception e) {
                throw new CacheException(e);
            }
        }
        return ttlg;
    }

    private CacheKeyGenerator getKeyGenerator(Cached cached) throws CacheException {
        CacheKeyGenerator kg;
        kg = keyGenerators.get(cached.keyGenerator());
        if (kg == null) {
            try {
                kg = cached.keyGenerator().newInstance();
                keyGenerators.put(cached.keyGenerator(), kg);
            } catch (Exception e) {
                throw new CacheException(e);
            }
        }
        return kg;
    }

    public static Info getInfo() {
        return CachingAspectHelper.info.get();
    }

    public static void setInfo() {
        CachingAspectHelper.info.set(new Info());
    }
}
