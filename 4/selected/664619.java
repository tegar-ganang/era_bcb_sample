package net.sf.jabs.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.activation.DataSource;

public class ByteArrayDataSource implements DataSource {

    private byte[] data;

    private String type;

    public ByteArrayDataSource(InputStream is, String type) {
        this.type = type;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int ch;
            while ((ch = is.read()) != -1) os.write(ch);
            data = os.toByteArray();
        } catch (IOException ioex) {
        }
    }

    public ByteArrayDataSource(byte[] data, String type) {
        this.data = data;
        this.type = type;
    }

    public ByteArrayDataSource(String data, String type) {
        try {
            this.data = data.getBytes("iso-8859-1");
        } catch (UnsupportedEncodingException uex) {
        }
        this.type = type;
    }

    /**
    * Return an InputStream for the data.
    * Note - a new stream must be returned each time.
    */
    public InputStream getInputStream() throws IOException {
        if (data == null) throw new IOException("no data");
        return new ByteArrayInputStream(data);
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException("cannot do this");
    }

    public String getContentType() {
        return type;
    }

    public String getName() {
        return "dummy";
    }
}
