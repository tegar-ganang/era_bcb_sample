package org.translationcomponent.api.impl.translator.cache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.translationcomponent.api.Parameter;
import org.translationcomponent.api.ResponseCode;
import org.translationcomponent.api.ResponseHeader;
import org.translationcomponent.api.TranslationRequest;
import org.translationcomponent.api.TranslationResponse;
import org.translationcomponent.api.TranslationResponseFactory;
import org.translationcomponent.api.TranslatorService;
import org.translationcomponent.api.impl.parameter.ParameterImpl;
import org.translationcomponent.api.impl.request.OverwriteIfModifiedSinceRequest;
import org.translationcomponent.api.impl.response.ResponseHeaderImpl;
import org.translationcomponent.api.impl.response.ResponseStateBean;
import org.translationcomponent.api.impl.response.ResponseStateException;
import org.translationcomponent.api.impl.response.ResponseStateNotModified;
import org.translationcomponent.api.impl.response.ResponseStateOk;
import org.translationcomponent.api.impl.translator.cache.key.KeyGenerator;
import org.translationcomponent.api.impl.translator.cache.lock.LockDecorator;
import org.translationcomponent.api.impl.translator.cache.lock.LockMap;
import org.translationcomponent.api.impl.translator.cache.resource.ResourceChecker;
import org.translationcomponent.api.impl.translator.cache.resource.ResourceProperties;
import org.translationcomponent.api.impl.translator.cache.times.ModifyTimes;

/**
 * The Cache implements two mechanisms: 1. Modification time of the original
 * file is retrieved by a ResourceChecker. 2. Modification time of a page is
 * determined by the "Last-Modified" HTTP header returned when the original page
 * is retrieved.
 * 
 * TODO: Split this class up in two subclasses for the two mechanisms as they
 * are exclusive. Make another class that can automatically select the one
 * desired based on which one works on the server.
 * 
 * 
 * @author ROB
 * 
 */
public class PageCacheTranslatorService implements TranslatorService {

    protected final Log log = LogFactory.getLog(getClass());

    private static final Parameter NOCACHESIGNATURE = new ParameterImpl("notranslatecache");

    private static final String ETAG_PREFIX_DEFAULT = "";

    /**
	 * The translator to call to actually perform the translation. After this
	 * translator does its work it can pass the text or parts of the text to
	 * this translator
	 */
    private TranslatorService chainedTranslator;

    private TranslationResponseFactory responseFactory;

    private CacheAdapter cacheAdapter;

    private KeyGenerator keyGenerator;

    /**
	 * For a lock per key.
	 */
    private LockMap lockMap = new LockMap();

    /**
	 * To check the original file on the harddisk. Not needed when
	 * If-Modified-Since is used, but it can still be used and is faster.
	 */
    private ResourceChecker resourceChecker;

    /**
	 * Intrenal variable: new request have to wait if the cache is being
	 * deleted.
	 */
    private boolean blockNewRequests = false;

    /**
	 * If true then cache expires when the cache time != file time. If false
	 * then cache expires when it is older then the file (cache time < file
	 * time) So if the cache is newer because the user backuped and restored a
	 * file then the cache is not refreshed.
	 */
    private boolean strict = false;

    private String eTagPrefix = ETAG_PREFIX_DEFAULT;

    public PageCacheTranslatorService() {
        super();
    }

