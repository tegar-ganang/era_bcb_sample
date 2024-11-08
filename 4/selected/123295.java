package com.makeabyte.jhosting.server.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Archiver {

    private Log log = LogFactory.getLog(Archiver.class);

    private final int BUFFER = 2048;

    /**
        * Extracts a zip or gzip archive
        * 
        * @param file The full path to the archive to extract
        */
    public void unzip(String resource) {
        File f = new File(resource);
        if (!f.exists()) throw new RuntimeException("The specified resources does not exist (" + resource + ")");
        String parent = f.getParent().toString();
        try {
            BufferedOutputStream dest = null;
            FileInputStream fis = new FileInputStream(resource);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                log.info("Extracting archive entry: " + entry);
                String entryPath = new StringBuilder(parent).append(System.getProperty("file.separator")).append(entry.getName()).toString();
                if (entry.isDirectory()) {
                    log.info("Creating directory: " + entryPath);
                    (new File(entryPath)).mkdir();
                    continue;
                }
                int count;
                byte data[] = new byte[BUFFER];
                FileOutputStream fos = new FileOutputStream(entryPath);
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER)) != -1) dest.write(data, 0, count);
                dest.flush();
                dest.close();
            }
            zis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
