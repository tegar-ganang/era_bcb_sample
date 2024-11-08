package edu.rice.cs.util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.*;
import edu.rice.cs.drjava.DrJava;
import edu.rice.cs.util.FileOps;
import edu.rice.cs.util.Log;
import edu.rice.cs.plt.io.IOUtil;
import edu.rice.cs.plt.text.TextUtil;
import static edu.rice.cs.drjava.config.OptionConstants.*;

/** A class to provide some convenient file operations as static methods.
  * It's abstract to prevent (useless) instantiation, though it can be subclassed
  * to provide convenient namespace importation of its methods.
  *
  * @version $Id: FileOps.java 5439 2011-08-11 17:13:04Z rcartwright $
  */
public abstract class FileOps {

    private static Log _log = new Log("FileOpsTest.txt", false);

    /** A singleton null file class. There is a separate NullFile class in this package. TODO: merge these two classes.  
    * This class is used for all NullFile.ONLY references while the other is used for distinct untitled documents.
    * Both appear to define the same notion of equality. 
    */
    public static class NullFile extends File {

        public static final NullFile ONLY = new NullFile();

        private NullFile() {
            super("");
        }

        public boolean canRead() {
            return false;
        }

        public boolean canWrite() {
            return false;
        }

        public int compareTo(File f) {
            return (f == this) ? 0 : -1;
        }

        public boolean createNewFile() {
            return false;
        }

        public boolean delete() {
            return false;
        }

        public void deleteOnExit() {
        }

        public boolean equals(Object o) {
            return o == this;
        }

        public boolean exists() {
            return false;
        }

        public int hashCode() {
            return getClass().hashCode();
        }

        public File getAbsoluteFile() {
            return this;
        }

        public String getAbsolutePath() {
            return "";
        }

        public File getCanonicalFile() {
            return this;
        }

        public String getCanonicalPath() {
            return "";
        }

        public String getName() {
            return "";
        }

        public String getParent() {
            return null;
        }

        public File getParentFile() {
            return null;
        }

        public String getPath() {
            return "";
        }

        public boolean isAbsolute() {
            return false;
        }

        public boolean isDirectory() {
            return false;
        }

        public boolean isFile() {
            return false;
        }

        public boolean isHidden() {
            return false;
        }

        public long lastModified() {
            return 0L;
        }

        public long length() {
            return 0L;
        }

        public String[] list() {
            return null;
        }

        public String[] list(FilenameFilter filter) {
            return null;
        }

        public File[] listFiles() {
            return null;
        }

        public File[] listFiles(FileFilter filter) {
            return null;
        }

        public File[] listFiles(FilenameFilter filter) {
            return null;
        }

        public boolean mkdir() {
            return false;
        }

        public boolean mkdirs() {
            return false;
        }

        public boolean renameTo(File dest) {
            return false;
        }

        public boolean setLastModified(long time) {
            return false;
        }

        public boolean setReadOnly() {
            return false;
        }

        public String toString() {
            return "";
        }
    }

    ;

    /** Special sentinal file used in FileOption and test classes among others. */
    public static final File NULL_FILE = NullFile.ONLY;

    /** @deprecated For a best-attempt canonical file, use {@link edu.rice.cs.plt.io.IOUtil#attemptCanonicalFile} instead.
    *             (for example, {@code IOUtil.attemptCanonicalFile(new File(path))})
    */
    @Deprecated
    public static File makeFile(String path) {
        File f = new File(path);
        try {
            return f.getCanonicalFile();
        } catch (IOException e) {
            return f;
        }
    }

    /** @deprecated For a best-attempt canonical file, use {@link edu.rice.cs.plt.io.IOUtil#attemptCanonicalFile} instead.
    *             (for example, {@code IOUtil.attemptCanonicalFile(new File(path))})
    */
    @Deprecated
    public static File makeFile(File parentDir, String child) {
        File f = new File(parentDir, child);
        try {
            return f.getCanonicalFile();
        } catch (IOException e) {
            return f;
        }
    }

    /** Determines whether the specified file in within the specified file tree. 
    * @deprecated Use {@link edu.rice.cs.plt.io.IOUtil#isMember} instead.  Note that the new method does not test for 
    *             {@code null} values and does not convert to canonical paths -- if these things are necessary, they 
    *             should be done before invoking the method.
    */
    @Deprecated
    public static boolean inFileTree(File f, File root) {
        if (root == null || f == null) return false;
        try {
            if (!f.isDirectory()) f = f.getParentFile();
            String filePath = f.getCanonicalPath() + File.separator;
            String projectPath = root.getCanonicalPath() + File.separator;
            return (filePath.startsWith(projectPath));
        } catch (IOException e) {
            return false;
        }
    }

    /** Return true if the directory ancestor is an ancestor of the file f, i.e.
    * you can get from f to ancestor by using getParentFile zero or more times.
    * @param ancestor the ancestor
    * @param f the file to test
    * @return true if ancestor is an ancestor of f. */
    public static boolean isAncestorOf(File ancestor, File f) {
        ancestor = ancestor.getAbsoluteFile();
        f = f.getAbsoluteFile();
        _log.log("ancestor = " + ancestor + "     f = " + f);
        while ((!ancestor.equals(f)) && (f != null)) {
            f = f.getParentFile();
        }
        return (ancestor.equals(f));
    }

