package org.opendte.common;

import java.util.*;
import java.util.zip.*;
import java.rmi.registry.*;
import java.io.*;

public class Util {

    public static void sleep(long duration) {
        if (duration < 0) throw new IllegalArgumentException("Duration must be greater than zero!");
        if (duration == 0) return;
        long slept = 0;
        while (slept < duration) {
            long sleepStart = System.currentTimeMillis();
            try {
                Thread.currentThread().sleep(10);
            } catch (InterruptedException x) {
            }
            long diff = System.currentTimeMillis() - sleepStart;
            slept += diff;
        }
    }

    public static int createLocalRegistry() throws DteException {
        int failed = 0;
        while (true) {
            int port = (int) (10000.0 + 54000.0 * Math.random());
            try {
                LocateRegistry.createRegistry(port);
                return port;
            } catch (Exception x) {
                failed++;
                if (failed >= 10) {
                    throw new DteException("Failed to create local RMI Registry", x);
                }
            }
        }
    }

    public static void unzipFromStream(InputStream in, File outputDirectory) throws IOException {
        ZipInputStream zin = new ZipInputStream(in);
        ZipEntry entry;
        while ((entry = zin.getNextEntry()) != null) {
            String entryName = entry.getName();
            if (entryName.startsWith("/")) entryName = entryName.substring(1); else if (entryName.startsWith("./")) entryName = entryName.substring(2);
            String outfileName = outputDirectory.getAbsolutePath() + File.separator + entryName;
            File outfile = new File(outfileName);
            if (entry.isDirectory()) {
                Logger.debug("next zip entry: " + entryName + " is a directory");
                if (!outfile.exists()) outfile.mkdirs();
            } else {
                Logger.debug("next zip entry: " + entryName + " is a regular file");
                Logger.debug("storing entry to file " + outfileName);
                File outfileParent = outfile.getParentFile();
                if (outfileParent != null) {
                    if (!outfileParent.exists()) outfileParent.mkdirs();
                }
                if (outfile.exists()) outfile.delete();
                outfile.createNewFile();
                FileOutputStream fout = new FileOutputStream(outfile);
                while (true) {
                    byte buf[] = new byte[4096];
                    int read = zin.read(buf, 0, 4096);
                    if (read > -1) fout.write(buf, 0, read); else break;
                }
                fout.flush();
                fout.close();
            }
        }
    }
}
