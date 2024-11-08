package zipfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.ZipEntry;
import org.das2.util.filesystem.FileObject;
import org.das2.util.filesystem.FileSystemUtil;
import org.das2.util.monitor.ProgressMonitor;

/**
 *
 * @author Ed Jackson
 */
public class ZipFileObject extends FileObject {

    private ZipFileSystem zfs;

    private ZipEntry zipEntry;

    private ZipFileObject parent;

    private String name;

    private ArrayList<ZipFileObject> children;

    /** Create a new <code>ZipFileObject</code>.
     * 
     * @param zfs the containing <code>ZipFileSystem</code>
     * @param zipEntry the {@link ZipEntry} associated with this object
     * @param par the parent <code>ZipFileObject</code>.  Set to null if this is the root object.
     */
    protected ZipFileObject(ZipFileSystem zfs, ZipEntry zipEntry, ZipFileObject par) {
        this(zfs, zipEntry, par, null);
    }

    /** Create a new <code>ZipFileObject</code> with the specified name.  The name
     * should normally not be specified.  However, a folder which contains only a single
     * folder will not have a corresponding {@link ZipEntry}.  In that case, <code>zipEntry</code>
     * should be set to <code>null</code> and the <code>name</code> set to the
     * name of this folder, with no path information slashes.
     *
     * @param zfs the containing ZipFileSystem
     * @param zipEntry the <code>ZipEntry</code> associated with this object
     * @param par the parent ZipFileObject. Set to null if this is the root object.
     * @param name the file name.  If <code>zipEntry</code> is not
     *      <code>null</code>, this is ignored.
     */
    protected ZipFileObject(ZipFileSystem zfs, ZipEntry zipEntry, ZipFileObject par, String name) {
        this.zfs = zfs;
        this.zipEntry = zipEntry;
        this.parent = par;
        this.name = name;
        children = new ArrayList<ZipFileObject>();
    }

    protected void addChildObject(ZipFileObject child) {
        children.add(child);
    }

    @Override
    public boolean canRead() {
        return exists();
    }

    @Override
    public FileObject[] getChildren() throws IOException {
        return children.toArray(new ZipFileObject[0]);
    }

    @Override
    public InputStream getInputStream(ProgressMonitor monitor) throws IOException {
        if (!exists()) {
            throw new FileNotFoundException("file not found in zip: " + name);
        } else {
            return zfs.getZipFile().getInputStream(zipEntry);
        }
    }

    @Override
    public ReadableByteChannel getChannel(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        return ((FileInputStream) getInputStream(monitor)).getChannel();
    }

    @Override
    public synchronized File getFile(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        if (!exists()) throw new FileNotFoundException(String.format("file %s does not exist in %s", this.name, this.zfs.toString()));
        String tmpFileName = zfs.getLocalRoot().getAbsoluteFile() + "/" + zipEntry.getName();
        File tmpFile = new File(tmpFileName);
        File tmpDir = tmpFile.getParentFile();
        FileSystemUtil.maybeMkdirs(tmpDir);
        if (tmpFile.exists()) {
            if (tmpFile.lastModified() < new File(zfs.getZipFile().getName()).lastModified()) {
                if (!tmpFile.delete()) {
                    throw new IOException("unable to delete old unzipped file: " + tmpFile);
                }
            } else {
                return tmpFile;
            }
        }
        if (!tmpFile.createNewFile()) {
            throw new IllegalArgumentException("unable to create file " + tmpFile);
        }
        InputStream zStream = null;
        try {
            zStream = zfs.getZipFile().getInputStream(zipEntry);
            FileSystemUtil.dumpToFile(zStream, tmpFile);
        } finally {
            zStream.close();
        }
        return tmpFile;
    }

    @Override
    public FileObject getParent() {
        return parent;
    }

    @Override
    public long getSize() {
        if (zipEntry == null) return 0;
        return zipEntry.getSize();
    }

    @Override
    public boolean isData() {
        if (zipEntry == null) return false;
        return !(zipEntry.isDirectory());
    }

    @Override
    public boolean isFolder() {
        if (zipEntry == null) return true;
        return zipEntry.isDirectory();
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean isRoot() {
        return parent == null;
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public boolean exists() {
        return this.zipEntry != null && this.parent != null;
    }

    @Override
    public String getNameExt() {
        if (isRoot()) return "/";
        if (zipEntry != null) {
            return "/" + zipEntry.getName();
        } else {
            return parent.getNameExt() + name + "/";
        }
    }

    @Override
    public Date lastModified() {
        if (isRoot()) {
            File f = new File(zfs.getZipFile().getName());
            return new Date(f.lastModified());
        }
        long when = zipEntry.getTime();
        return (when > 0 ? new Date(when) : new Date(0));
    }
}
