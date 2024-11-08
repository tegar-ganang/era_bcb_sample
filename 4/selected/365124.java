package de.beas.explicanto.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Unzipper {

    public static void unzip(String zipFile, String destDir) throws IOException {
        ZipFile zf = new ZipFile(zipFile);
        Enumeration enumeration = zf.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) enumeration.nextElement();
            System.out.println("enuming: " + ze.getName());
            File dest = new File(destDir + "/" + ze.getName());
            dest = dest.getParentFile();
            System.out.println("mkdir: " + ze.getName());
            dest.mkdirs();
        }
        enumeration = zf.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) enumeration.nextElement();
            System.out.println("enuming: " + ze.getName());
            File dest = new File(destDir + "/" + ze.getName());
            if (!ze.isDirectory()) writeFile(zf.getInputStream(ze), dest);
        }
        zf.close();
    }

    private static void writeFile(InputStream is, File file) throws IOException {
        if (file.exists()) return;
        System.out.println("unpacking " + file.getPath());
        FileOutputStream fos = new FileOutputStream(file);
        byte[] data = new byte[100000];
        int read = is.read(data);
        while (read > 0) {
            fos.write(data, 0, read);
            read = is.read(data);
        }
        fos.close();
    }
}
