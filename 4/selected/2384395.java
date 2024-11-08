package org.sltech.commons.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.ArrayUtils;
import org.sltech.commons.exception.SLException;

/**
 *
 * @author Jorge E. Alvarez P.
 */
public class FSUtils {

    /**
   * Verify if the route is a directory.
   * @param directory
   * @return
   * @throws SLException
   */
    public static boolean isValidDirectory(File directory) throws SLException {
        boolean flag = true;
        if (directory == null) {
            flag = false;
            throw new SLException("The directory can't be null.");
        }
        if (!directory.exists()) {
            flag = false;
            throw new SLException("The directory (" + directory.getAbsolutePath() + ") not exist.");
        }
        if (!directory.isDirectory()) {
            flag = false;
            throw new SLException("The route " + directory.getAbsolutePath() + " not is a directory.");
        }
        return (flag);
    }

    /**
   * Return the files in a directory with the suffix specified.
   * @param directory
   * @param suffix
   * @return
   * @throws SLException
   */
    public static String[] viewFiles(File directory, String suffix) throws SLException {
        String[] files = null;
        if (isValidDirectory(directory)) {
            files = directory.list(new SuffixFileFilter(suffix));
        }
        return (files);
    }

    /**
   * Delete all files in a directory specified.
   * @param directory
   * @throws SLException
   */
    public static void deleteDirectory(File directory) throws SLException {
        try {
            if (isValidDirectory(directory)) {
                FileUtils.cleanDirectory(directory);
            }
        } catch (IOException ex) {
            throw new SLException(ex.getMessage(), ex);
        }
    }

    /**
   * Delete a file specified.
   * @param file
   * @return
   * @throws SLException
   */
    public static boolean deleteFile(File file) throws SLException {
        boolean flag = false;
        flag = file.delete();
        if (flag == false) throw new SLException("Error, the file [" + file.getName() + "] can't be deleted.");
        return flag;
    }

    /**
   * Write a String into file.
   * @param filename
   * @param str
   * @throws SLException
   */
    public static void writeFile(String filename, String str) throws SLException {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(filename);
            fileWriter.write(str);
            fileWriter.close();
        } catch (IOException ex) {
            throw new SLException(ex.getMessage(), ex);
        }
    }

    /**
   * Append a text into file
   * @param filename
   * @param str
   * @throws SLException
   */
    public static void appendTextIntoFile(String filename, String str) throws SLException {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(filename, true);
            fileWriter.write(str);
            fileWriter.close();
        } catch (IOException ex) {
            throw new SLException(ex.getMessage(), ex);
        }
    }

    /**
   * Create a file.
   * @param filename
   * @return
   * @throws EMException
   */
    public static FileWriter createFile(String filename) throws SLException {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(filename, true);
            fileWriter.close();
        } catch (IOException ex) {
            throw new SLException(ex.getMessage(), ex);
        }
        return fileWriter;
    }

    /**
   * Write array of bytes into file.
   * @param filename
   * @param arrayOfBytes
   * @throws SLException
   */
    public static void writeFile(String filename, byte[] arrayOfBytes) throws SLException {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(filename);
            fileOutputStream.write(arrayOfBytes);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException ex) {
            throw new SLException(ex.getMessage(), ex);
        }
    }

    /**
   * Copy a file.
   * @param fileOrigen
   * @param fileDestiny
   * @throws IOException
   */
    public static void copyFile(String fileOrigen, String fileDestiny) throws IOException {
        FileUtils.copyFile(new File(fileOrigen), new File(fileDestiny));
    }

    /**
   * Move a file, it can be used like rename.
   * @param fileOrigen
   * @param fileDestiny
   * @throws IOException
   */
    public static void moveFile(String fileOrigen, String fileDestiny) throws IOException {
        File originalFile = new File(fileOrigen);
        FileUtils.copyFile(originalFile, new File(fileDestiny));
        originalFile.delete();
    }

    /**
   * Read a file and convert its content in byte[]
   * @param pathFile
   * @return
   * @throws SLException
   */
    public static byte[] readFileToByteArray(String pathFile) throws SLException {
        File file = null;
        InputStream inputStream = null;
        byte[] data = null;
        int buffer_length = 100;
        int readLength = 0;
        try {
            file = new File(pathFile);
            inputStream = new FileInputStream(file);
            byte[] buffer = new byte[buffer_length];
            while ((readLength = inputStream.read(buffer, 0, buffer_length)) != -1) {
                data = ArrayUtils.addAll(data, ArrayUtils.subarray(buffer, 0, readLength));
            }
            return data;
        } catch (FileNotFoundException e) {
            throw new SLException(e.getMessage(), e);
        } catch (IOException e) {
            throw new SLException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    throw new SLException(ex);
                }
            }
        }
    }

    /**
   * Convert byte[] and write into file.
   * @param pathFile
   * @param data
   * @throws SLException
   */
    public static void writeByteArrayToFile(String pathFile, byte[] data) throws SLException {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(new File(pathFile));
            outputStream.write(data);
            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException ex) {
            throw new SLException(ex);
        } catch (IOException ex) {
            throw new SLException(ex);
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ex) {
                throw new SLException(ex);
            }
        }
    }

    /**
   * Write byte[] to Outputstream.
   * @param data
   * @return
   * @throws SLException
   */
    public static byte[] writeByteArrayToOutputStream(byte[] data) throws SLException {
        java.io.ByteArrayOutputStream outputStream = null;
        try {
            outputStream = new java.io.ByteArrayOutputStream();
            outputStream.write(data);
            outputStream.flush();
            outputStream.close();
            return outputStream.toByteArray();
        } catch (FileNotFoundException ex) {
            throw new SLException(ex);
        } catch (IOException ex) {
            throw new SLException(ex);
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ex) {
                throw new SLException(ex);
            }
        }
    }

    /**
   * Create a directory.
   * @param pathName
   * @return
   */
    public static File createDirectory(String pathName) {
        File directory = new File(pathName);
        directory.mkdir();
        return directory;
    }

    /**
   * Verify if exist a directory.
   * @param pathName
   * @return
   * @throws SLException
   */
    public static boolean verifyExistsDirectory(String pathName) throws SLException {
        boolean flag = false;
        File directory = new File(pathName);
        if (directory.exists()) {
            if (directory.isDirectory()) flag = true; else throw new SLException("The route " + pathName + " exist but not is a directory.");
        }
        return flag;
    }
}