    /** Makes a file equivalent to the given file f that is relative to base file b.  In other words,
    * <code>new File(b,makeRelativeTo(base,abs)).getCanonicalPath()</code> equals
    * <code>f.getCanonicalPath()</code>
    * 
    * <p>In Linux/Unix, if the file f is <code>/home/username/folder/file.java</code> and the file b is 
    * <code>/home/username/folder/sublevel/file2.java</code>, then the resulting File path from this method would be 
    * <code>../file.java</code> while its canoncial path would be <code>/home/username/folder/file.java</code>.</p><p>
    * Warning: making paths relative is inherently broken on some file systems, because a relative path
    * requires that both the file and the base have the same root. The Windows file system, and therefore also
    * the Java file system model, however, support multiple system roots (see {@link File#listRoots}).
    * Thus, two files with different roots cannot have a relative path. In that case the absolute path of
    * the file will be returned</p> 
    * @param f The path that is to be made relative to the base file
    * @param b The file to make the next file relative to
    * @return A new file whose path is relative to the base file while the value of <code>getCanonicalPath()</code> 
    *         for the returned file is the same as the result of <code>getCanonicalPath()</code> for the given file.
    */
    public static File makeRelativeTo(File f, File b) throws IOException, SecurityException {
        return new File(b, stringMakeRelativeTo(f, b));
    }

    /** Makes a file equivalent to the given file f that is relative to base file b.  In other words,
    * <code>new File(b,makeRelativeTo(base,abs)).getCanonicalPath()</code> equals
    * <code>f.getCanonicalPath()</code>
    * 
    * <p>In Linux/Unix, if the file f is <code>/home/username/folder/file.java</code> and the file b is 
    * <code>/home/username/folder/sublevel/file2.java</code>, then the resulting File path from this method would be 
    * <code>../file.java</code> while its canoncial path would be <code>/home/username/folder/file.java</code>.</p><p>
    * Warning: making paths relative is inherently broken on some file systems, because a relative path
    * requires that both the file and the base have the same root. The Windows file system, and therefore also
    * the Java file system model, however, support multiple system roots (see {@link File#listRoots}).
    * Thus, two files with different roots cannot have a relative path. In that case the absolute path of
    * the file will be returned</p> 
    * @param f The path that is to be made relative to the base file
    * @param b The file to make the next file relative to
    * @return A new file whose path is relative to the base file while the value of <code>getCanonicalPath()</code> 
    *         for the returned file is the same as the result of <code>getCanonicalPath()</code> for the given file.
    */
    public static String stringMakeRelativeTo(File f, File b) throws IOException {
        try {
            File[] roots = File.listRoots();
            File fRoot = null;
            File bRoot = null;
            for (File r : roots) {
                if (isAncestorOf(r, f)) {
                    fRoot = r;
                }
                if (isAncestorOf(r, b)) {
                    bRoot = r;
                }
                if ((fRoot != null) && (bRoot != null)) {
                    break;
                }
            }
            if (((fRoot == null) || (!fRoot.equals(bRoot))) && (!f.getAbsoluteFile().getCanonicalFile().toString().startsWith(File.separator + File.separator))) {
                return f.getAbsoluteFile().getCanonicalFile().toString();
            }
        } catch (Exception e) {
        }
        File base = b.getCanonicalFile();
        File abs = f.getCanonicalFile();
        if (!base.isDirectory()) base = base.getParentFile();
        String last = "";
        if (!abs.isDirectory()) {
            String tmp = abs.getPath();
            last = tmp.substring(tmp.lastIndexOf(File.separator) + 1);
            abs = abs.getParentFile();
        }
        String[] basParts = splitFile(base);
        String[] absParts = splitFile(abs);
        final StringBuilder result = new StringBuilder();
        int diffIndex = -1;
        boolean different = false;
        for (int i = 0; i < basParts.length; i++) {
            if (!different && ((i >= absParts.length) || !basParts[i].equals(absParts[i]))) {
                different = true;
                diffIndex = i;
            }
            if (different) result.append("..").append(File.separator);
        }
        if (diffIndex < 0) diffIndex = basParts.length;
        for (int i = diffIndex; i < absParts.length; i++) {
            result.append(absParts[i]).append(File.separator);
        }
        result.append(last);
        return result.toString();
    }

