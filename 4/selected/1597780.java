package oracle.toplink.essentials.internal.weaving;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * The abstract class provides a set of methods to out outputs into the sepcified archive file.
 */
public abstract class AbstractStaticWeaveOutputHandler {

    protected JarOutputStream outputStreamHolder = null;

    /**
     * create directory into target directory, or insert directory entry into outputstream.
     * @param dirPath
     * @throws IOException
     */
    public abstract void addDirEntry(String dirPath) throws IOException;

    /**
     * Write entry bytes into target, this is usually called if class has been tranformed
     * @param targetEntry
     * @param entryBytes
     * @throws IOException
     */
    public abstract void addEntry(JarEntry targetEntry, byte[] entryBytes) throws IOException;

    /**
     * Write entry into target, this method usually copy original class into target.
     * @param jis
     * @param entry
     * @throws IOException
     */
    public abstract void addEntry(InputStream jis, JarEntry entry) throws IOException, URISyntaxException;

    /**
     * Close the output stream.
     * @throws IOException
     */
    public void closeOutputStream() throws IOException {
        if (outputStreamHolder != null) {
            outputStreamHolder.close();
        }
    }

    /**
     * Get the ouput stream instance.
     * @return
     */
    public JarOutputStream getOutputStream() {
        return this.outputStreamHolder;
    }

    protected void readwriteStreams(InputStream in, OutputStream out) throws IOException {
        int numRead;
        byte[] buffer = new byte[8 * 1024];
        while ((numRead = in.read(buffer, 0, buffer.length)) != -1) {
            out.write(buffer, 0, numRead);
        }
    }
}
