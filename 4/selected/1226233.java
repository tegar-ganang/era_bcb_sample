package uk.ac.cam.caret.minibix.general.io;

import java.io.*;
import org.apache.commons.io.*;

public class FileCacheInputStreamFountain implements InputStreamFountain {

    private File file;

    FileCacheInputStreamFountain(FileCacheInputStreamFountainFactory factory, InputStream in) throws IOException {
        file = factory.createFile();
        OutputStream out = new FileOutputStream(file);
        IOUtils.copy(in, out);
        in.close();
        out.close();
    }

    public void finished() {
        file.delete();
    }

    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }
}
