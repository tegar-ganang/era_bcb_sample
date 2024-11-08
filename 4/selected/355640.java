package net.sourceforge.gunner.commons.util;

import java.awt.Image;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;
import javax.swing.ImageIcon;

/** Provides utilities for working with files and their data.
 * When working with files there are many common actions that can become
 * repetitive, which in turn leads to harder to maintain code, due to the continual
 * change to old code. The purpose of this class is to provide a central location
 * in which common operations can be performed on files without the clients needing
 * to know lower level details, abstracting the reading for example allows further
 * down the line for a better, more efficient, algorithm being introduced once
 * and coming into effect many times with no extra changes.
 * @author Adam Barclay
 * @version $Revision: 1.6 $*/
public final class FileUtils {

    /** Unit of measurement as bytes. */
    public static final long BYTE = 1;

    public static final long KILOBYTE = BYTE * 1024;

    public static final long MEGABYTE = KILOBYTE * 1024;

    public static final long GIGABYTE = MEGABYTE * 1024;

    public static final long TERABYTE = GIGABYTE * 1024;

    public static final long PETABYTE = TERABYTE * 1024;

    public static final long EXABYTE = PETABYTE * 1024;

    public static final long ZETTABYTE = EXABYTE * 1024;

    public static final long YOTTABYTE = ZETTABYTE * 1024;

    /** List of measurements in which bytes may be repsented. */
    public static final Measurement[] MEASUREMENTS = { new Measurement(BYTE, " bytes"), new Measurement(KILOBYTE, " KB"), new Measurement(MEGABYTE, " MB"), new Measurement(GIGABYTE, " GB"), new Measurement(TERABYTE, " TB"), new Measurement(PETABYTE, " PB"), new Measurement(EXABYTE, " EB"), new Measurement(ZETTABYTE, " ZB"), new Measurement(YOTTABYTE, " YB") };

    /** Size of the buffers used when accessing a file sequentially (10kb) */
    private static final int SEQUENTIAL_BUF_SIZE = 10240;

    private FileUtils() {
    }

    /** Provides convenience of copying a file to another location.
     * This method is provided to alleviate the repition of code, the use of a
     * central method will mean that continual improvements can be made without
     * the clients worrying about the lower level details.<br />
     * In the case that <tt>to</tt> is a directory the new file will become
     * <tt>to</tt>/<tt>getFileName(from)</tt>
     * @param from file to be copied from, the source
     * @param to location of the new file where the copy will go to
     * @throws FileNotFoundException if a specified file cannot be found
     * @throws IOException if any low-level IO operations fails */
    public static void copyFile(File from, File to) throws FileNotFoundException, IOException {
        requireFile(from);
        requireFile(to);
        if (to.isDirectory()) {
            to = new File(to, getFileName(from));
        }
        FileChannel sourceChannel = new FileInputStream(from).getChannel();
        FileChannel destinationChannel = new FileOutputStream(to).getChannel();
        destinationChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        sourceChannel.close();
        destinationChannel.close();
    }

    /** Provides the ability of moving data from one file to another. The
     * method will basically make a copy of the file ({@link #copyFile(File, File)})
     * and then delete the file in which the data was oriinally located 
     * @param from file to be moved from, the source
     * @param to location of the new file where the copy will go to
     * @throws FileNotFoundException if a specified file cannot be found
     * @throws IOException if any low-level IO operations fails*/
    public static void moveFile(File from, File to) throws FileNotFoundException, IOException {
        copyFile(from, to);
        from.delete();
    }

