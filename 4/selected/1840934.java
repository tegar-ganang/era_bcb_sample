package mecca.lcms;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class Unzip {

    public static void main(String args[]) {
        String zipFilename = args[0];
        if (args.length == 2) unzipFiles(zipFilename, args[1]); else unzipFiles(zipFilename, "");
    }

    public static void unzipFiles(String zipFilename) {
        unzipFiles(zipFilename, "");
    }

    public static void unzipFiles(String zipFilename, String folder) {
        ZipFile zip = null;
        try {
            System.out.println(zipFilename + ", " + folder);
            zipFilename = zipFilename.replace('/', java.io.File.separatorChar);
            zip = new ZipFile(zipFilename);
            byte[] buffer = new byte[16384];
            for (Enumeration entries = zip.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (!entry.isDirectory()) {
                    String filename = entry.getName();
                    filename = filename.replace('/', java.io.File.separatorChar);
                    filename = !"".equals(folder) ? folder + java.io.File.separatorChar + filename : filename;
                    java.io.File destFile = new java.io.File(filename);
                    String parent = destFile.getParent();
                    if (parent != null) {
                        java.io.File parentFile = new java.io.File(parent);
                        if (!parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                    }
                    InputStream in = zip.getInputStream(entry);
                    OutputStream outs = new FileOutputStream(filename);
                    int count;
                    while ((count = in.read(buffer)) != -1) outs.write(buffer, 0, count);
                    in.close();
                    outs.close();
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (zip != null) zip.close();
            } catch (IOException e2) {
            }
        }
    }
}
