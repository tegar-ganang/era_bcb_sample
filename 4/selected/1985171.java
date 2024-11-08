package de.miij.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class FileUtil {

    /**
	 * Diese Methode kopiert die Datei src nach dest.
	 * 
	 * @param src
	 * @param dest
	 */
    public static void copy(String src, String dest) {
        try {
            copy(new FileInputStream(src), new FileOutputStream(dest));
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public static void writeFile(String fileName, String content) throws Exception {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileName));
        bos.write(content.getBytes());
        bos.close();
    }

    /**
	 * Diese Methode kopiert die Datei im InputStream fis in den OutputStream
	 * fos.
	 * 
	 * @param fis
	 * @param fos
	 */
    public static void copy(InputStream fis, OutputStream fos) {
        try {
            byte buffer[] = new byte[0xffff];
            int nbytes;
            while ((nbytes = fis.read(buffer)) != -1) fos.write(buffer, 0, nbytes);
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            if (fis != null) try {
                fis.close();
            } catch (IOException e) {
            }
            try {
                if (fos != null) fos.close();
            } catch (IOException e) {
            }
        }
    }

    /**
	 * Places a post fix on a filename just before the extension dot.
	 */
    public static String postfixFilename(String filename, String postfix) {
        int index = filename.lastIndexOf(".");
        return (filename.substring(0, index) + postfix + filename.substring(index));
    }

    /**
	 * Places a prefix on the filename. <br>
	 * <br>
	 * Note: Only works for windows style paths.
	 */
    public static String prefixFilename(String path, String prefix) {
        int index = path.lastIndexOf("\\");
        return (path.substring(0, index) + "\\" + prefix + path.substring(index + 1));
    }

    /**
	 * Places an extension on a filename if it does not already exist.
	 * 
	 * @param filename
	 *           Path to the file.
	 * @param ext
	 *           Extension of the file.
	 */
    public static String addExtIfNecessary(String filename, String ext) {
        if (hasExtension(filename, ext)) return (filename);
        return (setExtension(filename, ext));
    }

    /**
	 * Setzt die Endung des Dateinamens
	 * 
	 * @param filename
	 * @param ext
	 * @return
	 */
    public static String setExtension(String filename, String ext) {
        if (hasExtension(filename, ext)) return (filename);
        int index = filename.lastIndexOf(".");
        if (index == -1) return (filename + "." + ext);
        return (filename.substring(0, index + 1) + ext);
    }

    /**
	 * @param filename
	 * @param ext
	 * @return Ob der Dateiname die Extension hat
	 */
    public static boolean hasExtension(String filename, String ext) {
        int index = filename.lastIndexOf(".");
        if (index == -1) return (false);
        return (filename.substring(index + 1).compareTo(ext) == 0);
    }

    /**
	 * @param filename
	 * @return Dateiname ohne Pfad (dasselbe wie File.this.getName())
	 */
    public static String stripPath(String filename) {
        int index = filename.lastIndexOf("\\");
        if (index == -1) return (filename);
        return (filename.substring(index + 1));
    }

    /**
	 * @param filename
	 * @return Den Pfad ohne die Dateiendung
	 */
    public static String stripExtension(String filename) {
        int index = filename.lastIndexOf(".");
        return (filename.substring(0, index));
    }

    public static String getExtension(String filename) {
        int index = filename.lastIndexOf(".");
        return (index != -1 && index < filename.length() - 1) ? filename.substring(index + 1) : "";
    }

    public static String[] getAllFiles(File dir) {
        return getAllFiles(new ArrayList(), dir);
    }

    private static String[] getAllFiles(ArrayList tab, File dir) {
        File[] list = dir.listFiles();
        for (int i = 0; i < list.length; i++) {
            if (list[i].isDirectory()) {
                getAllFiles(tab, list[i]);
            } else {
                tab.add(list[i].getAbsolutePath());
            }
        }
        String[] retVal = new String[tab.size()];
        for (int i = 0; i < retVal.length; i++) {
            retVal[i] = tab.get(i).toString();
        }
        return retVal;
    }

    public static long getSizeOfDirectory(File dir) {
        try {
            if (dir.isFile()) return new RandomAccessFile(dir, "r").length(); else {
                File[] files = dir.listFiles();
                long size = 0;
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isFile()) size += new RandomAccessFile(files[i], "r").length(); else if (files[i].isDirectory()) size += getSizeOfDirectory(files[i]);
                }
                return size;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    public static void removeFilesRecursive(File folder) {
        String[] files = getAllFiles(folder);
        for (int i = 0; i < files.length; i++) {
            new File(files[i]).delete();
        }
    }
}
