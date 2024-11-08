package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author barkholt
 * 
 */
@SuppressWarnings("serial")
public class ZipHash extends HashMap<String, byte[]> {

    private static final int BUFFER_SIZE = 1024;

    public static ZipHash from(InputStream stream) throws IOException {
        ZipHash zipHash = new ZipHash();
        ZipInputStream zipInputStream = new ZipInputStream(stream);
        for (ZipEntry next = zipInputStream.getNextEntry(); next != null; next = zipInputStream.getNextEntry()) {
            if (!next.isDirectory()) {
                byte buffer[] = new byte[BUFFER_SIZE];
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                int read;
                while ((read = zipInputStream.read(buffer)) != -1) {
                    data.write(buffer, 0, read);
                }
                zipHash.put(next.getName(), data.toByteArray());
            }
        }
        zipInputStream.close();
        return zipHash;
    }
}
