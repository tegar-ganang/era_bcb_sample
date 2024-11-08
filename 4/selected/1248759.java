package commons;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ImageMagickResizer {

    private static final Log logger = LogFactory.getLog(ImageMagickResizer.class);

    static class StreamGobbler extends Thread {

        InputStream is;

        OutputStream os;

        StreamGobbler(InputStream is, OutputStream redirect, String name) {
            this.is = new BufferedInputStream(is);
            this.os = redirect;
            setName(name);
        }

        public void run() {
            try {
                IOUtils.copy(is, os);
                os.flush();
            } catch (IOException ioe) {
                logger.error("Unable to copy", ioe);
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
            }
        }
    }

    public static int convertImage(InputStream is, OutputStream os, String command) throws IOException, InterruptedException {
        if (logger.isInfoEnabled()) {
            logger.info(command);
        }
        Process p = Runtime.getRuntime().exec(command);
        ByteArrayOutputStream errOut = new ByteArrayOutputStream();
        StreamGobbler errGobbler = new StreamGobbler(p.getErrorStream(), errOut, "Convert Thread (err gobbler): " + command);
        errGobbler.start();
        StreamGobbler outGobbler = new StreamGobbler(new BufferedInputStream(is), p.getOutputStream(), "Convert Thread (out gobbler): " + command);
        outGobbler.start();
        try {
            IOUtils.copy(p.getInputStream(), os);
            os.flush();
            if (p.waitFor() != 0) {
                logger.error("Unable to convert, stderr: " + new String(errOut.toByteArray(), "UTF-8"));
            }
            return p.exitValue();
        } finally {
            IOUtils.closeQuietly(os);
        }
    }
}
