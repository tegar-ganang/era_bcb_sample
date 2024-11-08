package com.mainatom.utils;

import org.apache.commons.io.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

/**
 * Файловые утилиты. Для удобства унаследован от org.apache.commons.io.FileUtils и делегирует
 * org.apache.commons.io.FilenameUtils
 */
public class JpFileUtils {

    private static int BUFSIZE = 8192;

    /**
     * Gets path from a <code>List</code> of <code>String</code>s.
     *
     * @param pathStack <code>List</code> of <code>String</code>s to be concated
     *                  as a path.
     * @return <code>String</code>, never <code>null</code>
     * @since Ant 1.7
     */
    private static String internal_getPath(List pathStack) {
        return internal_getPath(pathStack, '/');
    }

    /**
     * Gets path from a <code>List</code> of <code>String</code>s.
     *
     * @param pathStack     <code>List</code> of <code>String</code>s to be concated
     *                      as a path.
     * @param separatorChar <code>char</code> to be used as separator between names in
     *                      path
     * @return <code>String</code>, never <code>null</code>
     */
    private static String internal_getPath(final List pathStack, final char separatorChar) {
        final StringBuffer buffer = new StringBuffer();
        final Iterator iter = pathStack.iterator();
        if (iter.hasNext()) {
            buffer.append(iter.next());
        }
        while (iter.hasNext()) {
            buffer.append(separatorChar);
            buffer.append(iter.next());
        }
        return buffer.toString();
    }

    /**
     * Gets all names of the path as an array of <code>String</code>s.
     *
     * @param path to get names from
     * @return <code>String</code>s, never <code>null</code>
     */
    private static String[] internal_getPathStack(String path) {
        String normalizedPath = path.replace(File.separatorChar, '/');
        return normalizedPath.split("/");
    }

    /**
     * Calculates the relative path between two files.
     * <p>
     * Implementation note:<br/> This function my throw an IOException if an
     * I/O error occurs because its use of the canonical pathname may require
     * filesystem queries.
     * </p>
     *
     * @param fromFile the <code>File</code> to calculate the path from
     * @param toFile   the <code>File</code> to calculate the path to
     * @return the relative path between the files
     * @throws Exception for undocumented reasons
     * @see File#getCanonicalPath()
     */
    public static String getRelativePath(File fromFile, File toFile) throws Exception {
        String fromPath = fromFile.getCanonicalPath();
        String toPath = toFile.getCanonicalPath();
        String[] fromPathStack = internal_getPathStack(fromPath);
        String[] toPathStack = internal_getPathStack(toPath);
        if (0 < toPathStack.length && 0 < fromPathStack.length) {
            if (!fromPathStack[0].equals(toPathStack[0])) {
                return internal_getPath(Arrays.asList(toPathStack));
            }
        } else {
            return internal_getPath(Arrays.asList(toPathStack));
        }
        int minLength = Math.min(fromPathStack.length, toPathStack.length);
        int same = 1;
        for (; same < minLength; same++) {
            if (!fromPathStack[same].equals(toPathStack[same])) {
                break;
            }
        }
        List relativePathStack = new ArrayList();
        for (int i = same; i < fromPathStack.length; i++) {
            relativePathStack.add("..");
        }
        for (int i = same; i < toPathStack.length; i++) {
            relativePathStack.add(toPathStack[i]);
        }
        return internal_getPath(relativePathStack);
    }

