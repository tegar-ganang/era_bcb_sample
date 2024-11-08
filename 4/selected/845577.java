package net.sf.refactorit.common.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.net.URLDecoder;

public class FileCopier {

    public void copy(File from, File to) throws IOException {
        OutputStream out = null;
        InputStream in = null;
        try {
            in = new FileInputStream(from);
            out = new BufferedOutputStream(new FileOutputStream(to));
            FileCopier.pump(in, out, 8192, false);
            out.flush();
        } finally {
            if (in != null) {
                in.close();
                in = null;
            }
            if (out != null) {
                out.flush();
                out.close();
                out = null;
            }
        }
    }

    public boolean canRead(File f) {
        return f.canRead();
    }

    public boolean canOpenInputStream(File f) {
        try {
            new FileReader(f).close();
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public boolean renameTo(File from, File to) {
        return from.renameTo(to);
    }

    /**
   * Pumps all contents of input stream to output stream.
   *
   * @param in input stream.
   * @param out output stream.
   * @param bufferSize read buffer size.
   * @param flushAfterWrite <code>true</code> to flush output stream after every
   *        write operation, <code>false</code> if no flushing required.
   *
   * @throws IOException if pumping fails.
   */
    public static void pump(InputStream in, OutputStream out, int bufferSize, boolean flushAfterWrite) throws IOException {
        int totalBytesRead = 0;
        final byte[] buffer = new byte[bufferSize];
        while (true) {
            final int readCount = in.read(buffer);
            if (readCount == -1) {
                break;
            }
            out.write(buffer, 0, readCount);
            totalBytesRead += readCount;
            if (flushAfterWrite) {
                out.flush();
            }
        }
    }

    /**
   * Returns file content in String for File object
   *
   * @param file file to read in
   * @return file content
   */
    public static String readFileToString(final File file) {
        return FileReadWriteUtil.read(file);
    }

    public static String readReaderToString(final Reader reader) throws IOException {
        return FileReadWriteUtil.read(reader);
    }

    /**
   * Writes string into file, replacing existing file.
   * @param file file to write
   * @param string file content
   */
    public static void writeStringToFile(File file, String string) {
        FileReadWriteUtil.writeStringToFile(file, string);
    }

    public static void writeStringToWriter(String string, Writer writer) throws IOException {
        FileReadWriteUtil.writeStringToWriter(string, writer);
    }

    /**
   * Appends string to end of file
   * @param file file to append string
   * @param string string to append to that file
   */
    public static synchronized void appendStringToFile(File file, String string) {
        FileReadWriteUtil.appendStringToFile(file, string);
    }

    /**
   * Recursively removes directory and all files in it
   * @param dir directory to remove
   */
    public static void removeDirectory(File dir) {
        if (!dir.exists()) {
            return;
        }
        emptyDirectory(dir);
        dir.delete();
    }

    /**
   * deletes all files and subdir in dir
   * @param dir
   */
    public static void emptyDirectory(final File dir) {
        String[] files = dir.list();
        for (int q = 0; q < files.length; q++) {
            File file = new File(dir, files[q]);
            if (file.isDirectory()) {
                removeDirectory(file);
            } else {
                file.delete();
            }
        }
    }

    /**
   * Gets file that is represented by the JAR URL.
   *
   * @param jarUrl JAR URL.
   *        Something like:
   *        <code>jar:file:/home/test/test.jar!SomeObject.class</code>
   *
   * @return file. Never returns <code>null</code>.
   */
    public static File getFileFromJarUrl(URL jarUrl) {
        final String url = jarUrl.toString();
        final int startIndex = "jar:file:".length();
        final int endIndex = url.lastIndexOf('!');
        String path = null;
        if (endIndex == -1) {
            path = url.substring(startIndex);
        } else {
            path = url.substring(startIndex, endIndex);
        }
        path = URLDecoder.decode(path);
        return new File(path);
    }

    /**
   * Traverses the File object entirely. Goes through all directories under the
   * file object if they exists and calls appropriate methods on listener object.
   *
   * @param file The file object to traverse.
   * @param listener The listener object on what the methods are called by the algorithm
   * to notify about the events (found file, entering/exiting directory).
   * @return The event (STOP_PROCESSING, CONTINUE_PROCESSING, ...) that finished the traverse.
   */
    public static int traverse(File file, FileTreeTraverseListener listener) {
        if (file.isFile()) {
            int fileTraversingStatus = listener.foundFile(file);
            return fileTraversingStatus;
        } else {
            int fileTraversingStatus = listener.enterDirectory(file);
            if (fileTraversingStatus != FileTreeTraverseListener.CONTINUE_PROCESSING) {
                return fileTraversingStatus;
            }
            File[] files = file.listFiles(listener.getFileFilter());
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    fileTraversingStatus = traverse(files[i], listener);
                    if (fileTraversingStatus == FileTreeTraverseListener.STOP_PROCESSING) {
                        return fileTraversingStatus;
                    }
                }
            }
            fileTraversingStatus = listener.exitDirectory(file);
            return fileTraversingStatus;
        }
    }

    public static File getChild(File parent, String childName) {
        File child = new File(parent, childName);
        if (child.exists()) {
            return child;
        }
        return null;
    }

    /** Deletes dirs recursively.
   *
   * @param file dir or single file.
   * @return <code>true</code> if succeeded to remove whole tree.
   */
    public static boolean delete(File file) {
        boolean result = true;
        File[] children = file.listFiles();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                result = delete(children[i]) && result;
            }
        }
        result = file.delete() && result;
        return result;
    }

    public static File findParent(File f, String name) {
        while (true) {
            f = f.getParentFile();
            if (f == null) {
                return null;
            }
            if (f.getName().equals(name)) {
                return f;
            }
        }
    }

    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }
}
