package com.crypticbit.ipa.io.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import com.crypticbit.ipa.central.LogFactory;

public final class IoUtils {

    public static final String IPHONE_PATH_SEP = "/";

    public static String convertStreamToString(final InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            LogFactory.getLogger().log(Level.SEVERE, "Exception", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                LogFactory.getLogger().log(Level.SEVERE, "Exception", e);
            }
        }
        return sb.toString();
    }

    public static void copyFile(final File source, final File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    public static File createTempDir(final String suffix) throws IOException {
        File tempDir = File.createTempFile("ipa", suffix);
        tempDir.delete();
        tempDir.mkdir();
        tempDir.deleteOnExit();
        return tempDir;
    }

    public static File getExtractFile(String filename) throws IOException {
        File tempDir = createTempDir("extract");
        return new File(tempDir, filename);
    }

    /**
	 * Read a file into a byte array.
	 * 
	 * @param file
	 *            - the File to read
	 * @return - the file contents as a byte array
	 * @throws IOException
	 */
    public static byte[] getBytesFromFile(final File file) throws IOException {
        return getBytesFromFile(new FileInputStream(file));
    }

    /**
	 * Read a input stream into a byte array.
	 * 
	 * @param file
	 *            - the File to read
	 * @return - the file contents as a byte array
	 * @throws IOException
	 */
    public static byte[] getBytesFromFile(final InputStream is) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) {
            byteArrayOutputStream.write((char) c);
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
	 * Get a filename with the extension removed if present
	 * 
	 * @param f
	 *            - the file to get the basename of eg. c:/temp/thefile.txt
	 * @return - the basename eg. thefile
	 */
    public static String getFilenameNoExtension(final File f) {
        return f.getName().substring(0, f.getName().indexOf("."));
    }

    private IoUtils() {
    }
}
