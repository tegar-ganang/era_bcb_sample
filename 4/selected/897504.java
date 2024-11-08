package net.sf.zip.internal.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.sf.zip.internal.ZipPlugin;
import net.sf.zip.internal.model.tar.TarEntry;
import net.sf.zip.internal.model.tar.TarFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * @author allon moritz
 */
public class ArchiveEntryFile extends ArchiveFile implements IArchiveEntryFile {

    private final Object entry;

    /**
	 * @param file
	 * @param entry
	 */
    public ArchiveEntryFile(Object entry) {
        super(null);
        setPath(ArchiveEntry.getPath(entry));
        this.entry = entry;
    }

    public String create(File parent) throws CoreException {
        FileOutputStream out = null;
        InputStream in = null;
        try {
            String name = parent + File.separator + getPath().lastSegment();
            if (new File(name).exists()) return name;
            out = new FileOutputStream(name);
            in = getStream();
            byte[] buffer = new byte[4096];
            int read = 0;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return name;
        } catch (Exception e) {
            throw ZipPlugin.createException(e);
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
            }
        }
    }

    private InputStream getStream() throws Exception {
        ArchiveFile rootFile = getRootFile();
        if (rootFile.file instanceof ZipFile) return ((ZipFile) rootFile.file).getInputStream((ZipEntry) entry);
        if (rootFile.file instanceof TarFile) return ((TarFile) rootFile.file).getInputStream((TarEntry) entry);
        return null;
    }

    protected void read() {
        FileOutputStream out = null;
        InputStream in = null;
        try {
            String name = ZipPlugin.getRootFolder() + File.separator + getPath().lastSegment();
            out = new FileOutputStream(name);
            ArchiveFile f = ((Archive) getParent()).getArchiveFile();
            Object archiveFile = f.file;
            if (archiveFile instanceof ZipFile) {
                ZipFile zipFile = (ZipFile) archiveFile;
                in = zipFile.getInputStream((ZipEntry) entry);
            } else if (archiveFile instanceof TarFile) {
                TarFile tarFile = (TarFile) archiveFile;
                in = tarFile.getInputStream((TarEntry) entry);
            }
            byte[] buffer = new byte[4096];
            int read = 0;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            file = ZipPlugin.createArchive(name);
            super.read();
        } catch (Exception e) {
            ZipPlugin.logError(e);
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
            }
        }
    }

    public IArchive[] requestChilds(ArchiveContainer container) {
        IPath path = container.getPath();
        if (container == this) path = ROOT_PATH;
        return search(container, path);
    }

    protected ArchiveFile getRootFile() {
        IArchiveContainer parent = getParent();
        while (parent != null) {
            if (parent instanceof ArchiveFile) return (ArchiveFile) parent;
            parent = parent.getParent();
        }
        return null;
    }

    public void dispose() {
        super.dispose();
        File f = new File(ZipPlugin.getRootFolder() + File.separator + getPath().lastSegment());
        if (f.exists()) f.delete();
    }
}
