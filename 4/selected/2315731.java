package com.idocbox.common.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * This class used to extract entries in zip file.
 * @author Chunhui Li  email:wiseneuron@gmail.com
 * @since 0.0.1.1
 */
public class ZipExtracter {

    /**
	 * extract given entry in zip file to destination directory.
	 * if given entry is a directory, extract all folder and files under it to
	 * destination directory.
	 * @param zipFileName  name of zip file.
	 * @param entry   it can be a directory or a file.(if it is end with "/",it 
	 *                will be treated as a directory.) . For example, 
	 *                conf/ represents subdirectory in zip file.
	 *                Use "/" to represent root of zip file.
     *                
	 * @param desDir  destination directory.
	 * @param startDirLevel the level to start create directory.
	 *                      Its value is 1,2,...
	 * @throws IOException 
	 * @throws ZipException 
	 */
    public static void extract(final String zipFileName, final String entry, final String desDir, final int... startDirLevel) throws ZipException, IOException {
        File f = new File(zipFileName);
        if (f.exists()) {
            File desf = new File(desDir);
            if (!desf.exists()) {
                desf.mkdirs();
            }
            boolean toExtractDir = false;
            if (entry.endsWith("/")) {
                toExtractDir = true;
            }
            ZipFile zf = new ZipFile(f);
            if (toExtractDir) {
                Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zf.entries();
                ZipEntry ent = null;
                while (entries.hasMoreElements()) {
                    ent = entries.nextElement();
                    if (isUnderDir(ent, entry)) {
                        extract(zf, ent, desDir, startDirLevel);
                    } else {
                    }
                }
            } else {
                ZipEntry zipEntry = zf.getEntry(entry);
                if (null != zipEntry) {
                    extract(zf, zipEntry, desDir, startDirLevel);
                }
            }
            if (null != zf) {
                zf.close();
            }
        }
    }

    /**
     * extract given zip entry file from zip file
     *  to given destrination directory.
     * @param zf         ZipFile object.
     * @param zipEntry   zip entry to extract.
     * @param desDir     destrination directory.
     * @param startDirLevel the level to start create directory.
	 *                      Its value is 1,2,...
     * @throws IOException throw the exception whil desDir is no directory.
     */
    private static void extract(final ZipFile zf, final ZipEntry zipEntry, final String desDir, final int... startDirLevel) throws IOException {
        File desf = new File(desDir);
        if (!desf.exists()) {
            desf.mkdirs();
        }
        int start = 1;
        if (null != startDirLevel && startDirLevel.length > 0) {
            start = startDirLevel[0];
            if (start < 1) {
                start = 1;
            }
        }
        String startDir = "";
        String zeName = zipEntry.getName();
        String folder = zeName;
        boolean isDir = zipEntry.isDirectory();
        if (null != folder) {
            String[] folders = folder.split("\\/");
            if (null != folders && folders.length > 0) {
                int len = folders.length;
                if (start == 1) {
                    startDir = zeName;
                } else {
                    if (start > len) {
                    } else {
                        for (int i = start - 1; i < len; i++) {
                            startDir += "/" + folders[i];
                        }
                        if (null != startDir) {
                            startDir = startDir.substring(1);
                        }
                    }
                }
            }
        }
        startDir = StringUtils.trim(startDir);
        if (StringUtils.isNotEmpty(startDir)) {
            StringBuilder desFileName = new StringBuilder(desDir);
            if (!desDir.endsWith("/") && !startDir.startsWith("/")) {
                desFileName.append("/");
            }
            desFileName.append(startDir);
            File destFile = new File(desFileName.toString());
            if (isDir) {
                if (!destFile.exists()) {
                    destFile.mkdirs();
                }
            } else {
                File parentDir = new File(destFile.getParentFile().getPath());
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                InputStream is = zf.getInputStream(zipEntry);
                OutputStream os = new FileOutputStream(destFile);
                IOUtils.copy(is, os);
                if (null != is) {
                    is.close();
                }
                if (null != os) {
                    os.close();
                }
            }
        }
    }

    /**
     * Is given zip entry under given directory?
     * @param zipEntry  zip entry
     * @param dir       directory in zip file, it is end with "/". 
     *                  use "/" to represent root of zip file.
     *                  For example, conf/ represents subdirectory in zip file.
     * @return true: the entry is under the directory.
     *         false:the entry is not under the directory.
     */
    private static boolean isUnderDir(final ZipEntry zipEntry, final String dir) {
        boolean isunder = false;
        if ("/".equals(dir)) {
            return true;
        }
        if (null != zipEntry) {
            String name = zipEntry.getName();
            int idx = name.lastIndexOf('/');
            if (idx > 0) {
                String subDir = name.substring(0, idx + 1);
                if (subDir.startsWith(dir)) {
                    isunder = true;
                } else {
                    isunder = false;
                }
            } else {
                isunder = false;
            }
        }
        return isunder;
    }
}
