package com.aptana.ia.fileutils;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class Unzip {

    static final boolean DEBUG = false;

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    public static final void unzip(String filename) throws IOException {
        Enumeration entries;
        ZipFile zipFile = null;
        String baseDir = null;
        try {
            zipFile = new ZipFile(filename);
            baseDir = new File(filename).getParent();
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String path = baseDir + File.separator + entry.getName();
                if (entry.isDirectory()) {
                    if (DEBUG) System.err.println("Extracting directory: " + path);
                    (new File(path)).mkdir();
                    continue;
                }
                File parentDir = new File((new File(path)).getParent());
                if (parentDir.exists() == false) parentDir.mkdirs();
                if (DEBUG) System.err.println("Extracting file: " + path);
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(path)));
            }
            zipFile.close();
        } catch (IOException ioe) {
            if (zipFile != null) zipFile.close();
            throw new IOException(ioe.getMessage());
        }
        if (DEBUG) System.err.println("Done.");
    }
}
