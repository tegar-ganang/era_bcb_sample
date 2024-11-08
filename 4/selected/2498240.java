package oscript.fs;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import oscript.exceptions.ProgrammingErrorException;

/**
 * A <code>JarFileSystem</code> implements a filesystem on top of a <i>.jar</i>
 * or <i>.zip</i> file.
 * 
 * @author Rob Clark (rob@ti.com)
 * <!--$Format: " * @version $Revision$"$-->
 * @version 1.21
 */
public class JarFileSystem extends AbstractFileSystem {

    private File file;

    private ZipFile zipFile;

    private String fileDescriptor;

    private final boolean autoflush;

    /**
   * Maps path to JarFile.
   */
    private Hashtable jarFileTable = new Hashtable();

    /**
   * Class Constructor.
   *
   * @param root         the root of the local filesystem
   */
    public JarFileSystem(File root) throws ZipException, IOException {
        this(root, false);
    }

    /**
   * Class Constructor.
   * 
   * @param root         the root of the local filesystem
   * @param autoflush    if <code>true</code> automatically flush the entire
   *    filesystem.  Note that flushing entire filesystem to disk involves
   *    writing the entire jar file, and can be expensive
   */
    public JarFileSystem(File root, boolean autoflush) throws ZipException, IOException {
        file = root;
        fileDescriptor = file.getAbsolutePath();
        this.autoflush = autoflush;
        if (file.exists()) zipFile = new ZipFile(fileDescriptor);
    }

    /**
   * Class Constructor.
   * 
   * @param root         the root of the local filesystem
   */
    public JarFileSystem(String root) throws ZipException, IOException {
        this(new File(root));
    }

    protected synchronized void finalize() throws Throwable {
        flush();
    }

    private boolean flushing = false;

    private boolean needsFlush = false;

    private Runnable flusher = null;

    private synchronized void scheduleFlush() {
        if (!flushing && (flusher == null)) {
            flusher = new Runnable() {

                public void run() {
                    try {
                        synchronized (JarFileSystem.this) {
                            flush();
                            flusher = null;
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }

                public String toString() {
                    return "[flusher, file=" + file + "]";
                }
            };
            needsFlush = true;
            if (autoflush) flusher.run(); else oscript.OscriptBuiltins.atExit(flusher);
        }
    }

    /**
   * If any changes have been made, flush them out to disk.
   */
    public synchronized void flush() throws IOException {
        flushing = true;
        try {
            if (needsFlush) {
                needsFlush = false;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(bos);
                if (zipFile != null) {
                    for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
                        ZipEntry zipEntry = (ZipEntry) (e.nextElement());
                        JarFile jarFile = (JarFile) (jarFileTable.get(getZipEntryName(zipEntry)));
                        if (jarFile == null) writeZipEntry(zos, zipEntry, zipFile.getInputStream(zipEntry));
                    }
                }
                for (Enumeration e = jarFileTable.keys(); e.hasMoreElements(); ) {
                    String name = (String) (e.nextElement());
                    JarFile jarFile = (JarFile) (jarFileTable.get(name));
                    if (jarFile.exists()) {
                        if (jarFile.isDirectory()) {
                            writeZipEntry(zos, jarFile.getZipEntry(), null);
                        } else {
                            jarFile.flush();
                            writeZipEntry(zos, jarFile.getZipEntry(), jarFile.getInputStream());
                        }
                    }
                }
                if (zipFile != null) zipFile.close();
                zos.flush();
                zos.close();
                file.delete();
                file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
                fos.write(bos.toByteArray());
                fos.flush();
                fos.close();
                zipFile = new ZipFile(file, ZipFile.OPEN_READ);
            }
        } finally {
            flushing = false;
        }
    }

    /**
   * Utility to write the specified entry to a file...
   */
    private void writeZipEntry(ZipOutputStream zos, ZipEntry zipEntry, InputStream is) throws IOException {
        zos.putNextEntry(zipEntry);
        if (is != null) {
            byte[] buf = new byte[1024];
            int in;
            while ((in = is.read(buf, 0, buf.length)) > 0) zos.write(buf, 0, in);
        }
        zos.closeEntry();
    }

    /**
   * Since the zip entry name may end with trailing '/', strip them off.
   */
    private String getZipEntryName(ZipEntry ze) {
        String name = ze.getName();
        while ((name.length() > 0) && name.endsWith("/")) name = name.substring(0, name.length() - 1);
        return name;
    }

    /**
   * Try to resolve the specified path.  If unresolved, return <code>null</code>.
   * 
   * @param mountPath    the path this fs is mounted at to resolve the requested file
   * @param path         path to file
   * @return file or <code>null</code>
   */
    protected AbstractFile resolveInFileSystem(String mountPath, String path) {
        if (path.endsWith("/")) throw new ProgrammingErrorException("this is bad, path=" + path);
        JarFile jarFile = (JarFile) (jarFileTable.get(path));
        if (jarFile == null) {
            jarFile = new JarFile(mountPath, path);
            jarFileTable.put(path, jarFile);
        }
        return jarFile;
    }

    /**
   * Return an iterator of children of the specified path.
   * 
   * @param mountPath    the path this fs is mounted at to resolve the requested file
   * @param path         path to file, relative to <code>mountPath</code>
   * @return a collection of <code>AbstractFile</code>
   */
    protected synchronized Collection childrenInFileSystem(String mountPath, String path) throws IOException {
        Hashtable childTable = new Hashtable();
        if (zipFile != null) for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) childrenInFileSystemHelper(mountPath, path, childTable, ((ZipEntry) (e.nextElement())).getName());
        for (Enumeration e = jarFileTable.keys(); e.hasMoreElements(); ) childrenInFileSystemHelper(mountPath, path, childTable, (String) (e.nextElement()));
        return childTable.values();
    }

