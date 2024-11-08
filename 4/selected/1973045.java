package net.sf.zip.internal.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import net.sf.zip.internal.Util;
import net.sf.zip.internal.ZipPlugin;
import net.sf.zip.internal.model.tar.TarEntry;
import net.sf.zip.internal.model.tar.TarException;
import net.sf.zip.internal.model.tar.TarFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

/**
 * @author allon moritz
 */
public class ArchiveFile extends ArchiveContainer {

    protected static final IPath ROOT_PATH = new Path("");

    protected Object file;

    private Set<IArchive> entries;

    /**
	 * @param file
	 */
    public ArchiveFile(Object file) {
        super(ROOT_PATH);
        this.file = file;
    }

    public IArchive[] getChildren() {
        if (entries == null) read();
        return super.getChildren();
    }

    protected void read() {
        Enumeration content = null;
        if (file instanceof ZipFile) content = ((ZipFile) file).entries();
        if (file instanceof TarFile) content = ((TarFile) file).entries();
        entries = new HashSet<IArchive>();
        Set<IPath> dir = new HashSet<IPath>();
        while (content.hasMoreElements()) {
            Object entry = content.nextElement();
            IPath p = getPath(entry);
            String[] segments = p.segments();
            for (int i = 0; i < segments.length - 1; i++) {
                IPath parentPath = p.removeLastSegments(segments.length - i - 1);
                if (!dir.contains(parentPath)) {
                    entries.add(new ArchiveContainer(parentPath));
                    dir.add(parentPath);
                }
            }
            Archive archive;
            if (isDirectory(entry)) {
                if (dir.contains(p)) continue;
                archive = new ArchiveContainer(p);
                dir.add(p);
            } else if (p.getFileExtension() != null && ZipPlugin.isSupportedType(p.getFileExtension())) archive = new ArchiveEntryFile(entry); else archive = new ArchiveEntry(entry, file);
            entries.add(archive);
        }
    }

    private IPath getPath(Object entry) {
        IPath path = null;
        if (entry instanceof ZipEntry) path = new Path(((ZipEntry) entry).getName());
        if (entry instanceof TarEntry) path = new Path(((TarEntry) entry).getName());
        return path;
    }

    private boolean isDirectory(Object entry) {
        if (entry instanceof ZipEntry) return ((ZipEntry) entry).isDirectory();
        if (entry instanceof TarEntry) return ((TarEntry) entry).getFileType() == TarEntry.DIRECTORY;
        return false;
    }

    public String toString() {
        if (file instanceof ZipFile) return ((ZipFile) file).getName();
        if (file instanceof TarFile) return ((TarFile) file).getName();
        return super.toString();
    }

    public IArchive[] requestChilds(ArchiveContainer container) {
        return search(container, container.getPath());
    }

    protected IArchive[] search(ArchiveContainer container, IPath path) {
        if (entries == null) read();
        List<IArchive> data = new LinkedList<IArchive>();
        for (Iterator it = entries.iterator(); it.hasNext(); ) {
            Archive archive = (Archive) it.next();
            if (archive.getPath().removeLastSegments(1).equals(path)) {
                data.add(archive);
                archive.setParent(container);
            }
        }
        entries.removeAll(data);
        return (IArchive[]) data.toArray(new IArchive[data.size()]);
    }

    @Override
    public ArchiveFile getArchiveFile() {
        return this;
    }

    @Override
    public void dispose() {
        try {
            if (file instanceof ZipFile) ((ZipFile) file).close();
            entries = null;
        } catch (IOException e) {
        }
        super.dispose();
    }

    public String getLabel(int type) {
        if (type == SIZE) {
            if (file instanceof ZipFile) return "" + new File(((ZipFile) file).getName()).length();
            if (file instanceof TarFile) return "" + new File(((TarFile) file).getName()).length();
        }
        return super.getLabel(type);
    }

    protected synchronized void addFiles(String[] fileNames, IArchiveContainer container, IProgressMonitor monitor) throws CoreException {
        monitor.beginTask("Adding files to the archive", 3000);
        File archiveDir = new File(ZipPlugin.getRootFolder(), new Path(toString()).lastSegment());
        try {
            monitor.subTask("Extracting to temporary dir");
            create(archiveDir);
            monitor.worked(10000);
            monitor.subTask("Copy files");
            File archivePath = new Path(archiveDir.getAbsolutePath()).append(container.getPath()).toFile();
            for (String fileName : fileNames) {
                File file = new File(fileName);
                Util.copy(file, new File(archivePath.getAbsolutePath(), file.getName()));
            }
            monitor.worked(1000);
            repackage(archiveDir);
            monitor.worked(1000);
        } catch (IOException e) {
            throw ZipPlugin.createException(e);
        } catch (TarException e) {
            throw ZipPlugin.createException(e);
        } finally {
            Util.delete(archiveDir);
            monitor.done();
        }
        if (file instanceof TarFile) {
        }
    }

    private void repackage(File archiveDir) throws IOException, TarException {
        if (file instanceof ZipFile) {
            ZipOutputStream out = null;
            ZipFile zip = (ZipFile) file;
            try {
                zip.close();
                out = new ZipOutputStream(new FileOutputStream(zip.getName()));
                write(archiveDir, out, archiveDir.getPath().length() + 1, zip);
            } finally {
                Util.close(out);
            }
            file = (ZipFile) ZipPlugin.createArchive(zip.getName());
            entries = null;
            super.dispose();
        }
        if (file instanceof TarFile) {
        }
    }

    private void write(File file, ZipOutputStream out, int pos, ZipFile zip) throws IOException {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            for (File f : entries) write(f, out, pos, zip);
        } else {
            byte[] buffer = new byte[4096];
            int bytesRead;
            FileInputStream in = null;
            try {
                in = new FileInputStream(file);
                ZipEntry entry = new ZipEntry(file.getPath().substring(pos));
                out.putNextEntry(entry);
                while ((bytesRead = in.read(buffer)) != -1) out.write(buffer, 0, bytesRead);
                out.closeEntry();
            } finally {
                Util.close(in);
            }
        }
    }
}
