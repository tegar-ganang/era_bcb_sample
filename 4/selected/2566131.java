package eu.planets_project.pp.plato.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.logging.Log;

public class FileUtils implements Serializable {

    private static final long serialVersionUID = -2554713564317100326L;

    static final Log log = PlatoLogger.getLogger(FileUtils.class);

    public static File getResourceFile(String name) throws URISyntaxException {
        URI uri = Thread.currentThread().getContextClassLoader().getResource(name).toURI();
        return new File(uri);
    }

    public static byte[] inputStreamToBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
        return out.toByteArray();
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        byte[] bytes;
        try {
            long length = file.length();
            if (length > Integer.MAX_VALUE) {
            }
            bytes = new byte[(int) length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }
        } finally {
            is.close();
        }
        return bytes;
    }

    public static String replaceExtension(String filename, String newExtension) {
        return filename.substring(0, filename.lastIndexOf(".")) + "." + newExtension;
    }

    public static String makeFilename(String value) {
        if (value == null) return "";
        return value.replaceAll("\\s", "_").replaceAll("[^\\w-]", "");
    }

    public static void writeToFile(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (FileNotFoundException ex) {
            log.debug("Error copying file " + ex.getMessage(), ex);
        } finally {
            try {
                in.close();
            } catch (Exception skip) {
            }
            try {
                out.close();
            } catch (Exception skip) {
            }
        }
    }
}
