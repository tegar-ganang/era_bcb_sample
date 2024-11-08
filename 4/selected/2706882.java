package com.luzan.common.nfs.s3;

import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.io.*;

/**
 * A Response object returned from AWSAuthConnection.get().  Exposes the attribute object, which
 * represents the retrieved object.
 */
public class GetResponse extends Response {

    public S3Object object;

    public InputStream in;

    public static final int READ_BUF_SIZE = 1024 * 1024;

    /**
     * Pulls a representation of an S3Object out of the HttpURLConnection response.
     */
    public GetResponse(HttpURLConnection connection) throws IOException {
        super(connection);
        init(connection, true, null);
    }

    public GetResponse(HttpURLConnection connection, boolean pull) throws IOException {
        super(connection);
        init(connection, pull, null);
    }

    public GetResponse(HttpURLConnection connection, OutputStream out) throws IOException {
        super(connection);
        init(connection, true, out);
    }

    protected void init(HttpURLConnection connection, boolean pull, OutputStream out) throws IOException {
        if (connection.getResponseCode() < 400) {
            Map metadata = extractMetadata(connection);
            in = connection.getInputStream();
            if (pull) if (out == null) {
                byte[] body = slurpInputStream(in);
                this.object = new S3Object(body, metadata);
            } else {
                byte[] buf = new byte[READ_BUF_SIZE];
                BufferedInputStream bin = new BufferedInputStream(in, READ_BUF_SIZE);
                BufferedOutputStream bout = new BufferedOutputStream(out, READ_BUF_SIZE);
                int count;
                while ((count = bin.read(buf)) != -1) bout.write(buf, 0, count);
                bout.flush();
            }
        }
    }

    /**
     * Examines the response's header fields and returns a Map from String to List of Strings
     * representing the object's metadata.
     */
    private Map extractMetadata(HttpURLConnection connection) {
        TreeMap metadata = new TreeMap();
        Map headers = connection.getHeaderFields();
        for (Iterator i = headers.keySet().iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            if (key == null) continue;
            if (key.startsWith(Utils.METADATA_PREFIX)) {
                metadata.put(key.substring(Utils.METADATA_PREFIX.length()), headers.get(key));
            }
        }
        return metadata;
    }

    /**
     * Read the input stream and dump it all into a big byte array
     */
    static byte[] slurpInputStream(InputStream is) throws IOException {
        final int chunkSize = 2048;
        byte[] buf = new byte[chunkSize];
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(chunkSize);
        int count;
        while ((count = is.read(buf)) != -1) byteStream.write(buf, 0, count);
        return byteStream.toByteArray();
    }
}
