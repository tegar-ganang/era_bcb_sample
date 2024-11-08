package org.translationcomponent.api.impl.translator.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
import org.translationcomponent.api.ResponseHeader;
import org.translationcomponent.api.Storage;
import org.translationcomponent.api.TranslationResponse;
import org.translationcomponent.api.TranslationResponseFactory;
import org.translationcomponent.api.impl.response.ResponseHeaderImpl;
import org.translationcomponent.api.impl.response.storage.StorageByteArray;
import org.translationcomponent.api.impl.translator.cache.times.ModifyTimes;

public class BaseItem implements CacheItem, Serializable {

    private static final long serialVersionUID = 8455672746257970480L;

    private transient Storage storage = null;

    private byte[] content;

    private String encoding;

    private Set<ResponseHeader> headers;

    private long lastModified = -1;

    private int translationCount = -1;

    private int failCount = -1;

    private boolean cached = false;

    public BaseItem() {
        super();
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    public Set<ResponseHeader> getHeaders() {
        return headers;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(final long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        if (isCached()) {
            return "cached entry";
        }
        return "new entry";
    }

    public InputStream getContentAsStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    public void setContentAsStream(final InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            IOUtils.copy(input, output);
        } finally {
            output.close();
        }
        this.content = output.toByteArray();
    }

    public boolean isEmpty() {
        return content == null;
    }

    public int getTranslationCount() {
        return translationCount;
    }

    public void setTranslationCount(int translationCount) {
        this.translationCount = translationCount;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public boolean isCached() {
        return cached;
    }

    public TranslationResponse createResponse(String encoding, final TranslationResponseFactory factory) {
        this.encoding = encoding;
        this.storage = createResponseStorage();
        return factory.createStorageResponse(storage, encoding);
    }

    protected Storage createResponseStorage() {
        return new StorageByteArray(new ByteArrayOutputStream(256 * 128), encoding);
    }

    public void updateAfterAllContentUpdated(final TranslationResponse response, final ModifyTimes times) {
        this.content = ((StorageByteArray) this.storage).toByteArray();
        setHeader(new ResponseHeaderImpl("Content-Length", new String[] { Long.toString(this.content.length) }));
    }

    public void setHeader(ResponseHeader header) {
        if (headers == null) {
            headers = new TreeSet<ResponseHeader>();
        }
        if (!headers.add(header)) {
            headers.remove(header);
            headers.add(header);
        }
    }

    public void updateCacheEntry(final TranslationResponse response, final ModifyTimes times) throws IOException {
        setTranslationCount(response.getTranslationCount());
        setFailCount(response.getFailCount());
        setEncoding(response.getCharacterEncoding());
        if (times.isFileLastModifiedKnown()) {
            setLastModified(times.getFileLastModified());
        } else {
            long lastModified = response.getLastModified();
            if (lastModified != -1) {
                setLastModified(times.round(lastModified));
            } else {
                setLastModified(times.now());
                LogFactory.getLog(this.getClass()).warn("The original page does not return a 'Last-Modified' HTTP header and also the ResourceChecker cannot determine the mod-date of the original file. Either configure the web server to return the 'Last-Modified' header or configure the right ResourceChecker. At the moment the cache does not use the cached pages. It might if the web server returns a 304 after a If-Modified-Since request. This is unlikely as the web server does not return a Last-Modified header.");
            }
        }
        for (ResponseHeader h : response.getHeaders()) {
            if (!TranslationResponse.LAST_MODIFIED.equals(h.getName())) {
                this.setHeader(h);
            }
        }
        updateAfterAllContentUpdated(response, times);
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int outOfServiceCount) {
        this.failCount = outOfServiceCount;
    }

    public ResponseHeader getHeader(String name) {
        for (ResponseHeader h : this.headers) {
            if (h.getName().equals(name)) {
                return h;
            }
        }
        return null;
    }
}