    private void childrenInFileSystemHelper(String mountPath, String path, Hashtable childTable, String name) {
        if (name.startsWith("META-INF")) return;
        if (name.startsWith(path)) {
            int plen = path.length();
            if ((plen < name.length()) && (name.charAt(plen) == '/')) plen++;
            int idx = name.indexOf('/', plen);
            String childName = name.substring(plen, (idx != -1) ? idx : name.length());
            if ((childName.length() > 0) && (childTable.get(childName) == null)) {
                AbstractFile file = resolveInFileSystem(mountPath, ((plen == 0) ? "" : (path + SEPERATOR_CHAR)) + childName);
                if (file.exists()) childTable.put(childName, file);
            }
        }
    }

    private static final byte[] EMPTY_BUF = new byte[0];

    /**
   * An interface to be implemented by something that can implement file-like
   * operations.  Ie read as a stream, write as a stream.  Note that a single
   * <code>JarFile</code> instance represent any given path.
   */
    class JarFile implements AbstractFile {

        private String mountPath;

        private String path;

        private String entryDescriptor;

        private String ext;

        /**
     * Since ZipEntry#getTime() seems to be very inefficient, in 
     * particular it seems to create a lot of garbage to be GC'd, we
     * cache the value.
     */
        private long lastModified = -1;

        /**
     * Buffer for write data to be flushed to disk.  Contains entire file
     * contents.  If this is not <code>null</code> it supercedes data from
     * ZipFile.
     */
        byte[] buf;

        private ZipEntry zipEntry;

        ZipEntry getZipEntry() {
            synchronized (JarFileSystem.this) {
                if (zipFile != null) {
                    if (zipEntry == null) zipEntry = zipFile.getEntry(path + "/");
                    if (zipEntry == null) zipEntry = zipFile.getEntry(path);
                }
                return zipEntry;
            }
        }

        /**
     * Class Constructor.
     */
        JarFile(String mountPath, String path) {
            this.mountPath = mountPath;
            this.path = path;
            entryDescriptor = fileDescriptor + "@@/" + path;
            int idx = path.lastIndexOf('.');
            if (idx != -1) ext = path.substring(idx + 1); else ext = "";
        }

        /**
     * Get the extension, which indicates the type of file.  Usually the extension
     * is part of the filename, ie. if the extension was <i>os</i>, the filename
     * would end with <i>.os</i>.
     *
     * @return a string indicating the type of file
     */
        public String getExtension() {
            return ext;
        }

        /**
     * Is it possible to write to this file.
     */
        public boolean canWrite() {
            return exists() && file.canWrite();
        }

        /**
     * Is it possible to read from this file.
     */
        public boolean canRead() {
            return exists() && file.canRead();
        }

        /**
     * Tests whether the file denoted by this abstract pathname exists.
     *
     * @return  <code>true</code> iff the file exists
     */
        public boolean exists() {
            return getZipEntry() != null;
        }

        /**
     * Test whether this file is a directory.
     * 
     * @return <code>true</code> iff this file is a directory
     */
        public boolean isDirectory() {
            return exists() && getZipEntry().isDirectory();
        }

        /**
     * Test whether this file is a regular file.  A file is a regular file if
     * it is not a directory.
     * 
     * @return <code>true</code> iff this file is a regular file.
     */
        public boolean isFile() {
            return exists() && !getZipEntry().isDirectory();
        }

        /**
     * Return the time of last modification.  The meaning of these value is not
     * so important, but the implementation must ensure that a higher value is
     * returned for the more recent version of a given element.  Ie. if at some
     * point this returns X, an <code>AbstractFile</code> representing the same
     * "file", but created at a later time, should return X if the file has not
     * been modified, or >X if the file has been modified.
     * 
     * @return larger value indicates more recent modification
     */
        public long lastModified() {
            if (lastModified != -1) return lastModified;
            synchronized (JarFileSystem.this) {
                if (exists()) return lastModified = getZipEntry().getTime(); else return -1;
            }
        }

        /**
     * Return the length of the file in bytes.
     */
        public long length() {
            if (!exists()) return -1; else if (buf != null) return buf.length; else return getZipEntry().getSize();
        }

        /**
     * Create a new empty file, if it does not yet exist.
     * 
     * @return <code>true</code> iff the file does not exist and was 
     *    successfully created.
     * @throws IOException if error
     */
        public boolean createNewFile() throws IOException {
            return createImpl(false);
        }

