package net.sf.gateway.mef.utilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Some basic file input/output utilities. Eventually we want to move away from
 * these methods and on to Apache Commons-IO.
 */
public final class IOUtils {

    /**
     * Logging facility.
     */
    protected static final Log LOG = LogFactory.getLog(IOUtils.class.getName());

    /**
     * Method reads a file in as a byte array identical to the byte array that
     * was written using byteSafeWrite().
     * 
     * @param file
     *        file to read byte array from
     * @return byte array as read from file
     * @throws Exceptions
     */
    public static byte[] byteSafeRead(String file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        FileChannel fc = fis.getChannel();
        byte[] data = new byte[(int) fc.size()];
        ByteBuffer bb = ByteBuffer.wrap(data);
        fc.read(bb);
        return data;
    }

    /**
     * Method writes a byte array to file, byteSafeRead reproduces the exact
     * byte array from file.
     * 
     * @param arr
     *        byte array to write
     * @param file
     *        file to write to
     * @throws Exceptions
     */
    public static void byteSafeWrite(byte[] arr, String file) throws Exception {
        File someFile = new File(file);
        FileOutputStream fos = new FileOutputStream(someFile);
        fos.write(arr);
        fos.flush();
        fos.close();
    }

    /**
     * Builds a byte[] from an InputStream. Attempts to read the entire stream.
     * Closes the stream when done.
     * 
     * @param is
     *        any InputStream
     * @return a byte[] containing the data read from the InputStream.
     * @throws IOException
     */
    public static byte[] inputStreamToByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf, 0, 4096)) != -1) {
            os.write(buf, 0, len);
        }
        os.close();
        is.close();
        return os.toByteArray();
    }

    public byte[] readFromClasspath(String path) {
        InputStream src = getClass().getResourceAsStream(path);
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        byte[] data = new byte[2048];
        int count;
        try {
            while ((count = src.read(data, 0, 2048)) != -1) {
                dest.write(data, 0, count);
            }
        } catch (IOException e) {
            LOG.error("IO Error Reading from Classpath", e);
            return null;
        }
        return dest.toByteArray();
    }
}
