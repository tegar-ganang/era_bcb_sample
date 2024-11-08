package jgnash.util;

import java.io.*;
import java.nio.channels.*;
import java.util.logging.*;

/**
 * Static methods to copy a file given a source and destination.
 * <p>
 * $Id: CopyFile.java 675 2008-06-17 01:36:01Z ccavanaugh $
 * 
 * @author Craig Cavanaugh
 */
public class CopyFile {

    /** Use static methods only */
    private CopyFile() {
    }

    /** Make a copy of a file given a source and destination.  The source and destination
     *  are locked to help prevent corruption.
     * 
     * @param src Source file
     * @param dest Destination file
     * @return true is the copy was successful
     */
    public static boolean copyFile(final String src, final String dest) {
        if (fileExists(src)) {
            try {
                FileChannel srcChannel = new FileInputStream(src).getChannel();
                FileChannel dstChannel = new FileOutputStream(dest).getChannel();
                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                srcChannel.close();
                dstChannel.close();
                return true;
            } catch (IOException e) {
                Logger.getAnonymousLogger().severe(e.getLocalizedMessage());
            }
        }
        return false;
    }

    /** Checks for the existence of the specified file
     */
    public static boolean fileExists(final String src) {
        if (src != null) {
            File file = new File(src);
            if (file.exists()) {
                return true;
            }
        }
        return false;
    }
}
