package ch.skyguide.tools.requirement.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IoHelper {

    /**
	 * <p>
	 * Specifics:
	 * <ul>
	 * <li>Buffers input
	 * <li>Does not close input
	 * </ul>
	 */
    public static byte[] readFully(InputStream _in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(_in, out);
        out.close();
        return out.toByteArray();
    }

    /**
	 * <p>
	 * Specifics:
	 * <ul>
	 * <li>Buffers input and output
	 * <li>Does not close input or output
	 * </ul>
	 */
    public static void copy(InputStream _in, OutputStream _out) throws IOException {
        byte[] buf = new byte[8 * 1024];
        for (; ; ) {
            int read = _in.read(buf);
            if (read == -1) {
                break;
            }
            _out.write(buf, 0, read);
        }
    }

    public static byte[] loadFile(File _f) throws IOException {
        FileInputStream in = new FileInputStream(_f);
        byte[] result = readFully(in);
        in.close();
        return result;
    }

    public static void write(byte[] _data, File _f) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(_data);
        FileOutputStream out = new FileOutputStream(_f);
        copy(in, out);
        out.close();
        in.close();
    }

    public static class DmuxOutputStream extends OutputStream {

        private final OutputStream[] streams;

        public DmuxOutputStream(OutputStream... _streams) {
            streams = _streams;
        }

        @Override
        public void write(int _b) throws IOException {
            for (OutputStream stream : streams) {
                stream.write(_b);
            }
        }

        @Override
        public void write(byte[] _b, int _off, int _len) throws IOException {
            for (OutputStream stream : streams) {
                stream.write(_b, _off, _len);
            }
        }

        @Override
        public void close() throws IOException {
            for (OutputStream stream : streams) {
                stream.close();
            }
        }

        @Override
        public void flush() throws IOException {
            for (OutputStream stream : streams) {
                stream.flush();
            }
        }
    }
}
