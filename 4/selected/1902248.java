package up2p.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import org.apache.log4j.Logger;

/**
 * A utility class for reading and writing files and streams.
 * 
 * @author Neal Arthorne
 * @version 1.0
 */
public abstract class FileUtil {

    /**
     * Worker thread for transferring bytes from one channel to another.
     * 
     * 
     * @author Neal Arthorne
     * @version 1.0
     */
    private static class ChannelWriter extends Thread {

        /** Input byte channel. */
        ReadableByteChannel inputChannel;

        /** Log for writing error messages. */
        Logger log;

        /** Output byte channel. */
        WritableByteChannel outputChannel;

        /**
         * Create worker with channels and a log.
         * 
         * @param input input channel from which bytes will be read until end of
         * stream is reached
         * @param output output channel to which bytes will be written
         * @param logger logger for writing error messages
         */
        public ChannelWriter(ReadableByteChannel input, WritableByteChannel output, Logger logger) {
            inputChannel = input;
            outputChannel = output;
            log = logger;
        }

        /**
         * Read from the data source and into the writable channel.
         */
        public void run() {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (true) {
                buffer.clear();
                try {
                    int bytesRead = inputChannel.read(buffer);
                    if (bytesRead == -1) break;
                } catch (IOException e) {
                    log.error("FileUtil.ChannelWriter: Error reading from byte" + " channel.", e);
                }
                buffer.flip();
                try {
                    while (outputChannel.write(buffer) > 0) {
                    }
                } catch (IOException e) {
                    log.error("FileUtil.ChannelWriter: Error writing to byte" + " channel.", e);
                }
            }
            try {
                outputChannel.close();
            } catch (IOException e) {
                log.error("FileUtil.ChannelWriter: Error closing output" + " channel.", e);
            }
        }
    }

    /**
     * The buffer size in bytes used for reading and writing to files and
     * streams.
     */
    public static final int BUFFER_SIZE = 2048;

    /** Line seperator. */
    public static String LINE_SEPERATOR = System.getProperty("line.separator");

    /**
     * Creates a unique file by appending an integer to the file name if it is
     * already taken.
     * 
     * @param inputFile input file to check for uniqueness
     * @return a unique file name based on the input file
     */
    public static File createUniqueFile(File inputFile) {
        if (!inputFile.exists()) return inputFile;
        File outputFile = inputFile;
        int extensionStartIndex = inputFile.getAbsolutePath().lastIndexOf(".");
        int counter = 1;
        while (outputFile.exists()) {
            if (extensionStartIndex != -1) {
                outputFile = new File(inputFile.getAbsolutePath().substring(0, extensionStartIndex) + "_" + counter + inputFile.getAbsolutePath().substring(extensionStartIndex));
            } else {
                outputFile = new File(inputFile.getAbsolutePath() + "_" + counter);
            }
            counter++;
        }
        return outputFile;
    }

    /**
     * Checks if the filename contains illegal characters and replaces them with
     * underscore.
     * 
     * @param fileName name of the file
     * @return file name with illegal characters replaced
     */
    public static String normalizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9\\.\\-@*!()_]", "_");
    }

    /**
     * Reads characters from a file using the default platform encoding and
     * returns a string.
     * 
     * @param inputFile file containing text to read
     * @return text read from the file
     * @throws IOException if an error occurs reading the file
     */
    public static String readFile(File inputFile) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(inputFile));
        String line;
        StringBuffer output = new StringBuffer();
        while ((line = in.readLine()) != null) output.append(line + LINE_SEPERATOR);
        in.close();
        return output.toString();
    }

    /**
     * Reads a file from the given input stream.
     * 
     * @param inStream the input stream to read from
     * @param outputFile the file to write the output to
     * @param closeStream set to true if the input stream is to be closed after
     * reading its bytes, false otherwise
     * @throws IOException when a read or write error occurs
     */
    public static void readFileFromStream(InputStream inStream, File outputFile, boolean closeStream) throws IOException {
        ReadableByteChannel srcChannel = Channels.newChannel(inStream);
        FileChannel dstChannel = new FileOutputStream(outputFile).getChannel();
        ByteBuffer byteBuff = ByteBuffer.allocateDirect(BUFFER_SIZE);
        int s = 0;
        while ((s = srcChannel.read(byteBuff)) != -1) {
            byteBuff.flip();
            dstChannel.write(byteBuff);
            if (byteBuff.hasRemaining()) byteBuff.compact(); else byteBuff.clear();
        }
        dstChannel.force(true);
        dstChannel.close();
        if (closeStream) {
            srcChannel.close();
        }
    }

    /**
     * Writes a file out to the given OutputStream.
     * 
     * @param outStream the outputstream to write the file to
     * @param inputFile the file to write to the given stream
     * @param closeOutputStream set to true if the output stream is to be closed
     * after writing the file, false otherwise
     * @throws IOException when an error occurs in reading the file or writing
     * to the stream
     */
    public static void writeFileToStream(OutputStream outStream, File inputFile, boolean closeOutputStream) throws IOException {
        FileChannel srcChannel = new FileInputStream(inputFile).getChannel();
        WritableByteChannel dstChannel = Channels.newChannel(outStream);
        srcChannel.transferTo(0, srcChannel.size(), dstChannel);
        srcChannel.close();
        if (closeOutputStream) dstChannel.close();
    }

    /**
     * Writes a file out to the given writer assuming a character-based file and
     * using the default character encoding for this locale.
     * 
     * @param outWriter the writer to write the file to
     * @param inputFile the file to write to the given stream
     * @param closeWriter set to true if the writer is to be closed after
     * writing the file, false otherwise
     * @throws IOException when an error occurs in reading the file or writing
     * to the wrtier
     */
    public static void writeFileToWriter(Writer outWriter, File inputFile, boolean closeWriter) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(inputFile));
        BufferedWriter out = new BufferedWriter(outWriter);
        char[] chars = new char[BUFFER_SIZE];
        int s = 0;
        while ((s = in.read(chars)) != -1) {
            out.write(new String(chars, 0, s));
        }
        out.flush();
        in.close();
        if (closeWriter) {
            out.close();
        }
    }

    /**
     * This method uses a pipe and worker thread to speed up the transfer of
     * bytes from input to output streams. The worker thread writes bytes read
     * from the input stream to the pipe sink. After starting the worker, the
     * main loop of this method reads bytes from the pipe source and writes them
     * to the output.
     * 
     * @param input input stream of bytes
     * @param output output stream of bytes
     * @param log logger for writing error messages
     * @throws IOException if an error occurs when reading or writing to the
     * streams
     */
    public static void writeStreamToStream(InputStream input, OutputStream output, Logger log) throws IOException {
        WritableByteChannel out = Channels.newChannel(output);
        ReadableByteChannel in = Channels.newChannel(input);
        Pipe pipe = Pipe.open();
        ChannelWriter writer = new ChannelWriter(in, pipe.sink(), log);
        writer.start();
        ReadableByteChannel pipeSource = pipe.source();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while (true) {
            buffer.clear();
            try {
                int bytesRead = pipeSource.read(buffer);
                if (bytesRead == -1) break;
            } catch (IOException e) {
                log.error("writeStreamToStream: Error reading from byte" + " channel.", e);
            }
            buffer.flip();
            try {
                while (out.write(buffer) > 0) {
                }
            } catch (IOException e) {
                log.error("writeStreamToStream: Error writing to byte" + " channel.", e);
            }
        }
        in.close();
        out.close();
    }
}
