package net.kano.joscar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Provides an efficient means of using the contents of a file as a
 * <code>LiveWritable</code>.
 * 
 * @see LiveWritable
 */
public class FileWritable implements LiveWritable {

    /**
     * The file from which to read.
     */
    private final String filename;

    /**
     * Creates a new Writable that will write the contents of the given file
     * on command.
     *
     * @param filename the file whose contents should be written
     */
    public FileWritable(String filename) {
        this.filename = filename;
    }

    /**
     * Returns the file to be written by this object.
     *
     * @return the file that this object represents
     */
    public final String getFilename() {
        return filename;
    }

    /**
     * Writes the contents of this object's file (<code>getFile()</code>) to
     * the given stream.
     *
     * @param out the stream to which to write the file's contents
     * @throws IOException if an I/O error occurs
     */
    public final void write(OutputStream out) throws IOException {
        File file = new File(filename);
        long len = file.length();
        FileInputStream in = new FileInputStream(file);
        try {
            FileChannel inch = in.getChannel();
            WritableByteChannel outch = Channels.newChannel(out);
            int pos = 0;
            while (pos < len) {
                pos += inch.transferTo(pos, len - pos, outch);
            }
        } finally {
            in.close();
        }
    }

    public String toString() {
        return "FileWritable for file " + filename;
    }
}
