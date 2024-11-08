package org.openconcerto.utils;

import org.openconcerto.utils.StringUtils.Escaper;
import org.openconcerto.utils.cc.ExnTransformer;
import org.openconcerto.utils.cc.IClosure;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FileUtils {

    private FileUtils() {
    }

    public static void browseFile(File f) {
        if (Desktop.isDesktopSupported()) {
            Desktop d = Desktop.getDesktop();
            if (d.isSupported(Desktop.Action.BROWSE)) {
                try {
                    d.browse(f.getCanonicalFile().toURI());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    openNative(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                openNative(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void openFile(File f) throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop d = Desktop.getDesktop();
            if (d.isSupported(Desktop.Action.OPEN)) {
                d.open(f.getCanonicalFile());
            } else {
                openNative(f);
            }
        } else {
            openNative(f);
        }
    }

    /**
     * All the files (see {@link File#isFile()}) contained in the passed dir.
     * 
     * @param dir the root directory to search.
     * @return a List of String.
     */
    public static List<String> listR(File dir) {
        return listR(dir, REGULAR_FILE_FILTER);
    }

    public static List<String> listR(File dir, FileFilter ff) {
        return listR_rec(dir, ff, ".");
    }

    private static List<String> listR_rec(File dir, FileFilter ff, String prefix) {
        if (!dir.isDirectory()) return null;
        final List<String> res = new ArrayList<String>();
        final File[] children = dir.listFiles();
        for (int i = 0; i < children.length; i++) {
            final String newPrefix = prefix + "/" + children[i].getName();
            if (ff == null || ff.accept(children[i])) {
                res.add(newPrefix);
            }
            if (children[i].isDirectory()) {
                res.addAll(listR_rec(children[i], ff, newPrefix));
            }
        }
        return res;
    }

    public static void walk(File dir, IClosure<File> c) {
        walk(dir, c, RecursionType.BREADTH_FIRST);
    }

    public static void walk(File dir, IClosure<File> c, RecursionType type) {
        if (type == RecursionType.BREADTH_FIRST) c.executeChecked(dir);
        if (dir.isDirectory()) {
            for (final File child : dir.listFiles()) {
                walk(child, c, type);
            }
        }
        if (type == RecursionType.DEPTH_FIRST) c.executeChecked(dir);
    }

    public static final List<File> list(File root, final int depth) {
        return list(root, depth, null);
    }

    /**
     * Finds all files at the specified depth below <code>root</code>.
     * 
     * @param root the base directory
     * @param depth the depth of the returned files.
     * @param ff a filter, can be <code>null</code>.
     * @return a list of files <code>depth</code> levels beneath <code>root</code>.
     */
    public static final List<File> list(File root, final int depth, final FileFilter ff) {
        return list(root, depth, depth, ff);
    }

    public static final List<File> list(final File root, final int minDepth, final int maxDepth, final FileFilter ff) {
        return list(root, minDepth, maxDepth, ff, false);
    }

    public static final List<File> list(final File root, final int minDepth, final int maxDepth, final FileFilter ff, final boolean sort) {
        if (minDepth > maxDepth) throw new IllegalArgumentException(minDepth + " > " + maxDepth);
        if (maxDepth < 0) throw new IllegalArgumentException(maxDepth + " < 0");
        if (!root.exists()) return Collections.<File>emptyList();
        final File currentFile = accept(ff, minDepth, maxDepth, root, 0) ? root : null;
        if (maxDepth == 0) {
            return currentFile == null ? Collections.<File>emptyList() : Collections.singletonList(currentFile);
        } else {
            final List<File> res = new ArrayList<File>();
            final File[] children = root.listFiles();
            if (children == null) throw new IllegalStateException("cannot list " + root);
            if (sort) Arrays.sort(children);
            for (final File child : children) {
                if (maxDepth > 1 && child.isDirectory()) {
                    res.addAll(list(child, minDepth - 1, maxDepth - 1, ff, sort));
                } else if (accept(ff, minDepth, maxDepth, child, 1)) {
                    res.add(child);
                }
            }
            if (currentFile != null) res.add(currentFile);
            return res;
        }
    }

    private static final boolean accept(final FileFilter ff, final int minDepth, final int maxDepth, final File f, final int depth) {
        return minDepth <= depth && depth <= maxDepth && (ff == null || ff.accept(f));
    }

    /**
     * Returns the relative path from one file to another in the same filesystem tree. Files are not
     * required to exist, see {@link File#getCanonicalPath()}.
     * 
     * @param fromDir the starting directory, eg /a/b/.
     * @param to the file to get to, eg /a/x/y.txt.
     * @return the relative path, eg "../x/y.txt".
     * @throws IOException if an error occurs while canonicalizing the files.
     * @throws IllegalArgumentException if fromDir exists and is not directory.
     */
    public static final String relative(File fromDir, File to) throws IOException {
        if (fromDir.exists() && !fromDir.isDirectory()) throw new IllegalArgumentException(fromDir + " is not a directory");
        final File fromF = fromDir.getCanonicalFile();
        final File toF = to.getCanonicalFile();
        final List<File> toPath = getAncestors(toF);
        final List<File> fromPath = getAncestors(fromF);
        if (!toPath.get(0).equals(fromPath.get(0))) {
            return toF.getPath();
        }
        int commonIndex = Math.min(toPath.size(), fromPath.size()) - 1;
        boolean found = false;
        while (commonIndex >= 0 && !found) {
            found = fromPath.get(commonIndex).equals(toPath.get(commonIndex));
            if (!found) commonIndex--;
        }
        final List<String> complete = new ArrayList<String>(Collections.nCopies(fromPath.size() - 1 - commonIndex, ".."));
        if (complete.isEmpty()) complete.add(".");
        for (File f : toPath.subList(commonIndex + 1, toPath.size())) {
            complete.add(f.getName());
        }
        return CollectionUtils.join(complete, File.separator);
    }

    public static final List<File> getAncestors(File f) {
        final List<File> path = new ArrayList<File>();
        File currentF = f;
        while (currentF != null) {
            path.add(0, currentF);
            currentF = currentF.getParentFile();
        }
        return path;
    }

    public static final File addSuffix(File f, String suffix) {
        return new File(f.getParentFile(), f.getName() + suffix);
    }

    /**
     * Prepend a string to a suffix.
     * 
     * @param f the file, e.g. "sample.xml".
     * @param toInsert the string to insert in the filename, e.g. "-sql".
     * @param suffix the suffix of <code>f</code>, e.g. ".xml".
     * @return a new file with <code>toInsert</code> prepended to <code>suffix</code>, e.g.
     *         "sample-sql.xml".
     */
    public static final File prependSuffix(File f, String toInsert, String suffix) {
        return new File(f.getParentFile(), removeSuffix(f.getName(), suffix) + toInsert + suffix);
    }

    public static final String removeSuffix(String name, String suffix) {
        return name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
    }

    /**
     * Rename a file if necessary by finding a free name. The tested names are
     * <code>name + "_" + i + suffix</code>.
     * 
     * @param parent the directory.
     * @param name the base name of the file.
     * @param suffix the suffix of the file, e.g. ".ods".
     * @return <code>new File(parent, name + suffix)</code> (always non existing) and the new file,
     *         (or <code>null</code> if no file was moved).
     */
    public static final File[] mvOut(final File parent, final String name, final String suffix) {
        final File fDest = new File(parent, name + suffix);
        final File renamed;
        if (fDest.exists()) {
            int i = 0;
            File free = fDest;
            while (free.exists()) {
                free = new File(parent, name + "_" + i + suffix);
                i++;
            }
            assert !fDest.equals(free);
            if (!fDest.renameTo(free)) throw new IllegalStateException("Couldn't rename " + fDest + " to " + free);
            renamed = free;
        } else {
            renamed = null;
        }
        assert !fDest.exists();
        return new File[] { fDest, renamed };
    }

    /**
     * Behave like the 'mv' unix utility, ie handle cross filesystems mv and <code>dest</code> being
     * a directory.
     * 
     * @param f the source file.
     * @param dest the destination file or directory.
     * @return the error or <code>null</code> if there was none.
     */
    public static String mv(File f, File dest) {
        final File canonF;
        File canonDest;
        try {
            canonF = f.getCanonicalFile();
            canonDest = dest.getCanonicalFile();
        } catch (IOException e) {
            return ExceptionUtils.getStackTrace(e);
        }
        if (canonF.equals(canonDest)) return null;
        if (canonDest.isDirectory()) canonDest = new File(canonDest, canonF.getName());
        final File destF;
        if (canonDest.exists()) return canonDest + " exists"; else if (!canonDest.getParentFile().exists()) return "parent of " + canonDest + " does not exist"; else destF = canonDest;
        if (!canonF.renameTo(destF)) {
            try {
                copyDirectory(canonF, destF);
                if (destF.exists()) rmR(canonF);
            } catch (IOException e) {
                return ExceptionUtils.getStackTrace(e);
            }
        }
        return null;
    }

    private static final int CHANNEL_MAX_COUNT = Math.min(64 * 1024 * 1024 - 32 * 1024, Integer.MAX_VALUE);

    public static void copyFile(File in, File out) throws IOException {
        copyFile(in, out, CHANNEL_MAX_COUNT);
    }

    /**
     * Copy a file. It is generally not advised to use 0 for <code>maxCount</code> since various
     * implementations have size limitations, see {@link #copyFile(File, File)}.
     * 
     * @param in the source file.
     * @param out the destination file.
     * @param maxCount the number of bytes to copy at a time, 0 meaning size of <code>in</code>.
     * @throws IOException if an error occurs.
     */
    public static void copyFile(File in, File out, long maxCount) throws IOException {
        final FileChannel sourceChannel = new FileInputStream(in).getChannel();
        final FileChannel destinationChannel = new FileOutputStream(out).getChannel();
        if (maxCount == 0) maxCount = sourceChannel.size();
        try {
            final long size = sourceChannel.size();
            long position = 0;
            while (position < size) {
                position += sourceChannel.transferTo(position, maxCount, destinationChannel);
            }
        } finally {
            sourceChannel.close();
            destinationChannel.close();
        }
    }

    public static void copyFile(File in, File out, final boolean useTime) throws IOException {
        if (!useTime || in.lastModified() != out.lastModified()) {
            copyFile(in, out);
            if (useTime) out.setLastModified(in.lastModified());
        }
    }

    public static void copyDirectory(File in, File out) throws IOException {
        copyDirectory(in, out, Collections.<String>emptySet());
    }

    public static final Set<String> VersionControl = CollectionUtils.createSet(".svn", "CVS");

    public static void copyDirectory(File in, File out, final Set<String> toIgnore) throws IOException {
        copyDirectory(in, out, toIgnore, false);
    }

    public static void copyDirectory(File in, File out, final Set<String> toIgnore, final boolean useTime) throws IOException {
        if (toIgnore.contains(in.getName())) return;
        if (in.isDirectory()) {
            if (!out.exists()) {
                out.mkdir();
            }
            String[] children = in.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(in, children[i]), new File(out, children[i]), toIgnore, useTime);
            }
        } else {
            if (!in.getName().equals("Thumbs.db")) {
                copyFile(in, out, useTime);
            }
        }
    }

    /**
     * Delete recursively the passed directory. If a deletion fails, the method stops attempting to
     * delete and returns false.
     * 
     * @param dir the dir to be deleted.
     * @return <code>true</code> if all deletions were successful.
     */
    public static boolean rmR(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            for (int i = 0; i < children.length; i++) {
                boolean success = rmR(children[i]);
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static void rm_R(File dir) throws IOException {
        if (dir.isDirectory()) {
            for (final File child : dir.listFiles()) {
                rmR(child);
            }
        }
        rm(dir);
    }

    public static void rm(File f) throws IOException {
        if (f.exists() && !f.delete()) throw new IOException("cannot delete " + f);
    }

    public static final File mkdir_p(File dir) throws IOException {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("cannot create directory " + dir);
            }
        }
        return dir;
    }

    /**
     * Create all ancestors of <code>f</code>.
     * 
     * @param f any file whose ancestors should be created.
     * @return <code>f</code>.
     * @throws IOException if ancestors cannot be created.
     */
    public static final File mkParentDirs(File f) throws IOException {
        final File parentFile = f.getParentFile();
        if (parentFile != null) mkdir_p(parentFile);
        return f;
    }

    /**
     * Read a file line by line with the default encoding and returns the concatenation of these.
     * 
     * @param f the file to read.
     * @return the content of f.
     * @throws IOException if a pb occur while reading.
     */
    public static final String read(File f) throws IOException {
        return read(f, null);
    }

    /**
     * Read a file line by line and returns the concatenation of these.
     * 
     * @param f the file to read.
     * @param charset the encoding of <code>f</code>, <code>null</code> means default encoding.
     * @return the content of f.
     * @throws IOException if a pb occur while reading.
     */
    public static final String read(File f, String charset) throws IOException {
        return read(new FileInputStream(f), charset);
    }

    public static final String read(InputStream ins, String charset) throws IOException {
        final Reader reader;
        if (charset == null) reader = new InputStreamReader(ins); else reader = new InputStreamReader(ins, charset);
        return read(reader);
    }

    public static final String read(final Reader reader) throws IOException {
        return read(reader, 8192);
    }

    public static final String read(final Reader reader, final int bufferSize) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final char[] buffer = new char[bufferSize];
        final BufferedReader in = new BufferedReader(reader);
        try {
            while (true) {
                final int count = in.read(buffer);
                if (count == -1) break;
                sb.append(buffer, 0, count);
            }
        } finally {
            in.close();
        }
        return sb.toString();
    }

    /**
     * Read the whole content of a file.
     * 
     * @param f the file to read.
     * @return its content.
     * @throws IOException if a pb occur while reading.
     * @throws IllegalArgumentException if f is longer than <code>Integer.MAX_VALUE</code>.
     */
    public static final byte[] readBytes(File f) throws IOException {
        final InputStream in = new FileInputStream(f);
        if (f.length() > Integer.MAX_VALUE) throw new IllegalArgumentException("file longer than Integer.MAX_VALUE" + f.length());
        final byte[] res = new byte[(int) f.length()];
        in.read(res);
        in.close();
        return res;
    }

    public static void write(String s, File f) throws IOException {
        write(s, f, null, false);
    }

    public static void write(String s, File f, String charset, boolean append) throws IOException {
        final FileOutputStream fileStream = new FileOutputStream(f, append);
        final OutputStreamWriter out = charset == null ? new OutputStreamWriter(fileStream) : new OutputStreamWriter(fileStream, charset);
        final BufferedWriter w = new BufferedWriter(out);
        try {
            w.write(s);
        } finally {
            w.close();
        }
    }

    /**
     * Execute the passed transformer with the lock on the passed file.
     * 
     * @param <T> return type.
     * @param f the file to lock.
     * @param transf what to do on the file.
     * @return what <code>transf</code> returns.
     * @throws Exception if an error occurs.
     */
    public static final <T> T doWithLock(final File f, ExnTransformer<RandomAccessFile, T, ?> transf) throws Exception {
        RandomAccessFile out = null;
        try {
            mkParentDirs(f);
            out = new RandomAccessFile(f, "rw");
            out.getChannel().lock();
            final T res = transf.transformChecked(out);
            out.close();
            out = null;
            return res;
        } catch (final Exception e) {
            Exception toThrow = e;
            if (out != null) try {
                out.close();
            } catch (final IOException e2) {
                toThrow = ExceptionUtils.createExn(IOException.class, "couldn't close: " + e2.getMessage(), e);
            }
            throw toThrow;
        }
    }

    private static final Map<URL, File> files = new HashMap<URL, File>();

    private static final File getShortCutFile() throws IOException {
        return getFile(FileUtils.class.getResource("shortcut.vbs"));
    }

    public static final File getFile(final URL url) throws IOException {
        final File shortcutFile;
        final File currentFile = files.get(url);
        if (currentFile == null || !currentFile.exists()) {
            shortcutFile = File.createTempFile("windowsIsLame", ".vbs");
            shortcutFile.deleteOnExit();
            files.put(url, shortcutFile);
            final InputStream stream = url.openStream();
            final FileOutputStream out = new FileOutputStream(shortcutFile);
            try {
                StreamUtils.copy(stream, out);
            } finally {
                out.close();
                stream.close();
            }
        } else shortcutFile = currentFile;
        return shortcutFile;
    }

    /**
     * Create a symbolic link from <code>link</code> to <code>target</code>.
     * 
     * @param target the target of the link, eg ".".
     * @param link the file to create or replace, eg "l".
     * @return the link if the creation was successfull, <code>null</code> otherwise, eg "l.LNK".
     * @throws IOException if an error occurs.
     */
    public static final File ln(final File target, final File link) throws IOException {
        final String os = System.getProperty("os.name");
        final Process ps;
        final File res;
        if (os.startsWith("Windows")) {
            ps = Runtime.getRuntime().exec(new String[] { "cscript", getShortCutFile().getAbsolutePath(), link.getAbsolutePath(), target.getCanonicalPath() });
            res = new File(link.getParentFile(), link.getName() + ".LNK");
        } else {
            final String rel = FileUtils.relative(link.getAbsoluteFile().getParentFile(), target);
            final String[] cmdarray = { "ln", "-sfn", rel, link.getAbsolutePath() };
            ps = Runtime.getRuntime().exec(cmdarray);
            res = link;
        }
        try {
            final int exitValue = ps.waitFor();
            if (exitValue == 0) return res; else throw new IOException("Abnormal exit value: " + exitValue);
        } catch (InterruptedException e) {
            throw ExceptionUtils.createExn(IOException.class, "interrupted", e);
        }
    }

    /**
     * Resolve a symbolic link or a windows shortcut.
     * 
     * @param link the shortcut, e.g. shortcut.lnk.
     * @return the target of <code>link</code>, <code>null</code> if not found, e.g. target.txt.
     * @throws IOException if an error occurs.
     */
    public static final File readlink(final File link) throws IOException {
        final String os = System.getProperty("os.name");
        final Process ps;
        if (os.startsWith("Windows")) {
            ps = Runtime.getRuntime().exec(new String[] { "cscript", "//NoLogo", getShortCutFile().getAbsolutePath(), link.getAbsolutePath() });
        } else {
            ps = Runtime.getRuntime().exec(new String[] { "readlink", "-f", link.getAbsolutePath() });
        }
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            final String res = reader.readLine();
            reader.close();
            if (ps.waitFor() != 0 || res == null || res.length() == 0) return null; else return new File(res);
        } catch (InterruptedException e) {
            throw ExceptionUtils.createExn(IOException.class, "interrupted", e);
        }
    }

    /**
     * Tries to open the passed file as if it were graphically opened by the current user (respect
     * user's "open with"). If a native way to open the file can't be found, tries the passed list
     * of executables.
     * 
     * @param f the file to open.
     * @param executables a list of executables to try, e.g. ["ooffice", "soffice"].
     * @throws IOException if the file can't be opened.
     */
    public static final void open(File f, String[] executables) throws IOException {
        try {
            openNative(f);
        } catch (IOException exn) {
            for (int i = 0; i < executables.length; i++) {
                final String executable = executables[i];
                try {
                    Runtime.getRuntime().exec(new String[] { executable, f.getCanonicalPath() });
                    return;
                } catch (IOException e) {
                }
            }
            throw ExceptionUtils.createExn(IOException.class, "unable to open " + f + " with: " + Arrays.asList(executables), exn);
        }
    }

    /**
     * Open the passed file as if it were graphically opened by the current user (user's "open
     * with").
     * 
     * @param f the file to open.
     * @throws IOException if f couldn't be opened.
     */
    private static final void openNative(File f) throws IOException {
        final String os = System.getProperty("os.name");
        final String[] cmdarray;
        if (os.startsWith("Windows")) {
            cmdarray = new String[] { "cmd", "/c", "start", "\"\"", f.getCanonicalPath() };
        } else if (os.startsWith("Mac OS")) {
            cmdarray = new String[] { "open", f.getCanonicalPath() };
        } else if (os.startsWith("Linux")) {
            cmdarray = new String[] { "xdg-open", f.getCanonicalPath() };
        } else {
            throw new IOException("unknown way to open " + f);
        }
        try {
            final int res = Runtime.getRuntime().exec(cmdarray).waitFor();
            if (res != 0) throw new IOException("error (" + res + ") executing " + Arrays.asList(cmdarray));
        } catch (InterruptedException e) {
            throw ExceptionUtils.createExn(IOException.class, "interrupted waiting for " + Arrays.asList(cmdarray), e);
        }
    }

    static final boolean gnomeRunning() {
        try {
            return Runtime.getRuntime().exec(new String[] { "pgrep", "-u", System.getProperty("user.name"), "nautilus" }).waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static final Map<String, String> ext2mime;

    static {
        ext2mime = new HashMap<String, String>();
        ext2mime.put(".xml", "text/xml");
        ext2mime.put(".jpg", "image/jpeg");
        ext2mime.put(".png", "image/png");
        ext2mime.put(".tiff", "image/tiff");
    }

    /**
     * Try to guess the media type of the passed file name (see <a
     * href="http://www.iana.org/assignments/media-types">iana</a>).
     * 
     * @param fname a file name.
     * @return its mime type.
     */
    public static final String findMimeType(String fname) {
        for (final Map.Entry<String, String> e : ext2mime.entrySet()) {
            if (fname.toLowerCase().endsWith(e.getKey())) return e.getValue();
        }
        return null;
    }

    /**
     * Return the string after the last dot.
     * 
     * @param fname a name, e.g. "test.odt" or "sans".
     * @return the extension, e.g. "odt" or <code>null</code>.
     */
    public static final String getExtension(String fname) {
        final int lastIndex = fname.lastIndexOf('.');
        return lastIndex < 0 ? null : fname.substring(lastIndex + 1);
    }

    /**
     * Chars not valid in filenames.
     */
    public static final Collection<Character> INVALID_CHARS;

    /**
     * An escaper suitable for producing valid filenames.
     */
    public static final Escaper FILENAME_ESCAPER = new StringUtils.Escaper('\'', 'Q');

    static {
        FILENAME_ESCAPER.add('"', 'D').add(':', 'C').add('/', 'S').add('\\', 'A');
        FILENAME_ESCAPER.add('<', 'L').add('>', 'G').add('*', 'R').add('|', 'P').add('?', 'M');
        INVALID_CHARS = FILENAME_ESCAPER.getEscapedChars();
    }

    public static final FileFilter DIR_FILTER = new FileFilter() {

        @Override
        public boolean accept(File f) {
            return f.isDirectory();
        }
    };

    public static final FileFilter REGULAR_FILE_FILTER = new FileFilter() {

        @Override
        public boolean accept(File f) {
            return f.isFile();
        }
    };

    /**
     * Return a filter that select regular files ending in <code>ext</code>.
     * 
     * @param ext the end of the name, eg ".xml".
     * @return the corresponding filter.
     */
    public static final FileFilter createEndFileFilter(final String ext) {
        return new FileFilter() {

            @Override
            public boolean accept(File f) {
                return f.isFile() && f.getName().endsWith(ext);
            }
        };
    }
}
