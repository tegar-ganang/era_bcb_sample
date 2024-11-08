package proj.mod.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Class that implements a copy operation using NIO streams.
 * 
 * @author Svilen Velikov
 * 
 * 08.07.2009
 */
public class FileCopyNIO implements Copier {

    /**
     * Makes a copy of the provided file to the needed destination place.
     * 
     * @param source
     * @param dest
     */
    @Override
    public void copy(File source, File dest) throws IOException {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = (new FileInputStream(source)).getChannel();
            out = (new FileOutputStream(dest)).getChannel();
            in.transferTo(0, source.length(), out);
        } catch (FileNotFoundException e) {
            throw new IOException("Wrong source or destination path for backup operation!");
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        }
    }
}
