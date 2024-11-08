package net.sf.zip.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.swt.widgets.Display;

public class Util {

    public static void close(InputStream in) {
        try {
            if (in != null) in.close();
        } catch (IOException e) {
        }
    }

    public static void close(OutputStream out) {
        try {
            if (out != null) out.close();
        } catch (IOException e) {
        }
    }

    public static boolean delete(File file) {
        if (file.isDirectory()) {
            String[] children = file.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = delete(new File(file, children[i]));
                if (!success) return false;
            }
        }
        if (file.exists()) return file.delete();
        return true;
    }

    /**
	 * Returns the standard display to be used. The method first checks, if the
	 * thread calling this method has an associated disaply. If so, this display
	 * is returned. Otherwise the method returns the default display.
	 * 
	 * @return Returns the standard display to be used.
	 */
    public static Display getStandardDisplay() {
        Display display;
        display = Display.getCurrent();
        if (display == null) display = Display.getDefault();
        return display;
    }

    public static void copy(File srcDir, File dstDir) throws IOException {
        if (srcDir.isDirectory()) {
            if (!dstDir.exists()) dstDir.mkdir();
            String[] children = srcDir.list();
            for (int i = 0; i < children.length; i++) copy(new File(srcDir, children[i]), new File(dstDir, children[i]));
        } else {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = new FileInputStream(srcDir);
                out = new FileOutputStream(dstDir);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            } finally {
                Util.close(in);
                Util.close(out);
            }
        }
    }
}