    /** Splits a file into an array of strings representing each parent folder of the given file.  The file whose path
    * is <code>/home/username/txt.txt</code> in linux would be split into the string array: 
    * {&quot;&quot;,&quot;home&quot;,&quot;username&quot;,&quot;txt.txt&quot;}. Delimeters are excluded.
    * @param fileToSplit  the file to split into its directories.
    * @deprecated Use {@link edu.rice.cs.plt.io.IOUtil#fullPath} instead.  It returns a list of {@code File}
    *             objects rather than strings, but they appear in the same order.
    */
    @Deprecated
    public static String[] splitFile(File fileToSplit) {
        String path = fileToSplit.getPath();
        ArrayList<String> list = new ArrayList<String>();
        while (!path.equals("")) {
            int idx = path.indexOf(File.separator);
            if (idx < 0) {
                list.add(path);
                path = "";
            } else {
                list.add(path.substring(0, idx));
                path = path.substring(idx + 1);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /** List all files (that is, {@code File}s for which {@code isFile()} is {@code true}) matching the provided filter in
    * the given directory.
    * @param d  The directory to search.
    * @param recur  Whether subdirectories accepted by {@code f} should be recursively searched.  Note that 
    *               subdirectories that <em>aren't</em> accepted by {@code f} will be ignored.
    * @param f  The filter to apply to contained {@code File}s.
    * @return  An array of Files in the directory specified; if the directory does not exist, returns an empty list.
    * @deprecated Use {@link edu.rice.cs.plt.io.IOUtil#attemptListFilesAsIterable} or
    *             {@link edu.rice.cs.plt.io.IOUtil#listFilesRecursively(File, FileFilter, FileFilter)} instead.
    */
    @Deprecated
    public static ArrayList<File> getFilesInDir(File d, boolean recur, FileFilter f) {
        ArrayList<File> l = new ArrayList<File>();
        getFilesInDir(d, l, recur, f);
        return l;
    }

    /** Helper fuction for getFilesInDir(File d, boolean recur). {@code acc} is mutated to contain
    * a list of <c>File</c>s in the directory specified, not including directories.
    */
    private static void getFilesInDir(File d, List<File> acc, boolean recur, FileFilter filter) {
        if (d.isDirectory()) {
            File[] files = d.listFiles(filter);
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory() && recur) getFilesInDir(f, acc, recur, filter); else if (f.isFile()) acc.add(f);
                }
            }
        }
    }

    /** @return the canonical file equivalent to f.  Identical to f.getCanonicalFile() except it does not throw an 
    * exception when the file path syntax is incorrect (or an IOException or SecurityException occurs for any
    * other reason).  It returns the absolute File intead.
    * @deprecated Use {@link edu.rice.cs.plt.io.IOUtil#attemptCanonicalFile} instead.
    */
    @Deprecated
    public static File getCanonicalFile(File f) {
        if (f == null) return f;
        try {
            return f.getCanonicalFile();
        } catch (IOException e) {
        } catch (SecurityException e) {
        }
        return f.getAbsoluteFile();
    }

    /** @return the canonical path for f.  Identical to f.getCanonicalPath() except it does not throw an 
    * exception when the file path syntax is incorrect; it returns the absolute path instead.
    * @deprecated  Use {@link edu.rice.cs.plt.io.IOUtil#attemptCanonicalFile} instead.  (The result will be a 
    * {@code File} instead of a {@code String}.)
    */
    @Deprecated
    public static String getCanonicalPath(File f) {
        return getCanonicalFile(f).getPath();
    }

    /** @return the file f unchanged if f exists; otherwise returns NULL_FILE. */
    public static File validate(File f) {
        if (f.exists()) return f;
        return FileOps.NULL_FILE;
    }

    /** This filter checks for files with names that end in ".java".  (Note that while this filter was <em>intended</em>
    * to be a {@code javax.swing.filechooser.FileFilter}, it actually implements a {@code java.io.FileFilter}, because
    * that is what {@code FileFilter} means in the context of this source file.)
    */
    @Deprecated
    public static final FileFilter JAVA_FILE_FILTER = new FileFilter() {

        public boolean accept(File f) {
            final StringBuilder name = new StringBuilder(f.getAbsolutePath());
            String shortName = f.getName();
            if (shortName.length() < 6) return false;
            name.delete(name.length() - 5, name.length());
            name.append(".java");
            File test = new File(name.toString());
            return (test.equals(f));
        }
    };

    /** Reads the stream until it reaches EOF, and then returns the read contents as a byte array. This call may block, 
    * since it will not return until EOF has been reached.
    * @param stream  Input stream to read.
    * @return Byte array consisting of all data read from stream.
    * @deprecated  Use {@link edu.rice.cs.plt.io.IOUtil#toByteArray} instead.  Note that the {@code IOUtil} method will
    * not close the {@code InputStream}, while this method does.
    */
    @Deprecated
    public static byte[] readStreamAsBytes(final InputStream stream) throws IOException {
        BufferedInputStream buffered;
        if (stream instanceof BufferedInputStream) buffered = (BufferedInputStream) stream; else buffered = new BufferedInputStream(stream);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int readVal = buffered.read();
        while (readVal != -1) {
            out.write(readVal);
            readVal = buffered.read();
        }
        stream.close();
        return out.toByteArray();
    }

