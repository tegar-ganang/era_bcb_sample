package org.jdna.minecraft.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

public class Util {

    public static String relativePath(String src, String dest) {
        String sp = src;
        String dp = dest;
        if (dp.startsWith(sp)) {
            dp = dp.substring(sp.length());
            if (dp.startsWith(File.separator)) dp = dp.substring(1);
        }
        return dp;
    }

    public static String relativePath(File src, File dest) {
        String sp = src.getAbsolutePath();
        String dp = dest.getAbsolutePath();
        return relativePath(sp, dp);
    }

    public static void unzip(File sourceZipFile, File unzipDestinationDirectory) throws IOException {
        unzip(sourceZipFile, unzipDestinationDirectory, null);
    }

    public static void unzip(File sourceZipFile, File unzipDestinationDirectory, FileFilter filter) throws IOException {
        unzipDestinationDirectory.mkdirs();
        if (!unzipDestinationDirectory.exists()) {
            throw new IOException("Unable to create destination directory: " + unzipDestinationDirectory);
        }
        ZipFile zipFile;
        zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
        Enumeration<? extends ZipEntry> zipFileEntries = zipFile.entries();
        while (zipFileEntries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
            if (!entry.isDirectory()) {
                String currentEntry = entry.getName();
                File destFile = new File(unzipDestinationDirectory, currentEntry);
                if (filter == null || filter.accept(destFile)) {
                    File destinationParent = destFile.getParentFile();
                    destinationParent.mkdirs();
                    BufferedInputStream is = new BufferedInputStream(zipFile.getInputStream(entry));
                    FileOutputStream fos = new FileOutputStream(destFile);
                    IOUtils.copyLarge(is, fos);
                    fos.flush();
                    IOUtils.closeQuietly(fos);
                }
            }
        }
        zipFile.close();
    }

    public static void zip(File srcDir, File destFile, FileFilter filter) throws IOException {
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(destFile));
            Collection<File> files = FileUtils.listFiles(srcDir, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
            for (File f : files) {
                if (filter == null || filter.accept(f)) {
                    FileInputStream in = FileUtils.openInputStream(f);
                    out.putNextEntry(new ZipEntry(Util.relativePath(srcDir, f).replace('\\', '/')));
                    IOUtils.copyLarge(in, out);
                    out.closeEntry();
                    IOUtils.closeQuietly(in);
                }
            }
            IOUtils.closeQuietly(out);
        } catch (Throwable t) {
            throw new IOException("Failed to create zip file", t);
        } finally {
            if (out != null) {
                out.flush();
                IOUtils.closeQuietly(out);
            }
        }
    }

    public static void zip(File srcDir, File destFile) throws IOException {
        zip(srcDir, destFile, null);
    }
}
