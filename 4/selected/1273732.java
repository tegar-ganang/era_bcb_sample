package oscript;

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
        file = root;
        fileDescriptor = file.getAbsolutePath();
        if (file.exists()) zipFile = new ZipFile(file);
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
            };
            OscriptBuiltins.atExit(flusher);
            needsFlush = true;
        }
    }

    /**
   * If any changes have been made, flush them out to disk.
   */
    protected synchronized void flush() throws IOException {
        flushing = true;
        try {
            if (needsFlush) {
                needsFlush = false;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(bos);
                if (zipFile != null) {
                    for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
                        ZipEntry zipEntry = (ZipEntry) (e.nextElement());
                        JarFile jarFile = (JarFile) (jarFileTable.get(zipEntry.getName()));
                        if (jarFile == null) writeZipEntry(zos, zipEntry, zipFile.getInputStream(zipEntry));
                    }
                }
                for (Enumeration e = jarFileTable.keys(); e.hasMoreElements(); ) {
                    String name = (String) (e.nextElement());
                    JarFile jarFile = (JarFile) (jarFileTable.get(name));
                    if (jarFile.exists() && !jarFile.isDirectory()) {
                        jarFile.flush();
                        writeZipEntry(zos, jarFile.getZipEntry(), jarFile.getInputStream());
                    }
                }
                if (zipFile != null) zipFile.close();
                zos.flush();
                zos.close();
                file.delete();
                file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file);
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
        byte[] buf = new byte[1024];
        int in;
        while ((in = is.read(buf, 0, buf.length)) > 0) zos.write(buf, 0, in);
        zos.closeEntry();
    }

    /**
   * Try to resolve the specified path.  If unresolved, return <code>null</code>.
   * 
   * @param mountPath    the path this fs is mounted at to resolve the requested file
   * @param path         path to file
   * @return file or <code>null</code>
   */
    protected AbstractFile resolveInFileSystem(String mountPath, String path) {
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
   * @return an iterator of <code>AbstractFile</code>
   */
    protected synchronized Iterator childrenInFileSystem(String mountPath, String path) throws IOException {
        LinkedList childList = new LinkedList();
        Hashtable childTable = new Hashtable();
        flush();
        if (zipFile != null) {
            for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
                ZipEntry ze = (ZipEntry) (e.nextElement());
                String name = ze.getName();
                if (!name.startsWith("META-INF") && name.startsWith(path)) {
                    int plen = path.length();
                    if ((plen < name.length()) && (name.charAt(plen) == '/')) plen++;
                    int idx = name.indexOf('/', plen);
                    String childName = name.substring(plen, (idx != -1) ? idx : name.length());
                    if ((childName.length() > 0) && (childTable.get(childName) == null)) {
                        childTable.put(childName, childName);
                        AbstractFile file = resolveInFileSystem(mountPath, ((plen == 0) ? "" : (path + SEPERATOR_CHAR)) + childName);
                        childList.add(file);
                    }
                }
            }
        }
        return childList.iterator();
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

        private ZipEntry zipEntry;

        ZipEntry getZipEntry() {
            synchronized (JarFileSystem.this) {
                if ((zipEntry == null) && (zipFile != null)) zipEntry = zipFile.getEntry(path + "/");
                if ((zipEntry == null) && (zipFile != null)) zipEntry = zipFile.getEntry(path);
                return zipEntry;
            }
        }

        /**
     * Buffer for write data to be flushed to disk.  Contains entire file
     * contents.  If this is not <code>null</code> it supercedes data from
     * ZipFile.
     */
        byte[] buf;

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
            ZipEntry zipEntry = getZipEntry();
            if (zipEntry != null) return zipEntry.isDirectory(); else return false;
        }

        /**
     * Test whether this file is a regular file.  A file is a regular file if
     * it is not a directory.
     * 
     * @return <code>true</code> iff this file is a regular file.
     */
        public boolean isFile() {
            ZipEntry zipEntry = getZipEntry();
            if (zipEntry != null) return !zipEntry.isDirectory(); else return false;
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
            synchronized (JarFileSystem.this) {
                ZipEntry zipEntry = getZipEntry();
                if (zipEntry != null) return zipEntry.getTime(); else return -1;
            }
        }

        /**
     * Return the length of the file in bytes.
     */
        public long length() {
            if (buf != null) return buf.length; else return getZipEntry().getSize();
        }

        /**
     * Create a new empty file, if it does not yet exist.
     * 
     * @return <code>true</code> iff the file does not exist and was successfully
     * created.
     * @throws IOException if error
     */
        public boolean createNewFile() throws java.io.IOException {
            synchronized (JarFileSystem.this) {
                boolean rc = (getZipEntry() == null);
                zipEntry = new ZipEntry(path);
                buf = EMPTY_BUF;
                return rc;
            }
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
        public InputStream getInputStream() throws java.io.IOException {
            if (isDirectory()) throw new java.io.IOException("cannot read directory: " + this);
            synchronized (JarFileSystem.this) {
                ZipEntry zipEntry = getZipEntry();
                if (zipEntry == null) throw new java.io.IOException("does not exist");
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
        public OutputStream getOutputStream(boolean append) throws java.io.IOException {
            if (isDirectory()) throw new java.io.IOException("cannot write directory: " + this);
            synchronized (JarFileSystem.this) {
                ZipEntry zipEntry = getZipEntry();
                if (zipEntry == null) throw new java.io.IOException("does not exist");
                return new BufferOutputStream(append);
            }
        }

        /**
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
                zipEntry.setTime(System.currentTimeMillis());
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
