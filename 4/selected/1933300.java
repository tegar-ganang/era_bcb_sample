package org.geoforge.io.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;
import org.geoforge.lang.util.logging.FileHandlerLogger;

/**
 *
 * @author bantchao
 *
 * email: bantchao_AT_gmail.com
 * ... please remove "_AT_" from the above string to get the right email address
 *
 */
public class GfrFile {

    private static final Logger _LOGGER_ = Logger.getLogger(GfrFile.class.getName());

    static {
        GfrFile._LOGGER_.addHandler(FileHandlerLogger.s_getInstance());
    }

    public static boolean s_delete(File fle) throws Exception {
        if (fle.isDirectory()) {
            File[] fles = fle.listFiles();
            for (File fleCur : fles) {
                if (!GfrFile.s_delete(fleCur)) {
                    String str = "! GfrFile.s_delete(fleCur): " + fleCur.getAbsolutePath();
                    GfrFile._LOGGER_.warning(str);
                    return false;
                }
            }
        }
        boolean bln = fle.delete();
        if (!bln) {
            String str = "! GfrFile.s_delete(fleCur): " + fle.getAbsolutePath();
            GfrFile._LOGGER_.warning(str);
        }
        return bln;
    }

    public static void s_moveDirectory(File fleSource, File fleTarget) throws Exception {
        if (fleSource.renameTo(fleTarget)) return;
        GfrFile.s_copyDirectory(fleSource, fleTarget);
        if (!GfrFile.s_delete(fleSource)) {
            String str = "Failed to delete:" + fleSource.getAbsolutePath();
            GfrFile._LOGGER_.severe(str);
            throw new Exception(str);
        }
    }

    public static void s_copyDirectory(File fleSource, File fleTarget) throws Exception {
        if (fleSource.isDirectory()) {
            if (!fleTarget.exists()) {
                fleTarget.mkdir();
            }
            String[] strsChildSource = fleSource.list();
            for (int i = 0; i < strsChildSource.length; i++) {
                GfrFile.s_copyDirectory(new File(fleSource, strsChildSource[i]), new File(fleTarget, strsChildSource[i]));
            }
        } else {
            InputStream ism = new FileInputStream(fleSource);
            OutputStream osm = new FileOutputStream(fleTarget);
            byte[] buf = new byte[1024];
            int len;
            while ((len = ism.read(buf)) > 0) {
                osm.write(buf, 0, len);
            }
            ism.close();
            osm.close();
        }
    }

    public static void s_copy(FileInputStream fis, FileOutputStream fos) throws Exception {
        FileChannel in = fis.getChannel();
        FileChannel out = fos.getChannel();
        in.transferTo(0, in.size(), out);
        if (in != null) in.close();
        if (out != null) out.close();
    }

    public static void s_copyFromJarred(String strPathRelIn, File fleOut) throws Exception {
        InputStream ism = GfrFile.class.getResourceAsStream("/" + strPathRelIn);
        OutputStream out = new FileOutputStream(fleOut);
        byte buf[] = new byte[1024];
        int len;
        while ((len = ism.read(buf)) > 0) out.write(buf, 0, len);
        out.close();
        ism.close();
    }

    public static void s_copy(File fleIn, File fleOut) throws Exception {
        if (!fleIn.exists()) {
            String str = "! fleIn.exists(), fleIn.getAbsolutePath()=" + fleIn.getAbsolutePath();
            GfrFile._LOGGER_.severe(str);
            throw new Exception(str);
        }
        if (!fleIn.canRead()) {
            String str = "! fleIn.canRead(), fleIn.getAbsolutePath()=" + fleIn.getAbsolutePath();
            GfrFile._LOGGER_.severe(str);
            throw new Exception(str);
        }
        if (!fleIn.isFile()) {
            String str = "! fleIn.isFile(), fleIn.getAbsolutePath()=" + fleIn.getAbsolutePath();
            GfrFile._LOGGER_.severe(str);
            throw new Exception(str);
        }
        GfrFile.s_copy(new FileInputStream(fleIn), new FileOutputStream(fleOut));
    }

    public static void s_copy(String strPathIn, String strPathOut) throws Exception {
        GfrFile.s_copy(new File(strPathIn), new File(strPathOut));
    }

    protected GfrFile() {
    }
}
