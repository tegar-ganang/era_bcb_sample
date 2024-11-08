package org.translationcomponent.api.impl.response.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import org.apache.commons.io.IOUtils;
import org.translationcomponent.api.ResponseState;
import org.translationcomponent.api.Storage;

public class StorageFile implements Storage {

    private final File f;

    private final String encoding;

    private OutputStream output = null;

    private Writer writer = null;

    private boolean closed = false;

    public StorageFile(final File f, String encoding) {
        super();
        this.f = f;
        this.encoding = encoding;
    }

    public void addText(final String text) throws IOException {
        if (writer != null) {
            writer.write(text);
        } else if (output != null) {
            output.write(text.getBytes(encoding));
        } else {
            getOutputStream().write(text.getBytes(encoding));
        }
    }

    public void close(final ResponseState state) throws IOException {
        closed = true;
        IOUtils.closeQuietly(writer);
        writer = null;
        IOUtils.closeQuietly(output);
        output = null;
    }

    public OutputStream getOutputStream() throws IOException {
        if (closed) {
            throw new IOException("Is closed");
        }
        if (output == null) {
            output = new BufferedOutputStream(new FileOutputStream(f));
        }
        return output;
    }

    public String getText() throws IOException {
        InputStreamReader r = new InputStreamReader(getInputStream(), encoding);
        StringWriter w = new StringWriter(256 * 128);
        try {
            IOUtils.copy(r, w);
        } finally {
            IOUtils.closeQuietly(w);
            IOUtils.closeQuietly(r);
        }
        return w.toString();
    }

    public InputStream getInputStream() throws IOException {
        if (output != null) {
            this.close(null);
        }
        return new BufferedInputStream(new FileInputStream(f));
    }

    public String toString() {
        return f.getAbsolutePath();
    }

    public Writer getWriter() throws IOException {
        if (writer == null) {
            writer = new OutputStreamWriter(getOutputStream(), encoding);
        }
        return writer;
    }
}
