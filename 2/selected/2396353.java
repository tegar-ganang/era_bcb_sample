package com.xiledsystems.AlternateJavaBridgelib.components.altbridge.util;

import android.os.Environment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import com.xiledsystems.AlternateJavaBridgelib.components.util.ErrorMessages;

/**
 * Utilities for reading and writing files to the external storage.
 *
 */
public class FileUtil {

    private static final String DOCUMENT_DIRECTORY = "My Documents/";

    private static final String DIRECTORY_RECORDINGS = "Recordings";

    private static final String FILENAME_PREFIX = "app_inventor_";

    private static final String DIRECTORY_PICTURES = "Pictures";

    private static final String DIRECTORY_DOWNLOADS = "Downloads";

    private FileUtil() {
    }

    /**
   * Returns an URL for the given local file.
   */
    public static String getFileUrl(String localFileName) {
        File file = new File(localFileName);
        return file.toURI().toString();
    }

    /**
   * Reads the given local file and returns the contents as a byte array.
   *
   * @param inputFileName the name of the file to read from
   * @return the file's contents as a byte array
   */
    public static byte[] readFile(String inputFileName) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = new FileInputStream(inputFileName);
        try {
            copy(in, out);
        } finally {
            in.close();
        }
        return out.toByteArray();
    }

    /**
   * Downloads the resource with the given URL and writes it as a local file.
   *
   * @param url the URL to read from
   * @param outputFileName the name of the file to write to
   * @return the URL for the local file
   */
    public static String downloadUrlToFile(String url, String outputFileName) throws IOException {
        InputStream in = new URL(url).openStream();
        try {
            return writeStreamToFile(in, outputFileName);
        } finally {
            in.close();
        }
    }

    /**
   * Writes the given byte array as a local file.
   *
   * @param array the byte array to read from
   * @param outputFileName the name of the file to write to
   * @return the URL for the local file
   */
    public static String writeFile(byte[] array, String outputFileName) throws IOException {
        InputStream in = new ByteArrayInputStream(array);
        try {
            return writeStreamToFile(in, outputFileName);
        } finally {
            in.close();
        }
    }

    /**
   * Copies the contents of one local file to another local file.
   *
   * @param inputFileName the name of the file to read to
   * @param outputFileName the name of the file to write to
   * @return the URL for the local file
   */
    public static String copyFile(String inputFileName, String outputFileName) throws IOException {
        InputStream in = new FileInputStream(inputFileName);
        try {
            return writeStreamToFile(in, outputFileName);
        } finally {
            in.close();
        }
    }

    /**
   * Writes the contents from the given input stream to the given file.
   *
   * @param in the InputStream to read from
   * @param outputFileName the name of the file to write to
   * @return the URL for the local file
   */
    public static String writeStreamToFile(InputStream in, String outputFileName) throws IOException {
        File file = new File(outputFileName);
        file.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(file);
        try {
            copy(in, out);
            return file.toURI().toString();
        } finally {
            out.flush();
            out.close();
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        out = new BufferedOutputStream(out, 0x1000);
        in = new BufferedInputStream(in, 0x1000);
        while (true) {
            int b = in.read();
            if (b == -1) {
                break;
            }
            out.write(b);
        }
        out.flush();
    }

    /**
   * Creates a {@link File} representing the complete path for an image
   * file, creating the enclosing directories if needed.  This does not actually
   * open the file.  Any component that calls this must have
   * {@link android.Manifest.permission.WRITE_EXTERNAL_STORAGE}.
   *
   * @param extension file extension, such as "png" or "jpg"
   * @return the path to the file
   * @throws IOException if the enclosing directory cannot be created
   * @throws FileException if external storage is not accessible or not writable
   *   with the appropriate ErrorMessages error code.
   */
    public static File getPictureFile(String extension) throws IOException, FileException {
        return getFile(DIRECTORY_PICTURES, extension);
    }

    /**
   * Creates a {@link File} representing the complete path for a recording,
   * creating the enclosing directories if needed.  This does not actually
   * open the file.  Any component that calls this must have
   * {@link android.Manifest.permission.WRITE_EXTERNAL_STORAGE}.
   *
   * @return the path to the file
   * @param extension file extension, such as "3gp"
   * @throws IOException if the enclosing directory cannot be created
   * @throws FileException if external storage is not accessible or not writable
   *   with the appropriate ErrorMessages error code.
   */
    public static File getRecordingFile(String extension) throws IOException, FileException {
        return getFile(DIRECTORY_RECORDINGS, extension);
    }

    /**
   * Creates a {@link File} representing the complete path for a downloaded file,
   * creating the enclosing directories if needed.  This does not actually
   * open the file.  Any component that calls this must have
   * {@link android.Manifest.permission.WRITE_EXTERNAL_STORAGE}.
   *
   * @return the path to the file
   * @param extension file extension, such as "tmp"
   * @throws IOException if the enclosing directory cannot be created
   * @throws FileException if external storage is not accessible or not writable
   *   with the appropriate ErrorMessages error code.
   */
    public static File getDownloadFile(String extension) throws IOException, FileException {
        return getFile(DIRECTORY_DOWNLOADS, extension);
    }

    /**
   * Determines the best directory in which to store a file of the given type
   * and creates the directory if it does not exist, generating a full path.
   *
   * @param category a descriptive category, such as {@link DIRECTORY_PICTURES}
   *        to include in the path
   * @param extension the extension for the end of the file, not including the
   *        period, such as "png"
   * @return the full path to the file
   * @throws IOException if the directory cannot be created
   */
    private static File getFile(String category, String extension) throws IOException, FileException {
        String fileName = DOCUMENT_DIRECTORY + category + "/" + FILENAME_PREFIX + System.currentTimeMillis() + "." + extension;
        return getExternalFile(fileName);
    }

    /**
   * Returns the File for fileName in the external storage directory in
   * preparation for writing the file. fileName may contain sub-directories.
   * Ensures that all subdirectories exist and that fileName does not exist
   * (deleting it if necessary).
   * @param fileName The path name of the file relative to the external storage
   *   directory
   * @return the File object for creating fileName in the external storage
   * @throws IOException if we are unable to create necessary parent directories
   *   or delete an existing file
   * @throws FileException if the external storage is not writeable.
   */
    public static File getExternalFile(String fileName) throws IOException, FileException {
        checkExternalStorageWriteable();
        File file = new File(Environment.getExternalStorageDirectory(), fileName);
        File directory = file.getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create directory " + directory.getAbsolutePath());
        }
        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("Cannot overwrite existing file " + file.getAbsolutePath());
            }
        }
        return file;
    }

    private static void checkExternalStorageWriteable() throws FileException {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return;
        }
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            throw new FileException(ErrorMessages.ERROR_MEDIA_EXTERNAL_STORAGE_READONLY);
        } else {
            throw new FileException(ErrorMessages.ERROR_MEDIA_EXTERNAL_STORAGE_NOT_AVAILABLE);
        }
    }

    /**
   * Exception class for reporting back media-related error numbers from
   * ErrorMessages, which the caller can in turn pass to
   * Form.dispatchErrorOccurredEvent if needed.
   */
    public static class FileException extends RuntimeException {

        private final int msgNumber;

        public FileException(int errorMsgNumber) {
            msgNumber = errorMsgNumber;
        }

        public int getErrorMessageNumber() {
            return msgNumber;
        }
    }
}
