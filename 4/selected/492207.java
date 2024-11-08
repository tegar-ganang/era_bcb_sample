package org.red5.io.mp4;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Wrapper class for input streams containing MPEG4 data.
 * 
 * Original idea based on code from MediaFrame (http://www.mediaframe.org)
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public final class MP4DataStream {

    /** The input stream. */
    private FileInputStream is;

    /** The current offset (position) in the stream. */
    private long offset = 0;

    /**
	 * Constructs an <code>MP4DataStream</code> object using the specified
	 * MPEG4 input stream.
	 * 
	 * @param is
	 *            the MPEG4 input stream.
	 */
    public MP4DataStream(FileInputStream is) {
        this.is = is;
    }

    public long readBytes(int n) throws IOException {
        int c = -1;
        long result = 0;
        while ((n-- > 0) && ((c = is.read()) != -1)) {
            result <<= 8;
            result += c & 0xff;
            offset++;
        }
        if (c == -1) {
            throw new EOFException();
        }
        return result;
    }

    public String readString(int n) throws IOException {
        char c = (char) -1;
        StringBuilder sb = new StringBuilder();
        while ((n-- > 0) && ((c = (char) is.read()) != -1)) {
            sb.append(c);
            offset++;
        }
        if (c == -1) {
            throw new EOFException();
        }
        return sb.toString();
    }

    public void skipBytes(long n) throws IOException {
        offset += n;
        is.skip(n);
    }

    public long getOffset() {
        return offset;
    }

    public FileChannel getChannel() {
        return is.getChannel();
    }

    public void close() throws IOException {
        is.close();
    }
}
