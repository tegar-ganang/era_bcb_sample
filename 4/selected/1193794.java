package fr.ens.transcriptome.teolenn.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * This class contains utilty methods.
 * @author Laurent Jourdren
 */
public final class FileUtils {

    /** The default size of the buffer. */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /** The charset to use. */
    private static final String CHARSET = "ISO-8859-1";

    /**
   * Utility method to create fast BufferedReader.
   * @param file File to read
   * @return a BufferedReader
   * @throws FileNotFoundException if the file is not found
   */
    public static final BufferedReader createBufferedReader(final File file) throws FileNotFoundException {
        if (file == null) return null;
        final FileInputStream inFile = new FileInputStream(file);
        final FileChannel inChannel = inFile.getChannel();
        return new BufferedReader(new InputStreamReader(Channels.newInputStream(inChannel)));
    }

    /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread. The created file use ISO-8859-1 encoding.
   * @param file File to write
   * @return a BufferedWriter
   * @throws FileNotFoundException if the file is not found
   */
    public static final UnSynchronizedBufferedWriter createBufferedWriter(final File file) throws FileNotFoundException {
        if (file == null) return null;
        if (file.isFile()) file.delete();
        final FileOutputStream outFile = new FileOutputStream(file);
        final FileChannel outChannel = outFile.getChannel();
        return new UnSynchronizedBufferedWriter(new OutputStreamWriter(Channels.newOutputStream(outChannel), Charset.forName(CHARSET)));
    }

    /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread. The created file use ISO-8859-1 encoding.
   * @param file File to write
   * @return a BufferedWriter
   * @throws IOException if an error occurs while creating the Writer
   */
    public static final UnSynchronizedBufferedWriter createBufferedGZipWriter(final File file) throws IOException {
        if (file == null) return null;
        if (file.exists()) file.delete();
        final FileOutputStream outFile = new FileOutputStream(file);
        final FileChannel outChannel = outFile.getChannel();
        final GZIPOutputStream gzos = new GZIPOutputStream(Channels.newOutputStream(outChannel));
        return new UnSynchronizedBufferedWriter(new OutputStreamWriter(gzos, Charset.forName(CHARSET)));
    }

    /**
   * Utility method to create fast ObjectOutput.
   * @param file File to write
   * @return a ObjectOutput
   * @throws IOException if an error occurs while creating the Writer
   */
    public static final ObjectOutputStream createObjectOutputWriter(final File file) throws IOException {
        if (file == null) return null;
        if (file.exists()) file.delete();
        final FileOutputStream outFile = new FileOutputStream(file);
        final FileChannel outChannel = outFile.getChannel();
        return new ObjectOutputStream(Channels.newOutputStream(outChannel));
    }

    /**
   * Utility method to create fast ObjectInputStream.
   * @param file File to read
   * @return a ObjectInputStream
   * @throws IOException if an error occurs while creating the reader
   */
    public static final ObjectInputStream createObjectInputReader(final File file) throws IOException {
        if (file == null) return null;
        final FileInputStream inFile = new FileInputStream(file);
        final FileChannel inChannel = inFile.getChannel();
        return new ObjectInputStream(Channels.newInputStream(inChannel));
    }

    /**
   * Copy bytes from an InputStream to an OutputStream.
   * @param input the InputStream to read from
   * @param output the OutputStream to write to
   * @return the number of bytes copied
   * @throws IOException In case of an I/O problem
   */
    public static int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
   * Get the files of a directory.
   * @param directory Directory to list files
   * @param extension extension of the file
   * @return an array of File objects
   */
    public static File[] listFilesByExtension(final File directory, final String extension) {
        if (directory == null || extension == null) return null;
        return directory.listFiles(new FilenameFilter() {

            public boolean accept(File arg0, String arg1) {
                return arg1.endsWith(extension);
            }
        });
    }

    /**
   * Remove a list of files.
   * @param filesToRemove An array with the files to remove
   * @param recursive true if the remove must be recursive
   */
    public static boolean removeFiles(final File[] filesToRemove, final boolean recursive) {
        if (filesToRemove == null) return false;
        for (int i = 0; i < filesToRemove.length; i++) {
            final File f = filesToRemove[i];
            if (f.isDirectory()) {
                if (recursive) {
                    if (!removeFiles(listFilesByExtension(f, ""), true)) return false;
                    if (!f.delete()) return false;
                }
            } else if (!f.delete()) return false;
        }
        return true;
    }

    /**
   * Get the prefix of a list of files.
   * @param files Files that we wants the prefix
   * @return the prefix of the files
   */
    public static String getPrefix(final List<File> files) {
        if (files == null) return null;
        File[] param = new File[files.size()];
        files.toArray(param);
        return getPrefix(param);
    }

