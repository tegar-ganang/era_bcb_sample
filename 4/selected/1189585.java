package org.netbeans.core.startup.layers;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import org.openide.ErrorManager;
import org.openide.filesystems.*;
import org.openide.util.Lookup;
import org.openide.util.SharedClassObject;
import org.openide.util.actions.SystemAction;
import org.openide.util.io.NbObjectInputStream;

/**
 * Read-only and fixed FileSystem over a binary file.
 * Its content can't be changed in any way, it can only be replaced by a new
 * instance over a different binary file.
 *
 * @author Petr Nejedly
 */
public class BinaryFS extends FileSystem {

    static final byte[] MAGIC = "org.netbeans.core.projects.cache.BinaryV3".getBytes();

    /** An empty array of SystemActions. */
    static final SystemAction[] NO_ACTIONS = new SystemAction[0];

    /** An empty array of FileObjects. */
    static final FileObject[] NO_CHILDREN = new FileObject[0];

    String binaryFile;

    ByteBuffer content;

    private FileObject root;

    /** list of urls (String) or Date of their modifications */
    private java.util.List modifications;

    private Date lastModified = new Date();

    /** Creates a new instance of BinaryFS */
    public BinaryFS(String binaryFile) throws IOException {
        this.binaryFile = binaryFile;
        RandomAccessFile file = new RandomAccessFile(binaryFile, "r");
        long len = file.length();
        MappedByteBuffer buff = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, len);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        file.close();
        byte[] magic = new byte[MAGIC.length];
        buff.get(magic);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new IOException("Bad magic header: " + new String(magic));
        }
        long storedLen = buff.getInt();
        if (len != storedLen) {
            throw new IOException("Corrupted image, correct length=" + storedLen);
        }
        int stop = buff.getInt() + 8 + MAGIC.length;
        modifications = new ArrayList();
        while (buff.position() < stop) {
            modifications.add(getString(buff));
        }
        content = buff.slice().order(ByteOrder.LITTLE_ENDIAN);
        root = new BFSFolder("", null, 0);
    }

    /** Finds a file given its full resource path.
     * @param name the resource path, e.g. "dir/subdir/file.ext" or "dir/subdir" or "dir"
     * @return a file object with the given path or
     *   <CODE>null</CODE> if no such file exists
     *
     */
    public FileObject findResource(String name) {
        if (name.length() == 0) {
            return root;
        }
        StringTokenizer st = new StringTokenizer(name, "/");
        FileObject fo = root;
        while (fo != null && st.hasMoreTokens()) {
            String next = st.nextToken();
            fo = fo.getFileObject(next, null);
        }
        return fo;
    }

    /** Returns an array of actions that can be invoked on any file in
     * this filesystem.
     * These actions should preferably
     * support the {@link org.openide.util.actions.Presenter.Menu Menu},
     * {@link org.openide.util.actions.Presenter.Popup Popup},
     * and {@link org.openide.util.actions.Presenter.Toolbar Toolbar} presenters.
     *
     * @return array of available actions
     *
     */
    public SystemAction[] getActions() {
        return NO_ACTIONS;
    }

    /** Provides a name for the system that can be presented to the user.
     * <P>
     * This call should <STRONG>never</STRONG> be used to attempt to identify the file root
     * of the filesystem. On some systems it may happen to look the same but this is a
     * coincidence and may well change in the future. Either check whether
     * you are working with a {@link LocalFileSystem} or similar implementation and use
     * {@link LocalFileSystem#getRootDirectory}; or better, try
     * {@link FileUtil#toFile} which is designed to do this correctly.
     *
     * @return user presentable name of the filesystem
     *
     */
    public String getDisplayName() {
        return "BinaryFS[" + binaryFile + "]";
    }

    /** Getter for root folder in the filesystem.
     *
     * @return root folder of whole filesystem
     *
     */
    public FileObject getRoot() {
        return root;
    }

    /** Test if the filesystem is read-only or not.
     * @return true if the system is read-only
     *
     */
    public boolean isReadOnly() {
        return true;
    }

    static String getString(ByteBuffer buffer) throws IOException {
        int len = buffer.getInt();
        byte[] arr = new byte[len];
        buffer.get(arr);
        return new String(arr, "UTF-8");
    }

    /** Base class for read-only FileObject, both file and folder
     * Handles common operations and throws exception for any modification
     * attempt.
     * Also handles attribute reading/parsing.
     * The layout in the bfs file is (beggining from the offset):
     *  <pre>
     *   int attrCount
     *   Attribute[attrCount] attrs
     *   int fileCount/filesize
     *   content
     * </pre>
     */
    private abstract class BFSBase extends FileObject {

        private final FileObject parent;

        protected final String name;

        protected final int offset;

        private boolean initialized = false;

        private Map attrs = Collections.EMPTY_MAP;

        public BFSBase(String name, FileObject parent, int offset) {
            this.name = name;
            this.parent = parent;
            this.offset = offset;
        }

        public final boolean equals(Object o) {
            if (!(o instanceof BFSBase)) return false;
            if (o == this) return true;
            BFSBase f = (BFSBase) o;
            return f.getPath().equals(getPath()) && specificEquals(f) && attributeEquals(f);
        }

        private final boolean attributeEquals(BFSBase base) {
            initialize();
            base.initialize();
            return attrs.equals(base.attrs);
        }

        public final int hashCode() {
            return getPath().hashCode();
        }

        protected abstract boolean specificEquals(BFSBase f);

        /** no-op implementations of read-only, fixed, never firing FS */
        public void addFileChangeListener(FileChangeListener fcl) {
        }

        public void removeFileChangeListener(FileChangeListener fcl) {
        }

        public boolean isReadOnly() {
            return true;
        }

        public void setImportant(boolean b) {
        }

        public FileObject createData(String name, String ext) throws IOException {
            throw new IOException();
        }

        public FileObject createFolder(String name) throws IOException {
            throw new IOException();
        }

        public void delete(FileLock lock) throws IOException {
            throw new IOException();
        }

        public OutputStream getOutputStream(FileLock lock) throws java.io.IOException {
            throw new IOException();
        }

        public FileLock lock() throws IOException {
            throw new IOException();
        }

        public void rename(FileLock lock, String name, String ext) throws IOException {
            throw new IOException();
        }

        public void setAttribute(String attrName, Object value) throws IOException {
            throw new IOException();
        }

        /** Get the filesystem containing this file. */
        public FileSystem getFileSystem() {
            return BinaryFS.this;
        }

        /** Get the parent folder. */
        public FileObject getParent() {
            return parent;
        }

        /** Test whether this object is the root folder. */
        public boolean isRoot() {
            return parent == null;
        }

        public boolean isValid() {
            return true;
        }

        /** Get last modification time. */
        public java.util.Date lastModified() {
            return lastModified;
        }

        /** Get the name without extension of this file or folder. */
        public String getName() {
            int i = name.lastIndexOf('.');
            return i <= 0 ? name : name.substring(0, i);
        }

        /** Get the extension of this file or folder. */
        public String getExt() {
            int i = name.lastIndexOf('.') + 1;
            return i <= 1 || i == name.length() ? "" : name.substring(i);
        }

        /** Getter for name and extension of a file object. */
        public String getNameExt() {
            return name;
        }

        /** Get the file attribute with the specified name. */
        public Object getAttribute(String attrName) {
            initialize();
            AttrImpl attr = (AttrImpl) attrs.get(attrName);
            if (attr != null) {
                FileObject topFO = null;
                try {
                    Class mfoClass = Class.forName("org.openide.filesystems.MultiFileObject");
                    Field field = mfoClass.getDeclaredField("attrAskedFileObject");
                    field.setAccessible(true);
                    ThreadLocal attrAskedFileObject = (ThreadLocal) field.get(null);
                    topFO = (FileObject) attrAskedFileObject.get();
                    attrAskedFileObject.set(null);
                } catch (Exception e) {
                    ErrorManager.getDefault().notify(e);
                }
                return attr.getValue(topFO == null ? this : topFO, attrName);
            } else {
                return null;
            }
        }

        /** Get all file attribute names for this file. */
        public Enumeration getAttributes() {
            initialize();
            if (attrs == null) return org.openide.util.Enumerations.empty();
            return Collections.enumeration(attrs.keySet());
        }

        /**
         * A method for fetching/parsing the content of the file/folder
         * to the memory. Should be called before any access operation
         * to check the content of the entity.
         */
        protected final void initialize() {
            if (initialized) return;
            try {
                ByteBuffer sub = (ByteBuffer) content.duplicate().order(ByteOrder.LITTLE_ENDIAN).position(offset);
                int attrCount = sub.getInt();
                if (attrCount > 0) attrs = new HashMap(attrCount * 4 / 3 + 1);
                for (int i = 0; i < attrCount; i++) {
                    String name = getString(sub).intern();
                    byte type = sub.get();
                    String value = getString(sub).intern();
                    attrs.put(name, new AttrImpl(type, value));
                }
                doInitialize(sub);
            } catch (Exception e) {
                System.err.println("exception in initialize() on " + name + ": " + e);
            }
            initialized = true;
        }

        /** A method called to finish the initialization in the subclasses.
         * When this method is called, the contentOffset field is already set up.
         */
        protected abstract void doInitialize(ByteBuffer sub) throws Exception;
    }

    static final class AttrImpl {

        private int index;

        private String value;

        AttrImpl(int type, String textValue) {
            index = type;
            value = textValue;
        }

        public boolean equals(Object o) {
            if (o instanceof AttrImpl) {
                AttrImpl impl = (AttrImpl) o;
                return index == impl.index && value.equals(impl.value);
            }
            return false;
        }

        public int hashCode() {
            return 2343 + index + value.hashCode();
        }

        public Object getValue(FileObject fo, String attrName) {
            try {
                switch(index) {
                    case 0:
                        return Byte.valueOf(value);
                    case 1:
                        return Short.valueOf(value);
                    case 2:
                        return Integer.valueOf(value);
                    case 3:
                        return Long.valueOf(value);
                    case 4:
                        return Float.valueOf(value);
                    case 5:
                        return Double.valueOf(value);
                    case 6:
                        return Boolean.valueOf(value);
                    case 7:
                        if (value.trim().length() != 1) break;
                        return new Character(value.charAt(0));
                    case 8:
                        return value;
                    case 9:
                        return new URL(value);
                    case 10:
                        return methodValue(value, fo, attrName);
                    case 11:
                        Class cls = findClass(value);
                        if (SharedClassObject.class.isAssignableFrom(cls)) {
                            return SharedClassObject.findObject(cls, true);
                        } else {
                            return cls.newInstance();
                        }
                    case 12:
                        return decodeValue(value);
                    default:
                        throw new IllegalStateException("Bad index: " + index);
                }
            } catch (Exception exc) {
                ErrorManager em = ErrorManager.getDefault();
                em.annotate(exc, "value = " + value);
                em.notify(ErrorManager.INFORMATIONAL, exc);
            }
            return null;
        }

        /** Constructs new attribute as Object. Used for dynamic creation: methodvalue. */
        private Object methodValue(String method, FileObject fo, String attr) throws Exception {
            String className, methodName;
            int i = method.lastIndexOf('.');
            if (i != -1) {
                methodName = value.substring(i + 1);
                className = value.substring(0, i);
                Class cls = findClass(className);
                Object objArray[][] = { null, null, null };
                Method methArray[] = { null, null, null };
                Class fParam = fo.getClass(), sParam = attr.getClass();
                Method[] allMethods = cls.getDeclaredMethods();
                Class[] paramClss;
                for (int j = 0; j < allMethods.length; j++) {
                    if (!allMethods[j].getName().equals(methodName)) continue;
                    paramClss = allMethods[j].getParameterTypes();
                    if (paramClss.length == 0) {
                        if (methArray[0] == null) {
                            methArray[0] = allMethods[j];
                            objArray[0] = new Object[] {};
                            continue;
                        }
                        continue;
                    }
                    if (paramClss.length == 2 && methArray[2] == null) {
                        if (paramClss[0].isAssignableFrom(fParam) && paramClss[1].isAssignableFrom(sParam)) {
                            methArray[2] = allMethods[j];
                            objArray[2] = new Object[] { fo, attr };
                            break;
                        }
                        if (paramClss[0].isAssignableFrom(sParam) && paramClss[1].isAssignableFrom(fParam)) {
                            methArray[2] = allMethods[j];
                            objArray[2] = new Object[] { attr, fo };
                            break;
                        }
                        continue;
                    }
                    if (paramClss.length == 1 && methArray[1] == null) {
                        if (paramClss[0].isAssignableFrom(fParam)) {
                            methArray[1] = allMethods[j];
                            objArray[1] = new Object[] { fo };
                            continue;
                        }
                        if (paramClss[0].isAssignableFrom(sParam)) {
                            methArray[1] = allMethods[j];
                            objArray[1] = new Object[] { attr };
                            continue;
                        }
                        continue;
                    }
                }
                for (int k = 2; k >= 0; k--) {
                    if (methArray[k] != null) {
                        methArray[k].setAccessible(true);
                        return methArray[k].invoke(null, objArray[k]);
                    }
                }
            }
            throw new InstantiationException(value);
        }

        private Object decodeValue(String value) throws Exception {
            if ((value == null) || (value.length() == 0)) return null;
            byte[] bytes = new byte[value.length() / 2];
            int count = 0;
            for (int i = 0; i < value.length(); i += 2) {
                int tempI = Integer.parseInt(value.substring(i, i + 2), 16);
                if (tempI > 127) tempI -= 256;
                bytes[count++] = (byte) tempI;
            }
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes, 0, count);
            return new NbObjectInputStream(bis).readObject();
        }

        /** Loads a class of given name
         * @param name name of the class
         * @return the class
         * @exception ClassNotFoundException if class was not found
         */
        private Class findClass(String name) throws ClassNotFoundException {
            ClassLoader c = (ClassLoader) Lookup.getDefault().lookup(ClassLoader.class);
            String tname = org.openide.util.Utilities.translate(name);
            try {
                if (c == null) {
                    return Class.forName(tname);
                } else {
                    return Class.forName(tname, true, c);
                }
            } catch (NoClassDefFoundError err) {
                throw new ClassNotFoundException("Cannot read " + name, err);
            }
        }
    }

    /** FileContent:
          int attrCount
          Attribute[attrCount] attrs
          int contentLength (or -1 for uri)
          byte[contentLength] content (or String uri)
     */
    final class BFSFile extends BFSBase {

        private int len;

        private int contentOffset;

        private String uri;

        private long lastModified = -1;

        public BFSFile(String name, FileObject parent, int offset) {
            super(name, parent, offset);
        }

        /** Get all children of this folder (files and subfolders). */
        public FileObject[] getChildren() {
            return NO_CHILDREN;
        }

        /** Retrieve file or folder contained in this folder by name. */
        public FileObject getFileObject(String name, String ext) {
            return null;
        }

        /** Test whether this object is a data object. */
        public boolean isData() {
            return true;
        }

        /** Test whether this object is a folder. */
        public boolean isFolder() {
            return false;
        }

        private byte[] data() throws IOException {
            if (len == -1) throw new IllegalArgumentException();
            byte[] data = new byte[len];
            ((ByteBuffer) content.duplicate().position(contentOffset)).get(data);
            return data;
        }

        /** Get input stream. */
        public InputStream getInputStream() throws java.io.FileNotFoundException {
            initialize();
            try {
                return len == -1 ? new URL(uri).openConnection().getInputStream() : new ByteArrayInputStream(data());
            } catch (Exception e) {
                FileNotFoundException x = new FileNotFoundException(e.getMessage());
                ErrorManager.getDefault().annotate(x, e);
                throw x;
            }
        }

        /** Get the size of the file. */
        public long getSize() {
            initialize();
            try {
                return len == -1 ? new URL(uri).openConnection().getContentLength() : len;
            } catch (Exception e) {
                System.err.println("exception in getSize() on " + name + ": " + e);
                return 0;
            }
        }

        /** A method called to finish the initialization in the subclasses.
         * When this method is called, the contentOffset field is already set up.
         *
         * the rest of the FileContent:
         * int contentLength (or -1)
         * byte[contentLength] content (or String uri);
         */
        protected void doInitialize(ByteBuffer sub) throws Exception {
            len = sub.getInt();
            ;
            contentOffset = sub.position();
            if (len == -1) {
                uri = getString(sub);
            } else {
                sub.position(contentOffset + len);
            }
            int base = sub.getInt();
            lastModified = -10 - base;
        }

        protected boolean specificEquals(BFSBase _f) {
            if (!(_f instanceof BFSFile)) return false;
            BFSFile f = (BFSFile) _f;
            initialize();
            f.initialize();
            if (len == -1 && f.len == -1) {
                return uri.equals(f.uri);
            } else if (len != -1 && f.len != -1) {
                byte[] data, fdata;
                try {
                    data = data();
                    fdata = f.data();
                } catch (IOException ioe) {
                    return false;
                }
                if (data.length != fdata.length) return false;
                for (int i = 0; i < data.length; i++) {
                    if (data[i] != fdata[i]) return false;
                }
                return true;
            } else {
                return false;
            }
        }

        public java.util.Date lastModified() {
            initialize();
            synchronized (modifications) {
                if (lastModified >= 0) {
                    return new java.util.Date(lastModified);
                }
                try {
                    int index = -1;
                    java.net.URLConnection conn = null;
                    if (len == -1) {
                        conn = new URL(uri).openConnection();
                    } else {
                        index = ((int) -lastModified) - 10;
                        Object obj = modifications.get(index);
                        if (obj instanceof Date) {
                            return (Date) obj;
                        }
                        conn = new URL((String) obj).openConnection();
                    }
                    if (conn instanceof java.net.JarURLConnection) {
                        conn = ((java.net.JarURLConnection) conn).getJarFileURL().openConnection();
                    }
                    if (conn != null) {
                        long date = conn.getLastModified();
                        if (date > 0) {
                            lastModified = date;
                            Date d = new java.util.Date(date);
                            if (index >= 0) {
                                modifications.set(index, d);
                            }
                            return d;
                        }
                    }
                } catch (Exception e) {
                    ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
                }
            }
            return super.lastModified();
        }
    }

    private class BFSFolder extends BFSBase {

        Map childrenMap = Collections.EMPTY_MAP;

        public BFSFolder(String name, FileObject parent, int offset) {
            super(name, parent, offset);
        }

        /** Test whether this object is a data object. */
        public boolean isData() {
            return false;
        }

        /** Test whether this object is a folder. */
        public boolean isFolder() {
            return true;
        }

        /** Get the size of the file. */
        public long getSize() {
            return 0;
        }

        /** Get input stream. */
        public InputStream getInputStream() throws java.io.FileNotFoundException {
            throw new FileNotFoundException("Is a directory: " + this);
        }

        /** Get all children of this folder (files and subfolders). */
        public FileObject[] getChildren() {
            initialize();
            return (FileObject[]) childrenMap.values().toArray(NO_CHILDREN);
        }

        /** Retrieve file or folder contained in this folder by name. */
        public FileObject getFileObject(String name, String ext) {
            initialize();
            String fullName = ext == null ? name : name + "." + ext;
            return (FileObject) childrenMap.get(fullName);
        }

        /** A method called to finish the initialization in the subclasses.
         * When this method is called, the contentOffset field is already set up.
         *
         * the rest of the FolderContent:
         *   int fileCount
         * File[fileCount] references    
         * File/FolderContent[fileCount] contents
         */
        protected void doInitialize(ByteBuffer sub) throws Exception {
            int files = sub.getInt();
            if (files > 0) {
                childrenMap = new HashMap(files * 4 / 3 + 1);
                for (int i = 0; i < files; i++) {
                    String name = getString(sub);
                    byte isFolder = sub.get();
                    int off = sub.getInt();
                    childrenMap.put(name, isFolder == 0 ? (Object) new BFSFile(name, this, off) : new BFSFolder(name, this, off));
                }
            }
        }

        protected boolean specificEquals(BFSBase _f) {
            if (!(_f instanceof BFSFolder)) return false;
            BFSFolder f = (BFSFolder) _f;
            initialize();
            return childrenMap.equals(f.childrenMap);
        }
    }
}
