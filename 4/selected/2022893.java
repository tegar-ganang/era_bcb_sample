package net.emotivecloud.vrmm.vtm.disk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Copy files taking into account if is it possible or not.
 * Based on FileUtils.
 * @author goirix
 */
class ProgressCopy extends FileUtils {

    private static final long FIFTY_MB = FileUtils.ONE_MB * 50;

    /**
	 * Copy a file using an intermediate file.
	 * @param srcFile
	 * @param destFile
	 * @throws IOException
	 */
    public static void copyFileWithTemp(File srcFile, File destFile) throws IOException {
        File tmp = new File(destFile.getAbsolutePath() + ".part");
        copyFile(srcFile, tmp, false);
        moveFile(tmp, destFile);
    }

    public static void copyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
        if (srcFile == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destFile == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (srcFile.exists() == false) {
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        }
        if (srcFile.isDirectory()) {
            throw new IOException("Source '" + srcFile + "' exists but is a directory");
        }
        if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
            throw new IOException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
        }
        if (destFile.getParentFile() != null && destFile.getParentFile().exists() == false) {
            if (destFile.getParentFile().mkdirs() == false) {
                throw new IOException("Destination '" + destFile + "' directory cannot be created");
            }
        }
        if (destFile.exists() && destFile.canWrite() == false) {
            throw new IOException("Destination '" + destFile + "' exists but is read-only");
        }
        doCopyFile(srcFile, destFile, preserveFileDate);
    }

    private static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
        if (destFile.exists() && destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' exists but is a directory");
        }
        long time = System.currentTimeMillis();
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel input = null;
        FileChannel output = null;
        try {
            fis = new FileInputStream(srcFile);
            fos = new FileOutputStream(destFile);
            input = fis.getChannel();
            output = fos.getChannel();
            long size = input.size();
            long pos = 0;
            long count = 0;
            while (pos < size && continueWriting(pos, size)) {
                count = (size - pos) > FIFTY_MB ? FIFTY_MB : (size - pos);
                pos += output.transferFrom(input, pos, count);
            }
        } finally {
            output.close();
            IOUtils.closeQuietly(fos);
            input.close();
            IOUtils.closeQuietly(fis);
        }
        if (srcFile.length() != destFile.length()) {
            if (DiskManager.isLocked()) throw new IOException("Copy stopped since VtM was working"); else throw new IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile + "'");
        } else {
            time = System.currentTimeMillis() - time;
            long speed = (destFile.length() / time) / 1000;
            DiskManager.addDiskSpeed(speed);
        }
        if (preserveFileDate) {
            destFile.setLastModified(srcFile.lastModified());
        }
    }

    /**
	 * Checks if is possible to write and if the progress is bigger enough to continue.
	 * @param pos Number of bytes already written.
	 * @param size Total number of bytes.
	 * @return
	 */
    private static boolean continueWriting(long pos, long size) {
        boolean ret = true;
        if (DiskManager.isLocked()) if (pos < 0.9 * size && ((size - pos) > 2 * FIFTY_MB)) ret = false;
        return ret;
    }
}
