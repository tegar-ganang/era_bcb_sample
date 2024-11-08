package jaxlib.prefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLockInterruptionException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import jaxlib.lang.Objects;
import jaxlib.io.IO;
import jaxlib.io.channel.FileChannels;
import jaxlib.io.file.FileLockTimeoutException;
import jaxlib.io.file.Files;
import jaxlib.io.file.LockFile;
import jaxlib.util.CheckArg;
import jaxlib.util.Strings;

/**
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: FileSystemPreferences.java 1405 2005-06-08 22:44:38Z joerg_wassmer $
 */
public final class FileSystemPreferences extends AbstractXPreferences {

    private static final long nanoTimeZero = System.nanoTime();

    private static long milliToNanoTime(long millis) {
        return TimeUnit.MILLISECONDS.toNanos(millis) - nanoTimeZero;
    }

    private static long nanoToMilliTime(long nanos) {
        return TimeUnit.NANOSECONDS.toMillis(nanos + nanoTimeZero);
    }

    private static long systemNanoTime() {
        return System.nanoTime() - nanoTimeZero;
    }

    /**
   * Translate a string into a byte array by translating each character
   * into two bytes, high-byte first ("big-endian").
   */
    private static byte[] byteArray(String s) {
        int len = s.length();
        byte[] result = new byte[2 * len];
        for (int i = 0, j = 0; i < len; i++) {
            char c = s.charAt(i);
            result[j++] = (byte) (c >> 8);
            result[j++] = (byte) c;
        }
        return result;
    }

