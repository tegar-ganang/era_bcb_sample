package net.sf.antdoc;

import java.io.*;
import java.util.*;

/**
 */
public abstract class FileUtils {

    /** the size of the copy buffer */
    public static final int COPY_BUFFER_BYTES = 1024;

    /** */
    public static void copy(InputStream in, OutputStream out) throws java.io.IOException {
        byte buffer[] = new byte[COPY_BUFFER_BYTES];
        for (int read = in.read(buffer); read >= 0; read = in.read(buffer)) {
            out.write(buffer, 0, read);
        }
    }

    /** */
    public static void copy(File in, File out) throws java.io.IOException {
        FileInputStream input = new FileInputStream(in);
        FileOutputStream output = new FileOutputStream(out);
        copy(input, output);
    }

    /** Compute the relative path from one file to another. */
    public static String href(File from, File to) throws java.io.IOException {
        to = to.getCanonicalFile();
        from = from.getCanonicalFile();
        File base = from.getParentFile();
        StringBuilder dots = new StringBuilder();
        while (from != null) {
            StringBuilder path = new StringBuilder(to.getName());
            File rel = to.getParentFile();
            while (rel != null) {
                StringBuilder href = new StringBuilder();
                href.append(dots).append(path);
                File check = new File(base, href.toString());
                check = check.getCanonicalFile();
                if (check.equals(to)) {
                    return href.toString();
                }
                path.insert(0, '/').insert(0, rel.getName());
                rel = rel.getParentFile();
            }
            dots.append("../");
            from = from.getParentFile();
        }
        return null;
    }

    /** Find the common parent directory of two files */
    public static File findCommonParent(File file1, File file2) throws java.io.IOException {
        file1 = file1.getCanonicalFile();
        file2 = file2.getCanonicalFile();
        File parent1 = file1.getParentFile();
        while (parent1 != null) {
            File parent2 = file2.getParentFile();
            while (parent2 != null) {
                if (parent2.equals(parent1)) {
                    return parent1;
                }
                parent2 = parent2.getParentFile();
            }
            parent1 = parent1.getParentFile();
        }
        return null;
    }

    /** */
    public static File[] findFiles(File path) {
        return findFiles(path, null);
    }

    /** */
    public static File[] findFiles(File path, FileFilter filter) {
        Vector<File> files = new Vector<File>();
        addToFiles(files, path, filter);
        return files.toArray(new File[files.size()]);
    }

    /** */
    private static void addToFiles(Vector<File> files, File path, FileFilter filter) {
        if (path.isDirectory()) {
            for (File entry : path.listFiles()) {
                addToFiles(files, entry, filter);
            }
        } else if (path.exists()) {
            if ((filter == null) || (filter.accept(path))) {
                files.add(path);
            }
        }
    }
}
