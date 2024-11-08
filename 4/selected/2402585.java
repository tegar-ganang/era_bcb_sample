package org.h2.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class is responsible to read resources and generate the
 * ResourceData.java file from the resources.
 */
public class Resources {

    private static final HashMap<String, byte[]> FILES = New.hashMap();

    private Resources() {
    }

    static {
        loadFromZip();
    }

    private static void loadFromZip() {
        InputStream in = Resources.class.getResourceAsStream("data.zip");
        if (in == null) {
            return;
        }
        ZipInputStream zipIn = new ZipInputStream(in);
        try {
            while (true) {
                ZipEntry entry = zipIn.getNextEntry();
                if (entry == null) {
                    break;
                }
                String entryName = entry.getName();
                if (!entryName.startsWith("/")) {
                    entryName = "/" + entryName;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                IOUtils.copy(zipIn, out);
                zipIn.closeEntry();
                FILES.put(entryName, out.toByteArray());
            }
            zipIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get a resource from the resource map.
     *
     * @param name the name of the resource
     * @return the resource data
     */
    public static byte[] get(String name) throws IOException {
        byte[] data;
        if (FILES.size() == 0) {
            InputStream in = Resources.class.getResourceAsStream(name);
            if (in == null) {
                data = null;
            } else {
                data = IOUtils.readBytesAndClose(in, 0);
            }
        } else {
            data = FILES.get(name);
        }
        return data == null ? MemoryUtils.EMPTY_BYTES : data;
    }
}
