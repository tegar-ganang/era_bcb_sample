package net.sourceforge.processdash.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

public class FileUtils {

    public static long computeChecksum(File file, Checksum verify) throws IOException {
        return computeChecksum(new FileInputStream(file), verify, true);
    }

    public static long computeChecksum(InputStream stream, Checksum verify, boolean close) throws IOException {
        InputStream in = new CheckedInputStream(new BufferedInputStream(stream), verify);
        try {
            while (in.read() != -1) ;
        } finally {
            if (close) in.close();
        }
        return verify.getValue();
    }

    /** Utility routine: slurp an entire file from an InputStream. */
    public static byte[] slurpContents(InputStream in, boolean close) throws IOException {
        byte[] result = null;
        ByteArrayOutputStream slurpBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) slurpBuffer.write(buffer, 0, bytesRead);
        result = slurpBuffer.toByteArray();
        if (close) try {
            in.close();
        } catch (IOException ioe) {
        }
        return result;
    }

    /**
     * Read data from an InputStream into an array.
     * 
     * This method is similar in function to the InputStream.read(byte[])
     * method, but differs in an important way.  That method may read fewer 
     * bytes than the actual length of the array; it then returns the actual
     * number of bytes read.  This method will perform as many reads as
     * needed to fill the array completely; if any of those reads fail, or
     * if the end-of-stream is reached before the array is filled, this will
     * throw an IOException.
     * 
     * @param in the stream to read from
     * @param data the array where the data should be stored; its length
     *     determines exactly how many bytes are expected.
     * @throws IOException if an error was encountered, or if the end-of-stream
     *     was reached before the array was filled.
     */
    public static void readAndFillArray(InputStream in, byte[] data) throws IOException {
        int length = data.length;
        int bytesRead = 0;
        while (bytesRead < length) {
            int b = in.read(data, bytesRead, length - bytesRead);
            if (b == -1) throw new IOException("Unexpected EOF reading form data"); else bytesRead += b;
        }
    }

    public static void copyFile(File src, File dest) throws IOException {
        InputStream inputStream = new FileInputStream(src);
        try {
            copyFile(inputStream, dest);
        } finally {
            inputStream.close();
        }
    }

    public static void copyFile(File src, String srcEncoding, File dest, String destEncoding) throws IOException {
        InputStreamReader in = new InputStreamReader(new FileInputStream(src), srcEncoding);
        OutputStreamWriter out = new OutputStreamWriter(new RobustFileOutputStream(dest), destEncoding);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        out.flush();
        in.close();
        out.close();
    }

    public static void copyFile(InputStream src, File dest) throws IOException {
        OutputStream outputStream = new FileOutputStream(dest);
        try {
            copyFile(src, outputStream);
        } finally {
            outputStream.close();
        }
    }

    public static void copyFile(File src, OutputStream dest) throws IOException {
        InputStream inputStream = new FileInputStream(src);
        try {
            copyFile(inputStream, dest);
        } finally {
            inputStream.close();
        }
    }

    public static void copyFile(InputStream src, OutputStream dest) throws IOException {
        BufferedInputStream in = new BufferedInputStream(src);
        BufferedOutputStream out = new BufferedOutputStream(dest);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        out.flush();
    }

    public static void safelyClose(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public static void safelyClose(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /** Rename a file.
     * 
     * The File.renameTo method can silently fail and simply return false.
     * This method will retry the operation several times until it succeeds,
     * or will throw an exception if the rename was unsuccessful.
     * 
     * @param src the existing file to rename
     * @param dest the target name for the file
     * @throws IOException if the rename operation does not succeed.
     */
    public static void renameFile(File src, File dest) throws IOException {
        if (dest.exists()) dest.delete();
        boolean success = false;
        for (int i = 0; i < 10; i++) {
            try {
                if (i > 0) Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            if (src.renameTo(dest)) {
                success = true;
                break;
            }
        }
        if (success) {
            for (int i = 0; i < 10; i++) {
                try {
                    if (i > 0) Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                if (dest.exists()) return;
            }
        }
        throw new IOException("Could not rename '" + src + "' to '" + dest + "'");
    }

    public static void deleteDirectory(File dir) throws IOException {
        deleteDirectory(dir, false);
    }

    public static void deleteDirectory(File dir, boolean recurse) throws IOException {
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().equals(".") || files[i].getName().equals("..")) continue; else if (files[i].isDirectory() && recurse) deleteDirectory(files[i], recurse); else files[i].delete();
        }
        dir.delete();
    }

    /** Create a tweaked version of the string that should be ultra-safe to
     * use as part of a filename.
     */
    public static String makeSafe(String s) {
        if (s == null) s = "";
        s = s.trim();
        s = new String(s.getBytes());
        StringBuffer result = new StringBuffer(s);
        for (int i = result.length(); i-- > 0; ) if (-1 == ULTRA_SAFE_CHARS.indexOf(result.charAt(i))) result.setCharAt(i, '_');
        return result.toString();
    }

    private static final String ULTRA_SAFE_CHARS = "abcdefghijklmnopqrstuvwxyz" + "0123456789" + "_" + "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** Create a tweaked version of a string that should be a legal filename,
     * discarding as little data as possible even if that means sacrificing
     * some legibility.
     */
    public static String makeSafeIdentifier(String s) {
        StringBuffer result = new StringBuffer();
        try {
            result.append(URLEncoder.encode(s.trim(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
        }
        StringUtils.findAndReplace(result, "%2f", ",");
        StringUtils.findAndReplace(result, "%2F", ",");
        StringUtils.findAndReplace(result, "%5c", ",");
        StringUtils.findAndReplace(result, "%5C", ",");
        StringUtils.findAndReplace(result, "%3a", ";");
        StringUtils.findAndReplace(result, "%3A", ";");
        StringUtils.findAndReplace(result, "*", "%2a");
        return result.toString();
    }

    /**
     * Perform a task similar to File.list(), but recurse into subdirectories.
     * 
     * @param directory
     *                the starting directory
     * @param filter
     *                a filter that can determine whether files should be
     *                included in the result. This filter will be called in a
     *                particular way:
     *                <ul>
     *                <li>the <code>dir</code> parameter will always be
     *                passed the same value that was passed into this method.
     *                Thus, it will contain the base of the search, rather than
     *                the deepest subdirectory containing the file in question</li>
     *                <li>for files in subdirectories, the <code>name</code>
     *                parameter will begin with a slash-separated list of
     *                subdirectory names. Thus, it will still indicate the name
     *                of the file relative to the base directory. Forward
     *                slashes will always be used, regardless of the
     *                platform-specific separator character.</li>
     *                <li>The filter will also be called for each subdirectory,
     *                as an inquiry about whether the logic should recurse into
     *                that subdirectory. Subdirectory names will be indicated
     *                by a terminal "/" character on the <code>name</code>
     *                parameter.</li>
     *                </ul>
     *                As of version 1.14.2, the filter can be null to indicate
     *                that all files and directories should be included.

     * @return a list of filenames in and underneath the given directory that
     *     match the filter.  the forward slash will be used as a directory
     *     separator, regardless of the platform-specific separator character.
     */
    public static final List<String> listRecursively(File directory, FilenameFilter filter) {
        ArrayList<String> result = new ArrayList<String>();
        listFilesRecursively(result, directory, filter, directory, "");
        return result;
    }

    private static void listFilesRecursively(List<String> result, File baseDir, FilenameFilter filter, File dir, String prefix) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            String name = prefix + f.getName();
            if (f.isDirectory()) {
                name = name + "/";
                if (filter == null || filter.accept(baseDir, name)) listFilesRecursively(result, baseDir, filter, f, name);
            } else if (filter == null || filter.accept(baseDir, name)) {
                result.add(name);
            }
        }
    }

    /**
     * Return true if the file exists, can be read, and if its first line
     * starts with a given prefix.
     * 
     * @param f the file to test
     * @param encoding the character encoding used by the file
     * @param prefix the prefix to look for
     * @since 1.14.1
     */
    public static boolean fileContentsStartWith(File f, String encoding, String prefix) {
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(f));
            Reader r = new InputStreamReader(in, encoding);
            for (int i = 0; i < prefix.length(); i++) if (r.read() != prefix.charAt(i)) return false;
            return true;
        } catch (IOException ioe) {
            return false;
        } finally {
            safelyClose(in);
        }
    }
}
