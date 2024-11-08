package net.sf.kerner.commons.io.lazy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import net.sf.kerner.commons.file.FileUtils;
import net.sf.kerner.commons.io.GenericReader;
import net.sf.kerner.commons.io.IOUtils;
import net.sf.kerner.commons.io.buffered.BufferedStringReader;

/**
 * A {@code LazyStringReader} provides the ability to read a string quickly from
 * <ul>
 * <li>
 * a {@link java.io.File}</li>
 * <li>
 * a {@link java.io.Writer}</li>
 * <li>
 * an {@link java.io.InputStream}</li>
 * </ul>
 * </p>
 * <p>
 * <b>Attention:</b> reading is not buffered! If you want to read large files,
 * consider to use {@link BufferedStringReader} instead.
 * </p>
 * <p>
 * <b>Example:</b>
 * 
 * <pre>
 * &#064;Test
 * public final void example() throws IOException {
 * 	final java.io.StringReader sr = new java.io.StringReader(&quot;Hallo Welt!&quot;);
 * 	assertEquals(&quot;Hallo Welt!&quot;, reader.read(sr));
 * }
 * </pre>
 * 
 * @author Alexander Kerner
 * @see java.io.File
 * @see java.io.Reader
 * @see java.io.InputStream
 * @version 2010-09-11
 * 
 */
public class LazyStringReader implements GenericReader<String> {

    public String read(File file) throws IOException {
        if (!FileUtils.fileCheck(file)) throw new IOException("cannot access file \"" + file + "\"");
        return read(IOUtils.getInputStreamFromFile(file));
    }

    public String read(Reader reader) throws IOException {
        if (reader == null) throw new NullPointerException();
        final StringWriter writer = new StringWriter();
        try {
            IOUtils.readerToWriter(reader, writer);
            return writer.toString();
        } finally {
            IOUtils.closeProperly(reader);
            IOUtils.closeProperly(writer);
        }
    }

    public String read(InputStream stream) throws IOException {
        if (stream == null) throw new NullPointerException();
        return read(IOUtils.inputStreamToReader(stream));
    }
}
