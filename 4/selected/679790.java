package netgest.utils.mail;

import java.io.*;
import javax.activation.*;
import netgest.bo.localizations.MessageLocalizer;

/**
 *
 * @author Francisco Câmara
 */
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
            this.data = data.getBytes("UTF-8");
        } catch (UnsupportedEncodingException uex) {
        }
        this.type = type;
    }

    /**
     * Return an InputStream for the data.
     * Note - a new stream must be returned each time.
     */
    public InputStream getInputStream() throws IOException {
        if (data == null) throw new IOException(MessageLocalizer.getMessage("NO_DATA"));
        return new ByteArrayInputStream(data);
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException(MessageLocalizer.getMessage("CANNOT_DO_THIS"));
    }

    public String getContentType() {
        return type;
    }

    public String getName() {
        return "dummy";
    }
}
