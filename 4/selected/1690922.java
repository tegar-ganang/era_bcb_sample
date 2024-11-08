package x.java.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import jvs.vfs.FileComparator;
import jvs.vfs.FileSystem;
import jvs.vfs.IFile;
import jvs.vfs.util.Log;
import x.java.lang.System;

/**
 * @author qiangli
 * 
 */
public class File extends java.io.File {

    private static final long serialVersionUID = 1L;

    public static final String separator = "/";

    public static final char separatorChar = '/';

    public static final String pathSeparator = ";";

    public static final char pathSeparatorChar = ';';

    private static long seq = System.currentTimeMillis();

    private static FileSystem fs = FileSystem.getFileSystem();

    private static final String vtmpdir = "v:/tmp/";

    private static void checkTemp() {
        File vtmp = new File(vtmpdir);
        if (!vtmp.exists() || !vtmp.toRealURI().getScheme().equals("file")) {
            java.io.File tmpdir = new java.io.File(java.lang.System.getProperty("java.io.tmpdir"));
            vtmp.mkdirs();
            fs.mount(vtmp.getPath(), tmpdir.toURI(), "rw");
        }
    }

    private static void copy(InputStream is, OutputStream os) throws IOException {
        InputStream bis = null;
        OutputStream bos = null;
        try {
            if (is instanceof BufferedInputStream) {
                bis = is;
            } else {
                bis = new BufferedInputStream(is);
            }
            if (os instanceof BufferedOutputStream) {
                bos = os;
            } else {
                bos = new BufferedOutputStream(os);
            }
            byte[] buf = new byte[1024];
            int nread = -1;
            while ((nread = bis.read(buf)) != -1) {
                bos.write(buf, 0, nread);
            }
        } finally {
            bos.close();
            bis.close();
        }
    }

    private static java.io.File createLocalFile(x.java.io.File file, boolean deleteOnExit) throws IOException {
        file.checkReadable();
        java.io.File cached = null;
        InputStream is = null;
        java.io.FileOutputStream os = null;
        try {
            String ext = file.getExt();
            if (ext == null || ext.length() == 0) {
                ext = "cache";
            }
            cached = java.io.File.createTempFile("vfs", "." + ext);
            if (deleteOnExit) {
                cached.deleteOnExit();
            }
            is = file.getInputStream();
            os = new java.io.FileOutputStream(cached);
            copy(is, os);
        } catch (IOException e) {
            throw new IOException("can't copy to local cache: " + e);
        } finally {
            is.close();
            os.close();
        }
        return cached;
    }

    public static java.io.File createTempFile(String prefix, String suffix) throws IOException {
        checkTemp();
        return new File(vtmpdir + prefix + getSeq() + suffix);
    }

    public static java.io.File createTempFile(String prefix, String suffix, java.io.File directory) throws IOException {
        checkTemp();
        return new File(vtmpdir + prefix + getSeq() + suffix);
    }

    /**
	 * Dissect the specified absolute path.
	 * 
	 * @param path
	 *            the path to dissect.
	 * @return String[] {root, remaining path}.
	 * @throws java.lang.NullPointerException
	 *             if path is null.
	 */
    private static String[] dissect(String path) {
        char sep = '/';
        path = path.replace('\\', sep);
        String root = "v:";
        int colon = path.indexOf(':');
        int next = colon + 1;
        char[] ca = path.toCharArray();
        if (ca[next] == sep) {
            root += sep;
        }
        next = (ca[next] == sep) ? next + 1 : next;
        StringBuffer sbPath = new StringBuffer();
        for (int i = next; i < ca.length; i++) {
            if (ca[i] != sep || ca[i - 1] != sep) {
                sbPath.append(ca[i]);
            }
        }
        path = sbPath.toString();
        return new String[] { root, path };
    }

    private static synchronized String getSeq() {
        ++seq;
        return Long.toString(seq, 16);
    }

    private static boolean isAbsolutePath(String filename) {
        return (filename.startsWith("v:/") || filename.startsWith("V:/") || filename.startsWith("/"));
    }

    public static java.io.File[] listRoots() {
        return fs.listRoots();
    }

