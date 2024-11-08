package org.biojava.utils.process;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@linkplain java.lang.Runnable multi threaded} class
 * which pipes the contents of an input reader to an output 
 * writer. 
 * @author <a href="mailto:Martin.Szugat@GMX.net">Martin Szugat</a>
 * @version $Revision: 1.3 $
 */
public class ReaderWriterPipe extends AbstractWaitAndRunnable {

    /**
     * The class logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ReaderWriterPipe.class.getName());

    /**
     * The reader from which to read.
     */
    private Reader reader;

    /**
     * The writer to which to write.
     */
    private Writer writer;

    /**
     * A tag for logging.
     */
    private String tag;

    /**
     * Initializes the reader writer pipe.
     * @param reader the reader from which to read. May be <code>null</code>.
     * @param writer the writer to which to write. May be <code>null</code>.
     * @param tag a tag for loggging. May be <code>null</code>.
     */
    public ReaderWriterPipe(Reader reader, Writer writer, String tag) {
        setReader(reader);
        setWriter(writer);
        this.tag = tag;
    }

    /**
     * Gets the reader.
     * @return the reader from which to read. May be <code>null</code>.
     */
    public Reader getReader() {
        return reader;
    }

    /**
     * Gets the writer.
     * @return the writer to which to write. May be <code>null</code>.
     */
    public Writer getWriter() {
        return writer;
    }

    /**
     * Sets the reader.
     * @param reader the reader from which to read. May be <code>null</code>.
     */
    public void setReader(Reader reader) {
        this.reader = reader;
    }

    /**
     * Sets the writer.
     * @param writer the writer to which to write. May be <code>null</code>.
     */
    public void setWriter(Writer writer) {
        this.writer = writer;
    }

    /**
     * {@inheritDoc}
     */
    protected void doRun() throws Exception {
        if (reader != null) {
            BufferedWriter bout = null;
            if (writer != null) {
                bout = new BufferedWriter(writer);
            }
            BufferedReader bin = new BufferedReader(reader);
            boolean log = LOGGER.isLoggable(Level.FINEST);
            String line = null;
            while ((line = bin.readLine()) != null) {
                if (log) {
                    if (tag == null) {
                        LOGGER.finest(line);
                    } else {
                        LOGGER.finest("<" + tag + "> " + line);
                    }
                }
                if (bout != null) {
                    bout.write(line);
                    bout.newLine();
                    bout.flush();
                }
            }
        }
    }
}