    public void service(final TranslationRequest request, final TranslationResponse response) {
        try {
            if (!cacheAdapter.isInitialized()) {
                synchronized (cacheAdapter) {
                    cacheAdapter.initialize(request, response, this);
                }
            }
            if (request.hasParameter(NOCACHESIGNATURE)) {
                request.removeParameter(NOCACHESIGNATURE);
                TranslationRequest wrappedRequest = new OverwriteIfModifiedSinceRequest(request, -1);
                chainedTranslator.service(wrappedRequest, response);
                return;
            }
            while (blockNewRequests) {
                Thread.sleep(300);
            }
            final ModifyTimes times = getModifyTimes(request);
            final String key = keyGenerator.getKey(request, response, cacheAdapter, times).intern();
            final LockDecorator rwl = lockMap.acquireLock(key);
            try {
                rwl.readLock().lock();
                try {
                    CacheItem entry = cacheAdapter.getOrCreateCacheEntry(key);
                    if (times.isFileLastModifiedKnown()) {
                        response.setHeaderValues(TranslationResponse.ETAG, new String[] { times.getFileETag() });
                        response.setLastModified(times.getFileLastModified());
                        if (entry.isCached() && !times.isFileModified()) {
                            response.setEndState(ResponseStateNotModified.getInstance());
                            return;
                        }
                    }
                    times.setCacheLastModified(entry.getLastModified());
                    if (times.isCacheExpired()) {
                        rwl.readLock().unlock();
                        rwl.writeLock().lock();
                        try {
                            entry = cacheAdapter.getOrCreateCacheEntry(key);
                            times.setCacheLastModified(entry.getLastModified());
                            if (times.isCacheExpired()) {
                                CacheItem currentEntry = entry;
                                entry = null;
                                entry = doForwardRequestAndCacheResponse(key, currentEntry, times, request, response);
                            }
                        } finally {
                            if (entry == null) {
                                rwl.readLock().lock();
                                rwl.writeLock().unlock();
                            } else if (entry.isCached()) {
                                rwl.readLock().lock();
                                rwl.writeLock().unlock();
                                this.writePage(entry, response, times);
                            } else {
                                this.writePage(entry, response, times);
                                cacheAdapter.removeCacheEntry(key, entry);
                                rwl.readLock().lock();
                                rwl.writeLock().unlock();
                            }
                        }
                    } else {
                        this.writePage(entry, response, times);
                    }
                } finally {
                    rwl.readLock().unlock();
                }
            } finally {
                lockMap.releaseLock(key, rwl);
            }
        } catch (Exception e) {
            response.setEndState(new ResponseStateException(e));
        } finally {
            if (!response.hasEnded()) {
                response.setEndState(new ResponseStateBean(ResponseCode.ERROR, "translation.cache.unknownerror"));
            }
        }
    }

    protected ModifyTimes getModifyTimes(final TranslationRequest request) {
        final ResourceProperties props = resourceChecker == null ? null : resourceChecker.getResourceProperties(request);
        final long ifModifiedSince = getIfModifiedSince(request);
        final String ifNoneMatch = getIfNoneMatch(request);
        if (props == null) {
            if (resourceChecker != null && resourceChecker.isEnabled()) {
                log.warn("Resource Checker is configured yet cannot find resource. ResourceChecker: " + resourceChecker + ", Resource path:" + resourceChecker.getRealPath(request) + ", URL: " + request.getFullUrlWithQueryString());
            }
            return new ModifyTimes(-1L, -1L, ifModifiedSince, ifNoneMatch, strict, eTagPrefix);
        }
        return new ModifyTimes(props.getLastModified(), props.getLength(), ifModifiedSince, ifNoneMatch, strict, eTagPrefix);
    }

    protected CacheItem doForwardRequestAndCacheResponse(final String key, final CacheItem entry, final ModifyTimes times, final TranslationRequest request, final TranslationResponse response) throws IOException {
        boolean useIfModifiedSince = !times.isFileLastModifiedKnown();
        boolean sendCacheIfModifiedSince = useIfModifiedSince && entry.isCached() && times.isUseCacheIfModifiedSince();
        TranslationRequest wrappedRequest = null;
        if (!useIfModifiedSince) {
            wrappedRequest = new OverwriteIfModifiedSinceRequest(request, -1L);
        } else if (sendCacheIfModifiedSince) {
            wrappedRequest = new OverwriteIfModifiedSinceRequest(request, times.getCacheLastModified());
        } else if (!entry.isCached()) {
            wrappedRequest = new OverwriteIfModifiedSinceRequest(request, -1L);
        } else {
            wrappedRequest = request;
        }
        TranslationResponse bufferResponse = entry.createResponse(response.getCharacterEncoding(), this.responseFactory);
        chainedTranslator.service(wrappedRequest, bufferResponse);
        switch(bufferResponse.getEndState().getCode()) {
            case OK:
                try {
                    entry.updateCacheEntry(bufferResponse, times);
                    if (!bufferResponse.isFail()) {
                        cacheAdapter.storeInCache(key, entry);
                    } else {
                        entry.setCached(false);
                    }
                    return entry;
                } catch (Exception e) {
                    log.warn(request.toString(), e);
                    cacheAdapter.removeCacheEntry(key, entry);
                    response.setEndState(new ResponseStateException(e));
                    return null;
                }
            case NOTMODIFIED:
                long responseLastModified = bufferResponse.getLastModified();
                if ((!useIfModifiedSince) && entry.isCached()) {
                    return entry;
                } else if (sendCacheIfModifiedSince && times.isBrowserCacheExpired(responseLastModified)) {
                    return entry;
                } else {
                    if (!times.isFileLastModifiedKnown()) {
                        long lastModified = bufferResponse.getLastModified();
                        if (lastModified == -1L) {
                            lastModified = entry.getLastModified();
                        }
                        if (lastModified != -1L) {
                            response.setLastModified(lastModified);
                        }
                        String[] eTagValues = bufferResponse.getHeaderValues(TranslationResponse.ETAG);
                        if (eTagValues != null) {
                            response.setHeaderValues(TranslationResponse.ETAG, doETagStripWeakMarker(eTagValues));
                        } else if (lastModified != -1L) {
                            response.setHeaderValues(TranslationResponse.ETAG, new String[] { this.eTagPrefix + lastModified });
                        }
                    }
                    response.setEndState(bufferResponse.getEndState());
                    return null;
                }
            case SECURITYERROR:
                response.setEndState(bufferResponse.getEndState());
                return null;
            case NOTFOUND:
                cacheAdapter.removeCacheEntry(key, entry);
                response.setEndState(bufferResponse.getEndState());
                return null;
            case ERROR:
                cacheAdapter.removeCacheEntry(key, entry);
                response.setEndState(bufferResponse.getEndState());
                return null;
            default:
                throw new IllegalStateException();
        }
    }

