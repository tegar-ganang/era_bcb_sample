package oracle.toplink.essentials.internal.weaving;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarEntry;

/**
 * The class provides a set of methods to pack passed-in entries into the sepcified archive file.
 * the class handle directory output.
 */
public class StaticWeaveDirectoryOutputHandler extends AbstractStaticWeaveOutputHandler {

    private URL source = null;

    private URL target = null;

    /**
     * Construct an instance of StaticWeaveDirectoryOutputHandler.
     * @param source
     * @param target
     */
    public StaticWeaveDirectoryOutputHandler(URL source, URL target) {
        this.source = source;
        this.target = target;
    }

    /**
     * create directory into target directory.
     * @param dirPath
     * @throws IOException
     */
    public void addDirEntry(String dirPath) throws IOException {
        File file = new File(this.target.getPath() + File.separator + dirPath).getAbsoluteFile();
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * Write entry bytes into target, this method is usually invoked  if class has been tranformed
     * @param targetEntry
     * @param entryBytes
     * @throws IOException
     */
    public void addEntry(JarEntry targetEntry, byte[] entryBytes) throws IOException {
        File target = new File(this.target.getPath() + targetEntry.getName()).getAbsoluteFile();
        if (!target.exists()) {
            target.createNewFile();
        }
        (new FileOutputStream(target)).write(entryBytes);
    }

    /**
     * Write entry into target, this method usually copy original class into target.
     * @param jis
     * @param entry
     * @throws IOException
     */
    public void addEntry(InputStream jis, JarEntry entry) throws IOException, URISyntaxException {
        File target = new File(this.target.getPath() + entry.getName()).getAbsoluteFile();
        if (!target.exists()) {
            target.createNewFile();
        }
        if ((new File(this.source.toURI())).isDirectory()) {
            File sourceEntry = new File(this.source.getPath() + entry.getName());
            FileInputStream fis = new FileInputStream(sourceEntry);
            byte[] classBytes = new byte[fis.available()];
            fis.read(classBytes);
            (new FileOutputStream(target)).write(classBytes);
        } else {
            readwriteStreams(jis, (new FileOutputStream(target)));
        }
    }
}
