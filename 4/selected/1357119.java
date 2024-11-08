package org.log5j.writer;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileLock;
import java.util.Properties;
import org.log5j.Format;
import org.log5j.Writer;

/**
 * <code>FileWriter</code> writes log messages to a file.
 * <p>
 * <code>FileWriter</code> recognises two configuration properties;
 * <ul>
 * <li><code>append</code>: If true, append messages to the end of the file
 * if it exists already; Over-write any existing file if false. The rules for
 * setting the value of <code>append</code> are those of the
 * {@link Boolean#valueOf(String)} method.</li>
 * <li><code>filename</code>: The name of the file to write to. If filename
 * is not an absolute path, It will be relative to the runtime directory of the
 * JVM.</li>
 * </ul>
 * 
 * @author Bruce Ashton
 * @date 2007-07-16
 */
public final class FileWriter extends Writer {

    private final FileLock fileLock;

    private final PrintStream out;

    /**
     * Create a new <code>FileWriter</code>.
     * 
     * @param format the <code>Format</code> object for this
     *            <code>FileWriter</code>
     * @param properties configuration properties for this
     *            <code>FileWriter</code>
     * @throws IOException if the file cannot be opened for writing
     */
    public FileWriter(final Format format, final Properties properties) throws IOException {
        super(format);
        final boolean append = Boolean.valueOf(properties.getProperty("append"));
        final String fileName = properties.getProperty("filename");
        if (fileName == null) {
            throw new NullPointerException("The filename property has not been set");
        }
        final FileOutputStream fileOut = new FileOutputStream(fileName, append);
        fileLock = fileOut.getChannel().tryLock();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                try {
                    if (fileLock != null) {
                        fileLock.release();
                    }
                } catch (IOException e) {
                }
            }
        });
        final BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut);
        out = new PrintStream(bufferedOut);
    }

    @Override
    public void write(final String message) {
        synchronized (out) {
            out.println(message);
            out.flush();
        }
    }
}
