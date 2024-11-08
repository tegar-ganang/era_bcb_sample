package com.myjavatools.lib;

import static com.myjavatools.lib.Bytes.toBytes;
import static com.myjavatools.lib.Strings.join;
import static com.myjavatools.lib.Strings.write;
import static com.myjavatools.lib.foundation.Objects.isEmpty;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.myjavatools.lib.foundation.Iterators.EmptyIterator;

/**
 * Files is a utility class that contains static methods for handling files and directories
 *
 * @version 5.0, 02/08/05
 * @since 5.0
 */
public abstract class Files {

    private static final boolean DEBUG = true;

    private static final char altSeparatorChar = File.separatorChar == '/' ? '\\' : '/';

    /**
   * Calculates full path of a file
   * @param file
   * @return full path
   */
    public static final String getFullPath(final File file) {
        try {
            return file.getCanonicalPath();
        } catch (final IOException ex) {
        }
        return file.getAbsolutePath();
    }

    /**
   * Calculates full path of a file by its path
   * @param path
   * @return full path
   */
    public static final String getFullPath(final String path) {
        return getFullPath(new File(path));
    }

    /**
   * Calculates relative path
   *
   * @param dir directory path that is expected to start the file path
   * @param path file path, relativer or not
   * @return if path starts with dir, then the rest of the path, else path
   *
   * <br><br><b>Examples</b>:
   * <li><code>relPath("c:\\MyHome\\dev", "c:\\MyHome\\dev\\src\\java")</code> returns "src\\java";</li>
   * <li><code>relPath("/home/zaphod", "/home/zaphod/jbuilder8/samples/welcome")</code> returns "jbuilder8/samples/welcome";</li>
   * <li><code>relPath("/home/zaphod", "/home/ford/jbuilder8")</code> returns "/home/ford/jbuilder8".</li>
   */
    public static String relPath(final String dir, final String path) {
        final String fullpath = getFullPath(path);
        final String fulldir = getFullPath(dir);
        if (!fullpath.startsWith(fulldir + File.separatorChar)) {
            return path;
        }
        final String result = fullpath.substring(fulldir.length() + 1);
        if ((dir.indexOf(File.separatorChar) < 0) && (path.indexOf(File.separatorChar) < 0) && ((dir.indexOf(altSeparatorChar) >= 0) || (path.indexOf(altSeparatorChar) >= 0))) {
            return result.replace(File.separatorChar, altSeparatorChar);
        }
        return result;
    }

    /**
   * Having a directory and file path (relative or absolute) calculates full path
   *
   * @param currentDir directory path
   * @param filepath file path, relative or not
   * @return full file path
   *
   * <br><br><b>Examples</b>:
   * <li><code>path("c:\\MyHome\\dev", "src\\java")</code> returns "c:\\MyHome\\dev\\src\\java";</li>
   * <li><code>path("/root/inetd", "/home/zaphod/jbuilder8/samples/welcome")</code> returns "/home/zaphod/jbuilder8/samples/welcome";</li>
   * <li><code>path("\\Program Files", "c:\\MyHome\\dev")</code> returns "c:\\MyHome\\dev".</li>
   */
    public static String path(final String currentDir, final String filepath) {
        return ((filepath.charAt(0) == File.separatorChar) || (filepath.charAt(0) == altSeparatorChar) || (filepath.indexOf(':') > 0) || isEmpty(currentDir)) ? filepath : (currentDir + (currentDir.endsWith(File.separator) ? "" : File.separator) + filepath);
    }

    /**
   * Splits a path into directory name and file name
   *
   * @param path
   * @return String array consisting of two elements
   *
   * <br><br><b>Examples</b>:
   * <li><code>splitPath("/home/zaphod/jbuilder8/samples/welcome")</code> returns {"/home/zaphod/jbuilder8/samples", "welcome"};</li>
   * <li><code>splitPath("src.java")</code> returns {".", "src.java"};</li>
   * <li><code>splitPath("MyHome\\dev")</code> returns {"MyHome", "dev"}.</li>
   */
    public static String[] splitPath(final String path) {
        return new String[] { dirname(path), new File(path).getName() };
    }

    /**
   * Calculates directory path for a file (like in Perl)
   *
   * @param file
   * @return directory path
   *
   * Unlike java.io.File.getParent(), never returns null (see example 2 below).
   *
   * <br><br><b>Examples</b>:
   * <li><code>dirname(new File("/home/zaphod/jbuilder11/samples/welcome"))</code> returns "/home/zaphod/jbuilder8/samples";</li>
   * <li><code>dirname(new File("src.java"))</code> returns ".";</li>
   * <li><code>dirname(new File("MyHome\\dev"))</code> returns "MyHome".</li>
   */
    public static String dirname(final File file) {
        String parent = file.getParent();
        if (parent == null) {
            parent = ".";
        }
        if ((file.getPath().indexOf(File.separatorChar) < 0) && (file.getPath().indexOf(altSeparatorChar) >= 0) && (parent.indexOf(File.separatorChar) >= 0)) {
            parent = parent.replace(File.separatorChar, altSeparatorChar);
        }
        return parent;
    }

    /**
   * Calculates directory path by file path (like in Perl)
   *
   * @param path
   * @return directory path
   *
   * Unlike java.io.File.getParent(), never returns null (see example 2 below).
   *
   * <br><br><b>Examples</b>:
   * <li><code>dirname("/home/zaphod/jbuilder11/samples/welcome")</code> returns "/home/zaphod/jbuilder8/samples";</li>
   * <li><code>dirname("src.java")</code> returns ".";</li>
   * <li><code>dirname("MyHome\\dev")</code> returns "MyHome".</li>
   */
    public static String dirname(final String path) {
        final String dirname = dirname(new File(path));
        if ((path.indexOf(altSeparatorChar) >= 0) && (path.indexOf(File.separatorChar) < 0)) {
            return dirname.replace(File.separatorChar, altSeparatorChar);
        }
        return dirname;
    }

    /**
   * Calculates filename by file path (like in Perl)
   *
   * @param path
   * @return file name
   *
   * <br><br><b>Example</b>:
   * <li><code>filename("/home/zaphod/jbuilder11/samples/welcome")</code> returns "welcome".</li>
   */
    public static String filename(final String path) {
        return new File(path).getName();
    }

