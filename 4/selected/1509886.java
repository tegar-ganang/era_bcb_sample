package org.xi8ix.loader;

import javax.swing.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.lang.reflect.Method;

/**
 * @author Iain Shigeoka
 */
public class Util {

    private static final Logger LOG = Logger.getLogger(Util.class.getName());

    private Util() {
    }

    /** milliseconds in an hour */
    private static final int HOUR_IN_MS = 60 * 60 * 1000;

    /** milliseconds in a day */
    private static final int DAY_IN_MS = 24 * HOUR_IN_MS;

    /**
     * Creates a human-readable, pretty printed version of the duration.
     *
     * @param duration the number of milliseconds
     * @return a string representing the duration in a nice, human friendly format
     */
    public static String toPrettyDurationString(long duration) {
        StringBuffer sb = new StringBuffer();
        if (duration < 0) {
            sb.append("-");
        }
        long absTime = Math.abs(duration);
        if (absTime > DAY_IN_MS) {
            sb.append(absTime / DAY_IN_MS).append("d ");
            absTime = absTime % DAY_IN_MS;
        }
        if (absTime > HOUR_IN_MS) {
            sb.append(absTime / HOUR_IN_MS).append("h ");
            absTime = absTime % HOUR_IN_MS;
        }
        if (absTime > 60000) {
            sb.append(absTime / 60000).append("m ");
            absTime = absTime % 60000;
        }
        if (absTime > 1000) {
            sb.append(absTime / 1000).append("s ");
            absTime = absTime % 1000;
        }
        sb.append(absTime).append("ms");
        return sb.toString().trim();
    }

    /**
     * Copy the input stream to the output stream and optionally leave both streams open when done.
     *
     * @pre in != null
     * @pre out != null
     * @param in the input stream to copy from
     * @param out the output stream to copy to
     * @param close true if the streams should be closed after the copy is completed
     * @throws IOException if there was a problem carrying out the copy
     */
    public static void copy(InputStream in, OutputStream out, boolean close) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
                out.write(buffer, 0, read);
            }
        } finally {
            if (close) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Unzips the Java binaries from a WAR archive.
     *
     * @param war the war file to open
     * @param dest the destination directory where the file should be unzipped or null to use current working directory
     * @pre war != null
     * @post $none
     * @throws IOException if there was a problem unzipping the entry
     */
    public static void unzipFile(File war, File dest) throws IOException {
        LOG.log(Level.FINEST, "Unzip files from {0}", war);
        ZipInputStream in = new ZipInputStream(new FileInputStream(war));
        File root;
        if (dest == null) {
            root = new File(".");
        } else {
            root = dest;
        }
        LOG.log(Level.FINEST, "Unzip destination {0}", root);
        for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
            LOG.log(Level.FINEST, "Extracting {0}", entry);
            if (entry.isDirectory()) {
                File dir = new File(root, entry.getName());
                dir.mkdirs();
            } else {
                FileOutputStream out = new FileOutputStream(new File(root, entry.getName()));
                copy(in, out, false);
                out.close();
            }
        }
    }

    /**
     * Deletes a file or directory and all subdirectories.
     *
     * @param dir the directory or file to delete
     */
    public static void delete(File dir) {
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (file.exists()) {
                    if (file.isDirectory()) {
                        LOG.log(Level.FINEST, "Deleting dir-tree {0}", file);
                        delete(file);
                    } else {
                        LOG.log(Level.FINEST, "Deleting file {0}", file);
                        file.delete();
                    }
                }
            }
            if (dir.exists()) {
                LOG.log(Level.FINEST, "Deleting dir {0}", dir);
                dir.delete();
            }
        } else {
            LOG.log(Level.FINEST, "No file {0} to delete", dir);
        }
    }

    public static void openURL(String url) {
        String osName = System.getProperty("os.name");
        try {
            if (osName.startsWith("Mac OS")) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL", String.class);
                openURL.invoke(null, url);
            } else if (osName.startsWith("Windows")) Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url); else {
                String[] browsers = { "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
                String browser = null;
                for (int count = 0; count < browsers.length && browser == null; count++) if (Runtime.getRuntime().exec(new String[] { "which", browsers[count] }).waitFor() == 0) browser = browsers[count];
                if (browser == null) throw new Exception("Could not find web browser"); else Runtime.getRuntime().exec(new String[] { browser, url });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Sorry, we could not open a web browser automatically, please go to:\n" + url);
        }
    }
}
