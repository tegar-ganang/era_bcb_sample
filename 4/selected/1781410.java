package net.sf.fc;

import net.sf.fc.io.event.FileCopyEvent.Level;
import net.sf.fc.io.event.FileCopyEventListener;
import net.sf.fc.io.event.ConsoleFileCopyEventListener;
import org.junit.Ignore;
import static org.junit.Assert.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.Closeable;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

/**
 *
 * @author david
 */
public class TestUtil {

    private static File testDir;

    private static File srcDir;

    private static File dstDir;

    private static FileCopyEventListener listener;

    @Ignore
    public static void createTestDirectory() {
        testDir = (new File("unittest-files/")).getAbsoluteFile();
        testDir.mkdirs();
        srcDir = new File(testDir, "srcDir");
        srcDir.mkdirs();
        dstDir = new File(testDir, "dstDir");
        dstDir.mkdirs();
    }

    @Ignore
    public static void createFileCopyEventListener() {
        listener = new ConsoleFileCopyEventListener(Level.INFO);
    }

    @Ignore
    public static FileCopyEventListener getFileCopyEventListener() {
        return listener;
    }

    @Ignore
    public static List<File> createLvlDir(File parentDir, int lvl) {
        return createLvlDir(parentDir, lvl, new ArrayList<File>());
    }

    @Ignore
    public static List<File> createLvlDir(File parentDir, int lvl, List<File> dirs) {
        return createLvlDir(parentDir, lvl, true, dirs);
    }

    @Ignore
    public static List<File> createLvlDir(File parentDir, int lvl, boolean create) {
        return createLvlDir(parentDir, lvl, create, new ArrayList<File>());
    }

    @Ignore
    public static List<File> createLvlDir(File parentDir, int lvl, boolean create, List<File> dirs) {
        File dir = null;
        if (lvl > 0) {
            dir = new File(parentDir, "lvl" + lvl + "Dir");
            createLvlDir(dir, lvl - 1, dirs);
        }
        if (lvl == 1 && create) dir.mkdirs();
        if (dir != null) dirs.add(dir);
        return dirs;
    }

    @Ignore
    public static List<File> createFlatDirs(File parentDir, int start, int ct) {
        return createFlatDirs(parentDir, "flat", start, ct, true);
    }

    @Ignore
    public static List<File> createFlatDirs(File parentDir, String baseNm, int start, int ct) {
        return createFlatDirs(parentDir, baseNm, start, ct, true);
    }

    @Ignore
    public static List<File> createFlatDirs(File parentDir, int start, int ct, boolean create) {
        return createFlatDirs(parentDir, "flat", start, ct, create);
    }

    @Ignore
    public static List<File> createFlatDirs(File parentDir, String baseNm, int start, int ct, boolean create) {
        List<File> dirs = new ArrayList<File>();
        for (int i = start; i <= ct + (start - 1); i++) {
            File dir = new File(parentDir, baseNm + i + "Dir");
            if (create) dir.mkdirs();
            dirs.add(dir);
        }
        return dirs;
    }

    @Ignore
    public static void deleteTestDirectory() {
        deleteTestFiles(testDir);
        testDir.delete();
    }