    /** Recurive search for files that meet a certain criterion. Using File.listFiles()
     * means you will get a list of files &amp; directories located <b>only</b>
     * directly under the given file. This method will perform the same sort of
     * operation with two important differences. Firstly this method works
     * recursively instead of only on one branch of a directory structure. Secondly
     * no actual directories will be listed.
     * @param top top level directory to start search from
     * @param filter fileter that will check for validity of given files
     * @param files a List into which the found files will be put into
     * @throws IllegalArgumentException if <tt>top</tt> is not a directory. */
    public static void listFiles(File top, FileFilter filter, List files) {
        requireDirectory(top);
        File[] children = top.listFiles();
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (child.isDirectory()) {
                listFiles(child, filter, files);
            } else if (filter.accept(child)) {
                files.add(child);
            }
        }
    }

    /** Makes sure that a given file exists. The
     * file will be created if there is no existing representation
     * of the given pathname.<br />
     * This method will only return false if a problem has occured at the lower
     * level, <tt>file.createNewFile()</tt>
     * returns <tt>false</tt>.
     * @param file pathname to check for existance
     * @throws IOException if unable to create file
     */
    public static void createFile(File file) throws IOException {
        if (file.exists()) {
            return;
        }
        if (!file.createNewFile()) {
            throw new IOException("Unable to create " + file.getName());
        }
    }

    /** Makes sure that a given pathname (<tt>File</tt>) is created. The
     * directory will be created if there is no existing representation
     * of the given pathname.<br />
     * This method will only return false if a problem has occured at the lower
     * level, <tt>file.mkdirs()</tt>
     * returns <tt>false</tt>.
     * @param file pathname to check for existance
     * @throws IOException if unable to create directory
     */
    public static void createDirectory(File file) throws IOException {
        if (file.exists()) {
            return;
        }
        if (!file.mkdirs()) {
            throw new IOException("Unable to create " + file.getName());
        }
    }

    /** Compares the contents of two <b>files</b> for equality. The two files
     * will be compared byte for byte, whereby if any byte is different the result
     * will be that of <tt>false</tt>.<br />
     * When comparing two options are available, either a slower sequential access
     * check or fast load-once comparison. The options will be chosen depending on
     * the amount of memory that can be used, as specified by <tt>maxSize<tt>.
     * If the total amount of memory consumed using the read-once method is greater
     * than that of <tt>maxSize</tt> the less memory intensive sequential access method
     * is used.<br />
     * If one file is non-existant no checking will be done, <tt>false</tt> will be returned.
     * If both files are non-existant <tt>true</tt> will be returned.
     * @param file file to check for
     * @param other file to check against
     * @param maxSize maximum size (in bytes) of the two loaded files combined
     * @return <tt>true</tt> is both files contents are exactly the same or both do
     * <b>not</b> exist
     * @throws IOException if any IO exception occurs
     * @throws IllegalArgumentException if either file is a directory
     */
    public static boolean contentsAreEqual(File file, File other, int maxSize) throws IOException {
        requireFile(file);
        requireFile(other);
        boolean fileExists = file.exists();
        boolean otherExists = other.exists();
        if (!(fileExists == otherExists)) {
            return false;
        }
        if (!fileExists) {
            return true;
        }
        if (!(file.length() == other.length())) {
            return false;
        }
        long totalSize = file.length() + other.length();
        if (totalSize <= maxSize || totalSize <= SEQUENTIAL_BUF_SIZE * 2) {
            return Arrays.equals(readFile(file), readFile(other));
        } else {
            return contentsAreEqualSequential(file, other);
        }
    }

    /** Compares two given files for equality using a sequential access method.
     * This method uses less memory (at most <tt>SEQUENTIAL_BUF_SIZE</tt> * 2 bytes)
     * than if two files were to be read totally and compared.
     * @param file first file to check
     * @param other second file to check
     * @return whether or not these two files contain exactly the same contents
     * @throws IOException if any I/O exception occurs during reading
     */
    private static boolean contentsAreEqualSequential(File file, File other) throws IOException {
        FileChannel fileChannel = new FileInputStream(file).getChannel();
        FileChannel otherChannel = new FileInputStream(other).getChannel();
        ByteBuffer fileBuf = ByteBuffer.allocate(SEQUENTIAL_BUF_SIZE);
        ByteBuffer otherBuf = ByteBuffer.allocate(SEQUENTIAL_BUF_SIZE);
        boolean success = true;
        while (true) {
            fileChannel.read(fileBuf);
            otherChannel.read(otherBuf);
            if (!(fileBuf.equals(otherBuf))) {
                success = false;
                break;
            }
            if (fileBuf.position() == 0) {
                break;
            }
            fileBuf.clear();
            otherBuf.clear();
        }
        fileChannel.close();
        otherChannel.close();
        return success;
    }

    /** Completely deletes a given <tt>File</tt>. If the pathname
     * represents a file then a simple call to <tt>file.delete()</tt>
     * will be called; in the case of a directory being passed the complete
     * underlying directory structure will be deleted.
     * @param file pathname (file or directory) to delete
     */
    public static void delete(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            for (int i = 0; i < children.length; i++) {
                delete(children[i]);
            }
        }
        file.delete();
    }

    /** Allows the reading of all data located within a file. The file will
     * have all it's data read into a byte array ready for direct use, or to
     * be converted into a more appropiate type (ie.
     * <code>new String(FileUtils.readFile(file))</code>).<br />
     * It is generall advised to avoid this method. The majority of file readings
     * will lead to the parsing of the file sequentially. To read in the complete
     * file can be wasteful of memory in this case, and it's better to provide a
     * custom implementation.
     * @param file file in which the required data is located
     * @return byte array containing all data contained within file
     * @throws FileNotFoundException if a specified file cannot be found
     * @throws IOException if any low-level IO operations fails */
    public static byte[] readFile(File file) throws FileNotFoundException, IOException {
        requireFile(file);
        FileChannel source = new FileInputStream(file).getChannel();
        ByteBuffer buf = ByteBuffer.allocate((int) source.size());
        source.read(buf);
        source.close();
        return buf.array();
    }

    /** Using the classpath makes an attempt to load an image icon. In many
     * applications resources files (ie. images) are kept in jar files. To
     * load these the jar file may be placed into the classpath and the image
     * retreieved via. this method.
     * @param file location and name of file to load
     * @return loaded image, or <tt>null</tt> if it does not exist
     * @throws IllegalArgumentException if <tt>file</tt> is <tt>null</tt> or
     * the file specification points to a directory
     */
    public static ImageIcon loadResourceIcon(String file) {
        Utils.isNotNull(file, "File is null");
        requireFile(new File(file));
        URL image = ClassLoader.getSystemResource(file);
        if (image == null) {
            return null;
        }
        return new ImageIcon(image);
    }

    /** Utility for creating an <tt>Image</tt> form a given resource, using
     * the same loading mechanism as loadReosurceIcon. In some cases an Image
     * is required, this can be achieved through <tt>getImage</tt> of <tt>ImageIcon</tt>
     * which leads to messy code, and also a case whereby a <tt>NullPointerException</tt>
     * could be thrown.
     * @param file location and name of file to load
     * @return loaded image, or <tt>null</tt> if it does not exist
     * @throws IllegalArgumentException if <tt>file</tt> is <tt>null</tt> or
     * the file specification points to a directory. */
    public static Image loadResourceImage(String file) {
        ImageIcon icon = loadResourceIcon(file);
        if (icon == null) {
            return null;
        } else {
            return icon.getImage();
        }
    }

    /** Writes out data to a given file. The method will make sure all necessary
     * initializationhas taken place (ie creating a non-existent file) before
     * writing out the data. The file is totally <b>rewritten</b>, no existing
     * data will be left intact.
     * @param file location into which data will be written
     * @param data data to be written, represented in most basic form (bytes)
     * @throws FileNotFoundException if a specified file cannot be found
     * @throws IOException if any low-level IO operations fails
     */
    public static void writeFile(File file, byte[] data) throws FileNotFoundException, IOException {
        requireFile(file);
        createFile(file);
        FileChannel dest = new FileOutputStream(file).getChannel();
        dest.write(ByteBuffer.wrap(data));
        dest.close();
    }

    /** Makes a check to make sure the given argument is a file (not a directory),
     * throwing an <tt>IllegalArgumentException</tt> if it is.<br />
     * Methods that require a <tt>File</tt> to only point to an actual file can use
     * this method for a convenient way for telling the client via. an appropiate
     * exception of the problem. Clients should make rigourous tests of code, using
     * asserts for developmental purpose, but sections can slip passed this, calling
     * this simple method prevents these bugs going undiscovered.
     * @param file pathname to check against
     * @throws IllegalArgumentException if the given pathname points to a directory
     * @see #requireDirectory(File)
     */
    public static void requireFile(File file) {
        if (file.isDirectory()) {
            throw new IllegalArgumentException("Can only accept files, not directories");
        }
    }

    /** Makes a check to make sure the given argument is a directory (not a  file),
     * throwing an <tt>IllegalArgumentException</tt> if it is.<br />
     * Methods that require a <tt>File</tt> to only point to a directory can use
     * this method for a convenient way for telling the client, via. an appropiate
     * exception, of the problem. Clients should make rigourous tests of code, using
     * asserts for developmental purpose, but sections can slip passed this, calling
     * this simple method prevents these bugs going undiscovered.
     * @param dir pathname to check against
     * @throws IllegalArgumentException if the given pathname points to a file
     * @see #requireFile(File)
     */
    public static void requireDirectory(File dir) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Can only accept directories, not files");
        }
    }

    /** Formats a number, representing bytes into a shortened version using
     * given larger measurements such as megabytes and gigabytes.<br />
     * The returned string will use the shortest (ie. largest) measurement
     * available for a more readable and understandable number for a user to read.
     * If an appropiate size is unavailable the number will be represented in bytes.
     * @param bytes bytes to be converted
     * @return a humam readable short form of the given bytes
     */
    public static String formatBytes(long bytes) {
        int index = 0;
        for (int i = 0; i < MEASUREMENTS.length; i++) {
            long size = MEASUREMENTS[i].getSize();
            if (bytes < size) {
                index = i;
            } else {
                break;
            }
        }
        Measurement measure = MEASUREMENTS[index];
        return Long.toString(bytes / measure.getSize()) + measure.getAbbreviation();
    }

    /**
     * Checks whether the given pathname represents a <b>file</b> with a given
     * extension.<br />
     * The extensions passed should <b>not</b> contain a period, this is automtically
     * added. If an extra period is present then the results will be incorrect due to
     * incorrectly looking for a file ending with '..<i>ext</i>'.<br />
     * <b>NB:</b> No directories are allowed, this method will return false in a case
     * of a directory being passed
     * @param file file to check for
     * @param ext extension to check for
     * @return whether the given file has the extensions <i>ext</i>
     */
    public static boolean hasExtension(File file, String ext) {
        return file.isFile() && getExtension(file).equals(ext);
    }

    /** Extracts the extension from a filename. The extension is anything
     * after the last period (<i>.</i>). For example:<br />
     * <ul>
     *  <li>C:\path\ -> ""</li>
     *  <li>C:\a.png -> png</li>
     * </ul>
     * @param file pathname to look for extension
     * @return extension of file minus the period, or an empty string is the file
     * is actually a directory or not extension exists
     */
    public static String getExtension(File file) {
        if (file.isDirectory()) {
            return "";
        }
        String path = file.getName();
        int index = path.lastIndexOf(".");
        if (index == -1) {
            return "";
        } else {
            return path.substring(index + 1);
        }
    }

    /** Extracts the name of a file (that not including any directory
     * structure) from a given pathname. For example:<br />
     * <ul>
     *  <li>C:\path\ -> ""</li>
     *  <li>C:\a.png -> a.png</li>
     * </ul>
     * @param file pathname ot check
     * @return the name of the file minus any directory structure, or an
     * empty string if the file represents a directory
     */
    public static String getFileName(File file) {
        if (file.isDirectory()) {
            return "";
        }
        String path = file.getName();
        int index = path.lastIndexOf(File.separatorChar);
        if (index == -1) {
            return "";
        } else {
            return path.substring(index + 1);
        }
    }

    /** Extracts the directory structure of a pathname. For example:<br />
     * <ul>
     *  <li>C:\path\ -> C:\path\</li>
     *  <li>C:\a.png -> C:\</li>
     * </ul>
     * @param file pathname to check
     * @return the directory structure of a pathname, minus any file name that may
     * be contained within the path
     */
    public static String getDirectoryName(File file) {
        if (file.isDirectory()) {
            return file.getName();
        } else {
            return file.getParentFile().getName();
        }
    }

    /** Represents a measurement of bytes. When the number of bytes become
     * very large a shortened form is usually represented to the user via. the
     * use of a larger measurement such as megabytes. A <tt>Measurement</tt> holds
     * the abbreviation and size of a single unit for one of these measurements.
     * @author Adam Barclay
     * @version $Revision: 1.6 $
     */
    public static class Measurement {

        /** The size of one of the units of measurement */
        private final long size;

        /** The shorthand form fo writing this unit. Most measurements
         * will use a two letter abbreviation, such as MB for Megabytes
         */
        private final String abbrev;

        public Measurement(long size, String abbrev) {
            this.size = size;
            this.abbrev = abbrev;
        }

        /**
         * @return the abbreviation used to identify this measurement
         */
        public String getAbbreviation() {
            return abbrev;
        }

        /**
         * @return the number of bytes this measurement consists of
         */
        public long getSize() {
            return size;
        }
    }
}
