package eu.popeye.middleware.dataSharing.test.plugin;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class Unzip {

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    public static final void extract(String inputFile, String outputDirName) {
        Enumeration entries;
        ZipFile zipFile;
        File outputDir = new File(outputDirName);
        if (!outputDir.exists()) outputDir.mkdirs();
        try {
            zipFile = new ZipFile(inputFile);
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
                    System.err.println("Extracting directory: " + entry.getName());
                    (new File(outputDir.getAbsolutePath() + entry.getName())).mkdir();
                    continue;
                }
                System.err.println("Extracting file: " + entry.getName());
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(outputDir.getAbsolutePath() + entry.getName())));
            }
            zipFile.close();
        } catch (IOException ioe) {
            System.err.println("Unhandled exception:");
            ioe.printStackTrace();
            return;
        }
    }
}