    /**
	 * This method is taken and modified from Ant 1.7 FileUtils class
	 * 
	 * Normalize the given absolute path.
	 * 
	 * <p>
	 * This includes:
	 * <ul>
	 * <li>Remove redundant slashes</li>
	 * <li>Resolve all ./, .\, ../ and ..\ sequences.</li>
	 * </ul>
	 * 
	 * @param path
	 *            the path to be normalized.
	 * @return the normalized version of the path.
	 * 
	 * @throws java.lang.NullPointerException
	 *             if path is null.
	 */
    private static String normalize(String path) {
        if (path == null) {
            return null;
        }
        if (path.equals("") || path.equals("v:")) {
            return "v:";
        }
        if (path.equals("/") || path.equals("v:/")) {
            return "v:/";
        }
        Stack s = new Stack();
        String[] dissect = dissect(path);
        s.push(dissect[0]);
        StringTokenizer tok = new StringTokenizer(dissect[1], "/");
        while (tok.hasMoreTokens()) {
            String thisToken = tok.nextToken();
            if (".".equals(thisToken)) {
                continue;
            } else if ("..".equals(thisToken)) {
                if (s.size() < 2) {
                    return path;
                }
                s.pop();
            } else {
                s.push(thisToken);
            }
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.size(); i++) {
            if (i > 1) {
                sb.append("/");
            }
            sb.append(s.elementAt(i));
        }
        return sb.toString() + (path.endsWith("/") ? "/" : "");
    }

    private static String slashend(String p, boolean isDirectory) {
        if (isDirectory && !p.endsWith("/")) {
            p += "/";
        }
        return p;
    }

    protected IFile file = null;

    protected String uri = null;

    /**
	 * Creates a new File instance from a parent abstract pathname and a child
	 * pathname string. If parent is null then the new File instance is created
	 * as if by invoking the single-argument File constructor on the given child
	 * pathname string. Otherwise the parent abstract pathname is taken to
	 * denote a directory, and the child pathname string is taken to denote
	 * either a directory or a file. If the child pathname string is absolute
	 * then it is converted into a relative pathname in a system-dependent way.
	 * If parent is the empty abstract pathname then the new File instance is
	 * created by converting child into an abstract pathname and resolving the
	 * result against a system-dependent default directory. Otherwise each
	 * pathname string is converted into an abstract pathname and the child
	 * abstract pathname is resolved against the parent. Parameters: parent -
	 * The parent abstract pathname child - The child pathname string Throws:
	 * NullPointerException - If child is null
	 */
    public File(File parent, String child) {
        super("/");
        if (child == null) {
            throw new NullPointerException();
        }
        if (parent == null) {
            uri = normalize(child);
            return;
        }
        if (parent.uri.equals("") || parent.uri.equals("v:")) {
            uri = normalize(fs.getDefaultDirectory().getPath() + "/" + child);
            return;
        }
        uri = normalize(parent.getPath() + "/" + child);
    }

    /**
	 * @param parent
	 * @param child
	 */
    public File(java.io.File parent, String child) {
        this((File) parent, child);
    }

    /**
	 * Creates a new File instance by converting the given pathname string into
	 * an abstract pathname. If the given string is the empty string, then the
	 * result is the empty abstract pathname. Parameters: pathname - A pathname
	 * string Throws: NullPointerException - If the pathname argument is null
	 */
    public File(String pathname) {
        super("/");
        if (pathname == null) {
            throw new NullPointerException();
        }
        if (pathname.equals("")) {
            uri = "";
            return;
        }
        uri = normalize(pathname);
    }

    /**
	 * Creates a new File instance from a parent pathname string and a child
	 * pathname string. If parent is null then the new File instance is created
	 * as if by invoking the single-argument File constructor on the given child
	 * pathname string. Otherwise the parent pathname string is taken to denote
	 * a directory, and the child pathname string is taken to denote either a
	 * directory or a file. If the child pathname string is absolute then it is
	 * converted into a relative pathname in a system-dependent way. If parent
	 * is the empty string then the new File instance is created by converting
	 * child into an abstract pathname and resolving the result against a
	 * system-dependent default directory. Otherwise each pathname string is
	 * converted into an abstract pathname and the child abstract pathname is
	 * resolved against the parent. Parameters: parent - The parent pathname
	 * string child - The child pathname string Throws: NullPointerException -
	 * If child is null
	 */
    public File(String parent, String child) {
        super("/");
        if (child == null) {
            throw new NullPointerException();
        }
        if (parent == null) {
            uri = normalize(child);
            return;
        }
        if (parent.equals("") || parent.equals("v:")) {
            uri = normalize(fs.getDefaultDirectory().getPath() + "/" + child);
            return;
        }
        uri = normalize(parent + "/" + child);
    }

