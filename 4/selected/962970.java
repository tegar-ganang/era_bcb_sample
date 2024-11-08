package org.dcm4chex.archive.hsm.spi.utils;

import org.dcm4chex.archive.util.FileUtils;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;
import org.dcm4chex.archive.common.FileStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.*;
import java.io.*;
import java.text.MessageFormat;
import java.net.URL;

/**
 * @author Fuad Ibrahimov
 * @since Mar 29, 2007
 */
public abstract class HsmUtils extends FileUtils {

    private static final Log logger = LogFactory.getLog(HsmUtils.class);

    private static final String COULDNT_DELETE_DIRECTORY = "Couldn''t delete directory [{0}]";

    private static final String DIRECTORY_IS_NOT_EMPTY_SKIPPING = "Directory is not empty: [{0}]. Skipping from delete.";

    private static final String M_DELETE = "M-DELETE [{0}]";

    private static final String FILE_WAS_NOT_FOUND_IN_CLASSPATH = "File <{0}> was not found in classpath";

    public static final String FS_PREFIX_REGEXP = "\\w{3}:";

    public static void deleteParentsTill(File file, String topParent) throws IOException {
        Assert.notNull(file, "file");
        Assert.hasText(topParent, "topParent");
        File top = toFile(topParent);
        if (!file.getCanonicalPath().startsWith(top.getCanonicalPath())) throw new IOException(top + " is not a parent directory of " + file);
        File parent = file.getParentFile();
        while (!top.equals(parent)) {
            String[] children = parent.list();
            if (children == null || children.length == 0) {
                if (parent.delete()) {
                    if (logger.isInfoEnabled()) {
                        logger.info(MessageFormat.format(M_DELETE, parent));
                    }
                } else {
                    logger.warn(MessageFormat.format(COULDNT_DELETE_DIRECTORY, parent));
                }
            } else {
                logger.warn(MessageFormat.format(DIRECTORY_IS_NOT_EMPTY_SKIPPING, parent));
            }
            parent = parent.getParentFile();
        }
    }

    public static File classpathResource(String path) throws FileNotFoundException {
        URL resource = HsmUtils.class.getClassLoader().getResource(path);
        if (resource == null) throw new FileNotFoundException(MessageFormat.format(FILE_WAS_NOT_FOUND_IN_CLASSPATH, path));
        return new File(resource.getFile());
    }

    public static String extractFileSpaceName(String destPath) {
        return destPath.replaceFirst(FS_PREFIX_REGEXP, "");
    }

    public static String resolveFileSpacePath(String fileSpaceName) throws IOException {
        return toFile(HsmUtils.extractFileSpaceName(fileSpaceName)).getCanonicalPath();
    }

    public static void copy(File from, File to, int bufferSize) throws IOException {
        if (to.exists()) {
            logger.info("File " + to + " exists, will replace it.");
            to.delete();
        }
        to.getParentFile().mkdirs();
        to.createNewFile();
        FileInputStream ois = null;
        FileOutputStream cos = null;
        try {
            ois = new FileInputStream(from);
            cos = new FileOutputStream(to);
            byte[] buf = new byte[bufferSize];
            int read;
            while ((read = ois.read(buf, 0, bufferSize)) > 0) {
                cos.write(buf, 0, read);
            }
            cos.flush();
        } finally {
            try {
                if (ois != null) ois.close();
            } catch (IOException ignored) {
                logger.warn("Could not close file input stream " + from, ignored);
            }
            try {
                if (cos != null) {
                    cos.close();
                }
            } catch (IOException ignored) {
                logger.warn("Could not close file output stream " + to, ignored);
            }
        }
    }
}
