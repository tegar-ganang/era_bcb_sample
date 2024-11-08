package net.hussnain.io.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

/**
 *
 * @author  BillGates
 * @version
 */
public class Utilities extends Object {

    public static final String jpeg = "jpeg";

    public static final String jpg = "jpg";

    public static final String gif = "gif";

    public static final String tiff = "tiff";

    public static final String tif = "tif";

    public static final String txt = "txt";

    public static final String htm = "htm";

    public static final String html = "html";

    public static final String rtf = "rtf";

    /** Creates new FileUtilities */
    public Utilities() {
    }

    public static final String parseExtension(File f) {
        String ext = null;
        if (f != null) {
            String s = f.getName();
            int i = s.lastIndexOf('.');
            if (i > 0 && i < s.length() - 1) {
                ext = s.substring(i + 1).toLowerCase();
            }
        }
        return ext;
    }

    /**
     * copy file with stream channels from one location to another
     */
    public static final void copyFile(File source, File target) {
        try {
            FileChannel srcChannel = new FileInputStream(source).getChannel();
            FileChannel dstChannel = new FileOutputStream(target).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } catch (java.io.IOException e) {
        }
    }
}