    /** Reads the entire contents of a file and return them as canonicalized Swing Document text. All newLine sequences,
    * including "\n", "\r", and "\r\n" are converted to "\n". Characters below 32, except for newlines, are changed to spaces. */
    public static String readFileAsSwingText(final File file) throws IOException {
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            final StringBuilder buf = new StringBuilder();
            char pred = (char) 0;
            while (reader.ready()) {
                char c = (char) reader.read();
                if (c == '\n' && pred == '\r') {
                } else if (c == '\r') buf.append('\n'); else if ((c < 32) && (c != '\n')) buf.append(' '); else buf.append(c);
                pred = c;
            }
            return buf.toString();
        } finally {
            if (reader != null) reader.close();
        }
    }

    /** Reads the entire contents of a file and return them as a String.
    * @deprecated  Use {@link edu.rice.cs.plt.io.IOUtil#toString(File)} instead, which provides the same functionality.
    */
    @Deprecated
    public static String readFileAsString(final File file) throws IOException {
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            final StringBuilder buf = new StringBuilder();
            while (reader.ready()) {
                char c = (char) reader.read();
                buf.append(c);
            }
            return buf.toString();
        } finally {
            if (reader != null) reader.close();
        }
    }

    /** Copies the text of one file into another.
    * @param source the file to be copied
    * @param dest the file to be copied to
    * @deprecated  Use {@link edu.rice.cs.plt.io.IOUtil#copyFile} instead; it scales in a much more efficiently.
    */
    @Deprecated
    public static void copyFile(File source, File dest) throws IOException {
        String text = readFileAsString(source);
        writeStringToFile(dest, text);
    }

    /** Creates a new temporary file and writes the given text to it. The file will be deleted on exit.
    * @param prefix Beginning part of file name, before unique number
    * @param suffix Ending part of file name, after unique number
    * @param text Text to write to file
    * @return name of the temporary file that was created
    * @deprecated  Instead, create a temp file with {@link edu.rice.cs.plt.io.IOUtil#createAndMarkTempFile(String, String)},
    * then write to it with {@link edu.rice.cs.plt.io.IOUtil#writeStringToFile(File, String)}.
    */
    @Deprecated
    public static File writeStringToNewTempFile(final String prefix, final String suffix, final String text) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        file.deleteOnExit();
        writeStringToFile(file, text);
        return file;
    }

    /** Writes text to the file overwriting whatever was there.
    * @param file File to write to
    * @param text Test to write
    * @deprecated  Use the equivalent {@link edu.rice.cs.plt.io.IOUtil#writeStringToFile(File, String)} instead
    */
    @Deprecated
    public static void writeStringToFile(File file, String text) throws IOException {
        writeStringToFile(file, text, false);
    }

    /** Writes text to the file.
    * @param file File to write to
    * @param text Text to write
    * @param append whether to append. (false=overwrite)
    * @deprecated  Use the equivalent {@link edu.rice.cs.plt.io.IOUtil#writeStringToFile(File, String, boolean)} instead
    */
    @Deprecated
    public static void writeStringToFile(File file, String text, boolean append) throws IOException {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file, append);
            writer.write(text);
        } finally {
            if (writer != null) writer.close();
        }
    }

    /** Writes text to the given file returning true if it succeeded and false if not.  This is a simple wrapper for
    * writeStringToFile that doesn't throw an IOException.
    * @param file  File to write to
    * @param text  Text to write
    * @param append  Whether to append. (false=overwrite)
    * @deprecated  Use {@link edu.rice.cs.plt.io.IOUtil#attemptWriteStringToFile(File, String, boolean)} instead.
    */
    @Deprecated
    public static boolean writeIfPossible(File file, String text, boolean append) {
        try {
            writeStringToFile(file, text, append);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static File createTempDirectory(final String name) throws IOException {
        return createTempDirectory(name, null);
    }

    public static File createTempDirectory(String name, File parent) throws IOException {
        File result = File.createTempFile(name, "", parent);
        boolean success = result.delete();
        success = success && result.mkdir();
        if (!success) {
            throw new IOException("Attempt to create directory failed");
        }
        IOUtil.attemptDeleteOnExit(result);
        return result;
    }

    /** Delete the given directory including any files and directories it contains.
    * @param dir  File object representing directory to delete. If, for some reason, this file object is not a 
    *             directory, it will still be deleted.
    * @return true if there were no problems in deleting. If it returns false, something failed and the directory
    * contents likely at least partially still exist.
    * @deprecated  Use {@link edu.rice.cs.plt.io.IOUtil#deleteRecursively} instead
    */
    @Deprecated
    public static boolean deleteDirectory(final File dir) {
        if (!dir.isDirectory()) {
            boolean res;
            res = dir.delete();
            return res;
        }
        boolean ret = true;
        File[] childFiles = dir.listFiles();
        if (childFiles != null) {
            for (File f : childFiles) {
                ret = ret && deleteDirectory(f);
            }
        }
        ret = ret && dir.delete();
        return ret;
    }

    /** Instructs Java to recursively delete the given directory and its contents when the JVM exits.
    * @param dir File object representing directory to delete. If, for some reason, this file object is not a 
    *            directory, it will still be deleted.
    * @deprecated  Use {@link edu.rice.cs.plt.io.IOUtil#deleteOnExitRecursively} instead
    */
    @Deprecated
    public static void deleteDirectoryOnExit(final File dir) {
        _log.log("Deleting file/directory " + dir + " on exit");
        dir.deleteOnExit();
        if (dir.isDirectory()) {
            File[] childFiles = dir.listFiles();
            if (childFiles != null) {
                for (File f : childFiles) {
                    deleteDirectoryOnExit(f);
                }
            }
        }
    }

    /** This function starts from the given directory and finds all  packages within that directory
    * @param prefix the package name of files in the given root
    * @param root the directory to start exploring from
    * @return a list of valid packages, excluding the root ("") package
    */
    public static LinkedList<String> packageExplore(String prefix, File root) {
        class PrefixAndFile {

            public String prefix;

            public File root;

            public PrefixAndFile(String prefix, File root) {
                this.root = root;
                this.prefix = prefix;
            }
        }
        final Set<File> exploredDirectories = new HashSet<File>();
        LinkedList<String> output = new LinkedList<String>();
        Stack<PrefixAndFile> working = new Stack<PrefixAndFile>();
        working.push(new PrefixAndFile(prefix, root));
        exploredDirectories.add(root);
        FileFilter directoryFilter = new FileFilter() {

            public boolean accept(File f) {
                boolean toReturn = f.isDirectory() && !exploredDirectories.contains(f);
                exploredDirectories.add(f);
                return toReturn;
            }
        };
        while (!working.empty()) {
            PrefixAndFile current = working.pop();
            File[] subDirectories = current.root.listFiles(directoryFilter);
            if (subDirectories != null) {
                for (File dir : subDirectories) {
                    PrefixAndFile paf;
                    if (current.prefix.equals("")) paf = new PrefixAndFile(dir.getName(), dir); else paf = new PrefixAndFile(current.prefix + "." + dir.getName(), dir);
                    working.push(paf);
                }
            }
            File[] javaFiles = current.root.listFiles(JAVA_FILE_FILTER);
            if (javaFiles != null) {
                if (javaFiles.length != 0 && !current.prefix.equals("")) {
                    output.add(current.prefix);
                }
            }
        }
        return output;
    }

    /** Renames the given file to the given destination.  Needed since Windows does not allow a rename to overwrite an 
    * existing file.
    * @param file the file to rename
    * @param dest the destination file
    * @return true iff the rename was successful
    * @deprecated  Use {@link edu.rice.cs.plt.io.IOUtil#attemptMove}, which is equally Windows-friendly, instead.
    */
    @Deprecated
    public static boolean renameFile(File file, File dest) {
        if (dest.exists()) dest.delete();
        return file.renameTo(dest);
    }

    /** This method writes files correctly; it takes care of catching errors, making backups, and keeping an unsuccessful 
    * file save from destroying the old file (unless a backup is made).  It makes sure that the file to be saved is not 
    * read-only, throwing an IOException if it is.  Note: if saving fails and a backup was being created, any existing 
    * backup will be destroyed (because the backup is written before saving begins, then moved back over the original 
    * file when saving fails).  Since the old backup would have been destroyed anyway if saving had succeeded, this
    * behavior is appropriate.
    * @param fileSaver  Keeps track of the name of the file to write, whether to back up the file, and has 
    *                   a method that actually performs the writing of the file
    * @throws IOException if the saving or backing up of the file fails for any reason
    */
    public static void saveFile(FileSaver fileSaver) throws IOException {
        boolean makeBackup = fileSaver.shouldBackup();
        boolean success = false;
        File file = fileSaver.getTargetFile();
        File backup = null;
        boolean tempFileUsed = true;
        if (file.exists() && !file.canWrite()) throw new IOException("Permission denied");
        if (makeBackup) {
            backup = fileSaver.getBackupFile();
            if (!renameFile(file, backup)) {
                throw new IOException("Save failed. Could not create backup file " + backup.getAbsolutePath() + "\nIt may be possible to save by disabling file backups\n");
            }
            fileSaver.backupDone();
        }
        File parent = file.getParentFile();
        File tempFile = File.createTempFile("drjava", ".temp", parent);
        try {
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(tempFile);
            } catch (FileNotFoundException fnfe) {
                if (fileSaver.continueWhenTempFileCreationFails()) {
                    fos = new FileOutputStream(file);
                    tempFileUsed = false;
                } else throw new IOException("Could not create temp file " + tempFile + " in attempt to save " + file);
            }
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            fileSaver.saveTo(bos);
            bos.close();
            if (tempFileUsed && !renameFile(tempFile, file)) throw new IOException("Save failed. Another process may be using " + file + ".");
            success = true;
        } finally {
            if (tempFileUsed) tempFile.delete();
            if (makeBackup) {
                if (success) fileSaver.backupDone(); else {
                    renameFile(backup, file);
                }
            }
        }
    }

    public interface FileSaver {

        /** This method tells what to name the backup file, if a backup is made.  It may depend on getTargetFile(), so it
      * can throw an IOException.
      */
        public abstract File getBackupFile() throws IOException;

        /** This method indicates whether or not a backup of the file should be made.  It may depend on getTargetFile(), 
      * so it can throw an IOException.
      */
        public abstract boolean shouldBackup() throws IOException;

        /** This method specifies if the saving process should continue trying to save the file if the temp file that is 
      * written initially cannot be created.  Continue saving in this case is dangerous because the original file may
      * be lost if saving fails.
      */
        public abstract boolean continueWhenTempFileCreationFails();

        /** This method is called to tell the file saver that a backup was successfully made. */
        public abstract void backupDone();

        /**
     * This method actually writes info to a file.  NOTE: It is important that this
     * method write to the stream it is passed, not the target file.  If you write
     * directly to the target file, the target file will be destroyed if saving fails.
     * Also, it is important that when saving fails this method throw an IOException
     * @throws IOException when saving fails for any reason
     */
        public abstract void saveTo(OutputStream os) throws IOException;

        /** This method specifies the file for saving.  It should return the canonical name of the file, resolving symlinks.
      * Otherwise, the saver cannot deal correctly with symlinks.  Resolving symlinks may cause an IOException, so this
      * method declares that it may throw an IOException.
      */
        public abstract File getTargetFile() throws IOException;
    }

    /** This class is a default implementation of FileSaver that makes only one backup of each file per instantiation of
    * the program (following the Emacs convention).  It creates a backup file named <file>~.  It does not implement the
    * saveTo method.
    */
    public abstract static class DefaultFileSaver implements FileSaver {

        private File outputFile = FileOps.NULL_FILE;

        private static Set<File> filesNotNeedingBackup = new HashSet<File>();

        private boolean backupsEnabled = DrJava.getConfig().getSetting(BACKUP_FILES);

        /** This field keeps track of whether or not outputFile has been resolved to its canonical name. */
        private boolean isCanonical = false;

        public DefaultFileSaver(File file) {
            outputFile = file.getAbsoluteFile();
        }

        public boolean continueWhenTempFileCreationFails() {
            return true;
        }

        public File getBackupFile() throws IOException {
            return new File(getTargetFile().getPath() + "~");
        }

        public boolean shouldBackup() throws IOException {
            if (!backupsEnabled) return false;
            if (!getTargetFile().exists()) return false;
            if (filesNotNeedingBackup.contains(getTargetFile())) return false;
            return true;
        }

        public void backupDone() {
            try {
                filesNotNeedingBackup.add(getTargetFile());
            } catch (IOException ioe) {
                throw new UnexpectedException(ioe, "getTargetFile should fail earlier");
            }
        }

        public File getTargetFile() throws IOException {
            if (!isCanonical) {
                outputFile = outputFile.getCanonicalFile();
                isCanonical = true;
            }
            return outputFile;
        }
    }

    /** Converts all path entries in a path string to absolute paths. The delimiter in the path string is the 
    * "path.separator" property.  Empty entries are equivalent to "." and thus are converted to the value of "user.dir".
    * Example: ".:drjava::/home/foo/junit.jar" with "user.dir" set to "/home/foo/bar" will be converted to 
    *   "/home/foo/bar:/home/foo/bar/drjava:/home/foo/bar:/home/foo/junit.jar".
    * @param path path string with entries to convert
    * @return path string with all entries as absolute paths
    * @deprecated  Use {@link edu.rice.cs.plt.io.IOUtil#parsePath}, {@link edu.rice.cs.plt.io.IOUtil#getAbsoluteFiles},
    * {@link edu.rice.cs.plt.io.IOUtil#attemptAbsoluteFiles}, and {@link edu.rice.cs.plt.io.IOUtil#pathToString},
    * as needed, instead.
    */
    @Deprecated
    public static String convertToAbsolutePathEntries(String path) {
        String pathSep = System.getProperty("path.separator");
        path += pathSep + "x";
        String[] pathEntries = path.split(pathSep);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pathEntries.length - 1; ++i) {
            File f = new File(pathEntries[i]);
            sb.append(f.getAbsolutePath());
            sb.append(pathSep);
        }
        String reconstructedPath = sb.toString();
        if (reconstructedPath.length() != 0) {
            reconstructedPath = reconstructedPath.substring(0, reconstructedPath.length() - 1);
        }
        return reconstructedPath;
    }

    /** Return a valid directory for use, i.e. one that exists and is as "close" to the file specified. It is
    * 1) file, if file is a directory and exists
    * 2) the closest parent of file, if file is not a directory or does not exist
    * 3) "user.home"
    * @return a valid directory for use */
    public static File getValidDirectory(final File origFile) {
        File file = origFile;
        if ((file == FileOps.NULL_FILE) || (file == null)) {
            file = new File(System.getProperty("user.home"));
        }
        assert file != null;
        while (file != null && !file.exists()) {
            file = file.getParentFile();
        }
        if (file == null) {
            file = new File(System.getProperty("user.home"));
        }
        assert file != null;
        if (!file.isDirectory()) {
            if (file.getParent() != null) {
                file = file.getParentFile();
                if (file == null) {
                    file = new File(System.getProperty("user.home"));
                }
                assert file != null;
            }
        }
        if (file.exists() && file.isDirectory()) return file;
        throw new UnexpectedException(new IOException(origFile.getPath() + " is not a valid directory, and all attempts " + "to locate a valid directory have failed. " + "Check your configuration."));
    }

    /** Converts the abstract pathname for f into a URL.  This method is included in class java.io.File as f.toURL(), but
    * has been deprecated in Java 6.0 because escape characters on some systems are not handled correctly.  The workaround,
    * f.toURI().toURL(), is unsatisfactory because we rely on the old (broken) behavior: toURI() produces escape
    * characters (for example, " " becomes "%20"), which remain in the name when we attempt to convert back
    * to a filename.  That is, f.toURI().toURL().getFile() may not be a valid path, even if f exists.  (The correct
    * solution is to avoid trying to convert from a URL to a File, because this conversion is not guaranteed
    * to work.)
    */
    public static URL toURL(File f) throws MalformedURLException {
        return f.toURI().toURL();
    }

    public static boolean makeWritable(File roFile) throws IOException {
        boolean shouldBackup = edu.rice.cs.drjava.DrJava.getConfig().getSetting(edu.rice.cs.drjava.config.OptionConstants.BACKUP_FILES);
        boolean madeBackup = false;
        File backup = new File(roFile.getAbsolutePath() + "~");
        try {
            boolean noBackup = true;
            if (backup.exists()) {
                try {
                    noBackup = backup.delete();
                } catch (SecurityException se) {
                    noBackup = false;
                }
            }
            if (noBackup) {
                try {
                    noBackup = roFile.renameTo(backup);
                    madeBackup = true;
                    roFile.createNewFile();
                } catch (SecurityException se) {
                    noBackup = false;
                } catch (IOException ioe) {
                }
                try {
                    roFile.createNewFile();
                } catch (SecurityException se) {
                } catch (IOException ioe) {
                }
            }
            if (!noBackup) {
                try {
                    roFile.delete();
                } catch (SecurityException se) {
                    return false;
                }
            }
            try {
                edu.rice.cs.plt.io.IOUtil.copyFile(backup, roFile);
            } catch (SecurityException se) {
                return false;
            } catch (IOException ioe) {
                return false;
            }
            return true;
        } finally {
            if (!shouldBackup && madeBackup) {
                try {
                    backup.delete();
                } catch (Exception e) {
                }
            }
        }
    }

    /** Move f to n, recursively if necessary.
    * @param f file or directory to move
    * @param n new location and name for the file or directory
    * @return true if successful */
    public static boolean moveRecursively(File f, File n) {
        boolean res = true;
        try {
            if (!f.exists()) {
                return false;
            }
            if (f.isFile()) {
                return edu.rice.cs.plt.io.IOUtil.attemptMove(f, n);
            } else {
                if (!n.mkdir()) {
                    return false;
                }
                for (String child : f.list()) {
                    File oldChild = new File(f, child);
                    File newChild = new File(n, child);
                    res = res && moveRecursively(oldChild, newChild);
                }
                if (!f.delete()) {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return res;
    }

    /** Generate a new file name that does not yet exist. Maximum of 20 attempts.
    * Example: generateNewFileName(new File("foo.bar"))
    * generates "foo.bar", "foo.bar-2", "foo.bar-3", and so on.
    * @param base base name of the file
    * @return new file name that does not yet exist
    * @throws IOException if file name cannot be generated within 100 attempts */
    public static File generateNewFileName(File base) throws IOException {
        return generateNewFileName(base.getParentFile(), base.getName());
    }

    /** Generate a new file name that does not yet exist. Maximum of 20 attempts.
    * Example: generateNewFileName(new File("."), "foo.bar")
    * generates "foo.bar", "foo.bar-2", "foo.bar-3", and so on.
    * @param dir directory of the file
    * @param name the base file name
    * @return new file name that does not yet exist
    * @throws IOException if file name cannot be generated within 100 attempts */
    public static File generateNewFileName(File dir, String name) throws IOException {
        return generateNewFileName(dir, name, "", 100);
    }

    /** Generate a new file name that does not yet exist. Maximum of 20 attempts.
    * @param dir directory of the file
    * @param prefix the beginning of the file name
    * @param suffix the end of the file name
    * @return new file name that does not yet exist
    * @throws IOException if file name cannot be generated within 100 attempts */
    public static File generateNewFileName(File dir, String prefix, String suffix) throws IOException {
        return generateNewFileName(dir, prefix, suffix, 100);
    }

    /** Generate a new file name that does not yet exist.
    * Example: generateNewFileName(new File("."), "foo", ".bar", 10)
    * generates "foo.bar", "foo-2.bar", "foo-3.bar", and so on.
    * @param dir directory of the file
    * @param prefix the beginning of the file name
    * @param suffix the end of the file name
    * @param max maximum number of attempts
    * @return new file name that does not yet exist
    * @throws IOException if file name cannot be generated within max attempts */
    public static File generateNewFileName(File dir, String prefix, String suffix, int max) throws IOException {
        File temp = new File(dir, prefix + suffix);
        if (temp.exists()) {
            int count = 2;
            do {
                temp = new File(dir, prefix + "-" + count + suffix);
                ++count;
            } while (temp.exists() && (count < max));
            if (temp.exists()) {
                throw new IOException("Could not generate a file name that did not already exist.");
            }
        }
        return temp;
    }

    /** On Windows, return an 8.3 file name for the specified file. On other OSes, return the file unmodified.
    * @param f file for which to find an 8.3 file name
    * @return short file name for the file (or unmodified on non-Windows)
    * @throws IOException if an 8.3 file name could not be found */
    public static File getShortFile(File f) throws IOException {
        if (!edu.rice.cs.drjava.platform.PlatformFactory.ONLY.isWindowsPlatform()) {
            return f;
        }
        String s = "";
        File parent = f.getParentFile();
        File[] roots = File.listRoots();
        File root = new File(File.separator);
        for (File r : roots) {
            if (f.getCanonicalPath().startsWith(r.getAbsolutePath())) {
                root = r;
                break;
            }
        }
        while (parent != null) {
            try {
                Process p = new ProcessBuilder("cmd", "/C", "dir", "/X", "/A").directory(parent).redirectErrorStream(true).start();
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                boolean found = false;
                while ((line = br.readLine()) != null) {
                    if (!found) {
                        if (line.trim().length() == 0) continue;
                        if (line.startsWith(" ")) continue;
                        int pos = line.indexOf("  ");
                        if (pos == -1) continue;
                        pos = line.indexOf("  ", pos + 2);
                        if (pos == -1) continue;
                        line = line.substring(pos).trim();
                        pos = line.indexOf(' ');
                        if (pos == -1) continue;
                        line = line.substring(pos).trim();
                        File shortF = null;
                        if (line.toLowerCase().equals(f.getName().toLowerCase())) {
                            shortF = new File(parent, line);
                            if (f.getCanonicalFile().equals(shortF.getCanonicalFile())) {
                                found = true;
                            }
                        } else if (line.toLowerCase().startsWith(f.getName().toLowerCase()) && f.getName().contains("~")) {
                            shortF = new File(parent, f.getName());
                            if (f.getCanonicalFile().equals(shortF.getCanonicalFile())) {
                                found = true;
                            }
                        } else if (line.toLowerCase().endsWith(" " + f.getName().toLowerCase())) {
                            String shortLine = line.substring(0, line.length() - f.getName().length()).trim();
                            if (line.length() == 0) {
                                found = true;
                                shortF = f;
                            } else {
                                shortF = new File(parent, shortLine);
                                if (shortF.exists()) {
                                    if (f.getCanonicalFile().equals(shortF.getCanonicalFile())) {
                                        found = true;
                                    }
                                }
                            }
                        }
                        if (found && (shortF != null)) {
                            s = shortF.getName() + ((s.length() == 0) ? "" : (File.separator + s));
                        }
                    }
                }
                try {
                    p.waitFor();
                } catch (InterruptedException ie) {
                    throw new IOException("Could not get short windows file name: " + ie);
                }
                if (!found) {
                    throw new IOException("Could not get short windows file name: " + f.getAbsolutePath() + " not found");
                }
            } catch (IOException ioe) {
                throw new IOException("Could not get short windows file name: " + ioe);
            }
            f = parent;
            parent = parent.getParentFile();
        }
        File shortF = new File(root, s);
        if (!shortF.exists()) {
            throw new IOException("Could not get short windows file name: " + shortF.getAbsolutePath() + " not found");
        }
        return shortF;
    }

    /** Returns the drjava.jar file.
    * @return drjava.jar file */
    public static File getDrJavaFile() {
        String[] cps = System.getProperty("java.class.path").split(TextUtil.regexEscape(File.pathSeparator), -1);
        File found = null;
        for (String cp : cps) {
            try {
                File f = new File(cp);
                if (!f.exists()) {
                    continue;
                }
                if (f.isDirectory()) {
                    File cf = new File(f, edu.rice.cs.drjava.DrJava.class.getName().replace('.', File.separatorChar) + ".class");
                    if (cf.exists() && cf.isFile()) {
                        found = f;
                        break;
                    }
                } else if (f.isFile()) {
                    JarFile jf = new JarFile(f);
                    if (jf.getJarEntry(edu.rice.cs.drjava.DrJava.class.getName().replace('.', '/') + ".class") != null) {
                        found = f;
                        break;
                    }
                }
            } catch (IOException e) {
            }
        }
        return found.getAbsoluteFile();
    }

    /** Returns the current DrJava application, i.e. the drjava.jar, drjava.exe or DrJava.app file.
    * @return DrJava application file */
    public static File getDrJavaApplicationFile() {
        File found = FileOps.getDrJavaFile();
        if (found != null) {
            if (edu.rice.cs.drjava.platform.PlatformFactory.ONLY.isMacPlatform()) {
                String s = found.getAbsolutePath();
                if (s.endsWith(".app/Contents/Resources/Java/drjava.jar")) {
                    found = new File(s.substring(0, s.lastIndexOf("/Contents/Resources/Java/drjava.jar")));
                }
            }
        }
        return found.getAbsoluteFile();
    }
}
