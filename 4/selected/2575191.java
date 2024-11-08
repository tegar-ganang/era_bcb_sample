package fi.tuska.jalkametri.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public final class FileUtil {

    public static final Charset CHARSET_UTF8 = Charset.forName("UTF8");

    private static final String TAG = "FileUtil";

    private FileUtil() {
    }

    public static boolean copyFileSafe(File src, File dst) {
        try {
            copyFile(src, dst);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean writeUTF8ToFileSafe(String dataToWrite, File file) {
        try {
            writeUTF8ToFile(dataToWrite, file);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static String readUTF8FromFileSafe(File file) {
        try {
            return readUTF8FromFile(file);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Copies a file.
     */
    public static void copyFile(File src, File dst) throws IOException {
        LogUtil.d(TAG, "Copying file %s to %s", src, dst);
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inChannel = new FileInputStream(src).getChannel();
            outChannel = new FileOutputStream(dst).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            closeSafe(inChannel);
            closeSafe(outChannel);
        }
    }

    public static void writeUTF8ToFile(String dataToWrite, File file) throws IOException {
        LogUtil.d(TAG, "Writing to file %s", file);
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        OutputStreamWriter osw = null;
        fos = new FileOutputStream(file);
        bos = new BufferedOutputStream(fos);
        osw = new OutputStreamWriter(bos, CHARSET_UTF8);
        try {
            osw.write(dataToWrite);
            osw.flush();
            bos.flush();
            fos.flush();
        } finally {
            closeSafe(osw);
            closeSafe(bos);
            closeSafe(fos);
        }
    }

    public static String readUTF8FromFile(File file) throws IOException {
        LogUtil.d(TAG, "Reading from file %s", file);
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        InputStreamReader isr = null;
        fis = new FileInputStream(file);
        bis = new BufferedInputStream(fis);
        isr = new InputStreamReader(bis, CHARSET_UTF8);
        try {
            char[] buf = new char[8192];
            int read = 0;
            StringBuilder b = new StringBuilder((int) file.length());
            while ((read = isr.read(buf)) > 0) {
                b.append(buf, 0, read);
            }
            return b.toString();
        } finally {
            closeSafe(isr);
            closeSafe(bis);
            closeSafe(fis);
        }
    }

    private static final void closeSafe(Closeable s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException e) {
                LogUtil.w(TAG, "Error when closing stream %s: %s (%s)", s, e.getMessage(), e);
            }
        }
    }
}
