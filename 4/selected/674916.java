package com.litt.core.mail;

import java.io.*;
import javax.activation.*;

public class ByteArrayDataSource implements DataSource {

    /** * Data to write. */
    private byte[] _data;

    /** * Content-Type. */
    private String _type;

    public ByteArrayDataSource(InputStream is, String type) {
        _type = type;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int ch;
            while ((ch = is.read()) != -1) os.write(ch);
            _data = os.toByteArray();
        } catch (IOException ioe) {
        }
    }

    public ByteArrayDataSource(byte[] data, String type) {
        _data = data;
        _type = type;
    }

    public ByteArrayDataSource(String data, String type) {
        try {
            _data = data.getBytes("iso-8859-1");
        } catch (UnsupportedEncodingException uee) {
        }
        _type = type;
    }

    public InputStream getInputStream() throws IOException {
        if (_data == null) throw new IOException("no data");
        return new ByteArrayInputStream(_data);
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException("cannot do this");
    }

    public String getContentType() {
        return _type;
    }

    public String getName() {
        return "dummy";
    }
}
