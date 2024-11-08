package net.sf.emailsink.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.FileCleaner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Functionality for creating temporary files.
 *
 * @author Peter Monks
 * @version $Id: TemporaryFileFactory.java 22 2007-01-08 02:21:13Z pmonks $
 */
public class TemporaryFileFactory {

    private static final Logger log = Logger.getLogger(TemporaryFileFactory.class);

    private static final String SYSTEM_PROPERTY_NAME_JAVA_TEMP_DIRECTORY = "java.io.tmpdir";

    private static final String TEMPORARY_SUBDIRECTORY_NAME = "emailSink";

    private static final String DEFAULT_FILENAME_PREFIX = "tempFile_";

    private static final String DEFAULT_FILENAME_SUFFIX = ".tmp";

    private final File tempDirectory;

    private final String prefix;

    private final String suffix;

    public TemporaryFileFactory() throws IOException {
        this(null);
    }

    public TemporaryFileFactory(final File tempDirectory) throws IOException {
        this(tempDirectory, null, null);
    }

    public TemporaryFileFactory(final File tempDirectory, final String filenamePrefix, final String filenameSuffix) throws IOException {
        File actualTempDirectory = tempDirectory;
        if (actualTempDirectory == null) {
            String javaTempDirectoryName = System.getProperty(SYSTEM_PROPERTY_NAME_JAVA_TEMP_DIRECTORY) + File.separator + TEMPORARY_SUBDIRECTORY_NAME;
            log.debug("No temp directory provided, defaulting to '" + javaTempDirectoryName + "'.");
            actualTempDirectory = new File(javaTempDirectoryName);
        }
        if (!actualTempDirectory.exists()) {
            log.debug(actualTempDirectory.getCanonicalPath() + " does not exist - creating it.");
            FileUtils.forceMkdir(actualTempDirectory);
            actualTempDirectory.deleteOnExit();
        }
        assert actualTempDirectory.exists() : actualTempDirectory.getCanonicalPath() + " does not exist and could not be created.";
        assert actualTempDirectory.isDirectory() : actualTempDirectory.getCanonicalPath() + " is not a directory.";
        assert actualTempDirectory.canRead() : actualTempDirectory.getCanonicalPath() + " is not readable.";
        assert actualTempDirectory.canWrite() : actualTempDirectory.getCanonicalPath() + " is not writable.";
        this.tempDirectory = actualTempDirectory;
        this.prefix = filenamePrefix == null ? DEFAULT_FILENAME_PREFIX : filenamePrefix;
        this.suffix = filenameSuffix == null ? DEFAULT_FILENAME_SUFFIX : filenameSuffix;
    }

    /**
     * @return A new temporary <code>File</code> <i>(will not be null)</i>.
     * @note The files created by this method will be automatically deleted after the associated in-memory
     * <code>File</code> object gets garbage collected.  This class makes no guarantees regarding the
     * exact timing of the deletion however.
     */
    public File createTemporaryFile() throws IOException {
        File result = null;
        result = File.createTempFile(prefix, suffix, tempDirectory);
        log.debug("Created temporary file " + result.getCanonicalPath());
        FileCleaner.track(result, result);
        result.deleteOnExit();
        return (result);
    }

    /**
     * Convenience method that reads data from the given <code>InputStream</code> and writes it out to the given <code>File</code>.
     * 
     * @param file   The file to write the data to <i>(must not be null, must not be a directory, must be writable)</i>.
     * @param source The input stream to read data from <i>(must not be null)</i>.
     * @throws IOException
     * @todo Refactor: this method doesn't really belong here...
     */
    public static void write(File file, InputStream source) throws IOException {
        OutputStream outputStream = null;
        assert file != null : "file must not be null.";
        assert file.isFile() : "file must be a file.";
        assert file.canWrite() : "file must be writable.";
        assert source != null : "source must not be null.";
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
            IOUtils.copy(source, outputStream);
            outputStream.flush();
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }
}
