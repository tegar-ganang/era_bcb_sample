package com.ivis.xprocess.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A number of utility methods for working with files
 *
 */
public class FileUtils {

    private static final String DOT = ".";

    private static final String SLASH = "/";

    /**
     * A handy method to delete a directory even if it is not empty.
     *
     * @param dir
     * @return true if the deletion was successful, otherwise false
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                deleteDir(new File(dir, children[i]));
            }
        }
        return dir.delete();
    }

    /**
     * Delete the contents of a directory but not the actual directory itself.
     *
     * @param dir
     * @return true if the deletion was successful, otherwise false
     */
    public static boolean deleteDirContents(File dir) {
        if (!dir.isDirectory()) {
            return false;
        }
        String[] children = dir.list();
        for (int i = 0; i < children.length; i++) {
            boolean success = deleteDir(new File(dir, children[i]));
            if (!success) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param path
     * @return the path with all the slash characters replaced with dots
     */
    public static String pathToPackage(String path) {
        String packageName = replace(path, SLASH, DOT);
        if (!packageName.endsWith(DOT)) {
            packageName += DOT;
        }
        return packageName;
    }

    /**
     * @param str
     * @param pattern
     * @param replace
     * @return the new String
     */
    public static String replace(String str, String pattern, String replace) {
        int s = 0;
        int e = 0;
        StringBuffer result = new StringBuffer();
        while ((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replace);
            s = e + pattern.length();
        }
        result.append(str.substring(s));
        return result.toString();
    }

    /**
     * A simple method for copying files.
     *
     * @param in
     * @param out
     * @throws IOException
     */
    public static void copyFile(File in, File out) throws IOException {
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[1024];
        int i = 0;
        while ((i = fis.read(buf)) > 0) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
    }

    /**
     * @param path
     * @return a path with the File.separator replaced by "/"
     */
    public static String fixPath(String path) {
        return replace(path, File.separator, SLASH);
    }

    /**
     * @param path
     * @return a path with the "/" character replaced with the File.separator
     */
    public static String fixPathToBackSlash(String path) {
        return replace(path, SLASH, File.separator);
    }

    /**
     * Parses a path into directories. The directories are ordered with the most
     * significant first The path must support File.separator ideally it should
     * have been returned from the file index
     *
     * @param fragment
     * @return an array of paths starting with the most significant
     */
    public static String[] getDirsFromFragment(String fragment) {
        Stack<String> stack = new Stack<String>();
        int lastSlash = fragment.lastIndexOf(File.separator);
        while (lastSlash != -1) {
            fragment = fragment.substring(0, lastSlash);
            stack.push(fragment);
            lastSlash = fragment.lastIndexOf(File.separator);
        }
        String[] paths = new String[stack.size()];
        int pos = 0;
        while (!stack.isEmpty()) {
            paths[pos] = stack.pop();
            pos++;
        }
        return paths;
    }

    /**
     * Copy a whole directory.
     *
     * @param sourceDirectory
     * @param destinationDirectory
     */
    public static void copyDir(File sourceDirectory, File destinationDirectory) {
        if (sourceDirectory.isDirectory()) {
            if (!destinationDirectory.exists()) {
                destinationDirectory.mkdir();
            }
            String[] children = sourceDirectory.list();
            for (int i = 0; i < children.length; i++) {
                copyDir(new File(sourceDirectory, children[i]), new File(destinationDirectory, children[i]));
            }
        } else {
            try {
                copyFile(sourceDirectory, destinationDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Zip a directory.
     *
     * @param dir2zip
     * @param zos
     */
    public static void zipDir(String dir2zip, ZipOutputStream zos) {
        try {
            File zipDir = new File(dir2zip);
            String[] dirList = zipDir.list();
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String filePath = f.getPath();
                    zipDir(filePath, zos);
                    continue;
                }
                FileInputStream fis = new FileInputStream(f);
                ZipEntry anEntry = new ZipEntry(f.getPath());
                zos.putNextEntry(anEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                fis.close();
            }
        } catch (Exception e) {
        }
    }

    /**
     * Zip files.
     *
     * @param zipFilePath
     * @param filenames
     */
    public static void zipFiles(String zipFilePath, String[] filenames) {
        byte[] buf = new byte[1024];
        try {
            String outFilename = zipFilePath;
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));
            for (int i = 0; i < filenames.length; i++) {
                File file = new File(filenames[i]);
                if (file.exists()) {
                    FileInputStream in = new FileInputStream(filenames[i]);
                    out.putNextEntry(new ZipEntry(filenames[i]));
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.closeEntry();
                    in.close();
                }
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String convertFileSeparators(String path) {
        if (File.separator.equals("\\")) {
            path = path.replaceAll("/", File.separator);
        } else {
            path = path.replaceAll("\\\\", File.separator);
        }
        return path;
    }
}
