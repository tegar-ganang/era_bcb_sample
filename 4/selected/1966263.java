package hermes.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import org.apache.log4j.Logger;

/**
 * Various utilities for java.io.
 */
public abstract class IoUtils {

    private static final Logger log = Logger.getLogger(IoUtils.class);

    public static void closeQuietly(Reader o) {
        try {
            if (o != null) {
                o.close();
            }
        } catch (IOException e) {
        }
    }

    public static void closeQuietly(Writer o) {
        try {
            if (o != null) {
                o.close();
            }
        } catch (IOException e) {
        }
    }

    public static void closeQuietly(OutputStream o) {
        try {
            if (o != null) {
                o.close();
            }
        } catch (IOException e) {
        }
    }

    public static void closeQuietly(InputStream o) {
        try {
            if (o != null) {
                o.close();
            }
        } catch (IOException e) {
        }
    }

    public static String readFile(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            return Charset.defaultCharset().decode(bb).toString();
        } finally {
            stream.close();
        }
    }

    public static byte[] readBytes(File f) throws IOException {
        FileInputStream fin = null;
        FileChannel ch = null;
        try {
            fin = new FileInputStream(f);
            ch = fin.getChannel();
            int size = (int) ch.size();
            MappedByteBuffer buf = ch.map(MapMode.READ_ONLY, 0, size);
            byte[] bytes = new byte[size];
            buf.get(bytes);
            return bytes;
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
                if (ch != null) {
                    ch.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String read(File from) throws Exception {
        final FileInputStream istream = new FileInputStream(from);
        final FileChannel channel = istream.getChannel();
        final StringWriter writer = new StringWriter();
        final ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
        int read = 0;
        while ((read = channel.read(buffer)) > 0) {
            writer.append(new String(buffer.array(), 0, read));
        }
        return writer.getBuffer().toString();
    }
}
