package persdocmanager.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.log4j.Logger;

public class Filecopy {

    protected static final Logger logger = Logger.getLogger(Filecopy.class);

    /**
	 * copy a file imported from filesystem or from temp directory (scanning) to
	 * the repository
	 * 
	 * @param src
	 *            the src file
	 * @param dest
	 *            the new repository file
	 * @param bufSize
	 *            the buffer size
	 * @param force
	 *            owerwrite boolean
	 * @throws IOException
	 *             if copy fails
	 */
    public static void copyFile(File src, File dest, int bufSize, boolean force) throws IOException {
        logger.info("copyFile(File src=" + src + ", File dest=" + dest + ", int bufSize=" + bufSize + ", boolean force=" + force + ") - start");
        File f = new File(Configuration.getArchiveDir());
        if (!f.exists()) {
            f.mkdir();
        }
        if (dest.exists()) {
            if (force) {
                dest.delete();
            } else {
                throw new IOException("Cannot overwrite existing file: " + dest);
            }
        }
        byte[] buffer = new byte[bufSize];
        int read = 0;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dest);
            while (true) {
                read = in.read(buffer);
                if (read == -1) {
                    break;
                }
                out.write(buffer, 0, read);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } finally {
                    if (out != null) {
                        out.close();
                    }
                }
            }
        }
        logger.debug("copyFile(File, File, int, boolean) - end");
    }
}