        /**
     * If this file does not exist, create it as a directory.
     * 
     * @return <code>true</code> iff directory successfully created
     */
        public boolean mkdir() throws IOException {
            String parentPath = dirname(path);
            if (parentPath != null) if (!resolveInFileSystem(mountPath, parentPath).exists()) throw new IOException("parent directory does not exist");
            return createImpl(true);
        }

        /**
     * If this file does not exist, create it as a directory.  All necessary
     * parent directories are also created.  If this operation fails, it may
     * have successfully created some or all of the parent directories.
     * 
     * @return <code>true</code> iff directory successfully created
     */
        public boolean mkdirs() throws IOException {
            return createImpl(true);
        }

        private boolean createImpl(boolean isDir) throws IOException {
            synchronized (JarFileSystem.this) {
                boolean rc = exists();
                if (!rc) {
                    String parentPath = dirname(path);
                    if (parentPath != null) {
                        AbstractFile parent = resolveInFileSystem(mountPath, parentPath);
                        parent.mkdirs();
                        parent.touch();
                    } else {
                        touchMountPoint(JarFileSystem.this);
                    }
                    String path = this.path;
                    if (isDir) path += "/";
                    zipEntry = new ZipEntry(path);
                    buf = EMPTY_BUF;
                    touch();
                }
                return rc;
            }
        }

        /**
     * Update the timestamp on this file to the current time.
     * 
     * @throws IOException if error
     */
        public void touch() throws IOException {
            if (!exists()) throw new IOException("does not exist");
            getZipEntry().setTime(lastModified = System.currentTimeMillis());
            scheduleFlush();
        }

        /**
     * Delete this file.  If this file is a directory, then the directory must
     * be empty.
     * 
     * @return <code>true<code> iff the directory is successfully deleted.
     * @throws IOException if error
     */
        public boolean delete() throws IOException {
            throw new ProgrammingErrorException("unimplemented");
        }

        /**
     * Get an input stream to read from this file.
     * 
     * @return input stream
     * @throws IOException if <code>canRead</code> returns <code>true</code>
     * @see #canRead
     */
        public InputStream getInputStream() throws IOException {
            if (isDirectory()) throw new IOException("cannot read directory: " + this);
            synchronized (JarFileSystem.this) {
                if (!exists()) throw new IOException("does not exist");
                flush();
                if (buf == null) return zipFile.getInputStream(zipEntry); else return new ByteArrayInputStream(buf);
            }
        }

        /**
     * Get an output stream to write to this file.
     * 
     * @return output stream
     * @throws IOException if <code>canWrite</code> returns <code>false</code>
     * @see #canWrite
     */
        public OutputStream getOutputStream(boolean append) throws IOException {
            if (isDirectory()) throw new IOException("cannot write directory: " + this);
            synchronized (JarFileSystem.this) {
                if (!exists()) throw new IOException("does not exist");
                return new BufferOutputStream(append);
            }
        }

        /**
     * Be the same as the entryDescriptor, to make JarFiles useful as a
     * key into a hash table.
     */
        public int hashCode() {
            return entryDescriptor.hashCode();
        }

        /**
     */
        public void flush() throws IOException {
            for (Iterator itr = bosMap.keySet().iterator(); itr.hasNext(); ) ((BufferOutputStream) (itr.next())).flush();
        }

        /**
     * Keep track of outstanding output streams, so we can force them
     * to flush iff needed.
     */
        private WeakHashMap bosMap = new WeakHashMap();

        /**
     */
        public boolean equals(Object obj) {
            return (obj instanceof JarFile) && entryDescriptor.equals(((JarFile) obj).entryDescriptor);
        }

        /**
     * Rather than use ByteArrayOutputStream directly, we need to overload so
     * we have control over flushing and finalizing.  Note that zipEntry must
     * be created (ie. the file entry must exist) in order to create the out-
     * put stream.
     */
        private class BufferOutputStream extends ByteArrayOutputStream {

            private boolean append;

            BufferOutputStream(boolean append) {
                this.append = append;
                bosMap.put(this, null);
            }

            public void flush() throws IOException {
                if (append) throw new ProgrammingErrorException("unimplemented"); else setBuf(toByteArray());
            }

            public void close() throws IOException {
                flush();
            }
        }

        /**
     * Accessor used by a BufferOutputStream to update the buf... does
     * extra house keeping like setting last modified time, etc.
     */
        private void setBuf(byte[] newBuf) {
            synchronized (JarFileSystem.this) {
                ZipEntry zipEntry = getZipEntry();
                buf = newBuf;
                zipEntry.setSize(buf.length);
                zipEntry.setCompressedSize(-1);
                zipEntry.setTime(lastModified = System.currentTimeMillis());
                scheduleFlush();
            }
        }

        /**
     * Get the file path, which globally identifies the file.
     * 
     * @return a unique string
     * @see #getName
     */
        public String getPath() {
            return mountPath + "/" + path;
        }

        /**
     * Get the name of this file, which is the last component of the complete path.
     * 
     * @return the file name
     * @see #getPath
     */
        public String getName() {
            return basename(getPath());
        }

        public String toString() {
            return getPath();
        }
    }
}
