package org.apache.commons.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility methods for dealng with FileObjects.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision: 480428 $ $Date: 2006-11-28 22:15:24 -0800 (Tue, 28 Nov 2006) $
 */
public class FileUtil {

    private FileUtil() {
    }

    /**
     * Returns the content of a file, as a byte array.
     *
     * @param file The file to get the content of.
     */
    public static byte[] getContent(final FileObject file) throws IOException {
        final FileContent content = file.getContent();
        final int size = (int) content.getSize();
        final byte[] buf = new byte[size];
        final InputStream in = content.getInputStream();
        try {
            int read = 0;
            for (int pos = 0; pos < size && read >= 0; pos += read) {
                read = in.read(buf, pos, size - pos);
            }
        } finally {
            in.close();
        }
        return buf;
    }

    /**
     * Writes the content of a file to an OutputStream.
     */
    public static void writeContent(final FileObject file, final OutputStream outstr) throws IOException {
        final InputStream instr = file.getContent().getInputStream();
        try {
            final byte[] buffer = new byte[1024];
            while (true) {
                final int nread = instr.read(buffer);
                if (nread < 0) {
                    break;
                }
                outstr.write(buffer, 0, nread);
            }
        } finally {
            instr.close();
        }
    }

    /**
     * Copies the content from a source file to a destination file.
     */
    public static void copyContent(final FileObject srcFile, final FileObject destFile) throws IOException {
        final OutputStream outstr = destFile.getContent().getOutputStream();
        try {
            writeContent(srcFile, outstr);
        } finally {
            outstr.close();
        }
    }
}
