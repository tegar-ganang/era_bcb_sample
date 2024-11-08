package org.systemsbiology.apps.gui.server.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 * Utility for working with Files
 * 
 * @author Mark Christiansen
 *
 */
public class FileUtils {

    private static Logger log = Logger.getLogger(FileUtils.class.getName());

    /**
	 * Example: for the path TestGWT/src/java this should to return TestGWT
	 * @param path path of parent directory
	 * @param separator character to separate with
	 * @return the root parent directory
	 */
    public static String getRootParentDir(String path, char separator) {
        path = removeFirstSeparator(path, separator);
        File file = new File(path);
        while (file.getParentFile() != null) {
            file = file.getParentFile();
        }
        String name = file.getName();
        if (name.length() == 0) name = file.getAbsolutePath();
        name = FileUtils.removeLastSeparator(name, File.separatorChar);
        return name;
    }

    /**
	 * Strips the extension (.xxx) from the fileName and returns the resulting string.
	 * Returns null if fileName is null.
	 * @param fileName name of file
	 * @return String filename without extension
	 */
    public static String removeExtension(String fileName) {
        if (fileName == null) return null;
        int i = fileName.lastIndexOf(".");
        if (i == -1) i = fileName.length();
        return fileName.substring(0, i);
    }

    /**
	 * Replaces the extension in the given fileName with newExt. If fileName does not
	 * have an extension (.xxx), the given extension is appended.
	 * Returns null if fileName is null. Returns fileName if given extension is null.
	 * @param fileName name of file
	 * @param newExt new file extension
	 * @return String file name with replaced extension
	 */
    public static String replaceExtension(String fileName, String newExt) {
        if (fileName == null) return null;
        if (newExt == null) return fileName;
        if (!newExt.startsWith(".")) newExt = "." + newExt;
        return removeExtension(fileName) + newExt;
    }

    /**
	 * Remove the last separator in a string
	 * @param path String to remove last separator from
	 * @param separator character separating the path
	 * @return new String without its last separator
	 */
    public static String removeLastSeparator(String path, char separator) {
        if (path.charAt(path.length() - 1) == separator) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
	 * Remove the first character from path if it begins with the character designated by separator
	 * @param path path with questionable first character
	 * @param separator character to remove from first character in path
	 * @return string without a separator at its first character
	 */
    public static String removeFirstSeparator(String path, char separator) {
        if (path == null || path.length() == 0) return path;
        if (path.charAt(0) == separator) {
            return path.substring(1, path.length());
        }
        return path;
    }

    /**
	 * Delete a directory
	 * @param dir directory to delete
	 * @return <code>true</code> if delete was successful
	 */
    public static boolean deleteDirectory(File dir) {
        if (!dir.exists()) {
            return false;
        }
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) deleteDirectory(file); else file.delete();
        }
        return dir.delete();
    }

    /**
	 * Returns an array of strings naming the files, that end with the given extension,
	 * in the directory denoted by the abstract path name <code>dir</code> 
	 * Returned array is null if a directory denoted by the abstract path name does
	 * not exit.
	 * @param dir directory to get all files for
	 * @param ext extension of interest
	 * @return String[]
	 */
    public static String[] getAllFilesWithExtension(String dir, final String ext) {
        if (ext.startsWith(".")) return listFiles(dir, ext); else return listFiles(dir, "." + ext);
    }

    /**
	 * Returns an array of strings naming the files, that end with the given suffix,
	 * in the directory denoted by the abstract path name <code>dir</code> 
	 * Returned array is null if a directory denoted by the abstract path name does
	 * not exit.
	 * @param dir directory to get all files for
	 * @param fileSuffix file suffix of interest
	 * @return String[] 
	 */
    public static String[] listFiles(String dir, final String fileSuffix) {
        File parentDir = new File(dir);
        if (!parentDir.exists()) {
            log.error("Directory " + dir + "does not exist!");
            return null;
        }
        if (!parentDir.isDirectory()) {
            log.error(dir + " is not a directory!");
            return null;
        }
        String[] files = parentDir.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(fileSuffix);
            }
        });
        return files;
    }

    /**
	 * copy file from one path to another
	 * 
	 * the file in the destination path must not exist,
	 * otherwise return false  
	 * 
	 * @param fromfile from file location
	 * @param tofile to file location
	 * @return boolean
	 */
    public static boolean copyFile(String fromfile, String tofile) {
        File from = new File(fromfile);
        File to = new File(tofile);
        if (!from.exists()) return false;
        if (to.exists()) {
            log.error(tofile + "exists already");
            return false;
        }
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        FileInputStream fis = null;
        FileOutputStream ois = null;
        boolean flag = true;
        try {
            to.createNewFile();
            fis = new FileInputStream(from);
            ois = new FileOutputStream(to);
            in = new BufferedInputStream(fis);
            out = new BufferedOutputStream(ois);
            byte[] buf = new byte[2048];
            int readBytes = 0;
            while ((readBytes = in.read(buf, 0, buf.length)) != -1) {
                out.write(buf, 0, readBytes);
            }
        } catch (IOException e) {
            log.error(e);
            flag = false;
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                log.error(e);
                flag = false;
            }
        }
        return flag;
    }
}
