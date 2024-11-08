package uk.azdev.openfire.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import uk.azdev.openfire.net.messages.IMessage;
import uk.azdev.openfire.net.util.IOUtil;

/**
 * Standalone test class that can read stored sessions with the xfire server
 * captured using a tool like ethereal.
 */
public class StoredSessionReader {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Missing argument: message data file");
            return;
        }
        File fileToRead = getFileToRead(args[0]);
        if (fileToRead == null) {
            return;
        }
        int numBytesToStrip = getNumBytesToStrip(args);
        if (numBytesToStrip == -1) {
            return;
        }
        File messageOutputRoot = getMessageOutputRoot(args);
        ReadableByteChannel channel = getChannel(fileToRead, numBytesToStrip);
        ChannelReader reader = new ChannelReader(channel);
        readAllMessages(reader, messageOutputRoot);
    }

    private static File getFileToRead(String filePath) {
        File fileToRead = new File(filePath);
        if (!fileToRead.exists()) {
            System.err.println("File \"" + filePath + "\" does not exist");
            return null;
        }
        if (!fileToRead.isFile()) {
            System.err.println("File \"" + filePath + "\" is not a file");
            return null;
        }
        return fileToRead;
    }

    private static int getNumBytesToStrip(String[] args) {
        int numBytesToStrip = 0;
        if (args.length > 1) {
            try {
                numBytesToStrip = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Second argument is not a valid integer");
                return -1;
            }
            if (numBytesToStrip < 0) {
                System.err.println("Second argument is not a positive integer");
                return -1;
            }
        }
        return numBytesToStrip;
    }

    private static File getMessageOutputRoot(String[] args) {
        if (args.length < 3) {
            return null;
        }
        String dirPath = args[2];
        File outputRoot = new File(dirPath);
        if (!outputRoot.exists()) {
            System.err.println("Message output root directory \"" + dirPath + "\" does not exist");
            return null;
        }
        if (!outputRoot.isDirectory()) {
            System.err.println("Message output root \"" + outputRoot + "\" is not a directory");
            return null;
        }
        return outputRoot;
    }

    private static FileChannel getChannel(File fileToRead, int numBytesToStrip) throws IOException {
        FileInputStream messageStream = new FileInputStream(fileToRead);
        FileChannel channel = messageStream.getChannel();
        stripBytes(channel, numBytesToStrip);
        return channel;
    }

    private static void stripBytes(ReadableByteChannel channel, int numBytesToStrip) throws IOException {
        if (numBytesToStrip == 0) {
            return;
        }
        ByteBuffer buffer = IOUtil.createBuffer(numBytesToStrip);
        IOUtil.readAllBytesOrFail(channel, buffer, numBytesToStrip);
    }

    private static void readAllMessages(ChannelReader reader, File messageOutputRoot) {
        try {
            IMessage message;
            while ((message = reader.readMessage()) != null) {
                System.out.println(message);
                System.out.println();
                writeMessageData(message, messageOutputRoot);
            }
        } catch (IOException e) {
            System.err.println("Error occurred while attempting to read message");
            e.printStackTrace();
        }
    }

    private static void writeMessageData(IMessage message, File messageOutputRoot) throws IOException {
        if (messageOutputRoot == null) {
            return;
        }
        int id = message.getMessageId();
        File tempFile = File.createTempFile(String.format("%1$07d_", id), ".dat", messageOutputRoot);
        System.out.println("Writing data to \"" + tempFile.getPath() + "\"");
        FileChannel channel = new FileOutputStream(tempFile).getChannel();
        ByteBuffer buffer = IOUtil.createBuffer(message.getMessageContentSize());
        message.writeMessageContent(buffer);
        buffer.flip();
        channel.write(buffer);
        channel.close();
    }
}
