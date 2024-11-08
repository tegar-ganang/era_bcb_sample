package info.jonclark.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convenience methods for deailing with files
 */
public class FileUtils {

    /**
	 * Copies a file using NIO
	 * 
	 * @param in
	 *            The source file
	 * @param out
	 *            The destination file
	 * @throws IOException
	 */
    public static void copyFile(final File in, final File out) throws IOException {
        final FileChannel sourceChannel = new FileInputStream(in).getChannel();
        final FileChannel destinationChannel = new FileOutputStream(out).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
    }

    /**
	 * Inserts the entire contents of a file into an open PrintWriter by reading
	 * each line from <code>in</code> and calling <code>out.println()</code>.
	 * 
	 * @param inFile
	 *            The file to be inserted into out.
	 * @param out
	 *            The destination PrintWriter.
	 * @throws IOException
	 *             If an error is encountered in reading or writing.
	 */
    public static void insertFile(final File inFile, final PrintWriter out) throws IOException {
        final BufferedReader in = new BufferedReader(new FileReader(inFile));
        String line = null;
        while ((line = in.readLine()) != null) out.println(line);
    }

    /**
	 * Inserts the entire contents of a file into an open PrintWriter by reading
	 * each line from <code>in</code> and calling <code>out.println()</code>.
	 * 
	 * @param inFile
	 *            The file to be inserted into out.
	 * @param out
	 *            The destination PrintWriter.
	 * @param A
	 *            prefix for every line in inFile.
	 * @throws IOException
	 *             If an error is encountered in reading or writing.
	 */
    public static void insertFile(final File inFile, final PrintWriter out, final String strLinePrefix) throws IOException {
        final BufferedReader in = new BufferedReader(new FileReader(inFile));
        String line = null;
        while ((line = in.readLine()) != null) out.println(strLinePrefix + line);
    }

    /**
	 * Inserts the entire contents of a file into a StringBuilder by reading
	 * each line from <code>in</code> and calling <code>out.append()</code>.
	 * 
	 * @param inFile
	 *            The file to be inserted into out.
	 * @param out
	 *            The destination StringBuilder.
	 * @throws IOException
	 *             If an error is encountered in reading or writing.
	 */
    public static void insertFile(final File inFile, final StringBuilder out) throws IOException {
        final BufferedReader in = new BufferedReader(new FileReader(inFile));
        String line = null;
        while ((line = in.readLine()) != null) out.append(line + "\n");
        in.close();
    }

    /**
	 * Returns a file with the given path, which is guaranteed to exist on
	 * return
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
    public static File createFileWithPath(final String path) throws IOException {
        File f = new File(path);
        f.getParentFile().mkdirs();
        f.createNewFile();
        return f;
    }

    public static BufferedReader openTextFile(final File f) throws FileNotFoundException {
        return new BufferedReader(new FileReader(f));
    }

    public static PrintStream getFileForWriting(final File f) throws FileNotFoundException {
        return new PrintStream(new FileOutputStream(f));
    }

    /**
	 * Adds a trailing slash to a directory path string if and only if the path
	 * string does not already end with a trailing slash. This ensures that a
	 * filename appended to the end of the string will function as a filename
	 * and not an invalid directory name.
	 * 
	 * @param str
	 *            The path string
	 * @return The path string guaranteed to end with a slash
	 */
    public static String forceTrailingSlash(final String str) {
        assert str != null : "str cannot be null";
        if (str.indexOf('/') != str.length() - 1) return str + "/"; else return str;
    }

    /**
	 * Adds a leading ./ to a file path string if and only if the path string is
	 * not an absolute path. This is necessary for relative paths that contain
	 * only one file if the getParentFile() method of File class is to work
	 * properly.
	 * 
	 * @param str
	 * @return
	 */
    public static String forceLeadingDotSlash(String str) {
        if (!isAbsolutePath(str)) return "./" + str; else return str;
    }

