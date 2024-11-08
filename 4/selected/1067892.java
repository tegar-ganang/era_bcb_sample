package org.mpn.contacts.importer;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;
import java.lang.ref.WeakReference;

/**
 * Generic utility class for stream handling purposes.
 *
 * todo [6] replace com.jnetx.management.util.ManagementUtil.copyChannel with this
 *
 * @author <a href="mailto:batoian@mail.ru">Alex Batoian</a>
 */
public final class IoUtils {

    private static final int BUFFER_SIZE = 16 * 1024;

    private static final ThreadLocal<WeakReference<ByteBuffer>> threadLocalBuffer = new ThreadLocal<WeakReference<ByteBuffer>>();

    private IoUtils() {
    }

    /**
     * Removes file or directory
     *
     * @param file
     */
    public static void removeFile(File file) {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            for (File file1 : file.listFiles()) {
                removeFile(file1);
            }
        }
        file.delete();
    }

    /**
     * Copy input stream content to output stream and closes input stream.
     *
     * @param in
     * @param out
     * @throws IOException
     */
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        for (int count; (count = in.read(buffer)) > 0; ) {
            out.write(buffer, 0, count);
        }
        in.close();
    }

    /**
     * Copy input stream content to output stream and closes input stream.
     *
     * @param in
     * @param out
     * @throws IOException
     */
    public static void copyCharacterStream(Reader in, Writer out) throws IOException {
        char[] buffer = new char[BUFFER_SIZE];
        for (int count; (count = in.read(buffer)) > 0; ) {
            out.write(buffer, 0, count);
        }
        in.close();
    }

    /**
     * Copy len bytes from input stream content and saves them to output stream. Doesn't close input stream.
     *
     * @param in
     * @param out
     * @param len
     * @throws IOException
     */
    public static void copyStream(InputStream in, OutputStream out, int len) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int done = 0;
        int count;
        do {
            int need = Math.min(len - done, buffer.length);
            count = in.read(buffer, 0, need);
            if (count == -1) count = 0;
            out.write(buffer, 0, count);
            done += count;
        } while (count > 0);
    }

    /**
     * Read file and return it's content as byte array
     *
     * @param in
     * @return
     * @throws IOException
     */
    public static byte[] readStreamAsByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        copyStream(in, outputStream);
        outputStream.close();
        byte[] data = outputStream.toByteArray();
        return data;
    }

    /**
     * Read file and return it's content as byte array
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static byte[] readFileAsByteArray(File file) throws IOException {
        FileChannel fileChannel = new FileInputStream(file).getChannel();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) file.length());
        copyChannel(fileChannel, Channels.newChannel(outputStream));
        fileChannel.close();
        outputStream.close();
        byte[] data = outputStream.toByteArray();
        return data;
    }

    /**
     * Write content of byte array to a file
     *
     * @param file file to write array to
     * @param buffer byte array buffer
     * @throws IOException
     */
    public static void writeByteArrayToFile(File file, byte[] buffer) throws IOException {
        FileChannel fileChannel = new FileOutputStream(file).getChannel();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer);
        copyChannel(Channels.newChannel(inputStream), fileChannel);
        fileChannel.close();
        inputStream.close();
    }

    /**
     * Read any channel and return it's content as byte array
     *
     * @param inChannel
     * @return
     * @throws IOException
     */
    public static byte[] readChannelAsByteArray(ReadableByteChannel inChannel) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        copyChannel(inChannel, Channels.newChannel(outputStream));
        inChannel.close();
        outputStream.close();
        byte[] data = outputStream.toByteArray();
        return data;
    }

    /**
     * Copy input file to output file.
     *
     * @param inFile input file
     * @param outFile output file
     * @throws IOException
     */
    public static void copyFile(File inFile, File outFile) throws IOException {
        ReadableByteChannel inChannel = new FileInputStream(inFile).getChannel();
        WritableByteChannel outChannel = new FileOutputStream(outFile).getChannel();
        copyChannel(inChannel, outChannel);
        inChannel.close();
        outChannel.close();
    }

    /**
     * Copy content of inputStream to output channel. Doesn't close output channel on the end
     *
     *
     * @param inChannel
     * @param outChannel
     * @throws IOException
     */
    public static void copyChannel(ReadableByteChannel inChannel, WritableByteChannel outChannel) throws IOException {
        ByteBuffer buffer;
        WeakReference<ByteBuffer> bufferRef = threadLocalBuffer.get();
        if (bufferRef == null || (buffer = bufferRef.get()) == null) {
            buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            bufferRef = new WeakReference<ByteBuffer>(buffer);
            threadLocalBuffer.set(bufferRef);
        }
        while (inChannel.read(buffer) != -1) {
            buffer.flip();
            outChannel.write(buffer);
            buffer.compact();
        }
        buffer.flip();
        while (buffer.hasRemaining()) {
            outChannel.write(buffer);
        }
    }
}
