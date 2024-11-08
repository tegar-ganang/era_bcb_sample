package com.entelience.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public abstract class FileHelper {

    private static org.apache.log4j.Logger _logger = com.entelience.util.Logs.getLogger();

    public static boolean fileExists(String fileName) {
        return (new File(fileName)).exists();
    }

    public static String asString(String filename) throws Exception {
        FileInputStream fis = new FileInputStream(filename);
        try {
            byte buffer[] = new byte[com.entelience.util.StaticConfig.ioBufferSize];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int i = 0;
            while ((i = fis.read(buffer, 0, buffer.length)) != 0) {
                if (i > 0) {
                    baos.write(buffer, 0, i);
                } else {
                    return baos.toString();
                }
            }
        } finally {
            fis.close();
        }
        return null;
    }

    /**
     * TRIVIA:
     * -------
     * java.util.File#renameTo has that fun operating system dependent behaviour;
     * here we want to copy the file if we renaming the file can't be successfully
     * moved.  on unix it renames if the destination is on the same disk (/dev/wd0a vs /dev/wd0d etc. etc.)
     * on windows only if the files are in the same directory...  so if renameTo
     * returns false (couldn't rename the file) we decide to copy it instead.
     */
    public static final boolean moveTo(String fromPath, String toPath) {
        try {
            File from = new File(fromPath);
            File to = new File(toPath);
            if (!(from.exists() && from.isFile())) {
                _logger.debug(fromPath + " does not exist / is not a regular file");
                return false;
            }
            if (to.exists()) {
                _logger.debug(toPath + " (target) already exists");
                return false;
            }
            _logger.debug("Moving [" + fromPath + "] to [" + toPath + "]");
            if (from.renameTo(to)) {
                return true;
            } else {
                FileInputStream fis = null;
                try {
                    _logger.debug("No, instead copying [" + fromPath + "] to [" + toPath + "]");
                    fis = new FileInputStream(from);
                    storeInputStream(fis, toPath);
                    try {
                        from.delete();
                    } catch (Exception e) {
                        _logger.warn("Couldn't delete after copying [" + fromPath + "] please remove manually", e);
                    }
                    return true;
                } finally {
                    if (fis != null) fis.close();
                }
            }
        } catch (Exception e) {
            _logger.warn("While moving [" + fromPath + "] to [" + toPath + "] : " + e.getMessage(), e);
        }
        return false;
    }

    public static final boolean copyTo(String fromPath, String toPath) {
        try {
            File from = new File(fromPath);
            File to = new File(toPath);
            if (!(from.exists() && from.isFile())) {
                _logger.warn(fromPath + " does not exist / is not a regular file");
                return false;
            }
            if (to.exists()) {
                _logger.warn(toPath + " (target) already exists");
                return false;
            }
            _logger.debug("Copying [" + fromPath + "] to [" + toPath + "]");
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(from);
                File copyfis = storeInputStream(fis, toPath);
                return (copyfis != null);
            } finally {
                if (fis != null) fis.close();
            }
        } catch (Exception e) {
            _logger.error("While copying [" + fromPath + "] to [" + toPath + "] : " + e.getMessage(), e);
        }
        return false;
    }

    public static final File storeInputStream(InputStream is, String toPath, String tempPath) throws Exception {
        byte buffer[] = new byte[com.entelience.util.StaticConfig.ioBufferSize];
        TempFileOutputStream tfos = viaTempFileOutputStream(toPath, tempPath);
        try {
            int i = 0;
            while ((i = is.read(buffer, 0, buffer.length)) != 0) {
                if (i > 0) tfos.fos.write(buffer, 0, i); else {
                    return successTempFileOutputStream(tfos, toPath);
                }
            }
        } finally {
            finallyTempFileOutputStream(tfos);
        }
        throw new Exception("InputStream.read returned 0");
    }

    /**
     * Store a stream as file using a temp file. Note that the parent path is used to store the
     * temp file.
     */
    public static final File storeInputStream(InputStream is, String toPath) throws Exception {
        return storeInputStream(is, toPath, null);
    }

    /**
     * Simple tuple containing file object (a temporary file, always)
     * and associated FileOutputStream.
     */
    public static class TempFileOutputStream {

        public File file;

        public OutputStream fos;
    }

    /**
     * Where an API insists on writing to an OutputStream (rather than providing us
     * data as an InputStream) we use this which writes to a temp file; then
     * the caller is responsible to finish this.
     *
     * The aim is always to store a file in its final filename ONLY IF THE WHOLE DOWNLOAD WORKS.
     */
    public static final TempFileOutputStream viaTempFileOutputStream(String toPath, String tempPath) throws Exception {
        File to = new File(toPath);
        if (to.exists()) throw new Exception("Unable to write to file [" + toPath + "] already exists.");
        File temp = (tempPath == null ? to.getParentFile() : new File(tempPath));
        if (!temp.exists()) throw new Exception("The directory (" + temp.getAbsolutePath() + ") doesn't exists");
        if (!temp.isDirectory()) throw new Exception("The path (" + temp.getAbsolutePath() + ") doesn't point to a directory");
        File tempFile = null;
        try {
            tempFile = File.createTempFile("tmp.esis.", ".bin", temp);
            tempFile.deleteOnExit();
        } catch (IOException ioe) {
            _logger.error("Failed to create temp file with prefix (tmp.esis.) and suffix (.bin) in directory (" + temp.getAbsolutePath() + ")");
            throw ioe;
        }
        FileOutputStream fos = new FileOutputStream(tempFile);
        TempFileOutputStream tfos = new TempFileOutputStream();
        tfos.file = tempFile;
        tfos.fos = fos;
        return tfos;
    }

    public static final TempFileOutputStream viaTempFileOutputStream(String toPath) throws Exception {
        return viaTempFileOutputStream(toPath, null);
    }

    /**
     * Finish up anything created by viaTempFileOutputStream.  Only call this on success.
     */
    public static final File successTempFileOutputStream(TempFileOutputStream tfos, String toPath) throws Exception {
        tfos.fos.close();
        tfos.fos = null;
        File newFile = new File(toPath);
        if (newFile.exists()) {
            throw new Exception("Unable to write to file [" + toPath + "] already exists.");
        }
        if (!tfos.file.renameTo(newFile)) {
            throw new Exception("Unable to rename " + tfos.file.getAbsolutePath() + " to " + toPath);
        }
        return newFile;
    }

    /**
     * Call this from a finally routine.
     */
    public static final void finallyTempFileOutputStream(TempFileOutputStream tfos) {
        try {
            if (tfos.fos != null) {
                tfos.fos.close();
                tfos.fos = null;
            }
        } catch (Exception e) {
            _logger.debug("Ignoring exception " + e);
        }
        try {
            if (tfos.file != null && tfos.file.exists()) {
                tfos.file.delete();
            }
        } catch (Exception e) {
            _logger.debug("Ignoring exception " + e);
        }
    }

    /**
	 * Dump a string into a file
	 */
    public static final boolean stringToFile(String fileName, String content) {
        File file = new File(fileName);
        try {
            if (!file.createNewFile()) {
                _logger.warn("Unable to create the file (" + fileName + ")");
                return false;
            }
            FileWriter fw = new FileWriter(file);
            BufferedWriter buffer = new BufferedWriter(fw);
            buffer.write(content);
            buffer.close();
            fw.close();
        } catch (Exception e) {
            _logger.error("Failed to generate file (" + fileName + ")", e);
            return false;
        }
        return true;
    }

    /**
     * Clear all files in a directory
     */
    public static final boolean deleteAll(String directoryName) throws Exception {
        File directory = new File(directoryName);
        File[] files = directory.listFiles();
        for (File file : files) {
            if (!file.delete()) {
                return false;
            }
        }
        return true;
    }
}
