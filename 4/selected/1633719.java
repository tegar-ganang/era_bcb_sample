package com.aaspring.util.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @author Balazs
 * 
 */
public class ZipFileEntryAccessor extends AbstractFileAccessor {

    private class ZipPathData {

        public Map<String, ZipEntry> entriesByName = new LinkedHashMap<String, ZipEntry>();

        public String tmpPath;
    }

    private static int counter = new Random().nextInt();

    public static final String ZIP_ENTRY_EXTENSION = "zipEntryExtension";

    public static final String ZIP_ENTRY_PATH = "zipEntryPath";

    public static final String ZIP_PATH = "zipPath";

    public static boolean deleteDirectory(final File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    private final String SEPARATOR = System.getProperty("file.separator");

    private final String systemTmpPath = System.getProperty("java.io.tmpdir");

    private final List<ZipFile> zipFiles = new ArrayList<ZipFile>();

    private Map<String, ZipPathData> zipPathEntryMap;

    @Override
    protected void closeImpl() {
        if (state == States.OPENED4INPUT) {
            for (ZipFile zipFile : zipFiles) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                }
            }
        } else {
            for (OutputStream out : outputStreamMap.values()) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            for (String zipPath : zipPathEntryMap.keySet()) {
                try {
                    ZipOutputStream zOut = new ZipOutputStream(new FileOutputStream(zipPath, false));
                    ZipPathData zpd = zipPathEntryMap.get(zipPath);
                    File tmpDir = new File(zpd.tmpPath);
                    File[] subDirs = tmpDir.listFiles();
                    for (File subDir : subDirs) {
                        recurseTmpDir(subDir.getName(), subDir, zpd, zOut);
                    }
                    zOut.close();
                    deleteDirectory(tmpDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Map<String, Set<FileAccessorProperties>> generateZipFileMap() {
        Map<String, Set<FileAccessorProperties>> zipFileMap = new LinkedHashMap<String, Set<FileAccessorProperties>>();
        for (FileAccessorProperties fap : fapSet) {
            Map<String, String> attrs = fap.getAttributes();
            String zipPath = attrs.get(ZIP_PATH);
            Set<FileAccessorProperties> zipList = zipFileMap.get(zipPath);
            if (zipList == null) {
                zipList = new LinkedHashSet<FileAccessorProperties>();
                zipFileMap.put(zipPath, zipList);
            }
            zipList.add(fap);
        }
        return zipFileMap;
    }

    @Override
    protected Map<FileAccessorProperties, InputStream> getInputStreamsImpl(final Set<FileAccessorProperties> fapSet) {
        Map<String, Set<FileAccessorProperties>> zipFileMap = generateZipFileMap();
        Map<FileAccessorProperties, InputStream> result = new LinkedHashMap<FileAccessorProperties, InputStream>();
        for (String zipPath : zipFileMap.keySet()) {
            ZipFile zipFile;
            try {
                zipFile = new ZipFile(zipPath);
                zipFiles.add(zipFile);
                Set<FileAccessorProperties> faps = zipFileMap.get(zipPath);
                for (FileAccessorProperties fap : faps) {
                    Map<String, String> props = fap.getAttributes();
                    String path = props.get(ZIP_ENTRY_PATH);
                    String extension = props.get(ZIP_ENTRY_EXTENSION);
                    Locale locale = fap.getLocale();
                    try {
                        ZipEntry entry = zipFile.getEntry(FileAccessorUtil.getFilePath(path, extension, locale));
                        if (entry != null) {
                            InputStream is = zipFile.getInputStream(entry);
                            if (is != null) result.put(fap, is);
                        }
                    } catch (IOException e) {
                    }
                }
            } catch (IOException e1) {
            }
        }
        return result;
    }

    @Override
    protected Map<FileAccessorProperties, OutputStream> getOutputStreamsImpl(final Set<FileAccessorProperties> fapSet) {
        Map<String, Set<FileAccessorProperties>> zipFileMap = generateZipFileMap();
        Map<FileAccessorProperties, OutputStream> result = new LinkedHashMap<FileAccessorProperties, OutputStream>();
        zipPathEntryMap = new LinkedHashMap<String, ZipPathData>();
        for (String zipPath : zipFileMap.keySet()) {
            String tmpPath = unPackZipIntoTmpPath(zipPath);
            for (FileAccessorProperties fap : zipFileMap.get(zipPath)) {
                Map<String, String> props = fap.getAttributes();
                String path = props.get(ZIP_ENTRY_PATH);
                String extension = props.get(ZIP_ENTRY_EXTENSION);
                Locale locale = fap.getLocale();
                File file = new File(tmpPath + "/" + FileAccessorUtil.getFilePath(path, extension, locale));
                File parentFile = file.getParentFile();
                if (parentFile != null) parentFile.mkdirs();
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    result.put(fap, out);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    private void recurseTmpDir(final String parentPath, final File parentDir, final ZipPathData zpd, final ZipOutputStream zOut) {
        String filePath = parentPath;
        if (parentDir.isDirectory()) {
            File[] subDirs = parentDir.listFiles();
            for (File subDir : subDirs) {
                recurseTmpDir(filePath + "/" + subDir.getName(), subDir, zpd, zOut);
            }
        } else {
            File file = parentDir;
            ZipEntry zipEntry = new ZipEntry(filePath);
            if (zipEntry == null) {
                zipEntry = new ZipEntry(filePath);
            }
            try {
                zOut.putNextEntry(zipEntry);
                FileInputStream fInput = new FileInputStream(file);
                byte[] buffer = new byte[10000];
                while (fInput.available() > 0) {
                    int byteRead = fInput.read(buffer);
                    zOut.write(buffer, 0, byteRead);
                }
                fInput.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String unPackZipIntoTmpPath(final String zipPath) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zipPath);
        } catch (IOException e) {
        }
        ZipPathData zipPathData = zipPathEntryMap.get(zipPath);
        if (zipPathData == null) {
            String tmpPath = systemTmpPath + "aaspringTranslator/" + Math.abs(zipPath.hashCode()) + "_" + new Date().getTime();
            File tmpDir = new File(tmpPath);
            tmpDir.mkdirs();
            zipPathData = new ZipPathData();
            zipPathData.tmpPath = tmpPath;
            zipPathEntryMap.put(zipPath, zipPathData);
        }
        try {
            if (zipFile != null) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    zipPathData.entriesByName.put(zipEntry.getName(), zipEntry);
                    File outputFile = new File(zipPathData.tmpPath + "/" + zipEntry.getName());
                    if (zipEntry.isDirectory()) outputFile.mkdirs(); else {
                        InputStream is = zipFile.getInputStream(zipEntry);
                        outputFile.getParentFile().mkdirs();
                        FileOutputStream fout = new FileOutputStream(outputFile);
                        byte[] buffer = new byte[2189];
                        while (is.available() > 0) {
                            int readBytes = is.read(buffer);
                            fout.write(buffer, 0, readBytes);
                        }
                        fout.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zipFile != null) try {
                zipFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return zipPathData.tmpPath;
    }
}