    /**
	 * Creates a new File instance by converting the given file: URI into an
	 * abstract pathname. The exact form of a file: URI is system-dependent,
	 * hence the transformation performed by this constructor is also
	 * system-dependent. For a given abstract pathname f it is guaranteed that
	 * new File( f.toURI()).equals( f.getAbsoluteFile()) so long as the original
	 * abstract pathname, the URI, and the new abstract pathname are all created
	 * in (possibly different invocations of) the same Java virtual machine.
	 * This relationship typically does not hold, however, when a file: URI that
	 * is created in a virtual machine on one operating system is converted into
	 * an abstract pathname in a virtual machine on a different operating
	 * system. Parameters: uri - An absolute, hierarchical URI with a scheme
	 * equal to "file", a non-empty path component, and undefined authority,
	 * query, and fragment components Throws: NullPointerException - If uri is
	 * null IllegalArgumentException - If the preconditions on the parameter do
	 * not hold
	 */
    public File(URI u) {
        super("/");
        if (u == null) {
            throw new NullPointerException();
        }
        String scheme = u.getScheme();
        if ((scheme != null) && !scheme.equalsIgnoreCase("v")) throw new IllegalArgumentException("URI scheme is not \"v\" : " + u);
        if (u.getAuthority() != null) throw new IllegalArgumentException("URI has an authority component: " + u);
        if (u.getFragment() != null) throw new IllegalArgumentException("URI has a fragment component: " + u);
        if (u.getQuery() != null) throw new IllegalArgumentException("URI has a query component: " + u);
        String p = u.getPath();
        if (p == null) {
            throw new NullPointerException("URI path is null: " + u);
        }
        uri = normalize(p);
    }

    /**
	 * Tests whether the application can read the file denoted by this abstract
	 * pathname. Returns: true if and only if the file specified by this
	 * abstract pathname exists and can be read by the application; false
	 * otherwise Throws: SecurityException - If a security manager exists and
	 * its SecurityManager.checkRead(java.lang.String) method denies read access
	 * to the file
	 */
    public boolean canRead() {
        try {
            return (fs.canRead(this) && getIFile().canRead());
        } catch (Exception e) {
            debug(e);
        }
        return false;
    }

    /**
	 * Tests whether the application can modify to the file denoted by this
	 * abstract pathname. Returns: true if and only if the file system actually
	 * contains a file denoted by this abstract pathname and the application is
	 * allowed to write to the file; false otherwise. Throws: SecurityException -
	 * If a security manager exists and its
	 * SecurityManager.checkWrite(java.lang.String) method denies write access
	 * to the file
	 */
    public boolean canWrite() {
        try {
            return (fs.canWrite(this) && getIFile().canWrite());
        } catch (Exception e) {
            debug(e);
        }
        return false;
    }

    /**
	 * If the named file does not exist, is a directory rather than a regular
	 * file, or for some other reason cannot be opened for reading then a
	 * FileNotFoundException is thrown.
	 * 
	 * @throws FileNotFoundException
	 */
    public void checkReadable() throws FileNotFoundException {
        IFile f = getIFile();
        if (!fs.canRead(this) || !f.exists() || f.isDirectory() || !f.canRead()) {
            throw new FileNotFoundException(getPath());
        }
    }

    /**
	 * If the file exists but is a directory rather than a regular file, does
	 * not exist but cannot be created, or cannot be opened for any other reason
	 * then a FileNotFoundException is thrown.
	 * 
	 * @throws FileNotFoundException
	 * 
	 */
    public void checkWritable() throws FileNotFoundException {
        IFile f = getIFile();
        if (!fs.canWrite(this) || (f.exists() && (!f.isFile() || !f.canWrite()))) {
            throw new FileNotFoundException(getPath());
        }
    }

