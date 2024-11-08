package zhyi.zse.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility methods for I/O.
 * @author Zhao Yi
 */
public final class IoHelper {

    private IoHelper() {
    }

    /**
     * Closes an {@link Closeable} and ignores any exception thrown from {@link
     * Closeable#close()}.
     */
    public static void closeSilently(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception ex) {
        }
    }

    /**
     * Writes all bytes available from an {@link InputStream} to an {@link
     * OutputStream}. Both streams remain open after this method returns.
     * @throws IOException If an I/O error has occurred.
     */
    public static void transfer(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024 * 1024];
        int read = -1;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
