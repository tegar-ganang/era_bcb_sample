package net.sf.syncopate.proxy.resources;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import org.apache.jmeter.samplers.SampleResult;
import net.sf.syncopate.proxy.util.IOUtils;

public abstract class ResourceResult extends SampleResult {

    private static final String RES_PKG = "net/sf/syncopate/proxy/resources/";

    private static final ClassLoader LOADER = ResourceResult.class.getClassLoader();

    static class ClasspathResourceResult extends ResourceResult {

        ClasspathResourceResult(String filename) throws IOException {
            super(filename);
        }

        @Override
        protected void copyContent(String filename) throws IOException {
            InputStream in = null;
            try {
                in = LOADER.getResourceAsStream(RES_PKG + filename);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                IOUtils.copy(in, out);
                setResponseData(out.toByteArray());
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
    }

    static class FileResourceResult extends ResourceResult {

        FileResourceResult(String filename) throws IOException {
            super(filename);
        }

        @Override
        protected void copyContent(String filename) throws IOException {
            InputStream in = null;
            try {
                String resourceDir = System.getProperty("resourceDir");
                File resource = new File(resourceDir, filename);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                if (resource.exists()) {
                    in = new FileInputStream(resource);
                } else {
                    in = LOADER.getResourceAsStream(RES_PKG + filename);
                }
                IOUtils.copy(in, out);
                setResponseData(out.toByteArray());
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
    }

    enum FileType {

        css("text/css"), js("text/javascript"), html("text/html"), jar("application/java-archive");

        private String contentType;

        FileType(String contentType) {
            this.contentType = contentType;
        }
    }

    protected ResourceResult(String filename) throws IOException {
        String[] ext = filename.split("\\.");
        FileType type = FileType.valueOf(ext[1]);
        setContentType(type.contentType);
        copyContent(filename);
        StringBuilder headers = new StringBuilder("HTTP/1.1 200 OK\n");
        headers.append("Connection: Keep-Alive\n");
        headers.append("Accept-Ranges: bytes\n");
        headers.append("Keep-Alive: timeout=5, max=100\nContent-Type: ");
        headers.append(type.contentType).append("\nContent-Length: ");
        headers.append(this.getResponseData().length).append('\n');
        setResponseHeaders(headers.toString());
    }

    protected abstract void copyContent(String filename) throws IOException;
}
