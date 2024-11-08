package org.wikiup.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.wikiup.core.inf.ExceptionHandler;
import org.wikiup.core.inf.ext.LogicalFilter;

public class FileUtil {

    public static String getFilePath(String file) {
        return getFilePath(file, File.separatorChar);
    }

    public static String getFilePath(String file, char separator) {
        int pos = file.lastIndexOf(separator);
        return pos != -1 ? file.substring(0, pos) : "";
    }

    public static String getFileExt(String file) {
        int pos = file.lastIndexOf('.');
        return pos != -1 ? file.substring(pos + 1) : "";
    }

    public static String getFileName(String file, char separator) {
        int epos = file.lastIndexOf('.');
        int ppos = file.lastIndexOf(separator);
        return epos != -1 && epos > ppos ? file.substring(ppos != -1 ? ppos + 1 : 0, epos) : file.substring(ppos != -1 ? ppos + 1 : 0);
    }

    public static String getFileName(String file) {
        return getFileName(file, File.separatorChar);
    }

    public static String getFileNameExt(String file) {
        int ppos = getSeparatorPosition(file);
        return file.substring(ppos != -1 ? ppos + 1 : 0);
    }

    public static void copy(File source, File target) throws IOException {
        if (source.isFile() && (target.isFile() || !target.exists())) copyFile(source, target); else if (source.isFile() && target.isDirectory()) copyFileToDirectory(source, target); else if (source.isDirectory()) copyDirectoryToDirectory(source, target);
    }

    public static void copyFile(File source, File dest) throws FileNotFoundException {
        copyFile(source, dest, null);
    }

    public static void copyFile(File source, File dest, ExceptionHandler eh) throws FileNotFoundException {
        OutputStream os = null;
        InputStream is = null;
        try {
            os = StreamUtil.getBufferedOutputStream(new FileOutputStream(dest));
            is = StreamUtil.getBufferedInputStream(new FileInputStream(source));
            StreamUtil.copy(os, is, eh);
        } finally {
            StreamUtil.close(os, eh);
            StreamUtil.close(is, eh);
        }
    }

    public static void copyFileToDirectory(File source, File dest) throws IOException {
        copyFile(source, new File(dest, source.getName()));
    }

    public static void copyDirectoryToDirectory(File source, File dest) throws IOException {
        File files[] = source.listFiles();
        int i;
        dest.mkdirs();
        for (i = 0; i < files.length; i++) copy(files[i], new File(dest, getFileNameExt(files[i].getName())));
    }

    public static String joinPath(String path, String file, char separator) {
        return StringUtil.connect(path, file, separator);
    }

    public static String joinPath(String path, String file) {
        return StringUtil.connect(path, file, File.separatorChar);
    }

    public static boolean isDirectory(String fileName) {
        File file = new File(fileName);
        return file.isDirectory();
    }

    public static boolean isExists(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    public static boolean makeDirectory(String path, boolean multi) {
        File file = new File(path);
        return multi ? file.mkdirs() : file.mkdir();
    }

    public static boolean makeDirectory(String path) {
        return makeDirectory(path, false);
    }

    public static String loadTextFromFile(String path) throws FileNotFoundException {
        try {
            return loadTextFromFile(new File(path), null);
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    public static String loadTextFromFile(String path, String charSet) throws FileNotFoundException, UnsupportedEncodingException {
        return loadTextFromFile(new File(path), charSet);
    }

    public static String loadTextFromFile(File file, String charSet) throws UnsupportedEncodingException, FileNotFoundException {
        StringWriter writer = new StringWriter();
        Reader reader = null;
        try {
            reader = charSet == null ? new FileReader(file) : new InputStreamReader(new FileInputStream(file), charSet);
            StreamUtil.copy(writer, reader);
        } finally {
            StreamUtil.close(reader);
        }
        return writer.toString();
    }

    public static File getFile(String path) {
        return new File(path);
    }

    public static File getFile(String path, String name) {
        return new File(path, name);
    }

    public static File touch(String filename) throws IOException {
        return touch(getFile(filename));
    }

    public static File touch(File file) throws IOException {
        if (!file.exists()) {
            makeDirectory(file.getParent(), true);
            file.createNewFile();
        }
        return file;
    }

    public static boolean delete(File file) {
        if (file.isDirectory()) {
            File files[] = file.listFiles();
            int loop;
            for (loop = 0; loop < files.length; loop++) if (!delete(files[loop])) return false;
        }
        return file.delete();
    }

    public static boolean isUpToDate(File file, long date) {
        return file.lastModified() >= date;
    }

    public static boolean isHidden(File file) {
        return file.getName().charAt(0) == '.' || file.isHidden();
    }

    public static void unzip(InputStream is, File directory, LogicalFilter<String> filter) throws FileNotFoundException, IOException {
        byte data[] = new byte[16384];
        ZipInputStream zis = null;
        try {
            ZipEntry entry;
            zis = new ZipInputStream(is);
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (filter == null || filter.filter(name)) if (entry.isDirectory()) makeDirectory(joinPath(directory.getAbsolutePath(), name), true); else unzip(data, directory, zis, entry);
            }
        } finally {
            if (zis != null) zis.close();
        }
    }

    public static void unzip(File file, File directory) throws FileNotFoundException, IOException {
        unzip(file, directory, null);
    }

    public static void unzip(File file, File directory, LogicalFilter<String> filter) throws FileNotFoundException, IOException {
        InputStream is = StreamUtil.getBufferedInputStream(new FileInputStream(file));
        unzip(is, directory, filter);
        is.close();
    }

    public static String getSystemFilePath(String path) {
        return StringUtil.trimRight(StringUtil.replaceAll(path, File.separatorChar == '/' ? '\\' : '/', File.separatorChar), "/\\");
    }

    private static void unzip(byte[] buffer, File directory, ZipInputStream zis, ZipEntry entry) throws FileNotFoundException, IOException {
        int count;
        FileOutputStream fos = new FileOutputStream(touch(joinPath(directory.getAbsolutePath(), entry.getName())));
        BufferedOutputStream dest = new BufferedOutputStream(fos, buffer.length);
        while ((count = zis.read(buffer, 0, buffer.length)) != -1) dest.write(buffer, 0, count);
        dest.close();
    }

    private static int getSeparatorPosition(String fileName) {
        int pos = fileName.lastIndexOf('\\');
        return pos != -1 ? pos : fileName.lastIndexOf('/');
    }
}
