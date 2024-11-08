package org.paccman.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author joao
 */
public class FileUtils {

    private static final int BUF_SIZE = 256 * 1024;

    /**
     * 
     * @param is
     * @param os
     * @throws java.io.IOException
     */
    public static void copyFile(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[BUF_SIZE];
        int len;
        while ((len = is.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
        is.close();
        os.close();
    }

    /**
     * Copy <code>inputFile</code> to <code>outputFile</code>
     * @param inputFile The source file.
     * @param outputFile The destination file.
     * @throws java.io.IOException
     */
    public static void copyFile(File inputFile, File outputFile) throws IOException {
        FileChannel srcChannel = new FileInputStream(inputFile).getChannel();
        FileChannel dstChannel = new FileOutputStream(outputFile).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
    }

    /**
     * Zip the content of the specified directory to the specified zip file.
     * @param srcDir The directory containing the files to zip.
     * @param destFile The zip file to be created.
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException 
     */
    public static void zipDirectory(File srcDir, File destFile) throws FileNotFoundException, IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destFile));
        assert srcDir.isDirectory();
        File rootDir = srcDir.getAbsoluteFile();
        for (File f : srcDir.listFiles()) {
            addFileToZip(zos, f, rootDir);
        }
        zos.close();
    }

    private static void addFileToZip(ZipOutputStream zos, File file, File rootDir) throws IOException {
        if (file.isFile()) {
            File relativeFile = new File(file.getAbsolutePath().substring(rootDir.getPath().length() + 1));
            ZipEntry ze = new ZipEntry(relativeFile.getPath());
            zos.putNextEntry(ze);
            int count;
            byte data[] = new byte[BUF_SIZE];
            BufferedInputStream origin = new BufferedInputStream(new FileInputStream(file));
            while ((count = origin.read(data, 0, BUF_SIZE)) != -1) {
                zos.write(data, 0, count);
            }
            origin.close();
        } else {
            for (File f : file.listFiles()) {
                addFileToZip(zos, f, rootDir);
            }
        }
    }

    /**
     * Unzip the content of the specified zip into the specified directory. 
     * The destination directory must exists.
     * @param srcZip The zip file.
     * @param destDir The directory to which unzip the file.
     * @throws java.io.IOException 
     */
    public static void unzipDirectory(File srcZip, File destDir) throws IOException {
        assert destDir.isDirectory();
        String rootDir = destDir.getAbsolutePath();
        ZipInputStream zis = new ZipInputStream(new FileInputStream(srcZip));
        ZipEntry ze;
        while ((ze = zis.getNextEntry()) != null) {
            final File file = new File(ze.getName());
            String fullPath;
            if (file.getParent() != null) {
                fullPath = rootDir + File.separator + file.getParent();
            } else {
                fullPath = rootDir;
            }
            new File(fullPath).mkdirs();
            String fileName = new File(ze.getName()).getName();
            extractFile(zis, fullPath, fileName);
        }
        zis.close();
    }

    private static void extractFile(ZipInputStream zin, String fullPath, String fileName) throws FileNotFoundException, IOException {
        FileOutputStream fos = new FileOutputStream(fullPath + File.separator + fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        int count;
        byte data[] = new byte[BUF_SIZE];
        while ((count = zin.read(data, 0, BUF_SIZE)) != -1) {
            bos.write(data, 0, count);
        }
        bos.close();
    }

    /**
     * Construct a string based on the current date and time. It is used to 
     * build "unique" file names.
     * @return A string formatted as <code>YYYYMMDDhhmmss<code>
     */
    public static synchronized String getTimeString() {
        return String.format("%1$tY%1$tm%1$tH%1$tM%1$tS", Calendar.getInstance());
    }

    /**
     * Recursively delete directory.
     * @param dir The directory to be deleted.
     * @throws java.io.IOException 
     */
    public static void deleteDir(File dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(dir + " is not a directory.");
        }
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                deleteDir(f);
            } else {
                if (!f.delete()) {
                    throw new IOException("Unable to delete file: " + f.getAbsolutePath());
                }
            }
        }
        dir.delete();
    }

    private FileUtils() {
    }
}
