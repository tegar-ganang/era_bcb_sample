package org.apache.axis.attachments;

import javax.activation.DataSource;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.net.URL;

public class SourceDataSource implements DataSource {

    public static final String CONTENT_TYPE = "text/xml";

    private final String name;

    private final String contentType;

    private byte[] data;

    private ByteArrayOutputStream os;

    public SourceDataSource(String name, StreamSource data) {
        this(name, CONTENT_TYPE, data);
    }

    public SourceDataSource(String name, String contentType, StreamSource data) {
        this.name = name;
        this.contentType = contentType == null ? CONTENT_TYPE : contentType;
        os = new ByteArrayOutputStream();
        try {
            if (data != null) {
                Reader reader = data.getReader();
                if (reader != null) {
                    reader = new BufferedReader(reader);
                    int ch;
                    while ((ch = reader.read()) != -1) {
                        os.write(ch);
                    }
                } else {
                    InputStream is = data.getInputStream();
                    if (is == null) {
                        String id = data.getSystemId();
                        if (id != null) {
                            URL url = new URL(id);
                            is = url.openStream();
                        }
                    }
                    if (is != null) {
                        is = new BufferedInputStream(is);
                        byte[] bytes = null;
                        int avail;
                        while ((avail = is.available()) > 0) {
                            if (bytes == null || avail > bytes.length) bytes = new byte[avail];
                            is.read(bytes, 0, avail);
                            os.write(bytes, 0, avail);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }

    public InputStream getInputStream() throws IOException {
        if (os.size() != 0) {
            data = os.toByteArray();
            os.reset();
        }
        return new ByteArrayInputStream(data == null ? new byte[0] : data);
    }

    public OutputStream getOutputStream() throws IOException {
        if (os.size() != 0) {
            data = os.toByteArray();
            os.reset();
        }
        return os;
    }
}
