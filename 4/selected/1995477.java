package jpfm.partialffs.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import jpfm.partialffs.Part;

/**
 *
 * @author Shashank Tulsyan
 */
public class RandomSplit {

    public static void main(String[] args) throws Exception {
        Path p = Paths.get("J:\\Video\\bigbuckbunny(200v+64a)kbps.mkv");
        FileChannel readChannel = FileChannel.open(p, StandardOpenOption.READ);
        Path destFolder = Paths.get("J:\\neembuu\\heap\\bigbuckbunny(200v+64a)kbps.mkv");
        for (Path fl : destFolder.newDirectoryStream()) {
            fl.delete();
        }
        HashSet<OpenOption> openOptions = new HashSet<OpenOption>();
        openOptions.add(StandardOpenOption.WRITE);
        openOptions.add(StandardOpenOption.CREATE);
        FileChannel writeChannel_i = FileChannel.open(destFolder.resolve("0x0" + Part.PARTIALFILE_EXTENSTION), openOptions);
        copyFully(readChannel, 0, writeChannel_i, 0, (int) (readChannel.size() / 30));
        writeChannel_i.force(true);
        writeChannel_i.close();
        for (int i = 0; i < 50; i++) {
            long readOffset = (long) (readChannel.size() * Math.random());
            int readSize = (int) (readChannel.size() * Math.random() / 80);
            writeChannel_i = FileChannel.open(destFolder.resolve("0x" + Long.toHexString(readOffset) + Part.PARTIALFILE_EXTENSTION), openOptions);
            copyFully(readChannel, readOffset, writeChannel_i, 0, readSize);
        }
        writeChannel_i.close();
    }

    public static void copyFully(FileChannel src, long src_offset, FileChannel dest, long dest_offset, int length) throws IOException {
        ByteBuffer buffer;
        int written_now = 0, read = 0, read_now, written = 0;
        System.out.println("src_offset=" + src_offset + " length= " + length);
        buffer = ByteBuffer.allocate(length);
        src.position(src_offset);
        int t = 0;
        while (buffer.hasRemaining() && t >= 0) t = src.read(buffer);
        buffer.rewind();
        dest.position(dest_offset);
        while (buffer.hasRemaining()) {
            dest.write(buffer);
        }
        dest.force(true);
    }
}
