package net.sourceforge.mazix.components.utils.file;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import static net.sourceforge.mazix.components.constants.CommonConstants.MINUS_ONE_RETURN_CODE;
import static net.sourceforge.mazix.components.constants.log.ErrorConstants.CLOSE_FILE_ERROR;
import static net.sourceforge.mazix.components.constants.log.ErrorConstants.CLOSE_STREAM_ERROR;
import static net.sourceforge.mazix.components.constants.log.ErrorConstants.COPY_FILE_ERROR;
import static net.sourceforge.mazix.components.constants.log.ErrorConstants.READ_FILE_ERROR;
import static net.sourceforge.mazix.components.constants.log.InfoConstants.COPY_FILE_INFO;
import static net.sourceforge.mazix.components.constants.log.InfoConstants.READ_FILE_INFO;
import static net.sourceforge.mazix.components.constants.log.InfoConstants.WRITE_FILE_INFO;
import static net.sourceforge.mazix.components.utils.log.LogUtils.buildLogString;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

/**
 * The class utilities managing files.
 *
 * @author Benjamin Croizet (graffity2199@yahoo.fr)
 * @since 0.6
 * @version 1.0
 */
public abstract class FileUtils {

    /** The class logger. */
    private static final transient Logger LOGGER = getLogger(FileUtils.class.getName());