    @Ignore
    public static void deleteTestFiles(File dir) {
        assertTrue(ensureDirIsTest(dir));
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                deleteTestFiles(f);
            }
            f.delete();
        }
    }

    public static boolean ensureDirIsTest(File dir) {
        if (dir.isDirectory()) {
            File parent = dir;
            while (parent != null) {
                if (parent.equals(testDir)) {
                    return true;
                }
                parent = parent.getParentFile();
            }
        }
        return false;
    }

    @Ignore
    public static void deleteTestFiles() {
        deleteTestFiles(getSrcDirectory());
        deleteTestFiles(getDstDirectory());
    }

    @Ignore
    public static File getTestDirectory() {
        return testDir;
    }

    @Ignore
    public static File getSrcDirectory() {
        return srcDir;
    }

    @Ignore
    public static File getDstDirectory() {
        return dstDir;
    }

    @Ignore
    public static File createFile(File file, long size) throws IOException {
        return createFile(file, size, false);
    }

    @Ignore
    public static File createFile(File file, long size, boolean changeLastModified) throws IOException {
        if (!file.getParentFile().exists()) {
            throw new IOException("Cannot create file " + file + " as the parent directory does not exist");
        }
        BufferedOutputStream output = new BufferedOutputStream(new java.io.FileOutputStream(file));
        try {
            generateTestData(output, size);
        } finally {
            closeQuietly(output);
        }
        if (changeLastModified) {
            file.setLastModified(file.lastModified() - 1000);
        }
        return file;
    }

    @Ignore
    public static File createSrcFile(String fileNm, long size) throws IOException {
        return createFile(new File(getSrcDirectory(), fileNm), size, true);
    }

    @Ignore
    public static List<File> createFiles(File dir, int count) throws IOException {
        return createFiles(dir, 1, count, false);
    }

    @Ignore
    public static List<File> createFiles(File dir, String baseFileNm, int count) throws IOException {
        return createFiles(dir, baseFileNm, 1, count, false);
    }

    @Ignore
    public static List<File> createFiles(File dir, int start, int count) throws IOException {
        return createFiles(dir, start, count, false);
    }

    @Ignore
    public static List<File> createFiles(File dir, int count, boolean changeLastModified) throws IOException {
        return createFiles(dir, 1, count, changeLastModified);
    }

    @Ignore
    public static List<File> createFiles(File dir, String baseFileNm, int count, boolean changeLastModified) throws IOException {
        return createFiles(dir, baseFileNm, 1, count, changeLastModified);
    }

    @Ignore
    public static List<File> createFiles(File dir, int start, int count, boolean changeLastModified) throws IOException {
        return createFiles(dir, "file", start, count, changeLastModified);
    }

    @Ignore
    public static List<File> createFiles(File dir, String baseFileNm, int start, int count, boolean changeLastModified) throws IOException {
        List<File> files = new ArrayList<File>();
        java.util.Random random = new java.util.Random();
        for (int i = start; i <= count + (start - 1); i++) {
            files.add(createFile(new File(dir, baseFileNm + i), Math.max(512, random.nextInt(4098)), changeLastModified));
        }
        return files;
    }

    @Ignore
    public static List<File> createFileObjects(File dir, int count) {
        return createFileObjects(dir, "file", 1, count);
    }

    @Ignore
    public static List<File> createFileObjects(File dir, int start, int count) {
        return createFileObjects(dir, "file", start, count);
    }

    @Ignore
    public static List<File> createFileObjects(File dir, String baseFileNm, int count) {
        return createFileObjects(dir, baseFileNm, 1, count);
    }

    @Ignore
    public static List<File> createFileObjects(File dir, String baseFileNm, int start, int count) {
        List<File> files = new ArrayList<File>();
        for (int i = start; i <= count + (start - 1); i++) {
            files.add(new File(dir, baseFileNm + i));
        }
        return files;
    }

    @Ignore
    public static List<File> createSrcFiles(int count) throws IOException {
        return createSrcFiles(1, count);
    }

    @Ignore
    public static List<File> createSrcFiles(int start, int count) throws IOException {
        return createFiles(getSrcDirectory(), start, count, true);
    }

    @Ignore
    public static List<File> createSrcFiles(String baseFileNm, int count) throws IOException {
        return createSrcFiles(baseFileNm, 1, count);
    }

    @Ignore
    public static List<File> createSrcFiles(String baseFileNm, int start, int count) throws IOException {
        return createFiles(getSrcDirectory(), baseFileNm, start, count, true);
    }

    @Ignore
    public static String getFileContents(File file) throws IOException {
        FileInputStream fis = null;
        FileChannel input = null;
        StringBuilder sBuilder = new StringBuilder();
        try {
            fis = new FileInputStream(file);
            input = fis.getChannel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(64);
            int bytesRead = input.read(byteBuffer);
            while (bytesRead != -1) {
                byteBuffer.flip();
                CharBuffer charBuffer = byteBuffer.asCharBuffer();
                sBuilder.append(charBuffer.toString());
                bytesRead = input.read(byteBuffer);
            }
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(fis);
        }
        return sBuilder.toString();
    }

    @Ignore
    public static void assertFiles(boolean preserveFileDate, File srcFile, File... destFiles) throws IOException {
        for (File destFile : destFiles) {
            assertTrue(destFile.exists());
            assertEquals(Files.getAttribute(srcFile.toPath(), "size"), Files.getAttribute(destFile.toPath(), "size"));
            if (srcFile.isFile() && destFile.isFile()) assertEquals(getFileContents(srcFile), getFileContents(destFile));
            if (preserveFileDate == true) {
                assertEquals(srcFile.lastModified(), destFile.lastModified());
            } else {
                assertTrue(srcFile.lastModified() != destFile.lastModified());
            }
        }
    }

    @Ignore
    public static File[] getNewNameFiles(File f, String appendStr, String sep, int renameCt) {
        return (f.getParentFile().listFiles((FilenameFilter) new RegexFileFilter("^" + f.getName() + appendStr + "$")));
    }

    @Ignore
    public static void assertRenamedFiles(boolean preserveFileDate, File srcFile, File dstFile, String appendStr, String sep, int renameCt) throws IOException {
        File[] dstFiles = new File[renameCt + 1];
        dstFiles[0] = dstFile;
        for (int i = 1; i <= renameCt; i++) {
            if (i == 1) {
                File[] dFiles = dstFile.getParentFile().listFiles((FilenameFilter) new RegexFileFilter("^" + dstFile.getName() + appendStr + "$"));
                assertEquals(1, dFiles.length);
                dstFiles[i] = dFiles[0];
            } else {
                File[] dFiles = dstFile.getParentFile().listFiles((FilenameFilter) new RegexFileFilter("^" + dstFile.getName() + appendStr + sep + (i - 1) + "$"));
                assertEquals(1, dFiles.length);
                dstFiles[i] = dFiles[0];
            }
        }
        assertFiles(preserveFileDate, srcFile, dstFiles);
    }

    @Ignore
    public static void assertRenamedFilesExist(File srcFile, File dstFile, String appendStr, String sep, int renameCt) throws IOException {
        File[] dstFiles = new File[renameCt];
        for (int i = 0; i < renameCt; i++) {
            if (i == 0) {
                File[] dFiles = dstFile.getParentFile().listFiles((FilenameFilter) new RegexFileFilter("^" + dstFile.getName() + appendStr + "$"));
                assertEquals(1, dFiles.length);
                dstFiles[i] = dFiles[0];
            } else {
                File[] dFiles = dstFile.getParentFile().listFiles((FilenameFilter) new RegexFileFilter("^" + dstFile.getName() + appendStr + sep + (i) + "$"));
                assertEquals(1, dFiles.length);
                dstFiles[i] = dFiles[0];
            }
        }
        assertEquals(dstFiles.length, renameCt);
        for (File dstF : dstFiles) {
            assertTrue(dstF.exists());
        }
    }

    @Ignore
    public static void assertDirectories(File srcDir, File dstDir, boolean preserveFileDate) throws IOException {
        assertDirectories(srcDir, dstDir, preserveFileDate, 1, 0, null, false);
    }

    @Ignore
    public static void assertDirectories(File srcDir, File dstDir, boolean preserveFileDate, Pattern flattenDirPattern, boolean inclusivePattern) throws IOException {
        assertDirectories(srcDir, dstDir, preserveFileDate, 1, 0, flattenDirPattern, inclusivePattern);
    }

    @Ignore
    public static void assertDirectories(File srcDir, File dstDir, boolean preserveFileDate, int maxDirLvl) throws IOException {
        assertDirectories(srcDir, dstDir, preserveFileDate, 1, maxDirLvl, null, false);
    }

    @Ignore
    public static void assertDirectories(File srcDir, File dstDir, boolean preserveFileDate, int maxDirLvl, Pattern flattenDirPattern, boolean inclusivePattern) throws IOException {
        assertDirectories(srcDir, dstDir, preserveFileDate, 1, maxDirLvl, flattenDirPattern, inclusivePattern);
    }

    @Ignore
    public static void assertDirectories(File srcDir, File dstDir, boolean preserveFileDate, int dirLvl, int maxDirLvl) throws IOException {
        assertDirectories(srcDir, dstDir, preserveFileDate, dirLvl, maxDirLvl, null, false);
    }

    @Ignore
    public static void assertDirectories(File srcDir, File dstDir, boolean preserveFileDate, int dirLvl, int maxDirLvl, Pattern flattenDirPattern, boolean inclusivePattern) throws IOException {
        assertTrue(srcDir.isDirectory());
        assertTrue(dstDir.isDirectory());
        File[] srcFiles = srcDir.listFiles();
        for (File srcFile : srcFiles) {
            File[] dFiles = dstDir.listFiles((FilenameFilter) new NameFileFilter(srcFile.getName()));
            if (srcFile.isDirectory()) {
                if (maxDirLvl == dirLvl) continue;
                boolean shouldDirBeFlattened = false;
                if (srcFile.isDirectory() && flattenDirPattern != null) {
                    shouldDirBeFlattened = inclusivePattern ? flattenDirPattern.matcher(srcFile.getName()).matches() : !flattenDirPattern.matcher(srcFile.getName()).matches();
                }
                if (shouldDirBeFlattened) {
                    assertDirectories(srcFile, dstDir, preserveFileDate, dirLvl + 1, maxDirLvl, flattenDirPattern, inclusivePattern);
                } else {
                    assertEquals(1, dFiles.length);
                    assertDirectories(srcFile, dFiles[0], preserveFileDate, dirLvl + 1, maxDirLvl, flattenDirPattern, inclusivePattern);
                }
            } else {
                assertEquals(1, dFiles.length);
                assertFiles(preserveFileDate, srcFile, dFiles[0]);
            }
        }
    }

    @Ignore
    public static void assertFlattenedDirectories(List<File> flattenedSrcDirs, File dstDir, boolean preserveFileDate) throws IOException {
        for (File srcDir : flattenedSrcDirs) {
            assertFlattenedDirectories(srcDir, dstDir, preserveFileDate);
        }
    }

    @Ignore
    public static void assertFlattenedDirectories(File srcDir, File dstDir, boolean preserveFileDate) throws IOException {
        assertTrue(srcDir.isDirectory());
        assertTrue(dstDir.isDirectory());
        File[] srcFiles = srcDir.listFiles((FilenameFilter) FileFileFilter.FILE);
        for (File srcFile : srcFiles) {
            File[] dFiles = dstDir.listFiles((FilenameFilter) new NameFileFilter(srcFile.getName()));
            assertEquals(1, dFiles.length);
            assertFiles(preserveFileDate, srcFile, dFiles[0]);
        }
    }

    @Ignore
    public static void assertDirectories(File srcDir, File dstDir, boolean preserveFileDate, int dirLvl, int maxDirLvl, Pattern mergeDirPattern, boolean inclusivePattern, String appendStr, String sep, int renameCt) throws IOException {
        assertTrue(srcDir.isDirectory());
        assertTrue(dstDir.isDirectory());
        if (maxDirLvl == 0 || maxDirLvl > dirLvl) {
            boolean shouldDirBeMerged = false;
            if (mergeDirPattern != null) {
                shouldDirBeMerged = inclusivePattern ? mergeDirPattern.matcher(srcDir.getName()).matches() : !mergeDirPattern.matcher(srcDir.getName()).matches();
            }
            if (!shouldDirBeMerged) {
                assertRenamedFiles(preserveFileDate, srcDir, dstDir, appendStr, sep, renameCt);
                File[] newNmDirs = getNewNameFiles(srcDir, appendStr, sep, renameCt);
                for (File d : newNmDirs) {
                    assertDirectories(d, dstDir, preserveFileDate, dirLvl + 1, maxDirLvl, mergeDirPattern, inclusivePattern, appendStr, sep, renameCt);
                }
            }
            File[] srcFiles = srcDir.listFiles();
            for (File srcFile : srcFiles) {
                File[] dFiles = dstDir.listFiles((FilenameFilter) new NameFileFilter(srcFile.getName()));
                if (srcFile.isDirectory()) {
                    assertEquals(1, dFiles.length);
                    assertDirectories(srcFile, dFiles[0], preserveFileDate, dirLvl + 1, maxDirLvl, mergeDirPattern, inclusivePattern, appendStr, sep, renameCt);
                } else {
                    assertEquals(1, dFiles.length);
                    if (shouldDirBeMerged) {
                        assertRenamedFiles(preserveFileDate, srcFile, dFiles[0], appendStr, sep, renameCt);
                    } else {
                        assertFiles(preserveFileDate, srcFile, dFiles[0]);
                    }
                }
            }
        }
    }

    @Ignore
    public static void generateTestData(OutputStream out, long size) throws IOException {
        for (int i = 0; i < size; i++) {
            out.write((byte) ((i % 127) + 1));
        }
    }

    @Ignore
    public static byte[] generateTestData(long size) {
        try {
            ByteArrayOutputStream baout = new ByteArrayOutputStream();
            generateTestData(baout, size);
            return baout.toByteArray();
        } catch (IOException ioe) {
            throw new RuntimeException("This should never happen: " + ioe.getMessage());
        }
    }

    @Ignore
    public static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }
}
