package net.sourceforge.javautil.common;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sourceforge.javautil.common.io.IModifiableInputSource;

/**
 * Utilities/methods for dealing with input and output streams
 * and transfering data between them.
 * 
 * @author ponder
 * @author $Author: ponderator $
 * @version $Id: IOUtil.java 2297 2010-06-16 00:13:14Z ponderator $
 */
public class IOUtil {

    public static int defaultBufferSize = 10240;

    /**
	 * @param data The byte[] array from which to make an input source
	 * @return The MIS wrapper
	 */
    public static IModifiableInputSource getInputSource(byte[] data) {
        return new IModifiableInputSource.MemoryInputSource().setContents(data);
    }

    /**
	 * @param url The url from which to make a MIS
	 * @return The MIS wrapper
	 */
    public static IModifiableInputSource getInputSource(URL url) {
        return new IModifiableInputSource.URLInputSource(url);
    }

    /**
	 * @param file The file from which to make a MIS
	 * @return The MIS wrapper
	 */
    public static IModifiableInputSource getInputSource(File file) {
        return new IModifiableInputSource.FileInputSource(file);
    }

    /**
	 * This assumes the default buffer.
	 * 
	 * @see #read(InputStream, byte[])
	 */
    public static byte[] read(InputStream i) {
        return read(i, null);
    }

    /**
	 * This assumes that the input stream will be closed after reading is completed.
	 * 
	 * @see #read(InputStream, byte[], boolean)
	 */
    public static byte[] read(InputStream i, byte[] buffer) {
        return read(i, buffer, true);
    }

    /**
	 * This will use a {@link ByteArrayOutputStream} to collect the output and
	 * return its collected contents.
	 * 
	 * @see #transfer(InputStream, OutputStream, byte[], boolean)
	 */
    public static byte[] read(InputStream i, byte[] buffer, boolean closeIS) {
        return transfer(i, new ByteArrayOutputStream(), buffer, closeIS).toByteArray();
    }

    /**
	 * This assumes the default buffer and to close the input stream after
	 * reading is completed.
	 * 
	 * @see #transfer(InputStream, OutputStream, byte[], boolean)
	 */
    public static <T extends OutputStream> T transfer(InputStream i, T o) {
        return transfer(i, o, null, true);
    }

    /**
	 * @param <T> The type of output stream
	 * @param i The input stream to read from
	 * @param o The output stream to write to
	 * @param buffer The buffer to use for reading/writing, null for default buffer
	 * @param closeInput True if the input stream should be closed after reading is completed, otherwise false
	 * @return The output stream that was written to
	 */
    public static <T extends OutputStream> T transfer(InputStream i, T o, byte[] buffer, boolean closeInput) {
        try {
            int read = -1;
            if (buffer == null) buffer = new byte[defaultBufferSize];
            while ((read = i.read(buffer)) != -1) {
                o.write(buffer, 0, read);
                o.flush();
            }
            return o;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            if (closeInput) try {
                i.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
	 * An output stream that will wrap various output streams and write
	 * all contents written to it to each one of them.
	 *
	 * @author elponderador
	 * @author $Author: ponderator $
	 * @version $Id: IOUtil.java 2297 2010-06-16 00:13:14Z ponderator $
	 */
    public static class MultiplexedOutputStream extends OutputStream {

        private OutputStream[] streams;

        public MultiplexedOutputStream(OutputStream... outputStreams) {
            this.streams = new OutputStream[outputStreams.length];
            System.arraycopy(outputStreams, 0, streams, 0, streams.length);
        }

        @Override
        public void close() throws IOException {
            for (int o = 0; o < streams.length; o++) streams[o].close();
        }

        @Override
        public void flush() throws IOException {
            for (int o = 0; o < streams.length; o++) streams[o].flush();
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            for (int o = 0; o < streams.length; o++) streams[o].write(b, off, len);
        }

        @Override
        public void write(byte[] b) throws IOException {
            for (int o = 0; o < streams.length; o++) streams[o].write(b);
        }

        @Override
        public void write(int b) throws IOException {
            for (int o = 0; o < streams.length; o++) streams[o].write(b);
        }
    }
}