    /**
   * Lists recursively files and directories with name matching a regexp
   *
   * @param subdir where to look
   * @param pattern to match
   * @return List&lt;String> of absolute filepaths
   *
   * <br><br><b>Example</b>:
   * <li><code>find(new File("."), Pattern.compile(".*les\\.java$")))</code> returns
   * Arrays.asList(new String[] {new File("Files.java").getCanonicalPath()}).</li>
   */
    public static List<String> find(final File subdir, final Pattern pattern) {
        final List<String> resultSet = new ArrayList<String>();
        final File contents[] = subdir.listFiles();
        for (final File file : contents) {
            String path = getFullPath(file);
            if (file.isDirectory()) {
                resultSet.addAll(find(file, pattern));
            } else if (pattern.matcher(path).find()) {
                resultSet.add(path);
            } else {
                path = path.replace(File.separatorChar, '/');
                if (pattern.matcher(path).find()) {
                    resultSet.add(path);
                }
            }
        }
        return resultSet;
    }

    /**
   * Lists recursively files and directories with name matching a regexp
   *
   * @param subdir where to look
   * @param pattern to match
   * @return List&lt;String> of absolute filepaths
   *
   * <br><br><b>Example</b>:
   * <li><code>find(".", Pattern.compile(".*les\\.java$")))</code> returns
   * Arrays.asList(new String[] {"Files.java"}).</li>
   */
    public static List<String> find(final String subdir, final Pattern pattern) {
        return find(new File(subdir), pattern);
    }

