package net.sf.xml2cb.test.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.apache.commons.codec.binary.Base64;

public class ResourceUtils {

    public static InputStream getResource(String classpath) throws IOException {
        InputStream in = ResourceUtils.class.getResourceAsStream(classpath);
        if (in == null) throw new IOException("Resource not found [" + classpath + "]");
        return in;
    }

    public static byte[] loadBase64Resource(String path) throws IOException {
        return Base64.decodeBase64(loadResource(path));
    }

    public static byte[] loadResource(String classpath) throws IOException {
        InputStream in = getResource(classpath);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
            }
        }
    }

    public static String convertToString(byte[] flat) throws UnsupportedEncodingException {
        if (flat[0] == '\0') return "";
        int pos = flat.length - 1;
        while (pos >= 0 && flat[pos] == '\0') pos--;
        return new String(flat, 0, pos + 1, "cp1147").replace('\0', '.');
    }
}
