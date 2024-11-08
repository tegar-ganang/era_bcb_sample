package org.sf.pkb.util.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import org.sf.pkb.util.P;

public class FileUtil {

    /**
	 * From apache common IOUtil.java
     * Unconditionally close a <code>Channel</code>.
     * <p>
     * Equivalent to {@link Channel#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     *
     * @param channel the Channel to close, may be null or already closed
     */
    private static void closeQuietly(Channel channel) {
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException ioe) {
        }
    }

    /**
     * Copy file;
     * src must be a file
     * dest can be file or directory
     * @param src
     * @param dest
     * @param preserveFileDate
     * @throws IOException
     */
    public static void copyFile(File src, File dest, boolean preserveFileDate) throws IOException {
        if (src.exists() && src.isDirectory()) {
            throw new IOException("source file exists but is a directory");
        }
        if (dest.exists() && dest.isDirectory()) {
            dest = new File(dest, src.getName());
        }
        if (!dest.exists()) {
            dest.createNewFile();
        }
        FileChannel srcCH = null;
        FileChannel destCH = null;
        try {
            srcCH = new FileInputStream(src).getChannel();
            destCH = new FileOutputStream(dest).getChannel();
            destCH.transferFrom(srcCH, 0, srcCH.size());
        } finally {
            closeQuietly(srcCH);
            closeQuietly(destCH);
        }
        if (src.length() != dest.length()) {
            throw new IOException("Failed to copy full contents from '" + src + "' to '" + dest + "'");
        }
        if (preserveFileDate) {
            dest.setLastModified(src.lastModified());
        }
    }

    /**
     * Move file;
     * src must be a file
	 * dest can be file or directory
	 * @param src
	 * @param dest
	 * @param overwrite
	 * @throws IOException
	 */
    public static void moveFile(File src, File dest, boolean overwrite) throws IOException {
        if (src.exists() && src.isDirectory()) {
            throw new IOException("source file exists but is a directory");
        }
        File reslDestFile = null;
        if (dest.exists() && dest.isDirectory()) {
            reslDestFile = new File(dest, src.getName());
        } else {
            reslDestFile = dest;
        }
        if (reslDestFile.exists()) {
            if (!overwrite) {
                return;
            }
            reslDestFile.delete();
        }
        src.renameTo(reslDestFile);
    }

    private static final String DOUBLE_SEP = "" + File.separatorChar + File.separatorChar;

    private static final String SINGLE_SEP = "" + File.separatorChar;

    public static String normalizePath(String path) {
        if (File.separatorChar == '/') {
            path = path.replace('\\', File.separatorChar);
        } else if (File.separatorChar == '\\') {
            path = path.replace('/', File.separatorChar);
        }
        while (path.indexOf(DOUBLE_SEP) >= 0) {
            path = P.STR.replace(path, DOUBLE_SEP, SINGLE_SEP, true);
        }
        return path;
    }

    public static String normalizePath(String path, boolean endWithSep) {
        String str = normalizePath(path);
        if (!endWithSep) {
            return str;
        }
        if (endWithSep && !str.endsWith(SINGLE_SEP)) {
            str += SINGLE_SEP;
        }
        return str;
    }
}
