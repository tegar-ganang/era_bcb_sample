package edu.xtec.jclic.fileSystem;

import java.io.*;
import java.util.zip.*;
import java.util.Vector;
import edu.xtec.util.ResourceBridge;

/**
 *
 * @author Francesc Busquets (fbusquets@xtec.net)
 * @version 1.0
 */
public class UrlZip extends ZipFileSystem {

    /** Creates new ZipFileSystem */
    public UrlZip(String rootPath, String fName, ResourceBridge rb) throws Exception {
        super(rootPath, fName, rb);
        ZipInputStream zis = new ZipInputStream(super.getInputStream(fName));
        Vector v = new Vector();
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = zis.read(buffer, 0, 1024)) > 0) baos.write(buffer, 0, bytesRead);
            v.add(new UrlZipEntry(entry, baos.toByteArray()));
            zis.closeEntry();
        }
        zis.close();
        entries = (UrlZipEntry[]) v.toArray(new UrlZipEntry[v.size()]);
    }

    protected class UrlZipEntry extends ExtendedZipEntry {

        byte[] data;

        UrlZipEntry(ZipEntry entry) {
            super(entry);
            data = null;
        }

        UrlZipEntry(ZipEntry entry, byte[] setData) {
            super(entry);
            data = setData;
        }

        public byte[] getBytes() throws IOException {
            return data;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }
    }
}
