package org.icy.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.ice.utils.Encode;

public class FileUtils {

    public static void copy(File src, File dst) throws IOException {
        if (src.isDirectory() && dst.isDirectory()) {
            org.apache.commons.io.FileUtils.copyDirectory(src, dst);
        } else if (src.isFile()) {
            if (dst.isFile()) {
                org.apache.commons.io.FileUtils.copyFile(src, dst);
            } else if (dst.isDirectory()) {
                org.apache.commons.io.FileUtils.copyFileToDirectory(src, dst);
            }
        }
    }

    public static String createTempFolder(String baseFolder) {
        String tempFolder = baseFolder + File.separator + Encode.random(32);
        File file = new File(tempFolder);
        while (file.exists()) {
            tempFolder = baseFolder + File.separator + Encode.random(32);
            file = new File(tempFolder);
        }
        file.mkdir();
        return tempFolder;
    }

    public static String unzip(File archive, File baseFolder) throws Exception {
        if (!baseFolder.exists() || !baseFolder.canWrite()) {
            throw new Exception("Cannot unzip archive. Permission denied.");
        }
        String temp = createTempFolder(baseFolder.getAbsolutePath());
        ZipInputStream zin = null;
        try {
            zin = new ZipInputStream(new FileInputStream(archive));
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                File destinationFile = new File(temp, ze.getName());
                unpackEntry(destinationFile, zin, ze);
            }
            return temp;
        } finally {
            if (zin != null) {
                try {
                    zin.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    private static void unpackEntry(File destinationFile, ZipInputStream zin, ZipEntry entry) throws Exception {
        if (!entry.isDirectory()) {
            createFolders(destinationFile.getParentFile());
            FileOutputStream fis = new FileOutputStream(destinationFile);
            try {
                IOUtils.copy(zin, fis);
            } finally {
                zin.closeEntry();
                fis.close();
            }
        } else {
            createFolders(destinationFile);
        }
    }

    public static void createFolders(File destinationFile) {
        if (!destinationFile.exists()) destinationFile.mkdirs();
    }

    public static Collection<File> listFiles(File file, String exts) {
        return org.apache.commons.io.FileUtils.listFiles(file, exts.split(","), false);
    }

    public static String readFile(File file) throws IOException {
        return org.apache.commons.io.FileUtils.readFileToString(file, "UTF-8");
    }

    public static void deleteFolder(File module) throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(module);
    }
}
