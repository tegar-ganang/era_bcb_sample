package swisseph;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class is meant to be a wrapper to some read functionality of the
 * memory mapped RandomAccessFile class.
 */
public class FilePtrJar extends FilePtrMap {

    private static Logger LOG = Logger.getLogger(SwissEph.class.getName());

    private RandomAccessFile fp;

    private FileChannel fc;

    private MappedByteBuffer mbb = null;

    private String fnamp;

    private long baseOffset;

    private long length;

    /**
   * Attempt to open a zip or jar file.
   * Format is like zip:file://xxx!/a/b
   * <br/> NEEDSWORK: what exceptions should get thrown?
   * @param fnamp the url of the zip-file and zip-section name.
   * @return null if could not open for any reason
   * @throws FileNotFoundException
   * @throws IOException
   */
    public static FilePtr get(String fnamp) throws IOException {
        FilePtrJar fp = null;
        boolean isJar = fnamp.startsWith("jar") || fnamp.startsWith("zip");
        try {
            fnamp = fnamp.replace('\\', '/');
            URL url = new URL(fnamp);
            if (!url.getProtocol().equals("jar")) {
                return null;
            }
            String f = url.getFile();
            String[] fa = f.split("!");
            url = new URL(fa[0]);
            String entryName = fa[1].substring(1);
            if (url.getProtocol().equals("file")) {
                f = url.getFile();
                FileInputStream in = new FileInputStream(f);
                ZipInputStream zin = new ZipInputStream(in);
                ZipEntry ze;
                while ((ze = zin.getNextEntry()) != null) {
                    if (ze.getName().equals(entryName) && !ze.isDirectory()) {
                        break;
                    }
                }
                if (ze == null) {
                    LOG.warning("Not found: " + fnamp + "???");
                } else {
                    if (ze.getMethod() != ZipEntry.STORED) {
                        LOG.warning("Found " + fnamp + " but it is COMPRESSED");
                    } else {
                        long size = ze.getCompressedSize();
                        long entryOffset = in.getChannel().position();
                        RandomAccessFile raf = new RandomAccessFile(f, "r");
                        zin.close();
                        raf.seek(entryOffset);
                        fp = new FilePtrJar(raf, fnamp, entryOffset, size);
                    }
                }
            }
        } catch (MalformedURLException ex) {
            if (isJar) LOG.warning("FilePtrJar: " + ex.getMessage());
        } catch (FileNotFoundException ex) {
            if (isJar) LOG.info(ex.getMessage());
        }
        return fp;
    }

    private FilePtrJar(RandomAccessFile fp, String fnamp, long baseOffset, long nBytes) throws IOException {
        super(fp, fnamp, baseOffset, nBytes);
    }
}
