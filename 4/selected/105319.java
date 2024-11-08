package repeatmap.util;

import repeatmap.types.Statics;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Vector;

/**
 * Generic class for reading files using the nio package.
 *
 * @author Eugene Ie
 */
public class NioFileReader {

    protected File file;

    protected MappedByteBuffer mbb;

    protected FileChannel in;

    protected RandomAccessFile fin;

    private int position_ptr = 0;

    private long[] positions;

    private int last_chunk;

    private final int MAX_SEGMENT_SIZE = 128 * 1024 * 1024;

    /**
     * Default constructor which takes in the projection file name.
     *
     * @param fname the name of the projection file.
     */
    public NioFileReader(String fname) {
        file = new File(fname);
    }

    /**
     * Default constructor which takes in a projection file.
     *
     * @param f the file to be read.
     */
    public NioFileReader(File f) {
        this.file = f;
    }

    /**
     * Method that loads the file into a {@link java.nio.MappedByteBuffer}.
     */
    public void loadFile() {
        try {
            fin = new RandomAccessFile(file, "r");
            in = fin.getChannel();
            initializeSegments();
            int tops = 0;
            if (positions.length > 1) {
                tops = MAX_SEGMENT_SIZE;
            } else {
                tops = last_chunk;
            }
            if (positions.length == 0 || tops == 0) {
                return;
            }
            mbb = in.map(FileChannel.MapMode.READ_ONLY, positions[0], tops);
            mbb.load();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Method that initializes the file into large segments.
     */
    private void initializeSegments() throws IOException {
        if (in == null) {
            return;
        }
        int num_parts = (int) Math.ceil(in.size() / (double) MAX_SEGMENT_SIZE);
        positions = new long[num_parts];
        Statics.logger.info("File name: " + file.getName());
        Statics.logger.info("Size:      " + in.size());
        Statics.logger.info("Num parts: " + num_parts);
        for (int i = 0; i < positions.length; i++) {
            positions[i] = (long) i * MAX_SEGMENT_SIZE;
        }
        last_chunk = (int) (in.size() - ((long) MAX_SEGMENT_SIZE * (positions.length - 1)));
    }

    /**
     * Reads a segment off from the file, large enough for processing.
     *
     * @return StringBuffer buffer containing the segment.
     */
    public StringBuffer readBuffer() {
        if (mbb == null) {
            return null;
        }
        char c;
        StringBuffer buffer = new StringBuffer();
        while (mbb.hasRemaining()) {
            if ((c = (char) mbb.get()) != '\n') {
                buffer.append(c);
            }
        }
        return buffer;
    }

    /**
     * Closes a file stream.
     *
     */
    public void close() {
        if (mbb != null) {
            mbb.clear();
            try {
                in.close();
                fin.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        mbb = null;
        in = null;
        fin = null;
        System.gc();
        System.runFinalization();
    }

    /**
     * Method to rewind file to the start.
     */
    public void rewindFile() {
        if (mbb != null) {
            if (position_ptr == 0) {
                mbb.rewind();
            } else {
                try {
                    mbb = in.map(FileChannel.MapMode.READ_ONLY, positions[0], MAX_SEGMENT_SIZE);
                    mbb.load();
                    position_ptr = 0;
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }

    /**
     * Method that will try to advance to the next segment if
     * the current buffer is done reading.  The call will be
     * return true if we advance to the next buffer.  If there
     * are no more buffers, the call will return false.
     */
    public boolean readNextSegment() {
        int next_position = position_ptr + 1;
        if (next_position == positions.length) {
            return false;
        } else {
            if (next_position % 2 == 0) {
                System.gc();
                System.runFinalization();
            }
            if (next_position % 20 == 0 || next_position == positions.length - 1) {
                Statics.logger.info("Reading segment " + next_position + " of " + positions.length);
            }
            int tops = MAX_SEGMENT_SIZE;
            if (next_position == positions.length - 1) {
                tops = last_chunk;
            }
            try {
                mbb = in.map(FileChannel.MapMode.READ_ONLY, positions[next_position], tops);
                mbb.load();
                position_ptr = next_position;
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        return true;
    }

    /**
     * Return the number of segments.  (length of positions array)
     */
    public int getNumSegments() {
        return positions.length;
    }

    /**
     * Return the file size.
     */
    public long getFileSize() {
        return (positions.length - 1) * (long) MAX_SEGMENT_SIZE + last_chunk;
    }
}
