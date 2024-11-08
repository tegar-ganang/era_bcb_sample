package com.eip.yost.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.commons.io.IOUtils;

/**
 * @author abrisard
 */
public final class ZipUtils {

    private static ZipUtils sInstance = null;

    /**
	 * Private constructor
	 */
    private ZipUtils() {
    }

    /**
     * This method allow to access to the object instance
     * @return sInstance Singleton.
     */
    public static synchronized ZipUtils getInstance() {
        if (sInstance == null) {
            sInstance = new ZipUtils();
        }
        return sInstance;
    }

    public void unzipArchive(File archive, File outputDir) throws ZipException, IOException {
        ZipFile zipfile = new ZipFile(archive);
        for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            unzipEntry(zipfile, entry, outputDir);
        }
    }

    private void unzipEntry(ZipFile zipfile, ZipEntry entry, File outputDir) throws IOException {
        if (entry.isDirectory()) {
            createDir(new File(outputDir, entry.getName()));
            return;
        }
        File outputFile = new File(outputDir, entry.getName());
        if (!outputFile.getParentFile().exists()) {
            createDir(outputFile.getParentFile());
        }
        BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
        IOUtils.copy(inputStream, outputStream);
        outputStream.close();
        inputStream.close();
    }

    private void createDir(File dir) {
        dir.mkdirs();
    }
}
