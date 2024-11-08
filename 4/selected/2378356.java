package net.pepperbytes.plaf.common;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.logging.LogFactory;

public class ResourceLoader {

    private static final int COPY_BUFFER_SIZE = 1048576;

    public static InputStream openResource(String namePath) throws IOException {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(namePath);
        return resource != null ? resource.openStream() : null;
    }

    /**
	 * 
	 * @param src the name of the source file in one of your jars distributed via Java WebStart
	 * @param dest the destination on the local file system
	 * @throws IOException 
	 */
    public static void copyResource(String src, File dest) throws IOException {
        InputStream fin = ResourceLoader.openResource(src);
        if (fin == null) {
            LogFactory.getLog(ResourceLoader.class).error("Failed to load resource '" + src + "'");
            LogFactory.getLog(ResourceLoader.class).info("Attempting to open file from from file system");
            fin = new FileInputStream(src);
        }
        if (fin == null) {
            LogFactory.getLog(ResourceLoader.class).warn("Giving up on copy operation, can't find source '" + src + "'");
            return;
        }
        BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(dest));
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        int readBytes = fin.read(buffer);
        while (readBytes > -1) {
            fout.write(buffer, 0, readBytes);
            readBytes = fin.read(buffer);
        }
        fout.close();
        fin.close();
    }

    public static void copyResource(InputStream fin, File dest) throws IOException {
        if (fin == null) {
            LogFactory.getLog(ResourceLoader.class).error("Can't read from NULL InputStream brew!");
            return;
        }
        BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(dest));
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        int readBytes = fin.read(buffer);
        while (readBytes > -1) {
            fout.write(buffer, 0, readBytes);
            readBytes = fin.read(buffer);
        }
        fout.close();
        fin.close();
    }
}
