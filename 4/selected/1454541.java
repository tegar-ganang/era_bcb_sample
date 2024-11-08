package org.happycomp.radio.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.logging.Level;
import org.happycomp.radio.StopDownloadCondition;

public class IOUtils {

    public static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(IOUtils.class.getName());

    private IOUtils() {
    }

    /**
	 * Kopirovani ze vstupniho proudo do vystupniho
	 * @param is Vstupni proud
	 * @param os Vystupni proud
	 * @throws IOException
	 */
    public static void copyStreams(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[8192];
        int read = -1;
        while ((read = is.read(buffer)) > 0) {
            os.write(buffer, 0, read);
        }
    }

    /**
	 * Kopiruje a pocita digest
	 * @param is Vstupni stream
	 * @param os Vystupni stream
	 * @param digest Digest
	 * @throws IOException
	 */
    public static boolean copyStreams(InputStream is, OutputStream os, StopDownloadCondition condition) throws IOException {
        byte[] buffer = new byte[8192];
        int read = -1;
        while ((read = is.read(buffer)) > 0) {
            os.write(buffer, 0, read);
            if (condition.isStopped()) {
                LOGGER.info("stopped copying stream");
                return true;
            }
        }
        return false;
    }

    public static void copyFile(File src, File dst) throws IOException {
        LOGGER.info("Copying file '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() + "'");
        FileChannel in = null;
        FileChannel out = null;
        try {
            FileInputStream fis = new FileInputStream(src);
            in = fis.getChannel();
            FileOutputStream fos = new FileOutputStream(dst);
            out = fos.getChannel();
            out.transferFrom(in, 0, in.size());
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
    }

    public static boolean deleteDirectory(File rootDir) {
        File[] listFiles = rootDir.listFiles();
        for (File file : listFiles) {
            if (file.isFile()) file.delete(); else if (file.isDirectory()) deleteDirectory(rootDir);
        }
        return rootDir.delete();
    }

    public static String readAsString(InputStream is, Charset charset, boolean closeInput) throws IOException {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copyStreams(is, bos);
            return new String(bos.toByteArray(), charset);
        } finally {
            if ((is != null) && closeInput) {
                is.close();
            }
        }
    }

    public static void writeString(String content, File batchFile) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(batchFile);
            fos.write(content.getBytes());
        } finally {
            fos.close();
        }
    }
}
