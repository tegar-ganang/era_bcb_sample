package FFIT.binFileReader;

import FFIT.IdentificationFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * Class providing common functionality for reading streams.
 *
 * <p>This class cannot be instantiated (there is no constructor).
 *
 * @author linb
 */
public class StreamByteReader extends AbstractByteReader {

    /** Creates a new instance of StreamByteReader */
    protected StreamByteReader(IdentificationFile theIDFile) {
        super(theIDFile);
    }

    /** Size of buffer to store the stream in */
    private static final int BUFFER_SIZE = 131072;

    /** Buffer to contain the contents of the stream. */
    protected ByteBuffer buffer = null;

    /** This will be non-null if the stream has been written to a temporary file. */
    protected File tempFile = null;

    /**
     * Read stream into a <code>ByteBuffer</code> or temporary file.
     *
     * <p>This method allocates a buffer, and then attempts to read the stream into
     * it. If the buffer isn't big enough, the contents of it are transferred to a 
     * temporary file, and then the rest of the stream is appended to this file.
     *
     * <p>After this method has been called, the field <code>tempFile</code> is
     * <code>null</code> if the contents of the stream could fit into the buffer,
     * and is the created temporary file otherwise.
     *
     * @param inStream the stream to read in.
     * @throws java.io.IOException if there is an error writing to the temporary 
     * file
     */
    protected void readStream(InputStream inStream) throws IOException {
        ReadableByteChannel c = Channels.newChannel(inStream);
        if (buffer == null) {
            buffer = ByteBuffer.allocate(BUFFER_SIZE);
        } else {
            buffer.clear();
        }
        int bytes = 0;
        while (bytes >= 0 && buffer.hasRemaining()) {
            bytes = c.read(buffer);
        }
        buffer.flip();
        if (buffer.limit() == 0) {
            this.setErrorIdent();
            this.setIdentificationWarning("Zero-length file");
            return;
        }
        if (bytes != -1) {
            tempFile = writeToTempFile(buffer, c);
        }
    }

    /**
     * Write contents of <code>buffer</code> to a temporary file, followed by the remaining bytes
     * in <code>channel</code>.
     *
     * <p>The bytes are read from <code>buffer</code> from the current position to its limit.
     *
     * @param buffer contains the contents of the channel read so far
     * @param channel the rest of the channel
     * @throws java.io.IOException if there is a problem writing to the file
     * @return <code>File</code> object for the temporary file.
     */
    static File writeToTempFile(ByteBuffer buffer, ReadableByteChannel channel) throws IOException {
        File tempFile = java.io.File.createTempFile("droid", null);
        FileChannel fc = (new FileOutputStream(tempFile)).getChannel();
        ByteBuffer buf = ByteBuffer.allocate(8192);
        fc.write(buffer);
        buf.clear();
        for (; ; ) {
            if (channel.read(buf) < 0) {
                break;
            }
            buf.flip();
            fc.write(buf);
            buf.compact();
        }
        fc.close();
        return tempFile;
    }

    /**
     * Get a byte from file
     * @param fileIndex position of required byte in the file
     * @return the byte at position <code>fileIndex</code> in the file
     */
    public byte getByte(long fileIndex) {
        return buffer.get((int) fileIndex);
    }

    /**
     * Gets the current position of the file marker.
     * @return the current position of the file marker
     */
    public long getFileMarker() {
        return buffer.position();
    }

    /**
     * Returns the number of bytes in the file
     */
    public long getNumBytes() {
        return buffer == null ? 0 : buffer.limit();
    }

    /**
     * Position the file marker at a given byte position.
     *
     * <p>The file marker is used to record how far through the file
     * the byte sequence matching algorithm has got.
     *
     * @param markerPosition   The byte number in the file at which to position the marker
     */
    public void setFileMarker(long markerPosition) {
        buffer.position((int) markerPosition);
    }
}
