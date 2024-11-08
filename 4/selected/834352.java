package org.genos.gmf.resources.search;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.genos.gmf.Configuration;

/**
 * Class to perform the indexing work of the contents of resources.
 */
public class OpenOfficeFileIndexer extends FileIndexer {

    OpenOfficeFileIndexer(String _file) {
        super();
        file = _file;
    }

    public String getContent() {
        Enumeration entries;
        ZipFile zipFile;
        String contents = "";
        try {
            zipFile = new ZipFile(file);
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.getName().equals("content.xml")) copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(Configuration.homeTemp + "content.xml")));
            }
            zipFile.close();
            File f = new File(Configuration.homeTemp + "content.xml");
            contents = new XMLFileIndexer(f.getAbsolutePath()).getContent();
            f.delete();
        } catch (Exception e) {
            Configuration.logger.error("OpenOfficeFileIndexer", e);
        }
        return contents;
    }

    /**
     * Buffered copy from input stream to outputstream
     * @param in		InputStream
     * @param out		OutStream
     * @throws IOException
     */
    private void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }
}
