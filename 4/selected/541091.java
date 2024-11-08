package hambo.messaging.hambo_db;

import java.io.*;
import java.net.UnknownServiceException;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.*;

class MessageDataSource implements DataSource {

    byte[] bytes;

    String contentType = "text/plain";

    MimePart part = null;

    public MessageDataSource(byte[] bytes, String contentType) {
        this.bytes = bytes;
        this.contentType = contentType;
    }

    public MessageDataSource(InputStream in, String contentType) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int readByte = 0;
            while ((readByte = in.read()) != -1) {
                out.write(readByte);
            }
            out.flush();
            this.bytes = out.toByteArray();
            this.contentType = contentType;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public MessageDataSource(MimePart part, String contentType) {
        try {
            this.part = part;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            part.writeTo(out);
            this.bytes = out.toByteArray();
            this.contentType = contentType;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public InputStream getInputStream() throws IOException {
        try {
            InputStream inputstream = new ByteArrayInputStream(bytes);
            String s = null;
            if (part != null) s = part.getEncoding();
            if (s != null) return MimeUtility.decode(inputstream, s); else return inputstream;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        InputStream inputstream = new ByteArrayInputStream(bytes);
        return inputstream;
    }

    public OutputStream getOutputStream() throws IOException {
        OutputStream outputStream = new ByteArrayOutputStream();
        OutputStream encOutputStream = null;
        try {
            if (part.getEncoding() == null) {
                encOutputStream = MimeUtility.encode(outputStream, "base64");
                return encOutputStream;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return outputStream;
    }

    public String getContentType() {
        return contentType;
    }

    public String getName() {
        return "";
    }
}
