package com.hongbo.cobweb.nmr.converter.stream;

import com.hongbo.cobweb.nmr.CobwebException;
import com.hongbo.cobweb.nmr.util.IOHelper;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileInputStreamCache extends InputStream implements StreamCache, Closeable {

    private InputStream stream;

    private File file;

    public FileInputStreamCache(File file) throws FileNotFoundException {
        this.file = file;
        this.stream = new FileInputStream(file);
    }

    @Override
    public void close() {
        if (isSteamOpened()) {
            IOHelper.close(getInputStream());
        }
    }

    @Override
    public void reset() {
        try {
            close();
            stream = new FileInputStream(file);
        } catch (Exception e) {
            throw new CobwebException("Cannot reset stream from file " + file, e);
        }
    }

    public void writeTo(OutputStream os) throws IOException {
        IOHelper.copy(getInputStream(), os);
    }

    @Override
    public int available() throws IOException {
        return getInputStream().available();
    }

    @Override
    public int read() throws IOException {
        return getInputStream().read();
    }

    protected InputStream getInputStream() {
        return stream;
    }

    private boolean isSteamOpened() {
        if (stream != null && stream instanceof FileInputStream) {
            return ((FileInputStream) stream).getChannel().isOpen();
        } else {
            return stream != null;
        }
    }
}
