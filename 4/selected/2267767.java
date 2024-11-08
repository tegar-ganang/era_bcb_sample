package com.anil.wibut.test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zipping {

    public void directory2zip(File dir, File zipFile, String zipComment, int zipCompressionLevel) {
        if (dir.canRead()) {
            try {
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
                zos.setMethod(ZipOutputStream.DEFLATED);
                zos.setComment(zipComment);
                zos.setLevel(zipCompressionLevel);
                this.putFileAsZipEntries(zos, dir);
                zos.finish();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("neden ki??");
        }
    }

    public void putFileAsZipEntries(ZipOutputStream zos, File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                this.putFileAsZipEntries(zos, files[i]);
            }
        } else {
            try {
                zos.putNextEntry(new ZipEntry(file.getAbsolutePath().substring(file.getAbsolutePath().indexOf(File.separator) + 1)));
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                copyStreamsWithoutClose(bis, zos, new byte[1024 * 20]);
                bis.close();
                zos.closeEntry();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Copies all data from in to out
     *  @param in the input stream
     *  @param out the output stream
     *  @param buffer copy buffer
     */
    static void copyStreamsWithoutClose(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        int b;
        while ((b = in.read(buffer)) != -1) out.write(buffer, 0, b);
    }

    public static void main(String[] args) {
        Zipping z = new Zipping();
        z.directory2zip(new File("C:\\ne"), new File("C:\\ne9.zip"), "Testing...", 9);
    }
}
