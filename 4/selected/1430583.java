package de.beas.explicanto.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zipper {

    public static void zip(String file, String dir) throws IOException {
        File zipFile = new File(file);
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        File[] kids = new File(dir).listFiles();
        for (int i = 0; i < kids.length; i++) {
            if (kids[i].isDirectory()) addDir(zos, kids[i], dir); else add(zos, kids[i], dir);
        }
        zos.close();
    }

    private static void add(ZipOutputStream zos, File file, String root) throws IOException {
        String name = file.getPath();
        name = name.substring(root.length());
        ZipEntry ze = new ZipEntry(name);
        zos.putNextEntry(ze);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[100000];
        int read = fis.read(data);
        while (read > 0) {
            zos.write(data, 0, read);
            read = fis.read(data);
        }
        zos.closeEntry();
        fis.close();
        file.delete();
    }

    private static void addDir(ZipOutputStream zos, File dir, String root) throws IOException {
        String name = dir.getPath();
        name = name.substring(root.length());
        ZipEntry ze = new ZipEntry(name + "/");
        zos.putNextEntry(ze);
        File[] kids = dir.listFiles();
        for (int i = 0; i < kids.length; i++) {
            if (kids[i].isDirectory()) addDir(zos, kids[i], root); else add(zos, kids[i], root);
        }
    }
}