    /**
     * Прочитать reader в строку
     */
    public static String readString(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] b = new char[BUFSIZE];
        int n;
        while ((n = reader.read(b)) > 0) {
            sb.append(b, 0, n);
        }
        return sb.toString();
    }

    /**
     * Прочитать InputStream в строку
     */
    public static String readString(InputStream st) throws IOException {
        Reader r = new InputStreamReader(st);
        return readString(r);
    }

    /**
     * Прочитать InputStream в строку
     */
    public static String readString(InputStream st, String charset) throws IOException {
        Reader r = new InputStreamReader(st, charset);
        return readString(r);
    }

    /**
     * Прочитать файл в строку
     */
    public static String readString(String filename) throws IOException {
        return readString(new File(filename));
    }

    /**
     * Прочитать файл в строку
     */
    public static String readString(File file) throws IOException {
        Reader r = new FileReader(file);
        String s;
        try {
            s = readString(r);
        } finally {
            r.close();
        }
        return s;
    }

    /**
     * Прочитать файл в строку
     */
    public static String readString(String filename, String charset) throws IOException {
        return readString(new File(filename));
    }

    /**
     * Прочитать файл в строку
     */
    public static String readString(File file, String charset) throws IOException {
        Reader r = new InputStreamReader(new FileInputStream(file), charset);
        String s;
        try {
            s = readString(r);
        } finally {
            r.close();
        }
        return s;
    }

    /**
     * Запись потока в файл
     *
     * @param src исходный поток (закрывается после окончания копирования)
     * @param dst куда копировать (закрывается после окончания копирования)
     */
    public static void saveStreamToFile(InputStream src, File dst) throws Exception {
        BufferedInputStream fSrc = null;
        BufferedOutputStream fDst = null;
        try {
            fSrc = new BufferedInputStream(src);
            fDst = new BufferedOutputStream(new FileOutputStream(dst));
            byte[] b = new byte[BUFSIZE];
            int n;
            while ((n = fSrc.read(b)) > 0) {
                fDst.write(b, 0, n);
            }
        } finally {
            if (fSrc != null) {
                fSrc.close();
            }
            if (fDst != null) {
                fDst.close();
            }
        }
    }

    /**
     * Запись потока в другой поток. Потоки после использования не закрываются.
     *
     * @param src исходный поток
     * @param dst куда копировать
     */
    public static void saveStreamToStream(InputStream src, OutputStream dst) throws Exception {
        BufferedInputStream fSrc = new BufferedInputStream(src);
        BufferedOutputStream fDst = new BufferedOutputStream(dst);
        byte[] b = new byte[BUFSIZE];
        int n;
        while ((n = fSrc.read(b)) > 0) {
            fDst.write(b, 0, n);
        }
        fDst.flush();
    }

    /**
     * Запись строки в файл
     *
     * @param s       строка
     * @param file    файл
     * @param charset кодировка, если не указана, берется текущая системная
     */
    public static void writeString(String s, File file, String charset) throws Exception {
        Writer f;
        if (charset.length() == 0) {
            f = new FileWriter(file);
        } else {
            f = new OutputStreamWriter(new FileOutputStream(file), charset);
        }
        try {
            f.write(s);
        } finally {
            f.close();
        }
    }

    /**
     * Запись строки в файл в текущей системной кодировке
     *
     * @param s    строка
     * @param file файл
     */
    public static void writeString(String s, File file) throws Exception {
        writeString(s, file, "");
    }

    public static String byteCountToDisplaySize(long size) {
        return FileUtils.byteCountToDisplaySize(size);
    }

    public static Checksum checksum(File file, Checksum checksum) throws IOException {
        return FileUtils.checksum(file, checksum);
    }

    public static long checksumCRC32(File file) throws IOException {
        return FileUtils.checksumCRC32(file);
    }

    public static void cleanDirectory(File directory) throws IOException {
        FileUtils.cleanDirectory(directory);
    }

    public static boolean contentEquals(File file1, File file2) throws IOException {
        return FileUtils.contentEquals(file1, file2);
    }

    public static void copyDirectory(File srcDir, File destDir) throws IOException {
        FileUtils.copyDirectory(srcDir, destDir);
    }

    public static void copyDirectory(File srcDir, File destDir, FileFilter filter) throws IOException {
        FileUtils.copyDirectory(srcDir, destDir, filter);
    }

    public static void copyDirectory(File srcDir, File destDir, FileFilter filter, boolean preserveFileDate) throws IOException {
        FileUtils.copyDirectory(srcDir, destDir, filter, preserveFileDate);
    }

    public static void copyDirectory(File srcDir, File destDir, boolean preserveFileDate) throws IOException {
        FileUtils.copyDirectory(srcDir, destDir, preserveFileDate);
    }

    public static void copyDirectoryToDirectory(File srcDir, File destDir) throws IOException {
        FileUtils.copyDirectoryToDirectory(srcDir, destDir);
    }

    public static void copyFile(File srcFile, File destFile) throws IOException {
        FileUtils.copyFile(srcFile, destFile);
    }

    public static void copyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
        FileUtils.copyFile(srcFile, destFile, preserveFileDate);
    }

    public static void copyFileToDirectory(File srcFile, File destDir) throws IOException {
        FileUtils.copyFileToDirectory(srcFile, destDir);
    }

    public static void copyFileToDirectory(File srcFile, File destDir, boolean preserveFileDate) throws IOException {
        FileUtils.copyFileToDirectory(srcFile, destDir, preserveFileDate);
    }

    public static void copyURLToFile(URL source, File destination) throws IOException {
        FileUtils.copyURLToFile(source, destination);
    }

    public static void deleteDirectory(File directory) throws IOException {
        FileUtils.deleteDirectory(directory);
    }

    public static boolean deleteQuietly(File file) {
        return FileUtils.deleteQuietly(file);
    }

    public static void forceDelete(File file) throws IOException {
        FileUtils.forceDelete(file);
    }

    public static void forceDeleteOnExit(File file) throws IOException {
        FileUtils.forceDeleteOnExit(file);
    }

    public static void forceMkdir(File directory) throws IOException {
        FileUtils.forceMkdir(directory);
    }

    public static boolean isFileNewer(File file, Date date) {
        return FileUtils.isFileNewer(file, date);
    }

    public static boolean isFileNewer(File file, File reference) {
        return FileUtils.isFileNewer(file, reference);
    }

    public static boolean isFileNewer(File file, long timeMillis) {
        return FileUtils.isFileNewer(file, timeMillis);
    }

    public static boolean isFileOlder(File file, Date date) {
        return FileUtils.isFileOlder(file, date);
    }

    public static boolean isFileOlder(File file, File reference) {
        return FileUtils.isFileOlder(file, reference);
    }

    public static boolean isFileOlder(File file, long timeMillis) {
        return FileUtils.isFileOlder(file, timeMillis);
    }

    public static void moveDirectory(File srcDir, File destDir) throws IOException {
        FileUtils.moveDirectory(srcDir, destDir);
    }

    public static void moveDirectoryToDirectory(File src, File destDir, boolean createDestDir) throws IOException {
        FileUtils.moveDirectoryToDirectory(src, destDir, createDestDir);
    }

    public static void moveFile(File srcFile, File destFile) throws IOException {
        FileUtils.moveFile(srcFile, destFile);
    }

    public static void moveFileToDirectory(File srcFile, File destDir, boolean createDestDir) throws IOException {
        FileUtils.moveFileToDirectory(srcFile, destDir, createDestDir);
    }

    public static void moveToDirectory(File src, File destDir, boolean createDestDir) throws IOException {
        FileUtils.moveToDirectory(src, destDir, createDestDir);
    }

    public static FileInputStream openInputStream(File file) throws IOException {
        return FileUtils.openInputStream(file);
    }

    public static FileOutputStream openOutputStream(File file) throws IOException {
        return FileUtils.openOutputStream(file);
    }

    public static byte[] readFileToByteArray(File file) throws IOException {
        return FileUtils.readFileToByteArray(file);
    }

    public static long sizeOfDirectory(File directory) {
        return FileUtils.sizeOfDirectory(directory);
    }

    public static File toFile(URL url) {
        return FileUtils.toFile(url);
    }

    public static File[] toFiles(URL[] urls) {
        return FileUtils.toFiles(urls);
    }

    public static void touch(File file) throws IOException {
        FileUtils.touch(file);
    }

    public static URL[] toURLs(File[] files) throws IOException {
        return FileUtils.toURLs(files);
    }

    public static boolean waitFor(File file, int seconds) {
        return FileUtils.waitFor(file, seconds);
    }

    public static void writeByteArrayToFile(File file, byte[] data) throws IOException {
        FileUtils.writeByteArrayToFile(file, data);
    }

    public static String concat(String basePath, String fullFilenameToAdd) {
        return FilenameUtils.concat(basePath, fullFilenameToAdd);
    }

    public static boolean equals(String filename1, String filename2) {
        return FilenameUtils.equals(filename1, filename2);
    }

    public static boolean equals(String filename1, String filename2, boolean normalized, IOCase caseSensitivity) {
        return FilenameUtils.equals(filename1, filename2, normalized, caseSensitivity);
    }

    public static boolean equalsNormalized(String filename1, String filename2) {
        return FilenameUtils.equalsNormalized(filename1, filename2);
    }

    public static boolean equalsNormalizedOnSystem(String filename1, String filename2) {
        return FilenameUtils.equalsNormalizedOnSystem(filename1, filename2);
    }

    public static boolean equalsOnSystem(String filename1, String filename2) {
        return FilenameUtils.equalsOnSystem(filename1, filename2);
    }

    public static String getBaseName(String filename) {
        return FilenameUtils.getBaseName(filename);
    }

    public static String getExtension(String filename) {
        return FilenameUtils.getExtension(filename);
    }

    public static String getFullPath(String filename) {
        return FilenameUtils.getFullPath(filename);
    }

    public static String getFullPathNoEndSeparator(String filename) {
        return FilenameUtils.getFullPathNoEndSeparator(filename);
    }

    public static String getName(String filename) {
        return FilenameUtils.getName(filename);
    }

    public static String getPath(String filename) {
        return FilenameUtils.getPath(filename);
    }

    public static String getPathNoEndSeparator(String filename) {
        return FilenameUtils.getPathNoEndSeparator(filename);
    }

    public static String getPrefix(String filename) {
        return FilenameUtils.getPrefix(filename);
    }

    public static int getPrefixLength(String filename) {
        return FilenameUtils.getPrefixLength(filename);
    }

    public static int indexOfExtension(String filename) {
        return FilenameUtils.indexOfExtension(filename);
    }

    public static int indexOfLastSeparator(String filename) {
        return FilenameUtils.indexOfLastSeparator(filename);
    }

    public static boolean isExtension(String filename, String extension) {
        return FilenameUtils.isExtension(filename, extension);
    }

    public static boolean isExtension(String filename, Collection extensions) {
        return FilenameUtils.isExtension(filename, extensions);
    }

    public static boolean isExtension(String filename, String[] extensions) {
        return FilenameUtils.isExtension(filename, extensions);
    }

    public static String normalize(String filename) {
        return FilenameUtils.normalize(filename);
    }

    public static String normalizeNoEndSeparator(String filename) {
        return FilenameUtils.normalizeNoEndSeparator(filename);
    }

    public static String removeExtension(String filename) {
        return FilenameUtils.removeExtension(filename);
    }

    public static String separatorsToSystem(String path) {
        return FilenameUtils.separatorsToSystem(path);
    }

    public static String separatorsToUnix(String path) {
        return FilenameUtils.separatorsToUnix(path);
    }

    public static String separatorsToWindows(String path) {
        return FilenameUtils.separatorsToWindows(path);
    }

    public static boolean wildcardMatch(String filename, String wildcardMatcher) {
        return FilenameUtils.wildcardMatch(filename, wildcardMatcher);
    }

    public static boolean wildcardMatch(String filename, String wildcardMatcher, IOCase caseSensitivity) {
        return FilenameUtils.wildcardMatch(filename, wildcardMatcher, caseSensitivity);
    }

    public static boolean wildcardMatchOnSystem(String filename, String wildcardMatcher) {
        return FilenameUtils.wildcardMatchOnSystem(filename, wildcardMatcher);
    }
}
