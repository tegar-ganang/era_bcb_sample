package org.translationcomponent.api.impl.translator.cache.disk;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.translationcomponent.api.ResponseHeader;
import org.translationcomponent.api.Storage;
import org.translationcomponent.api.TranslationResponse;
import org.translationcomponent.api.impl.response.ResponseHeaderImpl;
import org.translationcomponent.api.impl.response.storage.StorageFile;
import org.translationcomponent.api.impl.translator.cache.BaseItem;
import org.translationcomponent.api.impl.translator.cache.times.ModifyTimes;

public class DiskCacheItem extends BaseItem implements Serializable {

    private static final String PROPERTY_VALUE_DELIMITER = "],[";

    private static final Pattern PROPERTY_VALUE_DELIMITER_PATTERN = Pattern.compile("\\],\\[");

    private static final long serialVersionUID = -2343624658210166351L;

    private static final String PROPERTY_ENCODING = "encoding";

    private static final String PROPERTY_LASTMODIFIED = "lastmodified";

    private static final String PROPERTY_TRANSLATIONCOUNT = "translationcount";

    private static final String PROPERTY_OUTOFSERVICECOUNT = "outofservice";

    private static final String PROPERTY_HEADER = "header.";

    private final File htmlFile;

    public DiskCacheItem(final File htmlFile) {
        super();
        this.htmlFile = htmlFile;
    }

    public Properties getPersistenceProperties(boolean storeLastModified) {
        final Properties p = new Properties();
        p.setProperty(PROPERTY_ENCODING, this.getEncoding());
        p.setProperty(PROPERTY_TRANSLATIONCOUNT, Integer.toString(this.getTranslationCount()));
        p.setProperty(PROPERTY_OUTOFSERVICECOUNT, Integer.toString(this.getFailCount()));
        if (storeLastModified) {
            p.setProperty(PROPERTY_LASTMODIFIED, Long.toString(this.getLastModified()));
        }
        final Set<ResponseHeader> headers = this.getHeaders();
        if (headers != null) {
            final StringBuilder values = new StringBuilder(32);
            for (ResponseHeader header : headers) {
                if (!"Content-Length".equals(header.getName()) && !"Last-Modified".equals(header.getName())) {
                    values.setLength(0);
                    for (final String s : header.getValues()) {
                        if (values.length() != 0) {
                            values.append(PROPERTY_VALUE_DELIMITER);
                        }
                        values.append(s);
                    }
                    p.setProperty(PROPERTY_HEADER + header.getName(), values.toString());
                }
            }
        }
        return p;
    }

    public void setPersistenceProperties(final Properties p) {
        final Map<String, ResponseHeader> headers = new HashMap<String, ResponseHeader>();
        for (Map.Entry<Object, Object> entry : p.entrySet()) {
            if (PROPERTY_ENCODING.equals(entry.getKey())) {
                this.setEncoding((String) entry.getValue());
            } else if (PROPERTY_TRANSLATIONCOUNT.equals(entry.getKey())) {
                this.setTranslationCount(Integer.parseInt((String) entry.getValue()));
            } else if (PROPERTY_OUTOFSERVICECOUNT.equals(entry.getKey())) {
                this.setFailCount(Integer.parseInt((String) entry.getValue()));
            } else if (PROPERTY_LASTMODIFIED.equals(entry.getKey())) {
                this.setLastModified(Long.parseLong((String) entry.getValue()));
            } else if (((String) entry.getKey()).startsWith(PROPERTY_HEADER)) {
                final String name = ((String) entry.getKey()).substring(PROPERTY_HEADER.length());
                ResponseHeader header = headers.get(name);
                if (header == null) {
                    header = new ResponseHeaderImpl(name, PROPERTY_VALUE_DELIMITER_PATTERN.split((String) entry.getValue()));
                    headers.put(name, header);
                } else {
                    header.addValues(((String) entry.getValue()).split(PROPERTY_VALUE_DELIMITER));
                }
            }
        }
        for (ResponseHeader h : headers.values()) {
            this.setHeader(h);
        }
    }

    @Override
    public InputStream getContentAsStream() throws IOException {
        return new FileInputStream(htmlFile);
    }

    @Override
    public void setContentAsStream(InputStream input) throws IOException {
        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(htmlFile));
        try {
            IOUtils.copy(input, output);
        } finally {
            output.close();
        }
        if (this.getLastModified() != -1) {
            htmlFile.setLastModified(this.getLastModified());
        }
    }

    protected Storage createResponseStorage() {
        return new StorageFile(htmlFile, this.getEncoding());
    }

    public void updateAfterAllContentUpdated(final TranslationResponse response, final ModifyTimes times) {
        this.setHeader(new ResponseHeaderImpl("Content-Length", new String[] { Long.toString(this.htmlFile.length()) }));
    }
}
