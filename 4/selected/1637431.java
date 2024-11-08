package oracle.toplink.essentials.internal.weaving;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipException;

/**
 * The class provides a set of methods to pack passing in entries into the sepcified archive file.
 * the class JAR output.
 */
public class StaticWeaveJAROutputHandler extends AbstractStaticWeaveOutputHandler {

    /**
     * Construct an instance of StaticWeaveJAROutputHandler
     * @param outputStreamHolder
     */
    public StaticWeaveJAROutputHandler(JarOutputStream outputStreamHolder) {
        super.outputStreamHolder = outputStreamHolder;
    }

    /**
     * Add directory entry into outputstream.
     * @param dirPath
     * @throws IOException
     */
    public void addDirEntry(String dirPath) throws IOException {
        try {
            JarEntry newEntry = new JarEntry(dirPath);
            newEntry.setSize(0);
            addEntry(newEntry, null);
        } catch (ZipException e) {
        }
    }

    /**
     * Write entry bytes into target, this method is usually called if class has been tranformed
     * @param targetEntry
     * @param entryBytes
     * @throws IOException
     */
    public void addEntry(JarEntry targetEntry, byte[] entryBytes) throws IOException {
        outputStreamHolder.putNextEntry(targetEntry);
        if (entryBytes != null) {
            outputStreamHolder.write(entryBytes);
        }
        outputStreamHolder.closeEntry();
    }

    /**
     * Write entry into target, this method usually copy original class into target.
     * @param jis
     * @param entry
     * @throws IOException
     */
    public void addEntry(InputStream jis, JarEntry entry) throws IOException, URISyntaxException {
        outputStreamHolder.putNextEntry(entry);
        if (!entry.isDirectory()) {
            readwriteStreams(jis, outputStreamHolder);
        }
        outputStreamHolder.closeEntry();
    }
}
