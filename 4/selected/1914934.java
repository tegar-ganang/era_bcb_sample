package org.apache.ws.commons.tcpmon.core.engine;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

public class GZIPCompressedEntity extends HttpEntityWrapper {

    public GZIPCompressedEntity(HttpEntity entity) {
        super(entity);
    }

    @Override
    public Header getContentEncoding() {
        return new BasicHeader(HTTP.CONTENT_ENCODING, "gzip");
    }

    @Override
    public long getContentLength() {
        return -1;
    }

    @Override
    public boolean isChunked() {
        return true;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        OutputStream compressed = new GZIPOutputStream(out);
        IOUtils.copy(wrappedEntity.getContent(), compressed);
        compressed.close();
    }
}
