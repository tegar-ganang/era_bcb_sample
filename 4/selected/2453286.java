package com.bayareasoftware.chartengine.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class FileUtil {

    /**
     * deeply delete a directory's children but leave the directory intact
     * @param dir
     * @return
     */
    public static int deepDeleteChildren(File dir) {
        int deleted = 0;
        if (dir.isDirectory()) {
            File[] fs = dir.listFiles();
            for (File f : fs) {
                if (f.isDirectory()) {
                    deleted += deepDelete(f);
                } else {
                    boolean del = f.delete();
                    if (!del) {
                        throw new RuntimeException("cannot delete file " + f.getAbsolutePath());
                    }
                    deleted++;
                }
            }
        }
        return deleted;
    }

    /**
     * deeply delete a directory, including all its parents 
     * @param dir
     * @return number of files deleted
     */
    public static int deepDelete(File dir) {
        int deleted = deepDeleteChildren(dir);
        boolean del = !dir.exists() || dir.delete();
        if (!del) {
            throw new RuntimeException("cannot delete file/directory " + dir.getAbsolutePath());
        }
        return deleted;
    }

    /**
     * read the contents of the file into a single string
     * (file should not be too large)
     * @param f
     * @return null if there was a problem reading the file
     */
    public static String readAsString(File f) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            int x = (int) f.length();
            byte b[] = new byte[x];
            fis.read(b);
            return new String(b);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        } finally {
            close(fis);
        }
    }

    public static String readStreamAsString(InputStream is) throws IOException {
        StringBuilder ret = new StringBuilder();
        Reader rdr = new InputStreamReader(is);
        try {
            char[] buf = new char[2048];
            int r;
            while ((r = rdr.read(buf)) > 0) ret.append(buf, 0, r);
        } finally {
            rdr.close();
        }
        return ret.toString();
    }

    public static void writeFile(File f, Writer out) throws IOException {
        Reader rdr = new FileReader(f);
        char[] buf = new char[2048];
        int r;
        try {
            while ((r = rdr.read(buf)) > 0) out.write(buf, 0, r);
        } finally {
            rdr.close();
        }
    }

    public static String relativePath(File root, File sub) throws IOException {
        String ret = null;
        String rp = null, sp = null;
        rp = root.getCanonicalPath();
        sp = sub.getCanonicalPath();
        if (sp.length() > rp.length() && sp.startsWith(rp)) {
            ret = sp.substring(rp.length() + 1);
        }
        return ret;
    }

    public static void copyDirectory(File src, File dst, boolean recurse) throws IOException {
        dst.mkdirs();
        if (!dst.isDirectory()) {
            throw new IOException("cannot make target directory: " + dst.getAbsolutePath());
        }
        File[] files = src.listFiles();
        for (File f : files) {
            String name = f.getName();
            if (!".svn".equals(name)) {
                if (f.isFile()) {
                    copyFile(f, new File(dst, name));
                } else if (f.isDirectory() && recurse) {
                    copyDirectory(f, new File(dst, name), true);
                }
            }
        }
    }

    public static void copyFile(File src, File dst) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(src);
            os = new FileOutputStream(dst);
            byte[] buf = new byte[1024];
            int r;
            while ((r = is.read(buf)) > 0) {
                os.write(buf, 0, r);
            }
        } finally {
            close(is);
            close(os);
        }
    }

    public static void close(Object o) {
        if (o == null) {
            return;
        }
        try {
            if (o instanceof InputStream) {
                ((InputStream) o).close();
            } else if (o instanceof OutputStream) {
                ((OutputStream) o).close();
            } else if (o instanceof Writer) {
                ((Writer) o).close();
            } else if (o instanceof Reader) {
                ((Reader) o).close();
            }
        } catch (IOException ignore) {
        }
    }

    /**
     * write the contents of the string into a file
     * @param f
     * @param s
     * @return true if successful
     */
    public static boolean writeString(File f, String s) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(f);
            fw.write(s);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            close(fw);
        }
        return true;
    }
}
