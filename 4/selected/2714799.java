package jp.go.aist.six.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author  Akihito Nakamura, AIST
 * @version $Id: IoUtil.java 301 2011-03-02 07:31:03Z nakamura5akihito $
 */
public class IoUtil {

    /**
     * Logger.
     */
    private static final Logger _LOG_ = LoggerFactory.getLogger(IoUtil.class);

    /**
     */
    public static long readFrom(final OutputStream outstream, final File file) throws IOException {
        long size = 0L;
        InputStream inStream = null;
        try {
            inStream = new FileInputStream(file);
            size = _io(inStream, outstream);
        } finally {
            try {
                inStream.close();
            } catch (Exception ex) {
            }
        }
        return size;
    }

    /**
     */
    public static long writeTo(final InputStream inStream, final File file) throws IOException {
        OutputStream outStream = null;
        long size = 0L;
        try {
            outStream = new FileOutputStream(file);
            size = _io(inStream, outStream);
        } finally {
            try {
                outStream.close();
            } catch (Exception ex) {
            }
        }
        return size;
    }

    /**
     */
    public static long writeTo(final InputStream inStream, final OutputStream outStream) throws IOException {
        return _io(inStream, outStream);
    }

    public static String readCharacters(final File file) throws IOException {
        Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
        StringWriter sw = new StringWriter();
        Writer writer = new BufferedWriter(sw);
        _io(reader, writer);
        String characters = sw.toString();
        return characters;
    }

    /**
     */
    private static void _io(final Reader reader, final Writer writer) throws IOException {
        char[] buffer = new char[512];
        try {
            while (true) {
                int n = reader.read(buffer);
                if (n == -1) {
                    break;
                }
                writer.write(buffer, 0, n);
            }
        } finally {
            try {
                reader.close();
            } catch (Exception ex) {
            }
            try {
                writer.close();
            } catch (Exception ex) {
            }
        }
    }

    /**
     */
    private static long _io(final InputStream inStream, final OutputStream outStream) throws IOException {
        _LOG_.debug("begin IO");
        BufferedInputStream bin = new BufferedInputStream(inStream);
        BufferedOutputStream bout = new BufferedOutputStream(outStream);
        byte[] buffer = new byte[512];
        long size = 0;
        try {
            while (true) {
                int n = bin.read(buffer);
                if (n == -1) {
                    break;
                }
                bout.write(buffer, 0, n);
                size += n;
            }
        } finally {
            try {
                bout.flush();
            } catch (Exception ex) {
            }
        }
        _LOG_.debug("end IO: #bytes=" + size);
        return size;
    }
}
