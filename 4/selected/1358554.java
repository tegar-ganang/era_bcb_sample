package test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Stuff {

    public static void extractZip(String jarFile, String destDir) throws IOException {
        extractZip(new File(jarFile), new File(destDir));
    }

    public static void extractZip(File jarFile, File destDir) throws IOException {
        java.util.zip.ZipFile jar = new java.util.zip.ZipFile(jarFile);
        java.util.Enumeration en = jar.entries();
        while (en.hasMoreElements()) {
            java.util.zip.ZipEntry file = (java.util.zip.ZipEntry) en.nextElement();
            java.io.File f = new java.io.File(destDir + java.io.File.separator + file.getName());
            if (file.isDirectory()) {
                f.mkdir();
                continue;
            }
            f.getParentFile().mkdirs();
            java.io.InputStream is = jar.getInputStream(file);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
            while (is.available() > 0) {
                fos.write(is.read());
            }
            fos.close();
            is.close();
        }
    }

    public static File createZip(File tmpDir, String[] filenames, String zipName) throws IOException {
        return createZip(tmpDir, filenames, zipName, null);
    }

    public static File createZip(File tmpDir, String[] filenames, String zipName, String text) throws IOException {
        File zip = new File(tmpDir, zipName);
        byte[] buf = new byte[1024];
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
        for (int i = 0; i < filenames.length; i++) {
            out.putNextEntry(new ZipEntry(filenames[i]));
            if (filenames[i].endsWith("/")) continue;
            if (text == null) {
                text = "this is file " + filenames[i];
            }
            InputStream in = new ByteArrayInputStream(text.getBytes());
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.closeEntry();
            in.close();
        }
        out.close();
        return zip;
    }
}