    /**
	 * Determines whether a path string is a relative or absolute path.
	 * 
	 * @param str
	 *            The path string to be analyzed
	 * @return True iff the file is a UNIX, DOS, or Windows absolute path
	 */
    public static boolean isAbsolutePath(final String str) {
        if (str.startsWith("/")) return true;
        if (str.length() >= 3) {
            return (str.charAt(1) == ':' && (str.charAt(2) == '/' || str.charAt(2) == '\\'));
        } else {
            return false;
        }
    }

    /**
	 * Get files having the specified extention within the specified root
	 * directory.
	 * 
	 * @param ext
	 *            e.g. ".txt"
	 * @return
	 */
    public static File[] getFilesWithExt(final File root, final String... exts) {
        assert root != null : "root must not be null";
        final FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                for (final String ext : exts) {
                    if (name.endsWith(ext)) return true;
                }
                return false;
            }
        };
        return root.listFiles(filter);
    }

    public static File[] getFilesWithExt(File root, boolean scanSubdirs, String... exts) throws IOException {
        if (scanSubdirs) {
            ArrayList<File> files = new ArrayList<File>();
            ArrayList<File> subdirs = getSubdirectoriesRecursively(root);
            for (final File subdir : subdirs) {
                for (final File file : getFilesWithExt(subdir, exts)) {
                    files.add(file);
                }
            }
            return files.toArray(new File[files.size()]);
        } else {
            return getFilesWithExt(root, exts);
        }
    }

    public static boolean isAbsoltePath(String path) {
        return (path.length() >= 1 && path.charAt(0) == '/') || (path.length() >= 2 && path.charAt(1) == ':');
    }

    public static File[] getFilesFromWildcard(String wildcard) throws IOException {
        wildcard = wildcard.replace('\\', '/');
        wildcard = StringUtils.replaceFast(wildcard, "./", "");
        wildcard = StringUtils.replaceFast(wildcard, ".", "\\.");
        wildcard = StringUtils.replaceFast(wildcard, "?", ".");
        wildcard = StringUtils.replaceFast(wildcard, "*", ".+?");
        File parentDirectory;
        if (isAbsolutePath(wildcard)) {
            String parent = StringUtils.substringBefore(wildcard, "/", true);
            wildcard = StringUtils.substringAfter(wildcard, "/");
            parentDirectory = new File(parent);
        } else {
            parentDirectory = new File(".");
        }
        String[] wildcardTokens = StringUtils.tokenize(wildcard, "/");
        Pattern[] patterns = new Pattern[wildcardTokens.length];
        for (int i = 0; i < patterns.length; i++) patterns[i] = Pattern.compile(wildcardTokens[i]);
        ArrayList<File> files = new ArrayList<File>();
        getFilesFromWildcardRecursively(patterns, wildcardTokens, files, parentDirectory, 0);
        return files.toArray(new File[files.size()]);
    }

    private static void getFilesFromWildcardRecursively(Pattern[] patterns, String[] wildcardTokens, final ArrayList<File> files, File parentDirectory, int depth) throws IOException {
        if (!parentDirectory.isDirectory()) {
            throw new IOException("Not a valid directory: " + parentDirectory.getAbsolutePath());
        }
        final Pattern pattern = patterns[depth];
        final String currentToken = wildcardTokens[depth];
        if (depth == patterns.length - 1) {
            final FilenameFilter filter = new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    File file = new File(dir, name);
                    if (file.isFile()) {
                        final Matcher matcher = pattern.matcher(name);
                        return matcher.matches();
                    } else {
                        return false;
                    }
                }
            };
            File[] results = parentDirectory.listFiles(filter);
            for (final File result : results) {
                files.add(result);
            }
        } else {
            final FilenameFilter filter = new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    File file = new File(dir, name);
                    if (file.isDirectory()) {
                        final Matcher matcher = pattern.matcher(name);
                        return matcher.matches();
                    } else {
                        return false;
                    }
                }
            };
            if (currentToken.contains("*") || currentToken.contains(".")) {
                File[] results = parentDirectory.listFiles(filter);
                if (results == null) throw new RuntimeException("null list for results. Either an IO error occurred or the following path is not a directory:" + parentDirectory.getAbsolutePath());
                for (final File result : results) {
                    getFilesFromWildcardRecursively(patterns, wildcardTokens, files, result, depth + 1);
                }
            } else {
                File result = new File(parentDirectory, currentToken);
                getFilesFromWildcardRecursively(patterns, wildcardTokens, files, result, depth + 1);
            }
        }
    }

    public static File[] getNormalFiles(final File root) {
        assert root != null : "root must not be null";
        final FileFilter filter = new FileFilter() {

            public boolean accept(File f) {
                return !f.isDirectory();
            }
        };
        File[] files = root.listFiles(filter);
        return files;
    }

    public static File[] getSubdirectories(final File root) throws IOException {
        assert root != null : "root must not be null";
        final FileFilter filter = new FileFilter() {

            public boolean accept(File f) {
                return f.isDirectory();
            }
        };
        File[] files = root.listFiles(filter);
        if (files == null) throw new IOException("IO Error or not a valid path: " + root.getAbsolutePath());
        return files;
    }

    /**
	 * NOTE: The root directory is included in the returned list.
	 * 
	 * @param root
	 * @return
	 * @throws IOException
	 */
    public static ArrayList<File> getSubdirectoriesRecursively(File root) throws IOException {
        ArrayList<File> allSubdirs = new ArrayList<File>();
        allSubdirs.add(root);
        for (int i = 0; i < allSubdirs.size(); i++) {
            for (final File subdir : getSubdirectories(allSubdirs.get(i))) {
                allSubdirs.add(subdir);
            }
        }
        return allSubdirs;
    }

    public static void saveTextFileFromStream(final File file, final InputStream inStream) throws IOException {
        file.createNewFile();
        final BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
        final PrintWriter out = new PrintWriter(file);
        String line = in.readLine();
        while (line != null) {
            out.write(line);
            line = in.readLine();
        }
        out.close();
        in.close();
    }

    public static void saveFileFromString(final File file, final String str) throws IOException {
        file.createNewFile();
        final PrintWriter out = new PrintWriter(file);
        out.println(str);
        out.close();
    }

    public static String getFileAsString(final File file) throws IOException {
        final StringBuilder builder = new StringBuilder((int) file.length());
        final BufferedReader in = new BufferedReader(new FileReader(file));
        String line;
        while ((line = in.readLine()) != null) {
            builder.append(line + "\n");
        }
        in.close();
        return builder.toString();
    }

    public static String getFileAsString(final File file, Charset encoding) throws IOException {
        final StringBuilder builder = new StringBuilder((int) file.length());
        final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
        String line;
        while ((line = in.readLine()) != null) {
            builder.append(line + "\n");
        }
        in.close();
        return builder.toString();
    }

    public static void addLinesOfFileToCollection(final File file, Collection<String> col) throws IOException {
        final BufferedReader in = new BufferedReader(new FileReader(file));
        String line;
        while ((line = in.readLine()) != null) {
            col.add(line);
        }
        in.close();
    }

    /**
	 * Returns a new file in the same directory as the original, but with a new
	 * extension
	 * 
	 * @param file
	 * @param newExt
	 * @return
	 */
    public static File changeFileExt(File file, String newExt) {
        String newName = StringUtils.substringBefore(file.getName(), ".") + newExt;
        return new File(file.getParentFile(), newName);
    }

    /**
	 * Returns a new file with the same name as the original, but in a different
	 * directory
	 * 
	 * @param file
	 * @param newDir
	 * @return
	 */
    public static File changeFileDir(File file, File newDir) {
        return new File(newDir, file.getName());
    }

    public static void main(String... args) throws Exception {
        File[] files = getFilesFromWildcard("/media/disk/research/corpora/jpen/jp/*/*.txt");
        System.out.println("Found " + files.length + " files");
    }
}
