package org.apache.commons.vfs.util;

/**
 * An enumerated type representing the modes of a random access content.
 *
 * @author <a href="mailto:imario@apache.org">Mario Ivankovits</a>
 * @version $Revision: 537714 $ $Date: 2007-05-13 22:51:00 -0700 (Sun, 13 May 2007) $
 */
public class RandomAccessMode {

    /**
	 * read
	 */
    public static final RandomAccessMode READ = new RandomAccessMode(true, false);

    /**
	 * read/write
	 */
    public static final RandomAccessMode READWRITE = new RandomAccessMode(true, true);

    private final boolean read;

    private final boolean write;

    private RandomAccessMode(final boolean read, final boolean write) {
        this.read = read;
        this.write = write;
    }

    public boolean requestRead() {
        return read;
    }

    public boolean requestWrite() {
        return write;
    }

    public String getModeString() {
        if (requestRead()) {
            if (requestWrite()) {
                return "rw";
            } else {
                return "r";
            }
        } else if (requestWrite()) {
            return "w";
        }
        return "";
    }
}
