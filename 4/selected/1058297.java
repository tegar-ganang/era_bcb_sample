package com.moviejukebox.gui.tools;

import java.util.zip.*;
import java.io.*;

public class Zip {

    public static void zip(String destination, String folder) {
        File fdir = new File(folder);
        File[] files = fdir.listFiles();
        PrintWriter stdout = new PrintWriter(System.out, true);
        int read = 0;
        FileInputStream in;
        byte[] data = new byte[1024];
        try {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(destination));
            out.setMethod(ZipOutputStream.DEFLATED);
            for (int i = 0; i < files.length; i++) {
                try {
                    stdout.println(files[i].getName());
                    ZipEntry entry = new ZipEntry(files[i].getName());
                    in = new FileInputStream(files[i].getPath());
                    out.putNextEntry(entry);
                    while ((read = in.read(data, 0, 1024)) != -1) {
                        out.write(data, 0, read);
                    }
                    out.closeEntry();
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void unzip(String destDir, String zipPath) {
        PrintWriter stdout = new PrintWriter(System.out, true);
        int read = 0;
        byte[] data = new byte[1024];
        ZipEntry entry;
        try {
            ZipInputStream in = new ZipInputStream(new FileInputStream(zipPath));
            stdout.println(zipPath);
            while ((entry = in.getNextEntry()) != null) {
                if (entry.getMethod() == ZipEntry.DEFLATED) {
                    stdout.println("  Inflating: " + entry.getName());
                } else {
                    stdout.println(" Extracting: " + entry.getName());
                }
                FileOutputStream out = new FileOutputStream(destDir + File.separator + entry.getName());
                while ((read = in.read(data, 0, 1024)) != -1) {
                    out.write(data, 0, read);
                }
                out.close();
            }
            in.close();
            stdout.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