    public int compareTo(java.io.File pathname) {
        return FileComparator.compare(this, (File) pathname);
    }

    /**
	 * @param child
	 */
    public boolean copy(File dest) {
        try {
            if (!canRead() || !dest.canWrite()) {
                return false;
            }
            boolean rc = getIFile().copy(fs.resolve(dest));
            if (rc == false) {
            }
        } catch (Exception e) {
            debug(e);
        }
        return false;
    }

    /**
	 * Atomically creates a new, empty file named by this abstract pathname if
	 * and only if a file with this name does not yet exist. The check for the
	 * existence of the file and the creation of the file if it does not exist
	 * are a single operation that is atomic with respect to all other
	 * filesystem activities that might affect the file. Note: this method
	 * should not be used for file-locking, as the resulting protocol cannot be
	 * made to work reliably. The FileLock facility should be used instead.
	 * Returns: true if the named file does not exist and was successfully
	 * created; false if the named file already exists Throws: IOException - If
	 * an I/O error occurred SecurityException - If a security manager exists
	 * and its SecurityManager.checkWrite(java.lang.String) method denies write
	 * access to the file
	 */
    public boolean createNewFile() throws IOException {
        try {
            return (fs.canWrite(this) && getIFile().create());
        } catch (Exception e) {
            debug(e);
        }
        return false;
    }

    protected void debug(Object o) {
        if (o instanceof Throwable) {
            Throwable t = (Throwable) o;
            return;
        }
        Log.log(Log.DEBUG, this, o);
    }

    /**
	 * Deletes the file or directory denoted by this abstract pathname. If this
	 * pathname denotes a directory, then the directory must be empty in order
	 * to be deleted. Returns: true if and only if the file or directory is
	 * successfully deleted; false otherwise Throws: SecurityException - If a
	 * security manager exists and its
	 * SecurityManager.checkDelete(java.lang.String) method denies delete access
	 * to the file
	 */
    public boolean delete() {
        try {
            return (fs.canWrite(this) && getIFile().delete());
        } catch (Exception e) {
            debug(e);
        }
        return false;
    }

