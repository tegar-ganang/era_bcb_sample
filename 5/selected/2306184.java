package archlib.ziplib;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import archlib.AbstractArchiveInputStream;
import archlib.ArchiveEntry;
import archlib.util.CustomLogger;

/**
 * This class uses the {@link ZipInputStream} to extract a zip archive.
 * None of the methods from {@link ZipInputStream} are overridden in this
 * class. If you at all need to leverage the extra capabilities provided
 * by the {@link ZipInputStream} you can get its reference by <br/>
 * {@code ZipInputStream zis = inflater.getArchiveInputStream().getNativeInputStream(ZipInputStream.class);}
 * <br/>
 *
 *
 * @author Vaman Kulkarni
 *
 */
public class ZipFileInputStream extends AbstractArchiveInputStream {

    private Logger log = CustomLogger.getLogger(ZipFileInputStream.class);

    private ZipInputStream zis = null;

    public ZipFileInputStream(InputStream in) {
        zis = new ZipInputStream(in);
    }

    @Override
    public ArchiveEntry getNextEntry() throws IOException {
        ZipEntry local = zis.getNextEntry();
        if (null != local) return new ZipFileEntry(local);
        log.warning("Returning a NULL entry");
        return null;
    }

    @Override
    public <X extends InputStream> X getNativeInputStream(Class<X> streamClass) {
        return streamClass.cast(zis);
    }
}
