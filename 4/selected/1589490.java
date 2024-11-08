package net.sf.mustang.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.springframework.web.multipart.MultipartFile;
import sun.net.www.protocol.http.HttpURLConnection;

public class URLMultipartFile implements MultipartFile {

    private HttpURLConnection connection;

    public URLMultipartFile(URL url) throws IOException {
        connection = new HttpURLConnection(url, null);
    }

    public URLMultipartFile(String url) throws MalformedURLException, IOException {
        this(new URL(url));
    }

    public String getName() {
        return connection.getURL().toExternalForm();
    }

    public boolean isEmpty() {
        return getSize() == 0;
    }

    public String getOriginalFilename() {
        return connection.getURL().toExternalForm();
    }

    public String getContentType() {
        return connection.getContentType();
    }

    public long getSize() {
        return connection.getContentLength();
    }

    public byte[] getBytes() throws IOException {
        throw new IOException();
    }

    public InputStream getInputStream() throws IOException {
        return connection.getInputStream();
    }

    public void transferTo(File file) throws IOException, IllegalStateException {
        OutputStream out = new FileOutputStream(file);
        IOUtils.copy(getInputStream(), out);
        out.close();
    }
}