    public void deleteOnExit() {
        try {
            if (fs.canWrite(this)) {
                getIFile().deleteOnExit();
            }
        } catch (Exception e) {
            debug(e);
        }
    }

    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof File)) {
            return this.getPath().equals(((File) obj).getPath());
        }
        return false;
    }

    /**
	 * Tests whether the file or directory denoted by this abstract pathname
	 * exists. Returns: true if and only if the file or directory denoted by
	 * this abstract pathname exists; false otherwise Throws: SecurityException -
	 * If a security manager exists and its
	 * SecurityManager.checkRead(java.lang.String) method denies read access to
	 * the file or directory
	 */
    public boolean exists() {
        try {
            return fs.canRead(this) && getIFile().exists();
        } catch (Exception e) {
            debug(e);
        }
        return false;
    }

    public java.io.File getAbsoluteFile() {
        return this;
    }

    public String getAbsolutePath() {
        return this.getPath();
    }

    public Object getAttribute(String name) {
        return null;
    }

    public Map getAttributes() {
        return null;
    }

    public java.io.File getCanonicalFile() throws IOException {
        return this;
    }

    public String getCanonicalPath() throws IOException {
        return this.getPath();
    }

    public String getContent() {
        if (!fs.canRead(this)) {
            return null;
        }
        return getIFile().getContent();
    }

    public String getDisplayName() {
        if (!fs.canRead(this)) {
            return null;
        }
        return getIFile().getDisplayName();
    }

    public String getExt() {
        String n = getName();
        if (n == null || n.equals("")) {
            return "";
        }
        int idx = n.lastIndexOf(".");
        if (idx == -1) {
            return "";
        }
        return n.substring(idx + 1);
    }

    /**
	 * lazy initialization
	 */
    private synchronized IFile getIFile() {
        if (!isAbsolute()) {
            return null;
        }
        if (file == null) {
            try {
                File parent = this;
                while (true) {
                    parent = (File) parent.getParentFile();
                    if (parent == null) {
                        break;
                    }
                    parent.exists();
                }
                file = fs.createIFile(this);
                if (file.isLink()) {
                    String base = this.getPath();
                    String c = file.getContent().replaceAll("\t", " ");
                    StringTokenizer st = new StringTokenizer(c, "\r\n");
                    String u = st.nextToken().trim();
                    String options = "r";
                    if (st.hasMoreTokens()) {
                        options = st.nextToken().trim();
                        if (options.length() == 0) {
                            options = "r";
                        }
                    }
                    fs.mount(base, u, options);
                    file = fs.createIFile(this);
                    return file;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return file;
    }

    public InputStream getInputStream() throws FileNotFoundException {
        checkReadable();
        try {
            return getIFile().getInputStream();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
	 * Returns the name of the file or directory denoted by this abstract
	 * pathname. This is just the last name in the pathname's name sequence. If
	 * the pathname's name sequence is empty, then the empty string is returned.
	 * Returns: The name of the file or directory denoted by this abstract
	 * pathname, or the empty string if this pathname's name sequence is empty
	 */
    public String getName() {
        String p = getPath();
        if (p == null || p.equals("") || p.equals("/") || p.equals("v:/") || p.equals("v:")) {
            return "";
        }
        if (p.endsWith(separator)) {
            p = p.substring(0, p.length() - 1);
        }
        int idx = p.lastIndexOf(separator);
        if (idx == -1) {
            return p;
        }
        return p.substring(idx + 1);
    }

    public OutputStream getOutputStream() throws FileNotFoundException {
        checkWritable();
        try {
            return getIFile().getOutputStream();
        } catch (Exception e) {
            debug(e);
        }
        return null;
    }

    /**
	 * Returns the pathname string of this abstract pathname's parent, or null
	 * if this pathname does not name a parent directory. The parent of an
	 * abstract pathname consists of the pathname's prefix, if any, and each
	 * name in the pathname's name sequence except for the last. If the name
	 * sequence is empty then the pathname does not name a parent directory.
	 * Returns: The pathname string of the parent directory named by this
	 * abstract pathname, or null if this pathname does not name a parent
	 */
    public String getParent() {
        String p = getPath();
        if (p == null || p.equals("") || p.equals("/") || p.equals("v:/") || p.equals("v:")) {
            return null;
        }
        if (p.endsWith(separator)) {
            p = p.substring(0, p.length() - 1);
        }
        int idx = p.lastIndexOf(separator);
        if (idx == -1) {
            return null;
        }
        if (idx == 2 || idx == 0) {
            return "v:/";
        }
        return p.substring(0, idx);
    }

    /**
	 * Returns the abstract pathname of this abstract pathname's parent, or null
	 * if this pathname does not name a parent directory. The parent of an
	 * abstract pathname consists of the pathname's prefix, if any, and each
	 * name in the pathname's name sequence except for the last. If the name
	 * sequence is empty then the pathname does not name a parent directory.
	 * Returns: The abstract pathname of the parent directory named by this
	 * abstract pathname, or null if this pathname does not name a parent
	 */
    public java.io.File getParentFile() {
        String parent = this.getParent();
        if (parent == null) {
            return null;
        }
        return new File(parent);
    }

    public String getPath() {
        return uri;
    }

    public int hashCode() {
        return uri.hashCode();
    }

    public boolean isAbsolute() {
        return isAbsolutePath(uri);
    }

    /**
	 * Tests whether the file denoted by this abstract pathname is a directory.
	 * Returns: true if and only if the file denoted by this abstract pathname
	 * exists and is a directory; false otherwise Throws: SecurityException - If
	 * a security manager exists and its
	 * SecurityManager.checkRead(java.lang.String) method denies read access to
	 * the file
	 */
    public boolean isDirectory() {
        try {
            return (fs.canRead(this) && (getIFile().isDirectory()));
        } catch (Exception e) {
            debug(e);
        }
        return false;
    }

    /**
	 * Tests whether the file denoted by this abstract pathname is a normal
	 * file. A file is normal if it is not a directory and, in addition,
	 * satisfies other system-dependent criteria. Any non-directory file created
	 * by a Java application is guaranteed to be a normal file. Returns: true if
	 * and only if the file denoted by this abstract pathname exists and is a
	 * normal file; false otherwise Throws: SecurityException - If a security
	 * manager exists and its SecurityManager.checkRead(java.lang.String) method
	 * denies read access to the file
	 */
    public boolean isFile() {
        try {
            return (fs.canRead(this) && getIFile().isFile());
        } catch (Exception e) {
            debug(e);
        }
        return false;
    }

    /**
	 * Tests whether the file named by this abstract pathname is a hidden file.
	 * The exact definition of hidden is system-dependent. On UNIX systems, a
	 * file is considered to be hidden if its name begins with a period
	 * character ('.'). We use UNIX convention. Returns: true if and only if the
	 * file denoted by this abstract pathname is hidden Throws:
	 * SecurityException - If a security manager exists and its
	 * SecurityManager.checkRead(java.lang.String) method denies read access to
	 * the file
	 */
    public boolean isHidden() {
        String name = getName();
        if (name != null && !name.equals("") && name.charAt(0) == '.') {
            return true;
        }
        return false;
    }

    /**
	 * Returns the time that the file denoted by this abstract pathname was last
	 * modified. Returns: A long value representing the time the file was last
	 * modified, measured in milliseconds since the epoch (00:00:00 GMT, January
	 * 1, 1970), or 0L if the file does not exist or if an I/O error occurs
	 * Throws: SecurityException - If a security manager exists and its
	 * SecurityManager.checkRead(java.lang.String) method denies read access to
	 * the file
	 */
    public long lastModified() {
        try {
            if (!fs.canRead(this)) {
                return 0;
            }
            return getIFile().getLastModified();
        } catch (Exception e) {
            debug(e);
        }
        return 0;
    }

    /**
	 * public long length()Returns the length of the file denoted by this
	 * abstract pathname. The return value is unspecified if this pathname
	 * denotes a directory. Returns: The length, in bytes, of the file denoted
	 * by this abstract pathname, or 0L if the file does not exist Throws:
	 * SecurityException - If a security manager exists and its
	 * SecurityManager.checkRead(java.lang.String) method denies read access to
	 * the file
	 */
    public long length() {
        try {
            if (!fs.canRead(this)) {
                return 0;
            }
            long l = Math.max(0, getIFile().getLength());
            return l;
        } catch (Exception e) {
        }
        return 0;
    }

    /**
	 * Returns an array of strings naming the files and directories in the
	 * directory denoted by this abstract pathname. If this abstract pathname
	 * does not denote a directory, then this method returns null. Otherwise an
	 * array of strings is returned, one for each file or directory in the
	 * directory. Names denoting the directory itself and the directory's parent
	 * directory are not included in the result. Each string is a file name
	 * rather than a complete path. There is no guarantee that the name strings
	 * in the resulting array will appear in any specific order; they are not,
	 * in particular, guaranteed to appear in alphabetical order. Returns: An
	 * array of strings naming the files and directories in the directory
	 * denoted by this abstract pathname. The array will be empty if the
	 * directory is empty. Returns null if this abstract pathname does not
	 * denote a directory, or if an I/O error occurs. Throws: SecurityException -
	 * If a security manager exists and its
	 * SecurityManager.checkRead(java.lang.String) method denies read access to
	 * the directory
	 */
    public String[] list() {
        if (!fs.canRead(this)) {
            return null;
        }
        try {
            return getIFile().list();
        } catch (Exception e) {
            debug(e);
        }
        return null;
    }

    public String[] list(FilenameFilter filter) {
        String names[] = list();
        if ((names == null) || (filter == null)) {
            return null;
        }
        ArrayList v = new ArrayList();
        for (int i = 0; i < names.length; i++) {
            if (filter.accept(this, names[i])) {
                v.add(names[i]);
            }
        }
        return (String[]) (v.toArray(new String[0]));
    }

    @Override
    public File[] listFiles() {
        String[] ss = list();
        if (ss == null) {
            return null;
        }
        int n = ss.length;
        File[] fs = new File[n];
        for (int i = 0; i < n; i++) {
            fs[i] = new File(this, ss[i]);
        }
        return fs;
    }

    @Override
    public File[] listFiles(FileFilter filter) {
        String ss[] = list();
        if (ss == null) {
            return null;
        }
        ArrayList v = new ArrayList();
        for (int i = 0; i < ss.length; i++) {
            File f = new File(this, ss[i]);
            if ((filter == null) || filter.accept(f)) {
                v.add(f);
            }
        }
        return (File[]) (v.toArray(new File[0]));
    }

    @Override
    public File[] listFiles(FilenameFilter filter) {
        String ss[] = list();
        if (ss == null) {
            return null;
        }
        ArrayList v = new ArrayList();
        for (int i = 0; i < ss.length; i++) {
            if ((filter == null) || filter.accept(this, ss[i])) {
                v.add(new File(this, ss[i]));
            }
        }
        return (File[]) (v.toArray(new File[0]));
    }

    public boolean mkdir() {
        return (fs.canWrite(this) && getIFile().mkdir());
    }

    public boolean mkdirs() {
        if (this.exists()) {
            return false;
        }
        if (this.mkdir()) {
            return true;
        }
        String parent = this.getParent();
        return (parent != null) && (new File(parent).mkdirs() && this.mkdir());
    }

    /**
	 * Renames the file denoted by this abstract pathname. Whether or not this
	 * method can move a file from one filesystem to another is
	 * platform-dependent. The return value should always be checked to make
	 * sure that the rename operation was successful. Parameters: dest - The new
	 * abstract pathname for the named file Returns: true if and only if the
	 * renaming succeeded; false otherwise Throws: SecurityException - If a
	 * security manager exists and its
	 * SecurityManager.checkWrite(java.lang.String) method denies write access
	 * to either the old or new pathnames NullPointerException - If parameter
	 * dest is null
	 */
    public boolean renameTo(java.io.File dest) {
        try {
            if (!fs.canRead(this) || !fs.canWrite((File) dest)) {
                return false;
            }
            boolean rc = getIFile().move(fs.resolve((File) dest));
            if (rc == false) {
                debug("rename failed: " + uri + " -> " + dest);
            }
            return rc;
        } catch (Exception e) {
            debug(e + " " + uri + " -> " + dest);
        }
        return false;
    }

    public java.io.File replicate() throws IOException {
        return createLocalFile(this, true);
    }

    public boolean setAttribute(String name, Object value) {
        return false;
    }

    public boolean setAttributes(Map prop) {
        return false;
    }

    public boolean setContent(String s) throws FileNotFoundException {
        checkWritable();
        return (getIFile().setContent(s));
    }

    /**
	 * Set content of this file from in
	 * 
	 * @param in
	 * @return
	 * @throws FileNotFoundException
	 */
    public boolean setInputStream(InputStream in) throws FileNotFoundException {
        checkWritable();
        try {
            return (getIFile().setInputStream(in));
        } catch (Exception e) {
            debug(e);
        }
        return false;
    }

    public boolean setLastModified(long time) {
        try {
            return (fs.canWrite(this) && getIFile().setLastModified(time));
        } catch (Exception e) {
            debug(e);
        }
        return false;
    }

    /**
	 * Write content of this file to out
	 * 
	 * @param out
	 * @return
	 * @throws FileNotFoundException
	 */
    public boolean setOutputStream(OutputStream out) throws FileNotFoundException {
        checkReadable();
        try {
            return (getIFile().setOutputStream(out));
        } catch (Exception e) {
            debug(e);
        }
        return false;
    }

    public boolean setReadOnly() {
        return false;
    }

    @Override
    public boolean canExecute() {
        return false;
    }

    @Override
    public long getFreeSpace() {
        return 0L;
    }

    @Override
    public long getTotalSpace() {
        return 0L;
    }

    @Override
    public long getUsableSpace() {
        return 0L;
    }

    @Override
    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return false;
    }

    @Override
    public boolean setExecutable(boolean executable) {
        return false;
    }

    @Override
    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return false;
    }

    @Override
    public boolean setReadable(boolean readable) {
        return false;
    }

    @Override
    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return false;
    }

    @Override
    public boolean setWritable(boolean writable) {
        return false;
    }

    public URI toRealURI() {
        try {
            return fs.resolve(this);
        } catch (Exception e) {
            debug(e);
        }
        return null;
    }

    public String toString() {
        return uri;
    }

    public URI toURI() {
        try {
            return new URI(slashend(uri, isDirectory()));
        } catch (URISyntaxException e) {
        }
        return null;
    }

    public URL toURL() throws MalformedURLException {
        return new URL(slashend(uri, isDirectory()));
    }
}
