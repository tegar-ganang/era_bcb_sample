package com.bonkey.filesystem.S3.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import com.bonkey.filesystem.browsable.BrowsableFileSystem;

public class S3StreamObject extends S3Object {

    protected InputStream source;

    protected int retries = 0;

    private IProgressMonitor monitor;

    private float incrementSize;

    public S3StreamObject(InputStream data, Map metadata, long size) {
        super(metadata, size);
        this.source = data;
    }

    public S3StreamObject(InputStream data, Map metadata, long size, IProgressMonitor monitor, float incrementSize) {
        this(data, metadata, size);
        this.source = data;
        this.monitor = monitor;
        this.incrementSize = incrementSize;
    }

    public void put(OutputStream destination) throws IOException {
        retries++;
        byte[] data = new byte[BrowsableFileSystem.DEFAULT_BUFFER_SIZE];
        int read = 0;
        long totalRead = 0;
        float stackedWork = 0;
        boolean cancelled = false;
        while (((read = source.read(data)) > 0) && !cancelled) {
            destination.write(data, 0, read);
            destination.flush();
            totalRead += read;
            if (monitor != null) {
                stackedWork += (read * incrementSize);
                if (stackedWork >= 1) {
                    monitor.worked((int) stackedWork);
                    stackedWork = stackedWork - ((int) stackedWork);
                }
                if (monitor.isCanceled()) {
                    cancelled = true;
                }
            }
        }
        setSize(totalRead);
        source.close();
        destination.close();
    }

    public InputStream getInputStream() {
        return source;
    }

    public int getRetries() {
        return retries;
    }
}
