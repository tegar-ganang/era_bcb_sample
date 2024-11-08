package net.sf.gateway.util;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Some basic file input/output utilities
 */
public final class IOUtils {

    /**
     * Logging facility.
     */
    protected static final Log LOG = LogFactory.getLog(IOUtils.class.getName());

    /**
     * Utility classes should not have a public or default constructor. -
     * checkstyle
     */
    private IOUtils() {
    }

    /**
     * Copies one file to another file location
     * 
     * @param in -file to copy
     * @param out -new file dest
     * @throws Exception
     */
    public static void copyFile(File in, File out) throws Exception {
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[1024];
        int i = 0;
        while ((i = fis.read(buf)) != -1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
    }

    /**
     * Method to call file2String with File object
     */
    public static String file2String(File file) {
        return file2String(file.toString());
    }

    /**
     * Method to call string2File with File object
     */
    public static void string2File(File file, String text) {
        string2File(file.toString(), text);
    }

    /**
     * Converts a given input file into a string
     * 
     * @param file
     *        any file to be read into a string
     * @throws Exception
     *         on setting up file access.
     */
    public static String file2String(String file) {
        StringBuffer buffer = new StringBuffer();
        try {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, "UTF8");
            Reader in = new BufferedReader(isr);
            int ch;
            while ((ch = in.read()) > -1) {
                buffer.append((char) ch);
            }
            in.close();
            return buffer.toString();
        } catch (Exception e) {
            LOG.error("File Read Failed", e);
        }
        return "";
    }

    /**
     * Writes out a given String to the specified file
     * 
     * @param fileName
     *        a name for the file with or without path
     * @param output
     *        a given string to be written to file
     * @throws Exception
     *         on file write actions
     */
    public static void string2File(String fileName, String output) {
        try {
            File file = new File(fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(239);
            fos.write(187);
            fos.write(191);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
            osw.write(output);
            osw.close();
        } catch (Exception e) {
            LOG.error("File Output Error", e);
        }
    }

    /**
     * Gets contents of clipboard
     * 
     * @return null if nothing was in clipboard, otherwise returns clipboard
     *         data as a string
     */
    public static String getClipboard() {
        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable clipboardContents = systemClipboard.getContents(null);
        if (clipboardContents == null) {
            return (null);
        } else {
            try {
                if (clipboardContents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    String returnText = (String) clipboardContents.getTransferData(DataFlavor.stringFlavor);
                    return returnText;
                }
            } catch (Exception e) {
                System.err.print("Failed to get clipboard contents: " + e);
            }
        }
        return null;
    }

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
}
