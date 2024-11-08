package gov.nasa.jpf.util;

import java.io.*;
import java.util.*;

/**
 * Contains methods for easy manipulation of Files.
 */
public final class Files {

    private Files() {
        throw new IllegalStateException("no instances");
    }

    /** 
     * Deletes all files and subdirectories under dir.
     * Returns true if all deletions were successful.
     * If a deletion fails, the method stops attempting to delete and returns false.
     */
    public static boolean deleteDir(File dir) {
        boolean clean = cleanDir(dir);
        if (clean) return dir.delete(); else return false;
    }

    /**
     * Deletes all contents of the directory.
     * Returns whether the deletion succeeded.
     */
    public static boolean cleanDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String element : children) {
                boolean success = deleteDir(new File(dir, element));
                if (!success) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the list of names of files in the given directory, with specified name prefixes and suffixes.
     */
    public static List<String> findFilesInDir(File dir, String startsWith, String endsWith) {
        if (!dir.isDirectory()) throw new IllegalArgumentException("not a directory: " + dir.getAbsolutePath());
        File currentDir = dir;
        List<String> retval = new ArrayList<String>();
        for (String fileName : currentDir.list()) {
            if (fileName.startsWith(startsWith) && fileName.endsWith(endsWith)) retval.add(fileName);
        }
        return retval;
    }

    /**
     * Writes the string to the file.
     */
    public static void writeToFile(String s, File file) throws IOException {
        writeToFile(s, file, false);
    }

    /**
     * Writes the string to the file.
     */
    public static void writeToFile(String s, String fileName) throws IOException {
        writeToFile(s, fileName, false);
    }

    /**
     * Writes the string to the file.
     */
    public static void writeToFile(String s, File file, boolean append) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file, append));
        try {
            writer.append(s);
        } finally {
            writer.close();
        }
    }

    /**
     * Writes the string to the file.
     */
    public static void writeToFile(String s, String fileName, boolean append) throws IOException {
        writeToFile(s, new File(fileName), append);
    }

    /**
     * Writes the string to the file.
     */
    public static void writeToFile(List<String> lines, String fileName) throws IOException {
        writeToFile(CollectionsExt.toStringInLines(lines), fileName);
    }

    /**
     * Reads the whole file. Does not close the reader.
     * Returns the list of lines.
     */
    public static List<String> readWhole(BufferedReader reader) throws IOException {
        List<String> result = new ArrayList<String>();
        String line = reader.readLine();
        while (line != null) {
            result.add(line);
            line = reader.readLine();
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Reads the whole file. Returns the list of lines.
     */
    public static List<String> readWhole(String fileName) throws IOException {
        return readWhole(new File(fileName));
    }

    /**
     * Reads the whole file. Returns the list of lines.
     */
    public static List<String> readWhole(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        try {
            return readWhole(in);
        } finally {
            in.close();
        }
    }

    /**
     * Reads the whole file. Returns the list of lines.
     * Does not close the stream.
     */
    public static List<String> readWhole(InputStream is) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        return readWhole(in);
    }

    /**
     * Reads the whole contents read from the stream. Returns one big String.
     * Closes the stream.
     */
    public static String getFileContents(InputStream is) throws IOException {
        return getFileContents(new BufferedReader(new InputStreamReader(is)));
    }

    /**
     * Reads the whole file. Returns one big String.
     */
    public static String getFileContents(File file) throws IOException {
        if (!file.exists()) return null;
        return getFileContents(new BufferedReader(new FileReader(file)));
    }

    /**
     * Reads the file contents.
     */
    private static String getFileContents(Reader in) throws IOException {
        StringBuilder result = new StringBuilder();
        try {
            int c;
            while ((c = in.read()) != -1) {
                result.append((char) c);
            }
            in.close();
            return result.toString();
        } finally {
            in.close();
        }
    }

    /**
     * Reads the whole file. Returns one big String.
     */
    public static String getFileContents(String path) throws IOException {
        return getFileContents(new File(path));
    }

    public static LineNumberReader getFileReader(String fileName) {
        return getFileReader(new File(fileName));
    }

    public static LineNumberReader getFileReader(File fileName) {
        LineNumberReader reader;
        try {
            reader = new LineNumberReader(new BufferedReader(new FileReader(fileName)));
        } catch (FileNotFoundException e1) {
            throw new IllegalStateException("File was not found " + fileName + " " + e1.getMessage());
        }
        return reader;
    }

    public static String addProjectPath(String string) {
        return System.getProperty("user.dir") + File.separator + string;
    }

    /**
     * Deletes the file.
     */
    public static boolean deleteFile(String path) {
        File f = new File(path);
        return f.delete();
    }

    /**
     * Reads a single long from the file.
     * Returns null if the file does not exist.
     * @throws  IllegalStateException is the file contains not just 1 line or
     *          if the file contains something.
     */
    public static Long readLongFromFile(File file) {
        if (!file.exists()) return null;
        List<String> lines;
        try {
            lines = readWhole(file);
        } catch (IOException e) {
            throw new IllegalStateException("Problem reading file " + file + " ", e);
        }
        if (lines.size() != 1) throw new IllegalStateException("Expected exactly 1 line in " + file + " but found " + lines.size());
        try {
            return Long.valueOf(lines.get(0));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Expected a number (type long) in " + file + " but found " + lines.get(0));
        }
    }

    /**
     * Copies a file byte by byte from toCopy to dest.
     * 
     * @param toCopy the file to copy.
     * @param dest the destination file.
     * @throws IOException
     */
    public static void copy(File toCopy, File dest) throws IOException {
        FileInputStream src = new FileInputStream(toCopy);
        FileOutputStream out = new FileOutputStream(dest);
        try {
            while (src.available() > 0) {
                out.write(src.read());
            }
        } finally {
            src.close();
            out.close();
        }
    }
}
