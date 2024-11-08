package rachel.http.resource;

import java.io.*;
import houston.*;
import caramel.util.*;
import rachel.http.*;
import rachel.http.spi.*;

public class InputStreamResource implements WebResource {

    static Logger T = Logger.getLogger(InputStreamResource.class);

    String _name;

    String _contentType;

    long _contentLength;

    public String getName() {
        return _name;
    }

    public long getContentLength() {
        return _contentLength;
    }

    public String getContentType() {
        return _contentType;
    }

    public String getResponseMessage() {
        return "OK";
    }

    public int getResponseCode() {
        return 200;
    }

    byte _content[];

    public byte[] getContent() {
        return _content;
    }

    public InputStreamResource(String name, InputStream in) throws IOException {
        _name = name;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte buffer[] = new byte[2048];
        int bytes_read = 0;
        while ((bytes_read = in.read(buffer)) > 0) out.write(buffer, 0, bytes_read);
        out.flush();
        out.close();
        in.close();
        _content = out.toByteArray();
        _contentLength = _content.length;
        T.debug("contentLength=" + _contentLength);
        _contentType = MimeUtils.guessContentTypeFromName(name);
    }

    public String getHeader() {
        StringBuffer header = new StringBuffer();
        header.append("HTTP/1.0 200 OK\r\n");
        header.append(Http.Header.DATE + ": " + DateUtils.getHttpDate() + "\r\n");
        header.append(Http.Header.SERVER + ": " + Http.SERVER_ID + "\r\n");
        header.append(Http.Header.CONTENT_LENGTH + ": " + _contentLength + "\r\n");
        header.append(Http.Header.CONTENT_TYPE + ": " + _contentType + "\r\n");
        header.append("\r\n");
        T.debug("Content-length: " + _contentLength);
        T.debug("Content-type: " + _contentType);
        return header.toString();
    }
}
