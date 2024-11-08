package org.sekomintory.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

public class ZipUtil {

    public static Map<String, byte[]> unzip(InputStream inputStream) throws IOException {
        Map<String, byte[]> map = new HashMap<String, byte[]>();
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            byte[] data = IOUtils.toByteArray(zipInputStream);
            String relativePath = FilenameUtils.separatorsToSystem(zipEntry.getName());
            map.put(relativePath, data);
        }
        return map;
    }

    public static void unzip(File targetFolder, ZipInputStream zipInputStream) throws IOException {
        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            byte[] data = IOUtils.toByteArray(zipInputStream);
            String relativePath = FilenameUtils.separatorsToSystem(zipEntry.getName());
            FileUtils.writeByteArrayToFile(new File(targetFolder, relativePath), data);
        }
    }

    public static byte[] zipFolder(File folder) throws IOException {
        return ZipUtil.zipFolder(folder, folder.getCanonicalPath());
    }

    public static byte[] zipFolder(File folder, String relativePath) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
        ZipUtil.zipFolder(folder, zipOutputStream, relativePath);
        zipOutputStream.close();
        return byteArrayOutputStream.toByteArray();
    }

    private static void zipFolder(File folder, ZipOutputStream zipOutputStream, String relativePath) throws IOException {
        File[] children = folder.listFiles();
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (child.isFile()) {
                String zipEntryName = children[i].getCanonicalPath().replace(relativePath + File.separator, "");
                ZipEntry entry = new ZipEntry(zipEntryName);
                zipOutputStream.putNextEntry(entry);
                InputStream inputStream = new FileInputStream(child);
                IOUtils.copy(inputStream, zipOutputStream);
                inputStream.close();
            } else {
                ZipUtil.zipFolder(child, zipOutputStream, relativePath);
            }
        }
    }
}
