package consciouscode.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
    Class documentation.

    <p>
    For another approach to this problem, see
*/
public class ZipCreator extends ZipOutputStream {

    public ZipCreator(File zipFile) throws IOException {
        super(new FileOutputStream(zipFile));
    }

    /**
       @throws FileNotFoundException if the given file does not exist.
       @throws IOException for other IO problems.
    */
    public void addFile(String archiveName, File file) throws FileNotFoundException, IOException {
        FileInputStream fileIn = new FileInputStream(file);
        try {
            addStream(archiveName, fileIn);
        } finally {
            fileIn.close();
        }
    }

    public void addStream(String archiveName, InputStream input) throws IOException {
        ZipEntry entry = new ZipEntry(archiveName);
        entry.setMethod(ZipEntry.DEFLATED);
        putNextEntry(entry);
        byte[] buffer = new byte[1024];
        int readBytes = 0;
        while ((readBytes = input.read(buffer, 0, buffer.length)) > 0) {
            write(buffer, 0, readBytes);
        }
        closeEntry();
    }

    public void addReader(String archiveName, Reader reader) throws IOException {
        ZipEntry entry = new ZipEntry(archiveName);
        entry.setMethod(ZipEntry.DEFLATED);
        putNextEntry(entry);
        OutputStreamWriter writer = new OutputStreamWriter(this);
        char[] buffer = new char[1024];
        int readChars = 0;
        while ((readChars = reader.read(buffer, 0, buffer.length)) > 0) {
            writer.write(buffer, 0, readChars);
        }
        writer.flush();
        closeEntry();
    }
}