    protected void writePage(final CacheItem entry, final TranslationResponse response, ModifyTimes times) throws IOException {
        if (entry == null) {
            return;
        }
        Set<ResponseHeader> headers = new TreeSet<ResponseHeader>();
        for (ResponseHeader h : entry.getHeaders()) {
            if (TranslationResponse.ETAG.equals(h.getName())) {
                if (!times.isFileLastModifiedKnown()) {
                    headers.add(new ResponseHeaderImpl(h.getName(), doETagStripWeakMarker(h.getValues())));
                }
            } else {
                headers.add(h);
            }
        }
        response.addHeaders(headers);
        if (!times.isFileLastModifiedKnown()) {
            response.setLastModified(entry.getLastModified());
        }
        response.setTranslationCount(entry.getTranslationCount());
        response.setFailCount(entry.getFailCount());
        OutputStream output = response.getOutputStream();
        try {
            InputStream input = entry.getContentAsStream();
            try {
                IOUtils.copy(input, output);
            } finally {
                input.close();
            }
        } finally {
            response.setEndState(ResponseStateOk.getInstance());
        }
    }

    protected String[] doETagStripWeakMarker(String[] s) {
        for (int ii = 0; ii < s.length; ii++) {
            if (s[ii] != null && s[ii].contains("W/")) {
                log.warn("Page returns ETag header with value " + s[ii] + ". The W/ (Weak Tag) is not well supported by Internet Explorer.");
                s[ii] = s[ii].replace("W/", "").replace("\"", "");
            }
        }
        return s;
    }

    /**
	 * Get the If-Modified-Since header of the browser.
	 * 
	 * 
	 * 
	 * @param request
	 * @return The time or -1 when not passed.
	 */
    protected long getIfModifiedSince(final TranslationRequest request) {
        return request.getHeaderDateValue(TranslationRequest.IF_MODIFIED_SINCE);
    }

    protected String getIfNoneMatch(final TranslationRequest request) {
        return request.getHeaderValue(TranslationRequest.IF_NONE_MATCH);
    }

    public void setChainedTranslator(final TranslatorService chainedTranslator) {
        this.chainedTranslator = chainedTranslator;
    }

    public TranslatorService getChainedTranslator() {
        return chainedTranslator;
    }

    public TranslationResponseFactory getResponseFactory() {
        return responseFactory;
    }

    public void setResponseFactory(TranslationResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    public CacheAdapter getCacheAdapter() {
        return cacheAdapter;
    }

    public void setCacheAdapter(CacheAdapter cacheAdapter) {
        this.cacheAdapter = cacheAdapter;
    }

    public ResourceChecker getResourceChecker() {
        return resourceChecker;
    }

    public void setResourceChecker(ResourceChecker fileTimeChecker) {
        this.resourceChecker = fileTimeChecker;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public void clearCache(final String fromLanguage, final String toLanguage) throws IOException {
        blockNewRequests = true;
        try {
            while (lockMap.getSize() != 0) {
                Thread.sleep(300);
            }
            this.cacheAdapter.clearCache(fromLanguage, toLanguage);
        } catch (InterruptedException e) {
            throw new IOException(e.getMessage());
        } finally {
            blockNewRequests = false;
        }
    }

    public void setKeyGenerator(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    public String getETagPrefix() {
        return eTagPrefix;
    }

    public void setETagPrefix(String tagPrefix) {
        eTagPrefix = tagPrefix;
    }
}
