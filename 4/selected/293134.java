package application;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Utility methods for File
 * 
 * @version	$Revision: 1.1 $
 * @since	1.0
 *
 */
public class FileUtil {

    /**
	 * Copy a file to a destination directory
     * @param file to copy
     * @param destination file
     * @throws Exception
     */
    public static void copyFile(File file, File destination) throws Exception {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            out = new BufferedOutputStream(new FileOutputStream(destination));
            int c;
            while ((c = in.read()) != -1) out.write(c);
        } finally {
            try {
                if (out != null) out.close();
            } catch (Exception e) {
            }
            try {
                if (in != null) in.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Returns the directory the class was loaded from
     * @param the class
     * @return the directory as a File object
     * @throws Exception
     */
    public static File getCodebaseDirectory(Class<?> cls) throws Exception {
        URL locationURL = cls.getProtectionDomain().getCodeSource().getLocation();
        String location = locationURL.toExternalForm();
        File fileLocation = new File(new URI(location));
        if (location != null && location.endsWith(".jar")) {
            fileLocation = getDirectory(fileLocation.getAbsolutePath());
        }
        return fileLocation;
    }

    /**
     * @return the file extension or <b>null</b> if not found
     */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    /**
     * Return the directory of the specified file namae
     * @param fileName
     * @return File 
     */
    public static File getDirectory(String fileName) {
        File file = new File(fileName);
        File absFile = new File(file.getAbsolutePath());
        return absFile.getParentFile().getAbsoluteFile();
    }

    /**
      * Delete a file. If file is directory delete it and all sub-directories.
      * @param file to delete
      */
    public static void forceDelete(String file) throws Exception {
        forceDelete(new File(file));
    }

    /**
     * Delete a file. If file is directory delete it and all sub-directories.
     * @param file to delete
     */
    public static void forceDelete(File file) throws Exception {
        if (file.isDirectory()) {
            deleteDirectory(file);
            return;
        }
        if (!file.delete()) {
            throw new Exception("File " + file + " unable to be deleted.");
        }
    }

    /**
     * Recursively delete a directory.
     * @param directory to delete
     */
    public static void deleteDirectory(String directory) throws Exception {
        deleteDirectory(new File(directory));
    }

    /**
     * Recursively delete a directory.
     * @param directory to delete
     */
    public static void deleteDirectory(File directory) throws Exception {
        if (!directory.exists()) return;
        cleanDirectory(directory);
        if (!directory.delete()) {
            throw new Exception("Directory " + directory + " unable to be deleted.");
        }
    }

    /**
     * Clean a directory without deleting it.
     * @param directory to clean
     */
    public static void cleanDirectory(String directory) throws Exception {
        cleanDirectory(new File(directory));
    }

    /**
     * Clean a directory without deleting it.
     * @param directory to clean
     */
    public static void cleanDirectory(File directory) throws Exception {
        if (!directory.exists()) {
            throw new IllegalArgumentException(directory + " does not exist");
        }
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory + " is not a directory");
        }
        Exception exception = null;
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                if (file.isDirectory()) {
                    cleanDirectory(file);
                }
                FileUtil.forceDelete(file);
            } catch (Exception e) {
                exception = e;
            }
        }
        if (exception != null) throw exception;
    }

    /**
	 * Unzip the zip file to the specified directory
	 * @param fileName
	 * @param directory name
	 * @throws Exception
	 */
    public static void unzip(String fileName, String directory) throws Exception {
        unzip(new File(fileName), new File(directory));
    }

    /**
	 * Unzip the zip file to the specified directory
	 * @param fileName
	 * @param directory name
	 * @throws Exception
	 */
    public static void unzip(File fileName, File directory) throws Exception {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory + " is not a directory");
        }
        ZipFile zf = new ZipFile(fileName, ZipFile.OPEN_READ);
        for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements(); ) {
            ZipEntry target = (ZipEntry) e.nextElement();
            saveEntry(directory, zf, target);
        }
    }

    private static void saveEntry(File parent, ZipFile zf, ZipEntry target) throws Exception {
        File file = new File(parent, target.getName());
        if (target.isDirectory()) {
            file.mkdirs();
            return;
        }
        File dir = file.getParentFile();
        dir.mkdirs();
        BufferedInputStream bis = new BufferedInputStream(zf.getInputStream(target));
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        int c;
        while ((c = bis.read()) != -1) {
            bos.write((byte) c);
        }
        bis.close();
        bos.close();
    }

    /**
	 * Perform a line-by-line comparison of two text files
	 * @param file1 - file to compare
	 * @param file2 - the other file to compare
	 * @return true if they are completely identical.
	 */
    public static boolean compareTextFiles(File file1, File file2) {
        BufferedReader file1Reader;
        BufferedReader file2Reader;
        try {
            if (file1.length() != file2.length()) return false;
            file1Reader = new BufferedReader(new FileReader(file1));
            file2Reader = new BufferedReader(new FileReader(file2));
            String file1Str;
            String file2Str;
            while ((file1Str = file1Reader.readLine()) != null) {
                file2Str = file2Reader.readLine();
                if (!file1Str.equals(file2Str)) return false;
            }
            file1Reader.close();
            file2Reader.close();
        } catch (Exception exc) {
            return false;
        }
        return true;
    }

    /**
	 * Write the contents of the object to the file
	 * @param fileName
	 * @param object
	 * @throws Exception
	 */
    public static void write(String fileName, Object object) throws Exception {
        OutputStream os = null;
        FileOutputStream fos = new FileOutputStream(fileName);
        if (object instanceof byte[]) {
            os = fos;
            os.write((byte[]) object);
        } else {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(object);
            os = oos;
        }
        os.flush();
        os.close();
    }

    /**
     * private Constructor for FileUtil.
     */
    private FileUtil() {
    }
}