    /**
     * Properly close a {@link Closeable} object.
     *
     * @param closeable
     *            the {@link Closeable} instance to close, mustn't be <code>null</code>.
     * @since 1.0
     */
    public static void closeCloseable(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException e) {
                LOGGER.log(SEVERE, CLOSE_STREAM_ERROR, e);
            }
        }
    }

    /**
     * Properly close a {@link Closeable} object.
     *
     * @param closeable
     *            the {@link Closeable} instance to close, mustn't be <code>null</code>.
     * @param closeableFile
     *            the closeable {@link File} to log if any exception occurs.
     * @since 1.0
     */
    public static void closeCloseable(final Closeable closeable, final File closeableFile) {
        closeCloseable(closeable, closeableFile.toString());
    }

    /**
     * Properly close a {@link Closeable} object.
     *
     * @param closeable
     *            the {@link Closeable} instance to close, mustn't be <code>null</code>.
     * @param closeableName
     *            the closeable name to log if any exception occurs.
     * @since 1.0
     */
    public static void closeCloseable(final Closeable closeable, final String closeableName) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException e) {
                LOGGER.log(SEVERE, buildLogString(CLOSE_FILE_ERROR, closeableName), e);
            }
        }
    }

    /**
     * Copy a file.
     *
     * @param fileFrom
     *            the file from, mustn't be <code>null</code>.
     * @param fileTo
     *            the file to, mustn't be <code>null</code>.
     * @return <code>true</code> if no error have occurred while copying the file,
     *         <code>false</code> otherwise.
     * @since 0.8
     */
    public static boolean copyFile(final File fileFrom, final File fileTo) {
        assert fileFrom != null : "fileFrom is null";
        assert fileTo != null : "fileTo is null";
        LOGGER.info(buildLogString(COPY_FILE_INFO, new Object[] { fileFrom, fileTo }));
        boolean error = true;
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(fileFrom);
            outputStream = new FileOutputStream(fileTo);
            final FileChannel inChannel = inputStream.getChannel();
            final FileChannel outChannel = outputStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            error = false;
        } catch (final IOException e) {
            LOGGER.log(SEVERE, buildLogString(COPY_FILE_ERROR, new Object[] { fileFrom, fileTo }), e);
        } finally {
            closeCloseable(inputStream, fileFrom);
            closeCloseable(outputStream, fileTo);
        }
        return error;
    }

    /**
     * Copy a file.
     *
     * @param pathNameFrom
     *            the file path from, mustn't be <code>null</code>.
     * @param pathNameTo
     *            the file path to, mustn't be <code>null</code>.
     * @return <code>true</code> if no error have occurred while copying the file,
     *         <code>false</code> otherwise.
     * @since 0.8
     */
    public static boolean copyFile(final String pathNameFrom, final String pathNameTo) {
        assert pathNameFrom != null : "pathNameFrom is null";
        assert pathNameTo != null : "pathNameTo is null";
        return copyFile(new File(pathNameFrom), new File(pathNameTo));
    }

    /**
     * Deletes the file or directory denoted by this abstract pathname. If this pathname denotes a
     * directory, then the directory must be empty in order to be deleted.
     *
     * @param pathName
     *            the file or directory path name to delete, mustn't be <code>null</code>.
     * @return <code>true</code> if and only if the file or directory is successfully deleted;
     *         <code>false</code> otherwise.
     * @since 0.8
     */
    public static boolean deleteFile(final String pathName) {
        assert pathName != null : "pathName is null";
        final File fileToDelete = new File(pathName);
        return fileToDelete.delete();
    }

    /**
     * Creates the directory named by this abstract pathname.
     *
     * @param pathName
     *            the file or directory path name to create, mustn't be <code>null</code>.
     * @return <code>true</code> if and only if the directory was created; <code>false</code>
     *         otherwise.
     * @since 0.8
     */
    public static boolean mkdir(final String pathName) {
        assert pathName != null : "pathName is null";
        final File fileToCreate = new File(pathName);
        return fileToCreate.mkdir();
    }

    /**
     * This method reads the following file path as a <code>String</code>.
     *
     * @param file
     *            the file to read from, must'nt be <code>null</code>.
     * @return the file content as a <code>String</code>, or the <code>null</code> constant if any
     *         error has occurred.
     * @since 1.0
     */
    public static String readFileIntoString(final File file) {
        assert file != null : "file is null";
        LOGGER.info(buildLogString(READ_FILE_INFO, new Object[] { file }));
        FileInputStream fi = null;
        String result = null;
        try {
            fi = new FileInputStream(file);
            result = readStreamIntoString(fi);
        } catch (final IOException e) {
            LOGGER.log(SEVERE, buildLogString(READ_FILE_ERROR, file), e);
        } finally {
            closeCloseable(fi, file);
        }
        return result;
    }

    /**
     * This method reads the following file path as a <code>String</code>.
     *
     * @param pathName
     *            the file pathname to read from, must'nt be <code>null</code>.
     * @return the file content as a <code>String</code>, or the <code>null</code> constant if any
     *         error has occurred.
     * @since 1.0
     */
    public static String readFileIntoString(final String pathName) {
        assert pathName != null : "pathName is null";
        return readFileIntoString(new File(pathName));
    }

    /**
     * This method reads the following stream as a <code>String</code>. Doesn't close the stream.
     *
     * @param stream
     *            the input stream to read from, must'nt be <code>null</code>.
     * @return the stream content as a <code>String</code>, or the <code>null</code> constant if any
     *         error has occurred.
     * @throws IOException
     *             if any exception occurs while reading the stream.
     * @since 1.0
     */
    public static String readStreamIntoString(final InputStream stream) throws IOException {
        assert stream != null : "stream is null";
        StringWriter out = null;
        final BufferedInputStream in = new BufferedInputStream(stream);
        out = new StringWriter();
        int b;
        while ((b = in.read()) != MINUS_ONE_RETURN_CODE) {
            out.write(b);
        }
        return out.toString();
    }

    /**
     * This method writes the following <code>String</code> into the file.
     *
     * @param str
     *            the <code>String</code> to write, must'nt be <code>null</code> .
     * @param file
     *            the file to write into, must'nt be <code>null</code>.
     * @return <code>true</code> if no error have occurred while writing the file,
     *         <code>false</code> otherwise.
     * @since 1.0
     */
    public static boolean writeIntoFile(final String str, final File file) {
        return writeIntoFile(str, file, false);
    }

    /**
     * This method writes the following <code>String</code> into the file.
     *
     * @param str
     *            the <code>String</code> to write, must'nt be <code>null</code> .
     * @param file
     *            the file to write into, must'nt be <code>null</code>.
     * @param append
     *            if <code>true</code>, then data will be written to the end of the file rather than
     *            the beginning.
     * @return <code>true</code> if no error have occurred while writing the file,
     *         <code>false</code> otherwise.
     * @since 1.0
     */
    public static boolean writeIntoFile(final String str, final File file, final boolean append) {
        assert str != null : "str is null";
        assert file != null : "file is null";
        LOGGER.info(buildLogString(WRITE_FILE_INFO, file));
        boolean error = true;
        FileWriter writer = null;
        try {
            writer = new FileWriter(file, append);
            writer.write(str, 0, str.length());
            error = false;
        } catch (final IOException e) {
            LOGGER.log(SEVERE, buildLogString(READ_FILE_ERROR, file), e);
        } finally {
            closeCloseable(writer, file);
        }
        return error;
    }

    /**
     * This method writes the following <code>String</code> into the file.
     *
     * @param str
     *            the <code>String</code> to write, must'nt be <code>null</code> .
     * @param path
     *            the file path to write into, must'nt be <code>null</code>.
     * @return <code>true</code> if no error have occurred while writing the file,
     *         <code>false</code> otherwise.
     * @since 1.0
     */
    public static boolean writeIntoFile(final String str, final String path) {
        return writeIntoFile(str, path, false);
    }

    /**
     * This method writes the following <code>String</code> into the file.
     *
     * @param str
     *            the <code>String</code> to write, must'nt be <code>null</code> .
     * @param pathName
     *            the file path to write into, must'nt be <code>null</code>.
     * @param append
     *            if <code>true</code>, then data will be written to the end of the file rather than
     *            the beginning.
     * @return <code>true</code> if no error have occurred while writing the file,
     *         <code>false</code> otherwise.
     * @since 1.0
     */
    public static boolean writeIntoFile(final String str, final String pathName, final boolean append) {
        assert pathName != null : "pathName is null";
        return writeIntoFile(str, new File(pathName), append);
    }

    /**
     * Private constructor to prevent from instantiation.
     *
     * @since 1.0
     */
    private FileUtils() {
        super();
    }
}
