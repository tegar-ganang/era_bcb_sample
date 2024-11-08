package net.sf.dropboxmq;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.nio.channels.FileChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created: 02 Sep 2006
 *
 * @author <a href="mailto:dwayne@schultz.net">Dwayne Schultz</a>
 * @version $Revision: 235 $, $Date: 2011-08-27 00:55:14 -0400 (Sat, 27 Aug 2011) $
 */
public class FileSystem {

    private static final Log log = LogFactory.getLog(FileSystem.class);

    public static class FileSystemException extends Exception {

        private static final long serialVersionUID = -2199797453328061675L;

        FileSystemException(final String message) {
            super(message);
        }

        FileSystemException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    public static class SourceNotFoundException extends FileSystemException {

        private static final long serialVersionUID = 6334027485795248687L;

        SourceNotFoundException(final String message) {
            super(message);
        }
    }

    public InputStream newBufferedInputStream(final File file) throws IOException {
        LogHelper.logMethod(log, toObjectString(), "newBufferedInputStream(), file = " + file);
        return new BufferedInputStream(new FileInputStream(file));
    }

    public OutputStream newBufferedOutputStream(final File file) throws IOException {
        LogHelper.logMethod(log, toObjectString(), "newBufferedOutputStream(), file = " + file);
        return new BufferedOutputStream(new FileOutputStream(file));
    }

    public BufferedReader newBufferedReader(final File file) throws IOException {
        LogHelper.logMethod(log, toObjectString(), "newBufferedReader(), file = " + file);
        return new BufferedReader(new FileReader(file));
    }

    public BufferedWriter newBufferedWriter(final File file) throws IOException {
        LogHelper.logMethod(log, toObjectString(), "newBufferedWriter(), file = " + file);
        return new BufferedWriter(new FileWriter(file));
    }

    public boolean exists(final File file) {
        return file.exists();
    }

    public void mkdirs(final File directory) throws FileSystemException {
        LogHelper.logMethod(log, toObjectString(), "mkdirs(), directory = " + directory);
        if (!directory.mkdirs() && !exists(directory)) {
            throw new FileSystemException("Could not create directory " + directory);
        }
        log.info("Made directory " + directory);
    }

    public boolean createNewFile(final File file, final boolean deleteOnExit) throws FileSystemException {
        LogHelper.logMethod(log, toObjectString(), "createRuntimeFile(), file = " + file);
        final boolean created;
        try {
            created = file.createNewFile();
        } catch (IOException e) {
            throw new FileSystemException("Unexpected IOException while creating a runtime file", e);
        }
        if (deleteOnExit) {
            file.deleteOnExit();
        }
        return created;
    }

    public void copy(final File source, final File target) throws FileSystemException {
        LogHelper.logMethod(log, toObjectString(), "copy(), source = " + source + ", target = " + target);
        FileChannel sourceChannel = null;
        FileChannel targetChannel = null;
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            targetChannel = new FileOutputStream(target).getChannel();
            sourceChannel.transferTo(0L, sourceChannel.size(), targetChannel);
            log.info("Copied " + source + " to " + target);
        } catch (FileNotFoundException e) {
            throw new FileSystemException("Unexpected FileNotFoundException while copying a file", e);
        } catch (IOException e) {
            throw new FileSystemException("Unexpected IOException while copying a file", e);
        } finally {
            if (sourceChannel != null) {
                try {
                    sourceChannel.close();
                } catch (IOException e) {
                    log.error("IOException during source channel close after copy", e);
                }
            }
            if (targetChannel != null) {
                try {
                    targetChannel.close();
                } catch (IOException e) {
                    log.error("IOException during target channel close after copy", e);
                }
            }
        }
    }

    public void move(final File source, final File target) throws FileSystemException {
        LogHelper.logMethod(log, toObjectString(), "move(), source = " + source + ", target = " + target);
        if (target == null) {
            delete(source);
        } else {
            if (source.renameTo(target)) {
                log.info("Moved " + source + " to " + target);
            } else {
                final String message = "Failed to move " + source + " to " + target;
                if (exists(source)) {
                    throw new FileSystemException(message);
                } else {
                    throw new SourceNotFoundException(message);
                }
            }
        }
    }

    public void delete(final File file) throws FileSystemException {
        LogHelper.logMethod(log, toObjectString(), "delete(), file = " + file);
        deleteRecursively(file);
    }

    private static void deleteRecursively(final File file) throws FileSystemException {
        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteRecursively(files[i]);
            }
        }
        if (!file.delete()) {
            throw new FileSystemException("Could not delete " + file);
        }
        log.info("Deleted " + file);
    }

    public boolean readPropertiesFile(final File propertiesFile, final Properties properties) {
        LogHelper.logMethod(log, toObjectString(), "readPropertiesFile(), propertiesFile = " + propertiesFile + ", properties = " + properties);
        boolean successful = false;
        InputStream in = null;
        try {
            in = newBufferedInputStream(propertiesFile);
            properties.load(in);
            successful = true;
        } catch (IOException ignore) {
            log.warn("IOException while trying to properties, " + propertiesFile);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                    log.warn("IOException while trying to close properties, " + propertiesFile);
                }
            }
        }
        return successful;
    }

    protected final String toObjectString() {
        return super.toString();
    }
}
