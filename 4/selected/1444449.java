package com.threerings.getdown.net;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.List;
import com.samskivert.io.StreamUtil;
import com.threerings.getdown.data.Resource;
import static com.threerings.getdown.Log.log;

/**
 * Implements downloading files over HTTP
 */
public class HTTPDownloader extends Downloader {

    public HTTPDownloader(List<Resource> resources, Observer obs) {
        super(resources, obs);
    }

    @Override
    protected long checkSize(Resource rsrc) throws IOException {
        URLConnection conn = rsrc.getRemote().openConnection();
        try {
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection hcon = (HttpURLConnection) conn;
                hcon.setRequestMethod("HEAD");
                hcon.connect();
                if (hcon.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Unable to check up-to-date for " + rsrc.getRemote() + ": " + hcon.getResponseCode());
                }
            }
            return conn.getContentLength();
        } finally {
            conn.getInputStream().close();
        }
    }

    @Override
    protected void doDownload(Resource rsrc) throws IOException {
        URLConnection conn = rsrc.getRemote().openConnection();
        conn.connect();
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection hcon = (HttpURLConnection) conn;
            if (hcon.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unable to download resource " + rsrc.getRemote() + ": " + hcon.getResponseCode());
            }
        }
        long actualSize = conn.getContentLength();
        log.info("Downloading resource", "url", rsrc.getRemote(), "size", actualSize);
        InputStream in = null;
        FileOutputStream out = null;
        long currentSize = 0L;
        try {
            in = conn.getInputStream();
            out = new FileOutputStream(rsrc.getLocal());
            int read;
            while ((read = in.read(_buffer)) != -1) {
                out.write(_buffer, 0, read);
                if (_obs == null) {
                    continue;
                }
                currentSize += read;
                updateObserver(rsrc, currentSize, actualSize);
            }
        } finally {
            StreamUtil.close(in);
            StreamUtil.close(out);
        }
    }
}
