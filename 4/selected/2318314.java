package de.saly.javacommonslib.base.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IOCopyUtils {

    protected static final Log log = LogFactory.getLog(IOCopyUtils.class);

    public static void resourceToFile(final String resource, final String filePath) throws IOException {
        log.debug("Classloader is " + IOCopyUtils.class.getClassLoader());
        InputStream in = IOCopyUtils.class.getResourceAsStream(resource);
        if (in == null) {
            log.warn("Resource not '" + resource + "' found. Try to prefix with '/'");
            in = IOCopyUtils.class.getResourceAsStream("/" + resource);
        }
        if (in == null) {
            throw new IOException("Resource not '" + resource + "' found.");
        }
        final File file = new File(filePath);
        final OutputStream out = FileUtils.openOutputStream(file);
        final int bytes = IOUtils.copy(in, out);
        IOUtils.closeQuietly(out);
        IOUtils.closeQuietly(in);
        log.debug("Copied resource '" + resource + "' to file " + filePath + " (" + bytes + " bytes)");
    }

    public static String resourceToString(final String resource, final String encoding) throws IOException {
        log.debug("Classloader is " + IOCopyUtils.class.getClassLoader());
        InputStream in = IOCopyUtils.class.getResourceAsStream(resource);
        if (in == null) {
            log.warn("Resource not '" + resource + "' found. Try to prefix with '/'");
            in = IOCopyUtils.class.getResourceAsStream("/" + resource);
        }
        if (in == null) {
            throw new IOException("Resource not '" + resource + "' found.");
        }
        if (encoding != null) return IOUtils.toString(in, encoding); else return IOUtils.toString(in);
    }
}
