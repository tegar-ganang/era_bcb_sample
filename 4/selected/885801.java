package org.middleheaven.process.web.client.apache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;
import org.middleheaven.io.IOUtils;
import org.middleheaven.process.web.HttpEntry;

/**
 * 
 */
public class HttpEntryEntityAdapter implements HttpEntity {

    private HttpEntry entry;

    public HttpEntryEntityAdapter(HttpEntry entry) {
        this.entry = entry;
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public boolean isRepeatable() {
        return entry.isRepeatable();
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public boolean isChunked() {
        return entry.isChunked();
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public long getContentLength() {
        return entry.getContent().getSize();
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public Header getContentType() {
        return new BasicHeader("Content-Type", entry.getContent().getContentType());
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public Header getContentEncoding() {
        return new BasicHeader("Content-Encoding", entry.getContentEncoding());
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
        return entry.getContent().getInputStream();
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        IOUtils.copy(entry.getContent().getInputStream(), outstream);
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public boolean isStreaming() {
        return entry.isStreaming();
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public void consumeContent() throws IOException {
    }
}
