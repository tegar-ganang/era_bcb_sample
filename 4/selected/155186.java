package com.jigen;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class JarGenerator {

    private final byte buffer[] = new byte[200 * 1024];

    private final ZipOutputStream zos;

    public JarGenerator(File outputJarInstaller, String baseZipFileResource) throws IOException {
        outputJarInstaller.delete();
        InputStream jixJarInputStream = JarGenerator.class.getResourceAsStream(baseZipFileResource);
        if (jixJarInputStream == null) throw new IOException(baseZipFileResource + " was not found!");
        ZipInputStream zis = new ZipInputStream(jixJarInputStream);
        this.zos = new ZipOutputStream(new FileOutputStream(outputJarInstaller));
        ZipEntry zipEntry;
        while ((zipEntry = zis.getNextEntry()) != null) {
            zos.putNextEntry(new ZipEntry(zipEntry));
            int length;
            while ((length = zis.read(buffer)) != -1) zos.write(buffer, 0, length);
            zis.closeEntry();
            zos.closeEntry();
        }
        zis.close();
    }

    public void appendResource(String resourceName, String resource) throws IOException {
        InputStream fileStream = JarGenerator.class.getResourceAsStream(resource);
        appendFile(resourceName, fileStream);
        fileStream.close();
    }

    public void appendFile(String resourceName, File file) throws IOException {
        FileInputStream fileStream = new FileInputStream(file);
        appendFile(resourceName, fileStream);
        fileStream.close();
    }

    public void appendFile(String resourceName, byte[] file) throws IOException {
        ByteArrayInputStream fileStream = new ByteArrayInputStream(file);
        appendFile(resourceName, fileStream);
        fileStream.close();
    }

    public void appendFile(String resourceName, InputStream inputStream) throws IOException {
        zos.putNextEntry(new ZipEntry(resourceName));
        int length;
        while ((length = inputStream.read(buffer)) != -1) zos.write(buffer, 0, length);
        zos.closeEntry();
    }

    public void dispose() throws IOException {
        zos.close();
    }
}
