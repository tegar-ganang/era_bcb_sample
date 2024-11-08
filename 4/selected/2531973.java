package org.jjsc.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;

/**
 * Some file system utilities to access/manage files and folders.
 * @author alex.bereznevatiy@gmail.com
 */
public class FileSystem {

    private FileSystem() {
    }

    /**
	 * Creates directory with name specified within parent one.
	 * If parent is not exists - it will be created. 
	 * @param parent
	 * @param subdir
	 * @return path to newly created directory or <code>null</code>
	 * if creation process fails.
	 */
    public static File mkdir(String parent, String subdir) {
        return mkdir(new File(parent), subdir);
    }

    /**
	 * Creates directory with name specified within parent one.
	 * If parent is not exists - it will be created. 
	 * @param parent
	 * @param subdir
	 * @return path to newly created directory or <code>null</code>
	 * if creation process fails.
	 */
    public static File mkdir(File parent, String subdir) {
        return mkdir(new File(parent, subdir));
    }

    /**
	 * Creates directory with name specified. 
	 * @param dir
	 * @return path to newly created directory or <code>null</code>
	 * if creation process fails.
	 */
    public static File mkdir(String dir) {
        return mkdir(new File(dir));
    }

    /**
	 * Creates directory with path specified. 
	 * @param dir
	 * @return path to newly created directory or <code>null</code>
	 * if creation process fails.
	 */
    public static File mkdir(File dir) {
        if (dir.isDirectory()) {
            return dir;
        }
        if (dir.exists() || !dir.mkdirs()) {
            return null;
        }
        return dir;
    }

    /**
	 * Removes file with path specified. If error occurs or file is not
	 * existent - just put the warning message in the log.
	 * @param file
	 */
    public static void rm(String file) {
        rm(new File(file));
    }

    /**
	 * Removes file with path specified. If error occurs or file is not
	 * existent - just put the warning message in the log.
	 * @param file
	 */
    public static void rm(File file) {
        if (file.isDirectory()) {
            for (String sub : file.list()) {
                rm(new File(file, sub));
            }
        }
        if (!file.delete()) {
            Log.warn("Unable to remove " + file);
        }
    }

    /**
	 * Removes file with name specified in second parameter
	 * located in the directory path specified at first parameter. 
	 * If error occurs or file is not existent - just put 
	 * the warning message in the log.
	 * @param parent
	 * @param child
	 */
    public static void rm(String parent, String child) {
        rm(new File(parent, child));
    }

    /**
	 * @return <code>true</code> if file with name specified 
	 * in second parameter is exists in the directory path 
	 * specified in the first parameter. 
	 * @param parent
	 * @param child
	 */
    public static boolean exists(String parent, String child) {
        return new File(parent, child).exists();
    }

    /**
	 * Writes content represented by stream passed 
	 * to the file with path specified.
	 * @param file
	 * @param stream
	 * @throws IOException
	 */
    public static void write(File file, InputStream stream) throws IOException {
        OutputStream out = write(file);
        try {
            IO.flow(stream, out);
            out.flush();
        } finally {
            out.close();
        }
    }

    /**
	 * Opens connection to the file with path specified.
	 * @param file
	 * @return output stream to write to file.
	 * @throws IOException
	 */
    public static OutputStream write(File file) throws IOException {
        try {
            if (!file.getParentFile().isDirectory()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException ex) {
            Log.error("Error creating file: " + file.toString(), ex);
            throw ex;
        }
        if (!file.isFile() || !file.canWrite()) {
            throw new IOException("Unable to write to file: " + file);
        }
        return new BufferedOutputStream(new FileOutputStream(file));
    }

    /**
	 * Writes content represented by reader passed 
	 * to the file with path specified.
	 * @param file
	 * @param reader
	 * @throws IOException
	 */
    public static void write(File file, Reader reader) throws IOException {
        OutputStreamWriter out = new OutputStreamWriter(write(file));
        try {
            IO.flow(reader, out);
            out.flush();
        } finally {
            out.close();
        }
    }

    /**
	 * Marks the file for deletion. If file is a directory
	 * all child files will be also marked for deletion. 
	 * @param work
	 */
    public static void markForDeletion(File file) {
        if (!file.exists()) return;
        file.deleteOnExit();
        if (!file.isDirectory()) {
            return;
        }
        for (String child : file.list()) {
            if (child.equals(".") || child.equals("..")) {
                continue;
            }
            markForDeletion(new File(file, child));
        }
    }
}
