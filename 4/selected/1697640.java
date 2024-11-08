package info.nekonya.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A collection of utilities for IO tasks.
 */
public class IOUtils {

    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(IOUtils.class.getCanonicalName());

    private static final int BUFFER_SIZE_BYTES = 128 * 1024;

    public static enum CopyMode {

        OverwriteFile(1), OverwriteFolder(3);

        int val;

        private CopyMode(int value) {
            val = value;
        }
    }

    private IOUtils() {
    }

    public static void transfer(ReadableByteChannel in, WritableByteChannel out) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[BUFFER_SIZE_BYTES]);
        while (in.read(buffer) > 0) {
            buffer.flip();
            out.write(buffer);
            buffer.clear();
        }
    }

    public static void transfer(InputStream in, OutputStream out) throws IOException {
        ReadableByteChannel inChannel = Channels.newChannel(in);
        WritableByteChannel outChannel = Channels.newChannel(out);
        transfer(inChannel, outChannel);
    }

    public static void transfer(File fromFile, File toFile) throws IOException {
        InputStream in = new FileInputStream(fromFile);
        try {
            OutputStream out = new FileOutputStream(toFile);
            try {
                transfer(in, out);
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                    log.warning(e.getLocalizedMessage());
                }
            }
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                log.warning(e.getLocalizedMessage());
            }
        }
    }

    public static List<Byte> read(ReadableByteChannel in) throws IOException {
        List<Byte> byteList = new ArrayList<Byte>();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[BUFFER_SIZE_BYTES]);
        while (in.read(buffer) > 0) {
            buffer.flip();
            byte[] temp = buffer.array();
            for (byte b : temp) byteList.add(b);
        }
        return byteList;
    }

    public static void write(List<Byte> bytes, WritableByteChannel out) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[BUFFER_SIZE_BYTES]);
        int nextByteIndex = 0;
        while (nextByteIndex < bytes.size()) {
            int rangeEnd = (nextByteIndex + BUFFER_SIZE_BYTES > bytes.size()) ? bytes.size() : nextByteIndex + BUFFER_SIZE_BYTES;
            for (int i = nextByteIndex; i < rangeEnd; i++) buffer.put(bytes.get(i));
            nextByteIndex = rangeEnd;
            out.write(buffer);
        }
    }

    public static List<Byte> read(InputStream in) throws IOException {
        ReadableByteChannel channel = Channels.newChannel(in);
        return read(channel);
    }

    public static void write(List<Byte> bytes, OutputStream out) throws IOException {
        WritableByteChannel channel = Channels.newChannel(out);
        write(bytes, channel);
    }

    public static String readText(ReadableByteChannel in, Charset encoding) throws IOException {
        ByteBuffer bBuffer = ByteBuffer.wrap(new byte[BUFFER_SIZE_BYTES]);
        CharBuffer cBuffer = CharBuffer.wrap(new char[BUFFER_SIZE_BYTES]);
        StringBuffer strBuffer = new StringBuffer();
        CharsetDecoder decoder = encoding.newDecoder();
        boolean done = false;
        while (!done) {
            bBuffer.clear();
            cBuffer.clear();
            done = (in.read(bBuffer) == -1);
            bBuffer.flip();
            decoder.decode(bBuffer, cBuffer, done);
            cBuffer.flip();
            strBuffer.append(cBuffer);
        }
        return strBuffer.toString();
    }

    public static void writeText(String text, WritableByteChannel out, Charset encoding) throws IOException {
        CharBuffer cBuffer = CharBuffer.wrap(new char[BUFFER_SIZE_BYTES]);
        ByteBuffer bBuffer = ByteBuffer.wrap(new byte[BUFFER_SIZE_BYTES]);
        CharsetEncoder encoder = encoding.newEncoder();
        int nextCharIndex = 0;
        boolean done = false;
        while (!done) {
            int rangeEnd = (nextCharIndex + BUFFER_SIZE_BYTES > text.length()) ? text.length() : nextCharIndex + BUFFER_SIZE_BYTES;
            for (int i = nextCharIndex; i < rangeEnd; i++) cBuffer.append(text.charAt(i));
            nextCharIndex = rangeEnd;
            done = nextCharIndex < text.length();
            encoder.encode(cBuffer, bBuffer, done);
            out.write(bBuffer);
        }
    }

    public static String readText(InputStream in, String encoding) throws IOException {
        ReadableByteChannel channel = Channels.newChannel(in);
        return readText(channel, Charset.forName(encoding));
    }

    public static void writeText(String text, OutputStream out, String encoding) throws IOException {
        WritableByteChannel channel = Channels.newChannel(out);
        writeText(text, channel, Charset.forName(encoding));
    }

    public static String readText(File file, String encoding) throws IOException {
        FileChannel channel = new FileInputStream(file).getChannel();
        Charset charset = Charset.forName(encoding);
        return readText(channel, charset);
    }

    public static void writeText(String text, File file, String encoding) throws IOException {
        FileChannel channel = new FileInputStream(file).getChannel();
        writeText(text, channel, Charset.forName(encoding));
    }

    public static void copy(File from, File to, CopyMode mode) throws IOException {
        if (!from.exists()) {
            IllegalArgumentException e = new IllegalArgumentException("Source doesn't exist: " + from.getCanonicalFile());
            log.throwing("IOUtils", "copy", e);
            throw e;
        }
        if (from.isFile()) {
            if (!to.canWrite()) {
                IllegalArgumentException e = new IllegalArgumentException("Cannot write to target location: " + to.getCanonicalFile());
                log.throwing("IOUtils", "copy", e);
                throw e;
            }
        }
        if (to.exists()) {
            if ((mode.val & CopyMode.OverwriteFile.val) != CopyMode.OverwriteFile.val) {
                IllegalArgumentException e = new IllegalArgumentException("Target already exists: " + to.getCanonicalFile());
                log.throwing("IOUtils", "copy", e);
                throw e;
            }
            if (to.isDirectory()) {
                if ((mode.val & CopyMode.OverwriteFolder.val) != CopyMode.OverwriteFolder.val) {
                    IllegalArgumentException e = new IllegalArgumentException("Target is a folder: " + to.getCanonicalFile());
                    log.throwing("IOUtils", "copy", e);
                    throw e;
                } else to.delete();
            }
        }
        if (from.isFile()) {
            FileChannel in = new FileInputStream(from).getChannel();
            FileLock inLock = in.lock();
            FileChannel out = new FileOutputStream(to).getChannel();
            FileLock outLock = out.lock();
            try {
                in.transferTo(0, (int) in.size(), out);
            } finally {
                inLock.release();
                outLock.release();
                in.close();
                out.close();
            }
        } else {
            to.mkdirs();
            File[] contents = to.listFiles();
            for (File file : contents) {
                File newTo = new File(to.getCanonicalPath() + "/" + file.getName());
                copy(file, newTo, mode);
            }
        }
    }
}