    private static boolean isDirChar(char c) {
        return (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) || ((c >= '0') && (c <= '9')) || (c == '.') || (c == '_') || (c == '=') || (c == '+') || (c == '-') || (c == '$') || (c == '�') || (c == '#') || (c == '~') || (c == ',') || (c == '@') || (c == '!'));
    }

    /**
   * Returns the directory name corresponding to the specified node name.
   * Generally, this is just the node name.  If the node name includes
   * inappropriate characters (as per isDirChar) it is translated to Base64.
   * with the underscore  character ('_', 0x5f) prepended. 
   */
    private static String dirNameForNode(String nodeName) {
        final int len = nodeName.length();
        if (len == 0) {
            return nodeName;
        } else if (nodeName.equals(".data") || nodeName.endsWith(".temp") || nodeName.equals(".") || nodeName.equals("..") || (nodeName.charAt(0) == '_')) {
            return "_" + Base64.byteArrayToAltBase64(byteArray(nodeName));
        } else {
            for (int i = 0, n = nodeName.length(); i < n; i++) {
                if (!isDirChar(nodeName.charAt(i))) return "_" + Base64.byteArrayToAltBase64(byteArray(nodeName));
            }
            return nodeName;
        }
    }

    /**
   * Returns the node name corresponding to the specified directory name.
   * (Inverts the transformation of dirNameForNode(String).
   */
    private static String nodeNameForDir(String dirName) {
        if (dirName.charAt(0) != '_') return dirName;
        byte[] a = Base64.altBase64ToByteArray(dirName.substring(1));
        StringBuilder result = new StringBuilder(a.length >> 1);
        for (int i = 0; i < a.length; ) {
            int highByte = a[i++] & 0xff;
            int lowByte = a[i++] & 0xff;
            result.append((char) ((highByte << 8) | lowByte));
        }
        return result.toString();
    }

    final FileSystemPreferencesFactory factory;

    final File dir;

    final File file;

    long modTime = Long.MIN_VALUE;

    private PropertiesFile properties;

    FileSystemPreferences(FileSystemPreferences parent, File dir, String name, FileSystemPreferencesFactory factory) {
        super(parent, name, factory);
        if (parent == null) {
            if (dir == null) throw new IllegalArgumentException("parent and dir are both null");
            this.dir = dir;
        } else {
            if (dir != null) throw new IllegalArgumentException("parent and dir are both not null");
            this.dir = dir = new File(parent.dir, dirNameForNode(name));
        }
        this.factory = factory;
        this.file = new File(this.dir, factory.setup.nodeFileName + factory.setup.fileFormat.getFilenameSuffix());
        if (parent != null) {
            Logger logger = factory.getLogger();
            if ((logger != null) && logger.isLoggable(Level.CONFIG)) logger.config(toString());
        }
    }

    private PropertiesFile properties() throws BackingStoreException {
        assert Thread.holdsLock(this.lock);
        PropertiesFile properties = this.properties;
        if (properties == null) {
            try {
                properties = readFile();
            } catch (IOException ex) {
                this.properties = new PropertiesFile();
                throw new BackingStoreException(ex);
            }
            final Logger logger = this.factory.getLogger();
            if (properties == null) {
                if ((logger != null) && logger.isLoggable(Level.CONFIG)) logger.config("no persistent preferences found: " + this);
                properties = new PropertiesFile();
            } else {
                if ((logger != null) && logger.isLoggable(Level.CONFIG)) logger.config("first time read preferences: " + this);
            }
            this.properties = properties;
        }
        return properties;
    }

    private PropertiesFile readFile() throws IOException {
        if (this.file.isFile()) {
            FileInputStream in;
            try {
                in = Files.createLockedInputStream(this.file, this.factory.setup.fileLockTimeout, TimeUnit.MILLISECONDS);
            } catch (IOException ex) {
                if (this.file.isFile()) {
                    throw ex;
                } else {
                    return null;
                }
            }
            try {
                PropertiesFile properties = new PropertiesFile();
                properties.read(in, true);
                this.modTime = milliToNanoTime(this.file.lastModified());
                in.close();
                in = null;
                return properties;
            } finally {
                IO.tryClose(in);
            }
        } else {
            return null;
        }
    }

    private void writeFile() throws IOException {
        if (!this.dir.exists()) {
            Files.mkdirs(this.dir);
        }
        Logger logger = this.factory.getLogger();
        if ((logger != null) && logger.isLoggable(Level.CONFIG)) logger.config("writing preferences: " + this);
        long oldLastModified = this.file.lastModified();
        FileOutputStream out = Files.createLockedOutputStream(this.file, this.factory.setup.fileLockTimeout, TimeUnit.MILLISECONDS);
        try {
            this.properties.write(out, this.factory.setup.fileFormat, true);
            FileChannel outChannel = out.getChannel();
            outChannel.truncate(outChannel.size());
            out.close();
            long newLastModified = this.file.lastModified();
            if (newLastModified == oldLastModified) {
                newLastModified++;
                this.file.setLastModified(newLastModified);
            }
            out = null;
            this.modTime = milliToNanoTime(newLastModified);
        } finally {
            IO.tryClose(out);
        }
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        File[] dirContent = this.dir.listFiles();
        ArrayList<String> result = new ArrayList<String>(dirContent.length);
        for (File f : dirContent) {
            if (f.isDirectory()) result.add(f.getPath());
        }
        return result.toArray(new String[result.size()]);
    }

    @Override
    protected FileSystemPreferences childSpi(String name) {
        return new FileSystemPreferences(this, null, name, this.factory);
    }

    @Override
    public void clear() throws BackingStoreException {
        synchronized (this.lock) {
            if ((this.properties != null) && !this.properties.isEmpty()) {
                this.modTime = Math.max(this.modTime + 1, systemNanoTime());
                this.properties.clear();
            }
            if (!this.file.delete() && this.file.isFile()) throw new BackingStoreException("unable to delete file: " + this.file);
        }
    }

    @Override
    protected void flushSpi() throws BackingStoreException {
        if (!this.file.isFile() || (this.modTime != milliToNanoTime(this.file.lastModified()))) {
            final Logger logger = this.factory.getLogger();
            if ((logger != null) && logger.isLoggable(Level.CONFIG)) logger.config("flushing preferences: " + this);
            try {
                writeFile();
            } catch (IOException ex) {
                throw new BackingStoreException(ex);
            }
        }
    }

    public FileInputStream getBlobInputStream(String name) throws IOException, FileNotFoundException, FileLockInterruptionException, FileLockTimeoutException {
        return getBlobInputStream(name, this.factory.setup.fileLockTimeout);
    }

    public FileInputStream getBlobInputStream(String name, long timeout) throws IOException, FileNotFoundException, FileLockInterruptionException, FileLockTimeoutException {
        CheckArg.notEmpty(name, "name");
        CheckArg.notNegative(timeout, "timeout");
        File blobDir = new File(this.dir, ".data");
        File blobFile = new File(blobDir, dirNameForNode(name));
        synchronized (this.lock) {
            if (this.removed) throw new IllegalStateException("Node has been removed.");
        }
        if (!blobFile.isFile()) return null;
        try {
            return Files.createLockedInputStream(file, timeout, TimeUnit.MILLISECONDS);
        } catch (IOException ex) {
            synchronized (this.lock) {
                if (this.removed) throw new IllegalStateException("Node has been removed."); else if (!file.isFile()) return null; else throw ex;
            }
        }
    }

    @Override
    protected String getSpi(String key) throws BackingStoreException {
        if (this.factory.setup.syncInterval == 0) sync();
        return properties().get(key);
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
        PropertiesFile properties = properties();
        return properties.keySet().toArray(new String[properties.size()]);
    }

    @Override
    protected boolean putSpi(String key, String value) {
        PropertiesFile properties;
        try {
            properties = properties();
        } catch (BackingStoreException ex) {
            properties = this.properties;
            Logger logger = this.factory.getLogger();
            if (logger != null) logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        String oldValue = properties.put(key, value);
        boolean modified = (oldValue != value) || ((oldValue != null) && !oldValue.equals(value));
        if (modified) this.modTime = Math.max(this.modTime + 1, systemNanoTime());
        oldValue = null;
        if (this.factory.setup.syncInterval == 0) {
            try {
                syncSpi();
            } catch (BackingStoreException ex) {
                Logger logger = this.factory.getLogger();
                if (logger != null) logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return modified;
    }

    public void removeBlob(String name) throws IOException, FileLockInterruptionException, FileLockTimeoutException {
        removeBlob(name, this.factory.setup.fileLockTimeout, TimeUnit.MILLISECONDS);
    }

    public void removeBlob(String name, long timeout, TimeUnit timeunit) throws IOException, FileLockInterruptionException, FileLockTimeoutException {
        CheckArg.notEmpty(name, "name");
        File blobDir = new File(this.dir, ".data");
        File blobFile = new File(blobDir, dirNameForNode(name));
        synchronized (this.lock) {
            if (this.removed) throw new IllegalStateException("Node has been removed.");
        }
        if (blobFile.isFile() && !blobFile.delete() && blobFile.isFile()) {
            LockFile lockFile = new LockFile(blobFile);
            synchronized (this.lock) {
                if (this.removed) {
                    throw new IllegalStateException("Node has been removed.");
                } else {
                    timeout = Math.min(this.factory.setup.fileLockTimeout, timeout);
                    if (blobFile.isFile()) {
                        lockFile.lock(timeout, timeunit).deleteAndRelease();
                    }
                }
            }
        }
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        try {
            LockFile lockFile = isUserNode() ? this.factory.userRootLockFile : this.factory.systemRootLockFile;
            LockFile.Lock fileLock = null;
            try {
                try {
                    fileLock = lockFile.lockShared(this.factory.setup.fileLockTimeout, TimeUnit.MILLISECONDS);
                } catch (IOException ex) {
                    Logger logger = this.factory.getLogger();
                    if (logger != null) logger.log(Level.WARNING, "filelock timeout", ex);
                }
                if (!Files.delete(this.dir)) throw new BackingStoreException("unable to delete directory: " + this.dir);
            } finally {
                if (fileLock != null) fileLock.release();
            }
        } catch (IOException ex) {
            throw new BackingStoreException(ex);
        }
    }

    @Override
    protected String removeSpi(String key) throws BackingStoreException {
        String value = properties().remove(key);
        if (value != null) this.modTime = Math.max(this.modTime + 1, systemNanoTime());
        return value;
    }

    public void storeBlob(String name, File srcFile, boolean move) throws IOException, FileNotFoundException, FileLockInterruptionException, FileLockTimeoutException {
        storeBlob(name, srcFile, this.factory.setup.fileLockTimeout, move);
    }

    public void storeBlob(String name, File srcFile, long timeout, boolean move) throws IOException, FileNotFoundException, FileLockInterruptionException, FileLockTimeoutException {
        CheckArg.notEmpty(name, "name");
        CheckArg.notNull(srcFile, "srcFile");
        CheckArg.notNegative(timeout, "timeout");
        long maxTimeout = timeout;
        if ((timeout == 0) || (this.factory.setup.fileLockTimeout > 0)) maxTimeout = Math.min(timeout, this.factory.setup.fileLockTimeout);
        File blobDir = new File(this.dir, ".data");
        File blobFile = new File(blobDir, dirNameForNode(name));
        if (!srcFile.isFile()) throw new FileNotFoundException("source file not found: " + srcFile.toString());
        if (!move || !storeBlobTryRename(srcFile, blobDir, blobFile, maxTimeout)) {
            FileInputStream in = Files.createLockedInputStream(srcFile, timeout, TimeUnit.MILLISECONDS);
            File tempFile;
            try {
                tempFile = storeBlobCopyToTempFile(in, blobDir, maxTimeout);
                in.close();
                in = null;
                if (!storeBlobTryRename(tempFile, blobDir, blobFile, maxTimeout)) {
                    if (tempFile.isFile()) {
                        try {
                            tempFile.delete();
                        } finally {
                            throw new IOException("unable to rename file " + tempFile + " to " + blobFile);
                        }
                    } else {
                    }
                }
                if (move && !srcFile.delete() && srcFile.isFile()) throw new IOException("unable to delete source file");
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Throwable ex) {
                    }
                }
            }
        }
    }

    private File storeBlobCopyToTempFile(InputStream in, File blobDir, long timeout) throws IOException {
        if (in == null) throw new NullPointerException("in");
        File tempFile = Files.createTempFile(null, ".temp", blobDir);
        FileChannel out = null;
        try {
            synchronized (this.lock) {
                if (this.removed) {
                    throw new IllegalStateException("Node has been removed.");
                } else {
                    Files.mkdirs(blobDir);
                    out = Files.createLockedOutputStream(tempFile, timeout, TimeUnit.MILLISECONDS).getChannel();
                }
            }
            FileChannels.transferFrom(in, out, -1);
            out.close();
            out = null;
            return tempFile;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable ex) {
                }
                try {
                    tempFile.delete();
                } catch (Throwable ex) {
                }
                if (isRemoved()) throw new IllegalStateException("Node has been removed.");
            }
        }
    }

    private boolean storeBlobTryRename(File srcFile, File blobDir, File blobFile, long timeout) throws IOException {
        if (File.separatorChar == '\\') {
            String srcPath = srcFile.getPath();
            if ((srcPath.length() > 1) && (srcPath.charAt(1) == ':') && (srcPath.charAt(0) != blobFile.getPath().charAt(0))) {
                return false;
            }
        }
        LockFile lockFile = new LockFile(new File(this.dir, "_dataLock"));
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            } else {
                Files.mkdirs(blobDir);
                LockFile.Lock fileLock = null;
                try {
                    fileLock = lockFile.lock(timeout, TimeUnit.MILLISECONDS);
                    if (blobFile.isFile()) blobFile.delete();
                    boolean renamed = srcFile.renameTo(blobFile);
                    fileLock.deleteAndRelease();
                    fileLock = null;
                    return renamed;
                } finally {
                    if (fileLock != null) {
                        try {
                            fileLock.release();
                        } catch (Throwable ex) {
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void syncSpi() throws BackingStoreException {
        if (this.factory.setup.syncStrategy != FileSystemPreferences.SyncStrategy.READ_ONLY_ONCE) {
            try {
                if (this.properties != null) syncSpi0();
            } catch (IOException ex) {
                throw new BackingStoreException(ex);
            }
        }
    }

    private void syncSpi0() throws IOException {
        final boolean userNode = isUserNode();
        final LockFile lockFile = userNode ? this.factory.userRootLockFile : this.factory.systemRootLockFile;
        LockFile.Lock fileLock = null;
        try {
            try {
                fileLock = lockFile.lockShared(this.factory.setup.fileLockTimeout, TimeUnit.MILLISECONDS);
            } catch (IOException ex) {
                Logger logger = this.factory.getLogger();
                if (logger != null) logger.log(Level.WARNING, "filelock timeout", ex);
            }
            switch(this.factory.setup.syncStrategy) {
                case MERGE:
                    syncMerge(fileLock);
                    break;
                case OVERWRITE_ALWAYS:
                    syncOverwriteAlways(fileLock);
                    break;
                case SYNC_READ_ONLY:
                    syncReadOnly(fileLock);
                    break;
                default:
                    throw new AssertionError(this.factory.setup.syncStrategy);
            }
            if (fileLock != null) {
                fileLock.release();
                fileLock = null;
            }
        } finally {
            if (fileLock != null) {
                try {
                    fileLock.release();
                } catch (Throwable ex) {
                }
            }
        }
    }

    private void syncMerge(final LockFile.Lock fileLock) throws IOException {
        final long memoryTime = this.modTime;
        final long storageTime = milliToNanoTime(this.file.lastModified());
        PropertiesFile properties = this.properties;
        if (memoryTime != storageTime) {
            final Logger logger = this.factory.getLogger();
            if ((logger != null) && logger.isLoggable(Level.CONFIG)) {
                if (memoryTime > storageTime) logger.config("merging with older file: " + this); else logger.config("merging with newer file: " + this);
            }
            PropertiesFile newProperties = readFile();
            boolean write = false;
            if ((newProperties == null) || newProperties.isEmpty()) {
                write = !properties.isEmpty();
            } else {
                write = !newProperties.keySet().containsAll(properties.keySet());
                if (memoryTime > storageTime) {
                    for (PropertiesFile.Entry e : newProperties.propertySet()) {
                        String key = e.getKey();
                        String storageVal = e.getValue();
                        String memoryVal = properties.putIfAbsent(key, storageVal, e.getComment());
                        if (memoryVal == null) enqueuePreferenceChangeEvent(key, storageVal); else write = write || !Objects.equals(e.getComment(), properties.getComment(key));
                    }
                } else {
                    for (PropertiesFile.Entry e : newProperties.propertySet()) {
                        String key = e.getKey();
                        String val = e.getValue();
                        String comment = e.getComment();
                        if (comment == null) {
                            comment = properties.getComment(key);
                            if (comment != null) write = true;
                        }
                        if (!val.equals(properties.put(key, val, comment))) enqueuePreferenceChangeEvent(key, val);
                    }
                }
            }
            newProperties = null;
            if (write) {
                writeFile();
                if (fileLock != null) fileLock.getFile().setLastModified(nanoToMilliTime(this.modTime));
            }
        }
    }

    private void syncOverwriteAlways(final LockFile.Lock fileLock) throws IOException {
        if (this.modTime != milliToNanoTime(this.file.lastModified())) {
            final Logger logger = this.factory.getLogger();
            if ((logger != null) && logger.isLoggable(Level.CONFIG)) logger.config("writing modified preferences: " + this);
            writeFile();
            if (fileLock != null) fileLock.getFile().setLastModified(nanoToMilliTime(this.modTime));
        }
    }

    private void syncReadOnly(final LockFile.Lock fileLock) throws IOException {
        if (this.modTime != milliToNanoTime(this.file.lastModified())) {
            final PropertiesFile newProperties = readFile();
            if (newProperties != null) {
                final Logger logger = this.factory.getLogger();
                if ((logger != null) && logger.isLoggable(Level.CONFIG)) logger.config("read concurrently modified file: " + this);
                PropertiesFile properties = this.properties;
                for (PropertiesFile.Entry e : properties.propertySet()) {
                    String key = e.getKey();
                    if (!newProperties.containsKey(key)) enqueuePreferenceChangeEvent(key, null);
                }
                for (PropertiesFile.Entry e : newProperties.propertySet()) {
                    String key = e.getKey();
                    String val = e.getValue();
                    if (!val.equals(properties.put(key, val, e.getComment()))) enqueuePreferenceChangeEvent(key, val);
                }
            }
        }
    }

    public final File getDirectory() {
        return this.dir;
    }

    public final File getFile() {
        return this.file;
    }

    public final FileSystemPreferences getRoot() {
        return (FileSystemPreferences) this.root;
    }

    @Override
    public final boolean isSystemNode() {
        return this.root == this.factory.systemRoot;
    }

    @Override
    public final boolean isUserNode() {
        return this.root == this.factory.userRoot;
    }

    @Override
    public FileSystemPreferences node(String name) {
        return (FileSystemPreferences) super.node(name);
    }

    @Override
    public String toString() {
        return Strings.concat(isUserNode() ? "user" : "system", " node ", this.absolutePath, " of root ", getRoot().dir.toString());
    }

    public static enum SyncStrategy {

        /**
     * Merge persistent with in-memory preferences.
     * <ul>
     * <li>
     *   If the persistent preferences are newer then reread the preferences. Add all newly defined 
     *   preferences in memory to the persistent preferences. Replace the values of all preferences in memory 
     *   by those of the persistent storage. Finally write the merged preferences to the filesystem.
     * </li><li>
     *   If the persistent preferences have not been modified, but the preferences in memory have, then
     *   write all preferences to the filesystem.
     * </li>
     * </ul>
     *
     * <p>
     * This is the default strategy.
     * </p>
     * 
     * @since JaXLib 1.0
     */
        MERGE, /**
     * Always overwrite the persistent storage by the in-memory preferences when latter have been modified.
     * 
     * @since JaXLib 1.0
     */
        OVERWRITE_ALWAYS, /**
     * Just read the persistent storage the first time required and never reread or write anything.
     * 
     * @since JaXLib 1.0
     */
        READ_ONLY_ONCE, /**
     * Never write anything, always use the persistent storage.
     * If the persistent storage gets deleted then keep the already readed preferences in memory.
     * 
     * @since JaXLib 1.0
     */
        SYNC_READ_ONLY;

        /**
     * @since JaXLib 1.0
     */
        private static final long serialVersionUID = 1L;
    }
}
