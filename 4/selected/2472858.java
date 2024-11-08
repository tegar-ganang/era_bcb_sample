package opaqua.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

/**
 * This class is an adapted Template out of the Internet:
 * <Url>http://de.geocities.com/uweplonus/faq/io.html#dateiKopieren</Url>
 * 
 * @author Thomas Siegrist
 */
public class CopyFile {

    /**
	 * That method copies a File src to an other File dest
	 * 
	 * @param src
	 *            (Source File)
	 * @param dest
	 *            (Destination File)
	 * @param bufSize
	 *            (defines the size of the buffer = size of the file in bytes:
	 *            for example 1024 * 1024 = 1MB, maxBufSize = dimensionOf(int))
	 * @param force
	 *            (if true: if the destination file already exists, it's being
	 *            replaced)
	 * @throws IOException
	 */
    public static void copyFile(File src, File dest, boolean force) throws IOException, InterruptedIOException {
        if (dest.exists()) {
            if (force) {
                dest.delete();
            } else {
                throw new IOException("Cannot overwrite existing file!");
            }
        }
        byte[] buffer = new byte[5 * 1024 * 1024];
        int read = 0;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new BufferedInputStream(new FileInputStream(src));
            out = new BufferedOutputStream(new FileOutputStream(dest));
            while (true) {
                read = in.read(buffer);
                if (read == -1) {
                    break;
                }
                out.write(buffer, 0, read);
            }
        } finally {
            buffer = null;
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
    }
}
