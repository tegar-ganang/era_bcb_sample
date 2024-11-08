package de.schlund.pfixxml.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * This class contains a set of commonly used utility methods for the files.
 * 
 * @author mleidig@schlund.de
 *
 */
public class FileUtils {

    /**
     * Reads a text file into a string and replaces each substring that matches the regular
     * expression by a given replacement. Then the changed string is stored back to the file.
     * 
     * @param file - the text file
     * @param encoding - the file content's encoding
     * @param regexp - the regular expression to match substrings
     * @param replacement - the replacement for matched substrings
     */
    public static void searchAndReplace(File file, String encoding, String regexp, String replacement) {
        try {
            String content = load(file, encoding);
            content = content.replaceAll(regexp, replacement);
            save(content, file, encoding);
        } catch (IOException x) {
            throw new RuntimeException("Search and replace failed due to IO error", x);
        }
    }

    /**
     * Read a text file into a string.
     * 
     * @param file - the text file
     * @param encoding - text file content's encoding
     * @return the file content as string
     * @throws IOException
     */
    public static String load(File file, String encoding) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader reader = new InputStreamReader(fis, encoding);
        StringBuffer strBuf = new StringBuffer();
        char[] buffer = new char[4096];
        int i = 0;
        try {
            while ((i = reader.read(buffer)) != -1) strBuf.append(buffer, 0, i);
        } finally {
            fis.close();
        }
        return strBuf.toString();
    }

    /**
     * Saves a string to a text file.
     * 
     * @param fileContent - the file content string
     * @param file - the target file
     * @param encoding - the text encoding
     * @throws IOException
     */
    public static void save(String fileContent, File file, String encoding) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(fos, encoding);
        try {
            writer.write(fileContent);
            writer.flush();
        } finally {
            fos.close();
        }
    }

    /**
     * Copies files from source to destination dir. The files can be filtered by name using one
     * or more regular expressions (e.g. ".*gif", ".*jpg").
     * 
     * @param srcDir source directory
     * @param destDir destination directory
     * @param regexps regular expressions for file names
     * @throws IOException
     */
    public static void copyFiles(File srcDir, File destDir, String... regexps) throws IOException {
        if (!(srcDir.exists() && srcDir.isDirectory())) throw new IllegalArgumentException("Source directory doesn't exist: " + srcDir.getAbsolutePath());
        if (!(destDir.exists() && destDir.isDirectory())) throw new IllegalArgumentException("Destination directory doesn't exist: " + destDir.getAbsolutePath());
        File[] files = srcDir.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                String name = file.getName();
                boolean matches = true;
                for (String regexp : regexps) {
                    matches = name.matches(regexp);
                    if (matches) break;
                }
                if (matches) {
                    File destFile = new File(destDir, file.getName());
                    copyFile(file, destFile);
                }
            }
        }
    }

    /**
     * Copies a source file to a target file.
     * 
     * @param srcFile source file
     * @param destFile target file
     * @throws IOException
     */
    public static void copyFile(File srcFile, File destFile) throws IOException {
        if (!(srcFile.exists() && srcFile.isFile())) throw new IllegalArgumentException("Source file doesn't exist: " + srcFile.getAbsolutePath());
        if (destFile.exists() && destFile.isDirectory()) throw new IllegalArgumentException("Destination file is directory: " + destFile.getAbsolutePath());
        FileInputStream in = new FileInputStream(srcFile);
        FileOutputStream out = new FileOutputStream(destFile);
        byte[] buffer = new byte[4096];
        int no = 0;
        try {
            while ((no = in.read(buffer)) != -1) out.write(buffer, 0, no);
        } finally {
            in.close();
            out.close();
        }
    }

    /**
     * Recursively deletes directory.
     * 
     * @param file directory to delete
     * @return true if directory was deleted
     */
    public static boolean delete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                delete(files[i]);
            }
        }
        return file.delete();
    }
}
