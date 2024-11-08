package net.sf.zip.internal.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.sf.zip.internal.Util;
import net.sf.zip.internal.ZipPlugin;
import net.sf.zip.internal.model.tar.TarEntry;
import net.sf.zip.internal.model.tar.TarFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * @author allon moritz
 */
public class ArchiveEntry extends Archive {

    public static final SimpleDateFormat FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private final Object file;

    private final Object entry;

    public ArchiveEntry(Object entry, Object file) {
        super(getPath(entry));
        this.entry = entry;
        this.file = file;
    }

    protected static IPath getPath(Object entry) {
        if (entry instanceof ZipEntry) return new Path(((ZipEntry) entry).getName());
        if (entry instanceof TarEntry) return new Path(((TarEntry) entry).getName());
        return null;
    }

    @Override
    public String toString() {
        return getPath().lastSegment();
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
            Util.close(in);
            Util.close(out);
        }
    }

    private InputStream getStream() throws Exception {
        if (file instanceof ZipFile) return ((ZipFile) file).getInputStream((ZipEntry) entry);
        if (file instanceof TarFile) return ((TarFile) file).getInputStream((TarEntry) entry);
        return null;
    }

    public String getLabel(int type) {
        switch(type) {
            case NAME:
                return getPath().lastSegment();
            case SIZE:
                if (entry instanceof ZipEntry) return "" + ((ZipEntry) entry).getSize();
                if (entry instanceof TarEntry) return "" + ((TarEntry) entry).getSize();
            case PACKED_SIZE:
                if (entry instanceof ZipEntry) return "" + ((ZipEntry) entry).getCompressedSize();
                if (entry instanceof TarEntry) return NO_SIZE_LABEL;
            case TYPE:
                if (entry instanceof ZipEntry && ((ZipEntry) entry).isDirectory()) return FOLDER_LABEL;
                if (entry instanceof TarEntry && ((TarEntry) entry).getFileType() == TarEntry.DIRECTORY) return FOLDER_LABEL;
                return getPath().getFileExtension();
            case CHANGED:
                if (entry instanceof ZipEntry) return FORMAT.format(new Date(((ZipEntry) entry).getTime()));
                if (entry instanceof TarEntry) return FORMAT.format(new Date(((ZipEntry) entry).getTime()));
        }
        return null;
    }
}
