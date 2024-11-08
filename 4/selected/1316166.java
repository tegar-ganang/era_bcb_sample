package eu.somatik.moviebrowser.tools;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;

/**
 *
 * @author francisdb
 */
public class FileTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileTools.class);

    private static final int DEFAULT_BUFF_SIZE = 1024 * 64;

    private FileTools() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Deletes a directory and all its subdirectories
     * @param path
     * @return
     */
    public static boolean deleteDirectory(File path) {
        LOGGER.debug("Deleting recursively: " + path.getAbsolutePath());
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    boolean deleted = files[i].delete();
                    if (!deleted) {
                        LOGGER.error("Could not delete: " + files[i].getAbsolutePath());
                    }
                }
            }
        }
        return path.delete();
    }

    /**
     * Renames a directory.
     * @param oldFile
     * @param newFile 
     * @return boolean success
     */
    public static boolean renameDir(File oldFile, File newFile) {
        boolean success = false;
        if (newFile != null) {
            success = oldFile.renameTo(newFile);
        }
        return success;
    }

    /**
     * Copies a file using NIO.
     * @param source
     * @param dest
     * @throws IOException
     */
    public static void copy(final File source, final File dest) throws IOException {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
            dest.setLastModified(source.lastModified());
        } finally {
            close(in);
            close(out);
        }
    }

    public static long copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFF_SIZE];
        int readCount = 0;
        long written = 0;
        while ((readCount = input.read(buffer)) != -1) {
            output.write(buffer, 0, readCount);
            if (readCount > 0) {
                written += readCount;
            }
        }
        return written;
    }

    public static void writeToFile(InputStream input, File destination) throws IOException {
        OutputStream fs = null;
        try {
            fs = new BufferedOutputStream(new FileOutputStream(destination));
            copy(input, fs);
        } finally {
            close(fs);
        }
    }

    /**
     * Loads a properties file
     * @param propsFile
     * @return the properties (empty if the propsFile does not exist)
     */
    public static Properties loadProperties(File propsFile) {
        Properties props = new Properties();
        InputStream is = null;
        try {
            if (propsFile.exists()) {
                is = new FileInputStream(propsFile);
                props.load(is);
            }
        } catch (IOException ex) {
            LOGGER.error("Could not load properties from " + propsFile.getAbsolutePath(), ex);
        } catch (SecurityException ex) {
            LOGGER.error("Could not load preferences from " + propsFile.getAbsolutePath(), ex);
        } finally {
            close(is);
        }
        return props;
    }

    /**
     * Stores a properties file, will create the path if needed
     * @param properties
     * @param propsFile
     */
    public static void storePropeties(Properties properties, File propsFile) {
        File parent = propsFile.getParentFile();
        if (!parent.exists()) {
            LOGGER.debug("Recursively creating folder for properties: " + parent.getAbsolutePath());
            parent.mkdirs();
        }
        propsFile.getParentFile().mkdirs();
        OutputStream os = null;
        try {
            os = new FileOutputStream(propsFile);
            properties.store(os, "Database version file");
        } catch (IOException ex) {
            LOGGER.error("Could not save preferences to " + propsFile.getAbsolutePath(), ex);
        } catch (SecurityException ex) {
            LOGGER.error("Could not save preferences to " + propsFile.getAbsolutePath(), ex);
        } finally {
            close(os);
        }
    }

    /**
     * Closes a closeable logging possible problems or errors
     * @param closeable
     */
    public static void close(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        } else {
            IOException ex = new IOException("Trying to close a null closeable");
            LOGGER.warn(ex.getMessage(), ex);
        }
    }
}