    /**
   * Get the prefix of a list of files.
   * @param files Files that we wants the prefix
   * @return the prefix of the files
   */
    public static String getPrefix(final File[] files) {
        if (files == null) return null;
        String prefix = null;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < files.length; i++) {
            String filename = files[i].getName();
            if (prefix == null) prefix = filename; else if (!filename.startsWith(prefix)) {
                int max = Math.min(prefix.length(), filename.length());
                for (int j = 0; j < max; j++) {
                    if (prefix.charAt(j) == filename.charAt(j)) sb.append(prefix.charAt(j));
                }
                prefix = sb.toString();
                sb.setLength(0);
            }
        }
        return prefix;
    }

    /**
   * Set executable bits on file on *nix.
   * @param file File to handle
   * @param executable If true, sets the access permission to allow execute
   *          operations; if false to disallow execute operations
   * @param ownerOnly If true, the execute permission applies only to the
   *          owner's execute permission; otherwise, it applies to everybody. If
   *          the underlying file system can not distinguish the owner's execute
   *          permission from that of others, then the permission will apply to
   *          everybody, regardless of this value.
   * @return true if and only if the operation succeeded
   * @throws IOException
   */
    public static boolean setExecutable(final File file, final boolean executable, final boolean ownerOnly) throws IOException {
        if (file == null) return false;
        if (!file.exists() || !file.isFile()) throw new FileNotFoundException(file.getAbsolutePath());
        final String cmd = "chmod " + (ownerOnly ? "u+x " : "ugo+x ") + file.getAbsolutePath();
        ProcessUtils.exec(cmd, false);
        return true;
    }

    /**
   * Set executable bits on file on *nix.
   * @param file File to handle
   * @param ownerOnly If true, the execute permission applies only to the
   *          owner's execute permission; otherwise, it applies to everybody. If
   *          the underlying file system can not distinguish the owner's execute
   *          permission from that of others, then the permission will apply to
   *          everybody, regardless of this value.
   * @return true if and only if the operation succeeded
   * @throws IOException
   */
    public static boolean setExecutable(final File file, boolean executable) throws IOException {
        return setExecutable(file, executable, false);
    }

    /**
   * Set readable bits on file on *nix.
   * @param file File to handle
   * @param readable If true, sets the access permission to allow read
   *          operations; if false to disallow execute operations
   * @param ownerOnly If true, the execute permission applies only to the
   *          owner's execute permission; otherwise, it applies to everybody. If
   *          the underlying file system can not distinguish the owner's execute
   *          permission from that of others, then the permission will apply to
   *          everybody, regardless of this value.
   * @return true if and only if the operation succeeded
   * @throws IOException
   */
    public static boolean setReadable(final File file, final boolean readable, final boolean ownerOnly) throws IOException {
        if (file == null) return false;
        if (!file.exists() || !file.isFile()) throw new FileNotFoundException(file.getAbsolutePath());
        final String cmd = "chmod " + (ownerOnly ? "u+r " : "ugo+r ") + file.getAbsolutePath();
        ProcessUtils.exec(cmd, true);
        return true;
    }

    /**
   * Set readable bits on file on *nix.
   * @param file File to handle
   * @param readable If true, sets the access permission to allow read
   *          operations; if false to disallow execute operations
   * @return true if and only if the operation succeeded
   * @throws IOException
   */
    public static boolean setReadable(final File file, boolean readable) throws IOException {
        return setReadable(file, readable, true);
    }

    /**
   * Set writable bits on file on *nix.
   * @param file File to handle
   * @param writable If true, sets the access permission to allow read
   *          operations; if false to disallow execute operations
   * @param ownerOnly If true, the execute permission applies only to the
   *          owner's execute permission; otherwise, it applies to everybody. If
   *          the underlying file system can not distinguish the owner's execute
   *          permission from that of others, then the permission will apply to
   *          everybody, regardless of this value.
   * @return true if and only if the operation succeeded
   * @throws IOException
   */
    public static boolean setWritable(final File file, final boolean writable, final boolean ownerOnly) throws IOException {
        if (file == null) return false;
        if (!file.exists() || !file.isFile()) throw new FileNotFoundException(file.getAbsolutePath());
        final String cmd = "chmod " + (ownerOnly ? "u+w " : "ugo+w ") + file.getAbsolutePath();
        ProcessUtils.exec(cmd, true);
        return true;
    }

    /**
   * Set writable bits on file on *nix.
   * @param file File to handle
   * @param writable If true, sets the access permission to allow read
   *          operations; if false to disallow execute operations
   * @return true if and only if the operation succeeded
   * @throws IOException
   */
    public static boolean setWritable(final File file, boolean writable) throws IOException {
        return setWritable(file, writable, true);
    }
}