    /**
   * Lists recursively files and directories with name matching a regexp
   *
   * @param subdir where to look
   * @param pattern to match
   * @return List&lt;String> of absolute filepaths
   *
   * <br><br><b>Example</b>:
   * <li><code>find(".", ".*les\\.java$")</code> returns
   * Arrays.asList(new String[] {"Files.java"}).</li>
   */
    public static List<String> find(final String subdir, final String pattern) {
        try {
            return find(subdir, Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
        } catch (final Exception e) {
            return new ArrayList<String>();
        }
    }

    public static final int FIND_FILE = 1;

    public static final int FIND_DIRECTORY = 2;

    public static final int FIND_ALL = 3;

    /**
   * Finds latest file or directory or one of these which name matches a pattern
   *
   * @param subdir where to look
   * @param pattern to match
   * @param whatExactly can be FIND_FILE or FIND_DIRECTORY or FIND_ALL
   * @return the path found
   */
    public static String findLatest(final String subdir, final String pattern, final int whatExactly) {
        String currentFile = null;
        long currentTime = 0;
        for (final String path : find(subdir, pattern)) {
            final File candidate = new File(path);
            final boolean isGood = ((candidate.isDirectory() ? FIND_DIRECTORY : candidate.isFile() ? FIND_FILE : 0) & whatExactly) != 0;
            if ((currentTime < candidate.lastModified()) && isGood) {
                try {
                    currentTime = candidate.lastModified();
                    currentFile = candidate.getCanonicalPath();
                } catch (final Exception e) {
                }
            }
        }
        return currentFile;
    }

    /**
   * Finds latest file or directory which name matches a pattern
   *
   * @param subdir where to look
   * @param pattern to match
   * @return the path found
   */
    public static String findLatest(final String subdir, final String pattern) {
        return findLatest(subdir, pattern, FIND_ALL);
    }

    /**
   * Finds latest file which name matches a pattern
   *
   * @param subdir where to look
   * @param pattern to match
   * @return the path found
   */
    public static String findLatestFile(final String subdir, final String pattern) {
        return findLatest(subdir, pattern, FIND_FILE);
    }

    /**
   * Finds latest directory which name matches a pattern
   *
   * @param subdir where to look
   * @param pattern to match
   * @return the path found
   */
    public static String findLatestDirectory(final String subdir, final String pattern) {
        return findLatest(subdir, pattern, FIND_DIRECTORY);
    }

    /**
   * directoryFilter is a FileFilter that accepts directories only
   */
    public static FileFilter DIRECTORY_FILTER = new FileFilter() {

        public boolean accept(File file) {
            return file.isDirectory();
        }
    };

    /**
   * fileFilter is a FileFilter that accepts files only
   */
    public static FileFilter fileFilter = new FileFilter() {

        public boolean accept(File file) {
            return file.isFile();
        }
    };

    /**
   * Lists subdirectories of a directory
   * @param dir directory name
   * @return an array of subdirectores
   */
    public static final File[] listSubdirectories(final File dir) {
        return dir.isDirectory() ? dir.listFiles(DIRECTORY_FILTER) : null;
    }

    /**
   * Lists files in a directory
   * @param dir directory name
   * @return an array of files
   */
    public static final File[] listFiles(final File dir) {
        return dir.isDirectory() ? dir.listFiles(fileFilter) : null;
    }

    /**
   * Gets file modification date/time as a string
   * @param file
   * @return file modification time as a string
   *
   * <br><br><b>Example</b>:
   * <li><code>lastModified(new File("src/com/javatools/util/Objects.java"))</code> returns
   * "something".</li>
   */
    public static final String lastModified(final File file) {
        return (new Date(file.lastModified())).toString();
    }

    /**
   * Gets current directory path
   *
   * @return the current directory path
   */
    public static String getcwd() {
        final File here = new File(".");
        try {
            return here.getCanonicalPath();
        } catch (final Exception e) {
        }
        ;
        return here.getAbsolutePath();
    }

    /**
   * Deletes a file or a directory (with all its contents, they say it is dangerous)
   * @param filename
   * @return true if successful
   *
   * <br><br><b>Bad Example</b>:
   * <li><code>deleteFile("/etc")</code> returns true if the program runs as root.</li>
   */
    public static boolean deleteFile(final String filename) {
        return deleteFile(new File(filename));
    }

    /**
   * Deletes a file or a directory (with all its contents, they say it is dangerous)
   * @param file to delete
   * @return true if successful
   *
   */
    public static boolean deleteFile(final File file) {
        try {
            if (file.isDirectory()) {
                final String fullpath = file.getCanonicalPath();
                for (final String filename : file.list()) {
                    deleteFile(new File(fullpath, filename));
                }
            }
            return !file.exists() || file.delete();
        } catch (final Exception e) {
        }
        return false;
    }

    /**
   * Creates or opens a file for output.
   * If subdirectories in the path do not exist, they are created too.
   * If the file exists, it is overwritten, unless <code>append</code> is <b>true</b>.
   * <code>append</code> determines whether to open in <i>append</i> mode
   *
   * @param dirname file location
   * @param filename the name of the file
   * @param append <b>true</b> if open in <i>append</i> mode
   * @return file output stream
   * @throws IOException
   */
    public static FileOutputStream makeFile(final String dirname, final String filename, final boolean append) throws IOException {
        if (isEmpty(dirname)) {
            return new FileOutputStream(new File(filename), append);
        } else {
            final File dir = new File(dirname);
            if (!dir.isDirectory()) {
                if (dir.exists()) {
                    dir.delete();
                }
                dir.mkdirs();
            }
            return new FileOutputStream(new File(dirname, filename), append);
        }
    }

    /**
   * Creates or opens a file for output.
   * If subdirectories in the path do not exist, they are created too.
   * If the file exists, it is overwritten.
   *
   * @param dir file location
   * @param filename the name of the file
   * @return file output stream
   * @throws IOException
   */
    public static FileOutputStream makeFile(final String dir, final String filename) throws IOException {
        return makeFile(dir, filename, false);
    }

    /**
   * Creates or opens a file for output.
   * If subdirectories in the path do not exist, they are created too.
   * If the file exists, it is overwritten, unless <code>append</code> is <b>true</b>.
   * <code>append</code> determines whether to open in <i>append</i> mode
   *
   * @param path [0] is directory name, [1] is file name
   * @param append <b>true</b> if open in <i>append</i> mode
   * @return file output stream
   * @throws IOException
   */
    public static FileOutputStream makeFile(final String[] path, final boolean append) throws IOException {
        return makeFile(path[0], path[1], append);
    }

    /**
   * Creates or opens a file for output.
   * If subdirectories in the path do not exist, they are created too.
   * If the file exists, it is overwritten.
   *
   * @param path String[] - a compound file path, ending with file name
   * @return file output stream
   * @throws IOException
   */
    public static FileOutputStream makeFile(final String... path) throws IOException {
        return path.length < 1 ? null : path.length < 2 ? makeFile(path[0]) : path.length < 3 ? makeFile(path[0], path[1]) : makeFile(join(File.separator, path));
    }

    /**
   * Creates or opens a file for output.
   * If subdirectories in the path do not exist, they are created too.
   * If the file exists, it is overwritten, unless <code>append</code> is <b>true</b>.
   * <code>append</code> determines whether to open in <i>append</i> mode
   *
   * @param path file path
   * @param append <b>true</b> if open in <i>append</i> mode
   * @return file output stream
   * @throws IOException
   */
    public static FileOutputStream makeFile(final String path, final boolean append) throws IOException {
        return makeFile(splitPath(path), append);
    }

    /**
   * Creates or opens a file for output.
   * If subdirectories in the path do not exist, they are created too.
   * If the file exists, it is overwritten.
   *
   * @param path file path
   * @return file output stream
   * @throws IOException
   */
    public static FileOutputStream makeFile(final String path) throws IOException {
        return makeFile(splitPath(path));
    }

    /**
   * Creates or opens a file for output.
   * If subdirectories in the path do not exist, they are created too.
   * If the file exists, it is overwritten, unless <code>append</code> is <b>true</b>.
   * <code>append</code> determines whether to open in <i>append</i> mode
   *
   * @param file the file to open
   * @param append <b>true</b> if open in <i>append</i> mode
   * @return file output stream
   * @throws IOException
   */
    public static FileOutputStream makeFile(final File file, final boolean append) throws IOException {
        return makeFile(file.getCanonicalPath(), append);
    }

    /**
   * Creates or opens a file for output.
   * If subdirectories in the path do not exist, they are created too.
   * If the file exists, it is overwritten.
   *
   * @param file the file to open
   * @return file output stream
   * @throws IOException
   */
    public static FileOutputStream makeFile(final File file) throws IOException {
        return makeFile(file.getCanonicalPath());
    }

    /**
   * Creates or opens a file for output.
   * If subdirectories in the path do not exist, they are created too.
   * If the file exists, it is overwritten.
   *
   * @param path the file to open
   * @param encoding the encoding to use
   * @return output stream writer
   * @throws IOException
   */
    public static final OutputStreamWriter makeFileWriter(final String path, final String encoding) throws IOException {
        return new OutputStreamWriter(makeFile(path), encoding);
    }

    /**
   * Adjusts buffer size according to Moore's law
   * @param size int original buffer size
   * @param thisYear int the year this specific size was chosen
   * @return int buffer size adjusted by Moore's law: "double it every three years"
   */
    public static final int adjustSizeByMooreLaw(final int size, final int thisYear) {
        final double milli = System.currentTimeMillis();
        final double aYear = (double) 1000 * 60 * 60 * 24 * (365 * 4 + 1) / 4;
        final double q = Math.exp((milli / aYear + 1970 - thisYear) / 3 * Math.log(2));
        return (int) (size * q);
    }

    /**
   * Reads the whole reader contents into a string
   *
   * @param reader the reader to read
   * @return contents as a string, or null if error occurred
   *
   */
    private static final int MAX_BUFFER_SIZE = Files.adjustSizeByMooreLaw(65536, 2004);

    public static final String readString(final Reader reader) {
        try {
            final StringBuffer buf = new StringBuffer();
            final char[] chars = new char[MAX_BUFFER_SIZE];
            int l;
            while ((l = reader.read(chars)) > 0) {
                buf.append(chars, 0, l);
            }
            return buf.toString();
        } catch (final Exception e) {
        }
        return null;
    }

    /**
   * Reads the whole file into a string
   *
   * @param file the file to read
   * @return file contents as a string, or null if error occurred
   *
   * <br><br><b>Example</b>:
   * <li><code>readStringFromFile("../src/com/myjavatools/utils/Files.java")</code>
   * returns a string starting with "/**\r\n * &lt;p>Title: MyJavaTools: Files handling Tools&lt;/p>\r\n*".</li>
   */
    public static final String readStringFromFile(final File file) {
        try {
            return readString(new FileReader(file));
        } catch (final Exception e) {
            if (DEBUG) {
                System.out.println(e);
            }
        }
        return null;
    }

    /**
    * Reads the whole file into a string
    *
    * @param file the file to read
    * @param encoding the expected encoding
    * @return file contents as a string, or null if error occurred
    *
    */
    public static final String readStringFromFile(final File file, final String encoding) {
        try {
            return readString(new InputStreamReader(new FileInputStream(file), encoding));
        } catch (final Exception e) {
        }
        return null;
    }

    /**
   * Reads the whole file into a string
   *
   * @param filename the file to read
   * @return file contents as a string, or null if error occurred
   *
   * <br><br><b>Example</b>:
   * <li><code>readStringFromFile("../src/com/myjavatools/utils/Files.java")</code>
   * returns a string starting with "/**\r\n * &lt;p>Title: MyJavaTools: Files handling Tools&lt;/p>\r\n*".</li>
   */
    public static final String readStringFromFile(final String filename) {
        return readStringFromFile(new File(filename));
    }

    /**
   * Reads the whole input stream into a byte array
   *
   * @param is input stream to read
   * @return file contents as byte array, or null if error occurred
   *
   * <br><br><b>Example</b>:
   * <li><code>readBytesFromStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5}))</code>
   * returns new byte[] {1, 2, 3, 4, 5}.</li>
   */
    public static final byte[] readBytesFromStream(final InputStream is) {
        try {
            final ArrayList<byte[]> chunkList = new ArrayList<byte[]>();
            int total = 0;
            int l;
            while ((l = is.available()) > 0) {
                final byte[] chunk = new byte[l];
                is.read(chunk);
                chunkList.add(chunk);
                total += l;
            }
            final byte[] buffer = new byte[total];
            final int pos = 0;
            for (final byte[] chunk : chunkList) {
                java.lang.System.arraycopy(chunk, 0, buffer, pos, chunk.length);
            }
            return buffer;
        } catch (final Exception e) {
        }
        return null;
    }

    /**
   * Reads the whole input stream into a byte array
   *
   * @param filename file to read
   * @return file contents as byte array, or null if error occurred
   *
   * <br><br><b>Example</b>:
   * <li><code>readBytesFromFile("../src/com/myjavatools/utils/Files.java")</code>
   * returns a byte array starting with {51, 50, 50, 13, 10, 32, 50}.</li>
   */
    public static final byte[] readBytesFromFile(final String filename) {
        try {
            final File file = new File(filename);
            final long fullsize = file.length();
            if (fullsize > Integer.MAX_VALUE) {
                throw new IOException("File too large");
            }
            final FileChannel channel = new FileInputStream(file).getChannel();
            final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            final byte[] result = new byte[(int) fullsize];
            buffer.get(result);
            return result;
        } catch (final Exception e) {
        }
        return null;
    }

    /**
   * Creates a file and writes a char sequence into it
   *
   * @param data the char sequence to write to file
   * @param fileTo file path
   * @return file
   */
    public static final File writeToFile(final CharSequence data, final String fileTo) {
        try {
            final File file = new File(fileTo);
            final OutputStreamWriter sw = new OutputStreamWriter(makeFile(file));
            write(sw, data);
            sw.close();
            return file;
        } catch (final Exception e) {
        }
        return null;
    }

    /**
   * Creates a file and writes chars into it
   *
   * @param data array of characters to write to file
   * @param fileTo file path
   * @return file
   */
    public static final File writeToFile(final char[] data, final String fileTo) {
        try {
            final File file = new File(fileTo);
            final OutputStreamWriter sw = new OutputStreamWriter(makeFile(file));
            sw.write(data);
            sw.close();
            return file;
        } catch (final Exception e) {
        }
        return null;
    }

    /**
   * Creates a file and writes bytes into it
   *
   * @param data array of bytes to append to the end of file
   * @param fileTo file path
   * @return file
   */
    public static final File writeToFile(final byte[] data, final String fileTo) {
        try {
            final File file = new File(fileTo);
            final OutputStream os = makeFile(file);
            os.write(data);
            os.close();
            return file;
        } catch (final Exception e) {
        }
        return null;
    }

    /**
   * Writes bytes to a file
   *
   * @see #writeToFile(byte[],String)
   * @return file
   */
    public static final File writeBytesToFile(final byte[] data, final String fileTo) {
        return writeToFile(data, fileTo);
    }

    /**
   * Creates a file and copies into it bytes from an input stream
   *
   * @param is input stream
   * @param fileTo file path
   * @throws IOException
   * @return file
   */
    public static final File writeToFile(final InputStream is, final String fileTo) throws IOException {
        final File file = new File(fileTo);
        final OutputStream os = makeFile(file);
        pipe(is, os, false);
        os.close();
        return file;
    }

    /**
   * Appends a char sequence to the end of a file
   *
   * @param data char sequence to append to the end of file
   * @param fileTo file path
   * @return file
   */
    public static final File appendToFile(final CharSequence data, final String fileTo) {
        try {
            final File file = new File(fileTo);
            final OutputStreamWriter sw = new OutputStreamWriter(makeFile(file, true));
            write(sw, data);
            sw.close();
            return file;
        } catch (final Exception e) {
        }
        return null;
    }

    /**
   * Appends chars to the end of a file
   *
   * @param data array of characters to append to the end of file
   * @param fileTo file path
   * @return file
   */
    public static final File appendToFile(final char[] data, final String fileTo) {
        try {
            final File file = new File(fileTo);
            final OutputStreamWriter sw = new OutputStreamWriter(makeFile(file, true));
            sw.write(data);
            sw.close();
            return file;
        } catch (final Exception e) {
        }
        return null;
    }

    /**
   * Appends bytes to the end of a file
   *
   * @param data array of characters to append to the end of file
   * @param fileTo file path
   * @return file
   */
    public static final File appendToFile(final byte[] data, final String fileTo) {
        try {
            final File file = new File(fileTo);
            final OutputStream os = makeFile(file, true);
            os.write(data);
            os.close();
            return file;
        } catch (final Exception e) {
        }
        return null;
    }

    /**
   * Appends bytes to the end of a file
   *
   * @param data chars to append are converted to bytes
   * @param fileTo
   * @return file
   */
    public static final File appendBytesToFile(final char[] data, final String fileTo) {
        return appendBytesToFile(toBytes(data), fileTo);
    }

    /**
   * Appends bytes to a file
   * @see #appendToFile(byte[],String)
   */
    public static final File appendBytesToFile(final byte[] data, final String fileTo) {
        return appendToFile(data, fileTo);
    }

    /**
   * Calculates a java package name by directory name and base directory name
   *
   * @param basePath the base path for code source
   * @param currentPath the path of current directory
   * @return package name
   *
   * <br><br><b>Examples</b>:
   * <li><code>getPackageName("c:\\home\\myjavatools\\src", "c:\\home\\myjavatools\\src\\com\\myjavatools\\util")</code>
   * returns "com.myjavatools.util".</li>
   * <li><code>getPackageName("c:\\home\\myjavatools\\src\\java", "c:\\home\\myjavatools\\src\\com\\myjavatools\\util")</code>
   * returns null.</li>
   *
   */
    public static final String getPackageName(final String basePath, final String currentPath) {
        final String path = relPath(basePath, currentPath);
        return path == null ? null : path.equals(currentPath) ? null : path.replace(File.separatorChar, '.');
    }

    /**
   * <p>Description: The interface is used to define filters for
   * filtering data in pipes. Filters, similar to those in JSPs, can modify
   * the bytes going from one end of the pipe to another, or just sniff them
   * and act based on results - e.g. count bytes, calculate crc, you name it.</p>
   */
    public interface ByteFilter {

        /**
     * filters data coming from input
     * @param input byte[] input data
     * @param length int number of meaningful bytes
     * @return byte[] result of filtering
     */
        byte[] filter(byte[] input, int length);
    }

    /**
   * <p>Description: Buffering filter stores some data, and these data can be
   * retrieved later.
   * @see #ByteFilter
   */
    public interface BufferingFilter extends ByteFilter {

        /**
     * gets data stored as a result of filtering; no assumption regarding the nature of the data.
     * @return byte[]
     */
        byte[] getBuffer();

        /**
     * clears the filter buffer
     */
        void clear();
    }

    /**
   * pipes data from input stream to output stream, possibly pumping them through
   * the filter (if any)
   * @param in InputStream the source of data
   * @param out OutputStream where the output goes, filtered if filter is present, or unfiltered otherwise
   * @param isBlocking boolean whether input is blocking (in this case the maximum amount is read in one operation; for nonblocking in.available() determines how many bytes can be read)
   * @param filter ByteFilter the filter that applies to data; can be null
   * @throws IOException when input or output fails
   *
   * see the test for examples
   */
    public static void pipe(final InputStream in, final OutputStream out, final boolean isBlocking, final ByteFilter filter) throws IOException {
        byte[] buf = new byte[MAX_BUFFER_SIZE];
        int nread;
        int navailable;
        int total = 0;
        synchronized (in) {
            while (((navailable = isBlocking ? buf.length : in.available()) > 0) && ((nread = in.read(buf, 0, Math.min(buf.length, navailable))) >= 0)) {
                if (filter == null) {
                    out.write(buf, 0, nread);
                } else {
                    final byte[] filtered = filter.filter(buf, nread);
                    out.write(filtered);
                }
                total += nread;
            }
        }
        out.flush();
        buf = null;
    }

    /**
   * pipes data from input stream to output stream
   *
   * @param in InputStream the source of data
   * @param out OutputStream where the output goes, filtered if filter is present, or unfiltered otherwise
   * @param isBlocking boolean whether input is blocking (in this case the maximum amount is read in one operation; for nonblocking in.available() determines how many bytes can be read)
   * @throws IOException when input or output fails
   *
   * see the test for examples
   */
    public static void pipe(final InputStream in, final OutputStream out, final boolean isBlocking) throws IOException {
        pipe(in, out, isBlocking, null);
    }

    /**
   * pipes data from input stream to output stream
   *
   * @param in Reader the source of data
   * @param out Writer where the output goes, filtered if filter is present, or unfiltered otherwise
   * @return boolean true if successful, false otherwise
   *
   * see the test for examples
   */
    public static boolean pipe(final Reader in, final Writer out) {
        if (in == null) {
            return false;
        }
        if (out == null) {
            return false;
        }
        try {
            int c;
            synchronized (in) {
                while (in.ready() && ((c = in.read()) > 0)) {
                    out.write(c);
                }
            }
            out.flush();
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    private static final boolean COPY_DEBUG = false;

    /**
   * copies a file or a directory from one directory to another
   * @param from directory from where to copy
   * @param to directory where to copy
   * @param what what to copy (file or directory, recursively)
   * @return true if successful
   *
   * <br><br><b>Example</b>:
   * <li><code>copy("c:\\home\\vlad\\dev", "c:\\home\\vlad\\rtm", "contents.xml")</code></li>
   */
    public static boolean copy(final String from, final String to, final String what) {
        return copy(new File(from, what), new File(to, what));
    }

    /**
   * copy copies a file or a directory from one directory to another
   * @param from directory from where to copy
   * @param to directory where to copy
   * @param what what to copy (file or directory, recursively)
   * @return true if successful
   *
   * <br><br><b>Example</b>:
   * <li><code>copy(new File(myHomeDir, "dev"), new File(myHomeDir, "rtm"), "contents.xml")</code></li>
   */
    public static boolean copy(final File from, final File to, final String what) {
        return copy(new File(from, what), new File(to, what));
    }

    /**
   * copy copies a file or a directory to another
   * @param from
   * @param to
   * @return true if successful
   *
   * <br><br><b>Example</b>:
   * <li><code>copy("c:\\home\\vlad\\dev\\contents.xml", "c:\\home\\vlad\\rtm\\contents.rss")</code></li>
   */
    public static boolean copy(final String from, final String to) {
        return copy(new File(from), new File(to));
    }

    /**
   * copy copies a file or a directory to another
   * @param from
   * @param to
   * @return true if successful
   *
   * <br><br><b>Example</b>:
   * <li><code>copy(new File(myHomeDir, "contents.xml"), new File(mySite, "contents.rss")</code></li>
   */
    public static boolean USE_NIO = true;

    public static boolean copy(final File from, final File to) {
        if (from.isDirectory()) {
            to.mkdirs();
            for (final String name : Arrays.asList(from.list())) {
                if (!copy(from, to, name)) {
                    if (COPY_DEBUG) {
                        System.out.println("Failed to copy " + name + " from " + from + " to " + to);
                    }
                    return false;
                }
            }
        } else {
            try {
                final FileInputStream is = new FileInputStream(from);
                final FileChannel ifc = is.getChannel();
                final FileOutputStream os = makeFile(to);
                if (USE_NIO) {
                    final FileChannel ofc = os.getChannel();
                    ofc.transferFrom(ifc, 0, from.length());
                } else {
                    pipe(is, os, false);
                }
                is.close();
                os.close();
            } catch (final IOException ex) {
                if (COPY_DEBUG) {
                    System.out.println("Failed to copy " + from + " to " + to + ": " + ex);
                }
                return false;
            }
        }
        final long time = from.lastModified();
        setLastModified(to, time);
        final long newtime = to.lastModified();
        if (COPY_DEBUG) {
            if (newtime != time) {
                System.out.println("Failed to set timestamp for file " + to + ": tried " + new Date(time) + ", have " + new Date(newtime));
                to.setLastModified(time);
                final long morenewtime = to.lastModified();
                return false;
            } else {
                System.out.println("Timestamp for " + to + " set successfully.");
            }
        }
        return time == newtime;
    }

    /**
   * Sets the last-modified time of the file or directory named by this
   * abstract pathname.
   *
   * The reason for this specific method is Java bug 4243868:
   * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4243868
   * It does not always work without the trick...
   *
   * @see java.io.File#setLastModified
   *
   * @param file File
   * @param time long
   *
   * @return <code>true</code> if and only if the operation succeeded;
   *          <code>false</code> otherwise
   *
   * @throws  IllegalArgumentException  If the argument is negative
   *
   * @throws  SecurityException
   *          If a security manager exists and its <code>{@link
   *          java.lang.SecurityManager#checkWrite(java.lang.String)}</code>
   *          method denies write access to the named file
   *
   * @since 5.0
   */
    public static boolean setLastModified(final File file, final long time) {
        if (file.setLastModified(time)) {
            return true;
        }
        System.gc();
        return file.setLastModified(time);
    }

    private static final boolean EQUAL_DEBUG = DEBUG;

    /**
   * compares two files or directories, recursively
   * @param left File
   * @param right File
   * @return boolean
   */
    public static boolean equal(final File left, final File right) {
        if (left.isDirectory() && right.isDirectory()) {
            final Set<String> leftSet = new HashSet<String>(Arrays.asList(left.list()));
            final Set<String> rightSet = new HashSet<String>(Arrays.asList(right.list()));
            if (leftSet.size() != rightSet.size()) {
                if (EQUAL_DEBUG) {
                    System.out.println(left.getPath() + " has " + leftSet.size() + " while " + right.getPath() + " has " + rightSet.size());
                }
                return false;
            }
            for (final String name : leftSet) {
                if (rightSet.contains(name)) {
                    if (!equal(new File(left, name), new File(right, name))) {
                        if (EQUAL_DEBUG) {
                            System.out.println(left.getPath() + File.separator + name + " is different from " + right.getPath() + File.separator + name);
                        }
                        return false;
                    }
                } else {
                    if (EQUAL_DEBUG) {
                        System.out.println(right.getPath() + " does not contain " + name);
                    }
                    return false;
                }
            }
            return true;
        } else if (left.isFile() && right.isFile()) {
            try {
                return compare(left, right) == 0;
            } catch (final IOException e) {
                if (EQUAL_DEBUG) {
                    System.out.println(e.getMessage() + " while comparing " + left.getPath() + " and " + right.getPath());
                }
                return false;
            }
        } else {
            return false;
        }
    }

    /**
   * Compare two files
   * @param left
   * @param right
   * @return -1 if left file is older or shorter or "smaller" than right
   *          0 if files are equal
   *          1 if right file is older or shorter or "smaller" than left
   *
   *
   * <br><br><b>Example</b>:
   * <li><code>copy(new File(myHomeDir, "contents.xml"), new File(mySite, "contents.rss");<br>
   * compare(new File(myHomeDir, "contents.xml"), new File(mySite, "contents.rss")</code> returns -1
   * </li>
   */
    public static int compare(final File left, final File right) throws IOException {
        final long lm = left.lastModified() / 1000;
        final long rm = right.lastModified() / 1000;
        if (lm < rm) {
            return -1;
        }
        if (lm > rm) {
            return 1;
        }
        final long ll = left.length();
        final long rl = right.length();
        if (ll < rl) {
            return -1;
        }
        if (ll > rl) {
            return 1;
        }
        final InputStream is1 = new BufferedInputStream(new FileInputStream(left));
        final InputStream is2 = new BufferedInputStream(new FileInputStream(right));
        for (long i = 0; i < ll; i++) {
            final int b1 = is1.read();
            final int b2 = is2.read();
            if (b1 < 0) {
                return -1;
            }
            if (b2 < 0) {
                return 1;
            }
            if (b1 != b2) {
                return b1 < b2 ? -1 : 1;
            }
        }
        return 0;
    }

    /**
   * synchronizes two directories, <code>left/what</code> and <code>right/what</code>
   * @param left File first directory that contains directory <code>what
   * @param right File second directory that contains directory <code>what
   * @param what String name of directory which contents is being synchronized
   * @return boolean true if success
   *
   * <br><br><b>Example</b>:
   * <li><code>synchronize(new File(myHomeDir), new File(mySite), "myjavatools.com")</code>
   * will leave subdirectories named <code>myjavatools.com</code> in these two directories absolutely identical.</li>
   */
    public static boolean synchronize(final File left, final File right, final String what) {
        return synchronize(new File(left, what), new File(right, what));
    }

    /**
   * synchronizes two directories
   * @param left File first directory
   * @param right File second directory
   * @return boolean true if success
   *
   * <br><br><b>Example</b>:
   * <li><code>synchronize(new File(myHomeDir), new File(myBackupDir))</code>
   * will leave the contents of directories myHomeDIr and myBackupDir absolutely identical.</li>
   */
    public static boolean synchronize(final File left, final File right) {
        if (left.isDirectory() || right.isDirectory()) {
            final String[] leftContents = left.list();
            final Set<String> contents = leftContents == null ? new LinkedHashSet<String>() : new LinkedHashSet<String>(Arrays.asList(leftContents));
            final String[] rightContents = right.list();
            if (rightContents != null) {
                contents.addAll(Arrays.asList(rightContents));
            }
            for (final String name : contents) {
                if (!synchronize(left, right, name)) {
                    return false;
                }
            }
        } else {
            final long leftTime = left.lastModified();
            final long rightTime = right.lastModified();
            if (left.exists() && (!right.exists() || (leftTime < rightTime))) {
                return copy(left, right);
            } else if (right.exists() && (!left.exists() || (leftTime > rightTime))) {
                return copy(right, left);
            }
        }
        return true;
    }

    /**
   * unzips an input stream to a specified folder
   * @param zis ZipInputStream the source of zipped files
   * @param location File the folder (directory) where to unzip the files
   * @throws IOException when something went wrong
   * @return boolean true if success, false otherwise
   *
   *
   * <br><br><b>Example</b>:
   * <li><code>unzip(Web.getUrlInputStream(new URL(synchronize(new File(myHomeDir), new File(myBackupDir))</code>
   * will leave the contents of directories myHomeDIr and myBackupDir absolutely identical.</li>
   */
    public static boolean unzip(final ZipInputStream zis, final File location) throws IOException {
        if (!location.exists()) {
            location.mkdirs();
        }
        ZipEntry ze;
        while ((ze = zis.getNextEntry()) != null) {
            final File output = new File(location, ze.getName());
            if (ze.isDirectory()) {
                output.mkdirs();
            } else {
                final File dir = output.getParentFile();
                if (!dir.isDirectory()) {
                    dir.delete();
                }
                dir.mkdirs();
                if (!dir.exists()) {
                    System.err.println("Could not create directory " + dir.getCanonicalPath());
                    return false;
                }
                final OutputStream os = new FileOutputStream(output);
                pipe(zis, os, true);
                os.close();
            }
        }
        zis.close();
        return true;
    }

    /**
   * installs files from a resource archive
   * Reads a specified resource for aspecified class, unzips it to a specified directory
   * @param clazz Class the class whose package contains the archive as a resource
   * @param resourceArchiveName String the name of resource containing the archive
   * @param location File directory where the archive is unzipped
   * @throws IOException if something goes wrong
   * @return boolean true if success, false if failed
   */
    public static boolean install(final Class clazz, final String resourceArchiveName, final File location) throws IOException {
        final ZipInputStream zis = new ZipInputStream(clazz.getResourceAsStream(resourceArchiveName));
        return unzip(zis, location);
    }

    /**
   * installs files from a resource archive
   * Reads a specified resource for aspecified class, unzips it to a specified directory
   * @param clazz Class the class whose package contains the archive as a resource
   * @param resourceArchiveName String the name of resource containing the archive
   * @param folderName String name of directory where the archive is unzipped
   * @throws IOException if something goes wrong
   * @return boolean true if success, false if failed
   */
    public static boolean install(final Class clazz, final String resourceArchiveName, final String folderName) throws IOException {
        return install(clazz, resourceArchiveName, new File(folderName));
    }

    private static class ByteIterator implements Iterator<Byte> {

        IOException exception = null;

        byte next;

        boolean have = false;

        InputStream is = null;

        private ByteIterator(final InputStream is) {
            this.is = is;
        }

        public boolean hasNext() {
            if (have) {
                return true;
            } else if (is == null) {
                return false;
            } else {
                try {
                    final int input = is.read();
                    if (input < 0) {
                        close();
                    } else {
                        have = true;
                        next = (byte) input;
                    }
                } catch (final IOException ex) {
                    exception = ex;
                    close();
                }
            }
            return is != null;
        }

        public Byte next() {
            if (!hasNext()) {
                throw exception == null ? new NoSuchElementException() : new NoSuchElementException(exception.getMessage());
            }
            have = false;
            return next;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void close() {
            if (is != null) {
                try {
                    is.close();
                } catch (final Exception e) {
                }
            }
            is = null;
        }

        @Override
        protected void finalize() {
            close();
        }
    }

    /**
   * returns an Iterable&lt;Byte> enclosure that scans over the bytes returned by InputStream
   * @param is InputStream the stream to scan
   * @return Iterable&lt;Byte> the enclosure
   *
   * The Iterator returned by the Iterable is a singleton;
   * you cannot expect to get a fresh Iterator by calling
   * iterator() several times.
   *
   * <br><br><b>Example</b>:<br><br>
   * <code>
   *    for(byte b : bytes(new java.net.URL("http://yahoo.com").openSream())) {
   *      System.out.println(b);
   *    }
   * </code>
   */
    public static Iterable<Byte> bytes(final InputStream is) {
        return new Iterable<Byte>() {

            private final Iterator<Byte> iterator = new ByteIterator(is);

            public Iterator<Byte> iterator() {
                return iterator;
            }
        };
    }

    /**
   * returns an Iterable&lt;Byte> that scans over the bytes in a File
   * @param file File the file to scan
   * @return Iterable&lt;Byte> the Iterable
   *
   * <br><br><b>Example</b>:<br><br>
   *
   * <code><pre>
   *    for(byte b : bytes(new File("notepad.exe"))) {
   *      System.out.println(b);
   *    }
   * </pre></code>
   */
    public static Iterable<Byte> bytes(final File file) {
        return new Iterable<Byte>() {

            public Iterator<Byte> iterator() {
                try {
                    return new ByteIterator(new FileInputStream(file));
                } catch (final IOException e) {
                    return new EmptyIterator<Byte>(e.getMessage());
                }
            }
        };
    }

    private static class CharIterator implements Iterator<Character> {

        IOException exception = null;

        Reader reader;

        char next;

        boolean have = false;

        private CharIterator(final Reader reader) {
            this.reader = reader;
        }

        public boolean hasNext() {
            if (have) {
                return true;
            } else if (reader == null) {
                return false;
            } else {
                try {
                    final int input = reader.read();
                    if (input < 0) {
                        close();
                    } else {
                        have = true;
                        next = (char) input;
                        return true;
                    }
                } catch (final IOException ex) {
                    exception = ex;
                    close();
                }
                return reader != null;
            }
        }

        public Character next() {
            if (!hasNext()) {
                throw exception == null ? new NoSuchElementException() : new NoSuchElementException(exception.getMessage());
            }
            have = false;
            return next;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void close() {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final Exception e) {
                }
            }
            reader = null;
        }

        @Override
        protected void finalize() {
            close();
        }
    }

    /**
   * returns an Iterable&lt;Character> enclosure that scans over the characters returned by Reader
   * @param reader Reader the reader to scan
   * @return Iterable&lt;Character> the Iterable enclosure
   *
   * The Iterator returned by the Iterable is a singleton;
   * you cannot expect to get a fresh Iterator by calling
   * iterator() several times.
   * <br>
   * Usage example:<br><br>
   *
   * <code>
   *    for(char c : chars(new InputStreamReader(new java.net.URL("http://yahoo.com").openSream()))) {
   *      System.out.println("[" + c + "]");
   *    }
   * </code>
   */
    public static Iterable<Character> chars(final Reader reader) {
        return new Iterable<Character>() {

            private final Iterator<Character> iterator = new CharIterator(reader);

            public Iterator<Character> iterator() {
                return iterator;
            }
        };
    }

    /**
   * returns an Iterable&lt;Character> that scans over the characters in a File
   * @param file File the file to scan
   * @return Iterable&lt;Character> the Iterable
   * <br>
   * Usage example:<br><br>
   *
   * <code>
   *    for(char c : bytes(new File("readme.html"))) {
   *      System.out.println("[" + c + "]");
   *    }
   * </code>
   */
    public static Iterable<Character> chars(final File file) {
        return new Iterable<Character>() {

            public Iterator<Character> iterator() {
                try {
                    return new CharIterator(new FileReader(file));
                } catch (final IOException e) {
                    return new EmptyIterator<Character>(e.getMessage());
                }
            }
        };
    }

    private static class LineIterator implements Iterator<String> {

        LineNumberReader lr;

        IOException exception = null;

        String next;

        boolean have = false;

        private LineIterator(final Reader reader) {
            lr = new LineNumberReader(reader);
        }

        public boolean hasNext() {
            if (have) {
                return true;
            } else if (lr == null) {
                return false;
            } else {
                try {
                    next = lr.readLine();
                    if (next == null) {
                        close();
                    } else {
                        have = true;
                        return true;
                    }
                } catch (final IOException ex) {
                    exception = ex;
                    close();
                }
                return false;
            }
        }

        public String next() {
            if (!hasNext()) {
                throw exception == null ? new NoSuchElementException() : new NoSuchElementException(exception.getMessage());
            }
            have = false;
            return next;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void close() {
            if (lr != null) {
                try {
                    lr.close();
                } catch (final Exception e) {
                }
            }
            lr = null;
        }

        @Override
        protected void finalize() {
            close();
        }
    }

    /**
   * returns an Iterable&lt;String> enclosure that scans over the lines returned by Reader
   * @param reader Reader the reader to scan
   * @return Iterable&lt;String> the Iterable enclosure
   *
   * The Iterator returned by the Iterable is a singleton;
   * you cannot expect to get a fresh Iterator by calling
   * iterator() several times.
   * <br>
   * Usage example:<br><br>
   *
   * <code>
   *    for(String line : chars(new LineNumberReader(new java.net.URL("http://yahoo.com").openSream()))) {
   *      System.out.println(">" + line);
   *    }
   * </code>
   */
    public static Iterable<String> lines(final Reader reader) {
        return new Iterable<String>() {

            private final Iterator<String> iterator = new LineIterator(reader);

            public Iterator<String> iterator() {
                return iterator;
            }
        };
    }

    /**
   * returns an Iterable&lt;String> that scans over the lines in a File
   * @param file File the file to scan
   * @return Iterable&lt;String> the Iterable
   *
   * Usage example:
   *
   * <code>
   *    for(String line : lines(new File("readme.txt"))) {
   *      System.out.println(">" + line);
   *    }
   * </code>
   */
    public static Iterable<String> lines(final File file) {
        return new Iterable<String>() {

            public Iterator<String> iterator() {
                try {
                    return new LineIterator(new FileReader(file));
                } catch (final IOException e) {
                    return new EmptyIterator<String>(e.getMessage());
                }
            }
        };
    }

    /**
   * Returns an Iterable&lt;File> that scans, recursively, through
   * the directory structure.
   * Traversal order is depth-first, preorder
   *
   * @param folder File starting directory
   * @return Iterable&lt;File> the tree scanner
   *
   * Usage examples:
   *
   * <code>
   *    for(File subfolder : tree(new File("."))) {
   *      System.out.println(subfolder.getCanonicalPath());
   *    }
   *
   *    for(File folder : tree(new File("."))) {
   *      System.out.println(file.getCanonicalPath());
   *
   *      for (File file : files(folder)) {
   *        System.out.println("  " + file.getName());
   *      }
   *    }
   * </code>
   */
    public static Iterable<File> tree(final File folder) {
        return new Iterable<File>() {

            public Iterator<File> iterator() {
                return FolderIterator.preorder(folder);
            }
        };
    }

    /**
   * Returns an Iterable&lt;File> that scans, recursively, through
   * the directory structure.
   * Traversal order is depth-first, preorder
   *
   * @param folder File starting directory
   * @return Iterable&lt;File> the tree scanner
   *
   * Usage examples:
   *
   * <code>
   *    for(File subfolder : tree(new File("."))) {
   *      System.out.println(subfolder.getCanonicalPath());
   *    }
   *
   *    for(File folder : tree(new File("."))) {
   *      System.out.println(file.getCanonicalPath());
   *
   *      for (File file : files(folder)) {
   *        System.out.println("  " + file.getName());
   *      }
   *    }
   * </code>
   */
    public static Iterable<File> tree(final File folder, final FileFilter filter) {
        return new Iterable<File>() {

            public Iterator<File> iterator() {
                return FolderIterator.preorder(folder, filter);
            }
        };
    }

    /**
   * Returns an Iterable&lt;File> that scans, recursively,
   * through the directory structure.
   *
   * Traversal order is depth-first, postorder
   *
   * @param folder File starting directory
   * @return Iterable&lt;File> the tree scanner
   *
   * Usage examples:
   *
   * <code>
   *    for(File subfolder : treePostorder(new File("."))) {
   *      System.out.println(subfolder.getCanonicalPath());
   *    }
   *
   *    for(File folder : treePostorder(new File("."))) {
   *      System.out.println(file.getCanonicalPath());
   *
   *      for (File file : files(folder)) {
   *        System.out.println("  " + file.getName());
   *      }
   *    }
   * </code>
   */
    public static Iterable<File> treePostorder(final File folder) {
        return new Iterable<File>() {

            public Iterator<File> iterator() {
                return FolderIterator.postorder(folder);
            }
        };
    }

    /**
   * Returns an Iterable&lt;File> that scans all files in the folder
   * @param folder File directory to scan
   * @return Iterable&lt;File> the scanner
   * <br>
   * Usage example:<br><br>
   *
   * <code>
   *    for (File file : files(new File(".")) {
   *      System.out.println(file.getName());
   *    }
   * </code>
   */
    public static Iterable<File> files(final File folder) {
        return Arrays.asList(listFiles(folder));
    }
}
