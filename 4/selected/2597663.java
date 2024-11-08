package com.uspto.pati.Redbook;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.*;
import com.uspto.pati.PatiConstants;

public class UnzipRedbookFile {

    private static Logger LOG = Logger.getLogger("UnzipRedbookFile");

    private static long start, end, total;

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    public static void unzipFiles() {
        Enumeration entries;
        ZipFile zipFile;
        int dateCounter = 2010;
        try {
            LOG.setLevel(Level.FINE);
            start = System.currentTimeMillis();
            while (dateCounter < 2012) {
                String str = PatiConstants.REDBOOK_UNZIP_FOLDER + dateCounter + "\\";
                File inputDir = new File(str);
                File[] filesInInputDir = inputDir.listFiles();
                for (File f : filesInInputDir) {
                    if ((f.getName()).endsWith(".zip")) {
                        zipFile = new ZipFile(str + f.getName());
                        entries = zipFile.entries();
                        while (entries.hasMoreElements()) {
                            ZipEntry entry = (ZipEntry) entries.nextElement();
                            if (entry.isDirectory()) {
                                System.err.println("Extracting directory: " + entry.getName());
                                (new File(entry.getName())).mkdir();
                                continue;
                            }
                            System.err.println("Extracting file: " + entry.getName());
                            copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(str + entry.getName())));
                        }
                        zipFile.close();
                    }
                }
                dateCounter++;
            }
            end = System.currentTimeMillis();
            total = end - start;
            LOG.info("Time taken to unzip Redbook XML's is :" + total + "milliseconds");
        } catch (IOException ioe) {
            System.err.println("Unhandled exception:");
            ioe.printStackTrace();
            return;
        }
    }
}
