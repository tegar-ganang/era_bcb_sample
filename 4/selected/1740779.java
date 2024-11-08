package cn.sduo.app.util.mail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.AbstractResource;

public class StreamAttachmentDataSource extends AbstractResource {

    private ByteArrayOutputStream outputStream;

    private String name;

    private String contentType;

    public StreamAttachmentDataSource(InputStream inputStream, String name, String contentType) throws IOException {
        this.outputStream = new ByteArrayOutputStream();
        this.name = name;
        this.contentType = contentType;
        int read;
        byte[] buffer = new byte[256];
        while ((read = inputStream.read(buffer)) != -1) {
            getOutputStream().write(buffer, 0, read);
        }
    }

    public StreamAttachmentDataSource(ByteArrayOutputStream outputStream, String name, String contentType) throws IOException {
        this.name = name;
        this.contentType = contentType;
        this.outputStream = outputStream;
    }

    public String getDescription() {
        return "Stream resource used for attachments";
    }

    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(this.outputStream.toByteArray());
    }

    public String getContentType() {
        return contentType;
    }

    public String getName() {
        return name;
    }

    public ByteArrayOutputStream getOutputStream() {
        return outputStream;
    }
}
