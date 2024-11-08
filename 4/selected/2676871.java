package com.sshtools.j2ssh.io;

import java.io.*;

/**
 *
 *
 * @author $author$
 * @version $Revision: 1.20 $
 */
public class IOUtil {

    /**
     *
     *
     * @param in
     *
     * @return
     */
    public static boolean closeStream(InputStream in) {
        try {
            if (in != null) {
                in.close();
            }
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     *
     *
     * @param out
     *
     * @return
     */
    public static boolean closeStream(OutputStream out) {
        try {
            if (out != null) {
                out.close();
            }
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }

    public static boolean delTree(File file) {
        if (file.isFile()) {
            return file.delete();
        } else {
            File[] list = file.listFiles();
            for (int i = 0; i < list.length; i++) {
                if (!delTree(list[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void recurseDeleteDirectory(File dir) {
        File[] files = dir.listFiles(new FileFilter() {

            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        if (files == null) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            recurseDeleteDirectory(files[i]);
            files[i].delete();
        }
        files = dir.listFiles(new FileFilter() {

            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });
        for (int i = 0; i < files.length; i++) {
            files[i].delete();
        }
        dir.delete();
    }

    public static void copyFile(File from, File to) throws IOException {
        if (from.isDirectory()) {
            if (!to.exists()) {
                to.mkdir();
            }
            File[] children = from.listFiles();
            for (int i = 0; i < children.length; i++) {
                if (children[i].getName().equals(".") || children[i].getName().equals("..")) {
                    continue;
                }
                if (children[i].isDirectory()) {
                    File f = new File(to, children[i].getName());
                    copyFile(children[i], f);
                } else {
                    copyFile(children[i], to);
                }
            }
        } else if (from.isFile() && (to.isDirectory() || to.isFile())) {
            if (to.isDirectory()) {
                to = new File(to, from.getName());
            }
            FileInputStream in = new FileInputStream(from);
            FileOutputStream out = new FileOutputStream(to);
            byte[] buf = new byte[32678];
            int read;
            while ((read = in.read(buf)) > -1) {
                out.write(buf, 0, read);
            }
            closeStream(in);
            closeStream(out);
        }
    }

    public static void transfer(InputStream in, OutputStream out) throws IOException {
        try {
            long bytesSoFar = 0;
            byte[] buffer = new byte[65535];
            int read;
            while ((read = in.read(buffer)) > -1) {
                if (read > 0) {
                    out.write(buffer, 0, read);
                    bytesSoFar += read;
                }
            }
        } finally {
            closeStream(in);
            closeStream(out);
        }
    }
}
