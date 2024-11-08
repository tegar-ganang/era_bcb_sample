package gpsmate.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * FileTool
 * 
 * Provides common methods for file manipulation (like extension extraction...).
 * 
 * @author longdistancewalker
 */
public class FileTool {

    /**
   * Extracts a file's extension.
   * 
   * @param file
   * @return the extension, or null
   */
    public static String getExtension(File file) {
        return getExtension(file.getName());
    }

    /**
   * Extracts a file's extension.
   * 
   * @param filename
   * @return the extension, or null
   */
    public static String getExtension(String filename) {
        if (filename.contains(".")) return filename.substring(filename.lastIndexOf(".") + 1);
        return null;
    }

    /**
   * Extracts a file's name w/o extension.
   * 
   * @param file
   * @return the pure filename, or null
   */
    public static String getFilename(String file) {
        if (getExtension(file) == null) return file;
        int from = 0;
        if (file.contains(System.getProperty("file.separator"))) from = file.lastIndexOf(System.getProperty("file.separator")) + 1;
        return file.substring(from, file.length() - getExtension(file).length() - 1);
    }

    /**
   * Extracts a file's name and extension.
   * 
   * @param file
   * @return the filename including its extension, or null
   */
    public static String getFilenameWithExtension(File file) {
        return getFilenameWithExtension(file.getAbsolutePath());
    }

    /**
   * Extracts a file's name and extension.
   * 
   * @param file
   * @return the filename including its extension, or null
   */
    public static String getFilenameWithExtension(String file) {
        return getFilename(file) + "." + getExtension(file);
    }

    /**
   * Extracts a file's name w/o extension.
   * 
   * @param file
   * @return the pure filename, or null
   */
    public static String getFilename(File file) {
        return getFilename(file.getName());
    }

    /**
   * Extracts a file's path.
   * 
   * @param filename
   * @return the file's path
   */
    public static String getPath(String filename) {
        if (filename.contains(System.getProperty("file.separator"))) return filename.substring(0, filename.lastIndexOf(System.getProperty("file.separator")));
        return filename;
    }

    /**
   * Removes a directory's content; recursively if requested.
   * 
   * @param path
   * @param recursive
   */
    public static void cleanDirectory(String path, boolean recursive) {
        cleanDirectory(new File(path), recursive);
    }

    /**
   * Copies contents from file "in" to file "out".
   * 
   * @param in
   * @param out
   */
    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    /**
   * Extracts a file's parent directory's name. /foo/bar/fozzle.xy => "bar" will
   * be returned
   * 
   * @param filepath
   * @return name of the parent directory
   */
    public static String getParentDirectory(String filepath) {
        if (!filepath.contains(System.getProperty("file.separator"))) return null;
        if (filepath.endsWith(System.getProperty("file.separator"))) filepath = filepath.substring(0, filepath.length() - 1);
        filepath = filepath.substring(0, filepath.lastIndexOf(System.getProperty("file.separator")));
        if (filepath.contains(System.getProperty("file.separator"))) filepath = filepath.substring(filepath.lastIndexOf(System.getProperty("file.separator")) + 1);
        return filepath;
    }

    /**
   * Delete's all files in a directory (even recursively, if requested).
   * 
   * @param dir
   * @param recursive
   * @return true on success
   */
    public static boolean cleanDirectory(File dir, boolean recursive) {
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory() && recursive) cleanDirectory(file, recursive);
                file.delete();
            }
            return true;
        }
        return false;
    }

    public static String readFile(String file) {
        StringBuilder content = new StringBuilder();
        try {
            FileReader reader = new FileReader(new File(file));
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line = bufferedReader.readLine();
            while (line != null) {
                content.append(line);
                content.append(System.getProperty("line.separator"));
                line = bufferedReader.readLine();
            }
        } catch (FileNotFoundException e) {
            Logger.logException(e);
        } catch (IOException e) {
            Logger.logException(e);
        }
        return content.toString();
    }
}
