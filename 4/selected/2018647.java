package com.san.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.ZipFile;
import org.apache.commons.compress.zip.ZipEntry;
import org.apache.commons.compress.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;

public class ZipUtil {

    public static ZipFile readZipFile(String location) {
        ZipFile z = null;
        try {
            z = new ZipFile(location);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return z;
    }

    static List<String> readZipFilesOftypeToFolder(String zipFileLocation, String outputDir, String fileType) {
        List<String> list = new ArrayList<String>();
        ZipFile zipFile = readZipFile(zipFileLocation);
        FileOutputStream output = null;
        InputStream inputStream = null;
        Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipFile.entries();
        try {
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName != null && entryName.toLowerCase().endsWith(fileType)) {
                    inputStream = zipFile.getInputStream(entry);
                    String fileName = outputDir + entryName.substring(entryName.lastIndexOf("/"));
                    File file = new File(fileName);
                    output = new FileOutputStream(file);
                    IOUtils.copy(inputStream, output);
                    list.add(fileName);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (output != null) output.close();
                if (inputStream != null) inputStream.close();
                if (zipFile != null) zipFile.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return list;
    }

    public static void writeZipFile(String location, Map<String, String> files) {
        Set<Entry<String, String>> entrySet = files.entrySet();
        ZipOutputStream zo = null;
        try {
            zo = new ZipOutputStream(new FileOutputStream(location));
            for (Iterator<Entry<String, String>> iter = entrySet.iterator(); iter.hasNext(); ) {
                Map.Entry<String, String> entry = iter.next();
                String zipFileName = entry.getKey();
                String zipFileContent = entry.getValue();
                ZipEntry zEntry = new ZipEntry(zipFileName);
                BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(zipFileContent.getBytes()));
                int bytesRead;
                byte[] buffer = new byte[1024];
                zEntry.setMethod(ZipEntry.DEFLATED);
                zo.putNextEntry(zEntry);
                while ((bytesRead = bis.read(buffer)) != -1) {
                    zo.write(buffer, 0, bytesRead);
                }
                bis.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                zo.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
